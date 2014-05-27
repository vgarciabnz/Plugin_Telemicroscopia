import ij.Macro;
import ij.plugin.PlugIn;

import java.io.IOException;
import java.net.BindException;

import javax.swing.JOptionPane;

import util.StatusIndicator;

import communication.v4l.CamServer;

/**
 * This plugin runs when the activate/deactivate camera button is pushed.
 * 
 * @author ehas
 * 
 */
public class ActivateCamera_ implements PlugIn {

	public void run(String arg) {

		// This variable is used to avoid the execution of several run()
		// threads.
		// This happens when the activate/deactivate camera button is pushed
		// many times quickly.
		CamServer.LockCamServerInit.lock();

		// This variable is: true if the camera has been enabled, false if
		// disabled
		String state = Macro.getOptions().trim();

		if (state.equals("true") && !CamServer.active) {

			try {
				CamServer.start();
			} catch (IOException e) {
				if(e instanceof java.net.BindException){
					JOptionPane.showMessageDialog(null,
							"No se ha podido inicializar el servidor de internet.\n" +
							"Comprueba que el puerto " + CamServer.port + " está libre " +
							"y que tienes los\npermisos necesarios para ocuparlo.",
							"Fallo inicializando el servidor de internet",
							JOptionPane.ERROR_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(null,
							"No se ha podido inicializar el servidor de internet",
							"Fallo inicializando el servidor de internet",
							JOptionPane.ERROR_MESSAGE);
				}
				if (CamServer.indicator != null) {
					CamServer.indicator.setState(StatusIndicator.DOWN);
				}
				if (CamServer.cameraCheckBox != null){
					CamServer.cameraCheckBox.setSelected(false);					
				}
			}
		}
		// If camera has been disabled, the CamServer instance stops
		else if (state.equals("false")) {
			if (CamServer.active) {
				CamServer.stop();
			}
		}
		CamServer.LockCamServerInit.unlock();
	}

}
