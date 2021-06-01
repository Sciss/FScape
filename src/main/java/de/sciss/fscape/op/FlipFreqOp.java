/*
 *  FlipFreqOp.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.op;

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
import de.sciss.fscape.util.Envelope;
import de.sciss.fscape.util.Modulator;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;

import java.io.EOFException;
import java.io.IOException;

/**
 *	Operator zum Umkehren der Frequenzverteilung
 */
public class FlipFreqOp
		extends Operator {

// -------- private variables --------

	protected static final String defaultName = "Flip freq";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_FLIPFREQ		= 0;		// pr.bool
	private static final int PR_MIDMOD		= 1;
	private static final int PR_SHIFTMOD		= 2;
	private static final int PR_HIMOD			= 3;
	private static final int PR_LOMOD			= 4;
//	private static final int PR_CALCTYPE		= 0;		// pr.intg
	private static final int PR_MIDFREQ		= 0;		// pr.para
	private static final int PR_SHIFTFREQ		= 1;
	private static final int PR_HIFREQ		= 2;
	private static final int PR_LOFREQ		= 3;
	private static final int PR_MIDMODDEPTH	= 4;
	private static final int PR_SHIFTMODDEPTH	= 5;
	private static final int PR_HIMODDEPTH	= 6;
	private static final int PR_LOMODDEPTH	= 7;
	private static final int PR_MIDMODENV		= 0;		// pr.envl
	private static final int PR_SHIFTMODENV	= 1;
	private static final int PR_HIMODENV		= 2;
	private static final int PR_LOMODENV		= 3;

	private static final String PRN_FLIPFREQ		= "FlipFreq";
	private static final String PRN_MIDFREQ		= "MidFreq";
	private static final String PRN_SHIFTFREQ		= "ShiftFreq";
	private static final String PRN_HIFREQ		= "HiFreq";
	private static final String PRN_LOFREQ		= "LoFreq";
	private static final String PRN_MIDMOD		= "MidMod";
	private static final String PRN_SHIFTMOD		= "ShiftMod";
	private static final String PRN_HIMOD			= "HiMod";
	private static final String PRN_LOMOD			= "LoMod";
	private static final String PRN_MIDMODDEPTH	= "MidModDepth";
	private static final String PRN_SHIFTMODDEPTH	= "ShiftModDepth";
	private static final String PRN_HIMODDEPTH	= "HiModDepth";
	private static final String PRN_LOMODDEPTH	= "LoModDepth";
	private static final String PRN_MIDMODENV		= "MidModEnv";
	private static final String PRN_SHIFTMODENV	= "ShiftModEnv";
	private static final String PRN_HIMODENV		= "HiModEnv";
	private static final String PRN_LOMODENV		= "LoModEnv";
	private static final String PRN_CALCTYPE		= "CalcType";

//	private static final int PR_CALCTYPE_FACTOR		= 0;
	private static final int PR_CALCTYPE_FREQRANGE	= 1;

	private static final boolean	prBool[]		= { true, false, false, false, false };
	private static final String	prBoolName[]	= { PRN_FLIPFREQ, PRN_MIDMOD, PRN_SHIFTMOD, PRN_HIMOD, PRN_LOMOD };
	private static final int		prIntg[]		= { PR_CALCTYPE_FREQRANGE };
	private static final String	prIntgName[]	= { PRN_CALCTYPE };
	private static final Param	prPara[]		= { null, null, null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_MIDFREQ, PRN_SHIFTFREQ, PRN_HIFREQ, PRN_LOFREQ,
														PRN_MIDMODDEPTH, PRN_SHIFTMODDEPTH, PRN_HIMODDEPTH, PRN_LOMODDEPTH };
	private static final Envelope	prEnvl[]		= { null, null, null, null };
	private static final String	prEnvlName[]	= { PRN_MIDMODENV, PRN_SHIFTMODENV, PRN_HIMODENV, PRN_LOMODENV };
	
// -------- public methods --------
	// public Container createGUI( int type );

	public FlipFreqOp()
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
			static_pr.para[ PR_MIDFREQ ]		= new Param( 1760.0, Param.ABS_HZ );
			static_pr.para[ PR_SHIFTFREQ ]		= new Param(    0.0, Param.OFFSET_HZ );
			static_pr.para[ PR_HIFREQ ]			= new Param( 7040.0, Param.ABS_HZ );
			static_pr.para[ PR_LOFREQ ]			= new Param(    0.0, Param.ABS_HZ );
			static_pr.para[ PR_MIDMODDEPTH ]	= new Param(   12.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_SHIFTMODDEPTH ]	= new Param(   12.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_HIMODDEPTH ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
			static_pr.para[ PR_LOMODDEPTH ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
			static_pr.paraName	= prParaName;

			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_MIDMODENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envl[ PR_SHIFTMODENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envl[ PR_HIMODENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envl[ PR_LOMODENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName	= prEnvlName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder uebertragen
		opName		= "FlipFreqOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));		// SLOT_INPUT
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon
		icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
	}

// -------- Runnable methods --------

	public void run()
	{
		runInit();		// superclass

		// Haupt-Variablen fuer den Prozess
		SpectStreamSlot	runInSlot;
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream;

		SpectFrame		runInFr			= null;
		SpectFrame		runOutFr		= null;
	
		// Berechnungs-Grundlagen; in Hz
		Param			midBase, shiftBase, hiBase, loBase;
		int				phaseFactor;		// beim Flipping muss die Phase gedreht werden!

		// Modulations-Variablen
		boolean			recalc			= true;		// false, wenn sich die Werte nicht geaendert haben
		Param			midFreq;
		Param			shiftFreq;
		Param			hiFreq;
		Param			loFreq;
		Modulator		midMod			= null;
		Modulator		shiftMod		= null;
		Modulator		hiMod			= null;
		Modulator		loMod			= null;
		Param			foo;

		// fuer Lookup-Berechnung
		double			freqSpacing;		// linear mode: bandwidth (Hz)
		double			hiScaling;			// Scalierung der Frequenzen oberhalb von midFreq
		double			loScaling;			// Scalierung der Frequenzen unterhalb von midFreq
		double			loRange;
		double			hiRange;
		double			destFreq;
		double			srcFreq;
		float			srcBand;
		int				srcBands[];			// Beschreibung siehe new()-Befehl unten
		float			srcWeights[];

		// Ziel-Frame Berechnung
		int				srcFloorBand;
		int				srcCeilBand;
		float			srcFloorWeight;
		float			srcCeilWeight;
		
		float	srcAmp, srcPhase;
		double	destReal, destImg;

	topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			runInSlot = slots.elementAt( SLOT_INPUT );
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
				catch( InterruptedException ignored) {}
				runCheckPause();
			}
			if( threadDead ) break topLevel;

			// srcBand[ x ] ist das Band, das fuer das x-te destBand ausgelesen werden muss
			srcBands	= new int[ runInStream.bands + 1 ];		// ein Overhead-Band!
			// dazugehoerige Gewichtung 0...100%; zur Interpolation wird addiert
			srcWeights	= new float[ runInStream.bands + 1 ];
			// Frequency spacing (linear!)
			freqSpacing	= (runInStream.hiFreq - runInStream.loFreq) / runInStream.bands;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot	= slots.elementAt( SLOT_OUTPUT );
			runOutStream = new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------
			// Phasenumkehrung
			if( pr.bool[ PR_FLIPFREQ ]) {
				phaseFactor	= -1;
			} else {
				phaseFactor = +1;
			}

			// constants
			midBase		= Param.transform( pr.para[ PR_MIDFREQ ],   Param.ABS_HZ, null,    runInStream );
			shiftBase	= Param.transform( pr.para[ PR_SHIFTFREQ ], Param.ABS_HZ, midBase, runInStream );
			hiBase		= Param.transform( pr.para[ PR_HIFREQ ],    Param.ABS_HZ, midBase, runInStream );
			loBase		= Param.transform( pr.para[ PR_LOFREQ ],    Param.ABS_HZ, midBase, runInStream );

			midFreq		= midBase;		// zunaechst unmoduliert
			shiftFreq	= shiftBase;
			hiFreq		= hiBase;
			loFreq		= loBase;

			// Modulatoren erzeugen
			if( pr.bool[ PR_MIDMOD ]) {
				midMod		= new Modulator( midBase, pr.para[ PR_MIDMODDEPTH ],
											 pr.envl[ PR_MIDMODENV ], runInStream );
			}
			if( pr.bool[ PR_SHIFTMOD ]) {
				shiftMod	= new Modulator( shiftBase, pr.para[ PR_SHIFTMODDEPTH ],
											 pr.envl[ PR_SHIFTMODENV ], runInStream );
			}
			if( pr.bool[ PR_HIMOD ]) {
				hiMod		= new Modulator( hiBase, pr.para[ PR_HIMODDEPTH ],
											 pr.envl[ PR_HIMODENV ], runInStream );
			}
			if( pr.bool[ PR_LOMOD ]) {
				loMod		= new Modulator( loBase, pr.para[ PR_LOMODDEPTH ],
											 pr.envl[ PR_LOMODENV ], runInStream );
			}
			
			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
			// ---------- Process: (modulierte) Variablen ----------
				if( pr.bool[ PR_MIDMOD ]) {
					foo	= midMod.calc();
					if( Math.abs( foo.value - midFreq.value) >= 0.1 ) {		// 0.1 Hz treshhold improves speed
						midFreq		= foo;
						recalc		= true;
					}
				}
				if( pr.bool[ PR_SHIFTMOD ]) {
					foo	= shiftMod.calc();
					if( Math.abs( foo.value - shiftFreq.value) >= 0.1 ) {
						shiftFreq	= foo;
						recalc		= true;
					}
				}
				if( pr.bool[ PR_HIMOD ]) {
					foo	= hiMod.calc();
					if( Math.abs( foo.value - hiFreq.value) >= 0.1 ) {
						hiFreq		= foo;
						recalc		= true;
					}
				}
				if( pr.bool[ PR_LOMOD ]) {
					foo = loMod.calc();
					if( Math.abs( foo.value - loFreq.value) >= 0.1 ) {
						loFreq		= foo;
						recalc		= true;
					}
				}
				
			// ---------- Frame einlesen ----------
	 			for( boolean readDone = false; (!readDone) && !threadDead; ) {
					try {
						runInFr		= runInSlot.readFrame();	// throws InterruptedException
						readDone	= true;
						runOutFr	= runOutStream.allocFrame();
					}
					catch( InterruptedException ignored) {}
					catch( EOFException e ) {
						break mainLoop;
					}
					runCheckPause();
				}
				if( threadDead ) break mainLoop;

			// ---------- Process: Lookup-Table berechnen ----------
				if( recalc ) {									// something's changed...

					// Scalierungen
					loRange		= Math.max( 1, midFreq.value - loFreq.value);
					hiRange		= Math.max( 1, hiFreq.value - midFreq.value);
					hiScaling	= loRange / hiRange;
					loScaling	= hiRange / loRange;
							
					// Lookup-Tabelle berechnen
					if( pr.bool[ PR_FLIPFREQ ]) {		// flipped
			
						for( int band = 0; band <= runInStream.bands; band++ ) {	// ein Overhead-Band!
				
							destFreq = band * freqSpacing + runOutStream.loFreq - shiftFreq.value;
							if( destFreq >= midFreq.value) {
								srcFreq = midFreq.value - (destFreq - midFreq.value) * hiScaling;
							} else {
								srcFreq = (midFreq.value - destFreq) * loScaling + midFreq.value;
							}
			
							srcBand				= (float) ((srcFreq - runInStream.loFreq) / freqSpacing);
							srcBands[ band ]	= (int) Math.floor( srcBand );			// Floor = srcBands[ x + 1 ]
							srcWeights[ band ]	= srcBand - srcBands[ band ];
// System.out.println( "Band "+srcBands[band]+" ==> "+band+", weight "+srcWeights[band]*100+"%" );
						}
	
					} else {			// not flipped
						for( int band = 0; band <= runInStream.bands; band++ ) {
				
							destFreq = band * freqSpacing + runOutStream.loFreq + shiftFreq.value;
							if( destFreq >= midFreq.value) {
								srcFreq = (destFreq - midFreq.value) * loScaling + midFreq.value;
							} else {
								srcFreq = midFreq.value - (midFreq.value - destFreq) * hiScaling;
							}
			
							srcBand				= (float) ((srcFreq - runInStream.loFreq) / freqSpacing);
							srcBands[ band ]	= (int) Math.floor( srcBand );			// Ceil = srcBands[ x + 1 ]
							srcWeights[ band ]	= srcBand - srcBands[ band ];
						}
					}
					recalc = false;
				}
				
			// ---------- Process: Ziel-Frame berechnen ----------
bandLp:			for( int band = 0; band < runInStream.bands; band++ ) {

					// unterstes + oberstes Band; Gewichtung
					srcFloorBand		= srcBands[ band ];
					srcCeilBand			= srcBands[ band + 1 ];
					if( srcFloorBand > srcCeilBand ) {	// kleinstes zu unterst
						srcFloorBand	= srcBands[ band + 1 ];
						srcCeilBand		= srcBands[ band ];
						srcFloorWeight	= 1.0f - srcWeights[ band + 1 ];
						srcCeilWeight	= srcWeights[ band ];
					} else {
						srcFloorWeight	= 1.0f - srcWeights[ band ];
						srcCeilWeight	= srcWeights[ band + 1 ];
					}
					if( srcFloorBand < 0 ) {
						srcFloorBand		= 0;
						if( srcFloorBand < srcCeilBand ) {
							srcFloorWeight	= 1.0f;
						} else {
							// voellig ausserhalb; alles Null setzen geht schneller
							for( int ch = 0; ch < runInStream.chanNum; ch++ ) {
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] = 0.0f;
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = 0.0f;
							}
							continue bandLp;
						}
					}
					if( srcCeilBand >= runInStream.bands ) {
						srcCeilBand			= runInStream.bands - 1;
						if( srcCeilBand > srcFloorBand ) {
							srcCeilWeight	= 1.0f;
						} else {
							// voellig ausserhalb; alles Null setzen geht schneller
							for( int ch = 0; ch < runInStream.chanNum; ch++ ) {
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] = 0.0f;
								runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = 0.0f;
							}
							continue bandLp;
						}
					}

					if( srcFloorBand == srcCeilBand ) {
						srcFloorWeight = srcCeilWeight - (1.0f - srcFloorWeight);
						srcCeilWeight  = 0.0f;
//						assert( srcFloorWeight >= 0f );
					}
					
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
						
						// oberstes Band addieren
						srcAmp		= runInFr.data[ ch ][ (srcCeilBand << 1) + SpectFrame.AMP ];
						srcPhase	= runInFr.data[ ch ][ (srcCeilBand << 1) + SpectFrame.PHASE ];
						destImg	   += srcAmp * Math.sin( srcPhase ) * srcCeilWeight;
						destReal   += srcAmp * Math.cos( srcPhase ) * srcCeilWeight;

						runOutFr.data[ ch ][ (band << 1) + SpectFrame.AMP ] =
							(float) Math.sqrt( destImg*destImg + destReal*destReal );
						runOutFr.data[ ch ][ (band << 1) + SpectFrame.PHASE ] = phaseFactor *
							(float) Math.atan2( destImg, destReal );
					}
				} // calculation done

				runInSlot.freeFrame( runInFr );

				for( boolean writeDone = false; (!writeDone) && !threadDead; ) {
					try {	// Unterbrechung
						runOutSlot.writeFrame( runOutFr );	// throws InterruptedException
						writeDone = true;
						runFrameDone( runOutSlot, runOutFr  );
						runOutStream.freeFrame( runOutFr );
					}
					catch( InterruptedException ignored) {}	// mainLoop wird eh gleich verlassen
					runCheckPause();
				}
			} // end of main loop

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
//		catch( OutOfMemoryException e ) {
//			abort( e );
//			return;
//		}
		
		runQuit( null );
	}

// -------- GUI methods --------

	public PropertyGUI createGUI( int type )
	{
		PropertyGUI gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"cbFreq bands upside-down,pr"+PRN_FLIPFREQ+"\n" +
			"lbMiddle frequency;pf"+Constants.absHzSpace+",id1,pr"+PRN_MIDFREQ+"\n" +
			"lbPost freq shift;pf"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re1,id2,pr"+PRN_SHIFTFREQ+"\n" +
			"glCalculation\n" +
			"lbScaling;ch,pr"+PRN_CALCTYPE+"," +
				"itby factor," +
				"itby freq range\n" +
			"lbHigh frequency;pf"+Constants.absHzSpace+"|"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re1,id3,pr"+PRN_HIFREQ+"\n" +
			"lbLow frequency;pf"+Constants.absHzSpace+"|"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re1,id4,pr"+PRN_LOFREQ+"\n" +
			"gl"+GroupLabel.NAME_MODULATION+"\n" +
			"cbMiddle freq,actrue|5|en|6|en,acfalse|5|di|6|di,pr"+PRN_MIDMOD+";" +
			"pf"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re1,id5,pr"+PRN_MIDMODDEPTH+";en,id6,pr"+PRN_MIDMODENV+"\n" +
			"cbShift freq,actrue|7|en|8|en,acfalse|7|di|8|di,pr"+PRN_SHIFTMOD+";" +
			"pf"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re2,id7,pr"+PRN_SHIFTMODDEPTH+";en,id8,pr"+PRN_SHIFTMODENV+"\n" +
			"cbHigh freq,actrue|9|en|10|en,acfalse|9|di|10|di,pr"+PRN_HIMOD+";" +
			"pf"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re3,id9,pr"+PRN_HIMODDEPTH+";en,id10,pr"+PRN_HIMODENV+"\n" +
			"cbLow freq,actrue|11|en|12|en,acfalse|11|di|12|di,pr"+PRN_LOMOD+";" +
			"pf"+Constants.offsetFreqSpace+"|"+Constants.offsetHzSpace+
				"|"+Constants.offsetSemitonesSpace+",re4,id11,pr"+PRN_LOMODDEPTH+";en,id12,pr"+PRN_LOMODENV );

		return gui;
	}
}