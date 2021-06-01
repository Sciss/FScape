/*
 *  ZoomOp.java
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
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

public class ZoomOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Zoom";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_FACTOR		= 0;		// pr.intg
    private static final int PR_BAND			= 0;		// pr.para

    private static final String PRN_FACTOR		= "Factor";
    private static final String PRN_BAND			= "Band";

    private static final int		prIntg[]		= { 0 };
    private static final String	prIntgName[]	= { PRN_FACTOR };
    private static final Param	prPara[]		= { null };
    private static final String	prParaName[]	= { PRN_BAND };

// -------- public methods --------
    // public Container createGUI( int type );

    public ZoomOp()
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
            static_pr.para[ PR_BAND ]			= new Param( 16000.0, Param.ABS_HZ );
            static_pr.paraName	= prParaName;

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "ZoomOp";
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
        int		ch;

        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot;
        SpectStream		runInStream		= null;
        SpectStream		runOutStream;

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr		= null;

        // Ziel-Frame Berechnung
        int		zoomFactor;
        int		srcBands, destBands;
        int		startBand;

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

            zoomFactor	= 2 << pr.intg[ PR_FACTOR ];
            srcBands	= runInStream.bands;
            destBands	= (srcBands - 1) / zoomFactor + 1;

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot					= slots.elementAt( SLOT_OUTPUT );
            runOutStream				= new SpectStream( runInStream );
            runOutStream.bands			= destBands;
            runOutStream.smpPerFrame   /= zoomFactor;
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            // src-band into which specified freq. falls
            startBand	= (int) (pr.para[ PR_BAND ].value / ((runInStream.hiFreq - runInStream.loFreq) / srcBands) + 0.5);
            startBand	= startBand - (destBands >> 1);
            // limit to available srcbands
            if( startBand < 0 ) {
                startBand = 0;
            } else if( (startBand + destBands) > srcBands ) {
                startBand = srcBands - destBands;
            }

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
                    System.arraycopy( runInFr.data[ ch ], startBand << 1, runOutFr.data[ ch ], 0, destBands << 1 );
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
            "gl"+GroupLabel.NAME_GENERAL+"\n" +
            "lbZoom factor;ch,pr"+PRN_FACTOR+"," +
                "it1:2,it1:4,it1:8,it1:16\n" +
            "lbZoom to bands around;pf"+Constants.absHzSpace+",id1,pr"+PRN_BAND );

        return gui;
    }
}