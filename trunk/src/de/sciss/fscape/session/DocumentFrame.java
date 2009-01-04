/*
 *  DocumentFrame.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
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
 *		07-Jan-05	removed deprecated sound file methods ; 
 *					new method indicateOutputWrite()
 *		29-May-05	temp file creation / deletion
 *		24-Jun-06	renamed from ProcessWindow to DocumentFrame
 *		31-Aug-06	fixed weird race condition constructor init bug
 */

package de.sciss.fscape.session;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractWindow;
import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.common.AppWindow;
import de.sciss.common.BasicApplication;
import de.sciss.common.BasicWindowHandler;
import de.sciss.common.ProcessingThread;
import de.sciss.gui.AbstractWindowHandler;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.HelpFrame;
import de.sciss.gui.MenuAction;
import de.sciss.gui.MenuRoot;
import de.sciss.gui.ProgressComponent;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.util.Flag;

import de.sciss.fscape.gui.*;
import de.sciss.fscape.io.*;
import de.sciss.fscape.proc.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.util.*;

/**
 *	Superclass of all processing windows. This handles thread and progress bar
 *	management and has some utility methods such as sound file normalization.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 10-Sep-08
 */
public abstract class DocumentFrame
extends AppWindow
implements Processor, EventManager.Processor, ProgressComponent
{
// -------- public Variablen --------

	public static final String	PACKAGE			= "de.sciss.fscape.gui";
	public static final String	PROP_CLASS		= "Class";

// -------- private Variablen --------

	protected static final int FLAGS_TOOLBAR		= 0x01;
	protected static final int FLAGS_PRESETS		= 0x03;		// (0x02 + FLAGS_TOOLBAR)
	protected static final int FLAGS_PROGBAR		= 0x04;
	protected static final int FLAGS_NORESIZE		= 0x08;
	protected static final int FLAGS_NOPRESETLOAD	= 0x10;
	protected static final int FLAGS_PROGBARASYNC	= 0x24;		// (0x20 + FLAGS_PROGBAR)

	protected static final int GGTYPE_GAIN			= 0;

	protected static final int GAIN_UNITY			= 0;
	protected static final int GAIN_ABSOLUTE		= 1;

	protected static final String PRN_GAINTYPE		= "GainType";
	protected static final String PRN_GAIN			= "Gain";

	protected static final int GG_OFF_CHECKBOX	= 0x000000;
	protected static final int GG_OFF_CHOICE	= 0x000100;
	protected static final int GG_OFF_PARAMFIELD= 0x000200;
	protected static final int GG_OFF_TEXTFIELD	= 0x000300;
	protected static final int GG_OFF_PATHFIELD	= 0x000400;
	protected static final int GG_OFF_FONTFIELD	= 0x000500;
	protected static final int GG_OFF_COLORCHOICE=0x000600;
	protected static final int GG_OFF_ENVICON	= 0x000700;
	protected static final int GG_OFF_OTHER		= 0x000800;

//	private int				flags;
	private	JPanel			toolBar				= null;
	private ProcessPanel	pp					= null;
	private ProgressPanel	pProgress			= null;
	protected GUISupport	gui;

	/*
	 *	Subclassen muessen dieses pr ueberschreiben und super.static_pr in superPr eintragen!
	 *	presets mussen sie mit ihrem static_presets ueberschreiben
	 */
	protected		 PropertyArray	pr;
	protected 		 Presets		presets;
	protected static PropertyArray	static_pr	= null;
	
	public static final String		ERR_CORRUPTED		= "Internal data corrupted. Please report bug!";
	protected static final String	ERR_MEMORY			= "FScape ran out of memory";
	protected static final String	ERR_NOPROPERTIES	= "There are no properties...";
	protected static final String	TXT_OBSCURE			= "> Obscure?! ";
	protected static final String	ERR_CLASS			= "This chosen file was created\nby a different module:\n";
	protected static final String	TXT_SIGH			= ">-Sigh-";
	protected static final String	ERR_MISSINGPROP		= "Bug! Missing property!";
	protected static final String	ERR_EMPTY			= "File is empty";
	protected static final String	ERR_FRAMESYNC		= "Bug! Frame sync lost!";
	protected static final String	ERR_COMPLEX			= "Real and imaginary file must\nhave same # of channels";
	
	/*
	 *	Processor-Interface
	 */
	protected	boolean				threadRunning	= false;
	protected	boolean				threadPausing	= false;
	private		float				progress		= 0.0f;
	private final EventManager		elm				= new EventManager( this );
	private		int					listenerCount	= 0;
	private		Exception			threadError		= null;
	private		boolean				clipping;
	private		float				maxAmp;

	/*
	 *	Gadget goodies
	 */
	private	ParamField	ggGain		= null;
	private	JComboBox	ggGainType	= null;

	private static final Color  COLOR_NORM		= new Color( 0xFF, 0xFF, 0x00, 0x2F );

	private JMenu			mPresets		= new JMenu();
	
	private final DocumentFrame enc_this	= this;
	
	private final java.util.List collTempFiles	= new ArrayList();	// Elements = AudioFile instances
	
// FFFF
//	private final actionRevealFileClass		actionRevealFile;
	private final actionCloseClass			actionClose;
	private final actionSaveClass			actionSave;
	private final actionSaveAsClass			actionSaveAs;
//	private final actionSaveAsClass			actionSaveCopy;

//	private final Main		root;
	private final Session	doc;

//	private final boolean					internalFrames;
	private final BasicApplication			app;

	private final AbstractWindow.Adapter	winListener;
	private final actionShowWindowClass		actionShowWindow;

	private final JLabel					lbWriteProtected;
	private boolean							writeProtected			= false;
	private boolean							wpHaveWarned			= false;

	private final ProcessingThread.Listener	closeAfterSaveListener;

	private final String					procTitle;

	private boolean							disposed		= false;

//	// voruebergehende loesung
//	protected static final ItemListener		dummyItemListener;
//	protected static final PathListener		dummyPathListener;
//	protected static final ParamListener	dummyParamListener;
//	protected static final ActionListener	dummyActionListener;
//	
//	static {
//		dummyItemListener	= new ItemListener() { public void itemStateChanged( ItemEvent e ) {} };
//		dummyPathListener	= new PathListener() { public void pathChanged( PathEvent e ) {} };
//		dummyParamListener	= new ParamListener() { public void paramChanged( ParamEvent e ) {} };
//		dummyActionListener	= new ActionListener() { public void actionPerformed( ActionEvent e ) {} };
//	}
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 *
	 *	siehe Anm. zu static_pr etc. oben im Variablenteil!
	 */
	public DocumentFrame( String procTitle )
	{
		super( REGULAR );

		app					= (BasicApplication) AbstractApplication.getApplication();

		this.procTitle		= procTitle;
//		this.root			= root;
		this.doc			= new Session();
		
		doc.setFrame( this );

//		final int						modif		= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
//		final Container					cp			= getContentPane();
		final MenuRoot					mr;

		// einmalig PropertyArray initialisieren
		if( static_pr == null ) {
			static_pr = new PropertyArray();
		}

//		internalFrames		= app.getWindowHandler().usesInternalFrames();

//		pr = (PropertyArray) static_pr.clone();
//		presets = new Presets( concrete.getClass(), static_pr.toProperties( true ));

	// -------- Basic Listeners --------

//		addWindowListener( new WindowAdapter() {
//			public void windowClosing( WindowEvent e )
//			{	
//				if( (pp != null) && (pp.isSelected() != ProcessPanel.STATE_STOPPED) ) return;	// not while processing
//
//				setVisible( false );
//				dispose();
//			}
//		});
//
		closeAfterSaveListener	= new ProcessingThread.Listener() {
			public void processStarted( ProcessingThread.Event e ) {}

			public void processStopped( ProcessingThread.Event e )
			{
				if( e.isDone() ) {
					documentClosed();
				}
			}
		};

		winListener = new AbstractWindow.Adapter() {
			public void windowClosing( AbstractWindow.Event e ) {
				actionClose.perform();
			}
			
//			public void windowGainedFocus( AbstractWindow.Event e )
			public void windowActivated( AbstractWindow.Event e )
			{
				// need to check 'disposed' to avoid runtime exception in doc handler if document was just closed
				if( !disposed ) {
//System.err.println( "DocumentFrame.winListener.windowActivated(). hashCode = "+enc_this.hashCode() );
					app.getDocumentHandler().setActiveDocument( enc_this, doc );
					app.getMenuFactory().setSelectedWindow( actionShowWindow );
					((BasicWindowHandler) app.getWindowHandler()).setMenuBarBorrower( enc_this );
//				actionShowWindow.setSelected( true );
				}
			}
		};
		this.addListener( winListener );
//		this.addWindowFocusListener( winListener );

		lbWriteProtected	= new JLabel();

//		mb = new MainMenu( root );
//		setJMenuBar( mb );

		// --- Actions ---
		actionClose			= new actionCloseClass();
		actionSave			= new actionSaveClass();
//		actionSave			= new actionSaveClass( app.getResourceString( "procWinSave" ), 
//						KeyStroke.getKeyStroke( KeyEvent.VK_S, modif ), false );
		actionSaveAs		= new actionSaveAsClass( false, false );
//		actionSaveAs		= new actionSaveClass( app.getResourceString( "procWinSaveAs" ), 
//						KeyStroke.getKeyStroke( KeyEvent.VK_S, modif + KeyEvent.SHIFT_MASK ), true );
//		actionSaveCopy		= new actionSaveAsClass( true, false );
// FFFF
//		actionRevealFile	= new actionRevealFileClass();

		actionShowWindow	= new actionShowWindowClass();

		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		
		// ---- menus and actions ----
		mr = app.getMenuBarRoot();
		
// FFFF
//		mr.putMimic( "file.new.fromSelection", this, actionNewFromSel );
//		mr.putMimic( "file.import.markers", this, actionImportMarkers );
		mr.putMimic( "file.close", this, actionClose );
		mr.putMimic( "file.save", this, actionSave );
		mr.putMimic( "file.saveAs", this, actionSaveAs );
// FFFF
//		mr.putMimic( "file.saveCopyAs", this, actionSaveCopyAs );
//		mr.putMimic( "file.saveSelectionAs", this, actionSaveSelectionAs );

// FFFF
		mr.putMimic( "edit.undo", this, doc.getUndoManager().getUndoAction() );
		mr.putMimic( "edit.redo", this, doc.getUndoManager().getRedoAction() );
//		mr.putMimic( "edit.cut", this, doc.getCutAction() );
//		mr.putMimic( "edit.copy", this, doc.getCopyAction() );
//		mr.putMimic( "edit.paste", this, doc.getPasteAction() );
//		mr.putMimic( "edit.clear", this, doc.getDeleteAction() );
//		mr.putMimic( "edit.selectAll", this, actionSelectAll );
//
//		mr.putMimic( "timeline.insertSilence", this, doc.getSilenceAction() );
//		mr.putMimic( "timeline.insertRecording", this, actionInsertRec );
//		mr.putMimic( "timeline.trimToSelection", this, doc.getTrimAction() );
//
//		mr.putMimic( "process.again", this, actionProcessAgain );
//		mr.putMimic( "process.fadeIn", this, actionFadeIn );
//		mr.putMimic( "process.fadeOut", this, actionFadeOut );
//		mr.putMimic( "process.gain", this, actionGain );
//		mr.putMimic( "process.invert", this, actionInvert );
//		mr.putMimic( "process.mix", this, actionMix );
//		mr.putMimic( "process.reverse", this, actionReverse );
//		mr.putMimic( "process.rotateChannels", this, actionRotateChannels );
//		mr.putMimic( "process.fscape.needlehole", this, actionFScNeedlehole );
//
//		mr.putMimic( "debug.dumpTracks", this, actionDebugDump );
		
// FFFF
//		updateEditEnabled( false );
	}

	// hallelujah this was a weird bug, calling back subclass methods in the constructor
	// is a really bad idea. for preliminary fix, subclass must now call init2 after
	// return from super in the constructor !!!
	protected void init2()
	{
		buildGUI();

		AbstractWindowHandler.setDeepFont( getContentPane() );
		app.getMenuFactory().addToWindowMenu( actionShowWindow );	// MUST BE BEFORE INIT()!!
		init();

		updateTitle();
// FFFF
//		documentUpdate();

// FFFF
//		setVisible( true );
//		toFront();
	}

	protected void buildGUI()
	{
	
	}

//	protected void init( Main root )
//	{
//		super.init( root );
//		root.menuFactory.addToWindowMenu( actionShowWindow );
//	}

	protected boolean restoreVisibility()
	{
		return false;
	}

	protected boolean alwaysPackSize()
	{
		return false;
	}
	
	protected boolean autoUpdatePrefs()
	{
		return true;
	}
	
	public Session getDocument()
	{
		return doc;
	}
	
//	public String getProcessName()
//	{
//		return procTitle;
//	}
	
	/**
	 *  Recreates the main frame's title bar
	 *  after a sessions name changed (clear/load/save as session)
	 */
	public void updateTitle()
	{
		final File				f		= doc.getFile();
		final String			name	= doc.getName();
		final Icon				icn;

		writeProtected	= false;
		
// FFFF
//		actionRevealFile.setFile( f );
		
		if( f != null ) {
			try {
				writeProtected |= !f.canWrite() || ((f.getParentFile() != null) && !f.getParentFile().canWrite());
			} catch( SecurityException e ) {}
		}

		if( writeProtected ) {
			icn = GUIUtil.getNoWriteIcon();
			if( lbWriteProtected.getIcon() != icn ) {
				lbWriteProtected.setIcon( icn );
			}
		} else if( lbWriteProtected.getIcon() != null ) {
			lbWriteProtected.setIcon( null );
		}
//		icnWriteProtected.setID( writeProtected ? MutableIcon.WRITE_PROTECTED : MutableIcon.INVISIBLE );
//		lbWriteProtected.repaint();
		
// FFFF
//		if( internalFrames ) {
//			if( doc.isDirty() ) {
//				setTitle( "\u2022" + name );
//			} else {
//				setTitle( name );
//			}
//		} else {
//			setTitle( app.getName() + (doc.isDirty() ? " - \u2022" : " - " ) + name );
//		}
//		actionShowWindow.putValue( Action.NAME, name );
final String title = procTitle + (doc.isDirty() ? " - \u2022" : " - " ) + name;
setTitle( title );
		actionShowWindow.putValue( Action.NAME, title );
// FFFF
//		actionSave.setEnabled( !writeProtected && doc.isDirty() );
		actionSave.setEnabled( !writeProtected );
		setDirty( doc.isDirty() );
		
		if( writeProtected && !wpHaveWarned && doc.isDirty() ) {
//			MutableIcon warnIcon = new MutableIcon( 128 );
//			warnIcon.setID( MutableIcon.WRITE_PROTECTED );
			JOptionPane.showMessageDialog( getWindow(), getResourceString( "warnWriteProtected" ),
				getResourceString( "msgDlgWarn" ), JOptionPane.WARNING_MESSAGE, null );
			wpHaveWarned = true;
		}
	}

	/**
	 *	GUI bauen
	 *	- call once before setVisible() !
	 *	- invokes loading of default preset !
	 *
	 *	@param	concrete	subclass
	 *	@param	flags		FLAGS_...
	 *	@param	c			die "Innereien" (i.d.R. ein GUISupport Panel)
	 */
	protected void initGUI( DocumentFrame concrete, int flags, Component c )
	{
//		GridBagConstraints	con;
//		GridBagLayout		lay;
		
		DetachedMenu					dm;
		JPopupMenu						pop;
		final Container					cp	= getContentPane();
		final de.sciss.app.Application	app	= AbstractApplication.getApplication();
//		int								modif;

//		gui				= new GUISupport();
//		con				= gui.getGridBagConstraints();
//		lay				= gui.getGridBagLayout();
//		con.insets		= new Insets( 1, 2, 1, 2 );

		cp.setLayout( new BorderLayout( 0, 2 ));

	// -------- Toolbar --------
		if( (flags & FLAGS_TOOLBAR) != 0 ) {
			toolBar		= new JPanel( new FlowLayout( FlowLayout.LEFT, 2, 2 ));
			
			if( (flags & FLAGS_PRESETS) != 0 ) {

				Object[] presetNames = Util.iterToArray( getPresets().presetNames() );
				Util.sort( presetNames, true );
//				modif				= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

//				actionSave		= new actionSaveClass( app.getResourceString( "procWinSave" ), 
//					KeyStroke.getKeyStroke( KeyEvent.VK_S, modif ), false );
//				actionRevert	= new actionOpenClass( app.getResourceString( "procWinRevert" ), null );
 
				pop		= new JPopupMenu();
//				pop.add( new actionOpenClass( app.getResourceString( "procWinOpen" ), 
//					KeyStroke.getKeyStroke( KeyEvent.VK_O, modif )));
//				pop.add( actionSave );
//				pop.add( new actionSaveClass( app.getResourceString( "procWinSaveAs" ), 
//					KeyStroke.getKeyStroke( KeyEvent.VK_S, modif + KeyEvent.SHIFT_MASK ), true ));
//				pop.add( actionRevert );
//				dm		= new DetachedMenu( app.getResourceString( "menuFile" ), pop );
//				toolBar.add( dm );
//				updatePopUpStates();

				pop			= new JPopupMenu();
				mPresets.setText( app.getResourceString( "procWinRecallPreset" ));
				for( int i = 0; i < presetNames.length; i++ ) {
					mPresets.add( new actionRecallPresetClass( presetNames[ i ].toString(), null ));
				}
				pop.add( mPresets );
				pop.add( new actionAddPresetClass( app.getResourceString( "procWinAddPreset" ), null ));
				pop.add( new actionDelPresetClass( app.getResourceString( "procWinDelPreset" ), null ));
				dm			= new DetachedMenu( app.getResourceString( "menuPreset" ), pop );
				toolBar.add( dm );
				
				pop			= new JPopupMenu();
				pop.add( new actionHelpClass( app.getResourceString( "procWinShowHelp" ), null ));
				dm			= new DetachedMenu(  app.getResourceString( "menuOptions" ), pop );
				toolBar.add( dm );
			}
//			con.gridwidth	= GridBagConstraints.REMAINDER;
//			lay.setConstraints( toolBar, con );
//			gui.add( toolBar );
			cp.add( toolBar, BorderLayout.NORTH );
		}

	// -------- Die Innereien --------
//		con.fill		= GridBagConstraints.BOTH;
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		con.weightx		= 1.0;
//		con.weighty		= 1.0;
//		lay.setConstraints( c, con );
//		gui.add( c );
		cp.add( c, BorderLayout.CENTER );
		if( (flags & FLAGS_NOPRESETLOAD) == 0 ) {
			loadPreset( Presets.DEFAULT );
		}

	// -------- Close/Process Gadgets --------
		setResizable( (flags & FLAGS_NORESIZE) == 0 );

		if( (flags & FLAGS_PROGBAR) != 0 ) {
//			con.fill		= GridBagConstraints.HORIZONTAL;

//			sleepers		= new Component[ 1 ];
//			sleepers[ 0 ]	= gui;
			pProgress		= new ProgressPanel();
//			pp				= new ProcessPanel( ProcessPanel.TYPE_CLOSEGADGET | ProcessPanel.TYPE_CANPAUSE |
//												 ((flags & FLAGS_PROGBARASYNC) == FLAGS_PROGBARASYNC ?
//												   ProcessPanel.TYPE_ASYNC : 0), pProgress, this );
			pp				= new ProcessPanel( ((flags & FLAGS_PROGBARASYNC) == FLAGS_PROGBARASYNC ?
												   ProcessPanel.TYPE_ASYNC : 0), pProgress, this );
			pp.addProcessorListener( new ProcessorAdapter() {
				public void processorStopped( ProcessorEvent e )
				{
					if( isVisible() ) {
						Exception procErr = getError();
						if( procErr != null ) {
							displayError( procErr, getTitle() );
						}
						if( clipping ) {
							clippingDlg();
						}
					}
				}
			});

			cp.add( pp, BorderLayout.SOUTH );
		}
//		getContentPane().add( gui, "Center" );

//		super.initFrame();
	}

	public ProcessingThread closeDocument( boolean force, Flag wasClosed )
	{
		if( !force ) {
			final ProcessingThread pt = confirmUnsaved( getResourceString( "menuClose" ), wasClosed );
			if( pt != null ) {
				pt.addListener( closeAfterSaveListener );
				return pt;
			}
		}
		if( wasClosed.isSet() ) {
			documentClosed();
		}
		return null;
	}
	
	private void documentClosed()
	{
		disposed = true;	// important to avoid "too late window messages" to be processed; fucking swing doesn't kill them despite listener being removed
//System.err.println( "DocumentFrame.documentClosed()" );
		this.removeListener( winListener );
//		this.removeWindowFocusListener( winListener );	// otherwise we'll try to set an obsolete active doc
		app.getDocumentHandler().removeDocument( this, doc );	// invokes doc.dispose() and hence this.dispose()
//		actionShowWindow.removeAll();
	}

	public void dispose()
	{
// FFFF
//		playTimer.stop();

		app.getMenuFactory().removeFromWindowMenu( actionShowWindow );

// FFFF
//		AudioTrackRowHeader	chanHead;
//		Axis				chanRuler;
	
// FFFF
//		lmm.dispose();
//		wavePanel.dispose();
//		while( !collChannelHeaders.isEmpty() ) {
//			chanHead = (AudioTrackRowHeader) collChannelHeaders.remove( 0 );
//			chanHead.dispose();
//		}
//		while( !collChannelRulers.isEmpty() ) {
//			chanRuler = (Axis) collChannelRulers.remove( 0 );
//			chanRuler.dispose();
//		}
//		for( int ch = 0; ch < channelMeters.length; ch++ ) {
//			channelMeters[ ch ].dispose();
//		}
//		channelMeters = new LevelMeter[ 0 ];
//		markAxis.stopListening();
//		markAxis.dispose();
//		timeAxis.dispose();
//		timeTB.dispose();
//		transTB.dispose();
		
		super.dispose();
	}

	private String getResourceString( String key )
	{
		return AbstractApplication.getApplication().getResourceString( key );
	}

//	public void dispose()
//	{
//		super.dispose();
//	}

	/*
	 *  Checks if there are unsaved changes to
	 *  the session. If so, displays a confirmation
	 *  dialog. Invokes Save/Save As depending
	 *  on user selection. IF the doc was not dirty,
	 *	or if &quot;Cancel&quot; or
	 *	&quot;Don't save&quot; was chosen, the
	 *	method returns <code>null</code> and the
	 *	<code>confirmed</code> flag reflects whether
	 *	the document should be closed. If a saving
	 *	process should be started, that process is
	 *	returned. Note that the <code>ProcessingThread</code>
	 *	in this case has not yet been started, as to
	 *	allow interested objects to install a listener
	 *	first. So it's their job to call the <code>start</code>
	 *	method!
	 *
	 *  @param  actionName		name of the action that
	 *							threatens the session
	 *	@param	confirmed		a flag that will be set to <code>true</code> if
	 *							the doc is allowed to be closed
	 *							(doc was not dirty or user chose &quot;Don't save&quot;),
	 *							otherwise <code>false</code> (save process
	 *							initiated or user chose &quot;Cancel&quot;).
	 *  @return					a saving process yet to be started or <code>null</code>
	 *							if the doc needn't/shouldn't be saved
	 *
	 *	@see	de.sciss.eisenkraut.util.ProcessingThread#start
	 */
	private ProcessingThread confirmUnsaved( String actionName, Flag confirmed )
	{
// FFFF
		if( !confirmAbortProc( actionName )) return null;

		if( !doc.isDirty() ) {
			confirmed.set( true );
			return null;
		}
		
//		ProcessingThread				proc;
//		final de.sciss.app.Application	app		= AbstractApplication.getApplication();
		final String[]					options	= { getResourceString( "buttonSave" ),
													getResourceString( "buttonCancel" ),
													getResourceString( "buttonDontSave" ) };
		int								choice;
//		AudioFileDescr					displayAFD	= doc.getDisplayDescr();
//		AudioFileDescr[]				afds		= doc.getDescr();
		File							f		= doc.getFile();
		String							name;
		
//		if( displayAFD.file == null ) {
//			name = getResourceString( "frameUntitled" );
//		} else {
//			name = displayAFD.file.getName();
//		}
		name	= doc.getName();
		
		choice = JOptionPane.showOptionDialog( getWindow(), procTitle + " (" + name + ") :\n" + getResourceString( "optionDlgUnsaved" ),
											   actionName, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
											   options, options[1] );
		switch( choice ) {
		case JOptionPane.CLOSED_OPTION:
		case 1:	// cancel
			confirmed.set( false );
			return null;
			
		case 2:	// don't save
			confirmed.set( true );
			return null;
			
		case 0:
			confirmed.set( false );
			if( (f == null) || writeProtected ) {
				f = actionSaveAs.query( f, false, false, null );
			}
			if( f != null ) {
//				return actionSave.initiate( actionSave.getValue( Action.NAME ).toString(), null, f, false, false );
confirmed.set( actionSave.perform( actionSave.getValue( Action.NAME ).toString(), f, false, false ));
				return null;
			}
			return null;
			
		default:
			assert false : choice;
			return null;
		}
	}

	private boolean confirmAbortProc( String actionName )
	{
		if( pp.getState() == ProcessPanel.STATE_STOPPED ) return true;
		
//		final de.sciss.app.Application	app		= AbstractApplication.getApplication();
		int								choice;
//		ProcessingThread				proc;
		String							name;
		
		name	= doc.getName();
		
		choice = JOptionPane.showOptionDialog( getWindow(), name + " :\n" + getResourceString( "optionDlgAbortProc" ),
											   actionName, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
											   null, null );
		switch( choice ) {
		case JOptionPane.CLOSED_OPTION:
		case JOptionPane.NO_OPTION:		// cancel
			return false;
			
		case JOptionPane.YES_OPTION:	// abort
			for( int i = 0; i < 20; i++ ) {
				pp.stop();
				try {
					Thread.sleep( 1000 );
				}
				catch( InterruptedException e1 ) {}
				if( pp.getState() == ProcessPanel.STATE_STOPPED ) return true;
			}
			return false;
			
		default:
			assert false : choice;
			return false;
		}
	}

	/**
	 *	Besorgt die Parameter des Dialogs in Form eines PropertyArray Objekts
	 */
//	protected PropertyArray getPropertyArray()
	public PropertyArray getPropertyArray()
	{
		return pr;
	}
	
	/**
	 *	Liefert die Presets
	 */	
	protected Presets getPresets()
	{
		return presets;
	}

//	protected MainMenu getMenu()
//	{
//		return mb;
//	}

	/*
	 *	laedt Werte eines Presets ins GUI u. PropertyArray
	 */	
	private boolean loadPreset( String name )
	{
		PropertyArray	pa;
		Properties		preset;
		boolean			visible		= isVisible();
		boolean			success		= false;

//		if( visible ) GUISupport.sendComponentAsleep( this );
		try {
			pa		= getPropertyArray();
			preset	= getPresets().getPreset( name );
			pa.fromProperties( false, preset );
			fillGUI();
			success	= true;
//			if( visible ) GUISupport.wakeComponent( this );
		
		} catch( Exception e99 ) {
			if( visible ) {
				displayError( e99, getTitle() );
//				GUISupport.wakeComponent( this );
			}
		}

		if( success ) {
			presetsChanged();
		}
		return success;
	}

	/*
	 *	Stores the current GUI values in a new preset
	 *	of the given name.
	 */
	private boolean addPreset( String name )
	{
		PropertyArray	pa;
		Properties		preset;
		boolean			visible		= isVisible();
		boolean			success		= false;
		int				i, comp;

		fillPropertyArray();
		pa		= getPropertyArray();
		preset	= pa.toProperties( false );

		if( !preset.isEmpty() ) {								// erfolgreich?
			if( getPresets().setPreset( name, preset ) != null ) {
				success = true;
				for( i = 0; i < mPresets.getItemCount(); i++ ) {
					comp = mPresets.getItem( i ).getText().compareTo( name );
					if( comp == 0 ) return true;	// we're overwriting existing one, don't modify menu
					if( comp > 0 ) break;
				}
				mPresets.insert( new actionRecallPresetClass( name, null ), i );

			} else {
				if( visible ) displayError( new IllegalStateException( ERR_CORRUPTED ), getTitle() );
			}
		} else {
			if( visible ) {
				JOptionPane.showMessageDialog( getWindow(), ERR_NOPROPERTIES );
			}
		}
		
		if( success ) {
			presetsChanged();
			success = storePresetFile();
		}
		return success;
	}
	
	/*
	 *	Deletes a preset
	 */
	private boolean deletePreset( String name )
	{
		boolean	success = false;
		
		if( getPresets().removePreset( name ) != null ) {
			success = true;
			for( int i = 0; i < mPresets.getItemCount(); i++ ) {
				if( mPresets.getItem( i ).getText().equals( name )) {
					mPresets.remove( i );
					break;
				}
			}
		} else {
			if( isVisible() ) displayError( new IllegalStateException( ERR_CORRUPTED ), getTitle() );
		}

		if( success ) {
			presetsChanged();
			success = storePresetFile();
		}
		return success;
	}

	private boolean storePresetFile()
	{
		boolean							success		= false;
		final String					oldTitle	= getTitle();
		final de.sciss.app.Application	app			= AbstractApplication.getApplication();
	
//		if( getPresets().isModified() ) {
			try {
				setTitle( app.getResourceString( "procWinSavingPresets" ));
saveLoop:		do {
					try {
//						getPresets().store();		// save presets to harddisk
						getPresets().store( true );		// save presets to harddisk
						success = true;
						break saveLoop;
					} catch( IOException e1 ) {
						int result = JOptionPane.showConfirmDialog( getWindow(),
										app.getResourceString( "errSavingPresets" ) + ":\n" +
										e1.getMessage() + "\n" + app.getResourceString( "optionDlgRetry" ),
										app.getResourceString( "optionDlgConfirm" ), JOptionPane.YES_NO_OPTION );
						if( result != 0 ) break saveLoop;	// do not retry
					}
				} while( true );
			}
			finally {
				setTitle( oldTitle );
			}
//		}
		return success;
	}

	/**
	 *	Settings laden
	 *
	 *	@return		true on success
	 */
	public boolean loadFile( File f )
	{
		PropertyArray	pa;
		BasicProperties	preset;
		boolean			visible		= isVisible();
		String			str;
		boolean			success		= false;

//		if( visible ) GUISupport.sendComponentAsleep( this );
		try {
			pa		= getPropertyArray();
			preset	= new BasicProperties( null, f );
			try {
				preset.load();
				str = preset.getProperty( PROP_CLASS );
				if( (str != null) && getClass().getName().endsWith( str )) {
					pa.fromProperties( false, preset );
					fillGUI();
					success = true;
				} else {
					if( visible ) {
						JOptionPane.showMessageDialog( getWindow(), ERR_CLASS + str );
					}
				}
			} catch( IOException e1 ) {
				if( visible ) displayError( e1, getTitle() );
			}
//			if( visible ) GUISupport.wakeComponent( this );

		} catch( Exception e99 ) {
			if( visible ) {
				displayError( e99, getTitle() );
//				GUISupport.wakeComponent( this );
			}
		}
		
		if( success ) {
			fileChanged( f );
		}
		return success;
	}

	/*
	 *	Settings speichern
	 *
	 *	@return		true on success
	 */
	private boolean saveFile( File f )
	{
		PropertyArray	pa;
		BasicProperties	preset;
		boolean			visible		= isVisible();
		boolean			success		= false;

//		if( visible ) GUISupport.sendComponentAsleep( this );
		try {
			fillPropertyArray();
			pa		= getPropertyArray();
			preset	= new BasicProperties( null, f );
			pa.toProperties( false, preset );
	
			if( !preset.isEmpty() ) {								// erfolgreich?
				preset.setProperty( PROP_CLASS, getClass().getName() );
				try {
					preset.store( true );
					success = true;
				} catch( IOException e1 ) {
					if( visible ) displayError( e1, getTitle() );
				}
			} else {
				if( visible ) {
					JOptionPane.showMessageDialog( getWindow(), ERR_NOPROPERTIES );
				}
			}
//			if( visible ) GUISupport.wakeComponent( this );

		} catch( Exception e99 ) {
			if( visible ) {
				displayError( e99, getTitle() );
//				GUISupport.wakeComponent( this );
			}
		}

		if( success ) {
			fileChanged( f );
		}
		return success;
	}

	private void fileChanged( File f )
	{
		doc.setFile( f );
//		this.fileName	= fileName;

//		actionRevert.setEnabled( fileName != null );
//		actionSave.setEnabled( f != null );
		updateTitle();
	}

	private void presetsChanged()
	{
//		PopupStrip		pop;
//		JMenu			m;
//	
// ZZZ
//		if( dmPreset != null ) {
//			pop = dmPreset.getStrip();
//			m	= (JMenu) pop.getItem( MI_RECALL );
//			if( m != null ) {
//				pop.setItemEnabled( MI_DELPRESET, m.getItemCount() > 1 );
//			}
//		}
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 *	subclasses must override and invoke super.fillGUI() !
	 */
//	protected void fillGUI()
	public void fillGUI()
	{
		// nothing here yet
	}

	public boolean isThreadRunning()
	{
		return threadRunning;
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 *	subclasses can use this to make things easy
	 *	VORAUSSETZUNG:	Gadget-IDs in GUI stimmen mit denen in PropertyArray plus GG_OFF_...
	 *					ueberein
	 */
	public void fillGUI( GUISupport gui )
	{
		PropertyArray	pa	= getPropertyArray();
		int				i;
		Component		c;
		
		try {
			for( i = 0; i < pa.bool.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_CHECKBOX );
				if( c != null ) {
					((JCheckBox) c).setSelected( pa.bool[ i ]);
				}
			}
			for( i = 0; i < pa.intg.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_CHOICE );
				if( (c != null) && (((JComboBox) c).getItemCount() > pa.intg[ i ]) ) {
					((JComboBox) c).setSelectedIndex( pa.intg[ i ]);
				}
			}
			for( i = 0; i < pa.para.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_PARAMFIELD );
				if( c != null ) {
					((ParamField) c).setParam( pa.para[ i ]);
				}
			}
			for( i = 0; i < pa.text.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_TEXTFIELD );
				if( c != null ) {
					((JTextField) c).setText( pa.text[ i ]);
				} else {
					c = gui.getItemObj( i + GG_OFF_PATHFIELD );
					if( c != null ) {
						((PathField) c).setPath( new File( pa.text[ i ]));
					}
				}
			}
//			for( i = 0; i < pa.font.length; i++ ) {
//				c = gui.getItemObj( i + GG_OFF_FONTFIELD );
//				if( c != null ) {
//					((FontField) c).setFontSC( pa.font[ i ]);
//				}
//			}
//			for( i = 0; i < pa.colr.length; i++ ) {
//				c = gui.getItemObj( i + GG_OFF_COLORCHOICE );
//				if( c != null ) {
//					((ColorChoice) c).setColor( pa.colr[ i ]);
//				}
//			}
			for( i = 0; i < pa.envl.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_ENVICON );
				if( c != null ) {
					((EnvIcon) c).setEnv( pa.envl[ i ]);
				}
			}
		}
		catch( ClassCastException e1 ) {
			displayError( e1, getTitle() );
		}
		
		reflectPropertyChanges();
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 *	subclasses must override and invoke super.fillPropertyArray() !
	 */
//	protected void fillPropertyArray()
	public void fillPropertyArray()
	{
		// nothing here yet
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 *	subclasses can use this to make things easy
	 *	VORAUSSETZUNG:	Gadget-IDs in GUI stimmen mit denen in PropertyArray plus GG_OFF_...
	 *					ueberein
	 */
	protected void fillPropertyArray( GUISupport gui )
	{
		PropertyArray	pa	= getPropertyArray();
		int				i;
		Component		c;
		
		try {
			for( i = 0; i < pa.bool.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_CHECKBOX );
				if( c != null ) {
					pa.bool[ i ] = ((JCheckBox) c).isSelected();
				}
			}
			for( i = 0; i < pa.intg.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_CHOICE );
				if( c != null ) {
					pa.intg[ i ] = ((JComboBox) c).getSelectedIndex();
				}
			}
			for( i = 0; i < pa.para.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_PARAMFIELD );
				if( c != null ) {
					pa.para[ i ] = ((ParamField) c).getParam();
				}
			}
			for( i = 0; i < pa.text.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_TEXTFIELD );
				if( c != null ) {
					pa.text[ i ] = ((JTextField) c).getText();
				} else {
					c = gui.getItemObj( i + GG_OFF_PATHFIELD );
					if( c != null ) {
						pa.text[ i ] = ((PathField) c).getPath().getPath();
					}
				}
			}
//			for( i = 0; i < pa.font.length; i++ ) {
//				c = gui.getItemObj( i + GG_OFF_FONTFIELD );
//				if( c != null ) {
//					pa.font[ i ] = ((FontField) c).getFontSC();
//				}
//			}
//			for( i = 0; i < pa.colr.length; i++ ) {
//				c = gui.getItemObj( i + GG_OFF_COLORCHOICE );
//				if( c != null ) {
//					pa.colr[ i ] = ((ColorChoice) c).getColor();
//				}
//			}
			for( i = 0; i < pa.envl.length; i++ ) {
				c = gui.getItemObj( i + GG_OFF_ENVICON );
				if( c != null ) {
					pa.envl[ i ] = ((EnvIcon) c).getEnv();
				}
			}
		}
		catch( ClassCastException e1 ) {
			displayError( e1, getTitle() );
		}
	}

	/*
	 *	Standard-Gadgets erstellen
	 *
	 *	@param	type	GGTYPE_...
	 *	@return	Array mit den Gadgets. GGTYPE_GAIN: c[0]=ParamField,c[1]=JComboBox( GAIN_...)
	 */
	protected Component[] createGadgets( int type )
	{
		Component c[];
	
		switch( type ) {
		case GGTYPE_GAIN:
			c			= new Component[ 2 ];
			ggGain		= new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
			ggGainType	= new JComboBox();
			ggGainType.addItem( "normalized" );
			ggGainType.addItem( "immediate" );
			c[ 0 ]		= ggGain;
			c[ 1 ]		= ggGainType;
			break;
			
		default:
			c = new Component[ 0 ];
			break;
		}
		return c;
	}

	/*
	 *	Dialog mit Clipping-Info und Option zum Anpassen des Gain-Feldes
	 *	vorbereiten; aus process() aufzurufen
	 */
	protected void handleClipping( float maxAmp )
	{
		clipping = ((maxAmp < 0.707f) || (maxAmp > 1.0f));
		if( !clipping ) return;

		int	gainType = (ggGainType == null ? GAIN_ABSOLUTE : ggGainType.getSelectedIndex() );
		
		clipping	= clipping && (gainType != GAIN_UNITY);			// user might want low amp or clipping
		this.maxAmp	= maxAmp;
	}
	
	/*
	 *	Dialog mit Clipping-Info und Option zum Anpassen des Gain-Feldes
	 *	(Anpassung wird automatisch vorgenommen)
	 */
	private void clippingDlg()
	{
		double			newAmp	  	= 1.0 / ((double) maxAmp * 1.0115794543);	// 0.1 dB headroom
		Param			p, pa, ref;
//		ConfirmDlg		confirm;
		Double			ampCorrect	= new Double( 20 * Math.log( newAmp ) / Constants.ln10 );
		Object[]		msgArgs		= { ampCorrect };
		String			msgPtrn		= new String( "The output {0,choice,-1#is clipped|0#volume is suboptimal}!"+
												  "\nShall the gain be adjusted by {0,number,#,##0.0} dB?" );
		MessageFormat	msgForm		= new MessageFormat( msgPtrn );
		int				i;
														 
		msgForm.setLocale( Locale.US );
		msgForm.applyPattern( msgPtrn );
		
		if( ggGain != null ) {
			i = JOptionPane.showConfirmDialog( getWindow(), msgForm.format( msgArgs ), "Confirm", JOptionPane.YES_NO_OPTION );
			if( i == 0 ) {
				p	= ggGain.getParam();
				ref	= new Param( 1.0, Param.ABS_AMP );
				pa	= Param.transform( p, Param.ABS_AMP, ref, null );
				p	= Param.transform( new Param( pa.val * newAmp, pa.unit ), p.unit, ref, null );
				ggGain.setParam( p );
			}
			
		} else {
			JOptionPane.showMessageDialog( getWindow(), msgForm.format( msgArgs ) );
		}
	}

	/**
	 *	Sets graphical indicator
	 *	when module is writing to output file
	 *	(e.g. when normalizing)
	 *	so the user may pre-listen to
	 *	updates of the file
	 */
	protected void indicateOutputWrite()
	{
		pp.setPaint( COLOR_NORM );
	}
		
	/**
	 *	Float-File in AudioFile schreiben und Gain veraendern dabei
	 *
	 *	@param	srcF		entweder eindimensional (Daten interleaved) oder per Channel
	 *	@param	destF		Ziel
	 *	@param	buf			Puffer
	 *	@param	gain		gain
	 *	@param	progEnd		fuer setProgression()
	 *	@return	true, wenn abgeschlossen, sonst false (bei threadRunning == false)
	 */
	protected boolean normalizeAudioFile( FloatFile srcF[], AudioFile destF, float buf[][], float gain, float progEnd )
	throws IOException
	{
		AudioFileDescr	stream			= destF.getDescr();
		int				outChanNum		= stream.channels;
		int				outLength;
		int				framesWritten;
		int				bufSize			= buf[ 0 ].length;
		float			progOff			= getProgression();
		float			progWeight		= progEnd - progOff;
		float[]			bufSub, tempBuf;
		int				i, j, ch, len;
		
//		pp.setText( "normalize..." );
		indicateOutputWrite();

		for( ch = 0; ch < srcF.length; ch++ ) {
			srcF[ ch ].seekFloat( 0 );
		}
		framesWritten	= 0;
		
		if( srcF.length == outChanNum ) {

			outLength = (int) srcF[ 0 ].getSize();

			while( (framesWritten < outLength) && threadRunning ) {

				len  = Math.min( bufSize, outLength - framesWritten );
				for( ch = 0; ch < outChanNum; ch++ ) {
					bufSub = buf[ ch ];
					srcF[ ch ].readFloats( bufSub, 0, len );
					for( i = 0; i < len; i++ ) {
						bufSub[ i ] *= gain;
					}
				}
				destF.writeFrames( buf, 0, len );
				framesWritten	+= len;
			// .... progress ....
				setProgression( (float) framesWritten / (float) outLength * progWeight + progOff );
			}

		} else if( srcF.length == 1 ) {
		
			outLength		= (int) (srcF[ 0 ].getSize() / outChanNum);
			framesWritten	= 0;
			tempBuf			= new float[ bufSize * outChanNum ];

			while( (framesWritten < outLength) && threadRunning ) {

				len  = Math.min( bufSize, outLength - framesWritten );
				srcF[ 0 ].readFloats( tempBuf, 0, len * outChanNum );
				for( ch = 0; ch < outChanNum; ch++ ) {
					bufSub = buf[ ch ];
					for( i = 0, j = ch; i < len; i++, j += outChanNum ) {
						bufSub[ i ] = tempBuf[ j ] * gain;
					}
				}
				destF.writeFrames( buf, 0, len );
				framesWritten	+= len;
			// .... progress ....
				setProgression( (float) framesWritten / (float) outLength * progWeight + progOff );
			}

		} else {
			System.err.println( "DocumentFrame.normalizeAudioFile : illegal floatfile channel #" );
		}
		
		return( threadRunning );
	}

	protected boolean normalizeAudioFile( AudioFile srcF, AudioFile destF, float buf[][], float gain, float progEnd )
	throws IOException
	{
		final int		outChanNum		= destF.getDescr().channels;
		final long		outLength		= srcF.getFrameNum();
		final int		bufSize			= buf[ 0 ].length;
		final float		progOff			= getProgression();
		final float		progWeight		= progEnd - progOff;
		long			framesWritten;
		int				i, ch, len;
		float[]			convBuf1;
		
//		pp.setText( "normalize..." );
		indicateOutputWrite();

		srcF.seekFrame( 0 );
		for( framesWritten = 0; (framesWritten < outLength) && threadRunning; ) {
			len  = (int) Math.min( bufSize, outLength - framesWritten );
			srcF.readFrames( buf, 0, len );
			for( ch = 0; ch < outChanNum; ch++ ) {
				convBuf1 = buf[ ch ];
				for( i = 0; i < len; i++ ) {
					convBuf1[ i ] *= gain;
				}
			}
			destF.writeFrames( buf, 0, len );
			framesWritten	+= len;
		// .... progress ....
			setProgression( (float) framesWritten / (float) outLength * progWeight + progOff );
		}
		
		return( threadRunning );
	}

	protected AudioFile createTempFile( AudioFileDescr template )
	throws IOException
	{
		return createTempFile( template.channels, template.rate );
	}

	protected AudioFile createTempFile( int numChannels, double rate )
	throws IOException
	{
			final AudioFileDescr afd = new AudioFileDescr();
			AudioFile af;
			
			afd.type			= AudioFileDescr.TYPE_AIFF;
			afd.channels		= numChannels;
			afd.rate			= rate;
			afd.bitsPerSample	= 32;
			afd.sampleFormat	= AudioFileDescr.FORMAT_FLOAT;
			afd.file			= IOUtil.createTempFile( "fsc", ".aif" );
			af					= AudioFile.openAsWrite( afd );
			
			collTempFiles.add( af );
			return af;
	}
	
	protected void deleteTempFile( AudioFile af )
	{
		collTempFiles.remove( af );
		af.cleanUp();
		af.getDescr().file.delete();
	}
	
	private void deleteAllTempFiles()
	{
		while( !collTempFiles.isEmpty() ) {
			deleteTempFile( (AudioFile) collTempFiles.get( 0 ));
		}
	}

// -------- Processor Interface --------
		
	/**
	 *	Output synthetisieren
	 *	E N T E R   T H E   H E A R T   O F   T H E   D R A G O N   ! ! ! ==========================================
	 */
	public void run()
	{
		fillPropertyArray();	// retrieve latest values
//		System.gc();			// the algorithm may need a lot of memory
		setProgression( 0.0f );
		deleteAllTempFiles();

		setError( null );
		clipping	= false;
		resume();
		elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.STARTED,
			System.currentTimeMillis(), this ));

		try {
			process();
		}
		catch( OutOfMemoryError e1 ) {
			setError( new Exception( ERR_MEMORY ));
		}
		catch( Exception e2 ) {
			setError( e2 );
		}
		finally {
			deleteAllTempFiles();
			stop();
			elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.STOPPED,
				System.currentTimeMillis(), this ));
		}
	} // run()

	/*
	 *	Supclassiz ovaride dis wan
	 */
	protected abstract void process();

	public void start()
	{
		pp.start();
	}
	
	public void pause()
	{
		threadRunning = true;
		threadPausing = true;
	}

	public void resume()
	{
		synchronized( this ) {
			threadRunning = true;
			threadPausing = false;
			notify();
		}
	}
	public void stop()
	{
		synchronized( this ) {
			threadRunning = false;
			threadPausing = false;
			notify();
		}
	}

	public void addProcessorListener( ProcessorListener li )
	{
		elm.addListener( li );
		listenerCount++;
	}

	public void removeProcessorListener( ProcessorListener li )
	{
		elm.removeListener( li );
		listenerCount--;
	}
	
	public float getProgression()
	{
		return progress;
	}

	public Exception getError()
	{
		return threadError;
	}

	public void setError( Exception e )
	{
		threadError = e;
	}
	
//	protected void setProgression( float p )
//	{
//		progress = p;
//		if( listenerCount > 0 ) {	// avoid overhead when noone is listening
//			elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.PROGRESS,
//				System.currentTimeMillis(), this ));
//		}
//	// .... check pause ....
//		if( threadPausing ) {
//			try {
//				synchronized( this ) {
//					elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.PAUSED,
//						System.currentTimeMillis(), this ));
//					this.wait();
//				}
//			} catch( InterruptedException e1 ) {}
//
//			elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.RESUMED,
//				System.currentTimeMillis(), this ));
//		}
//	}
	
// ---------- EventManager.Processor interface ----------

	public void processEvent( BasicEvent e )
	{
		ProcessorListener li;
		
		for( int i = 0; i < elm.countListeners(); i++ ) {
			li = (ProcessorListener) elm.getListener( i );
			switch( e.getID() ) {
			case ProcessorEvent.PROGRESS:
				li.processorProgress( (ProcessorEvent) e );
				break;
			case ProcessorEvent.STARTED:
				li.processorStarted( (ProcessorEvent) e );
				break;
			case ProcessorEvent.STOPPED:
				li.processorStopped( (ProcessorEvent) e );
				break;
			case ProcessorEvent.PAUSED:
				li.processorPaused( (ProcessorEvent) e );
				break;
			case ProcessorEvent.RESUMED:
				li.processorResumed( (ProcessorEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}

	protected ProcessPanel getProcessPanel()
	{
		return pp;
	}

	/*
	 *	Subclasses should override & invoke this
	 *	; use to enable/disable gadgets after e.g. checkbox switches
	 */
	protected void reflectPropertyChanges()
	{
		// nothing
	}

// ---------------- ProgressComponent interface ---------------- 

	public void addCancelListener( ActionListener l )
	{
		pProgress.addCancelListener( l );
	}

	public void removeCancelListener( ActionListener l )
	{
		pProgress.removeCancelListener( l );
	}

	public Component getComponent()
	{
		return getWindow();
	}
	
	public void resetProgression()
	{
// XXX
//		pProgress.resetProgression();
//		pOverlay.performFade( 1f, 1000, 250 );
	}

	public void setProgression( float p )
	{
		progress = p;
		if( listenerCount > 0 ) {	// avoid overhead when noone is listening
			elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.PROGRESS,
				System.currentTimeMillis(), this ));
		}
	// .... check pause ....
		if( threadPausing ) {
			try {
				synchronized( this ) {
					elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.PAUSED,
						System.currentTimeMillis(), this ));
					this.wait();
				}
			} catch( InterruptedException e1 ) {}

			elm.dispatchEvent( new ProcessorEvent( this, ProcessorEvent.RESUMED,
				System.currentTimeMillis(), this ));
		}
	}

//	public void setProgression( float p )
//	{
//		pProgress.setProgression( p );
//	}
	
	public void	finishProgression( int result )
	{
// XXX FFFF
//		pProgress.finishProgression( success );
//		pOverlay.performFade( 0f, 4000, 250 );
	}
	
	public void setProgressionText( String text )
	{
// XXX
//		pProgress.setProgressionText( text );
	}
	
	public void showMessage( int type, String text )
	{
// XXX
//		pProgress.showMessage( type, text );
	}
	
	public void displayError( Exception e, String processName )
	{
		GUIUtil.displayError( getWindow(), e, processName );
	}

// -------- internal classes --------

	private class actionShowWindowClass
	extends MenuAction	// SyncedMenuAction
	{
		private actionShowWindowClass()
		{
			super( null, null );
		}

		public void actionPerformed( ActionEvent e )
		{
//			boolean state   = ((AbstractButton) e.getSource()).isSelected();
//
//			setSelected( true );
			enc_this.setVisible( true );
			enc_this.toFront();
		}
	}

//	private class actionOpenClass
//	extends MenuAction
//	{
//		private actionOpenClass( String name, KeyStroke acc )
//		{
//			super( name, acc );
//		}
//		
//		public void actionPerformed( ActionEvent e )
//		{
//			if( threadRunning ) return;	// not while processing
//		
//			final FileDialog	fDlg;
//			final String		fDir, fFile;
//			File				f;
//		
//			fDlg	= new FileDialog( enc_this, getValue( NAME ).toString(), FileDialog.LOAD );
//			f		= doc.getFile();
//			if( f != null ) {
//				fDlg.setFile( f.getName() );
//				fDlg.setDirectory( f.getParent() );
//			}
//			fDlg.setVisible( true );
//			fDir	= fDlg.getDirectory();
//			fFile	= fDlg.getFile();
//			if( (fDir != null) && (fFile != null) ) {
//				f = new File( fDir, fFile );
//			} else {
//				f	= null;
//			}
//			fDlg.dispose();
//			if( f != null ) {
//				loadFile( f );
//			}
//		}
//	}

	// action for the Save-Session menu item
	private class actionCloseClass
	extends MenuAction
	{
		public void actionPerformed( ActionEvent e )
		{
			perform();
		}
		
		public void perform()
		{
			final ProcessingThread pt = closeDocument( false, new Flag( false ));
			if( pt != null ) pt.start();
		}
	}

//	private class actionRevertClass
//	extends MenuAction
//	{
//		private actionRevertClass( String name, KeyStroke acc )
//		{
//			super( name, acc );
//		}
//		
//		public void actionPerformed( ActionEvent e )
//		{
//			if( threadRunning ) return;	// not while processing
//		
//			if( fileName != null ) {
//				loadFile( fileName );
//			}
//		}
//	}

	private class actionSaveClass
	extends MenuAction
	{
		private actionSaveClass()
		{
			super();
		}
		
		/**
		 *  Saves a Session. If the file
		 *  wasn't saved before, a file chooser
		 *  is shown before.
		 */
		public void actionPerformed( ActionEvent e )
		{
			File f = doc.getFile();			

			if( f == null ) {
				f = actionSaveAs.query( f, false, false, null );
			}
			if( f != null ) {
				perform( getValue( NAME ).toString(), f, false, false );
			}
		}
		
		private boolean perform( String name, File f, boolean asCopy, boolean openAfterSave )
		{
//			initiate( name, span, afds, asCopy, openAfterSave ).start();
			final boolean success = saveFile( f );
			if( asCopy ) {
				System.err.println( "WARNING: asCopy : NOT YET WORKING" );
			}
			if( openAfterSave ) {
				System.err.println( "WARNING: openAfterSave : NOT YET WORKING" );
			}
//			if( success ) {
//				doc.setFile( f );
//				updateTitle();
//			}
			return success;
		}
	}

	// action for the Save-Session-As menu item
	private class actionSaveAsClass
	extends MenuAction
	{
		private final boolean	asCopy;
		private final boolean	selection;
		private final Flag		openAfterSave;
	
		private actionSaveAsClass( boolean asCopy, boolean selection )
		{
			if( selection && !asCopy ) throw new IllegalArgumentException();

			this.asCopy		= asCopy;
			this.selection	= selection;
			openAfterSave	= new Flag( false );
		}
	
		/*
		 *  Query a file name from the user and save the Session
		 */
		public void actionPerformed( ActionEvent e )
		{
			final File f = query( doc.getFile(), asCopy, selection, openAfterSave );
			if( f != null ) {
				actionSave.perform( getValue( NAME ).toString(), f, asCopy, openAfterSave.isSet() );
			}
		}
		
		/**
		 *  Open a file chooser so the user
		 *  can select a new output file and format for the session.
		 *
		 *  @return the AudioFileDescr representing the chosen file path
		 *			and format or <code>null</code>
		 *			if the dialog was cancelled.
		 */
		protected File query( File protoType, boolean asCopy, boolean selection, Flag openAfterSave )
		{
			final FileDialog			fDlg;
			File						f, f2;
			int							i, result;
			String						str;
			String[]					queryOptions;

			if( protoType == null ) {
				f	= new File( System.getProperty( "user.home" ));
				f2	= new File( f, "Desktop" );
				f	= new File( f2.isDirectory() ? f2 : f, getResourceString( "frameUntitled" ));
			} else {
				if( asCopy || selection ) {
					str	= protoType.getName();
					i	= str.lastIndexOf( '.' );
					if( i == -1 ) i = str.length();
					f	= new File( protoType.getParentFile(), str.substring( 0, i ) +
						 (selection ? getResourceString( "fileDlgCut" ) : " " + getResourceString( "fileDlgCopy" )));
				} else {
					f	= protoType;
				}
			}

//			affp.automaticFileSuffix( ggPathFields[ j ] );
			f = IOUtil.setFileSuffix( f, "fsc" );

			if( (protoType == null) || asCopy || selection ) {	// create non-existent file name
				f = IOUtil.nonExistentFileVariant( f, -1, selection ? null : " ", null );
			}

			if( asCopy ) {
//				ggOpenAfterSave = new JCheckBox( getResourceString( "labelOpenAfterSave" ));
//				ggOpenAfterSave.setSelected( openAfterSave.isSet() );
//				msgPane.gridAdd( ggOpenAfterSave, 1, y );
			}

//			GUIUtil.setDeepFont( msgPane, GraphicsUtil.smallGUIFont );
//			msgPane.makeCompactGrid();
//			p			= new JPanel( new BorderLayout() );
//			p.add( msgPane, BorderLayout.NORTH );
//
			Frame fuckingAWT = new Frame();
			fDlg = new FileDialog( fuckingAWT, getValue( NAME ).toString(), FileDialog.SAVE );
			fDlg.setDirectory( f.getParent() );
			fDlg.setFile( f.getName() );
			
			fDlg.setVisible( true );
			fuckingAWT.dispose();
			
//			if( ggOpenAfterSave != null ) {
//				openAfterSave.set( ggOpenAfterSave.isSelected() );
//			}
if( asCopy ) openAfterSave.set( false );

			if( fDlg.getFile() != null ) {
				f = new File( fDlg.getDirectory(), fDlg.getFile() );
				fDlg.dispose();
				if( f.exists() ) {
					queryOptions = new String[] { getResourceString( "buttonOverwrite" ),
												  getResourceString( "buttonCancel" )};
					result = JOptionPane.showOptionDialog( getWindow(), getResourceString( "warnFileExists" ) +
						":\n" + f.getAbsolutePath() + "\n" + getResourceString( "warnOverwriteFile" ),
						getValue( NAME ).toString(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
						null, queryOptions, queryOptions[1] );
					if( result != 0 ) return null;
				}
				return f;
			} else {
				fDlg.dispose();
				return null;
			}
		}
	}

	private class actionHelpClass
	extends MenuAction
	{
		private actionHelpClass( String name, KeyStroke acc )
		{
			super( name, acc );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			try {
				final String className = enc_this.getClass().getName();
				HelpFrame.openViewerAndLoadHelpFile( className.substring( className.lastIndexOf( '.' ) + 1 ));
			} catch( Exception e1 ) {
				GUIUtil.displayError( getWindow(), e1, getTitle() );
			}
		}
	}

	private class actionAddPresetClass
	extends MenuAction
	{
		private actionAddPresetClass( String name, KeyStroke acc )
		{
			super( name, acc );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( threadRunning ) return;	// not while processing

			final de.sciss.app.Application app = AbstractApplication.getApplication();
			
			String name = JOptionPane.showInputDialog( getWindow(), app.getResourceString( "procWinEnterPresetName" ));
			if( name != null && name.length() > 0 ) {
				if( name.equals( Presets.DEFAULT )) {
					JOptionPane.showMessageDialog( getWindow(), app.getResourceString( "procWinDefaultPreset" ));
					return;
				}

				for( Iterator iter = getPresets().presetNames(); iter.hasNext(); ) {
					if( name.equals( iter.next() )) {
						if( JOptionPane.showConfirmDialog( getWindow(), name + ":\n"+
								app.getResourceString( "procWinOverwritePreset" ),
								app.getResourceString( "optionDlgConfirm" ),
								JOptionPane.YES_NO_OPTION ) != 0 ) return;	// abort
					}
				}
				
				addPreset( name );
			}
		}
	}

	private class actionDelPresetClass
	extends MenuAction
	{
		private actionDelPresetClass( String name, KeyStroke acc )
		{
			super( name, acc );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( threadRunning ) return;	// not while processing

			final de.sciss.app.Application app = AbstractApplication.getApplication();
			ListDlg list;
			String[] allowedNames;
			Object[] presetNames	= Util.iterToArray( getPresets().presetNames() );
			Util.sort( presetNames, true );
			
			allowedNames = new String[ presetNames.length - 1 ];
			for( int i = 0, j = 0; (i < presetNames.length) && (j < allowedNames.length); i++ ) {
				if( !presetNames[ i ].equals( Presets.DEFAULT )) {
					allowedNames[ j++ ] = presetNames[ i ].toString();
				}
			}
			list = new ListDlg( getWindow(), app.getResourceString( "procWinChooseDelPreset" ), allowedNames );
			if( list.getList() >= 0 ) {
				deletePreset( allowedNames[ list.getList() ]);
			}
		}
	}

	private class actionRecallPresetClass
	extends MenuAction
	{
		private actionRecallPresetClass( String name, KeyStroke acc )
		{
			super( name, acc );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( threadRunning ) return;	// not while processing

			loadPreset( getValue( NAME ).toString() );
		}
	}
}
// class DocumentFrame
