/*
 *  CompactPanel.java
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
 *		05-Mar-05	created
 */

package de.sciss.fscape.gui;

import java.awt.*;
import javax.swing.*;

import de.sciss.app.AbstractApplication;
import de.sciss.gui.GUIUtil;

/**
 *  A subclass of JPanel that automatically
 *	maintains a SpringLayout. Components must be
 *	added to this panel using the addGadget() method.
 *	Components are arranged from left to right in one row,
 *	until a newLine() call is made. each time a newLine()
 *	is performed, missing columns are automatically fillled
 *	with blanks. When all elements have been added to the
 *	panel, you should call compact() once, and the springs
 *	are installed as in GUIUtil.makeCompactSpringGrid().
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class CompactPanel
extends JPanel
{
	private int numColumns = 0, rowIdx = 0, columnIdx = 0;

	public CompactPanel()
	{
		super( new SpringLayout() );
	}
	
	public void addGadget( Component comp )
	{
		super.add( comp );
		
//		System.err.print( "adding "+comp.getClass().getName()+" at row "+rowIdx+" ; col "+columnIdx );
		increaseColumnIndex();
//		System.err.println( " ; now columnIdx = "+columnIdx+" ; numColumns = "+numColumns );
	}
	
	/**
	 *	Throws an AWTError to because you have to use addGadget()
	 */
	public Component add( Component comp )
	{
		throw new AWTError( AbstractApplication.getApplication().getResourceString( "errCompactPanelAdd" ));
	}

	/**
	 *	Throws an AWTError to because you have to use addGadget()
	 */
	public Component add( Component comp, int index )
	{
		throw new AWTError( AbstractApplication.getApplication().getResourceString( "errCompactPanelAdd" ));
	}
	
	public void addEmptyColumn()
	{
		super.add( new JLabel() );
		increaseColumnIndex();
	}

	private void addEmptyColumns()
	{
		while( columnIdx < numColumns ) addEmptyColumn();
	}

	private void increaseColumnIndex()
	{
		columnIdx++;
		if( columnIdx > numColumns ) {
			for( int row = rowIdx - 1; row >= 0; row-- ) {
				for( int col = numColumns, index = (row + 1) * numColumns; col < columnIdx; col++ ) {
					super.add( new JLabel(), index );
				}
			}
			numColumns = columnIdx;
		}
	}
	
	public void newLine()
	{
		if( rowIdx == 0 ) {
			numColumns = columnIdx;
		} else {
			addEmptyColumns();
		}
		columnIdx = 0;
		rowIdx++;
	}
	
	public void compact()
	{
		addEmptyColumns();
		GUIUtil.makeCompactSpringGrid( this, rowIdx + 1, numColumns, 0, 0, 4, 2 );
	}
}
