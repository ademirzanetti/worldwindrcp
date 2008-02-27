package org.bluemarble.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * New Server Dialog
 * @author vsilva
 *
 */
public class NewServerDialog extends JDialog implements PropertyChangeListener 
{
	private static final long serialVersionUID = -8935781503076903408L;

	private String btnString1 = "Enter";
	private String btnString2 = "Cancel";

	private JTextField txtServer, txtURL;
	private JOptionPane optionPane;

	private boolean canceled = false;
	
	static private final int WIDTH = 450;
	static private final int HEIGHT = 200;
	
	/**
	 * Constructor
	 */
	public NewServerDialog() 
	{
		// Make sure we call the parent
		super((Frame)null, true);

		// Set the characteristics for this dialog instance
		setTitle("Add a new WMS Server");
		setSize(WIDTH, HEIGHT);
		// setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);

		// center dialog
		Toolkit tk = Toolkit.getDefaultToolkit();
	    Dimension screenSize = tk.getScreenSize();
	    int screenHeight = screenSize.height;
	    int screenWidth = screenSize.width;
	    
	    setLocation((screenWidth - WIDTH) / 2, (screenHeight - HEIGHT) / 2);
	    
		txtServer = new JTextField(30);
		txtURL = new JTextField(30);

		// Create an array of the text and components to be displayed.
		Object[] array = { "Server", txtServer, "Capabilities URL", txtURL };

		// Create an array specifying the number of dialog buttons
		// and their text.
		Object[] options = { btnString1, btnString2 };

		// Create the JOptionPane.
		optionPane = new JOptionPane(array, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION, null, options, options[0]);

		// Make this dialog display it.
		setContentPane(optionPane);

		// Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});

		// Ensure the text field always gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				txtServer.requestFocusInWindow();
			}
		});
		// Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		// System.out.println(prop);

		if (isVisible()
				&& (e.getSource() == optionPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY
						.equals(prop))) {
			Object value = optionPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				// ignore reset
				return;
			}

			// Reset the JOptionPane's value.
			// If you don't do this, then if the user
			// presses the same button next time, no
			// property change event will be fired.
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

			if (btnString1.equals(value)) 
			{
				if (txtServer.getText().length() == 0
						|| txtURL.getText().length() == 0)
					JOptionPane.showMessageDialog(NewServerDialog.this,
							"All fields are required.");
				else
					clearAndHide();
			} else {
				// user closed dialog or clicked cancel
				canceled = true;
				clearAndHide();
			}
		}
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		setVisible(false);
	}

	public String getServerName() {
		return txtServer.getText();
	}

	public String getServerURL() {
		return txtURL.getText();
	}

	public boolean isCanceled () {
		return canceled;
	}

}
