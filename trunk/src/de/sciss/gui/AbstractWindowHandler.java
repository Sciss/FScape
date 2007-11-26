/*
 *  AbstractWindowHandler.java
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
 *		20-May-05	created
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;
import de.sciss.app.GraphicsHandler;

/**
 *  A rudimentary implementation of the <code>de.sciss.app.WindowHandler</code>
 *	interface.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.17, 09-Jul-06
 */
public abstract class AbstractWindowHandler
implements de.sciss.app.WindowHandler
{
	private final List	collWindows	= new ArrayList();

	protected AbstractWindowHandler()
	{
		super();
	}

	/**
	 *	@synchronization	this method is synchronized
	 */
	public void addWindow( AbstractWindow w, Map options )
	{
		synchronized( collWindows ) {
			if( collWindows.contains( w )) {
				throw new IllegalArgumentException( "Duplicate window registration" );
			}
			collWindows.add( w );
		}
	}

	/**
	 *	@synchronization	this method is synchronized
	 */
	public void removeWindow( AbstractWindow w, Map options )
	{
		synchronized( collWindows ) {
			if( !collWindows.remove( w )) {
				throw new IllegalArgumentException( "Tried to remove unknown window" );
			}
		}
	}

	public Iterator getWindows()
	{
		synchronized( collWindows ) {
			return Collections.unmodifiableList( collWindows ).iterator();
		}
	}

	public static void setDeepFont( Container c )
	{
		setDeepFont( c, null );
	}
	
	public static void setDeepFont( Container c, List exclude )
	{
		setDeepFont( c,
			AbstractApplication.getApplication().getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL ), exclude );
	}

	private static void setDeepFont( Container c, Font fnt, List exclude )
	{
		final Component[] comp = c.getComponents();
		
		c.setFont( fnt );
		for( int i = 0; i < comp.length; i++ ) {
			if( (exclude != null) && exclude.contains( comp[ i ])) continue;

			if( comp[ i ] instanceof Container ) {
				setDeepFont( (Container) comp[i], fnt, exclude );
			} else {
				comp[ i ].setFont( fnt );
			}
		}
	}
}