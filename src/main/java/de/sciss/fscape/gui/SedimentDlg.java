/*
 *  SedimentDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2013 Hanns Holger Rutz. All rights reserved.
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
 *		17-Jun-07	created
 */

package de.sciss.fscape.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Processing module for replacing occurrences of
 *	one sound by another. The idea is to correlate
 *	an input sound by an "icon" sound and whenever
 *	the correlation exceeds a threshold, a "replacement"
 *	sound is plotted to the output file. Never really worked.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 25-Dec-08
 */
public class SedimentDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_PATTERNFILE			= 1;
	private static final int PR_OUTPUTFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_CHANTYPE			= 3;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_WINSIZE				= 1;
	private static final int PR_MAXBOOST			= 2;
	private static final int PR_TIMESCALE			= 3;
	private static final int PR_MINGRAINLEN			= 4;
	private static final int PR_MAXGRAINLEN			= 5;
	private static final int PR_CLUMP				= 6;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_PATTERNFILE		= "PtrnFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_WINSIZE			= "WinSize";
	private static final String PRN_MAXBOOST		= "MaxBoost";
	private static final String PRN_TIMESCALE		= "TimeScale";
	private static final String PRN_CHANTYPE		= "ChanType";
	private static final String PRN_MINGRAINLEN		= "MinGrainLen";
	private static final String PRN_MAXGRAINLEN		= "MaxGrainLen";
	private static final String PRN_CLUMP			= "Clump";
		
	private static final int	CHAN_MIN			= 0;
	private static final int	CHAN_MAX			= 1;

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_PATTERNFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, CHAN_MAX };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_CHANTYPE };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_WINSIZE, PRN_MAXBOOST, PRN_TIMESCALE, PRN_MINGRAINLEN, PRN_MAXGRAINLEN, PRN_CLUMP };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_PATTERNFILE		= GG_OFF_PATHFIELD	+ PR_PATTERNFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_CHANTYPE		= GG_OFF_CHOICE		+ PR_CHANTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_WINSIZE			= GG_OFF_PARAMFIELD	+ PR_WINSIZE;
	private static final int GG_MAXBOOST		= GG_OFF_PARAMFIELD	+ PR_MAXBOOST;
	private static final int GG_TIMESCALE		= GG_OFF_PARAMFIELD	+ PR_TIMESCALE;
	private static final int GG_MINGRAINLEN		= GG_OFF_PARAMFIELD	+ PR_MINGRAINLEN;
	private static final int GG_MAXGRAINLEN		= GG_OFF_PARAMFIELD	+ PR_MAXGRAINLEN;
	private static final int GG_CLUMP			= GG_OFF_PARAMFIELD	+ PR_CLUMP;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "Input + pattern must share\nsame # of channels!";

	// debuggingdorfer
	private static final boolean checkNaN	= false;
	private static final boolean verbose	= false;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SedimentDlg()
	{
		super( "Sediment" );
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
			static_pr.para[ PR_WINSIZE ]		= new Param(   20.0, Param.FACTOR_TIME );
			static_pr.para[ PR_MAXBOOST ]		= new Param(   20.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_TIMESCALE ]		= new Param(  100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_MINGRAINLEN ]	= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_MAXGRAINLEN ]	= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_CLUMP ]			= new Param(    1.0, Param.NONE );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile, ggPtrnFile;
		PathField[]			ggInputs;
		JComboBox			ggChanType;
		Component[]			ggGain;
		ParamField			ggWinSize, ggMaxBoost, ggTimeScale, ggMinGrainLen, ggMaxGrainLen, ggClump;
		ParamSpace[]		spcGrain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
//				int	ID = gui.getItemID( e );
//
//				switch( ID ) {
//				case GG_PLOTQUANT:
//				case GG_PLOTMAX:
//					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
//					reflectPropertyChanges();
//					break;
//				}
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
		gui.addLabel( new JLabel( "Control input", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggPtrnFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select matching pattern file" );
		ggPtrnFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Pattern input", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggPtrnFile, GG_PATTERNFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 2 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggInputs[ 1 ]	= ggPtrnFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$B0Sedi$B1$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Plot output", SwingConstants.RIGHT ));
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
	gui.addLabel( new GroupLabel( "Plotter settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

//		spcTimeScale	= new ParamSpace[] { new ParamSpace( Constants.spaces[ Constants.factorTimeSpace ])};
		ggTimeScale		= new ParamField( Constants.spaces[ Constants.factorTimeSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Time scale", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggTimeScale, GG_TIMESCALE, null );

		ggClump			= new ParamField( new ParamSpace( 1, 65536, 1, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Clump", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggClump, GG_CLUMP, null );
		
		spcGrain		= new ParamSpace[ 2 ];
		spcGrain[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcGrain[1]		= Constants.spaces[ Constants.absBeatsSpace ];

		ggMinGrainLen	= new ParamField( spcGrain );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min. grain length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMinGrainLen, GG_MINGRAINLEN, null );


		ggMaxBoost		= new ParamField( new ParamSpace[] { Constants.spaces[ Constants.ratioAmpSpace ], Constants.spaces[ Constants.decibelAmpSpace ]});
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max boost", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMaxBoost, GG_MAXBOOST, null );

		ggMaxGrainLen	= new ParamField( spcGrain );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max. grain length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMaxGrainLen, GG_MAXGRAINLEN, null );

		ggWinSize		= new ParamField( Constants.spaces[ Constants.ratioTimeSpace ] );
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Win size", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggWinSize, GG_WINSIZE, null );

		ggChanType		= new JComboBox();
		ggChanType.addItem( "Minimum" );
		ggChanType.addItem( "Maximum" );
		con.weightx		= 0.1;
		con.gridwidth	= 3;
		gui.addLabel( new JLabel( "Multichannel correlation", SwingConstants.RIGHT ));
		con.weightx		= 0.2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggChanType, GG_CHANTYPE, il );


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
		long					progOff;
		final long				progLen;
		float					f1, f2;
		double					d1;
		
		// io
		AudioFile				inF				= null;
		AudioFile				outF			= null;
		AudioFile				ptrnF			= null;
		final AudioFile			tmpF;
		final AudioFileDescr	inStream;
		final AudioFileDescr	outStream;
		final AudioFileDescr	ptrnStream;
		final int				inChanNum, outChanNum, ptrnChanNum;
		final long				inLength, outLength, ptrnLength;
		
		// buffers
		final float[][]			inBuf;
		float[]					win;

		final int				maxGrainLength, maxFFTLength, inBufSize;
		int						grainLength, grainLengthH, grainFFTLength, winLen, winLenH;
		long					framesRead, framesRead2, framesWritten, inOff;

		final Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float					gain			= 1.0f;								 		// gain abs amp
		float					maxAmp			= 0.0f;
				
		final double			winSizeFactor	= pr.para[ PR_WINSIZE ].val / 100;
		final int				winType			= Filter.WIN_HANNING; // XXX pr.intg[ PR_FADEWIN ];
		final double			timeScale		= pr.para[ PR_TIMESCALE ].val / 100;
		final int				chanType		= pr.intg[ PR_CHANTYPE ];
		final float				maxBoost		= (float) (Param.transform( pr.para[ PR_MAXBOOST ], Param.ABS_AMP, ampRef, null )).val;
		final double			minGrainLen		= Math.min( pr.para[ PR_MINGRAINLEN ].val, pr.para[ PR_MAXGRAINLEN ].val );
		final double			maxGrainLen		= Math.max( pr.para[ PR_MINGRAINLEN ].val, pr.para[ PR_MAXGRAINLEN ].val );
		final double			factorGrainLen	= maxGrainLen / minGrainLen;

		final PathField			ggOutput;

		float					corrAbsMaxRMS;
		int						idx;
		boolean					inv;
		long					maxPos;
		
		final float[]			ptrnRMS;
		final float[][][]		ptrnBuf, ptrnFFTBuf;
		final float[][]			corrAbsMaxs;
		final float[]			corrFFTBuf;
		final float[]			totalCorrAbsMaxRMS;
		final long[][]			corrAbsMaxPoss;
		final boolean[][]		corrAbsMaxInvs;
		final float[]			corrAbsMax;
		final long[]			corrAbsMaxPos;
		final boolean[]			corrAbsMaxInv;
		final float[]			totalCorrAbsMax;
		final long[]			totalCorrAbsMaxPos;
		final boolean[]			totalCorrAbsMaxInv;
//		final long[]			ptrnRead;
		// the idea of caching the forward FFTs doesn't work,
		// because the input is windowed according to grainLength
		// which can fluctuate despite FFTsize remaining the same ... ;-/
//		final int				minGrainLength, minFFTLength;
//		final boolean[]			fftDone;
//		final float[][]			offBuf;
//		final int				numFFTs;
		int						clump, chunkLen;
//		int						fftIdx;
		final int				numClump;	//		= 10
//		AudioFile[]				fftF			= null;
		long					progDelta, progOff2;
		
		final Random			rnd				= new Random();

topLevel: try {

		// ---- open input, output; init ----
			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ptrn input
			ptrnF			= AudioFile.openAsRead( new File( pr.text[ PR_PATTERNFILE ]));
			ptrnStream		= ptrnF.getDescr();
			ptrnChanNum		= ptrnStream.channels;
			ptrnLength		= ptrnStream.length;
			// this helps to prevent errors from empty files!
			if( (ptrnLength < 1) || (ptrnChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			if( inChanNum != ptrnChanNum ) {
				throw new IOException( ERR_CHANNELS );
			}

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outChanNum	= inChanNum;
			outStream.channels = outChanNum;
			outF		= AudioFile.openAsWrite( outStream );
			tmpF		= createTempFile( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ---- further inits ----
			
			numClump			= (int) pr.para[ PR_CLUMP ].val;
			corrAbsMaxs			= new float[ numClump ][ inChanNum ];
			corrAbsMaxPoss		= new long[ numClump ][ inChanNum ];
			corrAbsMaxInvs		= new boolean[ numClump ][ inChanNum ];
			
//			minGrainLength		= (int) (AudioFileDescr.millisToSamples( inStream, minGrainLen ) + 0.5);
			maxGrainLength		= (int) (AudioFileDescr.millisToSamples( inStream, maxGrainLen ) + 0.5);
//			minFFTLength		= Util.nextPowerOfTwo( minGrainLength + minGrainLength - 1 );
			maxFFTLength		= Util.nextPowerOfTwo( maxGrainLength + maxGrainLength - 1 );
//			{
//				int testNumFFTs = 1;
//				for( int i = minFFTLength; i < maxFFTLength; i <<= 1, testNumFFTs++ ) ;
//				numFFTs			= testNumFFTs;
//				if( verbose ) System.out.println( "numFFTs = " + numFFTs );
//			}
//			fftDone				= new boolean[ numFFTs ];
//			fftF				= new AudioFile[ numFFTs ];
//			offBuf				= new float[ inChanNum ][ 4 ];
			
			inBufSize			= Math.max( 8192, maxFFTLength + 2 );
			inBuf				= new float[ inChanNum ][ inBufSize ];
			ptrnFFTBuf			= new float[ numClump ][ inChanNum ][ maxFFTLength + 2 ];
			ptrnBuf				= new float[ numClump ][ inChanNum ][ maxGrainLength ];
			corrAbsMax			= new float[ numClump ];
			corrAbsMaxInv		= new boolean[ numClump ];
			corrAbsMaxPos		= new long[ numClump ];
			ptrnRMS				= new float[ numClump ];
			totalCorrAbsMax		= new float[ numClump ];
			totalCorrAbsMaxInv	= new boolean[ numClump ];
			totalCorrAbsMaxPos	= new long[ numClump ];
			totalCorrAbsMaxRMS	= new float[ numClump ];
//			ptrnRead			= new long[ numClump ];
			corrFFTBuf			= new float[ maxFFTLength + 2 ];
			
			outLength			= (long) (inLength * timeScale + 0.5) + maxGrainLength;
			
//			progLen				= ptrnLength + inLength + (outLength * 3);
//			progLen				= (ptrnLength << 2) + (inLength * inChanNum) + (outLength * 4);
			progLen				= ptrnLength + (ptrnLength * inLength) + (outLength * 3);
			progOff				= 0;

			framesWritten		= 0;

		// ----==================== read ptrn file ====================----
		
ptrnLoop:	for( framesRead = 0; threadRunning && (framesRead < ptrnLength); ) {

//try {
//Thread.sleep( 2000 );
//} catch( InterruptedException e1 ) {}
//
if( verbose ) System.err.println( "ptrnLoop " + framesRead );

				grainLength	= Math.max( 2, (int) (AudioFileDescr.millisToSamples( inStream, Math.pow( factorGrainLen, rnd.nextFloat() ) * minGrainLen ) + 0.5) & ~1 );
				grainLengthH = grainLength >> 1;
				grainFFTLength = Util.nextPowerOfTwo( grainLength + grainLength - 1 );
//				fftIdx = 0;
//				for( int i = minFFTLength; i < grainFFTLength; i <<= 1, fftIdx ++ ) ;
				winLen		= Math.max( 2, (int) (grainLength * winSizeFactor + 0.5) & ~1 );
				winLenH		= winLen >> 1;
				win			= Filter.createFullWindow( winLen, winType );
				clump		= 0;
				
				progDelta	= 0;

				for( int clumpIdx = 0; threadRunning && (clumpIdx < numClump) && (framesRead < ptrnLength); clumpIdx++, clump++ ) {
					chunkLen = (int) Math.min( ptrnLength - framesRead, grainLength );
					ptrnF.readFrames( ptrnBuf[ clumpIdx ], 0, chunkLen );
					framesRead += chunkLen;
					// zero padding
					if( chunkLen < grainLength ) {
						Util.clear( ptrnBuf[ clumpIdx ], chunkLen, grainLength - chunkLen);
					}
					// apply window
					Util.mult( win, 0, ptrnBuf[ clumpIdx ], 0, winLenH );
					Util.mult( win, winLenH, ptrnBuf[ clumpIdx ], grainLength - winLenH, winLenH );
				
					// copy to fft buffer
					Util.copy( ptrnBuf[ clumpIdx ], 0, ptrnFFTBuf[ clumpIdx ], 0, chunkLen );
					Util.clear( ptrnFFTBuf[ clumpIdx ], chunkLen, grainFFTLength - chunkLen );
				
					// reverse
					Util.reverse( ptrnFFTBuf[ clumpIdx ], 0, grainLength );

					// remove DC
					// ... normalize RMS (so we do not need the rms-division in the original pearson's formula!)
					// ... and transform to fourier domain
					ptrnRMS[ clumpIdx ] = 0f;
					for( int ch = 0; ch < ptrnChanNum; ch++ ) {
						Util.removeDC( ptrnFFTBuf[ clumpIdx ][ ch ], 0, grainLength );
						d1 = Math.sqrt( Filter.calcEnergy( ptrnFFTBuf[ clumpIdx ][ ch ], 0, chunkLen ));		// i.e. sqrt( sum(y^2) )
// too small values produce inf values in the below Util.mult !!!
d1 = d1 * (1000 * grainLength);

					    if( d1 > 0.0 ) {						// (double) fftLength
					    	Util.mult( ptrnFFTBuf[ clumpIdx ][ ch ], 0, chunkLen, (float) (1.0 / d1) );
					    } else continue ptrnLoop;
					    Fourier.realTransform( ptrnFFTBuf[ clumpIdx ][ ch ], grainFFTLength, Fourier.FORWARD );
					    ptrnRMS[ clumpIdx ] += (float) d1;
					}
					totalCorrAbsMax[ clumpIdx ]	= 0f;
					totalCorrAbsMaxPos[ clumpIdx ]	= 0;
					totalCorrAbsMaxInv[ clumpIdx ]	= false;
					totalCorrAbsMaxRMS[ clumpIdx ]	= ptrnRMS[ clumpIdx ];
//					ptrnRead[ clumpIdx ] = chunkLen;

//					progOff		+= (chunkLen << 2);
					progOff		+= chunkLen;
					progDelta	+= chunkLen;
					// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // for clumpIdx
				if( !threadRunning ) break topLevel;
				
				/////////////////// read through input and calc best correlation

				inOff				= 0;

if( verbose ) System.err.println( "readInput " + grainLength + "; clump = " + clump );
//System.gc();
			
//				if( fftF[ fftIdx ] == null ) {
//					fftF[ fftIdx ] = createTempFile( inChanNum, inStream.rate );
//				} else {
//					fftF[ fftIdx ].seekFrame(  0L );
//				}

				progOff2 = progOff;

readInput:		for( framesRead2 = 0; threadRunning && (framesRead2 < inLength); ) {

					chunkLen	= (int) Math.min( inLength - framesRead2, grainLength );

if( verbose ) System.out.println( "framesRead2 = " + framesRead2 );
					
//					if( fftDone[ fftIdx ]) {
//						fftF[ fftIdx ].readFrames( offBuf, 0, 4 );
//						fftF[ fftIdx ].readFrames( inBuf, 0, grainFFTLength + 2 );
////						inOff = (((long) Float.floatToRawIntBits( offBuf[ 0 ][ 0 ])) << 32) |
////							    (((long) Float.floatToRawIntBits( offBuf[ 0 ][ 1 ])) & 0xFFFFFFFF );
////						corrAbsMaxRMS = offBuf[ 0 ][ 2 ];
//						inOff = (((long) offBuf[ 0 ][ 0 ]) << 48) |
//								(((long) offBuf[ 0 ][ 1 ]) << 24) |
//								 ((long) offBuf[ 0 ][ 2 ]);
//						corrAbsMaxRMS = offBuf[ 0 ][ 3 ];
//						if( verbose ) System.err.println( "fftIdx = " + fftIdx + "; read off " + inOff );
//						
//					} else {
						inF.seekFrame( inOff );
						inF.readFrames( inBuf, 0, chunkLen );
//						if( chunkLen < grainLength ) {
//							Util.clear( inBuf, chunkLen, grainLength - chunkLen );
//						}
						Util.clear( inBuf, chunkLen, grainFFTLength - chunkLen );
						// apply window
						Util.mult( win, 0, inBuf, 0, winLenH );
						Util.mult( win, winLenH, inBuf, grainLength - winLenH, winLenH );

	 				  	corrAbsMaxRMS	= 0f;
						for( int ch = 0; ch < inChanNum; ch++ ) {
							Util.removeDC( inBuf[ ch ], 0, chunkLen );
							d1 = Math.sqrt( Filter.calcEnergy( inBuf[ ch ], 0, chunkLen ));		// i.e. sqrt( sum(y^2) )
if( checkNaN ) checkForNaN( inBuf[ ch ], "A" );
//too small values produce inf values in the below Util.mult !!!
d1 = d1 * (1000 * grainLength);
							if( d1 > 0.0 ) {
								Util.mult( inBuf[ ch ], 0, chunkLen, (float) (1.0 / d1) );
if( checkNaN ) checkForNaN( inBuf[ ch ], "B" );
							} else {
								framesRead2  = inOff + chunkLen;
								inOff		 = Math.min( inOff + grainLengthH, inLength );	// i.e. overlap
								progOff += (chunkLen * progDelta);
								continue readInput;
							}
							
							corrAbsMaxRMS += (float) d1;
							Fourier.realTransform( inBuf[ ch ], grainFFTLength, Fourier.FORWARD );
if( checkNaN ) checkForNaN( inBuf[ ch ], "C" );
						} // for ch
//						offBuf[ 0 ][ 0 ] = (float) ((inOff >> 48) & 0x00FFFFFF);
//						offBuf[ 0 ][ 1 ] = (float) ((inOff >> 24) & 0x00FFFFFF);
//						offBuf[ 0 ][ 2 ] = (float) (inOff & 0x00FFFFFF);
//						offBuf[ 0 ][ 3 ] = corrAbsMaxRMS;
//						fftF[ fftIdx ].writeFrames( offBuf, 0, 4 );
//						fftF[ fftIdx ].writeFrames( inBuf, 0, grainFFTLength + 2 );
//						if( verbose ) System.err.println( "fftIdx = " + fftIdx + "; wrote off " + inOff );
//					}
					
					// remove DC
					// ... normalize RMS (so we do not need the rms-division in the original pearson's formula!)
					// ... transform to fourier domain
					// ... convolve with revsere-pattern FFT (= convolution in time domain)
					// ... go back to time domain
					// ... find position of abs max
 				  	for( int clumpIdx = 0; clumpIdx < clump; clumpIdx++ ) {
 				  		corrAbsMax[ clumpIdx ]		= 0f;
 				  		corrAbsMaxPos[ clumpIdx ]	= 0;
 				  		corrAbsMaxInv[ clumpIdx ]	= false;
 				  	}
					
					for( int ch = 0; ch < inChanNum; ch++ ) {
	 				  	for( int clumpIdx = 0; clumpIdx < clump; clumpIdx++ ) {
	 				  		Fourier.complexMult( ptrnFFTBuf[ clumpIdx ][ ch ], 0, inBuf[ ch ], 0, corrFFTBuf, 0, grainFFTLength + 2 );
if( checkNaN ) checkForNaN( corrFFTBuf, "D" );
							Fourier.realTransform( corrFFTBuf, grainFFTLength, Fourier.INVERSE );
if( checkNaN ) checkForNaN( corrFFTBuf, "E" ); // XXX inf at 0 !!!
						
						    f2 = 0.0f;
						    inv = false;
						    maxPos = inOff;
						    for( int i = 0; i < chunkLen; i++ ) {
						    	f1 = corrFFTBuf[ i ];
						    	if( f1 < 0f ) {
						    		if( -f1 > f2 ) {
						    			f2 = -f1;
						    			inv = true;
						    			maxPos = inOff + i;
						    		}
						    	} else {
						    		if( f1 > f2 ) {
						    			f2 = f1;
						    			inv = false;
						    			maxPos = inOff + i;
						    		}
						    	}
						    }
						    corrAbsMaxs[ clumpIdx ][ ch ]		= f2;
						    corrAbsMaxPoss[ clumpIdx ][ ch ]	= maxPos;
						    corrAbsMaxInvs[ clumpIdx ][ ch ]	= inv;
	 				  	}
//						progOff	+= chunkLen * progDelta; // ptrnRead[ clumpIdx ];
	 				  	progOff = progOff2 + progDelta * framesRead2 / inChanNum;
						// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}

//System.err.println( "  chanType" );
					switch( chanType ) {
					case CHAN_MIN:
						for( int clumpIdx = 0; clumpIdx < clump; clumpIdx++ ) {
							f1 = corrAbsMaxs[ clumpIdx ][ 0 ];
							idx = 0;
							for( int ch = 1; ch < inChanNum; ch++ ) {
								f2 = corrAbsMaxs[ clumpIdx ][ ch ];
								if( f2 < f1 ) {
									f1 = f2;
									idx = ch;
								}
							}
							corrAbsMax[ clumpIdx ]		= corrAbsMaxs[ clumpIdx ][ idx ];
							corrAbsMaxPos[ clumpIdx ]	= corrAbsMaxPoss[ clumpIdx ][ idx ];
							corrAbsMaxInv[ clumpIdx ]	= corrAbsMaxInvs[ clumpIdx ][ idx ];
						}
						break;
					case CHAN_MAX:
						for( int clumpIdx = 0; clumpIdx < clump; clumpIdx++ ) {
							f1 = corrAbsMaxs[ clumpIdx ][ 0 ];
							idx = 0;
							for( int ch = 1; ch < inChanNum; ch++ ) {
								f2 = corrAbsMaxs[ clumpIdx ][ ch ];
								if( f2 > f1 ) {
									f1 = f2;
									idx = ch;
								}
							}
							corrAbsMax[ clumpIdx ]		= corrAbsMaxs[ clumpIdx ][ idx ];
							corrAbsMaxPos[ clumpIdx ]	= corrAbsMaxPoss[ clumpIdx ][ idx ];
							corrAbsMaxInv[ clumpIdx ]	= corrAbsMaxInvs[ clumpIdx ][ idx ];
						}
						break;
//					case CHAN_AVG:
//						break;
					default:
						assert false : chanType;
						break;
					}
					
					for( int clumpIdx = 0; clumpIdx < clump; clumpIdx++ ) {
						if( corrAbsMax[ clumpIdx ] > totalCorrAbsMax[ clumpIdx ]) {
							totalCorrAbsMax	[ clumpIdx ]	= corrAbsMax[ clumpIdx ];
							totalCorrAbsMaxPos[ clumpIdx ]	= (long) (corrAbsMaxPos[ clumpIdx ] * timeScale + 0.5);
							totalCorrAbsMaxInv[ clumpIdx ]	= corrAbsMaxInv[ clumpIdx ];
							totalCorrAbsMaxRMS[ clumpIdx ]	= corrAbsMaxRMS;
						}
					}
					
//					n			 = framesRead2;
					framesRead2  = inOff + chunkLen;
					inOff		 = Math.min( inOff + grainLengthH, inLength );	// i.e. overlap
//					progOff     += inOff - n;
//					progOff		+= chunkLen;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // readInput
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
//				fftDone[ fftIdx ] = true;

//				progOff2 = progOff + (inLength * ptrnDelta);
				progOff = progOff2 + (inLength * progDelta); // ??? korekt

				/////////////////// plot
				
if( verbose ) System.err.println( "plot" );

	 			for( int clumpIdx = 0; threadRunning && (clumpIdx < clump); clumpIdx++ ) {
					// adjust gain
					f1 = Math.min( maxBoost, totalCorrAbsMaxRMS[ clumpIdx ] / ptrnRMS[ clumpIdx ]) *
							(totalCorrAbsMaxInv[ clumpIdx ] ? -1 : 1);
					Util.mult( ptrnBuf[ clumpIdx ], 0, grainLength, f1 );
					
					// zero padding output file
					if( framesWritten < totalCorrAbsMaxPos[ clumpIdx ]) {
						
if( verbose ) System.err.println( "clumpIdx = " + clumpIdx + " padding from " + framesWritten + " to " + totalCorrAbsMaxPos[ clumpIdx ]);
						
						Util.clear( inBuf );
						tmpF.seekFrame( framesWritten );
						while( threadRunning && (framesWritten < totalCorrAbsMaxPos[ clumpIdx ])) {
							chunkLen = (int) Math.min( inBufSize, totalCorrAbsMaxPos[ clumpIdx ] - framesWritten );
							tmpF.writeFrames( inBuf, 0, chunkLen );
							framesWritten += chunkLen;
//							progOff += chunkLen;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						}
					// .... check running ....
						if( !threadRunning ) break topLevel;
					} else {
if( verbose ) System.err.println( "clumpIdx = " + clumpIdx + " jumping to " + totalCorrAbsMaxPos[ clumpIdx ]);
						tmpF.seekFrame( totalCorrAbsMaxPos[ clumpIdx ]);
					}
					
					// mix with previous content
					chunkLen = (int) Math.min( framesWritten - totalCorrAbsMaxPos[ clumpIdx ], grainLength );
					if( chunkLen > 0 ) {
if( verbose ) System.err.println( "...mixing " + chunkLen + " frames" );
						tmpF.readFrames( inBuf, 0, chunkLen );
						Util.add( inBuf, 0, ptrnBuf[ clumpIdx ], 0, chunkLen );
						tmpF.seekFrame( totalCorrAbsMaxPos[ clumpIdx ]);
					}
					
if( verbose ) System.err.println( "...writing " + grainLength + " frames" );
					tmpF.writeFrames( ptrnBuf[ clumpIdx ], 0, grainLength );
					chunkLen = (int) Math.max( 0, totalCorrAbsMaxPos[ clumpIdx ] + grainLength - framesWritten );
					framesWritten += chunkLen;
					progOff += chunkLen;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // for clumpIdx
 			// .... check running ....
				if( !threadRunning ) break topLevel;
			}
// ------------------------------------ END ptrnLoop ------------------------------------
			
//			progOff = ptrnLength + inLength + outLength;
//			progOff = (ptrnLength << 4) + (outLength * 2);
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );

if( verbose ) System.err.println( "zero pad" );

//			for( int i = 0; i < numFFTs; i++ ) {
//				if( fftF[ i ] != null ) {
//					deleteTempFile( fftF[ i ]);
//					fftF[ i ] = null;
//				}
//			}

			// zero padding output file
			Util.clear( inBuf );
			tmpF.seekFrame( framesWritten );
			while( threadRunning && (framesWritten < outLength) ) {
				chunkLen = (int) Math.min( inBufSize, outLength - framesWritten );
				tmpF.writeFrames( inBuf, 0, chunkLen );
				framesWritten += chunkLen;
				progOff += chunkLen;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
if( verbose ) System.err.println( "calc max" );

			// calc max amp
			tmpF.seekFrame( 0L );
			for( framesRead = 0; threadRunning && (framesRead < outLength); ) {
				chunkLen = (int) Math.min( inBufSize, outLength - framesRead );
				tmpF.readFrames( inBuf, 0, chunkLen );
				maxAmp = Math.max( maxAmp, Util.maxAbs( inBuf, 0, chunkLen ));
				framesRead += chunkLen;
				progOff += chunkLen;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			ptrnF.close();
			ptrnF		= null;
			inF.close();
			inF			= null;

			// adjust gain
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
			} else {
				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}

if( verbose ) System.err.println( "norm" );

			normalizeAudioFile( tmpF, outF, inBuf, gain, 1.0f );
			deleteTempFile( tmpF );

		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;

			// inform about clipping/ low level
			maxAmp		*= gain;
//			handleClipping( maxAmp );

		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) inF.cleanUp();
		if( outF != null ) outF.cleanUp();
		if( ptrnF != null ) ptrnF.cleanUp();
	} // process()

// -------- private Methoden --------
	
	private static void checkForNaN( float[] a, String label )
	{
		for( int i = 0; i < a.length; i++ ) {
			if( Float.isNaN( a[ i ])) {
				System.err.println( "!!!!! NaN (" + label + ") at " + i );
				return;
			}
			if( Float.isInfinite( a[ i ])) {
				System.err.println( "!!!!! inf (" + label + ") at " + i );
				return;
			}
		}
	}

//	protected void reflectPropertyChanges()
//	{
//		super.reflectPropertyChanges();
//	
//		Component c;
//		
//		c = gui.getItemObj( GG_PLOTQUANTAMOUNT );
//		if( c != null ) {
//			c.setEnabled( pr.bool[ PR_PLOTQUANT ]);
//		}
//		c = gui.getItemObj( GG_PLOTNUM );
//		if( c != null ) {
//			c.setEnabled( pr.bool[ PR_PLOTMAX ]);
//		}
//		c = gui.getItemObj( GG_PLOTCHANGAIN );
//		if( c != null ) {
//			c.setEnabled( pr.intg[ PR_TRIGSOURCE ] != SRC_SUM );
//		}
//	}
}
// class SedimentDlg
