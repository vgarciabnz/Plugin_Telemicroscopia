package communication;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class FrameCamServer extends JFrame implements ActionListener{

	private static Frame instance;
	private JLabel l = new JLabel();
	private JButton capturarButton = new JButton("Capturar");
	
	public static FrameCamServer getInstance() {
		return (FrameCamServer)instance;
	}


	public JLabel getL() {
		return l;
	}


	public void setL(JLabel l) {
		this.l = l;
	}


	public FrameCamServer(Integer width,Integer height) throws HeadlessException {
		super();
		
		capturarButton.addActionListener(this);
        JPanel jPanelButton = new JPanel();
        jPanelButton.add(capturarButton);
		
        l.setPreferredSize(new Dimension(width, height));
        JPanel jPanelLabel = new JPanel();
        jPanelLabel.add(l);
        
        this.setLayout(new BoxLayout(this.getContentPane(),BoxLayout.PAGE_AXIS));
		this.getContentPane().add(jPanelLabel);
		this.getContentPane().add(jPanelButton);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //this.setDefaultCloseOperation(0);
        this.setVisible(true);
        instance = this;
        
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==capturarButton){
			
			CamServer.setLocalCapture(true);
			
		}
				
	}
	
	
        
        
        
        
    
}
