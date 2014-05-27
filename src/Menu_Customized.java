import gestion.GestorSave;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;
import ij.macro.MacroRunner;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import util.ConfigFile;
import util.StatusIndicator;

import communication.v4l.CamServer;
import communication.v4l.SurveyForm;

/**
 * @author jerome.mutterer(at)ibmp.fr
 * 
 */

public class Menu_Customized implements PlugIn, ActionListener {

	// private static final long serialVersionUID = 7436194992984622141L;
	String macrodir = IJ.getDirectory("macros");
	String name, title, path;
	String startupAction = "";
	String codeLibrary = "";
	String separator = System.getProperty("file.separator");
	JFrame frame = new JFrame();
	Frame frontframe;
	int xfw = 0;
	int yfw = 0;
	int wfw = 0;
	int hfw = 0;
	int preferredSize = 0;
	JToolBar toolBar = null;
	boolean tbOpenned = false;
	boolean grid = true;
	boolean visible = true;
	boolean shouldExit = false;
	boolean showSurveyForm = false;
	JButton button = null;
	JCheckBox checkBox = null;
	private boolean isPopup = false;
	private boolean isSticky = false;
	int nButtons = 0;

	public void run(String s) {
		// s used if called from another plugin, or from an installed command.
		// arg used when called from a run("command", arg) macro function
		// if both are empty, we choose to run the assistant "createAB.txt"
		
		String arg = Macro.getOptions();

		if (arg == null && s.equals("")) {
			try {
				File macro = new File(Menu_Customized.class.getResource(
						"createAB.txt").getFile());
				new MacroRunner(macro);
				return;
			} catch (Exception e) {
				IJ.error("createAB.txt file not found");
			}

		} else if (arg == null) { // call from an installed command
			path = IJ.getDirectory("startup") + s;
			try {
				name = path.substring(path.lastIndexOf("/") + 1);
			} catch (Exception e) {
			}
		} else { // called from a macro by run("Action Bar",arg)
			path = IJ.getDirectory("startup") + arg;
			try {
				path = path.substring(0, path.indexOf(".txt") + 4);
				name = path.substring(path.lastIndexOf("/") + 1);
			} catch (Exception e) {
			}
		}
		
		// title = name.substring(0, name.indexOf("."));
		title = name.substring(0, name.indexOf(".")).replaceAll("_", " ")
				.trim();
		frame.setTitle(title);
			
		if (WindowManager.getFrame(title) != null) {
			WindowManager.getFrame(title).toFront();
			return;
		}
		
	    // Change the default font size
	    if (ConfigFile.configFile == null){
	    	ConfigFile.setConfigFile(CamServer.configFile);
	    }
	    String sizeS = ConfigFile.getValue(ConfigFile.FONT_SIZE);
	    if (sizeS != null){
	    	preferredSize = Integer.parseInt(sizeS);
	    	
	    	UIDefaults defaults = UIManager.getDefaults();
		    List<Object> newDefaults = new ArrayList<Object>();
		    Map<Object, Font> newFonts = new HashMap<Object, Font>();
		    
		    Enumeration<Object> en = defaults.keys();
		    while (en.hasMoreElements()) {
		        Object key = en.nextElement();
		        Object value = defaults.get(key);
		        if (value instanceof Font) {
		            Font oldFont = (Font)value;
		            Font newFont = newFonts.get(oldFont);
		            if (newFont == null) {
		                newFont = new Font(oldFont.getName(), oldFont.getStyle(), preferredSize);
		                newFonts.put(oldFont, newFont);
		            }
		            newDefaults.add(key);
		            newDefaults.add(newFont);
		        }
		    }
		    defaults.putDefaults(newDefaults.toArray());
	    }
	    
	    
	    String showSurveyFormS = ConfigFile.getValue(ConfigFile.SHOW_SURVEY_FORM);
	    if (showSurveyFormS != null && showSurveyFormS.equalsIgnoreCase("true")){
	    	showSurveyForm = true;
	    }
	    		
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// this listener will save the bar's position and close it.
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int result = JOptionPane.showConfirmDialog(null,
						"¿Seguro que desea salir?", "Salir",
						JOptionPane.YES_NO_OPTION);
				switch(result){
				case 0:
					ImagePlus current = WindowManager.getCurrentImage();
					if (current != null){
						int resultSave = JOptionPane.showConfirmDialog(null,
								"Guardar los cambios\n" +
								"¿Desea guardar la sesión actual?", "Guardar cambios",
								JOptionPane.YES_NO_OPTION);
						switch(resultSave){
						case 0:
							GestorSave saver = new GestorSave();
							saver.saveAsTifInZip(current, false);
							new MacroRunner("run(\"ForceClose \")\n");
						case 1:
							break;
						}
					}
					break;
				case 1:
					return;
				}
				
				rememberXYlocation();
				Prefs.savePreferences();
				e.getWindow().dispose();
				WindowManager.removeWindow((Frame) frame);
				
				if (showSurveyForm){
					new SurveyForm();
				} else {
					System.exit(0);
				}
			}
		});
		
		frontframe = WindowManager.getFrontWindow();
				
		// toolbars will be added as lines in a n(0) rows 1 column layout
		frame.getContentPane().setLayout(new GridLayout(0, 1));

		// set an application icon image
		ImageIcon image = new ImageIcon("ImageJ.png");
		frame.setIconImage(image.getImage());
		
		// read the config file, and add toolbars to the frame
		designPanel();

		// captures the ImageJ KeyListener
		frame.setFocusable(true);
		frame.addKeyListener(IJ.getInstance());

		// setup the frame, and display it
		frame.setResizable(false);

		if (!isPopup) {

			frame.setLocation(
					(int) Prefs.get("actionbar" + title + ".xloc", 20),
					(int) Prefs.get("actionbar" + title + ".yloc", 20));
			WindowManager.addWindow(frame);
		}

		else {
			frame.setLocation(MouseInfo.getPointerInfo().getLocation());
			frame.setUndecorated(true);
			frame.addKeyListener(new KeyListener() {
				public void keyReleased(KeyEvent e) {
				}

				public void keyTyped(KeyEvent e) {
				}

				public void keyPressed(KeyEvent e) {
					int code = e.getKeyCode();
					if (code == KeyEvent.VK_ESCAPE) {
						frame.dispose();
						WindowManager.removeWindow(frame);
					}
				}
			});
		}

		if (isSticky) {
			frame.setUndecorated(true);
		}
		frame.pack();
		frame.setVisible(true);
		if (startupAction != "")
			try {
				new MacroRunner(startupAction + "\n" + codeLibrary);
			} catch (Exception fe) {
			}

		WindowManager.setWindow(frontframe);

		if (isSticky) {
			stickToActiveWindow();
			while ((shouldExit == false) && (frame.getTitle() != "xxxx")) {
				try {

					ImageWindow fw = WindowManager.getCurrentWindow();
					if (fw == null)
						frame.setVisible(false);
					if ((fw != null) && (fw.getLocation().x != xfw)
							|| (fw.getLocation().y != yfw)
							|| (fw.getWidth() != wfw)
							|| (fw.getHeight() != hfw)) {
						xfw = fw.getLocation().x;
						yfw = fw.getLocation().y;
						wfw = fw.getWidth();
						hfw = fw.getHeight();
						stickToActiveWindow();
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				IJ.wait(20);
			}
			if (frame.getTitle() == "xxxx")
				closeActionBar();
			if ((shouldExit))
				return;
		}

	}

	private void stickToActiveWindow() {
		ImageWindow fw = WindowManager.getCurrentWindow();
		try {
			if (fw != null) {
				if (!frame.isVisible())
					frame.setVisible(true);
				frame.toFront();
				frame.setLocation(fw.getLocation().x + fw.getWidth(),
						fw.getLocation().y);
				fw.toFront();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void closeActionBar() {
		frame.dispose();
		WindowManager.removeWindow(frame);
		WindowManager.setWindow(frontframe);
		shouldExit = true;
	}

	private void designPanel() {
		try {
			File file = new File(path);
			if (!file.exists())
				IJ.error("Config File not found");
			BufferedReader r = new BufferedReader(new FileReader(file));
			while (true) {
				String s = r.readLine();
				if (s.equals(null)) {
					r.close();
					closeToolBar();
					break;
				} else if (s.startsWith("<main>")) {
					// setABasMain();
					hideIJ();
				} else if (s.startsWith("<popup>")) {
					isPopup = true;
				} else if (s.startsWith("<sticky>")) {
					isSticky = true;
				} else if (s.startsWith("<DnD>")) {
					setABDnD();
				} else if (s.startsWith("<onTop>")) {
					setABonTop();
				} else if (s.startsWith("<startupAction>")) {
					String code = "";
					while (true) {
						String sc = r.readLine();
						if (sc.equals(null)) {
							break;
						}
						if (!sc.startsWith("</startupAction>")) {
							code = code + "" + sc;
						} else {
							startupAction = code;
							break;
						}
					}
				} else if (s.startsWith("<codeLibrary>")) {
					String code = "";
					while (true) {
						String sc = r.readLine();
						if (sc.equals(null)) {
							break;
						}
						if (!sc.startsWith("</codeLibrary>")) {
							code = code + "" + sc;
						} else {
							codeLibrary = code;
							break;
						}
					}
				} else if (s.startsWith("<icon>")) {
					String frameiconName = r.readLine().substring(5);
					setABIcon(frameiconName);
				} else if (s.startsWith("<noGrid>") && tbOpenned == false) {
					grid = false;
				} else if (s.startsWith("<text>") && tbOpenned == false) {
					frame.getContentPane().add(new JLabel(s.substring(6)));
				} else if (s.startsWith("<line>") && tbOpenned == false) {
					toolBar = new JToolBar();
					nButtons = 0;
					tbOpenned = true;
				} else if (s.startsWith("</line>") && tbOpenned == true) {
					closeToolBar();
					tbOpenned = false;
				} else if (s.startsWith("<separator>") && tbOpenned == true) {
					toolBar.addSeparator();
				} else if (s.startsWith("<button>") && tbOpenned == true) {
					String label = r.readLine().substring(6);
					String icon = r.readLine().substring(5);
					String arg = r.readLine().substring(4);
					if (arg.startsWith("<macro>")) {
						String code = "";
						while (true) {
							String sc = r.readLine();
							if (sc.equals(null)) {
								break;
							}
							if (!sc.startsWith("</macro>")) {
								code = code + "" + sc;
							} else {
								arg = code;
								break;
							}
						}
					} else if (arg.startsWith("<tool>")) {
						String code = "<tool>\n";
						while (true) {
							String sc = r.readLine();
							if (sc.equals(null)) {
								break;
							}
							if (!sc.startsWith("</tool>")) {
								code = code + "" + sc;
							} else {
								arg = code;
								break;
							}
						}
					} else if (arg.startsWith("<hide>")) {
						arg = "<hide>";
					} else if (arg.startsWith("<close>")) {
						arg = "<close>";
					}
					button = makeNavigationButton(icon, arg, label, label);
					toolBar.add(button);
					nButtons++;
				} else if (s.startsWith("<checkbox>") && tbOpenned == true) {
					String label = r.readLine().substring(6);
					String arg = r.readLine().substring(4);
					checkBox = makeNavigationCheckBox(arg, label, label);
					toolBar.add(checkBox);
					nButtons++;
					if (arg.contains("Camera")){
						CamServer.cameraCheckBox = checkBox;
					}

				} else if (s.startsWith("<status_indicator>")
						&& tbOpenned == true) {
					// This is a special component. It is used to show the
					// current state of the server
					String text = r.readLine().substring(6);
					String arg = r.readLine().substring(4);
					StatusIndicator indicator = makeNavigationStatusIndicator(arg,text);
					//indicator.setFont(new Font(null, Font.PLAIN, 20));
					indicator.setState(StatusIndicator.DOWN);
					CamServer.indicator = indicator;
					toolBar.add(indicator);
				}
			}
			r.close();
		} catch (Exception e) {
		}
	}

	private void closeToolBar() {
		toolBar.setFloatable(false);
		if (grid)
			toolBar.setLayout(new GridLayout(1, nButtons));
		frame.getContentPane().add(toolBar);
		tbOpenned = false;
	}

	protected JButton makeNavigationButton(String imageName,
			String actionCommand, String toolTipText, String altText) {

		String imgLocation = "icons/" + imageName;
		URL imageURL = Menu_Customized.class.getResource(imgLocation);
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setMargin(new Insets(2, 2, 2, 2));
		// button.setBorderPainted(true);
		button.addActionListener(this);
		button.setFocusable(true);
		//button.addKeyListener(IJ.getInstance());
		if (imageURL != null) {
			button.setIcon(new ImageIcon(imageURL, altText));
			button.setToolTipText(toolTipText);
		} else {
			button.setText(altText);
			button.setToolTipText(toolTipText);
		}
		return button;
	}

	protected JCheckBox makeNavigationCheckBox(String actionCommand,
			String toolTipText, String altText) {

		JCheckBox checkBox = new JCheckBox();
		checkBox.setActionCommand(actionCommand);
		checkBox.addActionListener(this);
		checkBox.setFocusable(true);
		checkBox.setSelected(false);

		return checkBox;
	}
	
	protected StatusIndicator makeNavigationStatusIndicator(String actionCommand,
			String text){
		StatusIndicator si = new StatusIndicator(text);
		si.setActionCommand(actionCommand);
		si.addActionListener(this);
		si.setOpaque(true);
		si.setFocusable(true);
		return si;
	}

	public void actionPerformed(ActionEvent e) {

		String cmd = e.getActionCommand();
		if (((e.getModifiers() & e.ALT_MASK) * ((e.getModifiers() & e.CTRL_MASK))) != 0) {
			IJ.run("Edit...", "open=[" + path + "]");
			return;
		}
		if (((e.getModifiers() & e.ALT_MASK)) != 0) {
			closeActionBar();
			return;
		}
		if (cmd.startsWith("<hide>")) {
			toggleIJ();
		} else if (cmd.startsWith("<close>")) {
			closeActionBar();
			return;
		} else if (cmd.startsWith("<tool>")) {
			// install this tool and select it
			String tool = cmd.substring(6, cmd.length());
			String toolname = ((JButton) e.getSource()).getToolTipText();
			tool = "macro '" + toolname + " Tool - C000'{\n" + tool + "\n}\n"
					+ codeLibrary;
			new MacroInstaller().install(tool);
			Toolbar.getInstance().setTool(10);
			IJ.showStatus(toolname + " Tool installed");

		} else {
			try {
				if (e.getSource() instanceof JCheckBox) {
					JCheckBox checkbox = (JCheckBox) e.getSource();
					cmd = cmd.substring(0, cmd.length() - 2) + ", \""
							+ checkbox.isSelected() + "\");";
					// JCheckBox box =
					// (JCheckBox)toolBar.getComponentAtIndex(toolBar.getComponentIndex(checkbox)+2);
					// box.setSelected(checkbox.isSelected());
				}
				if (e.getSource() instanceof StatusIndicator) {
					StatusIndicator checkbox = (StatusIndicator) e.getSource();
					cmd = cmd.substring(0, cmd.length() - 2) + ", \""
							+ (checkbox.getState() == StatusIndicator.DOWN ? "true" : "false") + "\");";
					// JCheckBox box =
					// (JCheckBox)toolBar.getComponentAtIndex(toolBar.getComponentIndex(checkbox)+2);
					// box.setSelected(checkbox.isSelected());
				}
				new MacroRunner(cmd + "\n" + codeLibrary);
			} catch (Exception fe) {
				fe.printStackTrace();
				IJ.error("Error in macro command");
			}
		}
		frame.repaint();
		if (isPopup) {
			frame.dispose();
			WindowManager.removeWindow(frame);
			WindowManager.setWindow(frontframe);
		}

	}

	private void toggleIJ() {
		IJ.getInstance().setVisible(!IJ.getInstance().isVisible());
		visible = IJ.getInstance().isVisible();
	}

	private void hideIJ() {
		IJ.getInstance().setVisible(false);
		visible = false;
	}

	protected void rememberXYlocation() {
		Prefs.set("actionbar" + title + ".xloc", frame.getLocation().x);
		Prefs.set("actionbar" + title + ".yloc", frame.getLocation().y);
	}

	private void setABasMain() {
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				rememberXYlocation();
				e.getWindow().dispose();
				IJ.run("Quit");
				
			}
		});
	}

	private void setABDnD() {
		DropTarget dt = new DropTarget(frame, IJ.getInstance().getDropTarget());
	}

	private void setABIcon(String s) {
		String imgLocation = "icons/" + s;
		try {
			URL imageURL = Menu_Customized.class.getResource(imgLocation);
			frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(
					imageURL));
		} catch (Exception fe) {
			IJ.error("Error creating the bar's icon");
		}
	}

	private void setABonTop() {
		frame.setAlwaysOnTop(true);
	}
}