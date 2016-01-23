/*
 *  CepstralOp.java
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

import java.awt.*;
import java.io.*;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFileDescr;

/**
 *  @version	0.71, 14-Nov-07
 */
public class CepstralOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Cepstral";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_LOFREQ		= 0;		// pr.para
	private static final int PR_HIFREQ		= 1;
	private static final int PR_QUALITY		= 0;		// pr.intg

	private static final String PRN_HIFREQ		= "HiFreq";
	private static final String PRN_LOFREQ		= "LoFreq";
	private static final String PRN_QUALITY		= "Quality";

	private static final int		prIntg[]		= { FIRDesignerDlg.QUAL_MEDIUM };
	private static final String	prIntgName[]	= { PRN_QUALITY };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_LOFREQ, PRN_HIFREQ };

	protected static final String ERR_BANDS			= "Band# not power of 2";

// -------- public Methoden --------
	// public Container createGUI( int type );

	public CepstralOp()
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
			static_pr.para		= prPara;
			static_pr.para[ PR_HIFREQ ]			= new Param( 22050.0, Param.ABS_HZ );
			static_pr.para[ PR_LOFREQ ]			= new Param(    50.0, Param.ABS_HZ );
			static_pr.paraName	= prParaName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder uebertragen
		opName		= "CepstralOp";
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
		int				ch, i, j;
		float			f1, f2, phase1, phase2;

		SpectStreamSlot	runInSlot;
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream	= null;

		SpectFrame		runInFr			= null;
		SpectFrame		runOutFr		= null;
	
		// Ziel-Frame Berechnung
		int				skip, srcBands, srcSize, fftSize, fullFFTsize;
		float			loFreq, hiFreq;
		float[]			fftBuf, fltBuf;

		AudioFileDescr		tmpStream;
		FilterBox		fltBox;
		Point			fltLength;

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

//			freqSpacing	= (runInStream.hiFreq - runInStream.loFreq) / runInStream.bands;
			srcBands	= runInStream.bands;
			srcSize		= srcBands << 1;
			loFreq		= Math.min( runInStream.smpRate / 2, (float) pr.para[ PR_LOFREQ ].val );
			hiFreq		= Math.max( 0.5f, Math.min( runInStream.smpRate / 2, (float) pr.para[ PR_HIFREQ ].val ));
			if( loFreq > hiFreq ) {
				f1		= loFreq;
				loFreq	= hiFreq;
				hiFreq	= f1;
			}

			fltBox			= new FilterBox();
			if( loFreq <= 0.1f ) {
				fltBox.filterType	= FilterBox.FLT_LOWPASS;
				fltBox.cutOff		= new Param( hiFreq, Param.ABS_HZ );
			} else if( hiFreq >= runInStream.smpRate/2 ) {
				fltBox.filterType	= FilterBox.FLT_HIGHPASS;
				fltBox.cutOff		= new Param( loFreq, Param.ABS_HZ );
			} else {
				fltBox.filterType	= FilterBox.FLT_BANDPASS;
				fltBox.cutOff		= new Param( (loFreq + hiFreq) / 2, Param.ABS_HZ );
				fltBox.bandwidth	= new Param(  hiFreq - loFreq,      Param.OFFSET_HZ );
			}
// System.out.println( "cutoff "+fltBox.cutOff+"; bandw "+fltBox.bandwidth );
			tmpStream		= new AudioFileDescr();	// unflexibel
			tmpStream.rate	= runInStream.smpRate;
			fltLength		= fltBox.calcLength( tmpStream, pr.intg[ PR_QUALITY ]);
			skip			= fltLength.x;								// complex support not written out
			i				= fltLength.x + fltLength.y;
			j				= i + srcBands - 1;							// length after conv. with input
			for( fftSize = 2; fftSize < j; fftSize <<= 1 ) ;
			fullFFTsize		= fftSize << 1;
			fftBuf			= new float[ fullFFTsize ];
			fltBuf			= new float[ fullFFTsize ];
			Util.clear( fltBuf );
			
			fltBox.calcIR( tmpStream, pr.intg[ PR_QUALITY ], Filter.WIN_BLACKMAN, fltBuf, fltLength );
			for( i = fftSize, j = fullFFTsize; i > 0; ) {
				fltBuf[ --j ]	= 0.0f;							// img
				fltBuf[ --j ]	= (float) fltBuf[ --i ];		// real
			}
			Util.rotate( fltBuf, fullFFTsize, fftBuf, -(skip<<1) );		// adjust support
			Fourier.complexTransform( fltBuf, fftSize, Fourier.FORWARD );
// Debug.view( fltBuf, "flt" );

// System.out.println( "SrcSize "+srcSize+"; fltSize "+(fltLength.x+fltLength.y)+"; fftSize "+fftSize );

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
					System.arraycopy( runInFr.data[ ch ], 0, fftBuf, 0, srcSize );
//					amp1		= fftBuf[ 0 ];
//					fftBuf[ 0 ]	= -1.0f;
					for( i = 0; i < srcSize; i+= 2 ) {
						f1			= fftBuf[ i ];					// Re(ln z)=ln r
						if( f1 > 1.266416555e-14f ) {
							fftBuf[ i ]  = (float) (Math.log( f1 ));	// (float) (Math.log( f1 ) / 32);
						} else {
							fftBuf[ i ]  = -32f; // -1.0f;			// c. ln( 2^-47 ) / 32
						}
					}
					Fourier.unwrapPhases( fftBuf, 0, fftBuf, 0, srcSize );
//					amp2	= fftBuf[ srcSize - 2 ];
					phase1	= fftBuf[ 1 ];
					phase2	= fftBuf[ srcSize - 1 ];
					f2		= (float) (srcSize - 2);
					for( i = 0; i < srcSize; ) {
						f1				= (float) i++ / f2;
//						fftBuf[ i++ ]  += amp1   * (f1 - 1.0f) - amp2   * f1;
						fftBuf[ i++ ]  += phase1 * (f1 - 1.0f) - phase2 * f1;
					}
//					for( i = fftSize, j = fftSize; i > 0; i -= 2, j += 2 ) {	// neg.freq. complex conjugate
//						fftBuf[ j ]		= fftBuf[ i ];
//						fftBuf[ j+1 ]	= -fftBuf[ i+1 ];
//					}
					for( i = srcSize; i < fullFFTsize; ) {
						fftBuf[ i++ ] = 0.0f;
					}
// if( runOutStream.framesWritten < 1 ) {
// 	Debug.view( fftBuf, "pre" );
// }

//					for( i = 0, f1 = 0.0f; i < srcSize; i++ ) {
//						f2 = Math.abs( fftBuf[ i ]);
//						if( f2 > f1 ) {
//							f1 = f2;
//						}
//					}
//					Util.mult( fftBuf, 0, srcSize, 1.0f / f1 );
					Fourier.complexTransform( fftBuf, fftSize, Fourier.FORWARD );
//					for( i = 0; i < loBand; ) {								// erase below (pos) lo
//						fftBuf[ i++ ] = 0.0f;
//						fftBuf[ i++ ] = 0.0f;
//					}
//					for( i = hiBand + 2; i < fullFFTsize - hiBand; ) {		// erase above (pos) hi / below (neg) hi
//						fftBuf[ i++ ] = 0.0f;
//						fftBuf[ i++ ] = 0.0f;
//					}
//					for( i = fullFFTsize - loBand; i < fullFFTsize; ) {		// erase above (neg) lo
//						fftBuf[ i++ ] = 0.0f;
//						fftBuf[ i++ ] = 0.0f;
//					}
					Fourier.complexMult( fltBuf, 0, fftBuf, 0, fftBuf, 0, fullFFTsize );
					Fourier.complexTransform( fftBuf, fftSize, Fourier.INVERSE );
//					Util.mult( fftBuf, 0, srcSize, f1 );
					f2		= (float) (srcSize - 2);
					for( i = 0; i < srcSize; ) {							// |exp z|=exp(Re(z))
						fftBuf[ i ]		= (float) Math.exp( fftBuf[ i ]);	// (float) Math.exp( fftBuf[ i ] * 32 );
//						fftBuf[ i++ ]	= (float) Math.exp( fftBuf[ i ] + amp1 * (1.0f - f1) + amp2 * f1 );	// (float) Math.exp( fftBuf[ i ] * 32 );
						f1				= (float) i++ / f2;
						fftBuf[ i++ ]  += phase1 * (1.0f - f1) + phase2 * f1;
					}
//					fftBuf[ 0 ] = amp1;
					System.arraycopy( fftBuf, 0, runOutFr.data[ ch ], 0, srcSize );
// if( runOutStream.framesWritten < 1 ) {
// 	Debug.view( fftBuf, "post" );
// }
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
		StringBuffer	qualJComboBox	= new StringBuffer();

		if( type != GUI_PREFS ) return null;

		for( int i = 0; i < FIRDesignerDlg.QUAL_NAMES.length; i++ ) {
			qualJComboBox.append( ",it" );
			qualJComboBox.append( FIRDesignerDlg.QUAL_NAMES[ i ]);
		}

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbHigh frequency;pf"+Constants.absHzSpace+",pr"+PRN_HIFREQ+"\n" +
			"lbLow frequency;pf"+Constants.absHzSpace+",pr"+PRN_LOFREQ+"\n" +
			"lbQuality;ch,pr"+PRN_QUALITY+qualJComboBox.toString()+"\n" );

		return gui;
	}
}
// class CepstralOp
