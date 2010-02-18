/*
 *  LogFreqOp.java
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

import java.io.EOFException;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.Random;

import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;

/**
 *	Operator to map the (linear) frequencies to
 *	logarithmic frequencies
 *
 *  @version	0.72, 04-Jan-09
 */
public class LogFreqOp
extends Operator
{
// -------- public fields --------

// -------- private fields --------

	protected static final String defaultName = "Log freq";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_RANDPHASE		= 0;		// pr.bool
//	private static final int PR_CALCTYPE		= 0;		// pr.intg
	private static final int PR_HIFREQ			= 0;		// pr.para
	private static final int PR_LOFREQ			= 1;
//	private static final int PR_MIDMODENV		= 0;		// pr.envl

	private static final String PRN_RANDPHASE		= "RandPhase";
	private static final String PRN_HIFREQ			= "HiFreq";
	private static final String PRN_LOFREQ			= "LoFreq";

	private static final boolean prBool[]		= { false };
	private static final String	prBoolName[]	= { PRN_RANDPHASE };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_HIFREQ, PRN_LOFREQ };
	
// -------- public Methoden --------
	// public Container createGUI( int type );

	public LogFreqOp()
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

			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_HIFREQ ]			= new Param( 18000.0, Param.ABS_HZ );
			static_pr.para[ PR_LOFREQ ]			= new Param(    32.0, Param.ABS_HZ );
			static_pr.paraName	= prParaName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder übertragen		
		opName		= "LogFreqOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon
		icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
	}

// -------- Runnable Methoden --------

	public void run()
	{
		runInit();

		final SpectStreamSlot	runInSlot;
		final SpectStreamSlot	runOutSlot;
		SpectStream				runInStream		= null;
		SpectStream				runOutStream	= null;

		SpectFrame				runInFr			= null;
		SpectFrame				runOutFr		= null;
	
		double					srcFreq;
		float					srcBand;
		final int[]				srcFloorBands, srcCeilBands;
		final float[]			srcFloorWeights, srcCeilWeights;

		int						srcFloorBand;
		int						srcCeilBand;
		float					srcFloorWeight;
		float					srcCeilWeight;
		
		float					srcAmp, srcPhase;
		double					destReal, destImg;

		final float				loFreq, hiFreq, freqSpacing;
		final boolean			randPhase	= pr.bool[ PR_RANDPHASE ];
		final float				pi			= (float) Math.PI;
		int						startBand = 0, stopBand;

topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			runInSlot = (SpectStreamSlot) slots.elementAt( SLOT_INPUT );
			if( runInSlot.getLinked() == null ) {
				runStop();	// threadDead = true -> folgendes for() wird übersprungen
			}
			// diese while Schleife ist nötig, da beim initReader ein Pause eingelegt werden kann
			// und die InterruptException ausgelöst wird; danach versuchen wir es erneut
			for( boolean initDone = false; !initDone && !threadDead; ) {
				try {
					runInStream	= runInSlot.getDescr();	// throws InterruptedException
					initDone = true;
				}
				catch( InterruptedException e ) {}
				runCheckPause();
			}
			if( threadDead ) break topLevel;

			// srcBand[ x ] ist das Band, daß für das x-te destBand ausgelesen werden muß
			srcFloorBands	= new int[ runInStream.bands + 1 ];		// ein Overhead-Band!
			// dazugehörige Gewichtung 0...100%; zur Interpolation wird addiert
			srcFloorWeights	= new float[ runInStream.bands + 1 ];

			srcCeilBands	= new int[ runInStream.bands + 1 ];
			srcCeilWeights	= new float[ runInStream.bands + 1 ];
			stopBand		= runInStream.bands;

			// Frequency spacing (linear!)
			freqSpacing	= (runInStream.hiFreq - runInStream.loFreq) / runInStream.bands;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot	= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream = new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			loFreq		= (float) pr.para[ PR_LOFREQ ].val;
			hiFreq		= (float) pr.para[ PR_HIFREQ ].val;
//			freqSpan	= hiFreq / loFreq;
			
//			// calculate lookup tables
//			for( int band = 0; band <= runInStream.bands; band++ ) {
//				srcFreq				= Math.pow( freqSpan, (double) band / runInStream.bands ) * loFreq;
//				srcBand				= (float) ((srcFreq - runInStream.loFreq) / freqSpacing);
//				srcBands[ band ]	= (int) Math.floor( srcBand );			// Ceil = srcBands[ x + 1 ]
//				srcWeights[ band ]	= 1.0f - (srcBand - srcBands[ band ]);
//			}
			
//			final float firstBandFreq = runInStream.loFreq + freqSpacing;
//			final double expLinFactor = (runInStream.bands - 1) / Math.log( runInStream.hiFreq / firstBandFreq );
			final double expLinFactor = (runInStream.bands - 1) / Math.log( hiFreq / loFreq );

			for( int band = 0; band <= runInStream.bands; band++ ) {
				srcFreq					= runInStream.loFreq + band * freqSpacing;
				srcBand 				= (float) (Math.log( srcFreq / loFreq ) * expLinFactor + 1);
				srcFloorBands[ band ]	= (int) Math.floor( srcBand );			// Floor = srcBands[ x + 1 ]
				srcFloorWeights[ band ]	= 1.0f - (srcBand - srcFloorBands[ band ]);
//				System.out.println( "band " + band + " -> srcBand = " + srcBand );
			}

fixLp:		for( int band = 0; band < runInStream.bands; band++ ) {
				srcFloorBand	= srcFloorBands[ band ];
				srcCeilBand		= srcFloorBands[ band + 1 ];
				srcFloorWeight	= srcFloorWeights[ band ];
				srcCeilWeight	= 1.0f - (srcFloorWeights[ band + 1 ]);

				if( srcFloorBand < 0 ) {
					srcFloorBand	= 0;
					if( srcFloorBand < srcCeilBand ) {
						srcFloorWeight	= 1.0f;
					} else {
						startBand = band + 1;
						continue fixLp;
					}
				}
				if( srcCeilBand >= runInStream.bands ) {
					srcCeilBand		= runInStream.bands - 1;
					if( srcCeilBand > srcFloorBand ) {
						srcCeilWeight	= 1.0f;
					} else {
						stopBand = band;
						break fixLp;
					}
				}
				
				if( srcFloorBand == srcCeilBand ) {
					srcFloorWeight = srcCeilWeight - (1.0f - srcFloorWeight);
					srcCeilWeight  = 0.0f;
//					assert( srcFloorWeight >= 0f );
				}
				
				srcFloorBands[ band ]	= srcFloorBand;
				srcFloorWeights[ band ]	= srcFloorWeight;
				srcCeilBands[ band ]	= srcCeilBand;
				srcCeilWeights[ band ]	= srcCeilWeight;
				
//				System.out.println( "band = " + band + " -> srcFloorBand = " + srcFloorBand + "; srcFloorWeight = " + srcFloorWeight + "; srcCeilBand = " + srcCeilBand + "; srcCeilWeight = " + srcCeilWeight );
			}

/*
			// debug: check the cumulative weights
			for( int band = startBand; band < stopBand; band++ ) {
				srcFloorBand	= srcFloorBands[ band ];
				srcCeilBand		= srcCeilBands[ band ];
				srcFloorWeight	= srcFloorWeights[ band ];
				srcCeilWeight	= srcCeilWeights[ band ];
				float f1 = srcFloorWeight;
				for( int i = srcFloorBand + 1; i < srcCeilBand; i++ ) {
					f1 += 1f;
				}
				f1 += srcCeilWeight;
				System.out.println( "band " + band + " -> " + f1 );
			}
*/
			// ------------------------------ main loop ------------------------------
			runSlotsReady();
			
final Random rnd = new Random();
			
mainLoop:	while( !threadDead ) {

			// ---------- read input frame ----------
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
				
			// ---------- calculate output frame ----------
				for( int band = 0; band < startBand; band++ ) {
					for( int ch = 0; ch < runInStream.chanNum; ch++ ) {
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] = 0.0f;
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = 0.0f;
					}
				}
				
				for( int band = startBand; band < stopBand; band++ ) {

					srcFloorBand	= srcFloorBands[ band ];
					srcCeilBand		= srcCeilBands[ band ];
					srcFloorWeight	= srcFloorWeights[ band ];
					srcCeilWeight	= srcCeilWeights[ band ];

					for( int ch = 0; ch < runInStream.chanNum; ch++ ) {		// alle Kanaele
						
						// unterstes Band berechnen
						srcAmp		= runInFr.data[ ch ][ (srcFloorBand << 1) + SpectFrame.AMP ];
						srcPhase	= runInFr.data[ ch ][ (srcFloorBand << 1) + SpectFrame.PHASE ];
						destImg		= srcAmp * Math.sin( srcPhase ) * srcFloorWeight;
						destReal	= srcAmp * Math.cos( srcPhase ) * srcFloorWeight;

						// Zwischenbaender addieren (sofern vorhanden)
						for( int i = srcFloorBand + 1; i < srcCeilBand; i++ ) {
							srcAmp		= runInFr.data[ ch ][ (i << 1) + SpectFrame.AMP ];
							srcPhase	= runInFr.data[ ch ][ (i << 1) + SpectFrame.PHASE ];
							destImg	   += srcAmp * Math.sin( srcPhase );	// Gewichtung 1.0
							destReal   += srcAmp * Math.cos( srcPhase );
						}
						
						if( srcCeilWeight > 0 ) {
							// oberstes Band addieren
							srcAmp		= runInFr.data[ ch ][ (srcCeilBand << 1) + SpectFrame.AMP ];
							srcPhase	= runInFr.data[ ch ][ (srcCeilBand << 1) + SpectFrame.PHASE ];
							destImg	   += srcAmp * Math.sin( srcPhase ) * srcCeilWeight;
							destReal   += srcAmp * Math.cos( srcPhase ) * srcCeilWeight;
						}
						
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] =
							(float) Math.sqrt( destImg*destImg + destReal*destReal );
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
							randPhase ? rnd.nextFloat() * pi : 
										(float) Math.atan2( destImg, destReal );
							
					}
				} // Berechnung fertich

				for( int band = stopBand; band < runInStream.bands ; band++ ) {
					for( int ch = 0; ch < runInStream.chanNum; ch++ ) {
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] = 0.0f;
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = 0.0f;
					}
				}

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
		catch( AlreadyBoundException e ) {
			runQuit( e );
			return;
		}
//		catch( OutOfMemoryException e ) {
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
			"lbHigh frequency;pf"+Constants.absHzSpace+",pr"+PRN_HIFREQ+"\n" +
			"lbLow frequency;pf"+Constants.absHzSpace+",pr"+PRN_LOFREQ+"\n" +
			"cbRandom Phase,pr"+PRN_RANDPHASE );

		return gui;
	}
}
// class LogFreqOp
