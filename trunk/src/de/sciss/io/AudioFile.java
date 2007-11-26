/*
 *  AudioFile.java
 *  de.sciss.io package
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
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
 *		21-May-05	created from de.sciss.eisenkraut.io.AudioFile
 *		15-Jul-05	KEY_APPCODE
 *		28-Aug-05	removed rounding (was buggy) in float<->int conversion
 *					; read/write 16...24 bit int completely noise free now
 *					; optimized buffer handler classes, up to two times faster now
 *		07-Sep-05	marker + region support for WAV, bugfix in WAV readHeader
 *		22-Dec-05	conforms to new contract that allows null-arrays in readFrames
 *		08-Jan-06	added setFrameNum()
 *		21-Feb-06	supports comments (AIFF, IRCAM, SND); don't know how to do it in WAVE (when not linked to a cuepoint)
 *		25-Feb-06	moved to double precision rate
 *		27-May-06	fixed bug in AIFF COMT chunk creation
 *		02-Jul-06	changed AIFF and WAVE read header to allow files > 2 GB
 *		31-Jan-07	added supported for AIFC little endian ; fixed sucky 8-bit WAV
 *		27-Mar-07	separate APPCODE reader, not requiring Application class; separate markers reading;
 *					fixed AIFF output file endian bug
 */

package de.sciss.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import de.sciss.app.AbstractApplication;

/**
 *	The <code>AudioFile</code> allows reading and writing
 *	of sound files. It wraps a <code>RandomAccessFile</code>
 *	and delegates the I/O to subclasses which deal with
 *	the specific sample format and endianess.
 *	<p>
 *	Currently supported formats are: AIFF, IRCAM,
 *  NeXT/Sun (.au), and WAVE. Supported resolutions are
 *  8/16/24/32 bit integer and 32/64 bit floating point.
 *  However not all audio formats support all bit depths.
 *  <p>
 *	Not all format combinations are supported, for example
 *	the rather exotic little-endian AIFF, but also
 *	little-endian SND, WAVE 8-bit.
 *	<p>
 *  In order to simplify communication with CSound,
 *  raw output files are supported, raw input files however
 *  are not recognized.
 *  <p>
 *  To create a new <code>AudioFile</code> you call
 *  one of its static methods <code>openAsRead</code> or
 *  <code>openAsWrite</code>. The format description
 *  is handled by an <code>AudioFileDescr</code> object.
 *	This object also contains information about what special
 *	tags are read/written for which format. For example,
 *	AIFF can read/write markers, and application-specific
 *	chunk, and a gain tag. WAVE can read/write markers and
 *	regions, and a gain tag, etc.
 *	<p>
 *	The <code>AudioFile</code> implements the generic
 *	interface <code>InterleavedStreamFile</code> (which
 *	is likely to be modified in the future) to allow
 *	clients to deal more easily with different sorts
 *	of streaming files, not just audio files.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.34, 22-Jun-07
 *
 *  @see		AudioFileDescr
 *
 *  @todo		more flexible handling of endianess,
 *				at least SND and IRCAM should support both
 *				versions.
 *
 *	@todo		more tags, like peak information and
 *				channel panning.
 *
 *	@todo		(faster) low-level direct file-to-file
 *				copy in the copyFrames method
 */
public class AudioFile
implements InterleavedStreamFile
{
	private static final int MODE_READONLY   = 0;
	private static final int MODE_READWRITE  = 1;

	private final RandomAccessFile	raf;
	private final FileChannel		fch;
	private final int				mode;

	private AudioFileDescr			afd;
	private AudioFileHeader			afh;
	
	private ByteBuffer				byteBuf;
	private int						byteBufCapacity;
	private int						bytesPerFrame;
	private int						frameBufCapacity;
	private BufferHandler			bh;
	private int						channels;
	private long					framePosition;
	
	private long					updateTime;
	private long					updateLen;
	private long					updateStep;

	private static final String	NAME_LOOP		= "loop";
	private static final String	NAME_MARK		= "mark";
	private static final String	NAME_REGION		= "region";

// -------- public Methoden --------

	/**
	 *  Opens an audio file for reading.
	 *
	 *  @param		f   the path name of the file
	 *  @return		a new <code>AudioFile</code> object
	 *				whose header is already parsed and can
	 *				be obtained through the <code>getDescr</code> method.
	 *
	 *  @throws IOException if the file was not found, could not be read
	 *						or has an unknown or unsupported format
	 */
	public static AudioFile openAsRead( File f )
	throws IOException
	{
		AudioFile af	= new AudioFile( f, MODE_READONLY );
		af.afd			= new AudioFileDescr();
		af.afd.file		= f;
		af.afd.type		= af.retrieveType();
		af.afh			= af.createHeader();
		af.afh.readHeader( af.afd );
		af.init();
		af.seekFrame( 0 );
		return af;
	}
	
	/**
	 *  Opens an audio file for reading/writing. The pathname
	 *	is determined by the <code>file</code> field of the provided <code>AudioFileDescr</code>.
	 *	If a file denoted by this path already exists, it will be
	 *	deleted before opening.
	 *	<p>
	 *	Note that the initial audio file header is written immediately.
	 *	Special tags for the header thus need to be set in the <code>AudioFileDescr</code>
	 *	before calling this method, including markers and regions. It is not
	 *	possible to write markers and regions after the file has been opened
	 *	(since the header size has to be constant).
	 *
	 *  @param  afd format and resolution of the new audio file.
	 *				the header is immediatly written to the harddisc
	 *
	 *  @throws IOException if the file could not be created or the
	 *						format is unsupported
	 */
	public static AudioFile openAsWrite( AudioFileDescr afd )
	throws IOException
	{
		if( afd.file.exists() ) afd.file.delete();
		AudioFile af	= new AudioFile( afd.file, MODE_READWRITE );
		af.afd			= afd;
		afd.length		= 0;
		af.afh			= af.createHeader();
		af.afh.writeHeader( af.afd );
		af.init();
		af.seekFrame( 0 );
		af.updateStep	= (long) afd.rate * 20;
		af.updateLen	= af.updateStep;
		af.updateTime	= System.currentTimeMillis() + 10000;
		return af;
	}
	
	private AudioFile( File f, int mode )
	throws IOException
	{
		raf			= new RandomAccessFile( f, mode == MODE_READWRITE ? "rw" : "r" );
		fch			= raf.getChannel();
		this.mode   = mode;
	}

	/**
	 *  Returns a description of the audio file's format.
	 *  Fields which are guaranteed to be filled in, are
	 *  the type (use <code>getType</code>), <code>channels</code>,
	 *  <code>bitsPerSample</code>, <code>sampleFormat</code>,
	 *  <code>rate</code> and <code>length</code>.
	 *
	 *  @return an <code>AudioFileDescr</code> describing
	 *			this audio file.
	 *
	 *  @warning	the returned description is not immutable but
	 *				should be considered read only, do not modify it.
	 *				the fields may change dynamically if the file
	 *				is modified, e.g. the <code>length</code> field
	 *				for a writable file.
	 */
	public AudioFileDescr getDescr()
	{
		return afd;
	}
	
	/**
	 *  Returns the file that was used to open
	 *  the audio file. Note that this simply returns
	 *	getDescr().file, so it's not a good idea to
	 *	modify this field after opening the audio file.
	 *
	 *  @return the <code>File</code> that was used in
	 *			the static constructor methods. Can be used
	 *			to query the pathname or to delete the file after
	 *			it has been closed
	 */
	public File getFile()
	{
		return afd.file;
	}
	
	private void init()
	throws IOException
	{
		channels		= afd.channels;
		bytesPerFrame	= (afd.bitsPerSample >> 3) * channels;
		frameBufCapacity= Math.max( 1, 65536 / Math.max( 1, bytesPerFrame ));
		byteBufCapacity = frameBufCapacity * bytesPerFrame;
		byteBuf			= ByteBuffer.allocateDirect( byteBufCapacity );
		byteBuf.order( afh.getByteOrder() );
		bh				= null;

		switch( afd.sampleFormat ) {
		case AudioFileDescr.FORMAT_INT:
			switch( afd.bitsPerSample ) {
			case 8:			// 8 bit int
				if( afh.isUnsignedPCM() ) {
					bh  = new UByteBufferHandler();
				} else {
					bh  = new ByteBufferHandler();
				}
				break;
			case 16:		// 16 bit int
				bh  = new ShortBufferHandler();
				break;
			case 24:		// 24 bit int
				if( afh.getByteOrder() == ByteOrder.BIG_ENDIAN ) {
					bh  = new ThreeByteBufferHandler();
				} else {
					bh  = new ThreeLittleByteBufferHandler();
				}
				break;
			case 32:		// 32 bit int
				bh  = new IntBufferHandler();
				break;
			}
			break;
		case AudioFileDescr.FORMAT_FLOAT:
			switch( afd.bitsPerSample ) {
			case 32:		// 32 bit float
				bh  = new FloatBufferHandler();
				break;
			case 64:		// 64 bit float
				bh  = new DoubleBufferHandler();
				break;
			}
		}
		if( bh == null) throw new IOException( getResourceString( "errAudioFileEncoding" ));
	}

	private AudioFileHeader createHeader()
	throws IOException
	{
		switch( afd.getType() ) {
		case AudioFileDescr.TYPE_AIFF:
			return new AIFFHeader();
		case AudioFileDescr.TYPE_SND:
			return new SNDHeader();
		case AudioFileDescr.TYPE_IRCAM:
			return new IRCAMHeader();
		case AudioFileDescr.TYPE_WAVE:
			return new WAVEHeader();
		case AudioFileDescr.TYPE_RAW:
			return new RawHeader();
		default:
			throw new IOException( getResourceString( "errAudioFileType" ));
		}
	}

	/*
	 *	Reads file header in order to determine file type
	 */
	private int retrieveType()
	throws IOException
	{
		long	len		= raf.length();
		long	oldpos	= raf.getFilePointer();
		int		magic;
		int		type	= AudioFileDescr.TYPE_UNKNOWN;

		if( len < 4 ) return AudioFileDescr.TYPE_UNKNOWN;

		raf.seek( 0L );
		magic = raf.readInt();
		switch( magic ) {
		case AIFFHeader.FORM_MAGIC:					// -------- probably AIFF --------
			if( len < 8 ) return AudioFileDescr.TYPE_UNKNOWN;
			raf.readInt();
			magic = raf.readInt();
			switch( magic ) {
			case AIFFHeader.AIFC_MAGIC:
			case AIFFHeader.AIFF_MAGIC:
				type = AudioFileDescr.TYPE_AIFF;
				break;
			}
			break;

		case SNDHeader.SND_MAGIC:					// -------- snd sound --------
			type = AudioFileDescr.TYPE_SND;
			break;

		case IRCAMHeader.IRCAM_VAXBE_MAGIC:			// -------- IRCAM sound --------
		case IRCAMHeader.IRCAM_SUNBE_MAGIC:
		case IRCAMHeader.IRCAM_MIPSBE_MAGIC:
			type = AudioFileDescr.TYPE_IRCAM;
			break;
			
		case WAVEHeader.RIFF_MAGIC:					// -------- probably WAVE --------
			if( len < 8 ) return AudioFileDescr.TYPE_UNKNOWN;
			raf.readInt();
			magic = raf.readInt();
			switch( magic ) {
			case WAVEHeader.WAVE_MAGIC:
				type = AudioFileDescr.TYPE_WAVE;
				break;
			}
			break;

		default:
			break;
		}

		raf.seek( oldpos );
		return type;
	}

	/**
	 *  Moves the file pointer to a specific
	 *  frame.
	 *
	 *  @param  frame   the sample frame which should be
	 *					the new file position. this is really
	 *					the sample index and not the physical file pointer.
	 *  @throws IOException when a seek error occurs or you try to
	 *						seek past the file's end.
	 */
	public void seekFrame( long frame )
	throws IOException
	{
		long physical	= afh.getSampleDataOffset() + frame * bytesPerFrame;

		// XXX fch.force( true );
		
		raf.seek( physical );
		framePosition = frame;
	}
	
	/**
	 *	Flushes pending buffer content, and 
	 *	updates the sound file header information
	 *	(i.e. length fields). Usually you
	 *	will not have to call this method directly,
	 *	unless you pause writing for some time
	 *	and want the file information to appear
	 *	as accurate as possible.
	 */
	public void flush()
	throws IOException
	{
		updateTime	= System.currentTimeMillis() + 10000;
		afd.length	= framePosition;
		afh.updateHeader( afd );
		updateLen	= framePosition + updateStep;
		fch.force( true );
	}
	
	/**
	 *  Returns the current file pointer in sample frames
	 *
	 *  @return		the sample frame index which is the offset
	 *				for the next read or write operation.
	 *
	 *  @throws IOException		when the position cannot be queried
	 */
	public long getFramePosition()
	throws IOException
	{
		return( framePosition );
	}

	/**
	 *	Reads sample frames from the current position
	 *
	 *  @param  data	buffer to hold the frames read from harddisc.
	 *					the samples will be deinterleaved such that
	 *					data[0][] holds the first channel, data[1][]
	 *					holds the second channel etc.
	 *					; it is allowed to have null arrays in the data
	 *					(e.g. data[0] == null), in which case these channels
	 *					are skipped when reading
	 *  @param  offset  offset in the buffer in sample frames, such
	 *					that he first frame of the first channel will
	 *					be placed in data[0][offset] etc.
	 *  @param  length  number of continuous frames to read.
	 *
	 *  @throws IOException if a read error or end-of-file occurs.
	 */
	public void readFrames( float[][] data, int offset, int length )
	throws IOException
	{
		bh.readFrames( data, offset, length );
		framePosition += length;
	}

	/**
	 *	Writes sample frames to the file starting at the current position.
	 *  If you write past the previous end of the file, the <code>length</code>
	 *  field of the internal <code>AudioFileDescr</code> is updated.
	 *  Since you get a reference from <code>getDescr</code> and not
	 *  a copy, using this reference to the description will automatically
	 *  give you the correct file length.
	 *
	 *  @param  data	buffer holding the frames to write to harddisc.
	 *					the samples must be deinterleaved such that
	 *					data[0][] holds the first channel, data[1][]
	 *					holds the second channel etc.
	 *  @param  offset  offset in the buffer in sample frames, such
	 *					that he first frame of the first channel will
	 *					be read from data[0][offset] etc.
	 *  @param  length  number of continuous frames to write.
	 *
	 *  @throws IOException if a write error occurs.
	 */
	public void writeFrames( float[][] data, int offset, int length )
	throws IOException
	{
		bh.writeFrames( data, offset, length );
		framePosition += length;

		if( framePosition > afd.length ) {
			if( (framePosition > updateLen) || (System.currentTimeMillis() > updateTime) ) {
				flush();
			} else {
				afd.length = framePosition;
			}
		}
	}
	
	/**
	 *	Returns the number of frames
	 *	in the file.
	 *
	 *	@return	the number of sample frames
	 *			in the file. includes pending
	 *			buffer content
	 *
	 *	@throws	IOException	this is never thrown
	 *			but declared as of the <code>InterleavedStreamFile</code>
	 *			interface
	 */
	public long getFrameNum()
	throws IOException
	{
		return afd.length;
	}

	public void setFrameNum( long frame )
	throws IOException
	{
		final long physical	= afh.getSampleDataOffset() + frame * bytesPerFrame;

		raf.setLength( physical );
		if( framePosition > frame ) framePosition = frame;
		afd.length	= frame;
//		updateTime	= System.currentTimeMillis() + 10000;
//		afh.updateHeader( afd );
//		updateLen	= framePosition + updateStep;
	}

	/**
	 *	Returns the number of channels
	 *	in the file.
	 *
	 *	@return	the number of channels
	 */
	public int getChannelNum()
	{
		return afd.channels;
	}

	/**
	 *	Truncates the file to the size represented
	 *	by the current file position. The file
	 *	must have been opened in write mode.
	 *	Truncation occurs only if frames exist
	 *	beyond the current file position, which implicates
	 *	that you have set the position using <code>seekFrame</code>
	 *	to a location before the end of the file.
	 *	The header information is immediately updated.
	 *
	 *	@throws	IOException	if truncation fails
	 */
	public void truncate()
	throws IOException
	{
		fch.truncate( fch.position() );
		if( framePosition != afd.length ) {
			afd.length	= framePosition;
			updateTime	= System.currentTimeMillis() + 10000;
			afh.updateHeader( afd );
			updateLen	= framePosition + updateStep;
		}
	}

	/**
	 *	Copies sample frames from a source sound file
	 *	to a target file (either another sound file
	 *	or any other class implementing the
	 *	<code>InterleavedStreamFile</code> interface).
	 *	Both files must have the same number of channels.
	 *
	 *	@param	target	to file to copy to from this audio file
	 *	@param	length	the number of frames to copy. Reading
	 *					and writing begins at the current positions
	 *					of both files.
	 *
	 *	@throws	IOException	if a read or write error occurs
	 */
	public void copyFrames( InterleavedStreamFile target, long length )
	throws IOException
	{
		int chunkLength;

//		if( (target instanceof AudioFile) && ((AudioFile) target).bh.getClass().equals( this.bh.getClass() ) &&
//			(((AudioFile) target).channels == this.channels) ) {
//		
//			while( length > 0 ) {
//				chunkLength = Math.min( frameBufCapacity, length );
//				byteBuf.clear();
//					...
//				byteBuf.flip();
//				fch.write( byteBuf );
//				length -= chunkLength;
//			}
//
//		} else {
			int			tempBufSize	= (int) Math.min( length, 8192 );
			float[][]	tempBuf		= new float[ channels ][ tempBufSize ];
			
			while( length > 0 ) {
				chunkLength	= (int) Math.min( length, tempBufSize );
				this.readFrames( tempBuf, 0, chunkLength );
				target.writeFrames( tempBuf, 0, chunkLength );
				length -= chunkLength;
			}
//		}
	}

	/**
	 *  Flushes and closes the file
	 *
	 *  @throws IOException if an error occurs during buffer flush
	 *						or closing the file.
	 */
	public void close()
	throws IOException
	{
		if( mode == MODE_READWRITE ) {
			fch.force( true );
			afh.updateHeader( afd );
		}
		raf.close();

	}

	/**
	 *  Flushes and closes the file. As opposed
	 *	to <code>close()</code>, this does not
	 *	throw any exceptions but simply ignores any errors.
	 *
	 *	@see	#close()
	 */
	public void cleanUp()
	{
		try {
			close();
		}
		catch( IOException e ) {}		// ignore
	}

	/**
	 *  Reads markers into the audio file description
	 *  if there are any. This method sets the <code>KEY_MARKERS</code>
	 *  property of the afd, if markers are available. It sets
	 *  the <code>KEY_LOOP</code> property if a loop span is available.
	 *
	 *	@see	#getDescr()
	 *	@see	AudioFileDescr#KEY_MARKERS
	 *	@see	AudioFileDescr#KEY_LOOP
	 *
	 *	@throws	IOException	if a read or parsing error occurs
	 */
	public void readMarkers()
	throws IOException
	{
		afh.readMarkers();
	}

	/**
	 *  Reads application specific code into the audio file description
	 *  if there is such code. This method sets the <code>KEY_APPCODE</code>
	 *  property of the afd. The caller can check the <code>appCode</code>
	 *  field of the afd to ensure that potential app code is relevant to it.
	 *
	 *	@see	#getDescr()
	 *	@see	AudioFileDescr#KEY_APPCODE
	 *	@see	AudioFileDescr#appCode
	 *
	 *	@throws	IOException	if a read or parsing error occurs
	 */
	public void readAppCode()
	throws IOException
	{
		afh.readAppCode();
	}

	private static final String getResourceString( String key )
	{
		return IOUtil.getResourceString( key );
	}
	
// -------- BufferHandler Klassen --------

	private abstract class BufferHandler
	{
		protected abstract void writeFrames( float[][] frames, int off, int len ) throws IOException;
		protected abstract void readFrames( float[][] frames, int off, int len ) throws IOException;
	}
	
	private class ByteBufferHandler
	extends BufferHandler
	{
		private final byte[]	arrayBuf;

		private ByteBufferHandler()
		{
			arrayBuf	= new byte[ byteBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = (byte) (b[ j ] * 0x7F);
					}
				}
				byteBuf.clear();
				byteBuf.put( arrayBuf, 0, m );
				byteBuf.flip();
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				byteBuf.rewind().limit( m );
				fch.read( byteBuf );
				byteBuf.flip();
				byteBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						b[ j ]	= (float) arrayBuf[ i ] / 0x7F;
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	// float to byte = f*0x7F+0x80 (-1 ... +1 becomes 0x01 to 0xFF)
	// which is how libsndfile behaves
	private class UByteBufferHandler
	extends BufferHandler
	{
		private final byte[]	arrayBuf;

		private UByteBufferHandler()
		{
			arrayBuf	= new byte[ byteBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = (byte) (b[ j ] * 0x7F + 0x80);
					}
				}
				byteBuf.clear();
				byteBuf.put( arrayBuf, 0, m );
				byteBuf.flip();
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				byteBuf.rewind().limit( m );
				fch.read( byteBuf );
				byteBuf.flip();
				byteBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						if( arrayBuf[ i ] < 0 ) { // hmmm, java can't handle unsigned bytes
							b[ j ]	= (float) (0x80 + arrayBuf[ i ]) / 0x7F;
						} else {
							b[ j ]	= (float) (arrayBuf[ i ] - 0x80) / 0x7F;
						}
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	private class ShortBufferHandler
	extends BufferHandler
	{
		private final ShortBuffer	viewBuf;
		private final short[]		arrayBuf;
	
		private ShortBufferHandler()
		{
			byteBuf.clear();
			viewBuf		= byteBuf.asShortBuffer();
			arrayBuf	= new short[ viewBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = (short) (b[ j ] * 0x7FFF);
					}
				}
				viewBuf.clear();
				viewBuf.put( arrayBuf, 0, m );
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.read( byteBuf );
				viewBuf.clear();
				viewBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						b[ j ]	= (float) arrayBuf[ i ] / 0x7FFF;
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	/*
	 *  24bit big endian
	 */
	private class ThreeByteBufferHandler
	extends BufferHandler
	{
		private final byte[]		arrayBuf;
		private final int			chStep = (channels - 1) * 3;
	
		private ThreeByteBufferHandler()
		{
			// note : it's *not* faster to use ByteBuffer.allocate()
			// and ByteBuffer.array() than this implementation
			// (using ByteBuffer.allocateDirect() and bulk get into a separate arrayBuf)
			arrayBuf	= new byte[ byteBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, k, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch * 3, j = offset; i < m; i += chStep, j++ ) {
						k				= (int)  (b[ j ] * 0x7FFFFF);
						arrayBuf[ i++ ] = (byte) (k >> 16);
						arrayBuf[ i++ ] = (byte) (k >> 8);
						arrayBuf[ i++ ] = (byte)  k;
					}
				}
				byteBuf.clear();
				byteBuf.put( arrayBuf, 0, m );
				byteBuf.flip();
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				byteBuf.rewind().limit( m );
				fch.read( byteBuf );
				byteBuf.flip();
				byteBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch * 3, j = offset; i < m; i += chStep, j++ ) {
						b[ j ]	= (float) ((arrayBuf[ i++ ] << 16 ) |
										  ((arrayBuf[ i++ ] & 0xFF) << 8) |
										   (arrayBuf[ i++ ] & 0xFF)) / 0x7FFFFF;
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	/*
	 *  24bit little endian
	 */
	private class ThreeLittleByteBufferHandler
	extends BufferHandler
	{
		private final byte[]		arrayBuf;
		private final int			chStep = (channels - 1) * 3;
	
		private ThreeLittleByteBufferHandler()
		{
			// note : it's *not* faster to use ByteBuffer.allocate()
			// and ByteBuffer.array() than this implementation
			// (using ByteBuffer.allocateDirect() and bulk get into a separate arrayBuf)
			arrayBuf	= new byte[ byteBuf.capacity() ];
		}
		
		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, k, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch * 3, j = offset; i < m; i += chStep, j++ ) {
						k				= (int)  (b[ j ] * 0x7FFFFF);
						arrayBuf[ i++ ] = (byte)  k;
						arrayBuf[ i++ ] = (byte) (k >> 8);
						arrayBuf[ i++ ] = (byte) (k >> 16);
					}
				}
				byteBuf.clear();
				byteBuf.put( arrayBuf, 0, m );
				byteBuf.flip();
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * bytesPerFrame;
				byteBuf.rewind().limit( m );
				fch.read( byteBuf );
				byteBuf.flip();
				byteBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch * 3, j = offset; i < m; i += chStep, j++ ) {
						b[ j ]	= (float) ((arrayBuf[ i++ ] & 0xFF) |
										  ((arrayBuf[ i++ ] & 0xFF) << 8) |
										   (arrayBuf[ i++ ] << 16 )) / 0x7FFFFF;
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	private class IntBufferHandler
	extends BufferHandler
	{
		private final IntBuffer		viewBuf;
		private final int[]			arrayBuf;
	
		private IntBufferHandler()
		{
			byteBuf.clear();
			viewBuf		= byteBuf.asIntBuffer();
			arrayBuf	= new int[ viewBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;

			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = (int) (b[ j ] * 0x7FFFFFFF);
					}
				}
				viewBuf.clear();
				viewBuf.put( arrayBuf, 0, m );
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.read( byteBuf );
				viewBuf.clear();
				viewBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						b[ j ]	= (float) arrayBuf[ i ] / 0x7FFFFFFF;
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	private class FloatBufferHandler
	extends BufferHandler
	{
		private final FloatBuffer	viewBuf;
		private final float[]		arrayBuf;
	
		private FloatBufferHandler()
		{
			byteBuf.clear();
			viewBuf		= byteBuf.asFloatBuffer();
			arrayBuf	= new float[ viewBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = b[ j ];
					}
				}
				viewBuf.clear();
				viewBuf.put( arrayBuf, 0, m );
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.read( byteBuf );
				viewBuf.clear();
				viewBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						b[ j ]	= arrayBuf[ i ];
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

	private class DoubleBufferHandler
	extends BufferHandler
	{
		private final DoubleBuffer	viewBuf;
		private final double[]		arrayBuf;
	
		private DoubleBufferHandler()
		{
			byteBuf.clear();
			viewBuf		= byteBuf.asDoubleBuffer();
			arrayBuf	= new double[ viewBuf.capacity() ];
		}

		protected void writeFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						arrayBuf[ i ] = b[ j ];
					}
				}
				viewBuf.clear();
				viewBuf.put( arrayBuf, 0, m );
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.write( byteBuf );
				length -= chunkLength;
				offset += chunkLength;
			}
		}

		protected void readFrames( float[][] frames, int offset, int length )
		throws IOException
		{
			int		i, j, m, ch, chunkLength;
			float[]	b;
		
			while( length > 0 ) {
				chunkLength = Math.min( frameBufCapacity, length );
				m			= chunkLength * channels;
				byteBuf.rewind().limit( chunkLength * bytesPerFrame );
				fch.read( byteBuf );
				viewBuf.clear();
				viewBuf.get( arrayBuf, 0, m );
				for( ch = 0; ch < channels; ch++ ) {
					b = frames[ ch ];
					if( b == null ) continue;
					for( i = ch, j = offset; i < m; i += channels, j++ ) {
						b[ j ]	= (float) arrayBuf[ i ];
					}
				}
				length -= chunkLength;
				offset += chunkLength;
			}
		}
	}

// -------- AudioFileHeader Klassen --------

	private abstract class AudioFileHeader
	{
		protected static final long SECONDS_FROM_1904_TO_1970 = 2021253247L;
	
		protected abstract void readHeader( AudioFileDescr afd ) throws IOException;
		protected abstract void writeHeader( AudioFileDescr afd ) throws IOException;
		protected abstract void updateHeader( AudioFileDescr afd ) throws IOException;
		protected abstract long getSampleDataOffset();
		protected abstract ByteOrder getByteOrder();

		// WAV might overwrite this
		protected boolean isUnsignedPCM() { return false; }

		// WAV and AIFF might overwrite this
		protected void readMarkers() throws IOException {}

		// AIFF might overwrite this
		protected void readAppCode() throws IOException {}

		protected final int readLittleUShort()
		throws IOException
		{
			int i = raf.readUnsignedShort();
			return( (i >> 8) | ((i & 0xFF) << 8) );
		}

		protected final int readLittleInt()
		throws IOException
		{
			int i = raf.readInt();
			return( ((i >> 24) & 0xFF) | ((i >> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | (i << 24) );
		}

		protected final void writeLittleShort( int i )
		throws IOException
		{
			raf.writeShort( (i >> 8) | ((i & 0xFF) << 8) );
		}

		protected final void writeLittleInt( int i )
		throws IOException
		{
			raf.writeInt( ((i >> 24) & 0xFF) | ((i >> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | (i << 24) );
		}
		
		protected final String readNullTermString()
		throws IOException
		{
			final StringBuffer	buf = new StringBuffer();
			byte				b;
			
			b	= raf.readByte();
			while( b != 0 ) {
				buf.append( (char) b );
				b	= raf.readByte();
			}
			return buf.toString();
		}
	}
	
	private class AIFFHeader
	extends AudioFileHeader
	{
		private static final int FORM_MAGIC		= 0x464F524D;	// 'FORM'
		private static final int AIFF_MAGIC		= 0x41494646;	// 'AIFF'   (offset 8)
		private static final int AIFC_MAGIC		= 0x41494643;	// 'AIFC'   (offset 8)

		// chunk identifiers
		private static final int COMM_MAGIC		= 0x434F4D4D;	// 'COMM'
		private static final int INST_MAGIC		= 0x494E5354;	// 'INST'
		private static final int MARK_MAGIC		= 0x4D41524B;	// 'MARK'
		private static final int SSND_MAGIC		= 0x53534E44;	// 'SSND
		private static final int FVER_MAGIC		= 0x46564552;	// 'FVER
		private static final int APPL_MAGIC		= 0x4150504C;	// 'APPL'
		private static final int COMT_MAGIC		= 0x434F4D54;	// 'COMT'
		private static final int ANNO_MAGIC		= 0x414E4E4F;	// 'ANNO'
		
		// aifc compression identifiers
		private static final int NONE_MAGIC		= 0x4E4F4E45;	// 'NONE' (AIFC-compression)
		private static final int fl32_MAGIC		= 0x666C3332;	// 'fl32' (AIFC-compression)
		private static final int FL32_MAGIC		= 0x464C3332;	// SoundHack variant
		private static final int fl64_MAGIC		= 0x666C3634;
		private static final int FL64_MAGIC		= 0x464C3634;	// SoundHack variant
		private static final int in16_MAGIC		= 0x696E3136;	// we "love" SoundHack for its special interpretations
		private static final int in24_MAGIC		= 0x696E3234;
		private static final int in32_MAGIC		= 0x696E3332;
		private static final int in16LE_MAGIC	= 0x736F7774;	// 'sowt' (16-bit PCM little endian)

		private boolean isAIFC					= true;			// default for writing files
		private static final int AIFCVersion1	= 0xA2805140;	// FVER chunk
//		private static final String NONE_HUMAN	= "uncompressed";
		private static final String fl32_HUMAN	= "32-bit float";
		private static final String fl64_HUMAN	= "64-bit float";

		private long 		sampleDataOffset;
		
		private long 		formLengthOffset	= 4L;
		private long 		commSmpNumOffset;
		private long 		ssndLengthOffset;
		private long 		lastUpdateLength	= 0L;
		// WARNING: this will be queried in openAsWrite, therefore
		// a default is required!!
		private ByteOrder	byteOrder			= ByteOrder.BIG_ENDIAN;
		
		private int			appCodeLen;
		private long		appCodeOff			= 0L;
		private long		markersOffset		= 0L;
		private boolean		loop				= false;
		private int			loopStart			= 0;
		private int			loopEnd				= 0;

		protected void readHeader( AudioFileDescr afd )
		throws IOException
		{
			long			l1, l2, l3, len;
			int				i, i1, i2, chunkLen, essentials, magic;
			byte[]			strBuf;
			boolean			comment			= false;

			raf.readInt();		// FORM
raf.readInt();
// trust the file length more than 32 bit form field which breaks for > 2 GB (> 1 GB if using signed ints)
len = raf.length() - 8;
//			len		= (raf.readInt() + 1) & 0xFFFFFFFE;		// Laenge ohne FORM-Header (Dateilaenge minus 8)
			isAIFC  = raf.readInt() == AIFC_MAGIC;
			len	   -= 4;
			chunkLen = 0;
			
			for( essentials = 2; (len > 0) && (essentials > 0); ) {
				if( chunkLen != 0 ) raf.seek( raf.getFilePointer() + chunkLen );	// skip to next chunk
			
				magic		= raf.readInt();
				chunkLen	= (raf.readInt() + 1) & 0xFFFFFFFE;
				len		   -= chunkLen + 8;

				switch( magic ) {
				case COMM_MAGIC:
					essentials--;
					afd.channels		= raf.readShort();	// # of channels
					commSmpNumOffset	= raf.getFilePointer();
					afd.length			= raf.readInt();	// # of samples
					afd.bitsPerSample	= raf.readShort();	// # of bits per sample
					afd.sampleFormat	= AudioFileDescr.FORMAT_INT;   // default, AIFC will be dealt with later
//					byteOrder			= ByteOrder.BIG_ENDIAN;   // default, AIFC will be dealt with later

					// suckers never die. perhaps the most stupid data format to store a float:
					l1 					= raf.readLong();
					l2	 				= raf.readUnsignedShort();
					l3	 				= l1 & 0x0000FFFFFFFFFFFFL;
					i1					= ((int) (l1 >> 48) & 0x7FFF) - 0x3FFE;
//					afd.rate			= (float) ((((double) l3 * Math.pow( 2.0, i1 - 48 )) +
//												    ((double) l2 * Math.pow( 2.0, i1 - 64 ))) * (l1 < 0 ? -1 : 1));
					afd.rate			= ((l3 * Math.pow( 2.0, i1 - 48 )) +
										   (l2 * Math.pow( 2.0, i1 - 64 ))) * (l1 < 0 ? -1 : 1);

					chunkLen -= 18;
					if( isAIFC ) {
						switch( raf.readInt() ) {
						case NONE_MAGIC:
							break;
						case in16_MAGIC:
							afd.bitsPerSample	= 16;
							break;
						case in24_MAGIC:
							afd.bitsPerSample	= 24;
							break;
						case in32_MAGIC:
							afd.bitsPerSample	= 32;
							break;
						case fl32_MAGIC:
						case FL32_MAGIC:
							afd.bitsPerSample	= 32;
							afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
							break;
						case fl64_MAGIC:
						case FL64_MAGIC:
							afd.bitsPerSample	= 64;
							afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
							break;
						case in16LE_MAGIC:
							afd.bitsPerSample	= 16;
							byteOrder			= ByteOrder.LITTLE_ENDIAN;
							break;
						default:
							throw new IOException( getResourceString( "errAudioFileEncoding" ));
						}
						chunkLen -= 4;
					}
					break;

				case INST_MAGIC:
					raf.readInt();		// char: MIDI Note, Detune, LowNote, HighNote
//					i1					= readInt();	// char: MIDI Note, Detune, LowNote, HighNote
//					b1					= (byte) ((i1 & 0x00FF0000) >> 16);	// Detune in -50...50 Cent
//											// MIDI-Note to Hz (69 = A4 = 440 Hz)
//					stream.base			= (float) (440.0 * Math.pow( 2, ((float) (((i1 & 0x7F000000) >> 24) -
//											69) + (float) b1 / 100.0f) / 12.0f ));
					i1					= raf.readInt();		// char velocityLo, char velocityHi, short gain [dB]
					afd.setProperty( AudioFileDescr.KEY_GAIN,
									 new Float( Math.exp( (double) (i1 & 0xFFFF) / 20 * Math.log( 10 ))));
					i1	 				= raf.readShort();		// Sustain-Loop: 0 = no loop, 1 = fwd, 2 = back
					loop				= i1 != 0;
					i1					= raf.readInt();		// Short Lp-Start-MarkerID, Short End-ID
					loopStart			= (i1 >> 16) & 0xFFFF;
					loopEnd				= i1 & 0xFFFF;
					chunkLen -= 14;
					break;

				case MARK_MAGIC:
					markersOffset = raf.getFilePointer();		// read them out later
					break;

				case SSND_MAGIC:
					essentials--;
					i1 = raf.readInt();			// sample data offset
					raf.readInt();
					sampleDataOffset = raf.getFilePointer() + i1;
					chunkLen -= 8;
					break;
				
				case APPL_MAGIC:
					strBuf		= new byte[ 4 ];
					raf.readFully( strBuf );		// App code
					chunkLen   -= 4;
					afd.appCode	= new String( strBuf );
					appCodeOff	= raf.getFilePointer();
					appCodeLen	= chunkLen;
					break;
				
				case COMT_MAGIC:
					i1		  = raf.readShort();	// number of comments
					chunkLen -= 2;
commentLp:			for( i = 0; !comment && (i < i1); i++ ) {
						raf.readInt();				// time stamp (ignore)
						i2	= raf.readInt();		// markerID << 16 | count
						chunkLen -= 8;
						if( (i2 != 0) && ((i2 >> 16) == 0) ) {		// ok, not empty and not linked to a marker
							strBuf	  = new byte[ i2 ];
							// NOTE: although it states "Pascal String" in AIFF.h
							// all text documents describing the chunk assume a plain string
							// ; PString wouldn't make sense anyway because we have
							// the dedicated count field. Logic Pro 6 writes a PString
							// but leaves count at zero, so this won't get read...
							raf.readFully( strBuf );
							afd.setProperty( AudioFileDescr.KEY_COMMENT, new String( strBuf ));
							if( (i2 & 1) == 1 ) {
								i2++;
								raf.readByte();
							}
							chunkLen   -= i2;
							comment		= true;
							break commentLp;
							
						} else {
							i2		  = (i2 + 1) & 0xFFFE;
							chunkLen -= i2;
							raf.seek( raf.getFilePointer() + i2 );
						}
					}
					break;
					
				case ANNO_MAGIC:
					if( !comment ) {
						strBuf		= new byte[ chunkLen ];
						raf.readFully( strBuf );
						afd.setProperty( AudioFileDescr.KEY_COMMENT, new String( strBuf ));
						chunkLen	= 0;
						comment		= true;
					}
					break;

				default:
					break;
				} // switch( magic )
			} // for( essentials = 2; (len > 0) && (essentials > 0); )
			if( essentials > 0 ) throw new IOException( getResourceString( "errAudioFileIncomplete" ));
		}
		
		protected void writeHeader( AudioFileDescr afd )
		throws IOException
		{
			int				i1, i2;
			String			str;
			byte[]			strBuf;
			Object			o;
			Region			region;
			List			markers;
			Marker			marker;
			double			d1, d2;
			long			pos, pos2;
			boolean			loop;

			isAIFC = afd.sampleFormat == AudioFileDescr.FORMAT_FLOAT;	// floating point requires AIFC compression extension
			raf.writeInt( FORM_MAGIC );
			raf.writeInt( 0 );				// Laenge ohne FORM-Header (Dateilaenge minus 8); unknown now
			raf.writeInt( isAIFC ? AIFC_MAGIC : AIFF_MAGIC );

			// FVER Chunk
			if( isAIFC ) {
				raf.writeInt( FVER_MAGIC );
				raf.writeInt( 4 );
				raf.writeInt( AIFCVersion1 );
			}
			
			// COMM Chunk
			raf.writeInt( COMM_MAGIC );
			pos = raf.getFilePointer();
			raf.writeInt( 0 );				// not known yet
			raf.writeShort( afd.channels );
			commSmpNumOffset = raf.getFilePointer();
			raf.writeInt( 0 );				// updated later
			raf.writeShort( isAIFC ? 16 : afd.bitsPerSample );	// a quite strange convention ...

			// suckers never die.
			i2		= (afd.rate < 0.0) ? 128 : 0;
			d2		= Math.abs( afd.rate  );
			i1		= (int) (Math.log( d2 ) / Math.log( 2 ) + 16383.0) & 0xFFFF;
			d1		= d2 * (1 << (0x401E-i1));	// Math.pow( 2.0, 0x401E - i1 );
			raf.writeShort( (((i2 | (i1 >> 8)) & 0xFF) << 8) | (i1 & 0xFF) );
			raf.writeInt( (int) ((long) d1 & 0xFFFFFFFF) );
			raf.writeInt( (int) ((long) ((d1 % 1.0) * 4294967296.0) & 0xFFFFFFFF) );

			if( isAIFC ) {
				if( afd.bitsPerSample == 32 ) {
					str	= fl32_HUMAN;
					i1	= fl32_MAGIC;
				} else {
					str = fl64_HUMAN;
					i1	= fl64_MAGIC;
				}
				raf.writeInt( i1 );
				raf.writeByte( str.length() );
				raf.writeBytes( str );
				if( (str.length() & 1) == 0 ) {
					raf.writeByte( 0x00 );
//				} else {
//					raf.writeShort( 0x0000 );
				}
			}
			// ...chunk length update...
			pos2 = raf.getFilePointer();
			raf.seek( pos );
			raf.writeInt( (int) (pos2 - pos - 4) );
			raf.seek( pos2 );

			// INST Chunk
			raf.writeInt( INST_MAGIC );
			raf.writeInt( 20 );

//			f1	= (float) (12 * Math.log( (double) stream.base / 440.0 ) / Constants.ln2);
//			i1	= (int) (f1 + 0.5f);
//			b1	= (byte) ((f1 - (float) i1) * 100.0f);
//			writeInt( (((i1 + 69) & 0xFF) << 24) | ((int) b1 << 16) | 0x007F );	// char: MIDI Note, Detune, LowNote, HighNote
			raf.writeInt( (69 << 24) | (0 << 16) | 0x007F );	// char: MIDI Note, Detune, LowNote, HighNote
			
			// XXX the gain information could be updated in updateHeader()
			o = afd.getProperty( AudioFileDescr.KEY_GAIN );
			if( o != null ) {
				i1	= (int) (20 * Math.log( ((Float) o).floatValue() ) / Math.log( 10 ) + 0.5);
			} else {
				i1  = 0;
			}
			raf.writeInt( (0x007F << 16) | (i1 & 0xFFFF) );		// char velLo, char velHi, short gain [dB]

			region  = (Region) afd.getProperty( AudioFileDescr.KEY_LOOP );
			loop	= region != null;
			raf.writeShort( loop ? 1 : 0 );					// No loop vs. loop forward
			raf.writeInt( loop ? 0x00010002 : 0 );			// Sustain-Loop Markers
			raf.writeShort( 0 );							// No release loop
			raf.writeInt( 0 );

			markers  = (List) afd.getProperty( AudioFileDescr.KEY_MARKERS );
			if( markers == null ) markers = new ArrayList();
			// MARK Chunk
			if( loop || !markers.isEmpty() ) {
				raf.writeInt( MARK_MAGIC );
				pos = raf.getFilePointer();
				raf.writeInt( 0 );				// not known yet
				i1	= markers.size() + (loop ? 2 : 0);
				raf.writeShort( i1 );
				i2	= 1;					// ascending marker ID
				if( loop ) {
					raf.writeShort( i2++ );						// loop start ID
					raf.writeInt( (int) region.span.getStart() );	// sample offset
					raf.writeLong( 0x06626567206C7000L );		// Pascal style String: "beg lp"
					raf.writeShort( i2++ );
					raf.writeInt( (int) region.span.getStop() );
					raf.writeLong( 0x06656E64206C7000L );		// Pascal style String: "end lp"
				}
				for( i1 = 0; i1 < markers.size(); i1++ ) {
					raf.writeShort( i2++ );
					marker = (Marker) markers.get( i1 );
					raf.writeInt( (int) marker.pos );
//					raf.writeByte( (marker.name.length() + 1) & 0xFE );
					raf.writeByte( marker.name.length()  & 0xFF );
					raf.writeBytes( marker.name );
					if( (marker.name.length() & 1) == 0 ) {
						raf.writeByte( 0x00 );
//					} else {
//						raf.writeShort( 0x2000 );	// padding space + zero pad to even address
					}
				}
				// ...chunk length update...
				pos2 = raf.getFilePointer();
				raf.seek( pos );
				raf.writeInt( (int) (pos2 - pos - 4) );
				raf.seek( pos2 );
			}

			// COMT Chunk
			str = (String) afd.getProperty( AudioFileDescr.KEY_COMMENT );
			if( (str != null) && (str.length() > 0) ) {
				raf.writeInt( COMT_MAGIC );
				raf.writeInt( (11 + str.length()) & ~1 );
				raf.writeShort( 1 );			// just one comment
				// time stamp "seconds since 1904"; this stupid idea dies around 2030
				// when 32bit unsigned will be overflowed
				raf.writeInt( (int) (System.currentTimeMillis() + SECONDS_FROM_1904_TO_1970) );
				raf.writeShort( 0 );			// no marker association
				raf.writeShort( str.length() );	// count
				raf.writeBytes( str );
				if( (str.length() & 1) == 1 ) {
					raf.writeByte( 0 );			// pad
				}
			}

			// APPL Chunk
			strBuf	= (byte[]) afd.getProperty( AudioFileDescr.KEY_APPCODE );
			if( (afd.appCode != null) && (strBuf != null) ) {
				raf.writeInt( APPL_MAGIC );
				raf.writeInt( 4 + strBuf.length );
				raf.write( afd.appCode.getBytes(), 0, 4 );
				raf.write( strBuf );
			}
			
			// SSND Chunk (Header)
			raf.writeInt( SSND_MAGIC );
			ssndLengthOffset = raf.getFilePointer();
			raf.writeInt( 8 );		// + stream.samples * frameLength );
			raf.writeInt( 0 );		// sample
			raf.writeInt( 0 );		// block size (?!)
			sampleDataOffset = raf.getFilePointer();
			
			updateHeader( afd );
		}
		
		protected void updateHeader( AudioFileDescr afd )
		throws IOException
		{
			final long oldPos	= raf.getFilePointer();
			final long len		= raf.length();
			if( len == lastUpdateLength ) return;
			
			if( len >= formLengthOffset + 4 ) {
				raf.seek( formLengthOffset );
				raf.writeInt( (int) (len - 8) );								// FORM Chunk len
			}
			if( len >= commSmpNumOffset + 4 ) {
				raf.seek( commSmpNumOffset );
				raf.writeInt( (int) afd.length );								// COMM: Sample-Num
			}
			if( len >= ssndLengthOffset + 4 ) {
				raf.seek( ssndLengthOffset );
				raf.writeInt( (int) (len - (ssndLengthOffset + 4)) );			// SSND Chunk len
			}
			raf.seek( oldPos );
			lastUpdateLength = len;
		}
		
		protected long getSampleDataOffset()
		{
			return sampleDataOffset;
		}
		
		protected ByteOrder getByteOrder()
		{
			return byteOrder;
		}
		
		protected void readMarkers()
		throws IOException
		{
			int i, i1, i2, i3;
			
			if( markersOffset > 0L ) {
				final List		markers 	= new ArrayList();
				final byte[]	strBuf 		= new byte[ 64 ];	// to store the names
				final long		oldPos		= raf.getFilePointer();
				int				essentials	= loop ? 2 : 0; 	// start+end for sustain-loop
				try {
					raf.seek( markersOffset );
					i1 = raf.readUnsignedShort();		// number of markers
					for( i = i1; i > 0; i-- ) {
						i3 = raf.readUnsignedShort();	// marker ID
						i2 = raf.readInt();				// marker position (sample offset)
						i1 = raf.readUnsignedByte();	// markerName String-len
						if( loop && i3 == loopStart ) {
							loopStart	= i2;
							essentials--;
						} else if( loop && i3 == loopEnd ) {
							loopEnd		= i2;
							essentials--;
						} else {
							i3	 = Math.min( i1, strBuf.length );
							raf.readFully( strBuf, 0, i3 );
							i1	-= i3;
							if( (i3 > 0) && (strBuf[ i3 - 1 ] == 0x20) ) {
								i3--;	// ignore padding space created by Peak
							}
							markers.add( new Marker( i2, new String( strBuf, 0, i3 )));
						}
						raf.seek( (raf.getFilePointer() + (i1 + 1)) & ~1 );
					}
					afd.setProperty( AudioFileDescr.KEY_MARKERS, markers );
					if( loop && essentials == 0 ) {
						afd.setProperty( AudioFileDescr.KEY_LOOP, new Region( new Span( loopStart, loopEnd ), NAME_LOOP ));
					}
				}
				finally {
					raf.seek( oldPos );
				}
			}
		}
		protected void readAppCode()
		throws IOException
		{
			if( appCodeOff > 0 ) {
				final byte[]	strBuf = new byte[ appCodeLen ];
				final long		oldPos = raf.getFilePointer();
				raf.seek( appCodeOff );
				raf.readFully( strBuf );
				afd.setProperty( AudioFileDescr.KEY_APPCODE, strBuf );
				raf.seek( oldPos );
			} else {
				afd.setProperty( AudioFileDescr.KEY_APPCODE, null );
			}
		}
	} // class AIFFHeader

	// WAVE is the most stupid and chaotic format. there are dozens
	// of alternatives to say the same thing and i'm too lazy to apply higher
	// order heuristics to find out what the creator application was trying
	// to say.
	//
	// therefore we are simply imitating the behaviour of bias pias
	// in terms of marker storage : use LIST/adtl with labl chunk for the marker
	// names and ltxt chunks for regions (purpose field == "rgn "). the loop
	// region is stored outside in the smpl chunk.
	//
	// in other words, if an application stores normal regions in the
	// smpl chunk we'll miss them. so what, blame m****s***
	//
	// http://www.borg.com/%7Ejglatt/tech/wave.htm for a discussion
	private class WAVEHeader
	extends AudioFileHeader
	{
		private static final int RIFF_MAGIC		= 0x52494646;	// 'RIFF'
		private static final int WAVE_MAGIC		= 0x57415645;	// 'WAVE' (offset 8)

		// chunk identifiers
		private static final int FMT_MAGIC		= 0x666D7420;	// 'fmt '
		private static final int FACT_MAGIC		= 0x66616374;	// 'fact'
		private static final int DATA_MAGIC		= 0x64617461;	// 'data'
		private static final int CUE_MAGIC		= 0x63756520;	// 'cue '
		private static final int SMPL_MAGIC		= 0x73616D6C;	// 'smpl'
		private static final int INST_MAGIC		= 0x696E7374;	// 'inst'

		// embedded LIST (peak speak) / list (rest of the universe speak) format
		private static final int LIST_MAGIC		= 0x6C697374;	// 'list'
		private static final int LIST_MAGIC2	= 0x4C495354;	// 'LIST'
		private static final int ADTL_MAGIC		= 0x6164746C;	// 'adtl'
		private static final int LABL_MAGIC		= 0x6C61626C;	// 'labl'
		private static final int LTXT_MAGIC		= 0x6C747874;	// 'ltxt'

		// ltxt purpose for regions
		private static final int RGN_MAGIC		= 0x72676E20;	// 'rgn '

		// fmt format-code
		private static final int FORMAT_PCM		= 0x0001;
		private static final int FORMAT_FLOAT	= 0x0003;
		private static final int FORMAT_EXT		= 0xFFFE;
		
		private long sampleDataOffset;
		
		private long	riffLengthOffset	= 4L;
		private long	dataLengthOffset;
		private long	factSmpNumOffset;
		private long	lastUpdateLength	= 0L;
		private boolean	isFloat				= false;
		private boolean	unsignedPCM;
		
		private long	smplMagicOff		= 0L;
		private long	listMagicOff		= 0L;
		private long	listMagicLen		= 0L;
		private long	cueMagicOff			= 0L;

		protected void readHeader( AudioFileDescr afd )
		throws IOException
		{
			int		i, i1, i2, i3, chunkLen, essentials, magic, dataLen = 0, bytesPerFrame = 0;
			long	len;

			raf.readInt();		// RIFF
raf.readInt();
len	= raf.length() - 8;
//			len		= (readLittleInt() + 1) & 0xFFFFFFFE;		// Laenge ohne RIFF-Header (Dateilaenge minus 8)
			raf.readInt();		// WAVE
			len	   -= 4;
			chunkLen = 0;
			
			for( essentials = 2; (len > 0) && (essentials > 0); ) {
				if( chunkLen != 0 ) raf.seek( raf.getFilePointer() + chunkLen );	// skip to next chunk
			
				magic		= raf.readInt();
				chunkLen	= (readLittleInt() + 1) & 0xFFFFFFFE;
				len		   -= chunkLen + 8;

				switch( magic ) {
				case FMT_MAGIC:
					essentials--;
					i					= readLittleUShort();		// format
					afd.channels		= readLittleUShort();		// # of channels
					i1					= readLittleInt();			// sample rate (integer)
					afd.rate			= i1;
					i2					= readLittleInt();			// bytes per frame and second (=#chan * #bits/8 * rate)
					bytesPerFrame		= readLittleUShort();		// bytes per frame (=#chan * #bits/8)
					afd.bitsPerSample	= readLittleUShort();		// # of bits per sample
					if( ((afd.bitsPerSample & 0x07) != 0) ||
						((afd.bitsPerSample >> 3) * afd.channels != bytesPerFrame) ||
						((afd.bitsPerSample >> 3) * afd.channels * i1 != i2) ) {
											
						throw new IOException( getResourceString( "errAudioFileEncoding" ));
					}
					unsignedPCM			= bytesPerFrame == 1;

					chunkLen -= 16;

					switch( i ) {
					case FORMAT_PCM:
						afd.sampleFormat = AudioFileDescr.FORMAT_INT;
						break;
					case FORMAT_FLOAT:
						afd.sampleFormat = AudioFileDescr.FORMAT_FLOAT;
						break;
					case FORMAT_EXT:
						if( chunkLen < 24 ) throw new IOException( getResourceString( "errAudioFileIncomplete" ));
						i1 = readLittleUShort();	// extension size
						if( i1 < 22 ) throw new IOException( getResourceString( "errAudioFileIncomplete" ));
						i2 = readLittleUShort();	// # valid bits per sample
						raf.readInt();				// channel mask, ignore
						i3 = readLittleUShort();	// GUID first two bytes
						if( (i2 != afd.bitsPerSample) ||
							((i3 != FORMAT_PCM) &&
							(i3 != FORMAT_FLOAT)) ) throw new IOException( getResourceString( "errAudioFileEncoding" ));
						afd.sampleFormat = i3 == FORMAT_PCM ? AudioFileDescr.FORMAT_INT : AudioFileDescr.FORMAT_FLOAT;
						chunkLen -= 10;
						break;
					default:
						throw new IOException( getResourceString( "errAudioFileEncoding" ));
					}
					break;

				case DATA_MAGIC:
					essentials--;
					sampleDataOffset	= raf.getFilePointer();
					dataLen				= chunkLen;
					break;
				
				case CUE_MAGIC:
					cueMagicOff			= raf.getFilePointer();
					break;

				case LIST_MAGIC:
				case LIST_MAGIC2:
					i	= raf.readInt();
					chunkLen -= 4;
					if( i == ADTL_MAGIC ) {
						listMagicOff = raf.getFilePointer();
						listMagicLen = chunkLen;
					} // if( i == ADTL_MAGIC )
					break;

				case SMPL_MAGIC:
					smplMagicOff = raf.getFilePointer() + 28;
					break;
				
				case INST_MAGIC:
					raf.readShort();	// skip UnshiftedNode, FineTune
					i = raf.readByte();	// gain (dB)
					if( i != 0 ) afd.setProperty( AudioFileDescr.KEY_GAIN, new Float( Math.exp(
						(double) i / 20 * Math.log( 10 ))));
					chunkLen -= 3;
					break;
				
				default:
					break;
				} // switch( magic )
			} // for( essentials = 2; (len > 0) && (essentials > 0); )
			if( essentials > 0 ) throw new IOException( getResourceString( "errAudioFileIncomplete" ));
			
			afd.length	= dataLen / bytesPerFrame;
		}
		
		protected void writeHeader( AudioFileDescr afd )
		throws IOException
		{
			int				i, i1, i2, i3;
//			String			str;
			Region			region;
			List			markers, regions;
			Marker			marker;
			long			pos, pos2;
			Object			o;

			isFloat = afd.sampleFormat == AudioFileDescr.FORMAT_FLOAT;	// floating point requires FACT extension
			raf.writeInt( RIFF_MAGIC );
			raf.writeInt( 0 );				// Laenge ohne RIFF-Header (Dateilaenge minus 8); unknown now
			raf.writeInt( WAVE_MAGIC );

			// fmt Chunk
			raf.writeInt( FMT_MAGIC );
			writeLittleInt( isFloat ? 18 : 16 );	// FORMAT_FLOAT has extension of size 0
			writeLittleShort( isFloat ? FORMAT_FLOAT : FORMAT_PCM );
			writeLittleShort( afd.channels );
			i1 = (int) (afd.rate + 0.5);
			writeLittleInt( i1 );
			i2 = (afd.bitsPerSample >> 3) * afd.channels;
			writeLittleInt( i1 * i2 );
			writeLittleShort( i2 );
			writeLittleShort( afd.bitsPerSample );
			
			if( isFloat ) raf.writeShort( 0 );

			// fact Chunk
			if( isFloat ) {
				raf.writeInt( FACT_MAGIC );
				writeLittleInt( 4 );
				factSmpNumOffset = raf.getFilePointer();
				raf.writeInt( 0 );
			}
			
			// cue Chunk
			markers  = (List) afd.getProperty( AudioFileDescr.KEY_MARKERS );
			regions  = (List) afd.getProperty( AudioFileDescr.KEY_REGIONS );
			if( ((markers != null) && !markers.isEmpty()) || ((regions != null) && !regions.isEmpty()) ) {
				if( markers == null ) markers = new ArrayList();
				if( regions == null ) regions = new ArrayList();
				
				raf.writeInt( CUE_MAGIC );
				i2	= markers.size() + regions.size();
				writeLittleInt( 24 * i2 + 4 );
				writeLittleInt( i2 );
				for( i = 0, i1 = 1; i < markers.size(); i++, i1++ ) {
					marker = (Marker) markers.get( i );
					writeLittleInt( i1 );
					writeLittleInt( i1 );
					raf.writeInt( DATA_MAGIC );
					raf.writeLong( 0 );	// ignore dwChunkStart, dwBlockStart
					writeLittleInt( (int) marker.pos );
				}
				for( i = 0; i < regions.size(); i++, i1++ ) {
					region = (Region) regions.get( i );
					writeLittleInt( i1 );
					writeLittleInt( i1 );
					raf.writeInt( DATA_MAGIC );
					raf.writeLong( 0 );	// ignore dwChunkStart, dwBlockStart
					writeLittleInt( (int) region.span.getStart() );
				}
				
				raf.writeInt( LIST_MAGIC );
				pos	= raf.getFilePointer();
				raf.writeInt( 0 );
				raf.writeInt( ADTL_MAGIC );
				
				for( i = 0, i1 = 1; i < markers.size(); i++, i1++ ) {
					marker	= (Marker) markers.get( i );
					i3		= marker.name.length() + 5;
					raf.writeInt( LABL_MAGIC );
					writeLittleInt( i3 );
					writeLittleInt( i1 );
					raf.writeBytes( marker.name );
					if( (i3 & 1) == 0 ) raf.writeByte( 0 ); else raf.writeShort( 0 );
				}
				
				for( i = 0; i < regions.size(); i++, i1++ ) {
					region	= (Region) regions.get( i );
					i3		= region.name.length() + 5;
					raf.writeInt( LABL_MAGIC );
					writeLittleInt( i3 );
					writeLittleInt( i1 );
					raf.writeBytes( region.name );
					if( (i3 & 1) == 0 ) raf.writeByte( 0 ); else raf.writeShort( 0 );
				}
				
				for( i = 0, i1 = markers.size() + 1; i < regions.size(); i++, i1++ ) {
					region	= (Region) regions.get( i );
					raf.writeInt( LTXT_MAGIC );
					writeLittleInt( 21 );
					writeLittleInt( i1 );
					writeLittleInt( (int) region.span.getLength() );
					raf.writeInt( RGN_MAGIC );
					raf.writeLong( 0 );		// wCountry, wLanguage, wDialect, wCodePage
					raf.writeShort( 0 );	// no name (already specified in 'labl' chunk (zero + pad)
				}
				
				// update 'list' chunk size
				pos2 = raf.getFilePointer();
				i	 = (int) (pos2 - pos - 4);
				if( (i & 1) == 1 ) {
					raf.write( 0 );	// padding byte
					pos2++;
				}
				raf.seek( pos );
				writeLittleInt( i );
				raf.seek( pos2 );
				
			} // if marker or region list not empty

			// smpl Chunk
			region  = (Region) afd.getProperty( AudioFileDescr.KEY_LOOP );
			if( region != null ) {
				raf.writeInt( SMPL_MAGIC );
				writeLittleInt( 36 + 24 );
				raf.writeLong( 0 );		// dwManufacturer, dwProduct
				writeLittleInt( (int) (1.0e9 / afd.rate + 0.5) );	// dwSamplePeriod
				writeLittleInt( 69 );	// dwMIDIUnityNote
				raf.writeInt( 0 );		// dwMIDIPitchFraction
				raf.writeLong( 0 );		// dwSMPTEFormat, dwSMPTEOffset
				writeLittleInt( 1 );	// just one loop
				raf.writeInt( 0 );		// no additional chunk information
				
				writeLittleInt( 0 );	// loop gets ID 0
				writeLittleInt( 0 );	// normal loop
				writeLittleInt( (int) region.span.getStart() );
				writeLittleInt( (int) region.span.getStop() );
				raf.writeLong( 0 );		// dwFraction, dwPlayCount
			}

			// inst Chunk
			o = afd.getProperty( AudioFileDescr.KEY_GAIN );
			if( o != null ) {
				i1	= Math.max( -64, Math.min( 63, (int) (20 * Math.log( ((Float) o).floatValue() ) / Math.log( 10 ) + 0.5) ));
				raf.writeInt( INST_MAGIC );
				writeLittleInt( 7 );
				raf.writeShort( (69 << 24) | (0 << 16) );	// char: MIDI Note, Detune
				raf.write( i1 );							// char gain (dB)
				raf.writeInt( 0x007F007F );					// char LowNote, HighNote, velLo, char velHi
				raf.write( 0 );								// pad byte
			}

			// data Chunk (Header)
			raf.writeInt( DATA_MAGIC );
			dataLengthOffset = raf.getFilePointer();
			raf.writeInt( 0 );
			sampleDataOffset = raf.getFilePointer();
			
			updateHeader( afd );
		}
		
		protected void updateHeader( AudioFileDescr afd )
		throws IOException
		{
			long oldPos	= raf.getFilePointer();
			long len	= raf.length();
			if( len == lastUpdateLength ) return;
			
			if( len >= riffLengthOffset + 4 ) {
				raf.seek( riffLengthOffset );
				writeLittleInt( (int) (len - 8) );								// RIFF Chunk len
			}
			if( isFloat && (len >= factSmpNumOffset + 4) ) {
				raf.seek( factSmpNumOffset );
				writeLittleInt( (int) (afd.length * afd.channels) );			// fact: Sample-Num XXX check multich.!
			}
			if( len >= dataLengthOffset + 4 ) {
				raf.seek( dataLengthOffset );
				writeLittleInt( (int) (len - (dataLengthOffset + 4)) );			// data Chunk len
			}
			raf.seek( oldPos );
			lastUpdateLength = len;
		}
		
		protected long getSampleDataOffset()
		{
			return sampleDataOffset;
		}
		
		protected ByteOrder getByteOrder()
		{
			return ByteOrder.LITTLE_ENDIAN;
		}

		protected boolean isUnsignedPCM()
		{
			return unsignedPCM;
		}
		
		protected void readMarkers()
		throws IOException
		{
			if( (smplMagicOff == 0L) && (listMagicOff == 0L) ) return;
			
			final Map	mapCues			= new HashMap();
			final Map	mapCueLengths	= new HashMap();
			final Map	mapCueNames		= new HashMap();
			final long	oldPos			= raf.getFilePointer();
			final List	markers, regions;
			int			i, i1, i2, i3, i4, i5;
			Object		o;
			String		str;
			byte[]		strBuf			= null;

			try {
				if( smplMagicOff > 0L ) {
					raf.seek( smplMagicOff );
					i		  = readLittleInt();	// cSampleLoops
					raf.readInt();					// chunk extension length
		//			chunkLen -= 36;
					if( i > 0 ) {
						i1	= readLittleInt(); 	// dwIdentifier
						o	= new Integer( i1 );
						mapCues.remove( o );
						mapCueLengths.remove( o );
						str	= (String) mapCueNames.remove( o );
						if( str == null ) str = NAME_LOOP;
						afd.setProperty( AudioFileDescr.KEY_LOOP, new Region( new Span(
							readLittleInt(), readLittleInt() ), str ));
		//				chunkLen -= 16;
					}
				}
				if( listMagicOff > 0L ) {
					raf.seek( listMagicOff );
					for( long chunkLen = listMagicLen; chunkLen >= 8; ) {
						i	= raf.readInt();		// sub chunk ID
						i1	= readLittleInt();
						i2	= (i1 + 1) & 0xFFFFFFFE;	// sub chunk length
						chunkLen -= 8;
						switch( i ) {
						case LABL_MAGIC:
							i3		  = readLittleInt();	// dwIdentifier
							i1		 -= 4;
							i2	     -= 4;
							chunkLen -= 4;
							if( strBuf == null || strBuf.length < i1 ) {
								strBuf  = new byte[ Math.max( 64, i1 )];
							}
							raf.readFully( strBuf, 0, i1 );	// null-terminated
							mapCueNames.put( new Integer( i3 ), new String( strBuf, 0, i1 - 1 ));
							chunkLen -= i1;
							i2		 -= i1;
							break;
							
						case LTXT_MAGIC:
							i3			= readLittleInt();	// dwIdentifier
							i4			= readLittleInt();	// dwSampleLength (= frames)
							i5			= raf.readInt();	// dwPurpose
							raf.readLong();					// skip wCountry, wLanguage, wDialect, wCodePage
							i1			-= 20;
							i2			-= 20;
							chunkLen	-= 20;
							o			 = new Integer( i3 );
							if( (i1 > 0) && !mapCueNames.containsKey( o )) {	// don't overwrite names
								if( strBuf == null || strBuf.length < i1 ) {
									strBuf  = new byte[ Math.max( 64, i1 )];
								}
								raf.readFully( strBuf, 0, i1 );	// null-terminated
								mapCueNames.put( o, new String( strBuf, 0, i1 - 1 ));
								chunkLen -= i1;
								i2		 -= i1;
							}
							if( (i4 > 0) || (i5 == RGN_MAGIC) ) {
								mapCueLengths.put( o, new Integer( i4 ));
							}
							break;
							
						default:
							break;
						}
						if( i2 != 0 ) {
							raf.seek( raf.getFilePointer() + i2 );
							chunkLen -= i2;
						}
					} // while( chunkLen >= 8 )
				}
				
				if( cueMagicOff > 0L ) {
					raf.seek( cueMagicOff );
					i	= readLittleInt();	// num cues
					for( int j = 0; j < i; j++ ) {
						i1	= readLittleInt();	// dwIdentifier
						raf.readInt();			// dwPosition (ignore, we don't use playlist)
						i2	= raf.readInt();	// should be 'data'
						raf.readLong();			// ignore dwChunkStart and dwBlockStart
						i3	= readLittleInt();	// dwSampleOffset (fails for 64bit space)
						if( i2 == DATA_MAGIC ) {
							mapCues.put( new Integer( i1 ), new Integer( i3 ));
						}
					}
	//				chunkLen -= i * 24 + 4;
				}
	
				// resolve markers and regions
				if( !mapCues.isEmpty() ) {
					markers = new ArrayList();
					regions	= new ArrayList();
					for( Iterator iter = mapCues.keySet().iterator(); iter.hasNext(); ) {
						o	= iter.next();
						i	= ((Integer) mapCues.get( o )).intValue();	// start frame
						str	= (String) mapCueNames.get( o );
						o	= mapCueLengths.get( o );
						if( o == null ) {	// i.e. marker
							if( str == null ) str = NAME_MARK;
							markers.add( new Marker( i, str ));
						} else {			// i.e. region
							if( str == null ) str = NAME_REGION;
							regions.add( new Region( new Span( i, ((Integer) o).intValue() ), str ));
						}
					}
					if( !markers.isEmpty() ) afd.setProperty( AudioFileDescr.KEY_MARKERS, markers );
					if( !regions.isEmpty() ) afd.setProperty( AudioFileDescr.KEY_REGIONS, regions );
				}
			}
			finally {
				raf.seek( oldPos );
			}
		}
	} // class WAVEHeader

	private class SNDHeader
	extends AudioFileHeader
	{
		private static final int SND_MAGIC		= 0x2E736E64;	// '.snd'

		private long sampleDataOffset;
		private long headDataLenOffset= 8L;
		private long lastUpdateLength = 0L;
		
		protected void readHeader( AudioFileDescr afd )
		throws IOException
		{
			int		i1, i2;
			String	str;
		
			raf.readInt();  // SND magic
			sampleDataOffset= raf.readInt();
			i2				= raf.readInt();
			i1				= raf.readInt();
			afd.rate		= raf.readInt();
			afd.channels	= raf.readInt();
			str				= readNullTermString();
			
			if( str.length() > 0 ) afd.setProperty( AudioFileDescr.KEY_COMMENT, str );

			switch( i1 ) {
			case 2:	// 8 bit linear
				afd.bitsPerSample	= 8;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 3:	// 16 bit linear
				afd.bitsPerSample	= 16;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 4:	// 24 bit linear
				afd.bitsPerSample	= 24;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 5:	// 32 bit linear
				afd.bitsPerSample	= 32;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 6:	// 32 bit float
				afd.bitsPerSample	= 32;
				afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
				break;
			case 7:	// 64 bit float
				afd.bitsPerSample	= 64;
				afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
				break;
			default:
				throw new IOException( getResourceString( "errAudioFileEncoding" ));
			}

			afd.length	= i2 / (((afd.bitsPerSample + 7) >> 3) * afd.channels);
		}
		
		protected void writeHeader( AudioFileDescr afd )
		throws IOException
		{
			String str;
		
			str					= (String) afd.getProperty( AudioFileDescr.KEY_COMMENT );
			sampleDataOffset	= str == null ? 28L : (long) ((28 + str.length()) & ~3);
			raf.writeInt( SND_MAGIC );
			raf.writeInt( (int) sampleDataOffset );
//			raf.writeInt( stream.samples * frameLength );	// len
			raf.writeInt( 0 );

			if( afd.sampleFormat == AudioFileDescr.FORMAT_INT ) {
				raf.writeInt( (afd.bitsPerSample >> 3) + 1 );
			} else {
				raf.writeInt( (afd.bitsPerSample >> 5) + 5 );
			}
			raf.writeInt( (int) (afd.rate + 0.5) );
			raf.writeInt( afd.channels );
			
			// comment
			if( str == null ) {
				raf.writeInt( 0 );  // minimum 4 byte character data
			} else {
				raf.writeBytes( str );
				switch( str.length() & 3 ) {
				case 0:
					raf.writeInt( 0 );
					break;
				case 1:
					raf.writeByte( 0 );
					raf.writeShort( 0 );
					break;
				case 2:
					raf.writeShort( 0 );
					break;
				case 3:
					raf.writeByte( 0 );
					break;
				}
			}

//			updateHeader( afd );
		}
		
		protected void updateHeader( AudioFileDescr afd )
		throws IOException
		{
			long oldPos;
			long len	= raf.length();
			if( len == lastUpdateLength ) return;
			
			if( len >= headDataLenOffset + 4 ) {
				oldPos = raf.getFilePointer();
				raf.seek( headDataLenOffset );
				raf.writeInt( (int) (len - sampleDataOffset) );		// data size
				raf.seek( oldPos );
				lastUpdateLength = len;
			}
		}

		protected long getSampleDataOffset()
		{
			return sampleDataOffset;
		}

		protected ByteOrder getByteOrder()
		{
			return ByteOrder.BIG_ENDIAN;
		}
	} // class SNDHeader

	private class IRCAMHeader
	extends AudioFileHeader
	{
		// http://www.tsp.ece.mcgill.ca/MMSP/Documents/AudioFormats/IRCAM/IRCAM.html
		// for details about the different magic cookies
		private static final int IRCAM_VAXBE_MAGIC		= 0x0001A364;
		private static final int IRCAM_SUNBE_MAGIC		= 0x64A30200;
		private static final int IRCAM_MIPSBE_MAGIC		= 0x0003A364;

		private static final short BICSF_END			= 0;
//		private static final short BICSF_MAXAMP			= 1;
		private static final short BICSF_COMMENT		= 2;
		private static final short BICSF_LINKCODE		= 3;
		private static final short BICSF_VIRTUALCODE	= 4;
		private static final short BICSF_CUECODE		= 8;
//		private static final short BICSF_PARENTCODE		= 11;

		private long sampleDataOffset;

		protected void readHeader( AudioFileDescr afd )
		throws IOException
		{
			int				i1, i2, i3;
			long			l1;
			byte[]			strBuf		= null;
			byte[]			strBuf2;
			List			regions		= new ArrayList();
		
			raf.readInt();		// IRCAM magic
			afd.rate		= raf.readFloat();
			afd.channels	= raf.readInt();
			i1				= raf.readInt();

			switch( i1 ) {
			case 1:	// 8 bit linear
				afd.bitsPerSample	= 8;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 2:	// 16 bit linear
				afd.bitsPerSample	= 16;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 3:	// 24 bit linear; existiert dieser wert offiziell?
				afd.bitsPerSample	= 24;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 0x40004:	// 32 bit linear
				afd.bitsPerSample	= 32;
				afd.sampleFormat	= AudioFileDescr.FORMAT_INT;
				break;
			case 4:	// 32 bit float
				afd.bitsPerSample	= 32;
				afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
				break;
			case 8:	// 64 bit float
				afd.bitsPerSample	= 64;
				afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
				break;
			default:
				throw new IOException( getResourceString( "errAudioFileEncoding" ));
			}

			do {
				i1   = raf.readInt();
				i2	 = i1 & 0xFFFF;		// last short = block size
				i1 >>= 16;				// first short = code
// System.err.println( "next tag: code "+i1+"; len "+i2 );
				switch( i1 ) {
				case BICSF_CUECODE:
					if( strBuf == null ) {
						strBuf = new byte[ 64 ];			// to store the names
					}
					raf.readFully( strBuf );				// region name
					for( i3 = 0; i3 < 64; i3++ ) {
						if( strBuf[ i3 ] == 0 ) break;
					}
					i1	= raf.readInt();					// begin smp
					i2	= raf.readInt();					// end smp
					regions.add( new Region( new Span( i1, i2 ), new String( strBuf, 0, i3 )));
					break;
					
				case BICSF_LINKCODE:
				case BICSF_VIRTUALCODE:
					throw new IOException( getResourceString( "errAudioFileEncoding" ));
				
				case BICSF_COMMENT:
					strBuf2	= new byte[ i2 ];
					raf.readFully( strBuf2 );
					afd.setProperty( AudioFileDescr.KEY_COMMENT, new String( strBuf2 ));
					break;
				
				default:
					raf.seek( raf.getFilePointer() + i2 );		// skip unknown code
					break;
				}
			} while( i1 != BICSF_END );
			
			if( !regions.isEmpty() ) {
				afd.setProperty( AudioFileDescr.KEY_REGIONS, regions );
			}
			
			l1				= raf.getFilePointer();
			sampleDataOffset= (l1 + 1023L) & ~1023L;			// aufgerundet auf ganze kilobyte
			l1				= raf.length() - sampleDataOffset;  // dataLen in bytes
			afd.length		= l1 / (((afd.bitsPerSample + 7) >> 3) * afd.channels);
		}
		
		protected void writeHeader( AudioFileDescr afd )
		throws IOException
		{
			int				i1, i2;
			List			regions;
			Region			region;
			byte[]			strBuf;
			long			pos;
			String			str;
		
			raf.writeInt( IRCAM_VAXBE_MAGIC );
			raf.writeFloat( (float) afd.rate );
			raf.writeInt( afd.channels );

			if( (afd.sampleFormat == AudioFileDescr.FORMAT_INT) && (afd.bitsPerSample == 32) ) {
				i1 = 0x40004;
			} else {
				i1	= afd.bitsPerSample >> 3;		// 1 = 8bit int, 2 = 16bit lin; 3 = 24 bit, 4 = 32bit float, 8 = 64bit float
			}
			raf.writeInt( i1 );

			// markers + regions, loop
			regions  = (List) afd.getProperty( AudioFileDescr.KEY_REGIONS );
			if( regions != null && !regions.isEmpty() ) {
				i1		= (BICSF_CUECODE << 16) + 72;		// short cue-code, short sizeof-cuepoint (64 + 4 + 4)
				strBuf	= new byte[ 64 ];
				strBuf[ 0 ] = 0;
				for( i2 = 0; i2 < regions.size(); i2++ ) {
					region	= (Region) regions.get( i2 );
					raf.writeInt( i1 );		// chunk header
					if( region.name.length() <= 64 ) {
						raf.writeBytes( region.name );
						raf.write( strBuf, 0, 64 - region.name.length() );
					} else {
						raf.writeBytes( region.name.substring( 0, 64 ));
					}
					raf.writeInt( (int) region.span.getStart() );
					raf.writeInt( (int) region.span.getStop() );
				}
			}
			
			// comment
			str	= (String) afd.getProperty( AudioFileDescr.KEY_COMMENT );
			if( str != null ) {
				i1		= (BICSF_COMMENT << 16) | str.length();
				raf.writeInt( i1 );
				raf.writeBytes( str );
			}
			
			raf.writeInt( BICSF_END << 16 );
			pos				= raf.getFilePointer();
			sampleDataOffset= (pos + 1023L) & ~1023L;		// aufgerundet auf ganze kilobyte
			strBuf			= new byte[ (int) (sampleDataOffset - pos) ];
			raf.write( strBuf );							// pad until sample offset
		}
		
		protected void updateHeader( AudioFileDescr afd )
		throws IOException
		{
			// not necessary
		}

		protected long getSampleDataOffset()
		{
			return sampleDataOffset;
		}

		protected ByteOrder getByteOrder()
		{
			return ByteOrder.BIG_ENDIAN;	// XXX at the moment only big endian is supported
		}
	} // class IRCAMHeader

	private class RawHeader
	extends AudioFileHeader
	{
		// this never get's called because
		// retrieveType will never say it's a raw file
		protected void readHeader( AudioFileDescr afd )
		throws IOException
		{}
		
		// naturally a raw file doesn't have a header
		protected void writeHeader( AudioFileDescr afd )
		throws IOException
		{}
		
		protected void updateHeader( AudioFileDescr afd )
		throws IOException
		{}
		
		protected long getSampleDataOffset()
		{
			return 0L;
		}
		
		protected ByteOrder getByteOrder()
		{
			return ByteOrder.BIG_ENDIAN;		// XXX check compatibility, e.g. with csound linux
		}
	} // class RawHeader
} // class AudioFile