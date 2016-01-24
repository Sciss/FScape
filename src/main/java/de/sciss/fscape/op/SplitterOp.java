/*
 *  SplitterOp.java
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
import de.sciss.fscape.util.Envelope;
import de.sciss.fscape.util.Modulator;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

public class SplitterOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Splitter";		// "real" name (z.B. Icons)

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    protected static final int NUM_OUTPUT		= 6;		// Zahl der Slots

    // Slots
    protected static final int SLOT_INPUT		= 0;
    protected static final int SLOT_OUTPUT		= 1;		// bis 6

    // Properties (defaults)
    private static final int PR_CHANNELS		= 0;		// Array-Indices: pr.intg
    private static final int PR_NORMALIZE		= 0;		// pr.bool
    private static final int PR_GAINMOD		= 1;		// bis 6
    private static final int PR_NORMGAIN		= 0;		// pr.para
    private static final int PR_GAIN			= 1;		// bis 6
    private static final int PR_GAINMODDEPTH	= 7;		// bis 12
    private static final int PR_GAINMODENV	= 0;		// pr.envl  ; bis 5

    private static final int PR_CHANNELS_ALL		= 0;
    private static final int PR_CHANNELS_SINGLE	= 1;

    private static final String PRN_CHANNELS		= "Channels";	// Property-Keynames
    private static final String PRN_GAIN			= "Gain";		// +0...5
    private static final String PRN_GAINMOD		= "GainMod";
    private static final String PRN_GAINMODDEPTH	= "GainModDepth";
    private static final String PRN_GAINMODENV	= "GainModEnv";
    private static final String PRN_NORMALIZE		= "Normalize";
    private static final String PRN_NORMGAIN		= "NormGain";

    private static final int		prIntg[]		= { PR_CHANNELS_ALL };
    private static final String	prIntgName[]	= { PRN_CHANNELS };

// -------- public methods --------
    // public PropertyGUI createGUI( int type );

    public SplitterOp()
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
            static_pr.bool		= new boolean[ 1 + NUM_OUTPUT ];
            static_pr.boolName	= new String[ 1 + NUM_OUTPUT ];
            static_pr.para		= new Param[ 1 + (NUM_OUTPUT << 1) ];
            static_pr.paraName	= new String[ 1 + (NUM_OUTPUT << 1) ];
            static_pr.envl		= new Envelope[ NUM_OUTPUT ];
            static_pr.envlName	= new String[ NUM_OUTPUT ];

            static_pr.bool[ PR_NORMALIZE ]		= false;
            static_pr.boolName[ PR_NORMALIZE ]	= PRN_NORMALIZE;
            static_pr.para[ PR_NORMGAIN ]		= new Param( 0.0, Param.DECIBEL_AMP );
            static_pr.paraName[ PR_NORMGAIN ]	= PRN_NORMGAIN;

            for( int i = 0; i < NUM_OUTPUT; i++ ) {

                static_pr.para[ PR_GAIN + i ]				= new Param(  0.0, Param.DECIBEL_AMP );
                static_pr.para[ PR_GAINMODDEPTH + i ]		= new Param( 96.0, Param.DECIBEL_AMP );
                static_pr.paraName[ PR_GAIN + i ]			= PRN_GAIN + (i+1);
                static_pr.paraName[ PR_GAINMODDEPTH + i ]	= PRN_GAINMODDEPTH + (i+1);
                static_pr.envl[ PR_GAINMODENV + i ]			= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
                static_pr.envlName[ PR_GAINMODENV + i ]		= PRN_GAINMODENV + (i+1);
                static_pr.bool[ PR_GAINMOD + i ]			= false;
                static_pr.boolName[ PR_GAINMOD + i ]		= PRN_GAINMOD + (i+1);
            }

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "SplitterOp";
        prefs		= static_prefs;
        presets		= static_presets;
        pr			= (PropertyArray) static_pr.clone();

        // slots
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
        for( int i = 0; i < NUM_OUTPUT; i++ ) {
            slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER,
                                                    Slots.SLOTS_DEFWRITER +(i+1) ));	// SLOT_OUTPUT
        }

        // icon
        icon = new OpIcon( this, OpIcon.ID_SPLITTER, defaultName );
    }

// -------- Runnable methods --------

    public void run() {
        runInit();        // superclass

        // main variables
        SpectStreamSlot	runInSlot;
        SpectStreamSlot	runOutSlot[]	= new SpectStreamSlot[ NUM_OUTPUT ];
        SpectStream		runInStream		= null;
        SpectStream		runOutStream[]	= new SpectStream[ NUM_OUTPUT ];

        SpectFrame		runInFr			= null;
        SpectFrame		runOutFr[]		= new SpectFrame[ NUM_OUTPUT ];
        int				runSlotNum[]	= new int[ NUM_OUTPUT ];
        int				runChanNum[]	= new int[ NUM_OUTPUT ];

        // basis of calculations
        Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform reference
        double			normBase;
        Param			gainBase[]		= new Param[ NUM_OUTPUT ];

        // modulations
        float			gain[]			= new float[ NUM_OUTPUT ];
        Modulator		gainMod[]		= new Modulator[ NUM_OUTPUT ];
        double			sumGain;

        // others
        int				numOut			= 0;	// number of used slots
        int				oldWriteDone;
        int				writeable;
        int				chanNum;
        float			srcData[];				// frame data of one channel
        float			destData[];

    topLevel:
        try {
            // ------------------------------ Input-Slot ------------------------------
            runInSlot = slots.elementAt(SLOT_INPUT);
            if (runInSlot.getLinked() == null) {
                runStop();    // threadDead = true -> the following for() will be skipped
            }
            // this while loop is necessary, since during initReader a pause could be made
            // and the InterruptException is thrown; after that we retry
            for (boolean initDone = false; !initDone && !threadDead; ) {
                try {
                    runInStream = runInSlot.getDescr();    // throws InterruptedException
                    initDone = true;
                } catch (InterruptedException ignored) {}
                runCheckPause();
            }
            if (threadDead) break topLevel;

            // ------------------------------ Output-Slots ------------------------------
            for (int i = 0; i < NUM_OUTPUT; i++) {
                runOutSlot[numOut] = slots.elementAt(SLOT_OUTPUT + i);
                if (runOutSlot[numOut].getLinked() != null) {
                    runOutStream[numOut] = new SpectStream(runInStream);

                    if (pr.intg[PR_CHANNELS] == PR_CHANNELS_SINGLE) {
                        runOutStream[numOut].setChannels(1);
                        runChanNum[numOut] = numOut % runInStream.chanNum;
                    } else {
                        runChanNum[numOut] = 0;
                    }
                    runOutSlot[numOut].initWriter(runOutStream[numOut]);
                    runSlotNum[numOut] = i;
                    numOut++;
                }
            }

            // ------------------------------ preparatory calculation  ------------------------------
            normBase = Param.transform(pr.para[PR_NORMGAIN], Param.ABS_AMP, ampRef, runInStream).value;

            // create modulators
            for (int i = 0; i < numOut; i++) {
                gainBase[i] = Param.transform(pr.para[PR_GAIN + runSlotNum[i]],
                        Param.ABS_AMP, ampRef, runInStream);

                if (pr.bool[PR_GAINMOD + runSlotNum[i]]) {
                    gainMod[i] = new Modulator(gainBase[i],
                            pr.para[PR_GAINMODDEPTH + runSlotNum[i]],
                            pr.envl[PR_GAINMODENV + runSlotNum[i]],
                            runInStream);
                }
            }

            if (pr.intg[PR_CHANNELS] == PR_CHANNELS_SINGLE) {
                chanNum = 1;
            } else {
                chanNum = runInStream.chanNum;
            }

            // ------------------------------ main loop ------------------------------
            runSlotsReady();

        mainLoop:
            while (!threadDead && (numOut > 0)) {

                // ---------- Process: (modulierte) Variablen ----------


                // ---------- Frame einlesen ----------
                for (boolean readDone = false; (!readDone) && !threadDead; ) {
                    try {
                        runInFr = runInSlot.readFrame();    // throws InterruptedException
                        readDone = true;
                    } catch (InterruptedException ignored) {
                    } catch (EOFException e) {
                        break mainLoop;
                    }
                    runCheckPause();
                }
                if (threadDead) break mainLoop;

                // ---------- Process: calculate modulations ----------

                sumGain = 0.0;
                for (int i = 0; i < numOut; i++) {
                    if (pr.bool[PR_GAINMOD + runSlotNum[i]]) {
                        gain[i] = (float) gainMod[i].calc().value;
                    } else {
                        gain[i] = (float) gainBase[i].value;
                    }
                    sumGain += gain[i];
                }

                // normalize
                if (pr.bool[PR_NORMALIZE]) {
                    for (int i = 0; i < numOut; i++) {
                        gain[i] *= (float) (normBase / sumGain);
                    }
                }

                // ---------- Process: calculate target frame ----------

//calcFrame:
                for (int i = 0; i < numOut; i++) {
                    if ((Math.abs(gain[i] - 1.0f) < 1.0e-7f) &&
                            (pr.intg[PR_CHANNELS] == PR_CHANNELS_ALL)) {        // 1:1 copy from InFrame

                        runOutFr[i] = new SpectFrame(runInFr);
                        gain[i] = 1.0f;    // important for successor

                    } else {

//						for( int j = 0; j < i; j++ ) {	// evtl. koennen wir Vorgaenger kopieren?
//
//							// that is, if there is the same gain and same channels
//							if( (Math.abs( gain[ i ] - gain[ j ]) < 0.01f) &&
//								((pr.intg[ PR_CHANNELS ] == PR_CHANNELS_ALL) ||
//								 ((i % runInStream.chanNum) == (j % runInStream.chanNum))) ) {
//
//								runOutFr[ i ]	= new SpectFrame( runOutFr[ j ]);
//								continue calcFrame;
//							}
//						}

                        // possibly copy channels or set them to zero
                        runOutFr[i] = runOutStream[i].allocFrame();

                        for (int ch = 0; ch < chanNum; ch++) {

                            srcData = runInFr.data[runChanNum[i] + ch];
                            destData = runOutFr[i].data[ch];

                            if (gain[i] < 1.0e-7f) {        // simply set to zero
                                for (int j = 0; j < destData.length; j++) {
                                    destData[j] = 0.0f;
                                }

                            } else {
                                System.arraycopy(srcData, 0, destData, 0, srcData.length);

                                if (Math.abs(gain[i] - 1.0f) >= 1.0e-7f) {    // we must compute
                                    for (int j = 0; j < destData.length; j += 2) {
                                        destData[j + SpectFrame.AMP] *= gain[i];
                                    }

                                } else {                    // 1:1 copy from channel is ok
                                    gain[i] = 1.0f;        // (successor)
                                }
                            }
                        }
                    }
                }
                runFrameDone(runInSlot, runInFr);
                runInSlot.freeFrame(runInFr);

                // ---------- write frame ----------

                for (int writeDone = 0; (writeDone < numOut) && !threadDead; ) {

                    oldWriteDone = writeDone;

                    for (int i = 0; i < numOut; i++) {
                        try {    // Unterbrechung

                            if (runOutFr[i] != null) {        // noch nicht geschrieben
                                writeable = runOutStream[i].framesWriteable();
                                if (writeable > 0) {
                                    runOutSlot[i].writeFrame(runOutFr[i]);
                                    writeDone++;
                                    runOutStream[i].freeFrame(runOutFr[i]);
                                    runOutFr[i] = null;    // erkennbar machen, dass wir damit fertig sind

                                } else if (writeable < 0) {    // Stream geschlossen

                                    writeDone++;
                                    runFrameDone(runOutSlot[i], runOutFr[i]);
                                    runOutStream[i].freeFrame(runOutFr[i]);

                                    // alles aufruecken
                                    for (int j = i + 1; j < numOut; j++) {
                                        runOutSlot[j] = runOutSlot[j - 1];
                                        runOutStream[j] = runOutStream[j - 1];
                                        runOutFr[j] = runOutFr[j - 1];
                                        runSlotNum[j] = runSlotNum[j - 1];
                                        runChanNum[j] = runChanNum[j - 1];
                                        gainBase[j] = gainBase[j - 1];
                                        gain[j] = gain[j - 1];
                                        gainMod[j] = gainMod[j - 1];
                                    }
                                    numOut--;
                                    i--;
                                }
                            }
                        } catch (InterruptedException e) {
                            break mainLoop;
                        }
                        runCheckPause();
                    }

                    if (oldWriteDone == writeDone) {    // nothing could be written
                        try {
                            Thread.sleep(500);    // ...thus pause shortly
                        } catch (InterruptedException ignored) {
                        }    // mainLoop will soon be automatically exited
                        runCheckPause();
                    }
                }
            } // end of main loop

            runInStream.closeReader();
            for (int i = 0; i < numOut; i++) {
                runOutStream[i].closeWriter();
            }

        } // break topLevel

        catch (IOException e) {
            runQuit(e);
            return;
        } catch (SlotAlreadyConnectedException e) {
            runQuit(e);
            return;
        }
//		catch( OutOfMemoryException e ) {
//			abort( e );
//			return;
//		}

        runQuit(null);
    }

// -------- GUI methods --------

    public PropertyGUI createGUI(int type) {
        PropertyGUI		gui;
        StringBuffer	gain		= new StringBuffer();
        StringBuffer	gainMod		= new StringBuffer();

        if( type != GUI_PREFS ) return null;

        for( int i = 1; i <= NUM_OUTPUT; i++ ) {
            gain.append( "\nlbSlot "+i+";pf"+Constants.decibelAmpSpace+",id"+(i<<2)+
                         ",pr"+pr.paraName[ PR_GAIN + i-1 ] );
            gainMod.append( "\ncbSlot "+i+" Gain,actrue|"+((i<<2)+1)+"|en|"+((i<<2)+2)+
                            "|en,acfalse|"+((i<<2)+1)+"|di|"+((i<<2)+2)+"|di,pr"+
                            (PRN_GAINMOD+i)+";pf"+Constants.decibelAmpSpace+",id"+((i<<2)+1)+
                            ",pr"+(PRN_GAINMODDEPTH+i)+";en,id"+((i<<2)+2)+",pr"+
                            (PRN_GAINMODENV+i) );
        }

        gui = new PropertyGUI(
            "gl"+GroupLabel.NAME_GENERAL+"\n" +
            "lbEach slot carries;ch,pr"+PRN_CHANNELS+"," +
                "itAll channels," +
                "itOne channel\n" +
            "cbNormalize sum,actrue|100|en,acfalse|100|di,pr"+PRN_NORMALIZE+";"+
            "pf"+Constants.decibelAmpSpace+",id100,pr"+PRN_NORMGAIN+"\n" +
            "glOutput slot gain" + gain +
            "\ngl"+GroupLabel.NAME_MODULATION + gainMod );

        return gui;
    }
}