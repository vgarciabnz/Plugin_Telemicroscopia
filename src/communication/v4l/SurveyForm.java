package communication.v4l;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.Lock;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import util.ConfigFile;
import util.MyLogger;
import java.awt.Component;
import javax.swing.border.EmptyBorder;

/**
 * This class represents a JFrame with a survey. It is intended to be used at the end of the program execution, so 
 * when the frame is closed it exits the program. Questions can be add by as SurveyFormQuestion objects.
 * @author ehas
 *
 */
public class SurveyForm extends JFrame {
	
	private final int DEFAULT_FRAME_WIDTH = 300;
	private final int DEFAULT_FRAME_HEIGHT = 300;
	
	private SurveyFormQuestion[] questions = {
			new SurveyFormQuestion("Funcionamiento","¿El equipo ha funcionado correctamente?",0),
			new SurveyFormQuestion("Calidad","¿La calidad de imagen y sonido ha sido adecuada?",0),
			new SurveyFormQuestion("Utilidad","¿Ha servido para obtener un diagnóstico?",0)
	};
		
	private final JPanel questionsPanel = new JPanel();
	
	/**
	 * Creates and shows the form.
	 */
	public SurveyForm (){
		
		setTitle("Formulario de satisfacción");
		
		// I suppose that 12 is the default font size. The frame is scaled
		// accordingly.
		String sizeS = ConfigFile.getValue(ConfigFile.FONT_SIZE);
		int width = DEFAULT_FRAME_WIDTH;
		int height = DEFAULT_FRAME_HEIGHT;
		if (sizeS != null) {
			int size = Integer.parseInt(sizeS);
			width = width * size / 12;
			height = height * size / 12;
		}
		
		setPreferredSize(new Dimension(width,height));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// Create the heading message
		JLabel lblNewLabel = new JLabel("<html>A continuación puede rellenar si lo desea este breve formulario de satisfacción del sistema " +
				"de telemicroscopía.</html>");
		lblNewLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(lblNewLabel, BorderLayout.NORTH);
		
		// Create the questions panel
		questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
		
		for (SurveyFormQuestion ques : questions){
			questionsPanel.add(ques);
		}
		
		JScrollPane questionsScroll = new JScrollPane();
		questionsScroll.setViewportView(questionsPanel);
		questionsScroll.getVerticalScrollBar().setBlockIncrement(40);
		questionsScroll.getVerticalScrollBar().setUnitIncrement(25);
		getContentPane().add(questionsScroll, BorderLayout.CENTER);
		
		//getContentPane().add(questionsPanel, BorderLayout.CENTER);
		
		// Create the bottom buttons.
		JPanel bottomPanel = new JPanel();
		JButton refuse = new JButton("No, gracias");
		refuse.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
			
		});
		
		JButton accept = new JButton("Aceptar");
		accept.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				for (SurveyFormQuestion ques : questions){
					CamServer.logger.info(MyLogger.getLogRecord("SURVEY_FORM") + " " + 
							ques.getName() + "= " + ques.getAnswer());
				}
				System.exit(0);
			}
			
		});
		
		bottomPanel.add(refuse, BorderLayout.LINE_END);
		bottomPanel.add(accept, BorderLayout.LINE_END);
		getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
