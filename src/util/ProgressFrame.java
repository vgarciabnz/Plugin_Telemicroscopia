package util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

/**
 * This class represents a frame with a progress bar in the middle of the screen. It is
 * used to visually represent that a process is under execution.
 * 
 * @author ehas
 *
 */
public class ProgressFrame extends JFrame{
	
	/**
	 * Create a frame with a progress bar.
	 * 
	 * @param title Title of the frame.
	 * @param message Message to show above the progress bar.
	 */
	public ProgressFrame (String title, String message){
		super(title);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container content = this.getContentPane();
		JProgressBar progress = new JProgressBar();
		progress.setIndeterminate(true);
		Border border = BorderFactory.createTitledBorder(message);
		progress.setBorder(border);
		content.add(progress, BorderLayout.NORTH);
		GraphicsEnvironment graphics = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		this.setLocation(graphics.getCenterPoint());
		this.setSize(300, 100);
	}
	
	/**
	 * Create a frame with a progress bar.
	 * 
	 * @param title Title and message of the progress frame.
	 */
	public ProgressFrame (String title){
		this(title,title);
	}
	
	/**
	 * Shows the frame with the progress bar.
	 */
	public void start(){
		this.setVisible(true);
	}
	
	/**
	 * Hides the frame with the progress bar.
	 */
	public void stop(){
		this.setVisible(false);

	}
}
