/*
 *	KeyedAction.java
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
 *		20-May-05	created from de.sciss.meloncillo.gui.KeyedAction
 *		24-Sep-05	added getStroke() method
 */
 
package de.sciss.gui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

/**
 *	A special <code>Action</code> class
 *	whose constructor requires an accelerator key.
 *	When the action listener is invoked, the
 *	current keyboard focus component is queried.
 *	If a text editing component has the focus,
 *	the action is silently aborted. Otherwise
 *	the abstract method <code>validActionPerformed</code>
 *	is called. This method must be overriden by subclasses.
 *	This allows global keyboard commands without modifier keys to be installed
 *	and they won't interfer when the user edits text fields.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.27, 24-Sep-05
 */
public abstract class KeyedAction
extends AbstractAction
{
	private boolean ignoreFocus;
	
//	private static final int[] IF_CODES = { KeyEvent.VK_ACCEPT, KeyEvent.VK_CANCEL, KeyEvent.VK_ESCAPE };

	public KeyedAction( KeyStroke stroke )
	{
		super();
		putValue( ACCELERATOR_KEY, stroke );
		
		ignoreFocus = (stroke == null) ||
					  ((stroke.getModifiers() & (KeyEvent.META_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)) != 0);
//		if( !ignoreFocus ) {
//			
//		}
	}
	
	public void setIgnoreFocus( boolean ignore )
	{
		ignoreFocus	= ignore;
	}
	
	public boolean getIgnoreFocus()
	{
		return ignoreFocus;
	}
	
	public KeyStroke getStroke()
	{
		return( (KeyStroke) getValue( ACCELERATOR_KEY ));
	}
	
	public final void actionPerformed( ActionEvent e )
	{
		if( !ignoreFocus ) {
			final Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if( (c != null) && (c instanceof JTextComponent) && ((JTextComponent) c).isEditable() ) return;
		}
		validActionPerformed( e );
	}
	
	protected abstract void validActionPerformed( ActionEvent e );
}
