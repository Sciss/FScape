/*
 *  OpIconLabel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.op.Operator;

import java.awt.*;
import javax.swing.*;

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
// -------- public variables --------

	public static final String OBJ_NAME = "OpIconLabel";

// -------- private variables --------

	private String		labName;
	private int			labNameWidth	= 0;
	private int			opFlags;
    //	private Font		fnt;
    //	private FontMetrics	fntMetr;

	private OpIcon		icon;

// -------- public methods --------
	// public void setJLabelLocation( Point p );
	// public getOpIcon();
	// public String toString();

	public OpIconLabel( OpIcon icon, String labName )
	{
		this.icon		= icon;
		this.labName	= labName;
		opFlags			= icon.getOperator().getFlags();

        setFont(UIManager.getFont("Label.font"));

newVisualProps();
//		new DynamicAncestorAdapter( new DynamicPrefChangeManager(
//			Application.userPrefs,
//			new String[] { MainPrefs.KEY_ICONFONT }, new LaterInvocationManager.Listener() {
//
//			public void laterInvocation( Object o )
//			{
//				newVisualProps();
//			}
//		})).addTo( this );

		setFocus( false );
	}

    public void setName(String labName) {
        this.labName = labName;
        Font fnt = getFont();
        if (fnt != null) {
            FontMetrics fntMetr = getFontMetrics(getFont());
            labNameWidth = fntMetr.stringWidth(labName);
            setSize(getPreferredSize());
            updateLocation();
        }
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

        FontMetrics fntMetr = g.getFontMetrics();
		g.drawString( labName, 2, fntMetr.getAscent() );
	}

	public Dimension getPreferredSize()
	{
        FontMetrics fntMetr = getFontMetrics(getFont());
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

// -------- private methods --------

	private void newVisualProps()
	{
        Font fnt = getFont();
		if( (opFlags & Operator.FLAGS_ALIAS) != 0 ) {		// Alia in kursiv
			fnt = new Font( fnt.getName(), Font.ITALIC, fnt.getSize() );
		}
		// setFont( fnt );
        // fntMetr = getFontMetrics(fnt);
        setName(labName);
        if (isVisible()) repaint();
    }
}
// class OpIconLabel
