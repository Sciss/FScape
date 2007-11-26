/*
 *  AbstractApplication.java
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *  A rudimentary implementation of the <code>de.sciss.app.Application</code>
 *	interface, which deals with component registration, quitting,
 *	preference, clipboard and resource bundle generation. It
 *	extends <code>net.roydesign.app.Application</code> in order
 *	to supply easy access to methods.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.13, 15-Sep-05
 */
public abstract class AbstractApplication
extends net.roydesign.app.Application
implements de.sciss.app.Application
{
	/*
	 *  This ResourceBundle contains all of the strings used in this application.
	 */
	private final ResourceBundle resBundle;

	private final Map					mapComponents	= new HashMap();

	private final Class					mainClass;
	private Preferences					userPrefs		= null;
	private Preferences					systemPrefs		= null;
	private Clipboard					clipboard		= null;

	private static Application app = null;

	/**
	 *	Creates a new instance of this class. The <code>mainClass</code>
	 *	will be used to determine the application's preference root node.
	 *	This <code>name</code> will be used to set the application's name
	 *	and to load a <code>ResourceBundle</code> with localized text strings.
	 *	The resource bundle must be stored in a file
	 *	<code>&quot;<var>&lt;name&gt;</var>Strings.properties&quot;</code>,
	 *	e.g. if <code>name</code> equals <code>&quot;FScape&quot;</code>, the
	 *	resources are retrieved from <code>&quot;FScapeStrings.properties&quot;</code>!
	 *	<p>
	 *	Only one instance of this class can be created, any further attempt
	 *	will result in a <code>RuntimeException</code>. The one and only instance
	 *	is available through the class method <code>getApplication</code>.
	 *
	 *	@param	mainClass	the main class of the application whose package
	 *						(e.g. <code>de.sciss.fscape</code>) is used to identify
	 *						the preferences files.
	 *	@param	name		application name, should not contain white spaces
	 *						or special characters.
	 *
	 *	@see	#getApplication
	 */
	protected AbstractApplication( Class mainClass, String name )
	{
		super();

		if( AbstractApplication.app != null ) {	// only one application allowed
			throw new RuntimeException( "AbstractApplication cannot be instantiated more than once" );
		}
		AbstractApplication.app	= this;
		setName( name );
		
		resBundle		= ResourceBundle.getBundle( name + "Strings", Locale.getDefault() );
		this.mainClass	= mainClass;
		
	}

	public final Preferences getSystemPrefs()
	{
		if( systemPrefs == null ) {
			systemPrefs = Preferences.systemNodeForPackage( mainClass );
		}
	
		return systemPrefs;
	}
	
	public final Preferences getUserPrefs()
	{
		if( userPrefs == null ) {
			userPrefs = Preferences.userNodeForPackage( mainClass );
		}
	
		return userPrefs;
	}

//	/**
//	 *	@sync	this method is synchronized
//	 */
//	public UndoManager getUndoManager()
//	{
//		if( undo == null ) {
//			// check again with synchronization
//			// ; the sync is placed here to speed
//			// up normal operation once the undo manager has been created!
//			synchronized( this ) {
//				if( undo == null ) {
//					undo = new de.sciss.app.UndoManager( this );
//				}
//			}
//		}
//		
//		return undo;
//	}
	
	/**
	 *	Returns the system's clipboard, or a local
	 *	clipboard if security permits to retrieve the system clipboard.
	 */
	public Clipboard getClipboard()
	{
		if( clipboard == null ) {
			try {
				clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			}
			catch( SecurityException e1 ) {
				clipboard = new Clipboard( getName() );
			}
		}
		
		return clipboard;
	}

	/**
	 *	@synchronization	this method is synchronized
	 */
	public Object getComponent( Object key )
	{
		synchronized( mapComponents ) {
			return mapComponents.get( key );
		}
	}

	/**
	 *	@synchronization	this method is synchronized
	 */
	public void addComponent( Object key, Object component )
	{
		synchronized( mapComponents ) {
			mapComponents.put( key, component );
		}
	}

	/**
	 *	@synchronization	this method is synchronized
	 */
	public void removeComponent( Object key )
	{
		synchronized( mapComponents ) {
			mapComponents.remove( key );
		}
	}

	/**
	 *	Returns <code>this</code>.
	 */
	public final net.roydesign.app.Application getMRJApplication()
	{
		return this;
	}

	public final String getResourceString( String key )
	{
		try {
			return resBundle.getString( key );
		}
		catch( MissingResourceException e1 ) {
			return( "[Missing Resource: " + key + "]" );
		}
	}

	public final String getResourceString( String key, String defaultValue )
	{
		try {
			return resBundle.getString( key );
		}
		catch( MissingResourceException e1 ) {
			return( defaultValue );
		}
	}

	/**
	 *	Flushes preferences and quits.
	 */
	public synchronized void quit()
	{
		boolean success = false;
	
		try {
			if( systemPrefs != null ) systemPrefs.flush();
			if( userPrefs != null )   userPrefs.flush();
			success = true;
		}
		catch( BackingStoreException e1 ) {
			System.err.println( "error while flushing prefs : "+e1.getLocalizedMessage() );
		}
		finally {
			System.exit( success ? 0 : 1 );
		}
	}
	
	/**
	 *	Returns the current runtime's application.
	 *	Since this is a class method, it can be used by
	 *	static classes such IO or GUI utilities to retrieve
	 *	the application and its preferences or resource bundle.
	 *
	 *	@return	the active <code>Application</code> or <code>null</code>
	 *			if no application has been created.
	 */
	public static final Application getApplication()
	{
		return app;
	}
}