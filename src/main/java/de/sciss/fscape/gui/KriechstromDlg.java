/*
 *  KriechstromDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
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
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *  Actually I have to puke when everyone feels
 *	granular synthesis is a great invention albeit
 *	it sucks in 99% of all cases, however this
 *	is a small imitation of the nice GRM freezer.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class KriechstromDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_FLTCOLOR		= 2;
	private static final int PR_GAINTYPE		= 3;
	private static final int PR_LENUPDATE		= 0;		// pr.bool
	private static final int PR_MINCHUNKNUM		= 0;		// pr.para
	private static final int PR_MAXCHUNKNUM		= 1;
	private static final int PR_MINCHUNKLEN		= 2;
	private static final int PR_MAXCHUNKLEN		= 3;
	private static final int PR_MINCHUNKREP		= 4;
	private static final int PR_MAXCHUNKREP		= 5;
	private static final int PR_CROSSFADE		= 6;
	private static final int PR_ENTRYPOINT		= 7;
	private static final int PR_GAIN			= 8;
	private static final int PR_FLTAMOUNT		= 9;
	private static final int PR_OUTLENGTH		= 10;
	private static final int PR_KRIECHENV		= 0;		// pr.envl

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_FLTCOLOR		= "FltColor";
	private static final String PRN_FLTAMOUNT		= "FltAmount";
	private static final String PRN_MINCHUNKNUM		= "MinChunkNum";
	private static final String PRN_MAXCHUNKNUM		= "MaxChunkNum";
	private static final String PRN_MINCHUNKLEN		= "MinChunkLen";
	private static final String PRN_MAXCHUNKLEN		= "MaxChunkLen";
	private static final String PRN_MINCHUNKREP		= "MinChunkRep";
	private static final String PRN_MAXCHUNKREP		= "MaxChunkRep";
	private static final String PRN_CROSSFADE		= "CrossFade";
	private static final String PRN_ENTRYPOINT		= "EntryPoint";
	private static final String PRN_KRIECHENV		= "KriechEnv";
	private static final String PRN_LENUPDATE		= "LenUpdate";
	private static final String PRN_OUTLENGTH		= "OutLength";

	private static final String[] FLTCOLOR_NAMES	= { "Dark", "Neutral", "Bright" };
	private static final int FLTCOLOR_DARK			= 0;
	private static final int FLTCOLOR_NEUTRAL		= 1;
	private static final int FLTCOLOR_BRIGHT		= 2;

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, FLTCOLOR_NEUTRAL, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_FLTCOLOR, PRN_GAINTYPE };
	private static final boolean	prBool[]	= { true };
	private static final String	prBoolName[]	= { PRN_LENUPDATE };
	private static final String	prParaName[]	= { PRN_MINCHUNKNUM, PRN_MAXCHUNKNUM, PRN_MINCHUNKLEN, PRN_MAXCHUNKLEN,
													PRN_MINCHUNKREP, PRN_MAXCHUNKREP, PRN_CROSSFADE, PRN_ENTRYPOINT,
													PRN_GAIN, PRN_FLTAMOUNT, PRN_OUTLENGTH };
	private static final Param	prPara[]		= new Param[ prParaName.length ];
	private static final Envelope	prEnvl[]	= { null };
	private static final String	prEnvlName[]	= { PRN_KRIECHENV };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_FLTCOLOR		= GG_OFF_CHOICE		+ PR_FLTCOLOR;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_LENUPDATE		= GG_OFF_CHECKBOX	+ PR_LENUPDATE;
	private static final int GG_MINCHUNKNUM		= GG_OFF_PARAMFIELD	+ PR_MINCHUNKNUM;
	private static final int GG_MAXCHUNKNUM		= GG_OFF_PARAMFIELD	+ PR_MAXCHUNKNUM;
	private static final int GG_MINCHUNKLEN		= GG_OFF_PARAMFIELD	+ PR_MINCHUNKLEN;
	private static final int GG_MAXCHUNKLEN		= GG_OFF_PARAMFIELD	+ PR_MAXCHUNKLEN;
	private static final int GG_MINCHUNKREP		= GG_OFF_PARAMFIELD	+ PR_MINCHUNKREP;
	private static final int GG_MAXCHUNKREP		= GG_OFF_PARAMFIELD	+ PR_MAXCHUNKREP;
	private static final int GG_CROSSFADE		= GG_OFF_PARAMFIELD	+ PR_CROSSFADE;
	private static final int GG_ENTRYPOINT		= GG_OFF_PARAMFIELD	+ PR_ENTRYPOINT;
	private static final int GG_FLTAMOUNT		= GG_OFF_PARAMFIELD	+ PR_FLTAMOUNT;
	private static final int GG_OUTLENGTH		= GG_OFF_PARAMFIELD	+ PR_OUTLENGTH;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_KRIECHENV		= GG_OFF_ENVICON	+ PR_KRIECHENV;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public KriechstromDlg()
	{
		super( "Kriechstrom" );
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
			static_pr.para[ PR_MINCHUNKNUM ]= new Param(    1.0, Param.NONE );
			static_pr.para[ PR_MAXCHUNKNUM ]= new Param(   10.0, Param.NONE );
			static_pr.para[ PR_MINCHUNKREP ]= new Param(    1.0, Param.NONE );
			static_pr.para[ PR_MAXCHUNKREP ]= new Param(    5.0, Param.NONE );
			static_pr.para[ PR_MINCHUNKLEN ]= new Param(   10.0, Param.ABS_MS );
			static_pr.para[ PR_MAXCHUNKLEN ]= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_CROSSFADE ]	= new Param(   20.0, Param.FACTOR_TIME );
			static_pr.para[ PR_ENTRYPOINT ]	= new Param(  500.0, Param.ABS_MS );
			static_pr.para[ PR_FLTAMOUNT ]	= new Param(   50.0, Param.FACTOR_AMP );
			static_pr.para[ PR_OUTLENGTH ]	= new Param(  100.0, Param.FACTOR_TIME );
			static_pr.paraName	= prParaName;
			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_KRIECHENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_UNSIGNED_TIME );
			static_pr.envlName	= prEnvlName;
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
		PathField[]			ggInputs;
		ParamField			ggMinChunkNum, ggMaxChunkNum, ggMinChunkLen, ggMaxChunkLen, ggMinChunkRep, ggMaxChunkRep;
		ParamField			ggCrossFade, ggEntryPoint, ggFltAmount, ggOutLength;
		JCheckBox			ggLenUpdate;
		ParamSpace[]		spcChunk, spcFade, spcOutLen;
		ParamSpace			spcNumber;
		Component[]			ggGain;
		EnvIcon				ggKriechEnv;
		JComboBox			ggFltColor;
		int					i;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE:
					setInput( ((PathField) e.getSource()).getPath().getPath() );
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
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Frz$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
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
	gui.addLabel( new GroupLabel( "Kriech Charakteristika", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcOutLen		= new ParamSpace[ 3 ];
		spcOutLen[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcOutLen[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcOutLen[2]	= new ParamSpace( Constants.spaces[ Constants.factorTimeSpace ]);
		spcChunk		= new ParamSpace[ 2 ];
		spcChunk[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcChunk[1]		= Constants.spaces[ Constants.absBeatsSpace ];
		spcNumber		= new ParamSpace( 1.0, 100000.0, 1.0, Param.NONE );
		spcFade			= new ParamSpace[ 3 ];
		spcFade[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcFade[1]		= Constants.spaces[ Constants.absBeatsSpace ];
		spcFade[2]		= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		spcFade[2].max	= 50.0;
		spcFade[2]		= new ParamSpace( spcFade[2].min, 50.0, spcFade[2].inc, spcFade[2].unit );

		ggOutLength		= new ParamField( spcOutLen );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Output length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= 4;
		gui.addParamField( ggOutLength, GG_OUTLENGTH, null );

		ggKriechEnv		= new EnvIcon( getComponent() );
		con.weightx		= 0.0;
		con.gridwidth	= 1;
		gui.addGadget( ggKriechEnv, GG_KRIECHENV );
		con.weightx		= 0.0;
		gui.addLabel( new JLabel() );

		ggEntryPoint	= new ParamField( spcChunk );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Max. entry offset", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggEntryPoint, GG_ENTRYPOINT, null );

		ggMinChunkNum	= new ParamField( spcNumber );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min/Max simult. chunks", SwingConstants.RIGHT ));
		con.weightx		= 0.0;
		gui.addParamField( ggMinChunkNum, GG_MINCHUNKNUM, null );
		ggMaxChunkNum	= new ParamField( spcNumber );
		con.weightx		= 0.0;
		gui.addLabel( new JLabel( "...", SwingConstants.LEFT ));
		con.weightx		= 0.4;
		con.gridwidth	= 4;
		gui.addParamField( ggMaxChunkNum, GG_MAXCHUNKNUM, null );

		ggCrossFade		= new ParamField( spcFade );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossfades", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggCrossFade, GG_CROSSFADE, null );

		ggMinChunkRep	= new ParamField( spcNumber );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min/Max chunks repeats", SwingConstants.RIGHT ));
		con.weightx		= 0.0;
		gui.addParamField( ggMinChunkRep, GG_MINCHUNKREP, null );
		ggMaxChunkRep	= new ParamField( spcNumber );
		con.weightx		= 0.0;
		gui.addLabel( new JLabel( "...", SwingConstants.LEFT ));
		con.weightx		= 0.4;
		con.gridwidth	= 4;
		gui.addParamField( ggMaxChunkRep, GG_MAXCHUNKREP, null );

		ggFltAmount		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Filter amount", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggFltAmount, GG_FLTAMOUNT, null );

		ggMinChunkLen	= new ParamField( spcChunk );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min. chunk length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= 4;
		gui.addParamField( ggMinChunkLen, GG_MINCHUNKLEN, null );

		ggFltColor		= new JComboBox();
		for( i = 0; i < FLTCOLOR_NAMES.length; i++ ) {
			ggFltColor.addItem( FLTCOLOR_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 3;
		gui.addLabel( new JLabel( "Filter colour", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggFltColor, GG_FLTCOLOR, null );

		ggMaxChunkLen	= new ParamField( spcChunk );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max. chunk length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= 4;
		gui.addParamField( ggMaxChunkLen, GG_MAXCHUNKLEN, null );
		ggLenUpdate		= new JCheckBox( "Instantaneous length update" );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggLenUpdate, GG_LENUPDATE, null );

		ggCrossFade.setReference( ggMinChunkLen );

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
		int					i, j, k, m, chunkLength;
		int					len, ch;
		long				progOff, progLen;
		double				d1, d2, d3, d4, d5, d6, d7;
		float				f1, f2, f3, f4, f5, fadeY1, fadeY2;
		boolean				b1;
		
		// io
		AudioFile			inF		= null;
		AudioFile			outF	= null;
		AudioFileDescr		inStream, outStream;
		
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;

		int					inChanNum;
		float[][]			inBuf, outBuf;
		float[]				convBuf1, convBuf2;

		int					inLength, outLength;
		int					framesWritten;
		
		SpectStream			mStream			= null;
		Modulator			mPosMod;
		Param				pInLength, pOutLength, pPos, pChunkLen;
		
//		Region				currentRegion, prevRegion, nextRegion;
		Vector				regionList;
		KriechChunk			chunk;
		
		float				chunkNumHist, chunkNumMean;
		int					chunkNumHistCnt;

		Random				rand			= new Random( System.currentTimeMillis() );
		int					chunkNumMin, chunkNumSpan, chunkLenMin, chunkLenSpan, chunkRepMin, chunkRepSpan;
		int					inOffMin, relOff;

		float				fltAmount;
		float[][]			filter			= null;

		PathField			ggOutput;

		float				gain			= 1.0f;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		float				maxAmp			= 0.0f;

topLevel: try {

		// ---- open input ----
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );

		// ---- open output ----
			ggOutput		= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream		= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF			= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- misc inits ----
			pInLength		= new Param( AudioFileDescr.samplesToMillis( inStream, inLength ), Param.ABS_MS );
			pOutLength		= Param.transform( pr.para[ PR_OUTLENGTH ], Param.ABS_MS, pInLength, null );
			outLength		= (int) (AudioFileDescr.millisToSamples( outStream, pOutLength.val ) + 0.5);
			
			mStream			= new SpectStream();
			mStream.setChannels( inChanNum );
			mStream.setBands( 0.0f, (float) inStream.rate / 2, 1, SpectStream.MODE_LIN );
			mStream.setRate( (float) inStream.rate, 1 );				// !!
			mStream.setEstimatedLength( outLength );
			mStream.getDescr();

			mPosMod			= new Modulator( new Param( 0.0, Param.ABS_MS ), pInLength, pr.envl[ PR_KRIECHENV ], mStream );
//			foo	= mPosMod.calc();
//			d1	= foo.val;
//				mStream.framesRead++;			// dirty! XXX

// System.out.println( "Total reads: "+mImpReads+"; morphs "+mMorphs );

			chunkNumMin		= (int) (pr.para[ PR_MINCHUNKNUM ].val + 0.5);
			chunkNumSpan	= (int) (pr.para[ PR_MAXCHUNKNUM ].val + 0.5) - chunkNumMin + 1;
			if( chunkNumSpan < 0 ) {
				chunkNumSpan = -chunkNumSpan;
				chunkNumMin	 = (int) (pr.para[ PR_MAXCHUNKNUM ].val + 0.5);
			}
			chunkRepMin		= (int) (pr.para[ PR_MINCHUNKREP ].val + 0.5);
			chunkRepSpan	= (int) (pr.para[ PR_MAXCHUNKREP ].val + 0.5) - chunkRepMin + 1;
			if( chunkRepSpan < 0 ) {
				chunkRepSpan = -chunkRepSpan;
				chunkRepMin	 = (int) (pr.para[ PR_MAXCHUNKREP ].val + 0.5);
			}
			chunkLenMin		= (int) (AudioFileDescr.millisToSamples( outStream,
										(Param.transform( pr.para[ PR_MINCHUNKLEN ], Param.ABS_MS, null, null )).val ) + 0.5);
			i				= (int) (AudioFileDescr.millisToSamples( outStream,
										(Param.transform( pr.para[ PR_MAXCHUNKLEN ], Param.ABS_MS, null, null )).val ) + 0.5);
			chunkLenSpan	= i - chunkLenMin + 1;
			if( chunkLenSpan < 0 ) {
				chunkLenSpan = -chunkLenSpan;
				chunkLenMin	 = i;
			}
			inOffMin		= Math.max( 0, inLength - chunkLenMin );
			
			fltAmount		= (float) Param.transform( pr.para[ PR_FLTAMOUNT ], Param.ABS_AMP, ampRef, null ).val;
			if( fltAmount > 0.0f ) {	// create filter table

				filter		= new float[ 512 ][ 3 ];
				d1			= 1.0;
				switch( pr.intg[ PR_FLTCOLOR ]) {
				case FLTCOLOR_DARK:		d1 = 2.0; break;
				case FLTCOLOR_NEUTRAL:	d1 = 0.8; break;
				case FLTCOLOR_BRIGHT:	d1 = 0.4; break;
				}

				for( i = 0; i < 512; i++ ) {	// 2nd order IIR filter, EDS script S. 101
					d2		= Math.exp( rand.nextDouble() * d1 ) * 20.0;

//					d2		= Constants.PI2 * d2 / inStream.smpRate;		// wc
//d2 = Math.PI / 2;
//					d3		= Math.log( rand.nextDouble() * 4.5 + 2.88 );
//d3 = 1.1;
//					d4		= d2 * d3;										// w2
//					d5		= d2 / d3;										// w1
//					d6		= Math.cos( d2 );									// alpha
//					d7		= -Math.tan( (d4 - d5 + Math.PI) / 2 ) * Math.tan( d2 / 2 );	// k
//					
//					filter[i][0] = (float) ((d7 - 1.0) / (d7 + 1.0));		// b0, a2
//					filter[i][1] = (float) (-2.0 * d6 * d7 / (d7 + 1.0));	// b1, a1

			//	For example a biquad IIR looks like that:
			// y[n] = x[n]*a0 + x[n-1]*a1 + x[n-2]*a2 + b1*y[n-1] + b2*y[n-2]
			// If you want a 2 pole bandpass you would setup a0,a1,a2,b1,b2 like this:
			// 
			// omega = 2.0*M_PI*center;
			// alpha = sin(omega)*sinh(log(2.0)/2.0*bandwidth*omega/sin(omega));
			// a0 = alpha;
			// a1 = 0.0;
			// a2 = -alpha;
			// b1 = 2.0 * cos(omega);
			// b2 = alpha - 1.0;
			// gain = 1.0 + alpha;
			// a0/=gain
			// a1/=gain 
			//
			// center is the normalized frequency of the bandpass:
			// center = center_frequency/sample_frequency
			// bandwidth is given in octaves between the -3dB attenuation levels of the passband

//					d2		= Constants.PI2 * d2 / inStream.smpRate;		// wc
//					d3		= rand.nextDouble() * 0.9 + 0.1;		// bandwidth
//					d4		= Math.sin( d2 ) * sinh( Constants.ln2 / 2.0 * d3 * d2 / Math.sin( d2 ));	// alpha
//					filter[i][0] = (float) (d4 / (1.0 + d4));		// b0
//					filter[i][1] = (float) (2.0 * Math.cos( d2 ));	// a1;
//					filter[i][2] = (float) (d4 - 1.0);				// a2

			// from Text:	The Equivalence of Various Methods of Computing
			//				Biquad Coefficients for Audio Parametric Equalizers
			
//					d2		= d2 / inStream.smpRate;					// Omega0
// fuer d3 = 0.3:
//d2 = 2.25;
//d2 = 0.11;
//d2 = Math.pow( Math.log( rand.nextDouble() * 1.7182818 + 1.0 ), 0.6 )  * 2.49 + 0.108;
//d2 = Math.pow( rand.nextDouble(), 0.4 )  * 2.45 + 0.108;
// fuer d3 = 0.67:
//d2 = 0.24 ... 2.3;
d2 = Math.pow( rand.nextDouble(), d1 )  * 2.06 + 0.24;

//					d3		= rand.nextDouble() * 0.9 + 0.1;			// bandwidth
d3 = 0.67;
//					d5		= 100.0;									// gain;
d5 = 1.0;
					d4		= Math.sin( d2 ) * sinh( Constants.ln2 / 2.0 * d3 * d2 / Math.sin( d2 ));	// gamma
					d6		= d4 * Math.sqrt( d5 );
//					d7		= 1.0 + d4 / Math.sqrt( d5 );				// a0
d7 = 1.0;
					filter[i][0] = (float) ((1.0 + d6) / d7);			// b0
					filter[i][1] = (float) (-Math.cos( d2 ) * 2.0 / d7);	// b1, a1
					filter[i][2] = (float) ((1.0 - d6) / d7);			// b2, a2
				}
			}

			progOff			= 0;
			progLen			= (long) outLength;

		// ---- misc inits ----

			regionList		= new Vector();		// Elemente = de.sciss.fscape.io.Region, chronologically added
			inBuf			= new float[ inChanNum ][ 8192 ];
			outBuf			= new float[ inChanNum ][ 8192 ];
			pChunkLen		= new Param( 0.0, Param.ABS_MS );

			chunk			= new KriechChunk( inChanNum );
			chunk.len		= 0;
			chunk.outOff	= 0;
			chunk.rep		= 0;
			regionList.addElement( chunk );
			framesWritten	= 0;
			chunkNumMean	= (chunkNumSpan >> 1) + chunkNumMin;
			chunkNumHist	= chunkNumMean;
			chunkNumHistCnt	= 1;


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
				progLen += outLength / chunkNumMean;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ================ PASS 1: Region detection ================

			while( (framesWritten < outLength) && threadRunning ) {

				chunkLength	= outLength - framesWritten;
				b1			= false;

				// calc next stop, remove obsolete chunks
				k			= regionList.size();
				for( i = 0; i < k; i++ ) {
					chunk	= (KriechChunk) regionList.elementAt( i );
					j		= chunk.len - framesWritten + chunk.outOff;
					if( j > 0 ) {
						chunkLength = Math.min( chunkLength, j );
						j -= chunk.fadeOut;
						if( (j >= 0) && (chunk.rep-- > 1) ) {		// repeat
							if( j == 0 ) {
								chunk			= new KriechChunk( chunk );
								chunk.outOff	= framesWritten;
								if( pr.bool[ PR_LENUPDATE ]) {
									chunk.len		= chunk.nextLen;
									chunkLength		= Math.min( chunkLength, chunk.len - (chunk.rep > 1 ? chunk.fadeOut : 0) );
									chunk.nextLen	= Math.min( inLength - chunk.inOff,
																(int) (rand.nextFloat() * chunkLenSpan) + chunkLenMin );
									pChunkLen.val	= AudioFileDescr.samplesToMillis( outStream, chunk.len );
									m				= (int) (AudioFileDescr.millisToSamples( outStream,
																(Param.transform( pr.para[ PR_CROSSFADE ], Param.ABS_MS, pChunkLen, null )).val ) + 0.5);
									chunk.fadeIn	= chunk.fadeOut;
									chunk.fadeOut	= Math.min( Math.min( chunk.nextLen >> 1, chunk.len - chunk.fadeIn ), m );
								}
								regionList.addElement( chunk );
// System.out.println(" new rep : "+chunk.rep );
							} else {
								chunkLength = Math.min( chunkLength, j );
							}
						}
					} else {
						regionList.removeElementAt( i );
						i--;
						k--;
						b1 = true;
					}
				}
			
				pPos				= mPosMod.calc();
				mStream.framesRead	= framesWritten;
				
				if( b1 ) { // chunks ceased to exist -> calc new chunk-num
// System.out.println( (chunkNumHistCnt*chunkNumMean)/chunkNumHist );
					i			= (int) (Math.min( 1.0f, rand.nextFloat() * (chunkNumHistCnt*chunkNumMean)/chunkNumHist) * chunkNumSpan) + chunkNumMin;
					j			= i - regionList.size();

// System.out.println( "written : "+framesWritten +" ; new chunks : "+j );
					
					for( ; j > 0; j-- ) {
						chunk			= new KriechChunk( inChanNum );
						d1				= (rand.nextDouble() * 2.0 - 1.0) * pr.para[ PR_ENTRYPOINT ].val + pPos.val;
						chunk.inOff		= Math.min( inOffMin, Math.max( 0, (int) (AudioFileDescr.millisToSamples( outStream, d1 ) + 0.5) ));
						chunk.rep		= (int) (rand.nextFloat() * chunkRepSpan) + chunkRepMin;
						chunk.len		= Math.min( inLength - chunk.inOff, (int) (rand.nextFloat() * chunkLenSpan) + chunkLenMin );
						pChunkLen.val	= AudioFileDescr.samplesToMillis( outStream, chunk.len );
						k				= (int) (AudioFileDescr.millisToSamples( outStream,
													(Param.transform( pr.para[ PR_CROSSFADE ], Param.ABS_MS, pChunkLen, null )).val ) + 0.5);
						chunk.fadeIn	= Math.min( chunk.len >> 1, k );
						if( pr.bool[ PR_LENUPDATE ]) {
							chunk.nextLen	= Math.min( inLength - chunk.inOff, (int) (rand.nextFloat() * chunkLenSpan) + chunkLenMin );
							chunk.fadeOut	= Math.min( Math.min( chunk.nextLen >> 1, chunk.len - chunk.fadeIn ), k );
						} else {
							chunk.fadeOut	= Math.min( chunk.len - chunk.fadeIn, k );
						}
						chunk.outOff	= framesWritten;
	
						if( rand.nextFloat() < fltAmount ) {
							chunk.flt	= (int) (rand.nextFloat() * 512);
						} else {
							chunk.flt	= -1;
						}
	
// System.out.println( "New region : "+chunk.inOff + "; len: "+chunk.len+"; rep: "+chunk.rep+"; fadeIn "+chunk.fadeIn + "; fadeOut "+chunk.fadeOut );
	
						regionList.addElement( chunk );
						
						chunkLength		= Math.min( chunkLength, chunk.len - (chunk.rep > 1 ? chunk.fadeOut : 0) );
					}
				}

				// mix regions
				while( (chunkLength > 0) && threadRunning ) {
					len = Math.min( 8192, chunkLength );

					for( i = 0; i < regionList.size(); i++ ) {
						chunk	= (KriechChunk) regionList.elementAt( i );
						relOff	= framesWritten - chunk.outOff;
						inF.seekFrame( chunk.inOff + relOff );
						inF.readFrames( inBuf, 0, len );
						
						// --- filter ---
						if( chunk.flt >= 0 ) {
							convBuf2	= filter[ chunk.flt ];
							for( ch = 0; ch < inChanNum; ch++ ) {
								convBuf1	= inBuf[ch];

// System.out.println(" filter : "+convBuf2[0] +" ; "+convBuf2[1]+" ; "+convBuf2[2] );

								f1			= chunk.fltMem[ch][0];	// b1
								f2			= chunk.fltMem[ch][1];	// b2
								f3			= chunk.fltMem[ch][2];	// a1
								f4			= chunk.fltMem[ch][3];	// a2
								
								for( j = 0; j < len; j++ ) {
									f5			= convBuf1[j];		// b0
//									convBuf1[j]	= convBuf2[0] * f5 + convBuf2[1] * f1 + f2 - convBuf2[1] * f3 - convBuf2[0] * f4;
//									convBuf1[j]	= f5 - f1;
//									convBuf1[j]	= convBuf2[0] * f5 + convBuf2[1] * f3 - convBuf2[2] * f4;
//									convBuf1[j]	= convBuf2[0] * (f5 - f2) + convBuf2[1] * f3 + convBuf2[2] * f4;
									convBuf1[j]	= convBuf2[0] * f5 + convBuf2[1] * f1 + convBuf2[2] * f2 -
												  convBuf2[1] * f3 - convBuf2[2] * f4;
									f1			= f5;				// b0 -> b1
									f4			= f3;				// a1 -> a2
									f3			= convBuf1[j];		// a0 -> a1
								}

								chunk.fltMem[ch][0]	= f1;
								chunk.fltMem[ch][1]	= f2;
								chunk.fltMem[ch][2] = f3;
								chunk.fltMem[ch][3] = f4;
							}
						}
						
						// --- fade in ---
						j		= chunk.fadeIn - relOff;
						if( j > 0 ) {
							k		= j;
							j		= Math.min( j, len );
							fadeY2	= (float) (k - j) / (float) chunk.fadeIn;
							fadeY1	= 1.0f - (float) relOff / (float) chunk.fadeIn;
							f1		= (fadeY1 - fadeY2) / (j - 1);
// System.out.println( "j "+ j+"; k "+k+"; Y1 "+fadeY1+"; Y2 "+fadeY2+"; f1 "+f1+"; relOff "+relOff );

							for( m = 0; m < j; m++ ) {
// if( b2 ) System.out.println( 1.0f-(fadeY1*fadeY1) );
								for( ch = 0; ch < inChanNum; ch++ ) {
									inBuf[ch][m] *= 1.0f - (fadeY1*fadeY1);
								}
								fadeY1 -= f1;
							}
						}
						// --- fade out ---
						j = chunk.len - chunk.fadeOut - relOff;
						if( j < len ) {	// fade out
							k		= j;
							j		= Math.max( j, 0 );
							fadeY1	= (float) (j - k) / (float) chunk.fadeOut;
							fadeY2	= (float) (k - len) / (float) chunk.fadeOut;
							f1		= (fadeY2 + fadeY1) / (len - j - 1);
// System.out.println( "OO Y1 "+fadeY1+"; Y2 "+fadeY2+"   ; j "+j+"; k "+k );
	
							for( m = j; m < len; m++ ) {
								for( ch = 0; ch < inChanNum; ch++ ) {
									inBuf[ch][m] *= 1.0f - (fadeY1*fadeY1);
								}
								fadeY1 -= f1;
							}
						}
						
						if( i == 0 ) {
							for( ch = 0; ch < inChanNum; ch++ ) {
								System.arraycopy( inBuf[ch], 0, outBuf[ch], 0, len );
							}
						} else {
							for( ch = 0; ch < inChanNum; ch++ ) {
								Util.add( inBuf[ch], 0, outBuf[ch], 0, len );
							}
						}
					}
					
					// write output
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
					framesWritten += len;
					chunkLength   -= len;
					progOff       += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
				
				chunkNumHist += regionList.size();
				chunkNumHistCnt++;
				
			} // while framesWritten < outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

			inF.close();
			inF = null;

		// ---- normalize output ----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
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
			outF = null;

//			setProgression( 1.0f );

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
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------

	protected double sinh( double x )
	{
		return( (Math.exp( x ) - Math.exp( -x )) / 2 );
	}

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
			ggSlave = (ParamField) gui.getItemObj( GG_OUTLENGTH );
			if( ggSlave != null ) {
				ggSlave.setReference( ref );
			}

		} catch( IOException ignored) {}
	}
}
// class KriechstromDlg

/**
 *	Interne Klasse fuer die Synthese
 */
class KriechChunk
{
	int			inOff, len, rep;
	int			fadeIn, fadeOut, outOff;
	int			nextLen;
	int			flt;
	float[][]	fltMem;
	
	public KriechChunk( int numChan )
	{
		fltMem	= new float[ numChan ][ 4 ];
	}
	
	public KriechChunk( KriechChunk src ) {
		this.inOff		= src.inOff;
		this.len		= src.len;
		this.rep		= src.rep;
		this.fadeIn		= src.fadeIn;
		this.fadeOut	= src.fadeOut;
		this.nextLen	= src.nextLen;
		this.flt		= src.flt;
		this.fltMem		= new float[ src.fltMem.length ][ 4 ];
	}
} // class KriechChunk
