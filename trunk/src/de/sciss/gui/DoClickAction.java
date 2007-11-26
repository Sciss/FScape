/*
 *  DoClickAction.java
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
 *		20-May-05	created from de.sciss.meloncillo.gui.DoClickAction
 */

package de.sciss.gui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

/**
 *  An <code>AbstractAction</code>
 *  that is linked to a button
 *  component. Whenever the action
 *  is performed, the button's
 *  <code>doClick</code> method
 *  is called. This allows the
 *  <code>DoClickAction</code> to
 *  be used as a global key driven
 *  action without the button being
 *  necessarily in the focussed window.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-05
 *
 *  @see	javax.swing.AbstractButton#doClick()
 */
public class DoClickAction
extends KeyedAction
{
	private final AbstractButton b;

	/**
	 *  Creates a new <code>DoClickAction</code>
	 *  for a given button. <strong>This action
	 *  should NOT be attached to this button</strong>
	 *  because that would produce an infinite feedback
	 *  loop. Instead it should be attached to another
	 *  object like a menu item or, as in the
	 *  <code>GUIUtil.createKeyAction</code> method,
	 *  to the button's input / action map.
	 *
	 *  @param  b   a button whose <code>doClick</code>
	 *				method is called when the action
	 *				is performed.
	 *  @see	GUIUtil#createKeyAction( AbstractButton, KeyStroke )
	 */
	public DoClickAction( AbstractButton b )
	{
		super( null );
		this.b = b;
	}

	/**
	 *  Creates a new <code>DoClickAction</code>
	 *  for a given button. <strong>This action
	 *  should NOT be attached to this button</strong>
	 *  because that would produce an infinite feedback
	 *  loop. Instead it should be attached to another
	 *  object. Since this constructor takes an additional
	 *  <code>KeyStroke</code> which is placed in the
	 *  <code>AbstractAction</code>'s <code>ACCELERATOR_KEY</code>
	 *  field, this is suitable as a <code>JMenuItem</code>'s
	 *  action or an invisible global key action. An example
	 *  of this behaviour can be seen in the <code>ToolPalette</code>
	 *  constructor which uses the <code>MenuFactory</code>'s
	 *  <code>addGlobalKeyCommand</code> method for its buttons.
	 *
	 *  @param  b   a button whose <code>doClick</code>
	 *				method is called when the action
	 *				is performed.
	 */
	public DoClickAction( AbstractButton b, KeyStroke stroke )
	{
		super( stroke );
		this.b = b;
	}

	/**
	 *  Invokes the button's <code>doClick()</code>
	 *  method.
	 *
	 *  @see	javax.swing.AbstractButton#doClick()
	 */
	protected void validActionPerformed( ActionEvent e )
	{
		b.doClick();
	}
}
