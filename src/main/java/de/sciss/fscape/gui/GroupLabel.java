/*
 *  GroupLabel.java
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
 *		06-Dec-04	bugfix in box filler dimensions
 */

package de.sciss.fscape.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  GUI label with grouping brackets.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class GroupLabel
extends JPanel
{
// -------- public Variablen --------

	public static final int	ORIENT_HORIZONTAL	= 0x01;
	public static final int	ORIENT_VERTICAL		= 0x02;
	
	public static final int	BRACE_TOPLEFT		= 0x01;
	public static final int	BRACE_TOPRIGHT		= 0x02;
	public static final int	BRACE_BOTTOMLEFT	= 0x04;
	public static final int	BRACE_BOTTOMRIGHT	= 0x08;

	public static final int BRACE_NONE	= 0;
	public static final int BRACE_LEFT	= BRACE_TOPLEFT		| BRACE_BOTTOMLEFT;
	public static final int BRACE_RIGHT	= BRACE_TOPRIGHT	| BRACE_BOTTOMRIGHT;
	public static final int BRACE_TOP	= BRACE_TOPLEFT		| BRACE_TOPRIGHT;
	public static final int BRACE_BOTTOM= BRACE_BOTTOMLEFT	| BRACE_BOTTOMRIGHT;
	
	/**
	 *	common names
	 */
	public static final String NAME_GENERAL		= "General";
	public static final String NAME_MODULATION	= "Modulation";

// -------- private Variablen --------

//	private int				orient;
	private JLabel			lab;
	private JComponent		filla1;
	private JComponent		filla2;
	private Dimension		maxHoriz	= new Dimension( 0x7FFF, 2 );
	private Dimension		prefHoriz	= new Dimension( 32, 2 );
	private Dimension		maxVert		= new Dimension( 2, 0x7FFF );
	private Dimension		prefVert	= new Dimension( 2, 32 );
	private Dimension		minDim		= new Dimension( 2, 2 );

// -------- public Methoden --------

	/**
	 *	@param	orient	Orientierung; ODER-verknuepfte ORIENT_...
	 *	@param	braces	Klammerung; ODER-verknuepfte BRACE_...
	 */
	public GroupLabel( String name, int orient, int braces )
	{
		lab = new JLabel( name, SwingConstants.CENTER );

		GridBagLayout gbl =  new GridBagLayout();
		setLayout( gbl );
		Insets insets = new Insets( 8, 8, 8, 8 );

		if( orient == ORIENT_HORIZONTAL ) {
			filla1 = new Box.Filler( minDim, prefHoriz, maxHoriz );
			filla2 = new Box.Filler( minDim, prefHoriz, maxHoriz );
			filla1.setBorder( new EtchedBorder() );
			filla2.setBorder( new EtchedBorder() );
			gbl.setConstraints( filla1, new GridBagConstraints( 0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0 ));
			add( filla1 );
			gbl.setConstraints( lab, new GridBagConstraints( 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0 ));
			add( lab );
			gbl.setConstraints( filla2, new GridBagConstraints( 2, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0 ));
			add( filla2 );
			if( (braces & BRACE_LEFT) != 0 ) {
//				filla1.setBorder( new EtchedBorder() );
			}
			if( (braces & BRACE_RIGHT) != 0 ) {
//				filla2.setBorder( new EtchedBorder() );
			}
		} else {
			filla1 = new Box.Filler( minDim, prefVert, maxVert );
			filla2 = new Box.Filler( minDim, prefVert, maxVert );
			filla1.setBorder( new EtchedBorder() );
			filla2.setBorder( new EtchedBorder() );
			gbl.setConstraints( filla1, new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0 ));
			add( filla1 );
			gbl.setConstraints( lab, new GridBagConstraints( 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0 ));
			add( lab );
			gbl.setConstraints( filla2, new GridBagConstraints( 0, 2, 1, 1, 0.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0 ));
			add( filla2 );
			if( (braces & BRACE_TOP) != 0 ) {
//				filla1.setBorder( new EtchedBorder() );
			}
			if( (braces & BRACE_BOTTOM) != 0 ) {
//				filla2.setBorder( new EtchedBorder() );
			}
			this.add( "North", filla1 );
			this.add( "South", filla2 );
		}
	}
		
// -------- private Methoden --------
}
// class GroupLabel
