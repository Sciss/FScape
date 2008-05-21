/*
 *  OutputOp.java
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

package de.sciss.fscape.op;

import java.awt.*;
import java.io.*;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *	Output Operator zum Schreiben von fft Files
 *
 *  @version	0.71, 14-Nov-07
 */
public class OutputOp
extends Operator
{
// -------- public Variablen --------

// -------- private Variablen --------

	protected static final String defaultName = "Output file";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;		// es kann nur einen geben

	// Properties (defaults)
	private static final int PR_FILENAME		= 0;		// Array-Indices: pr.text
	private static final int PR_CHANNELS		= 0;		// pr.intg
	private static final int PR_OMITDATA		= 1;
	private static final int PR_REMOVEDC		= 0;		// pr.bool

	private static final String PRN_FILENAME		= "Filename";	// Property-Keynames
	private static final String PRN_CHANNELS		= "Channels";
	private static final String PRN_OMITDATA		= "OmitData";
	private static final String PRN_REMOVEDC		= "RemoveDC";

	private static final int PR_CHANNELS_UNTOUCHED= SpectFrame.FLAGS_UNTOUCHED;
//	private static final int PR_CHANNELS_LEFT		= SpectFrame.FLAGS_LEFT;
//	private static final int PR_CHANNELS_RIGHT	= SpectFrame.FLAGS_RIGHT;
//	private static final int PR_CHANNELS_SUM		= SpectFrame.FLAGS_SUM;

	private static final int PR_OMITDATA_UNTOUCHED	= 0;
	private static final int PR_OMITDATA_PHASE		= 1;
	private static final int PR_OMITDATA_AMPLITUDE	= 2;
	
	private static final String	prText[]		= { "" };
	private static final String	prTextName[]	= { PRN_FILENAME };
	private static final int		prIntg[]		= { PR_CHANNELS_UNTOUCHED, PR_OMITDATA_UNTOUCHED };
	private static final String	prIntgName[]	= { PRN_CHANNELS, PRN_OMITDATA };
	private static final boolean	prBool[]		= { false };
	private static final String	prBoolName[]	= { PRN_REMOVEDC };

	// Laufzeitfehler
	protected static final String ERR_NOOUTPUT	= "No output file";

// -------- public Methoden --------
	// public Container createGUI( int type );

	public OutputOp()
	{
		super();
		
		// initialize only in the first instance
		// preferences laden
		if( static_prefs == null ) {
			static_prefs = new OpPrefs( getClass(), getDefaultPrefs() );
		}
		// propertyarray defaults
		if( static_pr == null ) {
			static_pr = new PropertyArray();

			static_pr.text		= prText;
			static_pr.textName	= prTextName;
			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;

			static_pr.superPr	= Operator.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		
		// superclass-Felder übertragen		
		opName		= "OutputOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));	// SLOT_INPUT

		// icon
		icon = new OpIcon( this, OpIcon.ID_OUTPUT, defaultName );
	}

// -------- Runnable Methoden --------

	/**
	 *	TESTSTADIUM XXX
	 */
	public void run()
	{
		runInit();		// superclass

		// Haupt-Variablen fuer den Prozess
		String			runFileName;
		SpectralFile	runFile			= null;
		int				runFileFormat;
	
		SpectStreamSlot	runInSlot;
		SpectStream		runInStream		= null;
		SpectStream		runOutStream	= null;

		SpectFrame		runInFr;
		SpectFrame		runOutFr		= null;
	
		// ------------------------------ Filename ------------------------------
		if( (pr.text[ PR_FILENAME ] == null) || (pr.text[ PR_FILENAME ].length() == 0) ) {

			FileDialog	fDlg;				// ausnahmsweise nicht am Methoden-Anfang ;)
			String		foName, foDir;
			final		Component	ownerF	= owner.getWindow().getWindow();
			final		boolean		makeF	= !(ownerF instanceof Frame);
			final		Frame		f		= makeF ? new Frame() : (Frame) ownerF;
		
			fDlg	= new FileDialog( f, ((OpIcon) getIcon()).getName() +
									  ": Select outputfile", FileDialog.SAVE );
			fDlg.setVisible( true );
			if( makeF ) f.dispose();
			foName	= fDlg.getFile();
			foDir	= fDlg.getDirectory();
			fDlg.dispose();

			if( foDir == null) foDir = "";
			if( foName == null) {				// Cancel. Wir koennen folglich nichts mehr tun
				runQuit( new IOException( ERR_NOOUTPUT ));
				return;
			}
			runFileName = foDir + foName;
			
		} else {
			runFileName = pr.text[ PR_FILENAME ];
		}

topLevel:
		try {
			runFile = new SpectralFile( runFileName, SpectralFile.MODE_OUTPUT |
										(pr.bool[ PR_REMOVEDC ] ? SpectralFile.MODE_REMOVEDC : 0) );

			// ------------------------------ Input-Slot ------------------------------
			runInSlot = (SpectStreamSlot) slots.elementAt( SLOT_INPUT );
			if( runInSlot.getLinked() == null ) {
				runStop();	// threadDead = true -> folgendes for() wird übersprungen
			}
			// diese while Schleife ist nötig, da beim initReader ein Pause eingelegt werden kann
			// und die InterruptException ausgelöst wird; danach versuchen wir es erneut
			for( boolean initDone = false; !initDone && !threadDead; ) {
				try {
					runInStream	= runInSlot.getDescr();	// throws InterruptedException
					initDone = true;
				}
				catch( InterruptedException e ) {}
				runCheckPause();
			}
			if( threadDead ) break topLevel;

			// ------------------------------ Kanäle ------------------------------
			runOutStream = new SpectStream( runInStream );
			if( pr.intg[ PR_CHANNELS ] != PR_CHANNELS_UNTOUCHED ) {
				runOutStream.setChannels( 1 );
			}

			// ------------------------------ Datenselektion ------------------------------
			switch( pr.intg[ PR_OMITDATA ]) {
			case PR_OMITDATA_AMPLITUDE:
				runFileFormat = SpectralFile.PVA_PHASE;
				break;
				
			case PR_OMITDATA_PHASE:
				runFileFormat = SpectralFile.PVA_MAG;
				break;
				
			default:	// UNTOUCHED
				runFileFormat = SpectralFile.PVA_POLAR;
				break;
			}
			runFile.initWriter( runOutStream, runFileFormat );

			// ------------------------------ Hauptschleife ------------------------------
			runSlotsReady();
mainLoop:	while( !threadDead ) {
	 			for( boolean readDone = false; (readDone == false) && !threadDead; ) {
					try {
						runInFr		= runInSlot.readFrame();	// throws InterruptedException
						readDone	= true;
						runOutFr	= new SpectFrame( runInFr, pr.intg[ PR_CHANNELS ]);
						runFrameDone( runInSlot, runInFr );
						runInSlot.freeFrame( runInFr );
					}
					catch( InterruptedException e ) {}
					catch( EOFException e ) {
						break mainLoop;
					}
					runCheckPause();
				}
				if( threadDead ) break mainLoop;

				runFile.writeFrame( runOutFr );
				runFile.freeFrame( runOutFr );
				
			} // break mainLoop (Ende Hauptschleife)

			runInStream.closeReader();
			runFile.close();

		} // break topLevel

		catch( IOException e ) {
			if( runFile != null ) {
				runFile.cleanUp();
			}
			runQuit( e );
			return;
		}
//		catch( OutOfMemoryException e ) {
//			if( runFile != null ) {
//				runFile.cleanUp();
//			}
//			abort( e );
//			return;
//		}
		
		runQuit( null );
	}

// -------- GUI Methoden --------

	public PropertyGUI createGUI( int type )
	{
		PropertyGUI gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"lbFilename;io"+PathField.TYPE_OUTPUTFILE+"|Select output file,pr"+PRN_FILENAME+"\n" +
			"lbChannel mode;ch,pr"+PRN_CHANNELS+"," +
				"itLeave untouched," +
				"itLeft channel only," +
				"itRight channel only," +
				"itSum left + right\n" +
			"cbRemove DC offset,pr"+PRN_REMOVEDC+"\n" +
			"lbSelect data;ch,pr"+PRN_OMITDATA+"," +
				"itLeave untouched," +
				"itOmit phase data," +
				"itOmit amplitude data" );

		return gui;
	}
}
// class OutputOp
