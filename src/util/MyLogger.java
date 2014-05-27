package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import communication.v4l.CamServer;

/**
 * This class extends the default java.util.logging.Logger class. The only
 * purpose of this class is to avoid the registration of the Logger in the
 * LogManager. LogManager adds a shutdownhook that resets all registered
 * loggers, so all loggers became disabled at shutdown rutine and it is not
 * possible to log anything at that moment. Loggers should be manually reseted.
 * 
 * @author ehas
 * 
 */
public class MyLogger extends Logger {
	
	/** Directory to store log files. */
	public static final String DEFAULT_LOGGERDIR = "log";
	/** Name of the default logger file (it can be overwritten by including a line in the config file 
	 * with the line "Name: name_of_the_log" */
	private static final String DEFAULT_LOGGERFILENAME = "AppTm4l";

	protected MyLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	/**
	 * This class directly create a new Logger and doesn't ask the LogManager
	 * for a Logger, so it avoids the registration of the Logger.
	 * 
	 * @param name
	 * @return
	 */
	public static MyLogger getLogger(String name) {
		return new MyLogger(name, null);
	}

	public static String getLogRecord(String key) {
		return logEnglishRecords.get(key);
	}

	private static HashMap<String, String> logEnglishRecords;
	static {
		logEnglishRecords = new HashMap<String, String>();
		logEnglishRecords.put("APPTM4L_START", "AppTm4l starts");
		logEnglishRecords.put("APPTM4L_FINISH", "AppTm4l finishes");
		logEnglishRecords.put("APPLICATION_TIME", "Application time (seconds):");
		logEnglishRecords.put("VIDEO_STREAMING_START", "Video streaming starts");
		logEnglishRecords.put("VIDEO_STREAMING_FINISH",	"Video streaming finishes");
		logEnglishRecords.put("INCOMING_CONNECTION", "Incoming connection from");
		logEnglishRecords.put("EXTERNAL_CAPTURE", "An external capture has been taken");
		logEnglishRecords.put("LOCAL_CAPTURE", "A local capture has been taken");
		logEnglishRecords.put("VIDEO_STREAMING_START_CLIENT", "Video streaming connection from");
		logEnglishRecords.put("VIDEO_STREAMING_FINISH_CLIENT", "Video streaming disconnection from");
		logEnglishRecords.put("VIDEO_STREAMING_CONNECTION_TIME", "Connection time (seconds):");
		logEnglishRecords.put("SURVEY_FORM", "Survey answer:");
	}
	
	public static String createLoggerName (){
		
		File log_dir = new File(DEFAULT_LOGGERDIR);
		
		if(!log_dir.exists()){
			log_dir.mkdir();
		}
		
		String tempName = ConfigFile.getValue(ConfigFile.LOG_NAME);
		String logFileName = DEFAULT_LOGGERDIR + "/";
		long currentTime = System.currentTimeMillis();
		if (tempName != null){
			logFileName = logFileName.concat(tempName + "_" + new Date(currentTime).toString());
			logFileName = logFileName.concat( "_" + currentTime + ".log");
		}
		else {
			logFileName = logFileName.concat(DEFAULT_LOGGERFILENAME + "_" + new Date(currentTime).toString());
			logFileName = logFileName.concat("_" + currentTime + ".log");
		}
		return logFileName;
	}

}
