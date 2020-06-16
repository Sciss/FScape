/*
 *  BasicCellRenderer.java
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

package de.sciss.fscape.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 *  Model for JTable cell renderers such
 *	as in the batch processor.
 */
public class BasicCellRenderer
        // extends JLabel
        implements TableCellRenderer, ListCellRenderer {

//    static final Color evenColor    = new Color( 0xFF, 0xFF, 0xFF );
//    static final Color oddColor     = new Color( 0xF5, 0xF5, 0xF5 );
//    static final Color selColor     = new Color( 0xC7, 0xD0, 0xDB );
//    static final EmptyBorder border = new EmptyBorder( 1, 4, 1, 4 );
    Font italicFont;
    Font boldFont;
    Font monoFont;

    protected final DefaultListCellRenderer   lr;
    protected final DefaultTableCellRenderer  tr;

    public BasicCellRenderer()
    {
        lr = new DefaultListCellRenderer();
        tr = new DefaultTableCellRenderer();
        //        setOpaque( true );  // MUST do this for background to show up.
        //        setBorder( border );

        Font fnt    = lr.getFont();
        int fntSize = fnt != null ? fnt.getSize() : 13;
        italicFont  = new Font( "Dialog", Font.ITALIC, fntSize );
        boldFont    = new Font( "Dialog", Font.BOLD, fntSize );
        monoFont    = new Font( "Monospaced", Font.PLAIN, fntSize );
    }

    public Component getTableCellRendererComponent( JTable table, Object obj, boolean isSelected, boolean hasFocus,
                                                    int row, int column )
    {
        final Component res = tr.getTableCellRendererComponent ( table, obj, isSelected, hasFocus, row, column );

//        setFont( null );
//        setBackground( isSelected ? selColor : ((row % 2) == 0 ? evenColor : oddColor ));
//        setHorizontalAlignment( LEFT );
//        setText( obj.toString() );
//        return this;
        return res;
    }

    public Component getListCellRendererComponent( JList list, Object obj, int index, boolean isSelected,
                                                   boolean cellHasFocus )
    {
        final Component res = lr.getListCellRendererComponent ( list, obj, index, isSelected, cellHasFocus );
//        setFont( null );
//        setBackground( isSelected ? selColor : ((index % 2) == 0 ? evenColor : oddColor ));
//        setHorizontalAlignment( LEFT );
//        setText( obj.toString() );
//        return this;
        return res;
    }
} // BasicCellRenderer
