/*
 *  MenuAction.java
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.MenuAction
 *		03-Aug-05	new constructor for copying existing actions
 *		01-Oct-05	added installOn() deinstallFrom()
 */

package de.sciss.gui;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 *  A simple extension of <code>AbstractAction</code>
 *  that puts a <code>KeyStroke</code> into its
 *  <code>ACCELERATOR_KEY</code> field. This field
 *  is read when the action is attached to a
 *  <code>JMenuItem</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.2, 01-Oct-05
 *
 *  @see	javax.swing.JMenuItem#setAccelerator( KeyStroke )
 *  @see	javax.swing.JMenuItem#configurePropertiesFromAction( Action )
 *  @see	javax.swing.AbstractButton#setAction( Action )
 */
public abstract class MenuAction
extends AbstractAction
{
	/**
	 *  Constructs a new <code>MenuAction</code> with the given
	 *  text and accelerator shortcut which will be
	 *  used when the action is attached to a <code>JMenuItem</code>.
	 *
	 *  @param  text		text to display in the menu item
	 *  @param  shortcut	<code>KeyStroke</code> for the
	 *						menu item's accelerator or <code>null</code>
	 */
	public MenuAction( String text, KeyStroke shortcut )
	{
		super( text );
		if( shortcut != null ) putValue( ACCELERATOR_KEY, shortcut );
	}

	/**
	 *  Constructs a new <code>MenuAction</code>
	 *  without accelerator key.
	 *
	 *  @param  text		text to display in the menu item
	 */
	public MenuAction( String text )
	{
		super( text );
	}

	/**
	 *  Constructs a new <code>MenuAction</code>
	 *  without name and accelerator key.
	 */
	public MenuAction()
	{
		super();
	}
	
	/**
	 *	Copies the mappings of a given
	 *	<code>Action</code> to this <code>Action</code>.
	 *	The entries which are copied are name,
	 *	key short cuts and descriptions. Therefore
	 *	a menu item carrying this action will look
	 *	exactly like the one associated with the
	 *	passed in action. Also the enabled flag is
	 *	toggled accordingly.
	 *
	 *	@param	a	an <code>Action</code> from which to
	 *				copy the mapping entries
	 */
	public void mimic( Action a )
	{
		this.putValue( NAME, a.getValue( NAME ));
		this.putValue( SMALL_ICON, a.getValue( SMALL_ICON ));
		this.putValue( ACCELERATOR_KEY, a.getValue( ACCELERATOR_KEY ));
		this.putValue( MNEMONIC_KEY, a.getValue( MNEMONIC_KEY ));
		this.putValue( SHORT_DESCRIPTION, a.getValue( SHORT_DESCRIPTION ));
		this.putValue( LONG_DESCRIPTION, a.getValue( LONG_DESCRIPTION ));
		this.setEnabled( a.isEnabled() );
	}
	
	/**
	 *	Installs this action on the
	 *	keyboard input and action map of the given
	 *	component.
	 *
	 *	@param	c			the component to install the action on
	 *	@param	condition	either of <code>JComponent.WHEN_FOCUSED</code>,
	 *						<code>JComponent.WHEN_IN_FOCUSED_WINDOW</code>, or
	 *						<code>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code>.
	 */
	public void installOn( JComponent c, int condition )
	{
		final InputMap	imap = c.getInputMap( condition );
		final ActionMap amap = c.getActionMap();
		final String	name = (String) getValue( Action.NAME );
		imap.put( (KeyStroke) getValue( Action.ACCELERATOR_KEY ), name );
		amap.put( name, this );
	}
	
	/**
	 *	Deinstalls this action from the
	 *	keyboard input and action map of the given
	 *	component.
	 *
	 *	@param	c			the component to remove the action from
	 *	@param	condition	either of <code>JComponent.WHEN_FOCUSED</code>,
	 *						<code>JComponent.WHEN_IN_FOCUSED_WINDOW</code>, or
	 *						<code>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code>.
	 */
	public void deinstallFrom( JComponent c, int condition )
	{
		final InputMap	imap = c.getInputMap( condition );
		final ActionMap amap = c.getActionMap();
		imap.remove( (KeyStroke) getValue( Action.ACCELERATOR_KEY ));
		amap.remove( getValue( Action.NAME ));
	}

	public abstract void actionPerformed( ActionEvent e );
}
