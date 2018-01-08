/*
 *  ChebychevDlg.java
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
 *		21-May-05	finally implemented frequency and time interpolation
 *		31-Aug-05	uses new temp file style, added drop-envelope-tracking option
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 *  Processing module for multi-band waveshaping using
 *	chebychev polynomials to describe the amount of
 *	each overtone.
 */
public class ChebychevDlg
        extends ModulePanel {
// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE			= 0;		// pr.text
    private static final int PR_OUTPUTFILE			= 1;
    private static final int PR_SPECTSLIDER1		= 2;
    private static final int PR_SPECTSLIDER2		= 3;
    private static final int PR_SPECTSLIDER3		= 4;
    private static final int PR_SPECTSLIDER4		= 5;
    private static final int PR_SPECTSLIDER5		= 6;
    private static final int PR_SPECTSLIDER6		= 7;
    private static final int PR_SPECTSLIDER7		= 8;
    private static final int PR_SPECTSLIDER8		= 9;
    private static final int PR_SPECTSLIDER9		= 10;
    private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
    private static final int PR_OUTPUTRES			= 1;
    private static final int PR_GAINTYPE			= 2;
    private static final int PR_FILTERLEN			= 3;
    private static final int PR_GAIN				= 0;		// pr.para
    private static final int PR_LOFREQ				= 1;
    private static final int PR_MIDFREQ				= 2;
    private static final int PR_HIFREQ				= 3;
    private static final int PR_MIDTIME				= 4;
    private static final int PR_DRYMIX				= 5;
    private static final int PR_WETMIX				= 6;
    private static final int PR_ROLLOFF				= 7;
    private static final int PR_BANDSPEROCT			= 8;
//	private static final int PR_DROPENV				= 0;		// pr.bool

//	private static final int FLT_SHORT				= 0;
//	private static final int FLT_MEDIUM				= 1;
    private static final int FLT_LONG				= 2;
//	private static final int FLT_VERYLONG			= 3;

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_SPECTSLIDER1	= "Shape1";
    private static final String PRN_SPECTSLIDER2	= "Shape2";
    private static final String PRN_SPECTSLIDER3	= "Shape3";
    private static final String PRN_SPECTSLIDER4	= "Shape4";
    private static final String PRN_SPECTSLIDER5	= "Shape5";
    private static final String PRN_SPECTSLIDER6	= "Shape6";
    private static final String PRN_SPECTSLIDER7	= "Shape7";
    private static final String PRN_SPECTSLIDER8	= "Shape8";
    private static final String PRN_SPECTSLIDER9	= "Shape9";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_FILTERLEN		= "FilterLen";
    private static final String PRN_LOFREQ			= "LoFreq";
    private static final String PRN_MIDFREQ			= "MidFreq";
    private static final String PRN_HIFREQ			= "HiFreq";
    private static final String PRN_MIDTIME			= "MidTime";
    private static final String PRN_DRYMIX			= "DryMix";
    private static final String PRN_WETMIX			= "WetMix";
    private static final String PRN_ROLLOFF			= "RollOff";
    private static final String PRN_BANDSPEROCT		= "BandsPerOct";
    private static final String PRN_DROPENV			= "DropEnv";

    private static final int SLIDER_NUM = 9;
    private static final int MAX_HARMON = 8;

    private static final String	prText[]			= new String[ PR_SPECTSLIDER1 + SLIDER_NUM ];
    private static final String	prTextName[]		= { PRN_INPUTFILE, PRN_OUTPUTFILE,
                                                        PRN_SPECTSLIDER1, PRN_SPECTSLIDER2,
                                                        PRN_SPECTSLIDER3, PRN_SPECTSLIDER4,
                                                        PRN_SPECTSLIDER5, PRN_SPECTSLIDER6,
                                                        PRN_SPECTSLIDER7, PRN_SPECTSLIDER8,
                                                        PRN_SPECTSLIDER9 };
    private static final int		prIntg[]		= { 0, 0, 0, FLT_LONG };
    private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
                                                        PRN_FILTERLEN };
    private static final Param	prPara[]			= new Param[ 9 ];
    private static final String	prParaName[]		= { PRN_GAIN, PRN_LOFREQ, PRN_MIDFREQ, PRN_HIFREQ, PRN_MIDTIME,
                                                        PRN_DRYMIX, PRN_WETMIX, PRN_ROLLOFF, PRN_BANDSPEROCT };
    private static final boolean	prBool[]		= { false };
    private static final String	prBoolName[]		= { PRN_DROPENV };

    private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_FILTERLEN			= GG_OFF_CHOICE		+ PR_FILTERLEN;
//	private static final int GG_DROPENV				= GG_OFF_CHECKBOX	+ PR_DROPENV;
    private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_LOFREQ				= GG_OFF_PARAMFIELD	+ PR_LOFREQ;
    private static final int GG_MIDFREQ				= GG_OFF_PARAMFIELD	+ PR_MIDFREQ;
    private static final int GG_HIFREQ				= GG_OFF_PARAMFIELD	+ PR_HIFREQ;
    private static final int GG_MIDTIME				= GG_OFF_PARAMFIELD	+ PR_MIDTIME;
//	private static final int GG_DRYMIX				= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
//	private static final int GG_WETMIX				= GG_OFF_PARAMFIELD	+ PR_WETMIX;
//	private static final int GG_ROLLOFF				= GG_OFF_PARAMFIELD	+ PR_ROLLOFF;
    private static final int GG_BANDSPEROCT			= GG_OFF_PARAMFIELD	+ PR_BANDSPEROCT;
    private static final int GG_PANEL				= GG_OFF_OTHER		+ 0;
    private static final int GG_SPECTSLIDER1		= GG_OFF_OTHER		+ PR_SPECTSLIDER1;	// thru 9 !

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    protected JButton ggToTime, ggToFreq;

// -------- public methods --------

	public ChebychevDlg() {
		super("Chebyshev Waveshaping");
		init2();
	}

	protected void buildGUI() {
		if( static_pr == null ) {
            static_pr			= new PropertyArray();
            static_pr.text		= prText;
            static_pr.textName	= prTextName;
            static_pr.text[ PR_INPUTFILE ]		= "";
            static_pr.text[ PR_OUTPUTFILE ]		= "";
            for( int i = 0; i < SLIDER_NUM; i++ ) {
                static_pr.text[ PR_SPECTSLIDER1 + i ] = "1.0,0,0,0,0,0,0,0";
            }
            static_pr.intg		= prIntg;
            static_pr.intgName	= prIntgName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.para		= prPara;
            static_pr.para[ PR_LOFREQ ]			= new Param(  400.0, Param.ABS_HZ );
            static_pr.para[ PR_MIDFREQ ]		= new Param( 2000.0, Param.ABS_HZ );
            static_pr.para[ PR_HIFREQ ]			= new Param( 9000.0, Param.ABS_HZ );
            static_pr.para[ PR_DRYMIX ]			= new Param(  100.0, Param.FACTOR_AMP );
            static_pr.para[ PR_WETMIX ]			= new Param(   25.0, Param.FACTOR_AMP );
            static_pr.para[ PR_ROLLOFF ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
            static_pr.para[ PR_BANDSPEROCT ]	= new Param(   36.0, Param.NONE );
            static_pr.para[ PR_MIDTIME ]		= new Param(   50.0, Param.FACTOR_TIME );
            static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
            fillDefaultGain( static_pr.para, PR_GAIN );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggOutputFile;
        PathField[]			ggParent1;
        JComboBox			ggFltLen;
        ParamField			ggLoFreq, ggMidFreq, ggHiFreq, ggBandsPerOct, ggMidTime;
        ParamSpace[]		spcHiCut, spcMidTime;
        Component[]			ggGain;
        SpectSlider			ggShape;
        JPanel				shapePanel;
        JPanel				p2;
        Box					b;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

        ActionListener al = new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                SpectSlider ggSpect = null;
                int focussed;

                for( focussed = 0; focussed < SLIDER_NUM; focussed++ ) {
                    ggSpect = (SpectSlider) gui.getItemObj( GG_SPECTSLIDER1 + focussed );
                    if( ggSpect.hasFocus() ) break;
                }
                if( focussed == SLIDER_NUM ) return;

                float[] bands = ggSpect.getBands();
                int col = focussed % 3;
                int row = focussed / 3;

                if( e.getSource() == ggToTime ) {
                    for( int i = col; i < SLIDER_NUM; i += 3 ) {
                        if( i != focussed ) ((SpectSlider) gui.getItemObj( GG_SPECTSLIDER1 + i )).setBands( bands );
                    }
                } else if( e.getSource() == ggToFreq ) {
                    for( int i = row * 3; i < (row + 1) * 3; i++ ) {
                        if( i != focussed ) ((SpectSlider) gui.getItemObj( GG_SPECTSLIDER1 + i )).setBands( bands );
                    }
                }
            }
        };

    // -------- I/O-Gadgets --------
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;
    gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select input file" );
        ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInputFile, GG_INPUTFILE, null );

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
        gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

        ggParent1		= new PathField[ 1 ];
        ggParent1[ 0 ]	= ggInputFile;
        ggOutputFile.deriveFrom( ggParent1, "$D0$F0Chby$E" );

        ggGain			= createGadgets( GGTYPE_GAIN );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

    // -------- Settings-Gadgets --------
    gui.addLabel( new GroupLabel( "Shape Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggLoFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
        con.weightx		= 0.1;
        con.weighty		= 0.0;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Low Freq", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggLoFreq, GG_LOFREQ, null );

        spcHiCut		= new ParamSpace[ 3 ];
        spcHiCut[0]		= Constants.spaces[ Constants.absHzSpace ];
        spcHiCut[1]		= Constants.spaces[ Constants.offsetHzSpace ];
        spcHiCut[2]		= Constants.spaces[ Constants.offsetSemitonesSpace ];

        ggMidFreq		= new ParamField( spcHiCut );
        ggMidFreq.setReference( ggLoFreq );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Mid Freq", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggMidFreq, GG_MIDFREQ, null );

        ggHiFreq		= new ParamField( spcHiCut );
        ggHiFreq.setReference( ggLoFreq );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "High Freq", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggHiFreq, GG_HIFREQ, null );

        shapePanel	= new JPanel( new GridLayout( 3, 3, 4, 4 ));
        p2			= new JPanel( new BorderLayout() );
        b			= Box.createVerticalBox();
        for( int i = 0; i < SLIDER_NUM; i++ ) {
            ggShape = new SpectSlider( MAX_HARMON, 32 /* 59 */, 59 );
            shapePanel.add( ggShape );
            gui.registerGadget( ggShape, GG_SPECTSLIDER1 + i );
        }
        con.weightx		= 1.0;
        con.weighty		= 1.0;
        p2.add( shapePanel, BorderLayout.CENTER );
        ggToTime		= new JButton( "\u2192all times" );
        ggToTime.setFocusable( false );
        ggToTime.addActionListener( al );
        ggToFreq		= new JButton( "\u2192all freq" );
        ggToFreq.setFocusable( false );
        ggToFreq.addActionListener( al );
        b.add( ggToTime );
        b.add( ggToFreq );
        p2.add( b, BorderLayout.EAST );
        gui.addGadget( p2, GG_PANEL );

        spcMidTime		= new ParamSpace[ 3 ];
        spcMidTime[0]	= Constants.spaces[ Constants.absMsSpace ];
        spcMidTime[1]	= Constants.spaces[ Constants.absBeatsSpace ];
        spcMidTime[2]	= Constants.spaces[ Constants.factorTimeSpace ];

        ggMidTime		= new ParamField( spcMidTime );
        con.weightx		= 0.1;
        con.weighty		= 0.0;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Mid Time", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggMidTime, GG_MIDTIME, null );

        ggBandsPerOct	= new ParamField( new ParamSpace( 1.0, 256.0, 1.0, Param.NONE ));
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Bands per Oct.", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggBandsPerOct, GG_BANDSPEROCT, null );

        ggFltLen		= new JComboBox();
        ggFltLen.addItem( "Short" );
        ggFltLen.addItem( "Medium" );
        ggFltLen.addItem( "Long" );
        ggFltLen.addItem( "Very long" );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Filter length", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggFltLen, GG_FILTERLEN, null );

//		ggDropEnv		= new JCheckBox();
//		con.weightx		= 0.1;
//		gui.addLabel( new JLabel( "Drop Envelope Tracking", SwingConstants.RIGHT ));
//		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addCheckbox( ggDropEnv, GG_DROPENV, il );

        initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
    }

	public void fillGUI() {
		super.fillGUI();
		super.fillGUI(gui);

		SpectSlider ggSpect;

		for (int i = 0; i < SLIDER_NUM; i++) {
			ggSpect = (SpectSlider) gui.getItemObj(GG_SPECTSLIDER1 + i);
			if (ggSpect != null) {
				ggSpect.setBands(SpectSlider.valueOf(pr.text[PR_SPECTSLIDER1 + i]));
			}
		}
	}

	public void fillPropertyArray() {
		super.fillPropertyArray();
		super.fillPropertyArray(gui);

		SpectSlider ggSpect;

		for (int i = 0; i < SLIDER_NUM; i++) {
			ggSpect = (SpectSlider) gui.getItemObj(GG_SPECTSLIDER1 + i);
			if (ggSpect != null) {
				pr.text[PR_SPECTSLIDER1 + i] = ggSpect.toString();
			}
		}
	}

// -------- Processor Interface --------


    /*
     *	How it works: input is low and highpass filtered. low/high are processed separately as
     *	"dry parts", the difference from the original signal is fed to the shaper stage: it is
     *	split up into a user defined number of bands per octave (cosine-modulated sinc FIR filters
     *	with variable taps) -> peak amp. for each band is tracked; gain is normalized, chebychev
     *	polynomial distortion is applied and original gain restored.
     *
     *	idea taken from fernandez (dafx/cost), multiband waveshaping
     */

// XXX  TODO: SpectSlider / ...

	protected void process() {
		int					i, j, k, m, len, off, ch, chunkLength, chunkLength2, band;
        long				progOff, progLen;
        double				d1, d2, loFreq, hiFreq, midFreq, freqFactor;
        float				f1, f2, f3, weight;
        boolean				beforeMidTime;

        // io
        AudioFile			inF				= null;
        AudioFile			outF			= null;
        AudioFileDescr		inStream;
        AudioFileDescr		outStream;
        AudioFile			tempF			= null;

        // buffers
        float[]				lowpass, highpass, hpFFTBuf, lpFFTBuf, dwFFTBuf, convFFTBuf;
        float[][]			shpFFTBuf, lpOverBuf, hpOverBuf, sliceFFTBuf;
        float[][][]			convOverBuf;
        float[]				convBuf1, convBuf2, convBuf3;
        float[]				dwWin, shpLowWin, shpHighWin;
        float[][]			inBuf, outBuf;

        int					inChanNum, inLength, inputLen, outSkip;
        int					framesRead, framesWritten;

        Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
        float				gain			= 1.0f;								 		// gain abs amp
        float				maxAmp			= 0.0f;

        PathField			ggOutput;

        int					dwNumPeriods, dwHalfWinSize, dwFltLength, dwFFTLength, dwOverLen;
        int					shpNumPeriods, shpMinHalfWin, shpMaxHalfWin, shpHighHalfWin, shpLowHalfWin;
        int					shpMinFFTLen, shpMaxFFTLen, shpFFTLen, shpFFTLenIdx, numShpLen, shpFFTCounter;
        int					inputConv = 0, convNum = 0, convOverLen = 0, convShift;
        float[]				crossFreqs, cosineFreqs;
        int[]				shpFFTLength, shpFFTCount, shpHalfWinSize, shpConvNum;
        double				freqNorm, freqBase, cosineNorm, cosineBase, cosineBaseSqr;
        float				nyquist;

        int					numBands;
        int					midTime;		// in samples

        // drove my cheby to the leby
        ChebyTracker[][]	chebys;
        ChebyTracker		chebyTrk;
        float[]				chbPeakBuf;		// see ChebyTracker class description!
        int					chbPeakBufLen, chbPeakBufStart, chbPeakBufOff, chbPeakValid, chbPeakScope;
        float				chbPeakValue;
        boolean				chbPeakBufCont;

        float[]				chbHarmonWeight1, chbHarmonWeight2, chbHarmonWeight3,
                            chbHarmonWeight4, chbHarmonWeight5, chbHarmonWeight6,
                            chbHarmonWeight7, chbHarmonWeight8, chbHarmonWeight9;
        int					chbHarmonNum;
        float				chbT0, chbT1, chbT2, chbTn;

        float				dryGain;

        final boolean		dropEnv	= false; // no marche pas

	topLevel:
		try {

        // ---- open input, output ----

            // input
            inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
            inStream		= inF.getDescr();
            inChanNum		= inStream.channels;
            inLength		= (int) inStream.length;
            // this helps to prevent errors from empty files!
            if( (inLength * inChanNum) < 1 ) throw new EOFException( ERR_EMPTY );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // output
            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream	= new AudioFileDescr( inStream );
            ggOutput.fillStream( outStream );
            outF		= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;



        // ---- calculate filters ----

            loFreq				= Param.transform( pr.para[ PR_LOFREQ ], Param.ABS_HZ, null, null ).value;
            midFreq				= Param.transform( pr.para[ PR_MIDFREQ ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).value;
            hiFreq				= Math.min( inStream.rate * 0.2475,
                                  Param.transform( pr.para[ PR_HIFREQ ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).value);
            freqFactor			= Math.pow( 2.0, 1.0 / pr.para[ PR_BANDSPEROCT ].value);
            nyquist				= (float) (inStream.rate * 0.49);	// well, almost ;)

            numBands			= (int) (Math.log( hiFreq / loFreq ) / Math.log( freqFactor ));
            crossFreqs			= new float[ numBands+1 ];
            cosineFreqs			= new float[ numBands+1 ];
            shpHalfWinSize		= new int[ numBands ];

            d1					= loFreq;
            d2			    	= d1 * freqFactor;

            midTime				= (int) (AudioFileDescr.millisToSamples( inStream, Param.transform(
                                    pr.para[ PR_MIDTIME ], Param.ABS_MS, new Param(
                                        AudioFileDescr.samplesToMillis( inStream, inLength ), Param.ABS_MS ),
                                            null ).value) + 0.5);
            midTime				= Math.max( 1, Math.min( midTime, inLength - 1 ));

        // ---- calc bandpass freqs ----

            for( i = 0; i <= numBands; i++ ) {
                crossFreqs[i]	= (float) d1;
                cosineFreqs[i]	= (float) (Math.sqrt( d2 * d1 ) - d1);
                d1			   	= d2;
                d2			    = d1 * freqFactor;
            }
            hiFreq				= crossFreqs[numBands];

            dwNumPeriods	= 6; // << pr.intg[ PR_FILTERLEN ];
            dwHalfWinSize	= Math.max( 1, (int) ((double) dwNumPeriods * inStream.rate / loFreq + 0.5) );
            freqNorm		= Constants.PI2 / inStream.rate;
            cosineNorm		= 4.0 / (Math.PI*Math.PI);
            dwFltLength		= dwHalfWinSize + dwHalfWinSize;

            j				= dwFltLength + dwFltLength - 1;
            for( dwFFTLength = 2; dwFFTLength < j; dwFFTLength <<= 1 ) ;
            inputLen		= dwFFTLength - dwFltLength + 1;
            dwOverLen		= dwFFTLength - inputLen;

        // ---- calc highpass filter ----
            // use a narrow window here to decrease time smearing!! (large window calculated in lowpass section)
            k				= Math.max( 1, (int) ((double) dwNumPeriods * inStream.rate / hiFreq + 0.5) );
            dwWin			= Filter.createFullWindow( k << 1, Filter.WIN_BLACKMAN );
            freqBase		= freqNorm * (inStream.rate/2);
            highpass			= new float[ dwFFTLength + 2 ];
            Util.clear( highpass );
            highpass[ dwHalfWinSize ] = (float) freqBase;

            freqBase		= freqNorm * hiFreq * 0.8; // / rollOff;
            cosineBase		= freqNorm * hiFreq * 0.04; // (1.0 - 1.0 / rollOff);
            cosineBaseSqr	= cosineBase * cosineBase;

            for( j = 1; j < k; j++ ) {
                d1							= (Math.sin( freqBase * j ) / (double) j);
                // raised cosine modulation
                d2							= cosineNorm * cosineBaseSqr * j * j;
                d1						   *= (Math.cos( cosineBase * j ) / (1.0 - d2));
                highpass[ dwHalfWinSize+j ]-= (float) d1;
                highpass[ dwHalfWinSize-j ]-= (float) d1;
            }
            highpass[ dwHalfWinSize ] -= (float) freqBase;

            Util.mult( dwWin, 0, highpass, dwHalfWinSize - k, dwWin.length );

            Fourier.realTransform( highpass, dwFFTLength, Fourier.FORWARD );
            Util.mult( highpass, 0, highpass.length, (float) (1.0 / Math.PI) );

// for( i = 0; i < highpass.length; i += 2 ) {
//  	highpass[ i ] = (float) Math.sqrt( highpass[ i ]*highpass[ i ]+highpass[ i+1 ]*highpass[ i+1 ] );
// 	highpass[ i+1 ] = highpass[ i ];
// }
// highpass[ (int) (hiFreq / inStream.smpRate * 2 * highpass.length) ] = -1.0f;
// Debug.view( highpass, "highpass" );

        // ---- calc lowpass filter ----
            dwWin			= Filter.createFullWindow( dwFltLength, Filter.WIN_BLACKMAN );
            freqBase		= freqNorm * (inStream.rate/2);
            lowpass		= new float[ dwFFTLength + 2 ];
            Util.clear( lowpass );
            lowpass[ dwHalfWinSize ] = (float) (Math.PI - freqBase);

            freqBase		= freqNorm * loFreq * 1.2;
            cosineBase		= freqNorm * loFreq * 0.04;
            cosineBaseSqr	= cosineBase * cosineBase;

            for( j = 1; j < dwHalfWinSize; j++ ) {
                d1							 = (Math.sin( freqBase * j ) / (double) j);
                // raised cosine modulation
                d2							 = cosineNorm * cosineBaseSqr * j * j;
                d1						    *= (Math.cos( cosineBase * j ) / (1.0 - d2));
                lowpass[ dwHalfWinSize+j ]   = (float) d1;
                lowpass[ dwHalfWinSize-j ]   = (float) d1;
            }
            lowpass[ dwHalfWinSize ] += (float) freqBase;

            // windowing
            Util.mult( dwWin, 0, lowpass, 0, dwFltLength );

            Fourier.realTransform( lowpass, dwFFTLength, Fourier.FORWARD );
            Util.mult( lowpass, 0, lowpass.length, (float) (1.0 / Math.PI) );

// for( i = 0; i < lowpass.length; i += 2 ) {
// 	lowpass[ i ] = (float) Math.sqrt( lowpass[ i ]*lowpass[ i ]+lowpass[ i+1 ]*lowpass[ i+1 ] );
// 	lowpass[ i+1 ] = lowpass[ i ];
// }
// lowpass[ (int) (loFreq / inStream.smpRate * 2 * lowpass.length) ] = -1.0f;
// Debug.view( lowpass, "lowpass" );

            // LP = +1.0 fc  -1.0 Zero
            // HP = +1.0 pi/2 -1.0 fc
            // BP = +1.0 fc2 -1.0 fc1

        // ---- calculate impulse response of the bandpasses + init tracker ----

            shpNumPeriods	= 3 << pr.intg[ PR_FILTERLEN ];
            shpMaxHalfWin	= Math.max( 1, (int) ((double) shpNumPeriods * inStream.rate / crossFreqs[0] + 0.5) );
            shpMinHalfWin	= Math.max( 1, (int) ((double) shpNumPeriods * inStream.rate / crossFreqs[numBands-1] + 0.5) );
            i				= shpMinHalfWin + shpMinHalfWin;
            j				= i + i - 1;
            for( shpMinFFTLen = 2; shpMinFFTLen < j; shpMinFFTLen <<= 1 ) ;		// FFTlength for shortest BP
            i				= shpMaxHalfWin + shpMaxHalfWin;
            j				= i + i - 1;
            for( shpMaxFFTLen = 2; shpMaxFFTLen < j; shpMaxFFTLen <<= 1 ) ;		// FFTlength for longest BP
            for( numShpLen = 1, i = shpMinFFTLen; i < shpMaxFFTLen; i <<= 1, numShpLen ++ ) ;	// how many different FFT lengths?

            shpFFTLength	= new int[ numShpLen ];		// each of the different lengths
            shpFFTCount		= new int[ numShpLen ];		// how many successive FFT of each FFT length
            shpConvNum		= new int[ numShpLen ];
            shpFFTBuf		= new float[ numBands ][];
            convFFTBuf		= new float[ shpMaxFFTLen + 2 ];
            convOverBuf		= new float[ inChanNum ][ numBands ][];

            shpFFTLen		= -1;	// shpMaxFFTLen;
            shpFFTLenIdx	= -1;
            shpFFTLength[0]	= shpFFTLen;
            shpFFTCount[0]	= 0;
            i				= shpMaxFFTLen - (shpMaxHalfWin << 1) + 1;
            shpConvNum[0]	= (inputLen + i - 1) / i;
            shpHighHalfWin	= shpMaxHalfWin;
            shpHighWin		= Filter.createFullWindow( shpHighHalfWin << 1, Filter.WIN_BLACKMAN );

            chebys				= new ChebyTracker[ inChanNum ][ numBands ];
            chbHarmonWeight1	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER1 ]);
            chbHarmonWeight2	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER2 ]);
            chbHarmonWeight3	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER3 ]);
            chbHarmonWeight4	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER4 ]);
            chbHarmonWeight5	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER5 ]);
            chbHarmonWeight6	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER6 ]);
            chbHarmonWeight7	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER7 ]);
            chbHarmonWeight8	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER8 ]);
            chbHarmonWeight9	= SpectSlider.valueOf( pr.text[ PR_SPECTSLIDER9 ]);

            for( band = 0; band < numBands; band++ ) {

                shpLowHalfWin	= shpHighHalfWin;
                shpLowWin		= shpHighWin;
                shpHalfWinSize[band]=shpLowHalfWin;
                shpHighHalfWin	= Math.max( 1, (int) ((double) shpNumPeriods * inStream.rate / crossFreqs[band+1] + 0.5) );
                shpHighWin		= Filter.createFullWindow( shpHighHalfWin << 1, Filter.WIN_BLACKMAN );

                j				= ((shpLowHalfWin + shpLowHalfWin) << 1) - 1;
                for( k = 2; k < j; k <<= 1 ) ;		// current FFTlength
                convBuf1		= new float[ k + 2 ];
                shpFFTBuf[band]	= convBuf1;

// System.out.println( "Band "+band+" (freq "+crossFreqs[band]+" ... "+crossFreqs[band+1]+") => halfwin "+shpLowHalfWin+"/"+shpHighHalfWin );

                if( k != shpFFTLen ) {
// System.out.println( "new FFT size "+k );
                    shpFFTLen = k;
                    shpFFTLenIdx++;
                    shpFFTLength[ shpFFTLenIdx ]	= shpFFTLen;
                    j								= shpFFTLen - (shpLowHalfWin << 1) + 1;
                    shpConvNum[ shpFFTLenIdx ]		= (inputLen + j - 1) / j;
                }
                shpFFTCount[ shpFFTLenIdx ]++;

                // overlap buffer
                inputConv	= shpFFTLen - (shpLowHalfWin << 1) + 1;
                convOverLen	= shpFFTLen - inputConv;
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convOverBuf[ ch ][ band ] = new float[ convOverLen ];
                }

                // first part of filter
                if( band < numBands-1 ) {	// damn overhead for highest band (we don't want an extra slope, we have the hp-slope already)
                    freqBase		= freqNorm * crossFreqs[ band+1 ];
                    cosineBase		= freqNorm * cosineFreqs[ band+1 ];

                    cosineBaseSqr	= cosineBase * cosineBase;

                    for( j = 1; j < shpHighHalfWin; j++ ) {
                        // sinc-filter
                        d1								= (Math.sin( freqBase * j ) / (double) j);
                        // raised cosine modulation
                        d2								= cosineNorm * cosineBaseSqr * j * j;
                        d1						 	   *= (Math.cos( cosineBase * j ) / (1.0 - d2));
                        convBuf1[ shpLowHalfWin+j ]		= (float) d1 * shpHighWin[ shpHighHalfWin+j ];
                        convBuf1[ shpLowHalfWin-j ]		= (float) d1 * shpHighWin[ shpHighHalfWin-j ];
                    }
                    convBuf1[ shpLowHalfWin ] = (float) freqBase * shpHighWin[ shpHighHalfWin ];

                } else {
                    Util.clear( convBuf1 );
                    convBuf1[ shpLowHalfWin ] = (float) Math.PI * shpHighWin[ shpHighHalfWin ];
                }

                // second part of filter
                if( band > 0 ) {		// damn overhead for lowest band (we don't want an extra slope, we have the lp-slope already)
                    freqBase		= freqNorm * crossFreqs[ band ];
                    cosineBase		= freqNorm * cosineFreqs[ band ];
                    cosineBaseSqr	= cosineBase * cosineBase;

                    for( j = 1; j < shpLowHalfWin; j++ ) {
                        d1							 = (Math.sin( freqBase * j ) / (double) j);
                        // raised cosine modulation
                        d2							 = cosineNorm * cosineBaseSqr * j * j;
                        d1						    *= (Math.cos( cosineBase * j ) / (1.0 - d2));
                        convBuf1[ shpLowHalfWin+j ] -= (float) d1 * shpLowWin[ shpLowHalfWin+j ];
                        convBuf1[ shpLowHalfWin-j ] -= (float) d1 * shpLowWin[ shpLowHalfWin-j ];
                    }
                    convBuf1[ shpLowHalfWin ] -= (float) freqBase * shpLowWin[ shpLowHalfWin ];

                    // zero padding
                    for( j = (shpLowHalfWin << 1); j < shpFFTLen; j++ ) {
                        convBuf1[ j ] = 0.0f;
                    }

                } // no else ;)

                // all filters are pre-transformed to fourier domain and normalized
                Fourier.realTransform( convBuf1, shpFFTLen, Fourier.FORWARD );
                Util.mult( convBuf1, 0, convBuf1.length, (float) (1.0 / Math.PI) );

            // ---- tracker init ----

                chebyTrk			= new ChebyTracker();
                f1					= (float) inStream.rate / crossFreqs[ band ]; // period [smp]
                chbPeakScope		= Math.max( 64, (int) (f1 * 1.5f + 0.5f) );	// scope 1.5 periods
                chbPeakBufLen		= /* dropEnv ? 1 :*/ (chbPeakScope << 1);
                chebyTrk.peakBufLen	= chbPeakBufLen;
                chebyTrk.peakScope	= chbPeakScope;
                chebyTrk.peakBuf	= new float[ chbPeakBufLen ];
                Util.clear( chebyTrk.peakBuf );
                chebyTrk.peakBufStart= 0;
                chebyTrk.peakBufOff	= chbPeakScope - 1;
                chebyTrk.peakValid	= 0;	// no valid peak
                chebyTrk.harmonWeight1	= new float[ MAX_HARMON ];
                chebyTrk.harmonWeight2	= new float[ MAX_HARMON ];
                chebyTrk.harmonWeight3	= new float[ MAX_HARMON ];

                if( dropEnv ) {
                    f1						= (float) band / (numBands - 1);
                    chebyTrk.peakBuf[ 0 ]	= 1.0e-4f; // 1.0f; // / (((1.0f - f1) * lowBoost) + f1 * highBoost);
                }

                for( i = 0; i < MAX_HARMON; i++ ) {
                    if( crossFreqs[ band+1 ] < nyquist ) {
                        if( crossFreqs[ band+1 ] < midFreq ) {	// -------- below mid freq --------
                            weight	= (float) ((crossFreqs[ band+1 ] - loFreq) / (midFreq - loFreq));
                            f1		= chbHarmonWeight1[ i ];
                            f2		= chbHarmonWeight2[ i ];
                            f1		= f1 * (1.0f - weight) + f2 * weight;
                            f3		= f1;
                            chebyTrk.harmonWeight1[ i ] = f1*f1*f1;

                            f1		= chbHarmonWeight4[ i ];
                            f2		= chbHarmonWeight5[ i ];
                            f1		= f1 * (1.0f - weight) + f2 * weight;
                            f3		= Math.max( f3, f1 );
                            chebyTrk.harmonWeight2[ i ] = f1*f1*f1;

                            f1		= chbHarmonWeight7[ i ];
                            f2		= chbHarmonWeight8[ i ];
                            f1		= f1 * (1.0f - weight) + f2 * weight;
                            f3		= Math.max( f3, f1 );
                            chebyTrk.harmonWeight3[ i ] = f1*f1*f1;

                        } else {								// -------- above mid freq --------
                            weight	= (float) ((hiFreq - crossFreqs[ band+1 ]) / (hiFreq - midFreq));
                            f1		= chbHarmonWeight2[ i ];
                            f2		= chbHarmonWeight3[ i ];
                            f1		= f1 * weight + f2 * (1.0f - weight);
                            f3		= f1;
                            chebyTrk.harmonWeight1[ i ] = f1*f1*f1;

                            f1		= chbHarmonWeight5[ i ];
                            f2		= chbHarmonWeight6[ i ];
                            f1		= f1 * weight + f2 * (1.0f - weight);
                            f3		= Math.max( f3, f1 );
                            chebyTrk.harmonWeight2[ i ] = f1*f1*f1;

                            f1		= chbHarmonWeight8[ i ];
                            f2		= chbHarmonWeight9[ i ];
                            f1		= f1 * weight + f2 * (1.0f - weight);
                            f3		= Math.max( f3, f1 );
                            chebyTrk.harmonWeight3[ i ] = f1*f1*f1;

                        }
                        if( f3 > 0f ) chebyTrk.harmonNum = i + 1;
                    } else {
                        chebyTrk.harmonWeight1[ i ] = 0f;
                        chebyTrk.harmonWeight2[ i ] = 0f;
                        chebyTrk.harmonWeight3[ i ] = 0f;
                    }
                }

                chebys[0][band]		= chebyTrk;
                for( ch = 1; ch < inChanNum; ch++ ) {
                    chebys[ch][band]= (ChebyTracker) chebyTrk.clone();
                }

            } // for bands

            numShpLen		= shpFFTLenIdx + 1;	// muss korrigiert werden, weil u.U. eine FFT-Laenge "uebersprungen" wurde!!

/*
float[] tmp = new float[ shpMaxFFTLen + 2];
float[] tmp2 = new float[ shpMaxFFTLen + 2];
for( band = 0; band < numBands; band++ ) {
    System.arraycopy( shpFFTBuf[band], 0, tmp2, 0, shpFFTBuf[band].length );
    Fourier.realTransform( tmp2, shpFFTBuf[band].length - 2, Fourier.INVERSE );
    Util.add( tmp2, 0, tmp, shpMaxFFTLen/2 - shpHalfWinSize[band], shpHalfWinSize[band]*2);
}
Debug.view( tmp, "Summed BPs" );
Fourier.realTransform( tmp, shpMaxFFTLen, Fourier.FORWARD );
for( i = 0; i < tmp.length; i+= 2 ) {
    tmp[ i ] = (float) Math.sqrt( tmp[ i ]*tmp[ i ]+tmp[ i+1 ]*tmp[ i+1 ] );
    tmp[ i+1 ] = tmp[ i ];
}
Debug.view( tmp, "Summed BPs FFT" );
*/
        // ---- further inits ----

            outBuf			= new float[ inChanNum ][ shpMaxHalfWin + inputLen ];
            dwFFTBuf		= new float[ dwFFTLength + 2 ];
            lpFFTBuf		= new float[ dwFFTLength + 2 ];
            hpFFTBuf		= new float[ dwFFTLength + 2 ];
            lpOverBuf		= new float[ inChanNum ][ dwOverLen ];
            hpOverBuf		= new float[ inChanNum ][ dwOverLen ];
            inBuf			= new float[ inChanNum ][ inputLen + dwHalfWinSize ];
            sliceFFTBuf		= new float[ shpConvNum[ numShpLen - 1 ]][ shpMaxFFTLen + 2 ];	// largets convNum x largest FFT size
            Util.clear( inBuf );

// System.out.println( "inputLen "+inputLen+"; dwFltLength "+dwFltLength+"; shpMaxFFTLen "+shpMaxFFTLen+"; shpMinFFTLen "+shpMinFFTLen+"; sliceFFTBuf dim "+(shpConvNum[ numShpLen - 1 ])+" x "+shpMaxFFTLen );

            progOff		= 0;
            progLen		= (long) inLength * (2 + inChanNum + inChanNum);
            m			= 0;	// java compiler complains if not initialized

            dryGain		= chebys[ 0 ][ 0 ].harmonWeight1[ 0 ];	// XXX

            // normalization requires temp files
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                tempF		= createTempFile( inStream );
                progLen	   += (long) inLength;
            } else {													// account for gain loss RealFFT => CmplxIFFT
                gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value);
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== the real stuff ====================----

            framesRead		= 0;
            framesWritten	= 0;
            outSkip			= dwHalfWinSize + shpMaxHalfWin;

            chbHarmonWeight3	= new float[ MAX_HARMON ];	// time interpolated

            while( threadRunning && (framesWritten < inLength) ) {

                chunkLength	 = Math.min( inputLen, inLength - framesRead );
                chunkLength2 = Math.min( inputLen, inLength - framesWritten );
            // ---- read input chunk ----
                for( off = 0; threadRunning && (off < chunkLength); ) {
                    len	= Math.min( 8192, chunkLength - off );
                    inF.readFrames( inBuf, off + dwHalfWinSize, len );		// inBuf delayed by dwHalfWinSize
                    framesRead	+= len;
                    progOff		+= len;
                    off			+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

                beforeMidTime	= framesRead <= midTime;
                if( beforeMidTime ) {
                    weight		= (float) framesRead / (float) midTime;
                } else {
                    weight		= (float) (framesRead - midTime) / (float) (inLength - midTime);
                }

            // ---- channels loop -----------------------------------------------------------------------
                for( ch = 0; threadRunning && (ch < inChanNum); ch++ ) {

                // ---- dry/wet pre-filtering ----
                    convBuf1 = outBuf[ ch ];
                    convBuf2 = inBuf[ ch ];

                    System.arraycopy( convBuf2, dwHalfWinSize, dwFFTBuf, 0, chunkLength );	// inBuf copy without delay
                    for( i = chunkLength; i < dwFFTLength; i++ ) {
                        dwFFTBuf[ i ] = 0.0f;
                    }
                    Fourier.realTransform( dwFFTBuf, dwFFTLength, Fourier.FORWARD );		// input chunk -> FFT
                    Fourier.complexMult( lowpass, 0, dwFFTBuf, 0, lpFFTBuf, 0, lowpass.length );	// convolve with LP
                    Fourier.realTransform( lpFFTBuf, dwFFTLength, Fourier.INVERSE );		// back to time domain (delayed by dwHalfWinSize)
                    Util.add( lpOverBuf[ ch ], 0, lpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( lpFFTBuf, inputLen, lpOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    Fourier.complexMult( highpass, 0, dwFFTBuf, 0, hpFFTBuf, 0, highpass.length );	// convolve with HP
                    Fourier.realTransform( hpFFTBuf, dwFFTLength, Fourier.INVERSE );		// back to time domain (delayed by dwHalfWinSize)
                    Util.add( hpOverBuf[ ch ], 0, hpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( hpFFTBuf, inputLen, hpOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    // now the signal to be fed into the waveshaper is input (delayed by dwHalfWinSize)
                    // - lowpass - highpass
                    Util.sub( lpFFTBuf, 0, convBuf2, 0, inputLen );
                    Util.sub( hpFFTBuf, 0, convBuf2, 0, inputLen );

                    System.arraycopy( lpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );		// dry lp/hp to output
                    Util.add( hpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );
                    Util.mult( convBuf1, shpMaxHalfWin, inputLen, dryGain );

                // ---- bands loop ----------------------------------------------------------------------

                    shpFFTLenIdx	= -1;
                    shpFFTCounter	= 0;

                    for( band = 0; threadRunning && (band < numBands); band++ ) {

                        shpLowHalfWin	= shpHalfWinSize[ band ];

                        if( shpFFTCounter == 0) {
                            shpFFTLenIdx++;
                            shpFFTCounter	= shpFFTCount[ shpFFTLenIdx ];
                            shpFFTLen		= shpFFTLength[ shpFFTLenIdx ];
                            inputConv		= shpFFTLen - (shpLowHalfWin << 1) + 1;	// max. input chunk for convolution
                            convNum			= shpConvNum[ shpFFTLenIdx ];			// number of conv's per inputLen

                            for( i = 0, off = 0; i < convNum; i++ ) {			// FFT all the sliced input chunk pieces
                                len			= Math.min( inputConv, inputLen - off );
                                convBuf3	= sliceFFTBuf[ i ];
                                System.arraycopy( convBuf2, off, convBuf3, 0, len );
                                for( j = len; j < shpFFTLen; j++ ) {
                                    convBuf3[ j ] = 0.0f;
                                }
                                Fourier.realTransform( convBuf3, shpFFTLen, Fourier.FORWARD );
                                off		   += len;
                            }
                        }
                        shpFFTCounter--;

                        convOverLen		= (shpLowHalfWin << 1) - 1;				// =< inputConv !
                        convShift		= shpMaxHalfWin - shpLowHalfWin;		// align all filters with uniform dly of shpMaxHalfWin

                        // cheby init (read)
                        chebyTrk		= chebys[ ch ][ band ];
                        chbPeakBuf		= chebyTrk.peakBuf;
                        chbPeakBufLen	= chebyTrk.peakBufLen;
                        chbPeakBufStart	= chebyTrk.peakBufStart;
                        chbPeakBufOff	= chebyTrk.peakBufOff;
                        chbPeakValid	= chebyTrk.peakValid;
                        chbPeakValue	= chebyTrk.peakValue;
                        chbPeakScope	= chebyTrk.peakScope;
                        chbPeakBufCont	= chbPeakBufStart < chbPeakBufOff;	// true = continuous; false = wrapped search
                        chbHarmonNum	= chebyTrk.harmonNum;

                        if( beforeMidTime ) {
                            chbHarmonWeight1	= chebyTrk.harmonWeight1;
                            chbHarmonWeight2	= chebyTrk.harmonWeight2;
                        } else {
                            chbHarmonWeight1	= chebyTrk.harmonWeight2;
                            chbHarmonWeight2	= chebyTrk.harmonWeight3;
                        }
                        for( i = 0; i < MAX_HARMON; i++ ) {
                            chbHarmonWeight3[ i ]	= chbHarmonWeight1[ i ] * (1.0f - weight) +
                                                      chbHarmonWeight2[ i ] * weight;
                        }

                        // convolve slices, inverse transform, overlap-add
                        for( i = 0, off = 0; i < convNum; i++ ) {
// System.out.println( "conv "+i+"; off "+off+"; shpFFTBuf[ band ].length "+shpFFTBuf[ band ].length  );
                            Fourier.complexMult( shpFFTBuf[ band ], 0, sliceFFTBuf[ i ], 0, convFFTBuf, 0,
                                shpFFTBuf[ band ].length );
                            Fourier.realTransform( convFFTBuf, shpFFTLen, Fourier.INVERSE );	// back to time domain (delayed by shpLowHalfWin)

                            len		 = Math.min( inputConv, inputLen - off );
                            Util.add( convOverBuf[ ch ][ band ], 0, convFFTBuf, 0, convOverLen );	// add old overlap
                            System.arraycopy( convFFTBuf, len, convOverBuf[ ch ][ band ], 0, convOverLen );	// save new overlap

                        // ======== Cheby Core =================================================
                        // peak tracking + cheby shaping

                            if( dropEnv ) {
                                chbPeakValue	= chbPeakBuf[ 0 ];
                            }

                            for( j = 0; j < len; j++ ) {	// all samples in a slice

                                f1 = convFFTBuf[ j ];

                                if( !dropEnv ) {
                                    f2							= Math.abs( f1 );
                                    chbPeakBuf[ chbPeakBufOff ]	= f2;

                                    if( chbPeakValid > 0 ) {
                                        if( f2 >= chbPeakValue ) {
                                            chbPeakValue	= f2;
                                            chbPeakValid	= chbPeakScope;
                                        }
                                        chbPeakValid--;
                                    } else {
                                        chbPeakValue = -1f;	// any next value will be a first peak guess
                                        if( chbPeakBufCont ) {
                                            for( k = chbPeakBufStart; k <= chbPeakBufOff; k++ ) {
                                                if( chbPeakBuf[ k ] >= chbPeakValue ) {
                                                    chbPeakValue	= chbPeakBuf[ k ];
                                                    m				= k;
                                                }
                                            }
                                            chbPeakValid = m - chbPeakBufStart;
                                        } else {
                                            for( k = chbPeakBufStart; k < chbPeakBufLen; k++ ) {
                                                if( chbPeakBuf[ k ] >= chbPeakValue ) {
                                                    chbPeakValue	= chbPeakBuf[ k ];
                                                    m				= k;
                                                }
                                            }
                                            for( k = 0; k <= chbPeakBufOff; k++ ) {
                                                if( chbPeakBuf[ k ] >= chbPeakValue ) {
                                                    chbPeakValue	= chbPeakBuf[ k ];
                                                    m				= k;
                                                }
                                            }
                                            chbPeakValid = m - chbPeakBufStart;
                                            if( chbPeakValid < 0 ) chbPeakValid += chbPeakBufLen;
                                        }
                                    }

                                    // circular buffer track keeping
                                    chbPeakBufOff++;
                                    chbPeakBufStart++;
                                    if( chbPeakBufOff == chbPeakBufLen ) {
                                        chbPeakBufOff	= 0;
                                        chbPeakBufCont	= false;
                                    } else if( chbPeakBufStart == chbPeakBufLen ) {
                                        chbPeakBufStart	= 0;
                                        chbPeakBufCont	= true;
                                    }
                                }	// if( !dropEnv )

                            // ---- at this point 'chbPeakValue' is a good guess for the amplitude
                            // calc weighted cheby polynoms (T0 = 1, T1 = x, Tn= 2xTn-1 - Tn-2)
                                if( chbPeakValue > 0.00001f ) {	// -100 dB thresh XXX

                                    chbT1	= 1.0f;								// first polynom
                                    chbT0	= f1 / chbPeakValue;				// second polynom (x normalized)
                                    f2		= 2 * chbT0;						// (2x term in recursion)
                                    chbTn	= chbT0 * chbHarmonWeight3[0];		// f3 will be summed harmonics
                                    for( k = 1; k < chbHarmonNum; k++ ) {
                                        chbT2	= chbT1;
                                        chbT1	= chbT0;
                                        chbT0	= f2 * chbT1 - chbT2;
                                        chbTn  += chbT0 * chbHarmonWeight3[k];
                                    }

                                    if( chbPeakValue > 0.0001f ) {	// -80 dB thresh XXX
                                        convFFTBuf[j]	= chbTn * chbPeakValue;
                                    } else {
//										f2				= (chbPeakValue - 0.0001f) / 0.00009f;	// map to 0...1
                                        f2				= chbPeakValue / 0.0001f;	// map to 0...1
                                        convFFTBuf[j]	= chbTn * f2 * chbPeakValue +			// weighted sum cheby/harmon1
                                                          f1 * chbHarmonWeight3[0] * (1.0f - f2);
                                    }

                                } else {
                                    convFFTBuf[j] *= chbHarmonWeight3[0];
                                }
                            } // for slice samples

                        // =====================================================================

                            Util.add( convFFTBuf, 0, convBuf1, off + convShift, len );
                            off		+= len;
                        } // for slices

                        // cheby finish (write)
                        chebyTrk.peakBufStart	= chbPeakBufStart;
                        chebyTrk.peakBufOff		= chbPeakBufOff;
                        chebyTrk.peakValid		= chbPeakValid;
                        chebyTrk.peakValue		= chbPeakValue;

                    } // for bands
                // .... check running ....
                    if( !threadRunning ) break topLevel;

                    // overlap handling input
                    System.arraycopy( convBuf2, inputLen, convBuf2, 0, dwHalfWinSize );

                    progOff	 += chunkLength2 + chunkLength2;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                } // for chanNum
            // .... check running ....
                if( !threadRunning ) break topLevel;

                for( off = outSkip; threadRunning && (off < chunkLength2); ) {
                    len			   = Math.min( 8192, chunkLength2 - off );
                    if( tempF != null ) {
                        tempF.writeFrames( outBuf, off, len );
                    } else {
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            Util.mult( outBuf[ ch ], off, len, gain );
                        }
                        outF.writeFrames( outBuf, off, len );
                    }
                    progOff		  += len;
                    off			  += len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }

                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = outBuf[ ch ];
                    // max measurement
                    for( off = outSkip; off < chunkLength2; off++ ) {
                        f1 = Math.abs( convBuf1[ off ]);
                        if( f1 > maxAmp ) {
                            maxAmp = f1;
                        }
                    }
                    // overlap handling output
                    System.arraycopy( convBuf1, inputLen, convBuf1, 0, shpMaxHalfWin );
// for( i = shpMaxHalfWin; i < convBuf1.length; i++ ) convBuf1[ i ] = 0.0f;
                }

                if( outSkip > 0 ) {
                    framesWritten += Math.max( 0, chunkLength2 - outSkip );
                    outSkip		   = Math.max( 0, outSkip - chunkLength2 );
                } else {
                    framesWritten += chunkLength2;
                }
            } // until framesWritten == outLength
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== normalize output ====================----

            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).value;
                f1		 = 1.0f;
                normalizeAudioFile( tempF, outF, outBuf, gain, f1 );
                maxAmp	*= gain;

                deleteTempFile( tempF );
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            outF.close();
            outF		= null;
            outStream	= null;
            inF.close();
            inF			= null;
            inStream	= null;

            shpFFTBuf	= null;
            lpOverBuf	= null;
            hpOverBuf	= null;
            sliceFFTBuf	= null;
            convOverBuf	= null;
            convBuf1	= null;
            convBuf2	= null;
            convBuf3	= null;
            inBuf		= null;
            outBuf		= null;
            chebys		= null;

            // inform about clipping/ low level
            handleClipping( maxAmp );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            outStream	= null;
            lowpass		= null;
            highpass	= null;
            hpFFTBuf	= null;
            lpFFTBuf	= null;
            dwFFTBuf	= null;
            convFFTBuf	= null;
            shpFFTBuf	= null;
            lpOverBuf	= null;
            hpOverBuf	= null;
            sliceFFTBuf	= null;
            convOverBuf	= null;
            convBuf1	= null;
            convBuf2	= null;
            convBuf3	= null;
            inBuf		= null;
            outBuf		= null;
            chebys		= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( inF != null ) {
            inF.cleanUp();
            inF = null;
        }
        if( outF != null ) {
            outF.cleanUp();
            outF = null;
        }
    } // process()

// -------- internal ChebyTracker class --------

	private static class ChebyTracker {
		float[]	peakBuf;		// buffer holding the 3 recent periods of the band's (abs!) samples (circular updated!)
        int		peakBufLen, peakBufStart, peakBufOff;	// len = peakBuf.length; start...off = peak search area (increased per sample)
        int		peakValid;		// how often is the value still valid (until peak position leaves scope of 1.5 periods)
        float	peakValue;		// last valid peak value
        int		peakScope;		// how many samples are inspected for peak finding

        int		harmonNum;		// number of harmonics (including first)
        float[]	harmonWeight1;	// 0...1	beginning of performance
        float[]	harmonWeight2;	// 0...1	middle time
        float[]	harmonWeight3;	// 0...1	end of performance

        protected ChebyTracker() { /* empty */}

        protected ChebyTracker( ChebyTracker orig )
        {
            this.peakBuf		= (float[]) orig.peakBuf.clone();
            this.peakBufLen		= orig.peakBufLen;
            this.peakBufStart	= orig.peakBufStart;
            this.peakBufOff		= orig.peakBufOff;
            this.peakValid		= orig.peakValid;
            this.peakValue		= orig.peakValue;
            this.peakScope		= orig.peakScope;
            this.harmonNum		= orig.harmonNum;
            this.harmonWeight1	= (float[]) orig.harmonWeight1.clone();
            this.harmonWeight2	= (float[]) orig.harmonWeight2.clone();
            this.harmonWeight3	= (float[]) orig.harmonWeight3.clone();
        }

        public Object clone()
        {
            return new ChebyTracker( this );
        }
    } // class ChebyTracker
}