package gestion;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.macro.MacroRunner;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import edition.RoiManager;

/**
 * This class is used to save the current session. It prompts a dialog frame asking for the place to save
 * the file.
 * 
 * @author ehas
 *
 */
public class GestorSave {

	public Boolean saveAsTifInZip(ImagePlus imp, boolean open) {

		Boolean savedOk = saveImageAsTifInZip(imp, open);

		if (savedOk != null && savedOk == true) {
			FileInfo ofi = null;
			if (imp != null)
				ofi = imp.getOriginalFileInfo();
			String name = imp.getTitle();
			String directory = ofi.directory;
			String path = directory + name;

			RoiManager rm = RoiManager.getInstance();
			if (rm != null) {// rm = new GestorRoi();
				rm.runCommand("Save_Especial",
						path.substring(0, path.lastIndexOf(".")) + ".zip");
			}

		}
		return savedOk;

	}

	public Boolean saveImageAsTifInZip(ImagePlus imp, boolean open) {
		boolean unlockedImage = imp.getStackSize() == 1 && !imp.isLocked();
		if (unlockedImage)
			imp.lock();
		Boolean savedOk = null;

		FileInfo ofi = null;
		if (imp != null){
			ofi = imp.getOriginalFileInfo();
		}
	
		savedOk = new FileSaver(imp).saveAsZip();

		if (unlockedImage)
			imp.unlock();

		boolean needToReopen = (ofi == null || !imp.getTitle()
				.substring(0, imp.getTitle().lastIndexOf("."))
				.equals(ofi.fileName.substring(0,
						ofi.fileName.lastIndexOf("."))));
		if (savedOk != null
				&& savedOk != false
				&& needToReopen) {
			// First, current image is closed
			new MacroRunner("run(\"ForceClose \")\n");
			// Then, new image is opened
			new Opener().open(imp.getOriginalFileInfo().directory
					+ imp.getTitle().substring(0, imp.getTitle().length() - 3)
					+ "zip");
			new GestorOpen().opener(imp);
		}

		return savedOk;
	}

	public PrintStream generateXml(String path, String imagen, String roiZip) {
		PrintStream xml;
		try {
			xml = new PrintStream(path + ".xml");

			StringBuffer StrXML = new StringBuffer();
			StrXML.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");

			StrXML.append("<TMFile>\n");

			StrXML.append("\t<imagen>");
			StrXML.append(imagen);
			StrXML.append("</imagen>\n");

			StrXML.append("\t<roiZip>");
			StrXML.append(roiZip);
			StrXML.append("</roiZip>\n");

			StrXML.append("</TMFile>");

			xml.println(StrXML.toString());
			return xml;
		} catch (FileNotFoundException e) {
			IJ.error("Error while writting XML File");
			return null;
		}

	}
}
