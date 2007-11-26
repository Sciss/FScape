/*
 *  PrefsUtil.java
 *  FScape
 *
 *  Copyright (c) 2004-2006 Hanns Holger Rutz. All rights reserved.
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
 *      25-Jan-05	created from de.sciss.meloncillo.util.PrefsUtil
 *		13-May-05	updated from Meloncillo
 *		22-Jul-05	suggests 57109 as default scsynth port ; no longer
 *					uses 'Built-in Audio' device on Mac (sc will use
 *					default system output instead) ; TEMPDIR refers to IOUtil
 *		24-Jun-06	created from de.sciss.eisenkraut.util.PrefsUtil and de.sciss.fscape.prop.MainPrefs
 */
 
package de.sciss.fscape.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import org.w3c.dom.*;

import de.sciss.app.*;
import de.sciss.io.*;

/**
 *	A helper class for programme preferences. It
 *	contains the globally known preferences keys,
 *	adds support for default preferences generation
 *	and some geometric object (de)serialization.
 *	Has utility methods for clearing preferences trees
 *	and importing and exporting from/to XML.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class PrefsUtil
{
	// ------------ root node level ------------

	/**
	 *  Value: Double representing the application
	 *  version of Meloncillo last time prefs was saved.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_VERSION	= "version";	// double : app version

//	/**
//	 *  Value: String representing the pathname
//	 *  of the temporary files directory.<br>
//	 *  Has default value: yes!<br>
//	 *  Node: root
//	 */
//	public static final String KEY_TEMPDIR	= "tmpdir";		// string : pathname
	/**
	 *  Value: String representing the path list of
	 *  a user's set favourite directories. See PathList
	 *  and PathField documentation.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_USERPATHS= "usrpaths";	// string : path list
	/**
	 *  Value: String representing a list of paths
	 *  of the recently used session files. See
	 *  PathList and MenuFactory.actionOpenRecentClass.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	public static final String KEY_OPENRECENT= "recent";	// string: path list
	/**
	 *  Value: Boolean stating whether frame bounds
	 *  should be recalled a session file when it's
	 *  loaded. Has default value: yes!<br>
	 *  Node: root
	 */
	public static final String KEY_LOOKANDFEEL = "lookandfeel";	
	/**
	 *  Value: Boolean stating whether frame size (grow) boxes
     *  intrude into the frame's pane. Has default value: yes!<br>
	 *  Node: root
	 */
	public static final String KEY_INTRUDINGSIZE = "intrudingsize";
//	/**
//	 *  Value: String "(int) modifiers (int) keyCode" for online
//     *  help accelerator. Has default value: yes!<br>
//	 *  Node: root
//	 */
//	public static final String KEY_KEYSTROKE_HELP = "keystrokehelp";

	/**
	 *  Value: String representing a Point object
	 *  describing a windows location. Use stringToPoint.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	public static final String KEY_LOCATION		= "location";   // point
	/**
	 *  Value: String representing a Dimension object
	 *  describing a windows size. Use stringToDimension.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	public static final String KEY_SIZE			= "size";		// dimension
	/**
	 *  Value: Boolean stating wheter a window is
	 *  shown or hidden.<br>
	 *  Has default value: no!<br>
	 *  Node: multiple occurences in shared -> (Frame-class)
	 */
	public static final String KEY_VISIBLE		= "visible";	// boolean

	public static final String KEY_BACKUP			= "makebackups";
	
	public static final String KEY_BAKDIR			= "backupdir";

	public static java.util.List createDefaults( Preferences mainPrefs, double lastVersion )
	{
		File					f;
//		String					value;
//		Preferences				childPrefs, childPrefs2;
//		final String			fs			= File.separator;
		final boolean			isMacOS		= System.getProperty( "os.name" ).indexOf( "Mac OS" ) >= 0;
//        final boolean			isWindows	= System.getProperty( "os.name" ).indexOf( "Windows" ) >= 0;
		final java.util.List	warnings	= new ArrayList();
	
		putDontOverwrite( IOUtil.getUserPrefs(), IOUtil.KEY_TEMPDIR, System.getProperty( "java.io.tmpdir" ));

//		// general
//		putDontOverwrite( GUIUtil.getUserPrefs(), HelpGlassPane.KEY_KEYSTROKE_HELP, strokeToPrefs(
//			KeyStroke.getKeyStroke( KeyEvent.VK_H, MenuFactory.MENU_SHORTCUT + KeyEvent.SHIFT_MASK )));

		putDontOverwrite( mainPrefs, KEY_LOOKANDFEEL, UIManager.getSystemLookAndFeelClassName() );
		putBooleanDontOverwrite( mainPrefs, KEY_INTRUDINGSIZE, isMacOS );

		putBooleanDontOverwrite( mainPrefs, KEY_BACKUP, true );

		if( mainPrefs.get( KEY_BAKDIR, null ) == null ) {
			f = new File( new File( System.getProperty( "user.home" ), AbstractApplication.getApplication().getName() ), "bak" );
			if( !f.isDirectory() ) {
				try {
					IOUtil.createEmptyDirectory( f );
				}
				catch( IOException e1 ) {
					warnings.add( f.getAbsolutePath() + " : " + AbstractApplication.getApplication().getResourceString( "errMakeDir" ));
				}
			}
			putDontOverwrite( mainPrefs, KEY_BAKDIR, f.getAbsolutePath() );
		}

		// save current version
		mainPrefs.putDouble( KEY_VERSION, AbstractApplication.getApplication().getVersion() );

		return warnings;
	}

/*
	private static File findFile( String fileName, String[] folders )
	{
		File f;
	
		for( int i = 0; i < folders.length; i++ ) {
			f = new File( folders[ i ], fileName );
			if( f.exists() ) return f;
		}
		return null;
	}
*/
	// --- custom put/get methods ---

	private static boolean putDontOverwrite( Preferences prefs, String key, String value )
	{
		boolean overwrite = prefs.get( key, null ) == null;
		
		if( overwrite ) {
			prefs.put( key, value );
		}
		
		return overwrite;
	}
/*
	private static boolean putIntDontOverwrite( Preferences prefs, String key, int value )
	{
		boolean overwrite = prefs.get( key, null ) == null;
		
		if( overwrite ) {
			prefs.putInt( key, value );
		}
		
		return overwrite;
	}
*/
	private static boolean putBooleanDontOverwrite( Preferences prefs, String key, boolean value )
	{
		boolean overwrite = prefs.get( key, null ) == null;
		
		if( overwrite ) {
			prefs.putBoolean( key, value );
		}
		
		return overwrite;
	}

/*
	private static boolean putDoubleDontOverwrite( Preferences prefs, String key, double value )
	{
		boolean overwrite = prefs.get( key, null ) == null;
		
		if( overwrite ) {
			prefs.putDouble( key, value );
		}
		
		return overwrite;
	}
*/
	public static Rectangle stringToRectangle( String value )
	{
		Rectangle		rect	= null;
		StringTokenizer tok;
		
		if( value != null ) {
			try {
				tok		= new StringTokenizer( value );
				rect	= new Rectangle( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ),
										 Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ));
			}
			catch( NoSuchElementException e1 ) {}
			catch( NumberFormatException e2 ) {}
		}
		return rect;
	}

	public static Point stringToPoint( String value )
	{
		Point			pt	= null;
		StringTokenizer tok;
		
		if( value != null ) {
			try {
				tok		= new StringTokenizer( value );
				pt		= new Point( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ));
			}
			catch( NoSuchElementException e1 ) {}
			catch( NumberFormatException e2 ) {}
		}
		return pt;
	}

	public static Dimension stringToDimension( String value )
	{
		Dimension		dim	= null;
		StringTokenizer tok;
		
		if( value != null ) {
			try {
				tok		= new StringTokenizer( value );
				dim		= new Dimension( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ));
			}
			catch( NoSuchElementException e1 ) {}
			catch( NumberFormatException e2 ) {}
		}
		return dim;
	}
	
	/**
	 *  Rectangle, z.B. von Frame.getBounds() in
	 *  einen String konvertieren, der als Prefs
	 *  gespeichert werden kann. Bei Fehler wird
	 *  null zurueckgeliefert. 'value' darf null sein.
	 */
	public static String rectangleToString( Rectangle value )
	{
		return( value != null ? (value.x + " " + value.y + " " + value.width + " " + value.height) : null );
	}
	
	public static String pointToString( Point value )
	{
		return( value != null ? (value.x + " " + value.y) : null );
	}
	
	public static String dimensionToString( Dimension value )
	{
		return( value != null ? (value.width + " " + value.height) : null );
	}
	
//	public static String classToNodeName( Class c )
//	{
//		return( "/" + c.getName().replace( '.', '/' ));
//	}

	/**
	 *  Converts a a key stroke's string representation as
	 *	from preference storage into a KeyStroke object.
	 *
	 *  @param		prefsValue		a string representation of the form &quot;modifiers keyCode&quot;
	 *								or <code>null</code>
	 *	@return		the KeyStroke parsed from the prefsValue or null if the string was
	 *				invalid or <code>null</code>
	 */
	public static final KeyStroke prefsToStroke( String prefsValue )
	{
		if( prefsValue == null ) return null;
		int i = prefsValue.indexOf( ' ' );
		KeyStroke prefsStroke = null;
		try {
			if( i < 0 ) return null;
			prefsStroke = KeyStroke.getKeyStroke( Integer.parseInt( prefsValue.substring( i+1 )),
												  Integer.parseInt( prefsValue.substring( 0, i )));
		}
		catch( NumberFormatException e1 ) {}

		return prefsStroke;
	}
	
	/**
	 *  Converts a KeyStroke into a string representation for
	 *	preference storage.
	 *
	 *  @param		prefsStroke	the KeyStroke to convert
	 *	@return		a string representation of the form &quot;modifiers keyCode&quot;
	 *				or <code>null</code> if the prefsStroke is invalid or <code>null</code>
	 */
	public static final String strokeToPrefs( KeyStroke prefsStroke )
	{
		if( prefsStroke == null ) return null;
		else return String.valueOf( prefsStroke.getModifiers() ) + ' ' +
					String.valueOf( prefsStroke.getKeyCode() );
	}

	/**
	 *  Traverse a preference node and optionally all
	 *  children nodes and remove any keys found.
	 */
	public static void removeAll( Preferences prefs, boolean deep )
	throws BackingStoreException
	{
		String[]	keys;
		String[]	children;
		int			i;

		keys = prefs.keys();
		for( i = 0; i < keys.length; i++ ) {
			prefs.remove( keys[i] );
		}
		if( deep ) {
			children	= prefs.childrenNames();
			for( i = 0; i < children.length; i++ ) {
				removeAll( prefs.node( children[i] ), deep );
			}
		}
	}

	/**
	 *  Get an Action object that will dump the
	 *  structure of the MultiTrackEditors of
	 *  all selected transmitters
	 */
	public static Action getDebugDumpAction()
	{
		AbstractAction a = new AbstractAction( "Dump preferences tree" ) {
			public void actionPerformed( ActionEvent e )
			{
				debugDump( AbstractApplication.getApplication().getUserPrefs() );
			}
			
			private void debugDump( Preferences prefs )
			{
				System.err.println( "------- debugDump prefs : "+prefs.name()+" -------" );
				String[]	keys;
				String[]	children;
				String		value;
				int			i;
				
				try {
					keys		= prefs.keys();
					for( i = 0; i < keys.length; i++ ) {
						value   = prefs.get( keys[i], null );
						System.err.println( "  key = '"+keys[i]+"' ; value = '"+value+"'" );
					}
					children	= prefs.childrenNames();
					for( i = 0; i < children.length; i++ ) {
						debugDump( prefs.node( children[i] ));
					}
				} catch( BackingStoreException e1 ) {
					System.err.println( e1.getLocalizedMessage() );
				}
			}
		};
		return a;
	}

	/**
	 *  Similar to the XMLRepresentation interface,
	 *  this method will append an XML representation
	 *  of some preferences to an existing node.
	 *
	 *  @param  prefs   the preferences node to write out.
	 *  @param  deep	- true to include a subtree with all
	 *					child preferences nodes.
	 *  @param  domDoc  the document in which the node will reside.
	 *  @param  node	the node to which a child is applied.
	 */
	public static void toXML( Preferences prefs, boolean deep, org.w3c.dom.Document domDoc,
							  Element node, Map options )
	throws IOException
	{
		String[]	keys;
		String[]	children;
		Element		childElement, entry;
		String		value;
		int			i;

//System.err.println( "node = "+prefs.name() );
		try {
			keys			= prefs.keys();
			childElement	= (Element) node.appendChild( domDoc.createElement( "map" ));
			for( i = 0; i < keys.length; i++ ) {
				value   = prefs.get( keys[i], null );
//System.err.println( "  key = "+keys[i]+"; value = "+value );
				if( value == null ) continue;
				entry = (Element) childElement.appendChild( domDoc.createElement( "entry" ));
				entry.setAttribute( "key", keys[i] );
				entry.setAttribute( "value", value );
			}
			if( deep ) {
				children	= prefs.childrenNames();
				for( i = 0; i < children.length; i++ ) {
					childElement = (Element) node.appendChild( domDoc.createElement( "node" ));
					childElement.setAttribute( "name", children[i] );
					toXML( prefs.node( children[i] ), deep, domDoc, childElement, options );
				}
			}
		} catch( DOMException e1 ) {
			throw IOUtil.map( e1 );
		} catch( BackingStoreException e2 ) {
			throw IOUtil.map( e2 );
		}
	}

	/**
	 *  Similar to the XMLRepresentation interface,
	 *  this method will parse an XML representation
	 *  of some preferences and restore it's values.
	 *
	 *  @param  prefs		the preferences node to import to.
	 *  @param  domDoc		the document in which the node resides.
	 *  @param  rootElement	the node whose children to parse.
	 */
	public static void fromXML( Preferences prefs, org.w3c.dom.Document domDoc,
								Element rootElement, Map options )
	throws IOException
	{
		NodeList	nl, nl2;
		Element		childElement, entry;
		Node		node;
		int			i, j;

		try {
			nl	= rootElement.getChildNodes();
			for( i = 0; i < nl.getLength(); i++ ) {
				node			= nl.item( i );
				if( !(node instanceof Element) ) continue;
				childElement	= (Element) node;
				nl2				= childElement.getElementsByTagName( "entry" );
				for( j = 0; j < nl2.getLength(); j++ ) {
					entry		= (Element) nl2.item( j );
					prefs.put( entry.getAttribute( "key" ), entry.getAttribute( "value" ));
//System.err.println( "auto : node = "+(prefs.name() )+"; key = "+entry.getAttribute( "key" )+"; val = "+entry.getAttribute( "value" ) );

				}
				break;
			}
			for( ; i < nl.getLength(); i++ ) {
				node			= nl.item( i );
				if( !(node instanceof Element) ) continue;
				childElement	= (Element) node;
				fromXML( prefs.node( childElement.getAttribute( "name" )), domDoc, childElement, options );
			}
		} catch( DOMException e1 ) {
			throw IOUtil.map( e1 );
		}
	}
}