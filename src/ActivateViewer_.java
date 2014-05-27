import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;

import communication.v4l.CamServer;

/**
 * This plugin runs when the activate viewer button is pushed. If the CamServer
 * has not been initialized, it does nothing.
 * 
 * @author ehas
 * 
 */
public class ActivateViewer_ implements PlugIn {

	@Override
	public void run(String arg0) {

		// If CamServer is active, it notifies that the button has been pushed
		if (CamServer.active == true) {
			try {
				synchronized (CamServer.LockViewer) {
					CamServer.LockViewer.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
		
	}
}
