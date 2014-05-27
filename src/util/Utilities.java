package util;

import ij.WindowManager;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class Utilities {

	public static void showProgressBar(Integer maxSize) {
		final JDialog dlg = new JDialog(WindowManager.getCurrentWindow(),
				"Cuadro de Progreso", true);
		JProgressBar dpb = new JProgressBar(0, maxSize);
		dlg.add(BorderLayout.CENTER, dpb);
		dlg.add(BorderLayout.NORTH, new JLabel("Capturando..."));
		dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dlg.setSize(300, 75);
		dlg.setLocationRelativeTo(WindowManager.getCurrentWindow());

		Thread t = new Thread(new Runnable() {
			public void run() {
				dlg.setVisible(true);
			}
		});
		t.start();
		for (int i = 0; i <= maxSize; i++) {
			// jl.setText("Count : " + i);
			dpb.setValue(i);
			if (dpb.getValue() == maxSize) {
				dlg.setVisible(false);
				// System.exit(0);
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static Boolean writeBufferedImageToFile(String path,
			BufferedImage bufferedImage) {
		try {
			File f = new File(path);
			return ImageIO.write(bufferedImage, "jpg", f);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

	}

	public static String[] executeLineCommand(String[] command,
			String inputData, Boolean readOutputAndErrorResponse) {
		try {
			String s;
			StringBuffer stdErrString = new StringBuffer();
			StringBuffer stdOutputString = new StringBuffer();
			String[] outputErrorResponse = new String[3];
			String commandLine = "Command = ";
			for (String commandItem : command) {
				commandLine += commandItem;
			}
			System.out.println(commandLine);
			ProcessBuilder builder = new ProcessBuilder(command);
			// builder.redirectErrorStream(true);
			/*
			 * Map<String, String> env=builder.environment(); for
			 * (Map.Entry<String, String> entry : env.entrySet()) { String key =
			 * entry.getKey(); String value = entry.getValue();
			 * System.out.println(key + value); }
			 */
			/*
			 * env.put("XMIPP_HOME", "/home/aquintana/xmipp");
			 * env.put("LD_LIBRARY_PATH", "/home/aquintana/xmipp/lib");
			 * env.put("PATH", "$XMIPP_HOME/bin");
			 */
			Process process = builder.start();
			// System.out.println(""+process.exitValue());
			try {
				outputErrorResponse[2] = String.valueOf(process.exitValue());
			} catch (Exception e) {
				outputErrorResponse[2] = null;
			}
			if (inputData != null) {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(process.getOutputStream()));
				writer.write(inputData);
				writer.flush();
				writer.close();
			}
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			// read the output from the command
			// System.out.println("Here is the standard output of the command:\n");
			if (readOutputAndErrorResponse) {
				while ((s = stdInput.readLine()) != null) {
					System.out.println("Input" + s);
					stdOutputString.append(s).append('\n');
				}

				// read any errors from the attempted command
				// System.out.println("Here is the standard error of the command (if any):\n");
				while ((s = stdError.readLine()) != null) {
					System.out.println("Error" + s);
					stdErrString.append(s).append('\n');
				}
			}
			outputErrorResponse[0] = stdOutputString.toString();
			outputErrorResponse[1] = stdErrString.toString();

			// outputErrorResponse[2]=String.valueOf(process.exitValue());

			System.out.println("Standard Output = " + outputErrorResponse[0]);
			System.out.println("Standard Error = " + outputErrorResponse[1]);
			System.out.println("Exit Value = " + outputErrorResponse[2]);

			return outputErrorResponse;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
