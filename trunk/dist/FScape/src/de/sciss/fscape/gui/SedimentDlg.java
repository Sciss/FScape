/*
 *  SedimentDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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

/**
 *	Processing module for replacing occurrences of
 *	one sound by another. The idea is to correlate
 *	an input sound by an "icon" sound and whenever
 *	the correlation exceeds a threshold, a "replacement"
 *	sound is plotted to the output file. Never really worked.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 17-Jun-07
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
		
	private static final int	CHAN_MIN			= 0;
	private static final int	CHAN_MAX			= 1;

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_PATTERNFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, CHAN_MAX };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_CHANTYPE };
	private static final Param	prPara[]		= { null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_WINSIZE, PRN_MAXBOOST, PRN_TIMESCALE, PRN_MINGRAINLEN, PRN_MAXGRAINLEN };

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

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "Input + pattern must share\nsame # of channels!";

	// debuggingdorfer
	private static final boolean checkNaN = false;
	private static final boolean verbose = false;

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
			static_pr.para[ PR_GAIN ]			= new Param(    0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_WINSIZE ]		= new Param(   20.0, Param.FACTOR_TIME );
			static_pr.para[ PR_MAXBOOST ]		= new Param(   20.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_TIMESCALE ]		= new Param(  100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_MINGRAINLEN ]	= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_MAXGRAINLEN ]	= new Param( 1000.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
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
		ParamField			ggWinSize, ggMaxBoost, ggTimeScale, ggMinGrainLen, ggMaxGrainLen;
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
		gui.addLabel( new JLabel( "Control input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggPtrnFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select matching pattern file" );
		ggPtrnFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Pattern input", JLabel.RIGHT ));
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
		gui.addLabel( new JLabel( "Plot output", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", JLabel.RIGHT ));
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
		gui.addLabel( new JLabel( "Time scale", JLabel.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggTimeScale, GG_TIMESCALE, null );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.0;
		gui.addLabel( new JLabel( "" ));

		spcGrain		= new ParamSpace[ 2 ];
		spcGrain[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcGrain[1]		= Constants.spaces[ Constants.absBeatsSpace ];

		ggMinGrainLen	= new ParamField( spcGrain );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min. grain length", JLabel.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMinGrainLen, GG_MINGRAINLEN, null );


		ggMaxBoost		= new ParamField( new ParamSpace[] { Constants.spaces[ Constants.ratioAmpSpace ], Constants.spaces[ Constants.decibelAmpSpace ]});
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max boost", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMaxBoost, GG_MAXBOOST, null );

		ggMaxGrainLen	= new ParamField( spcGrain );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max. grain length", JLabel.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMaxGrainLen, GG_MAXGRAINLEN, null );

		ggWinSize		= new ParamField( Constants.spaces[ Constants.ratioTimeSpace ] );
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Win size", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggWinSize, GG_WINSIZE, null );

		ggChanType		= new JComboBox();
		ggChanType.addItem( "Minimum" );
		ggChanType.addItem( "Maximum" );
		con.weightx		= 0.1;
		con.gridwidth	= 3;
		gui.addLabel( new JLabel( "Multichannel correlation", JLabel.RIGHT ));
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
		final float[][]			inBuf, ptrnBuf, ptrnFFTBuf;
		float[]					convBuf1;
		float[]					win;

		final int				maxGrainLength, maxFFTLength, inBufSize;
		int						grainLength, grainLengthH, grainFFTLength, winLen, winLenH, len, len2;
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

		float					totalCorrAbsMax, corrAbsMax, totalCorrAbsMaxRMS, corrAbsMaxRMS, ptrnRMS;
		long					totalCorrAbsMaxPos, corrAbsMaxPos;
		boolean					totalCorrAbsMaxInv, corrAbsMaxInv;
		final float[]			corrAbsMaxs;
		final long[]			corrAbsMaxPoss;
		final boolean[]			corrAbsMaxInvs;
		int						idx;
		boolean					inv;
		long					maxPos;
		
		final Random			rnd				= new Random();

topLevel: try {

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

			// ptrn input
			ptrnF			= AudioFile.openAsRead( new File( pr.text[ PR_PATTERNFILE ]));
			ptrnStream		= ptrnF.getDescr();
			ptrnChanNum		= ptrnStream.channels;
			ptrnLength		= (int) ptrnStream.length;
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
			
			corrAbsMaxs			= new float[ inChanNum ];
			corrAbsMaxPoss		= new long[ inChanNum ];
			corrAbsMaxInvs		= new boolean[ inChanNum ];
			
			maxGrainLength		= (int) (AudioFileDescr.millisToSamples( inStream, maxGrainLen ) + 0.5);
			maxFFTLength		= Util.nextPowerOfTwo( maxGrainLength + maxGrainLength - 1 );
			
			inBufSize			= Math.max( 8192, maxFFTLength + 2 );
			inBuf				= new float[ inChanNum ][ inBufSize ];
			ptrnFFTBuf			= new float[ inChanNum ][ maxFFTLength + 2 ];
			ptrnBuf				= new float[ inChanNum ][ maxGrainLength ];
			
			outLength			= (long) (inLength * timeScale + 0.5) + maxGrainLength;
			
//			progLen				= ptrnLength + inLength + (outLength * 3);
			progLen				= (ptrnLength << 4) + (outLength * 3);
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
				winLen		= Math.max( 2, (int) (grainLength * winSizeFactor + 0.5) & ~1 );
				winLenH		= winLen >> 1;
				win			= Filter.createFullWindow( winLen, winType );
				len			= (int) Math.min( ptrnLength - framesRead, grainLength );
				ptrnF.readFrames( ptrnBuf, 0, len );
				framesRead += len;
				// zero padding
				if( len < grainLength ) {
					Util.clear( ptrnBuf, len, grainLength - len );
				}
				// apply window
				Util.mult( win, 0, ptrnBuf, 0, winLenH );
				Util.mult( win, winLenH, ptrnBuf, grainLength - winLenH, winLenH );
				
				// copy to fft buffer
				Util.copy( ptrnBuf, 0, ptrnFFTBuf, 0, len );
				Util.clear( ptrnFFTBuf, len, grainFFTLength - len );
				
				// reverse
				Util.reverse( ptrnFFTBuf, 0, grainLength );

				// remove DC
				// ... normalize RMS (so we do not need the rms-division in the original pearson's formula!)
				// ... and transform to fourier domain
				ptrnRMS = 0f;
				for( int ch = 0; ch < ptrnChanNum; ch++ ) {
					Util.removeDC( ptrnFFTBuf[ ch ], 0, grainLength );
					d1 = Math.sqrt( Filter.calcEnergy( ptrnFFTBuf[ ch ], 0, len ));		// i.e. sqrt( sum(y^2) )
// too small values produce inf values in the below Util.mult !!!
d1 = d1 * (1000 * len);

					if( d1 > 0.0 ) {						// (double) fftLength
						Util.mult( ptrnFFTBuf[ ch ], 0, len, (float) (1.0 / d1) );
					} else continue ptrnLoop;
					Fourier.realTransform( ptrnFFTBuf[ ch ], grainFFTLength, Fourier.FORWARD );
					ptrnRMS += (float) d1;

//for( int i = 0; i < grainFFTLength + 2; i++ ) {
//	if( Float.isNaN( ptrnFFTBuf[ ch ][ i ])) {
//		System.err.println( "isNaN ! ch = "+ch+"; i = "+i+"; framesRead = "+framesRead + "; len = "+len );
//	}
//}
				}

				progOff		+= (len << 4);
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

				/////////////////// read through input and calc best correlation

				totalCorrAbsMax		= 0f;
				totalCorrAbsMaxPos	= 0;
				totalCorrAbsMaxInv	= false;
				totalCorrAbsMaxRMS	= ptrnRMS;
				inOff				= 0;

//System.err.println( "readInput " + grainLength );
//System.gc();

readInput:		for( framesRead2 = 0; threadRunning && (framesRead2 < inLength); ) {

//try {
//Thread.sleep( 1 );
//} catch( InterruptedException e1 ) {}

					len2	= (int) Math.min( inLength - framesRead2, grainLength );
//System.err.println( "  len2 = " + len2 );
//System.err.println( "  framesRead2 = " + framesRead2 );
					inF.seekFrame( inOff );
					inF.readFrames( inBuf, 0, len2 );
					if( len2 < grainLength ) {
						Util.clear( inBuf, len2, grainLength - len2 );
					}
					// apply window
					Util.mult( win, 0, inBuf, 0, winLenH );
					Util.mult( win, winLenH, inBuf, grainLength - winLenH, winLenH );
					
					// remove DC
					// ... normalize RMS (so we do not need the rms-division in the original pearson's formula!)
					// ... transform to fourier domain
					// ... convolve with revsere-pattern FFT (= convolution in time domain)
					// ... go back to time domain
					// ... find position of abs max
					corrAbsMaxRMS	= 0f;
					corrAbsMax		= 0f;
					corrAbsMaxPos	= 0;
					corrAbsMaxInv	= false;
					
					for( int ch = 0; ch < inChanNum; ch++ ) {
//System.err.println( "  chan loop " + ch );
						convBuf1 = inBuf[ ch ];
						Util.removeDC( convBuf1, 0, len2 );
						d1 = Math.sqrt( Filter.calcEnergy( convBuf1, 0, len2 ));		// i.e. sqrt( sum(y^2) )
if( checkNaN ) checkForNaN( convBuf1, "A" );
// too small values produce inf values in the below Util.mult !!!
d1 = d1 * (1000 * len);
						if( d1 > 0.0 ) {
							Util.mult( convBuf1, 0, len2, (float) (1.0 / d1) );
if( checkNaN ) checkForNaN( convBuf1, "B" );
						} else {
							framesRead2  = inOff + len2;
							inOff		 = Math.min( inOff + grainLengthH, inLength );	// i.e. overlap
							continue readInput;
						}
						corrAbsMaxRMS += (float) d1;
						Fourier.realTransform( convBuf1, grainFFTLength, Fourier.FORWARD );
if( checkNaN ) checkForNaN( convBuf1, "C" );
						Fourier.complexMult( ptrnFFTBuf[ ch ], 0, convBuf1, 0, convBuf1, 0, grainFFTLength + 2 );
if( checkNaN ) checkForNaN( convBuf1, "D" );
						Fourier.realTransform( convBuf1, grainFFTLength, Fourier.INVERSE );
if( checkNaN ) checkForNaN( convBuf1, "E" ); // XXX inf at 0 !!!
						
						f2 = 0.0f;
						inv = false;
						maxPos = inOff;
						for( int i = 0; i < len2; i++ ) {
							f1 = convBuf1[ i ];
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
						corrAbsMaxs[ ch ] = f2;
						corrAbsMaxPoss[ ch ] = maxPos;
						corrAbsMaxInvs[ ch ] = inv;
					}

//System.err.println( "  chanType" );
					switch( chanType ) {
					case CHAN_MIN:
						f1 = corrAbsMaxs[ 0 ];
						idx = 0;
						for( int ch = 1; ch < inChanNum; ch++ ) {
							f2 = corrAbsMaxs[ ch ];
							if( f2 < f1 ) {
								f1 = f2;
								idx = ch;
							}
						}
						corrAbsMax		= corrAbsMaxs[ idx ];
						corrAbsMaxPos	= corrAbsMaxPoss[ idx ];
						corrAbsMaxInv	= corrAbsMaxInvs[ idx ];
						break;
					case CHAN_MAX:
						f1 = corrAbsMaxs[ 0 ];
						idx = 0;
						for( int ch = 1; ch < inChanNum; ch++ ) {
							f2 = corrAbsMaxs[ ch ];
							if( f2 > f1 ) {
								f1 = f2;
								idx = ch;
							}
						}
						corrAbsMax		= corrAbsMaxs[ idx ];
						corrAbsMaxPos	= corrAbsMaxPoss[ idx ];
						corrAbsMaxInv	= corrAbsMaxInvs[ idx ];
						break;
//					case CHAN_AVG:
//						break;
					default:
						assert false : chanType;
						break;
					}
					
					if( corrAbsMax > totalCorrAbsMax ) {
						totalCorrAbsMax		= corrAbsMax;
						totalCorrAbsMaxPos	= (long) (corrAbsMaxPos * timeScale + 0.5);
						totalCorrAbsMaxInv	= corrAbsMaxInv;
						totalCorrAbsMaxRMS	= corrAbsMaxRMS;
					}
					
//					n			 = framesRead2;
					framesRead2  = inOff + len2;
					inOff		 = Math.min( inOff + grainLengthH, inLength );	// i.e. overlap
//					progOff     += inOff - n;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // readInput
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
				/////////////////// plot
				
if( verbose ) System.err.println( "plot" );

				// adjust gain
				f1 = Math.min( maxBoost, totalCorrAbsMaxRMS / ptrnRMS ) * (totalCorrAbsMaxInv ? -1 : 1);
				Util.mult( ptrnBuf, 0, grainLength, f1 );
				
				// zero padding output file
				if( framesWritten < totalCorrAbsMaxPos ) {
					Util.clear( inBuf );
					tmpF.seekFrame( framesWritten );
					while( threadRunning && (framesWritten < totalCorrAbsMaxPos) ) {
						len = (int) Math.min( inBufSize, totalCorrAbsMaxPos - framesWritten );
						tmpF.writeFrames( inBuf, 0, len );
						framesWritten += len;
						progOff += len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;
				} else {
					tmpF.seekFrame( totalCorrAbsMaxPos );
				}
				
				// mix with previous content
				len = (int) Math.min( framesWritten - totalCorrAbsMaxPos, grainLength );
				if( len > 0 ) {
					tmpF.readFrames( inBuf, 0, len );
					Util.add( inBuf, 0, ptrnBuf, 0, len );
					tmpF.seekFrame( totalCorrAbsMaxPos );
				}
				
				tmpF.writeFrames( ptrnBuf, 0, grainLength );
				len = (int) Math.max( 0, totalCorrAbsMaxPos + grainLength - framesWritten );
				framesWritten += len;
				progOff += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // ptrnLoop
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			progOff = ptrnLength + inLength + outLength;
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );

if( verbose ) System.err.println( "zero pad" );

			// zero padding output file
			Util.clear( inBuf );
			tmpF.seekFrame( framesWritten );
			while( threadRunning && (framesWritten < outLength) ) {
				len = (int) Math.min( inBufSize, outLength - framesWritten );
				tmpF.writeFrames( inBuf, 0, len );
				framesWritten += len;
				progOff += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
if( verbose ) System.err.println( "calc max" );

			// calc max amp
			tmpF.seekFrame( 0L );
			for( framesRead = 0; threadRunning && (framesRead < outLength); ) {
				len = (int) Math.min( inBufSize, outLength - framesRead );
				tmpF.readFrames( inBuf, 0, len );
				maxAmp = Math.max( maxAmp, Util.maxAbs( inBuf, 0, len ));
				framesRead += len;
				progOff += len;
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
