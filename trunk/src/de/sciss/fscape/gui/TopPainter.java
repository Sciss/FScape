/*
 *  TopPainter.java
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