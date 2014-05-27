import edition.ImageManager;
import edition.RoiManager;
import gestion.GestorOpen;
import gestion.GestorSave;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Commands;
import ij.plugin.PlugIn;

import javax.swing.JOptionPane;

public class Open_ implements PlugIn {

	private static GestorOpen gestorOpen = new GestorOpen();

	public void run(String arg) {

		// First, check if an image has been already opened. If so, ask the user
		// for saving it
		ImagePlus imp_old = WindowManager.getCurrentImage();

		if (imp_old != null) {
			int result = JOptionPane.showConfirmDialog(null,
					"¿Desea guardar el fichero actual?", "Guardar cambios",
					JOptionPane.YES_NO_CANCEL_OPTION);
			switch (result) {
			case 0:
				GestorSave saver = new GestorSave();
				saver.saveAsTifInZip(imp_old, false);
			case 1:
				// Close everything related to the previous session
				if (RoiManager.getInstance() != null) {
					RoiManager.getInstance().close();
					RoiManager.setInstance(null);
				}
				if(ImageManager.getInstance() != null) {
					ImageManager.getInstance().close();
				}
				while (WindowManager.getCurrentImage() != null) {
					WindowManager.getCurrentImage().close();
					WindowManager.setTempCurrentImage(null);
				}
				break;
			default:
				// Exit the dialog and do nothing
				return;
			}
		}

		// Open the new image
		(new Commands()).run("open");
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			gestorOpen.opener(imp);
		}
	}
}
