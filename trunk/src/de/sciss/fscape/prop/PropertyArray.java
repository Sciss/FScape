/*
 *  PropertyArray.java
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

import java.util.*;

import de.sciss.fscape.util.*;

/**
 *	All fields must be initialized by the object
 *	that creates an instance of this class,
 *	it must ensure that fields in use are
 *	valid arrays of the valid size!
 *
 *  @version	0.71, 14-Nov-07
 */
public class PropertyArray
implements Cloneable
{
// -------- public Variablen --------

	public PropertyArray superPr = null;	// allows hierarchical properties

	// primitives
	public boolean	bool[];			// JCheckBoxes
	public int		intg[];			// JComboBoxs
	public String	text[];			// JTextField, PathField

	// enhanced
	public Param	para[];			// ParamField
	public Envelope	envl[];
//	public FontSC	font[];			// FontField
//	public ColorSC	colr[];			// ColorJComboBox

	// corresponding Properties-Key-Names
	public String	boolName[];
	public String	intgName[];
	public String	textName[];
	public String	paraName[];
	public String	envlName[];
//	public String	fontName[];
//	public String	colrName[];

// -------- public Methoden --------
	// public Properties toProperties( boolean invokeSuper, Properties dest );
	// public Properties toProperties( boolean invokeSuper );
	// public void fromProperties( boolean invokeSuper, Properties src );

	/**
	 *	@param	src	PropertyArray, dessen Inhalte uebertragen werden sollen
	 *				nicht uebertragen wird das Feld superPr!
	 */
	public PropertyArray( PropertyArray src )
	{
		copyFrom( src );
	}
	
	public PropertyArray()
	{
		bool		= new boolean[	0 ];
		intg		= new int[		0 ];
		text		= new String[	0 ];
		para		= new Param[	0 ];
		envl		= new Envelope[ 0 ];
//		font		= new FontSC[	0 ];
//		colr		= new ColorSC[	0 ];

		boolName	= new String[ 	0 ];
		intgName	= new String[ 	0 ];
		textName	= new String[	0 ];
		paraName	= new String[	0 ];
		envlName	= new String[	0 ];
//		fontName	= new String[	0 ];
//		colrName	= new String[	0 ];
	}
	
	/**
	 *	super-PropertyArrays werden ggf. auch geclont!
	 */
	public Object clone()
	{
		PropertyArray cloned = new PropertyArray( this );
		if( this.superPr != null ) {
			cloned.superPr = new PropertyArray( this.superPr );
		}
		return cloned;
	}
	
	public void copyFrom( PropertyArray src )
	{
		// da kommt freude auf.
		this.bool = (boolean[]) src.bool.clone();
		this.intg = (int[])		src.intg.clone();
		this.text = (String[])	src.text.clone();
//		this.font = (FontSC[])	src.font.clone();
//		this.colr = (ColorSC[])	src.colr.clone();

		// Param is a non-constant object!
		this.para = new Param[ src.para.length ];
		for( int i = 0; i < src.para.length; i++ ) {
			this.para[ i ] = (Param) src.para[ i ].clone();
		}
		// Envelope is a non-constant object!
		this.envl = new Envelope[ src.envl.length ];
		for( int i = 0; i < src.envl.length; i++ ) {
			this.envl[ i ] = (Envelope) src.envl[ i ].clone();
		}

		this.boolName = (String[]) src.boolName.clone();
		this.intgName = (String[]) src.intgName.clone();
		this.textName = (String[]) src.textName.clone();
		this.paraName = (String[]) src.paraName.clone();
		this.envlName = (String[]) src.envlName.clone();
//		this.fontName = (String[]) src.fontName.clone();
//		this.colrName = (String[]) src.colrName.clone();
	}
	
	/**
	 *	Konvertiert die Inhalte des PAs in ein Properties-Objekt
	 *	; die Keynames sind in boolName, paraName etc. gespeichert;
	 *	; der Objektwert wird mittels toString() in einen String verwandelt
	 *
	 *	@param	invokeSuper	true, wenn auch Super-PAs in das Properties-Objekt
	 *						geschrieben werden sollen
	 *	@param	dest		(moeglicherweise bereits beschriebenes) Ziel-Properties
	 */
	public Properties toProperties( boolean invokeSuper, Properties dest )
	{
		if( invokeSuper && (superPr != null) ) {
			superPr.toProperties( true, dest );
		}

		PropertyArray.toProperties( bool, boolName, dest );
		PropertyArray.toProperties( intg, intgName, dest );
		PropertyArray.toProperties( text, textName, dest );
		PropertyArray.toProperties( para, paraName, dest );
		PropertyArray.toProperties( envl, envlName, dest );
//		PropertyArray.toProperties( font, fontName, dest );
//		PropertyArray.toProperties( colr, colrName, dest );

		return dest;
	}

	/**
	 *	Wie die vorherige Methode; nur wird hier ein neues Properties-Objekt
	 *	erzeugt
	 *
	 *	@return	neu erzeugtes und gefuelltes Properties-Objekt
	 */
	public Properties toProperties( boolean invokeSuper )
	{
		return toProperties( invokeSuper, new Properties() );
	}
	
	/**
	 *	Konvertiert die Inhalte eines Properties-Objekts in das PA.
	 *	; die Keynames sind in boolName, paraName etc. gespeichert;
	 *	; der Objektwert wird mittels valueOf() aus dem Value-String gewonnen
	 *
	 *	@param	invokeSuper	true, wenn auch Super-PAs mittels des Properties-Objekt
	 *						beschrieben werden sollen
	 *	@param	src			Quell-Properties
	 */
	public void fromProperties( boolean invokeSuper, Properties src )
	{
		if( invokeSuper && (superPr != null) ) {
			superPr.fromProperties( true, src );
		}

		PropertyArray.fromProperties( bool, boolName, src );
		PropertyArray.fromProperties( intg, intgName, src );
		PropertyArray.fromProperties( text, textName, src );
		PropertyArray.fromProperties( para, paraName, src );
		PropertyArray.fromProperties( envl, envlName, src );
//		PropertyArray.fromProperties( font, fontName, src );
//		PropertyArray.fromProperties( colr, colrName, src );
	}

// -------- protected Methoden --------

	protected static Properties toProperties( Object[] val, String[] name, Properties dest )
	{	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				dest.put( name[ i ], String.valueOf( val[ i ]));
			}
		}
		return dest;
	}

	protected static Properties toProperties( boolean[] val, String[] name, Properties dest )
	{	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				dest.put( name[ i ], String.valueOf( val[ i ]));
			}
		}
		
		return dest;
	}

	protected static Properties toProperties( int[] val, String[] name, Properties dest )
	{	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				dest.put( name[ i ], String.valueOf( val[ i ]));
			}
		}
		
		return dest;
	}

	protected static void fromProperties( boolean[] val, String[] name, Properties src )
	{
		String prop;
	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				prop = src.getProperty( name[ i ]);
				if( prop != null ) {
					val[ i ] = Boolean.valueOf( prop ).booleanValue();
				}
			}
		}
	}

	protected static void fromProperties( int[] val, String[] name, Properties src )
	{	
		String prop;
	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				prop = src.getProperty( name[ i ]);
				if( prop != null ) {
					val[ i ] = Integer.parseInt( prop );
				}
			}
		}
	}

	protected static void fromProperties( String[] val, String[] name, Properties src )
	{	
		String prop;
	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				prop = src.getProperty( name[ i ]);
				if( prop != null ) {
					val[ i ] = prop;
				}
			}
		}
	}

//	protected static void fromProperties( FontSC[] val, String[] name, Properties src )
//	{	
//		String prop;
//	
//		for( int i = 0; i < name.length; i++ ) {
//			if( name[ i ] != null ) {
//				prop = src.getProperty( name[ i ]);
//				if( prop != null ) {
//					val[ i ] = FontSC.valueOf( prop );
//				}
//			}
//		}
//	}

//	protected static void fromProperties( ColorSC[] val, String[] name, Properties src )
//	{	
//		String prop;
//	
//		for( int i = 0; i < name.length; i++ ) {
//			if( name[ i ] != null ) {
//				prop = src.getProperty( name[ i ]);
//				if( prop != null ) {
//					val[ i ] = ColorSC.valueOf( prop );
//				}
//			}
//		}
//	}

	protected static void fromProperties( Param[] val, String[] name, Properties src )
	{	
		String prop;
	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				prop = src.getProperty( name[ i ]);
				if( prop != null ) {
					val[ i ] = Param.valueOf( prop );
				}
			}
		}
	}

	protected static void fromProperties( Envelope[] val, String[] name, Properties src )
	{	
		String prop;
	
		for( int i = 0; i < name.length; i++ ) {
			if( name[ i ] != null ) {
				prop = src.getProperty( name[ i ]);
				if( prop != null ) {
					val[ i ] = Envelope.valueOf( prop );
				}
			}
		}
	}
}
// class PropertyArray
