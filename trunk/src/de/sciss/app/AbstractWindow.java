/*
 *  AbstractWindow.java
 *  de.sciss.app package
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
 *		19-Aug-06	created
 */

package de.sciss.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JMenuBar;
import javax.swing.event.InternalFrameEvent;

/**
 *	Interface that unites functionality
 *	from inhomogeneous classes such as JFrame, JDialog, JInternalFrame
 */
public interface AbstractWindow
{
	/**
	 *	Window type: regular full-fledged window
	 */
	public static final int REGULAR		= 0;
	/**
	 *	Window type: supplementary window
	 *	which for example might not need menubar
	 */
	public static final int SUPPORT		= 1;
	/**
	 *	Window type: supplementary window
	 *	which is a (possibly floating) palette window
	 */
	public static final int PALETTE		= 2;
	
	public static final int TYPES_MASK	= 0x0F;

	public void init();
	public Container getContentPane();
	public void setTitle( String title );
	public String getTitle();
	public Component getWindow();
	public void pack();
	public void setDefaultCloseOperation( int mode );
	public void dispose();
	public void setVisible( boolean b );
	public boolean isVisible();
	public void toFront();
	public boolean isFloating();
//	public void addWindowFocusListener( WindowFocusListener l );
//	public void removeWindowFocusListener( WindowFocusListener l );
//	public void addWindowListener( WindowListener l );
//	public void removeWindowListener( WindowListener l );
	public void addListener( Listener l );
	public void removeListener( Listener l );
//	public boolean hasFocus();
//	public boolean isFocused();
//	public void requestFocus();
	public boolean isActive();
//	public int getWidth();
//	public int getHeight();
	public Dimension getSize();
	public Rectangle getBounds();
	public void setBounds( Rectangle r );
	public void setSize( Dimension d );
	public void setPreferredSize( Dimension d );
//	public void setSize( int width, int height );
	public void setJMenuBar( JMenuBar m );
	public JMenuBar getJMenuBar();
	public InputMap getInputMap( int condition );
	public ActionMap getActionMap();
	public Window[] getOwnedWindows();
	public void setFocusTraversalKeysEnabled( boolean enabled );
	public void setDirty( boolean dirty );
	public void setLocationRelativeTo( Component c );
//	public void setUndecorated( boolean b );
	public void setResizable( boolean b );
	public boolean isResizable();
	
	public void revalidate();

	public static interface Listener
	{
		public void windowOpened( Event e );
		public void windowClosing( Event e );
		public void windowClosed( Event e );
		public void windowIconified( Event e );
		public void windowDeiconified( Event e );
		public void windowActivated( Event e );
		public void windowDeactivated( Event e );
//		public void windowGainedFocus( Event e );
//		public void windowLostFocus( Event e );
	}
	
	public static class Adapter
	implements Listener
	{
		public void windowOpened( Event e ) {}
		public void windowClosing( Event e ) {}
		public void windowClosed( Event e ) {}
		public void windowIconified( Event e ) {}
		public void windowDeiconified( Event e ) {}
		public void windowActivated( Event e ) {}
		public void windowDeactivated( Event e ) {}
//		public void windowGainedFocus( Event e ) {}
//		public void windowLostFocus( Event e ) {}
	}
	
	public static class Event
	extends java.awt.AWTEvent
	{
		public static int	WINDOW_FIRST		= WindowEvent.WINDOW_FIRST;
		public static int	WINDOW_OPENED		= WindowEvent.WINDOW_OPENED;
		public static int	WINDOW_CLOSING		= WindowEvent.WINDOW_CLOSING;
		public static int	WINDOW_CLOSED		= WindowEvent.WINDOW_CLOSED;
		public static int	WINDOW_ICONIFIED	= WindowEvent.WINDOW_ICONIFIED;
		public static int	WINDOW_DEICONIFIED	= WindowEvent.WINDOW_DEICONIFIED;
		public static int	WINDOW_ACTIVATED	= WindowEvent.WINDOW_ACTIVATED;
		public static int	WINDOW_DEACTIVATED	= WindowEvent.WINDOW_DEACTIVATED;
//		public static int	WINDOW_GAINED_FOCUS	= WindowEvent.WINDOW_GAINED_FOCUS;
//		public static int	WINDOW_LOST_FOCUS	= WindowEvent.WINDOW_LOST_FOCUS;
	
		private final AbstractWindow w;
	
		public Event( AbstractWindow source, int id )
		{
			super( source, id );
			
			w = source;
		}
		
		public AbstractWindow getWindow()
		{
			return w;
		}
		
		public static Event convert( AbstractWindow w, WindowEvent e )
		{
			return new Event( w, e.getID() );
		}

		public static Event convert( AbstractWindow w, InternalFrameEvent e )
		{
			return new Event( w, e.getID() - InternalFrameEvent.INTERNAL_FRAME_FIRST + WINDOW_FIRST );
		}
	}
}