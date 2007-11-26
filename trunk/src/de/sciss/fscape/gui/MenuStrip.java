/**
 *	Klasse zur Konstruktion von MenuStrips
 */

package de.sciss.fscape.gui;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 *	MenuBar-Klasse
 *
 *  @version	0.71, 15-Nov-07
 */
public class MenuStrip
extends JMenu
{
// -------- private Variablen --------

	protected Hashtable 		h;		// Objects = Menus/Items, Keys = ActionCommands (z.B. "&ZUndo")
	protected String			label;
	protected ActionListener	listener;

// -------- public Methoden --------
	//	public boolean setItemEnabled( String menuItem, boolean enable );
	//	public static boolean setItemEnabled( MenuBar mb, String menuItem, boolean enable );
	//	public boolean setItemJLabel( String menuItem, String label );
	//	public static boolean setItemJLabel( MenuBar mb, String menuItem, String label );
	//	public JMenuItem getItem( String menuItem );
	//	public static JMenuItem getItem( MenuBar mb, String menuItem );

	/**
	 *	@param	label		Name fuer den Strip
	 *	@param	menuItems	inneres Array: 1 String fuer normale Items, mehrere fuer SubMenus
	 *						( "&c"-Anfang fuer Shortcuts; "#"-Ende fuer JCheckBoxes )
	 *	@param	listener	ActionListener, der die Menuwahlen verarbeiten soll
	 */
	public MenuStrip( String label, String menuItems[][], ActionListener listener )
	{
		super( label );
	
		this.label		= label;
		this.listener	= listener;
		h				= new Hashtable();
		h.put( label, this );		// register myself

		createStrip( this, h, menuItems, listener );
	}
		
	/**
	 *	JMenuItem waehlbar/ghosted-Status aendern
	 *
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@param	enable		true (waehlbar) bzw. false (ghosted)
	 *	@return	false, wenn Item nicht gefunden wurde
	 */
	public boolean setItemEnabled( String menuItem, boolean enable )
	{
		JMenuItem mi = (JMenuItem) h.get( menuItem );
		if( mi != null ) {
			mi.setEnabled( enable );
		}
		return( mi != null );
	}
	
	/**
	 *	JMenuItem waehlbar/ghosted-Status aendern; durchsucht alle Menues einer MenuBar
	 *
	 *	@param	mb			MenuBar, in dessen Menus sich das Item befindet
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@param	enable		true (waehlbar) bzw. false (ghosted)
	 *	@return	false, wenn Item nicht gefunden wurde
	 */
	public static boolean setItemEnabled( JMenuBar mb, String menuItem, boolean enable )
	{
		MenuStrip	m;
		JMenuItem	mi;
		
		for( int i = 0; i < mb.getMenuCount(); i++ ) {
			m	= (MenuStrip) mb.getMenu( i );
			mi	= m.getItem( menuItem );
			if( mi != null ) {
				mi.setEnabled( enable );
				return true;
			}
		}
		return false;
	}

	/**
	 *	JLabel eines JMenuItems setzen
	 *
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@param	label		neuer String ggf. mit Shortcut; entspricht dem ActionCommand
	 *	@return	false, wenn Item nicht gefunden wurde
	 */
	public boolean setItemJLabel( String menuItem, String label )
	{
		JMenuItem mi = (JMenuItem) h.get( menuItem );
		if( mi != null ) {
			h.remove( menuItem );				// alter Key ungueltig
			setItemLabelAndCut( mi, label );
			h.put( menuItem, mi );				// mit neuem wieder anmelden
		}
		return( mi != null );
	}

	/**
	 *	JLabel eines JMenuItems setzen; durchsucht alle Menues einer MenuBar
	 *
	 *	@param	mb			MenuBar, in dessen Menus sich das Item befindet
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@param	label		neuer String ggf. mit Shortcut; entspricht dem ActionCommand
	 *	@return	false, wenn Item nicht gefunden wurde
	 */
	public static boolean setItemJLabel( JMenuBar mb, String menuItem, String label )
	{
		MenuStrip	m;
		JMenuItem	mi;
		
		for( int i = 0; i < mb.getMenuCount(); i++ ) {
			m	= (MenuStrip) mb.getMenu( i );
			mi	= m.getItem( menuItem );
			if( mi != null ) {
				return( m.setItemJLabel( menuItem, label ));
			}
		}
		return false;
	}

	/**
	 *	JMenuItem aus ActionCommand bzw. JLabel ermitteln
	 *
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@return	null, wenn Item nicht gefunden wurde
	 */
	public JMenuItem getItem( String menuItem )
	{
		return( (JMenuItem) h.get( menuItem ));
	}
	
	/**
	 *	JMenuItem aus ActionCommand bzw. JLabel ermitteln; durchsucht alle Menues einer MenuBar
	 *
	 *	@param	mb			MenuBar, in dessen Menus sich das Item befindet
	 *	@param	menuItem	ActionString des gewuenschten Items
	 *	@return	null, wenn Item nicht gefunden wurde
	 */
	public static JMenuItem getItem( JMenuBar mb, String menuItem )
	{
		MenuStrip	m;
		JMenuItem	mi;
		
		for( int i = 0; i < mb.getMenuCount(); i++ ) {
			m	= (MenuStrip) mb.getMenu( i );
			mi	= m.getItem( menuItem );
			if( mi != null ) {
				return mi;
			}
		}
		return null;
	}

// -------- quasi-protected Methoden --------

	/**
	 *	Strip erstellen
	 *	ONLY TO BE CALLED BY THIS CLASS OR POPUPSTRIP !!!
	 *
	 *	@param	m			aufrufender MenuStrip oder PopupStrip
	 *	@param	h			Hashtable mit Items als Objekte und ActionCommand als Keys
	 *	@param	menuItems	inneres Array: 1 String fuer normale Items, mehrere fuer SubMenus
	 *						( "&c"-Anfang fuer Shortcuts; "#"-Ende fuer JCheckBoxes )
	 *	@param	listener	ActionListener, der die Menuwahlen verarbeiten soll
	 */
	public static void createStrip( JMenu m, Hashtable h, String[][] menuItems, ActionListener listener )
	{
		JMenu		m2;
		JMenuItem	mi;
		boolean		sub;
		String		miStr;

		for( int i = 0; i < menuItems.length; i++ ) {
			sub = (menuItems[ i ].length > 1);
			m2 = m;

			for( int j = 0; j < menuItems[ i ].length; j++ ) {
				miStr = menuItems[ i ][ j ];
				if( miStr == null ) {
					m2.addSeparator();
				} else {
					if( (j == 0) && (sub) ) {
						m2	= new JMenu();
						mi	= m2;
						m.add( mi );
					} else {
						if( miStr.endsWith( "#" )) {	// JCheckBox
							mi = new JCheckBoxMenuItem();
						} else {
							mi = new JMenuItem();
						}
						m2.add( mi );
					}
					setItemLabelAndCut( mi, miStr );
					mi.addActionListener( listener );
					h.put( miStr, mi );		// Item in die Hashtable aufnehmen; Key = ActionCommand
				}
			}
		}
	}
	
	/**
	 *	Sichtbares JLabel und Shortcut des JMenuItems setzen
	 *	ONLY TO BE CALLED BY THIS CLASS AND POPUPSTRIP !!!
	 *
	 *	@param	mi		JMenuItem
	 *	@param	label	String ggf. inklusive Shortcut
	 */
	public static void setItemLabelAndCut( JMenuItem mi, String label )
	{
		mi.setActionCommand( label );
			
		if( label.endsWith( "#" )) {	// JCheckBox
			label = label.substring( 0, label.length() - 1 );
		}
		if( label.startsWith( "&" )) {
//			mi.setShortcut( new MenuShortcut( label.charAt( 1 )));
			label = label.substring( 2 );
		} else {
//			mi.deleteShortcut();
		}
		mi.setText( label );
	}
}
// class MenuStrip
