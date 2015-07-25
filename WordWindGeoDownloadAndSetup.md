# Downloading and building your own distro #

This page explains how to download the source and build a binary distro for your OS.

# Details #

You can use Eclipse to download the source from the SVN server and build a binary distribution.

## To Download the source ##

  * Install a eclipse SVN plugin (if not available). A popular one is Subeclipse from http://subclipse.tigris.org/
  * Within eclipse, open the SVN repository view: Window/Show View/Other/SVN/ SVN Repository.
  * Create a new repository location to http://worldwindrcp.googlecode.com/svn/trunk/ using the wizard easy steps.
  * Check out all the source projects: `org.eclipse.plugin.*`
  * At this point you can open the product descriptor (WorldWind.product within the WordWind project) and run the RCP application by clicking "Launch an Eclipse application"
  * The Java OpenGL (JOGL) natives have been implemeted as plugins for all big 3 OSes: Win32, Linux32, and OSX.
  * Now the application should start successfully.

## To build a distro ##
  * You will need to install the Eclipse delta pack. It allows you to build binaries for different OSes. The delta pack can be downloaded from: http://archive.eclipse.org/eclipse/downloads/drops/R-3.1-200506271435/index.php
  * Once the delta pack has been installed, open the product descriptor (WorldWind.product), and click "Eclipse product Export Wizard". Follow the easy instructions to build a binary for your OS (For example Win32: c:\temp\win32.win32.x86\eclipse).
  * The Java OpenGL (JOGL) natives have been implemeted as plugins for all big 3 OSes: Win32, Linux32, and OSX.


Your comments are welcome!

Regards,

Vladimir

