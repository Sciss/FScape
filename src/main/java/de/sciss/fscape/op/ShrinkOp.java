/*
 *  ShrinkOp.java
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

package de.sciss.fscape.op;

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
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

public class ShrinkOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Shrink";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_FACTOR		= 0;		// pr.intg
    private static final int PR_MODE			= 1;

    private static final String PRN_FACTOR		= "Factor";
    private static final String PRN_MODE			= "Mode";

    protected static final int MODE_SHRINK			= 0;
    protected static final int MODE_EXPAND			= 1;

    private static final int		prIntg[]		= { 0, MODE_SHRINK };
    private static final String	prIntgName[]	= { PRN_FACTOR, PRN_MODE };

// -------- public methods --------
    // public Container createGUI( int type );

    public ShrinkOp()
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
        opName		= "ShrinkOp";
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

        int				fltSmpPerCrossing, fltCrossings, fltLen;
        float			flt[], fltD[], filter[][], fltWin, fltRolloff;
        float			fltGain;

        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot;
        SpectStream		runInStream		= null;
        SpectStream		runOutStream;

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr		= null;

        // Ziel-Frame Berechnung
        int				shrinkFactor;
        int				srcBands, destBands;
        float			rsmpFactor;
        float[]			srcBuf, destBuf, rectBuf;

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

            shrinkFactor= 2 << pr.intg[ PR_FACTOR ];
            srcBands	= runInStream.bands;

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot					= slots.elementAt( SLOT_OUTPUT );
            runOutStream				= new SpectStream( runInStream );
            if( pr.intg[ PR_MODE ] == MODE_SHRINK ) {
                destBands	= (srcBands - 1) / shrinkFactor + 1;
                rsmpFactor	= (float) destBands / (float) srcBands;
                runOutStream.smpPerFrame   /= shrinkFactor;
            } else {
                destBands	= (srcBands - 1) * shrinkFactor + 1;
                rsmpFactor	= (float) destBands / (float) srcBands;
                runOutStream.smpPerFrame   *= shrinkFactor;
            }
            runOutStream.bands			= destBands;
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            fltSmpPerCrossing	= 4096;	// 2048 << quality;
            fltCrossings		= 5;
            fltRolloff			= 0.70f;
            fltWin				= 6.5f;
//			quality				= 1;
            fltSmpPerCrossing	= 4096;
            fltLen				= (int) ((float) (fltSmpPerCrossing * fltCrossings) / fltRolloff + 0.5f);
            flt					= new float[ fltLen ];
            fltD				= null;	// new float[ fltLen ];
            fltGain				= Filter.createAntiAliasFilter( flt, fltD, fltLen, fltSmpPerCrossing, fltRolloff, fltWin );
            filter				= new float[ 3 ][];
            filter[ 0 ]			= flt;
            filter[ 1 ]			= fltD;
            filter[ 2 ]			= new float[ 2 ];
            filter[ 2 ][ 0 ]	= fltSmpPerCrossing;
            filter[ 2 ][ 1 ]	= fltGain;
            rectBuf				= new float[ Math.max( srcBands, destBands ) << 1 ];
            srcBuf				= new float[ srcBands + 1 ];
            destBuf				= new float[ destBands + 1 ];

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
                    Fourier.polar2Rect( runInFr.data[ ch ], 0, rectBuf, 0, srcBands << 1 );
                    // 1. real part
                    for( i = 0, j = 0; i < srcBands; i++, j += 2 ) {
                        srcBuf[ i ] = rectBuf[ j ];
                    }
                    Filter.resample( srcBuf, 0.0, destBuf, 0, destBands, rsmpFactor, filter );
                    for( i = 0, j = 0; i < destBands; i++, j += 2 ) {
                        rectBuf[ j ] = destBuf[ i ];
                    }
                    // 1. img part
                    for( i = 0, j = 1; i < srcBands; i++, j += 2 ) {
                        srcBuf[ i ] = rectBuf[ j ];
                    }
                    Filter.resample( srcBuf, 0.0, destBuf, 0, destBands, rsmpFactor, filter );
                    for( i = 0, j = 1; i < destBands; i++, j += 2 ) {
                        rectBuf[ j ] = destBuf[ i ];
                    }
                    Fourier.rect2Polar( rectBuf, 0, runOutFr.data[ ch ], 0, destBands << 1 );
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

        gui = new PropertyGUI(
            "gl"+GroupLabel.NAME_GENERAL+"\n" +
            "lbMode;ch,pr"+PRN_MODE+",itShrink,itExpand\n" +
            "lbFactor;ch,pr"+PRN_FACTOR+"," +
                "it1:2,it1:4,it1:8,it1:16" );

        return gui;
    }
}