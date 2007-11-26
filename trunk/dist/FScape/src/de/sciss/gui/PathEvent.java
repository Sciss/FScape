/*
 *  PathEvent.java
 *  de.sciss.gui package
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
 *		20-May-05	created from de.sciss.meloncillo.gui.PathEvent
 */

package de.sciss.gui;

import java.io.File;

import de.sciss.app.BasicEvent;

/**
 *  This kind of event is fired
 *  from a <code>PathField</code> or
 *  <code>PathButton</code> gadget when
 *  the user modified the path.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-06
 *
 *  @see		PathField#addPathListener( PathListener )
 *  @see		PathListener
 *  @see		java.io.File
 */
public class PathEvent
extends BasicEvent
{
	// --- ID values ---
	/**
	 *  returned by getID() : the path changed
	 */
	public static final int CHANGED		= 0;

	private final File	path;

	/**
	 *  Constructs a new <code>PathEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>CHANGED</code>
	 *  @param  when	system time when the event occured
	 *  @param  path	the new path
	 */
	public PathEvent( Object source, int ID, long when, File path )
	{
		super( source, ID, when );
	
		this.path		= path;
	}
	
	/**
	 *  Queries the new path
	 *
	 *  @return the new path of the <code>PathField</code>
	 *			or <code>PathButton</code>.
	 */
	public File getPath()
	{
		return path;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof PathEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {
			
			// XXX beware, when the actionID and actionObj
			// are used, we have to deal with them here
			
			return true;

		} else return false;
	}
}
