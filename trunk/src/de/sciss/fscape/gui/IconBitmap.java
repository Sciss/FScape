/*
 *  IconBitmap.java
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
 */

package de.sciss.fscape.gui;

import java.awt.*;

/**
 *  Wrapper class for a bitmap image
 *	that gets sectioned into small icons.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class IconBitmap
extends Component
{
// -------- private Variablen --------

	private Image img;
	private Dimension d;
	private int rows, cols;

// -------- public Methoden --------
	// public Dimension getDimension();
	// public paint( Graphics g, int ID, int x, int y );

	/*		Konstruktor
	 *
	 *		arg:	fname = Dateiname des Bildes
	 *				width, height = Breite und Höhe der Icons in Pixels
	 */
	public IconBitmap( String fname, int width, int height )
	{
		img = getToolkit().getImage( fname );
		d = new Dimension( width, height );
		MediaTracker mt = new MediaTracker( this );
		mt.addImage( img, 0 );
		try {
			mt.waitForAll();
		} catch( InterruptedException ex ) {
			// nothing
		}
		cols = img.getWidth( this ) / d.width;
		rows = img.getHeight( this ) / d.height;
	}
	
	/*		Breite und Höhe der Icons ermitteln
	 *
	 *		ret: Dimensionen eines Icons
	 */
	public Dimension getDimension()
	{
		return new Dimension( d );
	}
	
	/*		Icon in einem Graphikkontext ausgeben
	 *
	 *		arg: g = Graphikkontext; ID = Icon-ID; x, y = Offset in g
	 */
	public void paint( Graphics g, int ID, int x, int y )
	{
		Point src = getOffset( ID );
		if( src != null )
		{
			g.drawImage( img, x, y, x + d.width, y + d.height,		// destination
				src.x, src.y, src.x + d.width, src.y + d.height,	// source (Image)
				this
			);
		} else {								// wrong ID, just draw a bevel box
			Color c = g.getColor();
			g.setColor( Color.red );
			g.fill3DRect( x, y, d.width, d.height, true );
			g.setColor( c );
		}
	}

// -------- private Methoden --------
	// private Point getOffset( int ID );

	/*		Position eines Icons in der Bitmap ermitteln
	 *
	 *		arg: ID = Icon-ID
	 *		ret: linke obere Ecke bzw. null bei ungültiger ID
	 */
	private Point getOffset( int ID )
	{
		if( ID < 0 || ID >= cols * rows )
			return null;
		else
			return new Point( (ID % cols) * d.width, (ID / cols) * d.height );
	}
}
// class IconBitmap
