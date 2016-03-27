/*
 *  DragContext.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.util.Slots;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

/**
 *  Provides useful information during a
 *	drag-n-drop gesture.
 */
public class DragContext
		implements KeyListener {

// -------- public variables --------

    public static final int MOVE	=	0;	// drag-type
    public static final int LINK	=	1;

// -------- private variables --------

    private Component	source;
    private Component	target			= null;
    private String		srcName;
    private Container	container;
    private int			type;

    private	Rectangle	srcBounds;

    private	Point		initialLoc;
//	private Point		lastLoc;
    private Point		currentLoc;

    private Cursor		dragCursor;
    private Cursor		oldCursor;

    private int			threshold;
    private	boolean		dragStarted		= false;
    private boolean		dragSuccessful	= false;

    private int targetLastState;			// previous to error state

    private	Point		srcPoint, targetPoint;		// fuer Link-Pfeil

// -------- public methods --------

    public DragContext( MouseEvent e, int type )
    {
        this.source	= e.getComponent();
        srcName		= source.toString();
        this.type	= type;

        srcBounds	= source.getBounds();
        initialLoc	= new Point( srcBounds.x + e.getX(), srcBounds.y + e.getY() );
//		lastLoc		= initialLoc;
        currentLoc	= initialLoc;
        srcPoint	= new Point( 0, 0 );
        targetPoint	= new Point( 0, 0 );

        dragStarted	= false;

        if( srcName.equals ( OpPanel.OBJ_NAME ) ) {
            container = (Container) source;
        } else {
            container = source.getParent();
        }

        dragCursor	= new Cursor( (( srcName.equals ( OpIcon.OBJ_NAME ) ) && (type == MOVE)) ?
                                  Cursor.MOVE_CURSOR : Cursor.CROSSHAIR_CURSOR );
        threshold	= 5;
    }

    public Component getSource()
    {
        return source;
    }

    public int getType()
    {
        return type;
    }

    public Component getTarget()
    {
        return target;
    }

    /**
     *	Schwellwert in Pixels setzen, den die Maus
     *	mindestens ueberschreiten muss, damit der Drag beginnt
     */
    public void setThreshold( int dist )
    {
        threshold	= dist;
    }

    /**
     *	@return	true, wenn der Schwellwert ueberschritten wurde
     */
    public boolean hasDragStarted()
    {
        return dragStarted;
    }

    /**
     *	@return	true, wenn der Drag gueltig war
     */
    public boolean wasDragSuccessful()
    {
        return( dragSuccessful );
    }

    /**
     *	Always call when you get a new Message;
     *	this will update the DragContext's internal state
     */
    public void mouseDragged( MouseEvent e )
    {
        if( e.getComponent() != source ) return;

        Component newTarget;

        if( dragStarted ) {
            paintScheme( false );
        }

//		lastLoc		= currentLoc;
        currentLoc	= new Point( srcBounds.x + e.getX(), srcBounds.y + e.getY() );

        if( !dragStarted ) {

            if( (currentLoc.x - initialLoc.x) * (currentLoc.x - initialLoc.x) +
                (currentLoc.y - initialLoc.y) * (currentLoc.y - initialLoc.y) >=
                threshold * threshold ) {

                oldCursor = container.getCursor();
                container.setCursor( dragCursor );
                source.requestFocus();
                source.addKeyListener( this );

                dragStarted	= true;
                target		= null;
            }
        }

        if( dragStarted ) {

            if( srcName.equals ( OpIcon.OBJ_NAME ) ) {

                switch( type ) {
                case MOVE:
                    newTarget = ((OpPanel) container).getOpIconAround(
                                    currentLoc.x, currentLoc.y, (OpIcon) source, 0, 0 );

                    if( newTarget != target ) {		// sth changed
                        if( target != null ) {
                            ((OpIcon) target).setSelected( targetLastState );
                        }
                        target = newTarget;
                        if( target != null ) {
                            targetLastState = ((OpIcon) target).isSelected();
                            ((OpIcon) target).setSelected( OpIcon.STATE_ERROR );
                        }
                    }

                    dragSuccessful = (target == null);
                    break;

                case LINK:
                    newTarget = ((OpPanel) container).getOpIconAt(
                                    currentLoc.x, currentLoc.y );
                    if( newTarget == source ) {
                        newTarget = null;
                    }
                    if( newTarget != target ) {		// sth changed
                        if( target != null ) {
                            ((OpIcon) target).setSelected( targetLastState );
                        }

                        dragSuccessful	= false;
                        target			= newTarget;

                        if( target != null ) {		// pruefen ob Link moeglich ist
                            targetLastState = ((OpIcon) target).isSelected();
                            OpConnector.calcArrow( source, target, srcPoint, targetPoint,2,2);

                            if( !((OpIcon) target).getOperator().getSlots(
                                    Slots.SLOTS_READER | Slots.SLOTS_FREE ).isEmpty() ) {

                                ((OpIcon) target).setSelected( OpIcon.STATE_FRIENDLY );	// we could link to this
                                dragSuccessful = true;

                            } else {
                                ((OpIcon) target).setSelected( OpIcon.STATE_ERROR );	// we cannot
                            }
                        }
                    }
                    break;

                default:
                    break;
                }

            } else if( srcName.equals ( OpConnector.OBJ_NAME ) ) {
                // XXX
                dragSuccessful = true;
            }
            paintScheme( true );
        }
    }

    /**
     *	Always call when you get dis Message;
     *	this will terminate the drag
     */
    public void mouseReleased( MouseEvent e )
    {
        if( dragStarted ) {

            if( (target != null) && ( target.toString ().equals ( OpIcon.OBJ_NAME ) ) ) {
                ((OpIcon) target).setSelected( targetLastState );
            }
            paintScheme( false );
            container.setCursor( oldCursor );
            source.removeKeyListener( this );
            dragStarted	= false;
        }
    }

    protected void paintScheme( boolean mode )
    {
        Graphics2D	g = (Graphics2D) container.getGraphics();
        Graphics2D	g2;
        Point		loc;

        if( g == null ) return;

        // g.setRenderingHint ( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setXORMode(container.getBackground());
        g.setColor(container.getForeground());

        switch( type ) {
        case MOVE:
            ((Dragable) source).paintScheme( g, currentLoc.x, currentLoc.y, mode );
            break;

        case LINK:
            if( target == null ) {
                OpConnector.drawArrow( g, srcBounds.x + (srcBounds.width >> 1),
                                       srcBounds.y + (srcBounds.height >> 1),
                                       currentLoc.x, currentLoc.y, true );
            } else {
                OpConnector.drawArrow( g, srcPoint.x, srcPoint.y,
                                       targetPoint.x, targetPoint.y, true );
                g2 = (Graphics2D) target.getGraphics();	// nochmal "innerhalb" des OpIcons zeichnen
                if( g2 != null ) {
                    g2.setXORMode( container.getBackground() );
                    // g2.setColor( Color.black );
                    g2.setColor( container.getForeground () );
                    loc = target.getLocation();
                    OpConnector.drawArrow( g2, srcPoint.x - loc.x, srcPoint.y - loc.y,
                                           targetPoint.x - loc.x, targetPoint.y - loc.y,
                                           true );
                    g2.dispose();
                }
            }
            break;

        default:
            break;
        }

        g.dispose();
    }

// -------- (static) Utility methods --------

// -------- Key Listener methods --------

    public void keyPressed(KeyEvent e) {
        MouseEvent virtual;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {

            virtual = new MouseEvent(source, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0,
                    currentLoc.x, currentLoc.y, 0, false);

            dragSuccessful = false;
            mouseReleased(virtual);        // sicherheitshalber jetzt schon
            source.dispatchEvent(virtual);
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}