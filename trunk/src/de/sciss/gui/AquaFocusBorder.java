/*
 *  AquaFocusBorder.java
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
 *		16-Apr-06	created
 */

package de.sciss.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 14-Oct-06
 */
public class AquaFocusBorder
extends AbstractBorder
{
	private static int aquaColorVariant	= -1;

	private final Insets	insets		= new Insets( 3, 3, 3, 3 );

	private static final Color[] colrFocusGraphite = {
		new Color( 0x96, 0xA5, 0xB7, 0x40 ), new Color( 0x96, 0xA5, 0xB7, 0x80 ),
		new Color( 0x96, 0xA5, 0xB7, 0xC0 ), new Color( 0x96, 0xA5, 0xB7, 0x90 ),
		new Color( 0x96, 0xA5, 0xB7, 0x40 )
	};

	private static final Color[] colrFocusBlue = {
		new Color( 0x64, 0x9C, 0xD1, 0x40 ), new Color( 0x64, 0x9C, 0xD1, 0x80 ),
		new Color( 0x64, 0x9C, 0xD1, 0xC0 ), new Color( 0x64, 0x9C, 0xD1, 0x90 ),
		new Color( 0x64, 0x9C, 0xD1, 0x40 )
	};
	
	private final Color[] colrFocus;

	public AquaFocusBorder()
	{
		super();
		switch( AquaFocusBorder.getAquaColorVariant() ) {
		case 1:		// blue
			colrFocus	= colrFocusBlue;
			break;
		default:	// otherwise use graphite
			colrFocus	= colrFocusGraphite;
			break;
		}
	}
	
	public Insets getBorderInsets( Component c )
	{
		return new Insets( insets.top, insets.left, insets.bottom, insets.right );
	}
	
	public Insets getBorderInsets( Component c, Insets insets )
	{
		insets.top		= this.insets.top;
		insets.left		= this.insets.left;
		insets.bottom	= this.insets.bottom;
		insets.right	= this.insets.right;
		return insets;
	}
	
	public void paintBorder( Component c, Graphics g, int x, int y, int width, int height )
	{
		if( !c.hasFocus() ) return;

		final Graphics2D g2	= (Graphics2D) g;
							
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		for( int j = 0, k = 1; j < 5; j++, k += 2 ) {
			g2.setColor( colrFocus[ j ]);
			g2.drawRoundRect( x + j, y + j, width - k, height - k, 10 - k, 10 - k );
		}
	}

	public static int getAquaColorVariant()
	{
determine: if( aquaColorVariant == -1 ) {
			final Color			colr;
			final BufferedImage img;
			final Graphics2D	g2;
			final int			argb, red, green, blue, blueError, graphiteError, totalError;
			
			colr	= UIManager.getColor( "Menu.selectionBackground" );
			if( colr == null ) {
				aquaColorVariant	= 6;	// graphite on non-mac systems
				break determine;
			}
			img		= new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );
			g2		= img.createGraphics();
			g2.setColor( colr );
			g2.fillRect( 0, 0, 1, 1 );
			g2.dispose();
			argb	= img.getRGB( 0, 0 );
			img.flush();
			red		= (argb & 0x00FF0000) >> 16;
			green	= (argb & 0x0000FF00) >> 8;
			blue	=  argb & 0x000000FF;

			blueError		= (int) Math.sqrt( ((red - 43) * (red - 43) + (green - 107) * (green - 107) + (blue - 206) * (blue - 206)) / 3 );
			graphiteError	= (int) Math.sqrt( ((red - 88) * (red - 88) + (green - 101) * (green - 101) + (blue - 116) * (blue - 116)) / 3 );
			totalError		= blueError + graphiteError;

			if( blueError == 0 ) {
				aquaColorVariant	= 1;	// It's blue for sure.
			} else if( graphiteError == 0 ) {
				aquaColorVariant	= 6;	// It's graphite for sure.
			} else if( 100 * blueError / totalError < 20 ) {
				aquaColorVariant	= 1;	// Look's like blue.
			} else if(  100 * graphiteError / totalError < 20 ) {
				aquaColorVariant	= 6;	// Look's like graphite.
			} else {
				aquaColorVariant	= 6;	// Wow. Must be Mac OS X 11
			}
		}
		return aquaColorVariant;
	}
}