/*
 *  DocumentHandler.java
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
 *		21-May-05	created
 *		02-Aug-05	infected with sense
 */

package de.sciss.app;

/**
 *  The <code>DocumentHandler</code> interface
 *	describes an object that manages registration
 *	and unregistration of documents in a
 *	multi-document-environment.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.13, 02-Aug-05
 */
public interface DocumentHandler
{
	/**
	 *	Determines whether the underlying application
	 *	handles more than a single open document or not.
	 *
	 *	@return	<code>true</code> if the application
	 *			allows more than one document to be opened
	 *			at a time. <code>false</code> for a
	 *			single-document-application.
	 */
	public boolean isMultiDocumentApplication();

	/**
	 *	Returns the number of open documents
	 *
	 *	@return	the number of open documents
	 */
	public int getDocumentCount();
	
	/**
	 *	Returns one of the open documents.
	 *
	 *	@param	idx		the index of the document, ranging
	 *					from <code>0</code> to <code>getDocumentCount() - 1</code>.
	 *	@return			the requested document
	 *
	 *	@throws	IndexOutOfBoundsException	if <code>idx</code> is invalid
	 */
	public Document getDocument( int idx );

	/**
	 *	Adds a new document to the list of open documents.
	 *	A <code>DocumentEvent</code> is generated and dispatched to all
	 *	registered listeners. The caller is responsible for calling
	 *	<code>setActiveDocument</code> separately, if desired.
	 *
	 *	@param	source	the source of the dispatched <code>DocumentEvent</code>,
	 *					or <code>null</code> to not dispatch an event.
	 *	@param	doc		the document to add.
	 *
	 *	@throws UnsupportedOperationException	if the caller tries to add
	 *											a document when there is already one
	 *											and <code>isMultiDocumentApplication</code>
	 *											returns <code>false</code>.
	 *	@throws	IllegalArgumentException		if the document has been added before
	 *
	 *	@see	#isMultiDocumentApplication()
	 */
	public void addDocument( Object source, Document doc );

	/**
	 *	Removes a document from the list of open documents.
	 *	A <code>DocumentEvent</code> is generated and dispatched to all
	 *	registered listeners. This method will first call
	 *	<code>setActiveDocument( null )</code> if the removed document
	 *	was the active document. After removing the document, this method
	 *	will call <code>doc.dispose()</code> to free resources occupied
	 *	by the document.
	 *
	 *	@param	source	the source of the dispatched <code>DocumentEvent</code>,
	 *					or <code>null</code> to not dispatch an event.
	 *	@param	doc		the document to remove.
	 *
	 *	@throws	IllegalArgumentException	if the document
	 *										was not in the list of known documents
	 */
	public void removeDocument( Object source, Document doc );

	/**
	 *	Makes a document the active one. The active document is the
	 *	one whose data representation window has the focus, are more generally
	 *	speaking, is the one who is affected by editing operations.
	 *	A <code>DocumentEvent</code> is generated and dispatched to all
	 *	registered listeners.
	 *
	 *	@param	source	the source of the dispatched <code>DocumentEvent</code>,
	 *					or <code>null</code> to not dispatch an event.
	 *	@param	doc		the document to make the active one, or <code>null</code>
	 *					to indicate that no document is active.
	 *
	 *	@throws	IllegalArgumentException	if the document
	 *										is not in the list of known documents
	 *
	 *	@see	#getActiveDocument()
	 */
	public void setActiveDocument( Object source, Document doc );

	/**
	 *	Determines the currently active document. The active document is the
	 *	one whose data representation window has the focus, are more generally
	 *	speaking, is the one who is affected by editing operations.
	 *
	 *	@return	the active document or <code>null</code>, if there is none
	 *			(this is the case when all documents have been removed)
	 *
	 *	@see	#setActiveDocument( Object, Document )
	 */
	public Document getActiveDocument();
	
	/**
	 *	Registeres a listener which will be informed about
	 *	document handler actions such as adding or removing a document.
	 *
	 *	@param	l	the listener to add
	 *
	 *	@see	#removeDocumentListener( DocumentListener )
	 */
	public void addDocumentListener( DocumentListener l );

	/**
	 *	Unregisteres a listener who wishes to not be notified any more
	 *	about document handler actions such as adding or removing a document.
	 *
	 *	@param	l	the listener to remove
	 *
	 *	@see	#addDocumentListener( DocumentListener )
	 */
	public void removeDocumentListener( DocumentListener l );
}
