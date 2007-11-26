/*
 *  DocumentEvent.java
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
 *		02-Aug-05	created
 */

package de.sciss.app;

import de.sciss.app.BasicEvent;

/**
 *  This kind of event is fired
 *  from a <code>DocumentHandler</code> when
 *  a document as been created, destroyed
 *	or switched.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-06
 *
 *  @see		DocumentHandler#addDocumentListener( DocumentListener )
 *  @see		DocumentHandler
 *  @see		Document
 */
public class DocumentEvent
extends BasicEvent
{
// --- ID values ---
	/**
	 *  returned by getID() : the document has been added
	 */
	public static final int ADDED		= 0;

	/**
	 *  returned by getID() : the document has been removed
	 */
	public static final int REMOVED		= 1;

	/**
	 *  returned by getID() : the document has become the active document
	 */
	public static final int FOCUSSED	= 2;

	private final Document	doc;

	/**
	 *  Constructs a new <code>DocumentEvent</code>
	 *
	 *  @param  source  who originated the action
	 *  @param  ID		<code>ADDED</code>, <code>REMOVED</code>, or <code>FOCUSSED</code>
	 *  @param  when	system time when the event occured
	 *  @param  doc		the related document
	 */
	public DocumentEvent( Object source, int ID, long when, Document doc )
	{
		super( source, ID, when );
	
		this.doc		= doc;
	}
	
	/**
	 *  Queries the related document
	 *
	 *  @return the document which was added, removed or focussed.
	 */
	public Document getDocument()
	{
		return doc;
	}

	public boolean incorporate( BasicEvent oldEvent )
	{
		if( oldEvent instanceof DocumentEvent &&
			this.getSource() == oldEvent.getSource() &&
			this.getID() == oldEvent.getID() ) {
			
			// XXX beware, when the actionID and actionObj
			// are used, we have to deal with them here
			
			return true;

		} else return false;
	}
}
