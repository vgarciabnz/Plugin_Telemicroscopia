package util;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * This class extends BasicComboBoxRenderer and is used to provide a custom layout to comboboxes. In particular,
 * it is used to show a short name of available resolutions and intervals.
 * @author ehas
 *
 */
public class MyComboBoxRenderer extends BasicComboBoxRenderer {
	
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		setText(value.toString().split(" ")[0]);
		
		return this;
	}

}
