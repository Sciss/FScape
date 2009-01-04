/*
 *  MainFrame.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
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
 *		25-Jan-05	created
 *		12-May-05	uses new synthesizers based on a Phasor and
 *					BufRd ugens, which allow sample accurate timing
 *					(crucial when cutting and re-arranging the sound file)
 *		13-Jul-05	bugfix with channel inputs
 *		22-Jul-05	sample-rate-conversion, adjusts to channel changes, etc.
 *		05-Aug-05	split of from SuperColliderFrame to be a mere GUI component
 *		24-Jun-06	created from de.sciss.eisenkraut.gui.MainFrame
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import de.sciss.fscape.*;
import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;
import de.sciss.app.Application;
import de.sciss.common.AppWindow;
import de.sciss.common.BasicApplication;

import de.sciss.gui.AbstractWindowHandler;
import de.sciss.gui.CoverGrowBox;
import de.sciss.gui.LogTextArea;

// XXX FFFF
//import de.sciss.jcollider.*;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 03-Oct-06
 */
public class MainFrame
extends AppWindow
// XXX FFFF
//implements de.sciss.jcollider.Constants, ServerListener
//implements	RunnableProcessing, ProgressComponent, TransportListener,
//			RealtimeConsumer, OSCListener, de.sciss.app.DocumentListener
{
//	private final PrintStream		logStream;
	
// XXX FFFF
//	private boolean					keepScRunning;
//	private ProcessingThread		pt				= null;
//	private boolean					booted			= false;
//	
//	private final actionBootClass	actionBoot;
//
////	private final JCheckBox			ggDumpOSC;
//	private final MultiStateButton	ggDumpOSC;
//	private final JLabel			lbStatus1;
//	private final JLabel			lbStatus2;
//
//	private ServerOptions			serverOptions;
//
//	private final MessageFormat		msgStatus1;
//	private final MessageFormat		msgStatus2;
//	private final Object[]			argsStatus		= new Object[ 5 ];
//	private final String			unknownStatus;
//	private final Box				boxStatus2;

	private final LogTextArea		lta;
// XXX FFFF
//	private final MultiStateButton	ggBoot;

	private final Font				fntMonoSpaced;

	private static final String[]	EZRA = {
		"rain is also of the process", "the stone knows the form", "by paint immortal as no other age is immortal",
		"in a land of maple", "beauty is difficult", "not of one bird but of many",
		"no cloud, but the crystal body", "well, my window", "the rest is explodable",
		"who fished for sound in the Seine", "the chess board too lucid", "there will certainly be minor plurloinments",
		"mint, thyme and basicilum", "the Godess was born of sea-foam", "in identity but not atom for atom",
		"in profusion nearly all humble persons", "hence the d\u00E9bacle", "je suis au bout de mes forces",
		"you can sleep here for a peseta", "but to have done instead of not doing", "and you might find a bit of enamel",
		"there is fatigue deep as a grave", "there are distinctions in clarity", ""
	};

	public MainFrame()
	{
		super( REGULAR );

		final Application	app	= AbstractApplication.getApplication();

		if( app.getWindowHandler().usesInternalFrames() ) {
			setTitle( app.getResourceString( "frameMain" ));
			((JInternalFrame) getWindow()).setClosable( false );
		} else {
			setTitle( app.getName() + " : " + app.getResourceString( "frameMain" ));
		}
		
		final Container					cp				= getContentPane();
// XXX FFFF
		final Box						boxStatus1		= Box.createHorizontalBox();
		final JPanel					bottomPane		= new JPanel( new BorderLayout( 4, 2 ));
		final JScrollPane				ggScroll;
//		final JButton					ggBoot;
		final AbstractWindow.Listener	winListener;
// XXX FFFF
//		final TreeExpanderButton		ggStatusExp;
//		String[]						cfgNames		= null;
		final String[]					fntNames;
	
		lta				= new LogTextArea( 4, 50, false, null );
//lta.setBackground( null );
//lta.setForeground( new Color( 0xFF, 0xFF, 0xFF ));
		ggScroll		= lta.placeMeInAPane();
//ggScroll.setBackground( null );
		lta.makeSystemOutput();

boxStatus1.add( new JLabel( EZRA[ Calendar.getInstance().get( Calendar.HOUR_OF_DAY )]));
		
// XXX FFFF
//		actionBoot		= new actionBootClass();
////		ggBoot			= new JButton( actionBoot );
//		ggBoot			= new MultiStateButton();
//		ggBoot.setFocusable( false );	// prevent user from accidentally starting/stopping server
//		ggBoot.setAutoStep( false );
//		ggBoot.addActionListener( actionBoot );
//		
//		ggDumpOSC		= new MultiStateButton();
//		ggDumpOSC.setFocusable( false );
//		ggDumpOSC.addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent e ) {
//				root.superCollider.dumpOSC( ggDumpOSC.getSelectedIndex() );
//			}
//		});
//		
//		boxStatus2		= Box.createHorizontalBox();
//		msgStatus1		= new MessageFormat( getResourceString( "ptrnServerStatus1" ), Locale.US );
//		msgStatus2		= new MessageFormat( getResourceString( "ptrnServerStatus2" ), Locale.US );
//		unknownStatus	= getResourceString( "labelServerNotRunning" );
//		lbStatus1		= new JLabel( unknownStatus );
//		lbStatus2		= new JLabel();
//
//		ggStatusExp	= new TreeExpanderButton();
//		ggStatusExp.setExpandedToolTip( getResourceString( "buttonExpStatsTT" ));
//		ggStatusExp.setCollapsedToolTip( getResourceString( "buttonCollStatsTT" ));
//		ggStatusExp.addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent e )
//			{
//				final int	width	= getWidth();
//				final int	height	= getHeight();
//				
//				if( ggStatusExp.isExpanded() ) {
//					boxStatus2.setVisible( true );
//					setSize( width, height + boxStatus2.getPreferredSize().height );
//				} else {
//					boxStatus2.setVisible( false );
//					setSize( width, height - boxStatus2.getPreferredSize().height );
//				}
//			}
//		});
//		
//		lbStatus1.setPreferredSize( new Dimension( 192, lbStatus1.getPreferredSize().height ));
//		lbStatus2.setPreferredSize( new Dimension( 226, lbStatus1.getPreferredSize().height ));
//		
//		boxStatus1.add( new JLabel( new ImageIcon( getClass().getResource( "sc-icon.png" ))));
//		boxStatus1.add( Box.createHorizontalStrut( 2 ));
//		boxStatus1.add( ggStatusExp );
//		boxStatus1.add( lbStatus1 );
//		boxStatus1.add( ggBoot );
//		boxStatus1.add( Box.createHorizontalGlue() );
//
////		boxStatus2.add( Box.createHorizontalStrut( 32 ));
//		boxStatus2.add( lbStatus2 );
//		boxStatus2.add( ggDumpOSC );
//		boxStatus2.add( Box.createHorizontalGlue() );
//		boxStatus2.setVisible( false );
//
		boxStatus1.add( CoverGrowBox.create() );
// XXX FFFF
//		boxStatus2.add( CoverGrowBox.create() );
//		
		bottomPane.add( boxStatus1, BorderLayout.NORTH );
// XXX FFFF
//		bottomPane.add( boxStatus2, BorderLayout.SOUTH );
////		bottomPane.add( ggBoot, BorderLayout.EAST );
//		bottomPane.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 4 ));
bottomPane.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ));
		cp.add( ggScroll, BorderLayout.CENTER );
		cp.add( bottomPane, BorderLayout.SOUTH );

		fntNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		if( contains( fntNames, "Monaco" )) {	// Mac OS
			fntMonoSpaced = new Font( "Monaco", Font.PLAIN, 9 );	// looks bigger than it is
		} else {
			fntMonoSpaced = new Font( "Monospaced", Font.PLAIN, 10 );
		}
		
		AbstractWindowHandler.setDeepFont( cp );
		
// XXX FFFF
//		ggBoot.setNumColumns( 9 );
//		ggBoot.addItem( getResourceString( "buttonBoot" ), null, null );
//		ggBoot.addItem( getResourceString( "buttonTerminate" ), null, new Color( 0xFF, 0xFA, 0x9D ), new Color( 0xFA, 0xE7, 0x9D ));
//		ggDumpOSC.setNumColumns( 9 );
//		ggDumpOSC.addItem( getResourceString( "labelDumpOff" ), null, null );
//		ggDumpOSC.addItem( getResourceString( "labelDumpText" ), null, new Color( 0xFF, 0xFA, 0x9D ), new Color( 0xFA, 0xE7, 0x9D ));
//		ggDumpOSC.addItem( getResourceString( "labelDumpHex" ), null, new Color( 0xFF, 0x9D, 0x9D ), new Color( 0xFA, 0x8D, 0x9D ));
//		final Dimension d = new Dimension( ggDumpOSC.getPreferredSize().width, ggDumpOSC.getMaximumSize().height );
//		ggBoot.setMaximumSize( d );
//		ggDumpOSC.setMaximumSize( d );
//		
//		lbStatus1.setFont( fntMonoSpaced );
//		lbStatus2.setFont( fntMonoSpaced );
		lta.setFont( fntMonoSpaced );

		// ---- menus and actions ----
		
		((BasicApplication) app).getMenuBarRoot().putMimic( "edit.clear", this, lta.getClearAction() );

		// ---- listeners -----
		
		winListener = new AbstractWindow.Adapter() {
			public void windowClosing( AbstractWindow.Event e ) {
				app.quit();
			}
		};
		addListener( winListener );
		
// FFFF
//		superCollider.addServerListener( this );
		
		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		
		init();
		app.addComponent( Main.COMP_MAIN, this );
// bisschen daemliche art, die zentrierung zu verhindern
		final Rectangle r = getBounds();
		r.setLocation( 0, 0 );
		setBounds( r );
		setVisible( true );
//		toFront();
	}

	protected boolean restoreVisibility()
	{
		return false;
	}
			
	public void dispose()
	{
		AbstractApplication.getApplication().removeComponent( Main.COMP_MAIN );
		super.dispose();
	}
	
	private static boolean contains( String[] array, String name )
	{
		for( int i = 0; i < array.length; i++ ) {
			if( array[ i ].equals( name )) return true;
		}
		return false;
	}
	
//	private String getResourceString( String key )
//	{
//		return AbstractApplication.getApplication().getResourceString( key );
//	}
	
// XXX FFFF
//	private void updateStatus()
//	{
//		final Server.Status s = root.superCollider.getStatus();
//		if( s != null ) {
//			argsStatus[ 0 ]	= new Float( s.sampleRate );
//			argsStatus[ 1 ]	= new Float( s.avgCPU );
//			lbStatus1.setText( msgStatus1.format( argsStatus ));
//			if( boxStatus2.isVisible() ) {
//				argsStatus[ 0 ]	= new Integer( s.numUGens );
//				argsStatus[ 1 ]	= new Integer( s.numSynths );
//				argsStatus[ 2 ]	= new Integer( s.numGroups );
//				argsStatus[ 3 ]	= new Integer( s.numSynthDefs );
//				lbStatus2.setText( msgStatus2.format( argsStatus ));
//			}
//		} else {
//			lbStatus1.setText( unknownStatus );
//		}
//	}
//	
//// ------------- ServerListener interface -------------
//
//	public void serverAction( ServerEvent e )
//	{
//		switch( e.getID() ) {
//		case ServerEvent.RUNNING:
//			actionBoot.booted();
//			break;
//
//		case ServerEvent.STOPPED:
//			actionBoot.terminated();
//			updateStatus();
//			break;
//
//		case ServerEvent.COUNTS:
//			updateStatus();
//			break;
//			
//		default:
//			break;
//		}
//	}
//
//// ------------- interne klassen -------------
//
//	private class actionBootClass
//	extends AbstractAction
//	{
//		private actionBootClass()
//		{
//			super();
////			super( getResourceString( "buttonBoot" ));
////			putValue( SMALL_ICON, new ImageIcon( getClass().getResource( "sc-icon.png" )));
//		}
//		
//		public void actionPerformed( ActionEvent e )
//		{
//			if( booted ) {
//				root.superCollider.stop();
//			} else {
//				root.superCollider.boot();
//			}
//		}
//		
//		private void terminated()
//		{
//			booted = false;
////			putValue( NAME, getResourceString( "buttonBoot" ));
//			ggBoot.setSelectedIndex( 0 );
//		}
//
//		private void booted()
//		{
//			booted = true;
////			putValue( NAME, getResourceString( "buttonTerminate" ));
//			ggBoot.setSelectedIndex( 1 );
//		}
//	} // class actionBootClass
}