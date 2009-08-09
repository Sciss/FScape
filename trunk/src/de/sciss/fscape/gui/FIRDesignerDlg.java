/*
 *  FIRDesignerDlg.java
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
 *		01-Mar-05	bugfix in calcIR()
 *		05-Mar-05	uses VectorPanel for spectrum view;
 *					uses Java 1.2 API (Iterator instead of Enumeration,
 *					ArrayList instead of Vector)
 *		14-Mar-05	support for roll-off (see FilterBox)
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.text.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.*;
import de.sciss.gui.VectorSpace;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;

/**
 *  Processing module for arranging and
 *	combining window'ed sinc filters
 *	that be written out as IR files
 *	to be used in the convolution modules.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 *
 *	@todo		support calculation for minimum phase
 *	@todo		automatically truncate file size if zero samples are at the end (minimum phase)
 */
public class FIRDesignerDlg
extends DocumentFrame
implements VectorPanel.Client
{
// -------- public Variablen --------

	public static final int QUAL_LOW			= 0;	// PR_QUALITY
	public static final int QUAL_MEDIUM			= 1;
	public static final int QUAL_GOOD			= 2;
	public static final int QUAL_VERYGOOD		= 3;

	public static final String[]	QUAL_NAMES		= { "Short", "Medium", "Long", "Very Long" };

	public static final String	MARK_SUPPORT		= "Support";

// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_OUTPUTFILE		= 0;		// pr.text
	private static final int PR_CIRCUIT			= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_OUTPUTRATE		= 2;
	private static final int PR_QUALITY			= 3;
	private static final int PR_WINDOW			= 4;
	private static final int PR_GAINTYPE		= 5;
	private static final int PR_MINPHASE		= 0;		// pr.bool
	private static final int PR_GAIN			= 0;		// pr.para

	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_CIRCUIT		= "Circuit";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputRes";
	private static final String PRN_OUTPUTRATE	= "OutputRate";
	private static final String PRN_QUALITY		= "Quality";
	private static final String PRN_WINDOW		= "Window";
	private static final String PRN_MINPHASE	= "MinPhase";

	private static final String[]	prText		= { "", "03{1;false;1000.0,3;250.0,35;0.0,785;0.0,2;false;5000.0,3;1000.0,35}" };
	private static final String[]	prTextName	= { PRN_OUTPUTFILE, PRN_CIRCUIT };
	private static final int[]		prIntg		= { 0, PathField.SNDRES_32F, 0, QUAL_GOOD, 0, GAIN_UNITY };
	private static final String[]	prIntgName	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE, PRN_QUALITY,
													PRN_WINDOW, PRN_GAINTYPE };
	private static final boolean[]	prBool		= { false };
	private static final String[]	prBoolName	= { PRN_MINPHASE };
	private static final Param[]	prPara		= { null };
	private static final String[]	prParaName	= { PRN_GAIN };

	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;
	private static final int GG_QUALITY			= GG_OFF_CHOICE		+ PR_QUALITY;
	private static final int GG_WINDOW			= GG_OFF_CHOICE		+ PR_WINDOW;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_MINPHASE		= GG_OFF_CHECKBOX	+ PR_MINPHASE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_CIRCUIT			= GG_OFF_OTHER		+ 0;
	private static final int GG_FILTERTYPE		= GG_OFF_OTHER		+ 1;
	private static final int GG_SIGN			= GG_OFF_OTHER		+ 2;
	private static final int GG_CUTOFF			= GG_OFF_OTHER		+ 3;
	private static final int GG_BANDWIDTH		= GG_OFF_OTHER		+ 4;
	private static final int GG_FILTERGAIN		= GG_OFF_OTHER		+ 5;
	private static final int GG_DELAY			= GG_OFF_OTHER		+ 6;
	private static final int GG_OTLIMIT			= GG_OFF_OTHER		+ 7;
	private static final int GG_OTSPACING		= GG_OFF_OTHER		+ 8;
	private static final int GG_OVERTONES		= GG_OFF_OTHER		+ 9;
	private static final int GG_ROLLOFF			= GG_OFF_OTHER		+ 10;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final int CMD_UNKNOWN			= -1;
	private static final int CMD_SELECTED			= 0;
	private static final int CMD_DESELECTED			= 1;
	private static final int CMD_CREATED			= 2;
	private static final int CMD_DELETED			= 3;

	private FilterBox currentFlt = null;

	private static final String	ERR_EMPTY			= "IR length will be zero";
	private static final String	ERR_MISSINGPROP		= "Bug! Missing property!";

	private final MessageFormat msgHertz	= new MessageFormat( "{0,number,0.0} Hz", Locale.US );  // XXX US locale
	private final MessageFormat msgDecibel	= new MessageFormat( "{0,number,0.0} dB", Locale.US );  // XXX US locale
	private final MessageFormat msgPlain	= new MessageFormat( "{0,number,0.000}", Locale.US );  // XXX US locale

	private VectorPanel		spectPane;
	private CircuitPanel	ggCircuit;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public FIRDesignerDlg()
	{
		super( "FIR Designer" );
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
			static_pr.paraName	= prParaName;

			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, -1, PR_OUTPUTRATE );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		PathField			ggOutputFile;
		JComboBox			ggFilterType, ggQuality, ggWindow;
		ParamField			ggCutOff, ggRollOff, ggBandwidth, ggFilterGain, ggDelay, ggOTLimit, ggOTSpacing;
		JCheckBox			ggSign, ggOvertones, ggMinPhase;
		ParamSpace			spcBandwidth[], spcDelay[], spcLimit[];
		JTabbedPane			ggTab;
		Box					box, pageBox;
		CompactPanel		panel;
		Component[]			ggGain;

		gui				= new GUISupport();
		pageBox			= Box.createVerticalBox();

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID		= gui.getItemID( e );
				JComboBox	ch;
				JCheckBox	cb;
				boolean		b		= (currentFlt != null);

				switch( ID ) {
				case GG_FILTERTYPE:
					ch = (JComboBox) e.getSource();
					if( b ) {
						currentFlt.filterType = ch.getSelectedIndex();
						ggCircuit.repaintBox( currentFlt );
						reflectPropertyChanges();
					}
					break;

				case GG_SIGN:
					cb = (JCheckBox) e.getSource();
					if( b ) {
						currentFlt.sign = cb.isSelected();
						ggCircuit.repaintBox( currentFlt );
					}
					break;

				case GG_OVERTONES:
					cb = (JCheckBox) e.getSource();
					if( b ) {
						currentFlt.overtones = cb.isSelected();
						ggCircuit.repaintBox( currentFlt );
						reflectPropertyChanges();
					}
					break;
				}
			}
		};

		ParamListener paramL = new ParamListener() {
			public void paramChanged( ParamEvent e )
			{
				int			ID	= gui.getItemID( e );
				ParamField	pf;
				boolean		b	= (currentFlt != null);

				switch( ID ) {
				case GG_CUTOFF:
					pf = (ParamField) e.getSource();
					if( b ) currentFlt.cutOff = pf.getParam();
					break;

				case GG_BANDWIDTH:
					pf = (ParamField) e.getSource();
					if( b ) currentFlt.bandwidth = pf.getParam();
					break;

				case GG_ROLLOFF:
					pf = (ParamField) e.getSource();
					if( b ) currentFlt.rollOff = pf.getParam();
					break;

				case GG_FILTERGAIN:
					pf = (ParamField) e.getSource();
					if( b ) {
						currentFlt.gain = pf.getParam();
						ggCircuit.repaintBox( currentFlt );
					}
					break;

				case GG_DELAY:
					pf = (ParamField) e.getSource();
					if( b ) {
						currentFlt.delay = pf.getParam();
						ggCircuit.repaintBox( currentFlt );
					}
					break;

				case GG_OTLIMIT:
					pf = (ParamField) e.getSource();
					if( b ) currentFlt.otLimit = pf.getParam();
					break;

				case GG_OTSPACING:
					pf = (ParamField) e.getSource();
					if( b ) currentFlt.otSpacing = pf.getParam();
					break;
				}
			}
		};
		
		ActionListener al = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int			ID	= gui.getItemID( e );
				String		val;
				int			cmd;
				ParamField	pf;
				JComboBox	ch;
				JCheckBox	cb;
				boolean		b	= (currentFlt != null);
				boolean		b2;
				FilterBox	flt;

				switch( ID ) {
				case GG_CIRCUIT:
					val = e.getActionCommand();
					cmd	= CMD_UNKNOWN;
					if( val == CircuitPanel.ACTION_BOXSELECTED ) {
						cmd = CMD_SELECTED;
					} else if( val == CircuitPanel.ACTION_BOXDESELECTED ) {
						cmd = CMD_DESELECTED;
					} else if( val == CircuitPanel.ACTION_BOXCREATED ) {
						cmd = CMD_CREATED;
					} else if( val == CircuitPanel.ACTION_BOXDELETED ) {
						cmd = CMD_DELETED;
					}

					flt = null;
					switch( cmd ) {
					case CMD_SELECTED:
					case CMD_CREATED:
						flt = (FilterBox) ((CircuitPanel) e.getSource()).getActiveBox();
						break;
					}
					
					if( flt == currentFlt ) return;
					currentFlt = flt;

				// ---- display new values ----

					b	= (flt != null);
					if( b ) {
						b2	= true;
//						b3	= true;
						ch	= (JComboBox) gui.getItemObj( GG_FILTERTYPE );
						if( ch != null ) {
							ch.setSelectedIndex( flt.filterType );
							switch( flt.filterType ) {
							case FilterBox.FLT_ALLPASS:
//								b3 = false;
								// THRU
							case FilterBox.FLT_LOWPASS:
							case FilterBox.FLT_HIGHPASS:
								b2 = false;				// !!
								break;
							}
						}
						cb	= (JCheckBox) gui.getItemObj( GG_SIGN );
						if( cb != null ) {
							cb.setSelected( flt.sign );
						}
						pf	= (ParamField) gui.getItemObj( GG_CUTOFF );
						if( pf != null ) {
							pf.setParam( flt.cutOff );
						}
						pf	= (ParamField) gui.getItemObj( GG_BANDWIDTH );
						if( pf != null ) {
							pf.setParam( flt.bandwidth );
						}
						pf	= (ParamField) gui.getItemObj( GG_ROLLOFF );
						if( pf != null ) {
							pf.setParam( flt.rollOff );
						}
						pf	= (ParamField) gui.getItemObj( GG_FILTERGAIN );
						if( pf != null ) {
							pf.setParam( flt.gain );
						}
						pf	= (ParamField) gui.getItemObj( GG_DELAY );
						if( pf != null ) {
							pf.setParam( flt.delay );
						}
						cb	= (JCheckBox) gui.getItemObj( GG_OVERTONES );
						if( cb != null ) {
							cb.setSelected( flt.overtones );
							b2 = (b2 && flt.overtones);		// !!
						}
						pf	= (ParamField) gui.getItemObj( GG_OTLIMIT );
						if( pf != null ) {
							pf.setParam( flt.otLimit );
						}
						pf	= (ParamField) gui.getItemObj( GG_OTSPACING );
						if( pf != null ) {
							pf.setParam( flt.otSpacing );
						}
					}
					reflectPropertyChanges();
					break;
				}
			}
		};

	// -------- I/O-Gadgets --------
		pageBox.add( new GroupLabel( "FIR Output", GroupLabel.ORIENT_HORIZONTAL,
									 GroupLabel.BRACE_NONE ));

		panel			= new CompactPanel();
		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD,
										 "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		gui.registerGadget( ggOutputFile, GG_OUTPUTFILE );
//		ggOutputFile.addActionListener( this );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );
		panel.addGadget( new JLabel( "File name", JLabel.RIGHT ));
		panel.addGadget( ggOutputFile );

		panel.newLine();
		box				= Box.createHorizontalBox();
		ggGain			= createGadgets( GGTYPE_GAIN );
		panel.addGadget( new JLabel( "Gain", JLabel.RIGHT ));
		gui.registerGadget( (ParamField) ggGain[ 0 ], GG_GAIN );
		((ParamField) ggGain[ 0 ]).addParamListener( paramL );
		gui.registerGadget( (JComboBox) ggGain[ 1 ], GG_GAINTYPE );
		((JComboBox) ggGain[ 1 ]).addItemListener( il );
		box.add( ggGain[ 0 ]);
		box.add( ggGain[ 1 ]);
		panel.addGadget( box );
		
		panel.compact();
		pageBox.add( panel );

	// -------- FIR Design-Parameter --------
		pageBox.add( new GroupLabel( "Filter Settings", GroupLabel.ORIENT_HORIZONTAL,
									 GroupLabel.BRACE_NONE ));

		ggCircuit		= new CircuitPanel( new FilterBox() );
//		gui.addGadget( ggCircuit, GG_CIRCUIT );
		ggTab			= new JTabbedPane();
		ggTab.addTab( "Circuit", ggCircuit );
		gui.registerGadget( ggCircuit, GG_CIRCUIT );
		ggCircuit.addActionListener( al );

		spectPane		= new VectorPanel( this, VectorPanel.FLAG_HLOG_GADGET | VectorPanel.FLAG_VLOG_GADGET |
												 VectorPanel.FLAG_UPDATE_GADGET );
		ggTab.addTab( "Mag. spectrum", spectPane );
//		ggTab.addChangeListener( new ChangeListener() {
//			public void stateChanged( ChangeEvent e )
//			{
//				if( ggTab.getSelectedIndex() == 1 ) updateVectorDisplay();
//			}
//		});

		panel			= new CompactPanel();
		box				= Box.createHorizontalBox();
		ggFilterType	= new JComboBox();
		ggFilterType.addItem( "All Pass" );
		ggFilterType.addItem( "Low Pass" );
		ggFilterType.addItem( "High Pass" );
		ggFilterType.addItem( "Band Pass" );
		ggFilterType.addItem( "Band Stop" );
		panel.addGadget( new JLabel( "Type", JLabel.RIGHT ));
		gui.registerGadget( ggFilterType, GG_FILTERTYPE );
		ggFilterType.addItemListener( il );
		box.add( ggFilterType );
		ggSign			= new JCheckBox( "Subtract" );
		gui.registerGadget( ggSign, GG_SIGN );
		ggSign.addItemListener( il );
		box.add( ggSign );
		panel.addGadget( box );
		
		spcBandwidth	= new ParamSpace[ 2 ];
		spcBandwidth[0]	= Constants.spaces[ Constants.offsetHzSpace ];
		spcBandwidth[1]	= Constants.spaces[ Constants.offsetSemitonesSpace ];
		spcLimit		= new ParamSpace[ 3 ];
		spcLimit[0]		= Constants.spaces[ Constants.absHzSpace ];
		spcLimit[1]		= spcBandwidth[ 0 ];
		spcLimit[2]		= spcBandwidth[ 1 ];
		spcDelay		= new ParamSpace[ 2 ];
		spcDelay[0]		= Constants.spaces[ Constants.absMsSpace ];
		spcDelay[1]		= Constants.spaces[ Constants.absBeatsSpace ];

		panel.newLine();
		ggCutOff		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		panel.addGadget( new JLabel( "Cutoff", JLabel.RIGHT ));
		gui.registerGadget( ggCutOff, GG_CUTOFF );
		ggCutOff.addParamListener( paramL );
		panel.addGadget( ggCutOff );

		panel.newLine();
		ggRollOff		= new ParamField( spcBandwidth );
		ggRollOff.setReference( ggCutOff );
		panel.addGadget( new JLabel( "Rolloff", JLabel.RIGHT ));
		gui.registerGadget( ggRollOff, GG_ROLLOFF );
		ggRollOff.addParamListener( paramL );
		panel.addGadget( ggRollOff );

		panel.newLine();
		ggBandwidth		= new ParamField( spcBandwidth );
		ggBandwidth.setReference( ggCutOff );
		panel.addGadget( new JLabel( "Bandwidth", JLabel.RIGHT ));
		gui.registerGadget( ggBandwidth, GG_BANDWIDTH );
		ggBandwidth.addParamListener( paramL );
		panel.addGadget( ggBandwidth );

		panel.newLine();
		ggFilterGain	= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		panel.addGadget( new JLabel( "Gain", JLabel.RIGHT ));
		gui.registerGadget( ggFilterGain, GG_FILTERGAIN );
		ggFilterGain.addParamListener( paramL );
		panel.addGadget( ggFilterGain );

		panel.newLine();
		ggDelay			= new ParamField( spcDelay );
		panel.addGadget( new JLabel( "Delay", JLabel.RIGHT ));
		gui.registerGadget( ggDelay, GG_DELAY );
		ggDelay.addParamListener( paramL );
		panel.addGadget( ggDelay );

		panel.newLine();
		panel.addEmptyColumn();
		ggOvertones		= new JCheckBox( "Add 'overtones'" );
		gui.registerGadget( ggOvertones, GG_OVERTONES );
		ggOvertones.addItemListener( il );
		panel.addGadget( ggOvertones );

		panel.newLine();
		ggOTLimit		= new ParamField( spcLimit );
		ggOTLimit.setReference( ggCutOff );
		panel.addGadget( new JLabel( "Limit freq", JLabel.RIGHT ));
		gui.registerGadget( ggOTLimit, GG_OTLIMIT );
		ggOTLimit.addParamListener( paramL );
		panel.addGadget( ggOTLimit );

		panel.newLine();
		ggOTSpacing		= new ParamField( spcBandwidth );
		ggOTSpacing.setReference( ggCutOff );
		panel.addGadget( new JLabel( "Spacing", JLabel.RIGHT ));
		gui.registerGadget( ggOTSpacing, GG_OTSPACING );
		ggOTSpacing.addParamListener( paramL );
		panel.addGadget( ggOTSpacing );

		panel.compact();
//		splitPane	= new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, ggTab, panel );
//		pageBox.add( splitPane );
box = Box.createHorizontalBox();
box.add( ggTab );
box.add( panel );
pageBox.add( box );

		panel			= new CompactPanel();
		ggWindow		= new JComboBox();
		GUISupport.addItemsToChoice( Filter.getWindowNames(), ggWindow );
		panel.addGadget( new JLabel( "IIR\u2192FIR win", JLabel.RIGHT ));	// unicode 2192 = arrow right
		gui.registerGadget( ggWindow, GG_WINDOW );
		ggWindow.addItemListener( il );
		panel.addGadget( ggWindow );

		ggQuality		= new JComboBox();
		for( int i = 0; i < QUAL_NAMES.length; i++ ) {
			ggQuality.addItem( QUAL_NAMES[ i ]);
		}
		panel.addGadget( new JLabel( "Filter Length", JLabel.RIGHT ));
		gui.registerGadget( ggQuality, GG_QUALITY );
		ggQuality.addItemListener( il );
		panel.addGadget( ggQuality );

		ggMinPhase		= new JCheckBox( "Make minimum phase" );
		gui.registerGadget( ggMinPhase, GG_MINPHASE );
		ggMinPhase.addItemListener( il );
		panel.addGadget( ggMinPhase );
		
		panel.compact();
		pageBox.add( panel );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, pageBox );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
		
		ggCircuit.setCircuit( pr.text[ PR_CIRCUIT ]);
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );

		pr.text[ PR_CIRCUIT ] = ggCircuit.getCircuit();
	}
	
// -------- Processor Interface --------
		
	protected void process()
	{
		long			progOff, progLen;
		float			maxAmp			= 0.0f;
		AudioFile		outF			= null;
		PathField		ggOutput;
		AudioFileDescr	outStream;
		int				outChanNum		= 1;		// fix
		int				outLength, fftLength, complexFFTsize, support;
		float[]			outBuf;
		float[][]		outBufWrap, impBuf;
		Point			impLength;
		float			f1;

		Param			ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		float			gain			= 1.0f;								 		// gain abs amp
		
		java.util.List markers;
		
topLevel: try {
		// ---- open files ----

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr();
			ggOutput.fillStream( outStream );
			outStream.channels = outChanNum;
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- preparations ----

			impLength	= calcLength( ggCircuit, outStream );
			outLength	= impLength.x + impLength.y;
			if( pr.bool[ PR_MINPHASE ]) outLength *= 2;	// account for modified ringing out
			if( outLength <= 0 ) throw new IOException( ERR_EMPTY );

			for( fftLength = 2; fftLength < outLength; fftLength <<= 1 ) ;
			complexFFTsize	= fftLength << 1;

			IOUtil.createEmptyFile( new File( pr.text[ PR_OUTPUTFILE ]));
// System.out.println( "impLength === "+impLength.x+" ... "+impLength.y+" fft "+fftLength );

			progOff		= 0;
			progLen		= (long) outLength * 3;

		// ---- da FIRDesigner ----
	
			outBuf			= new float[ pr.bool[ PR_MINPHASE ] ? complexFFTsize : fftLength + 2 ];
			outBufWrap		= new float[ outChanNum ][];
			for( int ch = 0; ch < outChanNum; ch++ ) {
				outBufWrap[ ch ] = outBuf;
			}
			impBuf			= new float[ 3 ][];
			impBuf[ 0 ]		= outBuf;
			impBuf[ 1 ]		= new float[ 1 ];
			impBuf[ 1 ][ 0 ]= 0.0f;	// time domain
			impBuf[ 2 ]		= new float[ impLength.x ];	// rotate buffer
			
			calcIR( ggCircuit, outStream, impBuf, impLength, fftLength );
			progOff	+= outLength;
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
			if( !threadRunning ) break topLevel;
			
			if( pr.bool[ PR_MINPHASE ]) {
				// need freq domain
				if( impBuf[ 1 ][ 0 ] == 0.0f ) {
					Util.rotate( outBuf, fftLength, impBuf[ 2 ], -impLength.x );	// rotation
					Fourier.realTransform( outBuf, fftLength, Fourier.FORWARD );
					impBuf[ 1 ][ 0 ] = 1.0f;
				}
				// complex log : real' = log(mag); imag' = phase
				Fourier.rect2Polar( outBuf, 0, outBuf, 0, fftLength + 2 );
//				Fourier.unwrapPhases( outBuf, 0, outBuf, 0, fftLength + 2 );
				// calc log(mag)
				for( int i = 0; i <= fftLength; i += 2 ) {
//					outBuf[ i ] = (float) Math.log( Math.max( 1.0e-24, outBuf[ i ]));
					outBuf[ i ] = (float) Math.log( Math.max( 1.0e-48, outBuf[ i ]));
				}
				// make full (complex) spectrum
				for( int i = fftLength + 2, j = fftLength - 2; i < complexFFTsize; j -= 2 ) {
					outBuf[ i++ ] = outBuf[ j ];
					outBuf[ i++ ] = 0.0f;
				}
				// transform to cepstrum domain
				Fourier.complexTransform( outBuf, fftLength, Fourier.INVERSE );
				// fold cepstrum (make anticausal parts causal)
				for( int i = 2, j = complexFFTsize - 2; i < fftLength; i += 2, j -= 2 ) {
					outBuf[ i ]   += outBuf[ j ];		// add conjugate left wing to right wing
					outBuf[ i+1 ] -= outBuf[ j+1 ];
				}
				outBuf[ fftLength + 1 ] = -outBuf[ fftLength + 1 ];
				// clear left wing
				for( int i = fftLength + 2; i < complexFFTsize; i++ ) {
					outBuf[ i ] = 0.0f;
				}
				// back to frequency domain
				Fourier.complexTransform( outBuf, fftLength, Fourier.FORWARD );
				// complex exponential : mag' = exp(real); phase' = imag
				for( int i = 0; i <= fftLength; i += 2 ) {
					outBuf[ i ]		= (float) Math.exp( outBuf[ i ]);
				}
				Fourier.polar2Rect( outBuf, 0, outBuf, 0, fftLength + 2 );
			}

			// make sure we're in the time domain
			if( impBuf[ 1 ][ 0 ] == 1.0f ) {
				Fourier.realTransform( outBuf, fftLength, Fourier.INVERSE );
				Util.rotate( outBuf, fftLength, impBuf[ 2 ], impLength.x );	// undo rotation
				impBuf[ 1 ][ 0 ] = 0.0f;
			}
			progOff	+= outLength;
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
			if( !threadRunning ) break topLevel;

		// ---- normalize output ----

			support	= impLength.x;
			for( int i = 0; i < outLength; i++ ) {
				f1 = Math.abs( outBuf[ i ]);
				if( f1 > maxAmp ) {
					maxAmp = f1;
					if( pr.bool[ PR_MINPHASE ]) support = i;	// empiricially determine support from greatest elongation
				}
			}
			if( pr.bool[ PR_MINPHASE ]) {	// apply window once more to improve stopband atten
				float[] win;
				int winLen;
				
				winLen	= support >> 1;
				win		= Filter.createWindow( winLen, Filter.WIN_KAISER6 );
				for( int i = winLen - 1, j = 0; j < win.length; i--, j++ ) {
					outBuf[ i ] *= win[ j ];
				}
				for( int i = outLength - winLen, j = 0; i < outLength; i++, j++ ) {
					outBuf[ i ] *= win[ j ];
				}
			}
		
			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			} else {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
			}
			for( int i = 0; i < outLength; i++ ) {
				outBuf[ i ] *= gain;
			}
			maxAmp *= gain;
			
		// ---- write output ----

			// add "support"
			markers		= new ArrayList( 1 );
			markers.add( new Marker( support, MARK_SUPPORT ));
			outStream.setProperty( AudioFileDescr.KEY_MARKERS, markers );
			outF = AudioFile.openAsWrite( outStream );

			for( int framesWritten = 0, len = 0; threadRunning && (framesWritten < outLength); ) {
				len				 = Math.min( 8192, outLength - framesWritten );
				outF.writeFrames( outBufWrap, framesWritten, len );
				framesWritten	+= len;
				progOff			+= len;
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
	}

// -------- VectorPanel.Client interface --------

	public void requestUpdate( boolean hlog, boolean vlog )
	{
		int				i, j, decimate = 1, dispLen, fftLength, outLength;
		Point			impLength;
		float[]			outBuf, magBuf;
		float[][]		impBuf;
		float			fMag, max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
		double			decibelWeight	= 20.0 / Constants.ln10;
		double			d2, d3, log, scale, weight, offset, fmin, fmax, fc = 1.0, f0, fRe, fIm;
		VectorSpace		space;
		
		AudioFileDescr	afd			= new AudioFileDescr();
		PathField		ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
		CircuitPanel	cp			= (CircuitPanel) gui.getItemObj( GG_CIRCUIT );
		if( ggOutput == null || cp == null ) return;
		
		fillPropertyArray();
		ggOutput.fillStream( afd );
		afd.channels	= 1;
		
		impLength			= calcLength( cp, afd );
		outLength			= impLength.x + impLength.y;
//		outStream.length= outLength;
		if( outLength <= 0 ) return;

		for( fftLength = 2; fftLength < outLength; fftLength <<= 1 ) ;
		if( hlog ) {	// oversampling in log.freq mode because lo freq are spread
			for( ; fftLength < 8192; fftLength <<= 1, impLength = new Point( impLength.x * 2, impLength.y * 2 )) ;
		} else {	// fft size min. 4096, decimation so displayed vector is not greater than 4096
			for( ; fftLength < 4096; fftLength <<= 1, impLength = new Point( impLength.x * 2, impLength.y * 2 )) ;
			for( i = 4096; i < fftLength; i <<= 1, decimate++ ) ;
		}
	
//		outBuf			= new float[ fftLength + 4 ];	// + 2 extra safety bound for log.freq interpolation!
		outBuf			= new float[ fftLength + 2 ];
		impBuf			= new float[ 3 ][];
		impBuf[ 0 ]		= outBuf;
		impBuf[ 1 ]		= new float[ 1 ];
		impBuf[ 1 ][ 0 ]= 0.0f;	// time domain
		impBuf[ 2 ]		= new float[ impLength.x ];	// rotate buffer
		
		calcIR( cp, afd, impBuf, impLength, fftLength );
		if( impBuf[ 1 ][ 0 ] != 1.0f ) {
			Fourier.realTransform( outBuf, fftLength, Fourier.FORWARD );
		}
		
		impBuf			= null;

// System.err.println( "length = "+fftLength );

// Random r = new Random( System.currentTimeMillis() );
	
		if( hlog ) {
			dispLen		= 2049; // 4097;
			magBuf		= new float[ dispLen ];
			fc			= 1000.0;		// gewuenschte centerfreq
			fmin		= 16.0;			// gewuenschte min. freq
			fmax		= afd.rate/2;	// oberste freq.
			f0			= fc*fc / fmax;
			log			= Math.log( fmax/f0 );
			offset		= Math.log( fmin/f0 ) / log;
			weight		= (1.0 - offset) / (dispLen - 1);
			scale		= f0 / afd.rate * fftLength;
// System.err.println( "fmax = "+fmax+"; f0 = "+f0+"; log = "+log+"; offset = "+offset+"; weight = "+weight+"; fftSize = "+fftLength );
		
			for( j = 0; j < dispLen; j++ ) {
				d2				= Math.exp( (j * weight + offset) * log );
// System.err.println( "freq = "+d2*f0 );
				d3				= d2 * scale;
// System.err.println( "bin = "+d3 );
				i				= ((int) d3) << 1;
				d2				= d3 % 1.0;
//				fRe				= outBuf[ i ] * (1.0 - d2) + outBuf[ i+2 ] * d2;
//				fIm				= outBuf[ i+1 ] * (1.0 - d2) + outBuf[ i+3 ] * d2;
				fRe				= outBuf[ i ];
				fIm				= outBuf[ i+1 ];
				d3				= Math.sqrt( fRe*fRe + fIm*fIm ) * (1.0 - d2);
				if( i < fftLength ) {
					fRe			= outBuf[ i+2 ];
					fIm			= outBuf[ i+3 ];
					fMag		= (float) (d3 + Math.sqrt( fRe*fRe + fIm*fIm ) * d2);
				} else {
					fMag		= (float) d3;
				}
				magBuf[ j ]		= fMag;
				if( fMag > max ) max = fMag;
				if( fMag < min ) min = fMag;
			}
		} else {

			dispLen		= (fftLength >> decimate) + 1;
			magBuf		= new float[ dispLen ];
		
// System.err.println( "dispLen = "+dispLen+"; decimate = "+decimate+"; fftLength "+fftLength+"; 1 << decimate "+(1 << decimate) );
			for( i = 0, j = 0, decimate = 1 << decimate; i <= fftLength; i += decimate, j++ ) {
				fRe				= outBuf[ i ];
				fIm				= outBuf[ i+1 ];
				fMag			= (float) Math.sqrt( fRe*fRe + fIm*fIm );
				magBuf[ j ]		= fMag;
				if( fMag > max ) max = fMag;
				if( fMag < min ) min = fMag;
			}

			fmin		= 0.0;
			fmax		= afd.rate/2;
		}
		if( vlog ) {
			for( j = 0; j < dispLen; j++ ) {
				magBuf[ j ] = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, magBuf[ j ])));
			}
			min = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, min )));
			max = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, max )));
//System.err.println( "min = "+min+"; max = "+max );
			max = (float) (Math.ceil( max / 6.0 ) * 6.0);
			min = (float) (Math.ceil( min / 6.0 ) * 6.0);
//System.err.println( "now min = "+min+"; max = "+max );
		} else {
//System.err.println( "min = "+min+"; max = "+max );
			max = (float) (Math.ceil( max * 10.0 ) / 10.0);
			min = (float) (Math.floor( min * 10.0 ) / 10.0);
//System.err.println( "now min = "+min+"; max = "+max );
		}
		
		outBuf			= null;

		if( hlog ) {
			space = VectorSpace.createLogLinSpace( fmin, fmax, fc, min, max, null, null, null, null );
		} else {
			space = VectorSpace.createLinSpace( fmin, fmax, min, max, null, null, null, null );
		}
		spectPane.setSpace( space );
		spectPane.setVector( magBuf );
	}

	public String formatHText( double x, boolean hlog )
	{
		return( msgHertz.format( new Object[] { new Double( x )}));
	}

	public String formatVText( double y, boolean vlog )
	{
		if( vlog ) {
			return( msgDecibel.format( new Object[] { new Double( y )}));
		} else {
			return( msgPlain.format( new Object[] { new Double( y )}));
		}
	}
				
// -------- private Methoden --------


	/*
	 *	Impulslaenge in Samples berechnen
	 *
	 *	@return	Point.x ist zeitlich negativer Anteil ("support"), Point.y positiver
	 */
	private Point calcLength( CircuitPanel cp, AudioFileDescr outStream )
	{
		Iterator	iter		= cp.getElements();
		Point		totalLength	= new Point( 0, 0 );
		Point		len;
		Object		o;

		if( iter.hasNext() ) {
			o = iter.next();
			if( o instanceof CircuitPanel ) {
				totalLength = calcLength( (CircuitPanel) o, outStream );	// recurse
			} else if( o instanceof FilterBox ) {
				totalLength = ((FilterBox) o).calcLength( outStream, pr.intg[ PR_QUALITY ]);
			} else {
				assert false : o.getClass().getName();
			}
		}
				
		while( iter.hasNext() ) {
			o = iter.next();
			if( o instanceof CircuitPanel ) {
				len = calcLength( (CircuitPanel) o, outStream );	// recurse
			} else if( o instanceof FilterBox ) {
				len = ((FilterBox) o).calcLength( outStream, pr.intg[ PR_QUALITY ]);
			} else {
				assert false : o.getClass().getName();
				len = null;
			}

			if( cp.getType() == CircuitPanel.TYPE_PARALLEL ) {
				totalLength.x = Math.max( totalLength.x, len.x );
				totalLength.y = Math.max( totalLength.y, len.y );
			} else {
				totalLength.x += len.x;
				totalLength.y += Math.max( 0, len.y - 1 );
			}
		}
		return totalLength;
	}

	/*
	 *	IR berechnen
	 *
	 *	@param	buf1		erste Dimension die Daten, zweite Dimension 1. Feld = 0 fuer timedomain, 1 fuer fft
	 *						dritte Dimension Rotations-Puffer (totalLength.x gross)
	 *						buf1[ 0 ].length must >= fftLength + 2 !!
	 */
	private void calcIR( CircuitPanel cp, AudioFileDescr outStream, float[][] buf1, Point totalLength, int fftLength )
	{
		Iterator	iter		= cp.getElements();
		Object		o;
		float[][]	buf2		= null;
		float[]		convBuf1, convBuf2;
		int			i;

		if( iter.hasNext() ) {
			o = iter.next();
			if( o instanceof CircuitPanel ) {
				calcIR( (CircuitPanel) o, outStream, buf1, totalLength, fftLength );	// recurse
			} else if( o instanceof FilterBox ) {
				((FilterBox) o).calcIR( outStream, pr.intg[ PR_QUALITY ], pr.intg[ PR_WINDOW ], buf1[ 0 ], totalLength );
			} else {
				assert false : o.getClass().getName();
			}
		}
				
		while( iter.hasNext() ) {
			o = iter.next();
			if( buf2 == null ) {
				buf2			= new float[ 3 ][];
				buf2[ 0 ]		= new float[ buf1[ 0 ].length ];
				buf2[ 1 ]		= new float[ 1 ];
				buf2[ 2 ]		= buf1[ 2 ];
			}
			buf2[ 1 ][ 0 ]	= 0.0f;	// time domain

			if( o instanceof CircuitPanel ) {
				calcIR( (CircuitPanel) o, outStream, buf2, totalLength, fftLength );	// recurse
			} else if( o instanceof FilterBox ) {
				((FilterBox) o).calcIR( outStream, pr.intg[ PR_QUALITY ], pr.intg[ PR_WINDOW ], buf2[ 0 ], totalLength );
			} else {
				assert false : o.getClass().getName();
			}

			convBuf1	= buf1[ 0 ];
			convBuf2	= buf2[ 0 ];
			if( buf1[ 1 ][ 0 ] == buf2[ 1 ][ 0 ] ) {	// same domain
				if( (cp.getType() == CircuitPanel.TYPE_SERIAL) && (buf1[ 1 ][ 0 ] == 0.0f) ) {	// need 2 ffts
					Util.rotate( convBuf1, fftLength, buf1[ 2 ], -totalLength.x );
					Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
					Util.rotate( convBuf2, fftLength, buf1[ 2 ], -totalLength.x );
					Fourier.realTransform( convBuf2, fftLength, Fourier.FORWARD );
					buf1[ 1 ][ 0 ] = 1.0f;	// freq domain
					buf2[ 1 ][ 0 ] = 1.0f;
				}
			} else {
				if( buf1[ 1 ][ 0 ] == 0.0f ) {
					Util.rotate( convBuf1, fftLength, buf1[ 2 ], -totalLength.x );
					Fourier.realTransform( convBuf1, fftLength, Fourier.FORWARD );
					buf1[ 1 ][ 0 ] = 1.0f;	// freq domain
				} else {
					Util.rotate( convBuf2, fftLength, buf1[ 2 ], -totalLength.x );
					Fourier.realTransform( convBuf2, fftLength, Fourier.FORWARD );
					buf2[ 1 ][ 0 ] = 1.0f;	// freq domain
				}
			}

			if( cp.getType() == CircuitPanel.TYPE_PARALLEL ) {
				for( i = fftLength + 1; i >= 0; i-- ) {
					convBuf1[ i ] += convBuf2[ i ];				// parallel adds up
				}
			} else {
				for( i = fftLength + 1; i >= 0; i-- ) {
					convBuf1[ i ] *= convBuf2[ i ];				// serial is convolution
				}
			}
		}
	}

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component	c;
		FilterBox	flt	= currentFlt;
		boolean		b	= (flt != null);
		boolean		b2	= true;
		boolean		b3	= true;

		c = gui.getItemObj( GG_FILTERTYPE );
		if( c != null ) {
			c.setEnabled( b );
		}
		c = gui.getItemObj( GG_SIGN );
		if( c != null ) {
			c.setEnabled( b );
		}
		c = gui.getItemObj( GG_FILTERGAIN );
		if( c != null ) {
			c.setEnabled( b );
		}
		c = gui.getItemObj( GG_DELAY );
		if( c != null ) {
			c.setEnabled( b );
		}

		if( b ) {
			switch( flt.filterType ) {
			case FilterBox.FLT_ALLPASS:
				b3 = false;
				// THRU
			case FilterBox.FLT_LOWPASS:
			case FilterBox.FLT_HIGHPASS:
				b2 = false;				// !!
				break;
			}
		}
		c = gui.getItemObj( GG_CUTOFF );
		if( c != null ) {
			c.setEnabled( b && b3 );
		}
		c = gui.getItemObj( GG_ROLLOFF );
		if( c != null ) {
			c.setEnabled( b && b3 );
		}
		c = gui.getItemObj( GG_BANDWIDTH );
		if( c != null ) {
			c.setEnabled( b && b2 );
		}
		c = gui.getItemObj( GG_OVERTONES );
		if( c != null ) {
			c.setEnabled( b && b2 );
			if( b ) {
				b2 = (b2 && flt.overtones);		// !!
			}
		}
		c = gui.getItemObj( GG_OTLIMIT );
		if( c != null ) {
			c.setEnabled( b && b2 );
		}
		c = gui.getItemObj( GG_OTSPACING );
		if( c != null ) {
			c.setEnabled( b && b2 );
		}
	}
}
// class FIRDesignerDlg
