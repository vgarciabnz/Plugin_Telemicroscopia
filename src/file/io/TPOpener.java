package file.io;
import ij.ImagePlus;
import ij.io.FileInfo;

import java.io.File;

import file.edition.ImageManager;
import file.edition.RoiManager;

/**
 * This class is used to open new sessions. An ImagePlus object is passed as parameter.
 * @author ehas
 *
 */
public class TPOpener {
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
