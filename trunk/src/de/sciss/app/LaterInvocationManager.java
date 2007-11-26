/*
 *  LaterInvocationManager.java
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
 *		20-May-05	created from de.sciss.meloncillo.util.LaterInvocationManager
 */

package de.sciss.app;

import java.util.prefs.PreferenceChangeEvent;

/**
 *  The LaterInvocationManager is a utility
 *  bastard synthesized from an EventManager
 *  and a Runnable. It's useful for queueing activities
 *  from outside the Swing event thread to be processed later
 *  in the event thread.
 *  <p>
 *  Note that in test scenarios on apple's VM, PreferenceChangeEvents
 *  were dispatched in a thread different from the normal Swing event
 *  thread. Due to this fact a lot of synchronization issues arise,
 *  so we decided to pass more or less all preference changes to a
 *  LaterInvocationManager.Listener instance...
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.13, 05-May-06
 *
 *  @todo   conduct further research on the preference dispatching thread
 *			and the necessity to invoke processing on the swing thread
 */
public class LaterInvocationManager
extends EventManager
implements EventManager.Processor
{
	/**
	 *  Constructs a new <code>LaterInvocationManager</code>
	 *  with one particular listener. Note that it's possible
	 *  to add more listeners through the standard mechanism
	 *  of <code>EventManager</code>, however situations
	 *  are rare where this would be useful.
	 *
	 *  @param  listener	This listener's laterInvocation method
	 *						gets called, when new objects have been
	 *						queued and the VM arrives at the event
	 *						dispatching thread.
	 */
	public LaterInvocationManager( LaterInvocationManager.Listener listener )
	{
		super();
		this.eventProcessor = this;		// egocentric radio waves
		this.addListener( listener );
	}
	
	/**
	 *  Queues an object for later processing. The listener's
	 *  laterInvocation method will be called later in the
	 *  event dispatcher thread
	 *
	 *  @param  o   the object to pass to the laterInvocation method.
	 *				beware not to pass a null value which will
	 *				result in an IllegalArgumentException.
	 */
	public void queue( Object o )
	{
		dispatchEvent( new LaterInvocationManager.Event( o ));
		if( EventManager.DEBUG_EVENTS && o instanceof PreferenceChangeEvent ) {
			PreferenceChangeEvent e = (PreferenceChangeEvent) o;
			// because addListener in the constructor may be
			// postponed, it's possible that we don't find our client here
			System.err.println( "queue "+(this.countListeners() == 0 ? "[client pending]: " :
				"[client "+this.getListener(0).getClass().getName()+"]: ")+e.getKey()+" = "+e.getNewValue() );
		}
	}

	/**
	 *  This is called by the EventManager
	 *  which is in fact ourself ;-)
	 *  if new events are to be processed. This
	 *  will invoke the listener's <code>laterInvocation</code> method.
	 */
	public void processEvent( BasicEvent e )
	{
		LaterInvocationManager.Listener listener;
		int i;
		
		for( i = 0; i < this.countListeners(); i++ ) {
			listener = (LaterInvocationManager.Listener) this.getListener( i );
			listener.laterInvocation( e.getSource() );
		} // for( i = 0; i < this.countListeners(); i++ )
	}
	
	/**
	 *  A simple wrapper <code>BasicEvent</code>.
	 *  The object from the queue method is passed
	 *  as the event's source.
	 *  
	 *  @todo   create an incorporate mechanism for
	 *			PreferenceChageEvent sources.
	 */
	private static class Event
	extends BasicEvent
	{
		private Event( Object o )
		{
			super( o, 0, System.currentTimeMillis() );
		}

		/**
		 *  Returns false always at the moment
		 */
		public boolean incorporate( BasicEvent oldEvent )
		{
			return false;
		}
	}
	
	/**
	 *  A simple interface describing
	 *  the method that gets called from
	 *  the event dispatching thread when
	 *  new objects have been queued.
	 */
	public interface Listener
	{
		/**
		 *  Called later in the event thread,
		 *  this passes the object given to
		 *  the queue method
		 *
		 *  @param  o   object as passed to <code>queue</code>
		 */
		public void laterInvocation( Object o );
	}
}