package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class extends Thread and is used to read the output of system processes. When launching a 
 * system process is necessary to read both standard and error outputs to avoid buffers to overflow.
 * @author ehas
 *
 */
public class DefaultStreamGobbler extends Thread {

	InputStream is;
	String type;
	boolean print;
	String name;

	/**
	 * Create a new DefaultStreamGobbler object.
	 * 
	 * @param is InputStream of the system process.
	 * @param type Type of output (standard or error). It is only used as a tag.
	 * @param print True - messages are printed in console.
	 * @param name Name of the system process.
	 */
	public DefaultStreamGobbler(InputStream is, String type, boolean print, String name) {
		this.is = is;
		this.type = type;
		this.print = print;
		this.name = name;
		this.setName(name);
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while (!Thread.interrupted() && ((line = br.readLine()) != null)) {

				analyzeLine(line);

				if (print) {
					System.out.println(name + "> " + line);
				}
			}
		} catch (IOException ioe) {
			if (ioe.getMessage().equalsIgnoreCase("stream closed")) {
				// Do nothing
				return;
			}
			ioe.printStackTrace();
		}
	}
	
	/**
	 * This method is intended to be overridden by the classes that extends this class.
	 * @param line 
	 */
	protected void analyzeLine (String line){
	}
}
