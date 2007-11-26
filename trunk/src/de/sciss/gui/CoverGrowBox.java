/*
 *  CoverGrowBox.java
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
 *		21-Sep-06	created
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;

import de.sciss.app.AbstractApplication;

public class CoverGrowBox
{
	/**
	 *  Value: Boolean stating whether frame size (grow) boxes
     *  intrude into the frame's pane. Has default value: yes!<br>
	 *  Node: root
	 */
	public static final String KEY_INTRUDINGSIZE = "intrudingsize";

	public static Component create()
	{
		return create( 0, 0 );
	}

	public static Component create( int padx, int pady )
	{
		final boolean intruding = AbstractApplication.getApplication().getUserPrefs().getBoolean( KEY_INTRUDINGSIZE, false );
		return Box.createRigidArea( new Dimension( intruding ? 16 + padx : padx, intruding ? 16 + pady : pady ));
	}
}