package communication.v4l;

import edition.ImageManager;
import gestion.GestorSave;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import util.MyLogger;
import util.ProgressFrame;

/**
 * This class performs a video streaming over RTMP using Ffmpeg/x264 as encoder software and 
 * crtmpserver as video streaming server. 
 * 
 * @author ehas
 * 
 */
public class FfmpegRtmpThread implements Streamer {

	private String encoderName = "ffmpeg";
	private String serverName = "crtmpserver/crtmpserver/crtmpserver";
	private String serverConfigFile = "crtmpserver.lua";
	private String playerName = "ffplay";

	private Process ffmpeg = null;
	private Process crtmpserver;
	private Process ffplay;
	private DefaultStreamGobbler ffmpegstdout, ffmpegstderr, crtmpstdout,
			crtmpstderr, ffplaystdout, ffplaystderr;

	private boolean ffplayActive = false;
	private boolean captureTaken = false;
	private GestorSave saver = new GestorSave();

	private String capturePath = "capture.jpg";
	private String captureQuality = "2";
	
	private HashMap<String, Long> clients = new HashMap<String, Long>();
	
	private static boolean streamNameUpdated = false;

	/**
	 * It starts the video streaming by running crtmpserver and Ffmpeg processes.
	 * When the streaming is initialized, it waits for a image capture
	 * notification.
	 */
	@Override
	public void run() {

		// Run avconv and avserver
		try {
			
			if (!streamNameUpdated){
				updateStreamName();				
			}

			startCrtmpserver(serverConfigFile);
			// startFfplay();

			synchronized (this) {
				startFfmpeg();
				this.notify();
			}
			// Start the libav processes

			Thread waitViewer = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.interrupted()) {
						// There are two events that can launch the viewer:
						// 1. If an image capture has been taken and the viewer
						// was previously active
						// 2. The "launch viewer button" has been pushed
						if (captureTaken == true) {
							captureTaken = false;
						} else {
							synchronized (CamServer.LockViewer) {
								try {
									// Wait for viewer button activation
									CamServer.LockViewer.wait();
								} catch (InterruptedException e) {
									break;
								}
							}
						}

						try {
							startFfplay();
							ffplay.waitFor();
							ffplayActive = false;
						} catch (InterruptedException | IOException e) {
							ffplay.destroy();
							ffplayActive = false;
							break;
						}

					}
				}

			}, "waitViewer");

			waitViewer.start();

			while (!Thread.interrupted()) {

				synchronized (CamServer.LockCapture) {

					// It waits until a CamServer.LockCapture notification. It
					// can be a local or an external capture.
					try {
						CamServer.LockCapture.wait();
					} catch (InterruptedException e) {
						if (waitViewer.isAlive()) {
							waitViewer.interrupt();
							try {
								waitViewer.join();
							} catch (InterruptedException i) {
								i.printStackTrace();
							}
						}
						break;
					}
					
					if (CamServer.cameraCheckBox != null){
						CamServer.cameraCheckBox.setEnabled(false);						
					}
					if (CamServer.indicator != null){
						CamServer.indicator.setEnabled(false);
					}
					
					checkLocalCapture();
					checkExternalCapture();
					
					if (CamServer.cameraCheckBox != null){
						CamServer.cameraCheckBox.setEnabled(true);						
					}
					if (CamServer.indicator != null){
						CamServer.indicator.setEnabled(true);
					}

				}
			}

			stopStreamProcesses();
			disconnectClients();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method must be called before the thread exit. It destroys all
	 * remaining processes.
	 */
	private void stopStreamProcesses() {
		System.out.println("Parando hilo ffmpeg");

		// Stop the libav processes
		try {
			stopFfmpeg();
			stopCrtmpserver();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (ffplay != null) {
			try {
				stopFfplay();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method must be called before the thread exit. It disconnects all
	 * the remaining clients and logs the event.
	 */
	private void disconnectClients(){
		synchronized (clients){
			Iterator it = clients.entrySet().iterator();
			while (it.hasNext()){
				Map.Entry pairs = (Map.Entry) it.next();
				logClientDisconnection((String) pairs.getKey());
				it.remove();
			}
		}
	}

	/**
	 * Starts a ffmpeg process with the parameters saved in the CamServer
	 * static instance. Two additional processes are created to read standard
	 * and error output. The methods waits until the ffmpeg process actually
	 * starts the video streaming
	 * 
	 * @throws IOException
	 */
	private void startFfmpeg() throws IOException {

		String command = encoderName;
		//command = command.concat(" -re");
		command = command.concat(" -f").concat(" video4linux2");
		command = command.concat(" -video_size").concat(
				" " + CamServer.videoWidth + "x" + CamServer.videoHeight);
		command = command.concat(" -framerate").concat(
				" " + CamServer.getFramerate());
		command = command.concat(" -i").concat(
				" " + CamServer.getVideoDevice().getDevicefile());
		
		command = command.concat(" -vcodec").concat(" libx264");
		command = command.concat(" -r").concat(" ") + CamServer.getFramerate();
		command = command.concat(" -pix_fmt").concat(" yuv420p");
		command = command.concat(" -profile:v").concat(" high");
		command = command.concat(" -tune").concat(" zerolatency");
		command = command.concat(" -s").concat(
				" " + CamServer.videoWidth + "x" + CamServer.videoHeight);
		if (CamServer.gop > 0) {
			command = command.concat(" -g").concat(" " + CamServer.gop);
		}
		if (!CamServer.bps.equalsIgnoreCase("-1")) {
			command = command.concat(" -b:v").concat(" " + CamServer.bps);
			command = command.concat(" -maxrate").concat(" " + CamServer.bps);
			command = command.concat(" -bufsize").concat(" " + CamServer.bps);
		}
		command = command.concat(" -me_method").concat(" zero");
		command = command.concat(" -acodec").concat(" none");
		//command = command.concat(" -vbsf").concat(" h264_mp4toannexb");
		command = command.concat(" -f").concat(" mpegts");
		command = command.concat(" udp://127.0.0.1:10000");
		
		ffmpeg = Runtime.getRuntime().exec(command);

		ffmpegstdout = new DefaultStreamGobbler(ffmpeg.getInputStream(), "OUTPUT",
				false, "ffmpeg_out");
		ffmpegstdout.start();
		ffmpegstderr = new FfmpegStreamGobbler(ffmpeg.getErrorStream(), "ERROR",
				true, "ffmpeg_err");
		ffmpegstderr.start();

		// Wait until the video streaming is started
		synchronized (ffmpegstderr) {
			try {
				ffmpegstderr.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the ffmpeg process
	 * 
	 * @throws Exception
	 */
	private void stopFfmpeg() throws Exception {

		// It is necessary to kill the process in this way to be sure that
		// SIGTERM (15) is sent
		// as the kill signal.
		String pid = "" + getUnixPID(ffmpeg);
		Process killingProcess = Runtime.getRuntime().exec(
				new String[] { "kill", "-15", pid });
		killingProcess.waitFor();
		ffmpeg.waitFor();
		ffmpegstdout.interrupt();
		ffmpegstderr.interrupt();
	}

	/**
	 * Starts a crtmpserver process with the configuration file given as parameter
	 * 
	 * @param configFile
	 *            - crtmpserver configuration file
	 * @throws IOException
	 */
	private void startCrtmpserver(String configFile) throws IOException {
		crtmpserver = Runtime.getRuntime().exec(
				new String[] { serverName, configFile });
		crtmpstdout = new CrtmpStreamGobbler(crtmpserver.getInputStream(), "OUTPUT",
				true, "crtmpserver_out");
		crtmpstderr = new DefaultStreamGobbler(crtmpserver.getErrorStream(), "ERROR",
				true, "crtmpserver_err");
		synchronized (crtmpstdout) {
			crtmpstderr.start();
			crtmpstdout.start();

			try {
				crtmpstdout.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Stops the crtmpserver process
	 * @throws InterruptedException
	 */
	private void stopCrtmpserver() throws InterruptedException {
		crtmpserver.destroy();
		crtmpserver.waitFor();
		crtmpstdout.interrupt();
		crtmpstderr.interrupt();
	}

	/**
	 * Starts a ffplay process. The streaming address must be the same that the
	 * one specified in the crtmpserver configuration file - usually default
	 * "rtmp://localhost/AppTm4l"
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void startFfplay() throws IOException, InterruptedException {
		ffplay = Runtime.getRuntime().exec(
				new String[] { playerName, "-probesize", "3000",
						"-window_title", "Visor Microscopio", 
						"rtmp://localhost/" + CamServer.streamName });
		
		ProgressFrame progressFrame = new ProgressFrame("Iniciando visor","Iniciando visor...");
		progressFrame.start();
		
		ffplaystdout = new DefaultStreamGobbler(ffplay.getInputStream(), "OUTPUT",
				false, "ffplay_out");
		ffplaystderr = new FfplayStreamGobbler(ffplay.getErrorStream(), "ERROR",
				false, "ffplay_err");

		synchronized (ffplaystderr) {
			ffplaystderr.start();
			ffplaystdout.start();

			try {
			ffplaystderr.wait();
			} catch (InterruptedException e){
				progressFrame.stop();
				throw new InterruptedException();
			}
			
			progressFrame.stop();
		}
		ffplayActive = true;
	}

	/**
	 * Stops the ffplay process
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void stopFfplay() throws IOException, InterruptedException {
		ffplay.destroy();
		ffplay.waitFor();
		ffplaystdout.interrupt();
		ffplaystderr.interrupt();

		ffplayActive = false;
	}

	/**
	 * This method checks if the capture that has been notified is a local
	 * capture or not by checking the method CamServer.isLocalCapture(). If so,
	 * it shows a "save file" menu and starts a work session.
	 * 
	 * @throws IOException
	 */
	private void checkLocalCapture() throws IOException {

		synchronized (CamServer.LockLocal) {
			try {
				if (CamServer.isLocalCapture()) {
	
					CamServer.setLocalCapture(false);
	
					BufferedImage currentImage = getCurrentImage();
	
					CamServer.logger.info(MyLogger.getLogRecord("LOCAL_CAPTURE"));
	
					ImagePlus imp2 = WindowManager.getCurrentImage();
					ImagePlus imagen = new ImagePlus("title", Toolkit
							.getDefaultToolkit().createImage(
									currentImage.getSource()));
					if (imp2 != null) {
	
						ImageStack stack = imp2.getStack();
						if (stack.getSize() == 1) {
							String label = stack.getSliceLabel(1);
							if (label != null && label.indexOf("\n") != -1)
								stack.setSliceLabel(null, 1);
							Object obj = imagen.getProperty("Label");
							if (obj != null && (obj instanceof String))
								stack.setSliceLabel((String) obj, 1);
						}
	
						stack.addSlice(null, imagen.getChannelProcessor());
						imp2.setStack(null, stack);
						imp2.setSlice(stack.getSize());
						imp2.unlock();
	
						ImageManager.getInstance().refresh();
	
					} else {
						saver.saveAsTifInZip(imagen, true);
					}
				}
			} catch (Exception e){
				if (e.getMessage().contains("Dimensions do not match")){
					// Error when trying to add a slide to the stack. The image dimensions do not match
					JOptionPane.showMessageDialog(null, 
							"No se ha podido añadir la imagen a la pila de imágenes\n" +
							"porque los tamaños son diferentes.",
							"Error al añadir imagen", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	/**
	 * This method checks if the capture that has been notified is an external
	 * capture or not by checking the method CamServer.getExternalCapture(). If
	 * so, the current image is sent to the client.
	 */
	private void checkExternalCapture() {
		DataOutputStream outStream;
		synchronized (CamServer.LockExternal) {
			if (CamServer.getExternalCapture() != null) {

				outStream = CamServer.getExternalCapture();
				// Clear the DataOutputStream
				CamServer.clearExternalCapture();

				BufferedImage currentImage;
				try {
					currentImage = getCurrentImage();
					CamServer.logger.info(MyLogger
							.getLogRecord("EXTERNAL_CAPTURE"));
					ClientConnection.sendCurrentImage(outStream, currentImage);
				} catch (IOException e) {
					System.out.println("Fallo al capturar o enviar la imagen");
					e.printStackTrace();
				}
			}
			CamServer.LockExternal.notify();
		}
	}

	/**
	 * This method return the current image with the parameters saved in the
	 * CamServer static instance (device, width, height).
	 * 
	 * @return - the current image
	 * @throws IOException
	 */
	private BufferedImage getCurrentImage() throws IOException {

		ProgressFrame progressFrame = new ProgressFrame("Capturando", "Capturando...");
		try {
			stopFfmpeg();
			stopCrtmpserver();

			progressFrame.start();

			Process capture = Runtime.getRuntime().exec(
					new String[] {
							encoderName,
							"-y",
							"-s",
							CamServer.captureWidth + "x"
									+ CamServer.captureHeight, "-f",
							"video4linux2", "-i",
							CamServer.getVideoDevice().getDevicefile(),
							"-vframes", "1", "-qmin", captureQuality, "-qmax",
							captureQuality, capturePath });
			capture.waitFor();

			BufferedImage currentImage = null;
			currentImage = ImageIO.read(new File(capturePath));

			startCrtmpserver(serverConfigFile);
			startFfmpeg();

			if (ffplayActive) {
				captureTaken = true;
				stopFfplay();
			}
			progressFrame.stop();

			return currentImage;

		} catch (Exception e) {
			progressFrame.stop();
			e.printStackTrace();
		}
		return null;
	}

	private int getUnixPID(Process process) throws Exception {

		if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
			Class cl = process.getClass();
			Field field = cl.getDeclaredField("pid");
			field.setAccessible(true);
			Object pidObject = field.get(process);
			return (Integer) pidObject;
		} else {
			throw new IllegalArgumentException("Needs to be a UNIXProcess");
		}
	}

	
	@Override
	public String getHeadHTMLText() {
		String headHTML = "<script type=\"text/javascript\" src=\"../flowplayer-3.2.13.min.js\"></script>";
		return headHTML;
	}
	
	
	@Override
	public String getBodyHTMLText() {
		String bodyHTML = "<object class=\'stream\' id=\'player\'>"
				+ "</object>"
				+ "<script>"
				// Create the rtmp server ip
				+ "var serverip = \'rtmp://\' + location.host.split(\':\')[0] + \'/\';"
				// Create the flowplayer instance
				+ "flowplayer(\"player\", \"../flowplayer-3.2.17.swf\", {"
				+ "buffering: false," + "clip: {" + "url: \'" + CamServer.streamName + "\',"
				+ "autoPlay: true," + "live: true," + "bufferLength: 0,"
				//+ "controls: null," 
				+ "provider: \'crtmpserver\',"
				+ "scaling: \'fit\'" + "}," + "plugins: {" + "crtmpserver: {"
				+ "url: \'flowplayer.rtmp-3.2.13.swf\',"
				+ "netConnectionUrl: serverip" + "}," + "controls: {" 
				+ "url: \'flowplayer.controls-3.2.16.swf\', right: 0, width: \'10%\', height: \'10%\', all: false, fullscreen:true" 
				+ "}}});" + "</script>";

		return bodyHTML;
	}
	
	
	/**
	@Override
	public String getHeadHTMLText() {
		String headHTML = "<link href=\"video-js.min.css\" rel=\"stylesheet\">" +
				"<script type=\"text/javascript\" src=\"video.js\"></script>";
		return headHTML;
	}

	@Override
	public String getBodyHTMLText() {
		String bodyHTML = "<object class=\'stream\'><video class=\'video-js vjs-default-skin\' id=\'video1\' width=\"100%\" height=\"100%\"" +
				"data-setup=\'{\"controls\": true, \"autoplay\": true, \"preload\":\"none\"}\'>" +
				"<script>var serverip = \'<source src=\"rtmp://\' + location.host.split(\':\')[0] + \'&AppTm4l\" type=\"rtmp/flv\">\';"
				+ "document.write(serverip);" + "</script>" +
				"</video></object>";
		return bodyHTML;
	}
	*/
	
	/**
	 * Remove a client from the client list, and return the connection time.
	 * @param client Ip and port of the client that has disconnected
	 * @return Client connection time.
	 */
	private void logClientDisconnection (String client){
		if (!(clients.get(client) == null)){
			long time = (System.currentTimeMillis() - clients.get(client)) /1000;
			
			CamServer.logger.info(MyLogger.getLogRecord("VIDEO_STREAMING_FINISH_CLIENT")
					+ " " + client);
			
			CamServer.logger.info(MyLogger.getLogRecord("VIDEO_STREAMING_CONNECTION_TIME")
					+ " " + time);
			
		}
	}
	
	/**
	 * Modifies the server configuration file in order to match the streamName variable created by CamServer.
	 */
	private void updateStreamName(){
		String temp_file = "crtmpserver.lua.temp";
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new FileReader(serverConfigFile));
			bw = new BufferedWriter(new FileWriter(temp_file));
			
			String line;
			while ((line = br.readLine()) != null){
				
				if(line.contains("localStreamName") || line.contains("targetStreamName")){
					line = line.replaceAll("localStreamName=\".*\"", "localStreamName=\"" + CamServer.streamName + "\"");
					line = line.replaceAll("targetStreamName=\".*\"", "targetStreamName=\"" + CamServer.streamName + "\"");
				}
				bw.write(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				if(br != null){
					br.close();
				}
				if (bw != null){
					bw.close();
				}
			} catch (IOException e){
			}
		}
		
		File oldFile = new File(serverConfigFile);
		oldFile.delete();
		
		File newFile = new File(temp_file);
		newFile.renameTo(oldFile);
		
		streamNameUpdated = true;
	}
	
	private class FfmpegStreamGobbler extends DefaultStreamGobbler {
		
		public FfmpegStreamGobbler (InputStream is, String type, boolean print, String name){
			super(is,type,print,name);
		}
		
		@Override
		public void analyzeLine (String line){
			if (line.startsWith("frame=")) {
				synchronized (this) {
					notify();
				}
			}
		}
	}
	
	
	private class CrtmpStreamGobbler extends DefaultStreamGobbler {
		
		public CrtmpStreamGobbler (InputStream is, String type, boolean print, String name){
			super(is,type,print,name);
		}
		
		@Override
		protected void analyzeLine (String line){
			if (line.contains("GO! GO! GO!")) {
				synchronized (this) {
					notify();
					return;
				}
			}
			if (line.contains("Inbound connection accepted")){
				String clientIPandPort = line.split("Far:")[1].split(";")[0];
				
				// Save a register in clients variable
				synchronized (clients){
					clients.put(clientIPandPort, System.currentTimeMillis());
				}
				
				CamServer.logger.info(MyLogger.getLogRecord("VIDEO_STREAMING_START_CLIENT")
						+ " " + clientIPandPort);
				return;
			}
			if (line.contains("Unable to read data from connection")){
				String clientIP = line.split("Far:")[1].split(";")[0];
				
				synchronized (clients){
					logClientDisconnection(clientIP);
					clients.remove(clientIP);
				}
				
				return;				
			}
		}
	}
	
	private class FfplayStreamGobbler extends DefaultStreamGobbler {
		
		public FfplayStreamGobbler (InputStream is, String type, boolean print, String name){
			super(is,type,print,name);
		}
		
		@Override
		protected void analyzeLine (String line){
			if (line.contains("Input #0")) {
				synchronized (this) {
					notify();
				}
			}
		}
	}
	
}
