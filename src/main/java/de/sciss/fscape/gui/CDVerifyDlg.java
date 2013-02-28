/*
 *  CDVerifyDlg.java
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
 *		28-May-05	created
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
 *  Processing module for verifying the content
 *	of a burned audio CD, by subtracting the original
 *	and the re-imported sound file (automatically
 *	finding the gap at the beginning produced by burning the CD).
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.68, 28-May-05
 *
 *	@warning	this will fail to work if the original sound starts
 *				with a purely periodic sound or DC signal
 */
public class CDVerifyDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_ORIGINFILE	= 0;		// pr.text
	private static final int PR_COPYINFILE	= 1;
//	private static final int PR_OUTPUTFILE	= 2;
//	private static final int PR_OUTPUTTYPE	= 0;		// pr.intg
	private static final int PR_MAXGAP		= 0;		// pr.para
	private static final int PR_BITTOGGLE	= 0;		// pr.bool
	private static final int PR_QUICKABORT	= 1;

	private static final String PRN_ORIGINFILE	= "OrigInFile";
	private static final String PRN_COPYINFILE	= "CopyInFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
//	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_MAXGAP		= "MaxGap";
	private static final String PRN_BITTOGGLE	= "BitToggle";
	private static final String PRN_QUICKABORT	= "QuickAbort";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_ORIGINFILE, PRN_COPYINFILE, PRN_OUTPUTFILE };
//	private static final int		prIntg[]	= { 0 };
//	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE };
	private static final Param	prPara[]		= { null };
	private static final String	prParaName[]	= { PRN_MAXGAP };
	private static final boolean	prBool[]	= { true, false };
	private static final String	prBoolName[]	= { PRN_BITTOGGLE, PRN_QUICKABORT };

	private static final int GG_ORIGINFILE		= GG_OFF_PATHFIELD	+ PR_ORIGINFILE;
	private static final int GG_COPYINFILE		= GG_OFF_PATHFIELD	+ PR_COPYINFILE;
	private static final int GG_MAXGAP			= GG_OFF_PARAMFIELD	+ PR_MAXGAP;
	private static final int GG_BITTOGGLE		= GG_OFF_CHECKBOX	+ PR_BITTOGGLE;
	private static final int GG_QUICKABORT		= GG_OFF_CHECKBOX	+ PR_QUICKABORT;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_TOOSHORT		= "Input file too short!";
	private static final String	ERR_SYNCFAILED		= "Failed to synchronize inputs!";
	private static final String	ERR_DIFFERENTCHAN	= "Input files have different number of channels!";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public CDVerifyDlg()
	{
		super( "CD Verification" );
		init2();
	}
	
	protected void buildGUI()
	{
		// einmalig PropertyArray initialisieren
		if( static_pr == null ) {
			static_pr			= new PropertyArray();
			static_pr.text		= prText;
			static_pr.textName	= prTextName;
//			static_pr.intg		= prIntg;
//			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_MAXGAP ]		= new Param(  20.0, Param.ABS_MS );
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

		GridBagConstraints	con;

		PathField			ggInputFile1, ggInputFile2;
		JCheckBox			ggBitToggle, ggQuickAbort;
		ParamField			ggGap;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile1	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select original input file" );
		ggInputFile1.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Original input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile1, GG_ORIGINFILE, null );

		ggInputFile2	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select re-imported (burned) input file" );
		ggInputFile2.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Re-imported (copy) input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile2, GG_COPYINFILE, null );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Verfication Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggGap			= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max. Start Gap", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggGap, GG_MAXGAP, null );

		ggBitToggle		= new JCheckBox( "Ignore LSB toggle" );
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggBitToggle, GG_BITTOGGLE, null );

		con.weightx		= 0.1;
		con.gridwidth	= 2;
		gui.addLabel( new JLabel() );
		
		ggQuickAbort	= new JCheckBox( "Quick Abort" );
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggQuickAbort, GG_QUICKABORT, null );

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
		long				progOff, progLen;
		long				origOff, copyOff;
		int					maxGap;
		AudioFile			origInF	= null;
		AudioFile			copyInF	= null;
		AudioFile			outF	= null;
		AudioFileDescr		afdOrig, afdCopy;
		int					len, cancelCount, cancelDelta;
		float[][]			inBuf, compBuf;
		float				thresh;
		long				deviationOff, deviationCount, framesRemaining;
		float				f1, deviationMax;
		
topLevel: try {
		// ---- open input, output; init ----
			origInF			= AudioFile.openAsRead( new File( pr.text[ PR_ORIGINFILE ]));
			copyInF			= AudioFile.openAsRead( new File( pr.text[ PR_COPYINFILE ]));
			afdOrig			= origInF.getDescr();
			afdCopy			= copyInF.getDescr();
			
			if( afdOrig.channels != afdCopy.channels ) throw new IOException( ERR_DIFFERENTCHAN );
		
			maxGap	= (int) (AudioFileDescr.millisToSamples( afdOrig, pr.para[ PR_MAXGAP ].val ) + 0.5);
			if( maxGap > afdOrig.length - 8192 ) throw new IOException( ERR_TOOSHORT );
			
			inBuf			= new float[ afdOrig.channels ][ 8192 ];
			compBuf			= new float[ afdOrig.channels ][ 8192 + maxGap ];
			origOff			= 0;
			copyOff			= 0;
			origInF.seekFrame( origOff );
			len				= 0;
			cancelDelta		= 0;
			thresh			= pr.bool[ PR_BITTOGGLE ] ? 6.103517e-05f : 0.0f;
			
			progOff			= 0;
			progLen			= (long) afdCopy.length;
			
			do {
				origOff	   += len;
				copyOff	   += len;
				len			= (int) Math.min( 8192, Math.min( afdCopy.length - copyOff - maxGap, afdOrig.length - origOff ));
				if( len <= 0 ) throw new IOException( ERR_SYNCFAILED );
				origInF.readFrames( inBuf, 0, len );
				copyInF.seekFrame( copyOff );
				copyInF.readFrames( compBuf, 0, len + maxGap );
				
				cancelCount	= 0;
				
deltaLp:		for( int delta = 0; delta < maxGap; delta++ ) {
					for( int ch = 0; ch < inBuf.length; ch++ ) {
						for( int i = 0, j = delta; i < len; i++, j++ ) {
//							if( inBuf[ ch ][ i ] != compBuf[ ch ][ j ]) {
							if( Math.abs( inBuf[ ch ][ i ] - compBuf[ ch ][ j ]) > thresh ) continue deltaLp;
						}
					}
					cancelCount++;
					cancelDelta = delta;
				}
				if( cancelCount == 0 && copyOff > maxGap ) throw new IOException( ERR_SYNCFAILED );	// no sync withing 'maxgap' window
				
			// .... progress ....
				progOff += len;
				setProgression( (float) progOff / (float) progLen );
				
			} while( cancelCount != 1 && threadRunning );	// repeat during ambigious sync withing 'maxgap' window
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			System.err.println( "synced with "+cancelDelta+" frames offset!" );

			deviationCount	= 0;
			deviationMax	= 0.0f;
			deviationOff	= 0;
			copyOff += cancelDelta;
			
			framesRemaining = Math.min( afdCopy.length - copyOff, afdOrig.length - origOff );
			progLen	= progOff + framesRemaining;
			
			origInF.seekFrame( origOff );
			copyInF.seekFrame( copyOff );
verifyLp:	while( threadRunning && framesRemaining > 0 ) {
				len = (int) Math.min( 8192, framesRemaining );
				origInF.readFrames( inBuf, 0, len );
				copyInF.readFrames( compBuf, 0, len );
				for( int ch = 0; ch < inBuf.length; ch++ ) {
					for( int i = 0; i < len; i++ ) {
						f1 = Math.abs( inBuf[ ch ][ i ] - compBuf[ ch ][ i ]);
						if( f1 > thresh ) {
							deviationCount++;
							if( f1 > deviationMax ) {
								deviationMax	= f1;
								deviationOff	= copyInF.getFramePosition() - len + i;
							}
							if( pr.bool[ PR_QUICKABORT ]) break verifyLp;
						}
					}
				}

				framesRemaining -= len;
			// .... progress ....
				progOff += len;
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			if( deviationCount > 0 ) {
				setError( new Exception( deviationCount + " samples were not verified (max amp " +
					(Math.log( deviationMax ) / Constants.ln10 * 20) +  " dBFS at frame offset " + deviationOff + ")!" ));
				
			} else {
				System.err.println( "Successfully verified" );
				setProgression( 1.0f );
			}
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		finally {
			if( origInF != null ) origInF.cleanUp();
			if( copyInF != null ) copyInF.cleanUp();
			if( outF != null ) outF.cleanUp();
		}
	} // process()
}
// class CDVerifyDlg
