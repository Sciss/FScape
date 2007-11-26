/*
 *  Document.java
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
 *		20-May-05	created
 */

package de.sciss.app;

import de.sciss.util.Disposable;

/**
 *  The <code>Document</code> interface is an attempt
 *	to create common classes and interfaces (the package
 *	<code>de.sciss.app</code>) which can be shared by
 *	different programmes, such as Meloncillo or FScape,
 *	without having to make adjustments in different places
 *	each time a modification is made. This interface
 *	describes the most prominent methods needed for
 *	a general application document.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.11, 24-Jun-06
 */
public interface Document
extends Disposable
{
	/**
	 *	Returns the application carrying this document.
	 *
	 *	@return	the application carrying this document
	 */
	public de.sciss.app.Application getApplication();
	
	/**
	 *	Returns the undo manager responsible for this document.
	 *
	 *	@return	the undo manager responsible for this document
	 */
	public de.sciss.app.UndoManager getUndoManager();

	/**
	 *	Asks whether this document contains unsaved changes or not.
	 *
	 *	@return	<code>true</code> indicates that the
	 *			document contains unsaved changes,
	 *			<code>false</code> indicates that the
	 *			document was saved or cleared.
	 */
	public boolean isDirty();

	/**
	 *	Marks this document as having unsaved changes or
	 *	as being saved.
	 *
	 *	@param	dirty	<code>true</code> indicates that the
	 *					document contains unsaved changes,
	 *					<code>false</code> indicates that the
	 *					document was saved or cleared.
	 */
	public void setDirty( boolean dirty );

	public String getName();

//	/**
//	 *	Call this method to dispose any resources
//	 *	associated with the document, when the document
//	 *	is destroyed.
//	 */
//	public void dispose();
}