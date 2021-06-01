/*
 *  IconBitmap.java
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

package de.sciss.fscape.gui;

import java.awt.*;

/**
 *  Wrapper class for a bitmap image
 *	that gets sectioned into small icons.
 */
public class IconBitmap
        extends Component {

// -------- private variables --------

    private final Image		img;
    private final Dimension d;
    private final int		rows, cols;

// -------- public methods --------
    // public Dimension getDimension();
    // public paint( Graphics g, int ID, int x, int y );

    /*		Konstruktor
     *
     *		arg:	fname = Dateiname des Bildes
     *				width, height = Breite und Hoehe der Icons in Pixels
     */
    public IconBitmap( Image img, int width, int height )
    {
        this.img = img;
        d = new Dimension( width, height );
        MediaTracker mt = new MediaTracker( this );
        mt.addImage( img, 0 );
        try {
            mt.waitForAll();
        } catch( InterruptedException ex ) {
            // nothing
        }
        cols = img.getWidth( this ) / d.width;
        rows = img.getHeight( this ) / d.height;
    }

    public IconBitmap( String fname, int width, int height )
    {
        this( Toolkit.getDefaultToolkit().getImage( fname ), width, height );
    }

    /*		Breite und Hoehe der Icons ermitteln
     *
     *		ret: Dimensionen eines Icons
     */
    public Dimension getDimension()
    {
        return new Dimension( d );
    }

    /*		Icon in einem Graphikkontext ausgeben
     *
     *		arg: g = Graphikkontext; ID = Icon-ID; x, y = Offset in g
     */
    public void paint( Graphics g, int ID, int x, int y )
    {
        final Point src = getOffset( ID );
        if( src != null )
        {
            g.drawImage( img, x, y, x + d.width, y + d.height,		// destination
                src.x, src.y, src.x + d.width, src.y + d.height,	// source (Image)
                this
            );
        } else {								// wrong ID, just draw a bevel box
            final Color c = g.getColor();
            g.setColor( Color.red );
            g.fill3DRect( x, y, d.width, d.height, true );
            g.setColor( c );
        }
    }

// -------- private methods --------
    // private Point getOffset( int ID );

    /*		Position eines Icons in der Bitmap ermitteln
     *
     *		arg: ID = Icon-ID
     *		ret: linke obere Ecke bzw. null bei ungueltiger ID
     */
    private Point getOffset(int ID) {
        if (ID < 0 || ID >= cols * rows)
            return null;
        else
            return new Point((ID % cols) * d.width, (ID / cols) * d.height);
    }
}