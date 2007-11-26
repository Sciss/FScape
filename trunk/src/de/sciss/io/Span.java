/*
 *  Span.java
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
 *		21-May-05	created from de.sciss.eisenkraut.util.Span
 *		28-Jul-05	overlaps, touches
 *		04-Aug-05	fixed missing hashCode() override
 *		12-Aug-05	truely immutable ; equals() allows null objects
 *		15-Aug-05	implements Serializable, Cloneable
 *		25-Aug-05	has comparator fields for start and stop
 *		08-Jan-05	added contains( Span )
 *		01-Ma-06	added replaceStart(), replaceStop()
 */

package de.sciss.io;

import java.io.Serializable;
import java.util.Comparator;

/**
 *  A struct class: a span between a start
 *  and end point in one dimensional
 *  space. The start point is
 *  considered inclusive while
 *  the end point is considered
 *  exclusive. In Melloncillo, it is
 *  mainly used to describe a time span
 *  in sense rate frames.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.28, 12-Aug-07
 */
public class Span
implements Serializable, Cloneable
{
	public static final Comparator	startComparator	= new StartComparator();
	public static final Comparator	stopComparator	= new StopComparator();

	/**
	 *  The span <code>start</code> should be treated
	 *  as if it was immutable!
	 */
	public final long start;
	/**
	 *  The span <code>start</code> should be treated
	 *  as if it was immutable!
	 */
	public final long stop;

	/**
	 *  Create a new empty span
	 *  whose start and stop are zero.
	 */
	public Span()
	{
		start	= 0;
		stop	= 0;
	}

	/**
	 *  Creates a span with the given
	 *  start and stop points.
	 *  The caller has to ensure that
	 *  start <= stop (this is not checked)
	 *
	 *  @param  start   beginning of the span
	 *  @param  stop	end of the span
	 */
	public Span( long start, long stop )
	{
		this.start	= start;
		this.stop	= stop;
	}

	/**
	 *  Create a span with the
	 *  start and stop points copied
	 *  from another span.
	 *
	 *  @param  span	template span whose start and end are copied
	 */
	public Span( Span span )
	{
		this.start	= span.start;
		this.stop	= span.stop;
	}

	/**
	 *	Returns a new span which is
	 *	equal to this one. <code>CloneNotSupportedException</code>
	 *	is never thrown.
	 *
	 *	@return		a new span with the same start and stop
	 *				as this span
	 */
	public Object clone()
	throws CloneNotSupportedException
	{
		return super.clone();	// field by field copy
	}
	    
	/**
	 *  Checks if a position lies within the span.
	 *
	 *  @return		<code>true</code>, if <code>start <= postion < stop</code>
	 */
    public boolean contains( long position )
    {
        return( position >= start && position < stop );
    }

	/**
	 *  Checks if another span lies within the span.
	 *
	 *	@param	anotherSpan	second span, may be <code>null</code> (in this case returns <code>false</code>)
	 *  @return		<code>true</code>, if <code>anotherSpan.start >= this.span &&
	 *				anotherSpan.stop <= this.stop</code>
	 */
    public boolean contains( Span anotherSpan )
    {
        return( (anotherSpan != null) &&
			(anotherSpan.start >= this.start) && (anotherSpan.stop <= this.stop) );
    }

	/**
	 *  Checks if a two spans overlap each other.
	 *
	 *	@param	anotherSpan	second span, may be <code>null</code> (in this case returns <code>false</code>)
	 *  @return		<code>true</code>, if the spans
	 *				overlap each other
	 */
    public boolean overlaps( Span anotherSpan )
    {
		if( anotherSpan == null ) return false;
		
		if( this.start <= anotherSpan.start ) {
			return( this.stop > anotherSpan.start );
		} else {
			return( anotherSpan.stop > this.start );
		}
    }

	/**
	 *  Checks if a two spans overlap or touch each other.
	 *
	 *	@param	anotherSpan	second span, may be <code>null</code> (in this case returns <code>false</code>)
	 *  @return		<code>true</code>, if the spans
	 *				overlap each other
	 */
    public boolean touches( Span anotherSpan )
    {
		if( anotherSpan == null ) return false;

		if( this.start <= anotherSpan.start ) {
			return( this.stop >= anotherSpan.start );
		} else {
			return( anotherSpan.stop >= this.start );
		}
    }

	/**
	 *  Checks if the span is empty.
	 *
	 *  @return		<code>true</code>, if <code>start == stop</code>
	 */
    public boolean isEmpty()
    {
        return( start == stop );
    }
    
	/**
	 *  Checks if this span is equal to an object.
	 *
	 *  @param  o   an object to compare to this span
	 *  @return		<code>true</code>, if <code>o</code> is a span with
	 *				the same start and end point
	 */
    public boolean equals( Object o )
    {
        return( (o != null) && (o instanceof Span) &&
				(((Span) o).start == this.start) && (((Span) o).stop == this.stop) );
    }

	public int hashCode()
	{
		return( (int) start ^ (-(int) stop) );
	}
    
	/**
	 *  Queries the span's start.
	 *
	 *  @return		the start point of the span
	 */
    public long getStart()
    {
        return start;
    }
    
	/**
	 *  Queries the span's end.
	 *
	 *  @return		the end point of the span
	 */
    public long getStop()
    {
        return stop;
    }
    
	/**
	 *  Queries the span's extent (duration, length etc.)
	 *
	 *  @return		length of the span, i.e. <code>stop - start</code>
	 */
    public long getLength()
    {
        return( stop - start );
    }
	
	public String toString()
	{
		return( String.valueOf( start ) + " ... " + String.valueOf( stop ));
	}
	
	/**
	 *  Union operation on two spans.
	 *
	 *  @param  span1   first span to fuse (may be <code>null</code>)
	 *  @param  span2   second span to fuse (may be <code>null</code>)
	 *  @return		a new span whose extension
	 *				covers both span1 and span2, or <code>null</code> if
	 *				both <code>span1</code> and <code>span2</code> are <code>null</code>
	 */
	public static Span union( Span span1, Span span2 )
	{
		if( span1 == null ) return span2;
	
		return span1.union( span2 );
	}
	
	public Span union( Span anotherSpan )
	{
		if( anotherSpan == null ) return this;
	
		return new Span( Math.min( this.start, anotherSpan.start ),
						 Math.max( this.stop, anotherSpan.stop ));
	}
	
	public static Span intersection( Span span1, Span span2 )
	{
		if( span1 == null ) return null;
		
		return span1.intersection( span2 );
	}
	
	public Span intersection( Span anotherSpan )
	{
		if( anotherSpan == null ) return null;
		
		final long start = Math.max( this.start, anotherSpan.start );
		final long stop	 = Math.min( this.stop, anotherSpan.stop );
//		if( stop < start ) return null;
		return new Span( start, stop );
	}

	public Span replaceStart( long newStart )
	{
		return new Span( newStart, stop );
	}

	public Span replaceStop( long newStop )
	{
		return new Span( start, newStop );
	}

	public Span shift( long delta )
	{
		return new Span( start + delta, stop + delta );
	}

// ---------------- internal classes ----------------

	private static class StartComparator
	implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			final long n1, n2;
		
			if( o1 instanceof Span ) {
				if( o2 instanceof Span ) {
					n1 = ((Span) o1).start;
					n2 = ((Span) o2).start;
				} else if( o2 instanceof Number ) {
					n1 = ((Span) o1).start;
					n2 = ((Number) o2).longValue();
				} else throw new ClassCastException();
			} else if( o1 instanceof Number ) {
				if( o2 instanceof Span ) {
					n1 = ((Number) o1).longValue();
					n2 = ((Span) o2).start;
				} else if( o2 instanceof Number ) {
					n1 = ((Number) o1).longValue();
					n2 = ((Number) o2).longValue();
				} else throw new ClassCastException();
			} else throw new ClassCastException();
			
			if( n1 < n2 ) return -1;
			if( n1 > n2 ) return 1;
			return 0;
		}
				   
		public boolean equals( Object o )
		{
			return( (o != null) && (o instanceof StartComparator) );
		}
	}

	private static class StopComparator
	implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{
			final long n1, n2;
		
			if( o1 instanceof Span ) {
				if( o2 instanceof Span ) {
					n1 = ((Span) o1).stop;
					n2 = ((Span) o2).stop;
				} else if( o2 instanceof Number ) {
					n1 = ((Span) o1).stop;
					n2 = ((Number) o2).longValue();
				} else throw new ClassCastException();
			} else if( o1 instanceof Number ) {
				if( o2 instanceof Span ) {
					n1 = ((Number) o1).longValue();
					n2 = ((Span) o2).stop;
				} else if( o2 instanceof Number ) {
					n1 = ((Number) o1).longValue();
					n2 = ((Number) o2).longValue();
				} else throw new ClassCastException();
			} else throw new ClassCastException();
			
			if( n1 < n2 ) return -1;
			if( n1 > n2 ) return 1;
			return 0;
		}
				   
		public boolean equals( Object o )
		{
			return( (o != null) && (o instanceof StopComparator) );
		}
	}
}