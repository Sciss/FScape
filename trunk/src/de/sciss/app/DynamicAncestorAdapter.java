/*
 *  DynamicAncestorAdapter.java
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
 *		20-May-05	created from de.sciss.meloncillo.gui.DynamicAncestorAdapter
 *		21-Sep-06	added remove(), isListening(), getComponent()
 */

package de.sciss.app;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;

import de.sciss.app.AncestorAdapter;

/**
 *  This class can be added as an <code>AncestorListener</code>
 *  and will call the passed <code>DynamicListening</code> object
 *  when the Component becomes visible or invisible in the
 *  sense that it's ancestor window is shown or hidden.
 *  <p>
 *  <strong>It's crucial that the <code>addTo</code> method is
 *  used to register the listener!</strong>
 *  <p>
 *  <code>Surface</code> is an example of the use of a
 *  <code>DynamicAncestorAdapter</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 20-May-05
 *
 *  @see		DynamicListening
 */
public class DynamicAncestorAdapter
extends AncestorAdapter
{
    private final DynamicListening	dynL;
	private final WindowListener	winL;
	private final ComponentListener	cmpL;
	private Window					win			= null;
	private JComponent				cmp			= null;
	private boolean					listening   = false;
    
	/**
	 *  Constructs a new <code>DynamicAncestorAdapter</code>
	 *  which will inform the <code>DynamicListening</code>
	 *  about changes in visibility of the ancestor window
	 *  of the component to which this adapter is added.
	 *
	 *  @param  listener	a <code>DynamicListening</code>
	 *						whose <code>startListening</code>
	 *						method is called when this adapter's
	 *						host component's ancestor is shown
	 *						or added to another component. the
	 *						listener's <code>stopListening</code>
	 *						method is called likewise when
	 *						this adapter's host component's ancestor
	 *						is hidden or removed from its parent.
	 */
    public DynamicAncestorAdapter( DynamicListening listener )
    {
        this.dynL = listener;
		winL = new WindowAdapter() {
			public void windowOpened( WindowEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) {
					System.err.println( "windowOpened() : " + e.getWindow().getClass().getName() );
				}
				if( !listening ) startListening();
			}

			public void windowClosed( WindowEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) {
					System.err.println( "windowClosed() : " + e.getWindow().getClass().getName() );
				}
				if( listening ) stopListening();
			}
		};
		cmpL = new ComponentAdapter() {
			public void componentShown( ComponentEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) {
					System.err.println( "componentShown() : " + e.getComponent().getClass().getName() );
				}
				if( !listening ) startListening();
			}

			public void componentHidden( ComponentEvent e )
			{
				if( EventManager.DEBUG_EVENTS ) {
					System.err.println( "componentHidden() : " + e.getComponent().getClass().getName() );
				}
				if( listening ) stopListening();
			}
		};
    }

	/**
	 *  Adds this adapter to a <code>JComponent</code>.
	 *  <strong>Use this method instead of calling
	 *  <code>cmp.addAncestorListener(...)</code></strong>
	 *  because this method will automatically detect
	 *  the component's window. This is crucial for
	 *  <code>JRootPane</code> components, because they are already
	 *  attached to a window when you register the
	 *  listener.
	 *
	 *  @param  cmp		the <code>JComponent</code> who will be tracked for
	 *					ancestor changes.
	 *  @see	javax.swing.JComponent#addAncestorListener( AncestorListener )
	 */
	public void addTo( JComponent cmp )
	{
		if( this.cmp != null ) throw new IllegalStateException( "Already added" );
	
		this.cmp = cmp;
		cmp.addAncestorListener( this );
		learnWindow( cmp.getTopLevelAncestor() );
	}
	
	public void remove()
	{
		if( cmp == null ) throw new IllegalStateException( "Was not added" );
		
		cmp.removeAncestorListener( this );
		forgetWindow();
		cmp = null;
	}
	
	public JComponent getComponent()
	{
		return cmp;
	}

	public boolean isListening()
	{
		return listening;
	}

	/**
	 *  Called when the tracked component or one of
	 *  its ancestors gets added in the container hierarchy.
	 *  This method checks to see if a change in the
	 *  component's top level window occured, and if
	 *  so, re-registers window and component listeners
	 *  on that window. Also if the window is visible
	 *  and the dynamic listener is not yet listening,
	 *  its <code>startListening</code> method is invoked.
	 */
	public void ancestorAdded( AncestorEvent e )
	{
		if( EventManager.DEBUG_EVENTS ) {
			System.err.println( "ancestorAdded.   cmp = "+(e.getComponent() != null ? e.getComponent().getClass().getName() + (e.getComponent() instanceof PreferenceEntrySync ? " / key " + ((PreferenceEntrySync) e.getComponent()).getPreferenceKey() : "")  : null)+
							  "\n                 anc = "+(e.getAncestor() != null ? e.getAncestor().getClass().getName() : null)+
							  "\n                 par = "+(e.getAncestorParent() != null ? e.getAncestorParent().getClass().getName() : null) );
		}
		Container c = e.getComponent().getTopLevelAncestor();
		if( c != win ) {
			forgetWindow();
			learnWindow( c );
		}
	}
	
	/**
	 *  Called when the tracked component or one of
	 *  its ancestors gets removed in the container hierarchy.
	 *  If the dynamic listener was started,
	 *  its <code>stopListening</code> method is invoked.
	 */
	public void ancestorRemoved( AncestorEvent e )
	{
		if( EventManager.DEBUG_EVENTS ) {
			System.err.println( "ancestorRemoved. cmp = "+(e.getComponent() != null ? e.getComponent().getClass().getName() + (e.getComponent() instanceof PreferenceEntrySync ? " / key " + ((PreferenceEntrySync) e.getComponent()).getPreferenceKey() : "")  : null)+
							  "\n                 anc = "+(e.getAncestor() != null ? e.getAncestor().getClass().getName() : null)+
							  "\n                 par = "+(e.getAncestorParent() != null ? e.getAncestorParent().getClass().getName() : null) );
		}
		forgetWindow();
	}
	
	private void forgetWindow()
	{
		if( win != null ) {
			win.removeWindowListener( winL );
			win.removeComponentListener( cmpL );
			if( EventManager.DEBUG_EVENTS ) {
				System.err.println( "DynamicAncestorAdapter removed WindowListener : "+
					win.getClass().getName() );
			}
			win = null;
			if( listening ) stopListening();
		}
	}

	private void learnWindow( Container c )
	{
		if( c != null && c instanceof Window ) {
			win = (Window) c;
			win.addWindowListener( winL );
			win.addComponentListener( cmpL );
			if( EventManager.DEBUG_EVENTS ) {
				System.err.println( "DynamicAncestorAdapter added WindowListener : "+
					win.getClass().getName() );
			}
			if( !listening && win.isShowing() ) startListening();
		}
	}

	private void startListening()
	{
		dynL.startListening();
		listening = true;
	}

	private void stopListening()
	{
		dynL.stopListening();
		listening = false;
	}
}