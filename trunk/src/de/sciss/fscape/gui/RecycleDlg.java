/*
 *  RecycleDlg.java
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
 *		03-Mar-05	added 'Analyze' button + equal power cross fading option, uses indicateOutputWrite().
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;
import de.sciss.io.Region;
import de.sciss.io.Span;

/**
 *	Processing module that looks at a source file and a compacted
 *	file and puts out all the stuff which wasn't used in the compacted file.
 *	An optional crossfade is performed between adjectant chunks to avoid
 *	clicks. If for example the original file is 20 seconds long and the
 *	compacted version consists of the material 5.5...15.5 seconds and padding
 *	is set to 500 millisec, the recycled output will consist of the chunks
 *	0...6 seconds and 15...20 seconds (re original file). if crossfade is
 *	set to 1.0 sec, the output file will contain the original material 0...5 seconds,
 *	followed by a 1 second crossfade of material 5...6 seconds versus 14...15 seconds,
 *	followed by an optional marker named 'Cut', followed by material 15...20 seconds
 *	(hence total duration 11 sec).
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class RecycleDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_ORIGINFILE	= 0;		// pr.text
	private static final int PR_COMPACTFILE	= 1;
	private static final int PR_OUTPUTFILE	= 2;
	private static final int PR_OUTPUTTYPE	= 0;		// pr.intg
	private static final int PR_OUTPUTRES	= 1;
	private static final int PR_MARKERS		= 0;		// pr.bool
	private static final int PR_MINMATCH	= 0;		// pr.para
	private static final int PR_MINRECYCLE	= 1;
	private static final int PR_PADDING		= 2;
	private static final int PR_CMPSPACING	= 3;
	private static final int PR_CROSSFADE	= 4;

	private static final String PRN_ORIGINFILE	= "OriginFile";
	private static final String PRN_COMPACTFILE	= "CompactFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";
	private static final String PRN_MARKERS		= "Markers";
	private static final String PRN_MINMATCH	= "MinMatch";
	private static final String PRN_MINRECYCLE	= "MinRecycle";
	private static final String PRN_PADDING		= "Padding";
	private static final String PRN_CMPSPACING	= "CmpSpacing";
	private static final String PRN_CROSSFADE	= "CrossFade";

	private static final String		prText[]		= { "", "", "" };
	private static final String		prTextName[]	= { PRN_ORIGINFILE, PRN_COMPACTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]		= { 0, 0 };
	private static final String		prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES };
	private static final boolean	prBool[]		= { true };
	private static final String		prBoolName[]	= { PRN_MARKERS };
	private static final Param		prPara[]		= { null, null, null, null, null };
	private static final String		prParaName[]	= { PRN_MINMATCH, PRN_MINRECYCLE, PRN_PADDING, PRN_CMPSPACING, PRN_CROSSFADE };

	private static final int GG_ORIGINFILE			= GG_OFF_PATHFIELD	+ PR_ORIGINFILE;
	private static final int GG_COMPACTFILE			= GG_OFF_PATHFIELD	+ PR_COMPACTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_MARKERS				= GG_OFF_CHECKBOX	+ PR_MARKERS;
	private static final int GG_MINMATCH			= GG_OFF_PARAMFIELD	+ PR_MINMATCH;
	private static final int GG_MINRECYCLE			= GG_OFF_PARAMFIELD	+ PR_MINRECYCLE;
	private static final int GG_PADDING				= GG_OFF_PARAMFIELD	+ PR_PADDING;
	private static final int GG_CMPSPACING			= GG_OFF_PARAMFIELD	+ PR_CMPSPACING;
	private static final int GG_CROSSFADE			= GG_OFF_PARAMFIELD	+ PR_CROSSFADE;
	private static final int GG_INFOFIELD			= GG_OFF_OTHER		+ 0;
	private static final int GG_ANALYZE				= GG_OFF_OTHER		+ 1;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNUM			= "Input files must have same # of channels";

	private static final String	MARK_CUT			= "Cut";

	private boolean regionsKnown					= false;		// true nach "Analyze"; false nach neuer Input-Wahl
	private boolean threadJustAnalyze				= false;		// true = find peak; false = change gain
	private final java.util.List regionList			= new ArrayList();
	private JTextField ggInfoField;

	private final MessageFormat	msgInfoField		= new MessageFormat(
		"{0,choice,-1#[É|0#}"+
		"{1,choice,0#No regions|1#One region|1<{1,number,integer} regions}"+
		"{1,choice,0#|1# with total duration of {2}}"+
		" to recycle{0,choice,-1#É]|0#}",
		Locale.US );

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public RecycleDlg()
	{
		super( "Recycle Or Die" );
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
			static_pr.para[ PR_MINMATCH ]		= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_MINRECYCLE ]		= new Param(  100.0, Param.ABS_MS );
			static_pr.para[ PR_PADDING ]		= new Param( 1000.0, Param.ABS_MS );
			static_pr.para[ PR_CMPSPACING ]		= new Param(    6.0, Param.ABS_MS );	// 256 smp for Logic
			static_pr.para[ PR_CROSSFADE ]		= new Param(    0.0, Param.ABS_MS );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggOriginFile, ggCompactFile, ggOutputFile;
		PathField[]			ggInputs;
		ParamField			ggMinMatch, ggMinRecycle, ggPadding, ggCmpSpacing, ggCrossFade;
		JCheckBox			ggMarkers;
		ParamSpace[]		spcInterval;
		JButton				ggAnalyze;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ActionListener al = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_ORIGINFILE:
				case GG_COMPACTFILE:
					clearInput();
					break;

				case GG_ANALYZE:
					threadJustAnalyze = true;
					clearInput();
					start();
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOriginFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select original input file" );
		ggOriginFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Original Input", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOriginFile, GG_ORIGINFILE, null );

		ggCompactFile	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select compacted input file" );
		ggCompactFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Compacted Input", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggCompactFile, GG_COMPACTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggOriginFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Rcyc$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Recycled Output", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggAnalyze		= new JButton( "Analyze" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addButton( ggAnalyze, GG_ANALYZE, al );

		con.fill		= GridBagConstraints.HORIZONTAL;
		ggInfoField		= new JTextField();
		ggInfoField.setEditable( false );
		ggInfoField.setBackground( null );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addTextField( ggInfoField, GG_INFOFIELD, al );

	// -------- Settings-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Recycling Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcInterval		= new ParamSpace[ 2 ];
		spcInterval[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcInterval[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		ggMinMatch		= new ParamField( spcInterval );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Min. Matching", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMinMatch, GG_MINMATCH, null );

		ggMarkers		= new JCheckBox();
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Write Markers", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggMarkers, GG_MARKERS, null );

		ggCmpSpacing	= new ParamField( spcInterval );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Compact Spacing", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCmpSpacing, GG_CMPSPACING, null );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggMinRecycle	= new ParamField( spcInterval );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Min. Recycling", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggMinRecycle, GG_MINRECYCLE, null );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggPadding		= new ParamField( spcInterval );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Pre/Post Padding", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggPadding, GG_PADDING, null );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggCrossFade		= new ParamField( spcInterval );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Crossfade", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCrossFade, GG_CROSSFADE, null );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
		clearInput();
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
		int					i, j, k, chunkLength;
		int					off, len, ch;
		long				progOff, progLen;
		double				d1;
		
		// io
		AudioFile			origF			= null;
		AudioFile			cmpF			= null;
		AudioFile			outF			= null;
		AudioFileDescr		origStream, cmpStream, outStream;
		int					chanNum;
		float[][]			origBuf, cmpBuf, fadeBuf;
		float[]				convBuf1, convBuf2;

		int					origLength, cmpLength, outLength;
		int					origFramesRead, cmpFramesRead;
		int					minMatch, minRecycle, padding, cmpSpacing, matchBegin, matchEnd;
		int					origBufLen, origBufOff, cmpBufLen, cmpBufOff, origBufPhys;
		
		Region				currentRegion;
		java.util.List		markers;

		PathField			ggOutput;
		boolean				justAnalyse;

		long				dispDelta, dispTime;
		boolean				infoChange;
		int					fadeLength, fadeInLength, fadeOutLength;
		
topLevel: try {
			justAnalyse		= threadJustAnalyze;	// "Analyze"-JButton acts like a JCheckBox,
			threadJustAnalyze	= false;					// 	we have to "turn it off" ourself

		// ---- open input, output; init ----

			// input
			origF			= AudioFile.openAsRead( new File( pr.text[ PR_ORIGINFILE ]));
			origStream		= origF.getDescr();
			chanNum			= origStream.channels;
			origLength		= (int) origStream.length;
			// this helps to prevent errors from empty files!
			if( (origLength < 1) || (chanNum < 1) ) throw new EOFException( ERR_EMPTY );

			ggOutput		= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream		= new AudioFileDescr( origStream );
			ggOutput.fillStream( outStream );
			markers			= (java.util.List) outStream.getProperty( AudioFileDescr.KEY_MARKERS );
			if( markers == null && pr.bool[ PR_MARKERS ]) {
				markers		= new ArrayList();
			}

		// ---- open output ----
			if( !justAnalyse ) {
				if( !pr.bool[ PR_MARKERS ]) {
					outF		= AudioFile.openAsWrite( outStream );
				} else {
					IOUtil.createEmptyFile( new File( pr.text[ PR_OUTPUTFILE ]));
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
		// ---- misc inits ----
			d1				= Param.transform( pr.para[ PR_MINMATCH ], Param.ABS_MS, null, null ).val;
			minMatch		= Math.max( 1, (int) (AudioFileDescr.millisToSamples( origStream, d1 ) + 0.5) );
			d1				= Param.transform( pr.para[ PR_MINRECYCLE ], Param.ABS_MS, null, null ).val;
			minRecycle		= Math.max( 1, (int) (AudioFileDescr.millisToSamples( origStream, d1 ) + 0.5) );
			d1				= Param.transform( pr.para[ PR_PADDING ], Param.ABS_MS, null, null ).val;
			padding			= (int) (AudioFileDescr.millisToSamples( origStream, d1 ) + 0.5);
			d1				= Param.transform( pr.para[ PR_CMPSPACING ], Param.ABS_MS, null, null ).val;
			cmpSpacing		= (int) (AudioFileDescr.millisToSamples( origStream, d1 ) + 0.5);
			d1				= Param.transform( pr.para[ PR_CROSSFADE ], Param.ABS_MS, null, null ).val;
			fadeLength		= Math.max( 1, (int) (AudioFileDescr.millisToSamples( origStream, d1 ) + 0.5) );

			origBufLen		= minMatch << 1;
			origBuf			= new float[ chanNum ][ Math.max( 8192, origBufLen )];
			fadeBuf			= new float[ chanNum ][ fadeLength ];
			
		// ================ PASS 1: Region detection ================
		
			currentRegion	= null;
			progOff			= 0;
			
			if( !regionsKnown ) {

				cmpF			= AudioFile.openAsRead( new File( pr.text[ PR_COMPACTFILE ]));
				cmpStream		= cmpF.getDescr();
				cmpLength		= (int) cmpStream.length;
				if( chanNum != cmpStream.channels ) throw new IOException( ERR_CHANNUM );
				// this helps to prevent errors from empty files!
				if( cmpLength < 1 ) throw new EOFException( ERR_EMPTY );
			
				cmpBufLen		= minMatch;
				cmpBuf			= new float[ chanNum ][ cmpBufLen ];
				cmpBufOff		= 0;
				origBufOff		= 0;
				cmpFramesRead	= 0;
				origFramesRead	= 0;
				origBufPhys		= 0;	// physical offset
				matchEnd		= 0;
				regionList.clear();		// Elemente = de.sciss.fscape.io.Region, chronologically added

				progLen			= ((long) origLength + (long) cmpLength) << (justAnalyse ? 0 : 1);
				dispDelta		= 10000;	// first prelim. display after 10 sec.
				dispTime		= System.currentTimeMillis() + dispDelta;
				infoChange		= true;
			
passOne:		while( threadRunning ) {

			// ======== 1.) Find track ========

					// skip space between compacted chunks
					if( cmpSpacing > 0 ) {
						i = cmpBufOff - cmpSpacing;
						if( i > 0 ) {
//System.out.println( "skipping "+i+" buffer frames" );
							cmpBufOff = i;
							for( ch = 0; ch < chanNum; ch++ ) {
								System.arraycopy( cmpBuf[ ch ], cmpSpacing, cmpBuf[ ch ], 0, cmpBufOff );
							}
						} else {
//System.out.println( "skipping "+(-i)+" cmpF frames" );
							cmpBufOff = 0;
							cmpFramesRead -= i;
							cmpF.seekFrame( cmpFramesRead );
						}
					}

					// compact der laenge "minMatch" einlesen
					chunkLength			 = Math.min( cmpLength - cmpFramesRead, cmpBufLen - cmpBufOff );
					for( off = 0; off < chunkLength; ) {
						len				 = Math.min( 8192, chunkLength - off );
						cmpF.readFrames( cmpBuf, cmpBufOff, len );
						cmpFramesRead	+= len;
						cmpBufOff		+= len;
						off				+= len;
						progOff			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					}

					if( cmpBufOff < minMatch ) break passOne;	// EOF -> continue pass2

matchLp2:			while( true ) {
						chunkLength		 	 = Math.min( origLength - origFramesRead, origBufLen - origBufOff );
						for( off = 0; off < chunkLength; ) {
							len				 = Math.min( 8192, chunkLength - off );
							origF.readFrames( origBuf, origBufOff, len );
							origFramesRead	+= len;
							origBufOff		+= len;
							off				+= len;
							progOff			+= len;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						// .... check running ....
							if( !threadRunning ) break topLevel;
						}
						
						if( (chunkLength == 0) || (origBufOff < minMatch) ) break passOne;	// EOF -> continue pass2

					// ---- update display after increasing time deltas ----
						if( (System.currentTimeMillis() > dispTime) && infoChange ) {
							showRegions( origStream.rate, false );
							dispDelta	= dispDelta * 3 / 2;
							dispTime   += dispDelta;
							infoChange	= false;
						}
						
// System.out.println( "minMatch "+minMatch+"; cmpBufLen "+cmpBufLen+"; cmpBufOff "+cmpBufOff+"; origBufLen "+origBufLen+"; origBufOff "+origBufOff );

matchLp:				for( i = minMatch; i <= origBufOff; i++ ) {
							for( ch = 0; ch < chanNum; ch++ ) {
								convBuf1 = origBuf[ ch ];
								convBuf2 = cmpBuf[ ch ];
								for( j = minMatch, k = i; j > 0; ) {
									if( convBuf1[ --k ] != convBuf2[ --j ]) continue matchLp;
								}
							}
							// when we get here origBuf matches cmpBuf with offset k
							matchBegin	= origBufPhys + i - minMatch;
							origBufOff -= i;
							origBufPhys+= i;
							cmpBufOff   = 0;
							for( ch = 0; ch < chanNum; ch++ ) {	// shift buffer content to offset 0
								System.arraycopy( origBuf[ ch ], i, origBuf[ ch ], 0, origBufOff );
							}
							break matchLp2;
						}

						i			= origBufOff - minMatch;
						origBufOff -= i;
						origBufPhys+= i;
						for( ch = 0; ch < chanNum; ch++ ) {	// shift buffer content to offset 0
							System.arraycopy( origBuf[ ch ], i, origBuf[ ch ], 0, origBufOff );
						}

					} // while( true )
					
			// ======== 2.) Follow track ========

					// add new region up to current match
					if( (matchBegin - matchEnd) >= minRecycle ) {
						i = Math.max( 0, matchEnd - padding );
						j = Math.min( origLength, matchBegin + padding );
						if( (currentRegion != null) && (currentRegion.span.getStop() >= i) ) {	// fuse overlapping regions
							currentRegion	= new Region( new Span( Math.min( currentRegion.span.getStart(), i ),
															Math.max( currentRegion.span.getStop(),  j )),
														  currentRegion.name );
							regionList.set( regionList.size() - 1, currentRegion );
							infoChange			= true;
						} else {	// add new region to end of list
							currentRegion		= new Region( new Span( i, j ), null );
							regionList.add( currentRegion );
							infoChange			= true;
						}
					}

				// ---- update display after increasing time deltas ----
					if( (System.currentTimeMillis() > dispTime) && infoChange ) {
						showRegions( origStream.rate, false );
						dispDelta	= dispDelta * 3 / 2;
						dispTime   += dispDelta;
						infoChange	= false;
					}
						
					do {
						chunkLength	= Math.min( Math.min( cmpLength - cmpFramesRead, cmpBufLen - cmpBufOff ),
												Math.min( origLength - origFramesRead, origBufLen - origBufOff ));
											
						if( chunkLength == 0 ) {	// EOF -> finalize region and continue pass2
							matchEnd	 = origBufPhys;
							break passOne;
						}
						
						for( off = 0; off < chunkLength; ) {
							len				 = Math.min( 8192, chunkLength - off );
							cmpF.readFrames( cmpBuf, cmpBufOff, len );
							cmpFramesRead	+= len;
							cmpBufOff		+= len;
							progOff			+= len;
							origF.readFrames( origBuf, origBufOff, len );
							origFramesRead	+= len;
							origBufOff		+= len;
							off				+= len;
							progOff			+= len;
						// .... progress ....
							setProgression( (float) progOff / (float) progLen );
						// .... check running ....
							if( !threadRunning ) break topLevel;
						}

						j = chunkLength;
followLp:				for( ch = 0; ch < chanNum; ch++ ) {
							convBuf1	= origBuf[ ch ];
							convBuf2	= cmpBuf[ ch ];
							for( i = 0; i < chunkLength; i++ ) {
								if( convBuf1[ i ] != convBuf2[ i ]) {
									matchEnd	= origBufPhys + i;
//System.out.println( "loose "+convBuf1[ i ]+" / "+convBuf2[ i ]+" @"+matchEnd+" ; i = "+i );
									j			= i;
									break followLp;
								}
							}
						}
						origBufOff -= j;
						origBufPhys+= j;
						cmpBufOff  -= j;
						for( ch = 0; ch < chanNum; ch++ ) {	// shift buffer content to offset 0
							System.arraycopy( origBuf[ ch ], j, origBuf[ ch ], 0, origBufOff );
							System.arraycopy( cmpBuf[ ch ],  j, cmpBuf[ ch ],  0, cmpBufOff );
						}
					} while( j == chunkLength ); 	// j < chunkLength means we lost track
				} // passOne loop
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// add final region
				if( (origLength - matchEnd) >= minRecycle ) {
					i = Math.max( 0, matchEnd - padding );
					j = origLength;
					if( (currentRegion != null) && (currentRegion.span.getStop() >= i) ) {	// fuse overlapping regions
						currentRegion	= new Region( new Span( Math.min( currentRegion.span.getStart(), i ),
														Math.max( currentRegion.span.getStop(),  j )),
													  currentRegion.name );
						regionList.set( regionList.size() - 1, currentRegion );
					} else {	// add new region to end of list
						currentRegion		= new Region( new Span( i, j ), null );
						regionList.add( currentRegion );
					}
				}

				cmpF.close();
				cmpF = null;
				regionsKnown = true;
				
				if( !justAnalyse ) indicateOutputWrite();
			} // if( !regionsKnown )

		// ================ PASS 2: Region copying ================

			showRegions( origStream.rate, true );
		
			// calc output length, ggf. marker erzeugen
			// XXX eigentlich muessten die originalen marker noch entsprechend verschoben werden
			for( i = 0, outLength = 0; i < regionList.size(); i++ ) {
				currentRegion	= (Region) regionList.get( i );
				j				= (int) currentRegion.span.getLength();
				if( pr.bool[ PR_MARKERS ]) {
					markers.add( new Marker( outLength, MARK_CUT ));
				}
				outLength += j;
			}

			if( !justAnalyse ) {
				if( pr.bool[ PR_MARKERS ]) {
					outStream.setProperty( AudioFileDescr.KEY_MARKERS, markers );
					outF		= AudioFile.openAsWrite( outStream );
				}

				progOff			= progOff == 0 ? 0 : (long) outLength;
				progLen			= progOff + outLength;
//				framesWritten	= 0;
				fadeOutLength	= 0;
				
				for( i = 0; i < regionList.size(); i++ ) {
					currentRegion	= (Region) regionList.get( i );
					chunkLength		= (int) currentRegion.span.getLength();
					
					fadeInLength	= fadeOutLength;

					origF.seekFrame( currentRegion.span.getStart() - fadeInLength );

					for( off = 0; off < fadeInLength; ) {
						len = Math.min( 8192, fadeInLength - off );
						origF.readFrames( origBuf, 0, len );
						for( ch = 0; ch < chanNum; ch++ ) {
							convBuf1 = fadeBuf[ ch ];
							convBuf2 = origBuf[ ch ];
							for( j = 0, k = off; j < len; j++, k++ ) {
								convBuf1[ k ] += convBuf2[ j ] * (float) Math.sqrt( (double) k / (double) fadeInLength );
							}
						}

						outF.writeFrames( fadeBuf, off, len );
						off				+= len;
						progOff			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					}

					if( i + 1 < regionList.size() ) {
						fadeOutLength	= (int) Math.min( fadeLength, Math.min( origLength - currentRegion.span.getStop(),
														  ((Region) regionList.get( i + 1 )).span.getStart() ));
						chunkLength	   -= fadeOutLength;
					} else {
						fadeOutLength	= 0;
					}

					for( off = 0; off < chunkLength; ) {
						len = Math.min( 8192, chunkLength - off );
						origF.readFrames( origBuf, 0, len );
						outF.writeFrames( origBuf, 0, len );
						off				+= len;
						progOff			+= len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					}
					
					if( fadeOutLength > 0 ) {
						origF.readFrames( fadeBuf, 0, fadeOutLength );
						for( ch = 0; ch < chanNum; ch++ ) {
							convBuf1 = fadeBuf[ ch ];
							for( j = 0; j < fadeOutLength; j++ ) {
								convBuf1[ j ] *= (float) Math.sqrt( 1.0 - ((double) j / (double) fadeOutLength) );
							}
						}
					}
				} // passTwo loop
			// .... check running ....
				if( !threadRunning ) break topLevel;

				outF.close();
				outF = null;
			}
			origF.close();
			origF = null;

			setProgression( 1.0f );		// in case regionList.size() == 0
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			origStream	= null;
			cmpStream	= null;
			outStream	= null;
			origBuf		= null;
			cmpBuf		= null;
			fadeBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( origF != null ) {
			origF.cleanUp();
		}
		if( cmpF != null ) {
			cmpF.cleanUp();
		}
	} // process()

// -------- private Methoden --------

	/*
	 *	Zahl und Laenge der Recycle-Regionen anzeigen
	 *
	 *	@param	complete	false wenn noch nicht zu Ende berechnet
	 *
	 *	@return	total length of all regions in frames
	 */
	private void showRegions( double sampleRate, boolean complete )
	{
		Integer		cmpChoice;
		Object[]	msgArgs;
		int			i, numRegions;
		long		duration;
		String		strTime;
		
		numRegions = regionList.size();
		
		if( numRegions > 0 ) {
			for( i = 0, duration = 0; i < regionList.size(); i++ ) {
				duration += ((Region) regionList.get( i )).span.getLength();
			}
			strTime	= new TimeFormat( 0, null, null, 3, Locale.US ).formatTime( new Double( duration / sampleRate ));
		} else {
			strTime	= null;
		}
		
		cmpChoice	= new Integer( complete ? 1 : -1 );
		msgArgs		= new Object[] { cmpChoice, new Integer( regionList.size() ), strTime };
		
		gui.stringToJTextField( msgInfoField.format( msgArgs ), GG_INFOFIELD );
	}

	/*
	 *	Neues Inputfile setzen
	 */
	protected void clearInput()
	{
		regionsKnown	= false;
		regionList.clear();
		
		gui.stringToJTextField( "", GG_INFOFIELD );
	}
}
// class RecycleDlg
