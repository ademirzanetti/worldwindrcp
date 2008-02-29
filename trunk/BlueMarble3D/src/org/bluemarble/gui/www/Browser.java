/*
 * Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */ 

package org.bluemarble.gui.www;

import gov.nasa.worldwind.layers.LayerList;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.SwingConstants;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import org.bluemarble.gui.NavigatorWindow;
import org.jdesktop.jdic.browser.*;

import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.SimpleHTTPClient;


/**
 * JDIC API demo main class.
 * <p>
 * <code>Browser</code> is a GUI application demonstrating the usage of the JDIC API package 
 * <code>org.jdesktop.jdic.browser</code> (Browser component).
 */

public class Browser extends JFrame // JPanel 
{
	private static final long serialVersionUID = 6255830829611296729L;

	public static ImageIcon browseIcon = new ImageIcon(
        Browser.class.getResource("/images/browser/Right.gif"));

    BorderLayout borderLayout1 = new BorderLayout();

    JToolBar jBrowserToolBar = new JToolBar();
    JButton jStopButton = new JButton("Stopp",
            new ImageIcon(getClass().getResource("/images/browser/Stop.png")));

    JButton jRefreshButton = new JButton("Refresh",
            new ImageIcon(getClass().getResource("/images/browser/Reload.png")));
    JButton jForwardButton = new JButton("Forward",
            new ImageIcon(getClass().getResource("/images/browser/Forward.png")));
    JButton jBackButton = new JButton("Back",
            new ImageIcon(getClass().getResource("/images/browser/Back.png")));

    JPanel jAddressPanel = new JPanel();
    JLabel jAddressLabel = new JLabel();
    JTextField jAddressTextField = new JTextField();
    JButton jGoButton = new JButton();
    JPanel jAddrToolBarPanel = new JPanel();
    StatusBar statusBar = new StatusBar();
    JPanel jBrowserPanel = new JPanel();

    IWebBrowser webBrowser;

    private static Browser browser;
    private NavigatorWindow navigator;

    /**
     * Constructor
     */
    public Browser() {
    	super("Web Browser");
        try 
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setPreferredSize(new Dimension(screenSize.width * 6 / 10
            		, screenSize.height * 8 / 10));
            
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            
            jbInit();
            pack();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Browser getInstance() {
    	if ( browser == null ) {
    		browser =  new Browser();
    	}
    	return browser;
    }
    
    /**
     * Main
     * @param args
     */
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        final Browser browser = new Browser();
        browser.pack();
        browser.setVisible(true);
    }

    private boolean duplicate = false;
    
    private void jbInit() throws Exception 
    {
        this.setLayout(borderLayout1);

        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        
        jAddressLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        jAddressLabel.setToolTipText("");
        jAddressLabel.setText(" URL: ");

        jGoButton.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0,
                2, 0, 2),
                new EtchedBorder()));
        jGoButton.setMaximumSize(new Dimension(60, 25));
        jGoButton.setMinimumSize(new Dimension(60, 25));
        jGoButton.setPreferredSize(new Dimension(60, 25));
        jGoButton.setToolTipText("Load the given URL");
        jGoButton.setIcon(browseIcon);
        jGoButton.setText("GO");
        jGoButton.addActionListener(new Browser_jGoButton_actionAdapter(this));
        jAddressPanel.setLayout(new BorderLayout());

        jAddressTextField.addActionListener(new Browser_jAddressTextField_actionAdapter(this));
        jBackButton.setToolTipText("Go back one page");
        jBackButton.setHorizontalTextPosition(SwingConstants.TRAILING);
        jBackButton.setEnabled(false);
        jBackButton.setMaximumSize(new Dimension(75, 27));
        jBackButton.setPreferredSize(new Dimension(75, 27));
        jBackButton.addActionListener(new Browser_jBackButton_actionAdapter(this));
        jForwardButton.setToolTipText("Go forward one page");
        jForwardButton.setEnabled(false);
        jForwardButton.addActionListener(new Browser_jForwardButton_actionAdapter(this));
        jRefreshButton.setToolTipText("Reload current page");
        jRefreshButton.setEnabled(true);
        jRefreshButton.setMaximumSize(new Dimension(75, 27));
        jRefreshButton.setMinimumSize(new Dimension(75, 27));
        jRefreshButton.setPreferredSize(new Dimension(75, 27));
        jRefreshButton.addActionListener(new Browser_jRefreshButton_actionAdapter(this));
        jStopButton.setToolTipText("Stop loading this page");
        jStopButton.setVerifyInputWhenFocusTarget(true);
        jStopButton.setText("Stop");
        jStopButton.setEnabled(true);
        jStopButton.setMaximumSize(new Dimension(75, 27));
        jStopButton.setMinimumSize(new Dimension(75, 27));
        jStopButton.setPreferredSize(new Dimension(75, 27));
        jStopButton.addActionListener(new Browser_jStopButton_actionAdapter(this));
        jAddressPanel.add(jAddressLabel, BorderLayout.WEST);
        jAddressPanel.add(jAddressTextField, BorderLayout.CENTER);
        jAddressPanel.add(jGoButton, BorderLayout.EAST);
        jAddressPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(2, 0, 2, 0)));

        jBrowserToolBar.setFloatable(false);
        jBrowserToolBar.add(jBackButton, null);
        jBrowserToolBar.add(jForwardButton, null);
        jBrowserToolBar.addSeparator();
        jBrowserToolBar.add(jRefreshButton, null);
        jBrowserToolBar.add(jStopButton, null);
        jBrowserToolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(2, 2, 2, 0)));

        jAddrToolBarPanel.setLayout(new BorderLayout());
        jAddrToolBarPanel.add(jAddressPanel, BorderLayout.CENTER);
        jAddrToolBarPanel.add(jBrowserToolBar, BorderLayout.WEST);
        jAddrToolBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        statusBar.lblDesc.setText("JDIC API Demo - Browser");

        try {
            BrowserEngineManager bem = BrowserEngineManager.instance();
            webBrowser = bem.getActiveEngine().getWebBrowser();
            webBrowser.setURL(new URL("http://www.google.com"));

        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            return;
        }
        
        webBrowser.addWebBrowserListener(new WebBrowserListener() {
            public void downloadStarted(WebBrowserEvent event) {
            	WebBrowser browser = ((WebBrowser)event.getSource());
            	URL url = browser.getURL();
            	
                //updateStatusInfo("Loading started.");
            	if ( ! duplicate ) {
            		duplicate = true;
            		System.out.println("Loading started. evt id=" + event.getID() + " u=" + url);
            		
            		if ( url != null) {
            			handleURL(browser);
            		}
            	}
            	else {
            		System.out.println("Loading started duplicate u=" + url);
            	}
            }
            
			public void initializationCompleted(WebBrowserEvent event){
			}
			
            public void downloadCompleted(WebBrowserEvent event) 
            {
            	//System.out.println("Loading completed. evt id=" + event.getID());
            	
                jBackButton.setEnabled(webBrowser.isBackEnabled());
                jForwardButton.setEnabled(webBrowser.isForwardEnabled());
                
                updateStatusInfo("Loading completed.");

                URL currentUrl = webBrowser.getURL();

                if (currentUrl != null) {
                    jAddressTextField.setText(currentUrl.toString());
                }
            }

            public void downloadProgress(WebBrowserEvent event) {
                // updateStatusInfo("Loading in progress...");
            	//System.out.println("Loading progress. evt id=" + event.getID());
            }

            public void downloadError(WebBrowserEvent event) {
                updateStatusInfo("Loading error.");
            }

            public void documentCompleted(WebBrowserEvent event) {
            	duplicate = false;
                updateStatusInfo("Document loading completed.");
                System.out.println("Document loading completed  u=" + webBrowser.getURL());
            }

            public void titleChange(WebBrowserEvent event) {
                updateStatusInfo("Title of the browser window changed.");
            }  

            public void statusTextChange(WebBrowserEvent event) {
                // updateStatusInfo("Status text changed.");
            } 
            public void windowClose(WebBrowserEvent event) {
            } 
        });

        jBrowserPanel.setLayout(new BorderLayout());
        jBrowserPanel.add(webBrowser.asComponent(), BorderLayout.CENTER);				

        this.add(jAddrToolBarPanel, BorderLayout.NORTH);
        this.add(statusBar, BorderLayout.SOUTH);
        this.add(jBrowserPanel, BorderLayout.CENTER);
    }

    /**
     * Handle URL
     * @param currentUrl
     */
    private void handleURL (WebBrowser browser ) // URL currentUrl)
    {
		// Duplicate download for URL to extract content type
        // No other way to get the CT :(
		try {
			URL url = browser.getURL();
			
			System.out.println("Loading manually " + url);
			
			SimpleHTTPClient client = new SimpleHTTPClient(url);
			
			client.doGet();
			//String contentType = client.getContentType();
			
			// Handle content type
			// Google earth or OpeNDAP
			if ( client.isContentTypeKML() || client.isContentTypeKMZ()) {
				browser.stop();
				
				// handle kml/kmz
				handleKmlKmz( url );
			}
			else {
				browser.stop();
				
				// is this a DODS Request?
				// GDS HTTP Headers: XDODS-Server=[3.1], Content-Description=[dods_info]
				// THREDDS Headers: XDODS-Server=[opendap/3.7], Content-Description=[dods-error]
				boolean isDODS = client.getHeaders().get("XDODS-Server") != null;
				
				if ( isDODS ) {
					
					System.out.println("DODS URL detected: " + url);
					//handleDODSLocation(currentUrl);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			updateStatusInfo(e.getMessage());
		}
    }
    
    /**
     * Target {@link NavigatorWindow} for HTTP content types: {@link KMLSource} or NetCDF
     * @param navigator
     */
    public void setNavigatorWindow ( NavigatorWindow navigator) {
    	this.navigator = navigator;
    }
    
    /**
	 * Deal with KML or KMZ files
	 * @param location
	 */
	private void handleKmlKmz ( URL location ) throws Exception
	{
		KMLSource kml = new KMLSource(location);
		LayerList list = kml.toLayerList();

		if ( list.size() == 0 )
			throw new Exception("No Ground, Screen or Placemark overlays in document.");
		
		if ( navigator != null)
			navigator.addKMLSource(kml);
	}
	
	
    void updateStatusInfo(String statusMessage) {
        statusBar.lblStatus.setText(statusMessage);
    }

    /**
     * Check the current input URL string in the address text field, load it,
     * and update the status info and toolbar info.
     */
    void loadURL() {
        String inputValue = jAddressTextField.getText();

        if (inputValue == null) {
            JOptionPane.showMessageDialog(this, "The given URL is NULL:",
                    "Warning", JOptionPane.WARNING_MESSAGE);
        } else {
            // Check if the text value is a URL string.
            URL curUrl = null;

            try {
                // Check if the input string is a local path by checking if it starts
                // with a driver name(on Windows) or root path(on Unix).               
                File[] roots = File.listRoots();

                for (int i = 0; i < roots.length; i++) {
                    if (inputValue.toLowerCase().startsWith(roots[i].toString().toLowerCase())) {
                        File curLocalFile = new File(inputValue);

                        curUrl = curLocalFile.toURL();
                        break;
                    }
                }

                if (curUrl == null) {
                    // Check if the text value is a valid URL.
                    try {
                        curUrl = new URL(inputValue);
                    } catch (MalformedURLException e) {
                            if (inputValue.toLowerCase().startsWith("ftp.")) {
                                curUrl = new URL("ftp://" + inputValue);
                            } else if (inputValue.toLowerCase().startsWith("gopher.")) {
                                curUrl = new URL("gopher://" + inputValue);
                            } else {
                                curUrl = new URL("http://" + inputValue);
                            }
                    }
                }
                            
                webBrowser.setURL(curUrl);

                // Update the address text field, statusbar, and toolbar info.
                updateStatusInfo("Loading " + curUrl.toString() + " ......");

            } catch (MalformedURLException mue) {
                JOptionPane.showMessageDialog(this,
                    "The given URL is not valid:" + inputValue, "Warning",
                    JOptionPane.WARNING_MESSAGE);
            }                
        }
    }

    void jGoButton_actionPerformed(ActionEvent e) {
        loadURL();
    }

    void jAddressTextField_actionPerformed(ActionEvent e) {
        loadURL();
    }

    void jBackButton_actionPerformed(ActionEvent e) {
        webBrowser.back();
    }

    void jForwardButton_actionPerformed(ActionEvent e) {
        webBrowser.forward();
    }

    void jRefreshButton_actionPerformed(ActionEvent e) {
        webBrowser.refresh();
    }

    void jStopButton_actionPerformed(ActionEvent e) {
        webBrowser.stop();
    }
}


class Browser_jAddressTextField_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jAddressTextField_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jAddressTextField_actionPerformed(e);
    }
}


class Browser_jBackButton_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jBackButton_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jBackButton_actionPerformed(e);
    }
}


class Browser_jForwardButton_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jForwardButton_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jForwardButton_actionPerformed(e);
    }
}


class Browser_jRefreshButton_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jRefreshButton_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jRefreshButton_actionPerformed(e);
    }
}


class Browser_jStopButton_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jStopButton_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jStopButton_actionPerformed(e);
    }
}


class Browser_jGoButton_actionAdapter implements java.awt.event.ActionListener {
    Browser adaptee;

    Browser_jGoButton_actionAdapter(Browser adaptee) {
        this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent e) {
        adaptee.jGoButton_actionPerformed(e);
    }
}
