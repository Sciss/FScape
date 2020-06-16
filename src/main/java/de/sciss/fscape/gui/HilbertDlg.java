/*
 *  HilbertDlg.java
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

package de.sciss.fscape.gui;

import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Envelope;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 *  Following a paper by J.O.Smith, this
 *	processing module implements a high
 *	quality FIR hilbert quadrature filter
 *	and allows frequency shifting and
 *	high resolution amplitude envelope
 *	calculation.
 */
public class HilbertDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE		= 0;		// pr.text
    private static final int PR_REOUTPUTFILE	= 1;
    private static final int PR_IMOUTPUTFILE	= 2;
    private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_OUTPUTRES		= 1;
    private static final int PR_GAINTYPE		= 2;
    private static final int PR_MODE			= 3;
    private static final int PR_GAIN			= 0;		// pr.para
    private static final int PR_FREQ			= 1;
    private static final int PR_FREQMODDEPTH	= 2;
    private static final int PR_ANTIALIAS		= 0;		// pr.bool
    private static final int PR_FREQMOD			= 1;
    private static final int PR_FREQENV			= 0;		// pr.envl

    private static final int MODE_UNTOUCHED		= 0;
    private static final int MODE_UPSHIFT		= 1;
    private static final int MODE_DOWNSHIFT		= 2;
    private static final int MODE_ENVELOPE		= 3;

    private static final String[] modeStr = { "Untouched", "Shift up (re+im)", "Shift down (re-im)",
                                                "Envelope (re^2+im^2)" };

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_REOUTPUTFILE	= "ReOutFile";
    private static final String PRN_IMOUTPUTFILE	= "ImOutFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_MODE			= "Mode";
    private static final String PRN_FREQ			= "Freq";
    private static final String PRN_FREQMODDEPTH	= "FreqModDepth";
    private static final String PRN_ANTIALIAS		= "AntiAlias";
    private static final String PRN_FREQMOD			= "FreqMod";
    private static final String PRN_FREQENV			= "FreqEnv";

    private static final String	prText[]		= { "", "", "" };
    private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_REOUTPUTFILE, PRN_IMOUTPUTFILE };
    private static final int		prIntg[]	= { 0, 0, 0, MODE_UNTOUCHED };
    private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_MODE };
    private static final boolean	prBool[]	= { true, false };
    private static final String	prBoolName[]	= { PRN_ANTIALIAS, PRN_FREQMOD };
    private static final Param	prPara[]		= { null, null, null };
    private static final String	prParaName[]	= { PRN_GAIN, PRN_FREQ, PRN_FREQMODDEPTH };
    private static final Envelope	prEnvl[]	= { null };
    private static final String	prEnvlName[]	= { PRN_FREQENV };

    private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_REOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_REOUTPUTFILE;
    private static final int GG_IMOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_IMOUTPUTFILE;
    private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_MODE				= GG_OFF_CHOICE		+ PR_MODE;
    private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_FREQ				= GG_OFF_PARAMFIELD	+ PR_FREQ;
    private static final int GG_FREQMODDEPTH		= GG_OFF_PARAMFIELD	+ PR_FREQMODDEPTH;
    private static final int GG_ANTIALIAS			= GG_OFF_CHECKBOX	+ PR_ANTIALIAS;
    private static final int GG_FREQMOD				= GG_OFF_CHECKBOX	+ PR_FREQMOD;
    private static final int GG_FREQENV				= GG_OFF_ENVICON	+ PR_FREQENV;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public HilbertDlg()
    {
        super( "Hilbert Filter" );
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
            static_pr.para[ PR_FREQ ]			= new Param(  200.0, Param.ABS_HZ );
            static_pr.para[ PR_FREQMODDEPTH ]	= new Param(   12.0, Param.OFFSET_SEMITONES );
            static_pr.paraName	= prParaName;
            static_pr.envl		= prEnvl;
            static_pr.envl[ PR_FREQENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
            static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
            fillDefaultGain( static_pr.para, PR_GAIN );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggReOutputFile, ggImOutputFile;
        PathField[]			ggParent1, ggParent2;
        JComboBox				ggMode;
        ParamField			ggFreq, ggFreqModDepth;
        JCheckBox			ggAntiAlias, ggFreqMod;
        ParamSpace[]		spcFreqModDepth;
        EnvIcon				ggFreqEnv;
        Component[]			ggGain;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

        ItemListener il = new ItemListener() {
            public void itemStateChanged( ItemEvent e )
            {
                int	ID = gui.getItemID( e );

                switch( ID ) {
                case GG_MODE:
                    pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
                    reflectPropertyChanges();
                    break;
                case GG_FREQMOD:
                    pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
                    reflectPropertyChanges();
                    break;
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

        ggReOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output for real part" );
        ggReOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output [Real]", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggReOutputFile, GG_REOUTPUTFILE, null );
        gui.registerGadget( ggReOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggReOutputFile.getResGadget(), GG_OUTPUTRES );

        ggImOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE, "Select output for imaginary part" );
//		ggImOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output [Imaginary]", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggImOutputFile, GG_IMOUTPUTFILE, null );

        ggParent1		= new PathField[ 1 ];
        ggParent1[ 0 ]	= ggInputFile;
        ggParent2		= new PathField[ 1 ];
        ggParent2[ 0 ]	= ggReOutputFile;
        ggReOutputFile.deriveFrom( ggParent1, "$D0$F0Hlb$E" );
        ggImOutputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );

        ggGain			= createGadgets( GGTYPE_GAIN );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

    // -------- Settings-Gadgets --------
    gui.addLabel( new GroupLabel( "Post processing", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggMode			= new JComboBox();
        for( int i = 0; i < modeStr.length; i++ ) {
            ggMode.addItem( modeStr[ i ]);
        }
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Operation", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addChoice( ggMode, GG_MODE, il );

        ggAntiAlias		= new JCheckBox();
        con.gridwidth	= 2;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Antialiasing", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.4;
        gui.addCheckbox( ggAntiAlias, GG_ANTIALIAS, il );

        ggFreq			= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Shift amount", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggFreq, GG_FREQ, null );

        spcFreqModDepth		= new ParamSpace[ 3 ];
        spcFreqModDepth[0]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
        spcFreqModDepth[1]	= Constants.spaces[ Constants.offsetFreqSpace ];
        spcFreqModDepth[2]	= Constants.spaces[ Constants.offsetHzSpace ];

        ggFreqModDepth	= new ParamField( spcFreqModDepth );
        ggFreqModDepth.setReference( ggFreq );
        ggFreqMod		= new JCheckBox();
        con.weightx		= 0.1;
        gui.addCheckbox( ggFreqMod, GG_FREQMOD, il );
        con.weightx		= 0.4;
        gui.addParamField( ggFreqModDepth, GG_FREQMODDEPTH, null );

        ggFreqEnv		= new EnvIcon( getComponent() );
        con.weightx		= 0.1;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addGadget( ggFreqEnv, GG_FREQENV );

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
        int					i, j, k, len, off, ch, chunkLength;
        long				progOff, progLen;
        double				d1, d2;
        float				f1;

        // io
        AudioFile			inF				= null;
        AudioFile			reOutF			= null;
        AudioFile			imOutF			= null;
        AudioFileDescr			inStream;
        AudioFileDescr			reOutStream;
        AudioFileDescr			imOutStream		= null;
        FloatFile[]			reFloatF		= null;
        FloatFile[]			imFloatF		= null;
        File				reTempFile[]	= null;
        File				imTempFile[]	= null;

        // buffers
        float[]				fftBuf1;					// holds filter
        float[]				fftBuf2;					// holds complex input
        float[][]			reBuf;						// holds real input/output
        float[][]			imBuf			= null;		// holds imag input/output
        float[][]			reOverBuf;					// convolution overlap add buffer
        float[][]			imOverBuf		= null;
        float[]				convBuf1, convBuf2;

        int					inChanNum, inLength, fftLength, inputLen;
        int					framesRead, framesWritten;

        Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
        Param				peakGain;
        float				gain			= 1.0f;								 		// gain abs amp
        float				maxAmp			= 0.0f;

        boolean				modulate;
        double				fltFreq, fltShift, shiftFreq;
        FilterBox			fltBox;
        Point				fltLength;		// for FilterBox (.x = support, .y = rightWing)
        int					skip;

        PathField			ggOutput;

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

            // output
            ggOutput	= (PathField) gui.getItemObj( GG_REOUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            reOutStream	= new AudioFileDescr( inStream );
            ggOutput.fillStream( reOutStream );
            reOutF		= AudioFile.openAsWrite( reOutStream );
            if( pr.intg[ PR_MODE ] == MODE_UNTOUCHED ) {
                imOutStream			= new AudioFileDescr( reOutStream );
                imOutStream.file	= new File( pr.text[ PR_IMOUTPUTFILE ]);
                imOutF				= AudioFile.openAsWrite( imOutStream );
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- parameter inits ----

            modulate		= (pr.intg[ PR_MODE ] == MODE_UPSHIFT) || (pr.intg[ PR_MODE ] == MODE_DOWNSHIFT);
            shiftFreq		= pr.para[ PR_FREQ ].value;	// ABS_HZ

            fltFreq		= inStream.rate * 0.245;					// account for lp transition band
            fltShift	= fltFreq;
            if( modulate && pr.bool[ PR_ANTIALIAS ]) {
//				d1		 = Math.max( 100.0, fltFreq - shiftFreq * 0.625 );
                d1		 = Math.max( 0.0, fltFreq - shiftFreq * 0.5 );
                if( pr.intg[ PR_MODE ] == MODE_UPSHIFT ) {
                    fltShift = d1;
                } else {
                    fltShift += fltFreq - d1;
                }
                fltFreq	 = d1;
            }
            fltShift	   *= Constants.PI2 / inStream.rate;		// normalized freq ready for Math.cos/sin
            shiftFreq	   *= Constants.PI2 / inStream.rate;
//System.out.println( "fltFreq "+fltFreq+"; fltShift "+fltShift+"; modFreq "+shiftFreq );

            fltBox			= new FilterBox();
            fltBox.filterType= FilterBox.FLT_LOWPASS;
            fltBox.cutOff	= new Param( fltFreq, Param.ABS_HZ );
//			fltBox.bandwidth= new Param( fltFreq, Param.OFFSET_HZ );
            fltLength		= fltBox.calcLength( inStream, FIRDesignerDlg.QUAL_VERYGOOD );
            skip			= fltLength.x * 2;							// support not written out
            i				= (fltLength.x + fltLength.y) * 2 - 1;		// length after conv. filter with itself
            j				= i + i - 1;								// length after conv. with input
            for( fftLength = 2; fftLength < j; fftLength <<= 1 ) ;
            inputLen		= fftLength - i - 1;	// + 1

// !we don't optimize because overlap handling would get tricky!
//			j				= (fftLength >> 1) + 1 - i;
//			if( (j > 0) && (((float) i / (float) j) < ((float) inputLen / (float) i)) ) {	// optimum fft length
//				fftLength >>= 1;
//				inputLen	= j;
//			}
//System.out.println( "fftLength : "+fftLength+"; input "+inputLen+"; flt "+(fltLength.x+fltLength.y)+"; skip "+skip );

            fftBuf1			= new float[ fftLength << 1 ];
            fftBuf2			= new float[ fftLength << 1 ];
            reBuf			= new float[ inChanNum ][ fftLength + 2 ];
            reOverBuf		= new float[ inChanNum ][ fftLength - inputLen ];
            if( imOutF != null ) {
                imBuf		= new float[ inChanNum ][ fftLength ];
                imOverBuf	= new float[ inChanNum ][ fftLength - inputLen ];
            }

            /*

             Q2.10: How do I calculate the coefficients for a Hilbert transformer?

            Updated 6/3/98

            For all the gory details, I suggest the paper: Andrew Reilly and Gordon Frazer and Boualem Boashash:
            Analytic signal generation---tips and traps, IEEE Transactions on Signal Processing, no. 11, vol. 42,
            Nov. 1994, pp. 3241-3245.

            For comp.dsp, the gist is:

            1. Design a half-bandwidth real low-pass FIR filter using whatever optimal method you choose, with the
            principle design criterion being minimization of the maximum attenuation in the band f_s/4 to f_s/2.

            2. Modulate this by exp(2 pi f_s/4 t), so that now your stop-band is the negative frequencies, the
            pass-band is the positive frequencies, and the roll-off at each end does not extend into the negative
            frequency band.

            3. either use it as a complex FIR filter, or a pair of I/Q real filters in whatever FIR implementation
            you have available.

            If your original filter design produced an impulse response with an even number of taps, then the
            filtering in 3 will introduce a spurious half-sample delay (resampling the real signal component),
            but that does not matter for many applications, and such filters have other features to recommend them.

            Andrew Reilly [Reilly@zeta.org.au]

             */

            // we design a half-nyquist lp filter and shift it up by that freq. to have a real=>analytic filter
            // see comp.dsp algorithms-faq Q2.10
            fltBox.calcIR( inStream, FIRDesignerDlg.QUAL_VERYGOOD, Filter.WIN_BLACKMAN, fftBuf1, fltLength );
            Fourier.realTransform( fftBuf1, fftLength, Fourier.FORWARD );
            // ---- "medium"-length sinc-lp convolved with itself produces a sharp and non-overshooting filter! ----
            for( i = 0; i <= fftLength; i += 2 ) {
                j			= i + 1;
                d1			= fftBuf1[ i ];
                d2			= fftBuf1[ j ];
                fftBuf1[ i ]= (float) (d1*d1 - d2*d2);		// complex mult
                fftBuf1[ j ]= (float) (2*d1*d2);
            }
            Fourier.realTransform( fftBuf1, fftLength, Fourier.INVERSE );
            // ---- real=>complex + modulation with exp(ismpRate/4 +/- (?) antialias) ----
            // Note 2019: Reilly/Frazer/Boashash use fs/4; I think the below is bad by using fltShift (0.245 fs)
            // k is chosen so that the filter-centre is zero degrees, otherwise we introduce phase shift
            for( i = fftLength - 1, k = i - fltLength.x, j = fftBuf1.length - 1; i >= 0; i--, k-- ) {
                d1				= -fltShift * k;
                fftBuf1[ j-- ]	= (float) (fftBuf1[ i ] * Math.sin( d1 ));		// img
                fftBuf1[ j-- ]	= (float) (fftBuf1[ i ] * Math.cos( d1 ));		// real
            }
            Fourier.complexTransform( fftBuf1, fftLength, Fourier.FORWARD );

//for( i = 0; i < fftLength; i++ ) {
//reBuf[ 0 ][ i ] = (float) Math.sqrt( fftBuf1[ i<<1 ]*fftBuf1[ i<<1 ]+fftBuf1[ (i<<1)+1 ]*fftBuf1[ (i<<1)+1 ]);
//}
//Debug.view( reBuf[ 0 ], "hilbert cmplx fft" );

            progOff		= 0;
            progLen		= (long) inLength * (2 + inChanNum);

            // normalization requires temp files
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                reTempFile	= new File[ inChanNum ];
                reFloatF	= new FloatFile[ inChanNum ];
                for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
                    reTempFile[ ch ]	= null;
                    reFloatF[ ch ]		= null;
                }
                for( ch = 0; ch < inChanNum; ch++ ) {
                    reTempFile[ ch ]	= IOUtil.createTempFile();
                    reFloatF[ ch ]		= new FloatFile( reTempFile[ ch ], GenericFile.MODE_OUTPUT );
                }
                if( imOutF != null ) {
                    imTempFile	= new File[ inChanNum ];
                    imFloatF	= new FloatFile[ inChanNum ];
                    for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
                        imTempFile[ ch ]	= null;
                        imFloatF[ ch ]		= null;
                    }
                    for( ch = 0; ch < inChanNum; ch++ ) {
                        imTempFile[ ch ]	= IOUtil.createTempFile();
                        imFloatF[ ch ]		= new FloatFile( imTempFile[ ch ], GenericFile.MODE_OUTPUT );
                    }
                }
                progLen	   += (long) inLength;
            } else {													// account for gain loss RealFFT => CmplxIFFT
                d1			= 2.0; // 0.89 * (double) fftLength;
                gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value);
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== the real stuff ====================----
// int b = 0;

            framesRead		= 0;
            framesWritten	= 0;

            while( threadRunning && (framesWritten < inLength) ) {

                chunkLength = Math.min( inputLen, inLength - framesRead );
            // ---- read input chunk ----
                for( off = 0; threadRunning && (off < chunkLength); ) {
                    len	= Math.min( 8192, chunkLength - off );
                    inF.readFrames( reBuf, off, len );
//					framesRead	+= len;
                    progOff		+= len;
                    off			+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

                // zero padding
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = reBuf[ ch ];
                    for( i = chunkLength; i < convBuf1.length; i++ ) {
                        convBuf1[ i ] = 0.0f;
                    }
                }

                for( ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
                    convBuf1 = reBuf[ ch ];
                // ---- real fft input + convert to complex + convolve with filter + ifft ----
                    Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
                    System.arraycopy( convBuf1, 0, fftBuf2, 0, fftLength );			// pos.freq. via real transform
                    for( i = fftLength, j = fftLength; i > 0; i -= 2, j += 2 ) {	// neg.freq. complex conjugate
                        fftBuf2[ j ]	= fftBuf2[ i ];
                        fftBuf2[ j+1 ]	= -fftBuf2[ i+1 ];
                    }
                    Fourier.complexMult( fftBuf1, 0, fftBuf2, 0, fftBuf2, 0, fftLength << 1 );
                    Fourier.complexTransform( fftBuf2, fftLength, Fourier.INVERSE );
                    progOff	 += chunkLength;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                // .... check running ....
                    if( !threadRunning ) break topLevel;

                // ---- post proc ----
                    switch( pr.intg[ PR_MODE ]) {
                    case MODE_UPSHIFT:
                        for( i = 0, j = 0, k = framesRead; i < fftLength; i++ ) {
                            d1				= shiftFreq * k++;
                            convBuf1[ i ]	= (float) (fftBuf2[ j++ ] * Math.cos( d1 ) + fftBuf2[ j++ ] * Math.sin( d1 ));
                        }
                        break;
                    case MODE_DOWNSHIFT:
                        for( i = 0, j = 0, k = framesRead; i < fftLength; i++ ) {
                            d1				= shiftFreq * k++;
                            convBuf1[ i ]	= (float) (fftBuf2[ j++ ] * Math.cos( d1 ) - fftBuf2[ j++ ] * Math.sin( d1 ));
                        }
                        break;
                    case MODE_ENVELOPE:
                        for( i = 0, j = 0; i < fftLength; i++ ) {
                            d1				=  fftBuf2[ j++ ];
                            d2				=  fftBuf2[ j++ ];
                            convBuf1[ i ]	= (float) Math.sqrt( d1*d1 + d2*d2 );	// cos^2 + sin^2 = 1
                        }
                        break;
                    default:	// MODE_UNTOUCHED
                        convBuf2 = imBuf[ ch ];
                        for( i = 0, j = 0; i < fftLength; i++ ) {
                            convBuf1[ i ]	= fftBuf2[ j++ ];
                            convBuf2[ i ]	= fftBuf2[ j++ ];
                        }
                        break;
                    }
                } // for channels

                framesRead	+= chunkLength;

            // ---- handle overlaps ----
                for( ch = 0; ch < inChanNum; ch++ ) {
                    Util.add( reOverBuf[ ch ], 0, reBuf[ ch ], 0, fftLength - inputLen );
                    System.arraycopy( reBuf[ ch ], chunkLength, reOverBuf[ ch ], 0, fftLength - inputLen );
                    if( imBuf != null ) {
                        Util.add( imOverBuf[ ch ], 0, imBuf[ ch ], 0, fftLength - inputLen );
                        System.arraycopy( imBuf[ ch ], chunkLength, imOverBuf[ ch ], 0, fftLength - inputLen );
                    }
                }

                chunkLength = Math.min( inputLen, inLength - framesWritten );
            // ---- write output chunk ----
                if( reFloatF != null ) {
                    for( off = skip; threadRunning && (off < chunkLength); ) {
                        len	= Math.min( 8192, chunkLength - off );
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            reFloatF[ ch ].writeFloats( reBuf[ ch ], off, len );
                            if( imFloatF != null ) {
                                imFloatF[ ch ].writeFloats( imBuf[ ch ], off, len );
                            }
                        }
                        progOff		  += len;
                        off			  += len;
                        framesWritten += len;
                    // .... progress ....
                        setProgression( (float) progOff / (float) progLen );
                    }
                } else {
                    for( ch = 0; ch < inChanNum; ch++ ) {
                        Util.mult( reBuf[ ch ], 0, chunkLength, gain );
                    }
                    for( off = skip; threadRunning && (off < chunkLength); ) {
                        len	= Math.min( 8192, chunkLength - off );
                        reOutF.writeFrames( reBuf, off, len );
                        if( imOutF != null ) {
                            imOutF.writeFrames( imBuf, off, len );
                        }
                        progOff		  += len;
                        off			  += len;
                        framesWritten += len;
                    // .... progress ....
                        setProgression( (float) progOff / (float) progLen );
                    }
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

                // check max amp
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = reBuf[ ch ];
                    for( i = skip; i < chunkLength; i++ ) {
                        f1 = Math.abs( convBuf1[ i ]);
                        if( f1 > maxAmp ) {
                            maxAmp = f1;
                        }
                    }
                    if( imBuf != null ) {
                        convBuf1 = imBuf[ ch ];
                        for( i = skip; i < chunkLength; i++ ) {
                            f1 = Math.abs( convBuf1[ i ]);
                            if( f1 > maxAmp ) {
                                maxAmp = f1;
                            }
                        }
                    }
                }

                if( skip > 0 ) {
                    skip = Math.max( 0, skip - chunkLength );
                }
            } // until framesWritten == outLength
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== normalize output ====================----

            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / peakGain.value, peakGain.unit ), null )).value;
                f1		 = (imOutF != null) ? ((1.0f + getProgression()) / 2) : 1.0f;
                normalizeAudioFile( reFloatF, reOutF, reBuf, gain, f1 );
                if( imOutF != null ) {
                    normalizeAudioFile( imFloatF, imOutF, reBuf, gain, 1.0f );
                }
                maxAmp	*= gain;

                for( ch = 0; ch < inChanNum; ch++ ) {
                    reFloatF[ ch ].cleanUp();
                    reFloatF[ ch ]		= null;
                    reTempFile[ ch ].delete();
                    reTempFile[ ch ]	= null;
                    if( imFloatF != null ) {
                        imFloatF[ ch ].cleanUp();
                        imFloatF[ ch ]		= null;
                        imTempFile[ ch ].delete();
                        imTempFile[ ch ]	= null;
                    }
                }
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            reOutF.close();
            reOutF		= null;
            reOutStream	= null;
            if( imOutF != null ) {
                imOutF.close();
                imOutF		= null;
                imOutStream	= null;
            }
            inF.close();
            inF			= null;
            inStream	= null;
            fftBuf1		= null;
            fftBuf2		= null;
            reBuf		= null;
            imBuf		= null;

            // inform about clipping/ low level
            handleClipping( maxAmp );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            reOutStream	= null;
            imOutStream	= null;
            reBuf		= null;
            imBuf		= null;
            fftBuf1		= null;
            fftBuf2		= null;
            convBuf1	= null;
            convBuf2	= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( inF != null ) {
            inF.cleanUp();
            inF = null;
        }
        if( reOutF != null ) {
            reOutF.cleanUp();
            reOutF = null;
        }
        if( imOutF != null ) {
            imOutF.cleanUp();
            imOutF = null;
        }
        if( reFloatF != null ) {
            for( ch = 0; ch < reFloatF.length; ch++ ) {
                if( reFloatF[ ch ] != null ) {
                    reFloatF[ ch ].cleanUp();
                    reFloatF[ ch ] = null;
                }
                if( reTempFile[ ch ] != null ) {
                    reTempFile[ ch ].delete();
                    reTempFile[ ch ] = null;
                }
            }
        }
        if( imFloatF != null ) {
            for( ch = 0; ch < imFloatF.length; ch++ ) {
                if( imFloatF[ ch ] != null ) {
                    imFloatF[ ch ].cleanUp();
                    imFloatF[ ch ] = null;
                }
                if( imTempFile[ ch ] != null ) {
                    imTempFile[ ch ].delete();
                    imTempFile[ ch ] = null;
                }
            }
        }
    } // process()


    protected void reflectPropertyChanges()
    {
        super.reflectPropertyChanges();

        Component	c;
        boolean		b;

        c = gui.getItemObj( GG_IMOUTPUTFILE );
        if( c != null ) {
            c.setEnabled( pr.intg[ PR_MODE ] == MODE_UNTOUCHED );
        }
        c = gui.getItemObj( GG_FREQ );
        b = (pr.intg[ PR_MODE ] == MODE_UPSHIFT) || (pr.intg[ PR_MODE ] == MODE_DOWNSHIFT);
        if( c != null ) {
            c.setEnabled( b );
        }
        c = gui.getItemObj( GG_FREQMOD );
        if( c != null ) {
            c.setEnabled( b );
        }
        c = gui.getItemObj( GG_FREQMODDEPTH );
        if( c != null ) {
            c.setEnabled( b && pr.bool[ PR_FREQMOD ]);
        }
        c = gui.getItemObj( GG_FREQENV );
        if( c != null ) {
            c.setEnabled( b && pr.bool[ PR_FREQMOD ]);
        }
        c = gui.getItemObj( GG_ANTIALIAS );
        if( c != null ) {
            c.setEnabled( b );
        }
    }
}