/*
 *  MenuRadioGroup.java
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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

import de.sciss.app.AbstractWindow;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 30-Aug-06
 */
public class MenuRadioGroup
{
	private final Map mapGroups	= new HashMap();	// key = AbstractWindow, value = ButtonGroup
	private int selected = -1;

	public MenuRadioGroup()
	{
	}
	
	protected void add( AbstractWindow w, AbstractButton b )
	{
		ButtonGroup g = (ButtonGroup) mapGroups.get( w );
	
		if( g == null ) {
			g = new ButtonGroup();
			mapGroups.put( w, g );
		}
		g.add( b );
		if( g.getButtonCount() == (selected + 1) ) g.setSelected( b.getModel(), true );
	}
	
	public void setSelected( int index )
	{
		ButtonGroup		g;
		Enumeration		buttons;
		AbstractButton	b;
		
		for( Iterator iter = mapGroups.values().iterator(); iter.hasNext(); ) {
			g		= (ButtonGroup) iter.next();
			buttons = g.getElements();
			b		= null;
			for( int i = 0; (i <= index) && buttons.hasMoreElements(); i++ ) {
				b	= (AbstractButton) buttons.nextElement();
			}
//			if( b != null ) b.setSelected( true );
			if( b != null ) g.setSelected( b.getModel(), true );
//			for( int i = 0; buttons.hasMoreElements(); i++ ) {
//				b	= (AbstractButton) buttons.nextElement();
//System.err.println( "index : " + i + "; b.getText() = "+b.getText() );
//				g.setSelected( b.getModel(), i == index );
//			}
		}
		
		selected = index;
	}
	
	protected void remove( AbstractWindow w, AbstractButton b )
	{
		final ButtonGroup g = (ButtonGroup) mapGroups.get( w );

		if( g == null ) throw new IllegalArgumentException( "Element was not found : " + w );

		g.remove( b );
		if( g.getButtonCount() == 0 ) {
			mapGroups.remove( w );
		}
	}
}