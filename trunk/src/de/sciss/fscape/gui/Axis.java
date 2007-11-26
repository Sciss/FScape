/*
 *  Axis.java
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
 *		16-Feb-05	created from de.sciss.eisenkraut.timeline.TimelineAxis
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.util.*;

/**
 *  A GUI element for displaying
 *  the timeline's axis (ruler)
 *  which is used to display the
 *  time indices and to allow the
 *  user to position and select the
 *  timeline.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class Axis
extends JComponent
implements SwingConstants
{
	private Dimension			recentSize		= null;
	private double				kPeriod			= 1000.0;
	private String[]			labels			= new String[0];
	private int[]				labelPos		= new int[0];
	private final GeneralPath   shpTicks		= new GeneralPath();

	private final int	orient;
	private VectorSpace space;

	private final Paint  pntBackground;
	private static final Font	fntLabel		= new Font( "Helvetica", Font.PLAIN, 10 );

	// the following are used for Number to String conversion using MessageFormat
	private static final String[] msgPtrn = { "{0,number,0}",
											  "{0,number,0.0}",
											  "{0,number,0.00}",
											  "{0,number,0.000}" };
	private final MessageFormat msgForm = new MessageFormat( msgPtrn[0], Locale.US );  // XXX US locale
	private	final Object[]		msgArgs = new Object[1];
	private static final int MIN_LABEL_DISTANCE = 72;   // minimum distance between two time labels in pixels
	
	private static final int[] pntBarGradientPixels = { 0xFFB8B8B8, 0xFFC0C0C0, 0xFFC8C8C8, 0xFFD3D3D3,
														0xFFDBDBDB, 0xFFE4E4E4, 0xFFEBEBEB, 0xFFF1F1F1,
														0xFFF6F6F6, 0xFFFAFAFA, 0xFFFBFBFB, 0xFFFCFCFC,
														0xFFF9F9F9, 0xFFF4F4F4, 0xFFEFEFEF };
	private static final int barExtent = pntBarGradientPixels.length;

	private final AffineTransform trnsVertical = new AffineTransform();

	static {
	}
	
	/**
	 *  @param	orient	either HORIZONTAL or VERTICAL
	 */
	public Axis( int orient )
	{
		super();
		
		this.orient = orient;

		int imgWidth, imgHeight;
		BufferedImage img;
		
		if( orient == HORIZONTAL ) {
			setMaximumSize( new Dimension( getMaximumSize().width, barExtent ));
			setMinimumSize( new Dimension( getMinimumSize().width, barExtent ));
			setPreferredSize( new Dimension( getPreferredSize().width, barExtent ));
			imgWidth	= 1;
			imgHeight	= barExtent;
		} else {
			setMaximumSize( new Dimension( barExtent, getMaximumSize().height ));
			setMinimumSize( new Dimension( barExtent, getMinimumSize().height ));
			setPreferredSize( new Dimension( barExtent, getPreferredSize().height ));
			imgWidth	= barExtent;
			imgHeight	= 1;
		}

		img = new BufferedImage( imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, imgWidth, imgHeight, pntBarGradientPixels, 0, imgWidth );
		pntBackground = new TexturePaint( img, new Rectangle( 0, 0, imgWidth, imgHeight ));

		setOpaque( true );
  	}
	
	public void setSpace( VectorSpace space )
	{
		this.space = space;
		recalcLabels();
	}
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Dimension			d           = getSize();
        Graphics2D			g2          = (Graphics2D) g;
		Stroke				strkOrig	= g2.getStroke();
//		Paint				pntOrig		= g2.getPaint();
		AffineTransform		trnsOrig	= g2.getTransform();
		FontMetrics			fm			= g2.getFontMetrics();
		int					i, y;

		if( recentSize == null || d.width != recentSize.width || d.height != recentSize.height ) {
			recentSize = d;
			recalcLabels( g2 );
			recalcTransforms();
		}

		g2.setPaint( pntBackground );
		g2.fillRect( 0, 0, recentSize.width, recentSize.height );
		g2.setColor( Color.black );
		g2.setFont( fntLabel );

		if( orient == VERTICAL ) {
			g2.transform( trnsVertical );
			y   = recentSize.width - 3 - fm.getMaxDescent();
		} else {
			y   = recentSize.height - 3 - fm.getMaxDescent();
		}
		for( i = 0; i < labels.length; i++ ) {
			g2.drawString( labels[i], labelPos[i], y );
		}
		g2.setColor( Color.lightGray );
		g2.draw( shpTicks );
		g2.setStroke( strkOrig );
		g2.setTransform( trnsOrig );
    }
    
	private void recalcLabels()
	{
//		kPeriod		= 1000.0 / root.getDocument().timeline.getRate();
		recentSize  = null;			// triggers recalcLabels( Graphics2D g )
		repaint();
	}
  
	private void recalcTransforms()
	{
//		trnsVertical.setToRotation( -Math.PI / 2, (double) barExtent / 2,
//												  (double) barExtent / 2 );
		trnsVertical.setToRotation( -Math.PI / 2, (double) recentSize.height / 2,
												  (double) recentSize.height / 2 );
	}
  
	private void recalcLabels( Graphics2D g )
	{
		int			i, j, k, width, height, valueOff, valueStep, numTicks;
		double		scale, pixelOff, pixelStep, tickStep, d1;

		shpTicks.reset();
		if( space == null ) {
			labels		= new String[ 0 ];
			labelPos	= new int[ 0 ];
			return;
		}

		if( orient == HORIZONTAL ) {
			if( space.hlog ) {
				recalcLogLabels( g );
				return;
			}
			width		= recentSize.width;
			height		= recentSize.height;
			scale		= (double) width / (space.hmax - space.hmin);
			i			= width / MIN_LABEL_DISTANCE;			// maximum # of labels
			valueStep	= (int) (kPeriod * (space.hmax - space.hmin) / i);
			d1			= kPeriod * space.hmin; // (double) visibleSpan.getStart() * kPeriod;
		} else {
			if( space.vlog ) {
				recalcLogLabels( g );
				return;
			}
			width		= recentSize.height;
			height		= recentSize.width;
			scale		= (double) width / (space.vmax - space.vmin);
			i			= width / MIN_LABEL_DISTANCE;			// maximum # of labels
			valueStep	= (int) (kPeriod * (space.vmax - space.vmin) / i);
			d1			= kPeriod * space.vmin; // (double) visibleSpan.getStart() * kPeriod;
		}
			
		if( valueStep >= 1000000 ) {
			j   = 0;
			k   = 1000000;
		} else if( valueStep >= 100000 ) {
			j   = 0;
			k   = 100000;
		} else if( valueStep >= 10000 ) {
			j	= 0;
			k	= 10000;
		} else if( valueStep >= 1000 ) {
			j	= 0;
			k	= 1000;
		} else if( valueStep >= 100 ) {
			j	= 1;
			k	= 100;
		} else if( valueStep >= 10 ) {
			j	= 2;
			k	= 10;
		} else {
			j	= 3;
			k	= 1;
		}
		valueStep	= Math.max( 1, (valueStep + (k >> 1)) / k );
		switch( valueStep ) {
		case 2:
		case 4:
		case 8:
			numTicks	= 4;
			break;
		case 3:
		case 6:
			numTicks	= 6;
			break;
		case 7:
		case 9:
			valueStep	= 10;
			numTicks	= 5;
			break;
		default:
			numTicks	= 5;
			break;
		}
		valueStep   *= k;

		msgForm.applyPattern( msgPtrn[j] );
		valueOff	= (int) (d1 / valueStep) * valueStep;
		pixelOff	= (valueOff - d1) / kPeriod * scale + 0.5;
		pixelStep   = valueStep / kPeriod * scale;
		tickStep	= pixelStep / numTicks;
		
		j			= (int) ((width - pixelOff + pixelStep - 1.0) / pixelStep);
		if( labels.length != j ) labels = new String[ j ];
		if( labelPos.length != j ) labelPos = new int[ j ];

//System.err.println( "valueOff = "+valueOff+"; valueStep = "+valueStep+"; pixelStep "+pixelStep+"; tickStep "+tickStep+
//					"; test "+(j * tickStep + pixelOff)+ "; pixelOff "+pixelOff+"; d1 "+d1 );
		for( i = 0; i < j; i++ ) {
			msgArgs[0]  = new Double( (double) valueOff / kPeriod );
			labels[i]   = msgForm.format( msgArgs );
			labelPos[i] = (int) pixelOff + 2;
			valueOff    += valueStep;
			shpTicks.moveTo( (float) pixelOff, 1 );
			shpTicks.lineTo( (float) pixelOff, height - 2 );
			pixelOff += tickStep;
			for( k = 1; k < numTicks; k++ ) {
				shpTicks.moveTo( (float) pixelOff, height - 4 );
				shpTicks.lineTo( (float) pixelOff, height - 2 );
				pixelOff += tickStep;
			}
		}
	}

	private void recalcLogLabels( Graphics2D g )
	{
		int				i, j, k, m, n, p, width, height, numTicks, mult, expon;
		double			spaceOff, factor, d1, pixelOff, min, max;

		if( orient == HORIZONTAL ) {
			width		= recentSize.width;
			height		= recentSize.height;
			min			= space.hmin;
			max			= space.hmax;
		} else {
			width		= recentSize.height;
			height		= recentSize.width;
			min			= space.vmin;
			max			= space.hmax;
		}
		
		factor	= Math.pow( max / min, (double) MIN_LABEL_DISTANCE / (double) width );
		expon	= (int) (Math.log( factor ) / Constants.ln10);
		mult	= (int) (Math.ceil( factor / Math.pow( 10, expon )) + 0.5);
		if( mult > 5 ) {
			expon++;
			mult = 1;
		} else if( mult > 3 ) {
			mult = 4;
		} else if( mult > 2 ) {
			mult = 5;
		}
		factor	= mult * Math.pow( 10, expon );
		
		j = (int) (Math.ceil( Math.log( max/min ) / Math.log( factor )) + 0.5);
		if( labels.length != j ) labels = new String[ j ];
		if( labelPos.length != j ) labelPos = new int[ j ];

//		if( (min * (factor - 1.0)) % 10 == 0.0 ) {
//			numTicks	= 10;
//		} else {
			numTicks	= 8;
//		}
//		tickFactor	= Math.pow( factor, 1.0 / numTicks );

//System.err.println( "factor "+factor+"; expon "+expon+"; mult "+mult+"; tickFactor "+tickFactor+"; j "+j );

		p = -1;

		for( i = 0; i < j; i++ ) {
			spaceOff	= min * Math.pow( factor, i );
			for( k = 1000, m = 3; k > 1 && (((spaceOff * k) % 1.0) == 0); k /= 10, m-- ) ;
			if( p != m ) {
				msgForm.applyPattern( msgPtrn[m] );
				p = m;
			}

			if( orient == HORIZONTAL ) {
				pixelOff	= space.hSpaceToUnity( spaceOff ) * width;
			} else {
				pixelOff	= space.vSpaceToUnity( spaceOff ) * width;
			}
//System.err.println( "#"+i+" : spaceOff = "+spaceOff+"; pixelOff "+pixelOff );
			msgArgs[0]  = new Double( spaceOff );
			labels[i]   = msgForm.format( msgArgs );
			labelPos[i] = (int) pixelOff + 2;
			shpTicks.moveTo( (float) pixelOff, 1 );
			shpTicks.lineTo( (float) pixelOff, height - 2 );
			d1			= spaceOff * (factor - 1) / numTicks;
			for( n = 1; n < numTicks; n++ ) {
				if( orient == HORIZONTAL ) {
					pixelOff	= space.hSpaceToUnity( spaceOff + d1 * n ) * width;
				} else {
					pixelOff	= space.vSpaceToUnity( spaceOff + d1 * n ) * width;
				}
				shpTicks.moveTo( (float) pixelOff, height - 4 );
				shpTicks.lineTo( (float) pixelOff, height - 2 );
			}
		}
	}
}