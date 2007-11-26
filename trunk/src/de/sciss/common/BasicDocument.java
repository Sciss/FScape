/*
 *  BasicDocument.java
 *  (de.sciss.common package)
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
 *		02-Sep-06	created
 */

package de.sciss.common;

import de.sciss.app.Document;
import de.sciss.util.Flag;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 02-Oct-07
 */
public abstract class BasicDocument
implements Document
{
	public abstract ProcessingThread closeDocument( boolean force, Flag wasClosed );
	public abstract void start( ProcessingThread pt );
}
