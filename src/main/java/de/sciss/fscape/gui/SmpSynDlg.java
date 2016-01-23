/*
 *  SmpSynDlg.java
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
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.GUIUtil;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.Region;

/**
 *	This processing module was inspired by a software
 *	called MetaSynth. It uses a pvoc file and instead
 *	of a sine oscillator bank uses a collection of sound
 *	files which get resampled and attenuated according
 *	to the pvoc data. Hell slow, rarely but sometimes
 *	interesting.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class SmpSynDlg
extends ModulePanel
{
// -------- public Variablen --------

// -------- private Variablen --------

	private static final int QUALITY_LOW		= 0;	// linear interp. + mono
	private static final int QUALITY_MID		= 1;	// linear interp.
	private static final int QUALITY_HIGH		= 2;	// bandlimited interp.
	
	// Fehlermeldungen
	private static final String	ERR_NOZONES		= "No sample zones defined";

	private static final double	MIDFREQ	= 523.2511306;	// C5 = Mittenfrequenz (Hz)

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private GUISupport	sgui	= null;	// settings

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_SMPMAP			= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_OUTPUTRATE		= 2;
	private static final int PR_QUALITY			= 3;
	private static final int PR_NOISEFLOOR		= 0;		// pr.para
	private static final int PR_TRIGTHRESH		= 1;
	private static final int PR_GAIN			= 2;
	private static final int PR_LENGTH			= 3;

	private static final String PRN_INPUTFILE	= "InputFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_SMPMAP		= "SampleMap";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";
	private static final String PRN_OUTPUTRATE	= "OutputRate";
	private static final String PRN_QUALITY		= "Quality";
	private static final String PRN_NOISEFLOOR	= "NoiseFloor";
	private static final String PRN_TRIGTHRESH	= "TrigThresh";
	private static final String PRN_LENGTH		= "Length";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_SMPMAP };
	private static final int		prIntg[]	= { 0, 0, 0, QUALITY_MID };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE, PRN_QUALITY };
	private static final Param	prPara[]		= { null, null, null, null };
	private static final String	prParaName[]	= { PRN_NOISEFLOOR, PRN_TRIGTHRESH,
													PRN_GAIN, PRN_LENGTH };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;

	private static final int GGS_SMPMAP			= GG_OFF_OTHER		+ 0;
	private static final int GGS_NOISEFLOOR		= GG_OFF_OTHER		+ 1;
	private static final int GGS_TRIGTHRESH		= GG_OFF_OTHER		+ 2;
	private static final int GGS_SMPHIFREQ		= GG_OFF_OTHER		+ 3;
	private static final int GGS_SMPLOFREQ		= GG_OFF_OTHER		+ 4;
	private static final int GGS_SMPHIVEL		= GG_OFF_OTHER		+ 5;
	private static final int GGS_SMPLOVEL		= GG_OFF_OTHER		+ 6;
	private static final int GGS_SMPFILE		= GG_OFF_OTHER		+ 7;
	private static final int GGS_SMPPHASE		= GG_OFF_OTHER		+ 8;
	private static final int GGS_SMPGAIN		= GG_OFF_OTHER		+ 9;
	private static final int GGS_SMPBASE		= GG_OFF_OTHER		+ 10;
	private static final int GGS_SMPLOOP		= GG_OFF_OTHER		+ 11;
	private static final int GGS_SMPATK			= GG_OFF_OTHER		+ 12;
	private static final int GGS_SMPRLS			= GG_OFF_OTHER		+ 13;
	private static final int GGS_GAIN			= GG_OFF_OTHER		+ 14;
	private static final int GGS_LENGTH			= GG_OFF_OTHER		+ 15;
	private static final int GGS_QUALITY		= GG_OFF_OTHER		+ 16;

	private static final int GG_SETTINGS		= GG_OFF_OTHER		+ 17;

	// das "Instrument"
	private	Vector	samples;			// SmpZones
	private	SmpZone	currentSmp	= null;	// angewaehltes Sample

	// uebersetzung von SmpMapPanel's ACTION-Strings
	private static final int CMD_UNKNOWN		= -1;
	private static final int CMD_SELECTED		= 0;
	private static final int CMD_DESELECTED	= 1;
	private static final int CMD_CREATED		= 2;
	private static final int CMD_DELETED		= 3;
	private static final int CMD_CHANGED		= 4;
	private static final int CMD_SPACE		= 5;

	// bandlimited interp
	private float	flt[];
	private float fltD[];
	private int	fltSmpPerCrossing;

	private	SmpMapPanel			ggSmpMap;

	private ParamListener paramL;
	private ItemListener il;
	private PathListener pathL;
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SmpSynDlg()
	{
		super( "Sample Synthesis" );
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
			static_pr.para[ PR_NOISEFLOOR ]	= new Param(   -60.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_TRIGTHRESH ]	= new Param(   +12.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_GAIN ]		= new Param(    -6.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_LENGTH ]		= new Param(   100.0, Param.FACTOR_TIME );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES, PR_OUTPUTRATE );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- Var init --------

		samples	= new Vector();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField	ggInputFile, ggOutputFile;
		PathField[] ggInputs;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );
		
		paramL = new ParamListener() {
			public void paramChanged( ParamEvent e )
			{
				int			ID		= gui.getItemID( e );
//				String		cmd		= e.getActionCommand();

				Component	gg;
				SmpZone		smp;
				ParamSpace	hSpace;

				switch( ID ) {
				case -1:							// -------------------- Settings GUI --------------------
					if( sgui == null ) return;
					ID = sgui.getItemID( e );
					
					switch( ID ) {
					case GGS_NOISEFLOOR:
						gg = sgui.getItemObj( GGS_SMPMAP );
						if( gg != null ) {
							hSpace		= new ParamSpace( ((SmpMapPanel) gg).getHSpace() );
//							hSpace.min	= ((ParamField) e.getSource()).getParam().val;
							hSpace		= new ParamSpace( ((ParamField) e.getSource()).getParam().val, hSpace.max, hSpace.inc, hSpace.unit );
							((SmpMapPanel) gg).setSpaces( hSpace, null );
						}
						break;
					
					case GGS_TRIGTHRESH:
						// nothing
						break;
					
					case GGS_SMPHIFREQ:
					case GGS_SMPLOFREQ:
					case GGS_SMPHIVEL:
					case GGS_SMPLOVEL:
					case GGS_SMPBASE:
						if( currentSmp != null ) {
							smp = (SmpZone) currentSmp.clone();
							switch( ID ) {
							case GGS_SMPHIFREQ:
								smp.freqHi	= ((ParamField) e.getSource()).getParam();
								break;
							case GGS_SMPLOFREQ:
								smp.freqLo	= ((ParamField) e.getSource()).getParam();
								break;
							case GGS_SMPHIVEL:
								smp.velHi	= ((ParamField) e.getSource()).getParam();
								break;
							case GGS_SMPLOVEL:
								smp.velLo	= ((ParamField) e.getSource()).getParam();
								break;
							case GGS_SMPBASE:
								smp.base	= ((ParamField) e.getSource()).getParam();
								currentSmp.base = smp.base;
								break;
							}
							gg = sgui.getItemObj( GGS_SMPMAP );
							if( gg != null ) {
								if( !((SmpMapPanel) gg).setSample( smp )) {		// invokes actionPerformed() if successfull
									((SmpMapPanel) gg).setSample( currentSmp );	// restore old values if not successfull
								}
							}
						}
						break;
					
					case GGS_SMPGAIN:
						if( currentSmp != null ) {
							currentSmp.gain	= ((ParamField) e.getSource()).getParam();
						}
						break;
					
					case GGS_SMPATK:
						if( currentSmp != null ) {
							currentSmp.atk	= ((ParamField) e.getSource()).getParam();
						}
						break;

					case GGS_SMPRLS:
						if( currentSmp != null ) {
							currentSmp.rls	= ((ParamField) e.getSource()).getParam();
						}
						break;
					}
					break;
				}
			}
		};
		
		pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int			ID		= gui.getItemID( e );

				Component	gg;

				switch( ID ) {
				case -1:							// -------------------- Settings GUI --------------------
					if( sgui == null ) return;
					ID = sgui.getItemID( e );
					
					switch( ID ) {
						case GGS_SMPFILE:
						if( currentSmp != null ) {
							currentSmp.fileName	= ((PathField) e.getSource()).getPath().getPath();
							setSample( currentSmp.fileName );
							gg = sgui.getItemObj( GGS_SMPMAP );
							if( gg != null ) {
								((SmpMapPanel) gg).setSample( currentSmp );
							}
						}
						break;
					}
					break;
				}
			}
		};
		
		ActionListener al = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int			ID		= gui.getItemID( e );
				String		cmd		= e.getActionCommand();
				int			cmdID;

				ParamField	pf;
				Component	gg;
				SmpZone		smp;
				int			smpID	= -1;
				boolean		fullRefresh;
				ParamSpace	hSpaces[], vSpaces[];
				SmpMap		smpMap;
				Enumeration	smpEnum;
				SmpZone		newSmp;
				Vector		newSamples;

				switch( ID ) {
				case -1:							// -------------------- Settings GUI --------------------
					if( sgui == null ) return;
					ID = sgui.getItemID( e );
					
					switch( ID ) {
					case GGS_SMPMAP:

						cmdID = CMD_UNKNOWN;
						if( cmd.startsWith( SmpMapPanel.ACTION_BOXSELECTED )) {
							cmdID = CMD_SELECTED;
						} else if( cmd.startsWith( SmpMapPanel.ACTION_BOXCHANGED )) {
							cmdID = CMD_CHANGED;
						} else if( cmd.startsWith( SmpMapPanel.ACTION_BOXCREATED )) {
							cmdID = CMD_CREATED;
						} else if( cmd.startsWith( SmpMapPanel.ACTION_BOXDESELECTED )) {
							cmdID = CMD_DESELECTED;
						} else if( cmd.startsWith( SmpMapPanel.ACTION_BOXDELETED )) {
							cmdID = CMD_DELETED;
						} else if( cmd.startsWith( SmpMapPanel.ACTION_SPACECHANGED )) {
							cmdID = CMD_SPACE;
						}

						try {
							smpID = Integer.parseInt( cmd.substring( 3 ));
						} catch( NumberFormatException e99 ) {
							cmdID = CMD_UNKNOWN;
						}

						switch( cmdID ) {
						case CMD_SELECTED:
						case CMD_CHANGED:
						case CMD_CREATED:

						// ---- load current SmpZone ----
							smp = ((SmpMapPanel) e.getSource()).getSample( smpID );
							if( smp == null ) {
								GUIUtil.displayError( getComponent(), new IllegalStateException( ERR_CORRUPTED ), String.valueOf( smpID ));
								return;
							}

							fullRefresh = (cmdID != CMD_CHANGED);
							
							if( cmdID == CMD_CREATED ) {
							
								currentSmp			= smp;	// smp is already a clone, we can keep it
								currentSmp.fileName	= "";	// sonst NullPtrExc
								samples.addElement( currentSmp );
							}

							// find the sample
							if( (currentSmp == null) || (currentSmp.uniqueID != smp.uniqueID) ) {

								for( int i = 0; i < samples.size(); i++ ) {
									currentSmp = (SmpZone) samples.elementAt( i );
									if( currentSmp.uniqueID == smpID ) break;
								}
								if( (currentSmp == null) || (currentSmp.uniqueID != smpID) ) {
									GUIUtil.displayError( getComponent(), new NoSuchElementException( ERR_CORRUPTED ), String.valueOf( smpID ));
									return;
								}

								fullRefresh	= true;
							}

							// copy changed values
							if( cmdID == CMD_CHANGED ) {		// means Freq/Vel-Range
							
								currentSmp.freqLo	= smp.freqLo;		// Params have been cloned already
								currentSmp.freqHi	= smp.freqHi;
								currentSmp.velLo	= smp.velLo;
								currentSmp.velHi	= smp.velHi;
							}
							
						// ---- display new values ----

							pf = (ParamField) sgui.getItemObj( GGS_SMPHIFREQ );
							if( pf != null ) pf.setParam( currentSmp.freqHi );
							pf = (ParamField) sgui.getItemObj( GGS_SMPLOFREQ );
							if( pf != null ) pf.setParam( currentSmp.freqLo );
							pf = (ParamField) sgui.getItemObj( GGS_SMPHIVEL );
							if( pf != null ) pf.setParam( currentSmp.velHi );
							pf = (ParamField) sgui.getItemObj( GGS_SMPLOVEL );
							if( pf != null ) pf.setParam( currentSmp.velLo );

							// these only when switched to a different zone
							if( fullRefresh ) {
							
								gg = sgui.getItemObj( GGS_SMPFILE );
								if( gg != null ) ((PathField) gg).setPath( new File( currentSmp.fileName ));
								gg = sgui.getItemObj( GGS_SMPLOOP );
								if( gg != null ) ((JCheckBox) gg).setSelected( (currentSmp.flags & SmpZone.LOOP) != 0 );
								gg = sgui.getItemObj( GGS_SMPPHASE );
								if( gg != null ) ((JComboBox) gg).setSelectedIndex( currentSmp.flags & SmpZone.PHASEMASK );
								pf = (ParamField) sgui.getItemObj( GGS_SMPGAIN );
								if( pf != null ) pf.setParam( currentSmp.gain );
								pf = (ParamField) sgui.getItemObj( GGS_SMPBASE );
								if( pf != null ) pf.setParam( currentSmp.base );
								pf = (ParamField) sgui.getItemObj( GGS_SMPATK );
								if( pf != null ) pf.setParam( currentSmp.atk );
								pf = (ParamField) sgui.getItemObj( GGS_SMPRLS );
								if( pf != null ) pf.setParam( currentSmp.rls );
							}
							break;

						case CMD_DELETED:
							for( int i = 0; i < samples.size(); i++ ) {
								currentSmp = (SmpZone) samples.elementAt( i );
								if( currentSmp.uniqueID == smpID ) break;
							}
							if( (currentSmp == null) || (currentSmp.uniqueID != smpID) ) {
								GUIUtil.displayError( getComponent(), new NoSuchElementException( ERR_CORRUPTED ), String.valueOf( ID ));
								return;
							}
							samples.removeElement( currentSmp );
							// THRU
						case CMD_DESELECTED:
							if( currentSmp != null ) {
								currentSmp = null;

								gg = sgui.getItemObj( GGS_SMPFILE );
								if( gg != null ) ((PathField) gg).setPath( new File( "(no sample zone selected)" ));
							}
							break;

						case CMD_SPACE:
							smpMap		= ((SmpMapPanel) e.getSource()).getSmpMap();
							smpEnum		= smpMap.getSamples();
							newSamples	= new Vector();
							currentSmp	= null;
							while( smpEnum.hasMoreElements() ) {
								smp = (SmpZone) smpEnum.nextElement();
								for( int i = 0; i < samples.size(); i++ ) {
									newSmp = (SmpZone) samples.elementAt( i );
									if( newSmp.uniqueID == smp.uniqueID ) {
										newSmp.freqLo	= (Param) smp.freqLo.clone();
										newSmp.freqHi	= (Param) smp.freqHi.clone();
										newSmp.velLo	= (Param) smp.velLo.clone();
										newSmp.velHi	= (Param) smp.velHi.clone();
										newSamples.addElement( newSmp );
										break;
									}
								}
							}
							samples = newSamples;
							
							vSpaces		= new ParamSpace[ 1 ];
							vSpaces[ 0 ]= ((SmpMapPanel) e.getSource()).getVSpace();
							hSpaces		= new ParamSpace[ 1 ];
							hSpaces[ 0 ]= ((SmpMapPanel) e.getSource()).getHSpace();
							gg			= sgui.getItemObj( GGS_SMPHIFREQ );
							if( gg != null ) {
								((ParamField) gg).setSpaces( vSpaces );
							}
							gg			= sgui.getItemObj( GGS_SMPLOFREQ );
							if( gg != null ) {
								((ParamField) gg).setSpaces( vSpaces );
							}
							gg			= sgui.getItemObj( GGS_SMPLOVEL );
							if( gg != null ) {
								((ParamField) gg).setSpaces( hSpaces );
							}
							gg			= sgui.getItemObj( GGS_SMPHIVEL );
							if( gg != null ) {
								((ParamField) gg).setSpaces( hSpaces );
							}
							break;

						case CMD_UNKNOWN:
							GUIUtil.displayError( getComponent(), new NoSuchMethodException( ERR_CORRUPTED ), String.valueOf( ID ));
							return;
						}
						break;
					}
					break;
				}
			}
		};	
		
		il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
//				Component		associate;
				int				ID			= gui.getItemID( e );
//				int				ID2;
//				String			name;
//				PropertyArray	pa;
//				Properties		preset;

				switch( ID ) {
				case -1:							// -------------------- Settings GUI --------------------
					if( sgui == null ) return;
					ID = sgui.getItemID( e );
					
					switch( ID ) {
					case GGS_SMPPHASE:
						if( currentSmp != null ) {
							currentSmp.flags &= ~SmpZone.PHASEMASK;
							currentSmp.flags |= ((JComboBox) e.getSource()).getSelectedIndex() & SmpZone.PHASEMASK;
						}
						break;
					
					case GGS_SMPLOOP:
						if( currentSmp != null ) {
							if( ((JCheckBox) e.getSource()).isSelected() ) {
								currentSmp.flags |=  SmpZone.LOOP;
							} else {
								currentSmp.flags &= ~SmpZone.LOOP;
							}
						}
						break;
					}
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
										 "Select spectral file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SPECT );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD,
										 "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Syn$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );
		
	// -------- Synthesis-Parameter --------
	gui.addLabel( new JLabel( "Synthesis settings", SwingConstants.CENTER ));

		con.fill		= GridBagConstraints.BOTH;
//		ggSettings		= new JScrollPane(); // ( ScrollPane.SCROLLBARS_AS_NEEDED );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		
		sgui			= createGUI();
//		ggSettings.setViewportView( sgui );
		
//		gui.addScrollPane( ggSettings, GG_SETTINGS, this );
gui.addGadget( sgui, GG_SETTINGS );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
		
		ggSmpMap.addActionListener( al );
	}

	/*
	 *	Settings-GUI
	 */
	protected GUISupport createGUI()
	{
		GUISupport			sg;
		GridBagConstraints	con;

		ParamField			ggNoiseFloor, ggTriggerThresh;
		ParamField			ggSmpLoFreq, ggSmpHiFreq, ggSmpLoVel, ggSmpHiVel;	// Freq + Velocity bounds
		PathField			ggSmpFile;
		JCheckBox			ggSmpLoop;
		JComboBox			ggSmpPhase, ggQuality;
		ParamField			ggSmpGain, ggSmpBase, ggSmpAtk, ggSmpRls;
		ParamField			ggGain, ggLength;

		ParamSpace			ampSpace, noiseSpace, trigSpace;
		ParamSpace			freqSpace;
		ParamSpace			lengthSpaces[]	= { Constants.spaces[ Constants.absMsSpace ],
												Constants.spaces[ Constants.absBeatsSpace ],
												Constants.spaces[ Constants.factorTimeSpace ]};
		SmpMap				smpMap;

		sg			= new GUISupport();
		con				= sg.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		con.fill		= GridBagConstraints.BOTH;
		ggSmpMap		= new SmpMapPanel();
		con.gridwidth	= 3;
		con.gridheight	= 8;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		sg.addGadget( ggSmpMap, GGS_SMPMAP );

		ampSpace		= new ParamSpace( Constants.spaces[ Constants.decibelAmpSpace ]);
		noiseSpace		= new ParamSpace( ampSpace );
		trigSpace		= new ParamSpace( ampSpace );
//		ampSpace.min	= pr.para[ PR_NOISEFLOOR ].val;
//		ampSpace.max	= 0.0;
		ampSpace		= new ParamSpace( pr.para[ PR_NOISEFLOOR ].val, 0.0, ampSpace.inc, ampSpace.unit );
//		noiseSpace.max	= -12.0;
		noiseSpace		= new ParamSpace( noiseSpace.min, -12.0, noiseSpace.inc, noiseSpace.unit );
//		trigSpace.min	= 1.0;
//		trigSpace.max	= 96.0;
		trigSpace		= new ParamSpace( 1.0, 96.0, trigSpace.inc, trigSpace.unit );
		freqSpace		= new ParamSpace( Constants.spaces[ Constants.absHzSpace ]);
//		freqSpace.max	= freqSpace.fitValue( 22050.0 );
//		freqSpace.min	= freqSpace.fitValue( MIDFREQ*MIDFREQ / freqSpace.max );
final double maxNew = freqSpace.fitValue( 22050.0 );
		freqSpace		= new ParamSpace( freqSpace.fitValue( MIDFREQ*MIDFREQ / maxNew ), maxNew, freqSpace.inc, freqSpace.unit );
		smpMap			= new SmpMap( ampSpace, freqSpace );
		ggSmpMap.setSmpMap( smpMap );

		ggNoiseFloor	= new ParamField( noiseSpace );
		ggTriggerThresh	= new ParamField( trigSpace );
		ggSmpLoFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		ggSmpHiFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		ggSmpLoVel		= new ParamField( ampSpace );
		ggSmpHiVel		= new ParamField( ampSpace );

		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.gridheight	= 1;
		con.weighty		= 0.0;
		con.weightx		= 0.0;
	sg.addLabel( new GroupLabel( "General Sensitivity", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM ));

		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "Noisefloor", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggNoiseFloor, GGS_NOISEFLOOR, paramL );

		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "Trigger", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggTriggerThresh, GGS_TRIGTHRESH, paramL );

		con.fill		= GridBagConstraints.BOTH;
	sg.addLabel( new GroupLabel( "Sample Zone", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM ));
		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "High freq", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpHiFreq, GGS_SMPHIFREQ, paramL );

		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "Low freq", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpLoFreq, GGS_SMPLOFREQ, paramL );

		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "High vel.", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpHiVel, GGS_SMPHIVEL, paramL );

		con.gridwidth	= GridBagConstraints.RELATIVE;
		sg.addLabel( new JLabel( "Low vel.", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpLoVel, GGS_SMPLOVEL, paramL );

		ggSmpFile		= new PathField( PathField.TYPE_INPUTFILE, "Select sample" );
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Sample", SwingConstants.RIGHT ));
		con.weightx		= 0.9;
		sg.addPathField( ggSmpFile, GGS_SMPFILE, pathL );

		ggSmpLoop		= new JCheckBox( "Loop" );
		con.weightx		= 0.1;
		sg.addCheckbox( ggSmpLoop, GGS_SMPLOOP, il );

		ggSmpPhase		= new JComboBox();
		ggSmpPhase.addItem( "Continuous" );
		ggSmpPhase.addItem( "Zero @trig" );
		ggSmpPhase.addItem( "Spect phase @trig" );
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Phase", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addChoice( ggSmpPhase, GGS_SMPPHASE, il );

		ggSmpGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.gridwidth	= 2;
		con.weightx		= 1.0;
		sg.addParamField( ggSmpGain, GGS_SMPGAIN, paramL );
		
		ggSmpAtk		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Attack", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpAtk, GGS_SMPATK, paramL );

		ggSmpBase		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.gridwidth	= 1;
		sg.addLabel( new JLabel( "Base freq", SwingConstants.RIGHT ));
		con.gridwidth	= 2;
		con.weightx		= 1.0;
		sg.addParamField( ggSmpBase, GGS_SMPBASE, paramL );

		ggSmpRls		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Release", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggSmpRls, GGS_SMPRLS, paramL );

		con.weightx		= 1.0;
	sg.addLabel( new GroupLabel( "Total", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM ));

		ggGain			= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.gridwidth	= 2;
		con.weightx		= 1.0;
		sg.addParamField( ggGain, GGS_GAIN, paramL );

		ggLength		= new ParamField( lengthSpaces );
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Length", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		sg.addParamField( ggLength, GGS_LENGTH, paramL );

		ggQuality		= new JComboBox();
		ggQuality.addItem( "Low: linear+mono" );
		ggQuality.addItem( "Medium: linear" );
		ggQuality.addItem( "High: filtered" );
		con.gridwidth	= 1;
		con.weightx		= 0.0;
		sg.addLabel( new JLabel( "Quality", SwingConstants.RIGHT ));
		con.gridwidth	= 2;
		con.weightx		= 1.0;
		sg.addChoice( ggQuality, GGS_QUALITY, il );

	 	return sg;
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
		
		Component		gg;
		String			val;
		SmpMap			smpMap;
		Enumeration		smpEnum;

		// top level
		gg	= sgui.getItemObj( GGS_QUALITY );
		if( gg != null ) {
			((JComboBox) gg).setSelectedIndex( pr.intg[ PR_QUALITY ]);
		}
		gg	= sgui.getItemObj( GGS_NOISEFLOOR );
		if( gg != null ) {
			((ParamField) gg).setParam( pr.para[ PR_NOISEFLOOR ]);
		}
		gg	= sgui.getItemObj( GGS_TRIGTHRESH );
		if( gg != null ) {
			((ParamField) gg).setParam( pr.para[ PR_TRIGTHRESH ]);
		}
		gg	= sgui.getItemObj( GGS_GAIN );
		if( gg != null ) {
			((ParamField) gg).setParam( pr.para[ PR_GAIN ]);
		}
		gg	= sgui.getItemObj( GGS_LENGTH );
		if( gg != null ) {
			((ParamField) gg).setParam( pr.para[ PR_LENGTH ]);
		}
		gg	= sgui.getItemObj( GGS_SMPMAP );
		if( gg != null ) {
			samples.setSize( 0 );	// "clear"
			currentSmp	= null;
			val			= pr.text[ PR_SMPMAP ];
			if( val.length() != 0 ) {
				smpMap	= SmpMap.valueOf( val );
				smpEnum	= smpMap.getSamples();
				while( smpEnum.hasMoreElements() ) {
					samples.addElement( smpEnum.nextElement() );
				}
				((SmpMapPanel) gg).setSmpMap( smpMap );
			} else {
				((SmpMapPanel) gg).clear();
			}
		}
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );

		Component		gg;
		SmpMap			smpMap;
		
		// top level
		gg	= sgui.getItemObj( GGS_QUALITY );
		if( gg != null ) {
			pr.intg[ PR_QUALITY ] = ((JComboBox) gg).getSelectedIndex();
		}
		gg	= sgui.getItemObj( GGS_NOISEFLOOR );
		if( gg != null ) {
			pr.para[ PR_NOISEFLOOR ] = ((ParamField) gg).getParam();
		}
		gg	= sgui.getItemObj( GGS_TRIGTHRESH );
		if( gg != null ) {
			pr.para[ PR_TRIGTHRESH ] = ((ParamField) gg).getParam();
		}
		gg	= sgui.getItemObj( GGS_GAIN );
		if( gg != null ) {
			pr.para[ PR_GAIN ] = ((ParamField) gg).getParam();
		}
		gg	= sgui.getItemObj( GGS_LENGTH );
		if( gg != null ) {
			pr.para[ PR_LENGTH ] = ((ParamField) gg).getParam();
		}
		gg	= sgui.getItemObj( GGS_SMPMAP );
		if( gg != null ) {
			smpMap 	= ((SmpMapPanel) gg).getSmpMap();
			while( smpMap.size() > 0 ) {
				smpMap.removeSample( 0 );								// "its" SmpZones contain only correct margins
			}
			for( int i = 0; i < samples.size(); i++ ) {
				smpMap.addSample( (SmpZone) samples.elementAt( i ));	// "our" SmpZones contain the correct data
			}
			pr.text[ PR_SMPMAP ] = smpMap.toString();
		}
	}
	
	/**
	 *	Neues Inputfile setzen
	 */
	public void setInput( String fname )
	{
		SpectralFile	f		= null;
		SpectStream		stream	= null;

		Component		gg;
		ParamSpace		vSpace;

	// ---- Header lesen ----
		try {
			f		= new SpectralFile( fname, GenericFile.MODE_INPUT );
			stream	= f.getDescr();
			f.close();
		
		} catch( IOException e1 ) {

			GUIUtil.displayError( getComponent(), e1, getTitle() );
			return;
		}

	// ---- Typ + Format ausgeben ----
		
	// ---- Kommunikation mit Settings-GUI ----
		gg = sgui.getItemObj( GGS_LENGTH );
		if( gg != null ) {
			((ParamField) gg).setReference( new Param( SpectStream.framesToMillis( stream, stream.frames ),
													   Param.ABS_MS ));
		}
		gg = sgui.getItemObj( GGS_SMPMAP );
		if( gg != null ) {
//			vSpace		= (ParamSpace) ((SmpMapPanel) gg).getVSpace().clone();
			vSpace		= new ParamSpace( ((SmpMapPanel) gg).getVSpace() );
//			vSpace.max	= stream.hiFreq;
//			vSpace.min	= vSpace.fitValue( MIDFREQ*MIDFREQ / vSpace.max );
			vSpace		= new ParamSpace( vSpace.fitValue( MIDFREQ*MIDFREQ / stream.hiFreq ), vSpace.max, vSpace.inc, vSpace.unit );
			((SmpMapPanel) gg).setSpaces( null, vSpace );
		}
	}

	/**
	 *	Sample-Filename geaendert => Datei wird ueberprueft
	 *	und ggf. Gadgets angepasst
	 */
	public void setSample( String fname )
	{
		AudioFile		f		= null;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
//			stream	= f.getDescr();
			f.close();
		
		} catch( IOException e1 ) {

			GUIUtil.displayError( getComponent(), e1, getTitle() );
			return;
		}

	// ---- Kommunikation mit Settings-GUI ----
// XXX check Loop and Base Freq!
	}

// -------- Processor Interface --------
		
	protected void process()
	{
		int					i, j, k;
		int					off, len;
		float				progOff, progWeight;
		
		// io
		SpectralFile		inF				= null;
		AudioFile			outF			= null;
		SpectStream			inStream		= null;
		AudioFileDescr			outStream		= null;
		SpectFrame			pfr				= null;		// "previous" frame
		SpectFrame			cfr				= null;		// current frame
		SpectFrame			nfr				= null;		// "next" frame
		PathField			ggOutput;
		int					framesRead;
		int					smpPerFrame;
		int					frameSize;					// = smpPerFrame * chanNum
		int					overhead;
		float[]				outData;
		float[][]			tempBuf;
		int					chanNum;

		int					numSmp			= samples.size();
		int[]				smpFindex		= null;	// this will point to the indices in smpF, smpStream, smpData
		AudioFile[]			smpF			= null;	// might be null			if smpFindex != index
		AudioFileDescr[]		smpStream		= null;	// containts correct val	if smpFindex != index
		float[][]			smpData			= null;	// containts correct val	if smpFindex != index

		// Synthesize
		SmpZoneGlobal[]		szgs;
		SmpZoneLocal[][]	szls;
		SmpZoneLocal		szl;
		float				noise;			// noise floor abs amp
		float				gain;			// global gain abs amp
		float				trig;			// trigger thresh abs amp
		float				length;			// output in abs ms
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		float				srcAmp;
		float				srcPhase;
		double				srcReal;
		double				srcImg;
		float				destAmp;
		float				floatPI2		= (float) Constants.PI2;
		boolean				triggered		= false;

		int					band;
		int					ch;

		// Smp Init
		SmpZone				smp, smp2;
		int					totalSamples	= 0;	// reichen 32 bit?
		int					samplesRead;
		int					maxRlsFrames;

		float				maxAmp			= 0.0f;

topLevel: try {
		// ---- open input ----
			if( numSmp == 0 ) {
				throw new IOException( ERR_NOZONES );
			}

			inF			= new SpectralFile( pr.text[ PR_INPUTFILE ], GenericFile.MODE_INPUT );
			inStream	= inF.getDescr();
			// this helps to prevent errors from empty files!
			if( inStream.frames <= 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			chanNum		= inStream.chanNum;
			if( pr.intg[ PR_QUALITY ] == QUALITY_LOW ) chanNum = 1;

		// ---- Filter berechnen ----
			if( pr.intg[ PR_QUALITY ] == QUALITY_HIGH ) {
				fltSmpPerCrossing	= Filter.FLTSMPPERCROSSING;
				len					= fltSmpPerCrossing * 6;
				flt					= new float[ len ];
				fltD				= new float[ len ];								// 15% oversamp.; kaiser 6.0
				gain				= Filter.createAntiAliasFilter( flt, fltD, len, 0.85f, 6.0f );
			} else {
				flt					= null;
				fltD				= null;
				gain				= 1.0f;
			}

		// ---- Open and prepare samples ----
		
			smpF		= new AudioFile[ numSmp ];
			smpStream	= new AudioFileDescr[ numSmp ];
			smpData		= new float[ numSmp ][];
			smpFindex	= new int[ numSmp ];

			for( i = 0; i < numSmp; i++ ) {
				smpF[ i ]		= null;			// allow save cleanup
				smpStream[ i ]	= null;
				smpData[ i ]	= null;
				smpFindex[ i ]	= -1;
			}

smpInit:	for( i = 0, k = 1; (i < numSmp) && threadRunning; i++ ) {
			
				smp = (SmpZone) samples.elementAt( i );

				for( j = 0; j < i; j++ ) {		// Sample mehrmals benutzt?
					smp2 = (SmpZone) samples.elementAt( j );
					if( smp.fileName.equals( smp2.fileName )) {
						smpFindex[ i ]	= j;
						smpStream[ i ]	= smpStream[ j ];
						smpData[ i ]	= smpData[ j ];
						continue smpInit;
					}
				}
				smpFindex[ i ]	= i;
				smpF[ i ]		= AudioFile.openAsRead( new File( smp.fileName ));
				smpStream[ i ]	= smpF[ i ].getDescr();
				
				if( smpStream[ i ].length <= 0 ) throw new EOFException( ERR_EMPTY );

				totalSamples += smpStream[ i ].length * smpStream[ i ].channels;
				
				k = Math.max( k, smpStream[ i ].channels );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// --- open + prepare output ----
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr();
			ggOutput.fillStream( outStream );
			outStream.channels = chanNum;
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			gain   *= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, inStream )).val;
			noise	= (float) (Param.transform( pr.para[ PR_NOISEFLOOR ], Param.ABS_AMP, ampRef, inStream )).val;
			trig	= (float) (Param.transform( pr.para[ PR_TRIGTHRESH ], Param.ABS_AMP, ampRef, inStream )).val;
			length	= (float) (Param.transform( pr.para[ PR_LENGTH ], Param.ABS_MS,
									new Param( SpectStream.framesToMillis( inStream, inStream.frames ), Param.ABS_MS ),
									inStream )).val / 1000.f;
			smpPerFrame	= (int) (length  / inStream.frames * outStream.rate + 0.5);
			frameSize	= smpPerFrame * chanNum;
			
			szgs		= new SmpZoneGlobal[ numSmp ];	// MAIN INITIALISATION !
			szls		= SmpZoneLocal.init( inStream, outStream, samples, smpStream, smpData, gain, szgs,
											 smpFindex, (flt != null) ? flt.length : 0 );

			for( i = 0, maxRlsFrames = 0; i < numSmp; i++ ) {
				maxRlsFrames = Math.max( maxRlsFrames, szgs[ i ].rls );
			}
			outData		= new float[ (Math.max( smpPerFrame, maxRlsFrames ) + 2) * chanNum ];
			overhead	= outData.length - frameSize;

			tempBuf	= new float[ k ][ Math.max( 8192, smpPerFrame )];

		// ---- preload all samples ----

			for( i = 0; (i < numSmp) && (smpFindex[ i ] != i); i++ ) ;	// next "physical" sample
			if( i == numSmp ) throw new IOException( "Missing "+ (totalSamples) +" samples?!" );
			samplesRead	= 0;
			off			= 0;
			progWeight	= 0.01f;			
			while( (samplesRead < totalSamples) && threadRunning ) {

					// XXX use prefs-buffersize
				len = (int) Math.min( 8192 - 8192 % smpStream[ i ].channels,
								(smpStream[ i ].length * smpStream[ i ].channels) - off );
				if( len > 0 ) {
					len			+= readInterleaved( smpF[ i ], smpData[ i ], off + szgs[ i ].dataOff, len, tempBuf, 
													(int) (smpStream[ i ].length * smpStream[ i ].channels) );
					off			+= len;
					samplesRead	+= len;

				} else {	// next one
					do i++; while( (i < numSmp) && (smpFindex[ i ] != i) );
					if( i == numSmp ) throw new IOException( "Missing "+ (totalSamples - samplesRead) +" samples?!" );
					off = 0;
				}
			// .... progress ....
				setProgression( ((float) samplesRead / (float) totalSamples) * progWeight );
			}

			// brauchen die Dateien nicht mehr
			for( i = 0; i < numSmp; i++ ) {
				if( smpF[ i ] != null ) {
					smpF[ i ].close();
					smpF[ i ] = null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Conversion Loop ----

			progOff		= getProgression();
			progWeight	= 1.0f - progOff;
			cfr			= inF.allocFrame();		// => previous frame
			nfr			= inF.readFrame();		// => current frame
			SpectFrame.clear( cfr );

			for( framesRead = 1; (framesRead < inStream.frames) && threadRunning; ) {

				pfr	= cfr;		// shift frames backwards
				cfr	= nfr;
				nfr	= inF.readFrame();							// read spect frame
				
				for( band = 0; band < inStream.bands; band++ ) {
					if( szls[ band ].length == 0 ) continue;		// eh keine zonen hier

					if( chanNum == 1 ) {	// faster if mono
						srcAmp		= cfr.data[ 0 ][ (band<<1) + SpectFrame.AMP ];
						srcPhase	= cfr.data[ 0 ][ (band<<1) + SpectFrame.PHASE ];
						while( srcPhase < 0 ) srcPhase += floatPI2;
						triggered	= (srcAmp / pfr.data[ 0 ][ (band<<1) + SpectFrame.AMP ]) >= trig;

					} else {				// else sum channels
						srcImg		= 0.0;
						srcReal		= 0.0;
						triggered	= false;
						for( ch = 0; ch < chanNum; ch++ ) {
							srcAmp		= cfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
							srcPhase	= cfr.data[ ch ][ (band<<1) + SpectFrame.PHASE ];
							srcImg	   += srcAmp * Math.sin( srcPhase );
							srcReal	   += srcAmp * Math.cos( srcPhase );
							if( (srcAmp / pfr.data[ ch ][ (band<<1) + SpectFrame.AMP ]) >= trig ) {
								triggered = true;
							}
						}
						srcAmp		= (float) Math.sqrt( srcImg*srcImg + srcReal*srcReal ) / chanNum;
						srcPhase	= (float) Math.atan2( srcImg, srcReal ) + floatPI2;
					}

			// ---------------- Noiselevel ueberschritten ----------------
					if( srcAmp > noise ) {
					
						for( i = szls[ band ].length - 1; i >=0; i-- ) {
							
							szl = szls[ band ][ i ];	// ------------ old triggaz ------------
							if( szl.triggered ) {	// maybe the velocity is invalid now?
								
								if( (szl.basic.velLo >= srcAmp) || (szl.basic.velHi < srcAmp) ) {	// ...release

									szl.triggered	= false;
									szl.lvlTarget	= 0.0f;
									szl.lvlIncr		= -szl.lvlCurrent / szl.basic.rls;

									for( ch = 0; ch < chanNum; ch++ ) {
										srcAmp = cfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
										if( flt != null ) {
											resampleFlt( szl, ch, chanNum, srcAmp, srcAmp, outData, 0, szl.basic.rls );
										} else {
											resampleLin( szl, ch, chanNum, srcAmp, srcAmp, outData, 0, szl.basic.rls );
										}
									}

								} else {									// ...continue oscillation

									for( ch = 0; ch < chanNum; ch++ ) {
										srcAmp 	= cfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
										destAmp	= nfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
										if( flt != null ) {
											resampleFlt( szl, ch, chanNum, srcAmp, destAmp, outData, 0, smpPerFrame );
										} else {
											resampleLin( szl, ch, chanNum, srcAmp, destAmp, outData, 0, smpPerFrame );
										}
									}
								}

							} else if( triggered ) {	// ------------ nu triggaz ------------

								if( (szl.basic.velLo < srcAmp) && (szl.basic.velHi >= srcAmp) ) {	// ...start the RIOT
									
									szl.triggered	= true;
									szl.lvlCurrent	= 0.0f;
									szl.lvlTarget	= szl.basic.gain;
									szl.lvlIncr		= szl.lvlTarget / szl.basic.atk;

									switch( szl.basic.phaseType ) {
									case SmpZone.PHASE_LINEAR:
										szl.phase	= outStream.length * szl.smpIncr;
										break;
									case SmpZone.PHASE_TRIGZERO:
										szl.phase	= 0.0f;
										break;
									case SmpZone.PHASE_TRIGSPECT:
										szl.phase	= ((srcPhase / floatPI2) % 1.0f) * szl.basic.stream.length;
										break;
									}

									for( ch = 0; ch < chanNum; ch++ ) {
										srcAmp = cfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
										destAmp	= nfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
										if( flt != null ) {
											resampleFlt( szl, ch, chanNum, srcAmp, destAmp, outData, 0, smpPerFrame );
										} else {
											resampleLin( szl, ch, chanNum, srcAmp, destAmp, outData, 0, smpPerFrame );
										}
									}
								}
							}
						}
			// ---------------- Noiselevel unterschritten ----------------
					} else {

						for( i = szls[ band ].length - 1; i >=0; i-- ) {
							
							szl = szls[ band ][ i ];	// ------------ old triggaz ------------
							if( szl.triggered ) {				// ...shut 'em down
								
								szl.triggered	= false;
								szl.lvlTarget	= 0.0f;
								szl.lvlIncr		= -szl.lvlCurrent / szl.basic.rls;
									
								for( ch = 0; ch < chanNum; ch++ ) {
									srcAmp = cfr.data[ ch ][ (band<<1) + SpectFrame.AMP ];
									if( flt != null ) {
										resampleFlt( szl, ch, chanNum, srcAmp, srcAmp, outData, 0, szl.basic.rls );
									} else {
										resampleLin( szl, ch, chanNum, srcAmp, srcAmp, outData, 0, szl.basic.rls );
									}
								}
							}
						}
					}
				} // for( bands )

//				outF.writeSamples( outData, 0, frameSize );
				writeInterleaved( outF, outData, 0, frameSize, tempBuf, chanNum );
				inF.freeFrame( pfr );

				for( i = 0; i < frameSize; i++ ) {
					if( Math.abs( outData[ i ]) > maxAmp ) {
						maxAmp = Math.abs( outData[ i ]);
					}
				}

				// shift data and clear new frame space
				System.arraycopy( outData, frameSize, outData, 0, overhead );
				for( i = overhead; i < outData.length; i++ ) {
					outData[ i ] = 0.0f;
				}

 				framesRead++;

		// .... progress ....
				setProgression( ((float) framesRead / (float) inStream.frames) * progWeight + progOff );
			} // while( not all read )
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
		
			inF.freeFrame( cfr );
			inF.freeFrame( nfr );
			outF.close();
			outF = null;
			inF.close();
			inF = null;

			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			smpStream	= null;
			smpData		= null;
			szgs		= null;
			szls		= null;
			szl			= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		smpStream	= null;
		smpData		= null;
		szgs		= null;
		szls		= null;
		szl			= null;
		flt			= null;
		fltD		= null;

		if( inF != null ) {
			inF.cleanUp();
			inF = null;
		}
		if( outF != null ) {
			outF.cleanUp();
			outF = null;
		}
		if( smpF != null ) {
			for( i = 0; i < smpF.length; i++ ) {
				if( smpF[ i ] != null ) {
					smpF[ i ].cleanUp();
					smpF[ i ] = null;
				}
			}
		}
	} // process()

// -------- private Methoden --------

	/**
	 *	Daten resamplen; einfache Version (linear interpoliert, kein Filter)
	 *	Phase und Gain werden in der szl geaendert, wenn der Kanal dem letzten output-Kanal entspricht!
	 *
	 *	@param	szl			Struktur des aktuellen Bandes
	 *	@param	ch			Kanal (in outData); 0...chanNum-1
	 *	@param	chanNum		gesamt in outData
	 *	@param	startAmp	Startwert gain-Faktor
	 *	@param	endAmp		Endwert gain-Faktor
	 *	@param	outData		wohin das Resamplete gespeichert werden soll; die Daten werden *addiert* zum bisherigen
	 *	@param	dataOff		SampleWords in outData
	 *	@param	dataLen		SampleWords in outData; mindestens 2!
	 */
	protected void resampleLin( SmpZoneLocal szl, int ch, int chanNum, float startAmp, float endAmp,
								float outData[], int dataOffStart, int dataLenStart )
	{
		int		srcChanNum	= szl.basic.stream.channels;
		int		srcCh		= ch % srcChanNum;
		int		srcOff;
		float	q;
		float	srcData[]	= szl.basic.data;
		float	startPhase	= szl.phase;
		float	phase;
		float	smpIncr		= szl.smpIncr;
		int		lvlIncrNum;
		float	gain;
		float	startGain;
		float	gainIncr;
		int		pass;
		
		Region	loop;

		startGain	= szl.lvlCurrent * startAmp;
		int dataOff		= dataOffStart * chanNum + ch;
		int dataLen		= dataLenStart;

	// ---- Gain-Zunahme pro Output-Sample (gainIncr) berechnen ----
		if( szl.lvlIncr != 0.0f ) {	// Atk/Rls Phase

			lvlIncrNum		= (int) ((szl.lvlTarget - szl.lvlCurrent) / szl.lvlIncr);
			if( lvlIncrNum <= dataLen ) {			// target level will be reached
			
				if( lvlIncrNum < 1 ) {
					lvlIncrNum = 1;
				}
				gain		= szl.lvlTarget * (startAmp + (float) lvlIncrNum / (float) dataLen * (endAmp - startAmp));
				gainIncr	= (gain - startGain) / lvlIncrNum;

				if( ch == chanNum - 1 ) {	// write back
					szl.lvlCurrent	= szl.lvlTarget;
					szl.lvlIncr		= 0.0f;
				}

			} else {	// target level will not be reached, interpolate

				gain		= endAmp * (szl.lvlCurrent + dataLen * szl.lvlIncr);
				gainIncr	= (gain - startGain) / dataLen;
				
				if( ch == chanNum - 1 ) {	// write back
					szl.lvlCurrent	+= szl.lvlIncr * dataLen;
					szl.lvlIncr		 = (szl.lvlTarget - szl.lvlCurrent) / (lvlIncrNum - dataLen);
				}
			}
			
		} else {	// sustain phase
			gain		= endAmp * szl.lvlCurrent;
			gainIncr	= (gain - startGain) / dataLen;
			lvlIncrNum	= 0;
		}

	// ---- Samples pro "Durchgang" berechnen (pass) ----
		do {
			loop = (Region) szl.basic.stream.getProperty( AudioFileDescr.KEY_LOOP );
			if( loop != null ) {
				while( startPhase >= loop.span.getStop() ) {
					startPhase -= loop.span.getLength();
				}
				pass = Math.min( dataLen, (int) Math.ceil( (loop.span.getStop() - startPhase) / smpIncr ));
				if( pass <= 0 ) {
					startPhase	= loop.span.getStart();
					pass		= Math.min( dataLen, (int) Math.ceil( (loop.span.getStop() - startPhase) / smpIncr ));
					if( pass <= 0 ) return;	// Loop zu klein
				}
	
			} else {
				pass	= Math.min( dataLen, (int) Math.ceil( (szl.basic.stream.length - startPhase) / smpIncr ));
				dataLen	= pass;	// no loop => only one pass
			}

	// ---- eigentliches Resampling ----
			phase	= startPhase;
			gain	= startGain;
			for( int i = 0; i < pass; i++,
									  phase = startPhase + i * smpIncr,
									  gain	= startGain  + i * gainIncr ) {

				q		= phase % 1.0f;
				srcOff	= ((int) phase + szl.basic.dataOff) * srcChanNum + srcCh;
				outData[ dataOff ] += gain * (srcData[ srcOff ]		   * (1.0f-q) +
											  srcData[ srcOff + srcChanNum ] * q);
				dataOff	+= chanNum;
		//		gain	+= gainIncr;
		//		phase	+= smpIncr;		// WARNUNG: HIER TRETEN MASSIVE RUNDUNGSFEHLER AUF, DIE ZU ARRAY-FEHLERN FUEHREN
	
				if( lvlIncrNum > 0 ) {		// ...amp-env
					if( --lvlIncrNum == 0 ) {	// Ende der Envelope, gainIncr muss neu berechnet werden
						gainIncr	= (endAmp * szl.lvlTarget - gain) / (dataLen - i);
						startGain	= gain - i * gainIncr;
					}
				}
			}
			
			startPhase	= phase;
			startGain	= phase;
			dataLen	   -= pass;
		} while( dataLen > 0 );
		
		if( ch == chanNum - 1 ) {	// write back
			szl.phase = phase;
		}
	}

	/**
	 *	Daten resamplen; "bandlimited interpolation"
	 *	Phase und Gain werden in der szl geaendert, wenn der Kanal dem letzten output-Kanal entspricht!
	 *
	 *	@param	szl			Struktur des aktuellen Bandes
	 *	@param	ch			Kanal (in outData); 0...chanNum-1
	 *	@param	chanNum		gesamt in outData
	 *	@param	startAmp	Startwert gain-Faktor
	 *	@param	endAmp		Endwert gain-Faktor
	 *	@param	outData		wohin das Resamplete gespeichert werden soll; die Daten werden *addiert* zum bisherigen
	 *	@param	dataOff		SampleWords in outData
	 *	@param	dataLen		SampleWords in outData; mindestens 2!
	 */
	protected void resampleFlt( SmpZoneLocal szl, int ch, int chanNum, float startAmp, float endAmp,
								float outData[], int dataOffStart, int dataLenStart )
	{
		int		srcChanNum	= szl.basic.stream.channels;
		int		srcCh		= ch % srcChanNum;
		int		srcOff;
		float	q;
		float	srcData[]	= szl.basic.data;
		float	startPhase	= szl.phase;
		float	phase;
		float	smpIncr		= szl.smpIncr;
		int		lvlIncrNum;
		float	gain;
		float	startGain;
		float	gainIncr;
		int		pass;

		// specific to the filtering		
		float	fltIncr;
		float	fltOff;
		int		fltOffI;
		int		j, k;
		float	r;
		float	val;
		
		Region	loop;

		if( smpIncr > 1.0f ) {
			fltIncr		= fltSmpPerCrossing / smpIncr;
			startAmp   /= smpIncr;
			endAmp	   /= smpIncr;
		} else {
			fltIncr		= fltSmpPerCrossing;
		}

		startGain	= szl.lvlCurrent * startAmp;
		int dataOff		= dataOffStart * chanNum + ch;
		int dataLen		= dataLenStart;

	// ---- Gain-Zunahme pro Output-Sample (gainIncr) berechnen ----
		if( szl.lvlIncr != 0.0f ) {	// Atk/Rls Phase

			lvlIncrNum		= (int) ((szl.lvlTarget - szl.lvlCurrent) / szl.lvlIncr);
			if( lvlIncrNum <= dataLen ) {			// target level will be reached
			
				if( lvlIncrNum < 1 ) {
					lvlIncrNum = 1;
				}
				gain		= szl.lvlTarget * (startAmp + (float) lvlIncrNum / (float) dataLen * (endAmp - startAmp));
				gainIncr	= (gain - startGain) / lvlIncrNum;

				if( ch == chanNum - 1 ) {	// write back
					szl.lvlCurrent	= szl.lvlTarget;
					szl.lvlIncr		= 0.0f;
				}

			} else {	// target level will not be reached, interpolate

				gain		= endAmp * (szl.lvlCurrent + dataLen * szl.lvlIncr);
				gainIncr	= (gain - startGain) / dataLen;
				
				if( ch == chanNum - 1 ) {	// write back
					szl.lvlCurrent	+= szl.lvlIncr * dataLen;
					szl.lvlIncr		 = (szl.lvlTarget - szl.lvlCurrent) / (lvlIncrNum - dataLen);
				}
			}
			
		} else {	// sustain phase
			gain		= endAmp * szl.lvlCurrent;
			gainIncr	= (gain - startGain) / dataLen;
			lvlIncrNum	= 0;
		}

	// ---- Samples pro "Durchgang" berechnen (pass) ----
		do {
			loop = (Region) szl.basic.stream.getProperty( AudioFileDescr.KEY_LOOP );
			if( loop != null ) {
				while( startPhase >= loop.span.getStop() ) {
					startPhase -= loop.span.getLength();
				}
				pass = Math.min( dataLen, (int) Math.ceil( (loop.span.getStop() - startPhase) / smpIncr ));
				if( pass <= 0 ) {
					startPhase	= loop.span.getStart();
					pass		= Math.min( dataLen, (int) Math.ceil( (loop.span.getStop() - startPhase) / smpIncr ));
					if( pass <= 0 ) return;	// Loop zu klein
				}
	
			} else {
				pass	= Math.min( dataLen, (int) Math.ceil( (szl.basic.stream.length - startPhase) / smpIncr ));
				dataLen	= pass;	// no loop => only one pass
			}

	// ---- eigentliches Resampling ----
			phase	= startPhase;
			gain	= startGain;
			for( int i = 0; i < pass; i++,
									  phase = startPhase + i * smpIncr,
									  gain	= startGain  + i * gainIncr ) {

				q		= phase % 1.0f;
				val		= 0.0f;
				k		= -srcChanNum;			// src-smpIncr
				for( j = 0; j < 2; j++ ) {		// 0 = leftwing, 1 = rightwing
					srcOff	= ((int) phase + szl.basic.dataOff + j) * srcChanNum + srcCh;
					fltOff	= q * fltIncr;
					fltOffI	= (int) fltOff;
					while( fltOffI < flt.length ) {
						r		= fltOff % 1.0f;	// 0...1 for interpol.
						val	   += srcData[ srcOff ] * (flt[ fltOffI ] + fltD[ fltOffI ] * r);
						srcOff += k;
						fltOff += fltIncr;
						fltOffI	= (int) fltOff;
					}
					q = 1.0f - q;
					k = -k;
				}
				outData[ dataOff ] += val * gain;
				dataOff += chanNum;
	
				if( lvlIncrNum > 0 ) {		// ...amp-env
					if( --lvlIncrNum == 0 ) {	// Ende der Envelope, gainIncr muss neu berechnet werden
						gainIncr	= (endAmp * szl.lvlTarget - gain) / (dataLen - i);
						startGain	= gain - i * gainIncr;
					}
				}
			}
			
			startPhase	= phase;
			startGain	= phase;
			dataLen	   -= pass;
		} while( dataLen > 0 );
		
		if( ch == chanNum - 1 ) {	// write back
			szl.phase = phase;
		}
	}

	private static int readInterleaved( AudioFile f, float[] iBuf, int iOff, int iLen, float[][] tempBuf, int numCh )
	throws IOException
	{
		assert (iLen % numCh) == 0 : "invalid interleaved length";
		
		int ch, i, j, len = iLen / numCh;
		
		f.readFrames( tempBuf, 0, len );
		
		for( ch = 0; ch < numCh; ch++ ) {
			for( i = iOff + ch, j = 0; j < iLen; i++, j += numCh ) {
				iBuf[ i ] = tempBuf[ ch ][ j ];
			}
		}
		
		return iLen;
	}

	private static void writeInterleaved( AudioFile f, float[] iBuf, int iOff, int iLen, float[][] tempBuf, int numCh )
	throws IOException
	{
		assert (iLen % numCh) == 0 : "invalid interleaved length";
		
		int ch, i, j, len = iLen / numCh;
		
		for( ch = 0; ch < numCh; ch++ ) {
			for( i = iOff + ch, j = 0; j < iLen; i++, j += numCh ) {
				tempBuf[ ch ][ j ] = iBuf[ i ];
			}
		}
		f.writeFrames( tempBuf, 0, len );
	}
}
// class SmpSynDlg


/**
 *	Interne Klasse fuer die Synthese
 *	nur einmal pro SmpZone vorhanden
 */
class SmpZoneGlobal
{
	public float		velHi, velLo;		// abs amp
	public float		gain;				// abs amp multiplied by global gain
	public float		base;				// abs Hz
	public int			atk, rls;			// output-samplewords
	public int			phaseType;			// SmpZone.flags & PHASEMASK	
	public AudioFileDescr	stream;
	public float		data[]		= null;	// sampledata
	public int			dataOff		= 0;	// noetiger Offset wegen Filter-Convolution;
											//		von SmpLocal.init() initialisiert; noch nicht mit chanNum multipliziert!

	/**
	 *	@param	basic		zugrundeliegende SmpZone
	 *	@param	smpStream	initialisierter AudioFileDescr
	 *	@param	outStream	Synthese-Ausgabe; outStream.samples muss tatsaechlicher Zielgroesse entsprechen!
	 *	@param	data		Sample-Daten
	 *	@param	gain		globales Gain als abs amp
	 */
	public SmpZoneGlobal( SmpZone basic, AudioFileDescr smpStream, AudioFileDescr outStream, float gain )
	{
		Param ampRef	= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		Param timeRef	= new Param( (float) outStream.length / outStream.rate * 1000.0f, Param.ABS_MS );
		
		this.stream		= smpStream;
		
		velHi			= (float) (Param.transform( basic.velHi, Param.ABS_AMP, ampRef, null )).val;
		velLo			= (float) (Param.transform( basic.velLo, Param.ABS_AMP, ampRef, null )).val;
		base			= (float) basic.base.val;
		this.gain		= (float) (Param.transform( basic.gain,  Param.ABS_AMP, ampRef, null )).val * gain;
		phaseType		= basic.flags & SmpZone.PHASEMASK;
		atk				= (int) ((Param.transform( basic.atk,    Param.ABS_MS, timeRef, null )).val / 1000.0 *
							outStream.rate + 0.5);
		rls				= (int) ((Param.transform( basic.rls,    Param.ABS_MS, timeRef, null )).val / 1000.0 *
							outStream.rate + 0.5);
	}
} // class SmpZoneGlobal

/**
 *	Interne Klasse fuer die Synthese
 *	pro Verwendung der SmpZone (Frequenz-Band) vorhanden
 */
class SmpZoneLocal
{
	public SmpZoneGlobal	basic;
	public float			phase		= 0.0f;
	public boolean			triggered	= false;

	public float			lvlCurrent	= 0.0f;		// current Level
	public float			lvlTarget	= 0.0f;		// target Level
	public float			lvlIncr		= 0.0f;		// lvlCurrent-increment per output-Sample
	
	public float			smpIncr;				// sample-increment = 1 / resmp-factor

	/**
	 *	@param	freq	Frequenz des Bandes, das diese szl enthaelt
	 */
	public SmpZoneLocal( SmpZoneGlobal basic, AudioFileDescr outStream, float freq )
	{
		this.basic	= basic;
		smpIncr		= (float) ((basic.stream.rate * freq) / (outStream.rate * basic.base));
	}

	/**
	 *	Init Array of this Struct
	 *
	 *	@param	smpStreams	Indices muessen mit smpZones uebereinstimmen
	 *	@param	smpData		dito; diese Methode fuellt die Felder aus, d.h. legt selbstaendig die Audiobuffer an!!!
	 *	@param	szgs		Groesse muss mit smpZones uebereinstimmen; wird gefuellt und
	 *						kann z.B. zur Ermittelung der Release-Zeiten benutzt werden!
	 *	@return	Array-Index entspricht Frequenz-Band im inStream
	 */
	public static SmpZoneLocal[][] init( SpectStream inStream, AudioFileDescr outStream, Vector smpZones,
										 AudioFileDescr[] smpStreams, float[][] smpData, float gain,
										 SmpZoneGlobal[] szgs, int smpFindex[], int Nwing )
	{
		int[]				ID			= new int[ smpZones.size() ];
		int					num;
		float				fNum;
		SmpZoneLocal[][]	szls		= new SmpZoneLocal[ inStream.bands ][];
		float				freq;
		float				maxIncr[]	= new float[ smpZones.size() ];
		SmpZone				smp;

		// transform SmpZones to SmpZoneGlobals
		for( int i = 0; i < szgs.length; i++ ) {
			szgs[ i ]	 = new SmpZoneGlobal( (SmpZone) smpZones.elementAt( i ), smpStreams[ i ], outStream, gain );
			maxIncr[ i ] = 0.0f;
		}

		for( int band = 0; band < inStream.bands; band++ ) {

			// replace by SpectStream.getFrequencies()! XXX
			freq	= ((inStream.hiFreq - inStream.loFreq) * band / (inStream.bands - 1)) + inStream.loFreq;
			num		= 0;

			if( freq > 0 ) {
				for( int i = 0; i < smpZones.size(); i++ ) {
					smp = (SmpZone) smpZones.elementAt( i );
					if( (smp.freqLo.val <= freq) && (smp.freqHi.val > freq) ) {
						ID[ num++ ] = i;
					}
				}
			}

			szls[ band ] = new SmpZoneLocal[ num ];
	
			for( int i = 0; i < num; i++ ) {
				szls[ band ][ i ]	= new SmpZoneLocal( szgs[ ID[ i ]], outStream, freq );
				maxIncr[ ID[ i ]]	= Math.max( maxIncr[ ID[ i ]], szls[ band ][ i ].smpIncr );
			}
		}

		// transform maxIncr to SmpGlobal.dataOff, alloc SmpGlobal.data
		for( int i = 0; i < szgs.length; i++ ) {
			if( smpFindex[ i ] != i ) continue;
			
			fNum = 1.0f;
			for( int j = 0; j < szgs.length; j++ ) {
				if( smpFindex[ j ] == i ) {
					fNum = Math.max( fNum, maxIncr[ j ]);
				}
			}
			num					= (int) (Nwing * fNum) + 10;
			szgs[ i ].dataOff	= num;
			smpData[ i ]		= new float[ (int) (((num << 1) + smpStreams[ i ].length) *
											 smpStreams[ i ].channels) ];
			szgs[ i ].data		= smpData[ i ];

			// clear overheads
			num *= smpStreams[ i ].channels;
			for( int j = 0; j < num; j++ ) {
				smpData[ i ][ j ]							= 0.0f;
				smpData[ i ][ smpData[ i ].length - j - 1 ]	= 0.0f;
			}
// System.out.println( i+": maxIncr="+fNum+" => dataOff="+szgs[ i ].dataOff );

			for( int j = 0; j < szgs.length; j++ ) {		// copy to all zones that refer to the same file
				if( (i == j) || (smpFindex[ j ] != i) ) continue;
				
				szgs[ j ].dataOff	= szgs[ i ].dataOff;
				smpData[ j ]		= smpData[ i ];
				szgs[ j ].data		= smpData[ i ];
			}
		}
		
		return szls;
	}
} // class SmpZoneLocal
