/*
 *  VectorDisplay.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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
 *		09-Jan-05	copied from Meloncillo and simplified (non-editable)
 */

package de.sciss.fscape.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import javax.swing.JComponent;

/**
 *  A <code>VectorDisplay</code> is a two dimensional
 *  panel which plots a sampled function (32bit float) and allows
 *  the user to edit this one dimensional data vector
 *  (or table, simply speaking). It implements
 *  the <code>EventManager.Processor</code> interface
 *  to handle custom <code>VectorDisplayEvent</code>s
 *  and implements the <code>VirtualSurface</code>
 *  interface to allow transitions between screen
 *  space and normalized virtual space.
 *  <p>
 *  Often it is convenient to attach a popup menu
 *  created by the static method in <code>VectorTransformer</code>.
 *  <p>
 *  Examples of using <code>VectorDisplay</code>s aer
 *  the <code>SigmaReceiverEditor</code> and the
 *  <code>SimpleTransmitterEditor</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.65, 11-Aug-04
 *
 *  @see		de.sciss.meloncillo.math.VectorTransformer#createPopupMenu( VectorDisplay )
 *  @see		VectorDisplayListener
 *  @see		VectorDisplayEvent
 *  @see		de.sciss.meloncillo.receiver.SigmaReceiverEditor
 *  @see		de.sciss.meloncillo.transmitter.SimpleTransmitterEditor
 *
 *  @todo		a vertical (y) wrapping mode should be implemented
 *				similar to x-wrap, useful for circular value spaces like angles
 *  @todo		due to a bug in horizontal wrapping, the modified span
 *				information is wrong?
 */
public class VectorDisplay
extends JComponent  // extends JPanel
// implements  
{
	private float[]		vector;

	private final GeneralPath   path		= new GeneralPath();
	private Shape				pathTrns;
	private TextLayout			txtLay		= null;
	private Rectangle2D			txtBounds;
	private Dimension			recentSize;
	private Image				image		= null;

	private static final Stroke	strkLine	= new BasicStroke( 0.5f );
	private static final Paint	pntArea		= new Color( 0x42, 0x5E, 0x9D, 0x7F );
	private static final Paint	pntLine		= Color.black;
	private static final Paint	pntLabel	= new Color( 0x00, 0x00, 0x00, 0x3F );
	
	private final AffineTransform trnsScreenToVirtual  = new AffineTransform();
	private final AffineTransform trnsVirtualToScreen  = new AffineTransform();

	private float		min			= 0.0f;		// minimum vector value
	private float		max			= 1.0f;		// maximum vector value
	private boolean		fillArea	= true;		// fill area under the vector polyline
	private String		label		= null;		// optional text label

	// --- top painter ---

	private final Vector collTopPainters = new Vector();
	
	/**
	 *  Creates a new VectorDisplay with an empty vector.
	 *  The defaults are wrapX = false, wrapY = false,
	 *  min = 0, max = 1.0, fillArea = true, no label
	 */
	public VectorDisplay()
	{
		this( new float[0] );
	}
	
	/**
	 *  Creates a new VectorDisplay with an given initial vector.
	 *  The defaults are wrapX = false, wrapY = false,
	 *  min = 0, max = 1.0, fillArea = true, no label
	 *
	 *  @param  vector  the initial vector data
	 */
	public VectorDisplay( float[] vector )
	{
		setOpaque( false );
		setMinimumSize( new Dimension( 64, 16 ));
//		setPreferredSize( new Dimension( 288, 144 )); // XXX
		recentSize  = getMinimumSize();
		setVector( null, vector );
//		addComponentListener( this );
	}

	/**
	 *  Replaces the existing vector by another one.
	 *  This dispatches a <code>VectorDisplayEvent</code>
	 *  to registered listeners.
	 *
	 *  @param  source  the source in the <code>VectorDisplayEvent</code>.
	 *					use <code>null</code> to prevent event dispatching.
	 *  @param  vector  the new vector data
	 */
	public void setVector( Object source, float[] vector )
	{
		this.vector = vector;
		
		recalcPath();
		repaint();
	}
	
	/**
	 *  Gets the current data array.
	 *
	 *  @return		the current vector data of the editor. valid data
	 *				is from index 0 to the end of the array.
	 *
	 *  @warning			the returned array is not a copy and therefore
	 *						any modifications are forbidden. this also implies
	 *						that relevant data be copied by the listener
	 *						immediately upon receiving the vector.
	 *  @synchronization	should only be called in the event thread
	 */
	public float[] getVector()
	{
		return vector;
	}

	/**
	 *  Changes the allowed range for vector values.
	 *  Influences the graphics display such that
	 *  the top margin of the panel corresponds to max
	 *  and the bottom margin corresponds to min. Also
	 *  user drawings are limited to these values unless
	 *  wrapY is set to true (not yet implemented).
	 *
	 *  @param  min		new minimum y value
	 *  @param  max		new maximum y value
	 *
	 *  @warning	the current vector is left untouched,
	 *				even if values lie outside the new
	 *				allowed range.
	 */
	public void setMinMax( float min, float max )
	{
		if( this.min != min || this.max != max ) {
			this.min	= min;
			this.max	= max;
			repaint();
		}
	}

	/**
	 *  Gets the minimum allowed y value
	 *
	 *  @return		the minimum specified function value
	 */
	public float getMin()
	{
		return min;
	}

	/**
	 *  Gets the maximum allowed y value
	 *
	 *  @return		the maximum specified function value
	 */
	public float getMax()
	{
		return max;
	}

	/**
	 *  Set the graph display mode
	 *
	 *  @param  fillArea	if <code>false</code>, a hairline is
	 *						drawn to connect the sample values. if
	 *						<code>true</code>, the area below the
	 *						curve is additionally filled with a
	 *						translucent colour.
	 */
	public void setFillArea( boolean fillArea )
	{
		if( this.fillArea != fillArea ) {
			this.fillArea   = fillArea;
			repaint();
		}
	}

	/**
	 *  Select the allowed range for vector values.
	 *  Influences the graphics display.
	 */
	public void setLabel( String label )
	{
		if( this.label == null || label == null || !this.label.equals( label )) {
			txtLay		= null;
			this.label  = label;
			repaint();
		}
	}

	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Dimension	d	= getSize();

		if( d.width != recentSize.width || d.height != recentSize.height ) {
			recentSize = d;
			recalcTransforms();
			recreateImage();
			redrawImage();
		} else if( pathTrns == null ) {
			recalcTransforms();
			recreateImage();	// XXX since we don't clear the background any more to preserve Aqua LAF
			redrawImage();
		}

		if( image != null ) {
			g.drawImage( image, 0, 0, this );
		}

		// --- invoke top painters ---
		if( !collTopPainters.isEmpty() ) {
			Graphics2D		g2			= (Graphics2D) g;
//			AffineTransform	trnsOrig	= g2.getTransform();
			int				i;

//			g2.transform( trnsVirtualToScreen );
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			for( i = 0; i < collTopPainters.size(); i++ ) {
				((TopPainter) collTopPainters.get( i )).paintOnTop( g2 );
			}
//			g2.setTransform( trnsOrig );
		}
	}
	
	/**
	 *  Registers a new top painter.
	 *  If the top painter wants to paint
	 *  a specific portion of the surface,
	 *  it must make an appropriate repaint call!
	 *
	 *  @param  p   the painter to be added to the paint queue
	 *
	 *  @synchronization	this method must be called in the event thread
	 */
	public void addTopPainter( TopPainter p )
	{
		if( !collTopPainters.contains( p )) {
			collTopPainters.add( p );
		}
	}

	/**
	 *  Removes a registered top painter.
	 *
	 *  @param  p   the painter to be removed from the paint queue
	 *
	 *  @synchronization	this method must be called in the event thread
	 */
	public void removeTopPainter( TopPainter p )
	{
		collTopPainters.remove( p );
	}
	
	private void recreateImage()
	{
		if( image != null ) image.flush();
		image = createImage( recentSize.width, recentSize.height );
	}
	
	private void redrawImage()
	{
		if( image == null ) return;

		Graphics2D g2 = (Graphics2D) image.getGraphics();
//		g2.setColor( Color.white );
//		g2.fillRect( 0, 0, recentSize.width, recentSize.height );
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
//		g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
		if( fillArea ) {
			g2.setPaint( pntArea );
			g2.fill( pathTrns );
		}
		g2.setStroke( strkLine );
		g2.setPaint( pntLine );
		g2.draw( pathTrns );
		if( label != null ) {
			g2.setPaint( pntLabel );
			if( txtLay == null ) {
				txtLay		= new TextLayout( label, getFont(), g2.getFontRenderContext() );
				txtBounds   = txtLay.getBounds();
			}
			txtLay.draw( g2, recentSize.width - (float) txtBounds.getWidth() - 4,
							 recentSize.height - (float) txtBounds.getHeight() );
		}
		g2.dispose();
	}

	/*
	 *  Recalculates a Java2D path from the vector
	 *  that will be used for painting operations
	 */
	private void recalcPath()
	{
		int			i;
		float		f1;
		float		f2 = (min - max) / recentSize.height + min;
		
		path.reset();
		if( vector.length > 0 ) {
			f1  = 1.0f / vector.length;
			path.moveTo( -0.01f, f2 );
			path.lineTo( -0.01f, vector[0] );
			for( i = 0; i < vector.length; i++ ) {
				path.lineTo( i * f1, vector[i] );
			}
			path.lineTo( 1.01f, vector[vector.length-1] );
			path.lineTo( 1.01f, f2 );
			path.closePath();
// System.out.println( "recalced path" );
		}
		pathTrns = null;
	}

// ---------------- VirtualSurface interface ---------------- 

	/*
	 *  Recalculates the transforms between
	 *  screen and virtual space
	 */
	private void recalcTransforms()
	{
// System.out.println( "recalc trns for "+recentSize.width+" x "+recentSize.height );

		trnsVirtualToScreen.setToTranslation( 0.0, recentSize.height );
		trnsVirtualToScreen.scale( recentSize.width,recentSize.height / (min - max) );
		trnsVirtualToScreen.translate( 0.0, -min );
		trnsScreenToVirtual.setToTranslation( 0.0, min );
		trnsScreenToVirtual.scale( 1.0 / recentSize.width, (min - max) / recentSize.height );
		trnsScreenToVirtual.translate( 0.0, -recentSize.height );

		pathTrns = path.createTransformedShape( trnsVirtualToScreen );
	}
	
	/**
	 *  Converts a location on the screen
	 *  into a point the virtual space.
	 *  Neither input nor output point need to
	 *  be limited to particular bounds
	 *
	 *  @param  screenPt		point in screen space
	 *  @return the input point transformed to virtual space
	 */
	public Point2D screenToVirtual( Point2D screenPt )
	{
		return trnsScreenToVirtual.transform( screenPt, null );
	}

	/**
	 *  Converts a shape in the screen space
	 *  into a shape in the virtual space.
	 *
	 *  @param  screenShape		arbitrary shape in screen space
	 *  @return the input shape transformed to virtual space
	 */
	public Shape screenToVirtual( Shape screenShape )
	{
		return trnsScreenToVirtual.createTransformedShape( screenShape );
	}

	/**
	 *  Converts a point in the virtual space
	 *  into a location on the screen.
	 *
	 *  @param  virtualPt   point in the virtual space whose
	 *						visible bounds are (0, 0 ... 1, 1)
	 *  @return				point in the display space
	 */
	public Point2D virtualToScreen( Point2D virtualPt )
	{
		return trnsVirtualToScreen.transform( virtualPt, null );
	}

	/**
	 *  Converts a shape in the virtual space
	 *  into a shape on the screen.
	 *
	 *  @param  virtualShape	arbitrary shape in virtual space
	 *  @return the input shape transformed to screen space
	 */
	public Shape virtualToScreen( Shape virtualShape )
	{
		return trnsVirtualToScreen.createTransformedShape( virtualShape );
	}

	/**
	 *  Converts a rectangle in the virtual space
	 *  into a rectangle suitable for Graphics clipping
	 *
	 *  @param  virtualClip		a rectangle in virtual space
	 *  @return the input rectangle transformed to screen space,
	 *			suitable for graphics clipping operations
	 */
	public Rectangle virtualToScreenClip( Rectangle2D virtualClip )
	{
		Point2D screenPt1 = trnsVirtualToScreen.transform( new Point2D.Double( virtualClip.getMinX(),
																			   virtualClip.getMinY() ), null );
		Point2D screenPt2 = trnsVirtualToScreen.transform( new Point2D.Double( virtualClip.getMaxX(),
																			   virtualClip.getMaxY() ), null );
	
		return new Rectangle( (int) Math.floor( screenPt1.getX() ), (int) Math.floor( screenPt1.getY() ),
							  (int) Math.ceil( screenPt2.getX() ), (int) Math.ceil( screenPt2.getY() ));
	}
}