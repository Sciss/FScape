/*
 *  SMPTESynthDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-08	created
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Param;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 *  Processing module synthesizing
 *	SMPTE longitudinal timecode.
 *
 *	http://www.philrees.co.uk/articles/timecode.htm
 *	http://en.wikipedia.org/wiki/SMPTE_time_code
 *	http://en.wikipedia.org/wiki/RC_time_constant
 *	http://en.wikipedia.org/wiki/Low-pass_filter
 *	http://local.wasp.uwa.edu.au/~pbourke/other/interpolation/
 */
public class SMPTESynthDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_OUTPUTFILE		= 0;		// pr.text
    private static final int PR_STARTTIME		= 1;
    private static final int PR_STOPTIME		= 2;
    private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_OUTPUTRES		= 1;
    private static final int PR_OUTPUTRATE		= 2;
    private static final int PR_FRAMES			= 3;
//	private static final int PR_MINPHASE		= 0;		// pr.bool
    private static final int PR_GAIN			= 0;		// pr.para

    private static final String PRN_OUTPUTFILE	= "OutputFile";
    private static final String PRN_STARTTIME	= "StartTime";
    private static final String PRN_STOPTIME	= "StopTime";
    private static final String PRN_OUTPUTTYPE	= "OutputType";
    private static final String PRN_OUTPUTRES	= "OutputRes";
    private static final String PRN_OUTPUTRATE	= "OutputRate";
    private static final String PRN_FRAMES		= "Frames";

    private static final int	FRAMES_24		= 0;
    private static final int	FRAMES_25		= 1;
    private static final int	FRAMES_29		= 2;	// 29.97 Drop Frame
    private static final int	FRAMES_30		= 3;

    private static final String[]	FRAMES_LABELS = { "24", "25", "29.97 DF", "30" };

    private static final String[]	prText		= { "", "00:00:00:00", "00:05:00:00" };
    private static final String[]	prTextName	= { PRN_OUTPUTFILE, PRN_STARTTIME, PRN_STOPTIME };
    private static final int[]		prIntg		= { 0, 0, 1, FRAMES_30 };
    private static final String[]	prIntgName	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE, PRN_FRAMES };
//	private static final boolean[]	prBool		= { false };
//	private static final String[]	prBoolName	= { PRN_MINPHASE };
    private static final Param[]	prPara		= { new Param( -18.0, Param.DECIBEL_AMP )};
    private static final String[]	prParaName	= { PRN_GAIN };

    private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_STARTTIME		= GG_OFF_TEXTFIELD	+ PR_STARTTIME;
    private static final int GG_STOPTIME		= GG_OFF_TEXTFIELD	+ PR_STOPTIME;
    private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;
    private static final int GG_FRAMES			= GG_OFF_CHOICE		+ PR_FRAMES;
//	private static final int GG_MINPHASE		= GG_OFF_CHECKBOX	+ PR_MINPHASE;
    private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    private final MessageFormat		smpteMsgFrmt = new MessageFormat( "{0}:{1}:{2}:{3}" );
    private final NumberFormat		smpteNumFrmt = NumberFormat.getIntegerInstance( Locale.US );

// -------- public methods --------

    public SMPTESynthDlg()
    {
        super( "SMPTE Synthesizer" );
        init2();
    }

    protected void buildGUI()
    {
        // einmalig PropertyArray initialisieren
        if( static_pr == null ) {
            static_pr			= new PropertyArray();

            static_pr.text		= prText;
            static_pr.textName	= prTextName;
            static_pr.intg		= prIntg;
            static_pr.intgName	= prIntgName;
//			static_pr.bool		= prBool;
//			static_pr.boolName	= prBoolName;
            static_pr.para		= prPara;
            static_pr.paraName	= prParaName;

//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES, PR_OUTPUTRATE );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        final GridBagConstraints	con;

        final PathField				ggOutputFile;
        final Component[]			ggGain;
        final JComboBox				ggFrames;
        final JFormattedTextField	ggStartTime, ggStopTime;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

    // -------- Output Gadgets --------
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;

    gui.addLabel( new GroupLabel( "LTC Output", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Filename", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
        gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
        gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );

        ggGain			= createGadgets( GGTYPE_GAIN );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Amplitude", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
//		con.weightx		= 0.5;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

        // -------- Settings Gadgets --------
    gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        smpteNumFrmt.setMinimumIntegerDigits( 2 );
        smpteNumFrmt.setMaximumIntegerDigits( 2 );
        for( int i = 0; i < 4; i++ ) smpteMsgFrmt.setFormat( i, smpteNumFrmt );

        ggStartTime		= new JFormattedTextField();
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Start Time", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addTextField( ggStartTime, GG_STARTTIME, null );

        ggFrames		= new JComboBox();
        for( int i = 0; i < FRAMES_LABELS.length; i++ ) {
            ggFrames.addItem( FRAMES_LABELS[ i ]);
        }
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Frames/sec.", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggFrames, GG_FRAMES, null );

        ggStopTime		= new JFormattedTextField();
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Stop Time", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addTextField( ggStopTime, GG_STOPTIME, null );

        initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
    }

    /**
     *	Transfer values from prop-array to GUI
     */
    public void fillGUI()
    {
        super.fillGUI();
        super.fillGUI( gui );
    }

    /**
     *	Transfer values from GUI to prop-array
     */
    public void fillPropertyArray()
    {
        super.fillPropertyArray();
        super.fillPropertyArray( gui );
    }

// -------- Processor Interface --------

    protected void process()
    {
        final Param		ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
        final float		amp;

        final int				frames, framesPerBuf;
        final boolean			drop;
        final byte[]			word			= new byte[ 10 ];
        final float[]			buf, bufR;
        final float[][]			outBuf;
        final AudioFileDescr	outStream;
        final PathField			ggOutput;
        final int				smpsPerHalfbit, smpsPerWord, calcRate;
        final int				inBufPre		= 2;
        final double			tau, rf;
        final float				alpha;
        final long				outLength;
        final int				inBufLen;
        AudioFile				outF			= null;
        byte					hoursStart, minsStart, secsStart, framesStart;
        byte					hoursStop, minsStop, secsStop, framesStop, tempB;
        int						numStartFrames,numStopFrames, frameOffset, tempI;
        long					progOff, progLen, sampleOffset, sampleOffsetR;
        int						chunkLen, outBufOff, phase, outBufPre, inBufOff;
        float					yold, y0, y1, y2, y3, mu, mu2, a0, a1, a2, a3;
        double					d1;
        Object[]				msgArgs;

topLevel: try {
        // ---- open files ----

//			if( true ) throw new IOException( "NOT YET WORKING" );

            switch( pr.intg[ PR_FRAMES ]) {
            case FRAMES_24:
                frames	= 24;
                drop	= false;
                break;
            case FRAMES_25:
                frames	= 25;
                drop	= false;
                break;
            case FRAMES_29:
                frames	= 30;
                drop	= true;
//				break;
                throw new IllegalArgumentException( "Drop Frame is not supported yet!" );
            case FRAMES_30:
                frames	= 30;
                drop	= false;
                break;
            default:
                throw new IllegalArgumentException( String.valueOf( pr.intg[ PR_FRAMES ]));
            }

            msgArgs		= smpteMsgFrmt.parse( pr.text[ PR_STARTTIME ]);
            hoursStart	= ((Number) msgArgs[ 0 ]).byteValue();
            minsStart	= ((Number) msgArgs[ 1 ]).byteValue();
            secsStart	= ((Number) msgArgs[ 2 ]).byteValue();
            framesStart	= ((Number) msgArgs[ 3 ]).byteValue();
            numStartFrames = ((hoursStart * 60 + minsStart) * 60 + secsStart) * frames + framesStart;

            msgArgs		= smpteMsgFrmt.parse( pr.text[ PR_STOPTIME ]);
            hoursStop	= ((Number) msgArgs[ 0 ]).byteValue();
            minsStop	= ((Number) msgArgs[ 1 ]).byteValue();
            secsStop	= ((Number) msgArgs[ 2 ]).byteValue();
            framesStop	= ((Number) msgArgs[ 3 ]).byteValue();
            numStopFrames = ((hoursStop * 60 + minsStop) * 60 + secsStop) * frames + framesStop;

            // allow swapping
            if( numStartFrames > numStopFrames ) {
                tempI			= numStartFrames;
                numStartFrames	= numStopFrames;
                numStopFrames	= tempI;
                tempB			= hoursStart;
                hoursStart		= hoursStop;
                hoursStop		= tempB;
                tempB			= minsStart;
                minsStart		= minsStop;
                minsStop		= tempB;
                tempB			= secsStart;
                secsStart		= secsStop;
                secsStop		= tempB;
                tempB			= framesStart;
                framesStart		= framesStop;
                framesStop		= tempB;
            }

            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream	= new AudioFileDescr();
            ggOutput.fillStream( outStream );
            outStream.channels = 1;
//			outStream.rate = frames * 160 * smpsPerHalfbit;	// XXX
            outF		= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

//			smpsPerHalfbit = (int) Math.floor( outStream.rate / (frames * 160) );
            smpsPerHalfbit = (int) Math.ceil( outStream.rate / (frames * 160) );
            calcRate	= frames * 160 * smpsPerHalfbit;
//			System.out.println( "calculation sample rate is " + calcRate );
            tau			= 25.0e-6 / Math.log( 9 );	// 25 micro-s rise time --> time constant
            alpha		= (float) (1.0 / (tau * calcRate + 1));
            rf			= calcRate / outStream.rate; // reciprocal resampling factor

//			System.out.println( "tau " + tau + "; alpha " + alpha );

            smpsPerWord	= 160 * smpsPerHalfbit;
            progOff		= 0;
            outLength	= (long) Math.ceil( (long) (numStopFrames - numStartFrames) * smpsPerWord / rf );
            progLen		= outLength;
            frameOffset	= numStartFrames;
            framesPerBuf = 50;
            yold		= 0f;
//			buf			= new float[ framesPerBuf * 160 ];
            inBufLen	= framesPerBuf * smpsPerWord + 3;
            buf			= new float[ inBufLen ];
            bufR		= new float[ (int) Math.ceil( framesPerBuf * smpsPerWord / rf ) + 1 ];
            outBuf		= new float[][] { bufR };
            outBufPre	= 1; // 2; // 1;	// delay compensation

        // ---- synthesize output ----

            // Bits 0-3   : Frame Units (BCD-coded with LSB-first)
            // Bits 4-7   : User Bits 1 (all zero)
            // Bits 8-9   : Frame Tens (BCD-coded with LSB-first)
            // Bit 10     : Drop Frame Bool
            // Bit 11     : Colour Frame Bool (always zero)
            // Bits 12-15 : User Bits 2 (all zero)
            // Bits 16-19 : Secs Units (BCD-coded with LSB-first)
            // Bits 20-23 : User Bits 3 (all zero)
            // Bits 24-26 : Secs Tens (BCD-coded with LSB-first)
            // Bit 27     : Bi-Phase Mark Correction Bit (always zero)
            // Bits 28-31 : User Bits 4 (all zero)
            // Bits 32-35 : Mins Units (BCD-coded with LSB-first)
            // Bits 36-39 : User Bits 5 (all zero)
            // Bits 40-42 : Mins Tens (BCD-coded with LSB-first)
            // Bit 43     : Binary Group Flag Bit 1 (always zero)
            // Bits 44-47 : User Bits 6 (all zero)
            // Bits 48-51 : Hours Units (BCD-coded with LSB-first)
            // Bits 52-55 : User Bits 7 (all zero)
            // Bits 56-57 : Hours Tens (BCD-coded with LSB-first)
            // Bit 58     : Reserved (always zero)
            // Bit 59     : Binary Group Flag Bit 2 (always zero)
            // Bits 60-63 : User Bits 8 (all zero)
            // Bits 64-79 : Sync Word (0011 1111 1111 1101)

            if( drop ) word[ 1 ] |= (byte) 0x04;
            word[ 8 ]		= (byte) 0xFC; // NOT: 0x3F;
            word[ 9 ]		= (byte) 0xBF;
            amp				= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value;
            phase			= -1;
            sampleOffset	= 0;
            sampleOffsetR	= 0;
//			y1 = 0; y2 = 0; y3 = 0;

            while( threadRunning && (sampleOffsetR < outLength) ) {
                chunkLen = Math.min( framesPerBuf, numStopFrames - frameOffset );
                inBufOff = inBufPre;
                for( int i = 0; i < chunkLen; i++ ) {
                    // if( drop ) ... XXX
                    bcd( framesStart % 10, word, 0 );
                    bcd( framesStart / 10, word, 8, 2 );
                    bcd( secsStart % 10, word, 16 );
                    bcd( secsStart / 10, word, 24, 3 );
                    bcd( minsStart % 10, word, 32 );
                    bcd( minsStart / 10, word, 40, 3 );
                    bcd( hoursStart % 10, word, 48 );
                    bcd( hoursStart / 10, word, 56, 2 );

                    for( int j = 0; j < 10; j++ ) {
                        for( int m = 0, n = word[ j ]; m < 8; m++, n >>= 1 ) {
                            phase = -phase;
                            for( int p = 0; p < smpsPerHalfbit; p++ ) buf[ inBufOff++ ] = phase;
                            if( (n & 1) == 1 ) phase = -phase;
                            for( int p = 0; p < smpsPerHalfbit; p++ ) buf[ inBufOff++ ] = phase;
                        }
                    }

                    // increment
                    if( ++framesStart == frames ) {
                        framesStart = 0;
                        if( ++secsStart == 60 ) {
                            secsStart = 0;
                            if( ++minsStart == 60 ) {
                                minsStart = 0;
                                if( ++hoursStart == 24 ) {
                                    hoursStart = 0;
                                }
                            }
                        }
                    }
                }

                for( int i = inBufOff; i < buf.length; i++ ) {
                    buf[ i ] = 0f;
                }
                // low pass filter
                for( int i = inBufPre; i < buf.length; i++ ) {
                    yold	 = yold + (alpha * (buf[ i ] - yold));
                    buf[ i ] = yold * amp;
                }

                // resample using cubic interolation
                d1	= sampleOffsetR * rf - sampleOffset + 1;
//				d1	= sampleOffset * rf - sampleOffsetR;
                outBufOff = 0;
//				assert ((int) d1) == 0;
//if( (int) d1 != 0 ) {
//	System.out.println( "for sampleOffset " + sampleOffset + "; sampleOffsetR " + sampleOffsetR + "; rf " + rf + "; d1 is " + d1 );
//}

//				y1	= buf[ 0 ];
//				y2	= buf[ 1 ];
//				y3	= buf[ 2 ];
                for( int j = (int) d1; j < inBufLen - inBufPre; outBufOff++ ) {
                    y0	= buf[ j - 1 ];
                    y1	= buf[ j ];
                    y2	= buf[ j + 1];
                    y3	= buf[ j + 2 ];
                    mu	= (float) (d1 % 1.0);
                    mu2	= mu * mu;
                    a0	= y3 - y2 - y0 + y1;
                    a1	= y0 - y1 - a0;
                    a2	= y2 - y0;
                    a3	= y1;
                    bufR[ outBufOff ] = a0 * mu * mu2 + a1 * mu2 + a2 * mu + a3;
//					bufR[ chunkLenR ] = y0 * (1f - mu) + y1 * mu;
                    d1 += rf;
                    j	= (int) d1;
                }
                // handle overlap
                buf[ 0 ] = buf[ inBufLen - 2 ];
                buf[ 1 ] = buf[ inBufLen - 1 ];
//				buf[ 2 ] = y3;

//				sampleOffsetR	+= chunkLenR;
//				chunkLenR		 = (int) Math.min( chunkLenR - outBufOff, outLength - sampleOffsetR );
                outBufOff		 = (int) Math.min( outBufOff, outLength - sampleOffsetR + outBufPre );
                outF.writeFrames( outBuf, outBufPre, outBufOff - outBufPre );
                frameOffset		+= chunkLen;
                sampleOffset	+= (inBufOff - inBufPre);
                sampleOffsetR	+= outBufOff;
                outBufPre		 = 0;
                progOff			+= outBufOff;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            outF.close();
            outF = null;
        // .... check running ....
            if( !threadRunning ) break topLevel;

        }
        catch( ParseException e1 ) {
            setError( e1 );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }

    // ---- cleanup ----
        if( outF != null ) {
            outF.cleanUp();
        }
    }

// -------- private Methods --------

    private void bcd( int units, byte[] word, int bitOffset )
    {
        bcd( units, word, bitOffset, 4 );
    }

    private void bcd( int units, byte[] word, int bitOffset, int numBits )
    {
        final int	unitsShift	= (bitOffset & 7);
//		final int	unitsMask	= ~(0x0F << unitsShift);
        final int	unitsMask	= ~(((1 << numBits) - 1) << unitsShift);
        final int	unitsEnc	= units << unitsShift;
        final int	unitsIdx	= bitOffset >> 3;

        word[ unitsIdx ]		= (byte) (word[ unitsIdx ] & unitsMask | unitsEnc);
        word[ unitsIdx + 1 ]	= (byte) (word[ unitsIdx + 1 ] & (unitsMask >> 8) | (unitsEnc >> 8));
    }

}