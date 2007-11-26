/*
 *  PrefTextField.java
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
 *		11-Aug-05	copied from de.sciss.eisenkraut.gui.PrefTextField
 *		21-Jan-06	implements ComboBoxEditor
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JTextField;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.EventManager;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;

/**
 *  Equips a normal JTextField with
 *  preference storing / recalling capabilities.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 17-Apr-07
 */
public class PrefTextField
extends JTextField
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync,
			de.sciss.gui.ComboBoxEditor
{
	private boolean					listening		= false;
	private Preferences				prefs			= null;
	private String					key				= null;
	private final LaterInvocationManager lim		= new LaterInvocationManager( this );
	private ActionListener			listener;
	
	private String					defaultValue	= null;
	private boolean					comboGate		= true;

	private boolean					readPrefs		= true;
	private boolean					writePrefs		= true;
	
	/**
	 *  Creates a new empty <code>PrefTextField</code>
	 *  with no preferences initially set
	 */
	public PrefTextField()
	{
		super();
		init();
	}

	/**
	 *  Creates a new <code>PrefTextField</code>
	 *  with no preferences initially set
	 *
	 *  @param  text	the initial gadget's content
	 */
	public PrefTextField( String text )
	{
		super( text );
		init();
	}

	/**
	 *  Creates a new <code>PrefTextField</code>
	 *  with no preferences initially set
	 *
	 *  @param  text	the initial gadget's content
	 *  @param  columns number of columns for the text field
	 *					(affects preferred layout size)
	 */
	public PrefTextField( String text, int columns )
	{
		super( text, columns );
		init();
	}

	/**
	 *  Creates a new empty <code>PrefTextField</code>
	 *  with no preferences initially set
	 *
	 *  @param  columns number of columns for the text field
	 *					(affects preferred layout size)
	 */
	public PrefTextField( int columns )
	{
		super( columns );
		init();
	}

	private void init()
	{
		new DynamicAncestorAdapter( this ).addTo( this );
		listener = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@text actionPerformed : "+key+" --> "+getText() );
				if( writePrefs ) writePrefs();
			}
		};
	}
	
	public void setReadPrefs( boolean b )
	{
		if( b != readPrefs ) {
			readPrefs	= b;
			if( (prefs != null) && listening ) {
				if( readPrefs ) {
					prefs.addPreferenceChangeListener( this );
				} else {
					prefs.removePreferenceChangeListener( this );
				}
			}
		}
	}
	
	public boolean getReadPrefs()
	{
		return readPrefs;
	}
	
	public void setWritePrefs( boolean b )
	{
		if( b != writePrefs ) {
			writePrefs	= b;
			if( (prefs != null) && listening ) {
				if( writePrefs ) {
					this.addActionListener( listener );
				} else {
					this.removeActionListener( listener );
				}
			}
		}
	}
	
	public boolean getWritePrefs()
	{
		return writePrefs;
	}

	public void writePrefs()
	{
		if( (prefs != null) && (key != null) ) {
			final String prefsValue = prefs.get( key, null );
			final String guiValue = getText();
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@text updatePrefs : "+this.key+"; old = "+prefsValue+" --> "+guiValue );
			if( (prefsValue == null && guiValue != null) ||
				(prefsValue != null && guiValue == null) ||
				(prefsValue != null && guiValue != null && !prefsValue.equals( guiValue ))) {

				prefs.put( key, guiValue );
			}
		}
	}

	// -------------- PreferenceEntrySync interface --------------

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
		if( (this.prefs == null) || (this.key == null) ) {
			defaultValue = getText();
		}
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

	// -------------- DynamicListening interface --------------

	public void startListening()
	{
		if( prefs != null ) {
			listening	= true;
			if( writePrefs ) this.addActionListener( listener );
			if( readPrefs ) {
				prefs.addPreferenceChangeListener( this );
				readPrefs();
			}
		}
	}

	public void stopListening()
	{
		if( prefs != null ) {
			if( readPrefs ) prefs.removePreferenceChangeListener( this );
			if( writePrefs ) this.removeActionListener( listener );
			listening = false;
		}
	}

	// -------------- LaterInvocationManager.Listener interface --------------

	// o instanceof PreferenceChangeEvent
	public void laterInvocation( Object o )
	{
		String prefsValue   = ((PreferenceChangeEvent) o).getNewValue();
		readPrefsFromString( prefsValue );
	}
	
	public void readPrefs()
	{
		if( (prefs != null) && (key != null) ) readPrefsFromString( prefs.get( key, null ));
	}

	private void readPrefsFromString( String prefsValue )
	{
		if( prefsValue == null ) {
			if( defaultValue != null ) {
//				if( listening && writePrefs ) this.removeActionListener( listener );
//				guiValue = defaultValue;
				setText( defaultValue );
				if( writePrefs ) writePrefs();
//				if( listening && writePrefs ) this.addActionListener( listener );
			}
			return;
		}
		final String guiValue		= getText();

		if( (guiValue == null) || !prefsValue.equals( guiValue )) {

			// though we filter out events when preferences effectively
			// remain unchanged, it's more clean and produces less
			// overhead to temporarily remove our ActionListener
			// so we don't produce potential loops
			if( listening && writePrefs ) this.removeActionListener( listener );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@text setText" );
			setText( prefsValue );
			fireActionPerformed();
			if( listening && writePrefs ) this.addActionListener( listener );
		}
	}

	// -------------- PreferenceChangeListener interface --------------
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@text preferenceChange : "+key+" --> "+e.getNewValue() );
			lim.queue( e );
		}
	}

	// -------------- ComboBoxEditor interface --------------
	
	public Component getEditorComponent()
	{
		return this;
	}
	
	public void setComboGate( boolean gate )
	{
		comboGate = gate;
	}

	public void setItem( Object anObject )
	{
		if( !comboGate || (anObject == null) ) return;
		
//System.err.println( "setItem "+anObject );
//		if( listening ) this.removeActionListener( listener );
		setText( anObject.toString() );
//		if( listening ) this.addActionListener( listener );
		if( writePrefs ) writePrefs();
	}
	
	public Object getItem()
	{
		return getText();
	}
	
	// already part of JTextField
//	public void selectAll()
//	public void addActionListener( ActionListener l );
//	public void removeActionListener( ActionListener l );
}
