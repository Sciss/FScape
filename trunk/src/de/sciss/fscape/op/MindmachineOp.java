/*
 *  MindmachineOp.java
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
public class MindmachineOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Mindmachine";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Properties (defaults)
	private static final int PR_ANGLE			= 0;		// pr.para

	private static final String PRN_ANGLE		= "Angle";

	// Slots
	protected static final int SLOT_INPUT1		= 0;
	protected static final int SLOT_INPUT2		= 1;
	protected static final int SLOT_OUTPUT		= 2;

	// Properties (defaults)

	private static final Param	prPara[]		= { null };
	private static final String	prParaName[]	= { PRN_ANGLE };

// -------- public Methoden --------
	// public Container createGUI( int type );

	public MindmachineOp()
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
			static_pr.para[ PR_ANGLE ]		= new Param(  25.0, Param.NONE );
			static_pr.paraName	= prParaName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder Ÿbertragen
		opName		= "MindmachineOp";
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
		int				ch, i, j, k, m;
		double			d1, d2;

		SpectStreamSlot[]	runInSlot	= new SpectStreamSlot[ 2 ];
		SpectStreamSlot	runOutSlot;
		SpectStream[]	runInStream		= new SpectStream[ 2 ];
		SpectStream		runOutStream	= null;

		SpectFrame[]	runInFr			= new SpectFrame[ 2 ];
		SpectFrame		runOutFr		= null;
	
		// Ziel-Frame Berechnung
		int[]			srcBands	= new int[ 2 ];
		int				fftSize, fullFFTsize, complexFFTsize;
		float[]			convBuf1, convBuf2;
		float[][]		fftBuf;
		float[]			win;
		int				readDone, oldReadDone;
		int[]			phase			= new int[2];
		
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
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------

			fftSize		= srcBands[0] - 1;
			fullFFTsize	= fftSize << 1;
			complexFFTsize = fullFFTsize << 1;
			fftBuf		= new float[2][ complexFFTsize ];
			win			= new float[ fullFFTsize ];
			
			d1 = 1.0 / (double) fullFFTsize * Math.PI;
			for( i = 0; i < fullFFTsize; i++ ) {
				d2		 = Math.cos( i * d1 );
				win[ i ] = (float) (d2 * d2);
			}

			phase[0] = (int) (pr.para[ PR_ANGLE ].val / 100.0 * fullFFTsize + 0.5) % fullFFTsize;
			phase[1] = (phase[0] + fftSize) % fullFFTsize;

//convBuf2 = fftBuf[0];
//Util.clear( convBuf2 );
//for( k = 0; k < 2; k++ ) {
//	m = k * fftSize;
//	for( i = 0; m < fullFFTsize; ) {
//		convBuf2[ i++ ] += win[ m ];
//		convBuf2[ i++ ] += win[ m++ ];
//	}
//	for( m = 0; i < complexFFTsize; ) {
//		convBuf2[ i++ ] += win[ m ];
//		convBuf2[ i++ ] += win[ m++ ];
//	}
//}
//Debug.view( convBuf2, "win overlap" );

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

				for( ch = 0; ch < runOutStream.chanNum; ch++ ) {
					for( k = 0; k < 2; k++ ) {
						convBuf1	= runInFr[k].data[ ch ];
						convBuf2	= fftBuf[k];

						// calculate complex log spectrum :
						// Re( target ) = Log( Mag( source ))
						// Im( target ) = Phase( source )
						// convBuf1 is already in polar style
						// -> fftBuf will be in rect style
						
						for( i = 0; i <= fullFFTsize; ) {
							convBuf2[ i ] = (float) Math.log( Math.max( 1.0e-24, convBuf1[ i ]));		// Re( target )
							i++;
							convBuf2[ i ] = convBuf1[ i ];		// Im( target )
							i++;
						}
					
						// make full spectrum (negative frequencies = conjugate positive frequencies)
						for( i = fullFFTsize + 2, j = fullFFTsize - 2; i < complexFFTsize; j -= 2 ) {
												// bug but nice?  - 1
							convBuf2[ i++ ] = convBuf2[ j ];
							convBuf2[ i++ ] = -convBuf2[ j+1 ];
						}
						// cepstrum domain
						Fourier.complexTransform( convBuf2, fullFFTsize, Fourier.INVERSE );
						
						// window
						m = phase[k];
						for( i = 0; m < fullFFTsize; ) {
							convBuf2[ i++ ] *= win[ m ];
							convBuf2[ i++ ] *= win[ m++ ];
						}
						for( m = 0; i < complexFFTsize; ) {
							convBuf2[ i++ ] *= win[ m ];
							convBuf2[ i++ ] *= win[ m++ ];
						}
					}
					
					// mix cepstra
					convBuf1	= fftBuf[0];
					convBuf2	= runOutFr.data[ ch ];
					Util.add( fftBuf[1], 0, convBuf1, 0, complexFFTsize );
					
					// back to frequency domain
					Fourier.complexTransform( convBuf1, fullFFTsize, Fourier.FORWARD );

					// calculate real exponential spectrum :
					// Mag( target ) = Exp( Re( source ))
					// Phase( target ) = Im( source )
					// ->convBuf2 shall be polar style, that makes things easy

					for( i = 0; i <= fullFFTsize; ) {
						convBuf2[ i ] = (float) Math.exp( convBuf1[ i ]);
						i++;
						convBuf2[ i ] = convBuf1[ i ];
						i++;
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
		PropertyGUI		gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbShift;pf"+Constants.unsignedModSpace+",pr"+PRN_ANGLE+"\n" );

		return gui;
	}
}
// class MindmachineOp
