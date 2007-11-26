/*
 *  MenuFactory.java
 *  (de.sciss.common package)
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.MenuFactory
 *		02-Aug-05	confirms to new document handler
 *		15-Sep-05	openDocument checks if file is already open
 */

package de.sciss.common;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import net.roydesign.app.AboutJMenuItem;
import net.roydesign.app.PreferencesJMenuItem;
import net.roydesign.app.QuitJMenuItem;
import net.roydesign.mac.MRJAdapter;

import de.sciss.app.AbstractWindow;
import de.sciss.app.DocumentEvent;
import de.sciss.app.DocumentHandler;
import de.sciss.app.DocumentListener;
import de.sciss.gui.AboutBox;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.HelpFrame;
import de.sciss.gui.MenuAction;
import de.sciss.gui.MenuGroup;
import de.sciss.gui.MenuItem;
import de.sciss.gui.MenuRadioGroup;
import de.sciss.gui.MenuRadioItem;
import de.sciss.gui.MenuRoot;
import de.sciss.gui.MenuSeparator;
import de.sciss.gui.PathList;
import de.sciss.io.IOUtil;
import de.sciss.util.Flag;

/**
 *  <code>JMenu</code>s cannot be added to more than
 *  one frame. Since on MacOS there's one
 *  global menu for all the application windows
 *  we need to 'duplicate' a menu prototype.
 *  Synchronizing all menus is accomplished
 *  by using the same action objects for all
 *  menu copies. However when items are added
 *  or removed, synchronization needs to be
 *  performed manually. That's the point about
 *  this class.
 *  <p>
 *  There can be only one instance of <code>MenuFactory</code>
 *  for the application, and that will be created by the
 *  <code>Main</code> class.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 02-Sep-06
 *
 *  @see	de.sciss.eisenkraut.Main#menuFactory
 */
public abstract class BasicMenuFactory
extends MenuRoot
implements DocumentListener
{
	/*
	 *  Value: String representing a list of paths
	 *  of the recently used session files. See
	 *  PathList and MenuFactory.actionOpenRecentClass.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	private static final String KEY_OPENRECENT= "recent";	// string: path list

	/**
	 *	<code>KeyStroke</code> modifier mask
	 *	representing the platform's default
	 *	menu accelerator (e.g. Apple-key on Mac,
	 *	Ctrl on Windows).
	 *
	 *	@see	Toolkit#getMenuShortcutKeyMask()
	 */
	public static final int				MENU_SHORTCUT				= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private int							uniqueNumber		= 0;

	// ---- window menu management ----
	private MenuGroup					mgWindow;
	private MenuRadioGroup				mWindowRadioGroup;

	// ---- recent files management ----
	private MenuGroup					mgRecent;
	private final PathList				openRecentPaths;

	// ---- misc actions ----
	private actionOpenRecentClass		actionOpenRecent;
	private Action						actionClearRecent;
	private Action						actionCloseAll;

//	private final List					collGlobalKeyCmd	= new ArrayList();
	
//	private static final String			CLIENT_BG	= "de.sciss.gui.BG";	// radio button group

	protected final BasicApplication	root;

	/**
	 *  The constructor is called only once by
	 *  the <code>Main</code> class and will create a prototype
	 *  main menu from which all copies are
	 *  derived.
	 */
	public BasicMenuFactory( BasicApplication app )
	{
		super();
		
		this.root		= app;
	
		openRecentPaths = new PathList( 8, root.getUserPrefs(), KEY_OPENRECENT );
	}
	
	public void init()
	{
		createActions();
		createProtoType();

		// ---- listeners -----
		
		root.getDocumentHandler().addDocumentListener( this );
	}
	
	public ProcessingThread closeAll( boolean force, Flag confirmed )
	{
		final DocumentHandler	dh	= root.getDocumentHandler();
		BasicDocument			doc;
		ProcessingThread		pt;

		while( dh.getDocumentCount() > 0 ) {
			doc	= (BasicDocument) dh.getDocument( 0 );
//if( doc.getFrame() == null ) {
//	System.err.println( "Yukk, no doc frame for "+doc.getDisplayDescr().file );
//	try {
//		Thread.sleep( 4000 );
//	} catch( InterruptedException e1 ) {}
//	confirmed.set( true );
//	return null;
//}
			pt	= doc.closeDocument( force, confirmed );
			if( pt == null ) {
				if( !confirmed.isSet() ) return null;
			} else {
				return pt;
			}
		}
		confirmed.set( true );
		return null;
	}

	private void createActions()
	{
		// --- file menu ---
		actionOpenRecent = new actionOpenRecentClass( getResourceString( "menuOpenRecent" ));
		actionClearRecent = new actionClearRecentClass( getResourceString( "menuClearRecent" ), null );
		actionCloseAll	= new actionCloseAllClass( getResourceString( "menuCloseAll" ), null );
	}
	
	// @todo	this should eventually read the tree from an xml file
	private void createProtoType()
	{
		MenuGroup				mg; //, smg;
//		MenuCheckItem			mci;
//		MenuRadioGroup			rg;
		Action					a;
//		BooleanPrefsMenuAction	ba;
//		IntPrefsMenuAction		ia;
//		Preferences				prefs;
		
		// Ctrl on Mac / Ctrl+Alt on PC
//		final int myCtrl = MENU_SHORTCUT == KeyEvent.CTRL_MASK ? KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK : KeyEvent.CTRL_MASK;

		// --- file menu ---
		
		mg	= new MenuGroup( "file", getResourceString( "menuFile" ));
//		smg = new MenuGroup( "new", getResourceString( "menuNew" ));
//		smg.add( new MenuItem( "empty", actionNewEmpty ));
//		smg.add( new MenuItem( "fromSelection", getResourceString( "menuNewFromSelection" )));
//		mg.add( smg );
		mg.add( new MenuItem( "open", getOpenAction() ));
//		mg.add( new MenuItem( "openMultipleMono", actionOpenMM ));
		mgRecent = new MenuGroup( "openRecent", actionOpenRecent );
		if( openRecentPaths.getPathCount() > 0 ) {
			for( int i = 0; i < openRecentPaths.getPathCount(); i++ ) {
				mgRecent.add( new MenuItem( String.valueOf( uniqueNumber++ ), new actionOpenRecentClass( openRecentPaths.getPath( i ))));
			}
			actionOpenRecent.setPath( openRecentPaths.getPath( 0 ));
			actionOpenRecent.setEnabled( true );
			actionClearRecent.setEnabled( true );
		}
		mgRecent.addSeparator();
		mgRecent.add( new MenuItem( "clearRecent", actionClearRecent ));
		mg.add( mgRecent );
		mg.add( new MenuItem( "close", getResourceString( "menuClose" ), KeyStroke.getKeyStroke( KeyEvent.VK_W, MENU_SHORTCUT )));
		mg.add( new MenuItem( "closeAll", actionCloseAll ));
		mg.add( new MenuSeparator() );
//		smg	= new MenuGroup( "import", getResourceString( "menuImport" ));
//		smg.add( new MenuItem( "markers", getResourceString( "menuImportMarkers" )));
//		mg.add( smg );
//		mg.addSeparator();
		mg.add( new MenuItem( "save", getResourceString( "menuSave" ), KeyStroke.getKeyStroke( KeyEvent.VK_S, MENU_SHORTCUT )));
		mg.add( new MenuItem( "saveAs", getResourceString( "menuSaveAs" ), KeyStroke.getKeyStroke( KeyEvent.VK_S, MENU_SHORTCUT + KeyEvent.SHIFT_MASK )));
		mg.add( new MenuItem( "saveCopyAs", getResourceString( "menuSaveCopyAs" )));
//		mg.add( new MenuItem( "saveSelectionAs", getResourceString( "menuSaveSelectionAs" )));
		if( QuitJMenuItem.isAutomaticallyPresent() ) {
			root.getQuitJMenuItem().setAction( root.getQuitAction() );
		} else {
			mg.addSeparator();
			mg.add( new MenuItem( "quit", root.getQuitAction() ));
		}
		this.add( mg );

		// --- edit menu ---
		mg	= new MenuGroup( "edit", getResourceString( "menuEdit" ));
		mg.add( new MenuItem( "undo", getResourceString( "menuUndo" ), KeyStroke.getKeyStroke( KeyEvent.VK_Z, MENU_SHORTCUT )));
		mg.add( new MenuItem( "redo", getResourceString( "menuRedo" ), KeyStroke.getKeyStroke( KeyEvent.VK_Z, MENU_SHORTCUT + KeyEvent.SHIFT_MASK )));
		mg.addSeparator();
		mg.add( new MenuItem( "cut", getResourceString( "menuCut" ), KeyStroke.getKeyStroke( KeyEvent.VK_X, MENU_SHORTCUT )));
		mg.add( new MenuItem( "copy", getResourceString( "menuCopy" ), KeyStroke.getKeyStroke( KeyEvent.VK_C, MENU_SHORTCUT )));
		mg.add( new MenuItem( "paste", getResourceString( "menuPaste" ), KeyStroke.getKeyStroke( KeyEvent.VK_V, MENU_SHORTCUT )));
		mg.add( new MenuItem( "clear", getResourceString( "menuClear" ), KeyStroke.getKeyStroke( KeyEvent.VK_BACK_SPACE, 0 )));
		mg.addSeparator();
		mg.add( new MenuItem( "selectAll", getResourceString( "menuSelectAll" ), KeyStroke.getKeyStroke( KeyEvent.VK_A, MENU_SHORTCUT )));
		a	= new actionPreferencesClass( getResourceString( "menuPreferences" ), KeyStroke.getKeyStroke( KeyEvent.VK_COMMA, MENU_SHORTCUT ));
		if( PreferencesJMenuItem.isAutomaticallyPresent() ) {
			root.getPreferencesJMenuItem().setAction( a );
		} else {
			mg.addSeparator();
			mg.add( new MenuItem( "preferences", a ));
		}
		this.add( mg );
		
		// --- window menu ---
		mWindowRadioGroup = new MenuRadioGroup();
		mgWindow = new MenuGroup( "window", getResourceString( "menuWindow" ));
//		mgWindow.add( new MenuItem( "ioSetup", new actionIOSetupClass( getResourceString( "frameIOSetup" ), null )));
//		mgWindow.addSeparator();
//		mgWindow.add( new MenuItem( "main", new actionShowWindowClass( getResourceString( "frameMain" ), null, Main.COMP_MAIN )));
//		mgWindow.add( new MenuItem( "observer", new actionObserverClass( getResourceString( "paletteObserver" ), KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD3, MENU_SHORTCUT ))));
//		mgWindow.add( new MenuItem( "ctrlRoom", new actionCtrlRoomClass( getResourceString( "paletteCtrlRoom" ), KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD2, MENU_SHORTCUT ))));
		mgWindow.addSeparator();
		mgWindow.add( new MenuItem( "collect", ((BasicWindowHandler) root.getWindowHandler()).getCollectAction() ));
		mgWindow.addSeparator();
		this.add( mgWindow );

		// --- help menu ---
		mg	= new MenuGroup( "help", getResourceString( "menuHelp" ));
		// this is pretty weird, but it works at least on german keyboards: command+questionmark is defaut help shortcut
		// on mac os x. KeyEvent.VK_QUESTION_MARK doesn't exist, plus apple's vm ignore german keyboard layout, therefore the
		// the question mark becomes a minus. however it's wrongly displayed in the menu...
		mg.add( new MenuItem( "manual", new actionURLViewerClass( getResourceString( "menuHelpManual" ), KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, MENU_SHORTCUT + KeyEvent.SHIFT_MASK ), "index", false )));
		mg.add( new MenuItem( "shortcuts", new actionURLViewerClass( getResourceString( "menuHelpShortcuts" ), null, "Shortcuts", false )));
		mg.addSeparator();
		mg.add( new MenuItem( "website", new actionURLViewerClass( getResourceString( "menuHelpWebsite" ), null, getResourceString( "appURL" ), true )));
		a = new actionAboutClass( getResourceString( "menuAbout" ), null );
		if( AboutJMenuItem.isAutomaticallyPresent() ) {
			root.getAboutJMenuItem().setAction( a );
		} else {
			mg.addSeparator();
			mg.add( new MenuItem( "about", a ));
		}

		this.add( mg );
		
		addMenuItems();
	}
	
	protected abstract void addMenuItems();
	public abstract void showPreferences();
	protected abstract Action getOpenAction();

	public abstract void openDocument( File f );

	
	// adds a file to the top of
	// the open recent menu of all menubars
	// and the prototype. calls
	// openRecentPaths.addPathToHead() and
	// thus updates the preferences settings
	// iteratively calls addRecent( JMenuBar, File, boolean )
	public void addRecent( File path )
	{
		int	i;
		
		i = openRecentPaths.indexOf( path );
		if( i == 0 ) return;	// this path is already topmost item
		if( (i == -1) && (openRecentPaths.getCapacity() == openRecentPaths.getPathCount()) ) {	// not in list and list too big now
			i = openRecentPaths.getPathCount() - 1;	// remove last item
		}
		if( i > 0 ) {	// remove an item
			openRecentPaths.remove( path );
			mgRecent.remove( i );
		}
		// add new item to top
		openRecentPaths.addPathToHead( path );
		actionOpenRecent.setPath( path );
		actionClearRecent.setEnabled( true );
		mgRecent.add( new MenuItem( String.valueOf( uniqueNumber++ ), new actionOpenRecentClass( path )), 0 );
	}
	
	protected String getResourceString( String key )
	{
		return root.getResourceString( key );
	}

	public void addToWindowMenu( Action a )
	{
		final String id = "window" + String.valueOf( uniqueNumber++ );

//System.err.println( "add "+a.hashCode() );
		mgWindow.add( new MenuRadioItem( mWindowRadioGroup, id, a ));
	}
	
	public void removeFromWindowMenu( Action a )
	{
		final MenuRadioItem mri = (MenuRadioItem) mgWindow.getByAction( a );		
		mgWindow.remove( mri );
	}
	

	public void setSelectedWindow( Action a )
	{
//System.err.println( "select "+a.hashCode() );
		final MenuRadioItem mri = (MenuRadioItem) mgWindow.getByAction( a );
		mri.setSelected( true );
	}
	
// ---------------- DocumentListener interface ---------------- 
	
	public void documentAdded( DocumentEvent e )
	{
		if( !actionCloseAll.isEnabled() ) actionCloseAll.setEnabled( true );
	}
	
	public void documentRemoved( DocumentEvent e )
	{
		if( root.getDocumentHandler().getDocumentCount() == 0 ) {
			actionCloseAll.setEnabled( false );
		}
	}

	public void documentFocussed( de.sciss.app.DocumentEvent e ) {}

// ---------------- Action objects for file (session) operations ---------------- 

	// action for the Open-Recent menu
	private class actionOpenRecentClass
	extends MenuAction
	{
		private File path;

		// new action with path set to null
		private actionOpenRecentClass( String text )
		{
			super( text );
			setPath( null );
		}

		// new action with given path
		private actionOpenRecentClass( File path )
		{
//			super( IOUtil.abbreviate( path.getParent(), 40 ));
			super( IOUtil.abbreviate( path.getAbsolutePath(), 40 ));
			setPath( path );
		}
		
		// set the path of the action. this
		// is the file that will be loaded
		// if the action is performed
		private void setPath( File path )
		{
			this.path = path;
			setEnabled( (path != null) && path.isFile() );
		}
		
		/**
		 *  If a path was set for the
		 *  action and the user confirms
		 *  an intermitting confirm-unsaved-changes
		 *  dialog, the new session will be loaded
		 */
		public void actionPerformed( ActionEvent e )
		{
			if( path == null ) return;
//			actionOpen.perform( path );
			openDocument( path );
		}
	} // class actionOpenRecentClass

	// action for clearing the Open-Recent menu
	private class actionClearRecentClass
	extends MenuAction
	{
		private actionClearRecentClass( String text, KeyStroke shortcut )
		{
			super( text, shortcut );
			setEnabled( false );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			for( int i = openRecentPaths.getPathCount() - 1; i >= 0; i-- ) {
				mgRecent.remove( i );
			}
			openRecentPaths.clear();
			actionOpenRecent.setPath( null );
			setEnabled( false );
		}
	} // class actionClearRecentClass
	
	// action for the Save-Session menu item
	private class actionCloseAllClass
	extends MenuAction
	implements ProcessingThread.Listener
	{
		private actionCloseAllClass( String text, KeyStroke shortcut )
		{
			super( text, shortcut );
			setEnabled( false );	// initially no docs open
		}

		public void actionPerformed( ActionEvent e )
		{
			perform();
		}
		
		private void perform()
		{
			final ProcessingThread pt = closeAll( false, new Flag( false ));
			if( pt != null ) {
				pt.addListener( this );	// ok, let's save the shit and re-try to close all after that
				((BasicDocument) pt.getClientArg( "doc" )).start( pt );
			}
		}
		
		public void processStarted( ProcessingThread.Event e ) {}

		// if the saving was successfull, we will call closeAll again
		public void processStopped( ProcessingThread.Event e )
		{
			if( e.isDone() ) {
				perform();
			}
		}
	}

	/**
	 *  Action to be attached to
	 *  the Preference item of the Edit menu.
	 *  Will bring up the Preferences frame
	 *  when the action is performed.
	 */
	public class actionPreferencesClass
	extends MenuAction
	{
		private actionPreferencesClass( String text, KeyStroke shortcut )
		{
			super( text, shortcut );
		}

		public void actionPerformed( ActionEvent e )
		{
			perform();
		}
		
		/**
		 *  Opens the preferences frame
		 */
		public void perform()
		{
			showPreferences();
		}
	}
	
// ---------------- Action objects for window operations ---------------- 

	// action for the About menu item
	private class actionAboutClass
	extends MenuAction
	{
		private actionAboutClass( String text, KeyStroke shortcut )
		{
			super( text, shortcut );
		}

		/**
		 *  Brings up the About-Box
		 */
		public void actionPerformed( ActionEvent e )
		{
			JFrame aboutBox = (JFrame) root.getComponent( AboutBox.COMP_ABOUTBOX );
		
			if( aboutBox == null ) {
				aboutBox = new AboutBox();
			}
			aboutBox.setVisible( true );
			aboutBox.toFront();
		}
	}

	// generic action for bringing up
	// a window which is identified by
	// a component object. the frame is
	// looked up using the Main's getComponent()
	// method.
	public class actionShowWindowClass extends MenuAction
	{
		private final Object component;
	
		// @param   component   the key for getting the
		//						component using Main.getComponent()
		public actionShowWindowClass( String text, KeyStroke shortcut, Object component )
		{
			super( text, shortcut );
			
			this.component = component;
		}

		/**
		 *  Tries to find the component using
		 *  the <code>Main</code> class' <code>getComponent</code>
		 *  method. It does not instantiate a
		 *  new object if the component is not found.
		 *  If the window is already open, this
		 *  method will bring it to the front.
		 */
		public void actionPerformed( ActionEvent e )
		{
			AbstractWindow w = (AbstractWindow) root.getComponent( component );
			if( w != null ) {
				w.setVisible( true );
				w.toFront();
			}
		}
	}

	// generic action for bringing up
	// a html document either in the
	// help viewer or the default web browser
	private class actionURLViewerClass extends MenuAction
	{
		private final String	theURL;
		private final boolean	openWebBrowser;
	
		// @param	theURL			what file to open ; when using the
		//							help viewer, that's the relative help file name
		//							without .html extension. when using web browser,
		//							that's the complete URL!
		// @param   openWebBrowser	if true, use the default web browser,
		//							if false use internal help viewer
		private actionURLViewerClass( String text, KeyStroke shortcut, String theURL, boolean openWebBrowser )
		{
			super( text, shortcut );
			
			this.theURL			= theURL;
			this.openWebBrowser	= openWebBrowser;
		}

		/**
		 *  Tries to find the component using
		 *  the <code>Main</code> class' <code>getComponent</code>
		 *  method. It does not instantiate a
		 *  new object if the component is not found.
		 *  If the window is already open, this
		 *  method will bring it to the front.
		 */
		public void actionPerformed( ActionEvent e )
		{
			if( openWebBrowser ) {
				try {
					MRJAdapter.openURL( theURL );
				}
				catch( IOException e1 ) {
					GUIUtil.displayError( null, e1, NAME );
				}
			} else {
				HelpFrame.openViewerAndLoadHelpFile( theURL );
			}
		}
	}
}