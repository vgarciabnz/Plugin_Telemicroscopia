package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigFile {
	
	// Relative path to the configuration file
	public static String configFile;
	
	// These key strings must match with the name in the configuration file
	public static final String LOG_NAME = "logname";
	public static final String LOG_SYNCHRONIZATION = "logsynchronization";
	public static final String LOG_EMAIL = "logemail";
	public static final String FONT_SIZE = "fontsize";
	public static final String SHOW_SURVEY_FORM = "showsurveyform";
	
	/**
	 * Set the path to the configuration file
	 * @param configFile
	 */
	public static void  setConfigFile (String configFile){
		ConfigFile.configFile = configFile;
	}
	
	public static String getConfigFile () {
		return configFile;
	}
	
	/**
	 * Return the value associated to the key. If it does not exist, it returns null.
	 * @param key
	 * @return
	 */
	public static String getValue (String key){
		
		Properties prop = new Properties();
		InputStream input = null;
		String result = null;
	
		try {
			input = new FileInputStream(configFile);
			prop.load(input);
			result = prop.getProperty(key);
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if (input != null){
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
}
