/*
 *  Presets.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.prop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 *	Abgeleitete Properties-Klasse zur Verwaltung von Presets
 *	Einem Key-Namen stet als Value eine Properties-Liste gegenueber, die automatisch
 *	in einen "String" umgewandelt wird (durch Ersetzen der Linefeeds)
 */
public class Presets
        extends BasicProperties {

// -------- public variables --------

    /**
     *	Name fuer den Default-Preset
     */
    public static final String DEFAULT = "(default)";

    private static final Comparator<String> caseInsensitiveComp = new Comparator<String>() {
        public int compare( String a, String b ) {
            return a.toUpperCase().compareTo(b.toUpperCase() );
        }
    };

// -------- public methods --------

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

    public boolean containsPreset( String name )
    {
        synchronized( this ) {
            return this.containsKey( stringToKey( name ));
        }
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
     *	Return List of all Preset-Names (sorted)
     */
    public List presetNames()
    {
        synchronized( this ) {
            final List<String> li = new ArrayList<String>( this.size() );
            final Enumeration e = propertyNames();
            while( e.hasMoreElements() ) {
                li.add( keyToString( (String) e.nextElement() ));
            }
            Collections.sort( li, caseInsensitiveComp );
            return li;
        }
    }

    /**
     *	Schreibt die uebergebenen Properties in einen ByteStream und
     *	liefert diesen als String zurueck
     *
     *	@return	null bei Fehler
     */
    public static String propertiesToValue( Properties val )
    {
        final OutputStream	outStream	= new ByteArrayOutputStream();

        try {
            val.store( outStream, null );
            final String strVal = outStream.toString();		// now we have the Properties as a String
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
     *	@param	strVal	null erlaubt, ergibt aber auch null als Ergebnis!
     *	@return	null bei Fehler
     */
    public static Properties valueToProperties(String strVal) {
        final Properties val = new Properties();

        try {
            if (strVal != null) {
                final InputStream inStream = new ByteArrayInputStream(strVal.getBytes());
                val.load(inStream);    // now we have the String as a Properites-Object
                inStream.close();
                return val;
            }
        } catch (IOException e) { /* nothing */ }

        return null;
    }

// -------- protected methods --------

    /*
     *	Filtert die unerlaubten Zeichen eines Presetnamens
     *
     *	@return gefilterter Name, einige Zeichen werden ersetzt durch Unicode-Symbole
     */
    protected static String stringToKey( String str )
    {
        String	unicode;

        for( int i = 0; i < str.length(); i++ ) {
            final char c = str.charAt( i );
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
        try {
            int i;
            while( (i = key.indexOf( "\\u" )) >= 0 ) {
                final char c = (char) Integer.parseInt( key.substring( i + 2, i + 6 ));
                key = key.substring( 0, i ) + c + key.substring( i + 6 );
            }
        }
        catch( IndexOutOfBoundsException ignored) {}		// nothing we can do unfortunately
        catch( NumberFormatException ignored) {}

        return key;
    }

    protected static Properties createDefaults( Properties defaultVal )
    {
        final Properties	defaults	= new Properties();
        final String		strVal		= propertiesToValue( defaultVal );

        if( strVal != null ) {
            defaults.put( DEFAULT, strVal );
        }
        return defaults;
    }
}
