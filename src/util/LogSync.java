package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import communication.CamServer;
import communication.DefaultStreamGobbler;

/**
 * This class provides a set of static methods to manage log synchronization. Sent logs are stored 
 * in the log_sync directory under default log directory.
 * @author ehas
 *
 */
public class LogSync {
	
	/** Directory to store synchronized log files. */
	private static final String LOG_SYNC_DIR = "log_sync";
	
	private static boolean unableToSend = false;
	
	/**
	 * This method must be executed BEFORE any log file is created. It takes any log in the default log 
	 * directory and send them to the email address specified in the configuration file. 
	 */
	public static void updatePendingLogs (){
		
		File f = new File(MyLogger.DEFAULT_LOGGERDIR); // log directory
		
		if(!f.exists()){
			f.mkdir();
			return;
		}

	    File[] files = f.listFiles();
	    for (File file : files) {
	    	String name = file.getName();
	        
	    	// If this file exists, it means that the program was incorrectly close. It must be deleted.
	    	if(name.contains(".log.lck")){
	    		file.delete();
	        } else if (name.contains(".log")){
	        	sendLogFile(file, false);
	        		        	
	        }
	    }
	}
	
	private static void storeLogFile (File logFile){
		
		File sync_dir = new File(MyLogger.DEFAULT_LOGGERDIR + "/" + LOG_SYNC_DIR);
		
		if (!sync_dir.exists()){
			sync_dir.mkdir();
		}
		
		logFile.renameTo(new File(sync_dir.getPath() + "/" + logFile.getName()));
    	logFile.delete();
	}
	
	/** 
	 * Send a file to the email address specified in the configuration file.
	 * @param attachment File to send
	 * @param onShutdownHook If it is on Shutdown routine or not.
	 */
	public static void sendLogFile (File attachment, boolean onShutdownHook){
		sendLogFile(ConfigFile.getValue(ConfigFile.LOG_EMAIL), "[TM Log] " + 
				ConfigFile.getValue(ConfigFile.LOG_NAME), attachment.getName(), attachment, onShutdownHook);
	}
	
	/**
	 * Send a file with the parameters specified in the args.
	 * @param destination Destination email address. Must have the form user@company.ext
	 * @param subject Subject of the email
	 * @param body Body of the email
	 * @param attachment File attached to the email
	 * @param onShutdownHook If it is on Shutdown routine or not.
	 */
	public static void sendLogFile (String destination, String subject, String body, File attachment, 
			boolean onShutdownHook) {
		
		if(unableToSend){
			return;
		}
		
		// If mutt/postfix is not installed, send a warning message and return.
		if (!isMuttInstalled()){
			if (!onShutdownHook) {
				JOptionPane.showMessageDialog(null,
						"El programa mutt no se encuentra instalado en el sistema.\n"
								+ "No se han podido enviar los archivos.",
						"Fallo enviando archivos", JOptionPane.ERROR_MESSAGE);
			}
			unableToSend = true;
			return;
		}
		
		// If the email address is incorrect, send a warning message and return.
		if (!destination.contains("@")){
			if (!onShutdownHook) {
				JOptionPane
						.showMessageDialog(
								null,
								"La dirección de correo electrónico <"
										+ destination
										+ "> \nproporcionada en el fichero de configuración\n"
										+ CamServer.configFile
										+ " no es correcta.\n"
										+ "No se han podido enviar los archivos.",
								"Fallo enviando archivos",
								JOptionPane.ERROR_MESSAGE);
			}
			unableToSend = true;
			return;
		}
				
		String command = "echo \"";
		if (body != null){
			command = command.concat(body);
		}
		command = command.concat("\" | mutt");
		
		if (subject != null){
			command = command.concat(" -s '" + subject + "'");
		}
		
		if (attachment != null){
			command = command.concat(" -a " + attachment.getPath() + " --");
		}
		
		command = command.concat(" " + destination + "");
		
		try {
			Process sendMail = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
			
			DefaultStreamGobbler mailstdout = new DefaultStreamGobbler(sendMail.getInputStream(), "OUTPUT",
					true, "ffmpeg_out");
			mailstdout.start();
			
			DefaultStreamGobbler mailstderr = new DefaultStreamGobbler(sendMail.getErrorStream(), "ERROR",
					true, "ffmpeg_err");
			mailstderr.start();
			
			sendMail.waitFor();
			
			storeLogFile(attachment);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private static boolean isMuttInstalled () {
		boolean isInstalled = false;
		try {
			Process checkMutt = Runtime.getRuntime().exec(new String[] {"mutt","-v"});
			BufferedReader br = new BufferedReader(new InputStreamReader(checkMutt.getInputStream()));
			String line = null;
			int count = 0;
			while (!Thread.interrupted() && ((line = br.readLine()) != null)) {

				// If Mutt is installed, the second line starts with Copyright
				if (count == 1 && line.startsWith("Copyright")){
					isInstalled = true;
				}
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isInstalled;
	}


}
