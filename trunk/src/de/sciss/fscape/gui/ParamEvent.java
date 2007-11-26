/*
 *  ParamEvent.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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
 *		21-May-05	created from de.sciss.meloncillo.gui.NumberEvent
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;

import de.sciss.app.BasicEvent;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.68, 21-May-05
 */
public class ParamEvent
extends BasicEvent
{
// --- ID values ---
	public static final int CHANGED		= 0;

	private final Param			param;
	private final ParamSpace	space;

	/**
	 */
	public ParamEvent( Object source, int ID, long when, Param param, ParamSpace space )
	{
		super( source, ID, when );
	
		this.param	= param;
		this.space	= space;
	}
	
	public Param getParam()
	{
		return param;
	}

	public ParamSpace getSpace()
	{
		return space;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof ParamEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {
			
			return true;

		} else return false;
	}
}
