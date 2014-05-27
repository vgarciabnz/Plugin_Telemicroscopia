package communication.v4l;

import java.awt.event.WindowAdapter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import javax.swing.JCheckBox;

import util.ConfigFile;
import util.LogSync;
import util.MyLogger;
import util.MyLoggerFormatter;
import util.StatusIndicator;
import au.edu.jcu.v4l4j.ControlList;
import au.edu.jcu.v4l4j.JPEGFrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

/**
 * This class manages the web server of AppTm4l and controls the video streaming thread. There are two main threads:
 * the first one is "serverThread", that listens for incoming connections. When a new connection arrives, it launches a new ConnectionThread
 * to handle the query. The second one is "streamThread", that control the video streaming through a "Streamer" object. Streamer represents
 * a possible technology to perform the streaming (FFmpeg, VLC, gstreamer, etc). This class also stores useful information about the 
 * video streaming parameters, such as bitrate, group of pictures, framerate, etc.
 * This class is a modification of gilles' CamServer class of video4linux4java project.
 * 
 * @author gilles
 * 
 */
public class CamServer extends WindowAdapter {

	/** Default tcp port to listen to */
	public static final int DEFAULT_PORT = 80;
	/** Default value for minimum video quality (-1 if not set). */
	public static final int DEFAULT_QMIN = -1;
	/** Default value for maximum video quality (-1 if not set). */
	public static final int DEFAULT_QMAX = -1;
	/** Default value for group of pictures (-1 if not set). */
	public static final int DEFAULT_GOP = 100;
	/** Default value for video bandwidth (-1 if not set). */
	public static final String DEFAULT_BPS = "256k";
	/** Default value for authentication protocol (true - active; false - inactive). */
	private static final boolean DEFAULT_AUTH_NEEDED = false;
	/** Default value for user name (http basic authentication). */
	private static final String DEFAULT_USER_NAME = "admin";
	/** Default value for user password (http basic authentication). */
	private static final String DEFAULT_USER_PASSWORD = "admin";
	/** Default value for stream name. */
	private static final String DEFAULT_STREAM_NAME = "AppTm4l";

	/** Configuration file */
	public static final String configFile = "AppTm4l.properties";
	
	/** AppTm4l logger. */
	public static MyLogger logger;
	/** AppTm4l logger file name. */
	public static String logFileName;
	/** It indicates if log files must be sent (synchronized). */
	public static boolean logSync = false;
	/** Time of application initialization */
	private static long startTime;
	
	/** Defines if authentication is needed or not. */
	public static boolean authentication = DEFAULT_AUTH_NEEDED;
	/** User name for http basic authentication. */
	public static String userName = DEFAULT_USER_NAME;
	/** Password for http basic authentication. */
	public static String userPassword = DEFAULT_USER_PASSWORD;
	/** Name for the stream file (a random string). */
	public static String streamName = DEFAULT_STREAM_NAME;

	/** Socket that listens for incoming connections. */
	private static ServerSocket serverSocket;
	public static VideoDevice videoDevice;
	public static JPEGFrameGrabber frameGrabber;
	public static ControlList controlList;
	/** This thread listens for incoming connections and launches a ConnectionThread object to handle it. */
	private static Thread serverThread;
	/** This thread controls the video streaming. */
	private static Thread streamThread;
	/** This is the object that actually performs the video streaming. */
	public static Streamer streamer;
	public static Vector<ClientConnection> clients;

	/** This variable shows visually the state of the camera (inactive, in progress, active). */
	public static StatusIndicator indicator;
	/** This variable shows the camera state in a checkbox. */
	public static JCheckBox cameraCheckBox;
	/** This boolean represents whether the camera is active or not. */
	public static boolean active = false;
	private static boolean localCapture = false;
	private static DataOutputStream externalCapture;
	/** This object must be locked in a synchronized clause when managing an external capture. */
	public static Object LockExternal = new Object();
	/** This object must be locked in a synchronized clause when managing a local capture. */
	public static Object LockLocal = new Object();
	/** This object is used to notify a capture event. It must be used in a synchronized clause. */
	public static Object LockCapture = new Object();
	/** This object is used to notify that viewer button has been pushed. It must be used in a synchronized clause. */
	public static Object LockViewer = new Object();
	/** This object is locked when the camera is activated/deactivated. */
	public static ReentrantLock LockCamServerInit = new ReentrantLock(true);
	/** Width of the video streaming in pixels. */
	public static int videoWidth;
	/** Height of the video streaming in pixels. */
	public static int videoHeight;
	/** Width of the capture image in pixels. */
	public static int captureWidth;
	/** Height of the capture image in pixels. */
	public static int captureHeight;
	/** Numerator of video streaming interval (inverse of framerate). */ 
	public static int intervalNum;
	/** Denominator of video streaming interval (inverse of framerate). */ 
	public static int intervalDen;
	/** Value of video streaming Qmin (if applied). */
	public static int qmin = DEFAULT_QMIN;
	/** Value of video streaming Qmax (if applied). */
	public static int qmax = DEFAULT_QMAX;
	/** Value of video streaming "group of pictures". */
	public static int gop = DEFAULT_GOP;
	/** TCP port for incoming connections. */
	public static int port = DEFAULT_PORT;
	/** Video streaming bitrate (in kbits).  */
	public static String bps = DEFAULT_BPS;

	/**
	 * Builds a camera server object capturing frames from the given device at
	 * the given resolution and sending them out to clients connected to the
	 * given TCP port number.
	 * 
	 * @param dev
	 *            the video device file
	 * @param videoWidth
	 *            the capture width
	 * @param videoHeight
	 *            the capture height
	 * @param port
	 *            the TCP port to listen on for incoming connections
	 * @throws V4L4JException
	 *             if a JPEG frame grabber cant be created
	 * @throws IOException
	 *             if a server socket on the given port cant be created
	 */
	public static void initialize(String dev, int videoWidth, int videoHeight,
			int captureWidth, int captureHeight, int port, int intervalNum,
			int intervalDen) throws V4L4JException, IOException {
		CamServer.videoHeight = videoHeight;
		CamServer.captureHeight = captureHeight;
		CamServer.videoWidth = videoWidth;
		CamServer.captureWidth = captureWidth;
		CamServer.port = port;
		CamServer.videoDevice = new VideoDevice(dev);
		CamServer.intervalNum = intervalNum;
		CamServer.intervalDen = intervalDen;
		
		startTime = System.currentTimeMillis();
		
		if(ConfigFile.configFile == null){
			// Set the configuration file
			ConfigFile.setConfigFile(configFile);			
		}
		
		if (ConfigFile.getValue(ConfigFile.LOG_SYNCHRONIZATION).equals("true")){
			logSync = true;
			// Check if there exist unsent logger files. If so, send them.
			LogSync.updatePendingLogs();
		}
		
		// Create the Logger object for the main events
		logger = MyLogger.getLogger("CamServer");
		logFileName = MyLogger.createLoggerName();
		FileHandler fh = new FileHandler(logFileName, true);
		fh.setFormatter(new MyLoggerFormatter());
		logger.addHandler(fh);
		logger.setUseParentHandlers(false);

		CamServer.logger.info("*******************");
		CamServer.logger.info(MyLogger.getLogRecord("APPTM4L_START"));

		// logExit thread will be added to the shoutdown routine.
		Thread logExit = new Thread(new Runnable() {

			@Override
			public void run() {
				if (active) {
					CamServer.stop();
				}
				CamServer.logger.info(MyLogger.getLogRecord("APPTM4L_FINISH"));
				long time = (System.currentTimeMillis() - startTime) / 1000;
				CamServer.logger.info(MyLogger.getLogRecord("APPLICATION_TIME") +" " + time);
				CamServer.logger.info("*******************");
				// The logger has been created using MyLogger class, so it is
				// not registered in LogManager
				// Because of that, we have to manually perform the reset()
				// method of LogManager
				Handler[] targets = logger.getHandlers();
				for (int i = 0; i < targets.length; i++) {
					Handler h = targets[i];
					logger.removeHandler(h);
					h.flush();
					h.close();
				}
				
				if(logSync == true){
					// Send the log file
					LogSync.sendLogFile(new File(CamServer.logFileName));					
				}
			}
		});

		Runtime.getRuntime().addShutdownHook(logExit);
			
		streamName = generateStreamName();

	}

	/**
	 * Invoke this method to start a video streaming with the selected
	 * parameters (video device, resolution, port,...). It will create two
	 * threads: server thread, that listens to the selected port and wait for
	 * incoming connections; and stream thread, that manages everything related
	 * to the streaming (video acquisition and encoding, video server).
	 * 
	 * @throws IOException
	 */
	public static void start() throws IOException {

		if (CamServer.indicator != null) {
			CamServer.indicator.setState(StatusIndicator.PROGRESS);		
		}			

		controlList = videoDevice.getControlList();
		clients = new Vector<ClientConnection>();

		// initialize tcp port to listen on
		
		serverSocket = new ServerSocket(port);
		System.out.println("Server listening at "
				+ serverSocket.getInetAddress().getHostAddress() + ":"
				+ serverSocket.getLocalPort());

		serverThread = new Thread(new Runnable() {

			/**
			 * implements the server thread: while we are not interrupted and
			 * the capture thread is running, we run the main loop. Before
			 * exiting, we close all client connections
			 */
			@Override
			public void run() {
				Vector<ClientConnection> copyClients = null;
				try {

					while (!Thread.interrupted()) {
						
						Socket clientSocket = serverSocket.accept();
						new Thread(new ConnectionThread(clientSocket)).start();
					}

				} catch (IOException e) {
					// error accepting new client connection over server socket
					// or closing connection with a client
				}

				// Stop all client connections
				synchronized (clients) {
					copyClients = new Vector<ClientConnection>(clients);
				}

				for (ClientConnection client : copyClients)
					client.stop();

				System.out.println("Server thread exiting");
			}
		}, "Server thread");

		// start the video capture thread
		// create capture and server threads
		// streamer = new LibavThread();
		// streamer = new VLCThread();
		streamer = new FfmpegRtmpThread();
		streamThread = new Thread(streamer, "Stream thread");

		// Waits until the video streaming is started
		synchronized (streamer) {
			streamThread.start();
			try {
				streamer.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// start the tcp server thread
		serverThread.start();

		active = true;

		CamServer.logger.info(MyLogger.getLogRecord("VIDEO_STREAMING_START"));
		if (CamServer.indicator != null) {
			CamServer.indicator.setState(StatusIndicator.UP);
		}
	}

	/**
	 * Invoke this method to stop the video streaming and the tcp server.
	 */
	public static void stop() {
	
		if (CamServer.indicator != null) {
			CamServer.indicator.setState(StatusIndicator.PROGRESS);
		}

		active = false;
		// if the streaming thread is alive interrupt it and close it
		if (streamThread.isAlive()) {
			streamThread.interrupt();
			try {
				streamThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// same with the server thread, but this time, to interrupt it
		// close the server socket first, which produces a socketException in
		// serverThread when
		// trying to execute serverSocket.accept()
		try {
			serverSocket.close();
		} catch (IOException e) {
			// error closing the server socket
		}

		if (serverThread.isAlive()) {
			serverThread.interrupt();
			try {
				serverThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		videoDevice.releaseControlList();

		CamServer.logger.info(MyLogger.getLogRecord("VIDEO_STREAMING_FINISH"));
		if (CamServer.indicator != null) {
			CamServer.indicator.setState(StatusIndicator.DOWN);
		}
	}

	public static void refreshStreaming() {
		CamServer.stop();
		try {
			CamServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Invoke this method if a local capture has been taken. It uses the
	 * LockCapture object to notify that a capture has been taken, and sets
	 * CamServer.localCapture variable to true in order to specify that the
	 * capture is local.
	 * 
	 * @param capture
	 *            set to "true" if a local capture has been taken
	 */
	public static void setLocalCapture(boolean capture) {

		synchronized (CamServer.LockCapture) {
			CamServer.localCapture = capture;
			LockCapture.notify();
		}
	}

	/**
	 * This method check if a local capture has been taken
	 * 
	 * @return "true" if a localCapture has been taken
	 */
	public static boolean isLocalCapture() {
		return localCapture;
	}

	/**
	 * It is used to check if an external capture has been taken
	 * 
	 * @return The client DataOutputStream if an external capture has been
	 *         taken; null if not
	 */
	public static DataOutputStream getExternalCapture() {
		return externalCapture;
	}

	/**
	 * Invoke this method when an external capture has been taken.
	 * 
	 * @param externalCapture
	 *            The client DataOutputStream to send the capture
	 */
	public static void setExternalCapture(DataOutputStream externalCapture) {
		CamServer.externalCapture = externalCapture;
	}

	/**
	 * Invoke this method when the external capture has been correctly managed.
	 * It clears the value of the client DataOutputStream
	 */
	public static void clearExternalCapture() {
		CamServer.externalCapture = null;
	}

	public static void setFrameGrabber(JPEGFrameGrabber frameGrabber) {
		CamServer.frameGrabber = frameGrabber;
	}

	/**
	 * Change the value of the tcp port to listen to.
	 * 
	 * @param port
	 *            Tcp port to listen to.
	 */
	public static void setPort(int port) {
		CamServer.port = port;
	}

	/**
	 * Get the value of the tcp port the server is listen to.
	 * 
	 * @return Tcp port the server is listen to.
	 */
	public static int getPort() {
		return port;
	}

	/**
	 * Get the current video device
	 * 
	 * @return current video devide
	 */
	public static VideoDevice getVideoDevice() {
		return videoDevice;
	}

	/**
	 * Change the value of the video device
	 * 
	 * @param file
	 *            A String with the value of the video device (example
	 *            "/dev/video0").
	 */
	public static void setVideoDevice(String file) {
		try {
			videoDevice = new VideoDevice(file);
		} catch (V4L4JException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the video framerate as a double.
	 * 
	 * @return Value of video framerate as a double.
	 */
	public static double getFramerate() {
		double framerate = (double) CamServer.intervalDen
				/ (double) CamServer.intervalNum;
		return framerate;
	}
	
	private static String generateStreamName() {
		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString();
		return output;
	}

}
