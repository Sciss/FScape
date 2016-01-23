/*
 *  Prefs.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.prop;

// import com.apple.mrj.*;
import java.awt.*;
import java.util.*;

/**
 *	Abstracte Klasse fuer Preferences
 *
 *  @version	0.71, 14-Nov-07
 */
public abstract class Prefs
extends BasicProperties
{
// -------- private Klassenvariablen --------
// -------- public Variablen --------
// -------- public Methoden --------

	/**
	 *	Legt das Preferences Objekt an mit entsprechenden Default-Eintraegen
	 *
	 *	@param owner	Inhaber der Preferences (daraus wird der Name abgeleitet)
	 *	@param defPrefs	Preferences-Voreinstellungen
	 */
	public Prefs( Class owner, Properties defPrefs )
	{
		super( owner, defPrefs, "prf" );
	}

	/**
	 *	Fuellt eine Property mit den uebergebenen Punkt-Koordinaten
	 */
	public Object setPointProperty( String key, Point pt )
	{
		synchronized( this ) {
			modified = true;
			return BasicProperties.setPointProperty( this, key, pt );
		}
	}

	/**
	 *	Fuellt eine Property mit den uebergebenen Dimensionen
	 */
	public Object setDimensionProperty( String key, Dimension dim )
	{
		synchronized( this ) {
			modified = true;
			return BasicProperties.setDimensionProperty( this, key, dim );
		}
	}

	/**
	 *	Fuellt eine Property mit den Koordinaten eines Rechtecks
	 */
	public Object setRectangleProperty( String key, Rectangle rect )
	{
		synchronized( this ) {
			modified = true;
			return BasicProperties.setRectangleProperty( this, key, rect );
		}
	}

	/**
	 *	Besorgt ein Property der Form &quot;x,y&quot; als Point Objekt
	 *
	 *	@return entsprechenden Punkt oder null bei Fehler
	 */
	public Point getPointProperty( String key )
	{
		synchronized( this ) {
			return BasicProperties.getPointProperty( this, key );
		}
	}
	
	/**
	 *	Besorgt ein Property der Form &quot;breite,hoehe&quot; als Dimension Objekt
	 *
	 *	@return entsprechende Dimension oder null bei Fehler
	 */
	public Dimension getDimensionProperty( String key )
	{
		synchronized( this ) {
			return BasicProperties.getDimensionProperty( this, key );
		}
	}

	/**
	 *	Besorgt ein Property der Form &quot;x,y,breite,hoehe&quot; als Rectangle Objekt
	 *
	 *	@return entsprechendes Rechteck oder null bei Fehler
	 */
	public Rectangle getRectangleProperty( String key )
	{
		synchronized( this ) {
			return BasicProperties.getRectangleProperty( this, key );
		}
	}
}
// class Prefs
