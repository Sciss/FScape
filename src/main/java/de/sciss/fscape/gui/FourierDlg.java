/*
 *  FourierDlg.java
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
 *		07-Jan-05	uses indicateOutputWrite() ; automatic gain compensation
 *					preserves original gain for 0 dB immediate forward + backward
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;

/**
 *  Processing module to transform a
 *	sound file from time domain to
 *	frequency domain and vice versa
 *	using harddisk temp files.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 23-Jan-09
 */
public class FourierDlg
extends ModulePanel
{
// -------- public Variablen --------

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_REINPUTFILE		= 0;		// pr.text
	private static final int PR_IMINPUTFILE		= 1;
	private static final int PR_REOUTPUTFILE	= 2;
	private static final int PR_IMOUTPUTFILE	= 3;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_DIRECTION		= 2;
	private static final int PR_GAINTYPE		= 3;
	private static final int PR_FORMAT			= 4;
	private static final int PR_LENGTH			= 5;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_MEMORY			= 1;
	private static final int PR_HASIMINPUT		= 0;		// pr.bool
	private static final int PR_HASIMOUTPUT		= 1;

	private static final int DIR_FORWARD		= 0;
	private static final int DIR_BACKWARD		= 1;

	private static final int FORMAT_NORMAL		= 0;
	private static final int FORMAT_POLAR		= 1;
//	private static final int FORMAT_CEPSTRAL	= 2;

	private static final int LENGTH_EXPAND		= 0;
	private static final int LENGTH_TRUNC		= 1;

	private static final String PRN_REINPUTFILE		= "ReInFile";
	private static final String PRN_IMINPUTFILE		= "ImInFile";
	private static final String PRN_REOUTPUTFILE	= "ReOutFile";
	private static final String PRN_IMOUTPUTFILE	= "ImOutFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_DIRECTION		= "Dir";
	private static final String PRN_FORMAT			= "Format";
	private static final String PRN_LENGTH			= "Length";
	private static final String PRN_MEMORY			= "Memory";
	private static final String PRN_HASIMINPUT		= "HasImInput";
	private static final String PRN_HASIMOUTPUT		= "HasImOutput";

	private static final String	prText[]		= { "", "", "", "" };
	private static final String	prTextName[]	= { PRN_REINPUTFILE, PRN_IMINPUTFILE,
													PRN_REOUTPUTFILE, PRN_IMOUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, DIR_FORWARD, GAIN_UNITY, FORMAT_NORMAL, LENGTH_EXPAND };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_DIRECTION, PRN_GAINTYPE,
													PRN_FORMAT, PRN_LENGTH };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_MEMORY };
	private static final boolean	prBool[]	= { false, true };
	private static final String	prBoolName[]	= { PRN_HASIMINPUT, PRN_HASIMOUTPUT };

	private static final int GG_REINPUTFILE		= GG_OFF_PATHFIELD	+ PR_REINPUTFILE;
	private static final int GG_IMINPUTFILE		= GG_OFF_PATHFIELD	+ PR_IMINPUTFILE;
	private static final int GG_REOUTPUTFILE	= GG_OFF_PATHFIELD	+ PR_REOUTPUTFILE;
	private static final int GG_IMOUTPUTFILE	= GG_OFF_PATHFIELD	+ PR_IMOUTPUTFILE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_MEMORY			= GG_OFF_PARAMFIELD	+ PR_MEMORY;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_FORMAT			= GG_OFF_CHOICE		+ PR_FORMAT;
	private static final int GG_LENGTH			= GG_OFF_CHOICE		+ PR_LENGTH;
	private static final int GG_DIRECTION		= GG_OFF_CHOICE		+ PR_DIRECTION;
	private static final int GG_HASIMINPUT		= GG_OFF_CHECKBOX	+ PR_HASIMINPUT;
	private static final int GG_HASIMOUTPUT		= GG_OFF_CHECKBOX	+ PR_HASIMOUTPUT;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	MARK_PIHALF			= "PiHalf";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public FourierDlg()
	{
		super( "Fourier Translation" );
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
//			static_pr.para[ PR_FREQSHIFT ]	= new Param(  0.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_MEMORY ]		= new Param( 16.0, Param.NONE );
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
//		GridBagLayout		lay;
//		Rectangle			bounds;

		PathField			ggImInputFile, ggReInputFile, ggReOutputFile, ggImOutputFile;
		JComboBox				ggDirection, ggFormat, ggLength;
		JCheckBox			ggHasImInput, ggHasImOutput;
		PathField[]			ggInputs, ggOutputs;
		Component[]			ggGain;
//		ParamField			ggFreqShift;
		ParamField			ggMemory;
//		ParamSpace[]		spcFreqShift;
		int					i;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
//		lay				= gui.getGridBagLayout();
		con.insets		= new Insets( 1, 2, 1, 2 );
		
		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_HASIMINPUT:
				case GG_HASIMOUTPUT:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggReInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select real part of input" );
		ggReInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input [Real]", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggReInputFile, GG_REINPUTFILE, null );

		ggImInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select imaginary part of input" );
		ggImInputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImInput	= new JCheckBox( "Input [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		i				= con.anchor;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImInput, GG_HASIMINPUT, il );
		con.anchor		= i;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImInputFile, GG_IMINPUTFILE, null );

		ggReOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output for real part" );
		ggReOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output [Real]", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggReOutputFile, GG_REOUTPUTFILE, null );
		gui.registerGadget( ggReOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggReOutputFile.getResGadget(), GG_OUTPUTRES );

		ggImOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE, "Select output for imaginary part" );
//		ggImOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImOutput	= new JCheckBox( "Output [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImOutput, GG_HASIMOUTPUT, il );
		con.anchor		= i;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImOutputFile, GG_IMOUTPUTFILE, null );

		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggReInputFile;
		ggOutputs		= new PathField[ 1 ];
		ggOutputs[ 0 ]	= ggReOutputFile;
		ggImInputFile.deriveFrom( ggInputs, "$D0$F0i$X0" );
		ggReOutputFile.deriveFrom( ggInputs, "$D0$F0FT$E" );
		ggImOutputFile.deriveFrom( ggOutputs, "$D0$F0i$X0" );
		
		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Translation", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggDirection		= new JComboBox();
		ggDirection.addItem( "Forward" );
		ggDirection.addItem( "Backward (Inverse)" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Direction", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggDirection, GG_DIRECTION, il );

		ggFormat		= new JComboBox();
		ggFormat.addItem( "Normal (rect)" );
		ggFormat.addItem( "Polar" );
		ggFormat.addItem( "Cepstral (log.)" );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Spectral format", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggFormat, GG_FORMAT, il );

		ggLength		= new JComboBox();
		ggLength.addItem( "Expand to 2^n" );
		ggLength.addItem( "Truncate to 2^n" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "FFT Length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggLength, GG_LENGTH, il );

//		spcFreqShift	= new ParamSpace[ 2 ];
//		spcFreqShift[0]	= Constants.spaces[ Constants.factorFreqSpace ];
//		spcFreqShift[1]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
//		ggFreqShift		= new ParamField( spcFreqShift );
//		ggFreqShift.setReference( new Param( 1.0, Param.ABS_HZ ));
		ggMemory		= new ParamField( new ParamSpace( 1.0, 2047.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		con.gridwidth	= 1;
//		gui.addLabel( new JLabel( "Freq scale", SwingConstants.RIGHT ));
		gui.addLabel( new JLabel( "Mem.alloc. [MB]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addParamField( ggFreqShift, GG_FREQSHIFT, this );
		gui.addParamField( ggMemory, GG_MEMORY, null );

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
//		int					i, j, k;
//		int					ch;
		float				f1;
		double				d1, d2;
		boolean				b1;
		long				progOff, progLen;
		
		// io
		AudioFile			reInF			= null;
		AudioFile			imInF			= null;
		AudioFile			reOutF			= null;
		AudioFile			imOutF			= null;
		AudioFileDescr		reInStream		= null;
		AudioFileDescr		imInStream		= null;
		AudioFileDescr		reOutStream		= null;
		AudioFileDescr		imOutStream		= null;
		FloatFile			floatF[][]		= null;						// see storageFFT
		File				tempFile[][]	= null;
		int					inChanNum;
		float[][]			buf1			= null;
		float[][]			buf2			= null;
		float[]				buf3			= null;
		float[][]			fftBuf			= null;
		float[]				convBuf1, convBuf2;
		int					memAmount;
		long				dataLen			= 0;
		long				halfLen;

		PathField			ggOutput;
//		Marker				mark			= null;

		// Synthesize
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float				gain;												 		// gain abs amp

		long				totalInSamples; // 	= 0;	// reichen 32 bit?
		long				inLength;
		long				framesRead, framesWritten;
		long				n1;
//		long				n2;
		int					chunkLen, i1;

		float				maxAmp			= 0.0f;

		int					scaleNum		= 0;
		float				freqShift;
		
		List				markers;

topLevel: try {

		// ---- first pass: copy input to two float files ----

			reInF		= AudioFile.openAsRead( new File( pr.text[ PR_REINPUTFILE ]));
			reInStream	= reInF.getDescr();
			inChanNum	= reInStream.channels;
			inLength	= (int) reInStream.length;
			if( pr.bool[ PR_HASIMINPUT ]) {
				imInF		= AudioFile.openAsRead( new File( pr.text[ PR_IMINPUTFILE ]));
				imInStream	= imInF.getDescr();
				if( imInStream.channels != inChanNum ) throw new IOException( ERR_COMPLEX );
				inLength	= (int) Math.min( inLength, imInStream.length );
			}
			totalInSamples= inLength * inChanNum;
			// this helps to prevent errors from empty files!
			if( totalInSamples < 1 ) throw new EOFException( ERR_EMPTY );

			// ---- FFT Length ----
//			i			= Marker.find( reInStream.markers, MARK_PIHALF, 0 );	// use this marker if available
n1 = -1;
/*
			if( i >= 0 ) {
				mark	= (Marker) reInStream.markers.elementAt( i );
				reInStream.markers.removeElement( mark );						// we recreate it for the output
			} else if( imInStream != null ) {
				i		= Marker.find( imInStream.markers, MARK_PIHALF, 0 );	// (maybe in the imag. file)
				if( i >= 0 ) {
					mark= (Marker) imInStream.markers.elementAt( i );
				}
			}
			if( n1 >= 0 ) {	// input contains "PiHalf" marker ==> use to calc FFTLength
				n2 = mark.pos << 1;
				for( dataLen = 2, scaleNum = 1; dataLen < n2; dataLen<<=1, scaleNum++ ) ;
				if( dataLen != n2 ) n1 = -1;	// calc the normal way below
			}
*/
			if( n1 < 0 ) {	// otherwise expand/trunc input length to power of 2
				for( dataLen = 2, scaleNum = 1; dataLen < inLength; dataLen<<=1, scaleNum++ ) ;
				if( (pr.intg[ PR_LENGTH ] == LENGTH_TRUNC) && (dataLen > inLength) ) {
					dataLen		  >>= 1;
					scaleNum--;
					inLength		= dataLen;
					totalInSamples	= inLength * inChanNum;
				}
			}
			halfLen = dataLen >> 1;
			if( pr.intg[ PR_DIRECTION ] == DIR_FORWARD ) {
				// will be carried forward to output files
				markers = (List) reInStream.getProperty( AudioFileDescr.KEY_MARKERS );
				if( markers == null ) markers = new ArrayList( 1 );
				markers.add( new Marker( halfLen, MARK_PIHALF ));
				reInStream.setProperty( AudioFileDescr.KEY_MARKERS, markers );
			}

			// ---- open files ----
			ggOutput	= (PathField) gui.getItemObj( GG_REOUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			reOutStream	= new AudioFileDescr( reInStream );
			ggOutput.fillStream( reOutStream );
			reOutF		= AudioFile.openAsWrite( reOutStream );

			if( pr.bool[ PR_HASIMOUTPUT ]) {
				imOutStream			= new AudioFileDescr( reInStream );
				ggOutput.fillStream( imOutStream );
				imOutStream.file	= new File( pr.text[ PR_IMOUTPUTFILE ]);
				imOutF				= AudioFile.openAsWrite( imOutStream );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;


			// ---- Other init ----
//			freqShift	= (float) Param.transform( pr.para[ PR_FREQSHIFT ], Param.FACTOR_FREQ,
//									new Param( 1.0, Param.ABS_HZ ), null ).val /
//						  ((pr.intg[ PR_DIRECTION ] == DIR_FORWARD) ? 100 : -100);
freqShift = (pr.intg[ PR_DIRECTION ] == DIR_FORWARD) ? 1 : -1;
// System.out.println( "freqShift "+freqShift );

			// ---- Progress Length ----
			progOff		= 0;			// write floats, write output, transform, read input
			progLen		= (long) dataLen * (3 + scaleNum * inChanNum) + (long) inLength;
			if( pr.bool[ PR_HASIMINPUT ]) {
				progLen	 += (long) inLength;
			}
			if( pr.bool[ PR_HASIMOUTPUT ]) {
				progLen	 += (long) dataLen;
			}
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				progLen	 += (long) dataLen * inChanNum;
			}
			
			// ---- create four temp files per channel ----
			floatF		= new FloatFile[ inChanNum ][ 4 ];
			tempFile	= new File[ inChanNum ][ 4 ];
			for( int ch = 0; ch < inChanNum; ch++ ) {
				for( int i = 0; i < 4; i++ ) {
					tempFile[ ch ][ i ]	= null;
					floatF[ ch ][ i ]	= null;
				}
			}
			for( int ch = 0; ch < inChanNum; ch++ ) {
				for( int i = 0; i < 4; i++ ) {
					tempFile[ ch ][ i ]	= IOUtil.createTempFile();	// for detail data
					floatF[ ch ][ i ]	= new FloatFile( tempFile[ ch ][ i ], GenericFile.MODE_OUTPUT );
				}
			}
			

			// take 75% of free memory, divide by sizeof( float ), divide by 3 (storageFFT needs 3 buffers)
//			i	= Math.min( dataLen, (int) (Runtime.getRuntime().freeMemory() >> 4) );
i1 = (int) Math.min( 0x40000000, Math.min( dataLen, ((long) pr.para[ PR_MEMORY ].val << 20) / 12 ));	// +/- (?) 3x float (=12 bytes)
			// power of 2
			for( memAmount = 4; memAmount <= i1; memAmount <<= 1 ) ;
			memAmount >>= 1;
// memAmount = Math.min( dataLen, 65536 );	// 32768 );

			// ---- write initial float files 1 + 2 ----
			b1 = (pr.intg[ PR_DIRECTION ] == DIR_BACKWARD) && (pr.intg[ PR_FORMAT ] == FORMAT_POLAR);

			buf1		= new float[ inChanNum ][ 8192 ];
			if( pr.bool[ PR_HASIMINPUT ] || pr.bool[ PR_HASIMOUTPUT ]) {
				buf2	= new float[ inChanNum ][ 8192 ];
			} else {
				buf2	= buf1;
			}
			buf3		= new float[ 16384 ];

			for( framesRead = 0; threadRunning && (framesRead < inLength); ) {
				chunkLen = (int) Math.min( 8192, inLength - framesRead );
				if( (framesRead < halfLen) && ((framesRead + chunkLen) > halfLen) ) {
					chunkLen = (int) (halfLen - framesRead);
				}
				reInF.readFrames( buf1, 0, chunkLen );
			// .... progress ....
				progOff += chunkLen;
				setProgression( (float) progOff / (float) progLen );

				if( imInF != null ) {
					imInF.readFrames( buf2, 0, chunkLen );
				// .... progress ....
					progOff += chunkLen;
					setProgression( (float) progOff / (float) progLen );
				}
				framesRead += chunkLen;
				// interleave real/img
				for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					convBuf1	= buf1[ ch ];
					convBuf2	= buf2[ ch ];
					// ---- real + img ----
					if( pr.bool[ PR_HASIMINPUT ]) {
						for( int j = 0, k = 0; j < chunkLen; j++ ) {
							buf3[ k++ ] = convBuf1[ j ];
							buf3[ k++ ] = convBuf2[ j ];
						}
						// ---- rect => polar ----
						if( b1 ) {
							Fourier.polar2Rect( buf3, 0, buf3, 0, chunkLen << 1 );
						}
					// ---- only real ---- NOTE: rect+polar is identical (amp = real; phase = 0)
					} else {
						for( int j = 0, k = 0; j < chunkLen; j++ ) {
							buf3[ k++ ] = convBuf1[ j ];
							buf3[ k++ ] = 0.0f;			// zero imaginary part
						}
					}
					if( framesRead <= halfLen ) {
						floatF[ ch ][ 0 ].writeFloats( buf3, 0, chunkLen << 1 );
					} else {
						floatF[ ch ][ 1 ].writeFloats( buf3, 0, chunkLen << 1 );
					}
				}
			// .... progress ....
				progOff += chunkLen;
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// zero pad to power of 2
			for( int i = 0; i < buf3.length; i++ ) {
				buf3[ i ] = 0.0f;
			}
			while( threadRunning && (framesRead < dataLen) ) {
				chunkLen = (int) Math.min( 8192, dataLen - framesRead );
				if( (framesRead < halfLen) && ((framesRead + chunkLen) > halfLen) ) {
					chunkLen = (int) (halfLen - framesRead);
				}
				framesRead += chunkLen;
				// interleave real/img
				for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					if( framesRead <= halfLen ) {
						floatF[ ch ][ 0 ].writeFloats( buf3, 0, chunkLen << 1 );
					} else {
						floatF[ ch ][ 1 ].writeFloats( buf3, 0, chunkLen << 1 );
					}
				}
			// .... progress ....
				progOff += chunkLen;
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			reInF.close();
			reInF = null;
			if( imInF != null ) {
				imInF.close();
				imInF = null;
			}

		// ---- transform ----

			fftBuf = new float[ 3 ][ memAmount ];
					
			for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
				progOff += dataLen * scaleNum;		// = progEnd
				storageFFT( floatF[ ch ], tempFile[ ch ], dataLen, freqShift, fftBuf, (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- calc unity gain ----

			b1 = (pr.intg[ PR_DIRECTION ] == DIR_FORWARD) && (pr.intg[ PR_FORMAT ] == FORMAT_POLAR);

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				for( int ch = 0; ch < inChanNum; ch++ ) {
					for( int i = 2; i < 4; i++ ) {
						floatF[ ch ][ i ].seekFloat( 0 );
						for( framesRead = 0; threadRunning && (framesRead < dataLen); ) {
							chunkLen = (int) Math.min( 8192, dataLen - framesRead );
							floatF[ ch ][ i ].readFloats( buf3, 0, chunkLen );
							framesRead += chunkLen;

							// ---- rect => polar ----
							if( b1 ) {
								for( int k = 0; k < chunkLen; ) {
									d1 = buf3[ k++ ];
									d2 = buf3[ k++ ];
									f1 = (float) Math.sqrt( d1*d1 + d2*d2 );
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
								}
							// ---- normal ----
							} else {
								// ---- real + img ----
								if( pr.bool[ PR_HASIMOUTPUT ]) {
									for( int k = 0; k < chunkLen; k++ ) {
										if( Math.abs( buf3[ k ]) > maxAmp ) {
											maxAmp = Math.abs( buf3[ k ]);
										}
									}
								// ---- only real ----
								} else {
									for( int k = 0; k < chunkLen; k += 2 ) {
										if( Math.abs( buf3[ k ]) > maxAmp ) {
											maxAmp = Math.abs( buf3[ k ]);
										}
									}
								}
							}
						// .... progress ....
							progOff += chunkLen >> 1;
							setProgression( (float) progOff / (float) progLen );
						}
					}
				}
				
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / (double) maxAmp, Param.ABS_AMP ), null )).val;
			// absolute gain change
			} else {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				gain = (float) (gain / Math.sqrt( dataLen ));
			}

		// ---- write output ----

			// delete unused + rewind
			for( int ch = 0; ch < inChanNum; ch++ ) {
				int i = 0;
				for( ; i < 2; i++ ) {
					floatF[ ch ][ i ].cleanUp();
					floatF[ ch ][ i ] = null;
//System.out.println( "deleting : " + tempFile[ch][i].getName() );
					tempFile[ ch ][ i ].delete();
					tempFile[ ch ][ i ] = null;
				}
				for( ; i < 4; i++ ) {
					floatF[ ch ][ i ].seekFloat( 0 );
				}
			}

			indicateOutputWrite();

			// ---- write data ----
			for( framesWritten = 0; threadRunning && (framesWritten < dataLen); ) {
				chunkLen = (int) Math.min( 8192, dataLen - framesWritten );
				if( (framesWritten < halfLen) && ((framesWritten + chunkLen) > halfLen) ) {
					chunkLen = (int) (halfLen - framesWritten);
				}
				// de-interleave real/img
				for( int ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					if( framesWritten < halfLen ) {
						floatF[ ch ][ 2 ].readFloats( buf3, 0, chunkLen << 1 );	// floatF[ ch ][ 2 ]
					} else {
						floatF[ ch ][ 3 ].readFloats( buf3, 0, chunkLen << 1 );	// floatF[ ch ][ 3 ]
					}
					convBuf1	= buf1[ ch ];
					convBuf2	= buf2[ ch ];
					// ---- rect => polar ----
					if( b1 ) {
						// ---- real + img ----
						if( pr.bool[ PR_HASIMOUTPUT ]) {
							Fourier.rect2Polar( buf3, 0, buf3, 0, chunkLen << 1 );
							// ---- needs gain measurement ----
							if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									if( Math.abs( buf3[ k ]) > maxAmp ) {
										maxAmp = Math.abs( buf3[ k ]);
									}
									convBuf1[ j ] = gain * buf3[ k++ ];
									convBuf2[ j ] = (float) (buf3[ k++ ] / Math.PI);	// -pi <= Phi <= +pi ===> map to -1...+1
								}
							// ---- gain already known ----
							} else {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									convBuf1[ j ] = gain * buf3[ k++ ];
									convBuf2[ j ] = (float) (buf3[ k++ ] / Math.PI);	// -pi <= Phi <= +pi ===> map to -1...+1
								}
							}
						// ---- only real ----
						} else {
							// ---- needs gain measurement ----
							if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									d1 = buf3[ k++ ];
									d2 = buf3[ k++ ];
									f1 = (float) Math.sqrt( d1*d1 + d2*d2 );
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
									convBuf1[ j ] = gain * f1;
								}
							// ---- gain already known ----
							} else {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									d1 = buf3[ k++ ];
									d2 = buf3[ k++ ];
									f1 = (float) Math.sqrt( d1*d1 + d2*d2 );
									convBuf1[ j ] = gain * f1;
								}
							}
						}
					// ---- normal ----
					} else {
						// ---- real + img ----
						if( pr.bool[ PR_HASIMOUTPUT ]) {
							// ---- needs gain measurement ----
							if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									if( Math.abs( buf3[ k ]) > maxAmp ) {
										maxAmp = Math.abs( buf3[ k ]);
									}
									convBuf1[ j ] = gain * buf3[ k++ ];
									if( Math.abs( buf3[ k ]) > maxAmp ) {
										maxAmp = Math.abs( buf3[ k ]);
									}
									convBuf2[ j ] = gain * buf3[ k++ ];
								}
							// ---- gain already known ----
							} else {
								for( int j = 0, k = 0; j < chunkLen; j++ ) {
									convBuf1[ j ] = gain * buf3[ k++ ];
									convBuf2[ j ] = gain * buf3[ k++ ];
								}
							}
						// ---- only real ----
						} else {
							// ---- needs gain measurement ----
							if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
								for( int j = 0, k = 0; j < chunkLen; j++, k += 2 ) {
									if( Math.abs( buf3[ k ]) > maxAmp ) {
										maxAmp = Math.abs( buf3[ k ]);
									}
									convBuf1[ j ] = gain * buf3[ k ];
								}
							// ---- gain already known ----
							} else {
								for( int j = 0, k = 0; j < chunkLen; j++, k += 2 ) {
									convBuf1[ j ] = gain * buf3[ k ];
								}
							}
						}
					}
				}
			// .... progress ....
				progOff += chunkLen;
				setProgression( (float) progOff / (float) progLen );

				reOutF.writeFrames( buf1, 0, chunkLen );
			// .... progress ....
				progOff += chunkLen;
				setProgression( (float) progOff / (float) progLen );

				if( imOutF != null ) {
					imOutF.writeFrames( buf2, 0, chunkLen );
				// .... progress ....
					progOff += chunkLen;
					setProgression( (float) progOff / (float) progLen );
				}
				framesWritten += chunkLen;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			reOutF.close();
			reOutF = null;
			if( imOutF != null ) {
				imOutF.close();
				imOutF = null;
			}

		// ---- Finish ----

			// inform about clipping/ low level
			maxAmp *= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			buf1	= null;
			buf2	= null;
			buf3	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		buf1	= null;
		buf2	= null;
		buf3	= null;

		if( reInF != null ) {
			reInF.cleanUp();
			reInF = null;
		}
		if( imInF != null ) {
			imInF.cleanUp();
			imInF = null;
		}
		if( reOutF != null ) {
			reOutF.cleanUp();
			reOutF = null;
		}
		if( imOutF != null ) {
			imOutF.cleanUp();
			imOutF = null;
		}
		if( floatF != null ) {
			for( int ch = 0; ch < floatF.length; ch++ ) {
				for( int i = 0; i < 4; i++ ) {
					if( floatF[ ch ][ i ] != null ) {
						floatF[ ch ][ i ].cleanUp();
						floatF[ ch ][ i ] = null;
					}
					if( tempFile[ ch ][ i ] != null ) {
//System.out.println( "deleting : " + tempFile[ch][i].getName() );
						tempFile[ ch ][ i ].delete();
						tempFile[ ch ][ i ] = null;
					}
				}
			}
		}
	} // process()

// -------- private Methoden --------

/**
 *	One-dimensional Fourier transform of a large data set stored on external media.
 *	len must be a power of 2. file[ 0...3 ] contains the stream pointers to 4 temporary files,
 *	each large enough to hold half of the data. The input data must have its first half stored
 *	in file file[ 0 ], its second half in file[ 1 ], in native floating point form.
 *	memAmount real numbers are processed per buffered read or write.
 *	Output: the  first half of the result is stored in file[ 2 ], the second half in file[ 3 ].
 *
 *	@param	file		0 = first half input,  1 = second half input
 *						2 = first half output, 3 = second half output
 *	@param	len			complex fft length (power of 2!)
 *	@param	dir			1 = forward, -1 = inverse transform (multiply by freqShift for special effect!)
 *	@param	buf			3 gleichgrosse float-puffer
 *	@param	progEnd		(progress-indicator)
 *
 *	Adaption von Numerical Recipes
 */
	protected void storageFFT( FloatFile[] file, File[] tempFile, long len, float dir, float[][] buf, float progEnd )
	throws IOException
	{
		int		g, h, i, j, k, kk, ks, kd;
		float	tempRe, tempIm;
		float[]	buf1, buf2, buf3;
		double	wRe, wIm, wpRe, wpIm, tempW, theta, thetaBase;
		float	wReF, wImF;	// reusable float versions
		int[]	indexMap	= { 1, 0, 3, 2 };
		int[]	fileIndex	= new int[ 4 ];
		int		memAmount;
		int		framesRead, framesWritten;
		long	mMax, step, numSteps, halfSteps, jk, n1, n2, kc;

		float		progOff			= getProgression();
		float		progWeight		= progEnd - progOff;
		int			prog			= 0;
		int			progLen;

		memAmount	= buf[ 0 ].length;
		buf1		= buf[ 0 ];
		buf2		= buf[ 1 ];
		buf3		= buf[ 2 ];

		kc		= 0;
		mMax	= len;
		numSteps= len/memAmount;
		halfSteps= numSteps >> 1;
		thetaBase= dir * Math.PI;
// System.out.println( "memAmount" +memAmount+"; len "+len+"; numSteps "+numSteps+"; halfSteps "+halfSteps );

		// calc prog len by simulation (can't figure it out in abstract ;)
		prog		= 0;
		progLen		= 0;
		n2			= len;
		kd			= memAmount >> 1;
		jk			= len;
		do {
			progLen	+= Math.max( 1, halfSteps ) * (memAmount << 2) * (halfSteps == 0 ? 1 : 2);
			jk >>= 1;
			n2 >>= 1;
			if( n2 > memAmount ) {
				n1		 = 2 * ((numSteps + (n2/memAmount) - 1) / (n2/memAmount)) * ((n2 + memAmount - 1) / memAmount);
				progLen	+= n1 * (memAmount << 1);
			}
		} while( n2 >= memAmount );
		j = 0;
		do {
			progLen += 2 * numSteps * memAmount;
			ks		 = kd;
			kd	   >>= 1;
			for( i = 0; i < 2; i++ ) {
				for( step = 0; step < numSteps; step++ ) {
					kk	= 0;
					k	= ks;
					do {
						h	= (k - kk + 1) & ~1;
						j  += h;
						kk += h;
						kk += ks;
						k	= kk + ks;
					} while( kk < memAmount );
					if( j >= memAmount ) {
						progLen += memAmount << 1;
						j = 0;
					}
				}
			}
			jk >>= 1;
		} while( jk > 1 );
		progWeight /= progLen;

		n2		= len;
		kd		= memAmount >> 1;
		jk		= len;
		rewind( file, tempFile, fileIndex );
//		file	= rewind( file, fileIndex );

		// The first phase of the transform starts here.
		do {		// Start of the computing pass.
			theta	= thetaBase / (len/mMax);
			tempW	= Math.sin( theta/2 );
			wpRe	= -2.0 * tempW * tempW;
			wpIm	= Math.sin( theta );
			wRe		= 1.0;
			wIm		= 0.0;
			wReF	= 1.0f;
			wImF	= 0.0f;
			mMax  >>= 1;

			for( i = 0; threadRunning && (i < 2); i++ ) {
				step = 0;
				do {
					for( framesRead = 0; threadRunning && (framesRead < memAmount); ) {
						g = Math.min( 32768, memAmount - framesRead );
						file[ fileIndex[ 0 ]].readFloats( buf1, framesRead, g );
						framesRead	+= g;
						prog		+= g;
					// .... progress ....
						setProgression( (float) prog * progWeight + progOff );
					}
					for( framesRead = 0; threadRunning && (framesRead < memAmount); ) {
						g = Math.min( 32768, memAmount - framesRead );
						file[ fileIndex[ 1 ]].readFloats( buf2, framesRead, g );
						framesRead	+= g;
						prog		+= g;
					// .... progress ....
						setProgression( (float) prog * progWeight + progOff );
					}
				// .... check running ....
					if( !threadRunning ) return;

					for( j = 0; j < memAmount; j += 2 ) {
						h			= j + 1;
						// double operations more accurate but probably slower ;(
						tempRe		= wReF * buf2[ j ] - wImF * buf2[ h ];
						tempIm		= wImF * buf2[ j ] + wReF * buf2[ h ];
//						tempRe		= (float) (wRe * buf2[ j ] - wIm * buf2[ h ]);
//						tempIm		= (float) (wIm * buf2[ j ] + wRe * buf2[ h ]);
						buf2[ j ]	= buf1[ j ] - tempRe;
						buf1[ j ]  += tempRe;
						buf2[ h ]	= buf1[ h ] - tempIm;
						buf1[ h ]  += tempIm;
					}

					kc += kd;
					if( kc == mMax ) {
						kc		= 0;
						tempW	= wRe;
						wRe	   += tempW * wpRe - wIm * wpIm;
						wIm	   += tempW * wpIm + wIm * wpRe;
						wReF	= (float) wRe;
						wImF	= (float) wIm;
					}

					for( framesWritten = 0; threadRunning && (framesWritten < memAmount); ) {
						g = Math.min( 32768, memAmount - framesWritten );
						file[ fileIndex[ 2 ]].writeFloats( buf1, framesWritten, g );
						framesWritten	+= g;
						prog			+= g;
					// .... progress ....
						setProgression( (float) prog * progWeight + progOff );
					}
					for( framesWritten = 0; threadRunning && (framesWritten < memAmount); ) {
						g = Math.min( 32768, memAmount - framesWritten );
						file[ fileIndex[ 3 ]].writeFloats( buf2, framesWritten, g );
						framesWritten	+= g;
						prog			+= g;
					// .... progress ....
						setProgression( (float) prog * progWeight + progOff );
					}

				} while( threadRunning && (++step < halfSteps) );
			// .... check running ....
				if( !threadRunning ) return;

				if( (i == 0) && (n2 != len) && (n2 == memAmount) ) {
					fileIndex[ 0 ] = indexMap[ fileIndex[ 0 ]];
					fileIndex[ 1 ] = fileIndex[ 0 ];
				}

				if( halfSteps == 0 ) break;
			}
		// .... check running ....
			if( !threadRunning ) return;

			rewind( file, tempFile, fileIndex );		// Start of the permutation pass.
//			file	= rewind( file, fileIndex );		// Start of the permutation pass.
			jk >>= 1;
			if( jk == 1 ) {
				mMax	= len;
				jk		= len;
System.out.println( "We never get here?!!" );
			}

			n2 >>= 1;
			if( n2 > memAmount ) {
				for( i = 0; i < 2; i++ ) {
					for( step = 0; step < numSteps; step += n2/memAmount ) {
						for( n1 = 0; n1 < n2; n1 += memAmount ) {
							for( framesRead = 0; threadRunning && (framesRead < memAmount); ) {
								g = Math.min( 32768, memAmount - framesRead );
								file[ fileIndex[ 0 ]].readFloats( buf1, framesRead, g );
								framesRead		+= g;
								prog			+= g;
							// .... progress ....
								setProgression( (float) prog * progWeight + progOff );
							}
							for( framesWritten = 0; threadRunning && (framesWritten < memAmount); ) {
								g = Math.min( 32768, memAmount - framesWritten );
								file[ fileIndex[ 2 ]].writeFloats( buf1, framesWritten, g );
								framesWritten	+= g;
								prog			+= g;
							// .... progress ....
								setProgression( (float) prog * progWeight + progOff );
							}
						// .... check running ....
							if( !threadRunning ) return;
						}
						fileIndex[ 2 ] = indexMap[ fileIndex[ 2 ]];
					}
					fileIndex[ 0 ] = indexMap[ fileIndex[ 0 ]];
				}
				rewind( file, tempFile, fileIndex );
//				file	= rewind( file, fileIndex );

			} else if( n2 == memAmount) {
				fileIndex[ 1 ] = fileIndex[ 0 ];
			}

		} while( threadRunning && (n2 >= memAmount) );
	// .... check running ....
		if( !threadRunning ) return;

// System.out.println( "part 2" );
		j = 0;
		// The second phase of the transform starts here. Now, the remaining permutations
		// are sufficiently local to be done in place.

		do {
			theta	= thetaBase / (len/mMax);
			tempW	= Math.sin( theta/2 );
			wpRe	= -2.0 * tempW * tempW;
			wpIm	= Math.sin( theta );
			wRe		= 1.0;
			wIm		= 0.0;
			wReF	= 1.0f;
			wImF	= 0.0f;
			mMax  >>= 1;
			ks		= kd;
			kd	  >>= 1;

			for( i = 0; threadRunning && (i < 2); i++ ) {
				for( step = 0; threadRunning && (step < numSteps); step++ ) {
					for( framesRead = 0; threadRunning && (framesRead < memAmount); ) {
						g = Math.min( 32768, memAmount - framesRead );
						file[ fileIndex[ 0 ]].readFloats( buf3, framesRead, g );
						framesRead	+= g;
						prog		+= g;
					// .... progress ....
						setProgression( (float) prog * progWeight + progOff );
					}
				// .... check running ....
					if( !threadRunning ) return;

					kk	= 0;
					k	= ks;

					do {
						h			= kk + ks + 1;
						tempRe		= wReF * buf3[ kk + ks ] - wImF * buf3[ h ];
						tempIm		= wImF * buf3[ kk + ks ] + wReF * buf3[ h ];
//						tempRe		= (float) (wRe * buf3[ kk + ks ] - wIm * buf3[ h ]);
//						tempIm		= (float) (wIm * buf3[ kk + ks ] + wRe * buf3[ h ]);
						buf1[ j ]	= buf3[ kk ]	+ tempRe;
						buf2[ j ]	= buf3[ kk ]	- tempRe;
						buf1[ ++j ]	= buf3[ ++kk ]	+ tempIm;
						buf2[ j++ ]	= buf3[ kk++ ]	- tempIm;

						if( kk < k ) continue;
						kc += kd;
						if( kc == mMax ) {
							kc		= 0;
							tempW	= wRe;
							wRe	   += tempW * wpRe - wIm * wpIm;
							wIm	   += tempW * wpIm + wIm * wpRe;
							wReF	= (float) wRe;
							wImF	= (float) wIm;
						}

						kk += ks;
//						k	= kk + ks;
// 					} while( kk < memAmount );
						if( kk >= memAmount ) break;
						k	= kk + ks;
					} while( true );

					// flush
					if( j >= memAmount ) {
						for( framesWritten = 0; threadRunning && (framesWritten < memAmount); ) {
							g = Math.min( 32768, memAmount - framesWritten );
							file[ fileIndex[ 2 ]].writeFloats( buf1, framesWritten, g );
							framesWritten	+= g;
							prog			+= g;
						// .... progress ....
							setProgression( (float) prog * progWeight + progOff );
						}
						for( framesWritten = 0; threadRunning && (framesWritten < memAmount); ) {
							g = Math.min( 32768, memAmount - framesWritten );
							file[ fileIndex[ 3 ]].writeFloats( buf2, framesWritten, g );
							framesWritten	+= g;
							prog			+= g;
						// .... progress ....
							setProgression( (float) prog * progWeight + progOff );
						}
					// .... check running ....
						if( !threadRunning ) return;
						j = 0;
					}
				} // for steps
				fileIndex[ 0 ] = indexMap[ fileIndex[ 0 ]];
			} // for( 1 ... 2 )
		// .... check running ....
//			if( !threadRunning ) return;

			rewind( file, tempFile, fileIndex );
//			file	= rewind( file, fileIndex );
			jk >>= 1;
		} while( threadRunning && (jk > 1) );

		buf1	= null;
		buf2	= null;
		buf3	= null;
//		file	= null;
	}
	
	// Utility used by storageFFT. Rewinds and renumbers the four files.
	protected void rewind( FloatFile[] file, File[] tempFile, int[] fileIndex )
	throws IOException
	{
		int			i;
		FloatFile	tempF;
		File		tempF2;

		for( i = 0; i < 4; i++ )
		{
			file[ i ].seekFloat( 0 );
		}
		tempF			= file[ 1 ];
		file[ 1 ]		= file[ 3 ];
		file[ 3 ]		= tempF;
		tempF			= file[ 0 ];
		file[ 0 ]		= file[ 2 ];
		file[ 2 ]		= tempF;
		
		tempF2			= tempFile[ 1 ];
		tempFile[ 1 ]	= tempFile[ 3 ];
		tempFile[ 3 ]	= tempF2;
		tempF2			= tempFile[ 0 ];
		tempFile[ 0 ]	= tempFile[ 2 ];
		tempFile[ 2 ]	= tempF2;
	
		fileIndex[ 0 ]	= 2;
		fileIndex[ 1 ]	= 3;
		fileIndex[ 2 ]	= 0;
		fileIndex[ 3 ]	= 1;
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_IMINPUTFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMINPUT ]);
		}
		c = gui.getItemObj( GG_IMOUTPUTFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMOUTPUT ]);
		}
	}
}
// class FourierDlg
