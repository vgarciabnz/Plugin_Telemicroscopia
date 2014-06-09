import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.MacroRunner;
import ij.plugin.PlugIn;

import javax.swing.JOptionPane;


import file.edition.ImageManager;
import file.edition.RoiManager;
import file.io.TPSaver;

/**
 * This plugin runs when the close button is pushed. This is the right way to
 * close the whole working area: Roi manager, Images manager and Displayer. 
 * 
 * @author ehas
 * 
 */
public class Close_ implements PlugIn {

	@Override
	public void run(String arg0) {
		ImagePlus current = WindowManager.getCurrentImage();
		if (current != null) {
			// First ask for saving the current image
			int result = JOptionPane.showConfirmDialog(null,
					"¿Desea guardar el fichero actual?", "Guardar cambios",
					JOptionPane.YES_NO_CANCEL_OPTION);
			switch (result) {
				case 0:
					TPSaver saver = new TPSaver();
					saver.saveAsTifInZip(current, false);
				case 1:
					new MacroRunner("run(\"ForceClose \")\n");
					break;
				default:
					// Exit the dialog and do nothing
					return;
			}
		} else {
			JOptionPane.showMessageDialog(null, "No hay imágenes abiertas",
					"Fallo cerrando ficheros", JOptionPane.ERROR_MESSAGE);
		}
	}

}
