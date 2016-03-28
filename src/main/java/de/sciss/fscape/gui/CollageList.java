/*
 *  CollageList.java
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 *  Panel containing a list of sound file
 *	names and list edit buttons to add
 *	or remove entries.
 */
public class CollageList
        extends Panel
        implements ActionListener, AdjustmentListener, MouseListener {

// -------- private variables --------

    // dieser Button wird nicht angezeigt, sondern dient nur als
    // Institution zum Verwalten der ActionListener und fuer
    // den Event-Dispatch!
    private Button		actionComponent;

    protected Scrollbar		ggVScroll;
//	protected Scrollbar		ggHScroll;
    protected JButton		ggAdd, ggDel, ggDup;

// -------- public methods --------

    public CollageList()
    {
        super();

        actionComponent		= new Button();

        Panel toolBar;

        setLayout( new BorderLayout() );
//		ggHScroll	= new Scrollbar( Scrollbar.HORIZONTAL, 0, 0x8000, 0, 0x8000 );
        ggVScroll	= new Scrollbar( Scrollbar.VERTICAL,   0, 0x8000, 0, 0x8000 );
//		ggHScroll.addAdjustmentListener( ggCurve );
        ggVScroll.addAdjustmentListener( this );

        toolBar		= new Panel();
        toolBar.setLayout( new FlowLayout( FlowLayout.LEFT, 2, 2 ) );
        ggAdd		= new JButton( "    Add    " );
        ggDel		= new JButton( "  Delete  " );
        ggDup		= new JButton( " Duplicate " );
        ggAdd.addActionListener( this );
        ggDel.addActionListener( this );
        ggDup.addActionListener( this );
        toolBar.add( ggAdd );
        toolBar.add( ggDel );
        toolBar.add( ggDup );

        add( toolBar,   BorderLayout.NORTH );
//		add( ggHScroll, BorderLayout.SOUTH );
        add( ggVScroll, BorderLayout.EAST );
        add( new CollageCanvas( this ), BorderLayout.CENTER );
//		add( ggCurve,   BorderLayout.CENTER );
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

// -------- Action Listener methods --------

    public void actionPerformed( ActionEvent e ) {}

// -------- Adjustment Listener methods --------

    public void adjustmentValueChanged( AdjustmentEvent e ) {}

// -------- Mouse Listener methods --------

    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}

// -------- private methods --------

// -------- interne CollageCanvas-Klasse --------

    class CollageCanvas
    extends Canvas
    {
    // ........ private variables ........

        CollageList cl;

    // ........ public methods ........

        public CollageCanvas(CollageList cl) {
            super();

            this.cl = cl;
            setForeground(SystemColor.control);
        }

        public void paint(Graphics g) {
            Dimension dim = getSize();

            g.draw3DRect(1, 1, dim.width - 3, dim.height - 3, true);
            g.setColor(Color.black);
            g.drawRect(0, 0, dim.width - 1, dim.height - 1);
        }

//		public Dimension getPreferredSize()
//		{
//			return new Dimension( 64, 64 );
//		}
//		public Dimension getMinimumSize()
//		{
//			return getPreferredSize();
//		}

    }
    // class CollageCanvas
}