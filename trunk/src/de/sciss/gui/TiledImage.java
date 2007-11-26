/*
 *  TiledImage.java
 *  de.sciss.gui package
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
 *		15-Feb-05	created from de.sciss.meloncillo.gui.TiledImage
 *		04-May-06	moved to de.sciss.gui package ; includes Icon
 *		31-Jul-06	added URL and plain Image constructors
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.net.URL;

/**
 *  An <code>Image</code> wrapper
 *  that generates a virtual grid of
 *  sub images which are accessible
 *  by x and y offset and paintable
 *  through a custom <code>paintTile</code>
 *  method.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.57, 31-Jul-06
 */
public class TiledImage
{
	private final Image	img;
	private final int	tileWidth, tileHeight;

	/**
	 *  Creates a new TiledImage from
	 *  an image file on harddisc and
	 *  applies a custom tiling grid.
	 *  This method waits until the image was loaded.
	 *
	 *  @param  imagePath   file name to the image
	 *						(can be relative to the
	 *						application path). allowed
	 *						image formats are GIF, PNG, JPG
	 *  @param  tileWidth   horizontal width of each tile.
	 *						thus number of columns = image width / tile width
	 *  @param  tileHeight  vertical height of each tile.
	 *						thus number of rows = image height / tile height
	 *
	 *  @see	java.awt.Toolkit#getDefaultToolkit()
	 *  @see	java.awt.Toolkit#getImage( String )
	 */
	public TiledImage( String imagePath, int tileWidth, int tileHeight )
	{
		this( Toolkit.getDefaultToolkit().getImage( imagePath ), tileWidth, tileHeight );
	}
	
	/**
	 *  Creates a new TiledImage from
	 *  an image URL and
	 *  applies a custom tiling grid.
	 *  This method waits until the image was loaded.
	 *
	 *  @param  imagePath   <code>URL</code> the image. allowed
	 *						image formats are GIF, PNG, JPG
	 *  @param  tileWidth   horizontal width of each tile.
	 *						thus number of columns = image width / tile width
	 *  @param  tileHeight  vertical height of each tile.
	 *						thus number of rows = image height / tile height
	 */
	public TiledImage( URL imagePath, int tileWidth, int tileHeight )
	{
		this( Toolkit.getDefaultToolkit().getImage( imagePath ), tileWidth, tileHeight );
	}
	
	/**
	 *  Creates a new TiledImage from
	 *  an image applies a custom tiling grid.
	 *  This method waits until the image was loaded.
	 *
	 *  @param  img			the image to use.
	 *  @param  tileWidth   horizontal width of each tile.
	 *						thus number of columns = image width / tile width
	 *  @param  tileHeight  vertical height of each tile.
	 *						thus number of rows = image height / tile height
	 */
	public TiledImage( Image img, int tileWidth, int tileHeight )
	{
		this.img		= img;
		this.tileWidth  = tileWidth;
		this.tileHeight = tileHeight;

		MediaTracker mediaTracker = new MediaTracker( new Container() );
		mediaTracker.addImage( img, 0 );
		try {
			mediaTracker.waitForID( 0 );
		} catch( InterruptedException e1 ) {}
	}
	
	/**
	 *  Queries the tile width
	 *
	 *  @return the width of each tile in pixels,
	 *			as specified in the constructor
	 */
	public int getTileWidth()
	{
		return tileWidth;
	}

	/**
	 *  Queries the tile height
	 *
	 *  @return the height of each tile in pixels,
	 *			as specified in the constructor
	 */
	public int getTileHeight()
	{
		return tileHeight;
	}
	
	/**
	 *  Paints a tile onto a graphics surface.
	 *
	 *  @param  g		<code>Graphics</code> used to draw the image
	 *  @param  x		x offset in the graphics context
	 *  @param  y		y offset in the graphics context
	 *  @param  tileX	column index of the tile (starting at zero)
	 *  @param  tileY	row index of the tile (starting at zero)
	 *  @param  o		asynchronous image update notification receiver
	 *  @return <code>true</code> if the current output representation
	 *			is complete; <code>false</code> otherwise.
	 *
	 *  @see	java.awt.Graphics#drawImage( Image, int, int, int, int, int, int, int, int, ImageObserver )
	 */
	public boolean paintTile( Graphics g, int x, int y, int tileX, int tileY, ImageObserver o )
	{
		int sx = tileX * tileWidth;
		int sy = tileY * tileHeight;
		
		return g.drawImage( img, x, y, x + tileWidth, y + tileHeight,
							sx, sy, sx + tileWidth, sy + tileHeight, o );
	}

	/**
	 *  Creates a new <code>Icon</code> from
	 *  this <code>TiledImage</code>, using one
	 *  particular tile of this image. In this way
	 *  multiply icons can share the same image file
	 *  and just use bits of it.
	 *
	 *  @param  col			tile column index in the tiled image
	 *						(starting at zero)
	 *  @param  row			tile row index in the tiled image
	 *						(starting at zero)
	 */
	public Icon createIcon( int col, int row )
	{
		return new Icon( col, row );
	}

	// ---------------- Icon class ---------------- 

	private class Icon
	implements javax.swing.Icon
	{
		private final int	col, row;

		private Icon( int col, int row )
		{
			this.col	= col;
			this.row	= row;
		}

		/**
		 *  Queries the icon width which is
		 *  identical to the tile width of the underlying
		 *  tiled image.
		 *
		 *  @return the width of the icon in pixels
		 *
		 *  @see	TiledImage#getTileWidth()
		 */
		public int getIconWidth()
		{
			return getTileWidth();
		}

		/**
		 *  Queries the icon height which is
		 *  identical to the tile height of the underlying
		 *  tiled image.
		 *
		 *  @return the height of the icon in pixels
		 *
		 *  @see	TiledImage#getTileHeight()
		 */
		public int getIconHeight()
		{
			return getTileHeight();
		}
		
		/**
		 *  Paints this icon into a graphics context
		 *  belonging to a GUI component.
		 *
		 *  @param  c   the Component to which the icon
		 *				is attached. This is used as
		 *				<code>ImageObserver</code>
		 *  @param  g   the <code>Graphics</code> context to paint into
		 *  @param  x   x offset in pixels in the graphics context
		 *  @param  y   y offset in pixels in the graphics context
		 *
		 *  @see	TiledImage#paintTile( Graphics, int, int, int, int, ImageObserver )
		 */
		public void paintIcon( Component c, Graphics g, int x, int y )
		{
			paintTile( g, x, y, col, row, c );
		}
	}
}