import settings.Settings;
import ij.plugin.PlugIn;


public class Settings_ implements PlugIn {

	public void run(String arg0) {
		new Settings().setVisible(true);
	}
}