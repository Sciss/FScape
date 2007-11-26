/*
 *  Slots.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.fscape.util;

import java.io.*;
import java.rmi.*;
import java.util.*;

public interface Slots
{
	// Flags
	public static final int SLOTS_STATUSMASK	= 0x0F;
	public static final int SLOTS_TYPEMASK		= 0xF0;

	public static final int SLOTS_FREE		= 0x01;
	public static final int SLOTS_LINKED	= 0x02;
	public static final int SLOTS_READER	= 0x10;
	public static final int SLOTS_WRITER	= 0x20;

	public static final int SLOTS_ANY		= 0;

	// Common names
	public static final String SLOTS_DEFREADER = "in";
	public static final String SLOTS_DEFWRITER = "out";

	/**
	 *	Liefert eine Aufzaehlung der Slotnamen mit gewuenschtem Filter
	 *
	 *	@param filter	SLOTS_..., z.B. SLOTS_READER | SLOTS_FREE liefert nur frei Leseslots
	 *	@return Aufzaehlung der Slots, evtl. leere Aufzaehlung
	 */
	public Vector getSlots( int filter );
	
	/**
	 *	Liefert das Slotobjekt mit gewuenschtem Filter
	 *
	 *	@param name	Slotname
	 *	@return Slotobjekt oder null, wenn nicht vorhanden
	 */
	public Object getSlot( String name );

	/**
	 *	Verknuepft einen Slot dieses Objects mit einem eines anderen
	 *
	 *	@param thisName		Name des eigenen Slots
	 *	@param destSlots	Ziel-Slots-Objekt
	 *	@param destName		Name des Ziel-Slots
	 */
	public void linkTo( String thisName, Slots destSlots, String destName )
	throws SyncFailedException, AlreadyBoundException, NoSuchElementException;
	
	public void divorceFrom( String thisName, Slots srcSlots, String srcName )
	throws NotBoundException, NoSuchElementException;
}
// interface Slots
