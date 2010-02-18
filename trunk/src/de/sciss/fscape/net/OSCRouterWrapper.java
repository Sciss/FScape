/*
 *  OSCRouterWrapper.java
 *  FScape
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
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
 *		05-May-06	created
 *		03-Oct-06	copied from eisenkraut
 */
 
package de.sciss.fscape.net;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 05-May-06
 */
public class OSCRouterWrapper
implements OSCRouter
{
	private final OSCRouter	r;
	private final OSCRouter	superRouter;
	private final Map		mapSubRouters	= new HashMap();
	
	private static final Class[] oscCmdMethodTypes		= { RoutedOSCMessage.class };
	private static final Class[] oscGetMethodTypes		= { RoutedOSCMessage.class };
	
	/**
	 *	@synchronization	call only in event thread
	 */
	public OSCRouterWrapper( OSCRouter superRouter, OSCRouter thisRouter )
	{
		r = thisRouter;
		this.superRouter	= superRouter;
		if( superRouter != null ) superRouter.oscAddRouter( r );
	}
	
	public void remove()
	{
		if( superRouter != null ) superRouter.oscRemoveRouter( r );
	}
	
	public void oscRoute( RoutedOSCMessage rom )
	{
		final int argCnt = rom.msg.getArgCount();
	
		if( rom.hasNext() ) {
			rom = rom.next();
			final OSCRouter nextR = (OSCRouter) mapSubRouters.get( rom.getPathComponent() );
			if( nextR != null ) {
				nextR.oscRoute( rom );
			} else {
				OSCRoot.failedUnknownPath( rom );
			}
		} else {
			final Object[]	replyArgs;
			final String	cmd;
			Method			oscMethod;

			if( argCnt == 0 ) {
				OSCRoot.failedArgCount( rom );
				return;
			}
			try {
				cmd = rom.msg.getArg( 0 ).toString();
				if( cmd.equals( OSCRoot.OSC_QUERY )) {
					if( argCnt < 2 ) {
						OSCRoot.failedArgCount( rom );
						return;
					}
					replyArgs = new Object[ argCnt - 1 ];
					replyArgs[ 0 ] = rom.msg.getArg( 1 );
					int argIdx = 2;
					try {
						for( ; argIdx < argCnt; argIdx++ ) {
							oscMethod = r.getClass().getMethod( "oscQuery_" + rom.msg.getArg( argIdx ).toString(), null );
							replyArgs[ argIdx - 1 ] = oscMethod.invoke( r, null );
						}
					}
					catch( NoSuchMethodException e1 ) {
						OSCRoot.failedArgValue( rom, argIdx );
						return;
					}
					rom.reply( OSCRoot.OSC_QUERYREPLY, replyArgs );
					
				} else if( cmd.equals( OSCRoot.OSC_GET )) {
				
					if( argCnt < 3 ) {
						OSCRoot.failedArgCount( rom );
						return;
					}
//					final Object[] getArgs = argCnt > 3 ? new Object[ argCnt - 3 ] : null;
					final Object[] methodResult;
//					for( int i = 3, j = 0; i < argCnt; i++ ) {
//						getArgs[ j ] = rom.msg.getArg( i );
//					}
					try {
						oscMethod = r.getClass().getMethod( "oscGet_" + rom.msg.getArg( 2 ), oscGetMethodTypes );
						methodResult = (Object[]) oscMethod.invoke( r, new Object[] { rom });
						if( methodResult != null ) {
							replyArgs = new Object[ methodResult.length + 1 ];
							replyArgs[ 0 ] = rom.msg.getArg( 1 );
							System.arraycopy( methodResult, 0, replyArgs, 1, methodResult.length );
							rom.reply( OSCRoot.OSC_GETREPLY, replyArgs );
						}
					}
					catch( NoSuchMethodException e1 ) {
						OSCRoot.failedArgValue( rom, 2 );
					}
					
				} else {	// any other command
					try {
						oscMethod = r.getClass().getMethod( "oscCmd_" + cmd, oscCmdMethodTypes );
						oscMethod.invoke( r, new Object[] { rom });
					}
					catch( NoSuchMethodException e1 ) {
						OSCRoot.failedUnknownCmd( rom );
					}
				}
			}
			catch( SecurityException e1 ) {
				OSCRoot.failed( rom, e1 );
			}
			catch( IllegalAccessException e1 ) {
				OSCRoot.failed( rom, e1 );
			}
			catch( IllegalArgumentException e1 ) {
				OSCRoot.failed( rom, e1 );
			}
			catch( InvocationTargetException e1 ) {
				OSCRoot.failed( rom, e1 );
			}
			catch( ExceptionInInitializerError e1 ) {
				OSCRoot.failed( rom, e1 );
			}
			catch( IOException e1 ) {
				OSCRoot.failed( rom, e1 );
			}
		}
	}
	
	public void oscAddRouter( OSCRouter subRouter )
	{
		if( mapSubRouters.put( subRouter.oscGetPathComponent(), subRouter ) != null ) {
			throw new IllegalArgumentException( "Tried to overwrite existing router for sub path " + subRouter.oscGetPathComponent() );
		}
	}
	
	public void oscRemoveRouter( OSCRouter subRouter )
	{
		if( mapSubRouters.remove( subRouter.oscGetPathComponent() ) == null ) {
			throw new IllegalArgumentException( "Tried to remove unknown router for sub path " + subRouter.oscGetPathComponent() );
		}
	}
	
	public String oscGetPathComponent()
	{
		return r.oscGetPathComponent();
	}
}