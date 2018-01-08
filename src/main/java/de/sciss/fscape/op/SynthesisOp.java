/*
 *  SynthesisOp.java
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

import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PathField;
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
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 *	Synthesis Operator
 */
public class SynthesisOp
        extends Operator {

// -------- private variables --------

    protected static final String defaultName = "Synthesize";

    protected static Presets		static_presets	= null;
    protected static Prefs			static_prefs	= null;
    protected static PropertyArray	static_pr		= null;

    // Slots
    protected static final int SLOT_INPUT		= 0;		// es kann nur einen geben

    // Properties (defaults)
    private static final int PR_FILENAME		= 0;		// Array-Indices: pr.text
    private static final int PR_WINDOW		= 0;		// pr.intg
    private static final int PR_TYPE			= 1;
    private static final int PR_ROTATE		= 0;		// pr.bool
    private static final int PR_LOFREQ		= 0;		// pr.para
    private static final int PR_HIFREQ		= 1;
    private static final int PR_LORADIUS		= 2;
    private static final int PR_HIRADIUS		= 3;

    protected static final int TYPE_FFT			= 0;
    protected static final int TYPE_CZT			= 1;
    protected static final int TYPE_NONE		= 2;

    private static final String PRN_FILENAME		= "Filename";	// Property-Keynames
    private static final String PRN_WINDOW		= "Window";
    private static final String PRN_ROTATE		= "Rotate";
    private static final String PRN_TYPE			= "Type";
    private static final String PRN_LOFREQ		= "LoFreq";
    private static final String PRN_HIFREQ		= "HiFreq";
    private static final String PRN_LORADIUS		= "LoRadius";
    private static final String PRN_HIRADIUS		= "HiRadius";

    private static final String	prText[]		= { "" };
    private static final String	prTextName[]	= { PRN_FILENAME };
    private static final int		prIntg[]		= { 0, TYPE_FFT };
    private static final String	prIntgName[]	= { PRN_WINDOW, PRN_TYPE };
    private static final boolean	prBool[]		= { true };
    private static final String	prBoolName[]	= { PRN_ROTATE };
    private static final Param	prPara[]		= { null, null, null, null };
    private static final String	prParaName[]	= { PRN_LOFREQ, PRN_HIFREQ, PRN_LORADIUS, PRN_HIRADIUS };

    // Laufzeitfehler
    protected static final String ERR_NOOUTPUT	= "No output file";

// -------- public methods --------
    // public Container createGUI( int type );

    public SynthesisOp()
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

            static_pr.text		= prText;
            static_pr.textName	= prTextName;
            static_pr.intg		= prIntg;
            static_pr.intgName	= prIntgName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.para		= prPara;
// XXX			static_pr.para[ PR_STARTSHIFT ]	= new Param(     0.0, Param.ABS_MS );
// XXX			static_pr.para[ PR_LENGTH ]		= new Param(  5000.0, Param.ABS_MS );
            static_pr.para[ PR_LOFREQ ]		= new Param(     0.0, Param.ABS_HZ );
            static_pr.para[ PR_HIFREQ ]		= new Param( 22050.0, Param.ABS_HZ );
            static_pr.para[ PR_LORADIUS ]	= new Param(     0.0, Param.DECIBEL_AMP );
            static_pr.para[ PR_HIRADIUS ]	= new Param(     0.0, Param.DECIBEL_AMP );
            static_pr.paraName	= prParaName;

            static_pr.superPr	= Operator.op_static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }

        // superclass-Felder uebertragen
        opName		= "SynthesisOp";
        prefs		= static_prefs;
        presets		= static_presets;
        pr			= (PropertyArray) static_pr.clone();

        // slots
        slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));	// SLOT_INPUT

        // icon
        icon = new OpIcon( this, OpIcon.ID_OUTPUT, defaultName );
    }

// -------- Runnable methods --------

    public void run()
    {
        runInit();		// superclass

        int		i, j, k, m, ch;
        double	d1;
        float	f1;

        // Haupt-Variablen fuer den Prozess
        String			runFileName;
        AudioFile		runFile			= null;

        SpectStreamSlot	runInSlot;
        SpectStream		runInStream		= null;
        SpectStream		tmpStream;
        AudioFileDescr		runOutStream;

        SpectFrame		runInFr			= null;

        int				winSize, winHalf, winStep, fftLength;
        float[]			win;
        int				inChanNum;

        float[]			fftBuf, convBuf1;
        float[][]		outBuf;
        int				outBufOff;
        int				samplesWritten;	// sample *frames*!
        int				chunkLength;

//		boolean			chirp			= pr.intg[ PR_TYPE ] == TYPE_CZT;
//		boolean			fourier			= pr.intg[ PR_TYPE ] == TYPE_FFT;
//		double			loFreq, hiFreq, loRadius, hiRadius, chirpAngle, chirpRadius;

        // ------------------------------ Filename ------------------------------
        if( (pr.text[ PR_FILENAME ] == null) || (pr.text[ PR_FILENAME ].length() == 0) ) {

            FileDialog	fDlg;				// ausnahmsweise nicht am methods-Anfang ;)
            String		foName, foDir;
            final		Component	ownerF	= owner.getModule().getComponent();
            final		boolean		makeF	= !(ownerF instanceof Frame);
            final		Frame		f		= makeF ? new Frame() : (Frame) ownerF;

            fDlg	= new FileDialog( f, ((OpIcon) getIcon()).getName() +
                                      ": Select output file", FileDialog.SAVE );
            fDlg.setVisible( true );
            if( makeF ) f.dispose();
            foName	= fDlg.getFile();
            foDir	= fDlg.getDirectory();
            fDlg.dispose();

            if( foDir == null) foDir = "";
            if( foName == null) {				// Cancel. Wir koennen folglich nichts mehr tun
                runQuit( new IOException( ERR_NOOUTPUT ));
                return;
            }
            runFileName = foDir + foName;

        } else {
            runFileName = pr.text[ PR_FILENAME ];
        }

topLevel:
        try {				// no format field yet and normalization would be a problem
                            // ; therefore just write a floating point ircam file
            IOUtil.createEmptyFile( new File( runFileName ));

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

            inChanNum = runInStream.chanNum;

            // ------------------------------ weitere init ------------------------------

            tmpStream	= new SpectStream();
            tmpStream.smpRate = runInStream.smpRate;	// this sucks like hell
//			loFreq		= Param.transform( pr.para[ PR_LOFREQ ], Param.ABS_HZ, null, tmpStream ).value *
//						  Constants.PI2 / tmpStream.smpRate;
//			hiFreq		= Param.transform( pr.para[ PR_HIFREQ ], Param.ABS_HZ, null, tmpStream ).value *
//						  Constants.PI2 / tmpStream.smpRate;
//			loRadius	= Param.transform( pr.para[ PR_LORADIUS ], Param.ABS_AMP, new Param( 1.0, Param.ABS_AMP ),
//										   null ).value;
//			hiRadius	= Param.transform( pr.para[ PR_HIRADIUS ], Param.ABS_AMP, new Param( 1.0, Param.ABS_AMP ),
//										   null ).value;

            winStep		= runInStream.smpPerFrame;
            fftLength	= (runInStream.bands - 1) << 1;
            winSize		= fftLength;
            winHalf		= winSize >> 1;
            fftBuf		= new float[ fftLength + 2 ];
            outBuf		= new float[ inChanNum ][ winSize ];
            Util.clear( outBuf );
            win			= Filter.createFullWindow( winSize, pr.intg[ PR_WINDOW ]);

            // normalize energy
            for( i = 0, d1 = 0.0; i < winSize; i++ ) {
                d1 += (double) win[ i ];
            }
            f1 = (float) ((double) fftLength * (double) winStep / d1);	// (1.0 / d1);
            for( i = 0; i < winSize; i++ ) {
                win[ i ] *= f1;
            }

            // ------------------------------ AudioFileDescr ------------------------------
            runOutStream				= new AudioFileDescr();
            runOutStream.channels		= inChanNum;
            runOutStream.rate			= runInStream.smpRate;
            runOutStream.bitsPerSample	= 32;
            runOutStream.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
            runOutStream.type			= AudioFileDescr.TYPE_IRCAM;
            runOutStream.file			= new File( runFileName );

            runFile	= AudioFile.openAsWrite( runOutStream );

            // ------------------------------ Hauptschleife ------------------------------
            runSlotsReady();

            outBufOff		= 0;		// rotate offset instead of buffer content shifting (faster)
            samplesWritten	= 0;
            chunkLength		= winStep;

// System.out.println( "Sy: chunkLen "+winStep );

mainLoop:	while( !threadDead ) {
                for( boolean readDone = false; (!readDone) && !threadDead; ) {
                    try {
                        runInFr		= runInSlot.readFrame();	// throws InterruptedException
                        readDone	= true;
                    }
                    catch( InterruptedException ignored) {}
                    catch( EOFException e ) {
                        break mainLoop;
                    }
                    runCheckPause();
                }
                if( threadDead ) break mainLoop;

                // transform
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = runInFr.data[ ch ];
                    Fourier.polar2Rect( convBuf1, 0, fftBuf, 0, fftLength + 2 );
// if( (runInStream.framesRead < 3) && (ch == 0) ) Debug.view( fftBuf, "sy IFFT "+runInStream.framesRead );

                    convBuf1 = outBuf[ ch ];

                    switch( pr.intg[ PR_TYPE ]) {
                    case TYPE_CZT:								// -------------------- CZT --------------------

                        throw new IOException( "CZT not yet implemented!" );
                    //	break;

                    case TYPE_FFT:								// -------------------- FFT --------------------

                        Fourier.realTransform( fftBuf, fftLength, Fourier.INVERSE );

                        if( pr.bool[ PR_ROTATE ]) {

                            // apply right wing window
                            for( i = 0, j = winHalf; i < winHalf; i++, j++ ) {
                                fftBuf[ i ] *= win[ j ];
                            }
                            // zero pad oversampling range
                            for( ; i < fftLength - winHalf; i++ ) {
                                fftBuf[ i ] = 0.0f;
                            }
                            // apply left wing window
                            for( j = 0; i < fftLength; i++, j++ ) {
                                fftBuf[ i ] *= win[ j ];
                            }

                            // we shift the origin so that the middle smp of inBuf becomes fftBuf[ 0 ],
                            //   this gives better phasograms
                            // not compatible with soundhack; but we aren't anyways ;---)
                            for( i = outBufOff, j = fftLength - winHalf, k = 0; k < winSize; ) {
                                m  = Math.min( winSize - k, Math.min( winSize - i, fftLength - j ));
                                for( ; m > 0; m--, k++ ) {
                                    convBuf1[ i++ ] += fftBuf[ j++ ];
                                }
                                i %= winSize;
                                j %= fftLength;
                            }

                        } else {

                            // mix windowed output portion
                            for( i = outBufOff, j = 0, k = 0; k < winSize; ) {
                                m  = Math.min( winSize - k, winSize - i );
                                for( ; m > 0; m-- ) {
                                    convBuf1[ i++ ] += fftBuf[ j++ ] * win[ k++ ];
                                }
                                i %= winSize;
                            }
                        }
                        break;

                    case TYPE_NONE:								// -------------------- None --------------------

                        if( pr.bool[ PR_ROTATE ]) {

                            // apply right wing window
                            for( i = 0, j = winHalf; i < winHalf; i++, j++ ) {
                                fftBuf[ i ] *= win[ j ];
                            }
                            // zero pad oversampling range
                            for( ; i < fftLength - winHalf; i++ ) {
                                fftBuf[ i ] = 0.0f;
                            }
                            // apply left wing window
                            for( j = 0; i < fftLength; i++, j++ ) {
                                fftBuf[ i ] *= win[ j ];
                            }

                            // we shift the origin so that the middle smp of inBuf becomes fftBuf[ 0 ],
                            //   this gives better phasograms
                            // not compatible with soundhack; but we aren't anyways ;---)
                            for( i = outBufOff, j = fftLength - winHalf, k = 0; k < winSize; ) {
                                m  = Math.min( winSize - k, Math.min( winSize - i, fftLength - j ));
                                for( ; m > 0; m--, k++ ) {
                                    convBuf1[ i++ ] += fftBuf[ j++ ];
                                }
                                i %= winSize;
                                j %= fftLength;
                            }

                        } else {

                            // mix windowed output portion
                            for( i = outBufOff, j = 0, k = 0; k < winSize; ) {
                                m  = Math.min( winSize - k, winSize - i );
                                for( ; m > 0; m-- ) {
                                    convBuf1[ i++ ] += fftBuf[ j++ ] * win[ k++ ];
                                }
                                i %= winSize;
                            }
                        }
                        break;

                    } // switch type
                } // for channels
// if( (runInStream.framesRead < 3) && (ch == 0) ) Debug.view( outBuf[0], "sy win outbuf "+runInStream.framesRead );

                // write sound chunk
                runFile.writeFrames( outBuf, outBufOff, chunkLength );
                // zero obsolete portion
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = outBuf[ ch ];
                    for( i = outBufOff + chunkLength; i > outBufOff; ) {
                        convBuf1[ --i ] = 0.0f;
                    }
                }
                samplesWritten += chunkLength;
                outBufOff		= (outBufOff + chunkLength) % winSize;

                runFrameDone( runInSlot, runInFr );
                runInSlot.freeFrame( runInFr );

            } // end of main loop

            runInStream.closeReader();

            // flush overlap buf
            if( !threadDead ) {
                for( i = winStep; i < winSize; i += winStep ) {
                    runFile.writeFrames( outBuf, outBufOff, chunkLength );
                    outBufOff = (outBufOff + chunkLength) % winSize;
                }
            }
            runFile.close();

        } // break topLevel

        catch( IOException e ) {
            if( runFile != null ) {
                runFile.cleanUp();
            }
            runQuit( e );
            return;
        }
//		catch( OutOfMemoryException e ) {
//			if( runFile != null ) {
//				runFile.cleanUp();
//			}
//			abort( e );
//			return;
//		}

        runQuit( null );
    }

// -------- GUI methods --------

    public PropertyGUI createGUI(int type) {
        PropertyGUI		gui;
        String[]		winNames	= Filter.getWindowNames();
        StringBuffer	winChoise	= new StringBuffer();

        if( type != GUI_PREFS ) return null;

        for( int i = 0; i < winNames.length; i++ ) {
            winChoise.append( ",it" );
            winChoise.append( winNames[ i ]);
        }

        gui = new PropertyGUI(
            "gl"+GroupLabel.NAME_GENERAL+"\n" +
            "lbFilename;io"+PathField.TYPE_OUTPUTFILE+"|Select output file,pr"+PRN_FILENAME+"\n" +
            "lbTransform;ch,ac0|3|di|4|di|5|di|6|di,ac1|3|en|4|en|5|en|6|en,pr"+PRN_TYPE+"," +
                "itDiscrete Fourier," +
                "itChirp Z," +
                "itNone\n" +
            "lbWindow;ch,pr"+PRN_WINDOW+winChoise.toString()+"\n"+
            "cbRotate origin,pr"+PRN_ROTATE+
            "\nglChirp transform specs\n" +
            "lbLow freq;pf"+Constants.absHzSpace+",id3,pr"+PRN_LOFREQ+"\n" +
            "lbHigh freq;pf"+Constants.absHzSpace+",id4,pr"+PRN_HIFREQ+"\n" +
            "lbLow radius;pf"+Constants.decibelAmpSpace+",id5,pr"+PRN_LORADIUS +"\n" +
            "lbHigh radius;pf"+Constants.decibelAmpSpace+",id6,pr"+PRN_HIRADIUS );

        return gui;
    }
}
