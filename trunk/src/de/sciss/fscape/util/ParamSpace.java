/*
 *  ParamSpace.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.util.NumberSpace;

/**
 *	deprecated		need a way to switch to de.sciss.util.ParamSpace
 */
public class ParamSpace
//implements Cloneable
extends NumberSpace
{
// -------- public Variablen --------

//	/**
//	 *	alle folgenden READ ONLY!
//	 */
//	public	double	min;
//	public	double	max;
	/**
	 *	Resolution: Rasterung der Werte
	 */
	public	double	inc;
	public	int		unit;

// -------- public Methoden --------

	public ParamSpace()
	{
//		super( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0 );
		super( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1 );
	
//		min		= Double.NEGATIVE_INFINITY;
//		max		= Double.POSITIVE_INFINITY;
		inc		= 0.1;
		unit	= Param.NONE;
	}

	/**
	 *	@param	unit	siehe Param
	 */
	public ParamSpace( double min, double max, double inc, int unit )
	{
//		super( min, max, 0.0 );
		super( min, max, inc );

//		this.min	= min;
//		this.max	= max;
		this.inc	= inc;
		this.unit	= unit;
	}

	/**
	 *	Kopiert ParamSpace
	 */
	public ParamSpace( ParamSpace src )
	{
		super( src.min, src.max, src.quant );

//		this.min	= src.min;
//		this.max	= src.max;
		this.inc	= src.inc;
		this.unit	= src.unit;
	}

//	public Object clone()
//	{
//		return new ParamSpace( this );
//	}

	/**
	 *	@return	true, wenn der Wert innerhalb der min/max Grenze liegt
	 */
	public boolean contains( double val )
	{
		return( (val + Constants.suckyDoubleError >= min) &&
				(val - Constants.suckyDoubleError <= max) );
	}
	
	/**
	 *	@return	true, wenn der ParamSpace other ohne Umrechnung in das
	 *			ParamSpace-Objekt ueberfuehrt werden kann
	 */
	public boolean contains( ParamSpace other )
	{
		return( (this.unit == other.unit) &&
				(this.min  <= other.min)  &&
				(this.max  >= other.max)  &&
				((other.inc / this.inc - Math.floor( other.inc /		// Vielfaches
					this.inc )) < Constants.suckyDoubleError) );
	}
	
	/**
	 *	Falls der uebergebene Wert zu gross oder zu klein ist,
	 *	wird er entsprechend "angepasst"
	 *	ausserdem wird er entsprechend der Aufloesung gerastert (gerundet)
	 */
	public double fitValue( double val )
	{
		val	= Math.round( val / inc ) * inc;
		val	= Math.max( min, Math.min( max, val ));
		return val;
	}

// -------- StringComm Methoden --------

	public String toString()
	{
		return( "" + min + ',' + max + ',' + inc + ',' + unit );
	}

	/**
	 *	@param	s	MUST BE in the format as returned by Param.toString()
	 */
	public static ParamSpace valueOf( String s )
	{
		int i = s.indexOf( ',' );
		int j = s.indexOf( ',', i+1 );
		int k = s.indexOf( ',', j+1 );
		
		return new ParamSpace( Double.valueOf( s.substring(   0,i )).doubleValue(),		// min
							   Double.valueOf( s.substring( i+1,j )).doubleValue(),		// max
							   Double.valueOf( s.substring( j+1,k )).doubleValue(),		// res
							   Integer.parseInt( s.substring( k+1 )));					// unit
	}
}
// class ParamSpace
