/*
 *  ProcessorEvent.java
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
 *		21-May-05	created
 */

package de.sciss.fscape.proc;

import de.sciss.app.BasicEvent;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 21-May-05
 */
public class ProcessorEvent
extends BasicEvent
{
	public static final int STARTED		= 0;
	public static final int STOPPED		= 1;
	public static final int PAUSED		= 2;
	public static final int RESUMED		= 3;
	public static final int PROGRESS	= 4;

	private final Processor proc;

	/**
	 *  Constructs a new <code>PathEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>CHANGED</code>
	 *  @param  when	system time when the event occured
	 *  @param  path	the new path
	 */
	public ProcessorEvent( Object source, int ID, long when, Processor proc )
	{
		super( source, ID, when );
	
		this.proc		= proc;
	}

	/**
	 *  Queries the processor
	 *
	 *  @return the processor who emitted the event
	 */
	public Processor getProcessor()
	{
		return proc;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof ProcessorEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() &&
			this.proc == ((ProcessorEvent) oldEvent).proc ) {
			
			return true;

		} else return false;
	}
}
