/*
 *  TopPainter.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		09-Jan-05	copied from Meloncillo
 */

package de.sciss.fscape.gui;

import java.awt.*;

/**
 *  Simple as that: paint something
 *  arbitrary on top of a hosting component
 *  See the implementing classes for examples.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.67, 02-Sep-04
 *
 *  @see	Surface#addTopPainter( TopPainter )
 */
public interface TopPainter
{
	/**
	 *	Paints something on top of a component's
	 *	graphics context. Components offering
	 *	adding and removal of top painters should
	 *	state which flags and transforms are initially
	 *	set for the context, e.g. if coordinates are
	 *	already normalized or not. The top painter
	 *	should undo any temporary changes to the graphics
	 *	context's affine transform, paint and stroke.
	 *
	 *	@param	g	the graphics context to paint onto.
	 */
	public void paintOnTop( Graphics2D g );
}