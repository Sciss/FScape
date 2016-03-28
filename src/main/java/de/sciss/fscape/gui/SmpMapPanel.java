/*
 *  SmpMapPanel.java
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

import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.DoublePoint;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.SmpMap;
import de.sciss.fscape.util.SmpZone;
import de.sciss.gui.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 *	Utility class for the SmpSynDlg allowing
 *	the placement of sound regions in a 
 *	frequency/dynamic plane.
 */
public class SmpMapPanel
        extends JPanel
        implements ComponentListener, MouseListener, MouseMotionListener {

// -------- public variables --------

    /**
     *	ActionCommands, die ein registrierter (AddActionListener())
     *	ActionListener empfaengt. Angefuegt wird die uniqueID der
     *	correspondierden SmpZone
     */
    public static final String	ACTION_BOXSELECTED		= "act";
    public static final String	ACTION_BOXDESELECTED	= "des";
    public static final String	ACTION_BOXCHANGED		= "mov";
    public static final String	ACTION_BOXCREATED		= "new";
    public static final String	ACTION_BOXDELETED		= "rem";
    public static final String	ACTION_SPACECHANGED		= "spc";

// -------- private variables --------

    // gerade angewaehlte Box; wenn keiner ausgewaehlt, muss
    // er auf dummyBox gesetzt werden, damit keine Null-Pointer Fehler
    // auftreten koennen!
    protected SmpBox currentSmpBox;
    protected SmpBox dummyBox;

    private Cursor	lastCursor;					// Cursor previously used to dnd or edit-op

    // this is all for drag+drop
    private SmpBox		dragSource	= null;
    private boolean		dragState	= false;	// true for drag+drop
    private int			dragType	= DRAG_NONE;
    private int			dragOriginX;			// last coordinates
    private int			dragOriginY;			// last coordinates
    private Rectangle	dragBounds	= null;
    private Rectangle	dragRubber;
    private SmpZone dragSmp;
    private DoublePoint	dragTopLeft;			// Space-Koordinaten
    private DoublePoint dragBottomRight;

    // folgende Werte ODER-verknuepft
    private static final int DRAG_NONE	=	0x00;	// no drag
    private	static final int DRAG_TOP	=	0x01;	// drag top margin
    private	static final int DRAG_LEFT	=	0x02;	// drag left margin
    private	static final int DRAG_BOTTOM=	0x04;	// drag bottom margin
    private	static final int DRAG_RIGHT	=	0x08;	// drag right margin
    private	static final int DRAG_MOVE	=	0x0F;	// (drag all four margins)

    private int cursorID[] = { Cursor.DEFAULT_CURSOR, Cursor.N_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR,
                               Cursor.NW_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR, Cursor.DEFAULT_CURSOR,
                               Cursor.SW_RESIZE_CURSOR, Cursor.DEFAULT_CURSOR, Cursor.E_RESIZE_CURSOR,
                               Cursor.NE_RESIZE_CURSOR, Cursor.DEFAULT_CURSOR, Cursor.DEFAULT_CURSOR,
                               Cursor.SE_RESIZE_CURSOR, Cursor.DEFAULT_CURSOR, Cursor.DEFAULT_CURSOR,
                               Cursor.MOVE_CURSOR };

    // fuer SmpBox
    protected static final int SB_STATE_NORMAL		= 0;
    protected static final int SB_STATE_SELECTED	= 3;
    protected static final int SB_STATE_UNKNOWN		= -1;

    protected SmpMap smpMap;
    protected Vector<SmpBox> smpBoxes;	// !selbe Indices wie smpMap
    protected Dimension	dim;		// Panel-Size; vom Component-Listener geupdated
    protected double	vSpaceLog;	// ln( vSpace.max / vSpace.min) fuer logarithmische Skala wichtig

    // dieser Button wird nicht angezeigt, sondern dient nur als
    // Institution zum Verwalten der ActionListener und fuer
    // den Event-Dispatch!
    private Button		actionComponent;

    // Fehlermeldungen
    protected static final String ERR_CORRUPTED	= "Internal data corrupted. Please report bug!";

// -------- public methods --------

    public SmpMapPanel()
    {
        super();
        actionComponent		= new Button();
        dummyBox			= new SmpBox();
        currentSmpBox		= dummyBox;
        smpMap				= new SmpMap( Constants.spaces[ Constants.emptySpace ],
                                          Constants.spaces[ Constants.emptySpace ]);
        smpBoxes			= new Vector<SmpBox>();
        dragRubber			= new Rectangle();

        setLayout( null );
        setBackground( Color.white );
        dim	= getPreferredSize();
        setSize( dim );

        addComponentListener( this );
        addMouseListener( this );
        addMouseMotionListener( this );

        setSmpMap( smpMap );

        setFocusable( false );
    }

    /**
     *	Sample-Map zuweisen
     */
    public void setSmpMap( SmpMap smpMap )
    {
        SmpBox	sb;

        clear();

        synchronized( this.smpMap ) {
            this.smpMap			= (SmpMap) smpMap.clone();
            vSpaceLog			= Math.log( smpMap.vSpace.max / smpMap.vSpace.min );
            currentSmpBox		= dummyBox;

            for( int i = 0; i < smpMap.size(); i++ ) {
                sb = new SmpBox();
                sb.setName( getSmpName( smpMap.getSample( i )));
                sb.addMouseListener( this );
                sb.addMouseMotionListener( this );
                add( sb );
                smpBoxes.addElement( sb );
            }
        }
        recalcScreenBoxes();
        recalcBoxColors();

        // Listener benachrichtigen
    //	notifyListener( ACTION_BOXDESELECTED, -1 );
    }

    /**
     *	Sample-Map besorgen
     */
    public SmpMap getSmpMap()
    {
        synchronized( smpMap ) {
            return (SmpMap) smpMap.clone();
        }
    }

    /**
     *	Alle Boxen entfernen
     */
    public void clear()
    {
        synchronized( smpMap ) {
            currentSmpBox	= dummyBox;
            removeAll();
            smpBoxes.setSize( 0 );

            while( smpMap.size() > 0 ) {
                smpMap.removeSample( 0 );
            }
        }
        repaint();
        // Listener benachrichtigen
        notifyListener( ACTION_BOXDESELECTED, -1 );
    }

    /**
     *	Dimensionen aendern
     *	erzeugt eine neue Sample Map mit entsprechend beschnittenen/expandierten Boxen
     *	(MOEGLICHERWEISE WERDEN EINIGE GELOESCHT; ES WERDEN KEINE BOXDELETED EVENTS GESENDET!)
     *	NOTE: UNIT-WECHSEL WIRD FEHLERHAFT BEHANDELT!!
     *
     *	@param	hSpace	neuer horizontaler Space, darf null sein
     *					(= keine Aenderung)
     *	@param	vSpace	neuer vertikaler Space, darf null sein
     *					(= keine Aenderung)
     */
    public void setSpaces( ParamSpace hSpace, ParamSpace vSpace )
    {
        SmpMap	newSmpMap;
        Vector<SmpBox>	newBoxes;
        SmpZone	smp;
        SmpBox	sb;
        int		newIndex;

        synchronized( smpMap ) {

            if( hSpace == null)	hSpace = smpMap.hSpace;
            if( vSpace == null)	vSpace = smpMap.vSpace;

            // Punkte skalieren
            newSmpMap	= new SmpMap( hSpace, vSpace, smpMap.type );
            newBoxes	= new Vector<SmpBox>();

            for( int index = smpMap.size() - 1; index >= 0; index-- ) {

                smp	= smpMap.getSample( index );
                sb	= smpBoxes.elementAt( index );

                if( Math.abs( smp.velLo.value - smpMap.hSpace.min ) < Constants.suckyDoubleError ) {		// expand
                    smp.velLo.value = hSpace.min;
                } else {
                    smp.velLo.value = hSpace.fitValue( smp.velLo.value);
                }
                if( Math.abs( smp.velHi.value - smpMap.hSpace.max ) < Constants.suckyDoubleError ) {		// expand
                    smp.velHi.value = hSpace.max;
                } else {
                    smp.velHi.value = hSpace.fitValue( smp.velHi.value);
                }
                if( Math.abs( smp.freqHi.value - smpMap.vSpace.max ) < Constants.suckyDoubleError ) {		// expand
                    smp.freqHi.value = vSpace.max;
                } else {
                    smp.freqHi.value = vSpace.fitValue( smp.freqHi.value);
                }
                if( Math.abs( smp.freqLo.value - smpMap.vSpace.min ) < Constants.suckyDoubleError ) {		// expand
                    smp.freqLo.value = vSpace.min;
                } else {
                    smp.freqLo.value = vSpace.fitValue( smp.freqLo.value);
                }

                if( ((smp.velHi.value - smp.velLo.value + Constants.suckyDoubleError) > hSpace.inc) &&	// not if too small
                    ((smp.freqHi.value - smp.freqLo.value + Constants.suckyDoubleError) > vSpace.inc) ) {

                    newIndex = newSmpMap.addSample( smp );
                    if( newIndex != -1 ) {
                        newBoxes.insertElementAt( sb, newIndex );
                    } else {
                        remove( sb );
                    }
                } else {
                    remove( sb );
                }
            }

            this.smpMap 	= newSmpMap;
            this.smpBoxes	= newBoxes;
            vSpaceLog		= Math.log( vSpace.max / vSpace.min );
            currentSmpBox.setSelected( SB_STATE_NORMAL );
            currentSmpBox	= dummyBox;
        }

        recalcScreenBoxes();
        recalcBoxColors();

        // Listener benachrichtigen
        notifyListener( ACTION_BOXDESELECTED, -1 );
        notifyListener( ACTION_SPACECHANGED, -1 );
    }

    /**
     *	Derzeitigen horizontalen Space ermitteln
     */
    public ParamSpace getHSpace()
    {
        synchronized( smpMap ) {
            return smpMap.hSpace;
        }
    }

    /**
     *	Derzeitigen vertikalen Space ermitteln
     */
    public ParamSpace getVSpace()
    {
        synchronized( smpMap ) {
            return smpMap.vSpace;
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
     *	Eine SampleZone besorgen
     *	DAS OBJECT IST EIN CLONE!
     *
     *	@param	uniqueID	bei -1 wird die aktuell angewaehlte Box genommen
     *
     *	@return	null, wenn Fehler oder keine Box angewaehlt
     */
    public SmpZone getSample( int uniqueID )
    {
        int			index;
        SmpZone		smp		= null;

        synchronized( smpMap ) {
            if( uniqueID == -1 ) {

                index	= smpBoxes.indexOf( currentSmpBox );
                smp		= smpMap.getSample( index );
                if( smp == null ) return null;

            } else {

                for( index = 0; index < smpMap.size(); index++ ) {
                    smp = smpMap.getSample( index );
                    if( (smp != null) && (smp.uniqueID == uniqueID) ) break;
                }
                if( (smp == null) || (smp.uniqueID != uniqueID) ) return null;
            }
            return( (SmpZone) smp.clone() );
        }
    }

    /**
     *	Neue Werte fuer aktuelle Sample-Zone mitteilen
     *	DAS OBJECT WIRD GECLONT!
     *
     *	@return	false, wenn keine Box angewaehlt ist oder Werte ungueltig sind
     */
    public boolean setSample( SmpZone smp )
    {
        SmpBox		sb		= currentSmpBox;
        int			index;
        int			newIndex;
        SmpZone		smpOld;

        synchronized( smpMap ) {
            index	= smpBoxes.indexOf( sb );
            smpOld	= smpMap.getSample( index );

            if( (smpOld == null) || (smpOld.uniqueID != smp.uniqueID) ) {	// hmmm, must have changed?!

                for( index = 0; index < smpMap.size(); index++ ) {
                    smpOld = smpMap.getSample( index );
                    if( smpOld.uniqueID == smp.uniqueID ) break;
                }
                if ((smpOld == null) || (smpOld.uniqueID != smp.uniqueID)) {
                    GUIUtil.displayError(this, new NoSuchElementException(ModulePanel.ERR_CORRUPTED), "setSample");
                    return false;
                }

                sb.setSelected( SB_STATE_NORMAL );
                sb = smpBoxes.elementAt( index );
                sb.setSelected( SB_STATE_SELECTED );
                currentSmpBox = sb;
            }

            smpMap.removeSample( index );
            smpBoxes.removeElement( sb );
            smp			= (SmpZone) smp.clone();
            newIndex	= smpMap.addSample( smp );
            sb.setName( getSmpName( smp ));

            if( newIndex == -1 ) {	// restore old one
                newIndex = smpMap.addSample( smpOld );
                if( newIndex == -1 ) {	// fatal error
                    GUIUtil.displayError(this, new IllegalStateException(ModulePanel.ERR_CORRUPTED), "setSample");

                    currentSmpBox.setSelected( SB_STATE_NORMAL );
                    currentSmpBox = dummyBox;
                    notifyListener( ACTION_BOXDELETED, smp.uniqueID );
                    return false;
                }
            }

            smpBoxes.insertElementAt( sb, newIndex );
            recalcScreenBox( sb );
            if( newIndex != index ) {
                recalcBoxColors();
            }

            // Listener benachrichtigen
            notifyListener( ACTION_BOXCHANGED, smp.uniqueID );
        }
        return true;
    }

    /**
     *	Box auf das Panel legen
     *
     *	@return	null bei Fehler (z.B. schon Box am selben Ort vorhanden)
     */
    public SmpZone addSample( SmpZone smp )
    {
        int			index;
        SmpBox		sb	= new SmpBox();

        sb.setName( getSmpName( smp ));
        sb.addMouseListener( this );
        sb.addMouseMotionListener( this );

        synchronized( smpMap ) {

            index = smpMap.addSample( smp );
            if( index >= 0 ) {
                add( sb );
                smpBoxes.insertElementAt( sb, index );
                recalcScreenBox( sb );
                recalcBoxColors();

                // umschalten
                currentSmpBox.setSelected( SB_STATE_NORMAL );
                currentSmpBox = sb;
                currentSmpBox.setSelected( SB_STATE_SELECTED );
                // Listener benachrichtigen
                notifyListener( ACTION_BOXCREATED, smp.uniqueID );

                return smp;
            }
            return null;
         }
    }

    /**
     *	Box vom Panel entfernen
     */
    public void removeBox( SmpBox sb )
    {
        int			index;
        SmpZone		smp;

        synchronized( smpMap ) {
            index = smpBoxes.indexOf( sb );
            if( index >= 0 ) {
                remove( sb );
                smp = smpMap.getSample( index );
                smpMap.removeSample( index );
                smpBoxes.removeElement( sb );
                recalcBoxColors();

                currentSmpBox.setSelected( SB_STATE_NORMAL );
                currentSmpBox = dummyBox;

                // Listener benachrichtigen
                notifyListener( ACTION_BOXDELETED, smp.uniqueID );
            }
        }
    }
 
    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );

        int hGrid, vGrid;

    // -------- Grid --------
        hGrid = 1 << (int) Math.max( 0.0, Math.floor( Math.log( (float) dim.width  / 20 ) / Constants.ln2 ));
        vGrid = 1 << (int) Math.max( 0.0, Math.floor( Math.log( (float) dim.height / 16 ) / Constants.ln2 ));

        g.setColor( Color.lightGray );
        for( int i = 0; i <= hGrid; i++ ) {
            g.drawLine( (dim.width-1) * i / hGrid, 0, (dim.width-1) * i / hGrid, dim.height );
        }
        for( int i = 0; i <= vGrid; i++ ) {
            g.drawLine( 0, (dim.height-1) * i / vGrid, dim.width, (dim.height-1) * i / vGrid );
        }
    }

    public void setEnabled( boolean state )
    {
        super.setEnabled( state );

        if( currentSmpBox != dummyBox ) {
            currentSmpBox.setSelected( SB_STATE_NORMAL );
            currentSmpBox = dummyBox;
            // Listener benachrichtigen
            notifyListener( ACTION_BOXDESELECTED, -1 );
        }

        setBackground( state ? Color.white : getParent().getBackground() );
        repaint();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension( 64, 64 );
    }
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

// -------- Component methods (nur Panel!) --------

    public void componentResized( ComponentEvent e )
    {
        dim = getSize();
        recalcScreenBoxes();
        repaint();
    }

    public void componentMoved( ComponentEvent e )	{}
    public void componentHidden( ComponentEvent e )	{}
    public void componentShown( ComponentEvent e )	{}

// -------- Mouse Listener methods --------

    public void mouseClicked( MouseEvent e )
    {
        DoublePoint dp1, dp2;
        SmpZone		smp;
        Rectangle	r1, r2;
        Point		p1, p2;
        Graphics	g;

        if( !isEnabled() ) return;	// not while inactive

        if( e.getSource() == this ) {				//-------- Panel hit -------------------

            if( (e.getClickCount() == 2) && !e.isAltDown() ) {

                dp1	= new DoublePoint();
                dp2	= new DoublePoint();

                if( findFreePlaceAround( e.getX(), e.getY(), dp1, dp2 )) {
                    synchronized( smpMap ) {
                        smp	= new SmpZone( new Param( dp1.y, smpMap.vSpace.unit ),
                                           new Param( dp2.y, smpMap.vSpace.unit ),
                                           new Param( dp2.x, smpMap.hSpace.unit ),
                                           new Param( dp1.x, smpMap.hSpace.unit ));
                    }
                    p1 = paramSpaceToScreen( dp1.x, dp1.y );
                    p2 = paramSpaceToScreen( dp2.x, dp2.y );
                    r1 = new Rectangle( e.getX(), e.getY(), 1, 1 );
                    r2 = new Rectangle( p1.x, p1.y, p2.x - p1.x - 1, p2.y - p1.y - 1 );

                    g = getGraphics();
                    g.setColor( Color.black );
                    g.setXORMode( getBackground() );
                    GUISupport.rubberGlide( r1, r2, g );
                    g.dispose();

                    addSample( smp );
                }
            }
        } else {

            if( e.isAltDown() ) {
                removeBox( (SmpBox) e.getSource() );
            }
        }
    }

    public void mousePressed( MouseEvent e )
    {
        Rectangle		sbBounds;
        int				index;

        if( !isEnabled() ) return;	// not while disabled

        // alte SmpBox deselektieren
        currentSmpBox.setSelected( SB_STATE_NORMAL );

        if( e.getSource() == this ) {				//-------- Panel hit -------------------

            if( currentSmpBox != dummyBox ) {
                currentSmpBox = dummyBox;
                // Listener benachrichtigen
                notifyListener( ACTION_BOXDESELECTED, -1 );
            }

            if( e.isControlDown() ) {	// PopUp-Menu
                // XXX
            }

        } else {									//-------- SmpBox hit ------------------

            currentSmpBox		= (SmpBox) e.getSource();
            sbBounds			= currentSmpBox.getBounds();

            // neue SmpBox selektieren
            currentSmpBox.setSelected( SB_STATE_SELECTED );

            if( e.isControlDown() ) {		// PopUp-Menu
                // XXX

            } else if( !e.isAltDown() ) {	// prepare Drag
                dragOriginX	= e.getX();
                dragOriginY	= e.getY();
                dragBounds	= sbBounds;
                dragSource	= currentSmpBox;

                synchronized( smpMap ) {
                    index = smpBoxes.indexOf( dragSource );
                    if( index >= 0 ) {

                        dragSmp		= smpMap.getSample( index );

                        dragType	= DRAG_NONE;
                        if( e.getY() < 4 ) {
                            dragType |= DRAG_TOP;
                        } else if( (sbBounds.height - e.getY()) < 5 ) {
                            dragType |= DRAG_BOTTOM;
                        }
                        if( e.getX() < 4 ) {
                            dragType |= DRAG_LEFT;
                        } else if( (sbBounds.width  - e.getX()) < 5 ) {
                            dragType |= DRAG_RIGHT;
                        }
                        if( dragType == DRAG_NONE )	dragType  = DRAG_MOVE;
                    }
                }
            }
        }
    }

    public void mouseReleased( MouseEvent e )
    {
        Graphics	g, g2;
        SmpZone		smp;
        int			index;

        if( !isEnabled() ) return;	// not while running the operators

        if( dragState ) {

            // clear rubberband
            g = getGraphics();
            g.setColor( Color.black );
            g.setXORMode( getBackground() );
            g2 = dragSource.getGraphics();
            g2.setColor( Color.black );
            g2.setXORMode( Color.black );
            g2.setClip( 1, 1, dragBounds.width - 2, dragBounds.height - 2 );
            g.drawRect( dragRubber.x, dragRubber.y, dragRubber.width - 1, dragRubber.height - 1 );
            g2.drawRect( dragRubber.x - dragBounds.x, dragRubber.y - dragBounds.y,
                         dragRubber.width - 1, dragRubber.height - 1 );

            smp	= (SmpZone) dragSmp.clone();
            smp.freqHi.value = dragTopLeft.y;
            smp.freqLo.value = dragBottomRight.y;
            smp.velHi.value = dragBottomRight.x;
            smp.velLo.value = dragTopLeft.x;

            if( !setSample( smp )) {	// failed to "resize" the zone
                GUISupport.rubberGlide( dragRubber, dragBounds, g );
                notifyListener( ACTION_BOXSELECTED, smp.uniqueID );
            }

            g.dispose();
            g2.dispose();

            dragState = false;
            setCursor( lastCursor );

        } else {

            // Listener benachrichtigen
            if( (currentSmpBox != dummyBox) && !e.isAltDown() ) {

                index	= smpBoxes.indexOf( currentSmpBox );
                smp		= smpMap.getSample( index );
                if( smp != null ) {
                    notifyListener( ACTION_BOXSELECTED, smp.uniqueID );
                }
            }
        }

        dragType	= DRAG_NONE;
        dragSource	= null;
    }

    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}

// -------- MouseMotion Listener methods --------

    public void mouseDragged( MouseEvent e )
    {
        Graphics	g, g2;
        int			dist;
        DoublePoint	dp1, dp2;
        DoublePoint	otl, obr;
        Point		p1, p2;

        if( !isEnabled() || (dragType == DRAG_NONE) ) return;

        g = getGraphics();
        g.setColor( Color.black );
        g.setXORMode( getBackground() );
        g2 = dragSource.getGraphics();
        g2.setColor( Color.black );
        g2.setXORMode( Color.black );
        g2.setClip( 1, 1, dragBounds.width - 2, dragBounds.height - 2 );

        if( !dragState ) {		// check if distance is ok to start drag

            dist = (e.getX() - dragOriginX) * (e.getX() - dragOriginX) +
                   (e.getY() - dragOriginY) * (e.getY() - dragOriginY);

            if( (dragType == DRAG_MOVE) && (dist <= 16) ) {
                g.dispose();
                g2.dispose();
                return;	// ...not ok
            }

            lastCursor = getCursor();
            setCursor( new Cursor( cursorID[ dragType ]));
            dragState = true;

            dragRubber.setBounds( dragSource.getBounds() );
            dragTopLeft		= new DoublePoint( dragSmp.velLo.value, dragSmp.freqHi.value);
            dragBottomRight	= new DoublePoint( dragSmp.velHi.value, dragSmp.freqLo.value);

        } else {
            // clear rubberband
            g.drawRect( dragRubber.x, dragRubber.y, dragRubber.width - 1, dragRubber.height - 1 );
            g2.drawRect( dragRubber.x - dragBounds.x, dragRubber.y - dragBounds.y,
                         dragRubber.width - 1, dragRubber.height - 1 );
        }

        // Screen-Koordinaten berechnen: l/r
        if( (dragType & DRAG_LEFT) != 0 ) {
            dragRubber.x = dragBounds.x + e.getX() - dragOriginX;
            if( (dragType & DRAG_RIGHT) == 0 ) {
                dragRubber.width = dragBounds.width - e.getX() + dragOriginX;
            }
        } else {
            if( (dragType & DRAG_RIGHT) != 0 ) {
                dragRubber.width = dragBounds.width + e.getX() - dragOriginX;
            }
        }
        if( dragRubber.x < 0 ) {
            if( (dragType & DRAG_RIGHT) == 0 ) {
                dragRubber.width += dragRubber.x;
            }
            dragRubber.x = 0;
        }
        if( (dragRubber.x + dragRubber.width) > dim.width ) {
            if( (dragType & DRAG_LEFT) != 0 ) {
                dragRubber.x = dim.width - dragRubber.width;
            } else {
                dragRubber.width = dim.width - dragRubber.x + 1;
            }
        }

        // Screen-Koordinaten berechnen: t/b
        if( (dragType & DRAG_TOP) != 0 ) {
            dragRubber.y = dragBounds.y + e.getY() - dragOriginY;
            if( (dragType & DRAG_BOTTOM) == 0 ) {
                dragRubber.height = dragBounds.height - e.getY() + dragOriginY;
            }
        } else {
            if( (dragType & DRAG_BOTTOM) != 0 ) {
                dragRubber.height = dragBounds.height + e.getY() - dragOriginY;
            }
        }
        if( dragRubber.y < 0 ) {
            if( (dragType & DRAG_BOTTOM) == 0 ) {
                dragRubber.height += dragRubber.y;
            }
            dragRubber.y = 0;
        }
        if( (dragRubber.y + dragRubber.height) > dim.height ) {
            if( (dragType & DRAG_TOP) != 0 ) {
                dragRubber.y = dim.height - dragRubber.height;
            } else {
                dragRubber.height = dim.height - dragRubber.y + 1;
            }
        }

        dp1	= screenToParamSpace( dragRubber.x, dragRubber.y );
        dp2	= screenToParamSpace( dragRubber.x + dragRubber.width - 1, dragRubber.y + dragRubber.height - 1 );
        otl	= (DoublePoint) dragTopLeft.clone();
        obr = (DoublePoint) dragBottomRight.clone();

        // Space-Koordinaten aendern
        if( (dragType & DRAG_LEFT) != 0 ) {
            if( e.getX() != dragOriginX ) {
                dragTopLeft.x = dp1.x;
            } else {
                dragTopLeft.x = dragSmp.velLo.value;	// keine Rundungsfehler wenn derselbe Screen-Pixel
            }
        }
        if( (dragType & DRAG_TOP) != 0 ) {
            if( e.getY() != dragOriginY ) {
                dragTopLeft.y = dp1.y;
            } else {
                dragTopLeft.y = dragSmp.freqHi.value;
            }
        }
        if( (dragType & DRAG_RIGHT) != 0 ) {
            if( e.getX() != dragOriginX ) {
                dragBottomRight.x = dp2.x;
            } else {
                dragBottomRight.x = dragSmp.velHi.value;
            }
        }
        if( (dragType & DRAG_BOTTOM) != 0 ) {
            if( e.getY() != dragOriginY ) {
                dragBottomRight.y = dp2.y;
            } else {
                dragBottomRight.y = dragSmp.freqLo.value;
            }
        }

        // dragTopLeft/BottomRight evtl. beschneiden
        cutTheCheese( dragSmp, dragTopLeft, dragBottomRight, otl, obr );

        p1	= paramSpaceToScreen( dragTopLeft.x, dragTopLeft.y );
        p2	= paramSpaceToScreen( dragBottomRight.x, dragBottomRight.y );

        dragRubber.setBounds( p1.x, p1.y, p2.x - p1.x + 1, p2.y - p1.y + 1 );

        // current one
        g.drawRect( dragRubber.x, dragRubber.y, dragRubber.width - 1, dragRubber.height - 1 );
        g2.drawRect( dragRubber.x - dragBounds.x, dragRubber.y - dragBounds.y,
                     dragRubber.width - 1, dragRubber.height - 1 );
        g.dispose();
        g2.dispose();
    }

    public void mouseMoved( MouseEvent e ) {}


// -------- private methods --------

    protected void notifyListener( String actionStr, int smpID )
    {
        ActionEvent e;

        e = new ActionEvent( this, ActionEvent.ACTION_PERFORMED, actionStr + smpID );
        actionComponent.dispatchEvent( e );
    }

    /*
     *	Berechnet die Position der Boxen neu
     */
    protected void recalcScreenBoxes()
    {
        synchronized( smpMap ) {
            for( int i = 0; i < smpBoxes.size(); i++ ) {
                recalcScreenBox(smpBoxes.elementAt( i ));
            }
        }
    }

    /*
     *	Berechnet die Position einer Box neu
     */
    protected void recalcScreenBox( SmpBox sb )
    {
        int			index;
        SmpZone		smp;
        Point		loc1, loc2, loc3;

        synchronized( smpMap ) {
            index	= smpBoxes.indexOf( sb );
            smp		= smpMap.getSample( index );
            if( (sb != null) && (smp != null) ) {
                loc1 = paramSpaceToScreen( smp.velLo.value, smp.freqHi.value);
                loc2 = paramSpaceToScreen( smp.velHi.value, smp.freqLo.value);
                loc3 = paramSpaceToScreen( smp.velLo.value, smp.base.value);
                sb.setBase( loc3.y - loc1.y );
                sb.setBounds( loc1.x, loc1.y, loc2.x - loc1.x + 1, loc2.y - loc1.y + 1 );
            }
        }
    }

    /*
     *	Weist den Boxen die Farben neu zu
     */
    protected void recalcBoxColors()
    {
        SmpBox	sb;
        int		num;

        synchronized( smpMap ) {
            num = smpBoxes.size();
            for( int i = 0; i < num; i++ ) {
                sb = smpBoxes.elementAt( i );
                sb.setColor( (float) i / (float) num );
            }
        }
    }

    /*
     *	Extrahiert logischen Samplenamen
     */
    protected String getSmpName( SmpZone smp )
    {
        int 	i1, i2;
        String	name	= smp.fileName;

        i1 = name.lastIndexOf( File.separatorChar ) + 1;
        i2 = name.lastIndexOf( '.' );
        if( i2 < i1 ) {
            i2 = name.length();
        }

        return( name.substring( i1, i2 ));
    }

    /*
     *	Uebersetzt Bildschirm-Koordinaten in Parameter-Koordinaten
     */
    protected DoublePoint screenToParamSpace( int x, int y )
    {
        double dx, dy;

        synchronized( smpMap ) {

            dx	= smpMap.hSpace.min + (smpMap.hSpace.max - smpMap.hSpace.min) * x / (dim.width-1);
            dy	= Math.exp( (1.0 - (double) y / (double) (dim.height-1)) * vSpaceLog ) * smpMap.vSpace.min;
//			dy	= smpMap.vSpace.max - (smpMap.vSpace.max - smpMap.vSpace.min) * y / (dim.height-1);

            return new DoublePoint( smpMap.hSpace.fitValue( dx ), smpMap.vSpace.fitValue( dy ));
        }
    }

    /*
     *	Uebersetzt Parameter-Koordinaten in Bildschirm-Koordinaten
     */
    protected Point paramSpaceToScreen( double dx, double dy )
    {
        int x, y;

        synchronized( smpMap ) {

            x	= (int) ((dim.width-1) *
                  (dx - smpMap.hSpace.min) / (smpMap.hSpace.max - smpMap.hSpace.min) + 0.5);
            y	= (int) ((dim.height-1) * (1.0 - Math.log( dy / smpMap.vSpace.min ) / vSpaceLog));
//			y	= (int) ((double) (dim.height-1) *
//				  (smpMap.vSpace.max - dy) / (smpMap.vSpace.max - smpMap.vSpace.min) + 0.5);

            return new Point( x, y );
        }
    }

    /*
     *	Findet freien Platz fuer neue Zone
     *
     *	@param	x		Ausgangs-Punkt, X-Koordinate
     *	@param	y		Ausgangs-Punkt, Y-Koordinate
     *	@param	tl		wird gefuellt mit Koordinaten links/oben
     *	@param	br		wird gefuellt mit Koordinaten rechts/unten
     *	@return	false, wenn kein freier Platz gefunden wurde
     */
    protected boolean findFreePlaceAround( int x, int y, DoublePoint tl, DoublePoint br )
    {
        int			num, i, j;
        SmpZone		smp;
        double		velMid, freqMid;				// Space-Repraesentation der Parameter x, y
        double		velLo, velHi, freqLo, freqHi;	// Space-Rechteck
        boolean		iHoriz, iVert;					// possible intersections
        double		velCutLo[], velCutHi[], freqCutLo[], freqCutHi[];	// Verkleinerungsalternativen
        int			cutNum, bestCut;
        double		aspect, bestAspect, spaceAspect;
        Point		pt1, pt2;						// Screen-Rechteck
        DoublePoint dp;

        velCutLo	= new double[ 4 ];
        velCutHi	= new double[ 4 ];
        freqCutLo	= new double[ 4 ];
        freqCutHi	= new double[ 4 ];

        dp			= screenToParamSpace( x, y );
        velMid		= dp.x;
        freqMid		= dp.y;

        synchronized( smpMap ) {

            velLo	= smpMap.hSpace.min;	// initial rectangle = biggest one possible on the map
            velHi	= smpMap.hSpace.max;
            freqLo	= smpMap.vSpace.min;
            freqHi	= smpMap.vSpace.max;

            spaceAspect = (freqHi - freqLo) / (velHi - velLo);

            num = smpMap.size();
            for( i = 0; i < num; i++ ) {	// step through all zones

                smp = smpMap.getSample( i );
                if( smp == null ) continue;		// actually data is corrupted; anyway ;)

                iHoriz	= (smp.velLo.value < velHi) && (smp.velHi.value > velLo);
                iVert	= (smp.freqHi.value > freqLo) && (smp.freqLo.value < freqHi);
                if( !iHoriz || !iVert ) continue;		// no intersection

                cutNum	= 0;
                if( (smp.velHi.value <= velMid) && (smp.velHi.value < velHi) ) {		// cut left border
                    velCutLo[ cutNum ]	= smp.velHi.value;
                    velCutHi[ cutNum ]	= velHi;
                    freqCutLo[ cutNum ]	= freqLo;
                    freqCutHi[ cutNum ]	= freqHi;
                    cutNum++;
                }
                if( (smp.velLo.value >= velMid) && (smp.velLo.value > velLo) ) {		// cut right border
                    velCutLo[ cutNum ]	= velLo;
                    velCutHi[ cutNum ]	= smp.velLo.value;
                    freqCutLo[ cutNum ]	= freqLo;
                    freqCutHi[ cutNum ]	= freqHi;
                    cutNum++;
                }
                if( (smp.freqLo.value >= freqMid) && (smp.freqLo.value > freqLo) ) {	// cut top border
                    velCutLo[ cutNum ]	= velLo;
                    velCutHi[ cutNum ]	= velHi;
                    freqCutLo[ cutNum ]	= freqLo;
                    freqCutHi[ cutNum ]	= smp.freqLo.value;
                    cutNum++;
                }
                if( (smp.freqHi.value <= freqMid) && (smp.freqHi.value < freqHi) ) {	// cut bottom
                    velCutLo[ cutNum ]	= velLo;
                    velCutHi[ cutNum ]	= velHi;
                    freqCutLo[ cutNum ]	= smp.freqHi.value;
                    freqCutHi[ cutNum ]	= freqHi;
                    cutNum++;
                }
// ((SmpBox) smpBoxes.elementAt( i )).setSelected( SB_STATE_SELECTED );
// Graphics g2 = ((SmpBox) smpBoxes.elementAt( i )).getGraphics();
// ((SmpBox) smpBoxes.elementAt( i )).paint( g2 );

                bestAspect	= 0.0;
                bestCut		= -1;
                for( j = 0; j < cutNum; j++ ) {		// find "best" cut (i.e. the one mostly quadratic)

// Graphics g = getGraphics();
// g.setXORMode( getBackground() );
// pt1	= paramSpaceToScreen( velCutLo[ j ], freqCutHi[ j ] );
// pt2	= paramSpaceToScreen( velCutHi[ j ], freqCutLo[ j ] );
// g.drawRect( pt1.x, pt1.y, pt2.x - pt1.x - 1, pt2.y - pt1.y - 1 );
// g.drawLine( pt1.x, pt1.y, pt2.x, pt2.y );
// g.drawLine( pt1.x, pt2.y, pt2.x, pt1.y );
// try { Thread.currentThread().sleep( 1000 ); } catch( InterruptedException e99 ) {}
// g.drawRect( pt1.x, pt1.y, pt2.x - pt1.x - 1, pt2.y - pt1.y - 1 );
// g.drawLine( pt1.x, pt1.y, pt2.x, pt2.y );
// g.drawLine( pt1.x, pt2.y, pt2.x, pt1.y );
// g.dispose();

                    aspect = (velCutHi[ j ] - velCutLo[ j ]) / (freqCutHi[ j ] - freqCutLo[ j ]) *
                             spaceAspect;
                    if( aspect > 1.0 ) aspect = 1/aspect;
                    if( aspect > bestAspect ) {
                        bestAspect	= aspect;
                        bestCut		= j;
                    }
                }
// try { Thread.currentThread().sleep( 1000 ); } catch( InterruptedException e98 ) {}
// ((SmpBox) smpBoxes.elementAt( i )).setSelected( SB_STATE_NORMAL );
// ((SmpBox) smpBoxes.elementAt( i )).paint( g2 );
// g2.dispose();
                if( bestCut == -1 ) return false;	// no place to grow

                velLo	= velCutLo[ bestCut ];
                velHi	= velCutHi[ bestCut ];
                freqLo	= freqCutLo[ bestCut ];
                freqHi	= freqCutHi[ bestCut ];
            }	// loop
        }

        tl.x	= velLo;
        tl.y	= freqHi;
        br.x	= velHi;
        br.y	= freqLo;

        pt1	= paramSpaceToScreen( velLo, freqHi );
        pt2	= paramSpaceToScreen( velHi, freqLo );
        return !(((pt2.x - pt1.x) < 6) || ((pt2.y - pt1.y) < 6));

    }

    /*
     *	Beschneidet eine SmpZone-Flaeche, so dass sie nicht mit anderen Zonen ueberlappt
     *
     *	@param	tl			zu beschneidende Koordinaten links/oben
     *	@param	br			zu beschneidende Koordinaten rechts/unten
     *	@param	otl			alte Koordinate
     *	@param	obr			alte Koordinate
     *	@param	dragType	DRAG_LEFT etc.
     *	@return				false, wenn Beschnitt nicht moeglich (vorherige Koordinaten
     *						sollten wiederhergestellt werden)
     */
    protected boolean cutTheCheese( SmpZone smp, DoublePoint tl, DoublePoint br,
                                    DoublePoint otl, DoublePoint obr )
    {
        int		num;
        SmpZone	smp2;
        boolean	iHoriz, iVert;
        boolean	orientL, orientR, orientT, orientB;
        double	freqSpan, velSpan;
        boolean	intersected;
        int		i;
        long	gedultsFaden	= System.currentTimeMillis() + 500;

        freqSpan	= smp.freqHi.value - smp.freqLo.value;
        velSpan		= smp.velHi.value - smp.velLo.value;

        synchronized( smpMap ) {

            num = smpMap.size();
            do {
                for( i = 0, intersected = false; i < num; i++ ) {	// step through all zones

                    smp2 = smpMap.getSample( i );
                    if( (smp2 == null) || (smp2 == smp) ) continue;		// actually data is corrupted; anyway

                    iHoriz	= (smp2.velLo.value < br.x) && (smp2.velHi.value > tl.x);
                    iVert	= (smp2.freqHi.value > br.y) && (smp2.freqLo.value < tl.y);
                    if( !iHoriz || !iVert ) continue;		// no intersection

                    intersected = true;

                    orientL	= obr.x <= smp2.velLo.value + Constants.suckyDoubleError;
                    orientR	= otl.x >= smp2.velHi.value - Constants.suckyDoubleError;
                    orientT	= obr.y >= smp2.freqHi.value - Constants.suckyDoubleError;
                    orientB	= otl.y <= smp2.freqLo.value + Constants.suckyDoubleError;

                    if( orientL && ((dragType & DRAG_RIGHT) != 0) ) {
                        br.x = smp2.velLo.value;
                        if( (dragType & DRAG_LEFT) != 0 ) {
                            tl.x = br.x - velSpan;
                        }
                    }
                    if( orientR && ((dragType & DRAG_LEFT) != 0) ) {
                        tl.x = smp2.velHi.value;
                        if( (dragType & DRAG_RIGHT) != 0 ) {
                            br.x = tl.x + velSpan;
                        }
                    }
                    if( orientT && ((dragType & DRAG_BOTTOM) != 0) ) {
                        br.y = smp2.freqHi.value;
                        if( (dragType & DRAG_TOP) != 0 ) {
                            tl.y = br.y + freqSpan;
                        }
                    }
                    if( orientB && ((dragType & DRAG_TOP) != 0) ) {
                        tl.y = smp2.freqLo.value;
                        if( (dragType & DRAG_BOTTOM) != 0 ) {
                            br.y = tl.y - freqSpan;
                        }
                    }
                }
            } while( intersected && (System.currentTimeMillis() < gedultsFaden) );

            if( intersected ) {
                tl.x	= otl.x;
                tl.y	= otl.y;
                br.x	= obr.x;
                br.y	= obr.y;
            }
        }

        return !intersected;
    }


// -------- interne SmpBox-Klasse --------

    class SmpBox
    extends Canvas
    {
    // ........ private variables ........

        // Status wie STATE_NORMAL, selektiert etc.
        private int state = SmpMapPanel.SB_STATE_UNKNOWN;

        private int			base		= 0;
        private	String		name		= "";
        private int			nameWidth	= 0;
        private Font		fnt;
        private FontMetrics	fntMetr;

        private Color normalColor	= SystemColor.control;
        private Color activeColor	= OpIcon.selectColor;

    // ........ public methods ........

        public SmpBox()
        {
            super();

            Font fnt = getFont();
            fntMetr		= getFontMetrics( fnt );

            setSelected( SmpMapPanel.SB_STATE_NORMAL );
            setSize( getPreferredSize() );
            setLocation( 0, 0 );

            // Event handling
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);

            setFocusable( false );
        }

        /**
         *	Basisfrequenz-Linie setzen
         */
        public void setBase( int base )
        {
            if( this.base != base ) {
                this.base = base;
                repaint();
            }
        }

        /**
         *	Namen setzen
         */
        public void setName( String name )
        {
            this.name	= name;
            nameWidth	= fntMetr.stringWidth( name );
            repaint();
        }

        /**
         *	Status veraendern
         *
         *	@return	vorheriger Status
         */
        public int setSelected( int state )
        {
            int lastState	= this.state;
            this.state		= state;

            if( lastState != state ) {
                if( state == SmpMapPanel.SB_STATE_NORMAL ) {
                    setForeground( normalColor );
                    setBackground( normalColor );
                } else {
                    setForeground( activeColor );
                    setBackground( activeColor );
                }
                repaint();
            }
            return lastState;
        }

        public int isSelected()
        {
            return state;
        }

        /**
         *	Farbe aendern
         *
         *	@param	hue		Farbwert im HSB System (0...1)
         */
        public void setColor( float hue )
        {
            int lastState	= this.state;
            this.state		= SmpMapPanel.SB_STATE_UNKNOWN;

            normalColor	= new Color( Color.HSBtoRGB( hue, 0.4f, 0.8f ));
            activeColor	= new Color( Color.HSBtoRGB( hue, 1.0f, 0.6f ));
            setSelected( lastState );	// invokes repaint()
        }

        public void paint( Graphics g )
        {
            Dimension d = getSize();

            g.draw3DRect( 1, 1, d.width - 3, d.height - 3, true );
            g.setColor( Color.black );
            g.drawRect( 0, 0, d.width - 1, d.height - 1 );

            if( state == SmpMapPanel.SB_STATE_SELECTED ) {
                g.setColor( Color.white );
            }

            // Basisfrequenz symbolisieren
            if( base < 2 ) {						// arrow up
                g.drawLine( d.width - 11, 5, d.width - 7, 1 );
                g.drawLine( d.width -  6, 2, d.width - 3, 5 );
            } else if( base < d.height - 2 ) {		// line
                if( base > fntMetr.getHeight() ) {
                    g.drawLine( 2, base, d.width - 3, base );
                } else {
                    g.drawLine( 3 + nameWidth, base, d.width - 3, base );
                }
            } else {								// arrow down
                g.drawLine( d.width - 11, d.height - 6, d.width - 7, d.height - 2 );
                g.drawLine( d.width -  6, d.height - 3, d.width - 3, d.height - 6 );
            }

            g.drawString( name, 2, fntMetr.getAscent() + 1 );
        }

        public Dimension getPreferredSize()
        {
            return new Dimension( 6, 6 );
        }
        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }

    // ........ private methods ........

        protected void processMouseEvent( MouseEvent e )
        {
            if( e.getID() == MouseEvent.MOUSE_PRESSED ) {
                requestFocus();
            }
            super.processMouseEvent( e );
        }
    }
    // class SmpBox
}