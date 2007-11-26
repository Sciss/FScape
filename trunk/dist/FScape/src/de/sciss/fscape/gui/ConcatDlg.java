/*
 *  ConcatDlg.java
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
 *		25-Jun-06	fixed bug in analyze / synthesize names ; reverse order works
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
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
 *  Processing module for concatening
 *	a bunch of sound files with optional
 *	overlap and crossfade.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ConcatDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE1	= 0;		// pr.text
	private static final int PR_INPUTFILE2	= 1;
	private static final int PR_OUTPUTFILE	= 2;
	private static final int PR_OUTPUTTYPE	= 0;		// pr.intg
	private static final int PR_OUTPUTRES	= 1;
	private static final int PR_GAINTYPE	= 2;
	private static final int PR_FADESHAPE	= 3;
	private static final int PR_FADETYPE	= 4;
	private static final int PR_GAIN		= 0;		// pr.para
	private static final int PR_OFFSET		= 1;
	private static final int PR_LENGTH		= 2;
	private static final int PR_OVERLAP		= 3;
	private static final int PR_FADE		= 4;

	private static final String PRN_INPUTFILE1	= "InputFile1";
	private static final String PRN_INPUTFILE2	= "InputFile2";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";
	private static final String PRN_FADESHAPE	= "FadeShape";
	private static final String PRN_FADETYPE	= "FadeType";
	private static final String PRN_OFFSET		= "Offset";
	private static final String PRN_LENGTH		= "Length";
	private static final String PRN_OVERLAP		= "Overlap";
	private static final String PRN_FADE		= "Fade";

	protected static final String[]	SHAPE_NAMES		= { "Normal", "Fast in", "Slow in", "Easy in Easy out" };

	private static final int		SHAPE_NORMAL	= 0;
//	private static final int		SHAPE_FASTIN	= 1;
//	private static final int		SHAPE_SLOWIN	= 2;
//	private static final int		SHAPE_EASY		= 3;

	private static final String[]	TYPE_NAMES		= { "Equal Energy", "Equal Power" };

	private static final int		TYPE_ENERGY		= 0;
	private static final int		TYPE_POWER		= 1;

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE1, PRN_INPUTFILE2, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, SHAPE_NORMAL, TYPE_POWER };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
													PRN_FADESHAPE, PRN_FADETYPE };
	private static final Param	prPara[]		= { null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_OFFSET, PRN_LENGTH, PRN_OVERLAP, PRN_FADE };

	private static final int GG_INPUTFILE1		= GG_OFF_PATHFIELD	+ PR_INPUTFILE1;
	private static final int GG_INPUTFILE2		= GG_OFF_PATHFIELD	+ PR_INPUTFILE2;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_FADESHAPE		= GG_OFF_CHOICE		+ PR_FADESHAPE;
	private static final int GG_FADETYPE		= GG_OFF_CHOICE		+ PR_FADETYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_OFFSET			= GG_OFF_PARAMFIELD	+ PR_OFFSET;
	private static final int GG_LENGTH			= GG_OFF_PARAMFIELD	+ PR_LENGTH;
	private static final int GG_OVERLAP			= GG_OFF_PARAMFIELD	+ PR_OVERLAP;
	private static final int GG_FADE			= GG_OFF_PARAMFIELD	+ PR_FADE;
	private static final int GG_CURRENTINFO		= GG_OFF_OTHER		+ 0;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_NOFILES			= "No files detected";

	private static final	String			FILENAMEPTRN		= "{1}{0}{2}";
	private 				Object[]		fileNameArgs		= new Object[5];
//	private					MessageFormat	fileNameForm		= new MessageFormat( FILENAMEPTRN );
	private					boolean			usePad;
	private					boolean			reverse;
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ConcatDlg()
	{
		super( "Concat" );
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
			static_pr.para[ PR_GAIN ]		= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_OFFSET ]		= new Param(   0.0, Param.ABS_MS );
			static_pr.para[ PR_LENGTH ]		= new Param( 100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_OVERLAP ]	= new Param(   0.0, Param.FACTOR_TIME );
			static_pr.para[ PR_FADE ]		= new Param(   0.0, Param.FACTOR_TIME );
			static_pr.paraName	= prParaName;
			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- Misc init --------

//		fileNameForm.setLocale( Locale.US );
//		fileNameForm.applyPattern( FILENAMEPTRN );

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile1, ggInputFile2, ggOutputFile;
		Component[]			ggGain;
		PathField[]			ggInputs;
		ParamField			ggOffset, ggLength, ggOverlap, ggFade;
		JComboBox				ggFadeShape, ggFadeType;
		ParamSpace[]		spcOffset, spcLength;
		ParamSpace			spc;
		JTextField			ggCurrentInfo;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE1:
				case GG_INPUTFILE2:
					pr.text[ ID - GG_OFF_PATHFIELD ] = ((PathField) e.getSource()).getPath().getPath();
					if( ID == GG_INPUTFILE1 ) {
						setInput( pr.text[ ID - GG_OFF_PATHFIELD ]);
					}
					analyseFileNames();
					break;
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile1	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select first input file" );
		ggInputFile1.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "First input file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile1, GG_INPUTFILE1, pathL );

		ggInputFile2	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select first input file" );
		ggInputFile2.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Last input file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile2, GG_INPUTFILE2, pathL );

		ggCurrentInfo	= new JTextField();
		ggCurrentInfo.setEditable( false );
		ggCurrentInfo.setBackground( null );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Current input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addTextField( ggCurrentInfo, GG_CURRENTINFO, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile1;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0CC$E" );
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Scissor Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcOffset		= new ParamSpace[ 3 ];
		spcOffset[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcOffset[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcOffset[2]	= Constants.spaces[ Constants.ratioTimeSpace ];
		spcLength		= new ParamSpace[ 6 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spc				= new ParamSpace( Constants.spaces[ Constants.offsetMsSpace ]);
//		spc.max			= 0.0;
		spc				= new ParamSpace( spc.min, 0.0, spc.inc, spc.unit );
		spcLength[1]	= spc;
		spcLength[2]	= Constants.spaces[ Constants.absBeatsSpace ];
		spc				= new ParamSpace( Constants.spaces[ Constants.offsetBeatsSpace ]);
//		spc.max			= 0.0;
		spc				= new ParamSpace( spc.min, 0.0, spc.inc, spc.unit );
		spcLength[3]	= spc;
		spc				= new ParamSpace( Constants.spaces[ Constants.offsetTimeSpace ]);
//		spc.max			= 0.0;
		spc				= new ParamSpace( spc.min, 0.0, spc.inc, spc.unit );
		spcLength[4]	= spc;
		spcLength[5]	= Constants.spaces[ Constants.ratioTimeSpace ];
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;

		ggOffset		= new ParamField( spcOffset );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Chunk Offset", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggOffset, GG_OFFSET, null );

		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Chunk Length", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLength, GG_LENGTH, null );

		ggOverlap		= new ParamField( spcLength );
		ggOverlap.setReference( ggLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Chunk Overlap", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggOverlap, GG_OVERLAP, null );

		ggFade			= new ParamField( spcOffset );
		ggFade.setReference( ggOverlap );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Overlap Crossfade", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggFade, GG_FADE, null );

		ggFadeShape		= new JComboBox();
ggFadeShape.setEnabled( false );
		for( int i = 0; i < SHAPE_NAMES.length; i++ ) {
			ggFadeShape.addItem( SHAPE_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossfade Shape", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggFadeShape, GG_FADESHAPE, null );

		ggFadeType		= new JComboBox();
		for( int i = 0; i < TYPE_NAMES.length; i++ ) {
			ggFadeType.addItem( TYPE_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossfade Type", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggFadeType, GG_FADETYPE, null );

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
		int					i, j, k, m, n, p;
		int					ch;
		long				progOff, progLen;
		double				d1;
		float				f1;
		
		// io
		SoundChunk[]		sndChunks		= null;
		SoundChunk			currentChunk;
		AudioFile			outF			= null;
		AudioFileDescr			outStream		= null;
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;
		float[][]			inBuf, outBuf;
		float[]				convBuf1;


		float				gain			= 1.0f;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		Param				ref;

		int					numChunks, outChanNum, outLength, nextOpened;
		int					framesWritten, chunkLength, relOff;

		float				maxAmp			= 0.0f;

		float				fadeY1, fadeY2;

		PathField			ggOutput;
		
		Vector				activeChunks	= new Vector();
		
topLevel: try {
		// ---- open input, output; init ----

			numChunks		= analyseFileNames();
			if( numChunks <= 0 ) throw new EOFException( ERR_NOFILES );
			sndChunks		= new SoundChunk[ numChunks ];
			progOff			= 0;
			progLen			= numChunks * 25;
			outChanNum		= 1;
			outLength		= 0;

			for( i = 0; i < numChunks; i++ ) {
				sndChunks[i]		= new SoundChunk();
				sndChunks[i].name	= synthesizeFileName( reverse ? (numChunks - i - 1) : i );
				gui.stringToJTextField( "Opening '" + sndChunks[i].name + "'...", GG_CURRENTINFO );
//File xxx = new File( sndChunks[i].name );
//System.err.println( "\""+xxx.toString()+"\" ; exists ? "+xxx.exists() );
				sndChunks[i].f		= AudioFile.openAsRead( new File( sndChunks[i].name ));
				sndChunks[i].stream	= sndChunks[i].f.getDescr();
				if( sndChunks[i].stream.channels <= 0 ) throw new EOFException( ERR_EMPTY );
				outChanNum	= Math.max( outChanNum, sndChunks[i].stream.channels );

				ref					= new Param( AudioFileDescr.samplesToMillis( sndChunks[i].stream, sndChunks[i].stream.length ), Param.ABS_MS );
				sndChunks[i].off	= (int) Math.max( 0, Math.min( sndChunks[i].stream.length, (int) (AudioFileDescr.millisToSamples( sndChunks[i].stream,
																   (Param.transform( pr.para[ PR_OFFSET ], Param.ABS_MS, ref, null )).val ) + 0.5) ));

				sndChunks[i].len	= (int) Math.max( 0, Math.min( sndChunks[i].stream.length - sndChunks[i].off, (int) (AudioFileDescr.millisToSamples( sndChunks[i].stream,
																   (Param.transform( pr.para[ PR_LENGTH ], Param.ABS_MS, ref, null )).val ) + 0.5) ));
				ref					= new Param( AudioFileDescr.samplesToMillis( sndChunks[i].stream, sndChunks[i].len ), Param.ABS_MS );
				sndChunks[i].over	= Math.max( 0, Math.min( sndChunks[i].len, (int) (AudioFileDescr.millisToSamples( sndChunks[i].stream,
																   (Param.transform( pr.para[ PR_OVERLAP ], Param.ABS_MS, ref, null )).val ) + 0.5) ));
				ref					= new Param( AudioFileDescr.samplesToMillis( sndChunks[i].stream, sndChunks[i].over ), Param.ABS_MS );
				j					= Math.max( 0, Math.min( sndChunks[i].over, (int) (AudioFileDescr.millisToSamples( sndChunks[i].stream,
																   (Param.transform( pr.para[ PR_FADE ], Param.ABS_MS, ref, null )).val ) + 0.5) ));
				if( i > 0 ) {
					sndChunks[i].fadeIn		= Math.min( j, sndChunks[i-1].fadeOut );
					sndChunks[i-1].fadeOut	= sndChunks[i].fadeIn;
					sndChunks[i].punchIn	= sndChunks[i-1].punchIn + sndChunks[i-1].len - sndChunks[i-1].over;
				} else {
					sndChunks[i].fadeIn		= 0;
					sndChunks[i].punchIn	= 0;
				}
				sndChunks[i].fadeOut= Math.min( j, sndChunks[i].len - sndChunks[i].fadeIn );
				
				outLength			= Math.max( outLength, sndChunks[i].punchIn + sndChunks[i].len );

				progOff++;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;
			}
			sndChunks[numChunks-1].fadeOut	= 0;

/*
for( i = 0; i < numChunks; i++ ) {
System.out.println( sndChunks[i].name );
System.out.println( "offset   "+ sndChunks[i].off );
System.out.println( "length   "+ sndChunks[i].len );
System.out.println( "overlap  "+ sndChunks[i].over );
System.out.println( "fade in  "+ sndChunks[i].fadeIn );
System.out.println( "fade out "+ sndChunks[i].fadeOut );
System.out.println( "punch in "+ sndChunks[i].punchIn );
}
System.out.println( "outLen   "+ outLength );
*/

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( sndChunks[0].stream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
	// .... check running ....
			if( !threadRunning ) break topLevel;

			inBuf			= new float[ outChanNum ][ 8192 ];
			outBuf			= new float[ outChanNum ][ 8192 ];

			progLen			= (long) outLength * (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ? 3 : 2);
			d1				= getProgression();
			progOff			= (long) ((double) progLen * d1 / (1.0 - d1));
			progLen		   += progOff;

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
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// =================== CORE ===================

			framesWritten	= 0;
			nextOpened		= 0;

			while( threadRunning && (framesWritten < outLength) ) {
			
				chunkLength	= Math.min( 8192, outLength - framesWritten );
				Util.clear( outBuf );
				// check for new chunks
				for( ; nextOpened < numChunks; nextOpened++ ) {
					if( sndChunks[nextOpened].punchIn >= framesWritten + chunkLength ) break;
					activeChunks.addElement( sndChunks[nextOpened] );
					gui.stringToJTextField( "Concatening '" + sndChunks[nextOpened].name + "'...", GG_CURRENTINFO );
					sndChunks[nextOpened].f.seekFrame( sndChunks[nextOpened].off );
				}

				for( i = 0; i < activeChunks.size(); i++ ) {
					currentChunk	= (SoundChunk) activeChunks.elementAt( i );
					relOff			= currentChunk.punchIn - framesWritten;
					j = Math.max( 0, relOff );
//					if( j > 0 ) {	// punchIn
//						for( ch = 0; ch < currentChunk.stream.chanNum; ch++ ) {
//							convBuf1 = inBuf[ ch ];
//							for( m = 0; m < j; m++ ) {
//								convBuf1[m] = 0.0f;
//							}
//						}
//					}
					k = Math.min( chunkLength, relOff + currentChunk.len );
					currentChunk.f.readFrames( inBuf, j, k - j );

					// ---- fades ----
					m = relOff + currentChunk.fadeIn;
					if( m > j ) {	// fade in
						p		= m;
						m		= Math.min( m, chunkLength );
						fadeY2	= 1.0f - (float) (p - m) / (float) currentChunk.fadeIn;
						fadeY1	= (float) (j - relOff) / (float) currentChunk.fadeIn;
						f1		= (fadeY2 - fadeY1) / (m - j - 1);
// System.out.println( "II Y1 "+fadeY1+"; Y2 "+fadeY2 );
						
						switch( pr.intg[ PR_FADETYPE ]) {
						case TYPE_ENERGY:
							for( n = j; n < m; n++ ) {
								for( ch = 0; ch < currentChunk.stream.channels; ch++ ) {
									inBuf[ch][n] *= fadeY1;
								}
								fadeY1 += f1;
							}
							break;
						
						case TYPE_POWER:
							fadeY1 = 1.0f - fadeY1;
							for( n = j; n < m; n++ ) {
								for( ch = 0; ch < currentChunk.stream.channels; ch++ ) {
									inBuf[ch][n] *= 1.0f - (fadeY1*fadeY1);
								}
								fadeY1 -= f1;
							}
							break;
						}
					}
					m = relOff + currentChunk.len - currentChunk.fadeOut;
					if( m < k ) {	// fade out
						p		= m;
						m		= Math.max( m, 0 );
						fadeY1	= 1.0f - (float) (m - p) / (float) currentChunk.fadeOut;
						fadeY2	= 1.0f + (float) (p - k) / (float) currentChunk.fadeOut;
						f1		= (fadeY2 - fadeY1) / (k - m - 1);
// System.out.println( "OO Y1 "+fadeY1+"; Y2 "+fadeY2+"   ; m "+m+"; k "+k );

						switch( pr.intg[ PR_FADETYPE ]) {
						case TYPE_ENERGY:
							for( n = m; n < k; n++ ) {
								for( ch = 0; ch < currentChunk.stream.channels; ch++ ) {
									inBuf[ch][n] *= fadeY1;
								}
								fadeY1 += f1;
							}
							break;
						
						case TYPE_POWER:
							fadeY1 = 1.0f - fadeY1;
							for( n = m; n < k; n++ ) {
								for( ch = 0; ch < currentChunk.stream.channels; ch++ ) {
									inBuf[ch][n] *= 1.0f - (fadeY1*fadeY1);
								}
								fadeY1 -= f1;
							}
							break;
						}
					}

					if( k < chunkLength ) {	// punchOut
//						for( ch = 0; ch < currentChunk.stream.chanNum; ch++ ) {
//							convBuf1 = inBuf[ ch ];
//							for( m = k; m < chunkLength; m++ ) {
//								convBuf1[m] = 0.0f;
//							}
//						}
						activeChunks.removeElement( currentChunk );
						currentChunk.f.close();
						currentChunk.f = null;
						i--;
					}

					// mix to output buffer					
					for( ch = 0; ch < outChanNum; ch ++ ) {
						Util.add( inBuf[ ch % currentChunk.stream.channels ], j, outBuf[ ch ], j, k - j );
					}
				}
				progOff	+= chunkLength;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			
				// ---- save output ----

				if( floatF != null ) {
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < chunkLength; i++ ) {	// measure max amp
							f1 = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
						floatF[ ch ].writeFloats( convBuf1, 0, chunkLength );
					}
				} else {						// i.e. abs gain
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < chunkLength; i++ ) {	// measure max amp + adjust gain
							f1				= Math.abs( convBuf1[ i ]);
							convBuf1[ i ]  *= gain;
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
					outF.writeFrames( outBuf, 0, chunkLength );
				}
				framesWritten	+= chunkLength;
				progOff			+= chunkLength;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
				
			} // while framesWritten < outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- normalize output ----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				for( ch = 0; ch < outChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outF.close();
			outF	= null;

			// inform about clipping/ low level
			maxAmp		*= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			outStream	= null;
			outBuf		= null;
			convBuf1	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( sndChunks != null ) {
			for( i = 0; i < sndChunks.length; i++ ) {
				if( (sndChunks[ i ] != null) && (sndChunks[ i ].f != null) ) {
					sndChunks[ i ].f.cleanUp();
				}
			}
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

			ref		= new Param( AudioFileDescr.samplesToMillis( stream, stream.length ), Param.ABS_MS );
			ggSlave = (ParamField) gui.getItemObj( GG_OFFSET );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
			ggSlave = (ParamField) gui.getItemObj( GG_LENGTH );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}
		} catch( IOException e1 ) {}
	}

	/**
	 *	Maske zur Berechnung der Chunk Filenamen erstellen
	 *
	 *	gibt Zahl der Files zurueck oder Null bei Fehler
	 */
	protected int analyseFileNames()
	{
		String s1	= pr.text[ PR_INPUTFILE1 ];
		String s2	= pr.text[ PR_INPUTFILE2 ];
		int len1	= s1.length();
		int	len2	= s2.length();
		int	i, j, numStart, numEnd, numMin, numMax, num;
		char[]	numPad;
		
		// Mechanismus: erste Abweichung links->rechts + erste Abweichung rechts->links
		// finden, dann Bereich auf evtl. benachbarte numerische Charaktere ausdehnen
		// und als Integer interpretieren
		
		for( numStart = 0; numStart < Math.min( len1, len2 ); numStart++ ) {
			if( s1.charAt( numStart ) != s2.charAt( numStart )) break;
		}
		for( ; numStart > 0; numStart-- ) {
			if( !Character.isDigit( s1.charAt( numStart - 1 )) ||
				!Character.isDigit( s2.charAt( numStart - 1 ))) break;
		}
		for( numEnd = 0; numEnd < Math.min( len1, len2 ); numEnd++ ) {
			if( s1.charAt( len1 - numEnd - 1 ) != s2.charAt( len2 - numEnd - 1 )) break;
		}
		for( ; numEnd > 0; numEnd-- ) {
			if( !Character.isDigit( s1.charAt( len1 - numEnd )) ||
				!Character.isDigit( s2.charAt( len2 - numEnd ))) break;
		}
//System.out.println( "numStart "+numStart+"; numEnd "+numEnd+"; len1 "+len1+"; len2 "+len2 );

		try {
			i		= Integer.parseInt( s1.substring( numStart, len1 - numEnd ));
			j		= Integer.parseInt( s2.substring( numStart, len2 - numEnd ));
			numMin	= Math.min( i, j );
			numMax	= Math.max( i, j );
			reverse	= i > j;
			num		= numMax - numMin + 1;
			gui.stringToJTextField( num + " files detected.", GG_CURRENTINFO );
			
			fileNameArgs[1]	= s1.substring( 0, numStart );
//System.err.println( "beg \""+ fileNameArgs[1] + "\"" );
			fileNameArgs[2]	= s1.substring( len1 - numEnd );
//System.err.println( "end \""+ fileNameArgs[2] + "\"" );
			fileNameArgs[3]	= new Integer( numMin );
//System.err.println( "num \""+ fileNameArgs[3] + "\"" );
			i				= Math.min( len1, len2 ) - numEnd - numStart;
			numPad			= new char[ i + Math.max( len1, len2 ) - Math.min( len1, len2 )];
//			for( j = 0; j < i; j++ ) numPad[j] = '0';
			for( j = 0; j < numPad.length; j++ ) numPad[j] = '0';
			fileNameArgs[4]	= new String( numPad );
			usePad			= len1 == len2;
//System.err.println( "pad \""+ fileNameArgs[4] + "\"" );
			
			return num;
			
//		} catch( NumberFormatException e1 ) {
		} catch( Exception e1 ) {	// either NumberFormat or StringIndexOutOfBounds (when s1 == s2)
			numMin	= -1;
			numMax	= -1;
			gui.stringToJTextField( "Filename analysis failed!", GG_CURRENTINFO );
			return 0;
		}
	}

	/*
	 *	Generiert Filenamen aus mit analyseFilenames() erstellter Maske
	 *
	 *	id laueft von 0 bis numFiles-1
	 */
	protected String synthesizeFileName( int id )
	{
		String s		= String.valueOf( id + ((Integer) fileNameArgs[3]).intValue() );
		if( usePad ) {
			fileNameArgs[0]	= ((String) fileNameArgs[4]).substring( s.length() ) + s;
		} else {
			fileNameArgs[0]	= s;
		}
	
		return( MessageFormat.format( FILENAMEPTRN, fileNameArgs ));
	}
}
// class ConcatDlg

/**
 *	Interne Klasse fuer die Synthese
 */
class SoundChunk
{
	String		name;
	AudioFile	f			= null;
	AudioFileDescr	stream;
	int			off, len, over, fadeIn, fadeOut, punchIn;
} // class SoundChunk
