package communication;

import edition.ImageManager;
import edition.RoiManager;
import gestion.GestorSave;
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

import util.MyLogger;

/**
 * 
 * @author ehas
 * 
 */
public class LibavThread implements Streamer {

	private String encoderName = "ffmpeg";
	private String serverName = "ffserver";
	private String playerName = "ffplay";

	private Process avconv = null;
	private Process avserver;
	private Process avplay;
	private DefaultStreamGobbler avconvstdout, avconvstderr, avserverstdout,
			avserverstderr, avplaystdout, avplaystderr;

	private boolean ffplayActive = false;
	private boolean captureTaken = false;
	private GestorSave saver = new GestorSave();

	private String capturePath = "capture.jpg";
	private String captureQuality = "2";

	private String avserverTempFilePath = "feed1.ffm";
	private String avserverConfigFile = "server.conf";

	/**
	 * It starts the video streaming by running avserver and avconv processes.
	 * When the streaming is initialized, it waits for a image capture
	 * notification.
	 */
	@Override
	public void run() {

		// Run avconv and avserver
		try {

			// Start the libav processes
			startAvserver(avserverConfigFile);
			try {
				Thread.sleep(1300);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			synchronized (this) {
				startAvconv();
				this.notify();
			}
			// startFfplay();

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

			stopLibavProcesses();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isStreamerReady() throws Exception{
		//TODO 
		return true;
	}
	
	/**
	 * This method must be called before the thread exit. It destroys all
	 * remaining processes.
	 */
	private void stopLibavProcesses() {
		System.out.println("Parando hilo ffmpeg");

		// Stop the libav processes
		try {
			stopAvconv();
			stopAvserver();
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

		// Remove temporal file
		try {
			Runtime.getRuntime().exec(
					new String[] { "rm", avserverTempFilePath });
		} catch (IOException e) {
			e.printStackTrace();
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
	private void startAvconv() throws IOException {

		String command = encoderName;
		command = command.concat(" -f").concat(" video4linux2");
		command = command.concat(" -video_size").concat(
				" " + CamServer.videoWidth + "x" + CamServer.videoHeight);
		command = command.concat(" -framerate").concat(
				" " + CamServer.getFramerate());
		command = command.concat(" -i").concat(
				" " + CamServer.getVideoDevice().getDevicefile());
		/**
		 * if (true){ command = command.concat(" -vf") .concat(
		 * " \"drawtext=fontfile=/usr/share/fonts/truetype/ttf-dejavu/DejaVuSans-Bold.ttf:"
		 * ) .concat(" text=\'\\%T\':").concat(" fontcolor=white@0.8:").concat(
		 * " x=7:").concat(" y=460\""); }
		 */
		command = command.concat(" -vcodec").concat(" flv");
		if (CamServer.gop > 0) {
			command = command.concat(" -g").concat(" " + CamServer.gop);
		}
		if (CamServer.qmin > 0) {
			command = command.concat(" -qmin").concat(" " + CamServer.qmin);
		}
		if (CamServer.qmax > 0) {
			command = command.concat(" -qmax").concat(" " + CamServer.qmax);
		}
		if (!CamServer.bps.equalsIgnoreCase("-1")) {
			command = command.concat(" -b:v").concat(" " + CamServer.bps);
			command = command.concat(" -maxrate").concat(" " + CamServer.bps);
		}
		command = command.concat(" http://localhost:8090/feed1.ffm");

		avconv = Runtime.getRuntime().exec(command);

		avconvstdout = new DefaultStreamGobbler(avconv.getInputStream(), "OUTPUT",
				false, "avconv_out");
		avconvstdout.start();
		avconvstderr = new FfmpegStreamGobbler(avconv.getErrorStream(), "ERROR",
				true, "avconv_err");
		avconvstderr.start();

		// Wait until the video streaming is started
		synchronized (avconvstderr) {
			try {
				avconvstderr.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the avconv process
	 * 
	 * @throws Exception
	 */
	private void stopAvconv() throws Exception {

		// It is necessary to kill the process in this way to be sure that
		// SIGTERM (15) is sent
		// as the kill signal.
		String pid = "" + getUnixPID(avconv);
		Process killingProcess = Runtime.getRuntime().exec(
				new String[] { "kill", "-15", pid });
		killingProcess.waitFor();
		avconv.waitFor();
		avconvstdout.interrupt();
		avconvstderr.interrupt();
	}

	/**
	 * Starts a avserver process with the configuration file given as parameter
	 * 
	 * @param configFile
	 *            - avserver configuration file
	 * @throws IOException
	 */
	private void startAvserver(String configFile) throws IOException {
		avserver = Runtime.getRuntime().exec(
				new String[] { serverName, "-f", configFile, "-v", "info" });
		avserverstdout = new DefaultStreamGobbler(avserver.getInputStream(), "OUTPUT",
				true, "avserver_out");
		avserverstdout.start();
		avserverstderr = new DefaultStreamGobbler(avserver.getErrorStream(), "ERROR",
				true, "avserver_err");
		avserverstderr.start();
	}

	private void stopAvserver() throws InterruptedException {
		avserver.destroy();
		avserver.waitFor();
		avserverstdout.interrupt();
		avserverstderr.interrupt();
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
						"http://localhost:8090/test.swf" });
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
	 * private BufferedImage getCurrentImage() throws IOException{
	 * 
	 * try { stopAvconv();
	 * 
	 * JFrame progressFrame = new JFrame("Capturando...");
	 * progressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); Container
	 * content = progressFrame.getContentPane(); JProgressBar progress = new
	 * JProgressBar(); progress.setIndeterminate(true); Border border =
	 * BorderFactory.createTitledBorder("Capturando...");
	 * progress.setBorder(border); content.add(progress, BorderLayout.NORTH);
	 * GraphicsEnvironment graphics =
	 * GraphicsEnvironment.getLocalGraphicsEnvironment();
	 * progressFrame.setLocation(graphics.getCenterPoint());
	 * progressFrame.setSize(300,70); progressFrame.setVisible(true);
	 * 
	 * FrameGrabber frameGrabber; //frameGrabber =
	 * CamServer.getVideoDevice().getJPEGFrameGrabber(CamServer.widthFrame, //
	 * CamServer.heightFrame, CamServer.input, CamServer.std,
	 * CamServer.getJpegQuality());
	 * 
	 * //Set the jpegQuality to 99 //frameGrabber =
	 * CamServer.getVideoDevice().getJPEGFrameGrabber(CamServer.captureWidth, //
	 * CamServer.captureHeight, CamServer.input, CamServer.std, 99);
	 * 
	 * frameGrabber =
	 * CamServer.getVideoDevice().getYUVFrameGrabber(CamServer.captureWidth,
	 * CamServer.captureHeight, CamServer.input, CamServer.std);
	 * 
	 * 
	 * ImageCallback imageCallback = new ImageCallback();
	 * frameGrabber.setCaptureCallback(imageCallback);
	 * frameGrabber.startCapture();
	 * 
	 * BufferedImage currentImage = imageCallback.getCurrentImage();
	 * 
	 * frameGrabber.stopCapture();
	 * CamServer.getVideoDevice().releaseFrameGrabber();
	 * 
	 * //Thread.sleep(3000);
	 * 
	 * startAvconv();
	 * 
	 * if (ffplayActive){ stopAvplay(); startAvplay(); }
	 * progressFrame.setVisible(false); progressFrame.dispose();
	 * 
	 * return currentImage;
	 * 
	 * } catch (V4L4JException | InterruptedException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); return null; }
	 * 
	 * }
	 */

	/**
	 * This method return the current image with the parameters saved in the
	 * CamServer static instance (device, width, height).
	 * 
	 * @return - the current image
	 * @throws IOException
	 */
	private BufferedImage getCurrentImage() throws IOException {

		try {
			stopAvconv();

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

			startAvconv();

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
		String headHTML = "<script type=\"text/javascript\">"
				+ "string1 = \"<object class=\'stream\' type=\'application/x-shockwave-flash\' data=\'http://\";"
				+ "string2 = location.host.split(\':\')[0];"
				+ "string3 = \":8090/test.swf\'> <\\/object>\";"
				+ "streamurl = string1 + string2 + string3;" + "</script>";
		return headHTML;
	}

	@Override
	public String getBodyHTMLText() {
		String bodyHTML = "<script type=\"text/javascript\">"
				+ "document.write(streamurl);" + "</script>";
		return bodyHTML;
	}
	
	public class FfmpegStreamGobbler extends DefaultStreamGobbler {
		
		public FfmpegStreamGobbler (InputStream is, String type, boolean print, String name){
			super(is,type,print,name);
		}
		
		@Override
		protected void analyzeLine (String line){
			if (line.startsWith("frame=")) {
				synchronized (this) {
					notify();
				}
			}
		}
	}

}