/*
 *  DynamicPrefChangeManager.java
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
 *		20-May-05	created from de.sciss.meloncillo.util.DynamicPrefChangeManager
 */

package de.sciss.app;

import java.util.prefs.Preferences;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import de.sciss.app.DynamicListening;
import de.sciss.app.PreferenceNodeSync;

/**
 *  A utility class for a mechanism common to many
 *  GUI objects : they listen to certain preferences
 *  and need to act upon changes. However to improve
 *  performance, the listening is paused when the components
 *  are hidden and resumed when they are shown or added
 *  to ancestor GUI components.
 *  <p>
 *  This class tracks a set of preferences specified by
 *  key strings. It implements the DynamicListening interface and
 *  thus can be used in a DynamicAncestorAdapter.
 *  When the listening is resumes, this class checks for
 *  changes that might have occured to the tracked preferences.
 *  All relevant PreferenceChangeEvents are forwarded to the
 *  LaterInvocationManager.Listener provided in the constructor
 *  call.
 *  <p>
 *  Another important issue is to avoid deadlocks that can be
 *  easily caused in the java.util.prefs.Preferences context because
 *  it provides no means to find out who changed the preferences.
 *  Therefore if a component both changes a preferences and tracks
 *  (listens to) changes of the same preference, it can produce
 *  and infinite loop. Besides, if another component changes to
 *  prefs but the old value equals the new value, unneccessary
 *  overhead might be created. To avoid this, this class checks to
 *  see if a preference value really changed - if not, that PreferenceChangeEvent
 *  is <strong>NOT</strong> forwarded to the LIM-Listener.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.13, 15-Sep-05
 */
public class DynamicPrefChangeManager
implements DynamicListening, PreferenceChangeListener, PreferenceNodeSync
{
	private final String[]							keys;
	private Preferences								prefs;
	private final LaterInvocationManager			lim;
	private final LaterInvocationManager.Listener	limClient;
	private final String[]							values;
	private boolean									listening   = false;

	/**
	 *  Constructs a new <code>DynamicPrefChangeManager</code>.
	 *
	 *  @param  prefs		the Preference object to track. Note that the constructor
	 *						does not register a PreferenceChangeListener, this is
	 *						done when the DynamicListening.startListening() is involved.
	 *						Therefore if you plan to not attach the DynamicPrefChangeManager
	 *						to a DynamicAncestor- or -ComponentListener, you'll have
	 *						to call startListening() manually.
	 *  @param  keys		the keys in the specified prefs which we shall track. Initially
	 *						all values are considered null and nothing is broadcast. However,
	 *						if startListening() is called, all values are checked, therefore
	 *						all preference values which are not null will be initially forwarded
	 *						to the LIM-Listener.
	 *  @param  limClient   usually the calling instance. laterInvocation() is called whenever
	 *						the dynamic listening is active and preference changes occur which
	 *						really alter the preference values. The object passed to
	 *						laterInvocation is a PreferenceChangeEvent.
	 */
	public DynamicPrefChangeManager( Preferences prefs, String[] keys,
									 LaterInvocationManager.Listener limClient )
	{
		this.keys		= keys;
		this.prefs		= prefs;
		this.limClient  = limClient;
		lim				= new LaterInvocationManager( limClient );
		values			= new String[ keys.length ];	// all set null by java VM automatically
	}

	public void setPreferences( Preferences prefs )
	{
		if( listening ) {
			stopListening();
			this.prefs	= prefs;
			startListening();
		} else {
			this.prefs	= prefs;
		}
		if( EventManager.DEBUG_EVENTS ) System.err.println( "@pref setPreferences" );
	}

	private void analyzeChanges()
	{
		int		i;
		String  oldValue, newValue;
		
		for( i = 0; i < keys.length; i++ ) {
			oldValue	= values[i];
			newValue	= prefs.get( keys[i], oldValue );

			if( newValue != oldValue && (newValue == null || oldValue == null || !newValue.equals( oldValue ))) {
				values[i] = newValue;
				limClient.laterInvocation( new PreferenceChangeEvent( prefs, keys[i], newValue ));
				if( EventManager.DEBUG_EVENTS ) System.err.println( "@pref direct lim "+keys[i]+"; old = "+oldValue+" --> "+newValue );
			}
		}
	}

// ---------------- DynamicListening interface ---------------- 

    public void startListening()
    {
		if( EventManager.DEBUG_EVENTS ) {
			System.err.print( "@pref startListening. keys = " );
			for( int i = 0; i < keys.length; i++ ) {
				System.err.print( keys[i] + ", " );
			}
			System.err.println();
		}
		if( prefs != null ) {
			prefs.addPreferenceChangeListener( this );
			analyzeChanges();
		}
		listening   = true;
	}

    public void stopListening()
    {
		if( EventManager.DEBUG_EVENTS ) System.err.println( "@pref stopListening" );
		if( prefs != null ) prefs.removePreferenceChangeListener( this );
		listening   = false;
    }

// ---------------- PreferenceChangeListener interface ---------------- 

	public void preferenceChange( PreferenceChangeEvent e )
	{
		String  key		= e.getKey();
		String  newValue, oldValue;
		int		i;
		
// System.err.println( "currentThread : "+Thread.currentThread().getName()+" ; is awt thread ? "+EventQueue.isDispatchThread() );

		for( i = 0; i < keys.length; i++ ) {
			if( keys[i].equals( key )) {
				oldValue	= values[i];
				newValue	= e.getNewValue();
				if( newValue != oldValue &&
					(newValue == null || oldValue == null || !newValue.equals( oldValue ))) {
					
					values[i] = newValue;
					lim.queue( e );
					if( EventManager.DEBUG_EVENTS ) System.err.println( "@pref lim queue "+key+"; old = "+oldValue+" --> "+newValue );
				}
				return;
			}
		}
	}
}
