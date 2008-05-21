/*
 *  NeedleholeDlg.java
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
 *		06-Feb-05	added standard deviation + minimum filter + improved speed
 *		17-Mar-05	added center clipping
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
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
 *	Processing module for moving window
 *	based filtering of a sound.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.67, 17-Mar-05
 */
public class NeedleholeDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_FILTER			= 3;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_LENGTH			= 1;
	private static final int PR_THRESH			= 2;
	private static final int PR_SUBDRY			= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_FILTER			= "Filter";
	private static final String PRN_LENGTH			= "Length";
	private static final String PRN_THRESH			= "Thresh";
	private static final String PRN_SUBDRY			= "SubDry";

	private static final int FILTER_MEDIAN		= 0;
	private static final int FILTER_STDDEV		= 1;
	private static final int FILTER_MINIMUM		= 2;
	private static final int FILTER_CENTER		= 3;

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]		= { 0, 0, GAIN_UNITY, FILTER_MEDIAN };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_FILTER };
	private static final Param	prPara[]		= { new Param(   0.0, Param.DECIBEL_AMP ),
													new Param(  50.0, Param.ABS_MS ),
													new Param( -18.0, Param.DECIBEL_AMP )};
	private static final String	prParaName[]	= { PRN_GAIN, PRN_LENGTH, PRN_THRESH };
	private static final boolean prBool[]		= { false };
	private static final String	prBoolName[]	= { PRN_SUBDRY };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_FILTER			= GG_OFF_CHOICE		+ PR_FILTER;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_LENGTH			= GG_OFF_PARAMFIELD	+ PR_LENGTH;
	private static final int GG_THRESH			= GG_OFF_PARAMFIELD	+ PR_THRESH;
	private static final int GG_SUBDRY			= GG_OFF_CHECKBOX	+ PR_SUBDRY;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public NeedleholeDlg()
	{
		super( "Needlehole Cherry Blossom" );
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
			static_pr.paraName	= prParaName;
			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		JComboBox			ggFilter;
		JCheckBox			ggSubDry;
		ParamField			ggLength, ggThresh;
		PathField[]			ggInputs;
		Component[]			ggGain;
		ParamSpace[]		spcThresh;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_FILTER:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
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
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Bloss$E" );
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Peer-to-Peer Needle Sharing", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		
		ggLength		= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Window length", JLabel.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLength, GG_LENGTH, null );

		ggFilter		= new JComboBox();
		ggFilter.addItem( "Median" );
		ggFilter.addItem( "Standard Deviation" );
		ggFilter.addItem( "Minimum" );
		ggFilter.addItem( "Center Clipping" );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Filter", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggFilter, GG_FILTER, il );

		spcThresh		= new ParamSpace[] { Constants.spaces[ Constants.ratioAmpSpace ],
											 Constants.spaces[ Constants.decibelAmpSpace ]};
		ggThresh		= new ParamField( spcThresh );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Clip thresh", JLabel.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggThresh, GG_THRESH, null );

		ggSubDry		= new JCheckBox( "Subtract dry signal" );
		con.weightx		= 0.4;
//		con.gridwidth	= 1;
		gui.addCheckbox( ggSubDry, GG_SUBDRY, il );

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
		int					i, j, ch, len;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile[]			floatF			= null;
		File[]				tempFile		= null;
		int					inChanNum;
		float[][]			inBuf;
		float[][]			outBuf;
		float[]				convBuf1, convBuf2;
		RunningWindowFilter filter;
//		boolean				finished;

		// Synthesize
		float				gain			= 1.0f;								// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		int					inLength;
		int					framesRead, framesWritten;
		int					inBufSize, winSize, winSizeH, offStart, outBufSize, transLen;

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
			if( inLength * inChanNum < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			progOff				= 0;
			progLen				= (long) inLength*3;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ---- filter init ----
			winSize 			= Math.max( 1, (int) (AudioFileDescr.millisToSamples( inStream,
														pr.para[ PR_LENGTH ].val ) + 0.5) );
			winSizeH			= winSize >> 1;
			outBufSize			= Math.max( 8192, winSize );
			inBufSize			= outBufSize + winSize;	// winSize << 1

			inBuf				= new float[ inChanNum ][ inBufSize ];
			outBuf				= new float[ inChanNum ][ outBufSize ];

			switch( pr.intg[ PR_FILTER ]) {
			case FILTER_MEDIAN:
				filter  = new MedianFilter( winSize, inChanNum );
				break;
			case FILTER_STDDEV:
				filter  = new StdDevFilter( winSize, inChanNum );
				break;
			case FILTER_MINIMUM:
				filter  = new MinimumFilter( winSize, inChanNum );
				break;
			case FILTER_CENTER:
				filter  = new CenterClippingFilter( winSize, inChanNum );
				break;
			default:
				assert false : pr.intg[ PR_FILTER ];
				filter	= null;
				break;
			}

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
			}
			if( pr.intg[ PR_GAINTYPE ] == GAIN_ABSOLUTE ) {
				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== kulchur ====================----

			framesWritten	= 0;
			framesRead		= 0;
			
//			offStart		= winSizeH;
			offStart		= 0;
//			finished		= false;

			while( threadRunning && framesWritten < inLength ) {
				// ==================== read input chunk ====================
				len			= Math.min( inLength - framesRead, inBufSize - offStart );
				inF.readFrames( inBuf, offStart, len );
				framesRead += len;
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero-padding last chunk
				if( offStart + len < inBufSize ) {
//System.out.println( "zero "+(inBufSize-chunkLength)+" frames" );
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = offStart + len; i < inBufSize; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
				}
				transLen = Math.min( inBufSize - winSize,  inLength - framesWritten );

				// ==================== needle sharing ====================

				filter.process( inBuf, outBuf, 0, 0, transLen );
				if( pr.bool[ PR_SUBDRY ]) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						convBuf2 = inBuf[ ch ];
						for( i = winSizeH, j = 0; j < transLen; i++, j++ ) {
							convBuf1[j] -= convBuf2[i];
						}
					}
				}
			// .... progress ....
				progOff += transLen;
				setProgression( (float) progOff / (float) progLen );		

				// ==================== write output ====================
				if( floatF != null ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						floatF[ ch ].writeFloats( convBuf1, 0, transLen );
						for( i = 0; i < transLen; i++ ) {
							f1 = Math.abs( convBuf1[i] );
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
				} else {
					// adjust gain
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = outBuf[ ch ];
						for( j = 0; j < transLen; j++ ) {
							convBuf1[ j ] *= gain;
							f1 = Math.abs( convBuf1[j] );
							if( f1 > maxAmp ) {
								maxAmp = f1;
							}
						}
					}
					outF.writeFrames( outBuf, 0, transLen );
				}
				framesWritten += transLen;
						
			// ---- resample : dun ----------------------------------------------------------------------
					
				// shift buffers
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch ], inBufSize - winSize, inBuf[ ch ], 0, winSize );
				}
				offStart = winSize;

			// .... progress ....
				progOff += transLen;
				setProgression( (float) progOff / (float) progLen );		
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----
	
			inF.close();
			inF			= null;
			inStream	= null;

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
								new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
												
				normalizeAudioFile( floatF, outF, outBuf, gain, 1.0f );
				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
				maxAmp *= gain;
			}

			outF.close();
			outF = null;

		// ---- Finish ----

			// inform about clipping/ low level
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
		if( inF != null ) {
			inF.cleanUp();
		}
		if( outF != null ) {
			outF.cleanUp();
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) floatF[ ch ].cleanUp();
				if( tempFile[ ch ] != null ) tempFile[ ch ].delete();
			}
		}
	} // process()

// -------- private Methoden --------
	
	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_THRESH );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_FILTER ] == FILTER_CENTER );
		}
	}

// -------- Window Filter --------

	abstract class RunningWindowFilter
	{
		public abstract void process( float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len ) throws IOException;
	}
	
	private class StdDevFilter
	extends RunningWindowFilter
	{
		final int			winSize;
		final int			channels;
		final double[][]	dcMem;
		final int			winSizeM1;
	
		public StdDevFilter( int winSize, int channels )
		{
			this.winSize	= winSize;
			this.channels   = channels;
			winSizeM1		= winSize - 1;
			
			dcMem		= new double[channels][2];
		}
	
		public void process( float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len )
		throws IOException
		{
			int			ch, i, j, k, m, n;
			float[]		convBuf2, convBuf3;
			double[]	convBuf4;
			double		d1, d2, mu, mus, omus, sum;
			
			for( ch = 0; ch < channels; ch++ ) {
				convBuf4	= dcMem[ ch ];
				convBuf2	= inBuf[ch];
				convBuf3	= outBuf[ch];
				// calc first full window sum
				mus			= 0.0;
				for( i = 0, m = inOff; i < winSizeM1; i++, m++ ) {
					mus	   += convBuf2[ m ];	// sum all but last one in window
				}
				omus		= 0.0;
				for( j = 0, m = inOff, n = outOff; j < len; j++, m++, n++ ) {
					// shift by one : remove obsolete sample
					// and add new last window sample
					mus		= mus - omus + convBuf2[ m + winSizeM1 ];
					mu		= mus / winSize;	// mean now
					sum		= 0.0;
					for( i = 0, k = m; i < winSize; i++, k++ ) {
						d1		= convBuf2[ k ] - mu;
						sum	   += d1 * d1;	// variance
					}
					d1				= Math.sqrt( sum );	// standard deviation
				// ---- remove DC ----
					d2				= d1 - convBuf4[ 0 ] + 0.99 * convBuf4[ 1 ];
					convBuf3[ n ]	= (float) d2;
					convBuf4[ 0 ]	= d1;
					convBuf4[ 1 ]	= d2;
					omus			= convBuf2[ m ];
				}
			} // for channels
		} // process
	} // class StdDevFilter

	private class MinimumFilter
	extends RunningWindowFilter
	{
		final int			winSize;
		final int			channels;
		final int			winSizeM1;
	
		public MinimumFilter( int winSize, int channels )
		{
			this.winSize	= winSize;
			this.channels   = channels;
			winSizeM1		= winSize - 1;
		}
	
		public void process( float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len )
		throws IOException
		{
			int			ch, i, j, k, m, n, minidx;
			float[]		convBuf2, convBuf3;
			float		f1, f2, min;
			
			for( ch = 0; ch < channels; ch++ ) {
				convBuf2	= inBuf[ch];
				convBuf3	= outBuf[ch];
				minidx		= -1;
				min			= 0.0f;
				for( j = 0, m = inOff, n = outOff; j < len; j++, m++, n++ ) {
					if( minidx < m ) {	// need to find again
						f1		= Math.abs( convBuf2[ m ]);
						minidx	= m;
						for( i = 1, k = m + 1; i < winSize; i++, k++ ) {
							f2	= Math.abs( convBuf2[ k ]);
							if( f2 < f1 ) {
								f1		= f2;
								minidx	= k;
							}
						}
						min	= convBuf2[ minidx ];
					} else {
						f1	= convBuf2[ m + winSizeM1 ];
						if( Math.abs( f1 ) < Math.abs( min )) {
							min		= f1;
							minidx	= m + winSizeM1;
						}
					}
					convBuf3[ n ]	= min;
					minidx--;
				}
			} // for channels
		} // process
	} // class MinimumFilter

	private class MedianFilter
	extends RunningWindowFilter
	{
		final int		winSize, medianOff, winSizeM;
		final int		channels;
		final float[][] buf;
		final int[][]	idxBuf;
	
		private MedianFilter( int winSize, int channels )
		{
			this.winSize	= winSize;
			this.channels   = channels;
			
			buf			= new float[channels][winSize];
			idxBuf		= new int[channels][winSize];
			medianOff   = winSize >> 1;
			winSizeM	= winSize - 1;
		}
	
		public void process( float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len )
		throws IOException
		{
//			Util.clear( buf );
			
			int		ch, i, j, k, m, n;
			float[] convBuf1, convBuf2, convBuf3;
			int[]   convBuf4;
			float   f1;
			
			for( ch = 0; ch < channels; ch++ ) {
				convBuf1	= buf[ch];
				convBuf2	= inBuf[ch];
				convBuf3	= outBuf[ch];
				convBuf4	= idxBuf[ch];
				m			= inOff;
				n			= outOff;
				convBuf1[0] = convBuf2[m++];
				convBuf4[0] = 0;

				// --- calculate the initial median by sorting inBuf content of length 'winSize ---
				// XXX this is a really slow sorting algorithm and should be replaced by a fast one
				// e.g. by exchanging the j-loop by a step-algorithm (stepping right into
				// i/2 and if f1 < convBuf1[i/2] steppping to i/4 else i*3/4 etc.
				for( i = 1; i < winSize; i++ ) {
					f1  = convBuf2[m++];
					for( j = 0; j < i; j++ ) {
						if( f1 < convBuf1[j] ) {
							System.arraycopy( convBuf1, j, convBuf1, j + 1, i - j );
							for( k = 0; k < i; k++ ) {
								if( convBuf4[k] >= j ) convBuf4[k]++;
							}
							break;
						}
					}
					convBuf1[j] = f1;
					convBuf4[i] = j;
				}
				// now the median is approx. (for winSize >> 1) the sample in convBuf1[winSize/2]

//System.err.println( "A---unsorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf2[inOff+p] );
//}
//System.err.println( " --sorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf1[p] );
//}

				// XXX this is a really slow sorting algorithm and should be replaced by a fast one
				// e.g. by exchanging the j-loop by a step-algorithm (stepping right into
				// i/2 and if f1 < convBuf1[i/2] steppping to i/4 else i*3/4 etc.
				// ; also the two arraycopies could be collapsed into one or two shorter ones
				for( i = 0; i < len; i++ ) {
					convBuf3[n++] = convBuf1[medianOff];

					j   = convBuf4[i%winSize];  // index of the element to be removed (i.e. shifted left out of the win)
					System.arraycopy( convBuf1, j + 1, convBuf1, j, winSizeM - j );
					for( k = 0; k < winSize; k++ ) {
						if( convBuf4[k] > j ) convBuf4[k]--;
					}
					f1  = convBuf2[m++];
					for( j = 0; j < winSizeM; j++ ) {
						if( f1 < convBuf1[j] ) {
							System.arraycopy( convBuf1, j, convBuf1, j + 1, winSizeM - j );
							for( k = 0; k < winSize; k++ ) {
								if( convBuf4[k] >= j ) convBuf4[k]++;
							}
							break;
						}
					}
					// j = index of the element to be inserted (i.e. coming from the right side of the win)
					convBuf1[j] = f1;
					convBuf4[i%winSize] = j;
				}
//System.err.println( "B---unsorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf2[inOff+len+p] );
//}
//System.err.println( " ---sorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf1[p] );
//}
			} // for channels
		} // process
	} // class MedianFilter

	// Center Clipping for a variable threshold
	// which is determined by a running histogram
	// and a percentage threshold value
	//
	// this only works if a) process() is
	// called on successive chunks; b) samples don't exceed +12 dBFS
	private class CenterClippingFilter
	extends RunningWindowFilter
	{
		final int			winSize;
		final int			channels;
		final int			winSizeM1;
		final int[][]		histogram;
		final int			threshSum;
		boolean	init	= false;
	
		public CenterClippingFilter( int winSize, int channels )
		{
			this.winSize	= winSize;
			this.channels   = channels;
			winSizeM1		= winSize - 1;
			histogram		= new int[ channels ][ 16384 ];
			threshSum		= (int) (Param.transform( pr.para[ PR_THRESH ], Param.ABS_AMP,
													  new Param( 1.0, Param.ABS_AMP ), null ).val * winSize + 0.5);
		}
	
		public void process( float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len )
		throws IOException
		{
			float[]		convBuf2, convBuf3;
			int[]		convBuf4;
			int			histoIdx, histoSum;
			float		f1, clip;
			
			for( int ch = 0; ch < channels; ch++ ) {
				convBuf4	= histogram[ ch ];
				convBuf2	= inBuf[ ch ];
				convBuf3	= outBuf[ ch ];
				// calc first maximum
//				max			= 0.0f;
//				for( int i = 0, m = inOff; i < len; i++, m++ ) {
//					f1 = Math.abs( convBuf2[ m ]);
//					if( f1 > max ) max = f1;
//				}
				// then calc initial histo
//				for( int i = 0; i < 8192; i++ ) {
//					convBuf4[ i ] = 0;
//				}
				if( !init ) {
					for( int i = 0, j = inOff; i < winSizeM1; i++, j++ ) {
						f1			= convBuf2[ j ];
						histoIdx	= (int) (Math.sqrt( Math.min( 1.0f, Math.abs( f1 / 4))) * 16383.5);
//						histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
						convBuf4[ histoIdx ]++;
					}
				}
				for( int j = 0, m = inOff, n = outOff; j < len; j++, m++, n++ ) {
					// shift by one : remove obsolete sample
					// and add new last window sample
					f1			= convBuf2[ m + winSizeM1 ];
					histoIdx	= (int) (Math.sqrt( Math.min( 1.0f, Math.abs( f1 / 4))) * 16383.5);
//					histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
					convBuf4[ histoIdx ]++;

					// find thresh
					for( histoIdx = 0, histoSum = 0; histoIdx < 8192 && histoSum < threshSum; histoIdx++ ) {
						histoSum += convBuf4[ histoIdx ];
					}
					clip	= (float) histoIdx / 16383;
					clip	= clip*clip*4;
//					clip	= (float) (Math.exp( (histoIdx - 8191) / 381.2437 ) * 4);
					f1		= convBuf2[ m ];
					if( f1 >= 0.0f ) {
						convBuf3[ n ] = Math.max( 0.0f, f1 - clip );
					} else {
						convBuf3[ n ] = Math.min( 0.0f, f1 + clip );
					}
					f1			= convBuf2[ m ];	// now obsolete
					histoIdx	= (int) (Math.sqrt( Math.min( 1.0f, Math.abs( f1 / 4))) * 16383.5);
//					histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
					convBuf4[ histoIdx ]--;
				}
			} // for channels
			
			init = true;
		} // process
	} // class CenterClippingFilter
} // class NeedleholeDlg
