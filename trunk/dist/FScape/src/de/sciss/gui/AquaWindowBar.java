/*
 *  AquaWindowBar.java
 *  (de.sciss.gui package)
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
 *  Change log:
 *		14-Apr-06	created
 *		31-May-06	added small size mode, added horizontal option
 */

package de.sciss.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.JTextComponent;

import de.sciss.app.AbstractWindow;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.TiledImage;
import de.sciss.util.Disposable;

/**
 *	An Aqua-LnF fake window bar to be used on the top or left
 *	border of frame. This bar can be used to drag the window around,
 *	plus it can display the window's title. in the small size version
 *	it can be used to simulate floating palette windows.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 03-Oct-07
 *
 *	@todo		title should be abbreviated automatically if too long for display
 *	@todo		font should be made resistant against GUIUtil.setDeepFont
 *	@todo		DO_NOTHING_ON_CLOSE : windowClosing should be fired
 *
 *	@todo		all the window emulation stuff should be removed, as FloatingPalette should
 *				override methods in AppWindow itself
 */
public class AquaWindowBar
//extends JComponent
extends JPanel
implements Disposable, SwingConstants, WindowConstants
{
	private static final int	WIDTH		= 22;
	private static final int	WIDTHS		= 16;
	private static final int	PNTWIDTH	= 20;
	private static final int	PNTWIDTHS	= 15;
//	private static final int[]	bgPixels	= { 0xFFCACACA, 0xFFCCCCCC, 0xFFCFCFCF, 0xFFD1D1D1, 0xFFD3D3D3, 0xFFD6D6D6,
//												0xFFD7D7D7, 0xFFD9D9D9, 0xFFDBDBDB, 0xFFDDDDDD, 0xFFDFDFDF, 0xFFE0E0E0,
//												0xFFE2E2E2, 0xFFE3E3E3, 0xFFE4E4E4, 0xFFE6E6E6, 0xFFE6E6E6, 0xFFE6E6E6,
//												0xFFE8E8E8, 0xFFEFEFEF };
	private static final int[]	bgPixels	= { 0xFFEFEFEF, 0xFFE8E8E8, 0xFFE6E6E6, 0xFFE6E6E6, 0xFFE6E6E6, 0xFFE4E4E4,
												0xFFE3E3E3, 0xFFE2E2E2, 0xFFE0E0E0, 0xFFDFDFDF, 0xFFDDDDDD, 0xFFDBDBDB,
												0xFFD9D9D9, 0xFFD7D7D7, 0xFFD6D6D6, 0xFFD3D3D3, 0xFFD1D1D1, 0xFFCFCFCF,
												0xFFCCCCCC, 0xFFCACACA };
//	private static final int[]	bgPixelsS	= { 0xFFF7F7F7, 0xFFEBEBEB, 0xFFE6E6E6, 0xFFE6E6E6, 0xFFE5E5E5, 0xFFE3E3E3,
//												0xFFE1E1E1, 0xFFDFDFDF, 0xFFDDDDDD, 0xFFDADADA, 0xFFD7D7D7, 0xFFD5D5D5,
//												0xFFD2D2D2, 0xFFCFCFCF, 0xFFCFCFCF };
	
	private final BufferedImage	imgActive;
	private final Paint			pntActive;
	private String				title;
	
	private static final Color	colrTextActive		= Color.black;
	private static final Color	colrTextInactive	= new Color( 0x00, 0x00, 0x00, 0x7F );
	private static final Color	colrLine1			= new Color( 0xF9, 0xF9, 0xF9 );
	private static final Color	colrLine2			= new Color( 0x8C, 0x8C, 0x8C );
	private static final Color	colrLineS			= new Color( 0x8E, 0x8E, 0x8E );
	private static final Color	colrLineSI			= new Color( 0x8E, 0x8E, 0x8E, 0x97 );
	private static final Paint	pntInactive			= new Color( 0x00, 0x00, 0x00, 0x08 );
	
	private static final double	nintyCCW			= -Math.PI / 2;	// yes, it's really counter clockwise
	
	private final AbstractWindow			w;
	private final AbstractWindow.Listener	winListener;
	
	private final int			flags;
	private final boolean		small;
	private final boolean		vertical;
	private final int			width;
	private final int			pntExtent;
	private boolean				alwaysOnTop			= false;
	
	private final TripletButton[]	ggTriplet			= new TripletButton[ 3 ];
	private int tripletX;
	private final MouseAdapter tripletML;
	
	private static TiledImage	imgTriplet			= null;	// lazy creation
	
	// ------------- Flags -------------
	public static final int		CLOSEGADGET			= 0x01;
	public static final int		MINGADGET			= 0x02;
	public static final int		MAXGADGET			= 0x04;
	public static final int		TRIPLET				= CLOSEGADGET | MINGADGET | MAXGADGET;
	
	private int dco = HIDE_ON_CLOSE;

	private final boolean	isMac;
	
	private TempFocusTracker	tft					= null;
				
	public AquaWindowBar( AbstractWindow w, String title, boolean small )
	{
		this( w, title, small, HORIZONTAL );
	}

	public AquaWindowBar( AbstractWindow w, String title, boolean small, int orient )
	{
		this( w, title, small, orient, TRIPLET );
	}

	public AquaWindowBar( AbstractWindow w, String title, boolean small, int orient, int flags )
	{
		this( w, small, orient, flags );
		this.title	= title;
	}

	public AquaWindowBar( AbstractWindow w, boolean small )
	{
		this( w, small, HORIZONTAL );
	}

	public AquaWindowBar( AbstractWindow w, boolean small, int orient )
	{
		this( w, small, orient, TRIPLET );
	}

	public AquaWindowBar( final AbstractWindow w, boolean small, int orient, int flags )
	{
		super( null );
//		super();
		
//System.err.println( "flags = "+flags+"; small = "+small );

		this.w			= w;
		this.small		= small;
		this.vertical	= orient == VERTICAL;
		this.flags		= flags;
		width			= small ? WIDTHS : WIDTH;
		pntExtent		= small ? PNTWIDTHS : PNTWIDTH;
		isMac			= System.getProperty( "os.name" ).indexOf( "Mac OS" ) >= 0;
				
		if( vertical ) {
			GUIUtil.constrainWidth( this, width );
			imgActive = new BufferedImage( pntExtent, 1, BufferedImage.TYPE_INT_ARGB );
			imgActive.setRGB( 0, 0, pntExtent, 1, bgPixels, 0, pntExtent );
			pntActive = new TexturePaint( imgActive, new Rectangle( 0, 0, pntExtent, 1 ));
		} else {
			GUIUtil.constrainHeight( this, width );
			imgActive = new BufferedImage( 1, pntExtent, BufferedImage.TYPE_INT_ARGB );
			imgActive.setRGB( 0, 0, 1, pntExtent, bgPixels, 0, 1 );
			pntActive = new TexturePaint( imgActive, new Rectangle( 0, 0, 1, pntExtent ));
		}
		
		if( small ) {
			setFont( new Font( "Lucida Grande", Font.PLAIN, 11 ));
		}
				
		final MouseInputAdapter mia = new MouseInputAdapter() {
			private Point		initialMouse = null;
			private Point		initialLoc;
			private Rectangle	bounds;
		
			public void mousePressed( MouseEvent e )
			{
				final Window			win = SwingUtilities.getWindowAncestor( e.getComponent() );
				final GraphicsDevice[]	devs;
			
				if( win != null ) {
					initialMouse	= e.getPoint();
					SwingUtilities.convertPointToScreen( initialMouse, e.getComponent() );
					initialLoc		= win.getLocation();
					devs			= GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
					bounds			= devs[ 0 ].getDefaultConfiguration().getBounds();
					for( int i = 1; i < devs.length; i++ ) {
						bounds		= bounds.union( devs[ i ].getDefaultConfiguration().getBounds() );
					}
				}
			}

			public void mouseReleased( MouseEvent e )
			{
				initialMouse	= null;
			}
			
			public void mouseDragged( MouseEvent e )
			{
				final Window win = SwingUtilities.getWindowAncestor( e.getComponent() );

				if( (initialMouse != null) && (win != null) ) {
					final Point currentMouse = e.getPoint();
					SwingUtilities.convertPointToScreen( currentMouse, e.getComponent() );
//					win.setLocation( Math.max( 0, initialLoc.x + currentMouse.x - initialMouse.x ),
//									 Math.max( 22, initialLoc.y + currentMouse.y - initialMouse.y ));
					win.setLocation( Math.max( bounds.x, initialLoc.x + currentMouse.x - initialMouse.x ),
									 Math.max( bounds.y + (isMac ? 22 : 0), initialLoc.y + currentMouse.y - initialMouse.y ));
				}
			}

			public void mouseMoved( MouseEvent e )
			{
				mouseDragged( e );
			}
		};
		
		winListener = new AbstractWindow.Adapter() {
			public void windowActivated( AbstractWindow.Event e )
			{
				repaint();
			}
			
			public void windowDeactivated( AbstractWindow.Event e )
			{
				repaint();
			}
		};

		addMouseListener( mia );
		addMouseMotionListener( mia );
		w.addListener( winListener );
		
		// ------- flags -------
		
		if( ((flags & TRIPLET) != 0) && small ) {
			if( imgTriplet == null ) {
				imgTriplet = new TiledImage( getClass().getResource( AquaFocusBorder.getAquaColorVariant() == 1 ? "paletteblue.png" : "palettegraphite.png" ), 11, 12 );
			}

			tripletML = new MouseAdapter() {
				public void mouseEntered( MouseEvent e )
				{
					for( int i = 0; i < ggTriplet.length; i++ ) {
						if( ggTriplet[ i ] != null ) ggTriplet[ i ].setArmed( true );
					}
				}

				public void mouseExited( MouseEvent e )
				{
					for( int i = 0; i < ggTriplet.length; i++ ) {
						if( ggTriplet[ i ] != null ) ggTriplet[ i ].setArmed( false );
					}
				}
			};

			tripletX = vertical ? 3 : 5;
			for( int i = 0; i < ggTriplet.length; i++ ) {
				createGadget( i );
			}

		} else {
			if( (flags & TRIPLET) != 0 ) {
				throw new IllegalArgumentException( "Gadgets not yet working for regular size bars" );
			}
			tripletML = null;
		}
		
		if( ggTriplet[ 0 ] != null ) {
			ggTriplet[ 0 ].addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e )
				{
					w.getWindow().dispatchEvent( new WindowEvent( (Window) w.getWindow(), WindowEvent.WINDOW_CLOSING ));
				
					switch( dco ) {
					case HIDE_ON_CLOSE:
						w.setVisible( false );
						break;
					case DISPOSE_ON_CLOSE:
//						w.setVisible( false );
						w.dispose();
						break;
					case EXIT_ON_CLOSE:
						System.exit( 0 );
						break;
					default:	// DO_NOTHING_ON_CLOSE
						break;
					}
				}
			});
		}
		if( ggTriplet[ 1 ] != null ) ggTriplet[ 1 ].setEnabled( false ); // palettes aren't minimizable
		if( ggTriplet[ 2 ] != null ) ggTriplet[ 2 ].setEnabled( false ); // until calling setResizable
	}
	
	public void setResizable( boolean b )
	{
		// XXX
	}
	
	public void addCloseActionListener( ActionListener l )
	{
		ggTriplet[ 0 ].addActionListener( l );
	}

	public void removeCloseActionListener( ActionListener l )
	{
		ggTriplet[ 0 ].removeActionListener( l );
	}
	
	public void addMinActionListener( ActionListener l )
	{
		ggTriplet[ 1 ].addActionListener( l );
	}

	public void removeMinActionListener( ActionListener l )
	{
		ggTriplet[ 1 ].removeActionListener( l );
	}
	
	public void addMaxActionListener( ActionListener l )
	{
		ggTriplet[ 2 ].addActionListener( l );
	}

	public void removeMaxActionListener( ActionListener l )
	{
		ggTriplet[ 2 ].removeActionListener( l );
	}
	
	public int getDefaultCloseOperation()
	{
		return dco;
	}
	
	public void setDefaultCloseOperation( int operation )
	{
		dco = operation;
	}
	
	private void createGadget( int id )
	{
		if( (flags & (1 << id)) != 0 ) {
			ggTriplet[ id ] = new TripletButton( id );
			if( vertical ) ggTriplet[ id ].setLocation( 3, tripletX ); else ggTriplet[ id ].setLocation( tripletX, 2 );
			ggTriplet[ id ].addMouseListener( tripletML );
			add( ggTriplet[ id ]);
			tripletX += 18;
			if( id > 0 ) {
				ggTriplet[ id - 1 ].setGaps( vertical ? 0 : 5, vertical ? 5 : 0 );
			}
		}
	}
				
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		final Graphics2D		g2			= (Graphics2D) g;
		final boolean			winActive	= alwaysOnTop || w.isActive();
		final AffineTransform	atOrig;
		final FontMetrics		fntMetr;

		g2.setPaint( ((pntActive != null) && winActive) ? pntActive : pntInactive );
		
		if( vertical ) {
			g2.fillRect( 0, 0, pntExtent, getHeight() );
			if( small ) {
				g2.setColor( winActive ? colrLineS : colrLineSI );
				g2.drawLine( pntExtent, 0, pntExtent, getHeight() - 1 );		
			} else {
				g2.setColor( colrLine1 );
				g2.drawLine( pntExtent, 0, pntExtent, getHeight() - 1 );		
				g2.setColor( colrLine2 );
				g2.drawLine( pntExtent + 1, 0, pntExtent + 1, getHeight() - 1 );
			}
			if( title != null ) {
				atOrig	= g2.getTransform();
				fntMetr	= g2.getFontMetrics();
				g2.setColor( winActive || small ? colrTextActive : colrTextInactive );
				g2.rotate( nintyCCW );
				g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				g2.drawString( title, -((getHeight() + fntMetr.stringWidth( title )) >> 1), ((pntExtent + fntMetr.getAscent()) >> 1) - 1 );
				g2.setTransform( atOrig );
			}
			
		} else {
			g2.fillRect( 0, 0, getWidth(), pntExtent );
			if( small ) {
				g2.setColor( winActive ? colrLineS : colrLineSI );
				g2.drawLine( 0, pntExtent, getWidth() - 1, pntExtent );		
			} else {
				g2.setColor( colrLine1 );
				g2.drawLine( 0, pntExtent, getWidth() - 1, pntExtent );		
				g2.setColor( colrLine2 );
				g2.drawLine( 0, pntExtent + 1, getWidth() - 1, pntExtent + 1 );
			}
			if( title != null ) {
				atOrig	= g2.getTransform();
				fntMetr	= g2.getFontMetrics();
				g2.setColor( winActive || small ? colrTextActive : colrTextInactive );
				g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				g2.drawString( title, Math.max( tripletX, (getWidth() - fntMetr.stringWidth( title )) >> 1 ), ((pntExtent + fntMetr.getAscent()) >> 1) - 1 );
				g2.setTransform( atOrig );
			}			
		}
	}
	
	public void setAlwaysOnTop( boolean b )
	{
		if( b != alwaysOnTop ) {
			if( tft != null ) {
				tft.dispose();
				tft = null;
			}
			// setAlwaysOnTop doesn't exist in Java 1.4
			try {
				final Object o = w.getWindow();
				final Method m = o.getClass().getMethod( "setAlwaysOnTop", new Class[] { Boolean.TYPE });
				final JFrame jf;
				m.invoke( o, new Object[] { new Boolean( b )});
				alwaysOnTop = b;
				if( alwaysOnTop && w.isVisible() ) w.toFront();
				jf = (javax.swing.JFrame) o;
				jf.setFocusableWindowState( false );
				// now track components that need to be focussed
				tft = new TempFocusTracker( jf );
				repaint();
			}
			catch( NoSuchMethodException e1 ) {}
			catch( NullPointerException e1 ) {}
			catch( SecurityException e1 ) {}
			catch( IllegalAccessException e1 ) {}
			catch( IllegalArgumentException e1 ) {}
			catch( InvocationTargetException e1 ) {}
			catch( ExceptionInInitializerError e1 ) {}
		}
	}
	
	public boolean isAlwaysOnTop()
	{
		return alwaysOnTop;
	}
	
	public void setTitle( String title )
	{
		this.title	= title;
		repaint();
	}
	
	public String getTitle()
	{
		return title;
	}

	public void dispose()
	{
		imgActive.flush();
	}
	
	// ------------------------ internal classes ------------------------
	
	private static class TripletButton
	extends JButton
	{
		private final Icon icnNormal, icnDisabled, icnOver, icnPressed;

		private TripletButton( int id )
		{
			this( imgTriplet.createIcon( id, 0 ), imgTriplet.createIcon( id, 1 ),
				  imgTriplet.createIcon( id, 2 ), imgTriplet.createIcon( id, 3 ));
		}

		private TripletButton( Icon icnNormal, Icon icnDisabled, Icon icnOver, Icon icnPressed )
		{
			super();

			this.icnNormal			= icnNormal;
			this.icnDisabled		= icnDisabled;
			this.icnOver			= icnOver;
			this.icnPressed			= icnPressed;

			final int		width	= icnNormal.getIconWidth();
			final int		height	= icnNormal.getIconHeight();
			final Dimension d		= new Dimension( width, height );
		
			setPreferredSize( d );
			setMinimumSize( d );
			setMaximumSize( d );
			setSize( d );
			
			setContentAreaFilled( false );
			setBorderPainted( false );
			setFocusable( false );
		}

		private void setGaps( int gapH, int gapV )
		{
			final int		width	= icnNormal.getIconWidth();
			final int		height	= icnNormal.getIconHeight();
			final Dimension d		= new Dimension( width + gapH, height + gapV );
		
//			setBorder( BorderFactory.createEmptyBorder( 0, 0, gapH, gapV ));
//			setMargin( new Insets( 0, 0, gapH, gapV ));
			setPreferredSize( d );
			setMinimumSize( d );
			setMaximumSize( d );
			setSize( d );
		}
		
		private void setArmed( boolean b )
		{
			final ButtonModel	m = getModel();
			m.setArmed( b );
			repaint();
		}

		// overriden without calling super
		// to avoid lnf border painting
		// which is happening despite setting our own border
		public void paintComponent( Graphics g )
		{
			final Icon			icn;
			final ButtonModel	m;
		
			if( isEnabled() ) {
				m = getModel();
				if( m.isPressed() ) {
					icn = icnPressed;
				} else if( m.isArmed() ) {
					icn = icnOver;
				} else {
					icn = icnNormal;
				}
			} else {
				icn = icnDisabled;
			}
			icn.paintIcon( this, g, 0, 0 );
//System.err.println( "GAGA" );
		}
	}
	
	private static class TempFocusTracker
	implements Disposable, ContainerListener, MouseListener, FocusListener, AncestorListener
	{
		private static final boolean	DEBUG	= false;
		
		private final JFrame	jf;
		private Component		currentFocus	= null;
		
		private TempFocusTracker( JFrame jf )
		{
			this.jf	= jf;
//			addContainer( jf.getContentPane() );
			jf.getRootPane().addAncestorListener( this );
		}
		
		public void dispose()
		{
			removeContainer( jf.getContentPane() );
			jf.getRootPane().removeAncestorListener( this );
		}
		
		private void addContainer( Container c )
		{
			for( int i = 0; i < c.getComponentCount(); i++ ) {
				addComponent( c.getComponent( i ));
			}
			c.addContainerListener( this );
			if( DEBUG ) System.err.println( "addContainerListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
		}

		private void removeContainer( Container c )
		{
			c.removeContainerListener( this );
			if( DEBUG ) System.err.println( "removeContainerListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
			for( int i = c.getComponentCount() - 1; i >= 0; i-- ) {
				removeComponent( c.getComponent( i ));
			}
		}
		
		private void addComponent( Component comp ) 
		{
			if( comp instanceof JTextComponent ) {
				addTextComponent( (JTextComponent) comp );
			} else if( (comp instanceof JPanel) || (comp instanceof Box) || (comp instanceof JTabbedPane) ) {
				addContainer( (Container) comp );
			} else if( comp instanceof JScrollPane ) {
				addScrollPane( (JScrollPane) comp );
			}
		}

		private void removeComponent( Component comp )
		{
			if( comp instanceof JTextComponent ) {
				removeTextComponent( (JTextComponent) comp );
			} else if( (comp instanceof JPanel) || (comp instanceof Box) || (comp instanceof JTabbedPane) ) {
				removeContainer( (Container) comp );
			} else if( comp instanceof JScrollPane ) {
				removeScrollPane( (JScrollPane) comp );
			}
		}
		
		private void addTextComponent( JTextComponent c )
		{
			c.addMouseListener( this );
			c.addFocusListener( this );
			if( DEBUG ) System.err.println( "addFocus/MouseListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
		}

		private void removeTextComponent( JTextComponent c )
		{
			c.removeMouseListener( this );
			c.removeFocusListener( this );
			if( DEBUG ) System.err.println( "removeFocus/MouseListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
		}

		private void addScrollPane( JScrollPane p )
		{
			final Component c = p.getViewport().getView();
			c.addMouseListener( this );
			c.addFocusListener( this );
			if( DEBUG ) System.err.println( "addFocus/MouseListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
		}

		private void removeScrollPane( JScrollPane p )
		{
			final Component c = p.getViewport().getView();
			c.removeMouseListener( this );
			c.removeFocusListener( this );
			if( DEBUG ) System.err.println( "removeFocus/MouseListener " + c.getClass().getName() + "(" + c.hashCode() + ")" );
		}

		public void componentAdded( ContainerEvent e )
		{
			if( DEBUG ) System.out.println( "componentAdded to " +e.getContainer().getClass().getName() + "(" + e.getContainer().hashCode() + ") : " + e.getChild().getClass().getName() + "(" + e.getChild().hashCode() + ") ");
			addComponent( e.getChild() );
		}

		public void componentRemoved( ContainerEvent e )
		{
			if( DEBUG ) System.out.println( "componentRemoved from " +e.getContainer().getClass().getName() + "(" + e.getContainer().hashCode() + ") : " + e.getChild().getClass().getName() + "(" + e.getChild().hashCode() + ") ");
			removeComponent( e.getChild() );
		}
		
		public void mousePressed( MouseEvent e )
		{
			final Component c = e.getComponent();
			
			jf.setFocusableWindowState( true );
			c.requestFocus();
			currentFocus = c;
		}
		
		public void mouseClicked( MouseEvent e ) {}
		public void mouseEntered( MouseEvent e ) {}
		public void mouseExited( MouseEvent e ) {}
		public void mouseReleased( MouseEvent e ) {}

		public void focusLost( FocusEvent e )
		{
			if( e.getComponent() == currentFocus ) {
				currentFocus = null;
				jf.setFocusableWindowState( false );
			}
		}

		public void focusGained( FocusEvent e )
		{
			currentFocus = e.getComponent();
		}
		
		public void ancestorAdded( AncestorEvent e )
		{
			if( DEBUG ) System.out.println( "ancestorAdded " + e.getAncestor().getClass().getName() + "(" + + e.getAncestor().hashCode() + ")" );
			addContainer( jf.getContentPane() );
		}
		
		public void ancestorRemoved( AncestorEvent e )
		{
			if( DEBUG ) System.out.println( "ancestorRemoved " + e.getAncestor().getClass().getName() + "(" + + e.getAncestor().hashCode() + ")" );
			removeContainer( jf.getContentPane() );
		}
		
		public void ancestorMoved( AncestorEvent e ) {}
	}
}