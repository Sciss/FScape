/*
 *  OSCRoot.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		19-Jan-06	created
 *		03-Oct-06	copied from eisenkraut
 */

package de.sciss.fscape.net;

import de.sciss.fscape.Application;
import de.sciss.net.OSCChannel;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCPacket;
import de.sciss.net.OSCServer;
import de.sciss.util.Param;
import de.sciss.util.ParamSpace;

import java.awt.*;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class OSCRoot
        implements OSCRouter, OSCListener, Runnable, PreferenceChangeListener {

    /**
     *	Convenient name for preferences node
     */
    public static final String				DEFAULT_NODE	= "oscserver";

    public static final String				KEY_ACTIVE		= "active";		// boolean
    public static final String				KEY_PROTOCOL	= "protocol";	// String
    public static final String				KEY_PORT		= "port";		// Param

//	public static final Param				DEFAULT_PORT	= new Param( 0x4549, ParamSpace.NONE | ParamSpace.ABS );

    private final Preferences				prefs;

    private OSCServer						serv			= null;
//	private	DatagramChannel					dch				= null;
//	private OSCTransmitter					trns			= null;
//	private OSCReceiver						rcv				= null;
    private boolean							running			= false;
// FFF
//	private OSCGUI							gui				= null;

    private final Pattern					oscPathPtrn		= Pattern.compile( "/" );

    // elements = RoutedOSCMessage instances
    private final List<RoutedOSCMessage> collMessages	= Collections.synchronizedList( new ArrayList<RoutedOSCMessage>() );
    private final OSCRouterWrapper			osc;

    private static final String				OSC_DUMP		= "dumpOSC";

    public static final String				OSC_QUERY		= "query";
    public static final String				OSC_GET			= "get";

    public static final String				OSC_QUERYREPLY	= "/query.reply";
    public static final String				OSC_GETREPLY	= "/get.reply";
    public static final String				OSC_FAILEDREPLY	= "/failed";
    public static final String				OSC_DONEREPLY	= "/done";

    private final Param						defaultPortParam;

    private static OSCRoot				instance;

    public OSCRoot( Preferences prefs, int defaultPort )
    {
        super();

        if( instance != null ) throw new IllegalStateException( "Only one instance allowed" );

        instance		= this;

        this.prefs		= prefs;

        defaultPortParam = new Param(defaultPort, ParamSpace.NONE | ParamSpace.ABS);

        if( prefs.get( KEY_PORT, null ) == null ) {	// create defaults
            prefs.putBoolean( KEY_ACTIVE, false );
            prefs.put( KEY_PORT, defaultPortParam.toString() );
        }
        osc = new OSCRouterWrapper( null, this );
        osc.oscAddRouter( new OSCRouter() {
            public String oscGetPathComponent()
            {
                return OSC_DUMP;
            }

            public void oscRoute( RoutedOSCMessage rom )
            {
                oscCmdDump( rom );
            }

            public void oscAddRouter( OSCRouter subRouter )
            {
                throw new IllegalStateException( "Path endpoint" );
            }

            public void oscRemoveRouter( OSCRouter subRouter )
            {
                throw new IllegalStateException( "Path endpoint" );
            }
        });

//		// not really a path but
//		// the general responder for query replies
//		osc.oscAddRouter( new OSCRouter() {
//			public String oscGetPathComponent()
//			{
//				return OSC_QUERYREPLY;
//			}
//			
//			public void oscRoute( RoutedOSCMessage rom )
//			{
//				oscCmdDump( rom );
//			}
//		
//			public void oscAddRouter( OSCRouter subRouter )
//			{
//				throw new IllegalStateException( "Path endpoint" );
//			}
//		});
    }

    public static OSCRoot getInstance() {
        return instance;
    }

    public void init() {
        if (prefs.getBoolean(KEY_ACTIVE, false)) {
            boot();
        }
        prefs.addPreferenceChangeListener(this);
    }

    public Preferences getPreferences()
    {
        return prefs;
    }

    public void boot() {
        try {
            boot(prefs.get(KEY_PROTOCOL, OSCChannel.TCP), (int) Param.fromPrefs(prefs, KEY_PORT, defaultPortParam).val, true);
        } catch (IOException e1) {
            System.err.println(e1.getClass().getName() + " : " + e1.getLocalizedMessage());
        }
    }

    public void boot(String protocol, int port, boolean loopBack)
            throws IOException {

        synchronized (this) {
            if (running) {
                throw new IllegalStateException("Already booted");
            }

// FFF
//			if( gui == null ) gui = new OSCGUI();

//			final InetSocketAddress addr = loopBack ?
//				new InetSocketAddress( "127.0.0.1", port ) :
//				new InetSocketAddress( InetAddress.getLocalHost(), port );

            try {
//				dch = DatagramChannel.open();
//				dch.socket().bind( addr );

                serv	= OSCServer.newUsing( protocol, port, loopBack );
//				rcv     = new OSCReceiver( dch );
//				trns    = new OSCTransmitter( dch );
//				rcv.addOSCListener( this );
//				rcv.startListening();
                serv.addOSCListener( this );
                serv.start();
//                System.out.println(Application.name + " " +
//                        getResourceString("oscRcvAt") + " " + protocol.toUpperCase() + " " + getResourceString("oscPort") + " " + port);
            }
            catch( IOException e1 ) {
                if( serv != null ) {
                    serv.dispose();
                    serv = null;
//					try {
//						rcv.stopListening();
//					}
//					catch( IOException e2 ) {}
                }
//				if( dch != null ) {
//					try {
//						dch.close();
//					}
//					catch( IOException e2 ) {}
//				}
//				rcv		= null;
//				trns	= null;
//				dch		= null;
                throw e1;
            }
            running	= true;
        }
    }

    public static String getResourceString( String key )
    {
        return key;
    }

    public void send( OSCPacket p, SocketAddress addr )
    throws IOException
    {
        if( running ) {
//System.err.println( "sending to "+addr );
//			trns.send( p, addr );
            serv.send( p, addr );
        } else {
            throw new IllegalStateException( "Not running" );
        }
    }

//	public OSCMessage sendSync( OSCPacket p, InetSocketAddress addr, String doneCmd, String failCmd, int argIdx, Object argMatch )
//	throws IOException
//	{
//		return sendSync( p, addr, doneCmd, failCmd, argIdx, argMatch, 4f );
//	}
//
//	public OSCMessage sendSync( OSCPacket p, InetSocketAddress addr, String doneCmd, String failCmd, int argIdx, Object argMatch, float timeout )
//	throws IOException
//	{
//		final SyncResponder	resp = new SyncResponder( addr, doneCmd, failCmd, argIdx, argMatch );
//		
//		try {
//			synchronized( resp ) {
//				resp.add();
//				send( p, addr );
//				resp.wait( (long) (timeout * 1000) );
//			}
//		}
//		catch( InterruptedException e1 ) {}
//		finally {
//			resp.remove();
//		}
//		return resp.doneMsg;
//	}

    public boolean isRunning()
    {
        synchronized( this ) {
            return running;
        }
    }

//	public Object[] query( InetSocketAddress addr, String path, String[] properties )
//	throws IOException
//	{
//		final Object		queryID = new Integer( ++uniqueID );
//		final Object[]		args	= new Object[ properties.length + 2 ];
//		final OSCMessage	reply;
//		
//		args[ 0 ]	= "query";
//		args[ 1 ]	= queryID;
//		System.arraycopy( properties, 0, args, 2, properties.length );
//		
//		reply = sendSync( new OSCMessage( path, args ), addr, "/query.reply", "/failed", 0, queryID );
//		if( (reply != null) && reply.getName().equals( "/query.reply" )) {
//			final Object[] result = new Object[ Math.min( properties.length, reply.getArgCount() - 1 )];
//			for( int i = 1, j = 0; j < result.length; i++, j++ ) {
//				result[ j ] = reply.getArg( i );
//			}
//			return result;
//		} else {
//			return null;
//		}
//	}

    public void quit()
    {
        synchronized( this ) {
            if( running ) {
                serv.dispose();
                serv = null;
                running = false;
            }
        }
    }

//	/**
//	 *	@synchronization	call only in event thread
//	 */
//	public void oscRemoveRouter( OSCRouter r )
//	{
//		final String cmd = r.oscGetPathComponent();
//
////		synchronized( mapRouters ) {
//			if( mapRouters.remove( cmd ) == null ) {
//				throw new IllegalStateException( "Trying to remove unregistered command '" + cmd + "'" );
//			}
////		}
//	}

    public static void failedUnknownPath( OSCMessage msg )
    {
        failed( msg, "Path not found" );
    }

    public static void failedUnknownPath( RoutedOSCMessage rom )
    {
        failedUnknownPath( rom, rom.getPathIndex() );
    }

    public static void failedUnknownCmd( RoutedOSCMessage rom )
    {
        System.err.println( "FAILURE " +rom.msg.getName() + " " + rom.msg.getArg( 0 ).toString() + " Command not found" );
    }

    public static void failedUnknownPath( RoutedOSCMessage rom, int pathIdx )
    {
        final int		endIdx;
        int				startIdx = 0;
        final String	fullCmd	= rom.msg.getName();

        if( rom.getPathCount() > 0 ) {
            startIdx += rom.getPathComponent( 0 ).length();
        }
        for( int i = 1; i < pathIdx; i++ ) {
            startIdx += rom.getPathComponent( i ).length() + 1;
        }
        endIdx	= Math.min( fullCmd.length(),
            startIdx + (pathIdx < rom.getPathCount() ? rom.getPathComponent( pathIdx ).length() + 1 : 0 ));

        System.err.println( "FAILURE " +fullCmd.substring( 0, startIdx ) + "!" +
                            fullCmd.substring( startIdx, endIdx ) + "!" +
                            fullCmd.substring( endIdx ) + " Path not found" );
    }

    public static void failedArgCount( RoutedOSCMessage rom )
    {
        failed( rom.msg, "Illegal argument count (" + rom.msg.getArgCount() + ")" );
    }

    public static void failedArgType( RoutedOSCMessage rom, int argIdx )
    {
        failed( rom.msg, "Illegal argument type (" + rom.msg.getArg( argIdx ) + ")" );
    }

    public static void failedQuery( RoutedOSCMessage rom, String property )
    {
        failed( rom.msg, "Illegal query property (" + property + ")" );
    }

    public static void failedGet( RoutedOSCMessage rom, String param )
    {
        failed( rom.msg, "Illegal get command (" + param + ")" );
    }

    public static void failedArgValue( RoutedOSCMessage rom, int argIdx )
    {
        failed( rom.msg, "Illegal argument value (" + rom.msg.getArg( argIdx ) + ")" );
    }

    public static void failed( RoutedOSCMessage rom, Throwable t )
    {
        failed( rom.msg, t.getClass().getName() + " : " + t.getLocalizedMessage() );
    }

    public static void failed( OSCMessage msg, String why )
    {
        System.err.println( "FAILURE " + msg.getName() + " " + why );
    }

    // ------------ Runnable interface ------------

    // called from the event thread
    // when new messages have been queued
    public void run()
    {
        while( !collMessages.isEmpty() ) {
            osc.oscRoute( (RoutedOSCMessage) collMessages.remove( 0 ));
        }
    }

    // ------------ OSCRouter interface ------------

    public String oscGetPathComponent()
    {
        return null;
    }

    public void oscRoute( RoutedOSCMessage rom )
    {
        osc.oscRoute( rom );
    }

    public void oscAddRouter( OSCRouter subRouter )
    {
        osc.oscAddRouter( subRouter );
    }

    public void oscRemoveRouter( OSCRouter subRouter )
    {
        osc.oscRemoveRouter( subRouter );
    }

    /*
     *	Command: /dumpOSC, int <incomingMode> [, int <outgoingMode> ]
     */
    protected void oscCmdDump( RoutedOSCMessage rom )
    {
        final int	numArgs	= rom.msg.getArgCount();
        int			argIdx	= 0;

        try {
//			rcv.dumpOSC( ((Number) rom.msg.getArg( argIdx )).intValue(), System.out );
            serv.dumpIncomingOSC( ((Number) rom.msg.getArg( argIdx )).intValue(), System.out );
            argIdx++;
            if( numArgs == 2 ) {
//				trns.dumpOSC( ((Number) rom.msg.getArg( argIdx )).intValue(), System.out );
                serv.dumpOutgoingOSC( ((Number) rom.msg.getArg( argIdx )).intValue(), System.out );
            }
        }
        catch( ClassCastException e1 ) {
            failedArgType( rom, argIdx );
        }
        catch( IndexOutOfBoundsException e1 ) {
            failedArgCount( rom );
        }
    }

//	public void routeMessage( RoutedOSCMessage rom )
//	{
//		final String cmd = rom.getPathComponent();
//	
//		try {
//			if( cmd.equals( OSC_SYNC )) {
//				send( new OSCMessage( "/synced", new Object[] { rom.msg.getArg( 0 )}), rom.addr );
//			} else if( cmd.equals( OSC_DUMP )) {
//				cmdDumpOSC( rom );
//			} else {
//				failedUnknownCmd( rom );
//			}
//		}
//		catch( IndexOutOfBoundsException e1 ) {
//			failedArgCount( rom );
//		}
//		catch( IOException e1 ) {
//			failed( rom, e1 );
//		}
//	}

    // ------------ OSCListener interface ------------

    public void messageReceived( OSCMessage msg, SocketAddress addr, long when )
    {
        final String[]			path	= oscPathPtrn.split( msg.getName() );
//		final java.util.List	r;

        if( path.length < 2 ) {
            failedUnknownPath( msg );
            return;
        }

//		synchronized( mapRouters ) {
//			r = (java.util.List) mapRouters.get( path[ 1 ]);
//		}

//		if( r != null ) {
            collMessages.add( new RoutedOSCMessage( msg, addr, when, this, path, 0 ));
            EventQueue.invokeLater( this );
//		} else {
//			failedUnknownCmd( msg );
//		}
    }

// ------- PreferenceChangeListener interface -------

    public void preferenceChange(PreferenceChangeEvent e) {
        final String key = e.getKey();

        if (key.equals(KEY_ACTIVE)) {
            if (Boolean.valueOf(e.getNewValue()).booleanValue()) {
                if (!isRunning()) {
                    boot();
                }
            } else {
                if (isRunning()) {
                    quit();
                }
            }
        }
    }

/*
    private static void printError( String name, Throwable t )
    {
        System.err.print( name + " : " );
        t.printStackTrace( System.err );
    }
*/
    // ------------- internal classes -------------

//	private class SyncResponder
//	implements OSCListener
//	{
//		private OSCMessage				doneMsg	= null;
//		private final OSCResponderNode	resp1;
//		private final OSCResponderNode	resp2;
//		private final int				argIdx;
//		private final Object			argMatch;
//		
//		private SyncResponder( InetSocketAddress addr, String doneCmdName, String failCmdName, int argIdx, Object argMatch )
//		throws IOException
//		{
//			this.argIdx		= argIdx;
//			this.argMatch	= argMatch;
//			resp1			= new OSCResponderNode( addr, doneCmdName, this );
//			resp2			= failCmdName == null ? null : new OSCResponderNode( addr, failCmdName, this );
//		}
//		
//		private void add()
//		throws IOException
//		{
//			resp1.add();
//			if( resp2 != null ) resp2.add();
//		}
//
//		private void remove()
//		{
//			try {
//				resp1.remove();
//			}
//			catch( IOException e1 ) {
//				printError( "SyncResponder.remove", e1 );
//			}
//			try {
//				if( resp2 != null ) resp2.remove();
//			}
//			catch( IOException e1 ) {
//				printError( "SyncResponder.remove", e1 );
//			}
//		}
//		
//		public void messageReceived( OSCMessage msg, SocketAddress sender, long time )
//		{
//			if( (msg.getArgCount() > argIdx) && (msg.getArg( argIdx ).equals( argMatch ))) {
//				doneMsg	= msg;
//				remove();
//				synchronized( this ) {
//					this.notifyAll();
//				}
//			}
//		}
//	}
}