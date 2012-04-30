/*
 *  OpIconLabel.java
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
 */

package de.sciss.fscape.gui;

import java.awt.*;
import javax.swing.*;

import de.sciss.app.AbstractApplication;
import de.sciss.app.GraphicsHandler;
import de.sciss.fscape.op.*;

/**
 *	GUI element representing the text label
 *	of a spectral operator.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class OpIconLabel
extends JComponent
{
// -------- public Variablen --------

	public static final String OBJ_NAME = "OpIconLabel";

// -------- private Variablen --------

	private String		labName;
	private int			labNameWidth	= 0;
	private int			opFlags;
	private Font		fnt;
	private FontMetrics	fntMetr;

	private OpIcon		icon;

// -------- public Methoden --------
	// public void setJLabelLocation( Point p );
	// public getOpIcon();
	// public String toString();

	public OpIconLabel( OpIcon icon, String labName )
	{
		this.icon		= icon;
		this.labName	= labName;
		opFlags			= icon.getOperator().getFlags();

newVisualProps();
//		new DynamicAncestorAdapter( new DynamicPrefChangeManager(
//			AbstractApplication.getApplication().getUserPrefs(),
//			new String[] { MainPrefs.KEY_ICONFONT }, new LaterInvocationManager.Listener() {
//
//			public void laterInvocation( Object o )
//			{
//				newVisualProps();
//			}
//		})).addTo( this );

		setFocus( false );
	}

	public void setName( String labName )
	{
		this.labName = labName;
		labNameWidth = fntMetr.stringWidth( labName );
		setSize( getPreferredSize() );
		updateLocation();
	}
	
	public String getName()
	{
		return labName;
	}

	/**
	 *	JLabel mitteilen, dass der Operator
	 *	neue Flags besitzt
	 */
	public void operatorFlagsChanged( int newFlags )
	{
		// Alias-Type changed?
		if( ((newFlags ^ opFlags) & Operator.FLAGS_ALIAS) != 0 ) {
			opFlags = newFlags;
			newVisualProps();
		} else {
			opFlags = newFlags;
		}
	}

	/**
	 *	Erklaert OpIcon als selektiert/deselektiert
	 */
	public void setFocus( boolean state )
	{
//		setForeground( state ? Color.white : Color.black);
//		setBackground( state ? Color.black : Color.white );
//		repaint();
	}

	/**
	 *	@return returns OBJ_OPICONLABEL so you can identify it ('==' Operator) as the OpIcon JLabel!
	 */
	public String toString()
	{
		return OBJ_NAME;
	}

	public OpIcon getOpIcon()
	{
		return icon;
	}

	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
	
		g.drawString( labName, 2, fntMetr.getAscent() );
	}

	public Dimension getPreferredSize()
	{
		return new Dimension( labNameWidth + 4, fntMetr.getHeight() );
	}
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	public void updateLocation()
	{
		Point	loc = icon.getLocation();
	
		setLocation( loc.x + ((OpIcon.ICON_WIDTH - labNameWidth) >> 1) - 2,
					 loc.y + OpIcon.ICON_HEIGHT );
	}

// -------- private Methoden --------

	private void newVisualProps()
	{
//		fnt = AbstractApplication.getApplication().getWindowHandler().getDefaultFont(); // Main.getFont( Main.FONT_ICON );
		fnt = AbstractApplication.getApplication().getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL );
		if( (opFlags & Operator.FLAGS_ALIAS) != 0 ) {		// Alia in kursiv
			fnt = new Font( fnt.getName(), Font.ITALIC, fnt.getSize() );
		}
		setFont( fnt );
		fntMetr = getFontMetrics( fnt );
		setName( labName );
		if( isVisible() ) repaint();
	}
}
// class OpIconLabel
