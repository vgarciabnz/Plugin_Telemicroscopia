package settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import communication.CamServer;

import util.MyComboBoxRenderer;

import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

public class StreamingConfig extends JPanel {

	private JComboBox textVideoResolution;
	private JComboBox textInterval;
	private JComboBox textCaptureResolution;
	private JTextField textPort;
	private JTextField textQmin;
	private JTextField textQmax;
	private JTextField textGop;
	private JTextField textBps;

	protected String dev;

	public StreamingConfig(String device) {
		dev = device;
		initCommon();
		initComboBoxes();
	}

	private void initCommon() {
		setLayout(new GridBagLayout());
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.gridx = 0;
		gbc1.gridy = 0;
		gbc1.gridwidth = 2;
		gbc1.anchor = GridBagConstraints.WEST;
		gbc1.insets = new Insets(0, 0, 5, 0);
		JLabel lblOpcionesDeVdeo = new JLabel("Opciones de vídeo:");
		this.add(lblOpcionesDeVdeo, gbc1);
		
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridx = 0;
		gbc2.gridy = 1;
		gbc2.anchor = GridBagConstraints.WEST;
		JLabel lblResolucin_1 = new JLabel("Resolución:");
		this.add(lblResolucin_1, gbc2);
				
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.gridx = 0;
		gbc3.gridy = 2;
		gbc3.anchor = GridBagConstraints.WEST;
		JLabel lblTasa = new JLabel("Tasa:");
		this.add(lblTasa, gbc3);
		
		GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.gridx = 0;
		gbc4.gridy = 3;
		gbc4.anchor = GridBagConstraints.WEST;
		JLabel lblPuerto = new JLabel("Puerto:");
		this.add(lblPuerto, gbc4);
		
		GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.gridx = 1;
		gbc5.gridy = 3;
		gbc5.fill = GridBagConstraints.HORIZONTAL;
		textPort = new JTextField();
		this.add(textPort, gbc5);
		
		GridBagConstraints gbc6 = new GridBagConstraints();
		gbc6.gridx = 0;
		gbc6.gridy = 4;
		gbc6.anchor = GridBagConstraints.WEST;
		JLabel lblQmin = new JLabel("Qmin (0-31):");
		this.add(lblQmin, gbc6);
		
		GridBagConstraints gbc7 = new GridBagConstraints();
		gbc7.gridx = 1;
		gbc7.gridy = 4;
		gbc7.fill = GridBagConstraints.HORIZONTAL;
		textQmin = new JTextField();
		this.add(textQmin, gbc7);
		
		GridBagConstraints gbc8 = new GridBagConstraints();
		gbc8.gridx = 0;
		gbc8.gridy = 5;
		gbc8.anchor = GridBagConstraints.WEST;
		JLabel lblNewLabel = new JLabel("Qmax (0-31):");
		this.add(lblNewLabel, gbc8);
		
		GridBagConstraints gbc9 = new GridBagConstraints();
		gbc9.gridx = 1;
		gbc9.gridy = 5;
		gbc9.fill = GridBagConstraints.HORIZONTAL;
		textQmax = new JTextField();
		this.add(textQmax, gbc9);
		
		GridBagConstraints gbc10 = new GridBagConstraints();
		gbc10.gridx = 0;
		gbc10.gridy = 6;
		gbc10.anchor = GridBagConstraints.WEST;
		JLabel lblGop = new JLabel("GOP:");
		this.add(lblGop, gbc10);
		
		GridBagConstraints gbc11 = new GridBagConstraints();
		gbc11.gridx = 1;
		gbc11.gridy = 6;
		gbc11.fill = GridBagConstraints.HORIZONTAL;
		textGop = new JTextField();
		this.add(textGop, gbc11);
		
		GridBagConstraints gbc12 = new GridBagConstraints();
		gbc12.gridx = 0;
		gbc12.gridy = 7;
		gbc12.anchor = GridBagConstraints.WEST;
		JLabel lblBps = new JLabel("bps:");
		this.add(lblBps, gbc12);
		
		GridBagConstraints gbc13 = new GridBagConstraints();
		gbc13.gridx = 1;
		gbc13.gridy = 7;
		gbc13.fill = GridBagConstraints.HORIZONTAL;
		textBps = new JTextField();
		textBps.setText("-1");
		this.add(textBps, gbc13);
		
		GridBagConstraints gbc14 = new GridBagConstraints();
		gbc14.gridx = 0;
		gbc14.gridy = 8;
		gbc14.gridwidth = 2;
		gbc14.anchor = GridBagConstraints.WEST;
		gbc14.insets = new Insets(10, 0, 5, 0);
		JLabel lblOpcionesDeCaptura = new JLabel("Opciones de captura:");
		this.add(lblOpcionesDeCaptura, gbc14);
		
		GridBagConstraints gbc15 = new GridBagConstraints();
		gbc15.gridx = 0;
		gbc15.gridy = 9;
		gbc15.anchor = GridBagConstraints.WEST;
		JLabel lblResolucin = new JLabel("Resolución:");
		this.add(lblResolucin, gbc15);
		
	}

	protected void initComboBoxes() {
		
		textVideoResolution = new JComboBox();
		textVideoResolution.setRenderer(new MyComboBoxRenderer());
		textVideoResolution.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				textInterval.setModel(new DefaultComboBoxModel(
						Settings.listAvailableIntervals((DiscreteResolution) textVideoResolution
								.getSelectedItem())));
			}
		});
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.gridx = 1;
		gbc1.gridy = 1;
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		this.add(textVideoResolution,  gbc1);
		textVideoResolution.setModel(new DefaultComboBoxModel(Settings
				.listAvailableResolutions(dev)));
		

		textInterval = new JComboBox();
		textInterval.setRenderer(new MyComboBoxRenderer());
		textInterval
		.setModel(new DefaultComboBoxModel(
				Settings.listAvailableIntervals((DiscreteResolution) textVideoResolution
						.getSelectedItem())));
		
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridx = 1;
		gbc2.gridy = 2;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		this.add(textInterval, gbc2);
				
		textCaptureResolution = new JComboBox();
		textCaptureResolution.setRenderer(new MyComboBoxRenderer());
		
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.gridx = 1;
		gbc3.gridy = 9;
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		this.add(textCaptureResolution, gbc3);
		textCaptureResolution.setModel(new DefaultComboBoxModel(Settings
			.listAvailableResolutions(dev)));
		
	}

	public void setValues() {

		// Write variables in use
		textPort.setText("" + CamServer.getPort());
		textQmin.setText("" + CamServer.qmin);
		textQmax.setText("" + CamServer.qmax);
		textGop.setText("" + CamServer.gop);
		textBps.setText("" + CamServer.bps);

		setValuesComboBoxes();
	}

	protected void setValuesComboBoxes() {
		// Highlight the video resolution in use
		String videoResolution = CamServer.videoWidth + "x"
				+ CamServer.videoHeight;
		ComboBoxModel videoResolutions = textVideoResolution.getModel();
		for (int i = 0; i < videoResolutions.getSize(); i++) {
			if (videoResolutions.getElementAt(i).toString()
					.startsWith(videoResolution)) {
				textVideoResolution.setSelectedItem(videoResolutions
						.getElementAt(i));
			}
		}

		// Highlight the frame interval in use
		String frameInterval = CamServer.intervalNum + "/"
				+ CamServer.intervalDen;
		ComboBoxModel frameIntervals = textInterval.getModel();
		for (int i = 0; i < frameIntervals.getSize(); i++) {
			if (frameIntervals.getElementAt(i).toString()
					.startsWith(frameInterval)) {
				textInterval.setSelectedItem(frameIntervals.getElementAt(i));
			}
		}

		// Highlight the capture resolution in use
		String captureResolution = CamServer.captureWidth + "x"
				+ CamServer.captureHeight;
		ComboBoxModel captureResolutions = textCaptureResolution.getModel();
		for (int i = 0; i < captureResolutions.getSize(); i++) {
			if (captureResolutions.getElementAt(i).toString()
					.startsWith(captureResolution)) {
				textCaptureResolution.setSelectedItem(captureResolutions
						.getElementAt(i));
			}
		}
	}

	public void setDefaultSettings() {

		// Set default values
		textPort.setText("" + CamServer.DEFAULT_PORT);
		textQmin.setText("" + CamServer.DEFAULT_QMIN);
		textQmax.setText("" + CamServer.DEFAULT_QMAX);
		textGop.setText("" + CamServer.DEFAULT_GOP);
		textBps.setText(CamServer.DEFAULT_BPS);

		setDefaultSettingsComboBoxes();

	}

	protected void setDefaultSettingsComboBoxes() {
		Hashtable<String, String> defaultSettings = Settings
				.getDefaultSettings(dev);
		String videoResolution = defaultSettings.get("videoResolution");
		String captureResolution = defaultSettings.get("captureResolution");
		String interval = defaultSettings.get("interval");

		// Highlight the default video resolution
		ComboBoxModel videoResolutions = textVideoResolution.getModel();
		for (int i = 0; i < videoResolutions.getSize(); i++) {
			if (videoResolutions.getElementAt(i).toString()
					.startsWith(videoResolution)) {
				textVideoResolution.setSelectedItem(videoResolutions
						.getElementAt(i));
			}
		}

		// Highlight the default capture resolution
		ComboBoxModel captureResolutions = textCaptureResolution.getModel();
		for (int i = 0; i < captureResolutions.getSize(); i++) {
			if (captureResolutions.getElementAt(i).toString()
					.startsWith(captureResolution)) {
				textCaptureResolution.setSelectedItem(captureResolutions
						.getElementAt(i));
			}
		}

		// Highlight the frame interval in use
		ComboBoxModel frameIntervals = textInterval.getModel();
		for (int i = 0; i < frameIntervals.getSize(); i++) {
			if (frameIntervals.getElementAt(i).toString().startsWith(interval)) {
				textInterval.setSelectedItem(frameIntervals.getElementAt(i));
			}
		}
	}

	public void saveValues() {

		// Save variables
		CamServer.port = Integer.parseInt(textPort.getText());
		CamServer.qmin = Integer.parseInt(textQmin.getText());
		CamServer.qmax = Integer.parseInt(textQmax.getText());
		CamServer.gop = Integer.parseInt(textGop.getText());
		CamServer.bps = textBps.getText();

		saveValuesComboBoxes();

	}

	protected void saveValuesComboBoxes() {
		// Save video resolution values
		CamServer.videoWidth = Integer.parseInt(textVideoResolution
				.getSelectedItem().toString().split("x")[0]);
		CamServer.videoHeight = Integer.parseInt(textVideoResolution
				.getSelectedItem().toString().split("x")[1].split(" ")[0]);

		// Save capture resolution values
		CamServer.captureWidth = Integer.parseInt(textCaptureResolution
				.getSelectedItem().toString().split("x")[0]);
		CamServer.captureHeight = Integer.parseInt(textCaptureResolution
				.getSelectedItem().toString().split("x")[1].split(" ")[0]);

		// Save frame rate values
		CamServer.intervalNum = Integer.parseInt(textInterval
				.getSelectedItem().toString().split("/")[0]);
		CamServer.intervalDen = Integer.parseInt(textInterval
				.getSelectedItem().toString().split("/")[1]);
	}
}
