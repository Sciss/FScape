/*
 *  RotationDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		05-Nov-04	bugfix
 *		12-Mar-05	added 'subtract dry signal' + output gain
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
 *	Processing module that tracks zero crossings and
 *	"rotates" or "flips" wave sections, hence producing
 *	nice distortions.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.67, 12-Mar-05
 *
 *	@todo		pass two needs I/O speed up by increasing
 *				inBuf.length to 8k and reading multiple
 *				chunks at once
 */
public class RotationDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_MODE				= 2;
	private static final int PR_GAINTYPE			= 3;
	private static final int PR_REPEATS				= 0;		// pr.para
	private static final int PR_GAIN				= 1;
	private static final int PR_SUBDRY				= 0;		// pr.bool

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_MODE			= "Mode";
	private static final String PRN_REPEATS			= "Repeats";
	private static final String PRN_SUBDRY			= "SubDry";

	private static final int MODE_ROTATION			= 0;
	private static final int MODE_REPEAT			= 1;

	private static final String	prText[]			= { "", "" };
	private static final String	prTextName[]		= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int	prIntg[]			= { 0, 0, MODE_ROTATION, GAIN_UNITY };
	private static final String	prIntgName[]		= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_MODE, PRN_GAINTYPE };
	private static final Param	prPara[]			= { new Param( 2.0, Param.NONE ),
														null };
	private static final String	prParaName[]		= { PRN_REPEATS, PRN_GAIN };
	private static final boolean prBool[]			= { false };
	private static final String	prBoolName[]		= { PRN_SUBDRY };

	private static final int GG_INPUTFILE			= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_MODE				= GG_OFF_CHOICE		+ PR_MODE;
	private static final int GG_GAINTYPE			= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_REPEATS				= GG_OFF_PARAMFIELD	+ PR_REPEATS;
	private static final int GG_GAIN				= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_SUBDRY				= GG_OFF_CHECKBOX	+ PR_SUBDRY;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public RotationDlg()
	{
		super( "Rotation" );
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
		JComboBox			ggMode;
		JCheckBox			ggSubDry;
		ParamField			ggRepeats;
		PathField[]			ggInputs;
		Component[]			ggGain;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case GG_MODE:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
					reflectPropertyChanges();
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
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
//		ggInputs[ 1 ]	= ggModFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Rot$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );

		ggGain			= createGadgets( GGTYPE_GAIN );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Gain", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( (ParamField) ggGain[ 0 ], GG_GAIN, null );
		con.weightx		= 0.5;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, il );

	// -------- Settings-Gadgets --------
	gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggMode			= new JComboBox();
		ggMode.addItem( "Rotate" );
		ggMode.addItem( "Repeat" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Mode", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggMode, GG_MODE, il );

		ggRepeats		= new ParamField( new ParamSpace( 2.0, 10000.0, 1.0, Param.NONE ));
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Repeats", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggRepeats, GG_REPEATS, null );

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
		int					len;
		long				progOff, progLen;
		float				f1, f2, f3, f4;
		
		// io
		AudioFile			inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		inStream		= null;
		AudioFileDescr		outStream		= null;
		FloatFile			zcFloatFile		= null;
		File				zcTempFile		= null;
		float[][]			inBuf			= null;
		float[]				convBuf1;

		// Smp Init
		int					inLength, inChanNum, zcLength;
		int					framesRead, framesWritten;

		int					inBufSize, zcRead;
		float				oldGrad, newGrad;
		int[]				zcBuf;

		int					loc, lastLoc, maxDist;
		float[]				lastSample, steigung;

		PathField			ggOutput;

		boolean				rotate			= pr.intg[ PR_MODE ] == MODE_ROTATION;
		boolean				subDry			= rotate && pr.bool[PR_SUBDRY];
		int					numRepeats		= rotate ? 1 : (int) (pr.para[ PR_REPEATS ].val + 0.5);

		float				gain			= 1.0f;								 		// gain abs amp
		float				maxAmp			= 0.0f;
		
		FloatFile[]			floatF			= null;
		File				tempFile[]		= null;
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz

		int					distance;

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
			progLen				= (long) inLength * 2 + (long) inLength * numRepeats;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( inStream );
			ggOutput.fillStream( outStream );
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			inBufSize		= 8192;
			inBuf			= new float[ inChanNum ][ inBufSize ];

			zcBuf			= new int[ 1 ]; // next dist
			zcTempFile		= IOUtil.createTempFile();
			zcFloatFile		= new FloatFile( zcTempFile, GenericFile.MODE_OUTPUT );

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				tempFile	= new File[ inChanNum ];
				floatF		= new FloatFile[ inChanNum ];
				for( int ch = 0; ch < inChanNum; ch++ ) {
					tempFile[ ch ]	= IOUtil.createTempFile();
					floatF[ ch ]	= new FloatFile( tempFile[ ch ], GenericFile.MODE_OUTPUT );
				}
				progLen	   += inLength;
			} else {
				gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== finding min/max's ====================----

			framesRead		= 0;
			newGrad			= Float.NaN; // 0.0f;
			maxDist			= 0;
			loc				= 0;

			while( threadRunning && (framesRead < inLength) ) {
				for( int ch = 0; ch < inChanNum; ch++ ) {
					inBuf[ ch ][ 0 ] = inBuf[ ch ][ inBufSize - 1 ];
				}
				// ==================== read input chunk ====================
				len			= Math.min( inLength - framesRead, inBufSize - 1 );
				inF.readFrames( inBuf, 1, len );
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// ---- Rotation ----------------------------------------------------------------------

				convBuf1 = inBuf[ 0 ];
				f2		 = convBuf1[ 0 ];
				for( int i = 1; i <= len; i++ ) {
					f1		= f2;
					f2		= convBuf1[ i ];
					oldGrad = newGrad;
					newGrad = f2 - f1;
					
					if( ((oldGrad <= 0f) && (newGrad > 0f) ) || ((oldGrad > 0f) && (newGrad <= 0f)) ) {		// local min or max
						lastLoc = loc;
						loc		= framesRead + i - 1;
						distance= loc - lastLoc;
						if( distance > maxDist ) {
							maxDist = distance;
						}
						zcBuf[ 0 ] = distance;
						zcFloatFile.writeInts( zcBuf );
//						if( j == 0 ) {
//							System.err.println( "j = 0! framesRead = "+framesRead+"; i = "+i+"; lastLoc = "+lastLoc+ "; loc = "+loc );
//						}
					}
				}

				convBuf1[ 0 ] = convBuf1[ len ];
				framesRead   += len;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
			// zero crossing file erhaelt einen terminierenden eintrag
			lastLoc  = loc;
			loc		 = inLength;
			distance = loc - lastLoc;

			if( distance > maxDist ) {
				maxDist = distance;
			}
			zcBuf[ 0 ] = distance;
			zcFloatFile.writeInts( zcBuf );
	
			if( floatF == null ) indicateOutputWrite();
	
		// ============= PASS TWO ================

			inBufSize		= Math.max( 8192, maxDist * numRepeats );	// min. 8192 otherwise normalize is slow
			inBuf			= new float[ inChanNum ][ inBufSize ];
//System.err.println( "maxDist : "+maxDist );

			framesRead		= 0;
			zcRead			= 0;
			framesWritten	= 0;
			
			inF.seekFrame( 0 );
			zcFloatFile.seekFloat( 0 );
			zcLength		= (int) zcFloatFile.getSize();
			lastSample		= new float[ inChanNum ];
			steigung		= new float[ inChanNum ];

			while( threadRunning && (zcRead < zcLength) ) {

				zcFloatFile.readInts( zcBuf );
				zcRead++;
				len = zcBuf[0];
				if( !rotate && zcRead < zcLength ) {	// repeats uses full cycle of course
					zcFloatFile.readInts( zcBuf );
					zcRead++;
					len += zcBuf[0];
				}
				inF.readFrames( inBuf, 0, len );
				framesRead += len;
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;
				
				if( rotate ) {							// rotation
					for( int ch = 0; ch < inChanNum; ch++ ) {
						f1		= lastSample[ch];
						convBuf1= inBuf[ch];
						f2		= convBuf1[ len - 1 ];
						f3		= f1 + f2;
						if( subDry ) {
							for( int i = 0, j = len - 1; i <= j; i++, j-- ) {
//								f4			= f3 - convBuf1[i];
//								convBuf1[i]	= f3 - convBuf1[j] - convBuf1[i];
//								convBuf1[j]	= f4 - convBuf1[j];
								f4			= f3 - convBuf1[i] - convBuf1[j];
								convBuf1[i]	= f4;
								convBuf1[j]	= f4;
							}
						} else {
							for( int i = 0, j = len - 1; i <= j; i++, j-- ) {
								f4			= f3 - convBuf1[i];
								convBuf1[i]	= f3 - convBuf1[j];
								convBuf1[j]	= f4;
							}
						}
						lastSample[ch]  = f2;
					}
				} else {								// repetition
					for( int ch = 0; ch < inChanNum; ch++ ) {
						f2				= inBuf[ch][len-1];
						f1				= f2 - lastSample[ch];
						steigung[ch]	= f1 / numRepeats;
						lastSample[ch]  = f2;
						convBuf1		= inBuf[ch];
						f1				= (-f1 + steigung[ch]) / len;
						for( int j = 0, k = 1; j < len; j++, k++ ) {
							convBuf1[j] += k * f1;
						}
					}
				}
				
				// ---- write output ----
				if( floatF == null ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( int i = 0; i < len; i++ ) {
							convBuf1[ i ] *= gain;
							f1			   = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) maxAmp = f1;
						}
					}
					outF.writeFrames( inBuf, 0, len );
				} else {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( int i = 0; i < len; i++ ) {
							f1 = Math.abs( convBuf1[ i ]);
							if( f1 > maxAmp ) maxAmp = f1;
						}
						floatF[ ch ].writeFloats( inBuf[ ch ], 0, len );
					}
				}
				framesWritten += len;
				progOff		  += len;
				
				// ---- repeats ----
				for( int repeat = 1; repeat < numRepeats; repeat++ ) {
					for( int ch = 0; ch < inChanNum; ch++ ) {
						f1			= steigung[ch] * gain;
						convBuf1	= inBuf[ch];
						for( int i = 0; i < len; i++ ) {
							convBuf1[ i ] += f1;
							f2			   = Math.abs( convBuf1[ i ]);
							if( f2 > maxAmp ) maxAmp = f2;
						}
					}
					// ---- write output ----
					if( floatF == null ) {
						outF.writeFrames( inBuf, 0, len );
					} else {
						for( int ch = 0; ch < inChanNum; ch++ ) {
							floatF[ ch ].writeFloats( inBuf[ ch ], 0, len );
						}
					}
					framesWritten += len;
					progOff		  += len;
				} // for repeats
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );		
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----	
			inF.close();
			inF			= null;
			inStream	= null;

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
									new Param( 1.0 / maxAmp, Param.ABS_AMP ), null )).val;
				normalizeAudioFile( floatF, outF, inBuf, gain, 1.0f );
				maxAmp	*= gain;

				for( int ch = 0; ch < inChanNum; ch++ ) {
					floatF[ ch ].cleanUp();
					floatF[ ch ]	= null;
					tempFile[ ch ].delete();
					tempFile[ ch ]	= null;
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outF.close();
			outF = null;

			zcFloatFile.cleanUp();
			zcFloatFile = null;
			zcTempFile.delete();

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
			System.gc();

			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
		}
		if( outF != null ) {
			outF.cleanUp();
		}
		if( zcFloatFile != null ) {
			zcFloatFile.cleanUp();
			zcTempFile.delete();
		}
		if( floatF != null ) {
			for( int ch = 0; ch < floatF.length; ch++ ) {
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

// -------- private Methoden --------

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_REPEATS );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_MODE ] == MODE_REPEAT );
		}
		c = gui.getItemObj( GG_SUBDRY );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_MODE ] == MODE_ROTATION );
		}
	}
}
// class RotationDlg