/*
 *  Marker.java
 *  de.sciss.io package
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
 *		21-May-05	created from de.sciss.eisenkraut.util.Marker
 *		12-Aug-05	removed custom sort() method, instead uses Comparable
 *					interface ; implements Cloneable ; defines
 *					a NameComparator
 *		15-Aug-05	implements Serializable
 */

package de.sciss.io;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 *  A struct class: marker in
 *  an audio file. (copied from FScape).
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.22, 05-May-06
 *
 *  @see	de.sciss.io.AudioFileDescr#KEY_MARKERS
 */
public class Marker
implements Cloneable, Comparable, Serializable
{
// -------- public class fields --------
	/**
	 *	A <code>Comparator</code> which can be
	 *	used to sort a list of markers according
	 *	to the markers' names. Note that to sort
	 *	markers according to their position, you
	 *	can use the <I>natural ordering</I> of
	 *	the markers as defined by the <code>Comparable</code>
	 *	interface.
	 *
	 *	@see	java.util.Collections#sort( List )
	 *	@see	java.util.Collections#sort( List, Comparator )
	 */
	public static final Comparator	nameComparator	= new NameComparator();

// -------- public instance fields --------	

	/**
	 *  A marker's position in sample frames
	 */
	public final long	pos;
	/**
	 *  A marker's name
	 */
	public final String	name;

// -------- public methods --------

	/**
	 *  Constructs a new immutable marker
	 *
	 *  @param  pos		position in sample frames
	 *  @param  name	marker's name
	 */
	public Marker( long pos, String name )
	{
		this.pos	= pos;
		this.name	= name;
	}
	
	/**
	 *  Constructs a new immutable marker
	 *  identical to a given marker.
	 *
	 *  @param  orig	the marker to copy
	 */
	public Marker( Marker orig )
	{
		this.pos	= orig.pos;
		this.name	= orig.name;
	}		

	/**
	 *	Returns a new marker which is
	 *	equal to this one. <code>CloneNotSupportedException</code>
	 *	is never thrown.
	 *
	 *	@return		a new marker with the same name and position
	 *				as this marker
	 */
	public Object clone()
	throws CloneNotSupportedException
	{
		return super.clone();	// field by field copy
	}
	
	public int hashCode()
	{
		return( name.hashCode() ^ (int) pos );
	}
	
	public boolean equals( Object o )
	{
		if( (o != null) && (o instanceof Marker) ) {
			final Marker m = (Marker) o;
			return( this.name.equals( m.name ) && (this.pos == m.pos) );
		} else {
			return false;
		}
	}
	
	/**
	 *	Implementation of the <code>Comparable</code> interface.
	 *	The passed object can be either another <code>Marker</code>
	 *	or a <code>Region</code>. In the latter case, the region's
	 *	start position is compared to this marker's position.
	 *
	 *	@param	o					the object to compare to this marker
	 *	@return						negative or positive value, if the
	 *								object is greater or smaller compared to
	 *								this marker, zero if they are equal
	 *
	 *	@throws	ClassCastException	if <code>o</code> is neither a <code>Markers</code>
	 *								nor a <code>Region</code>
	 */
	public int compareTo( Object o )
	{
		final long pos2;
	
		if( o instanceof Marker ) {
			pos2 = ((Marker) o).pos;	// cannot return mere difference because it might exceed 32bit
		} else if( o instanceof Region ) {
			pos2 = ((Region) o).span.start;
		} else{
			throw new ClassCastException();
		}

		if( this.pos < pos2 ) return -1;
		if( this.pos > pos2 ) return 1;
		return 0;
	}
	
	/**
	 *	Adds marker chronologically to
	 *  a pre-sorted list.
	 *
	 *  @param  markers		a chronological marker list
	 *  @param  marker		the marker to insert such that
	 *						its predecessor has a position
	 *						less or equal this marker's position
	 *						and the marker's successor has a position
	 *						greater than this marker's position.
	 *	@return	marker index in vector at which it was inserted
	 */
	public static int add( List markers, Marker marker )
	{
		int i;
		for( i = 0; i < markers.size(); i++ ) {
			if( ((Marker) markers.get( i )).pos > marker.pos ) break;
		}
		markers.add( i, marker );
		return i;
	}

	/**
	 *	Gets the index for specific marker in a list.
	 *	Note that if the markers have distinct names (no duplicates), it
	 *	may be more convenient to create a list copy,
	 *	sort it using the <code>nameComparator</code>,
	 *	and looking it up using <code>Collections.binarySearch()</code>.
	 *
	 *  @param  markers		a <code>List</code> whose elements are
	 *						instanceof <code>Marker</code>.
	 *	@param	name		marker name to find
	 *	@param	startIndex	where to begin
	 *	@return				The list index of the first occurance (beginning
	 *						at <code>startIndex</code>) of a marker whose name equals
	 *						the given name.
	 *
	 *	@see	#nameComparator
	 *	@see	java.util.Collections#binarySearch( List, Object, Comparator )
	 */	
	public static int find( List markers, String name, int startIndex )
	{
		for( int i = startIndex; i < markers.size(); i++ ) {
			if( ((Marker) markers.get( i )).name.equals( name )) return i;
		}
		return -1;
	}
	
// ----------------------- internal classes -----------------------

	private static class NameComparator
	implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			if( o1 instanceof String ) {
				if( o2 instanceof String ) {
					return( ((Comparable) o1).compareTo( o2 ));
				} else if( o2 instanceof Marker ) {
					return( ((Comparable) o1).compareTo( ((Marker) o2).name ));
				} else if( o2 instanceof Region ) {
					return( ((Comparable) o1).compareTo( ((Region) o2).name ));
				}
			} else if( o1 instanceof Marker ) {
				if( o2 instanceof String ) {
					return( ((Comparable) ((Marker) o1).name).compareTo( o2 ));
				} else if( o2 instanceof Marker ) {
					return( ((Marker) o1).name.compareTo( ((Marker) o2).name ));
				} else if( o2 instanceof Region ) {
					return( ((Marker) o1).name.compareTo( ((Region) o2).name ));
				}
			} else if( o1 instanceof Region ) {
				if( o2 instanceof String ) {
					return( ((Comparable) ((Region) o1).name).compareTo( o2 ));
				} else if( o2 instanceof Marker ) {
					return( ((Region) o1).name.compareTo( ((Marker) o2).name ));
				} else if( o2 instanceof Region ) {
					return( ((Region) o1).name.compareTo( ((Region) o2).name ));
				}
			}
			throw new ClassCastException();
		}
				   
		public boolean equals( Object o )
		{
			return( (o != null) && (o instanceof NameComparator) );
		}
	}
} // class Marker