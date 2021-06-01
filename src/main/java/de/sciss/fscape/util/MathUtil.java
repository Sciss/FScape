/*
 *  MathUtil.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		25-Jan-05	created from de.sciss.meloncillo.math.MathUtil
 *		15-Jan-09	copied from EisK
 */

package de.sciss.fscape.util;

/**
 *  This is a helper class containing utility static functions
 *  for common math operations and constants
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 22-Apr-08
 */
public class MathUtil
{
	/**
	 *  2 * PI (Outline of the unit circle)
	 */
	public static final double PI2  = Math.PI * 2;
	/**
	 *  logarithmus naturalis of 2
	 */
	public static final double LN2  = Math.log( 2 );
	/**
	 *  logarithmus naturalis of 10
	 */
	public static final double LN10 = Math.log( 10 );

	private MathUtil() { /* empty */ }

	/**
	 *  Decibel-to-Linear conversion.
	 *
	 *  @param  dB  volume in decibels
	 *  @return volume linear, such that
	 *			dBToLinear( -6 ) returns c. 0.5
	 */
	public static double dBToLinear( double dB )
	{
		return Math.exp( dB / 20 * LN10 );
	}
		
	/**
	 *  Linear-to-Decibel conversion
	 *
	 *  @param  linear  volume linear
	 *  @return volume in decibals, such that
	 *			linearToDB( 2.0 ) returns c. +6
	 */
	public static double linearToDB( double linear )
	{
		return Math.log( linear ) * 20 / LN10;
	}

	/**
	 *  Calculate the logarithm with base 2.
	 *
	 *  @param  val	the input value
	 *  @return the log2 of the value
	 */
	public static double log2( double val )
	{
		return Math.log(  val ) / LN2;
	}

	/**
	 *  Calculates an integer that is a power
	 *	of two and is equal or greater than a given integer
	 *
	 *	@param	x	the minimum value to return
	 *	@return		an integer 2^n which is equal or greater than x
	 */
	public static int nextPowerOfTwo( int x )
	{
		int y;
		for( y = 1; y < x; y <<= 1 ) ;
		return y;
	}
}
