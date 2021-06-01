/*
 *  MosaicDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		20-Dec-08	created
 *		20-Jun-09	audio input loops
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.ConstQ;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.Marker;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *	Processing module for approaching a file (fit input)
 *	through evolution using a genetic algorithm.
 */
public class MosaicDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INIMGFILE			= 0;		// pr.text
    private static final int PR_INMATFILE			= 1;
    private static final int PR_OUTPUTFILE			= 2;
    private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
    private static final int PR_OUTPUTRES			= 1;
    private static final int PR_GAINTYPE			= 2;
    private static final int PR_FILTERTYPE			= 3;
    private static final int PR_GAIN				= 0;		// pr.para
    private static final int PR_MINFREQ				= 1;
    private static final int PR_MAXFREQ				= 2;
    private static final int PR_DURATION			= 3;
    private static final int PR_TIMEOVERLAP			= 4;
    private static final int PR_TIMEJITTER			= 5;
    private static final int PR_FREQOVERLAP			= 6;
    private static final int PR_FREQJITTER			= 7;
    private static final int PR_NOISEFLOOR			= 8;
    private static final int PR_MAXBOOST			= 9;
    private static final int PR_ATTACK				= 10;
    private static final int PR_RELEASE				= 11;
    private static final int PR_READMARKERS			= 0;		// pr.bool

    private static final String PRN_INIMGFILE		= "InImgFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_INMATFILE		= "InMatFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_FILTERTYPE		= "FilterType";
    private static final String PRN_MINFREQ			= "MinFreq";
    private static final String PRN_MAXFREQ			= "MaxFreq";
    private static final String PRN_DURATION		= "Duration";
    private static final String PRN_TIMEOVERLAP		= "TimeOverlap";
    private static final String PRN_TIMEJITTER		= "TimeJitter";
    private static final String PRN_FREQOVERLAP		= "FreqOverlap";
    private static final String PRN_FREQJITTER		= "FreqJitter";
    private static final String PRN_NOISEFLOOR		= "NoiseFloor";
    private static final String PRN_MAXBOOST		= "MaxBoost";
    private static final String PRN_ATTACK			= "Attack";
    private static final String PRN_RELEASE			= "Release";
    private static final String PRN_READMARKERS		= "ReadMarkers";

    private static final int	FILTER_NONE			= 0;
    private static final int	FILTER_HIGHPASS		= 1;
    private static final int	FILTER_LOWPASS		= 2;
    private static final int	FILTER_BANDPASS		= 3;

    private static final String	prText[]		= { "", "", "" };
    private static final String	prTextName[]	= { PRN_INIMGFILE, PRN_INMATFILE, PRN_OUTPUTFILE };
    private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, FILTER_BANDPASS };
    private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_FILTERTYPE };
    private static final Param	prPara[]		= { null, null, null, null, null,
                                                    null, null, null, null, null,
                                                    null, null };
    private static final String	prParaName[]	= { PRN_GAIN, PRN_MINFREQ, PRN_MAXFREQ, PRN_DURATION, PRN_TIMEOVERLAP,
                                                    PRN_TIMEJITTER, PRN_FREQOVERLAP, PRN_FREQJITTER, PRN_NOISEFLOOR, PRN_MAXBOOST,
                                                    PRN_ATTACK, PRN_RELEASE };
    private static final boolean prBool[]		= { false };
    private static final String	prBoolName[]	= { PRN_READMARKERS };

    private static final int GG_INIMGFILE		= GG_OFF_PATHFIELD	+ PR_INIMGFILE;
    private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_INMATFILE		= GG_OFF_PATHFIELD	+ PR_INMATFILE;
    private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_FILTERTYPE		= GG_OFF_CHOICE		+ PR_FILTERTYPE;
    private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_MINFREQ			= GG_OFF_PARAMFIELD	+ PR_MINFREQ;
    private static final int GG_MAXFREQ			= GG_OFF_PARAMFIELD	+ PR_MAXFREQ;
    private static final int GG_DURATION		= GG_OFF_PARAMFIELD	+ PR_DURATION;
    private static final int GG_TIMEOVERLAP		= GG_OFF_PARAMFIELD	+ PR_TIMEOVERLAP;
    private static final int GG_TIMEJITTER		= GG_OFF_PARAMFIELD	+ PR_TIMEJITTER;
    private static final int GG_FREQOVERLAP		= GG_OFF_PARAMFIELD	+ PR_FREQOVERLAP;
    private static final int GG_FREQJITTER		= GG_OFF_PARAMFIELD	+ PR_FREQJITTER;
    private static final int GG_NOISEFLOOR		= GG_OFF_PARAMFIELD	+ PR_NOISEFLOOR;
    private static final int GG_MAXBOOST		= GG_OFF_PARAMFIELD	+ PR_MAXBOOST;
    private static final int GG_ATTACK			= GG_OFF_PARAMFIELD	+ PR_ATTACK;
    private static final int GG_RELEASE			= GG_OFF_PARAMFIELD	+ PR_RELEASE;
    private static final int GG_READMARKERS		= GG_OFF_CHECKBOX	+ PR_READMARKERS;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

//	private static final String	ERR_MONO		= "Audio file must be monophonic";

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public MosaicDlg()
    {
        super( "Mosaic" );
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
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.para		= prPara;
            static_pr.para[ PR_MINFREQ ]		= new Param(    32.0, Param.ABS_HZ );
            static_pr.para[ PR_MAXFREQ ]		= new Param( 18000.0, Param.ABS_HZ );
            static_pr.para[ PR_DURATION ]		= new Param( 60000.0, Param.ABS_MS );
            static_pr.para[ PR_TIMEOVERLAP ]	= new Param(   100.0, Param.FACTOR_TIME );
            static_pr.para[ PR_TIMEJITTER ]		= new Param(    50.0, Param.FACTOR_TIME );
            static_pr.para[ PR_FREQOVERLAP ]	= new Param(   100.0, Param.FACTOR_FREQ );
            static_pr.para[ PR_FREQJITTER ]		= new Param(    50.0, Param.FACTOR_FREQ );
            static_pr.para[ PR_NOISEFLOOR ]		= new Param(   -96.0, Param.DECIBEL_AMP );
            static_pr.para[ PR_MAXBOOST ]		= new Param(    96.0, Param.DECIBEL_AMP );
            static_pr.para[ PR_ATTACK ]			= new Param(     2.0, Param.ABS_MS );
            static_pr.para[ PR_RELEASE ]		= new Param(   100.0, Param.ABS_MS );
            static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
            fillDefaultGain( static_pr.para, PR_GAIN );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        final GridBagConstraints	con;
        final PathField				ggInImgFile, ggOutputFile, ggInMatFile;
        final PathField[]			ggInputs;
        final Component[]			ggGain;
        final ParamField			ggDuration, ggTimeOverlap, ggMinFreq, ggMaxFreq;
        final ParamField			ggFreqOverlap, ggFreqJitter, ggMaxBoost, ggNoiseFloor;
        final ParamField			ggTimeJitter, ggAttack, ggRelease;
        final JComboBox				ggFilterType;
        final JCheckBox				ggReadMarkers;
        final ParamSpace[]			spcAtkRls;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

        final ItemListener il = new ItemListener() {
            public void itemStateChanged( ItemEvent e )
            {
                int	ID = gui.getItemID( e );

                switch( ID ) {
                case GG_READMARKERS:
                    pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
                    reflectPropertyChanges();
                    break;
                }
            }
        };

    // -------- Input-Gadgets --------
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;

    gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggInImgFile		= new PathField( PathField.TYPE_INPUTFILE /* + PathField.TYPE_FORMATFIELD */,
                                         "Select input image file" );
//		ggInImgFile.handleTypes( GenericFile.TYPES_IMAGE );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Image input", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInImgFile, GG_INIMGFILE, null );

        ggInMatFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select input audio file" );
        ggInMatFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Audio input", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInMatFile, GG_INMATFILE, null );

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 2 ];
        ggInputs[ 0 ]	= ggInImgFile;
        ggInputs[ 1 ]	= ggInMatFile;
        ggOutputFile.deriveFrom( ggInputs, "$D0$B0Mos$B1$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Mosaic output", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
        gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

        ggGain			= createGadgets( GGTYPE_GAIN );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

    // -------- Plot Settings --------
    gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggDuration		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Nominal Duration", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggDuration, GG_DURATION, null );

        ggMinFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Min Freq.", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggMinFreq, GG_MINFREQ, null );

        ggFilterType	= new JComboBox();
        ggFilterType.addItem( "None" );
        ggFilterType.addItem( "Highpass" );
        ggFilterType.addItem( "Lowpass" );
        ggFilterType.addItem( "Bandpass" );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Filter Type", SwingConstants.RIGHT ));
        con.weightx		= 0.2;
        gui.addChoice( ggFilterType, GG_FILTERTYPE, il );

        ggMaxFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Max Freq.", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggMaxFreq, GG_MAXFREQ, null );

        ggReadMarkers	= new JCheckBox();
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Read Markers", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addCheckbox( ggReadMarkers, GG_READMARKERS, il );

        ggTimeOverlap	= new ParamField( Constants.spaces[ Constants.factorTimeSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Time Spacing", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggTimeOverlap, GG_TIMEOVERLAP, null );

        ggFreqOverlap	= new ParamField(
            new ParamSpace( 100.0, 1000000.0, 0.01, Param.FACTOR_FREQ ));
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Freq Spacing", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggFreqOverlap, GG_FREQOVERLAP, null );

        ggTimeJitter = new ParamField( Constants.spaces[ Constants.factorTimeSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Time Jitter", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggTimeJitter, GG_TIMEJITTER, null );

        ggFreqJitter	= new ParamField( Constants.spaces[ Constants.factorFreqSpace ]);
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Freq Jitter", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggFreqJitter, GG_FREQJITTER, null );

        ggMaxBoost		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Max Boost", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggMaxBoost, GG_MAXBOOST, null );

        spcAtkRls		= new ParamSpace[] {
            Constants.spaces[ Constants.absMsSpace ],
            Constants.spaces[ Constants.factorTimeSpace ]};

        ggAttack		= new ParamField( spcAtkRls );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Attack", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggAttack, GG_ATTACK, null );

        ggNoiseFloor	= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Noise Floor", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggNoiseFloor, GG_NOISEFLOOR, null );

        ggRelease		= new ParamField( spcAtkRls );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Release", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggRelease, GG_RELEASE, null );

//		ggElitism		= new JCheckBox();
//		con.weightx		= 0.1;
//		con.gridwidth	= 1;
//		gui.addLabel( new JLabel( "Elitism", SwingConstants.RIGHT ));
//		con.weightx		= 0.2;
//		gui.addCheckbox( ggElitism, GG_ELITISM, il );

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
        long					progOff;
        final long				progLen;

        BufferedImage			img				= null;
        AudioFile				outF			= null;
        AudioFile				inMatF			= null;
        final AudioFile			tmpF;
        final AudioFileDescr	outStream;
        final AudioFileDescr	inMatDescr;
        final int				inChanNum;
        final long				inMatLength;

        final Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
        float					gain			= 1.0f;								 		// gain abs amp
        float					maxAmp			= 0.0f;

        final PathField			ggOutput;

        final Random			rnd				= new Random();
        int						chunkLen, chunkLen2;

        final ConstQ			constQ;

        final double			minFreq		= pr.para[ PR_MINFREQ ].value;
        final double			maxFreq		= pr.para[ PR_MAXFREQ ].value;
        final double			duration	= pr.para[ PR_DURATION ].value;
        final int				width, height;
        final double			framesPerPixel;
        final double			timeOverlap	= pr.para[ PR_TIMEOVERLAP ].value / 100;
        final double			freqOverlap	= pr.para[ PR_FREQOVERLAP ].value / 100;
        final int				fltType		= pr.intg[ PR_FILTERTYPE ];
        final boolean			readMarkers	= pr.bool[ PR_READMARKERS ];
        final int				inBufSize, largestWinSize;
        final float[][]			inBuf, mixBuf;
        final int				fftSize;
        final float[][]			kernels;
        final float				timeRes		= 20.0f;	// ... subject to configuration?
        final int				stepSize;
        final int				numKernels, kernelsPerPixel;
        final float[]			tmpKernel;
        final int				numRMS, numRMS10, numRMS90;
        final double[]			rmss, vars;
        final Integer[]			indices, sortedIndices;
        final float[]			hsb	= new float[ 3 ];
        final double			timeJitter	= pr.para[ PR_TIMEJITTER ].value / 100;
        final double			freqJitter	= pr.para[ PR_FREQJITTER ].value / 100;
        final double			maxBoost	= (Param.transform( pr.para[ PR_MAXBOOST ], Param.ABS_AMP, ampRef, null )).value;
        final double			noiseFloor	= (Param.transform( pr.para[ PR_NOISEFLOOR ], Param.ABS_AMP, ampRef, null )).value;
        final double			nyquist;
        final List<Marker>		markers;
        final int				largestNumSteps;
        Param					pWinSize;
        int						atkLen		= -1;
        int						rlsLen		= -1;
        float[]					atkWin		= null;
        float[]					rlsWin		= null;
        double					d1, d2, bestVar, bestRMS;
        float					f1, f2, f3, chunkGain;
        int						i1, i2, rgb, percentile, idx, bestIdx;
        int						winSize, markIdx, numSteps;
        double					midMatFreq, midImgFreq;
        long					outOff, lastMarkPos, n1, n2, framePos;
        float					brightness;
        Marker					mark;

        // karlheinz hilbert
        double					hlbFltFreq, hlbFltShift, hlbShiftFreq;
        FilterBox				hlbFltBox;
        Point					hlbFltLen;
        int						hlbSkip, hlbFFTLen, hlbInputLen, hlbChunkLen;
        int						hlbChunkLen2, hlbOverLen;
        float[]					hlbFFTBuf1, hlbFFTBuf2;
        float[][]				hlbOverBuf;
        long					hlbTotalInLen, hlbFramesRead, hlbFramesWritten, hlbFramePos;

        // karlheinz filter
        final int				fltNumPeriods	= 6;
        final double			fltFreqNorm, fltCosineNorm;
        float[][]				fltOverBuf;
        float[]					fltWin, fltFFTBuf1, fltFFTBuf2;
        double					fltLowFreq, fltHighFreq, fltLowCosFreq, fltHighCosFreq;
        int						fltFFTLen, fltHalfWinSize, fltInputLen;
        int						fltOverLen, fltLength;
        double					fltFreqBase, fltCosineBase, fltJitter;
        long					fltFramesRead, fltFramesWritten, fltTotalInLen;
        int						fltSkip, fltChunkLen, fltChunkLen2;

topLevel: try {

        // ---- open input, output; init ----
            // input
            img			= ImageIO.read( new File( pr.text[ PR_INIMGFILE ]));
            if( img == null ) throw new IOException( "No matching image decoder found" );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // ptrn input
            inMatF			= AudioFile.openAsRead( new File( pr.text[ PR_INMATFILE ]));
            inMatDescr		= inMatF.getDescr();
            inChanNum		= inMatDescr.channels;
            inMatLength		= inMatDescr.length;
            // this helps to prevent errors from empty files!
            if( (inMatLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
//			if( inPopChanNum != 1 ) throw new EOFException( ERR_MONO );
        // .... check running ....
            if( !threadRunning ) break topLevel;

//			if( inChanNum > 1 ) {
//				System.out.println( "WARNING: Multichannel input. Using mono mix for mosaic correlation!" );
//			}

            // output
            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream	= new AudioFileDescr( inMatDescr );
            ggOutput.fillStream( outStream );
            outF		= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // ---- further inits ----

            constQ				= new ConstQ();
            constQ.setSampleRate( inMatDescr.rate );
            constQ.setMinFreq( (float) minFreq );
            constQ.setMaxFreq( (float) maxFreq );
            constQ.setMaxFFTSize( 8192 );
            constQ.setMaxTimeRes( timeRes );
            constQ.createKernels();
            fftSize				= constQ.getFFTSize();
            numKernels			= constQ.getNumKernels();

            if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
                gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value;
            }
            tmpF				= createTempFile( outStream );

            width				= img.getWidth();
            height				= img.getHeight();
//			framesPerPixel		= (double) inMatDescr.length / width;
            framesPerPixel		= AudioFileDescr.millisToSamples( inMatDescr, duration ) / width;

            if( readMarkers ) {
                inMatF.readMarkers();
                markers			= (List<Marker>) inMatDescr.getProperty( AudioFileDescr.KEY_MARKERS );
                if( markers == null || markers.isEmpty() ) throw new IOException( "Soundfile does not contain markers" );
                Collections.sort( markers );
                mark			= (Marker) markers.get( 0 );
                if( mark.pos == 0 ) markers.remove( 0 );
                mark			= (Marker) markers.get( markers.size() - 1 );
                if( mark.pos < inMatLength ) markers.add( new Marker( inMatLength, "End" ));
                i1				= 0;
                lastMarkPos		= 0;
                for( int i = 0; i < markers.size(); i++ ) {
                    n2	= ((Marker) markers.get( i )).pos;
                    i1	= Math.max( i1, (int) (n2 - lastMarkPos) );
                    lastMarkPos	= n2;
                }
                largestWinSize	= i1;
            } else {
                largestWinSize	= (int) (framesPerPixel * timeOverlap + 0.5);
                markers			= null;
            }
            inBufSize			= Math.max( 8192, largestWinSize );
            stepSize			= (int) (AudioFileDescr.millisToSamples( inMatDescr, timeRes ) + 0.5);
            largestNumSteps		= Math.max( 0, largestWinSize - fftSize ) / stepSize + 1;
            inBuf				= new float[ inChanNum ][ inBufSize ];
            mixBuf				= new float[ inChanNum ][ inBufSize ];
            kernels				= new float[ largestNumSteps ][ numKernels ];
            tmpKernel			= new float[ numKernels ];
            kernelsPerPixel		= (int) ((double) numKernels / height + 0.5);
            numRMS				= Math.max( 0, numKernels - kernelsPerPixel ) + 1;
            numRMS90			= (int) (numRMS * 0.9 + 0.5);
            numRMS10			= numRMS - numRMS90;
            rmss				= new double[ numRMS ];
            vars				= new double[ numRMS ];
            sortedIndices		= new Integer[ numRMS ];
            for( int i = 0; i < numRMS; i++ ) {
                sortedIndices[ i ] = new Integer( i );
            }
            indices				= new Integer[ numRMS ];

//			pWinSize			= new Param( AudioFileDescr.samplesToMillis( outStream, winSize ), Param.ABS_MS );
//			atkLen				= Math.min( winSize, (int) (AudioFileDescr.millisToSamples( outStream, Param.transform( pr.para[ PR_ATTACK ], Param.ABS_MS, pWinSize, null ).value ) + 0.5) );
//			rlsLen				= Math.min( winSize - atkLen, (int) (AudioFileDescr.millisToSamples( outStream, Param.transform( pr.para[ PR_RELEASE ], Param.ABS_MS, pWinSize, null ).value ) + 0.5) );
//			atkWin				= Filter.createWindow( atkLen, Filter.WIN_HANNING );
//			Util.reverse( atkWin, 0, atkLen );
//			rlsWin				= Filter.createWindow( rlsLen, Filter.WIN_HANNING );

//System.out.println( "atkLen " + atkLen + "; rlsLen " + rlsLen + "; winSize " + winSize );
//Debug.view( atkWin, 0, atkLen, "Atk", true, false );
//Debug.view( rlsWin, 0, rlsLen, "Rls", true, false );

            nyquist				= (inMatDescr.rate/2);
            fltFreqNorm			= Math.PI / nyquist;
            fltCosineNorm		= 4.0 / (Math.PI*Math.PI);

            progLen				= ((width + 1) * height * inBufSize);
            progOff				= 0;
            lastMarkPos			= 0;
            markIdx				= 0;
            winSize				= largestWinSize;

        // ----==================== processing loop ====================----

            for( int x = 0; x < width; x++ ) {
lpY:			for( int y = 0; threadRunning && (y < height); y++ ) {

//System.out.println( "x = " + x + "; y = " + y );

                    rgb = img.getRGB( x, y );
                    Color.RGBtoHSB( (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb );
                    brightness = hsb[ 2 ];
                    if( brightness == 0f ) {
                        progOff += inBufSize;
                        continue lpY;
                    }

//					inMatF.seekFrame( (long) (x * framesPerPixel + 0.5) );
                    framePos = inMatF.getFramePosition();
                    if( readMarkers ) {
                        if( markIdx == markers.size() ) {
                            markIdx = 0;
                            lastMarkPos = 0L;
                            inMatF.seekFrame( 0L );
                        }
                        n1 = ((Marker) markers.get( markIdx++ )).pos;
                        winSize = (int) (n1 - lastMarkPos);
                        lastMarkPos = n1;
                    } else if( framePos == inMatLength ) {
                        inMatF.seekFrame( 0L );
                    }
                    chunkLen = (int) Math.min( inMatLength - framePos, winSize );
                    inMatF.readFrames( inBuf, 0, chunkLen );
    //				if( chunkLen < winSize ) {
    //					Util.clear( inBuf, chunkLen, winSize - chunkLen );
    //				}

                    // XXX mult with hanning window

    //				Util.clear( rmss );
    //				Util.clear( vars );
                    for( int i = 0; i < numRMS; i++ ) {
                        rmss[ i ] = 0.0;
                        vars[ i ] = 0.0;
                    }

                    numSteps = Math.max( 0, winSize - fftSize ) / stepSize + 1;

                    for( int step = 0, stepOff = 0; step < numSteps; step++, stepOff += stepSize ) {
                        chunkLen2 = Math.max( 0, Math.min( fftSize, chunkLen - stepOff ));
                        constQ.transform( inBuf[ 0 ], stepOff, chunkLen2, kernels[ step ], 0 );
                        for( int ch = 1; ch < inChanNum; ch++ ) {
                            constQ.transform( inBuf[ ch ], stepOff, chunkLen2, tmpKernel, 0 );
                            Util.add( tmpKernel, 0, kernels[ step ], 0, numKernels );
                        }
                        d1 = 0.0; d2 = 0.0;
                        f2 = Util.mean( kernels[ step ], 0, numKernels );
                        for( int i = 0; i < kernelsPerPixel; i++ ) {
                            f1 = kernels[ step ][ i ];
                            f3 = f1 - f2;
                            d1 += f1 * f1;
                            d2 += f3 * f3;
                        }
                        rmss[ 0 ] += d1;
                        vars[ 0 ] += d2;
                        for( int i = 1, j = kernelsPerPixel; i < numRMS; i++, j++ ) {
                            f1 = kernels[ step ][ j - kernelsPerPixel ];
                            f3 = f1 - f2;
                            d1 -= f1 * f1;
                            d2 -= f3 * f3;
                            f1 = kernels[ step ][ j ];
                            f3 = f1 - f2;
                            d1 += f1 * f1;
                            d2 += f3 * f3;
                            rmss[ i ] += d1;
                            vars[ i ] += d2;
                        }
                    }

                    System.arraycopy( sortedIndices, 0, indices, 0, numRMS );
                    Util.sort( rmss, indices, 0, numRMS );

                    percentile = (int) (brightness * numRMS90 + numRMS10 + 0.5f);
                    bestIdx = -1;
                    bestVar = Double.POSITIVE_INFINITY;
                    bestRMS = 0.0;
                    for( int i = numRMS - percentile; i < numRMS; i++ ) {
                        idx = indices[ i ].intValue();
                        if( vars[ idx ] < bestVar ) {
                            bestVar = vars[ idx ];
                            bestRMS = rmss[ idx ];
                            bestIdx = idx;
                        }
                    }

//					midMatFreq = Util.linexp( bestIdx + kernelsPerPixelH, 0, numRMS, minFreq, maxFreq );
                    midMatFreq = Util.linexp( bestIdx, 0, numRMS - 1, minFreq, maxFreq );
                    midImgFreq = Util.linexp( y, height - 1, 0, minFreq, maxFreq );
//					hlbShiftFreq = Math.abs( midImgFreq - midMatFreq );
                    hlbShiftFreq = midImgFreq - midMatFreq;
                    hlbTotalInLen	= chunkLen;

                    // =================================================================
                    // =============== here comes the hilbert shift part ===============
                    // =================================================================

//if( x == 51 && y == 23 ) {
//	Debug.view( inBuf[ 0 ], 0, 1000, "In x = " + x + "; y = " + y, true, false );
//}

                    hlbFltFreq		= inMatDescr.rate * 0.245;					// account for lp transition band
                    hlbFltShift		= hlbFltFreq;
                    hlbFltShift	   *= Constants.PI2 / inMatDescr.rate;		// normalized freq ready for Math.cos/sin
                    hlbShiftFreq   *= Constants.PI2 / inMatDescr.rate;

                    hlbFltBox		= new FilterBox();
                    hlbFltBox.filterType= FilterBox.FLT_LOWPASS;
                    hlbFltBox.cutOff = new Param( hlbFltFreq, Param.ABS_HZ );
                    hlbFltLen		= hlbFltBox.calcLength( inMatDescr, FIRDesignerDlg.QUAL_VERYGOOD );
                    hlbSkip			= hlbFltLen.x * 2;							// support not written out
                    i1				= (hlbFltLen.x + hlbFltLen.y) * 2 - 1;		// length after conv. filter with itself
                    i2				= i1 + i1 - 1;								// length after conv. with input
                    for( hlbFFTLen = 2; hlbFFTLen < i2; hlbFFTLen <<= 1 ) ;
                    hlbInputLen		= hlbFFTLen - i1 - 1;	// + 1

                    hlbFFTBuf1		= new float[ hlbFFTLen << 1 ];
                    hlbFFTBuf2		= new float[ hlbFFTLen << 1 ];
//					hlbReBuf		= new float[ inChanNum ][ hlbFFTLen + 2 ];
                    hlbOverLen		= hlbFFTLen - hlbInputLen;
                    hlbOverBuf		= new float[ inChanNum ][ hlbOverLen ];

                    // we design a half-nyquist lp filter and shift it up by that freq. to have a real=>analytic filter
                    // see comp.dsp algorithms-faq Q2.10
                    hlbFltBox.calcIR( inMatDescr, FIRDesignerDlg.QUAL_VERYGOOD, Filter.WIN_BLACKMAN, hlbFFTBuf1, hlbFltLen );
                    Fourier.realTransform( hlbFFTBuf1, hlbFFTLen, Fourier.FORWARD );
                    // ---- "medium"-length sinc-lp convolved with itself produces a sharp and non-overshooting filter! ----
                    for( int i = 0; i <= hlbFFTLen; i += 2 ) {
                        d1			= hlbFFTBuf1[ i ];
                        d2			= hlbFFTBuf1[ i + 1 ];
                        hlbFFTBuf1[ i ]= (float) (d1*d1 - d2*d2);		// complex mult
                        hlbFFTBuf1[ i + 1 ]= (float) (2*d1*d2);
                    }
                    Fourier.realTransform( hlbFFTBuf1, hlbFFTLen, Fourier.INVERSE );
                    // ---- real=>complex + modulation with exp(ismpRate/4 +/- (?) antialias) ----
                    // k is chosen so that the filter-centre is zero degrees, otherwise we introduce phase shift
                    for( int i = hlbFFTLen - 1, k = i - hlbFltLen.x, j = hlbFFTBuf1.length - 1; i >= 0; i--, k-- ) {
                        d1					= -hlbFltShift * k;
                        hlbFFTBuf1[ j-- ]	= (float) (hlbFFTBuf1[ i ] * Math.sin( d1 ));		// img
                        hlbFFTBuf1[ j-- ]	= (float) (hlbFFTBuf1[ i ] * Math.cos( d1 ));		// real
                    }
                    Fourier.complexTransform( hlbFFTBuf1, hlbFFTLen, Fourier.FORWARD );

                    hlbFramesRead		= 0;
                    hlbFramesWritten	= 0;

                    while( threadRunning && (hlbFramesWritten < hlbTotalInLen) ) {
                        hlbChunkLen  = (int) Math.min( hlbInputLen, hlbTotalInLen - hlbFramesRead );
                        hlbChunkLen2 = (int) Math.min( hlbInputLen, hlbTotalInLen - hlbFramesWritten );

//						// fake read in
//						for( int ch = 0; ch < inChanNum; ch++ ) {
//							convBuf1 = hlbReBuf[ ch ];
//							System.arraycopy( inBuf[ ch ], (int) hlbFramesRead, convBuf1, 0, hlbChunkLen );
//							for( int i = hlbChunkLen; i < convBuf1.length; i++ ) {
//								convBuf1[ i ] = 0f;
//							}
//						}

                        for( int ch = 0; ch < inChanNum; ch++ ) {
                            // fake read
//if( x == 8 && y == 12 ) {
//	System.out.println( "ch = " + ch + "; copying from inBuf (" + hlbFramesRead + ") to fftBuf (0), len = " + hlbChunkLen );
//}
                            if( hlbChunkLen > 0 ) {
                                System.arraycopy( inBuf[ ch ], (int) hlbFramesRead, hlbFFTBuf2, 0, hlbChunkLen );

                                for( int i = hlbChunkLen; i < hlbFFTLen; i++ ) {
                                    hlbFFTBuf2[ i ] = 0.0f;
                                }

                            // ---- real fft input + convert to complex + convolve with filter + ifft ----
                                Fourier.realTransform( hlbFFTBuf2, hlbFFTLen, Fourier.FORWARD );
                                for( int i = hlbFFTLen, j = hlbFFTLen; i > 0; i -= 2, j += 2 ) {	// neg.freq. complex conjugate
                                    hlbFFTBuf2[ j ]		= hlbFFTBuf2[ i ];
                                    hlbFFTBuf2[ j+1 ]	= -hlbFFTBuf2[ i+1 ];
                                }
                                Fourier.complexMult( hlbFFTBuf1, 0, hlbFFTBuf2, 0, hlbFFTBuf2, 0, hlbFFTLen << 1 );
                                Fourier.complexTransform( hlbFFTBuf2, hlbFFTLen, Fourier.INVERSE );

                            // ---- post proc ----
                                hlbFramePos = hlbFramesRead; // hlbFramesWritten;
                                for( int i = 0, j = 0; i < hlbFFTLen; i++ ) {
                                    d1				= hlbShiftFreq * hlbFramePos++;
                                    hlbFFTBuf2[ i ]	= (float) (hlbFFTBuf2[ j++ ] * Math.cos( d1 ) + hlbFFTBuf2[ j++ ] * Math.sin( d1 ));
                                }

                            } else {
                                Util.clear( hlbFFTBuf2 );
                            }

                        // ---- handle overlaps ----
                            Util.add( hlbOverBuf[ ch ], 0, hlbFFTBuf2, 0, hlbOverLen );
////						System.arraycopy( hlbReBuf[ ch ], hlbInputLen, hlbOverBuf[ ch ], 0, hlbFFTLen - hlbInputLen );
//							System.arraycopy( hlbFFTBuf2, hlbChunkLen, hlbOverBuf[ ch ], 0, hlbOverLen );
                            System.arraycopy( hlbFFTBuf2, hlbChunkLen2, hlbOverBuf[ ch ], 0, hlbOverLen );

                            // fake write
                            System.arraycopy( hlbFFTBuf2, hlbSkip, inBuf[ ch ], (int) hlbFramesWritten, Math.max( 0, hlbChunkLen2 - hlbSkip ));

                        } // for channels

//						// fake write out
//						for( int ch = 0; ch < inChanNum; ch++ ) {
//							convBuf1 = hlbReBuf[ ch ];
//							System.arraycopy( convBuf1, hlbSkip, inBuf[ ch ], (int) hlbFramesWritten, hlbChunkLen2 - hlbSkip );
//						}

                        hlbFramesRead    += hlbChunkLen;
                        hlbFramesWritten += Math.max( 0, hlbChunkLen2 - hlbSkip );

                        if( hlbSkip > 0 ) {
                            hlbSkip = Math.max( 0, hlbSkip - hlbChunkLen2 );
                        }

                    } // until framesWritten == outLength

//if( x == 51 && y == 23 ) {
//	Debug.view( inBuf[ 0 ], 0, 1000, "Hlb x = " + x + "; y = " + y, true, false );
//}

                    fltTotalInLen	= chunkLen;
                    // ============================================================
                    // =============== here comes the bandpass part ===============
                    // ============================================================

                    if( fltType != FILTER_NONE ) {
                        // LP = +1.0 fc  -1.0 Zero
                        // HP = +1.0 pi/2 -1.0 fc
                        // BP = +1.0 fc2 -1.0 fc1
                        fltJitter = (rnd.nextDouble() * 2 - 1) * freqJitter;
                        if( fltType == FILTER_LOWPASS ) {
                            fltLowFreq		= 0.0f;
                            fltLowCosFreq	= 0.0f;
                        } else {
                            fltLowFreq		= Math.max( 0.0,
                                Util.linexp( y + fltJitter - 0.5, height - 1, 0, minFreq, maxFreq ));
                            fltLowCosFreq	= fltLowFreq - Math.max( 0.0,
                                Util.linexp( y + fltJitter - 0.5 - (freqOverlap - 1.0), height - 1, 0, minFreq, maxFreq ));
                        }

                        if( fltType == FILTER_HIGHPASS ) {
                            fltHighFreq		= nyquist;
                            fltHighCosFreq	= 0.0;
                        } else {
                            fltHighFreq		= Math.min( nyquist,
                                Util.linexp( y + fltJitter + 0.5, height - 1, 0, minFreq, maxFreq ));
                            fltHighCosFreq	= Math.min( nyquist,
                                Util.linexp( y + fltJitter + 0.5 + (freqOverlap - 1.0), height - 1, 0, minFreq, maxFreq ))
                                - fltHighFreq;
                        }

//						fltCosineFreqs		= new float[ numBands+1 ];
//						fltCosineFreqs[0]	= 0.0f;
//						fltCosineFreqs[numBands] = 0.0f;
//						for( int i = 1; i < numBands; i++ ) {
//							d1			= Math.sqrt( fltCrossFreqs[i-1] * fltCrossFreqs[i] ) - fltCrossFreqs[i-1];	// middle freq - lower freq
//							d2			= Math.sqrt( fltCrossFreqs[i] * fltCrossFreqs[i+1] ) - fltCrossFreqs[i];
//							fltCosineFreqs[i] = Math.max( 0.0f, (float) (Math.min( d1, d2 ) * fltRollOff ));
//						}

//						fltLowCosFreq	= (float) (Math.sqrt( fltLowFreq * fltHighFreq ) - fltLowFreq);
//						fltLowCosFreq	= 0.0f; // XXX
//						fltHighCosFreq	= 0.0f; // XXX

    //					fltNumPeriods	= 6;
                        fltHalfWinSize	= Math.max( 1, (int) ((double) fltNumPeriods * inMatDescr.rate / fltHighFreq + 0.5) );
                        fltLength		= fltHalfWinSize + fltHalfWinSize;
                        fltWin			= Filter.createFullWindow( fltLength, Filter.WIN_BLACKMAN );

                        i1				= fltLength + fltLength - 1;
                        for( fltFFTLen = 2; fltFFTLen < i1; fltFFTLen <<= 1 ) ;
                        fltInputLen		= fltFFTLen - fltLength + 1;
                        fltOverLen		= fltFFTLen - fltInputLen;

//if( x == 51 && y == 23 ) {
//	System.out.println( "Filter: loFreq " + fltLowFreq + "; hiFreq " + fltHighFreq + "; loCosFreq " + fltLowCosFreq + "; hiCosFreq " + fltHighCosFreq );
//	System.out.println( "        fltHalfWinSize " + fltHalfWinSize + "; fltFFTLen " + fltFFTLen + "; fltInputLen " + fltInputLen + "; fltOverLen " + fltOverLen + "; fltTotalInLen " + fltTotalInLen );
//}

//						fltFFTBuf		= new float[ numBands ][ fltFFTLen + 2 ];
                        fltFFTBuf1		= new float[ fltFFTLen + 2 ];
                        fltFFTBuf2		= new float[ fltFFTLen + 2 ];
    //					fltInBuf		= new float[ inChanNum ][ fltInputLen ];
                        fltOverBuf		= new float[ inChanNum ][ fltOverLen ];

                        // calculate impulse response of the bandpass
                        fltFreqBase		= fltFreqNorm * fltHighFreq;
                        fltCosineBase	= fltFreqNorm * fltHighCosFreq;

                        for( int j = 1; j < fltHalfWinSize; j++ ) {
                            // sinc-filter
                            d1							= (Math.sin( fltFreqBase * j ) / (double) j);
                            // raised cosine modulation
                            d2							= fltCosineNorm * fltCosineBase * j * fltCosineBase * j;
                            d1						   *= (Math.cos( fltCosineBase * j ) / (1.0 - d2));
                            fltFFTBuf1[ fltHalfWinSize + j ]	= (float) d1;
                            fltFFTBuf1[ fltHalfWinSize - j ]	= (float) d1;
                        }
                        fltFFTBuf1[ fltHalfWinSize ] = (float) fltFreqBase;

                        fltFreqBase		= fltFreqNorm * fltLowFreq;
                        fltCosineBase	= fltFreqNorm * fltLowCosFreq;

                        for( int j = 1; j < fltHalfWinSize; j++ ) {
                            d1							 = (Math.sin( fltFreqBase * j ) / (double) j);
                            // raised cosine modulation
                            d2							 = fltCosineNorm * fltCosineBase * j * fltCosineBase * j;
                            d1						    *= (Math.cos( fltCosineBase * j ) / (1.0 - d2));
                            fltFFTBuf1[ fltHalfWinSize + j ] -= (float) d1;
                            fltFFTBuf1[ fltHalfWinSize - j ] -= (float) d1;
                        }
                        fltFFTBuf1[ fltHalfWinSize ] -= (float) fltFreqBase;

                        // zero padding
                        for( int j = fltLength; j < fltFFTLen; j++ ) {
                            fltFFTBuf1[ j ] = 0.0f;
                        }
                        // windowing
                        Util.mult( fltWin, 0, fltFFTBuf1, 0, fltLength );

//if( x == 51 && y == 23 ) {
//	Debug.view( fltFFTBuf1, 0, fltLength, "Filter Time Signal", true, false );
//}

                        Fourier.realTransform( fltFFTBuf1, fltFFTLen, Fourier.FORWARD );

                        fltFramesRead		= 0;
                        fltFramesWritten	= 0;
                        fltSkip				= fltHalfWinSize;

                        while( threadRunning && (fltFramesWritten < fltTotalInLen) ) {

                            fltChunkLen = (int) Math.min( fltInputLen, fltTotalInLen - fltFramesRead );
                            fltChunkLen2 = (int) Math.min( fltInputLen, fltTotalInLen - fltFramesWritten );

//if( x == 51 && y == 23 ) {
//	System.out.println( "fltChunkLen = " + fltChunkLen + " ; fltChunkLen2 = " + fltChunkLen2 + "; fltFramesRead = " + fltFramesRead + "; fltFramesWritten " + fltFramesWritten );
//}

                        // ---- channels loop -----------------------------------------------------------------------
                            for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
                                // fake read
//if( x == 51 && y == 23 ) {
//	System.out.println( "ch = " + ch + "; copying from inBuf (" + fltFramesRead + ") to fftBuf (0), len = " + fltChunkLen );
//}
                                if( fltChunkLen > 0 ) {
                                    System.arraycopy( inBuf[ ch ], (int) fltFramesRead, fltFFTBuf2, 0, fltChunkLen );

                                    for( int i = fltChunkLen; i < fltFFTLen; i++ ) {
                                        fltFFTBuf2[ i ] = 0.0f;
                                    }
                                    Fourier.realTransform( fltFFTBuf2, fltFFTLen, Fourier.FORWARD );
                                    Fourier.complexMult( fltFFTBuf2, 0, fltFFTBuf1, 0, fltFFTBuf2, 0, fltFFTBuf1.length );
                                    Fourier.realTransform( fltFFTBuf2, fltFFTLen, Fourier.INVERSE );
    //if( x == 51 && y == 23 ) {
    //	System.out.println( "adding from fltOverBuf (0) to fftBuf (0), len = " + fltOverLen );
    ////	System.out.println( "copying from fftBuf (" + fltChunkLen + ") to fltOverBuf( 0 ), len = " + fltOverLen );
    //	System.out.println( "copying from fftBuf (" + fltChunkLen2 + ") to fltOverBuf( 0 ), len = " + fltOverLen );
    //	System.out.println( "copying from fftBuf (" + fltSkip + ") to inBuf( " + fltFramesWritten + " ), len = " + Math.max( 0, fltChunkLen2 - fltSkip ) );
    //}
                                } else {
                                    Util.clear( fltFFTBuf2 );
                                }

                                Util.add( fltOverBuf[ ch ], 0, fltFFTBuf2, 0, fltOverLen );
//								System.arraycopy( fltFFTBuf2, fltChunkLen, fltOverBuf[ ch ], 0, fltOverLen );
                                System.arraycopy( fltFFTBuf2, fltChunkLen2, fltOverBuf[ ch ], 0, fltOverLen );

                                // fake write
                                System.arraycopy( fltFFTBuf2, fltSkip, inBuf[ ch ], (int) fltFramesWritten, Math.max( 0, fltChunkLen2 - fltSkip ));
                            }

                            fltFramesRead    += fltChunkLen;
                            fltFramesWritten += Math.max( 0, fltChunkLen2 - fltSkip );
                            if( fltSkip > 0 ) {
                                fltSkip = Math.max( 0, fltSkip - fltChunkLen2 );
                            }
                        } // until framesWritten == outLength
                    // .... check running ....
                        if( !threadRunning ) break topLevel;

                    } // if( pr.intg[ PR_FILTERTYPE ] != FILTER_NONE )

                    // =================================================
                    // =============== writing out chunk ===============
                    // =================================================

//if( x == 51 && y == 23 ) {
//	Debug.view( inBuf[ 0 ], 0, 1000, "Flt x = " + x + "; y = " + y, true, false );
//}

                    pWinSize			= new Param( AudioFileDescr.samplesToMillis( outStream, winSize ), Param.ABS_MS );
                    i1					= Math.min( winSize, (int) (AudioFileDescr.millisToSamples( outStream, Param.transform( pr.para[ PR_ATTACK ], Param.ABS_MS, pWinSize, null ).value) + 0.5) );
                    if( i1 != atkLen ) {
                        atkLen			= i1;
                        atkWin			= Filter.createWindow( atkLen, Filter.WIN_HANNING );
                        Util.reverse( atkWin, 0, atkLen );
                    }
                    i1					= Math.min( winSize - atkLen, (int) (AudioFileDescr.millisToSamples( outStream, Param.transform( pr.para[ PR_RELEASE ], Param.ABS_MS, pWinSize, null ).value) + 0.5) );
                    if( i1 != rlsLen ) {
                        rlsLen			= i1;
                        rlsWin			= Filter.createWindow( rlsLen, Filter.WIN_HANNING );
                    }

                    // apply envelope
                    Util.mult( atkWin, 0, inBuf, 0, atkLen );
                    Util.mult( rlsWin, Math.max( 0, rlsLen - chunkLen ),
                               inBuf,  Math.max( 0, chunkLen - rlsLen ),
                               Math.min( rlsLen, chunkLen ));

//if( x == 51 && y == 23 ) {
//	Debug.view( inBuf[ 0 ], 0, 1000, "Out x = " + x + "; y = " + y, true, false );
//}

                    // apply gain
                    chunkGain	= gain * (float) Math.min( maxBoost,
                        (Util.linexp( brightness, 0.0, 1.0, noiseFloor, 1.0 ) / Math.sqrt( bestRMS / (numSteps * numRMS) )));

//if( x != 8 ) chunkGain = 0.0f;

                    Util.mult( inBuf, 0, chunkLen, chunkGain );

                    outOff = Math.max( 0, (long) (x * framesPerPixel + ((rnd.nextDouble() * 2 - 1) * timeJitter * framesPerPixel) + 0.5) );

                    if( tmpF.getFrameNum() < outOff ) {
                        tmpF.setFrameNum( outOff );
                    }
                    tmpF.seekFrame( outOff );
                    chunkLen2 = (int) Math.min( chunkLen, tmpF.getFrameNum() - outOff );
                    if( chunkLen2 > 0 ) {
                        tmpF.readFrames( mixBuf, 0, chunkLen2 );
                        Util.add( mixBuf, 0, inBuf, 0, chunkLen2 );
                        tmpF.seekFrame( outOff );
                    }
                    tmpF.writeFrames( inBuf, 0, chunkLen );

                    progOff			+= inBufSize;
                    // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                } // for y
            // .... check running ....
                if( !threadRunning ) break topLevel;
            } // for x

            inMatF.close();
            inMatF		= null;

            // adjust gain
            tmpF.seekFrame( 0 );
            for( long framesRead = 0; threadRunning && (framesRead < tmpF.getFrameNum()); ) {
                chunkLen = (int) Math.min( inBufSize, tmpF.getFrameNum() - framesRead );
                tmpF.readFrames( inBuf, 0, chunkLen );
                maxAmp = Math.max( maxAmp, Util.maxAbs( inBuf, 0, chunkLen ));
                framesRead += chunkLen;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).value;
            } else {
                gain	= 1.0f;
            }
            normalizeAudioFile( tmpF, outF, inBuf, gain, 1.0f );
            deleteTempFile( tmpF );

        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            outF.close();
            outF		= null;

            // inform about clipping/ low level
            maxAmp		*= gain;
            handleClipping( maxAmp );

        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( outF != null ) outF.cleanUp();
        if( inMatF != null ) inMatF.cleanUp();
    } // process()

// -------- private methods --------

    protected void reflectPropertyChanges()
    {
        super.reflectPropertyChanges();

        Component c;

        c = gui.getItemObj( GG_TIMEOVERLAP );
        if( c != null ) {
            c.setEnabled( !pr.bool[ PR_READMARKERS ]);
        }
    }
}