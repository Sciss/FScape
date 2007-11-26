/*
 *  PrefRecentItemsCombo.java
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
 *		21-Jan-06	created
 */

package de.sciss.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JComboBox;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.EventManager;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 17-Apr-07
 *
 *  @see		java.util.prefs.PreferenceChangeListener
 */
public class PrefRecentItemsCombo
extends JComboBox
implements  DynamicListening, PreferenceChangeListener,
			LaterInvocationManager.Listener, PreferenceEntrySync
{
	private boolean								listening		= false;
	private Preferences							prefs			= null;
	private String								key				= null;
	private final LaterInvocationManager		lim				= new LaterInvocationManager( this );
	
	private final de.sciss.gui.ComboBoxEditor	editor;
	private final int							maxItems;
	
	private ActionListener						listener;

	private boolean								readPrefs		= true;
	private boolean								writePrefs		= true;

	/**
	 *  Creates a new <code>PrefRecentItemsCombo</code>
	 *  with default data model and no initial preferences set.
	 */
	public PrefRecentItemsCombo( de.sciss.gui.ComboBoxEditor editor, int maxItems )
	{
		super();
		this.editor		= editor;
		this.maxItems	= maxItems;
		init();
	}
	
	private void init()
	{
		setEditor( editor );
		setEditable( true );
		
		new DynamicAncestorAdapter( this ).addTo( this );
		listener = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				final Object item = editor.getItem();
			
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@ric actionPerformed : "+key+" --> "+item );
				for( int i = 0; i < getItemCount(); i++ ) {
					if( getItemAt( i ).equals( item )) {
						if( getSelectedIndex() != i ) setSelectedIndex( i );
						return;
					}
				}
//				editor.removeActionListener( this );
				addItem( item );
				setSelectedIndex( getItemCount() - 1 );
				if( getItemCount() > maxItems ) removeItemAt( 0 );
				
				if( writePrefs ) writePrefs();
//				editor.addActionListener( this );
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
		final String		value;
		final String		oldValue;
		final StringBuffer	buf;
		Object				item;
		String				itemStr;
	
		if( (prefs != null) && (key != null) ) {
			buf = new StringBuffer();
			for( int i = 0; i < getItemCount(); i++ ) {
				item	= getItemAt( i );
				itemStr	= item.toString();
				// "pascal"-string alike
				buf.append( itemStr.length() );
				if( item instanceof StringItem ) {
					buf.append( ";key=" );
					buf.append( ((StringItem) item).getKey() );
				}
				buf.append( ";val=" );
				buf.append( itemStr );
				buf.append( ';' );
			}
			value		= buf.toString();			
			oldValue	= prefs.get( key, null );
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@comb updatePrefs : "+this.key+"; old = "+oldValue+" --> "+value );
			if( (value != null && oldValue == null) ||
				(value != null && !value.equals( oldValue ))) {
				
				prefs.put( key, value );

//			} else if( value == null && oldValue != null ) {
//				prefs.remove( key );
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
//			defaultValue = getSelectedItem();
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
			if( writePrefs ) editor.addActionListener( listener );
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
			if( writePrefs ) editor.removeActionListener( listener );
			listening = false;
		}
	}

	private void printParseError( String prefsValue )
	{
		System.err.println( "Warning: for key '"+this.key + "' illegal prefs value '"+prefsValue + "'" );
	}
	
	// o instanceof PreferenceChangeEvent
	public void laterInvocation( Object o )
	{
		final String prefsValue = ((PreferenceChangeEvent) o).getNewValue();
		readPrefsFromString( prefsValue );
	}
	
	public void readPrefs()
	{
		if( (prefs != null) && (key != null) ) readPrefsFromString( prefs.get( key, null ));
	}
		
	private void readPrefsFromString( String prefsValue )
	{
//		if( prefsValue == null && defaultValue != null ) {
//			updatePrefs( defaultValue );
//			return;
//		}
		
		final Object			item;
		final List				collItems	= new ArrayList();
		int						k, m;
		String					s, key;
		Object					item2;
		
//System.err.println( "checking "+prefsValue );
		if( listening && writePrefs ) editor.removeActionListener( listener );

		try {
			for( int i = 0; i < prefsValue.length(); ) {
				key	= null;
				k	= prefsValue.indexOf( ';', i );
				m	= Integer.parseInt( prefsValue.substring( i, k ));
				i	= k + 1;
				k	= i + 4;
				s	= prefsValue.substring( i, k );
				i	= k;
				if( s.equals( "key=" )) {
					k	= prefsValue.indexOf( ';', i );
					key	= prefsValue.substring( i, k );
					i	= k + 1;
					k	= i + 4;
					s	= prefsValue.substring( i, k );
					i	= k;
				}
				if( !s.equals( "val=" )) {
					printParseError( prefsValue );
					return;
				}
				s	= prefsValue.substring( i, i + m );
				i	= i + m + 1;
				if( key == null ) {
					collItems.add( s );
				} else {
					collItems.add( new StringItem( key, s ));
				}
			}
		}
		catch( NumberFormatException e1 ) {
			printParseError( prefsValue );
			return;
		}
		catch( IndexOutOfBoundsException e1 ) {
			printParseError( prefsValue );
			return;
		}
		
checkEquality:
		if( collItems.size() == getItemCount() ) {
			for( int i = 0; i < collItems.size(); i++ ) {
				if( !collItems.get( i ).equals( getItemAt( i ))) break checkEquality;
			}
//System.err.println( "all the same!" );
			return;	// prefs value equals gui value
		}
		
//		editor.removeActionListener( listener );
		item	= editor.getItem();
		editor.setComboGate( false );
		removeAllItems();	// for fuck's sake this will overwrite the editor item
		k		= -1;
		for( int i = 0; i < collItems.size(); i++ ) {
			item2	= collItems.get( i );
			addItem( item2 );
			if( item2.equals( item )) {
				k = i;
			}
		}
		setSelectedIndex( k );
		editor.setComboGate( true );
//System.err.println( "settin "+item );
//		editor.setItem( item );

		if( listening && writePrefs ) editor.addActionListener( listener );
	}
	
	public void preferenceChange( PreferenceChangeEvent e )
	{
		if( e.getKey().equals( key )) {
			if( EventManager.DEBUG_EVENTS ) System.err.println( "@comb preferenceChange : "+key+" --> "+e.getNewValue() );
			lim.queue( e );
		}
	}
}
