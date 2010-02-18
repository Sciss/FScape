/*
 *  SachensucherDlg.java
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
 *		05-nov-04	bugfix
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  Experimental processing module that will read
 *	any kind of file and convert it to a sound file
 *	making guessings about the data format (float/ int)
 *	and wordsize. Nice for uncompressed tiff
 *	images or uncompressed movie files.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class SachensucherDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PR_OUTPUTRES			= 1;
	private static final int PR_OUTPUTRATE			= 2;
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PR_LENGTH				= 1;

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputReso";
	private static final String PRN_OUTPUTRATE		= "OutputRate";
	private static final String PRN_LENGTH			= "Length";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 0, 0 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_LENGTH };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;
	private static final int GG_GAIN			= GG_OFF_PARAMFIELD	+ PR_GAIN;
	private static final int GG_LENGTH			= GG_OFF_PARAMFIELD	+ PR_LENGTH;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public SachensucherDlg()
	{
		super( "Sachensucher" );
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
			static_pr.para[ PR_LENGTH ]				= new Param( 768.0, Param.NONE );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;

			fillDefaultAudioDescr( static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES, PR_OUTPUTRATE );
			fillDefaultGain( static_pr.para, PR_GAIN );
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField			ggInputFile, ggOutputFile;
		ParamField			ggLength;
		PathField[]			ggInputs;
		ParamField			ggGain;
		ParamSpace			spcLength;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

	// -------- Input-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "File I/O", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE, "Select input file" );
//		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Any input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD,
										 "Select output file" );
		ggOutputFile.handleTypes( GenericFile.TYPES_SOUND );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0Sonif$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, null );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		gui.registerGadget( ggOutputFile.getResGadget(), GG_OUTPUTRES );
		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );

		ggGain			= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Headroom", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addParamField( ggGain, GG_GAIN, null );
		
	// -------- Settings --------
	gui.addLabel( new GroupLabel( "Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		
		spcLength		= new ParamSpace( 12.0, 98304.0, 3.0, Param.NONE );
		ggLength		= new ParamField( spcLength );
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Window length [bytes]", SwingConstants.RIGHT ));
		con.weightx		= 0.4;
		gui.addParamField( ggLength, GG_LENGTH, null );

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
		int					i, j, k, len;
		long				progOff, progLen;
		float				f1, f2;
		
		// io
		RandomAccessFile	inF				= null;
		AudioFile			outF			= null;
		AudioFileDescr		outStream		= null;
		ByteBuffer			bb;
		FileChannel			inCh;
		int					bufSize;
		Info				info;
		float[][]			outBuf;

		// Synthesize
		float				gain;
		Param				ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz

		// Smp Init
		long				inLength;
		long				framesRead;
		long				pos, lastPos;
		float				maxAmp, lastMaxAmp, diff;
		int					chunkCount, type, lastType, offset, lastOffset;
		int[]				stat	= new int[5];
		float[]				amp		= new float[4];
		float[]				prob	= new float[4];

		PathField			ggOutput;

topLevel: try {

		// ---- open input, output; init ----

			// input
			inF				= new RandomAccessFile( pr.text[ PR_INPUTFILE ], "r" );
			inLength		= inF.length();
			inCh			= inF.getChannel();

			progOff			= 0;
			progLen			= inLength*2;
			
			bufSize			= (int) pr.para[ PR_LENGTH ].val / 3 * 3;

			// output
			ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
			if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
			outStream	= new AudioFileDescr();
			ggOutput.fillStream( outStream );
			outStream.channels = 1;
			outF		= AudioFile.openAsWrite( outStream );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			gain		= (float) ((Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val);
//			if( gain > 1.0f ) gain = 1.0f / gain;
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ----==================== kulchur ====================----

//			framesWritten	= 0L;
			framesRead		= 0L;
			
			lastType		= 0;
			lastMaxAmp		= 0.0f;
			lastPos			= 0L;
			chunkCount		= 0;
			lastOffset		= 0;
			
			info			= new Info();
//			info.outF		= outF;
			bb				= ByteBuffer.allocate( bufSize );
			info.bb			= bb;
			outBuf			= new float[1][ bb.capacity() ];
			info.fb			= outBuf[0];
			
			while( threadRunning && framesRead < inLength ) {
				// ==================== read input chunk ====================
				bb.clear();
				len			= (int) Math.min( inLength - framesRead, bufSize );
				inCh.read( bb );
				type		= 0;
				f2			= idByte( info, 0 );
				offset		= 0;
				maxAmp		= info.maxAmp;
				diff		= Float.POSITIVE_INFINITY;
				for( i = 0, j = 0; i < 2; i++ ) {
					prob[i] = idShort( info, i );
					amp[i]	= info.maxAmp;
					if( info.diff < diff ) {
						j = i;
						diff = info.diff;
					}
				}
				if( prob[j] < f2 ) {
					f2		= prob[j];
					type	= 1;
					maxAmp	= amp[j];
					offset	= j;
				}
				diff		= Float.POSITIVE_INFINITY;
				for( i = 0, j = 0; i < 3; i++ ) {
					prob[i] = idTri( info, i );
					amp[i]	= info.maxAmp;
					if( info.diff < diff ) {
						j = i;
						diff = info.diff;
					}
				}
				if( prob[j] < f2 ) {
					f2		= prob[j];
					type	= 2;
					maxAmp	= amp[j];
					offset	= j;
				}
				diff		= Float.POSITIVE_INFINITY;
				for( i = 0, j = 0; i < 4; i++ ) {
					prob[i] = idInt( info, i );
					amp[i]	= info.maxAmp;
					if( info.diff < diff ) {
						j = i;
						diff = info.diff;
					}
				}
				if( prob[j] < f2 ) {
					f2		= prob[j];
					type	= 3;
					maxAmp	= amp[j];
					offset	= j;
				}
				diff		= Float.POSITIVE_INFINITY;
				for( i = 0, j = 0; i < 4; i++ ) {
					prob[i] = idFloat( info, i );
					amp[i]	= info.maxAmp;
					if( info.diff < diff ) {
						j = i;
						diff = info.diff;
					}
				}
				if( prob[j] < f2 ) {
					f2		= prob[j];
					type	= 3;
					maxAmp	= amp[j];
					offset	= j;
				}

				if( (type != lastType) || (offset != 0) || (len < bufSize) ) {
					pos = inCh.position();
					inCh.position( lastPos );
					f1 = lastMaxAmp > 0.0f ? (gain / lastMaxAmp) : 0.0f;
					stat[lastType]++;
// System.err.println( "new type at "+pos+"; lastType = "+lastType+"; lastMaxAmp = "+lastMaxAmp+" -> gain "+f1 );
					for( i = 0; i < chunkCount; i++ ) {
						bb.clear();
						inCh.read( bb );
						j = info.fb.length;
						switch( lastType ) {
						case 0:
							copyBytes( info, lastOffset );
							break;
						case 1:
							copyShorts( info, lastOffset );
							j = (j - lastOffset) / 2;
//							j = (j - 0) / 2;
							break;
						case 2:
							copyTris( info, lastOffset );
							j = (j - lastOffset) / 3;
							break;
						case 3:
							copyInts( info, lastOffset );
							j = (j - lastOffset) / 4;
							break;
						case 4:
							copyFloats( info, lastOffset );
							j = (j - lastOffset) / 4;
							break;
						}
						for( k = 0; k < j; k++ ) {
							info.fb[k] *= f1;
						}
						outF.writeFrames( outBuf, 0, j );
						progOff       += bufSize;
					// .... progress ....
						setProgression( (float) progOff / (float) progLen );
					// .... check running ....
						if( !threadRunning ) break topLevel;
					}
					lastType	= type;
					lastPos		= pos;
					lastOffset	= offset;
					inCh.position( pos + offset );
					lastMaxAmp	= maxAmp;
					chunkCount	= 1;
				} else {
					lastMaxAmp	= Math.max( maxAmp, lastMaxAmp );
					chunkCount++;
				}
				
				framesRead += len;
				progOff	   += len;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			} // while( threadRunning && framesRead < inLength )
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- clean up, normalize ----
	
			inF.close();
			inF			= null;
			outF.close();
			outF = null;
			
			setProgression( 1.0f );

		// ---- Finish ----

			// inform about clipping/ low level
//			handleClipping( maxAmp );

			System.out.println( "# of chunks: "+stat[0]+" bytes, "+stat[1]+" shorts, "+
								stat[2]+" tris, "+stat[3]+" ints, "+stat[4]+" floats." );

		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			outStream	= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			try {
				inF.close();
			} catch( Exception e11 ) {}
		}
		if( outF != null ) {
			outF.cleanUp();
		}
	} // process()

// -------- private Methoden --------

	private float idByte( Info info, int offset )
	{
		int		num = (info.bb.capacity() - offset);
		int		i;
		float	f1	= 0.0f;
		float	f2, f3;
		double	d1	= 0.0;
		double	d2	= 0.0;
		int		mul	= 0xFF;
		
		info.maxAmp = 0.0f;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			f2	= f1;
			f1  = (float) info.bb.get() / mul;
			f3	= Math.abs( f1 );
			if( f3 > info.maxAmp ) info.maxAmp = f3;
			f1 *= f1;
			d1	+= f1;
			d2	+= Math.abs( f2 - f1 );
		}
		
//		info.energy = (float) d1;
		info.diff	= (float) d2;
		return( (float) (d2 / d1) );
	}

	private float idShort( Info info, int offset )
	{
		int		num = (info.bb.capacity() - offset) >> 1;
		int		i;
		float	f1	= 0.0f;
		float	f2;
		float	f3;
		double	d1	= 0.0;
		double	d2	= 0.0;
		int		mul	= 0xFFFF;

		info.maxAmp = 0.0f;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			f2	= f1;
			f1  = (float) info.bb.getShort() / mul;
			f3	= Math.abs( f1 );
			if( f3 > info.maxAmp ) info.maxAmp = f3;
			f1 *= f1;
			d1	+= f1;
			d2	+= Math.abs( f2 - f1 );
		}
		
//		info.energy = (float) d1;
		info.diff	= (float) d2;
		return( (float) (d2 / d1) );
	}

	private float idTri( Info info, int offset )
	{
		int		num = (info.bb.capacity() - offset) / 3;
		int		i;
		float	f1	= 0.0f;
		float	f2;
		float	f3;
		double	d1	= 0.0;
		double	d2	= 0.0;
		int		mul	= 0xFFFFFF;

		info.maxAmp = 0.0f;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			f2	= f1;
			f1  = (float) (((info.bb.get() << 16)) | ((info.bb.get() & 0xFF) << 8) | (info.bb.get() & 0xFF) )  / mul;
			f3	= Math.abs( f1 );
			if( f3 > info.maxAmp ) info.maxAmp = f3;
			f1 *= f1;
			d1	+= f1;
			d2	+= Math.abs( f2 - f1 );
		}
		
//		info.energy = (float) d1;
		info.diff	= (float) d2;
		return( (float) (d2 / d1) );
	}

	private float idInt( Info info, int offset )
	{
		int		num = (info.bb.capacity() - offset) >> 2;
		int		i;
		float	f1	= 0.0f;
		float	f2;
		float	f3;
		double	d1	= 0.0;
		double	d2	= 0.0;
		long	mul	= 0xFFFFFFFF;

		info.maxAmp = 0.0f;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			f2	= f1;
			f1  = (float) info.bb.getInt() / mul;
			f3	= Math.abs( f1 );
			if( f3 > info.maxAmp ) info.maxAmp = f3;
			f1 *= f1;
			d1	+= f1;
			d2	+= Math.abs( f2 - f1 );
		}
		
//		info.energy = (float) d1;
		info.diff	= (float) d2;
		return( (float) (d2 / d1) );
	}

	private float idFloat( Info info, int offset )
	{
		int		num = (info.bb.capacity() - offset) >> 2;
		int		i;
		float	f1	= 0.0f;
		float	f2	= 0.0f;
		float	f3;
		double	d1	= 0.0;
		double	d2	= 0.0;
		int		valid		= 0;

		info.maxAmp = 0.0f;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			f1  = info.bb.getFloat();
			f3	= Math.abs( f1 );
			f1 *= f1;
			if( f1 > -1.0e6f & f1 < 1.0e6f ) {
				if( f3 > info.maxAmp ) info.maxAmp = f3;
				f2	 = f1;
				d1	+= f1;
				d2	+= Math.abs( f2 - f1 );
				valid++;
			}
		}
//		info.energy = (float) d1;
		info.diff	= (float) d2;
		
		if( (float) valid / (float) num < 0.8f ) return Float.POSITIVE_INFINITY;

		if( d1 != 0.0 ) {
			return( (float) (d2 / d1) );
		} else return 1.0f;
	}
	
	private void copyBytes( Info info, int offset )
	throws IOException
	{
		int	num = (info.bb.capacity() - offset);
		int	mul	= 0xFF;
		int i;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			info.fb[i]  = (float) info.bb.get() / mul;
		}
	}

	private void copyShorts( Info info, int offset )
	throws IOException
	{
		int	num = (info.bb.capacity() - offset) >> 1;
		int	mul	= 0xFFFF;
		int i;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			info.fb[i]  = (float) info.bb.getShort() / mul;
		}
	}

	private void copyTris( Info info, int offset )
	throws IOException
	{
		int	num = (info.bb.capacity() - offset) / 3;
		int	mul	= 0xFFFFFF;
		int i;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			info.fb[i]  = (float) (((info.bb.get() << 16)) | ((info.bb.get() & 0xFF) << 8) | (info.bb.get() & 0xFF) ) / mul;
		}
	}

	private void copyInts( Info info, int offset )
	throws IOException
	{
		int	num = (info.bb.capacity() - offset) >> 2;
		long mul = 0xFFFFFFFF;
		int i;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			info.fb[i]  = (float) info.bb.getInt() / mul;
		}
	}

	private void copyFloats( Info info, int offset )
	throws IOException
	{
		int	num = (info.bb.capacity() - offset) >> 2;
		int i;
		info.bb.clear();
		info.bb.position( offset );
		for( i = 0; i < num; i++ ) {
			info.fb[i]  = info.bb.getFloat();
		}
	}

	protected class Info
	{
		private	ByteBuffer	bb;
//		private AudioFile	outF;
		private float		maxAmp;
		private float[]		fb;
//		private float		energy;
		private float		diff;
	}
} // class SachensucherDlg