/*
 *  Spectrogram.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Slots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 *	Optional window when using the spectral
 *	patcher, will output the current sonogram,
 *	however in a really cheesy way. Needs to be rewritten.
 */
public class Spectrogram
        extends Frame
        implements AdjustmentListener, MouseListener, MouseMotionListener, WindowListener {

// -------- public variables --------

    public			Image	img 	= null;
    public boolean			pausing	= false;

// -------- private variables --------

    private static final int GG_STARTPAUSE	= 1;
    private static final int GG_ZOOMIN		= 3;
    private static final int GG_ZOOMOUT		= 4;
    private static final int GG_PANEL			= 5;

    protected GUISupport	gui			= null;
    protected SpectPanel	spectPanel;
    protected JLabel		lbFreq;						// JLabel zum Anzeigen der Frequenz
    private   Cursor		lastCursor	= null;			// Cursor previously used to dragging

    private	Graphics	imgG	= null;
    private	float		rgb[]	= { 0.0f, 0.0f, 0.0f };

    private SpectStreamSlot	owner;
    private	SpectStream		stream	= null;

    private	int			width	= 192;
    private	int			height	= 128;
    private int			x		= 0;
    private int[]		xTime;				// Zeitpunkt, den ein Pixel repraesentiert
    private int			zoom	= 0;		// 2^n vergroessert
    private int			maxZoom	= 0;
    private int			lines	= 0;
    private int			bottomLine = 0;		// unterste angezeigte Zeile (bei Zoom)
    private	double		freqSpacing;		// bandwidth (Hz)

    private	Cursor		dragCursor;
    private	int			dragLastY;

// -------- public methods --------
    // public void newStream( SpectStream stream );
    // public void ownerTerminated();
    // public void addFrame( float frame[] );

    /**
     *	aktiviert automatisch setVisible() !
     */
    public Spectrogram( SpectStreamSlot slot )
    {
        super( ((OpIcon) slot.getOwner().getIcon()).getName() +
               ((slot.toString() == Slots.SLOTS_DEFWRITER) ? "" : "(" + slot.toString() + ")") +
               " - Spectrogram" );

        this.owner	= slot;
        rgb			= new float[ 3 ];
        dragCursor	= new Cursor( Cursor.N_RESIZE_CURSOR );
        xTime		= new int[ width ];
        for( int i = 0; i < xTime.length; i++ ) {
            xTime[ i ] = -1;
        }

        GridBagConstraints	con;

        ToolIcon	ggStartPause;
        ToolIcon	ggZoomIn, ggZoomOut;

        gui				= new GUISupport();
//		gui.setFont( Main.getFont( Main.FONT_GUI ));
        con				= gui.getGridBagConstraints();

        con.insets		= new Insets( 2, 2, 0, 2 );
        con.anchor		= GridBagConstraints.WEST;
        ggStartPause	= new ToolIcon( ToolIcon.ID_PAUSE, null );
        gui.addCanvas( ggStartPause, GG_STARTPAUSE, this );

        con.insets		= new Insets( 2, 16, 0, 2 );
        con.anchor		= GridBagConstraints.WEST;
        ggZoomIn		= new ToolIcon( ToolIcon.ID_ZOOMIN, null );
        ggZoomIn.setEnabled( false );
        gui.addCanvas( ggZoomIn, GG_ZOOMIN, this );
        con.insets		= new Insets( 2, 0, 0, 2 );
        ggZoomOut		= new ToolIcon( ToolIcon.ID_ZOOMOUT, null );
        ggZoomOut.setEnabled( false );
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addCanvas( ggZoomOut, GG_ZOOMOUT, this );

        con.fill		= GridBagConstraints.BOTH;
        con.insets		= new Insets( 2, 0, 0, 0 );
        spectPanel		= new SpectPanel( this, width, height );
        spectPanel.addMouseListener( this );
        spectPanel.addMouseMotionListener( this );
        con.weightx 	= 1.0;
        con.weighty 	= 1.0;
        gui.addGadget( spectPanel, GG_PANEL );

        con.fill		= GridBagConstraints.HORIZONTAL;
        con.insets		= new Insets( 0, 2, 0, 0 );
        lbFreq			= new JLabel();
        con.weighty 	= 0.0;
        gui.addLabel( lbFreq );

        add( gui );
        pack();

        addWindowListener( this );
        setVisible( true );

        img		= createImage( width, height );
        imgG	= img.getGraphics();
        imgG.setColor( Color.black );
        imgG.fillRect( 0, 0, width, height );
    }

    /**
     *	Meldet einen Stream zum Monitoren an
     */
    public void newStream( SpectStream strm )
    {
        synchronized( this ) {
            stream	= strm;
            for( maxZoom = 0; (strm.bands >> maxZoom) > height; maxZoom++ );
            zoom		= Math.max( 0, maxZoom - 1 );		// i.d.R. 10000 Hz, vernuenftig
            bottomLine	= 0;
            freqSpacing	= (strm.hiFreq - strm.loFreq) / strm.bands;
            updateZoomGG();
            spectPanel.setCursor( new Cursor( Cursor.CROSSHAIR_CURSOR ));
        }
    }

    /**
     *	Meldet einen Operator ab, weil sein run() zu Ende ist
     */
    public void ownerTerminated()
    {
        Component	gg;

        synchronized( this ) {
            owner	= null;
            updateZoomGG();		// disables Zoom-Gadgets
            stream	= null;
            pausing	= true;
            gg = gui.getItemObj( GG_STARTPAUSE );
            if( gg != null ) {
                gg.setEnabled( false );
            }
            spectPanel.setCursor( null );
        }
    }

    /**
     *	Zeichnet ein neues Frame
     */
    public void addFrame( SpectFrame fr )
    {
        int i, j, y;

        synchronized( this ) {
            if( (stream == null) || pausing ) return;			// "died" / Pause

            if( lines < height ) {				// nicht benutzten Raum loeschen
                imgG.setColor( Color.black );
                imgG.drawLine( x, lines, x, height - 1 );
            }

            if( stream.chanNum == 1 ) {
                for( i = bottomLine, y = lines - 1; i < (bottomLine + lines); i++, y-- ) {
                                                                    // 1.17 fuer ein wenig Overhead
                    rgb[ 0 ] = (float) Math.sqrt( fr.data[ 0 ][ i << (zoom + 1) + SpectFrame.AMP ]
                                                  * 1.17f );
                    if( rgb[ 0 ] < 0.0f ) rgb[ 0 ] = 0.0f;
                    if( rgb[ 0 ] > 1.0f ) rgb[ 0 ] = 1.0f;
                    imgG.setColor( new Color( rgb[ 0 ], rgb[ 0 ], rgb[ 0 ] ));
                    imgG.drawLine( x, y, x, y );
                }
            } else {
                for( i = bottomLine, y = lines - 1; i < (bottomLine + lines); i++, y-- ) {
                    for( j = 0; j < Math.min( stream.chanNum, 3 ); j++ ) {
                        rgb[ j ] = (float) Math.sqrt( fr.data[ j ][ (i << (zoom + 1)) +
                                                      SpectFrame.AMP] * 1.17f );
                        if( rgb[ j ] < 0.0f ) rgb[ j ] = 0.0f;
                        if( rgb[ j ] > 1.0f ) rgb[ j ] = 1.0f;
                    }
                    imgG.setColor( new Color( rgb[ 0 ], rgb[ 1 ], rgb[ 2 ]));
                    imgG.drawLine( x, y, x, y );
                }
            }
            xTime[ x ] = (int) SpectStream.framesToMillis( stream, stream.framesWritten - 1 );
            x = (x + 1) % width;

            // Mittenfrequenz-Indikator (oben orange, unten rot)
            y = Math.min( 4, bottomLine - (stream.bands >> (zoom + 1)) );
            y = Math.max( y, 5 - lines );

            imgG.setColor( OpIcon.progColor );
            imgG.drawLine( x, 0, x, lines - 2 + y );
            imgG.setColor( Color.red );
            imgG.drawLine( x, lines - 1 + y, x, height );

            spectPanel.repaint();
        }
    }

// -------- Adjustment methods --------

    public void adjustmentValueChanged( AdjustmentEvent e ) {}

// -------- Mouse methods --------

    public void mouseClicked( MouseEvent e )
    {
        int			ID			= gui.getItemID( e );
        int			ID2;
        Component	gg1;
        int			oldVal;

        synchronized( this ) {
            if( stream == null ) return;			// "died"

            switch( ID ) {
            case GG_STARTPAUSE:
                gg1 = gui.getItemObj( GG_STARTPAUSE );
                if( gg1 != null ) {
                    ID2 = ((ToolIcon) gg1).getID();
                    pausing = (ID2 == ToolIcon.ID_PAUSE);
                    ((ToolIcon) gg1).setID( pausing ? ToolIcon.ID_START : ToolIcon.ID_PAUSE );	// switch
                }
                break;

            case GG_ZOOMIN:
            case GG_ZOOMOUT:
                synchronized( this ) {
                    oldVal = (bottomLine + (lines >> 1)) << zoom;	// versuchen, alte Mitte zu behalten
                    if( ID == GG_ZOOMIN ) {
                        if( zoom > 0 ) {
                            zoom--;
                        }
                    } else {
                        if( zoom < maxZoom ) {
                            zoom++;
                        }
                    }
                    bottomLine = (oldVal >> zoom) - (lines >> 1);
                    updateZoomGG();
                }
                break;

            default:
                break;
            }
        }
    }

    public void mousePressed( MouseEvent e )
    {
        if( (e.getSource() == spectPanel) && (stream != null) && (height == lines) ) {
            dragLastY	= e.getY();
            lastCursor	= spectPanel.getCursor();
            spectPanel.setCursor( dragCursor );
            lbFreq.setText( null );
        }
    }

    public void mouseReleased( MouseEvent e )
    {
        if( (e.getSource() == spectPanel) && (stream != null) ) {
            spectPanel.setCursor( lastCursor );
        }
    }

    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}

// -------- MouseMotion Listener methods (spectPanel) --------

    public void mouseDragged( MouseEvent e )
    {
        int	oldVal;

        synchronized( this ) {
            if( (stream == null) || (height > lines) || (e.getY() == dragLastY) ) return;

            oldVal		= bottomLine;
            bottomLine	= Math.max( 0, bottomLine + e.getY() - dragLastY );
            bottomLine	= Math.min( bottomLine, (stream.bands >> zoom) - lines );

            if( oldVal != bottomLine ) {
                imgG.copyArea( 0, 0, width, height, 0, bottomLine - oldVal );
                dragLastY = e.getY();
                imgG.setColor( Color.black );
                if( oldVal < bottomLine ) {
                    imgG.fillRect( 0, 0, width, bottomLine - oldVal );
                } else {
                    imgG.fillRect( 0, height - oldVal + bottomLine, width, height );
                }
                spectPanel.repaint();
            }
        }
    }

    public void mouseMoved( MouseEvent e )
    {
        int band	= bottomLine + (lines - 1 - e.getY()) << zoom;

        synchronized( this ) {
            if( (e.getSource() == spectPanel) && (stream != null) &&
                (band >= 0) && (band < stream.bands) ) {

                lbFreq.setText( "" + (int) (band * freqSpacing + stream.loFreq) +
                                ((e.getX() < xTime.length) && (xTime[ e.getX() ] != -1) ?
                                    " Hz; " + xTime[ e.getX() ] + " ms"
                                  : " Hz") );
            }
        }
    }

// -------- Window methods --------

    public void windowClosing( WindowEvent e )
    {
        setVisible( false );
        dispose();
        synchronized( this ) {
            pausing = true;
            if( img != null ) {
                img.flush();
                img = null;
            }
            if( imgG != null ) {
                imgG.dispose();
                imgG = null;
            }
            owner	= null;
            stream	= null;
        }
    }

    public void windowActivated( WindowEvent e ) {}
    public void windowClosed( WindowEvent e ) {}
    public void windowDeactivated( WindowEvent e ) {}
    public void windowDeiconified( WindowEvent e ) {}
    public void windowIconified( WindowEvent e ) {}
    public void windowOpened( WindowEvent e ) {}

// -------- private methods --------

    // nur innerhalb synchronized( this ) block aufrufen!
    private void updateZoomGG()
    {
        int			oldVal;
        Component	gg1, gg2;

        gg1 = gui.getItemObj( GG_ZOOMIN );
        gg2 = gui.getItemObj( GG_ZOOMOUT );
        if( gg1 != null ) {
            gg1.setEnabled( (zoom > 0) && (owner != null) );
        }
        if( gg2 != null ) {
            gg2.setEnabled( (zoom < maxZoom) && (owner != null) );
        }

        if( stream != null ) {
            lines = Math.min( height, stream.bands >> zoom );

            oldVal		= bottomLine;
            bottomLine	= Math.max( bottomLine, 0 );
            bottomLine	= Math.min( bottomLine, (stream.bands >> zoom) - lines );

            if( oldVal != bottomLine ) {
                imgG.copyArea( 0, 0, width, height, 0, bottomLine - oldVal );
                imgG.setColor( Color.black );
                if( oldVal < bottomLine ) {
                    imgG.fillRect( 0, 0, width, bottomLine - oldVal );
                } else {
                    imgG.fillRect( 0, height - oldVal + bottomLine, width, height );
                }
                spectPanel.repaint();
            }
        }
    }
}
// class Spectrogram

/*
 *	Hilfsklasse: Panel zeichnet BufferedImage
 */
class SpectPanel
extends Panel
{
    private Spectrogram	owner;
    private	int			width, height;

    public SpectPanel( Spectrogram owner, int width, int height )
    {
        super();

        this.owner	= owner;
        this.width	= width;
        this.height	= height;

        setBackground( Color.black );
    }

    public void update( Graphics g )
    {
        paint( g );
    }

    public void paint( Graphics g )
    {
        if( owner.img != null ) {
            g.drawImage( owner.img, 0, 0, owner );
        }
    }

    public Dimension getPreferredSize()
    {
        return new Dimension( width, height );
    }
}