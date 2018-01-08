/*
 *  ProgressBar.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		14-Apr-06	created
 *		24-Jun-06	copied from de.sciss.eisenkraut.gui.ProgressPanel
 *		03-Oct-06	added methods required by ProcessPanel
 */

package de.sciss.fscape.gui;

import de.sciss.gui.ProgressComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 *  A bit more sophisticated progression bar
 *  than the default one provided by Swing,
 *  a slim version of the one used in FScape.
 *  This class provides methods for changing
 *  the bar's colour and text. If no text is
 *  set, the bar will try to estimate the
 *  remaining progression time.
 */
public class ProgressBar
        extends JProgressBar {

// -------- private variables --------

    protected float		p			= 0.0f;

    private long		startTime;				// time at which reset() was called
    private long		pauseTime;
    private int			remain;
    private byte		timeStr[];
    protected int		maximum		= 0;

    private String altString = null;

    private static final Color  COLOR_SUCCESS   = new Color( 0x00, 0xFF, 0x00, 0x2F );
    private static final Color  COLOR_FAILURE	= new Color( 0xFF, 0x00, 0x00, 0x2F );

    private Paint pnt   = null;

    private boolean timePainted = false;

// -------- public --------

    /**
     *  Constructs a new horizontal
     *  progression bar
     */
    public ProgressBar() {
        super(SwingConstants.HORIZONTAL, 0, 0x1000);
        // web-laf uses different component heights; therefore leave this flag on
        setStringPainted(true);

        timeStr = "00:00:00".getBytes();

        reset();

        this.addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent e1 )
            {
                Dimension d = getSize();
                maximum		= d.width;
                setMaximum( d.width );
                setValue( (int) (p * maximum) );
            }
        });
    }

    /**
     *  Sets or clears the text that is displayed on top
     *  of the bar.
     *
     *  @param  altString   the text to display on the
     *						bar or <code>null</code> to clear
     *						the text. in this case the bar will
     *						display a remaining-time string
     */
    public void setText(String altString) {
        this.altString = altString;
        setString(altString == null ? "" : altString);
        // setStringPainted( altString != null );
    }

    /**
     *	Sets the progression amount in percent.
     *
     *  Warning:	use only this method to change the progression,
     *				don't use the superclass's setValue method
     *
     *	@param	p	progression, between 0 and 1 (i.e. 0 and 100%)
     */
    public synchronized void setProgression(float p) {
        int			oldProgWidth	= (int) (this.p * maximum);
        this.p						= Math.max( 0.0f, Math.min( 1.0f, p ));
        int			progWidth		= (int) (this.p * maximum);

        int	lastRemain				= remain;
        int	elapsed, hours, minutes, secs;

        if( progWidth != oldProgWidth ) {

            setValue( progWidth );

            if( p > 0f ) {
                elapsed = (int) (System.currentTimeMillis() - startTime) / 1000;
                remain	= (int) (elapsed / p) - elapsed;
                if( (((elapsed >= 10) & (remain >= 10)) | timePainted) & (altString == null) & !isIndeterminate() ) {

                    // only update if changes are reasonable
                    if( (remain < lastRemain) || ((remain - lastRemain) >= 10) ) {

                        secs	= remain % 60;
                        timeStr[ 6 ] = (byte) ((secs / 10) + 48);
                        timeStr[ 7 ] = (byte) ((secs % 10) + 48);
                        minutes	= (remain / 60) % 60;
                        timeStr[ 3 ] = (byte) ((minutes / 10) + 48);
                        timeStr[ 4 ] = (byte) ((minutes % 10) + 48);
                        hours	= (remain / 3600) % 100;
                        timeStr[ 0 ] = (byte) ((hours / 10) + 48);
                        timeStr[ 1 ] = (byte) ((hours % 10) + 48);
                        setString(new String(timeStr));
                        timePainted = true;
                        // setStringPainted(true);

                    } else {
                        remain	= lastRemain;				// vvv avoid flickering
                    }
                }
            }
        }
    }

    /**
     *	Queries the progression
     *
     *	@return	the progression value between 0 and 1 (i.e. 0 and 100%)
     */
    public float getProgression()
    {
        return p;
    }

    /**
     *	Reset progression to zero. Remove any
     *  custom text strings and custom bar colour.
     *  Initializes the remaining-time estimation
     *  with the current system time.
     */
    public void reset() {
        setIndeterminate(false);
        setProgression(0f);
        setPaint(null);
        // setString(""); // null);
        timePainted = false;
        // setStringPainted( false );
        setText(null);
        remain = 1 << 29;
        startTime = System.currentTimeMillis();
    }

    /**
     *	Pauses progression. You have to call
     *  this if the user pauses a progress
     *  because in this case the calculation
     *  of the remaining time should be paused
     *  as well.
     */
    public void pause()
    {
        pauseTime	= System.currentTimeMillis();
    }

    /**
     *	Resumes progression and calculation of
     *  remaining time.
     */
    public void resume()
    {
        startTime  += System.currentTimeMillis() - pauseTime;
    }

    /**
     *	Finish the progression. This clears
     *  custom text strings, clears indeterminate
     *  state and sets the bars colour depending
     *  on the success.
     */
    public void finish(int result) {
        setIndeterminate(false);
        setValue(maximum);
        setString(""); // null);
        timePainted = false;
        // setStringPainted(false);

        if (result == ProgressComponent.DONE) {
            setPaint(COLOR_SUCCESS);
        } else if (result == ProgressComponent.FAILED) {
            setPaint(COLOR_FAILURE);
        }
    }

    /**
     * Sets a custom bar color
     *
     * @param pnt the new color or <code>null</code>
     *            to remove custom color.
     */
    public void setPaint(Paint pnt) {
        this.pnt = pnt;
        repaint();
    }

    /**
     * Queries the custom bar color.
     *
     * @return the bar color or <code>null</code> if
     * no custom color was set
     */
    public Paint getPaint() {
        return pnt;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (pnt == null) return;

        final Dimension     d   = getSize();
        final Graphics2D    g2  = (Graphics2D) g;
        final Paint		    op  = g2.getPaint();

        final String    lafName     = UIManager.getLookAndFeel().getName();
        final boolean   isWebLaF    = lafName.equals("WebLaF");
        final int       top         = isWebLaF ? 3 : 1;
        final int       left        = isWebLaF ? 3 : 2;
        final int       bottom      = isWebLaF ? 3 : 2;
        final int       right       = isWebLaF ? 3 : 2;

        g2.setPaint(pnt);
        g2.fillRect(left, top, d.width - (left + right), d.height - (top + bottom));
        g2.setPaint(op);
    }
}