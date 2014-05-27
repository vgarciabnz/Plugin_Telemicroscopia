import ij.plugin.PlugIn;

import communication.v4l.Settings;

public class Settings_ implements PlugIn {

	public void run(String arg0) {
		new Settings().setVisible(true);
	}
}