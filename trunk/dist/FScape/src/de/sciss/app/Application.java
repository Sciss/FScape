/*
 *  Application.java
 *  de.sciss.app package
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		20-May-05	created as an extract from de.sciss.meloncillo.Main
 */

package de.sciss.app;

import java.awt.datatransfer.Clipboard;
import java.util.prefs.Preferences;

// import net.roydesign.app.Application;

/**
 *  The <code>Application</code> interface is an attempt
 *	to create common classes and interfaces (the package
 *	<code>de.sciss.app</code>) which can be shared by
 *	different programmes, such as Meloncillo or FScape,
 *	without having to make adjustments in different places
 *	each time a modification is made. This interface
 *	describes the most prominent methods needed for
 *	a general GUI based application.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 20-May-05
 */
public interface Application
{
	/**
	 *	Returns the applications system wide preferences.
	 *
	 *	@return	the root node of the application's global
	 *			preferences
	 *
	 *	@see	java.util.prefs.Preferences#systemNodeForPackage( Class )
	 */
	public Preferences getSystemPrefs();
	
	/**
	 *	Returns the applications user specific preferences.
	 *
	 *	@return	the root node of the application's preferences
	 *			from the current user's local folder
	 *
	 *	@see	java.util.prefs.Preferences#userNodeForPackage( Class )
	 */
	public Preferences getUserPrefs();
	
	/**
	 *	Returns the application programme version.
	 *
	 *	@return	current application version
	 */
	public double getVersion();

	/**
	 *	Returns the application's name.
	 *
	 *	@return	the application's name
	 */
	public String getName();

	/**
	 *	Returns the four character Mac OS specific creator code
	 *	as registered on the Apple Developer Connection. This creator
	 *	code can be used to tag files with the corresponding
	 *	application. This method will return the code independant
	 *	of the running operating system.
	 *
	 *	@return	the creator code for this application or <code>null</code>
	 *			if there hasn't been registered any code.
	 *
	 *	@see	net.roydesign.mac.MRJAdapter#setFileCreator( java.io.File, java.lang.String )
	 */
	public String getMacOSCreator();

	/**
	 *	Returns the clipboard used by the application.
	 *
	 *	@return	the clipboard used by the application
	 *
	 *	@see	java.awt.Toolkit#getSystemClipboard()
	 */
	public Clipboard getClipboard();

	/**
	 *  Retrieves a specific component (such as a GUI frame) of the application.
	 *  
	 *  @param  key		agreed upon idenfier for the component,
	 *					e.g. a string or class
	 *  @return			the requested component or <code>null</code> if absent or unknown
	 */
	public Object getComponent( Object key );
	
	/**
	 *  Adds a newly created component (e.g. a specific frame) to the application.
	 *	Adding means making it known to other components which can retrieve this
	 *	object by calling the <code>getComponent</code> method.
	 *  
	 *  @param	key			agreed upon idenfier for the component,
	 *						e.g. a string or class
	 *  @param  component	the component to be registered
	 */
	public void addComponent( Object key, Object component );
	
	/**
	 *  Unregisters a component, for example when a frame has been disposed.
	 *	This will reomve the component from the internal dictionary.
	 *  
	 *  @param	key			agreed upon idenfier for the component to be removed,
	 *						e.g. a string or class
	 */
	public void removeComponent( Object key );
	
	public DocumentHandler getDocumentHandler();

	public WindowHandler getWindowHandler();

	public GraphicsHandler getGraphicsHandler();
	
	/**
	 *	Returns an instance of MRJAdapter's <code>Application</code>
	 *	class which will deal with basic platform dependent GUI
	 *	operations such as providing a Quit or Preferences menu item,
	 *	handling file open requests from the system etc.
	 *
	 *	@return	the <code>Application</code> class providing access to
	 *			common menu items and system events registration
	 */
	public net.roydesign.app.Application getMRJApplication();

	/**
	 *	Returns a localized string for a given
	 *	key. If the key is not found, returns a warning
	 *	text and the key name.
	 *
	 *	@param	key		a key into the application's main string recource file
	 *	@return	the localized text
	 *
	 *	@see	java.util.ResourceBundle#getString( String )
	 */
	public String getResourceString( String key );

	/**
	 *	Returns a localized string for a given
	 *	key. If the key is not found, returns the
	 *	given default string.
	 *
	 *	@param	key				a key into the application's main string recource file
	 *	@param	defaultValue	the text to return if the key is
	 *							not in the dictionary
	 *	@return					the localized text
	 *
	 *	@see	java.util.ResourceBundle#getString( String )
	 */
	public String getResourceString( String key, String defaultValue );
	
	/**
	 *	Forces to application to quit.
	 *	The application will perform necessary cleanup
	 *	such as flushing the preferences.
	 */
	public void quit();
}