/*
 *  OpPrefs.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.prop;

import java.util.Properties;

/**
 *	Klasse fuer die Operator-Preferences
 */
public class OpPrefs
        extends Prefs {
// -------- public static variables --------

    public static final String PR_EDITDIM	= "OpEditDimensions";

// -------- public methods --------

    /**
     *	Legt das Preferences Objekt an mit entsprechenden Default-Eintraegen
     */
    public OpPrefs( Class owner, Properties defPrefs )
    {
        super( owner, defPrefs );
    }
}