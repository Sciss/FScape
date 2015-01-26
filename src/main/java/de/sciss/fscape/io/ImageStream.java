/*
 *  ImageStream.java
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
 
package de.sciss.fscape.io;		// bad choice actually

public class ImageStream
{
// -------- public Variablen --------

	/**
	 *	Width and Height in Pixels
	 */
	public int		width			= 0;
	public int		height			= 0;

	/**
	 *	Dimensional Resolution
	 */
	public float	hRes			= 0.0f;
	public float	vRes			= 0.0f;
	public int		resUnit			= RES_NONE;
	
	public static final int RES_NONE	= 0x0001;		// MUST BE TIFF-compatible!!
	public static final int RES_INCH	= 0x0002;
	public static final int RES_CM		= 0x0003;
	
	/**
	 *	Color Resolution
	 */
	public int		smpPerPixel		= 0;		// 1 for grayscale, 3 for RGB
	public int		bitsPerSmp		= 0;		// 8 or 16

	/**
	 *	Additional Description String
	 */
	public String	descr			= null;

	/**
	 *	I/O progress
	 */
	public int		rowsWritten		= 0;
	public int		rowsRead		= 0;
	
// -------- public Methoden --------

	public ImageStream()
	{
		// nothing
	}

	public ImageStream( ImageStream orig )
	{
		this.width		= orig.width;
		this.height		= orig.height;
		this.hRes		= orig.hRes;
		this.vRes		= orig.vRes;
		this.resUnit	= orig.resUnit;
		this.smpPerPixel= orig.smpPerPixel;
		this.bitsPerSmp	= orig.bitsPerSmp;
		this.descr		= orig.descr;
	}		

// XXX QUICKTIME
/*
	public ImageStream( MovieStream orig )
	{
		this.width		= orig.width;
		this.height		= orig.height;
		this.hRes		= orig.hRes;
		this.vRes		= orig.vRes;
		this.resUnit	= orig.resUnit;
		this.smpPerPixel= 3;
		this.bitsPerSmp	= 8;	// default;
		this.descr		= orig.descr;
	}
*/
	public Object clone()
	{
		return new ImageStream( this );
	}

	/**
	 *	Format-String erzeugen
	 */
	public static String getFormat( ImageStream stream )
	{
		return( "TIFF image; " + stream.bitsPerSmp + "-bit " + ((stream.smpPerPixel == 1) ?
					"grayscale; " : "colour; ") +
				stream.width + " x " + stream.height + " pixels" );
	}
}
// class ImageStream
