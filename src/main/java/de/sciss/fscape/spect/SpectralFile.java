/*
 *  SpectralFile.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.spect;

import de.sciss.fscape.io.*;

import java.io.*;

/**
 *  @version	0.71, 15-Nov-07
 */
public class SpectralFile
extends GenericFile
{
// -------- public Variablen --------

	/**
	 *	OR this to remove DC offset
	 */
	public static final int MODE_REMOVEDC	= 0x010000;

	// values for freqFormat field
	public static final int PVA_LIN		= 1;		// linearly spaced frequency bins
	public static final int PVA_EXP		= 2;		// exponentially spaced frequency bins

	// values for frameFormat field
	public static final int PVA_MAG		= 1;		// magnitude only
	public static final int PVA_PHASE	= 2;		// phase only (!)
	public static final int PVA_POLAR	= 3;		// mag, phase pairs
	public static final int PVA_REAL	= 4; 		// real only
	public static final int PVA_IMAG	= 5;		// imaginary only
	public static final int PVA_RECT	= 6; 		// real, imag pairs
	public static final int PVA_PVOC	= 7;		// weirdo mag, phi-dot format for phase vocoder
	public static final int PVA_CQ		= 32;		// ORed with previous to indicate one frame per 8ve

	public static final String ERR_ILLEGALFREQFORMAT	= "Unsupported frequency format";
	public static final String ERR_ILLEGALFRAMEFORMAT	= "Unsupported frame format";

// -------- private Variablen --------

	private int hdr[];

	// hdr[]-Aufbau; adapted from SpectralAssistant by Tom Erbe
	protected static final int hdr_magic		= 0;	// CSA_MAGIC or SHA_MAGIC to identify
	protected static final int hdr_headBsize	= 1;	// byte offset from start to data
	protected static final int hdr_dataBsize	= 2;	// number of bytes of data
	protected static final int hdr_dataFormat	= 3;	// format specifier
	protected static final int hdr_smpRate		= 4;	// FLOAT
	protected static final int hdr_chanNum		= 5;
	protected static final int hdr_frameSize	= 6;	// size of FFT frames (2^n)
	protected static final int hdr_smpPerFrame	= 7;	// number of new samples each frame (overlap)
	protected static final int hdr_frameBsize	= 8;	// bytes in each file frame, (don't trust this value,
    													// use (frameSize + 2) * sizeof(float) instead
	protected static final int hdr_frameFormat	= 9;	// how words are org'd in frms
	protected static final int hdr_loFreq		= 10;	// FLOAT  freq in Hz of lowest bin (exists)
	protected static final int hdr_hiFreq		= 11;	// FLOAT  freq in Hz of highest (or next)
	protected static final int hdr_freqMode		= 12;	// flag for log/lin frq
	protected static final int hdr_info			= 13;	// ?
	protected static final int hdr_sizeof		= 14;

	protected static final int CSA_UNK_LEN		= -1;			// flag if dataBsize unknown in hdr
	// values for dataFormat field
	protected static final int PVA_SHORT		= 2;		// 16 bit linear data
	protected static final int PVA_LONG			= 4;		// 32 bit linear data
	protected static final int PVA_FLOAT		= (4+32);	// 32 bit float data
	protected static final int PVA_DOUBLE		= (8+32);	// 64 bit float data

	protected int bands;	// frameSize / datanum + 1; where datanum = 2 for polar+rect, else 1
	protected long frames;	// total number
	protected int dataOffset;	// MAG, POLAR, RECT = 0, PHASE = 1
	protected int dataNum;		// MAG, PHASE = 1, POLAR, RECT = 2

	protected SpectStream	stream	= null;
	protected byte			buffer[];
	
// -------- public Methoden --------

	/**
	 *	Datei, die spektrale Daten enthaelt bzw. enthalten soll
	 *
	 *	@param spectF	entsprechende Datei
	 *	@param mode		MODE_INPUT zum Lesen, MODE_OUTPUT zum Schreiben
	 */
	public SpectralFile( File spectF, int mode )
	throws IOException
	{
		super( spectF, (mode & ~MODE_TYPEMASK) | MODE_SPECT );
		hdr	= new int[ hdr_sizeof ];
	}
	
	public SpectralFile( String fname, int mode )
	throws IOException
	{
		this( new File( fname ), mode );
	}

	/**
	 *	erzeugt einen SpectStream, in den das File eingelesen werden kann;
	 *	der Stream ist "passiv", es werden keine Writer/Reader eingetragen;
	 *	er dient nur zum Auslesen des Headers fuer den Operator;
	 *	DENNOCH KEINE AENDERUNGEN MEHR AM SPECTSTREAM VORNEHMEN!
	 */
	public SpectStream getDescr()
	throws IOException
	{
		float hiFreq, smpRate;

		if( stream == null ) {
			stream = new SpectStream();
			readHeader();

// SND HACK BUG FIX XXXX
			smpRate	= Float.intBitsToFloat( hdr[ hdr_smpRate ]);
			hiFreq	= Float.intBitsToFloat( hdr[ hdr_hiFreq ]);
			if( hiFreq > smpRate/2 ) {		// SndHck writes "Infinity" sometimes
				hiFreq = smpRate/2;
			}

			stream.setChannels( hdr[ hdr_chanNum ]);
			stream.setBands( Float.intBitsToFloat( hdr[ hdr_loFreq ]), hiFreq, bands, hdr[ hdr_freqMode ] );
			stream.setRate( smpRate, hdr[ hdr_smpPerFrame ] );
			stream.setEstimatedLength( frames );

			buffer = new byte[ (stream.bands * dataNum * stream.chanNum) << 2 ];
		}
		seekFrame( 0 );
		return stream;
	}

	/**
	 *	Meldet einen SpectStream zum Schreiben in das File an;
	 *	der Stream bleibt "passiv", vgl. getDescr()!
	 *
	 *	@param	format	PVA_MAG, PVA_PHASE oder PVA_POLAR
	 */
	public void initWriter( SpectStream strm, int format )
	throws IOException
	{	
		dataNum		= (format == PVA_POLAR) ? 2 : 1;
		dataOffset	= (format == PVA_PHASE) ? 1 : 0;
		bands		= strm.bands;
		frames		= strm.frames;

		hdr[ hdr_magic ]		= SHA_MAGIC;
		hdr[ hdr_headBsize ]	= hdr_sizeof << 2;
		hdr[ hdr_dataBsize ]	= CSA_UNK_LEN;		// am Schluss korrigiert
		hdr[ hdr_dataFormat ]	= PVA_FLOAT;
		hdr[ hdr_smpRate ]		= Float.floatToIntBits( strm.smpRate );
		hdr[ hdr_chanNum ]		= strm.chanNum;
		hdr[ hdr_frameSize ]	= (strm.bands - 1) * dataNum;
		hdr[ hdr_smpPerFrame ]	= strm.smpPerFrame;
		hdr[ hdr_frameBsize ]	= hdr[ hdr_frameSize ] << 1;		// correct? XXX
		hdr[ hdr_frameFormat ]	= format;
		hdr[ hdr_loFreq ]		= Float.floatToIntBits( strm.loFreq );
		hdr[ hdr_hiFreq ]		= Float.floatToIntBits( strm.hiFreq );
		hdr[ hdr_freqMode ]		= strm.freqMode;
		hdr[ hdr_info ]			= 0x46534350;			// 'FSCP'

		for( int i = 0; i < hdr.length; i++ ) {
			writeInt( hdr[ i ]);
		}

// SND HACK BUG FIX XXXX
if( hdr[ hdr_chanNum ] == 2 ) {
	byte[] foo = new byte[ (strm.bands * dataNum) << 2 ];
	for( int i = 0; i < foo.length; i++ ) foo[ i ] = 0;
	write( foo );
}
		
		buffer = new byte[ (strm.bands * dataNum * strm.chanNum) << 2 ];
		this.stream = strm;
	}

	/**
	 *	Springt zu einem bestimmten Frame
	 *	(nachfolgende readFrame() und writeFrame()s sind betroffen)
	 */
	public void seekFrame( long frame )
	throws IOException
	{
		seek( hdr[ hdr_headBsize ] + frame * hdr[ hdr_frameBsize ] * hdr[ hdr_chanNum ]);
	}
	
	/**
	 *	Liest einen Frame aus der Datei ein
	 */
	public SpectFrame readFrame()
	throws IOException
	{
		int i, j, k;
	
		SpectFrame fr = stream.allocFrame();
	
		// frame.length * dataNum/2 * sizeof(data)
		readFully( buffer, 0,
				   fr.data.length * (fr.data[ 0 ].length >> 1) * dataNum * (hdr[ hdr_dataFormat ] & 0x0F) );

		switch( hdr[ hdr_dataFormat ] ) {
		case PVA_FLOAT:							// -------------------------- FLOAT ---------------------
			for( i = 0, k = 0; i < fr.data.length; i++ ) {	// ch
				for( j = 0; j < fr.data[ i ].length; j += 2 ) {
					if( dataOffset == 0 ) {
						fr.data[ i ][ j + SpectFrame.AMP ] =
								Float.intBitsToFloat( ((((int) buffer[ k   ]) & 0xFF) << 24) |
													  ((((int) buffer[ k+1 ]) & 0xFF) << 16) |
													  ((((int) buffer[ k+2 ]) & 0xFF) <<  8) |
													   (((int) buffer[ k+3 ]) & 0xFF) );
						k += 4;
					} else {
						fr.data[ i ][ j + SpectFrame.AMP ] = 0.0f;
					}
					if( dataOffset + dataNum == 2 ) {
						fr.data[ i ][ j + SpectFrame.PHASE ] =
								Float.intBitsToFloat( ((((int) buffer[ k   ]) & 0xFF) << 24) |
													  ((((int) buffer[ k+1 ]) & 0xFF) << 16) |
													  ((((int) buffer[ k+2 ]) & 0xFF) <<  8) |
													   (((int) buffer[ k+3 ]) & 0xFF) );
						k += 4;
					} else {
						fr.data[ i ][ j + SpectFrame.PHASE ] = 0.0f;
					}
				}
			}
			break;
			
		case PVA_LONG:							// -------------------------- LONG ---------------------
			for( i = 0, k = 0; i < fr.data.length; i++ ) {	// ch
				for( j = 0; j < fr.data[ i ].length; j += 2 ) {
					if( dataOffset == 0 ) {
						fr.data[ i ][ j + SpectFrame.AMP ]  =
								(float) (((((int) buffer[ k   ]) & 0xFF) << 24) |
								  		 ((((int) buffer[ k+1 ]) & 0xFF) << 16) |
								  		 ((((int) buffer[ k+2 ]) & 0xFF) <<  8) |
										  (((int) buffer[ k+3 ]) & 0xFF));
						k += 4;
					} else {
						fr.data[ i ][ j + SpectFrame.AMP ] = 0.0f;
					}
					if( dataOffset + dataNum == 2 ) {
						fr.data[ i ][ j + SpectFrame.PHASE ] =
								(float) (((((int) buffer[ k   ]) & 0xFF) << 24) |
								 	     ((((int) buffer[ k+1 ]) & 0xFF) << 16) |
								   		 ((((int) buffer[ k+2 ]) & 0xFF) <<  8) |
										  (((int) buffer[ k+3 ]) & 0xFF));
						k += 4;
					} else {
						fr.data[ i ][ j + SpectFrame.PHASE ] = 0.0f;
					}
				}
			}
			break;

		case PVA_DOUBLE:						// -------------------------- DOUBLE ---------------------
			for( i = 0, k = 0; i < fr.data.length; i++ ) {	// ch
				for( j = 0; j < fr.data[ i ].length; j += 2 ) {
					if( dataOffset == 0 ) {
						fr.data[ i ][ j + SpectFrame.AMP ]  = (float)
								   Double.longBitsToDouble ( ((((long) buffer[ k   ]) & 0xFF) << 56) |
															 ((((long) buffer[ k+1 ]) & 0xFF) << 48) |
															 ((((long) buffer[ k+2 ]) & 0xFF) << 40) |
															 ((((long) buffer[ k+3 ]) & 0xFF) << 32) |
															 ((((long) buffer[ k+4 ]) & 0xFF) << 24) |
															 ((((long) buffer[ k+5 ]) & 0xFF) << 16) |
															 ((((long) buffer[ k+6 ]) & 0xFF) <<  8) |
															  (((long) buffer[ k+7 ]) & 0xFF) );
						k += 8;
					} else {
						fr.data[ i ][ j + SpectFrame.AMP ] = 0.0f;
					}
					if( dataOffset + dataNum == 2 ) {
						fr.data[ i ][ j + SpectFrame.PHASE ]= (float)
								   Double.longBitsToDouble ( ((((long) buffer[ k   ]) & 0xFF) << 56) |
															 ((((long) buffer[ k+1 ]) & 0xFF) << 48) |
															 ((((long) buffer[ k+2 ]) & 0xFF) << 40) |
															 ((((long) buffer[ k+3 ]) & 0xFF) << 32) |
															 ((((long) buffer[ k+4 ]) & 0xFF) << 24) |
															 ((((long) buffer[ k+5 ]) & 0xFF) << 16) |
															 ((((long) buffer[ k+6 ]) & 0xFF) <<  8) |
															  (((long) buffer[ k+7 ]) & 0xFF) );
						k += 8;
					} else {
						fr.data[ i ][ j + SpectFrame.PHASE ] = 0.0f;
					}
				}
			}
			break;

		case PVA_SHORT:						// -------------------------- SHORT ---------------------
			for( i = 0, k = 0; i < fr.data.length; i++ ) {	// ch
				for( j = 0; j < fr.data[ i ].length; j += 2 ) {
					if( dataOffset == 0 ) {
						fr.data[ i ][ j + SpectFrame.AMP ] =
								(float) (((((int) buffer[ k   ]) & 0xFF) << 8) |
										  (((int) buffer[ k+1 ]) & 0xFF));
						k += 2;
					} else {
						fr.data[ i ][ j + SpectFrame.AMP ] = 0.0f;
					}
					if( dataOffset + dataNum == 2 ) {
						fr.data[ i ][ j + SpectFrame.PHASE ] =
								(float) (((((int) buffer[ k   ]) & 0xFF) << 8) |
										  (((int) buffer[ k+1 ]) & 0xFF));
						k += 2;
					} else {
						fr.data[ i ][ j + SpectFrame.PHASE ] = 0.0f;
					}
				}
			}
			break;

		default:
			break;
		}

		// Rechteck in Polar-Format konviertieren
		if( hdr[ hdr_frameFormat ] == PVA_RECT ) {

			float srcReal, srcImg;

			for( i = 0; i < fr.data.length; i++ ) {	// ch
				for( j = 0; j < fr.data[ i ].length; j += 2 ) {
					srcReal	= fr.data[ i ][ j + SpectFrame.AMP ];
					srcImg	= fr.data[ i ][ j + SpectFrame.PHASE ];
					fr.data[ i ][ j + SpectFrame.AMP ]		= (float)
							Math.sqrt( srcReal*srcReal + srcImg*srcImg );
					fr.data[ i ][ j + SpectFrame.PHASE ]	= (float)
							Math.atan2( srcImg, srcReal );
				}
			}
		}
		
		// lockere Auslegung von DC-Offset: alles unter 10 Hz wegschneiden
		if( ((mode & MODE_REMOVEDC) != 0) && (stream.loFreq < 10.0f) ) {
		
			float freqSpacing = (stream.hiFreq - stream.loFreq) / stream.bands;
		
			for( i = 0; i * freqSpacing + stream.loFreq < 10.0f; i++ ) {
				for( j = 0; j < fr.data.length; j++ ) {	// ch
					fr.data[ j ][ (i << 1) + SpectFrame.AMP ]	=	0.0f;
					fr.data[ j ][ (i << 1) + SpectFrame.PHASE ]	=	0.0f;
				}
			}
		}
		
		return fr;
	}

	/**
	 *	Schreibt einen Frame in die Datei
	 */
	public void writeFrame( SpectFrame fr )
	throws IOException
	{
		int i, j, k, l;

		for( i = 0, k = 0; i < fr.data.length; i++ ) {	// ch
			for( j = 0; j < fr.data[ i ].length; j += 2 ) {
				if( dataOffset == 0 ) {
					l = Float.floatToIntBits( fr.data[ i ][ j + SpectFrame.AMP ]);
					buffer[ k   ] = (byte)  ((l        & 0xFF000000) >> 24);
					buffer[ k+1 ] = (byte) (((l <<  8) & 0xFF000000) >> 24);
					buffer[ k+2 ] = (byte) (((l << 16) & 0xFF000000) >> 24);
					buffer[ k+3 ] = (byte)  ((l << 24)               >> 24);
					k += 4;
				}
				if( dataOffset + dataNum == 2 ) {
					l = Float.floatToIntBits( fr.data[ i ][ j + SpectFrame.PHASE ]);
					buffer[ k   ] = (byte)  ((l        & 0xFF000000) >> 24);
					buffer[ k+1 ] = (byte) (((l <<  8) & 0xFF000000) >> 24);
					buffer[ k+2 ] = (byte) (((l << 16) & 0xFF000000) >> 24);
					buffer[ k+3 ] = (byte)  ((l << 24)               >> 24);
					k += 4;
				}
			}
		}

		// lockere Auslegung von DC-Offset: alles unter 10 Hz wegschneiden
		if( ((mode & MODE_REMOVEDC) != 0) && (stream.loFreq < 10.0f) ) {
		
			float freqSpacing = (stream.hiFreq - stream.loFreq) / stream.bands;
		
			for( i = 0, k = 0; i < stream.chanNum; i++ ) {	// ch
				for( j = 0, l = k; j * freqSpacing + stream.loFreq < 10.0f; j++ ) {
					buffer[ l++ ]	= 0;
					buffer[ l++ ]	= 0;
					if( dataNum == 2 ) {
						buffer[ l++ ]	= 0;
						buffer[ l++ ]	= 0;
					}
				}
				k += stream.bands * (dataNum << 1);		// next channel
			}
		}

		// frame.length * dataNum/2 * sizeof(data)
		write( buffer, 0, fr.data.length * (fr.data[ 0 ].length << 1) * dataNum );
	}

	/**
	 *	Erzeugt ein fuer diese Objekt zum Schreiben geeignetes Frame
	 */
	public SpectFrame allocFrame()
	{
		return stream.allocFrame();
	}

	/**
	 *	Gibt einen ueber dieses Objekt erzeugtes Frame frei
	 */
	public void freeFrame( SpectFrame fr )
	{
		stream.freeFrame( fr );
	}

	/**
	 *	Datei schliessen
	 */
	public void close()
	throws IOException
	{
		stream = null;

		if( (mode & MODE_FILEMASK) == MODE_OUTPUT ) {
			try {
				if( length() >= (hdr_sizeof << 2) ) {	// haben wir schon was geschrieben? (= gueltiger hdr)
					seek( hdr_dataBsize << 2 );			// ...dann update file length
					writeInt( (int) length() - hdr[ hdr_headBsize ]);
				}
			}
			catch( IOException e1 ) {}		// egal, man kann auch mit CSA_UNK_LEN leben...
			
		}
		super.close();
	}

	/**
	 *	Format string besorgen
	 */
	public String getFormat()
	throws IOException
	{
		SpectStream tmpStream;
	
		tmpStream = stream;
		if( tmpStream == null ) {
			tmpStream = getDescr();
		}
		return( SpectStream.getFormat( tmpStream ));
	}
	
// -------- private Methoden --------
	
	/*
	 *	CSound/SoundHack Header einlesen
	 */
	private void readHeader()
	throws IOException
	{	
		seek( 0L );
		hdr[ hdr_magic ] = readInt();
		// only SoundHack supported at the moment
		if( (hdr[ hdr_magic ] != SHA_MAGIC) && (hdr[ hdr_magic ] != CSA_MAGIC) ) {
			throw new UnsupportedEncodingException( ERR_ILLEGALFILE );
		}
		mode = (mode & ~MODE_TYPEMASK) | MODE_SPECT;

		for( int i = 1; i < hdr.length; i++ ) {
			hdr[ i ] = readInt();
		}
		// only logarithmic + linear scaling supported
		if( (hdr[ hdr_freqMode ] != PVA_LIN) && (hdr[ hdr_freqMode ] != PVA_EXP) ) {
			throw new UnsupportedEncodingException( ERR_ILLEGALFREQFORMAT );
		}
		// supported only mag, phase, polar, rect
		switch( hdr[ hdr_frameFormat ] &= 0x0F ) {
		case PVA_MAG:
			dataOffset	= 0;
			dataNum		= 1;
			break;
		case PVA_PHASE:
			dataOffset	= 1;
			dataNum		= 1;
			break;
		case PVA_POLAR:
			dataOffset	= 0;
			dataNum		= 2;
			break;
		case PVA_RECT:
			dataOffset	= 0;
			dataNum		= 2;
			break;
			
		default:
			throw new UnsupportedEncodingException( ERR_ILLEGALFRAMEFORMAT );
		}
		// calc number of frames
		if( hdr[ hdr_dataBsize ] <= 0 ) {
			hdr[ hdr_dataBsize ] = (int) this.length() - hdr[ hdr_headBsize ];
		}
		
		bands	= (hdr[ hdr_frameSize ] / dataNum) + 1;
		hdr[ hdr_frameBsize ] = (bands * dataNum) * (hdr[ hdr_dataFormat ] & 0x0F);	// * bytes per word
		frames	= hdr[ hdr_dataBsize ] / (hdr[ hdr_frameBsize ] * hdr[ hdr_chanNum ]);

// SND HACK BUG FIX XXXX
if( hdr[ hdr_chanNum ] == 2 ) {
	hdr[ hdr_headBsize ] += hdr[ hdr_frameBsize ];	// adjust correct channel offset; loosing 1 frame...
}

	}
}
// class SpectralFile
