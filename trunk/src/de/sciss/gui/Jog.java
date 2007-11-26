/*
 *  Jog.java
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
 *		07-Sep-05	created
 *		06-Nov-05	different painting when disabled
 */

package de.sciss.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;

/**
 *  @version	0.3, 06-Nov-05
 */
public class Jog
extends JComponent
implements PropertyChangeListener, EventManager.Processor
{
	// ---- normal colours ----
	private static final Paint	pntBack			= new GradientPaint( 10,  9, new Color( 235, 235, 235 ),
																	 10, 19, new Color( 248, 248, 248 ));
//	private static final Color	colrOutline		= new Color( 0, 0, 0 ); // 0xC0 );
	private static final Color	colrOutline		= new Color( 40, 40, 40 ); // 0xC0 );
	private static final Color	colrLight		= new Color( 251, 251, 251 );
	private static final Color	colrArcLight	= new Color( 255, 255, 255 );
//	private static final Color	colrArcShadow	= new Color( 0, 0, 0, 0x80 );
//	private static final Paint	pntArcShadow	= new GradientPaint( 12,  0, new Color( 0, 0, 0, 0xA0 ),
//																	 8, 15, new Color( 0, 0, 0, 0x00 ));
	private static final Paint	pntArcShadow	= new GradientPaint( 12,  0, new Color( 40, 40, 40, 0xA0 ),
																	  8, 15, new Color( 40, 40, 40, 0x00 ));
	private static final Paint	pntBelly		= new GradientPaint( 0, -3, new Color( 0x58, 0x58, 0x58 ),
																	 0,  3, new Color( 0xD0, 0xD0, 0xD0 ));

	// ---- disabled colours ----
	private static final Paint	pntBackD		= new GradientPaint( 10,  9, new Color( 235, 235, 235, 0x7F ),
																	 10, 19, new Color( 248, 248, 248, 0x7F ));
	private static final Color	colrOutlineD	= new Color( 40, 40, 40, 0x7F );
	private static final Color	colrLightD		= new Color( 251, 251, 251, 0x7F );
	private static final Color	colrArcLightD	= new Color( 255, 255, 255, 0x7F );
	private static final Paint	pntArcShadowD	= new GradientPaint( 12,  0, new Color( 40, 40, 40, 0x50 ),
																	  8, 15, new Color( 40, 40, 40, 0x00 ));
	private static final Paint	pntBellyD		= new GradientPaint( 0, -3, new Color( 0x58, 0x58, 0x58, 0x7F ),
																	 0,  3, new Color( 0xD0, 0xD0, 0xD0, 0x7F ));

	// ---- strokes and shapes ----
	private static final Stroke	strkOutline		= new BasicStroke( 0.5f );
	private static final Stroke	strkArcShadow	= new BasicStroke( 1.2f );
	private static final Stroke	strkArcLight	= new BasicStroke( 1.0f );
	private static final Shape	shpBelly		= new Ellipse2D.Double( -2.5, -2.5, 5.0, 5.0 );
	private final Point2D		bellyPos		= new Point2D.Double( -0.7071064, -0.7071064 );
																	 
	private final Jog			enc_this		= this;

	private static final Cursor	dragCursor		= new Cursor( Cursor.MOVE_CURSOR );
	private Cursor				savedCursor		= null;
	private int					dragX, dragY;
	private double				dragArc;
	private double				dispArc			= -2.356194;
	private boolean				refire			= false;	// if true, refire a number event after dragging
	
	private static final double	PI2				= Math.PI * 2;
	
	private Insets				in;

	private EventManager		elm				= null;	// lazy creation

	public Jog()
	{
		updatePreferredSize();
		setFocusable( true );

		final MouseInputAdapter mia = new MouseInputAdapter() {
			public void mousePressed( MouseEvent e )
			{
				refire = false;
			
				if( !isEnabled() ) return;
			
				requestFocus();
				final Window w = SwingUtilities.getWindowAncestor( enc_this );
				if( w != null ) {
					savedCursor	= w.getCursor();
					w.setCursor( dragCursor );
				}
				
				processMouse( e, false );
			}
			
			public void mouseReleased( MouseEvent e )
			{
				if( !isEnabled() ) return;
				
				final Window w = SwingUtilities.getWindowAncestor( enc_this );
				if( w != null ) {
					w.setCursor( savedCursor );
				}
				
				if( refire ) {
					dispatchChange( 0, false );
					refire = false;
				}
			}

			public void mouseDragged( MouseEvent e )
			{
				if( !isEnabled() ) return;
				
				processMouse( e, true );
			}
			
			private void processMouse( MouseEvent e, boolean isDrag )
			{
				final int		w		= getWidth()  - in.left - in.right;
				final int		h		= getWidth()  - in.top - in.bottom;
				double			dx, dy;
				double			weight, thisArc, deltaArc, newDispArc;
				int				dragAmount;

				dx			= e.getX() - in.left - w * 0.5;
				dy			= e.getY() - in.top  - h * 0.5;

				if( isDrag ) {
					thisArc		= Math.atan2( dx, dy ) + Math.PI;
					dx		   /= w;
					dy		   /= h;
					weight		= Math.max( 0.125, Math.sqrt( dx*dx + dy*dy ) / 2 );
					deltaArc	= thisArc - dragArc;
					if( deltaArc < -Math.PI ) {
						deltaArc = PI2 - deltaArc;
					} else if( deltaArc > Math.PI ) {
						deltaArc = -PI2 + deltaArc;
					}

					dx			= (e.getX() - dragX); // (double) (e.getX() - dragX) / w;
					dy			= (e.getY() - dragY); // (double) (e.getY() - dragY) / h;
					dragAmount	= (int) (Math.sqrt( dx*dx + dy*dy ) * 0.5);
					newDispArc  = (dispArc + ((deltaArc < 0) ? -1 : 1) * Math.min( 0.4,
										weight * dragAmount )) % PI2;

					if( dragAmount >= 1 ) {
						if( dragAmount >= 17 ) {		// Beschleunigen
							dragAmount *= (dragAmount - 16);
						}

						dispArc		= newDispArc;
						dragArc		= thisArc;
						dragX		= e.getX();
						dragY		= e.getY();
						repaint();

						dragAmount	*= (deltaArc < 0) ? 1 : -1;
						dispatchChange( dragAmount, true );
						refire		= true;
					}
				} else {
					dragX	= e.getX();
					dragY	= e.getY();
					dragArc	= Math.atan2( dx, dy ) + Math.PI;
				}
			
				bellyPos.setLocation( Math.cos( dispArc ), Math.sin( dispArc ));
				repaint();
			}
		};

		addMouseListener( mia );
		addMouseMotionListener( mia );
		this.addPropertyChangeListener( "border", this );
	}

	private void updatePreferredSize()
	{
		in	= getInsets();

		final Dimension d = new Dimension( 20 + in.left + in.right, 20 + in.top + in.bottom );
	
		setMinimumSize( d );
		setPreferredSize( d );
	}
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
	
		final Graphics2D		g2			= (Graphics2D) g;
		final Stroke			strkOrig	= g2.getStroke();
		final AffineTransform	atOrig		= g2.getTransform();

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

g2.translate( 0.5f + in.left, 0.5f + in.top );	// tricky disco to blur the outlines 'bit more

		if( isEnabled() ) {
			g2.setPaint( pntBack );
			g2.fillOval( 2, 3, 16, 16 );
			g2.setColor( colrLight );
			g2.fillOval( 5, 1, 9, 10 );
	//		g2.setColor( colrArcShadow );
			g2.setPaint( pntArcShadow );
			g2.setStroke( strkArcShadow );
			g2.drawOval( 1, 1, 17, 17 );
	//		g2.drawArc( 1, 1, 17, 17, 0, 180 );

			g2.setStroke( strkArcLight );
			g2.setColor( colrArcLight );
			g2.drawArc( 1, 2, 17, 17, 180, 180 );

			g2.setColor( colrOutline );
			g2.setStroke( strkOutline );
			g2.drawOval( 1, 1, 17, 17 );
			
			g2.translate( bellyPos.getX() * 4 + 10.0, -bellyPos.getY() * 4.5 + 10.0 );
			g2.setPaint( pntBelly );
			g2.fill( shpBelly );
		
		} else {
			g2.setPaint( pntBackD );
			g2.fillOval( 2, 3, 16, 16 );
			g2.setColor( colrLightD );
			g2.fillOval( 5, 1, 9, 10 );
			g2.setPaint( pntArcShadowD );
			g2.setStroke( strkArcShadow );
			g2.drawOval( 1, 1, 17, 17 );

			g2.setStroke( strkArcLight );
			g2.setColor( colrArcLightD );
			g2.drawArc( 1, 2, 17, 17, 180, 180 );

			g2.setColor( colrOutlineD );
			g2.setStroke( strkOutline );
			g2.drawOval( 1, 1, 17, 17 );
			
			g2.translate( bellyPos.getX() * 4 + 10.0, -bellyPos.getY() * 4.5 + 10.0 );
			g2.setPaint( pntBellyD );
			g2.fill( shpBelly );
		}
		
		g2.setStroke( strkOrig );
		g2.setTransform( atOrig );
	}

	/**
	 *  Register a <code>NumberListener</code>
	 *  which will be informed about changes of
	 *  the gadgets content.
	 *
	 *  @param  listener	the <code>NumberListener</code> to register
	 */
	public void addListener( NumberListener listener )
	{
		synchronized( this ) {
			if( elm == null ) {
				elm = new EventManager( this );
			}
			elm.addListener( listener );
		}
	}

	/**
	 *  Unregister a <code>NumberListener</code>
	 *  from receiving number change events.
	 *
	 *  @param  listener	the <code>NumberListener</code> to unregister
	 */
	public void removeListener( NumberListener listener )
	{
		if( elm != null ) elm.removeListener( listener );
	}

	public void processEvent( BasicEvent e )
	{
		NumberListener listener;
		
		for( int i = 0; i < elm.countListeners(); i++ ) {
			listener = (NumberListener) elm.getListener( i );
			switch( e.getID() ) {
			case NumberEvent.CHANGED:
				listener.numberChanged( (NumberEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}

	private void dispatchChange( int delta, boolean adjusting )
	{
		if( elm != null ) {
			elm.dispatchEvent( new NumberEvent( this, NumberEvent.CHANGED, System.currentTimeMillis(),
							   new Integer( delta ), adjusting ));
		}
	}

// ------------------- PropertyChangeListener interface -------------------

	public void propertyChange( PropertyChangeEvent e )
	{
		if( e.getPropertyName().equals( "border" )) {
			updatePreferredSize();
		}
	}
}
