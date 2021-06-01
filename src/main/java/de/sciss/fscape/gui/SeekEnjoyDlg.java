/*
 *  SeekEnjoyDlg.java
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
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Region;
import de.sciss.io.Span;

import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 *	Time fragment rearrangier using probably output
 *	from the serial killa.
 */
public class SeekEnjoyDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE		= 0;		// pr.text
    private static final int PR_CTRLFILE		= 1;
    private static final int PR_OUTPUTFILE		= 2;
    private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_OUTPUTRES		= 1;
    private static final int PR_FADETYPE		= 2;
    private static final int PR_GAINTYPE		= 3;
    private static final int PR_SORTING			= 4;
    private static final int PR_MINCHUNK		= 0;		// pr.para
    private static final int PR_CROSSFADE		= 1;
    private static final int PR_TOLERANCE		= 2;
    private static final int PR_GAIN			= 3;

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_CTRLFILE		= "CtrlFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_FADETYPE		= "FadeType";
    private static final String PRN_MINCHUNK		= "MinChunk";
    private static final String PRN_CROSSFADE		= "CrossFade";
    private static final String PRN_TOLERANCE		= "Tolerance";
    private static final String PRN_SORTING			= "Sorting";

    private static final String[]	TYPE_NAMES		= { "Equal Energy", "Equal Power" };
    private static final int		TYPE_ENERGY		= 0;
    private static final int		TYPE_POWER		= 1;

    private static final String[]	SORT_NAMES		= { "Ascending", "Descending" };
    private static final int		SORT_ASCEND		= 0;
    private static final int		SORT_DESCEND	= 1;

    private static final String	prText[]		= { "", "", "" };
    private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_CTRLFILE, PRN_OUTPUTFILE };
    private static final int		prIntg[]	= { 0, 0, TYPE_POWER, GAIN_UNITY, SORT_ASCEND };
    private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_FADETYPE, PRN_GAINTYPE, PRN_SORTING };
    private static final Param	prPara[]		= { null, null, null, null };
    private static final String	prParaName[]	= { PRN_MINCHUNK, PRN_CROSSFADE, PRN_TOLERANCE, PRN_GAIN };

    private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_CTRLFILE			= GG_OFF_PATHFIELD	+ PR_CTRLFILE;
    private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_FADETYPE			= GG_OFF_CHOICE		+ PR_FADETYPE;
    private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_SORTING				= GG_OFF_CHOICE		+ PR_SORTING;
    private static final int GG_MINCHUNK			= GG_OFF_PARAMFIELD	+ PR_MINCHUNK;
    private static final int GG_CROSSFADE			= GG_OFF_PARAMFIELD	+ PR_CROSSFADE;
    private static final int GG_TOLERANCE			= GG_OFF_PARAMFIELD	+ PR_TOLERANCE;
    private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_CURRENTINFO			= GG_OFF_OTHER		+ 0;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    private static final String	ERR_LENGTH			= "Control file cannot be\nlonger than input file!";

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public SeekEnjoyDlg()
    {
        super( "Seek + Enjoy" );
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
            static_pr.para[ PR_MINCHUNK ]	= new Param(   50.0, Param.ABS_MS );
            static_pr.para[ PR_CROSSFADE ]	= new Param(   50.0, Param.FACTOR_TIME );
            static_pr.para[ PR_TOLERANCE ]	= new Param(    3.0, Param.DECIBEL_AMP );
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

        PathField			ggInputFile, ggCtrlFile, ggOutputFile;
        PathField[]			ggInputs;
        ParamField			ggMinChunk, ggCrossFade, ggTolerance;
        ParamSpace[]		spcChunk, spcFade, spcTolerance;
        Component[]			ggGain;
        JTextField			ggCurrentInfo;
        JComboBox			ggFadeType, ggSorting;

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

        ggCtrlFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select control file" );
        ggCtrlFile.handleTypes( GenericFile.TYPES_SOUND );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Control file", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggCtrlFile, GG_CTRLFILE, null );

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD, "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 1 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggOutputFile.deriveFrom( ggInputs, "$D0$F0Enj$E" );
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
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;
    gui.addLabel( new GroupLabel( "Segmentation Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        spcChunk		= new ParamSpace[ 2 ];
        spcChunk[0]		= Constants.spaces[ Constants.absMsSpace ];
        spcChunk[1]		= Constants.spaces[ Constants.absBeatsSpace ];
        ggMinChunk		= new ParamField( spcChunk );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Min. Chunk Length", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggMinChunk, GG_MINCHUNK, null );

        spcFade			= new ParamSpace[ 3 ];
        spcFade[0]		= Constants.spaces[ Constants.absMsSpace ];
        spcFade[1]		= Constants.spaces[ Constants.absBeatsSpace ];
        spcFade[2]		= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		spcFade[2].max	= 50.0;
        spcFade[2]		= new ParamSpace( spcFade[2].min, 50.0, spcFade[2].inc, spcFade[2].unit );
        ggCrossFade		= new ParamField( spcFade );
        ggCrossFade.setReference( ggMinChunk );
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Crossfades", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggCrossFade, GG_CROSSFADE, null );

        spcTolerance	= new ParamSpace[ 2 ];
        spcTolerance[0]	= Constants.spaces[ Constants.offsetAmpSpace ];
        spcTolerance[1]	= Constants.spaces[ Constants.decibelAmpSpace ];
        ggTolerance		= new ParamField( spcTolerance );
        ggTolerance.setReference( new Param( 1.0, Param.ABS_AMP ));
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "Tolerance", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addParamField( ggTolerance, GG_TOLERANCE, null );

        ggFadeType		= new JComboBox();
        for( int i = 0; i < TYPE_NAMES.length; i++ ) {
            ggFadeType.addItem( TYPE_NAMES[ i ]);
        }
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Crossfade Type", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggFadeType, GG_FADETYPE, null );

        ggCurrentInfo	= new JTextField();
        ggCurrentInfo.setEditable( false );
        ggCurrentInfo.setBackground( null );
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel( new JLabel( "# of Regions", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        gui.addTextField( ggCurrentInfo, GG_CURRENTINFO, null );

        ggSorting		= new JComboBox();
        for( int i = 0; i < SORT_NAMES.length; i++ ) {
            ggSorting.addItem( SORT_NAMES[ i ]);
        }
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Sorting", SwingConstants.RIGHT ));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggSorting, GG_SORTING, null );

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
        int					i, j, k, m, chunkLength;
        int					off, prevOff, len, ch;
        long				progOff, progLen, oldProg;
        double				d1;
        float				f1, f2;

        // io
        AudioFile			inF		= null;
        AudioFile			ctrlF	= null;
        AudioFile			outF	= null;
        AudioFileDescr			inStream, ctrlStream, outStream;

        FloatFile[]			floatF			= null;
        File				tempFile[]		= null;

        int					inChanNum, ctrlChanNum;
        float[][]			inBuf, outBuf, ctrlBuf2;
        float[]				ctrlBuf, ctrlBufSort;
        int[]				ctrlIdx, ctrlIdxRvs, ctrlIdxFoo;
        float[]				convBuf1, convBuf2;

        int					inLength, ctrlLength;
        int					framesRead;
        int					minCtrl, fadeOut;
        float				topTol, botTol, ctrlRate, ctrl, ctrlBot, ctrlTop;

        Region				currentRegion, prevRegion, nextRegion;
        Vector<Region> regionList;
        Param				chunkLenParam	= new Param( 0.0, Param.ABS_MS );

        PathField			ggOutput;

        float				gain			= 1.0f;			// gain abs amp
        Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
        float				maxAmp			= 0.0f;

topLevel: try {

            gui.stringToJTextField( "", GG_CURRENTINFO );

        // ---- open input, output; init ----

            // input
            inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
            inStream		= inF.getDescr();
            inChanNum		= inStream.channels;
            inLength		= (int) inStream.length;
            // this helps to prevent errors from empty files!
            if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );

            ctrlF			= AudioFile.openAsRead( new File( pr.text[ PR_CTRLFILE ]));
            ctrlStream		= ctrlF.getDescr();
            ctrlChanNum		= ctrlStream.channels;
            ctrlLength		= (int) ctrlStream.length;
            // this helps to prevent errors from empty files!
            if( (ctrlLength < 1) || (ctrlChanNum < 1)  ) throw new EOFException( ERR_EMPTY );
            if( ctrlLength > inLength ) throw new IOException( ERR_LENGTH );

            ctrlRate		= (float) ctrlLength / (float) inLength;

        // ---- open output ----
            ggOutput		= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream		= new AudioFileDescr( inStream );
            ggOutput.fillStream( outStream );
            outF			= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            progOff			= 0;
            progLen			= ((long) inLength << 1) + ((long) ctrlLength << 1);

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
                progLen += inLength;
            } else {
                gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- completely read in control file ----
            ctrlBuf2		= new float[ ctrlChanNum ][ 8192 ];
            ctrlBuf			= new float[ ctrlLength ];
            ctrlBufSort		= new float[ ctrlLength ];
            ctrlIdx			= new int[ ctrlLength ];	// mapping ctrlBufSort -> ctrlBuf
            ctrlIdxRvs		= new int[ ctrlLength ];	// mapping ctrlBuf -> ctrlBufSort
            ctrlIdxFoo		= new int[ ctrlLength ];

            for( i = 0; i < ctrlLength; i++ ) ctrlIdx[ i ] = i;
            System.arraycopy( ctrlIdx, 0, ctrlIdxRvs, 0, ctrlLength );

            for( framesRead = 0; threadRunning && (framesRead < ctrlLength); ) {
                len				 = Math.min( 8192, ctrlLength - framesRead );
                ctrlF.readFrames( ctrlBuf2, 0, len );
                System.arraycopy( ctrlBuf2[0], 0, ctrlBuf, framesRead, len );
                framesRead		+= len;
                progOff			+= len;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            ctrlF.close();
            ctrlF = null;

        // ---- normalize control file ----
            ctrlTop = Float.NEGATIVE_INFINITY;
            ctrlBot = Float.POSITIVE_INFINITY;
            for( i = 0; i < ctrlLength; i++ ) {
                ctrl = ctrlBuf[ i ];
                if( ctrl < ctrlBot ) {
                    ctrlBot = ctrl;
                }
                if( ctrl > ctrlTop ) {
                    ctrlTop = ctrl;
                }
            }

            if( ctrlBot != ctrlTop ) {
                f1 = 1.0f / (ctrlTop - ctrlBot);
            } else {
                f1 = 1.0f;
            }
            for( i = 0; i < ctrlLength; i++ ) {
                ctrlBuf[ i ] = (ctrlBuf[ i ] - ctrlBot) * f1;
            }

        // ---- sort controlfile ----
// float[] test = new float[ ctrlLength ];
// for( i = 0; i < ctrlLength; i++ ) test[i]=ctrlIdx[i];
// Debug.view( ctrlBuf, "Ctrl" );
// Debug.view( test, "CtrlIdx" );

            System.arraycopy( ctrlBuf, 0, ctrlBufSort, 0, ctrlLength );
            Util.quickSortFloat( ctrlBufSort, ctrlIdx, ctrlLength );	// sorts in ascending order
            if( pr.intg[ PR_SORTING ] == SORT_DESCEND ) {
                for( i = 0, j = ctrlLength-1; i < j; i++, j-- ) {
                    ctrl				= ctrlBufSort[ i ];
                    ctrlBufSort[ i ]	= ctrlBufSort[ j];
                    ctrlBufSort[ j]		= ctrl;

                    k					= ctrlIdx[ i ];
                    ctrlIdx[ i ]		= ctrlIdx[ j ];
                    ctrlIdx[ j ]		= k;
                }
            }

// for( i = 1; i < ctrlLength; i++ ) {
// 	if( ctrlBufSort[ i-1 ] > ctrlBufSort[ i ]) { System.out.println( "Sort error" ); break; }
// }
// for( i = 0; i < ctrlLength; i++ ) {
// 	if( ctrlBufSort[ i ] != ctrlBuf[ ctrlIdx[ i ]]) { System.out.println( "Idx error" ); break; }
// }
// Debug.view( ctrlBufSort, "Ctrl sort" );
// for( i = 0; i < ctrlLength; i++ ) test[i]=ctrlIdx[i];
// Debug.view( test, "CtrlIdx sort" );

            System.arraycopy( ctrlIdx, 0, ctrlIdxFoo, 0, ctrlLength );
            Util.quickSortInt( ctrlIdxFoo, ctrlIdxRvs, ctrlLength );
            progOff			+= ctrlLength;
        // .... progress ....
            setProgression( (float) progOff / (float) progLen );


// for( i = 0; i < ctrlLength; i++ ) {
// 	if( ctrlIdxRvs[ ctrlIdx[ i ]] != i ) { System.out.println( "Reversal error1" ); break; }
// 	if( ctrlIdx[ ctrlIdxRvs[ i ]] != i ) { System.out.println( "Reversal error2" ); break; }
// }

        // ---- misc inits ----
            d1				= Param.transform( pr.para[ PR_MINCHUNK ], Param.ABS_MS, null, null ).value;
            d1				= AudioFileDescr.millisToSamples( inStream, d1 );
//			minChunk		= Math.max( 2, (int) (d1 + 0.5) );
            minCtrl			= Math.max( 1, (int) (Math.ceil( d1 * ctrlRate ) + 0.5) );
            d1				= Param.transform( pr.para[ PR_TOLERANCE ], Param.ABS_AMP, new Param( 1.0, Param.ABS_AMP ), null ).value;
            if( d1 > 1.0 ) {
                topTol		= (float) d1;
                botTol		= (float) (1.0 / d1);
            } else {
                botTol		= (float) d1;
                topTol		= (float) (1.0 / d1);
            }

// System.out.println( "inLength "+inLength+"; ctrlLength "+ctrlLength+"; ctrlRate "+ctrlRate+"; minChunk "+minChunk+"; minCtrl "+minCtrl+"; fade "+crossFade );

            regionList		= new Vector<Region>();		// Elemente = de.sciss.fscape.io.Region, chronologically added
            inBuf			= new float[ inChanNum ][ 8192 ];
            outBuf			= new float[ inChanNum ][ 8192 ];

        // ================ PASS 1: Region detection ================

            off = 0;

regionLp:	while( true ) {

                // find next valid offset
                while( (off < ctrlLength) && (ctrlIdx[ off ] < 0) ) off++;
                if( off == ctrlLength ) break regionLp;		// no more regions left

                i			= ctrlIdx[ off ];
                ctrl		= ctrlBufSort[ off ];
                ctrlTop		= ctrl * topTol;
                ctrlBot		= ctrl * botTol;

//				j			= Math.min( ctrlLength, i + minChunk );
//				while( (j < ctrlLength) && (ctrlBuf[ j ] <= ctrlTop) && (ctrlBuf[ j ] >= ctrlBot) ) j++;
//				i--;
//				while( (i > 0) && (ctrlBuf[ i ] <= ctrlTop) && (ctrlBuf[ i ] >= ctrlBot) ) i--;
//				i++;

                i			= Math.max( 0, i - (minCtrl >> 1) );
                j			= Math.min( ctrlLength, i + minCtrl );
                while( (j < ctrlLength) && (ctrlBuf[ j ] <= ctrlTop) && (ctrlBuf[ j ] >= ctrlBot) && (ctrlIdx[ ctrlIdxRvs[ j ]] >= 0) ) j++;
                i--;
                while( (i > 0) && (ctrlBuf[ i ] <= ctrlTop) && (ctrlBuf[ i ] >= ctrlBot) && (ctrlIdx[ ctrlIdxRvs[ i ]] >= 0) ) i--;
                i++;

                // clear out ctrlIdx's so they will be skipped in the next region search
                for( k = i; k < j; k++ ) {
                    ctrlIdx[ ctrlIdxRvs[ k ]] = -1;
                }

                // chunks -> samples
                i			= (int) (i / ctrlRate + 0.5f);
                j			= Math.max( i, Math.min( inLength, (int) (j / ctrlRate + 0.5f) ));

// System.out.println( "Region : "+i+" ... "+j );

                currentRegion		= new Region( new Span( i, j ), null );
                regionList.addElement( currentRegion );
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            gui.stringToJTextField( String.valueOf( regionList.size()), GG_CURRENTINFO );

        // ================ PASS 2: Region copying ================

            len = 0;
            for( i = 1; i < regionList.size(); i++ ) {
                currentRegion = regionList.elementAt( i );
                len          += currentRegion.span.getLength();
            }

            progLen			= (long) len * (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ? 3 : 2);
            d1				= getProgression();
            progOff			= (long) (progLen * d1 / (1.0 - d1));
            progLen		   += progOff;

            regionList.addElement( new Region( new Span( inLength, inLength ), null ));
            currentRegion		= new Region( new Span( 0, 0 ), null );
            nextRegion			= regionList.elementAt( 0 );
            off					= 0;
            fadeOut				= 0;

// System.out.println( "off "+ progOff + " ; len "+progLen +"; delta "+(progLen - progOff)+"; allregions "+len );

            for( i = 1; i < regionList.size(); i++ ) {

                oldProg			= progOff;

// System.out.println( i + " : "+fadeOut );
                prevRegion		= currentRegion;
                currentRegion	= nextRegion;
                nextRegion		= regionList.elementAt( i );
                prevOff			= off;
                off				= 0;

                chunkLength		= fadeOut;
                while( chunkLength > 0 ) {
                    len			= Math.min( 8192, chunkLength );
                    inF.seekFrame( prevOff + prevRegion.span.getStart() );
                    inF.readFrames( outBuf, 0, len );
                    inF.seekFrame( off + currentRegion.span.getStart() );
                    inF.readFrames( inBuf, 0, len );

                    switch( pr.intg[ PR_FADETYPE ]) {
                    case TYPE_ENERGY:
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = inBuf[ ch ];
                            convBuf2 = outBuf[ ch ];
                            for( j = 0, k = off; j < len; j++, k++ ) {
                                f1 = (float) k / (float) fadeOut;
                                convBuf2[ j ] = convBuf1[ j ] * f1 + convBuf2[ j ] * (1.0f - f1);	// X-Fade
                            }
                        }
                        break;

                    case TYPE_POWER:
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = inBuf[ ch ];
                            convBuf2 = outBuf[ ch ];
                            for( j = 0, k = off; j < len; j++, k++ ) {
                                f1 = (float) k / (float) fadeOut;
                                f2 = 1.0f - f1;
                                convBuf2[ j ] = convBuf1[ j ] * (1.0f - f2*f2) + convBuf2[ j ] * (1.0f - f1*f1);	// X-Fade
                            }
                        }
                        break;
                    }

                    if( floatF != null ) {
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( j = 0; j < len; j++ ) {		// measure max amp
                                f1 = Math.abs( convBuf1[ j ]);
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                            floatF[ ch ].writeFloats( convBuf1, 0, len );
                        }
                    } else {						// i.e. abs gain
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( j = 0; j < len; j++ ) {	// measure max amp + adjust gain
                                f1				= Math.abs( convBuf1[ j ]);
                                convBuf1[ j ]  *= gain;
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                        }
                        outF.writeFrames( outBuf, 0, len );
                    }
                    prevOff		+= len;
                    off			+= len;
                    chunkLength -= len;
                    progOff		+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                // .... check running ....
                    if( !threadRunning ) break topLevel;
                }

                j					= (int) currentRegion.span.getLength();
                k					= (int) nextRegion.span.getLength();
                m					= Math.min( j, k );
// System.out.println(" :::: "+j+" ; "+k+"; "+m +"; "+ (Math.min( k >> 1, j - off )) );
                chunkLenParam.value = AudioFileDescr.samplesToMillis( inStream, m );
                d1					= Param.transform( pr.para[ PR_CROSSFADE ], Param.ABS_MS, chunkLenParam, null ).value;
                fadeOut				= Math.min( Math.min( k >> 1, j - off ),
                                                (int) (AudioFileDescr.millisToSamples( inStream, d1 ) + 0.5) );
                chunkLength			= j - fadeOut;

                while( off < chunkLength ) {
                    len			= Math.min( 8192, chunkLength - off );
                    inF.readFrames( outBuf, 0, len );
                    if( floatF != null ) {
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( k = 0; k < len; k++ ) {		// measure max amp
                                f1 = Math.abs( convBuf1[ k ]);
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                            floatF[ ch ].writeFloats( convBuf1, 0, len );
                        }
                    } else {						// i.e. abs gain
                        for( ch = 0; ch < inChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( k = 0; k < len; k++ ) {	// measure max amp + adjust gain
                                f1				= Math.abs( convBuf1[ k ]);
                                convBuf1[ k ]  *= gain;
                                if( f1 > maxAmp ) {
                                    maxAmp = f1;
                                }
                            }
                        }
                        outF.writeFrames( outBuf, 0, len );
                    }
                    off			+= len;
                    progOff		+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                // .... check running ....
                    if( !threadRunning ) break topLevel;
                }

                progOff = oldProg + j + j;
            }

            inF.close();
            inF = null;

        // ---- normalize output ----

            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).value;
                normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
                for( ch = 0; ch < inChanNum; ch++ ) {
                    floatF[ ch ].cleanUp();
                    floatF[ ch ] = null;
                    tempFile[ ch ].delete();
                    tempFile[ ch ] = null;
                }
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            outF.close();
            outF = null;

            setProgression( 1.0f );

            // inform about clipping/ low level
            maxAmp		*= gain;
            handleClipping( maxAmp );
        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            ctrlStream	= null;
            outStream	= null;
            inBuf		= null;
            ctrlBuf		= null;
            ctrlBuf2	= null;
            ctrlIdx		= null;
            convBuf1	= null;
            convBuf2	= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( outF != null ) {
            outF.cleanUp();
        }
        if( inF != null ) {
            inF.cleanUp();
        }
        if( ctrlF != null ) {
            ctrlF.cleanUp();
        }
        if( floatF != null ) {
            for( ch = 0; ch < floatF.length; ch++ ) {
                if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
                if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
            }
        }
    } // process()
}