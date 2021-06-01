/*
 *  TimeFormat.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		03-Mar-05	copied from Meloncillo
 */
 
package de.sciss.fscape.util;

import java.text.*;
import java.util.*;

/**
 *  A <code>MessageFormat</code> subclass for
 *  displaying time values in minutes
 *  and seconds.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.61, 09-Aug-04
 *
 *  @see	de.sciss.meloncillo.gui.NumberField#FLAG_MINSEC
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
	private Object[] msgArgs  = new Object[2];
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
		super( (prefix == null ? "" : prefix) + "{0}:{1}" + (suffix == null ? "" : suffix), locale );
		
		isInteger				= (numDecimals == 0);
		NumberFormat formatSecs = NumberFormat.getInstance( locale );
		formatSecs.setMinimumIntegerDigits( 2 );
		formatSecs.setMaximumIntegerDigits( 2 );
		formatSecs.setMinimumFractionDigits( numDecimals );
		formatSecs.setMaximumFractionDigits( numDecimals );
		
		this.setFormatByArgumentIndex( 0, NumberFormat.getIntegerInstance( locale ));
		this.setFormatByArgumentIndex( 1, formatSecs );
	}
	
	/**
	 *  Creates a formatted string
	 *  using a parameter for seconds
	 *
	 *  @param  seconds		the time in seconds which will
	 *						be formatted using an MM:SS.millis pattern
	 *  @return the formatted string ready for display
	 */
	public String formatTime( Number seconds )
	{
		double d	= seconds.doubleValue();
		msgArgs[0]  = new Integer( (int) (d / 60) );	// XXX ?
		msgArgs[1]  = new Double( d % 60 );
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
		if( results.length != 2 || !(results[0] instanceof Number) || !(results[1] instanceof Number) ) {
			throw new ParseException( str, 0 );
		}
		if( isInteger ) {
			return new Long( ((Number) results[0]).longValue() * 60 + ((Number) results[1]).longValue() );
		} else {
			return new Double( ((Number) results[0]).longValue() * 60 + ((Number) results[1]).doubleValue() );
		}
	}
}
