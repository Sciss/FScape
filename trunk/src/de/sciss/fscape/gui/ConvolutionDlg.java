/*
 *  ConvolutionDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		11-Mar-05	added minimum phase option
 *		28-Aug-06	provisorischer fix fuer min phase delay spike
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;
import de.sciss.io.Region;
import de.sciss.io.Span;

/**
 *  Processing module for sound file convolution.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 12-May-08
 */
public class ConvolutionDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_IMPULSEFILE			= 1;
	private static final int PR_OUTPUTFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_MODE				= 3;
	private static final int PR_MORPHPOLICY			= 4;
	private static final int PR_LENGTH				= 5;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_FADELENGTH			= 1;
	private static final int PR_IRNUMBER			= 2;
	private static final int PR_WINSTEP				= 3;
	private static final int PR_WINOVERLAP			= 4;
	private static final int PR_NORMIMPPOWER		= 0;		// pr.bool
	private static final int PR_TRUNCOVER			= 1;
	private static final int PR_MORPH				= 2;
	private static final int PR_MINPHASE			= 3;
	private static final int PR_IRMODENV			= 0;		// pr.envl

	private static final int MODE_CONV				= 0;
	private static final int MODE_DECONV			= 1;
	private static final int MODE_CONVINV			= 2;

	private static final int MORPH_RECT				= 0;
	private static final int MORPH_POLAR			= 1;
//	private static final int MORPH_SHIFT			= 2;

	private static final int LENGTH_FULL			= 0;
	private static final int LENGTH_INPUT			= 1;
	private static final int LENGTH_SKIPSUPPORT		= 2;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_IMPULSEFILE		= "ImpulseFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputRes";
	private static final String PRN_MODE			= "Mode";
	private static final String PRN_MORPHPOLICY		= "Policy";
	private static final String PRN_LENGTH			= "Length";
	private static final String PRN_FADELENGTH		= "FadeLen";
	private static final String PRN_IRNUMBER		= "IRNumber";
	private static final String PRN_WINSTEP			= "WinStep";
	private static final String PRN_WINOVERLAP		= "WinOverlap";
	private static final String PRN_NORMIMPPOWER	= "NormImp";
	private static final String PRN_TRUNCOVER		= "TruncOver";
	private static final String PRN_MORPH			= "Morph";
	private static final String PRN_IRMODENV		= "IRModEnv";
	private static final String PRN_MINPHASE		= "MinPhase";

	private static final String		prText[]		= { "", "", "" };
	private static final String		prTextName[]	= { PRN_INPUTFILE, PRN_IMPULSEFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]		= { 0, 0, GAIN_UNITY, MODE_CONV, MORPH_RECT, LENGTH_FULL };
	private static final String		prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_MODE,
														PRN_MORPHPOLICY, PRN_LENGTH };
	private static final Param		prPara[]		= { null, null, null, null, null };
	private static final String		prParaName[]	= { PRN_GAIN, PRN_FADELENGTH, PRN_IRNUMBER, PRN_WINSTEP, PRN_WINOVERLAP };
	private static final boolean	prBool[]		= { false, false, false, false };
	private static final String		prBoolName[]	= { PRN_NORMIMPPOWER, PRN_TRUNCOVER, PRN_MORPH, PRN_MINPHASE };
	private static final Envelope	prEnvl[]		= { null };
	private static final String		prEnvlName[]	= { PRN_IRMODENV };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_IMPULSEFILE			= GG_OFF_PATHFIELD	+ PR_IMPULSEFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_MODE				= GG_OFF_CHOICE		+ PR_MODE;
	private static final int GG_MORPHPOLICY			= GG_OFF_CHOICE		+ PR_MORPHPOLICY;
	private static final int GG_LENGTH				= GG_OFF_CHOICE		+ PR_LENGTH;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_FADELENGTH			= GG_OFF_PARAMFIELD	+ PR_FADELENGTH;
	private static final int GG_IRNUMBER			= GG_OFF_PARAMFIELD	+ PR_IRNUMBER;
	private static final int GG_WINSTEP				= GG_OFF_PARAMFIELD	+ PR_WINSTEP;
	private static final int GG_WINOVERLAP			= GG_OFF_PARAMFIELD	+ PR_WINOVERLAP;
	private static final int GG_NORMIMPPOWER		= GG_OFF_CHECKBOX	+ PR_NORMIMPPOWER;
	private static final int GG_TRUNCOVER			= GG_OFF_CHECKBOX	+ PR_TRUNCOVER;
	private static final int GG_MORPH				= GG_OFF_CHECKBOX	+ PR_MORPH;
	private static final int GG_MINPHASE			= GG_OFF_CHECKBOX	+ PR_MINPHASE;
	private static final int GG_IRMODENV			= GG_OFF_ENVICON	+ PR_IRMODENV;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	MARK_SUPPORT		= FIRDesignerDlg.MARK_SUPPORT;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ConvolutionDlg()
	{
		super( "Convolution" );
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
			static_pr.para[ PR_FADELENGTH ]	= new Param( 10.0, Param.ABS_MS );
			static_pr.para[ PR_IRNUMBER ]	= new Param(  1.0, Param.NONE );
			static_pr.para[ PR_WINSTEP ]	= new Param( 20.0, Param.ABS_MS );
			static_pr.para[ PR_WINOVERLAP ]	= new Param(  0.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_IRMODENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_UNSIGNED_TIME );
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
		
		PathField			ggInputFile, ggImpulseFile, ggOutputFile;
		JComboBox			ggMode, ggMorphPolicy, ggLength;
		ParamField			ggFadeLength, ggIRNumber, ggWinStep, ggWinOverlap;
		JCheckBox			ggNormImpPower, ggTruncOver, ggMorph, ggMinPhase;
		EnvIcon				ggIRModEnv;
		PathField[]			ggInputs;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_TRUNCOVER:
				case GG_MORPH:
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

		ggInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
									 "Select input sound" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridheight	= 2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggImpulseFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select impulse response" );
		ggImpulseFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridheight	= 1;
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Impulse response", SwingConstants.RIGHT ));
		con.gridheight	= 2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImpulseFile, GG_IMPULSEFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 2 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggInputs[ 1 ]	= ggImpulseFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$B0Con$B1$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridheight	= 2;
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

	// -------- Convolution-Parameter --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridheight	= 1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Convolution Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggMode			= new JComboBox();
		ggMode.addItem( "Convolution" );
		ggMode.addItem( "Deconvolution" );
		ggMode.addItem( "Conv w/ inversion" );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mode", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggMode, GG_MODE, il );

		ggTruncOver		= new JCheckBox( "Truncate overlaps" );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggTruncOver, GG_TRUNCOVER, il );

		ggNormImpPower	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Norm. IR energy", SwingConstants.RIGHT ));
		gui.addCheckbox( ggNormImpPower, GG_NORMIMPPOWER, il );

		ggFadeLength	= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Trunc fadeout", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggFadeLength, GG_FADELENGTH, null );

		ggMinPhase		= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Make minimum phase", SwingConstants.RIGHT ));
		gui.addCheckbox( ggMinPhase, GG_MINPHASE, il );

		ggLength		= new JComboBox();
		ggLength.addItem( "Input + IR" );
		ggLength.addItem( "Input (no change)" );
		ggLength.addItem( "Inp + IR\\{support}" );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "File length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggLength, GG_LENGTH, il );

	// -------- Morphing-Parameter --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridheight	= 1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Morphing", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggMorph			= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Morph IRs", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addCheckbox( ggMorph, GG_MORPH, il );

		ggMorphPolicy	= new JComboBox();
		ggMorphPolicy.addItem( "Rect X-Fade" );
		ggMorphPolicy.addItem( "Polar X-Fade" );
		ggMorphPolicy.addItem( "Correlate + shift" );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Policy", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggMorphPolicy, GG_MORPHPOLICY, il );

		ggIRNumber		= new ParamField( new ParamSpace( 1.0, 100000.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "# of IRs in file", SwingConstants.RIGHT ));
		con.weightx		= 0.8;
		gui.addParamField( ggIRNumber, GG_IRNUMBER, null );
		ggIRModEnv		= new EnvIcon( getWindow() );
		con.weightx		= 0.0;
		gui.addGadget( ggIRModEnv, GG_IRMODENV );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggWinStep		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Window size", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggWinStep, GG_WINSTEP, null );

		ggWinOverlap	= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Overlap", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggWinOverlap, GG_WINOVERLAP, null );

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
		long		progOff, progLen;
		float		maxAmp			= 0.0f;
	
		AudioFile	inF				= null;
		AudioFile	impF			= null;
		AudioFile	outF			= null;
		FloatFile	floatF[]		= null;
		File		tempFile[]		= null;
		
		AudioFileDescr	inStream;
		AudioFileDescr	impStream;
		AudioFileDescr	outStream;

		int			inChanNum, impChanNum, outChanNum;
		int			inLength, impLength, outLength;
		int			fftLength, frameSize, overlap;
		int			off, chunkLength;

		int			framesRead, framesWritten, impFramesRead;
		int			totalInSamples, totalImpSamples, totalOutSamples;
		
		float		inBuf[][]		= null;
		float		impBuf[][]		= null;
		float		overBuf[][];
		float		convBuf1[], convBuf2[], convBuf5[], convBuf3[][], convBuf4[][];
		float		fadeWin[]		= null;

		float		f1, f2;
		double		d1, d2, d3;
		double		impPower;						// impulse total power
		boolean		fftPolar		= false;		// Berechnungen sparen bei MORPH_POLAR

		Param		ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		Param		peakGain;													// (abs amp)
		float		gain			= 1.0f;								 		// gain abs amp

		// morphing
		int			mNumImp			= 1;		// number of IRs in imp-file
		Modulator	mImpMod			= null;
		Param		mImpBase;
		Param		mImpDepth		= null;
		Param		mImpIndex;
		Param		foo;
		SpectStream	mStream			= null;				// sucky Modulator-class is SpectStream dependant!
		int			oldImpIndex[]	= new int[ 2 ];		// 0...mNumImp-1
		int			newImpIndex[]	= new int[ 2 ];		// 0...mNumImp-1
		int			mImpReads		= 0;				// how many times do we read the impulse file
		int			mMorphs			= 0;				// how many times do we morph the mBufs
		boolean		needsMorph		= false;
		float		mBuf[][][]		= new float[2][][];	// [2][chan][fft]
		
		// overlap
		float		inOverBuf[][]	= null;		// der Teil vom Input der ein zweites Mal benutzt wird
		float		outOverBuf[][]	= null;		// der Teil vom Output der ueberblendet wird
//		float		fadeWin2[]		= null;		// Ueberblendfenster
		int			inOutOverlap	= 0;		// Laenge von in/outOverBuf
		int			inOverlap		= 0;
		int			outOverlap		= 0;
		
		// interp
		float[]		xa				= new float[ 4 ];
		float[]		ya				= new float[ 4 ];
		
		PathField	ggOutput;
		Marker		mark;
		Region		region;
		java.util.List	markers;
		java.util.List	regions;
		int			support			= 0;
		
		boolean		minPhase		= pr.bool[ PR_MINPHASE ];
		int			complexFFTsize, virtualImpLen, swap, len, startIdx, stopIdx, markerIdx;
		float		maxImpAmp;
		float[]		win;
		
topLevel: try {
		// ---- open files ----

			inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream	= inF.getDescr();
			inChanNum	= inStream.channels;
			inLength	= (int) inStream.length;
			totalInSamples= inLength * inChanNum;
			// this helps to prevent errors from empty files!
			if( totalInSamples <= 0 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			impF			= AudioFile.openAsRead( new File( pr.text[ PR_IMPULSEFILE ]));
			impStream		= impF.getDescr();
			impChanNum		= impStream.channels;
			impLength		= (int) impStream.length;
			virtualImpLen	= impLength * (minPhase ? 2 : 1);
			totalImpSamples	= impLength * impChanNum;
			// this helps to prevent errors from empty files!
			if( totalImpSamples <= 0 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			IOUtil.createEmptyFile( new File( pr.text[ PR_OUTPUTFILE ]));

			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outChanNum	= Math.max( inChanNum, impChanNum );
			outStream.channels = outChanNum;

		// ---- preparations, load impulse, transform it ----

			mImpBase 			= new Param( 0.0, Param.NONE );
			mImpIndex			= mImpBase;				// (zunaechst) unmoduliert
			oldImpIndex[ 0 ]	= -1;
			oldImpIndex[ 1 ]	= -1;
			newImpIndex[ 0 ]	= 0;
			newImpIndex[ 1 ]	= 0;

			// .... morph ....
			if( pr.bool[ PR_MORPH ]) {
				mNumImp			= Math.max( 1, Math.min( impLength, (int) pr.para[ PR_IRNUMBER ].val ));
				impLength  	   /= mNumImp;					// re-adjust
				virtualImpLen	= impLength * (minPhase ? 2 : 1);
				totalImpSamples	= impLength * impChanNum;
				frameSize		= Math.max( 1, (int)
									(AudioFileDescr.millisToSamples( outStream, pr.para[ PR_WINSTEP ].val) + 0.5) );
				// fftLength = frameSize+impulseResp.-1 auf 2er Potenz aufgerundet
				// phase interpolation is a kind of convolution (imp1 with imp2); therefore the
				// fft size must be expanded(?)
//				if( fftPolar ) i += impLength;
				for( len = frameSize + virtualImpLen - 1, fftLength = 2; fftLength < len; fftLength <<= 1 ) ;
				complexFFTsize	= fftLength << 1;

				fftPolar		= pr.intg[ PR_MORPHPOLICY ] == MORPH_POLAR;
				inOutOverlap	= Math.min( (frameSize >> 1), (int)
									(AudioFileDescr.millisToSamples( outStream, pr.para[ PR_WINOVERLAP ].val) + 0.5) );

				// window overlap
				if( inOutOverlap > 0 ) {
					inOverBuf	= new float[ inChanNum ][ inOutOverlap ];
					outOverBuf	= new float[ inChanNum ][ inOutOverlap ];
//					fadeWin2	= Filter.createKaiserWindow( inOutOverlap, 6.0f );

					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inOverBuf[ ch ];
						for( int i = 0; i < inOutOverlap; i++ ) {			// clear overlap buffer
							convBuf1[ i ] = 0.0f;
						}
					}
					for( int ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outOverBuf[ ch ];
						for( int i = 0; i < inOutOverlap; i++ ) {			// clear overlap buffer
							convBuf1[ i ] = 0.0f;
						}
					}
				}

				mImpDepth		= new Param( pr.para[ PR_IRNUMBER ].val - 1.0, pr.para[ PR_IRNUMBER ].unit );
				mStream			= new SpectStream();
				mStream.setChannels( inChanNum );
				mStream.setBands( 0.0f, (float) inStream.rate / 2, 1, SpectStream.MODE_LIN );
				mStream.setRate( (float) inStream.rate, frameSize - inOutOverlap );				// !!
				mStream.setEstimatedLength( (inLength + frameSize - inOutOverlap - 1) /
											(frameSize - inOutOverlap) );					// !!
				mStream.getDescr();

				impBuf			= new float[ impChanNum ][ fftLength+2 ];
				for( int i = 0; i < 2; i++ ) {
					mBuf[ i ] = new float[ impChanNum ][ minPhase ? complexFFTsize : fftLength+2 ];		// lot's of mem ;(
				}
				
				mImpMod			= new Modulator( mImpBase, mImpDepth, pr.envl[ PR_IRMODENV ], mStream );
				mImpIndex.val	= -1.0;
				for( int i = 0; i < inLength; i += frameSize ) {	// calc. mImpReads
					foo	= mImpMod.calc();						// index
					d1	= foo.val;
					if( (d1 % 1.0) < 0.0012 ) d1 = Math.floor( d1 );	// ŸberflŸssiges 0.0000xyz% morphing
					if( (d1 % 1.0) > 0.9988 ) d1 = Math.ceil( d1 );		// ŸberflŸssiges 0.9999xyz% morphing
					foo.val = d1;

					if( Math.abs( d1 - mImpIndex.val ) >= 0.0012 ) {	// (only if +- 0.01 dB to improve speed)
						newImpIndex[ 0 ] = (int) Math.floor( d1 );
						newImpIndex[ 1 ] = (int) Math.ceil( d1 );
						mMorphs++;
						mImpIndex = foo;
					}
					for( int j = 0; j < 2; j++ ) {
						if( (newImpIndex[ j ] != oldImpIndex[ 0 ]) && (newImpIndex[ j ] != oldImpIndex[ 1 ])) {
							mImpReads++;
							if( newImpIndex[ 1-j ] == oldImpIndex[ j ]) {		// be kind and don't overwrite useful mBuf
								swap				= oldImpIndex[ j ];
								oldImpIndex[ j ]	= oldImpIndex[ 1-j ];
								oldImpIndex[ 1-j]	= swap;
							}
							oldImpIndex[ j ] = newImpIndex[ j ];	// simulate reading
						}
					}
					mStream.framesRead++;			// dirty! XXX
				}
				mImpIndex.val		= -1.0;		// reset values; modulator must be reset
				oldImpIndex[ 0 ]	= -1;
				oldImpIndex[ 1 ]	= -1;
				newImpIndex[ 0 ]	= 0;
				newImpIndex[ 1 ]	= 0;
				mStream.framesRead	= 0;
				mImpMod				= new Modulator( mImpBase, mImpDepth, pr.envl[ PR_IRMODENV ], mStream );
// System.out.println( "Total reads: "+mImpReads+"; morphs "+mMorphs );

			} else {
				// fftLength = 2*impulseResp.laenge auf 2er Potenz aufgerundet
				for( len = 2*(virtualImpLen-1), fftLength = 2; fftLength < len; fftLength <<= 1 ) ;
				complexFFTsize	= fftLength << 1;
				frameSize		= fftLength - virtualImpLen + 1;
				mImpReads		= 1;
				impBuf			= new float[ impChanNum ][ minPhase ? complexFFTsize : fftLength+2 ];
				for( int i = 0; i < 2; i++ ) {
					mBuf[ i ] = impBuf;
				}
			} // if PR_MORPH

			overlap = virtualImpLen - 1;
//			if( fftPolar ) overlap += impLength;
			if( pr.bool[ PR_TRUNCOVER ]) {				// truncate overlap; calculate window
				overlap = Math.min( overlap, (int)
									(AudioFileDescr.millisToSamples( outStream, pr.para[ PR_FADELENGTH ].val) + 0.5) );
				if( overlap > 0 ) {
					fadeWin = Filter.createKaiserWindow( overlap, 6.0f );
				}
			}

			// adjust "support"
			impF.readMarkers();
			markers = (java.util.List) impStream.getProperty( AudioFileDescr.KEY_MARKERS );
			regions = (java.util.List) impStream.getProperty( AudioFileDescr.KEY_REGIONS );
			if( markers != null ) {
				markerIdx = Marker.find( markers, MARK_SUPPORT, 0 );
				if( markerIdx >= 0 ) {	// impulse file contains "support" marker ==> adjust output markers
					support	= (int) ((Marker) markers.get( markerIdx )).pos;
					if( support > impLength ) {	// may be due to morphing
						support = 0;
					}
					if( minPhase ) support = 0;	// XXX unable to calculate in advance
				}
			}
			if( pr.intg[ PR_LENGTH ] == LENGTH_FULL ) {
				if( support > 0 ) {	// impulse file contains "support" marker ==> adjust output markers
					if( markers != null ) {
						for( int k = 0; k < markers.size(); k++ ) {
							mark = (Marker) markers.get( k );
							markers.set( k, new Marker( mark.pos + support, mark.name ));
						}
						if( Marker.find( markers, MARK_SUPPORT, 0 ) < 0 ) {
							Marker.add( markers, new Marker( support, MARK_SUPPORT ));
						}
					} else {
						markers = new Vector();
						Marker.add( markers, new Marker( support, MARK_SUPPORT ));
						outStream.setProperty( AudioFileDescr.KEY_MARKERS, markers );
					}
					if( regions != null ) {
						for( int k = 0; k < regions.size(); k++ ) {
							region			= (Region) regions.get( k );
							regions.set( k, new Region( new Span( region.span.getStart() + support,
																  region.span.getStop() + support ), region.name ));
						}
					}
					region = (Region) outStream.getProperty( AudioFileDescr.KEY_LOOP );
					if( region != null ) {
						outStream.setProperty( AudioFileDescr.KEY_LOOP,
							new Region( new Span( region.span.getStart() + support,
												  region.span.getStop() + support ), region.name ));
					}
				}
			}
			outF = AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			switch( pr.intg[ PR_LENGTH ]) {
			case LENGTH_FULL:
				outLength	= inLength + overlap;
				break;
			case LENGTH_INPUT:
				outLength	= inLength;
				break;
			case LENGTH_SKIPSUPPORT:
				outLength	= inLength + Math.max( 0, overlap - support );
				break;
			default:
				throw new IllegalArgumentException( String.valueOf( pr.intg[ PR_LENGTH ]));
			}
			totalOutSamples	= outLength * outChanNum;

// System.out.println( "imp.length "+impLength+"; fft length "+fftLength+"; input step"+frameSize+"; overlap "+overlap+"; totalIn "+totalInSamples+"; totalOut "+totalOutSamples );

			inBuf	= new float[ outChanNum ][ fftLength+2 ];
			overBuf	= new float[ outChanNum ][ overlap ];

			progOff	= 0;
			progLen	= (long) (mImpReads*(impLength + totalImpSamples)) + (long) (inLength + totalInSamples) + (long) (outLength + totalOutSamples);

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				tempFile	= new File[ outChanNum ];
				floatF		= new FloatFile[ outChanNum ];
				for( int ch = 0; ch < outChanNum; ch++ ) {
					tempFile[ ch ]	= IOUtil.createTempFile();
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
				}
				progLen	   += (long) outLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== da convolution ====================----
//boolean shown = false;
 
			framesRead		= 0;
			framesWritten	= 0;
			if( pr.intg[ PR_LENGTH ] != LENGTH_FULL ) {
				framesWritten = -support;	// frames to skip
			}

			// includes final overlap flush
			for( boolean unfinished = true; threadRunning && unfinished; ) {

				// ==================== read input chunk ====================
				if( inOverlap > 0 ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {		// input overlap buffer => input buffer
						System.arraycopy( inOverBuf[ ch ], 0, inBuf[ ch ], 0, inOverlap );
					}
				}
				for( off = inOverlap, chunkLength = Math.min( inLength - framesRead + off, frameSize );
					 threadRunning && (off < chunkLength); ) {

					len			 = Math.min( 8192, chunkLength - off );
					inF.readFrames( inBuf, off, len );
					framesRead	+= len;
					off			+= len;
					progOff		+= len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero-padding
				for( int ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = inBuf[ ch ];
					for( int i = chunkLength; i < fftLength+2; i++ ) {
						convBuf1[ i ] = 0.0f;
					}
				}

// System.out.println( "Samples Read "+framesRead+"; chunk len "+chunkLength );
				if( inOutOverlap > 0 ) {
					inOverlap = inOutOverlap; 	// Math.max( 0, chunkLength - frameSize + inOutOverlap );
// System.out.println( inOverlap+" from inBuf "+(frameSize-inOutOverlap)+" => inOver" );
					for( int ch = 0; ch < inChanNum; ch++ ) {		// input buffer => input overlap buffer
						System.arraycopy( inBuf[ ch ], frameSize - inOutOverlap, inOverBuf[ ch ], 0, inOverlap );
					}
				}

				// forward transform
				for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					Fourier.realTransform( inBuf[ ch ], fftLength, Fourier.FORWARD );
					progOff += chunkLength - inOverlap;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
				// copy transforms in case of channel expansion
				for( int ch = inChanNum; ch < outChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch % inChanNum ], 0, inBuf[ ch ], 0, fftLength+2 );
				}

				if( chunkLength > inOverlap ) {
					// ==================== calc impulse-index ====================
					if( mImpMod != null ) {
						foo	= mImpMod.calc();
						mStream.framesRead++;
						d1	= Math.max( 0.0, Math.min( mImpDepth.val, foo.val ));
						if( (d1 % 1.0) < 0.0012 ) d1 = Math.floor( d1 );	// ŸberflŸssiges 0.0000xyz% morphing
						if( (d1 % 1.0) > 0.9988 ) d1 = Math.ceil( d1 );		// ŸberflŸssiges 0.9999xyz% morphing
						foo.val = d1;
						if( Math.abs( d1 - mImpIndex.val ) >= 0.0012 ) {
							mImpIndex	= foo;
							needsMorph	= true;
// System.out.println( "needs remorph; mImpIndex.val = "+d1 );
							newImpIndex[ 0 ] = (int) Math.floor( d1 );
							newImpIndex[ 1 ] = (int) Math.ceil(  d1 );
						}
					}
	
					// ==================== check IR validity ====================
					for( int i = 0; threadRunning && (i < 2); i++ ) {
// System.out.println( "newImpIndex["+i+"] = "+newImpIndex[ i ]);
						if( (newImpIndex[ i ] != oldImpIndex[ 0 ]) && (newImpIndex[ i ] != oldImpIndex[ 1 ])) {
// System.out.println( "needs to be loaded" );
							if( newImpIndex[ 1 - i ] == oldImpIndex[ i ]) {	// be kind and don't overwrite useful mBuf
// System.out.println( "swap buffers" );
								convBuf3			= mBuf[ i ];		// swap
								mBuf[ i ]			= mBuf[ 1-i ];
								mBuf[ 1-i ]			= convBuf3;
								swap				= oldImpIndex[ i ];
								oldImpIndex[ i ] 	= oldImpIndex[ 1-i ];
								oldImpIndex[ 1-i ]	= swap;
							}
				
							// ==================== read impulse ====================
							convBuf3 = mBuf[ i ];
							impF.seekFrame( newImpIndex[ i ] * impLength );
// System.out.println( "read imp from "+(newImpIndex[ i ] * impLength) );
							for( impFramesRead = 0; threadRunning && (impFramesRead < impLength); ) {
								len				 = Math.min( 8192, impLength - impFramesRead );
								impF.readFrames( convBuf3, impFramesRead, len );
								impFramesRead	+= len;
								progOff			+= len;
							// .... progress ....
								setProgression( (float) progOff / (float) progLen );
							}
						// .... check running ....
							if( !threadRunning ) break topLevel;

							// zero-padding
							for( int ch = 0; ch < impChanNum; ch++ ) {
								convBuf1 = convBuf3[ ch ];
								for( int j = impLength; j < fftLength+2; j++ ) {
									convBuf1[ j ] = 0.0f;
								}
							}

							if( pr.bool[ PR_NORMIMPPOWER ]) {		// normalize IR power
								impPower = 0.0;
								for( int ch = 0; ch < impChanNum; ch++ ) {
									convBuf1 = convBuf3[ ch ];
									for( int j = 0; j < impLength; j++ ) {
										f1		  = convBuf1[ j ];
										impPower += f1*f1;
//										impPower += f1;
									}
								}
								impPower	= Math.sqrt( impPower ) / impChanNum;
//								impPower   /= impChanNum;
								d1			= Math.min( 1000.0, 1.0 / Math.abs( impPower ));	// max. 60 dB boost
								for( int ch = 0; ch < impChanNum; ch++ ) {
									convBuf1 = convBuf3[ ch ];
									for( int j = 0; j < impLength; j++ ) {
										convBuf1[ j ] = (float) ((double) convBuf1[ j ] * d1);
									}
								}
							}

							// ==================== impulse forward FFT ====================
							for( int ch = 0; threadRunning && (ch < impChanNum); ch++ ) {
								convBuf1 = convBuf3[ ch ];
								Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
								if( pr.intg[ PR_MODE ] == MODE_DECONV ) {			// reziprok spectrum
									startIdx	= fftLength+4;
									stopIdx		= -1;
									for( int k = 0, j = 1; k <= fftLength; k += 2, j += 2 ) {
										f1			  = convBuf1[ k ] * convBuf1[ k ] + convBuf1[ j ] * convBuf1[ j ];
										if( f1 != 0.0f ) {
											convBuf1[ k ]  /= f1;
											convBuf1[ j ]  /= -f1;
										} else {
											convBuf1[ k ]	= Float.POSITIVE_INFINITY;
											convBuf1[ j ]	= Float.POSITIVE_INFINITY;
											startIdx		= Math.min( startIdx, k );
											stopIdx			= Math.max( stopIdx, j );
										}
									}
									// Division by zero may produce NaN values; interpolate these
									for( int k = startIdx; k <= stopIdx; k++ ) {
										f1 = convBuf1[ k ];
										if( f1 != Float.POSITIVE_INFINITY ) continue;

										for( int j = k - 2, m = 1, l = 0; m >= 0; j -= 2 ) {
											l = Math.abs( j );
											if( l <= fftLength+1 ) {
												f1 = convBuf1[ l ];
												if( f1 != Float.POSITIVE_INFINITY ) {
													ya[ m ]		= f1;
													xa[ m-- ]	= (float) j;
												}
											} else {
												ya[ m ]		= 0.0f;
												xa[ m-- ]	= j;
											}
										}
										for( int j = k + 2, m = 2, l = 0; m <= 3; j += 2 ) {
											l = (j <= fftLength+1) ? j : ((fftLength << 1) - j);
											if( l >= 0 ) {
												f1 = convBuf1[ l ];
												if( f1 != Float.POSITIVE_INFINITY ) {
													ya[ m ]		= f1;
													xa[ m++ ]	= (float) j;
												}
											} else {
												ya[ m ]		= 0.0f;
												xa[ m++]	= (float) j;
											}
										}

// System.out.println( "gonna interp" );		
										convBuf1[ k ] = Util.polyInterpolate( xa, ya, 4, (float) k );
// System.out.println( "interpolated value "+convBuf1[ k ]+" @"+k);
									}


								} else if( pr.intg[ PR_MODE ] == MODE_CONVINV ) {	// inverse spectrum
									d1	= Double.POSITIVE_INFINITY;
									d2	= 0.0f;
									for( int k = 0, j = 1; k <= fftLength; k += 2, j += 2 ) {	// find min/max amp
										d3	= convBuf1[ k ] * convBuf1[ k ] + convBuf1[ j ] * convBuf1[ j ];
										if( d3 > d2 ) {
											d2	= d3;
										} else if( d3 < d1 ) {
											d1	= d3;
										}
									}
									d1 = Math.sqrt( d1 ) + Math.sqrt( d2 );
									for( int k = 0, j = 1; k <= fftLength; k += 2, j += 2 ) {	// make min => max & max => min
										f1		= (float) (d1 / Math.sqrt( convBuf1[ k ] * convBuf1[ k ] +
																		   convBuf1[ j ] * convBuf1[ j ]) - 1);
										convBuf1[ k ] *= f1;	// i.e. scale with delta-radius divided by radius
										convBuf1[ j ] *= f1;
									}
								} // if( pr.intg[ PR_MODE ] == MODE_DECONV or MODE_CONVINV
								// MORPH_POLAR: convert to polar and unwrap phases
								if( fftPolar || minPhase ) {
									Fourier.rect2Polar(   convBuf1, 0, convBuf1, 0, fftLength+2 );
									Fourier.unwrapPhases( convBuf1, 0, convBuf1, 0, fftLength+2 );
								}
								progOff += impLength;
							// .... progress ....
								setProgression( (float) progOff / (float) progLen );
								
							// .............. convert to minimum phase ..............
								if( minPhase ) {
									for( int j = 0; j <= fftLength; j += 2 ) {
										convBuf1[ j ] = (float) Math.log( Math.max( 1.0e-48, convBuf1[ j ]));
									}
									// remove phase line ? XXX
									
									// make complex conjugate spectrum
									for( int j = fftLength + 2, k = fftLength - 2; j < complexFFTsize; k -= 2, j += 2 ) {
										convBuf1[ j ]	= convBuf1[ k ];
										convBuf1[ j+1 ] = -convBuf1[ k+1 ];
									}
									// transform to cepstrum domain
									Fourier.complexTransform( convBuf1, fftLength, Fourier.INVERSE );
									// fold cepstrum (make anticausal parts causal)
									for( int j = 2, k = complexFFTsize - 2; j < fftLength; j += 2, k -= 2 ) {
										convBuf1[ j ]   += convBuf1[ k ];		// add conjugate left wing to right wing
										convBuf1[ j+1 ] -= convBuf1[ k+1 ];
									}
									convBuf1[ fftLength + 1 ] = -convBuf1[ fftLength + 1 ];
									// clear left wing
									for( int j = fftLength + 2; j < complexFFTsize; j++ ) {
										convBuf1[ j ] = 0.0f;
									}
									// back to frequency domain
									Fourier.complexTransform( convBuf1, fftLength, Fourier.FORWARD );
									// complex exponential : mag' = exp(real); phase' = imag
									for( int j = 0; j <= fftLength; j += 2 ) {
										convBuf1[ j ]		= (float) Math.exp( convBuf1[ j ]);
									}
									// go to timedomain and apply window
									Fourier.polar2Rect( convBuf1, 0, convBuf1, 0, fftLength + 2 );
									Fourier.realTransform( convBuf1, fftLength, Fourier.INVERSE );
									maxImpAmp	= 0.0f;
									support		= virtualImpLen >> 1;	// will be overriden below
//									for( int j = 0; j < virtualImpLen; j++ ) {
									for( int j = 0; j < impLength; j++ ) {
										f1 = Math.abs( convBuf1[ j ]);
										if( f1 > maxImpAmp ) {
											maxImpAmp	= f1;
											support		= j;	// empiricially determine support from greatest elongation
										}
									}
									len		= support >> 1;

//if( !shown ) {
//Debug.view( convBuf1, 0, fftLength, "Time Domain" );
//System.err.println( "fftLength = "+fftLength+"; convBuf1.length =" +convBuf1.length+"; support = "+support+ "; virtualImpLen = "+virtualImpLen );
//}


									if( len > 0 ) {
										win		= Filter.createWindow( len, Filter.WIN_KAISER6 );
//if( !shown ) {
//Debug.view( win, 0, win.length, "Window" );
//System.err.println( "win.length = "+win.length );
//}
										for( int j = len - 1, k = 0; k < win.length; j--, k++ ) {
											convBuf1[ j ] *= win[ k ];
										}
//										for( int j = virtualImpLen - len, k = 0; j < virtualImpLen; j++, k++ ) {
										for( int j = impLength - len, k = 0; j < impLength; j++, k++ ) {
											convBuf1[ j ] *= win[ k ];
										}
									}
//									for( int j = virtualImpLen; j < fftLength; j++ ) {
									for( int j = impLength; j < fftLength; j++ ) {
										convBuf1[ j ] = 0.0f;
									}
//if( !shown ) {
//Debug.view( convBuf1, 0, fftLength, "Windowed" );
//shown = true;
//}
									// back to spectrum
									Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
									if( fftPolar ) Fourier.rect2Polar( convBuf1, 0, convBuf1, 0, fftLength + 2 );
								} // if( minPhase )
							} // for channels
	
							oldImpIndex[ i ] = newImpIndex[ i ];			// impulse erfolgreich geladen
						} // if impulse needs to be loaded
					} // for 2
				// .... check running ....
					if( !threadRunning ) break topLevel;
	
					// ==================== IR morphing ====================
					if( needsMorph ) {
// System.out.println( "morphing "+mImpIndex.val );
// System.out.println( "convBuf3 = mBuf0 ? "+(newImpIndex[ 0 ] == oldImpIndex[ 0 ]));
// System.out.println( "convBuf4 = mBuf1 ? "+(newImpIndex[ 1 ] == oldImpIndex[ 1 ]));
						convBuf3 = (newImpIndex[ 0 ] == oldImpIndex[ 0 ]) ? mBuf[ 0 ] : mBuf[ 1 ];
						convBuf4 = (newImpIndex[ 1 ] == oldImpIndex[ 1 ]) ? mBuf[ 1 ] : mBuf[ 0 ];
	
						if( convBuf3 != convBuf4 ) {
							d2	= mImpIndex.val % 1.0;
							d1	= 1.0 - d2;
// System.out.println( "need real morph A="+f1+"; B="+f2 );
							for( int ch = 0; ch < impChanNum; ch++ ) {
								convBuf1 = convBuf3[ ch ];
								convBuf2 = convBuf4[ ch ];
								convBuf5 = impBuf[ ch ];
								for( int i = 0; i <= fftLength; ) {
									convBuf5[ i ]	= (float) (d1 * convBuf1[ i ] + d2 * convBuf2[ i ]); i++;
									convBuf5[ i ]	= (float) (d1 * convBuf1[ i ] + d2 * convBuf2[ i ]); i++;
								}
							}
						} else {
// System.out.println( "just copy" );
							for( int ch = 0; ch < impChanNum; ch++ ) {
								System.arraycopy( convBuf3[ ch ], 0, impBuf[ ch ], 0, fftLength+2 );
							}
						}
						if( fftPolar ) {
							for( int ch = 0; ch < impChanNum; ch++ ) {
								// Fourier.wrapPhases( impBuf[ ch ], 0, impBuf[ ch ], 0, fftLength+2 );
								Fourier.polar2Rect( impBuf[ ch ], 0, impBuf[ ch ], 0, fftLength+2 );
							}
						}
						needsMorph = false;
					}
					
					// ==================== convolution = mult+inverse FFT ====================
					for( int ch = 0; threadRunning && (ch < outChanNum); ch++ ) {
						convBuf1 = inBuf[ ch ];
						convBuf2 = impBuf[ ch % impChanNum ];
	
						// note: fftLength+1 steps
						for( int i = 0, j = 1; i <= fftLength; i += 2, j += 2 ) {		// complex multiplication
							f1				= convBuf1[ i ];		// (avoid overwrite)
							convBuf1[ i ]	= f1 * convBuf2[ i ] - convBuf1[ j ] * convBuf2[ j ];
							convBuf1[ j ]	= f1 * convBuf2[ j ] + convBuf1[ j ] * convBuf2[ i ];
						}
						Fourier.realTransform( convBuf1, fftLength, Fourier.INVERSE );	// inverse transform
	
						convBuf2	= overBuf[ ch ];
						len			= Math.min( overlap, chunkLength );
						for( int i = len - 1; i >= 0; i-- ) {								// add old overlap
							convBuf1[ i ] += convBuf2[ i ];
						}
						System.arraycopy( convBuf2, len, convBuf2, 0, overlap - len );	// shift buffer
						if( fadeWin == null ) {
							for( int i = 0, k = chunkLength; i < overlap - len; i++, k++ ) {	// save new overlap (add)
								convBuf2[ i ] += convBuf1[ k ];
							}
							for( int i = overlap - len, k = chunkLength + i; i < overlap; i++, k++ ) {		// save new overlap (replace)
								convBuf2[ i ] = convBuf1[ k ];
							}
						} else {
							for( int i = 0, k = chunkLength; i < overlap - len; i++, k++ ) {	// save new overlap (add)
								convBuf2[ i ] += fadeWin[ i ] * convBuf1[ k ];			// (w/ window multiplication)
							}
							for( int i = overlap - len, k = chunkLength + i; i < overlap; i++, k++ ) {			// save new overlap (replace)
								convBuf2[ i ] = fadeWin[ i ] * convBuf1[ k ];
							}
						}
						
						progOff += chunkLength - inOverlap;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

				} // if chunkLength > 0
				else {
// System.out.println( framesWritten+" von "+outLength+"; outOverlap "+outOverlap+"; overlap "+overlap );
					// final flushing
					chunkLength = Math.min( outLength - framesWritten, Math.max( outOverlap, overlap ));
					inOverlap	= 0;
					unfinished	= false;
					for( int ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						convBuf2 = overBuf[ ch ];
						for( int i = 0; i < overlap; i++ ) {								// add old overlap
							convBuf1[ i ] += convBuf2[ i ];
						}
						progOff += chunkLength;
					}
				}
	
				// ==================== write output chunk ====================
				if( inOutOverlap > 0 ) {
					for( int ch = 0; ch < outChanNum; ch++ ) {			// x-fade output buffer, copy output buf => overbuf
						convBuf1 = inBuf[ ch ];
						convBuf2 = outOverBuf[ ch ];
						for( int j = 0, k = frameSize - inOutOverlap; j < outOverlap; ) {
							f1				= j / (outOverlap - 1 );	// fadeWin2[ j ];
							f2				= convBuf1[ k++ ];
							convBuf1[ j ]	= convBuf1[ j ] * f1 + convBuf2[ j ] * (1.0f - f1);
							convBuf2[ j++ ]	= f2;
						}
						System.arraycopy( convBuf1, frameSize - inOutOverlap + outOverlap, convBuf2, outOverlap,
										  inOutOverlap - outOverlap );
// for( j = outOverlap; j < chunkLength; j++ ) convBuf1[ j ] = 0.0f;
					}
					outOverlap = inOverlap;
				}
// System.out.println( "Samples written "+framesWritten );

				chunkLength -= outOverlap;
				off			 = 0;
//				chunkLength = Math.min( outLength - framesWritten, chunkLength - outOverlap );
				if( framesWritten < 0 ) {	// support skipping
					off			   = Math.min( -framesWritten, chunkLength );
					framesWritten += off;
				}
				if( framesWritten >= 0 ) {
					while( threadRunning && (off < chunkLength) ) {
						len = Math.min( 8192, chunkLength - off );
						if( floatF != null ) {
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1 = inBuf[ ch ];
								for( int i = off, k = 0; k < len; i++, k++ ) {
									f1 = Math.abs( convBuf1[ i ]);
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
								}
								floatF[ ch ].writeFloats( convBuf1, off, len );
							}
	
						} else {	// needs extra mult.
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1 = inBuf[ ch ];
								for( int i = off, k = 0; k < len; i++, k++ ) {
									f1			   = Math.abs( convBuf1[ i ]);
									convBuf1[ i ] *= gain;
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
								}
							}
							outF.writeFrames( inBuf, off, len );
						}
						framesWritten	+= len;
						off				+= len;
						progOff			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
// System.out.println( "Samples written "+framesWritten );
			} // for all output samples

		// ---- normalize output ----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;

				normalizeAudioFile( floatF, outF, inBuf, gain, 1.0f );
				for( int ch = 0; ch < outChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}

// circumvent progLen calculation bug in non-normalize mode XXX
setProgression( 1.0f );

		// ---- Finish ----

			impF.close();
			impF		= null;
			impStream	= null;
			convBuf1	= null;
			convBuf2	= null;
			convBuf3	= null;
			convBuf4	= null;
			convBuf5	= null;
			impBuf		= null;
			mBuf		= null;
			overBuf		= null;
			inOverBuf	= null;
			outOverBuf	= null;
			fadeWin		= null;
//			fadeWin2	= null;
			inF.close();
			inF			= null;
			inStream	= null;
			inBuf		= null;
			outF.close();
			outF		= null;
			outStream	= null;
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// inform about clipping/ low level
			maxAmp *= gain;
			handleClipping( maxAmp );

		} // topLevel
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			convBuf1	= null;
			convBuf2	= null;
			convBuf3	= null;
			convBuf4	= null;
			convBuf5	= null;
			inBuf		= null;
			impBuf		= null;
			mBuf		= null;
			overBuf		= null;
			inOverBuf	= null;
			outOverBuf	= null;
			fadeWin		= null;
//			fadeWin2	= null;
			inStream	= null;
			impStream	= null;
			outStream	= null;
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
		if( impF != null ) {
			impF.cleanUp();
		}
		if( floatF != null ) {
			for( int ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_FADELENGTH );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_TRUNCOVER ]);
		}
		c = gui.getItemObj( GG_MORPHPOLICY );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_MORPH ]);
		}
		c = gui.getItemObj( GG_IRNUMBER );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_MORPH ]);
		}
		c = gui.getItemObj( GG_IRMODENV );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_MORPH ]);
		}
		c = gui.getItemObj( GG_WINSTEP );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_MORPH ]);
		}
		c = gui.getItemObj( GG_WINOVERLAP );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_MORPH ]);
		}
	}
}
// class ConvolutionDlg
