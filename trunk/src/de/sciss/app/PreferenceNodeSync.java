/*
 *  PreferenceNodeSync.java
 *  de.sciss.app package
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
 *		20-May-05	created from de.sciss.meloncillo.util.PreferenceNodeSync
 */

package de.sciss.app;

import java.util.prefs.Preferences;

/**
 *  Objects implementing this interface
 *  state that they will store their (and their children's)
 *  serialized representation in privately named
 *  entries in a given preference node
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 20-May-05
 */
public interface PreferenceNodeSync
{
	/**
	 *  Enables Preferences synchronization.
	 *  This method is not thread safe and
	 *  must be called from the event thread.
	 *  When a preference change is received,
	 *  the GUI is updated and dispatches an event
	 *  to registered listeners.
	 *  Likewise, if the user adjusts the GUI
	 *  value, the preference will be
	 *  updated. The same is true, if you
	 *  call one of the value changing methods.
	 *  
	 *  @param  prefs   the preferences node in which
	 *					the values are stored, or null
	 *					to disable prefs sync.
	 */
	public void setPreferences( Preferences prefs );
}