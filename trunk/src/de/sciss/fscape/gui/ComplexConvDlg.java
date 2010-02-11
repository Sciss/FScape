/*
 *  ComplexConvDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
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
 *		29-May-05	uses new (audio) temp files, greatly speed up, bug fixes
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  Processing module for convolution of long or complex files
 *	using harddisk based FFT.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ComplexConvDlg
extends DocumentFrame
{
// -------- public Variablen --------

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_REINPUTFILE		= 0;		// pr.text
	private static final int PR_IMINPUTFILE		= 1;
	private static final int PR_REIMPFILE		= 2;
	private static final int PR_IMIMPFILE		= 3;
	private static final int PR_REOUTPUTFILE	= 4;
	private static final int PR_IMOUTPUTFILE	= 5;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_MEMORY			= 1;
	private static final int PR_HASIMINPUT		= 0;		// pr.bool
	private static final int PR_HASIMIMPULSE	= 1;
	private static final int PR_HASIMOUTPUT		= 2;
	private static final int PR_CEPSTRAL1		= 3;
	private static final int PR_CEPSTRAL2		= 4;

	private static final String PRN_REINPUTFILE		= "ReInFile";
	private static final String PRN_IMINPUTFILE		= "ImInFile";
	private static final String PRN_REIMPFILE		= "ReImpFile";
	private static final String PRN_IMIMPFILE		= "ImImpFile";
	private static final String PRN_REOUTPUTFILE	= "ReOutFile";
	private static final String PRN_IMOUTPUTFILE	= "ImOutFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_HASIMINPUT		= "HasImInput";
	private static final String PRN_HASIMIMPULSE	= "HasImImp";
	private static final String PRN_HASIMOUTPUT		= "HasImOutput";
	private static final String PRN_CEPSTRAL1		= "Cepstral1";
	private static final String PRN_CEPSTRAL2		= "Cepstral2";
	private static final String PRN_MEMORY			= "Memory";

	private static final String	prText[]		= { "", "", "", "", "", "" };
	private static final String	prTextName[]	= { PRN_REINPUTFILE, PRN_IMINPUTFILE, PRN_REIMPFILE, PRN_IMIMPFILE,
													PRN_REOUTPUTFILE, PRN_IMOUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_MEMORY };
	private static final boolean	prBool[]	= { false, false, false, false, false };
	private static final String	prBoolName[]	= { PRN_HASIMINPUT, PRN_HASIMIMPULSE, PRN_HASIMOUTPUT,
													PRN_CEPSTRAL1, PRN_CEPSTRAL2 };

	private static final int GG_REINPUTFILE			= GG_OFF_PATHFIELD	+ PR_REINPUTFILE;
	private static final int GG_IMINPUTFILE			= GG_OFF_PATHFIELD	+ PR_IMINPUTFILE;
	private static final int GG_REIMPFILE			= GG_OFF_PATHFIELD	+ PR_REIMPFILE;
	private static final int GG_IMIMPFILE			= GG_OFF_PATHFIELD	+ PR_IMIMPFILE;
	private static final int GG_REOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_REOUTPUTFILE;
	private static final int GG_IMOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_IMOUTPUTFILE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_MEMORY				= GG_OFF_PARAMFIELD	+ PR_MEMORY;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_HASIMINPUT			= GG_OFF_CHECKBOX	+ PR_HASIMINPUT;
	private static final int GG_HASIMIMPULSE		= GG_OFF_CHECKBOX	+ PR_HASIMIMPULSE;
	private static final int GG_HASIMOUTPUT			= GG_OFF_CHECKBOX	+ PR_HASIMOUTPUT;
//	private static final int GG_CEPSTRAL1			= GG_OFF_CHECKBOX	+ PR_CEPSTRAL1;
//	private static final int GG_CEPSTRAL2			= GG_OFF_CHECKBOX	+ PR_CEPSTRAL2;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private JTextField ggInfo;
	protected ParamField ggMemory;
	protected final long[] guiInLength	= new long[ 4 ];
	protected final int[] inChanNum	= new int[ 4 ];

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ComplexConvDlg()
	{
		super( "Complex Convolution" );
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
			static_pr.para[ PR_MEMORY ]		= new Param( 32.0, Param.NONE );
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

		PathField			ggImInputFile, ggReInputFile, ggReOutputFile, ggImOutputFile;
		JCheckBox			ggHasImInput, ggHasImOutput;
		PathField[]			ggParent1, ggParent2;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_HASIMINPUT:
				case GG_HASIMIMPULSE:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					recalcSteps();
					break;

				case GG_HASIMOUTPUT:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};

		PathListener pathL	= new PathListener() {
			public void pathChanged( PathEvent e ) {
				int	ID	= gui.getItemID( e );
				int i;

				switch( ID ) {
				case GG_REINPUTFILE:
					i = 0;
					break;
				case GG_IMINPUTFILE:
					i = 1;
					break;
				case GG_REIMPFILE:
					i = 2;
					break;
				case GG_IMIMPFILE:
					i = 3;
					break;
				default:
					return;
				}
				
				try {
					AudioFile		af	= AudioFile.openAsRead( e.getPath() );
					AudioFileDescr	afd	= af.getDescr();
					af.close();
					guiInLength[ i ]	= afd.length;
					inChanNum[ i ]	= afd.channels;
				}
				catch( IOException e1 ) {
					guiInLength[ i ]	= 0;
					inChanNum[ i ]	= 0;
				}
				recalcSteps();
			}
		};

		ParamListener paramL= new ParamListener()
		{
			public void paramChanged( ParamEvent e )
			{
				if( e.getSource() == ggMemory ) {
					pr.para[ PR_MEMORY ] = e.getParam();
					recalcSteps();
				}
			}
		};
		
	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggParent1		= new PathField[ 2 ];
		for( int i = 0; i < 2; i++ ) {
			ggReInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
											 "Select real part of input" );
			ggReInputFile.handleTypes( GenericFile.TYPES_SOUND );
			ggParent1[ i ]	= ggReInputFile;
			con.gridwidth	= 1;
			con.weightx		= 0.1;
			gui.addLabel( new JLabel( "Input "+(i+1)+" [Real]", SwingConstants.RIGHT ));
			con.gridwidth	= GridBagConstraints.REMAINDER;
			con.weightx		= 0.9;
			gui.addPathField( ggReInputFile, GG_REINPUTFILE + (i<<1), pathL );
	
			ggImInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
											 "Select imaginary part of input" );
			ggImInputFile.handleTypes( GenericFile.TYPES_SOUND );
			ggParent2		= new PathField[ 1 ];
			ggParent2[ 0 ]	= ggReInputFile;
			ggImInputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );
			ggHasImInput	= new JCheckBox( "Input "+(i+1)+" [Imaginary]" );
			con.gridwidth	= 1;
			con.weightx		= 0.1;
			final int tmpAnchor = con.anchor;
			con.anchor		= GridBagConstraints.EAST;
			gui.addCheckbox( ggHasImInput, GG_HASIMINPUT + (i<<1), il );
			con.anchor		= tmpAnchor;
			con.gridwidth	= GridBagConstraints.REMAINDER;
			con.weightx		= 0.9;
			gui.addPathField( ggImInputFile, GG_IMINPUTFILE + (i<<1), pathL );
			
//			ggCepstral		= new JCheckBox();
//			con.gridwidth	= 1;
//			con.weightx		= 0.1;
//			gui.addLabel( new JLabel( "Cepstral", SwingConstants.RIGHT ));
//			con.gridwidth	= GridBagConstraints.REMAINDER;
//			gui.addCheckbox( ggCepstral, GG_CEPSTRAL1 + i, il );
		}
	
		ggReOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output for real part" );
		ggReOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output [Real]", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggReOutputFile, GG_REOUTPUTFILE, pathL );
		gui.registerGadget( ggReOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggReOutputFile.getResGadget(), GG_OUTPUTRES );

		ggImOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE, "Select output for imaginary part" );
//		ggImOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImOutput	= new JCheckBox( "Output [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		final int tmpAnchor = con.anchor;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImOutput, GG_HASIMOUTPUT, il );
		con.anchor		= tmpAnchor;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImOutputFile, GG_IMOUTPUTFILE, pathL );

		ggParent2		= new PathField[ 1 ];
		ggParent2[ 0 ]	= ggReOutputFile;
		ggReOutputFile.deriveFrom( ggParent1, "$D0$B0Con$B1$E" );
		ggImOutputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );
		
		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, paramL );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

		ggMemory		= new ParamField( new ParamSpace( 1.0, 2047.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mem.alloc. [MB]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMemory, GG_MEMORY, paramL );

		ggInfo			= new JTextField( 32 );
		ggInfo.setEditable( false );
		ggInfo.setBackground( null );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "\u2192", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addGadget( ggInfo, GG_OFF_OTHER + 0 );
		
		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
		
		for( int i = PR_REINPUTFILE, j = 0; i < PR_IMIMPFILE; i++, j++ ) {
			try {
				AudioFile		af	= AudioFile.openAsRead( new File( pr.text[ i ]));
				AudioFileDescr	afd	= af.getDescr();
				af.close();
				guiInLength[ j ]	= afd.length;
				inChanNum[ j ]	= afd.channels;
			}
			catch( IOException e1 ) {
				guiInLength[ j ]	= 0;
				inChanNum[ j ]	= 0;
			}
		}
		recalcSteps();
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
		int					off, len, chunkLength;
		float				f1;
		boolean				imag;
		long				progOff, progLen;
		float				PIf			= (float) Math.PI; // (Math.PI / 32);
		
		// io
		AudioFile[]			reInF			= new AudioFile[ 2 ];
		AudioFile[]			imInF			= new AudioFile[ 2 ];
		AudioFile			reOutF			= null;
		AudioFile			imOutF			= null;
		AudioFileDescr[]	reInStream		= new AudioFileDescr[ 2 ];
		AudioFileDescr[]	imInStream		= new AudioFileDescr[ 2 ];
		AudioFileDescr		reOutStream		= null;
		AudioFileDescr		imOutStream		= null;
		AudioFile[]			tempF			= new AudioFile[ 4 ];	// 0,1 = inputs complex-interlvd; 2=reOut, 3=imOut
		AudioFile			tempFe;
		float[][]			inBuf;
		float[][]			fftBuf;
		float[]				convBuf1, convBuf2;

		PathField			ggOutput;

		// Synthesize
		Param				ampRef;
		float				gain;

		int					outChanNum;
		long				outLength;
		long[]				inLength		= new long[ 2 ];
//		int[]				inChanNum		= new int[ 2 ];
		long				framesRead, framesWritten;
		int					fullFFTsize, convLen, gainLen;

		float				maxAmp			= 0.0f;

		boolean				autoConv;	// both inputs the same => speed up
		
		Descriptor			d;

topLevel: try {
			
			autoConv = (pr.text[ PR_REINPUTFILE ].equals( pr.text[ PR_REIMPFILE ])) &&
					   (pr.bool[ PR_HASIMINPUT ] == pr.bool[ PR_HASIMIMPULSE ]) &&
					   (!pr.bool[ PR_HASIMINPUT ] || (pr.text[ PR_IMINPUTFILE ].equals( pr.text[ PR_IMIMPFILE ]))) &&
					   (pr.bool[ PR_CEPSTRAL1 ] == pr.bool[ PR_CEPSTRAL2 ]);
		
		// ---- open inputs ----
			for( int i = 0, j = 0; i < (autoConv ? 1 : 2); i++, j += 2 ) {
				reInF[i]		= AudioFile.openAsRead( new File( pr.text[ PR_REINPUTFILE + j ]));
				reInStream[i]	= reInF[i].getDescr();
				inChanNum[i]	= reInStream[i].channels;
				inLength[i]		= reInStream[i].length;
				if( pr.bool[ PR_HASIMINPUT + i ]) {
					imInF[i]	= AudioFile.openAsRead( new File( pr.text[ PR_IMINPUTFILE + j ]));
					imInStream[i]= imInF[i].getDescr();
					if( imInStream[i].channels != inChanNum[i] ) throw new IOException( ERR_COMPLEX );
					inLength[i]	= Math.min( inLength[i], imInStream[i].length );
				}
				// this helps to prevent errors from empty files!
				if( (inLength[i] * inChanNum[i]) < 1 ) throw new EOFException( ERR_EMPTY );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			if( autoConv ) {
				inChanNum[1]	= inChanNum[0];
				inLength[1]		= inLength[0];
				reInStream[1]	= reInStream[0];
				imInStream[1]	= imInStream[0];
			}

			outChanNum	= Math.max( inChanNum[0], inChanNum[1] );

		// ---- calc lengths ----

			d			= initDescriptor( inLength, outChanNum );

		// ---- open output ----
			ggOutput	= (PathField) gui.getItemObj( GG_REOUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			reOutStream	= new AudioFileDescr( reInStream[0] );
			ggOutput.fillStream( reOutStream );
			reOutStream.channels = outChanNum;
			reOutF		= AudioFile.openAsWrite( reOutStream );

			if( pr.bool[ PR_HASIMOUTPUT ]) {
				imOutStream	= new AudioFileDescr( reInStream[0] );
				ggOutput.fillStream( imOutStream );
				imOutStream.channels	= outChanNum;
				imOutStream.file		= new File( pr.text[ PR_IMOUTPUTFILE ]);
				imOutF					= AudioFile.openAsWrite( imOutStream );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			convLen			= d.inputLen[0] + d.inputLen[1] - 1;
			outLength		= inLength[0] + inLength[1] - 1;

			fullFFTsize		= d.complex ? (d.fftSize << 1) : d.fftSize;
			fftBuf			= new float[ outChanNum ][ fullFFTsize + 2 ];
			inBuf			= new float[ outChanNum ][ 8192 ];

// System.out.println( "inLen0 "+inLength[0]+"/step"+inputLen[0]+"; inLen1 "+inLength[1]+"/step"+inputLen[1]+" ; out "+outLength );
//System.out.println( "cnvLen "+convLen+"; fftSize "+d.fftSize+" (full "+fullFFTsize+"); steps0 "+d.steps[0]+"/steps1 "+d.steps[1]+"/tot "+d.totalSteps );
		// ---- Progress Length ----
			progOff		= 0;
			progLen		= pr.bool[ PR_HASIMOUTPUT ] ? (outLength << 1) : outLength;		// normalize
			for( int i = 0; i < (autoConv ? 1 : 2); i++ ) {		// per channel: read input, transform, save
				progLen	+= (inLength[i] + (long) fullFFTsize * d.steps[i] * 2);
//				progLen	+= inChanNum[i] * ((long) inLength[i] + (long) fullFFTsize * steps[i] * 2);
			}
//			for( int ch = 0; ch < outChanNum; ch++ ) {
				long lo1, lo2 = 0;
				for( int step0 = 0; step0 < d.steps[0]; step0++ ) {
					for( int step1 = 0; step1 < d.steps[1]; step1++ ) {
						progLen += 3*fullFFTsize;								// load, multiply, transform
						lo1		 = step0 * (long) d.inputLen[0] + step1 * (long) d.inputLen[1];
						progLen += Math.min( lo2 - lo1, convLen );				// overlap-add
						chunkLength	= (int) Math.min( convLen, outLength - lo1 );
						progLen += chunkLength;
						lo2		 = Math.max( lo2, lo1 + chunkLength );
					}
				}
//			}
						
		// ---- create temp files ----
			for( int i = 0; i < 2; i++ ) {		// alloc
				tempF[i]	= createTempFile( reInStream[i] );
			}
			for( int i = 2; i < (d.complex ? 4 : 3); i++ ) {
				tempF[i]	= createTempFile( outChanNum, reInStream[0].rate );
			}

		// ============================== create fft files ==============================

			for( int i = 0; threadRunning && (i < (autoConv ? 1 : 2)); i++ ) {		// for both input files

				reInF[i].seekFrame( 0 );
				imag = pr.bool[ PR_HASIMINPUT + i ] || pr.bool[ PR_CEPSTRAL1 + i ];
				if( imInF[i] != null ) {
					imInF[i].seekFrame( 0 );
				}

				for( framesRead = 0; threadRunning && (framesRead < inLength[i]); ) {

					chunkLength	= (int) Math.min( d.inputLen[i], inLength[i] - framesRead );
				// ---- read input chunk ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len	= Math.min( 8192, chunkLength - off );
						reInF[i].readFrames( inBuf, 0, len );
						if( imag ) {
							for( int ch = 0; ch < inChanNum[i]; ch++ ) {
								convBuf1	= inBuf[ch];
								convBuf2	= fftBuf[ch];
								for( int j = (off << 1), k = 0; k < len; k++, j += 2 ) {
									convBuf2[ j ] = convBuf1[ k ];
								}
								if( imInF[i] != null ) {
									imInF[i].readFrames( inBuf, 0, len );
									for( int j = (off << 1) + 1, k = 0; k < len; k++, j += 2 ) {
										convBuf2[ j ] = convBuf1[ k ];
									}
								}
							}
						} else {
							for( int ch = 0; ch < inChanNum[i]; ch++ ) {
								System.arraycopy( inBuf[ch], 0, fftBuf[ch], off, len );
							}
						}
						framesRead	+= len;
						progOff		+= len;
						off			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

					for( int ch = 0; ch < inChanNum[i]; ch++ ) {
						convBuf2	= fftBuf[ch];
						// zero padding
						if( imag ) {
							for( int j = chunkLength << 1; j < fullFFTsize; ) {
								convBuf2[ j++ ] = 0.0f;
							}
						} else {
							for( int j = chunkLength; j < d.fftSize; ) {
								convBuf2[ j++ ] = 0.0f;
							}
						}

					// ---- transform forward ----
						if( imag ) {
							if( pr.bool[ PR_CEPSTRAL1 + i ]) {			// take log pre-fft for cepstral filtering
								if( pr.bool[ PR_HASIMINPUT + i ]) {
									Fourier.rect2Polar( convBuf2, 0, convBuf2, 0, fullFFTsize );	// Im(ln z)=phi +k2¹
									for( int j = 0; j < fullFFTsize; j+=2 ) {						// Re(ln z)=ln r
										f1 = convBuf2[ j ];
										if( f1 > 1.266416555e-14f ) {
											convBuf2[ j ]  = (float) Math.log( f1 ); // (float) (Math.log( f1 ) / 32);
										//	fftBuf[ j++ ] /= 32;
										} else {
											convBuf2[ j ]  = -32f; // -1.0f;		// c. ln( 2^-47 ) / 32
										//	fftBuf[ j++ ] /= 32;
										}
									}
								} else {								// only real input, i.e. phi=0¡ or 180¡
									for( int j = 0; j < fullFFTsize; ) {
										f1 = convBuf2[ j ];
										if( f1 > 1.266416555e-14f ) {
											convBuf2[ j++ ] = (float) Math.log( f1 ); // (float) (Math.log( f1 ) / 32);
											convBuf2[ j++ ] = 0.0f;
										} else if( f1 < -1.266416555e-14f ) {
											convBuf2[ j++ ] = (float) Math.log( -f1 ); // (float) Math.log( -f1 ) / 32;
											convBuf2[ j++ ] = PIf;
										} else {
											convBuf2[ j++ ] = -32f; // -1.0f;
											convBuf2[ j++ ] = 0.0f;
										}
									}
								}
							}
							Fourier.complexTransform( convBuf2, d.fftSize, Fourier.FORWARD );
						} else {
							Fourier.realTransform( convBuf2, d.fftSize, Fourier.FORWARD );
							if( d.complex ) {		// construct complex conjugate negative spectrum
								for( int k = d.fftSize, j = d.fftSize; k > 0; k -= 2, j += 2 ) {
									convBuf2[ j ]	= convBuf2[ k ];
									convBuf2[ j+1 ]	= -convBuf2[ k+1 ];
								}
							}
						}
					} // for input channels

					chunkLength	 = fullFFTsize;
					progOff		+= chunkLength;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				// .... check running ....
					if( !threadRunning ) break topLevel;

				// ---- write float chunk ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len	= Math.min( 8192, chunkLength - off );
//						tempF[i].writeFloats( fftBuf, off, len );
						tempF[i].writeFrames( fftBuf, off, len );
						progOff			+= len;
						off				+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;
				
				} // for input-frames

				reInF[i].close();
				reInF[i] = null;
				if( imInF[i] != null ) {
					imInF[i].close();
					imInF[i] = null;
				}
			} // for input0/1

		// ============================== create output files ==============================

			framesWritten	= 0;
			for( int step0 = 0; threadRunning && (step0 < d.steps[0]); step0++ ) {
				for( int step1 = 0; threadRunning && (step1 < d.steps[1]); step1++ ) {
		
//					tempFe = tempF[ 0 ][ ch % inChanNum[0] ];
					tempFe = tempF[ 0 ];
//					tempFe.seekFloat( step0 * fullFFTsize );
					tempFe.seekFrame( (long) step0 * (long) fullFFTsize );
					chunkLength	= fullFFTsize;
				// ---- read fft1 chunk ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len			 = Math.min( 8192, chunkLength - off );
//						tempFe.readFloats( fftBuf, off, len );
						tempFe.readFrames( fftBuf, off, len );
						progOff		+= len;
						off			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

//					tempFe = tempF[ autoConv ? 0 : 1 ][ ch % inChanNum[1] ];
					tempFe = tempF[ autoConv ? 0 : 1 ];
//					tempFe.seekFloat( step1 * fullFFTsize );
					tempFe.seekFrame( (long) step1 * (long) fullFFTsize );
				// ---- read + multiply fft2 chunk ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len	= Math.min( 8192, chunkLength - off );
						tempFe.readFrames( inBuf, 0, len );
						for( int ch = outChanNum - 1; ch >= 0; ch-- ) {
							Fourier.complexMult( inBuf[ ch % inChanNum[1] ], 0,
												 fftBuf[ ch % inChanNum[0] ], off,
												 fftBuf[ ch ], off, len );
						}
						progOff		+= len;
						off			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

					for( int ch = 0; ch < outChanNum; ch++ ) {
						convBuf2	= fftBuf[ ch ];
					// ---- transform backward ----
						if( d.complex ) {
							Fourier.complexTransform( convBuf2, d.fftSize, Fourier.INVERSE );
							for( int i = 0; i < 2; i++ ) {		// take post-ifft exp for cepstral filtering
								if( pr.bool[ PR_CEPSTRAL1 + i ]) {
									for( int j = 0; j < fullFFTsize; j+=2 ) {							// |exp z|=exp(Re(z))
										convBuf2[ j ]  = (float) Math.exp( convBuf2[ j ]); // (float) Math.exp( fftBuf[ j ] * 32 );
									}
									Fourier.polar2Rect( convBuf2, 0, convBuf2, 0, fullFFTsize );	// arg(exp z)= Im z
								}
							}
						} else {
							Fourier.realTransform( convBuf2, d.fftSize, Fourier.INVERSE );
						}
					} // for output channels

					chunkLength	 = fullFFTsize;
					progOff		+= chunkLength;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				// .... check running ....
					if( !threadRunning ) break topLevel;

					long tempOff = (long) step0 * (long) d.inputLen[0] + (long) step1 * (long) d.inputLen[1];
//					tempF[ 2 ][ ch ].seekFloat( i );
					tempF[ 2 ].seekFrame( tempOff );
					if( d.complex ) {
//						tempF[ 3 ][ ch ].seekFloat( i );
						tempF[ 3 ].seekFrame( tempOff );
					}
					chunkLength	= (int) Math.min( framesWritten - tempOff, convLen );
				// ---- add old overlap ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len		 = Math.min( 8192, chunkLength - off );
//							tempF[ 2 ][ ch ].readFloats( convBuf1, 0, len );
						tempF[ 2 ].readFrames( inBuf, 0, len );
						if( d.complex ) {
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1	= inBuf[ ch ];
								convBuf2	= fftBuf[ ch ];
								for( int j = 0, k = (off << 1); j < len; j++, k += 2 ) {
									convBuf2[ k ] += convBuf1[ j ];
								}
							}
//								tempF[ 3 ][ ch ].readFloats( convBuf1, 0, len );
							tempF[ 3 ].readFrames( inBuf, 0, len );
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1	= inBuf[ ch ];
								convBuf2	= fftBuf[ ch ];
								for( int j = 0, k = (off << 1) + 1; j < len; j++, k += 2 ) {
									convBuf2[ k ] += convBuf1[ j ];
								}
							}
						} else {
							for( int ch = 0; ch < outChanNum; ch++ ) {
								Util.add( inBuf[ ch ], 0, fftBuf[ ch ], off, len );
							}
						}
						progOff		+= len;
						off			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

//					tempF[ 2 ][ ch ].seekFloat( i );
					tempF[ 2 ].seekFrame( tempOff );
					if( d.complex ) {
//						tempF[ 3 ][ ch ].seekFloat( i );
						tempF[ 3 ].seekFrame( tempOff );
					}
					chunkLength	= (int) Math.min( convLen, outLength - tempOff );
					long tempOff2 = tempOff + chunkLength; // (first as a stop-offset)
					if( step1 < (d.steps[1] - 1) ) {	// don't measure parts that will be updated in the future!
						tempOff2	= Math.min( tempOff2, tempOff + d.inputLen[1] );			// min in this step0
					}
					if( step0 < (d.steps[0] - 1) ) {
						tempOff2	= Math.min( tempOff2, (long) (step0 + 1) * (long) d.inputLen[0] );	// total min
					}
					gainLen = (int) (tempOff2 - tempOff);					// (now offset turned to length)
				// ---- write output chunk ----
					for( off = 0; threadRunning && (off < chunkLength); ) {
						len			 = Math.min( 8192, chunkLength - off );
						if( d.complex ) {												// ------- write cmplx
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1	= inBuf[ ch ];
								convBuf2	= fftBuf[ ch ];
								for( int j = 0, k = (off << 1) + 1; j < len; j++, k += 2 ) {
									convBuf1[ j ] = convBuf2[ k ];
								}
							}
//								tempF[ 3 ][ ch ].writeFloats( convBuf1, 0, len );
							tempF[ 3 ].writeFrames( inBuf, 0, len );
							// measure gain
							if( pr.bool[ PR_HASIMOUTPUT ]) {
								for( int ch = 0; ch < outChanNum; ch++ ) {
									convBuf1	= inBuf[ ch ];
									for( int j = Math.min( len, gainLen ); j > 0; ) {
										f1 = Math.abs( convBuf1[ --j ]);
										if( f1 > maxAmp ) {
											maxAmp = f1;
										}
									}
								}
							}
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1	= inBuf[ ch ];
								convBuf2	= fftBuf[ ch ];
								for( int j = 0, k = (off << 1); j < len; j++, k += 2 ) {
									convBuf1[ j ] = convBuf2[ k ];
								}
							}
//								tempF[ 2 ][ ch ].writeFloats( convBuf1, 0, len );
							tempF[ 2 ].writeFrames( inBuf, 0, len );
							// measure gain
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf1	= inBuf[ ch ];
								for( int j = Math.min( len, gainLen ); j > 0; ) {
									f1 = Math.abs( convBuf1[ --j ]);
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
								}
							}
						} else {													// ------- write real
//							tempF[ 2 ][ ch ].writeFloats( fftBuf, off, len );
							tempF[ 2 ].writeFrames( fftBuf, off, len );
							// measure gain
							for( int ch = 0; ch < outChanNum; ch++ ) {
								convBuf2	= fftBuf[ ch ];
								for( int j = off, k = Math.min( len, gainLen ) + off; j < k; ) {
									f1 = Math.abs( convBuf2[ j++ ]);
									if( f1 > maxAmp ) {
										maxAmp = f1;
									}
								}
							}
						}
						progOff	+= len;
						off		+= len;
						gainLen	-= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;

					framesWritten = Math.max( framesWritten, tempOff + chunkLength );						
				} // for step1
			} // for step0

		// ============================== normalize output ==============================
// System.out.println( "progOff "+progOff+"; len "+progLen );

		// ---- delete obsolete temp files ----
			for( int i = 0; i < 2; i++ ) {
				if( tempF[i] != null ) deleteTempFile( tempF[ i ]);
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- norm ----
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				ampRef	= new Param( 1.0 / (double) maxAmp, Param.ABS_AMP );
			} else {
				ampRef	= new Param( 1.0, Param.ABS_AMP );
			}
			gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			f1		= (imOutF != null) ? ((1.0f + getProgression()) / 2) : 1.0f;
			normalizeAudioFile( tempF[2], reOutF, inBuf, gain, f1 );
			if( imOutF != null ) {
				normalizeAudioFile( tempF[3], imOutF, inBuf, gain, 1.0f );
			}
			maxAmp	*= gain;

		// ---- delete obsolete temp files ----
			for( int i = 2; i < 4; i++ ) {
				if( tempF[i] != null ) deleteTempFile( tempF[ i ]);
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
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			convBuf1= null;
			fftBuf	= null;
			inBuf	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}
		finally {
	// ---- cleanup (topLevel) ----
			for( int i = 0; i < 2; i++ ) {
				if( reInF[i] != null ) reInF[i].cleanUp();
				if( imInF[i] != null ) imInF[i].cleanUp();
			}
			if( reOutF != null ) reOutF.cleanUp();
			if( imOutF != null ) imOutF.cleanUp();
		}
	} // process()

	protected void recalcSteps()
	{
		int outChanNum = Math.max( inChanNum[ 0 ], inChanNum[ 2 ]);
		long[] minLength = new long[] {
			pr.bool[ PR_HASIMINPUT ]   ? Math.min( guiInLength[ 0 ], guiInLength[ 1 ]) : guiInLength[ 0 ],
			pr.bool[ PR_HASIMIMPULSE ] ? Math.min( guiInLength[ 2 ], guiInLength[ 3 ]) : guiInLength[ 2 ],
		};
		
		if( outChanNum > 0 && minLength[ 0 ] > 0 && minLength[ 1 ] > 0 ) {
			final Descriptor d = initDescriptor( minLength, outChanNum );
			ggInfo.setText( "process divided into "+d.totalSteps+" steps." );
		} else {
			ggInfo.setText( null );
		}
	}

	private Descriptor initDescriptor( long[] inLength, int outChanNum )
	{
		boolean autoConv;
		int j, minSize;
		long lo1, lo2, lo4;
		Descriptor	d	= new Descriptor();
		
		autoConv = (pr.text[ PR_REINPUTFILE ].equals( pr.text[ PR_REIMPFILE ])) &&
				   (pr.bool[ PR_HASIMINPUT ] == pr.bool[ PR_HASIMIMPULSE ]) &&
				   (!pr.bool[ PR_HASIMINPUT ] || (pr.text[ PR_IMINPUTFILE ].equals( pr.text[ PR_IMIMPFILE ]))) &&
				   (pr.bool[ PR_CEPSTRAL1 ] == pr.bool[ PR_CEPSTRAL2 ]);
	
		d.complex	= pr.bool[ PR_HASIMINPUT ] || pr.bool[ PR_HASIMIMPULSE ] || pr.bool[ PR_HASIMOUTPUT ] ||
					  pr.bool[ PR_CEPSTRAL1 ] || pr.bool[ PR_CEPSTRAL2 ];

		minSize = (int) Math.min( 0x3FFFFFFF, ((((long) pr.para[ PR_MEMORY].val * 1024 * 1024) >> (d.complex ? 5 : 4))
			* 3 / outChanNum));
//			int minSize = (int) Math.min( 0x3FFFFFFF, ((Runtime.getRuntime().freeMemory() >> (complex ? 5 : 4)) * 3 / outChanNum));
		for( d.fftSize = 32; d.fftSize <= minSize; d.fftSize <<= 1 ) ;

		if( !autoConv ) {			// ----- different inputs -----

			lo2		= inLength[0];
			lo4		= (inLength[0] * inLength[1] + 1) * d.fftSize * d.fftSize;	// worst case
			// calc best performance

			long bestCost	= Long.MAX_VALUE;
			int bestFFTsize	= 0;
			long bestInpLen	= 0;
			
			do {
//				lo3			= lo4;
//				lo5			= lo2;
				d.fftSize >>= 1;
				j			= (int) Math.min( inLength[0], d.fftSize + 1 - Math.min( 32, inLength[1] ));
				lo1			= inLength[0] * inLength[1] + 1;	// worst case
				lo2			= inLength[0];
				for( int i = (int) Math.min( 32, inLength[0] ); i < j; i += 512 ) {
					d.inputLen[0]	= i;
					d.inputLen[1]	= (int) Math.min( inLength[1], d.fftSize + 1 - i );
					d.steps[0]		= (int) ((inLength[0] + d.inputLen[0] - 1) / d.inputLen[0]);
					d.steps[1]		= (int) ((inLength[1] + d.inputLen[1] - 1) / d.inputLen[1]);
					d.totalSteps	= ((long) d.steps[0] * (long) d.steps[1]) + (long) d.steps[0] + (long) d.steps[1];
//System.err.println( "with i = "+i+" : d.inputLen[0] "+d.inputLen[0]+"; d.inputLen[1] "+d.inputLen[1]+"; d.steps[0] "+d.steps[0]+"; d.steps[1] "+d.steps[1]+"; d.totalSteps "+d.totalSteps+"; d.fftSize "+d.fftSize );
					if( d.totalSteps < lo1 ) {
						lo2	= i;	// minimum step-num so far
						lo1	= d.totalSteps;
//System.err.println( " --> lo2 = "+i+"; lo1 = "+lo1 );
					}
				}
				lo4 = (long) (lo1 * d.fftSize * Math.log( d.fftSize ));
//System.err.println( "= cost "+lo4 );
				if( lo4 < bestCost ) {
					bestCost = lo4;
					bestFFTsize	= d.fftSize;
					bestInpLen	= lo2;
				}
			} while( d.fftSize > 32 );

			d.fftSize		= bestFFTsize;
			d.inputLen[0]	= (int) bestInpLen;
			d.inputLen[1]	= (int) Math.min( inLength[1], d.fftSize + 1 - d.inputLen[0] );
		
		} else {					// ----- same inputs -----
		
			lo1				= inLength[0] << 2;
			while( d.fftSize >= lo1 ) d.fftSize >>= 1;
			d.inputLen[0]	= (int) Math.min( inLength[0], d.fftSize >> 1 );
			d.inputLen[1]	= d.inputLen[0];
		}
		
		d.steps[0]		= (int) ((inLength[0] + d.inputLen[0] - 1) / d.inputLen[0]);
		d.steps[1]		= (int) ((inLength[1] + d.inputLen[1] - 1) / d.inputLen[1]);
//System.err.println( "d.inputLen[0] "+d.inputLen[0]+"; d.inputLen[1] "+d.inputLen[1]+"; d.steps[0] "+d.steps[0]+"; d.steps[1] "+d.steps[1]+"; d.fftSize "+d.fftSize );
		d.totalSteps	= (long) d.steps[0] * (long) d.steps[1];
		
		return d;
	}
	
	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component	c;
		boolean		b;
		
		c = gui.getItemObj( GG_IMINPUTFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMINPUT ]);
		}
		c = gui.getItemObj( GG_IMIMPFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMIMPULSE ]);
		}
		b = pr.bool[ PR_HASIMINPUT ] || pr.bool[ PR_HASIMIMPULSE ];
		c = gui.getItemObj( GG_HASIMOUTPUT );
		if( c != null ) {
			c.setEnabled( b );
		}
		c = gui.getItemObj( GG_IMOUTPUTFILE );
		if( c != null ) {
			c.setEnabled( b && pr.bool[ PR_HASIMOUTPUT ]);
		}
	}
	
	protected static class Descriptor
	{
		int		fftSize;
		long	totalSteps;
		boolean	complex;
		int[]	inputLen	= new int[ 2 ];
		int[]	steps		= new int[ 2 ];
	}
}
// class ComplexConvDlg
