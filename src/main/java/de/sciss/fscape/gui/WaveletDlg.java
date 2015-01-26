/*
 *  WaveletDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		07-Jan-05	removed obsolete soundfile methods,
 *					then had to remove interleaved-proc. option
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
 *	Processing module for wavelet pyramid (octave decimation) decomposition
 *	forward + backward.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.65, 07-Jan-05
 */
public class WaveletDlg
extends ModulePanel
{
// -------- public Variablen --------

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_DIRECTION		= 2;
	private static final int PR_FILTER			= 3;		// !!! CORRESPONDS TO Wavelet.COEFFS_... !!!
	private static final int PR_GAINTYPE		= 4;
	private static final int PR_LENGTH			= 5;
	private static final int PR_GAIN			= 0;		// pr.para	
	private static final int PR_SCALEGAIN		= 1;

	private static final int DIR_FORWARD			= 0;
	private static final int DIR_BACKWARD			= 1;
	private static final int LENGTH_EXPAND			= 0;
	private static final int LENGTH_TRUNC			= 1;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_DIRECTION		= "Dir";
	private static final String PRN_FILTER			= "Filter";
	private static final String PRN_SCALEGAIN		= "ScaleGain";
	private static final String PRN_LENGTH			= "Length";

	private static final String	prText[]			= { "", "" };
	private static final String	prTextName[]		= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]		= { 0, 0, DIR_FORWARD, Wavelet.COEFFS_DAUB4,
														GAIN_UNITY, LENGTH_EXPAND };
	private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_DIRECTION, PRN_FILTER,
														PRN_GAINTYPE, PRN_LENGTH };
	private static final Param	prPara[]			= { null, null };
	private static final String	prParaName[]		= { PRN_GAIN, PRN_SCALEGAIN };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_LENGTH				= GG_OFF_CHOICE		+ PR_LENGTH;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_SCALEGAIN			= GG_OFF_PARAMFIELD	+ PR_SCALEGAIN;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_DIRECTION			= GG_OFF_CHOICE		+ PR_DIRECTION;
	private static final int GG_FILTER				= GG_OFF_CHOICE		+ PR_FILTER;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public WaveletDlg()
	{
		super( "Wavelet Translation" );
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
			static_pr.para[ PR_SCALEGAIN ]	= new Param( 3.0, Param.DECIBEL_AMP );
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
		JComboBox			ggDirection, ggFilter, ggLength;
		ParamField			ggScaleGain;
		PathField[]			ggInputs;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

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
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0WT$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Translation", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggFilter		= new JComboBox();
		for( int i = 4; i <= 20; i += 2 ) {
			ggFilter.addItem( "Daubechies "+i );
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filter", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggFilter, GG_FILTER, null );

		ggScaleGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Gain per Scale", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggScaleGain, GG_SCALEGAIN, null );

		ggDirection		= new JComboBox();
		ggDirection.addItem( "Forward" );
		ggDirection.addItem( "Backward (Inverse)" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Direction", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggDirection, GG_DIRECTION, null );

//		ggInterleave	= new JCheckBox( "Interleaved processing" );
//		con.weightx		= 0.5;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addCheckbox( ggInterleave, GG_INTERLEAVE, this );

		ggLength		= new JComboBox();
		ggLength.addItem( "Expand to flt*2^n" );
		ggLength.addItem( "Truncate to flt*2^n" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "FWT Length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggLength, GG_LENGTH, null );

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
		
	/**
	 *	Translation durchfuehren
	 */
	public void process()
	{
		int					i, j, k;
		int					off, len, ch;
		int					progOff, progLen;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile[][]		floatF			= null;						// index 0+1 = smooth, 2 = detail
		File[][]			tempFile		= null;
		int					smoothIndex		= 0;						// rotates between 0+1
		int					inChanNum;
		float[][]			inputBuf		= null;
		int					inputMem;
		float[][]			detailBuf		= null;
		float[][]			smoothBuf		= null;
//		float				buf[];
		int					outputMem;
		int					dataLen, passLen, transLen;
		int					pass, passes;
		int					overlap, offStart;

		PathField			ggOutput;

		// Synthesize
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		Param				peakGain;													// (abs amp)
		float				gain;												 		// gain abs amp
		float[][]			flt;
		int					fltSize;

//		int					totalInSamples	= 0;	// reichen 32 bit?
		int					inLength;
		int					framesRead;
		int					framesWritten;
		int					framesToGo;

		float				maxAmp			= 0.0f;

		int					scale, scaleNum;		// Skalen-Ebene, Gesamtzahl
		float				scaleGain;				// (abs amp)

		System.gc();	// this algorithm may need a lot of memory

topLevel: try {
			
			flt		= Wavelet.getCoeffs( pr.intg[ PR_FILTER ]);
			fltSize	= flt[ 0 ].length;
		// ---- first pass: load and transform input ----

			inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream	= inF.getDescr();
			inChanNum	= inStream.channels;
			inLength	= (int) inStream.length;
//			if( pr.bool[ PR_INTERLEAVE ]) {
//				inLength   *= inChanNum;
//				inChanNum	= 1;
//			}
//			totalInSamples= inLength * inChanNum;
			// this helps to prevent errors from empty files!
			if( inLength * inChanNum < 1 ) throw new EOFException( ERR_EMPTY );

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
//			outChanNum	= outStream.channels;
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			floatF		= new FloatFile[ inChanNum ][ 3 ];
			tempFile	= new File[ inChanNum ][ 3 ];
			for( ch = 0; ch < inChanNum; ch++ ) {
				for( i = 0; i < 2; i++ ) {
					tempFile[ ch ][ i ]	= null;
					floatF[ ch ][ i ]	= null;
				}
			}

//			buf			= new float[ 8192 ];
			
			// lowest level gotta be >= fltSize (for Daub. Boundary Wavelet Algorithm), then power of 2
			for( dataLen = fltSize, scaleNum = 1; dataLen < inLength; dataLen<<=1, scaleNum++ ) ;
			if( (dataLen > inLength) && (pr.intg[ PR_LENGTH ] == LENGTH_TRUNC) ) {
				dataLen >>= 1;
			}

			// take 75% of free memory, divide by sizeof( float ), divide by 2 (50% input/ 50% output)
			inputMem	= (int) ((Runtime.getRuntime().freeMemory() >> 5) * 3 / inChanNum) & ~1;
//System.out.println( "inpMem"+ inputMem+"; fltSize "+fltSize+"; dataLen "+dataLen+"; inLength "+inLength+"; totalIn "+totalInSamples );
//			if( inputMem < fltSize ) throw new OutOfMemoryError( ERR_MEMORY );
//			if( inputMem > dataLen ) {
//				inputMem = dataLen;
//			}
			inputMem	= Math.min( Math.max( inputMem, fltSize ), dataLen );

			outputMem	= inputMem >> 1;			// je 25% fuer smooth und detail output

		// ************************************************** FORWARD **************************************************
			if( pr.intg[ PR_DIRECTION ] == DIR_FORWARD ) {

				inputBuf	= new float[ inChanNum ][ inputMem + (fltSize<<1) ];	// may cause OutOfMemoryError!
				detailBuf	= new float[ inChanNum ][ outputMem ];
				smoothBuf	= new float[ inChanNum ][ outputMem ];

				passLen		= dataLen;					// number of samples to filter in current pass
				framesRead	= 0;
				scale		= scaleNum;
			
				for( ch = 0; ch < inChanNum; ch++ ) {
					tempFile[ ch ][ 2 ]	= IOUtil.createTempFile();	// for detail data
					floatF[ ch ][ 2 ]	= new FloatFile( tempFile[ ch ][ 2 ], GenericFile.MODE_OUTPUT );
				}

				progOff		= 0;
				progLen		= (dataLen - (fltSize>>1)) << 3;	// number of operations (for prog-bar)
			
			// ---- each pass is one complete filtering (decimation in time, one step of the pyramid) ----
				while( (passLen >= fltSize) && threadRunning ) {

					scale--;
					scaleGain	= (float) (Param.transform( new Param( pr.para[ PR_SCALEGAIN ].val * scale, pr.para[ PR_SCALEGAIN ].unit ),
										   Param.ABS_AMP, ampRef, null )).val;

					for( ch = 0; ch < inChanNum; ch++ ) {
						for( i = 0; i < fltSize; i++ ) {
							inputBuf[ ch ][ i ] = 0.0f;			// zero padding left wing
						}
					}
//System.out.println( "-----passLen "+passLen+"; zero padded left wing" );

					overlap		= fltSize;
					offStart	= fltSize;
					for( framesToGo = passLen, passes = 0; (framesToGo > 0) && threadRunning; passes++ ) {
						transLen	= Math.min( framesToGo, inputMem );	// number of samples to transform
// System.out.println( "pass "+passes+"; transLen "+transLen );
						// ---- read input (smooth) ----
						off		= 0;
						do {
//							len = Math.min( samplesToGo, Math.min( 8192 / ChanNum, transLen - off + overlap ));
							len = Math.min( framesToGo, Math.min( 8192, transLen - off + overlap ));
//							len2= len * inChanNum;

							// read from input file
							if( inF != null ) {
//								k = Math.min( totalInSamples - samplesRead, len2 );
								k = Math.min( inLength - framesRead, len );

								inF.readFrames( inputBuf, off + offStart, k );

								// zeropadding
								for( ch = 0; ch < inChanNum; ch++ ) {
									for( i = k, j = i + off + offStart; i < len; i++, j++ ) {
										inputBuf[ ch ][ j ] = 0.0f;
									}
								}
								framesRead	+= k;

							// read from smooth tempfile								
							} else if( floatF[ 0 ][ smoothIndex ] != null ) {		// from smooth tempfile
								for( ch = 0; ch < inChanNum; ch++ ) {
									floatF[ ch ][ smoothIndex ].readFloats( inputBuf[ ch ], off + offStart, len );
								}
								
							// read from RAM
							} else {
								for( ch = 0; ch < inChanNum; ch++ ) {
									System.arraycopy( smoothBuf[ ch ], off, inputBuf[ ch ], off + offStart, len );
								}
							}
							off			+= len;
							framesToGo	-= len;
						// .... progress ....
							progOff		+= len;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
					// .... check running ....
						if( !threadRunning ) break topLevel;

						if( framesToGo == 0 ) {
// System.out.println( "zero padded right wing" );
							for( ch = 0; ch < inChanNum; ch++ ) {
								for( j = 0, k = offStart + transLen; j < fltSize; j++, k++ ) {
									inputBuf[ ch ][ k ] = 0.0f;			// zero padding right wing
								}
							}
							transLen += offStart - fltSize;
						}
	
						// ---- transform ----
// System.out.println( "transforming... "+(transLen) );
						for( ch = 0; ch < inChanNum; ch++ ) {
							Wavelet.fwdTransform( inputBuf[ ch ], smoothBuf[ ch ], detailBuf[ ch ], fltSize, transLen, flt );

							// adjust scale gain, measure max. gain in detail buf
							for( j = 0; j < (transLen>>1); j++ ) {
								detailBuf[ ch ][ j ] *= scaleGain;
								if( Math.abs( detailBuf[ ch ][ j ]) > maxAmp ) {
									maxAmp = Math.abs( detailBuf[ ch ][ j ]);
								}
							}
						}
						progOff += transLen;
						setProgression( (float) progOff / (float) progLen );
						if( !threadRunning ) break topLevel;
	
						// ---- write detail ----
						off	= 0;
						do {
							len = Math.min( 8192, (transLen >> 1) - off );
							for( ch = 0; ch < inChanNum; ch++ ) {
								floatF[ ch ][ 2 ].writeFloats( detailBuf[ ch ], off, len );
							}
							off		+= len;
						// .... progress ....
							progOff += len;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
					// .... check running ....
						if( !threadRunning ) break topLevel;
							
						if( framesToGo > 0 ) {						// need several passes, so copy overlap + create temp
// System.out.println( "copy overlap, write to smooth" );
							for( ch = 0; ch < inChanNum; ch++ ) {
								System.arraycopy( inputBuf[ ch ], transLen, inputBuf[ ch ], 0, fltSize << 1 );	// copy overlap
							}
							if( passes == 0 ) {
								offStart	= fltSize << 1;
								overlap		= 0;
								for( j = 0; j < 2; j++ ) {
									if( tempFile[ 0 ][ j ] == null ) {	// ....create temp files if we haven't done it before
// System.out.println( "   create smooth temp files" );
										for( ch = 0; ch < inChanNum; ch++ ) {
											tempFile[ ch ][ j ]	= IOUtil.createTempFile();
											floatF[ ch ][ j ]	= new FloatFile( tempFile[ ch ][ j ], GenericFile.MODE_OUTPUT );
										}
									}
								}
							}
						}
						if( floatF[ 0 ][ 1 - smoothIndex ] != null ) {	// need several passes, so write out smooth
							// ---- write smooth ----
							off	= 0;
							do {
								len = Math.min( 8192, (transLen >> 1) - off );
// System.out.println( "wrote "+len+" to smooth "+(1 - smoothIndex));
								for( ch = 0; ch < inChanNum; ch++ ) {
									floatF[ ch ][ 1 - smoothIndex ].writeFloats( smoothBuf[ ch ], off, len );
								}
								off		+= len;
							// .... progress ....
								progOff += len;
								setProgression( (float) progOff / (float) progLen );
							} while( (len > 0) && threadRunning );
						} else {									// quasi-inplace
							progOff += (transLen >> 1);
						}						
					} // for( passes )
				// .... check running ....
					if( !threadRunning ) break topLevel;
					
					// ---- do some cleanup ----
					if( inF != null ) {			// only needed in the first pyramid pass
						inF.close();
						inF = null;
					}
					for( i = 0; i < 2; i++ ) {
						if( floatF[ 0 ][ i ] != null ) {		// reset read/write positions
							for( ch = 0; ch < inChanNum; ch++ ) {
								floatF[ ch ][ i ].seekFloat( 0 );
							}
						}
					}
					if( passes > 1 ) {			// swap smooth files 1&2 if we needed several passes
						smoothIndex	= 1 - smoothIndex;
					} else {					// ,else delete smooth files (can continue quasi-inplace)
						for( i = 0; i < 2; i++ ) {
							if( tempFile[ 0 ][ i ] != null ) {
								for( ch = 0; ch < inChanNum; ch++ ) {
									floatF[ ch ][ i ].cleanUp();
									floatF[ ch ][ i ]	= null;
									tempFile[ ch ][ i ].delete();
									tempFile[ ch ][ i ]	= null;
								}
							}
						}
					}
				
					passLen >>= 1;
				} // for( pyramid )

				// don't forget to measure max. gain in last smooth buf
				for( ch = 0; ch < inChanNum; ch++ ) {
					for( j = 0; j < passLen; j++ ) {
						if( Math.abs( smoothBuf[ ch ][ j ]) > maxAmp ) {
							maxAmp = Math.abs( smoothBuf[ ch ][ j ]);
						}
					}
				}

		// ************************************************** BACKWARD **************************************************
			} else if( pr.intg[ PR_DIRECTION ] == DIR_BACKWARD ) {

				inputBuf	= new float[ inChanNum ][ inputMem ];	// well, it's actually the output, not the input ;))))
				detailBuf	= new float[ inChanNum ][ outputMem + (fltSize<<1) ];
				smoothBuf	= new float[ inChanNum ][ outputMem + (fltSize<<1) ];

				passLen		= fltSize>>1;					// number of samples/2 to synthesize in current pass
			
				// ---- read out smooth (>>RAM) ----
				inF.readFrames( inputBuf, 0, passLen );
				
//				if( inChanNum > 1 )	{			// fucking de-interleave shit
//					inF.readSamples( buf, 0, passLen * inChanNum );
//					for( ch = 0; ch < inChanNum; ch++ ) {
//						for( i = ch, j = 0; j < passLen; i += inChanNum, j++ ) {
//							inputBuf[ ch ][ j ] = buf[ i ];
//						}
//					}
//				} else {
//					inF.readSamples( inputBuf[ 0 ], 0, passLen );
// System.out.println( "pre-read "+len+" from input = smooth RAM" );
//				}
//				samplesRead	= passLen * inChanNum;
				framesRead	= passLen;
				scale		= 0;

				progOff		= 0;
				progLen		= (dataLen<<3) - fltSize*3;		// number of operations (for prog-bar)
			
			// ---- each pass is one complete synthesis (one backward step of the pyramid) ----
				while( (passLen < dataLen) && threadRunning ) {

					scaleGain	= (float) (Param.transform( new Param( -pr.para[ PR_SCALEGAIN ].val * scale, pr.para[ PR_SCALEGAIN ].unit ),
										   Param.ABS_AMP, ampRef, null )).val;
					scale++;

					for( ch = 0; ch < inChanNum; ch++ ) {
						for( i = 0; i < fltSize; i++ ) {
							detailBuf[ ch ][ i ] = 0.0f;			// zero padding left wing
							smoothBuf[ ch ][ i ] = 0.0f;
						}
					}
// System.out.println( "-----passLen "+passLen+"; zero padded left wing" );

					overlap		= fltSize;
					offStart	= fltSize;
					for( framesToGo = passLen, passes = 0; (framesToGo > 0) && threadRunning; passes++ ) {
						transLen	= Math.min( framesToGo, outputMem );	// number of samples/2 to transform
// System.out.println( "pass "+passes+"; transLen "+transLen );
						// ---- read smooth+detail ----
						off		= 0;
						do {
//							len  = Math.min( samplesToGo, Math.min( 8192 / inChanNum, transLen - off + overlap ));
//							len2 = len * inChanNum;
							len  = Math.min( framesToGo, Math.min( 8192, transLen - off + overlap ));

							// read from smooth tempfile
							if( floatF[ 0 ][ smoothIndex ] != null ) {
								for( ch = 0; ch < inChanNum; ch++ ) {
									floatF[ ch ][ smoothIndex ].readFloats( smoothBuf[ ch ], off + offStart, len );
								}					
								
							// read from RAM
							} else {
								for( ch = 0; ch < inChanNum; ch++ ) {
									System.arraycopy( inputBuf[ ch ], off, smoothBuf[ ch ], off + offStart, len );
								}
							}

							// read detail from input
							inF.readFrames( detailBuf, off + offStart, len );
//							if( inChanNum > 1 ) {		// needs de-interleave
//								inF.readSamples( buf, 0, len2 );
//								for( ch = 0; ch < inChanNum; ch++ ) {
//									for( i = ch, j = off + offStart; i < len2; i += inChanNum, j++ ) {
//										detailBuf[ ch ][ j ] = buf[ i ];
//									}
//								}
//							} else {				// can read directly
//								inF.readSamples( detailBuf[ 0 ], off + offStart, len );
//							}
							
							// apply scale gain
							for( ch = 0; ch < inChanNum; ch++ ) {
								for( j = 0, k = off + offStart; j < len; j++, k++ ) {
									detailBuf[ ch ][ k ] *= scaleGain;
								}
							}
							off			+= len;
							framesToGo	-= len;
						// .... progress ....
							progOff		+= len<<1;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
					// .... check running ....
						if( !threadRunning ) break topLevel;

						if( framesToGo == 0 ) {
							transLen += offStart - fltSize;
						}
						// zero padding right wing
						for( ch = 0; ch < inChanNum; ch++ ) {
							for( k = off + offStart, j = transLen + (fltSize<<1) - k; j > 0; j--, k++ ) {
								smoothBuf[ ch ][ k ] = 0.0f;
								detailBuf[ ch ][ k ] = 0.0f;
							}
						}
//System.out.println( "off+offStart" + (off+offStart)+"; trans until "+(transLen+fltSize+fltSize));
						transLen <<= 1;		// output samples instead of detail/smooth sample number
	
						// ---- transform ----
// System.out.println( "transforming... "+(transLen) );
						for( ch = 0; ch < inChanNum; ch++ ) {

							Wavelet.invTransform( inputBuf[ ch ], smoothBuf[ ch ], detailBuf[ ch ], fltSize, transLen, flt );

							// measure max. gain in last pass
							if( (passLen<<1) >= dataLen ) {
								for( j = 0; j < transLen; j++ ) {
									if( Math.abs( inputBuf[ ch ][ j ]) > maxAmp ) {
										maxAmp = Math.abs( inputBuf[ ch ][ j ]);
									}
								}
							}
						}
						progOff += transLen;
						setProgression( (float) progOff / (float) progLen );
						if( !threadRunning ) break topLevel;
	
						if( framesToGo > 0 ) {						// need several passes, so copy overlap + create temp
// System.out.println( "copy overlap, write to smooth" );
							for( ch = 0; ch < inChanNum; ch++ ) {
								System.arraycopy( smoothBuf[ ch ], transLen>>1, smoothBuf[ ch ], 0, fltSize<<1 );	// copy overlap
								System.arraycopy( detailBuf[ ch ], transLen>>1, detailBuf[ ch ], 0, fltSize<<1 );
							}
							if( passes == 0 ) {
								offStart	= fltSize << 1;
								overlap		= 0;
								if( tempFile[ 0 ][ 1 - smoothIndex ] == null ) {	// ....create temp file if we haven't done it before
// System.out.println( "   create smooth temp file "+(1 - smoothIndex) );
									for( ch = 0; ch < inChanNum; ch++ ) {
										tempFile[ ch ][ 1 - smoothIndex ]	= IOUtil.createTempFile();
										floatF[ ch ][ 1 - smoothIndex ]		= new FloatFile( tempFile[ ch ][ 1 - smoothIndex ],
										                               		              GenericFile.MODE_OUTPUT );
									}
								}
							}
						}
						if( floatF[ 0 ][ 1 - smoothIndex ] != null ) {	// need several passes, so write out smooth
							// ---- write synthesis ("smooth") ----
							off	= 0;
							do {
								len = Math.min( 8192, transLen - off );
// System.out.println( "wrote "+len+" to smooth "+(1 - smoothIndex));
								for( ch = 0; ch < inChanNum; ch++ ) {
									floatF[ ch ][ 1 - smoothIndex ].writeFloats( inputBuf[ ch ], off, len );
								}
								off		+= len;
							// .... progress ....
								progOff += len;
								setProgression( (float) progOff / (float) progLen );
							} while( (len > 0) && threadRunning );
						} else {									// quasi-inplace
							progOff += transLen;
						}						
					} // for( passes )
				// .... check running ....
					if( !threadRunning ) break topLevel;
					
					// ---- do some cleanup ----
					for( i = 0; i < 2; i++ ) {
						if( floatF[ 0 ][ i ] != null ) {		// reset read/write positions
							for( ch = 0; ch < inChanNum; ch++ ) {
								floatF[ ch ][ i ].seekFloat( 0 );
							}
						}
					}
					if( passes > 1 ) {			// swap smooth files 1&2 if we needed several passes
						smoothIndex	= 1 - smoothIndex;
					}
				
					passLen <<= 1;
				} // for( pyramid )

				// ---- do some cleanup ----
				if( inF != null ) {								// close input
					inF.close();
					inF = null;
				}
				if( tempFile[ 0 ][ 1 - smoothIndex ] != null ) {		// delete unused temp file
					for( ch = 0; ch < inChanNum; ch++ ) {
						floatF[ ch ][ 1 - smoothIndex ].cleanUp();
						floatF[ ch ][ 1 - smoothIndex ]	= null;
						tempFile[ ch ][ 1 - smoothIndex ].delete();
						tempFile[ ch ][ 1 - smoothIndex ]	= null;
					}
				}
			} else {	// (if backward)
				throw new IllegalArgumentException( String.valueOf( pr.intg[ PR_DIRECTION ])); 
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
	
		// ---- intermezzo: calc gain adjustment ----
			peakGain	= new Param( maxAmp, Param.ABS_AMP );
			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			} else {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
			}
//System.out.println( "prog "+progOff+" of "+progLen );
		// ************************************************** second pass: save output **************************************************

		// ************************************************** FORWARD **************************************************
			if( pr.intg[ PR_DIRECTION ] == DIR_FORWARD ) {

				// ---- write out smooth (RAM) ----
				for( ch = 0; ch < inChanNum; ch++ ) {
					for( j = 0; j < passLen; j++ ) {
						smoothBuf[ ch ][ j ] *= gain;
					}
				}
				outF.writeFrames( smoothBuf, 0, passLen );
//				if( inChanNum > 1 )	{			// fucking interleave shit
//					for( ch = 0; ch < inChanNum; ch++ ) {
//						for( i = ch, j = 0; j < passLen; i += inChanNum, j++ ) {
//							buf[ i ] = smoothBuf[ ch ][ j ];
//						}
//					}
//					outF.writeSamples( buf, 0, passLen * inChanNum );
//				} else {
//					outF.writeSamples( smoothBuf[ 0 ], 0, passLen );
// System.out.println( "pre-read "+len+" from input = smooth RAM" );
//				}
				framesWritten	= passLen;
				smoothBuf		= null;
				detailBuf		= null;
				framesRead		= 0;
	
			// ---- each pass writes out one complete detail line in the pyramid ----
				while( (passLen < dataLen) && threadRunning ) {
					// passes = number of successive transforms of inputBuf in current pass
					passes	= (passLen + inputMem - 1) / inputMem;
	
					framesToGo	= passLen;
					for( ch = 0; ch < inChanNum; ch++ ) {
						floatF[ ch ][ 2 ].seekFloat( dataLen - passLen - framesWritten );
					}
					for( pass = 0; (pass < passes) && threadRunning; pass++ ) {
						transLen	= Math.min( framesToGo, inputMem );	// number of samples to copy
						// ---- read detail from temp ----
						off	= 0;
						do {
							len	 = Math.min( 8192, transLen - off );
//							len2 = len * inChanNum;
							for( ch = 0; ch < inChanNum; ch++ ) {
								floatF[ ch ][ 2 ].readFloats( inputBuf[ ch ], off, len );
							}
							framesRead	+= len;
							off			+= len;
						// .... progress ....
							progOff		+= len;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
					// .... check running ....
						if( !threadRunning ) break topLevel;
						
						// adjust gain
						for( ch = 0; ch < inChanNum; ch++ ) {
							for( j = 0; j < transLen; j++ ) {
								inputBuf[ ch ][ j ] *= gain;
							}
						}
	
						// ---- write detail to output ----
						off	= 0;
						do {
//							len	 = Math.min( 8192 / inChanNum, transLen - off );
//							len2 = len * inChanNum;
							len	 = Math.min( 8192, transLen - off );
							outF.writeFrames( inputBuf, off, len );
//							if( inChanNum > 1 )	{			// fucking interleave shit
//								for( ch = 0; ch < inChanNum; ch++ ) {
//									for( i = ch, j = off; i < len2; i += inChanNum, j++ ) {
//										buf[ i ] = inputBuf[ ch ][ j ];
//									}
//								}
//								outF.writeSamples( buf, 0, len2 );
//							} else {
//								outF.writeSamples( inputBuf[ 0 ], off, len2 );
//							}
							framesWritten	+= len;
							off				+= len;
						// .... progress ....
							progOff			+= len;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
					// .... check running ....
						if( !threadRunning ) break topLevel;
	
						framesToGo -= transLen;
					} // for( passes )
				// .... check running ....
					if( !threadRunning ) break topLevel;
	
					passLen <<= 1;
				} // for( pyramid )
			// .... check running ....
				if( !threadRunning ) break topLevel;
	
				inputBuf	= null;
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ][ 2 ].cleanUp();
					floatF[ ch ][ 2 ]	= null;
					tempFile[ ch ][ 2 ].delete();
					tempFile[ ch ][ 2 ] = null;
				}

		// ************************************************** BACKWARD **************************************************
			} else {
			
				framesWritten	= 0;
				smoothBuf		= null;
				detailBuf		= null;
				framesRead		= 0;
				framesToGo		= dataLen;
				
				while( (framesToGo > 0) && threadRunning ) {
					transLen	= Math.min( framesToGo, inputMem );	// number of samples to copy
				// ---- read synthesis ("smooth") from temp ----
					if( floatF[ 0 ][ smoothIndex ] != null ) {
						off	= 0;
						do {
							len  = Math.min( 8192, transLen - off );
//							len2 = len * inChanNum;
							for( ch = 0; ch < inChanNum; ch++ ) {
								i = floatF[ ch ][ smoothIndex ].readFloats( inputBuf[ ch ], off, len );
							}
							framesRead	+= len;
							off			+= len;
						// .... progress ....
							progOff		+= len;
							setProgression( (float) progOff / (float) progLen );
						} while( (len > 0) && threadRunning );
				// ---- read synthesis ("smooth") from RAM (inplace) ----
//System.out.println(" out of place" );
					} else {
//System.out.println(" in place "+transLen+"; to go "+samplesToGo );
						progOff += transLen;
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

					// adjust gain
					for( ch = 0; ch < inChanNum; ch++ ) {
						for( j = 0; j < transLen; j++ ) {
							inputBuf[ ch ][ j ] *= gain;
						}
					}

					// ---- write detail to output ----
					off	= 0;
					do {
//						len  = Math.min( 8192 / inChanNum, transLen - off );
//						len2 = len * inChanNum;
						len  = Math.min( 8192, transLen - off );
						outF.writeFrames( inputBuf, off, len );
//						if( inChanNum > 1 )	{			// fucking interleave shit
//							for( ch = 0; ch < inChanNum; ch++ ) {
//								for( i = ch, j = off; i < len2; i += inChanNum, j++ ) {
//									buf[ i ] = inputBuf[ ch ][ j ];
//								}
//							}
//							outF.writeSamples( buf, 0, len2 );
//						} else {
//							outF.writeSamples( inputBuf[ 0 ], off, len2 );
//						}
						framesWritten	+= len;
						off				+= len;
					// .... progress ....
						progOff			+= len;
						setProgression( (float) progOff / (float) progLen );
					} while( (len > 0) && threadRunning );
				// .... check running ....
					if( !threadRunning ) break topLevel;

					framesToGo -= transLen;
				} // while framesToGo
			// .... check running ....
				if( !threadRunning ) break topLevel;
	
				inputBuf	= null;
				if( tempFile[ 0 ][ smoothIndex ] != null ) {			// delete unused temp file
					for( ch = 0; ch < inChanNum; ch++ ) {
						floatF[ ch ][ smoothIndex ].cleanUp();
						floatF[ ch ][ smoothIndex ]	= null;
						tempFile[ ch ][ smoothIndex ].delete();
						tempFile[ ch ][ smoothIndex ]	= null;
					}
				}
			} // (else Backward)

			maxAmp *= gain;	// now maxInputAmp = maxOutputAmp
			outF.close();
			outF = null;

// System.out.println( "progOff "+progOff+"; progLen "+progLen );

		// ---- Finish ----

			// inform about clipping/ low level
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inputBuf	= null;
			smoothBuf	= null;
			detailBuf	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		inputBuf	= null;
		smoothBuf	= null;
		detailBuf	= null;

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
				for( i = 0; i < 3; i++ ) {
					if( floatF[ ch ][ i ] != null ) {
						floatF[ ch ][ i ].cleanUp();
						floatF[ ch ][ i ] = null;
					}
					if( tempFile[ ch ][ i ] != null ) {
						tempFile[ ch ][ i ].delete();
						tempFile[ ch ][ i ] = null;
					}
				}
			}
		}
	} // process()

// -------- private Methoden --------
}
// class WaveletDlg
