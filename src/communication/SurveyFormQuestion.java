package communication;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;

public class SurveyFormQuestion extends JPanel {
	
	public static final int YES_NO = 0;
	public static final int TEXT = 1;
	
	private String name;
	private String question;
	private int type;
	private ButtonGroup bGroup;
	private String answer = "null";
	
	public SurveyFormQuestion (String name, String question, int type){
		setBorder(new CompoundBorder(new LineBorder(new Color(238, 238, 238), 3), new LineBorder(new Color(0, 0, 0), 2, true)));
		setBackground(UIManager.getColor("Button.disabledToolBarBorderBackground"));
		setPreferredSize(new Dimension(400, 100));
		this.name = name;
		this.question = "<html>" + question + "</html>";
		//this.question = question;
		this.type = type;
		
		createYesNoPanel();			
		
	}
	
	private void createYesNoPanel (){
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{300,100};
		gridBagLayout.rowWeights = new double[]{0.9};
		setLayout(gridBagLayout);
		
		JLabel jQuestion = new JLabel();
		jQuestion.setText(question);
		GridBagConstraints gbc_ques = new GridBagConstraints();
		gbc_ques.weighty = 1.0;
		gbc_ques.weightx = 1.0;
		gbc_ques.anchor = GridBagConstraints.WEST;
		gbc_ques.gridx = 0;
		gbc_ques.gridy = 0;
		gbc_ques.fill = GridBagConstraints.HORIZONTAL;
		gbc_ques.insets = new Insets(5,5,5,5);
		add(jQuestion, gbc_ques);
		
		JPanel answer = new JPanel();
		answer.setBackground(UIManager.getColor("Button.disabledToolBarBorderBackground"));
		
		JToggleButton jTrue = new JToggleButton("SI");
		jTrue.setBackground(UIManager.getColor("Button.disabledToolBarBorderBackground"));
		jTrue.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setAnswer("true");
			}
		});
		
		JToggleButton jFalse = new JToggleButton("NO");
		jFalse.setBackground(UIManager.getColor("Button.disabledToolBarBorderBackground"));
		jFalse.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setAnswer("false");
			}
		});
		
		answer.add(jTrue, BorderLayout.LINE_START);
		answer.add(jFalse, BorderLayout.LINE_END);
		
		GridBagConstraints gbc_ans = new GridBagConstraints();
		gbc_ans.anchor = GridBagConstraints.CENTER;
		gbc_ans.gridx = 1;
		gbc_ans.gridy = 0;
		add(answer, gbc_ans);
		
		// Create the button group
		bGroup = new ButtonGroup();
		bGroup.add(jTrue);
		bGroup.add(jFalse);
		
	}
	
	
	public String getAnswer(){
		return answer;
	}
	
	public void setAnswer(String ans){
		answer = ans;
	}
	
	public String getName(){
		return name;
	}

}
