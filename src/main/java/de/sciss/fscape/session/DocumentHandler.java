///*
// *  DocumentHandler.java
// *  FScape
// *
// *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
// *
// *	This software is free software; you can redistribute it and/or
// *	modify it under the terms of the GNU General Public License
// *	as published by the Free Software Foundation; either
// *	version 2, june 1991 of the License, or (at your option) any later version.
// *
// *	This software is distributed in the hope that it will be useful,
// *	but WITHOUT ANY WARRANTY; without even the implied warranty of
// *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// *	General Public License for more details.
// *
// *	You should have received a copy of the GNU General Public
// *	License (gpl.txt) along with this software; if not, write to the Free Software
// *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// *
// *
// *	For further information, please contact Hanns Holger Rutz at
// *	contact@sciss.de
// *
// *
// *  Changelog:
// *      21-May-05	created
// *		20-Jan-06	implements OSCRouter
// *		03-Oct-06	copied from eisenkraut
// */
//
//package de.sciss.fscape.session;
//
//import java.io.*;
//import java.util.*;
//
//// FFF
////import de.sciss.eisenkraut.*;
////import de.sciss.eisenkraut.net.*;
//import de.sciss.fscape.*;
//import de.sciss.fscape.net.*;
//
//
///**
// *  @author		Hanns Holger Rutz
// *  @version	0.71, 14-Nov-07
// */
//public class DocumentHandler
//extends AbstractDocumentHandler
//implements OSCRouter
//{
//	private static final String OSC_DOC			= "doc";
//	// sub level
//	private static final String OSC_ACTIVE		= "active";
//	private static final String OSC_INDEX		= "index";
//	private static final String OSC_ID			= "id";
//
//	// key = Integer( nodeID ) ; value = Session
//	private final Map	mapIDs		= new HashMap();
//
//	private final OSCRouterWrapper	osc;
//	private final Main				root;
//
//	public DocumentHandler( Main root )
//	{
//		super( true );	// we are multi-document aware
//		this.root	= root;
//		osc			= new OSCRouterWrapper( OSCRoot.getInstance(), this );
//	}
//
//	public void addDocument( Object source, Document doc )
//	{
//		synchronized( sync ) {
//			super.addDocument( source, doc );
//			mapIDs.put( new Integer( ((Session) doc).getNodeID() ), doc );
//		}
//	}
//
//	public void removeDocument( Object source, Document doc )
//	{
//		synchronized( sync ) {
//			mapIDs.remove( new Integer( ((Session) doc).getNodeID() ));
//			super.removeDocument( source, doc );
//		}
//	}
//
//	// ------------- OSCRouter interface -------------
//
//	public String oscGetPathComponent()
//	{
//		return OSC_DOC;
//	}
//
//	public void oscRoute( RoutedOSCMessage rom )
//	{
//		if( rom.hasNext() ) {	// special handling here as documents can be accessed with different paths
//			oscRouteNext( rom.next() );
//		} else {
//			osc.oscRoute( rom );
//		}
//	}
//
//	private void oscRouteNext( RoutedOSCMessage rom )
//	{
//		final String	subPath;
//		final Document	doc;
//
//		try {
//			subPath = rom.getPathComponent();
//
//			if( subPath.equals( OSC_ACTIVE )) {
//				doc = getActiveDocument();
//			} else if( subPath.equals( OSC_ID )) {
//				rom = rom.next();
//				final Integer id = new Integer( rom.getPathComponent() );
//				synchronized( sync ) {
//					doc = (Document) mapIDs.get( id );
//				}
//			} else if( subPath.equals( OSC_INDEX )) {
//				rom = rom.next();
//				final int idx = Integer.parseInt( rom.getPathComponent() );
//				if( getDocumentCount() > idx ) {
//					doc = getDocument( idx );
//				} else {
//					doc = null;
//				}
//			} else {
//				OSCRoot.failedUnknownPath( rom );
//				return;
//			}
//
//			if( doc == null ) {
//				OSCRoot.failed( rom.msg, "Document not found" );
//				return;
//			}
//
//			if( !(doc instanceof OSCRouter) ) {
//				OSCRoot.failed( rom.msg, "Document doesn't speak OSC" );
//				return;
//			}
//
//			((OSCRouter) doc).oscRoute( rom );
//		}
//		catch( IndexOutOfBoundsException e1 ) {
//			OSCRoot.failedUnknownPath( rom );
//		}
//		catch( NumberFormatException e1 ) {
//			OSCRoot.failedUnknownPath( rom );
//		}
//	}
//
//	public void oscAddRouter( OSCRouter subRouter )
//	{
//		osc.oscAddRouter( subRouter );
//	}
//
//	public void oscRemoveRouter( OSCRouter subRouter )
//	{
//		osc.oscRemoveRouter( subRouter );
//	}
//
//	public Object oscQuery_count()
//	{
//		return new Integer( getDocumentCount() );
//	}
//
//// FFFF
////	public void oscCmd_open( RoutedOSCMessage rom )
////	{
////		try {
////			final String path = rom.msg.getArg( 1 ).toString();
////			root.getMenuFactory().openDocument( new File( path ));
////		}
////		catch( IndexOutOfBoundsException e1 ) {
////			OSCRoot.failedArgCount( rom );
////			return;
////		}
//////		catch( ClassCastException e1 ) {
//////			OSCRoot.failedArgType( rom, 1 );
//////		}
////	}
//	public void oscCmd_open( final RoutedOSCMessage rom )
//	{
//		final boolean 	visible;
//		final int		numCopyArgs	= 2;  // 'open', path
//		int argIdx		= 1;
//
//		try {
//			final String path = rom.msg.getArg( argIdx ).toString();
//			argIdx++;
//			if( rom.msg.getArgCount() > argIdx ) {
//				visible = ((Number) rom.msg.getArg( argIdx )).intValue() != 0;
//			} else {
//				visible = true;
//			}
//			final OpenDoneHandler odh = new OpenDoneHandler() {
//				public void openSucceeded( Session doc ) {
//					rom.tryReplyDone( numCopyArgs, new Object[] { new Integer( doc.getNodeID() )});
//				}
//
//				public void openFailed() {
//					rom.tryReplyFailed( numCopyArgs );
//				}
//			};
//			((de.sciss.fscape.gui.MenuFactory) root.getMenuFactory()).openDocument( new File( path ), visible, odh );
//		}
//		catch( IndexOutOfBoundsException e1 ) {
//			OSCRoot.failedArgCount( rom );
//			rom.tryReplyFailed( numCopyArgs );
//			return;
//		}
//		catch( ClassCastException e1 ) {
//			OSCRoot.failedArgType( rom, argIdx );
//			rom.tryReplyFailed( numCopyArgs );
//		}
//	}
//
//	public void oscCmd_new( RoutedOSCMessage rom )
//	{
//		try {
//			final String procName = rom.msg.getArg( 1 ).toString();
////			root.menuFactory.newDocument( procName );
//			((de.sciss.fscape.gui.MenuFactory) root.getMenuFactory()).newDocument( procName );
//		}
//		catch( IndexOutOfBoundsException e1 ) {
//			OSCRoot.failedArgCount( rom );
//			return;
//		}
////		catch( ClassCastException e1 ) {
////			OSCServer.failedArgType( rom, 1 );
////		}
//	}
//
//	public static interface OpenDoneHandler {
//		public void openSucceeded( Session doc );
//		public void openFailed();
//	}
//}