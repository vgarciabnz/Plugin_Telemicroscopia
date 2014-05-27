import ij.plugin.PlugIn;

import communication.CamServer;

/**
 * This plugin runs when the Capture button is pushed.
 * 
 * @author ehas
 * 
 */
public class Capture_ implements PlugIn {
	public void run(String arg) {

		try {
			synchronized (CamServer.LockLocal) {
				CamServer.setLocalCapture(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
