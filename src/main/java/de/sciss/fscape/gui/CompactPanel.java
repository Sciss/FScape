/*
 *  CompactPanel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		05-Mar-05	created
 */

package de.sciss.fscape.gui;

import de.sciss.gui.GUIUtil;

import javax.swing.*;
import java.awt.*;

/**
 *  A subclass of JPanel that automatically
 *	maintains a SpringLayout. Components must be
 *	added to this panel using the addGadget() method.
 *	Components are arranged from left to right in one row,
 *	until a newLine() call is made. each time a newLine()
 *	is performed, missing columns are automatically fillled
 *	with blanks. When all elements have been added to the
 *	panel, you should call compact() once, and the springs
 *	are installed as in GUIUtil.makeCompactSpringGrid().
 */
public class CompactPanel
        extends JPanel {

    private int numColumns = 0, rowIdx = 0, columnIdx = 0;

    public CompactPanel() {
        super(new SpringLayout());
    }

    public void addGadget(Component comp) {
        super.add(comp);

        increaseColumnIndex();
    }

    /**
     * Throws an AWTError to because you have to use addGadget()
     */
    public Component add(Component comp) {
        throw new AWTError("CompactPanel.add");
    }

    /**
     * Throws an AWTError to because you have to use addGadget()
     */
    public Component add(Component comp, int index) {
        throw new AWTError("CompactPanel.add");
    }

    public void addEmptyColumn() {
        super.add(new JLabel());
        increaseColumnIndex();
    }

    private void addEmptyColumns() {
        while (columnIdx < numColumns) addEmptyColumn();
    }

    private void increaseColumnIndex() {
        columnIdx++;
        if (columnIdx > numColumns) {
            for (int row = rowIdx - 1; row >= 0; row--) {
                for (int col = numColumns, index = (row + 1) * numColumns; col < columnIdx; col++) {
                    super.add(new JLabel(), index);
                }
            }
            numColumns = columnIdx;
        }
    }

    public void newLine() {
        if (rowIdx == 0) {
            numColumns = columnIdx;
        } else {
            addEmptyColumns();
        }
        columnIdx = 0;
        rowIdx++;
    }

    public void compact() {
        compact(0, 0);
    }

    public void compact(int left, int top) {
        compact(left, top, 4, 2);
    }

    public void compact(int left, int top, int padx, int pady) {
        addEmptyColumns();
        GUIUtil.makeCompactSpringGrid(this, rowIdx + 1, numColumns, left, top, padx, pady);
    }
}
