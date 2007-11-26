/*
 *  OpPrefs.java
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

package de.sciss.fscape.prop;

import java.util.*;

/**
 *	Klasse fuer die Operator-Preferences
 *
 *  @version	0.71, 15-Nov-07
 */
// public synchronized class OpPrefs
public class OpPrefs
extends Prefs
{
// -------- public Klassenvariablen --------

	public static final String PR_EDITDIM	= "OpEditDimensions";

// -------- public Methoden --------

	/**
	 *	Legt das Preferences Objekt an mit entsprechenden Default-Eintraegen
	 *
	 *	@param opName	Name des Operators, dient auch zur Filename-Kreierung
	 */
	public OpPrefs( Class owner, Properties defPrefs )
	{
		super( owner, defPrefs );
	}
}
// class OpPrefs
