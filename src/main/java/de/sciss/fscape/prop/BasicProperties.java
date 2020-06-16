/*
 *  BasicProperties.java
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

import de.sciss.fscape.Application;
import de.sciss.fscape.util.PrefsUtil;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *	Abgeleitete Properties-Klasse zur Verwaltung von Presets
 *	Einem Key-Namen stet als Value eine Properties-Liste gegenueber, die automatisch
 *	in einen "String" umgewandelt wird (durch Ersetzen der Linefeeds)
 */
public class BasicProperties
        extends Properties {

// -------- private variables --------

    private File f;
    protected static final String header = "Created by FScape; do not edit manually!";

    // true, wenn Presets seit dem letzten Speichern geaendert
    protected boolean modified = false;

// -------- public methods --------

    /**
     *	Legt das Presets-Objekt an mit entsprechenden Default-Eintraegen
     *
     *	@param	owner		Inhaber der Preferences (daraus wird der Name abgeleitet)
     *	@param	defProp	voreingestellter Preset
     *	@param	typeName	such as "presets" or "prefs"
     */
    public BasicProperties( Class owner, Properties defProp, String typeName )
    {
        // default Properties, der Rest wird von oben gemanagt
        super( defProp );

        String ownName, presetsName;
        int i;

        ownName 	= owner.getName();
        i			= ownName.lastIndexOf( '.' ) + 1;		// find last part of the name
        presetsName = ownName.substring( i );

        try {
//			fileName	= FileManager.findFolder( 0x70726566 ) + File.separator+ "FScape" + File.separator + typeName + File.separator + presetsName + '.' + typeName;
            f			= new File( System.getProperty( "user.home" ) + File.separator+ "FScape" + File.separator + typeName + File.separator + presetsName + '.' + typeName );
            load();
        } catch( IOException e ) {
            // nothing
        }
    }

    /**
     *	Constructs empty Properties object
     */
    public BasicProperties( Properties defProp, File f )
    {
        super( defProp );

        this.f = f;
    }

    public boolean isModified()
    {
        return modified;
    }

    /**
     * Loads FScape document from disk
     */
    public void load()
            throws IOException {
        synchronized (this) {
            final BufferedReader dis = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = "";
            do {
                line = dis.readLine();
                if (!line.startsWith("#")) {
                    final String message;
                    if (AudioFile.retrieveType(f) == AudioFileDescr.TYPE_UNKNOWN) {
                        message = "Unknown file format";
                    } else {
                        message = "This is an audio file.\nYou don't open audio files via the File menu!";
                    }
                    throw new IOException("This file is not an FScape document.\n" + message);
                }
            } while (!line.contains(header));

            load(dis);
            modified = false;
        }
    }

    /**
     *	Sichert die Presets auf der Festplatte
     *
     *	@param force	wenn false, wird nur gespeichert, wenn es Modifikationen gab
     */
    public void store( boolean force )
    throws IOException
    {
        File f2, p;

        synchronized( this ) {
            if( force || modified ) {
                if( f.exists() && Application.userPrefs.getBoolean(PrefsUtil.KEY_BACKUP, false)) {
                    f2 = new File(Application.userPrefs.get(PrefsUtil.KEY_BAKDIR, ""), f.getName() );
                    copyFile( f, f2 );
                }
                try {
                    store( new FileOutputStream( f ), header );
                    modified = false;
                }
                catch( IOException e ) {
                    p = f.getParentFile();
                    p.mkdirs();
                    // retry after folder-creation
                    store( new FileOutputStream( f ), header );
                    modified = false;
                }

//				try {
//					MRJAdapter.setFileType( f.getAbsoluteFile(), "FScP" ); // Constants.OSTypePrefs );
//				} catch( IOException e ) { /* ignored */}		// Filetype nicht wichtig
            }
        }
    }
    public void store()
    throws IOException
    {
        store( false );
    }

    private static void copyFile( File source, File target )
    throws IOException
    {
        if( target.exists() ) target.delete();

        RandomAccessFile	sourceRAF = new RandomAccessFile( source, "r" );
        RandomAccessFile	targetRAF = new RandomAccessFile( target, "rw" );

        byte[] buffer = new byte[ 16768 ];

        int		len;
        long	pos		= 0;
        long	totLen  = sourceRAF.length();

        while( pos < totLen ) {
            len = (int) Math.min( 16768, totLen - pos );
            sourceRAF.readFully( buffer, 0, len );
            targetRAF.write( buffer, 0, len );
            pos += len;
        }

        sourceRAF.close();
        targetRAF.close();
    }
   
    /**
     *	Fuellt eine Property mit dem uebergebenen String
     *
     *	@return vorheriger Wert
     */
    public Object setProperty( String key, String val )
    {
        return put( key, val );
    }

    public Object put( Object key, Object val )
    {
        synchronized( this ) {
            modified = true;	// indicate that prefs should be saved on exit
            return super.put( key, val );
        }
    }

    // don't know why we need this...
    public Object put( String key, String val )
    {
        synchronized( this ) {
            modified = true;	// indicate that prefs should be saved on exit
            return super.put( key, val );
        }
    }

// ---------- statische Utility methoden ----------

    /**
     *	Fuellt eine Property mit den uebergebenen Punkt-Koordinaten
     */
    public static Object setPointProperty( Properties p, String key, Point pt )
    {
        return p.put( key, "" + pt.x + "," + pt.y );
    }

    /**
     *	Fuellt eine Property mit den uebergebenen Dimensionen
     */
    public static Object setDimensionProperty( Properties p, String key, Dimension dim )
    {
        return p.put( key, "" + dim.width + "," + dim.height );
    }

    /**
     *	Fuellt eine Property mit den Koordinaten eines Rechtecks
     */
    public static Object setRectangleProperty( Properties p, String key, Rectangle rect )
    {
        return p.put( key, "" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height );
    }

    /**
     *	Besorgt ein Property der Form &quot;x,y&quot; als Point Objekt
     *
     *	@return entsprechenden Punkt oder null bei Fehler
     */
    public static Point getPointProperty( Properties p, String key )
    {
        int x, y;
        StringTokenizer valTok;
        String val = p.getProperty( key );
        try {
            if( val == null ) throw new NoSuchElementException();
            valTok = new StringTokenizer( val, "," );
            x = Integer.parseInt( valTok.nextToken() );
            y = Integer.parseInt( valTok.nextToken() );
            return new Point( x, y );
        } catch( NoSuchElementException e1 ) {
            return null;
        } catch( NumberFormatException e2 ) {
            return null;
        }
    }

    /**
     *	Besorgt ein Property der Form &quot;breite,hoehe&quot; als Dimension Objekt
     *
     *	@return entsprechende Dimension oder null bei Fehler
     */
    public static Dimension getDimensionProperty( Properties p, String key )
    {
        int width, height;
        StringTokenizer valTok;
        String val = p.getProperty( key );
        try {
            if( val == null ) throw new NoSuchElementException();
            valTok = new StringTokenizer( val, "," );
            width  = Integer.parseInt( valTok.nextToken() );
            height = Integer.parseInt( valTok.nextToken() );
            return new Dimension( width, height );
        } catch( NoSuchElementException e1 ) {
            return null;
        } catch( NumberFormatException e2 ) {
            return null;
        }
    }

    /**
     *	Besorgt ein Property der Form &quot;x,y,breite,hoehe&quot; als Rectangle Objekt
     *
     *	@return entsprechendes Rechteck oder null bei Fehler
     */
    public static Rectangle getRectangleProperty( Properties p, String key )
    {
        int x, y, width, height;
        StringTokenizer valTok;
        String val = p.getProperty( key );
        try {
            if( val == null ) throw new NoSuchElementException();
            valTok	= new StringTokenizer( val, "," );
            x		= Integer.parseInt( valTok.nextToken() );
            y		= Integer.parseInt( valTok.nextToken() );
            width	= Integer.parseInt( valTok.nextToken() );
            height	= Integer.parseInt( valTok.nextToken() );
            return new Rectangle( x, y, width, height );
        } catch( NoSuchElementException e1 ) {
            return null;
        } catch( NumberFormatException e2 ) {
            return null;
        }
    }
}