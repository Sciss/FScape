/*
 *  NumberListener.java
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.NumberListener
 */

package de.sciss.gui;

import java.util.EventListener;

/**
 *  Interface for listening
 *  the changes of the contents
 *  of a <code>NumberField</code> gadget
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.25, 17-Sep-05
 *
 *  @see	NumberField#addListener( NumberListener )
 */
public interface NumberListener
extends EventListener
{
	/**
	 *  Notifies the listener that
	 *  a number changed occured.
	 *
	 *  @param  e   the event describing
	 *				the number change
	 */
	public void numberChanged( NumberEvent e );
}