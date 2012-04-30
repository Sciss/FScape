/*
 *  SpliceDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
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
 *		25-Jun-06	created
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

/**
 *  Processing module for splitting
 *	up a file into several splices.
 *	use Concat to re-glue these splices.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 25-Jun-06
 */
public class SpliceDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_INITIALSKIP			= 0;		// pr.para
	private static final int PR_SPLICELENGTH		= 1;
	private static final int PR_SKIPLENGTH			= 2;
	private static final int PR_AUTONUM				= 3;
	private static final int PR_FINALSKIP			= 4;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_AUTOSCALE			= 0;		// pr.bool
	private static final int PR_SEPARATEFILES		= 1;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_INITIALSKIP		= "InitialSkip";
	private static final String PRN_SPLICELENGTH	= "SpliceLen";
	private static final String PRN_SKIPLENGTH		= "SkipLen";
	private static final String PRN_AUTONUM			= "AutoNum";
	private static final String PRN_FINALSKIP		= "FinalSkip";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_AUTOSCALE		= "AutoScale";
	private static final String PRN_SEPARATEFILES	= "SeparateFiles";

	private static final String		prText[]		= { "", "" };
	private static final String		prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]		= { 0, 0 };
	private static final String		prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES };
	private static final Param		prPara[]		= { null, null, null, null, null };
	private static final String		prParaName[]	= { PRN_INITIALSKIP, PRN_SPLICELENGTH, PRN_SKIPLENGTH, PRN_AUTONUM, PRN_FINALSKIP };
	private static final boolean	prBool[]		= { false, true };
	private static final String		prBoolName[]	= { PRN_AUTOSCALE, PRN_SEPARATEFILES };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_INITIALSKIP			= GG_OFF_PARAMFIELD	+ PR_INITIALSKIP;
	private static final int GG_SPLICELENGTH		= GG_OFF_PARAMFIELD	+ PR_SPLICELENGTH;
	private static final int GG_SKIPLENGTH			= GG_OFF_PARAMFIELD	+ PR_SKIPLENGTH;
	private static final int GG_AUTONUM				= GG_OFF_PARAMFIELD	+ PR_AUTONUM;
	private static final int GG_FINALSKIP			= GG_OFF_PARAMFIELD	+ PR_FINALSKIP;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_AUTOSCALE			= GG_OFF_CHECKBOX	+ PR_AUTOSCALE;
	private static final int GG_SEPARATEFILES		= GG_OFF_CHECKBOX	+ PR_SEPARATEFILES;
//	private static final int GG_CURRENTINFO			= GG_OFF_OTHER		+ 0;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SpliceDlg()
	{
		super( "Splice" );
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
			static_pr.para[ PR_INITIALSKIP ]	= new Param(    0.0, Param.ABS_MS );
			static_pr.para[ PR_SPLICELENGTH ]	= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_SKIPLENGTH ]		= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_AUTONUM ]		= new Param(      2, Param.NONE );
			static_pr.para[ PR_FINALSKIP ]		= new Param(    0.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- Misc init --------

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		PathField[]			ggInputs;
		ParamField			ggParam, ggSpliceLength;
		ParamSpace[]		spcOffset, spcLength;
		ParamSpace			spc;
		JCheckBox			ggCheck;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				final boolean b = ((JCheckBox) e.getSource()).isSelected();
				pr.bool[ PR_AUTOSCALE ] = b;
				gui.getItemObj( GG_AUTONUM ).setEnabled( b );
				gui.getItemObj( GG_FINALSKIP ).setEnabled( b );
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
//					analyseFileNames();
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

//		ggCurrentInfo	= new JTextField();
//		ggCurrentInfo.setEditable( false );
//		ggCurrentInfo.setBackground( null );
//		con.weightx		= 0.1;
//		con.gridwidth	= 1;
//		gui.addLabel( new JLabel( "Current input", SwingConstants.RIGHT ));
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		con.weightx		= 0.9;
//		gui.addTextField( ggCurrentInfo, GG_CURRENTINFO, al );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Cut$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		
		ggCheck			= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Separate files", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggCheck, GG_SEPARATEFILES, il );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Splice Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcLength		= new ParamSpace[ 3 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]	= Constants.spaces[ Constants.ratioTimeSpace ];
		spcOffset		= new ParamSpace[ 3 ];
		spc				= new ParamSpace( Constants.spaces[ Constants.absMsSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
		spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
		spcOffset[0]	= spc; // Constants.spaces[ Constants.offsetMsSpace ];
		spc				= new ParamSpace( Constants.spaces[ Constants.absBeatsSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
		spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
		spcOffset[1]	= spc; // Constants.spaces[ Constants.offsetBeatsSpace ];
//		spc				= new ParamSpace( Constants.spaces[ Constants.fTimeSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
		spc				= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//		spc.min			= Double.NEGATIVE_INFINITY;
		spc				= new ParamSpace( Double.NEGATIVE_INFINITY, spc.max, spc.inc, spc.unit );
		spcOffset[2]	= spc; // Constants.spaces[ Constants.factorTimeSpace ];

		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;

		ggParam			= new ParamField( spcLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Splice Length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggParam, GG_SPLICELENGTH, null );

		ggSpliceLength	= new ParamField( spcLength );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Initial Skip", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggSpliceLength, GG_INITIALSKIP, null );

		ggParam		= new ParamField( spcOffset );
//		ggParam.setReference( ggSpliceLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Skip Length", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggParam, GG_SKIPLENGTH, null );

		ggParam			= new ParamField( spcLength );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Final Skip", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggParam, GG_FINALSKIP, null );
		ggParam.setEnabled( false );	// XXX funzt niet???

		ggCheck			= new JCheckBox();
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Automatic rescale", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addCheckbox( ggCheck, GG_AUTOSCALE, il );

		ggParam			= new ParamField( new ParamSpace( 1, Double.POSITIVE_INFINITY, 1, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "# of Splices", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggParam, GG_AUTONUM, null );
		ggParam.setEnabled( false );	// XXX funzt niet???

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
		long				progOff, progLen;

		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inDescr;
		AudioFileDescr		outDescr;
		float[][]			inBuf;

		long				numSlices, initialSkip, finalSkip, sliceLen, sliceSkip, inLength, off, stop;
		int					len, idx, inBufLen;
		double				d1, d2, d3;
		Param				ref;
		boolean				dirty, separate;
		String				str, sepBeg = null, sepEnd = null;

		PathField			ggOutput;
		
topLevel: try {
		// ---- open input, output; init ----

			inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inDescr		= inF.getDescr();
			inLength	= inDescr.length;
			if( inDescr.channels * inLength <= 0 ) throw new EOFException( ERR_EMPTY );
			
			ref			= new Param( AudioFileDescr.samplesToMillis( inDescr, inLength ), Param.ABS_MS );
			d1			= AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_SPLICELENGTH ], Param.ABS_MS, ref, null ).val );
			d2			= AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_SKIPLENGTH ], Param.ABS_MS, ref, null ).val );
			initialSkip	= (long) (AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_INITIALSKIP ], Param.ABS_MS, ref, null ).val ) + 0.5);
			finalSkip	= (long) (AudioFileDescr.millisToSamples( inDescr, Param.transform( pr.para[ PR_FINALSKIP ], Param.ABS_MS, ref, null ).val ) + 0.5);

			if( pr.bool[ PR_AUTOSCALE ]) {
				numSlices	= (long) (pr.para[ PR_AUTONUM ].val + 0.5);
				d2			= Math.max( d2, -d1 + 1 );
				d3			= Math.max( 0L, inLength - initialSkip - finalSkip ) / (d1 * numSlices + (d2 * numSlices - 1));
				sliceLen	= (long) (d1 * d3 + 0.5);
				sliceSkip	= (long) (d2 * d3 + 0.5);
			} else {
				sliceLen	= (long) (d1 + 0.5);
				sliceSkip	= (long) (d2 + 0.5);
				if( sliceLen + sliceSkip == 0L ) {
					sliceSkip = -sliceLen + 1;
				}
				if( sliceLen + sliceSkip < 0 ) {
//					initialSkip + (sliceLen + sliceSkip) * numSlices <! 0
					numSlices = (-initialSkip - 1) / (sliceLen + sliceSkip) + 2;
				} else {
//					initialSkip + (sliceLen + sliceSkip) * numSlices >=! length
					numSlices	= (inDescr.length - initialSkip - 1) / (sliceLen + sliceSkip) + 1;
				}
			}
			
			separate	= pr.bool[ PR_SEPARATEFILES ];

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outDescr	= new AudioFileDescr( inDescr );
			ggOutput.fillStream( outDescr );
			if( separate ) {
				str		= outDescr.file.getName();
				idx		= str.lastIndexOf( '.' );
				if( idx == -1 ) idx = str.length();
				sepBeg	= str.substring( 0, idx );
				sepEnd	= str.substring( idx );
			} else {
				outF	= AudioFile.openAsWrite( outDescr );
			}
	// .... check running ....
			if( !threadRunning ) break topLevel;
			
			inBufLen		= (int) Math.min( 8192, sliceLen );
			inBuf			= new float[ inDescr.channels ][ inBufLen ];

			progOff			= 0;
			progLen			= numSlices * sliceLen;
			off				= initialSkip;
			dirty			= false;

			for( long n = 0; (n < numSlices) && threadRunning; n++ ) {
				if( separate ) {
					outDescr.file = new File( outDescr.file.getParentFile(), sepBeg + String.valueOf( n + 1 ) + sepEnd );
					outF	= AudioFile.openAsWrite( outDescr );
				}
				stop = off + sliceLen;
				if( off < 0 ) {	// this shouldn't theoretically happen; but it does ... ;-|
					if( dirty ) {
						Util.clear( inBuf );
						dirty = false;
					}
					do {
						len			   = (int) Math.min( inBufLen, -off );
						outF.writeFrames( inBuf, 0, len );
						off			  += len;
						progOff		   = progOff + len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					} while( off < 0 );
				}
				if( off < inLength ) {
					dirty	= true;
					inF.seekFrame( off );
					len		= (int) Math.min( inBufLen, Math.min( stop - off, inLength - off ));
					do {
						inF.readFrames( inBuf, 0, len );
						outF.writeFrames( inBuf, 0, len );
						off			  += len;
						progOff		   = progOff + len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
						len			   = (int) Math.min( inBufLen, Math.min( stop - off, inLength - off ));
					} while( len > 0 );
				}
				if( off < stop ) {
					if( dirty ) {
						Util.clear( inBuf );
						dirty = false;
					}
					do {
						len			   = (int) Math.min( inBufLen, stop - off );
						outF.writeFrames( inBuf, 0, len );
						off			  += len;
						progOff		   = progOff + len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					} while( off < stop );
				}
				off = off + sliceSkip;
				if( separate ) {
					outF.close();
					outF = null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			if( !separate ) {
				outF.close();
				outF	= null;
			}
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			outDescr	= null;
			inBuf		= null;
			inDescr		= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
	} // process()

// -------- private Methoden --------

	/*
	 *	Neues Inputfile setzen
	 */
	protected void setInput( String fname )
	{
		final AudioFile			f;
		final AudioFileDescr	stream;
		final Param				ref;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

			ref		= new Param( AudioFileDescr.samplesToMillis( stream, stream.length ), Param.ABS_MS );
			((ParamField) gui.getItemObj( GG_INITIALSKIP )).setReference( ref );
			((ParamField) gui.getItemObj( GG_FINALSKIP )).setReference( ref );
			((ParamField) gui.getItemObj( GG_SPLICELENGTH )).setReference( ref );
			((ParamField) gui.getItemObj( GG_SKIPLENGTH )).setReference( ref );
		} catch( IOException e1 ) {}
	}

//	/**
//	 *	Maske zur Berechnung der Chunk Filenamen erstellen
//	 *
//	 *	gibt Zahl der Files zurueck oder Null bei Fehler
//	 */
//	protected int analyseFileNames()
//	{
//		String s1	= pr.text[ PR_INPUTFILE ];
//		String s2	= pr.text[ PR_INPUTFILE2 ];
//		int len1	= s1.length();
//		int	len2	= s2.length();
//		int	i, j, numStart, numEnd, numMin, numMax, num;
//		char[]	numPad;
//		DecimalFormat	numForm;
//		
//		// Mechanismus: erste Abweichung links->rechts + erste Abweichung rechts->links
//		// finden, dann Bereich auf evtl. benachbarte numerische Charaktere ausdehnen
//		// und als Integer interpretieren
//		
//		for( numStart = 0; numStart < Math.min( len1, len2 ); numStart++ ) {
//			if( s1.charAt( numStart ) != s2.charAt( numStart )) break;
//		}
//		for( ; numStart > 0; numStart-- ) {
//			if( !Character.isDigit( s1.charAt( numStart - 1 )) ||
//				!Character.isDigit( s2.charAt( numStart - 1 ))) break;
//		}
//		for( numEnd = 0; numEnd < Math.min( len1, len2 ); numEnd++ ) {
//			if( s1.charAt( len1 - numEnd - 1 ) != s2.charAt( len2 - numEnd - 1 )) break;
//		}
//		for( ; numEnd > 0; numEnd-- ) {
//			if( !Character.isDigit( s1.charAt( len1 - numEnd )) ||
//				!Character.isDigit( s2.charAt( len2 - numEnd ))) break;
//		}
////System.out.println( "numStart "+numStart+"; numEnd "+numEnd+"; len1 "+len1+"; len2 "+len2 );
//
//		try {
//			i		= Integer.parseInt( s1.substring( numStart, len1 - numEnd ));
//			j		= Integer.parseInt( s2.substring( numStart, len2 - numEnd ));
//			numMin	= Math.min( i, j );
//			numMax	= Math.max( i, j );
//			num		= numMax - numMin + 1;
//			gui.stringToJTextField( num + " files detected.", GG_CURRENTINFO );
//			
//			fileNameArgs[1]	= s1.substring( 0, numStart );
//			fileNameArgs[2]	= s1.substring( len1 - numEnd );
//			fileNameArgs[3]	= new Integer( numMin );
//			i				= Math.min( len1, len2 ) - numEnd - numStart;
//			numPad			= new char[ i + Math.max( len1, len2 ) - Math.min( len1, len2 )];
//			for( j = 0; j < i; j++ ) numPad[j] = '0';
//			fileNameArgs[4]	= new String( numPad );
//			
//			return num;
//			
//		} catch( NumberFormatException e1 ) {
//			numMin	= -1;
//			numMax	= -1;
//			gui.stringToJTextField( "Filename analysis failed!", GG_CURRENTINFO );
//			return 0;
//		}
//	}

//	/*
//	 *	Generiert Filenamen aus mit analyseFilenames() erstellter Maske
//	 *
//	 *	ID laueft von 0 bis numFiles-1
//	 */
//	protected String synthesizeFileName( int ID )
//	{
//		String s		= String.valueOf( ID + ((Integer) fileNameArgs[3]).intValue() );
//		fileNameArgs[0]	= ((String) fileNameArgs[4]).substring( s.length() ) + s;
//	
//		return( fileNameForm.format( FILENAMEPTRN, fileNameArgs ));
//	}
}
// class SpliceDlg