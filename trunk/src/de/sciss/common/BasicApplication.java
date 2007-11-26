/*
 *  BasicApplication.java
 *  de.sciss.common package
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
 *		02-Sep-06	created
 */

package de.sciss.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

import de.sciss.app.AbstractApplication;
import de.sciss.app.DocumentHandler;
import de.sciss.app.GraphicsHandler;
import de.sciss.app.WindowHandler;
import de.sciss.gui.MenuAction;
import de.sciss.gui.MenuRoot;

public abstract class BasicApplication
extends AbstractApplication
{
	private final actionQuitClass		actionQuit;
	private BasicWindowHandler			wh;
	private DocumentHandler				dh;
	private GraphicsHandler				gh;
	private BasicMenuFactory			mf;

	protected BasicApplication( Class c, String name )
	{
		super( c, name );

		actionQuit			= new actionQuitClass( getResourceString( "menuQuit" ),
												   KeyStroke.getKeyStroke( KeyEvent.VK_Q,
												   BasicMenuFactory.MENU_SHORTCUT ));
	}
	
	protected void init()
	{
		gh					= new BasicGraphicsHandler();
		dh					= createDocumentHandler();
		mf					= createMenuFactory();
		wh					= createWindowHandler();

		mf.init();
		wh.init();

		// ---- listeners ----

		MRJAdapter.addOpenDocumentListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				handleOpenFile( (ApplicationEvent) e );
			}
		});
	}

	public GraphicsHandler getGraphicsHandler()
	{
		return gh;
	}

	protected abstract BasicMenuFactory		createMenuFactory();
	protected abstract BasicWindowHandler	createWindowHandler();
	protected abstract DocumentHandler		createDocumentHandler();

	public WindowHandler getWindowHandler()
	{
		return wh;
	}

	public DocumentHandler getDocumentHandler()
	{
		return dh;
	}

	public MenuRoot getMenuBarRoot()
	{
		return mf;
	}

	public BasicMenuFactory getMenuFactory()
	{
		return mf;
	}
	
	public Action getQuitAction()
	{
		return actionQuit;
	}

	private void handleOpenFile( ApplicationEvent e )
	{
		mf.openDocument( e.getFile() );
	}

// ---------------- internal classes ---------------- 

	// action for Application-Quit menu item
	private class actionQuitClass
	extends MenuAction
	{
//		private String text;
	
		private actionQuitClass( String text, KeyStroke shortcut )
		{
			super( text, shortcut );
			
//			this.text = text;
		}
		
		public void actionPerformed( ActionEvent e )
		{
			quit();
		}
	}
}