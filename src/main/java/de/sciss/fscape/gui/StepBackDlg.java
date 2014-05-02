/*
 *  StepBackDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *  	04-Aug-10	corrected marker position (we lost one stepsize per marker)
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
import de.sciss.io.Marker;

/**
 *	Experimentation with transient detection: Produces
 *	a sound file segmentation and rearranges the
 *	time slices.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 04-Aug-10
 */
public class StepBackDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_CORRLENGTH			= 2;
	private static final int PR_CORRSTEP			= 3;
	private static final int PR_CORRFINE			= 4;
	private static final int PR_MODE				= 5;
	private static final int PR_MINSPACING			= 0;		// pr.para
	private static final int PR_MAXSPACING			= 1;
	private static final int PR_MINXFADE			= 2;
	private static final int PR_MAXXFADE			= 3;
	private static final int PR_OFFSET				= 4;
	private static final int PR_WEIGHT				= 5;
	private static final int PR_MARKERS				= 0;		// pr.bool

	private static final int MODE_RVSDECON			= 0;
	private static final int MODE_RNDDECON			= 1;
	private static final int MODE_RVSRECON			= 2;
	private static final int MODE_FWD				= 3;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_CORRLENGTH		= "CorrLength";
	private static final String PRN_CORRSTEP		= "CorrStep";
	private static final String PRN_CORRFINE		= "CorrFine";
	private static final String PRN_MODE			= "Mode";
	private static final String PRN_MINSPACING		= "MinSpacing";
	private static final String PRN_MAXSPACING		= "MaxSpacing";
	private static final String PRN_MINXFADE		= "MinXFade";
	private static final String PRN_MAXXFADE		= "MaxXFade";
	private static final String PRN_OFFSET			= "Offset";
	private static final String PRN_WEIGHT			= "Weight";
	private static final String PRN_MARKERS			= "Markers";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, 7, 9, 12, MODE_RVSDECON };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_CORRLENGTH, PRN_CORRSTEP,
													PRN_CORRFINE, PRN_MODE };
	private static final Param	prPara[]		= { null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_MINSPACING, PRN_MAXSPACING, PRN_MINXFADE, PRN_MAXXFADE,
													PRN_OFFSET, PRN_WEIGHT };
	private static final boolean	prBool[]	= { true };
	private static final String	prBoolName[]	= { PRN_MARKERS };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_CORRLENGTH		= GG_OFF_CHOICE		+ PR_CORRLENGTH;
	private static final int GG_CORRSTEP		= GG_OFF_CHOICE		+ PR_CORRSTEP;
	private static final int GG_CORRFINE		= GG_OFF_CHOICE		+ PR_CORRFINE;
	private static final int GG_MODE			= GG_OFF_CHOICE		+ PR_MODE;
	private static final int GG_MINSPACING		= GG_OFF_PARAMFIELD	+ PR_MINSPACING;
	private static final int GG_MAXSPACING		= GG_OFF_PARAMFIELD	+ PR_MAXSPACING;
	private static final int GG_MINXFADE		= GG_OFF_PARAMFIELD	+ PR_MINXFADE;
	private static final int GG_MAXXFADE		= GG_OFF_PARAMFIELD	+ PR_MAXXFADE;
	private static final int GG_OFFSET			= GG_OFF_PARAMFIELD	+ PR_OFFSET;
	private static final int GG_WEIGHT			= GG_OFF_PARAMFIELD	+ PR_WEIGHT;
	private static final int GG_MARKERS			= GG_OFF_CHECKBOX	+ PR_MARKERS;
	private static final int GG_CUTINFO			= GG_OFF_OTHER		+ 0;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	MARK_CUT			= "Cut";

	private double inLengthMillis = -1.0;	// [ms]

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public StepBackDlg()
	{
		super( "Step Back" );
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
			static_pr.para[ PR_MINSPACING ]			= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_MAXSPACING ]			= new Param( 5000.0, Param.ABS_MS );
			static_pr.para[ PR_MINXFADE ]			= new Param(    1.0, Param.ABS_MS );
			static_pr.para[ PR_MAXXFADE ]			= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_OFFSET ]				= new Param(    0.0, Param.OFFSET_MS );
			static_pr.para[ PR_WEIGHT ]				= new Param(   50.0, Param.FACTOR_AMP );
			static_pr.paraName	= prParaName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
//			static_pr.envl		= prEnvl;
//			static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		JComboBox			ggCorrLength, ggCorrStep, ggCorrFine, ggMode;
		ParamField			ggMinSpacing, ggMaxSpacing, ggMinXFade, ggMaxXFade, ggOffset, ggWeight;
		JTextField			ggCutInfo;
		JCheckBox			ggMarkers;
		PathField[]			ggInputs;
		ParamSpace[]		spcSpacing, spcOffset;
		String				s;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ParamListener paramL = new ParamListener() {
			public void paramChanged( ParamEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_MINSPACING:
				case GG_MAXSPACING:
					pr.para[ ID - GG_OFF_PARAMFIELD ] = ((ParamField) e.getSource()).getParam();
					recalcApprox();
					break;
				}
			}
		};
		
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
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Back$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Tiling Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		con.fill		= GridBagConstraints.HORIZONTAL;
		
		spcSpacing		= new ParamSpace[ 2 ];
		spcSpacing[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcSpacing[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcOffset		= new ParamSpace[ 2 ];
		spcOffset[0]	= Constants.spaces[ Constants.offsetMsSpace ];
		spcOffset[1]	= Constants.spaces[ Constants.offsetBeatsSpace ];

		ggMinSpacing	= new ParamField( spcSpacing );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min.spacing", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMinSpacing, GG_MINSPACING, paramL );
		ggMinXFade		= new ParamField( spcSpacing );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Min.X-fade", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMinXFade, GG_MINXFADE, paramL );

		ggMaxSpacing	= new ParamField( spcSpacing );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Max.spacing", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMaxSpacing, GG_MAXSPACING, paramL );
		ggMaxXFade		= new ParamField( spcSpacing );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Max.X-fade", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggMaxXFade, GG_MAXXFADE, paramL );

		ggOffset		= new ParamField( spcOffset );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Offset", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggOffset, GG_OFFSET, paramL );
		ggWeight		= new ParamField( Constants.spaces[ Constants.ratioAmpSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Energy priority", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggWeight, GG_WEIGHT, paramL );

		ggCutInfo		= new JTextField();
		ggCutInfo.setEditable( false );
		ggCutInfo.setBackground( null );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Approx.# of tiles", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addTextField( ggCutInfo, GG_CUTINFO, null );
		ggMarkers		= new JCheckBox();
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Write markers", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggMarkers, GG_MARKERS, null );

		ggCorrLength	= new JComboBox();
		ggCorrStep		= new JComboBox();
		ggCorrFine		= new JComboBox();
ggCorrFine.setEnabled( false ); // XXX
		for( int i = 131072; i > 0; i>>=1 ) {
			s = String.valueOf( i );
			if( i >= 32 ) ggCorrLength.addItem( s );
			ggCorrStep.addItem( s );
			ggCorrFine.addItem( s );
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Corr.length [smp]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggCorrLength, GG_CORRLENGTH, null );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Stepsize [smp]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggCorrStep, GG_CORRSTEP, null );

		ggMode			= new JComboBox();
		ggMode.addItem( "Reverse De-construct" );
		ggMode.addItem( "Random De-construct" );
		ggMode.addItem( "Reverse Re-construct" );
		ggMode.addItem( "Forward Segmentation" );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mode", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggMode, GG_MODE, null );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Fine step", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggCorrFine, GG_CORRFINE, null );

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
		int					i, j, k, ch, len, off, chunkLength;
		long				progOff, progLen;
		double				d1, d2;
		float				f1, f2;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		float[][]			inBuf			= null;
		float[][]			xfadeBuf;
		float[]				fftBuf1, fftBuf2, fftBuf3, timeBuf, win, convBuf1;
		float[]				xcorrs, energies;

		// Smp Init
		int					inLength, inChanNum;
		int					framesRead, numCuts, fadeLen, cutOffset;
		int					lastCut, stopCut, minLoc, spacing, minSpacing, maxSpacing;	// [chunks � corrStep]
		int					minXFade, maxXFade, corrLength, corrStep;			// [smp]
		float				minCorr, xcorr, minEnergy;
		double				energy1, energy2, crossTerm;
		int					bufOffset, bufLength, overlaps;
		float				noiseFloor	= 1.0e-6f;	// -120 dB
		float				spectWeight, rmsWeight;	// , totMinCorr, totMaxCorr;

		PathField			ggOutput;
		Vector				rndMarkers		= new Vector();
		Random				rnd				= new Random( System.currentTimeMillis() );
		java.util.List		markers;

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
			if( !pr.bool[ PR_MARKERS ]) {	// otherwise wait...
				outF	= AudioFile.openAsWrite( outStream );
			} else {
				IOUtil.createEmptyFile( new File( pr.text[ PR_OUTPUTFILE ]));
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			markers			= (java.util.List) outStream.getProperty( AudioFileDescr.KEY_MARKERS );
			if( markers == null ) {
				markers		= new Vector();
				outStream.setProperty( AudioFileDescr.KEY_MARKERS, markers );
			}

			// initialize various stuff
			corrLength		= 131072 >> pr.intg[ PR_CORRLENGTH ];
			corrStep		= Math.min( corrLength, 131072 >> pr.intg[ PR_CORRSTEP ]);
//			corrFine		= Math.min( corrStep,   131072 >> pr.intg[ PR_CORRFINE ]);
			overlaps		= corrLength / corrStep;
			stopCut			= (inLength + corrStep-1) / corrStep;
// System.out.println( "minSpc "+minSpacing+"; maxSpc "+maxSpacing+"; inLength "+inLength );

			minSpacing		= ((int) (AudioFileDescr.millisToSamples( inStream, Param.transform( pr.para[ PR_MINSPACING ],
									  Param.ABS_MS, null, null ).val ) + 0.5) + corrStep-1) / corrStep;
			maxSpacing		= ((int) (AudioFileDescr.millisToSamples( inStream, Param.transform( pr.para[ PR_MAXSPACING ],
									  Param.ABS_MS, null, null ).val ) + 0.5) + corrStep-1) / corrStep;
			minXFade		= (int) (AudioFileDescr.millisToSamples( inStream, Param.transform( pr.para[ PR_MINXFADE ],
																  Param.ABS_MS, null, null ).val ) + 0.5);
			maxXFade		= (int) (AudioFileDescr.millisToSamples( inStream, Param.transform( pr.para[ PR_MAXXFADE ],
																  Param.ABS_MS, null, null ).val ) + 0.5);
			cutOffset		= (int) (AudioFileDescr.millisToSamples( inStream, Param.transform( pr.para[ PR_OFFSET ],
																  Param.ABS_MS, new Param( 0.0, Param.ABS_MS ), null ).val ) + 0.5);
			
			if( minSpacing > maxSpacing ) {
				i			= minSpacing;
				minSpacing	= maxSpacing;
				maxSpacing	= i;
			}
			if( minXFade > maxXFade ) {
				i			= minXFade;
				minXFade	= maxXFade;
				maxXFade	= i;
			}
			
			xcorrs			= new float[ maxSpacing - minSpacing + 1 ];
			energies		= new float[ xcorrs.length ];

			spectWeight		= Math.min( 1.0f, 2.0f - (float) (pr.para[ PR_WEIGHT ].val / 50) );
			rmsWeight		= Math.min( 1.0f, (float) (pr.para[ PR_WEIGHT ].val / 50) );
			f1				= spectWeight + rmsWeight;
			spectWeight	   /= f1;
			rmsWeight	   /= f1;
// System.out.println( "spectWeight "+spectWeight+"; rmsWeight "+rmsWeight );
//			totMinCorr		= 2.0f;
//			totMaxCorr		= 0.0f;

			// buffers
			inBuf			= new float[ inChanNum ][ 8192 ];
			fftBuf1			= new float[ corrLength + 2 ];
			fftBuf2			= new float[ corrLength + 2 ];
			if( pr.intg[ PR_MODE ] == MODE_RVSRECON ) {
				fftBuf3		= new float[ corrLength + 2 ];
			} else {
				fftBuf3		= null;
			}
			bufLength		= corrLength * 3 - corrStep;
			bufOffset		= bufLength - corrLength;		// for successive reads
			timeBuf			= new float[ bufLength ];
			Util.clear( timeBuf );
			win				= Filter.createFullWindow( corrLength, Filter.WIN_BLACKMAN );
			
			progOff			= 0;
			progLen			= (long) inLength*3;

		// ----==================== step one: segmentation ====================----

			numCuts			= 0;
			lastCut			= 0;	// XXX +offset
			framesRead		= 0;
			spacing			= -(overlaps-1);
			minCorr			= -1.0f;
			minLoc			= 0;
			
			do {
				chunkLength	= Math.min( inLength - framesRead, corrLength );
// System.out.println( "Chunk length "+chunkLength );
				System.arraycopy( timeBuf, corrLength, timeBuf, 0, bufLength - corrLength );
					
				for( off = 0; threadRunning && (off < chunkLength); ) {
					len	= Math.min( 8192, chunkLength - off );
					inF.readFrames( inBuf, 0, len );
					System.arraycopy( inBuf[ 0 ], 0, timeBuf, bufOffset + off, len );
					for( ch = 1; ch < inChanNum; ch++ ) {	// sum channels, not really true-rms-summing...
						Util.add( inBuf[ ch ], 0, timeBuf, bufOffset + off, len );
					}
					off			+= len;
					progOff		+= len;
					framesRead	+= len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
	
				// post zero padding
				if( chunkLength < corrLength ) {
					for( i = bufOffset + chunkLength; i < bufLength; ) {
						timeBuf[ i++ ] = 0.0f;
					}
				}
	
				for( i = 0; threadRunning && (i < overlaps); i++ ) {
					// track corr.
					if( spacing >= minSpacing ) {
						if( pr.intg[ PR_MODE ] != MODE_RVSRECON ) {
							System.arraycopy( timeBuf, i * corrStep, fftBuf1, 0, corrLength );
							Util.mult( win, 0, fftBuf1, 0, corrLength );
							Fourier.realTransform( fftBuf1, corrLength, Fourier.FORWARD );
						}
						System.arraycopy( timeBuf, i * corrStep + corrLength, fftBuf2, 0, corrLength );
						Util.mult( win, 0, fftBuf2, 0, corrLength );
						Fourier.realTransform( fftBuf2, corrLength, Fourier.FORWARD );
		
						energy1		= 0.0;
						energy2		= 0.0;
						crossTerm	= 0.0;
						for( j = 0; j <= corrLength; j += 2 ) {
							k			 = j + 1;
							d1			 = fftBuf1[ j ]*fftBuf1[ j ] + fftBuf1[ k ]*fftBuf1[ k ];
							d2			 = fftBuf2[ j ]*fftBuf2[ j ] + fftBuf2[ k ]*fftBuf2[ k ];
							energy1		+= d1;
							energy2		+= d2;
							crossTerm	+= Math.sqrt( d1 ) * Math.sqrt( d2 );
						}
						energy1 = Math.sqrt( energy1 );
						energy2 = Math.sqrt( energy2 );
						if( (energy1 > noiseFloor) && (energy2 > noiseFloor) ) {
							if( energy1 > energy2 ) {
								d1 = energy2 / energy1;
							} else {
								d1 = energy1 / energy2;
							}
							xcorr = (float) (crossTerm / (energy1 * energy2) * spectWeight + d1 * rmsWeight);
						} else {
							xcorr	= 0.0f;
						}
							
						xcorrs[ spacing - minSpacing ]		= xcorr;
						energies[ spacing - minSpacing ]	= (float) (energy1 + energy2);
						
						if( pr.intg[ PR_MODE ] == MODE_RVSRECON ) {
							if( xcorr > minCorr ) {
								minCorr = xcorr;
								minLoc	= spacing - minSpacing;
								System.arraycopy( fftBuf2, 0, fftBuf3, 0, corrLength );
							}
						}
					}

				// .... progress ....
					progOff	+= chunkLength/overlaps;
					setProgression( (float) progOff / (float) progLen );
						
					spacing++;
					// find cut, reset buffers
					if( spacing > maxSpacing ) {
						if( pr.intg[ PR_MODE ] != MODE_RVSRECON ) {
							minCorr		= 2.0f;
							minLoc		= 0;
							minEnergy	= 0.0f;
findMinLp:					for( j = 0; j < xcorrs.length; j++ ) {
								xcorr	= xcorrs[ j ];
								f1		= xcorr/minCorr;
								if( (f1 < 0.9f) || ((f1 < 1.0f) && (energies[ j ]/minEnergy < 1.1)) ) {
									minCorr		= xcorr;
									minEnergy	= energies[ j ];
									minLoc		= j;
									if( minCorr == 0.0f ) break findMinLp;
									
								} else if( (f1 < 1.1f) && (energies[ j ]/minEnergy < 0.9) ) {
	
									minCorr		= Math.min( minCorr, xcorr );
									minEnergy	= energies[ j ];
									minLoc		= j;
								}
							}
						} else {
							minCorr		= -1.0f;
							minLoc		= 0;
							System.arraycopy( fftBuf3, 0, fftBuf1, 0, corrLength );
						}
						
//						totMinCorr	 = Math.min( totMinCorr, minCorr );
//						totMaxCorr	 = Math.max( totMaxCorr, minCorr );
						if( pr.intg[ PR_MODE ] == MODE_FWD ) {
							j		 = Math.min( inLength, Math.max( 0, lastCut * corrStep + cutOffset ));	// sample position
						} else {
							j		 = Math.min( inLength, Math.max( 0, inLength - lastCut * corrStep - cutOffset ));	// sample position
						}
//						lastCut		+= minSpacing + minLoc;
						lastCut		+= minSpacing + minLoc + 1; // XXX why + 1? 
						if( pr.intg[ PR_MODE ] == MODE_RNDDECON ) {
							k		 = (int) (rnd.nextFloat() * numCuts + 0.5f);
							markers.add( k, new Marker( j, MARK_CUT ));
							j		 = Math.min( inLength, Math.max( 0, lastCut * corrStep + cutOffset ));
							rndMarkers.insertElementAt( new Marker( j, MARK_CUT ), k );
						} else {
							markers.add( 0, new Marker( j, MARK_CUT ));
						}
						numCuts++;
// System.out.println( "@ "+((float) (lastCut * corrStep) / inStream.smpRate)+" secs." );
						minLoc++;
						j			 = minLoc + minSpacing;
						if( j < xcorrs.length ) {	// reuse (shifted) data
							System.arraycopy( xcorrs,   j, xcorrs,   0, xcorrs.length - j );
							System.arraycopy( energies, j, energies, 0, xcorrs.length - j );
						}
						spacing	= xcorrs.length - minLoc;
					}
				} // for i = 0 to overlaps-1
				setProgression( (float) progOff / (float) progLen );
					
			} while( threadRunning && (lastCut < stopCut) );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			gui.stringToJTextField( numCuts + " detected.", GG_CUTINFO );

		// ----==================== step two: write output ====================----

			xcorrs		= null;
			energies	= null;
			timeBuf		= null;
			xfadeBuf	= new float[ inChanNum ][ maxXFade ];

			if( pr.bool[ PR_MARKERS ]) {	// now got it
				outF	= AudioFile.openAsWrite( outStream );
			}

			lastCut = inLength;
// System.out.println( "progOff "+progOff+"; progLen-inLength "+(progLen-inLength) );
			progOff	= progLen - inLength;

			if( pr.intg[ PR_MODE ] == MODE_FWD ) {
			
				// simply copy input to output
				framesRead = 0;
				inF.seekFrame( 0 );
				while( threadRunning && (framesRead < inLength) ) {
					len				= Math.min( 8192, inLength - framesRead );
					inF.readFrames( inBuf, 0, len );
					outF.writeFrames( inBuf, 0, len );
					progOff		   += len;
					framesRead	   += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}

			} else {
			
				for( i = 0; threadRunning && (i < numCuts); i++ ) {
				
					if( pr.intg[ PR_MODE ] == MODE_RNDDECON ) {
						stopCut	= (int) ((Marker) rndMarkers.elementAt( i )).pos;
					} else {
						stopCut	= lastCut;
					}
					lastCut		= inLength - (int) ((Marker) markers.get( i )).pos;
					convBuf1	= fftBuf1;
					fftBuf1		= fftBuf2;
					fftBuf2		= convBuf1;
// System.out.println( "cut "+lastCut+" � "+stopCut );
	
				// ---------- xfade part ----------
					if( i > 0 ) {
						j	= lastCut - corrLength;
						off	= 0;
						if( j < 0 ) {
							j = -j;
							while( off < j ) {
								fftBuf1[ off++ ] = 0.0f;
							}
						}
						inF.seekFrame( j - off );
						while( off < corrLength ) {
							len = Math.min( 8192, corrLength - off );
							inF.readFrames( inBuf, 0, len );
							System.arraycopy( inBuf[ 0 ], 0, fftBuf1, off, len );
							for( ch = 1; ch < inChanNum; ch++ ) {	// sum channels, not really true-rms-summing...
								Util.add( inBuf[ ch ], 0, fftBuf1, off, len );
							}
							off += len;
						}
						Util.mult( win, 0, fftBuf1, 0, corrLength );
						Fourier.realTransform( fftBuf1, corrLength, Fourier.FORWARD );
	
						energy1		= 0.0;
						energy2		= 0.0;
						crossTerm	= 0.0;
						for( j = 0; j <= corrLength; j += 2 ) {
							k			 = j + 1;
							d1			 = fftBuf1[ j ]*fftBuf1[ j ] + fftBuf1[ k ]*fftBuf1[ k ];
							d2			 = fftBuf2[ j ]*fftBuf2[ j ] + fftBuf2[ k ]*fftBuf2[ k ];
							energy1		+= d1;
							energy2		+= d2;
							crossTerm	+= Math.sqrt( d1 ) * Math.sqrt( d2 );
						}
						energy1 = Math.sqrt( energy1 );
						energy2 = Math.sqrt( energy2 );
						if( (energy1 > noiseFloor) && (energy2 > noiseFloor) ) {
							if( energy1 > energy2 ) {
								d1 = energy2 / energy1;
							} else {
								d1 = energy1 / energy2;
							}
							xcorr = (float) (crossTerm / (energy1 * energy2) * spectWeight + d1 * rmsWeight);
						} else {
							xcorr	= 0.0f;
						}
//						if( totMaxCorr != totMinCorr ) {
//							xcorr  = (xcorr - totMinCorr) / (totMaxCorr - totMinCorr);	// normalize
//							xcorr *= xcorr;
//						} else {
						xcorr	= (float) Math.log( xcorr * 1.718281828f + 1.0f );
						fadeLen = (int) (maxXFade * xcorr + minXFade * (1.0f - xcorr)) >> 1;
						fadeLen	= Math.min( lastCut, Math.min( bufLength, Math.max( minXFade, fadeLen )));
// System.out.println( i+": creating xfade. bufLength "+bufLength+"; fadeLen "+fadeLen );

// Debug.view( xfadeBuf[0], i+" - untouched" );

						// fadeout
						energy1 = 0.0;
						for( ch = 0; ch < inChanNum; ch++ ) {
							convBuf1 = xfadeBuf[ ch ];
							for( off = bufLength - fadeLen; off < bufLength; off++ ) {
								energy1			+= convBuf1[ off ]*convBuf1[ off ];
								f1				 = (float) (bufLength - off) / (float) fadeLen;
								convBuf1[ off ] *= f1;
							}
						}
						energy1 = Math.sqrt( energy1 );
// System.out.println( "fading in" );
// Debug.view( xfadeBuf[0], i+" - faded out" );

						// fadein
						j		= lastCut - fadeLen;
						inF.seekFrame( j );
						energy2	= 0.0;
						for( off = bufLength - fadeLen; off < bufLength; ) {
							len = Math.min( 8192, bufLength - off );
							inF.readFrames( inBuf, 0, len );
							for( ch = 0; ch < inChanNum; ch++ ) {	// sum channels, not really true-rms-summing...
								convBuf1 = xfadeBuf[ ch ];
								for( j = 0, k = off; j < len; ) {
									energy2			  += inBuf[ ch ][ j ]*inBuf[ ch ][ j ];
									f1				   = 1.0f - (float) (bufLength - k) / (float) fadeLen;
									convBuf1[ k++ ] += inBuf[ ch ][ j++ ] * f1;
								}
							}
							off += len;
						}
						energy1 = (energy1 + Math.sqrt( energy2 )) / 2;
// Debug.view( xfadeBuf[0], i+" - plus fadein" );

						// adjusting volume in 1 dB Schritten
						k = bufLength - fadeLen/2;
volumeLp:				for( j = 0, f1 = 1.122462f; j < 6; j++, f1 *= 1.122462f ) {
							energy2 = 0.0;
							for( ch = 0; ch < inChanNum; ch++ ) {	// sum channels, not really true-rms-summing...
								convBuf1 = xfadeBuf[ ch ];
								for( off = bufLength - fadeLen; off < bufLength; off++ ) {
									// parabel
									f2	= 2 * (off - k) / (float) fadeLen;
									f2	= (f1 - 1.0f) * (1.0f - f2*f2) + 1.0f;
									d1  = convBuf1[ off ]*f2;
									d1 *= d1;
									if( d1 > 1.0 ) break volumeLp;
									energy2 += d1;
								}
							}
							if( energy2 > energy1 ) break volumeLp;
						}
// System.out.println( "vol: "+j+"; "+f1+"; energy1 "+energy1+"; 2= "+energy2 );
						energy1 = 0;
						if( j > 0 ) {
							f1 /= 1.122462f;
							for( ch = 0; ch < inChanNum; ch++ ) {	// sum channels, not really true-rms-summing...
								convBuf1 = xfadeBuf[ ch ];
								for( off = bufLength - fadeLen; off < bufLength; off++ ) {
									f2				 = 2 * (off - k) / (float) fadeLen;
									f2				 = (f1 - 1.0f) * (1.0f - f2*f2) + 1.0f;
									convBuf1[ off ] *= f2;
								}
							}
						}
// Debug.view( xfadeBuf[0], i+" - plus volume" );
						
						// write xfade
						for( off = 0; off < bufLength; ) {
							len		 = Math.min( 8192, bufLength - off );
							outF.writeFrames( xfadeBuf, off, len );
							off		+= len;
							progOff	+= len;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						}
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
	
				// ---------- xfade ende ----------
					} else {
						inF.seekFrame( lastCut );
					}
					
					chunkLength 	= stopCut - lastCut;
					if( i+1 != numCuts ) {
						bufLength	= Math.min( chunkLength, maxXFade );
					} else {
						bufLength	= 0;	// no xfade after the last cut
					}
					off				= chunkLength - bufLength;
					framesRead		= 0;
// System.out.println( "writing output; reserved for xfade: " + bufLength );
					// write to output
					while( threadRunning && (framesRead < off) ) {
						len				= Math.min( 8192, off - framesRead );
						inF.readFrames( inBuf, 0, len );
						outF.writeFrames( inBuf, 0, len );
						progOff		   += len;
						framesRead	   += len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
					// read to xfade buf
					while( threadRunning && (framesRead < chunkLength) ) {
						len				= Math.min( 8192, chunkLength - framesRead );
						inF.readFrames( xfadeBuf, framesRead - off, len );
						framesRead	   += len;
					}
					
				} // for i = 0 to numCuts-1
			} // if not MODE_FWD
		// .... check running ....
			if( !threadRunning ) break topLevel;
	
		// ---- clean up ----

			setProgression( 1.0f );
		
			inF.close();
			inF			= null;
			inStream	= null;

			outF.close();
			outF = null;

		// ---- Finish ----

		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			inBuf		= null;
			fftBuf1		= null;
			fftBuf2		= null;
			fftBuf3		= null;
			timeBuf		= null;
			win			= null;
			xcorrs		= null;
			energies	= null;
			convBuf1	= null;
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
	} // process()

// -------- private Methoden --------

	/**
	 *	Neues Inputfile setzen
	 */
	protected void setInput( String fname )
	{
		AudioFile		f		= null;
		AudioFileDescr		stream	= null;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

			inLengthMillis = AudioFileDescr.samplesToMillis( stream, stream.length );
			recalcApprox();
			
		} catch( IOException e1 ) {
			inLengthMillis = -1.0;
		}
	}
	
	// calc approximate # of tiles according to input length, min+max spacing
	protected void recalcApprox()
	{
		int			minNum, maxNum, meanNum;
		double		d1, d2;

		if( inLengthMillis >= 0 ) {

			d1			= Param.transform( pr.para[ PR_MAXSPACING ], Param.ABS_MS, null, null ).val;
			d2			= Param.transform( pr.para[ PR_MINSPACING ], Param.ABS_MS, null, null ).val;
			minNum		= (int) Math.ceil( inLengthMillis / d1 );
			maxNum		= (int) Math.ceil( inLengthMillis / d2 );
			meanNum		= (int) Math.ceil( inLengthMillis / ((d1+d2)/2) );

			gui.stringToJTextField( "\u2300" + meanNum + " (" +minNum + "\u2013" + maxNum + ")", GG_CUTINFO );
			
		} else {
		
			gui.stringToJTextField( "", GG_CUTINFO );
		
		}
	}
}
// class StepBackDlg
