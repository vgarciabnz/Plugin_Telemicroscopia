import file.io.TPSaver;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import javax.swing.JOptionPane;


/**
 * This plugin runs when the Save button is pressed. If no image is opened, it
 * launches a warning message
 * 
 * @author ehas
 * 
 */
public class Save_ implements PlugIn {
	private static TPSaver saver = new TPSaver();

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();

		if (imp != null) {
			saver.saveAsTifInZip(imp, false);
		} else {
			JOptionPane.showMessageDialog(null, "No hay imágenes abiertas",
					"Fallo guardando ficheros", JOptionPane.ERROR_MESSAGE);
		}
	}
}
