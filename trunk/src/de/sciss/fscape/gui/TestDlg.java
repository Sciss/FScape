/*
 *  TestDlg.java
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
 
package de.sciss.fscape.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *	Tried something out.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class TestDlg
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
	private static final int PR_CALCLEN			= 1;
	private static final int PR_STEPSIZE		= 2;
	private static final int PR_LPORDER			= 3;
	private static final int PR_RESIDUAL		= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_CALCLEN			= "CalcLen";
	private static final String PRN_STEPSIZE		= "StepSize";
	private static final String PRN_LPORDER			= "LPOrder";
	private static final String PRN_RESIDUAL		= "Residual";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_CALCLEN, PRN_STEPSIZE, PRN_LPORDER };
	private static final boolean	prBool[]	= { true };
	private static final String	prBoolName[]	= { PRN_RESIDUAL };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_CALCLEN				= GG_OFF_PARAMFIELD	+ PR_CALCLEN;
	private static final int GG_STEPSIZE			= GG_OFF_PARAMFIELD	+ PR_STEPSIZE;
	private static final int GG_LPORDER				= GG_OFF_PARAMFIELD	+ PR_LPORDER;
	private static final int GG_RESIDUAL			= GG_OFF_CHECKBOX	+ PR_RESIDUAL;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public TestDlg()
	{
		super( "Sample Duplication" );
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
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_CALCLEN ]	= new Param( 32.0, Param.ABS_MS );
			static_pr.para[ PR_STEPSIZE ]	= new Param(  8.0, Param.ABS_MS );
			static_pr.para[ PR_LPORDER ]	= new Param( 10.0, Param.NONE );
			static_pr.paraName	= prParaName;
			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		Component[]			ggGain;
		PathField[]			ggInputs;
		ParamField			ggCalcLen, ggStepSize, ggLPOrder;
		JCheckBox			ggResidual;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Pre$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
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

	// -------- Settings-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "LP Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggCalcLen		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Calc. Interval", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggCalcLen, GG_CALCLEN, null );

		ggLPOrder		= new ParamField( new ParamSpace( 2.0, 100000.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "LP Order", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLPOrder, GG_LPORDER, null );

		ggStepSize		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Step Size", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggStepSize, GG_STEPSIZE, null );

		ggResidual		= new JCheckBox();
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Residual", JLabel.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.4;
		gui.addCheckbox( ggResidual, GG_RESIDUAL, null );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );
	}

// -------- Processor Interface --------
		
	protected void process()
	{
		int					i, j;
		int					len, ch;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream		= null;
		AudioFileDescr			outStream		= null;
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;
		float[][]			inBuf, outBuf;
		float[]				convBuf1, convBuf2;

		// Synthesize
		float				gain			= 1.0f;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		int					inLength, inChanNum, outLength, inBufSize, outBufSize;
		int					framesRead, framesWritten;

		float				maxAmp			= 0.0f;

		PathField			ggOutput;
		
topLevel: try {
		// ---- open input, output; init ----

			// input
			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			// this helps to prevent errors from empty files!
			if( (inLength * inChanNum) < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;
		
			outLength		= inLength << 1;
			inBufSize		= 4096;
			outBufSize		= inBufSize << 1;
			inBuf			= new float[ inChanNum ][ inBufSize ];
			outBuf			= new float[ inChanNum ][ outBufSize ];

			progOff			= 0;
			progLen			= (long) inLength + (long) outLength;

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				tempFile	= new File[ inChanNum ];
				floatF		= new FloatFile[ inChanNum ];
				for( ch = 0; ch < inChanNum; ch++ ) {		// first zero them because an exception might be thrown
					tempFile[ ch ]	= null;
					floatF[ ch ]	= null;
				}
				for( ch = 0; ch < inChanNum; ch++ ) {
					tempFile[ ch ]	= IOUtil.createTempFile();
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], FloatFile.MODE_OUTPUT );
				}
				progLen	   += inLength;
			} else {
				gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// =================== CORE ===================

			framesRead		= 0;
			framesWritten	= 0;

			while( threadRunning && (framesWritten < outLength) ) {
			
				// ---- get input ----

				len	= Math.min( inBufSize, inLength - framesRead );
				inF.readFrames( inBuf, 0, len );
				progOff		+= len;
				framesRead	+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- process ----

				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = inBuf[ ch ];
					convBuf2 = outBuf[ ch ];
					for( i = 0, j = 0; i < len; ) {
						convBuf2[j++] = convBuf1[i];
						convBuf2[j++] = convBuf1[i++];
					}
				}
				progOff		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- save output ----

				len <<= 1;
				if( floatF != null ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < len; i++ ) {	// measure max amp
							f1 = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
						floatF[ ch ].writeFloats( convBuf1, 0, len );
					}
				} else {						// i.e. abs gain
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( i = 0; i < len; i++ ) {	// measure max amp + adjust gain
							f1				= Math.abs( convBuf1[ i ]);
							convBuf1[ i ]  *= gain;
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
					outF.writeFrames( outBuf, 0, len );
				}
				framesWritten	+= len;
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );				

			} // while framesWritten < outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- normalize output ----

			inF.close();
			inF		= null;

			// sound file
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, inBuf, gain, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outF.close();
			outF	= null;

			// inform about clipping/ low level
			maxAmp		*= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inStream	= null;
			outStream	= null;
			inBuf		= null;
			outBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( outF != null ) {
			outF.cleanUp();
		}
		if( inF != null ) {
			inF.cleanUp();
		}
	} // process()

// -------- private Methoden --------

	/*
	 *	Calculates linear prediction sample by sample from a dataset data[] by extrapolating the
	 *	past coeffNum values data[ dataOff - 1 ... dataOff - coeffNum ] and puts it into
	 *	future[ futOff ]; performs this operation for futLen samples
	 */
	protected static void lpFilter( float[] data, int dataOff, float[] coeffBuf, int coeffNum,
								   float[] future, int futOff, int futLen )
	{
		int		i, j, k, m;
		float	sum;	// discrp;

		for( j = futOff, m = dataOff - 1, i = futOff + futLen; j < i; m++ ) {
			sum		= 0.0f; // discrp;
			for( k = 0; k < coeffNum; k++ ) {
				sum += coeffBuf[ k ] * data[ m - k ];
			}
			future[ j++ ]	= sum; // data[ ++m ] - sum;
		}
	}

	// ---- predic routine from NR ¤13.6 ----

	/*
	 *	Given data[dataOff - coeffNum...dataOff-1] (!!), and given the data's LP coefficients d[0...m-1] (!!), this routine applies
	 *	equation (13.6.11) to predict the next futLen data points, which it returns in the array
	 *	future[futOff...futOff+futLen-1] (!!). Note that the routine references only the last coeffNum values of data, as initial
	 *	values for the prediction.
	 */
//	public static void predic(float data[], int ndata, float d[], int m, float future[], int nfut)
	protected static void linearPrediction( float[] data, int dataOff, float[] coeffBuf, int coeffNum,
											float[] future, int futOff, int futLen )
	{
		int		i, j, k;
		float	sum;	// discrp;
		float[]	reg	= new float[ coeffNum ];

		for( j = 0, k = dataOff; j < coeffNum; ) {
			reg[ j++ ] = data[ --k ];
		}

		for( j = futOff, i = futOff + futLen; j < i; j++ ) {
//			discrp	= 0.0f;
			// This is where you would put in a known discrepancy if you were reconstructing a
			// function by linear predictive coding rather than extrapolating a function by linear prediction.
			// See text.
			sum		= 0.0f; // discrp;
			for( k = 0; k < coeffNum; k++ ) {
				sum += coeffBuf[k] * reg[k];
			}
			System.arraycopy( reg, 0, reg, 1, coeffNum-1 ); // [If you want to implement circular arrays, you can avoid this shifting of coefficients.]
			reg[0]		= sum;
			future[j]	= sum;
		}
	}

	// ---- memcof routine from NR ¤13.6 ----

	/*
	 *	Given a real vector of data[dataOff...dataOff+dataLen-1] (!!), and given coeffNum, this routine returns coeffNum linear prediction
	 *	coefficients as coeffBuf[0...m-1] (!!), and returns the mean square discrepancy as xms.
	 */
//	public static void memcof( float data[], int n, int m, float *xms, float d[])
	protected static float lpCoeffs( float[] data, int dataOff, int dataLen, float[] coeffBuf, int coeffNum )
	{
		int		i, j, k;
		float	f;
		float[] wk1	= new float[ dataLen-1 ];
		float[] wk2	= new float[ dataLen-1 ];
		float[]	wkm	= new float[ coeffNum-1 ];
		float	xms, num, denom;

		for( j = dataLen, i = dataLen + dataOff, f = 0.0f; j < i; j++ ) {
			f += data[j]*data[j];
		}
		xms = f / dataLen;
	
		wk1[0]			= data[0];
		wk2[dataLen-2]	= data[dataLen-1];

		System.arraycopy( data, dataOff,   wk1, 0, dataLen-1 );
		System.arraycopy( data, dataOff+1, wk2, 0, dataLen-1 );

		for( k = 0;; ) {
			num		= 0.0f;
			denom	= 0.0f;
			
			for( j = 0, i = dataLen - k - 1; j < i; j++ ) {
				num		+= wk1[j]*wk2[j];
				denom	+= wk1[j]*wk1[j] + wk2[j]*wk2[j];
			}
			if( denom > 0.0f ) {
				f			= 2.0f * num / denom;
			} else {
				f			= 1.0f;
			}
			coeffBuf[k]		= f;
			xms	  		   *= 1.0f - f*f;
			for( i = 0; i < k; i++ ) {
				coeffBuf[i]	= wkm[i] - f * wkm[k-i-1];
			}
			// The algorithm is recursive, building up the answer for larger and larger values of m
			// until the desired value is reached. At this point in the algorithm, one could return
			// the vector d and scalar xms for a set of LP coefficients with k (rather than m) terms.
			if( ++k == coeffNum ) return xms;		// !! k increased here for efficiency
 
			System.arraycopy( coeffBuf, 0, wkm, 0, k );

			for( j = 0, i = dataLen - k - 1; j < i; j++ ) {
				wk1[j]	-= f * wk2[j];
				wk2[j]	 = wk2[j+1] - f * wk1[j+1];
			}
		}	
	}

	// ---- fixrts routine from NR ¤13.6 ----

	/*
	 *	Given the LP coefficients d[0...m-1] (!!), this routine finds all roots of the characteristic polynomial
	 *	(13.6.14), reflects any roots that are outside the unit circle back inside, and then returns a
	 *	modified set of coefficients d[0...m-1].
	 */
	protected static void fixRoots( float coeffBuf[], int coeffNum )
	{
		int		i, j, coeffNum2;
		float	rootsRe, rootsIm, f1;
		
		coeffNum2 = coeffNum << 1;
		
		float[]	a		= new float[ coeffNum2 + 2 ];
		float[] roots	= new float[ coeffNum2 ];
		float[] cmplxRes= new float[ 2 ];
		
		a[ coeffNum2 ]		= 1.0f;
		a[ coeffNum2+1 ]	= 0.0f;
		for( i = 0, j = coeffNum2; j > 0; ) { // Set up complex coefficients for polynomial root finder.
			a[ --j ]	= 0.0f;
			a[ --j ]	= -coeffBuf[ i++ ];
		}
		
		zRoots( a, coeffNum, roots, true );	// Find all the roots.

		for( j = 0; j < coeffNum2; j += 2 ) {	// Look for a...
			f1 = complexAbs( roots[j], roots[j+1] );
			if( f1 > 1.0f ) {	// 1.0f	// root outside the unit circle,
				// pull back on unit circle
//				roots[j]   *= 0.0f / f1;
//				roots[j+1] *= 0.0f / f1;

				// alternative: reflection
				complexDiv( 1.0f, 0.0f, roots[ j ], -roots[ j+1 ], cmplxRes );
				roots[ j ]		= cmplxRes[0];
				roots[ j+1 ]	= cmplxRes[1];
			}
		}
		
		a[0]	= -roots[0];
		a[1]	= -roots[1];
		a[2]	= 1.0f;
		a[3]	= 0.0f;		// Now reconstruct the polynomial coefficients,
		for( j = 2; j < coeffNum2; j += 2 ) {	// by looping over the roots
			a[ j+2 ]	= 1.0f;
			a[ j+3 ]	= 0.0f;
			rootsRe		= roots[j];
			rootsIm		= roots[j+1];
			for( i = j; i >= 2; i -= 2 ) {	// and synthetically multiplying.
				a[i]	= a[i-2] - (rootsRe * a[i] - rootsIm * a[i+1]);
				a[i+1]	= a[i-1] - (rootsIm * a[i] + rootsRe * a[i+1]);
			}
			a[0]	= -rootsRe * a[0] + rootsIm * a[1];
			a[1]	= -rootsIm * a[0] - rootsRe * a[1];
		}

		for( i = coeffNum, j = 0; j < coeffNum2; j += 2 ) {	// The polynomial coefficients are guaranteed to be real, so we need only return the real part as new LP coefficients.
			coeffBuf[ --i ] = -a[j];
		}
	}

	// ---- zroots routine from NR ¤9.5 ----

	protected static final float	EXPECTEDERROR2	= 4.0e-6f;

	/*
	 * Given the degree m and the m+1 complex coefficients a[0...m*2] of the polynomial ·i=0...m(a[i]x^i),
	 * this routine successively calls laguer and finds all m complex roots in roots[0...(m-1)*2] (!!). The
	 * boolean variable polish should be input as true if polishing (also by Laguerre's method)
	 * is desired, false if the roots will be subsequently polished by other means.
	 */
	protected static void zRoots( float[] coeffBuf, int coeffNum, float[] roots, boolean polish )
	{
		int		i, j, jj;
		float	bRe,bIm, cRe,cIm;
		int		coeffNum2	= coeffNum << 1;
		float[]	ad	= new float[ coeffNum2 + 2 ];
		float[]	x	= new float[ 2 ];
		
		System.arraycopy( coeffBuf, 0, ad, 0, coeffNum2 + 2 );	// Copy of coefficients for successive deflation.
	
		for( j = coeffNum2; j >= 2; j -= 2 ) {		// Loop over each root to be found.
			jj = j - 2;
			// Start at zero to favor convergence to smallest remaining root, and find the root.
			x[0]	= 0.0f;
			x[1]	= 0.0f;
			i		= laguerre( ad, j >> 1, x );

//			if( i == 0 ) {
//System.out.println( "retrying complex" );
//				x[0]	= 0.0f;
//				x[1]	= 0.5f;		// try complex
//				laguerre( ad, j >> 1, x );
//			}
			if( Math.abs( x[1] ) <= EXPECTEDERROR2 * Math.abs( x[0] )) {
				x[1] = 0.0f;
			}
			roots[ jj ]		= x[0];
			roots[ jj+1 ]	= x[1];

			bRe				= ad[ j ];
			bIm				= ad[ j + 1 ];	// Forward deflation.
			for( ; jj >= 0; jj -= 2 ) {
				cRe			= ad[ jj ];
				cIm			= ad[ jj+1 ];
				ad[ jj ]	= bRe;
				ad[ jj+1 ]	= bIm;
				bRe			= x[0] * bRe - x[1] * bIm + cRe;
				bIm			= x[1] * bRe + x[0] * bIm + cIm;
			}
		}

		if( polish ) {
			for( j = 0; j < coeffNum2; ) {	// Polish the roots using the undeflated coefficients.
				x[0]		= roots[ j ];
				x[1]		= roots[ j+1 ];
				laguerre( coeffBuf, coeffNum, x );
				roots[j++]	= x[0];
				roots[j++]	= x[1];
			}
		}

		for( j = 2; j < coeffNum2; j += 2 ) {		// Sort roots by their real parts by straight insertion.
			x[0]	= roots[ j ];
			x[1]	= roots[ j+1 ];
			for( i = j - 2; i >= 2; i -= 2 ) {
				if( roots[ i ] <= x[0] ) break;
				roots[ i+2 ]	= roots[ i ];
				roots[ i+3 ]	= roots[ i+1 ];
			}
			roots[ i+2 ]	= x[0];
			roots[ i+3 ]	= x[1];
		}

//System.out.println( "roots:" );
//for( j = 0; j < coeffNum; j++ ) {
//	System.out.println( roots[j<<1]+" + "+roots[(j<<1)+1]+"i" );
//}
	}

	// ---- laguer routine from NR ¤9.5 ----

	protected static final float	EXPECTEDERROR 	= 1.0e-7f;
	protected static final int		MR 				= 8;
	protected static final int		MT				= 10; // 10;
	protected static final int		MAXITER			= (MT*MR);
	protected static final float[]	frac			= { 0.0f, 0.5f, 0.25f, 0.75f, 0.13f, 0.38f, 0.62f, 0.88f, 1.0f }; // Fractions used to break a limit cycle. [MR+1]

	/*
	 * Given the degree m and the m+1 complex coefficients a[0..m] of the polynomial ·i=0...m(a[i]x^i),
	 * and given a complex value x, this routine improves x by Laguerre's method until it converges,
	 * within the achievable roundoff limit, to a root of the given polynomial. The number of iterations
	 * taken is returned as its.
	 *
	 * Here EXPECTEDERROR is the estimated fractional roundoff error. We try to break (rare) limit cycles with
	 * MR different fractional values, once every MT steps, for MAXITER total allowed iterations.
	 *
	 *	WARNING: dispite complex 'x' sucky algorithm doesn't work for complex roots!!!!
	 *
	 *	@return		number of iterations performed or 0 if not converged (complex root)
	 */
//	protected int laguer( fcomplex a[], int m, fcomplex *x )
	protected static int laguerre( float[] coeffBuf, int coeffNum, float[] x )
	{
		int		iter, j, coeffNum2, fooInt;
		float	absX, absP, absM, err, absB;
		float	bRe,bIm, fRe,fIm, dRe,dIm, g2Re,g2Im, gRe,gIm, gmRe, gmIm;
		float	gpRe,gpIm, sqRe,sqIm, hRe,hIm, x1Re,x1Im, dxRe,dxIm, fooFloat;
		float[] cmplxRes = new float[ 2 ];	// return values from complexDiv and complexSqrt
		
		coeffNum2	= coeffNum << 1;
		
		for( iter = 1; iter <= MAXITER; iter++ ) {	// Loop over iterations up to allowed maximum.
		
			bRe		= coeffBuf[ coeffNum2 ];
			bIm		= coeffBuf[ coeffNum2 + 1 ];
			absB	= complexAbs( bRe, bIm );
			err		= absB;
			fRe		= 0.0f;
			fIm		= 0.0f;
			dRe		= 0.0f;
			dIm		= 0.0f;
			absX	= complexAbs( x[0], x[1] );

			for( j = coeffNum2; j > 0; ) {			// Effcient computation of the polynomial and its first two derivatives. f=Cadd(Cmul(*x,f),d);
			
				dRe		= x[0] * dRe - x[1] * dIm + bRe;
				dIm		= x[1] * dRe + x[0] * dIm + bIm;
				bIm		= x[1] * bRe + x[0] * bIm + coeffBuf[ --j ];
				bRe		= x[0] * bRe - x[1] * bIm + coeffBuf[ --j ];
				absB	= complexAbs( bRe, bIm );
				err		= absB + absX * err;
			}

			err	   *= EXPECTEDERROR;				// Estimate of roundoff error in evaluating polynomial.
			if( absB <= err ) return iter;			// We are on the root.

			complexDiv( dRe, dIm, bRe, bIm, cmplxRes );	// The generic case: use Laguerre's formula.
			gRe		= cmplxRes[0];
			gIm		= cmplxRes[1];
			g2Re	= gRe * gRe - gIm * gIm;
			g2Im	= gIm * gRe * 2;
			complexDiv( fRe, fIm, bRe, bIm, cmplxRes );
			hRe		= g2Re - 2.0f * cmplxRes[0];
			hIm		= g2Im - 2.0f * cmplxRes[1];

			sqRe	= (coeffNum - 1) * (coeffNum * hRe - g2Re);
			sqIm	= (coeffNum - 1) * (coeffNum * hIm - g2Im);
			complexSqrt( sqRe, sqIm, cmplxRes );
			sqRe	= cmplxRes[0];
			sqIm	= cmplxRes[1];
			gpRe	= gRe + sqRe;
			gpIm	= gIm + sqIm;
			gmRe	= gRe - sqRe;
			gmIm	= gIm - sqIm;
			absP	= complexAbs( gpRe, gpIm );
			absM	= complexAbs( gmRe, gmIm );

			if( absP < absM ) {
				gpRe = gmRe;
				gpIm = gmIm;
			}
			if( (absP > 0.0f) || (absM > 0.0f) ) {
				complexDiv( coeffNum, 0.0f, gpRe, gpIm, cmplxRes );
				dxRe	= cmplxRes[0];
				dxIm	= cmplxRes[1];
			} else {
				fooFloat= (1.0f + absX);
				dxRe	= fooFloat * (float) Math.cos( iter );
				dxIm	= fooFloat * (float) Math.sin( iter );
			}
			x1Re	= x[0] - dxRe;
			x1Im	= x[1] - dxIm;

			if( (dxRe == 0.0f) && (dxIm == 0.0f) ) return iter; // Converged.
			if( (iter % MT) != 0 ) {
				x[0]	= x1Re;
				x[1]	= x1Im;
			} else {
				fooInt  = iter / MT;
				x[0]   -= frac[ fooInt ] * dxRe;
				x[1]   -= frac[ fooInt ] * dxIm;	// Every so often we take a fractional step, to break any limit cycle (itself a rare occurrence).
			}
		}
//		System.out.println( "too many iterations in laguerre()" );	// Very unusual | can occur only for complex roots. Try a different starting guess for the root.
// XXX
			
		return 0;
	}

	// from NR Appendix C
	protected static float complexAbs( float re, float im )
	{
		if( re == 0.0f ) return Math.abs( im );
		if( im == 0.0f ) return Math.abs( re );
		return (float) Math.sqrt( re*re + im*im );
	}
	
	// from NR Appendix C
	protected static void complexDiv( float aRe, float aIm, float bRe, float bIm, float[] result )
	{
		float r, den;
		
		if( Math.abs( bRe ) >= Math.abs( bIm )) {
			r			= bIm / bRe;
			den 		= bRe + r * bIm;
			result[0]	= (aRe + r * aIm) / den;
			result[1]	= (aIm - r * aRe) / den;
		} else {
			r			= bRe / bIm;
			den			= bIm + r * bRe;
			result[0]	= (aRe * r + aIm) / den;
			result[1]	= (aIm * r - aRe) / den;
		}
	}

	// from NR Appendix C
	protected static void complexSqrt( float Re, float Im, float[] result )
	{
		float absRe, absIm, w, r;
		
		if( (Re == 0.0f) && (Im == 0.0f)) {
			result[0] = 0.0f;
			result[1] = 0.0f;
		} else {
			absRe = Math.abs( Re );
			absIm = Math.abs( Im );
			if( absRe >= absIm ) {
				r = absIm / absRe;
				w = (float) (Math.sqrt( absRe ) * Math.sqrt( 0.5f * (1.0f + Math.sqrt( 1.0f + r*r ))));
			} else {
				r = absRe / absIm;
				w = (float) (Math.sqrt( absIm ) * Math.sqrt( 0.5f * (r + Math.sqrt( 1.0f + r*r ))));
			}
			if( Re >= 0.0 ) {
				result[0] = w;
				result[1] = Im / (2.0f * w);
			} else {
				result[1] = (Im >= 0.0f) ? w : -w;
				result[0] = Im / (2.0f * result[1] );
			}
		}
	}
}
// class TestDlg
