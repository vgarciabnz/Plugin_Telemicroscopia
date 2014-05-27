package util;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class MyComboBoxRenderer extends BasicComboBoxRenderer {
	
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		setText(value.toString().split(" ")[0]);
		
		return this;
	}

}
