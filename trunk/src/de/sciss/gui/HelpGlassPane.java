/*
 *  HelpGlassPane.java
 *  de.sciss.gui package
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		20-May-05	created from de.sciss.meloncillo.gui.HelpGlassPane
 */

package de.sciss.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.prefs.PreferenceChangeEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicPrefChangeManager;
import de.sciss.app.LaterInvocationManager;

/**
 *  A component suitable for using as
 *  a frame's glass pane : it add's a
 *  semi transparent colour on top, while
 *  intercepting mouse events so as to
 *  open help pages for GUI components
 *  instead of the normal operation.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-06
 *
 *  @see	javax.swing.JFrame#setGlassPane( Component )
 *
 *	@deprecated	use HelpButton instead
 */
public class HelpGlassPane
extends JComponent
implements	LaterInvocationManager.Listener
{
	/**
	 *  Value: Prefs key string "(int) modifiers (int) keyCode" for online
     *  help accelerator. Has default value: yes!<br>
	 *  Node: root
	 */
	public static final String KEY_KEYSTROKE_HELP = "keystrokehelp";

	private final Color				colrBg					= new Color( 0x00, 0x00, 0xFF, 0x5F );
	private final Color				colrBg2					= new Color( 0xF0, 0xFF, 0x00, 0x7F );
//  private final Color				colrBg					= GraphicsUtil.colrSelection;
	private final JFrame			f;
    private Component				normalGlassPane			= null;
    private JComponent				focussedHelpComponent	= null;
	private FocusTraversalPolicy	normalFocus				= null;
    private Area					focussedArea			= new Area();
	private final Action helpAction;

	private KeyStroke helpStroke	= null;

    private static final Object HELP_PROPERTY	= HelpGlassPane.class;

	/**
	 *  Creates a new dimming help glass pane.
	 *  This method will install a keystroke action for
     *  the help key so the frame needn't deal with the help invocation.
	 *
	 *  @param  f     the frame to which the help pane
     *                belongs
	 */
    public static void attachTo( JFrame f )
    {
        new HelpGlassPane( f );
    }

 	/**
	 *  Adds a help file tag to a component which
     *  will be examined when the user clicks on the component
     *  in help-mode.
	 *
	 *  @param  c           the component for which the help tag
     *                      shall be installed
	 *  @param  fileName    plain file name (omitting parent path
     *                      and suffix) of the associated help file
     *                      or <code>null</code> to remove the help tag.
	 *
	 *  @see	javax.swing.JComponent#putClientProperty( Object, Object )
	 */
    public static void setHelp( JComponent c, String fileName )
    {
        c.putClientProperty( HELP_PROPERTY, fileName );
    }
 
	private HelpGlassPane( JFrame f )
	{
		this.f			= f;
        JRootPane grass = f.getRootPane();
		
        MouseInputAdapter mia = new MouseInputAdapter() {
            public void mousePressed( MouseEvent e )
            {
                checkMousePressed( e );
            }
            
            public void mouseMoved( MouseEvent e )
            {
                checkMouseMoved( e );
            }
        };
		KeyAdapter ka = new KeyAdapter() {
            public void keyPressed( KeyEvent e )
            {
                if( e.getKeyCode() == KeyEvent.VK_ESCAPE ) deactivateHelpMode();
            }
		};
		helpAction = new AbstractAction() {
            public void actionPerformed( ActionEvent e )
            {
                activateHelpMode();
            }
        };
		
        this.addMouseListener( mia );
        this.addMouseMotionListener( mia );
		this.addKeyListener( ka );
		this.setCursor( new Cursor( Cursor.HAND_CURSOR ));

		grass.getActionMap().put( HELP_PROPERTY, helpAction );

		// --- Listener ---
        new DynamicAncestorAdapter( new DynamicPrefChangeManager( GUIUtil.getUserPrefs(),
			new String[] { KEY_KEYSTROKE_HELP }, this )).addTo( grass );
	}
    
    private void activateHelpMode()
    {
        Component recentGlassPane = f.getGlassPane();
        if( recentGlassPane == this || recentGlassPane.isVisible() ) return;
 
        normalGlassPane         = recentGlassPane;
        focussedHelpComponent   = null;
        focussedArea.reset();
        f.setGlassPane( this );
        this.setVisible( true );
		normalFocus				= f.getFocusTraversalPolicy();
		f.setFocusTraversalPolicy( new NoFocusTraversalPolicy() );
		this.requestFocus();
    }
	
    private void deactivateHelpMode()
    {
        if( f.getGlassPane() != this ) return;

        this.setVisible( false );
        f.setGlassPane( normalGlassPane );
		f.setFocusTraversalPolicy( normalFocus );
        normalGlassPane         = null;
		normalFocus				= null;
        focussedHelpComponent   = null;
        focussedArea.reset();
		f.requestFocus();
    }
	
	/**
	 *  Paints translucent grey over
	 *  the rectangle given by the content pane's
	 *  top margin and the bottom most gadget's
	 *  bottom margin.
	 */
	public void paintComponent( Graphics g )
	{
//		super.paintComponent( g );
	
//		Point		gpPoint = SwingUtilities.convertPoint( bottomMost, 0, bottomMost.getHeight(), this );

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        Area a1 = new Area( g.getClipBounds() );
        a1.subtract( focussedArea );
        g2.setColor( colrBg );
        g2.fill( a1 );
        g2.setColor( colrBg2 );
        g2.fill( focussedArea );
     }
	
	private void checkMousePressed( MouseEvent e )
	{
        checkMouseMoved( e );
		if( focussedHelpComponent != null ) {
			HelpFrame.openViewerAndLoadHelpFile( (String)
				focussedHelpComponent.getClientProperty( HELP_PROPERTY ));
            deactivateHelpMode();
		}
	}

	private void checkMouseMoved( MouseEvent e )
	{
        JComponent c = findHelpComponentAt( e.getPoint() );
		if( c != focussedHelpComponent ) {
            focussedHelpComponent   = c;
//          Container contentPane   = f.getContentPane();
            if( c == null ) {
                focussedArea.reset();
            } else {
                Point p1 = SwingUtilities.convertPoint( c, 0, 0, this );
                Point p2 = SwingUtilities.convertPoint( c, c.getWidth(), c.getHeight(), this );
                RoundRectangle2D rr = new RoundRectangle2D.Double( p1.x - 4, p1.y - 4,
                                                                   (p2.x - p1.x) + 8, (p2.y - p1.y) + 8,
                                                                   8.0, 8.0 );
                focussedArea = new Area( rr ); // new Rectangle2D.Double( r2 ));
            }
            repaint();
        }
	}
    
    private JComponent findHelpComponentAt( Point p )
    {
        Container   contentPane = f.getContentPane();
//		Point		cPoint      = SwingUtilities.convertPoint( bottomMost, 0, bottomMost.getHeight(), contentPane );
		Point		cp          = SwingUtilities.convertPoint( this, p, contentPane );
		Component   comp;
        JComponent  jc;
		
		comp = SwingUtilities.getDeepestComponentAt( contentPane, cp.x, cp.y );
        while( comp != null ) {
            if( comp instanceof JComponent ) {
                jc = (JComponent) comp;
//              return jc;
                if( jc.getClientProperty( HELP_PROPERTY ) != null ) return jc;
            }
            comp = comp.getParent();
        }
        return null;
    }

	private void updateHelpStroke( KeyStroke newStroke )
	{
		InputMap imap = f.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );

		if( helpStroke != null ) imap.remove( helpStroke );
		helpStroke = newStroke;
		if( helpStroke != null ) imap.put( helpStroke, HELP_PROPERTY );
	}

// ---------------- LaterInvocationManager.Listener interface ---------------- 

	// o is PreferenceChangeEvent
	public void laterInvocation( Object o )
	{
		updateHelpStroke( KeyStrokeTextField.prefsToStroke( ((PreferenceChangeEvent) o).getNewValue() ));
	}
} // class HelpGlassPane
