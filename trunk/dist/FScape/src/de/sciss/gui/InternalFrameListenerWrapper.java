/*
 *  InternalFrameListenerWrapper.java
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
 *  Changelog:
 *		30-Aug-06	created
 */

package de.sciss.gui;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import de.sciss.app.AbstractWindow;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 30-Aug-06
 */
public class InternalFrameListenerWrapper
implements InternalFrameListener
{
	private static	Map							mapListeners = null;
	private final	AbstractWindow.Listener		l;
	private final	AbstractWindow				w;

	private InternalFrameListenerWrapper( AbstractWindow.Listener l, AbstractWindow w  )
	{
		this.l	= l;
		this.w	= w;
	}
	
	public static void add( AbstractWindow.Listener l, AbstractWindow w )
	{
		final InternalFrameListenerWrapper iflw = new InternalFrameListenerWrapper( l, w );
		((JInternalFrame) w.getWindow()).addInternalFrameListener( iflw );
		if( mapListeners == null ) mapListeners = new HashMap();
		mapListeners.put( w, iflw );
	}
	
	public static void remove( AbstractWindow.Listener l, AbstractWindow w )
	{
		final InternalFrameListenerWrapper iflw = (InternalFrameListenerWrapper) mapListeners.remove( w );
		((JInternalFrame) w.getWindow()).removeInternalFrameListener( iflw );
	}

	public void internalFrameOpened( InternalFrameEvent e )
	{
		l.windowOpened( wrap( e ));
	}
	
	public void internalFrameClosing( InternalFrameEvent e )
	{
		l.windowClosing( wrap( e ));
	}
	
	public void internalFrameClosed( InternalFrameEvent e )
	{
		l.windowClosed( wrap( e ));
	}
	
	public void internalFrameIconified( InternalFrameEvent e )
	{
		l.windowIconified( wrap( e ));
	}
	
	public void internalFrameDeiconified( InternalFrameEvent e )
	{
		l.windowDeiconified( wrap( e ));
	}
	
	public void internalFrameActivated( InternalFrameEvent e )
	{
		l.windowActivated( wrap( e ));
	}
	
	public void internalFrameDeactivated( InternalFrameEvent e )
	{
		l.windowDeactivated( wrap( e ));
	}
	
	private AbstractWindow.Event wrap( InternalFrameEvent e )
	{
		return AbstractWindow.Event.convert( w, e );
	}
}