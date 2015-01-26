/*
 *  ListDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  Dialog allowing the user to
 *	select a string from a list.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ListDlg
extends BasicDialog
implements ActionListener
{
// -------- private Variablen --------

	private static final int		GG_OK		= 0;
	private static final int		GG_CANCEL	= 1;

	private	final GUISupport		gui;
	private final JList				ggList;
	private final JButton			ggCancel, ggOk;

	private int						chosen		= -1;

// -------- public Methoden --------

	/**
	 *	setVisible() wird automatisch aufgerufen
	 *
	 *	@param parent	aufrufendes Fenster
	 *	@param title	Dialog-titel
	 *	@param list		Strings der Liste
	 */
	public ListDlg( Component parent, String title, String[] list )
	{
		super( parent, title, true );
//		setResizable( false );

		GridBagConstraints	con;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();

		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.fill		= GridBagConstraints.BOTH;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		ggList			= new JList( list );
//		for( int i = 0; i < list.length; i++ ) {
//			ggList.addItem( list[ i ]);
//		}
//		ggList.addItemListener( this );
		gui.addGadget( ggList, 9999 );
		
		con.gridwidth	= 1;
		con.fill		= GridBagConstraints.HORIZONTAL;
		con.weightx		= 0.0;
		con.weighty		= 0.0;
		ggCancel		= new JButton( " Cancel " );
		ggOk			= new JButton( "   Ok   " );
		gui.addButton( ggCancel, GG_CANCEL, this );
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "      " ));
		con.weightx		= 0.0;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addButton( ggOk, GG_OK, this );
		
		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e )
			{
				quit( -1 );
			}
		});

		getContentPane().add( gui );
		pack();
		Dimension d1 = getSize();
		Dimension d2 = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation( (d2.width - d1.width) >> 1, (d2.height - d1.height) >> 1 );
//		addKeyListener( this );
//		requestFocus();

		super.initDialog();

		setVisible( true );
	}

	protected void quit( int choice )
	{
		chosen = choice;
		setVisible( false );
		dispose();
	}

	/**
	 *	liefert das Auswahlergebnis
	 *
	 *	@return	Item Nummer von links nach rechts beginnend bei 0
	 */
	public int getList()
	{
		return chosen;
	}

// -------- Action Listener Methoden  --------

	public void actionPerformed( ActionEvent e )
	{
		switch( gui.getItemID( e )) {
		case GG_OK:
			quit( ggList.getSelectedIndex() );
			break;
		case GG_CANCEL:
			quit( -1 );
			break;
		}
	}

// -------- Item Listener Methoden --------
/*
	public void itemStateChanged( ItemEvent e )
	{
		// nothing XXX
	}*/
}
// class ListDlg
