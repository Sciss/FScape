/*
 *  BleachDlg.java
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
 *		18-Feb-10	created
 */

package de.sciss.fscape.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.util.Debug;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Adaptive whitening filter.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 18-Feb-10
 */
public class BleachDlg
extends DocumentFrame
{
// -------- private variables --------

	// properties
	private static final int PR_ANAINFILE			= 0;		// pr.text
	private static final int PR_FLTINFILE			= 1;
	private static final int PR_OUTPUTFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_GAINTYPE			= 2;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_FILTERLENGTH		= 1;
	private static final int PR_FEEDBACKGAIN		= 2;
//	private static final int PR_ITERATIONS			= 3;
	private static final int PR_USEANAASFLT			= 0;		// pr.bool
//	private static final int PR_PRECALC				= 1;

	private static final String PRN_ANAINFILE		= "AnaInFile";
	private static final String PRN_FLTINFILE		= "FltInFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_FILTERLENGTH	= "FilterLength";
//	private static final String PRN_ITERATIONS		= "Iterations";
	private static final String PRN_FEEDBACKGAIN	= "FeedbackGain";
	private static final String PRN_USEANAASFLT		= "UseAnaAsFilter";
//	private static final String PRN_PRECALC			= "PreCalc";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_ANAINFILE, PRN_FLTINFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
	private static final Param	prPara[]		= { null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_FILTERLENGTH, PRN_FEEDBACKGAIN };
	private static final boolean prBool[]		= { true /*, true*/ };
	private static final String	prBoolName[]	= { PRN_USEANAASFLT /*, PRN_PRECALC*/ };

	private static final int GG_ANAINFILE		= GG_OFF_PATHFIELD	+ PR_ANAINFILE;
	private static final int GG_FLTINFILE		= GG_OFF_PATHFIELD	+ PR_FLTINFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_FILTERLENGTH	= GG_OFF_PARAMFIELD	+ PR_FILTERLENGTH;
//	private static final int GG_ITERATIONS		= GG_OFF_PARAMFIELD	+ PR_ITERATIONS;
	private static final int GG_FEEDBACKGAIN	= GG_OFF_PARAMFIELD	+ PR_FEEDBACKGAIN;
	private static final int GG_USEANAASFLT		= GG_OFF_CHECKBOX	+ PR_USEANAASFLT;
//	private static final int GG_PRECALC			= GG_OFF_CHECKBOX	+ PR_PRECALC;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private static final String	ERR_CHANNELS		= "Inputs must share\nsame # of channels!";

// -------- public methods --------

	public BleachDlg()
	{
		super( "Bleach" );
		init2();
	}
	
	protected void buildGUI()
	{
		// initialize PropertyArray once
		if( static_pr == null ) {
			static_pr			= new PropertyArray();
			static_pr.text		= prText;
			static_pr.textName	= prTextName;
			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_FILTERLENGTH ]	= new Param(  441.0, Param.NONE );
//			static_pr.para[ PR_ITERATIONS ]		= new Param(  300.0, Param.NONE );
			static_pr.para[ PR_FEEDBACKGAIN ]	= new Param(  -60.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- build GUI --------

		final GridBagConstraints con;

		final PathField		ggAnaInFile, ggOutputFile, ggFltInFile;
		final PathField[]	ggInputs;
		final JCheckBox		ggUseAnaAsFlt;
//		final JCheckBox		ggPreCalc;
		final Component[]	ggGain;
		final ParamField	ggFilterLength, ggFeedbackGain;
		final ParamSpace	spcFeedbackGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		final ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				final int id = gui.getItemID( e );

				switch( id ) {
				case GG_USEANAASFLT:
					pr.bool[ id - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
				
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggAnaInFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select analysis input file" );
		ggAnaInFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Analysis Input:", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggAnaInFile, GG_ANAINFILE, null );

		ggFltInFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select filter input file" );
		ggFltInFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filter Input:", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		ggUseAnaAsFlt	= new JCheckBox( "Use Analysis File" );
		gui.addCheckbox( ggUseAnaAsFlt, GG_USEANAASFLT, il );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel() );
		con.weightx		= 0.9;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addPathField( ggFltInFile, GG_FLTINFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggAnaInFile;
//		ggInputs[ 1 ]	= ggFltInFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0White$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
//		con.gridx		= 0;
		gui.addLabel( new JLabel( "Whitened Output:", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

	// -------- Plot Settings --------
	gui.addLabel( new GroupLabel( "Filter Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggFilterLength	= new ParamField( new ParamSpace[] {
			new ParamSpace( 2, 1000000, 1, Param.NONE )});
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Filter Length:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggFilterLength, GG_FILTERLENGTH, null );

//		ggIterations	= new ParamField( new ParamSpace[] {
//			new ParamSpace( 1, 1000000, 1, Param.NONE )});
//		con.weightx		= 0.1;
////		con.gridwidth	= 1;
//		gui.addLabel( new JLabel( "Iterations:", SwingConstants.RIGHT ));
//		con.weightx		= 0.4;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addParamField( ggIterations, GG_ITERATIONS, null );

		spcFeedbackGain	= new ParamSpace( Double.NEGATIVE_INFINITY, 0.0, 0.1, Param.DECIBEL_AMP );
		ggFeedbackGain	= new ParamField( spcFeedbackGain );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Feedback Gain:", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggFeedbackGain, GG_FEEDBACKGAIN, null );

//		con.gridwidth	= 1;
//		con.weightx		= 0.1;
//		gui.addLabel( new JLabel( "Pre-Calc Initial Filter:", SwingConstants.RIGHT ));
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		con.weightx		= 0.9;
//		ggPreCalc		= new JCheckBox();
//		gui.addCheckbox( ggPreCalc, GG_PRECALC, null );
		
		reflectPropertyChanges();
		
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
		AudioFile				anaInF			= null;
		AudioFile				outF			= null;
		AudioFile				fltInF			= null;
		final AudioFile			tmpF;
		final AudioFileDescr	anaInDescr, outDescr;
		final int				numCh;
		final long				anaInLength, numFrames;
		final boolean			useAnaAsFilter	= pr.bool[ PR_USEANAASFLT ];
		final PathField			ggOutput;
		final int				fltLength		= (int) pr.para[ PR_FILTERLENGTH ].val;
//		final int				iterations		= (int) pr.para[ PR_ITERATIONS ].val;
		final Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		final double			feedbackGain	= (Param.transform( pr.para[ PR_FEEDBACKGAIN ], Param.ABS_AMP, ampRef, null )).val;
		final double[][]		fltKernel;
		final float[][]			anaBuf, outBuf, fltBuf;
		final boolean			absGain			= pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE;
//		final boolean			preCalc			= pr.bool[ PR_PRECALC ];
		final boolean			preCalc			= false; // XXX man this shit doesn't do anything. but why???
		
		long					framesRead, framesWritten, progOff, progLen;
		int						chunkLen;
		float[]					anaChanBuf, outChanBuf, fltChanBuf;
		double[]				fltChanKernel;
		double					d1, errNeg;
		float					gain;
		float					maxAmp			= 0.0f;
		
topLevel: try {

		// ---- open input, output; init ----
			// analysis input
			anaInF			= AudioFile.openAsRead( new File( pr.text[ PR_ANAINFILE ]));
			anaInDescr		= anaInF.getDescr();
			numCh			= anaInDescr.channels;
			anaInLength		= anaInDescr.length;
			// this helps to prevent errors from empty files!
			if( (anaInLength < 1) || (numCh < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// filter input
			if( !useAnaAsFilter ) {
				fltInF	= AudioFile.openAsRead( new File( pr.text[ PR_FLTINFILE ]));
				final AudioFileDescr fltInDescr = fltInF.getDescr();
			// .... check running ....
				if( !threadRunning ) break topLevel;

				if( numCh != fltInDescr.channels ) {
					throw new IOException( ERR_CHANNELS );
				}
				numFrames	= Math.min( anaInLength, fltInDescr.length );
			} else {
				numFrames	= anaInLength;
			}

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outDescr	= new AudioFileDescr( anaInDescr );
			ggOutput.fillStream( outDescr );
			outF		= AudioFile.openAsWrite( outDescr );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ---- further inits ----

			if( absGain ) {
				gain	= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
				tmpF	= null;
			} else {
				gain	= 1f;
				tmpF	= createTempFile( outDescr );
			}
			
			fltKernel	= new double[ numCh ][ fltLength ];
			// FFFFFBBBBBBBBB
			anaBuf		= new float[ numCh ][ fltLength + 8192 ];
			outBuf		= new float[ numCh ][ 8192 ];
			if( useAnaAsFilter ) {
				fltBuf	= anaBuf;
			} else {
				fltBuf	= new float[ numCh ][ fltLength + 8192 ];
			}

			// ---- main loop ----
			progLen			= numFrames * 3 + (absGain ? 0L : numFrames);
			progOff			= 0L;
			
//			R = exp( -SampleDur/t )
//			t60 = 6.91t
//			log R = -SampleDur * 6.91 / t60
//			t60 = -SampleDur * 6.91 / log R
//			t60 = -(1/SampleRate) * 6.91 / log R
//			t60 = -(1/SampleRate) * 6.91 / log R
//			t60 = -6.91 / (log R * SampleRate)
//			frames = -6.91 / log R
//			R == (1-r) ???
//			r = -50.dbamp
//			-6.91 / log( (1-r) )
//			r = -80.dbamp
//			-6.91 / log( (1-r) )
			if( preCalc ) { // precalc going backwards to the beginning
//				final long preFrames = Math.max( 0, Math.min( numFrames,
//				    (long) (-6.91 / Math.log( 1 - feedbackGain ))));
				final long preFrames = numFrames;
				progLen += preFrames * 2;
				framesRead = 0L;
//System.out.println( "preFrames = " + preFrames );
				while( threadRunning && (framesRead < preFrames) ) {
					// read input
					chunkLen = (int) Math.min( 8192, preFrames - framesRead );
					anaInF.seekFrame( preFrames - framesRead - chunkLen );
					anaInF.readFrames( anaBuf, fltLength, chunkLen );
					framesRead += chunkLen;
					progOff    += chunkLen;

				if( framesRead == chunkLen ) {
					final float[] tmp = new float[ anaBuf[ 0 ].length ];
					for( int i = 0; i < tmp.length; i++ ) {
						tmp[ i ] = anaBuf[ 0 ][ i ];
					}
					Debug.view( tmp, 0, tmp.length, "Fwd" );
				}
					
					Util.reverse( anaBuf, fltLength, chunkLen );

				if( framesRead == chunkLen ) {
					final float[] tmp = new float[ anaBuf[ 0 ].length ];
					for( int i = 0; i < tmp.length; i++ ) {
						tmp[ i ] = anaBuf[ 0 ][ i ];
					}
					Debug.view( tmp, 0, tmp.length, "Bwd" );
				}
				
					// process
					for( int ch = 0; ch < numCh; ch++ ) {
						anaChanBuf		= anaBuf[ ch ];
						fltChanKernel	= fltKernel[ ch ];
						for( int i = 0, k = 0; i < chunkLen; i++ ) {
							// calc error
							d1 = 0.0;
							k  = i;
							for( int j = 0; j < fltLength; j++, k++ ) {
								d1 += anaChanBuf[ k ] * fltChanKernel[ j ]; 
							}
							errNeg = anaChanBuf[ k ] - d1;
							// update kernel
							d1 = errNeg * feedbackGain;
							k = i;
							for( int j = 0; j < fltLength; j++, k++ ) {
								fltChanKernel[ j ] += d1 * anaChanBuf[ k ];
							}
						}
					}
					progOff += chunkLen;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
				Util.reverse( fltKernel, 0, fltLength );
				anaInF.seekFrame( 0L );
				
				final float[] tmp = new float[ fltLength ];
				for( int i = 0; i < fltLength; i++ ) {
					tmp[ i ] = (float) fltKernel[ 0 ][ i ];
				}
				Debug.view( tmp, 0, fltLength, "Kernel" );
			}
			
			framesWritten	= 0L;
			framesRead		= 0L;
			while( threadRunning && (framesWritten < numFrames) ) {
				// read input
				chunkLen = (int) Math.min( 8192, numFrames - framesRead );
				anaInF.readFrames( anaBuf, fltLength, chunkLen );
				if( !useAnaAsFilter ) {
					fltInF.readFrames( fltBuf, fltLength, chunkLen );
				}
				framesRead += chunkLen;
				progOff    += chunkLen;
				
				// process
				for( int ch = 0; ch < numCh; ch++ ) {
					anaChanBuf		= anaBuf[ ch ];
					fltChanKernel	= fltKernel[ ch ];
					outChanBuf		= outBuf[ ch ];
					fltChanBuf		= fltBuf[ ch ];
					for( int i = 0, k = 0; i < chunkLen; i++ ) {
						// calc error
						d1 = 0.0;
						k  = i;
						for( int j = 0; j < fltLength; j++, k++ ) {
							d1 += anaChanBuf[ k ] * fltChanKernel[ j ]; 
						}
//						err = d1 - anaChanBuf[ k ];
						errNeg = anaChanBuf[ k ] - d1;
						if( useAnaAsFilter ) {
							// use straight as output...
							outChanBuf[ i ] = (float) errNeg;
						} else {
							// ...or calc output according to filter buffer
							k  = i;
							for( int j = 0; j < fltLength; j++, k++ ) {
								d1 += fltChanBuf[ k ] * fltChanKernel[ j ]; 
							}
							outChanBuf[ i ] = (float) (fltChanBuf[ k ] - d1);
						}
						
						// update kernel
						d1 = errNeg * feedbackGain;
						k = i;
						for( int j = 0; j < fltLength; j++, k++ ) {
							fltChanKernel[ j ] += d1 * anaChanBuf[ k ];
						}
					}
				}
				progOff += chunkLen;

				// handle overlap
//				Util.copy( anaBuf, fltLength, anaBuf, 0, chunkLen );
				Util.copy( anaBuf, 8192, anaBuf, 0, fltLength );
				if( !useAnaAsFilter ) {
//					Util.copy( fltBuf, fltLength, fltBuf, 0, chunkLen );
					Util.copy( fltBuf, 8192, fltBuf, 0, fltLength );
				}
				
				// write output
				maxAmp = Math.max( maxAmp, Util.maxAbs( outBuf, 0, chunkLen ));
				if( absGain ) {
					Util.mult( outBuf, 0, chunkLen, gain );
					outF.writeFrames( outBuf, 0, chunkLen );
				} else {
					tmpF.writeFrames( outBuf, 0, chunkLen );
				}
				framesWritten += chunkLen;
				progOff       += chunkLen;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			// ---- finish ----
			
			// adjust gain
			if( !absGain ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( tmpF, outF, anaBuf, gain, 1.0f );
				deleteTempFile( tmpF );
			}

			outF.close();
			outF		= null;

			// inform about clipping/ low level
			maxAmp		*= gain;
			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( anaInF != null ) anaInF.cleanUp();
		if( fltInF != null ) fltInF.cleanUp();
		if( outF != null ) outF.cleanUp();
	} // process()

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		final PathField ggFltInFile	= (PathField) gui.getItemObj( GG_FLTINFILE );
		if( ggFltInFile != null ) {
			ggFltInFile.setEnabled( !pr.bool[ PR_USEANAASFLT ]);
		}
	}
}
