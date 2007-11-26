/*
 *  BasicEvent.java
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
 *		20-May-05	created from de.sciss.meloncillo.util.BasicEvent
 */

package de.sciss.app;

import java.util.EventObject;

/**
 *  <code>BasicEvent</code> is the superclass of all events
 *  to be processed through <code>EventManager</code>s.
 *  It subclases <code>java.util.EventObject</code> and thus
 *  inherits an event <code>source</code> object.
 *  <p>
 *  The source
 *  is usually the object that caused the event to be dispatched,
 *  see the Timeline's setPosition for an example of the source
 *  usage. This allows objects which both dispatch and receive
 *  events to recognize if the event was fired by themselves,
 *  in which case they might optimize graphical updates or simply
 *  ignore the event, or by other objects.
 *  <p>
 *  Furthermore, a time tag (<code>getWhen()</code>) can be
 *  read to find out when the event was generated.
 *  <p>
 *  If events are dispatched at a heavy frequency, the
 *  <code>incorporate</code> method can help to shrink the
 *  queue by fusing events of the same type.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.62, 17-Oct-06
 *
 *  @see	EventManager
 */
public abstract class BasicEvent
extends EventObject
{
	private final int	id;
	private final long	when;

	/**
	 *  Constructs a new <code>BasicEvent</code>.
	 *
	 *  @param  source  Since <code>BasicEvent</code>
	 *					is a subclass of <code>java.util.EventObject</code>,
	 *					the given 'source' is directly passed to
	 *					the superclass and can be queried with <code>getSource()</code>.
	 *					The <code>source</code> describes the object that
	 *					originated an action.
	 *  @param  id		type of action depending on the concrete
	 *					subclass. Generally the <code>id</code> is used to
	 *					distinguish between different method calls
	 *					on the registered listeners, hence will be
	 *					usually ignored by the listeners themselves.
	 *  @param  when	When the event was generated. See <code>getWhen()</code>.
	 */
	public BasicEvent( Object source, int id, long when )
	{
		super( source );
	
		this.id		= id;
		this.when   = when;
	}
	
	/**
	 *  Requests an identifier specifying the
	 *  exact type of action that was performed.
	 *
	 *  @return a subclass specific identifier
	 */
	public int getID()
	{
		return id;
	}

	/**
	 *  State whens the event has been generated,
	 *  a timestamp specifying system time millisecs
	 *  as returned by <code>System.currentTimeMillis()</code>.
	 *
	 *  @return time when the event was generated
	 */
	public long getWhen()
	{
		return when;
	}
	
	/**
	 *  Asks the event to incorporate the action
	 *  described by another (older) event.
	 *  This method has been created to reduce overhead;
	 *  when many events are added to the event queue
	 *  of an ELM, this allows to fuse two adjectant
	 *  events. The idea is mainly based on the <code>replaceEdit()</code>
	 *  method of the <code>javax.swing.undo.UndoableEdit</code>
	 *  interface; a pendant of a symmetric <code>addEdit()</code>
	 *  like method is not provided because it seems to
	 *  be unnecessary.
	 *  <p>
	 *  Implementation notes : the <code>oldEvent</code> should
	 *  generally only be incorporated if it refers to
	 *  the same source object (<code>getSource()</code>) and has
	 *  the same ID (<code>getD()</code>). the
	 *  timestamp of the current event should not be modified.
	 *
	 *  @param		oldEvent	the most recent event in the queue
	 *							which might be incorporated by this
	 *							new event.
	 *  @return		<code>true</code> if this object was able to
	 *				incorporate the older event. in this
	 *				case the <code>oldEvent</code> is removed from the
	 *				event queue. <code>false</code> states
	 *				that the <code>oldEvent</code> was incompatible and
	 *				should remain in the queue.
	 *
	 *  @see	javax.swing.undo.UndoableEdit#replaceEdit( UndoableEdit )
	 */
	public abstract boolean incorporate( BasicEvent oldEvent );
}