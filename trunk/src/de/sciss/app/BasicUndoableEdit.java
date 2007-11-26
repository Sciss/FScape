/*
 *  BasicUndoableEdit.java
 *  de.sciss.app package
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
 *		07-Jul-05	copied from de.sciss.meloncillo.edit.BasicUndoableEdit
 *		21-Apr-06	new concept of necessary separate perform() call ; and cancel()
 *		01-May-06	moved to de.sciss.app package
 */

package de.sciss.app;

import javax.swing.undo.AbstractUndoableEdit;

/**
 *	@version	0.70, 01-May-06
 *	@author		Hanns Holger Rutz
 */
public abstract class BasicUndoableEdit
extends AbstractUndoableEdit
implements PerformableEdit
{
	protected String getResourceString( String key )
	{
		return AbstractApplication.getApplication().getResourceString( key );
	}
	
// UUU
//	public void cancel();

	public void debugDump( int nest )
	{
//		final StringBuffer strBuf = new StringBuffer( nest << 1 );
//		for( int i = 0; i < nest; i++ ) strBuf.append( "  " );
//		System.err.print( strBuf.toString() );
		System.err.println( this.toString() );
	}
	
	public String toString()
	{
		return( getClass().getName().toString() + " (\"" + getPresentationName() + "\") ; canUndo = "+canUndo()+"; canRedo = "+canRedo()+"; isSignificant = "+isSignificant() );
	}
}
