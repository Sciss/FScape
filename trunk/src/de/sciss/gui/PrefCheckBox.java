/*
 *  PrefCheckBox.java
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
 *		20-May-05	created from de.sciss.meloncillo.gui.PrefCheckBox
 */

package de.sciss.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JCheckBox;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.EventManager;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;

/**
 *  Equips a normal JCheckBox with
 *  preference storing / recalling capabilities.
 *  To preserve maximum future compatibility,
 *  we decided to not override setSelected()
 *  and the like but to install an internal
 *  ActionListener. Thus, there are two ways
 *  to alter the gadget state, either by invoking
 *  the doClick() methods (DON'T USE setSelected()
 *  because it doesn't fire events) or by
 *  changing the associated preferences.
 *  The whole mechanism would be much simpler
 *  if we reduced listening to the preference
 *  changes, but a) this wouldn't track user
 *  GUI activities, b) the PrefCheckBox can
 *  be used with preferences set to null.
 *  When a preference change occurs, the
 *  doClick() method is called, allowing
 *  clients to add ActionListeners to the
 *  gadget in case they don't want to deal
 *  with preferences. However, when possible
 *  it is recommended to use PreferenceChangeListener
 *  mechanisms.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.28, 17-Apr-07
 *
 *  @see		java.util.prefs.PreferenceChangeListener
 */
public class PrefCheckBox
extends JCheckBox
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync
{
	private boolean							listening		= false;
	private Preferences						prefs			= null;
	private String							key				= null;
	private final LaterInvocationManager	lim				= new LaterInvocationManager( this );
	private ActionListener					listener;
	
	private boolean							defaultValue;

	private boolean							readPrefs		= true;
	private boolean							writePrefs		= true;

	/**
	 *  Constructs a new <code>PrefCheckBox</code>
	 *  with no initial preferences set.
	 */
	public PrefCheckBox()
	{
		super();
		init();
	}

	/**
	 *  Constructs a new <code>PrefCheckBox</code>
	 *  with a given text label and no preferences set.
	 *
	 *  @param  text	label of the checkbox
	 */
	public PrefCheckBox( String text )
	{
		super( text );
		init();
	}

	/**
	 *  Constructs a new <code>PrefCheckBox</code>
	 *  with a given action and no initial preferences set.
	 *
	 *  @param  a   action to attach to the checkbox
	 */
	public PrefCheckBox( Action a )
	{
		super( a );
		init();
	}

	private void init()
	{
		new DynamicAncestorAdapter( this ).addTo( this );
		listener = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@chbx actionPerformed : "+key+" --> "+isSelected() );
//				guiState = isSelected();
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

//	public void setSelected( boolean state )
//	{
//		if( EventManager.DEBUG_EVENTS ) System.err.println( "@chbx setSelected : "+key+" --> "+state );
//		super.setSelected( state );
//		updatePrefs( state );
//	}
	
	public void writePrefs()
	{
		if( (prefs != null) && (key != null) ) {
			final boolean guiState		= isSelected();
			final boolean prefsState	= prefs.getBoolean( key, !guiState );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@chbx updatePrefs : "+this.key+"; old = "+prefsState+" --> "+guiState );
			if( prefsState != guiState ) {
				prefs.putBoolean( key, guiState );
			}
		}
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
		if( (this.prefs == null) || (this.key == null) ) {
			defaultValue = isSelected();
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
//			if( listening && writePrefs ) this.removeActionListener( listener );
			setSelected( defaultValue );
//			if( listening && writePrefs ) this.addActionListener( listener );
			if( writePrefs ) writePrefs();
			return;
		}
		boolean prefsState;
		boolean guiState	= isSelected();

		prefsState  = new Boolean( prefsValue ).booleanValue();
		if( prefsState != guiState ) {
			// though we filter out events when preferences effectively
			// remain unchanged, it's more clean and produces less
			// overhead to temporarily remove our ActionListener
			// so we don't produce potential loops
			if( listening && writePrefs ) this.removeActionListener( listener );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@chbx doClick" );
			doClick(); // setSelected( b );
			if( listening && writePrefs ) this.addActionListener( listener );
		}
	}
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@chbx preferenceChange : "+key+" --> "+e.getNewValue() );
			lim.queue( e );
		}
	}
}
