/*
 *  ImageFile.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.fscape.io;

import java.io.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class ImageFile
extends GenericFile
{
// -------- public Variablen --------

	public static final String ERR_PARALLEL		= "Colour planes not interleaved";
	public static final String ERR_COMPRESSED	= "Unsupported compression";

// -------- private Variablen --------

	private boolean suckyPCdata	= false;	// true for PC-style least-signifcant-byte first shit
	private boolean invert		= false;	// TIFF-grayscale: highest value is black
	private long	stripOffsetOffs = 0L;
	private int		stripOffsetSize;
//	private long	stripCountOffs	= 0L;
//	private int		stripCountSize;
	private int		rowsPerStrip;
	private int		numStrips;
	private int		bytesPerRow;
	private int		stripNum		= -1;	// which is the current strip #
	private int		stripLeft		= 0;	// number of bytes left in the strip

	private byte[]	buf				= null;
	private int		bufSize;
	private int		bufOffset;

	protected ImageStream	stream	= null;

	// TIFF essential Tags
	protected static final int NEWSUBTYPE_TAG	= 0x00FE;
	protected static final int SUBTYPE_TAG		= 0x00FF;
	protected static final int WIDTH_TAG		= 0x0100;
	protected static final int HEIGHT_TAG		= 0x0101;
	protected static final int BITSPERSMP_TAG	= 0x0102;	// bits per sample
	protected static final int CMPTYPE_TAG		= 0x0103;	// compression
	protected static final int PHOTOMETR_TAG	= 0x0106;	// whether grayscale or rgb
	protected static final int DESCR_TAG		= 0x010E;	// where we can keep track of the spect origin
	protected static final int STRIPOFFS_TAG	= 0x0111;	// where the image data is stored
	protected static final int SMPPERPIXEL_TAG	= 0x0115;	// color depth
	protected static final int ROWSPERSTRIP_TAG	= 0x0116;
	protected static final int STRIPCOUNT_TAG	= 0x0117;
	protected static final int XRES_TAG			= 0x011A;
	protected static final int YRES_TAG			= 0x011B;
	protected static final int PLANE_TAG		= 0x011C;	// rgb or seperate planes
	protected static final int RESUNIT_TAG		= 0x0128;	// inches, cm etc.
	
	protected static final int CMPTYPE_NONE		= 0x0001;	// no compression
	protected static final int CMPTYPE_LZW		= 0x0005;

	protected static final int NEWSUBTYPE_THUMB	= 0x0001;	// reduced version
	protected static final int NEWSUBTYPE_TRANSP= 0x0004;	// transparent mask

	protected static final int SUBTYPE_THUMB	= 0x0002;	// reduced version (old tag)

	protected static final int PHOTOMETR_GRAYINV= 0x0000;	// grayscale inverted
	protected static final int PHOTOMETR_GRAY	= 0x0001;	// normal grayscale
	protected static final int PHOTOMETR_RGB	= 0x0002;
	
	protected static final int PLANE_SERIAL		= 0x0001;	// r1g1b2, r2g2b2 etc.
	protected static final int PLANE_PARALLEL	= 0x0002;	// r, g, b got separate planes

	protected static final int RESUNIT_NONE		= ImageStream.RES_NONE;	// 0x0001;
	protected static final int RESUNIT_INCH		= ImageStream.RES_INCH;	// 0x0002;
	protected static final int RESUNIT_CM		= ImageStream.RES_CM;	// 0x0003;

// -------- public Methoden --------

	/**
	 *	Datei, die Imagedaten enthaelt bzw. enthalten soll, oeffnen
	 *
	 *	@param imageF	entsprechende Datei
	 *	@param mode		MODE_INPUT zum Lesen, MODE_OUTPUT zum Schreiben
	 */
	public ImageFile( File imageF, int mode )
	throws IOException
	{
		super( imageF, (mode & ~MODE_TYPEMASK) | MODE_TIFF );
	}
	
	public ImageFile( String fname, int mode )
	throws IOException
	{
		this( new File( fname ), mode );
	}

	/**
	 *	erzeugt einen ImageStream, in den der Header der Datei
	 *	uebertragen wird
	 *	KEINE AENDERUNGEN AM SOUNDSTREAM VORNEHMEN!
	 */
	public ImageStream initReader()
	throws IOException
	{
		if( stream == null ) {
			stream		= new ImageStream();
			readHeader();
			bufSize		= 131072;
//			bufSize		= getDefaultBufSize();
			bufSize	   -= bufSize % bytesPerRow;
			buf			= new byte[ Math.max( bytesPerRow, bufSize )];
		}
		stripNum	= -1;
		stripLeft	= 0;
		bufOffset	= bufSize;	// "empty"
		return stream;
	}

	/**
	 *	Meldet einen ImageStream zum Schreiben in das File an;
	 */
	public void initWriter( ImageStream stream )
	throws IOException
	{	
		this.stream		= stream;
		bytesPerRow		= stream.width * stream.smpPerPixel * ((stream.bitsPerSmp + 7) >> 3);
		suckyPCdata		= false;
		invert			= false;
		rowsPerStrip	= stream.height;
		numStrips		= 1;
		bufSize			= 131072;
//		bufSize			= getDefaultBufSize();
		bufSize		   -= bufSize % bytesPerRow;
		buf				= new byte[ Math.max( bytesPerRow, bufSize )];
		bufOffset		= 0;			// "empty"

		writeHeader();
	}
	
	/**
	 *	Erzeugt ein fuer diese Objekt zum Lesen/Schreiben geeignetes Array von Bytes
	 */
	public byte[] allocRow()
	{
		return new byte[ stream.width * stream.smpPerPixel *
						 ((stream.bitsPerSmp + 7) >> 3) ];
	}

	protected void seekNewStrip()
	throws IOException
	{
		stripNum++;
		if( stripNum >= numStrips ) throw new EOFException();
		
		if( stripOffsetSize == TIFFentry.LONG ) {
			seek( stripOffsetOffs + (stripNum << 2) );
			seek( readUniversalInt() );
		} else {
			seek( stripOffsetOffs + stripNum << 1 );
			seek( readUniversalUShort() );
		}
		stripLeft = bytesPerRow * rowsPerStrip;
	}

	/**
	 *	Liest einen Zeile aus der Datei ein
	 *
	 *	@param	data	sollte mit allocRow() beschafft worden sein!
	 */
	public void readRow( byte[] data )
	throws IOException
	{
		int		i, num;
		byte	b;
	
	// fill buffer
		if( bufOffset >= bufSize ) {		// buffer empty? physically load new rows
		
			if( stripLeft == 0 ) {
				seekNewStrip();
			}
			num = Math.min( bufSize, stripLeft );
			readFully( buf, bufSize - num, num );
			bufOffset	= bufSize - num;
			stripLeft  -= num;

			if( suckyPCdata && (stream.bitsPerSmp == 16) ) {
				for( i = 0; i < bufSize; i += 2 ) {
					b			= data[ i ];
					data[ i ]	= data[ i + 1 ];
					data[ i+1 ]	= b;
				}
			}
		}

	// copy data
		System.arraycopy( buf, bufOffset, data, 0, data.length );
		bufOffset += data.length;
		stream.rowsRead++;
		
		if( invert ) {
			for( i = 0; i < data.length; i++ ) {
				data[ i ] = (byte) ~data[ i ];
			}
		}
	}

	/**
	 *	Schreibt einen Zeile in die Datei
	 *
	 *	@param	data	sollte mit allocRow() beschafft worden sein!
	 */
	public void writeRow( byte[] data )
	throws IOException
	{
	// flush buffer
		if( bufOffset >= bufSize ) {		// buffer full? physically save rows
		
			write( buf );
			bufOffset	= 0;
		}

	// copy data
		System.arraycopy( data, 0, buf, bufOffset, data.length );
		bufOffset += data.length;
		stream.rowsWritten++;
	}

	/**
	 *	Datei schliessen
	 */
	public void close()
	throws IOException
	{
		stream			= null;
		int bufOffset	= this.bufOffset;
		this.bufOffset	= 0;
		byte buf[]		= this.buf;
		this.buf		= null;

		if( (mode & MODE_FILEMASK) == MODE_OUTPUT ) {

			if( (buf != null) && (bufOffset > 0) ) {		// ...then flush buffer
				write( buf, 0, bufOffset );
			}
		}
		super.close();
	}
	
	/**
	 *	Format string besorgen
	 */
	public String getFormat()
	throws IOException
	{
		ImageStream tmpStream;
	
		tmpStream = stream;
		if( tmpStream == null ) {
			tmpStream = initReader();
		}
		return( ImageStream.getFormat( tmpStream ));
	}

// -------- private Methoden --------
	
	/*
	 *	Datei Header einlesen; bei nicht unterstuetzen Formaten wird eine IOException
	 *	vom Typ UnsupportedEncodingException ausgeloest
	 */
	private void readHeader()
	throws IOException
	{
		int 		magic;
		long		oldPos;
		int			essentials;
		int			offset;
		int			i;
		// TIFF:
		int			entries;
		boolean		gray;
		TIFFentry	entry		= new TIFFentry();
	
		seek( 0L );
		magic = readInt();
		switch( magic ) {
		case TIFF_MAC_MAGIC:
			suckyPCdata	= false;
			break;
		case TIFF_IBM_MAGIC:
			suckyPCdata	= true;
			break;
		default:
			throw new UnsupportedEncodingException( ERR_ILLEGALFILE );
		}

		mode = (mode & ~MODE_TYPEMASK) | MODE_TIFF;	// only supported one so far

		do {	// go through all the IFD's
			offset = readUniversalInt();
			if( offset == 0 ) {
				throw new UnsupportedEncodingException( ERR_MISSINGDATA );
			}

			seek( offset );

//			stripCountOffs		= 0L;		// optional
			invert				= false;
			gray				= true;
			
			stream.hRes			= 1.0f / 72.0f;
			stream.hRes			= 1.0f / 72.0f;
			stream.resUnit		= ImageStream.RES_INCH;		// TIFF-default
			stream.bitsPerSmp	= 1;	// TIFF-default, will be rejected!
			stream.smpPerPixel	= 1;	// TIFF-default

			entries		= readUniversalUShort();
			essentials	= 5;
IFD:		for( i = 0; i < entries; i++ ) {

				readTIFFentry( entry );
				switch( entry.tag ) {

				case NEWSUBTYPE_TAG:
					if( (entry.value == NEWSUBTYPE_THUMB) || (entry.value == NEWSUBTYPE_TRANSP) ) {
						break IFD;	// can't use this image
					}
					break;
				case SUBTYPE_TAG:
					if( entry.value == SUBTYPE_THUMB ) {
						break IFD;	// can't use this image
					}
					break;

				case CMPTYPE_TAG:
					if( entry.value != CMPTYPE_NONE ) {
						throw new UnsupportedEncodingException( ERR_COMPRESSED );
					}
					break;

				case WIDTH_TAG:
					essentials--;
					stream.width = entry.value;
					break;
				case HEIGHT_TAG:
					essentials--;
					stream.height = entry.value;
					break;
				case XRES_TAG:
					stream.hRes = Float.intBitsToFloat( entry.value );
					break;
				case YRES_TAG:
					stream.vRes = Float.intBitsToFloat( entry.value );
					break;
				case RESUNIT_TAG:
					stream.resUnit = entry.value & 0x03;
					break;

				case BITSPERSMP_TAG:
					if( entry.count == 1 ) {
						stream.bitsPerSmp = entry.value;
					} else {
						oldPos = getFilePointer();
						seek( entry.value );
						for( int j = 0, k = 0; j < entry.count; j++, k = stream.bitsPerSmp ) {
							stream.bitsPerSmp = readUniversalUShort();
							if( (j > 0) & (k != stream.bitsPerSmp) ) {	// cannot mix different depths
								throw new UnsupportedEncodingException( ERR_UNSUPPORTED );
							}
						}
						seek( oldPos );
					}				// only 8-Bit and 16-Bit
					if( (stream.bitsPerSmp != 8) && (stream.bitsPerSmp != 16) ) {
						throw new UnsupportedEncodingException( ERR_UNSUPPORTED );
					}
					break;

				case SMPPERPIXEL_TAG:
					if( (entry.value < 1) || (gray && (entry.value != 1)) ) {	// grayscale must be 1 smp
						throw new UnsupportedEncodingException( ERR_CORRUPTED );
					}
					stream.smpPerPixel = entry.value;
					break;

				case PHOTOMETR_TAG:
					essentials--;
					switch( entry.value ) {
					case PHOTOMETR_GRAY:
						gray	= true;
						invert	= false;
						break;
					case PHOTOMETR_GRAYINV:
						gray	= true;
						invert	= true;
						break;
					case PHOTOMETR_RGB:
						gray	= false;
						invert	= false;
						break;
					default:
						throw new UnsupportedEncodingException( ERR_UNSUPPORTED );
					}
					break;

				case DESCR_TAG:
					stream.descr = readTIFFstring( entry );
					break;

				case STRIPOFFS_TAG:
					essentials--;
					if( entry.count == 1 ) {
						stripOffsetOffs = getFilePointer() - 4;
						stripOffsetSize = TIFFentry.LONG;
					} else {
						stripOffsetOffs	= entry.value;
						stripOffsetSize = entry.type;
					}
					break;
				case ROWSPERSTRIP_TAG:
					essentials--;
					rowsPerStrip = entry.value;
					numStrips	 = (stream.height + rowsPerStrip - 1) / rowsPerStrip;
					bytesPerRow	 = stream.width * stream.smpPerPixel * ((stream.bitsPerSmp + 7) >> 3);
   					break;
				case STRIPCOUNT_TAG:
					if( entry.count == 1 ) {
//						stripCountOffs = getFilePointer() - 4;
//						stripCountSize = TIFFentry.LONG;
					} else {
//						stripCountOffs = entry.value;
//						stripCountSize = entry.type;
					}
					break;

				case PLANE_TAG:
					if( entry.value == PLANE_PARALLEL ) {
						throw new UnsupportedEncodingException( ERR_PARALLEL );
					} else if( entry.value != PLANE_SERIAL ) {
						throw new UnsupportedEncodingException( ERR_UNSUPPORTED );
					}
					break;

				default:
					break;
				}
			} // for( IFD-entries )

			if( (i == entries) && (essentials > 0) ) {
				throw new UnsupportedEncodingException( ERR_MISSINGDATA );
			}
			
			if( i < entries ) {		// means this subfile was useless, so go to the next one
				seek( getFilePointer() + 12 * (entries - i) );
			}
		} while( essentials > 0 );	// until we found a completely useable subfile
	}
	
	/*
	 *	Unsigned Short lesen, ggf. PC-Codierung anwenden
	 */
	protected int readUniversalUShort()
	throws IOException
	{
		if( !suckyPCdata ) {
			return readUnsignedShort();
		} else {
			int i = readUnsignedShort();
			return( (i & 0x00FF) << 8 | ((i & 0xFF00) >> 8) );
		}
	}

	/*
	 *	4-Byte Integer lesen, ggf. PC-Codierung anwenden
	 */
	protected int readUniversalInt()
	throws IOException
	{
		if( !suckyPCdata ) {
			return readInt();
		} else {
			int i = readInt();
			return( (int) ((((long) i & 0xFF000000) >> 24) | ((i & 0x00FF0000) >> 8) |
					((i & 0x0000FF00) << 8) | ((i & 0x000000FF) << 24)) );
		}
	}

	/*
	 *	TIFF-Entry einlesen; Typen-Konvertierung erfolgt automatisch
	 *
	 *	einzelne Bytes und Shorts werden rechtsbŸndig formattiert
	 *	einzelne "Rational"-Typen werden in ein Float konvertiert (zurŸck Ÿber Float.intBitsToFloat())
	 */
	protected void readTIFFentry( TIFFentry entry )
	throws IOException
	{
		entry.tag	= readUniversalUShort();
		entry.type	= readUniversalUShort();
		entry.count	= readUniversalInt();
		entry.value	= readUniversalInt();

		switch( entry.type ) {
		case TIFFentry.BYTE:
			if( entry.count == 1 ) {
				entry.value = (entry.value >> 24) & 0x000000FF;
			}
			break;
		case TIFFentry.SHORT:
			if( entry.count == 1 ) {
				entry.value = (entry.value >> 16) & 0x0000FFFF;
			}
			break;
		case TIFFentry.RATIONAL:
			if( entry.count == 1 ) {
				long oldPos = getFilePointer();
				seek( entry.value );
				long val	= readLong();
				seek( oldPos );
				entry.value = Float.floatToIntBits( (float) (val >> 32) / (float) (val & 0xFFFFFFFF) );
			}
			break;
		default:
			break;
		}
	}

	/*
	 *	TIFF-Entry schreiben
	 */
	protected void writeTIFFentry( int tag, int type, int count, int val )
	throws IOException
	{
		writeShort( tag );
		writeShort( type );
		writeInt( count );
		
		if( count == 1 ) {
			if( type == TIFFentry.SHORT ) {
				val <<= 16;
			} else if( type == TIFFentry.BYTE ) {
				val <<= 24;
			}
		}

		writeInt( val );
	}
	
	/*
	 *	ASCII-String, der durch TIFFentry markiert ist lesen
	 */
	protected String readTIFFstring( TIFFentry entry )
	throws IOException
	{
		byte ascii[] = new byte[ entry.count - 1 ];
	
		long oldPos = getFilePointer();
		seek( entry.value );
		readFully( ascii );
		seek( oldPos );
		
		return new String( ascii );
	}

	/*
	 *	Datei Header schreiben
	 */
	private void writeHeader()
	throws IOException
	{
		int		entries = (stream.smpPerPixel == 1) ? 13 : 14;	// rgb has photometric interpret.
		String	descr	= (stream.descr != null) ? stream.descr : "";
		int		offset	= 24 + 6 + (descr.length() + 2) & ~1;

		seek( 0L );
		writeInt( TIFF_MAC_MAGIC );
		writeInt( offset );						// offset
		writeInt( (int) (stream.hRes * 10000f) );
		writeInt( 10000 );
		writeInt( (int) (stream.vRes * 10000f) );
		writeInt( 10000 );
		writeShort( stream.bitsPerSmp );	// auch grayscale, um gleichen offset zu haben
		writeShort( stream.bitsPerSmp );
		writeShort( stream.bitsPerSmp );
		if( descr.length() > 0 ) {
			writeBytes( descr );
		}
		if( (getFilePointer() & 1) == 0 ) {
			writeShort( 0 );
		} else {
			writeByte( 0 );
		}
		
		writeShort( entries );

		for( int tag = NEWSUBTYPE_TAG; tag <= RESUNIT_TAG; tag++ ) {
		
			switch( tag ) {
			case NEWSUBTYPE_TAG:
				writeTIFFentry( tag, TIFFentry.LONG, 1, 0 );
				break;

			case WIDTH_TAG:
				writeTIFFentry( tag, TIFFentry.LONG, 1, stream.width );
				break;
			case HEIGHT_TAG:
			case ROWSPERSTRIP_TAG:
				writeTIFFentry( tag, TIFFentry.LONG, 1, stream.height );
				break;

			case STRIPOFFS_TAG:
				writeTIFFentry( tag, TIFFentry.LONG, 1, offset + entries * 12 + 4 );
				break;
			case STRIPCOUNT_TAG:
				writeTIFFentry( tag, TIFFentry.LONG, 1, bytesPerRow * stream.height );
				break;

			case XRES_TAG:
				writeTIFFentry( tag, TIFFentry.RATIONAL, 1, 8 );
				break;
 			case YRES_TAG:
				writeTIFFentry( tag, TIFFentry.RATIONAL, 1, 16 );
				break;
 			case RESUNIT_TAG:
				writeTIFFentry( tag, TIFFentry.SHORT, 1, stream.resUnit );
				break;

			case SMPPERPIXEL_TAG:
				writeTIFFentry( tag, TIFFentry.SHORT, 1, stream.smpPerPixel );
				break;
				
			case BITSPERSMP_TAG:
				writeTIFFentry( tag, TIFFentry.SHORT, stream.smpPerPixel,
								(stream.smpPerPixel == 1) ? stream.bitsPerSmp : 24 );	// ggf. offset
				break;

			case CMPTYPE_TAG:
				writeTIFFentry( tag, TIFFentry.SHORT, 1, CMPTYPE_NONE );
				break;

			case PHOTOMETR_TAG:
				writeTIFFentry( tag, TIFFentry.SHORT, 1, (stream.smpPerPixel == 1) ?
								PHOTOMETR_GRAY : PHOTOMETR_RGB );
				break;

			case PLANE_TAG:
				if( stream.smpPerPixel == 3 ) {
					writeTIFFentry( tag, TIFFentry.SHORT, 1, PLANE_SERIAL );
				}
				break;
			
			case DESCR_TAG:
				if( descr.length() > 0 ) {
					writeTIFFentry( tag, TIFFentry.ASCII, descr.length() + 1, 30 );
				}
				break;
							
			default:
				break;
			}
		}
		
		writeShort( 0 );	// no more sub images
	}
}
// class ImageFile

class TIFFentry
{
	int	tag;
	int	type;
	int count;
	int value;
	
	static final int	BYTE		= 1;
	static final int	ASCII		= 2;
	static final int	SHORT		= 3;
	static final int	LONG		= 4;
	static final int	RATIONAL	= 5;
}
// class TIFFentry
