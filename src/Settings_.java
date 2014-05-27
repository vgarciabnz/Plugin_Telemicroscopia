import ij.plugin.PlugIn;

import communication.Settings;

public class Settings_ implements PlugIn {

	public void run(String arg0) {
		new Settings().setVisible(true);
	}
}