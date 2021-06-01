/*
 *  Wavelet.java
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

package de.sciss.fscape.spect;

public class Wavelet
{
// -------- public variables --------

    public static final int COEFFS_DAUB4	= 0;
    public static final int COEFFS_DAUB6	= 1;
    public static final int COEFFS_DAUB8	= 2;
    public static final int COEFFS_DAUB10	= 3;
    public static final int COEFFS_DAUB12	= 4;
    public static final int COEFFS_DAUB14	= 5;
    public static final int COEFFS_DAUB16	= 6;
    public static final int COEFFS_DAUB18	= 7;
    public static final int COEFFS_DAUB20	= 8;

    public static final int COEFFS_MAX		= 8;

// -------- private variables --------

    protected static final float daub4_c0	= +0.4829629131445341f;	// Daubechies4 Coefficients
    protected static final float daub4_c1	= +0.8365163037378079f;
    protected static final float daub4_c2	= +0.2241438680420134f;
    protected static final float daub4_c3	= -0.1294095225512604f;

    protected static final float static_cc[][]	=	{
        // Daubechies 4
        { daub4_c0, daub4_c1, daub4_c2, daub4_c3 },
        // the following taken from S.Mallat, Wavelet Tour of Signal Processing, p.251
        // Daubechies 6
        { +0.332670552950f, +0.806891509311f, +0.459877502118f, -0.135011020010f,
          -0.085441273882f, +0.035226291882f },
        // Daubechies 8
        { +0.230377813309f, +0.714846570553f, +0.630880767930f, -0.027983769417f,
          -0.187034811719f, +0.030841381836f, +0.032883011667f, -0.010597401785f },
        // Daubechies 10
        { +0.160102397974f, +0.603829269797f, +0.724308528438f, +0.138428145901f,
          -0.242294887066f, -0.032244869585f, +0.077571493840f, -0.006241490213f,
          -0.012580751999f, +0.003335725285f },
        // Daubechies 12
        { +0.111540743350f, +0.494623890398f, +0.751133908021f, +0.315250351709f,
          -0.226264693965f, -0.129766867567f, +0.097501605587f, +0.027522865530f,
          -0.031582039318f, +0.000553842201f, +0.004777257511f, -0.001077301085f },
        // Daubechies 14
        { +0.077852054085f, +0.396539319482f, +0.729132090846f, +0.469782287405f,
          -0.143906003929f, -0.224036184994f, +0.071309219267f, +0.080612609151f,
          -0.038029936935f, -0.016574541631f, +0.012550998556f, +0.000429577973f,
          -0.001801640704f, +0.000353713800f },
        // Daubechies 16
        { +0.054415842243f, +0.312871590914f, +0.675630736297f, +0.585354683654f,
          -0.015829105256f, -0.284015542962f, +0.000472484574f, +0.128747426620f,
                        // check vvv this one! XXX
          -0.017369301002f, -0.04408825393f,  +0.013981027917f, +0.008746094047f,
          -0.004870352993f, -0.000391740373f, +0.000675449406f, -0.000117476784f },
        // Daubechies 18
        { +0.038077947364f, +0.243834674613f, +0.604823123690f, +0.657288078051f,
          +0.133197385825f, -0.293273783279f, -0.096840783223f, +0.148540749338f,
          +0.030725681479f, -0.067632829061f, +0.000250947115f, +0.022361662124f,
          -0.004723204758f, -0.004281503682f, +0.001847646883f, +0.000230385764f,
          -0.000251963189f, +0.000039347320f },
        // Daubechies 20
        { +0.026670057901f, +0.188176800078f, +0.527201188932f, +0.688459039454f,
          +0.281172343661f, -0.249846424327f, -0.195946274377f, +0.127369340336f,
          +0.093057364604f, -0.071394147166f, -0.029457536822f, +0.033212674059f,
          +0.003606553567f, -0.010733175483f, +0.001395351747f, +0.001992405295f,
          -0.000685856695f, -0.000116466855f, +0.000093588670f, -0.000013264203f }
    };

    protected static final String[] filterNames	= {
        "Daubechies  4", "Daubechies  6", "Daubechies  8", "Daubechies 10", "Daubechies 12",
        "Daubechies 14", "Daubechies 16", "Daubechies 18", "Daubechies 20"
    };

// -------- public methods --------

    /**
     *	One-dimensional discrete (forward) wavelet transform with daubechies4 coefficents
     *	; pyramid algorithm, replacing a[ 0...len-1 ] by its wavelet transform
     *	Note that len MUST be an integer power of 2 and greater or equal 4.
     *
     *	NOTE: support differs from general transform functions!!
     *
     *	The routine was adapted from 'Numerical Recipes in C' (well at least originally ;)
     */
    public static void fwdTransformDaub4( float a[], int len )
    {
//		int		len		= a.length;
        float[]	temp	= new float[ len >> 1 ];
        int		i, j, k;
        int		detail;
        float	a0, a1;

        // Start at largest hierarchy, and work towards smallest
        for( i = len; i >= 4; i >>= 1 ) {

            detail	= (i >> 1) - 1;		// NOTE: minus 1 !!
            a0		= a[ 0 ];
            a1		= a[ 1 ];

            for( j = 0, k = 0; j < detail; ) {

                temp[ j ]	= daub4_c3*a[k]   - daub4_c2*a[k+1] + daub4_c1*a[k+2] - daub4_c0*a[k+3];	// detail
                a[ j++ ]	= daub4_c0*a[k++] + daub4_c1*a[k++] + daub4_c2*a[k]   + daub4_c3*a[k+1];	// smooth	(j <= k)
            }
            a[ detail ]	= daub4_c0*a[i-2] + daub4_c1*a[i-1] + daub4_c2*a0 + daub4_c3*a1;		// wrap around
            a[ i - 1 ]	= daub4_c3*a[i-2] - daub4_c2*a[i-1] + daub4_c1*a0 - daub4_c0*a1;

            for( j--; j >= 0; ) {					// replace by System.copyArray()?
                a[ k-- ] = temp[ j-- ];				// copy temporarily saved values back
            }
        }
    }

    /**
     *	One-dimensional discrete inverse wavelet transform with daubechies4 coefficents
     *	; pyramid algorithm, replacing wavelet a[ 0...len-1 ] by its original signal
     *	Note that len MUST be an integer power of 2 and greater or equal 4.
     *
     *	The routine was adapted from 'Numerical Recipes in C' (well at least originally ;)
     */
    public static void invTransformDaub4( float a[], int len )
    {
//		int		len		= a.length;
        float[]	temp	= new float[ len ];
        int		i, j, k;
        int		detail;

        // Start at smallest hierarchy, and work towards largest.
        for( i = 4; i <= len; i <<= 1 ) {

            detail		= (i >> 1);

            for( j = detail - 2, k = i; j >= 0; j-- ) {

                temp[ --k ]	= daub4_c3*a[j] - daub4_c0*a[j+detail] + daub4_c1*a[j+1] - daub4_c2*a[j+detail+1];	// detail
                temp[ --k ]	= daub4_c2*a[j] + daub4_c1*a[j+detail] + daub4_c0*a[j+1] + daub4_c3*a[j+detail+1];	// smooth
            }
            temp[ 1 ]	= daub4_c3*a[detail-1] - daub4_c0*a[i-1] + daub4_c1*a[0] - daub4_c2*a[detail];	// wrap around
            a[ 0 ]		= daub4_c2*a[detail-1] + daub4_c1*a[i-1] + daub4_c0*a[0] + daub4_c3*a[detail];

            for( j = 1; j < i; j++ ) {				// replace by System.copyArray()?
                a[ j ] = temp[ j ];					// copy temporarily saved values back
            }
        }
    }

    /**
     *	Custom wavelet coefficients besorgen
     *
     *	@param	ID	COEFFS_...
     *	@return		Coeffizienten fuer fwdTransform/invTransform; null bei Fehler
     */
    public static float[][] getCoeffs( int ID )
    {
        float	cc[];
        float	ccStat[];
        float	cr[];
        float	flt[][]	= null;
        int		nCoeffs;

        if( (ID >= 0) && (ID <= COEFFS_MAX) ) {
            ccStat		= Wavelet.static_cc[ ID ];
            nCoeffs		= ccStat.length;
            cc			= new float[ nCoeffs ];
            flt			= new float[ 2 ][];
            cr			= new float[ nCoeffs ];
            flt[ 0 ]	= cc;
            flt[ 1 ]	= cr;

            for( int i = 0, sig = -1; i < nCoeffs; i++) {
                cc[ i ]					= ccStat[ i ];	// (float) ((double) ccStat[ i ] / sqrt2);
                cr[ nCoeffs - i - 1 ]	= sig * cc[ i ];
                sig = -sig;
            }
        }
        return flt;
    }

    /**
     *	Array mit den Namen der vorhandenen Filter besorgen
     */
    public static String[] getFilterNames()
    {
        return filterNames;
    }

    /**
     *	ID zu FilterNamen besorgen; kann als Parameter fuer getCoeffs benutzt werden
     *	wenn Name unbekannt, wird Daub4-Filter zurueckgegeben
     */
    public static int getCoeffsID( String filterName )
    {
        for( int i = 0; i < filterNames.length; i++ ) {
            if( filterNames[ i ].equals( filterName )) return i;
        }
        return COEFFS_DAUB4;
    }

    /**
     *	One-dimensional discrete (forward) wavelet transform with custom coefficents
     *	; pyramid algorithm, replacing a[ 0...len-1 ] by its wavelet transform
     *	Note that len MUST be an integer power of 2 and greater or equal 4.
     *
     *	@param	c	filter coefficients as received by getCoeffs
     *
     *	The routine was adapted from 'Numerical Recipes in C' and optimized
     */
    public static void fwdTransform( float a[], int len, float c[][] )
    {
        float[]	temp	= new float[ len ];
        float	tempC, tempR;
        int		h, i, j, k;
        float	cc[]	= c[ 0 ];
        float	cr[]	= c[ 1 ];
        float	cc0		= cc[ 0 ];
        float	cr0		= cr[ 0 ];
        int		nCoeffs	= cc.length;
        int		nMod, nMask;
        int		nHalf;
        int		cOff, nOff;

        for( h = len; h >= 4; h >>= 1 ) {			// Start at largest hierarchy, and work towards smallest.

            nMod	= nCoeffs * h;					// A positive constant equal to zero mod len.
            nMask	= h-1;							// Mask of all bits, since len a power of 2.
            nHalf	= h >> 1;
            cOff	= -((nCoeffs-1) >> 1) + nMod;	// ALREADY WRAPPED AROUND!

            for( i = 0, nOff = cOff; i < nHalf; i++, nOff += 2 ) {
                k		= nOff & nMask;
                tempC	= cc0 * a[ k ];				// first values outside j-loop
                tempR	= cr0 * a[ k ];
                for( j = 1; j < nCoeffs; j++ ) {
                    k		 = (nOff + j) & nMask;	// We use bitwise and to wrap-around the pointers.
                    tempC	+= cc[ j ] * a[ k ];
                    tempR	+= cr[ j ] * a[ k ];
                }
                temp[ i ]			= tempC;
                temp[ i + nHalf ]	= tempR;
            }

            System.arraycopy( temp, 0, a, 0, h );	// Copy the results back from workspace.
        }
    }

    /**
     *	One-dimensional discrete (forward) wavelet transform with custom coefficents
     *	takes different input and output buffers, performs only one step at a time
     *	NOTE: a must have c[0].length samples preceeding a[ off ] and
     *		  the same number succeeding a[ off+len ]!!!!!
     *
     *	@param	a	input
     *	@param	s	smooth buffer (half size of len)
     *	@param	d	detail buffer (half size of len)
     *	@param	off	offset in a; see NOTE above!
     *	@param	len	number of samples to filter; see NOTE above! muss durch 2 teilbar sein!
     *	@param	c	filter coefficients as received by getCoeffs
     */
    public static void fwdTransform( float a[], float s[], float d[], int off, int len, float c[][] )
    {
        float	tempC, tempR;
        int		i, j, k;
        float	cc[]	= c[ 0 ];
        float	cr[]	= c[ 1 ];
        float	cc0		= cc[ 0 ];
        float	cr0		= cr[ 0 ];
        int		nCoeffs	= cc.length;
        int		nHalf	= len >> 1;
        int		nOff	= -((nCoeffs-1) >> 1) + off;		// approx. centered support

        for( i = 0; i < nHalf; i++, nOff += 2 ) {
            k		= nOff;
            tempC	= cc0 * a[ k ];				// first values outside j-loop
            tempR	= cr0 * a[ k++ ];
            for( j = 1; j < nCoeffs; j++ ) {
                tempC	+= cc[ j ] * a[ k ];
                tempR	+= cr[ j ] * a[ k++ ];
            }
            s[ i ]	= tempC;
            d[ i ]	= tempR;
        }
    }

    /**
     *	One-dimensional discrete (backward) wavelet transform with custom coefficents
     *	; pyramid algorithm, replacing a[ 0...len-1 ] by its inverse wavelet transform
     *	Note that len MUST be an integer power of 2 and greater or equal 4.
     *
     *	@param	c	filter coefficients as received by getCoeffs
     *
     *	The routine was adapted from 'Numerical Recipes in C' and optimized
     */
    public static void invTransform( float a[], int len, float c[][] )
    {
        float[]	temp	= new float[ len ];
        float	aC, aR;
        int		h, i, j, k;
        float	cc[]	= c[ 0 ];
        float	cr[]	= c[ 1 ];
//		float	cc0		= cc[ 0 ];
//		float	cr0		= cr[ 0 ];
        int		nCoeffs	= cc.length;
        int		nMod, nMask;
        int		nHalf;
        int		cOff, nOff;

        for( h = 4; h <= len; h <<= 1 ) {			// Start at smallest hierarchy, and work towards largest.

            nMod	= nCoeffs * h;					// A positive constant equal to zero mod len.
            nMask	= h-1;							// Mask of all bits, since len a power of 2.
            nHalf	= h >> 1;
            cOff	= -((nCoeffs-1) >> 1) + nMod;	// ALREADY WRAPPED AROUND!

            for( i = 0; i < h; i++ ) {
                temp[ i ] = 0.0f;
            }

            for( i = 0, nOff = cOff; i < nHalf; i++, nOff += 2 ) {
                aC = a[ i ];
                aR = a[ i + nHalf ];
                for( j = 0; j < nCoeffs; j++ ) {
                    k		   = (nOff + j) & nMask;	// We use bitwise and to wrap-around the pointers.
                    temp[ k ] += cc[ j ] * aC + cr[ j ] * aR;
                }
            }

            System.arraycopy( temp, 0, a, 0, h );	// Copy the results back from workspace.
        }
    }

    /**
     *	One-dimensional discrete (backward) wavelet transform with custom coefficents
     *	takes different input and output buffers, performs only one step at a time
     *	NOTE: s and d must have c[0].length samples preceeding s[ off ] resp. d[ off ] and
     *		  the same number succeeding s[ off+len/2 ] resp. d[ off+len/2 ]!!!!!
     *
     *	@param	a	output
     *	@param	s	smooth input (half size of len)
     *	@param	d	detail input (half size of len)
     *	@param	off	offset in s, d; see NOTE above!
     *	@param	len	number of samples to synthesize; see NOTE above! muss durch 2 teilbar sein!
     *	@param	c	filter coefficients as received by getCoeffs
     */
    public static void invTransform( float a[], float s[], float d[], int off, int len, float c[][] )
    {
        int		i, j, k;
        float	cc[]	= c[ 0 ];
        float	cr[]	= c[ 1 ];
        int		nCoeffs	= cc.length;
        int		nOff	= (nCoeffs-1) >> 1;
        int		jStart	= nOff & 1;

        for( k = 0; k < len; k++ ) {
// System.out.print( "a["+k+"] =" );
            a[ k ] = 0.0f;
            // the next line is the result from several hours of brain torture ...
            // ... corresponds to Mallat Theorem 7.18 after Daubechies et al.
            for( j = jStart, i = ((k - j + nOff) >> 1) + off; j < nCoeffs; j += 2, i-- ) {
                a[ k ] += cc[ j ] * s[ i ] + cr[ j ] * d[ i ];
// System.out.print( " + "+cc[ j ]+" * "+s[ i ]+" + "+cr[ j ]+" * "+d[ i ]);
            }
// System.out.println( "" );
            jStart = 1 - jStart;
        }
    }
}
// class Wavelet
