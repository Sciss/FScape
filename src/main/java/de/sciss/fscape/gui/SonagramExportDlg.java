/*
 *  SonagramExportDlg.java
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
 *		20-Dec-08	created
 */

package de.sciss.fscape.gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.io.ImageFile;
import de.sciss.fscape.io.ImageStream;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.ConstQ;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.MathUtil;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Processing module for approaching a file (fit input)
 *	throw evolution using a genetic algorithm.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.73, 24-May-09
 *  
 *  @todo		image resolution (8-bit vs 16-bit) is not saved
 */
public class SonagramExportDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_OUTPUTFILE			= 0;		// pr.text
	private static final int PR_INPUTFILE			= 1;
	private static final int PR_MAXFFTSIZE			= 0;		// pr.intg
	private static final int PR_MINFREQ				= 0;		// pr.para
	private static final int PR_MAXFREQ				= 1;
	private static final int PR_BANDSPEROCT			= 2;
	private static final int PR_TIMERES				= 3;
	private static final int PR_SIGNALCEIL			= 4;
	private static final int PR_NOISEFLOOR			= 5;
//	private static final int PR_READMARKERS			= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_MAXFFTSIZE		= "MaxFFTSize";
	private static final String PRN_MINFREQ			= "MinFreq";
	private static final String PRN_MAXFREQ			= "MaxFreq";
	private static final String PRN_BANDSPEROCT		= "BandsPerOct";
	private static final String PRN_TIMERES			= "TimeReso";
	private static final String PRN_SIGNALCEIL		= "SignalCeil";
	private static final String PRN_NOISEFLOOR		= "NoiseFloor";
	
	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 5 /* 8192 */ };
	private static final String	prIntgName[]	= { PRN_MAXFFTSIZE };
	private static final Param	prPara[]		= { null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_MINFREQ, PRN_MAXFREQ, PRN_BANDSPEROCT, PRN_TIMERES, PRN_SIGNALCEIL, PRN_NOISEFLOOR };
//	private static final boolean prBool[]		= { false };
//	private static final String	prBoolName[]	= { PRN_READMARKERS };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_MAXFFTSIZE		= GG_OFF_CHOICE		+ PR_MAXFFTSIZE;
	private static final int GG_MINFREQ			= GG_OFF_PARAMFIELD	+ PR_MINFREQ;
	private static final int GG_MAXFREQ			= GG_OFF_PARAMFIELD	+ PR_MAXFREQ;
	private static final int GG_BANDSPEROCT		= GG_OFF_PARAMFIELD	+ PR_BANDSPEROCT;
	private static final int GG_TIMERES			= GG_OFF_PARAMFIELD	+ PR_TIMERES;
	private static final int GG_SIGNALCEIL		= GG_OFF_PARAMFIELD	+ PR_SIGNALCEIL;
	private static final int GG_NOISEFLOOR		= GG_OFF_PARAMFIELD	+ PR_NOISEFLOOR;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_MONO		= "Audio file must be monophonic";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SonagramExportDlg()
	{
		super( "Sonogram Export" );
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
			static_pr.para[ PR_MINFREQ ]		= new Param(    32.0, Param.ABS_HZ );
			static_pr.para[ PR_MAXFREQ ]		= new Param( 18000.0, Param.ABS_HZ );
			static_pr.para[ PR_BANDSPEROCT ]	= new Param(    12.0, Param.NONE );
			static_pr.para[ PR_TIMERES ]		= new Param(    20.0, Param.ABS_MS );
			static_pr.para[ PR_SIGNALCEIL ]		= new Param(     0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_NOISEFLOOR ]		= new Param(   -96.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		final GridBagConstraints	con;
		final PathField				ggInputFile, ggOutputFile;
		final PathField[]			ggInputs;
		final ParamField			ggMinFreq, ggMaxFreq, ggBandsPerOct, ggTimeRes;
		final ParamField			ggSignalCeil, ggNoiseFloor;
		final JComboBox				ggMaxFFTSize;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

//		final ItemListener il = new ItemListener() {
//			public void itemStateChanged( ItemEvent e )
//			{
//				int	ID = gui.getItemID( e );
//
//				switch( ID ) {
//				case GG_READMARKERS:
//					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
//					reflectPropertyChanges();
//					break;
//				}
//			}
//		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input sound file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Audio input", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output image file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_IMAGE );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Sono$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Image output", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );

	// -------- Plot Settings --------
	gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
	
		ggMinFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Lowest Frequency:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMinFreq, GG_MINFREQ, null );

		ggBandsPerOct	= new ParamField( new ParamSpace( 1, /* 96 */ 32768, 1, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Bands Per Octave:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggBandsPerOct, GG_BANDSPEROCT, null );

		ggMaxFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Highest Frequency:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMaxFreq, GG_MAXFREQ, null );

		ggTimeRes		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Max. Time Resolution:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggTimeRes, GG_TIMERES, null );

		ggSignalCeil	= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Signal Ceiling:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggSignalCeil, GG_SIGNALCEIL, null );
		
		ggMaxFFTSize	= new JComboBox();
		for( int i = 256; i <= 32768; i <<= 1 ) {
			ggMaxFFTSize.addItem( String.valueOf( i ));
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Max. FFT Size:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggMaxFFTSize, GG_MAXFFTSIZE, null );
		
		ggNoiseFloor	= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Noise Floor:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggNoiseFloor, GG_NOISEFLOOR, null );
		
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
		
		AudioFile				inF				= null;
		final AudioFileDescr	inDescr;
		final int				inChanNum;
		final long				inLength;

//		final Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz

		final PathField			ggOutput;

		int						chunkLen;
		
		final ConstQ			constQ;
		final float				boost		= 1f; // 1000f;
		
		final double			minFreq		= pr.para[ PR_MINFREQ ].val;
		final double			maxFreq		= pr.para[ PR_MAXFREQ ].val;
		final double			timeRes		= pr.para[ PR_TIMERES ].val;
		final int				bandsPerOct	= (int) pr.para[ PR_BANDSPEROCT ].val;
		final int				maxFFTSize	= 256 << pr.intg[ PR_MAXFFTSIZE ];
		final boolean			color		= false;
		
		final int				bitsPerSmp;
		ImageFile				outF		= null;
		final ImageStream		imgStream;
		final byte[]			row;
		final int				overlapSize;
		
		final int				width, height;
		final int				inBufSize;
		final float[][]			inBuf;
		final int				fftSize;
		final int				stepSize;
		final int				numKernels;
		final float[]			kernel;
//		final float[]			hsb	= new float[ 3 ];
		final double			signalCeil	= pr.para[ PR_SIGNALCEIL ].val; // (Param.transform( pr.para[ PR_SIGNALCEIL ], Param.ABS_AMP, ampRef, null )).val;
		final double			noiseFloor	= pr.para[ PR_NOISEFLOOR ].val; // (Param.transform( pr.para[ PR_NOISEFLOOR ], Param.ABS_AMP, ampRef, null )).val;
		final double			dynamic		= signalCeil - noiseFloor;
		int						rgb;
		int						winSize, inOff;
		long					framesRead;
//		float					brightness;
		
topLevel: try {

		// ---- open input, output; init ----
			// ptrn input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inDescr			= inF.getDescr();
			inChanNum		= inDescr.channels;
			inLength		= inDescr.length;
			// this helps to prevent errors from empty files!
			if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
if( inChanNum != 1 ) throw new EOFException( ERR_MONO );
// .... check running ....
			if( !threadRunning ) break topLevel;
	
//			if( inChanNum > 1 ) {
//				System.out.println( "WARNING: Multichannel input. Using mono mix for mosaic correlation!" );
//			}
			
			// ---- further inits ----
			
			constQ				= new ConstQ();
			constQ.setSampleRate( inDescr.rate );
			constQ.setMinFreq( (float) minFreq );
			constQ.setMaxFreq( (float) maxFreq );
			constQ.setBandsPerOct( bandsPerOct );
			constQ.setMaxFFTSize( maxFFTSize );
			constQ.setMaxTimeRes( (float) timeRes );
			constQ.createKernels();
			fftSize				= constQ.getFFTSize();
			numKernels			= constQ.getNumKernels();

			winSize				= fftSize; // << 1;
			stepSize			= (int) (AudioFileDescr.millisToSamples( inDescr, timeRes ) + 0.5);
			overlapSize			= fftSize - stepSize;
			height				= (int) ((inLength + stepSize - 1) / stepSize);
			width				= numKernels;
			
//System.out.println( "w " + width + "; h " + height + "; winSize " + winSize + "; inLength " + inLength );

			ggOutput				= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outF					= new ImageFile( pr.text[ PR_OUTPUTFILE ], GenericFile.MODE_OUTPUT | ggOutput.getType() );
			imgStream				= new ImageStream();
			imgStream.bitsPerSmp	= 8;	// ??? fillStream might not work correctly?
			ggOutput.fillStream( imgStream );
			imgStream.width			= width;
			imgStream.height		= height;
			imgStream.smpPerPixel	= /* color ? 3 :*/ 1;
			bitsPerSmp				= imgStream.bitsPerSmp;
			outF.initWriter( imgStream );
			row						= outF.allocRow();

			inBufSize			= Math.max( 8192, fftSize );
			inBuf				= new float[ inChanNum ][ inBufSize ];
			kernel				= new float[ numKernels ];

			progLen				= height;
			progOff				= 0;

		// ----==================== processing loop ====================----
		
			framesRead			= 0;
			inOff				= 0;
			
//final java.util.Random rnd = new java.util.Random();
			
			for( int y = 0; y < height; y++ ) {
				if( inOff < 0 ) {
					inF.seekFrame( Math.min( inF.getFrameNum(), inF.getFramePosition() - inOff ));
					inOff = 0;
				}
				// read
				chunkLen = (int) Math.min( inLength - framesRead, winSize - inOff );

//System.out.println( "readFrames " + inOff + " -> " + chunkLen );
				
				inF.readFrames( inBuf, inOff, chunkLen );
				if( (inOff + chunkLen) < winSize ) {
					Util.clear( inBuf, inOff + chunkLen, winSize - (inOff + chunkLen) );
				}
				
				// transform
				constQ.transform( inBuf[ 0 ], 0, winSize, kernel, 0 );
				for( int x = 0; x < width; x++ ) {
					kernel[ x ] = (float) ((Math.min( signalCeil, (Math.max( noiseFloor,
					    MathUtil.linearToDB( kernel[ x ] * boost )))) - noiseFloor) / dynamic);
//					kernel[ x ] = rnd.nextFloat();
				}
				
				if( color ) {
					throw new IllegalStateException( "Color not yet implemented" );
//					if( bitsPerSmp == 8 ) {
//						for( int x = 0; x < width; x++ ) {
//						
//						}
//					} else {
//						for( int x = 0; x < width; x++ ) {
//							
//						}
//					}
				} else {
					if( bitsPerSmp == 8 ) {
						for( int x = 0; x < width; x++ ) {
							row[ x ] = (byte) (kernel[ x ] * 0xFF + 0.5f);
						}
					} else {
						for( int x = 0, cnt = 0; x < width; x++ ) {
							rgb				= (int) (kernel[ x ] * 0xFFFF + 0.5f);
							row[ cnt++ ]	= (byte) (rgb >> 8);  
							row[ cnt++ ]	= (byte) rgb;  
						}
					}
				}
				
				outF.writeRow( row );
			
				// handle overlap
//System.out.println( "inBuf : " + inBuf[0].length + "; stepSize = " + stepSize + "; overlap = " + overlapSize );
				if( overlapSize > 0 ) Util.copy( inBuf, stepSize, inBuf, 0, overlapSize );
				inOff = overlapSize;
				
				framesRead += chunkLen;
				
				progOff++;
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;
			} // for x
			
			inF.close();
			inF		= null;
			outF.close();
			outF	= null;
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) outF.cleanUp();
		if( inF != null ) inF.cleanUp();
	} // process()

// -------- private Methoden --------
}