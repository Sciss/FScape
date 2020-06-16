/*
 *  Constants.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

public class Constants
{
	public static final double	ln10		= 2.302585092994046;
	public static final double  ln2			= 0.6931471805599453;
	public static final double	PI2			= Math.PI * 2;
	public static final double	PIH			= Math.PI / 2;
	public static final double	minPercent	= 6.30957344480193e-6;	// Grenze von -144 dB
	public static final double	minDecibel	= -144.0;
	public static final double	maxDecibel	= +144.0;
	
	public static final	double	suckyDoubleError	=	1.0e-6;	// Fehlerspanne z.B. bei ParamSpace.contains()
	
	public static int	OSTypeFScape	= 0x46536320;   // "FSc "
	public static int	OSTypePrefs		= 0x46536350;   // "FScP"
	public static int	OSTypeDoc		= 0x46536344;   // "FScP"
		
	// spaces[ ... ]
	public static final int		emptySpace				=  0;
	public static final int		modSpace				=  1;
	public static final int		unsignedModSpace		=  2;

	public static final int		absAmpSpace				=  3;
	public static final int		factorAmpSpace			=  4;
	public static final int		ratioAmpSpace			=  5;
	public static final int		offsetAmpSpace			=  6;
	public static final int		decibelAmpSpace			=  7;

	public static final int		absMsSpace				=  8;
	public static final int		absBeatsSpace			=  9;
	public static final int		factorTimeSpace			= 10;
	public static final int		ratioTimeSpace			= 11;
	public static final int		offsetMsSpace			= 12;
	public static final int		offsetBeatsSpace		= 13;
	public static final int		offsetTimeSpace			= 14;

	public static final int		absHzSpace				= 15;
	public static final int		factorFreqSpace			= 16;
	public static final int		ratioFreqSpace			= 17;
	public static final int		offsetHzSpace			= 18;
	public static final int		offsetSemitonesSpace	= 19;
	public static final int		offsetFreqSpace			= 20;
	public static final int		lfoHzSpace				= 21;

	public static final int		NUM_SPACES				= 22;

	/**
	 *	READONLY, ggf clonen
	 */
	public static ParamSpace	spaces[];

	static {
		// ParamSpaces
		spaces = new ParamSpace[ NUM_SPACES ];

		spaces[ emptySpace ]		= new ParamSpace();
		spaces[ modSpace ]			= new ParamSpace(      -100.0,      100.0, 0.01,   Param.FACTOR );
		spaces[ unsignedModSpace ]	= new ParamSpace(         0.0,      100.0, 0.01,   Param.FACTOR );

		spaces[ absAmpSpace ]		= new ParamSpace(         0.0,        1.0, 0.0001, Param.ABS_AMP );
		spaces[ factorAmpSpace ]	= new ParamSpace(         0.0,  1000000.0, 0.01,   Param.FACTOR_AMP );
		spaces[ ratioAmpSpace ]		= new ParamSpace(         0.0,      100.0, 0.01,   Param.FACTOR_AMP );
		spaces[ offsetAmpSpace ]	= new ParamSpace(      -100.0,   999900.0, 0.01,   Param.OFFSET_AMP );
		spaces[ decibelAmpSpace ]	= new ParamSpace(  minDecibel, maxDecibel, 0.01,   Param.DECIBEL_AMP );
		spaces[ absMsSpace ]		= new ParamSpace(		  0.0, 36000000.0, 0.1,    Param.ABS_MS );
		spaces[ absBeatsSpace ]		= new ParamSpace(		  0.0,   240000.0, 0.001,  Param.ABS_BEATS );
		spaces[ factorTimeSpace ]	= new ParamSpace(		  0.0,  1000000.0, 0.01,   Param.FACTOR_TIME );
		spaces[ ratioTimeSpace ]	= new ParamSpace(		  0.0,      100.0, 0.01,   Param.FACTOR_TIME );
		spaces[ offsetMsSpace ]		= new ParamSpace( -36000000.0, 36000000.0, 0.1,    Param.OFFSET_MS );
		spaces[ offsetBeatsSpace ]	= new ParamSpace(   -240000.0,   240000.0, 0.001,  Param.OFFSET_BEATS );
		spaces[ offsetTimeSpace ]	= new ParamSpace(      -100.0,   999900.0, 0.01,   Param.OFFSET_TIME );
		spaces[ absHzSpace ]		= new ParamSpace(         0.0,    48000.0, 0.1,    Param.ABS_HZ );
		spaces[ factorFreqSpace ]	= new ParamSpace(         0.0,  1000000.0, 0.01,   Param.FACTOR_FREQ );
		spaces[ ratioFreqSpace ]	= new ParamSpace(         0.0,      100.0, 0.01,   Param.FACTOR_FREQ );
		spaces[ offsetHzSpace ]		= new ParamSpace(	 -48000.0,    48000.0, 0.1,    Param.OFFSET_HZ );
		spaces[ offsetSemitonesSpace]=new ParamSpace(	   -180.0,      180.0, 0.001,  Param.OFFSET_SEMITONES );
		spaces[ offsetFreqSpace ]	= new ParamSpace(	   -100.0,   999900.0, 0.01,   Param.OFFSET_FREQ );
		spaces[ lfoHzSpace ]		= new ParamSpace(	      0.0,     1000.0, 0.01,   Param.ABS_HZ );
	}

// -------- private variables --------

	private static int uniqueID	= 0;

// -------- public methods --------


	/**
	 *	Creates a unique integer, i.e. calling this method
	 *	several times will always produces new numbers; they
	 *	can therefore be used to exactly identify any object
	 *	(e.g. when an object is cloned it's reference is not
	 *	unique anymore but if you copy its ID it can be identified)
	 *
	 *	NOTE: if you save uniqueID's into a file and load them
	 *		  again, there meaning might be senseless!
	 */
	static int createUniqueID()
	{
		return uniqueID++;
	}
}
// class Constants
