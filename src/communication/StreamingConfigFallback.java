package communication;

import java.awt.GridBagConstraints;
import java.util.Hashtable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;

public class StreamingConfigFallback extends StreamingConfig {

	private JTextField textVideoResolution;
	private JTextField textInterval;
	private JTextField textCaptureResolution;

	public StreamingConfigFallback(String device) {
		super(device);
	}

	@Override
	protected void initComboBoxes() {
		this.textVideoResolution = new JTextField();
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.gridx = 1;
		gbc1.gridy = 1;
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		this.add(textVideoResolution,  gbc1);
	

		this.textInterval = new JTextField();	
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.gridx = 1;
		gbc2.gridy = 2;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		this.add(textInterval, gbc2);

		
		this.textCaptureResolution = new JTextField();		
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.gridx = 1;
		gbc3.gridy = 9;
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		this.add(textCaptureResolution, gbc3);
	}

	@Override
	protected void setValuesComboBoxes() {
		// Highlight the video resolution in use
		String videoResolution = CamServer.videoWidth + "x"
				+ CamServer.videoHeight;
		this.textVideoResolution.setText(videoResolution);

		// Highlight the frame interval in use
		String frameInterval = CamServer.intervalNum + "/"
				+ CamServer.intervalDen;
		this.textInterval.setText(frameInterval);

		// Highlight the capture resolution in use
		String captureResolution = CamServer.captureWidth + "x"
				+ CamServer.captureHeight;
		this.textCaptureResolution.setText(captureResolution);
	}

	@Override
	protected void setDefaultSettingsComboBoxes() {
		Hashtable<String, String> defaultSettings = Settings
				.getDefaultSettings(dev);
		String videoResolution = defaultSettings.get("videoResolution");
		String captureResolution = defaultSettings.get("captureResolution");
		String interval = defaultSettings.get("interval");

		// Highlight the default video resolution
		this.textVideoResolution.setText(videoResolution);

		// Highlight the default video resolution
		this.textCaptureResolution.setText(captureResolution);

		// Highlight the frame interval in use
		this.textInterval.setText(interval);
	}

	@Override
	protected void saveValuesComboBoxes() {
		// Save video resolution values
		CamServer.videoWidth = Integer.parseInt(textVideoResolution.getText()
				.split("x")[0]);
		CamServer.videoHeight = Integer.parseInt(textVideoResolution.getText()
				.split("x")[1].split(" ")[0]);

		// Save capture resolution values
		CamServer.captureWidth = Integer.parseInt(textCaptureResolution
				.getText().split("x")[0]);
		CamServer.captureHeight = Integer.parseInt(textCaptureResolution
				.getText().split("x")[1].split(" ")[0]);

		// Save frame rate values
		CamServer.intervalNum = Integer.parseInt(textInterval.getText().split(
				"/")[0]);
		CamServer.intervalDen = Integer.parseInt(textInterval.getText().split(
				"/")[1]);

	}
}
