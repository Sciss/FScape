/*
 *  FreqModDlg.java
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
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.IOUtil;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  Processing module for frequency modulation
 *	of a sound file using high quality bandlimited
 *	resampling.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class FreqModDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_MODFILE			= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_QUALITY			= 3;
	private static final int PR_SOURCE			= 4;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_OSCFREQ			= 1;
	private static final int PR_RATEMODDEPTH	= 2;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_MODFILE			= "ModFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_QUALITY			= "Quality";
	private static final String PRN_SOURCE			= "Source";
	private static final String PRN_OSCFREQ			= "OscFreq";
	private static final String PRN_RATEMODDEPTH	= "RateModDepth";

	private static final int QUAL_MEDIUM			= 0;	// PR_QUALITY
	private static final int QUAL_GOOD				= 1;
	private static final int QUAL_VERYGOOD			= 2;

	private static final String[] QUAL_NAMES		= { "Medium", "Good", "Very good" };

	private static final int SRC_FILE				= 0;	// PR_SOURCE
	private static final int SRC_SINE				= 1;
	private static final int SRC_TRI				= 2;

	private static final String[] SRC_NAMES		= { "Soundfile", "Sine", "Triangle" };

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_MODFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, QUAL_GOOD, SRC_SINE };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_QUALITY,
													PRN_SOURCE };
	private static final Param	prPara[]		= { null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_OSCFREQ, PRN_RATEMODDEPTH };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_MODFILE			= GG_OFF_PATHFIELD	+ PR_MODFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_QUALITY			= GG_OFF_CHOICE		+ PR_QUALITY;
	private static final int GG_SOURCE			= GG_OFF_CHOICE		+ PR_SOURCE;
	private static final int GG_OSCFREQ			= GG_OFF_PARAMFIELD	+ PR_OSCFREQ;
	private static final int GG_RATEMODDEPTH	= GG_OFF_PARAMFIELD	+ PR_RATEMODDEPTH;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public FreqModDlg()
	{
		super( "Frequency Modulation" );
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
			static_pr.para[ PR_GAIN ]				= new Param(    0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_OSCFREQ ]			= new Param( 2000.0, Param.ABS_HZ );
			static_pr.para[ PR_RATEMODDEPTH ]		= new Param(    3.0, Param.OFFSET_SEMITONES );
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

		PathField			ggInputFile, ggOutputFile, ggModFile;
		JComboBox			ggQuality, ggSource;
		ParamField			ggRate, ggRateModDepth;
		PathField[]			ggInputs;
		ParamSpace[]		spcRateModDepth;
		Component[]			ggGain;
		int					i;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_SOURCE:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
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
										 "Select carrier file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Carrier input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggModFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select modulator file" );
		ggModFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Modulator input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggModFile, GG_MODFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
//		ggInputs[ 1 ]	= ggModFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0FM$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
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
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Modulation", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggSource		= new JComboBox();
		for( i = 0; i < SRC_NAMES.length; i++ ) {
			ggSource.addItem( SRC_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Source", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggSource, GG_SOURCE, il );

		ggQuality		= new JComboBox();
		for( i = 0; i < QUAL_NAMES.length; i++ ) {
			ggQuality.addItem( QUAL_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Quality", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggQuality, GG_QUALITY, il );
		
//		spcRate				= new ParamSpace[ 4 ];
//		spcRate[0]			= new ParamSpace(    689.1,   768000.0, 0.1,   Param.ABS_HZ );
//		spcRate[1]			= Constants.spaces[ Constants.offsetSemitonesSpace ];
//		spcRate[2]			= Constants.spaces[ Constants.offsetFreqSpace ];
//		spcRate[3]			= new ParamSpace( -96000.0,    96000.0, 0.1,   Param.OFFSET_HZ );
		spcRateModDepth		= new ParamSpace[ 3 ];
		spcRateModDepth[0]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
		spcRateModDepth[1]	= Constants.spaces[ Constants.offsetFreqSpace ];
		spcRateModDepth[2]	= Constants.spaces[ Constants.offsetHzSpace ];

		ggRateModDepth	= new ParamField( spcRateModDepth );
//		ggRateModDepth.setReference( ggRate );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mod. depth", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggRateModDepth, GG_RATEMODDEPTH, null );

		ggRate			= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Osc. freq.", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggRate, GG_OSCFREQ, null );

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
		int					i, j, k;
		int					len, chunkLength;
		long				progOff, progLen;
		boolean				finished;
		double				d1, d2;
		
		// io
		AudioFile			inF				= null;
		AudioFile			modF			= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			modStream		= null;
		AudioFileDescr			outStream		= null;
		FloatFile[]			floatF			= null;
		File[]				tempFile		= null;
		float[][]			inBuf			= null;
		float[][]			outBuf			= null;
		float[]				convBuf1;

		// Synthesize
		float				gain			= 1.0f;								// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		int					ch;

		// Smp Init
		int					inLength, inChanNum, modChanNum, outChanNum;
		int					modLength		= 0;
		int					framesRead, framesRead2, framesWritten;

		double				inRate, inPhase;
		double				minRsmpFactor;
		double				dOff, inputIncr;
		int					outTransLen, factorOff;
		int					overlapSize, inputStep, inBufSize, outBufSize;
		int					offStart;
		int					fltSmpPerCrossing, fltLen, fltCrossings;
		float[]				flt, fltD;
		double[]			factor;
		float[][]			filter;
		float				fltGain, fltRolloff, fltWin;
		double				fltIncr;
		double				oscFreq			= 1.0;
		Param				rateBase, rateDepth;
		
		float				maxAmp			= 0.0f;

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
			modChanNum		= 1;

			switch( pr.intg[ PR_SOURCE ]) {
			case SRC_FILE:
				modF			= AudioFile.openAsRead( new File( pr.text[ PR_MODFILE ]));
				modStream		= modF.getDescr();
				modChanNum		= modStream.channels;
				modLength		= (int) modStream.length;
				outChanNum		= Math.max( modChanNum, inChanNum );
				// this helps to prevent errors from empty files!
				if( modLength * modChanNum < 1 ) throw new EOFException( ERR_EMPTY );
			// .... check running ....
				if( !threadRunning ) break topLevel;
				break;
			
			case SRC_SINE:
			case SRC_TRI:
				oscFreq			= pr.para[ PR_OSCFREQ ].val / inRate;
				break;
			}

			// initialize various stuff
			rateBase 			= new Param( inRate, Param.ABS_HZ );
			rateDepth			= pr.para[ PR_RATEMODDEPTH ];

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
//			if( !rateMod && ((rateBase.val / inRate) >= 0.99) ) fltRolloff = 0.95f;	// no aliasing
			
			fltLen				= (int) ((float) (fltSmpPerCrossing * fltCrossings) / fltRolloff + 0.5f);
			flt					= new float[ fltLen ];
//			fltD				= pr.bool[ PR_INTERPOLE ] ? new float[ fltLen ] : null;
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
			progLen				= (long) inLength*2;

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
			minRsmpFactor	= Math.min( minRsmpFactor,
										(Param.transform( rateDepth, Param.ABS_HZ, rateBase, null )).val );
			minRsmpFactor	= Math.min( minRsmpFactor,
										(Param.transform( new Param( -rateDepth.val, rateDepth.unit ),
										Param.ABS_HZ, rateBase, null )).val );
			minRsmpFactor /= inRate;
			fltIncr			= fltSmpPerCrossing * minRsmpFactor;

			overlapSize		= (int) ((double) fltLen / fltIncr) + 1;
			inBufSize		= (overlapSize << 1) + inputStep;
			outBufSize		= (int) ((double) inputStep / minRsmpFactor) + 1;		// +1 als ArrayBounds security fuer FreqMod

			inBuf			= new float[ inChanNum ][ inBufSize ];
			outBuf			= new float[ Math.max( modChanNum, outChanNum )][ outBufSize ];
			factor			= new double[ outBufSize ];
			Util.clear( inBuf );

// System.out.println( "minRsmpFactor "+minRsmpFactor+"; overlapSize "+overlapSize );

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
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], FloatFile.MODE_OUTPUT );
				}
				progLen	   += (long) inLength;	// (outLength unknown)
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== da resampling ====================----

			framesWritten	= 0;
			framesRead		= 0;	// re input
			framesRead2		= 0;	// re mod
			factorOff		= 0;
//			overlap			= overlapSize;
			offStart		= overlapSize;
			finished		= false;

//System.out.println( "overlap "+overlapSize+"; inputStep "+inputStep );
//System.out.println( "rsmp "+rsmpFactor+"; frames to go"+inLength );

			while( threadRunning && !finished ) {
				// ==================== read input chunk ====================
//				len			= Math.min( inLength - framesRead, inputStep + overlap );
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
//System.out.println( "zero "+(inBufSize-chunkLength)+" frames" );
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
// System.out.println( "read "+len+" => "+offStart+"; chunkLength "+chunkLength+"; inPhase "+inPhase );

				// ==================== read mod chunk ====================
				switch( pr.intg[ PR_SOURCE ]) {
				case SRC_FILE:
					i			= Math.min( modLength - framesRead2, outBufSize - factorOff );
					modF.readFrames( outBuf, 0, i );
					framesRead2 += i;
//					progOff	   += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				// .... check running ....
					if( !threadRunning ) break topLevel;
					
					convBuf1 = outBuf[ 0 ];		// XXX just take the left channel
					for( j = factorOff + i; j < outBufSize; ) {
						factor[ j++ ]	= 1.0;
					}
					for( j = factorOff, k = 0; k < i; j++, k++ ) {
						factor[ j ] = Param.transform( new Param( rateDepth.val * convBuf1[ k ], rateDepth.unit ),
													   Param.ABS_HZ, rateBase, null ).val / inRate;
					}
					break;

				case SRC_SINE:
					i	= outBufSize - factorOff;
					for( j = factorOff, k = 0; k < i; j++, k++ ) {
						d1			= Math.sin( (framesRead2++) * oscFreq );
						factor[ j ] = Param.transform( new Param( rateDepth.val * d1, rateDepth.unit ),
													   Param.ABS_HZ, rateBase, null ).val / inRate;
					}
					break;

				case SRC_TRI:
					throw new IOException( "Not yet implemented!" );
				}

//System.out.println( "read "+len+" frames" );
//System.out.println( "dOff "+dOff+" chunkLength "+chunkLength );

				// ---- FreqMod ----------------------------------------------------------------------

				dOff	= (double) overlapSize + inPhase;
				d1		= (double) chunkLength - overlapSize; // YYY
				d2		= dOff;

// double f99 = 9999.0;
 				for( outTransLen = 0; (d2 < d1) && (outTransLen < outBufSize); outTransLen++ ) {
// if( factor[ outTransLen ] < f99 ) {
// 	f99 = factor[ outTransLen ];
// }
					inputIncr	 = 1.0 / factor[ outTransLen ];
					d2			+= inputIncr;
				}
// System.out.println( "Min factor "+f99+"; dOff "+dOff+"; outTransLen "+outTransLen );

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
// System.out.println( "dOff now "+dOff );

				// ---- write output or temp ----
				if( floatF != null ) {
//System.out.println( "write "+outTransLen+" to temp" );
					for( ch = 0; ch < outChanNum; ch++ ) {
						floatF[ ch ].writeFloats( outBuf[ ch ], 0, outTransLen );
					}

				} else {
		
//System.out.println( "write "+outTransLen+" to out" );
					// adjust gain
					for( ch = 0; ch < outChanNum; ch++ ) {
						Util.mult( outBuf[ ch ], 0, outTransLen, gain );
					}
					outF.writeFrames( outBuf, 0, outTransLen );
				}
				framesWritten += outTransLen;

						
			// ---- FreqMod : dun ----------------------------------------------------------------------
					
				i		= (int) (dOff - overlapSize);
				j		= chunkLength - i;
				inPhase	= dOff % 1.0;
				offStart= j; // YYY

				// shift buffers
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch ], i, inBuf[ ch ], 0, j );
				}
				factorOff	= outBufSize - outTransLen;
				System.arraycopy( factor, outTransLen, factor, 0, factorOff );

//			// ---- FreqMod : dun ----------------------------------------------------------------------
//					
//				inPhase	= dOff - d1;		// offset next time
//				// shift buffers
//				for( ch = 0; ch < inChanNum; ch++ ) {
//					System.arraycopy( inBuf[ ch ], inputStep, inBuf[ ch ], 0, overlapSize << 1 );
//				}
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
			if( modF != null ) {
				modF.close();
				modF		= null;
				modStream	= null;
			}

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
			modStream	= null;
			inBuf		= null;
			outBuf		= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
		}
		if( modF != null ) {
			modF.cleanUp();
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
	 *	Daten resamplen; "bandlimited interpolation"
	 *
	 *	@param	srcOff		erlaubt verschiebung unterhalb sample-laenge!
	 *	@param	length		bzgl. dest!! src muss laenge 'length/factor' aufgerundet haben!!
	 *	@param	factor		dest-smpRate/src-smpRate
	 *	@param	filter		Dimension 0: flt (erstes createAntiAliasFilter Argument), 1: fltD;
	 *						2: fltSmpPerCrossing; 3: fltGain (createAAF result)
	 *
	 *	keine lineare Interpolation (wesentlich schneller!)
	 */
	protected void resample( float[] src, double srcOff, float[] dest, int destOff, int length,
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
		AudioFileDescr		stream	= null;

		ParamField		ggSlave;
		Param			ref;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

//			refInp	= stream;
			ref		= new Param( stream.rate, Param.ABS_HZ );
			ggSlave = (ParamField) gui.getItemObj( GG_RATEMODDEPTH );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
			
		} catch( IOException e1 ) {
//			refInp	= null;
		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_MODFILE );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_SOURCE ] == SRC_FILE );
		}
		c = gui.getItemObj( GG_OSCFREQ );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_SOURCE ] != SRC_FILE );
		}
	}
}
// class FreqModDlg
