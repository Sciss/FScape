/*
 *  ConvOp.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.fscape.op;

import java.io.*;
import java.rmi.AlreadyBoundException;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class ConvOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Convolve";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT1		= 0;
	protected static final int SLOT_INPUT2		= 1;
	protected static final int SLOT_OUTPUT		= 2;

	// Properties (defaults)
	private static final int PR_MODE			= 0;		// pr.intg

//	private static final String PRN_FACTOR		= "Factor";
	private static final String PRN_MODE			= "Mode";

	protected static final int MODE_CIRCLE			= 0;
	protected static final int MODE_EXPAND			= 1;
	protected static final int MODE_MULT			= 2;

	private static final int		prIntg[]		= { MODE_CIRCLE };
	private static final String	prIntgName[]	= { PRN_MODE };

// -------- public Methoden --------
	// public Container createGUI( int type );

	public ConvOp()
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

			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;

			static_pr.superPr	= Operator.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder Ÿbertragen		
		opName		= "ConvOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER, "in1" ));		// SLOT_INPUT1
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER, "in2" ));		// SLOT_INPUT2
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon						// XXX
		icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
	}

// -------- Runnable Methoden --------

	public void run()
	{
		runInit();		// superclass

		// Haupt-Variablen fuer den Prozess
		int				ch, i, j, k;

		SpectStreamSlot[]	runInSlot	= new SpectStreamSlot[ 2 ];
		SpectStreamSlot	runOutSlot;
		SpectStream[]	runInStream		= new SpectStream[ 2 ];
		SpectStream		runOutStream	= null;

		SpectFrame[]	runInFr			= new SpectFrame[ 2 ];
		SpectFrame		runOutFr		= null;
	
		// Ziel-Frame Berechnung
		int[]			srcBands	= new int[ 2 ];
		int				destBands, fftSize, fullFFTsize;
		float[][]		fftBuf		= new float[ 2 ][];
		float[]			convBuf1, convBuf2;
		int				readDone, oldReadDone;
		
topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			for( i = 0; i < 2; i++ ) {
				runInSlot[i] = (SpectStreamSlot) slots.elementAt( SLOT_INPUT1 + i );
				if( runInSlot[i].getLinked() == null ) {
					runStop();	// threadDead = true -> folgendes for() wird Ÿbersprungen
				}
				// diese while Schleife ist nštig, da beim initReader ein Pause eingelegt werden kann
				// und die InterruptException ausgelšst wird; danach versuchen wir es erneut
				for( boolean initDone = false; !initDone && !threadDead; ) {
					try {
						runInStream[i]	= runInSlot[i].getDescr();	// throws InterruptedException
						initDone		= true;
						srcBands[i]		= runInStream[i].bands;
					}
					catch( InterruptedException e ) {}
					runCheckPause();
				}
			}
			if( threadDead ) break topLevel;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot					= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream				= new SpectStream( runInStream[0] );
			if( pr.intg[ PR_MODE ] == MODE_EXPAND ) {
				i			= srcBands[0] + srcBands[1] - 2;
				for( destBands = 2; destBands < i; destBands <<= 1 ) ;
				destBands  += 1;
				runOutStream.smpPerFrame   *= (float) (destBands-1) / (float) (srcBands[0]-1);
			} else {
				destBands	= srcBands[0];
			}			
			runOutStream.bands			= destBands;
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------

			fftSize				= destBands - 1;
			fullFFTsize			= fftSize << 1;
			if( pr.intg[ PR_MODE ] != MODE_MULT ) {
				fftBuf[0]			= new float[ fullFFTsize ];
				fftBuf[1]			= new float[ fullFFTsize ];
			}
			
			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
			// ---------- Frame einlesen ----------
				for( readDone = 0; (readDone < 2) && !threadDead; ) {
					oldReadDone = readDone;
					for( i = 0; i < 2; i++ ) {
						try {
							if( runInStream[ i ].framesReadable() > 0 ) {
								runInFr[ i ] = runInSlot[ i ].readFrame();
								readDone++;
							}
						}
						catch( InterruptedException e ) {}
						catch( EOFException e ) {
							break mainLoop;
						}
						runCheckPause();
					}

					if( oldReadDone == readDone ) {		// konnte nix gelesen werden
						try {
							 Thread.sleep( 500 );	// ...deshalb kurze Pause
						}
						catch( InterruptedException e ) {}	// mainLoop wird gleich automatisch verlassen
						runCheckPause();
					}
				}
				if( threadDead ) break mainLoop;

				runOutFr	= runOutStream.allocFrame();

			// ---------- Process: Ziel-Frame berechnen ----------
			
				if( pr.intg[ PR_MODE ] == MODE_MULT ) {
					for( ch = 0; ch < runOutStream.chanNum; ch++ ) {
						convBuf1	= runInFr[0].data[ ch % runInStream[0].chanNum ];
						convBuf2	= runOutFr.data[ ch ];
						System.arraycopy( convBuf1, 0, convBuf2, 0, srcBands[0] << 1 );
						j = Math.min( fullFFTsize + 2, srcBands[1] << 1 );
						convBuf1	= runInFr[1].data[ ch % runInStream[1].chanNum ];
						for( i = 0; i < j; ) {
							convBuf2[ i ] *= convBuf1[ i ];
							i++;
							convBuf2[ i ] += convBuf1[ i ];
							i++;
						}
					}
				} else {
					for( ch = 0; ch < runOutStream.chanNum; ch++ ) {
						for( i = 0; i < 2; i++ ) {
							j = Math.min( fullFFTsize, (srcBands[i] - 1) << 1 );
							Fourier.polar2Rect( runInFr[i].data[ ch % runInStream[i].chanNum ], 0, fftBuf[i], 0, j );
							for( k = j; k < fullFFTsize; k++ ) {
								fftBuf[i][k] = 0.0f;
							}
							Fourier.complexTransform( fftBuf[i], fftSize, Fourier.FORWARD );
						}
						Fourier.complexMult( fftBuf[0], 0, fftBuf[1], 0, fftBuf[1], 0, fullFFTsize );
						Fourier.complexTransform( fftBuf[1], fftSize, Fourier.INVERSE );
						Fourier.rect2Polar( fftBuf[1], 0, runOutFr.data[ ch ], 0, fullFFTsize );
						// XXX just zero highest band XXX
						runOutFr.data[ ch ][ fullFFTsize ]		= 0.0f;
						runOutFr.data[ ch ][ fullFFTsize+1 ]	= 0.0f;
					}
				}
				// Berechnung fertich

				runInSlot[0].freeFrame( runInFr[0] );
				runInSlot[1].freeFrame( runInFr[1] );

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

			runInStream[0].closeReader();
			runInStream[1].closeReader();
			runOutStream.closeWriter();

		} // break topLevel

		catch( IOException e ) {
			runQuit( e );
			return;
		}
		catch( AlreadyBoundException e ) {
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
		PropertyGUI gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbMode;ch,pr"+PRN_MODE+",itCircular conv,itExpanding conv,itMultiplication" );

		return gui;
	}
}
// class ZoomOp
