/*
 *  MenuGroup.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;

import de.sciss.app.AbstractWindow;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 30-Aug-06
 */
public class MenuGroup
extends MenuItem // implements MenuNode
{
	protected final List		collElements	= new ArrayList();
	protected final Map			mapElements		= new HashMap();	// key = (String) id, value = (MenuNode) element
	
	public MenuGroup( String id, Action a )
	{
		super( id, a );
	}
	
	public MenuGroup( String id, String text )
	{
		this( id, new DummyAction( text ));
	}
	
	public MenuNode get( String id )
	{
		final int i	= id.indexOf( '.' );
		
		if( i == -1 ) {
			return (MenuNode) mapElements.get( id );
		} else {
			final MenuGroup mg = (MenuGroup) mapElements.get( id.substring( 0, i ));
			if( mg == null ) throw new NullPointerException( id );
			return mg.get( id.substring( i + 1 ));
		}
	}

	public int indexOf( String id )
	{
		final int i	= id.indexOf( '.' );
		
		if( i == -1 ) {
			return collElements.indexOf( mapElements.get( id ));
		} else {
			final MenuGroup mg = (MenuGroup) mapElements.get( id.substring( 0, i ));
			if( mg == null ) throw new NullPointerException( id );
			return mg.indexOf( id.substring( i + 1 ));
		}
	}
	
	public MenuNode getByAction( Action a )
	{
		MenuNode n;
		
		for( Iterator iter = collElements.iterator(); iter.hasNext(); ) {
			n = (MenuNode) iter.next();
			if( n.getAction() == a ) return n;
		}
		
		return null;
	}

	// adds to the tail
	public void add( MenuNode n )
	{
		add( n, collElements.size() );
	}

	// inserts at given index
	public void add( MenuNode n, int index )
	{
		if( mapElements.put( n.getID(), n ) != null ) throw new IllegalArgumentException( "Element already added : " + n );
		
		Realized r;
		
		collElements.add( index, n );
		
		for( Iterator iter = mapRealized.values().iterator(); iter.hasNext(); ) {
			r = (Realized) iter.next();
			r.c.add( n.create( r.w ), index );
		}
	}
	
	public void addSeparator()
	{
		add( new MenuSeparator() );
	}
	
	public void remove( int index )
	{
		final MenuNode	mn = (MenuNode) collElements.remove( index );
		Realized		r;
		
		mapElements.remove( mn.getID() );

		for( Iterator iter = mapRealized.values().iterator(); iter.hasNext(); ) {
			r = (Realized) iter.next();
			r.c.remove( index );
			mn.destroy( r.w );
		}
	}
	
	public void remove( MenuNode n )
	{
		remove( collElements.indexOf( n ));
	}

	public JComponent create( AbstractWindow w )
	{
		final JComponent c = super.create( w );
	
		for( Iterator iter = collElements.iterator(); iter.hasNext(); ) {
			c.add( ((MenuNode) iter.next()).create( w ));
		}
	
		return c;
	}

	public void destroy( AbstractWindow w )
	{
		super.destroy( w );
	
		for( Iterator iter = collElements.iterator(); iter.hasNext(); ) {
			((MenuNode) iter.next()).destroy( w );
		}
	}
	
	protected JComponent createComponent( Action a )
	{
		return new JMenu( a );
	}

	public void putMimic( String id, AbstractWindow w, Action a )
	{
		final MenuItem mi = (MenuItem) get( id );
		if( mi == null ) throw new NullPointerException( id );

		final Action src = mi.getAction();
		a.putValue( Action.NAME, src.getValue( Action.NAME ));
		a.putValue( Action.SMALL_ICON, src.getValue( Action.SMALL_ICON ));
		a.putValue( Action.ACCELERATOR_KEY, src.getValue( Action.ACCELERATOR_KEY ));
		a.putValue( Action.MNEMONIC_KEY, src.getValue( Action.MNEMONIC_KEY ));
		a.putValue( Action.SHORT_DESCRIPTION, src.getValue( Action.SHORT_DESCRIPTION ));
		a.putValue( Action.LONG_DESCRIPTION, src.getValue( Action.LONG_DESCRIPTION ));
		
		mi.put( w, a );
	}

	public void put( String id, AbstractWindow w, Action a )
	{
		final MenuItem mi = (MenuItem) get( id );
		if( mi == null ) throw new NullPointerException( id );
		mi.put( w, a );
	}
}