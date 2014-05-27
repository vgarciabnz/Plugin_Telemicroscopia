import edition.ImageManager;
import edition.RoiManager;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;


public class ForceClose_ implements PlugIn {

	@Override
	public void run(String arg0) {
		ImagePlus current = WindowManager.getCurrentImage();
		if (current != null) {
			// Close everything related to the current session
			if (RoiManager.getInstance() != null) {
				RoiManager.getInstance().close();
				RoiManager.setInstance(null);
			}
			if (ImageManager.getInstance() != null) {
				ImageManager.getInstance().close();
			}
			while (WindowManager.getCurrentImage() != null) {
				WindowManager.getCurrentImage().close();
				WindowManager.setTempCurrentImage(null);
			}
		}
		
	}

}
