/*
 *  DetachedMenu.java
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
 *		21-May-05	completely simplified
 */

package de.sciss.fscape.gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *  Combination of a JLabel and a JPopupMenu which
 *	provides a window based menu on MacOS.
 */
public class DetachedMenu
        extends JLabel {

// -------- private variables --------

    private final JPopupMenu	pop;

// -------- public methods --------

    public DetachedMenu(String name, final JPopupMenu pop) {
        super(name, CENTER);
        setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(0, 16, 1, 16)));

        this.pop = pop;

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                pop.show(e.getComponent(), 1, e.getComponent().getHeight() - 1);
            }
        });
    }

    public void setName(String labName) {
        setText(labName);
    }

    public JPopupMenu getStrip() {
        return pop;
    }

    public String getName() {
        return getText();
    }
}