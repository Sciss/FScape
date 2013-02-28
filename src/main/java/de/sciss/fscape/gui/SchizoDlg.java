/*
 *  SchizoDlg.java
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
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Fake mono to stereo processing module
 *	using different filtering of the left
 *	and right channel.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class SchizoDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_CALLOSUMFILE		= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_OFFSET				= 1;
	private static final int PR_MGAIN				= 2;
	private static final int PR_SGAIN				= 3;
	private static final int PR_CONVLENGTH			= 4;
	private static final int PR_FLATSPECT			= 0;		// pr.bool
	private static final int PR_ZEROPHASE			= 1;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_CALLOSUMFILE	= "CallosumFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_OFFSET			= "Offset";
	private static final String PRN_MGAIN			= "MGain";
	private static final String PRN_SGAIN			= "SGain";
	private static final String PRN_CONVLENGTH		= "ConvLen";
	private static final String PRN_FLATSPECT		= "FlatSpect";
	private static final String PRN_ZEROPHASE		= "ZeroPhase";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_CALLOSUMFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final boolean	prBool[]	= { true, false };
	private static final String	prBoolName[]	= { PRN_FLATSPECT, PRN_ZEROPHASE };
	private static final Param	prPara[]		= { null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_OFFSET, PRN_MGAIN, PRN_SGAIN, PRN_CONVLENGTH };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_CALLOSUMFILE		= GG_OFF_PATHFIELD	+ PR_CALLOSUMFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_OFFSET				= GG_OFF_PARAMFIELD	+ PR_OFFSET;
	private static final int GG_MGAIN				= GG_OFF_PARAMFIELD	+ PR_MGAIN;
	private static final int GG_SGAIN				= GG_OFF_PARAMFIELD	+ PR_SGAIN;
	private static final int GG_CONVLENGTH			= GG_OFF_PARAMFIELD	+ PR_CONVLENGTH;
	private static final int GG_FLATSPECT			= GG_OFF_CHECKBOX	+ PR_FLATSPECT;
	private static final int GG_ZEROPHASE			= GG_OFF_CHECKBOX	+ PR_ZEROPHASE;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "Callosum chan# cannot be\ngreater than output chan#!";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SchizoDlg()
	{
		super( "Schizophrenia" );
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
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_OFFSET ]			= new Param( -100.0, Param.ABS_MS );
			static_pr.para[ PR_MGAIN ]			= new Param(   -1.4, Param.DECIBEL_AMP );
			static_pr.para[ PR_SGAIN ]			= new Param(  -17.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_CONVLENGTH ]		= new Param(    5.0, Param.ABS_MS );
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

		PathField			ggInputFile, ggOutputFile, ggCallosumFile;
		PathField[]			ggInputs;
		ParamField			ggOffset, ggMGain, ggSGain, ggConvLength;
		JCheckBox			ggFlatSpect, ggZeroPhase;
		Component[]			ggGain;
		ParamSpace[]		spcOffset;

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

		ggCallosumFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select c.callosum file" );
		ggCallosumFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Corpus callosum", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggCallosumFile, GG_CALLOSUMFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Schz$E" );
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
	gui.addLabel( new GroupLabel( "Surgeon Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcOffset		= new ParamSpace[ 2 ];
		spcOffset[ 0 ]	= Constants.spaces[ Constants.offsetMsSpace ];
		spcOffset[ 1 ]	= Constants.spaces[ Constants.offsetBeatsSpace ];
		ggOffset		= new ParamField( spcOffset );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Callosum offset", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggOffset, GG_OFFSET, null );
		
		ggFlatSpect		= new JCheckBox( "Flatten spect" );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggFlatSpect, GG_FLATSPECT, null );

		ggMGain			= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mid. gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMGain, GG_MGAIN, null );

		ggZeroPhase		= new JCheckBox( "Zero phases" );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggZeroPhase, GG_ZEROPHASE, null );

		ggSGain			= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Side gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggSGain, GG_SGAIN, null );

		ggConvLength	= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Conv. length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggConvLength, GG_CONVLENGTH, null );

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
		int					i, j, ch, len, off;
		long				progOff, progLen;
		float				f1;
		double				d1, d2, d3, d4;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFile			callF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		AudioFileDescr			callStream		= null;
		FloatFile[]			outFloatF		= null;
		File				outTempFile[]	= null;
		int					inChanNum, outChanNum, callChanNum;
		
		// buffers
		float[][]			inBuf			= null;
		float[][]			outBuf			= null;
		float[][]			callBuf			= null;
		float[]				fftBuf1			= null;
		float[]				fftBuf2			= null;
		float[]				fftBuf3			= null;
		float[][]			overBuf			= null;
		float[]				convBuf1, convBuf2;
		int					inBufSize;

		int[][]				callChan		= null;
		float[][]			callChanWeight	= null;
		double[][]			phase			= null;
		boolean				easyCallChan, easyPhase, newData;

		int					inLength, outLength, callLength, callOffset;
		int					preCall, postCall, frameSize, fftLength;
		int					framesRead, framesWritten;

		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		Param				peakGain;
		float				gain			= 1.0f;								 		// gain abs amp
		float				mGain, sGain;
		float				maxAmp			= 0.0f;

		PathField			ggOutput;

topLevel: try {

		// ---- open input ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// callosum input
			callF			= AudioFile.openAsRead( new File( pr.text[ PR_CALLOSUMFILE ]));
			callStream		= callF.getDescr();
			callChanNum		= callStream.channels;
			callLength		= (int) callStream.length;
			// this helps to prevent errors from empty files!
			if( (callLength < 1) || (callChanNum < 1) ) throw new EOFException( ERR_EMPTY );

			callOffset		= ((pr.para[ PR_OFFSET ].val >= 0.0) ? 1 : -1) * (int)
							  (AudioFileDescr.millisToSamples( callStream, Math.abs( pr.para[ PR_OFFSET ].val )) + 0.5);
			if( callOffset >= 0 ) {
				callOffset	= Math.min( callOffset, callLength );
				callF.seekFrame( callOffset );
				callLength -= callOffset;
				preCall		= 0;
			} else {
				callOffset	= Math.max( callOffset, -inLength );
				preCall		= -callOffset;
			}
			callLength		= Math.min( inLength - preCall, callLength );
			postCall		= inLength - preCall - callLength;
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outChanNum		= Math.max( 2, inChanNum );
			outLength		= inLength;
			if( callChanNum > outChanNum ) throw new EOFException( ERR_CHANNELS );

// System.out.println( "inLen "+inLength+"; preCall "+preCall+"; calllen "+callLength+"; postCall "+postCall );

			frameSize		= Math.max( pr.bool[ PR_FLATSPECT ] ? 128 : 32, (int)
								(AudioFileDescr.millisToSamples( inStream, pr.para[ PR_CONVLENGTH ].val) + 0.5) );
			// fftLength = frameSize+impulseResp.-1 auf 2er Potenz aufgerundet
			i = frameSize * ((pr.bool[ PR_FLATSPECT ] || pr.bool[ PR_ZEROPHASE ]) ? 3 : 2) - 1;
			for( fftLength = 2; fftLength < i; fftLength <<= 1 ) ;

// System.out.println( "frameLen "+frameSize+"; fftLen "+fftLength );

		// ---- open output ----

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outStream.channels = outChanNum;
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			progOff		= 0;
			progLen		= (long) inLength + (long) callLength + outLength;

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
				progLen	   += outLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- further inits ----

			inBufSize	= 8192 + frameSize - 1;
			inBufSize  -= inBufSize % frameSize;
// System.out.println( "inBufSize "+inBufSize );

			inBuf		= new float[ inChanNum ][ inBufSize ];
			callBuf		= new float[ callChanNum ][ inBufSize ];
			outBuf		= new float[ outChanNum ][ inBufSize ];
			fftBuf1		= new float[ fftLength + 2 ];
			fftBuf2		= new float[ fftLength + 2 ];
			fftBuf3		= new float[ fftLength + 2 ];
			overBuf		= new float[ outChanNum ][ frameSize ];		// for overlap-add convolution method
			Util.clear( overBuf );

			mGain		= (float) ((Param.transform( pr.para[ PR_MGAIN ], Param.ABS_AMP, ampRef, null )).val * gain);
			sGain		= (float) ((Param.transform( pr.para[ PR_SGAIN ], Param.ABS_AMP, ampRef, null )).val * gain);
// System.out.println( "mGain "+mGain+"; sGain "+sGain );

			easyCallChan	= (callChanNum == 1) || (callChanNum == outChanNum);
			if( !easyCallChan ) {
				callChan		= new int[ outChanNum ][ 2 ];
				callChanWeight	= new float[ outChanNum ][ 2 ];
	
				// calc weights
				for( ch = 0; ch < outChanNum; ch++ ) {
					f1							= ((float) ch / (float) (outChanNum - 1)) * (callChanNum - 1);
					callChan[ ch ][ 0 ]			= (int) f1;
					callChan[ ch ][ 1 ]			= ((int) f1 + 1) % callChanNum;		// last one just wrapped around w/ weight 0
					f1						   %= 1.0f;
					callChanWeight[ ch ][ 0 ]	= 1.0f - f1;
					callChanWeight[ ch ][ 1 ]	= f1;
				}
			}

			easyPhase		= outChanNum == 2;			// rotation by 180� can be calc'ed without trigonometrics
			if( !easyPhase ) {
				phase		= new double[ outChanNum ][ 2 ];
				d1			= 0.0;
				d2			= Constants.PI2 / outChanNum;
				for( ch = 0; ch < outChanNum; ch++, d1 += d2 ) {
					phase[ ch ][ 0 ] = Math.cos( d1 );
					phase[ ch ][ 1 ] = Math.sin( d1 );
				}
			}
	
		// ----==================== preCall ====================----

			framesRead		= 0;
			framesWritten	= 0;
			
			while( threadRunning && (framesRead < preCall) ) {
				len = Math.min( preCall - framesRead, inBufSize );

			// ---- read ----
				inF.readFrames( inBuf, 0, len );
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1	= outBuf[ ch ];
					convBuf2	= inBuf[ ch % inChanNum ];		// either inChanNum == 1 or inChanNum == outChanNum
					System.arraycopy( convBuf2, 0, convBuf1, 0, len );
					Util.mult( convBuf1, 0, len, mGain );
				}
				framesRead	+= len;
				progOff		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

			// ---- write ----
				if( outFloatF != null ) {			// unity gain > temp
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						outFloatF[ ch ].writeFloats( convBuf1, 0, len );
					}
				} else {							// abs gain > out
					outF.writeFrames( outBuf, 0, len );
				}
				// check max amp
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}
				framesWritten	+= len;
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // while framesRead < preCall

		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;
			
			while( threadRunning && (framesWritten < callLength) ) {
				len = Math.min( callLength - framesRead, inBufSize );

			// ---- read ----
				inF.readFrames( inBuf, 0, len );
				callF.readFrames( callBuf, 0, len );
				framesRead	+= len;
				progOff		+= len << 1;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero pad
				if( len < inBufSize ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = len; i < inBufSize; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
					for( ch = 0; ch < callChanNum; ch++ ) {
						convBuf1 = callBuf[ ch ];
						for( i = len; i < inBufSize; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
				}

			// ---- the main part ----
				for( off = 0; threadRunning && (off < len); off += frameSize ) {
					for( ch = 0; ch < outChanNum; ch++ ) {
						// step 1: copy mid part to outBuf
						convBuf1	= outBuf[ ch ];
						convBuf2	= inBuf[ ch % inChanNum ];		// either inChanNum == 1 or inChanNum == outChanNum
						System.arraycopy( convBuf2, off, convBuf1, off, frameSize );
						Util.mult( convBuf1, off, frameSize, mGain );
// System.out.println( "pre energy "+Filter.calcEnergy( convBuf1, off, frameSize ));
						Util.add( overBuf[ ch ], 0, convBuf1, off, frameSize );
						newData = !easyPhase;

						// step 2: take FFT of input
						if( (ch == 0) || (inChanNum > 1) ) {
							System.arraycopy( convBuf2, off, fftBuf1, 0, frameSize );
							Util.mult( fftBuf1, 0, frameSize, sGain );
							for( i = frameSize; i < fftLength; i++ ) {
								fftBuf1[ i ] = 0.0f;			// zero pad
							}
							Fourier.realTransform( fftBuf1, fftLength, Fourier.FORWARD );
							newData = true;
// System.out.println( "fftBuf1 energy "+(Filter.calcEnergy( fftBuf1, 0, fftLength + 2 ) / (2*fftLength)));
						}

						// step 3: take FFT of callosum
						if( (ch == 0) || (callChanNum > 1) ) {
							convBuf2 = pr.bool[ PR_FLATSPECT ] ? fftBuf3 : fftBuf2;
							if( easyCallChan ) {	// faster
								System.arraycopy( callBuf[ ch ], off, convBuf2, 0, frameSize );
							} else {
								for( i = 0, j = off; i < frameSize; i++, j++ ) {
									convBuf2[ i ]	= callBuf[ callChan[ ch ][ 0 ]][ j ] * callChanWeight[ ch ][ 0 ] +
													  callBuf[ callChan[ ch ][ 1 ]][ j ] * callChanWeight[ ch ][ 1 ];
								}
							}
							for( i = frameSize; i < fftLength; i++ ) {
								convBuf2[ i ] = 0.0f;			// zero pad
							}
							Fourier.realTransform( convBuf2, fftLength, Fourier.FORWARD );
							if( pr.bool[ PR_FLATSPECT ]) {
								flattenSpect( convBuf2, fftBuf2, fftLength + 2 );
							}
							if( pr.bool[ PR_ZEROPHASE ]) {
								zeroPhase( fftBuf2, fftLength + 2 );
							}
// System.out.println( "fftBuf2 energy "+(Filter.calcEnergy( convBuf2, 0, fftLength + 2 ) / (2*fftLength)));
							newData = true;
						}

						// step 4: convolve + phase shift + ifft
						if( newData ) {
							Fourier.complexMult( fftBuf1, 0, fftBuf2, 0, fftBuf3, 0, fftLength + 2 );
							
							if( ch > 0 ) {	// phase
								if( easyPhase ) {		// easier: a+ib => -a-ib
									for( i = 0; i < fftLength + 2; i++ ) {
										fftBuf3[ i ] = -fftBuf3[ i ];
									}
								} else {
									d1 = phase[ ch ][ 0 ];
									d2 = phase[ ch ][ 1 ];
									for( i = 0; i < fftLength + 2; ) {
										d3 = fftBuf3[ i ];
										d4 = fftBuf3[ i+1 ];
										fftBuf3[ i++ ] = (float) (d3 * d1 - d4 * d2);
										fftBuf3[ i++ ] = (float) (d3 * d2 + d4 * d1);
									}
								}
							}
							Fourier.realTransform( fftBuf3, fftLength, Fourier.INVERSE );

						} else {	// easyPhase constant data; take old IFFT and change sign
							for( i = 0; i < fftLength + 2; i++ ) {
								fftBuf3[ i ] = -fftBuf3[ i ];
							}
						}
						
						// step 5: mix side to output + save overlap
						Util.add( fftBuf3, 0, convBuf1, off, frameSize );
// System.out.println( "post energy "+Filter.calcEnergy( convBuf1, off, frameSize ));
						System.arraycopy( fftBuf3, frameSize, overBuf[ ch ], 0, frameSize );
						
					} // for outChanNum
				} // for frames
			// .... check running ....
				if( !threadRunning ) break topLevel;

			// ---- write ----
				if( outFloatF != null ) {			// unity gain > temp
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						outFloatF[ ch ].writeFloats( convBuf1, 0, len );
					}
				} else {							// abs gain > out
					outF.writeFrames( outBuf, 0, len );
				}
				// check max amp
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}
				framesWritten	+= len;
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // while framesRead < preCall

		// ----==================== postCall ====================----

			framesRead		= 0;
			framesWritten	= 0;
			off				= 0;		// don't forget to carry the remaining overlap!
			
			while( threadRunning && (framesRead < postCall) ) {
				len = Math.min( postCall - framesRead, inBufSize );

			// ---- read ----
				inF.readFrames( inBuf, 0, len );
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1	= outBuf[ ch ];
					convBuf2	= inBuf[ ch % inChanNum ];		// either inChanNum == 1 or inChanNum == outChanNum
					System.arraycopy( convBuf2, 0, convBuf1, 0, len );
					Util.mult( convBuf1, 0, len, mGain );
					if( off < frameSize ) {
						Util.add( overBuf[ ch ], off, convBuf1, 0, Math.min( len, frameSize - off ));
					}
				}
				framesRead	+= len;
				off			+= len;
				progOff		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

			// ---- write ----
				if( outFloatF != null ) {			// unity gain > temp
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						outFloatF[ ch ].writeFloats( convBuf1, 0, len );
					}
				} else {							// abs gain > out
					outF.writeFrames( outBuf, 0, len );
				}
				// check max amp
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}
				framesWritten	+= len;
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // while framesRead < postCall

		// ----==================== normalize output ====================----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				peakGain = new Param( maxAmp, Param.ABS_AMP );
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
				normalizeAudioFile( outFloatF, outF, outBuf, gain, 1.0f );
				maxAmp	*= gain;

				for( ch = 0; ch < outChanNum; ch++ ) {
					outFloatF[ ch ].cleanUp();
					outFloatF[ ch ] = null;
					outTempFile[ ch ].delete();
					outTempFile[ ch ] = null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;
			outStream	= null;
			callF.close();
			callF		= null;
			callStream	= null;
			inF.close();
			inF			= null;
			inStream	= null;
			outBuf		= null;
			inBuf		= null;
			callBuf		= null;

			// inform about clipping/ low level
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;				// make available to garbage collector
			outStream	= null;
			callStream	= null;
			inBuf		= null;
			outBuf		= null;
			callBuf		= null;
			fftBuf1		= null;
			fftBuf2		= null;
			fftBuf3		= null;
			overBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
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
		if( callF != null ) {
			callF.cleanUp();
			callF = null;
		}
		if( outFloatF != null ) {
			for( ch = 0; ch < outFloatF.length; ch++ ) {
				if( outFloatF[ ch ] != null ) outFloatF[ ch ].cleanUp();
				if( outTempFile[ ch ] != null ) outTempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------
	
	/*
	 *	Entfernt Formanten eines Spektrums; ein Moving-RMS der Laenge FFT-Size/32
	 *	wird berechnet und zur Gesamtenergie ins Verhaeltnis gesetzt
	 *
	 *	@param	src			Quell-Spectrum
	 *	@param	dest		Ziel mit herausgerechneten Formanten
	 *	@param	length		Spectrum Laenge (complex * 2)
	 */
	protected void flattenSpect( float[] src, float[] dest, int length )
	{
		int		average	= length >> 5;		// 1/32 bandwidth i.e. 689Hz, 1378Hz etc.
		int		i, j, k;
		double	energy	= 0.0;
		double	tot		= 0.0;
		double	d1;
		float	f1;
		
		for( i = 0, j = average >> 1; i < j; i++ ) {	// initial right wing
			energy	   += src[ i ] * src[ i ];
		}
// System.out.println( "i = "+i+"; j = "+j+"; energy = "+energy );
		for( j = 0; i < average; i++, j++ ) {			// unvollstaendiger anfang
			dest[ j ]	= Math.max( 1e-15f, (float) (energy / i) );
			energy	   += src[ i ] * src[ i ];
		}
// System.out.println( "i = "+i+"; j = "+j+"; tot    = "+energy );
		tot	= energy;
		for( k = 0; i < length; i++, j++, k++ ) {		// mitte
			dest[ j ]	= Math.max( 1e-15f, (float) (energy / average) );
			d1			= src[ i ] * src[ i ];
			energy		= energy - src[ k ] * src[ k ] + d1;	// moving average
			tot		   += d1;									// total accum.
		}
		tot /= (length >> 1);
// System.out.println( "i = "+i+"; j = "+j+"; k = "+k+"; energy = "+energy+"; tot = "+tot );
		for( i = average; j < length; j++, i--, k++ ) {	// unvollstaendiges ende
			dest[ j ]	= Math.max( 1e-15f, (float) (energy / i) );
			energy	   -= src[ k ] * src[ k ];
		}
// System.out.println( "i = "+i+"; j = "+j+"; k = "+k+"; energy = "+energy );
	
		// envelope removal
		for( i = 0; i < length; ) {	// max +96 dB
			f1			= Math.min( 63096.0f, (float) Math.sqrt( tot / (dest[ i ] + dest[ i+1 ])));
			dest[ i ]	= src[ i ] * f1; i++;
			dest[ i ]	= src[ i ] * f1; i++;
		}
	}

	/*
	 *	Entfernt Phase eines Spektrums (setzt alle auf Null)
	 *
	 *	@param	a			Spectrum
	 *	@param	length		Spectrum Laenge (complex * 2)
	 */
	protected void zeroPhase( float[] a, int length )
	{
		double	re, im;
		int		i, j;
		
		for( i = 0; i < length; ) {
			j		= i;
			re		= a[ i++ ];
			im		= a[ i ];
			a[ j ]	= (float) Math.sqrt( re*re + im*im );
			a[ i++ ]= 0.0f;
		}
	}
}
// class SchizoDlg
