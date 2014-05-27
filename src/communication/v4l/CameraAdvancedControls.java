package communication.v4l;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.Dictionary;
import au.edu.jcu.v4l4j.Control;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.examples.videoViewer.AbstractVideoViewer.ControlGUI;
import au.edu.jcu.v4l4j.exceptions.ControlException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

/**
 * CameraAdvancedControls class represents a panel with the camera controls.
 * This panel is meant to be embedded in a bigger JFrame. The device name is
 * passed to the class constructor, and the available controls will depend on
 * the kind of camera. Typical controls are bright, gamma, exposure,...
 * 
 * @author ehas
 * 
 */
public class CameraAdvancedControls extends JPanel {

	/** Hashtable representing the control list */
	private Hashtable<String, Control> controls;
	/** Instance of the class */
	private static CameraAdvancedControls instance;

	public CameraAdvancedControls(String dev) {
		try {
			VideoDevice vd = new VideoDevice(dev);
			this.controls = vd.getControlList().getTable();
			this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			initControlPane();
		} catch (V4L4JException e) {
			e.printStackTrace();
		}

	}

	private void initControlPane() {
		ControlGUI gui;
		for (Control c : controls.values()) {
			gui = getControlGUI(c);
			if (gui != null)
				this.add(gui.getPanel());
		}
	}

	private ControlGUI getControlGUI(Control c) {
		ControlGUI ctrl = null;
		if (c.getType() == V4L4JConstants.CTRL_TYPE_SLIDER)
			ctrl = new SliderControl(c);
		else if (c.getType() == V4L4JConstants.CTRL_TYPE_BUTTON)
			ctrl = new ButtonControl(c);
		else if (c.getType() == V4L4JConstants.CTRL_TYPE_SWITCH)
			ctrl = new SwitchControl(c);
		else if (c.getType() == V4L4JConstants.CTRL_TYPE_DISCRETE)
			ctrl = new MenuControl(c);
		return ctrl;
	}

	/**
	 * This class provides common functionality to SliderControl, ButtonControl,
	 * SwitchControl and MenuControl classes.
	 * 
	 * @author ehas
	 * 
	 */
	public class ControlModelGUI implements ControlGUI {
		protected JPanel contentPanel;
		private JLabel value;
		protected Control ctrl;

		public ControlModelGUI(Control c) {
			ctrl = c;
			initControlGUI();
		}

		private void initControlGUI() {
			contentPanel = new JPanel();
			contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

			TitledBorder b = BorderFactory.createTitledBorder(
					BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
					Dictionary.translateWord(ctrl.getName()));
			b.setTitleJustification(TitledBorder.LEFT);
			contentPanel.setBorder(b);

			if (ctrl.getType() != V4L4JConstants.CTRL_TYPE_BUTTON
					&& ctrl.getType() != V4L4JConstants.CTRL_TYPE_SWITCH) {
				contentPanel.setLayout(new BoxLayout(contentPanel,
						BoxLayout.PAGE_AXIS));
				value = new JLabel("Value: ");
				contentPanel.add(value);
				contentPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			} else {
				contentPanel.setLayout(new GridLayout());
				value = null;
			}

		}

		public final void updateValue(int v) {
			if (value != null)
				value.setText("Value: " + String.valueOf(v));
		}

		public final JPanel getPanel() {
			return contentPanel;
		}
	}

	/**
	 * This class represents a SliderControl component. When the value of the
	 * slider changes, it is notified to the camera and immediately changed.
	 * 
	 * @author ehas
	 * 
	 */
	public class SliderControl extends ControlModelGUI implements
			ChangeListener {
		private JSlider slider;

		public SliderControl(Control c) {
			super(c);
			int v = c.getDefaultValue();
			try {
				v = c.getValue();
			} catch (ControlException e) {
			}
			slider = new JSlider(JSlider.HORIZONTAL, c.getMinValue(),
					c.getMaxValue(), v);

			setSlider();
			contentPanel.add(slider);
			updateValue(v);
		}

		private void setSlider() {
			Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			int length = (ctrl.getMaxValue() - ctrl.getMinValue())
					/ ctrl.getStepValue() + 1;
			int middle = ctrl.getDefaultValue();

			slider.setSnapToTicks(true);
			slider.setPaintTicks(false);
			slider.setMinorTickSpacing(ctrl.getStepValue());
			labels.put(ctrl.getMinValue(),
					new JLabel(String.valueOf(ctrl.getMinValue())));
			labels.put(ctrl.getMaxValue(),
					new JLabel(String.valueOf(ctrl.getMaxValue())));
			labels.put(middle, new JLabel(String.valueOf(middle)));

			if (length < 100 && length > 10) {
				slider.setMajorTickSpacing(middle / 2);
				slider.setPaintTicks(true);
			} else if (length < 10) {
				slider.setMajorTickSpacing(middle);
				slider.setPaintTicks(true);
			}
			slider.setLabelTable(labels);
			slider.setPaintLabels(true);

			slider.addChangeListener(this);
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				int v = 0;
				try {
					v = ctrl.setValue(source.getValue());
				} catch (ControlException e1) {
					JOptionPane.showMessageDialog(null,
							"Error setting value.\n" + e1.getMessage());
					try {
						v = ctrl.getValue();
					} catch (ControlException ce) {
						v = ctrl.getDefaultValue();
					}
				} finally {
					updateValue(v);
					source.removeChangeListener(this);
					source.setValue(v);
					source.addChangeListener(this);
				}
			}
		}
	}

	/**
	 * 
	 * @author ehas
	 * 
	 */
	public class ButtonControl extends ControlModelGUI implements
			ActionListener {
		private JButton button;

		public ButtonControl(Control c) {
			super(c);
			button = new JButton("Activate");
			button.setAlignmentX(Component.CENTER_ALIGNMENT);
			button.addActionListener(this);
			contentPanel.add(button);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				ctrl.setValue(0);
			} catch (ControlException e1) {
				JOptionPane.showMessageDialog(null, "Error setting value.\n"
						+ e1.getMessage());
			}
		}
	}

	/**
	 * This class represent a button with two options: yes (selected) and no
	 * (unselected). Any change is immediately notified to the camera.
	 * 
	 * @author ehas
	 * 
	 */
	public class SwitchControl extends ControlModelGUI implements ItemListener {
		private JCheckBox box;

		public SwitchControl(Control c) {
			super(c);
			int v = c.getDefaultValue();
			box = new JCheckBox();
			box.setAlignmentX(Component.CENTER_ALIGNMENT);
			try {
				v = c.getValue();
			} catch (ControlException e) {
			}
			box.setSelected(v == 1);
			box.addItemListener(this);
			contentPanel.add(box);
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			try {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					ctrl.setValue(0);
				else
					ctrl.setValue(1);
			} catch (ControlException e1) {
				JOptionPane.showMessageDialog(null, "Error setting value.\n"
						+ e1.getMessage());
			}
		}
	}

	/**
	 * This class represent a combobox with several options. Any change is
	 * immediately notified to the camera.
	 * 
	 * @author ehas
	 * 
	 */
	public class MenuControl extends ControlModelGUI implements ActionListener {
		private JComboBox box;
		String[] names = new String[0];
		Integer[] values = new Integer[0];

		public MenuControl(Control c) {
			super(c);
			names = (String[]) ctrl.getDiscreteValueNames().toArray(names);
			values = (Integer[]) ctrl.getDiscreteValues().toArray(values);

			int v = c.getDefaultValue();
			box = new JComboBox(names);
			try {
				v = c.getValue();
			} catch (ControlException e) {
			}
			box.setSelectedIndex(ctrl.getDiscreteValues().indexOf(v));
			initPanel();
		}

		private void initPanel() {
			box.addActionListener(this);
			contentPanel.add(box);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				ctrl.setValue(values[box.getSelectedIndex()].intValue());
			} catch (ControlException e1) {
				JOptionPane.showMessageDialog(null, "Error setting value.\n"
						+ e1.getMessage());
			}

		}
	}

	public static CameraAdvancedControls getInstance() {
		return instance;
	}

}
