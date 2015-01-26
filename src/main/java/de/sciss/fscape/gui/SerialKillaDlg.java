/*
 *  SerialKillaDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Serialism sucked and it's still there out in the world in
 *	the heads of too many composers who think pitch is some
 *	important quality of music. This module generates tracks
 *	of three alternative parameters which can be used to
 *	organize sound.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class SerialKillaDlg
extends ModulePanel
{
    // Properties (defaults)
    private static final int PR_INPUTFILE		= 0;		// pr.text
    private static final int PR_NOUTPUTFILE		= 1;
    private static final int PR_TOUTPUTFILE		= 2;
    private static final int PR_EOUTPUTFILE		= 3;
    private static final int PR_NOUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_NOUTPUTRES		= 1;
    private static final int PR_TOUTPUTTYPE		= 2;		// pr.intg
    private static final int PR_TOUTPUTRES		= 3;
    private static final int PR_EOUTPUTTYPE		= 4;		// pr.intg
    private static final int PR_EOUTPUTRES		= 5;
//	private static final int PR_GAINTYPE		= 6;
    private static final int PR_FRAMESIZE		= 7;
    private static final int PR_OVERLAP			= 8;
//	private static final int PR_GAIN			= 0;		// pr.para
    private static final int PR_OUTNOISE		= 0;		// pr.bool
    private static final int PR_OUTTILT			= 1;
    private static final int PR_OUTENERGY		= 2;

    private static final String PRN_INPUTFILE			= "InputFile";
    private static final String PRN_NOUTPUTFILE		= "NOutputFile";
    private static final String PRN_NOUTPUTTYPE		= "NOutputType";
    private static final String PRN_NOUTPUTRES		= "NOutputReso";
    private static final String PRN_TOUTPUTFILE		= "TOutputFile";
    private static final String PRN_TOUTPUTTYPE		= "TOutputType";
    private static final String PRN_TOUTPUTRES		= "TOutputReso";
    private static final String PRN_EOUTPUTFILE		= "EOutputFile";
    private static final String PRN_EOUTPUTTYPE		= "EOutputType";
    private static final String PRN_EOUTPUTRES		= "EOutputReso";
    private static final String PRN_OUTNOISE		= "OutNoise";
    private static final String PRN_OUTTILT			= "OutTilt";
    private static final String PRN_OUTENERGY		= "OutEnergy";
    private static final String PRN_FRAMESIZE		= "FrameSize";
    private static final String PRN_OVERLAP			= "Overlap";

    private static final String	prText[]		= { "", "", "", "" };
    private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_NOUTPUTFILE, PRN_TOUTPUTFILE, PRN_EOUTPUTFILE };
    private static final int		prIntg[]	= { 0, 0, 0, 0, 0, 0, GAIN_UNITY, 5, 2 };
    private static final String	prIntgName[]	= { PRN_NOUTPUTTYPE, PRN_NOUTPUTRES, PRN_TOUTPUTTYPE, PRN_TOUTPUTRES,
                                                    PRN_EOUTPUTTYPE, PRN_EOUTPUTRES, PRN_GAINTYPE, PRN_FRAMESIZE,
                                                    PRN_OVERLAP };
    private static final boolean	prBool[]	= { true, false, false };
    private static final String	prBoolName[]	= { PRN_OUTNOISE, PRN_OUTTILT, PRN_OUTENERGY };
//	private static final Param	prPara[]		= { null };
//	private static final String	prParaName[]	= { PRN_GAIN };

    private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_NOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_NOUTPUTFILE;
    private static final int GG_NOUTPUTTYPE		= GG_OFF_CHOICE		+ PR_NOUTPUTTYPE;
    private static final int GG_NOUTPUTRES		= GG_OFF_CHOICE		+ PR_NOUTPUTRES;
    private static final int GG_TOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_TOUTPUTFILE;
    private static final int GG_TOUTPUTTYPE		= GG_OFF_CHOICE		+ PR_TOUTPUTTYPE;
    private static final int GG_TOUTPUTRES		= GG_OFF_CHOICE		+ PR_TOUTPUTRES;
    private static final int GG_EOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_EOUTPUTFILE;
    private static final int GG_EOUTPUTTYPE		= GG_OFF_CHOICE		+ PR_EOUTPUTTYPE;
    private static final int GG_EOUTPUTRES		= GG_OFF_CHOICE		+ PR_EOUTPUTRES;
//	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_FRAMESIZE		= GG_OFF_CHOICE		+ PR_FRAMESIZE;
    private static final int GG_OVERLAP			= GG_OFF_CHOICE		+ PR_OVERLAP;
//	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_OUTNOISE		= GG_OFF_CHECKBOX	+ PR_OUTNOISE;
    private static final int GG_OUTTILT			= GG_OFF_CHECKBOX	+ PR_OUTTILT;
    private static final int GG_OUTENERGY		= GG_OFF_CHECKBOX	+ PR_OUTENERGY;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    private static final String	ERR_NOOUTPUT		= "No outputs checked!";

    private static final String	PTRN_NOISE			= "Mean noise  : {0,number,#,##0.0}%";
    private static final String	PTRN_TILT			= "Mean tilt   : {0,number,#,##0.0} Hz";
    private static final String	PTRN_ENERGY			= "Mean energy : {0,number,#,##0.0} dB";

// -------- public Methoden --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public SerialKillaDlg() {
        super("Serial Killer");
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
//			static_pr.para		= prPara;
//			static_pr.para[ PR_GAIN ]				= new Param(  0.0, Param.DECIBEL_AMP );
//			static_pr.paraName	= prParaName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
//			static_pr.envl		= prEnvl;
//			static_pr.envl[ PR_RATEMODENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
//			static_pr.envl[ PR_RIGHTCHANMODENV ]= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
//			static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_NOUTPUTTYPE ); // just the type
            fillDefaultAudioDescr( static_pr.intg, PR_TOUTPUTTYPE );
            fillDefaultAudioDescr( static_pr.intg, PR_EOUTPUTTYPE );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- GUI bauen --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggNOutputFile, ggTOutputFile, ggEOutputFile;
        PathField[]			ggInputs;
        JComboBox			ggFrameSize, ggOverlap;
        JCheckBox			ggOutNoise, ggOutTilt, ggOutEnergy;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

        ItemListener il = new ItemListener() {
            public void itemStateChanged( ItemEvent e )
            {
                int	ID = gui.getItemID( e );

                switch( ID ) {
                case GG_OUTNOISE:
                case GG_OUTTILT:
                case GG_OUTENERGY:
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

        ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select input file" );
        ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInputFile, GG_INPUTFILE, null );

        ggNOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggNOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 1 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggNOutputFile.deriveFrom( ggInputs, "$D0$F0SKNoise$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        ggOutNoise		= new JCheckBox( "Noise output" );
        gui.addCheckbox( ggOutNoise, GG_OUTNOISE, il );
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggNOutputFile, GG_NOUTPUTFILE, null );
        gui.registerGadget( ggNOutputFile.getTypeGadget(), GG_NOUTPUTTYPE );
        gui.registerGadget( ggNOutputFile.getResGadget(), GG_NOUTPUTRES );

        ggTOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggTOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggTOutputFile.deriveFrom( ggInputs, "$D0$F0SKTilt$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        ggOutTilt		= new JCheckBox( "Tilt output" );
        gui.addCheckbox( ggOutTilt, GG_OUTTILT, il );
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggTOutputFile, GG_TOUTPUTFILE, null );
        gui.registerGadget( ggTOutputFile.getTypeGadget(), GG_TOUTPUTTYPE );
        gui.registerGadget( ggTOutputFile.getResGadget(), GG_TOUTPUTRES );

        ggEOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggEOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggEOutputFile.deriveFrom( ggInputs, "$D0$F0SKEnergy$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        ggOutEnergy		= new JCheckBox( "Energy output" );
        gui.addCheckbox( ggOutEnergy, GG_OUTENERGY, il );
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggEOutputFile, GG_EOUTPUTFILE, null );
        gui.registerGadget( ggEOutputFile.getTypeGadget(), GG_EOUTPUTTYPE );
        gui.registerGadget( ggEOutputFile.getResGadget(), GG_EOUTPUTRES );

//		ggGain			= createGadgets( GGTYPE_GAIN );
//		con.weightx		= 0.1;
//		con.gridwidth	= 1;
//		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
//		con.weightx		= 0.4;
//		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, this );
//		con.weightx		= 0.5;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, this );

    // -------- Settings --------
    gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggFrameSize		= new JComboBox();
        for( int i = 32; i <= 32768; i <<= 1 ) {
            ggFrameSize.addItem( String.valueOf( i ));
        }
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Frame size [smp]", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addChoice( ggFrameSize, GG_FRAMESIZE, il );

        ggOverlap		= new JComboBox();
        for( int i = 0; i < 8; i++ ) {
            ggOverlap.addItem( (1<<i) + "x" );
        }
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Overlap", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggOverlap, GG_OVERLAP, il );

        initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
    }

    /**
     *	Werte aus Prop-Array in GUI uebertragen
     */
    public void fillGUI()
    {
        super.fillGUI();
        super.fillGUI( gui );
    }

    /**
     *	Werte aus GUI in Prop-Array uebertragen
     */
    public void fillPropertyArray()
    {
        super.fillPropertyArray();
        super.fillPropertyArray( gui );
    }

// -------- Processor Interface --------

    protected void process()
    {
        int					i, j, k, m, ch, off;
        int					len, chunkLength;
        long				progOff, progLen;
        float				f1, f2, f3;
        double				d1, d4;

        // io
        AudioFile			inF				= null;
        AudioFile			nOutF			= null;
        AudioFile			tOutF			= null;
        AudioFile			eOutF			= null;
        AudioFileDescr			inStream		= null;
        AudioFileDescr			nOutStream		= null;
        AudioFileDescr			tOutStream		= null;
        AudioFileDescr			eOutStream		= null;
//		FloatFile[]			floatF			= null;
//		File[]				tempFile		= null;
        float[][]			inBuf			= null;
        float[][]			nOutBuf			= null;
        float[][]			tOutBuf			= null;
        float[][]			eOutBuf			= null;
        float[]				convBuf1, fftBuf, weightBuf, window, wincorr, maxima;
        int[]				maximaPos;

        int					numMaxima;

        // Synthesize
//		float				gain			= 1.0f;								// gain abs amp
//		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

        // Smp Init
        int					inLength, inChanNum, outLength, outChanNum;
        int					framesRead, framesWritten;
        int					fftLength, frameLength, winStep;

//		float				maxAmp			= 0.0f;

//		float				winEnergy;
        double				totalTilt, totalNoise, totalEnergy;

        boolean				outNoise, outTilt, outEnergy;
        MessageFormat		msgForm;
        Object[]			msgArgs			= new Object[1];

        PathField			ggOutput;

topLevel: try {

        // ---- open input, output; init ----

            // input
            inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
            inStream		= inF.getDescr();
            inChanNum		= inStream.channels;
            inLength		= (int) inStream.length;
            // this helps to prevent errors from empty files!
            if( inLength * inChanNum < 1 ) throw new EOFException( ERR_EMPTY );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            outChanNum 		= inChanNum;
            outNoise		= pr.bool[ PR_OUTNOISE ];
            outTilt			= pr.bool[ PR_OUTTILT ];
            outEnergy		= pr.bool[ PR_OUTENERGY ];

            if( !outNoise && !outTilt && !outEnergy ) throw new IOException( ERR_NOOUTPUT );

            // output
            if( outNoise ) {
                ggOutput	= (PathField) gui.getItemObj( GG_NOUTPUTFILE );
                if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
                nOutStream	= new AudioFileDescr( inStream );
                nOutStream.channels = outChanNum;
                ggOutput.fillStream( nOutStream );
                nOutF		= AudioFile.openAsWrite( nOutStream );
            // .... check running ....
                if( !threadRunning ) break topLevel;
            }
            if( outTilt ) {
                ggOutput	= (PathField) gui.getItemObj( GG_TOUTPUTFILE );
                if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
                tOutStream	= new AudioFileDescr( inStream );
                tOutStream.channels = outChanNum;
                ggOutput.fillStream( tOutStream );
                tOutF		= AudioFile.openAsWrite( tOutStream );
            // .... check running ....
                if( !threadRunning ) break topLevel;
            }
            if( outEnergy ) {
                ggOutput	= (PathField) gui.getItemObj( GG_EOUTPUTFILE );
                if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
                eOutStream	= new AudioFileDescr( inStream );
                eOutStream.channels = outChanNum;
                ggOutput.fillStream( eOutStream );
                eOutF		= AudioFile.openAsWrite( eOutStream );
            // .... check running ....
                if( !threadRunning ) break topLevel;
            }

            frameLength			= 32 << pr.intg[ PR_FRAMESIZE ];
            fftLength			= frameLength << 1;
            winStep				= frameLength >> pr.intg[ PR_OVERLAP ];
            numMaxima			= frameLength >> 7;
            outLength			= (inLength + winStep - 1) / winStep;

            inBuf				= new float[ inChanNum ][ frameLength ];
            nOutBuf				= new float[ outChanNum ][ 8 ];		// XXX
            tOutBuf				= new float[ outChanNum ][ 8 ];		// XXX
            eOutBuf				= new float[ outChanNum ][ 8 ];		// XXX
            fftBuf				= new float[ fftLength + 2 ];
            window				= Filter.createFullWindow( frameLength, Filter.WIN_HAMMING );
            wincorr				= new float[ frameLength ];
            maxima				= new float[ numMaxima ];
            maximaPos			= new int[ numMaxima ];

            totalTilt			= 0.0;
            totalNoise			= 0.0;
            totalEnergy			= 0.0;

            // Fenster muss aus Autokorr. rausgerechnet werden
            System.arraycopy( window, 0, fftBuf, 0, frameLength );
            for( i = frameLength; i < fftLength; ) {
                fftBuf[ i++ ] = 0.0f;
            }
            Fourier.realTransform( fftBuf, fftLength, Fourier.FORWARD );
            d1	= 0.0;
            for( i = 0, k = 0; i <= fftLength; ) {
                j				= i++;
                f1				= (fftBuf[ j ]*fftBuf[ j ] + fftBuf[ i ]*fftBuf[ i ]);
                fftBuf[ j ]		= f1;
                d1			   += f1;
//				fftBuf[ j ]		= (fftBuf[ j ]*fftBuf[ j ] + fftBuf[ i ]*fftBuf[ i ]) * weightBuf[ k++ ];
                fftBuf[ i++ ]	= 0.0f;
            }
//			winEnergy			= (float) Math.sqrt( d1 );
            Fourier.realTransform( fftBuf, fftLength, Fourier.INVERSE );
            Util.mult( fftBuf, 0, frameLength, 1.0f / fftBuf[0] );

            f1	= fftBuf[ 0 ];
            for( i = 1; i < frameLength; i++ ) {
                f1				= ((float) (frameLength - i) / (float) frameLength) / fftBuf[ i ];
                if( f1 > 500f ) break;	// begins to oscillate wild
                wincorr[i]		= f1;
            }
            for( ; i < frameLength; i++ ) {
                wincorr[i]		= 500f;
            }
//Debug.view( wincorr, "win corr" );

            // calc dB(A) weighting factors
            weightBuf			= new float[ frameLength + 1 ];
            f1					= (float) inStream.rate / 2;
            for( i = 0; i <= frameLength; i++ ) {
                weightBuf[ i ]	= (float) i / (float) frameLength * f1;
            }
            Filter.getDBAweights( weightBuf, weightBuf, frameLength + 1 );
            for( i = 0; i <= frameLength; i++ ) {
                weightBuf[ i ] *= weightBuf[ i ];	// squared to be used directly in autocorrelation formula
            }

            // initialize various stuff
            progOff				= 0;
            progLen				= (long) inLength + (long) outLength;

            // normalization requires temp files
//			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
//				tempFile	= new File[ outChanNum ];
//				floatF		= new FloatFile[ outChanNum ];
//				for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
//					tempFile[ ch ]	= null;
//					floatF[ ch ]	= null;
//				}
//				for( ch = 0; ch < outChanNum; ch++ ) {
//					tempFile[ ch ]	= IOUtil.createTempFile();
//					floatF[ ch ]	= new FloatFile( tempFile[ ch ], FloatFile.MODE_OUTPUT );
//				}
//				progLen	   += (long) outLength;
//			}
//			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
//				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
//gain	= 1.0f;
//			}
//		// .... check running ....
//			if( !threadRunning ) break topLevel;

        // ----==================== core ====================----
        //
        //	How it works:	overlapped FFT-frames read in ---> autocorrelation calculated + normalized
        //					24 biggest local maxima picked out
        //					areas between neighbouring maxima calculated (triangular interpolation)
        //					1.0 - ratio between area and max. area calculated = degree of noise
        //					needs smoothing?

            framesWritten	= 0;
            framesRead		= 0;
            off				= 0;

            while( threadRunning && (framesWritten < outLength) ) {
                // ==================== read input chunk ====================
                len			= Math.min( inLength - framesRead, frameLength - off );
                chunkLength	= len + off;
                inF.readFrames( inBuf, off, len );
                framesRead += len;
                progOff	   += len;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            // .... check running ....
                if( !threadRunning ) break topLevel;

                // zero-padding last chunk
                if( chunkLength < frameLength ) {
                    for( ch = 0; ch < inChanNum; ch++ ) {
                        convBuf1 = inBuf[ ch ];
                        for( i = chunkLength; i < frameLength; i++ ) {
                            convBuf1[ i ] = 0.0f;
                        }
                    }
                }

                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = inBuf[ ch ];
                    System.arraycopy( convBuf1, 0, fftBuf, 0, frameLength );

                    Util.mult( window, 0, fftBuf, 0, frameLength );
                    for( i = frameLength; i < fftLength; ) {
                        fftBuf[ i++ ] = 0.0f;
                    }
                    Fourier.realTransform( fftBuf, fftLength, Fourier.FORWARD );

                    d1	= 0.0;
//					d2	= 0.0;
                    d4	= 0.0;

                    // Autocorrelation (a + ib) * (a - ib)
                    for( i = 0, k = 0; i <= fftLength; k++ ) {
                        j				= i++;
//						fftBuf[ j ]		= (fftBuf[ j ]*fftBuf[ j ] + fftBuf[ i ]*fftBuf[ i ]);
                        f1				= (fftBuf[ j ]*fftBuf[ j ] + fftBuf[ i ]*fftBuf[ i ]) * weightBuf[ k ];
                        d4			   += f1;
                        if( outTilt ) {
                            d1		   += f1 * k;
                        }
                        fftBuf[ j ]		= f1;
                        fftBuf[ i++ ]	= 0.0f;
                    }

//if( framesWritten == 0 ) System.out.println( "d1 "+d1+"; d2 "+d2 );

//					d4	= Math.sqrt( d4 ) / frameLength;

                // ---------- Tilt ----------
                    if( outTilt ) {
                        if( d4 > 0.0 ) {
                            f1	= (float) (d1 / (d4 * frameLength));	// XXX logarithmische korrektur XXX
                        } else {
                            f1 = 0.5f;
                        }
                        tOutBuf[ ch ][ 0 ] = f1;
                        totalTilt += f1;
                    }

                // ---------- Energy ----------
                    if( outEnergy ) {
                        f1	= Math.min( 1.0f, (float) (Math.sqrt( d4 ) / frameLength) );		// RMS (nach Parseval)
                        eOutBuf[ ch ][ 0 ] = f1;
                        totalEnergy += f1;
                    }

                // ---------- Noise ----------
                    if( outNoise ) {
                        Fourier.realTransform( fftBuf, fftLength, Fourier.INVERSE );

                        f1 = fftBuf[0];
                        if( f1 > 0.0f ) {
                            Util.mult( fftBuf, 0, frameLength, 1.0f / f1 );
                            Util.mult( wincorr, 0, fftBuf, 0, frameLength );

//if( (framesWritten == 0) && (ch == 0) ) {
//	float[] test = new float[ frameLength ];
//	System.arraycopy( fftBuf, 0, test, 0, frameLength );
//	Debug.view( test, "AutoCorr" );
//}

                            Util.clear( maxima );
                            for( i = 0; i < numMaxima; i++ ) {
                                maximaPos[ i ] = frameLength;
                            }

                            fftBuf[ fftLength ] = 0f;	// last sample is allowed to be local maximum
                            f2	= 1.1f;
                            d1	= 0.0;
                            for( i = 0; i < frameLength; i++ ) {
                                f3  = fftBuf[i];
                                if( (f3 > f2) && (f3 > fftBuf[i+1]) && (f3 > 0.0f) ) {	// trace local maxima contour
                                    if( f3 > maxima[0] ) {
                                        for( j = 1; j < numMaxima; j++ ) {
                                            if( f3 <= maxima[j] ) break;
                                        }
                                        j--;
                                        for( k = 0; k < j; k++ ) {
                                            maxima[k]		= maxima[k+1];
                                            maximaPos[k]	= maximaPos[k+1];
                                        }
                                        maxima[j]		= f3;
                                        maximaPos[j]	= i;
                                    }
//									d1 += (f3 + f1) / 2.0f * (i - j);	// linear area interpolation
//									f1	= f3;
//									j	= i;
                                }
                                f2	= f3;
                            }

//System.out.println( "----Maxima----" );
//for( i = 0; i < numMaxima; i++ ) {
//	System.out.println( "  "+i+" : "+maximaPos[i]+" = "+maxima[i]);
//}	

//System.out.println( "max.[0] = "+maxima[0]+" @ "+ maximaPos[0] + " ---> "+(maxima[0] / 2.0f * (frameLength - maximaPos[0])));				
                            d1 += maxima[0] / 2.0f * (frameLength - maximaPos[0]);	// "kleinstes" Maximum bis zum Ende interpolieren
                                                                                    // ; damit auch Funktionen wie hochfreq. Sinus
                                                                                    // nicht unterbewertet werden, sondern das
                                                                                    // Autokorr.-Dreieck quasi fortgesetzt wird
                            f1	= 1.0f;
                            j	= 0;
                            do {
                                m	= -1;
                                k	= frameLength;
                                for( i = 0; i < numMaxima; i++ ) {		// chronologisch naechstes max finden
                                    if( maximaPos[i] < k ) {
                                        k = maximaPos[i];
                                        m = i;
                                    }
                                }
                                if( m < 0 ) break;

                                d1			   += (f1 + maxima[m]) / 2.0f * (maximaPos[m] - j);	// linear area interpolation
                                j				= maximaPos[m];
                                f1				= maxima[m];
                                maximaPos[m]	= frameLength;	// raus aus dem spiel

                            } while( true );

                            f1 = 1.13f - ((float) d1 * 2.1f / frameLength);	// Erfahrungswerte ;-)
                            f1 = Math.min( 1.0f, Math.max( 0.0f, f1 * f1 ));
                        } else {
                            f1 = 0.0f;
                        }

//if( (framesWritten == 0) && (ch == 0) ) System.out.println( "f1 = "+f1 );

//						nOutBuf[ ch ][ 0 ] = f1 * gain;
                        nOutBuf[ ch ][ 0 ] = f1;
                        totalNoise += f1;
                    } // if( outNoise )
                } // for( channels )

            // ----- write output -----
                if( outTilt ) {
                    tOutF.writeFrames( tOutBuf, 0, 1 );
                }
                if( outNoise ) {
                    nOutF.writeFrames( nOutBuf, 0, 1 );
                }
                if( outEnergy ) {
                    eOutF.writeFrames( eOutBuf, 0, 1 );
                }
                framesWritten++;
                progOff++;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ----- buffer rotation -----
                off = frameLength - winStep;
                for( ch = 0; ch < inChanNum; ch++ ) {
                    System.arraycopy( inBuf[ ch ], winStep, inBuf[ ch ], 0, off );
                }
            } // while( framesWritten < outLength )
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- clean up, normalize ----

            if( outNoise ) {
                msgArgs[0]	= new Double( totalNoise / (outLength * outChanNum) * 100 );
                msgForm		= new MessageFormat( PTRN_NOISE );
                msgForm.setLocale( Locale.US );
                msgForm.applyPattern( PTRN_NOISE );
                System.out.println( msgForm.format( msgArgs ) );
            }
            if( outTilt ) {
                msgArgs[0]	= new Double( totalTilt / (outLength * outChanNum) * (inStream.rate/2) );
                msgForm		= new MessageFormat( PTRN_TILT );
                msgForm.setLocale( Locale.US );
                msgForm.applyPattern( PTRN_TILT );
                System.out.println( msgForm.format( msgArgs ) );
            }
            if( outEnergy ) {
                msgArgs[0]	= new Double( 20 * Math.log( totalEnergy / (outLength * outChanNum) ) / Constants.ln10 );
                msgForm		= new MessageFormat( PTRN_ENERGY );
                msgForm.setLocale( Locale.US );
                msgForm.applyPattern( PTRN_ENERGY );
                System.out.println( msgForm.format( msgArgs ) );
            }

            inF.close();
            inF			= null;
            inStream	= null;

//			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
//				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
//								new Param( 1.0 / (double) maxAmp, Param.ABS_AMP ), null )).val;
//
//				normalizeAudioFile( floatF, nOutF, inBuf, gain, 1.0f );
//				for( ch = 0; ch < outChanNum; ch++ ) {
//					floatF[ ch ].cleanUp();
//					floatF[ ch ] = null;
//					tempFile[ ch ].delete();
//					tempFile[ ch ] = null;
//				}
//				maxAmp *= gain;
//			}

            if( nOutF != null ) {
                nOutF.close();
                nOutF = null;
            }
            if( tOutF != null ) {
                tOutF.close();
                tOutF = null;
            }
            if( eOutF != null ) {
                eOutF.close();
                eOutF = null;
            }

        // ---- Finish ----

            // inform about clipping/ low level
//			handleClipping( maxAmp );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            nOutStream	= null;
            tOutStream	= null;
            inBuf		= null;
            nOutBuf		= null;
            tOutBuf		= null;
            eOutBuf		= null;
            fftBuf		= null;
            weightBuf	= null;
            window		= null;
            wincorr		= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));;
        }

    // ---- cleanup (topLevel) ----
        if( inF != null ) {
            inF.cleanUp();
        }
        if( nOutF != null ) {
            nOutF.cleanUp();
        }
        if( tOutF != null ) {
            tOutF.cleanUp();
        }
        if( eOutF != null ) {
            eOutF.cleanUp();
        }
//		if( floatF != null ) {
//			for( ch = 0; ch < floatF.length; ch++ ) {
//				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
//				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
//			}
//		}
    } // process()

// -------- private Methoden --------

    protected void reflectPropertyChanges()
    {
        super.reflectPropertyChanges();

        Component c;

        c = gui.getItemObj( GG_NOUTPUTFILE );
        if( c != null ) {
            c.setEnabled( pr.bool[ PR_OUTNOISE ]);
        }
        c = gui.getItemObj( GG_TOUTPUTFILE );
        if( c != null ) {
            c.setEnabled( pr.bool[ PR_OUTTILT ]);
        }
        c = gui.getItemObj( GG_EOUTPUTFILE );
        if( c != null ) {
            c.setEnabled( pr.bool[ PR_OUTENERGY ]);
        }
    }
}
// class SerialKillaDlg
