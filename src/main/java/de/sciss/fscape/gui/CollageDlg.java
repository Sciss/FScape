/*
 *  CollageDlg.java
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
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

/**
 *  Processing module for mixing sounds together.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class CollageDlg
extends ModulePanel
{
// -------- public Variablen --------

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_OUTPUTFILE		= 0;		// pr.text
//	private static final int PR_BOARD			= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_OUTPUTCHAN		= 0;		// pr.para
	private static final int PR_GAIN			= 1;

	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_BOARD		= "Board";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputRes";
	private static final String PRN_OUTPUTCHAN	= "OutputChan";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_OUTPUTFILE, PRN_BOARD };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_OUTPUTCHAN, PRN_GAIN };

	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_OUTPUTCHAN		= GG_OFF_PARAMFIELD	+ PR_OUTPUTCHAN;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_BOARD			= GG_OFF_OTHER		+ 0;
	private static final int GG_INPUTFILE		= GG_OFF_OTHER		+ 1;
	private static final int GG_INGAIN			= GG_OFF_OTHER		+ 2;
	private static final int GG_INVERT			= GG_OFF_OTHER		+ 4;
	private static final int GG_SUMCHAN			= GG_OFF_OTHER		+ 5;
	private static final int GG_PAN				= GG_OFF_OTHER		+ 6;
	private static final int GG_FADEIN			= GG_OFF_OTHER		+ 7;
	private static final int GG_FADEINTYPE		= GG_OFF_OTHER		+ 8;
	private static final int GG_FADEINLEN		= GG_OFF_OTHER		+ 9;
	private static final int GG_FADEOUT			= GG_OFF_OTHER		+ 10;
	private static final int GG_FADEOUTTYPE		= GG_OFF_OTHER		+ 11;
	private static final int GG_FADEOUTLEN		= GG_OFF_OTHER		+ 12;
	private static final int GG_STARTTIME		= GG_OFF_OTHER		+ 13;
	private static final int GG_INFILEOFFSET	= GG_OFF_OTHER		+ 14;
	private static final int GG_LENGTH			= GG_OFF_OTHER		+ 15;
	private static final int GG_ENDTIME			= GG_OFF_OTHER		+ 16;
//	private static final int GG_ACTIONADD		= GG_OFF_OTHER		+ 17;
//	private static final int GG_ACTIONDEL		= GG_OFF_OTHER		+ 18;
//	private static final int GG_ACTIONDUP		= GG_OFF_OTHER		+ 19;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public CollageDlg()
	{
		super( "Collage" );
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
			static_pr.para[ PR_OUTPUTCHAN ]		= new Param(  2.0, Param.NONE );
			static_pr.paraName	= prParaName;
//			static_pr.bool		= prBool;
//			static_pr.boolName	= prBoolName;

//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;
		
		PathField			ggOutputFile, ggInputFile;
		JComboBox			ggFadeType;
		ParamField			ggOutChan, ggInGain, ggPan, ggFadeLen, ggStartTime, ggOffset, ggLength, ggEndTime;
		JCheckBox			ggInvert, ggMono, ggFade;
		CollageList			ggBoard;
		Component[]			ggGain;
		ParamSpace[]		spcOffset, spcLength;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- I/O-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform Output", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "File name", SwingConstants.RIGHT ));
		con.gridheight	= 2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
//		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.05;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.2;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.2;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

		ggOutChan		= new ParamField( new ParamSpace( 1.0, 10000.0, 1.0, Param.NONE ));
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "# of channels", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggOutChan, GG_OUTPUTCHAN, null );

	// -------- Collage Board --------
		con.fill		= GridBagConstraints.BOTH;
	gui.addLabel( new GroupLabel( "Collage Board", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggBoard			= new CollageList();
		con.weightx		= 0.9;
		con.weighty		= 0.9;
		con.gridwidth	= GridBagConstraints.REMAINDER;
//		con.gridwidth	= 4;
		con.gridheight	= 4;
//		gui.addGadget( ggCircuit, GG_CIRCUIT );
		gui.addGadget( ggBoard, GG_BOARD );
//		ggBoard.addActionListener( this );

		spcOffset		= new ParamSpace[ 3 ];
		spcOffset[0]	= Constants.spaces[ Constants.offsetMsSpace ];
		spcOffset[1]	= Constants.spaces[ Constants.offsetBeatsSpace ];
		spcOffset[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength		= new ParamSpace[ 4 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength[3]	= Constants.spaces[ Constants.factorTimeSpace ];

		con.weightx		= 0.05;
		con.weighty		= 0.0;
		con.gridheight	= 1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.fill		= GridBagConstraints.HORIZONTAL;
//		ggAction		= new JButton( "Add" );
//		gui.addButton( ggAction, GG_ACTIONADD, this );
//		ggAction		= new JButton( "Delete" );
//		gui.addButton( ggAction, GG_ACTIONDEL, this );
//		ggAction		= new JButton( "Duplicate" );
//		gui.addButton( ggAction, GG_ACTIONDUP, this );
//		gui.addLabel( new JLabel() );

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
									 "Select input sound" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggInGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggInGain, GG_INGAIN, null );

		ggInvert		= new JCheckBox( "Invert" );
		con.weightx		= 0.05;
		gui.addCheckbox( ggInvert, GG_INVERT, null );

		ggOffset		= new ParamField( spcOffset );
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Cut offset", SwingConstants.RIGHT ));
		con.weightx		= 0.3;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggOffset, GG_INFILEOFFSET, null );

		ggPan			= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Pan", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggPan, GG_PAN, null );

		ggMono			= new JCheckBox( "Make mono" );
		con.weightx		= 0.05;
		gui.addCheckbox( ggMono, GG_SUMCHAN, null );

		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Cut length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLength, GG_LENGTH, null );

		ggFade			= new JCheckBox( "Fade in" );
		con.weightx		= 0.05;
		con.gridwidth	= 1;
		gui.addCheckbox( ggFade, GG_FADEIN, null );

		ggFadeLen		= new ParamField( spcLength );
		con.weightx		= 0.4;
		gui.addParamField( ggFadeLen, GG_FADEINLEN, null );

		ggFadeType		= new JComboBox();
		ggFadeType.addItem( "Linear" );
		ggFadeType.addItem( "Slow rise" );
		ggFadeType.addItem( "Fast rise" );
		ggFadeType.addItem( "Easy in+out" );
		con.weightx		= 0.2;
		gui.addChoice( ggFadeType, GG_FADEINTYPE, null );

		ggStartTime		= new ParamField( spcOffset );
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "Start time", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggStartTime, GG_STARTTIME, null );
		
		ggFade			= new JCheckBox( "Fade out" );
		con.weightx		= 0.05;
		con.gridwidth	= 1;
		gui.addCheckbox( ggFade, GG_FADEOUT, null );

		ggFadeLen		= new ParamField( spcLength );
		con.weightx		= 0.4;
		gui.addParamField( ggFadeLen, GG_FADEOUTLEN, null );

		ggFadeType		= new JComboBox();
		ggFadeType.addItem( "Linear" );
		ggFadeType.addItem( "Slow fall" );
		ggFadeType.addItem( "Fast fall" );
		ggFadeType.addItem( "Easy in+out" );
		con.weightx		= 0.2;
		gui.addChoice( ggFadeType, GG_FADEOUTTYPE, null );

		ggEndTime		= new ParamField( spcOffset );
		con.weightx		= 0.05;
		gui.addLabel( new JLabel( "End time", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggEndTime, GG_ENDTIME, null );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
		
//		CircuitPanel cp = (CircuitPanel) gui.getItemObj( GG_CIRCUIT );
//		if( cp != null ) {
//			cp.setCircuit( pr.text[ PR_CIRCUIT ]);
//		}
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );

//		CircuitPanel cp = (CircuitPanel) gui.getItemObj( GG_CIRCUIT );
//		if( cp != null ) {
//			pr.text[ PR_CIRCUIT ] = cp.getCircuit();
//		}
	}
	
// -------- Processor Interface --------
		
	protected void process()
	{
/*		int			i, j, k, ch;
		long		progOff, progLen;
		float		maxAmp			= 0.0f;
	
		AudioFile	outF			= null;
		PathField	ggOutput;
		AudioFileDescr	outStream;

		int			outChanNum		= 1;		// fix
		int			outLength;
		int			fftLength;
		int			off;

		int			framesWritten;
		
		float		outBuf[], outBufWrap[][];
		float		impBuf[][];

		CircuitPanel	cp;
		Point			impLength;

		float		floaty;

		Param		ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		Param		peakGain;													// (abs amp)
		float		gain			= 1.0f;								 		// gain abs amp
		
topLevel: try {
		// ---- open files ----

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outF		= new AudioFile( pr.text[ PR_OUTPUTFILE ], AudioFile.MODE_OUTPUT | ggOutput.getType() );
			outStream	= new AudioFileDescr();
			ggOutput.fillStream( outStream );
			outStream.chanNum = outChanNum;
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- preparations ----

			cp			= (CircuitPanel) gui.getItemObj( GG_CIRCUIT );
			if( cp == null ) throw new IOException( ERR_MISSINGPROP );
			
			impLength		= calcLength( cp, outStream );
			outLength		= impLength.x + impLength.y;
			outStream.samples= outLength;
			if( (outLength*outChanNum) <= 0 ) throw new IOException( ERR_EMPTY );

			for( fftLength = 2; fftLength < outLength; fftLength <<= 1 ) ;

			// add "support"
			outStream.markers.addElement( new Marker( impLength.x, MARK_SUPPORT ));
			outF.initWriter( outStream );
// System.out.println( "impLength === "+impLength.x+" ... "+impLength.y+" fft "+fftLength );

			progOff		= 0;
			progLen		= outLength + (outLength & ~1);

		// ---- da Collage ----
	
			outBuf			= new float[ fftLength + 2 ];
			outBufWrap		= new float[ outChanNum ][];
			for( ch = 0; ch < outChanNum; ch++ ) {
				outBufWrap[ ch ] = outBuf;
			}
			impBuf			= new float[ 3 ][];
			impBuf[ 0 ]		= outBuf;
			impBuf[ 1 ]		= new float[ 1 ];
			impBuf[ 1 ][ 0 ]= 0.0f;	// time domain
			impBuf[ 2 ]		= new float[ impLength.x ];	// rotate buffer
			
			calcIR( cp, outStream, impBuf, impLength );
			progOff	+= (outLength >> 1);
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
			if( !threadRunning ) break topLevel;
			if( impBuf[ 1 ][ 0 ] == 1.0f ) {
				Fourier.realTransform( outBuf, fftLength, Fourier.INVERSE );
				Util.rotate( outBuf, fftLength, impBuf[ 2 ], impLength.x );	// undo rotation
			}
			progOff	+= (outLength >> 1);
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
			if( !threadRunning ) break topLevel;

		// ---- normalize output ----

			if( pr.bool[ PR_NORMGAIN ]) {
				for( i = 0; i < outLength; i++ ) {
					floaty = Math.abs( outBuf[ i ]);
					if( floaty > maxAmp ) {
						maxAmp = floaty;
					}
				}
				gain 	= 1.0f / maxAmp;
				maxAmp	= 1.0f;
				for( i = 0; i < outLength; i++ ) {
					outBuf[ i ] *= gain;
				}
			}

		// ---- write output ----

			for( framesWritten = 0; threadRunning && (framesWritten < outLength); ) {

				j  = Math.min( 8192, outLength - framesWritten );
				outF.writeFrames( outBufWrap, framesWritten, j );
				framesWritten	+= j;
				progOff			+= j;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----

			outF.close();
			outF		= null;
			outStream	= null;
		// .... check running ....
			if( !threadRunning ) break topLevel;

// System.out.println( "progOff "+progOff+"; progLen "+progLen );

		} // topLevel
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			impBuf		= null;
			outBuf		= null;
			outBufWrap	= null;
			outStream	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
*/	}
}
// class CollageDlg
