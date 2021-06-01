/*
 *  Param.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

import de.sciss.fscape.spect.SpectStream;

public class Param implements Cloneable {

    // -------- Unit constants --------

	/**
	 *	Magnitudes (Dimensions)
	 */
	public static final int NONE		=	0x0000;
	public static final int AMP			=	0x0001;		// has no units
	public static final int TIME		=	0x0002;		// default: ms
	public static final int FREQ		=	0x0003;		// default: Hz
	public static final int PHASE		=	0x0004;		// default: degrees

	public static final int DIM_MASK	=	0x000F;

	/**
	 *	Display (Form)
	 */
	public static final int ABS_UNIT    =	0x0000;		// ms, Hz, ...
	public static final int ABS_PERCENT =	0x0010;		// %
	public static final int REL_UNIT    =	0x0020;		// +/- ms, +/- Hz, ...
	public static final int REL_PERCENT =	0x0030;		// +/- %

	public static final int FORM_MASK	=	0x00F0;
	
	/**
	 *	Special forms
	 */
	public static final int BEATS		=	0x0100;
	public static final int SEMITONES	=	0x0200;
	public static final int DECIBEL		=	0x0300;

	public static final int SPECIAL_MASK=	0x0F00;

	/**
	 *	Common units
	 */
	public static final int FACTOR			=	NONE | ABS_PERCENT;
	public static final int ABS_AMP			=	AMP  | ABS_UNIT;
	public static final int FACTOR_AMP		=	AMP  | ABS_PERCENT;
	public static final int DECIBEL_AMP		=	AMP  | ABS_PERCENT | DECIBEL;
	public static final int OFFSET_AMP		=	AMP  | REL_PERCENT;
	public static final int ABS_MS			=	TIME | ABS_UNIT;
	public static final int ABS_BEATS		=	TIME | ABS_UNIT | BEATS;
	public static final int FACTOR_TIME		=	TIME | ABS_PERCENT;
	public static final int OFFSET_MS		=	TIME | REL_UNIT;
	public static final int OFFSET_BEATS	=	TIME | REL_UNIT | BEATS;
	public static final int OFFSET_TIME		=	TIME | REL_PERCENT;
	public static final int ABS_HZ			=	FREQ | ABS_UNIT;
	public static final int FACTOR_FREQ		=	FREQ | ABS_PERCENT;
	public static final int OFFSET_HZ		=	FREQ | REL_UNIT;
	public static final int OFFSET_SEMITONES=	FREQ | REL_UNIT | SEMITONES;
	public static final int OFFSET_FREQ		=	FREQ | REL_PERCENT;

    // -------- public variables --------

	public	double  value;
	public	int		unit;

    // -------- private variables --------

	private final static int chain[]	= { 1, 0, 2, 3 };	// mapping; vgl. transform()
	private static double	freqScale;
	private static double	timeScale;

	static {
		updateScales();
	}

    // -------- public methods --------

    public Param() {
        value = 0.0;
        unit    = NONE;
    }

	/**
	 *	@param	value	value
	 *	@param	unit	unit of measure
	 */
    public Param(double value, int unit) {
        this.value  = value;
        this.unit   = unit;
    }

    public Param(Param src) {
        this.value  = src.value;
        this.unit   = src.unit;
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
//		String value;
//	
//		value = Application.userPrefs.get( MainPrefs.KEY_FREQSCALE, null );
//		if( value != null ) {
//			freqScale	= Param.valueOf( value ).value;
//		} else {
			freqScale	= 12.0;
//		}
//		value = Application.userPrefs.get( MainPrefs.KEY_TIMESCALE, null );
//		if( value != null ) {
//			timeScale	= Param.valueOf( value ).value;
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
    public static Param transform(Param src, int destUnit, Param reference, SpectStream stream) {
        if (src.unit == destUnit) return src;

        double  destVal = src.value;
        int     srcChain, destChain;

        // -------- take out special forms --------
        switch (src.unit & SPECIAL_MASK) {
            case BEATS:
                destVal *= 60000.0 / timeScale;
                break;

            case SEMITONES:
                if (reference == null) return null;
                destVal = reference.value * (Math.pow(2, destVal / freqScale) - 1);
                break;

            case DECIBEL:
                destVal = Math.exp(destVal / 20 * Constants.ln10) * 100;
                break;

            default:
                break;
        }

        // chain: Abs% <==> AbsUnit <==> RelUnit <==> Rel%
        srcChain  = chain[(src.unit & FORM_MASK) >> 4];
        destChain = chain[(destUnit & FORM_MASK) >> 4];

        if (srcChain != destChain) {

            if (reference == null) return null;    // we need reference here
            // i.e. in the form of an absolute unit:
            reference = Param.transform(reference,
                    (reference.unit & ~(FORM_MASK | SPECIAL_MASK)) | ABS_UNIT,
                    null, stream);

            if (reference == null) return null;        // transformation did not work out

            try {
                synchronized (reference) {

                    if (srcChain < destChain) {    // -------- chain from left to right --------

                        for (int i = srcChain; i < destChain; i++) {

                            switch (i) {
                                case 0:
                                    destVal *= reference.value / 100;     // Abs% ==> AbsUnit
                                    break;
                                case 1:
                                    destVal -= reference.value;           // AbsUnit ==> RelUnit
                                    break;
                                case 2:
                                    destVal *= 100 / reference.value;     // RelUnit ==> Rel%
                                    break;
                                default:
                                    break;
                            }
                        }

                    } else {                        // -------- chain from right to left --------

                        for (int i = srcChain; i > destChain; i--) {

                            switch (i) {
                                case 3:
                                    destVal *= reference.value / 100;    // Rel% ==> RelUnit
                                    break;
                                case 2:
                                    destVal += reference.value;        // RelUnit ==> AbsUnit
                                    break;
                                case 1:
                                    destVal *= 100 / reference.value;    // AbsUnit ==> Abs%
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            } catch (ArithmeticException e) {    // division by zero
                return null;
            }
        }

		// -------- include special forms --------

		switch (destUnit & SPECIAL_MASK) {
			case BEATS:
				destVal *= timeScale / 60000;
				break;

			case SEMITONES:
				if (reference == null) return null;
				destVal = Math.log(1 + destVal / reference.value) / Constants.ln2 * freqScale;
				break;

			case DECIBEL:
				if (destVal > Constants.minPercent) {
					destVal = Math.log(destVal / 100) * 20 / Constants.ln10;
				} else {    // value too small; minus infinite dB assumed to be -144 dB
					destVal = Constants.minDecibel;
				}
				break;

			default:
				break;
		}

		return new Param(destVal, destUnit);
	}

	/**
	 *	Ermittelt, wie "gut" eine Einheiten-Umwandlung waere,
	 *	das Ergebnis kann mit anderen verglichen werden; je hoeher
	 *	der Wert, desto eher ist die entsprechende Umwandlung
	 *	anderen vorzuziehen
	 */
	public static int getPriority(int srcUnit, int destUnit) {
		if (srcUnit == destUnit) return 99;                                // best one
		if ((srcUnit & DIM_MASK) != (destUnit & DIM_MASK)) return -99;        // worst

		int srcChain, destChain;

		srcChain  = chain[(srcUnit  & FORM_MASK) >> 4];
		destChain = chain[(destUnit & FORM_MASK) >> 4];

		if (srcChain <= destChain) {
			return (destChain - srcChain);
		} else {
			return (srcChain - destChain);
		}
	}

// -------- StringComm methods --------

	public String toString()
	{
		return( "" + value + ',' + unit );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by Param.toString()
	 */
	public static Param valueOf(String s) {
		int i = s.indexOf(',');

		return new Param(Double.valueOf(s.substring(0, i)).doubleValue(),    // value
				Integer.parseInt(s.substring(i + 1)));                // unit
	}
}