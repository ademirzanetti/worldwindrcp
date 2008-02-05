/*******************************************************************************
 * Copyright (c) 2006 Vladimir Silva and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Silva - initial API and implementation
 *******************************************************************************/
package org.eclipse.plugin.worldwind;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;


public class Messages 
{
	private static final String BUNDLE_NAME = Messages.class.getName();
	
	static public String NL = System.getProperty("line.separator");

	private static String retrieveMessage(String property, String bundle)
    {
        ResourceBundle res = ResourceBundle.getBundle(bundle, Locale.getDefault());
        return (String) res.getObject(property);
    }
	
	static public String getString(String key) {
		return retrieveMessage(key, BUNDLE_NAME);
	}

	/**
	 * Returns a string from the resource bundle and binds it
	 * with the given arguments. If the key is not found,
	 * return the key.
	 */
	public static String getString(String key, Object[] args) {
		try {
			return MessageFormat.format(getString(key), args);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";
		}
	}

	public static void showInfoMessage(Shell shell, String message) {
		MessageDialog.openInformation(shell, getString("info.dialog.title"), message);
	}
	
	public static void showErrorMessage(Shell shell, String message) {
		MessageDialog.openError(shell, getString("err.dialog.title"), message);
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
	
}
