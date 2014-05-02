/*
 *  Dragable.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
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
