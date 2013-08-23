/*
 *  Main.java
 *  FScape
 *
 *  Copyright (c) 2001-2013 Hanns Holger Rutz. All rights reserved.
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
 *		06-Dec-04	prefs version check
 *		20-May-05	subclasses de.sciss.app.AbstractApplication
 *		02-Jun-05	includes the static main call from removed FScape.class
 */

package de.sciss.fscape;

import java.awt.EventQueue;
import java.util.prefs.Preferences;
import javax.swing.UIManager;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.net.*;
import de.sciss.fscape.util.*;

import de.sciss.app.DocumentHandler;
import de.sciss.common.AppWindow;
import de.sciss.common.BasicApplication;
import de.sciss.common.BasicMenuFactory;
import de.sciss.common.BasicWindowHandler;
import de.sciss.common.ProcessingThread;
import de.sciss.gui.AboutBox;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.HelpFrame;
import de.sciss.util.Flag;

public class Main
extends BasicApplication
implements OSCRouter
{
	private static final String APP_NAME	= "FScape";

	/*
	 *  Current version of the application. This is stored
	 *  in the preferences file.
	 *
	 *  @todo   should be saved in the session file as well
	 */
	private static final double APP_VERSION		= 1.0;

	/*
	 *  The MacOS file creator string.
	 */
	private static final String					CREATOR			= "FSc ";
	
	/**
	 *  Value for add/getComponent(): the preferences frame
	 *
	 *  @see	#getComponent( Object )
	 */
	public static final Object					COMP_PREFS		= PrefsFrame.class.getName();
	/**
	 *  Value for add/getComponent(): the about box
	 *
	 *  @see	#getComponent( Object )
	 */
	public static final Object					COMP_ABOUTBOX	= AboutBox.class.getName();
	/**
	 *  Value for add/getComponent(): the main log frame
	 *
	 *  @see	#getComponent( Object )
	 */
	public static final Object					COMP_MAIN		= MainFrame.class.getName();
	/**
	 *  Value for add/getComponent(): the online help display frame
	 *
	 *  @see	#getComponent( Object )
	 */
	public static final Object					COMP_HELP  		= HelpFrame.class.getName();

	private final OSCRouterWrapper				osc;
	private static final String					OSC_MAIN		= "main";

	private final ProcessingThread.Listener		quitAfterSaveListener;
	
	public Main( String[] args )
	{
		super( Main.class, APP_NAME );
	
		final java.util.List		warnings;
		final Preferences			prefs			= getUserPrefs();
		final double				prefsVersion;
		final MainFrame				mainFrame;
		final OSCRoot				oscServer;
		String						name;
		
		// ---- init prefs ----

		prefsVersion = prefs.getDouble( PrefsUtil.KEY_VERSION, 0.0 );
		if( prefsVersion < APP_VERSION ) {
			warnings = PrefsUtil.createDefaults( prefs, prefsVersion );
		} else {
			warnings = null;
		}
        
        // ---- init look-and-feel
//        // register submin
//        UIManager.installLookAndFeel( "SubminDark", "de.sciss.submin.SubminDarkLookAndFeel" );
//        UIManager.installLookAndFeel( "Web Look and Feel", "com.alee.laf.WebLookAndFeel" );

		name = prefs.get( PrefsUtil.KEY_LOOKANDFEEL, null );
		if( args.length >= 3 && args[ 0 ].equals( "-laf" )) {
			UIManager.installLookAndFeel( args[ 1 ], args[ 2 ]);
			if( name == null ) name = args[ 2 ];
		}
		lookAndFeelUpdate( prefs.get( PrefsUtil.KEY_LOOKANDFEEL, name ));

//		JFrame.setDefaultLookAndFeelDecorated( true );

		// ---- init infrastructure ----
		// warning : reihenfolge is crucial
		oscServer			= new OSCRoot( prefs.node( OSCRoot.DEFAULT_NODE ), 0x4653 );
		osc					= new OSCRouterWrapper( oscServer, this );
// FFFF
//		cacheManager		= new CacheManager( prefs.node( CacheManager.DEFAULT_NODE ));
//		superCollider		= new SuperColliderClient( this );

		init();

		// ---- listeners ----

		quitAfterSaveListener = new ProcessingThread.Listener() {
			public void processStarted( ProcessingThread.Event e ) {}

			// if the saving was successfull, we will call closeAll again
			public void processStopped( ProcessingThread.Event e )
			{
				if( e.isDone() ) {
					quit();
				}
			}
		};

		// ---- component views ----

		mainFrame   = new MainFrame();
		((BasicWindowHandler) getWindowHandler()).setDefaultBorrower( mainFrame );
// FFFF
//		paletteCtrlRoom = new ControlRoomFrame();
//        frameObserver	= new ObserverPalette();

		if( prefsVersion == 0.0 ) { // means no preferences found, so display splash screen
    		new de.sciss.fscape.gui.WelcomeScreen( this );
		}

		if( warnings != null ) {
			for( int i = 0; i < warnings.size(); i++ ) {
				System.err.println( warnings.get( i ));
			}
		}

		oscServer.init();
	}

	protected BasicMenuFactory createMenuFactory()
	{
		return new MenuFactory( this );
	}
	
	protected DocumentHandler createDocumentHandler()
	{
		return new de.sciss.fscape.session.DocumentHandler( this );
	}
	
	protected BasicWindowHandler createWindowHandler()
	{
		return new BasicWindowHandler( this );
	}
	
	private boolean forcedQuit = false;

	public synchronized void quit()
	{
		final Flag				confirmed	= new Flag( false );
		final ProcessingThread	pt			= getMenuFactory().closeAll( forcedQuit, confirmed );

		if( pt != null ) {
			pt.addListener( quitAfterSaveListener );
			pt.start();
		} else if( confirmed.isSet() ) {
			OSCRoot.getInstance().quit();
//			superCollider.quit();
			super.quit();
		}
	}

	public void forceQuit()
	{
		forcedQuit = true;
		this.quit();
	}

	private void lookAndFeelUpdate( String className )
	{
		if( className != null ) {
			try {
				UIManager.setLookAndFeel( className );
				AppWindow.lookAndFeelUpdate();
			}
			catch( Exception e1 ) {
				GUIUtil.displayError( null, e1, null );
			}
		}
    }

	/**
	 *  java VM starting method. does some
	 *  static initializations and then creates
	 *  an instance of <code>Main</code>.
	 *
	 *  @param  args	are not parsed.
	 */
	public static void main( final String args[] )
	{
		// --- run the main application ---
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		EventQueue.invokeLater( new Runnable() {
			public void run()
			{
				new Main( args );
			}
		});
	}

// ------------ Application interface ------------
	
	public String getMacOSCreator()
	{
		return CREATOR;
	}

	public double getVersion()
	{
		return APP_VERSION;
	}
	
// ---------------- OSCRouter interface ---------------- 

	public String oscGetPathComponent()
	{
		return OSC_MAIN;
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

	public void oscCmd_quit( RoutedOSCMessage rom )
	{
		try {
			if( rom.msg.getArgCount() > 1 ) {
				if( ((Number) rom.msg.getArg( 1 )).intValue() != 0 ) {
					forceQuit();
					return;
				}
			}
			quit();
		}
		catch( ClassCastException e1 ) {
			OSCRoot.failedArgType( rom, 1 );
		}
	}
	
	public Object oscQuery_version()
	{
		return new Float( getVersion() );
	}
}