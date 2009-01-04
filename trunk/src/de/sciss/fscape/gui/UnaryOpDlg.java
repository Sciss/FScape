/*
 *  UnaryOpDlg.java
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
 *		09-Jan-05	correct offset unit switch, correct offset reference
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
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
 *  Processing module for algebraic
 *	modification of a sound files on
 *	a sample-by-sample basis.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 12-May-08
 */
public class UnaryOpDlg
extends DocumentFrame
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
	private static final int PR_OPERATOR		= 2;
	private static final int PR_GAINTYPE		= 3;
//	private static final int PR_INGAINTYPE		= 4;
	private static final int PR_GAIN			= 0;		// pr.para	
	private static final int PR_INPUTGAIN		= 1;
	private static final int PR_OFFSET			= 2;
	private static final int PR_LENGTH			= 3;
	private static final int PR_DRYMIX			= 4;
	private static final int PR_WETMIX			= 5;
	private static final int PR_HASIMINPUT		= 0;		// pr.bool
	private static final int PR_HASIMOUTPUT		= 1;
	private static final int PR_RECTIFY			= 2;
	private static final int PR_INVERT			= 3;
	private static final int PR_DRYINVERT		= 4;
	private static final int PR_REVERSE			= 5;

	private static final int OP_NONE			= 0;
	private static final int OP_SIN				= 1;
	private static final int OP_SQR				= 2;
	private static final int OP_SQRT			= 3;
	private static final int OP_LOG				= 4;
	private static final int OP_EXP				= 5;
	private static final int OP_RECT2POLARW		= 6;
	private static final int OP_RECT2POLAR		= 7;
	private static final int OP_POLAR2RECT		= 8;
	private static final int OP_NOT				= 9;

	private static final String OPNAMES[]		= { "Untouched", "Sin(x)", "Square", "Square root", "Log(x)", "Exp(x)",
													"Rect->Polar (wrapped)", "Rect->Polar", "Polar->Rect", "NOT" };

	private static final String PRN_REINPUTFILE		= "ReInFile";
	private static final String PRN_IMINPUTFILE		= "ImInFile";
	private static final String PRN_REOUTPUTFILE	= "ReOutFile";
	private static final String PRN_IMOUTPUTFILE	= "ImOutFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_OPERATOR		= "Operator";
	private static final String PRN_INGAINTYPE		= "InGainType";
	private static final String PRN_INPUTGAIN		= "InGain";
	private static final String PRN_OFFSET			= "Offset";
	private static final String PRN_LENGTH			= "Length";
	private static final String PRN_DRYMIX			= "DryMix";
	private static final String PRN_WETMIX			= "WetMix";
	private static final String PRN_HASIMINPUT		= "HasImInput";
	private static final String PRN_HASIMOUTPUT		= "HasImOutput";
	private static final String PRN_RECTIFY			= "Rectify";
	private static final String PRN_INVERT			= "Invert";
	private static final String PRN_DRYINVERT		= "DryInvert";
	private static final String PRN_REVERSE			= "Reverse";

	private static final String	prText[]			= { "", "", "", "" };
	private static final String	prTextName[]		= { PRN_REINPUTFILE, PRN_IMINPUTFILE, PRN_REOUTPUTFILE,
														PRN_IMOUTPUTFILE };
	private static final int	prIntg[]			= { 0, 0, OP_NONE, GAIN_UNITY, GAIN_ABSOLUTE };
	private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OPERATOR, PRN_GAINTYPE,
														PRN_INGAINTYPE };
	private static final Param	prPara[]			= { null, null, null, null, null, null };
	private static final String	prParaName[]		= { PRN_GAIN, PRN_INPUTGAIN, PRN_OFFSET, PRN_LENGTH,
														PRN_DRYMIX, PRN_WETMIX };
	private static final boolean	prBool[]		= { false, false, false, false, false, false };
	private static final String	prBoolName[]		= { PRN_HASIMINPUT, PRN_HASIMOUTPUT, PRN_RECTIFY, PRN_INVERT,
														PRN_DRYINVERT, PRN_REVERSE };

	private static final int GG_REINPUTFILE			= GG_OFF_PATHFIELD	+ PR_REINPUTFILE;
	private static final int GG_IMINPUTFILE			= GG_OFF_PATHFIELD	+ PR_IMINPUTFILE;
	private static final int GG_REOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_REOUTPUTFILE;
	private static final int GG_IMOUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_IMOUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OPERATOR			= GG_OFF_CHOICE		+ PR_OPERATOR;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
//	private static final int GG_INGAINTYPE			= GG_OFF_CHOICE		+ PR_INGAINTYPE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_INPUTGAIN			= GG_OFF_PARAMFIELD	+ PR_INPUTGAIN;
	private static final int GG_OFFSET				= GG_OFF_PARAMFIELD	+ PR_OFFSET;
	private static final int GG_LENGTH				= GG_OFF_PARAMFIELD	+ PR_LENGTH;
	private static final int GG_DRYMIX				= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
	private static final int GG_WETMIX				= GG_OFF_PARAMFIELD	+ PR_WETMIX;
	private static final int GG_HASIMINPUT			= GG_OFF_CHECKBOX	+ PR_HASIMINPUT;
	private static final int GG_HASIMOUTPUT			= GG_OFF_CHECKBOX	+ PR_HASIMOUTPUT;
	private static final int GG_RECTIFY				= GG_OFF_CHECKBOX	+ PR_RECTIFY;
	private static final int GG_INVERT				= GG_OFF_CHECKBOX	+ PR_INVERT;
	private static final int GG_DRYINVERT			= GG_OFF_CHECKBOX	+ PR_DRYINVERT;
	private static final int GG_REVERSE				= GG_OFF_CHECKBOX	+ PR_REVERSE;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_NOTCOMPLEX		= "This operation requires complex input";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public UnaryOpDlg()
	{
		super( "Unary Operator" );
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
			static_pr.para[ PR_GAIN ]		= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_INPUTGAIN ]	= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_OFFSET ]		= new Param(   0.0, Param.ABS_MS );
			static_pr.para[ PR_LENGTH ]		= new Param( 100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_DRYMIX ]		= new Param(   0.0, Param.FACTOR_AMP );
			static_pr.para[ PR_WETMIX ]		= new Param( 100.0, Param.FACTOR_AMP );
			static_pr.paraName	= prParaName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.superPr	= DocumentFrame.static_pr;
		}
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggImInputFile, ggReInputFile, ggReOutputFile, ggImOutputFile;
		ParamField			ggInputGain, ggOffset, ggLength, ggDryMix, ggWetMix;
		JComboBox			ggOperator;
		JCheckBox			ggHasImInput, ggHasImOutput, ggRectify, ggInvert, ggDryInvert, ggReverse;
		PathField[]			ggParent1, ggParent2;
		Component[]			ggGain;
		ParamSpace[]		spcOffset, spcLength;
		int					anchor;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
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
		
		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case PR_REINPUTFILE:
					setInput( ((PathField) e.getSource()).getPath().getPath() );
					break;
				}
			}
		};
		
	// -------- Input-Gadgets --------
		spcOffset		= new ParamSpace[ 3 ];
		spcOffset[0]	= new ParamSpace( -36000000.0, 36000000.0, 0.1,   Param.ABS_MS );	// allow negative offset
		spcOffset[1]	= new ParamSpace(   -240000.0,   240000.0, 0.001, Param.ABS_BEATS );
		spcOffset[2]	= new ParamSpace(	   -100.0,      100.0, 0.01,  Param.FACTOR_TIME );
//		spcOffset[0]	= Constants.spaces[ Constants.absMsSpace ];
//		spcOffset[1]	= Constants.spaces[ Constants.absBeatsSpace ];
//		spcOffset[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength		= new ParamSpace[ 4 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength[3]	= Constants.spaces[ Constants.factorTimeSpace ];

		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new GroupLabel( "Input file", GroupLabel.ORIENT_HORIZONTAL,
									  GroupLabel.BRACE_NONE ));
		con.fill		= GridBagConstraints.HORIZONTAL;

		ggReInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select real part of input" );
		ggReInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input [Real]", JLabel.RIGHT ));
		con.gridwidth	= 2;
		con.weightx		= 3.0;
		gui.addPathField( ggReInputFile, GG_REINPUTFILE, pathL );

		ggOffset		= new ParamField( spcOffset );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Offset", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggOffset, GG_OFFSET, null );
	
		ggImInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select imaginary part of input" );
		ggImInputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImInput	= new JCheckBox( "Input [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		anchor			= con.anchor;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImInput, GG_HASIMINPUT, il );
		con.anchor		= anchor;
		con.weightx		= 3.0;
		con.gridwidth	= 2;
		gui.addPathField( ggImInputFile, GG_IMINPUTFILE, pathL );

		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Length", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLength, GG_LENGTH, null );

		ggParent1		= new PathField[ 1 ];
		ggParent1[ 0 ]	= ggReInputFile;
		ggParent2		= new PathField[ 1 ];
		ggParent2[ 0 ]	= ggReInputFile;
		ggImInputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );

		ggInputGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Drive", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggInputGain, GG_INPUTGAIN, null );

		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "" ));
		ggRectify		= new JCheckBox( "Rectify" );
		con.gridwidth	= 1;
		con.weightx		= 0.4;
		gui.addCheckbox( ggRectify, GG_RECTIFY, il );
		ggInvert		= new JCheckBox( "Invert" );
		gui.addCheckbox( ggInvert, GG_INVERT, il );
		ggReverse		= new JCheckBox( "Reverse" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggReverse, GG_REVERSE, il );

	// -------- Output-Gadgets --------
	gui.addLabel( new GroupLabel( "Output", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggReOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output for real part" );
		ggReOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "File [Real]", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggReOutputFile, GG_REOUTPUTFILE, pathL );
		gui.registerGadget( ggReOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggReOutputFile.getResGadget(), GG_OUTPUTRES );

		ggImOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE, "Select output for imaginary part" );
//		ggImOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImOutput	= new JCheckBox( "File [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		anchor			= con.anchor;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImOutput, GG_HASIMOUTPUT, il );
		con.anchor		= anchor;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImOutputFile, GG_IMOUTPUTFILE, pathL );

		ggParent2		= new PathField[ 1 ];
		ggParent2[ 0 ]	= ggReOutputFile;
		ggReOutputFile.deriveFrom( ggParent1, "$D0$F0Op$E" );
		ggImOutputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );
		
		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Operation", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOperator		= new JComboBox();
		for( int i = 0; i < OPNAMES.length; i++ ) {
			ggOperator.addItem( OPNAMES[ i ]);
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Operator", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggOperator, GG_OPERATOR, il );

//		con.weightx		= 0.1;
//		gui.addLabel( new JLabel( "" ));
		ggDryMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		gui.addLabel( new JLabel( "Dry mix", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= 2;
		gui.addParamField( ggDryMix, GG_DRYMIX, null );

		ggDryInvert		= new JCheckBox( "Invert" );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggDryInvert, GG_DRYINVERT, il );

//		con.gridwidth	= 3;
		con.gridwidth	= 2;
		gui.addLabel( new JLabel( "" ));
		ggWetMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Wet mix", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= 2;
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
		
	/**
	 *	Translation durchfuehren
	 */
	public void process()
	{
		int					i, j, k;
		int					ch, len;
		float				f1;
		double				d1, d2, d3, d4, d5;
		long				progOff, progLen, lo;
		
		// io
		AudioFile			reInF			= null;
		AudioFile			imInF			= null;
		AudioFile			reOutF			= null;
		AudioFile			imOutF			= null;
		AudioFileDescr		reInStream		= null;
		AudioFileDescr		imInStream		= null;
		AudioFileDescr		reOutStream		= null;
		AudioFileDescr		imOutStream		= null;
		FloatFile			reFloatF[]		= null;
		FloatFile			imFloatF[]		= null;
		File				reTempFile[]	= null;
		File				imTempFile[]	= null;
		int					outChanNum;

		float[][]			reInBuf;										// [ch][i]
		float[][]			imInBuf;										// [ch][i]
		float[][]			reOutBuf		= null;							// [ch][i]
		float[][]			imOutBuf		= null;							// [ch][i]
		float[]				convBuf1, convBuf2;
		boolean				complex;

		PathField			ggOutput;

		// Synthesize
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float				gain;												 		// gain abs amp
		float				dryGain, wetGain;
		float				inGain;
		float				maxAmp			= 0.0f;
		Param				peakGain;

		int					inLength, inOff;
		int					pre;
		int					post;
		int					length;
		int					framesRead, framesWritten, outLength;
		boolean				polarIn, polarOut;

		// phase unwrapping
		double[]			phi;
		int[]				wrap;
		double[]			carry;
		
		Param				lenRef;

topLevel: try {

			complex = pr.bool[ PR_HASIMINPUT ] || pr.bool[ PR_HASIMOUTPUT ];
			polarIn	= pr.intg[ PR_OPERATOR ] == OP_POLAR2RECT;
			polarOut= pr.intg[ PR_OPERATOR ] == OP_RECT2POLAR;
			if( (polarIn || polarOut) && !complex ) throw new IOException( ERR_NOTCOMPLEX );

		// ---- open input ----

			reInF				= AudioFile.openAsRead( new File( pr.text[ PR_REINPUTFILE ]));
			reInStream			= reInF.getDescr();
			inLength			= (int) reInStream.length;
			reInBuf				= new float[ reInStream.channels ][ 8192 ];
			imInBuf				= new float[ reInStream.channels ][ 8192 ];

			if( pr.bool[ PR_HASIMINPUT ]) {
				imInF			= AudioFile.openAsRead( new File( pr.text[ PR_IMINPUTFILE ]));
				imInStream		= imInF.getDescr();
				if( imInStream.channels != reInStream.channels ) throw new IOException( ERR_COMPLEX );
				inLength		= (int) Math.min( inLength, imInStream.length );
			}

			lenRef			= new Param( AudioFileDescr.samplesToMillis( reInStream, inLength ), Param.ABS_MS );
			d1				= AudioFileDescr.millisToSamples(
								reInStream, (Param.transform( pr.para[ PR_OFFSET ], Param.ABS_MS, lenRef, null ).val ));
			j				= (int) (d1 >= 0.0 ? (d1 + 0.5) : (d1 - 0.5));	// correct rounding for negative values!
			length			= (int) (AudioFileDescr.millisToSamples( reInStream,
									 (Param.transform( pr.para[ PR_LENGTH ], Param.ABS_MS, lenRef, null )).val ) + 0.5);

// System.err.println( "offset = "+j );

			if( j >= 0 ) {
				inOff		= Math.min( j, inLength );
				if( !pr.bool[ PR_REVERSE ]) {
					reInF.seekFrame( inOff );
					if( pr.bool[ PR_HASIMINPUT ]) {
						imInF.seekFrame( inOff );
					}
				}
				inLength   -= inOff;
				pre			= 0;
			} else {
				inOff		= 0;
				pre			= Math.min( -j, length );
			}
			inLength		= Math.min( inLength, length - pre );
			post			= length - pre - inLength;
			
			if( pr.bool[ PR_REVERSE ]) {
				i			= pre;
				pre			= post;
				post		= i;
				inOff	   += inLength;
			}

		// .... check running ....
			if( !threadRunning ) break topLevel;

// for( op = 0; op < 2; op++ ) {
// 	System.out.println( op +": pre "+pre[op]+" / len "+inLength[op]+" / post "+post[op] );
// }
// System.out.println( "tot "+length[0]);

			outLength	= length;
			outChanNum	= reInStream.channels;

		// ---- open output ----

			ggOutput	= (PathField) gui.getItemObj( GG_REOUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			reOutStream	= new AudioFileDescr( reInStream );
			ggOutput.fillStream( reOutStream );
			reOutStream.channels = outChanNum;
			// well, more sophisticated code would
			// move and truncate the markers...
			if( (pre == 0) /* && (post == 0) */ ) {
				reInF.readMarkers();
				reOutStream.setProperty( AudioFileDescr.KEY_MARKERS,
				    reInStream.getProperty( AudioFileDescr.KEY_MARKERS ));
			}
			reOutF		= AudioFile.openAsWrite( reOutStream );
			reOutBuf	= new float[ outChanNum ][ 8192 ];
			imOutBuf	= new float[ outChanNum ][ 8192 ];

			if( pr.bool[ PR_HASIMOUTPUT ]) {
				imOutStream				= new AudioFileDescr( reInStream );
				ggOutput.fillStream( imOutStream );
				imOutStream.channels	= outChanNum;
				imOutStream.file		= new File( pr.text[ PR_IMOUTPUTFILE ]);
				imOutF					= AudioFile.openAsWrite( imOutStream );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Further inits ----

			phi			= new double[ outChanNum ];
			wrap		= new int[ outChanNum ];
			carry		= new double[ outChanNum ];
			for( ch = 0; ch < outChanNum; ch++ ) {
				phi[ ch ]	= 0.0;
				wrap[ ch ]	= 0;
				carry[ ch ]	= Double.NEGATIVE_INFINITY;
			}

			progOff		= 0;			// read, transform, write
			progLen		= (long) outLength * 3;

			wetGain		= (float) (Param.transform( pr.para[ PR_WETMIX ], Param.ABS_AMP, ampRef, null )).val;
			dryGain		= (float) (Param.transform( pr.para[ PR_DRYMIX ], Param.ABS_AMP, ampRef, null )).val;
			if( pr.bool[ PR_DRYINVERT ]) {
				dryGain = -dryGain;
			}
			inGain		= (float) (Param.transform( pr.para[ PR_INPUTGAIN ], Param.ABS_AMP, ampRef, null )).val;
			if( pr.bool[ PR_INVERT ]) {
				inGain = -inGain;
			}
			
			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				reTempFile	= new File[ outChanNum ];
				reFloatF	= new FloatFile[ outChanNum ];
				for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
					reTempFile[ ch ]	= null;
					reFloatF[ ch ]		= null;
				}
				for( ch = 0; ch < outChanNum; ch++ ) {
					reTempFile[ ch ]	= IOUtil.createTempFile();
					reFloatF[ ch ]		= new FloatFile( reTempFile[ ch ], FloatFile.MODE_OUTPUT );
				}
				if( pr.bool[ PR_HASIMOUTPUT ]) {
					imTempFile	= new File[ outChanNum ];
					imFloatF	= new FloatFile[ outChanNum ];
					for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
						imTempFile[ ch ]	= null;
						imFloatF[ ch ]		= null;
					}
					for( ch = 0; ch < outChanNum; ch++ ) {
						imTempFile[ ch ]	= IOUtil.createTempFile();
						imFloatF[ ch ]		= new FloatFile( imTempFile[ ch ], FloatFile.MODE_OUTPUT );
					}
				}
				progLen	   += outLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				wetGain	   *= gain;
				dryGain	   *= gain;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		
		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;
			
			while( threadRunning && (framesWritten < outLength) ) {
				// ---- choose chunk len ----
				len = Math.min( 8192, outLength - framesWritten );
				if( pre > 0 ) {
					len	= Math.min( len, pre );
				} else if( inLength > 0 ) {
					len = Math.min( len, inLength );
				} else {
					len = Math.min( len, post );
				}

				// ---- read input chunks ----
				if( pre > 0 ) {
					Util.clear( reInBuf );
					if( complex ) {
						Util.clear( imInBuf );
					}
					pre  -= len;
				} else if( inLength > 0 ) {
					if( pr.bool[ PR_REVERSE ]) {	// ---- read reversed ----
						reInF.seekFrame( inOff - framesRead - len );
						reInF.readFrames( reInBuf, 0, len );
						for( ch = 0; ch < reInStream.channels; ch++ ) {
							convBuf1 = reInBuf[ ch ];
							for( i = 0, j = len - 1; i < j; i++, j-- ) {
								f1				= convBuf1[ j ];
								convBuf1[ j ]	= convBuf1[ i ];
								convBuf1[ i ]	= f1;
							}
						}
						if( pr.bool[ PR_HASIMINPUT ]) {
							imInF.seekFrame( inOff - framesRead - len );
							imInF.readFrames( imInBuf, 0, len );
							for( ch = 0; ch < imInStream.channels; ch++ ) {
								convBuf1 = imInBuf[ ch ];
								for( i = 0, j = len - 1; i < j; i++, j-- ) {
									f1				= convBuf1[ j ];
									convBuf1[ j ]	= convBuf1[ i ];
									convBuf1[ i ]	= f1;
								}
							}
						} else if( complex ) {
							Util.clear( imInBuf );
						}
					} else {						// ---- read normal ----
						reInF.readFrames( reInBuf, 0, len );
						if( pr.bool[ PR_HASIMINPUT ]) {
							imInF.readFrames( imInBuf, 0, len );
						} else if( complex ) {
							Util.clear( imInBuf );
						}
					}
					inLength	-= len;
					framesRead	+= len;
				} else {
					Util.clear( reInBuf );
					if( complex ) {
						Util.clear( imInBuf );
					}
					post -= len;
				}
				progOff += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- save dry signal ----
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = reInBuf[ ch ];
					convBuf2 = reOutBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						convBuf2[ i ] = convBuf1[ i ] * dryGain;
					}
					if( complex ) {
						convBuf1 = imInBuf[ ch ];
						convBuf2 = imOutBuf[ ch ];
						for( i = 0; i < len; i++ ) {
							convBuf2[ i ] = convBuf1[ i ] * dryGain;
						}
					}
				}

				// ---- rectify + apply input gain ----
				for( ch = 0; ch < reInStream.channels; ch++ ) {
					convBuf1 = reInBuf[ ch ];
					convBuf2 = imInBuf[ ch ];
					// ---- rectify ----
					if( pr.bool[ PR_RECTIFY ]) {
						if( complex ) {
							if( polarIn ) {
								for( i = 0; i < len; i++ ) {
									convBuf2[ i ]	= 0.0f;
								}
							} else {
								for( i = 0; i < len; i++ ) {
									d1				= convBuf1[ i ];
									d2				= convBuf2[ i ];
									convBuf1[ i ]	= (float) Math.sqrt( d1*d1 + d2*d2 );
									convBuf2[ i ]	= 0.0f;
								}
							}
						} else {
							for( i = 0; i < len; i++ ) {
								convBuf1[ i ]	= Math.abs( convBuf1[ i ]);
							}
						}
					}
					// ---- apply input gain ----
					Util.mult( convBuf1, 0, len, inGain );
					if( complex & !polarIn ) {
						Util.mult( convBuf2, 0, len, inGain );
					}
				}
				
				// ---- heart of the dragon ----
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = reInBuf[ ch ];
					convBuf2 = imInBuf[ ch ];

					switch( pr.intg[ PR_OPERATOR ]) {
					case OP_NONE:										// ================ None ================
						for( i = 0; i < len; i++ ) {
							reOutBuf[ ch ][ i ] += wetGain * convBuf1[ i ];
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								imOutBuf[ ch ][ i ] += wetGain * convBuf2[ i ];
							}
						}
						break;
						
					case OP_SIN:										// ================ Cosinus ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (float) Math.sin( convBuf1[ i ] * Math.PI );
								imOutBuf[ ch ][ i ] += wetGain * (float) Math.sin( convBuf2[ i ] * Math.PI );
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (float) Math.sin( convBuf1[ i ] * Math.PI );
							}
						}
						break;

					case OP_SQR:										// ================ Square ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (convBuf1[i]*convBuf1[i] - convBuf2[i]*convBuf2[i]);
								imOutBuf[ ch ][ i ] -= wetGain * (convBuf1[i]*convBuf2[i]*2);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (convBuf1[i]*convBuf1[i]);
							}
						}
						break;

					case OP_SQRT:										// ================ Square root ================
						if( complex ) {
							d3 = phi[ ch ];
							k  = wrap[ ch ];
							d4 = k * Constants.PI2;
							for( i = 0; i < len; i++ ) {
								d1 = wetGain * Math.pow( convBuf1[ i ] * convBuf1[ i ] + convBuf2[ i ] * convBuf2[ i ], 0.25 );
								d2 = Math.atan2( convBuf2[i], convBuf1[i] );
								if( d2 - d3 > Math.PI ) {
									k--;
									d4 = k * Constants.PI2;
								} else if( d3 - d2 > Math.PI ) {
									k++;
									d4 = k * Constants.PI2;
								}
								d2 += d4;
								d3  = d2;
								d2 /= 2;
								reOutBuf[ ch ][ i ] += (float) (d1 * Math.cos( d2 ));
								imOutBuf[ ch ][ i ] += (float) (d1 * Math.sin( d2 ));
							}
							phi[ ch ]	= d3;
							wrap[ ch ]	= k;

						} else {
							for( i = 0; i < len; i++ ) {
								f1	= convBuf1[ i ];
								if( f1 > 0 ) {
									reOutBuf[ ch ][ i ] += wetGain * (float) Math.sqrt( f1 );
								} // else undefiniert
							}
						}
						break;

					case OP_RECT2POLARW:									// ================ Rect->Polar (wrapped) ================
						for( i = 0; i < len; i++ ) {
							d1 = wetGain * Math.sqrt( convBuf1[ i ] * convBuf1[ i ] + convBuf2[ i ] * convBuf2[ i ]);
							d2 = Math.atan2( convBuf2[i], convBuf1[i] );
							reOutBuf[ ch ][ i ] += (float) d1;
							imOutBuf[ ch ][ i ] += (float) d2;
						}
						break;

					case OP_RECT2POLAR:									// ================ Rect->Polar ================
						d3 = phi[ ch ];
						k  = wrap[ ch ];
						d4 = k * Constants.PI2;
						for( i = 0; i < len; i++ ) {
							d1 = wetGain * Math.sqrt( convBuf1[ i ] * convBuf1[ i ] + convBuf2[ i ] * convBuf2[ i ]);
							d2 = Math.atan2( convBuf2[i], convBuf1[i] );
							if( d2 - d3 > Math.PI ) {
								k--;
								d4 = k * Constants.PI2;
							} else if( d3 - d2 > Math.PI ) {
								k++;
								d4 = k * Constants.PI2;
							}
							d2 += d4;
							reOutBuf[ ch ][ i ] += (float) d1;
							imOutBuf[ ch ][ i ] += (float) d2;
							d3  = d2;
						}
						phi[ ch ]	= d3;
						wrap[ ch ]	= k;
						break;

					case OP_POLAR2RECT:									// ================ Polar->Rect ================
						for( i = 0; i < len; i++ ) {
							f1 = wetGain * convBuf1[ i ];
							reOutBuf[ ch ][ i ] += f1 * (float) Math.cos( convBuf2[ i ]);
							imOutBuf[ ch ][ i ] += f1 * (float) Math.sin( convBuf2[ i ]);
						}
						break;

					case OP_LOG:										// ================ Log ================
						if( complex ) {
							d3 = phi[ ch ];
							k  = wrap[ ch ];
							d4 = k * Constants.PI2;
							d5 = carry[ ch ];
							for( i = 0; i < len; i++ ) {
								d1 = Math.sqrt( convBuf1[i] * convBuf1[i] + convBuf2[i] * convBuf2[i] );
								d2 = Math.atan2( convBuf2[i], convBuf1[i] );
								if( d2 - d3 > Math.PI ) {
									k--;
									d4 = k * Constants.PI2;
								} else if( d3 - d2 > Math.PI ) {
									k++;
									d4 = k * Constants.PI2;
								}
								if( d1 > 0.0 ) {
									d5 = Math.log( d1 );
								}
								d2 += d4;
								reOutBuf[ ch ][ i ] += (float) d5;
								imOutBuf[ ch ][ i ] += (float) d2;
								d3  = d2;
							}
							phi[ ch ]	= d3;
							wrap[ ch ]	= k;
							carry[ ch ]	= d5;
							
						} else {
							for( i = 0; i < len; i++ ) {
								f1	= convBuf1[ i ];
								if( f1 > 0 ) {
									reOutBuf[ ch ][ i ] += wetGain * (float) Math.log( f1 );
								} // else undefiniert
							}
						}
						break;

					case OP_EXP:										// ================ Exp ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								d1 = wetGain * Math.exp( convBuf1[ i ]);
								reOutBuf[ ch ][ i ] += (float) (d1 * Math.cos( convBuf2[i] ));
								imOutBuf[ ch ][ i ] += (float) (d1 * Math.sin( convBuf2[i] ));
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (float) Math.exp( convBuf1[ i ]);
							}
						}
						break;

					case OP_NOT:										// ================ NOT ================
						for( i = 0; i < len; i++ ) {
							lo = ~((long) (convBuf1[i] * 2147483647.0));
							reOutBuf[ ch ][ i ] += wetGain * (float) ((lo & 0xFFFFFFFFL) / 2147483647.0);
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								lo = ~((long) (convBuf2[i] * 2147483647.0));
								imOutBuf[ ch ][ i ] += wetGain * (float) ((lo & 0xFFFFFFFFL) / 2147483647.0);
							}
						}
						break;
					}
				} // for outChan
				progOff += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- write output chunk ----
				if( reFloatF != null ) {
					for( ch = 0; ch < outChanNum; ch++ ) {
						reFloatF[ ch ].writeFloats( reOutBuf[ ch ], 0, len );
						if( pr.bool[ PR_HASIMOUTPUT ]) {
							imFloatF[ ch ].writeFloats( imOutBuf[ ch ], 0, len );
						}
					}
				} else {
					reOutF.writeFrames( reOutBuf, 0, len );
					if( pr.bool[ PR_HASIMOUTPUT ]) {
						imOutF.writeFrames( imOutBuf, 0, len );
					}
				}
				// check max amp
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = reOutBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
					if( pr.bool[ PR_HASIMOUTPUT ]) {
						convBuf1 = imOutBuf[ ch ];
						for( i = 0; i < len; i++ ) {
							f1 = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
				}

				progOff		  += len;
				framesWritten += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

			} // while not framesWritten
			
		// ----==================== normalize output ====================----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				peakGain = new Param( maxAmp, Param.ABS_AMP );
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
				f1		 = pr.bool[ PR_HASIMOUTPUT ] ? ((1.0f + getProgression()) / 2) : 1.0f;
				normalizeAudioFile( reFloatF, reOutF, reOutBuf, gain, f1 );
				if( pr.bool[ PR_HASIMOUTPUT ]) {
					normalizeAudioFile( imFloatF, imOutF, imOutBuf, gain, 1.0f );
				}
				maxAmp	*= gain;

				for( ch = 0; ch < outChanNum; ch++ ) {
					reFloatF[ ch ].cleanUp();
					reFloatF[ ch ]		= null;
					reTempFile[ ch ].delete();
					reTempFile[ ch ]	= null;
					if( pr.bool[ PR_HASIMOUTPUT ]) {
						imFloatF[ ch ].cleanUp();
						imFloatF[ ch ]		= null;
						imTempFile[ ch ].delete();
						imTempFile[ ch ]	= null;
					}
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			reOutF.close();
			reOutF		= null;
			reOutStream	= null;
			if( imOutF != null ) {
				imOutF.close();
				imOutF		= null;
				imOutStream	= null;
			}
			reInF.close();
			reInF		= null;
			reInStream	= null;
			if( pr.bool[ PR_HASIMINPUT ]) {
				imInF.close();
				imInF		= null;
				imInStream	= null;
			}
			reOutBuf	= null;
			imOutBuf	= null;
			reInBuf		= null;
			imInBuf		= null;

			// inform about clipping/ low level
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			reOutBuf	= null;
			imOutBuf	= null;
			reInBuf		= null;
			imInBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		convBuf1	= null;
		convBuf2	= null;

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
		if( reFloatF != null ) {
			for( ch = 0; ch < reFloatF.length; ch++ ) {
				if( reFloatF[ ch ] != null ) {
					reFloatF[ ch ].cleanUp();
					reFloatF[ ch ] = null;
				}
				if( reTempFile[ ch ] != null ) {
					reTempFile[ ch ].delete();
					reTempFile[ ch ] = null;
				}
			}
		}
		if( imFloatF != null ) {
			for( ch = 0; ch < imFloatF.length; ch++ ) {
				if( imFloatF[ ch ] != null ) {
					imFloatF[ ch ].cleanUp();
					imFloatF[ ch ] = null;
				}
				if( imTempFile[ ch ] != null ) {
					imTempFile[ ch ].delete();
					imTempFile[ ch ] = null;
				}
			}
		}
	} // process()

// -------- private Methoden --------

	/*
	 *	Neues Inputfile setzen
	 */
	private void setInput( String fname )
	{
		AudioFile		f		= null;
		AudioFileDescr	stream	= null;

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

		} catch( IOException e1 ) {}
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
// class UnaryOpDlg
