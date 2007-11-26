/*
 *  AboutBox.java
 *  de.sciss.gui package
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
 *		31-Jul-04   commented
 *		26-May-05	moved from de.sciss.meloncillo.gui
 */

package de.sciss.gui;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.ImageIcon;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.roydesign.mac.MRJAdapter;
import net.roydesign.ui.StandardMacAboutFrame;

import de.sciss.app.AbstractApplication;

/**
 *  About, Copyright + Credits Frame
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 02-Sep-06
 *
 *	@todo		integrate with new AbstractWindow concept
 */
public class AboutBox
extends StandardMacAboutFrame
implements HyperlinkListener
{
	/**
	 *  Value for add/getComponent(): the about box
	 *
	 *  @see	#getComponent( Object )
	 */
	public static final Object					COMP_ABOUTBOX	= AboutBox.class.getName();

	private static final String CREDITS_START   =
		"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" "+
		"\"http://www.w3.org/TR/REC-html40/loose.dtd\">"+
		"<html><head><style type=\"text/css\"><!--\n"+
		"p { font-family:\"Lucida Grande\" Helvetica sans-serif;font-size:"+
		"10pt;padding:2pt 0 2pt 0;margin:0; }\n"+
		"--></style></head><body>";
	private static final String CREDITS_END		= "</body></html>";

    public AboutBox()
	{
        super( AbstractApplication.getApplication().getName(),
			   String.valueOf( AbstractApplication.getApplication().getVersion() ));

		final StringBuffer				credits	= new StringBuffer( CREDITS_START );
		final de.sciss.app.Application	app		= AbstractApplication.getApplication();
//		final char						sep		= File.separatorChar;

//		setApplicationIcon( new ImageIcon( app.getClass().getResource( "application.png" )));
		setApplicationIcon( new ImageIcon( "images" + File.separator + "application.png" ));
		setCopyright( app.getResourceString( "copyright" ));
		setHyperlinkListener( this );
		credits.append( app.getResourceString( "credits" ));
		credits.append( "<P>Java " );
		credits.append( System.getProperty( "java.version" ));
		credits.append( "</P>" );
		credits.append( CREDITS_END );
		setCredits( credits.toString(), "text/html" );
//System.err.println( credits.toString() );

//		pack();

		app.addComponent( COMP_ABOUTBOX, this );
	}
	
	public void dispose()
	{
		AbstractApplication.getApplication().removeComponent( COMP_ABOUTBOX );
		super.dispose();
	}
	
	public void setBuildVersion( File f )
	{
		final long build   = f.lastModified();

		if( build != 0L ) {
			setBuildVersion( DateFormat.getDateInstance( DateFormat.SHORT ).format( new Date( build )));
		}
	}

	// --------- HyperlinkListener interface ---------

	public void hyperlinkUpdate( HyperlinkEvent e )
	{
		if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
			try {
				MRJAdapter.openURL( e.getURL().toString() );
			}
			catch( Exception e1 ) {
				GUIUtil.displayError( this, e1, this.getTitle() );
			}
		}
	}
}