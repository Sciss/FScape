/*
 *  SpectSlider.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	modernized
 */

package de.sciss.fscape.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.StringTokenizer;


/**
 *	Used in chebychev waveshaper for adjusting the colour.
 */
public class SpectSlider
        extends JPanel
        implements ComponentListener, FocusListener, KeyListener, MouseListener, MouseMotionListener {

// -------- private variables --------

    private int		bands			= -1;
    private float[]	value			= null;
    private Color[]	sliderColor		= null;
    private Color[]	borderColor		= null;
	private Dimension	currentSize;

    private Cursor	lastCursor		= null;
    private Cursor	drawCursor;
    private int		currentSlider;

    private boolean focused = false;
    private static final Color colrBorder	= new Color( 128, 128, 128 );
    private static final Color colrFocus	= new Color( 100, 115, 162 );

    // dieser Button wird nicht angezeigt, sondern dient nur als
    // Institution zum Verwalten der ActionListener und fuer
    // den Event-Dispatch!
    private Button		actionComponent	= new Button();

    private static float[] clipboard	= null;

// -------- public methods --------
    // public void setColor( Color c );
    // public Color getColor();

    public SpectSlider( float[] value, int width, int height )
    {
        setBands( value );

        drawCursor		= new Cursor( Cursor.HAND_CURSOR );
		Dimension preferredSize = new Dimension(width, height);
        setPreferredSize(preferredSize);
        setMinimumSize( new Dimension( width >> 1, height >> 1 ));

        currentSize		= preferredSize;

        setForeground( Color.black );
//		setBackground( Color.white );

        addMouseListener( this );
        addMouseMotionListener( this );
        addComponentListener( this );
        addKeyListener( this );
        addFocusListener( this );

//		Random rnd = new Random( System.currentTimeMillis() );
//		for( int i = 0; i < bands; i++ ) {
//			value[ i ] = rnd.nextFloat();
//		}
    }

    public SpectSlider( int bands, int width, int height )
    {
        this( new float[ bands ], width, height );
    }

    public SpectSlider( int bands )
    {
        this( new float[ bands ], bands * 11 + 3, bands * 11 + 3 );
    }

    public void setBand( int band, float value )
    {
        this.value[ band ] = value;
        repaint();
    }

    public float getBand( int band )
    {
        return value[ band ];
    }

    public void setBands( float[] value )
    {
        int oldBands	= bands;

        bands			= value.length;

        if( (oldBands != bands) || (this.value == null) ) {
            this.value	= new float[ value.length ];
        }
        System.arraycopy( value, 0, this.value, 0, bands );

    // ---- re-color ----
        if( (oldBands != bands) || (sliderColor == null) ) {
            sliderColor	= new Color[ bands ];
            borderColor	= new Color[ bands ];
        }

        float hue, f;
        f				= 0.75f / (bands-1);
        for( int i = 0; i< bands; i++ ) {
            hue					= i * f;
            sliderColor[ i ]	= new Color( Color.HSBtoRGB( hue, 0.5f, 1.0f ));
            borderColor[ i ]	= new Color( Color.HSBtoRGB( hue, 0.5f, 0.5f ));
        }

        repaint();
    }

    public float[] getBands()
    {
        final float[] b = new float[ value.length ];
        System.arraycopy( value, 0, b, 0, value.length );
        return b;
    }

    /**
     *	Registriert einen ActionListener;
     *	Action-Events kommen, wenn sich der Wert des PathFieldes aendert
     */
    public void addActionListener( ActionListener list )
    {
        actionComponent.addActionListener( list );
    }

    /**
     *	Entfernt einen ActionListener
     */
    public void removeActionListener( ActionListener list )
    {
        actionComponent.removeActionListener( list );
    }

    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );

        g.setColor( colrBorder );
        g.draw3DRect( 0, 0, currentSize.width - 1, currentSize.height - 1, false );
        if(focused) {
            g.setColor( colrFocus );
            g.drawRect( 1, 1, currentSize.width - 3, currentSize.height - 3 );
        }

        int i, x1, x2, y2, h;
        float f;

        f	= (float) (currentSize.width - 3) / (float) bands;
        y2	= currentSize.height - 2;

        for( i = 0, x2 = 0; i < bands; i++ ) {
            h	= (int) (value[ i ] * (currentSize.height - 4) + 0.5f);
            x1	= x2 + 2;
            x2	= (int) ((i + 1) * f + 0.5f);
            if( h <= 0 ) continue;
            g.setColor( sliderColor[ i ]);
            g.fillRect( x1, y2 - h, x2 - x1, h - 1 );
            g.setColor( borderColor[ i ]);
            g.drawRect( x1, y2 - h, x2 - x1, h - 1 );
        }
    }

//	public boolean isFocusTraversable()
//	{
//		return true;
//	}

// -------- StringComm methods --------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for( int i = 0; i < bands; i++ ) {
            sb.append( value[i] );
            sb.append( ',' );
        }
        return( sb.toString() );
    }

    public static float[] valueOf( String s )
    {
        StringTokenizer	st		= new StringTokenizer( s, "," );
        int				bands	= st.countTokens();
        float[]			value	= new float[ bands ];

        for( int i = 0; i < bands; i++ ) {
            value[ i ]	= Float.valueOf( st.nextToken() ).floatValue();
        }
        return value;
    }

// -------- Component methods --------

    public void componentResized( ComponentEvent e )
    {
        currentSize = getSize();
        repaint();
    }

    public void componentMoved( ComponentEvent e )	{}
    public void componentHidden( ComponentEvent e )	{}
    public void componentShown( ComponentEvent e )	{}

// -------- Focus Listener methods --------


    public void focusGained( FocusEvent e )
    {
        focused = true;
        repaint();
    }

    public void focusLost( FocusEvent e )
    {
        focused = false;
        repaint();
    }

// -------- Key Listener methods --------

    public void keyTyped( KeyEvent e )
    {
        switch( e.getKeyChar() ) {
        case 'c':
            synchronized( getClass() )  {
                clipboard = getBands();
            }
            break;

        case 'v':
            synchronized( getClass() )  {
                setBands( clipboard );
            }
            break;
        }
    }

    public void keyPressed( KeyEvent e ) {}
    public void keyReleased( KeyEvent e ) {}

// -------- Mouse Listener methods --------

    public void mousePressed( MouseEvent e )
    {
        requestFocus();
        lastCursor = getCursor();
        setCursor( drawCursor );
        processMouse( e.getX(), e.getY() );
    }

    public void mouseReleased( MouseEvent e )
    {
        if( currentSlider >= 0 ) {
            // Event mit SpectSlider als Source eintragen, damit wir vom
            // GUISupport gefunden werden
            ActionEvent parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "" );
            actionComponent.dispatchEvent( parentE );
        }

        currentSlider = -1;
        setCursor( lastCursor );
    }

    public void mouseClicked( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}

// -------- MouseMotion Listener methods --------

    public void mouseDragged( MouseEvent e )
    {
        processMouse( e.getX(), e.getY() );
    }

    public void mouseMoved( MouseEvent e ) {}

// -------- private methods --------

    private void processMouse( int x, int y )
    {
        int		slider;
        float	f;

        f		= (float) (currentSize.width - 3) / (float) bands;
        slider	= (int) ((x - 2) / f);

        if( (slider < 0) || (slider >= bands) ) {
            if( currentSlider == -1 ) return;
        } else {
            currentSlider = slider;
        }

        f		= (float) (currentSize.height - 2 - y) / (float) (currentSize.height - 4);
        value[ currentSlider ] = Math.max( 0f, Math.min( 1f, f ));
        repaint();
    }
}