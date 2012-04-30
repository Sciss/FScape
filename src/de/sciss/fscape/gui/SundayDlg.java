/*
 *  SundayDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.io.IOUtil;

/**
 *	Something I tried out on a Sunday,
 *	can't remember what it was.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class SundayDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_WINDOW			= 3;
	private static final int PR_OVERLAP			= 4;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_CALCLEN			= 1;
	private static final int PR_STEPSIZE		= 2;
	private static final int PR_LPORDER			= 3;
	private static final int PR_RESIDUAL		= 0;		// pr.bool

	private static final String PRN_INPUTFILE	= "InputFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";
	private static final String PRN_CALCLEN		= "CalcLen";
	private static final String PRN_STEPSIZE	= "StepSize";
	private static final String PRN_LPORDER		= "LPOrder";
	private static final String PRN_RESIDUAL	= "Residual";
	private static final String PRN_WINDOW		= "Window";
	private static final String PRN_OVERLAP		= "Overlap";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, Filter.WIN_HANNING, 2 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_WINDOW, PRN_OVERLAP };
	private static final Param	prPara[]		= { null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_CALCLEN, PRN_STEPSIZE, PRN_LPORDER };
	private static final boolean	prBool[]	= { true };
	private static final String	prBoolName[]	= { PRN_RESIDUAL };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_WINDOW			= GG_OFF_CHOICE		+ PR_WINDOW;
	private static final int GG_OVERLAP			= GG_OFF_CHOICE		+ PR_OVERLAP;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_CALCLEN			= GG_OFF_PARAMFIELD	+ PR_CALCLEN;
	private static final int GG_STEPSIZE		= GG_OFF_PARAMFIELD	+ PR_STEPSIZE;
	private static final int GG_LPORDER			= GG_OFF_PARAMFIELD	+ PR_LPORDER;
	private static final int GG_RESIDUAL		= GG_OFF_CHECKBOX	+ PR_RESIDUAL;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SundayDlg()
	{
		super( "Sunday" );
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
			static_pr.para[ PR_CALCLEN ]	= new Param( 32.0, Param.ABS_MS );
			static_pr.para[ PR_STEPSIZE ]	= new Param(  8.0, Param.ABS_MS );
			static_pr.para[ PR_LPORDER ]	= new Param( 10.0, Param.NONE );
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
		Component[]			ggGain;
		PathField[]			ggInputs;
		ParamField			ggCalcLen, ggStepSize, ggLPOrder;
		JCheckBox			ggResidual;
		JComboBox			ggWindow, ggOverlap;
		String[]			winNames;

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
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Sun$E" );
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

	// -------- Settings-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "LP Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggCalcLen		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Analysis Size", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCalcLen, GG_CALCLEN, null );

		ggLPOrder		= new ParamField( new ParamSpace( 2.0, 100000.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "LP Order", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLPOrder, GG_LPORDER, null );

		ggStepSize		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Synthesis Size", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggStepSize, GG_STEPSIZE, null );

		winNames		= Filter.getWindowNames();
		ggWindow		= new JComboBox();
		for( int i = 0; i < winNames.length; i++ ) {
			ggWindow.addItem( winNames[ i ]);
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Window", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.4;
		gui.addChoice( ggWindow, GG_WINDOW, null );

		ggOverlap		= new JComboBox();
		for( int i = 0; i < 5; i++ ) {
			ggOverlap.addItem( "1/"+(1 << i) );
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Window Step", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggOverlap, GG_OVERLAP, null );

		ggResidual		= new JCheckBox();
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Residual", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.4;
		gui.addCheckbox( ggResidual, GG_RESIDUAL, null );

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
		int					i, j, k;
		int					len, ch;
		long				progOff, progLen;
		double				d1;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;
		float[][]			inBuf, outBuf;
		float[]				convBuf1, convBuf2;

		// Synthesize
		float				gain			= 1.0f;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		int					inLength, inChanNum;
		int					framesRead, framesWritten;

		float				maxAmp			= 0.0f;

		PathField			ggOutput;
		
		int					bufSize, bufSizeH, readOff;
		double				pow;

topLevel: try {
		// ---- open input, output; init ----

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
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			bufSizeH		= 64;
			bufSize			= bufSizeH << 1;;
			pow				= 1.0 / (bufSizeH - 1);
			inBuf			= new float[ inChanNum ][ bufSize ];
			outBuf			= new float[ inChanNum ][ bufSizeH ];
			Util.clear( inBuf );

			progOff			= 0;
			progLen			= (long) inLength * 3;

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
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
				progLen	   += inLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// =================== CORE ===================

			framesRead		= 0;
			framesWritten	= 0;
			readOff			= 0;

			while( threadRunning && (framesWritten < inLength) ) {
			
				// ---- get input ----
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ch], bufSizeH, inBuf[ch], 0, bufSizeH );
				}				
				len	= Math.min( bufSize - readOff, inLength - framesRead );
				inF.readFrames( inBuf, readOff, len );
				if( len + readOff < bufSize ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = readOff + len; i < bufSize; ) {
							convBuf1[ i++ ] = 0.0f;
						}
					}
				}
				progOff		+= len;
				framesRead	+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- process ----

				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1	= inBuf[ ch ];
					convBuf2	= outBuf[ ch ];
					
					for( j = 0; j < bufSizeH; j++ ) {
				
						d1			= convBuf1[j];
						
						for( i = j+1, k = bufSizeH-1; k > 0; i++, k-- ) {
//							d1 *= Math.pow( convBuf1[i], pow );
//							d1 *= 1.0 - Math.abs( convBuf1[i]);
							d1 *= convBuf1[i];
						}
//						convBuf2[j]	= (float) d1; // Math.pow( d1, pow );
						convBuf2[j]	= (float) Math.pow( d1, pow );
					}
				}
				progOff		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- save output ----

				len	= Math.min( bufSizeH, inLength - framesWritten );
				if( floatF != null ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < len; i++ ) {	// measure max amp
							f1 = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
						floatF[ ch ].writeFloats( convBuf1, 0, len );
					}
				} else {						// i.e. abs gain
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < len; i++ ) {	// measure max amp + adjust gain
							f1				= Math.abs( convBuf1[ i ]);
							convBuf1[ i ]  *= gain;
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
					outF.writeFrames( outBuf, 0, len );
				}
				framesWritten	+= len;
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
				
				readOff			= bufSizeH;

			} // while framesWritten < outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- normalize output ----

			inF.close();
			inF		= null;

			// sound file
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, inBuf, gain, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
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
			inStream	= null;
			outStream	= null;
			inBuf		= null;
			outBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
	} // process()

// -------- private Methoden --------
}
// class SundayDlg
