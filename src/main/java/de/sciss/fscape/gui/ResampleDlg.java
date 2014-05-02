/*
 *  ResampleDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
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
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *	Processing module for bandlimited resampling using
 *	an algorithm described by J.O.Smith.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class ResampleDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_QUALITY				= 3;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_RATE				= 1;
	private static final int PR_RATEMODDEPTH		= 2;
	private static final int PR_RIGHTCHANMODDEPTH	= 3;
	private static final int PR_LENGTH				= 4;
	private static final int PR_RATEMOD				= 0;		// pr.bool
	private static final int PR_KEEPHEADER			= 1;
	private static final int PR_RIGHTCHAN			= 2;
	private static final int PR_USELENGTH			= 3;
	private static final int PR_INTERPOLE			= 4;
	private static final int PR_RATEMODENV			= 0;		// pr.envl
	private static final int PR_RIGHTCHANMODENV		= 1;

	private static final String PRN_INPUTFILE			= "InputFile";
	private static final String PRN_OUTPUTFILE			= "OutputFile";
	private static final String PRN_OUTPUTTYPE			= "OutputType";
	private static final String PRN_OUTPUTRES			= "OutputReso";
	private static final String PRN_QUALITY				= "Quality";
	private static final String PRN_RATE				= "Rate";
	private static final String PRN_RATEMODDEPTH		= "RateModDepth";
	private static final String PRN_RIGHTCHANMODDEPTH	= "RightChanModDepth";
	private static final String PRN_LENGTH				= "Length";
	private static final String PRN_RATEMOD				= "RateMod";
	private static final String PRN_KEEPHEADER			= "KeepHeader";
	private static final String PRN_RIGHTCHAN			= "RightChan";
	private static final String PRN_USELENGTH			= "UseLength";
	private static final String PRN_INTERPOLE			= "Interpole";
	private static final String PRN_RATEMODENV			= "RateModEnv";
	private static final String PRN_RIGHTCHANMODENV		= "RightChanModEnv";

	private static final int QUAL_MEDIUM		= 0;	// PR_QUALITY
	private static final int QUAL_GOOD			= 1;
	private static final int QUAL_VERYGOOD		= 2;

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, QUAL_GOOD };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_QUALITY };
	private static final Param	prPara[]		= { null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_RATE, PRN_RATEMODDEPTH, PRN_RIGHTCHANMODDEPTH, PRN_LENGTH };
	private static final boolean	prBool[]	= { false, true, false, false, false };
	private static final String	prBoolName[]	= { PRN_RATEMOD, PRN_KEEPHEADER, PRN_RIGHTCHAN, PRN_USELENGTH, PRN_INTERPOLE };
	private static final Envelope	prEnvl[]	= { null, null };
	private static final String	prEnvlName[]	= { PRN_RATEMODENV, PRN_RIGHTCHANMODENV };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_QUALITY				= GG_OFF_CHOICE		+ PR_QUALITY;
	private static final int GG_RATE				= GG_OFF_PARAMFIELD	+ PR_RATE;
	private static final int GG_RATEMODDEPTH		= GG_OFF_PARAMFIELD	+ PR_RATEMODDEPTH;
	private static final int GG_RIGHTCHANMODDEPTH	= GG_OFF_PARAMFIELD	+ PR_RIGHTCHANMODDEPTH;
	private static final int GG_LENGTH				= GG_OFF_PARAMFIELD	+ PR_LENGTH;
	private static final int GG_RATEMOD				= GG_OFF_CHECKBOX	+ PR_RATEMOD;
	private static final int GG_KEEPHEADER			= GG_OFF_CHECKBOX	+ PR_KEEPHEADER;
	private static final int GG_RIGHTCHAN			= GG_OFF_CHECKBOX	+ PR_RIGHTCHAN;
	private static final int GG_INTERPOLE			= GG_OFF_CHECKBOX	+ PR_INTERPOLE;
	private static final int GG_RATEMODENV			= GG_OFF_ENVICON	+ PR_RATEMODENV;
	private static final int GG_RIGHTCHANMODENV		= GG_OFF_ENVICON	+ PR_RIGHTCHANMODENV;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private AudioFileDescr	refInp	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ResampleDlg()
	{
		super( "Resample" );
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
			static_pr.para[ PR_LENGTH ]				= new Param(  0.0, Param.OFFSET_TIME );
			static_pr.para[ PR_RATE ]				= new Param(  0.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_RATEMODDEPTH ]		= new Param(  1.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_RIGHTCHANMODDEPTH ]	= new Param(  1.0, Param.OFFSET_SEMITONES );
			static_pr.paraName	= prParaName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_RATEMODENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envl[ PR_RIGHTCHANMODENV ]= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		JComboBox			ggQuality;
		ParamField			ggRate, ggRateModDepth, ggRightChanModDepth, ggLength;
		EnvIcon				ggRateModEnv, ggRightChanModEnv;
		JCheckBox			ggRateMod, ggRightChan, ggKeepHeader, ggInterpole;
		PathField[]			ggInputs;
		ParamSpace[]		spcRate, spcRateModDepth, spcLength;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ParamListener paramL = new ParamListener() {
			public void paramChanged( ParamEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_RATE:
					pr.bool[ PR_USELENGTH ] = false;
					rateLenInterference();
					break;
				
				case GG_LENGTH:
					pr.bool[ PR_USELENGTH ] = true;
					rateLenInterference();
					break;
				}
			}
		};

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_RATEMOD:
				case GG_RIGHTCHAN:
				case GG_KEEPHEADER:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};

		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE:
					setInput( ((PathField) e.getSource()).getPath().getPath() );
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
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Rsmp$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		
		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, paramL );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Sample Rate Conversion", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		
		spcRate				= new ParamSpace[ 4 ];
		spcRate[0]			= new ParamSpace(    689.1,   768000.0, 0.1,   Param.ABS_HZ );
		spcRate[1]			= Constants.spaces[ Constants.offsetSemitonesSpace ];
		spcRate[2]			= Constants.spaces[ Constants.offsetFreqSpace ];
		spcRate[3]			= new ParamSpace( -96000.0,    96000.0, 0.1,   Param.OFFSET_HZ );
		spcLength			= new ParamSpace[ 5 ];
		spcLength[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]		= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]		= Constants.spaces[ Constants.offsetMsSpace ];
		spcLength[3]		= Constants.spaces[ Constants.offsetBeatsSpace ];
		spcLength[4]		= Constants.spaces[ Constants.offsetTimeSpace ];
		spcRateModDepth		= new ParamSpace[ 3 ];
		spcRateModDepth[0]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
		spcRateModDepth[1]	= Constants.spaces[ Constants.offsetFreqSpace ];
		spcRateModDepth[2]	= spcRate[ 3 ];

		ggRate			= new ParamField( spcRate );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "New rate", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggRate, GG_RATE, paramL );
		ggRateMod		= new JCheckBox();
		con.weightx		= 0.0;
		gui.addCheckbox( ggRateMod, GG_RATEMOD, il );
		ggRateModDepth	= new ParamField( spcRateModDepth );
		ggRateModDepth.setReference( ggRate );
		con.weightx		= 0.5;
		con.gridwidth	= 2;
		gui.addParamField( ggRateModDepth, GG_RATEMODDEPTH, paramL );
		ggRateModEnv	= new EnvIcon( getComponent() );
		con.weightx		= 0.0;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addGadget( ggRateModEnv, GG_RATEMODENV );

		ggRightChan		= new JCheckBox();
		con.gridwidth	= 2;
		con.weightx		= 0.5;
		gui.addLabel( new JLabel( "Destinct right channel mod.", SwingConstants.RIGHT ));
		con.weightx		= 0.0;
		con.gridwidth	= 1;
		gui.addCheckbox( ggRightChan, GG_RIGHTCHAN, il );
		ggRightChanModDepth	= new ParamField( spcRateModDepth );
		ggRightChanModDepth.setReference( ggRate );
		con.weightx		= 0.5;
		con.gridwidth	= 2;
		gui.addParamField( ggRightChanModDepth, GG_RIGHTCHANMODDEPTH, paramL );
		ggRightChanModEnv= new EnvIcon( getComponent() );
		con.weightx		= 0.0;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addGadget( ggRightChanModEnv, GG_RIGHTCHANMODENV );

		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Desired length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLength, GG_LENGTH, paramL );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggKeepHeader	= new JCheckBox( "Keep old rate in header" );
		con.weightx		= 0.0;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel() );
		con.weightx		= 0.5;
		gui.addCheckbox( ggKeepHeader, GG_KEEPHEADER, il );
		ggQuality		= new JComboBox();
		ggQuality.addItem( "Short" );
		ggQuality.addItem( "Medium" );
		ggQuality.addItem( "Long" );
		con.weightx		= 0.1;
		con.gridwidth	= 2;
		gui.addLabel( new JLabel( "FIR length", SwingConstants.RIGHT ));
		con.weightx		= 0.2;
		gui.addChoice( ggQuality, GG_QUALITY, il );
		ggInterpole		= new JCheckBox( "Interpolate" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggInterpole, GG_INTERPOLE, il );

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
		int					i, j;
		int					len, chunkLength;
		long				progOff, progLen;
		boolean				finished;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		FloatFile			floatF[]		= null;
		File				tempFile[]		= null;
		int					inChanNum;
		float				inBuf[][]		= null;
		float				outBuf[][]		= null;
		float				convBuf1[];

		// Synthesize
		float				gain			= 1.0f;								// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		int					ch;

		// Smp Init
        long			    totalInSamples;						// reichen 32 bit?
		long				inLength;
		long    			framesRead, framesWritten;
		int					inBufSize, outBufSize;

		double				inPhase;
		double				rsmpFactor;
		double				minRsmpFactor;
		int					quality;
		double				dOff, inTransLen, inputIncr;
		int					outTransLen;
		int					overlapSize, inputStep;
		int					offStart, overlap;
		int					fltSmpPerCrossing, fltCrossings, fltLen;
		float				flt[], fltD[], filter[][];
		float				fltGain, fltRolloff, fltWin;
		double				fltIncr;
		
		Param				rateBase, rateDepth;
		boolean				rateMod, rightMod;
		double				inRate, outRate;

		// mod
		SpectStream			mStream			= null;
		double				mFrames			= 0.0;				// mStream.framesRead
		Modulator			mRateMod		= null;
		int					mStep;

		// right mod
		double				rmFrames[]		= null;
		double				rdOff[]			= null;
		double				rInPhase[]		= null;
		double				leftFactor, rightFactor;
		Modulator			mRightMod		= null;
		double				rInputIncr;
		float				floaty;
		Param				rightDepth;

		float				maxAmp			= 0.0f;

		PathField			ggOutput;

topLevel: try {

		// ---- open input, output; init ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= inStream.length;
// System.out.println("inLength = " + inLength);
			totalInSamples	= inLength * inChanNum;
			inRate			= inStream.rate;
			// this helps to prevent errors from empty files!
			if( totalInSamples < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// initialize various stuff
			rateBase 			= Param.transform( pr.para[ PR_RATE ], Param.ABS_HZ,
												   new Param( inStream.rate, Param.ABS_HZ ), null );
			rateDepth			= pr.para[ PR_RATEMODDEPTH ];
			rightDepth			= pr.para[ PR_RIGHTCHANMODDEPTH ];
			rateMod				= pr.bool[ PR_RATEMOD ];
			rightMod			= pr.bool[ PR_RIGHTCHAN ] && (inChanNum > 1) && rateMod;
			quality				= pr.intg[ PR_QUALITY ];

			fltSmpPerCrossing	= 4096;	// 2048 << quality;
			switch( quality ) {
			case QUAL_MEDIUM:
				fltRolloff		= 0.70f;
				fltWin			= 6.5f;
				fltCrossings	= 5;
				break;
			case QUAL_GOOD:
				fltRolloff		= 0.80f;
				fltWin			= 7.0f;
				fltCrossings	= 9;
				break;
			case QUAL_VERYGOOD:
				fltRolloff		= 0.86f;
				fltWin			= 7.5f;
				fltCrossings	= 15;
				break;
			default:
				throw new IllegalArgumentException( String.valueOf( quality ));
			}
//			if( !rateMod && ((rateBase.val / inRate) >= 0.99) ) fltRolloff = 0.95f;	// no aliasing
			
			fltLen				= (int) ((fltSmpPerCrossing * fltCrossings) / fltRolloff + 0.5f);
			flt					= new float[ fltLen ];
			fltD				= pr.bool[ PR_INTERPOLE ] ? new float[ fltLen ] : null;
			fltGain				= Filter.createAntiAliasFilter( flt, fltD, fltLen, fltSmpPerCrossing, fltRolloff, fltWin );
			filter				= new float[ 3 ][];
			filter[ 0 ]			= flt;
			filter[ 1 ]			= fltD;
			filter[ 2 ]			= new float[ 2 ];
			filter[ 2 ][ 0 ]	= fltSmpPerCrossing;
			filter[ 2 ][ 1 ]	= fltGain;
			inputStep			= 32768 / inChanNum;
			inPhase				= 0.0;

// Debug.view( flt, "filter" );
			progOff				= 0;
			progLen				= (long) inLength*2;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			if( !pr.bool[ PR_KEEPHEADER ]) {
				outStream.rate = (float) rateBase.val;
			}
			outRate		= outStream.rate;
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// calc minimum rsmp.factor; used for overlap-size
			minRsmpFactor		= rateBase.val;
			if( rateMod ) {
				minRsmpFactor	= Math.min( minRsmpFactor,
											(Param.transform( rateDepth, Param.ABS_HZ, rateBase, null )).val );
				minRsmpFactor	= Math.min( minRsmpFactor,
											(Param.transform( new Param( -rateDepth.val, rateDepth.unit ),
											Param.ABS_HZ, rateBase, null )).val );
				mStream			= new SpectStream();
				mStream.setChannels( inChanNum );
				mStream.setBands( 0.0f, (float) inStream.rate / 2, 1, SpectStream.MODE_LIN );
				mStream.setRate( (float) inStream.rate, 1 );										// !!
				mStream.setEstimatedLength( inLength );										// !!
				mStream.getDescr();
				mRateMod		= new Modulator( rateBase, rateDepth, pr.envl[ PR_RATEMODENV ], mStream );
				
				if( rightMod ) {
					rdOff		= new double[ inChanNum ];
					rInPhase	= new double[ inChanNum ];
					rmFrames	= new double[ inChanNum ];
					for( ch = 0; ch < inChanNum; ch++ ) {
						rInPhase[ ch ] = 0.0;
						rmFrames[ ch ] = 0.0;
					}
					mRightMod	= new Modulator( rateBase, rightDepth, pr.envl[ PR_RIGHTCHANMODENV ], mStream );
				}
			}

			minRsmpFactor /= inRate;
			if( minRsmpFactor < 1.0 ) {
				fltIncr			= fltSmpPerCrossing * minRsmpFactor;
			} else {
				fltIncr			= fltSmpPerCrossing;
			}
			overlapSize			= (int) (fltLen / fltIncr) + 1;
			inBufSize			= (overlapSize << 1) + inputStep;
			outBufSize			= inputStep + 1;		// +1 als ArrayBounds security fuer Resample

			inBuf				= new float[ inChanNum ][ inBufSize ];
			outBuf				= new float[ inChanNum ][ outBufSize ];

			// clear buffers
			for( ch = 0; ch < inChanNum; ch++ ) {
				convBuf1 = inBuf[ ch ];
				for( i = 0; i < convBuf1.length; i++ ) {
					convBuf1[ i ] = 0.0f;
				}
				convBuf1 = outBuf[ ch ];
				for( i = 0; i < convBuf1.length; i++ ) {
					convBuf1[ i ] = 0.0f;
				}
			}

			// normalization requires temp files
			if( (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY) || rightMod ) {
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
			}
			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== da resampling ====================----

			framesWritten	= 0L;
			// samplesWritten	= 0L;
			framesRead		= 0L;
			rsmpFactor		= rateBase.val / inRate;
			overlap			= overlapSize;
			offStart		= overlapSize;
			finished		= false;

//System.out.println( "overlap "+overlapSize+"; inputStep "+inputStep );
//System.out.println( "rsmp "+rsmpFactor+"; frames to go"+inLength );

			while( threadRunning && !finished ) {
				// ==================== read input chunk ====================
				len			= (int) Math.min( inLength - framesRead, inputStep + overlap );
				chunkLength	= offStart + len;
				inF.readFrames( inBuf, offStart, len );
				framesRead += len;
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero-padding last chunk
				if( chunkLength < inBufSize ) {
//System.out.println( "zero "+(inBufSize-chunkLength)+" frames" );
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = chunkLength; i < inBufSize; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
					chunkLength	= overlapSize + len + overlapSize - overlap;
					finished	= true;
				} else {
					chunkLength	= overlapSize + Math.min( inputStep, len );
				}

				if( overlap > 0 ) {		// first pass sucky overhead
					overlap		= 0;
					offStart	= overlapSize << 1;
				}

//System.out.println( "read "+len+" frames" );
//System.out.println( "dOff "+dOff+" chunkLength "+chunkLength );

				// ---- resample : "normal" ----------------------------------------------------------------------
				if( !rightMod ) {

					dOff = overlapSize + inPhase;

					while( dOff < chunkLength ) {
						inTransLen	= chunkLength - dOff;
						// modulated
						if( rateMod ) {
							rsmpFactor	= mRateMod.calc().val / inRate;
							mStep		= (int) (rsmpFactor * outRate / 50);	// to be smooth just step max. 20 ms per mod.
							outTransLen	= Math.min( mStep, (int) (inTransLen * rsmpFactor) );
							outTransLen	= Math.max( 1, Math.min( outTransLen, inputStep ));	// outBuf.length limit
							inputIncr	= outTransLen / rsmpFactor;
							mFrames	   += inputIncr;
							mStream.framesRead = (long) mFrames;	// dirty little boy, dirty little boy
		
						// ae-statikk
						} else {
							outTransLen	= Math.max( 1, Math.min( (int) (inTransLen * rsmpFactor), inputStep ));
							inputIncr	= outTransLen / rsmpFactor;
						}
						if( inputIncr <= 0 ) break;
//System.out.println( "inTrans "+inTransLen+"; outTrans "+outTransLen+"; inpIncr "+inputIncr );
		
						for( ch = 0; ch < inChanNum; ch++ ) {
							convBuf1 = outBuf[ ch ];
							Filter.resample( inBuf[ ch ], dOff, convBuf1, 0, outTransLen, rsmpFactor, filter );
		
							// measure max. gain
							for( j = 0; j < outTransLen; j++ ) {
								if( Math.abs( convBuf1[ j ]) > maxAmp ) {
									maxAmp = Math.abs( convBuf1[ j ]);
								}
							}
						}
					
						dOff += inputIncr;
//System.out.println( "dOff now "+dOff );

						// ---- write output or temp ----
						if( floatF != null ) {
//System.out.println( "write "+outTransLen+" to temp" );
							for( ch = 0; ch < inChanNum; ch++ ) {
								floatF[ ch ].writeFloats( outBuf[ ch ], 0, outTransLen );
							}
		
						} else {
		
//System.out.println( "write "+outTransLen+" to out" );
							// adjust gain
							for( ch = 0; ch < inChanNum; ch++ ) {
								convBuf1 = outBuf[ ch ];
								for( j = 0; j < outTransLen; j++ ) {
									convBuf1[ j ] *= gain;
								}
							}
							outF.writeFrames( outBuf, 0, outTransLen );
						}
						framesWritten += outTransLen;
					} // while resample

					inPhase	= dOff - chunkLength;		// offset next time
						
			// ---- resample : "split brain" ----------------------------------------------------------------------
				} else {
					for( ch = 0; ch < inChanNum; ch++ ) {
						rdOff[ ch ] = overlapSize + rInPhase[ ch ];
					}
					do {
						rInputIncr = 0.0;
						for( ch = 0; ch < inChanNum; ch++ ) {
							if( rdOff[ ch ] >= chunkLength ) continue;
							inTransLen		= chunkLength - rdOff[ ch ];
							mStream.framesRead = (long) rmFrames[ ch ];	// dirty little boy, dirty little boy
							leftFactor		= mRateMod.calc().val / inRate;
							rightFactor		= mRightMod.calc().val / inRate;
							floaty			= (float) ch / (float) (inChanNum - 1);		// gewichtung; (rein theoret. >2 chn. moeglich!!!!)
							rsmpFactor		= leftFactor * (1.0f - floaty) +
											  rightFactor * floaty;
							mStep			= (int) (rsmpFactor * outRate / 50);	// to be smooth just step max. 20 ms per mod.
							outTransLen		= Math.min( mStep, (int) (inTransLen * rsmpFactor) );
							outTransLen		= Math.max( 1, Math.min( outTransLen, inputStep ));	// outBuf.length limit
							inputIncr		= outTransLen / rsmpFactor;
							rmFrames[ ch ] += inputIncr;
							rInputIncr	   += inputIncr;
							
							if( inputIncr <= 0 ) continue;
		
							convBuf1		= outBuf[ ch ];
							Filter.resample( inBuf[ ch ], rdOff[ ch ], convBuf1, 0, outTransLen, rsmpFactor, filter );

							// measure max. gain
							for( j = 0; j < outTransLen; j++ ) {
								if( Math.abs( convBuf1[ j ]) > maxAmp ) {
									maxAmp = Math.abs( convBuf1[ j ]);
								}
							}

							rdOff[ ch ] += inputIncr;

							// ---- write to temp ----
							floatF[ ch ].writeFloats( outBuf[ ch ], 0, outTransLen );
							// samplesWritten += outTransLen;
						} // for channels
					} while( rInputIncr > 0.0 );

					for( ch = 0; ch < inChanNum; ch++ ) {
						rInPhase[ ch ]	= rdOff[ ch ] - chunkLength;		// offset next time
					}
				}
			// ---- resample : dun ----------------------------------------------------------------------
					
				// shift buffers
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch ], inputStep, inBuf[ ch ], 0, overlapSize << 1 );
				}

			// .... progress ....
				progOff += len;
				setProgression( (float) progOff / (float) progLen );		
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----
	
			inF.close();
			inF			= null;
			// inStream	= null;

			if( (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY) || rightMod ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
			}

			if( floatF != null ) {
				// right mod. split brain files do not have the
				// same length; paddem widd zeroz
				if( rightMod ) {
					framesWritten = 0;
					for( ch = 0; ch < inChanNum; ch++ ) {
						framesWritten = Math.max( framesWritten, floatF[ ch ].getSize());
					}
					convBuf1 = outBuf[ 0 ];
					for( i = 0; i < outBufSize; i++ ) {		// create empty space
						convBuf1[ i ] = 0.0f;
					}
					for( ch = 0; ch < inChanNum; ch++ ) {
						for( long ii = floatF[ ch ].getSize(); threadRunning && (ii < framesWritten); ) {
							len = (int) Math.min( framesWritten - ii, outBufSize );
							floatF[ ch ].writeFloats( convBuf1, 0, len );
							ii += len;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						}
					// .... check running ....
						if( !threadRunning ) break topLevel;
					}
				}
				
				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}

			outF.close();
			outF = null;

		// ---- Finish ----

			// inform about clipping/ low level
			maxAmp *= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			inBuf		= null;
			outBuf		= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
		}
		if( outF != null ) {
			outF.cleanUp();
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------

	/**
	 *	Neues Inputfile setzen
	 */
	protected void setInput( String fname )
	{
		AudioFile		f;
		AudioFileDescr	stream;

		ParamField		ggSlave;
		Param			ref;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

			refInp	= stream;
			ref		= new Param( AudioFileDescr.samplesToMillis( stream, stream.length ), Param.ABS_MS );
			ggSlave = (ParamField) gui.getItemObj( GG_LENGTH );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
			ref = new Param( stream.rate, Param.ABS_HZ );
			ggSlave = (ParamField) gui.getItemObj( GG_RATE );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
			rateLenInterference();
			
		} catch( IOException e1 ) {
			refInp	= null;
		}
	}
	
	protected void rateLenInterference()
	{
		ParamField	ggLength	= (ParamField) gui.getItemObj( GG_LENGTH );
		ParamField	ggRate		= (ParamField) gui.getItemObj( GG_RATE );
		JCheckBox	ggKeepHeader= (JCheckBox) gui.getItemObj( GG_KEEPHEADER );
		boolean		keepHeader;
		Param		ref1, ref2;
		Param		p1, pa1, p2, pa2;

		if( (ggLength == null) || (ggRate == null) || (ggKeepHeader == null) || (refInp == null) ) return;
		
		keepHeader	= ggKeepHeader.isSelected();
		ref1		= new Param( refInp.rate, Param.ABS_HZ );
		ref2		= new Param( AudioFileDescr.samplesToMillis( refInp, refInp.length ), Param.ABS_MS );
		p1			= ggRate.getParam();
		p2			= ggLength.getParam();
		pa1			= Param.transform( p1, Param.ABS_HZ, ref1, null );
		pa2			= Param.transform( p2, Param.ABS_MS, ref2, null );
		
		if( pr.bool[ PR_USELENGTH ]) {
			pa1 = new Param( (pa2.val / ref2.val) * ref1.val, Param.ABS_HZ );
			p1	= Param.transform( pa1, p1.unit, ref1, null );
			ggRate.setParam( p1 );
		
		} else {
			if( !keepHeader ) {
				pa2 = ref2;
			} else {
				pa2	= new Param( (pa1.val / ref1.val) * ref2.val, Param.ABS_MS );
			}
			p2	= Param.transform( pa2, p2.unit, ref2, null );
			ggLength.setParam( p2 );
		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_RATEMODDEPTH );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_RATEMOD ]);
		}
		c = gui.getItemObj( GG_RATEMODENV );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_RATEMOD ]);
		}
		c = gui.getItemObj( GG_RIGHTCHAN );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_RATEMOD ]);
		}
		c = gui.getItemObj( GG_RIGHTCHANMODDEPTH );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_RATEMOD ] && pr.bool[ PR_RIGHTCHAN ]);
		}
		c = gui.getItemObj( GG_RIGHTCHANMODENV );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_RATEMOD ] && pr.bool[ PR_RIGHTCHAN ]);
		}
		c = gui.getItemObj( GG_LENGTH );
		if( c != null ) {
			c.setEnabled( !pr.bool[ PR_RATEMOD ] && pr.bool[ PR_KEEPHEADER ]);
		}
	}
}
// class ResampleDlg
