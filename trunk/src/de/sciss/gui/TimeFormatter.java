/*
 *  TimeFormatter.java
 *  de.sciss.gui package
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
 *		16-Sep-05	created
 */
 
package de.sciss.gui;

import java.text.ParseException;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;

/**
 *  @version	0.25, 05-May-06
 */
public class TimeFormatter
extends DefaultFormatter
{
	private final DocumentFilter	docFilter	= new DocFilter();
	private final NavigationFilter	navFilter	= new NavFilter();
	
	private static final String	sepChars		= ":.";
	private static final String	numChars		= "0123456789";

	private TimeFormat				tf			= null;

	public TimeFormatter()
	{
		super();
		setAllowsInvalid( true );
//		setOverwriteMode( true );	// fucked up because we use our own filter
		setCommitsOnValidEdit( false );
	}
	
	public void setFormat( TimeFormat tf )
	{
		this.tf	= tf;
	}
	
	public Object stringToValue( String string )
	throws ParseException
	{
		if( tf != null ) {
			return tf.parseTime( string );
		} else {
			throw new ParseException( "Format hasn't been set", 0 );
		}
	}
	
	public String valueToString( Object value )
    throws ParseException
	{
		if( tf != null ) {
			return tf.formatTime( (Number) value );
		} else {
			return value.toString();
		}
	}

	protected DocumentFilter getDocumentFilter()
	{
		return docFilter;
	}

	protected NavigationFilter getNavigationFilter()
	{
		return navFilter;
	}

	private class DocFilter
	extends DocumentFilter
	{
		public void insertString( DocumentFilter.FilterBypass fb, int off, String s, AttributeSet attr )
		throws BadLocationException
		{
			replace( fb, off, s.length(), s, attr );
//System.err.println( "insertString "+off+" : "+s );
		}
		
		public void remove( DocumentFilter.FilterBypass fb, int off, int len )
		throws BadLocationException
		{
//System.err.println( "remove "+off+" : "+len );
//			super.remove( fb, off, len );
			final String s = fb.getDocument().getText( 0, off + 1 );
			
			for( int i = 0; i <= off; i++ ) {
				if( sepChars.indexOf( s.charAt( i )) >= 0 ) return;
			}
			
			super.remove( fb, off, len );
		}

		public void replace( DocumentFilter.FilterBypass fb, int off, int len, String s, AttributeSet attr )
		throws BadLocationException
		{
//			final String oldTxt = fb.getDocument().getText( 0, off + len );
			final String oldTxt = getFormattedTextField().getText();
			
			for( int i = 0; i < off; i++ ) {
				if( sepChars.indexOf( oldTxt.charAt( i )) >= 0 ) {
					len = s.length();
					if( off + len > oldTxt.length() ) return;
					break;
				}
			}
			if( !(s.equals( "-" ) && (off == 0)) ) {
				for( int i = 0; i < s.length(); i++ ) {
					if( numChars.indexOf( s.charAt( i )) == -1 ) return;
				}
			}
//			super.insertString( fb, off, s, attr );
//
//			len = s.length();	// overwrite
		
			char			ch1, ch2;
			
			for( int i = 0, j = off; i < Math.min( s.length(), len ); i++, j++ ) {
				ch1 = oldTxt.charAt( j );
				ch2	= s.charAt( i );
				if( sepChars.indexOf( ch1 ) >= 0 ) {
					if( sepChars.indexOf( ch2 ) == -1 ) {
						replace( fb, off + 1, len, s, attr );
						try {
							getFormattedTextField().setCaretPosition( off + 1 + len );
						}
						catch( IllegalArgumentException e1 ) {}
						return;
					}
				} else {
					if( numChars.indexOf( ch2 ) >= 0 ) continue;
//					if( ch2 == ' ' ) {
//						s2	= fb.getDocument().getText( 0, off + i );
//						for( int j = 0; j < s2.length(); j++ ) {
//							if( s2.charAt( j ) != ' ' ) return;
//						}
//					}
					return;
				}
			}

//System.err.println( "replace "+off+" : "+len+" : "+s );
//			super.replace( fb, off, len, s, attr );
			fb.replace( off, len, s, attr );
//			fb.remove( off, len );
//			fb.insertString( off, s, attr );
		}
	}
	
	private static class NavFilter
	extends NavigationFilter
	implements SwingConstants
	{
		public void setDot( NavigationFilter.FilterBypass fb, int dot, Position.Bias bias )
		{
			super.setDot( fb, dot, bias );
		}

		public void moveDot( NavigationFilter.FilterBypass fb, int dot, Position.Bias bias )
		{
			super.moveDot( fb, dot, bias );
		}

		public int getNextVisualPositionFrom( JTextComponent c, int pos, Position.Bias bias,
											  int dir, Position.Bias[] biasRet )
		throws BadLocationException
		{
			final String s = c.getText();
			if( (dir == WEST) && (pos > 0) && (pos <= s.length()) ) {
				if( sepChars.indexOf( s.charAt( pos - 1 )) >= 0 ) {
					pos--;
				}
			} else if( (dir == EAST) && (pos + 1 < s.length()) ) {
				if( sepChars.indexOf( s.charAt( pos + 1 )) >= 0 ) {
					pos++;
				}
			}
			return super.getNextVisualPositionFrom( c, pos, bias, dir, biasRet );
		}
	}
}