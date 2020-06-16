/*
 *  BasicDialog.java
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

import de.sciss.fscape.Application;
import de.sciss.fscape.util.PrefsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.prefs.Preferences;

/**
 * Common superclass of all modal dialogs.
 */
public abstract class BasicDialog
        extends JDialog {

    // windows bounds get saved to a sub node inside the shared node
    // the node's name is the class name's last part (omitting the package)
    protected final Preferences	classPrefs;

    protected BasicDialog(Component parent, String title, boolean mode) {
        this(parent, title, mode, "");
    }

    protected BasicDialog(Component parent, String title, boolean mode, String nodeSuffix) {
        super(determineFrame(parent), title, mode);

        String  className   = getClass().getName();
        classPrefs			= Application.userPrefs.node(
                className.substring(className.lastIndexOf('.') + 1) + nodeSuffix);

        ComponentListener	cmpListener;
        WindowListener		winListener;

        cmpListener = new ComponentAdapter() {
            public void componentResized( ComponentEvent e )
            {
                classPrefs.put( PrefsUtil.KEY_SIZE, PrefsUtil.dimensionToString( getSize() ));
            }

            public void componentMoved( ComponentEvent e )
            {
                classPrefs.put( PrefsUtil.KEY_LOCATION, PrefsUtil.pointToString( getLocation() ));
            }

            public void componentShown( ComponentEvent e )
            {
                classPrefs.putBoolean( PrefsUtil.KEY_VISIBLE, true );
            }

            public void componentHidden( ComponentEvent e )
            {
                classPrefs.putBoolean( PrefsUtil.KEY_VISIBLE, false );
            }
        };
        winListener = new WindowAdapter() {
            public void windowOpened( WindowEvent e )
            {
                classPrefs.putBoolean( PrefsUtil.KEY_VISIBLE, true );
            }

            public void windowClosing( WindowEvent e )
            {
                classPrefs.putBoolean( PrefsUtil.KEY_VISIBLE, false );

            }
        };

        addComponentListener( cmpListener );
        addWindowListener( winListener );

//		new DynamicAncestorAdapter( new DynamicPrefChangeManager(
//			Application.userPrefs,
//			new String[] { MainPrefs.KEY_GUIFONT }, new LaterInvocationManager.Listener() {
//
//			public void laterInvocation( Object o )
//			{
//				newVisualProps();
//			}
//		})).addTo( getRootPane() );
    }

    private static Frame determineFrame( Component c )
    {
        return (Frame) SwingUtilities.getAncestorOfClass( Frame.class, c );
    }

    /**
     */
    protected void initDialog()
    {
//		HelpGlassPane.attachTo( root, this );
        restoreFromPrefs();
    }

/*
    private void newVisualProps()
    {
//		setFont( Main.getFont( Main.FONT_GUI ));
//		String value = Main.getPrefs().getProperty( MainPrefs.KEY_WINCOLOR );
//		try {
//			setBackground( new Color( Integer.parseInt( value )));
//			repaint();
//		} catch( NumberFormatException e ) {}
    }
*/
    /*
     *  Restores this frame's bounds and visibility
     *  from its class preferences.
     *
     *  @see	#restoreAllFromPrefs()
     */
    private void restoreFromPrefs()
    {
        String		sizeVal = classPrefs.get( PrefsUtil.KEY_SIZE, null );
        String		locVal  = classPrefs.get( PrefsUtil.KEY_LOCATION, null );
//		String		visiVal	= classPrefs.get( PrefsUtil.KEY_VISIBLE, null );
        Rectangle   r		= getBounds();
//		Insets		i		= getInsets();

        Dimension d			= PrefsUtil.stringToDimension( sizeVal );
        if( d == null || alwaysPackSize() ) {
            pack();
            d				= getSize();
        }

        r.setSize( d );
        Point p = PrefsUtil.stringToPoint( locVal );
        if( p != null ) {
            r.setLocation( p );
        }
        setBounds( r );
        invalidate();
//		if( alwaysPackSize() ) {
//			pack();
//		} else {
            validate();
//		}
//		lim.queue( this );
//		if( visiVal != null ) {
//			setVisible( new Boolean( visiVal ).booleanValue() );
//		}
    }

    /**
     *	<code>false</code> by default.
     */
    protected boolean alwaysPackSize()
    {
        return false;
    }

    public void setFont( Font fnt )
    {
        super.setFont( fnt );
        updateFont( getContentPane(), fnt );
//		setFont( Main.getFont( Main.FONT_GUI ));
    }

    protected void updateFont( Container c, Font fnt )
    {
        Component[] comp = c.getComponents();
        int i;

        c.setFont( fnt );
        for( i = 0; i < comp.length; i++ ) {
            if( comp[i] instanceof Container ) {
                updateFont( (Container) comp[i], fnt );
            } else {
                comp[i].setFont( fnt );
            }
        }
    }
}