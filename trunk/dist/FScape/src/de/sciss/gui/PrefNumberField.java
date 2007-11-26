/*
 *  PrefNumberField.java
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.PrefNumberField
 */

package de.sciss.gui;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.EventManager;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;

import de.sciss.gui.NumberEvent;
import de.sciss.gui.NumberField;
import de.sciss.gui.NumberListener;

/**
 *  Equips a NumberField with
 *  preference storing / recalling capabilities.
 *  We decided not to override setNumber().
 *  Thus, there are two ways
 *  to alter the gadget state, either by invoking
 *  the setNumberAndDispatch() method (DON'T USE setNumber()
 *  because it doesn't fire events) or by
 *  changing the associated preferences.
 *  When a preference change occurs, the
 *  setNumberAndDispatch() method is called, allowing
 *  clients to add NumberListeners to the
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
public class PrefNumberField
extends NumberField
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync
{
	private boolean							listening		= false;
	private Preferences						prefs			= null;
	private String							key				= null;
	private final LaterInvocationManager	lim				= new LaterInvocationManager( this );
	private NumberListener					listener;
	
	private Number							defaultValue	= null;

	private boolean							readPrefs		= true;
	private boolean							writePrefs		= true;

	/**
	 *  Constructs a new <code>PrefNumberField</code>.
	 *
	 *  @synchronization	Like any other Swing component,
	 *						the constructor is to be called
	 *						from the event thread.
	 */
	public PrefNumberField()
	{
		super();
		
		new DynamicAncestorAdapter( this ).addTo( this );
		listener = new NumberListener() {
			public void numberChanged( NumberEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb numberChanged : "+key+" --> "+e.getNumber()+" ; node = "+(prefs != null ? prefs.name() : "null" ));
				if( writePrefs ) writePrefs();
			}
		};
	}
	
//	public void setNumber( Number number )
//	{
//		if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb setNumber : "+key+" --> "+number );
//		super.setNumber( number );
//		updatePrefs( number );
//	}
	
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
					this.addListener( listener );
				} else {
					this.removeListener( listener );
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
			Number prefsNumber;
			final Number guiNumber = getNumber();
			if( getSpace().isInteger() ) {
							// default value mustn't be guiNumber.doubleValue()
				prefsNumber	= new Long( prefs.getLong( key, guiNumber.longValue() + 1 ));
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb updatePrefs : "+this.key+"; old = "+prefsNumber+" --> "+guiNumber );
				if( !guiNumber.equals( prefsNumber )) {
					prefs.putLong( key, guiNumber.longValue() );
				}
			} else {
				prefsNumber	= new Double( prefs.getDouble( key, guiNumber.doubleValue() + 1.0 ));
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb updatePrefs : "+this.key+"; old = "+prefsNumber+" --> "+guiNumber );
				if( !guiNumber.equals( prefsNumber )) {
					prefs.putDouble( key, guiNumber.doubleValue() );
				}
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
	
	/**
	 *  Enable Preferences synchronization.
	 *  This method is not thread safe and
	 *  must be called from the event thread.
	 *  When a preference change is received,
	 *  the number is updated in the GUI and
	 *  the NumberField dispatches a NumberEvent.
	 *  Likewise, if the user adjusts the number
	 *  in the GUI, the preference will be
	 *  updated. The same is true, if you
	 *  call setNumber.
	 *  
	 *  @param  prefs   the preferences node in which
	 *					the value is stored, or null
	 *					to disable prefs sync.
	 *  @param  key		the key used to store and recall
	 *					prefs. the value is the number
	 *					converted to a string.
	 */
	public void setPreferences( Preferences prefs, String key )
	{
		if( (this.prefs == null) || (this.key == null) ) {
			defaultValue = getNumber();
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
			if( writePrefs ) this.addListener( listener );
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
			if( writePrefs ) this.removeListener( listener );
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
			if( defaultValue != null ) {
				setNumber( defaultValue );
				if( writePrefs ) writePrefs();
			}
			return;
		}
		Number prefsNumber;
		Number guiNumber	= getNumber();
		
		try {
			if( getSpace().isInteger() ) {
				prefsNumber	= new Long( prefsValue );
			} else {
				prefsNumber	= new Double( prefsValue );
			}
		}
		catch( NumberFormatException e1 ) {
			prefsNumber		= guiNumber;
		}
	
//System.err.println( "lim : "+prefsNumber );
		if( !prefsNumber.equals( guiNumber )) {
			// thow we filter out events when preferences effectively
			// remain unchanged, it's more clean and produces less
			// overhead to temporarily remove our NumberListener
			// so we don't produce potential loops
			if( listening && writePrefs ) this.removeListener( listener );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb setNumberAndDispatchEvent" );
			setNumber( prefsNumber );
			fireNumberChanged(); // setNumberAndDispatchEvent( prefsNumber );
			if( listening && writePrefs ) this.addListener( listener );
		}
	}
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
//System.err.println( "preferenceChange : "+e.getKey()+" = "+e.getNewValue() );
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@numb preferenceChange : "+key+" --> "+e.getNewValue() );
			lim.queue( e );
		}
	}
}