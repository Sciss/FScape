/*
 *  UnitorOp.java
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
public class UnitorOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Unitor";		// "real" name (z.B. Icons)

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	protected static final int NUM_INPUT		= 6;		// Zahl der Slots

	// Slots
	protected static final int SLOT_INPUT		= 0;		// bis 5
	protected static final int SLOT_OUTPUT		= NUM_INPUT;
	
	// Properties (defaults)
	private static final int PR_OPERATION		= 0;		// Array-Indices: pr.intg
	private static final int PR_DATAFORM		= 1;
	private static final int PR_ADJUSTGAIN	= 0;		// pr.bool
	private static final int PR_DELAY			= 1;		// bis 6
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_DELAYTIME		= 1;		// bis 6

	private static final int PR_OPERATION_COLLECT		= 0;
	private static final int PR_OPERATION_ADD			= 1;
	private static final int PR_OPERATION_SUBSTRACT	= 2;
	private static final int PR_OPERATION_MULTIPLY	= 3;
	private static final int PR_OPERATION_DIVIDE		= 4;
	private static final int PR_OPERATION_AND			= 5;
	private static final int PR_OPERATION_OR			= 6;
	private static final int PR_OPERATION_XOR			= 7;
	private static final int PR_OPERATION_EXP			= 8;
	private static final int PR_OPERATION_LOG			= 9;
	private static final int PR_OPERATION_MODULO		= 10;
	private static final int PR_OPERATION_ATAN		= 11;

	private static final int PR_DATAFORM_AMP			= 0;
	private static final int PR_DATAFORM_RECT			= 1;
	private static final int PR_DATAFORM_POLAR		= 2;

	private static final String PRN_OPERATION		= "Operation";	// Property-Keynames
	private static final String PRN_DATAFORM		= "DataForm";
	private static final String PRN_ADJUSTGAIN	= "AdjustGain";
	private static final String PRN_GAIN			= "Gain";
	private static final String PRN_DELAY			= "Delay";		// 1...6
	private static final String PRN_DELAYTIME		= "DelayTime";	// 1...6

	private static final int		prIntg[]		= { PR_OPERATION_ADD, PR_DATAFORM_RECT };
	private static final String	prIntgName[]	= { PRN_OPERATION, PRN_DATAFORM };

// -------- public Methoden --------
	// public PropertyGUI createGUI( int type );

	public UnitorOp()
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
			static_pr.bool		= new boolean[ 1 + NUM_INPUT ];
			static_pr.boolName	= new String[ 1 + NUM_INPUT ];
			static_pr.para		= new Param[ 1 + NUM_INPUT ];
			static_pr.paraName	= new String[ 1 + NUM_INPUT ];
			
			static_pr.bool[ PR_ADJUSTGAIN ]		= false;
			static_pr.boolName[ PR_ADJUSTGAIN ]	= PRN_ADJUSTGAIN;
			static_pr.para[ PR_GAIN ]			= new Param( 0.0, Param.DECIBEL_AMP );
			static_pr.paraName[ PR_GAIN ]		= PRN_GAIN;
			
			for( int i = 0; i < NUM_INPUT; i++ ) {
			
				static_pr.para[ PR_DELAYTIME + i ]			= new Param( (double) i, Param.ABS_BEATS );
				static_pr.paraName[ PR_DELAYTIME + i ]		= PRN_DELAYTIME + (i+1);
				static_pr.bool[ PR_DELAY + i ]				= false;
				static_pr.boolName[ PR_DELAY + i ]			= PRN_DELAY + (i+1);
			}
			
			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		
		// superclass-Felder Ÿbertragen		
		opName		= "UnitorOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		for( int i = 0; i < NUM_INPUT; i++ ) {
			slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER,
												    Slots.SLOTS_DEFREADER +(i+1) ));	// SLOT_INPUT
		}
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));		// SLOT_OUTPUT

		// icon
		icon = new OpIcon( this, OpIcon.ID_UNITOR, defaultName );
	}

// -------- Runnable Methoden --------

	/**
	 *	TESTSTADIUM XXX
	 */
	public void run()
	{
		runInit();		// superclass

		int i, j, ch, numFinished;
		float	f1;

		// Haupt-Variablen fuer den Prozess
		SpectStreamSlot	runInSlot[]		= new SpectStreamSlot[ NUM_INPUT ];
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream[]	= new SpectStream[ NUM_INPUT ];
		SpectStream		runOutStream;

		SpectFrame		runInFr[]		= new SpectFrame[ NUM_INPUT ];
		SpectFrame		runOutFr;
//		int				runSlotNum[]	= new int[ NUM_INPUT ];
		int				runChanStart[]	= new int[ NUM_INPUT ];

		// Berechnungs-Grundlagen
		Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		double			gain			= 1.0;

		// andere
		int				numIn			= 0;	// number of used slots
		int				oldReadDone;
		int				readable;
		int				chanNum			= 0;
		float			srcData[];				// Frame-Daten eines Kanals
		float			destData[];
		int				srcCh;
		int				dataLen;
		
		float			srcAmp, srcPhase;		// Polar <==> Rect
		double			srcReal, srcImg;
		
		boolean			wantsRect;				// true if polar->rect transform required
		boolean			wantsInt;				// true if float->int  transform required
		int				intFrame[][]	= null;	// fuer AND/OR/XOR brauchen wir Integer!

topLevel:
		try {
			// ------------------------------ Input-Slots ------------------------------
			for( i = 0; (i < NUM_INPUT) && !threadDead; i++ ) {
				try {
					runInFr[ numIn ]	= null;
					runInSlot[ numIn ]	= (SpectStreamSlot) slots.elementAt( SLOT_INPUT + i );

					if( runInSlot[ numIn ].getLinked() != null ) {
						runInStream[ numIn] = runInSlot[ numIn ].getDescr(); // throws InterruptedException

						if( pr.intg[ PR_OPERATION ] == PR_OPERATION_COLLECT ) {
							runChanStart[ numIn ]	= chanNum;
							chanNum				   += runInStream[ numIn ].chanNum;
						} else {
							runChanStart[ numIn ]	= 0;
							chanNum					= Math.max( chanNum, runInStream[ numIn ].chanNum );
						}	
						numIn++;
					}
				}
				catch( InterruptedException e ) {
					i--;	// retry this getDescr()
				}
				runCheckPause();
			}
			if( numIn == 0 ) runStop();	// threadDead = true
			if( threadDead ) break topLevel;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot	= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream = new SpectStream( runInStream[ 0 ]);
			runOutStream.setChannels( chanNum );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------
			if( pr.bool[ PR_ADJUSTGAIN ]) {
				gain	= (float) Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
												   ampRef, runInStream[ 0 ]).val;
				if( Math.abs( gain - 1.0f ) < 0.01f ) {
					gain = 1.0f;	// schneller kopieren
				}
			};

			wantsRect	= (pr.intg[ PR_DATAFORM ]  == PR_DATAFORM_RECT);
			wantsInt	= (pr.intg[ PR_OPERATION ] == PR_OPERATION_AND) ||
						  (pr.intg[ PR_OPERATION ] == PR_OPERATION_OR)  ||
//						  (pr.intg[ PR_OPERATION ] == PR_OPERATION_MODULO)  ||
						  (pr.intg[ PR_OPERATION ] == PR_OPERATION_XOR);

			if( wantsInt ) {
				intFrame = new int[ runOutStream.chanNum ][ runOutStream.bands << 1 ];
			}
			
			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
			// ---------- Frame einlesen ----------
				for( int readDone = 0; (readDone < numIn) && !threadDead; ) {
	
					oldReadDone = readDone;
	
					for( i = 0, numFinished = 0; i < numIn; i++ ) {
						try {	// Unterbrechung

							if( runInFr[ i ] == null ) {		// noch nicht gelesen

								readable = runInStream[ i ].framesReadable();
								if( readable > 0 ) {
									runInFr[ i ] = runInSlot[ i ].readFrame();
									readDone++;

								} else if( readable < 0 ) {	// Stream geschlossen, leeres Frame erzeugen

									numFinished++;
									if( numFinished == numIn ) break mainLoop;	// fertisch
	
									runInFr[ i ] = runInStream[ i ].allocFrame();
									for( ch = 0; ch < runInFr[ i ].data.length; ch++ ) {
										srcData = runInFr[ i ].data[ ch ];
										for( j = 0; j < srcData.length; j++ ) {
											srcData[ j ] = 0.0f;
										}
									}
									readDone++;
								}
							}
						}
						catch( InterruptedException e ) {
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

				if( threadDead ) break mainLoop;	// Schluss machen

			// ---------- Process: Ziel-Frame berechnen ----------

				runOutFr = runOutStream.allocFrame();

				if( pr.intg[ PR_OPERATION ] == PR_OPERATION_COLLECT ) {	// Kanaele zusammenfassen
					
					for( i = 0; i < numIn; i++ ) {
						for( ch = 0; ch < runInFr[ i ].data.length; ch++ ) {
						
							srcData		= runInFr[ i ].data[ ch ];
							destData	= runOutFr.data[ runChanStart[ i ] + ch ];

							System.arraycopy( srcData, 0, destData, 0,
											  Math.min( destData.length, srcData.length ));

							// ggf. overhead nullen
							for( j = srcData.length; j < destData.length; j++ ) {
								destData[ j ] = 0.0f;
							}
						}
					}

				} else {	// mathematische Operationen

		// ---------- erster Schritt: unterstes Frame ins Ziel kopieren, ggf. Polar => Rect / Float => int
					
					srcCh	= runInFr[ 0 ].data.length;
					if( !wantsRect ) {
						if( !wantsInt ) {
						
							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								srcData		= runInFr[ 0 ].data[ ch % srcCh ];	// ggf. Kanaele wiederholen
								destData	= runOutFr.data[ ch ];
						
								System.arraycopy( srcData, 0, destData, 0,
												  Math.min( destData.length, srcData.length ));

								// ggf. overhead nullen
								for( j = srcData.length; j < destData.length; j++ ) {
									destData[ j ] = 0.0f;
								}
							}
						
						} else {	// !wantsRect && wantsInt
						
							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								srcData	= runInFr[ 0 ].data[ ch % srcCh ];	// ggf. Kanaele wiederholen
								dataLen	= Math.min( intFrame[ ch ].length, srcData.length );
	
								for( j = 0; j < dataLen; j += 2 ) {
									intFrame[ ch ][ j+SpectFrame.AMP ] = (int) (srcData[ j+SpectFrame.AMP ] *
																		 0x7FFFFFFF);
									intFrame[ ch ][ j+SpectFrame.PHASE]= (int) (((srcData[ j+SpectFrame.PHASE]+
																		 Math.PI) % Constants.PI2) *
																		 0x145F306D);
								}								
							
								// ggf. overhead nullen
								for( j = srcData.length; j < intFrame[ ch ].length; j += 2 ) {
									intFrame[ ch ][ j+SpectFrame.AMP ] = 0;
									intFrame[ ch ][ j+SpectFrame.PHASE]= 0x3FFFFFFF;
								}
							}
						}
					} else {
						if( !wantsInt ) {		// && wantsRect
						
							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								srcData		= runInFr[ 0 ].data[ ch % srcCh ];	// ggf. Kanaele wiederholen
								destData	= runOutFr.data[ ch ];
								dataLen		= Math.min( destData.length, srcData.length );
						
								for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ]  = srcAmp * (float) Math.cos( srcPhase );	// real
										destData[ j+1 ]  = srcAmp * (float) Math.sin( srcPhase );	// img
								}
						
								// ggf. overhead nullen
								for( j = srcData.length; j < destData.length; j++ ) {
									destData[ j ] = 0.0f;
								}
							}							
						
						} else {				// wantsRect && wantsInt

							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								srcData	= runInFr[ 0 ].data[ ch % srcCh ];	// ggf. Kanaele wiederholen
								dataLen	= Math.min( intFrame[ ch ].length, srcData.length );
	
								for( j = 0; j < dataLen; j += 2 ) {
									srcAmp					= srcData[ j + SpectFrame.AMP ] * 0x3FFFFFFF;
									srcPhase				= srcData[ j + SpectFrame.PHASE ];
									intFrame[ ch ][ j ]		= (int) (srcAmp * Math.cos( srcPhase ));
									intFrame[ ch ][ j+1 ]	= (int) (srcAmp * Math.sin( srcPhase ));
								}								
							
								// ggf. overhead nullen
								for( j = srcData.length; j < intFrame[ ch ].length; j++ ) {
									intFrame[ ch ][ j ] = 0;
								}
							}
						}
					}

		// ---------- zweiter Schritt: alle folgenden via spezifizierte Operation hinzufuegen
					
					for( i = 1; i < numIn; i++ ) {
						srcCh = runInFr[ i ].data.length;
						for( ch = 0; ch < runOutFr.data.length; ch++ ) {
							srcData		= runInFr[ i ].data[ ch % srcCh ];	// ggf. Kanaele wiederholen
							destData	= runOutFr.data[ ch ];
							dataLen		= Math.min( destData.length, srcData.length );

							switch( pr.intg[ PR_OPERATION ]) {
							case PR_OPERATION_ADD:						// -------- Addition --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] += srcData[ j + SpectFrame.AMP ];
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j ] += srcData[ j ];
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ] += srcAmp * Math.cos( srcPhase );
										destData[ j+1 ] += srcAmp * Math.sin( srcPhase );
									}
									break;
								default:
									break;
								}
								break;

							case PR_OPERATION_SUBSTRACT:				// -------- Subtraktion --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										f1 = destData[ j + SpectFrame.AMP ] - srcData[ j + SpectFrame.AMP ];
										destData[ j + SpectFrame.AMP ] = Math.max( 0.0f, f1 );
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j++ ) {
										destData[ j ] -= srcData[ j ];
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ] -= srcAmp * Math.cos( srcPhase );
										destData[ j+1 ] -= srcAmp * Math.sin( srcPhase );
									}
									break;
								default:
									break;
								}							
								break;

							case PR_OPERATION_MULTIPLY:					// -------- Multiplikation --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] *= srcData[ j + SpectFrame.AMP ];
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j++ ) {
										destData[ j ] *= srcData[ j ];
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ] *= srcAmp * Math.cos( srcPhase );
										destData[ j+1 ] *= srcAmp * Math.sin( srcPhase );
									}
									break;
								default:
									break;
								}							
								break;

							case PR_OPERATION_DIVIDE:					// -------- Division --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] = (float) (destData[ j + SpectFrame.AMP ] /
											Math.max( srcData[ j+ SpectFrame.AMP ], 1.0e-6 )) % 1.0f;
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
//										destData[ j + SpectFrame.AMP ] *= 0.5f / (0.5f +
//											srcData[ j+ SpectFrame.AMP ]);
//										destData[ j + SpectFrame.PHASE ] /= srcData[ j+ SpectFrame.PHASE ];
										destData[ j + SpectFrame.AMP ] = (float) (destData[ j + SpectFrame.AMP ] /
											Math.max( srcData[ j+ SpectFrame.AMP ], 1.0e-6 )) % 1.0f;
										destData[ j + SpectFrame.PHASE ] = (float)
											((destData[ j + SpectFrame.PHASE ] /
											Math.max( srcData[ j+ SpectFrame.PHASE ] + Math.PI, 1.0e-6 )) %
											Constants.PI2);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
//										srcAmp			 = 0.5f / (0.5f + srcData[ j + SpectFrame.AMP ]);
//										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
//										destData[ j   ] *= srcAmp * Math.cos( srcPhase );
//										destData[ j+1 ] *= srcAmp * Math.sin( srcPhase );
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ]  = (float) (destData[ j   ] /
											Math.max( srcAmp * (1.0 + Math.cos( srcPhase )), 1.0e-6 )) % 1.0f;
										destData[ j+1 ]  = (float) (destData[ j+1 ] /
											Math.max( srcAmp * (1.0 + Math.sin( srcPhase )), 1.0e-6 )) % 1.0f;
									}
									break;
								default:
									break;
								}
								break;

							case PR_OPERATION_AND:						// -------- AND --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] &= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] &= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
										intFrame[ ch ][ j + SpectFrame.PHASE ] &= (int)
											(srcData[ j + SpectFrame.PHASE ] * 0x145F306D);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp					= srcData[ j + SpectFrame.AMP ] * 0x3FFFFFFF;
										srcPhase				= srcData[ j + SpectFrame.PHASE ];
										intFrame[ ch ][ j ]	   &= (int) (srcAmp * Math.cos( srcPhase ));
										intFrame[ ch ][ j+1 ]  &= (int) (srcAmp * Math.sin( srcPhase ));
									}
									break;
								default:
									break;
								}							
								break;

							case PR_OPERATION_OR:						// -------- OR --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] |= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] |= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
										intFrame[ ch ][ j + SpectFrame.PHASE ] |= (int)
											(srcData[ j + SpectFrame.PHASE ] * 0x145F306D);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp					= srcData[ j + SpectFrame.AMP ] * 0x3FFFFFFF;
										srcPhase				= srcData[ j + SpectFrame.PHASE ];
										intFrame[ ch ][ j ]	   |= (int) (srcAmp * Math.cos( srcPhase ));
										intFrame[ ch ][ j+1 ]  |= (int) (srcAmp * Math.sin( srcPhase ));
									}
									break;
								default:
									break;
								}							
								break;

							case PR_OPERATION_XOR:						// -------- XOR --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] ^= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] ^= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
										intFrame[ ch ][ j + SpectFrame.PHASE ] ^= (int)
											(srcData[ j + SpectFrame.PHASE ] * 0x145F306D);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp					= srcData[ j + SpectFrame.AMP ] * 0x3FFFFFFF;
										srcPhase				= srcData[ j + SpectFrame.PHASE ];
										intFrame[ ch ][ j ]	   ^= (int) (srcAmp * Math.cos( srcPhase ));
										intFrame[ ch ][ j+1 ]  ^= (int) (srcAmp * Math.sin( srcPhase ));
									}
									break;
								default:
									break;
								}							
								break;

							case PR_OPERATION_EXP:
								break;

							case PR_OPERATION_LOG:
								break;

							case PR_OPERATION_MODULO:					// -------- Modulo --------
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] %= Math.max(
											srcData[ j + SpectFrame.AMP ], 1.0e-6 );
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] %= Math.max(
											srcData[ j + SpectFrame.AMP ], 1.0e-6 );
										destData[ j + SpectFrame.PHASE] %= Math.max(
											srcData[ j + SpectFrame.PHASE] + Math.PI, 1.0e-6 );
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j   ] %= Math.max(
											srcAmp * (1.0 + Math.cos( srcPhase )) / 2, 1.0e-6 );
										destData[ j+1 ] %= Math.max(
											srcAmp * (1.0 + Math.sin( srcPhase )) / 2, 1.0e-6 );
									}
									break;
								default:
									break;
								}					
								break;
/*								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] %= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										intFrame[ ch ][ j + SpectFrame.AMP ] %= (int)
											(srcData[ j + SpectFrame.AMP ] * 0x7FFFFFFF);
										intFrame[ ch ][ j + SpectFrame.PHASE ] %= (int)
											(srcData[ j + SpectFrame.PHASE ] * 0x145F306D);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp					= srcData[ j + SpectFrame.AMP ] * 0x3FFFFFFF;
										srcPhase				= srcData[ j + SpectFrame.PHASE ];
										intFrame[ ch ][ j ]	    = (int) (intFrame[ ch ][ j ] %
																  (srcAmp * (1.0 + Math.cos( srcPhase ))/2));
										intFrame[ ch ][ j+1 ]   = (int) (intFrame[ ch ][ j+1 ] %
																  (srcAmp * (1.0 + Math.sin( srcPhase ))/2));
									}
									break;
								default:
									break;
								}							
								break;
*/
							case PR_OPERATION_ATAN:
								switch( pr.intg[ PR_DATAFORM ]) {
								case PR_DATAFORM_AMP:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] = 1.0f - (float) (Math.atan2(
											destData[ j + SpectFrame.AMP ],
											srcData[ j + SpectFrame.AMP ]) / Constants.PIH);
									}
									break;
								case PR_DATAFORM_POLAR:
									for( j = 0; j < dataLen; j += 2 ) {
										destData[ j + SpectFrame.AMP ] = 1.0f - (float) (Math.atan2(
											destData[ j + SpectFrame.AMP ],
											srcData[ j + SpectFrame.AMP ]) / Constants.PIH);
										destData[ j + SpectFrame.PHASE ] = (float) Math.atan2(
											destData[ j + SpectFrame.PHASE ],
											srcData[ j + SpectFrame.PHASE ]);
									}
									break;
								case PR_DATAFORM_RECT:
									for( j = 0; j < dataLen; j += 2 ) {
										srcAmp			 = srcData[ j + SpectFrame.AMP ];
										srcPhase		 = srcData[ j + SpectFrame.PHASE ];
										destData[ j ]	 = (float) (Math.atan2(
											destData[ j ], srcAmp * Math.cos( srcPhase )) / Math.PI);
										destData[ j+1 ]	 = (float) (Math.atan2(
											destData[ j+1 ], srcAmp * Math.sin( srcPhase )) / Math.PI);
									}
									break;
								default:
									break;
								}							
								break;

							default:
								break;
							}
						}
					}
					
		// ---------- letzter Schritt: ggf. Int nach Float zurŸckrechnen bzw. Rect => Polar
				
					if( !wantsInt ) {
						if( wantsRect ) {
						
							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								destData = runOutFr.data[ ch ];

								for( j = 0; j < destData.length; j += 2 ) {						
									srcReal	= destData[ j ];
									srcImg	= destData[ j + 1 ];
									destData[ j + SpectFrame.AMP ]		= (float)
											Math.sqrt( srcReal*srcReal + srcImg*srcImg );
									destData[ j + SpectFrame.PHASE ]	= (float)
											Math.atan2( srcImg, srcReal );			
								}
							}
						}
					} else {
						if( !wantsRect ) {	// && wantsInt

							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								destData	= runOutFr.data[ ch ];
	
								for( j = 0; j < destData.length; j += 2 ) {
									destData[ j+SpectFrame.AMP ] = (float) intFrame[ ch ][ j+SpectFrame.AMP ] /
																   0x7FFFFFFF;
									destData[ j+SpectFrame.PHASE]= (float)
																   ((double) intFrame[ ch ][ j+SpectFrame.PHASE]/
																   0x145F306D - Math.PI);
								}
							}

						} else {	// wantsInt && wantsRect

							for( ch = 0; ch < runOutFr.data.length; ch++ ) {
								destData = runOutFr.data[ ch ];

								for( j = 0; j < destData.length; j += 2 ) {
									srcReal	= (double) intFrame[ ch ][ j ]   / 0x3FFFFFFF;
									srcImg	= (double) intFrame[ ch ][ j+1 ] / 0x3FFFFFFF;
									destData[ j + SpectFrame.AMP ]		= (float)
											Math.sqrt( srcReal*srcReal + srcImg*srcImg );
									destData[ j + SpectFrame.PHASE ]	= (float)
											Math.atan2( srcImg, srcReal );			
								}
							}
						}
					}
				}
		// ---------- Frame berechnung Ende

				// Lautstaerke anpassen
				if( gain != 1.0f ) {
					for( ch = 0; ch < runOutFr.data.length; ch++ ) {
						destData = runOutFr.data[ ch ];
						for( j = 0; j < destData.length; j += 2 ) {
							destData[ j + SpectFrame.AMP ] *= gain;
						}
					}
				}

				for( i = 0; i < numIn; i++ ) {
					runInSlot[ i ].freeFrame( runInFr[ i ]);
					runInFr[ i ] = null;
				}

			// ---------- Frame schreiben ----------

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

			for( i = 0; i < numIn; i++ ) {
				runInStream[ i ].closeReader();
			}
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
		PropertyGUI		gui;
		StringBuffer	delay		= new StringBuffer();
		StringBuffer	opAction	= new StringBuffer( "ac0|101|di" );

		if( type != GUI_PREFS ) return null;

		for( int i = 1; i <= NUM_INPUT; i++ ) {
			delay.append( "\ncbSlot "+i+",actrue|"+i+"|en,acfalse|"+i+"|di,pr"+
						  pr.boolName[ PR_DELAY + (i-1) ] + ";pf"+Constants.absMsSpace+"|"+
						  Constants.absBeatsSpace+"|"+Constants.ratioTimeSpace+",id"+i+",pr"+
						  pr.paraName[ PR_DELAYTIME + (i-1) ] );
		}
		for( int i = 1; i <= PR_OPERATION_ATAN; i++ ) {
			opAction.append( ",ac"+i+"|101|en" );
		}
		
		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbOperation;ch,pr"+PRN_OPERATION + ","+opAction+","+
				"itCollect channels,"+
				"itAdd up,"+
				"itSubstract,"+
				"itMultiply,"+
				"itDivide,"+
				"itBoolean AND,"+
				"itBoolean OR,"+
				"itBoolean XOR,"+
				"itPower,"+
				"itLogarithm,"+
				"itModulo,"+
				"itArcus tangens\n"+
			"lbOperate on;ch,id101,pr"+PRN_DATAFORM+"," +
				"itAmplitude,"+
				"itZ-plane rect,"+
				"itPolar (phase/amp)\n"+
			"cbAdjust gain,actrue|100|en,acfalse|100|di,pr"+PRN_ADJUSTGAIN+";"+
			"pf"+Constants.decibelAmpSpace+",id100,pr"+PRN_GAIN+"\n" +
			"glInput slot delay" + delay );

		return gui;
	}
}
// class UnitorOp
