/*
 *  ChangeGainDlg.java
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
 *		07-Jan-05	uses indicateOutputWrite()
 *		07-Jun-05	printMarkers feature
 *		25-Aug-08	64bit frames savvy
 */

package de.sciss.fscape.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Util;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.Marker;
import de.sciss.io.Region;

/**
 *  Processing module for querying or changing a sound files volume.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 25-Aug-08
 */
public class ChangeGainDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_GAIN			= 0;		// pr.para

	private static final String PRN_INPUTFILE	= "InputFile";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null };
	private static final String	prParaName[]	= { PRN_GAIN };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_INPUTFORMAT		= GG_OFF_OTHER		+ 0;
	private static final int GG_FINDPEAK		= GG_OFF_OTHER		+ 1;
	
	private boolean peakKnown		= false;	// true nach "FindPeak"; false nach neuer Input-Wahl
	private Param peakGain;					// (abs amp)

	public boolean	threadJustFind	= false;		// true = find peak; false = change gain
	private boolean	printMarkers	= false;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public ChangeGainDlg()
	{
		super( "Change Gain" );
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
			static_pr.para[ PR_GAIN ]		= new Param( 0.0, Param.DECIBEL_AMP );
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

		PathField			ggInputFile, ggOutputFile;
		JTextField			ggInputFormat;
		JButton				ggFindPeak;
		Component[]			ggGain;
		PathField[]			ggInputs;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );
		
		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE:
					clearInput();
					break;
				}
			}
		};

		ActionListener al = new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_FINDPEAK:
					threadJustFind	= true;
					printMarkers	= (e.getModifiers() & ActionEvent.META_MASK) != 0;
					clearInput();
					start();
				}
			}
		};
		
	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;

	gui.addLabel( new GroupLabel( "Waveform Input", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filename", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		con.fill		= GridBagConstraints.HORIZONTAL;
		ggFindPeak		= new JButton( "Info" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addButton( ggFindPeak, GG_FINDPEAK, al );

		ggInputFormat	= new JTextField();
		ggInputFormat.setEditable( false );
		ggInputFormat.setBackground( null );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addTextField( ggInputFormat, GG_INPUTFORMAT, al );

	// -------- Output-Gadgets --------
	gui.addLabel( new GroupLabel( "Output", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Gain$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filename", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

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

	/**
	 *	Neues Inputfile setzen
	 */
	public void clearInput()
	{
		peakKnown	= false;
		gui.stringToJTextField( "", GG_INPUTFORMAT );
	}

// -------- Processor Interface --------
		
	protected void process()
	{
		int					len;
		long				progOff, progLen;
		float				f1;
		boolean				b1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		int					chanNum;
		float[][]			inBuf			= null;
		float[]				convBuf1;

		// Synthesize
		float				gain;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		long				inLength;
		long				framesRead;

		float				maxAmp;
		double				energy, power;
		int					maxChan;
		long				maxFrame;

		PathField			ggOutput;
		boolean				threadJustFind, printMarkers;
		boolean				needsRead, needsWrite;
		long				dispDelta, dispTime;

topLevel: try {
			threadJustFind		= this.threadJustFind;	// "Find Peak"-JButton acts like a JCheckBox,
			this.threadJustFind	= false;				// 	we have to "turn it off" ourself
			printMarkers		= this.printMarkers;
			this.printMarkers	= false;

		// ---- open input ----
			inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream	= inF.getDescr();
			
			if( printMarkers ) printMarkers( inStream );
			
			chanNum		= inStream.channels;
			inLength	= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( chanNum * inLength < 1 ) throw new EOFException( ERR_EMPTY );

			inBuf		= new float[ chanNum ][ 8192 ];

			needsRead	= threadJustFind || (!peakKnown && (pr.intg[ PR_GAINTYPE ] == GAIN_UNITY));
			needsWrite	= !threadJustFind;
			
			progLen		= (needsRead ? (long) inLength : 0) + (needsWrite ? (long) inLength*2 : 0);
			progOff		= 0L;
			dispDelta	= 10000;	// first prelim. display after 10 sec.
//dispDelta	= 1000;	// first prelim. display after 10 sec.
			dispTime	= System.currentTimeMillis() + dispDelta;

		// ---- open output ----
			if( needsWrite ) {
				ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
				if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
				outStream	= new AudioFileDescr( inStream );
				ggOutput.fillStream( outStream );
				outF		= AudioFile.openAsWrite( outStream );
			// .... check running ....
				if( !threadRunning ) break topLevel;
			}

		// ---- first pass: get input gain peak ----

			maxAmp		= -1f;
			maxFrame	= -1;
			maxChan		= -1;
			energy		= 0.0;

			if( needsRead ) {

				for( framesRead = 0; (framesRead < inLength) && threadRunning; ) {
					len		= (int) Math.min( 8192, inLength - framesRead );
					inF.readFrames( inBuf, 0, len );

					for( int ch = 0; ch < chanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( int i = 0; i < len; i++ ) {
							f1		= convBuf1[ i ];
							f1	   *= f1;
							energy  += f1;
							if( f1 >= maxAmp ) {
								final long frame	= framesRead + i + 1;
								final int ch2		= ch + 1;
								if( f1 > maxAmp ) {
									maxAmp	= f1;
									maxFrame= frame;
									maxChan	= ch2;
								} else {
									if( maxFrame != frame ) {
										maxFrame = -Math.abs( maxFrame );
									}
									if( maxChan != ch2 ) {
										maxChan	= -Math.abs( maxChan );
									}
								}
							}
						}
					}
					framesRead	+= len;
					progOff		+= len;
			// .... progress ....
					setProgression( (float) progOff / (float) progLen );
	
				// ---- update display after increasing time deltas and after last chunk ----
					b1 = framesRead == inLength;
					if( (System.currentTimeMillis() > dispTime) || b1 ) {
					
//						inDur		= AudioFileDescr.samplesToMillis( inStream, framesRead ) / 1000;	// * chanNum
						power	    = energy / (framesRead * chanNum);	// / inDur
						showPeak( inStream, Math.sqrt( maxAmp ), maxFrame, maxChan, power, b1 );
						
						dispDelta <<= 1;
						dispTime   += dispDelta;
					}
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
				peakGain	= new Param( Math.sqrt( maxAmp ), Param.ABS_AMP );
				peakKnown	= true;

				if( needsWrite ) indicateOutputWrite();
			}
			
		// ---- second pass: adjust gain ----

//long t1, t2;

			if( needsWrite ) {
	
				inF.seekFrame( 0 );
	
				if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
					gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				} else {
					gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
				}
				if( peakKnown ) {
					maxAmp = (float) peakGain.val * (float) peakGain.val;
				}

			// ---- process loop ----
//t1 = System.currentTimeMillis();	
				for( framesRead = 0; (framesRead < inLength) && threadRunning; ) {
					len			= (int) Math.min( 8192, inLength - framesRead );
					inF.readFrames( inBuf, 0, len );

					if( !peakKnown ) {
						for( int ch = 0; ch < chanNum; ch++ ) {
							convBuf1 = inBuf[ ch ];
							for( int i = 0; i < len; i++ ) {
								f1		= convBuf1[ i ];
								f1	   *= f1;
								energy  += f1;
								if( f1 >= maxAmp ) {
									final long	frame	= framesRead + i + 1;
									final int	ch2		= ch + 1;
									if( f1 > maxAmp ) {
										maxAmp	= f1;
										maxFrame= frame;
										maxChan	= ch2;
									} else {
										if( maxFrame != frame ) {
											maxFrame = -Math.abs( maxFrame );
										}
										if( maxChan != ch2 ) {
											maxChan	= -Math.abs( maxChan );
										}
									}
								}
							}
						}
					// ---- update display after increasing time deltas and after last chunk ----
						b1 = framesRead == inLength;
						if( (System.currentTimeMillis() > dispTime) || b1 ) {
						
//							inDur		= AudioFileDescr.samplesToMillis( inStream, framesRead ) / 1000;	// * chanNum
							power	    = energy / (framesRead * chanNum);	// / inDur
							showPeak( inStream, Math.sqrt( maxAmp ), maxFrame, maxChan, power, b1 );
							
							dispDelta <<= 1;
							dispTime   += dispDelta;
						}
					} // if !peakKnown

					framesRead += len;
					progOff	   += len;
					
				// ---- adjust gain + write ----
					for( int ch = 0; ch < chanNum; ch++ ) {
						Util.mult( inBuf[ ch ], 0, len, gain );
					}
					outF.writeFrames( inBuf, 0, len );
//					framesWritten += len;
					progOff		  += len;
			// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
//t2 = System.currentTimeMillis();
//System.err.println( "took "+(t2-t1)+" millis." );
				if( !peakKnown ) {
					peakGain	= new Param( Math.sqrt( maxAmp ), Param.ABS_AMP );
					peakKnown	= true;			
				}
				maxAmp *= gain * gain;		// now maxInputAmp = maxOutputAmp

				outF.close();
				outF = null;
			}

		// ---- Finish ----
			inF.close();
			inF			= null;
			inStream	= null;

			// inform about clipping
			if( !threadJustFind && (maxAmp > 1.0f) && (pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE) ) {
				handleClipping( (float) Math.sqrt( maxAmp ));
			}
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			inBuf		= null;
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

	private static void printMarkers( AudioFileDescr afd )
	{
		List			markers	= (List) afd.getProperty( AudioFileDescr.KEY_MARKERS );
		List			regions	= (List) afd.getProperty( AudioFileDescr.KEY_REGIONS );
		final List		empty	= new ArrayList();
		Marker			mark;
		Region			region;
		
		if( markers == null ) markers = empty;
		if( regions == null ) regions = empty;
	
		System.out.println( "File: " + afd.file.getName() + "\n" +
							"   # of Markers : " + markers.size()+ "; # of Regions : " + regions.size() );
							
		System.out.println( "\nMarker names: " );
		for( int i = 0; i < markers.size(); i++ ) {
			mark = (Marker) markers.get( i );
			System.out.println( mark.name );
		}

		System.out.println( "\nMarker times in sample frames: " );
		for( int i = 0; i < markers.size(); i++ ) {
			mark = (Marker) markers.get( i );
			System.out.println( mark.pos );
		}

		System.out.println( "\nMarker times in seconds: " );
		for( int i = 0; i < markers.size(); i++ ) {
			mark = (Marker) markers.get( i );
			System.out.println( AudioFileDescr.samplesToMillis( afd, mark.pos ) / 1000 );
		}

		System.out.println( "\nRegion names: " );
		for( int i = 0; i < regions.size(); i++ ) {
			region = (Region) regions.get( i );
			System.out.println( region.name );
		}

//		System.out.println( "\nRegion times in sample frames: " );
//		for( int i = 0; i < markers.size(); i++ ) {
//			mark = (Marker) markers.get( i );
//			System.out.println( mark.pos );
//		}
//
//		System.out.println( "\nRegion times in seconds: " );
//		for( int i = 0; i < markers.size(); i++ ) {
//			mark = (Marker) markers.get( i );
//			System.out.println( AudioFileDescr.sampleToMillis( afd, mark.pos ) / 1000 );
//		}
	}

	/*
	 *	Maximalen Ausschlag im InputFormat-Gadget anzeigen
	 *
	 *	@param	inStream	re Sound
	 *	@param	maxAmp		der Peak
	 *	@param	maxFrame	Zeitpunkt des Peaks (+1)
	 *	@param	maxChan		Kanal des Peaks (+1)
	 *	@param	power		gemessene Durchschnitts-Leistung
	 *	@param	complete	false wenn noch nicht zu Ende berechnet
	 */
	private void showPeak( AudioFileDescr inStream, double maxAmp, long maxFrame, int maxChan,
						   double power, boolean complete )
	{
		String	chanTxt;
		double	absTime;
		int		min;
		boolean	b;

		absTime		= (AudioFileDescr.samplesToMillis( inStream, Math.abs( maxFrame ) - 1 ) + 0.5) / 1000;
		min			= (int) (absTime / 60);
		absTime	   %= 60;
		b			= (maxChan < 0) || (inStream.channels == 1);
		chanTxt		= b ? "" : (inStream.channels == 2 ? (maxChan == 1 ? "[L]" : "[R]") : ("[" + maxChan + "]"));

// System.out.println( "maxAmp "+maxAmp+"; maxFrame "+maxFrame+"; maxChan "+maxChan+"; power "+power+"; complete "+complete );
		
//		if( maxAmp > 0.0 ) {
//			power				   *= 1000.0 / (maxAmp * maxAmp);	// "normalized"
//		}
		Double			ampDB		= new Double( 20 * Math.log( maxAmp ) / Constants.ln10 );
		Double			rmsDB		= new Double( 10 * Math.log( power ) / Constants.ln10 );
		Integer			cmpJComboBox	= new Integer( complete ? 1 : -1 );
		Object[]		msgArgs		= { ampDB, rmsDB, cmpJComboBox,
										new Integer( min ), new Double( absTime ), new Long( maxFrame ) };
		String			msgPtrn		= new String( "{2,choice,-1#[É|0#}" +
												  "Max amp {0,number,#,##0.0} dBFS @" +
												  "{3,number,##0}:{4,number,00.000}" +
												  "{5,choice,-1#+|0#}" + chanTxt + 
												  "; RMS {1,number,##,##0.0} dB" +
												  "{2,choice,-1#É]|0#}" );
		MessageFormat	msgForm		= new MessageFormat( msgPtrn );
														 
		msgForm.setLocale( Locale.US );
		msgForm.applyPattern( msgPtrn );

		gui.stringToJTextField( msgForm.format( msgArgs ), GG_INPUTFORMAT );
	}
}
// class ChangeGainDlg
