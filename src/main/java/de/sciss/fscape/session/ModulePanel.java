/*
 *  ModulePanel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.swing.*;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.common.ProcessingThread;
import de.sciss.fscape.Application;
import de.sciss.fscape.gui.EnvIcon;
import de.sciss.fscape.gui.GUISupport;
import de.sciss.fscape.gui.ParamField;
import de.sciss.fscape.gui.PathField;
import de.sciss.fscape.gui.ProcessPanel;
import de.sciss.fscape.gui.ProgressPanel;
import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.proc.Processor;
import de.sciss.fscape.proc.ProcessorAdapter;
import de.sciss.fscape.proc.ProcessorEvent;
import de.sciss.fscape.proc.ProcessorListener;
import de.sciss.fscape.prop.BasicProperties;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.MenuAction;
import de.sciss.gui.MenuItem;
import de.sciss.gui.MenuRoot;
import de.sciss.gui.ProgressComponent;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.util.Flag;

/**
 *	Superclass of all processing windows. This handles thread and progress bar
 *	management and has some utility methods such as sound file normalization.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.73, 09-Aug-09
 */
public abstract class ModulePanel
    extends JPanel
// extends AppWindow
implements Processor, EventManager.Processor, ProgressComponent
{
// -------- public Variablen --------

    public static final String	PACKAGE			= "de.sciss.fscape.gui";
    public static final String	PROP_CLASS		= "Class";

// -------- private Variablen --------

    protected static final int FLAGS_TOOLBAR		= 0x01;
    protected static final int FLAGS_PRESETS		= 0x02; // 0x03;		// (0x02 + FLAGS_TOOLBAR)
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

    private	JPanel			toolBar				= null;
    private ProcessPanel	pp					= null;
    private ProgressPanel	pProgress			= null;
    protected GUISupport	gui;

    private MenuAction		actionDeletePreset	= null;
    /*
     *	Subclassen muessen dieses pr ueberschreiben und super.static_pr in superPr eintragen!
     *	presets mussen sie mit ihrem static_presets ueberschreiben
     */
    protected		 PropertyArray	pr;
    protected 		 Presets		presets;

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

    private final ModulePanel enc_this	= this;

    private final List collTempFiles	= new ArrayList();	// Elements = AudioFile instances

    //	private final ActionClose			actionClose;
    //	private final ActionSave			actionSave;
    //	private final ActionSaveAs			actionSaveAs;

    private final Session	doc;

    //	private final BasicApplication			app;
    //	private final AbstractWindow.Adapter	winListener;

    // private final ActionShowWindow			actionShowWindow;

    private final JLabel					lbWriteProtected;
    private boolean							writeProtected			= false;
    private boolean							wpHaveWarned			= false;

    private final ProcessingThread.Listener	closeAfterSaveListener;

    private final String					procTitle;

    private boolean							disposed		= false;

// -------- public --------

    public ModulePanel(String procTitle) {
        super(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        this.procTitle		= procTitle;
        this.doc			= new Session();

        doc.setFrame( this );

        final MenuRoot					mr;

    // -------- Basic Listeners --------

        closeAfterSaveListener	= new ProcessingThread.Listener() {
            public void processStarted( ProcessingThread.Event e ) {}

            public void processStopped( ProcessingThread.Event e )
            {
                if( e.isDone() ) {
                    documentClosed();
                }
            }
        };

        //		winListener = new AbstractWindow.Adapter() {
        //			public void windowClosing( AbstractWindow.Event e ) {
        //				actionClose.perform();
        //			}
        //
        //			public void windowActivated( AbstractWindow.Event e )
        //			{
        //				// need to check 'disposed' to avoid runtime exception in doc handler if document was just closed
        //				if( !disposed ) {
        //					app.getDocumentHandler().setActiveDocument( enc_this, doc );
        //					app.getMenuFactory().setSelectedWindow( actionShowWindow );
        //					((BasicWindowHandler) app.getWindowHandler()).setMenuBarBorrower( enc_this );
        //				}
        //			}
        //		};
        //		this.addListener( winListener );

        lbWriteProtected	= new JLabel();

        // --- Actions ---
        //		actionClose			= new ActionClose();
        //		actionSave			= new ActionSave();
        //		actionSaveAs		= new ActionSaveAs( false, false );
        //
        //		actionShowWindow	= new ActionShowWindow();

        // setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );

        // ---- menus and actions ----
        //		mr = app.getMenuBarRoot();
        //
        //		mr.putMimic( "file.close", this, actionClose );
        //		mr.putMimic( "file.save", this, actionSave );
        //		mr.putMimic( "file.saveAs", this, actionSaveAs );
        //
        //		mr.putMimic( "edit.undo", this, doc.getUndoManager().getUndoAction() );
        //		mr.putMimic( "edit.redo", this, doc.getUndoManager().getRedoAction() );
        //
        //		mr.putMimic( "help.module", this, new ActionHelp( procTitle + " " + getResourceString( "menuHelp" ), null ));
    }

    public String getModuleName() { return procTitle; }

    // hallelujah this was a weird bug, calling back subclass methods in the constructor
    // is a really bad idea. for preliminary fix, subclass must now call init2 after
    // return from super in the constructor !!!
    protected void init2()
    {
        buildGUI();

        // AbstractWindowHandler.setDeepFont( getContentPane() );
        // app.getMenuFactory().addToWindowMenu( actionShowWindow );	// MUST BE BEFORE INIT()!!
        // init();

        // updateTitle();
    }

    protected void buildGUI()
    {

    }

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

    protected void fillDefaultAudioDescr( int[] intg, int typeIdx )
    {
        fillDefaultAudioDescr( intg, typeIdx, -1, -1 );
    }

    protected void fillDefaultAudioDescr( int[] intg, int typeIdx, int resIdx )
    {
        fillDefaultAudioDescr( intg, typeIdx, resIdx, -1 );
    }

    protected void fillDefaultAudioDescr( int[] intg, int typeIdx, int resIdx, int rateIdx )
    {
        final Preferences prefs = Application.userPrefs;

        if( typeIdx >= 0 ) {
            final int typ = GenericFile.getType( prefs.get( "audioFileType", "" ));
            for( int idx = 0; idx < GenericFile.TYPES_SOUND.length; idx++ ) {
                if( GenericFile.TYPES_SOUND[ idx ] == typ ) {
                    intg[ typeIdx ] = idx;
                    break;
                }
            }
        }
        if( resIdx >= 0 ) {
            final int idx = PathField.getSoundResIdx( prefs.get( "audioFileRes", "" ));
            if( idx >= 0 ) {
                intg[ resIdx ] = idx;
            }
        }
        if( rateIdx >= 0 ) {
            final int idx = PathField.getSoundRateIdx( prefs.get( "audioFileRate", "" ));
            if( idx >= 0 ) {
                intg[ rateIdx ] = idx;
            }
        }
    }

    protected void fillDefaultGain( Param[] para, int gainIdx )
    {
        para[ gainIdx ] = getDefaultGain();
    }

    protected Param getDefaultGain()
    {
        final Preferences prefs = Application.userPrefs;
        final de.sciss.util.Param gainP = de.sciss.util.Param.fromPrefs( prefs, "headroom", null );
        if( gainP != null ) {
            return new Param( gainP.val, Param.DECIBEL_AMP );
        } else {
            return null;
        }
    }

    public Session getDocument()
    {
        return doc;
    }

    //	/**
    //	 *  Recreates the main frame's title bar
    //	 *  after a sessions name changed (clear/load/save as session)
    //	 */
    //	public void updateTitle()
    //	{
    //		final File				f		= doc.getFile();
    //		final String			name	= doc.getName();
    //		final Icon				icn;
    //
    //		writeProtected	= false;
    //
    //		if( f != null ) {
    //			try {
    //				writeProtected |= !f.canWrite() || ((f.getParentFile() != null) && !f.getParentFile().canWrite());
    //			} catch( SecurityException e ) {}
    //		}
    //
    //		if( writeProtected ) {
    //			icn = GUIUtil.getNoWriteIcon();
    //			if( lbWriteProtected.getIcon() != icn ) {
    //				lbWriteProtected.setIcon( icn );
    //			}
    //		} else if( lbWriteProtected.getIcon() != null ) {
    //			lbWriteProtected.setIcon( null );
    //		}
    //
    //final String title = procTitle + (doc.isDirty() ? " - \u2022" : " - " ) + name;
    //setTitle( title );
    //		actionShowWindow.putValue( Action.NAME, title );
    //
    //		actionSave.setEnabled( !writeProtected );
    //		setDirty( doc.isDirty() );
    //
    //		if( writeProtected && !wpHaveWarned && doc.isDirty() ) {
    //			JOptionPane.showMessageDialog( getWindow(), getResourceString( "warnWriteProtected" ),
    //				getResourceString( "msgDlgWarn" ), JOptionPane.WARNING_MESSAGE, null );
    //			wpHaveWarned = true;
    //		}
    //	}

    private String createPresetMenuID( String name )
    {
        return( "preset_" + name );  // warning: don't use period
    }

    private MenuItem createPresetMenuItem( String name )
    {
        return new MenuItem( createPresetMenuID( name ), new ActionRecallPreset( name, null ));
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
    protected void initGUI( ModulePanel concrete, int flags, Component c )
    {
        //		final Container					cp	= getContentPane();
        //
        //		cp.setLayout( new BorderLayout( 0, 2 ));
        final Container cp = this;

    // -------- Toolbar --------
        if( (flags & FLAGS_TOOLBAR) != 0 ) {
            toolBar		= new JPanel( new FlowLayout( FlowLayout.LEFT, 2, 2 ));

            //			if( (flags & FLAGS_PRESETS) != 0 ) {
            //                actionDeletePreset = new ActionDeletePreset("Delete Preset", null);
            //                final List presetNames = getPresets().presetNames();
            //				final MenuGroup mg = getPresetMenu();
            //                mg.add(this, new MenuItem("store", new ActionAddPreset("Add Preset", null)));
            //                mg.add( this, new MenuItem( "delete", actionDeletePreset ));
            //				mg.addSeparator( this );
            //				for( int i = 0; i < presetNames.size(); i++ ) {
            //					final String pstName = presetNames.get( i ).toString();
            //					mg.add( this, createPresetMenuItem( pstName ));
            //				}
            //				presetNames.remove( Presets.DEFAULT );
            //				actionDeletePreset.setEnabled( !presetNames.isEmpty() );
            //			}
            cp.add( toolBar, BorderLayout.NORTH );
        }

    // -------- Die Innereien --------
        cp.add( c, BorderLayout.CENTER );
        if( (flags & FLAGS_NOPRESETLOAD) == 0 ) {
            loadPreset( Presets.DEFAULT );
        }

    // -------- Close/Process Gadgets --------
        if( (flags & FLAGS_PROGBAR) != 0 ) {
            pProgress		= new ProgressPanel();
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
    }

    public ProcessingThread closeDocument(boolean force, Flag wasClosed) {
        if (!force) {
            final ProcessingThread pt = confirmUnsaved(getResourceString("menuClose"), wasClosed);
            if (pt != null) {
                pt.addListener(closeAfterSaveListener);
                return pt;
            }
        }
        if (wasClosed.isSet()) {
            documentClosed();
        }
        return null;
    }

    protected void documentClosed() {
        disposed = true;    // important to avoid "too late window messages" to be processed; fucking swing doesn't kill them despite listener being removed
        // this.removeListener( winListener );
        // app.getDocumentHandler().removeDocument( this, doc );	// invokes doc.dispose() and hence this.dispose()
    }

    public void dispose()
    {
        // app.getMenuFactory().removeFromWindowMenu( actionShowWindow );

        // super.dispose();
    }

    protected String getResourceString( String key )
    {
        return key;
    }

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
        if( !confirmAbortProc( actionName )) return null;

        if( !doc.isDirty() ) {
            confirmed.set( true );
            return null;
        }

        final String[]					options	= { getResourceString( "buttonSave" ),
                                                    getResourceString( "buttonCancel" ),
                                                    getResourceString( "buttonDontSave" ) };
        int								choice;
        File							f		= doc.getFile();
        String							name;

        name	= doc.getName();

        choice = JOptionPane.showOptionDialog(getComponent(), procTitle + " (" + name + ") :\n" + getResourceString( "optionDlgUnsaved" ),
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
//			if( (f == null) || writeProtected ) {
//				f = actionSaveAs.query( f, false, false, null );
//			}
//			if( f != null ) {
//confirmed.set( actionSave.perform( actionSave.getValue( Action.NAME ).toString(), f, false, false ));
//				return null;
//			}
            return null;

        default:
            assert false : choice;
            return null;
        }
    }

    public boolean isRunning() { return pp.getState() != ProcessPanel.STATE_STOPPED; }

    private boolean confirmAbortProc(String actionName) {
        if (pp.getState() == ProcessPanel.STATE_STOPPED) return true;

        int choice;
        String name;

        name = doc.getName();

        choice = JOptionPane.showOptionDialog(getComponent(), name + " :\n" + getResourceString("optionDlgAbortProc"),
                actionName, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                null, null);
        switch (choice) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.NO_OPTION:         // cancel
                return false;

            case JOptionPane.YES_OPTION:        // abort
                for (int i = 0; i < 20; i++) {
                    pp.stop();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // ignore
                    }
                    if (pp.getState() == ProcessPanel.STATE_STOPPED) return true;
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

    /*
     *	laedt Werte eines Presets ins GUI u. PropertyArray
     */
    protected boolean loadPreset( String name )
    {
        PropertyArray	pa;
        Properties		preset;
        boolean			visible		= isVisible();
        boolean			success		= false;

        try {
            pa      = getPropertyArray();
            preset  = getPresets().getPreset(name);
            pa.fromProperties(false, preset);
            fillGUI();
            success = true;

        } catch (Exception e99) {
            if( visible ) {
                displayError( e99, getTitle() );
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
    protected boolean addPreset( String name )
    {
//		fillPropertyArray();
//		final PropertyArray	pa		= getPropertyArray();
//		final Properties	preset	= pa.toProperties( false );
//
//		if( !preset.isEmpty() ) {								// erfolgreich?
//			final Presets pst = getPresets();
//			final boolean overwrite = pst.containsPreset( name );
//			if( pst.setPreset( name, preset ) != null ) {
//				if( !overwrite ) { // create new menu entry
//					final List presetNames = pst.presetNames();
//					final int idx = presetNames.indexOf( name ) + 3; // store / remove / sep
////					getPresetMenu()
//					final MenuGroup mg = getPresetMenu();
//					if( mg != null ) mg.add( this, createPresetMenuItem( name ), idx );
//					if( actionDeletePreset != null ) actionDeletePreset.setEnabled( true );
//				}
//				presetsChanged();
//				return true;
//
//			} else {
//				if( isVisible() ) displayError( new IllegalStateException( ERR_CORRUPTED ), getTitle() );
//			}
//		} else {
//			if( isVisible() ) {
//				JOptionPane.showMessageDialog( getWindow(), ERR_NOPROPERTIES );
//			}
//		}
        return false;
    }

    /*
     *	Deletes a preset
     */
    protected boolean deletePreset( String name )
    {
//		final Presets pst = getPresets();
//		if( pst.removePreset( name ) != null ) {
//			final MenuGroup mg = getPresetMenu();
//			if( mg != null ) mg.remove( this, mg.get( this, createPresetMenuID( name )));
//			if( actionDeletePreset != null ) {
//				final List presetNames = pst.presetNames();
//				presetNames.remove( Presets.DEFAULT );
//				if( presetNames.isEmpty() ) actionDeletePreset.setEnabled( false );
//			}
//			presetsChanged();
//			return storePresetFile();
//		} else {
//			if( isVisible() ) displayError( new IllegalStateException( ERR_CORRUPTED ), getTitle() );
//			return false;
//		}
        return false;
    }

    private boolean storePresetFile()
    {
        return false;
//		boolean							success		= false;
//saveLoop:		do {
//					try {
//						getPresets().store( true );		// save presets to harddisk
//						success = true;
//						break saveLoop;
//					} catch( IOException e1 ) {
//						int result = JOptionPane.showConfirmDialog( getWindow(),
//										app.getResourceString( "errSavingPresets" ) + ":\n" +
//										e1.getMessage() + "\n" + app.getResourceString( "optionDlgRetry" ),
//										app.getResourceString( "optionDlgConfirm" ), JOptionPane.YES_NO_OPTION );
//						if( result != 0 ) break saveLoop;	// do not retry
//					}
//				} while( true );
//		return success;
    }

    /**
     *	Settings laden
     *
     *	@return		true on success
     */
    public boolean loadFile(File f) {
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
                        JOptionPane.showMessageDialog(getComponent(), ERR_CLASS + str );
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

        if (success) {
            fileChanged(f);
        }
        return success;
    }

    /*
     *	Settings speichern
     *
     *	@return		true on success
     */
    public boolean saveFile(File f) {
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
                    JOptionPane.showMessageDialog( getComponent(), ERR_NOPROPERTIES );
                }
            }
//			if( visible ) GUISupport.wakeComponent( this );

        } catch( Exception e99 ) {
            if( visible ) {
                displayError( e99, getTitle() );
//				GUISupport.wakeComponent( this );
            }
        }

        if (success) {
            fileChanged(f);
        }
        return success;
    }

    private void fileChanged(File f) {
        doc.setFile(f);
        // updateTitle();
    }

    private void presetsChanged()
    {
    }

    /**
     *	Werte aus Prop-Array in GUI uebertragen
     *	subclasses must override and invoke super.fillGUI() !
     */
    public void fillGUI()
    {
        // nothing here yet
    }

    public boolean isThreadRunning()
    {
        return threadRunning;
    }

    private void setCheckBoxQuiet( JCheckBox cb, boolean selected )
    {
        final ActionListener[] al = cb.getActionListeners();
        for( int i = 0; i < al.length; i++ ) {
            cb.removeActionListener( al[ i ]);
        }
        try {
            cb.setSelected( selected );
        } finally {
            for( int i = 0; i < al.length; i++ ) {
                cb.addActionListener( al[ i ]);
            }
        }
    }

    private void setComboBoxQuiet( JComboBox cb, int idx )
    {
        final ActionListener[] al = cb.getActionListeners();
        for( int i = 0; i < al.length; i++ ) {
            cb.removeActionListener( al[ i ]);
        }
        try {
            cb.setSelectedIndex( idx );
        } finally {
            for( int i = 0; i < al.length; i++ ) {
                cb.addActionListener( al[ i ]);
            }
        }
    }

    /**
     *	Werte aus Prop-Array in GUI uebertragen
     *	subclasses can use this to make things easy
     *	VORAUSSETZUNG:	Gadget-IDs in GUI stimmen mit denen in PropertyArray plus GG_OFF_...
     *					ueberein
     */
    public void fillGUI( GUISupport g )
    {
        PropertyArray	pa	= getPropertyArray();
        int				i;
        Component		c;

        try {
            for( i = 0; i < pa.bool.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_CHECKBOX );
                if( c != null ) {
                    this.setCheckBoxQuiet( (JCheckBox) c, pa.bool[ i ]);
                }
            }
            for( i = 0; i < pa.intg.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_CHOICE );
                if( (c != null) && (((JComboBox) c).getItemCount() > pa.intg[ i ]) ) {
                    this.setComboBoxQuiet( (JComboBox) c, pa.intg[ i ]);
                }
            }
            for( i = 0; i < pa.para.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_PARAMFIELD );
                if( c != null ) {
                    ((ParamField) c).setParam( pa.para[ i ]);
                }
            }
            for( i = 0; i < pa.text.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_TEXTFIELD );
                if( c != null ) {
                    ((JTextField) c).setText( pa.text[ i ]);
                } else {
                    c = g.getItemObj( i + GG_OFF_PATHFIELD );
                    if( c != null ) {
                        ((PathField) c).setPath( new File( pa.text[ i ]));
                    }
                }
            }
            for( i = 0; i < pa.envl.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_ENVICON );
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
    protected void fillPropertyArray( GUISupport g )
    {
        PropertyArray	pa	= getPropertyArray();
        int				i;
        Component		c;

        try {
            for( i = 0; i < pa.bool.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_CHECKBOX );
                if( c != null ) {
                    pa.bool[ i ] = ((JCheckBox) c).isSelected();
                }
            }
            for( i = 0; i < pa.intg.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_CHOICE );
                if( c != null ) {
                    pa.intg[ i ] = ((JComboBox) c).getSelectedIndex();
                }
            }
            for( i = 0; i < pa.para.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_PARAMFIELD );
                if( c != null ) {
                    pa.para[ i ] = ((ParamField) c).getParam();
                }
            }
            for( i = 0; i < pa.text.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_TEXTFIELD );
                if( c != null ) {
                    pa.text[ i ] = ((JTextField) c).getText();
                } else {
                    c = g.getItemObj( i + GG_OFF_PATHFIELD );
                    if( c != null ) {
                        pa.text[ i ] = ((PathField) c).getPath().getPath();
                    }
                }
            }
            for( i = 0; i < pa.envl.length; i++ ) {
                c = g.getItemObj( i + GG_OFF_ENVICON );
                if( c != null ) {
                    pa.envl[ i ] = ((EnvIcon) c).getEnv();
                }
            }
        }
        catch( ClassCastException e1 ) {
            displayError( e1, getTitle() );
        }
    }

    protected String getTitle() { return procTitle; }

    /*
     *	Standard-Gadgets erstellen
     *
     *	@param	type	GGTYPE_...
     *	@return	Array mit den Gadgets. GGTYPE_GAIN: c[0]=ParamField,c[1]=JComboBox( GAIN_...)
     */
    protected Component[] createGadgets( int type )
    {
        final Component c[];

        switch( type ) {
        case GGTYPE_GAIN:
            final ParamField gg1 = new ParamField( Constants.spaces[ Constants.decibelAmpSpace ]);
            final JComboBox  gg2 = new JComboBox();
            gg2.addItem( "normalized" );
            gg2.addItem( "immediate" );
            ggGain		= gg1;
            ggGainType	= gg2;
            c = new Component[] { gg1, gg2 };
            gg2.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e )
                {
                    switch( gg2.getSelectedIndex() ) {
                    case GAIN_ABSOLUTE:
                        gg1.setParam( new Param( 0.0, Param.DECIBEL_AMP ));
                        break;
                    case GAIN_UNITY:
                        gg1.setParam( getDefaultGain() );
                        break;
                    default:
                        break;
                    }
                }
            });
            break;

        default:
            throw new IllegalArgumentException( String.valueOf( type ));
        }
        return c;
    }

    /*
     *	Dialog mit Clipping-Info und Option zum Anpassen des Gain-Feldes
     *	vorbereiten; aus process() aufzurufen
     */
    protected void handleClipping( float mxAmp )
    {
        clipping = ((mxAmp < 0.707f) || (mxAmp > 1.0f));
        if( !clipping ) return;

        int	gainType = (ggGainType == null ? GAIN_ABSOLUTE : ggGainType.getSelectedIndex() );

        clipping	= clipping && (gainType != GAIN_UNITY);			// user might want low amp or clipping
        maxAmp		= mxAmp;
    }

    /*
     *	Dialog mit Clipping-Info und Option zum Anpassen des Gain-Feldes
     *	(Anpassung wird automatisch vorgenommen)
     */
    protected void clippingDlg()
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
            i = JOptionPane.showConfirmDialog(getComponent(), msgForm.format( msgArgs ), "Confirm", JOptionPane.YES_NO_OPTION );
            if( i == 0 ) {
                p	= ggGain.getParam();
                ref	= new Param( 1.0, Param.ABS_AMP );
                pa	= Param.transform( p, Param.ABS_AMP, ref, null );
                p	= Param.transform( new Param( pa.val * newAmp, pa.unit ), p.unit, ref, null );
                ggGain.setParam( p );
            }

        } else {
            JOptionPane.showMessageDialog(getComponent(), msgForm.format(msgArgs));
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
        return this; // getWindow();
    }

    public void resetProgression()
    {
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

    public void	finishProgression( int result )
    {
    }

    public void setProgressionText( String text )
    {
    }

    public void showMessage( int type, String text )
    {
    }

    public void displayError( Exception e, String processName )
    {
        GUIUtil.displayError(getComponent(), e, processName);
    }

// -------- internal classes --------

    //	private class ActionShowWindow
    //	extends MenuAction	// SyncedMenuAction
    //	{
    //		protected ActionShowWindow()
    //		{
    //			super( null, null );
    //		}
    //
    //		public void actionPerformed( ActionEvent e )
    //		{
    //			enc_this.setVisible( true );
    //			enc_this.toFront();
    //		}
    //	}

    //	// action for the Save-Session menu item
    //	protected class ActionClose
    //	extends MenuAction
    //	{
    //		public void actionPerformed( ActionEvent e )
    //		{
    //			perform();
    //		}
    //
    //		public void perform()
    //		{
    //			final ProcessingThread pt = closeDocument( false, new Flag( false ));
    //			if( pt != null ) pt.start();
    //		}
    //	}

    //	private class ActionHelp
    //	extends MenuAction
    //	{
    //		protected ActionHelp( String name, KeyStroke acc )
    //		{
    //			super( name, acc );
    //		}
    //
    //		public void actionPerformed( ActionEvent e )
    //		{
    //			try {
    //				final String className = enc_this.getClass().getName();
    //				HelpFrame.openViewerAndLoadHelpFile( className.substring( className.lastIndexOf( '.' ) + 1 ));
    //			} catch( Exception e1 ) {
    //				GUIUtil.displayError( getWindow(), e1, getTitle() );
    //			}
    //		}
    //	}

    //	private class ActionAddPreset
    //	extends MenuAction
    //	{
    //		protected ActionAddPreset( String name, KeyStroke acc )
    //		{
    //			super( name, acc );
    //		}
    //
    //		public void actionPerformed( ActionEvent e )
    //		{
    //			if( threadRunning ) return;	// not while processing
    //
    //			String name = JOptionPane.showInputDialog( getWindow(), app.getResourceString( "procWinEnterPresetName" ));
    //			if( name != null && name.length() > 0 ) {
    //				name = name.replace( '.', ' ' ); // period not allowed, as we use it as menu node id
    //				if( name.equals( Presets.DEFAULT )) {
    //					JOptionPane.showMessageDialog( getWindow(), app.getResourceString( "procWinDefaultPreset" ));
    //					return;
    //				}
    //
    //				for( Iterator iter = getPresets().presetNames().iterator(); iter.hasNext(); ) {
    //					if( name.equals( iter.next() )) {
    //						if( JOptionPane.showConfirmDialog( getWindow(), name + ":\n"+
    //								app.getResourceString( "procWinOverwritePreset" ),
    //								app.getResourceString( "optionDlgConfirm" ),
    //								JOptionPane.YES_NO_OPTION ) != 0 ) return;	// abort
    //					}
    //				}
    //
    //				addPreset( name );
    //			}
    //		}
    //	}
    //
    //	private class ActionDeletePreset
    //	extends MenuAction
    //	{
    //		protected ActionDeletePreset( String name, KeyStroke acc )
    //		{
    //			super( name, acc );
    //		}
    //
    //		public void actionPerformed( ActionEvent e )
    //		{
    //			if( threadRunning ) return;	// not while processing
    //
    //			final List presetNames	= getPresets().presetNames();
    //			presetNames.remove( Presets.DEFAULT );
    //			final JList list = new JList( presetNames.toArray() );
    //			final JScrollPane scroll = new JScrollPane( list );
    //			final JOptionPane op = new JOptionPane( scroll, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
    //			final int result = BasicWindowHandler.showDialog( op, getComponent(), app.getResourceString( "procWinChooseDelPreset" ));
    //			if( result == JOptionPane.OK_OPTION ) {
    //				final Object[] selNames = list.getSelectedValues();
    //				for( int i = 0; i < selNames.length; i++ ) {
    //					deletePreset( selNames[ i ].toString() );
    //				}
    //			}
    //		}
    //	}

    private class ActionRecallPreset
    extends MenuAction
    {
        protected ActionRecallPreset( String name, KeyStroke acc )
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