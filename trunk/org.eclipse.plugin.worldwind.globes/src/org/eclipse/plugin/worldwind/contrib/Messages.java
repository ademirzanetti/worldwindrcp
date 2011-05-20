package org.eclipse.plugin.worldwind.contrib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;


public class Messages 
{
	static public String NL = System.getProperty("line.separator");
	
	private static final String BUNDLE_NAME = "org.eclipse.plugin.worldwind.contrib.Messages";


    private static String retrieveMessage(String property, String bundle)
    {
        ResourceBundle res = ResourceBundle.getBundle(bundle, Locale.getDefault());
        return (String) res.getObject(property);
    }
	
	static public String getText(String key) {
		return retrieveMessage(key, BUNDLE_NAME); 
	}

	static public String getText(String key, String defaultValue) {
		final String val = retrieveMessage(key, BUNDLE_NAME);
		return val != null ? val : defaultValue;
	}
	
	/**
	 * Returns a string from the resource bundle and binds it
	 * with the given arguments. If the key is not found,
	 * return the key.
	 */
	public static String getText(String key, Object[] args) {
		try {
			return MessageFormat.format(getText(key), args);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
	}

	public static void showInfoMessage(Shell shell, String title, String message) {
		MessageDialog.openInformation(shell, title, message);
	}
	
	public static void showErrorMessage(Shell shell, String title, String message) {
		MessageDialog.openError(shell, title, message);
	}

	/**
	 * 
	 * @param clazz
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 */
    public static InputStream getInputStream (Class<? extends Object> clazz, String path)
		throws FileNotFoundException
	{
		InputStream is = clazz.getResourceAsStream(path);
	
		if ( is == null ) {
			File file = new File(path);
			
			if ( file.exists() ) {
				return new FileInputStream(file);
			}
			else {
				throw new IllegalArgumentException(path + ": file not found");
			}
		}
		return is;
	}
	
    /**
     * Sub to clean up invalid path chars from a name for storage in WW cache
     * @param name String to clean up
     * @return Clean name: name.replaceAll("[:;\\(\\) ]", "");
     */
    public static String forCachePath(String name) {
    	return name.replaceAll("[,:;\\(\\) ]", "");
    }
    
    /*
     * File Utilities
     */
    public static void writeToFile (File file, byte[] data)
    	throws FileNotFoundException, IOException
    {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(data);
		fos.close();
    }
    
    public static byte[] readFile (File file)
    	throws FileNotFoundException, IOException
    {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		byte[] bytes = new byte[(int)raf.length()];
		raf.readFully(bytes);
		raf.close();
		return bytes;
    }
    
    static public void copyResource(File src, File dest)
    	throws FileNotFoundException, IOException
    {
    	Messages.writeToFile(dest, Messages.readFile(src));
    }

    static public void moveResource(File src, File dest)
		throws FileNotFoundException, IOException
	{
		Messages.writeToFile(dest, Messages.readFile(src));
		src.delete();
	}
    
}
