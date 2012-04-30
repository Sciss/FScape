/*
 *  GUISupport.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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
 *		09-Jan-05	killed sleep/wake methods
 */
 
package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import de.sciss.gui.PathListener;

/**
 *  A panel for "easy" adding and administration
 *	of buttons. This is totally stupid and the
 *	whole GUI generation should be destroyed and
 *	put into XML files or so.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class GUISupport
extends JPanel
{
// -------- private Variablen --------

	private final Hashtable hObj;			// keys = IDs, objects = Gadgets
	private final Hashtable hID;			// keys = Gadgets, objects = IDs
	protected final GridBagLayout lay;
	protected final GridBagConstraints con;

	private int type;

// -------- public Methoden --------
	// public GridBagLayout getGridBagLayout();
	// public GridBagConstraints getGridBagConstraints();

	// public int getItemID( ComponentEvent e );
	// public Component getItemObj( int ID );
	// public void addLabel( JLabel lb );
	// public void addButton( JButton gg, int ID, ActionListener l );
	// public void addCanvas( Canvas gg, int ID, MouseListener l );
	// public void addCheckbox( JCheckBox gg, int ID, ItemListener l );
	// public void addChoice( JComboBox gg, int ID, ItemListener l );
	// public void addScrollbar( Scrollbar gg, int ID, AdjustmentListener l );
	// public void addScrollPane( ScrollPane gg, int ID, AdjustmentListener l );
	// public void addTextField( JTextField gg, int ID, ActionListener l );
	// public void addParamField( ParamField gg, int ID, ActionListener l );
	// public void addPathField( PathField gg, int ID, ActionListener l );
	// public void addFontField( FontField gg, int ID, ActionListener l );
	// public void addGadget( Component gg, int hashKey );
	// public void addGadget( Component gg, int hashKey, Container container );

	// public static void sendComponentAsleep( Component c );
	// public static boolean isComponentSleeping( Component c );
	// public static void wakeComponent( Component c );
	// public static void sendComponentAsleep( Frame f );
	// public static void wakeComponent( Frame f );
	// public static void sendMenuBarSleeping( Frame f );
	// public static boolean isMenuBarAsleep( Frame f );
	// public static void wakeMenuBar( Frame f );
	// public static void beep();
	
	// public JCheckBox stringToCheckbox( String s, int ID );
	// public JComboBox stringToChoice( String s, int ID );
	// public Scrollbar stringToScrollbar( String s, int ID );
	// public JTextField stringToJTextField( String s, int ID );

	// public String JCheckBoxToString( int ID );
	// public String JComboBoxToString( int ID );
	// public String ScrollbarToString( int ID );
	// public String JTextFieldToString( int ID );

	/**
	 *	@param type	GUI-Typ (ID)
	 */
	public GUISupport( int type )
	{
		hObj	= new Hashtable();
		hID		= new Hashtable();
		lay		= new GridBagLayout();
		con		= new GridBagConstraints();
		setLayout( lay );
		setType( type );
//		setFont( Main.getFont( Main.FONT_GUI ));
	}
	public GUISupport()
	{
		this( -1 );
	}

	/**
	 *	@return GUI-Typ (ID) oder -1, wenn keine festgelegt wurde
	 */
	public int getType()
	{
		return type;
	}
	/**
	 *	@param type	neuer GUI-Typ (ID)
	 */
	public void setType( int type )
	{
		this.type = type;
	}

	/**
	 *	@return	speziellen LayoutManager vom Typ GridBagLayout
	 */
	public GridBagLayout getGridBagLayout()
	{
		return lay;
	}
	/**
	 *	@return	zum Layout gehoerende Constraints; immer _diese_ modifizieren!
	 */
	public GridBagConstraints getGridBagConstraints()
	{
		return con;
	}

	/**
	 *	Gadget-ID �ber gesendetes ComponentEvent
	 *
	 *	@param e	ComponentEvent, das durch Gadgetklick ausgeloest wurde
	 *	@return	Gadget-ID, -1 bei Fehler
	 */
	public int getItemID( EventObject e )
//	public int getItemID( AWTEvent e )
	{
		Integer ID = (Integer) hID.get( e.getSource());
		if( ID != null )
			return( ID.intValue() );
		else
			return -1;
	}

	/**
	 *	Gadget-Object �ber ID-Parameter aus dem Hash ermitteln
	 *
	 *	@param ID	Gadget-ID
	 *	@return	Componente, null bei Fehler
	 */
	public Component getItemObj( int ID )
	{
		return( (Component) hObj.get( new Integer( ID )));
	}

	/**
	 *	@param lb	hinzuzufuegende Componente vom Typ JLabel
	 */
	public void addLabel( JLabel lb )
	{
		lay.setConstraints( lb, con );
		add( lb );	
	}
	public void addLabel( GroupLabel lb )
	{
		lay.setConstraints( lb, con );
		add( lb );	
	}
	
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ JButton
	 *	@param ID	ID, ueber die bei JButtonclick das Object identifiziert werden kann
	 *	@param l	wo sollen Clicks gemeldet werden?
	 */
	public void addButton( JButton gg, int ID, ActionListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addActionListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ Canvas
	 *	@param ID	ID, ueber die bei Mausereignissen das Object identifiziert werden kann
	 *	@param l	wo sollen Mausereignisse gemeldet werden? darf null sein
	 */
	public void addCanvas( JComponent gg, int ID, MouseListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addMouseListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ JCheckBox
	 *	@param ID	ID, ueber die bei Buttonclick das Object identifiziert werden kann
	 *	@param l	wo sollen Clicks gemeldet werden?
	 */
	public void addCheckbox( JCheckBox gg, int ID, ItemListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addItemListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ JComboBox
	 *	@param ID	ID, ueber die bei Buttonclick das Object identifiziert werden kann
	 *	@param l	wo sollen Clicks gemeldet werden?
	 */
	public void addChoice( JComboBox gg, int ID, ItemListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addItemListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ Scrollbar
	 *	@param ID	ID, ueber die bei Veraenderungen das Object identifiziert werden kann
	 *	@param l	wo sollen Veraenderungen gemeldet werden?
	 */
	public void addScrollbar( Scrollbar gg, int ID, AdjustmentListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addAdjustmentListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ ScrollPane
	 *	@param ID	ID, ueber die bei Veraenderungen das Object identifiziert werden kann
	 *	@param l	wo sollen Veraenderungen gemeldet werden?
	 */
	public void addScrollPane( JScrollPane gg, int ID, AdjustmentListener l )
	{
		addGadget( gg, ID );
// XXX		gg.getHAdjustable().addAdjustmentListener( l );
// XXX		gg.getVAdjustable().addAdjustmentListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ Textfield
	 *	@param ID	ID, ueber die bei Veraenderung das Object identifiziert werden kann
	 *	@param l	wo sollen Veraenderungen gemeldet werden?
	 */
	public void addTextField( JTextField gg, int ID, ActionListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addActionListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ ParamField
	 *	@param ID	ID, ueber die bei Veraenderung das Object identifiziert werden kann
	 *	@param l	wo sollen Veraenderungen gemeldet werden?
	 */
	public void addParamField( ParamField gg, int ID, ParamListener l )
//	public void addParamField( ParamField gg, int ID, ActionListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addParamListener( l );
	}
	/**
	 *	@param gg	hinzuzufuegende Componente vom Typ PathField
	 *	@param ID	ID, ueber die bei Veraenderung das Object identifiziert werden kann
	 *	@param l	wo sollen Veraenderungen gemeldet werden?
	 */
	public void addPathField( PathField gg, int ID, PathListener l )
//	public void addPathField( PathField gg, int ID, ActionListener l )
	{
		addGadget( gg, ID );
		if( l != null ) gg.addPathListener( l );
	}
//	/**
//	 *	@param gg	hinzuzufuegende Componente vom Typ FontField
//	 *	@param ID	ID, ueber die bei Veraenderung das Object identifiziert werden kann
//	 *	@param l	wo sollen Veraenderungen gemeldet werden?
//	 */
//	public void addFontField( FontField gg, int ID, ActionListener l )
//	{
//		addGadget( gg, ID );
////		gg.addActionListener( l );
//	}

	/**
	 *	Fuegt eine Componente dem GUI-Panel hinzu
	 *
	 *	@param	hashKey	Gadget-ID wie auch bei addCheckbox etc.
	 */
	public void addGadget( Component gg, int hashKey )
	{
		lay.setConstraints( gg, con );
		add( gg );
		registerGadget( gg, hashKey );
	}

	/**
	 *	Registriert eine Componente beim GUI-Panel;
	 *	d.h. sie kann ueber getItemObj/ID identifiziert werden
	 *
	 *	@param	hashKey	Gadget-ID wie auch bei addCheckbox etc.
	 */
	public void registerGadget( Component gg, int hashKey )
	{
		Integer ID = new Integer( hashKey );
		hObj.put( ID, gg );		// ID = key
		hID.put( gg, ID );		// ID = value
	}

	/**
	 *	Haengt alle Strings in einem Array
	 *	als Items an ein JComboBox-Gadget
	 */
	public static void addItemsToChoice( String items[], JComboBox gg )
	{
		for( int i = 0; i < items.length; i++ ) {
			gg.addItem( items[ i ]);
		}
	}

	/**
	 *	Haengt alle Elemente einer String-Enumeration
	 *	als Items an ein JComboBox-Gadget
	 *
	 *	IMPORTANT: auf die Elemente wird ein String-ClassCast ausgefuehrt!!
	 *
	 *	@param	sort	true, wenn alphabetisch sortiert werden soll
	 */
	public static void addItemsToChoice( Enumeration items, JComboBox gg, boolean sort )
	{
		String item;
	
		if( sort ) {
			String	itemUC;
			int		i;
			while( items.hasMoreElements() ) {
				item	= (String) items.nextElement();
				itemUC	= item.toUpperCase();
				for( i = 0; i < gg.getItemCount(); i++ ) {
					if( itemUC.compareTo( gg.getItemAt( i ).toString().toUpperCase() ) < 0 ) break;
				}
				gg.insertItemAt( item, i );
			}

		} else {
			while( items.hasMoreElements() ) {
				item = (String) items.nextElement();
				gg.addItem( item );
			}
		}
	}

	/**
	 *	Gibt Warnton aus
	 */
	public static void beep()
	{
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	 *	Laesst ein Rechteck von einer Position
	 *	zur anderen "fahren"
	 *
	 *	@param	g	sollte im XOR-Modus sein!
	 */
	public static void rubberGlide( Rectangle src, Rectangle dest, Graphics g )
	{
		int		x1, y1, x2, y2, j;
		long	startTime;
	
		for( int i = 0; i < 17; i++ ) {
		
			j = 16 - i;
			x1 = (src.x * j + dest.x * i) >> 4;
			y1 = (src.y * j + dest.y * i) >> 4;
			x2 = ((src.x + src.width) * j + (dest.x + dest.width) * i) >> 4;
			y2 = ((src.y + src.height) * j + (dest.y + dest.height) * i) >> 4;

			startTime = System.currentTimeMillis();
			g.drawRect( x1, y1, x2 - x1 - 1, y2 - y1 - 1 );
			while( (System.currentTimeMillis() - startTime) < 12 ) ;	// Thread.sleep() not accurate
			g.drawRect( x1, y1, x2 - x1 - 1, y2 - y1 - 1 );
		}
	}

	/**
	 *	Konvertiert einen Font in den Text eines JTextFields
	 */
	public static void fontToJTextField( Font fnt, JTextField tf )
	{
		tf.setText( fnt.getName() + " (" + fnt.getSize() + " pt)" );
		tf.setFont( fnt );
	}

	/**
	 *	Konvertiert String nach Boolean und setzt demenstprechend ein JCheckBox-Gadget
	 *
	 *	@param	s	String, der einen Boolean-Wert repraesentiert; darf null sein
	 *	@param	ID	Gadget-ID einer JCheckBox auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public JCheckBox stringToCheckbox( String s, int ID )
	{
		boolean		selected;
		JCheckBox	gg			= (JCheckBox) getItemObj( ID );

		if( gg != null ) {
			try {
				selected = Boolean.valueOf( s ).booleanValue();
				gg.setSelected( selected );
				return gg;
			}
			catch( NumberFormatException e ) {}
		}
		return null;
	}

	/**
	 *	Konvertiert String nach Integer und setzt demenstprechend ein JComboBox-Gadget
	 *
	 *	@param	s	String, der einen Integer-Wert repraesentiert; darf null sein
	 *	@param	ID	Gadget-ID eines JComboBoxs auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public JComboBox stringToChoice( String s, int ID )
	{
		int		chosen;
		JComboBox	gg		= (JComboBox) getItemObj( ID );

		if( gg != null ) {
			try {
				chosen = Integer.parseInt( s );
				gg.setSelectedIndex( chosen );
				return gg;
			}
			catch( NumberFormatException e ) {}
		}
		return null;
	}

	/**
	 *	Konvertiert String nach Integer und setzt demenstprechend ein Scrollbar-Gadget
	 *
	 *	@param	s	String, der einen Integer-Wert repraesentiert; darf null sein
	 *	@param	ID	Gadget-ID einer Scrollbar auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public Scrollbar stringToScrollbar( String s, int ID )
	{
		int			position;
		Scrollbar	gg		= (Scrollbar) getItemObj( ID );

		if( gg != null ) {
			try {
				position = Integer.parseInt( s );
				gg.setValue( position );
				return gg;
			}
			catch( NumberFormatException e ) {}
		}
		return null;
	}

	/**
	 *	Setzt den Inhalt eines JTextField-Gadget
	 *
	 *	@param	s	Text, den das Gadget erhalten soll
	 *	@param	ID	Gadget-ID eines JTextFields auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public JTextField stringToJTextField( String s, int ID )
	{
		JTextField gg = (JTextField) getItemObj( ID );

		if( gg != null ) {
			gg.setText( s );
			return gg;
		}
		return null;
	}

	/**
	 *	Setzt den Inhalt eines PathField-Gadget
	 *
	 *	@param	s	Text, den das Gadget erhalten soll
	 *	@param	ID	Gadget-ID eines PathFields auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public PathField stringToPathField( String s, int ID )
	{
		PathField gg = (PathField) getItemObj( ID );

		if( gg != null ) {
			gg.setPath( new File( s ));
			return gg;
		}
		return null;
	}

	/**
	 *	Setzt den Inhalt eines ParamField-Gadget
	 *
	 *	@param	s	Parameter, den das Gadget erhalten soll
	 *	@param	ID	Gadget-ID eines ParamFields auf diesem GUI
	 *	@return	das Gadget oder null bei Fehler
	 */
	public ParamField stringToParamField( String s, int ID )
	{
		ParamField gg = (ParamField) getItemObj( ID );

		if( gg != null ) {
// NOT YET			gg.setParam( s );
			return gg;
		}
		return null;
	}

	/**
	 *	Liest JCheckBox-Gadget aus und liefert eine String-Repraesentation von dessen Zustand
	 *
	 *	@param	ID	Gadget-ID einer JCheckBox auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String checkboxToString( int ID )
	{
		JCheckBox gg = (JCheckBox) getItemObj( ID );

		if( gg != null ) {
			return String.valueOf( gg.isSelected() );
		}
		return null;
	}
	
	/**
	 *	Liest JComboBox-Gadget aus und liefert eine String-Repraesentation von dessen Zustand
	 *
	 *	@param	ID	Gadget-ID eines JComboBoxs auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String choiceToString( int ID )
	{
		JComboBox gg = (JComboBox) getItemObj( ID );

		if( gg != null ) {
			return String.valueOf( gg.getSelectedIndex() );
		}
		return null;
	}

	/**
	 *	Liest Scrollbar-Gadget aus und liefert eine String-Repraesentation von dessen Zustand
	 *
	 *	@param	ID	Gadget-ID einer Scrollbar auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String scrollbarToString( int ID )
	{
		Scrollbar gg = (Scrollbar) getItemObj( ID );

		if( gg != null ) {
			return String.valueOf( gg.getValue() );
		}
		return null;
	}

	/**
	 *	Liest JTextField-Gadget aus
	 *
	 *	@param	ID	Gadget-ID eines JTextFields auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String textFieldToString( int ID )
	{
		JTextField gg = (JTextField) getItemObj( ID );

		if( gg != null ) {
			return gg.getText();
		}
		return null;
	}

	/**
	 *	Liest PathField-Gadget aus
	 *
	 *	@param	ID	Gadget-ID eines PathFields auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String pathFieldToString( int ID )
	{
		PathField gg = (PathField) getItemObj( ID );

		if( gg != null ) {
			return gg.getPath().getPath();
		}
		return null;
	}

	/**
	 *	Liest ParamField-Gadget aus
	 *
	 *	@param	ID	Gadget-ID eines ParamFields auf diesem GUI
	 *	@return	null bei Fehler
	 */
	public String paramFieldToString( int ID )
	{
//		ParamField gg = (ParamField) getItemObj( ID );
//
// XXX NOT YET		if( gg != null ) {
//			return gg.getParam();
//		}
		return null;
	}

//	/**
//	 *	Displays a dialog with an error message
//	 *	retrieved from Exception e
//	 *
//	 *	@param	e		may be null
//	 *	@param	parent	may be null (only a beep will be output)
//	 */
//	public static void displayError( Frame parent, Exception e )
//	{
//		String		className;
//		int			i;
//		String		msg;
//		String[]	options = { "Print stack", "Ok" };
//	
//		if( parent != null ) {
//			if( e != null ) {
//				className	= e.getClass().getName();
//				i			= className.lastIndexOf( '.' );
//				msg			= e.getLocalizedMessage();
//				if( className.endsWith( "Exception" )) {
//					className = ((i > 0) ? ('(' + className.substring( 0, i ) + ") ") : "") +
//								className.substring( i + 1, className.length() - 9 );
//				} else {
//					className = ((i > 0) ? ('(' + className.substring( 0, i ) + ") ") : "") +
//								className.substring( i + 1 );
//				}
//				i = JOptionPane.showOptionDialog( parent, className + ((msg != null) ? ("\n"+msg) : ""), "Error", JOptionPane.YES_NO_OPTION,
//						JOptionPane.ERROR_MESSAGE, null, options, options[1] );
//				if( i == 0 ) {
//					e.printStackTrace();
//				}
//			} else {
//				JOptionPane.showMessageDialog( Main.getWindow(), "Unknown error occured.\n" +
//					"Please report how this happened!" );
//			}
//		}
//	}
}
// class GUISupport
