/*
 *  AppWindow.java
 *  (de.sciss.common package)
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
 *		19-Aug-06	created from de.sciss.eisenkraut.gui.BasicFrame
 */

package de.sciss.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyVetoException;
import java.util.prefs.Preferences;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.RootPaneContainer;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;
import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.gui.AquaWindowBar;
import de.sciss.gui.InternalFrameListenerWrapper;
import de.sciss.gui.WindowListenerWrapper;

/**
 *  Common functionality for all application windows.
 *  This class provides means for storing and recalling
 *  window bounds in preferences. All subclass windows
 *  will get a copy of the main menubar as well.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 02-Sep-06
 *
 *  @todo   the window bounds prefs storage sucks like hell
 *          ; there's a bug: if recall-window-bounds is deactivated
 *          the prefs are loaded nevertheless, hence when restarting
 *          the application, the bounds will be those of the
 *          last loaded session
 */
public class AppWindow
implements AbstractWindow
{
	/*
	 *  Value: String representing a Point object
	 *  describing a windows location. Use stringToPoint.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	private static final String KEY_LOCATION		= "location";   // point
	/*
	 *  Value: String representing a Dimension object
	 *  describing a windows size. Use stringToDimension.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	private static final String KEY_SIZE			= "size";		// dimension
	/*
	 *  Value: Boolean stating wheter a window is
	 *  shown or hidden.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	private static final String KEY_VISIBLE		= "visible";	// boolean

	private final ComponentListener			cmpListener;
	private final Listener					winListener;

	// windows bounds get saved to a sub node inside the shared node
	// the node's name is the class name's last part (omitting the package)
	private Preferences						classPrefs	= null;
	
	// fucking aliases
	private final Component					c;
	private final Window					w;
	private final Frame						f;
	private final Dialog					d;
	private final JComponent				jc;
	private final JDialog					jd;
	private final JFrame					jf;
	private final JInternalFrame			jif;
	
	private final AquaWindowBar				ggTitle;
	
	// menu bar matrix:
	//			screenMenuBar	internalFrames		other
	// regular	own				own (deleg)			own
	// support	own				---					---
	// palette	borrow			borrow (deleg)		---
	
	private final boolean					floating, ownMenuBar, borrowMenuBar;
	private final BasicWindowHandler		wh;
	private JMenuBar						bar			= null;
	private AbstractWindow					barBorrower	= null;
	private boolean							active		= false;
	
//	private final int						flags;
	
	private boolean							initialized	= false;
	
	public AppWindow( int flags )
	{
		super();
		
//		this.flags	= flags;
		
		wh = (BasicWindowHandler) AbstractApplication.getApplication().getWindowHandler();
		
		switch( flags & TYPES_MASK ) {
		case REGULAR:
		case SUPPORT:
			if( wh.usesInternalFrames() ) {
				c = jc = jif	= new JInternalFrame( null, true, true, true, true );
				w = f = jf		= null;
				d = jd			= null;
				wh.getDesktop().add( jif );
				ownMenuBar		= (flags & TYPES_MASK) == REGULAR;

			} else {
				c = w = f = jf	= new JFrame();
				jc = jif		= null;
				d = jd			= null;
				ownMenuBar		= wh.usesScreenMenuBar() || ((flags & TYPES_MASK) == REGULAR);
			}
			floating			= false;
			borrowMenuBar		= false;
			ggTitle				= null;
			break;
			
		case PALETTE:
			final Preferences prefs = AbstractApplication.getApplication().getUserPrefs();
			
			floating			= prefs.getBoolean( BasicWindowHandler.KEY_FLOATINGPALETTES, false );
			ownMenuBar			= false;
			
			if( wh.usesInternalFrames() ) {
				c = jc = jif	= new JInternalFrame( null, true, true, true, true );
				w = f = jf		= null;
				d = jd			= null;
				borrowMenuBar	= true;
				ggTitle			= null;
				
				if( floating ) jif.putClientProperty( "JInternalFrame.isPalette", Boolean.TRUE );
				wh.getDesktop().add( jif, floating ? JLayeredPane.PALETTE_LAYER : JLayeredPane.DEFAULT_LAYER );

			} else {

				c = w = f = jf	= new JFrame();
				jc = jif		= null;
				d = jd			= null;
//				borrowMenuBar	= wh.usesScreenMenuBar();
				
				if( floating ) {
					ggTitle = new AquaWindowBar( this, true );
					ggTitle.setAlwaysOnTop( true );
borrowMenuBar = false;
					jf.setUndecorated( true );
					
					final Container cp = jf.getContentPane();
					
//					cp.add( ggTitle, orient == HORIZONTAL ? BorderLayout.NORTH : BorderLayout.WEST );
					cp.add( ggTitle, BorderLayout.NORTH );
					
//					if( resizable ) {
//						final JPanel p = new JPanel( new BorderLayout() );
//						p.add( new AquaResizeGadget(), BorderLayout.EAST );
//						cp.add( p, BorderLayout.SOUTH );
//					}
//				} else {
//					if( prefs.getBoolean( PrefsUtil.KEY_INTRUDINGSIZE, false )) {
//						getContentPane().add( Box.createVerticalStrut( 16 ), BorderLayout.SOUTH );
//					}
				} else {
borrowMenuBar = wh.usesScreenMenuBar();
					ggTitle		= null;
				}
			}
			break;
		
		default:
			throw new IllegalArgumentException( "Unsupported window type : " + (flags & TYPES_MASK) );
		}

		cmpListener = new ComponentAdapter() {
			public void componentResized( ComponentEvent e )
			{
				if( classPrefs != null ) classPrefs.put( KEY_SIZE, dimensionToString( e.getComponent().getSize() ));
			}

			public void componentMoved( ComponentEvent e )
			{
				if( classPrefs != null ) classPrefs.put( KEY_LOCATION, pointToString( e.getComponent().getLocation() ));
			}

			public void componentShown( ComponentEvent e )
			{
				if( classPrefs != null ) classPrefs.putBoolean( KEY_VISIBLE, true );
//System.err.println( "shown" );
			}

			public void componentHidden( ComponentEvent e )
			{
				if( classPrefs != null ) classPrefs.putBoolean( KEY_VISIBLE, false );
//System.err.println( "hidden" );
			}
		};
		winListener = new AbstractWindow.Adapter() {
			public void windowOpened( AbstractWindow.Event e )
			{
//System.err.println( "shown" );
				if( classPrefs != null ) classPrefs.putBoolean( KEY_VISIBLE, true );
				if( !initialized ) System.err.println( "WARNING: window not initialized (" + e.getWindow() + ")" );
			}

//			public void windowClosing( WindowEvent e )
//			{
//				classPrefs.putBoolean( PrefsUtil.KEY_VISIBLE, false );
//			}

			public void windowClosed( AbstractWindow.Event e )
			{
//System.err.println( "hidden" );
				if( classPrefs != null ) classPrefs.putBoolean( KEY_VISIBLE, false );
			}
			
			public void windowActivated( AbstractWindow.Event e )
			{
				try {
					active = true;
					if( wh.usesInternalFrames() && ownMenuBar ) {
						wh.getMasterFrame().setJMenuBar( bar );
					} else if( borrowMenuBar && (barBorrower != null) ) {
						barBorrower.setJMenuBar( null );
						if( jf != null ) {
							jf.setJMenuBar( bar );
						} else if( jif != null ) {
							wh.getMasterFrame().setJMenuBar( bar );
						} else {
							throw new IllegalStateException();
						}
					}
				}
				// seems to be a bug ... !
				catch( NullPointerException e1 ) {
					e1.printStackTrace();
				}
			}

			public void windowDeactivated( AbstractWindow.Event e )
			{
				try {
					active = false;
					if( wh.usesInternalFrames() && ownMenuBar ) {
						if( wh.getMasterFrame().getJMenuBar() == bar ) wh.getMasterFrame().setJMenuBar( null );
					} else if( borrowMenuBar && (barBorrower != null) ) {
						if( jf != null ) {
							jf.setJMenuBar( null );
						}
						barBorrower.setJMenuBar( bar );
					}
				}
				// seems to be a bug ... !
				catch( NullPointerException e1 ) {
					e1.printStackTrace();
				}
			}
		};
   	}

	private static Dimension stringToDimension( String value )
	{
		Dimension		dim	= null;
		StringTokenizer tok;
		
		if( value != null ) {
			try {
				tok		= new StringTokenizer( value );
				dim		= new Dimension( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ));
			}
			catch( NoSuchElementException e1 ) {}
			catch( NumberFormatException e2 ) {}
		}
		return dim;
	}

	private static Point stringToPoint( String value )
	{
		Point			pt	= null;
		StringTokenizer tok;
		
		if( value != null ) {
			try {
				tok		= new StringTokenizer( value );
				pt		= new Point( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ));
			}
			catch( NoSuchElementException e1 ) {}
			catch( NumberFormatException e2 ) {}
		}
		return pt;
	}
	
	private static String pointToString( Point value )
	{
		return( value != null ? (value.x + " " + value.y) : null );
	}
	
	private static String dimensionToString( Dimension value )
	{
		return( value != null ? (value.width + " " + value.height) : null );
	}

	public boolean isFloating()
	{
		return floating;
	}
	
	/*
	 *  Restores this frame's bounds and visibility
	 *  from its class preferences.
	 *
	 *  @see	#restoreAllFromPrefs()
	 */
	private void restoreFromPrefs()
	{
		String		sizeVal = classPrefs.get( KEY_SIZE, null );
		String		locVal  = classPrefs.get( KEY_LOCATION, null );
		String		visiVal	= classPrefs.get( KEY_VISIBLE, null );
		Rectangle   r		= c.getBounds();
//		Insets		i		= getInsets();

//System.err.println( "this "+this.getClass().getName()+ " visi = "+visiVal );

		Dimension d			= stringToDimension( sizeVal );
		if( d == null || alwaysPackSize() ) {
			pack();
			d				= c.getSize();
		}

		r.setSize( d );
		Point p = stringToPoint( locVal );
		if( p != null ) {
			r.setLocation( p );
			c.setBounds( r );
		} else {
			c.setSize( d );
			setLocationRelativeTo( null );
		}
		c.invalidate();
//		if( alwaysPackSize() ) {
//			pack();
//		} else {
			c.validate();
//		}
//		lim.queue( this );
		if( (visiVal != null) && restoreVisibility() ) {
			setVisible( new Boolean( visiVal ).booleanValue() );
		}
	}
	
 	/**
	 *  Updates Swing component tree for all
     *  frames after a look-and-feel change
	 */
	public static void lookAndFeelUpdate()
	{
//      if( springContainer == null ) return;
//        
//		AppWindow		bf;
//		int				i;
//		LayoutComponent springComp;
//	
//		for( i = 0; i < springContainer.getComponentCount(); i++ ) {
//			springComp	= (LayoutComponent) springContainer.getComponent( i );
//			bf			= springComp.getRealOne();
//            SwingUtilities.updateComponentTreeUI( bf );
//      }
	}

	/**
	 *  Queries whether this frame's bounds
	 *  should be packed automatically to the
	 *  preferred size independent of
	 *  concurrent preference settings
	 *
	 *  @return	<code>true</code>, if the frame wishes
	 *			to be packed each time a custom setSize()
	 *			would be applied in the course of a
	 *			preference recall. The default value
	 *			of <code>true</code> can be modified by
	 *			subclasses by overriding this method.
	 *  @see	java.awt.Window#pack()
	 */
	protected boolean alwaysPackSize()
	{
		return true;
	}

	protected boolean restoreVisibility()
	{
		return true;
	}

//	protected boolean maintainPrefs()
//	{
//		return true;
//	}

//	/**
//	 *  Queries whether this frame should
//     *  have a copy of the menu bar. The default
//     *  implementation returns true, basic palettes
//     *  will return false.
//	 *
//	 *  @return	<code>true</code>, if the frame wishes
//	 *			to be given a distinct menu bar
//	 */
//	protected boolean hasMenuBar()
//	{
//		switch( flags & TYPES_MASK ) {
//		case REGULAR:
//			return true;
//		
//		default:
//			return false;
//		}
//	}

	/**
	 *	MenuFactory uses this method to replace dummy
	 *	menu items such as File->Save with real actions
	 *	depending on the concrete frame. By default this
	 *	method just returns <code>dummyAction</code>, indicating
	 *	that there is no replacement for the dummy action.
	 *	Subclasses may check the provided <code>ID</code>
	 *	and return replacement actions instead.
	 *
	 *	@param	ID	an identifier for the menu item, such
	 *				as <code>MenuFactory.MI_FILE_SAVE</code>.
	 *
	 *  @return		the action to use instead of the inactive
	 *				dummy action, or <code>dummyAction</code> if no
	 *				specific action exists (menu item stays ghosted)
	 *
	 *	@see	MenuFactory#MI_FILE_SAVE
	 *	@see	MenuFactory#gimmeSomethingReal( AppWindow )
	 */
//	protected Action replaceDummyAction( int ID, Action dummyAction )
//	{
//		return dummyAction;
//	}

	/**
	 *  Subclasses should call this
	 *  after having constructed their GUI.
	 *  Then this method will attach a copy of the main menu
	 *  from <code>root.menuFactory</code> and
	 *  restore bounds from preferences.
	 *
	 *  @param  root	application root
	 *
	 *  @see	MenuFactory#gimmeSomethingReal( AppWindow )
	 */
	public void init()
	{
		if( borrowMenuBar ) {
			borrowMenuBar( wh.getMenuBarBorrower() );
			wh.addBorrowListener( this );
		} else if( ownMenuBar ) {
			setJMenuBar( wh.getMenuBarRoot().createBar( this ));
		}
		wh.addWindow( this, null );
//		AbstractApplication.getApplication().addComponent( getClass().getName(), this );
		
		addListener( winListener );

		initialized = true;
		
//		if( maintainPrefs() ) {
			final String  className	= getClass().getName();
			classPrefs				= AbstractApplication.getApplication().getUserPrefs().node(
										className.substring( className.lastIndexOf( '.' ) + 1 ));
			restoreFromPrefs();
			c.addComponentListener( cmpListener );
//		}
	}

	protected void addDynamicListening( DynamicListening l )
	{
		if( c instanceof RootPaneContainer ) {
			new DynamicAncestorAdapter( l ).addTo( ((RootPaneContainer) c).getRootPane() );
		} else if( jc != null ) {
			new DynamicAncestorAdapter( l ).addTo( jc );
		}
	}

	// ---------- AbstractWindow interface ----------
	
	public Component getWindow()
	{
		return c;
	}
    
	/**
	 *  Frees resources, clears references
	 */
	public void dispose()
	{
		if( initialized ) {
			removeListener( winListener );
			c.removeComponentListener( cmpListener );
			
			wh.removeWindow( this, null );
//			AbstractApplication.getApplication().addComponent( getClass().getName(), null );
			
			if( borrowMenuBar ) {
				borrowMenuBar( null );
				wh.removeBorrowListener( this );
			}
			if( wh.getMenuBarBorrower() == this ) wh.setMenuBarBorrower( null );
			if( ownMenuBar ) {
				setJMenuBar( null );
				wh.getMenuBarRoot().destroy( this );
			}
		}
		
		if( w != null ) {
			w.dispose();
		} else if( jif != null ) {
			jif.dispose();
		}
		
		if( ggTitle != null ) ggTitle.dispose();
		
		classPrefs	= null;
	}
	
//	public void setSize( int width, int height )
//	{
//		c.setSize( width, height );
//	}

	public void setSize( Dimension d )
	{
		c.setSize( d );
	}
	
//	public int getWidth()
//	{
//		return c.getWidth();
//	}
//	
//	public int getHeight()
//	{
//		return c.getHeight();
//	}

	public Dimension getSize()
	{
		return c.getSize();
	}

	public Rectangle getBounds()
	{
		return c.getBounds();
	}
	
	public void setBounds( Rectangle r )
	{
		c.setBounds( r );
	}

	public void setPreferredSize( Dimension d )
	{
		if( c instanceof RootPaneContainer ) {
			((RootPaneContainer) c).getRootPane().setPreferredSize( d );
		} else if( jc != null ) {
			jc.setPreferredSize( d );
		} else {
			throw new IllegalStateException();
		}
	}
	
//	public boolean hasFocus()
//	{
//		return c.hasFocus();
//	}

//	public boolean isFocused()
//	{
//		if( w != null ) {
//			return w.isFocused();
//		} else {
//			return c.hasFocus();
//		}
//	}
	
//	public void requestFocus()
//	{
//		c.requestFocus();
//	}
	
	public boolean isActive()
	{
		if( w != null ) {
			return w.isActive();
		} else {
			return false;
		}
	}

	public void addListener( Listener l )
	{
		if( w != null ) {
			WindowListenerWrapper.add( l, this );
		} else if( jif != null ) {
			InternalFrameListenerWrapper.add( l, this );
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void removeListener( Listener l )
	{
		if( w != null ) {
			WindowListenerWrapper.remove( l, this );
		} else if( jif != null ) {
			InternalFrameListenerWrapper.remove( l, this );
		} else {
			throw new IllegalStateException();
		}
	}

//	public void addWindowFocusListener( WindowFocusListener l )
//	{
//		if( w != null ) {
//			w.addWindowFocusListener( l );
//		} else if( jif != null ) {
//			throw new IllegalStateException( "InternalFrameListener wrapper not yet implemented" );
//		} else {
//			throw new IllegalStateException();
//		}
//	}
//	
//	public void removeWindowFocusListener( WindowFocusListener l )
//	{
//		if( w != null ) {
//			w.removeWindowFocusListener( l );
//		} else if( jif != null ) {
//			throw new IllegalStateException( "InternalFrameListener wrapper not yet implemented" );
//		} else {
//			throw new IllegalStateException();
//		}
//	}
	
	public void toFront()
	{
		if( w != null ) {
			w.toFront();
		} else if( jif != null ) {
			jif.toFront();
			try {
				jif.setSelected( true );
			} catch( PropertyVetoException e ) {}
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void setVisible( boolean b )
	{
		c.setVisible( b );
	}
	
	public boolean isVisible()
	{
		return c.isVisible();
	}
	
	public void setDefaultCloseOperation( int mode )
	{
		if( ggTitle != null ) {
			ggTitle.setDefaultCloseOperation( mode );
		} else if( jf != null ) {
			jf.setDefaultCloseOperation( mode );
		} else if( jd != null ) {
			jd.setDefaultCloseOperation( mode );
		} else if( jif != null ) {
			jif.setDefaultCloseOperation( mode );
		} else {
			throw new IllegalStateException( "setDefaultCloseOperation wrapper not yet implemented" );
		}
	}
	
	public void pack()
	{
		if( w != null ) {
			w.pack();
		} else if( jif != null ) {
			jif.pack();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void setTitle( String title )
	{
		if( ggTitle != null ) {
			ggTitle.setTitle( title );
		} else if( f != null ) {
			f.setTitle( title );
		} else if( d != null ) {
			d.setTitle( title );
		} else if( jif != null ) {
			jif.setTitle( title );
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getTitle()
	{
		if( f != null ) {
			return f.getTitle();
		} else if( d != null ) {
			return d.getTitle();
		} else if( jif != null ) {
			return jif.getTitle();
		} else {
			return null; // throw new IllegalStateException();
		}
	}
	
	public Container getContentPane()
	{
		if( c instanceof RootPaneContainer ) {
			return ((RootPaneContainer) c).getContentPane();
		} else {
			return w;
		}
	}
	
	public void setJMenuBar( JMenuBar m )
	{
		try {
			if( jf != null ) {
				bar = m;
				jf.setJMenuBar( m );
			} else if( jif != null ) {
				bar = m;
				if( active && ownMenuBar ) wh.getMasterFrame().setJMenuBar( bar );
	//			jif.setJMenuBar( m );
			} else {
				throw new IllegalStateException();
			}
		}
		// seems to be a bug ... !
		catch( NullPointerException e1 ) {
			e1.printStackTrace();
		}
	}

	public JMenuBar getJMenuBar()
	{
		return bar;
//		if( jf != null ) {
//			return jf.getJMenuBar();
//		} else if( jif != null ) {
//			return bar; // jif.getJMenuBar();
//		} else {
//			return null; // throw new IllegalStateException();
//		}
	}
	
	protected void borrowMenuBar( AbstractWindow w )
	{
		if( borrowMenuBar && (barBorrower != w) ) {
			if( (bar != null) && (barBorrower != null) ) {
				barBorrower.setJMenuBar( bar );
				bar = null;
			}
			barBorrower = w;
			bar			= barBorrower == null ? null : barBorrower.getJMenuBar();
//System.err.println( "setting bar " + bar + " for window " + this + "; active = "+active );
			if( active ) {
				if( barBorrower != null ) barBorrower.setJMenuBar( null );
				if( jf != null ) {
					jf.setJMenuBar( bar );
				} else if( jif != null ) {
					wh.getMasterFrame().setJMenuBar( bar );
				} else {
					throw new IllegalStateException();
				}
			}
		}
	}
	
	public InputMap getInputMap( int condition )
	{
		if( c instanceof RootPaneContainer ) {
			return ((RootPaneContainer) c).getRootPane().getInputMap( condition );
		} else {
			throw new IllegalStateException();
		}
	}

	public ActionMap getActionMap()
	{
		if( c instanceof RootPaneContainer ) {
			return ((RootPaneContainer) c).getRootPane().getActionMap();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Window[] getOwnedWindows()
	{
		if( w != null ) {
			return w.getOwnedWindows();
		} else {
			return new Window[ 0 ];
		}
	}
	
	public void setFocusTraversalKeysEnabled( boolean enabled )
	{
		c.setFocusTraversalKeysEnabled( enabled );
	}
	
	public void setDirty( boolean dirty )
	{
		if( c instanceof RootPaneContainer ) {
			((RootPaneContainer) c).getRootPane().putClientProperty( "windowModified", new Boolean( dirty ));
		}
	}

	public void setLocationRelativeTo( Component c )
	{
		if( w != null ) {
			w.setLocationRelativeTo( c );
		} else {
//			throw new IllegalStateException();
			final Point p;
			if( c == null ) {
				if( jif == null ) {
					p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
				} else {
					c = wh.getMasterFrame().getWindow();
					p = new Point( c.getWidth() >> 1, c.getHeight() >> 1 );
				}
 			} else {
				p = c.getLocation();
				p.translate( c.getWidth() >> 1, c.getHeight() >> 1 );
			}
			final Point p2 = SwingUtilities.convertPoint( c, p, this.c );
			p2.translate( -(this.c.getWidth() >> 1), -(this.c.getHeight() >> 1) );
			this.c.setLocation( p2 );
		}
	}
	
//	public void setUndecorated( boolean b )
//	{
//		if( d != null ) {
//			d.setUndecorated( b );
//		} else if( f != null ) {
//			f.setUndecorated( b );
//		} else {
////			throw new IllegalStateException();
//System.err.println( "FUCKING HELL setUndecorated NOT POSSIBLE WITH THIS WINDOW TYPE" );
//		}
//	}

	public void setResizable( boolean b )
	{
		if( f != null ) {
			f.setResizable( b );
		} else if( d != null ) {
			d.setResizable( b );
		} else if( jif != null ) {
			jif.setResizable( b );
		} else {
			throw new IllegalStateException();
		}
	}
	
	public boolean isResizable()
	{
		if( f != null ) {
			return f.isResizable();
		} else if( d != null ) {
			return d.isResizable();
		} else if( jif != null ) {
			return jif.isResizable();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void revalidate()
	{
		if( c instanceof RootPaneContainer ) {
			((RootPaneContainer) c).getRootPane().revalidate();
		} else if( jc != null ) {
			jc.revalidate();
		}
	}
}