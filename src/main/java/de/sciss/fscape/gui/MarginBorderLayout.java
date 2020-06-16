/*
 *  MarginBorderLayout.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import java.awt.*;

/** 
 * BorderLayout has all relevant fields private, so
 * we essential have to duplicate all functionality.
 */
public class MarginBorderLayout implements LayoutManager2 {
    private final Insets  margin;
    private int     hGap;
    private int     vGap;

    Component north;
    Component west;
    Component east;
    Component south;
    Component center;
    Component firstLine;
    Component lastLine;
    Component firstItem;
    Component lastItem;

    public static final String NORTH                = "North";
    public static final String SOUTH                = "South";
    public static final String EAST                 = "East";
    public static final String WEST                 = "West";
    public static final String CENTER               = "Center";
    public static final String BEFORE_FIRST_LINE    = "First";
    public static final String AFTER_LAST_LINE      = "Last";
    public static final String BEFORE_LINE_BEGINS   = "Before";
    public static final String AFTER_LINE_ENDS      = "After";
    public static final String PAGE_START           = BEFORE_FIRST_LINE;
    public static final String PAGE_END             = AFTER_LAST_LINE;
    public static final String LINE_START           = BEFORE_LINE_BEGINS;
    public static final String LINE_END             = AFTER_LINE_ENDS;

    public MarginBorderLayout() {
        this(0, 0);
    }

    public MarginBorderLayout(int hGap, int vGap) {
        this(hGap, vGap, new Insets(0, 0, 0, 0));
    }

    public MarginBorderLayout(Insets margin) {
        this(0, 0, margin);
    }

    public MarginBorderLayout(int hGap, int vGap, Insets margin) {
        this.hGap   = hGap;
        this.vGap   = vGap;
        this.margin = (Insets) margin.clone();
    }

    public int getHgap() {
        return hGap;
    }
    public void setHgap(int hGap) {
        this.hGap = hGap;
    }
    public int getVgap() {
        return vGap;
    }
    public void setVgap(int vGap) {
        this.vGap = vGap;
    }
    public Insets getMargin() {
        return (Insets) margin.clone();
    }
    public void setMargin(Insets margin) {
        this.margin.left    = margin.left;
        this.margin.top     = margin.top;
        this.margin.right   = margin.right;
        this.margin.bottom  = margin.bottom;
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = null;

            if ((c=getChild(EAST,ltr)) != null) {
                Dimension d = c.getMinimumSize();
                dim.width += d.width + hGap;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(WEST,ltr)) != null) {
                Dimension d = c.getMinimumSize();
                dim.width += d.width + hGap;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(CENTER,ltr)) != null) {
                Dimension d = c.getMinimumSize();
                dim.width += d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(NORTH,ltr)) != null) {
                Dimension d = c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vGap;
            }
            if ((c=getChild(SOUTH,ltr)) != null) {
                Dimension d = c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vGap;
            }

            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            dim.width  += margin.left + margin.right;
            dim.height += margin.top  + margin.bottom;
            return dim;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = null;

            if ((c=getChild(EAST,ltr)) != null) {
                Dimension d = c.getPreferredSize();
                dim.width += d.width + hGap;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(WEST,ltr)) != null) {
                Dimension d = c.getPreferredSize();
                dim.width += d.width + hGap;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(CENTER,ltr)) != null) {
                Dimension d = c.getPreferredSize();
                dim.width += d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            if ((c=getChild(NORTH,ltr)) != null) {
                Dimension d = c.getPreferredSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vGap;
            }
            if ((c=getChild(SOUTH,ltr)) != null) {
                Dimension d = c.getPreferredSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vGap;
            }

            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            dim.width  += margin.left + margin.right;
            dim.height += margin.top  + margin.bottom;
            return dim;
        }
    }

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    public void invalidateLayout(Container target) {}

    private Component getChild(String key, boolean ltr) {
        Component result = null;

        switch (key) {
            case NORTH:
                result = (firstLine != null) ? firstLine : north;
                break;
            case SOUTH:
                result = (lastLine != null) ? lastLine : south;
                break;
            case WEST:
                result = ltr ? firstItem : lastItem;
                if (result == null) {
                    result = west;
                }
                break;
            case EAST:
                result = ltr ? lastItem : firstItem;
                if (result == null) {
                    result = east;
                }
                break;
            case CENTER:
                result = center;
                break;
        }
        if (result != null && !result.isVisible()) {
            result = null;
        }
        return result;
    }

    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            if ((constraints == null) || (constraints instanceof String)) {
                addLayoutComponent((String)constraints, comp);
            } else {
                throw new IllegalArgumentException("cannot add to layout: constraint must be a string (or null)");
            }
        }
    }

    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
        /* Special case:  treat null the same as "Center". */
            if (name == null) {
                name = "Center";
            }

        /* Assign the component to one of the known regions of the layout.
         */
            switch (name) {
                case "Center":
                    center = comp;
                    break;
                case "North":
                    north = comp;
                    break;
                case "South":
                    south = comp;
                    break;
                case "East":
                    east = comp;
                    break;
                case "West":
                    west = comp;
                    break;
                case BEFORE_FIRST_LINE:
                    firstLine = comp;
                    break;
                case AFTER_LAST_LINE:
                    lastLine = comp;
                    break;
                case BEFORE_LINE_BEGINS:
                    firstItem = comp;
                    break;
                case AFTER_LINE_ENDS:
                    lastItem = comp;
                    break;
                default:
                    throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
            }
        }
    }

    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == center) {
                center = null;
            } else if (comp == north) {
                north = null;
            } else if (comp == south) {
                south = null;
            } else if (comp == east) {
                east = null;
            } else if (comp == west) {
                west = null;
            }
            if (comp == firstLine) {
                firstLine = null;
            } else if (comp == lastLine) {
                lastLine = null;
            } else if (comp == firstItem) {
                firstItem = null;
            } else if (comp == lastItem) {
                lastItem = null;
            }
        }
    }


    public Component getLayoutComponent(Object constraints) {
        if (CENTER.equals(constraints)) {
            return center;
        } else if (NORTH.equals(constraints)) {
            return north;
        } else if (SOUTH.equals(constraints)) {
            return south;
        } else if (WEST.equals(constraints)) {
            return west;
        } else if (EAST.equals(constraints)) {
            return east;
        } else if (PAGE_START.equals(constraints)) {
            return firstLine;
        } else if (PAGE_END.equals(constraints)) {
            return lastLine;
        } else if (LINE_START.equals(constraints)) {
            return firstItem;
        } else if (LINE_END.equals(constraints)) {
            return lastItem;
        } else {
            throw new IllegalArgumentException("cannot get component: unknown constraint: " + constraints);
        }
    }


    public Component getLayoutComponent(Container target, Object constraints) {
        boolean ltr = target.getComponentOrientation().isLeftToRight();
        Component result = null;

        if (NORTH.equals(constraints)) {
            result = (firstLine != null) ? firstLine : north;
        } else if (SOUTH.equals(constraints)) {
            result = (lastLine != null) ? lastLine : south;
        } else if (WEST.equals(constraints)) {
            result = ltr ? firstItem : lastItem;
            if (result == null) {
                result = west;
            }
        } else if (EAST.equals(constraints)) {
            result = ltr ? lastItem : firstItem;
            if (result == null) {
                result = east;
            }
        } else if (CENTER.equals(constraints)) {
            result = center;
        } else {
            throw new IllegalArgumentException("cannot get component: invalid constraint: " + constraints);
        }

        return result;
    }


    public Object getConstraints(Component comp) {
        //fix for 6242148 : API method java.awt.BorderLayout.getConstraints(null) should return null
        if (comp == null){
            return null;
        }
        if (comp == center) {
            return CENTER;
        } else if (comp == north) {
            return NORTH;
        } else if (comp == south) {
            return SOUTH;
        } else if (comp == west) {
            return WEST;
        } else if (comp == east) {
            return EAST;
        } else if (comp == firstLine) {
            return PAGE_START;
        } else if (comp == lastLine) {
            return PAGE_END;
        } else if (comp == firstItem) {
            return LINE_START;
        } else if (comp == lastItem) {
            return LINE_END;
        }
        return null;
    }
    
    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets   = target.getInsets();
            int top         = insets.top + margin.top;
            int bottom      = target.getHeight() - (insets.bottom + margin.bottom);
            int left        = insets.left + margin.left;
            int right       = target.getWidth() - (insets.right + margin.right);

            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = null;

            if ((c=getChild(NORTH,ltr)) != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, right - left, d.height);
                top += d.height + vGap;
            }
            if ((c=getChild(SOUTH,ltr)) != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, bottom - d.height, right - left, d.height);
                bottom -= d.height + vGap;
            }
            if ((c=getChild(EAST,ltr)) != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(right - d.width, top, d.width, bottom - top);
                right -= d.width + hGap;
            }
            if ((c=getChild(WEST,ltr)) != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, d.width, bottom - top);
                left += d.width + hGap;
            }
            if ((c=getChild(CENTER,ltr)) != null) {
                c.setBounds(left, top, right - left, bottom - top);
            }
        }
    }

    public String toString() {
        return getClass().getName() + "[hGap=" + hGap + ",vGap=" + vGap + ",margin=" + margin + "]";
    }
}
