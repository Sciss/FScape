/*
 *  AmpEnvOp.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.op;

import java.io.EOFException;
import java.io.IOException;

import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Envelope;
import de.sciss.fscape.util.Modulator;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;
import de.sciss.fscape.util.Util;

/**
 * 	@author		Hanns Holger Rutz
 *  @version	0.71, 19-Feb-10
 */
public class AmpEnvOp
extends Operator
{
// -------- public fields --------

// -------- private fields --------

	protected static final String defaultName = "Amp Env";

	protected static Presets		static_presets	= null;
	protected static Prefs			static_prefs	= null;
	protected static PropertyArray	static_pr		= null;

	// Slots
	protected static final int SLOT_INPUT		= 0;
	protected static final int SLOT_OUTPUT		= 1;

	private static final int PR_ENV			= 0;		// pr.envl

	private static final String PRN_ENV				= "Env";

	private static final Envelope	prEnvl[]	= { null };
	private static final String	prEnvlName[]	= { PRN_ENV };
	
// -------- public methods --------

	public AmpEnvOp()
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

//			static_pr.bool		= prBool;
//			static_pr.boolName	= prBoolName;
//			static_pr.intg		= prIntg;
//			static_pr.intgName	= prIntgName;

//			static_pr.para		= prPara;
//			static_pr.para[ PR_LOMODDEPTH ]		= new Param(   12.0, Param.OFFSET_SEMITONES );
//			static_pr.paraName	= prParaName;

			static_pr.envl				= prEnvl;
			static_pr.envl[ PR_ENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName			= prEnvlName;

			static_pr.superPr	= Operator.op_static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
				
		// register superclass entries		
		opName		= "AmpEnvOp";
		prefs		= static_prefs;
		presets		= static_presets;
		pr			= (PropertyArray) static_pr.clone();

		// slots
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_READER ));
		slots.addElement( new SpectStreamSlot( this, Slots.SLOTS_WRITER ));

		// icon
		icon = new OpIcon( this, OpIcon.ID_FLIPFREQ, defaultName );
	}

// -------- Runnable Methoden --------

	public void run()
	{
		runInit();

		final SpectStreamSlot	runInSlot;
		final SpectStreamSlot	runOutSlot;
		SpectStream				runInStream	= null;
		final SpectStream		runOutStream;
		SpectFrame				runInFr		= null;
		SpectFrame				runOutFr	= null;
		float					gain;
		float[]					convBuf1;

topLevel:
		try {
			// ------------------------------ Input-Slot ------------------------------
			runInSlot = (SpectStreamSlot) slots.elementAt( SLOT_INPUT );
			if( runInSlot.getLinked() == null ) {
				runStop();	// -> threadDead = true
			}
			for( boolean initDone = false; !initDone && !threadDead; ) {
				try {
					runInStream	= runInSlot.getDescr();	// throws InterruptedException
					initDone = true;
				}
				catch( InterruptedException ignored) {}
				runCheckPause();
			}
			if( threadDead ) break topLevel;

			// ------------------------------ Output-Slot ------------------------------
			runOutSlot	= (SpectStreamSlot) slots.elementAt( SLOT_OUTPUT );
			runOutStream = new SpectStream( runInStream );
			runOutSlot.initWriter( runOutStream );

			// ------------------------------ init ------------------------------

			// create modulators
			final Modulator envMod = new Modulator( new Param( 0.0, Param.ABS_AMP ), new Param( 1.0, Param.ABS_AMP ),
											 pr.envl[ PR_ENV ], runInStream );
			
			runSlotsReady();
			// ------------------------------ main process ------------------------------
mainLoop:	while( !threadDead ) {
				gain = (float) envMod.calc().val;

	 			for( boolean readDone = false; (!readDone) && !threadDead; ) {
					try {
						runInFr		= runInSlot.readFrame();	// throws InterruptedException
						readDone	= true;
						runOutFr	= runOutStream.allocFrame();
					}
					catch( InterruptedException ignored) {}
					catch( EOFException e ) {
						break mainLoop;
					}
					runCheckPause();
				}
				if( threadDead ) break mainLoop;

				Util.copy( runInFr.data, 0, runOutFr.data, 0, runInStream.bands << 1 );
				for( int ch = 0; ch < runInStream.chanNum; ch++ ) {		// alle Kanaele
					convBuf1 = runOutFr.data[ ch ];
					for( int i = 0, j = 0; j < runInStream.bands; i += 2, j++ ) {
						convBuf1[ i ] *= gain;
					}
				}

				runInSlot.freeFrame( runInFr );

				for( boolean writeDone = false; (!writeDone) && !threadDead; ) {
					try {	// Unterbrechung
						runOutSlot.writeFrame( runOutFr );	// throws InterruptedException
						writeDone = true;
						runFrameDone( runOutSlot, runOutFr  );
						runOutStream.freeFrame( runOutFr );
					}
					catch( InterruptedException ignored) {}	// mainLoop wird eh gleich verlassen
					runCheckPause();
				}
			}

			runInStream.closeReader();
			runOutStream.closeWriter();

		}
		catch( IOException e ) {
			runQuit( e );
			return;
		}
		catch( SlotAlreadyConnectedException e ) {
			runQuit( e );
			return;
		}

		runQuit( null );
	}

// -------- GUI Methoden --------

	public PropertyGUI createGUI( int type )
	{
		PropertyGUI gui;

		if( type != GUI_PREFS ) return null;

		gui = new PropertyGUI(
			"gl"+GroupLabel.NAME_GENERAL+"\n" +
			"en,pr"+PRN_ENV );

		return gui;
	}
}
// class XFadeOp
