/*
 *  Mono2StereoOp.java
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

public class Mono2StereoOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Mono\u2192Stereo";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;

    // Properties (defaults)
    private static final int PR_PHASEMOD		= 0;		// pr.bool
    private static final int PR_HIDEPTH		= 0;		// pr.para
    private static final int PR_LODEPTH		= 1;
    private static final int PR_BANDWIDTH		= 2;
    private static final int PR_PHASEMODFREQ	= 3;
    private static final int PR_GAIN			= 4;

    private static final String PRN_PHASEMOD		= "PhaseMod";
    private static final String PRN_HIDEPTH		= "HiDepth";
    private static final String PRN_LODEPTH		= "LoDepth";
    private static final String PRN_BANDWIDTH		= "Bandwidth";
    private static final String PRN_PHASEMODFREQ	= "PhaseModFreq";
    private static final String PRN_GAIN			= "Gain";

    private static final boolean	prBool[]		= { true };
    private static final String	prBoolName[]	= { PRN_PHASEMOD };
    private static final Param	prPara[]		= { null, null, null, null, null };
    private static final String	prParaName[]	= { PRN_HIDEPTH, PRN_LODEPTH, PRN_BANDWIDTH,
                                                        PRN_PHASEMODFREQ, PRN_GAIN };

    protected static final float	hiFreq		= 16000.0f;	// [Hz] corresponds to PR_HIDEPTH

// -------- public methods --------
    // public Container createGUI( int type );

    public Mono2StereoOp()
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

            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;

            static_pr.para		= prPara;
            static_pr.para[ PR_HIDEPTH ]		= new Param(  7.5,  Param.DECIBEL_AMP );
            static_pr.para[ PR_LODEPTH ]		= new Param(  3.0,  Param.DECIBEL_AMP );
            static_pr.para[ PR_BANDWIDTH ]		= new Param( 92.0,  Param.OFFSET_HZ );
            static_pr.para[ PR_PHASEMODFREQ ]	= new Param(  0.66, Param.ABS_HZ );
            static_pr.para[ PR_GAIN ]			= new Param(  0.0,  Param.DECIBEL_AMP );
            static_pr.paraName	= prParaName;

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "Mono2StereoOp";
        prefs		= static_prefs;
        presets		= static_presets;
        pr			= (PropertyArray) static_pr.clone();

        // slots
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

        // icon
        icon = new OpIcon( this, OpIcon.ID_MONO2STEREO, defaultName );
    }

// -------- Runnable methods --------

    public void run()
    {
        runInit();		// superclass

        // Haupt-Variablen fuer den Prozess
        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot;
        SpectStream		runInStream		= null;
        SpectStream		runOutStream;

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr		= null;

        // Berechnungs-Grundlagen
        Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
        double			gain;
        int				ch2;						// Source-Kanal fuer rechten Zielkanal (0 oder 1)

        // Modulations-Variablen
        boolean			recalc			= true;		// false, wenn sich die Werte nicht geaendert haben
        float			phaseOffset		= 0.0f;
        float			foo;

        // fuer Lookup-Berechnung
        int				loBand;						// unterstes benutztes Band; i.d.R. 1 (DC produziert Division by 0)
        Param			freq;
        Param			freqFloorOffs;
        Param			freqCeilOffs;
        Param			freqFloor;
        Param			freqCeil;
        float			depthFactor[];
        float			depth[];
        float			freqPhase[];
        double			val;

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

            // band-abhaengige Stereo-Breite
            depthFactor	= new float[ runInStream.bands ];
            depth		= new float[ runInStream.bands ];
            // frequency angle depending on bandwidth;
            freqPhase	= new float[ runInStream.bands ];
            // Source-Kanal fuer rechten Zielkanal
            ch2			= 1 % runInStream.chanNum;

            // ------------------------------ Output-Slot ------------------------------
            runOutSlot	= slots.elementAt( SLOT_OUTPUT );
            runOutStream = new SpectStream( runInStream );
            runOutStream.setChannels( 2 );
            runOutSlot.initWriter( runOutStream );

            // ------------------------------ Vorberechnungen ------------------------------

            gain	= (float) Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                               ampRef, runInStream ).value;

            // Winkelfrequenzen und Depth-Koeffizienten Tabelle	(dL + (dH - dL) * f / fH)
            freq			= new Param( 0.0, Param.ABS_HZ );
            freqCeilOffs	= pr.para[ PR_BANDWIDTH ];
            freqFloorOffs	= new Param( -freqCeilOffs.value, freqCeilOffs.unit );
// XXX
loBand = 1;
            for( int i = loBand; i < runInStream.bands; i++ ) {

// XXX				freq.value		= runInStream.freq[ i ];
freq.value = i * runInStream.hiFreq / (runInStream.bands - 1);

                freqCeil		= Param.transform( freqCeilOffs,  Param.ABS_HZ, freq, runInStream );
                freqFloor		= Param.transform( freqFloorOffs, Param.ABS_HZ, freq, runInStream );
                freqPhase[ i ]	= (float) ((freq.value / (freqCeil.value - freqFloor.value)) % 1.0);
                depthFactor[ i ]= (float) (pr.para[ PR_LODEPTH ].value + ((pr.para[ PR_HIDEPTH ].value -
                                           pr.para[ PR_LODEPTH ].value) * freq.value / hiFreq ));

// System.out.println( (int) freq.value + " Hz: ceil = "+(int) freqCeil.value +"/floor = "+(int) freqFloor.value+"; phase = "+(int) (freqPhase[ i ]*360) );
            }

            // ------------------------------ Hauptschleife ------------------------------
            runSlotsReady();
mainLoop:	while( !threadDead ) {
            // ---------- Process: (modulierte) Variablen ----------
                if( pr.bool[ PR_PHASEMOD ]) {
                    foo	= (float) (pr.para[ PR_PHASEMODFREQ ].value *		  // "sawtooth"
                          ((runInStream.getTime() / 1000.0) % (1.0 / pr.para[ PR_PHASEMODFREQ ].value)));

                    if( Math.abs( foo - phaseOffset ) >= 0.01 ) {		// 1.8 degrees treshhold improves speed
                        phaseOffset	= foo;
                        recalc		= true;
                    }
                }

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

            // ---------- Process: Lookup-Table berechnen ----------
                if( recalc ) {									// something's changed...
                    // Depth-Tabelle berechnen
                    for( int i = loBand; i < runInStream.bands; i++ ) {

                        val	= Math.sin( Math.PI * (freqPhase[ i ] + phaseOffset) );
                        val	= (val * val - 0.5) * depthFactor[ i ];

                        depth[ i ] = (float) Math.exp( val / 20 * Constants.ln10 );
// System.out.println( i + ": " + (int) (depth[ i ] * 100) + "%" );
                    }

                    recalc = false;
                }

            // ---------- Process: Ziel-Frame berechnen ----------

                System.arraycopy( runInFr.data[ 0 ], 0, runOutFr.data[ 0 ], 0,
                                  runInFr.data[ 0 ].length );
                System.arraycopy( runInFr.data[ ch2 ], 0, runOutFr.data[ 1 ], 0,
                                  runInFr.data[ ch2 ].length );

                for( int band = loBand; band < runOutStream.bands; band++ ) {

                    runOutFr.data[ 0 ][ (band << 1) + SpectFrame.AMP ] *= gain * depth[ band ];
                    runOutFr.data[ 1 ][ (band << 1) + SpectFrame.AMP ] *= gain / depth[ band ];
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
                "gl" + GroupLabel.NAME_GENERAL + "\n" +
                        "lbHigh freq stereo depth;pf" + Constants.decibelAmpSpace + ",pr" + PRN_HIDEPTH + "\n" +
                        "lbLow freq stereo depth;pf" + Constants.decibelAmpSpace + ",pr" + PRN_LODEPTH + "\n" +
                        "lbBandwidth;pf" + Constants.offsetFreqSpace + "|" + Constants.offsetHzSpace +
                        "|" + Constants.offsetSemitonesSpace + ",pr" + PRN_BANDWIDTH + "\n" +
                        "lbTotal gain;pf" + Constants.decibelAmpSpace + ",pr" + PRN_GAIN + "\n" +
                        "gl" + GroupLabel.NAME_MODULATION + "\n" +
                        "cbPhase motion,actrue|1|en,acfalse|1|di,pr" + PRN_PHASEMOD + ";" +
                        "pf" + Constants.lfoHzSpace + ",id1,pr" + PRN_PHASEMODFREQ);

        return gui;
    }
}