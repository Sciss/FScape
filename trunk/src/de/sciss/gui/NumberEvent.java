/*
 *  NumberEvent.java
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.NumberEvent
 */

package de.sciss.gui;

import de.sciss.app.BasicEvent;

/**
 *  This kind of event is fired
 *  from a <code>NumberField</code> gadget when
 *  the user modified its contents.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.25, 05-May-06
 *
 *  @see	NumberField#addListener( NumberListener )
 *  @see	NumberListener
 *  @see	java.lang.Number
 */
public class NumberEvent
extends BasicEvent
{
// --- ID values ---
	/**
	 *  returned by getID() : the number changed
	 */
	public static final int CHANGED		= 0;

	private final Number	number;
	private final boolean	adjusting;

	/**
	 *  Constructs a new <code>NumberEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>CHANGED</code>
	 *  @param  when	system time when the event occured
	 *  @param  number  the new number
	 */
	public NumberEvent( Object source, int ID, long when, Number number, boolean adjusting )
	{
		super( source, ID, when );
	
		this.number		= number;
		this.adjusting	= adjusting;
	}
	
	public boolean isAdjusting()
	{
		return adjusting;
	}
	
	/**
	 *  Queries the new number
	 *
	 *  @return the new <code>Number</code> of the
	 *			<code>NumberField</code>. This is either
	 *			an <code>Long</code> or a <code>Double</code>
	 *			depening of the <code>NumberField</code>'s
	 *			<code>NumberSpace</code>.
	 *
	 *  @see	de.sciss.util.NumberSpace#isInteger()
	 */
	public Number getNumber()
	{
		return number;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof NumberEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {
			
			return true;

		} else return false;
	}
}
