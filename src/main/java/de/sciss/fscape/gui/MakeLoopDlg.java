/*
 *  MakeLoopDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		29-Jun-06	created
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 *  Processing module for splitting
 *	up a file into several splices.
 *	use Concat to re-glue these splices.
 *
 *	@todo		handle markers
 */
public class MakeLoopDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE			= 0;		// pr.text
    private static final int PR_OUTPUTFILE			= 1;
    private static final int PR_GAIN				= 0;		// pr.para
    private static final int PR_INITIALSKIP			= 1;		// pr.para
    private static final int PR_FADELENGTH			= 2;
    private static final int PR_FINALSKIP			= 3;
    private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
    private static final int PR_OUTPUTRES			= 1;
    private static final int PR_FADEPOS				= 2;
    private static final int PR_FADESHAPE			= 3;
    private static final int PR_FADETYPE			= 4;
    private static final int PR_GAINTYPE			= 5;
//	private static final int PR_AUTOSCALE			= 0;		// pr.bool
//	private static final int PR_SEPARATEFILES		= 1;

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_INITIALSKIP		= "InitialSkip";
    private static final String PRN_FADEPOS			= "FadePos";
    private static final String PRN_FADELENGTH		= "FadeLen";
    private static final String PRN_FINALSKIP		= "FinalSkip";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_FADESHAPE		= "FadeShape";
    private static final String PRN_FADETYPE		= "FadeType";
//	private static final String PRN_AUTOSCALE		= "AutoScale";
//	private static final String PRN_SEPARATEFILES	= "SeparateFiles";

    private static final String[]	POS_NAMES		= { "End of Loop \\ Pre Loop /", "Begin of Loop \\ Post Loop /" };
    private static final int		POS_PRE			= 0;
    private static final int		POS_POST		= 1;

    private static final String[]	SHAPE_NAMES		= { "Normal", "Fast in", "Slow in", "Easy in Easy out" };
    private static final int		SHAPE_NORMAL	= 0;
//	private static final int		SHAPE_FASTIN	= 1;
//	private static final int		SHAPE_SLOWIN	= 2;
//	private static final int		SHAPE_EASY		= 3;

    private static final String[]	TYPE_NAMES		= { "Equal Energy", "Equal Power" };
//	private static final int		TYPE_ENERGY		= 0;
    private static final int		TYPE_POWER		= 1;

    private static final String		prText[]		= { "", "" };
    private static final String		prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
    private static final int		prIntg[]		= { 0, 0, POS_PRE, SHAPE_NORMAL, TYPE_POWER, GAIN_UNITY };
    private static final String		prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_FADEPOS, PRN_FADESHAPE, PRN_FADETYPE, PRN_GAINTYPE };
    private static final Param		prPara[]		= { null, null, null, null };
    private static final String		prParaName[]	= { PRN_GAIN, PRN_INITIALSKIP, PRN_FADELENGTH, PRN_FINALSKIP };
//	private static final boolean	prBool[]		= { false, true };
//	private static final String		prBoolName[]	= { PRN_AUTOSCALE, PRN_SEPARATEFILES };

    private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_INITIALSKIP			= GG_OFF_PARAMFIELD	+ PR_INITIALSKIP;
    private static final int GG_FADELENGTH			= GG_OFF_PARAMFIELD	+ PR_FADELENGTH;
    private static final int GG_FINALSKIP			= GG_OFF_PARAMFIELD	+ PR_FINALSKIP;
    private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_FADEPOS				= GG_OFF_CHOICE		+ PR_FADEPOS;
    private static final int GG_FADESHAPE			= GG_OFF_CHOICE		+ PR_FADESHAPE;
    private static final int GG_FADETYPE			= GG_OFF_CHOICE		+ PR_FADETYPE;
    private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
//	private static final int GG_AUTOSCALE			= GG_OFF_CHECKBOX	+ PR_AUTOSCALE;
//	private static final int GG_SEPARATEFILES		= GG_OFF_CHECKBOX	+ PR_SEPARATEFILES;
//	private static final int GG_CURRENTINFO			= GG_OFF_OTHER		+ 0;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public MakeLoopDlg()
    {
        super( "Make Loop" );
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
            static_pr.para[ PR_INITIALSKIP ]	= new Param( 1000.0, Param.ABS_MS );
            static_pr.para[ PR_FADELENGTH ]		= new Param( 1000.0, Param.ABS_MS );
            static_pr.para[ PR_FINALSKIP ]		= new Param(    0.0, Param.ABS_MS );
            static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
            fillDefaultGain( static_pr.para, PR_GAIN );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- Misc init --------

    // -------- build GUI --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggOutputFile;
        Component[]			ggGain;
        PathField[]			ggInputs;
        ParamField			ggParam;
        ParamSpace[]		spcOffset, spcLength;
        ParamSpace			spc;
        JComboBox			ggCombo;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

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

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 1 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggOutputFile.deriveFrom( ggInputs, "$D0$F0Loop$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
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
    gui.addLabel( new GroupLabel( "Loop Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        spcLength		= new ParamSpace[ 3 ];
        spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
        spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
        spcLength[2]	= Constants.spaces[ Constants.ratioTimeSpace ];
        spcOffset		= new ParamSpace[ 3 ];
        spc				= new ParamSpace( Constants.spaces[ Constants.absMsSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
        spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
        spcOffset[0]	= spc; // Constants.spaces[ Constants.offsetMsSpace ];
        spc				= new ParamSpace( Constants.spaces[ Constants.absBeatsSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
        spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
        spcOffset[1]	= spc; // Constants.spaces[ Constants.offsetBeatsSpace ];
//		spc				= new ParamSpace( Constants.spaces[ Constants.fTimeSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
        spc				= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
        spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
        spcOffset[2]	= spc; // Constants.spaces[ Constants.factorTimeSpace ];

        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;

        ggParam			= new ParamField( spcLength );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Crossfade Length", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggParam, GG_FADELENGTH, null );

        ggParam			= new ParamField( spcLength );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Initial Skip", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggParam, GG_INITIALSKIP, null );

        ggCombo			= new JComboBox();
        for( int i = 0; i < POS_NAMES.length; i++ ) {
            ggCombo.addItem( POS_NAMES[ i ]);
        }
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Crossfade Position", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addChoice( ggCombo, GG_FADEPOS, null );

        ggParam			= new ParamField( spcLength );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Final Skip", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggParam, GG_FINALSKIP, null );

        ggCombo			= new JComboBox();
ggCombo.setEnabled( false );
        for( int i = 0; i < SHAPE_NAMES.length; i++ ) {
            ggCombo.addItem( SHAPE_NAMES[ i ]);
        }
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Crossfade Shape", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addChoice( ggCombo, GG_FADESHAPE, null );

        ggCombo			= new JComboBox();
        for( int i = 0; i < TYPE_NAMES.length; i++ ) {
            ggCombo.addItem( TYPE_NAMES[ i ]);
        }
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Crossfade Type", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggCombo, GG_FADETYPE, null );

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
        long				progOff, progLen;

        AudioFile			inF				= null;
        AudioFile			outF			= null;
        AudioFileDescr		inDescr;
        AudioFileDescr		outDescr;
        float[][]			inBuf, fadeBuf;
        float[]				convBuf1;
        float				f1, f2;
        float				maxAmp			= 0f;
        double				d1;
        AudioFile			outF2;
        AudioFile			tempF			= null;
        float				gain;

        long				fadeLen, initialSkip, finalSkip, inLength, fadeInOff, fadeOutOff, copyOff, copyLen, framesWritten;
        int					len, inChans;
        Param				ref;
        boolean				postPos, eqP;

        PathField			ggOutput;

topLevel: try {
        // ---- open input, output; init ----

            inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
            inDescr		= inF.getDescr();
            inChans		= inDescr.channels;
            inLength	= inDescr.length;
            if( inChans * inLength <= 0 ) throw new EOFException( ERR_EMPTY );

            ref			= new Param( AudioFileDescr.samplesToMillis( inDescr, inLength ), Param.ABS_MS );
            fadeLen		= (long) (AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_FADELENGTH ], Param.ABS_MS, ref, null ).value) + 0.5);
            initialSkip	= (long) (AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_INITIALSKIP ], Param.ABS_MS, ref, null ).value) + 0.5);
            finalSkip	= (long) (AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_FINALSKIP ], Param.ABS_MS, ref, null ).value) + 0.5);

            postPos		= pr.intg[ PR_FADEPOS ] == POS_POST; // true; // pr.intg[ PR_FADEPPOS ] == POS_POST;
            eqP			= pr.intg[ PR_FADETYPE] == TYPE_POWER; // pr.intg[ PR_FADEPTYPE ] == TYPE_POWER;

            // constrain
            initialSkip	= Math.min( initialSkip, inLength );
            finalSkip	= Math.min( finalSkip, inLength - initialSkip );
            fadeLen		= Math.min( Math.min( postPos ? finalSkip : initialSkip, fadeLen ), inLength - finalSkip - initialSkip );

            copyOff		= postPos ? initialSkip + fadeLen : initialSkip;
            copyLen		= inLength - finalSkip - initialSkip - fadeLen;
            fadeInOff	= postPos ? initialSkip : initialSkip - fadeLen;
            fadeOutOff	= postPos ? inLength - finalSkip : inLength - finalSkip - fadeLen;

//System.err.println( "copyOff = "+copyOff+"; fadeInOff = "+fadeInOff+"; fadeOutOff = "+fadeOutOff+"; copyLen = "+copyLen );

            // output
            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outDescr	= new AudioFileDescr( inDescr );
            ggOutput.fillStream( outDescr );
            outF		= AudioFile.openAsWrite( outDescr );

            // normalization requires temp files
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                tempF		= createTempFile( inDescr );
                outF2		= tempF;
                gain		= 1.0f;
            } else {
                ref			= new Param( 1.0, Param.ABS_AMP );
                gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ref, null )).value;
                outF2		= outF;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

    // .... check running ....
            if( !threadRunning ) break topLevel;

            inBuf			= new float[ inChans ][ 8192 ];
            fadeBuf			= new float[ inChans ][ (int) Math.min( fadeLen, 8192 )];

            progOff			= 0;
            progLen			= copyLen + fadeLen;

            for( int i = 0; (i < 2) && threadRunning; i++ ) {
                framesWritten = 0;
                if( (i == 0) == postPos ) {		// -------- dem fade --------

                    inF.seekFrame( copyOff );
                    while( (framesWritten < copyLen) && threadRunning ) {
                        len = (int) Math.min( copyLen - framesWritten, 8192 );
//						inF.copyFrames( outF, len );
                        inF.readFrames( inBuf, 0, len );
                        if( gain != 1.0f ) {
                            for( int ch = 0; ch < inChans; ch++ ) {
                                Util.mult( inBuf[ ch ], 0, len, gain );
                            }
                        }
                        outF2.writeFrames( inBuf, 0, len );
                        for( int ch = 0; ch < inChans; ch++ ) {
                            convBuf1 = inBuf[ ch ];
                            for( int j = 0; j < len; j++ ) {
                                f1 = Math.abs( convBuf1[ j ]);
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                        }
                        framesWritten += len;
                        progOff		  += len;
                    // .... progress ....
                        setProgression( (float) progOff / (float) progLen );
                    }

                } else {						// -------- dem copy --------

                    while( (framesWritten < fadeLen) && threadRunning ) {
                        len = (int) Math.min( fadeLen - framesWritten, 8192 );
                        inF.seekFrame( fadeOutOff + framesWritten );
                        inF.readFrames( fadeBuf, 0, len );
                        inF.seekFrame( fadeInOff + framesWritten );
                        inF.readFrames( inBuf, 0, len );

                        if( eqP ) {
                            for( int j = 0; j < len; j++ ) {
                                d1 = (double) (j + framesWritten) / fadeLen;
                                f1 = (float) Math.sqrt( d1 ) * gain;
                                f2 = (float) Math.sqrt( 1.0 - d1 ) * gain;
                                for( int ch = 0; ch < inChans; ch++ ) {
                                    inBuf[ ch ][ j ] = inBuf[ ch ][ j ] * f1 + fadeBuf[ ch ][ j ] * f2;
                                }
                            }
                        } else {
                            for( int j = 0; j < len; j++ ) {
                                d1 = (double) (j + framesWritten) / fadeLen;
                                f1 = (float) d1 * gain;
                                f2 = (float) (1.0 - d1) * gain;
                                for( int ch = 0; ch < inChans; ch++ ) {
                                    inBuf[ ch ][ j ] = inBuf[ ch ][ j ] * f1 + fadeBuf[ ch ][ j ] * f2;
                                }
                            }
                        }
                        outF2.writeFrames( inBuf, 0, len );
                        for( int ch = 0; ch < inChans; ch++ ) {
                            convBuf1 = inBuf[ ch ];
                            for( int j = 0; j < len; j++ ) {
                                f1 = Math.abs( convBuf1[ j ]);
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                        }

                        framesWritten += len;
                        progOff		  += len;
                    // .... progress ....
                        setProgression( (float) progOff / (float) progLen );
                    }
                }
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- norm ----
            if( tempF != null ) {
                ref		= new Param( 1.0 / maxAmp, Param.ABS_AMP );
                gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ref, null )).value;
                normalizeAudioFile( tempF, outF, inBuf, gain, 1.0f );
                maxAmp	*= gain;
                deleteTempFile( tempF );
            }

            // done

            outF.close();
            outF	= null;

        // ---- Finish ----
//System.err.println( "maxAmp "+maxAmp );
            // inform about clipping/ low level
            handleClipping( maxAmp );

        }
        catch( IOException e1 ) {
            setError( e1 );
        }

    // ---- cleanup (topLevel) ----
        if( outF != null ) {
            outF.cleanUp();
        }
        if( inF != null ) {
            inF.cleanUp();
        }
    } // process()
}