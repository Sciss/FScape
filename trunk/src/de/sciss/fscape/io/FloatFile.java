/*
 *  FloatFile.java
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
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class FloatFile
extends GenericFile
{
// -------- public Variablen --------

// -------- private Variablen --------

	private byte	buf[];
	private int		bufSize;			// = buf.length
	private int		bufOffset	= 0;
	private int		bufLength	= 0;	// tatsaechlicher Inhalt!
	private boolean	flushed		= true;
	private long	bufPhysical	= 0L;	// FilePointer of Buffer-Start

// -------- public Methoden --------

	/**
	 *	Datei, die Float-Daten enthaelt bzw. enthalten soll, oeffnen
	 *
	 *	@param f		entsprechende Datei
	 *	@param mode		MODE_INPUT zum Lesen, MODE_OUTPUT zum Schreiben
	 */
	public FloatFile( File f, int mode )
	throws IOException
	{
		super( f, (mode & ~MODE_TYPEMASK) | MODE_FLOAT );

		bufSize			= 131072; // getDefaultBufSize() & ~3;
		buf				= new byte[ bufSize ];

		if( (mode & MODE_FILEMASK) == MODE_INPUT ) {		// "read/write header"
			if( (this.mode & MODE_TYPEMASK) != MODE_FLOAT ) {
				throw new UnsupportedEncodingException( ERR_ILLEGALFILE );
			}
		} else {
			writeInt( FLOAT_MAGIC );
		}
		seekFloat( 0 );
	}
	
	public FloatFile( String fname, int mode )
	throws IOException
	{
		this( new File( fname ), mode );
	}


	/**
	 *	Springt zu einem bestimmten Float
	 *	(nachfolgende readFloats() und writeFloats() sind betroffen)
	 */
	public void seekFloat( int offset )
	throws IOException
	{
		long physical	= (offset + 1) << 2;
	
		if( !flushed ) {
			flush();
		} else {
			if( (bufPhysical <= physical) && (bufPhysical + (long) bufLength > physical) ) {
				bufOffset	= (int) (physical - bufPhysical);		// minimize loading
				physical	= bufPhysical + (long) bufLength;
			} else {
				bufOffset = 0;
				bufLength = 0;
			}
		}
		seek( physical );
	}
	
	public void flush()
	throws IOException
	{
		if( !flushed ) {
			write( buf, 0, bufLength );		// flush
			flushed		= true;
			bufOffset	= 0;
			bufLength	= 0;
			bufPhysical	= getFilePointer();
		}
	}

	public long getSize()
	throws IOException
	{
		flush();
		return( (length() >> 2) - 1 );
	}

	/**
	 *	Liest Floats aus der Datei ein
	 *	auch bei Output-Files erlaubt
	 *
	 *	@return	Zahl der gelesenen Floats
	 *	(wenn kleiner als length, wurde der Rest automatisch mit Nullen gefuellt!)
	 */
	public int readFloats( float[] data, int offset, int length )
	throws IOException
	{
		int 	dataEnd		= offset + length;
		int 	i, num;

		flush();

loop:	while( offset < dataEnd ) {
		
			if( bufOffset >= bufLength ) {
			
				bufPhysical	= getFilePointer();
				num			= (int) Math.min( (long) bufSize, this.length() - bufPhysical ) & ~3;
				readFully( buf, 0, num );
				bufOffset	= 0;
				bufLength	= num;
			}

			num = Math.min( dataEnd - offset, (bufLength - bufOffset) >> 2 );
			if( num <= 0 ) break loop;
				
			for( i = 0; i < num; i++ ) {
				data[ offset++ ] = Float.intBitsToFloat(
											((int) buf[ bufOffset++ ] << 24) |
										   (((int) buf[ bufOffset++ ] & 0xFF) << 16) |
										   (((int) buf[ bufOffset++ ] & 0xFF) << 8) |
											((int) buf[ bufOffset++ ] & 0xFF) );
			}
		} // main loop

		num = length - dataEnd + offset;
		if( (num == 0) && (length > 0) ) throw new EOFException();
		while( offset < dataEnd ) {
			data[ offset++ ] = 0.0f;		// zero padding
		}
		
		return num;
	}

	// full array read
	public int readFloats( float[] data )
	throws IOException
	{
		return readFloats( data, 0, data.length );
	}

	/**
	 *	Liest Ints aus der Datei ein
	 *	auch bei Output-Files erlaubt
	 *
	 *	@return	Zahl der gelesenen Ints
	 *	(wenn kleiner als length, wurde der Rest automatisch mit Nullen gefuellt!)
	 */
	public int readInts( int[] data, int offset, int length )
	throws IOException
	{
		int 	dataEnd		= offset + length;
		int 	i, num;

		flush();

loop:	while( offset < dataEnd ) {
		
			if( bufOffset >= bufLength ) {
			
				bufPhysical	= getFilePointer();
				num			= (int) Math.min( (long) bufSize, this.length() - bufPhysical ) & ~3;
				readFully( buf, 0, num );
				bufOffset	= 0;
				bufLength	= num;
			}

			num = Math.min( dataEnd - offset, (bufLength - bufOffset) >> 2 );
			if( num <= 0 ) break loop;
				
			for( i = 0; i < num; i++ ) {
				data[ offset++ ] =   ((int) buf[ bufOffset++ ] << 24) |
									(((int) buf[ bufOffset++ ] & 0xFF) << 16) |
									(((int) buf[ bufOffset++ ] & 0xFF) << 8) |
									((int) buf[ bufOffset++ ] & 0xFF);
			}
		} // main loop

		num = length - dataEnd + offset;
		if( (num == 0) && (length > 0) ) throw new EOFException();
		while( offset < dataEnd ) {
			data[ offset++ ] = 0;		// zero padding
		}
		
		return num;
	}

	// full array read
	public int readInts( int[] data )
	throws IOException
	{
		return readInts( data, 0, data.length );
	}

	/**
	 *	Read an unbuffered Float
	 *	Switching between this and readFloats()/writeFloats() MUST be
	 *	intercepted by a seekFloat() to ensure that the buffer holds
	 *	no artifacts!!!
	 */
	public float readUnbufferedFloat()
	throws IOException
	{
		return readFloat();
	}
	
	/**
	 *	Schreibt Floats in die Datei
	 */
	public void writeFloats( float[] data, int offset, int length )
	throws IOException
	{
		int 	dataEnd		= offset + length;
		int 	i, j, num;

		if( flushed ) {
			if( bufOffset != bufLength ) {		// falls readFloats() noch Restpuffer erzeugt hat
				seek( bufPhysical + (long) bufOffset );
			}
			bufOffset	= 0;
			bufLength	= 0;
			bufPhysical	= getFilePointer();
		}

		while( offset < dataEnd ) {
			if( bufLength >= bufSize ) {
				flush();
			}
			flushed	= false;

			num		= Math.min( dataEnd - offset, (bufSize - bufLength) >> 2 );

			for( i = 0; i < num; i++ ) {
				j = Float.floatToIntBits( data[ offset++ ]);
				buf[ bufLength++ ] = (byte) (j >> 24);
				buf[ bufLength++ ] = (byte) (j >> 16);
				buf[ bufLength++ ] = (byte) (j >> 8);
				buf[ bufLength++ ] = (byte) j;
			}
		} // main loop
		
		bufOffset = bufLength;
	}

	// full array write
	public void writeFloats( float[] data )
	throws IOException
	{
		writeFloats( data, 0, data.length );
	}

	/**
	 *	Schreibt Ints in die Datei
	 */
	public void writeInts( int[] data, int offset, int length )
	throws IOException
	{
		int 	dataEnd		= offset + length;
		int 	i, j, num;

		if( flushed ) {
			if( bufOffset != bufLength ) {		// falls readInts() noch Restpuffer erzeugt hat
				seek( bufPhysical + (long) bufOffset );
			}
			bufOffset	= 0;
			bufLength	= 0;
			bufPhysical	= getFilePointer();
		}

		while( offset < dataEnd ) {
			if( bufLength >= bufSize ) {
				flush();
			}
			flushed	= false;

			num		= Math.min( dataEnd - offset, (bufSize - bufLength) >> 2 );

			for( i = 0; i < num; i++ ) {
				j = data[ offset++ ];
				buf[ bufLength++ ] = (byte) (j >> 24);
				buf[ bufLength++ ] = (byte) (j >> 16);
				buf[ bufLength++ ] = (byte) (j >> 8);
				buf[ bufLength++ ] = (byte) j;
			}
		} // main loop
		
		bufOffset = bufLength;
	}

	// full array write
	public void writeInts( int[] data )
	throws IOException
	{
		writeInts( data, 0, data.length );
	}

	/**
	 *	Datei schliessen
	 */
	public void close()
	throws IOException
	{
		int bufLength	= this.bufLength;
		this.bufLength	= 0;
		byte buf[]		= this.buf;
		this.buf		= null;

		if( !flushed ) {
			flushed = true;
			if( (buf != null) && (bufLength > 0) ) {		// ...then flush buffer
				write( buf, 0, bufLength );
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
		return( "temporary file; "+getSize() + " 16-bit floating point numbers" );
	}

// -------- private Methoden --------
}
// class FloatFile
