/*
 *  PromptDlg.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *	Prompt for a string. THIS WHOLE CLASS IS OBSOLETE AND SHOULDN'T BE USED ANY MORE.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class PromptDlg
extends Dialog
{
// -------- private Variablen --------

	private JTextField	ggText;
	private boolean		success = false;	// false bei abbruch

// -------- public Methoden --------
	// public String getString();

	/**
	 *	@param parent	aufrufendes Fenster
	 *	@param title	Dialog-Titel
	 *	@param defVal	voreingestellter Text
	 */
	public PromptDlg( Frame parent, String title, String defVal )
	{
		super( parent, title, true );
		
//		setFont( Main.getFont( Main.FONT_GUI ));
		setResizable( false );
		setLayout( new FlowLayout( FlowLayout.CENTER, 0, 0 ));
		ggText = new JTextField( defVal, 24 );
		add( ggText );
		
		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e )
			{
				success = false;
				setVisible( false );
				dispose();
			}
		});
		ggText.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				success = true;
				setVisible( false );
				dispose();
			}
		});

		pack();
		Dimension d1 = getSize();
		Dimension d2 = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation( (d2.width - d1.width) >> 1, (d2.height - d1.height) >> 1 );
		setVisible( true );
	}

	public PromptDlg( Frame parent, String title )
	{
		this( parent, title, "" );
	}

	/**
	 *	liefert Ergebnisstring
	 *
	 *	@return	null bei Cancel
	 */
	public String getString()
	{
		return( success ? ggText.getText() : null );
	}
}
// class PromptDlg
