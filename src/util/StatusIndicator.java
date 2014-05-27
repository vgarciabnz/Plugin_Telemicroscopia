package util;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JLabel;

/**
 * This class extends the JLabel class to create a JLabel with three possible
 * status: DOWN (red color), PROGRESS (orange color) and UP (GREEN color). This
 * is used to monitor the state of the video server.
 * 
 * @author ehas
 * 
 */
public class StatusIndicator extends JButton {

	public static final int DOWN = 0;
	public static final int PROGRESS = 1;
	public static final int UP = 2;
	
	private int state = DOWN;

	/**
	 * Create a JLabel with the text provided as argument
	 * 
	 * @param name
	 *            Text to be displayed in the JLabel
	 */
	public StatusIndicator(String name) {
		super(name);
	}

	/**
	 * Change the state of the StatusIndicator object
	 * 
	 * @param state
	 *            Values allowed: StatusIndicator.DOWN, StatusIndicator.PROGRESS
	 *            and StatusIndicator.UP
	 */
	public void setState(int state) {
		switch (state) {
		case DOWN:
			this.setBackground(new Color(255, 120, 120));
			this.setToolTipText("Camara desactivada");
			this.state = DOWN;
			break;
		case PROGRESS:
			this.setBackground(new Color(255, 165, 0));
			this.setToolTipText("Camara en progreso");
			this.state = PROGRESS;
			break;
		case UP:
			this.setBackground(new Color(154, 205, 50));
			this.setToolTipText("Camara activada");
			this.state = UP;
			break;
		}
	}

	public int getState(){
		return state;
	}
}