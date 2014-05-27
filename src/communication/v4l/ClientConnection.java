/*
 * Copyright (C) 2011 Gilles Gigan (gilles.gigan@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public  License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package communication.v4l;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import util.Dictionary;
import au.edu.jcu.v4l4j.Control;
import au.edu.jcu.v4l4j.ControlList;
import au.edu.jcu.v4l4j.JPEGFrameGrabber;
import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.ControlException;

/**
 * This class contains static methods that respond to incoming connections. There are methods to
 * send the web server main page, a jpeg image, css elements, etc.
 * 
 * @author ehas
 *
 */
public class ClientConnection {
	private static String textHeader = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
	private static String mjpegHeader = "HTTP/1.1 200 OK\r\nExpires: 0\r\nPragma: no-cache\r\nCache-Control: no-cache\r\nContent-Type: multipart/x-mixed-replace;boundary=\"boundary\"\r\n\r\n";
	private static String mjpegFrameheader = "--boundary\r\nContent-Type: image/jpeg\r\nContent-Length: ";
	private static String JPEGImageHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/x-unknown\r\nContent-Length:";
	private static String jsHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/javascript; charset=utf-8\r\n\r\n";
	private static String cssHeader = "HTTP/1.1 200 OK\r\nContent-Type: text/css; charset=utf-8\r\n\r\n";
	private static String swfHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/x-shockwave-flash\r\n\r\n";
	private static String woffHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/font-woff\r\n\r\n";
	private static String ttfHeader = "HTTP/1.1 200 OK\r\nContent-Type: font/ttf\r\n\r\n";
	private static String authHeader = "HTTP/1.1 401 \r\nWWW-Authenticate: Basic realm=\"Acceso al servidor de telemicroscopia.\"\r\n\r\n";

	private static String captureButton = "<form action=\"capture.jpg\">"
			+ "<button class=\"capture\" type=\"submit\" name=\"set\" value=\"capture\">Capturar</button>"
			+ "</form>";

	private static String refreshButton = "<form >"
			+ "<button class=\"refresh\" type=\"submit\" name=\"set\" value=\"refresh\">Actualizar</button>"
			+ "</form>";

	private static String mainPageHTML = "<!DOCTYPE HTML>"
			+ "<html>"
			+ "<head>"
			+ "<style>"
			+ ".control {position:absolute; width:28%;}"
			//+ ".videoOptions {position:absolute; left:30%; height:15%; width:69%; top:5%; border: groove;}"
			+ ".stream {position:absolute; left:30%; top:5%; width:60%; height:90%;}"
			//+ ".zoomIn {position:absolute; left:92%; width:7%; height:5%; top:38%;}"
			//+ ".zoomOut {position:absolute; left:92%; width:7%; height:5%; top:42%;}"
			+ ".capture {position:absolute; left:92%; width:7%; height:5%; top:22%;}"
			+ ".refresh {position:absolute; left:92%; width:7%; height:5%; top:30%;}"
			+ "</style>"
			+ CamServer.streamer.getHeadHTMLText()
			+ "<title>Servidor de telemicroscopia</title>"
			+ "</head>"
			+ "<body>"
			+ "<object class=\"control\" type=\"text/html\" data=\"control\" name=\"control list\" width=\"400\" height=\"600\">"
			+ "</object>"
			//+ "<object class=\"videoOptions\" type=\"text/html\" data=\"videoOptions\" name=\"video options\" width=\"400\" height=\"200\">"
			//+ "</object>"
			+ CamServer.streamer.getBodyHTMLText()
			//+ "<form ><button class=\"zoomIn\" type=\"submit\" name=\"set\" value=\"zoomIn\">Ampliar</button>"
			//+ "<button class=\"zoomOut\" type=\"submit\" name=\"set\" value=\"zoomOut\" disabled=\"disabled\">Reducir</button></form>"
			+ captureButton + refreshButton + "</body>" + "</html>";

	private static String zoomPage = "<!DOCTYPE HTML>"
			+ "<html>"
			+ "<head>"
			+ "<style>"
			+ ".stream {position:absolute; left:2%; top:2%; width:90%; height:95%;}"
			+ ".zoomIn {position:absolute; left:92%; width:7%; height:5%; top:30%;}"
			+ ".zoomOut {position:absolute; left:92%; width:7%; height:5%; top:36%;}"
			+ ".capture {position:absolute; left:92%; width:7%; height:5%; top:22%;}"
			+ ".refresh {position:absolute; left:92%; width:7%; height:5%; top:44%;}"
			+ "</style>"
			+ CamServer.streamer.getHeadHTMLText()
			+ "<title>Servidor de telemicroscopia</title>"
			+ "</head>"
			+ "<body>"
			+ CamServer.streamer.getBodyHTMLText()
			+ "<form ><button class=\"zoomIn\" type=\"submit\" name=\"set\" value=\"zoomIn\" disabled=\"disabled\">Ampliar</button>"
			+ "<button class=\"zoomOut\" type=\"submit\" name=\"set\" value=\"zoomOut\">Reducir</button></form>"
			+ captureButton + refreshButton + "</body>" + "</html>";

	private static String mainPageHTMLOld = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\""
			+ " http://www.w3.org/TR/html4/frameset.dtd>"
			+ "<html>"
			+ "<head>"
			+ "<title>Servidor de telemicroscopía</title>"
			+ "</head>"
			+ "<frameset cols=\"4*,6*\">"
			+ "<frame src=\"control\" name=\"control list\">"
			+ "<frame src=\"webcam\" name=\"video stream\">"
			+ "</frameset>"
			+ "</html>";

	private static String webcamPageHTML = "<html>" + "<body>" + "<table>"
			+ "<td>" + "<tr>" + "<img src=\"stream.jpg\">" + "</tr>" + "</td>"
			+ "</table>" + "</body>" + "</html>";

	private static String controlPageHTMLHeader = "<html>" + "<body>"
			+ "<table>";

	private static String controlPageHTMLFooter = "</table>" + "</body>"
			+ "</html>";

	private Socket clientSocket;
	private BufferedReader inStream;
	private DataOutputStream outStream;

	/**
	 * Builds an object handling a tcp connection to one client. Sends the MJPEG
	 * header straight away
	 * 
	 * @param client
	 *            the client who just connected to us
	 * @param in
	 *            the input stream
	 * @param out
	 *            the ouput stream
	 * @throws IOException
	 *             if there is an error get in/output streams
	 */
	public ClientConnection(Socket client, BufferedReader in,
			DataOutputStream out) throws IOException {
		if ((client == null) || (in == null) || (out == null))
			throw new NullPointerException("client, in and out cannot be null");

		clientSocket = client;
		inStream = in;
		outStream = out;

		// send mjpeg header
		outStream.writeBytes(mjpegHeader);
	}

	/**
	 * Close the input and output streams and closes the socket
	 */
	public void stop() {
		try {
			System.out.println("Disconnected from "
					+ clientSocket.getInetAddress().getHostAddress() + ":"
					+ clientSocket.getPort());

			inStream.close();
			outStream.close();
			clientSocket.close();
		} catch (IOException e) {
			// error closing connection with client
			e.printStackTrace();
		}
	}

	/**
	 * Send the given frame in an mpjeg frame header
	 * 
	 * @param frame
	 *            the frame to be send
	 * @throws IOException
	 *             if there is an error writing over the socket
	 */
	public void sendNextFrame(VideoFrame frame) throws IOException {
		outStream.writeBytes(mjpegFrameheader
				+ Integer.toString(frame.getFrameLength()) + "\r\n\r\n");
		outStream.write(frame.getBytes(), 0, frame.getFrameLength());
	}

	/**
	 * Send the given frame in an mpjeg frame header
	 * 
	 * @param frame
	 *            the frame to be send
	 * @throws IOException
	 *             if there is an error writing over the socket
	 */
	public void sendNextFrameBufferedImage(BufferedImage frame)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(frame, "jpg", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		outStream.writeBytes(mjpegFrameheader
				+ Integer.toString(imageInByte.length) + "\r\n\r\n");
		outStream.write(imageInByte, 0, imageInByte.length);
	}

	/**
	 * Send the main page in html over the given output stream
	 * 
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             if the stream is closed
	 */
	public static void sendMainPage(DataOutputStream out)
			throws IOException {
		out.writeBytes(textHeader);
		out.writeBytes(mainPageHTML);
	}

	public static void sendZoomPage(DataOutputStream out) throws IOException {
		out.writeBytes(textHeader);
		out.writeBytes(zoomPage);
	}

	public static void sendWebcamPage(DataOutputStream out)
			throws IOException {
		out.writeUTF(textHeader);
		out.writeUTF(webcamPageHTML);
		
	}

	public static void sendVideoOptionsPage(DataOutputStream out)
			throws IOException {
		out.writeBytes(textHeader);
		out.writeBytes("<html>");
		out.writeBytes("<head><meta http-equiv=\'Content-Type\' content=\'text/html; charset=iso-8859-1\' /> </head>");
		out.writeBytes("<body><table>");
		out.writeBytes("<tr><td>");
		out.writeUTF("Opciones de vídeo:</td>");
		out.writeBytes("<tr><td>");
		out.writeUTF("Resolución:</td>");
		out.writeBytes("</td><td><form action=\"updateVideoOption\">");
		out.writeBytes("<input type=\"hidden\" name=\"id\" value=\"videoResolution\">");
		String selectedResolution = CamServer.videoWidth + "x"
				+ CamServer.videoHeight;
		if (!Settings.needFallbackMode(CamServer.videoDevice.getDevicefile())) {
			out.writeBytes("<select name=\"val\" size=\"1\">");
			DiscreteResolution[] resolutions = Settings
					.listAvailableResolutions(CamServer.getVideoDevice()
							.getDevicefile());

			for (DiscreteResolution resolution : resolutions) {
				String name = resolution.width + "x" + resolution.height;
				out.writeUTF("<option value=\"" + name + "\"");
				if (name.equalsIgnoreCase(selectedResolution)) {
					out.writeBytes(" selected=\"selected\"");
				}
				out.writeBytes(" >");
				out.writeUTF(name);
				out.writeBytes("</option>");
			}
			out.writeBytes("</select>");
		} else {
			out.writeBytes("<input type=\"text\" name=\"val\" value=\""
					+ selectedResolution + "\" size=\"10\">");
		}
		out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"Ok\"></form></td>");

		out.writeBytes("<td><object width=\"50\" height=\"0\" ></object></td>");

		out.writeBytes("<td>Ancho de banda: </td>");
		out.writeBytes("<td><form action=\"updateVideoOption\" oninput=\"current.value=val.value + ' kbps'\">");
		out.writeBytes("<input type=\"hidden\" name=\"id\" value=\"anchoBanda\" >");
		String currentbps = CamServer.bps.split("k")[0];
		out.writeBytes("<input type=\"range\" name=\"val\" value=\""
				+ currentbps + "\" min=\"128\" max=\"2048\" step=\"64\" >");
		out.writeBytes("<output name=\"current\">" + currentbps + " kbps"
				+ "</output>");
		out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"Ok\"></form></td>");
		out.writeBytes("</table></body></html>");

	}

	public static void updateVideoOption(String httpLine, DataOutputStream out)
			throws IOException {
		String id = null;
		String value = null;

		StringTokenizer tokens = new StringTokenizer(httpLine, "?=&", false);

		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();

			if ((next.equalsIgnoreCase("id")) && tokens.hasMoreTokens()) {
				id = tokens.nextToken();
			} else if ((next.equalsIgnoreCase("val")) && tokens.hasMoreTokens()) {
				value = tokens.nextToken();
			}
		}

		if (id.equalsIgnoreCase("videoResolution")) {
			CamServer.videoWidth = Integer.parseInt(value.split("x")[0]);
			CamServer.videoHeight = Integer.parseInt(value.split("x")[1]);
		} else if (id.equalsIgnoreCase("anchoBanda")) {
			CamServer.bps = value + "k";
		}

		if (CamServer.active) {
			CamServer.refreshStreaming();
			// TODO send refresh page to all clients
			sendRefreshPage(out);
		}

	}

	/**
	 * Send a basic HTML table containing a form per control allowing the user
	 * to view the current value and it update it.
	 * 
	 * @param ctrlList
	 *            the list of control for which the HTML form should be created
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             if there is an error writing out the stream
	 */
	public static void sendControlListPage(ControlList ctrlList,
			DataOutputStream out) throws IOException {
		out.writeBytes(textHeader);
		out.writeBytes(controlPageHTMLHeader);

		/**
		// add a fake control to adjust the jpeg quality
		out.writeBytes("<tr>");
		out.writeBytes("<td>Calidad JPEG</td>");
		out.writeBytes("<td><form action=\"updateControl\">");
		out.writeBytes("<input type=\"hidden\" name=\"id\" value=\"-1\">");
		out.writeBytes("<input type=\"text\" name=\"val\" value=\""
				+ communication.v4l.CamServer.getJpegQuality()
				+ "\" size=\"10\" maxlength=\"10\">");
		out.writeBytes("<br>Min: 0 - Max: 100 - Intervalo: 1");
		out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"set\"></form></td></tr>");
		*/
		
		// for each control, create an entry in the table
		for (Control control : ctrlList.getList()) {
			out.writeBytes("<tr><td>");
			// AQP
			out.writeUTF(Dictionary.translateWord((control.getName()))
					+ "</td>");
			out.writeBytes("<td><form action=\"updateControl\">");
			out.writeBytes("<input type=\"hidden\" name=\"id\" value=\""
					+ ctrlList.getList().indexOf(control) + "\">");

			try {
				// Select the best HTML element to represent the control based
				// on its type
				switch (control.getType()) {
				case V4L4JConstants.CTRL_TYPE_BUTTON:
					out.writeBytes("<input type=\"hidden\" name=\"val\" value=\"0\">");
					out.writeBytes("</td><td><input type=\"submit\" name=\"Activate\">");
					break;

				case V4L4JConstants.CTRL_TYPE_SLIDER:
					out.writeBytes("<input type=\"text\" name=\"val\" value=\""
							+ control.getValue()
							+ "\" size=\"10\" maxlength=\"10\">");
					out.writeBytes("<br>Min: " + control.getMinValue()
							+ " - Max: " + control.getMaxValue() + " - Step: "
							+ control.getStepValue());
					out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"Ok\">");
					break;

				case V4L4JConstants.CTRL_TYPE_DISCRETE:
					out.writeBytes("<select name=\"val\" size=\"1\">");
					Map<String, Integer> valueMap = control
							.getDiscreteValuesMap();
					for (String name : valueMap.keySet()) {
						out.writeBytes("<option value=\"" + valueMap.get(name)
								+ "\"");
						if (control.getValue() == valueMap.get(name).intValue())
							out.writeBytes(" selected=\"selected\"");
						out.writeBytes(" >");
						out.writeUTF(name);
						out.writeBytes("</option>");
					}
					out.writeBytes("</select>");
					out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"Ok\">");
					break;

				case V4L4JConstants.CTRL_TYPE_SWITCH:
					out.writeBytes("<input type=\"checkbox\" name=\"val\" value=\"");
					if (control.getValue() == 1)
						out.writeBytes("0\" checked=\"checked\">");
					else
						out.writeBytes("1\">");
					// out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"set\">");
					out.writeBytes("</td><td><input type=\"submit\" name=\"set\" value=\"Ok\">");
					break;
				}
			} catch (Exception e) {
				// error creating form
			}

			out.writeBytes("</form></td>");
			out.writeBytes("</tr>");
		}

		out.writeBytes(controlPageHTMLFooter);
	}

	/**
	 * Parses the given http line, expecting to find something along the lines
	 * of <code>GET /control?id=ID&val=VAL&submit=set HTTP/1.1</code> where ID
	 * and VAL are integers.
	 * 
	 * @param ctrlList
	 *            the control list
	 * @param httpLine
	 *            the http line to be parsed
	 * @throws ControlException
	 *             if there is an error setting the new value
	 * @throws IOException
	 */
	public static void updateControlValue(ControlList ctrlList,
			JPEGFrameGrabber fg, String httpLine, DataOutputStream outStream)
			throws ControlException, IOException {
		boolean hasValue = false;
		boolean hasID = false;
		int controlID = 0;
		int value = 0;

		// parse the http line to find out the control index and
		// its new value. Expected line:
		// "GET /control?id=ID&val=VAL&submit=set HTTP/1.1"
		StringTokenizer tokens = new StringTokenizer(httpLine, "?=&", false);

		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();

			if ((next.equalsIgnoreCase("id")) && tokens.hasMoreTokens()) {
				try {
					controlID = Integer.parseInt(tokens.nextToken());
					hasID = true;
				} catch (NumberFormatException e) {
					// control id is not a number, ignore
				}
			} else if ((next.equalsIgnoreCase("val")) && tokens.hasMoreTokens()) {
				try {
					value = Integer.parseInt(tokens.nextToken());
					hasValue = true;
				} catch (NumberFormatException e) {
					// control value is not a number, ignore
				}
			}
		}

		// HTML checkboxes dont return a value if unchecked, which means
		// hasValue is false in this case. Check if ID is of type SWICTH
		// and if hasValues == false, in which case, set it to true,
		// and use default value of 0
		if (hasID
				&& !hasValue
				&& (ctrlList.getList().get(controlID).getType() == V4L4JConstants.CTRL_TYPE_SWITCH)) {
			hasValue = true;
			value = 0;
		}

		// Set new value
		if (hasValue && hasID) {
			// catch the jpeg quality control which is not a real control
			
			if (ctrlList.getList().get(controlID).getType() == V4L4JConstants.CTRL_TYPE_SLIDER) {
				if (value < ctrlList.getList().get(controlID).getMinValue()
						|| value > ctrlList.getList().get(controlID)
								.getMaxValue()) {

					// Value out of range
					String message = "El valor introducido para "
							+ Dictionary.translateWord((ctrlList.getList()
									.get(controlID).getName()))
							+ " no es correcto. ";
					message = message.concat("El valor debe estar entre "
							+ ctrlList.getList().get(controlID)
									.getMinValue()
							+ " y "
							+ ctrlList.getList().get(controlID)
									.getMaxValue());
					sendWarningMessage(outStream, message);
					return;
				}
			}
			ctrlList.getList().get(controlID).setValue(value);
		
		}
	}

	/**
	 * This method send a warning message with text 'message' and refreshes the
	 * webpage
	 * 
	 * @param outStream
	 * @param message
	 * @throws IOException
	 */
	public static void sendWarningMessage(DataOutputStream out, String message)
			throws IOException {
		out.writeBytes(textHeader);
		out.writeBytes("<HTML>");
		out.writeBytes("<HEAD>");
		out.writeBytes("<TITLE>window.alert() Method</TITLE>");
		out.writeBytes("</HEAD>");
		out.writeBytes("<BODY>");
		out.writeBytes("<SCRIPT LANGUAGE=\"JavaScript\">");
		out.writeBytes("alert(\"" + message + "\");");
		out.writeBytes("string1 = \"http://\";");
		out.writeBytes("string2 = location.host.split(\'/\')[0];");
		out.writeBytes("redirect = string1 + string2;");
		out.writeBytes("top.location.href = redirect;");
		out.writeBytes("</SCRIPT>");
		out.writeBytes("</BODY>");
		out.writeBytes("</HTML>");

	}

	/**
	 * Send the current image to an external viewer.
	 * 
	 * @param outStream DataOutputStream of the client.
	 * @param frame BufferedImage to send to the client.
	 * @throws IOException
	 */
	public static void sendCurrentImage(DataOutputStream outStream,
			BufferedImage frame) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(frame, "jpg", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		outStream.writeBytes(JPEGImageHeader
				+ Integer.toString(imageInByte.length) + "\r\n\r\n");
		outStream.write(imageInByte, 0, imageInByte.length);

	}

	public static void sendRefreshPage(DataOutputStream outStream)
			throws IOException {
		outStream.writeBytes(textHeader);
		outStream.writeBytes("<HTML>");
		outStream.writeBytes("<BODY>");
		outStream.writeBytes("<SCRIPT LANGUAGE=\"JavaScript\">");
		outStream.writeBytes("string1 = \"http://\";");
		outStream.writeBytes("string2 = location.host.split(\'/\')[0];");
		outStream.writeBytes("redirect = string1 + string2;");
		outStream.writeBytes("top.location.href = redirect;");
		outStream.writeBytes("</SCRIPT>");
		outStream.writeBytes("</BODY>");
		outStream.writeBytes("</HTML>");
	}

	public static void sendCurrentImage(DataOutputStream outStream,
			VideoFrame frame) throws IOException {
		outStream.writeBytes(JPEGImageHeader
				+ Integer.toString(frame.getFrameLength()) + "\r\n\r\n");
		outStream.write(frame.getBytes(), 0, frame.getFrameLength());
	}

	/**
	 * Send a javascript file the client.
	 * @param file Path to javascript (js) file.
	 * @param outStream DataOutputStream of the client.
	 * @throws IOException
	 */
	public static void sendJs(String file, DataOutputStream outStream)
			throws IOException {
		Path path = Paths.get(file);
		outStream.writeBytes(jsHeader);
		byte[] fileBytes = Files.readAllBytes(path);
		outStream.write(fileBytes);
	}

	/**
	 * Send a css file to the client.
	 * @param file Path to the css file.
	 * @param outStream DataOutputStream of the client.
	 * @throws IOException
	 */
	public static void sendCss(String file, DataOutputStream outStream)
			throws IOException {
		Path path = Paths.get(file);
		outStream.writeBytes(cssHeader);
		byte[] fileBytes = Files.readAllBytes(path);
		outStream.write(fileBytes);
	}

	/**
	 * Send a swf file to the client.
	 * @param file Path to the swf file.
	 * @param outStream DataOutputStream of the client.
	 * @throws IOException
	 */
	public static void sendSwf(String file, DataOutputStream outStream)
			throws IOException {
		Path path = Paths.get(file);
		outStream.writeBytes(swfHeader);
		byte[] fileBytes = Files.readAllBytes(path);
		outStream.write(fileBytes);
	}
	
	/**
	 * Send a woff file to the client.
	 * @param file Path to the woff file.
	 * @param outStream DataOutputStream of the client.
	 * @throws IOException
	 */
	public static void sendWoff(String file, DataOutputStream outStream) throws IOException{
		Path path = Paths.get(file);
		outStream.writeBytes(woffHeader);
		byte[] fileBytes = Files.readAllBytes(path);
		outStream.write(fileBytes);
	}
	
	/**
	 * Send a ttf file to the client.
	 * @param file Path to the ttf file.
	 * @param outStream DataOutputStream of the client.
	 * @throws IOException
	 */
	public static void sendTtf(String file, DataOutputStream outStream) throws IOException{
		Path path = Paths.get(file);
		outStream.writeBytes(ttfHeader);
		byte[] fileBytes = Files.readAllBytes(path);
		outStream.write(fileBytes);
	}

	public static void sendAuthPage (DataOutputStream outStream) throws IOException {
		outStream.writeBytes(authHeader);
	}
}
