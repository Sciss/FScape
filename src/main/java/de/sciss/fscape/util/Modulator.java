/*
 *  Modulator.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

import de.sciss.fscape.spect.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class Modulator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected	Curve		curve[];		// atk/rls/sus in ABS_UNIT transformiert!

	protected	double		atkStart		= 0.0;	// [ms]
	protected	double		atkEnd			= 0.0;
	protected	double		susStart		= 0.0;
	protected	double		susPeriod		= 0.0;
	protected	double		rlsStart		= 0.0;
	protected	double		rlsEnd			= 0.0;

	protected	double		streamLength	= 0.0;
	protected	double		frameLength		= 0.0;

	protected	Param		base;
	protected	Param		depth;
	protected	Envelope	env;
	protected	SpectStream	stream;

// -------- public Methoden --------

	/**
	 *	@param	base		in ABS_UNIT
	 *	@param	depth		Ausmass (Envelope-Maximum) der Modulation von base
	 *	@param	env			Verlauf der Modulation (-100% ... +100%), d.h.
	 *						vSpace.unit sollte Param.FACTOR sein!
	 *	@param	refStream	Referenz, die z.B. fuer BPM-Geschwindigkeit benoetigt wird
	 *						; hieran wird auch der Zeitoffset gemessen!
	 */
	public Modulator( Param base, Param depth, Envelope env, SpectStream refStream )
	{
		ParamSpace	hSpace;
		Param		min, max;
		Curve		c		= null;
		Param		hRef;
	
		this.base	= base;
		this.depth	= depth;
		this.env	= env;
		this.stream	= refStream;
		
		// Stream Laenge
		if( stream != null ) {

			frameLength	= SpectStream.framesToMillis( stream, 1 );
			streamLength= SpectStream.framesToMillis( stream, stream.frames );
		}

		// Kurve in ABS_UNIT transformieren
		curve = new Curve[ 3 ];
		
		rlsStart	= streamLength;
		rlsEnd		= streamLength;

		// atk+rls benutzen streamLength als Referenz
		hRef		= new Param( streamLength, env.hUnit );

		for( int i = 0; i < 3; i++ ) {
			switch( i ) {
			case 0:
				c	= env.atkState ? env.atkCurve : null;
				break;
			case 1:
				c	= env.rlsState ? env.rlsCurve : null;
				break;
			case 2:
				c	= env.susState ? env.susCurve : null;
				hRef = new Param( rlsStart - atkEnd, env.hUnit );	// 100% sus = stream-atk-rls
				break;
			default:
				break;
			}
			
			if( c == null) continue;
		
			min		= Param.transform( new Param( c.hSpace.min, c.hSpace.unit ), env.hUnit,
									   hRef, stream );
			max		= Param.transform( new Param( c.hSpace.max, c.hSpace.unit ), env.hUnit,
									   hRef, stream );
			if( (min != null) && (max != null) ) {
			
				hSpace		= new ParamSpace( min.val, max.val, 0.0001, env.hUnit );
				curve[ i ]	= Curve.transform( c, hSpace, c.vSpace, hRef, null, stream );
				
				switch( i ) {
				case 0:
					atkStart	= hSpace.min;
					atkEnd		= hSpace.max;
					break;
				case 1:
					rlsStart   -= (hSpace.max - hSpace.min);
					break;
				case 2:
					susStart	= hSpace.min + atkEnd;
					susPeriod	= hSpace.max - hSpace.min;
					break;
				}
			}
		}
	}

	public Param calc()
	{
		return calc( stream.getTime(), frameLength );
	}

	public Param calc( double x )
	{
		return calc( x, frameLength );
	}
	
	public Param calc( double x, double width )
	{
		Curve	c			= null;
		double	modDepth	= 0.0;
		Param	mod;

		if( x >= atkEnd ) {
			if( x >= rlsStart ) {
				c = curve[ 1 ];
				x -= rlsStart;
			} else {
				c = curve[ 2 ];
				x = (x - susStart) % susPeriod;
			}
		} else {
			c = curve[ 0 ];
			x -= atkStart;
		}

		if( c != null ) {
		
			// dub it. rub it. right on!!!
			modDepth	= Curve.average( c, x, x + width ) / 100;	// % ==> 0...1
			mod			= Param.transform( new Param( depth.val * modDepth, depth.unit ), env.hUnit,
										   base, stream );
			if( mod != null ) {
				return mod;
			}			
		}

		return base;	// no modulation
	}
}
// class Modulator
