/*
 *  AudioFileRegion.java
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
 *		15-Aug-05	created
 */

package de.sciss.io;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *	A small class that allows
 *	inter-application drag-and-drop
 *	of regions of audio files.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.15, 05-May-06
 */
public class AudioFileRegion
implements Serializable, Cloneable, Transferable
{
	public final File	file;
	public final Region	region;

	public static DataFlavor flavor = new DataFlavor( AudioFileRegion.class, "Audio File Region" );

	private static final DataFlavor[] supportedFlavors = {
		DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor, flavor
	};
	
	public AudioFileRegion( File file, Region region )
	{
		this.file	= file;
		this.region	= region;
	}

	public AudioFileRegion( AudioFile af, Region region )
	{
		this.file	= af.getFile();
		this.region	= region;
	}

	public AudioFileRegion( File file, Span region )
	{
		this.file	= file;
		this.region	= new Region( region, file.getName() );
	}
	
// ------------- Cloneable interface -------------

	/**
	 *	Returns a new audio file region which is
	 *	equal to this one. <code>CloneNotSupportedException</code>
	 *	is never thrown.
	 *
	 *	@return		a new audio file region identical to this one
	 */
	public Object clone()
	throws CloneNotSupportedException
	{
		return super.clone();	// field by field copy
	}

// ------------- Transferable interface -------------

	public DataFlavor[] getTransferDataFlavors()
	{
		return supportedFlavors;
	}
	
	public boolean isDataFlavorSupported( DataFlavor flavor )
	{
		for( int i = 0; i < supportedFlavors.length; i++ ) {
			if( supportedFlavors[ i ].equals( flavor )) return true;
		}
		return false;
	}
	
	public Object getTransferData( DataFlavor flavor )
	throws UnsupportedFlavorException, IOException
	{
		if( flavor.equals( AudioFileRegion.flavor )) {
			return this;
		} else if( flavor.equals( DataFlavor.javaFileListFlavor )) {
			final List coll = new ArrayList( 1 );
			coll.add( file );
			return coll;
		} else if( flavor.equals( DataFlavor.stringFlavor )) {
			return region.name;
		} else {
			throw new UnsupportedFlavorException( flavor );
		}
	}
}