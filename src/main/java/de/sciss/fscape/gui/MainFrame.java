///*
// *  MainFrame.java
// *  (FScape)
// *
// *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
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
// *		25-Jan-05	created
// *		12-May-05	uses new synthesizers based on a Phasor and
// *					BufRd ugens, which allow sample accurate timing
// *					(crucial when cutting and re-arranging the sound file)
// *		13-Jul-05	bugfix with channel inputs
// *		22-Jul-05	sample-rate-conversion, adjusts to channel changes, etc.
// *		05-Aug-05	split of from SuperColliderFrame to be a mere GUI component
// *		24-Jun-06	created from de.sciss.eisenkraut.gui.MainFrame
// */
//
//package de.sciss.fscape.gui;
//
//import java.awt.*;
//import java.util.*;
//import javax.swing.*;
//import de.sciss.fscape.*;
//
//import de.sciss.gui.CoverGrowBox;
//import de.sciss.gui.GUIUtil;
//import de.sciss.gui.LogTextArea;
//import de.sciss.gui.MenuRoot;
//
///**
// *  @author		Hanns Holger Rutz
// *  @version	0.70, 03-Oct-06
// */
//public class MainFrame
//extends AppWindow
//{
//
//	private final LogTextArea		lta;
//
//	private final Font				fntMonoSpaced;
//
//	private static final String[]	EZRA = {
//		"rain is also of the process", "the stone knows the form", "by paint immortal as no other age is immortal",
//		"in a land of maple", "beauty is difficult", "not of one bird but of many",
//		"no cloud, but the crystal body", "well, my window", "the rest is explodable",
//		"who fished for sound in the Seine", "the chess board too lucid", "there will certainly be minor plurloinments",
//		"mint, thyme and basilicum", "the Godess was born of sea-foam", "in identity but not atom for atom",
//		"in profusion nearly all humble persons", "hence the d\u00E9bacle", "je suis au bout de mes forces",
//		"you can sleep here for a peseta", "but to have done instead of not doing", "and you might find a bit of enamel",
//		"there is fatigue deep as a grave", "there are distinctions in clarity", ""
//	};
//
//	public MainFrame()
//	{
//		super( REGULAR );
//
//		final Application	app	= AbstractApplication.getApplication();
//
//		if( app.getWindowHandler().usesInternalFrames() ) {
//			setTitle( app.getResourceString( "frameMain" ));
//			((JInternalFrame) getWindow()).setClosable( false );
//		} else {
//			setTitle( app.getName() + " : " + app.getResourceString( "frameMain" ));
//		}
//
//		final Container					cp				= getContentPane();
//		final Box						boxStatus1		= Box.createHorizontalBox();
//		final JPanel					bottomPane		= new JPanel( new BorderLayout( 4, 2 ));
//		final JScrollPane				ggScroll;
//		final AbstractWindow.Listener	winListener;
//
//		final String[]					fntNames;
//
//		lta				= new LogTextArea( 4, 50, false, null );
//		ggScroll		= lta.placeMeInAPane();
//		lta.makeSystemOutput();
//
//boxStatus1.add( new JLabel( EZRA[ Calendar.getInstance().get( Calendar.HOUR_OF_DAY )]));
//
//		boxStatus1.add( CoverGrowBox.create() );
//
//		bottomPane.add( boxStatus1, BorderLayout.NORTH );
//
//bottomPane.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ));
//		cp.add( ggScroll, BorderLayout.CENTER );
//		cp.add( bottomPane, BorderLayout.SOUTH );
//
//		fntNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//		if( contains( fntNames, "Monaco" )) {	// Mac OS
//			fntMonoSpaced = new Font( "Monaco", Font.PLAIN, 9 );	// looks bigger than it is
//		} else {
//			fntMonoSpaced = new Font( "Monospaced", Font.PLAIN, 10 );
//		}
//
//		AbstractWindowHandler.setDeepFont( cp );
//
//		lta.setFont( fntMonoSpaced );
//
//		// ---- menus and actions ----
//
//		final MenuRoot mr = ((BasicApplication) app).getMenuBarRoot();
//		mr.putMimic( "edit.clear", this, lta.getClearAction() );
//
//		// ---- listeners -----
//
//		winListener = new AbstractWindow.Adapter() {
//			public void windowClosing( AbstractWindow.Event e ) {
//				app.quit();
//			}
//		};
//		addListener( winListener );
//
//		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
//
//		init();
//		app.addComponent( Main.COMP_MAIN, this );
//		final Rectangle r = getBounds();
//		r.setLocation( 0, 0 );
//		setBounds( r );
//		setVisible( true );
//
//		GUIUtil.removeMenuModifierBindings( lta, mr );
//		GUIUtil.removeMenuModifierBindings( new JTextField(), mr );
//	}
//
//	protected boolean restoreVisibility()
//	{
//		return false;
//	}
//
//	public void dispose()
//	{
//		AbstractApplication.getApplication().removeComponent( Main.COMP_MAIN );
//		super.dispose();
//	}
//
//	private static boolean contains( String[] array, String name )
//	{
//		for( int i = 0; i < array.length; i++ ) {
//			if( array[ i ].equals( name )) return true;
//		}
//		return false;
//	}
//}