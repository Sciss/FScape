/*
 *  SeekDestroyDlg.java
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

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Processing module for "really large scale" waveshaping.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class SeekDestroyDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_SHAPEFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_TRAJFILE			= 2;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_SHAPE				= 2;
	private static final int PR_SIGMODE				= 3;
	private static final int PR_DCBLOCK				= 0;		// pr.bool

	private static final int SHAPE_FILE				= 0;
	private static final int SHAPE_SINE				= 1;
	private static final int SHAPE_TRI				= 2;
	private static final int SHAPE_SQRT				= 3;
	private static final int SHAPE_SQR				= 4;

	private static final int SIGMODE_BICENTER		= 0;
	private static final int SIGMODE_BIWRAP			= 1;
	private static final int SIGMODE_UNI			= 2;
	private static final int SIGMODE_UNISIGN		= 3;

	private static final String PRN_SHAPEFILE		= "ShapeFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_TRAJFILE		= "TrajFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_SHAPE			= "Shape";
	private static final String PRN_SIGMODE			= "SigMode";
	private static final String PRN_DCBLOCK			= "DCBlock";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_SHAPEFILE, PRN_OUTPUTFILE, PRN_TRAJFILE };
	private static final int		prIntg[]	= { 0, 0, SHAPE_SINE, SIGMODE_BIWRAP };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_SHAPE, PRN_SIGMODE };
	private static final boolean	prBool[]	= { false };
	private static final String	prBoolName[]	= { PRN_DCBLOCK };

	private static final int GG_SHAPEFILE			= GG_OFF_PATHFIELD	+ PR_SHAPEFILE;
	private static final int GG_OUTPUTFILE			= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_TRAJFILE			= GG_OFF_PATHFIELD	+ PR_TRAJFILE;
	private static final int GG_OUTPUTTYPE			= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES			= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_SHAPE				= GG_OFF_CHOICE		+ PR_SHAPE;
	private static final int GG_SIGMODE				= GG_OFF_CHOICE		+ PR_SIGMODE;
	private static final int GG_DCBLOCK				= GG_OFF_CHECKBOX	+ PR_DCBLOCK;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SeekDestroyDlg()
	{
		super( "Seek + Destroy" );
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
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggShapeFile, ggOutputFile, ggTrajFile;
		PathField[]			ggInputs;
		JComboBox			ggShape, ggSigMode;
		JCheckBox			ggDCBlock;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int	ID = gui.getItemID( e );

				switch( ID ) {
				case GG_SHAPE:
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

		ggTrajFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggTrajFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggTrajFile, GG_TRAJFILE, null );

		ggShapeFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select shape file" );
		ggShapeFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Shape file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggShapeFile, GG_SHAPEFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggTrajFile;
//		ggInputs[ 1 ]	= ggShapeFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Shp$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Shaper Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		
		ggShape			= new JComboBox();
		ggShape.addItem( "Soundfile" );
		ggShape.addItem( "Sine" );
		ggShape.addItem( "Triangle" );
		ggShape.addItem( "Sqrt(x)" );
		ggShape.addItem( "Square(x)" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Function", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addChoice( ggShape, GG_SHAPE, il );

		ggSigMode		= new JComboBox();
		ggSigMode.addItem( "Bipolar centered" );
		ggSigMode.addItem( "Bipolar wrapped" );
		ggSigMode.addItem( "Unipolar" );
		ggSigMode.addItem( "Unipolar*sign(x)" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Signal mode", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.4;
		gui.addChoice( ggSigMode, GG_SIGMODE, il );

		ggDCBlock		= new JCheckBox();
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "DC blocking", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.4;
		gui.addCheckbox( ggDCBlock, GG_DCBLOCK, il );
	
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
		double				d1, d2, d3;
		boolean				b1;
		
		// io
		AudioFile			shapeF			= null;
		AudioFile			trajF			= null;
		AudioFile			outF			= null;
		AudioFileDescr			shapeStream		= null;
		AudioFileDescr			trajStream		= null;
		AudioFileDescr			outStream		= null;
		int					trajChanNum, outChanNum;
		int					shapeChanNum	= 0;

		int[][]				shapeChan		= null;
		int[][]				trajChan		= null;
		float[][]			shapeChanWeight	= null;
		float[][]			trajChanWeight	= null;

		// buffers
		int					shapeMem		= 0;
		float[][]			shapeBuf		= null;
		float[][]			trajBuf			= null;
		float[][]			outBuf			= null;
		float[][]			dcMem;
		float[]				convBuf1, convBuf2;

		// Smp Init
		int					trajLength, outLength;
		int					shapeLength		= 0;
		int					framesRead, framesWritten;

		int					pass, passes, inOff;
		int					passLen			= 0;

		PathField			ggOutput;

topLevel: try {

		// ---- open input, output; init ----

			// traj input
			trajF			= AudioFile.openAsRead( new File( pr.text[ PR_TRAJFILE ]));
			trajStream		= trajF.getDescr();
			trajChanNum		= trajStream.channels;
			trajLength		= (int) trajStream.length;
			// this helps to prevent errors from empty files!
			if( (trajLength < 1) || (trajChanNum < 1) ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			outChanNum		= trajChanNum;
			passes			= 1;
			shapeLength		= 0;

			// shape input
			if( pr.intg[ PR_SHAPE ] == SHAPE_FILE ) {
				shapeF			= AudioFile.openAsRead( new File( pr.text[ PR_SHAPEFILE ]));
				shapeStream		= shapeF.getDescr();
				shapeChanNum	= shapeStream.channels;
				shapeLength		= (int) shapeStream.length;
				// this helps to prevent errors from empty files!
				if( (shapeLength < 1) || (shapeChanNum < 1) ) throw new EOFException( ERR_EMPTY );

				outChanNum		= Math.max( shapeChanNum, trajChanNum );
	
				shapeChan		= new int[ outChanNum ][ 2 ];
				shapeChanWeight	= new float[ outChanNum ][ 2 ];
				trajChan		= new int[ outChanNum ][ 2 ];
				trajChanWeight	= new float[ outChanNum ][ 2 ];
	
				// calc weights
				for( ch = 0; ch < outChanNum; ch++ ) {
					if( shapeChanNum == 1 ) {
						shapeChan[ ch ][ 0 ]		= 0;
						shapeChan[ ch ][ 1 ]		= 0;
						shapeChanWeight[ ch ][ 0 ]	= 1.0f;
						shapeChanWeight[ ch ][ 1 ]	= 0.0f;
					} else {
						f1							= ((float) ch / (float) (outChanNum - 1)) * (shapeChanNum - 1);
						shapeChan[ ch ][ 0 ]		= (int) f1;
						shapeChan[ ch ][ 1 ]		= (int) f1 + 1;
						f1						   %= 1.0f;
						shapeChanWeight[ ch ][ 0 ]	= 1.0f - f1;
						shapeChanWeight[ ch ][ 1 ]	= f1;
					}
					if( trajChanNum == 1 ) {
						trajChan[ ch ][ 0 ]			= 0;
						trajChan[ ch ][ 1 ]			= 0;
						trajChanWeight[ ch ][ 0 ]	= 1.0f;
						trajChanWeight[ ch ][ 1 ]	= 0.0f;
					} else {
						f1							= ((float) ch / (float) (outChanNum - 1)) * (trajChanNum - 1);
						trajChan[ ch ][ 0 ]			= (int) f1;
						trajChan[ ch ][ 1 ]			= (int) f1 + 1;
						f1						   %= 1.0f;
						trajChanWeight[ ch ][ 0 ]	= 1.0f - f1;
						trajChanWeight[ ch ][ 1 ]	= f1;
					}
				}

				// take 75% of free memory, divide by sizeof( float ), divide by shapeChanNum
				shapeMem	= (int) ((Runtime.getRuntime().freeMemory() >> 4) * 3 / shapeChanNum);
//System.out.println( "inpMem"+ shapeMem+"; fltSize "+fltSize+"; dataLen "+dataLen+"; shapeLength "+shapeLength+"; totalIn "+totalInSamples );
//shapeMem = 1024;
//outF.setBufSize( 1 );
				if( shapeMem < 1024 ) throw new OutOfMemoryError( ERR_MEMORY );
				if( shapeMem > shapeLength ) {
					shapeMem = shapeLength;
				}
				passes	= (shapeLength + shapeMem - 1) / shapeMem;
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr( trajStream );
			ggOutput.fillStream( outStream );
			outStream.channels = outChanNum;
			outF		= AudioFile.openAsWrite( outStream );
			outLength	= trajLength;
		// .... check running ....
			if( !threadRunning ) break topLevel;

//System.out.println( passes+" passes shapeMem "+shapeMem +" frames" );

			progOff				= 0;
			progLen				= (long) trajLength * passes + shapeLength + (long) outLength * passes;

			// buffers
			if( shapeF != null ) {
				shapeBuf				= new float[ shapeChanNum+1 ][];	// +1 to avoid arrayindex-out-of-bounds
				for( ch = 0; ch < shapeChanNum; ch++ ) {
					shapeBuf[ ch ]		= new float[ shapeMem ];
				}
				shapeBuf[ shapeChanNum ]	= shapeBuf[ 0 ];	// will always be zero-weighted so content is unimportant
			}
			trajBuf				= new float[ trajChanNum+1 ][];
			for( ch = 0; ch < trajChanNum; ch++ ) {
				trajBuf[ ch ]	= new float[ 8192 ];
			}
			trajBuf[ trajChanNum ] = trajBuf[ 0 ];
			outBuf				= new float[ outChanNum ][ 8192 ];

			// DC block init
			dcMem				= new float[ outChanNum ][ 2 ];
			for( ch = 0; ch < outChanNum; ch++ ) {
				dcMem[ ch ][ 0 ]= 0.0f;
				dcMem[ ch ][ 1 ]= 0.0f;
			}
			
		// ----==================== da massacre ====================----

			for( pass = 0, inOff = 0; threadRunning && (pass < passes); pass++, inOff += shapeMem ) {

				framesWritten	= 0;
				outF.seekFrame( 0 );
				trajF.seekFrame( 0 );

			// ---- step 1: read input portion ----
				if( shapeF != null ) {
					shapeF.seekFrame( inOff );
					passLen			= Math.min( shapeLength - inOff, shapeMem );
//					inStop			= inOff + passLen;

					for( framesRead = 0; threadRunning && (framesRead < passLen); ) {
						len			= Math.min( 8192, passLen - framesRead );
						shapeF.readFrames( shapeBuf, framesRead, len );
						framesRead += len;
						progOff	   += len;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					}
				// .... check running ....
					if( !threadRunning ) break topLevel;
				}

			// ---- step 2: read traj and calc output for available input portion ----
				for( framesRead = 0; threadRunning && (framesRead < trajLength); ) {
					len			= Math.min( 8192, trajLength - framesRead );
					trajF.readFrames( trajBuf, 0, len );
					framesRead += len;
					progOff	   += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				// .... check running ....
					if( !threadRunning ) break topLevel;

				// ---------------------------- shape file -------------------------
					if( shapeF != null ) {
						// read output that was generated in the recent pass
						b1 = (pass == 0);		// always update in first pass, otherwise only if sth. changes
						if( !b1 ) {
							outF.readFrames( outBuf, 0, len );
						} else {	// all zero in first pass
							for( ch = 0; ch < outChanNum; ch++ ) {
								convBuf1 = outBuf[ ch ];
								for( i = 0; i < len; i++ ) {
									convBuf1[ i ] = 0.0f;
								}
							}
						}
	
						// update output for available input portion
						for( ch = 0; ch < outChanNum; ch++ ) {
							convBuf1 = outBuf[ ch ];
							for( i = 0; i < len; i++ ) {
								d1 = trajBuf[ trajChan[ ch ][ 0 ]][ i ] * trajChanWeight[ ch ][ 0 ] +
									 trajBuf[ trajChan[ ch ][ 1 ]][ i ] * trajChanWeight[ ch ][ 1 ];
								j  = (int) (Math.min( 1.0, Math.abs( d1 )) * (shapeLength - 1)) - inOff;
								if( (j >= 0) && (j < passLen) ) {
									f1 = shapeBuf[ shapeChan[ ch ][ 0 ]][ j ] * shapeChanWeight[ ch ][ 0 ] +
										 shapeBuf[ shapeChan[ ch ][ 1 ]][ j ] * shapeChanWeight[ ch ][ 1 ];
									convBuf1[ i ] = f1;
									b1 = true;	// needs rewrite
								}
							}
						}
	
						// write output
						if( b1 ) {
							outF.seekFrame( framesWritten );		// go back to last read pos
							outF.writeFrames( outBuf, 0, len );		//   and replace content
						}
				// ---------------------------- internal function -------------------------
					} else {
					
						for( ch = 0; ch < outChanNum; ch++ ) {
							convBuf1 = outBuf[ ch ];
							convBuf2 = trajBuf[ ch ];
							switch( pr.intg[ PR_SHAPE ]) {
							case SHAPE_SINE:						// ........ sine(x) ........
								switch( pr.intg[ PR_SIGMODE ]) {
								case SIGMODE_BICENTER:
									d2	= -Math.PI;
									break;
								case SIGMODE_UNI:
									for( i = 0; i < len; i++ ) {
										convBuf2[ i ] = Math.abs( convBuf2[ i ]);
									}
									// THRU
								case SIGMODE_BIWRAP:
								default:	// case SIGMODE_UNISIGN:
									d2	= Constants.PI2;
									break;
								}
								for( i = 0; i < len; i++ ) {
									d1 = convBuf2[ i ];
									convBuf1[ i ] = (float) Math.sin( d1 * d2 );
								}
								break;

							case SHAPE_TRI:						// ........ tri(x) ........
								switch( pr.intg[ PR_SIGMODE ]) {
								case SIGMODE_BICENTER:
									d2	= -0.5;
									break;
								case SIGMODE_UNI:
									for( i = 0; i < len; i++ ) {
										convBuf2[ i ] = Math.abs( convBuf2[ i ]);
									}
									// THRU
								case SIGMODE_BIWRAP:
								default:	// case SIGMODE_UNISIGN:
									d2	= 1.0;
									break;
								}
								for( i = 0; i < len; i++ ) {
									d1 = convBuf2[ i ];
									j  = (d1 >= 0.0) ? 2 : -2;
									d3 = (d1 * d2 * j) % 2.0;
									if( d3 < 0.5 ) {
										// d3 *= 1.0;
									} else if( d3 < 1.5 ) {
										d3 = 1.0 - d3;
									} else {
										d3 -= 2.0;
									}
									convBuf1[ i ] = (float) (d3 * j);
								}
								break;

							case SHAPE_SQRT:						// ........ sqrt(x) ........
								// XXX
								throw new IOException( "Sqrt not yet implemented!" );
								// break;
							case SHAPE_SQR:							// ........ sqr(x) ........
								switch( pr.intg[ PR_SIGMODE ]) {
								case SIGMODE_BICENTER:
									for( i = 0; i < len; i++ ) {
										d1 = convBuf2[ i ];
										convBuf1[ i ] = (float) (d1 * d1 * 2 - 1.0f);
									}
									break;
								case SIGMODE_UNI:
									for( i = 0; i < len; i++ ) {
										d1 = convBuf2[ i ];
										d1 = Math.abs( d1 + 1.0 ) / 2;
										convBuf1[ i ] = (float) (d1 * d1);
									}
									break;
								case SIGMODE_BIWRAP:
									for( i = 0; i < len; i++ ) {
										d1 = convBuf2[ i ];
										d1 = 1.0 - Math.abs( d1 ) * 2;
										convBuf1[ i ] = (float) (d1 * d1 * 2 - 1.0f);
									}
									break;
								case SIGMODE_UNISIGN:
									for( i = 0; i < len; i++ ) {
										d1 = convBuf2[ i ];
										j  = (d1 >= 0.0) ? 1 : -1;
										d1 = Math.abs( d1 + 1.0 ) / 2;
										convBuf1[ i ] = (float) (d1 * d1 * j);
									}
									break;
								}
								break;
							}

						// ---- remove DC ----					adapted from CSound
							if( pr.bool[ PR_DCBLOCK ]) {
								convBuf2 = dcMem[ ch ];
								for( i = 0; i < len; i++ ) {
									f1				= convBuf1[ i ];								// X1
									convBuf1[ i ]	= f1 - convBuf2[ 0 ] + 0.99f * convBuf2[ 1 ];	// Y1 = X1-X0+Y0*gain
									convBuf2[ 0 ]	= f1;											// next X0
									convBuf2[ 1 ]	= convBuf1[ i ];								// next Y0
								}
							}
						} // for outChanNum
						outF.writeFrames( outBuf, 0, len );		//   and replace content
					}
				// ----------------------------
					
					framesWritten += len;
					progOff		  += len;
				// .... progress ....
					setProgression( (float) progOff / (float) progLen );
				} // for trajLength
			} // for passes
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
	
			outF.close();
			outF		= null;
			outStream	= null;
			if( shapeF != null ) {
				shapeF.close();
				shapeF		= null;
				shapeStream	= null;
				shapeBuf	= null;
			}
			trajF.close();
			trajF		= null;
			trajStream	= null;
			outBuf		= null;
			trajBuf		= null;
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			shapeStream	= null;
			trajStream	= null;
			outStream	= null;
			shapeBuf	= null;
			outBuf		= null;
			trajBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));
		}

	// ---- cleanup (topLevel) ----
		if( shapeF != null ) {
			shapeF.cleanUp();
			shapeF = null;
		}
		if( trajF != null ) {
			trajF.cleanUp();
			trajF = null;
		}
		if( outF != null ) {
			outF.cleanUp();
			outF = null;
		}
	} // process()

// -------- private Methoden --------

	protected void reflectPropertyChanges()
	{
		super.reflectPropertyChanges();
	
		Component c;
		
		c = gui.getItemObj( GG_SHAPEFILE );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_SHAPE ] == SHAPE_FILE );
		}
		c = gui.getItemObj( GG_DCBLOCK );
		if( c != null ) {
			c.setEnabled( pr.intg[ PR_SHAPE ] != SHAPE_FILE );
		}
	}
}
// class SeekDestroyDlg
