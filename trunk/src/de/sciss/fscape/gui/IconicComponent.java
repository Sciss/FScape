/*
 *  IconicComponent.java
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
 *		24-Jun-06	renamed to IconicComponent
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import javax.swing.*;

/**
 *  Created on Java 1.1 using the
 *	same name as the Swing interface,
 *	this class draws a portion of
 *	a icon collection bitmap graphic.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class IconicComponent
extends JComponent
implements Dragable
{
// -------- public Variablen --------

	public static DataFlavor flavor	= null;		// DataFlavor representing this class

// -------- private Variablen --------

	protected IconBitmap ib;
	protected Dimension d;
	protected int ID;

	private static DataFlavor flavors[] = null;	// alle erlaubten DataFlavors

// -------- public Methoden --------
	// public void setID( int ID );
	// public int getID();

	/**
	 *	@param	ib	IconBitmap, die die Graphik enthaelt
	 *	@param	ID	Icon-ID in der Bitmap-Matrix
	 */
	protected IconicComponent( IconBitmap ib, int ID )
	{
		this.ib	= ib;
		d		= ib.getDimension();
		setSize( getPreferredSize() );
		setID( ID );

		// data flavor
		if( flavor == null ) {
			flavor			= new DataFlavor( getClass(), "Icon" );
			flavors			= new DataFlavor[ 1 ];
			flavors[ 0 ]	= IconicComponent.flavor;
		}
	}

	/**
	 *	@param	ib	IconBitmap, die die Graphik enthaelt
	 */
	protected IconicComponent( IconBitmap ib )
	{
		this( ib, -1 );
	}
	
	/**
	 *	ID des Icons setzen
	 *
	 *	@param	ID	Icon-ID in der Bitmap-Matrix
	 */
	public void setID( int ID )
	{
		this.ID = ID;
	}

	/**
	 *	ID des Icons ermitteln
	 *
	 *	@return	Icon-ID in der Bitmap-Matrix
	 */
	public int getID()
	{
		return ID;
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension( d );
	}
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
	
		Dimension realD = getSize();
		ib.paint( g, ID, (realD.width - d.width) >> 1, (realD.height - d.height) >> 1 );
	}

// -------- Dragable Methoden --------

	/**
	 *	Zeichnet ein Schema des Icons
	 */
	public void paintScheme( Graphics g, int x, int y, boolean mode )
	{
		g.drawRect( x - (d.width>>1), y - (d.height>>1), d.width - 1, d.height - 1 );
	}

// -------- Transferable Methoden --------

	public DataFlavor[] getTransferDataFlavors()
	{
		return flavors;
	}

	public boolean isDataFlavorSupported( DataFlavor fl )
	{
		DataFlavor flavs[] = getTransferDataFlavors();
		for( int i = 0; i < flavs.length; i++ ) {
			if( flavs[ i ].equals( fl )) return true;
		}
		return false;
	}
	
	public Object getTransferData( DataFlavor fl )
	throws UnsupportedFlavorException, IOException
	{
		if( fl.equals( IconicComponent.flavor )) {
			return this;

		} else throw new UnsupportedFlavorException( fl );
	}
}