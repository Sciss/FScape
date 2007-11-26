/*
 *  KeyStrokeTextField.java
 *  de.sciss.gui package
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
 *		20-May-05	created from de.sciss.meloncillo.gui.KeyStrokeTextField
 */

package de.sciss.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.EventManager;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;

/**
 *  A specialized textfield that records
 *  a keystroke shortcut. Preferences are
 *	stored as a string of two integers
 *	separated by a comma : "keyCode,modifiers"
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 17-Apr-07
 */
public class KeyStrokeTextField
extends JTextField
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync
{
	private boolean					listening		= false;
	private Preferences				prefs			= null;
	private String					key				= null;
	private final LaterInvocationManager lim		= new LaterInvocationManager( this );
//	private ActionListener listener;
	
	private KeyStroke				guiStroke		= null;
	private KeyStroke				defaultValue	= null;

	private boolean					readPrefs		= true;
	private boolean					writePrefs		= true;
	
	/**
	 *  Creates a new empty <code>KeyStrokeTextField</code>
	 *  with no preferences initially set
	 */
	public KeyStrokeTextField()
	{
		super( 24 );
		init();
	}

	/**
	 *  Creates a new <code>KeyStrokeTextField</code>
	 *  with no preferences initially set
	 *
	 *  @param  stroke	the initial gadget's key stroke content
	 */
	public KeyStrokeTextField( KeyStroke stroke )
	{
		super( strokeToString( stroke ), 24 );
		defaultValue = stroke;
		init();
	}
	
	public void setReadPrefs( boolean b )
	{
		if( b != readPrefs ) {
			readPrefs	= b;
			if( (prefs != null) && listening ) {
				if( readPrefs ) prefs.addPreferenceChangeListener( this );
				else prefs.removePreferenceChangeListener( this );
			}
		}
	}
	
	public boolean getReadPrefs()
	{
		return readPrefs;
	}
	
	public void setWritePrefs( boolean b )
	{
		writePrefs	= b;
	}
	
	public boolean getWritePrefs()
	{
		return writePrefs;
	}

	/**
	 *  Converts a a key stroke's string representation as
	 *	from preference storage into a KeyStroke object.
	 *
	 *  @param		prefsValue		a string representation of the form &quot;modifiers keyCode&quot;
	 *								or <code>null</code>
	 *	@return		the KeyStroke parsed from the prefsValue or null if the string was
	 *				invalid or <code>null</code>
	 */
	public static final KeyStroke prefsToStroke( String prefsValue )
	{
		if( prefsValue == null ) return null;
		int i = prefsValue.indexOf( ' ' );
		KeyStroke prefsStroke = null;
		try {
			if( i < 0 ) return null;
			prefsStroke = KeyStroke.getKeyStroke( Integer.parseInt( prefsValue.substring( i+1 )),
												  Integer.parseInt( prefsValue.substring( 0, i )));
		}
		catch( NumberFormatException e1 ) {}

		return prefsStroke;
	}
	
	/**
	 *  Converts a KeyStroke into a string representation for
	 *	preference storage.
	 *
	 *  @param		prefsStroke	the KeyStroke to convert
	 *	@return		a string representation of the form &quot;modifiers keyCode&quot;
	 *				or <code>null</code> if the prefsStroke is invalid or <code>null</code>
	 */
	public static final String strokeToPrefs( KeyStroke prefsStroke )
	{
		if( prefsStroke == null ) return null;
		else return String.valueOf( prefsStroke.getModifiers() ) + ' ' +
					String.valueOf( prefsStroke.getKeyCode() );
	}

	private void init()
	{
		this.addKeyListener( new KeyListener() {
			public void keyPressed( KeyEvent e )
			{
				switch( e.getKeyCode() ) {
				case KeyEvent.VK_UNDEFINED:
				case KeyEvent.VK_ALT:
				case KeyEvent.VK_SHIFT:
				case KeyEvent.VK_META:
				case KeyEvent.VK_CONTROL:
				case KeyEvent.VK_ALT_GRAPH:
				case KeyEvent.VK_NUM_LOCK:
				case KeyEvent.VK_CAPS_LOCK:
					return;
				default:
					break;
				}
				
				guiStroke = KeyStroke.getKeyStroke( e.getKeyCode(), e.getModifiers() );
				
				if( writePrefs ) writePrefs();
				
				e.consume();
			}
			
			public void keyReleased( KeyEvent e )
			{
				e.consume();
			}

			public void keyTyped( KeyEvent e )
			{
				e.consume();
			}
		});

		new DynamicAncestorAdapter( this ).addTo( this );
	}
	
	public void writePrefs()
	{
		if( (prefs != null) && (key != null) ) {
			KeyStroke prefsStroke = KeyStrokeTextField.prefsToStroke( prefs.get( key, null ));
			if( EventManager.DEBUG_EVENTS ) {
				System.err.println( "@text updatePrefs : "+this.key+"; old = "+strokeToString( prefsStroke )+" --> "+strokeToString( guiStroke ));
			}
			if( (prefsStroke == null && guiStroke != null) ||
				(prefsStroke != null && guiStroke == null) ||
				(prefsStroke != null && guiStroke != null && !prefsStroke.equals( guiStroke ))) {

				prefs.put( key, KeyStrokeTextField.strokeToPrefs( guiStroke ));
			}
			setText( strokeToString( guiStroke ));
		}
	}

	private static final String strokeToString( KeyStroke stroke )
	{
		return KeyEvent.getKeyModifiersText( stroke.getModifiers() ) + ' ' +
			   KeyEvent.getKeyText( stroke.getKeyCode() );
	}

	public void setPreferenceNode( Preferences prefs )
	{
		setPreferences( prefs, this.key );
	}

	public void setPreferenceKey( String key )
	{
		setPreferences( this.prefs, key );
	}

	public void setPreferences( Preferences prefs, String key )
	{
		if( listening ) {
			stopListening();
			this.prefs  = prefs;
			this.key	= key;
			startListening();
		} else {
			this.prefs  = prefs;
			this.key	= key;
		}
	}

	public Preferences getPreferenceNode() { return prefs; }
	public String getPreferenceKey() { return key; }

	public void startListening()
	{
		if( (prefs != null) && readPrefs ) {
			prefs.addPreferenceChangeListener( this );
			listening	= true;
			if( key != null ) {
				readPrefsFromString( prefs.get( key, null ));
			}
		}
	}

	public void stopListening()
	{
		if( prefs != null ) {
			prefs.removePreferenceChangeListener( this );
			listening = false;
		}
	}
	
	// o instanceof PreferenceChangeEvent
	public void laterInvocation( Object o )
	{
		readPrefsFromString( ((PreferenceChangeEvent) o).getNewValue() );
	}
	
	public void readPrefs()
	{
		if( (prefs != null) && (key != null) ) readPrefsFromString( prefs.get( key, null ));
	}

	private void readPrefsFromString( String prefsValue )
	{
		KeyStroke prefsStroke   = KeyStrokeTextField.prefsToStroke( prefsValue );
		if( prefsStroke == null ) {
			if( defaultValue != null ) {
				guiStroke = defaultValue;
				if( writePrefs ) writePrefs();
			}
			return;
		}

		if( guiStroke == null || (guiStroke != null && !prefsStroke.equals( guiStroke ))) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@text setText" );
			setText( strokeToString( prefsStroke ));
			guiStroke = prefsStroke;
		}
	}
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) {
				System.err.println( "@text preferenceChange : "+key+" --> "+e.getNewValue() );
			}
			lim.queue( e );
		}
	}
}