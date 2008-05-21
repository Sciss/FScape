/*
 *  PropertyGUI.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;

import de.sciss.fscape.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.util.*;

/**
 *	Another idea to simplify GUI creation which kind
 *	of misses the point of getting the GUI description
 *	out of the source code.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.64, 06-Dec-04
 */
public class PropertyGUI
extends GUISupport
implements ItemListener
{
// -------- public Variablen --------

	// toplevel codes; separator ';' oder '\n' für neue Zeile
	public static final int	objGroupLabel	= 'g' << 8 + 'l';	// <Name>
	public static final int	objLabel		= 'l' << 8 + 'b';	// <Name>
	public static final int	objJCheckBox		= 'c' << 8 + 'b';	// <Name>
	public static final int	objJComboBox		= 'c' << 8 + 'h';
	public static final int	objParamField	= 'p' << 8 + 'f';	// [<spaceID>[|<spaceID>...]]
//	public static final int	objFontField	= (int) 'f' << 8 + (int) 'f';	// <Type>
	public static final int	objPathField	= 'i' << 8 + 'o';	// <Type>|<Requester-Txt>
//	public static final int	objColorJComboBox	= (int) 'c' << 8 + (int) 'c';	// <Flags>[|<Image-Filename>]
	public static final int	objEditEnv		= 'e' << 8 + 'n';
	public static final int	objSkip			= 's' << 8 + 'k';	// Platzhalter

	// generic; separator ','
	public static final	int	attrCols		= 'w' << 8 + 'i';	// <Spaltenbreite>
	public static final	int	attrID			= 'i' << 8 + 'd';	// <Array-Index>
	public static final	int	attrPropName	= 'p' << 8 + 'r';	// <Properties-Key>
	public static final	int	attrAction		= 'a' << 8 + 'c';	// <condition>|<target obj ID>|<Command>[|<target 2>|<cmd 2>...]

	/**
	 * generic action commands
	 *   mehrere Befehle einfach durch mehrere <ac> Befehle!
	 */
	public static final	int	actionEnable	= 'e' << 8 + 'n';
	public static final	int	actionDisable	= 'd' << 8 + 'i';

	/**
	 * choice
	 * action-conditions: <itemID> ==> Action wird ausgefuehrt,
	 *						wenn diese Item selectiert wurde
	 *		z.B. <ac1|5|en> ==> wenn Item#1 selektiert wurde,
	 *							soll die Componente mit ID5 enabled werden
	 */
	public static final	int	chItem			= 'i' << 8 + 't';	// JComboBox-Item <Name>
	public static final int chSelect		= 's' << 8 + 'e';	// <ItemID>

	// paramfield
	public static final	int	pfSpaces		= 's' << 8 + 'p';	// <ParamUnit>[|<ParamUnit>...]
	public static final	int	pfReference		= 'r' << 8 + 'e';	// <val>|<unit> oder <obj ID>

	// label
	public static final int lbText			= 't' << 8 + 'x';	// <new name>

	// checkbox
	public static final int cbState			= 's' << 8 + 't';	// <true> bzw. <false>

	public static final int MAX_ID			= 0xFFFF;	// maximale User-Object-ID

// -------- private Variablen --------

	protected PropertyArray	pr;
	protected Hashtable		hID;		// key = ID, value = PropertyComponent
	protected Hashtable		hObj;		// key = Component
	protected Hashtable		hPropName;	// key = Properties-Keyname

// -------- public Methoden --------
	// public void fillPropertyArray( PropertyArray pr );
	// public void fillGUI( PropertyArray pr );

/* LANGFRISTIG

	GUISupport entsprechend anpassen und einiges von hier rausschmeissen
	und dort implementieren: die komplexeren Hashtables + PropComp-Class!

*/

	/**
	 *	@param	descr	Beschreibung der Oberflaeche. Zeilen werden mit '\n'
	 *					abgeschlossen, Objekte mit ';' getrennt. Jedes Objekt
	 *					beginnt mit einem 2-Buchstaben-Namen (siehe Konstanten
	 *					'obj...').
	 *
	 *					Es folgt der Name und alle Attribute durch Kommata
	 *					getrennt. Attribut: 2-Buchstaben-ID gefolgt von Auspraegung,
	 *					z.B. 'wi2' = Breite zwei Spalten.
	 */
	public PropertyGUI( String descr )
	{
		super( -1 );

//		this.pr = pr;
		
		final Component parent = ((AbstractWindow) AbstractApplication.getApplication().getComponent( Main.COMP_MAIN )).getWindow();
		
		hObj			= new Hashtable();
		hID				= new Hashtable();
		hPropName		= new Hashtable();

		StringTokenizer linTok;		// Zeilen
		StringTokenizer objTok;		// Objekte ("Components")
		StringTokenizer attrTok;	// Parameter eines Objekts
		StringTokenizer microTok;	// unterste Ebene, "|" Separator
		
		String			token;
		String			objName;
		String			attrName;

		String			objPropName;	// null, wenn kein zu registrierendes Property
		String			objActionCmd;	// ggf. null; mehrere Cmd durch Kommata getrennt

		int				objCode;		// type
		int				attrCode;
		int				objID;			// fuer Hash; -1 wenn es nicht registriert werden muss
		int				autoID;			// automatisch registrieren wenn Listener installiert wird
		int				toLineEnd;
		ParamSpace[]	spaces;			// für ParamField

		Component			cmp	= null;
		PropertyComponent	prCmp;
	
//		setBackground( Color.white );
		con.anchor		= GridBagConstraints.WEST;
		con.insets		= new Insets( 0, 8, 0, 0 );
		con.fill		= GridBagConstraints.BOTH;

		autoID			= MAX_ID + 1;

		linTok = new StringTokenizer( descr, "\n" );
		while( linTok.hasMoreTokens() ) {
			
			// Neue Gadget-Zeile
			objTok		= new StringTokenizer( linTok.nextToken(), ";" );
			toLineEnd	= objTok.countTokens();
			for( int col = 0; objTok.hasMoreTokens(); toLineEnd--, col++ ) {

				objPropName		= null;
				objActionCmd	= null;
				objID			= -1;

				if( toLineEnd == 1 ) {
					con.weightx		= 0.9;
				} else {
					con.gridwidth	= 1;
					con.weightx		= 0.0;
				}

				attrTok		= new StringTokenizer( objTok.nextToken(), "," );
				token		= attrTok.nextToken();
				objCode		= token.charAt( 0 ) << 8 + token.charAt( 1 );
				objName		= token.substring( 2 );

				// Componente erzeugen
				switch( objCode ) {
				case objParamField:
					if( objName.length() == 0 ) {
						cmp = new ParamField();
					} else {
						microTok	= new StringTokenizer( objName, "|" );
						spaces		= new ParamSpace[ microTok.countTokens() ];
						for( int i = 0; i < spaces.length; i++ ) {
							spaces[ i ] = Constants.spaces[ Integer.parseInt( microTok.nextToken() )];
						}
						cmp = new ParamField( spaces );
					}
					con.weightx		= 0.5;
					con.gridwidth	= 3;
					break;

				case objEditEnv:
					cmp = new EnvIcon( parent );
					con.weightx	= 0.0;
					break;

				case objJCheckBox:
					cmp = new JCheckBox( objName );
					break;

				case objJComboBox:
					cmp = new JComboBox();
					break;

				case objLabel:
					cmp = new JLabel( objName, col == 0 ? JLabel.RIGHT : JLabel.LEFT );
					break;

				case objGroupLabel:
					cmp = new GroupLabel( objName, GroupLabel.ORIENT_HORIZONTAL,
										  GroupLabel.BRACE_NONE );
					break;

				case objPathField:
					microTok = new StringTokenizer( objName, "|" );
					cmp = new PathField( Integer.parseInt( microTok.nextToken()),
										 microTok.nextToken() );
					con.weightx		= 0.5;
					con.gridwidth	= 2;
					break;

//				case objFontField:
//					cmp = new FontField( Integer.parseInt( objName ));
//					con.weightx		= 0.5;
//					con.gridwidth	= 2;
//					break;

//				case objColorJComboBox:
//					microTok = new StringTokenizer( objName, "|" );
//					if( microTok.countTokens() > 1 ) {	// Flags + Image file
//						cmp = new ColorChoice( Integer.parseInt( microTok.nextToken() ),
//											   microTok.nextToken() );
//					} else {	// just Flags
//						cmp = new ColorChoice( Integer.parseInt( objName ));
//					}
//					break;

				case objSkip:
					cmp = new JLabel();
					break;
					
				default:	// ERROR
					System.out.println( "PropertyGUI: Unknown Objectcode " + objCode );
					return;
				}
				
				// Attribute übersetzen
				while( attrTok.hasMoreTokens() ) {
					token		= attrTok.nextToken();
					attrCode	= token.charAt( 0 ) << 8 + token.charAt( 1 );
					attrName	= token.substring( 2 );

					// generic
					switch( attrCode ) {
					case attrID:
						objID = Integer.parseInt( attrName );
						break;

					case attrPropName:
						objPropName = attrName;
						if( objID == -1 ) {
							objID = autoID;		// wichtig, damit die fill-Routine uns findet
						}
						break;
					
					case attrAction:
						if( objActionCmd == null ) {
							objActionCmd = attrName;
						} else {
							objActionCmd += ',' + attrName;
						}
						if( objID == -1 ) {
							objID = autoID;		// wichtig, damit der Listener uns findet
						}
						break;
					
					case attrCols:
						con.gridwidth = Integer.parseInt( attrName );
						break;

					default:
						// Component-spezifisch
						switch( objCode ) {
						case objParamField:
						
							switch( attrCode ) {
							case pfReference:
								int i = attrName.indexOf( '|' );
								if( i > 0 ) {	// Param-Reference
									((ParamField) cmp).setReference( new Param( Double.valueOf(
										attrName.substring( 0, i )).doubleValue(),
										Integer.parseInt( attrName.substring( i + 1 ))));
								} else {		// ParamField-Reference
									PropertyComponent targetPrCmp = (PropertyComponent) hID.get(
										Integer.valueOf( attrName ));
									if( targetPrCmp != null ) {
										((ParamField) cmp).setReference( (ParamField) targetPrCmp.cmp );
									}
								}
								break;
							
							default:
								break;
							}
							break;

						case objJComboBox:
						
							switch( attrCode ) {
							case chItem:
								((JComboBox) cmp).addItem( attrName );
								break;
								
							default:
								break;
							}
													
						default:
							break;
						}
						break;	// Component-spezifisch Ende
					}
				} // attrTok

				if( toLineEnd == 1 ) {
					con.gridwidth	= GridBagConstraints.REMAINDER;
				}

				// register Component in da hash
				if( objID != -1 ) {
					prCmp			= new PropertyComponent();
					prCmp.cmp		= cmp;
					prCmp.ID		= objID;
					prCmp.type		= objCode;
					prCmp.actionCmd	= objActionCmd;

					hID.put( new Integer( objID ), prCmp );
					hObj.put( cmp, prCmp );
					
					if( objPropName != null ) {
						hPropName.put( objPropName, prCmp );
					}
					
					// Listener installieren
					if( objActionCmd != null ) {
						switch( objCode ) {
						case objJCheckBox:
							((JCheckBox) cmp).addItemListener( this );
							break;

						case objJComboBox:
							((JComboBox) cmp).addItemListener( this );
							break;

						default:
							break;
						}
					}
				}

				// add Component to this panel
				lay.setConstraints( cmp, con );
				add( cmp );

				autoID++;

			} // objTok

			con.fill = GridBagConstraints.HORIZONTAL;
		}
	}

	/**
	 *	Das PropertyArray wird mit Werten aus den Gadgets gefuellt;
	 *	NOTE:	Nur mit PropertyName (boolName etc.) versehene Werte
	 *			werden beruecksichtigt
	 */
	public void fillPropertyArray( PropertyArray pr )
	{
		fillPropertyArray( pr, pr.boolName );
		fillPropertyArray( pr, pr.intgName );
		fillPropertyArray( pr, pr.textName );
		fillPropertyArray( pr, pr.paraName );
		fillPropertyArray( pr, pr.envlName );
//		fillPropertyArray( pr, pr.fontName );
//		fillPropertyArray( pr, pr.colrName );
	}

	/**
	 *	Die Gadgets mit Werten aus einem PropertyArray fuellen;
	 *	ggf. werden ActionCommands ausgefuehrt.
	 *	NOTE:	Nur mit PropertyName (boolName etc.) versehene Werte
	 *			werden beruecksichtigt
	 */
	public void fillGUI( PropertyArray pr )
	{
		fillGUI( pr, pr.boolName );
		fillGUI( pr, pr.intgName );
		fillGUI( pr, pr.textName );
		fillGUI( pr, pr.paraName );
		fillGUI( pr, pr.envlName );
//		fillGUI( pr, pr.fontName );
//		fillGUI( pr, pr.colrName );
	}
	
// -------- Item Methoden (JCheckBox + JComboBox) --------

	public void itemStateChanged( ItemEvent e )
	{
		PropertyComponent	prCmp;
		
		prCmp	= (PropertyComponent) hObj.get( e.getSource() );
		if( prCmp != null ) {
			processActionCommand( prCmp );
		}
	}

// -------- protected Methoden --------

	/*
	 *	Fuellt die PropertyArray-Variablen mit den
	 *	Werten der korrespondierenden Gadgets
	 *
	 *	@param	prNames	zu bearbeitender Feldtyp, z.B. pr.boolName
	 */
	protected void fillPropertyArray( PropertyArray pr, String prNames[] )
	{
		PropertyComponent	prCmp;
		Component			cmp;

		for( int i = 0; i < prNames.length; i++ ) {

			if( prNames[ i ] != null ) {
				prCmp = (PropertyComponent) hPropName.get( prNames[ i ]);

				if( prCmp != null ) {
					cmp = prCmp.cmp;
				
					switch( prCmp.type ) {
					case objParamField:
						pr.para[ i ] = ((ParamField) cmp).getParam();
						break;

					case objEditEnv:
						pr.envl[ i ] = ((EnvIcon) cmp).getEnv();
						break;

					case objJCheckBox:
						pr.bool[ i ] = ((JCheckBox) cmp).isSelected();
						break;

					case objJComboBox:
						pr.intg[ i ] = ((JComboBox) cmp).getSelectedIndex();
						break;

					case objPathField:
						pr.text[ i ] = ((PathField) cmp).getPath().getPath();
						break;

//					case objFontField:
//						pr.font[ i ] = ((FontField) cmp).getFontSC();
//						break;

//					case objColorJComboBox:
//						pr.colr[ i ] = ((ColorChoice) cmp).getColor();
//						break;

					default:
						return;
					}
				}
			}
		}
	}

	/*
	 *	Fuellt die Gadgets mit den Werten der
	 *	korrespondierenden PropertyArray-Variablen;
	 *	ggf. werden ActionCommands ausgefuehrt
	 *
	 *	@param	prNames	zu bearbeitender Feldtyp, z.B. pr.boolName
	 */
	protected void fillGUI( PropertyArray pr, String prNames[] )
	{
		PropertyComponent	prCmp;
		Component			cmp;

		for( int i = 0; i < prNames.length; i++ ) {

			if( prNames[ i ] != null ) {
				prCmp = (PropertyComponent) hPropName.get( prNames[ i ]);

				if( prCmp != null ) {
					cmp = prCmp.cmp;
				
					switch( prCmp.type ) {
					case objParamField:
						((ParamField) cmp).setParam( pr.para[ i ]);
						break;

					case objEditEnv:
						((EnvIcon) cmp).setEnv( pr.envl[ i ]);
						break;

					case objJCheckBox:
						((JCheckBox) cmp).setSelected( pr.bool[ i ]);
						break;

					case objJComboBox:
						((JComboBox) cmp).setSelectedIndex( pr.intg[ i ]);
						break;

					case objPathField:
						((PathField) cmp).setPath( new File( pr.text[ i ]));
						break;

//					case objFontField:
//						((FontField) cmp).setFont( pr.font[ i ]);
//						break;

//					case objColorJComboBox:
//						((ColorChoice) cmp).setColor( pr.colr[ i ]);
//						break;

					default:
						return;
					}
					
					if( prCmp.actionCmd != null ) {
						processActionCommand( prCmp );
					}
				}
			}
		}
	}

	/**
	 *	Fuehrt die Befehle einer PropertyComponente aus
	 */
	protected void processActionCommand( PropertyComponent srcPrCmp )
	{
		StringTokenizer		actionTok;
		StringTokenizer		cmdTok;
		String				condition;
		String				target;
		String				cmd;
		int					cmdCode;
		PropertyComponent	targetPrCmp;
		boolean				conditionFulfilled	= false;

		actionTok = new StringTokenizer( srcPrCmp.actionCmd, "," );
		while( actionTok.hasMoreTokens() ) {
			
			cmdTok		= new StringTokenizer( actionTok.nextToken(), "|" );
			condition	= cmdTok.nextToken();
			while( cmdTok.hasMoreTokens() ) {
			
				target	= cmdTok.nextToken();
				cmd		= cmdTok.nextToken();
				targetPrCmp = (PropertyComponent) hID.get( Integer.valueOf( target ));
				if( targetPrCmp != null ) {
				
					switch( srcPrCmp.type ) {
					case objJCheckBox:
						conditionFulfilled = ((JCheckBox) srcPrCmp.cmp).isSelected() ==
											 Boolean.valueOf( condition ).booleanValue();
						break;

					case objJComboBox:
						conditionFulfilled = ((JComboBox) srcPrCmp.cmp).getSelectedIndex() ==
											 Integer.parseInt( condition );
						break;
					
					default:
						conditionFulfilled = false;
						break;
					}
					
					if( conditionFulfilled ) {		// Bedingung erfüllt, Cmd ausführen
					
						cmdCode	= cmd.charAt( 0 ) << 8 + cmd.charAt( 1 );
						// generic
						switch( cmdCode ) {
						case actionEnable:
							targetPrCmp.cmp.setEnabled( true );
							break;
							
						case actionDisable:
							targetPrCmp.cmp.setEnabled( false );
							break;
						
						default:
							// Component-type specific
							// XXX NOT YET
							switch( targetPrCmp.type ) {
							default:
								break;
							}
						}
					}
				}
			} // alle Befehle zu einer Condition
		} // nächste Befehlskette
	}

// -------- Interne Klasse zur Verwaltung der Components --------

	class PropertyComponent
	{
		Component	cmp;
		int			ID;
		int			type;		// e.g. objJCheckBox
		String		actionCmd;	// <condition>|<targetID>|<cmd>[|<targetID2>|<cmd2>...]
								// [,<condition>|<targetID>...]
	}
}
