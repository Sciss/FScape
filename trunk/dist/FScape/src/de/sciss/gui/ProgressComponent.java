/*
 *  ProgressComponent.java
 *  (de.sciss.gui package)
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.ProgressComponent
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.event.ActionListener;

/**
 *  An interface for classes
 *  that are capable of displaying
 *  progression information to the user
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.61, 12-Oct-06
 */
public interface ProgressComponent
{
	public static final int	DONE		= 0;
	public static final int	FAILED		= 1;
	public static final int	CANCELLED	= 2;
	
	/**
	 *  Gets the component responsible
	 *  for displaying progression,
	 *  such as a ProgressBar.
	 */
	public Component getComponent();
	/**
	 *  Asks the component to reset
	 *  progression to zero at the
	 *  beginning of a process.
	 */
	public void resetProgression();
	/**
	 *  Asks the component to update
	 *  progression amount to the given value.
	 *
	 *  @param  p   the new progression amount between 0 and 1
	 */
	public void setProgression( float p );
	/**
	 *  Asks the component to indicate that the
	 *  progression is finished.
	 *
	 *  @param  success		whether the process was successful or not
	 */
	public void finishProgression( int result );
	/**
	 *  Asks the component to display a custom
	 *  string describing the current process stage
	 *
	 *  @param  text	text to display in the progression component
	 */
	public void setProgressionText( String text );
	/**
	 *  Asks the component to display a message
	 *  related to the process.
	 *
	 *  @param  type	what type of message it is. Values are those
	 *					from JOptionPane : INFORMATION_MESSAGE, WARNING_MESSAGE
	 *					PLAIN_MESSAGE or ERROR_MESSAGE
	 *  @param  text	the message text to output
	 */
	public void showMessage( int type, String text );
	/**
	 *  Asks the component to display an error dialog.
	 *
	 *  @param  e			an <code>Exception</code> describing the error
	 *						which occured
	 *  @param  processName the name of the process in which the error
	 *						occured. this is usually used as a dialog's title string
	 */
	public void displayError( Exception e, String processName );

	public void addCancelListener( ActionListener l );
	public void removeCancelListener( ActionListener l );
}