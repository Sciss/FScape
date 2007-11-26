/*
 *  PathList.java
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
 *		20-May-05	created from de.sciss.meloncillo.io.PathList
 *		25-Feb-06	added stuff from de.sciss.eisenkraut.io.PathList : remove( File ), indexOf, getCapacity
 */

package de.sciss.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

/**
 *  Manages a list of paths
 *  and allows conversion to / from
 *  preferences value strings.
 *  This is used to manage the
 *  list of recently opened files.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.11, 25-Feb-06
 */
public class PathList
{
	/**
	 *  Value: Prefs key string representing the path list of
	 *  a user's set favourite directories. See PathList
	 *  and PathField documentation.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_USERPATHS= "usrpaths";	// string : path list

	private final int				capacity;
	private final List				paths;
	private final Preferences		prefs;
	private final String			prefsKey;

	/**
	 *  Creates a new empty PathList.
	 *
	 *  @param  capacity	maximum number of paths
	 */
	public PathList( int capacity )
	{
		this.capacity   = capacity;
		paths			= new ArrayList( capacity );
		prefs			= null;
		prefsKey		= null;
	}

	/**
	 *  Creates a new PathList reflecting
	 *  a value in a Preferences object.
	 *  The list is initially filled with
	 *  the paths stored in the preference
	 *  entry (if present). All modifications
	 *  like adding or removing paths are
	 *  immediately reflected in the provided
	 *  Preferences.
	 *
	 *  @param  capacity	maximum number of paths
	 */
	public PathList( int capacity, Preferences prefs, String prefsKey )
	{
		this.capacity   = capacity;
		this.paths		= new ArrayList( capacity );
		this.prefs		= prefs;
		this.prefsKey   = prefsKey;
		fromPrefs();
	}

	public int getCapacity()
	{
		return capacity;
	}

	/**
	 *  Returns the number of paths stored
	 *  in the list
	 *
	 *  @return number of stored paths
	 *			(which can be smaller than the capacity)
	 */
	public int getPathCount()
	{
		synchronized( paths ) {
			return paths.size();
		}
	}
	
	/**
	 *  Returns a path at some index in the list
	 *
	 *  @param  index of the path to query, must
	 *			be smaller than getPathCount()
	 *  @return the path at that index
	 */
	public File getPath( int index )
	{
		synchronized( paths ) {
			return( (File) paths.get( index ));
		}
	}
	
	/**
	 *  Replaces a path at some index in the list
	 *
	 *  @param  index in the list whose path should
	 *			be replaced. must be smaller than
	 *			getPathCount()
	 */
	public void setPath( int index, File path )
	{
		synchronized( paths ) {
			paths.set( index, path );
			toPrefs();
		}
	}

	/**
	 *  Removes a path at some index in the list
	 *
	 *  @param  index in the list whose path should
	 *			deleted. Paths following in the list
	 *			will be shifted accordingly. Must be
	 *			smaller than getPathCount()
	 */
	public void remove( int index )
	{
		synchronized( paths ) {
			paths.remove( index );
			toPrefs();
		}
	}

	/**
	 *  Removes a path
	 *
	 *  @param  path	path which should
	 *					deleted. Paths following in the list
	 *					will be shifted accordingly.
	 */
	public void remove( File path )
	{
		synchronized( paths ) {
			paths.remove( path );
			toPrefs();
		}
	}

	/**
	 *  Inserts a new path at
	 *  the head of the list.
	 *  If this would cause the
	 *  capacity to overflow,
	 *  the tail path is removed
	 *
	 *  @param		path	the path to insert
	 *  @return				true if the tail had to be removed
	 */
	public boolean addPathToHead( File path )
	{
		boolean result = false;
	
		synchronized( paths ) {
			paths.add( 0, path );
			if( paths.size() > capacity ) {
				paths.remove( paths.size() - 1 );
				result = true;
			}
			toPrefs();
		}
		return result;
	}

	/**
	 *  Adds a new path to
	 *  the tail of the list.
	 *  If this would cause the
	 *  capacity to overflow,
	 *  the head path is removed
	 *
	 *  @param		path	the path to insert
	 *  @return				true if the head had to be removed
	 */
	public boolean addPathToTail( File path )
	{
		boolean result = false;

		synchronized( paths ) {
			paths.add( path );
			if( paths.size() > capacity ) {
				paths.remove( 0 );
				result = true;
			}
			toPrefs();
		}
		return result;
	}
	
	/**
	 *  Removes all paths from the list.
	 */
	public void clear()
	{
		synchronized( paths ) {
			paths.clear();
			toPrefs();
		}
	}

	/**
	 *  Determines whether a particular
	 *  path is included in the list
	 *
	 *  @param  path	path to look for
	 *  @return true if the path is contained in the list
	 */
	public boolean contains( File path )
	{
		synchronized( paths ) {
			return paths.contains( path );
		}
	}

	/**
	 *  Determines whether a particular
	 *  path is included in the list
	 *
	 *  @param  path	path to look for
	 *  @return the index of the path in the list or -1 if not in the list
	 */
	public int indexOf( File path )
	{
		synchronized( paths ) {
			return paths.indexOf( path );
		}
	}
	
	/*
	 *  Converts all paths into strings
	 *  and concatenate them using a File.pathSeparator separating
	 *  two adjectant paths. Then
	 *  store it in the Preferences.
	 */
	private void toPrefs()
	{
		if( prefs == null ) return;
	
		StringBuffer	buf = new StringBuffer();
	
		synchronized( paths ) {
			for( int i = 0; i < paths.size(); i++ ) {
				buf.append( ((File) paths.get( i )).getAbsolutePath() );
				buf.append( File.pathSeparator );
			}
			if( buf.length() > 0 ) buf.deleteCharAt( buf.length() - 1 );
			
			prefs.put( prefsKey, buf.toString() );
		}
	}

	/*
	 *  Reads a string from the prefs
	 *  which has to be a list of path names
	 *  separated by File.pathSeparator strings.
	 *  This object's path list then
	 *  reflects the paths of the Preferences.
	 */
	private void fromPrefs()
	{
		synchronized( paths ) {
			paths.clear();
			if( prefs == null ) return;
		
			StringTokenizer tok = new StringTokenizer( prefs.get( prefsKey, "" ), File.pathSeparator );
			while( tok.hasMoreTokens() && paths.size() < capacity ) {
				paths.add( new File( tok.nextToken() ));
			}
		}
	}
}