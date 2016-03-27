/*
 *  IconicComponent.java
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
 *		24-Jun-06	renamed to IconicComponent
 */

package de.sciss.fscape.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *  Created on Java 1.1 using the
 *	same name as the Swing interface,
 *	this class draws a portion of
 *	a icon collection bitmap graphic.
 */
public class IconicComponent
        extends JComponent
        implements Dragable {

// -------- public variables --------

    public static DataFlavor flavor	= null;		// DataFlavor representing this class

// -------- private variables --------

    protected IconBitmap ib;
    protected Dimension d;
    protected int ID;

    private static DataFlavor flavors[] = null;	// all supported DataFlavors

// -------- public methods --------
    // public void setID( int ID );
    // public int getID();

    /**
     *	@param	ib	IconBitmap that contains the image
     *	@param	ID	Icon-ID in der Bitmap-Matrix
     */
    protected IconicComponent(IconBitmap ib, int ID) {
        this.ib = ib;
        d = ib.getDimension();
        setSize(getPreferredSize());
        setID(ID);

        // data flavor
        if (flavor == null) {
            flavor = new DataFlavor(getClass(), "Icon");
            flavors = new DataFlavor[1];
            flavors[0] = IconicComponent.flavor;
        }
    }

    /**
     *	@param	ib	IconBitmap that contains the image
     */
    protected IconicComponent( IconBitmap ib )
    {
        this( ib, -1 );
    }

    /**
     *	Sets the identifier of the icon
     *
     *	@param	ID	Icon-ID in the Bitmap-Matrix
     */
    public void setID( int ID )
    {
        this.ID = ID;
    }

    /**
     *	Returns the identifier of the icon
     *
     *	@return	Icon-ID in the Bitmap-Matrix
     */
    public int getID()
    {
        return ID;
    }

    public Dimension getPreferredSize()
    {
        return new Dimension( d );
    }
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension realD = getSize();
        ib.paint(g, ID, (realD.width - d.width) >> 1, (realD.height - d.height) >> 1);
    }

// -------- Dragable methods --------

    /**
     *	Draws a contour of the icon
     */
    public void paintScheme(Graphics g, int x, int y, boolean mode) {
        g.drawRect(x - (d.width >> 1), y - (d.height >> 1), d.width - 1, d.height - 1);
    }

// -------- Transferable methods --------

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor fl) {
        DataFlavor flavors[] = getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(fl)) return true;
        }
        return false;
    }

    public Object getTransferData(DataFlavor fl)
            throws UnsupportedFlavorException, IOException {
        if (fl.equals(IconicComponent.flavor)) {
            return this;

        } else throw new UnsupportedFlavorException(fl);
    }
}