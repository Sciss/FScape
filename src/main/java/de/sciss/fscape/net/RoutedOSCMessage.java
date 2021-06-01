/*
 *  RoutedOSCMessage.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		20-Jan-06	created
 *		03-Oct-06	copied from eisenkraut
 */

package de.sciss.fscape.net;

import de.sciss.net.OSCMessage;

import java.io.IOException;
import java.net.SocketAddress;

public class RoutedOSCMessage {

    public final OSCMessage			msg;
    public final SocketAddress		addr;
    public final long				when;
    public final OSCRoot			server;

    private final String[]			path;
    private final int				pathIdx;
//	private final java.util.List	routers;

//	public RoutedOSCMessage( OSCMessage msg, SocketAddress addr, long when, OSCRoot server,
//							 String[] path, int pathIdx, java.util.List routers )
    public RoutedOSCMessage( OSCMessage msg, SocketAddress addr, long when, OSCRoot server, String[] path, int pathIdx )
    {
        this.msg		= msg;
        this.addr		= addr;
        this.when		= when;
        this.server		= server;
        this.path		= path;
        this.pathIdx	= pathIdx;
//		this.routers	= routers;
    }

//	public void route()
//	{
//		for( int i = 0; i < routers.size(); i++ ) {
//			((OSCRouter) routers.get( i )).routeMessage( this );
//		}
//	}

    public int getPathIndex()
    {
        return pathIdx;
    }

    public int getPathCount()
    {
        return path.length;
    }

    public String getPathComponent( int idx )
    {
        return path[ idx ];
    }

    public String getPathComponent()
    {
        return path[ pathIdx ];
    }

    public String getNextPathComponent()
    {
        return getNextPathComponent( 1 );
    }

    public String getNextPathComponent( int skip )
    {
        return path[ pathIdx + skip ];
    }

    public boolean hasNext()
    {
        return( hasNext( 1 ));
    }

    public boolean hasNext( int numComponents )
    {
        return( pathIdx + numComponents < path.length );
    }

//	public RoutedOSCMessage next( java.util.List routers )
//	{
//		return next( 1, routers );
//	}

    public RoutedOSCMessage next()
    {
        return next( 1 );
    }

//	public RoutedOSCMessage next( int skip, java.util.List routers )
//	{
//		return new RoutedOSCMessage( msg, addr, when, server, path, pathIdx + skip, routers );
//	}

    public RoutedOSCMessage next( int skip )
    {
        return new RoutedOSCMessage( msg, addr, when, server, path, pathIdx + skip );
    }

//	/**
//	 *	Queries must have the form
//	 *	[ <path>, <queryID>, <property1> [, <property2> ... ]]
//	 *	; this method is called with an array of values for all the properties
//	 *	such that values[0] is the value of property1, values[1] is the value of property2 etc.
//	 *	; this method replies with
//	 *	[ "/query.reply", <queryID>, <value1> [, <value2> ... ]
//	 *	to the sender.
//	 */
//	public void replyQuery( Object[] values )
//	throws IOException
//	{
//		final Object[] args = new Object[ values.length + 1 ];
//		args[ 0 ] = msg.getArg( 0 );
//		System.arraycopy( values, 0, args, 1, values.length );
//
//		server.send( new OSCMessage( OSC_QUERYREPLY, args ), addr );
//	}

//	/**
//	 */
//	public void replyGet( Object[] values )
//	throws IOException
//	{
//		final Object[] args = new Object[ values.length + 1 ];
//		args[ 0 ] = msg.getArg( 0 );
//		System.arraycopy( values, 0, args, 1, values.length );
//		server.send( new OSCMessage( OSC_GETREPLY, args ), addr );
//	}

    public void reply( String cmd, Object[] args )
    throws IOException
    {
        server.send( new OSCMessage( cmd, args ), addr );
    }

//	public void replyFailed()
//	throws IOException
//	{
//		replyFailed( 0 );
//	}

    public void tryReplyFailed( int argCount ) {
        try {
            replyFailed( argCount );
        } catch( IOException e1 ) {
            e1.printStackTrace();
        }
    }

    public void replyFailed( int argCount )
    throws IOException
    {
        final Object[] args = new Object[ argCount + 1 ];
        args[ 0 ] = msg.getName();
        for( int i = 0; i < argCount; i++ ) {
            args[ i + 1 ] = msg.getArg( i );
        }
        server.send( new OSCMessage( OSCRoot.OSC_FAILEDREPLY, args ), addr );
    }

    public void tryReplyDone( int copyArgCount, Object[] doneArgs ) {
        try {
            replyDone( copyArgCount, doneArgs );
        } catch( IOException e1 ) {
            e1.printStackTrace();
        }
    }

    public void replyDone( int copyArgCount, Object[] doneArgs )
    throws IOException
    {
        final Object[]	args	= new Object[ copyArgCount + doneArgs.length + 1 ];
        int				j		= 0;
        args[ j++ ] = msg.getName();
        for( int i = 0; i < copyArgCount; i++ ) {
            args[ j++ ] = msg.getArg( i );
        }
        for( int i = 0; i < doneArgs.length; i++ ) {
            args[ j++ ] = doneArgs[ i ];
        }
        server.send( new OSCMessage( OSCRoot.OSC_DONEREPLY, args ), addr );
    }
}
