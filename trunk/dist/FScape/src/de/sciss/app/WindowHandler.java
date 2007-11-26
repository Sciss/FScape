/*
 *  WindowHandler.java
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
 *		21-May-05	created
 *		09-Jul-06	uses Window instead of JFrame
 */

package de.sciss.app;

import java.util.Iterator;
import java.util.Map;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.15, 19-Aug-06
 */
public interface WindowHandler
{
	public static final Object OPTION_EXCLUDE_FONT		= "excludefont";	// value : java.util.List of components
	public static final Object OPTION_GLOBAL_MENUBAR	= "globalmenu";		// value : null

//	public void addWindow( Window w, Map options );
//	public void removeWindow( Window w, Map options );
	public void addWindow( AbstractWindow w, Map options );
	public void removeWindow( AbstractWindow w, Map options );
	public Iterator getWindows();
	public AbstractWindow createWindow( int flags );
	public boolean usesInternalFrames();
	public boolean usesScreenMenuBar();
}
