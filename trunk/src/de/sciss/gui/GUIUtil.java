/*
 *  GUIUtil.java
 *  de.sciss.gui package
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
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
 *		20-May-05	created from de.sciss.meloncillo.gui.GUIUtil
 *		26-May-05	now the 'main' class of the package; getResourceString()
 *		07-Aug-05	setDeepFont() can be called with null font
 *		14-Apr-06	added constrainWidth / constrainHeight
 *		11-Sep-06	added setInitialDialogFocus()
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.event.AncestorEvent;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AncestorAdapter;
import de.sciss.app.Application;
import de.sciss.app.GraphicsHandler;
import de.sciss.app.PreferenceEntrySync;

/**
 *  This is a helper class containing utility static functions
 *  for common Swing / GUI operations
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.35, 14-Oct-06
 */
public class GUIUtil
{
	private static final double VERSION	= 0.35;
	private static final ResourceBundle resBundle = ResourceBundle.getBundle( "GUIUtilStrings" );
	private static final Preferences prefs = Preferences.userNodeForPackage( GUIUtil.class );

	private static Icon icnNoWrite = null;
	
    private GUIUtil() {}

	public static final Preferences getUserPrefs()
	{
		return prefs;
	}

	public static final double getVersion()
	{
		return VERSION;
	}

	public static final String getResourceString( String key )
	{
		try {
			return resBundle.getString( key );
		}
		catch( MissingResourceException e1 ) {
			return( "[Missing Resource: " + key + "]" );
		}
	}
   
	/**
	 *  Displays an error message dialog by
	 *  examining a given <code>Exception</code>. Returns
	 *  after the dialog was closed by the user.
	 *
	 *  @param  component   the component in which to open the dialog.
	 *						<code>null</code> is allowed in which case
	 *						the dialog will appear centered on the screen.
	 *  @param  exception   the exception that was thrown. the message's
	 *						text is displayed using the <code>getLocalizedMessage</code>
	 *						method.
	 *  @param  title		name of the action in which the error occurred
	 *
	 *  @see	javax.swing.JOptionPane#showOptionDialog( Component, Object, String, int, int, Icon, Object[], Object )
	 *  @see	java.lang.Throwable#getLocalizedMessage()
	 */
	public static void displayError( Component component, Exception exception, String title )
	{
		String							message = exception.getClass().getName() + " - " + exception.getLocalizedMessage();
		StringTokenizer					tok;
		final StringBuffer				strBuf  = new StringBuffer( GUIUtil.getResourceString( "errException" ));
		int								lineLen = 0;
		String							word;
		String[]						options = { GUIUtil.getResourceString( "buttonOk" ),
													GUIUtil.getResourceString( "optionDlgStack" )};
	
		if( message == null ) message = exception.getClass().getName();
		tok = new StringTokenizer( message );
		strBuf.append( ":\n" );
		while( tok.hasMoreTokens() ) {
			word = tok.nextToken();
			if( lineLen > 0 && lineLen + word.length() > 40 ) {
				strBuf.append( "\n" );
				lineLen = 0;
			}
			strBuf.append( word );
			strBuf.append( ' ' );
			lineLen += word.length() + 1;
		}
		if( JOptionPane.showOptionDialog( component, strBuf.toString(), title, JOptionPane.YES_NO_OPTION,
									      JOptionPane.ERROR_MESSAGE, null, options, options[0] ) == 1 ) {
			exception.printStackTrace();
		}
	}

	/**
	 *  Convenience method that will add new
	 *  corresponding entries in a button's input and action map,
	 *  such that a given <code>KeyStroke<code> will cause a
	 *  <code>DoClickAction</code> to be performed on that button.
	 *  The key stroke is performed whenever the button is in
	 *  the current focussed window.
	 *
	 *  @param  comp	an <code>AbstractButton</code> to which a
	 *					a new keyboard action is attached.
	 *  @param  stroke  the <code>KeyStroke</code> which causes a
	 *					click on the button.
	 *
	 *  @see	DoClickAction
	 *  @see	javax.swing.JComponent#getInputMap( int )
	 *  @see	javax.swing.JComponent#getActionMap()
	 *  @see	javax.swing.JComponent#WHEN_IN_FOCUSED_WINDOW
	 */
	public static void createKeyAction( AbstractButton comp, KeyStroke stroke )
	{
		comp.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( stroke, "shortcut" );
		comp.getActionMap().put( "shortcut", new DoClickAction( comp ));
	}

	/**
	 *  Set a font for a container
	 *  and all children we can find
	 *  in this container (calling this
	 *  method recursively). This is
	 *  necessary because calling <code>setFont</code>
	 *  on a <code>JPanel</code> does not
	 *  cause the <code>Font</code> of the
	 *  gadgets contained in the panel to
	 *  change their fonts.
	 *
	 *  @param  c		the container to traverse
	 *					for children whose font is to be changed
	 *  @param  fnt		the new font to apply; if <code>null</code>
	 *					the current application's window handler's
	 *					default font is used
	 *
	 *  @see	java.awt.Component#setFont( Font )
	 *	@see	de.sciss.app.WindowHandler#getDefaultFont()
	 */
	public static void setDeepFont( Container c, Font fnt )
	{
		final Component[] comp = c.getComponents();
		
		if( fnt == null ) {
			final Application app = AbstractApplication.getApplication();
			if( app == null ) return;
			fnt = app.getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL );
		}
		
		c.setFont( fnt );
		for( int i = 0; i < comp.length; i++ ) {
			if( comp[ i ] instanceof Container ) {
				setDeepFont( (Container) comp[i], fnt );
			} else {
				comp[ i ].setFont( fnt );
			}
		}
	}

	public static void setPreferences( Container c, Preferences prefs )
	{
		final Component[] comp = c.getComponents();

		if( c instanceof PreferenceEntrySync ) {
			((PreferenceEntrySync) c).setPreferenceNode( prefs );
		}
		for( int i = 0; i < comp.length; i++ ) {
			if( comp[ i ] instanceof Container ) {
				setPreferences( (Container) comp[i], prefs );
			} else if( c instanceof PreferenceEntrySync ) {
				((PreferenceEntrySync) c).setPreferenceNode( prefs );
			}
		}
	}

    /**
     *  A debugging utility that prints to stdout the component's
     *  minimum, preferred, and maximum sizes.
	 *  This is taken from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>.
     */
    public static void printSizes( Component c )
	{
        System.err.println( "minimumSize   = " + c.getMinimumSize() );
        System.err.println( "preferredSize = " + c.getPreferredSize() );
        System.err.println( "maximumSize   = " + c.getMaximumSize() );
    }

    /**
     *  Aligns the first <code>rows</code> * <code>cols</code>
     *  components of <code>parent</code> in
     *  a grid. Each component is as big as the maximum
     *  preferred width and height of the components.
     *  The parent is made just big enough to fit them all.
	 *  <p>
	 *  The code is taken from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>.
	 *
     *  @param  rows		number of rows
     *  @param  cols		number of columns
     *  @param  initialX	x location to start the grid at
     *  @param  initialY	y location to start the grid at
     *  @param  xPad		x padding between cells
     *  @param  yPad		y padding between cells
     */
    public static void makeSpringGrid( Container parent, int rows, int cols,
									   int initialX, int initialY, int xPad, int yPad )
	{
        SpringLayout				layout;
		Spring						xPadSpring, yPadSpring, initialXSpring, initialYSpring;
		Spring						maxWidthSpring, maxHeightSpring;
		SpringLayout.Constraints	cons;
        SpringLayout.Constraints	lastCons		= null;
        SpringLayout.Constraints	lastRowCons		= null;
		int							i;
		int							max				= rows * cols;
		
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch( ClassCastException e1 ) {
            System.err.println( "The first argument to makeGrid must use SpringLayout." );
            return;
        }

        xPadSpring		= Spring.constant( xPad );
        yPadSpring		= Spring.constant( yPad );
        initialXSpring  = Spring.constant( initialX );
        initialYSpring  = Spring.constant( initialY );

        // Calculate Springs that are the max of the width/height so that all
        // cells have the same size.
        maxWidthSpring  = layout.getConstraints( parent.getComponent( 0 )).getWidth();
        maxHeightSpring = layout.getConstraints( parent.getComponent( 0 )).getWidth();
        for( i = 1; i < max; i++ ) {
            cons			= layout.getConstraints( parent.getComponent( i ));
            maxWidthSpring  = Spring.max( maxWidthSpring, cons.getWidth() );
            maxHeightSpring = Spring.max( maxHeightSpring, cons.getHeight() );
        }

        // Apply the new width/height Spring. This forces all the
        // components to have the same size.
        for( i = 0; i < max; i++ ) {
            cons = layout.getConstraints( parent.getComponent( i ));
            cons.setWidth( maxWidthSpring );
            cons.setHeight( maxHeightSpring );
        }

        // Then adjust the x/y constraints of all the cells so that they
        // are aligned in a grid.
        for( i = 0; i < max; i++ ) {
            cons = layout.getConstraints( parent.getComponent( i ));
            if( i % cols == 0 ) {   // start of new row
                lastRowCons = lastCons;
                cons.setX( initialXSpring );
            } else {				// x position depends on previous component
                cons.setX( Spring.sum( lastCons.getConstraint( SpringLayout.EAST ), xPadSpring ));
            }

            if( i / cols == 0 ) {   // first row
                cons.setY( initialYSpring );
            } else {				// y position depends on previous row
                cons.setY( Spring.sum( lastRowCons.getConstraint( SpringLayout.SOUTH ), yPadSpring ));
            }
            lastCons = cons;
        }

		// Set the parent's size.
		cons = layout.getConstraints( parent );
		cons.setConstraint( SpringLayout.SOUTH, Spring.sum( Spring.constant( yPad ),
							lastCons.getConstraint( SpringLayout.SOUTH )));
        cons.setConstraint( SpringLayout.EAST, Spring.sum( Spring.constant( xPad ),
							lastCons.getConstraint( SpringLayout.EAST )));
    }

    /**
     *  Aligns the first <code>rows</code> * <code>cols</code>
     *  components of <code>parent</code> in
     *  a grid. Each component in a column is as wide as the maximum
     *  preferred width of the components in that column;
     *  height is similarly determined for each row.
     *  The parent is made just big enough to fit them all.
	 *  <p>
	 *  The code is based on one from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>. It was optimized and includes support for hidden components.
     *
     *  @param  rows		number of rows
     *  @param  cols		number of columns
     *  @param  initialX	x location to start the grid at
     *  @param  initialY	y location to start the grid at
     *  @param  xPad		x padding between cells
     *  @param  yPad		y padding between cells
	 *
	 *	@warning	the spring layout seems to accumulate information; this method should not be called
	 *				many times on the same spring layout, it will substantially slow down the layout
	 *				process. though it's not very elegant, to solve this problem - e.g. in TimelineFrame -,
	 *				simply create set a panel's layout manager to a new SpringLayout before calling this
	 *				method!
     */
    public static void makeCompactSpringGrid( Container parent, int rows, int cols,
											  int initialX, int initialY, int xPad, int yPad )
	{
		SpringLayout				layout;
		Spring						x, y, width, height;
		SpringLayout.Constraints	constraints;
		Component					comp;
		boolean						anyVisible;

		try {
			layout = (SpringLayout) parent.getLayout();
		} catch( ClassCastException e1 ) {
			System.err.println( "The first argument to makeCompactGrid must use SpringLayout." );
			return;
		}

		// Align all cells in each column and make them the same width.
		x = Spring.constant( initialX );
		for( int col = 0; col < cols; col++ ) {
			width		= Spring.constant( 0 );
			anyVisible	= false;
			for( int row = 0; row < rows; row++ ) {
				comp	= parent.getComponent( row * cols + col );
				if( comp.isVisible() ) {
					width		= Spring.max( width, layout.getConstraints( comp ).getWidth() );
					anyVisible	= true;
				}
			}
			for( int row = 0; row < rows; row++ ) {
				comp		= parent.getComponent( row * cols + col );
				constraints = layout.getConstraints( comp );
				constraints.setX( x );
				if( comp.isVisible() ) constraints.setWidth( width );
			}
			if( anyVisible) x = Spring.sum( x, Spring.sum( width, Spring.constant( xPad )));
        }

		// Align all cells in each row and make them the same height.
		y = Spring.constant( initialY );
		for( int row = 0; row < rows; row++ ) {
			height		= Spring.constant( 0 );
			anyVisible	= false;
			for( int col = 0; col < cols; col++ ) {
				comp	= parent.getComponent( row * cols + col );
				if( comp.isVisible() ) {
					height		= Spring.max( height, layout.getConstraints( comp ).getHeight() );
					anyVisible	= true;
				}
			}
			for( int col = 0; col < cols; col++ ) {
				comp		= parent.getComponent( row * cols + col );
				constraints = layout.getConstraints( comp );
				constraints.setY( y );
				if( comp.isVisible() ) constraints.setHeight( height );
			}
			if( anyVisible ) y = Spring.sum( y, Spring.sum( height, Spring.constant( yPad )));
		}

		// Set the parent's size.
		constraints = layout.getConstraints( parent );
		constraints.setConstraint( SpringLayout.SOUTH, y );
		constraints.setConstraint( SpringLayout.EAST, x );
	}

	/**
	 *	Returns an <code>Icon</code> for a no-write
	 *	or write-protection indicator. The icon has
	 *	a dimension of 16x16 pixels with transparent background.
	 *
	 *	@return	the write-protected icon
	 */
	public static Icon getNoWriteIcon()
	{
		if( icnNoWrite == null ) {
//			icnNoWrite = new ImageIcon( ClassLoader.getSystemClassLoader().getResource( "nowrite.png" ));
			icnNoWrite = new ImageIcon( GUIUtil.class.getResource( "nowrite.png" ));
		}
		return icnNoWrite;
	}
	
	/**
	 *	Adjusts minimum, maximum and preferred size
	 *	of a component so as to constrain its width
	 *	to a given value.
	 *
	 *	@param	c	the component to constrain
	 *	@param	w	the width in pixels
	 */
	public static void constrainWidth( JComponent c, int w )
	{
		Dimension d;
		
		d = c.getMinimumSize();
		c.setMinimumSize( new Dimension( w, d.height ));
		d = c.getMaximumSize();
		c.setMaximumSize( new Dimension( w, d.height ));
		d = c.getPreferredSize();
		c.setPreferredSize( new Dimension( w, d.height ));
	}

	/**
	 *	Adjusts minimum, maximum and preferred size
	 *	of a component so as to constrain its height
	 *	to a given value.
	 *
	 *	@param	c	the component to constrain
	 *	@param	h	the height in pixels
	 */
	public static void constrainHeight( JComponent c, int h )
	{
		Dimension d;
		
		d = c.getMinimumSize();
		c.setMinimumSize( new Dimension( d.width, h ));
		d = c.getMaximumSize();
		c.setMaximumSize( new Dimension( d.width, h ));
		d = c.getPreferredSize();
		c.setPreferredSize( new Dimension( d.width, h ));
	}

	/**
	 *	Sets minimum, maximum and preferred size
	 *	to given values.
	 *
	 *	@param	c	the component to constrain
	 *	@param	w	the width in pixels
	 *	@param	h	the height in pixels
	 */
	public static void constrainSize( JComponent c, int w, int h )
	{
		final Dimension d = new Dimension( w, h );
		
		c.setMinimumSize( d );
		c.setMaximumSize( d );
		c.setPreferredSize( d );
	}

	/**
	 *	Passes keyboard focus to a given component when
	 *	that component is to be presented in a dialog.
	 *	This temporarily adds an <code>AncestorListener</code>
	 *	to the component, detecting when its parent container
	 *	is made visible. This is usefull for defining an
	 *	initial focus owner in <code>JOptionPane</code> calls for example.
	 *
	 *	@param	c	the component to make focussed once its parent container is shown
	 */
	public static void setInitialDialogFocus( final JComponent c )
	{
		c.addAncestorListener( new AncestorAdapter() {
			public void ancestorAdded( AncestorEvent e ) {
				c.requestFocusInWindow();
				c.removeAncestorListener( this );
			}
		});
	}
}