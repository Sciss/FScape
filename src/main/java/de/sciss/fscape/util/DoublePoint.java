/*
 *  DoublePoint.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

public class DoublePoint
implements Cloneable
{
// -------- public variables --------
	public	double	x;
	public	double	y;

// -------- public methods --------

	public DoublePoint( double x, double y )
	{
		this.x	= x;
		this.y	= y;
	}

	public DoublePoint( DoublePoint src )
	{
		this.x	= src.x;
		this.y	= src.y;
	}
	
	public DoublePoint()
	{
		this.x	= 0.0;
		this.y	= 0.0;
	}

	public Object clone()
	{
		return new DoublePoint( this );
	}

// -------- StringComm methods --------

	public String toString()
	{
		return( "" + x + ',' + y );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by Param.toString()
	 */
	public static DoublePoint valueOf( String s )
	{
		int i = s.indexOf( ',' );
		
		return new DoublePoint( Double.valueOf( s.substring( 0,i )).doubleValue(),		// x
						  		Double.valueOf( s.substring( i+1 )).doubleValue() );	// y
	}
}
// class DoublePoint
