/*
 *  SmearOp.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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
public class SmearOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Smear";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_DECAYMOD		= 0;		// pr.bool
	private static final int PR_MODE			= 0;		// pr.intg
	private static final int PR_DRYAPPLY		= 1;
	private static final int PR_DECAY			= 0;		// pr.para
	private static final int PR_DECAYMODDEPTH	= 1;
	private static final int PR_DECAYMODENV	= 0;		// pr.envl

	private static final int PR_MODE_SMEAR	= 0;	// "Nachleuchten"
	private static final int PR_MODE_FREEZE	= 1;	// akkumulierter Durchschnitt (=> "Spektrum")

	private static final int PR_APPLY_NONE	= 0;
	private static final int PR_APPLY_SUB		= 1;
	private static final int PR_APPLY_THRESH	= 2;	// never inverts phase

	private static final String PRN_DECAYMOD		= "DecayMod";
	private static final String PRN_MODE			= "Mode";
	private static final String PRN_DRYAPPLY		= "LoDepth";
	private static final String PRN_DECAY			= "Decay";
	private static final String PRN_DECAYMODDEPTH	= "DecayModDepth";
	private static final String PRN_DECAYMODENV	= "DecayModEnv";

	private static final boolean	prBool[]		= { false };
	private static final String	prBoolName[]	= { PRN_DECAYMOD };
	private static final int		prIntg[]		= { PR_MODE_SMEAR, PR_APPLY_NONE };
	private static final String	prIntgName[]	= { PRN_MODE, PRN_DRYAPPLY };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_DECAY, PRN_DECAYMODDEPTH };
	private static final Envelope	prEnvl[]		= { null };
	private static final String	prEnvlName[]	= { PRN_DECAYMODENV };

// -------- public Methoden --------
	// public Container createGUI( int type );

	public SmearOp()
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
			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;

			static_pr.para		= prPara;
			static_pr.para[ PR_DECAY ]			= new Param( 60, Param.DECIBEL_AMP );
			static_pr.para[ PR_DECAYMODDEPTH ]	= new Param( 60, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;

			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_DECAYMODENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName	= prEnvlName;

			static_pr.superPr	= Operator.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder Ÿbertragen		
		opName		= "SmearOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon
		icon = new OpIcon( this, OpIcon.ID_SMEAR, defaultName );
	}

// -------- Runnable Methoden --------

	public void run()
	{
		runInit();		// superclass

		// Haupt-Variablen fuer den Prozess
		SpectStreamSlot	runInSlot;
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream	= null;

		SpectFrame		runInFr			= null;
		SpectFrame		runOutFr		= null;
		SpectFrame		bufFr			= null;
	
		// Berechnungs-Grundlagen
		Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		Param			decayBase;
	
		// Modulations-Variablen
		Param			decay;
		Modulator		decayMod		= null;

		// Ziel-Frame Berechnung
		float	srcAmp, srcPhase;
		float	srcAmp2, srcPhase2;
		double	destReal, destImg;
		int		divisor					= 0;
		
topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			runInSlot = (SpectStreamSlot) slots.elementAt( SLOT_INPUT );
			if( runInSlot.getLinked() == null ) {
				runStop();	// threadDead = true -> folgendes for() wird Ÿbersprungen
			}
			// diese while Schleife ist nštig, da beim initReader ein Pause eingelegt werden kann
			// und die InterruptException ausgelšst wird; danach versuchen wir es erneut
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
			runOutSlot	= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream = new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------
			
			// Konstanten
			decayBase	= Param.transform( new Param( pr.para[ PR_DECAY ].val * SpectStream.framesToMillis(
										   runInStream, 1 ) / 1000, pr.para[ PR_DECAY].unit ),
										   Param.ABS_AMP, ampRef, runInStream );

			decay		= decayBase;		// zunaechst unmoduliert

			// Modulatoren erzeugen
			if( pr.bool[ PR_DECAYMOD ]) {
				decayMod	= new Modulator( decayBase, pr.para[ PR_DECAYMODDEPTH ],
											 pr.envl[ PR_DECAYMODENV ], runInStream );
			}

			// leeres Buffer-Frame
			bufFr = new SpectFrame( runInStream.chanNum, runInStream.bands );
			SpectFrame.clear( bufFr );
			
			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
			// ---------- Process: (modulierte) Variablen ----------
				if( pr.bool[ PR_DECAYMOD ]) {
					decay	= decayMod.calc();
				}
				
			// ---------- Frame einlesen ----------
	 			for( boolean readDone = false; (readDone == false) && !threadDead; ) {
					try {
						runInFr		= runInSlot.readFrame();	// throws InterruptedException
						readDone	= true;
					}
					catch( InterruptedException e ) {}
					catch( EOFException e ) {
						break mainLoop;
					}
					runCheckPause();
				}
				if( threadDead ) break mainLoop;

			// ---------- Process: Ziel-Frame berechnen ----------
			
				switch( pr.intg[ PR_MODE ]) {
				case PR_MODE_SMEAR:							// ---------- Smear-Modus ----------

					switch( pr.intg[ PR_DRYAPPLY ]) {
					case PR_APPLY_NONE:

						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] / (float) decay.val;
								srcPhase	= bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								destImg	   	= srcAmp * Math.sin( srcPhase );
								destReal   	= srcAmp * Math.cos( srcPhase );
								srcAmp		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								destImg	   += srcAmp * Math.sin( srcPhase );
								destReal   += srcAmp * Math.cos( srcPhase );
								
								bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] =
									(float) Math.sqrt( destImg*destImg + destReal*destReal );
								bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
									(float) Math.atan2( destImg, destReal );
							}
						}
						runOutFr = new SpectFrame( bufFr );
						break;
					
					case PR_APPLY_SUB:

						runOutFr = runOutStream.allocFrame();
						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] / (float) decay.val;
								srcPhase	= bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] = srcAmp;
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = srcPhase;
								
								destImg	   	= srcAmp * Math.sin( srcPhase );
								destReal   	= srcAmp * Math.cos( srcPhase );
								srcAmp		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								destImg	   += srcAmp * Math.sin( srcPhase );
								destReal   += srcAmp * Math.cos( srcPhase );
								
								bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] =
									(float) Math.sqrt( destImg*destImg + destReal*destReal );
								bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
									(float) Math.atan2( destImg, destReal );
							}
						}
						break;
					
					case PR_APPLY_THRESH:

						runOutFr = runOutStream.allocFrame();
						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] / (float) decay.val;
								srcPhase	= bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];						
								destImg	   	= srcAmp * Math.sin( srcPhase );
								destReal   	= srcAmp * Math.cos( srcPhase );
								srcAmp2		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase2	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								destImg	   += srcAmp2 * Math.sin( srcPhase2 );
								destReal   += srcAmp2 * Math.cos( srcPhase2 );
								
								bufFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] =
									(float) Math.sqrt( destImg*destImg + destReal*destReal );
								bufFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
									(float) Math.atan2( destImg, destReal );

								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ]	= Math.max( 0.0f,
									srcAmp - srcAmp2 );
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = srcPhase;
							}
						}
						break;
					}
					break;
	
				case PR_MODE_FREEZE:					// ---------- Freeze-Modus ----------

					divisor++;
					runOutFr = runOutStream.allocFrame();

					switch( pr.intg[ PR_DRYAPPLY ]) {
					case PR_APPLY_NONE:

						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								
								bufFr.data[ ch ][ band << 1 ]       += srcAmp * Math.sin( srcPhase );
								bufFr.data[ ch ][ (band << 1) + 1 ] += srcAmp * Math.cos( srcPhase );

								destImg		= bufFr.data[ ch ][ band << 1 ]       / divisor;
								destReal	= bufFr.data[ ch ][ (band << 1) + 1 ] / divisor;

								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ]	= 
									(float) Math.sqrt( destImg*destImg + destReal*destReal );
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
									(float) Math.atan2( destImg, destReal );
							}
						}
						break;
						
					case PR_APPLY_SUB:

						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								destImg		= srcAmp * Math.sin( srcPhase );
								destReal	= srcAmp * Math.cos( srcPhase );
								
								bufFr.data[ ch ][ band << 1 ]       += destImg;
								bufFr.data[ ch ][ (band << 1) + 1 ] += destReal;

								destImg	   -= bufFr.data[ ch ][ band << 1 ]       / divisor;
								destReal   -= bufFr.data[ ch ][ (band << 1) + 1 ] / divisor;

								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ]	= 
									(float) Math.sqrt( destImg*destImg + destReal*destReal );
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] =
									(float) Math.atan2( destImg, destReal );
							}
						}
						break;

					case PR_APPLY_THRESH:

						for( int ch = 0; ch < runOutStream.chanNum; ch++ ) {
							for( int band = 0; band < runOutStream.bands; band++ ) {

								srcAmp		= runInFr.data[ ch ][ (band << 1) + SpectFrame.AMP ];
								srcPhase	= runInFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ];
								
								bufFr.data[ ch ][ band << 1 ]       += srcAmp * Math.sin( srcPhase );
								bufFr.data[ ch ][ (band << 1) + 1 ] += srcAmp * Math.cos( srcPhase );

								destImg		= bufFr.data[ ch ][ band << 1 ]       / divisor;
								destReal	= bufFr.data[ ch ][ (band << 1) + 1 ] / divisor;

								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ]	= Math.max( 0.0f,
									srcAmp - (float) Math.sqrt( destImg*destImg + destReal*destReal ));
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = srcPhase;
							}
						}
						break;
					}
					break;
				}

				// Berechnung fertich

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
			"lbMode;ch,ac0|1|en|2|en,ac1|1|di|2|di,pr"+PRN_MODE+"," +
				"itSmear," +
				"itFreeze\n" +
			"lbDecay per sec;pf"+Constants.decibelAmpSpace+",id1,pr"+PRN_DECAY+"\n" +
			"lbDry application;ch,pr"+PRN_DRYAPPLY+"," +
				"itNone," +
				"itSubtract," +
				"itThreshold\n" +
			"gl"+GroupLabel.NAME_MODULATION+"\n" +
			"cbDecay,actrue|3|en|4|en,acfalse|3|di|4|di,id2,pr"+PRN_DECAYMOD+";" +
			"pf"+Constants.decibelAmpSpace+"|"+Constants.offsetAmpSpace+",re1,id3,pr"+PRN_DECAYMODDEPTH+
				";en,id4,pr"+PRN_DECAYMODENV );

		return gui;
	}
}
// class SmearOp
