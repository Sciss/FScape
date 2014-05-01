/*
 *  Param.java
 *  FScape
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.fscape.spect.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class Param
implements Cloneable
{
// -------- Unit-Konstanten --------

	/**
	 *	Groessen (Dimensions)
	 */
	public static final int NONE		=	0x0000;
	public static final int AMP			=	0x0001;		// has no units
	public static final int TIME		=	0x0002;		// default: ms
	public static final int FREQ		=	0x0003;		// default: Hz
	public static final int PHASE		=	0x0004;		// default: degrees

	public static final int DIM_MASK	=	0x000F;

	/**
	 *	Darstellung (Form)
	 */
	public static final int ABSUNIT		=	0x0000;		// ms, Hz, ...
	public static final int ABSPERCENT	=	0x0010;		// %
	public static final int RELUNIT		=	0x0020;		// +/- ms, +/- Hz, ...
	public static final int RELPERCENT	=	0x0030;		// +/- %

	public static final int FORM_MASK	=	0x00F0;
	
	/**
	 *	Spezialdarstellungen
	 */
	public static final int BEATS		=	0x0100;
	public static final int SEMITONES	=	0x0200;
	public static final int DECIBEL		=	0x0300;

	public static final int SPECIAL_MASK=	0x0F00;

	/**
	 *	Gaengige Einheiten
	 */
	public static final int FACTOR			=	NONE | ABSPERCENT;
	public static final int ABS_AMP			=	AMP  | ABSUNIT;
	public static final int FACTOR_AMP		=	AMP  | ABSPERCENT;
	public static final int DECIBEL_AMP		=	AMP  | ABSPERCENT	| DECIBEL;
	public static final int OFFSET_AMP		=	AMP  | RELPERCENT;
	public static final int ABS_MS			=	TIME | ABSUNIT;
	public static final int ABS_BEATS		=	TIME | ABSUNIT		| BEATS;
	public static final int FACTOR_TIME		=	TIME | ABSPERCENT;
	public static final int OFFSET_MS		=	TIME | RELUNIT;
	public static final int OFFSET_BEATS	=	TIME | RELUNIT		| BEATS;
	public static final int OFFSET_TIME		=	TIME | RELPERCENT;
	public static final int ABS_HZ			=	FREQ | ABSUNIT;
	public static final int FACTOR_FREQ		=	FREQ | ABSPERCENT;
	public static final int OFFSET_HZ		=	FREQ | RELUNIT;
	public static final int OFFSET_SEMITONES=	FREQ | RELUNIT		| SEMITONES;
	public static final int OFFSET_FREQ		=	FREQ | RELPERCENT;

// -------- public Variablen --------

	public	double	val;
	public	int		unit;

// -------- private Variablen --------

	private final static int chain[]	= { 1, 0, 2, 3 };	// mapping; vgl. transform()
	private static double	freqScale;
	private static double	timeScale;

	static {
		updateScales();
	}

// -------- public Methoden --------

	public Param()
	{
		val		= 0.0;
		unit	= NONE;
	}

	/**
	 *	@param	val		Wert
	 *	@param	unit	Groesse/Masseinheit
	 */
	public Param( double val, int unit )
	{
		this.val	= val;
		this.unit	= unit;
	}

	public Param( Param src )
	{
		this.val	= src.val;
		this.unit	= src.unit;
	}
	
	public Object clone()
	{
		return new Param( this );
	}

	/**
	 *	Veraenderung der Scalen mitteilen
	 */
	public synchronized static void updateScales()
	{
//		String val;
//	
//		val = AbstractApplication.getApplication().getUserPrefs().get( MainPrefs.KEY_FREQSCALE, null );
//		if( val != null ) {
//			freqScale	= Param.valueOf( val ).val;
//		} else {
			freqScale	= 12.0;
//		}
//		val = AbstractApplication.getApplication().getUserPrefs().get( MainPrefs.KEY_TIMESCALE, null );
//		if( val != null ) {
//			timeScale	= Param.valueOf( val ).val;
//		} else {
			timeScale	= 120.0;
//		}
	}

	/**
	 *	Transformiert einen Parameter von einer Groesse/Masseinheit (eindimensional)
	 *	in eine andere (eindimensional)
	 *
	 *	- bei gleichen Einheiten (ohne Umrechnung) wird das Original zurueckgeliefert!
	 *	- ein ParamSpace.fitValue() wird nicht durchgefuehrt!
	 *
	 *	@param	reference	optionaler Referrer, der ggf. fuer Umrechnungen von/in
	 *						relative Werte benoetigt wird; kann null sein;
	 *						WENN ER BENOETIGT WIRD, MUSS ER ENTWEDER IN DER FORM
	 *						ABS_UNIT VORLIEGEN ODER (OHNE REFERENZ) IN DIESE UMWANDELBAR SEIN!
	 *
	 *						reference wird synchronisiert!
	 *
	 *	@param	stream		optionaler Stream-Referrer, der ggf. fuer Umrechnungen
	 *						von/in Beats oder Semitones benoetigt wird; darf null sein
	 *						(ggf. wird dann auf die globale Geschwindigkeit/Scala
	 *						zurueckgegriffen)
	 *
	 *	@return	null, wenn wg. fehlendem Referrer die Transformation nicht durchgefuehrt
	 *			werden kann oder bei Inkompatiblitaet (Zeit kann nicht in Frequenzen
	 *			umgerechnet werden etc.)
	 */
	public static Param transform( Param src, int destUnit, Param reference, SpectStream stream )
	{
		if( src.unit == destUnit ) return src;

		double	destVal = src.val;
		int		srcChain, destChain;

		// -------- Sonderformen rausrechnen --------			
		switch( src.unit & SPECIAL_MASK ) {
		case BEATS:
			destVal *= 60000.0 / timeScale;
			break;
			
		case SEMITONES:
			if( reference == null ) return null;
			destVal = reference.val * (Math.pow( 2, destVal / freqScale ) - 1);
			break;
			
		case DECIBEL:
			destVal = Math.exp( destVal / 20 * Constants.ln10 ) * 100;
			break;
		
		default:
			break;
		}

		// Kette: Abs% <==> AbsUnit <==> RelUnit <==> Rel%
		srcChain	= chain[ (src.unit & FORM_MASK) >> 4 ];
		destChain	= chain[ (destUnit & FORM_MASK) >> 4 ];
		
		if( srcChain != destChain ) {

			if( reference == null ) return null;	// we need reference here
													// und zwar in der Form: Absolute Masseinheit:
			reference = Param.transform( reference,
										 (reference.unit & ~(FORM_MASK | SPECIAL_MASK)) | ABSUNIT,
										 null, stream );

			if( reference == null ) return null;		// Umwandlung klappte nicht

			try {
				synchronized( reference ) {
		
					if( srcChain < destChain ) {	// -------- Kette von links nach rechts --------
				
						for( int i = srcChain; i < destChain; i++ ) {
					
							switch( i ) {
							case 0:
								destVal	*= reference.val / 100;	// Abs% ==> AbsUnit
								break;
							case 1:
								destVal -= reference.val;		// AbsUnit ==> RelUnit
								break;
							case 2:
								destVal *= 100 / reference.val;	// RelUnit ==> Rel%
								break;
							default:
								break;
							}
						}
				
					} else {						// -------- Kette von rechts nach links --------
				
						for( int i = srcChain; i > destChain; i-- ) {
					
							switch( i ) {
							case 3:
								destVal	*= reference.val / 100;	// Rel% ==> RelUnit
								break;
							case 2:
								destVal += reference.val;		// RelUnit ==> AbsUnit
								break;
							case 1:
								destVal *= 100 / reference.val;	// AbsUnit ==> Abs%
								break;
							default:
								break;
							}
						}
					}
				}
			}
			catch( ArithmeticException e ) {	// division by zero
				return null;
			}
		}
		
		// -------- Sonderformen reinrechnen --------
		switch( destUnit & SPECIAL_MASK ) {
		case BEATS:
			destVal *= timeScale / 60000;
			break;
			
		case SEMITONES:
			if( reference == null ) return null;
			destVal = Math.log( 1 + destVal / reference.val ) / Constants.ln2 * freqScale;
			break;
			
		case DECIBEL:
			if( destVal > Constants.minPercent ) {
				destVal = Math.log( destVal / 100 ) * 20 / Constants.ln10;
			} else {	// zu kleiner Wert; minus unendlich dB sei -144 dB
				destVal = Constants.minDecibel;
			}
			break;
		
		default:
			break;
		}
		
		return new Param( destVal, destUnit );
	}

	/**
	 *	Ermittelt, wie "gut" eine Einheiten-Umwandlung waere,
	 *	das Ergebnis kann mit anderen verglichen werden; je hoeher
	 *	der Wert, desto eher ist die entsprechende Umwandlung
	 *	anderen vorzuziehen
	 */
	public static int getPriority( int srcUnit, int destUnit )
	{
		if( srcUnit == destUnit ) return 99;								// best one
		if( (srcUnit & DIM_MASK) != (destUnit & DIM_MASK) ) return -99;		// worst

		int srcChain, destChain;
		
		srcChain	= chain[ (srcUnit & FORM_MASK) >> 4 ];
		destChain	= chain[ (destUnit & FORM_MASK) >> 4 ];

		if( srcChain <= destChain ) {
			return( destChain - srcChain );
		} else {
			return( srcChain - destChain );
		}
	}

// -------- StringComm Methoden --------

	public String toString()
	{
		return( "" + val + ',' + unit );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by Param.toString()
	 */
	public static Param valueOf( String s )
	{
		int i = s.indexOf( ',' );
		
		return new Param( Double.valueOf( s.substring( 0, i )).doubleValue(),	// val
						  Integer.parseInt( s.substring( i + 1 )));				// unit
	}
}
// class Param
