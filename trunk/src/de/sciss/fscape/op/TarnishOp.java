/*
 *  TarnishOp.java
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
public class TarnishOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Tarnish";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	// Properties (defaults)
	private static final int PR_LOFREQ		= 0;		// pr.para
	private static final int PR_HIFREQ		= 1;
	private static final int PR_WINDOW		= 0;		// pr.intg
	private static final int PR_OVERSMP		= 1;
	private static final int PR_MODE			= 2;
	private static final int PR_RESIDUAL		= 0;		// pr.bool
	private static final int PR_MINPHASE		= 1;

	protected static final int MODE_OVERTONES	= 0;
	protected static final int MODE_ALL			= 1;

	private static final String PRN_LOFREQ		= "LoFreq";
	private static final String PRN_HIFREQ		= "HiFreq";
	private static final String PRN_OVERSMP		= "OverSmp";
	private static final String PRN_WINDOW		= "Window";
	private static final String PRN_RESIDUAL		= "Residual";
	private static final String PRN_MINPHASE		= "MinPhase";
	private static final String PRN_MODE			= "Mode";

	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_LOFREQ, PRN_HIFREQ };
	private static final int		prIntg[]		= { 0, 0, MODE_OVERTONES };
	private static final String	prIntgName[]	= { PRN_OVERSMP, PRN_WINDOW, PRN_MODE };
	private static final boolean	prBool[]		= { false, true };
	private static final String	prBoolName[]	= { PRN_RESIDUAL, PRN_MINPHASE };

// -------- public Methoden --------
	// public Container createGUI( int type );

	public TarnishOp()
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
			static_pr.para[ PR_LOFREQ ]		= new Param(  100.0, Param.ABS_HZ );
			static_pr.para[ PR_HIFREQ ]		= new Param( 3000.0, Param.ABS_HZ );
			static_pr.paraName	= prParaName;
			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// superclass-Felder Ÿbertragen
		opName		= "TarnishOp";
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
		int				ch, i, j, k, m, n, p, q;
		float			f1, f2, f3;

		SpectStreamSlot	runInSlot;
		SpectStreamSlot	runOutSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream	= null;

		SpectFrame		runInFr			= null;
		SpectFrame		runOutFr		= null;
	
		// Ziel-Frame Berechnung
		int				srcBands, fftSize, fullFFTsize, complexFFTsize, winSize, winHalf;
		float[]			fftBuf, convBuf1, convBuf2, win;
		
		int[]			belowMin, aboveMin;
		float			freqSpacing, minAmp;
		float[]			slope, maxima;
		int				oversmp			= pr.intg[ PR_OVERSMP ];
		int				loBand, hiBand;
		int				numPeaks;
		
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
			runOutSlot					= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream				= new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ Vorberechnungen ------------------------------

			srcBands	= runInStream.bands;
			winSize		= (srcBands - 1) << 1 >> oversmp;
			winHalf		= winSize >> 1;
			win			= Filter.createFullWindow( winSize, pr.intg[ PR_WINDOW ]);
			fftSize		= srcBands - 1;
			fullFFTsize	= fftSize << 1;
			complexFFTsize = fullFFTsize << 1;
//			fftBuf		= new float[ fullFFTsize + 2];
			fftBuf		= new float[ complexFFTsize ];
			freqSpacing	= (runInStream.hiFreq - runInStream.loFreq) / runInStream.bands;	// bin width

//			for( i = 0; i < winSize; i++ ) {
//				fftBuf[ i ] = (float) (Math.sin( Math.PI/2 * i ) + Math.sin( Math.PI/4 * i )) * win[ i ];
//			}
//			for( ; i < fullFFTsize; ) fftBuf[ i++ ] = 0.0f;
			System.arraycopy( win, winHalf, fftBuf, 0, winHalf );	// Zero Phase Rotation!
			System.arraycopy( win, 0, fftBuf, fullFFTsize - winHalf, winHalf );
			win			= new float[ fftSize + 1];
			Fourier.realTransform( fftBuf, fullFFTsize, Fourier.FORWARD );
			f1	= 1.0f / fftBuf[ 0 ];
			for( i = 0, j = 0; i <= fullFFTsize; i += 2, j++ ) {
//				win[ j ]	= Math.max( 0.0f, fftBuf[ i ] * f1 );		// because of the zero phase we can just take the real values
				win[ j ]	= fftBuf[ i ] * f1;		// because of the zero phase we can just take the real values
			}
			for( j = 0; j <= fftSize; j++ ) {
				if( win[ j ] < 0.125f ) break;		// stop at -24 dB window slope falloff
			}
			winSize		= j;						// bin-smearing size of the window (till -12 dB)

// Debug.view( win, "win" );

			loBand		= Math.max( winSize,
							Math.min( srcBands - 1, (int) (((pr.para[ PR_LOFREQ].val - runInStream.loFreq) / freqSpacing) + 0.5) ));
			hiBand		= Math.min( srcBands - winSize,
							Math.max( loBand, (int) (((pr.para[ PR_HIFREQ].val - runInStream.loFreq) / freqSpacing) + 0.5) ));

// System.out.println( "bands "+srcBands+"; loBand "+loBand+"; hiBand "+hiBand+ "; winSize "+winSize );
			
			slope		= new float[ srcBands ];
			maxima		= new float[ srcBands ];
			belowMin	= new int[ srcBands ];
			aboveMin	= new int[ srcBands ];
//			minDamp		= 0.25f;
			minAmp		= 1.0e-3f;

//			win2Size	= fullFFTsize;
//			win2Half	= fftSize;
//			win2		= Filter.createFullWindow( win2Size, Filter.WIN_BLACKMAN );

// for( i = 0, j = 0; i < fullFFTsize; i+=2, j++ ) {
// 	slope[j] = fftBuf[i];
// }
// Debug.view( slope, "window spectrum" );
			
//			exp			= (Param.transform( pr.para[ PR_CONTRAST ], Param.ABS_AMP, ampRef, null )).val - 1.0;
//			maxGain		= (float) (Param.transform( pr.para[ PR_MAXBOOST ], Param.ABS_AMP, ampRef, null )).val;

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

// System.out.println( "frame "+runInStream.framesRead );

				for( ch = 0; ch < runOutStream.chanNum; ch++ ) {
					convBuf1	= runInFr.data[ ch ];
					convBuf2	= runOutFr.data[ ch ];

					Util.clear( maxima );

					f1			= 0.0f;
					for( i = 0, j = 0; j < srcBands; i += 2, j++ ) {
						f2			= f1;
						f1			= convBuf1[ i ];
						slope[ j ]	= f1 - f2;
					}

// if( runInStream.framesRead < 2 ) Debug.view( slope, "slope "+runInStream.framesRead );
					
//					numPeaks	= 0;
					
					f1			= slope[ 0 ];
					for( i = loBand, m = i << 1; i < (srcBands - winSize); i++, m += 2 ) {
						f2			= f1;
						f1			= slope[ i ];
						f3			= convBuf1[ m ];
						
peakPick:				if( (f2 >= 0.0f) && (f1 <= 0.0f) && (f3 > minAmp) ) { // local maximum
// System.out.println("   check "+ (i * freqSpacing + runInStream.loFreq) );
							k = i - winSize;
							for( j = i; j > k; ) {
								if( slope[ --j ] <= 0.0f ) break peakPick;
							}
							while( j > 0 ) {
								if( slope[ --j ] <= 0.0f ) break;
							}
							j++;
//							if( (convBuf1[ j << 1 ] / f3) > (2 * win[i-j]) ) break peakPick;
							k = i + winSize;
							for( p = i; p < k; ) {
								if( slope[ ++p ] >= 0.0f ) break peakPick;
							}
							while( p < fftSize ) {
								if( slope[ ++p ] >= 0.0f ) break;
							}
							p--;
//							if( (convBuf1[ m << 1 ] / f3) > (2 * win[m-i]) ) break peakPick;

							maxima[ i ]		= f3;
							belowMin[ i ]	= j;
							aboveMin[ i ]	= p;
//							numPeaks++;
// System.out.println( "peak "+i );
						}
					}

// System.out.println( numPeaks );

					// init allpass
					for( i = 0; i <= fftSize; ) {
						fftBuf[ i++ ] = 1.0f;
					}
					
					numPeaks=0;
					
					// kill overtones
					for( i = loBand; i <= hiBand; i++ ) {
						if( maxima[i] == 0.0f ) continue;
						
						switch( pr.intg[ PR_MODE ] ) {
						case MODE_OVERTONES:
							for( j = i << 1, q = 2; j < srcBands; j += i, q++ ) {
								for( m = Math.max( i + 1, j - winSize - q ); m < Math.min( srcBands, j + winSize + q ); m++ ) {
									if( maxima[m] == 0.0f ) continue;
	
									// overtone detected
									n	= belowMin[m];
									p	= aboveMin[m];
	
									for( n = m - 1, p = 0; p < (m - 1); n--, p++ ) {
										fftBuf[ n ] *= 1.0f - win[ p ]; // * f1;
									}
									fftBuf[ m ] = 0.0f;
									for( n = m+1, p = 0; p < fftSize - m; n++, p++ ) {
										fftBuf[ n ] *= 1.0f - win[ p ]; // * f1;
									}
	
									maxima[m] = 0.0f;
									numPeaks++;
								}
							}
							break;
						
						case MODE_ALL:
							m	= i;
							// overtone detected
							n	= belowMin[m];
							p	= aboveMin[m];

							for( n = m - 1, p = 0; p < (m - 1); n--, p++ ) {
								fftBuf[ n ] *= 1.0f - win[ p ]; // * f1;
							}
							fftBuf[ m ] = 0.0f;
							for( n = m+1, p = 0; p < fftSize - m; n++, p++ ) {
								fftBuf[ n ] *= 1.0f - win[ p ]; // * f1;
							}
	
							maxima[m] = 0.0f;
							numPeaks++;
							break;
						}
					}
					
// System.out.println( numPeaks );
// Debug.view( fftBuf, "filter "+runInStream.framesRead );

// float[] test= new float[fftSize];
// System.arraycopy( fftBuf, 0, test, 0, fftSize );
// Debug.view( test, "spect1" );

					if( pr.bool[ PR_RESIDUAL ]) {
						for( i = fftSize+1, j = fullFFTsize+2; i > 0; ) {
							fftBuf[--j]	= 0.0f;
							fftBuf[--j] = 1.0f - fftBuf[--i];
						}
					} else {
						for( i = fftSize+1, j = fullFFTsize+2; i > 0; ) {
							fftBuf[--j]	= 0.0f;
							fftBuf[--j] = fftBuf[--i];
						}
					}

//for( i = 0; i <= fullFFTsize; i += 2 ) {
//	fftBuf[ i ]	 = (float) (0.6 + 0.4 * Math.sin( i * 0.001 ));
//}
/*
float[] ttt= new float[fftSize+1];
float[] fftBuf2= new float[fullFFTsize+2];
for( i = 0, j = 0; i <= fullFFTsize; i+=2, j++ ) {
	ttt[j] = fftBuf[i];
}
Debug.view( ttt, "amp spect1" );
System.arraycopy( fftBuf, 0, fftBuf2, 0, fullFFTsize + 2 );
Fourier.realTransform( fftBuf2, fullFFTsize, Fourier.INVERSE );
Debug.view( fftBuf2, "time1" );
*/
					// ---- make minimum phase ----
					if( pr.bool[ PR_MINPHASE ] && (numPeaks > 0) ) {
					
						// log magnitude
						for( i = 0; i <= fullFFTsize; i += 2 ) {
							fftBuf[ i ] = (float) Math.log( Math.max( 1.0e-24, fftBuf[ i ]));
						}
						// make full spectrum
						for( i = fullFFTsize + 2, j = fullFFTsize - 2; i < complexFFTsize; j -= 2 ) {
							fftBuf[ i++ ] = fftBuf[ j ];
							fftBuf[ i++ ] = 0.0f;
						}
						// cepstrum domain
						Fourier.complexTransform( fftBuf, fullFFTsize, Fourier.INVERSE );
						// fold cepstrum (make anticausal parts causal)
						for( i = 2, j = complexFFTsize - 2; i < fullFFTsize; i += 2, j -= 2 ) {
							fftBuf[ i ]   += fftBuf[ j ];		// add conjugate left wing to right wing
							fftBuf[ i+1 ] -= fftBuf[ j+1 ];
						}
						i++;
						fftBuf[ i ] = -fftBuf[ i ];
						i++;
						// clear left wing
						while( i < complexFFTsize ) fftBuf[ i++ ] = 0.0f;
						// back to frequency domain
						Fourier.complexTransform( fftBuf, fullFFTsize, Fourier.FORWARD );
						// complex exponential (mag' = exp(real); phase' = imag)
						for( i = 0; i <= fullFFTsize; i += 2 ) {
							fftBuf[ i ]		= (float) Math.exp( fftBuf[ i ]);
							// imag untouched -> rect2polar automatic
						}
					}
/*
for( i = 0, j = 0; i <= fullFFTsize; i+=2, j++ ) {
	ttt[j] = fftBuf[i];
}
Debug.view( ttt, "amp spect2" );
System.arraycopy( fftBuf, 0, fftBuf2, 0, fullFFTsize + 2 );
Fourier.realTransform( fftBuf2, fullFFTsize, Fourier.INVERSE );
Debug.view( fftBuf2, "time2" );
*/

					for( i = 0; i <= fullFFTsize; ) {
						convBuf2[ i ] = convBuf1[ i ] * fftBuf[ i ];
						i++;
						convBuf2[ i ] = convBuf1[ i ] + fftBuf[ i ];
						i++;
					}
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
		PropertyGUI		gui;
		String[]		winNames	= Filter.getWindowNames();
		StringBuffer	winChoise	= new StringBuffer();

		if( type != GUI_PREFS ) return null;

		for( int i = 0; i < winNames.length; i++ ) {
			winChoise.append( ",it" );
			winChoise.append( winNames[ i ]);
		}

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbLow frequency;pf"+Constants.absHzSpace+",pr"+PRN_LOFREQ+"\n" +
			"lbHigh frequency;pf"+Constants.absHzSpace+",pr"+PRN_HIFREQ+"\n" +
			"lbTarnish;ch,pr"+PRN_MODE+",itOvertones,itAll Harmonics\n"+
			"lbWindow;ch,pr"+PRN_WINDOW+winChoise.toString()+"\n"+
			"lbOversampling;ch,pr"+PRN_OVERSMP+"," +
				"it1x (none)," +
				"it2x," +
				"it4x," +
				"it8x\n"+
			"cbMinimum phase,pr"+PRN_MINPHASE+"\n"+
			"cbResidual,pr"+PRN_RESIDUAL+"\n" );

		return gui;
	}
}
// class TarnishOp
