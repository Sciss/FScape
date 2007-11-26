/*
 *  DynamicListening.java
 *  de.sciss.app package
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
 *		20-May-05	created from de.sciss.meloncillo.gui.DynamicListening
 */

package de.sciss.app;

/**
 *  This interface is used by Components that
 *  need to register and unregister listeners
 *  whenever they are shown or hidden in order
 *  to improve performance and garbage collection.
 *  <code>ObserverPalette</code> is a good example
 *  where listeners are dynamically added and removed.
 *  Usually you don't call the interface methods
 *  directly but let <code>DynamicAncestorAdapter</code>
 *  do the work.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.13, 15-Sep-05
 *
 *  @see	DynamicAncestorAdapter
 */
public interface DynamicListening
{
    /**
     *  will be called when the component
     *  becomes visible
     */
    public void startListening();
    /**
     *  will be called when the component
     *  is hidden
     */
    public void stopListening();
}