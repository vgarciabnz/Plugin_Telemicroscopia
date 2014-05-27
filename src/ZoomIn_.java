import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;

/**
 * This is a zoomIn tool (if an image is opened).
 * @author ehas
 *
 */
public class ZoomIn_ implements PlugIn{

	@Override
	public void run(String arg0) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null){
			ImageCanvas ic = imp.getCanvas();
			int x = imp.getWindow().getWidth()/2;
			int y = imp.getWindow().getHeight()/2;
			ic.zoomIn(x, y);
		}		
	}

}
