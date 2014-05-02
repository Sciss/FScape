/*
 *  DrMurkeDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
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
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  @version	0.74, 03-Nov-10
 */
public class DrMurkeDlg
extends ModulePanel
{
// -------- private Variablen --------
	private static final boolean verbose = false;

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_CTRLFILE		= 1;
	private static final int PR_OUTPUTFILE		= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_MODE			= 3;
	private static final int PR_CHANNELUP		= 4;
	private static final int PR_CHANNELDOWN		= 5;
	private static final int PR_SPACINGTYPE		= 6;
	private static final int PR_THRESHUP		= 0;		// pr.para
	private static final int PR_THRESHDOWN		= 1;
	private static final int PR_DURUP			= 2;
	private static final int PR_DURDOWN			= 3;
	private static final int PR_ATTACK			= 4;
	private static final int PR_RELEASE			= 5;
	private static final int PR_GAIN			= 6;
	private static final int PR_SPACING			= 7;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_CTRLFILE		= "CtrlFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_MODE			= "Mode";
	private static final String PRN_CHANNELUP		= "ChannelUp";
	private static final String PRN_CHANNELDOWN		= "ChannelDown";
	private static final String PRN_SPACINGTYPE		= "SpacingType";
	private static final String PRN_THRESHUP		= "ThreshUp";
	private static final String PRN_THRESHDOWN		= "ThreshDown";
	private static final String PRN_DURUP			= "DurUp";
	private static final String PRN_DURDOWN			= "DurDown";
	private static final String PRN_ATTACK			= "Attack";
	private static final String PRN_RELEASE			= "Release";
	private static final String PRN_SPACING			= "Spacing";
	
	private static final String[]	MODE_NAMES		= { "Keep Upper Chunks", "Keep Down Chunks" };
	private static final int		MODE_UPPER		= 0;
	private static final int		MODE_DOWN		= 1;

	private static final String[]	CHANNEL_NAMES	= { "Take Maximum", "Take Minimum" };
	private static final int		CHANNEL_MAX		= 0;
	private static final int		CHANNEL_MIN		= 1;

	private static final String[]	SPACING_NAMES	= { "Fixed Spacing:", "Original Spacing" };
	private static final int		SPACING_FIXED	= 0;
	private static final int		SPACING_ORIGINAL= 1;

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_CTRLFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, MODE_UPPER, CHANNEL_MIN, CHANNEL_MAX, SPACING_FIXED };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_MODE, PRN_CHANNELUP,
													PRN_CHANNELDOWN, PRN_SPACINGTYPE };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_THRESHUP, PRN_THRESHDOWN, PRN_DURUP, PRN_DURDOWN, PRN_ATTACK, PRN_RELEASE,
													PRN_GAIN, PRN_SPACING };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_CTRLFILE			= GG_OFF_PATHFIELD	+ PR_CTRLFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_MODE				= GG_OFF_CHOICE		+ PR_MODE;
	private static final int GG_CHANNELUP			= GG_OFF_CHOICE		+ PR_CHANNELUP;
	private static final int GG_CHANNELDOWN			= GG_OFF_CHOICE		+ PR_CHANNELDOWN;
	private static final int GG_SPACINGTYPE			= GG_OFF_CHOICE		+ PR_SPACINGTYPE;
	private static final int GG_THRESHUP			= GG_OFF_PARAMFIELD	+ PR_THRESHUP;
	private static final int GG_THRESHDOWN			= GG_OFF_PARAMFIELD	+ PR_THRESHDOWN;
	private static final int GG_DURUP				= GG_OFF_PARAMFIELD	+ PR_DURUP;
	private static final int GG_DURDOWN				= GG_OFF_PARAMFIELD	+ PR_DURDOWN;
	private static final int GG_ATTACK				= GG_OFF_PARAMFIELD	+ PR_ATTACK;
	private static final int GG_RELEASE				= GG_OFF_PARAMFIELD	+ PR_RELEASE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_SPACING				= GG_OFF_PARAMFIELD	+ PR_SPACING;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public DrMurkeDlg()
	{
		super( "Dr Murke" );
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
//			static_pr.bool		= prBool;
//			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_THRESHUP ]	= new Param(   30.0, Param.FACTOR_AMP );
			static_pr.para[ PR_THRESHDOWN ]	= new Param(   20.0, Param.FACTOR_AMP );
			static_pr.para[ PR_DURUP ]		= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_DURDOWN ]	= new Param(   10.0, Param.ABS_MS );
			static_pr.para[ PR_ATTACK ]		= new Param(   10.0, Param.ABS_MS );
			static_pr.para[ PR_RELEASE ]	= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_SPACING ]	= new Param( 1000.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		final GridBagConstraints	con;

		final PathField			ggInputFile, ggCtrlFile, ggOutputFile;
		final PathField[]		ggInputs;
		final ParamField		ggDurUp, ggDurDown, ggThreshUp, ggThreshDown, ggAttack, ggRelease, ggSpacing;
		final ParamSpace[]		spcDur, spcThresh;
		final Component[]		ggGain;
		final JComboBox			ggMode, ggChannelUp, ggChannelDown, ggSpacingType;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		final ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID			= gui.getItemID( e );

				switch( ID ) {
				case GG_SPACINGTYPE:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
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
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggCtrlFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select control file" );
		ggCtrlFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Control file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggCtrlFile, GG_CTRLFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Mur$E" );
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
	gui.addLabel( new GroupLabel( "Segmentation Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcThresh		= new ParamSpace[] {
			Constants.spaces[ Constants.ratioAmpSpace ],
			Constants.spaces[ Constants.decibelAmpSpace ]
		};
		spcDur			= new ParamSpace[] {
			Constants.spaces[ Constants.absMsSpace ]
		};

		ggThreshUp		= new ParamField( spcThresh );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Up Thresh:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggThreshUp, GG_THRESHUP, null );

		ggDurUp			= new ParamField( spcDur );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Min. Up Duration:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggDurUp, GG_DURUP, null );

		ggThreshDown	= new ParamField( spcThresh );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Down Thresh:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggThreshDown, GG_THRESHDOWN, null );

		ggDurDown		= new ParamField( spcDur );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Min. Down Duration:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggDurDown, GG_DURDOWN, null );
		
		ggChannelUp		= new JComboBox();
		for( int i = 0; i < CHANNEL_NAMES.length; i++ ) {
			ggChannelUp.addItem( CHANNEL_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Multi-channel Up Treatment:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggChannelUp, GG_CHANNELUP, null );

		ggAttack		= new ParamField( spcDur );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Attack:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggAttack, GG_ATTACK, null );

		ggChannelDown	= new JComboBox();
		for( int i = 0; i < CHANNEL_NAMES.length; i++ ) {
			ggChannelDown.addItem( CHANNEL_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Multi-channel Down Treatment:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggChannelDown, GG_CHANNELDOWN, null );
		
		ggRelease		= new ParamField( spcDur );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Release:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggRelease, GG_RELEASE, null );

		ggMode			= new JComboBox();
		for( int i = 0; i < MODE_NAMES.length; i++ ) {
			ggMode.addItem( MODE_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Mode:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggMode, GG_MODE, null );

		ggSpacingType	= new JComboBox();
		for( int i = 0; i < SPACING_NAMES.length; i++ ) {
			ggSpacingType.addItem( SPACING_NAMES[ i ]);
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addChoice( ggSpacingType, GG_SPACINGTYPE, il );
		ggSpacing		= new ParamField( spcDur );
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggSpacing, GG_SPACING, null );
		
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
		// io
		AudioFile			inF		= null;
		AudioFile			ctrlF	= null;
		AudioFile			outF	= null;

		float				gain			= 1.0f;			// gain abs amp
		final Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		float				maxAmp			= 0.0f;

topLevel: try {

		// ---- open input, output; init ----

			// input
			inF								= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			final AudioFileDescr inStream 	= inF.getDescr();
			final int inChanNum				= inStream.channels;
			final long inLength				= inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength < 1) || (inChanNum < 1) ) throw new EOFException( ERR_EMPTY );

			ctrlF							= AudioFile.openAsRead( new File( pr.text[ PR_CTRLFILE ]));
			final AudioFileDescr ctrlStream	= ctrlF.getDescr();
			final int ctrlChanNum			= ctrlStream.channels;
			final long ctrlLength			= ctrlStream.length;
			// this helps to prevent errors from empty files!
			if( (ctrlLength < 1) || (ctrlChanNum < 1)  ) throw new EOFException( ERR_EMPTY );
//			if( ctrlLength > inLength ) throw new IOException( ERR_LENGTH );
			
			final double ctrlRate		= (double) ctrlLength / (double) inLength;

		// ---- open output ----
			final PathField ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			final AudioFileDescr outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF			= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			final AudioFile tmpF;
			final boolean normalized = pr.intg[ PR_GAINTYPE ] == GAIN_UNITY;
			if( normalized ) {
				tmpF	= createTempFile( outStream );
			} else {
				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				tmpF	= null;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			long progOff		= 0L;
			final long progLen	= ctrlLength * (normalized ? 2 : 1);

			final float[][] inBuf	= new float[ inChanNum ][   8192 ];
			final float[][] outBuf	= new float[ inChanNum ][   8192 ];
			final float[][] ctrlBuf	= new float[ ctrlChanNum ][ 8192 ];
			final float[] ctrlFrame = new float[ ctrlChanNum ];
			
			final float upThresh 	= (float) (Param.transform( pr.para[ PR_THRESHUP ],   Param.ABS_AMP, ampRef, null )).val;
			final float dnThresh 	= (float) (Param.transform( pr.para[ PR_THRESHDOWN ], Param.ABS_AMP, ampRef, null )).val;
			final long upCount		= Math.max( 1L,
					(long) (AudioFileDescr.millisToSamples( inStream, pr.para[ PR_DURUP ].val ) * ctrlRate + 0.5) );
			final long dnCount	 	= Math.max( 1L,
					(long) (AudioFileDescr.millisToSamples( inStream, pr.para[ PR_DURDOWN ].val ) * ctrlRate + 0.5) );
			
			final long fadeInFrames		= (long) (AudioFileDescr.millisToSamples( inStream, pr.para[ PR_ATTACK ].val ) + 0.5);
			final long fadeOutFrames	= (long) (AudioFileDescr.millisToSamples( inStream, pr.para[ PR_RELEASE ].val ) + 0.5);

			final ThreshFunc upFun;
			if( pr.intg[ PR_CHANNELUP ] == CHANNEL_MAX ) {
				upFun = new AboveFunc( new MaxFunc(), dnThresh );  
			} else {
				upFun = new AboveFunc( new MinFunc(), dnThresh );
			}
			final ThreshFunc dnFun;
			if( pr.intg[ PR_CHANNELDOWN ] == CHANNEL_MAX ) {
				dnFun = new BelowFunc( new MaxFunc(), upThresh );  
			} else {
				dnFun = new BelowFunc( new MinFunc(), upThresh );
			}
			
			final Track upTrack	= new Track( "up", dnCount, upFun, pr.intg[ PR_MODE ] == MODE_UPPER );
			final Track dnTrack	= new Track( "dn", upCount, dnFun, pr.intg[ PR_MODE ] == MODE_DOWN );
			upTrack.other	= dnTrack;
			dnTrack.other	= upTrack;
			Track current 	= dnTrack.write ? upTrack : dnTrack;
if( verbose ) System.out.println( "--> initial = " + current.name );
			final List fades = new ArrayList();
			
//			long framesRead 	= 0L;
			long ctrlRead		= 0L;
			long framesWritten	= 0L;
//			long framesSkipped	= 0L;
			long writeStart		= 0L;
			long gagaFrame = 0L;
			Fade currentFade	= null;
			final boolean spcOrig = pr.intg[ PR_SPACINGTYPE ] == SPACING_ORIGINAL;
			final long spcFrames = spcOrig ? 0L : (long) (AudioFileDescr.millisToSamples( inStream, pr.para[ PR_SPACING ].val ) + 0.5);
			
//final long spacing = fadeOutFrames; // 0L; // this could be another parameter one day...
			
			while( threadRunning && ctrlRead < ctrlLength ) {
				final int len = (int) Math.min( 8192, ctrlLength - ctrlRead );
				ctrlF.readFrames( ctrlBuf, 0, len );
				
				for( int i = 0; i < len; i++ ) {
					for( int ch = 0; ch < ctrlChanNum; ch++ ) {
						ctrlFrame[ ch ] = ctrlBuf[ ch ][ i ];
					}
					final boolean inTrack = current.fun.thresh( ctrlFrame );
//					boolean advance = current.write;
//					final boolean oldWrite = current.write;
					final long ctrlPos = ctrlRead + i;
					final long inFramePos = (long) (ctrlPos / ctrlRate + 0.5);
					if( inTrack ) {
						current.offCount = 0L; // begin frames-outside-thresh ("off"-track) counting again from zero
if( verbose ) System.out.println( "@" + ctrlPos + " : inTrack = " + inTrack );
					} else {
						if( current.offCount == 0L ) {
							current.offFrame = inFramePos; // store beginning pos of going off-track
						}
						current.offCount++;
if( verbose ) System.out.println( "@" + ctrlPos + " : inTrack = " + inTrack + ", offCount = " + current.offCount + " (" + current.maxCount + ")" );
						if( current.offCount >= current.maxCount ) {  // enough off-track frames to switch track
							if( current.write ) {
//								processTo( inFrame1 );
								currentFade.out( current.offFrame, fadeOutFrames );
								fades.add( currentFade );
//								fades.add( new Fade( inF, inBuf, inFrame1, inFrame1 - framesSkipped, 1f, 0f, fadeOutFrames ));
//								advance = false;
//								gagaFrame += inFramePos - currentFade.inFadeInOffset + spacing;
								gagaFrame += inFramePos - writeStart + spcFrames;
								currentFade = null;
								final long len20 = Math.max( 0L, gagaFrame - fadeInFrames - framesWritten );
								final long len2 = spcOrig ? Math.min( inLength - framesWritten, len20 ) : len20;
if( verbose ) System.out.println( "--> write( " + framesWritten + ", " + len2 + " ; gagaFrame = " + gagaFrame );
								maxAmp = writeFrames( tmpF == null ? outF : tmpF, framesWritten, len2, outBuf, fades, maxAmp, 
										normalized, gain );
								framesWritten += len2;
							} else {
//								writeStart = framesWritten; // inFramePos;
//								final long inFrame1 = Math.max( 0L, (long) (current.offFrame / ctrlRate + 0.5) - fadeInFrames );
								final long inOffset = Math.max( 0L, current.offFrame - fadeInFrames );
								final long fadeIn2  = Math.min( fadeInFrames, current.offFrame - inOffset );
								writeStart = inOffset + fadeIn2;
								if( spcOrig ) {
									final long tmp = inOffset + fadeIn2;
									if( tmp < gagaFrame ) throw new IllegalArgumentException( "Ooops. Assertion" );
									gagaFrame = tmp;
								}
								currentFade = new Fade( inF, inBuf, inOffset, gagaFrame - fadeIn2, fadeIn2 );
//								advance = true;
							}
							current = current.other;
							current.offCount = 0L;
if( verbose ) System.out.println( "--> current = " + current.name );
						}
					}
				}
				ctrlRead += len;
				progOff	 += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			ctrlF.close();
			ctrlF = null;

			long maxNumFrames = 0L;
			if( currentFade != null ) fades.add( currentFade );
			if( spcOrig ) {
				maxNumFrames = inLength - framesWritten;
			} else {
				for( int j = 0; j < fades.size(); j++ ) {
					final Fade f = (Fade) fades.get( j );
					maxNumFrames = Math.max( maxNumFrames, f.remaining( framesWritten ));
				}
			}
if( verbose ) System.out.println( "--> final write( " + framesWritten + ", " + maxNumFrames );
			maxAmp = writeFrames( tmpF == null ? outF : tmpF, framesWritten, maxNumFrames, outBuf, fades, maxAmp, 
					normalized, gain );
			
			inF.close();
			inF = null;

			// adjust gain
			if( normalized ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( tmpF, outF, inBuf, gain, 1.0f );
				deleteTempFile( tmpF );
			}
			setProgression( 1f );

		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
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

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
		if( ctrlF != null ) {
			ctrlF.cleanUp();
		}
	} // process()

// -------- private Methoden --------
	private float writeFrames( AudioFile outF, long pos, long numFrames, float[][] outBuf, List fades, float maxAmp, 
							   boolean normalize, float gain ) throws IOException {
		final long stop = pos + numFrames;
		while( pos < stop ) {
			final int len2 = (int) Math.min( 8192, stop - pos );
			Util.clear( outBuf );
			for( int j = fades.size() - 1; j >= 0; j-- ) {
				final Fade f = (Fade) fades.get( j );
				if( f.remaining( pos ) == 0L ) {
					fades.remove( j );
				} else {
					f.process( outBuf, pos, 0, len2 );
				}
			}
			maxAmp = Math.max( maxAmp, Util.maxAbs( outBuf, 0, len2 ));
			if( !normalize ) {
				Util.mult( outBuf, 0, len2, gain );
			}
			outF.writeFrames( outBuf, 0, len2 );
			pos += len2;
		}
		return maxAmp;
	}
	
	private interface CollapseFunc {
		public float collapse( float[] chan );
	}
	
	private interface ThreshFunc {
		public boolean thresh( float[] chan );
	}
	
	private static class MinFunc implements CollapseFunc {
		public float collapse( float[] chan ) {
			float v = chan[ 0 ];
			for( int i = 0; i < chan.length; i++ ) v = Math.min( v, chan[ i ]);
			return v;
		}
	}

	private static class MaxFunc implements CollapseFunc {
		public float collapse( float[] chan ) {
			float v = chan[ 0 ];
			for( int i = 0; i < chan.length; i++ ) v = Math.max( v, chan[ i ]);
			return v;
		}
	}
	
	private static class AboveFunc implements ThreshFunc {
		private final CollapseFunc collapse;
		private final float thresh;
		
		public AboveFunc( CollapseFunc collapse, float thresh ) {
			this.collapse  = collapse;
			this.thresh		= thresh;
		}
		
		public boolean thresh( float[] chan ) {
			return collapse.collapse( chan ) >= thresh;
		}
	}

	private static class BelowFunc implements ThreshFunc {
		private final CollapseFunc collapse;
		private final float thresh;
		
		public BelowFunc( CollapseFunc collapse, float thresh ) {
			this.collapse  = collapse;
			this.thresh		= thresh;
		}
		
		public boolean thresh( float[] chan ) {
			return collapse.collapse( chan ) < thresh;
		}
	}
	
	private static class Track {
		final String		name;
		final long			maxCount;
		final ThreshFunc	fun;
		final boolean		write;
		long				offCount = 0L;
		long				offFrame;
		Track				other;
		
		public Track( String name, long maxCount, ThreshFunc fun, boolean write ) {
			this.name		= name;
			this.maxCount  	= maxCount;
			this.fun		= fun;
			this.write		= write;
		}
	}
	
	private static class Fade {
		private final AudioFile	af;
		private final float[][]	inBuf;
		private final long		inOffset;	// this is the absolute offset in the input file
		private long			fadeOutOffset = -1L;  // this is the relative offset of the fadeout begin from the chunk start
		private final long		outOffset;
		private final long		fadeInFrames;
		private long			fadeOutFrames;
		private long			fadeOutStop;
//		private long			processed = 0L;
		private final long		fileNumFrames;
//		private final long		maxChunkLen;
		
		public Fade( AudioFile af, float[][] inBuf, long inOffset, long outOffset, long numFrames )
		throws IOException {
			this.af				= af;
			this.inBuf			= inBuf;
			this.inOffset		= inOffset;
			this.outOffset		= outOffset;
			this.fadeInFrames	= numFrames;
			fileNumFrames		= af.getFrameNum();
//			maxChunkLen			= af.getFrameNum() - inOffset;
		}
		
		public void out( long inPos, long numFrames ) {
			if( fadeOutOffset >= 0 ) throw new IllegalStateException( "Duplicate call" );
			if( inPos < inOffset ) throw new IllegalArgumentException( "" + inOffset + " -> " + inPos );
			fadeOutOffset	= inPos - inOffset;
			fadeOutFrames	= numFrames;
			fadeOutStop		= Math.min( fileNumFrames - inOffset, fadeOutOffset + fadeOutFrames );
		}
		
		public long remaining( long outPos ) throws IOException {
			return Math.max( 0L, (fadeOutOffset == -1L ? (fileNumFrames - inOffset) : fadeOutStop) - (outPos - outOffset) );
		}
		
		public void process( float[][] outBuf, long outPos, int off, int len ) throws IOException {
if( verbose ) System.out.println( "process : " + outPos + " / " + off + " / " + len );
			fadeIn( outBuf, outPos, off, len );
			fadeOut( outBuf, outPos, off, len );
		}
		
		private void fadeIn( float[][] outBuf, long outPos, int off, int len ) throws IOException {
			final long start = outPos - outOffset;
			final long add = Math.max( 0L, -start );
			if( add >= len ) return;
			final int addi = (int) add;
			
//			final long pos1		= pos + addi;
			final int off1		= off + addi;
			final int len1		= len - addi;
			final long start1	= start + addi;
			
			final long stop		= fadeOutOffset == -1L ? start1 + len1 : Math.min( fadeOutOffset, start1 + len1 );
			if( stop <= start1 ) return;
			final int len2		= (int) (stop - start1);
			
			final long inPos = inOffset + start1;
			final long add1	 = Math.max( 0L, -inPos );
			if( add >= len2 ) return;
			final int addi1  = (int) add1;
			final int off2		= off1 + addi1;
			final int len3		= len2 - addi1;
			final long start2	= start1 + addi1;
			final long inPos1	= inPos + addi1;
			
//System.out.println( "  start " + start + ", add " + add + ", off1 " + off1 + ", len1 " + len1 + ", start1 " + start1 + ", len2 " + len2 + ", inPos " + inPos );
			if( af.getFramePosition() != inPos1 ) af.seekFrame( inPos1 );
			af.readFrames( inBuf, 0, len3 );
			for( int i = 0, j = off2; i < len3; i++, j++ ) {
				final float f = Math.max( 0f, Math.min( 1f, (float) (start2 + i) / (float) fadeInFrames ));
				for( int ch = 0; ch < outBuf.length; ch++ ) {
					outBuf[ ch ][ j ] += inBuf[ ch ][ i ] * f;
				}
			}
		}

		private void fadeOut( float[][] outBuf, long outPos, int off, int len ) throws IOException {
			if( fadeOutOffset == -1L ) return;
			
			final long start = outPos - (outOffset + fadeOutOffset);
			final long add = Math.max( 0L, -start );
			if( add >= len ) return;
			final int addi = (int) add;
			
//			final long pos1		= pos + addi;
			final int off1		= off + addi;
			final long start1	= start + addi;
			
			final long inPos 	= Math.min( fileNumFrames, inOffset + fadeOutOffset + start1 );
			final int len1		= (int) Math.min( len - addi, fileNumFrames - inPos );
			if( af.getFramePosition() != inPos ) af.seekFrame( inPos );
			af.readFrames( inBuf, 0, len1 );
			for( int i = 0, j = off1; i < len1; i++, j++ ) {
				final float f = Math.max( 0f, Math.min( 1f, 1f - ((float) (start1 + i) / (float) fadeOutFrames) ));
				for( int ch = 0; ch < outBuf.length; ch++ ) {
					outBuf[ ch ][ j ] += inBuf[ ch ][ i ] * f;
				}
			}
		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_SPACING );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_SPACINGTYPE ] == SPACING_FIXED );
		}
	}
}
// class DrMurkeDlg
