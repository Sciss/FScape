/*
 *  PrefParamField.java
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
 *		25-Sep-05	created
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

import de.sciss.util.Param;
import de.sciss.util.ParamSpace;

/**
 *  Equips a ParamField with
 *  preference storing / recalling capabilities.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.30, 25-Sep-07
 *
 *  @see		java.util.prefs.PreferenceChangeListener
 */
public class PrefParamField
extends ParamField
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync
{
	private boolean							listening		= false;
	private Preferences						prefs			= null;
	private String							key				= null;
	private final LaterInvocationManager	lim				= new LaterInvocationManager( this );
	private ParamField.Listener				listener;
	
	private Param							defaultValue	= null;

	private boolean							readPrefs		= true;
	private boolean							writePrefs		= true;

	/**
	 *  Constructs a new <code>PrefParamField</code>.
	 *  @synchronization	Like any other Swing component,
	 *						the constructor is to be called
	 *						from the event thread.
	 */
	public PrefParamField()
	{
		super();
		init();
	}
	
	public PrefParamField( final ParamSpace.Translator ut )
	{
		super( ut );
		init();
	}

	private void init()
	{
		new DynamicAncestorAdapter( this ).addTo( this );
		listener = new ParamField.Listener() {
			public void paramValueChanged( ParamField.Event e )
			{
				if( e.isAdjusting() ) return;
			
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@param paramValueChanged : "+key+" --> "+e.getValue()+" ; node = "+(prefs != null ? prefs.name() : "null" ));
				if( writePrefs ) writePrefs();
			}

			public void paramSpaceChanged( ParamField.Event e )
			{
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@param paramSpaceChanged : "+key+" --> "+e.getSpace()+" ; node = "+(prefs != null ? prefs.name() : "null" ));
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
			final String	prefsStr	= prefs.get( key, null );
			final Param		prefsValue	= prefsStr == null ? null : Param.valueOf( prefsStr );
			final Param		guiValue	= getValue();
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@param updatePrefs : "+this.key+"; old = "+prefsValue+" --> "+guiValue );
			if( ((prefsValue == null) && (guiValue != null)) ||
				((prefsValue != null) && ((guiValue == null) || !guiValue.equals( prefsValue ))) ) {

//System.out.println( "Write " + guiValue.toString() );
				prefs.put( key, guiValue.toString() );
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
	 *  the ParamField dispatches a ParamEvent.
	 *  Likewise, if the user adjusts the number
	 *  in the GUI, the preference will be
	 *  updated. The same is true, if you
	 *  call setParam.
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
			defaultValue	= getValue();
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
			listening = true;
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
		final String prefsStr = ((PreferenceChangeEvent) o).getNewValue();
		readPrefsFromString( prefsStr );
	}
	
	public void readPrefs()
	{
		if( (prefs != null) && (key != null) ) readPrefsFromString( prefs.get( key, null ));
	}
		
	public void readPrefsFromString( String prefsStr )
	{
		if( prefsStr == null ) {
			if( defaultValue != null ) {
				setValue( defaultValue );
				if( writePrefs ) writePrefs();
			}
			return;
		}
		
		final int		prefsUnit;
		final int		sepIdx		= prefsStr.indexOf( ' ' );
		final Param		guiValue	= getValue();
		final double	prefsVal;
		Param			prefsValue;
		ParamSpace		newSpace	= null;
		boolean			switchSpace	= false;
		
		try {
			if( sepIdx >= 0 ) {
				prefsVal		= Double.parseDouble( prefsStr.substring( 0, sepIdx ));
				prefsUnit		= ParamSpace.stringToUnit( prefsStr.substring( sepIdx + 1 ));
			} else {
				prefsVal		= Double.parseDouble( prefsStr );
				prefsUnit		= guiValue.unit;	// backward compatibility to number fields
			}
			
			if( prefsUnit != guiValue.unit ) {	// see if there's another space
				for( int i = 0; i < collSpaces.size(); i++ ) {
					newSpace = (ParamSpace) collSpaces.get( i );
					if( newSpace.unit == prefsUnit ) {
						switchSpace = true;
						break;
					}
				}
				if( switchSpace ) {
					prefsValue = new Param( prefsVal, newSpace.unit );
				} else {
					prefsValue = getTranslator().translate( new Param( prefsVal, prefsUnit ), newSpace );
				}
			} else {
				prefsValue = new Param( prefsVal, prefsUnit );
			}
		}
		catch( NumberFormatException e1 ) {
			prefsValue = guiValue;
		}
	
//System.err.println( "lim : "+prefsParam );
		if( !prefsValue.equals( guiValue )) {
			// thow we filter out events when preferences effectively
			// remain unchanged, it's more clean and produces less
			// overhead to temporarily remove our ParamListener
			// so we don't produce potential loops
			if( listening && writePrefs ) this.removeListener( listener );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@param setParamAndDispatchEvent" );
			if( switchSpace ) {
				setSpace( newSpace );
				fireSpaceChanged();
			}
			setValue( prefsValue );
			fireValueChanged( false );
			if( listening && writePrefs ) this.addListener( listener );
		}
	}
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
//System.err.println( "preferenceChange : "+e.getKey()+" = "+e.getNewValue() );
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@param preferenceChange : "+key+" --> "+e.getNewValue() );
			lim.queue( e );
		}
	}

	public void setItem( Object anObject )
	{
		if( !getComboGate() || (anObject == null) ) return;
		
		super.setItem( anObject );
		if( writePrefs ) writePrefs();
	}
}