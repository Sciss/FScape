/*
 *  EventManager.java
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
 *		20-May-05	created from from de.sciss.meloncillo.util.EventManager
 *		06-Aug-05	added dispose()
 */

package de.sciss.app;

import java.awt.EventQueue;
import java.util.ArrayList;

import de.sciss.util.Disposable;

/**
 *  A custom event dispatcher which
 *  carefully deals with synchronization issues.
 *  Assuming, the synchronization requests specified for
 *  some methods are fulfilled, this class is completely
 *  thread safe.
 *  <p>
 *  It is constructed using a second object, the manager's
 *  processor which will be invoked whenever new events are
 *  available in the event FIFO queue. the processor is then
 *  responsible for querying all registered listeners and
 *  calling their appropriate event listening methods.
 *  <p>
 *  Event dispatching is deferred to the Swing thread execution
 *  time since this makes the whole application much more
 *  predictable and easily synchronizable.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 17-Oct-06
 */
public class EventManager
implements Runnable, Disposable
{
	public static final boolean DEBUG_EVENTS	= false;
	
	private final ArrayList		collListeners   = new ArrayList();  // sync'ed because always in Swing thread
	private final ArrayList		collQueue		= new ArrayList();  // sync'ed through synchronized( this )
	private boolean				paused			= false;

	protected EventManager.Processor eventProcessor;

	public EventManager( EventManager.Processor eventProcessor )
	{
		this.eventProcessor = eventProcessor;
	}
	
	protected EventManager() {}
	
	public void dispose()
	{
		synchronized( this ) {
			collListeners.clear();
			collQueue.clear();
		}
	}
	
	/**
	 *  Adds a new listener. The listner
	 *  will receive all events queued after this
	 *  method is called. Events already in queue
	 *  at the moment this method is called are not
	 *  passed to the listener.
	 *
	 *  @param  listener	the listener to add
	 */
	public void addListener( Object listener )
	{
		if( listener != null ) {
			synchronized( this ) {
				// since methods executed within the eventProcessor's run method
				// are possible candidates for calling addListener(), we postpone
				// the adding so it is acertained that the getListener() calls
				// in the eventProcessor's run method won't be disturbed!!
				collQueue.add( new PostponedAction( listener, true ));
				EventQueue.invokeLater( this );
			}
		}
	}
	
	/**
	 *  Removes a listener. Similar to the
	 *  adding process, the listener won't receive
	 *  any events queued after this method is called.
	 *  However, when there are events in the queue
	 *  at the moment when this method is called, they
	 *  will still be past to the old listener.
	 *
	 *  @param  listener	the listener to remove. <code>null</code>
	 *						is allowed (no op).
	 */
	public void removeListener( Object listener )
	{
		if( listener != null ) {
			synchronized( this ) {
				// since methods excuted within the eventProcessor's run method
				// are possible candidates for calling removeListener(), we postpone
				// the adding so it is acertained that the getListener() calls
				// in the eventProcessor's run method won't be disturbed!!
				collQueue.add( new PostponedAction( listener, false ));
				EventQueue.invokeLater( this );
			}
		}
	}

	/**
	 *  Called by add/removeListener and dispatchEvent.
	 *  This method makes the postponed
	 *  collection modifications permanent.
	 *  It calls the eventProcessor as long as there
	 *  are events in the queue.
	 */
	public void run()
	{
		Object  o;
		int		eventsInCycle;

		synchronized( this ) {
			if( paused ) return;
			// we only process that many events
			// we find NOW in the queue. if the
			// event processor or its listeners
			// add new events they will be processed
			// in the next later invocation
			eventsInCycle = collQueue.size();
		}

		for( ; eventsInCycle > 0; eventsInCycle-- ) {
			synchronized( this ) {
				o = collQueue.remove( 0 );
			}
			if( o instanceof BasicEvent ) {
				eventProcessor.processEvent( (BasicEvent) o );
			} else if( o instanceof PostponedAction ) {
				if( ((PostponedAction) o).state ) {
					if( !collListeners.contains( ((PostponedAction) o).listener )) {
						collListeners.add( ((PostponedAction) o).listener );
					}
				} else {
					collListeners.remove( ((PostponedAction) o).listener );
				}
			} else {
				assert false : o.getClass().getName();
			}
		}
	}

	/**
	 *  Gets a listener from the list
	 *
	 *  @synchronization	MUST BE CALLED FROM THE EVENT DISPATCH THREAD
	 */
	public Object getListener( int index )
	{
		return( collListeners.get( index ));
	}

	/**
	 *  Get the number of listeners
	 *
	 *  @synchronization	MUST BE CALLED FROM THE EVENT DISPATCH THREAD
	 */
	public int countListeners()
	{
		return( collListeners.size() );
	}
	
	public void debugDump()
	{
		for( int i = 0; i < collListeners.size(); i++ ) {
			System.err.println( "listen "+i+" = "+collListeners.get( i ).toString() );
		}
	}

	/**
	 *  Puts a new event in the queue.
	 *  If the most recent event can
	 *  be incorporated by the new event,
	 *  it will be replaced, otherwise the new
	 *  one is appended to the end. The
	 *  eventProcessor is invoked asynchronously
	 *  in the Swing event thread
	 *
	 *  @param  e   the event to add to the queue.
	 *				before it's added, the event's incorporate
	 *				method will be checked against the most
	 *				recent event in the queue.
	 */
	public void dispatchEvent( BasicEvent e )
	{
		final int		i;
		final boolean	invoke;
		final Object	o;

sync:	synchronized( this ) {
			invoke  = !paused;
			i		= collQueue.size() - 1;
			if( i >= 0 ) {
				o = collQueue.get( i );
				if( (o instanceof BasicEvent) && e.incorporate( (BasicEvent) o )) {
					collQueue.set( i, e );
					break sync;
				}
			}
			collQueue.add( e );
		} // synchronized( this )

		if( invoke ) EventQueue.invokeLater( this );
	}
	
	/**
	 *  Pauses event dispatching.
	 *  Events will still be queued, but the
	 *  dispatcher will wait to call any processors
	 *  until resume() is called.
	 */
	public void pause()
	{
		synchronized( this ) {
//System.err.println( "pause" );
			paused = true;
		} // synchronized( this )
	}
	
	/**
	 *  Resumes event dispatching.
	 *  Any events in the queue will be
	 *  distributed as normal.
	 */
	public void resume()
	{
		boolean invoke;
	
		synchronized( this ) {
//System.err.println( "resume" );
			paused = false;
			invoke = !collQueue.isEmpty();
		} // synchronized( this )

		if( invoke ) EventQueue.invokeLater( this );
	}

// -------------------- processor interface --------------------

	/**
	 *  Callers of the EventManager constructor
	 *  must provide an object implementing this interface
	 */
	public interface Processor
	{
		/**
		 *  Processes the next event in the queue.
		 *  This gets called in the event thread.
		 *  Usually implementing classes should
		 *  loop through all listeners by calling
		 *  elm.countListeners() and elm.getListener(),
		 *  and invoke specific dispatching methods
		 *  on these listeners.
		 */
		public void processEvent( BasicEvent e );
	}

// -------------------- postpone helper class --------------------

	private class PostponedAction
	{
		private final Object   listener;
		private final boolean  state;
		
		private PostponedAction( Object listener, boolean state )
		{
			this.listener   = listener;
			this.state		= state;
		}
	}
}
