/*
 *  ToolIcon.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	modernized
 */

package de.sciss.fscape.gui;

import java.awt.AWTEvent;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *	Icon subclass for tool icons such
 *	as 'add' or 'delete preset'.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 21-Jan-09
 */
public class ToolIcon
extends IconicComponent
{
// -------- private Variablen --------

	protected int state;	// Status wie normal, selektiert etc.
	private int basicID;	// ohne Status Addition

	protected boolean clicked = false;	// fuer den MouseEvent Listener

// -------- public Variablen --------

	public static final int ID_ADDPRESET	= 0;
	public static final int ID_DELPRESET	= 1;
	public static final int ID_CHOOSEFILE	= 2;
	public static final int ID_START		= 3;
	public static final int ID_PAUSE		= 4;
	public static final int ID_STOP			= 5;
	public static final int ID_CHOOSEFONT	= 6;
	public static final int ID_ZOOMOUT		= 7;
	public static final int ID_ZOOMIN		= 8;
	public static final int ID_EDITENV		= 10;
	public static final int ID_ADDSAMPLE	= 11;
	public static final int ID_DELSAMPLE	= 12;

	// DataWheel
	protected static final	int	ID_WHEEL		= 9;

	protected static final int STATE_NORMAL		= 0;
	protected static final int STATE_SELECTED	= 1;
	protected static final int STATE_DISABLED	= 2;

	private static final int STATE_FACTOR		= 3;	// private: multiply IDs above widdis one

	protected static final int ibWidth			= 32;			// Breite der Icons
	protected static final int ibHeight			= 32;			// Hoehe der Icons
	protected static IconBitmap toolib;

// -------- private Klassenvariablen und -konstruktor --------

//	private static final String ibName	= "images" + File.separator + "tools.png";	// IconBitmap

	static	// Icon-Bitmap laden
	{
		final Image imgTools = Toolkit.getDefaultToolkit().getImage(
		    ToolIcon.class.getResource( "tools.png" ));
		toolib = new IconBitmap( imgTools, ibWidth, ibHeight );
	}

// -------- public Methoden --------

	/**
	 *	@param	ID		Icon-ID
	 */
	public ToolIcon( int ID, String toolTip )
	{
		super( toolib, ID );
		setToolTipText( toolTip );
		
		// Event handling
		enableEvents( AWTEvent.MOUSE_EVENT_MASK );
		
		addPropertyChangeListener( "enabled", new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent e )
			{
				setSelected( isEnabled() ? STATE_NORMAL : STATE_DISABLED );
			}
		});
	}

	/**
	 *	ID des Icons setzen
	 *
	 *	@param ID	Icon-ID (nur logisch!)
	 */
	public void setID( int ID )	// override Icon...
	{
		this.basicID = ID;
		super.setID( ID * STATE_FACTOR + state );
		repaint();
	}
	
	/**
	 *	ID des Icons ermitteln
	 *
	 *	@return	Icon-ID (nur logisch!)
	 */
	public int getID()		// override Icon...
	{
		return basicID;
	}

//	public boolean isFocusTraversable()
//	{
//		return false;
//	}

// -------- private Methoden --------

	protected void setSelected( int state )
	{
		super.setID( basicID * STATE_FACTOR + state );
		this.state = state;
		repaint();
	}

	// won't be invoked when gadget is disabled XXX wrong!
	protected void processMouseEvent( MouseEvent e )
	{
		if( isEnabled() ) {
			switch( e.getID() ) {
			case MouseEvent.MOUSE_PRESSED:
				setSelected( STATE_SELECTED );
				clicked = true;
				break;
			case MouseEvent.MOUSE_ENTERED:
				if( clicked ) setSelected( STATE_SELECTED );
				break;
			case MouseEvent.MOUSE_RELEASED:
				setSelected( STATE_NORMAL );
				clicked = false;
				break;
			case MouseEvent.MOUSE_EXITED:
				if( clicked ) setSelected( STATE_NORMAL );
				break;
			default:
				break;
			}
		}
		super.processMouseEvent( e );
	}
	
	/*
	 *	Fuer Subclassen, die obige Process-Routine ueberschreiben
	 */
	protected void passMouseEvent( MouseEvent e )
	{
		super.processMouseEvent( e );
	}
}
// class ToolIcon
