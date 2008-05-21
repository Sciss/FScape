/*
 *  GenericFile.java
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
 *		07-Jan-05	added WAVE support; switched to AudioFile, killed SoundFile
 */

package de.sciss.fscape.io;

import java.io.*;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import de.sciss.fscape.spect.SpectralFile;

import net.roydesign.mac.MRJAdapter;

/**
 *  @version	0.71, 14-Nov-07
 */
public class GenericFile
extends RandomAccessFile
{
// -------- public Variablen --------

	/**
	 *	File will be an Input resp. Output Object
	 */
	public static final int MODE_INPUT		= 0x00;
	public static final int MODE_OUTPUT		= 0x01;
	public static final int MODE_FILEMASK	= 0x0F;
	/**
	 *	Type specific
	 */
	public static final int MODE_GENERIC	= 0x0000;
	public static final int MODE_TIFF		= 0x0010;
	public static final int MODE_AIFF		= 0x0020;
	public static final int MODE_SND		= 0x0030;
	public static final int MODE_SPECT		= 0x0040;
	public static final int MODE_FLOAT		= 0x0050;
	public static final int MODE_MPEG		= 0x0060;
	public static final int MODE_MOV		= 0x0070;
	public static final int MODE_IRCAM		= 0x0080;
	public static final int MODE_WAVE		= 0x0090;
	public static final int MODE_TYPEMASK	= 0xFFF0;

	public static final int TYPES_IMAGE[]	= { MODE_TIFF };
	public static final int TYPES_MOVIE[]	= { MODE_MPEG, MODE_MOV };
	public static final int TYPES_SOUND[]	= { MODE_AIFF, MODE_SND, MODE_IRCAM, MODE_WAVE };
	public static final int TYPES_SPECT[]	= { MODE_SPECT };

	public static final int MODE_SPECIALMASK= 0xFF0000;	// individuell AudioFile, ImageFile, ...
	
	public int	mode;				// MODE_...

	public static final String ERR_ILLEGALFILE	= "Unsupported file format";
	public static final String ERR_CORRUPTED	= "File is corrupted";
	public static final String ERR_MISSINGDATA	= "Essential data missing";
	public static final String ERR_UNSUPPORTED	= "Unsupported encoding";

// -------- private Variablen --------

	protected File file;

	// First 4 bytes
	protected static final int TIFF_MAC_MAGIC	= 0x4D4D002A;	// 'MM' Version 42
	protected static final int TIFF_IBM_MAGIC	= 0x4949002A;	// 'II' Version 42
	protected static final int FORM_MAGIC		= 0x464F524D;	// 'FORM'
	protected static final int AIFF_MAGIC		= 0x41494646;	// 'AIFF'
	protected static final int AIFC_MAGIC		= 0x41494643;	// 'AIFC'
	protected static final int SND_MAGIC		= 0x2E736E64;	// '.snd'
	protected static final int CSA_MAGIC		= 517730;
	protected static final int SHA_MAGIC		= 0x45726265;	// 'Erbe'
	protected static final int FLOAT_MAGIC		= 0x46536346;	// 'FScF' (FScape Float)
	protected static final int MPEG_MAGIC1		= 0x000001B3;
	protected static final int MPEG_MAGIC2		= 0x000001BA;
	protected static final int MOV_MAGIC		= 0x6D6F6F76;	// 'moov'
	protected static final int IRCAM_MAGIC		= 0x0001A364;
	protected static final int RIFF_MAGIC		= 0x52494646;	// 'RIFF'
	protected static final int WAVE_MAGIC		= 0x57415645;	// 'WAVE'

	// index corresponds to MODE_TYPE >> TYPESHIFT
	protected static final int TYPESHIFT		= 4;
												//  "????"		"TIFF"		"AIFF"		"NeXT"		"DATA"		"FScF"		"MPEG"		"MooV"		"IRCM"        "WAVE"
	protected static final int FILETYPE[]		= { 0x3F3F3F3F, 0x54494646, 0x41494646, 0x4E655854, 0x44415441, 0x46536346, 0x4D504547, 0x4D6F6F56, 0x4952434D, 0x57415645  };
	protected static final String FILETYPESTR[]	= { "????"	,	"TIFF"	,	"AIFF"	,	"NeXT"	,	"DATA"	,	"FScF"	,	"MPEG"	,	"MooV"	,	"IRCM",       "WAVE" };
	protected static final String EXTSTR[]		= { ".???", ".tif", ".aif", ".snd", ".spc", ".dat", ".mpg",
													".mov", ".irc", ".wav" };
	protected static final String TYPEDESCR[]	= {	"Unknown type", "Image: TIFF", "Waveform: AIFF",
													"Waveform: Snd", "Spectral: SndHack/FScape",
													"Float stream", "Movie: MPEG", "Movie: QuickTime",
													"Waveform: IRCAM", "Waveform: WAVE" };
												// array-index = MainPrefs.KEY_IOBUFSIZE
//	private static final int[] BUFSIZE			= { 8192, 32768, 131072 };

// -------- public Methoden --------

	/**
	 *	Datei, die Daten enthaelt bzw. enthalten soll, oeffnen
	 *
	 *	@param f		entsprechende Datei
	 *	@param mode		MODE_INPUT zum Lesen, MODE_OUTPUT zum Schreiben
	 */
	public GenericFile( File f, int mode )
	throws IOException
	{
		super( prepareOpen( f, mode ), ((mode & MODE_FILEMASK) == MODE_OUTPUT) ? "rw" : "r" );

		this.mode	= mode;
		this.file	= f;
		
		if( (mode & MODE_FILEMASK) == MODE_INPUT ) {
			retrieveType();		// check if it's TIFF, AIFF etc. and modifiy 'mode' field
		}
	}
	
	public GenericFile( String fname, int mode )
	throws IOException
	{
		this( new File( fname ), mode );
	}

	/**
	 *	Datei schliessen
	 */
	public void close()
	throws IOException
	{
		super.close();

		if( (mode & MODE_FILEMASK) == MODE_OUTPUT ) {

//			try {
//				String type	= getFileTypeStr();
//				String val	= Main.getPrefs().getProperty( MainPrefs.KEY_CREATOR + type );
// XXX causes trouble: AIFF files appear to be QuickTime Movies???
//				MRJAdapter.setFileCreatorAndType( file.getAbsoluteFile(), type, "FSc " ); // Constants.OSTypeFScape );
//			} catch( IOException e2 ) {}	// Filetype nicht essentiell
		}
	}
	
	/**
	 *	Datei aufraeumen; ggf. schliessen
	 */	
	public void cleanUp()
	{
		try {
			close();
		}
		catch( IOException e ) {}		// jetzt nicht mehr
	}

	public String getTypeDescr()
	{
		return getTypeDescr( mode );
	}

	public static String getTypeDescr( int mode )
	{
		return( TYPEDESCR[ (mode & MODE_TYPEMASK) >> TYPESHIFT ]);
	}
	
	public int getFileType()
	{
		return getFileType( mode );
	}

	public String getFileTypeStr()
	{
		return getFileTypeStr( mode );
	}
	
	public static int getFileType( int mode )
	{
		return( FILETYPE[ (mode & MODE_TYPEMASK) >> TYPESHIFT ]);
	}
	
	public static String getFileTypeStr( int mode )
	{
		return( FILETYPESTR[ (mode & MODE_TYPEMASK) >> TYPESHIFT ]);
	}

	public static String getExtStr( int mode )
	{
		return( EXTSTR[ (mode & MODE_TYPEMASK) >> TYPESHIFT ]);
	}
	
	/**
	 *	Aus Type-Description String den mode-Wert ermitteln
	 *	ebenso moeglich ist uebergabe eines File-Types oder File-Endung
	 *
	 *	@return	MODE_GENERIC, wenn kein anderer Typ gefunden wurde
	 */
	public static int getType( String typeStr )
	{
		for( int i = 0; i < TYPEDESCR.length; i++ ) {
			if( TYPEDESCR[ i ].equals( typeStr )) return( i << TYPESHIFT );
		}
		for( int i = 0; i < FILETYPE.length; i++ ) {			// retry with abstract types
			if( FILETYPESTR[ i ].equals( typeStr )) return( i << TYPESHIFT );
		}
		for( int i = 0; i < EXTSTR.length; i++ ) {			// retry with abstract types
			if( typeStr.endsWith( EXTSTR[ i ])) return( i << TYPESHIFT );
		}
		return MODE_GENERIC;
	}

	/**
	 *	subclasses MUST override
	 */
	public String getFormat()
	throws IOException
	{
		String		format;
		GenericFile	f2		= null;
	
		switch( mode & MODE_TYPEMASK ) {
		case MODE_TIFF:
			f2 = new ImageFile( file, MODE_INPUT );
			try {
				format = f2.getFormat();
				f2.cleanUp();
				return format;
			}
			catch( IOException e1 ) {
				f2.cleanUp();
				throw e1;
			}
			
		case MODE_AIFF:
		case MODE_SND:
		case MODE_IRCAM:
		case MODE_WAVE:
			AudioFile af = AudioFile.openAsRead( file );
			AudioFileDescr afd = af.getDescr();
			format = afd.getFormat();
			af.cleanUp();
			return format;

		case MODE_SPECT:
			f2 = new SpectralFile( file, MODE_INPUT );
			try {
				format = f2.getFormat();
				f2.cleanUp();
				return format;
			}
			catch( IOException e1 ) {
				f2.cleanUp();
				throw e1;
			}
// XXX QUICKTIME
//		case MODE_MPEG:
//		case MODE_MOV:
//			f2 = new MovieFile( file, MODE_INPUT );
//			break;

		default:
			throw new IOException( ERR_ILLEGALFILE );
		}
	}

// -------- private Methoden --------

	/*
	 *	Default BufferSize ermitteln
	 */
//	protected int getDefaultBufSize()
//	{
//		int	i = Math.max( 0, Math.min( BUFSIZE.length - 1,
//					AbstractApplication.getApplication().getUserPrefs().getInt( MainPrefs.KEY_IOBUFSIZE, 0 )));
//
//		return BUFSIZE[ i ];
//	}
	
	/*
	 *	Datei zum Oeffnen vorbereiten; konkret: loeschen, wenn sie im Schreibmodus schon existiert
	 */
	private static synchronized File prepareOpen( File f, int mode )
	{
		if( (mode & MODE_FILEMASK) == MODE_OUTPUT )
		{
			// ensure the file is killed before we open it to write to
			if( f.length() > 0L ) f.delete();
		}
		return f;	// Java austricksen im Konstruktor; wir sind schneller als SuperClass ;)
	}

	/*
	 *	Datei Header einlesen, um Filetype zu ermitteln
	 */
	private void retrieveType()
	throws IOException
	{
		long		oldpos	= getFilePointer();
		seek( 0L );
		int			magic	= readInt();
		int			type	= MODE_GENERIC;
		String		osType;
	
		switch( magic ) {
		case TIFF_MAC_MAGIC:		// -------- TIFF image --------
		case TIFF_IBM_MAGIC:
			type = MODE_TIFF;
			break;

		case FORM_MAGIC:			// -------- probably AIFF --------
			readInt();
			magic = readInt();
			switch( magic ) {
			case AIFC_MAGIC:
			case AIFF_MAGIC:
				type = MODE_AIFF;
				break;
			default:
				break;
			}
			break;

		case SND_MAGIC:				// -------- snd sound --------
			type = MODE_SND;
			break;

		case IRCAM_MAGIC:			// -------- IRCAM sound --------
			type = MODE_IRCAM;
			break;

		case RIFF_MAGIC:			// -------- probably WAVE --------
			readInt();
			magic = readInt();
			switch( magic ) {
			case WAVE_MAGIC:
				type = MODE_WAVE;
				break;
			default:
				break;
			}
			break;

		case SHA_MAGIC:				// -------- spect file --------
		case CSA_MAGIC:
			type = MODE_SPECT;
			break;
			
		case FLOAT_MAGIC:			// -------- float file --------
			type = MODE_FLOAT;
			break;

		case MPEG_MAGIC1:			// -------- movie --------
		case MPEG_MAGIC2:
			type = MODE_MPEG;
			break;

		default:
			magic	= readInt();	// offset 4
			if( magic == MOV_MAGIC ) {
				type = MODE_MOV;
			} else {				// QT may use File Resource for Movie Identification
				osType = MRJAdapter.getFileType( file.getAbsoluteFile() );
				if( osType.equals( FILETYPESTR[ MODE_MOV >> TYPESHIFT ])) {
					type = MODE_MOV;
				}
			}
			break;
		}

		mode = (mode & ~MODE_TYPEMASK) | type;
		seek( oldpos );
	}
}
// class GenericFile
