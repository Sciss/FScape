/*
 *  EnvIcon.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.fscape.util.Curve;
import de.sciss.fscape.util.DoublePoint;
import de.sciss.fscape.util.Envelope;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *  Tool icon with a small representation
 *	of a breakpoint envelope.
 */
public class EnvIcon
        extends ToolIcon {

// -------- private variables --------

    private Envelope		env		= null;

    private static final Color colrNormalL  = new Color(0x00, 0x00, 0x30, 0xFF);
    private static final Color colrGhostedL = new Color(0x00, 0x00, 0x00, 0x7F);
    private static final Color colrNormalD  = new Color(0xC8, 0xC8, 0xC8, 0xFF);
    private static final Color colrGhostedD = new Color(0xC8, 0xC8, 0xC8, 0x7F);

// -------- public methods --------

    private final boolean isDark = UIManager.getBoolean("dark-skin");

    public EnvIcon(final Component parent) {
        super(ToolIcon.ID_EDITENV, null);

//		win = parent;
        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!isEnabled() || (env == null) || !contains(e.getPoint())) return;

                EditEnvDlg envDlg;
                Envelope result;

                envDlg = new EditEnvDlg(parent, env);
                envDlg.setVisible(true);

                result = envDlg.getEnvelope();
                if (result != null) {        // "Ok"
                    setEnv(result);
                }

                envDlg.dispose();
            }
        });
    }

    public void setEnv(Envelope env) {
        this.env = env;
        repaint();
    }

    public Envelope getEnv()
    {
        return env;
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isEnabled() ? (isDark ? colrNormalD : colrNormalL) : (isDark ? colrGhostedD : colrGhostedD));
        drawPictogram(g2);

        super.paintComponent(g);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension( d.width + (d.width >> 1), d.height );
    }
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

// -------- protected methods --------

    /*
     *	Berechnet aus der Envelope eine Pictogram-Darstellung (Polyline)
     */
    private void drawPictogram(Graphics g) {
        if (env == null) return;

        final int			width			= getWidth();
        final int			height			= getHeight();
        Curve[]				curve			= new Curve[ 3 ];
        double[]			xScale			= { 0.0, 0.0, 0.0 };
        double				fullScale		= 0.0;
        DoublePoint			pt;
        int					numPt			= 0;
        int[]				x, y;
        double				spaceWidth, spaceHeight;

        synchronized( env ) {
            if( env.atkState )	curve[ 0 ]	= env.atkCurve;
            if( env.susState )	curve[ 1 ]	= env.susCurve;
            if( env.rlsState )	curve[ 2 ]	= env.rlsCurve;
            for( int i = 0; i < 3; i++ ) {
                if( curve[ i ] != null ) {
                    xScale[ i ]	= (i == 1) ? 0.5 : 0.25;
                    numPt	   += curve[ i ].size();
                    fullScale  += xScale[ i ];
                }
            }
            if( numPt == 0 ) return;

            xScale[ 0 ] /= fullScale;
            xScale[ 1 ] /= fullScale;
            xScale[ 2 ] /= fullScale;

            x	= new int[ numPt ];
            y	= new int[ numPt ];

            for( int i = 0, xOffset = 2, k = 0; i < 3; i++ ) {
                if( curve[ i ] == null ) continue;

                spaceWidth	= (curve[ i ].hSpace.max - curve[ i ].hSpace.min);
                spaceHeight	= (curve[ i ].vSpace.max - curve[ i ].vSpace.min);

                for( int j = 0; j < curve[ i ].size(); j++, k++ ) {

                    pt	= curve[ i ].getPoint( j );

                    x[ k ]	= xOffset + (int) ((double) (width-3) * xScale[ i ] *
                              (pt.x - curve[ i ].hSpace.min) / spaceWidth );
                    y[ k ]	= 1 + (int) ((double) (height-3) *
                              (curve[ i ].vSpace.max - pt.y) / spaceHeight );
                }
                xOffset += (int) (xScale[ i ] * (width-1));
            }
        }

        g.drawPolyline( x, y, numPt );
    }
}