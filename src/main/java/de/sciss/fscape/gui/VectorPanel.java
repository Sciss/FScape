/*
 *  VectorPanel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		05-Mar-05	created
 */

package de.sciss.fscape.gui;

import de.sciss.gui.Axis;
import de.sciss.gui.VectorSpace;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *	GUI container with a VectorDisplay,
 *	axis, and mouse cross-hair information features.
 */
public class VectorPanel
        extends JPanel
        implements TopPainter {

    public static final int	FLAG_HLOG_GADGET	= 0x01;
    public static final int	FLAG_VLOG_GADGET	= 0x02;
    public static final int	FLAG_UPDATE_GADGET	= 0x04;

    private static final int GADGET_MASK		= 0x07;

    private final VectorPanel.Client client;
    private VectorSpace			space			= null;

    // GUI components
    private final VectorDisplay	ggVectorDisplay;
    private final Axis hAxis, vAxis;
    private JCheckBox			ggHLog			= null;
    private JCheckBox			ggVLog			= null;

    // mouse listening
    private FontMetrics fntMetrics;
    private static final Color	colrCross		= new Color(0x00, 0x00, 0x00, 0x7F);
    private static final Color	colrTextBg		= new Color(0xFF, 0xFF, 0xFF, 0xA0);
    private final Cursor		csrCrossHair	= new Cursor(Cursor.CROSSHAIR_CURSOR);
    private String				lastTxt;
    private boolean				paintCrossHair	= false;
    private Point				lastPt			= null;		// X-Hair

    public VectorPanel(VectorPanel.Client client, int flags) {
        super(new BorderLayout());

        final Color c1 = getForeground();
        final Color c2 = getBackground();
        final boolean isDark = UIManager.getBoolean("dark-skin");

        JPanel							buttonPane;
        JButton							ggUpdate;
        Box								box;
        ActionListener					actionListener;
        MouseInputAdapter				mouseListener;

        this.client		= client;

        // ---- create vector display ----
        ggVectorDisplay = new VectorDisplay(isDark);
        ggVectorDisplay.addTopPainter(this);
        ggVectorDisplay.setCursor(csrCrossHair);

        mouseListener = new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                ggVectorDisplay.requestFocus();
                redrawCrossHair(e);
            }

            public void mouseDragged(MouseEvent e) {
                redrawCrossHair(e);
            }
        };
        ggVectorDisplay.addMouseListener(mouseListener);
        ggVectorDisplay.addMouseMotionListener(mouseListener);

        this.add(ggVectorDisplay, BorderLayout.CENTER);

        // ---- create south button panel gadgets ----
        if( (flags & GADGET_MASK) != 0 ) {
            buttonPane		= new JPanel( new FlowLayout( FlowLayout.LEADING, 4, 1 ));
            actionListener	= new ActionListener() {
                public void actionPerformed( ActionEvent e )
                {
                    requestUpdate();
                }
            };
            if( (flags & FLAG_UPDATE_GADGET) != 0 ) {
                ggUpdate		= new JButton("Update");
                ggUpdate.addActionListener( actionListener );
                buttonPane.add( ggUpdate );
            }
            if( (flags & FLAG_HLOG_GADGET) != 0 ) {
                ggHLog		= new JCheckBox("Horiz.log.");
                ggHLog.addActionListener( actionListener );
                buttonPane.add( ggHLog );
            }
            if( (flags & FLAG_VLOG_GADGET) != 0 ) {
                ggVLog		= new JCheckBox("Vert.log.");
                ggVLog.addActionListener( actionListener );
                buttonPane.add( ggVLog );
            }
            this.add( buttonPane, BorderLayout.SOUTH );
        }

        // ---- create axis gadgets ----
        hAxis   = new Axis(Axis.HORIZONTAL);
        vAxis   = new Axis(Axis.VERTICAL  );
        box     = Box.createHorizontalBox();
        box.add(Box.createHorizontalStrut(vAxis.getPreferredSize().width));
        box.add(hAxis);
        this.add(box  , BorderLayout.NORTH);
        this.add(vAxis, BorderLayout.WEST );

        fntMetrics = getFontMetrics(getFont());
        this.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("font")) {
                    fntMetrics = getFontMetrics((Font) e.getNewValue());
                }
            }
        });
    }

    protected void redrawCrossHair(MouseEvent e) {
        Dimension	dim			= ggVectorDisplay.getSize();
        int			x			= e.getX();
        int			y			= e.getY();
        String		xTxt, yTxt;
        int			dataLen;
        double		dx, dy;
        float[]		v;
        boolean		hLog		= ggHLog != null && ggHLog.isSelected();
        boolean		vLog		= ggVLog != null && ggVLog.isSelected();

        paintCrossHair	= false;

        if ((space != null) && !e.isAltDown()) {

            v		= ggVectorDisplay.getVector();
            dataLen	= v.length;
            if( dataLen < 1 ) return;

            if ((x >= 0) && (y >= 0) && (x < dim.width) && (y < dim.height)) {
                if (e.isShiftDown()) {
                    dy = space.vUnityToSpace(1.0 - (double) y / (dim.height - 1));
                    dx = space.hUnityToSpace((double) x / (dim.width - 1));
                } else {
                    x  = (int) ((double) x / (dim.width - 1) * (dataLen - 1) + 0.5);
                    dy = v[x];
                    dx = space.hUnityToSpace((double) x / (dataLen - 1));
                    y  = (int) ((1.0 - space.vSpaceToUnity(dy)) * (dim.height - 1) + 0.5);
                    x  = e.getX(); // (int) (rec.space.hSpaceToUnity( dx ) * (dim.width - 1) + 0.5);
                }
                lastPt = new Point(x, y);

                yTxt = client.formatVText(dy, vLog);
                xTxt = client.formatHText(dx, hLog);
                lastTxt = yTxt + " @ " + xTxt;
                paintCrossHair = true;
            }
        }
        ggVectorDisplay.repaint();
    }

    protected void requestUpdate() {
        boolean hLog = ggHLog != null && ggHLog.isSelected();
        boolean vLog = ggVLog != null && ggVLog.isSelected();

        client.requestUpdate(hLog, vLog);
    }

    public void setVector(float[] data) {
        ggVectorDisplay.setVector(this, data);
    }

    public void setSpace(VectorSpace space) {
        this.space = space;
        ggVectorDisplay.setMinMax((float) space.vmin, (float) space.vmax);
        hAxis.setSpace(space);
        vAxis.setSpace(space);
    }

// -------- TopPainter methods --------

    public void paintOnTop(Graphics2D g) {
        Dimension dim = ggVectorDisplay.getSize();

        if (paintCrossHair) {
            g.setColor(colrCross);
            g.drawLine(0, lastPt.y, dim.width - 1, lastPt.y);
            g.drawLine(lastPt.x, 0, lastPt.x, dim.height - 1);
            g.setColor(colrTextBg);
            g.fillRect(1, 1, fntMetrics.stringWidth(lastTxt) + 6, fntMetrics.getHeight() + 4);
            g.setColor(Color.blue);
            g.drawString(lastTxt, 4, fntMetrics.getHeight() + 1);
        }
    }

// -------- internal client class --------

    public interface Client {
        public void requestUpdate(boolean hLog, boolean vLog);

        public String formatVText(double vValue, boolean vLog);

        public String formatHText(double hValue, boolean hLog);
    }
}