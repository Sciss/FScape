/*
 *  BooleanPrefsMenuAction.java
 *  (de.sciss.gui package)
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
 *  Change log:
 *		10-Sep-06	created
 */

package de.sciss.gui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

/**
 *	Adds PreferenceEntrySync functionality to the superclass
 *	note that unlike PrefCheckBox and the like, it's only
 *	valid to listen to the prefs changes, not the action events
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 10-Sep-06
 */
public class BooleanPrefsMenuAction
extends PrefMenuAction
{
	private MenuCheckItem mci;

	public BooleanPrefsMenuAction( String text, KeyStroke shortcut )
	{
		super( text, shortcut );
	}
	
	public void setCheckItem( MenuCheckItem mci )
	{
		this.mci = mci;
	}

	/**
	 *  Switches button state
	 *  and updates preferences. 
	 */
	public void actionPerformed( ActionEvent e )
	{
		boolean state   = ((AbstractButton) e.getSource()).isSelected();

		if( mci != null ) mci.setSelected( state );
	
		if( shouldWritePrefs() ) {
			getPreferenceNode().putBoolean( getPreferenceKey(), state );
		}
	}
	
	public void writePrefs()
	{
		if( canWritePrefs() && (mci != null) ) getPreferenceNode().putBoolean( getPreferenceKey(), mci.isSelected() );
	}

	protected void readPrefsFromString( String prefsValue )
	{
		if( prefsValue == null ) return;
		final boolean prefsVal	= Boolean.valueOf( prefsValue ).booleanValue();

		if( mci != null ) mci.setSelected( prefsVal );
	}
}