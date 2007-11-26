/*
 *  BasicWindowHandler.java
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
 *		21-May-05	created
 *		09-Jul-06	hosts a FloatingPaletteHandler
 */

package de.sciss.common;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import net.roydesign.mac.MRJAdapter;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;
import de.sciss.app.WindowHandler;
import de.sciss.gui.AbstractWindowHandler;
import de.sciss.gui.FloatingPaletteHandler;
import de.sciss.gui.MenuRoot;
import de.sciss.gui.WindowListenerWrapper;

//import de.sciss.eisenkraut.Main;
//import de.sciss.eisenkraut.util.PrefsUtil;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 14-Jun-07
 */
public class BasicWindowHandler
extends AbstractWindowHandler
{
	/**
	 *  Value: Boolean stating whether internal frames within one
     *  big app frame are used. Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_INTERNALFRAMES = "internalframes";

	/**
	 *  Value: Boolean stating whether palette windows should
     *  be floating on top and have palette decoration. Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_FLOATINGPALETTES = "floatingpalettes";

	/**
	 *  Value: Boolean stating whether to use the look-and-feel (true)
     *  or native (false) decoration for frame borders. Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_LAFDECORATION = "lafdecoration";

	private final FloatingPaletteHandler	fph;
	private final boolean					internalFrames;
	private final JDesktopPane				desktop;
	private final MasterFrame				masterFrame;
	
	private final List						collBorrowListeners		= new ArrayList();
	private AbstractWindow					borrower				= null;
	private AbstractWindow					defaultBorrower			= null;		// when no doc frame active (usually the main log window)

	private final Action					actionCollect;
	
	private final BasicApplication			root;

	public BasicWindowHandler( BasicApplication root )
	{
		super();
		
		final boolean lafDeco = root.getUserPrefs().getBoolean( KEY_LAFDECORATION, false );
		JFrame.setDefaultLookAndFeelDecorated( lafDeco );
		
		this.root		= root;
		internalFrames	= root.getUserPrefs().getBoolean( KEY_INTERNALFRAMES, false );
		fph				= FloatingPaletteHandler.getInstance();

		if( internalFrames ) {
			masterFrame = new MasterFrame();
			masterFrame.setTitle( root.getName() );
//			masterFrame.setSize( 400, 400 ); // XXX
//			masterFrame.setVisible( true );
			desktop		= new JDesktopPane();
			masterFrame.getContentPane().add( desktop );
		} else {
			desktop		= null;
			masterFrame	= null;
			fph.setListening( true );
		}
		
		actionCollect	= new actionCollectClass( root.getResourceString( "menuCollectWindows" ));

//		MenuGroup	mg;
//
//		mg	= (MenuGroup) root.getMenuFactory().get( "window" );
//		mg.add( new MenuItem( "collect", new actionCollectClass( root.getResourceString( "menuCollectWindows" ))));
//		mg.addSeparator();
	}
	
	public MenuRoot getMenuBarRoot()
	{
		return root.getMenuBarRoot();
	}
	
	public Action getCollectAction()
	{
		return actionCollect;
	}
	
	public static Component getWindowAncestor( Component c )
	{
		final WindowHandler wh = AbstractApplication.getApplication().getWindowHandler();
		
		return SwingUtilities.getAncestorOfClass( (wh instanceof BasicWindowHandler) && ((BasicWindowHandler) wh).internalFrames ?
												  JInternalFrame.class : Window.class, c );
	}
	
	// make sure the menuFactory is ready when calling this
	public void init()
	{
		if( masterFrame != null ) {
			masterFrame.setDefaultMenuBar( root.getMenuBarRoot().createBar( masterFrame ));
			masterFrame.setVisible( true );
		}
	}
	
	public void setMenuBarBorrower( AbstractWindow w )
	{
		borrower = w;
		for( Iterator iter = collBorrowListeners.iterator(); iter.hasNext(); ) {
			((AppWindow) iter.next()).borrowMenuBar( borrower == null ? defaultBorrower : borrower );
		}
	}
	
	public void setDefaultBorrower( AbstractWindow w )
	{
		if( !internalFrames ) {
			defaultBorrower = w;
		}
	}
	
	public void addBorrowListener( AppWindow w )
	{
		collBorrowListeners.add( w );
	}
	
	public void removeBorrowListener( AppWindow w )
	{
		collBorrowListeners.remove( w );
	}
	
	public AbstractWindow getMenuBarBorrower()
	{
		return( borrower == null ? defaultBorrower : borrower );
	}
	
//	public MenuRoot getMenuBarRoot()
//	{
//		return ((BasicApplication) AbstractApplication.getApplication()).getMenuBarRoot();
//	}
//
//	public Font getDefaultFont()
//	{
//		return GraphicsUtil.smallGUIFont;
//	}
//
//	public void addWindow( Window w, Map options )
//	{
//		super.addWindow( w, options );
//		if( (w instanceof FloatingPalette) && ((FloatingPalette) w).isFloating() ) {
//			fph.addPalette( w );
//		} else {
//			fph.addFrame( w );
//		}
//	}
	
	public void addWindow( AbstractWindow w, Map options )
	{
		super.addWindow( w, options );
//		if( (w instanceof FloatingPalette) && ((FloatingPalette) w).isFloating() ) {
		if( fph != null ) fph.add( w );
//		if( w.isFloating() ) {
//			fph.addPalette( w );
//		} else {
//			fph.addFrame( w );
//		}
	}
	
//	public void removeWindow( Window w, Map options )
//	{
//		super.removeWindow( w, options );
//		if( (w instanceof FloatingPalette) && ((FloatingPalette) w).isFloating() ) {
//			fph.removePalette( w );
//		} else {
//			fph.removeFrame( w );
//		}
//	}

	public void removeWindow( AbstractWindow w, Map options )
	{
		super.removeWindow( w, options );
		if( fph != null ) fph.remove( w );
//		if( w.isFloating() ) {
//			fph.removePalette( w );
//		} else {
//			fph.removeFrame( w );
//		}
	}
	
	public AbstractWindow createWindow( int flags )
	{
//		final BasicFrame f = new BasicFrame();
//		f.init( root );
//		return f;
		return new AppWindow( flags );
	}
	
	public boolean usesInternalFrames()
	{
		return internalFrames;
	}

	public boolean usesScreenMenuBar()
	{
		return MRJAdapter.isSwingUsingScreenMenuBar();
	}

	public JDesktopPane getDesktop()
	{
		return desktop;
	}
	
	public AbstractWindow getMasterFrame()
	{
		return masterFrame;
	}
	
//	public Action getCollectAction()
//	{
//		return actionCollect;
//	}
	
	// -------------------- internal classes --------------------
	private static class MasterFrame
	extends JFrame
	implements AbstractWindow
	{
		private JMenuBar bar = null;
		
		private MasterFrame()
		{
			super();
			
			setBounds( GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds() );
		}
		
		public void init() {}
		
		private void setDefaultMenuBar( JMenuBar m )
		{
			bar = m;
			setJMenuBar( null );
		}
		
		public void setJMenuBar( JMenuBar m )
		{
			super.setJMenuBar( m == null ? bar : m );
		}
		
		public void revalidate()
		{
			getRootPane().revalidate();
		}
		
		public void setDirty( boolean b ) {}
		
		public ActionMap getActionMap()
		{
			return getRootPane().getActionMap();
		}

		public InputMap getInputMap( int mode )
		{
			return getRootPane().getInputMap( mode );
		}

		public void addListener( Listener l )
		{
			WindowListenerWrapper.add( l, this );
		}
		
		public void removeListener( Listener l )
		{
			WindowListenerWrapper.remove( l, this );
		}
		
		public boolean isFloating()
		{
			return false;
		}
		
		public Component getWindow()
		{
			return this;
		}
	}
	
	private class actionCollectClass
	extends AbstractAction
	{
		private actionCollectClass( String text )
		{
			super( text );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			AbstractWindow w;

			final Rectangle	outerBounds;
			Rectangle		winBounds;
			boolean			adjustLeft, adjustTop, adjustRight, adjustBottom;
			final boolean	isMacOS = System.getProperty( "os.name" ).indexOf( "Mac OS" ) >= 0;
			
			if( desktop == null ) {
				outerBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
				if( isMacOS ) {
					outerBounds.y     += 22;
					outerBounds.width -= 80;
					outerBounds.height-= 22;
				}
			} else {
				outerBounds = new Rectangle( 0, 0, desktop.getWidth(), desktop.getHeight() );
			}
			  		
			for( Iterator iter = getWindows(); iter.hasNext(); ) {
				w			= (AbstractWindow) iter.next();
				winBounds	= w.getBounds();
				adjustLeft	= winBounds.x < outerBounds.x;
				adjustTop	= winBounds.y < outerBounds.y;
				adjustRight	= (winBounds.x + winBounds.width) > (outerBounds.x + outerBounds.width);
				adjustBottom= (winBounds.y + winBounds.height) > (outerBounds.y + outerBounds.height);
				
				if( !(adjustLeft || adjustTop || adjustRight || adjustBottom) ) continue;

				if( adjustLeft ) {
					winBounds.x = outerBounds.x;
					adjustRight	= (winBounds.x + winBounds.width) > (outerBounds.x + outerBounds.width);
				}
				if( adjustTop ) {
					winBounds.y = outerBounds.y;
					adjustBottom= (winBounds.y + winBounds.height) > (outerBounds.y + outerBounds.height);
				}
				if( adjustRight ) {
					winBounds.x = Math.max( outerBounds.x, outerBounds.x + outerBounds.width - winBounds.width );
					adjustRight	= (winBounds.x + winBounds.width) > (outerBounds.x + outerBounds.width);
					if( adjustRight && w.isResizable() ) winBounds.width = outerBounds.width;
				}
				if( adjustBottom ) {
					winBounds.y = Math.max( outerBounds.y, outerBounds.y + outerBounds.height - winBounds.height );
					adjustBottom= (winBounds.y + winBounds.height) > (outerBounds.y + outerBounds.height);
					if( adjustBottom && w.isResizable() ) winBounds.height = outerBounds.height;
				}
				w.setBounds( winBounds );
			}
		}
	}
}