/*
 *  Presets.java
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
 */

package de.sciss.fscape.prop;

import java.io.*;
import java.util.*;

/**
 *	Abgeleitete Properties-Klasse zur Verwaltung von Presets
 *	Einem Key-Namen stet als Value eine Properties-Liste gegenueber, die automatisch
 *	in einen "String" umgewandelt wird (durch Ersetzen der Linefeeds)
 *
 *  @version	0.71, 14-Nov-07
 */
public class Presets
extends BasicProperties
{
// -------- public Variablen --------

	/**
	 *	Name fuer den Default-Preset
	 */
	public static final String DEFAULT = ".default";

// -------- private Variablen --------
// -------- public Methoden --------
	
	/**
	 *	Legt das Presets-Objekt an mit entsprechenden Default-Eintraegen
	 *
	 *	@param owner		Inhaber der Preferences (daraus wird der Name abgeleitet)
	 *	@param defPreset	voreingestellter Preset
	 */
	public Presets( Class owner, Properties defPreset )
	{
		// default Properties, der Rest wird von oben gemanagt
		super( owner, createDefaults( defPreset ), "pst" );
	}

	/**
	 *	Aendert einen Preset bzw. legt diesen ggf. an
	 *
	 *	@param	name	Preset-Name
	 *	@param	val	Werte des Presets in Properties-Form
	 *	@return	Wert als String, null bei Fehler
	 */
	public String setPreset( String name, Properties val )
	{
		name			= stringToKey( name );
		String strVal	= Presets.propertiesToValue( val );

		if( strVal != null ) {
			synchronized( this ) {
				put( name, strVal );
				modified = true;	// indicate that presets should be saved on exit
			}
		}
		return strVal;
	}
	
	/**
	 *	Besorgt einen Preset
	 *
	 *	@param	name	Preset-Name
	 *	@return	entsprechende Werte als Properties-Object oder null bei Fehler
	 */
	public Properties getPreset( String name )
	{
		name			= stringToKey( name );
		String strVal;
		synchronized( this ) {
			strVal	= getProperty( name );
		}
		Properties val	= null;

		if( strVal != null ) {
			val = Presets.valueToProperties( strVal );
		}
		return val;
	}

	/**
	 *	Loescht einen Preset
	 *
	 *	@param	name	Preset-Name
	 *	@return	vorheriger Wert als String oder null wenn nicht existent
	 */
	public String removePreset( String name )
	{
		String val;

		name = stringToKey( name );

		synchronized( this ) {
			val	= (String) remove( name );
			if( val != null) modified = true;
		}
		return val;
	}

	/**
	 *	Liste aller Preset-Namen ermitteln
	 */
	public Iterator presetNames()
	{
		java.util.List li = new ArrayList();
		synchronized( this ) {
			Enumeration	e = propertyNames();

			while( e.hasMoreElements() ) {
				li.add( keyToString( (String) e.nextElement() ));
			}
		}
		return li.iterator();
	}

	/**
	 *	Schreibt die uebergebenen Properties in einen ByteStream und
	 *	liefert diesen als String zurueck
	 *
	 *	@return	null bei Fehler
	 */
	public static String propertiesToValue( Properties val )
	{
		ByteArrayOutputStream	outStream	= new ByteArrayOutputStream();
		String					strVal;

		try {
			val.store( outStream, null );
			strVal = outStream.toString();		// now we have the Properties as a String
			outStream.close();
			return strVal;
		}
		catch( IOException e ) {
			return null;
		}	
	}

	/**
	 *	Schreibt die uebergebenen String in einen ByteStream und
	 *	liefert diesen als Properties-Objekt zurueck
	 *
	 *	@param	null erlaubt, ergibt aber auch null als Ergebnis!
	 *	@return	null bei Fehler
	 */
	public static Properties valueToProperties( String strVal )
	{
		ByteArrayInputStream	inStream;
		Properties				val			= new Properties();

		try {
			if( strVal != null ) {
				inStream = new ByteArrayInputStream( strVal.getBytes() );
				val.load( inStream );	// now we have the String as a Properites-Object
				inStream.close();
				return val;
			}
		}
		catch( IOException e ) {}
		
		return null;
	}

// -------- protected Methoden --------

	/*
	 *	Filtert die unerlaubten Zeichen eines Presetnamens
	 *
	 *	@return gefilterter Name, einige Zeichen werden ersetzt durch Unicode-Symbole
	 */
	protected static String stringToKey( String str )
	{
		char	c;
		String	unicode;
	
		for( int i = 0; i < str.length(); i++ ) {
			c = str.charAt( i );
			if( ("#!=:".indexOf( c ) >= 0) ||
				Character.isWhitespace( c ) ) {		// verbotenes Zeichen

				unicode = Integer.toString( (int) c );
				unicode = "\\u0000".substring( 0, 6 - unicode.length() ) + unicode;		// Ersatzstring
				
				str = str.substring( 0, i ) + unicode + str.substring( i + 1 );		// ...einfuegen
			}
		}
		return str;
	}

	/*
	 *	Konvertiert einen Presetnamen in einen String,
	 *	evtl. gefiltertete Zeichen werden wieder zurueckkonvertiert
	 */
	protected static String keyToString( String key )
	{
		int		i;
		char	c;
	
		try {
			while( (i = key.indexOf( "\\u" )) >= 0 ) {
		
				c = (char) Integer.parseInt( key.substring( i + 2, i + 6 ));		
				key = key.substring( 0, i ) + c + key.substring( i + 6 );
			}
		}
		catch( IndexOutOfBoundsException e1 ) {}		// nothing we can do unfortunately
		catch( NumberFormatException e2 ) {}
		
		return key;
	}
	
	protected static Properties createDefaults( Properties defaultVal )
	{
		Properties	defaults	= new Properties();
		String		strVal		= propertiesToValue( defaultVal );
		
		if( strVal != null ) {
			defaults.put( DEFAULT, strVal );
		}
		return defaults;
	}
}
// class Presets
