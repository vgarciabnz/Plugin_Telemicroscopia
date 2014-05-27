package communication.v4l;

import java.util.Hashtable;

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
		textVideoResolution.setBounds(101, 39, 103, 19);
		this.add(textVideoResolution);

		this.textInterval = new JTextField();
		textInterval.setBounds(101, 91, 103, 19);
		this.add(textInterval);

		this.textCaptureResolution = new JTextField();
		textCaptureResolution.setBounds(101, 265, 103, 19);
		this.add(textCaptureResolution);
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
