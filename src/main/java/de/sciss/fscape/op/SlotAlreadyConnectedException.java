/*
 *  SlotAlreadyConnectedException.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.op;

public class SlotAlreadyConnectedException
        extends IllegalStateException {

    public SlotAlreadyConnectedException() {}

    public SlotAlreadyConnectedException(String s) {
        super(s);
    }
}
