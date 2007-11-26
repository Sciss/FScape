/*
 *  SpringPanel.java
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
 *		07-Sep-05	created
 *		15-Jan-06	Spring classes are public ; makeCompactGrid : bugfix + support
 *					for "negative" dimensions ; commmented
 *		19-Feb-06	removes old constraints before making new grids (fixes slow down bug)
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

/**
 *	A subclass of <code>JPanel</code> that is linked
 *	to a <code>SpringLayout</code>. Components must be
 *	added using one of the <code>gridAdd</code> methods
 *	which define the component's space in terms of a
 *	virtual grid. When all components have been added,
 *	the actual layout contraints are calculated by calling
 *	either <code>makeGrid</code> (for equal cell size grid)
 *	or <code>makeCompactGrid</code>. Just like <code>GridBagLayout</code>,
 *	the grid positions need not be &quot;compact&quot;, i.e.
 *	you may omit complete columns or rows and start at
 *	an arbitrary offset.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.33, 19-Feb-06
 *
 *	@todo		the elastic/nonelastic choice isn't working yet
 */
public class SpringPanel
extends JPanel
{
	private final	SpringLayout	layout;
	private			int				xPad, yPad;
	private			int				initialX, initialY;
	
	private static final Object GRID	= "de.sciss.gui.GRID";

	/**
	 *	Creates a new panel with zero padding and offset
	 */
	public SpringPanel()
	{
		super();

		layout	= new SpringLayout();
		setLayout( layout );
	}

	/**
	 *	Creates a new panel with given padding and offset
	 *	(in pixels).
	 *
	 *	@param	initialX	the spacing for gadgets from the left border of the panel
	 *	@param	initialY	the spacing for gadgets from the top border of the panel
	 *	@param	xPad		the spacing between a gadget and its horizontal neighbour
	 *	@param	yPad		the spacing between a gadget and its vertical neighbour
	 */
	public SpringPanel( int initialX, int initialY, int xPad, int yPad )
	{
		this();
		
		setPadding( initialX, initialY, xPad, yPad );
	}
	
	/**
	 *	Changes the horizontal offset. This takes
	 *	effect, next time makeGrid is called.
	 *
	 *	@param	x	the spacing for gadgets from the left border of the panel
	 */
	public void setInitialX( int x )
	{
		initialX = x;
	}

	/**
	 *	Changes the vertical offset. This takes
	 *	effect, next time makeGrid is called.
	 *
	 *	@param	y	the spacing for gadgets from the top border of the panel
	 */
	public void setInitialY( int y )
	{
		initialY = y;
	}

	/**
	 *	Changes the horizontal padding. This takes
	 *	effect, next time makeGrid is called.
	 *
	 *	@param	x		the spacing between a gadget and its horizontal neighbour
	 */
	public void setXPad( int x )
	{
		xPad = x;
	}

	/**
	 *	Changes the vertical padding. This takes
	 *	effect, next time makeGrid is called.
	 *
	 *	@param	y		the spacing between a gadget and its vertical neighbour
	 */
	public void setYPad( int y )
	{
		yPad = y;
	}
	
	/**
	 *	Changes the padding and offset for the panel. These take
	 *	effect, next time makeGrid is called.
	 *
	 *	@param	initialX	the spacing for gadgets from the left border of the panel
	 *	@param	initialY	the spacing for gadgets from the top border of the panel
	 *	@param	xPad		the spacing between a gadget and its horizontal neighbour
	 *	@param	yPad		the spacing between a gadget and its vertical neighbour
	 */
	public void setPadding( int initialX, int initialY, int xPad, int yPad )
	{
		this.initialX	= initialX;
		this.initialY	= initialY;
		this.xPad		= xPad;
		this.yPad		= yPad;
	}
	
	/**
	 *	Add a component to the panel and store its
	 *	placement information for use in makeGrid.
	 *	The component is assumed to occupy a grid
	 *	dimension of 1 x 1 cell.
	 *
	 *	@param	c	the component
	 *	@param	x	the horizontal grid position
	 *	@param	y	the vertical grid position
	 */
	public void gridAdd( JComponent c, int x, int y )
	{
		gridAdd( c, x, y, 1, 1 );
	}

	/**
	 *	Add a component to the panel and store its
	 *	placement information for use in makeGrid.
	 *
	 *	@param	c		the component
	 *	@param	x		the horizontal grid position
	 *	@param	y		the vertical grid position
	 *	@param	width	the number of columns to occupy
	 *					(a negative width means the width is
	 *					not elastic)
	 *	@param	height	the number of rows to occupy
	 *					(a negative height means the height is
	 *					not elastic)
	 */
	public void gridAdd( JComponent c, int x, int y, int width, int height )
	{
		gridAdd( c, new Rectangle( x, y, width, height ));
	}

	/**
	 *	Add a component to the panel and store its
	 *	placement information for use in makeGrid.
	 *
	 *	@param	c		the component
	 *	@param	r		the grid location and extent.
	 *					(a negative width or height means the width or height is
	 *					not elastic)
	 */
	public void gridAdd( JComponent c, Rectangle r )
	{
		c.putClientProperty( GRID, r );
		add( c );
	}
	
	/**
	 *	Adds a titled border to the panel.
	 *
	 *	@param	title	the text to display in the titled border
	 */
	public void setTitle( String title )
	{
		setBorder( BorderFactory.createTitledBorder( null, title ));
	}
	
	/**
	 *	Aligns all the gadgets in a regular grid whose virtual
	 *	cells are all of the same size (calculated from the maximum
	 *	dimensions of all cells).
	 *
	 *	@param	elastic		not yet fully working, has to be false for now(?)
	 *
	 *	@todo	this method is not yet fully working
	 */
	public void makeGrid( boolean elastic )
	{
		final List					realOnes		= new ArrayList( getComponentCount() );
		final Spring				xPadSpring, yPadSpring, initialXSpring, initialYSpring;
		final int[]					colCnt;
		final int[]					rowCnt;
		final int					effCols, effRows;
//		final SpringLayout			layout			= new SpringLayout();

		Spring						maxWidthSpring, maxHeightSpring, spX, spY, spW, spH;
		SpringLayout.Constraints	cons;
//		SpringLayout.Constraints	lastCons		= null;
//		SpringLayout.Constraints	lastRowCons		= null;
		Rectangle					r;
		Component					comp;
		JComponent					jc;
		int							rows			= 0;
		int							cols			= 0;

//		setLayout( layout );

		xPadSpring		= Spring.constant( xPad );
		yPadSpring		= Spring.constant( yPad );
		initialXSpring	= Spring.constant( initialX );
		initialYSpring	= Spring.constant( initialY );

		for( int i = 0; i < getComponentCount(); i++ ) {
			comp			= getComponent( i );
			layout.removeLayoutComponent( comp );
			if( !(comp instanceof JComponent) || !comp.isVisible() ) continue;
			jc				= (JComponent) comp;
			r				= (Rectangle) jc.getClientProperty( GRID );
			if( r == null ) continue;
			realOnes.add( jc );
			cols			= Math.max( cols, r.x + r.width );
			rows			= Math.max( rows, r.y + r.height );
		}
		
		if( (cols == 0) || (rows == 0) ) return;
		
		colCnt = new int[ cols ];
		rowCnt = new int[ rows ];
		
		for( int i = 0; i < realOnes.size(); i++ ) {
			jc				= (JComponent) realOnes.get( i );
			r				= (Rectangle) jc.getClientProperty( GRID );
			for( int col = r.x; col < r.x + r.width; col++ ) {
				colCnt[ col ]++;
			}
			for( int row = r.y; row < r.y + r.height; row++ ) {
				rowCnt[ row ]++;
			}
		}
		
		for( int col = 0, colOff = 0; col < cols; col++ ) {
			if( colCnt[ col ] > 0 ) {
				colCnt[ col ] = colOff++;
			}
		}
		for( int row = 0, rowOff = 0; row < rows; row++ ) {
			if( rowCnt[ row ] > 0 ) {
				rowCnt[ row ] = rowOff++;
			}
		}

		effCols = colCnt[ cols - 1 ] + 1;
		effRows = rowCnt[ rows - 1 ] + 1;

		if( elastic ) {
//			maxWidthSpring	= Spring.constant( 64 );
//			maxHeightSpring = Spring.constant( 32 );
			maxWidthSpring	= new ComponentWidthRatioSpring( this, 1, effCols );
			maxHeightSpring = new ComponentHeightRatioSpring( this, 1, effRows );
		} else {
			// Calculate Springs that are the max of the width/height so that all
			// cells have the same size.
			maxWidthSpring	= Spring.constant( 0 );
			maxHeightSpring = Spring.constant( 0 );
		}
		for( int i = 0; i < realOnes.size(); i++ ) {
			jc				= (JComponent) realOnes.get( i );
			r				= (Rectangle) jc.getClientProperty( GRID );
			cons			= layout.getConstraints( jc );
			spW				= new RatioSpring( cons.getWidth(), 1, r.width );
			spH				= new RatioSpring( cons.getHeight(), 1, r.height );
			maxWidthSpring	= Spring.max( maxWidthSpring, spW );
			maxHeightSpring = Spring.max( maxHeightSpring, spH );
		}
		
		System.err.println( "cols "+cols+"; rows "+rows+"; maxWidthSpring "+maxWidthSpring.getValue()+
			"; maxHeightSpring "+maxHeightSpring.getValue() );

		// Apply the new width/height Spring. This forces all the
		// components to have the same size.
		// Adjust the x/y constraints of all the cells so that they
		// are aligned in a grid.
		for( int i = 0; i < realOnes.size(); i++ ) {
			jc		= (JComponent) realOnes.get( i );
			r		= (Rectangle) jc.getClientProperty( GRID );
			cons	= layout.getConstraints( jc );
			spW		= new RatioSpring( maxWidthSpring, r.width, 1 );
			spH		= new RatioSpring( maxHeightSpring, r.height, 1 );
			cons.setWidth( spW );
			cons.setHeight( spH );

			spX		= initialXSpring;
			if( colCnt[ r.x ] > 0 ) {
				spX	= Spring.sum( spX, new RatioSpring( maxWidthSpring, colCnt[ r.x ], 1 ));
			}
			spY		= initialYSpring;
			if( rowCnt[ r.y ] > 0 ) {
				spY	= Spring.sum( spY, new RatioSpring( maxHeightSpring, rowCnt[ r.y ], 1 ));
			}
			cons.setX( spX );
			cons.setY( spY );
			
			if( jc instanceof AbstractButton ) {
				System.out.println( "For "+((AbstractButton) jc).getText()+
					" spX "+spX.getValue()+"; spY "+spY.getValue()+"; spW "+spW.getValue()+"; spH "+spH.getValue()+
					"; r.x "+r.x+"; r.y "+r.y+"; r.width "+r.width+"; r.height "+r.height );
			}
		}

//		// Then adjust the x/y constraints of all the cells so that they
//		// are aligned in a grid.
//		for( int i = 0; i < realOnes.size(); i++ ) {
//			jc		= (JComponent) realOnes.get( i );
//			r		= (Rectangle) jc.getClientProperty( GRID );
//			cons	= layout.getConstraints( jc );
//			if( i % cols == 0 ) {	// start of new row
//				lastRowCons = lastCons;
//				cons.setX( initialXSpring );
//			} else {				// x position depends on previous component
//				cons.setX( Spring.sum( lastCons.getConstraint( SpringLayout.EAST ), xPadSpring ));
//			}
//
//			if( i / cols == 0 ) {	// first row
//				cons.setY( initialYSpring );
//			} else {				// y position depends on previous row
//				cons.setY( Spring.sum( lastRowCons.getConstraint( SpringLayout.SOUTH ), yPadSpring ));
//			}
//			lastCons = cons;
//		}

System.err.println( "effCols = "+effCols+"; effRows = "+effRows );

		if( !elastic ) {
			// Set the parent's size.
			spX		= Spring.sum( initialXSpring, Spring.sum( xPadSpring, new RatioSpring( maxWidthSpring, effCols, 1 )));
			spY		= Spring.sum( initialYSpring, Spring.sum( yPadSpring, new RatioSpring( maxHeightSpring, effRows, 1 )));
			
System.err.println( " yields east : "+(Spring.sum( Spring.constant( xPad ), spX )).getValue()+
		"; south : "+(Spring.sum( Spring.constant( yPad ), spY )).getValue() );
			
			cons = layout.getConstraints( this );
			cons.setConstraint( SpringLayout.EAST, spX );
			cons.setConstraint( SpringLayout.SOUTH, spY );
	//		cons.setConstraint( SpringLayout.SOUTH, Spring.sum( Spring.constant( yPad ),
	//							lastCons.getConstraint( SpringLayout.SOUTH )));
	//		cons.setConstraint( SpringLayout.EAST, Spring.sum( Spring.constant( xPad ),
	//							lastCons.getConstraint( SpringLayout.EAST )));
		}
	}

	/**
	 *	Aligns all the gadgets in a compact grid whose virtual
	 *	cells are as small as possible (calculated from the maximum
	 *	width of a column and maximum height of a row). The layout
	 *	is automatically stretched when the panel is resized.
	 */
    public void makeCompactGrid()
	{
		makeCompactGrid( true, true );
	}
	
	/**
	 *	Aligns all the gadgets in a compact grid whose virtual
	 *	cells are as small as possible (calculated from the maximum
	 *	width of a column and maximum height of a row).
	 *
	 *	@param	hElastic	whether the layout should be horizontally stretched
	 *						as to use additional space when the panel is horizontally resized
	 *	@param	vElastic	whether the layout should be vertically stretched
	 *						as to use additional space when the panel is vertically resized
	 *
	 *	@todo	the elastic flags should be set to true for now
	 */
    public void makeCompactGrid( boolean hElastic, boolean vElastic )
	{
		final List					realOnes		= new ArrayList( getComponentCount() );
		final Spring				xPadSpring, yPadSpring; // , initialXSpring, initialYSpring;
		final int[]					colCnt;
		final int[]					rowCnt;
		final Spring[]				spXs, spYs, spWs, spHs;
//		final SpringLayout			layout			= new SpringLayout();

		Spring						spX, spY, spW, spH;
		SpringLayout.Constraints	cons;
//		SpringLayout.Constraints	lastCons		= null;
//		SpringLayout.Constraints	lastRowCons		= null;
		Rectangle					r;
		Component					comp;
		JComponent					jc;
		int							rows			= 0;
		int							cols			= 0;

//		setLayout( layout );

		xPadSpring		= Spring.constant( xPad );
		yPadSpring		= Spring.constant( yPad );
//		initialXSpring	= Spring.constant( initialX );
//		initialYSpring	= Spring.constant( initialY );

		for( int i = 0; i < getComponentCount(); i++ ) {
			comp			= getComponent( i );
			layout.removeLayoutComponent( comp );
			if( !(comp instanceof JComponent) || !comp.isVisible() ) continue;
			jc				= (JComponent) comp;
			r				= (Rectangle) jc.getClientProperty( GRID );
			if( r == null ) continue;
//System.err.println( "for gadget "+comp.getClass().getName() + "; rect "+r.x+", "+r.y+", "+r.width+", "+r.height );
			realOnes.add( jc );
			cols			= Math.max( cols, r.x + Math.abs( r.width ));
			rows			= Math.max( rows, r.y + Math.abs( r.height ));
		}
		
		if( (cols == 0) || (rows == 0) ) return;
		
		colCnt	= new int[ cols ];
		rowCnt	= new int[ rows ];
		spXs	= new Spring[ cols ];
		spYs	= new Spring[ rows ];
		spWs	= new Spring[ cols ];
		spHs	= new Spring[ rows ];
		
		for( int i = 0; i < realOnes.size(); i++ ) {
			jc		= (JComponent) realOnes.get( i );
			r		= (Rectangle) jc.getClientProperty( GRID );
			cons	= layout.getConstraints( jc );
			for( int col = r.x; col < r.x + Math.abs( r.width ); col++ ) {
				colCnt[ col ]++;
				spW			= new RatioSpring( cons.getWidth(), 1, Math.abs( r.width ) );
				spWs[ col ] = (spWs[ col ] == null) ? spW : Spring.max( spWs[ col ], spW );
			}
			for( int row = r.y; row < r.y + Math.abs( r.height ); row++ ) {
				rowCnt[ row ]++;
				spH			= new RatioSpring( cons.getHeight(), 1, Math.abs( r.height ));
				spHs[ row ] = (spHs[ row ] == null) ? spH : Spring.max( spHs[ row ], spH );
			}
		}
		
		spX = Spring.constant( initialX );
		spY = Spring.constant( initialY );
		
		for( int col = 0, colOff = 0; col < cols; col++ ) {
			if( colCnt[ col ] > 0 ) {
				colCnt[ col ] = colOff++;
				spXs[ col ]	= spX;
				spX			= Spring.sum( spX, Spring.sum( spWs[ col ], xPadSpring ));
			}
		}
		for( int row = 0, rowOff = 0; row < rows; row++ ) {
			if( rowCnt[ row ] > 0 ) {
				rowCnt[ row ] = rowOff++;
				spYs[ row ]	= spY;
				spY			= Spring.sum( spY, Spring.sum( spHs[ row ], yPadSpring ));
			}
		}

		for( int i = 0; i < realOnes.size(); i++ ) {
			jc		= (JComponent) realOnes.get( i );
			r		= (Rectangle) jc.getClientProperty( GRID );
			cons	= layout.getConstraints( jc );
			if( r.width > 0 ) {
				spW		= spWs[ r.x ];
				for( int j = 1; j < r.width; j++ ) {
					spW	= Spring.sum( spW, spWs[ r.x + j ]);
				}
				cons.setWidth( spW );
			}
			if( r.height > 0 ) {
				spH		= spHs[ r.y ];
				for( int j = 1; j < r.height; j++ ) {
					spH	= Spring.sum( spH, spHs[ r.y + j ]);
				}
				cons.setHeight( spH );
			}

			cons.setX( spXs[ r.x ]);
			cons.setY( spYs[ r.y ]);
			
//			if( jc instanceof AbstractButton ) {
//				System.out.println( "For "+((AbstractButton) jc).getText()+
//					" spX "+spX.getValue()+"; spY "+spY.getValue()+"; spW "+spW.getValue()+"; spH "+spH.getValue()+
//					"; r.x "+r.x+"; r.y "+r.y+"; r.width "+r.width+"; r.height "+r.height );
//			}
		}

		cons	= layout.getConstraints( this );
		if( hElastic ) {
			spX		= Spring.sum( spXs[ cols - 1 ], Spring.sum( xPadSpring, spWs[ cols - 1 ]));
			cons.setConstraint( SpringLayout.EAST, spX );
		} else {
			// XXX
		}

		if( vElastic ) {
			spY		= Spring.sum( spYs[ rows - 1 ], Spring.sum( yPadSpring, spHs[ rows - 1 ]));
			cons.setConstraint( SpringLayout.SOUTH, spY );
		} else {
			// XXX
		}
	}

// --------------- internal classes ---------------

	public static class RatioSpring
	extends Spring
	{
		final Spring	s;
		final int		nom, div;

		public RatioSpring( Spring s, int nom, int div )
		{
			this.s		= s;
			this.nom	= nom;
			this.div	= div;
		}
		
		public int getMinimumValue()
		{
			return s.getMinimumValue() * nom / div;
		}
		
		public int getPreferredValue()
		{
			return s.getPreferredValue() * nom / div;
		}
		
		public int getMaximumValue()
		{
			return s.getMaximumValue() * nom / div;
		}
		
		public int getValue()
		{
			return s.getValue() * nom / div;
		}
		
		public void setValue( int value )
		{
			s.setValue( value * div / nom );
		}
	}

	public static class ComponentHeightRatioSpring
	extends Spring
	{
		final Component	c;
		final int		nom, div;

		public ComponentHeightRatioSpring( Component c, int nom, int div )
		{
			this.c		= c;
			this.nom	= nom;
			this.div	= div;
		}
		
		public int getMinimumValue()
		{
			return c.getMinimumSize().height * nom / div;
		}
		
		public int getPreferredValue()
		{
			return getValue(); // c.getPreferredSize().height * nom / div;
		}
		
		public int getMaximumValue()
		{
			return c.getMaximumSize().height * nom / div;
		}
		
		public int getValue()
		{
			return c.getHeight() * nom / div;
		}
		
		public void setValue( int value )
		{
	//		s.setValue( value * div / nom );
		}
	}

	public static class ComponentWidthRatioSpring
	extends Spring
	{
		final Component	c;
		final int		nom, div;

		public ComponentWidthRatioSpring( Component c, int nom, int div )
		{
			this.c		= c;
			this.nom	= nom;
			this.div	= div;
		}
		
		public int getMinimumValue()
		{
			return c.getMinimumSize().width * nom / div;
		}
		
		public int getPreferredValue()
		{
			return getValue(); // c.getPreferredSize().width * nom / div;
		}
		
		public int getMaximumValue()
		{
			return c.getMaximumSize().width * nom / div;
		}
		
		public int getValue()
		{
			return c.getWidth() * nom / div;
		}
		
		public void setValue( int value )
		{
	//		s.setValue( value * div / nom );
		}
	}
}