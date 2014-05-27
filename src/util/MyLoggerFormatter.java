package util;

import java.sql.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * This class extends Formatter class and is used to customize the AppTm4l log
 * appearance
 * 
 * @author ehas
 * 
 */
public class MyLoggerFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		Date date = new Date(record.getMillis());
		String date_string = date.toString() + " " + record.getMillis();
		String message = "[" + date_string + "] " + record.getMessage()
				+ "\n";
		return message;
	}

}
