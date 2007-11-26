/*
 *  NumberSpace.java
 *  de.sciss.util package
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
 *		25-Jan-05	created from de.sciss.meloncillo.math.NumberSpace
 *		17-Sep-05	modified to work with new NumberField (fracDigits added, inc dropped)
 */

package de.sciss.util;

/**
 *  A number space
 *  describes a field of possible
 *  numeric values with a minimum
 *  and maximum (which can be infinity),
 *  a central value (usually zero)
 *  a quantization size which can be
 *  used to describe integers or to
 *  limit the numeric resolution.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.25, 17-Sep-05
 */
public class NumberSpace
{
	/**
	 *  Minimum allowed value
	 *  or Double.NEGATIVE_INFINITY
	 */
	public final double min;
	/**
	 *  Maximum allowed value
	 *  or Double.POSITIVE_INFINITY
	 */
	public final double max;
	/**
	 *  Quantization of values
	 *  or zero
	 */
	public final double quant;
	/**
	 *  Reset value, i.e.
	 *  a kind of default value.
	 */
	public final double reset;
	/**
	 */
	public final int minFracDigits;
	/**
	 */
	public final int maxFracDigits;
	
	/**
	 *  Ready-made NumberField for
	 *  double values, without boundaries.
	 */
	public static NumberSpace   genericDoubleSpace  = new NumberSpace(
		Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0 );
	/**
	 *  Ready-made NumberField for
	 *  integer values, without boundaries.
	 */
	public static NumberSpace   genericIntSpace  = new NumberSpace(
		Integer.MIN_VALUE, Integer.MAX_VALUE, 1.0 );

	private final boolean isInteger;

	/**
	 *  Creates a new <code>NumberSpace</code>
	 *  with the given values.
	 *
	 *  @param  min		minimum allowed value or a special value like Double.NEGATIVE_INFINITY
	 *					or Integer.MIN_VALUE
	 *  @param  max		maximum allowed value or a special value like Float.POSITIVE_INFINITY
	 *					or Long.MAX_VALUE
	 *  @param  quant   coarsity for each allowed value. E.g. if quant is 0.1, then a
	 *					value of 0.12 becomes 0.1 if you call fitValue. If quant is 0.0,
	 *					no quantization is used. If quant is integer, the number space is
	 *					marked integer and calling isInteger returns true.
	 *  @param  reset   central value for initializations of unknown values. Usually zero.
	 */
	public NumberSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits, double reset )
	{
		this.min			= min;
		this.max			= max;
		this.quant			= quant;
		this.reset			= reset;
		this.minFracDigits	= minFracDigits;
		this.maxFracDigits	= maxFracDigits;
		
		isInteger			= quant > 0.0 && (quant % 1.0) == 0.0;
	}

	/**
	 *  Creates a new NumberSpace
	 *  with the given values. Reset is
	 *  zero or on of min/max, 
	 *  increment is max( 1, quant )
	 */
	public NumberSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits )
	{
		this( min, max, quant, minFracDigits, maxFracDigits, NumberSpace.fitValue( 0.0, min, max, quant ));
	}

	/**
	 *  Creates a new NumberSpace
	 *  with the given values. Reset is
	 *  zero or on of min/max, 
	 *  increment is max( 1, quant )
	 */
	public NumberSpace( double min, double max, double quant )
	{
		this( min, max, quant, Math.max( 1, NumberSpace.fracDigitsFromQuant( quant )),
							   Math.min( 2, NumberSpace.fracDigitsFromQuant( quant )));
	}
	
	public static int fracDigitsFromQuant( double quant )
	{
		if( quant > 0.0 ) {
			int maxFracDigits = 0;
			while( (quant % 1.0) != 0.0 ) {
				maxFracDigits++;
				quant *= 10;
			}
			return maxFracDigits;
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	/**
	 *  States if the NumberSpace's quant
	 *  is integer (and not zero).
	 *
	 *  @return true if the quantization is integer and
	 *			hence all valid values are integers
	 */
	public boolean isInteger()
	{
		return isInteger;
	}
	
	/**
	 *  Utility method for creating a generic integer space
	 *  for a given minimum and maximum value. Quant will
	 *  be set to 1.0.
	 */
	public static NumberSpace createIntSpace( int min, int max )
	{
		return new NumberSpace( min, max, 1.0 );
	}
	
	/**
	 *  Validates a value for this number space. First,
	 *  it is quantized (rounded) if necessary. Then it
	 *  is limited to the minimum and maximum value.
	 *
	 *  @param  value   a value to validate
	 *  @return the input value possibly quantisized and limited
	 *			the space's bounds.
	 */
	public double fitValue( double value )
	{
		if( quant > 0.0 ) {
			value = Math.round( value / quant ) * quant;
		}
		return Math.min( max, Math.max( min, value ));
	}

	/**
	 *  Validates a value for an ad-hoc number space. First,
	 *  it is quantized (rounded) if necessary. Then it
	 *  is limited to the minimum and maximum value.
	 *
	 *  @param  value   a value to validate
	 *  @param  min		the minimum limitation
	 *  @param  max		the maximum limitation
	 *  @param  quant	the quantization to apply
	 *  @return the input value possibly quantisized and limited
	 *			the described space's bounds.
	 */
	public static double fitValue( double value, double min, double max, double quant )
	{
		if( quant > 0.0 ) {
			value = Math.round( value / quant ) * quant;
		}
		return Math.min( max, Math.max( min, value ));
	}
}