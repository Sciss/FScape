/*
 *  VirtualChoice.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.fscape.gui;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 *  JComboBox subclass that remembers "impossible"
 *	selection, i.e. those with item indices
 *	beyond the current item list which may become
 *	available in the future.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class VirtualChoice
extends JComboBox
{
// -------- private Variablen --------

	protected int		virtualIndex;
	protected Vector	listeners;

// -------- public Methoden --------

	public VirtualChoice()
	{
		super();
		virtualIndex	= super.getSelectedIndex();
		listeners		= new Vector();
	}
	
// XXX
//	public void addItem( String item )
//	{
//		addItem( item );
//	}

	/**
	 *	Wenn der virtuelle Index nach dem Add-Vorgang
	 *	real als Item existiert, so wird dieses angewaehlt
	 */
	public void addItem( String item )
	{
		super.addItem( item );
		if( (getItemCount() - 1) == virtualIndex ) {
			try {
				super.setSelectedIndex( virtualIndex );
			} catch( IllegalArgumentException e ) {}
		}
	}

	public void insertItemAt( String item, int index )
	throws IllegalArgumentException
	{
		super.insertItemAt( item, index );
		if( (getItemCount() - 1) == virtualIndex ) {
			super.setSelectedIndex( virtualIndex );
		}
	}             

	public int getSelectedIndex()
	{
		return virtualIndex;
	}

	/**
	 *	defacto wird keine Exception ausgeloest;
	 *	falls der Index groesser als die Zahl der Items
	 *	ist, wird er intern gespeichert
	 *
	 *	Eingetragene spezielle ItemListener werden benachrichtigt
	 */
	public void setSelectedIndex( int pos )
	throws IllegalArgumentException
	{
		if( pos >= 0 ) {
			virtualIndex = pos;
// System.out.println( "select "+virtualIndex );
			if( getItemCount() > virtualIndex ) {
				super.setSelectedIndex( pos );
			}
		}
		synchronized( listeners ) {
			int size = listeners.size();
			if( size > 0 ) {
				Object[] obj= getSelectedObjects();
				ItemEvent e = new ItemEvent( this, ItemEvent.ITEM_STATE_CHANGED,
											 ((obj != null) && (obj.length > 0)) ? obj : null,
											 ItemEvent.SELECTED );
				for( int i = 0; i < size; i++ ) {
					((ItemListener) listeners.elementAt( i )).itemStateChanged( e );
				}
			}
		}
	}

//	public void removeAll()
//	{
//		int pos = virtualIndex;
//		super.removeAll();
//		virtualIndex = pos;
//System.out.println( "removedAll "+virtualIndex );
//	}

//	public void remove( int pos )
//	{
//		int pos2 = virtualIndex;
//		super.remove( pos );
//		virtualIndex = pos2;
//System.out.println( "removed; "+virtualIndex );
//	}

	public void addSpecialItemListener( ItemListener l )
	{
		synchronized( listeners ) {
			listeners.addElement( l );
		}
	}

	public void removeSpecialItemListener( ItemListener l )
	{
		synchronized( listeners ) {
			listeners.removeElement( l );
		}
	}
}
// class VirtualChoice
