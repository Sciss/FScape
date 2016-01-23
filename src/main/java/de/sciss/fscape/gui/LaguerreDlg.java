/*
 *  LaguerreDlg.java
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

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *	Frequency Warping processing module via Short-Time Laguerre Transform
 *	based on papers by G. Evangelista. This is totally slow
 *	and also buggy causing comb filter effects for wrong window
 *	ratios. However kind of works when splitting the sound file
 *	into two frequency bands which are warped independantly with
 *	small windows for high frequencies and large windows for low
 *	frequencies. Tends to sound chirpy.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class LaguerreDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_OUTPUTFILE		= 1;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_FRAMESIZE		= 3;
	private static final int PR_OVERLAP			= 4;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_WARP			= 1;
	private static final int PR_WARPMODDEPTH	= 2;
	private static final int PR_INFREQ			= 3;
	private static final int PR_OUTFREQ			= 4;
	private static final int PR_WARPMOD			= 0;		// pr.bool
	private static final int PR_WARPENV			= 0;		// pr.envl

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_WARP			= "Warp";
	private static final String PRN_WARPMODDEPTH	= "WarpModDepth";
	private static final String PRN_INFREQ			= "InFreq";
	private static final String PRN_OUTFREQ			= "OutFreq";
	private static final String PRN_WARPMOD			= "WarpMod";
	private static final String PRN_WARPENV			= "WarpEnv";
	private static final String PRN_FRAMESIZE		= "FrameSize";
	private static final String PRN_OVERLAP			= "Overlap";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, 0, 4, 2 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_FRAMESIZE,
													PRN_OVERLAP };
	private static final boolean	prBool[]	= { false };
	private static final String	prBoolName[]	= { PRN_WARPMOD };
	private static final Param	prPara[]		= { null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_WARP, PRN_WARPMODDEPTH, PRN_INFREQ, PRN_OUTFREQ };
	private static final Envelope	prEnvl[]	= { null };
	private static final String	prEnvlName[]	= { PRN_WARPENV };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_FRAMESIZE		= GG_OFF_CHOICE		+ PR_FRAMESIZE;
	private static final int GG_OVERLAP			= GG_OFF_CHOICE		+ PR_OVERLAP;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_WARP			= GG_OFF_PARAMFIELD	+ PR_WARP;
	private static final int GG_WARPMODDEPTH	= GG_OFF_PARAMFIELD	+ PR_WARPMODDEPTH;
	private static final int GG_INFREQ			= GG_OFF_PARAMFIELD	+ PR_INFREQ;
	private static final int GG_OUTFREQ			= GG_OFF_PARAMFIELD	+ PR_OUTFREQ;
	private static final int GG_WARPMOD			= GG_OFF_CHECKBOX	+ PR_WARPMOD;
	private static final int GG_WARPENV			= GG_OFF_ENVICON	+ PR_WARPENV;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private float		inRate	= 0.0f;	// sample rate input file

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public LaguerreDlg()
	{
		super( "Laguerre Warping" );
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
			static_pr.para[ PR_WARP ]			= new Param(  -10.0, Param.FACTOR );
			static_pr.para[ PR_WARPMODDEPTH ]	= new Param(   20.0, Param.OFFSET_AMP );
			static_pr.para[ PR_INFREQ ]			= new Param( 1000.0, Param.ABS_HZ );
			static_pr.para[ PR_OUTFREQ ]		= new Param( 1000.0, Param.ABS_HZ );
			static_pr.paraName	= prParaName;
			static_pr.envl		= prEnvl;
			static_pr.envl[ PR_WARPENV ]		= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName	= prEnvlName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		PathField[]			ggParent1;
		ParamField			ggWarp, ggWarpModDepth, ggInFreq, ggOutFreq;
		JCheckBox			ggWarpMod;
		EnvIcon				ggWarpEnv;
		Component[]			ggGain;
		JComboBox				ggFrameSize, ggOverlap;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ParamListener paramL = new ParamListener() {
			public void paramChanged( ParamEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_WARP:
				case GG_INFREQ:
					pr.para[ ID - GG_OFF_PARAMFIELD ] = ((ParamField) e.getSource()).getParam();
					recalcOutFreq();
					break;

				case GG_OUTFREQ:
					pr.para[ ID - GG_OFF_PARAMFIELD ] = ((ParamField) e.getSource()).getParam();
					recalcWarpAmount();
					break;
				}
			}
		};
		
		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_WARPMOD:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					reflectPropertyChanges();
					break;
				}
			}
		};
		
		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_INPUTFILE:
					setInput( ((PathField) e.getSource()).getPath().getPath() );
					break;
				}
			}
		};
		
	// -------- I/O-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggParent1		= new PathField[ 1 ];
		ggParent1[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggParent1, "$D0$F0Wrp$E" );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, paramL );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Warp settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggWarp			= new ParamField( Constants.spaces[ Constants.modSpace ]);			// XXX
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Warp amount", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggWarp, GG_WARP, paramL );

		ggWarpModDepth	= new ParamField( Constants.spaces[ Constants.offsetAmpSpace ]);	// XXX
		ggWarpModDepth.setReference( ggWarp );
		ggWarpMod		= new JCheckBox();
		con.weightx		= 0.1;
		gui.addCheckbox( ggWarpMod, GG_WARPMOD, il );
		con.weightx		= 0.4;
		gui.addParamField( ggWarpModDepth, GG_WARPMODDEPTH, paramL );

		ggWarpEnv		= new EnvIcon( getComponent() );
		con.weightx		= 0.1;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addGadget( ggWarpEnv, GG_WARPENV );

		ggInFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Input freq.", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggInFreq, GG_INFREQ, paramL );
		ggOutFreq		= new ParamField( Constants.spaces[ Constants.absHzSpace ]);
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "\u2192 Output freq.", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggOutFreq, GG_OUTFREQ, paramL );

		ggFrameSize		= new JComboBox();
		for( int i = 32; i <= 32768; i <<= 1 ) {
			ggFrameSize.addItem( String.valueOf( i ));
		}
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Frame size [smp]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggFrameSize, GG_FRAMESIZE, il );

		ggOverlap		= new JComboBox();
		for( int i = 1; i <= 16; i++ ) {
			ggOverlap.addItem( i + "x" );
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Overlap", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggOverlap, GG_OVERLAP, il );

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
		int					i, j, len, ch, chunkLength;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr			inStream;
		AudioFileDescr			outStream;
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;
		
		// buffers
		float[][]			inBuf, outBuf;
		float[]				win;
		float[]				convBuf1, convBuf2;
		float[]				tempFlt;

		int					inChanNum, inLength, inputStep, outputStep, winSize;
		int					transLen, skip, inputLen, outputLen, fltLen;
		int					framesRead, framesWritten;
		float				warp, a1, b0, b1, x0, x1, y0, y1, b0init;

		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		Param				peakGain;
		float				gain			= 1.0f;								 		// gain abs amp
		float				maxAmp			= 0.0f;

		PathField			ggOutput;

topLevel: try {

		// ---- open input, output ----

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

		// ---- parameter inits ----

			warp		= Math.max( -0.98f, Math.min( 0.98f, (float) (pr.para[ PR_WARP ].val / 100) ));	// DAFx2000 'b'
			f1			= (1.0f - warp) / (1.0f + warp);					// DAFx2000 (25)
			winSize		= 32 << pr.intg[ PR_FRAMESIZE ];					// DAFx2000 'N'
			j			= winSize >> 1;
			transLen	= (int) (f1 * winSize + 0.5f);						// DAFx2000 'P' (26)
			i			= pr.intg[ PR_OVERLAP ] + 1;
			while( ((float) transLen / (float) i) > j ) i++;
			inputStep	= (int) (((float) transLen / (float) i) + 0.5f);	// DAFx2000 'L'
 			fltLen		= Math.max( winSize, transLen );
// System.out.println( "inputStep "+inputStep+"; winSize "+winSize+"; transLen "+transLen+"; fltLen "+fltLen+"; warp "+warp+"; f1 "+f1 );
			win			= Filter.createFullWindow( winSize, Filter.WIN_HANNING );		// DAFx2000 (27)
			outputStep	= inputStep;

			b0init		= (float) Math.sqrt( 1.0f - warp*warp );

			progOff		= 0;
			progLen		= (long) inLength * (2 + inChanNum); // + winSize;

			tempFlt		= new float[ fltLen ];
			inputLen	= winSize + inputStep;
			inBuf		= new float[ inChanNum ][ inputLen ];
			outputLen	= transLen + outputStep;
			outBuf		= new float[ inChanNum ][ outputLen ];

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
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
				}
				progLen	   += (long) inLength;
			} else {
				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== the real stuff ====================----

			framesRead		= 0;
			framesWritten	= 0;
			skip			= 0;

			while( threadRunning && (framesWritten < inLength) ) {

				chunkLength = Math.min( inputLen, inLength - framesRead + skip );
			// ---- read input chunk ----
				len			 = Math.max( 0, chunkLength - skip );
				inF.readFrames( inBuf, skip, len );
				framesRead	+= len;
				progOff		+= len;
//				off			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero padding
				if( chunkLength < inputLen ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = chunkLength; i < convBuf1.length; i++ ) {
							convBuf1[ i ] = 0.0f;
						}
					}
				}

				for( ch = 0; threadRunning && (ch < inChanNum); ch++ ) {
					convBuf1 = inBuf[ ch ];
					convBuf2 = outBuf[ ch ];

					for( i = 0, j = fltLen; i < winSize; i++ ) {
						tempFlt[ --j ] = convBuf1[ i ] * win[ i ];
					}
					while( j > 0 ) {
						tempFlt[ --j ] = 0.0f;
					}

					a1			= -warp;						// inital allpass
					b0			= b0init;
					b1			= 0.0f;
					for( j = 0; j < transLen; j++ ) {
						x1			= 0.0f;
						y1			= 0.0f;

//						for( i = 0; i < transLen; i++ ) {		// DAFx2000 (2 resp. 3)
						for( i = 0; i < fltLen; i++ ) {			// DAFx2000 (2 resp. 3)
							x0			= tempFlt[i];
							y0			= b0*x0 + b1*x1 - a1*y1;
							tempFlt[i]	= y0;	// (work with double precision while computing cascades)
							y1			= y0;
							x1			= x0;
						}

						a1			= -warp;					// cascaded allpasses
						b0			= -warp;
						b1			= 1.0f;
				
						convBuf2[j] += y1;
					}
				// .... progress ....
					progOff += chunkLength - skip;
					setProgression( (float) progOff / (float) progLen );

				} // for channels
			// .... check running ....
				if( !threadRunning ) break topLevel;

				chunkLength = Math.min( outputStep, inLength - framesWritten );
			// ---- write output chunk ----
				if( floatF != null ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						floatF[ ch ].writeFloats( outBuf[ ch ], 0, chunkLength );
					}
					progOff		  += chunkLength;
//					off			  += len;
					framesWritten += chunkLength;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );

				} else {
					for( ch = 0; ch < inChanNum; ch++ ) {
						Util.mult( outBuf[ ch ], 0, chunkLength, gain );
					}
					outF.writeFrames( outBuf, 0, chunkLength );
					progOff		  += chunkLength;
//					off			  += len;
					framesWritten += chunkLength;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				}
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// check max amp
				for( ch = 0; ch < inChanNum; ch++ ) {
					convBuf1 = outBuf[ ch ];
					for( i = 0; i < chunkLength; i++ ) {
						f1 = Math.abs( convBuf1[ i ]);
						if( f1 > maxAmp ) {
							maxAmp = f1;
						}
					}
				}
				
				// overlaps
				skip = winSize;
				for( ch = 0; ch < inChanNum; ch++ ) {
					System.arraycopy( inBuf[ ch ], inputStep, inBuf[ ch ], 0, winSize );
					convBuf1 = outBuf[ ch ];
					System.arraycopy( convBuf1, outputStep, convBuf1, 0, transLen );
					for( i = transLen; i < outputLen; ) {
						convBuf1[ i++ ] = 0.0f;
					}
				}

			} // until framesWritten == outLength
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== normalize output ====================----

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
				normalizeAudioFile( floatF, outF, inBuf, gain, 1.0f );
				maxAmp	*= gain;

				for( ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ]	= null;
					tempFile[ ch ].delete();
					tempFile[ ch ]	= null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----

			outF.close();
			outF		= null;
			outStream	= null;
			inF.close();
			inF			= null;
			inStream	= null;
			inBuf		= null;

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
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
			inF = null;
		}
		if( outF != null ) {
			outF.cleanUp();
			outF = null;
		}
		if( floatF != null ) {
			for( ch = 0; ch < floatF.length; ch++ ) {
				if( floatF[ ch ] != null ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ] = null;
				}
				if( tempFile[ ch ] != null ) {
					tempFile[ ch ].delete();
					tempFile[ ch ] = null;
				}
			}
		}
	} // process()


	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component	c;
		
		c = gui.getItemObj( GG_WARPMODDEPTH );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_WARPMOD ]);
		}
		c = gui.getItemObj( GG_WARPENV );
		if( c != null ) {
			c.setEnabled( pr.bool[ PR_WARPMOD ]);
		}
		c = gui.getItemObj( GG_OUTFREQ );
		if( c != null ) {
			c.setEnabled( inRate != 0f );
		}
	}

	/**
	 *	Neues Inputfile setzen
	 */
	protected void setInput( String fname )
	{
		AudioFile		f		= null;
		AudioFileDescr	stream	= null;

	// ---- Header lesen ----
		try {
			f		= AudioFile.openAsRead( new File( fname ));
			stream	= f.getDescr();
			f.close();

			inRate = (float) stream.rate;
			recalcOutFreq();
			
		} catch( IOException e1 ) {
			inRate = 0f;
		}
		
		reflectPropertyChanges();
	}
	
	protected void recalcOutFreq()
	{
		if( inRate == 0f ) return;

		double		omegaIn, omegaOut, warp;
		ParamField	ggOutFreq;
		
		omegaIn		= pr.para[ PR_INFREQ ].val / inRate * Constants.PI2;
		warp		= Math.max( -0.98, Math.min( 0.98, pr.para[ PR_WARP ].val / 100 ));	// DAFx2000 'b'
		omegaOut	= omegaIn + 2 * Math.atan2( warp * Math.sin( omegaIn ), 1.0 - warp * Math.cos( omegaIn ));
		
		ggOutFreq	= (ParamField) gui.getItemObj( GG_OUTFREQ );
		if( ggOutFreq != null ) {
			ggOutFreq.setParam( new Param( omegaOut / Constants.PI2 * inRate, Param.ABS_HZ ));
		}
	}

	protected void recalcWarpAmount()
	{
		if( inRate == 0f ) return;

		double		omegaIn, omegaOut, warp, d1;
		ParamField	ggWarp;
		
		omegaIn		= pr.para[ PR_INFREQ ].val  / inRate * Constants.PI2;
		omegaOut	= pr.para[ PR_OUTFREQ ].val / inRate * Constants.PI2;
		d1			= Math.tan( (omegaOut - omegaIn) / 2 );
		warp		= Math.max( -0.98, Math.min( 0.98, d1 / (Math.sin( omegaIn ) + Math.cos( omegaIn ) * d1 )));	// DAFx2000 'b'
		
		ggWarp		= (ParamField) gui.getItemObj( GG_WARP );
		if( ggWarp != null ) {
			ggWarp.setParam( new Param( warp * 100, Param.FACTOR ));
		}
	}
}
// class LaguerreDlg
