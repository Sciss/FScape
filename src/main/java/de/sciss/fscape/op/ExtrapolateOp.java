/*
 *  ExtrapolateOp.java
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

package de.sciss.fscape.op;

import de.sciss.fscape.gui.FIRDesignerDlg;
import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

public class ExtrapolateOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Extrapolate";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_LOFREQ		= 0;		// pr.para
    private static final int PR_HIFREQ		= 1;
//	private static final int PR_QUALITY		= 0;		// pr.intg

    private static final String PRN_HIFREQ		= "HiFreq";
    private static final String PRN_LOFREQ		= "LoFreq";
    private static final String PRN_QUALITY		= "Quality";

    private static final int		prIntg[]		= { FIRDesignerDlg.QUAL_MEDIUM };
    private static final String	prIntgName[]	= { PRN_QUALITY };
    private static final Param	prPara[]		= { null, null };
    private static final String	prParaName[]	= { PRN_LOFREQ, PRN_HIFREQ };

    protected static final String ERR_BANDS			= "Band# not power of 2";

// -------- public methods --------
    // public Container createGUI( int type );

    public ExtrapolateOp()
    {
        super();

        // initialize only in the first instance
        // preferences laden
        if( static_prefs == null ) {
            static_prefs = new OpPrefs( getClass(), getDefaultPrefs() );
        }
        // propertyarray defaults
        if( static_pr == null ) {
            static_pr = new PropertyArray();

            static_pr.intg		= prIntg;
            static_pr.intgName	= prIntgName;
            static_pr.para		= prPara;
            static_pr.para[ PR_HIFREQ ]			= new Param(   440.0, Param.ABS_HZ );
            static_pr.para[ PR_LOFREQ ]			= new Param(     0.0, Param.ABS_HZ );
            static_pr.paraName	= prParaName;

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "ExtrapolateOp";
        prefs		= static_prefs;
        presets		= static_presets;
        pr			= (PropertyArray) static_pr.clone();

        // slots
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

        // icon						// XXX
        icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
    }

// -------- Runnable methods --------

    public void run()
    {
        runInit();		// superclass

        // Haupt-Variablen fuer den Prozess
        int				ch, i, j;
        float			f1, f2;

        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot;
        SpectStream		runInStream		= null;
        SpectStream		runOutStream;

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr		= null;

        // Ziel-Frame Berechnung
        int				srcBands, lpCoeffNum;
        float			loFreq, hiFreq, freqSpacing;
        float[]			lpCoeffBuf, convBuf1, convBuf2, convBuf3;
        float[][]		lpBuf;
        int				loBand, hiBand;

topLevel:
        try {

// for( i = 0; i < 8; i++ ) {
// 	test[ i ] = (rnd.nextFloat() * 4.0f) - 2.0f;
//	System.out.println( "o"+i+" = "+test[i] );
// }

            // ------------------------------ Input-Slot ------------------------------
            runInSlot = slots.elementAt( SLOT_INPUT );
            if( runInSlot.getLinked() == null ) {
                runStop();	// threadDead = true -> folgendes for() wird uebersprungen
            }
            // diese while Schleife ist noetig, da beim initReader ein Pause eingelegt werden kann
            // und die InterruptException ausgeloest wird; danach versuchen wir es erneut
            for( boolean initDone = false; !initDone && !threadDead; ) {
                try {
                    runInStream	= runInSlot.getDescr();	// throws InterruptedException
                    initDone = true;
                }
                catch( InterruptedException ignored) {}
                runCheckPause();
            }
            if( threadDead ) break topLevel;

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot					= slots.elementAt( SLOT_OUTPUT );
            runOutStream				= new SpectStream( runInStream );
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            freqSpacing	= (runInStream.hiFreq - runInStream.loFreq) / runInStream.bands;
            srcBands	= runInStream.bands;
//			srcSize		= srcBands << 1;
            lpBuf		= new float[ 2 ][ srcBands ];
            loFreq		= Math.min( runInStream.smpRate / 2, (float) pr.para[ PR_LOFREQ ].value);
            hiFreq		= Math.max( 0.5f, Math.min( runInStream.smpRate / 2, (float) pr.para[ PR_HIFREQ ].value));
            if( loFreq > hiFreq ) {
                f1		= loFreq;
                loFreq	= hiFreq;
                hiFreq	= f1;
            }
            loBand		= (int) ((loFreq - runInStream.loFreq) / freqSpacing + 0.5f);
            hiBand		= (int) ((hiFreq - runInStream.loFreq) / freqSpacing + 0.5f);
            lpCoeffNum	= Math.min( 64, hiBand - loBand );
            lpCoeffBuf	= new float[ lpCoeffNum ];

// System.out.println( "SrcSize "+srcSize+"; loBand "+loBand+"; hiBand "+hiBand+"; lpCoeffNum "+lpCoeffNum );

            // ------------------------------ Hauptschleife ------------------------------
            runSlotsReady();
mainLoop:	while( !threadDead ) {
            // ---------- Frame einlesen ----------
                for( boolean readDone = false; (!readDone) && !threadDead; ) {
                    try {
                        runInFr		= runInSlot.readFrame();	// throws InterruptedException
                        readDone	= true;
                        runOutFr	= runOutStream.allocFrame();
                    }
                    catch( InterruptedException ignored) {}
                    catch( EOFException e ) {
                        break mainLoop;
                    }
                    runCheckPause();
                }
                if( threadDead ) break mainLoop;

            // ---------- Process: Ziel-Frame berechnen ----------


                for( ch = 0; ch < runOutStream.chanNum; ch++ ) {

                    convBuf1 = runInFr.data[ ch ];
                    convBuf2 = lpBuf[ 0 ];
                    convBuf3 = lpBuf[ 1 ];

//					for( i = loBand << 1, j = loBand; j < hiBand; ) {
                    for( i = 0, j = 0; j < srcBands; ) {
                        f2				= convBuf1[ i++ ];		// amp
                        f1				= convBuf1[ i++ ];		// phase
                        convBuf2[ j ]	= f2 * (float) Math.cos( f1 );	// real
                        convBuf3[ j++ ]	= f2 * (float) Math.sin( f1 );	// imag
                    }

                    for( i = 0; i < 2; i++ ) {
                        convBuf1 = lpBuf[ i ];
                        lpCoeffs( convBuf1, loBand, hiBand - loBand, lpCoeffBuf, lpCoeffNum );
//						fixRoots( lpCoeffBuf, lpCoeffNum );
                        linearPrediction2( convBuf1, loBand, hiBand - loBand, lpCoeffBuf, lpCoeffNum,
                                          convBuf1, hiBand, srcBands - hiBand );
                        // XXX lo freq reverse lp
                    }

                    convBuf1 = runOutFr.data[ ch ];
                    convBuf2 = lpBuf[ 0 ];
                    convBuf3 = lpBuf[ 1 ];

                    for( i = 0, j = 0; j < srcBands; ) {
                        f1				= convBuf2[ j ];		// real
                        f2				= convBuf3[ j++ ];		// imag
                        convBuf1[ i++ ]	= complexAbs( f1, f2 );	// amp
                        convBuf1[ i++ ]	= (float) Math.atan2( f2, f1 );	// phase
                    }
                }
                // calculation done

                runInSlot.freeFrame( runInFr );

                for( boolean writeDone = false; (!writeDone) && !threadDead; ) {
                    try {	// Unterbrechung
                        runOutSlot.writeFrame( runOutFr );	// throws InterruptedException
                        writeDone = true;
                        runFrameDone( runOutSlot, runOutFr  );
                        runOutStream.freeFrame( runOutFr );
                    }
                    catch( InterruptedException e ) {}	// mainLoop wird eh gleich verlassen
                    runCheckPause();
                }
            } // end of main loop

            runInStream.closeReader();
            runOutStream.closeWriter();

        } // break topLevel

        catch( IOException e ) {
            runQuit( e );
            return;
        }
        catch( SlotAlreadyConnectedException e ) {
            runQuit( e );
            return;
        }
//		catch( OutOfMemoryError e ) {
//			abort( e );
//			return;
//		}

        runQuit( null );
    }

    // ---- predic routine from NR paragraph 13.6 ----

    /*
     *	Given data[dataOff...dataOff+dataLen-1] (!!), and given the data's LP coefficients d[0...m-1] (!!), this routine applies
     *	equation (13.6.11) to predict the next nfut data points, which it returns in the array
     *	future[futOff...futOff+futLen-1] (!!). Note that the routine references only the last m values of data, as initial
     *	values for the prediction.
     */
//	public static void predic(float data[], int ndata, float d[], int m, float future[], int nfut)
    public static void linearPrediction( float[] data, int dataOff, int dataLen, float[] coeffBuf, int coeffNum,
                                         float[] future, int futOff, int futLen )
    {
        int		i, j, k;
        float	sum;	// discrp;
        float[]	reg	= new float[ coeffNum ];

        for( j = 0, k = dataOff + dataLen; j < coeffNum; ) {
            reg[ j++ ] = data[ --k ];
        }

        for( j = futOff, i = futOff + futLen; j < i; j++ ) {
//			discrp	= 0.0f;
            // This is where you would put in a known discrepancy if you were reconstructing a
            // function by linear predictive coding rather than extrapolating a function by linear prediction.
            // See text.
            sum		= 0.0f; // discrp;
            for( k = 0; k < coeffNum; k++ ) {
                sum += coeffBuf[k] * reg[k];
            }
            System.arraycopy( reg, 0, reg, 1, coeffNum-1 ); // [If you want to implement circular arrays, you can avoid this shifting of coefficients.]
            reg[0]		= sum;
            future[j]	= sum;
        }
    }

    public static void linearPrediction2( float[] data, int dataOff, int dataLen, float[] coeffBuf, int coeffNum,
                                         float[] future, int futOff, int futLen )
    {
        int		i, j, k;
        float	sum;	// discrp;
        float[]	reg	= new float[ coeffNum ];
        float	f1	= 1.0f;

        for( j = 0, k = dataOff + dataLen; j < coeffNum; ) {
            reg[ j++ ] = data[ --k ];
        }

        for( j = futOff, i = futOff + futLen; j < i; j++ ) {
//			discrp	= 0.0f;
            // This is where you would put in a known discrepancy if you were reconstructing a
            // function by linear predictive coding rather than extrapolating a function by linear prediction.
            // See text.
            sum		= 0.0f; // discrp;
            for( k = 0; k < coeffNum; k++ ) {
                sum += coeffBuf[k] * reg[k];
            }
            System.arraycopy( reg, 0, reg, 1, coeffNum-1 ); // [If you want to implement circular arrays, you can avoid this shifting of coefficients.]
            f1		    = Math.min( 16f, f1 * 0.99f + 0.01f * Math.abs( future[j] ) / Math.max( 1.0e-6f, Math.abs( sum )));
            future[j]	= f1 * sum;
            reg[0]		= f1 * sum;
        }
    }

    // ---- memcof routine from NR paragraph 13.6 ----

    /*
     *	Given a real vector of data[dataOff...dataOff+dataLen-1] (!!), and given m, this routine returns m linear prediction
     *	coefficients as coeffBuf[0...m-1] (!!), and returns the mean square discrepancy as xms.
     */
//	public static void memcof( float data[], int n, int m, float *xms, float d[])
    protected static float lpCoeffs( float[] data, int dataOff, int dataLen, float[] coeffBuf, int coeffNum )
    {
        int		i, j, k;
        float	f;
        float[] wk1	= new float[ dataLen-1 ];
        float[] wk2	= new float[ dataLen-1 ];
        float[]	wkm	= new float[ coeffNum-1 ];
        float	xms, num, denom;

        for( j = dataLen, i = dataLen + dataOff, f = 0.0f; j < i; j++ ) {
            f += data[j]*data[j];
        }
        xms = f / dataLen;

        wk1[0]			= data[0];
        wk2[dataLen-2]	= data[dataLen-1];

        System.arraycopy( data, dataOff,   wk1, 0, dataLen-1 );
        System.arraycopy( data, dataOff+1, wk2, 0, dataLen-1 );

        for( k = 0;; ) {
            num		= 0.0f;
            denom	= 0.0f;

            for( j = 0, i = dataLen - k - 1; j < i; j++ ) {
                num		+= wk1[j]*wk2[j];
                denom	+= wk1[j]*wk1[j] + wk2[j]*wk2[j];
            }
            if( denom > 0.0f ) {
                f			= 2.0f * num / denom;
            } else {
                f			= 1.0f;
            }
            coeffBuf[k]		= f;
            xms	  		   *= 1.0f - f*f;
            for( i = 0; i < k; i++ ) {
                coeffBuf[i]	= wkm[i] - f * wkm[k-i-1];
            }
            // The algorithm is recursive, building up the answer for larger and larger values of m
            // until the desired value is reached. At this point in the algorithm, one could return
            // the vector d and scalar xms for a set of LP coefficients with k (rather than m) terms.
            if( ++k == coeffNum ) return xms;		// !! k increased here for efficiency
 
            System.arraycopy( coeffBuf, 0, wkm, 0, k );

            for( j = 0, i = dataLen - k - 1; j < i; j++ ) {
                wk1[j]	-= f * wk2[j];
                wk2[j]	 = wk2[j+1] - f * wk1[j+1];
            }
        }
    }

    // ---- fixrts routine from NR paragraph 13.6 ----

    /*
     *	Given the LP coefficients d[0...m-1] (!!), this routine finds all roots of the characteristic polynomial
     *	(13.6.14), reflects any roots that are outside the unit circle back inside, and then returns a
     *	modified set of coefficients d[0...m-1].
     */
    protected static void fixRoots( float coeffBuf[], int coeffNum )
    {
        int		i, j, coeffNum2;
        float	rootsRe, rootsIm, f1;

        coeffNum2 = coeffNum << 1;

        float[]	a		= new float[ coeffNum2 + 2 ];
        float[] roots	= new float[ coeffNum2 ];
        float[] cmplxRes= new float[ 2 ];

        a[ coeffNum2 ]		= 1.0f;
        a[ coeffNum2+1 ]	= 0.0f;
        for( i = 0, j = coeffNum2; j > 0; ) { // Set up complex coefficients for polynomial root finder.
            a[ --j ]	= 0.0f;
            a[ --j ]	= -coeffBuf[ i++ ];
        }

        zRoots( a, coeffNum, roots, true );	// Find all the roots.

        for( j = 0; j < coeffNum2; j += 2 ) {	// Look for a...
            f1 = complexAbs( roots[j], roots[j+1] );
            if( f1 > 1.0f ) {	// 1.0f	// root outside the unit circle,
                // pull back on unit circle
//				roots[j]   *= 0.0f / f1;
//				roots[j+1] *= 0.0f / f1;

                // alternative: reflection
                complexDiv( 1.0f, 0.0f, roots[ j ], -roots[ j+1 ], cmplxRes );
                roots[ j ]		= cmplxRes[0];
                roots[ j+1 ]	= cmplxRes[1];
            }
        }

        a[0]	= -roots[0];
        a[1]	= -roots[1];
        a[2]	= 1.0f;
        a[3]	= 0.0f;		// Now reconstruct the polynomial coefficients,
        for( j = 2; j < coeffNum2; j += 2 ) {	// by looping over the roots
            a[ j+2 ]	= 1.0f;
            a[ j+3 ]	= 0.0f;
            rootsRe		= roots[j];
            rootsIm		= roots[j+1];
            for( i = j; i >= 2; i -= 2 ) {	// and synthetically multiplying.
                a[i]	= a[i-2] - (rootsRe * a[i] - rootsIm * a[i+1]);
                a[i+1]	= a[i-1] - (rootsIm * a[i] + rootsRe * a[i+1]);
            }
            a[0]	= -rootsRe * a[0] + rootsIm * a[1];
            a[1]	= -rootsIm * a[0] - rootsRe * a[1];
        }

        for( i = coeffNum, j = 0; j < coeffNum2; j += 2 ) {	// The polynomial coefficients are guaranteed to be real, so we need only return the real part as new LP coefficients.
            coeffBuf[ --i ] = -a[j];
        }
    }

    // ---- zroots routine from NR paragraph 9.5 ----

    protected static final float	EXPECTEDERROR2	= 4.0e-6f;

    /*
     * Given the degree m and the m+1 complex coefficients a[0...m*2] of the polynomial (?) i=0...m(a[i]x^i),
     * this routine successively calls laguer and finds all m complex roots in roots[0...(m-1)*2] (!!). The
     * boolean variable polish should be input as true if polishing (also by Laguerre's method)
     * is desired, false if the roots will be subsequently polished by other means.
     */
    protected static void zRoots( float[] coeffBuf, int coeffNum, float[] roots, boolean polish )
    {
        int		i, j, jj;
        float	bRe,bIm, cRe,cIm;
        int		coeffNum2	= coeffNum << 1;
        float[]	ad	= new float[ coeffNum2 + 2 ];
        float[]	x	= new float[ 2 ];

        System.arraycopy( coeffBuf, 0, ad, 0, coeffNum2 + 2 );	// Copy of coefficients for successive deflation.

        for( j = coeffNum2; j >= 2; j -= 2 ) {		// Loop over each root to be found.
            jj = j - 2;
            // Start at zero to favor convergence to smallest remaining root, and find the root.
            x[0]	= 0.0f;
            x[1]	= 0.0f;
            /* i = */ laguerre( ad, j >> 1, x );

//			if( i == 0 ) {
//System.out.println( "retrying complex" );
//				x[0]	= 0.0f;
//				x[1]	= 0.5f;		// try complex
//				laguerre( ad, j >> 1, x );
//			}
            if( Math.abs( x[1] ) <= EXPECTEDERROR2 * Math.abs( x[0] )) {
                x[1] = 0.0f;
            }
            roots[ jj ]		= x[0];
            roots[ jj+1 ]	= x[1];

            bRe				= ad[ j ];
            bIm				= ad[ j + 1 ];	// Forward deflation.
            for( ; jj >= 0; jj -= 2 ) {
                cRe			= ad[ jj ];
                cIm			= ad[ jj+1 ];
                ad[ jj ]	= bRe;
                ad[ jj+1 ]	= bIm;
                bRe			= x[0] * bRe - x[1] * bIm + cRe;
                bIm			= x[1] * bRe + x[0] * bIm + cIm;
            }
        }

        if( polish ) {
            for( j = 0; j < coeffNum2; ) {	// Polish the roots using the undeflated coefficients.
                x[0]		= roots[ j ];
                x[1]		= roots[ j+1 ];
                laguerre( coeffBuf, coeffNum, x );
                roots[j++]	= x[0];
                roots[j++]	= x[1];
            }
        }

        for( j = 2; j < coeffNum2; j += 2 ) {		// Sort roots by their real parts by straight insertion.
            x[0]	= roots[ j ];
            x[1]	= roots[ j+1 ];
            for( i = j - 2; i >= 2; i -= 2 ) {
                if( roots[ i ] <= x[0] ) break;
                roots[ i+2 ]	= roots[ i ];
                roots[ i+3 ]	= roots[ i+1 ];
            }
            roots[ i+2 ]	= x[0];
            roots[ i+3 ]	= x[1];
        }

//System.out.println( "roots:" );
//for( j = 0; j < coeffNum; j++ ) {
//	System.out.println( roots[j<<1]+" + "+roots[(j<<1)+1]+"i" );
//}
    }

    // ---- laguer routine from NR paragraph 9.5 ----

    protected static final float	EXPECTEDERROR 	= 1.0e-7f;
    protected static final int		MR 				= 8;
    protected static final int		MT				= 10; // 10;
    protected static final int		MAXITER			= (MT*MR);
    protected static final float[]	frac			= { 0.0f, 0.5f, 0.25f, 0.75f, 0.13f, 0.38f, 0.62f, 0.88f, 1.0f }; // Fractions used to break a limit cycle. [MR+1]

    /*
     * Given the degree m and the m+1 complex coefficients a[0..m] of the polynomial (?) i=0...m(a[i]x^i),
     * and given a complex value x, this routine improves x by Laguerre's method until it converges,
     * within the achievable roundoff limit, to a root of the given polynomial. The number of iterations
     * taken is returned as its.
     *
     * Here EXPECTEDERROR is the estimated fractional roundoff error. We try to break (rare) limit cycles with
     * MR different fractional values, once every MT steps, for MAXITER total allowed iterations.
     *
     *	WARNING: dispite complex 'x' sucky algorithm doesn't work for complex roots!!!!
     *
     *	@return		number of iterations performed or 0 if not converged (complex root)
     */
//	protected int laguer( fcomplex a[], int m, fcomplex *x )
    protected static int laguerre( float[] coeffBuf, int coeffNum, float[] x )
    {
        int		iter, j, coeffNum2, fooInt;
        float	absX, absP, absM, err, absB;
        float	bRe,bIm, fRe,fIm, dRe,dIm, g2Re,g2Im, gRe,gIm, gmRe, gmIm;
        float	gpRe,gpIm, sqRe,sqIm, hRe,hIm, x1Re,x1Im, dxRe,dxIm, fooFloat;
        float[] cmplxRes = new float[ 2 ];	// return values from complexDiv and complexSqrt

        coeffNum2	= coeffNum << 1;

        for( iter = 1; iter <= MAXITER; iter++ ) {	// Loop over iterations up to allowed maximum.

            bRe		= coeffBuf[ coeffNum2 ];
            bIm		= coeffBuf[ coeffNum2 + 1 ];
            absB	= complexAbs( bRe, bIm );
            err		= absB;
            fRe		= 0.0f;
            fIm		= 0.0f;
            dRe		= 0.0f;
            dIm		= 0.0f;
            absX	= complexAbs( x[0], x[1] );

            for( j = coeffNum2; j > 0; ) {			// Effcient computation of the polynomial and its first two derivatives. f=Cadd(Cmul(*x,f),d);

                dRe		= x[0] * dRe - x[1] * dIm + bRe;
                dIm		= x[1] * dRe + x[0] * dIm + bIm;
                bIm		= x[1] * bRe + x[0] * bIm + coeffBuf[ --j ];
                bRe		= x[0] * bRe - x[1] * bIm + coeffBuf[ --j ];
                absB	= complexAbs( bRe, bIm );
                err		= absB + absX * err;
            }

            err	   *= EXPECTEDERROR;				// Estimate of roundoff error in evaluating polynomial.
            if( absB <= err ) return iter;			// We are on the root.

            complexDiv( dRe, dIm, bRe, bIm, cmplxRes );	// The generic case: use Laguerre's formula.
            gRe		= cmplxRes[0];
            gIm		= cmplxRes[1];
            g2Re	= gRe * gRe - gIm * gIm;
            g2Im	= gIm * gRe * 2;
            complexDiv( fRe, fIm, bRe, bIm, cmplxRes );
            hRe		= g2Re - 2.0f * cmplxRes[0];
            hIm		= g2Im - 2.0f * cmplxRes[1];

            sqRe	= (float) (coeffNum - 1) * ((float) coeffNum * hRe - g2Re);
            sqIm	= (float) (coeffNum - 1) * ((float) coeffNum * hIm - g2Im);
            complexSqrt( sqRe, sqIm, cmplxRes );
            sqRe	= cmplxRes[0];
            sqIm	= cmplxRes[1];
            gpRe	= gRe + sqRe;
            gpIm	= gIm + sqIm;
            gmRe	= gRe - sqRe;
            gmIm	= gIm - sqIm;
            absP	= complexAbs( gpRe, gpIm );
            absM	= complexAbs( gmRe, gmIm );

            if( absP < absM ) {
                gpRe = gmRe;
                gpIm = gmIm;
            }
            if( (absP > 0.0f) || (absM > 0.0f) ) {
                complexDiv( (float) coeffNum, 0.0f, gpRe, gpIm, cmplxRes );
                dxRe	= cmplxRes[0];
                dxIm	= cmplxRes[1];
            } else {
                fooFloat= (1.0f + absX);
                dxRe	= fooFloat * (float) Math.cos( (float) iter );
                dxIm	= fooFloat * (float) Math.sin( (float) iter );
            }
            x1Re	= x[0] - dxRe;
            x1Im	= x[1] - dxIm;

            if( (dxRe == 0.0f) && (dxIm == 0.0f) ) return iter; // Converged.
            if( (iter % MT) != 0 ) {
                x[0]	= x1Re;
                x[1]	= x1Im;
            } else {
                fooInt  = iter / MT;
                x[0]   -= frac[ fooInt ] * dxRe;
                x[1]   -= frac[ fooInt ] * dxIm;	// Every so often we take a fractional step, to break any limit cycle (itself a rare occurrence).
            }
        }
//		System.out.println( "too many iterations in laguerre()" );	// Very unusual | can occur only for complex roots. Try a different starting guess for the root.
// XXX

        return 0;
    }

    // from NR Appendix C
    protected static float complexAbs( float re, float im )
    {
        if( re == 0.0f ) return Math.abs( im );
        if( im == 0.0f ) return Math.abs( re );
        return (float) Math.sqrt( re*re + im*im );
    }

    // from NR Appendix C
    protected static void complexDiv( float aRe, float aIm, float bRe, float bIm, float[] result )
    {
        float r, den;

        if( Math.abs( bRe ) >= Math.abs( bIm )) {
            r			= bIm / bRe;
            den 		= bRe + r * bIm;
            result[0]	= (aRe + r * aIm) / den;
            result[1]	= (aIm - r * aRe) / den;
        } else {
            r			= bRe / bIm;
            den			= bIm + r * bRe;
            result[0]	= (aRe * r + aIm) / den;
            result[1]	= (aIm * r - aRe) / den;
        }
    }

    // from NR Appendix C
    protected static void complexSqrt( float Re, float Im, float[] result )
    {
        float absRe, absIm, w, r;

        if( (Re == 0.0f) && (Im == 0.0f)) {
            result[0] = 0.0f;
            result[1] = 0.0f;
        } else {
            absRe = Math.abs( Re );
            absIm = Math.abs( Im );
            if( absRe >= absIm ) {
                r = absIm / absRe;
                w = (float) (Math.sqrt( absRe ) * Math.sqrt( 0.5f * (1.0f + Math.sqrt( 1.0f + r*r ))));
            } else {
                r = absRe / absIm;
                w = (float) (Math.sqrt( absIm ) * Math.sqrt( 0.5f * (r + Math.sqrt( 1.0f + r*r ))));
            }
            if( Re >= 0.0 ) {
                result[0] = w;
                result[1] = Im / (2.0f * w);
            } else {
                result[1] = (Im >= 0.0f) ? w : -w;
                result[0] = Im / (2.0f * result[1] );
            }
        }
    }

// -------- GUI methods --------

    public PropertyGUI createGUI(int type) {
        PropertyGUI gui;
        StringBuffer qualJComboBox = new StringBuffer();

        if (type != GUI_PREFS) return null;

        for (int i = 0; i < FIRDesignerDlg.QUAL_NAMES.length; i++) {
            qualJComboBox.append(",it");
            qualJComboBox.append(FIRDesignerDlg.QUAL_NAMES[i]);
        }

        gui = new PropertyGUI(
                "gl" + GroupLabel.NAME_GENERAL + "\n" +
                        "lbHigh frequency;pf" + Constants.absHzSpace + ",pr" + PRN_HIFREQ + "\n" +
                        "lbLow frequency;pf" + Constants.absHzSpace + ",pr" + PRN_LOFREQ + "\n" +
                        "lbQuality;ch,pr" + PRN_QUALITY + qualJComboBox.toString() + "\n");

        return gui;
    }
}