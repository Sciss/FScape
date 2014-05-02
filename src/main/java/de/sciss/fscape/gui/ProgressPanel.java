/*
 *  ProgressPanel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
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
 *		03-Oct-06	added methods reqiured by ProcessPanel
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import javax.swing.*;

import de.sciss.gui.GUIUtil;
import de.sciss.gui.ProgressComponent;
import de.sciss.icons.raphael.Shapes;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 30-Dec-06
 */
public class ProgressPanel
extends JPanel
implements ProgressComponent
{
    private final ProgressBar	pb;
    private final JLabel		lb;
    private final JButton	    ggCancel;

    public ProgressPanel()
    {
        super();

        pb			= new ProgressBar();
        lb			= new JLabel( "", SwingConstants.RIGHT );
        lb.setBorder( BorderFactory.createEmptyBorder( 0, 8, 0, 4 )); // T L B R
        // ggCancel	= new ModificationButton( ModificationButton.SHAPE_ABORT );
        Path2D p = new GeneralPath();
        Shapes.Cross(p);
        // Shapes.ListView(p);
        // Shapes.Picker(p);
        final Shape icnShp = p.createTransformedShape(AffineTransform.getScaleInstance(0.5, 0.5));

        Icon icnCancel = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(ggCancel.isEnabled() ? Color.black : Color.gray);
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.fill(icnShp);
                g2.translate(-x, -y);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };

        ggCancel = new JButton(icnCancel);
        ggCancel.setEnabled(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(lb);
        add(pb);
        add(ggCancel);
    }

    public void pause()
    {
        pb.pause();
    }

    public void resume()
    {
        pb.resume();
    }

    public void setPaint(Paint c) {
        pb.setPaint(c);
    }

// ---------------- ProgressComponent interface ---------------- 

    public void addCancelListener( ActionListener l )
    {
        ggCancel.addActionListener( l );
    }

    public void removeCancelListener( ActionListener l )
    {
        ggCancel.removeActionListener( l );
    }

    public Component getComponent()
    {
        return this;
    }

    public void resetProgression()
    {
        pb.reset();
        ggCancel.setEnabled( true );
    }

    public void setProgression( float p )
    {
        if( p >= 0 ) {
            pb.setProgression( p );
        } else {
            pb.setIndeterminate( true );
        }
    }

    public void	finishProgression( int result )
    {
        pb.finish( result );
        ggCancel.setEnabled( false );
    }

    public void setProgressionText(String text) {
        lb.setText(text);
    }

    public void displayError( Exception e, String processName )
    {
        GUIUtil.displayError( this, e, processName );
    }

    public void showMessage( int type, String text )
    {
System.out.println( text );
/*
        // potentially condidates of unicodes
        // for the different messages types are:
        // ERROR_MESSAGE		2620  21AF
        // INFORMATION_MESSAGE  24D8'(i)' 2148'i' 2139'i'
        // PLAIN_MESSAGE
        // QUESTION_MESSAGE		2047
        // WARNING_MESSAGE		261D  2297'X'  203C

        // the print stream is using bytes not unicode,
        // therefore the 'icons' are appended directly
        // to the textarea (so they won't appear in a
        // logfile which is quite unnecessary anyway).
        switch( type ) {
        case JOptionPane.ERROR_MESSAGE:
            lta.append( "\u21AF " );		// Blitz
            break;
        case JOptionPane.INFORMATION_MESSAGE:
            lta.append( "\u263C " );		// Sun
            break;
        case JOptionPane.QUESTION_MESSAGE:
            lta.append( "\u2047 " );		// '??'
            break;
        case JOptionPane.WARNING_MESSAGE:
            lta.append( "\u203C " );		// '!!'
            break;
        default:
            lta.append( "   " );
            break;
        }
        // due to inserting unicode characters we have to
        // advance manually to keep the scrollpane working for us.
// 		lta.setCaretPosition( lta.getText().length() );
        logStream.println( text );
*/
    }
}