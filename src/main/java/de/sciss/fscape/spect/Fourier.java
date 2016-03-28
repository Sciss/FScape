/*
 *  Fourier.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */
package de.sciss.fscape.spect;

import de.sciss.fscape.util.Constants;

public class Fourier
{
// -------- public variables --------

    public static final int	FORWARD	=	+1;
    public static final int	INVERSE	=	-1;

// -------- public methods --------

    /**
     *	One-dimensional discrete complex fourier transform
     *	Replaces a[ 0...2*len ] by its discrete Fourier transform
     *	Normalisierung mit 1/len findet nach INVERSE Transformation statt!
     *
     *	@param	a	complex array mit realteil in a[ 0, 2, 4, ... 2*len - 2 ], img.teil in a[ 1, 3, ... 2 * len -1 ]
     *			len	MUST be an integer power of 2
     *			dir	+1 means forward, -1 means backward (inverse); use Fourier.INVERSE / FORWARD !
     *
     *	The routine was adapted from 'Numerical Recipes in C' and optimized
     */
    public static void complexTransform( float a[], int len, int dir )
    {
        int		i, j;
        int		n, m;
        int		mMax, iStep;
        double	tempW, wRe, wpRe, wpIm, wIm, theta;		// Double precision for the trigonometric recurrences.
        float	tempRe, tempIm;

        n			= len << 1;
        theta		= dir * Constants.PI2;

        // This is the bit-reversal section of the routine.
        for( i = 1, j = 1; i < n; i += 2 ) {
            if( j > i ) {
                tempRe		= a[ j-1 ];					// Exchange the two complex numbers ... Real
                a[ j-1 ]	= a[ i-1 ];
                a[ i-1 ]	= tempRe;

                tempIm		= a[ j ];					// ... Imaginary
                a[ j ]		= a[ i ];
                a[ i ]		= tempIm;
            }

            for( m = len; (m >= 2) && (j > m); j -= m, m >>= 1 ) ;
            j += m;
        }

        // Here begins the Danielson-Lanczos section of the routine.
        // Outer loop executed log 2 len times.
        for( mMax = 2; n > mMax; ) {
            iStep	= mMax << 1;

            tempW	= Math.sin( theta / iStep );	 			// Initialize the trigonometric recurrence.
            wpRe	= -2 * tempW * tempW;
            wpIm	= Math.sin( theta / mMax );
            wRe		= 1.0;
            wIm		= 0.0;

            for( m = 1; m < mMax; m += 2 ) {					// Here are the two nested inner loops.
                for( i = m; i <= n; i += iStep ) {
                    j		= i + mMax;
                    tempRe	= (float) (wRe * a[ j-1 ] - wIm * a[ j ]);		// This is the Danielson-Lanczos formula
                    tempIm	= (float) (wRe * a[ j ]   + wIm * a[ j-1 ]);
                    a[ j-1 ]= a[ i-1 ] - tempRe;
                    a[ j ]	= a[ i ]   - tempIm;
                    a[ i-1 ]+= tempRe;
                    a[ i ]	+= tempIm;
                }
                tempW	 = wRe;
                wRe		+= tempW * wpRe - wIm * wpIm;		 	// Trigonometric recurrence.
                wIm		+= tempW * wpIm + wIm * wpRe;
            }
            mMax = iStep;
        }

        // Normalize data when inverse transforming
        if( dir == INVERSE ) {
            for( i = 0; i < n; i++ ) {
                a[ i ] /= len;
            }
        }
    }

    /**
     *	One-dimensional discrete real fourier transform
     *	Replaces a[ 0...len ] by its discrete Fourier transform
     *	(positive freq. half of the complex spectrum)
     *
     *	@param	a	real array; Ausgabe ist komplex mit realteil in a[ 0, 2, 4, ... len ],
     *					img.teil in a[ 1, 3, ... len + 1 ];
     *				ACHTUNG: a hat also len+2 Elemente!! Die letzten zwei muessen fuer FORWARD Null sein;
     *			len	MUST be an integer power of 2
     *			dir	+1 means forward, -1 means backward (inverse); use Fourier.INVERSE / FORWARD !
     *
     *	The routine was adapted from 'Numerical Recipes in C' and optimized
     */

    public static void realTransform( float a[], int len, int dir )
    {
        int		i, i2, i3, i4;
        float	c1, c2, h1Re, h1Im, h2Re, h2Im;
        double	wRe, wIm, wpRe, wpIm, tempW, theta;		//  Double precision for the trigonometric recurrences.
        int		cLen;

        cLen	= len >> 1;
        theta	= dir * Math.PI / cLen;				// Initialize the recurrence.
        c1		= 0.5f;
        c2		= -dir * 0.5f;
        tempW	= Math.sin( theta / 2 );
        wpRe	= -2 * tempW * tempW;
        wpIm	= Math.sin( theta );
        wRe		= 1.0 + wpRe;
        wIm		= wpIm;

        if( dir == FORWARD ) {
            complexTransform( a, cLen, dir );		//  The forward transform is here.
        }

        for( i = 2; i < cLen; i += 2 ) {			// Case i == 0 done separately below.
            i2		= i  + 1;
            i3		= len - i;
            i4		= i3 + 1;
            h1Re	= c1 * (a[ i  ] + a[ i3 ]);		// The two separate transforms are separated out of data.
            h1Im	= c1 * (a[ i2 ] - a[ i4 ]);
            h2Re	= -c2* (a[ i2 ] + a[ i4 ]);
            h2Im	= c2 * (a[ i  ] - a[ i3 ]);
            a[ i  ]	= (float) (h1Re + wRe * h2Re - wIm * h2Im);	// Here they are recombined to form
            a[ i2 ]	= (float) (h1Im + wRe * h2Im + wIm * h2Re);	//   the true transform of the original real data.
            a[ i3 ]	= (float) (h1Re - wRe * h2Re + wIm * h2Im);
            a[ i4 ]	= (float) (-h1Im+ wRe * h2Im + wIm * h2Re);
            tempW	= wRe;
            wRe	   += tempW * wpRe - wIm * wpIm;		// The recurrence.
            wIm	   += tempW * wpIm + wIm * wpRe;
        }

        if( dir == INVERSE ) {
            h1Re		= a[ 0 ];
            a[ 0 ]		= c1 * (h1Re + a[ len ]);
            a[ 1 ]		= c1 * (h1Re - a[ len ]);
            a[ len ]	= 0.0f;
            a[ len+1 ]	= 0.0f;
            complexTransform( a, cLen, dir );		// This is the inverse transform for the case dir=-1.
        } else {
            h1Re		= a[ 0 ];
            a[ 0 ]		= h1Re + a[ 1 ];				// Squeeze the first and last data together
            a[ len ]	= h1Re - a[ 1 ];				//   to get them all within the original array.
            a[ 1 ]		= 0.0f;
            a[ len+1 ]	= 0.0f;
        }
    }

    /**
     *	Komplexes Arrays von Rechteck in Polarform wandeln
     *
     *	@param	src		Array mit abwechselnd Real/Img Teil
     *	@param	dest	kann identisch mit src sein (in-place)
     *	@param	srcOff	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	length	complexe Laenge mal 2
     */
    public static void rect2Polar( float src[], int srcOff, float dest[], int destOff, int length )
    {
        int		i, j;
        double	d1, d2;

        if( (src == dest) && (srcOff < destOff) ) {		// in-place rueckwaerts
            for( i = srcOff + length, j = destOff + length; i > srcOff; ) {
                d1			= src[ --i ];		// img
                d2			= src[ --i ];		// real
                dest[ --j ] = (float) Math.atan2( d1, d2 );
                dest[ --j ] = (float) Math.sqrt( d1*d1 + d2*d2 );
            }
        } else {
            for( i = srcOff, j = destOff; i < srcOff + length; ) {
                d2			= src[ i++ ];		// real
                d1			= src[ i++ ];		// img
                dest[ j++ ] = (float) Math.sqrt( d1*d1 + d2*d2 );
                dest[ j++ ] = (float) Math.atan2( d1, d2 );
            }
        }
    }

    /**
     *	Komplexes Arrays von Polar in Rechteckform wandeln
     *
     *	@param	src		Array mit abwechselnd Amp/Phase Teil
     *	@param	dest	kann identisch mit src sein (in-place)
     *	@param	srcOff	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	length	complexe Laenge mal 2
     */
    public static void polar2Rect( float src[], int srcOff, float dest[], int destOff, int length )
    {
        int		i, j;
        double	d1, d2;

        if( (src == dest) && (srcOff < destOff) ) {		// in-place rueckwaerts
            for( i = srcOff + length, j = destOff + length; i > srcOff; ) {
                d1			= src[ --i ];		// phase
                d2			= src[ --i ];		// amp
                dest[ --j ] = (float) (d2 * Math.sin( d1 ));
                dest[ --j ] = (float) (d2 * Math.cos( d1 ));
            }
        } else {
            for( i = srcOff, j = destOff; i < srcOff + length; ) {
                d2			= src[ i++ ];		// amp
                d1			= src[ i++ ];		// phase
                dest[ j++ ] = (float) (d2 * Math.cos( d1 ));
                dest[ j++ ] = (float) (d2 * Math.sin( d1 ));
            }
        }
    }

    /**
     *	Komplexe Arrays multiplizieren
     *
     *	@param	src1	Array complex
     *	@param	srcOff1	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	src2	Array complex
     *	@param	srcOff2	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	dest	kann identisch mit *einem* src sein (in-place)
     *	@param	length	complexe Laenge mal 2
     */
    public static void complexMult( float src1[], int srcOff1, float src2[], int srcOff2,
                                    float dest[], int destOff, int length )
    {
        int		i, j, k;
        float	im1, im2, re1, re2;

        if( ((src1 == dest) && (srcOff1 < destOff)) ||
            ((src2 == dest) && (srcOff2 < destOff)) ) {		// in-place rueckwaerts

            for( i = srcOff1 + length, j = srcOff2 + length, k = destOff + length; i > srcOff1; ) {
                im1			= src1[ --i ];
                re1			= src1[ --i ];
                im2			= src2[ --j ];
                re2			= src2[ --j ];
                dest[ --k ] = im1 * re2 + re1 * im2;
                dest[ --k ] = re1 * re2 - im1 * im2;
            }

        } else {
            for( i = srcOff1, j = srcOff2, k = destOff; i < srcOff1 + length; ) {
                re1			= src1[ i++ ];
                im1			= src1[ i++ ];
                re2			= src2[ j++ ];
                im2			= src2[ j++ ];
                dest[ k++ ] = re1 * re2 - im1 * im2;
                dest[ k++ ] = im1 * re2 + re1 * im2;
            }
        }
    }

    /**
     *	2-PI geclippte Phasen eines komplexes Polar-Arrays "entfalten"
     *
     *	@param	src		Array mit abwechselnd Amp/Phase Teil
     *	@param	dest	kann identisch mit src sein (in-place),
     *					dann aber darf destOff nicht kleiner als srcOff sein!!!
     *	@param	srcOff	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	length	complexe Laenge mal 2
     */
    public static void unwrapPhases( float src[], int srcOff, float dest[], int destOff, int length )
    {
        int		i, j, k;
        double	d2;
        double	d1	= 0.0;
        double	d3	= 0.0;

        for( i = srcOff + 1, j = destOff + 1, k = 0; i < srcOff + length; i += 2, j += 2 ) {
            d2 = src[ i ];
            if( d2 - d1 > Math.PI ) {			// neg. fold
                k--;
                d3 = k * Constants.PI2;
            } else if( d1 - d2 > Math.PI ) {	// pos. fold
                k++;
                d3 = k * Constants.PI2;
            }
            dest[ j ] = (float) (d2 + d3);
            d1 = d2;
        }
    }

    /**
     *	faltet die Phasen eines komplexes Polar-Arrays, so dass sie zw. -Pi und +Pi liegen
     *
     *	@param	src		Array mit abwechselnd Amp/Phase Teil
     *	@param	dest	kann identisch mit src sein (in-place),
     *					dann aber darf destOff nicht kleiner als srcOff sein!!!
     *	@param	srcOff	tatsaechlicher Array-Index (complexer Offset mal 2)
     *	@param	length	complexe Laenge mal 2
     */
    public static void wrapPhases( float src[], int srcOff, float dest[], int destOff, int length )
    {
        int		i, j, k;
        double	d2;
        double	d3	= 0.0;

        for( i = srcOff + 1, j = destOff + 1, k = 0; i < srcOff + length; i += 2, j += 2 ) {
            d2 = src[ i ];
            while( d2 - d3 > Math.PI ) {
                k++;
                d3 = k * Constants.PI2;
            }
            while( d3 - d2 > Math.PI ) {
                k--;
                d3 = k * Constants.PI2;
            }
            dest[ j ] = (float) (d2 - d3);
        }
    }
}
// class Fourier
