/*
 *  PopupStrip.java
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

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Hashtable;

/**
 *	Popmenu-Generator. Utilizes MenuStrip.createStrip() und MenuStrip.setItemLabelAndCut()!
 *	Bescheuerterweise kann MenuStrip selbst nicht von JPopupMenu abgeleitet werden,
 *	da ein MenuBar.add() dann nicht funktioniert... :-(
 */
public class PopupStrip
        extends JPopupMenu {

// -------- private variables --------

    protected Hashtable<String, JMenuItem> h;		// Objects = Menus/Items, Keys = ActionCommands (z.B. "&ZUndo")
    protected ActionListener	listener;

// -------- public methods --------
    //	public boolean setItemEnabled( String menuItem, boolean enable );
    //	public static boolean setItemEnabled( MenuBar mb, String menuItem, boolean enable );
    //	public boolean setItemJLabel( String menuItem, String label );
    //	public static boolean setItemJLabel( MenuBar mb, String menuItem, String label );
    //	public JMenuItem getItem( String menuItem );
    //	public static JMenuItem getItem( MenuBar mb, String menuItem );

    /**
     *	@param	menuItems	inneres Array: 1 String fuer normale Items, mehrere fuer SubMenus
     *						( "&c"-Anfang fuer Shortcuts; "#"-Ende fuer JCheckBoxes )
     *	@param	listener	ActionListener, der die Menuwahlen verarbeiten soll
     */
    public PopupStrip( String menuItems[][], ActionListener listener )
    {
        super();

        h				= new Hashtable<String, JMenuItem>();
        this.listener	= listener;

        PopupStrip.createStrip( this, h, menuItems, listener );
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
        JMenuItem mi = h.get( menuItem );
        if( mi != null ) {
            mi.setEnabled( enable );
        }
        return( mi != null );
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
            PopupStrip.setItemLabelAndCut( mi, label );
            h.put( menuItem, mi );				// mit neuem wieder anmelden
        }
        return( mi != null );
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

    public boolean addItem( String subMenu, String menuItem )
    {
        JMenu sm;
        Object o;

        JMenuItem mi = new JMenuItem();
        PopupStrip.setItemLabelAndCut( mi, menuItem );
        if( subMenu != null ) {
            o = getItem( subMenu );
            if( o == null || !(o instanceof JMenu) ) return false;
            sm = (JMenu) o;
            sm.add( mi );
        } else {
            this.add( mi );
        }

        mi.addActionListener( listener );
        h.put( menuItem, mi );

        return true;
    }

    public boolean removeItem( String menuItem )
    {
        JMenuItem mi = getItem( menuItem );
        if( mi != null ) {
            h.remove( menuItem );
            mi.removeActionListener( listener );
            mi.getParent().remove( mi );
        }
        return( mi != null );
    }

// -------- quasi-protected methods --------

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
    public static void createStrip( JPopupMenu m, Hashtable<String, JMenuItem> h, String[][] menuItems, ActionListener listener )
    {
        Object		m2;
        JMenuItem	mi;
        boolean		sub;
        String		miStr;

        for( int i = 0; i < menuItems.length; i++ ) {
            sub = (menuItems[ i ].length > 1);
            m2 = m;

            for( int j = 0; j < menuItems[ i ].length; j++ ) {
                miStr = menuItems[ i ][ j ];
                if( miStr == null ) {
                    if( m2 instanceof JPopupMenu ) {
                        ((JPopupMenu) m2).addSeparator();
                    } else if( m2 instanceof JMenu ) {
                        ((JMenu) m2).addSeparator();
                    }
                } else {
                    if( (j == 0) && (sub) ) {
                        m2	= new JMenu();
                        mi	= (JMenu) m2;
                        m.add( mi );
                    } else {
                        if( miStr.endsWith( "#" )) {	// JCheckBox
                            mi = new JCheckBoxMenuItem();
                        } else {
                            mi = new JMenuItem();
                        }
                        if( m2 instanceof JPopupMenu ) {
                            ((JPopupMenu) m2).add( mi );
                        } else if( m2 instanceof JMenu ) {
                            ((JMenu) m2).add( mi );
                        }
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