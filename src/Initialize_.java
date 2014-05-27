import ij.IJ;
import ij.Prefs;
import ij.plugin.PlugIn;

import java.io.IOException;
import java.util.Hashtable;

import au.edu.jcu.v4l4j.exceptions.V4L4JException;

import communication.CamServer;
import communication.Settings;

/**
 * This plugin runs at the beginning of the telemicroscopy toolbar creation.
 * 
 * @author ehas
 * 
 */
public class Initialize_ implements PlugIn {

	public void run(String arg) {

		// Set the useJFileChooser preference to false in order to avoid the
		// change of Look&Feel when managing windows
		Prefs.useJFileChooser = false;

		// Hide the main IJ instance
		IJ.getInstance().setVisible(false);

		// Initialize the values with a device /dev/video0
		String defaultDevice = Settings.getDefaultVideoDevice();
		if (defaultDevice == null) {
			// TODO print message
		} else {
			Hashtable<String, String> defaultSettings = Settings
					.getDefaultSettings(defaultDevice);
			try {
				String videoResolution = defaultSettings.get("videoResolution");
				String captureResolution = defaultSettings
						.get("captureResolution");
				String interval = defaultSettings.get("interval");
				CamServer.initialize(defaultDevice,
						Integer.parseInt(videoResolution.split("x")[0]),
						Integer.parseInt(videoResolution.split("x")[1]),
						Integer.parseInt(captureResolution.split("x")[0]),
						Integer.parseInt(captureResolution.split("x")[1]),
						CamServer.port, Integer.parseInt(interval.split("/")[0]),
						Integer.parseInt(interval.split("/")[1]));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (V4L4JException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
