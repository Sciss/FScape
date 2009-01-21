/*
 *  OpIcon.java
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
 */

package de.sciss.fscape.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

import de.sciss.fscape.op.Operator;

/**
 *  GUI element representing a spectral
 *	operator's icon.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 21-Jan-09
 */
public class OpIcon
extends IconicComponent
{
// -------- private Klassenvariablen --------

//	private static final String ibName	= "images" + File.separator + "op.gif";		// IconBitmap
	private static final int ibWidth	= 45;			// Breite der Icons
	private static final int ibHeight	= 45;			// Höhe der Icons	

	private static IconBitmap opib		= null;
	private static IconicComponent basicIcons[];

	private static DataFlavor flavors[] = null;	// alle erlaubten DataFlavors

// -------- public Variablen --------

	public static final String OBJ_NAME = "OpIcon";

	public static DataFlavor flavor	= null;				// DataFlavor representing this class

	public static final int ICON_WIDTH		= ibWidth;
	public static final int ICON_HEIGHT		= ibHeight;

	public static final int ID_INPUT		= 8;
	public static final int ID_OUTPUT		= 9;
	public static final int ID_FOLDER		= 10;
	public static final int ID_FLIPTIME		= 11;
	public static final int ID_FLIPFREQ		= 12;
	public static final int ID_SHARED		= 13;
	public static final int	ID_UNITOR		= 14;
	public static final int ID_SPLITTER		= 15;
	public static final int ID_MONO2STEREO	= 16;
	public static final int ID_SMEAR		= 17;

	public static final int STATE_NORMAL	= 0;
	public static final int STATE_ERROR		= 1;
	public static final int STATE_FRIENDLY	= 4;
	public static final int STATE_BYPASS	= 5;
	
	public static Color progColor	= new Color( 255, 160,   0 );	// Farbe für Progressindicatoren
	public static Color normalColor	= new Color( 152, 179, 132 );	// Farbe für normale Objekte
	public static Color selectColor	= new Color( 100, 115, 162 );	// Farbe für selektierte Objekte

// -------- private Variablen --------

	private static final int STATE_UNKNOWN	= -1;
	private static final int STATE_SELECTED	= 3;	// "plus"

	private Operator	op;
	private OpIconLabel	lab;
	private int			opFlags;

	private int state		= STATE_UNKNOWN;		// Status wie STATE_NORMAL, selektiert etc.
	private int statePlus	= 0;


// -------- public Methoden --------
	// public void addTo( Container c );
	// public void removeFrom( Container c );
	// public void setIconLocation( Point p );
	// public Point recallLocation();
	// public Operator getOperator();
	// public void setName( String iName );
	// public String getName();
	// public int setSelected( int state );
	// public int isSelected();

	/**
	 *	@param owner	Operator who own da icon
	 *	@param ID		Icon-ID
	 *	@param iName	Name für den Operator
	 */
	public OpIcon( Operator op, int ID, String iName )
	{
		super( getIconBitmap(), ID );
		this.op = op;

		// data flavors
		if( OpIcon.flavor == null ) {
			OpIcon.flavor = new DataFlavor( getClass(), "Operator Icon" );
		}

		lab		= new OpIconLabel( this, iName );
		opFlags	= ~op.getFlags();					// pretend everything changed
		operatorFlagsChanged( ~opFlags );			// invokes setSelected()

		setSize( getPreferredSize() );
		setLocation( 0, 0 );
		
		// Event handling
		enableEvents( AWTEvent.FOCUS_EVENT_MASK );
		enableEvents( AWTEvent.MOUSE_EVENT_MASK );
	}
	
	public OpIcon( Operator op )
	{
		this( op, -1, "Untitled" );
	}

	// initializer!!
	protected synchronized static IconBitmap getIconBitmap()
	{
		if( opib == null ) {
			final Image imgOp = Toolkit.getDefaultToolkit().getImage(
				OpIcon.class.getResource( "op.gif" ));
			opib = new IconBitmap( imgOp, ibWidth, ibHeight );
			basicIcons = new IconicComponent[ 5 ];
			for( int i = 0; i < basicIcons.length; i++ ) {
				basicIcons[ i ] = new IconicComponent( opib, i );
			}
		}
		return opib;
	}

	public void setLocation( int x, int y )
	{
		super.setLocation( x, y );
		lab.updateLocation();
	}
	
	/**
	 *	Ermittelt die vereinigten Ausmasse
	 *	aus Icon und JLabel
	 */
	public Rectangle getUnionBounds()
	{
		final Rectangle	iconBnd	= getBounds();
		final Rectangle	labBnd	= lab.getBounds();
		final Rectangle	union;
		
		union	= new Rectangle( Math.min( iconBnd.x, labBnd.x ), iconBnd.y,
								 Math.max( iconBnd.width, labBnd.width ),
								 iconBnd.height + labBnd.height );
		return union;
	}
	
	/**
	 *	Fuegt dieses Icon einem Container zu; STATT Container.add( OpIcon ) zu verwenden!!
	 */
	public void addTo( Container c )
	{
		c.add( this );
		c.add( lab );
	}

	/**
	 *	Entfernt dieses Icon von einem Container; STATT Container.remove( OpIcon ) zu verwenden!!
	 */
	public void removeFrom( Container c )
	{
		c.remove( this );
		c.remove( lab );
	}

	/**
	 *	Zugehoerigen Operator ermitteln
	 */
	public Operator getOperator()
	{
		return op;
	}

	/**
	 *	Zugehoeriges JLabel ermitteln
	 */
	public OpIconLabel getLabel()
	{
		return lab;
	}

	/**
	 *	JLabel des Icons festlegen
	 *
	 *	@param iName	JLabel
	 */
	public void setName( String iName )
	{
		lab.setName( iName );
	}
	
	/**
	 *	JLabel des Icons ermitteln
	 *
	 *	@return JLabel-String
	 */
	public String getName()
	{
		return lab.getName();
	}
	
	/**
	 *	@return returns OBJ_OPICON so you can identify it ('==' Operator) as the real OpIcon
	 */
	public String toString()
	{
		return OBJ_NAME;
	}
	
	public void update( Graphics g )
	{
		paint( g );			// don't clear rect previously; we're proud to be round
	}
	
//	public void paint( Graphics g )
	public void paintComponent( Graphics g )
	{
		basicIcons[ state + statePlus ].paint( g );
		opib.paint( g, ID, 0, 0 );

		if( !op.threadDead ) {		// wenn Operator "läuft", Progress-Indikator zeichnen
			paintProgress( g );
		}
	}

	/**
	 *	Zeichnet nur den run()-Fortschritt
	 *
	 *	@param	g	darf null sein
	 */
	public void paintProgress( Graphics g )
	{
		final int		prog	= (int) (op.getProgress() * 360);
		final Graphics	g2		= g != null ? g : getGraphics();

		g2.setColor( progColor );
		g2.drawArc( 1, 1, ICON_WIDTH - 2, ICON_HEIGHT - 2, 90, prog );
		g2.drawArc( 2, 2, ICON_WIDTH - 4, ICON_HEIGHT - 4, 90, prog );

		if( g == null ) {
			g2.dispose();
		}
	}

	/**
	 *	Status veraendern
	 *
	 *	@param state	neuer Status wie STATE_ERROR
	 *	@return	vorheriger Status
	 */
	public int setSelected( int newState )
	{
		final int lastState	= state;
		state				= newState;

		if( lastState != newState ) {
			repaint();
			lab.repaint();
		}

		return lastState;
	}
	
	public int isSelected()
	{
		return state;
	}

	/**
	 *	Icon mitteilen, dass der Operator neue Flags besitzt
	 *	(bsp. Alias, Bypass)
	 */
	public void operatorFlagsChanged( int newFlags )
	{
		// Bypass-Type changed?
		if( ((newFlags ^ opFlags) & Operator.FLAGS_BYPASS) != 0 ) {

			setSelected( ((newFlags & Operator.FLAGS_BYPASS) == 0) ? STATE_NORMAL : STATE_BYPASS );
		}
		opFlags = newFlags;
		lab.operatorFlagsChanged( newFlags );
	}

//	public boolean isFocusTraversable()
//	{
//		return true;
//	}

// -------- Dragable Methoden --------

	/**
	 *	Zeichnet ein OpIcon-Schema mit den angegebenen Koordinaten
	 */
	public void paintScheme( Graphics g, int x, int y, boolean mode )
	{
		final int			top	= y - (OpIcon.ICON_HEIGHT>>1);
		final Dimension		dim	= lab.getSize();
	
		g.drawOval( x - (OpIcon.ICON_WIDTH>>1), top, OpIcon.ICON_WIDTH - 1, OpIcon.ICON_HEIGHT - 1 );
		g.drawRect( x - (dim.width>>1), top + OpIcon.ICON_HEIGHT, dim.width - 1, dim.height - 1 );
	}

// -------- Transferable Methoden --------

	public DataFlavor[] getTransferDataFlavors()
	{
		if( flavors == null ) {
			final DataFlavor iconFlavors[] = super.getTransferDataFlavors();
			flavors			= new DataFlavor[ iconFlavors.length + 1 ];
			flavors[ 0 ]	= OpIcon.flavor;
			for( int i = 0; i < iconFlavors.length; i++ ) {
				flavors[ i + 1 ] = iconFlavors[ i ];
			}
		}
		return flavors;
	}

	public boolean isDataFlavorSupported( DataFlavor flavor )
	{
		final DataFlavor flavors[] = getTransferDataFlavors();
		for( int i = 0; i < flavors.length; i++ ) {
			if( flavors[ i ].equals( flavor )) return true;
		}
		return false;
	}
	
	public Object getTransferData( DataFlavor flavor )
	throws UnsupportedFlavorException, IOException
	{
		if( flavor.equals( OpIcon.flavor )) {
			return this;
		} else if( flavor.equals( DataFlavor.stringFlavor )) {
			return getName();
		} else {
			return super.getTransferData( flavor );
		}
	}

// -------- private Methoden --------

	protected void processMouseEvent( MouseEvent e )
	{
		if( e.getID() == MouseEvent.MOUSE_PRESSED ) {
			requestFocus();
		}
		super.processMouseEvent( e );
	}
	
	protected void processFocusEvent( FocusEvent e )
	{
		int oldStatePlus = statePlus;
	
		if( e.getID() == FocusEvent.FOCUS_GAINED ) {
			lab.setFocus( true );
			statePlus = (state == STATE_NORMAL) ? STATE_SELECTED : 0;
		} else if( e.getID() == FocusEvent.FOCUS_LOST ) {
			lab.setFocus( false );
			statePlus = 0;
		}
		
		if( statePlus != oldStatePlus ) repaint();
		
		super.processFocusEvent( e );
	}
}
// class OpIcon
