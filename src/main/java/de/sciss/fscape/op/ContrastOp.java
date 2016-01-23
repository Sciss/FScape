/*
 *  ContrastOp.java
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

package de.sciss.fscape.op;

import java.io.*;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

public class ContrastOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Contrast";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_CONTRAST		= 0;		// pr.para
	private static final int PR_MAXBOOST		= 1;

	private static final String PRN_CONTRAST		= "Contrast";
	private static final String PRN_MAXBOOST		= "MaxBoost";

	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_CONTRAST, PRN_MAXBOOST };

// -------- public Methoden --------
	// public Container createGUI( int type );

	public ContrastOp()
	{
		super();
		
		// initialize only in the first instance
		// preferences laden
		if( static_prefs == null ) {
			static_prefs = new OpPrefs( getClass(), getDefaultPrefs() );
		}
		// propertyarray defaults
		if( static_pr == null ) {
			static_pr = new PropertyArray();

//			static_pr.intg		= prIntg;
//			static_pr.intgName	= prIntgName;
			static_pr.para		= prPara;
			static_pr.para[ PR_CONTRAST ]		= new Param(  24.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_MAXBOOST ]		= new Param(  36.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder uebertragen
		opName		= "ContrastOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon						// XXX
		icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
	}

// -------- Runnable Methoden --------

	public void run()
	{
		runInit();		// superclass

		// Haupt-Variablen fuer den Prozess
		int				ch, i;
		float			f1, f2, maxGain;
		double			exp;

		Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		SpectStreamSlot	runInSlot;
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream	= null;

		SpectFrame		runInFr			= null;
		SpectFrame		runOutFr		= null;
	
		// Ziel-Frame Berechnung
		int				srcBands, fftSize, fullFFTsize, winSize, winHalf;
		float[]			fftBuf, convBuf1, convBuf2, win;

topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			runInSlot = (SpectStreamSlot) slots.elementAt( SLOT_INPUT );
			if( runInSlot.getLinked() == null ) {
				runStop();	// threadDead = true -> folgendes for() wird uebersprungen
			}
			// diese while Schleife ist noetig, da beim initReader ein Pause eingelegt werden kann
			// und die InterruptException ausgeloest wird; danach versuchen wir es erneut
			for( boolean initDone = false; !initDone && !threadDead; ) {
				try {
					runInStream	= runInSlot.getDescr();	// throws InterruptedException
					initDone = true;
				}
				catch( InterruptedException e ) {}
				runCheckPause();
			}
			if( threadDead ) break topLevel;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot					= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream				= new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------

			srcBands	= runInStream.bands;
			winSize		= srcBands - 1;
			winHalf		= winSize >> 1;
			win			= Filter.createFullWindow( winSize, Filter.WIN_BLACKMAN ); // pr.intg[ PR_WINDOW ]);
			fftSize		= srcBands - 1;
			fullFFTsize	= fftSize << 1;
			fftBuf		= new float[ fullFFTsize + 2 ];
			exp			= (Param.transform( pr.para[ PR_CONTRAST ], Param.ABS_AMP, ampRef, null )).val - 1.0;
			maxGain		= (float) (Param.transform( pr.para[ PR_MAXBOOST ], Param.ABS_AMP, ampRef, null )).val;

// System.out.println( "srcBands "+srcBands+"; fftSize "+fftSize+"; exp "+exp+"; maxGain "+maxGain );

			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
			// ---------- Frame einlesen ----------
	 			for( boolean readDone = false; (readDone == false) && !threadDead; ) {
					try {
						runInFr		= runInSlot.readFrame();	// throws InterruptedException
						readDone	= true;
						runOutFr	= runOutStream.allocFrame();
					}
					catch( InterruptedException e ) {}
					catch( EOFException e ) {
						break mainLoop;
					}
					runCheckPause();
				}
				if( threadDead ) break mainLoop;

			// ---------- Process: Ziel-Frame berechnen ----------
			
				for( ch = 0; ch < runOutStream.chanNum; ch++ ) {
					convBuf1	= runInFr.data[ ch ];
					convBuf2	= runOutFr.data[ ch ];
					fftBuf[ 0 ]	= 1.0f;
					fftBuf[ 1 ]	= 0.0f;
					
					for( i = 2; i < fullFFTsize; ) {
						f2 = (convBuf1[ i - 2 ] + convBuf1[ i + 2 ]);
						if( f2 > 0.0f ) {
							f1 = (float) Math.min( maxGain, Math.pow( 2.0f * convBuf1[ i ] / f2, exp ));
						} else {
							if( convBuf1[ i ] == 0.0f ) {
								f1 = 1.0f;
							} else {
								f1 = maxGain;
							}
						}
// System.out.println( f1 );
						fftBuf[ i++ ]	= f1;
						fftBuf[ i++ ]	= 0.0f;
					}
					fftBuf[ i++ ]	= 1.0f;
					fftBuf[ i++ ]	= 0.0f;

					Fourier.realTransform( fftBuf, fullFFTsize, Fourier.INVERSE );
					Util.mult( win, winHalf, fftBuf, 0, winHalf );
					for( i = winHalf; i < fullFFTsize - winHalf; ) {
						fftBuf[ i++ ]	= 0.0f;
					}
					Util.mult( win, 0, fftBuf, i, winHalf );
// if( (runOutStream.framesWritten < 2) && (ch == 0) ) Debug.view( fftBuf, "time" );
					Fourier.realTransform( fftBuf, fullFFTsize, Fourier.FORWARD );
// if( (runOutStream.framesWritten < 2) && (ch == 0) ) Debug.view( fftBuf, "freq" );

					for( i = 0; i <= fullFFTsize; ) {
						convBuf2[ i ] = convBuf1[ i ] * fftBuf[ i ];
						i++;
						convBuf2[ i ] = convBuf1[ i ];
						i++;
					}
				}
				// Berechnung fertich

// if( (runOutStream.framesWritten < 2) ) { 
// Debug.view( fftBuf, "flt "+runOutStream.framesWritten );
// Debug.view( runInFr.data[0], "in "+runOutStream.framesWritten );
// Debug.view( runOutFr.data[0], "out "+runOutStream.framesWritten );
// }
				runInSlot.freeFrame( runInFr );

				for( boolean writeDone = false; (writeDone == false) && !threadDead; ) {
					try {	// Unterbrechung
						runOutSlot.writeFrame( runOutFr );	// throws InterruptedException
						writeDone = true;
						runFrameDone( runOutSlot, runOutFr  );
						runOutStream.freeFrame( runOutFr );
					}
					catch( InterruptedException e ) {}	// mainLoop wird eh gleich verlassen
					runCheckPause();
				}
			} // Ende Hauptschleife

			runInStream.closeReader();
			runOutStream.closeWriter();

		} // break topLevel

		catch( IOException e ) {
			runQuit( e );
			return;
		}
		catch( SlotAlreadyConnectedException e ) {
			runQuit( e );
			return;
		}
//		catch( OutOfMemoryError e ) {
//			abort( e );
//			return;
//		}

		runQuit( null );
	}

// -------- GUI Methoden --------

	public PropertyGUI createGUI( int type )
	{
		PropertyGUI		gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbContrast;pf"+Constants.decibelAmpSpace+",pr"+PRN_CONTRAST+"\n"+
			"lbMax.boost;pf"+Constants.decibelAmpSpace+",pr"+PRN_MAXBOOST+"\n" );

		return gui;
	}
}
// class ContrastOp
