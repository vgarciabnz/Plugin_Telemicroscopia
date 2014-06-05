package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class provides a set of static methods to manage the configuration file.
 * @author ehas
 *
 */
public class ConfigFile {
	
	/**
	 * Relative path to the configuration file.
	 */
	public static String configFile;
	
	// These key strings must match with the name in the configuration file
	public static final String LOG_NAME = "logname";
	public static final String LOG_SYNCHRONIZATION = "logsynchronization";
	public static final String LOG_EMAIL = "logemail";
	public static final String FONT_SIZE = "fontsize";
	public static final String SHOW_SURVEY_FORM = "showsurveyform";
	public static final String PORT = "port";
	public static final String QMIN = "qmin";
	public static final String QMAX = "qmax";
	public static final String GOP = "gop";
	public static final String AUTHENTICATION = "authentication";
	public static final String USER_NAME = "username";
	public static final String USER_PASSWORD = "userpassword";
	public static final String BPS = "bps";
	
	/**
	 * Set the path to the configuration file.
	 * @param configFile Path to the configuration file
	 */
	public static void  setConfigFile (String configFile){
		ConfigFile.configFile = configFile;
	}
	
	/**
	 * Get the path to the configuration file.
	 * @return Path to the configuration file
	 */
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
	
	/**
	 * Return the int value associated to the key. If the value does not exist, or the value is not an int, 
	 * it returns -1.
	 * @param key
	 * @return
	 */
	public static int getValueInt (String key){
		String result = getValue (key);
		int r;
		try{
			r = Integer.parseInt(result);
		} catch (Exception e){
			r = -1;
		}
		return r;
	}
	
}
