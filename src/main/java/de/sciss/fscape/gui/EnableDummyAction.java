/*
 *  EnableDummyAction.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		24-Jun-06	copied from de.sciss.eisenkraut.gui.EnableDummyAction
 */

package de.sciss.fscape.gui;

import java.awt.event.ActionEvent;
import javax.swing.Action;

import de.sciss.gui.MenuAction;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.56, 05-May-06
 */
public class EnableDummyAction
extends MenuAction
{
	public EnableDummyAction( Action dummy )
	{
		super();
		mimic( dummy );
		setEnabled( true );
	}
	
	public void actionPerformed( ActionEvent e ) {}
}