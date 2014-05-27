package gestion;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.FileInfo;

import java.awt.event.KeyListener;
import java.io.File;

import communication.v4l.FrameCamServer;
import edition.ImageManager;
import edition.RoiManager;

/**
 * This class is used to open new sessions. An ImagePlus object is passed as parameter.
 * @author ehas
 *
 */
public class GestorOpen {
	public void opener(ImagePlus imp) {
		FileInfo ofi = null;
		if (imp!=null) {
			ofi = imp.getOriginalFileInfo();
		}
		String name = imp.getTitle();
        String directory = ofi.directory;
		String imagePath = directory+name;
		String filePath= imagePath.substring(0, imagePath.length()-3)+"zip";
			
		
		if (new File(filePath).exists()==true){	
			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			rm.runCommand("reset");
			rm.runCommand("Open", filePath);
		}
		else{
			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			rm.runCommand("reset");
		}
				
		ImageManager.getInstance().run("");
		
	}
}
