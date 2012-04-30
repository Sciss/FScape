/*
 *  DoublePoint.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.fscape.util;

public class DoublePoint
implements Cloneable
{
// -------- public Variablen --------
	public	double	x;
	public	double	y;

// -------- public Methoden --------

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

// -------- StringComm Methoden --------

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
