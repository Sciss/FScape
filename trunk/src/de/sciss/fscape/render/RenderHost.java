/*
 *  RenderHost.java
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
 *		14-Jul-05	created from de.sciss.meloncillo.render.PlugInHost
 *		??-???-??	copied from eisenkraut
 */

//package de.sciss.eisenkraut.render;
package de.sciss.fscape.render;

/**
 *	A simple extension to
 *	the <code>PlugInHost</code>
 *	interface which adds support
 *	for progress bar and exception
 *	display.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.56, 15-Sep-05
 */
public interface RenderHost
{
	/**
	 *	Called by the plug-in to
	 *	request a message display
	 *
	 *	@param	type	type of message as given by
	 *					the JOptionPane
	 *	@param	text	the text to display
	 *
	 *	@see	javax.swing.JOptionPane#WARNING_MESSAGE 
	 *	@see	javax.swing.JOptionPane#ERROR_MESSAGE 
	 *	@see	javax.swing.JOptionPane#INFORMATION_MESSAGE 
	 */
	public void	showMessage( int type, String text );
	
	/**
	 *	Determines if the host is active
	 *	that is processing or realtime enabled
	 *
	 *	@return	true if the host and thus the plug-in is actively processing
	 */
	public boolean isRunning();

	/**
	 *	Tells the host to update the progression bar.
	 *
	 *	@param	p	the new progression normalized
	 *				to 0.0 ... 1.0 . use -1 for
	 *				indeterminate mode
	 */
	public void setProgression( float p );
	
	/**
	 *	Saves the last internally caught exception.
	 *	This will be displayed when rendering aborts
	 *	with a failure.
	 *
	 *	@param	e	the recently caught exception
	 */
	public void setException( Exception e );
}