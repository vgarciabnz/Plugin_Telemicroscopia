package communication;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import au.edu.jcu.v4l4j.DeviceInfo;
import au.edu.jcu.v4l4j.FrameInterval.DiscreteInterval;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.ImageFormatList;
import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import javax.swing.JCheckBox;
import java.awt.GridLayout;
import javax.swing.BoxLayout;

import util.ConfigFile;


import java.awt.Insets;
import javax.swing.border.MatteBorder;
import java.awt.Font;

/**
 * This class represents a control panel of the streaming options: selection of
 * the camera, selection of video streaming parameters and adjusting of camera
 * controls (bright, gamma, etc). It shows a JFrame that is split into three
 * parts: the top one shows information about the camera and allows to choose a
 * different one (this is implemented in Settings class); the medium-left one is
 * about streaming parameters such as resolution, framerate, group of pictures,
 * etc (this is implemented in StreamingConfig or StreamingConfigFallback class,
 * depending on the camera characteristics); and the medium-right one is about
 * camera controls such as bright, gamma, contrast, etc (this is implemented in
 * CameraAdvancedControls class). This class also provides some static methods
 * that retrieves information about the cameras connected to the computer.
 * 
 * @author ehas
 * 
 */
public class Settings extends JFrame {

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;
	private static final int DEFAULT_INTERVAL_NUM = 1;
	private static final int DEFAULT_INTERVAL_DEN = 10;

	private static Vector<String> deviceFiles;

	private JPanel contentPane;
	private JPanel centerPanel;
	private StreamingConfig videoOptionPanel;
	private JScrollPane scrollPaneEast;
	private JScrollPane scrollPaneWest;
	private JTextField textVideoDeviceFile;

	private static String v4lSysfsPath = "/sys/class/video4linux/";
	private String videoDeviceFile = "---";
	private JComboBox deviceComboBox;
	public Settings frame;
	private JTextField textName;
	private JTextField textIPAddress;
	private JTextField textUserName;
	private JTextField textUserPassword;
	private JToggleButton security;
	private JLabel securitySelected;
	
	private final int DEFAULT_FRAME_WIDTH = 420;
	private final int DEFAULT_FRAME_HEIGHT = 420;

	/**
	 * Create the frame.
	 */
	public Settings() {
		setTitle("Panel de Control");

		// Check if any device has been selected
		if (CamServer.getVideoDevice() != null) {
			videoDeviceFile = CamServer.getVideoDevice().getDevicefile();
		} else {
			Object[] devices = listV4LDeviceFiles();
			if (devices.length == 0) {

			} else {
				videoDeviceFile = devices[0].toString();
			}
		}

		// I suppose that 12 is the default font size. The frame is scaled accordingly.
		String sizeS = ConfigFile.getValue(ConfigFile.FONT_SIZE);
		int width = DEFAULT_FRAME_WIDTH;
		int height = DEFAULT_FRAME_HEIGHT;
		if (sizeS != null){
			int size = Integer.parseInt(sizeS);
			width = width * size / 12; 
			height = (height/2) * (1 + (size / 12));
		} 
		
		setBounds(100,100,width,height);
		setLocationRelativeTo(null);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBackground(Color.LIGHT_GRAY);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		initGUI();
		setValues();

	}

	private void initGUI() {

		JPanel deviceInfoPanel = new JPanel();
		deviceInfoPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		deviceInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		deviceInfoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		deviceInfoPanel.setToolTipText("Dispositivo");
		contentPane.setLayout(new BorderLayout(0, 0));

		GridBagLayout gbl_deviceInfoPanel = new GridBagLayout();
		gbl_deviceInfoPanel.columnWidths = new int[] {10, 20, 10, 20};
		gbl_deviceInfoPanel.columnWeights = new double[] { 1, 1, 1, 1};
		deviceInfoPanel.setLayout(gbl_deviceInfoPanel);

		deviceComboBox = new JComboBox();
		deviceComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				videoDeviceFile = deviceComboBox.getSelectedItem().toString();
				try {
					VideoDevice dev = new VideoDevice(videoDeviceFile);
					textName.setText(dev.getDeviceInfo().getName());
				} catch (V4L4JException e) {
					e.printStackTrace();
				}

				if (videoOptionPanel != null) {
					contentPane.remove(videoOptionPanel);
				}

				// Add panel with the streaming parameters
				if (needFallbackMode(videoDeviceFile)) {
					videoOptionPanel = new StreamingConfigFallback(
							videoDeviceFile);
				} else {
					videoOptionPanel = new StreamingConfig(videoDeviceFile);
				}
				// videoOptionPanel.setBounds(5, 75, 233, 292);
				// videoOptionPanel.setLayout(null);
				// contentPane.add(videoOptionPanel, BorderLayout.LINE_START);
				// Set default settings
				videoOptionPanel.setDefaultSettings();
				scrollPaneWest.setViewportView(videoOptionPanel);

				// Add panel with the camera controls
				scrollPaneEast.setViewportView(new CameraAdvancedControls(
						videoDeviceFile));

			}
		});

		JLabel lblDispositivo = new JLabel("En uso:");
		lblDispositivo.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblDispositivo = new GridBagConstraints();
		gbc_lblDispositivo.fill = GridBagConstraints.BOTH;
		gbc_lblDispositivo.insets = new Insets(0, 0, 5, 5);
		gbc_lblDispositivo.gridx = 0;
		gbc_lblDispositivo.gridy = 0;
		deviceInfoPanel.add(lblDispositivo, gbc_lblDispositivo);

		textVideoDeviceFile = new JTextField();
		textVideoDeviceFile.setText(videoDeviceFile);
		textVideoDeviceFile.setEditable(false);
		GridBagConstraints gbc_textVideoDeviceFile = new GridBagConstraints();
		gbc_textVideoDeviceFile.fill = GridBagConstraints.BOTH;
		gbc_textVideoDeviceFile.insets = new Insets(0, 0, 5, 5);
		gbc_textVideoDeviceFile.gridx = 1;
		gbc_textVideoDeviceFile.gridy = 0;
		deviceInfoPanel.add(textVideoDeviceFile, gbc_textVideoDeviceFile);

		JLabel lblPosibles = new JLabel("Posibles:");
		GridBagConstraints gbc_lblPosibles = new GridBagConstraints();
		gbc_lblPosibles.fill = GridBagConstraints.BOTH;
		gbc_lblPosibles.insets = new Insets(0, 0, 5, 5);
		gbc_lblPosibles.gridx = 2;
		gbc_lblPosibles.gridy = 0;
		deviceInfoPanel.add(lblPosibles, gbc_lblPosibles);
		
		GridBagConstraints gbc_deviceComboBox = new GridBagConstraints();
		gbc_deviceComboBox.fill = GridBagConstraints.BOTH;
		gbc_deviceComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_deviceComboBox.gridx = 3;
		gbc_deviceComboBox.gridy = 0;
		deviceInfoPanel.add(deviceComboBox, gbc_deviceComboBox);

		JLabel lblIp = new JLabel("IP (eth0):");
		GridBagConstraints gbc_lblIp = new GridBagConstraints();
		gbc_lblIp.fill = GridBagConstraints.BOTH;
		gbc_lblIp.insets = new Insets(0, 0, 0, 5);
		gbc_lblIp.gridx = 0;
		gbc_lblIp.gridy = 1;
		deviceInfoPanel.add(lblIp, gbc_lblIp);

		textIPAddress = new JTextField();
		textIPAddress.setEditable(false);
		GridBagConstraints gbc_textIPAddress = new GridBagConstraints();
		gbc_textIPAddress.fill = GridBagConstraints.BOTH;
		gbc_textIPAddress.insets = new Insets(0, 0, 0, 5);
		gbc_textIPAddress.gridx = 1;
		gbc_textIPAddress.gridy = 1;
		deviceInfoPanel.add(textIPAddress, gbc_textIPAddress);

		JLabel lblNombre = new JLabel("Nombre:");
		GridBagConstraints gbc_lblNombre = new GridBagConstraints();
		gbc_lblNombre.fill = GridBagConstraints.BOTH;
		gbc_lblNombre.insets = new Insets(0, 0, 0, 5);
		gbc_lblNombre.gridx = 2;
		gbc_lblNombre.gridy = 1;
		deviceInfoPanel.add(lblNombre, gbc_lblNombre);

		textName = new JTextField();
		textName.setEditable(false);
		GridBagConstraints gbc_textName = new GridBagConstraints();
		gbc_textName.fill = GridBagConstraints.BOTH;
		gbc_textName.gridx = 3;
		gbc_textName.gridy = 1;
		deviceInfoPanel.add(textName, gbc_textName);

		// contentPane.setLayout(null);
		contentPane.add(deviceInfoPanel, BorderLayout.NORTH);

		JPanel panel = new JPanel();

		JButton btnAceptar = new JButton("Aceptar/reiniciar");
		btnAceptar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!saveValues()) {
					return;
				}
				dispose();
				if (CamServer.active) {
					CamServer.stop();
					try {
						CamServer.start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		// btnAceptar.setBounds(12, 12, 163, 25);
		panel.add(btnAceptar);
		contentPane.add(panel, BorderLayout.SOUTH);

		centerPanel = new JPanel();
		GridBagLayout gbl_centerPanel = new GridBagLayout();
		gbl_centerPanel.columnWeights = new double[] { 1, 1 };
		gbl_centerPanel.rowWeights = new double[] { 1, 1, 1, 1, 0.5 };
		centerPanel.setLayout(gbl_centerPanel);
		contentPane.add(centerPanel, BorderLayout.CENTER);

		GridBagConstraints c = new GridBagConstraints();

		scrollPaneEast = new JScrollPane();
		scrollPaneEast.setBorder(new MatteBorder(5, 5, 5, 5, (Color) Color.LIGHT_GRAY));
		scrollPaneEast.getVerticalScrollBar().setBlockIncrement(40);
		scrollPaneEast.getVerticalScrollBar().setUnitIncrement(25);

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 4;
		c.gridwidth = 1;
		centerPanel.add(scrollPaneEast, c);

		scrollPaneWest = new JScrollPane();
		scrollPaneWest.setBorder(new MatteBorder(5, 5, 5, 5, (Color) Color.LIGHT_GRAY));
		scrollPaneWest.getVerticalScrollBar().setBlockIncrement(40);
		scrollPaneWest.getVerticalScrollBar().setUnitIncrement(25);

		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.BOTH;
		c2.gridx = 0;
		c2.gridy = 0;
		c2.gridheight = 4;
		centerPanel.add(scrollPaneWest, c2);

		JPanel authorizationPane = new JPanel();
		authorizationPane.setBorder(new MatteBorder(5, 5, 5, 5, (Color) Color.LIGHT_GRAY));

		GridBagConstraints c3 = new GridBagConstraints();
		c3.fill = GridBagConstraints.BOTH;
		c3.gridx = 0;
		c3.gridy = 4;
		c3.gridheight = 1;
		c3.gridwidth = 2;
		centerPanel.add(authorizationPane, c3);

		GridBagLayout gbl_auth = new GridBagLayout();
		gbl_auth.columnWeights = new double[]{0.2, 0.5, 1};
		authorizationPane.setLayout(gbl_auth);
		
		GridBagConstraints gbc_auth1 = new GridBagConstraints();
		gbc_auth1.fill = GridBagConstraints.BOTH;
		gbc_auth1.gridx = 0;
		gbc_auth1.gridy = 0;
		gbc_auth1.insets = new Insets(5, 5, 5, 5);
		
		security = new JToggleButton("Seguridad");
		security.setHorizontalAlignment(SwingConstants.LEFT);
		authorizationPane.add(security, gbc_auth1);
		security.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (((JToggleButton) arg0.getSource()).isSelected()) {
					textUserName.setEnabled(true);
					textUserPassword.setEnabled(true);
					securitySelected.setText("Activada");
				} else {
					textUserName.setEnabled(false);
					textUserPassword.setEnabled(false);
					securitySelected.setText("Desactivada");
				}
			}

		});

		GridBagConstraints gbc_auth2 = new GridBagConstraints();
		gbc_auth2.fill = GridBagConstraints.BOTH;
		gbc_auth2.gridx = 1;
		gbc_auth2.gridy = 0;
		gbc_auth2.insets = new Insets(5, 5, 5, 5);
		JLabel lblUserName = new JLabel("Nombre:");
		lblUserName.setHorizontalAlignment(SwingConstants.LEFT);
		authorizationPane.add(lblUserName, gbc_auth2);

		GridBagConstraints gbc_auth3 = new GridBagConstraints();
		gbc_auth3.fill = GridBagConstraints.BOTH;
		gbc_auth3.gridx = 2;
		gbc_auth3.gridy = 0;
		gbc_auth3.insets = new Insets(5, 5, 5, 5);
		textUserName = new JTextField();
		authorizationPane.add(textUserName, gbc_auth3);

		GridBagConstraints gbc_auth4 = new GridBagConstraints();
		gbc_auth4.fill = GridBagConstraints.BOTH;
		gbc_auth4.gridx = 1;
		gbc_auth4.gridy = 1;
		gbc_auth4.insets = new Insets(5, 5, 5, 5);
		JLabel lblUserPassword = new JLabel("Contraseña:");
		lblUserPassword.setHorizontalAlignment(SwingConstants.LEFT);
		authorizationPane.add(lblUserPassword, gbc_auth4);

		GridBagConstraints gbc_auth5 = new GridBagConstraints();
		gbc_auth5.fill = GridBagConstraints.BOTH;
		gbc_auth5.gridx = 2;
		gbc_auth5.gridy = 1;
		gbc_auth5.insets = new Insets(5, 5, 5, 5);
		textUserPassword = new JTextField();
		authorizationPane.add(textUserPassword, gbc_auth5);

		GridBagConstraints gbc_auth6 = new GridBagConstraints();
		gbc_auth6.fill = GridBagConstraints.BOTH;
		gbc_auth6.gridx = 0;
		gbc_auth6.gridy = 1;
		gbc_auth6.anchor = GridBagConstraints.WEST;
		gbc_auth6.insets = new Insets(5, 5, 5, 5);
		securitySelected = new JLabel("Desactivada");
		securitySelected.setHorizontalAlignment(SwingConstants.LEFT);
		authorizationPane.add(securitySelected, gbc_auth6);

	}

	private void setValues() {

		try {
			String IPAddress = getIPAddress();
			textIPAddress.setText(IPAddress);
		} catch (Exception e1) {
			textIPAddress.setText("Error con IP en eth0");
		}

		Object[] devices = listV4LDeviceFiles();
		deviceComboBox.setModel(new DefaultComboBoxModel(devices));

		// Highlight the device in use
		for (Object device : devices) {
			if (device.toString().equals(videoDeviceFile)) {
				deviceComboBox.setSelectedItem(device);
				try {
					VideoDevice vd = new VideoDevice(videoDeviceFile);
					String name = vd.getDeviceInfo().getName();
					textName.setText(name);
				} catch (V4L4JException e) {
					e.printStackTrace();
				}
			}
		}
		videoOptionPanel.setValues();

		// Authorization panel
		if (CamServer.authentication) {
			security.doClick();
		} else {
			textUserName.setEnabled(false);
			textUserPassword.setEnabled(false);
		}
		textUserName.setText(CamServer.userName);
		textUserPassword.setText(CamServer.userPassword);

	}

	private static Object[] listV4LDeviceFiles() {

		if (deviceFiles == null) {

			deviceFiles = new Vector<String>();
			File dir = new File(v4lSysfsPath);
			String[] files = dir.list();

			for (String file : files) {
				// the following test the presence of "video" in
				// each file name - not very portable - relying on HAL
				// would be much better ...
				if (file.indexOf("video") != -1) {
					File inputDir = new File(v4lSysfsPath + "/" + file
							+ "/device/driver/module");
					if (inputDir.exists()) {
						deviceFiles.add("/dev/" + file);
					}
				}

			}

			if (deviceFiles.size() == 0) {
				System.err.println("Unable to detect any V4L device file\n"
						+ "Set the 'test.device' property to a valid\nvideo "
						+ "device file and run this program again ");
				System.exit(0);
			}
		}

		return deviceFiles.toArray();
	}

	/**
	 * This class returns an array with the available resolutions for a camera.
	 * If it is not possible to retrieve that information, it returns null.
	 * 
	 * @param dev
	 *            Camera to obtain the available resolution for.
	 * @return Array of available resolutions. Null if not possible to retrieve
	 *         that information.
	 */
	public static DiscreteResolution[] listAvailableResolutions(String dev) {
		try {
			VideoDevice vd = new VideoDevice(dev);
			DeviceInfo devInfo = vd.getDeviceInfo();
			ImageFormatList formatList = devInfo.getFormatList();
			List<ImageFormat> imageFormats = formatList.getNativeFormats();
			List<DiscreteResolution> resolutionList = imageFormats.get(0)
					.getResolutionInfo().getDiscreteResolutions();
			Collections.sort(resolutionList, new ResolutionComparator());
			DiscreteResolution[] resolutions = resolutionList
					.toArray(new DiscreteResolution[resolutionList.size()]);

			return resolutions;

		} catch (V4L4JException e) {
			return null;
		}
	}

	private static class ResolutionComparator implements
			Comparator<DiscreteResolution> {
		@Override
		public int compare(DiscreteResolution s, DiscreteResolution t) {
			if (s.getWidth() < t.getWidth()) {
				return -1;
			} else if (s.getWidth() > t.getWidth()) {
				return 1;
			} else {
				if (s.getHeight() < t.getHeight()) {
					return -1;
				} else if (s.getHeight() > t.getHeight()) {
					return 1;
				} else {
					return 0;
				}
			}
		}
	}

	/**
	 * This class returns an array with the available intervals associated to a
	 * DiscreteResolution object. If it is not possible to obtain that
	 * information, it returns null.
	 * 
	 * @param disRes
	 *            DiscreteResolution object that intervals are associated to.
	 * @return Array of available intervals. Null if not possible to retrieve
	 *         that information.
	 */
	public static DiscreteInterval[] listAvailableIntervals(
			DiscreteResolution disRes) {

		try {
			List<DiscreteInterval> intervalList = disRes.getFrameInterval()
					.getDiscreteIntervals();
			DiscreteInterval[] intervals = intervalList
					.toArray(new DiscreteInterval[intervalList.size()]);

			return intervals;
		} catch (Exception e) {
			return null;
		}
	}

	private boolean saveValues() {
		// Save authorization options
		if (security.isSelected()) {
			if (textUserName.getText().trim().isEmpty()
					|| textUserPassword.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(null,
						"El usuario o la contraseña están vacíos.",
						"Fallo al guardar", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			CamServer.authentication = true;
			CamServer.userName = textUserName.getText();
			CamServer.userPassword = textUserPassword.getText();
		} else {
			CamServer.authentication = false;
		}

		// Save the new video device
		CamServer.setVideoDevice(deviceComboBox.getSelectedItem().toString());

		videoOptionPanel.saveValues();

		return true;
	}

	/**
	 * Returns a string with the path to the default device. It gets the list of
	 * available devices and chooses the first one. If no devices are available,
	 * it returns null.
	 * 
	 * @return String with the default device path. Null if no devices are
	 *         available.
	 */
	public static String getDefaultVideoDevice() {
		Object[] devices = listV4LDeviceFiles();
		if (devices.length == 0) {
			return null;
		} else {
			return devices[0].toString();
		}
	}

	/**
	 * This class returns a hashtable with three elements identified by
	 * "videoResolution", "captureResolution" and "interval" labels that
	 * represent default settings for the camera.
	 * 
	 * @param dev
	 *            Camera to obtain default settings for.
	 * @return Hashtable with the default settings.
	 */
	public static Hashtable<String, String> getDefaultSettings(String dev) {

		Hashtable<String, String> settings = new Hashtable<String, String>();

		DiscreteResolution[] resolutions = listAvailableResolutions(dev);

		if (!needFallbackMode(dev)) {

			DiscreteResolution defaultVideoResolution = resolutions[0];
			DiscreteResolution defaultCaptureResolution = resolutions[resolutions.length - 1];

			for (DiscreteResolution resolution : resolutions) {
				// Look for a default video resolution. If 640x480 is present,
				// it is selected
				if (resolution.getWidth() == DEFAULT_WIDTH
						&& resolution.getHeight() == DEFAULT_HEIGHT) {
					defaultVideoResolution = resolution;
				}
			}

			DiscreteInterval[] intervals = listAvailableIntervals(defaultVideoResolution);
			DiscreteInterval defaultInterval = intervals[0];

			for (DiscreteInterval interval : intervals) {
				// Look for a default video interval. If 10 is present, it is
				// selected
				if (interval.getNum() == DEFAULT_INTERVAL_NUM
						&& interval.getDenom() == DEFAULT_INTERVAL_DEN) {
					defaultInterval = interval;
				}
			}

			settings.put("videoResolution", defaultVideoResolution.getWidth()
					+ "x" + defaultVideoResolution.getHeight());
			settings.put("captureResolution",
					defaultCaptureResolution.getWidth() + "x"
							+ defaultCaptureResolution.getHeight());
			settings.put("interval", defaultInterval.getNum() + "/"
					+ defaultInterval.getDenom());
			return settings;
		} else {
			settings.put("videoResolution", "640x480");
			settings.put("captureResolution", "640x480");
			settings.put("interval", "1/10");
			return settings;
		}

	}

	private String getIPAddress() throws Exception {
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			if (n.getDisplayName().equals("eth0")) {
				Enumeration ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					if (i.getHostAddress().indexOf(".") != -1) {
						return i.getHostAddress();
					}
				}
			}
		}
		throw new Exception();
	}

	/**
	 * This method is used to check if the camera can provide information about
	 * the available resolutions and frame intervals. If that information cannot
	 * be retrieved, it is necessary to use the fallback mode, which means that
	 * it is not possible to create a combobox with the available resolutions.
	 * Instead of that, a JTextField object is created, so resolution value must
	 * be entered manually.
	 * 
	 * @param dev
	 *            Device to check if fallback mode is needed
	 * @return true if fallback mode is needed, false if not
	 */
	public static boolean needFallbackMode(String dev) {
		DiscreteResolution[] resolutions = listAvailableResolutions(dev);
		return (resolutions == null || listAvailableIntervals(resolutions[0]) == null);
	}
}