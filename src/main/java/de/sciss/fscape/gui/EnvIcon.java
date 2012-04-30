/*
 *  EnvIcon.java
 *  FScape
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
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
 *		21-May-05	modernized
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;

import de.sciss.fscape.util.*;

/**
 *  Tool icon with a small representation
 *	of a breakpoint envelope.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class EnvIcon
extends ToolIcon
{
// -------- public Variablen --------

// -------- private Variablen --------

//	private final Frame		win;
	private Envelope		env		= null;
//	private final Image		img		= null;
	
	private static final Color	colrNormal	= new Color( 0x00, 0x00, 0x30, 0xFF );
	private static final Color	colrGhosted	= new Color( 0x00, 0x00, 0x00, 0x7F );
	
// -------- public Methoden --------

	public EnvIcon( final Component parent )
	{
		super( ToolIcon.ID_EDITENV, null );

//		win = parent;
		addMouseListener( new MouseAdapter() {
			public void mouseReleased( MouseEvent e )
			{
				if( !isEnabled() || (env == null) || !contains( e.getPoint() )) return;
			
				EditEnvDlg	envDlg;
				Envelope	result;
				
				envDlg	= new EditEnvDlg( parent, env );
				envDlg.setVisible( true );

				result	= envDlg.getEnvelope();
				if( result != null ) {		// "Ok"
					setEnv( result );
				}

				envDlg.dispose();
			}
		});

//		addComponentListener( new ComponentAdpater() {
//			public void componentResized( ComponentEvent e )
//			{
//				calcPictogram();
//				repaint();
//			}
//		});
	}
	
	public void setEnv( Envelope env )
	{
		this.env = env;
//		calcPictogram();
		repaint();
	}
	
	
	public Envelope getEnv()
	{
		return env;
	}

	public void paintComponent( Graphics g )
	{
		Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g.setColor( isEnabled() ? colrNormal : colrGhosted );
		drawPictogram( g2 );

		super.paintComponent( g );
	}

	public Dimension getPreferredSize()
	{
		return new Dimension( d.width + (d.width >> 1), d.height );
	}
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

// -------- protected Methoden --------

	/*
	 *	Berechnet aus der Envelope eine Pictogram-Darstellung (Polyline)
	 */
	private void drawPictogram( Graphics g )
	{
		if( env == null ) return;

		final int			width			= getWidth();
		final int			height			= getHeight();
		Curve[]				curve			= new Curve[ 3 ];
		double[]			xScale			= { 0.0, 0.0, 0.0 };
		double				fullScale		= 0.0;
		DoublePoint			pt;
		int					numPt			= 0;
		int[]				x, y;
		double				spaceWidth, spaceHeight;

		synchronized( env ) {
			if( env.atkState )	curve[ 0 ]	= env.atkCurve;
			if( env.susState )	curve[ 1 ]	= env.susCurve;
			if( env.rlsState )	curve[ 2 ]	= env.rlsCurve;
			for( int i = 0; i < 3; i++ ) {
				if( curve[ i ] != null ) {
					xScale[ i ]	= (i == 1) ? 0.5 : 0.25;
					numPt	   += curve[ i ].size();
					fullScale  += xScale[ i ];
				}
			}
			if( numPt == 0 ) return;
			
			xScale[ 0 ] /= fullScale;
			xScale[ 1 ] /= fullScale;
			xScale[ 2 ] /= fullScale;
			
			x	= new int[ numPt ];
			y	= new int[ numPt ];

			for( int i = 0, xOffset = 2, k = 0; i < 3; i++ ) {
				if( curve[ i ] == null ) continue;

				spaceWidth	= (curve[ i ].hSpace.max - curve[ i ].hSpace.min);
				spaceHeight	= (curve[ i ].vSpace.max - curve[ i ].vSpace.min);
				
				for( int j = 0; j < curve[ i ].size(); j++, k++ ) {

					pt	= curve[ i ].getPoint( j );

					x[ k ]	= xOffset + (int) ((double) (width-3) * xScale[ i ] *
							  (pt.x - curve[ i ].hSpace.min) / spaceWidth );
					y[ k ]	= 1 + (int) ((double) (height-3) *
							  (curve[ i ].vSpace.max - pt.y) / spaceHeight );
				}
				xOffset += (int) (xScale[ i ] * (width-1));
			}
		}

		g.drawPolyline( x, y, numPt );
	}
}
// class EnvIcon
