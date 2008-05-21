/*
 *  TransientExtrDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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
 *	Trying out to extract only the transient noise
 *	from a sound using wavelet decomposition, never worked.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class TransientExtrDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_MODFILE			= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_LPMODE			= 3;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_LPORDER1		= 1;
	private static final int PR_LPORDER2		= 2;
	private static final int PR_CMORDER			= 3;
	private static final int PR_LPADAPT1		= 4;
	private static final int PR_LPADAPT2		= 5;
	private static final int PR_CMADAPT			= 6;
	private static final int PR_CROSSSYNTH		= 0;		// pr.bool
	private static final int PR_CONSTANTMODULUS	= 1;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_MODFILE			= "ModFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_LPMODE			= "LPMode";
	private static final String PRN_LPORDER1		= "LPOrder1";
	private static final String PRN_LPORDER2		= "LPOrder2";
	private static final String PRN_CMORDER			= "CMOrder";
	private static final String PRN_LPADAPT1		= "LPAdapt1";
	private static final String PRN_LPADAPT2		= "LPAdapt2";
	private static final String PRN_CMADAPT			= "CMAdapt";
	private static final String PRN_CROSSSYNTH		= "CrossSynth";
	private static final String PRN_CONSTANTMODULUS	= "ConstMod";

	private static final int LPMODE_BYPASS		= 0;	// PR_LPMODE
	private static final int LPMODE_FILTER		= 1;
	private static final int LPMODE_RESIDUAL	= 2;

	private static final String[] LPMODE_NAMES	= { "Bypass", "Filter", "Residual" };

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_MODFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, LPMODE_RESIDUAL };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_LPMODE };
	private static final boolean	prBool[]	= { true, false };
	private static final String	prBoolName[]	= { PRN_CROSSSYNTH, PRN_CONSTANTMODULUS };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_LPORDER1, PRN_LPORDER2, PRN_CMORDER,
													PRN_LPADAPT1, PRN_LPADAPT2, PRN_CMADAPT };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_MODFILE			= GG_OFF_PATHFIELD	+ PR_MODFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_LPMODE			= GG_OFF_CHOICE		+ PR_LPMODE;
	private static final int GG_LPORDER1		= GG_OFF_PARAMFIELD	+ PR_LPORDER1;
	private static final int GG_LPORDER2		= GG_OFF_PARAMFIELD	+ PR_LPORDER2;
	private static final int GG_CMORDER			= GG_OFF_PARAMFIELD	+ PR_CMORDER;
	private static final int GG_LPADAPT1		= GG_OFF_PARAMFIELD	+ PR_LPADAPT1;
	private static final int GG_LPADAPT2		= GG_OFF_PARAMFIELD	+ PR_LPADAPT2;
	private static final int GG_CMADAPT			= GG_OFF_PARAMFIELD	+ PR_CMADAPT;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_CROSSSYNTH		= GG_OFF_CHECKBOX	+ PR_CROSSSYNTH;
	private static final int GG_CONSTANTMODULUS	= GG_OFF_CHECKBOX	+ PR_CONSTANTMODULUS;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public TransientExtrDlg()
	{
		super( "Adaptive LP Filter" );
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
			static_pr.para[ PR_GAIN ]				= new Param(  0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_LPORDER1 ]			= new Param( 18.0, Param.NONE );
			static_pr.para[ PR_LPORDER2 ]			= new Param( 12.0, Param.NONE );
			static_pr.para[ PR_CMORDER ]			= new Param( 24.0, Param.NONE );
			static_pr.para[ PR_LPADAPT1 ]			= new Param(  2.0, Param.FACTOR_AMP );
			static_pr.para[ PR_LPADAPT2 ]			= new Param(  0.5, Param.FACTOR_AMP );
			static_pr.para[ PR_CMADAPT ]			= new Param(  0.1, Param.FACTOR_AMP );
			static_pr.paraName	= prParaName;
			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile, ggModFile;
		JComboBox			ggLPMode;
		JCheckBox			ggCrossSynth, ggConstMod;
		ParamField			ggLPOrder1, ggLPOrder2, ggCMOrder, ggLPAdapt1, ggLPAdapt2, ggCMAdapt;
		PathField[]			ggInputs;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_LPMODE:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
					reflectPropertyChanges();
					break;
				case GG_CROSSSYNTH:
				case GG_CONSTANTMODULUS:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select (excitation) input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "(Excitation) input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggModFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select (filter) input file" );
		ggModFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filter input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggModFile, GG_MODFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
//		ggInputs[ 1 ]	= ggModFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0LP$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Filter Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggLPMode		= new JComboBox();
		for( int i = 0; i < LPMODE_NAMES.length; i++ ) {
			ggLPMode.addItem( LPMODE_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Source LP output", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggLPMode, GG_LPMODE, il );

		ggLPOrder1		= new ParamField( new ParamSpace( 1.0, 4096.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Source LP order", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLPOrder1, GG_LPORDER1, null );

		ggLPAdapt1		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Adaptiveness", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLPAdapt1, GG_LPADAPT1, null );

		ggCrossSynth	= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Cross synthesis", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addCheckbox( ggCrossSynth, GG_CROSSSYNTH, il );

		ggLPOrder2		= new ParamField( new ParamSpace( 1.0, 4096.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Target LP order", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLPOrder2, GG_LPORDER2, null );

		ggLPAdapt2		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Adaptiveness", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLPAdapt2, GG_LPADAPT2, null );

		ggConstMod		= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Constant Modulus Flt.", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addCheckbox( ggConstMod, GG_CONSTANTMODULUS, il );

		ggCMOrder		= new ParamField( new ParamSpace( 1.0, 4096.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "CM order", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCMOrder, GG_CMORDER, null );

		ggCMAdapt		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Adaptiveness", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggCMAdapt, GG_CMADAPT, null );

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
		int					i, j, k, m, n, ch, chunkLength;
		long				progOff, progLen;
		float				f1;
		double				d1, d2;
		
		// io
		AudioFile			inF				= null;
		AudioFile			modF			= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		modStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile[]			floatF			= null;
		File[]				tempFile		= null;
		float[][]			inBuf;
		float[][]			outBuf;
		float[][]			cmBuf			= null;
		float[][]			modBuf			= null;
		float[][]			lpBuf1			= null;
		float[][]			lpBuf2			= null;
		float[]				convBuf1, convBuf2, convBuf3, convBuf4;
		float[][]			lpCoeffs1		= null;
		float[][]			lpCoeffs2		= null;
		float[][]			cmCoeffs		= null;

		// Synthesize
		float				gain			= 1.0f;								// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		boolean				crossSynth		= pr.bool[ PR_CROSSSYNTH ];
		boolean				constMod		= pr.bool[ PR_CONSTANTMODULUS ];
		boolean				lpFilter		= pr.intg[ PR_LPMODE ] != LPMODE_BYPASS;
		
		// Smp Init
		int					inLength, inChanNum, modLength, modChanNum, outChanNum, outLength;
		int					framesRead, framesRead2, framesWritten;
		int					lpOrder1, lpOrder2, cmOrder, maxOrder;
		double				lpAdapt1, lpAdapt2, cmAdapt, cmDispersion;

		float				maxAmp			= 0.0f;

		PathField			ggOutput;

topLevel: try {

// System.out.println( "lp" + lpFilter+"; cross "+crossSynth+"; cm "+constMod );
		// ---- open input, output; init ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( inLength * inChanNum < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			progOff			= 0;
			progLen			= 0;

			if( crossSynth ) {
				// input
				modF			= AudioFile.openAsRead( new File( pr.text[ PR_MODFILE ]));
				modStream		= modF.getDescr();
				modChanNum		= modStream.channels;
				modLength		= (int) modStream.length;
				// this helps to prevent errors from empty files!
				if( modLength * modChanNum < 1 ) throw new EOFException( ERR_EMPTY );
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
				outLength		= Math.min( inLength, modLength );
				outChanNum		= Math.max( inChanNum, modChanNum );
				progLen		   += outLength;
				
			} else {
				modLength		= inLength;
				modChanNum		= inChanNum;
				outLength		= inLength;
				outChanNum		= inChanNum;
			}

			progLen	 += (long) outLength * 2;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			lpOrder1		= (int) (pr.para[ PR_LPORDER1 ].val + 0.5);
			lpOrder2		= (int) (pr.para[ PR_LPORDER2 ].val + 0.5);
			cmOrder			= (int) (pr.para[ PR_CMORDER  ].val + 0.5);
			lpAdapt1		= pr.para[ PR_LPADAPT1 ].val / 100;
			lpAdapt2		= pr.para[ PR_LPADAPT2 ].val / 100;
			cmAdapt			= pr.para[ PR_CMADAPT  ].val / 100;
			cmDispersion	= 1.0;		// user-adjustable? XXX

			maxOrder		= Math.max( constMod ? cmOrder : 0,
							  Math.max( crossSynth ? lpOrder2 : 0, lpFilter ? lpOrder1 : 0 ));

			inBuf	= new float[ inChanNum ][ 8192 + maxOrder ];
			Util.clear( inBuf );
			outBuf	= inBuf;

			// standard coeff init
			// the buffers are assigned to minimize arraycopying operations
			// (outBuf automatically points to the right array holding the ready samples for the output file)
			if( lpFilter ) {
				lpCoeffs1	= new float[ inChanNum ][ lpOrder1 ];
				Util.clear( lpCoeffs1 );
				for( ch = 0; ch < inChanNum; ch++ ) {
					lpCoeffs1[ ch ][ lpOrder1 >> 1 ]	= 1.0f;
				}
				lpBuf1	= new float[ inChanNum ][ 8192 + maxOrder ];
				Util.clear( lpBuf1 );
				outBuf	= lpBuf1;
			} else {
				lpBuf1	= inBuf;
			}
			if( crossSynth) {
				lpCoeffs2	= new float[ modChanNum ][ lpOrder2 ];
				Util.clear( lpCoeffs2 );
				for( ch = 0; ch < modChanNum; ch++ ) {
					lpCoeffs2[ ch ][ lpOrder2 >> 1 ]	= 1.0f;
				}
				modBuf	= new float[ modChanNum ][ 8192 + maxOrder ];
				Util.clear( modBuf );
				lpBuf2	= new float[ outChanNum ][ 8192 + maxOrder ];
				Util.clear( lpBuf2 );
				outBuf	= lpBuf2;
			}
			if( constMod ) {
				cmCoeffs	= new float[ outChanNum ][ cmOrder ];
				Util.clear( cmCoeffs );
				for( ch = 0; ch < outChanNum; ch++ ) {
					cmCoeffs[ ch ][ cmOrder >> 1 ]		= 1.0f;
				}
				cmBuf	= outBuf;
				outBuf	= new float[ outChanNum ][ 8192 + maxOrder ];
				Util.clear( outBuf );
			}

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
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], FloatFile.MODE_OUTPUT );
				}
				progLen	   += outLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== the ultraworld ====================----
		// based on formulae published in the article
		// "Blind Adapted, Pre-Whitened Constant Modulus Algorithm" by James P. LeBlanc and Inbar Fijalkow
		
			framesWritten	= 0;
			framesRead		= 0;	// re inF
			framesRead2		= 0;	// re modF

// System.out.println( "outBuf == inBuf ? "+(outBuf == inBuf));
			while( threadRunning && (framesWritten < outLength) ) {

				chunkLength	= Math.min( 8192, Math.min( inLength - framesRead, modLength - framesRead2 ));

				// ==================== read input chunk ====================
				inF.readFrames( inBuf, maxOrder, chunkLength );
				framesRead += chunkLength;
				progOff	   += chunkLength;
				
				if( crossSynth ) {
					modF.readFrames( modBuf, maxOrder, chunkLength );
					framesRead2 += chunkLength;
					progOff	    += chunkLength;
				}
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- Source LP filter ----------------------------------------------------------------------
				switch( pr.intg[ PR_LPMODE ]) {
				case LPMODE_FILTER:
					// d(n) = D'*X(n); D = [d0 d1 ... dN]'; X(n) = [x(n-1) x(n-2) ... x(n-N)]'
					// y(n) = d(n) (Mode=Filter) oder y(n) = x(n) - d(n) (Mode=Residual)
					// D(n+1) = D(n) - §*d(n)*X(n)
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = lpCoeffs1[ ch ];
						convBuf2 = inBuf[ ch ];
						convBuf3 = lpBuf1[ ch ];
						for( i = 0, j = maxOrder; i < chunkLength; i++, j++ ) {
							d1 = 0.0;
							d2 = 0.0;
							for( k = 0, m = j; k < lpOrder1; ) {			// calc d(n)
								f1	= convBuf2[ --m ];
								d1 += convBuf1[ k++ ] * f1;
								d2 += f1*f1;
							}
							convBuf3[ j ]	= (float) d1;					// y(n)
							d1				= convBuf2[ j ] - d1;			// error
							f1				= (float) (d1 * lpAdapt1);
							for( k = 0, m = j; k < lpOrder1; ) {			// update D
								convBuf1[ k++ ] -= f1 * convBuf2[ --m ];
							}
						}
					}

					// energy correction
//					adjustEnergy( lpCoeffs1 );

					// buffer rotation
					for( ch = 0; ch < inChanNum; ch++ ) {
						System.arraycopy( inBuf[ ch ], chunkLength, inBuf[ ch ], 0, maxOrder );
					}
					break;
					
				case LPMODE_RESIDUAL:
					// d(n) = D'*X(n); D = [d0 d1 ... dN]'; X(n) = [x(n-1) x(n-2) ... x(n-N)]'
					// y(n) = d(n) (Mode=Filter) oder y(n) = x(n) - d(n) (Mode=Residual)
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = lpCoeffs1[ ch ];
						convBuf2 = inBuf[ ch ];
						convBuf3 = lpBuf1[ ch ];
						for( i = 0, j = maxOrder; i < chunkLength; i++, j++ ) {
							d1 = 0.0;
							for( k = 0, m = j; k < lpOrder1; ) {			// calc d(n)
								d1 += convBuf1[ k++ ] * convBuf2[ --m ];
							}
							d1				= convBuf2[ j ] - d1;			// error
							convBuf3[ j ]	= (float) d1;					// y(n)
							f1				= (float) (d1 * lpAdapt1);
							for( k = 0, m = j; k < lpOrder1; ) {			// update D
								convBuf1[ k++ ] -= f1 * convBuf2[ --m ];
							}
						}
					}

					// buffer rotation
					for( ch = 0; ch < inChanNum; ch++ ) {
						System.arraycopy( inBuf[ ch ], chunkLength, inBuf[ ch ], 0, maxOrder );
					}
					break;

				default:	// LPMODE_BYPASS
					break;
				} // preliminary output now available in lpBuf1 (evtl. auch in outBuf)

				// ---- Target LP filter ----------------------------------------------------------------------
				if( crossSynth ) {
					// d(n) = D'*Xmod(n); D = [d0 d1 ... dN]'; X(n) = [x(n-1) x(n-2) ... x(n-N)]'
					// y(n) = D'*Xin(n)
					if( inChanNum <= modChanNum ) {
						for( ch = 0; ch < modChanNum; ch++ ) {
							convBuf1 = lpCoeffs2[ ch ];
							convBuf2 = modBuf[ ch ];
							convBuf3 = lpBuf1[ ch % inChanNum ];
							convBuf4 = lpBuf2[ ch ];
							for( i = 0, j = maxOrder; i < chunkLength; i++, j++ ) {
								d1 = 0.0;
								d2 = 0.0;
								for( k = 0, m = j; k < lpOrder2; ) {			// calc d(n)
									d1 += convBuf1[ k ]   * convBuf2[ --m ];
									d2 += convBuf1[ k++ ] * convBuf3[ m ];		// ...and y(n)
								}
								convBuf4[ j ]	= (float) d2;					// y(n)
								d1				= convBuf2[ j ] - d1;			// error
								f1				= (float) (d1 * lpAdapt2);
								for( k = 0, m = j; k < lpOrder2; ) {			// update D
									convBuf1[ k++ ] -= f1 * convBuf2[ --m ];
								}
							}
						}
					} else {
						for( ch = 0; ch < modChanNum; ch++ ) {
							convBuf1 = lpCoeffs2[ ch ];
							convBuf2 = modBuf[ ch ];
							for( i = 0, j = maxOrder; i < chunkLength; i++, j++ ) {
								d1 = 0.0;
								for( k = 0, m = j; k < lpOrder2; ) {			// calc d(n)
									d1 += convBuf1[ k ]   * convBuf2[ --m ];
								}
								for( n = ch; n < inChanNum; n += modChanNum ) {
									convBuf3	= lpBuf1[ n ];
									convBuf4	= lpBuf2[ n ];
									d2			= 0.0;
									for( k = 0, m = j; k < lpOrder2; ) {		// calc d(n)
										d2 += convBuf1[ k++ ] * convBuf3[ m ];	// ...and y(n)
									}
									convBuf4[ j ] = (float) d2;					// y(n)
								}
								d1 = convBuf2[ j ] - d1;						// error
								f1 = (float) (d1 * lpAdapt2);
								for( k = 0, m = j; k < lpOrder2; ) {			// update D
									convBuf1[ k++ ] -= f1 * convBuf2[ --m ];
								}
							}
						}
					}

					// buffer rotation
					for( ch = 0; ch < modChanNum; ch++ ) {
						System.arraycopy( modBuf[ ch ], chunkLength, modBuf[ ch ], 0, maxOrder );
					}
					for( ch = 0; ch < inChanNum; ch++ ) {
						System.arraycopy( lpBuf1[ ch ], chunkLength, lpBuf1[ ch ], 0, maxOrder );
					}
				} // preliminary output now available in lpBuf2 (evtl. auch in outBuf)

				// ---- CM filter ----------------------------------------------------------------------
				if( constMod ) {
					// y(n) = D'*X(n); D = [d0 d1 ... dN]'; X(n) = [x(n) x(n-1) ... x(n-N+1)]'
					// D(n+1) = D(n) - §*(y(n)*y(n) - mu)*y(n)*X(n)
					for( ch = 0; ch < outChanNum; ch++ ) {
						convBuf1 = cmCoeffs[ ch ];
						convBuf2 = cmBuf[ ch ];
						convBuf3 = outBuf[ ch ];
						for( i = 0, j = maxOrder; i < chunkLength; i++, j++ ) {
							d1 = 0.0;
							for( k = 0, m = j; k < cmOrder; ) {			// calc d(n)
								d1 += convBuf1[ k++ ] * convBuf2[ m-- ];
							}
							convBuf3[ j ]	= (float) d1;					// y(n)
							f1				= (float) (cmAdapt * (d1*d1 - cmDispersion) * d1);
							for( k = 0, m = j; k < cmOrder; ) {			// update D
								convBuf1[ k++ ] -= f1 * convBuf2[ m-- ];
							}
						}
					}
					
					// buffer rotation
					for( ch = 0; ch < outChanNum; ch++ ) {
						System.arraycopy( cmBuf[ ch ], chunkLength, cmBuf[ ch ], 0, maxOrder );
					}
				} // output now available outBuf

				// ---- Measure gain ----------------------------------------------------------------------
				for( ch = 0; ch < outChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					for( i = 0; i < chunkLength; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}
					
				// ---- write output or temp ----
				if( floatF != null ) {
					for( ch = 0; ch < outChanNum; ch++ ) {
						floatF[ ch ].writeFloats( outBuf[ ch ], maxOrder, chunkLength );
					}
				} else {
					// adjust gain
					for( ch = 0; ch < outChanNum; ch++ ) {
						Util.mult( outBuf[ ch ], maxOrder, chunkLength, gain );
					}
					outF.writeFrames( outBuf, maxOrder, chunkLength );
				}
				framesWritten += chunkLength;

			// .... progress ....
				progOff += chunkLength;
				setProgression( (float) progOff / (float) progLen );		
			} // while not framesWritten
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----
	
			inF.close();
			inF			= null;
			inStream	= null;
			if( modF != null ) {
				modF.close();
				modF	= null;
				modStream=null;
			}

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {

				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;

				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}

			outF.close();
			outF = null;

		// ---- Finish ----

			// inform about clipping/ low level
			maxAmp *= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			modStream	= null;
			inBuf		= null;
			outBuf		= null;
			modBuf		= null;
			lpBuf1		= null;
			lpBuf2		= null;
			cmBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			convBuf3	= null;
			convBuf4	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
		}
		if( outF != null ) {
			outF.cleanUp();
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------

	protected void adjustEnergy( float[][] coeffs )
	{
		int		chanNum		= coeffs.length;
		int		numCoeffs	= coeffs[0].length;
		float[]	convBuf1;
		int		ch, i;
		float	f1;
		
		for( ch = 0; ch < chanNum; ch++ ) {
			convBuf1 = coeffs[ ch ];
			for( i = 0; i < numCoeffs; i++ ) {
				f1 = Math.abs( convBuf1[ i ]);
				if( f1 > 1.0f ) {
					convBuf1[ i ] /= f1;
				}
			}
		}
//			for( i = 0, d1 = 0.0; i < numCoeffs; i++ ) {
//				d1 += convBuf1[ i ]*convBuf1[ i ];
//			}
//			if( d1 > 0.0 ) {
//				Util.mult( convBuf1, 0, numCoeffs, (float) (1.0 / Math.sqrt( d1 )));
//			}
//		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component	c;
		
		c = gui.getItemObj( GG_MODFILE );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_CROSSSYNTH ]);
		}
		c = gui.getItemObj( GG_LPORDER1 );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_LPMODE ] != LPMODE_BYPASS );
		}
		c = gui.getItemObj( GG_LPADAPT1 );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_LPMODE ] != LPMODE_BYPASS );
		}
		c = gui.getItemObj( GG_LPORDER2 );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_CROSSSYNTH ]);
		}
		c = gui.getItemObj( GG_LPADAPT2 );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_CROSSSYNTH ]);
		}
		c = gui.getItemObj( GG_CMORDER );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_CONSTANTMODULUS ]);
		}
		c = gui.getItemObj( GG_CMADAPT );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_CONSTANTMODULUS ]);
		}
	}
}
// class TransientExtrDlg
