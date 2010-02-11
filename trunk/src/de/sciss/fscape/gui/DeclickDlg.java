/*
 *  DeclickDlg.java
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
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;

/**
 *  Rather strange processing module for soundfile declicking
 *	which is not really too successfull.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class DeclickDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_IMPULSEFILE		= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_CHECKSIZE		= 2;
	private static final int PR_XFADE			= 3;
	private static final int PR_PROBBOUND		= 4;
	private static final int PR_MINAMP			= 0;		// pr.para
	private static final int PR_FSCAPEDETECT	= 0;		// pr.bool
	private static final int PR_FSCAPEREPAIR	= 1;
	private static final int PR_MARKERDETECT	= 2;
	private static final int PR_MARKERREPAIR	= 3;

	private static final int CHECK_16			= 0;
	private static final int CHECK_32			= 1;
	private static final int CHECK_64			= 2;
	private static final int XFADE_512			= 0;
	private static final int XFADE_1024			= 1;
	private static final int XFADE_2048			= 2;
	private static final int PROB_5PM			= 0;
	private static final int PROB_1PM			= 1;
	private static final int PROB_05PM			= 2;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_IMPULSEFILE		= "ImpFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_CHECKSIZE		= "CheckSize";
	private static final String PRN_XFADE			= "XFade";
	private static final String PRN_PROBBOUND		= "ProbBound";
	private static final String PRN_MINAMP			= "MinAmp";
	private static final String PRN_FSCAPEDETECT	= "FScDetect";
	private static final String PRN_FSCAPEREPAIR	= "FScRepair";
	private static final String PRN_MARKERDETECT	= "MarkDetect";
	private static final String PRN_MARKERREPAIR	= "MarkRepair";

	private static final String	prText[]		= { "", "", "sounds" + File.separator + "declickIR.aif" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_IMPULSEFILE };
	private static final int		prIntg[]	= { 0, 0, CHECK_32, XFADE_1024, PROB_1PM };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_CHECKSIZE,
													PRN_XFADE, PRN_PROBBOUND };
	private static final Param	prPara[]		= { null };
	private static final String	prParaName[]	= { PRN_MINAMP };
	private static final boolean	prBool[]	= { true, true, true, true };
	private static final String	prBoolName[]	= { PRN_FSCAPEDETECT, PRN_FSCAPEREPAIR,
													PRN_MARKERDETECT, PRN_MARKERREPAIR };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_IMPULSEFILE			= GG_OFF_PATHFIELD	+ PR_IMPULSEFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_CHECKSIZE			= GG_OFF_CHOICE		+ PR_CHECKSIZE;
	private static final int GG_XFADE				= GG_OFF_CHOICE		+ PR_XFADE;
	private static final int GG_PROBBOUND			= GG_OFF_CHOICE		+ PR_PROBBOUND;
	private static final int GG_MINAMP				= GG_OFF_PARAMFIELD	+ PR_MINAMP;
	private static final int GG_FSCAPEDETECT		= GG_OFF_CHECKBOX	+ PR_FSCAPEDETECT;
	private static final int GG_FSCAPEREPAIR		= GG_OFF_CHECKBOX	+ PR_FSCAPEREPAIR;
	private static final int GG_MARKERDETECT		= GG_OFF_CHECKBOX	+ PR_MARKERDETECT;
	private static final int GG_MARKERREPAIR		= GG_OFF_CHECKBOX	+ PR_MARKERREPAIR;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;
	private			JLabel			lbClicks;

	private static final String	MARK_CLICK			= "Click";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public DeclickDlg()
	{
		super( "Declick" );
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
			static_pr.para[ PR_MINAMP ]		= new Param( -60.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
//			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile, ggImpulseFile;
		PathField			ggInputs[];
		JComboBox				ggCheckSize, ggProbBound, ggXFade;
		ParamField			ggMinAmp;
		ParamSpace			spcMinAmp;
		JCheckBox			ggFScapeDetect, ggMarkerDetect, ggFScapeRepair, ggMarkerRepair;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_FSCAPEDETECT:
				case GG_FSCAPEREPAIR:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
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
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0DClk$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

	// -------- Detect-Gadgets --------
	gui.addLabel( new GroupLabel( "Detection Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggFScapeDetect	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Let FScape detect", SwingConstants.RIGHT ));
		gui.addCheckbox( ggFScapeDetect, GG_FSCAPEDETECT, il );

		ggCheckSize		= new JComboBox();
		ggCheckSize.addItem( "16" );
		ggCheckSize.addItem( "32" );
		ggCheckSize.addItem( "64" );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Check size [smp]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggCheckSize, GG_CHECKSIZE, il );

		ggMarkerDetect	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Read markers", SwingConstants.RIGHT ));
		gui.addCheckbox( ggMarkerDetect, GG_MARKERDETECT, il );

		ggProbBound		= new JComboBox();
		ggProbBound.addItem( "1 : 200" );
		ggProbBound.addItem( "1 : 1000" );
		ggProbBound.addItem( "1 : 2000" );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Probablity bound", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggProbBound, GG_PROBBOUND, il );

		spcMinAmp		= new ParamSpace( Constants.minDecibel, 0.0, 0.1, Param.DECIBEL_AMP );
		ggMinAmp		= new ParamField( spcMinAmp );
		con.weightx		= 0.1;
		con.gridwidth	= 3;
		gui.addLabel( new JLabel( "Min. amp.", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMinAmp, GG_MINAMP, null );

	// -------- Repair-Gadgets --------
	gui.addLabel( new GroupLabel( "Repair Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggFScapeRepair	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Let FScape repair", SwingConstants.RIGHT ));
		gui.addCheckbox( ggFScapeRepair, GG_FSCAPEREPAIR, il );

		ggXFade			= new JComboBox();
		ggXFade.addItem( "512" );
		ggXFade.addItem( "1024" );
		ggXFade.addItem( "2048" );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "XFade size [smp]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggXFade, GG_XFADE, il );

		ggMarkerRepair	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Write markers", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggMarkerRepair, GG_MARKERREPAIR, il );

		ggImpulseFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select IR file" );
		ggImpulseFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Impulse response", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImpulseFile, GG_IMPULSEFILE, null );

	// -------- Click-View --------
	gui.addLabel( new GroupLabel( "Click View", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		lbClicks		= new JLabel( " " );
		lbClicks.setBackground( Color.white );
		gui.addLabel( lbClicks );

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
	 *	Gain-Change durchfuehren
	 */
	public void process()
	{
		int					i, j, k;
		int					off, chunkLength;
		int					ch;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFile			impF			= null;
		FloatFile[]			floatF			= null;
		File[]				tempFile		= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		AudioFileDescr			impStream		= null;

		int					inChanNum;
		int					impChanNum		= 0;
		float[][]			inBuf;
		float[][]			impBuf			= null;
		float[]				zData;
		float[]				fftBuf;
		float[]				win;
		float[]				convBuf1, convBuf2;
		PathField			ggOutput;

		float				mean;
		float				stddev;
		float				critical;
		float				probBound;
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		
		float				minAmp;
		int					checkOff, checkInner, checkLen;
		int					chunkMin, chunkMax, chunkNum, chunkDone;
		int					fadeLen, prePost, frameSize, inOff;
		int					firstClk, lastClk;
		
		int					markerID		= -1;
		Marker				marker			= null;
		java.util.List		markers, outMarkers;
		
		Graphics			g				= null;	// we use a JLabel to draw to click-positions
		Dimension			gDim;
		int					x;

		int					inLength;
		int					outLength		= 0;
		int					impLength		= 0;				// reichen 32 bit?
		int					framesRead, framesWritten;

topLevel: try {
		// ---- init ----
	
			minAmp		= (float) (Param.transform( pr.para[ PR_MINAMP ], Param.ABS_AMP, ampRef, null )).val;
			switch( pr.intg[ PR_PROBBOUND ]) {
			case PROB_5PM:
				probBound	= 2.58f;
				break;
			case PROB_1PM:
				probBound	= 3.10f;
				break;
			default:
			case PROB_05PM:
				probBound	= 3.30f;
				break;
			}
			g		= lbClicks.getGraphics();
			gDim	= lbClicks.getSize();
			if( g != null ) {
				lbClicks.repaint();
				g.setColor( Color.red );
			}

			switch( pr.intg[ PR_CHECKSIZE ]) {
			case CHECK_16:
				checkLen	= 16;
				break;
			case CHECK_32:
				checkLen	= 32;
				break;
			default:
			case CHECK_64:
				checkLen	= 64;
				break;
			}

			switch( pr.intg[ PR_XFADE ]) {
			case XFADE_512:
				fadeLen		= 512;
				break;
			case XFADE_1024:
				fadeLen		= 1024;
				break;
			default:
			case XFADE_2048:
				fadeLen		= 2048;
				break;
			}

			prePost		= fadeLen >> 1;
			frameSize	= prePost + checkLen/4 + checkLen/2 + checkLen/4 + prePost;	// !! inBuf Aufbau !!
			checkOff	= prePost + (checkLen >> 2);
			checkInner	= checkLen >> 1;
//			checkEnd	= checkOff + checkInner;
			inOff		= frameSize - checkInner;

		// ---- open and prepare files ----

			inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream	= inF.getDescr();
			inChanNum	= inStream.channels;
			inLength	= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength*inChanNum) < 1 ) throw new EOFException( ERR_EMPTY );

			markers		= (java.util.List) inStream.getProperty( AudioFileDescr.KEY_MARKERS );
			if( markers != null ) {
				outMarkers	= new Vector( markers );
			} else {
				outMarkers	= new Vector();
			}

			if( pr.bool[ PR_FSCAPEREPAIR ] || pr.bool[ PR_MARKERREPAIR ]) {
				impF			= AudioFile.openAsRead( new File( pr.text[ PR_IMPULSEFILE ]));
				impStream		= impF.getDescr();
				impChanNum		= impStream.channels;
				impLength		= (int) Math.min( impStream.length, prePost );
				impBuf			= new float[ impChanNum ][ fadeLen + 2 ];
				for( ch = 0; ch < impChanNum; ch++ ) {
					convBuf1 = impBuf[ ch ];
					for( i = 0; i < convBuf1.length; i++ ) {
						convBuf1[ i ] = 0.0f;
					}
				}

				ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
				if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
				outStream	= new AudioFileDescr( inStream );
				ggOutput.fillStream( outStream );
				IOUtil.createEmptyFile( new File( pr.text[ PR_OUTPUTFILE ]));
				outLength	= inLength;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			inBuf		= new float[ inChanNum ][ frameSize ];
			zData		= new float[ checkLen ];
			fftBuf		= new float[ fadeLen + 2 ];
			chunkMin	= frameSize / checkInner;		// frameSize must be durch checkInner teilbar!!!
			chunkNum	= ((inLength + checkInner - 1) / checkInner) + (chunkMin - 1);
			chunkMax	= chunkNum - chunkMin;
			chunkDone	= 0;

			win			= Filter.createWindow( prePost, Filter.WIN_BLACKMAN );

			for( ch = 0; ch < inChanNum; ch++ ) {
				convBuf1 = inBuf[ ch ];
				for( i = 0; i < frameSize; i++ ) {
					convBuf1[ i ] = 0.0f;
				}
			}
			for( i = 0; i < fftBuf.length; i++ ) {
				fftBuf[ i ] = 0.0f;
			}			

			// get first click mark
			if( pr.bool[ PR_MARKERDETECT ] && markers != null ) {
				Collections.sort( markers );
//				markers		= Marker.sort( markers );
				markerID	= Marker.find( markers, MARK_CLICK, 0 );
				if( markerID >= 0 ) {
					marker	= (Marker) markers.get( markerID );
				}
			}
			
			progOff		= 0;
			progLen		= (long) inLength + (long) outLength + (long) (chunkNum * checkInner);

			// if we do not create markers then open output now
			if( pr.bool[ PR_FSCAPEREPAIR ] && !pr.bool[ PR_MARKERREPAIR ]) {
				outF	= AudioFile.openAsWrite( outStream );
			}
			// else open temp file
			if( pr.bool[ PR_MARKERREPAIR ]) {
				tempFile	= new File[ inChanNum ];
				floatF		= new FloatFile[ inChanNum ];
				for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
					tempFile[ ch ]	= null;
					floatF[ ch ]	= null;
				}
				for( ch = 0; ch < inChanNum; ch++ ) {
					tempFile[ ch ]	= IOUtil.createTempFile();
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
				}
				progLen	   += (long) outLength;	// account for extra reads/writes
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
//System.out.println( "frameSize "+frameSize+"; checkInner "+checkInner+"; chunkMin "+chunkMin );

		// ---- read impulse ----
			if( impF != null ) {
				progLen		+= (long) impLength;
				impF.readFrames( impBuf, 0, impLength );
				// forward fft
				for( ch = 0; ch < impChanNum; ch++ ) {
					Fourier.realTransform( impBuf[ ch ], fadeLen, Fourier.FORWARD );
				}
				impF.close();
				impF		 = null;
				progOff		+= impLength;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- process loop ----
			framesRead		= 0;
			framesWritten	= 0;

			while( threadRunning && (chunkDone < chunkNum) ) {
				for( off = 0, chunkLength = Math.min( inLength - framesRead, checkInner );
					 threadRunning && (off < chunkLength); ) {
//System.out.println(" Read "+chunkLength);

					j  = Math.min( 8192, chunkLength - off );
					inF.readFrames( inBuf, off + inOff, j );
					framesRead	+= j;
					off			+= j;
					progOff		+= j;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero-padding
				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = inBuf[ ch ];
					for( i = chunkLength + inOff; i < frameSize; i++ ) {
						convBuf1[ i ] = 0.0f;
					}
				}

//System.out.println(" Chck ");
				// check for clicks
				firstClk	= checkInner;			// "no clicks"
				lastClk		= -1;

				if( pr.bool[ PR_FSCAPEDETECT ]) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1	= inBuf[ ch ];
						mean		= 0f;
						for( i = prePost, j = 0; j < checkLen; i++, j++ ) {				// 1. Sample-Veraenderung u. Mittelwert
							zData[ j ]	= convBuf1[ i + 1 ] - convBuf1[ i ];
							mean	   += zData[ j ];
						}
						mean	/= checkLen;
						stddev	 = 0f;
						for( j = 0; j < checkLen; j++ ) {								// 2. Standardabweichung
							f1		= zData[ j ] - mean;
							stddev += f1*f1;
						}
						stddev	 = (float) Math.sqrt( stddev / checkLen );
						critical = probBound * stddev;
//System.out.println( "mean "+mean+"; stddev "+stddev+"; crit "+critical );
	
						for( i = 0, j = (checkLen >> 2) - 1; i < checkInner; i++, j++ ) {	// 3. z-normalisierte Veraenderung pruefen
							if( (Math.abs( zData[ j ] - mean ) >= critical) &&
								(Math.abs( zData[ j ]) >= minAmp) ) {
	
								firstClk	= Math.min( firstClk, i );
								lastClk		= Math.max( lastClk,  i );
								
								if( pr.bool[ PR_MARKERREPAIR ]) {
									outMarkers.add( new Marker( (chunkDone-chunkMin+1)*checkInner +
																i + checkOff, MARK_CLICK ));
								}
							}
						}
					}
				}
				if( pr.bool[ PR_MARKERDETECT ]) {
					i = (chunkDone-chunkMin+1)*checkInner + checkOff;
					while( marker != null ) {
						if( (marker.pos >= i) && (marker.pos < (i + checkInner)) ) {

							firstClk	= Math.min( firstClk, (int) marker.pos - i );
							lastClk		= Math.max( lastClk,  (int) marker.pos - i );
							
							markerID	= Marker.find( markers, MARK_CLICK, markerID + 1 );
							if( markerID >= 0 ) {
								marker	= (Marker) markers.get( markerID );
							} else {
								marker	= null;
							}
						} else if( marker.pos < i ) {		// "veraltet"
							markerID	= Marker.find( markers, MARK_CLICK, markerID + 1 );
							if( markerID >= 0 ) {
								marker	= (Marker) markers.get( markerID );
							} else {
								marker	= null;
							}
						} else break;	// noch zu frueh
					}
				}

				// declick
declick:		if( (lastClk >= 0) && (chunkDone >= chunkMin) && (chunkDone <= chunkMax) ) {

					// visualize
					if( g != null ) {
						x = (int) (((float) chunkDone / (float) chunkNum) * gDim.width + 0.5f);
						g.drawLine( x, 0, x, gDim.height - 1 );
					}

					if( !pr.bool[ PR_FSCAPEREPAIR ]) break declick;

					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						// copy left wing
						System.arraycopy( convBuf1, firstClk + checkOff - prePost, fftBuf, 0, prePost );
						// zero padding
						for( i = prePost; i < fadeLen; i++ ) {
							fftBuf[ i ] = 0.0f;
						}
						// apply fadein/fadeout
						for( i = 0, j = prePost - 1; j >= 0; i++, j -= 2 ) {
							fftBuf[ i ] *= win[ j ];
						}
						for( j = 0; j < prePost; i++, j += 2 ) {
							fftBuf[ i ] *= win[ j ];
						}
						// forward transform
						Fourier.realTransform( fftBuf, fadeLen, Fourier.FORWARD );
						// convolve
						convBuf2 = impBuf[ ch % impChanNum ];
						for( i = 0; i <= fadeLen; i += 2 ) {		// complex multiplication
							j				= i + 1;
							f1				= fftBuf[ i ];			// (avoid overwrite)
							fftBuf[ i ]		= f1 * convBuf2[ i ] - fftBuf[ j ] * convBuf2[ j ];
							fftBuf[ j ]		= f1 * convBuf2[ j ] + fftBuf[ j ] * convBuf2[ i ];
						}
						// inverse transform
						Fourier.realTransform( fftBuf, fadeLen, Fourier.INVERSE );
						// xfade left wing
						for( j = (prePost>>1), i = firstClk + checkOff - j, k = 0; j < prePost; i++, j++, k += 2 ) {
							f1				= win[ k ];
							convBuf1[ i ]	= f1 * convBuf1[ i ] + (1.0f - f1) * fftBuf[ j ];
						}
						// overwrite clip part
						for( k = lastClk + checkOff; i <= k; i++, j++ ) {
							convBuf1[ i ]	= fftBuf[ j ];
						}
						// xfade right wing
						for( k = 0; k < prePost; k += 2, i++, j++ ) {
							f1				= win[ k ];
							convBuf1[ i ]	= (1.0f - f1) * convBuf1[ i ] + f1 * fftBuf[ j ];
						}

/*						// apply fadeout to convolved data
						for( i = prePost, j = 0; i < fadeLen; i++, j++ ) {
							fftBuf[ i ] *= win[ j ];
						}
						// apply fadeout/fadein to orig. input ("dip")
						for( i = firstClk + checkOff - prePost + 1, k = lastClk + checkOff + prePost - 1,
							 j = 0; j < prePost; i++, j++, k-- ) {

							f1			   = win[ j ];
							convBuf1[ i ] *= f1;
							convBuf1[ k ] *= f1;
						}
						for( ; i < k; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
						// add convolved chunk to orig. "dipped" data
						for( i = firstClk + checkOff - prePost, j = 0; j < fadeLen; i++, j++ ) {
							convBuf1[ i ] += fftBuf[ j ];
						}
*/					} // for chan, declick
				}
				chunkDone++;
				progOff += checkInner;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// write output
				if( chunkDone >= chunkMin ) {
					chunkLength = Math.min( outLength - framesWritten, checkInner );

					if( floatF != null ) {
						// ...to temp
						for( ch = 0; ch < inChanNum; ch++ ) {
							floatF[ ch ].writeFloats( inBuf[ ch ], 0, chunkLength );
						}
						framesWritten  += chunkLength;
						off			   += chunkLength;
						progOff		   += chunkLength;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );

					} else if( outF != null ) {
						// ...directly
						for( off = 0; threadRunning && (off < chunkLength); ) {
							j  = Math.min( 8192, chunkLength - off );
							outF.writeFrames( inBuf, off, j );
							framesWritten	+= j;
							off				+= j;
							progOff			+= j;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						}
					}
				}

				// shift data
				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1	= inBuf[ ch ];
					System.arraycopy( convBuf1, checkInner, convBuf1, 0, frameSize - checkInner );
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- copy temp => output ----

			if( floatF != null ) {
				Collections.sort( outMarkers );
				if( outMarkers.size() > 0xFFFF ) {
					System.out.println( "WARNING: too many markers (" + outMarkers.size() + "). Truncating to 65535!" );
					while( outMarkers.size() > 0xFFFF ) outMarkers.remove( 0xFFFF );
				}
				outStream.setProperty( AudioFileDescr.KEY_MARKERS, outMarkers );
//				outStream.setProperty( AudioFileDescr.KEY_MARKERS, Marker.sort( outMarkers ));
				outF = AudioFile.openAsWrite( outStream );
								
				normalizeAudioFile( floatF, outF, inBuf, 1.0f, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}

		// ---- Finish ----

			if( outF != null ) {
				outF.close();
				outF = null;
			}
			inF.close();
			inF = null;
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			impStream	= null;
			inBuf		= null;
			zData		= null;
			fftBuf		= null;
			win			= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( g != null ) {
			g.dispose();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
		if( outF != null ) {
			outF.cleanUp();
		}
		if( impF != null ) {
			impF.cleanUp();
			impF = null;
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_CHECKSIZE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_FSCAPEDETECT ]);
		}
		c = gui.getItemObj( GG_PROBBOUND );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_FSCAPEDETECT ]);
		}
		c = gui.getItemObj( GG_MINAMP );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_FSCAPEDETECT ]);
		}
		c = gui.getItemObj( GG_XFADE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_FSCAPEREPAIR ]);
		}
		c = gui.getItemObj( GG_IMPULSEFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_FSCAPEREPAIR ]);
		}
	}
}
// class DeclickDlg
