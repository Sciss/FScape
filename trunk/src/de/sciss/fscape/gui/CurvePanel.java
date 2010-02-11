/*
 *  CurvePanel.java
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
 *		21-May-05	improved ; extends JPanel instead of Panel
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import de.sciss.fscape.util.*;

/**
 *  Panel which hosts breakpoint
 *	editors for parameter dynamization.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class CurvePanel
extends JPanel
{
// -------- public Variablen --------

	/**
	 *	ActionCommands, die ein registrierter (AddActionListener())
	 *	ActionListener empfaengt. Kann mit == verglichen werden,
	 *	equals() nicht noetig
	 */
	public static final String	ACTION_POINTSELECTED	= "PointSelect";
	public static final String	ACTION_POINTDESELECTED	= "PointDeselect";
	public static final String	ACTION_POINTCHANGED		= "PointMoved";
	public static final String	ACTION_SPACECHANGED		= "SpaceChange";

// -------- private Variablen --------

	// gerade angewaehlter Punkt; wenn keiner ausgewaehlt, muss
	// er auf dummyPoint gesetzt werden, damit keine Null-Pointer Fehler
	// auftreten koennen!
	protected CurvePoint currentCurvePoint;
	protected CurvePoint dummyPoint;

	protected Cursor lastCursor;					// Cursor previously used to dnd or edit-op

	// this is all for drag+drop
	protected CurvePoint	dragSource	= null;
	protected DoublePoint	dragLeft, dragRight;	// Punkt links + rechts neben dem gezogenen
	protected boolean		dragState	= false;	// true for drag+drop
	protected int			dragType	= DRAG_NONE;
	protected int			dragLastX;				// last coordinates
	protected int			dragLastY;				// last coordinates
	protected DoublePoint	dragPoint;

	private static final int DRAG_MOVE	=	0;	// drag object
	private	static final int DRAG_NONE	=  -1;

	// fuer CurvePoint
	protected static final int CP_WIDTH		= 8;
	protected static final int CP_HEIGHT	= 8;

	protected static final int CP_STATE_NORMAL		= 0;
	protected static final int CP_STATE_SELECTED	= 3;
	protected static final int CP_STATE_UNKNOWN		= -1;

	protected Curve		curve;
	protected Dimension	dim;		// Panel-Size; vom Component-Listener geupdated

	// dieser Button wird nicht angezeigt, sondern dient nur als
	// Institution zum Verwalten der ActionListener und fuer
	// den Event-Dispatch!
	protected Button		actionComponent;

	private final MouseInputAdapter mia;

// -------- public Methoden --------

	public CurvePanel()
	{
		super( null );
		
		actionComponent		= new Button();
		dummyPoint			= new CurvePoint();
		currentCurvePoint	= dummyPoint;
		curve				= new Curve( Constants.spaces[ Constants.emptySpace ],
										 Constants.spaces[ Constants.emptySpace ]);
		
		setBackground( Color.white );
		setOpaque( true );
		dim	= new Dimension( 64, 64 );
		setMinimumSize( dim );
		setPreferredSize( new Dimension( 128, 128 ));
		setSize( dim );

		addComponentListener( new ComponentAdapter() {
			public void componentResized( ComponentEvent e )
			{
				dim = getSize();
				recalcScreenPoints();
				repaint();
			}
		});
		
		final CurvePanel enc_this	= this;

		mia = new MouseInputAdapter() {
			public void mouseClicked( MouseEvent e )
			{
				DoublePoint pt;
				int			index;
				CurvePoint	cp;
			
				if( !isEnabled() ) return;	// not while inactive

				if( e.getSource() == enc_this ) {			//-------- Panel hit -------------------
					if( (e.getClickCount() == 2) && !e.isAltDown() ) {
					
						pt	= screenToParamSpace( e.getX(), e.getY() );
						cp	= new CurvePoint();
						addPoint( cp, pt.x, pt.y );
					}
				} else if( e.isAltDown() ) {
					index = getComponentIndex( (Component) e.getSource() );
					if( (curve.getPoint( index - 1 ) != null) &&
						(curve.getPoint( index + 1 ) != null) ) {	// Endpunkte nicht lšschen
						
						removePoint( (CurvePoint) e.getSource() );
					}
				}
			}

			public void mousePressed( MouseEvent e )
			{
				Point			cpLoc;
				ActionEvent		parentE;

				if( !isEnabled() ) return;	// not while running the operators

				// alten CurvePoint deselektieren
				currentCurvePoint.setSelected( CP_STATE_NORMAL );

				if( e.getSource() == enc_this ) {				//-------- Panel hit -------------------

					if( currentCurvePoint != dummyPoint ) {
						currentCurvePoint = dummyPoint;
						// Listener benachrichtigen
						parentE = new ActionEvent( enc_this, ActionEvent.ACTION_PERFORMED,
												   ACTION_POINTDESELECTED );
						actionComponent.dispatchEvent( parentE );
					}

					if( e.isControlDown() ) {	// PopUp-MenŸ
						// XXX
					}

				} else {									//-------- CurvePoint hit ------------------

					currentCurvePoint	= (CurvePoint) e.getSource();
					cpLoc				= currentCurvePoint.getLocation();

					// neuen CurvePoint selektieren
					currentCurvePoint.setSelected( CP_STATE_SELECTED );

					if( e.isControlDown() ) {		// PopUp-MenŸ
						// XXX

					} else if( !e.isAltDown() ) {	// prepare Drag
						dragLastX	= e.getX() + cpLoc.x;
						dragLastY	= e.getY() + cpLoc.y;
						dragPoint	= screenToParamSpace( cpLoc.x + (CP_WIDTH>>1), cpLoc.y + (CP_HEIGHT>>1) );
						dragType	= DRAG_MOVE;
					}
				}	
			}
			
			public void mouseReleased( MouseEvent e )
			{
				Graphics	g;
				ActionEvent	parentE;
				
				if( !isEnabled() ) return;	// not while running the operators

				if( dragState ) {

					// clear rubberband
					g = getGraphics();
					g.setXORMode( getBackground() );
					g.setColor( Color.black );
					g.drawRect( dragLastX - (CP_WIDTH>>1), dragLastY - (CP_HEIGHT>>1), CP_WIDTH-1, CP_HEIGHT-1 );

					movePoint( dragSource, dragPoint.x, dragPoint.y );

					dragState = false;
					setCursor( lastCursor );
					g.dispose();			

				} else {

					// Listener benachrichtigen
					if( (currentCurvePoint != dummyPoint) && !e.isAltDown() ) {
						parentE = new ActionEvent( enc_this, ActionEvent.ACTION_PERFORMED,
												   ACTION_POINTSELECTED );
						actionComponent.dispatchEvent( parentE );
					}
				}
				
				dragType	= DRAG_NONE;
				dragSource	= null;
			}
			
			public void mouseDragged( MouseEvent e )
			{
				Graphics	g;
				Point		cpLoc;
				int			currentX;
				int			currentY;
				int			index;
				DoublePoint	currentPt;
				double		dragPtX, dragPtY;

				if( !isEnabled() || (dragType == DRAG_NONE) ) return;

				dragSource	= (CurvePoint) e.getSource();
				cpLoc		= dragSource.getLocation();
				currentX	= e.getX() + cpLoc.x;
				currentY	= e.getY() + cpLoc.y;

				g = getGraphics();
				g.setXORMode( getBackground() );
				g.setColor( Color.black );

				if( !dragState ) {		// check if distance is ok to start drag
					if( (currentX-dragLastX)*(currentX-dragLastX) +
						(currentY-dragLastY)*(currentY-dragLastY) <= 16 ) return;	// ...not ok

					synchronized( curve ) {
						index		= getComponentIndex( dragSource );
						dragLeft	= curve.getPoint( index - 1 );
						dragRight	= curve.getPoint( index + 1 );
					}
					lastCursor = getCursor();
					setCursor( new Cursor( Cursor.CROSSHAIR_CURSOR ));
					dragState = true;

				} else {
					// clear rubberband
					g.drawRect( dragLastX - (CP_WIDTH>>1), dragLastY - (CP_HEIGHT>>1), CP_WIDTH-1, CP_HEIGHT-1 );
				}

				// Werte korrigieren
				currentPt	= screenToParamSpace( currentX, currentY );
				dragPtX		= currentPt.x;
				dragPtY		= currentPt.y;
				if( (dragLeft != null) && (dragRight != null) ) {
					if( dragPtX <= dragLeft.x ) {
						dragPtX	= dragLeft.x + curve.hSpace.inc;
					}
					if( dragPtX >= dragRight.x ) {
						dragPtX	= dragRight.x - curve.hSpace.inc;
					}
				} else {
					dragPtX = dragPoint.x;		// Endpunkte nicht horizontal verschieben
				}

				cpLoc		= paramSpaceToScreen( dragPtX, dragPtY );
				dragLastX	= cpLoc.x;
				dragLastY	= cpLoc.y;
				dragPoint	= new DoublePoint( dragPtX, dragPtY );

				// current one
				g.drawRect( dragLastX - (CP_WIDTH>>1), dragLastY - (CP_HEIGHT>>1), CP_WIDTH-1, CP_HEIGHT-1 );

				g.dispose();
			}
		};

		addMouseListener( mia );
		addMouseMotionListener( mia );

		addPropertyChangeListener( "enabled", new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent e )
			{
				ActionEvent	parentE;
				
				if( currentCurvePoint != dummyPoint ) {
					currentCurvePoint.setSelected( CP_STATE_NORMAL );
					currentCurvePoint = dummyPoint;
					// Listener benachrichtigen
					parentE = new ActionEvent( enc_this, ActionEvent.ACTION_PERFORMED,
											   ACTION_POINTDESELECTED );
					actionComponent.dispatchEvent( parentE );
				}

				setBackground( isEnabled() ? Color.white : null );
			}
		});

		setCurve( curve );
		
		setFocusable( false );
	}

	/**
	 *	Kurve zuweisen
	 */
	public void setCurve( Curve curve )
	{
		ActionEvent	parentE;
		CurvePoint	cp;

		clear();
	
		synchronized( this.curve ) {
			this.curve			= (Curve) curve.clone();
			currentCurvePoint	= dummyPoint;

			for( int i = curve.size(); i > 0; i-- ) {
				cp = new CurvePoint();
				cp.addMouseListener( mia );
				cp.addMouseMotionListener( mia );
				add( cp );
			}
		}
		recalcScreenPoints();
		repaint();
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_POINTDESELECTED );
		actionComponent.dispatchEvent( parentE );
	}

	/**
	 *	Kurve besorgen
	 */
	public Curve getCurve()
	{
		synchronized( curve ) {
			return (Curve) curve.clone();
		}
	}
	
	/**
	 *	Alle Punkte entfernen
	 */
	public void clear()
	{
		ActionEvent	parentE;
		CurvePoint	cp;

		synchronized( curve ) {
			currentCurvePoint	= dummyPoint;
//			removeAll();

			while( curve.size() > 0 ) {
				curve.removePoint( 0 );
				cp = (CurvePoint) getComponent( 0 );
				remove( cp );
				cp.removeMouseListener( mia );
				cp.removeMouseMotionListener( mia );
			}
		}
		repaint();
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_POINTDESELECTED );
		actionComponent.dispatchEvent( parentE );
	}

	/**
	 *	Dimensionen aendern
	 *	erzeugt eine neue Kurve mit entsprechend skalierten Punkten
	 *
	 *	@param	hSpace	neuer horizontaler Space, darf null sein
	 *					(= keine Aenderung)
	 *	@param	vSpace	neuer vertikaler Space, darf null sein
	 *					(= keine Aenderung)
	 */
	public void rescale( ParamSpace hSpace, ParamSpace vSpace )
	{
		ActionEvent	parentE;
		Curve		newCurve;
		DoublePoint	pt ,pt2;
		double		hScaling, vScaling;
		Vector		revisit;

		int			index;
		double		dIndex;
		int			ceil;

		synchronized( curve ) {

			if( hSpace == null)	hSpace = curve.hSpace;
			if( vSpace == null)	vSpace = curve.vSpace;

			// Scalierung berechnen
			hScaling	= (hSpace.max - hSpace.min) / (curve.hSpace.max - curve.hSpace.min);
			vScaling	= (vSpace.max - vSpace.min) / (curve.vSpace.max - curve.vSpace.min);

			// Punkte skalieren
			newCurve	= new Curve( hSpace, vSpace, curve.type );
			revisit		= new Vector();
			
			for( int i = curve.size() - 1; i >= 0; i-- ) {

				pt		= curve.getPoint( i );
				pt		= new DoublePoint( (pt.x - curve.hSpace.min) * hScaling + hSpace.min,
										   (pt.y - curve.vSpace.min) * vScaling + vSpace.min );
				
				index	= newCurve.addPoint( pt );
										     
				if( index < 0 ) {	// passt nicht, erst mal merken
					revisit.addElement( pt );
				}
			}

			for( int i = revisit.size() - 1; i >= 0; i-- ) {

				pt		= (DoublePoint) revisit.elementAt( i );
				dIndex	= newCurve.indexOf( pt.x );
				if( (dIndex >= 0.0) && (dIndex <= (double) (curve.size() - 1)) ) {
				
					ceil  = (int) Math.ceil( dIndex );
					pt2	  = newCurve.getPoint( ceil );
					if( pt2 != null ) {
					
						// nochmal mit leicht veraenderter Position versuchen
						index = newCurve.addPoint( pt2.x - newCurve.hSpace.inc, pt.y );
						if( index < 0 ) {
							remove( 0 );	// ueberschuessigen CurvePoint entfernen
						}
					}
				}
			}
			this.curve = newCurve;
		}

		recalcScreenPoints();
		repaint();
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_SPACECHANGED );
		actionComponent.dispatchEvent( parentE );
		if( currentCurvePoint != dummyPoint ) {
			parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
									   ACTION_POINTCHANGED );
			actionComponent.dispatchEvent( parentE );
		}
	}
	
	/**
	 *	Aendert die Werte fuer X-Max / Y-Max
	 *
	 *	@param	maxX	neues X-Maximum; Double.NEGATIVE_INFINITY
	 *					= keine Aenderung
	 *	@param	maxY	neues Y-Maximum; Double.NEGATIVE_INFINITY
	 *					= keine Aenderung
	 */
/*	public void rescale( double maxX, double maxY )
	{
		ActionEvent	parentE;
		Curve		newCurve;
		DoublePoint	pt;
		double		hScaling, vScaling;
		ParamSpace	newHSpace, newVSpace;

		int			index;

		synchronized( curve ) {

			if( maxX == Double.NEGATIVE_INFINITY ) {
				maxX = curve.hSpace.max;
			}
			if( maxY == Double.NEGATIVE_INFINITY ) {
				maxY = curve.vSpace.max;
			}

			// neue Spaces
			newHSpace	= new ParamSpace( curve.hSpace.min, maxX, curve.hSpace.inc, curve.hSpace.unit );
			newVSpace	= new ParamSpace( curve.vSpace.min, maxY, curve.vSpace.inc, curve.vSpace.unit );

			// Scalierung berechnen
			hScaling	= (newHSpace.max - newHSpace.min) / (curve.hSpace.max - curve.hSpace.min);
			vScaling	= (newVSpace.max - newVSpace.min) / (curve.vSpace.max - curve.vSpace.min);

			// Punkte skalieren
			newCurve = new Curve( newHSpace, newVSpace, curve.type );
			for( int i = curve.size() - 1; i >= 0; i-- ) 
			{
				pt		= curve.getPoint( i );
				
				index	= newCurve.addPoint( (pt.x - curve.hSpace.min) * hScaling + newHSpace.min,
										     (pt.y - curve.vSpace.min) * vScaling + newVSpace.min );
				if( index < 0 ) {	// ueberfluessiger Punkt
					remove( i );
				}
			}
			
			this.curve = newCurve;
		}

		recalcScreenPoints();
		repaint();
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_SPACECHANGED );
		actionComponent.dispatchEvent( parentE );
		if( currentCurvePoint != dummyPoint ) {
			parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
									   ACTION_POINTCHANGED );
			actionComponent.dispatchEvent( parentE );
		}
	}
*/
	/**
	 *	Derzeitigen horizontalen Space ermitteln
	 */
	public ParamSpace getHSpace()
	{
		synchronized( curve ) {
			return curve.hSpace;
		}
	}

	/**
	 *	Derzeitigen vertikalen Space ermitteln
	 */
	public ParamSpace getVSpace()
	{
		synchronized( curve ) {
			return curve.vSpace;
		}
	}

	/**
	 *	Registriert einen ActionListener;
	 *	Action-Events kommen, wenn sich der Wert des ParamFieldes aendert
	 */
	public void addActionListener( ActionListener list )
	{
		actionComponent.addActionListener( list );
	}

	/**
	 *	Entfernt einen ActionListener
	 */
	public void removeActionListener( ActionListener list )
	{
		actionComponent.removeActionListener( list );
	}

	/**
	 *	(Space)Koordinaten des aktuellen Punktes besorgen
	 *
	 *	@return	null, wenn Fehler oder kein Punkt angewaehlt
	 */
	public DoublePoint getPoint()
	{
		CurvePoint	cp		= currentCurvePoint;
		int			index	= getComponentIndex( cp );
		DoublePoint	pt;

		if( (cp == dummyPoint) || (index < 0) ) return null;

		synchronized( curve ) {
			pt = curve.getPoint( index );
			return pt;
		}	
	}
	
	/**
	 *	(Space)Koordinaten des aktuellen Punktes setzen
	 *
	 *	@return	false, wenn kein Punkt angewaehlt ist
	 */
	public boolean setPoint( DoublePoint pt )
	{
		CurvePoint	cp = currentCurvePoint;
		if( cp == dummyPoint ) return false;

		movePoint( cp, pt.x, pt.y );
		return true;
	}
	
	/**
	 *	Punkt auf das Panel legen
	 *
	 *	@return	null bei Fehler (z.B. schon Punkt am selben Ort vorhanden)
	 */
	public CurvePoint addPoint( CurvePoint cp, double x, double y )
	{
		ActionEvent	parentE;
		int			index;
		Point		loc;
		
		cp.addMouseListener( mia );
		cp.addMouseMotionListener( mia );

		loc = paramSpaceToScreen( x, y );
		cp.setLocation( loc.x - (CP_WIDTH>>1), loc.y - (CP_HEIGHT>>1) );		

		synchronized( curve ) {

			index = curve.addPoint( x, y );
			if( index >= 0 ) {
				add( cp, index );
				repaint();

				// umschalten
				currentCurvePoint.setSelected( CP_STATE_NORMAL );
				currentCurvePoint = cp;
				currentCurvePoint.setSelected( CP_STATE_SELECTED );
				// Listener benachrichtigen
				parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
										   ACTION_POINTSELECTED );
				actionComponent.dispatchEvent( parentE );

				return cp;
			}
		 	else return null;
		 }
	}

	/**
	 *	Punkt vom Panel entfernen
	 */
	public void removePoint( CurvePoint cp )
	{
		ActionEvent	parentE;
		int			index = getComponentIndex( cp );

		synchronized( curve ) {
			if( index >= 0 ) {
				remove( cp );
				curve.removePoint( index );
				repaint();
			}
		}
		currentCurvePoint.setSelected( CP_STATE_NORMAL );
		currentCurvePoint = dummyPoint;
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_POINTDESELECTED );
		actionComponent.dispatchEvent( parentE );

		cp.removeMouseListener( mia );
		cp.removeMouseMotionListener( mia );
	}
 
	/**
	 *	Punkt verschieben
	 */
	public void movePoint( CurvePoint cp, double x, double y )
	{
		ActionEvent	parentE;
		Point		loc;
		int			index	= getComponentIndex( cp );
		DoublePoint	ptThis, ptLeft, ptRight;

		synchronized( curve ) {

			ptThis	= curve.getPoint( index );
			if( ptThis == null ) return;
			
			ptLeft	= curve.getPoint( index - 1 );
			ptRight	= curve.getPoint( index + 1 );
			if( (ptLeft != null) && (ptRight != null) ) {
				if( x <= ptLeft.x ) {
					x = ptLeft.x + curve.hSpace.inc;
				}
				if( x >= ptRight.x ) {
					x = ptRight.x - curve.hSpace.inc;
				}
			} else {
				x = ptThis.x;		// Endpunkte nicht horizontal verschieben
			}
		
			loc = paramSpaceToScreen( x, y );
			cp.setLocation( loc.x - (CP_WIDTH>>1), loc.y - (CP_HEIGHT>>1) );
			curve.removePoint( index );
			curve.addPoint( x, y );
			repaint();
		}
		// Listener benachrichtigen
		parentE = new ActionEvent( this, ActionEvent.ACTION_PERFORMED,
								   ACTION_POINTCHANGED );
		actionComponent.dispatchEvent( parentE );
	}

	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
	
		int			hGrid, vGrid;
		int			numPoints;
		Point		pt1, pt2;
		Graphics2D	g2	= (Graphics2D) g;

        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

	// -------- Grid --------
		hGrid = 1 << (int) Math.max( 0.0, Math.floor( Math.log( (float) dim.width  / 20 ) / Constants.ln2 ));
		vGrid = 1 << (int) Math.max( 0.0, Math.floor( Math.log( (float) dim.height / 16 ) / Constants.ln2 ));
	
		g2.setColor( Color.lightGray );
		for( int i = 0; i <= hGrid; i++ ) {
			g2.drawLine( (dim.width-1) * i / hGrid, 0, (dim.width-1) * i / hGrid, dim.height );
		}
		for( int i = 0; i <= vGrid; i++ ) {
			g2.drawLine( 0, (dim.height-1) * i / vGrid, dim.width, (dim.height-1) * i / vGrid );
		}

	// -------- Punkt-Verbindungen --------
		numPoints = getComponentCount();
		if( numPoints >= 2 ) {

			g2.setColor( Color.black );

			pt1 = getComponent( 0 ).getLocation();
			if( curve.type == Curve.TYPE_DIA ) {		// diagonal verbinden

				for( int i = 1; i < numPoints; i++, pt1 = pt2 ) {

					pt2	= getComponent( i ).getLocation();
					g2.drawLine( pt1.x + (CP_WIDTH>>1), pt1.y + (CP_HEIGHT>>1),
								 pt2.x + (CP_WIDTH>>1), pt2.y + (CP_HEIGHT>>1) );
				}
			} else {									// orthogonal verbinden

				for( int i = 1; i < numPoints; i++, pt1 = pt2 ) {

					pt2	= getComponent( i ).getLocation();
					g2.drawLine( pt1.x + (CP_WIDTH>>1), pt1.y + (CP_HEIGHT>>1),
								 pt2.x + (CP_WIDTH>>1), pt1.y + (CP_HEIGHT>>1) );
					g2.drawLine( pt2.x + (CP_WIDTH>>1), pt1.y + (CP_HEIGHT>>1),
								 pt2.x + (CP_WIDTH>>1), pt2.y + (CP_HEIGHT>>1) );
				}
			}
		}
	}

// -------- private Methoden --------

	/*
	 *	Berechnet die Position der Punkt neu
	 */
	protected void recalcScreenPoints()
	{
		Component	c;
		DoublePoint	pt;
		Point		loc;

		synchronized( curve ) {
	
			for( int i = curve.size() - 1; i >= 0; i-- ) {

				c	= getComponent( i );
				pt	= curve.getPoint( i );
				loc	= paramSpaceToScreen( pt.x, pt.y );
				c.setLocation( loc.x - (CP_WIDTH>>1), loc.y - (CP_HEIGHT>>1) );
			}
		}
	}

	/*
	 *	Uebersetzt Bildschirm-Koordinaten in Parameter-Koordinaten
	 */
	protected DoublePoint screenToParamSpace( int x, int y )
	{
		double dx, dy;

		synchronized( curve ) {
	
			dx	= curve.hSpace.min + (curve.hSpace.max - curve.hSpace.min) * x / (dim.width-1);
			dy	= curve.vSpace.max - (curve.vSpace.max - curve.vSpace.min) * y / (dim.height-1);

			return new DoublePoint( curve.hSpace.fitValue( dx ), curve.vSpace.fitValue( dy ));
		}
	}

	/*
	 *	Uebersetzt Parameter-Koordinaten in Bildschirm-Koordinaten
	 */
	protected Point paramSpaceToScreen( double dx, double dy )
	{
		int x, y;

		synchronized( curve ) {

			x	= (int) ((double) (dim.width-1) *
				  (dx - curve.hSpace.min) / (curve.hSpace.max - curve.hSpace.min));
			y	= (int) ((double) (dim.height-1) *
				  (curve.vSpace.max - dy) / (curve.vSpace.max - curve.vSpace.min));

			return new Point( x, y );
		}
	}

	/*
	 *	Index auf dem Panel, -1 bei Fehler
	 */
	protected int getComponentIndex( Component c )
	{
		Component c2;
	
		for( int i = getComponentCount() - 1; i >= 0; i-- ) {
			c2 = getComponent( i );
			if( c2 == c ) {
				return i;
			}
		}
		return -1;
	}

// -------- interne CurvePoint-Klasse --------

	private static class CurvePoint
	extends JPanel
	{
	// ........ private Variablen ........

		// Status wie STATE_NORMAL, selektiert etc.
		private int state = CurvePanel.CP_STATE_UNKNOWN;

	// ........ public Methoden ........

		protected CurvePoint()
		{
			super( null );

			Dimension dim = new Dimension( CurvePanel.CP_WIDTH, CurvePanel.CP_HEIGHT );

			setSelected( CurvePanel.CP_STATE_NORMAL );
			setSize( dim );
			setLocation( 0, 0 );

			// Event handling
			enableEvents(AWTEvent.MOUSE_EVENT_MASK);
			
			setFocusable( false );
		}

		/**
		 *	Status veraendern
		 *
		 *	@return	vorheriger Status
		 */
		protected int setSelected( int state )
		{
			int lastState	= this.state;
			this.state		= state;
	
			if( lastState != state ) {
				if( state == CurvePanel.CP_STATE_NORMAL ) {
					setForeground( SystemColor.control );
					setBackground( SystemColor.control );
				} else {
					setForeground( OpIcon.selectColor );
					setBackground( OpIcon.selectColor );
				}
				repaint();
			}
			return lastState;
		}
		
//		private int isSelected()
//		{
//			return state;
//		}

		public void paintComponent( Graphics g )
		{
			super.paintComponent( g );
		
			g.draw3DRect( 1, 1, CurvePanel.CP_WIDTH - 3, CurvePanel.CP_HEIGHT - 3, true );
			g.setColor( Color.black );
			g.drawRect( 0, 0, CurvePanel.CP_WIDTH - 1, CurvePanel.CP_HEIGHT - 1 );
		}

	// ........ private Methoden ........

		protected void processMouseEvent( MouseEvent e )
		{
			if( e.getID() == MouseEvent.MOUSE_PRESSED ) {
				requestFocus();
			}
			super.processMouseEvent( e );
		}
	}
	// class CurvePoint
}
// class CurvePanel
