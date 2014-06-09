package video;

import file.edition.ImageManager;
import file.edition.RoiManager;
import file.io.TPSaver;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import communication.CamServer;
import communication.ClientConnection;


import util.DefaultStreamGobbler;
import util.MyLogger;

/**
 * 
 * @author ehas
 * 
 */
public class VLCThread implements Streamer {

	private String encoderName = "cvlc";
	private String capturerName = "ffmpeg";
	private String playerName = "ffplay";

	private Process vlc = null;
	private Process avplay;
	private DefaultStreamGobbler vlcstdout, vlcstderr, avplaystdout, avplaystderr;

	private boolean ffplayActive = false;
	private boolean captureTaken = false;
	private TPSaver saver = new TPSaver();

	private String capturePath = "capture.jpg";
	private String captureQuality = "2";

	/**
	 * It starts the video streaming by running vlc process. When the streaming
	 * is initialized, it waits for a image capture notification.
	 */
	@Override
	public void run() {

		// Run avconv and avserver
		try {

			// Start the libav processes

			synchronized (this) {
				startVLC();
				this.notify();
			}

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
							startAvplay();
							avplay.waitFor();
							ffplayActive = false;
						} catch (InterruptedException | IOException e) {
							avplay.destroy();
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

			stopVLCProcesses();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method must be called before the thread exit. It destroys all
	 * remaining processes.
	 */
	private void stopVLCProcesses() {
		System.out.println("Parando hilo vlc");

		// Stop the vlc processes
		try {
			stopVLC();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (avplay != null) {
			try {
				stopAvplay();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Starts an avconv process with the parameters saved in the CamServer
	 * static instance. Two additional processes are created to read standard
	 * and error output. The methods waits until the avconv process actually
	 * starts the video streaming
	 * 
	 * @throws IOException
	 */
	private void startVLC() throws IOException {

		String command = encoderName;
		command = command.concat(" -vv --color");
		command = command.concat(" v4l://").concat(
				CamServer.getVideoDevice().getDevicefile());
		// command = command.concat(":width=" + CamServer.videoWidth);
		// command = command.concat(":height=" + CamServer.videoHeight);
		command = command.concat(":size=" + CamServer.videoWidth + "x"
				+ CamServer.videoHeight);
		command = command.concat(":fps=" + CamServer.getFramerate());
		command = command.concat(" --sout");
		// Start of x264 parameters definition
		command = command.concat(" #transcode{venc=x264{");
		// Obtain the bps and keep it in a variable
		int bps = Integer.parseInt(CamServer.bps.split("k")[0]);
		command = command.concat("vbv-maxrate=" + bps + ",");
		command = command.concat("vbv-bufsize=" + (2 * bps) + ",");
		command = command.concat("fps=" + CamServer.getFramerate() + ",");
		command = command.concat("keyint=" + CamServer.gop + ",");
		command = command.concat("profile=high,");
		command = command.concat("tune=zerolatency");
		command = command.concat("},");
		// End of x264 definition
		command = command.concat("vcodec=h264,");
		command = command.concat("width=" + CamServer.videoWidth + ",");
		command = command.concat("height=" + CamServer.videoHeight + ",");
		command = command.concat("acodec=none}");
		command = command
				.concat(":std{access=http{mime/x-flv},dst=:8090/stream.flv}");
		command = command.concat(" --sout-keep");
		command = command.concat(" --no-sout-audio");
		System.out.println(command);

		vlc = Runtime.getRuntime().exec(command);

		vlcstdout = new DefaultStreamGobbler(vlc.getInputStream(), "OUTPUT", false,
				"vlc_out");
		vlcstdout.start();
		vlcstderr = new VLCStreamGobbler(vlc.getErrorStream(), "ERROR", true,
				"vlc_err");
		vlcstderr.start();

		// Wait until the video streaming is started
		synchronized (vlcstderr) {
			try {
				vlcstderr.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isStreamerReady() throws Exception{
		//TODO 
		return true;
	}

	/**
	 * Stops the vlc process
	 * 
	 * @throws Exception
	 */
	private void stopVLC() throws Exception {

		// It is necessary to kill the process in this way to be sure that
		// SIGTERM (15) is sent
		// as the kill signal.
		String pid = "" + getUnixPID(vlc);
		// TODO change to signal -15 if -9 is not necessary
		Process killingProcess = Runtime.getRuntime().exec(
				new String[] { "kill", "-9", pid });
		killingProcess.waitFor();
		vlc.waitFor();
		vlcstdout.interrupt();
		vlcstderr.interrupt();
	}

	/**
	 * Starts a avplay process. The streaming address must be the same that the
	 * one specified in the avserver configuration file - usually default
	 * "http://localhost:8090/test.swf"
	 * 
	 * @throws IOException
	 */
	public void startAvplay() throws IOException {
		avplay = Runtime.getRuntime().exec(
				new String[] { playerName, "-probesize", "3000",
						"http://localhost:8090/stream.flv" });
		avplaystdout = new DefaultStreamGobbler(avplay.getInputStream(), "OUTPUT",
				false, "avplay_out");
		avplaystdout.start();
		avplaystderr = new DefaultStreamGobbler(avplay.getErrorStream(), "ERROR",
				false, "avplay_err");
		avplaystderr.start();

		ffplayActive = true;
	}

	/**
	 * Stops the avplay process
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void stopAvplay() throws IOException, InterruptedException {
		avplay.destroy();
		avplay.waitFor();
		avplaystdout.interrupt();
		avplaystderr.interrupt();

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

		try {
			stopVLC();

			JFrame progressFrame = new JFrame("Capturando...");
			progressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			Container content = progressFrame.getContentPane();
			JProgressBar progress = new JProgressBar();
			progress.setIndeterminate(true);
			Border border = BorderFactory.createTitledBorder("Capturando...");
			progress.setBorder(border);
			content.add(progress, BorderLayout.NORTH);
			GraphicsEnvironment graphics = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			progressFrame.setLocation(graphics.getCenterPoint());
			progressFrame.setSize(300, 70);
			progressFrame.setVisible(true);

			Process capture = Runtime.getRuntime().exec(
					new String[] {
							capturerName,
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

			startVLC();

			if (ffplayActive) {
				captureTaken = true;
				stopAvplay();
			}
			progressFrame.setVisible(false);
			progressFrame.dispose();

			return currentImage;

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
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
		String headHTML = "<link href=\"video-js.min.css\" rel=\"stylesheet\">"
				+ "<script src=\"video.js\"></script>";
		String headHTML2 = "<script type=\"text/javascript\">"
				+ "string1 = \"<source type=\'video/x-flv\' src=\'http://\";"
				+ "string2 = location.host.split(\':\')[0];"
				+ "string3 = \":8090/stream.flv\'> <\\/source>\";"
				+ "streamurl = string1 + string2 + string3;" + "</script>";
		return headHTML + headHTML2;
	}

	@Override
	public String getBodyHTMLText() {
		String bodyHTML = "<div class=\'stream\'><video id=\'video1\' class=\'video-js vjs-default-skin\'"
				+ "width=\"640\" height=\"480\" "
				+ "data-setup='{\"controls\" : false, \"autoplay\" : true, \"preload\" : \"auto\"}'>"
				+ "<script type=\"text/javascript\">"
				+ "document.write(streamurl);" + "</script>" + "</video></div>";
		return bodyHTML;
	}
	
	public class VLCStreamGobbler extends DefaultStreamGobbler {
		
		public VLCStreamGobbler (InputStream is, String type, boolean print, String name){
			super(is,type,print,name);
		}
		
		@Override
		protected void analyzeLine (String line){
			if (line.contains("avformat mux debug: writing header")) {
				synchronized (this) {
					notify();
				}
			}
		}
	}

}