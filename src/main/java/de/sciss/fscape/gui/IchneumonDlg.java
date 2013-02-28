/*
 *  IchneumonDlg.java
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
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Processing module that tracks zero
 *	crossings and applies variable resampling
 *	to wave sections around these crossings.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class IchneumonDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_QUALITY			= 3;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_MINRSMP			= 1;
	private static final int PR_MAXRSMP			= 2;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_QUALITY			= "Quality";
	private static final String PRN_MINRSMP			= "MinRsmp";
	private static final String PRN_MAXRSMP			= "MaxRsmp";

	private static final int QUAL_MEDIUM		= 0;	// PR_QUALITY
	private static final int QUAL_GOOD			= 1;
	private static final int QUAL_VERYGOOD		= 2;

	private static final String[] QUAL_NAMES		= { "Medium", "Good", "Very good" };

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, QUAL_GOOD };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_QUALITY };
	private static final Param	prPara[]		= { null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_MINRSMP, PRN_MAXRSMP };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_QUALITY			= GG_OFF_CHOICE		+ PR_QUALITY;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_MINRSMP			= GG_OFF_PARAMFIELD	+ PR_MINRSMP;
	private static final int GG_MAXRSMP			= GG_OFF_PARAMFIELD	+ PR_MAXRSMP;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public IchneumonDlg()
	{
		super( "Ichneumon" );
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
			static_pr.para[ PR_MINRSMP ]	= new Param(    1.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_MAXRSMP ]	= new Param(   24.0, Param.OFFSET_SEMITONES );
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

		PathField			ggInputFile, ggOutputFile;
		ParamField			ggMinRsmp, ggMaxRsmp;
		JComboBox			ggQuality;
		PathField[]			ggInputs;
		ParamSpace[]		spcRateModDepth;
		Component[]			ggGain;
		int					i;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

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
//		ggInputs[ 1 ]	= ggModFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Ich$E" );
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
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Radio Massacre", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcRateModDepth		= new ParamSpace[ 2 ];
		spcRateModDepth[0]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
		spcRateModDepth[1]	= Constants.spaces[ Constants.offsetFreqSpace ];
//		spcRateModDepth[2]	= Constants.spaces[ Constants.offsetHzSpace ];
		ggMinRsmp			= new ParamField( spcRateModDepth );
		con.weightx			= 0.1;
		con.gridwidth		= 1;
		gui.addLabel( new JLabel( "Min.rsmp", SwingConstants.RIGHT ));
		con.weightx			= 0.4;
		gui.addParamField( ggMinRsmp, GG_MINRSMP, null );

		ggQuality		= new JComboBox();
		for( i = 0; i < QUAL_NAMES.length; i++ ) {
			ggQuality.addItem( QUAL_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Quality", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggQuality, GG_QUALITY, null );

		ggMaxRsmp			= new ParamField( spcRateModDepth );
		con.weightx			= 0.1;
		con.gridwidth		= 1;
		gui.addLabel( new JLabel( "Max.rsmp", SwingConstants.RIGHT ));
		con.weightx			= 0.4;
		gui.addParamField( ggMaxRsmp, GG_MAXRSMP, null );
		
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
		int					ch, i, j, k;
		int					len, chunkLength;
		long				progOff, progLen;
		double				d1, d2, d3;
		float				f1, f2;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		FloatFile			zcFloatFile		= null;
		File				zcTempFile		= null;
		FloatFile[]			floatF			= null;
		File[]				tempFile		= null;
		float[][]			inBuf			= null;
		float[][]			outBuf			= null;
		float[]				convBuf1;

		// Smp Init
		int					inLength, inChanNum, outChanNum;
		int					framesRead, framesWritten, timeIndex;

		float				gain			= 1.0f;								// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		float				maxAmp			= 0.0f;

		double				inRate, inPhase;
		double				minRsmpFactor;
		double				dOff, inputIncr;
		int					outTransLen;
		int					overlapSize, inputStep, inBufSize, outBufSize;
		int					offStart;
		int					fltSmpPerCrossing, fltLen, fltCrossings;
		float[]				flt, fltD;
		double[]			factor;
		float[][]			filter;
		float				fltGain, fltRolloff, fltWin;
		double				fltIncr;
		
		float				oldGrad, newGrad;
		int[]				crossHist;
		int					crossCount, maxGrad;
		int[]				zcBuf;

		int					maxLoc, minLoc, zeroLoc;
		float				maxVal, minVal;

		float				minFactor, maxFactor;
		float[]				factorLookup;
		Param				rateBase;

		boolean				finished;

		PathField			ggOutput;


topLevel: try {

		// ---- open input, output; init ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			inRate			= inStream.rate;
			// this helps to prevent errors from empty files!
			if( inLength * inChanNum < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outChanNum		= inChanNum;
			rateBase 		= new Param( inRate, Param.ABS_HZ );

			fltSmpPerCrossing	= 4096;	// 2048 << quality;
			switch( pr.intg[ PR_QUALITY ]) {
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
				throw new IllegalArgumentException( String.valueOf( pr.intg[ PR_QUALITY ]));
			}
			
			fltLen				= (int) ((float) (fltSmpPerCrossing * fltCrossings) / fltRolloff + 0.5f);
			flt					= new float[ fltLen ];
			fltD				= null;
			fltGain				= Filter.createAntiAliasFilter( flt, fltD, fltLen, fltSmpPerCrossing, fltRolloff, fltWin );
			filter				= new float[ 3 ][];
			filter[ 0 ]			= flt;
			filter[ 1 ]			= fltD;
			filter[ 2 ]			= new float[ 2 ];
			filter[ 2 ][ 0 ]	= fltSmpPerCrossing;
			filter[ 2 ][ 1 ]	= fltGain;
			inputStep			= 32768 / inChanNum;
			inPhase				= 0.0;

			progOff				= 0;
			progLen				= (long) inLength*4;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// calc minimum rsmp.factor; used for overlap-size
			minRsmpFactor	= rateBase.val;
			minFactor		= (float) ((Param.transform( pr.para[ PR_MINRSMP ], Param.ABS_HZ, rateBase, null )).val / inRate);
			maxFactor		= (float) ((Param.transform( pr.para[ PR_MAXRSMP ], Param.ABS_HZ, rateBase, null )).val / inRate);
			minRsmpFactor	= Math.min( 1.0, Math.min( minFactor, maxFactor ));

			fltIncr			= fltSmpPerCrossing * minRsmpFactor;

			overlapSize		= (int) ((double) fltLen / fltIncr) + 1;
			inBufSize		= (overlapSize << 1) + inputStep;
			outBufSize		= (int) ((double) inputStep / minRsmpFactor) + 1;		// +1 als ArrayBounds security fuer FreqMod

			inBuf			= new float[ inChanNum ][ inBufSize ];
			outBuf			= new float[ outChanNum ][ outBufSize ];
			factor			= new double[ outBufSize ];

// System.out.println( "minRsmpFactor "+minRsmpFactor+"; overlapSize "+overlapSize+"; minFactor "+minFactor+"; maxFactor "+maxFactor );

			zcBuf			= new int[ 4 ]; // pre-zc-loc / zc-loc / post-zc-loc / factor-index
			zcTempFile		= IOUtil.createTempFile();
			zcFloatFile		= new FloatFile( zcTempFile, GenericFile.MODE_OUTPUT );

// System.out.println( "fltIncr "+fltIncr+"; overlapSize "+overlapSize+"; inBufSize "+inBufSize+"; outBufSize "+outBufSize );

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				tempFile	= new File[ outChanNum ];
				floatF		= new FloatFile[ outChanNum ];
				for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
					tempFile[ ch ]	= null;
					floatF[ ch ]	= null;
				}
				for( ch = 0; ch < outChanNum; ch++ ) {
					tempFile[ ch ]	= IOUtil.createTempFile();
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
				}
				progLen	   += (long) inLength;	// (outLength unknown)
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== da resampling ====================----

			framesRead		= 0;
			maxLoc			= 0;
			minLoc			= 0;
			maxVal			= 0.0f;
			minVal			= 0.0f;
			zeroLoc			= 0;
			newGrad			= 0.0f;
			maxGrad			= (int) inStream.rate / 50;
			crossHist		= new int[ maxGrad + 1 ];
			factorLookup	= new float[ maxGrad + 1 ];

			while( threadRunning && (framesRead < inLength) ) {
				for( ch = 0; ch < inChanNum; ch++ ) {
					inBuf[ch][0]=inBuf[ch][inBufSize-1];
				}
				// ==================== read input chunk ====================
				len			= Math.min( inLength - framesRead, inBufSize-1 );
				inF.readFrames( inBuf, 1, len );
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- Ichneumon ----------------------------------------------------------------------

				convBuf1 = inBuf[0];
				f2		 = convBuf1[0];
				for( i = 1; i <= len; i++ ) {		// < inBufSize
					f1 = f2;
					f2 = convBuf1[i];
					oldGrad = newGrad;
					newGrad = f2 - f1;
					
					if( (oldGrad <= 0f) && (newGrad > 0f) ) {		// local min
						minLoc = framesRead + i - 2;
						minVal = f1;
						if( maxLoc < zeroLoc ) {
							k = Math.max( 0, Math.min( maxGrad, (int) ((maxVal - minVal) * 2 * (minLoc - maxLoc)) ));
							crossHist[k]++;
							zcBuf[0] = maxLoc;
							zcBuf[1] = zeroLoc;
							zcBuf[2] = minLoc + 1; // ZZZ
							zcBuf[3] = k;
							zcFloatFile.writeInts( zcBuf );
						}

					} else if( (oldGrad > 0f) && (newGrad <= 0f) ) { // local max
						maxLoc = framesRead + i - 2;
						maxVal = f1;
						if( minLoc < zeroLoc ) {
							k = Math.max( 0, Math.min( maxGrad, (int) ((maxVal - minVal) * 2 * (maxLoc - minLoc)) ));
							crossHist[k]++;
							zcBuf[0] = minLoc;
							zcBuf[1] = zeroLoc;
							zcBuf[2] = maxLoc + 1; // ZZZ
							zcBuf[3] = k;
							zcFloatFile.writeInts( zcBuf );
						}
					}

					if( (f1 > 0f && f2 <= 0f) || (f1 <= 0f && f2 > 0f) ) {  // zero crossing
						zeroLoc = framesRead + i - 1;
//						System.out.println( "zc "+zeroLoc );
					}
				}

				framesRead += len;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			// factor lookup berechnen
			// es wird ein Histogramm der zc-Werte (Produkt aus Distanz lokales Min/Maximum und Amplitude min/max)
			// erstellt. entsprechend wird dem zc-Index ein Wert zwischen minFactor und maxFactor zugeordnet
			// ; d.h. bei geringem zc-Wert wird staerker resamplet als bei hohem zc-Wert
			
			crossCount = 0;
			for( i = 0; i < maxGrad; i++ ) {
				crossCount   += crossHist[i];
			}
			for( i = 0, j = 0; i <= maxGrad; i++ ) {
				f1 = (float) j / (float) crossCount;
				factorLookup[i] = f1 * maxFactor + (1.0f - f1) * minFactor;
				j += crossHist[i];
			}
//			Debug.view( factorLookup, "factorLookUp" );
			
			// zero crossing file erhaelt einen terminierenden eintrag
			zcBuf[0] = inLength;
			zcBuf[1] = inLength+1;
			zcBuf[2] = inLength+2;
			zcBuf[3] = 0;
			zcFloatFile.writeInts( zcBuf );

		// ============= PASS TWO ================

//			factorOff		= 0;
			offStart		= overlapSize;
			finished		= false;
			framesRead		= 0;
			framesWritten	= 0;
			timeIndex		= -overlapSize;
			
			inF.seekFrame( 0 );
			zcFloatFile.seekFloat( 0 );
			zcFloatFile.readInts( zcBuf );

			while( threadRunning && !finished ) {
				// ==================== read input chunk ====================
				len			= Math.min( inLength - framesRead, inBufSize - offStart );
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
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = chunkLength; i < inBufSize; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
					chunkLength += overlapSize;		// YYY
					if( chunkLength <= inBufSize ) {
						finished = true;
					} else {
						chunkLength = inBufSize;
					}
				}

				// ==================== read mod chunk ====================
//				i	= outBufSize - factorOff;
//				for( j = factorOff, k = 0; k < i; j++, k++ ) {
//					d1			= 1.5; // XXX
//					factor[ j ] = d1;
//				}

				// ---- FreqMod ----------------------------------------------------------------------

				dOff	= (double) overlapSize + inPhase;
				d1		= (double) chunkLength - overlapSize; // YYY
				d2		= dOff;
				d3		= d2 + (double) timeIndex;

 				for( outTransLen = 0; (d2 < d1) && (outTransLen < outBufSize); outTransLen++ ) {
					while( zcBuf[2] < d3 ) {	// ggf. naechste zc koordinaten lesen
						zcFloatFile.readInts( zcBuf );
					}
					if( (double) zcBuf[0] > d3 ) {  // zwischen zwei zc's
						factor[outTransLen] = 1.0f;
					} else if( (double) zcBuf[1] > d3 ) {  // kurz vor zc
						f1			= factorLookup[ zcBuf[3] ] - 1.0f;
						f2			= ((float) d3 - (float) zcBuf[0]) / (float) (zcBuf[1] - zcBuf[0]);
						factor[outTransLen]   = f2 * f1 + 1.0f;
					} else {  // kurz nach zc
						f1			= factorLookup[ zcBuf[3] ] - 1.0f;
						f2			= ((float) zcBuf[2] - (float) d3) / (float) (zcBuf[2] - zcBuf[1]);
						factor[outTransLen]   = f2 * f1 + 1.0f;
					}
					inputIncr = 1.0 / factor[outTransLen];
// System.out.println( inputIncr );
					d2			+= inputIncr;
					d3			+= inputIncr;
				}

// System.out.println( "dOff "+dOff+"; d1 "+d1+"; d2 "+d2+"; outTransLen "+outTransLen );

				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					resample( inBuf[ ch % inChanNum ], dOff, convBuf1, 0, outTransLen, factor, filter );

					// measure max. gain
					for( j = 0; j < outTransLen; j++ ) {
						if( Math.abs( convBuf1[ j ]) > maxAmp ) {
							maxAmp = Math.abs( convBuf1[ j ]);
						}
					}
				}
					
				dOff = d2;
				progOff += len;

				// ---- write output or temp ----
				if( floatF != null ) {
					for( ch = 0; ch < outChanNum; ch++ ) {
						floatF[ ch ].writeFloats( outBuf[ ch ], 0, outTransLen );
					}
				} else {
					// adjust gain
					for( ch = 0; ch < outChanNum; ch++ ) {
						Util.mult( outBuf[ ch ], 0, outTransLen, gain );
					}
					outF.writeFrames( outBuf, 0, outTransLen );
				}
				framesWritten += outTransLen;
						
			// ---- FreqMod : dun ----------------------------------------------------------------------
				i			= (int) (dOff - overlapSize);
				j			= chunkLength - i;
				inPhase		= dOff % 1.0;
				offStart	= j; // YYY
				timeIndex  += i;
				// shift buffers
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch ], i, inBuf[ ch ], 0, j );
				}
//				factorOff	= outBufSize - outTransLen;
//				System.arraycopy( factor, outTransLen, factor, 0, factorOff );

			// .... progress ....
				progOff += len;
				setProgression( (float) progOff / (float) progLen );		
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----	
			inF.close();
			inF			= null;
			inStream	= null;

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {

				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / (double) maxAmp, Param.ABS_AMP ), null )).val;

				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				for( ch = 0; ch < outChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}

			outF.close();
			outF = null;

			zcFloatFile.cleanUp();
			zcFloatFile = null;
			zcTempFile.delete();

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
		if( zcFloatFile != null ) {
			zcFloatFile.cleanUp();
			zcTempFile.delete();
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
	 *	Daten resamplen; "bandlimited interpolation"
	 *
	 *	@param	srcOff		erlaubt verschiebung unterhalb sample-laenge!
	 *	@param	length		bzgl. dest!! src muss laenge 'length/factor' aufgerundet haben!!
	 *	@param	factor		dest-smpRate/src-smpRate
	 *	@param	filter		Dimension 0: flt (erstes createAntiAliasFilter Argument), 1: fltD;
	 *						2: fltSmpPerCrossing; 3: fltGain (createAAF result)
	 */
	protected void resample( float[] src, double srcOff, float[] dest, int destOffStart, int length,
							 double[] factor, float[][] filter )
	{
		int		i, fltOffI, srcOffI;
		double	q, val, f, fltIncr, fltOff;
		double	phase				= srcOff;

		float[]	flt					= filter[ 0 ];
//		float	fltD[]				= filter[ 1 ];
		double	fltSmpPerCrossing	= (double) filter[ 2 ][ 0 ];
		double	gain				= (double) filter[ 2 ][ 1 ];
		double  rsmpGain;
		int		fltLen				= flt.length;
		int		srcLen				= src.length;

// System.out.println( "Rsmp "+srcOff+" ["+srcLen+"] => "+destOff+" +"+length+" ["+dest.length+"]" );
		int destOff = destOffStart;

		for( i = 0; i < length; i++, phase += 1.0 / f ) {
			f		= factor[ i ];
			if( f < 1.0 ) {
				fltIncr	= fltSmpPerCrossing * f;
				rsmpGain= gain;
			} else {
				fltIncr	= fltSmpPerCrossing;
				rsmpGain= gain / f;
			}
			
			q		= phase % 1.0;
			val		= 0.0;

			srcOffI	= (int) phase;
			fltOff	= q * fltIncr + 0.5f;	// wenn wir schon keine interpolation mehr benutzen...
			fltOffI	= (int) fltOff;

			while( (fltOffI < fltLen) && (srcOffI >= 0) ) {
				val	   += (double) src[ srcOffI ] * flt[ fltOffI ];
				srcOffI--;
				fltOff += fltIncr;
				fltOffI	= (int) fltOff;
			}
			srcOffI	= (int) phase + 1;
			fltOff	= (1.0 - q) * fltIncr;
			fltOffI	= (int) fltOff;

			while( (fltOffI < fltLen) && (srcOffI < srcLen) ) {
				val	   += (double) src[ srcOffI ] * flt[ fltOffI ];
				srcOffI++;
				fltOff += fltIncr;
				fltOffI	= (int) fltOff;
			}
	
			dest[ destOff++ ] = (float) (val * rsmpGain);
		}
	}

	/**
	 *	Neues Inputfile setzen
	 */
	protected void setInput( String fname )
	{
		AudioFile		f		= null;
		AudioFileDescr	stream	= null;

		ParamField		ggSlave;
		Param			ref;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

			ref		= new Param( stream.rate, Param.ABS_HZ );
			ggSlave = (ParamField) gui.getItemObj( GG_MINRSMP );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
			ggSlave = (ParamField) gui.getItemObj( GG_MAXRSMP );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}

		} catch( IOException e1 ) {}
	}
}
// class IchneumonDlg
