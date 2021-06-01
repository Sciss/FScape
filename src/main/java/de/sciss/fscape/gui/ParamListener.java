/*
 *  ParamListener.java
 *  Meloncillo
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	created from de.sciss.meloncillo.gui.NumberListener
 */

package de.sciss.fscape.gui;

import java.util.EventListener;

public interface ParamListener
        extends EventListener {
    public void paramChanged(ParamEvent e);
}