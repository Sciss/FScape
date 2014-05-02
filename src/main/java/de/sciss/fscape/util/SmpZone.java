/*
 *  SmpZone.java
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

import java.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class SmpZone
implements Cloneable
{
// -------- public Variablen --------

	public	Param	freqHi, freqLo;		// Frequenz-Bereich
	public	Param	velHi, velLo;		// Velocity-Bereich
	public	Param	gain		= null;
	public	Param	base		= null;	// basic freq
	public	Param	atk			= null;
	public	Param	rls			= null;

	public	String	fileName	= "";
	public	int		flags		= 0;

	public	int		uniqueID;
	
	// Flags
	public static final int	PHASE_LINEAR	=	0x00;
	public static final int	PHASE_TRIGZERO	=	0x01;
	public static final int	PHASE_TRIGSPECT	=	0x02;
	public static final int	PHASEMASK		=	0x0F;
	public static final int	LOOP			=	0x10;
	
// -------- private Variablen --------

// -------- public Methoden --------

	public SmpZone( Param freqHi, Param freqLo, Param velHi, Param velLo )
	{
		this.freqHi	= freqHi;
		this.freqLo	= freqLo;
		this.velHi	= velHi;
		this.velLo	= velLo;

		gain		= new Param(   0.0, Param.DECIBEL_AMP );
		base		= new Param( 262.0, Param.ABS_HZ );			// C4
		atk			= new Param(   1.0, Param.ABS_MS );
		rls			= new Param(   1.0, Param.ABS_MS );

		uniqueID	= Constants.createUniqueID();
	}

	/**
	 *	Clont vorgegebene SmpZone
	 *	IMPORTANT:	uniqueID bleibt dieselbe
	 */
	public SmpZone( SmpZone src )
	{
		this.freqHi		= (Param) src.freqHi.clone();
		this.freqLo		= (Param) src.freqLo.clone();
		this.velHi		= (Param) src.velHi.clone();
		this.velLo		= (Param) src.velLo.clone();
		this.atk		= (Param) src.atk.clone();
		this.rls		= (Param) src.rls.clone();
		this.gain		= (Param) src.gain.clone();
		this.base		= (Param) src.base.clone();
	
		this.fileName	= src.fileName;
		this.flags		= src.flags;
		this.uniqueID	= src.uniqueID;	// !!!
	}

	public Object clone()
	{
		return new SmpZone( this );
	}

// -------- StringComm Methoden --------

	public String toString()
	{
		return( freqHi.toString() + ';' + freqLo.toString() + ';' + velHi.toString() + ';' +
				velLo.toString() + ';' + gain.toString() + ';' + base.toString() + ';' +
				atk.toString() + ';' + rls.toString() + ";x" + fileName + ';' + String.valueOf( flags ));
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by SmpZone.toString()
	 */
	public static SmpZone valueOf( String s )
	{
		StringTokenizer strTok;
		SmpZone			smp;
		
		strTok		= new StringTokenizer( s, ";" );
		smp			= new SmpZone( Param.valueOf( strTok.nextToken() ),		// freqHi
								   Param.valueOf( strTok.nextToken() ),		// freqLo
								   Param.valueOf( strTok.nextToken() ),		// velHi
								   Param.valueOf( strTok.nextToken() ));	// velLo
		smp.gain	= new Param( Param.valueOf( strTok.nextToken() ));
		smp.base	= new Param( Param.valueOf( strTok.nextToken() ));
		smp.atk		= new Param( Param.valueOf( strTok.nextToken() ));
		smp.rls		= new Param( Param.valueOf( strTok.nextToken() ));
		smp.fileName= strTok.nextToken().substring( 1 );
		smp.flags	= Integer.parseInt( strTok.nextToken() );

		return smp;
	}
}
// class SmpZone
