package util;

import java.util.HashMap;
import java.util.Map;

/**
 * It provides a static method to get word translations.
 * 
 * @author ehas
 *
 */
public class Dictionary {

	private static Map<String, String> dictionary = new HashMap<String,String>(){
		{
			put("Brightness", "Brillo");
			put("Contrast", "Contraste");
			put("Sharpness", "Nitidez");
			put("Auto Gain", "Ganancia Automática");
			put("Power Line Frequency", "Frecuencia de la corriente");
			put("Saturation", "Saturación");
			put("Hue", "Tono");
			put("Backlight Compensation", "Compensación luz de fondo");
			
		}
	};	
		
	public static String translateWord(String word){ 
		if (dictionary.containsKey(word))
			return dictionary.get(word);
		return word;
	
	};
}
