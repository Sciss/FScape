/*
 *  VoocooderDlg.java
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

package de.sciss.fscape.gui;

import de.sciss.fscape.io.FloatFile;
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
import de.sciss.io.IOUtil;

import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 *	Processing module for multiband combination of two sounds.
 */
public class VoocooderDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE		= 0;		// pr.text
    private static final int PR_MODFILE			= 1;
    private static final int PR_OUTPUTFILE		= 2;
    private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_OUTPUTRES		= 1;
    private static final int PR_GAINTYPE		= 2;
    private static final int PR_FILTERLEN		= 3;
    private static final int PR_KOMBINATION		= 4;
    private static final int PR_GAIN			= 0;		// pr.para
    private static final int PR_LOFREQ			= 1;
    private static final int PR_HIFREQ			= 2;
    private static final int PR_DRYMIX			= 3;
    private static final int PR_WETMIX			= 4;
    private static final int PR_ROLLOFF			= 5;
    private static final int PR_BANDSPEROCT		= 6;

    private static final int FLT_SHORT				= 0;
//	private static final int FLT_MEDIUM				= 1;
//	private static final int FLT_LONG				= 2;
//	private static final int FLT_VERYLONG			= 3;

    private static final int KMB_MULTIPLY			= 0;
    private static final int KMB_MODULO				= 1;
    private static final int KMB_MIN				= 2;
    private static final int KMB_MAX				= 3;
    private static final int KMB_VOCODER			= 4;

    private static final String[] KMB_NAMES			= { "Multiply", "Modulo", "Min", "Max", "Vocoder" };

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_MODFILE			= "ModFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_FILTERLEN		= "FilterLen";
    private static final String PRN_KOMBINATION		= "Kombi";
    private static final String PRN_LOFREQ			= "LoFreq";
    private static final String PRN_HIFREQ			= "HiFreq";
    private static final String PRN_DRYMIX			= "DryMix";
    private static final String PRN_WETMIX			= "WetMix";
    private static final String PRN_ROLLOFF			= "RollOff";
    private static final String PRN_BANDSPEROCT		= "BandsPerOct";

    private static final String	prText[]		= { "", "", "" };
    private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_MODFILE, PRN_OUTPUTFILE };
    private static final int		prIntg[]	= { 0, 0, 0, FLT_SHORT, KMB_MULTIPLY };
    private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
                                                    PRN_FILTERLEN, PRN_KOMBINATION };
    private static final Param	prPara[]		= new Param[ 7 ];
    private static final String	prParaName[]	= { PRN_GAIN, PRN_LOFREQ, PRN_HIFREQ,
                                                    PRN_DRYMIX, PRN_WETMIX, PRN_ROLLOFF, PRN_BANDSPEROCT };

    private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_MODFILE			= GG_OFF_PATHFIELD	+ PR_MODFILE;
    private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_FILTERLEN		= GG_OFF_CHOICE		+ PR_FILTERLEN;
    private static final int GG_KOMBINATION		= GG_OFF_CHOICE		+ PR_KOMBINATION;
    private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_LOFREQ			= GG_OFF_PARAMFIELD	+ PR_LOFREQ;
    private static final int GG_HIFREQ			= GG_OFF_PARAMFIELD	+ PR_HIFREQ;
//	private static final int GG_DRYMIX			= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
//	private static final int GG_WETMIX			= GG_OFF_PARAMFIELD	+ PR_WETMIX;
//	private static final int GG_ROLLOFF			= GG_OFF_PARAMFIELD	+ PR_ROLLOFF;
    private static final int GG_BANDSPEROCT		= GG_OFF_PARAMFIELD	+ PR_BANDSPEROCT;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    private static final String	ERR_CHAN			= "Input 2 must not have\nmore channels than input 1";

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public VoocooderDlg()
    {
        super( "Voocooder" );
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
            static_pr.para		= prPara;
            static_pr.para[ PR_LOFREQ ]			= new Param(  400.0, Param.ABS_HZ );
            static_pr.para[ PR_HIFREQ ]			= new Param( 9000.0, Param.ABS_HZ );
            static_pr.para[ PR_DRYMIX ]			= new Param(  100.0, Param.FACTOR_AMP );
            static_pr.para[ PR_WETMIX ]			= new Param(   25.0, Param.FACTOR_AMP );
            static_pr.para[ PR_ROLLOFF ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
            static_pr.para[ PR_BANDSPEROCT ]	= new Param(   12.0, Param.NONE );
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

        PathField			ggInputFile, ggOutputFile, ggModFile;
        JComboBox			ggFltLen, ggKombination;
        ParamField			ggLoFreq, ggHiFreq, ggBandsPerOct;
        ParamSpace[]		spcHiCut;
        Component[]			ggGain;
        PathField[]			ggInputs;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

    // -------- I/O-Gadgets --------
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;
    gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select first input file" );
        ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Input 1", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInputFile, GG_INPUTFILE, null );

        ggModFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select second input file" );
        ggModFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Input 2", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggModFile, GG_MODFILE, null );

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 2 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggInputs[ 1 ]	= ggModFile;
        ggOutputFile.deriveFrom( ggInputs, "$D0$B0Vcd$B1$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output", SwingConstants.RIGHT ));
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
        gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

    // -------- Settings-Gadgets --------
    gui.addLabel( new GroupLabel( "Voodoo Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggLoFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Low Freq", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggLoFreq, GG_LOFREQ, null );

        spcHiCut		= new ParamSpace[ 3 ];
        spcHiCut[0]		= Constants.spaces[ Constants.absHzSpace ];
        spcHiCut[1]		= Constants.spaces[ Constants.offsetHzSpace ];
        spcHiCut[2]		= Constants.spaces[ Constants.offsetSemitonesSpace ];

        ggHiFreq		= new ParamField( spcHiCut );
        ggHiFreq.setReference( ggLoFreq );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "High Freq", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggHiFreq, GG_HIFREQ, null );

        ggBandsPerOct	= new ParamField( new ParamSpace( 1.0, 256.0, 1.0, Param.NONE ));
        con.weightx		= 0.1;
        con.gridwidth	= 1;
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

        ggKombination	= new JComboBox();
        for( int i = 0; i < KMB_NAMES.length; i++ ) {
            ggKombination.addItem( KMB_NAMES[i] );
        }
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Combination", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addChoice( ggKombination, GG_KOMBINATION, null );

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


    /*
     *	How it works: input is low and highpass filtered. low/high are processed separately as
     *	"dry parts", the difference from the original signal is fed to the shaper stage: it is
     *	split up into a user defined number of bands per octave (cosine-modulated sinc FIR filters
     *	with variable taps) -> peak amp. for each band is tracked; gain is normalized, chebychev
     *	polynomial distortion is applied and original gain restored.
     */

// XXX TO-DO : dry/wet mix

    protected void process()
    {
        int					i, j, k, len, off, ch, chunkLength, chunkLength2, band;
        long				progOff, progLen;
        double				d1, d2, loFreq, hiFreq, freqFactor;
        float				f1, f2, f3, f4;

        // io
        AudioFile			inF				= null;
        AudioFile			modF			= null;
        AudioFile			outF			= null;
        AudioFileDescr			inStream;
        AudioFileDescr			modStream;
        AudioFileDescr			outStream;
        FloatFile[]			floatF			= null;
        File[]				tempFile		= null;

        // buffers
        float[]				lowpass, highpass, hpFFTBuf, lpFFTBuf, dwFFTBuf;
        float[][]			shpFFTBuf, inLPOverBuf, modLPOverBuf, inHPOverBuf, modHPOverBuf, inConvFFTBuf, modConvFFTBuf;
        float[][][]			inConvOverBuf, modConvOverBuf, inSliceFFTBuf, modSliceFFTBuf;
        float[]				convBuf1, convBuf2, convBuf3;
        float[]				dwWin, shpLowWin, shpHighWin;
        float[][]			inBuf, modBuf, outBuf;

        int					inChanNum, inLength, modChanNum, outChanNum, inputLen, outSkip;
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

        int					numBands;

        float				dryGain, nyquist;

        // kombi
        float[][]			kmbEnv		= null;		// first order IIR save for envelope following
        float[]				kmbEnvDec	= null;		// decay of IIR

topLevel: try {

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

            modF			= AudioFile.openAsRead( new File( pr.text[ PR_MODFILE ]));
            modStream		= modF.getDescr();
            modChanNum		= modStream.channels;
            inLength		= (int) Math.min( inLength, modStream.length );
            outChanNum		= Math.max( modChanNum, inChanNum );
            // this helps to prevent errors from empty files!
            if( inLength < 1 ) throw new EOFException( ERR_EMPTY );
            if( modChanNum > inChanNum ) throw new IOException( ERR_CHAN );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // output
            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream	= new AudioFileDescr( inStream );
            ggOutput.fillStream( outStream );
            outStream.channels = outChanNum;
            outF		= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- calculate filters ----

            loFreq				= Param.transform( pr.para[ PR_LOFREQ ], Param.ABS_HZ, null, null ).value;
//			hiFreq				= Math.min( inStream.smpRate * 0.5,
//								  Param.transform( pr.para[ PR_HIFREQ ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).value );
            hiFreq				= Param.transform( pr.para[ PR_HIFREQ ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).value;
            freqFactor			= Math.pow( 2.0, 1.0 / pr.para[ PR_BANDSPEROCT ].value);

            numBands			= (int) (Math.log( hiFreq / loFreq ) / Math.log( freqFactor ));
            crossFreqs			= new float[ numBands+1 ];
            cosineFreqs			= new float[ numBands+1 ];
            shpHalfWinSize		= new int[ numBands ];

            d1					= loFreq;
            d2			    	= d1 * freqFactor;
            nyquist				= (float) (inStream.rate * 0.5);

        // ---- calc bandpass freqs ----

            for( i = 0; i <= numBands; i++ ) {
                crossFreqs[i]	= (float) d1;
                cosineFreqs[i]	= (float) (Math.sqrt( d2 * d1 ) - d1);
                d1			   	= d2;
                d2			    = d1 * freqFactor;
                if( d2 > nyquist ) {
                    numBands	= i;
                    break;
                }
            }
            hiFreq				= crossFreqs[numBands];

            dwNumPeriods	= 6; // << pr.intg[ PR_FILTERLEN ];
            dwHalfWinSize	= Math.max( 1, (int) (dwNumPeriods * inStream.rate / loFreq + 0.5) );
            freqNorm		= Constants.PI2 / inStream.rate;
            cosineNorm		= 4.0 / (Math.PI*Math.PI);
            dwFltLength		= dwHalfWinSize + dwHalfWinSize;

            j				= dwFltLength + dwFltLength - 1;
            for( dwFFTLength = 2; dwFFTLength < j; dwFFTLength <<= 1 ) ;
            inputLen		= dwFFTLength - dwFltLength + 1;
            dwOverLen		= dwFFTLength - inputLen;

        // ---- calc highpass filter ----
            // use a narrow window here to decrease time smearing!! (large window calculated in lowpass section)
            k				= Math.max( 1, (int) (dwNumPeriods * inStream.rate / hiFreq + 0.5) );
            dwWin			= Filter.createFullWindow( k << 1, Filter.WIN_BLACKMAN );
            freqBase		= freqNorm * (inStream.rate/2);
            highpass			= new float[ dwFFTLength + 2 ];
            Util.clear( highpass );
            highpass[ dwHalfWinSize ] = (float) freqBase;

            freqBase		= freqNorm * hiFreq * 0.8; // / rollOff;
            cosineBase		= freqNorm * hiFreq * 0.04; // (1.0 - 1.0 / rollOff);
            cosineBaseSqr	= cosineBase * cosineBase;

            for( j = 1; j < k; j++ ) {
                d1							= (Math.sin( freqBase * j ) / j);
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
                d1							 = (Math.sin( freqBase * j ) / j);
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
            shpMaxHalfWin	= Math.max( 1, (int) (shpNumPeriods * inStream.rate / crossFreqs[0] + 0.5) );
            shpMinHalfWin	= Math.max( 1, (int) (shpNumPeriods * inStream.rate / crossFreqs[numBands-1] + 0.5) );
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
            inConvFFTBuf	= new float[ inChanNum ][ shpMaxFFTLen + 2 ];
            modConvFFTBuf	= new float[ modChanNum ][ shpMaxFFTLen + 2 ];
            inConvOverBuf	= new float[ inChanNum ][ numBands ][];
            modConvOverBuf	= new float[ modChanNum ][ numBands ][];

            shpFFTLen		= -1;	// shpMaxFFTLen;
            shpFFTLenIdx	= -1;
            shpFFTLength[0]	= shpFFTLen;
            shpFFTCount[0]	= 0;
            i				= shpMaxFFTLen - (shpMaxHalfWin << 1) + 1;
            shpConvNum[0]	= (inputLen + i - 1) / i;
            shpHighHalfWin	= shpMaxHalfWin;
            shpHighWin		= Filter.createFullWindow( shpHighHalfWin << 1, Filter.WIN_BLACKMAN );

            for( band = 0; band < numBands; band++ ) {

                shpLowHalfWin	= shpHighHalfWin;
                shpLowWin		= shpHighWin;
                shpHalfWinSize[band]=shpLowHalfWin;
                shpHighHalfWin	= Math.max( 1, (int) (shpNumPeriods * inStream.rate / crossFreqs[band+1] + 0.5) );
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
                    inConvOverBuf[ ch ][ band ] = new float[ convOverLen ];
                }
                for( ch = 0; ch < modChanNum; ch++ ) {
                    modConvOverBuf[ ch ][ band ] = new float[ convOverLen ];
                }

                // first part of filter
                if( band < numBands-1 ) {	// damn overhead for highest band (we don't want an extra slope, we have the hp-slope already)
                    freqBase		= freqNorm * crossFreqs[ band+1 ];
                    cosineBase		= freqNorm * cosineFreqs[ band+1 ];

                    cosineBaseSqr	= cosineBase * cosineBase;

                    for( j = 1; j < shpHighHalfWin; j++ ) {
                        // sinc-filter
                        d1								= (Math.sin( freqBase * j ) / j);
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
                        d1							 = (Math.sin( freqBase * j ) / j);
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

            } // for bands

            numShpLen		= shpFFTLenIdx + 1;	// muss korrigiert werden, weil u.U. eine FFT-Laenge "uebersprungen" wurde!!

        // ---- further inits ----

            outBuf			= new float[ outChanNum ][ shpMaxHalfWin + inputLen ];
            dwFFTBuf		= new float[ dwFFTLength + 2 ];
            lpFFTBuf		= new float[ dwFFTLength + 2 ];
            hpFFTBuf		= new float[ dwFFTLength + 2 ];
            inLPOverBuf		= new float[ inChanNum ][ dwOverLen ];
            modLPOverBuf	= new float[ modChanNum ][ dwOverLen ];
            inHPOverBuf		= new float[ inChanNum ][ dwOverLen ];
            modHPOverBuf	= new float[ modChanNum ][ dwOverLen ];
            inBuf			= new float[ inChanNum ][ inputLen + dwHalfWinSize ];
            modBuf			= new float[ modChanNum ][ inputLen + dwHalfWinSize ];
            inSliceFFTBuf	= new float[ inChanNum ][ shpConvNum[ numShpLen - 1 ]][ shpMaxFFTLen + 2 ];	// largets convNum x largest FFT size
            modSliceFFTBuf	= new float[ inChanNum ][ shpConvNum[ numShpLen - 1 ]][ shpMaxFFTLen + 2 ];	// largets convNum x largest FFT size
            Util.clear( inBuf );
            Util.clear( modBuf );

            switch( pr.intg[ PR_KOMBINATION ]) {
            case KMB_VOCODER:
                kmbEnv		= new float[ outChanNum ][ numBands ];
                kmbEnvDec	= new float[ numBands ];
                Util.clear( kmbEnv );
                for( band = 0; band < numBands; band++ ) {
                    f1				= (float) inStream.rate / crossFreqs[ band ]; // period [smp]
                    f1				= (float) Math.pow( 0.125, 1.0 / (f1 * 4) );	// -18 dB after 4 periods
                    kmbEnvDec[band]	= f1;
                }
                break;
            }

// System.out.println( "inputLen "+inputLen+"; dwFltLength "+dwFltLength+"; shpMaxFFTLen "+shpMaxFFTLen+"; shpMinFFTLen "+shpMinFFTLen+"; sliceFFTBuf dim "+(shpConvNum[ numShpLen - 1 ])+" x "+shpMaxFFTLen );

            progOff		= 0;
            progLen		= (long) inLength * 4;	// read, 2x process, write
//			m			= 0;	// java compiler complains if not initialized

            dryGain		= 0.0f; // XXX 1.0f;

            // normalization requires temp files
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                tempFile	= new File[ inChanNum ];
                floatF		= new FloatFile[ inChanNum ];
                for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
                    tempFile[ ch ]	= null;
                    floatF[ ch ]	= null;
                }
                for( ch = 0; ch < inChanNum; ch++ ) {
                    tempFile[ ch ]	= IOUtil.createTempFile();
                    floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
                }
                progLen	   += inLength;
            } else {													// account for gain loss RealFFT => CmplxIFFT
                gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value);
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== the real stuff ====================----

            framesRead		= 0;
            framesWritten	= 0;
            outSkip			= dwHalfWinSize + shpMaxHalfWin;

            while( threadRunning && (framesWritten < inLength) ) {

                chunkLength	 = Math.min( inputLen, inLength - framesRead );
                chunkLength2 = Math.min( inputLen, inLength - framesWritten );
            // ---- read input chunk ----
                for( off = 0; threadRunning && (off < chunkLength); ) {
                    len	= Math.min( 8192, chunkLength - off );
                    inF.readFrames( inBuf, off + dwHalfWinSize, len );		// inBuf delayed by dwHalfWinSize
                    modF.readFrames( modBuf, off + dwHalfWinSize, len );
                    framesRead	+= len;
                    progOff		+= len;
                    off			+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ---- preprocess in -----------------------------------------------------------------------
            // after this inBuf is the bandpass filtered pre-FFT stage
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
                    Util.add( inLPOverBuf[ ch ], 0, lpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( lpFFTBuf, inputLen, inLPOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    Fourier.complexMult( highpass, 0, dwFFTBuf, 0, hpFFTBuf, 0, highpass.length );	// convolve with HP
                    Fourier.realTransform( hpFFTBuf, dwFFTLength, Fourier.INVERSE );		// back to time domain (delayed by dwHalfWinSize)
                    Util.add( inHPOverBuf[ ch ], 0, hpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( hpFFTBuf, inputLen, inHPOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    // now the signal to be fed into the waveshaper is input (delayed by dwHalfWinSize)
                    // - lowpass - highpass
                    Util.sub( lpFFTBuf, 0, convBuf2, 0, inputLen );
                    Util.sub( hpFFTBuf, 0, convBuf2, 0, inputLen );

                    System.arraycopy( lpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );		// dry lp/hp to output
                    Util.add( hpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );
                    Util.mult( convBuf1, shpMaxHalfWin, inputLen, dryGain );

                } // for chanNum
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ---- preprocess mod -----------------------------------------------------------------------
            // after this modBuf is the bandpass filtered pre-FFT stage
                for( ch = 0; threadRunning && (ch < modChanNum); ch++ ) {

                // ---- dry/wet pre-filtering ----
                    convBuf2 = modBuf[ ch ];

                    System.arraycopy( convBuf2, dwHalfWinSize, dwFFTBuf, 0, chunkLength );	// modBuf copy without delay
                    for( i = chunkLength; i < dwFFTLength; i++ ) {
                        dwFFTBuf[ i ] = 0.0f;
                    }
                    Fourier.realTransform( dwFFTBuf, dwFFTLength, Fourier.FORWARD );		// input chunk -> FFT
                    Fourier.complexMult( lowpass, 0, dwFFTBuf, 0, lpFFTBuf, 0, lowpass.length );	// convolve with LP
                    Fourier.realTransform( lpFFTBuf, dwFFTLength, Fourier.INVERSE );		// back to time domain (delayed by dwHalfWinSize)
                    Util.add( modLPOverBuf[ ch ], 0, lpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( lpFFTBuf, inputLen, modLPOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    Fourier.complexMult( highpass, 0, dwFFTBuf, 0, hpFFTBuf, 0, highpass.length );	// convolve with HP
                    Fourier.realTransform( hpFFTBuf, dwFFTLength, Fourier.INVERSE );		// back to time domain (delayed by dwHalfWinSize)
                    Util.add( modHPOverBuf[ ch ], 0, hpFFTBuf, 0, dwOverLen );					// add old overlap
                    System.arraycopy( hpFFTBuf, inputLen, modHPOverBuf[ ch ], 0, dwOverLen );	// save new overlap

                    // now the signal to be fed into the waveshaper is input (delayed by dwHalfWinSize)
                    // - lowpass - highpass
                    Util.sub( lpFFTBuf, 0, convBuf2, 0, inputLen );
                    Util.sub( hpFFTBuf, 0, convBuf2, 0, inputLen );

//					System.arraycopy( lpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );		// dry lp/hp to output
//					Util.add( hpFFTBuf, 0, convBuf1, shpMaxHalfWin, inputLen );
//					Util.mult( convBuf1, shpMaxHalfWin, inputLen, dryGain );

                } // for chanNum
            // .... check running ....
                if( !threadRunning ) break topLevel;

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
                            for( ch = 0; ch < inChanNum; ch++ ) {
                                convBuf3	= inSliceFFTBuf[ ch ][ i ];
                                convBuf2 	= inBuf[ ch ];
                                System.arraycopy( convBuf2, off, convBuf3, 0, len );
                                for( j = len; j < shpFFTLen; j++ ) {
                                    convBuf3[ j ] = 0.0f;
                                }
                                Fourier.realTransform( convBuf3, shpFFTLen, Fourier.FORWARD );
                            }
                            for( ch = 0; ch < modChanNum; ch++ ) {
                                convBuf3	= modSliceFFTBuf[ ch ][ i ];
                                convBuf2 	= modBuf[ ch ];
                                System.arraycopy( convBuf2, off, convBuf3, 0, len );
                                for( j = len; j < shpFFTLen; j++ ) {
                                    convBuf3[ j ] = 0.0f;
                                }
                                Fourier.realTransform( convBuf3, shpFFTLen, Fourier.FORWARD );
                            }
                            off		   += len;
                        }
                    }
                    shpFFTCounter--;

                    convOverLen		= (shpLowHalfWin << 1) - 1;				// =< inputConv !
                    convShift		= shpMaxHalfWin - shpLowHalfWin;		// align all filters with uniform dly of shpMaxHalfWin

                    // convolve slices, inverse transform, overlap-add
                    for( i = 0, off = 0; i < convNum; i++ ) {
                        len		 = Math.min( inputConv, inputLen - off );
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            Fourier.complexMult( shpFFTBuf[ band ], 0, inSliceFFTBuf[ ch ][ i ], 0,
                                                 inConvFFTBuf[ ch ], 0, shpFFTBuf[ band ].length );
                            Fourier.realTransform( inConvFFTBuf[ ch ], shpFFTLen, Fourier.INVERSE );	// back to time domain (delayed by shpLowHalfWin)
                            Util.add( inConvOverBuf[ ch ][ band ], 0, inConvFFTBuf[ ch ], 0, convOverLen );	// add old overlap
                            System.arraycopy( inConvFFTBuf[ ch ], len, inConvOverBuf[ ch ][ band ], 0, convOverLen );	// save new overlap
                        }
                        for( ch = 0; ch < modChanNum; ch++ ) {
                            Fourier.complexMult( shpFFTBuf[ band ], 0, modSliceFFTBuf[ ch ][ i ], 0,
                                                 modConvFFTBuf[ ch ], 0, shpFFTBuf[ band ].length );
                            Fourier.realTransform( modConvFFTBuf[ ch ], shpFFTLen, Fourier.INVERSE );	// back to time domain (delayed by shpLowHalfWin)
                            Util.add( modConvOverBuf[ ch ][ band ], 0, modConvFFTBuf[ ch ], 0, convOverLen );	// add old overlap
                            System.arraycopy( modConvFFTBuf[ ch ], len, modConvOverBuf[ ch ][ band ], 0, convOverLen );	// save new overlap
                        }

                    // ======KOMBINATION===============================================================

                        switch( pr.intg[ PR_KOMBINATION ]) {
                        case KMB_MULTIPLY:
                            for( ch = 0; ch < outChanNum; ch++ ) {
                                convBuf1 	= outBuf[ ch ];
                                convBuf2 	= inConvFFTBuf[ ch % inChanNum ];
                                convBuf3 	= modConvFFTBuf[ ch % modChanNum ];

                                for( j = 0, k = off + convShift; j < len; j++, k++ ) {	// all samples in a slice
                                    convBuf1[ k ] += convBuf2[ j ] * convBuf3[ j ] * numBands;	// * wetGain  XXX
                                }
                            }
                            break;

                        case KMB_MODULO:
                            for( ch = 0; ch < outChanNum; ch++ ) {
                                convBuf1 	= outBuf[ ch ];
                                convBuf2 	= inConvFFTBuf[ ch % inChanNum ];
                                convBuf3 	= modConvFFTBuf[ ch % modChanNum ];

                                for( j = 0, k = off + convShift; j < len; j++, k++ ) {	// all samples in a slice
                                    convBuf1[ k ] += convBuf2[ j ] % convBuf3[ j ];	// * wetGain  XXX
                                }
                            }
                            break;

                        case KMB_MIN:
                            for( ch = 0; ch < outChanNum; ch++ ) {
                                convBuf1 	= outBuf[ ch ];
                                convBuf2 	= inConvFFTBuf[ ch % inChanNum ];
                                convBuf3 	= modConvFFTBuf[ ch % modChanNum ];

                                for( j = 0, k = off + convShift; j < len; j++, k++ ) {	// all samples in a slice
                                    f1		= convBuf2[ j ];
                                    f2		= convBuf3[ j ];
                                    if( f1 > 0.0f ) {
                                        if( f2 < 0.0f ) {
                                            convBuf1[ k ] += (-f2 > f1) ? f1 : f2;
                                        } else {
                                            convBuf1[ k ] += (f2 > f1) ? f1 : f2;
                                        }
                                    } else {
                                        if( f2 < 0.0f ) {
                                            convBuf1[ k ] += (f2 < f1) ? f1 : f2;
                                        } else {
                                            convBuf1[ k ] += (f2 > -f1) ? f1 : f2;
                                        }
                                    }
                                }
                            }
                            break;


                        case KMB_MAX:
                            for( ch = 0; ch < outChanNum; ch++ ) {
                                convBuf1 	= outBuf[ ch ];
                                convBuf2 	= inConvFFTBuf[ ch % inChanNum ];
                                convBuf3 	= modConvFFTBuf[ ch % modChanNum ];

                                for( j = 0, k = off + convShift; j < len; j++, k++ ) {	// all samples in a slice
                                    f1		= convBuf2[ j ];
                                    f2		= convBuf3[ j ];
                                    if( f1 > 0.0f ) {
                                        if( f2 < 0.0f ) {
                                            convBuf1[ k ] += (-f2 > f1) ? f2 : f1;
                                        } else {
                                            convBuf1[ k ] += (f2 > f1) ? f2 : f1;
                                        }
                                    } else {
                                        if( f2 < 0.0f ) {
                                            convBuf1[ k ] += (f2 < f1) ? f2 : f1;
                                        } else {
                                            convBuf1[ k ] += (f2 > -f1) ? f2 : f1;
                                        }
                                    }
                                }
                            }
                            break;

                        case KMB_VOCODER:
                            for( ch = 0; ch < outChanNum; ch++ ) {
                                convBuf1 	= outBuf[ ch ];
                                convBuf2 	= inConvFFTBuf[ ch % inChanNum ];
                                convBuf3 	= modConvFFTBuf[ ch % modChanNum ];

                                f2			= kmbEnv[ ch ][ band ];
                                f3			= kmbEnvDec[ band ];
                                f4			= (1.0f - f3) * numBands;
                                for( j = 0, k = off + convShift; j < len; j++, k++ ) {	// all samples in a slice
                                    f1 = convBuf3[ j ];
                                    f2 = Math.abs( f1 ) * f4 + f3 * f2;
                                    convBuf1[ k ] += convBuf2[ j ] * f2;
                                }
                                kmbEnv[ ch ][ band ] = f2;
                            }
                            break;
                        }
                        off		+= len;
                    } // for slices

                } // for bands
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // overlap handling input
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf2 	= inBuf[ ch ];
                    System.arraycopy( convBuf2, inputLen, convBuf2, 0, dwHalfWinSize );
                }
                for( ch = 0; ch < modChanNum; ch++ ) {
                    convBuf2 	= modBuf[ ch ];
                    System.arraycopy( convBuf2, inputLen, convBuf2, 0, dwHalfWinSize );
                }

                progOff	 += chunkLength2 + chunkLength2;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            // .... check running ....
                if( !threadRunning ) break topLevel;

                for( off = outSkip; threadRunning && (off < chunkLength2); ) {
                    len			   = Math.min( 8192, chunkLength2 - off );
                    if( floatF != null ) {
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            floatF[ ch ].writeFloats( outBuf[ ch ], off, len );
                        }
                    } else {
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            Util.mult( outBuf[ ch ], off, len, gain );
                        }
                        outF.writeFrames( outBuf, off, len );
                    }
                    progOff		  += len;
                    off			  += len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }

                for( ch = 0; ch < outChanNum; ch++ ) {
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
                normalizeAudioFile( floatF, outF, outBuf, gain, f1 );
                maxAmp	*= gain;

                for( ch = 0; ch < inChanNum; ch++ ) {
                    floatF[ ch ].cleanUp();
                    floatF[ ch ]		= null;
                    tempFile[ ch ].delete();
                    tempFile[ ch ]	= null;
                }
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
            modF.close();
            modF		= null;
            modStream	= null;

            shpFFTBuf	= null;
            inLPOverBuf	= null;
            modLPOverBuf= null;
            inHPOverBuf	= null;
            modHPOverBuf= null;
            inSliceFFTBuf= null;
            modSliceFFTBuf= null;
            inConvOverBuf= null;
            modConvOverBuf= null;
            convBuf1	= null;
            convBuf2	= null;
            convBuf3	= null;
            inBuf		= null;
            modBuf		= null;
            outBuf		= null;

            // inform about clipping/ low level
            handleClipping( maxAmp );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            modStream	= null;
            outStream	= null;
            lowpass		= null;
            highpass	= null;
            hpFFTBuf	= null;
            lpFFTBuf	= null;
            dwFFTBuf	= null;
            inConvFFTBuf= null;
            modConvFFTBuf= null;
            shpFFTBuf	= null;
            inLPOverBuf	= null;
            modLPOverBuf= null;
            inHPOverBuf	= null;
            modHPOverBuf= null;
            inSliceFFTBuf= null;
            modSliceFFTBuf= null;
            inConvOverBuf= null;
            modConvOverBuf= null;
            convBuf1	= null;
            convBuf2	= null;
            convBuf3	= null;
            inBuf		= null;
            modBuf		= null;
            outBuf		= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( inF != null ) {
            inF.cleanUp();
            inF = null;
        }
        if( modF != null ) {
            modF.cleanUp();
            modF = null;
        }
        if( outF != null ) {
            outF.cleanUp();
            outF = null;
        }
        if( floatF != null ) {
            for( ch = 0; ch < floatF.length; ch++ ) {
                if( floatF[ ch ] != null ) {
                    floatF[ ch ].cleanUp();
                    floatF[ ch ] = null;
                }
                if( tempFile[ ch ] != null ) {
                    tempFile[ ch ].delete();
                    tempFile[ ch ] = null;
                }
            }
        }
    } // process()
}