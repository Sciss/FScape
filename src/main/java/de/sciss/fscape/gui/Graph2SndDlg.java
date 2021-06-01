/*
 *  Graph2SndDlg.java
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

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.io.ImageFile;
import de.sciss.fscape.io.ImageStream;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 *	This module was supposed to translate a scanned
 *	Cage score / gesture drawing (Ryoanji) into continuous sound samples.
 *	The project was never completed and I'm not
 *	sure if this module works at all.
 */
public class Graph2SndDlg
        extends ModulePanel {

// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE		= 0;		// pr.text
    private static final int PR_OUTPUTFILE		= 1;
    private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
    private static final int PR_OUTPUTRES		= 1;
    private static final int PR_OUTPUTRATE		= 2;

    private static final String PRN_INPUTFILE	= "InputFile";
    private static final String PRN_OUTPUTFILE	= "OutputFile";
    private static final String PRN_OUTPUTTYPE	= "OutputType";
    private static final String PRN_OUTPUTRES	= "OutputRes";
    private static final String PRN_OUTPUTRATE	= "OutputRate";

    private static final String	prText[]		= { "", "" };
    private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
    private static final int		prIntg[]	= { 0, 0, 0 };
    private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE };

    private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

// -------- public methods --------

    /**
     *	!! setVisible() bleibt dem Aufrufer ueberlassen
     */
    public Graph2SndDlg()
    {
        super( "Graph->Sound Conversion" );
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

//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES, PR_OUTPUTRATE );
//			fillDefaultGain( static_pr.para, PR_GAIN );
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggOutputFile;
        PathField[]			ggInputs;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets( 1, 2, 1, 2 );

    // -------- Input-Gadgets --------
        con.fill		= GridBagConstraints.BOTH;
        con.gridwidth	= GridBagConstraints.REMAINDER;
    gui.addLabel( new GroupLabel( "File I/O", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                                         "Select input file" );
        ggInputFile.handleTypes( GenericFile.TYPES_IMAGE );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Input image", SwingConstants.RIGHT ));
        con.gridheight	= 2;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInputFile, GG_INPUTFILE, null );

        ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                                         PathField.TYPE_RESFIELD   + PathField.TYPE_RATEFIELD,
                                         "Select output file" );
        ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
        ggInputs		= new PathField[ 1 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggOutputFile.deriveFrom( ggInputs, "$D0$F0$E" );
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel( new JLabel( "Output sound", SwingConstants.RIGHT ));
        con.gridheight	= 2;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
        gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
        gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );

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
        int			h, i, j, k, ch;
        float		f1, f2, f3;
        long		progOff, progLen;

        ImageFile	imgF			= null;
        AudioFile	sndF			= null;

        ImageStream	imgStream		= null;
        AudioFileDescr	sndStream		= null;

        int			width, height, frameSize;
        int			bitsPerSmp		= 8;
        int			inChanNum, outChanNum;
        int			framesWritten;	// re sound

        PathField	ggOutput;

        byte[]		row				= null;						// raw image (RGB interleaved)
        float[][]	outBuf;

topLevel: try {
        // ---- open input ----

            imgF		= new ImageFile( pr.text[ PR_INPUTFILE ], GenericFile.MODE_INPUT );
            imgStream	= imgF.initReader();
            width		= imgStream.width;
            height		= imgStream.height;
            bitsPerSmp	= imgStream.bitsPerSmp;
            inChanNum	= imgStream.smpPerPixel;
            frameSize	= width * height;
            row			= imgF.allocRow();

        // ---- open output ----

            ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outChanNum	= 1;
            sndStream	= new AudioFileDescr();
            ggOutput.fillStream( sndStream );
            sndStream.channels	= outChanNum;
            sndF		= AudioFile.openAsWrite( sndStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ============================== da processing ==============================

            progOff			= 0;
            progLen			= (long) frameSize + (long) height;
//			framesRead		= 0;	// sound-frames!!
            framesWritten	= 0;

            outBuf			= new float[ outChanNum ][ height ];

        // ============================== read image ==============================

            for( i = 0; threadRunning && (i < height); i++ ) {
                imgF.readRow( row );
// HSB => RGB  XXX
                for( ch = 0; ch < 1; ch++ ) {	// XXX
                // ---------- deinterleave raw image ----------
                    f1 = 0.0f;
                    f3 = 0.0f;
                    if( bitsPerSmp == 8 ) {			// deinterleave, scale to -1.0 ... +1.0
                        for( j = 0, k = ch; j < width; j++, k += inChanNum ) {
                            f2  = 1.0f - ((float) ((int) row[ k ] & 0xFF) / 0xFF);
                            f1 += f2 * ((float) j / (float) (width-1));
                            f3 += f2;
                        }
                    } else {	// 16 bit
                        for( j = 0, k = ch; j < width; j++, k += inChanNum ) {
                            h				= k << 1;
                            f2				= 1.0f - (float) ((((int) row[ h++ ] & 0xFF) << 8) | ((int) row[ h ] & 0xFF)) / (float) 0xFFFF;
                            f1			   += f2 * ((float) j / (float) (width-1));
                            f3			   += f2;
                        }
                    }

                    if( f3 > 0.0f ) {
                        outBuf[ ch ][ i ] = f1 / f3;
                    } else {
                        outBuf[ ch ][ i ] = 0.0f;
                    }
                } // for channels

                progOff += width;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            } // for rows
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ============================== write sound ==============================

            sndF.writeFrames( outBuf, 0, height );
            progOff			+= height;
            framesWritten	+= height;
        // .... progress ....
            setProgression( (float) progOff / (float) progLen );
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            imgF.close();
            imgF	= null;
            sndF.close();
            sndF	= null;

        } // topLevel
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            outBuf		= null;
            row			= null;
            System.gc();

            setError( new Exception( ERR_MEMORY ));
        }

    // ---- cleanup (topLevel) ----
        if( imgF != null ) {
            imgF.cleanUp();
            imgF = null;
        }
        if( sndF != null ) {
            sndF.cleanUp();
            sndF = null;
        }
    }
}