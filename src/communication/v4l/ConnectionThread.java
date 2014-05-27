package communication.v4l;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Vector;

import org.apache.commons.codec.binary.Base64;

import util.MyLogger;

/**
 * This class manages an incoming connection to the tcp server and delivers what
 * it is asked for. It reads the http line from the client and deduce the kind
 * of petition. It performs a different action for each kind of petition.
 * 
 * @author ehas
 * 
 */
public class ConnectionThread implements Runnable {

	private static final int AUTH_PAGE = -1;
	private static final int MAIN_PAGE = 0;
	private static final int WEBCAM_PAGE = 1;
	private static final int CONTROL_PAGE = 2;
	private static final int VIDEO_STREAM = 3;
	private static final int UPDATE_CONTROL_VALUE = 4;
	private static final int CAPTURE_FRAME = 5;
	private static final int VIDEO_OPTIONS = 6;
	private static final int UPDATE_VIDEO_OPTIONS = 7;
	private static final int ZOOM_IN = 8;
	private static final int VIDEO_JS = 9;
	private static final int VIDEO_JS_CSS = 10;
	private static final int VIDEO_JS_SWF = 11;
	private static final int VIDEO_JS_WOFF = 12;
	private static final int VIDEO_JS_TTF = 13;
	private static final int FLOWPLAYER_JS = 14;
	private static final int FLOWPLAYER_SWF = 15;
	private static final int FLOWPLAYER_RTMP_SWF = 16;
	private static final int FLOWPLAYER_CONTROLS = 17;

	public static Vector<ClientConnection> clients;
	private String httpLineFromClient;
	private Socket clientSocket;

	/**
	 * Create a new ConnectionThread object for the socket.
	 * 
	 * @param clientSocket Socket of the incoming connection.
	 */
	
	public ConnectionThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	/**
	 * Manages the incoming connection. First it reads the http line from the
	 * client. Then it deduces the action that is requested. Finally, it
	 * performs the action and closes the socket.
	 */
	@Override
	public void run() {

		BufferedReader inStream = null;
		DataOutputStream outStream = null;
		int requestedAction = MAIN_PAGE;

		System.out.println("Connection from "
				+ clientSocket.getInetAddress().getHostAddress() + ":"
				+ clientSocket.getPort());

		// Create input/output streams then check what action
		// was requested

		try {
			inStream = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			outStream = new DataOutputStream(clientSocket.getOutputStream());
			requestedAction = parseLine(inStream);
			if (CamServer.authentication){
				if (!checkAuthorization(inStream)){
					requestedAction = AUTH_PAGE;
					ClientConnection.sendAuthPage(outStream);
				}
			}
		} catch (IOException e) {
			// error setting up in and out streams with this client, abort
			System.out.println("Hay un error");
			try {
				inStream.close();
				outStream.close();
				clientSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}

		// check what other page was requested
		try {
			switch (requestedAction) {
			case AUTH_PAGE:
				ClientConnection.sendAuthPage(outStream);
				break;
			case WEBCAM_PAGE:
				// send webcam viewer page
				ClientConnection.sendWebcamPage(outStream);
				break;
			case CAPTURE_FRAME:
				try {
					// GestorV4L.getInstance().setLocalCapture(true);
					// Set the outstream as external capturer
					synchronized (CamServer.LockCapture) {
						CamServer.setExternalCapture(outStream);
						CamServer.LockCapture.notify();
					}
					// Wait until the image is sent to the user
					synchronized (CamServer.LockExternal) {
						while (CamServer.getExternalCapture() != null) {
							CamServer.LockExternal.wait();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case VIDEO_OPTIONS:
				ClientConnection.sendVideoOptionsPage(outStream);
				break;
			case UPDATE_VIDEO_OPTIONS:
				ClientConnection.updateVideoOption(httpLineFromClient,
						outStream);
				break;
			case ZOOM_IN:
				ClientConnection.sendZoomPage(outStream);
				break;
			case UPDATE_CONTROL_VALUE:
				// parse http line and update the requested control's value
				ClientConnection.updateControlValue(CamServer.controlList,
						CamServer.frameGrabber, httpLineFromClient, outStream);
				// fallthrough so we re-send the control list
			case CONTROL_PAGE:
				// send the control list page
				ClientConnection.sendControlListPage(CamServer.controlList,
						outStream);
				break;
			case VIDEO_JS:
				ClientConnection.sendJs("web/video-js/video.js", outStream);
				break;
			case VIDEO_JS_CSS:
				ClientConnection.sendCss("web/video-js/video-js.min.css",
						outStream);
				break;
			case VIDEO_JS_SWF:
				ClientConnection.sendSwf("web/video-js/video-js.swf", outStream);
				break;
			case VIDEO_JS_WOFF:
				ClientConnection.sendSwf("web/video-js/vjs.woff", outStream);
				break;
			case VIDEO_JS_TTF:
				ClientConnection.sendSwf("web/video-js/vjs.ttf", outStream);
				break;
			case FLOWPLAYER_JS:
				ClientConnection.sendJs(
						"web/flowplayer/flowplayer-3.2.13.min.js", outStream);
				break;
			case FLOWPLAYER_SWF:
				ClientConnection.sendSwf(
						"web/flowplayer/flowplayer-3.2.17.swf", outStream);
				break;
			case FLOWPLAYER_RTMP_SWF:
				ClientConnection.sendSwf(
						"web/flowplayer/flowplayer.rtmp-3.2.13.swf", outStream);
				break;
			case FLOWPLAYER_CONTROLS:
				ClientConnection.sendSwf(
						"web/flowplayer/flowplayer.controls-3.2.16.swf", outStream);
				break;
			case MAIN_PAGE:
			default:
				// send the main page
				ClientConnection.sendMainPage(outStream);
				/**
				CamServer.logger.info(MyLogger
						.getLogRecord("INCOMING_CONNECTION")
						+ " "
						+ clientSocket.getInetAddress().getHostAddress());
				 */
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// close the connection with the client
			try {
				System.out.println("Disconnected from "
						+ clientSocket.getInetAddress().getHostAddress() + ":"
						+ clientSocket.getPort());
				
				inStream.close();
				outStream.close();
				clientSocket.close();
			} catch (Exception e) {
			}
		}
	}

	private int parseLine(BufferedReader in) throws IOException {
		// read the first line to determine which page to send
		httpLineFromClient = in.readLine();

		System.out.println(httpLineFromClient);
		if (httpLineFromClient == null)
			throw new IOException("Read null line");

		if (httpLineFromClient.indexOf("flowplayer-3.2.13.min.js") != -1) {
			return FLOWPLAYER_JS;
		}
		if (httpLineFromClient.indexOf("flowplayer-3.2.17.swf") != -1) {
			return FLOWPLAYER_SWF;
		}
		if (httpLineFromClient.indexOf("flowplayer.rtmp-3.2.13.swf") != -1) {
			return FLOWPLAYER_RTMP_SWF;
		}
		if (httpLineFromClient.indexOf("flowplayer.controls-3.2.16.swf") != -1) {
			return FLOWPLAYER_CONTROLS;
		}
		// if the line contains the word webcam, we want the video viewing page
		if (httpLineFromClient.indexOf("webcam") != -1) {
			return WEBCAM_PAGE;
		}
		
		// if the line contains the word update, we want to update a control's
		// value
		if (httpLineFromClient.indexOf("updateControl") != -1){
			return UPDATE_CONTROL_VALUE;
		}
			
		// if the line contains the word control, we want the control list page
		if (httpLineFromClient.indexOf("control") != -1) {
			return CONTROL_PAGE;
		}
		
		// if the line contains the word stream, we want the control list page
		if (httpLineFromClient.indexOf("stream") != -1) {
			return VIDEO_STREAM;
		}

		// if the line contains the word update, we want to update a control's
		// value
		if (httpLineFromClient.indexOf("capture") != -1) {
			return CAPTURE_FRAME;
		}

		if (httpLineFromClient.indexOf("Options") != -1) {
			return VIDEO_OPTIONS;
		}

		if (httpLineFromClient.indexOf("updateVideo") != -1) {
			return UPDATE_VIDEO_OPTIONS;
		}
		if (httpLineFromClient.indexOf("zoomIn") != -1) {
			return ZOOM_IN;
		}
		if (httpLineFromClient.indexOf("video-js.min.css") != -1) {
			return VIDEO_JS_CSS;
		}
		if (httpLineFromClient.indexOf("video.js") != -1) {
			return VIDEO_JS;
		}
		if (httpLineFromClient.indexOf("video-js.swf") != -1){
			return VIDEO_JS_SWF;
		}
		if (httpLineFromClient.indexOf("/font/vjs.woff") != -1){
			return VIDEO_JS_WOFF;
		}
		if (httpLineFromClient.indexOf("/font/vjs.ttf") != -1){
			return VIDEO_JS_TTF;
		}
		
		return MAIN_PAGE;
	}

	private boolean checkAuthorization (BufferedReader instream) throws IOException{
		String line;
		while ((line = instream.readLine()) != null && !line.trim().isEmpty()){
			if (line.contains("Authorization")){
				line = line.split("Basic ")[1];
				String[] fullLine = new String(Base64.decodeBase64(line), "UTF-8").split(":");
				if (fullLine.length != 2){
					return false;
				}
				String userName = fullLine[0];
				String userPassword = fullLine[1];
				
				if (userName.equals(CamServer.userName) && userPassword.equals(CamServer.userPassword)){
					return true;					
				}
			}
		}
		return false;
	}
}
