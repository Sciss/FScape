/*
 *  AmpShaperDlg.java
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

package de.sciss.fscape.gui;

import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Envelope;
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
 *  Processing module for modification
 *	of a sound's amplitude envelope.
 */
public class AmpShaperDlg
        extends ModulePanel {
// -------- private variables --------

    // Properties (defaults)
    private static final int PR_INPUTFILE			= 0;		// pr.text
    private static final int PR_OUTPUTFILE			= 1;
    private static final int PR_ENVINFILE			= 2;
    private static final int PR_ENVOUTFILE			= 3;
    private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
    private static final int PR_OUTPUTRES			= 1;
    private static final int PR_ENVOUTTYPE			= 2;
    private static final int PR_ENVOUTRES			= 3;
    private static final int PR_GAINTYPE			= 4;
    private static final int PR_ENVGAINTYPE			= 5;
    private static final int PR_ENVSOURCE			= 6;
    private static final int PR_MODE				= 7;
    private static final int PR_GAIN				= 0;		// pr.para
    private static final int PR_ENVGAIN				= 1;
    private static final int PR_MAXCHANGE			= 2;
    private static final int PR_AVERAGE				= 3;
    private static final int PR_ENVOUTPUT			= 0;		// pr.bool
    private static final int PR_INVERT				= 1;
    private static final int PR_RIGHTCHAN			= 2;
    private static final int PR_ENV					= 0;		// pr.envl
    private static final int PR_RIGHTCHANENV		= 1;

    private static final int SRC_INPUT				= 0;
    private static final int SRC_SOUNDFILE			= 1;
    private static final int SRC_ENVFILE			= 2;
    private static final int SRC_ENV				= 3;

    private static final int MODE_SUPERPOSE			= 0;
    private static final int MODE_REPLACE			= 1;

    private static final String PRN_INPUTFILE		= "InputFile";
    private static final String PRN_OUTPUTFILE		= "OutputFile";
    private static final String PRN_ENVINFILE		= "EnvInFile";
    private static final String PRN_ENVOUTFILE		= "EnvOutFile";
    private static final String PRN_OUTPUTTYPE		= "OutputType";
    private static final String PRN_OUTPUTRES		= "OutputReso";
    private static final String PRN_ENVOUTTYPE		= "EnvOutType";
    private static final String PRN_ENVOUTRES		= "EnvOutReso";
    private static final String PRN_ENVSOURCE		= "EnvSource";
    private static final String PRN_MODE			= "Mode";
    private static final String PRN_ENVGAINTYPE		= "EnvGainType";
    private static final String PRN_ENVGAIN			= "EnvGain";
    private static final String PRN_MAXCHANGE		= "MaxChange";
    private static final String PRN_AVERAGE			= "Average";
    private static final String PRN_ENVOUTPUT		= "EnvOuput";
    private static final String PRN_INVERT			= "Invert";
    private static final String PRN_RIGHTCHAN		= "RightChan";
    private static final String PRN_ENV				= "Env";
    private static final String PRN_RIGHTCHANENV	= "RightEnv";

    private static final String	prText[]			= { "", "", "", "" };
    private static final String	prTextName[]		= {	PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_ENVINFILE, PRN_ENVOUTFILE };
    private static final int		prIntg[]		= { 0, 0, 0, 0, GAIN_UNITY, GAIN_UNITY, SRC_INPUT, MODE_SUPERPOSE };
    private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_ENVOUTTYPE, PRN_ENVOUTRES,
                                                        PRN_GAINTYPE, PRN_ENVGAINTYPE, PRN_ENVSOURCE, PRN_MODE };
    private static final boolean	prBool[]		= { false, true, false };
    private static final String	prBoolName[]		= { PRN_ENVOUTPUT, PRN_INVERT, PRN_RIGHTCHAN };
    private static final Param	prPara[]			= { null, null, null, null };
    private static final String	prParaName[]		= { PRN_GAIN, PRN_ENVGAIN, PRN_MAXCHANGE, PRN_AVERAGE };
    private static final Envelope	prEnvl[]		= { null, null };
    private static final String	prEnvlName[]		= { PRN_ENV, PRN_RIGHTCHANENV };

    private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
    private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
    private static final int GG_ENVINFILE			= GG_OFF_PATHFIELD	+ PR_ENVINFILE;
    private static final int GG_ENVOUTFILE			= GG_OFF_PATHFIELD	+ PR_ENVOUTFILE;
    private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
    private static final int GG_ENVOUTTYPE			= GG_OFF_CHOICE		+ PR_ENVOUTTYPE;
    private static final int GG_ENVOUTRES			= GG_OFF_CHOICE		+ PR_ENVOUTRES;
    private static final int GG_ENVSOURCE			= GG_OFF_CHOICE		+ PR_ENVSOURCE;
    private static final int GG_MODE				= GG_OFF_CHOICE		+ PR_MODE;
    private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
    private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
    private static final int GG_ENVGAINTYPE			= GG_OFF_CHOICE		+ PR_ENVGAINTYPE;
    private static final int GG_ENVGAIN				= GG_OFF_PARAMFIELD	+ PR_ENVGAIN;
    private static final int GG_MAXCHANGE			= GG_OFF_PARAMFIELD	+ PR_MAXCHANGE;
    private static final int GG_AVERAGE				= GG_OFF_PARAMFIELD	+ PR_AVERAGE;
    private static final int GG_ENVOUTPUT			= GG_OFF_CHECKBOX	+ PR_ENVOUTPUT;
    private static final int GG_INVERT				= GG_OFF_CHECKBOX	+ PR_INVERT;
    private static final int GG_RIGHTCHAN			= GG_OFF_CHECKBOX	+ PR_RIGHTCHAN;
    private static final int GG_ENV					= GG_OFF_ENVICON	+ PR_ENV;
    private static final int GG_RIGHTCHANENV		= GG_OFF_ENVICON	+ PR_RIGHTCHANENV;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

// -------- public mehods --------

    public AmpShaperDlg() {
        super("Amplitude Shaper");
        init2();
    }

    protected void buildGUI() {
        if (static_pr == null) {
            static_pr			= new PropertyArray();
            static_pr.text		= prText;
            static_pr.textName	= prTextName;
            static_pr.intg		= prIntg;
            static_pr.intgName	= prIntgName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.para		= prPara;
            static_pr.para[ PR_MAXCHANGE ]		= new Param(   96.0, Param.DECIBEL_AMP );
            static_pr.para[ PR_AVERAGE ]		= new Param( 1000.0, Param.ABS_MS );
            static_pr.paraName	= prParaName;
            static_pr.envl		= prEnvl;
            static_pr.envl[ PR_ENV ]			= Envelope.createBasicEnvelope(Envelope.BASIC_UNSIGNED_TIME);
            static_pr.envl[ PR_RIGHTCHANENV ]	= Envelope.createBasicEnvelope(Envelope.BASIC_UNSIGNED_TIME);
            static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

            fillDefaultAudioDescr(static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES);
            fillDefaultAudioDescr(static_pr.intg, PR_ENVOUTTYPE, PR_ENVOUTRES);
            fillDefaultGain(static_pr.para, PR_GAIN);
            fillDefaultGain(static_pr.para, PR_ENVGAIN);
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

        GridBagConstraints	con;

        PathField			ggInputFile, ggOutputFile, ggEnvInFile, ggEnvOutFile;
        PathField[]			ggInputs;
        JComboBox			ggEnvSource, ggMode;
        ParamField			ggMaxChange, ggAverage;
        JCheckBox			ggEnvOutput, ggInvert, ggRightChan;
        EnvIcon				ggEnv, ggRightChanEnv;
        Component[]			ggGain, ggEnvGain;
        ParamSpace[]		spcAverage;
        ParamSpace			spcMaxChange;

        gui				= new GUISupport();
        con				= gui.getGridBagConstraints();
        con.insets		= new Insets(1, 2, 1, 2);

        ItemListener	il = new ItemListener() {
            public void itemStateChanged( ItemEvent e )
            {
                int ID = gui.getItemID(e);

                switch (ID) {
                    case GG_ENVSOURCE:
                        pr.intg[ID - GG_OFF_CHOICE] = ((JComboBox) e.getSource()).getSelectedIndex();
                        reflectPropertyChanges();
                        break;
                    case GG_ENVOUTPUT:
                    case GG_RIGHTCHAN:
                        pr.bool[ID - GG_OFF_CHECKBOX] = ((JCheckBox) e.getSource()).isSelected();
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
        gui.addLabel( new JLabel( "Input file:", SwingConstants.RIGHT ));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggInputFile, GG_INPUTFILE, null );

        ggEnvInFile = new PathField(PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                "Select input envelope file");
        ggEnvInFile.handleTypes(GenericFile.TYPES_SOUND);
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Env input:", SwingConstants.RIGHT));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField(ggEnvInFile, GG_ENVINFILE, null);

        ggOutputFile = new PathField(PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                PathField.TYPE_RESFIELD, "Select output file");
        ggOutputFile.handleTypes(GenericFile.TYPES_SOUND);
        ggInputs		= new PathField[ 1 ];
        ggInputs[ 0 ]	= ggInputFile;
        ggOutputFile.deriveFrom(ggInputs, "$D0$F0Amp$E");
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Output file:", SwingConstants.RIGHT));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
        gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
        gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

        ggGain			= createGadgets(GGTYPE_GAIN);
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel(new JLabel("Gain:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        gui.addParamField((ParamField) ggGain[0], GG_GAIN, null);
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice((JComboBox) ggGain[1], GG_GAINTYPE, il);

    // -------- Env-Output-Gadgets --------
    gui.addLabel(new GroupLabel("Separate envelope output", GroupLabel.ORIENT_HORIZONTAL,
                GroupLabel.BRACE_NONE));

        ggEnvOutFile = new PathField(PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                PathField.TYPE_RESFIELD, "Select output envelope file");
        ggEnvOutFile.handleTypes(GenericFile.TYPES_SOUND);
        ggEnvOutFile.deriveFrom(ggInputs, "$D0$F0Env$E");
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        ggEnvOutput		= new JCheckBox( "Env output" );
        gui.addCheckbox( ggEnvOutput, GG_ENVOUTPUT, il );
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.9;
        gui.addPathField( ggEnvOutFile, GG_ENVOUTFILE, null );
        gui.registerGadget( ggEnvOutFile.getTypeGadget(), GG_ENVOUTTYPE );
        gui.registerGadget( ggEnvOutFile.getResGadget(), GG_ENVOUTRES );

    // cannot call createGadgets twice (BUG!) XXX
        ggEnvGain		= new Component[ 2 ];	// createGadgets( GGTYPE_GAIN );
        ggEnvGain[ 0 ]	= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
        JComboBox ch		= new JComboBox();
        ch.addItem( "normalized" );
        ch.addItem( "immediate" );
        ggEnvGain[ 1 ]	= ch;

        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addLabel(new JLabel("Gain:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        gui.addParamField( (ParamField) ggEnvGain[ 0 ], GG_ENVGAIN, null );
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( (JComboBox) ggEnvGain[ 1 ], GG_ENVGAINTYPE, il );

    // -------- Settings --------
    gui.addLabel( new GroupLabel( "Shaper Settings", GroupLabel.ORIENT_HORIZONTAL,
                                  GroupLabel.BRACE_NONE ));

        ggEnvSource		= new JComboBox();
        ggEnvSource.addItem("Input file");
        ggEnvSource.addItem("Sound file");
        ggEnvSource.addItem("Envelope file");
        ggEnvSource.addItem("Envelope");
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Source:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        gui.addChoice( ggEnvSource, GG_ENVSOURCE, il );

        ggInvert		= new JCheckBox();
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Inversion:", SwingConstants.RIGHT));
        con.gridwidth	= GridBagConstraints.REMAINDER;
        con.weightx		= 0.4;
        gui.addCheckbox( ggInvert, GG_INVERT, il );

        ggMode			= new JComboBox();
        ggMode.addItem("Superposition");
        ggMode.addItem("Replacement");
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Apply mode:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addChoice( ggMode, GG_MODE, il );

        ggEnv = new EnvIcon(getComponent());
        con.gridwidth	= 1;
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Envelope:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        gui.addGadget(ggEnv, GG_ENV);

        spcMaxChange	= new ParamSpace( Constants.spaces[ Constants.decibelAmpSpace ]);
//		spcMaxChange.min= spcMaxChange.inc;
        spcMaxChange	= new ParamSpace( spcMaxChange.inc, spcMaxChange.max, spcMaxChange.inc, spcMaxChange.unit );
        ggMaxChange		= new ParamField( spcMaxChange );
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Max boost:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggMaxChange, GG_MAXCHANGE, null );

        ggRightChan		= new JCheckBox( "Right chan." );
        ggRightChanEnv	= new EnvIcon(getComponent());
        con.weightx		= 0.1;
        con.gridwidth	= 1;
        gui.addCheckbox(ggRightChan, GG_RIGHTCHAN, il);
        con.weightx		= 0.4;
        gui.addGadget(ggRightChanEnv, GG_RIGHTCHANENV);

        spcAverage		= new ParamSpace[ 3 ];
        spcAverage[0]	= Constants.spaces[ Constants.absMsSpace ];
        spcAverage[1]	= Constants.spaces[ Constants.absBeatsSpace ];
        spcAverage[2]	= Constants.spaces[ Constants.ratioTimeSpace ];
        ggAverage		= new ParamField( spcAverage );
        con.weightx		= 0.1;
        gui.addLabel(new JLabel("Smoothing:", SwingConstants.RIGHT));
        con.weightx		= 0.4;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addParamField( ggAverage, GG_AVERAGE, null );

        initGUI(this, FLAGS_PRESETS | FLAGS_PROGBAR, gui);
    }

    public void fillGUI() {
        super.fillGUI();
        super.fillGUI(gui);
    }

    public void fillPropertyArray() {
        super.fillPropertyArray();
        super.fillPropertyArray(gui);
    }

// -------- Processor Interface --------

    protected void process() {
        int					i, j, ch, len, off, chunkLength;
        long				progOff, progLen;
        float				f1, f2;
        double				d1;
        boolean				extraAudioFile;

        // io
        AudioFile			inF				= null;
        AudioFile			outF			= null;
        AudioFile			envInF			= null;
        AudioFile			envOutF			= null;
        AudioFileDescr			inStream;
        AudioFileDescr			outStream;
        AudioFileDescr			envInStream;
        AudioFileDescr			envOutStream;
        FloatFile[]			outFloatF		= null;
        FloatFile[]			envFloatF		= null;
        File				outTempFile[]	= null;
        File				envTempFile[]	= null;
        int					inChanNum, outChanNum, envInChanNum, envOutChanNum, shapeChanNum;

        int[][]				shapeChan;
        int[][]				inChan;
        float[][]			shapeChanWeight;
        float[][]			inChanWeight;

        // buffers
        float[][]			inBuf			= null;		// Sound-In
        float[][]			outBuf			= null;		// Sound-Out
        float[][]			inEnvBuf		= null;		// Envelope of Input
        float[][]			shapeEnvBuf		= null;		// Envelope of Shaper
        float[][]			envInBuf		= null;		// Direct-In of Shaper-File
        float[]				convBuf1, convBuf2;

        int					inLength, outLength, envInLength, envOutLength;
        int					framesRead, framesWritten;		// re sound-files
        int					framesRead2, framesWritten2;	// re env-files

        Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
        Param				peakGain;
        float				gain			= 1.0f;								 		// gain abs amp
        float				envGain			= 1.0f;								 		// gain abs amp
        float				maxAmp			= 0.0f;
        float				envMaxAmp		= 0.0f;

        float				maxChange;
        int					average, avrOff;

        double[]			inEnergy, envInEnergy;

        PathField			ggOutput;

    topLevel:
        try {

        // ---- open input, output; init ----

            // input
            inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
            inStream		= inF.getDescr();
            inChanNum		= inStream.channels;
            inLength		= (int) inStream.length;
            // this helps to prevent errors from empty files!
            if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            envInLength		= 0;
            envInChanNum	= inChanNum;
            shapeChanNum	= 0;

            // shape input
            switch( pr.intg[ PR_ENVSOURCE ]) {
            case SRC_SOUNDFILE:
            case SRC_ENVFILE:
                envInF			= AudioFile.openAsRead( new File( pr.text[ PR_ENVINFILE ]));
                envInStream		= envInF.getDescr();
                envInChanNum	= envInStream.channels;
                shapeChanNum	= envInChanNum;
                envInLength		= (int) envInStream.length;
                // this helps to prevent errors from empty files!
                if( (envInLength < 1) || (envInChanNum < 1) ) throw new EOFException( ERR_EMPTY );

                i			= Math.min( inLength, envInLength );
                inLength	= i;
                envInLength	= i;
                break;

            case SRC_ENV:
                if( pr.bool[ PR_RIGHTCHAN ]) {
                    shapeChanNum	= 2;
                    envInChanNum	= Math.max( envInChanNum, shapeChanNum );	// ggf. mono => stereo
                } else {
                    shapeChanNum	= 1;
                }
                break;

            case SRC_INPUT:
                shapeChanNum	= inChanNum;
                break;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            outChanNum		= Math.max( inChanNum, envInChanNum );
            outLength		= inLength;

            shapeChan		= new int[ outChanNum ][ 2 ];
            shapeChanWeight	= new float[ outChanNum ][ 2 ];
            inChan			= new int[ outChanNum ][ 2 ];
            inChanWeight	= new float[ outChanNum ][ 2 ];
            extraAudioFile	= (envInF != null) && (pr.intg[ PR_ENVSOURCE ] == SRC_SOUNDFILE);	// not if SRC_ENVFILE!!!

            // calc weights
            for( ch = 0; ch < outChanNum; ch++ ) {
                if( shapeChanNum == 1 ) {
                    shapeChan[ ch ][ 0 ]		= 0;
                    shapeChan[ ch ][ 1 ]		= 0;
                    shapeChanWeight[ ch ][ 0 ]	= 1.0f;
                    shapeChanWeight[ ch ][ 1 ]	= 0.0f;
                } else {
                    f1							= ((float) ch / (float) (outChanNum - 1)) * (float) (shapeChanNum - 1);
                    shapeChan[ ch ][ 0 ]		= (int) f1;									// Math.max verhindert ArrayIndex-Fehler
                    shapeChan[ ch ][ 1 ]		= Math.min( (int) f1 + 1, shapeChanNum-1 );	// (Weight ist dabei eh Null)
                    f1						   %= 1.0f;
                    shapeChanWeight[ ch ][ 0 ]	= 1.0f - f1;
                    shapeChanWeight[ ch ][ 1 ]	= f1;
                }
                if( inChanNum == 1 ) {
                    inChan[ ch ][ 0 ]			= 0;
                    inChan[ ch ][ 1 ]			= 0;
                    inChanWeight[ ch ][ 0 ]		= 1.0f;
                    inChanWeight[ ch ][ 1 ]		= 0.0f;
                } else {
                    f1							= ((float) ch / (float) (outChanNum - 1)) * (float) (inChanNum - 1);
                    inChan[ ch ][ 0 ]			= (int) f1;
                    inChan[ ch ][ 1 ]			= Math.min( (int) f1 + 1, inChanNum-1 );
                    f1						   %= 1.0f;
                    inChanWeight[ ch ][ 0 ]		= 1.0f - f1;
                    inChanWeight[ ch ][ 1 ]		= f1;
                }
/*				
for( i = 0; i < 2; i++ ) {
    System.out.println( "shapeChan["+ch+"]["+i+"] = "+shapeChan[ch][i] );
    System.out.println( "shapeWeig["+ch+"]["+i+"] = "+shapeChanWeight[ch][i] );
    System.out.println( "inputChan["+ch+"]["+i+"] = "+inChan[ch][i] );
    System.out.println( "inputWeig["+ch+"]["+i+"] = "+inChanWeight[ch][i] );
}
*/
            }

            // output
            ggOutput			= (PathField) gui.getItemObj( GG_OUTPUTFILE );
            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
            outStream			= new AudioFileDescr( inStream );
            ggOutput.fillStream( outStream );
            outStream.channels	= outChanNum;
            outF				= AudioFile.openAsWrite( outStream );
        // .... check running ....
            if( !threadRunning ) break topLevel;

            envOutLength	= 0;
            envOutChanNum	= 0;

            // envelope output
            if( pr.bool[ PR_ENVOUTPUT ]) {
                ggOutput	= (PathField) gui.getItemObj( GG_ENVOUTFILE );
                if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
                envOutStream	= new AudioFileDescr( inStream );
                ggOutput.fillStream( envOutStream );
                envOutStream.file = new File( pr.text[ PR_ENVOUTFILE ]);
                envOutF			= AudioFile.openAsWrite( envOutStream );
                envOutLength	= inLength;
                envOutChanNum	= inChanNum;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // average buffer size
            d1			= Param.transform( pr.para[ PR_AVERAGE ], Param.ABS_MS,
                                           new Param( AudioFileDescr.samplesToMillis( inStream, inLength ), Param.ABS_MS ),
                                           null ).value;		// average in millis
            average		= ((int) (AudioFileDescr.millisToSamples( inStream, d1 ) + 0.5) & ~1) + 1;		// always odd
            avrOff		= (average >> 1) + 1;				// first element needed for subtraction (see calcEnv())

            progOff		= 0;
            progLen		= (long) Math.max( average - avrOff, inLength) +
                          (long) (extraAudioFile ? Math.max( average - avrOff, envInLength ) : envInLength) +
                          (long) outLength + (long) envOutLength;

            // normalization requires temp files
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                outTempFile	= new File[ outChanNum ];
                outFloatF	= new FloatFile[ outChanNum ];
                for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
                    outTempFile[ ch ]	= null;
                    outFloatF[ ch ]		= null;
                }
                for( ch = 0; ch < outChanNum; ch++ ) {
                    outTempFile[ ch ]	= IOUtil.createTempFile();
                    outFloatF[ ch ]		= new FloatFile( outTempFile[ ch ], GenericFile.MODE_OUTPUT );
                }
                progLen	   += (long) outLength;
            } else {
                gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).value;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // normalization requires temp files
            if( pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY ) {
                envTempFile	= new File[ envOutChanNum ];
                envFloatF	= new FloatFile[ envOutChanNum ];
                for( ch = 0; ch < envOutChanNum; ch++ ) {		// first zero them because an exception might be thrown
                    envTempFile[ ch ]	= null;
                    envFloatF[ ch ]		= null;
                }
                for( ch = 0; ch < envOutChanNum; ch++ ) {
                    envTempFile[ ch ]	= IOUtil.createTempFile();
                    envFloatF[ ch ]		= new FloatFile( envTempFile[ ch ], GenericFile.MODE_OUTPUT );
                }
                progLen	   += (long) envOutLength;
            } else {
                envGain		= (float) (Param.transform( pr.para[ PR_ENVGAIN ], Param.ABS_AMP, ampRef, null )).value;
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // ---- further inits ----
            maxChange	= (float) (Param.transform( pr.para[ PR_MAXCHANGE ], Param.ABS_AMP, ampRef, null )).value;

            inBuf		= new float[ inChanNum ][ 8192 + average ];
            Util.clear( inBuf );
            outBuf		= new float[ outChanNum ][ 8192 ];
            Util.clear( outBuf );
            if( extraAudioFile ) {
                envInBuf= new float[ envInChanNum ][ 8192 + average ];
                Util.clear( envInBuf );
            }
            inEnvBuf	= new float[ inChanNum ][ 8192 ];	// = envOutBuf
            Util.clear( inEnvBuf );
            shapeEnvBuf	= new float[ envInChanNum ][ 8192 ];
            Util.clear( shapeEnvBuf );

            inEnergy	= new double[ inChanNum ];
            for( ch = 0; ch < inChanNum; ch++ ) {
                inEnergy[ ch ] = 0.0;
            }
            envInEnergy	= new double[ envInChanNum ];
            for( ch = 0; ch < envInChanNum; ch++ ) {
                envInEnergy[ ch ] = 0.0;
            }

// System.out.println( "inLength "+inLength+"; envInLength "+envInLength+"; envOutLength "+envOutLength+"; outLength "+outLength );
// System.out.println( "average "+average+"; avrOff "+avrOff );

        // ----==================== buffer init ====================----

            framesRead	= 0;	// re inF
            framesRead2	= 0;	// re envInF

            // ---- init buffers ----
            for( off = avrOff; threadRunning && (off < average); ) {
                len = Math.min( inLength - framesRead, Math.min( 8192, average - off ));
                if( len == 0 ) break;

                inF.readFrames( inBuf, off, len );
                // calc initial energy per channel (see calcEnv())
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1	= inBuf[ ch ];
                    d1			= 0.0;
                    for( i = 0, j = off; i < len; i++ ) {
                        f1	= convBuf1[ j++ ];
                        d1 += f1*f1;
                    }
                    inEnergy[ ch ] += d1;
                }
                framesRead	+= len;
                off			+= len;
                progOff		+= len;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            }
            // zero padding bereits durch initialisierung mit Util.clear() passiert!

            if( extraAudioFile ) {
                for( off = avrOff; threadRunning && (off < average); ) {
                    len = Math.min( envInLength - framesRead2, Math.min( 8192, average - off ));
                    if( len == 0 ) break;

                    envInF.readFrames( envInBuf, off, len );
                    // calc initial energy per channel (see calcEnv())
                    for( ch = 0; ch < envInChanNum; ch++ ) {
                        convBuf1	= envInBuf[ ch ];
                        d1			= 0.0;
                        for( i = 0, j = off; i < len; i++ ) {
                            f1	 = convBuf1[ j++ ];
                            d1	+= f1*f1;
                        }
                        envInEnergy[ ch ] += d1;
                    }
                    framesRead2	+= len;
                    off			+= len;
                    progOff		+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
                // zero padding bereits durch initialisierung mit Util.clear() passiert!
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ----==================== the real stuff ====================----

            framesWritten	= 0;	// re OutF
            framesWritten2	= 0;	// re envOutF

            while( threadRunning && (framesWritten < outLength) ) {

                chunkLength = Math.min( 8192, outLength - framesWritten );
            // ---- read input chunk ----
                len			= Math.min( inLength - framesRead, chunkLength );
                inF.readFrames( inBuf, average, len );
                // zero padding
                for( ch = 0; ch < inChanNum; ch++ ) {
                    convBuf1 = inBuf[ ch ];
                    for( i = len, j = len + average; i < chunkLength; i++ ) {
                        convBuf1[ j++ ] = 0.0f;
                    }
                }
                framesRead	+= len;
                progOff		+= len;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ---- read input env chunk ----
                if( envInF != null ) {
                    len	= Math.min( envInLength - framesRead2, chunkLength );
                    if( extraAudioFile) {											// ........ needs averaging ........
                        envInF.readFrames( envInBuf, average, len );
                        // zero padding
                        for( ch = 0; ch < envInChanNum; ch++ ) {
                            convBuf1 = envInBuf[ ch ];
                            for( i = len, j = len + average; i < chunkLength; i++ ) {
                                convBuf1[ j++ ] = 0.0f;
                            }
                        }
                    } else {														// ........ is already env ........
                        envInF.readFrames( shapeEnvBuf, 0, len );
                        // zero padding
                        for( ch = 0; ch < envInChanNum; ch++ ) {
                            convBuf1 = shapeEnvBuf[ ch ];
                            for( i = len; i < chunkLength; i++ ) {
                                convBuf1[ i ] = 0.0f;
                            }
                        }
                    }
                    framesRead2	+= len;
                    progOff		+= len;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ---- calc input envelope ----
                for( ch = 0; ch < inChanNum; ch++ ) {
                    inEnergy[ ch ] = calcEnv( inBuf[ ch ], inEnvBuf[ ch ], average, chunkLength, inEnergy[ ch ]);
                }

            // ---- write output env file ----
                if( pr.bool[ PR_ENVOUTPUT ]) {
                    if( envFloatF != null ) {		// i.e. unity gain
                        for( ch = 0; ch < envOutChanNum; ch++ ) {
                            convBuf1 = inEnvBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {	// measure max amp
                                f1 = Math.abs( convBuf1[ i ]);
                                if( f1 > envMaxAmp ) {
                                    envMaxAmp = f1;
                                }
                            }
                            envFloatF[ ch ].writeFloats( convBuf1, 0, chunkLength );
                        }
                    } else {						// i.e. abs gain
                        for( ch = 0; ch < envOutChanNum; ch++ ) {
                            convBuf1 = inEnvBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {	// measure max amp + adjust gain
                                f1				= Math.abs( convBuf1[ i ]);
                                convBuf1[ i ]  *= envGain;
                                if( f1 > envMaxAmp ) {
                                    envMaxAmp = f1;
                                }
                            }
                        }
                        envOutF.writeFrames( inEnvBuf, 0, chunkLength );
                    }
                    framesWritten2	+= chunkLength;
                    progOff			+= chunkLength;
                // .... progress ....
                    setProgression( (float) progOff / (float) progLen );
                }
            // .... check running ....
                if( !threadRunning ) break topLevel;

            // ---- calc shape envelope ----
                switch( pr.intg[ PR_ENVSOURCE ]) {
                case SRC_INPUT:								// shape env = input env
                    for( ch = 0; ch < inChanNum; ch++ ) {
                        System.arraycopy( inEnvBuf[ ch ], 0, shapeEnvBuf[ ch ], 0, chunkLength );
                    }
                    break;
                case SRC_SOUNDFILE:							// calc shape env from envInBuf
                    for( ch = 0; ch < envInChanNum; ch++ ) {
                        envInEnergy[ ch ] = calcEnv( envInBuf[ ch ], shapeEnvBuf[ ch ], average, chunkLength, envInEnergy[ ch ]);
                    }
                    break;
                case SRC_ENVFILE:							// nothing to do, we have already loaded the env
                    break;									//    in the correct buffer
                case SRC_ENV:
                    throw new IOException( "Graphic env not yet supported" );
                }

            // ---- calc output ----
                // first generate output envelope
                switch( pr.intg[ PR_MODE ]) {
                case MODE_SUPERPOSE:
                    if( !pr.bool[ PR_INVERT ]) {			// multiply by shape
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {
                                f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
                                     shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
                                convBuf1[ i ] = Math.min( maxChange, f1 );
                            }
                        }

                    } else {								// divide by shape
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {
                                f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
                                     shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
                                if( f1 > 0.0f ) {
                                    convBuf1[ i ] = Math.min( maxChange, 1.0f / f1 );
                                } else {
                                    convBuf1[ i ] = maxChange;
                                }
                            }
                        }
                    }
                    break;

                case MODE_REPLACE:
                    if( !pr.bool[ PR_INVERT ]) {			// shape / input
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {
                                f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
                                     shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
                                f2 = inEnvBuf[ inChan[ ch ][ 0 ]][ i ] * inChanWeight[ ch ][ 0 ] +
                                     inEnvBuf[ inChan[ ch ][ 1 ]][ i ] * inChanWeight[ ch ][ 1 ];
                                if( f2 > 0.0f ) {
                                    convBuf1[ i ] = Math.min( maxChange, f1 / f2 );
                                } else {
                                    convBuf1[ i ] = 0.0f;	// input ist eh ueberall null, somit unveraenderlich
                                }
                            }
                        }

                    } else {								// 1 / (shape * input)
                        for( ch = 0; ch < outChanNum; ch++ ) {
                            convBuf1 = outBuf[ ch ];
                            for( i = 0; i < chunkLength; i++ ) {
                                f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
                                     shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
                                f1*= inEnvBuf[ inChan[ ch ][ 0 ]][ i ] * inChanWeight[ ch ][ 0 ] +
                                     inEnvBuf[ inChan[ ch ][ 1 ]][ i ] * inChanWeight[ ch ][ 1 ];
                                if( f1 > 0.0f ) {
                                    convBuf1[ i ] = Math.min( maxChange, 1.0f / f1 );
                                } else {
                                    convBuf1[ i ] = maxChange;
                                }
                            }
                        }
                    }
                    break;
                }
                // then multiply input bites
                if( inChanNum == outChanNum ) {		// no weighting - use faster routine
                    for( ch = 0; ch < outChanNum; ch++ ) {
                        convBuf1 = outBuf[ ch ];
                        convBuf2 = inBuf[ ch ];
                        for( i = 0, j = avrOff; i < chunkLength; i++, j++ ) {
                            convBuf1[ i ] *= convBuf2[ j ];
                        }
                    }
                } else {
                    for( ch = 0; ch < outChanNum; ch++ ) {
                        convBuf1 = outBuf[ ch ];
                        for( i = 0, j = avrOff; i < chunkLength; i++, j++ ) {
                            f1 = inBuf[ inChan[ ch ][ 0 ]][ j ] * inChanWeight[ ch ][ 0 ] +
                                 inBuf[ inChan[ ch ][ 1 ]][ j ] * inChanWeight[ ch ][ 1 ];
                            convBuf1[ i ] *= f1;
                        }
                    }
                }

            // ---- write output sound file ----
                if( outFloatF != null ) {		// i.e. unity gain
                    for( ch = 0; ch < outChanNum; ch++ ) {
                        convBuf1 = outBuf[ ch ];
                        for( i = 0; i < chunkLength; i++ ) {	// measure max amp
                            f1 = Math.abs( convBuf1[ i ]);
                            if( f1 > maxAmp ) {
                                maxAmp = f1;
                            }
                        }
                        outFloatF[ ch ].writeFloats( convBuf1, 0, chunkLength );
                    }
                } else {						// i.e. abs gain
                    for( ch = 0; ch < outChanNum; ch++ ) {
                        convBuf1 = outBuf[ ch ];
                        for( i = 0; i < chunkLength; i++ ) {	// measure max amp + adjust gain
                            f1				= Math.abs( convBuf1[ i ]);
                            convBuf1[ i ]  *= gain;
                            if( f1 > maxAmp ) {
                                maxAmp = f1;
                            }
                        }
                    }
                    outF.writeFrames( outBuf, 0, chunkLength );
                }
                framesWritten	+= chunkLength;
                progOff			+= chunkLength;
            // .... progress ....
                setProgression( (float) progOff / (float) progLen );

            // ---- shift buffers ----
                for( ch = 0; ch < inChanNum; ch++ ) {	// zero padding is performed after AudioFile.readFrames()!
                    System.arraycopy( inBuf[ ch ], chunkLength, inBuf[ ch ], 0, average );
                }
                if( extraAudioFile ) {
                    for( ch = 0; ch < envInChanNum; ch++ ) {	// zero padding is performed after AudioFile.readFrames()!
                        System.arraycopy( envInBuf[ ch ], chunkLength, envInBuf[ ch ], 0, average );
                    }
                }

            } // until framesWritten == outLength
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- normalize output ----

            // sound file
            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
                peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / peakGain.value, peakGain.unit ), null )).value;
                f1		 = 1.0f;
                if( (envOutF != null) && (pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY) ) {	// leave prog space
                    f1	 = (1.0f + getProgression()) / 2;
                }
                normalizeAudioFile( outFloatF, outF, outBuf, gain, f1 );
                for( ch = 0; ch < outChanNum; ch++ ) {
                    outFloatF[ ch ].cleanUp();
                    outFloatF[ ch ] = null;
                    outTempFile[ ch ].delete();
                    outTempFile[ ch ] = null;
                }
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

            // envelope file
            if( (envOutF != null) && (pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY) ) {
                peakGain = new Param( (double) envMaxAmp, Param.ABS_AMP );
                envGain	 = (float) (Param.transform( pr.para[ PR_ENVGAIN ], Param.ABS_AMP,
                                    new Param( 1.0 / peakGain.value, peakGain.unit ), null )).value;

                normalizeAudioFile( envFloatF, envOutF, inEnvBuf, envGain, 1.0f );
                for( ch = 0; ch < envOutChanNum; ch++ ) {
                    envFloatF[ ch ].cleanUp();
                    envFloatF[ ch ] = null;
                    envTempFile[ ch ].delete();
                    envTempFile[ ch ] = null;
                }
            }
        // .... check running ....
            if( !threadRunning ) break topLevel;

        // ---- Finish ----

            outF.close();
            outF		= null;
            outStream	= null;
            if( envOutF != null ) {
                envOutF.close();
                envOutF		= null;
                envOutStream= null;
            }
            if( envInF != null ) {
                envInF.close();
                envInF		= null;
                envInStream	= null;
            }
            inF.close();
            inF			= null;
            inStream	= null;
            outBuf		= null;
            inBuf		= null;
            inEnvBuf	= null;
            envInBuf	= null;
            shapeEnvBuf	= null;

            // inform about clipping/ low level
            maxAmp		*= gain;
            handleClipping( maxAmp );
            envMaxAmp	*= envGain;
//			handleClipping( envMaxAmp );	// ;( routine nicht flexibel genug!

        }
        catch( IOException e1 ) {
            setError( e1 );
        }
        catch( OutOfMemoryError e2 ) {
            inStream	= null;
            outStream	= null;
            envInStream	= null;
            envOutStream= null;
            inBuf		= null;
            outBuf		= null;
            inEnvBuf	= null;
            envInBuf	= null;
            shapeEnvBuf	= null;
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
        if( outF != null ) {
            outF.cleanUp();
            outF = null;
        }
        if( envInF != null ) {
            envInF.cleanUp();
            envInF = null;
        }
        if( envOutF != null ) {
            envOutF.cleanUp();
            envOutF = null;
        }
        if( outFloatF != null ) {
            for( ch = 0; ch < outFloatF.length; ch++ ) {
                if( outFloatF[ ch ] != null ) outFloatF[ ch ].cleanUp();
                if( outTempFile[ ch ] != null ) outTempFile[ ch ].delete();
            }
        }
        if( envFloatF != null ) {
            for( ch = 0; ch < envFloatF.length; ch++ ) {
                if( envFloatF[ ch ] != null ) envFloatF[ ch ].cleanUp();
                if( envTempFile[ ch ] != null ) envTempFile[ ch ].delete();
            }
        }
    } // process()

// -------- private methods --------

    /*
     *	@param	a			Quell-Wellenform
     *	@param	env			Ziel-RMS
     *	@param	average		Laenge der Samples in a, aus denen jeweils ein RMS berechnet wird
     *						(RMS = sqrt( energy/average ))
     *	@param	length		Zahl der generierten RMS (in env)
     *	@param	lastEnergy	Rueckgabewert aus dem letzten Aufruf dieser Routine
     *						(richtige Initialisierung siehe process(): summe der quadrate der prebuffered samples)
     *	@return				neuer Energiewert, der beim naechsten Aufruf als lastEnergy uebergeben werden muss
     */
    protected double calcEnv( float[] a, float[] env, int average, int length, double lastEnergy )
    {
        for( int i = 0, j = average; i < length; i++, j++ ) {			//   zu alten leistungswert "vergessen" und
            lastEnergy	= lastEnergy - a[ i ]*a[ i ] + a[ j ]*a[ j ];	// neuen addieren
            env[ i ]	= (float) Math.sqrt( lastEnergy / average );
        }
        return lastEnergy;
    }

    protected void reflectPropertyChanges() {
        super.reflectPropertyChanges();

        Component c;

        c = gui.getItemObj( GG_ENVINFILE );
        if( c != null ) {
            c.setEnabled( (pr.intg[ PR_ENVSOURCE ] == SRC_ENVFILE) ||
                          (pr.intg[ PR_ENVSOURCE ] == SRC_SOUNDFILE) );
        }
        c = gui.getItemObj( GG_ENV );
        if( c != null ) {
            c.setEnabled( pr.intg[ PR_ENVSOURCE ] == SRC_ENV );
        }
        c = gui.getItemObj( GG_RIGHTCHAN );
        if( c != null ) {
            c.setEnabled( pr.intg[ PR_ENVSOURCE ] == SRC_ENV );
        }
        c = gui.getItemObj( GG_RIGHTCHANENV );
        if( c != null ) {
            c.setEnabled( (pr.intg[ PR_ENVSOURCE ] == SRC_ENV) && pr.bool[ PR_RIGHTCHAN ]);
        }
//		c = gui.getItemObj( GG_AVERAGE );
//		if( c != null ) {
//			c.setEnabled( (pr.intg[ PR_ENVSOURCE ] == SRC_INPUT) ||
//						  (pr.intg[ PR_ENVSOURCE ] == SRC_SOUNDFILE) );
//		}
        c = gui.getItemObj( GG_ENVOUTFILE );
        if( c != null ) {
            c.setEnabled( pr.bool[ PR_ENVOUTPUT ]);
        }
    }
}