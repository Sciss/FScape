/*
 *  Envelope.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	more useful basic envelope
 */

package de.sciss.fscape.util;

import java.util.*;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class Envelope
implements Cloneable
{
// -------- public Variablen --------

	/**
	 *	Fuer getBasicEnvelope
	 */
	public static final int	BASIC_TIME				= 0;	// Mod. -100% ... +100%
	public static final int	BASIC_UNSIGNED_TIME		= 1;	// Mod.    0%.... +100%

	public	boolean		atkState;	// true = active; false = inactive
	public	boolean		susState;
	public	boolean		rlsState;
	
	public	int			hUnit;		// ABS_UNIT!
	public	int			vUnit;

	/**
	 *	READ-ONLY!
	 */
	public	Curve		atkCurve;
	public	Curve		susCurve;
	public	Curve		rlsCurve;

// -------- private Variablen --------

// -------- public Methoden --------

	/**
	 *	Alle drei Kurventeile sind defaultmaessig aktiv
	 *	; einzelne Kurventeile duerfen null sein!
	 */
	public Envelope( Curve atkCurve, Curve susCurve, Curve rlsCurve )
	{
		this.atkCurve	= atkCurve;
		this.susCurve	= susCurve;
		this.rlsCurve	= rlsCurve;

		atkState		= atkCurve != null;
		susState		= susCurve != null;
		rlsState		= rlsCurve != null;

		if( susState ) {
			hUnit		= susCurve.hSpace.unit & Param.DIM_MASK;
			vUnit		= susCurve.vSpace.unit & Param.DIM_MASK;
		} else if( atkState ) {
			hUnit		= atkCurve.hSpace.unit & Param.DIM_MASK;
			vUnit		= atkCurve.vSpace.unit & Param.DIM_MASK;
		} else if( rlsState ) {
			hUnit		= rlsCurve.hSpace.unit & Param.DIM_MASK;
			vUnit		= rlsCurve.vSpace.unit & Param.DIM_MASK;
		} else {
			hUnit		= Param.NONE;
			vUnit		= Param.NONE;
		}
	}

	/**
	 *	Clont vorgegebene Envelope
	 */
	public Envelope( Envelope src )
	{
		this.atkCurve	= (Curve) src.atkCurve.clone();
		this.susCurve	= (Curve) src.susCurve.clone();
		this.rlsCurve	= (Curve) src.rlsCurve.clone();

		this.atkState	= src.atkState;
		this.susState	= src.susState;
		this.rlsState	= src.rlsState;

		this.hUnit		= src.hUnit;
		this.vUnit		= src.vUnit;
	}

	public Object clone()
	{
		return new Envelope( this );
	}

	/**
	 *	Standard-Envelope erzeugen
	 *
	 *	@param	type	BASIC_...
	 */
	public static Envelope createBasicEnvelope( int type )
	{
		Curve		atkCurve, susCurve, rlsCurve;
		ParamSpace	atkHSpace, susHSpace, rlsHSpace;
		ParamSpace	vSpace	= null;
		Envelope	basic;

		if( type == BASIC_TIME ) {
			vSpace		= Constants.spaces[ Constants.modSpace ];
		} else {	// BASIC_UNSIGNED_TIME
			vSpace		= Constants.spaces[ Constants.unsignedModSpace ];
		}
		
		atkHSpace		= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		atkHSpace.max  *= 0.1;
		atkHSpace		= new ParamSpace( atkHSpace.min, atkHSpace.max * 0.1, atkHSpace.inc, atkHSpace.unit );
		susHSpace		= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		susHSpace.max  *= 0.8;
		rlsHSpace		= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		rlsHSpace.max  *= 0.1;
		rlsHSpace		= new ParamSpace( rlsHSpace.min, rlsHSpace.max * 0.1, rlsHSpace.inc, rlsHSpace.unit );

		atkCurve	= new Curve( atkHSpace, vSpace );
		atkCurve.addPoint( atkHSpace.min, 0.0 );
		atkCurve.addPoint( atkHSpace.max, vSpace.max );
		susCurve	= new Curve( susHSpace, vSpace );
		susCurve.addPoint( susHSpace.min, vSpace.min );
		susCurve.addPoint( susHSpace.max, vSpace.max );
		rlsCurve	= new Curve( rlsHSpace, vSpace );
		rlsCurve.addPoint( rlsHSpace.min, vSpace.max );
		rlsCurve.addPoint( rlsHSpace.max, 0.0 );

		basic			= new Envelope( atkCurve, susCurve, rlsCurve );
		basic.atkState	= false;
		basic.rlsState	= false;

		return basic;
	}

// -------- StringComm Methoden --------

	public String toString()
	{
		String	atkCurveStr	= "";
		String	susCurveStr = "";
		String	rlsCurveStr = "";
		
		if( atkCurve != null )	atkCurveStr = atkCurve.toString();
		if( susCurve != null )	susCurveStr = susCurve.toString();
		if( rlsCurve != null )	rlsCurveStr = rlsCurve.toString();

		return( "" + atkCurveStr + '|' + susCurveStr + '|' + rlsCurveStr + '|' +
				atkState + '|' + susState + '|' + rlsState );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by Envelope.toString()
	 */
	public static Envelope valueOf( String s )
	{
		StringTokenizer strTok;
		String			tok;
		Curve			atkCurve	= null;
		Curve			susCurve	= null;
		Curve			rlsCurve	= null;
		Envelope		env;
		
		strTok			= new StringTokenizer( s, "|" );

		tok				= strTok.nextToken();
		if( tok.length() > 0 )	atkCurve = Curve.valueOf( tok );
		tok				= strTok.nextToken();
		if( tok.length() > 0 )	susCurve = Curve.valueOf( tok );
		tok				= strTok.nextToken();
		if( tok.length() > 0 )	rlsCurve = Curve.valueOf( tok );
		
		env				= new Envelope( atkCurve, susCurve, rlsCurve );
		env.atkState	= Boolean.valueOf( strTok.nextToken() ).booleanValue();
		env.susState	= Boolean.valueOf( strTok.nextToken() ).booleanValue();
		env.rlsState	= Boolean.valueOf( strTok.nextToken() ).booleanValue();

		return env;
	}
}
// class Envelope
