/*
 *  EnableDummyAction.java
 *  (FScape)
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
 *		24-Jun-06	copied from de.sciss.eisenkraut.gui.EnableDummyAction
 */

package de.sciss.fscape.gui;

import de.sciss.gui.MenuAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EnableDummyAction
        extends MenuAction {
    public EnableDummyAction(Action dummy) {
        super();
        mimic(dummy);
        setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
    }
}