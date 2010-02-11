/*
 *  TrajectoryDlg.java
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

/**
 *	Playing around trying to generate
 *	images from sound, was never finished.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class TrajectoryDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE1		= 0;		// pr.text
	private static final int PR_INPUTFILE2		= 1;
	private static final int PR_OUTPUTFILE		= 2;
	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
	private static final int PR_OUTPUTRES		= 1;
	private static final int PR_GAINTYPE		= 2;
	private static final int PR_ZOOM			= 3;
	private static final int PR_DECAY			= 4;
	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_LIMITTHRESH		= 1;
	private static final int PR_IMGWIDTH		= 2;
	private static final int PR_IMGHEIGHT		= 3;
	private static final int PR_LENGTH			= 4;
	private static final int PR_OFFSET			= 5;

	private static final String PRN_INPUTFILE1	= "InputFile1";
	private static final String PRN_INPUTFILE2	= "InputFile2";
	private static final String PRN_OUTPUTFILE	= "OutputFile";
	private static final String PRN_OUTPUTTYPE	= "OutputType";
	private static final String PRN_OUTPUTRES	= "OutputReso";
	private static final String PRN_LIMITTHRESH	= "LimitTresh";
	private static final String PRN_IMGWIDTH	= "ImgWidth";
	private static final String PRN_IMGHEIGHT	= "ImgHeight";
	private static final String PRN_LENGTH		= "Length";
	private static final String PRN_OFFSET		= "Offset";
	private static final String PRN_ZOOM		= "Zoom";
	private static final String PRN_DECAY		= "Decay";

	private static final String	prText[]		= { "", "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE1, PRN_INPUTFILE2, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY, 2, 3 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE, PRN_ZOOM, PRN_DECAY };
	private static final Param	prPara[]		= { null, null, null, null, null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_LIMITTHRESH, PRN_IMGWIDTH, PRN_IMGHEIGHT, PRN_LENGTH, PRN_OFFSET };

	private static final int GG_INPUTFILE1		= GG_OFF_PATHFIELD	+ PR_INPUTFILE1;
	private static final int GG_INPUTFILE2		= GG_OFF_PATHFIELD	+ PR_INPUTFILE2;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_GAINTYPE		= GG_OFF_CHOICE		+ PR_GAINTYPE;
	private static final int GG_ZOOM			= GG_OFF_CHOICE		+ PR_ZOOM;
	private static final int GG_DECAY			= GG_OFF_CHOICE		+ PR_DECAY;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_LIMITTHRESH		= GG_OFF_PARAMFIELD	+ PR_LIMITTHRESH;
	private static final int GG_IMGWIDTH		= GG_OFF_PARAMFIELD	+ PR_IMGWIDTH;
	private static final int GG_IMGHEIGHT		= GG_OFF_PARAMFIELD	+ PR_IMGHEIGHT;
	private static final int GG_LENGTH			= GG_OFF_PARAMFIELD	+ PR_LENGTH;
	private static final int GG_OFFSET			= GG_OFF_PARAMFIELD	+ PR_OFFSET;
	
	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public TrajectoryDlg()
	{
		super( "Trajectory Image Generator" );
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
//			static_pr.bool		= prBool;
//			static_pr.boolName	= prBoolName;
			static_pr.para		= prPara;
			static_pr.para[ PR_LIMITTHRESH ]= new Param(   0.0, Param.DECIBEL_AMP );
			static_pr.para[ PR_IMGWIDTH ]	= new Param( 360.0, Param.NONE );
			static_pr.para[ PR_IMGHEIGHT ]	= new Param( 284.0, Param.NONE );
			static_pr.para[ PR_LENGTH ]		= new Param( 100.0, Param.FACTOR_TIME );
			static_pr.para[ PR_OFFSET ]		= new Param(   0.0, Param.OFFSET_MS );
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

		PathField			ggInputFile1, ggInputFile2, ggOutputFile;
		Component[]			ggGain;
		PathField[]			ggInputs;
		ParamField			ggLimitThresh, ggImgWidth, ggImgHeight, ggLength, ggOffset;
		ParamSpace			spcLimitThresh, spcImgDim;
		ParamSpace[]		spcOffset, spcLength;
		JComboBox			ggZoom, ggDecay;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Waveform I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile1	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile1.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file 1", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile1, GG_INPUTFILE1, null );

		ggInputFile2	= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile2.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file 2", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile2, GG_INPUTFILE2, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD, "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_IMAGE );
		ggInputs		= new PathField[ 2 ];
		ggInputs[ 0 ]	= ggInputFile1;
		ggInputs[ 1 ]	= ggInputFile2;
		ggOutputFile.deriveFrom( ggInputs, "$D0$B0Trj$B1$E" );
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
		gui.addChoice( (JComboBox) ggGain[ 1 ], GG_GAINTYPE, null );

		spcLimitThresh	= new ParamSpace( Constants.minDecibel, 0.0, 0.1, Param.DECIBEL_AMP );
		ggLimitThresh	= new ParamField( spcLimitThresh );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Limit.thresh.", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLimitThresh, GG_LIMITTHRESH, null );

	// -------- Settings-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Trajectory Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		spcImgDim		= new ParamSpace( 1.0, 32768.0, 1.0, Param.NONE );
		ggImgWidth		= new ParamField( spcImgDim );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Width [pixels]", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		gui.addParamField( ggImgWidth, GG_IMGWIDTH, null );
			
		ggImgHeight		= new ParamField( spcImgDim );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Height [pixels]", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggImgHeight, GG_IMGHEIGHT, null );

		spcOffset		= new ParamSpace[ 3 ];
		spcOffset[0]	= Constants.spaces[ Constants.offsetMsSpace ];
		spcOffset[1]	= Constants.spaces[ Constants.offsetBeatsSpace ];
		spcOffset[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength		= new ParamSpace[ 4 ];
		spcLength[0]	= Constants.spaces[ Constants.absMsSpace ];
		spcLength[1]	= Constants.spaces[ Constants.absBeatsSpace ];
		spcLength[2]	= Constants.spaces[ Constants.offsetTimeSpace ];
		spcLength[3]	= Constants.spaces[ Constants.factorTimeSpace ];

		ggOffset		= new ParamField( spcOffset );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Traj. Offset", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		gui.addParamField( ggOffset, GG_OFFSET, null );

		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Traj. Length", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggLength, GG_LENGTH, null );

		ggZoom			= new JComboBox();
		for( int i = 0; i < 4; i++ ) {
			ggZoom.addItem( (1 << i) + "x" );
		}
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Zoom factor", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		gui.addChoice( ggZoom, GG_ZOOM, null );

		ggDecay			= new JComboBox();
		for( int i = -3; i <= 3; i++ ) {
			ggDecay.addItem( String.valueOf( Math.pow( 2, i )));
		}
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Decay", SwingConstants.RIGHT ));
		con.weightx		= 0.233;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggDecay, GG_DECAY, null );

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
		int					h, i, j, k, m, n, p;
		int					len, ch;
		long				progOff, progLen;
		float				f1;
		
		// io
		AudioFile			sndF1			= null;
		AudioFile			sndF2			= null;
		ImageFile			imgF			= null;
		AudioFileDescr			sndStream1		= null;
		AudioFileDescr			sndStream2		= null;
		ImageStream			imgStream		= null;
		float[][]			inBuf1, inBuf2;
		float[]				convBuf1, convBuf2;
		byte[]				row				= null;						// raw image (RGB interleaved)
		float[][]			imgBuf			= null;						// full image
		
		// Synthesize
		float				gain			= 1.0f;			// gain abs amp
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		int					inLength, inChanNum1, inChanNum2;
		int					framesRead;
		int					width, height, frameSize;
		int					bitsPerSmp		= 8;
		int					outChanNum		= 1;	// XXX
		
		float				maxAmp			= 0.0f;

		PathField			ggOutput;
		
		int					trajLen;
		float				cx, cy, rx, ry, tx, ty, wx1, wy1, wx2, wy2;
		int					txi, tyi;
		float				limit, compr, over;
		int					zoom			= 1 << pr.intg[ PR_ZOOM ];
		float				decay			= (float) Math.pow( 2, pr.intg[ PR_DECAY ] - 3 );

topLevel: try {
		// ---- open input, output; init ----

			// input
			sndF1			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE1 ]));
			sndStream1		= sndF1.getDescr();
			inChanNum1		= sndStream1.channels;
			inLength		= (int) sndStream1.length;
			if( (inLength * inChanNum1) < 1 ) throw new EOFException( ERR_EMPTY );
			sndF2			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE2 ]));
			sndStream2		= sndF2.getDescr();
			inChanNum2		= sndStream2.channels;
			inLength		= (int) Math.min( inLength, sndStream2.length );
			if( (inLength * inChanNum2) < 1 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// output
			width			= (int) pr.para[ PR_IMGWIDTH ].val;
			height			= (int) pr.para[ PR_IMGHEIGHT ].val;
			frameSize		= width * height;
			rx				= (float) (width - 1) / 2;
			ry				= (float) (height - 1) / 2;
			cx				= rx;
			cy				= ry;

			rx 			   *= zoom / (float) Constants.ln2;
			ry			   *= zoom / (float) Constants.ln2;

			j				= Math.max( 0, Math.min( inLength - 1, (int) (AudioFileDescr.millisToSamples( sndStream1,
									 (Param.transform( pr.para[ PR_OFFSET ], Param.ABS_MS,
									  new Param( 0.0, Param.ABS_MS ), null )).val ) + 0.5)));
			sndF1.seekFrame( j );
			sndF2.seekFrame( j );

			trajLen			= Math.min( inLength - j, (int) (AudioFileDescr.millisToSamples( sndStream1,
									 (Param.transform( pr.para[ PR_LENGTH ], Param.ABS_MS,
									  new Param( AudioFileDescr.samplesToMillis( sndStream1, inLength ), Param.ABS_MS ),
									  null )).val ) + 0.5) );

			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			imgF					= new ImageFile( pr.text[ PR_OUTPUTFILE ], GenericFile.MODE_OUTPUT | ggOutput.getType() );
			imgStream				= new ImageStream();
			imgStream.bitsPerSmp	= 8;	// ??? fillStream might not work correctly?
			ggOutput.fillStream( imgStream );
			imgStream.width			= width;
			imgStream.height		= height;
			imgStream.smpPerPixel	= outChanNum;
			bitsPerSmp				= imgStream.bitsPerSmp;
			imgF.initWriter( imgStream );
			row						= imgF.allocRow();

			imgBuf					= new float[ outChanNum ][ frameSize + 1];
			inBuf1					= new float[ inChanNum1 ][ 8192 ];
			inBuf2					= new float[ inChanNum2 ][ 8192 ];
//			Util.clear( imgBuf );

		// .... check running ....
			if( !threadRunning ) break topLevel;

			progOff			= 0;
			progLen			= (long) trajLen * 2 + height;

			// normalization requires temp files
			if( pr.intg[ PR_GAINTYPE ] != GAIN_UNITY ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}

			limit = (float) (Param.transform( pr.para[ PR_LIMITTHRESH ], Param.ABS_AMP, ampRef, null )).val;
			if( limit < 1.0f ) {
				compr = Math.min( 1.0f, (1.0f - limit) / (1.0f / limit - limit) );
				gain /= limit;
			} else {
				compr = 0.0f;
			}

		// =================== CORE ===================

			framesRead		= 0;

			convBuf1		= inBuf1[0];
			convBuf2		= inBuf2[0];

			k				= 0;

//System.out.println( decay );

			while( threadRunning && framesRead < trajLen ) {

				len				= Math.min( 8192, trajLen - framesRead );
				sndF1.readFrames( inBuf1, 0, len );				
				sndF2.readFrames( inBuf2, 0, len );				
				progOff			+= len;
				framesRead		+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;
	
trajLp:			for( i = 0; i < len; i++, k++ ) {
					
					f1	= Math.min( 1.0f, Math.max( -1.0f, convBuf1[ i ]));
					j	= f1 < 0f ? -1 : 1;
//					tx	= j * (float) Math.pow( f1 * f1, 0.25 ) * rx + cx;
//					tx	= j * (float) Math.sqrt( Math.log( Math.abs( f1 ) + 1)) * rx + cx;
					tx	= j * (float) Math.log( Math.abs( f1 ) + 1) * rx + cx;
					
					f1	= Math.min( 1.0f, Math.max( -1.0f, convBuf2[ i ]));
					j	= f1 < 0f ? -1 : 1;
//					ty	= j * (float) Math.pow( f1 * f1, 0.25 ) * ry + cy;
//					ty	= j * (float) Math.sqrt( Math.log( Math.abs( f1 ) + 1)) * ry + cy;
					ty	= j * (float) Math.log( Math.abs( f1 ) + 1) * ry + cy;
					
					wx2	= tx % 1.0f;
					wy2	= ty % 1.0f;
					wx1	= 1.0f - wx2;
					wy1 = 1.0f - wy2;
					txi	= (int) tx;
					tyi = (int) ty;
					
					if( (tx < 0) || (txi > width - 2) || (ty < 0) || (tyi > height - 2) ) continue trajLp;
					
					tyi *= width;
					
					f1	=  (float) Math.pow( (float) k / (float) trajLen, decay );
					wx1	*= f1;
					wx2	*= f1;
					
					imgBuf[ 0 ][ txi   + tyi ]           += wx1 * wy1;
					imgBuf[ 0 ][ txi+1 + tyi ]           += wx2 * wy1;
					imgBuf[ 0 ][ txi   + tyi+width ]	 += wx1 * wy2;
					imgBuf[ 0 ][ txi+1 + tyi+width ]	 += wx2 * wy2;
				}
				progOff			+= len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;
			}

			sndF1.close();
			sndF1		= null;
			sndF2.close();
			sndF2		= null;

			for( ch = 0; ch < outChanNum; ch++ ) {
				for( i = 0; i < frameSize; i++ ) {
					f1 = imgBuf[ ch ][ i ];
					if( f1 > maxAmp ) {
						maxAmp = f1;
					}
				}
			}

			if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, new Param( 1 / maxAmp, Param.ABS_AMP ), null )).val;
				if( limit < 1.0f ) {
					gain /= limit;
				}
			}

			// ============================== gain ==============================

			for( ch = 0; ch < outChanNum; ch++ ) {
				convBuf1	= imgBuf[ ch ];
				if( gain != 1.0f ) {
					Util.mult( imgBuf[ ch ], 0, frameSize, gain );
				}
				for( i = 0, n = 0; i < height; i++, n += width ) {
					for( j = 0, k = n; j < width; j++, k++ ) {
						f1		= convBuf1[ k ];
						over	= f1 - limit;
						if( over > 0.0f ) {
							f1 = limit + over * compr;
						}
						convBuf1[ k ] = f1;
					}
				}
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

			// ============================== write image ==============================

			for( i = 0, n = 0; threadRunning && (i < height); i++, n += width ) {
				for( ch = 0; ch < outChanNum; ch++ ) {
				// ---------- interlace raw image ----------
					convBuf1 = imgBuf[ ch ];
		
					if( bitsPerSmp == 8 ) {			// interleave, scale to integers
						for( j = 0, m = n, k = ch; j < width; j++, m++, k += outChanNum ) {
							row[ k ]	= (byte) (convBuf1[ m ] * 0xFF);
//							row[ k ]	= (byte) ((convBuf1[ m ] + 1.0f) * 0x80);
						}
					} else {	// 16 bit
						for( j = 0, m = n, k = ch; j < width; j++, m++, k += outChanNum ) {
							h				= k << 1;
							p				= (int) (convBuf1[ m ] * 0xFFFF);
//							p				= (int) ((convBuf1[ m ] + 1.0f) * 0x8000);
							row[ h++ ]		= (byte) (p >> 8);
							row[ h ]		= (byte) p;
						}
					}
				} // for channels
				imgF.writeRow( row );
		
				progOff++;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // for rows
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- cleanup ----

			imgF.close();
			imgF	= null;

			// inform about clipping/ low level
//			maxAmp		*= gain;
//			handleClipping( maxAmp );
		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			sndStream1	= null;
			sndStream2	= null;
			imgStream	= null;
			inBuf1		= null;
			inBuf2		= null;
			imgBuf		= null;
			convBuf1	= null;
			convBuf2	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( sndF1 != null ) {
			sndF1.cleanUp();
		}
		if( sndF2 != null ) {
			sndF2.cleanUp();
		}
		if( imgF != null ) {
			imgF.cleanUp();
		}
	} // process()

// -------- private Methoden --------
}
// class TrajectoryDlg
