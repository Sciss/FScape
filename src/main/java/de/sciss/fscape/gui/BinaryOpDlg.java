/*
 *  BinaryOpDlg.java
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

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Processing module for algebraic
 *	combination of two sound files on
 *	a sample-by-sample basis.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 12-May-08
 */
public class BinaryOpDlg
extends DocumentFrame
{
// -------- public Variablen --------

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_REINPUTFILE1	= 0;		// pr.text
//	private static final int PR_REINPUTFILE2	= 1;
	private static final int PR_IMINPUTFILE1	= 2;
	private static final int PR_IMINPUTFILE2	= 3;
	private static final int PR_REOUTPUTFILE	= 4;
	private static final int PR_IMOUTPUTFILE	= 5;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_OPERATOR		= 2;
	private static final int PR_GAINTYPE		= 3;
//	private static final int PR_INGAINTYPE1		= 4;
//	private static final int PR_INGAINTYPE2		= 5;
	private static final int PR_GAIN			= 0;		// pr.para	
	private static final int PR_INPUTGAIN1		= 1;
	private static final int PR_INPUTGAIN2		= 2;
	private static final int PR_OFFSET1			= 3;
	private static final int PR_OFFSET2			= 4;
	private static final int PR_LENGTH1			= 5;
	private static final int PR_LENGTH2			= 6;
	private static final int PR_DRYMIX			= 7;
	private static final int PR_WETMIX			= 8;
	private static final int PR_HASIMINPUT1		= 0;		// pr.bool
	private static final int PR_HASIMINPUT2		= 1;
	private static final int PR_HASIMOUTPUT		= 2;
	private static final int PR_RECTIFY1		= 3;
//	private static final int PR_RECTIFY2		= 4;
	private static final int PR_INVERT1			= 5;
//	private static final int PR_INVERT2			= 6;
	private static final int PR_DRYINVERT		= 7;

	private static final int OP_ADD			= 0;
	private static final int OP_MULT		= 1;
	private static final int OP_DIV			= 2;
	private static final int OP_MOD			= 3;
	private static final int OP_POW			= 4;
	private static final int OP_AND			= 5;
	private static final int OP_OR			= 6;
	private static final int OP_XOR			= 7;
	private static final int OP_SIGN		= 8;
	private static final int OP_AMP			= 9;
	private static final int OP_MIN1		= 10;		// a < b ? a : b
	private static final int OP_MAX1		= 11;
	private static final int OP_MIN2		= 12;		// |a| < |b| ? a : b
	private static final int OP_MAX2		= 13;
	private static final int OP_MIN3		= 14;		// min2( Re a, Re b ) + i min2( Im a, Im b )
	private static final int OP_MAX3		= 15;
	private static final int OP_MIN4		= 16;		// min( a, b proj. auf a )
	private static final int OP_MAX4		= 17;
	private static final int OP_GATE		= 18;
	private static final int OP_ARCTAN		= 19;

	private static final String OPNAMES[]		= { "Add", "Multiply", "Divide", "Modulo", "Power", "AND", "OR", "XOR",
													"Apply sign (phase)", "Apply amp", "Min(a,b)", "Max(a,b)", "Min(|a|,|b|)",
													"Max(|a|,|b|)", "Min(Re)+iMin(Im)", "Max(Re)+iMax(Im)", "Min(a,b->a)",
													"Max(a,b->a)", "Gate", "ArcTangens" };

	private static final String PRN_REINPUTFILE1	= "ReInFile1";
	private static final String PRN_REINPUTFILE2	= "ReInFile2";
	private static final String PRN_IMINPUTFILE1	= "ImInFile1";
	private static final String PRN_IMINPUTFILE2	= "ImInFile2";
	private static final String PRN_REOUTPUTFILE	= "ReOutFile";
	private static final String PRN_IMOUTPUTFILE	= "ImOutFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_OPERATOR		= "Operator";
	private static final String PRN_INGAINTYPE1		= "InGainType1";
	private static final String PRN_INGAINTYPE2		= "InGainType2";
	private static final String PRN_INPUTGAIN1		= "InGain1";
	private static final String PRN_INPUTGAIN2		= "InGain2";
	private static final String PRN_OFFSET1			= "Offset1";
	private static final String PRN_OFFSET2			= "Offset2";
	private static final String PRN_LENGTH1			= "Length1";
	private static final String PRN_LENGTH2			= "Length2";
	private static final String PRN_DRYMIX			= "DryMix";
	private static final String PRN_WETMIX			= "WetMix";
	private static final String PRN_HASIMINPUT1		= "HasImInput1";
	private static final String PRN_HASIMINPUT2		= "HasImInput2";
	private static final String PRN_HASIMOUTPUT		= "HasImOutput";
	private static final String PRN_RECTIFY1		= "Rectify1";
	private static final String PRN_RECTIFY2		= "Rectify2";
	private static final String PRN_INVERT1			= "Invert1";
	private static final String PRN_INVERT2			= "Invert2";
	private static final String PRN_DRYINVERT		= "DryInvert";

	private static final String	prText[]		= { "", "", "", "", "", "" };
	private static final String	prTextName[]	= { PRN_REINPUTFILE1, PRN_REINPUTFILE2, PRN_IMINPUTFILE1,
													PRN_IMINPUTFILE2, PRN_REOUTPUTFILE, PRN_IMOUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, OP_ADD, GAIN_UNITY, GAIN_ABSOLUTE, GAIN_ABSOLUTE };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OPERATOR, PRN_GAINTYPE,
													PRN_INGAINTYPE1, PRN_INGAINTYPE2 };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_INPUTGAIN1, PRN_INPUTGAIN2, PRN_OFFSET1,
													PRN_OFFSET2, PRN_LENGTH1, PRN_LENGTH2, PRN_DRYMIX, PRN_WETMIX };
	private static final boolean	prBool[]	= { false, false, false, false, false, false, false, false };
	private static final String	prBoolName[]	= { PRN_HASIMINPUT1, PRN_HASIMINPUT2, PRN_HASIMOUTPUT,
													PRN_RECTIFY1, PRN_RECTIFY2, PRN_INVERT1, PRN_INVERT2, PRN_DRYINVERT };

	private static final int GG_REINPUTFILE1	= GG_OFF_PATHFIELD	+ PR_REINPUTFILE1;
//	private static final int GG_REINPUTFILE2	= GG_OFF_PATHFIELD	+ PR_REINPUTFILE2;
	private static final int GG_IMINPUTFILE1	= GG_OFF_PATHFIELD	+ PR_IMINPUTFILE1;
	private static final int GG_IMINPUTFILE2	= GG_OFF_PATHFIELD	+ PR_IMINPUTFILE2;
	private static final int GG_REOUTPUTFILE	= GG_OFF_PATHFIELD	+ PR_REOUTPUTFILE;
	private static final int GG_IMOUTPUTFILE	= GG_OFF_PATHFIELD	+ PR_IMOUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OPERATOR		= GG_OFF_CHOICE		+ PR_OPERATOR;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
//	private static final int GG_INGAINTYPE1		= GG_OFF_CHOICE		+ PR_INGAINTYPE1;
//	private static final int GG_INGAINTYPE2		= GG_OFF_CHOICE		+ PR_INGAINTYPE2;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_INPUTGAIN1		= GG_OFF_PARAMFIELD	+ PR_INPUTGAIN1;
//	private static final int GG_INPUTGAIN2		= GG_OFF_PARAMFIELD	+ PR_INPUTGAIN2;
	private static final int GG_OFFSET1			= GG_OFF_PARAMFIELD	+ PR_OFFSET1;
//	private static final int GG_OFFSET2			= GG_OFF_PARAMFIELD	+ PR_OFFSET2;
	private static final int GG_LENGTH1			= GG_OFF_PARAMFIELD	+ PR_LENGTH1;
//	private static final int GG_LENGTH2			= GG_OFF_PARAMFIELD	+ PR_LENGTH2;
	private static final int GG_DRYMIX			= GG_OFF_PARAMFIELD	+ PR_DRYMIX;
	private static final int GG_WETMIX			= GG_OFF_PARAMFIELD	+ PR_WETMIX;
	private static final int GG_HASIMINPUT1		= GG_OFF_CHECKBOX	+ PR_HASIMINPUT1;
	private static final int GG_HASIMINPUT2		= GG_OFF_CHECKBOX	+ PR_HASIMINPUT2;
	private static final int GG_HASIMOUTPUT		= GG_OFF_CHECKBOX	+ PR_HASIMOUTPUT;
	private static final int GG_RECTIFY1		= GG_OFF_CHECKBOX	+ PR_RECTIFY1;
//	private static final int GG_RECTIFY2		= GG_OFF_CHECKBOX	+ PR_RECTIFY2;
	private static final int GG_INVERT1			= GG_OFF_CHECKBOX	+ PR_INVERT1;
//	private static final int GG_INVERT2			= GG_OFF_CHECKBOX	+ PR_INVERT2;
	private static final int GG_DRYINVERT		= GG_OFF_CHECKBOX	+ PR_DRYINVERT;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public BinaryOpDlg()
	{
		super( "Binary Operator" );
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
			static_pr.para[ PR_INPUTGAIN1 ]	= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_INPUTGAIN2 ]	= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_OFFSET1 ]	= new Param(   0.0, Param.ABS_MS );
			static_pr.para[ PR_OFFSET2 ]	= new Param(   0.0, Param.ABS_MS );
			static_pr.para[ PR_LENGTH1 ]	= new Param( 100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_LENGTH2 ]	= new Param( 100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_DRYMIX ]		= new Param(   0.0, Param.FACTOR_AMP );
			static_pr.para[ PR_WETMIX ]		= new Param( 100.0, Param.FACTOR_AMP );
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
		ParamField			ggInputGain, ggOffset, ggLength, ggDryMix, ggWetMix;
		JComboBox			ggOperator;
		JCheckBox			ggHasImInput, ggHasImOutput, ggRectify, ggInvert, ggDryInvert;
		PathField[]			ggParent1, ggParent2;
		Component[]			ggGain;
		ParamSpace[]		spcOffset, spcLength;
		int					i, j;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );
		ggParent1		= new PathField[ 2 ];

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_HASIMINPUT1:
				case GG_HASIMINPUT2:
				case GG_HASIMOUTPUT:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};
		
	// -------- Input-Gadgets --------
		spcOffset		= new ParamSpace[ 3 ];
		spcOffset[0]	= new ParamSpace( -36000000.0, 36000000.0, 0.1,   Param.ABS_MS );	// allow negative offset
		spcOffset[1]	= new ParamSpace(   -240000.0,   240000.0, 0.001, Param.ABS_BEATS );
		spcOffset[2]	= new ParamSpace(	   -100.0,      100.0, 0.01,  Param.FACTOR_TIME );
//		spcOffset[0]	= Constants.spaces[ Constants.offsetMsSpace ];
//		spcOffset[1]	= Constants.spaces[ Constants.offsetBeatsSpace ];
//		spcOffset[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength		= new ParamSpace[ 4 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength[3]	= Constants.spaces[ Constants.factorTimeSpace ];

		for( i = 0; i < 2; i++ ) {
			con.fill		= GridBagConstraints.BOTH;
			con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new GroupLabel( "Operand " + (i+1), GroupLabel.ORIENT_HORIZONTAL,
									  GroupLabel.BRACE_NONE ));
			con.fill		= GridBagConstraints.HORIZONTAL;

			ggReInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
											 "Select real part of input" );
			ggReInputFile.handleTypes( GenericFile.TYPES_SOUND );
			con.gridwidth	= 1;
			con.weightx		= 0.1;
			gui.addLabel( new JLabel( "Input [Real]", SwingConstants.RIGHT ));
			con.gridwidth	= 2;
			con.weightx		= 3.0;
			gui.addPathField( ggReInputFile, GG_REINPUTFILE1 + i, null );

			ggOffset		= new ParamField( spcOffset );
			con.weightx		= 0.1;
			con.gridwidth	= 1;
			gui.addLabel( new JLabel( "Offset", SwingConstants.RIGHT ));
			con.weightx		= 0.4;
			con.gridwidth	= GridBagConstraints.REMAINDER;
			gui.addParamField( ggOffset, GG_OFFSET1 + i, null );
	
			ggImInputFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
											 "Select imaginary part of input" );
			ggImInputFile.handleTypes( GenericFile.TYPES_SOUND );
			ggHasImInput	= new JCheckBox( "Input [Imaginary]" );
			con.gridwidth	= 1;
			con.weightx		= 0.1;
			j				= con.anchor;
			con.anchor		= GridBagConstraints.EAST;
			gui.addCheckbox( ggHasImInput, GG_HASIMINPUT1 + i, il );
			con.anchor		= j;
			con.weightx		= 3.0;
			con.gridwidth	= 2;
			gui.addPathField( ggImInputFile, GG_IMINPUTFILE1 + i, null );

			ggLength		= new ParamField( spcLength );
			con.weightx		= 0.1;
			con.gridwidth	= 1;
			gui.addLabel( new JLabel( "Length", SwingConstants.RIGHT ));
			con.weightx		= 0.4;
			con.gridwidth	= GridBagConstraints.REMAINDER;
			gui.addParamField( ggLength, GG_LENGTH1 + i, null );

			ggParent1[ i ]	= ggReInputFile;
			ggParent2		= new PathField[ 1 ];
			ggParent2[ 0 ]	= ggReInputFile;
			ggImInputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );

			ggInputGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
			con.gridwidth	= 1;
			con.weightx		= 0.1;
			gui.addLabel( new JLabel( "Drive", SwingConstants.RIGHT ));
			con.weightx		= 0.4;
			gui.addParamField( ggInputGain, GG_INPUTGAIN1 + i, null );

			con.weightx		= 0.1;
			con.gridwidth	= 2;
			gui.addLabel( new JLabel( "" ));
			ggRectify		= new JCheckBox( "Rectify" );
			con.gridwidth	= 1;
			con.weightx		= 0.4;
			gui.addCheckbox( ggRectify, GG_RECTIFY1 + i, il );
			ggInvert		= new JCheckBox( "Invert" );
			con.gridwidth	= GridBagConstraints.REMAINDER;
			gui.addCheckbox( ggInvert, GG_INVERT1 + i, il );
		}
	
	// -------- Output-Gadgets --------
	gui.addLabel( new GroupLabel( "Output", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggReOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output for real part" );
		ggReOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "File [Real]", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggReOutputFile, GG_REOUTPUTFILE, null );
		gui.registerGadget( ggReOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggReOutputFile.getResGadget(), GG_OUTPUTRES );

		ggImOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE, "Select output for imaginary part" );
//		ggImOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggHasImOutput	= new JCheckBox( "File [Imaginary]" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		j				= con.anchor;
		con.anchor		= GridBagConstraints.EAST;
		gui.addCheckbox( ggHasImOutput, GG_HASIMOUTPUT, il );
		con.anchor		= j;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggImOutputFile, GG_IMOUTPUTFILE, null );

		ggParent2		= new PathField[ 1 ];
		ggParent2[ 0 ]	= ggReOutputFile;
		ggReOutputFile.deriveFrom( ggParent1, "$D0$B0Op$B1$E" );
		ggImOutputFile.deriveFrom( ggParent2, "$D0$F0i$X0" );
		
		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Operation", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOperator		= new JComboBox();
		for( i = 0; i < OPNAMES.length; i++ ) {
			ggOperator.addItem( OPNAMES[ i ]);
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Operator", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggOperator, GG_OPERATOR, il );

		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "" ));
		ggDryMix		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		gui.addLabel( new JLabel( "Dry mix", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggDryMix, GG_DRYMIX, null );

		ggDryInvert		= new JCheckBox( "Invert" );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggDryInvert, GG_DRYINVERT, il );

		con.gridwidth	= 3;
		gui.addLabel( new JLabel( "" ));
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
		
	/**
	 *	Translation durchfuehren
	 */
	public void process()
	{
		int					i, j, k;
		int					ch, op, len;
		float				f1, f2;
		double				d1, d2, d3, d4;
		long				progOff, progLen;
		
		// io
		AudioFile[]			reInF			= new AudioFile[ 2 ];
		AudioFile[]			imInF			= new AudioFile[ 2 ];
		AudioFile			reOutF			= null;
		AudioFile			imOutF			= null;
		AudioFileDescr[]		reInStream		= new AudioFileDescr[ 2 ];
		AudioFileDescr[]		imInStream		= new AudioFileDescr[ 2 ];
		AudioFileDescr			reOutStream		= null;
		AudioFileDescr			imOutStream		= null;
		FloatFile			reFloatF[]		= null;
		FloatFile			imFloatF[]		= null;
		File				reTempFile[]	= null;
		File				imTempFile[]	= null;
		int					outChanNum;

		float[][][]			reInBuf			= new float[2][][];				// [op][ch][i]
		float[][][]			imInBuf			= new float[2][][];				// [op][ch][i]
		float[][]			reOutBuf		= null;							// [ch][i]
		float[][]			imOutBuf		= null;							// [ch][i]
		float[]				convBuf1, convBuf2, convBuf3, convBuf4;
		boolean				complex;

		PathField			ggOutput;

		// Synthesize
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float				gain;												 		// gain abs amp
		float				dryGain, wetGain;
		float[]				inGain			= new float[ 2 ];
		float				maxAmp			= 0.0f;
		Param				peakGain;

		int[]				inLength		= new int[ 2 ];
		int[]				pre				= new int[ 2 ];
		int[]				post			= new int[ 2 ];
		int[]				length			= new int[ 2 ];
		int					framesWritten, outLength;

topLevel: try {

			for( op = 0; op < 2; op++ ) {
				reInF[op]	= null;
				imInF[op]	= null;
			}

			complex = pr.bool[ PR_HASIMINPUT1 ] || pr.bool[ PR_HASIMINPUT2 ] || pr.bool[ PR_HASIMOUTPUT ];

		// ---- open input ----

			for( op = 0; op < 2; op++ ) {
				reInF[op]			= AudioFile.openAsRead( new File( pr.text[ PR_REINPUTFILE1 + op ]));
				reInStream[op]		= reInF[op].getDescr();
				inLength[op]		= (int) reInStream[op].length;
				reInBuf[op]			= new float[ reInStream[op].channels ][ 8192 ];
				imInBuf[op]			= new float[ reInStream[op].channels ][ 8192 ];

				if( pr.bool[ PR_HASIMINPUT1 + op ]) {
					imInF[op]		= AudioFile.openAsRead( new File( pr.text[ PR_IMINPUTFILE1 + op ]));
					imInStream[op]	= imInF[op].getDescr();
					if( imInStream[op].channels != reInStream[op].channels ) throw new IOException( ERR_COMPLEX );
					inLength[op]	= (int) Math.min( inLength[op], imInStream[op].length );
				}

				final Param lenRef = new Param(
				   AudioFileDescr.samplesToMillis( reInStream[op], inLength[op] ), Param.ABS_MS );
				j				= (int) (AudioFileDescr.millisToSamples( reInStream[op],
										 (Param.transform( pr.para[ PR_OFFSET1 + op], Param.ABS_MS,
										                   lenRef, null )).val ) + 0.5);
				length[op]		= (int) (AudioFileDescr.millisToSamples( reInStream[op],
										 (Param.transform( pr.para[ PR_LENGTH1 + op], Param.ABS_MS,
										  lenRef,
										  null )).val ) + 0.5);

				if( j >= 0 ) {
					i			= Math.min( j, inLength[op] );
					reInF[op].seekFrame( i );
					if( pr.bool[ PR_HASIMINPUT1 + op ]) {
						imInF[op].seekFrame( i );
					}
					inLength[op]-= i;
					pre[op]		= 0;
				} else {
					i			= Math.min( -j, length[op] );
					pre[op]		= i;
				}
				inLength[op]	= Math.min( inLength[op], length[op] - pre[op] );
				post[op]		= length[op] - pre[op] - inLength[op];

			// .... check running ....
				if( !threadRunning ) break topLevel;
			}

			// zero pad operands so that their op-lengths are equal
			i = length[0] - length[1];
			if( i > 0 ) {
				post[1]		+= i;
				length[1]	+= i;
			} else if( i < 0 ) {
				post[0]		-= i;
				length[0]	-= i;
			}
			
// for( op = 0; op < 2; op++ ) {
// 	System.out.println( op +": pre "+pre[op]+" / len "+inLength[op]+" / post "+post[op] );
// }
// System.out.println( "tot "+length[0]);

			outLength	= length[0];
			outChanNum	= Math.max( reInStream[0].channels, reInStream[1].channels );

		// ---- open output ----

			ggOutput	= (PathField) gui.getItemObj( GG_REOUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			reOutStream	= new AudioFileDescr( reInStream[0] );
			ggOutput.fillStream( reOutStream );
			reOutStream.channels = outChanNum;
			// well, more sophisticated code would
			// move and truncate the markers...
			if( (pre[0] == 0) /* && (post[0] == 0) */ ) {
				reInF[0].readMarkers();
				reOutStream.setProperty( AudioFileDescr.KEY_MARKERS,
				    reInStream[0].getProperty( AudioFileDescr.KEY_MARKERS ));
			}
			reOutF		= AudioFile.openAsWrite( reOutStream );
			reOutBuf	= new float[ outChanNum ][ 8192 ];
			imOutBuf	= new float[ outChanNum ][ 8192 ];

			if( pr.bool[ PR_HASIMOUTPUT ]) {
				imOutStream	= new AudioFileDescr( reInStream[0] );
				ggOutput.fillStream( imOutStream );
				imOutStream.channels	= outChanNum;
				imOutStream.file		= new File( pr.text[ PR_IMOUTPUTFILE ]);
				imOutF		= AudioFile.openAsWrite( imOutStream );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Further inits ----

			progOff		= 0;			// read 2x, transform, write
			progLen		= (long) outLength << 2;

			wetGain		= (float) (Param.transform( pr.para[ PR_WETMIX ], Param.ABS_AMP, ampRef, null )).val;
			dryGain		= (float) (Param.transform( pr.para[ PR_DRYMIX ], Param.ABS_AMP, ampRef, null )).val;
			if( pr.bool[ PR_DRYINVERT ]) {
				dryGain = -dryGain;
			}
			for( op = 0; op < 2; op++ ) {
				inGain[op] = (float) (Param.transform( pr.para[ PR_INPUTGAIN1 + op ], Param.ABS_AMP, ampRef, null )).val;
				if( pr.bool[ PR_INVERT1 + op ]) {
					inGain[op] = -inGain[op];
				}
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
					reFloatF[ ch ]		= new FloatFile( reTempFile[ ch ], GenericFile.MODE_OUTPUT );
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
						imFloatF[ ch ]		= new FloatFile( imTempFile[ ch ], GenericFile.MODE_OUTPUT );
					}
				}
				progLen	   += (long) outLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				wetGain	   *= gain;
				dryGain	   *= gain;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		
		// ----==================== the real stuff ====================----

//			framesRead		= 0;
			framesWritten	= 0;
			
			while( threadRunning && (framesWritten < outLength) ) {
				// ---- choose chunk len ----
				len = Math.min( 8192, outLength - framesWritten );
				for( op = 0; op < 2; op++ ) {
					if( pre[op] > 0 ) {
						len	= Math.min( len, pre[op] );
					} else if( inLength[op] > 0 ) {
						len = Math.min( len, inLength[op] );
					} else {
						len = Math.min( len, post[op] );
					}
				}

				// ---- read input chunks ----
				for( op = 0; op < 2; op++ ) {
					if( pre[op] > 0 ) {
						Util.clear( reInBuf[op] );
						if( complex ) {
							Util.clear( imInBuf[op] );
						}
						pre[op]  -= len;
					} else if( inLength[op] > 0 ) {
						reInF[op].readFrames( reInBuf[op], 0, len );
						if( pr.bool[ PR_HASIMINPUT1 + op ]) {
							imInF[op].readFrames( imInBuf[op], 0, len );
						} else if( complex ) {
							Util.clear( imInBuf[op] );
						}
						inLength[op] -= len;
					} else {
						Util.clear( reInBuf[op] );
						if( complex ) {
							Util.clear( imInBuf[op] );
						}
						post[op] -= len;
					}
					progOff += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				// .... check running ....
					if( !threadRunning ) break topLevel;
				}

				// ---- save dry signal ----
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = reInBuf[ 0 ][ ch % reInStream[0].channels ];
					convBuf2 = reOutBuf[ ch ];
					for( i = 0; i < len; i++ ) {
						convBuf2[ i ] = convBuf1[ i ] * dryGain;
					}
					if( complex ) {
						convBuf1 = imInBuf[ 0 ][ ch % reInStream[0].channels ];
						convBuf2 = imOutBuf[ ch ];
						for( i = 0; i < len; i++ ) {
							convBuf2[ i ] = convBuf1[ i ] * dryGain;
						}
					}
				}

				// ---- rectify + apply input gain ----
				for( op = 0; op < 2; op++ ) {
					for( ch = 0; ch < reInStream[op].channels; ch++ ) {
						convBuf1 = reInBuf[op][ ch ];
						convBuf2 = imInBuf[op][ ch ];
						// ---- rectify ----
						if( pr.bool[ PR_RECTIFY1 + op ]) {
							if( complex ) {
								for( i = 0; i < len; i++ ) {
									d1				= convBuf1[ i ];
									d2				= convBuf2[ i ];
									convBuf1[ i ]	= (float) Math.sqrt( d1*d1 + d2*d2 );
									convBuf2[ i ]	= 0.0f;
								}
							} else {
								for( i = 0; i < len; i++ ) {
									convBuf1[ i ]	= Math.abs( convBuf1[ i ]);
								}
							}
						}
						// ---- apply input gain ----
						Util.mult( convBuf1, 0, len, inGain[op] );
						if( complex ) {
							Util.mult( convBuf2, 0, len, inGain[op] );
						}
					}
				}
				
				// ---- heart of the dragon ----
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = reInBuf[0][ ch % reInStream[0].channels ];
					convBuf2 = reInBuf[1][ ch % reInStream[1].channels ];
					convBuf3 = imInBuf[0][ ch % reInStream[0].channels ];
					convBuf4 = imInBuf[1][ ch % reInStream[1].channels ];

					switch( pr.intg[ PR_OPERATOR ]) {
					case OP_ADD:										// ================ Add ================
						for( i = 0; i < len; i++ ) {
							reOutBuf[ ch ][ i ] += wetGain * (convBuf1[ i ] + convBuf2[ i ]);
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								imOutBuf[ ch ][ i ] += wetGain * (convBuf3[ i ] + convBuf4[ i ]);
							}
						}
						break;
						
					case OP_MULT:										// ================ Multiply ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (convBuf1[i]*convBuf2[i] - convBuf3[i]*convBuf4[i]);
								imOutBuf[ ch ][ i ] += wetGain * (convBuf1[i]*convBuf4[i] + convBuf2[i]*convBuf3[i]);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (convBuf1[ i ] * convBuf2[ i ]);
							}
						}
						break;

					case OP_DIV:										// ================ Divide ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								if( Math.abs( convBuf2[i] ) >= Math.abs( convBuf4[i] )) {
									f1					 = convBuf4[i] / convBuf2[i];
									f2					 = convBuf2[i] + f1 * convBuf4[i];
									reOutBuf[ ch ][ i ] += wetGain * (convBuf1[i] + f1 * convBuf3[i]) / f2;
									imOutBuf[ ch ][ i ] += wetGain * (convBuf3[i] - f1 * convBuf1[i]) / f2;
								} else {
									f1					 = convBuf2[i] / convBuf4[i];
									f2					 = convBuf4[i] + f1 * convBuf2[i];
									reOutBuf[ ch ][ i ] += wetGain * (convBuf1[i] * f1 + convBuf3[i]) / f2;
									imOutBuf[ ch ][ i ] += wetGain * (convBuf3[i] * f1 - convBuf1[i]) / f2;
								}								
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (convBuf1[ i ] / convBuf2[ i ]);
							}
						}
						break;

					case OP_MOD:										// ================ Mod ================
						if( complex ) {
							throw new IOException( "Op. not yet implemented!" );	// XXX
						} else {
							for( i = 0; i < len; i++ ) {
								if( convBuf2[ i ] != 0.0f ) {
									reOutBuf[ ch ][ i ] += wetGain * (convBuf1[ i ] % convBuf2[ i ]);
								}
							}
						}

					case OP_POW:										// ================ Power ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								d1 = Math.sqrt( convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i] );
								if( d1 > 0.0 ) {
									// pow( r1, re2 ) * exp( -im2 * phi1 ) * exp( i( re2*phi1 + im2*ln r1 ))
									d2 = Math.atan2( convBuf3[i], convBuf1[i] );
									d3 = wetGain * Math.pow( d1, convBuf2[i] ) * Math.exp( -convBuf4[i] * d2 );	// new radius
									d4 = convBuf2[i] * d2 + convBuf4[i] * Math.log( d1 );						// new angle
									reOutBuf[ ch ][ i ] += (float) (d3 * Math.cos( d4 ));
									imOutBuf[ ch ][ i ] += (float) (d3 * Math.sin( d4 ));
								} else if( (convBuf2[i] == 0.0f) && (convBuf4[i] == 0.0f) ) {
									reOutBuf[ ch ][ i ] += wetGain;	// * pow( e, 0 ) = 1
								} // else nothing : pow( 0, x ) = 0
							}
						} else {
							for( i = 0; i < len; i++ ) {
								f1 = convBuf1[i];
								if( f1 != 0.0f ) {
									reOutBuf[ ch ][ i ] += (float) (wetGain * Math.pow( f1, convBuf2[i] ));
								} else if( convBuf2[i] == 0.0f ) {
									reOutBuf[ ch ][ i ] += wetGain;
								} // pow( 0, x ), x < 0 not defined ... ? interpolate?
							}
						}
						break;
						
					case OP_AND:										// ================ AND ================
						for( i = 0; i < len; i++ ) {
							j = (int) convBuf1[i] & (int) convBuf2[i];
							k = ((int) (((double) convBuf1[i] % 1.0) * 2147483647.0)) &
								((int) (((double) convBuf2[i] % 1.0) * 2147483647.0));
							reOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								j = (int) convBuf3[i] & (int) convBuf4[i];
								k = ((int) (((double) convBuf3[i] % 1.0) * 2147483647.0)) &
									((int) (((double) convBuf4[i] % 1.0) * 2147483647.0));
								imOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
							}
						}
						break;

					case OP_OR:											// ================ OR ================
						for( i = 0; i < len; i++ ) {
							j = (int) convBuf1[i] | (int) convBuf2[i];
							k = ((int) (((double) convBuf1[i] % 1.0) * 2147483647.0)) |
								((int) (((double) convBuf2[i] % 1.0) * 2147483647.0));
							reOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								j = (int) convBuf3[i] | (int) convBuf4[i];
								k = ((int) (((double) convBuf3[i] % 1.0) * 2147483647.0)) |
									((int) (((double) convBuf4[i] % 1.0) * 2147483647.0));
								imOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
							}
						}
						break;

					case OP_XOR:										// ================ XOR ================
						for( i = 0; i < len; i++ ) {
							j = (int) convBuf1[i] ^ (int) convBuf2[i];
							k = ((int) (((double) convBuf1[i] % 1.0) * 2147483647.0)) ^
								((int) (((double) convBuf2[i] % 1.0) * 2147483647.0));
							reOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
						}
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								j = (int) convBuf3[i] ^ (int) convBuf4[i];
								k = ((int) (((double) convBuf3[i] % 1.0) * 2147483647.0)) ^
									((int) (((double) convBuf4[i] % 1.0) * 2147483647.0));
								imOutBuf[ ch ][ i ] += (float) (wetGain * (j + (double) k / 2147483647.0));
							}
						}
						break;

					case OP_SIGN:										// ================ Apply sign (phase) ================
						if( complex ) {		// apply sign here means apply phase
							for( i = 0; i < len; i++ ) {
								d1					 = wetGain * Math.sqrt( convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i] );
								d2					 = Math.atan2( convBuf4[i], convBuf2[i] );
								reOutBuf[ ch ][ i ] += (float) (d1 * Math.cos( d2 ));
								imOutBuf[ ch ][ i ] += (float) (d1 * Math.sin( d2 ));
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += (convBuf2[i] >= 0 ? wetGain : -wetGain) * Math.abs( convBuf1[ i ]);
							}
						}
						break;

					case OP_AMP:										// ================ Apply amp ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {		// |op1|
								d1					 = wetGain * Math.sqrt( convBuf2[i]*convBuf2[i] + convBuf4[i]*convBuf4[i] );
								d2					 = Math.atan2( convBuf3[i], convBuf1[i] );
								reOutBuf[ ch ][ i ] += (float) (d1 * Math.cos( d2 ));
								imOutBuf[ ch ][ i ] += (float) (d1 * Math.sin( d2 ));
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += (convBuf1[i] >= 0 ? wetGain : -wetGain) * Math.abs( convBuf2[ i ]);
							}
						}
						break;

					case OP_MIN1:										// ================ Min(a,b) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * Math.min( convBuf1[ i ], convBuf2[ i ]);
								imOutBuf[ ch ][ i ] += wetGain * Math.min( convBuf3[ i ], convBuf4[ i ]);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * Math.min( convBuf1[ i ], convBuf2[ i ]);
							}
						}
						break;

					case OP_MAX1:										// ================ Max(a,b) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * Math.max( convBuf1[ i ], convBuf2[ i ]);
								imOutBuf[ ch ][ i ] += wetGain * Math.max( convBuf3[ i ], convBuf4[ i ]);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * Math.max( convBuf1[ i ], convBuf2[ i ]);
							}
						}
						break;
						
					case OP_MIN2:										// ================ Min(|a|,|b|) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								d1	 = convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i];
								d2	 = convBuf2[i]*convBuf2[i] + convBuf4[i]*convBuf4[i];
								if( d1 < d2 ) {
									reOutBuf[ ch ][ i ] += wetGain * convBuf1[ i ];
									imOutBuf[ ch ][ i ] += wetGain * convBuf3[ i ];
								} else {
									reOutBuf[ ch ][ i ] += wetGain * convBuf2[ i ];
									imOutBuf[ ch ][ i ] += wetGain * convBuf4[ i ];
								}
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) < Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
							}
						}
						break;

					case OP_MAX2:										// ================ Max(|a|,|b|) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								d1	 = convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i];
								d2	 = convBuf2[i]*convBuf2[i] + convBuf4[i]*convBuf4[i];
								if( d1 >= d2 ) {
									reOutBuf[ ch ][ i ] += wetGain * convBuf1[ i ];
									imOutBuf[ ch ][ i ] += wetGain * convBuf3[ i ];
								} else {
									reOutBuf[ ch ][ i ] += wetGain * convBuf2[ i ];
									imOutBuf[ ch ][ i ] += wetGain * convBuf4[ i ];
								}
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) >= Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
							}
						}
						break;

					case OP_MIN3:										// ================ Min(Re)+iMin(Im) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) < Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
								imOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf3[ i ]) < Math.abs( convBuf4[ i ]) ?
																  convBuf3[ i ] : convBuf4[ i ]);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) < Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
							}
						}
						break;

					case OP_MAX3:										// ================ Max(Re)+iMax(Im) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) >= Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
								imOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf3[ i ]) >= Math.abs( convBuf4[ i ]) ?
																  convBuf3[ i ] : convBuf4[ i ]);
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * (Math.abs( convBuf1[ i ]) >= Math.abs( convBuf2[ i ]) ?
																  convBuf1[ i ] : convBuf2[ i ]);
							}
						}
						break;
					
					case OP_MIN4:										// ================ Min(a,b->a) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {		// |op1|
								d1		 = Math.sqrt( convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i] );
								if( d1 > 0.0 ) {				// cos phi * |op2|
									d2	 = Math.abs( convBuf1[i]*convBuf2[i] + convBuf3[i]*convBuf4[i] ) / d1;
									if( d1 <= d2 ) {
										reOutBuf[ ch ][ i ]	+= wetGain * convBuf1[ i ];
										imOutBuf[ ch ][ i ]	+= wetGain * convBuf3[ i ];
									} else {
										d2 *= wetGain / d1;		// Strecken um |op2->op1| / |op1|
										reOutBuf[ ch ][ i ]	+= (float) (d2 * convBuf1[ i ]);
										imOutBuf[ ch ][ i ]	+= (float) (d2 * convBuf3[ i ]);
									}
								} // else |op1| == 0, don't have to add anything
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += (convBuf1[i] >= 0 ? wetGain : -wetGain) *
														Math.min( Math.abs( convBuf1[ i ]), Math.abs( convBuf2[ i ]));
							}
						}
						break;

					case OP_MAX4:										// ================ Max(a,b->a) ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {		// |op1|
								d1		 = Math.sqrt( convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i] );
								if( d1 > 0.0 ) {				// cos phi * |op2|
									d2	 = Math.abs( convBuf1[i]*convBuf2[i] + convBuf3[i]*convBuf4[i] ) / d1;
									if( d1 >= d2 ) {
										reOutBuf[ ch ][ i ]	+= wetGain * convBuf1[ i ];
										imOutBuf[ ch ][ i ]	+= wetGain * convBuf3[ i ];
									} else {
										d2 *= wetGain / d1;		// Strecken um |op2->op1| / |op1|
										reOutBuf[ ch ][ i ]	+= (float) (d2 * convBuf1[ i ]);
										imOutBuf[ ch ][ i ]	+= (float) (d2 * convBuf3[ i ]);
									}
								} else {	// phase undefined ;(
									reOutBuf[ ch ][ i ]	+= wetGain * convBuf2[ i ];
									imOutBuf[ ch ][ i ]	+= wetGain * convBuf4[ i ];
								}
							}
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += (convBuf1[i] >= 0 ? wetGain : -wetGain) *
														Math.max( Math.abs( convBuf1[ i ]), Math.abs( convBuf2[ i ]));
							}
						}
						break;

					case OP_GATE:										// ================ Gate ================
						if( complex ) {
							for( i = 0; i < len; i++ ) {		// |op1| resp. 2
								d1 = convBuf1[i]*convBuf1[i] + convBuf3[i]*convBuf3[i];
								d2 = convBuf2[i]*convBuf2[i] + convBuf4[i]*convBuf4[i];
								if( d1 > d2 ) {
									reOutBuf[ ch ][ i ]	+= wetGain * convBuf1[ i ];
									imOutBuf[ ch ][ i ]	+= wetGain * convBuf3[ i ];
								}
							}
						} else {
							for( i = 0; i < len; i++ ) {
								if( Math.abs( convBuf1[ i ]) > Math.abs( convBuf2[ i ]) ) {
									reOutBuf[ ch ][ i ] += wetGain * convBuf1[ i ];
								}
							}
						}
						break;

					case OP_ARCTAN:										// ================ ArcTangens ================
						if( complex ) {
							throw new IOException( "Op. not yet implemented!" );	// XXX
						} else {
							for( i = 0; i < len; i++ ) {
								reOutBuf[ ch ][ i ] += wetGain * Math.atan2( convBuf2[ i ], convBuf1[ i ]);
							}
						}
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
				peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
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
			for( op = 0; op < 2; op++ ) {
				reInF[op].close();
				reInF[op]		= null;
				reInStream[op]	= null;
				if( pr.bool[ PR_HASIMINPUT1 + op ]) {
					imInF[op].close();
					imInF[op]		= null;
					imInStream[op]	= null;
				}
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
			convBuf3	= null;
			convBuf4	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		convBuf1	= null;
		convBuf2	= null;
		convBuf3	= null;
		convBuf4	= null;

		for( op = 0; op < 2; op++ ) {
			if( reInF[op] != null ) {
				reInF[op].cleanUp();
				reInF[op] = null;
			}
			if( imInF[op] != null ) {
				imInF[op].cleanUp();
				imInF[op] = null;
			}
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

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_IMINPUTFILE1 );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMINPUT1 ]);
		}
		c = gui.getItemObj( GG_IMINPUTFILE2 );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMINPUT2 ]);
		}
		c = gui.getItemObj( GG_IMOUTPUTFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_HASIMOUTPUT ]);
		}
	}
}
// class BinaryOpDlg
