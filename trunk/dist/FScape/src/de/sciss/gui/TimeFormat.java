/*
 *  TimeFormat.java
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
 *		25-Jan-05	created from de.sciss.meloncillo.util.TimeFormat
 *		17-Sep-05	extended to display hours
 */

package de.sciss.gui;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 *  A <code>MessageFormat</code> subclass for
 *  displaying time values in minutes
 *  and seconds.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.3, 03-Dec-05
 *
 *  @see	de.sciss.gui.NumberField#HHMMSS
 *
 *  @todo   this <strong>might</strong> be buggy (in apple's VM).
 *			the transport position label used to feed a time format
 *			which caused StackOverflow errors are a while. those errors
 *			disappeared when we programmed a custom label. however the
 *			bug might also come from JLabel. it should be checked if
 *			high frequency updates in the SimpleTransmitterEditor's setCursorInfo
 *			call -- which still use a TimeFormat -- provoke those StackOverflowErrors.
 */
public class TimeFormat
extends MessageFormat
{
	private final Object[] msgArgs  = new Object[3];
	private boolean isInteger;

	/**
	 *  Constructs a new <code>TimeFormat</code>
	 *  with automatically created pattern.
	 *
	 *  @param  flags			0 at the moment
	 *  @param  prefix			a string to prepend to the formatted string, or <code>null</code>
	 *  @param  suffix			a string to append to the formatted string, or <code>null</code>
	 *  @param  numDecimals		number of decimals, e.g. 3 for millisecs, 0 for secs
	 *  @param  locale			<code>Locale</code> to choose for number formatting
	 */
	public TimeFormat( int flags, String prefix, String suffix, int numDecimals, Locale locale )
	{
		super( (prefix == null ? "" : prefix) + "{0}:{1}:{2}" + (suffix == null ? "" : suffix), locale );
		
		isInteger						= (numDecimals == 0);
		final NumberFormat formatMin	= NumberFormat.getInstance( locale );
		final NumberFormat formatSec	= NumberFormat.getInstance( locale );
		final NumberFormat formatHour	= NumberFormat.getIntegerInstance( locale );
		formatSec.setMinimumIntegerDigits( 2 );
		formatSec.setMaximumIntegerDigits( 2 );
		formatSec.setMinimumFractionDigits( numDecimals );
		formatSec.setMaximumFractionDigits( numDecimals );
		formatMin.setMinimumIntegerDigits( 2 );
		formatMin.setMaximumIntegerDigits( 2 );
		formatMin.setMinimumFractionDigits( 0 );
		formatMin.setMaximumFractionDigits( 0 );
		formatHour.setGroupingUsed( false );

		this.setFormatByArgumentIndex( 0, formatHour );
		this.setFormatByArgumentIndex( 1, formatMin );
		this.setFormatByArgumentIndex( 2, formatSec );
	}
	
	/**
	 *  Creates a formatted string
	 *  using a parameter for seconds
	 *
	 *  @param  seconds		the time in seconds which will
	 *						be formatted using an MM:SS.millis pattern
	 *  @return the formatted string ready for display
	 *
	 *	@synchronization	when called concurrently, explicit synchronization must be enforced
	 */
	public String formatTime( Number seconds )
	{
		final int	millis	= (int) (seconds.floatValue() * 1000);
		final float	secs	= (float) (millis % 60000) / 1000;
		final int	mins	= millis / 60000;
		final int	hours	= mins / 60;
		
		msgArgs[0]	= new Integer( hours );
		msgArgs[1]  = new Integer( mins % 60 );
		msgArgs[2]  = new Float( secs );
		
		return this.format( msgArgs );
	}

	/**
	 *  Tries to parse a formatted time string
	 *  (as produced by <code>formatTime</code>).
	 *
	 *  @param  str		a formatted string such as "12:33.456"
	 *  @return the number found by parsing the string. Either
	 *			a Long, if the seconds are integer, otherwise
	 *			a Double containing decimals for milliseconds.
	 *  @throws ParseException  if the string cannot be parsed
	 */
	public Number parseTime( String str )
	throws ParseException
	{
		Object[] results = this.parse( str );
		if( results.length != 3 || !(results[0] instanceof Number) ||
			!(results[1] instanceof Number) || !(results[2] instanceof Number) ) {
			
			throw new ParseException( str, 0 );
		}
		if( isInteger ) {
			return new Long( ((Number) results[0]).longValue() * 3600 +
							 ((Number) results[1]).longValue() * 60 + ((Number) results[2]).longValue() );
		} else {
			return new Double( ((Number) results[0]).longValue() * 3600 +
							   ((Number) results[1]).longValue() * 60 + ((Number) results[2]).doubleValue() );
		}
	}
}