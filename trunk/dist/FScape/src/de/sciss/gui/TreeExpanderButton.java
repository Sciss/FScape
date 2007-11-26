/*
 *  TreeExpanderButton.java
 *  de.sciss.gui
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
 *  Change log:
 *		14-Feb-06	created
 */

package de.sciss.gui;

import java.awt.AWTEventMulticaster;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;

/**
 *	A triangle icon button which looks like an Aqua tree expander.
 *	In collapsed state, triangle points to the right, in expanded
 *	state, triangle points to the bottom.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.26, 11-Mar-06
 */
public class TreeExpanderButton
extends AbstractButton
implements ActionListener, MouseListener, MouseMotionListener
{
	public static final int				DEFAULT_SIZE	= 13;

	private boolean						expanded		= false;
	private boolean						turning			= false;

	private static final Color			colrNormal		= new Color( 0x73, 0x73, 0x73, 0xFF );
	private static final Color			colrPressed		= new Color( 0x39, 0x39, 0x39, 0xFF );
	private static final Color			colrDisabled	= new Color( 0x73, 0x73, 0x73, 0x7F );

	private static final GeneralPath	shpCollapsed;
	private static final GeneralPath	shpExpanded;
	private static final GeneralPath	shpTurning; //		= new Polygon( new int[] { 7, 10, 1 }, new int[] { 2, 11, 8 }, 3 );

	private final DefaultButtonModel	model;
	private final javax.swing.Timer		timerTurning;

	private	ActionListener				al				= null;
	
	private String expandedTT							= null;	// ToolTip
	private String collapsedTT							= null;

	static {
		shpCollapsed	= new GeneralPath();
		shpCollapsed.moveTo( 2.0f, 0.6f );
		shpCollapsed.lineTo( 11.2f, 5.5f );
		shpCollapsed.lineTo( 2.0f, 10.4f );
		shpCollapsed.closePath();

		shpExpanded	= new GeneralPath();
		shpExpanded.moveTo( 0.6f, 2.0f );
		shpExpanded.lineTo( 5.5f, 11.2f );
		shpExpanded.lineTo( 10.4f, 2.0f );
		shpExpanded.closePath();
//
//3.4648232278141
//-3.4648232278141
//
//
//6.5053823869162
//
//6.5053823869162
//
//-3.4648232278141
//
//3.4648232278141
//
//
//9.96, 3.03
//
//
		shpTurning	= new GeneralPath();
		shpTurning.moveTo( 6.96f, 0.03f );
		shpTurning.lineTo( 10.0f, 10.0f );
		shpTurning.lineTo( 0.03f, 6.96f );
		shpTurning.closePath();
	}
	
	public TreeExpanderButton()
	{
		super();

		final int		width	= DEFAULT_SIZE;
		final int		height	= DEFAULT_SIZE;
		final Dimension d		= new Dimension( width + 4, height + 2 );
	
//		setBorderPainted( false );
		setBorder( BorderFactory.createEmptyBorder( 1, 2, 1, 2 ));
		setMargin( new Insets( 1, 2, 1, 2 ));
		setPreferredSize( d );
		setMinimumSize( d );
		setMaximumSize( d );
//		setContentAreaFilled( false );
		setFocusable( false );
		model					= new DefaultButtonModel();
		setModel( model );
		
		addMouseListener( this );
		addMouseMotionListener( this );
		
		timerTurning			= new javax.swing.Timer( 100, this );
		timerTurning.setRepeats( false );
	}
	
	public void setExpandedToolTip( String tt )
	{
		expandedTT = tt;
		if( expanded ) setToolTipText( tt );
	}

	public void setCollapsedToolTip( String tt )
	{
		collapsedTT = tt;
		if( !expanded ) setToolTipText( tt );
	}

	public boolean isExpanded()
	{
		return expanded;
	}

	public void setExpanded( boolean exp )
	{
		if( exp != expanded ) {
			turning		= false;
			expanded	= exp;
			timerTurning.stop();
			model.setArmed( false );
			repaint();
			setToolTipText( expanded ? expandedTT : collapsedTT );
		}
	}

	// overriden without calling super
	// to avoid lnf border painting
	// which is happening despite setting our own border
	public void paintComponent( Graphics g )
	{
		final Graphics2D	g2 = (Graphics2D) g;
		final Shape			shp;
		final Color			colr;
		
		if( isEnabled() ) {
//			if( model.isPressed() ) {
			if( model.isArmed() ) {
				colr = colrPressed;
			} else {
				colr = colrNormal;
			}
		} else {
			colr = colrDisabled;
		}
		
		if( turning ) {
			shp	= shpTurning;
		} else if( expanded ) {
			shp = shpExpanded;
		} else {
			shp = shpCollapsed;
		}

		g2.translate( 1, 2 );
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g2.setColor( colr );
		g2.fill( shp );
		g2.translate( -1, -2 );
	}
	
	private void lala()
	{
		model.setPressed( true );
		if( model.isArmed() ) {
			turning		= true;
			expanded	= !expanded;
			timerTurning.restart();
//			fireActionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "" ));
			model.setArmed( false );
			repaint();
			setToolTipText( expanded ? expandedTT : collapsedTT );
		}
	}

	public synchronized void addActionListener( ActionListener l )
	{
		al = AWTEventMulticaster.add( al, l );
	}
	
	public synchronized void removeActionListener( ActionListener l )
	{
		al = AWTEventMulticaster.remove( al, l );
	}

	private void fireActionPerformed()
	{
		final ActionListener l = al;
		if( l != null ) {
			l.actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, null ));
		}
	}

// ---------------- ActionListener interfaces ----------------

	// called by the turning triangle timer upon completion
	public void actionPerformed( ActionEvent e )
	{
		turning	= false;
		repaint();
		fireActionPerformed();
	}

// ---------------- Mouse(Motion)Listener interfaces ----------------

	public void mousePressed( MouseEvent e1 )
	{
		if( isEnabled() ) {
			model.setArmed( true );
			requestFocus();
			repaint();
		}
	}

	public void mouseReleased( MouseEvent e1 )
	{
		if( isEnabled() ) {
			lala();
		}
	}

	public void mouseDragged( MouseEvent e1 )
	{
		if( isEnabled() ) {
			final boolean oldState = model.isArmed();
			final boolean newState = this.contains( e1.getPoint() );
			if( oldState != newState ) {
				model.setArmed( newState );
				repaint();
			}
		}
	}
	
	public void mouseClicked( MouseEvent e1 ) {}
	public void mouseEntered( MouseEvent e1 ) {}
	public void mouseExited( MouseEvent e1 ) {}
	public void mouseMoved( MouseEvent e1 ) {}
}
