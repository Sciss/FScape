/*
 *  LueckenbuesserDlg.java
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
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  I hazily remember having seen this
 *	algorithm somewhere using CSound. It
 *	tracks two (preferably similar) soundfiles
 *	and zigzags between them whenever it
 *	finds suitable similarities or "wave crossings".
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class LueckenbuesserDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE1			= 0;		// pr.text
	private static final int PR_INPUTFILE2			= 1;
	private static final int PR_OUTPUTFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_PATIENCE			= 0;		// pr.para
	private static final int PR_CROSSFADE			= 1;
	private static final int PR_EQP					= 0;		// pr.bool

	private static final String PRN_INPUTFILE1		= "InputFile1";
	private static final String PRN_INPUTFILE2		= "InputFile2";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_EQP				= "EqualPower";
	private static final String PRN_PATIENCE		= "Patience";
	private static final String PRN_CROSSFADE		= "Crossfade";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE1, PRN_INPUTFILE2, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_PATIENCE, PRN_CROSSFADE };
	private static final boolean	prBool[]	= { false };
	private static final String	prBoolName[]	= { PRN_EQP };

	private static final int GG_INPUTFILE1		= GG_OFF_PATHFIELD	+ PR_INPUTFILE1;
	private static final int GG_INPUTFILE2		= GG_OFF_PATHFIELD	+ PR_INPUTFILE2;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_EQP				= GG_OFF_CHECKBOX	+ PR_EQP;
	private static final int GG_PATIENCE		= GG_OFF_PARAMFIELD	+ PR_PATIENCE;
	private static final int GG_CROSSFADE		= GG_OFF_PARAMFIELD	+ PR_CROSSFADE;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "All inputs must have the same # of channels!";
	private static final String	ERR_CHANNELS2		= "Inputs must have at least one channel!";
	private static final String	WARN_CHANNELS		= "Warning, multi channel input! Only first channel is analyzed!";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public LueckenbuesserDlg()
	{
		super( "L\u00FCckenb\u00FC\u00DFer" );
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
			static_pr.para[ PR_PATIENCE ]		= new Param( 100.0, Param.ABS_MS );
			static_pr.para[ PR_CROSSFADE ]		= new Param(  10.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile1, ggInputFile2, ggOutputFile;
		JCheckBox			ggEqP;
		ParamField			ggPatience, ggCrossFade;
		PathField[]			ggInputs;
		ParamSpace			spcPatience;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "File I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile1	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile1.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input A", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile1, GG_INPUTFILE1, null );

		ggInputFile2	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile2.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input B", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile2, GG_INPUTFILE2, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD,
										 "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 2 ];
		ggInputs[ 0 ]	= ggInputFile1;
		ggInputs[ 1 ]	= ggInputFile2;
		ggOutputFile.deriveFrom( ggInputs, "$D0$B0Fuse$B1$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
//		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );

	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		
		spcPatience		= new ParamSpace( 0.01, 10000.0, 0.01, Param.ABS_MS );
		ggPatience		= new ParamField( spcPatience );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Patience", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggPatience, GG_PATIENCE, null );

		ggCrossFade		= new ParamField( spcPatience );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossfade", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCrossFade, GG_CROSSFADE, null );

		ggEqP			= new JCheckBox( "Equal Power" );
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggEqP, GG_EQP, null );

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
		int					i, ch, len;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			outF			= null;
		AudioFileDescr			outStream		= null;
		Info[]				info			= new Info[2];

		int					chanNum, schoko, gaga, outBufOff, outBufLen;
		float				lastSample, switchSample;
		float[][]			outBuf;
		int					inBufLen		= 4096;
		int					patience, patienceReset, patienceLeft;
		int					crossfadeLeft, crossfadeLen;
		float[]				crossfade, crossfade2;

		PathField			ggOutput;

topLevel: try {

		// ---- open input, output; init ----

			// input
			chanNum		= 0;
			progOff		= 0;
			progLen		= 0;
			
			for( i = 0; i < 2; i++ ) {
				info[ i ]		= new Info();
				info[ i ].sf	= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE1 + i ]));
				info[ i ].stream= info[ i ].sf.getDescr();
				info[ i ].len	= (int) info[ i ].stream.length;
				if( i == 0 ) chanNum = info[ i ].stream.channels;
				else if( chanNum != info[ i ].stream.channels ) throw new IOException( ERR_CHANNELS );
				info[ i ].buf	= new float[ chanNum ][ inBufLen ];
				progLen		   += info[ i ].len;
			}
			
			if( chanNum < 1 ) throw new IOException( ERR_CHANNELS2 );
			if( chanNum != 1 ) System.out.println( WARN_CHANNELS );
			
			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( info[ 0 ].stream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outBufOff	= 0;
			outBufLen	= 4096;
			outBuf		= new float[ chanNum ][ outBufLen ];
			crossfadeLen= Math.max( 1, (int) (AudioFileDescr.millisToSamples( info[ 0 ].stream,
									pr.para[ PR_CROSSFADE ].val ) + 0.5) );
			patience	= Math.max( 1, (int) (AudioFileDescr.millisToSamples( info[ 0 ].stream,
									pr.para[ PR_PATIENCE ].val ) + 0.5) );

			crossfade	= Filter.createWindow( crossfadeLen, Filter.WIN_HANNING );
			crossfade2	= new float[ crossfadeLen ];
			if( pr.bool[ PR_EQP ]) {
				for( i = 0; i < crossfadeLen; i++ ) {
					crossfade2[ i ] = (float) Math.sqrt( 1.0 - crossfade[ i ] * crossfade[ i ] );
				}
			} else {
				for( i = 0; i < crossfadeLen; i++ ) {
					crossfade2[ i ] = 1.0f - crossfade[ i ];
				}
			}

		// ----==================== kulchur ====================----

			schoko			= 0;
			gaga			= 1;
			lastSample		= 0.0f;
			patienceLeft	= patience;
			patienceReset	= patience;
			crossfadeLeft	= 0;
			
halllo:		while( threadRunning ) {
				for( i = 0; i < 2; i++ ) {
					if( info[ i ].bufStop == info[ i ].bufOff ) {
						len	= Math.min( info[ i ].len - info[ i ].framesRead, inBufLen );
						info[ i ].sf.readFrames( info[ i ].buf, 0, len );
						info[ i ].bufOff	= 0;
						info[ i ].bufStop	= len;
						progOff += len;
						info[ i ].framesRead += len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
					if( info[ i ].bufStop == info[ i ].bufOff ) {
						schoko	= (i + 1) % 2;
						gaga	= i;
						break halllo;
					}
				}
					
				switchSample = info[ gaga ].buf[ 0 ][ info[ gaga ].bufOff ];
				for( ; info[ schoko ].bufOff < info[ schoko ].bufStop; info[ schoko ].bufOff++ ) {
					if( crossfadeLeft == 0 ) {
						f1		= info[ schoko ].buf[ 0 ][ info[ schoko ].bufOff ];
						if( (switchSample >= lastSample && switchSample <= f1) ||
							(switchSample <= lastSample && switchSample >= f1) ) {	// yeah switch
						
							schoko			= gaga;
							gaga			= 1 - schoko;
							lastSample		= switchSample;
							switchSample	= f1;
							patienceReset	= patience;
							patienceLeft	= patience;
							crossfadeLeft	= crossfadeLen - 1;
						} else {
							lastSample		= f1;
							patienceLeft--;
						}
					}
					
					for( ch = 0; ch < chanNum; ch++ ) {
//							outBuf[ ch ][ outBufOff ] = info[ schoko ].buf[ ch ][ info[ schoko ].bufOff ];
						outBuf[ ch ][ outBufOff ] =
							info[ schoko ].buf[ ch ][ info[ schoko ].bufOff ] * crossfade[ crossfadeLeft ] +
							info[ gaga ].buf[ ch ][ info[ gaga ].bufOff ] * crossfade2[ crossfadeLeft ];
					}
					if( ++outBufOff == outBufLen ) {	// flush
						outF.writeFrames( outBuf, 0, outBufOff );
						outBufOff = 0;
					}
					if( patienceLeft == 0 ) {
						patienceLeft = patienceReset;
						if( patienceReset > 1 ) patienceReset >>= 1;
						if( ++info[ gaga ].bufOff < info[ gaga ].bufStop ) {
							switchSample = info[ gaga ].buf[ 0 ][ info[ gaga ].bufOff ];
						} else {
							continue halllo;
						}
					}

					if( crossfadeLeft > 0 ) {
						crossfadeLeft--;
						if( ++info[ gaga ].bufOff == info[ gaga ].bufStop ) continue halllo;
					}
				}
			} // while( threadRunning )
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outF.writeFrames( outBuf, 0, outBufOff );	// flush
			outBufOff = 0;
			
		// ---- clean up, normalize ----

			for( i = 0; i < 2; i++ ) {
				info[ i ].sf.close();
				info[ i ].sf = null;
			}
			outF.close();
			outF = null;
			
			setProgression( 1.0f );

		// ---- Finish ----

			// inform about clipping/ low level
//			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}

	// ---- cleanup (topLevel) ----
		for( i = 0; i < 2; i++ ) {
			if( info[ i ] != null && info[ i ].sf != null ) {
				info[ i ].sf.cleanUp();
			}
		}
		if( outF != null ) {
			outF.cleanUp();
		}
	} // process()

// -------- private Methoden --------

	protected class Info
	{
		private AudioFile	sf;
		private AudioFileDescr	stream;
		private float[][]	buf;
		private int			bufOff = 0;
		private int			bufStop = 0;
		private int			framesRead = 0, len;
	}
} // class LueckenbuesserDlg