package edition;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.macro.MacroRunner;
import ij.plugin.PlugIn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import communication.v4l.FrameCamServer;

/**
 * This class creates a frame to group a list of pictures in a scroll view. They are the frames of the 
 * current working session, and the can be selected, deleted and drag up or down.
 * 
 * @author ehas
 *
 */
public class ImageManager implements PlugIn {

	private static ImageManager instance = new ImageManager();

	GestorImagenesModel model;
	GestorImagenesView view;
	GestorImagenesController controller;

	public void run(String arg) {
		model = new GestorImagenesModel();
		view = new GestorImagenesView("Gestor Imagenes", model);
		controller = new GestorImagenesController(model, view);
	}

	public static ImageManager getInstance() {
		return instance;
	}

	public void refresh() {
		model.refresh(ImageManager.getInstance().view.list);
	}

	public void close() {
		if (view != null) {
			view.dispose();
		}
		controller = null;
		view = null;
		model = null;
	}
}

// Model contains the functionality (properties and methods)
class GestorImagenesModel extends DefaultListModel {

	StackEditorTM stackEditorTM = new StackEditorTM();
	Image[] imagesArray;

	public void exit() {
		System.exit(0);
	}

	public void loadImage(int indice) {
		IJ.setSlice(indice + 1);
	}

	public void addImage(int index) {
		ImagePlus imp = readImage();
		if (imp != null) {

			ImagePlus imp2 = IJ.getImage();
			ImageStack stack = imp2.getStack();
			if (stack.getSize() == 1) {
				String label = stack.getSliceLabel(1);
				if (label != null && label.indexOf("\n") != -1) {
					stack.setSliceLabel(null, 1);
				}
				Object obj = imp.getProperty("Label");
				if (obj != null && (obj instanceof String)) {
					stack.setSliceLabel((String) obj, 1);
				}
			}

			stack.addSlice(null, imp.getChannelProcessor());
			imp2.setStack(null, stack);
			imp2.setSlice(index + 1);
			imp2.unlock();
		}
	}

	private ImagePlus readImage() {
		OpenDialog od = new OpenDialog("Open", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		ImagePlus imp = null;
		if (name != null) {
			String path = directory + name;
			imp = new ImagePlus(path);
		}
		return imp;
	}

	public void deleteImage(int index) {
		stackEditorTM.deleteSlice(index + 1);
	}

	public void refresh(JList list) {
		// Image thumbImage;
		imagesArray = stackEditorTM.getImages();
		list.setListData(imagesArray);
	}

	public void addImage(int index, BufferedImage dragged) {
		ImagePlus imp = new ImagePlus("title", Toolkit.getDefaultToolkit()
				.createImage(dragged.getSource()));
		if (imp != null) {

			ImagePlus imp2 = IJ.getImage();
			ImageStack stack = imp2.getStack();
			if (stack.getSize() == 1) {
				String label = stack.getSliceLabel(1);
				if (label != null && label.indexOf("\n") != -1)
					stack.setSliceLabel(null, 1);
				Object obj = imp.getProperty("Label");
				if (obj != null && (obj instanceof String))
					stack.setSliceLabel((String) obj, 1);
			}

			stack.addSlice(null, imp.getChannelProcessor(), index);
			imp2.setStack(null, stack);
			imp2.setSlice(index + 1);
			imp2.unlock();
		}
	}
}

// View is where the GUI is built
class GestorImagenesView extends JFrame {
	StackEditorTM stackEditorTM = new StackEditorTM();

	JButton deleteImage = new JButton("Borrar");
	JPanel buttonsPanel = new JPanel();
	ReorderableJList list;
	
	GestorImagenesView(String title, GestorImagenesModel model) // the
																// constructor
	{
		super(title);
		list = new ReorderableJList(model);

		setSize(350, 450);
		setLayout(new BorderLayout());
		
		// List
		list.setLayoutOrientation(JList.VERTICAL);
		list.setBorder(new LineBorder(Color.black));
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(list, BorderLayout.NORTH);

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.getViewport().setView(panel);
		jScrollPane.setPreferredSize(new Dimension(250, 250));

		this.getContentPane().add(jScrollPane, BorderLayout.WEST);

		// Buttons
		buttonsPanel.add(deleteImage);
		buttonsPanel.setPreferredSize(new Dimension(105, 250));

		this.getContentPane().add(buttonsPanel, BorderLayout.EAST);

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setVisible(true);

		addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {

			}

			@Override
			public void windowIconified(WindowEvent e) {

			}

			@Override
			public void windowDeiconified(WindowEvent e) {

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				new MacroRunner("run(\"Close \")\n");
			}

			@Override
			public void windowClosed(WindowEvent e) {

			}

			@Override
			public void windowActivated(WindowEvent e) {
				
			}
		});

		addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				RoiManager rm = RoiManager.getInstance();
				rm.setVisible(true);
				FrameCamServer fcs = FrameCamServer.getInstance();
				fcs.setVisible(true);
				WindowManager.getCurrentImage().getWindow().setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {

			}

			@Override
			public void componentMoved(ComponentEvent e) {

			}

			@Override
			public void componentHidden(ComponentEvent e) {

			}
		});

	}

	// method to add ActionListener passed by Controller to buttons
	public void buttonActionListeners(ActionListener al) {
		deleteImage.setActionCommand("borrarImagen");
		deleteImage.addActionListener(al);
	}

	// method to add ActionListener passed by Controller to buttons
	public void listActionListeners(ListSelectionListener al) {
		list.addListSelectionListener(al);
	}

	class ImageRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			// for default cell renderer behavior
			Component c = super.getListCellRendererComponent(list, value,
					index, isSelected, cellHasFocus);
			// set icon for cell image
			Image image = (Image) value;
			ImageIcon imageIcon = new ImageIcon(image.getScaledInstance(200,
					-1, Image.SCALE_DEFAULT));
			((JLabel) c).setIcon(imageIcon);
			((JLabel) c).setText("");
			return c;
		}

	}
}

class ReorderableJList extends JList implements DragSourceListener,
		DropTargetListener, DragGestureListener {

	static DataFlavor localObjectFlavor;
	static {
		try {
			localObjectFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
	}
	static DataFlavor[] supportedFlavors = { localObjectFlavor };
	DragSource dragSource;
	DropTarget dropTarget;
	Object dropTargetCell;
	int draggedIndex = -1;
	GestorImagenesModel model;

	public ReorderableJList(GestorImagenesModel model) {
		super();
		this.model = model;
		setCellRenderer(new ReorderableListCellRenderer());
		setModel(new DefaultListModel());
		dragSource = new DragSource();
		DragGestureRecognizer dgr = dragSource
				.createDefaultDragGestureRecognizer(this,
						DnDConstants.ACTION_MOVE, this);
		dropTarget = new DropTarget(this, this);
	}

	// DragGestureListener
	public void dragGestureRecognized(DragGestureEvent dge) {
		System.out.println("dragGestureRecognized");
		// find object at this x,y
		Point clickPoint = dge.getDragOrigin();
		int index = locationToIndex(clickPoint);
		if (index == -1)
			return;
		Object target = getModel().getElementAt(index);
		Transferable trans = new RJLTransferable(target);
		draggedIndex = index;
		dragSource.startDrag(dge, Cursor.getDefaultCursor(), trans, this);
	}

	// DragSourceListener events
	public void dragDropEnd(DragSourceDropEvent dsde) {
		System.out.println("dragDropEnd()");
		dropTargetCell = null;
		draggedIndex = -1;
		repaint();

	}

	public void dragEnter(DragSourceDragEvent dsde) {
	}

	public void dragExit(DragSourceEvent dse) {
	}

	public void dragOver(DragSourceDragEvent dsde) {
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
	}

	// DropTargetListener events
	public void dragEnter(DropTargetDragEvent dtde) {
		System.out.println("dragEnter");
		if (dtde.getSource() != dropTarget)
			dtde.rejectDrag();
		else {
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
			System.out.println("accepted dragEnter");
		}
	}

	public void dragExit(DropTargetEvent dte) {
	}

	public void dragOver(DropTargetDragEvent dtde) {
		// figure out which cell it's over, no drag to self
		if (dtde.getSource() != dropTarget)
			dtde.rejectDrag();
		Point dragPoint = dtde.getLocation();
		int index = locationToIndex(dragPoint);
		if (index == -1)
			dropTargetCell = null;
		else
			dropTargetCell = getModel().getElementAt(index);
		repaint();
	}

	public void drop(DropTargetDropEvent dtde) {
		System.out.println("drop()!");
		if (dtde.getSource() != dropTarget) {
			System.out.println("rejecting for bad source ("
					+ dtde.getSource().getClass().getName() + ")");
			dtde.rejectDrop();
			return;
		}
		Point dropPoint = dtde.getLocation();
		int index = locationToIndex(dropPoint);
		System.out.println("drop index is " + index);
		boolean dropped = false;
		try {
			if ((index == -1) || (index == draggedIndex)) {
				System.out.println("dropped onto self");
				dtde.rejectDrop();
				return;
			}
			dtde.acceptDrop(DnDConstants.ACTION_MOVE);
			System.out.println("accepted");
			Object dragged = dtde.getTransferable().getTransferData(
					localObjectFlavor);
			// move items - note that indicies for insert will
			// change if [removed] source was before target
			System.out.println("drop " + draggedIndex + " to " + index);
			boolean sourceBeforeTarget = (draggedIndex < index);
			System.out.println("source is" + (sourceBeforeTarget ? "" : " not")
					+ " before target");
			System.out.println("insert at "
					+ (sourceBeforeTarget ? index - 1 : index));
			// model.anadirImagen(view.list.getModel().getSize());
			model.deleteImage(draggedIndex);
			model.addImage((sourceBeforeTarget ? index - 1 : index),
					(BufferedImage) dragged);
			model.refresh(this);

			// Reordenamos la lista de rois
			RoiManager rm = RoiManager.getInstance();
			if (rm != null) {
				rm.runCommand("reorder", draggedIndex, index);
			}
			dropped = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		dtde.dropComplete(dropped);
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	// main() method to test - listed below

	class RJLTransferable implements Transferable {
		Object object;

		public RJLTransferable(Object o) {
			object = o;
		}

		public Object getTransferData(DataFlavor df)
				throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(df))
				return object;
			else
				throw new UnsupportedFlavorException(df);
		}

		public boolean isDataFlavorSupported(DataFlavor df) {
			return (df.equals(localObjectFlavor));
		}

		public DataFlavor[] getTransferDataFlavors() {
			return supportedFlavors;
		}
	}

	class ReorderableListCellRenderer extends DefaultListCellRenderer {
		boolean isTargetCell;
		boolean isLastItem;

		public ReorderableListCellRenderer() {
			super();
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean hasFocus) {
			isTargetCell = (value == dropTargetCell);
			isLastItem = (index == list.getModel().getSize() - 1);
			boolean showSelected = isSelected & (dropTargetCell == null);

			Component c = super.getListCellRendererComponent(list, value,
					index, showSelected, hasFocus);
			Image image = (Image) value;
			ImageIcon imageIcon = new ImageIcon(image.getScaledInstance(200,
					-1, Image.SCALE_DEFAULT));
			((JLabel) c).setIcon(imageIcon);
			((JLabel) c).setText("");

			return c;

		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (isTargetCell) {
				g.setColor(Color.black);
				g.drawLine(0, 0, getSize().width, 0);
			}
		}
	}

}


// the controller listens for actions and reacts
class GestorImagenesController implements ActionListener, ListSelectionListener {
	GestorImagenesModel model;
	GestorImagenesView view;

	public GestorImagenesController(GestorImagenesModel model,
			GestorImagenesView view) {
		// create the model and the GUI view
		this.model = model;
		this.view = view;

		view.list.setSelectedIndex(0);
		model.refresh(view.list);

		// Add action listener from this class to view buttons
		view.buttonActionListeners(this);
		view.listActionListeners(this);

	}

	// Provide interactions for actions performed in the view
	public void actionPerformed(ActionEvent ae) {
		String action_com = ae.getActionCommand();
		
		if (action_com != null && action_com.equals("anadirImagen")) {
			model.addImage(view.list.getModel().getSize());
		} else if (action_com != null && action_com.equals("borrarImagen")) {
			if (!(view.list.isSelectionEmpty())) {
				// If it is the only image in the stack, close all windows
				if (view.list.getModel().getSize() == 1) {
					if (RoiManager.getInstance() != null) {
						RoiManager.getInstance().close();
					}
					ImageManager.getInstance().close();
					if (WindowManager.getCurrentImage() != null) {
						WindowManager.getCurrentImage().close();
						WindowManager.setTempCurrentImage(null);
					}
					return;
				}
				model.deleteImage(view.list.getSelectedIndex());
			} else {
				IJ.error("No hay imagen seleccionada");
			}
		}
		model.refresh(view.list);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!(view.list.isSelectionEmpty())) {
			model.loadImage(view.list.getSelectedIndex());
		}
	}

}
