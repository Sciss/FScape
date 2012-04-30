/*
 *  BandSplitDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Processing module for splitting a sound into
 *	a number of adjectant frequency bands.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 21-Jan-09
 */
public class BandSplitDlg
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
	private static final int PR_NUMBANDS		= 4;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_FREQ1			= 1;
	private static final int PR_FREQ2			= 2;
	private static final int PR_FREQ3			= 3;
	private static final int PR_FREQ4			= 4;
	private static final int PR_FREQ5			= 5;
	private static final int PR_FREQ6			= 6;
	private static final int PR_FREQ7			= 7;
	private static final int PR_FREQ8			= 8;
	private static final int PR_ROLLOFF			= 9;
	private static final int PR_NORMEACH		= 0;		// pr.bool

//	private static final int QUAL_LOW			= 0;	// PR_QUALITY
//	private static final int QUAL_MEDIUM		= 1;
	private static final int QUAL_GOOD			= 2;
//	private static final int QUAL_VERYGOOD		= 3;

	private static final String[]	QUAL_NAMES	= { "Low", "Medium", "Good", "Very good" };

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_QUALITY			= "Quality";
	private static final String PRN_NUMBANDS		= "NumBands";
	private static final String PRN_FREQ1			= "Freq1";
	private static final String PRN_FREQ2			= "Freq2";
	private static final String PRN_FREQ3			= "Freq3";
	private static final String PRN_FREQ4			= "Freq4";
	private static final String PRN_FREQ5			= "Freq5";
	private static final String PRN_FREQ6			= "Freq6";
	private static final String PRN_FREQ7			= "Freq7";
	private static final String PRN_FREQ8			= "Freq8";
	private static final String PRN_ROLLOFF			= "RollOff";
	private static final String PRN_NORMEACH		= "NormEach";

	private static final String	prText[]			= { "", "" };
	private static final String	prTextName[]		= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]		= { 0, 0, 0, QUAL_GOOD, 2 };
	private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
														PRN_QUALITY, PRN_NUMBANDS };
	private static final Param	prPara[]			= { null, null, null, null, null, null, null, null,
														null, null };
	private static final String	prParaName[]		= { PRN_GAIN, PRN_FREQ1, PRN_FREQ2, PRN_FREQ3, PRN_FREQ4,
														PRN_FREQ5, PRN_FREQ6, PRN_FREQ7, PRN_FREQ8,
														PRN_ROLLOFF };
	private static final boolean	prBool[]		= { false };
	private static final String	prBoolName[]		= { PRN_NORMEACH };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_QUALITY			= GG_OFF_CHOICE		+ PR_QUALITY;
	private static final int GG_NUMBANDS		= GG_OFF_CHOICE		+ PR_NUMBANDS;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_FREQ1			= GG_OFF_PARAMFIELD	+ PR_FREQ1;
//	private static final int GG_FREQ2			= GG_OFF_PARAMFIELD	+ PR_FREQ2;
//	private static final int GG_FREQ3			= GG_OFF_PARAMFIELD	+ PR_FREQ3;
//	private static final int GG_FREQ4			= GG_OFF_PARAMFIELD	+ PR_FREQ4;
//	private static final int GG_FREQ5			= GG_OFF_PARAMFIELD	+ PR_FREQ5;
//	private static final int GG_FREQ6			= GG_OFF_PARAMFIELD	+ PR_FREQ6;
//	private static final int GG_FREQ7			= GG_OFF_PARAMFIELD	+ PR_FREQ7;
//	private static final int GG_FREQ8			= GG_OFF_PARAMFIELD	+ PR_FREQ8;
	private static final int GG_ROLLOFF			= GG_OFF_PARAMFIELD	+ PR_ROLLOFF;
	private static final int GG_NORMEACH		= GG_OFF_CHECKBOX	+ PR_NORMEACH;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public BandSplitDlg()
	{
		super( "Band Splitting" );
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
			static_pr.para[ PR_FREQ1 ]			= new Param(   200.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ2 ]			= new Param(   800.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ3 ]			= new Param(  1600.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ4 ]			= new Param(  3200.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ5 ]			= new Param(  4800.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ6 ]			= new Param(  6400.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ7 ]			= new Param(  8000.0, Param.ABS_HZ );
			static_pr.para[ PR_FREQ8 ]			= new Param(  9600.0, Param.ABS_HZ );
			static_pr.para[ PR_ROLLOFF ]		= new Param(   100.0, Param.FACTOR_FREQ );
			static_pr.paraName	= prParaName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
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
		PathField[]			ggParent1;
		JComboBox				ggQuality, ggNumBands;
		JCheckBox			ggNormEach;
		ParamField			ggFreq, ggRollOff;
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
				case GG_NUMBANDS:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
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

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggParent1		= new PathField[ 1 ];
		ggParent1[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggParent1, "$D0$F0Splt$E" );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.3;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		ggNormEach		= new JCheckBox( "Norm. each file" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggNormEach, GG_NORMEACH, il );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Band Adjust", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggNumBands		= new JComboBox();
		for( i = 2; i <= 9; i++ ) {
			ggNumBands.addItem( String.valueOf( i ));
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "# of Bands", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggNumBands, GG_NUMBANDS, il );

		ggQuality		= new JComboBox();
		for( i = 0; i < QUAL_NAMES.length; i++ ) {
			ggQuality.addItem( QUAL_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Quality", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggQuality, GG_QUALITY, il );

		for( i = 0; i < 8; i++ ) {
			ggFreq			= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
			con.weightx		= 0.1;
			con.gridwidth	= 1;
			gui.addLabel( new JLabel( "CrossOver " + (i+1), SwingConstants.RIGHT ));
			con.weightx		= 0.4;
			con.gridwidth	= (i & 1) == 0 ? 1 : GridBagConstraints.REMAINDER;
			gui.addParamField( ggFreq, GG_FREQ1 + i, null );
		}

		ggRollOff		= new ParamField( Constants.spaces[ Constants.ratioFreqSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "RollOff", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggRollOff, GG_ROLLOFF, null );

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
		int					i, j, len, band, off, ch, chunkLength, chunkLength2;
		long				progOff, progLen;
		double				d1, d2;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile[]			outF;
		AudioFileDescr		inStream;
		AudioFileDescr[]	outStream;
		FloatFile[][]		floatF			= null;
		File[][]			tempFile		= null;
		
		// buffers
		float[]				fftBuf1, fftBuf2;
		float[][]			fftBuf;
		float[]				convBuf1;
		float[]				win;
		float[][]			inBuf;
		float[][][]			overBuf;

		int					inChanNum, inLength, fftLength, inputLen;
		int					framesRead, framesWritten;

		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float				gain			= 1.0f;								 		// gain abs amp
		float[]				maxAmp;

		PathField			ggOutput;

		int					numPeriods;
		int					halfWinSize, fltLength, overLen, skip;
		float[]				crossFreqs, cosineFreqs;
		double				freqNorm, freqBase, cosineNorm, cosineBase, rollOff;

		int					numBands		= pr.intg[ PR_NUMBANDS ] + 2;


		outF		= new AudioFile[ numBands ];
		outStream	= new AudioFileDescr[ numBands ];
		maxAmp		= new float[ numBands ];
		for( i = 0; i < numBands; i++ ) {
			outF[ i ]		= null;
			outStream[ i ]	= null;
			maxAmp[ i ]		= 0.0f;
		}
		
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
			j			= pr.text[ PR_OUTPUTFILE ].lastIndexOf( '.' );
			if( j < 0 ) j = pr.text[ PR_OUTPUTFILE ].length();

			for( i = 0; threadRunning && (i < numBands); i++ ) {
				outStream[i]		= new AudioFileDescr( inStream );
				ggOutput.fillStream( outStream[i] );
				outStream[i].file	= new File(
										pr.text[ PR_OUTPUTFILE ].substring( 0, j ) + (i+1) +
										pr.text[ PR_OUTPUTFILE ].substring( j ));
				outF[i]				= AudioFile.openAsWrite( outStream[i] );
			}			
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- calculate filters ----

			crossFreqs			= new float[ numBands+1 ];
			crossFreqs[0]		= 0.0f;
			crossFreqs[numBands]= (float) (inStream.rate/2);
			for( i = 1; i < numBands; i++ ) {
				crossFreqs[i]	= (float) pr.para[ PR_FREQ1 + (i-1) ].val;
			}

			cosineFreqs		= new float[ numBands+1 ];
			cosineFreqs[0]	= 0.0f;
			rollOff			= pr.para[ PR_ROLLOFF ].val/100;
			for( i = 1; i < numBands; i++ ) {
				d1			= Math.sqrt( crossFreqs[i-1] * crossFreqs[i] ) - crossFreqs[i-1];	// middle freq - lower freq
				d2			= Math.sqrt( crossFreqs[i] * crossFreqs[i+1] ) - crossFreqs[i];
				cosineFreqs[i] = Math.max( 0.0f, (float) (Math.min( d1, d2 ) * rollOff ));
			}
			cosineFreqs[numBands] = 0.0f;

			numPeriods		= 3 * (1 << pr.intg[ PR_QUALITY ]);
			halfWinSize		= Math.max( 1, (int) ((double) numPeriods * inStream.rate / crossFreqs[1] + 0.5) );
			freqNorm		= Constants.PI2 / inStream.rate;
			cosineNorm		= 4.0 / (Math.PI*Math.PI);
			fltLength		= halfWinSize + halfWinSize;
			win				= Filter.createFullWindow( fltLength, Filter.WIN_BLACKMAN );

			j				= fltLength + fltLength - 1;
			for( fftLength = 2; fftLength < j; fftLength <<= 1 ) ;
			inputLen		= fftLength - fltLength + 1;
			overLen			= fftLength - inputLen;

			fftBuf			= new float[ numBands ][ fftLength + 2 ];
			fftBuf1			= new float[ fftLength + 2 ];
			fftBuf2			= new float[ fftLength + 2 ];
			inBuf			= new float[ inChanNum ][ inputLen ];
			overBuf			= new float[ inChanNum ][ numBands ][ overLen ];
//			for( i = 0; i < inChanNum; i++ ) {
//				Util.clear( overBuf[ i ]);
//			}

			// LP = +1.0 fc  -1.0 Zero
			// HP = +1.0 �/2 -1.0 fc
			// BP = +1.0 fc2 -1.0 fc1
			
			// calculate impulse response of the bandpasses
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
			}

		// ---- parameter inits ----

			progOff		= 0;
			progLen		= (long) inLength * (1 + numBands * inChanNum);

			tempFile	= new File[ numBands ][ inChanNum ];
			floatF		= new FloatFile[ numBands ][ inChanNum ];
			for( i = 0; i < numBands; i++ ) {
				for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
					tempFile[i][ch]		= null;
					floatF[i][ch]		= null;
				}
			}
			for( i = 0; i < numBands; i++ ) {
				for( ch = 0; ch < inChanNum; ch++ ) {
					tempFile[i][ch]		= IOUtil.createTempFile();
					floatF[i][ch]		= new FloatFile( tempFile[i][ch], GenericFile.MODE_OUTPUT );
				}
			}
			progLen	   += (long) inLength * numBands;
		// .... check running ....
			if( !threadRunning ) break topLevel;

			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null ).val / Math.PI);
			}

		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;
			skip			= halfWinSize;

			while( threadRunning && (framesWritten < inLength) ) {

				chunkLength = Math.min( inputLen, inLength - framesRead );
			// ---- read input chunk ----
				for( off = 0; threadRunning && (off < chunkLength); ) {
					len	= Math.min( 8192, chunkLength - off );
					inF.readFrames( inBuf, off, len );
					framesRead	+= len;
					progOff		+= len;
					off			+= len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				chunkLength2 = Math.min( inputLen, inLength - framesWritten );

			// ---- channels loop -----------------------------------------------------------------------
				for( ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					System.arraycopy( inBuf[ ch ], 0, fftBuf1, 0, chunkLength );
					for( i = chunkLength; i < fftLength; i++ ) {
						fftBuf1[ i ] = 0.0f;
					}
					Fourier.realTransform( fftBuf1, fftLength, Fourier.FORWARD );

				// ---- bands loop ----------------------------------------------------------------------
					for( band = 0; threadRunning && (band < numBands); band++ ) {
					
						Fourier.complexMult( fftBuf1, 0, fftBuf[ band ], 0, fftBuf2, 0, fftBuf1.length );
						Fourier.realTransform( fftBuf2, fftLength, Fourier.INVERSE );
						Util.add( overBuf[ ch ][ band ], 0, fftBuf2, 0, overLen );
//						System.arraycopy( fftBuf2, inputLen, overBuf[ ch ][ band ], 0, overLen );
						System.arraycopy( fftBuf2, chunkLength, overBuf[ ch ][ band ], 0, overLen );

						for( off = skip; threadRunning && (off < chunkLength2); ) {
							len			   = Math.min( 8192, chunkLength2 - off );
							floatF[ band ][ ch ].writeFloats( fftBuf2, off, len );
							// check max amp
							for( i = off, j = off + len; i < j; i++ ) {
								f1 = Math.abs( fftBuf2[ i ]);
								if( f1 > maxAmp[band] ) {
									maxAmp[band] = f1;
								}
							}							
							progOff		  += len;
							off			  += len;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
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

			if( (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY) && !pr.bool[ PR_NORMEACH ]) {
				f1 = 0.0f;
				for( i = 0; i < numBands; i++ ) {
					f1 = Math.max( f1, maxAmp[i] );
				}
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / f1, Param.ABS_AMP ), null )).val;
			}
			for( band = 0; threadRunning && (band < numBands); band++ ) {
				if( (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY) && pr.bool[ PR_NORMEACH ]) {
					gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
										new Param( 1.0 / maxAmp[ band ],  Param.ABS_AMP ), null )).val;
				}
				maxAmp[ band ] *= gain;
			
				f1 = getProgression() + (1.0f - getProgression()) / (numBands - band);
				normalizeAudioFile( floatF[ band ], outF[ band ], inBuf, gain, f1 );

				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[band][ch].cleanUp();
					floatF[band][ch]		= null;
					tempFile[band][ch].delete();
					tempFile[band][ch]	= null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----

			for( i = 0; i < numBands; i++ ) {
				outF[i].close();
				outF[i]			= null;
				outStream[i]	= null;
			}
			inF.close();
			inF			= null;
			inStream	= null;
			fftBuf1		= null;
			fftBuf2		= null;
			fftBuf		= null;
			overBuf		= null;

			// inform about clipping/ low level
			f1			= 0.0f;
			for( i = 0; i < numBands; i++ ) {
				f1 		= Math.max( f1, maxAmp[ i ]);
			}
			handleClipping( f1 );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			fftBuf1		= null;
			fftBuf2		= null;
			overBuf		= null;
			fftBuf		= null;
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
			for( i = 0; i < numBands; i++ ) {
				if( outF[i] != null ) {
					outF[i].cleanUp();
					outF[i] = null;
				}
			}
		}
		if( floatF != null ) {
			for( i = 0; i < numBands; i++ ) {
				for( ch = 0; ch < floatF[i].length; ch++ ) {
					if( floatF[i][ch] != null ) {
						floatF[i][ch].cleanUp();
						floatF[i][ch] = null;
					}
					if( tempFile[i][ch] != null ) {
						tempFile[i][ch].delete();
						tempFile[i][ch] = null;
					}
				}
			}
		}
	} // process()

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component	c;
		int			i;
		int			numBands = pr.intg[ PR_NUMBANDS ];
		
		for( i = 0; i < 8; i++ ) {
			c = gui.getItemObj( GG_FREQ1 + i );
			if( c != null ) {
				c.setEnabled( i <= numBands );
			}
		}
	}
}
// class BandSplitDlg
