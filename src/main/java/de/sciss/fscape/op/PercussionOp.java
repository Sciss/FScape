/*
 *  PercussionOp.java
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
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

public class PercussionOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Percussion";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_CRR			= 0;		// pr.intg
    private static final int PR_CRI			= 1;
    private static final int PR_CLR			= 2;
    private static final int PR_CLI			= 3;
    private static final int PR_CCR			= 4;
    private static final int PR_CCI			= 5;
    private static final int PR_CAR			= 6;
    private static final int PR_CAI			= 7;

    private static final String PRN_CRR			= "CRR";
    private static final String PRN_CRI			= "CRI";
    private static final String PRN_CLR			= "CLR";
    private static final String PRN_CLI			= "CLI";
    private static final String PRN_CCR			= "CCR";
    private static final String PRN_CCI			= "CCI";
    private static final String PRN_CAR			= "CAR";
    private static final String PRN_CAI			= "CAI";

    private static final int		prIntg[]		= { 2, 2, 1, 1, 2, 0, 1, 1 };
    private static final String	prIntgName[]	= { PRN_CRR, PRN_CRI, PRN_CLR, PRN_CLI,
                                                        PRN_CCR, PRN_CCI, PRN_CAR, PRN_CAI };

// -------- public methods --------
    // public Container createGUI( int type );

    public PercussionOp()
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

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "PercussionOp";
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
        int				srcBands, fftSize, fullFFTsize, complexFFTsize;
        float[]			fftBuf, convBuf1, convBuf2;

        int				clr, cli, crr, cri, ccr, cci, car, cai;

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

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot					= slots.elementAt( SLOT_OUTPUT );
            runOutStream				= new SpectStream( runInStream );
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            srcBands	= runInStream.bands;
            fftSize		= srcBands - 1;
            fullFFTsize	= fftSize << 1;
            complexFFTsize = fullFFTsize << 1;
            fftBuf		= new float[ complexFFTsize ];

            crr = pr.intg[ PR_CRR ] - 1;
            cri = pr.intg[ PR_CRI ] - 1;
            clr = pr.intg[ PR_CLR ] - 1;
            cli = pr.intg[ PR_CLI ] - 1;
            ccr = pr.intg[ PR_CCR ] - 1;
            cci = pr.intg[ PR_CCI ] - 1;
            car = pr.intg[ PR_CAR ] - 1;
            cai = pr.intg[ PR_CAI ] - 1;

            // ------------------------------ Hauptschleife ------------------------------
            runSlotsReady();
        mainLoop:
            while( !threadDead ) {
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
                    convBuf1	= runInFr.data[ ch ];
                    convBuf2	= runOutFr.data[ ch ];

                    // calculate complex log spectrum :
                    // Re( target ) = Log( Mag( source ))
                    // Im( target ) = Phase( source )
                    // convBuf1 is already in polar style
                    // -> fftBuf will be in rect style

                    for( i = 0; i <= fullFFTsize; ) {
                        fftBuf[ i ] = (float) Math.log( Math.max( 1.0e-24, convBuf1[ i ]));		// Re( target )
                        i++;
                        fftBuf[ i ] = convBuf1[ i ];		// Im( target )
                        i++;
                    }

                    // make full spectrum (negative frequencies = conjugate positive frequencies)
                    for( i = fullFFTsize + 2, j = fullFFTsize - 2; i < complexFFTsize; j -= 2 ) {
                                            // bug but nice?  - 1
                        fftBuf[ i++ ] = fftBuf[ j ];
                        fftBuf[ i++ ] = -fftBuf[ j+1 ];
                    }
                    // cepstrum domain
                    Fourier.complexTransform( fftBuf, fullFFTsize, Fourier.INVERSE );
                    // fold cepstrum (make anticausal parts causal)
                    fftBuf[ 0 ] *= crr;
                    fftBuf[ 1 ] *= cri;
                    for( i = 2, j = complexFFTsize - 2; i < fullFFTsize; i += 2, j -= 2 ) {
                        f1				= fftBuf[ i ];
                        f2				= fftBuf[ j ];
                        fftBuf[ i ]		= crr * f1 + ccr * f2;
                        fftBuf[ j ]		= clr * f2 + car * f1;
                        f1				= fftBuf[ i+1 ];
                        f2				= fftBuf[ j+1 ];
                        fftBuf[ i+1 ]	= cri * f1 + cci * f2;
                        fftBuf[ j+1 ]	= cli * f2 + cai * f1;

//						fftBuf[ i ]   += causal[ k ] * fftBuf[ j ];		// add conjugate left wing to right wing
//						fftBuf[ j ]   *= anticausal[ k ];
//						fftBuf[ i+1 ] -= causal[ k ] * fftBuf[ j+1 ];
//						fftBuf[ j+1 ] *= anticausal[ k ];

//						fftBuf[ i ]   *= anticausal[ k ];
//						fftBuf[ j ]  += causal[ k ] * fftBuf[ i ];		// add conjugate left wing to right wing
//						fftBuf[ i+1 ] *= anticausal[ k ];
//						fftBuf[ j+1 ] -= causal[ k ] * fftBuf[ i+1 ];
//						i+=2;
                    }
                    fftBuf[ i++ ] *= ccr + clr;
                    fftBuf[ i++ ] *= cci + cli;

//					fftBuf[ i++ ] *= causal[ k ];
//					fftBuf[ i++ ] *= -causal[ k ];

//					fftBuf[ j ]   *= causal[ k ];
//					fftBuf[ j+1 ] *= -causal[ k ];

                    // back to frequency domain
                    Fourier.complexTransform( fftBuf, fullFFTsize, Fourier.FORWARD );

                    // calculate real exponential spectrum :
                    // Mag( target ) = Exp( Re( source ))
                    // Phase( target ) = Im( source )
                    // ->convBuf2 shall be polar style, that makes things easy

                    for( i = 0; i <= fullFFTsize; ) {
                        convBuf2[ i ] = (float) Math.exp( fftBuf[ i ]);
                        i++;
                        convBuf2[ i ] = fftBuf[ i ];
                        i++;
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

    public PropertyGUI createGUI(int type) {
        PropertyGUI gui;

        if (type != GUI_PREFS) return null;

        String coeff = ",it -1,it  0,it +1\n";

        gui = new PropertyGUI(
            "glCoefficients\n" +
            "lbRight Wing Real;ch,pr"+PRN_CRR+coeff+
            "lbRight Wing Imag;ch,pr"+PRN_CRI+coeff+
            "lbLeft Wing Real;ch,pr"+PRN_CLR+coeff+
            "lbLeft Wing Imag;ch,pr"+PRN_CLI+coeff+
            "lbCausal Real;ch,pr"+PRN_CCR+coeff+
            "lbCausal Imag;ch,pr"+PRN_CCI+coeff+
            "lbAnticausal Real;ch,pr"+PRN_CAR+coeff+
            "lbAnticausal Imag;ch,pr"+PRN_CAI+coeff );

        return gui;
    }
}