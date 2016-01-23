/*
 *  CombFilerDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
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
 *  Processing module for a plain comb filter.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class CombFilterDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_FREQ			= 1;
	private static final int PR_QUALITY			= 2;
	private static final int PR_DRYMIX			= 3;
	private static final int PR_WETMIX			= 4;
	private static final int PR_DRYINVERT		= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_FREQ			= "Freq";
	private static final String PRN_QUALITY			= "Quality";
	private static final String PRN_DRYMIX			= "DryMix";
	private static final String PRN_WETMIX			= "WetMix";
	private static final String PRN_DRYINVERT		= "DryInvert";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_FREQ, PRN_QUALITY, PRN_DRYMIX, PRN_WETMIX };
	private static final boolean	prBool[]	= { false };
	private static final String	prBoolName[]	= { PRN_DRYINVERT };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_FREQ			= GG_OFF_PARAMFIELD	+ PR_FREQ;
	private static final int GG_QUALITY			= GG_OFF_PARAMFIELD	+ PR_QUALITY;
	private static final int GG_DRYMIX			= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
	private static final int GG_WETMIX			= GG_OFF_PARAMFIELD	+ PR_WETMIX;
	private static final int GG_DRYINVERT		= GG_OFF_CHECKBOX	+ PR_DRYINVERT;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_FREQ			= "Illegal frequency!";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public CombFilterDlg()
	{
		super( "Comb Filter" );
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
			static_pr.para[ PR_FREQ ]		= new Param(  50.0, Param.ABS_HZ );
			static_pr.para[ PR_QUALITY ]	= new Param(  20.0, Param.FACTOR_AMP );
			static_pr.para[ PR_DRYMIX ]		= new Param(   0.0, Param.FACTOR_AMP );
			static_pr.para[ PR_WETMIX ]		= new Param( 100.0, Param.FACTOR_AMP );
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
		ParamField			ggFreq, ggQuality, ggDryMix, ggWetMix;
		JCheckBox			ggDryInvert;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

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
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Comb$E" );
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

	// -------- Filter-Gadgets --------
	gui.addLabel( new GroupLabel( "Filter Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggFreq			= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Base freq.", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggFreq, GG_FREQ, null );

		con.weightx		= 0.1;
		ggQuality		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		gui.addLabel( new JLabel( "Quality", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggQuality, GG_QUALITY, null );

		con.weightx		= 0.1;
		con.gridwidth	= 1;
		ggDryMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		gui.addLabel( new JLabel( "Dry mix", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggDryMix, GG_DRYMIX, null );

		ggDryInvert		= new JCheckBox( "Invert" );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggDryInvert, GG_DRYINVERT, null );

		ggWetMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Wet mix", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggWetMix, GG_WETMIX, null );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel( "" ));

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
		
//		[1 0 0 0 0 0 0 .... 0 -1] and A=[1 0 0 0 .... 0 -.1]
		
	protected void process()
	{
		int					i, j, ch, len;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile			floatF[]		= null;
		File				tempFile[]		= null;
		int					inLength, inChanNum, framesRead, framesWritten;

		// Synthesize
		float				gain;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		float				dryGain, wetGain, quality;
		int					delay;			// #samples of the IIR feedback/feedforward loop
		float				iirB0, iirBN, iirAN;	// filter coeffs; N = delay
		float[][]			inBuf, outBuf;
		float[]				rotBuf, convBuf1, convBuf2;
	
		float				maxAmp			= 0.0f;

		PathField			ggOutput;

topLevel: try {
		// ---- open input, output ----

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

			wetGain		= (float) (Param.transform( pr.para[ PR_WETMIX ], Param.ABS_AMP, ampRef, null )).val;
			dryGain		= (float) (Param.transform( pr.para[ PR_DRYMIX ], Param.ABS_AMP, ampRef, null )).val;
			quality		= (float) (Param.transform( pr.para[ PR_QUALITY ],Param.ABS_AMP, ampRef, null )).val;
			delay		= (int) (inStream.rate / pr.para[ PR_FREQ ].val + 0.5);
			if( (delay == 0) || (pr.para[ PR_FREQ ].val == 0.0) ) throw new IOException( ERR_FREQ );
			if( pr.bool[ PR_DRYINVERT ]) {
				dryGain = -dryGain;
			}

			progOff		= 0;
			progLen		= (long) inLength * 2;		// read, write
			
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
				progLen	   += (long) inLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				wetGain	   *= gain;
				dryGain	   *= gain;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// init iir filter
			iirB0		= 1.0f * wetGain + 1.0f * dryGain;
			iirBN		= 1.0f * wetGain;
			iirAN		= -1.0f + quality;
			
			inBuf		= new float[ inChanNum ][ delay + 8192 ];	// read starts at [delay]
			outBuf		= new float[ inChanNum ][ delay + 8192 ];	// write starts at [delay]
			rotBuf		= new float[ 8192 ];
			Util.clear( inBuf );
			Util.clear( outBuf );

		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;

			while( threadRunning && (framesWritten < inLength) ) {

				len = Math.min( 8192, inLength - framesRead );
			// ---- read input chunk ----
				inF.readFrames( inBuf, delay, len );
				framesRead	+= len;
				progOff		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

			// ---- filtering ----
				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = inBuf[ ch ];
					convBuf2 = outBuf[ ch ];
					for( i = 0, j = delay; i < len; i++, j++ ) {
						f1				= iirB0 * convBuf1[ j ] + iirBN * convBuf1[ i ] + iirAN * convBuf2[ i ];
						convBuf2[ j ]	= f1;
						if( Math.abs( f1 ) > maxAmp ) {
							maxAmp		= Math.abs( f1 );
						}
					}
				}

			// ---- write output chunk ----
				if( floatF != null ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						floatF[ ch ].writeFloats( outBuf[ ch ], delay, len );
					}
				} else {
					outF.writeFrames( outBuf, delay, len );
				}
				progOff		  += len;
				framesWritten += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

			// ---- delay buffers ----
			
				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = inBuf[ ch ];
					convBuf2 = outBuf[ ch ];
					Util.rotate( convBuf1, convBuf1.length, rotBuf, -len );
					Util.rotate( convBuf2, convBuf2.length, rotBuf, -len );
				}

			} // while frames not written
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== normalize output ====================----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				maxAmp	*= gain;

				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ]	= null;
					tempFile[ ch ].delete();
					tempFile[ ch ]	= null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;
			outStream	= null;
			inF.close();
			inF			= null;
			inStream	= null;
			inBuf		= null;
			outBuf		= null;
			rotBuf		= null;
			convBuf1	= null;
			convBuf2	= null;

			// inform about clipping/ low level
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
			rotBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));
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
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
				}
				if( tempFile[ ch ] != null ) {
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}
		}
	} // process()

// -------- private Methoden --------
}
// class CombFilterDlg
