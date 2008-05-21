/*
 *  ExciterDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Processing module for multiband waveshaping
 *	using cubic distortion.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ExciterDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_FILTERLEN		= 3;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_LOFREQ			= 1;
	private static final int PR_HIFREQ			= 2;
	private static final int PR_DRYMIX			= 3;
	private static final int PR_WETMIX			= 4;
	private static final int PR_ROLLOFF			= 5;
	private static final int PR_BANDSPEROCT		= 6;

//	private static final int FLT_SHORT			= 0;
//	private static final int FLT_MEDIUM			= 1;
	private static final int FLT_LONG			= 2;
//	private static final int FLT_VERYLONG		= 3;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_FILTERLEN		= "FilterLen";
	private static final String PRN_LOFREQ			= "LoFreq";
	private static final String PRN_HIFREQ			= "HiFreq";
	private static final String PRN_DRYMIX			= "DryMix";
	private static final String PRN_WETMIX			= "WetMix";
	private static final String PRN_ROLLOFF			= "RollOff";
	private static final String PRN_BANDSPEROCT		= "BandsPerOct";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, 0, FLT_LONG };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
													PRN_FILTERLEN };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_LOFREQ, PRN_HIFREQ, PRN_DRYMIX, PRN_WETMIX,
													PRN_ROLLOFF, PRN_BANDSPEROCT };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_FILTERLEN		= GG_OFF_CHOICE		+ PR_FILTERLEN;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_LOFREQ			= GG_OFF_PARAMFIELD	+ PR_LOFREQ;
	private static final int GG_HIFREQ			= GG_OFF_PARAMFIELD	+ PR_HIFREQ;
	private static final int GG_DRYMIX			= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
	private static final int GG_WETMIX			= GG_OFF_PARAMFIELD	+ PR_WETMIX;
	private static final int GG_ROLLOFF			= GG_OFF_PARAMFIELD	+ PR_ROLLOFF;
	private static final int GG_BANDSPEROCT		= GG_OFF_PARAMFIELD	+ PR_BANDSPEROCT;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ExciterDlg()
	{
		super( "Exciter" );
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
			static_pr.para[ PR_GAIN ]			= new Param(    0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_LOFREQ ]			= new Param(  400.0, Param.ABS_HZ );
			static_pr.para[ PR_HIFREQ ]			= new Param( 9000.0, Param.ABS_HZ );
			static_pr.para[ PR_DRYMIX ]			= new Param(  100.0, Param.FACTOR_AMP );
			static_pr.para[ PR_WETMIX ]			= new Param(   25.0, Param.FACTOR_AMP );
			static_pr.para[ PR_ROLLOFF ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_BANDSPEROCT ]	= new Param(   36.0, Param.NONE );
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

		PathField			ggInputFile, ggOutputFile;
		PathField[]			ggParent1;
		JComboBox			ggFltLen;
		ParamField			ggLoFreq, ggHiFreq, ggRollOff, ggBandsPerOct, ggDryMix, ggWetMix;
		ParamSpace[]		spcHiCut;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- I/O-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggParent1		= new PathField[ 1 ];
		ggParent1[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggParent1, "$D0$F0Exc$E" );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Post processing", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggLoFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Low CrossOver", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLoFreq, GG_LOFREQ, null );

		spcHiCut		= new ParamSpace[ 3 ];
		spcHiCut[0]		= Constants.spaces[ Constants.absHzSpace ];
		spcHiCut[1]		= Constants.spaces[ Constants.offsetHzSpace ];
		spcHiCut[2]		= Constants.spaces[ Constants.offsetSemitonesSpace ];

		ggRollOff		= new ParamField( spcHiCut );
		ggRollOff.setReference( ggLoFreq );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "RollOff", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggRollOff, GG_ROLLOFF, null );

		ggHiFreq		= new ParamField( spcHiCut );
		ggHiFreq.setReference( ggLoFreq );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "High CrossOver", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggHiFreq, GG_HIFREQ, null );

		ggBandsPerOct	= new ParamField( new ParamSpace( 1.0, 256.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Bands per Oct.", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggBandsPerOct, GG_BANDSPEROCT, null );

		ggFltLen		= new JComboBox();
		ggFltLen.addItem( "Short" );
		ggFltLen.addItem( "Medium" );
		ggFltLen.addItem( "Long" );
		ggFltLen.addItem( "Very long" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filter length", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggFltLen, GG_FILTERLEN, null );

		ggDryMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Dry mix", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggDryMix, GG_DRYMIX, null );

		con.gridwidth	= 2;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel() );
		ggWetMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		gui.addLabel( new JLabel( "Wet mix", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggWetMix, GG_WETMIX, null );

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
		int					i, j, k, m, len, off, ch, chunkLength, chunkLength2, band;
		long				progOff, progLen;
		double				d1, d2, d3, loFreq, hiFreq;
		float				f1, f2;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream;
		AudioFileDescr		outStream;
		FloatFile[]			floatF		= null;
		File				tempFile[]	= null;
		
		// buffers
		float[]				fftBuf1, fftBuf2, highpass;
		float[][]			fftBuf, fftBuf3;
		float[]				convBuf1, convBuf2;
		float[]				win;
		float[][]			inBuf;
		float[][][]			overBuf;
		float[][]			overBuf2;
//		float[][]			dcMem;
		int[]				peakLen;

		int					inChanNum, inLength, fftLength, inputLen;
		int					framesRead, framesWritten;

		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float				gain			= 1.0f;								 		// gain abs amp
		float				maxAmp			= 0.0f;
		float				dryGain, wetGain;
		boolean				hasDry;

		PathField			ggOutput;

		int					numPeriods;
		int					halfWinSize, fltLength, overLen, skip;
		float[]				crossFreqs, cosineFreqs;
		double				freqNorm, freqBase, cosineNorm, cosineBase, rollOff;

		int					numBands;

topLevel: try {

		// ---- open input, output ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength * inChanNum) < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- calculate filters ----

			loFreq				= Param.transform( pr.para[ PR_LOFREQ ], Param.ABS_HZ, null, null ).val;
			hiFreq				= Param.transform( pr.para[ PR_HIFREQ ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).val;
			rollOff				= Param.transform( pr.para[ PR_ROLLOFF ], Param.ABS_HZ, pr.para[ PR_LOFREQ ], null ).val;
			if( loFreq > rollOff ) {
				d1		= loFreq;
				loFreq	= rollOff;
				rollOff	= d1;
			}
			d2					= Math.pow( 2.0, 1.0 / pr.para[ PR_BANDSPEROCT ].val );
			dryGain				= (float) (pr.para[ PR_DRYMIX ].val / 100);
			wetGain				= (float) (pr.para[ PR_WETMIX ].val / 100);
			hasDry				= dryGain > 0.0f;

			numBands			= (int) (Math.log( hiFreq / loFreq ) / Math.log( d2 ));
			crossFreqs			= new float[ numBands+1 ];
			cosineFreqs			= new float[ numBands+1 ];
			

			d1					= loFreq;
			d3			    	= d1 * d2;

			for( i = 0; i <= numBands; i++ ) {
				crossFreqs[i]	= (float) d1;
				cosineFreqs[i]	= (float) (Math.sqrt( d3 * d1 ) - d1);
				d1			   	= d3;
				d3			    = d1 * d2;
			}
//			for( i = 0; i <= numBands; i++ ) {
//				System.out.println( i +" : cut "+crossFreqs[i]+"; roll "+cosineFreqs[i] );
//			}

			numPeriods		= 3 << pr.intg[ PR_FILTERLEN ];
			halfWinSize		= Math.max( 1, (int) ((double) numPeriods * inStream.rate / crossFreqs[0] + 0.5) );
			freqNorm		= Constants.PI2 / inStream.rate;
			cosineNorm		= 4.0 / (Math.PI*Math.PI);
			fltLength		= halfWinSize + halfWinSize;
			win				= Filter.createFullWindow( fltLength, Filter.WIN_BLACKMAN );

			j				= fltLength + fltLength - 1;
			for( fftLength = 2; fftLength < j; fftLength <<= 1 ) ;
			inputLen		= fftLength - fltLength + 1;
			overLen			= fftLength - inputLen;

// System.out.println( "halfWinSize "+halfWinSize+"; fltLength "+fltLength+"; fftLength "+fftLength+"; inputLen "+inputLen+"; overLen "+overLen+"; numBands "+numBands );

			fftBuf			= new float[ numBands ][ fftLength + 2 ];
			fftBuf1			= new float[ fftLength + 2 ];
			fftBuf2			= new float[ fftLength + 2 ];
			fftBuf3			= new float[ inChanNum ][ fftLength + 2 ];
			highpass		= new float[ fftLength + 2 ];
			inBuf			= new float[ inChanNum ][ inputLen + fltLength ];
			overBuf			= new float[ inChanNum ][ numBands ][ overLen ];
			overBuf2		= new float[ inChanNum ][ overLen ];
			peakLen			= new int[ numBands ];
			for( i = 0; i < inChanNum; i++ ) {
				Util.clear( overBuf[ i ]);
			}
			Util.clear( inBuf );

			// LP = +1.0 fc  -1.0 Zero
			// HP = +1.0 ¹/2 -1.0 fc
			// BP = +1.0 fc2 -1.0 fc1
			
		// ---- calculate impulse response of the bandpasses ----
			for( i = 0; i < numBands; i++ ) {
			
				convBuf1	= fftBuf[ i ];
			
				freqBase	= freqNorm * crossFreqs[ i+1 ];
				cosineBase	= freqNorm * cosineFreqs[ i+1 ];

				for( j = 1; j < halfWinSize; j++ ) {
					// sinc-filter
					d1							= (Math.sin( freqBase * j ) / (double) j);
					// raised cosine modulation
					d2							= cosineNorm * cosineBase * j * cosineBase * j;
					d1						   *= (Math.cos( cosineBase * j ) / (1.0 - d2));
					convBuf1[ halfWinSize + j ]	= (float) d1;
					convBuf1[ halfWinSize - j ]	= (float) d1;
				}
				convBuf1[ halfWinSize ] = (float) freqBase;

				freqBase	= freqNorm * crossFreqs[ i ];
				cosineBase	= freqNorm * cosineFreqs[ i ];

				for( j = 1; j < halfWinSize; j++ ) {
					d1							 = (Math.sin( freqBase * j ) / (double) j);
					// raised cosine modulation
					d2							 = cosineNorm * cosineBase * j * cosineBase * j;
					d1						    *= (Math.cos( cosineBase * j ) / (1.0 - d2));
					convBuf1[ halfWinSize + j ] -= (float) d1;
					convBuf1[ halfWinSize - j ] -= (float) d1;
				}
				convBuf1[ halfWinSize ] -= (float) freqBase;
				
				// zero padding
				for( j = fltLength; j < fftLength; j++ ) {
					convBuf1[ j ] = 0.0f;
				}
				// windowing
				Util.mult( win, 0, convBuf1, 0, fltLength );

				Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
				// normalize
				d1 = 0.0f;
				for( j = 0; j < convBuf1.length; ) {
					d2 = Math.sqrt( convBuf1[ j ]*convBuf1[ j++ ]+convBuf1[ j ]*convBuf1[ j++ ]);
					if( d2 > d1 ) {
						d1 = d2;
					}
				}
//				System.out.println( d1 );
				f1 = (float) (1.0 / Math.sqrt( d1 ));
				Util.mult( convBuf1, 0, convBuf1.length, f1 );
				
				// peakLen is the length of 1.5 periods of a band's frequency
				// it is the length used for peak scanning
				peakLen[ i ]	= Math.min( overLen, (int) (inStream.rate / crossFreqs[ i ] * 1.5f + 0.5f) );
			}

		// ---- calc highpass filter ----
			freqBase	= freqNorm * (inStream.rate/2);
			highpass[ halfWinSize ] = (float) freqBase;

			freqBase	= freqNorm * rollOff; // Math.sqrt( rollOff * loFreq ); // ((rollOff + loFreq) / 2 );	// ? XXX
			cosineBase	= freqNorm * (rollOff - loFreq);

			for( j = 1; j < halfWinSize; j++ ) {
				d1							 = (Math.sin( freqBase * j ) / (double) j);
				// raised cosine modulation
				d2							 = cosineNorm * cosineBase * j * cosineBase * j;
				d1						    *= (Math.cos( cosineBase * j ) / (1.0 - d2));
				highpass[ halfWinSize + j ] -= (float) d1;
				highpass[ halfWinSize - j ] -= (float) d1;
			}
			highpass[ halfWinSize ] -= (float) freqBase;
				
			// zero padding
			for( j = fltLength; j < fftLength; j++ ) {
				highpass[ j ] = 0.0f;
			}
			// windowing
			Util.mult( win, 0, highpass, 0, fltLength );

			Fourier.realTransform( highpass, fftLength, Fourier.FORWARD );
			// normalize
			d1 = 0.0f;
			for( j = 0; j < highpass.length; ) {
				d2 = Math.sqrt( highpass[ j ]*highpass[ j++ ]+highpass[ j ]*highpass[ j++ ]);
				if( d2 > d1 ) {
					d1 = d2;
				}
			}
			f1 = (float) (0.25 / Math.sqrt( d1 ));	// 1.0
			Util.mult( highpass, 0, highpass.length, f1 );


// for( i = 0; i < fftLength; i++ ) {
//	reBuf[ 0 ][ i ] = (float) Math.sqrt( fftBuf1[ i<<1 ]*fftBuf1[ i<<1 ]+fftBuf1[ (i<<1)+1 ]*fftBuf1[ (i<<1)+1 ]);
// }
// Debug.view( reBuf[ 0 ], "hilbert cmplx fft" );

			// DC block init
//			dcMem	= new float[ inChanNum ][ 2 ];
//			Util.clear( dcMem );

			progOff		= 0;
			progLen		= (long) inLength * (2 + inChanNum + inChanNum);

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
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], FloatFile.MODE_OUTPUT );
				}
				progLen	   += (long) inLength;
			} else {													// account for gain loss RealFFT => CmplxIFFT
				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
				dryGain	   *= gain;
				wetGain	   *= gain;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;
//			skip			= halfWinSize;
			skip			= halfWinSize;		// 1x bandpass filter, 2x complete highpass

			while( threadRunning && (framesWritten < inLength) ) {

				chunkLength = Math.min( inputLen, inLength - framesRead );
			// ---- read input chunk ----
				for( off = 0; threadRunning && (off < chunkLength); ) {
					len	= Math.min( 8192, chunkLength - off );
					inF.readFrames( inBuf, off + fltLength, len );
					framesRead	+= len;
					progOff		+= len;
					off			+= len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				chunkLength2 = Math.min( inputLen, inLength - framesWritten );
//				if( floatF == null ) {
//					for( ch = 0; ch < inChanNum; ch++ ) {
//						Util.mult( inBuf[ ch ], 0, chunkLength2, gain );
//					}
//				}

			// ---- channels loop -----------------------------------------------------------------------
				for( ch = 0; threadRunning && (ch < inChanNum); ch++ ) {

					convBuf1 = fftBuf3[ ch ];
					convBuf2 = inBuf[ ch ];
					Util.clear( convBuf1 );

					System.arraycopy( convBuf2, fltLength, fftBuf1, 0, chunkLength );
					for( i = chunkLength; i < fftLength; i++ ) {
						fftBuf1[ i ] = 0.0f;
					}
					Fourier.realTransform( fftBuf1, fftLength, Fourier.FORWARD );

				// ---- bands loop ----------------------------------------------------------------------
				
					for( band = 0; threadRunning && (band < numBands); band++ ) {
					
						Fourier.complexMult( fftBuf1, 0, fftBuf[ band ], 0, fftBuf2, 0, fftBuf1.length );
						Fourier.realTransform( fftBuf2, fftLength, Fourier.INVERSE );
						Util.add( overBuf[ ch ][ band ], 0, fftBuf2, 0, overLen );
						System.arraycopy( fftBuf2, inputLen, overBuf[ ch ][ band ], 0, overLen );
// 020220 begin
						// d1	= Math.sqrt( crossFreqs[ band ] * crossFreqs[ band+1 ]) * freqNorm;
						
						m = -1;
						f1 = 0.0f;
						for( i = 0, k = peakLen[ band ]; i < chunkLength2; i++, k++ ) {
							if( i > m ) {
								f1 = 0.0f;
							}
							// peak scan; we reuse old max (f1) if it still lies in our search area (i <= m)
							for( j = m + 1; j < k; j++ ) {
								f2 = Math.abs( fftBuf2[ j ]);
								if( f2 >= f1 ) {
									f1 = f2;
									m  = j;
								}
							}
							if( f1 > 0.0f ) {
								convBuf1[ i ] += fftBuf2[ i ]*fftBuf2[ i ] / f1;	// self modulated + gain adjusted
							}
						}

//						for( i = 0, j = framesWritten; i < chunkLength2; i++, j++ ) {
//						for( i = 0; i < chunkLength2; i++ ) {
//							convBuf1[ i ] += fftBuf2[ i ]*fftBuf2[ i ]; // (float) (fftBuf2[ i ] * Math.cos( d1 * j ));
//						}

// 020220 end

/* 020220 removed:
						f1		= 0.0f;
						f3		= peakBuf[ ch ][ band ];
						decay	= 0.0;
						decayNum= 1;
						for( i = 0; i < chunkLength2; i++ ) {
							f2 = Math.abs( fftBuf2[ i ]);
							if( f2 > f1 ) {
								f1 = f2;
								if( f1 > f3 ) {
									d1	  = ((double) f1 - (double) f3) / (i + 1);
									if( d1 > decay ) {
										decay		= d1;
										decayNum	= i + 1;
									}
								}
							}
						}

						if( f1 > 1.0e-3f ) {
							if( f1 < f3 ) {
								decay	= ((double) f1 - (double) f3) / chunkLength2;
								decayNum= chunkLength2;
							}
//							decay * chunkLength2 + f1
							decay = Math.min( 0.0227, Math.max( -0.0227, decay ));	// max -1 dB/mSec
							d2 = (double) f3;
							for( i = 0; i < chunkLength2; i++ ) {
								if( decayNum-- > 0 ) {
									d2 += decay;
								}
								if( d2 > 1.0e-3 ) {
									d3  = 0.5 / d2;
									d1  = (double) fftBuf2[ i ] * d3 + 0.5;
									convBuf1[ i ] += (float) ((Math.min( 2.0, (d1 * d1) * 2 ) - 1.0) * d2);
								} else {
									convBuf1[ i ] += fftBuf2[ i ];
								}
							}						
							peakBuf[ ch ][ band ] = (float) d2;

						} else {
							// XXX knackser bei 0 <= f1 <= 1.0e-3 ?!
							Util.add( fftBuf2, 0, convBuf1, 0, chunkLength2 );
//							peakBuf[ ch ][ band ] = f1;
						}
*/
					} // for bands

				// ---- remove DC ----					adapted from CSound
//					if( pr.bool[ PR_DCBLOCK ]) {
//						convBuf2 = dcMem[ ch ];
//						for( i = 0; i < chunkLength2; i++ ) {
//							f1				= convBuf1[ i ];								// X1
//							convBuf1[ i ]	= f1 - convBuf2[ 0 ] + 0.99f * convBuf2[ 1 ];	// Y1 = X1-X0+Y0*gain
//							convBuf2[ 0 ]	= f1;											// next X0
//							convBuf2[ 1 ]	= convBuf1[ i ];								// next Y0
//						}
//					}

				// ---- highpass ----
					Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
					Fourier.complexMult( convBuf1, 0, highpass, 0, convBuf1, 0, highpass.length );
					Fourier.realTransform( convBuf1, fftLength, Fourier.INVERSE );
					Util.add( overBuf2[ ch ], 0, convBuf1, 0, overLen );
					System.arraycopy( convBuf1, inputLen, overBuf2[ ch ], 0, overLen );

				// ---- dry/wet ----
					if( hasDry ) {
						for( i = 0; i < chunkLength2; i++ ) {
							convBuf1[ i ] = convBuf1[ i ] * wetGain + convBuf2[ i ] * dryGain;
						}
						// inBuf always delayed by 2x halfWin to get correct dry/wet synchro
						System.arraycopy( convBuf2, chunkLength2, convBuf2, 0, fltLength );
					} else if( wetGain != 1.0f ) {
						Util.mult( convBuf1, 0, chunkLength2, wetGain );
					}

					progOff	 += chunkLength2 + chunkLength2;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // for chanNum
			// .... check running ....
				if( !threadRunning ) break topLevel;

				for( off = skip; threadRunning && (off < chunkLength2); ) {
					len			   = Math.min( 8192, chunkLength2 - off );
					if( floatF != null ) {
						for( ch = 0; ch < inChanNum; ch++ ) {
							floatF[ ch ].writeFloats( fftBuf3[ ch ], off, len );
						}
					} else {
						outF.writeFrames( fftBuf3, off, len );
					}
					progOff		  += len;
					off			  += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}

				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = fftBuf3[ ch ];
					for( off = skip; off < chunkLength2; off++ ) {
						f1 = Math.abs( convBuf1[ off ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}

				framesWritten += Math.max( 0, chunkLength2 - skip );
				if( skip > 0 ) {
					skip = Math.max( 0, skip - chunkLength2 );
				}
			} // until framesWritten == outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== normalize output ====================----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				f1		 = 1.0f;
				normalizeAudioFile( floatF, outF, fftBuf3, gain, f1 );
				maxAmp	*= gain;

				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ]		= null;
					tempFile[ ch ].delete();
					tempFile[ ch ]	= null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;
			outStream	= null;
			inF.close();
			inF			= null;
			inStream	= null;
			fftBuf1		= null;
			fftBuf2		= null;
			fftBuf3		= null;
			fftBuf		= null;
			highpass	= null;
//			peakBuf		= null;

			// inform about clipping/ low level
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			fftBuf1		= null;
			fftBuf2		= null;
			fftBuf3		= null;
			fftBuf		= null;
			highpass	= null;
//			peakBuf		= null;
			convBuf1	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
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
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
				}
				if( tempFile[ ch ] != null ) {
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}
		}
	} // process()

// -------- Item Methoden --------

}
// class ExciterDlg
