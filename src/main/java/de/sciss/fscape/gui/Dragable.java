/*
 *  Dragable.java
 *  FScape
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
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

/**
 *  Interface for draggable objects that
 *	is used for intermediate symbolic graphic representation
 *	during a drag gesture.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public interface Dragable
{
	/**
	 *	Schematische Darstellung der Componente zeichnen
	 *	(i.d.R. waehrend eines Drags)
	 *
	 *	@param	mode	false fuer Loeschen, true fuer Zeichnen in aktueller Farbe
	 */
	public void paintScheme( Graphics g, int x, int y, boolean mode );
}
// interface Dragable
