//
//  ParamSpace.java
//  NumberField
//
//  Created by Hanns Holger Rutz on 16.09.05.
//  Copyright 2005 __MyCompanyName__. All rights reserved.
//

package de.sciss.util;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 *  @version	0.28, 10-Mar-06
 */
public class ParamSpace
extends NumberSpace
{
	/**
	 *	Dimensions
	 */
	public static final int NONE			=	0x00000;
	public static final int AMP				=	0x00001;		// i.e. "voltage"
	public static final int TIME			=	0x00002;
	public static final int FREQ			=	0x00003;
	public static final int PHASE			=	0x00004;
	public static final int SPACE			=	0x00005;
	
	private static final int DIM_SHIFT		=	0;
	public static final int DIM_MASK		=	0x0000F;

	private static final String[] DIM_NAMES	= { "none", "amp", "time", "freq", "phase", "space" };

	/**
	 *	Units
	 */
	public static final int VOLTS			=	0x00010;
	public static final int SECS			=	0x00020;		// seconds
	public static final int SMPS			=	0x00030;		// samples
	public static final int BEATS			=	0x00040;
	public static final int HERTZ			=	0x00050;
	public static final int PITCH			=	0x00060;		// MIDI
	public static final int DEGREES			=	0x00070;
	public static final int METERS			=	0x00080;
	public static final int PIXELS			=	0x00090;

	private static final int UNIT_SHIFT		=	4;
	public static final int UNIT_MASK		=	0x000F0;

	private static final String[] UNIT_NAMES	= { "none", "volts", "secs", "smps", "beats", "hertz",
													"pitch", "degrees", "meters", "pixels" };

	/**
	 *	Dependencies
	 */
	public static final int ABS				=	0x00000;
	public static final int REL				=	0x00100;
	public static final int OFF				=	0x00200;

	private static final int REL_SHIFT		=	8;
	public static final int REL_MASK		=	0x00F00;

	private static final String[] REL_NAMES	= { "abs", "rel", "off" };

	/**
	 *	Scalings
	 */
	public static final int PERCENT			=	0x01000;
	public static final int DECIBEL			=	0x02000;
	public static final int MILLI			=	0x03000;
	public static final int CENTI			=	0x04000;
	public static final int KILO			=	0x05000;

	private static final int SCALE_SHIFT	=	12;
	public static final int SCALE_MASK		=	0x0F000;

	private static final String[] SCALE_NAMES = { "none", "percent", "decibel", "milli", "centi", "kilo" };

	public static final int CRUCIAL_MASK	=	0x0FFFF;

	/**
	 *	Special attributes
	 */
	public static final int BARSBEATS		=	0x10000;		// display as bars |Â beats
	public static final int HHMMSS			=	0x20000;		// display as HH:MM:SS.xxx
	public static final int MIDINOTE		=	0x30000;		// display as C#4 etc.

	private static final int SPECIAL_SHIFT	=	16;
	public static final int SPECIAL_MASK	=	0xF0000;

	private static final String[] SPECIAL_NAMES = { "none", "barsbeats", "hhmmss", "midinote" };

	public static final int[] DEFAULT_UNIT	= { NONE, VOLTS, SECS, HERTZ, DEGREES, METERS };

	public final double		inc;
	public final Object		warp;
	public final int		unit;
	
	// utility default spaces
	public static final ParamSpace	spcTimeHHMMSS	= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 0.0, 1, 3, 0.0,
															ParamSpace.TIME | ParamSpace.SECS | ParamSpace.HHMMSS );
	public static final ParamSpace	spcTimeSmps		= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 1.0, 0, 0, 0.0,
															ParamSpace.TIME | ParamSpace.SMPS );
	public static final ParamSpace	spcTimeSmpsD	= new ParamSpace( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
															1.0, 0, 0, 0.0, ParamSpace.TIME | ParamSpace.SMPS | ParamSpace.OFF );
	public static final ParamSpace	spcTimeMillis	= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 0.0, 0, 2, 0.0,
															ParamSpace.TIME | ParamSpace.SECS | ParamSpace.MILLI );
	public static final ParamSpace	spcTimeMillisD	= new ParamSpace( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0, 2, 0.0,
															ParamSpace.TIME | ParamSpace.SECS | ParamSpace.MILLI | ParamSpace.OFF );
	public static final ParamSpace	spcTimePercentF	= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 0.0, 0, 3, 100.0,
															ParamSpace.TIME | ParamSpace.REL | ParamSpace.PERCENT, 0.1 );
	public static final ParamSpace	spcTimePercentR	= new ParamSpace( 0.0, 100.0, 0.0, 0, 3, 100.0,
															ParamSpace.TIME | ParamSpace.REL | ParamSpace.PERCENT, 0.1 );
	public static final ParamSpace	spcTimePercentD	= new ParamSpace( -100.0, Double.POSITIVE_INFINITY, 0.0, 0, 3, 0.0,
															ParamSpace.TIME | ParamSpace.OFF | ParamSpace.PERCENT, 0.1 );
	public static final ParamSpace	spcFreqHertz	= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 0.0, 0, 3, 0.0,
															ParamSpace.FREQ | ParamSpace.HERTZ );
	public static final ParamSpace	spcAmpRel		= new ParamSpace( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0, 2, 1.0,
															ParamSpace.AMP | ParamSpace.REL );
	public static final ParamSpace	spcAmpDecibels	= new ParamSpace( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0, 2, 0.0,
															ParamSpace.AMP | ParamSpace.REL | ParamSpace.DECIBEL, 0.1 );
	public static final ParamSpace	spcAmpPercentF	= new ParamSpace( 0.0, Double.POSITIVE_INFINITY, 0.0, 0, 3, 100.0,
															ParamSpace.AMP | ParamSpace.REL | ParamSpace.PERCENT, 0.1 );

	public ParamSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits,
					   double reset, int unit, double inc, Object warp )
	{
		super( min, max, quant, Math.max( minFracDigits, NumberSpace.fracDigitsFromQuant( inc )),
			   maxFracDigits, reset );
			   
		this.inc	= inc;
		this.warp	= warp;
		this.unit	= unit;
	}

	public ParamSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits,
					   double reset, int unit, double inc )
	{
		this( min, max, quant, minFracDigits, maxFracDigits, reset, unit, inc, null );
	}

	public ParamSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits,
					   double reset, int unit )
	{
		this( min, max, quant, minFracDigits, maxFracDigits, reset, unit,
			  Math.max( Math.pow( 10, -minFracDigits ), quant ));
	}

	public ParamSpace( double min, double max, double quant, int minFracDigits, int maxFracDigits,
					   double reset )
	{
		this( min, max, quant, minFracDigits, maxFracDigits, reset, NONE );
	}
	
	public ParamSpace reshape( double newMin, double newMax, double newReset )
	{
		return new ParamSpace( newMin, newMax, this.quant, this.minFracDigits, this.maxFracDigits,
							   newReset, this.unit, this.inc );
	}

	public interface Translator
	{
		public Param translate( Param oldParam, ParamSpace newSpace );
		public void setCoefficient( int sourceUnit, int targetUnit, double coeff );
	}
	
	/**
	 *	@throws	IllegalArgumentException	if the unit contains unknown flags
	 */
	public static String unitToString( int unit )
	{
		final StringBuffer	result	= new StringBuffer();
		int					i, j;
		
		try {
			i = (unit & DIM_MASK) >> DIM_SHIFT;
			result.append( DIM_NAMES[ i ]);
			
			i = (unit & UNIT_MASK) >> UNIT_SHIFT;
			result.append( '_' );
			result.append( UNIT_NAMES[ i ]);

			i = (unit & REL_MASK) >> REL_SHIFT;
			result.append( '_' );
			result.append( REL_NAMES[ i ]);

			i = (unit & SCALE_MASK) >> SCALE_SHIFT;
			j = (unit & SPECIAL_MASK) >> SPECIAL_SHIFT;
			
			if( (i > 0) || (j > 0) ) {
				result.append( '_' );
				result.append( SCALE_NAMES[ i ]);
				if( j > 0 ) {
					result.append( '_' );
					result.append( SPECIAL_NAMES[ j ]);
				}
			}
		
			return result.toString();
		}
		catch( IndexOutOfBoundsException e1 ) {
			throw new IllegalArgumentException( String.valueOf( unit ));
		}
	}
	
	/**
	 *	@throws	NumberFormatException	if the string does not contain a valid unit desciption
	 */
	public static int stringToUnit( String str )
	{
		final StringTokenizer	tok		= new StringTokenizer( str, "_" );
		int						result	= 0;
		String					s;
		
		try {
			s = tok.nextToken();
			for( int dim = 0; dim < DIM_NAMES.length; dim++ ) {
				if( DIM_NAMES[ dim ].equals( s )) {
					result |= dim << DIM_SHIFT;
					s		= tok.nextToken();
					for( int unit = 0; unit < UNIT_NAMES.length; unit++ ) {
						if( UNIT_NAMES[ unit ].equals( s )) {
							result |= unit << UNIT_SHIFT;
							s		= tok.nextToken();
							for( int rel = 0; rel < REL_NAMES.length; rel++ ) {
								if( REL_NAMES[ rel ].equals( s )) {
									result |= rel << REL_SHIFT;
									if( !tok.hasMoreTokens() ) return result;
									s		= tok.nextToken();
									for( int scale = 0; scale < SCALE_NAMES.length; scale++ ) {
										if( SCALE_NAMES[ scale ].equals( s )) {
											result |= scale << SCALE_SHIFT;
											if( !tok.hasMoreTokens() ) return result;
											s		= tok.nextToken();
											for( int special = 0; special < SPECIAL_NAMES.length; special++ ) {
												if( SPECIAL_NAMES[ special ].equals( s )) {
													result |= special << SPECIAL_SHIFT;
													return result;	// c'est a
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch( NoSuchElementException e1 ) {}
		throw new NumberFormatException( str );
	}
}