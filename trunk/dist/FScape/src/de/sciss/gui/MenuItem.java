/*
 *  MenuItem.java
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
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import de.sciss.app.AbstractWindow;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 30-Aug-06
 */
public class MenuItem
implements MenuNode
{
	private final String	id;
	private final Action	a;
	private final Map		mapWindowActions	= new HashMap();	// key = AbstractWindow; value = Action
	protected final Map		mapRealized			= new HashMap();

	public MenuItem( String id, Action a )
	{
		this.id	= id;
		this.a	= a;
	}

	public MenuItem( String id, String text )
	{
		this( id, new DummyAction( text ));
		setEnabled( false );
	}

	public MenuItem( String id, String text, KeyStroke stroke )
	{
		this( id, new DummyAction( text, stroke ));
		setEnabled( false );
	}

	public void setEnabled( boolean b )
	{
		a.setEnabled( b );
	}
	
	public String getID()
	{
		return id;
	}
	
	public void put( AbstractWindow w, Action a )
	{
		if( mapWindowActions.put( w, a ) != null ) throw new IllegalArgumentException( "Window specific action has already been added" );
	}
	
	public void remove( AbstractWindow w )
	{
		if( mapWindowActions.remove( w ) == null ) throw new IllegalArgumentException( "Window specific action not found" );
	}
	
	public Action getAction()
	{
		return a;
	}
	
	public Action getAction( AbstractWindow w )
	{
		final Action wa = (Action) mapWindowActions.get( w );
		return( wa == null ? a : wa );
	}

	public JComponent create( AbstractWindow w )
	{
		final JComponent c = createComponent( getAction( w ));
		mapRealized.put( w, new Realized( w, c ));
		return c;
	}

	protected JComponent createComponent( Action a )
	{
		return new JMenuItem( a );
	}
	
	public void destroy( AbstractWindow w )
	{
		if( mapRealized.remove( w ) == null ) throw new IllegalArgumentException( "Element was not found : " + w );
		mapWindowActions.remove( w );
	}
		
	// -------------------- internal classes --------------------
	
	protected static class Realized
	{
		protected final AbstractWindow	w;
		protected final JComponent		c;
	
		private Realized( AbstractWindow w, JComponent c )
		{
			this.w	= w;
			this.c	= c;
		}
	}
}