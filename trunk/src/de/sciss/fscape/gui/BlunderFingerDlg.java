/*
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
 *		20-Dec-08	created
 */

package de.sciss.fscape.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.spect.Wavelet;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Processing module for approaching a file (fit input)
 *	throw evolution using a genetic algorithm.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 21-Dec-08
 */
public class BlunderFingerDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INFITFILE			= 0;		// pr.text
	private static final int PR_INPOPFILE			= 1;
	private static final int PR_OUTPUTFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_CHROMOLEN			= 3;
	private static final int PR_CHANFIT				= 4;
	private static final int PR_DOMAIN				= 5;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_CROSSPOINTS			= 1;
	private static final int PR_POPULATION			= 2;
	private static final int PR_ITERATIONS			= 3;
	private static final int PR_MUTAPROB			= 4;
	private static final int PR_MUTAAMOUNT			= 5;
	private static final int PR_ELITISM				= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InFitFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_PATTERNFILE		= "InPopFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_CROSSPOINTS		= "CrossPoints";
	private static final String PRN_POPULATION		= "Population";
	private static final String PRN_ITERATIONS		= "Iterations";
	private static final String PRN_CHROMOLEN		= "ChromoLen";
	private static final String PRN_CHANFIT			= "ChanFit";
	private static final String PRN_DOMAIN			= "Domain";
	private static final String PRN_MUTAPROB		= "MutaProb";
	private static final String PRN_MUTAAMOUNT		= "MutaAmount";
	private static final String PRN_ELITISM			= "Elitism";
	
	private static final int	CHANFIT_WORST		= 0;
	private static final int	CHANFIT_BEST		= 1;
	private static final int	CHANFIT_MEAN		= 2;

	private static final int	DOMAIN_TIME			= 0;
	private static final int	DOMAIN_WAVELET		= 1;

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_PATTERNFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, 4, CHANFIT_WORST, DOMAIN_TIME };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_CHROMOLEN, PRN_CHANFIT, PRN_DOMAIN };
	private static final Param	prPara[]		= { null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_CROSSPOINTS, PRN_POPULATION, PRN_ITERATIONS, PRN_MUTAPROB, PRN_MUTAAMOUNT };
	private static final boolean prBool[]		= { true };
	private static final String	prBoolName[]	= { PRN_ELITISM };

	private static final int GG_INFITFILE		= GG_OFF_PATHFIELD	+ PR_INFITFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_INPOPFILE		= GG_OFF_PATHFIELD	+ PR_INPOPFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_CHROMOLEN		= GG_OFF_CHOICE		+ PR_CHROMOLEN;
	private static final int GG_CHANFIT			= GG_OFF_CHOICE		+ PR_CHANFIT;
	private static final int GG_DOMAIN			= GG_OFF_CHOICE		+ PR_DOMAIN;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_CROSSPOINTS		= GG_OFF_PARAMFIELD	+ PR_CROSSPOINTS;
	private static final int GG_POPULATION		= GG_OFF_PARAMFIELD	+ PR_POPULATION;
	private static final int GG_ITERATIONS		= GG_OFF_PARAMFIELD	+ PR_ITERATIONS;
	private static final int GG_MUTAPROB		= GG_OFF_PARAMFIELD	+ PR_MUTAPROB;
	private static final int GG_MUTAAMOUNT		= GG_OFF_PARAMFIELD	+ PR_MUTAAMOUNT;
	private static final int GG_ELITISM			= GG_OFF_CHECKBOX	+ PR_ELITISM;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "Inputs must share\nsame # of channels!";

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public BlunderFingerDlg()
	{
		super( "BlunderFinger" );
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
			static_pr.para[ PR_GAIN ]			= new Param(    0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_CROSSPOINTS ]	= new Param(    1.0, Param.NONE );
			static_pr.para[ PR_POPULATION ]		= new Param(   32.0, Param.NONE );
			static_pr.para[ PR_ITERATIONS ]		= new Param(  300.0, Param.NONE );
			static_pr.para[ PR_MUTAPROB ]		= new Param(   33.3, Param.RELPERCENT );
			static_pr.para[ PR_MUTAAMOUNT ]		= new Param(    0.1, Param.FACTOR_AMP );
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

		PathField			ggInFitFile, ggOutputFile, ggInPopFile;
		PathField[]			ggInputs;
		JComboBox			ggChromoLen, ggChanFit, ggDomain;
		JCheckBox			ggElitism;
		Component[]			ggGain;
		ParamField			ggCrossPoints, ggPopulation, ggIterations, ggMutaProb, ggMutaAmount;
		ParamSpace[]		spcMutaAmt;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_CHROMOLEN:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
					reflectPropertyChanges();
					break;
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		
		gui.addLabel( new JLabel( "Fuck the genfood mafia", JLabel.RIGHT ));
		
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInFitFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input fit file" );
		ggInFitFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Fit input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInFitFile, GG_INFITFILE, null );

		ggInPopFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input population file" );
		ggInPopFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Population input", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInPopFile, GG_INPOPFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 2 ];
		ggInputs[ 0 ]	= ggInFitFile;
		ggInputs[ 1 ]	= ggInPopFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$B0Gen$B1$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Evolved output", JLabel.RIGHT ));
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

	// -------- Plot Settings --------
	gui.addLabel( new GroupLabel( "Evolution settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggChromoLen		= new JComboBox();
		for( int i = 2; i <= 16384; i <<= 1 ) {
		ggChromoLen.addItem( String.valueOf( i ));
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Chromosome length", JLabel.RIGHT ));
		con.weightx		= 0.2;
		gui.addChoice( ggChromoLen, GG_CHROMOLEN, il );
	
		ggCrossPoints	= new ParamField( new ParamSpace[] {
			new ParamSpace( 1, 1000000, 1, Param.NONE )});
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossing points", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggCrossPoints, GG_CROSSPOINTS, null );

		ggIterations	= new ParamField( new ParamSpace[] {
			new ParamSpace( 1, 1000000, 1, Param.NONE )});
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Iterations", JLabel.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggIterations, GG_ITERATIONS, null );

		ggMutaProb		= new ParamField( Constants.spaces[ Constants.unsignedModSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mutation probability", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMutaProb, GG_MUTAPROB, null );

		ggPopulation	= new ParamField( new ParamSpace[] {
			new ParamSpace( 2, 65536, 2, Param.NONE )});
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Population", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggPopulation, GG_POPULATION, null );

		spcMutaAmt		= new ParamSpace[ 2 ];
		spcMutaAmt[0]	= Constants.spaces[ Constants.ratioAmpSpace ];
		spcMutaAmt[1]	= new ParamSpace( Constants.minDecibel, 0.0, 0.01, Param.DECIBEL_AMP );

		ggMutaAmount	= new ParamField( spcMutaAmt );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Mutation amount", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMutaAmount, GG_MUTAAMOUNT, null );

		ggElitism		= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Elitism", JLabel.RIGHT ));
		con.weightx		= 0.2;
		gui.addCheckbox( ggElitism, GG_ELITISM, il );
		
		ggChanFit		= new JComboBox();
		ggChanFit.addItem( "Worst" );
		ggChanFit.addItem( "Best" );
		ggChanFit.addItem( "Mean" );
		con.weightx		= 0.1;
//		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Multichannel fitness", JLabel.RIGHT ));
		con.weightx		= 0.2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggChanFit, GG_CHANFIT, il );

		ggDomain		= new JComboBox();
		ggDomain.addItem( "Time" );
		ggDomain.addItem( "Wavelet" );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Domain", JLabel.RIGHT ));
		con.weightx		= 0.2;
		gui.addChoice( ggDomain, GG_DOMAIN, il );
		
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
		long					progOff;
		final long				progLen;
		
		AudioFile				inFitF			= null;
		AudioFile				outF			= null;
		AudioFile				inPopF			= null;
		final AudioFile			tmpF;
		final AudioFileDescr	inFitDescr;
		final AudioFileDescr	outStream;
		final AudioFileDescr	inPopDescr;
		final int				inChanNum, outChanNum, inPopChanNum;
		final long				inFitLength, inPopLength;

		long					fitFramesRead;

		final Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float					gain			= 1.0f;								 		// gain abs amp
		float					maxAmp			= 0.0f;

		final PathField			ggOutput;

		final Random			rnd				= new Random();
		final int				population, populationM1, iterations, chromoLen, inPopBufSize;
		final int 				chromoLenM1, multiChanType, numCrossPoints;
		final float[][]			inFitBuf, inPopBuf;
		double[]				fitness, newFitness, tempFit;
		final List				crossings;
		final float[]			weights;
		final int[]				crossPoints;
		final boolean			elitism;
		final float				mutaProb, mutaAmtD;
		float[][][]				tempPop, popBuf, newPopBuf;
		float[][]				outBuf;
		final float[]			inOverlap, popOverlap;
		final float[][]			outOverlap;	// includes DC-block filter coeffs
		float					muta;
		int						chunkLen, chunkLen2, inPopBufOff, newPopCount;
		int						parentIdx1, parentIdx2;
		long					popFramesRead;
		final boolean			wavelet;
		final boolean			diff	= true;	// XXX
		final float[][]			waveletCoeffs;
		float					f1, f2, f3, f4;
		float[]					convBuf1;

topLevel: try {

		// ---- open input, output; init ----
			// input
			inFitF			= AudioFile.openAsRead( new File( pr.text[ PR_INFITFILE ]));
			inFitDescr		= inFitF.getDescr();
			inChanNum		= inFitDescr.channels;
			inFitLength		= (int) inFitDescr.length;
			// this helps to prevent errors from empty files!
			if( (inFitLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ptrn input
			inPopF			= AudioFile.openAsRead( new File( pr.text[ PR_INPOPFILE ]));
			inPopDescr		= inPopF.getDescr();
			inPopChanNum	= inPopDescr.channels;
			inPopLength		= inPopDescr.length;
			// this helps to prevent errors from empty files!
			if( (inPopLength < 1) || (inPopChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			if( inChanNum != inPopChanNum ) {
				throw new IOException( ERR_CHANNELS );
			}

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inFitDescr );
			ggOutput.fillStream( outStream );
			outChanNum	= inChanNum;
			outStream.channels = outChanNum;
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ---- further inits ----
			
			chromoLen			= 2 << pr.intg[ PR_CHROMOLEN ];
			chromoLenM1			= chromoLen - 1;
			
			inFitBuf			= new float[ inChanNum ][ Math.max( 8192, chromoLen )];
			inOverlap			= new float[ inChanNum ];
			popOverlap			= new float[ inChanNum ];
			outOverlap			= new float[ inChanNum ][ 3 ];
			population			= (int) pr.para[ PR_POPULATION ].val;
			populationM1		= population - 1;
			inPopBufSize		= chromoLen + populationM1;
			inPopBuf			= new float[ inChanNum ][ inPopBufSize ];
			inPopBufOff			= 0;
			popBuf				= new float[ population ][ inChanNum ][ chromoLen ];
			newPopBuf			= new float[ population ][ inChanNum ][ chromoLen ];
			fitness				= new double[ population ];
			newFitness			= new double[ population ];
			mutaProb			= (float) (pr.para[ PR_MUTAPROB ].val / 100);
			multiChanType		= pr.intg[ PR_CHANFIT ];
			elitism				= pr.bool[ PR_ELITISM ];
			iterations			= (int) pr.para[ PR_ITERATIONS ].val;
			numCrossPoints		= (int) pr.para[ PR_CROSSPOINTS ].val;
			mutaAmtD			= (float) ((Param.transform( pr.para[ PR_MUTAAMOUNT ], Param.ABS_AMP, ampRef, null )).val * 2);
// System.out.println( "mutaAmtD = " + mutaAmtD );
			weights				= new float[ population ];
			final int div		= ((population + 1) * population) / 2;
			double sum			= 0.0;
			double weight;
			for( int i = 0; i < population; i++ ) {
				weight			= (double) (population - i) / div;
				weights[ i ]	= (float) (weight + sum);
				sum			   += weight;
			}
//			assert( Math.abs( sum - 1.0 ) < 1.0e-6 ) : sum;
			assert( Math.abs( weights[ population - 1 ] - 1.0 ) < 1.0e-6 ) : weights[ population - 1 ];
			weights[ populationM1 ] = 1.0f;
			crossings			= new ArrayList( chromoLen - 2 );
			crossPoints			= new int[ numCrossPoints + 1 ];
			crossPoints[ numCrossPoints ] = chromoLen;
			
			wavelet				= pr.intg[ PR_DOMAIN ] == DOMAIN_WAVELET;
			if( wavelet ) {
				waveletCoeffs = Wavelet.getCoeffs( Wavelet.COEFFS_DAUB4 );
			} else {
				waveletCoeffs = null;
			}

			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				tmpF	= null;
			} else {
				tmpF	= createTempFile( outStream );
			}

			progLen				= (inFitLength << 2) + (tmpF == null ? 0 : inFitLength);
			progOff				= 0;

//			framesWritten		= 0;
			popFramesRead		= 0;

		// ----==================== processing loop ====================----
		
			for( fitFramesRead = 0; threadRunning && (fitFramesRead < inFitLength); ) {

				chunkLen	= (int) Math.min( chromoLen, inFitLength - fitFramesRead );
				inFitF.readFrames( inFitBuf, 0, chunkLen );
				if( chunkLen < chromoLen ) {
					Util.clear( inFitBuf, chunkLen, chromoLen - chunkLen );
				}
				if( diff ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inFitBuf[ ch ];
						f2		 = inOverlap[ ch ];
						for( int i = 0; i < chunkLen; i++ ) {
							f1 = convBuf1[ i ];
							convBuf1[ i ] -= f2;
							f2 = f1;
						}
						inOverlap[ ch ] = f2;
					}
				}
				
				if( wavelet ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						Wavelet.fwdTransform( inFitBuf[ ch ], chromoLen, waveletCoeffs );
					}
				}

// XXX
//popFramesRead = Math.min( inPopLength, fitFramesRead );
//inPopF.seekFrame( popFramesRead );
				chunkLen2	= (int) Math.min( inPopBufSize - inPopBufOff, inPopLength - popFramesRead );
				Util.copy( inPopBuf, chromoLen, inPopBuf, 0, populationM1 );
				inPopF.readFrames( inPopBuf, inPopBufOff, chunkLen2 );
				if( chunkLen2 + inPopBufOff < inPopBufSize ) {
					Util.clear( inPopBuf, chunkLen2 + inPopBufOff, inPopBufSize - (chunkLen2 + inPopBufOff) );
				}
				if( diff ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inPopBuf[ ch ];
						f2		 = popOverlap[ ch ];
						for( int i = inPopBufOff, j = i + chunkLen2; i < j; i++ ) {
							f1 = convBuf1[ i ];
							convBuf1[ i ] -= f2;
							f2 = f1;
						}
						popOverlap[ ch ] = f2;
					}
				}
				
				for( int i = 0; i < population; i++ ) {
					Util.copy( inPopBuf, i, popBuf[ i ], 0, chromoLen );
					if( wavelet ) {
						for( int ch = 0; ch < inChanNum; ch++ ) {
							Wavelet.fwdTransform( popBuf[ i ][ ch ], chromoLen, waveletCoeffs );
						}
					}
					fitness[ i ] = calcFitness( popBuf[ i ], inFitBuf, multiChanType );
				}
				
				Util.sort( fitness, popBuf, 0, population );
				
				for( int it = 0; it < iterations; it++ ) {
					newPopCount = 0;
					if( elitism ) {
						for( int i = 0; i < 2; i++, newPopCount++ ) {
							Util.copy( popBuf[ newPopCount ], 0, newPopBuf[ newPopCount ], 0, chromoLen );
							newFitness[ newPopCount ] = fitness[ newPopCount ];
						}
					}
				
					for( ; newPopCount < population; newPopCount += 2 ) {

						// parent selection
						parentIdx1 = wchoose( weights, rnd );
						parentIdx2 = wchoose( weights, rnd );
						if( parentIdx2 == parentIdx1 ) { // no hermaphrodites
							parentIdx2 = (parentIdx1 == populationM1) ? parentIdx1 - 1 : parentIdx1 + 1;
						}
						
						// selecting crossings
						crossings.clear();
						for( int i = 1; i < chromoLenM1; i++ ) {
							crossings.add( new Integer( i ));
						}
						for( int i = 0; i < numCrossPoints; i++ ) {
							crossPoints[ i ] = ((Integer) crossings.remove(
							    rnd.nextInt( crossings.size() ))).intValue();
						}
						Arrays.sort( crossPoints, 0, numCrossPoints );

						// breeding
						for( int i = 0, lastPoint = 0, tempIdx; i <= numCrossPoints; i++ ) {
							Util.copy( popBuf[ parentIdx1 ], lastPoint, newPopBuf[ newPopCount ], lastPoint, crossPoints[ i ] - lastPoint );
							Util.copy( popBuf[ parentIdx2 ], lastPoint, newPopBuf[ newPopCount + 1 ], lastPoint, crossPoints[ i ] - lastPoint );
							tempIdx		= parentIdx1;
							parentIdx1	= parentIdx2;
							parentIdx2	= tempIdx;
							lastPoint	= crossPoints[ i ];
						}
						
						// mutation
						if( mutaProb > 0f ) {
							for( int ch = 0; ch < inChanNum; ch++ ) {
								for( int child = 0; child < 2; child++ ) {
									for( int i = 0; i < chromoLen; i++ ) {
										if( rnd.nextFloat() < mutaProb ) {
											muta = (rnd.nextFloat() - 0.5f) * mutaAmtD;
											newPopBuf[ newPopCount + child ][ ch ][ i ] += muta;
										}
									}
								}
							}
						}
						
						for( int child = 0; child < 2; child++ ) {
							newFitness[ newPopCount + child ] =
								calcFitness( newPopBuf[ newPopCount + child ], inFitBuf, multiChanType );
						}
					}
					
					// new pop becomes current pop
					tempPop		= popBuf;
					popBuf		= newPopBuf;
					newPopBuf	= tempPop;
					
					tempFit		= fitness;
					fitness		= newFitness;
					newFitness	= tempFit;

					Util.sort( fitness, popBuf, 0, population );
				}
				
				// best fit is written out
				outBuf = popBuf[ 0 ];
				if( wavelet ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						Wavelet.invTransform( outBuf[ ch ], chromoLen, waveletCoeffs );
					}
				}
				if( diff ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						f2		 = outOverlap[ ch ][ 0 ];
						f3		 = outOverlap[ ch ][ 1 ];
						f4		 = outOverlap[ ch ][ 2 ];
						for( int i = 0; i < chunkLen; i++ ) {
							f2 += convBuf1[ i ];
//							convBuf1[ i ] = f2;
							convBuf1[ i ] = f2 - f3 + 0.99f * f4;
							f3 = f2;
							f4 = convBuf1[ i ];
						}
						outOverlap[ ch ][ 0 ] = f2;
						outOverlap[ ch ][ 1 ] = f3;
						outOverlap[ ch ][ 2 ] = f4;
					}
				}

				maxAmp = Math.max( maxAmp, Util.maxAbs( outBuf, 0, chunkLen ));
				if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
					Util.mult( outBuf, 0, chunkLen, gain );
				}
				if( tmpF != null ) {
					tmpF.writeFrames( outBuf, 0, chunkLen );
				} else {
					outF.writeFrames( outBuf, 0, chunkLen );
				}
				fitFramesRead	+= chunkLen;
				popFramesRead	+= chunkLen2;
				inPopBufOff		 = populationM1;
				progOff			+= chunkLen << 2;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
	// .... check running ....
			if( !threadRunning ) break topLevel;
			
			inPopF.close();
			inPopF		= null;
			inFitF.close();
			inFitF		= null;

			// adjust gain
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( tmpF, outF, inFitBuf, gain, 1.0f );
				deleteTempFile( tmpF );
			}

		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;

			// inform about clipping/ low level
			maxAmp		*= gain;
			handleClipping( maxAmp );

		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( inFitF != null ) inFitF.cleanUp();
		if( outF != null ) outF.cleanUp();
		if( inPopF != null ) inPopF.cleanUp();
	} // process()

// -------- private Methoden --------

	private static double calcFitness( float[][] pop, float[][] fit, int multiChanType )
	{
		double	fitness = 0f, d1;
//		double	d2;
		float	f1;
		float[] popC, fitC;
		
		for( int ch = 0; ch < pop.length; ch++ ) {
			popC	= pop[ ch ];
			fitC	= fit[ ch ];
			d1		= 0.0;
			for( int i = 0; i < popC.length; i++ ) {
				f1	 = popC[ i ] - fitC[ i ];
//				d2	 = popC[ i ] - fitC[ i ];
//				d1	+= d2 * d2;
				d1	+= f1 * f1;
//				d1	 = Math.max( d1, d2 * d2 );
//				d1	+= d2 * d2 * d2 * d2;
			}
			if( ch == 0 ) {
				fitness = d1;
			} else {
				switch( multiChanType ) {
				case CHANFIT_WORST:
					fitness = Math.max( fitness, d1 );
					break;
				case CHANFIT_BEST:
					fitness = Math.min( fitness, d1 );
					break;
				case CHANFIT_MEAN:
					fitness += d1;
					break;
				default:
					throw new IllegalArgumentException( String.valueOf( multiChanType ));
				}
			}
		}
		
		return fitness;
	}

	private static int wchoose( float[] weights, Random rnd )
	{
		final int idx = Arrays.binarySearch( weights, rnd.nextFloat() );
		return( idx < 0 ? -(idx + 1) : idx ); 
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		final JComboBox ggChromoLen		= (JComboBox) gui.getItemObj( GG_CHROMOLEN );
		final ParamField ggCrossPoints	= (ParamField) gui.getItemObj( GG_CROSSPOINTS );
		if( (ggChromoLen != null) && (ggCrossPoints != null) ) {
			final int chromoLenM1 = (2 << ggChromoLen.getSelectedIndex()) - 1;
			ggCrossPoints.setSpaces( new ParamSpace[] {
				new ParamSpace( 1, chromoLenM1, 1, Param.NONE )
			});
			if( ggCrossPoints.getParam().val > chromoLenM1 ) {
				ggCrossPoints.setParam( new Param( chromoLenM1, Param.NONE ));
			}
		}
	}
}
// class BlunderFingerDlg
