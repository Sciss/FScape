/*
 *  OpPrefs.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
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
