/*
 *  BasicGraphicsHandler.java
 *  de.sciss.common package
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

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import de.sciss.app.GraphicsHandler;

public class BasicGraphicsHandler
implements GraphicsHandler
{
	private final String	systemTypeFace;
	private final String	userTypeFace;
	private final String	labelTypeFace;
	private final int		mediumSize, smallSize, miniSize;
	
	private final Map		mapFonts	= new HashMap();

	public BasicGraphicsHandler()
	{
		final String os			= System.getProperty( "os.name" );
		final boolean isMacOS	= os.indexOf( "Mac OS" ) >= 0;
        final boolean isWindows	= os.indexOf( "Windows" ) >= 0;

		if( isMacOS ) {
			systemTypeFace = userTypeFace = labelTypeFace = "LucidaGrande";
		} else if( isWindows ) {
//			systemTypeFace = userTypeFace = labelTypeFace = "Arial";
			systemTypeFace = userTypeFace = labelTypeFace = "Tahoma";
		} else {
			systemTypeFace = userTypeFace = labelTypeFace = "SansSerif";
		}
		
		mediumSize	= 13;
		smallSize	= 11;
		miniSize	= 9;
	}
	
	public Font getFont( int type )
	{
		final Object key = new Integer( type );
		Font f = (Font) mapFonts.get( key );
		if( f != null ) return f;
	
		final int		size;
		final String	face;
		final int		style;

		switch( type & FONT_SIZE_MASK ) {
		case FONT_MEDIUM:
			size	= mediumSize;
			break;
		case FONT_SMALL:
			size	= smallSize;
			break;
		case FONT_MINI:
			size	= miniSize;
			break;
		default:
			throw new IllegalArgumentException( "Invalid type " + type );
		}			
	
		switch( type & FONT_TYPE_MASK ) {
		case FONT_SYSTEM:
			face	= systemTypeFace;
			style	= Font.PLAIN;
			break;
		case FONT_BOLDSYSTEM:
			face	= systemTypeFace;
			style	= Font.BOLD;
			break;
		case FONT_USER:
			face	= userTypeFace;
			style	= Font.PLAIN;
			break;
		case FONT_LABEL:
			face	= labelTypeFace;
			style	= Font.PLAIN;
			break;
		default:
			throw new IllegalArgumentException( "Invalid type " + type );
		}
		
		f = new Font( face, style, size );
		mapFonts.put( key, f ); 
		return f;
	}
}