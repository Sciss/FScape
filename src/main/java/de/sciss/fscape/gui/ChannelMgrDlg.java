/*
 *  ChannelMgrDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		12-May-05	fixed bug in output normalizing
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  Processing module for splitting a
 *	multichannel file into several mono
 *	files or vice versa.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ChannelMgrDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_INPUTLIST		= 2;
	private static final int PR_OUTPUTLIST		= 3;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_CHANMODE		= 3;
	private static final int PR_NAMEMODE		= 4;
	private static final int PR_GAIN			= 0;		// pr.para
//	private static final int PR_RESIDUAL		= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_CHANMODE		= "ChanMode";
	private static final String PRN_NAMEMODE		= "NameMode";
	private static final String PRN_INPUTLIST		= "InputList";
	private static final String PRN_OUTPUTLIST	= "OutputList";

	private static final String[]	CHAN_NAMES		= { "Split channels", "Merge channels" };

	private static final int		CHAN_SPLIT		= 0;
	private static final int		CHAN_MERGE		= 1;

	private static final String[]	NAME_NAMES		= { "Spatial (L,C,R)", "Numbers (1,2,3)", "Letters (A,B,C)" };

	private static final int		NAME_SPATIAL	= 0;
	private static final int		NAME_NUMBERS	= 1;
	private static final int		NAME_LETTERS	= 2;

	private static final String[][]	SPATIAL_NAMES	= { {}, { "Mono" }, { "L", "R" }, { "L", "C", "R" }, { "L", "R", "Rs", "Ls" },
															{ "L", "C", "R", "Rs", "Ls" }, { "L", "C", "R", "Rs", "Ls", "LFE" },
															{ "L", "Lc", "C", "Rc", "R", "Rs", "Ls" }, { "L", "Lc", "C", "Rc", "R", "Rs", "Ls", "LFE" }
														  };

	private static final String[]		prText			= { "", "", "", "" };
	private static final String[]		prTextName		= { PRN_INPUTFILE, PRN_OUTPUTFILE, PRN_INPUTLIST, PRN_OUTPUTLIST };
	private static final int[]			prIntg			= { 0, 0, GAIN_ABSOLUTE, CHAN_MERGE, NAME_SPATIAL };
	private static final String[]		prIntgName		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE,
															PRN_CHANMODE, PRN_NAMEMODE };
	private static final Param[]		prPara			= { null };
	private static final String[]		prParaName		= { PRN_GAIN };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_CHANMODE		= GG_OFF_CHOICE		+ PR_CHANMODE;
	private static final int GG_NAMEMODE		= GG_OFF_CHOICE		+ PR_NAMEMODE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_INPUTLIST		= GG_OFF_OTHER		+ 0;
	private static final int GG_CMDINDEL		= GG_OFF_OTHER		+ 1;
	private static final int GG_CMDINDELALL		= GG_OFF_OTHER		+ 2;
	private static final int GG_CMDINUP			= GG_OFF_OTHER		+ 3;
	private static final int GG_CMDINDOWN		= GG_OFF_OTHER		+ 4;
	private static final int GG_OUTPUTLIST		= GG_OFF_OTHER		+ 10;
	private static final int GG_CMDOUTAUTO		= GG_OFF_OTHER		+ 11;
	private static final int GG_CMDOUTAUTOALL	= GG_OFF_OTHER		+ 12;
	private static final int GG_CMDOUTUP		= GG_OFF_OTHER		+ 13;
	private static final int GG_CMDOUTDOWN		= GG_OFF_OTHER		+ 14;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_OUTNUM			= "Please update # of output files";

	private String		outPath			= "";
	private String		outFileName		= "";
	private String		outFileExt		= "";
	
	protected JList ggInputList, ggOutputList;
	protected DefaultListModel  ggInputListModel, ggOutputListModel;
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ChannelMgrDlg()
	{
		super( "Channel Manager" );
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
			static_pr.para[ PR_GAIN ]		= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
//			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- other init --------
	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		Component[]			ggGain;
		PathField[]			ggInputs;
		JComboBox			ggChanMode, ggNameMode;
		JButton				ggCmd;
		JScrollPane			ggInputScroll, ggOutputScroll;
		BasicCellRenderer   bcr;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );
		
		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_CHANMODE:
				case GG_NAMEMODE:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
					recalcOutChanNum();
					outListToPathField();
					break;
				}
			}
		};

		ListSelectionListener lsl = new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent e )
			{
				int i;
				
				if( e.getSource() == ggInputList ) {
					i = ggInputList.getSelectedIndex();
					if( i != -1 ) {
						inListToPathField();
					}
				} else if( e.getSource() == ggOutputList ) {
					i = ggOutputList.getSelectedIndex();
					if( i != -1 ) {
						outListToPathField();
					}
				}
			}
		};
		
		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE:
					pr.text[ ID - GG_OFF_PATHFIELD ] = ((PathField) e.getSource()).getPath().getPath();
					setInput( pr.text[ ID - GG_OFF_PATHFIELD ]);
					recalcOutChanNum();
					break;

				case GG_OUTPUTFILE:
					pr.text[ ID - GG_OFF_PATHFIELD ] = ((PathField) e.getSource()).getPath().getPath();
					setOutput( pr.text[ ID - GG_OFF_PATHFIELD ]);
					break;
				}
			}
		};
		
		ActionListener al = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int		ID = gui.getItemID( e );
				int		i;
				int[]   idx;
				Object  o;

				switch( ID ) {

				// input

				case GG_CMDINUP:
				case GG_CMDINDOWN:
					i = ggInputList.getSelectedIndex();
					if( (i < (ID == GG_CMDINUP ? 1 : 0)) || ((ID == GG_CMDINDOWN) && (i >= ggInputListModel.size() - 1) )) break;
					
					o  = ggInputListModel.remove( i );
					i += (ID == GG_CMDINUP) ? -1 : 1;
					ggInputListModel.insertElementAt( o, i );
					ggInputList.setSelectedIndex( i );
					ggInputList.ensureIndexIsVisible( i );
					break;
					
				case GG_CMDINDEL:
					idx = ggInputList.getSelectedIndices();
					if( idx.length == 0 ) break;
					
					for( i = idx.length - 1; i >= 0; i-- ) {
						ggInputListModel.removeElementAt( idx[i] );
					}
					inListToPathField();
					recalcOutChanNum();
					break;

				case GG_CMDINDELALL:
					ggInputListModel.removeAllElements();
					inListToPathField();
					recalcOutChanNum();
					outListToPathField();
					break;

				// output

				case GG_CMDOUTUP:
				case GG_CMDOUTDOWN:
					i = ggOutputList.getSelectedIndex();
					if( (i < (ID == GG_CMDOUTUP ? 1 : 0)) || ((ID == GG_CMDOUTDOWN) && (i >= ggOutputListModel.size() - 1) )) break;
					
					o  = ggOutputListModel.remove( i );
					i += (ID == GG_CMDOUTUP) ? -1 : 1;
					ggOutputListModel.insertElementAt( o, i );
					ggOutputList.setSelectedIndex( i );
					ggOutputList.ensureIndexIsVisible( i );
					break;

				case GG_CMDOUTAUTO:
					i = ggOutputList.getSelectedIndex();
					if( i == -1 ) break;
					
					((OutputEntry) ggOutputListModel.elementAt( i )).auto = true;
					recalcOutChanNum();
					outListToPathField();
					break;
								
				case GG_CMDOUTAUTOALL:
					for( i = 0; i < ggOutputListModel.size(); i++ ) {
						((OutputEntry) ggOutputListModel.elementAt( i )).auto = true;
					}
					recalcOutChanNum();
					outListToPathField();
					break;
				}
			}
		};
		
	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Input(s)", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select an input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Add file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

	// -------- Command-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		ggInputListModel= new DefaultListModel();
		ggInputList		= new JList( ggInputListModel );
		bcr				= new BasicCellRenderer() {
			public Component getListCellRendererComponent( JList list, Object obj, int index, boolean isSelected,
														   boolean cellHasFocus )
			{
				super.getListCellRendererComponent( list, obj, index, isSelected, cellHasFocus );
				if( obj instanceof InputEntry ) {
					setText( ((InputEntry) obj).name );
				} else if( obj instanceof OutputEntry ) {
					setText( ((OutputEntry) obj).name );
				}
				return this;
			}
		};
		ggInputList.setCellRenderer( bcr );
		ggInputList.addListSelectionListener( lsl );
		con.gridwidth	= 5;
		con.gridheight	= 4;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		ggInputScroll   = new JScrollPane( ggInputList );
		gui.addGadget( ggInputScroll, GG_INPUTLIST );

		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridheight	= 1;
		con.weightx		= 0.025;
		con.weighty		= 0.0;
		ggCmd			= new JButton( "Remove" );
		con.gridwidth	= GridBagConstraints.RELATIVE;
		gui.addButton( ggCmd, GG_CMDINDEL, al );
		ggCmd			= new JButton( "Remove all" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addButton( ggCmd, GG_CMDINDELALL, al );
		ggCmd			= new JButton( "Move up" );
		con.gridwidth	= GridBagConstraints.RELATIVE;
		gui.addButton( ggCmd, GG_CMDINUP, al );
		ggCmd			= new JButton( "Move down" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addButton( ggCmd, GG_CMDINDOWN, al );
		gui.addLabel( new JLabel() );
		gui.addLabel( new JLabel() );

	// -------- Settings-Gadgets --------
		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggChanMode		= new JComboBox();
		for( int i = 0; i < CHAN_NAMES.length; i++ ) {
			ggChanMode.addItem( CHAN_NAMES[ i ]);
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Operation mode", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addChoice( ggChanMode, GG_CHANMODE, il );

	// -------- Output-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Output(s)", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select an output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0-$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filename", SwingConstants.RIGHT ));
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

	// -------- Command-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		ggOutputListModel= new DefaultListModel();
		ggOutputList	= new JList( ggOutputListModel );
		ggOutputList.setCellRenderer( bcr );
		ggOutputList.addListSelectionListener( lsl );
		con.gridwidth	= 5;
		con.gridheight	= 4;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		ggOutputScroll  = new JScrollPane( ggOutputList );
		gui.addGadget( ggOutputScroll, GG_OUTPUTLIST );

		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridheight	= 1;
		con.weightx		= 0.025;
		con.weighty		= 0.0;
		ggCmd			= new JButton( "Auto" );
		con.gridwidth	= GridBagConstraints.RELATIVE;
		gui.addButton( ggCmd, GG_CMDOUTAUTO, al );
		ggCmd			= new JButton( "Auto all" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addButton( ggCmd, GG_CMDOUTAUTOALL, al );
		ggCmd			= new JButton( "Move up" );
		con.gridwidth	= GridBagConstraints.RELATIVE;
		gui.addButton( ggCmd, GG_CMDOUTUP, al );
		ggCmd			= new JButton( "Move down" );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addButton( ggCmd, GG_CMDOUTDOWN, al );

		ggNameMode		= new JComboBox();
		for( int i = 0; i < NAME_NAMES.length; i++ ) {
			ggNameMode.addItem( NAME_NAMES[ i ]);
		}
		con.gridwidth	= GridBagConstraints.RELATIVE;
		gui.addLabel( new JLabel( "Auto scheme", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggNameMode, GG_NAMEMODE, il );

		gui.addLabel( new JLabel() );


		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		int				num, i;
		Properties		p;
		InputEntry		ie;
		OutputEntry		oe;

		String			suckMyPlasma = pr.text[ PR_OUTPUTFILE ];

// System.out.println( "A: "+pr.text[ PR_OUTPUTFILE ]);

		super.fillGUI();
		super.fillGUI( gui );

// System.out.println( "B: "+pr.text[ PR_OUTPUTFILE ]);

//			gui.stringToPathField( null, GG_INPUTFILE );

		ggInputListModel.removeAllElements();
		p	= Presets.valueToProperties( getPropertyArray().text[ PR_INPUTLIST ]);
		num	= p.size();

		for( i = 0; i < num; i++ ) {
			ie = InputEntry.valueOf( p.getProperty( String.valueOf( i )));
			ggInputListModel.addElement( ie );
		}

		ggOutputListModel.removeAllElements();
		p	= Presets.valueToProperties( getPropertyArray().text[ PR_OUTPUTLIST ]);
		num	= p.size();

		for( i = 0; i < num; i++ ) {
			oe = OutputEntry.valueOf( p.getProperty( String.valueOf( i )));
			ggOutputListModel.addElement( oe );
		}
		
		pr.text[ PR_OUTPUTFILE ] = suckMyPlasma;
		gui.stringToPathField( pr.text[ PR_OUTPUTFILE ], GG_OUTPUTFILE );
		calcOutNameComponents( pr.text[ PR_OUTPUTFILE ]);
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		Properties	p;
		int			i, num;
	
		super.fillPropertyArray();
		super.fillPropertyArray( gui );
		
		num	= ggInputListModel.size();
		p	= new Properties();
		for( i = 0; i < num; i++ ) {
			p.put( String.valueOf( i ), ((InputEntry) ggInputListModel.elementAt( i )).toString() );
		}
		getPropertyArray().text[ PR_INPUTLIST ] = Presets.propertiesToValue( p );

		num = ggOutputListModel.size();
		p	= new Properties();
		for( i = 0; i < num; i++ ) {
			p.put( String.valueOf( i ), ((OutputEntry) ggOutputListModel.elementAt( i )).toString() );
		}
		getPropertyArray().text[ PR_OUTPUTLIST ] = Presets.propertiesToValue( p );
	}

// -------- Processor Interface --------
		
	protected void process()
	{
		int					maxInChanNum, len;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile[]			inF				= null;
		AudioFileDescr[]	inStream		= null;
		AudioFile[]			outF			= null;
		AudioFileDescr[]	outStream		= null;
		float[][]			outBuf;
		float[][]			inBuf;
		float[]				convBuf1, convBuf2;


		float				gain			= 1.0f;			// gain abs amp
		final Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		int					numInputs, numOutputs, outChanNum, totOutChan, outLength;

		float				maxAmp			= 0.0f;

		final String		inPath			= pr.text[ PR_INPUTFILE ].substring( 0, pr.text[ PR_INPUTFILE ].lastIndexOf( File.separatorChar ) + 1 );

		PathField			ggOutput;
		
topLevel: try {
		// ---- open input, output; init ----

			numInputs	= ggInputListModel.size();
			inF			= new AudioFile[ numInputs ];
			inStream	= new AudioFileDescr[ numInputs ];

			for( int i = 0; i < numInputs; i++ ) {
				inF[i]		= null;
				inStream[i]	= null;
			}
			
			totOutChan	= 0;
			maxInChanNum= 0;
			outLength	= 0x7FFFFFFF;
			
			for( int i = 0; (i < numInputs) && threadRunning; i++ ) {
				inF[i]		= AudioFile.openAsRead( new File( inPath + ((InputEntry) ggInputListModel.elementAt( i )).name ));
				inStream[i]	= inF[i].getDescr();
				maxInChanNum= Math.max( maxInChanNum, inStream[i].channels );
				totOutChan += inStream[i].channels;
				outLength	= (int) Math.min( outLength, inStream[i].length );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			if( (outLength < 1) || (totOutChan < 1) ) throw new EOFException( ERR_EMPTY );

			numOutputs	= pr.intg[ PR_CHANMODE ] == CHAN_SPLIT ? totOutChan : 1;
			outChanNum	= totOutChan / numOutputs;
			if( numOutputs > ggOutputListModel.size() ) throw new IOException( ERR_OUTNUM );

			outF		= new AudioFile[ numOutputs ];
			outStream	= new AudioFileDescr[ numOutputs ];
			outBuf		= new float[ outChanNum ][ 8192 ];
			inBuf		= new float[ maxInChanNum ][ 8192 ];	// maximum input chan.num -> buffer will work for all input files

			for( int i = 0; i < numOutputs; i++ ) {
				outF[i]			= null;
				outStream[i]	= null;
			}

			ggOutput		= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			calcOutNameComponents( pr.text[ PR_OUTPUTFILE ]);

			for( int i = 0; (i < numOutputs) && threadRunning; i++ ) {
				outStream[i]= new AudioFileDescr( inStream[0] );
				ggOutput.fillStream( outStream[i] );
				outStream[i].channels	= outChanNum;
				outStream[i].file		= new File(
					outPath + outFileName + (pr.intg[ PR_CHANMODE ] == CHAN_SPLIT ?
						("-" + ((OutputEntry) ggOutputListModel.elementAt( i )).name) : "") + outFileExt );
				outF[i]		= AudioFile.openAsWrite( outStream[i] );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			progLen	= (long) totOutChan * (long) outLength * 2;
			progOff	= 0;

		// ---- calc peak amp ----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				progLen += (long) totOutChan * (long) outLength;

				for( int i = 0; (i < numInputs) && threadRunning; i++ ) {
					for( int framesRead = 0; (framesRead < outLength) && threadRunning; ) {
						len = Math.min( outLength - framesRead, 8192 );
						inF[ i ].readFrames( inBuf, 0, len );
						framesRead += len;
						progOff    += len * inStream[i].channels;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
						
						for( int ch = 0; ch < inStream[i].channels; ch++ ) {
							convBuf1 = inBuf[ ch ];
							for( int j = 0; j < len; j++ ) {
								f1 = Math.abs( convBuf1[ j ]);
								if( f1 > maxAmp ) {
									maxAmp = f1;
								}
							}
						}
					}
					inF[ i ].seekFrame( 0 );	// return to beginning for copy core
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								   new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
			} else {
				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}

		// ---- copy core ----

			for( int framesWritten = 0; (framesWritten < outLength) && threadRunning; ) {

				len	= Math.min( outLength - framesWritten, 8192 );

				for( int i = 0, j = 0, ch = 0; (i < numInputs) && threadRunning; i++ ) {
					inF[i].readFrames( inBuf, 0, len );
					progOff    += len * inStream[i].channels;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );

					for( int k = 0; k < inStream[i].channels; k++ ) {
						if( gain == 1.0f ) {
							System.arraycopy( inBuf[k], 0, outBuf[ch], 0, len );
						} else {
							convBuf1 = inBuf[k];
							convBuf2 = outBuf[ch];
							for( int m = 0; m < len; m++ ) {
								convBuf2[m] = convBuf1[m] * gain;
							}
						}
						if( ++ch == outChanNum ) {
							outF[j].writeFrames( outBuf, 0, len );
							progOff    += len * outChanNum;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );

							ch = 0;
							j++;
						}
					}

					if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
						for( int k = 0; k < inStream[i].channels; k++ ) {
							convBuf1 = inBuf[ k ];
							for( int m = 0; m < len; m++ ) {
								f1 = Math.abs( convBuf1[ m ]);
								if( f1 > maxAmp ) {
									maxAmp = f1;
								}
							}
						}
					}
				}
				framesWritten += len;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- finish ----

			for( int i = 0; i < numInputs; i++ ) {
				inF[i].close();
				inF[i] 			= null;
				inStream[i]		= null;
			}
			for( int i = 0; i < numOutputs; i++ ) {
				outF[i].close();
				outF[i] 		= null;
				outStream[i]	= null;
			}

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
			outBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			for( int i = 0; i < outF.length; i++ ) {
				if( outF[i] != null ) {
					outF[i].cleanUp();
				}
			}
		}
		if( inF != null ) {
			for( int i = 0; i < inF.length; i++ ) {
				if( inF[i] != null ) {
					inF[i].cleanUp();
				}
			}
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

		int				i;
		InputEntry		ie;

	// ---- Header lesen ----
		try {
			f			= AudioFile.openAsRead( new File( fname ));
			stream		= f.getDescr();
			f.close();

			ie			= new InputEntry();
			ie.name		= fname.substring( fname.lastIndexOf( File.separatorChar ) + 1 );
			ie.chan		= stream.channels;

			if( ggInputList == null ) {
				obscure();
				return;
			}
			i = ggInputList.getSelectedIndex();
			if( i >= 0 ) {
				ggInputList.clearSelection(); // deselect( i );
			}
			i = ggInputListModel.size();
			ggInputListModel.add( i, ie );
			ggInputList.setSelectedIndex( i );
			ggInputList.ensureIndexIsVisible( i );

//			guiToBatchEntry( be );
//			recalcChannelSituation();

		} catch( IOException ignored) {}
	}

	protected String calcOutNameComponents( String fname )
	{
		int				fIdx			= fname.lastIndexOf( File.separatorChar ) + 1;
		int				eIdx			= fname.lastIndexOf( '.' );
		int				cIdx			= fname.lastIndexOf( '-' );
		String			cExt;
		
		outPath			= fname.substring( 0, fIdx );
		if( (eIdx <= cIdx) || (eIdx <= fIdx) ) {
			eIdx = fname.length();
		}
		if( cIdx < fIdx ) {
			cIdx = eIdx;
		}
		outFileName	= fname.substring( fIdx, cIdx );
		outFileExt	= fname.substring( eIdx );
		cExt		= fname.substring( Math.min( eIdx, cIdx + 1 ), eIdx );

		return cExt;
	}

	/**
	 *	Neues Outputfile setzen
	 */
	protected void setOutput( String fname )
	{
		String			cExt	= calcOutNameComponents( fname );
		int				i;
		OutputEntry		currentOutEntry;
		
		i = ggOutputList.getSelectedIndex();
		if( i == -1 ) return;
		currentOutEntry = ((OutputEntry) ggOutputListModel.elementAt( i ));
		
		if( (cExt.equals( currentOutEntry.name )) || (pr.intg[ PR_CHANMODE ] == CHAN_MERGE) ) return;
		
		currentOutEntry.name = cExt;
		currentOutEntry.auto = false;

		ggOutputListModel.setElementAt( currentOutEntry, i );
		ggOutputList.ensureIndexIsVisible( i );
		ggOutputList.setSelectedIndex( i );
	}
	
	protected void obscure()
	{
		if( this.isVisible() ) {
			JOptionPane.showMessageDialog( getComponent(), "Oscure! Gadget missing..." );
		}
	}

	/**
	 *	Copy filename from input list to input path field
	 */
	protected void inListToPathField()
	{
		int i = ggInputList.getSelectedIndex();
		InputEntry currentInEntry;
	
		if( i == -1 ) return;
		
		currentInEntry = (InputEntry) ggInputListModel.elementAt( i );

		PathField	ggInputFile = (PathField) gui.getItemObj( GG_INPUTFILE );
		String		fPath;
		
		if( ggInputFile == null ) {
			obscure();
			return;
		}
		
		fPath	= ggInputFile.getPath().getPath();
		ggInputFile.setPath( new File( fPath.substring( 0, fPath.lastIndexOf( File.separatorChar ) + 1 ) + currentInEntry.name ));
	}

	/**
	 *	Copy filename from output list to output path field
	 */
	protected void outListToPathField()
	{
		PathField	ggOutputFile	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
		int			i				= ggOutputList.getSelectedIndex();
		OutputEntry currentOutEntry = (i != -1) ? (OutputEntry) ggOutputListModel.elementAt( i ) : null;
		
		if( ggOutputFile == null ) {
			obscure();
			return;
		}
		
		ggOutputFile.setPath( new File( outPath + outFileName + 
							  (pr.intg[ PR_CHANMODE ] == CHAN_SPLIT ? ("-" + (currentOutEntry == null ? "" : currentOutEntry.name)) : "") + outFileExt ));
	}

	protected void recalcOutChanNum()
	{
		int			i, ch, oldIdx;
		OutputEntry	oe;
	
		for( i = 0, ch = 0; i < ggInputListModel.size(); i++ ) {
			ch += ((InputEntry) ggInputListModel.elementAt( i )).chan;
		}
		
		if( pr.intg[ PR_CHANMODE ] == CHAN_MERGE ) {
			ch = Math.min( 1, ch );
		}
		int guiOutChanNum = ch;
		
		oldIdx = ggOutputList.getSelectedIndex();
		if( oldIdx >= 0 ) {
			ggOutputList.clearSelection(); // ( oldIdx );
		}
		if( guiOutChanNum < ggOutputListModel.size() ) {
			for(i = ggOutputListModel.size() - 1; i >= guiOutChanNum; i-- ) {
				ggOutputListModel.removeElementAt( i );
			}
		}
		for( i = 0; i < ggOutputListModel.size(); i++ ) {
			oe		= (OutputEntry) ggOutputListModel.elementAt( i );
			if( oe.auto ) {
				oe.name = createAutoExt( i, guiOutChanNum);
				ggOutputListModel.setElementAt( oe, i );
			}
		}
		while( guiOutChanNum > ggOutputListModel.size() ) {
			oe		= new OutputEntry();
			oe.auto = true;
			oe.name	= createAutoExt( ggOutputListModel.size(), guiOutChanNum);
			ggOutputListModel.addElement( oe );
		}
		if( (oldIdx >= 0) && (oldIdx < guiOutChanNum) ) {
			ggOutputList.setSelectedIndex( oldIdx );
			ggOutputList.ensureIndexIsVisible( oldIdx );
		}
	}

	protected String createAutoExt( int index, int totalCount )
	{
		int		mode = pr.intg[ PR_NAMEMODE ];
		String	cExt = "";
	
		if( ((mode == NAME_SPATIAL) && (totalCount > 8))  || 
			((mode == NAME_LETTERS) && (totalCount > 26)) ) mode = NAME_NUMBERS;

		switch( mode ) {
		case NAME_SPATIAL:
			cExt = SPATIAL_NAMES[ totalCount ][ index ];
			break;
		case NAME_NUMBERS:
			cExt = String.valueOf( index + 1 );
			break;
		case NAME_LETTERS:
			cExt = String.valueOf( ((char) (index + 65)) );
			break;
		default:
			break;
		}
		
		return cExt;
	}
}
// class ChannelMgrDlg

/**
 *	Interne Klasse fuer die Listeneintraege
 */
class InputEntry
{
	String			name;
	int				chan;
	
	private final static String	PR_NAME		= "nam";
	private final static String	PR_CHAN		= "cha";
	
// -------- StringComm Methoden --------

	public String toString()
	{
		Properties	p	= new Properties();

		p.put( PR_NAME,		this.name );
		p.put( PR_CHAN,		String.valueOf( this.chan ));
		return( Presets.propertiesToValue( p ));
	}
	
	public static InputEntry valueOf( String s )
	{
		Properties	p		= Presets.valueToProperties( s );
		InputEntry	ie		= new InputEntry();

		ie.name				= p.getProperty( PR_NAME );
		try {
			ie.chan			= Integer.parseInt( p.getProperty( PR_CHAN ));
		}
		catch( NumberFormatException ignored) {}
		catch( NullPointerException ignored) {}
		
		return ie;
	}
}

/**
 *	Interne Klasse fuer die Listeneintraege
 */
class OutputEntry
{
	String			name;
	boolean			auto;
	
	private final static String	PR_NAME		= "nam";
	private final static String	PR_AUTO		= "aut";
	
// -------- StringComm Methoden --------

	public String toString()
	{
		Properties	p	= new Properties();

		p.put( PR_NAME,		this.name );
		p.put( PR_AUTO,		String.valueOf( this.auto ));
		return( Presets.propertiesToValue( p ));
	}
	
	public static OutputEntry valueOf( String s )
	{
		Properties	p		= Presets.valueToProperties( s );
		OutputEntry	oe		= new OutputEntry();

		oe.name				= p.getProperty( PR_NAME );
		try {
			oe.auto			= Boolean.valueOf( p.getProperty( PR_AUTO )).booleanValue();
		}
		catch( NumberFormatException ignored) {}
		catch( NullPointerException ignored) {}
		
		return oe;
	}
}
