/*
 *  SpectPatch.java
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
 *		24-Jun-06	renamed from Document to SpectPatch
 */

package de.sciss.fscape.session;

import java.awt.*;
import java.io.*;
import java.rmi.AlreadyBoundException;
import java.util.*;

import de.sciss.common.AppWindow;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.op.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  Contains the data of a spectral patch.
 *	This is historic and should be completely rewritten
 *	or deleted.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class SpectPatch
{
// -------- private Variablen --------

	private SpectPatchDlg		win;
	private String				name		= null;
//	private String				pathName	= null;

//	private boolean				modified	= false;
	public boolean				running		= false;
	public boolean				pausing		= false;

	private Vector				ops;					// Operatoren
	private ThreadGroup			opThreadGroup = null;	// deren Thread-Gruppe

//	private static final String	FILE_HEADER	= AbstractApplication.getApplication().getName() + " document; do not edit manually.";

	private static final String	FILE_OP		= "Op";		// Erkennungs-Strings
	private static final String	FILE_OPHEAD	= "Head";
	private static final String	FILE_OPPROP	= "Prop";

	private static final String LINK_DEST	= "Dest";	// Value = <ID>,<SlotName>
	private static final String LINK_LOC	= "Loc";	// wenn vorhanden ==> con ist sichtbar

	private static final String HEAD_CLASS	= "Class";
	private static final String HEAD_NAME	= "Name";
	private static final String HEAD_LOC	= "Loc";
	private static final String HEAD_ALIAS	= "Alias";	// Value = <FirstAlias>,<SecondAlias> etc.
	private static final String HEAD_ORIGINAL= "Orig";	// Value = <OriginalID>

//	private static final String ERR_ILLEGALFILE	= "Wrong file format";

// -------- public Methoden --------
	// public boolean load( String fname );
	// public boolean saveAs( String fname );
	// public boolean save();
	// public boolean revert();
	// public boolean clear();
	// public boolean close();
	
	// public void start();
	// public void stop();
	// public void pause( boolean state );
	
	// public void addOperator( Operator op );
	// public void removeOperator( Operator op );
	
	// public MainWindow getWindow();
	// public String getName();
	// public String getPathName();
	// public Enumeration getOperators();
	
	// public void setModified( boolean modified );
	// public boolean getModified();
	
	/**
	 *	Leeres SpectPatch erzeugen
	 */
	public SpectPatch( SpectPatchDlg win )
	{
		ops = new Vector();
	
		this.win = win;
//		win.setVisible( true );
	}

	/**
	 *	Dieses SpectPatch ueberladen
	 *
	 *	@return	false bei Fehler
	 */
	public boolean load( Properties file  )
	{
		Operator		op, op2;
		Point			opLoc, conLoc;
		OpPanel			opPanel;
		OpConnector		con;
		String			val;
		StringTokenizer	valTok;
		Properties		head;
		Properties		opProp;
		Properties		link;
		PropertyArray	pa;
		boolean			success = true;

		Vector			tempOps	= new Vector();		// Index dort = ID aus dem file!
		Enumeration		slots;
		SpectStreamSlot	slot1, slot2;
		String			slotName;
		int				k;
		int				op2ID;

		try {
			// Datei geladen, wir koennen das Dokument leeren
			if( !clear()) {
				return false;
			}
			opPanel = win.getOpPanel();		// always do this *after* clear()
//			opPanel.setVisible( false );

			for( int i = 0;
				 (val = file.getProperty( FILE_OP + i + FILE_OPHEAD )) != null;
				 i++ ) {

				head	= Presets.valueToProperties( val );
				val		= file.getProperty( FILE_OP + i + FILE_OPPROP );
				opProp	= Presets.valueToProperties( val );
				val		= head.getProperty( HEAD_CLASS );
				opLoc	= Prefs.getPointProperty( head, HEAD_LOC );
				if( opLoc == null ) {
					opLoc = new Point();
				}

				if( val != null ) {
					val = Operator.PACKAGE + '.' + val;		// voller Klassenname
					try {
						op = (Operator) Class.forName( val ).newInstance();
						tempOps.addElement( op );	// um ihn bei der Linksuche wiederzufinden

						if( opProp != null ) {
							pa = op.getPropertyArray();
							pa.fromProperties( true, opProp );	// Parameter laden
						}

						opPanel.addOperator( op, opLoc.x, opLoc.y );

					// ---- Links ----

						slots = op.getSlots( Slots.SLOTS_FREE ).elements();

						while( slots.hasMoreElements() ) {
							slot1	= (SpectStreamSlot) slots.nextElement();
							val		= file.getProperty( FILE_OP + i + slot1.toString() );
							link	= Presets.valueToProperties( val );

							if( link != null ) {
							
								val	= link.getProperty( LINK_DEST );
								if( (val == null) || (val.length() == 0) ) continue;
								
								try {
									k			= val.indexOf( ',' );
									slotName	= val.substring( k + 1 );
									op2ID		= Integer.parseInt( val.substring( 0, k ));
									op2			= (Operator) tempOps.elementAt( op2ID );
									slot2		= (SpectStreamSlot) op2.getSlot( slotName );
									if( slot2 != null ) {
										opPanel.linkOperators( slot1, slot2 );
										con		= opPanel.getConnector( slot1 );
										conLoc	= Prefs.getPointProperty( link, LINK_LOC );
										
										if( con != null ) {
											if( conLoc != null ) {	// means it is visible!
												con.setVisible( true );
												opPanel.moveConnector( con, conLoc.x, conLoc.y );
											} else {
												con.setVisible( false );
											}
										}
									}
								}
								catch( AlreadyBoundException e ) {
									success = false;		// noch nichts genaueres XXX
								}
								catch( NoSuchElementException e ) {
									success = false;		// noch nichts genaueres XXX
								}
								catch( NumberFormatException e ) {
									// kommt von Integer.parseInt()
									success = false;		// noch nichts genaueres XXX
								}
								catch( ArrayIndexOutOfBoundsException e ) {
									// kommt von tempOps.elementAt()
									// macht nichts; wir sind nur noch nicht bis zu diesem
									// Element vorgedrungen
								}
							}
						}

					// ---- Name ----
						val	= head.getProperty( HEAD_NAME );
						if( val != null ) {
							opPanel.renameOperator( op, val );
						}
				
					// ---- Alia ----
						val = head.getProperty( HEAD_ALIAS );
						if( val != null ) {
							valTok = new StringTokenizer( val, "," );
							while( valTok.hasMoreTokens() ) {
								try {
									op2ID	= Integer.parseInt( valTok.nextToken() );
									op2		= (Operator) tempOps.elementAt( op2ID );
									op2.turnIntoAlias( op );
								}
								catch( AlreadyBoundException e ) {
									success = false;		// noch nichts genaueres XXX
								}
								catch( SyncFailedException e ) {
									success = false;		// noch nichts genaueres XXX
								}
								catch( NumberFormatException e ) {
									// kommt von Integer.parseInt()
									success = false;		// noch nichts genaueres XXX
								}
								catch( ArrayIndexOutOfBoundsException e ) {
									// kommt von tempOps.elementAt()
									// macht nichts; wir sind nur noch nicht bis zu diesem
									// Element vorgedrungen
								}
							}
						}
						
						val = head.getProperty( HEAD_ORIGINAL );
						if( val != null ) {
							try {
								op2ID	= Integer.parseInt( val );
								op2		= (Operator) tempOps.elementAt( op2ID );
								op.turnIntoAlias( op2 );
							}
							catch( AlreadyBoundException e ) {
								success = false;		// noch nichts genaueres XXX
							}
							catch( SyncFailedException e ) {
								success = false;		// noch nichts genaueres XXX
							}
							catch( NumberFormatException e ) {
								// kommt von Integer.parseInt()
								success = false;		// noch nichts genaueres XXX
							}
							catch( ArrayIndexOutOfBoundsException e ) {
								// kommt von tempOps.elementAt()
								// macht nichts; wir sind nur noch nicht bis zu diesem
								// Element vorgedrungen
							}
						}
					} // if( HEAD_CLASS exists )
					
					catch( InstantiationException e1 ) {
						success = false;		// noch nichts genaueres XXX
					}
					catch( IllegalAccessException e2 ) {
						success = false;		// noch nichts genaueres XXX
					}
					catch( ClassNotFoundException e3 ) {
						success = false;		// noch nichts genaueres XXX
					}
					
				} else {
					success = false;	// kein Klassenname gefunden
				}
			}
		}
		catch( IOException e ) {
			success = false;
		}

//		if( success ) {
//			setFileName( pathName, name );
//			setModified( false );
//		}
		
//		opPanel = win.getOpPanel();
//		opPanel.setVisible( true );
		return success;
	}

	/*
	 *	Dieses SpectPatch unter bisherigem Namen speichern
	 *
	 *	@return	false bei Fehler
	 */
	public Properties save()
	{
		Operator		op, op2;
		OpIcon			opIcon;
		OpConnector		con;
		OpPanel			opPanel = win.getOpPanel();
		String			val;
		StringBuffer	valBuf;
		Properties		file	= new Properties();
		Properties		head	= new Properties();
		Properties		link	= new Properties();
		PropertyArray	pa;

		Enumeration		slots, alia;
		SpectStreamSlot	slot1, slot2;
		int				op2ID;

		synchronized( ops ) {
			for( int i = 0; i < ops.size(); i++ ) {
				op		= (Operator) ops.elementAt( i );
				opIcon	= (OpIcon) op.getIcon();

				head.clear();
				link.clear();

			// ---- Head ----
				val		= op.getClass().getName();
				val		= val.substring( val.lastIndexOf( '.' ) + 1 );
				head.put( HEAD_CLASS, val );									// Operator-Klasse
				head.put( HEAD_NAME, opIcon.getName() );						// Icon-Name
				Prefs.setPointProperty( head, HEAD_LOC, opIcon.getLocation() );	// Icon-Position

				op2		= op.getOriginal();
				if( op2 != null ) {
					op2ID	= ops.indexOf( op2 );
					if( op2ID >= 0 ) {
						head.put( HEAD_ORIGINAL, String.valueOf( op2ID ));
					} else {
						// nothing yet XXX
					}
				}
				alia	= op.getAliases();
				if( alia.hasMoreElements() ) {
					valBuf	= new StringBuffer();
					while( alia.hasMoreElements() ) {
						op2		= (Operator) alia.nextElement();
						op2ID	= ops.indexOf( op2 );
						if( op2ID >= 0 ) {
							valBuf.append( op2ID );
							if( alia.hasMoreElements() ) valBuf.append( ',' );
						} else {
							// nothing yet XXX
						}
					}
					head.put( HEAD_ALIAS, valBuf.toString() );
				}

				val		= Presets.propertiesToValue( head );					// in String verwandeln
				file.put( FILE_OP + i + FILE_OPHEAD, val );						// ...und schreiben

			// ---- Properties ----
				pa		= op.getPropertyArray();
				val		= Presets.propertiesToValue( pa.toProperties( true ));	// Op-Einstellungen				
				file.put( FILE_OP + i + FILE_OPPROP, val );

			// ---- Links ----
				slots = op.getSlots( Slots.SLOTS_LINKED ).elements();
				while( slots.hasMoreElements() ) {
					slot1		= (SpectStreamSlot) slots.nextElement();
					slot2		= slot1.getLinked();
					if( slot2 != null ) {
						op2		= slot2.getOwner();
						op2ID	= ops.indexOf( op2 );
						if( op2ID >= 0 ) {
							// Aufbau: <destOpID>,<destSlot>
							link.put( LINK_DEST, "" + op2ID + ',' + slot2.toString() );
							con	= opPanel.getConnector( slot1 );
							if( (con != null) && con.isVisible() ) {
								Prefs.setPointProperty( link, LINK_LOC, con.getLocation() );
							}
						} else {
							// nothing yet XXX
						}
					}
					val	= Presets.propertiesToValue( link );
					file.put( FILE_OP + i + slot1.toString(), val );
				}
			}
		}

		return file;
	}
	
	/**
	 *	Dieses SpectPatch leeren
	 *
	 *	@return	false bei Fehler
	 */
	public boolean clear()
	{
		synchronized( ops ) {
			win.clear();
			ops.removeAllElements();
//			setFileName( null, null );
//			setModified( false );
		}
		return true;
	}
	
	/**
	 *	Operatoren starten
	 *	TEST STADIUM
	 */
	public void start()
	{
		Operator	op;
		Thread		thread;
	
		synchronized( ops )
		{
			if( !running ) {
				pausing = false;
				
				opThreadGroup = new ThreadGroup( "Operators" );

				for( int i = 0; i < ops.size(); i++ ) {
					op = (Operator) ops.elementAt( i );
					if( op.threadDead ) {
						thread = new Thread( opThreadGroup, op, op.getIcon().getName() );
						op.threadPaused	= false;
						op.threadDead	= false;
						op.owner		= this;
				//		thread.setPriority( Thread.NORM_PRIORITY + 1 );
						thread.start();
					}
				}
				running = true;
			//	Thread.currentThread().setPriority( Thread.NORM_PRIORITY - 1 );
			}
		}
	}
	
	/**
	 *	Operatoren stoppen
	 *	TEST STADIUM
	 */
	public void stop()
	{
		Operator		op;
		Enumeration		slots;
		SpectStreamSlot	slot;
	
		synchronized( ops )
		{
			if( running ) {
				pausing = false;

				for( int i = 0; i < ops.size(); i++ ) {
					op = (Operator) ops.elementAt( i );
					if( !op.threadDead ) {
						op.runStop();
						slots = op.getSlots( Slots.SLOTS_LINKED ).elements();
						while( slots.hasMoreElements() ) {
							slot = (SpectStreamSlot) slots.nextElement();
							synchronized( slot ) {
								if( slot.state == SpectStreamSlot.STATE_WAITING ) {
									// wartende Ops informieren
									slot.getOwnerThread().interrupt();
								}
							}
						}
					}
				}
				while( opThreadGroup.activeCount() > 0 ) {
					try {
						Thread.sleep( 500 );	// Operatoren schicken notify() beim Beenden!
					}
					catch( InterruptedException e ) {}
				}
				running = false;
			}
		}
	//	thread.setPriority( Thread.NORM_PRIORITY );
		win.stop();		// möglicherweise wurde gar kein Thread gestartet, der win.stop()
	}					// aufrufen würde
	
	/**
	 *	Operatoren pausieren / Pause beenden
	 *	TEST STADIUM
	 *
	 *	@param	state	true = Pause einlegen; false = Pause beenden
	 */
	public synchronized void pause( boolean state )
	{
		Operator		op;
		Enumeration		slots;
		SpectStreamSlot	slot;
		boolean			allPausing;

		synchronized( ops )
		{
			if( running && (state != pausing) ) {

				for( int i = 0; i < ops.size(); i++ ) {
					op = (Operator) ops.elementAt( i );
					if( !op.threadDead ) {
						op.runPause( state );
						slots = op.getSlots( Slots.SLOTS_LINKED ).elements();
						while( slots.hasMoreElements() ) {
							slot = (SpectStreamSlot) slots.nextElement();
							synchronized( slot ) {
								if( slot.state == SpectStreamSlot.STATE_WAITING ) {
									// wartende Ops informieren
									slot.getOwnerThread().interrupt();
								}
							}
						}
					}
				}
				if( state ) {
					do {
						try {
							Thread.sleep( 500 );
						}
						catch( InterruptedException e ) {}
						allPausing = true;
						for( int i = 0; i < ops.size(); i++ ) {
							op = (Operator) ops.elementAt( i );
							if( !op.threadDead && !op.threadPausing ) {
								allPausing = false;
							}
						}
					} while( !allPausing );
				}
				pausing = state;
			}
		}	
	}

	/**
	 *	Muss von Operatoren aufgerufen werden,
	 *	wenn sie die run() Methode verlassen
	 */
	public synchronized void operatorTerminated( Operator op )
	{
//		notify();
		if( Thread.activeCount() == 1) {		// wenn ich der letzte bin
			running = false;
			win.stop();							// ...dann die Gadgets wieder freigeben
		}
	}

	/**
	 *	Muss von Operatoren aufgerufen werden,
	 *	wenn sie in den Pause-Modus wechseln
	 */
	public synchronized void operatorPaused( Operator op )
	{
//		notify();
	}

	/**
	 *	Neuen Operator hinzufuegen
	 */
	public void addOperator( Operator op )
	{
		synchronized( ops ) {
			ops.addElement( op );
		}
//		setModified( true );
	}
	
	/**
	 *	Operator loeschen
	 *	RUFT Operator.dispose() AUF!
	 */
	public void removeOperator( Operator op )
	{
		synchronized( ops ) {
			ops.removeElement( op );
		}
		op.dispose();
//		setModified( true );
	}

	public AppWindow getWindow()
	{
		return win;
	}

	/**
	 *	(Datei)namen des Dokuments ermitteln
	 *
	 *	@return	moeglicherweise null!
	 */
	public String getName()
	{
		return name;
	}

	/**
	 *	Liste aller Operatoren besorgen
	 */
	public Enumeration getOperators()
	{
		synchronized( ops ) {
			return ops.elements();
		}
	}
}
// class SpectPatch
