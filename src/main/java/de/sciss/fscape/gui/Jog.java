/*
 *  Jog.java
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

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.gui.NumberEvent;
import de.sciss.gui.NumberListener;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class Jog
        extends JComponent
        implements PropertyChangeListener, EventManager.Processor {
    private static final class ColorScheme {
        final Paint pntBack;
        final Paint pntOutline;
        final Paint pntLight;
        final Paint pntArcLight;
        final Paint pntArcShadow;
        final Paint pntBelly;

        ColorScheme(Paint pntBack, Paint pntOutline, Paint pntLight, Paint pntArcLight,
                    Paint pntArcShadow, Paint pntBelly) {
            this.pntBack = pntBack;
            this.pntOutline = pntOutline;
            this.pntLight = pntLight;
            this.pntArcLight = pntArcLight;
            this.pntArcShadow = pntArcShadow;
            this.pntBelly = pntBelly;
        }
    }

    // light normal
    private static final ColorScheme lightScheme = new ColorScheme(
        /* pntBack */ new GradientPaint(
            10, 9, new Color(235, 235, 235),
            10, 19, new Color(248, 248, 248)),
        /* pntOutline */   new Color(40, 40, 40),
        /* pntLight */     new Color(251, 251, 251),
        /* pntArcLight */  new Color(255, 255, 255),
        /* pntArcShadow */ new GradientPaint(
            12, 0, new Color(40, 40, 40, 0xA0),
            8, 15, new Color(40, 40, 40, 0x00)),
        /* pntBelly */ new GradientPaint(
            0, -3, new Color(0x58, 0x58, 0x58),
            0, 3, new Color(0xD0, 0xD0, 0xD0))
    );

    // light disabled
    private static final ColorScheme lightSchemeD = new ColorScheme(
        /* pntBack */ new GradientPaint(
            10, 9, new Color(235, 235, 235, 0x7F),
            10, 19, new Color(248, 248, 248, 0x7F)),
        /* pntOutline */    new Color(40, 40, 40, 0x7F),
        /* pntLight */      new Color(251, 251, 251, 0x7F),
        /* pntArcLight */   new Color(255, 255, 255, 0x7F),
        /* pntArcShadow */  new GradientPaint(
            12, 0, new Color(40, 40, 40, 0x50),
            8, 15, new Color(40, 40, 40, 0x00)),
        /* pntBelly */ new GradientPaint(
            0, -3, new Color(0x58, 0x58, 0x58, 0x7F),
            0, 3, new Color(0xD0, 0xD0, 0xD0, 0x7F))
    );

    // dark normal
    private static final ColorScheme darkScheme = new ColorScheme(
        /* pntBack */ new GradientPaint(
            10, 9, new Color(24, 24, 24),
            10, 19, new Color(32, 32, 32)),
        /* pntOutline */    new Color(0, 0, 0),
//        /* pntLight */      new Color( 36, 36, 36),
            new GradientPaint(
                    0, 1, new Color(72, 72, 72, 0x80),
                    0, 10, new Color(48, 48, 48, 0x40)),
        /* pntArcLight */  new Color(64, 64, 64),
        /* pntArcShadow */ new GradientPaint(
            12, 0, new Color(16, 16, 16, 0xA0),
            8, 15, new Color(16, 16, 16, 0x00)),
        /* pntBelly */ new GradientPaint(
            0, -3, new Color(0x48, 0x48, 0x48),
            0, 3, new Color(0xA0, 0xA0, 0xA0))
    );

    // dark disabled
    private static final ColorScheme darkSchemeD = new ColorScheme(
        /* pntBack */ new GradientPaint(
            10, 9, new Color(24, 24, 24, 0x7F),
            10, 19, new Color(32, 32, 32, 0x7F)),
        /* pntOutline */    new Color(0, 0, 0, 0x7F),
//        /* pntLight */      new Color( 36, 36, 36),
            new GradientPaint(
                    0, 1, new Color(72, 72, 72, 0x40),
                    0, 10, new Color(48, 48, 48, 0x20)),
        /* pntArcLight */  new Color(64, 64, 64),
        /* pntArcShadow */ new GradientPaint(
            12, 0, new Color(16, 16, 16, 0x50),
            8, 15, new Color(16, 16, 16, 0x00)),
        /* pntBelly */ new GradientPaint(
            0, -3, new Color(0x48, 0x48, 0x48, 0x7F),
            0, 3, new Color(0xA0, 0xA0, 0xA0, 0x7F))
    );

    // ---- strokes and shapes ----
    private static final Stroke strkOutline = new BasicStroke(0.5f);
    private static final Stroke strkArcShadow = new BasicStroke(1.2f);
    private static final Stroke strkArcLight = new BasicStroke(1.0f);
    private static final Shape shpBelly = new Ellipse2D.Double(-2.5, -2.5, 5.0, 5.0);
    protected final Point2D bellyPos = new Point2D.Double(-0.7071064, -0.7071064);

    protected static final Cursor dragCursor = new Cursor(Cursor.MOVE_CURSOR);
    protected Cursor savedCursor = null;
    protected int dragX, dragY;
    protected double dragArc;
    protected double dispArc = -2.356194;
    protected boolean reFire = false;    // if true, re-fire a number event after dragging

    private static final double PI2 = Math.PI * 2;

    protected Insets in;

    private EventManager elm = null;    // lazy creation

    private final ColorScheme colors;
    private final ColorScheme colorsD;  // disabled

    public Jog() {
        this(false);
    }

    public Jog(boolean dark) {
        colors = dark ? darkScheme : lightScheme;
        colorsD = dark ? darkSchemeD : lightSchemeD;
        updatePreferredSize();
        setFocusable(true);

        final MouseInputAdapter mia = new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                reFire = false;

                if (!isEnabled()) return;

                requestFocus();
                final Window w = SwingUtilities.getWindowAncestor(Jog.this);
                if (w != null) {
                    savedCursor = w.getCursor();
                    w.setCursor(dragCursor);
                }

                processMouse(e, false);
            }

            public void mouseReleased(MouseEvent e) {
                if (!isEnabled()) return;

                final Window w = SwingUtilities.getWindowAncestor(Jog.this);
                if (w != null) {
                    w.setCursor(savedCursor);
                }

                if (reFire) {
                    dispatchChange(0, false);
                    reFire = false;
                }
            }

            public void mouseDragged(MouseEvent e) {
                if (!isEnabled()) return;

                processMouse(e, true);
            }

            private void processMouse(MouseEvent e, boolean isDrag) {
                final int w = getWidth() - in.left - in.right;
                final int h = getWidth() - in.top - in.bottom;
                double dx, dy;
                double weight, thisArc, deltaArc, newDispArc;
                int dragAmount;

                dx = e.getX() - in.left - w * 0.5;
                dy = e.getY() - in.top - h * 0.5;

                if (isDrag) {
                    thisArc = Math.atan2(dx, dy) + Math.PI;
                    dx /= w;
                    dy /= h;
                    weight = Math.max(0.125, Math.sqrt(dx * dx + dy * dy) / 2);
                    deltaArc = thisArc - dragArc;
                    if (deltaArc < -Math.PI) {
                        deltaArc = PI2 - deltaArc;
                    } else if (deltaArc > Math.PI) {
                        deltaArc = -PI2 + deltaArc;
                    }

                    dx = (e.getX() - dragX); // (double) (e.getX() - dragX) / w;
                    dy = (e.getY() - dragY); // (double) (e.getY() - dragY) / h;
                    dragAmount = (int) (Math.sqrt(dx * dx + dy * dy) * 0.5);
                    newDispArc = (dispArc + ((deltaArc < 0) ? -1 : 1) * Math.min(0.4,
                            weight * dragAmount)) % PI2;

                    if (dragAmount >= 1) {
                        if (dragAmount >= 17) {        // Beschleunigen
                            dragAmount *= (dragAmount - 16);
                        }

                        dispArc = newDispArc;
                        dragArc = thisArc;
                        dragX = e.getX();
                        dragY = e.getY();
                        repaint();

                        dragAmount *= (deltaArc < 0) ? 1 : -1;
                        dispatchChange(dragAmount, true);
                        reFire = true;
                    }
                } else {
                    dragX = e.getX();
                    dragY = e.getY();
                    dragArc = Math.atan2(dx, dy) + Math.PI;
                }

                bellyPos.setLocation(Math.cos(dispArc), Math.sin(dispArc));
                repaint();
            }
        };

        addMouseListener(mia);
        addMouseMotionListener(mia);
        this.addPropertyChangeListener("border", this);
    }

    private void updatePreferredSize() {
        in = getInsets();

        final Dimension d = new Dimension(20 + in.left + in.right, 20 + in.top + in.bottom);

        setMinimumSize(d);
        setPreferredSize(d);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;
        final Stroke strkOrig = g2.getStroke();
        final AffineTransform atOrig = g2.getTransform();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.translate(0.5f + in.left, 0.5f + in.top);    // tricky disco to blur the outlines 'bit more

        final ColorScheme c = isEnabled() ? colors : colorsD;

        g2.setPaint(c.pntBack);
        g2.fillOval(2, 3, 16, 16);
        g2.setPaint(c.pntLight);
        g2.fillOval(5, 1, 9, 10);
        g2.setPaint(c.pntArcShadow);
        g2.setStroke(strkArcShadow);
        g2.drawOval(1, 1, 17, 17);

        g2.setStroke(strkArcLight);
        g2.setPaint(c.pntArcLight);
        g2.drawArc(1, 2, 17, 17, 180, 180);

        g2.setPaint(c.pntOutline);
        g2.setStroke(strkOutline);
        g2.drawOval(1, 1, 17, 17);

        g2.translate(bellyPos.getX() * 4 + 10.0, -bellyPos.getY() * 4.5 + 10.0);
        g2.setPaint(c.pntBelly);
        g2.fill(shpBelly);

        g2.setStroke(strkOrig);
        g2.setTransform(atOrig);
    }

    /**
     * Register a <code>NumberListener</code>
     * which will be informed about changes of
     * the gadgets content.
     *
     * @param listener the <code>NumberListener</code> to register
     */
    public void addListener(NumberListener listener) {
        synchronized (this) {
            if (elm == null) {
                elm = new EventManager(this);
            }
            elm.addListener(listener);
        }
    }

    /**
     * Unregister a <code>NumberListener</code>
     * from receiving number change events.
     *
     * @param listener the <code>NumberListener</code> to unregister
     */
    public void removeListener(NumberListener listener) {
        if (elm != null) elm.removeListener(listener);
    }

    public void processEvent(BasicEvent e) {
        NumberListener listener;

        for (int i = 0; i < elm.countListeners(); i++) {
            listener = (NumberListener) elm.getListener(i);
            switch (e.getID()) {
                case NumberEvent.CHANGED:
                    listener.numberChanged((NumberEvent) e);
                    break;
                default:
                    assert false : e.getID();
            }
        } // for( i = 0; i < elm.countListeners(); i++ )
    }

    protected void dispatchChange(int delta, boolean adjusting) {
        if (elm != null) {
            elm.dispatchEvent(new NumberEvent(this, NumberEvent.CHANGED, System.currentTimeMillis(),
                    new Integer(delta), adjusting));
        }
    }

// ------------------- PropertyChangeListener interface -------------------

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("border")) {
            updatePreferredSize();
        }
    }
}
