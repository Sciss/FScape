/*
 *  CircuitPanel.java
 *  FScape
 *
 *  Copyright (c) 2001-2007 Hanns Holger Rutz. All rights reserved.
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
 *		05-Mar-05	extends JPanel instead of Panel
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 *  Panel for arranging symbolic block
 *	elements in series or parallel such
 *	as in the FIR filter processing module.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class CircuitPanel
extends JPanel
{
// -------- public Variablen --------

	/**
	 *	ActionCommands, die ein registrierter (AddActionListener())
	 *	ActionListener empfaengt. Angefuegt wird die uniqueID des
	 *	correspondierenden Boxes
	 */
	public static final String	ACTION_BOXSELECTED		= "act";
	public static final String	ACTION_BOXDESELECTED	= "des";
	public static final String	ACTION_BOXCREATED		= "new";
	public static final String	ACTION_BOXDELETED		= "rem";

	public static final int TYPE_GROUND		= 0;
	public static final int TYPE_SERIAL		= 1;
	public static final int TYPE_PARALLEL	= 2;
	public static final int TYPE_BOX		= 3;

// -------- private Variablen --------

	// gerade angewaehlte Box; wenn keiner ausgewaehlt, muss
	// er auf dummyBox gesetzt werden, damit keine Null-Pointer Fehler
	// auftreten koennen!
	private CircuitPanel.Box		currentBox	= null;
	private CircuitPanel			ground;
	private final CircuitPanel.Box	protoType;

	private final java.util.List boxes	= new ArrayList();
	private final Map mapBoxToView;		// only for ground!
	private int				type;

	// dieser Button wird nicht angezeigt, sondern dient nur als
	// Institution zum Verwalten der ActionListener und fuer
	// den Event-Dispatch!
	private Button		actionComponent;
	
	// Fehlermeldungen
//	private static final String ERR_CORRUPTED	= "Internal data corrupted. Please report bug!";

	private static final int DIR_WEST	= 0x01;		// they add up as DIR_WEST + DIR_NORTH
	private static final int DIR_EAST	= 0x02;
	private static final int DIR_NORTH	= 0x10;
	private static final int DIR_SOUTH	= 0x20;

	private static final Color colrConnections = new Color( 0x00, 0x00, 0x00, 0x7F );

	private GridBagLayout		lay;
	private GridBagConstraints	con;

	protected final CircuitPanel enc_cp = this;

// -------- public Methoden --------

	/**
	 *	@param	protoType	Basis-Objekt ueber dessen nichtparametrisierten Constructor
	 *						neue Boxen erzeugt werden
	 */
	public CircuitPanel( CircuitPanel.Box protoType )
	{
		super();

		type				= TYPE_GROUND;
		ground				= this;
		actionComponent		= new Button();
		this.protoType		= protoType;
		mapBoxToView		= new HashMap();
		
		init();

		setCircuit( "" );
	}

	private CircuitPanel( CircuitPanel ground, String circuit )
	{
		super();
		
		type			= (int) circuit.charAt( 0 ) - 48;
		this.ground		= ground;
		this.protoType	= ground.protoType;
		mapBoxToView	= null;

		init();

		setCircuit( circuit );
	}
	
	public void repaintBox( CircuitPanel.Box box )
	{
		Component c = (Component) ground.mapBoxToView.get( box );
		if( c != null ) c.repaint();
	}
	
	private void insertBox( Object o, int index )
	{
		JComponent	view;
		int			numBoxes;
		Object		o2;
		Component	c;
		
		if( o instanceof CircuitPanel.Box ) {
			view = new JToggleButton( new BoxAction( (CircuitPanel.Box) o ));
		} else if( o instanceof CircuitPanel ) {
			view = (CircuitPanel) o;
		} else {
			assert false : o.getClass().getName();
			view = null;
		}
	
//System.err.println( "insert box "+o.getClass().getName()+" at "+index );

		con.gridx = 1;
		con.gridy = 1;
		switch( type ) {
		case TYPE_SERIAL:
			con.gridx += index;
			break;
		case TYPE_PARALLEL:
			con.gridy += index;
			break;
		}
		lay.setConstraints( view, con );
		this.add( view, index );
		boxes.add( index, o );
		ground.mapBoxToView.put( o, view );
		numBoxes = boxes.size();
		
		for( int i = ++index; i < numBoxes; i++ ) {
			switch( type ) {
			case TYPE_SERIAL:
				con.gridx++;
				break;
			case TYPE_PARALLEL:
				con.gridy++;
				break;
			}
			o2 = boxes.get( i );
			if( o2 instanceof Box ) {
				c = (Component) ground.mapBoxToView.get( o2 );
			} else if( o2 instanceof CircuitPanel ) {
				c = (CircuitPanel) o2;
			} else {
				assert false : o2.getClass().getName();
				c = null;
			}
			lay.setConstraints( c, con );
		}

		if( this.isVisible() ) {
			invalidate();
			ground.validate();
		}
		
		notifyListeners( ACTION_BOXCREATED );
	}
	
	private void removeBox( int index )
	{
		Object o = boxes.remove( index );
		Component c;
		Object o2;
		int numBoxes;

		if( o instanceof CircuitPanel.Box ) {
			c = (Component) ground.mapBoxToView.remove( o );
			if( c != null ) {
				this.remove( c );
			}
		} else if( o instanceof CircuitPanel ) {
			((CircuitPanel) o).clear();
			this.remove( (CircuitPanel) o );
		} else {
			assert false : o.getClass().getName();
		}

		con.gridx = 1;
		con.gridy = 1;
		switch( type ) {
		case TYPE_SERIAL:
			con.gridx += index;
			break;
		case TYPE_PARALLEL:
			con.gridy += index;
			break;
		}

		numBoxes = boxes.size();

		for( int i = index; i < numBoxes; i++ ) {
			o2 = boxes.get( i );
			if( o2 instanceof Box ) {
				c = (Component) ground.mapBoxToView.get( o2 );
			} else if( o2 instanceof CircuitPanel ) {
				c = (CircuitPanel) o2;
			} else {
				assert false : o2.getClass().getName();
				c = null;
			}
			lay.setConstraints( c, con );
			switch( type ) {
			case TYPE_SERIAL:
				con.gridx++;
				break;
			case TYPE_PARALLEL:
				con.gridy++;
				break;
			}
		}

		if( this.isVisible() ) {
			invalidate();
			ground.validate();
		}

		notifyListeners( ACTION_BOXDELETED );
	}
	
	private void clear()
	{
		Object o;
		Component c;
		
		while( !boxes.isEmpty() ) {
			o = boxes.remove( 0 );
			if( o instanceof CircuitPanel.Box ) {
				c = (Component) ground.mapBoxToView.remove( o );
				if( c != null ) {
					this.remove( c );
				}
			} else if( o instanceof CircuitPanel ) {
				((CircuitPanel) o).clear();
				this.remove( (CircuitPanel) o );
			} else {
				assert false : o.getClass().getName();
			}
		}
	}
	
	private void init()
	{
		this.addMouseListener( new MouseAdapter() {
			public void mouseClicked( MouseEvent e )
			{
				CircuitPanel.Box	box;
				Object				o;
				Component			c;
				CircuitPanel		cp;
				boolean				b;
				int					dir, i, numBoxes;
			
				if( !enc_cp.isEnabled() ) return;	// not while inactive

				if( e.getSource() == enc_cp ) {				//-------- Panel hit -------------------
					if( (e.getClickCount() == 2) && !e.isAltDown() ) {
						box			= ground.protoType.duplicate();
						numBoxes	= boxes.size();
						if( numBoxes == 0 ) {
							insertBox( box, 0 );
						} else {
							switch( type ) {
							case TYPE_GROUND:
								o	= boxes.get( 0 );
								if( o instanceof CircuitPanel.Box ) {
									c = (Component) ground.mapBoxToView.get( o );
								} else if( o instanceof CircuitPanel ) {
									c = (CircuitPanel) o;
								} else {
									assert false : o.getClass().getName();
									c = null;
								}
								dir		= calcDirection( c, e.getX(), e.getY() );
								if( (dir != DIR_WEST) && (dir != DIR_EAST) &&
									(dir != DIR_NORTH) && (dir != DIR_SOUTH) ) break;

								b = ((dir == DIR_WEST) || (dir == DIR_NORTH));
								cp = new CircuitPanel( ground, (((dir == DIR_WEST) || (dir == DIR_EAST)) ? TYPE_SERIAL : TYPE_PARALLEL) +
													   encodeBox( b ? box : boxes.get( 0 )) + encodeBox( b ? boxes.get( 0 ) : box ));
								removeBox( 0 );
								insertBox( cp, 0 );
								break;

							case TYPE_SERIAL:
serialLp:							for( i = 0; i < numBoxes; i++ ) {
									o	= boxes.get( i );
									if( o instanceof CircuitPanel.Box ) {
										c = (Component) ground.mapBoxToView.get( o );
									} else if( o instanceof CircuitPanel ) {
										c = (CircuitPanel) o;
									} else {
										assert false : o.getClass().getName();
										c = null;
									}
									dir	= calcDirection( c, e.getX(), e.getY() );
									switch( dir ) {
									case DIR_NORTH:
									case DIR_SOUTH:
										b	= (dir == DIR_NORTH);
										cp	= new CircuitPanel( ground, TYPE_PARALLEL + encodeBox( b ? box : boxes.get( i )) + encodeBox( b ? boxes.get( i ) : box ));
										removeBox( i );
										insertBox( cp, i );
										break serialLp;
									case DIR_WEST:
										insertBox( box, i );
										break serialLp;
									case DIR_EAST:
										if( i == numBoxes - 1 ) {
											insertBox( box, numBoxes );
											break serialLp;
										}
										break;
									}
								}
								break;

							case TYPE_PARALLEL:
parallelLp:							for( i = 0; i < numBoxes; i++ ) {
									o	= boxes.get( i );
									if( o instanceof CircuitPanel.Box ) {
										c = (Component) ground.mapBoxToView.get( o );
									} else if( o instanceof CircuitPanel ) {
										c = (CircuitPanel) o;
									} else {
										assert false : o.getClass().getName();
										c = null;
									}
									dir	= calcDirection( c, e.getX(), e.getY() );
									switch( dir ) {
									case DIR_WEST:
									case DIR_EAST:
										b	= (dir == DIR_WEST);
										cp	= new CircuitPanel( ground, TYPE_SERIAL + encodeBox( b ? box : boxes.get( i )) + encodeBox( b ? boxes.get( i ) : box ));
										removeBox( i );
										insertBox( cp, i );
										break parallelLp;
									case DIR_NORTH:
										insertBox( box, i );
										break parallelLp;
									case DIR_SOUTH:
										if( i == numBoxes - 1 ) {
											insertBox( box, numBoxes );
											break parallelLp;
										}
										break;
									}
								}
								break;
							}
						}
						AbstractButton ab = (AbstractButton) ground.mapBoxToView.get( box );
						if( ab != null ) ab.doClick();
					}

//				} else {						//-------- Box hit -------------------
//					if( e.isAltDown() ) {
//						c = (Component) e.getSource();
//						if( c .getClass() == ground.dummyBox.getClass() ) {
//							synchronized( boxes ) {
//								removeBox( c );
//								numBoxes = boxes.size();
//								if( this != ground ) {
//									if( numBoxes == 1 ) {			// replace panel by box
//										c = (Component) boxes.get( 0 );
//										((CircuitPanel) getParent()).replaceBox( this, c );
//									} else if( numBoxes == 0 ) {	// kill panel
//										((CircuitPanel) getParent()).removeBox( this );
//									}
//								}
//							}
//						}
//					}
				}
			}
		});
		
		javax.swing.Icon icn = ground.protoType.getIcon();
		
		setMinimumSize( new Dimension( icn.getIconWidth() + 8, icn.getIconHeight() + 8 ));
		setBorder( BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder( Color.lightGray, 2 ),
			BorderFactory.createEmptyBorder( 0, 2, 0, 2 )
		));
		lay					= new GridBagLayout();
		con					= new GridBagConstraints();
		con.gridx			= 1;
		con.gridy			= 1;
		con.gridwidth		= 1;
		con.gridheight		= 1;
		con.weightx			= 0.0;
		con.weighty			= 0.0;
		con.fill			= GridBagConstraints.NONE;
		con.anchor			= GridBagConstraints.CENTER;
		con.insets			= new Insets( 3, 3, 3, 3 );
		setLayout( lay );
	}

	/**
	 *	Blockschaltbild zuweisen
	 *	Aufbau: <type><obj1>{<obj1settings}<obj2>{<obj2settings>} etc.
	 */
	public void setCircuit( String circuit )
	{
// System.out.println( "set circuit: '"+circuit+"'" );
		Object		o;
		int			numBoxes;
		int			subType;
		String		settings;
		int			cIndex;

//			selectBox( null );
//			ground.currentBox	= ground.dummyBox;
		clear();
		cIndex		= 1;
		numBoxes	= parseNumBoxes( circuit, cIndex );
// System.out.println(" numBoxes: "+numBoxes );
	
boxLp:		for( int i = 0; i < numBoxes; i++ ) {
			subType		= (int) circuit.charAt( cIndex++ ) - 48;
			settings	= parseSettings( circuit, cIndex );
			cIndex	   += settings.length() + 2;	// plus brackets
			o			= null;
// System.out.println(" subtype "+subType+" settings "+i+" = "+settings );

			switch( subType ) {
			case TYPE_SERIAL:
			case TYPE_PARALLEL:
				o = new CircuitPanel( ground, settings );
				break;
			case TYPE_BOX:
				o = ground.protoType.fromString( settings );
//					box.addMouseListener( this );
				break;
			default:
				break boxLp;
			}

			if( o != null ) {
				insertBox( o, i );
			}
		}
	}

	/**
	 *	Blockschaltbild besorgen
	 */
	public String getCircuit()
	{
		return this.toString();
	}

	public Iterator getElements()
	{
		return boxes.iterator();
	}

	public int getType()
	{
		return type;
	}

	/**
	 *	Alle Boxen entfernen
	 */
//	public void clear()
//	{
//		Component	box;
//
//		synchronized( boxes ) {
//			while( !boxes.isEmpty() ) {
//				box = (Component) boxes.get( 0 );
//				boxes.remove( box );
//				this.remove( box );
//				box.removeMouseListener( this );
//			}
//		}
//	}

	/**
	 *	Registriert einen ActionListener;
	 *	Action-Events kommen, wenn sich der Wert des ParamFieldes aendert
	 */
	public void addActionListener( ActionListener list )
	{
		ground.actionComponent.addActionListener( list );
	}

	/**
	 *	Entfernt einen ActionListener
	 */
	public void removeActionListener( ActionListener list )
	{
		ground.actionComponent.removeActionListener( list );
	}

	/**
	 *	Aktuelle Box besorgen
	 *
	 *	@return	null, wenn Fehler oder keine Box angewaehlt
	 */
	public CircuitPanel.Box getActiveBox()
	{
		return ground.currentBox;
	}

	/**
	 *	Box auf das Panel legen
	 */
//	private void insertBox( Component newBox, int pos )
//	{
//		boolean		isBox	= newBox.getClass() == ground.dummyBox.getClass();
//		int			numBoxes;
//		int			i;
//		Component	oldBox;
//
//		synchronized( boxes ) {
//			con.gridx = 1;
//			con.gridy = 1;
//			switch( type ) {
//			case TYPE_SERIAL:
//				con.gridx += pos;
//				break;
//			case TYPE_PARALLEL:
//				con.gridy += pos;
//				break;
//			}
//			lay.setConstraints( newBox, con );
//			add( newBox, pos );
//			boxes.add( pos, newBox );
//			numBoxes = boxes.size();
//			
//			for( i = ++pos; i < numBoxes; i++ ) {
//				switch( type ) {
//				case TYPE_SERIAL:
//					con.gridx++;
//					break;
//				case TYPE_PARALLEL:
//					con.gridy++;
//					break;
//				}
//				oldBox = (Component) boxes.get( i );
//				lay.setConstraints( oldBox, con );
//			}
//		}
//		invalidate();
//		ground.validate();
//
//		if( isBox ) {
//			newBox.addMouseListener( this );
//			selectBox( (CanvasSC) newBox );
//		}
//	}

//	private void selectBox( CanvasSC box )
//	{
//		boolean changed;
//	
//		synchronized( boxes ) {
//			ground.currentBox.setSelected( CanvasSC.STATE_NORMAL );
//			if( box != null ) {
//				changed = (box != ground.currentBox);
//				ground.currentBox = box;
//				ground.currentBox.setSelected( CanvasSC.STATE_SELECTED );
//				// Listener benachrichtigen
//				if( changed ) notifyListener( ACTION_BOXSELECTED );
//			} else {
//				changed = (ground.currentBox != ground.dummyBox);
//				ground.currentBox = ground.dummyBox;
//				// Listener benachrichtigen
//				if( changed ) notifyListener( ACTION_BOXDESELECTED );
//			}
//		}
//	}

	/**
	 *	Box vom Panel entfernen
	 */
//	private void removeBox( Component box )
//	{
//		int			numBoxes;
//		int			i;
//		int			pos;
//
//		synchronized( boxes ) {
//			pos = boxes.indexOf( box );
//			
//			con.gridx = 1;
//			con.gridy = 1;
//			switch( type ) {
//			case TYPE_SERIAL:
//				con.gridx += pos;
//				break;
//			case TYPE_PARALLEL:
//				con.gridy += pos;
//				break;
//			}
//
//			box.removeMouseListener( this );
//			remove( box );
//			boxes.remove( box );
//			numBoxes = boxes.size();
//
//			for( i = pos; i < numBoxes; i++ ) {
//				box = (Component) boxes.get( i );
//				lay.setConstraints( box, con );
//				switch( type ) {
//				case TYPE_SERIAL:
//					con.gridx++;
//					break;
//				case TYPE_PARALLEL:
//					con.gridy++;
//					break;
//				}
//			}
//		}
//		invalidate();
//		ground.validate();
//
//		// Listener benachrichtigen
//		notifyListener( ACTION_BOXDELETED );
//	}

	/**
	 *	Box ersetzen
	 */
//	private void replaceBox( Component oldBox, Component newBox )
//	{
//		boolean	isBox;
//		int		pos;
//
//		synchronized( boxes ) {
//			isBox = oldBox.getClass() == ground.dummyBox.getClass();
//			remove( oldBox );
//			pos = boxes.indexOf( oldBox );
//			boxes.remove( oldBox );
//			oldBox.removeMouseListener( this );
//			if( !isBox ) {
//				((CircuitPanel) oldBox).clear();
//			}
//			// Listener benachrichtigen
//			notifyListener( ACTION_BOXDELETED );
//
//			con.gridx = 1;
//			con.gridy = 1;
//			switch( type ) {
//			case TYPE_SERIAL:
//				con.gridx += pos;
//				break;
//			case TYPE_PARALLEL:
//				con.gridy += pos;
//				break;
//			}
//			lay.setConstraints( newBox, con );
//			boxes.add( pos, newBox );
//			add( newBox, pos );
//		}
//		invalidate();
//		ground.validate();
//
//		isBox = newBox.getClass() == ground.dummyBox.getClass();
//		if( isBox ) {
//			newBox.addMouseListener( this );
//			selectBox( (CanvasSC) newBox );
//		}
//	}

	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
	
		Dimension	dim		= getSize();
		int			midY	= dim.height >> 1;
		int			cMidY;
		Component	c[];
		Rectangle	bounds;
//		Graphics2D	g2		= (Graphics2D) g;

		g.setColor( colrConnections );
		if( type != TYPE_SERIAL ) {
			g.fillRect( 2, midY - 2, 2, 5 );
			g.fillRect( dim.width - 4, midY - 2, 2, 5 );
		}
		if( type == TYPE_PARALLEL ) {
			c = getComponents();
			int maxMid = 0, minMid = dim.height;
			for( int i = 0; i < c.length; i++ ) {
				bounds	= c[ i ].getBounds();
				cMidY	= bounds.y + (bounds.height >> 1);
				g.drawLine( 4, cMidY, dim.width - 5, cMidY );
				if( cMidY < minMid ) minMid = cMidY;
				if( cMidY > maxMid ) maxMid = cMidY;
			}
			g.drawLine( 3, minMid, 3, midY - 3 );
			g.drawLine( 3, midY + 3, 3, maxMid );
			g.drawLine( dim.width - 4, minMid, dim.width - 4, midY - 3 );
			g.drawLine( dim.width - 4, midY + 3, dim.width - 4, maxMid );
		} else {
			g.drawLine( 4, midY, dim.width - 5, midY );
		}
	}

// -------- private Methoden --------

	private void notifyListeners( String actionStr )
	{
		ActionEvent e;
	
		e = new ActionEvent( ground, ActionEvent.ACTION_PERFORMED, actionStr );
		ground.actionComponent.dispatchEvent( e );
	}

	private int calcDirection( Component c, int x, int y )
	{
		Rectangle	bounds	= c.getBounds();
		int			dir		= 0;
		
		if( x < bounds.x )							dir  = DIR_WEST;
		else if( x >= bounds.x + bounds.width )		dir  = DIR_EAST;
		if( y < bounds.y )							dir += DIR_NORTH;
		else if( y >= bounds.y + bounds.height )	dir += DIR_SOUTH;

		return dir;
	}

	private int parseNumBoxes( String circuit, int cIndex )
	{
		int		numBoxes, bracketCount;
		char	c;
	
		for( numBoxes = 0; cIndex < circuit.length(); ) {
			c = circuit.charAt( cIndex++ );
			if( c == '{' ) {
				numBoxes++;
				for( bracketCount = 1; (bracketCount > 0) && (cIndex < circuit.length()); ) {
					c = circuit.charAt( cIndex++ );
					if( c == '{' ) {
						bracketCount++;
					} else if( c == '}' ) {
						bracketCount--;
					}
				}
				if( bracketCount != 0 ) return 0;	// corrupted!
			}
		}
		return numBoxes;
	}	

	private String parseSettings( String circuit, int cIndex )
	{
		int		startID, endID, bracketCount;
		char	ch;
	
		while( cIndex < circuit.length() ) {
			ch = circuit.charAt( cIndex++ );
			if( ch == '{' ) {
				startID = cIndex;
				for( bracketCount = 1; (bracketCount > 0) && (cIndex < circuit.length()); ) {
					ch = circuit.charAt( cIndex++ );
					if( ch == '{' ) {
						bracketCount++;
					} else if( ch == '}' ) {
						bracketCount--;
					}
				}
				if( bracketCount != 0 ) return "";	// corrupted!
				endID = cIndex - 1;
				return( circuit.substring( startID, endID ));
			}
		}
		return "";
	}
	
	private String encodeBox( Object o )
	{
		if( o instanceof CircuitPanel.Box ) {
			return( String.valueOf( TYPE_BOX ) + '{' + o.toString() + '}' );
		} else if( o instanceof CircuitPanel ) {
			return( String.valueOf( ((CircuitPanel) o).type ) + '{' + o.toString() + '}' );
		} else {
			assert false : o.getClass().getName();
		}
		return null;
	}
	
	public String toString()
	{
		StringBuffer str = new StringBuffer();
	
		str.append( type );
		synchronized( boxes ) {
			for( int i = 0; i < boxes.size(); i++ ) {
				str.append( encodeBox( boxes.get( i )));
			}
		}
		return str.toString();
	}

// ---------- internal classes / interfaces ----------

	public interface Box
	{
		public javax.swing.Icon getIcon();
		public CircuitPanel.Box duplicate();
		public String toString();
		public CircuitPanel.Box fromString( String s );
	}
	
//	public interface Group
//	{
//		public static final int SERIAL		= 0;
//		public static final int PARALLEL	= 1;
//		
//		public int getOrient();
//		public java.util.List getBoxes();
//	}
	
	private class BoxAction
	extends AbstractAction
	{
		private CircuitPanel.Box box;
	
		private BoxAction( CircuitPanel.Box box )
		{
			super();
			
			putValue( SMALL_ICON, box.getIcon() );
			this.box = box;
		}
		
		public void actionPerformed( ActionEvent e )
		{
			AbstractButton button = (AbstractButton) e.getSource();
		
			if( (e.getModifiers() & ActionEvent.ALT_MASK) != 0 ) {
				if( ground.currentBox != null ) {
					AbstractButton otherButton = (AbstractButton) ground.mapBoxToView.get( ground.currentBox );
					if( (otherButton) != null ) {
						otherButton.setSelected( false );
					}
					ground.currentBox = null;
					notifyListeners( ACTION_BOXDESELECTED );
				}
				int i = boxes.indexOf( this.box );
				if( i >= 0 ) {
					removeBox( i );
					if( (enc_cp != ground) && (boxes.size() < 2) ) {			// replace panel by box
						Object o2 = null;
						if( boxes.size() == 1 ) {
							o2 = boxes.get( 0 );
							removeBox( 0 );
						}
						CircuitPanel parent = (CircuitPanel) enc_cp.getParent();
						int index = parent.boxes.indexOf( enc_cp );
						parent.removeBox( index );
						if( o2 != null ) {
							parent.insertBox( o2, index );
						}
					} else if( boxes.isEmpty() ) {	// kill panel
//						CircuitPanel parent = (CircuitPanel) enc_cp.getParent();
					}

				}
			} else {
				if( button.isSelected() ) {
					if( ground.currentBox != null ) {
						AbstractButton otherButton = (AbstractButton) ground.mapBoxToView.get( ground.currentBox );
						if( (otherButton) != null && (otherButton != button) ) {
							otherButton.setSelected( false );
						}
					}
					ground.currentBox = this.box;
					notifyListeners( ACTION_BOXSELECTED );
				} else {
					if( ground.currentBox == this.box ) {
						ground.currentBox = null;
					}
					notifyListeners( ACTION_BOXDESELECTED );
				}
			}
		}
	}
}
// class CircuitPanel
