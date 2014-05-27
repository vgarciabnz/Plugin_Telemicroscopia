package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import communication.v4l.DefaultStreamGobbler;

public class LogSync {
	
	/** Directory to store synchronized log files. */
	private static final String LOG_SYNC_DIR = "log_sync";
	
	/**
	 * This method must be executed BEFORE any log file is created.
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
	        	sendLogFile(file);
	        		        	
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
	
	public static void sendLogFile (File attachment){
		sendLogFile(ConfigFile.getValue(ConfigFile.LOG_EMAIL), "[TM Log] " + 
				ConfigFile.getValue(ConfigFile.LOG_NAME), attachment.getName(), attachment);
	}
	
	public static void sendLogFile (String destination, String subject, String body, File attachment) {
		
		// If mutt/postfix is not installed, send a warning message and return.
		if (!isMuttInstalled()){
			//TODO Send a warning message
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
		
		//TODO check if the destination address is right
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
	
	public static boolean isMuttInstalled () {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isInstalled;
	}


}
