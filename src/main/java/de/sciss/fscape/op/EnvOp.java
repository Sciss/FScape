/*
 *  EnvOp.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.op;

import de.sciss.fscape.gui.FIRDesignerDlg;
import de.sciss.fscape.gui.FilterBox;
import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFileDescr;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;

public class EnvOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Envelope";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_GAIN			= 0;		// pr.para
    private static final int PR_INVERT		= 0;		// pr.bool

    private static final String PRN_GAIN		= "Gain";
    private static final String PRN_INVERT	= "Invert";

    private static final Param	prPara[]		= { null };
    private static final String	prParaName[]	= { PRN_GAIN };
    private static final boolean	prBool[]		= { false };
    private static final String	prBoolName[]	= { PRN_INVERT };

// -------- public methods --------
    // public Container createGUI( int type );

    public EnvOp()
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
            static_pr.para		= prPara;
            static_pr.para[ PR_GAIN ]		= new Param(    0.0, Param.DECIBEL_AMP );
            static_pr.paraName	= prParaName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "EnvOp";
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
        double			d1, d2;

        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot;
        SpectStream		runInStream		= null;
        SpectStream		runOutStream;

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr		= null;

        Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
        float			gain;

        // Ziel-Frame Berechnung
        int				srcBands;
        float[]			fftBuf1, fftBuf2, convBuf1;

        double			fltFreq, fltShift;
        int				fftLength, skip;
        AudioFileDescr		tmpStream;
        FilterBox		fltBox;
        Point			fltLength;

    topLevel:
        try {
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

            srcBands	= runInStream.bands;

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot					= slots.elementAt( SLOT_OUTPUT );
            runOutStream				= new SpectStream( runInStream );
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            gain			= 2 * (float) Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, runInStream ).value;

            fltFreq			= runInStream.smpRate * 0.245;					// account for lp transition band
            fltShift	    = Constants.PI2 * 0.245;						// normalized freq ready for Math.cos/sin
            fltBox			= new FilterBox();
            fltBox.filterType= FilterBox.FLT_LOWPASS;
            fltBox.cutOff	= new Param( fltFreq, Param.ABS_HZ );
            tmpStream		= new AudioFileDescr();	// unflexibel
            tmpStream.rate	= runInStream.smpRate;
            fltLength		= fltBox.calcLength( tmpStream, FIRDesignerDlg.QUAL_VERYGOOD );
            skip			= fltLength.x;								// support not written out
            i				= fltLength.x + fltLength.y;
            j				= i + srcBands - 1;							// length after conv. with input
            for( fftLength = 2; fftLength < j; fftLength <<= 1 ) ;
            fftBuf1			= new float[ fftLength << 1 ];		// *2 = complex
            fftBuf2			= new float[ fftLength << 1 ];
            Util.clear( fftBuf1 );

            // we design a half-nyquist lp filter and shift it up by that freq. to have a real=>analytic filter
            // see comp.dsp algorithms-faq Q2.10
            fltBox.calcIR( tmpStream, FIRDesignerDlg.QUAL_VERYGOOD, Filter.WIN_BLACKMAN, fftBuf1, fltLength );
            // ---- real=>complex + modulation with exp(ismpRate/4 +/- (?) antialias) ----
            for( i = fftLength - 1, j = fftBuf1.length - 1; i >= 0; i-- ) {
                d1				= -fltShift * i;
                fftBuf1[ j-- ]	= (float) (fftBuf1[ i ] * Math.sin( d1 ));		// img
                fftBuf1[ j-- ]	= (float) (fftBuf1[ i ] * Math.cos( d1 ));		// real
            }
            Fourier.complexTransform( fftBuf1, fftLength, Fourier.FORWARD );

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
                    Fourier.polar2Rect( convBuf1, 0, fftBuf2, 0, srcBands << 1 );
                    for( i = srcBands << 1; i < fftBuf2.length; i++ ) {	// zero pad
                        fftBuf2[ i ] = 0.0f;
                    }
                    Fourier.complexTransform( fftBuf2, fftLength, Fourier.FORWARD );
                    Fourier.complexMult( fftBuf1, 0, fftBuf2, 0, fftBuf2, 0, fftLength << 1 );
                    Fourier.complexTransform( fftBuf2, fftLength, Fourier.INVERSE );
                    convBuf1 = runOutFr.data[ ch ];
                    for( i = 0, j = (skip << 1); i < (srcBands << 1); ) {
                        d1				= fftBuf2[ j++ ];
                        d2				= fftBuf2[ j++ ];
                        convBuf1[ i++ ]	= gain * (float) Math.sqrt( d1*d1 + d2*d2 );	// env.amp
                        convBuf1[ i++ ]	= 0.0f;											// zero phase
                    }
                    if( pr.bool[ PR_INVERT ]) {
                        for( i = 0, j = 0; i < srcBands; i++, j += 2 ) {
                            convBuf1[ j ]	= 1.0f - convBuf1[ j ];
                        }
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
                    catch( InterruptedException ignored) {}	// mainLoop wird eh gleich verlassen
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

// -------- GUI methods --------

    public PropertyGUI createGUI( int type )
    {
        PropertyGUI gui;

        if( type != GUI_PREFS ) return null;

        gui = new PropertyGUI(
            "gl"+GroupLabel.NAME_GENERAL+"\ncbInvert,pr"+PRN_INVERT+
            "\nlbGain;pf"+Constants.decibelAmpSpace+",pr"+pr.paraName[ PR_GAIN ]);

        return gui;
    }
}