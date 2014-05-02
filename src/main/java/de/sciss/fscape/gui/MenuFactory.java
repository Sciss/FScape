///*
// *  MenuFactory.java
// *  (FScape)
// *
// *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
// *
// *	This software is free software; you can redistribute it and/or
// *	modify it under the terms of the GNU General Public License
// *	as published by the Free Software Foundation; either
// *	version 2, june 1991 of the License, or (at your option) any later version.
// *
// *	This software is distributed in the hope that it will be useful,
// *	but WITHOUT ANY WARRANTY; without even the implied warranty of
// *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// *	General Public License for more details.
// *
// *	You should have received a copy of the GNU General Public
// *	License (gpl.txt) along with this software; if not, write to the Free Software
// *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// *
// *
// *	For further information, please contact Hanns Holger Rutz at
// *	contact@sciss.de
// *
// *
// *  Changelog:
// *		25-Jan-05	created from de.sciss.meloncillo.gui.MenuFactory
// *		02-Aug-05	confirms to new document handler
// *		15-Sep-05	openDocument checks if file is already open
// *		03-Oct-06	created from de.sciss.eisenkraut.gui.MenuFactory
// */
//
//package de.sciss.fscape.gui;
//
//import java.awt.Component;
//import java.awt.FileDialog;
//import java.awt.Frame;
//import java.awt.Toolkit;
//import java.awt.event.ActionEvent;
//import java.awt.event.InputEvent;
//import java.awt.event.KeyEvent;
//import java.io.File;
//import java.io.IOException;
//import javax.swing.Action;
//import javax.swing.KeyStroke;
//
//// FFFF
////import de.sciss.eisenkraut.Main;
////import de.sciss.eisenkraut.io.AudioTrail;
////import de.sciss.eisenkraut.io.CacheManager;
////import de.sciss.eisenkraut.io.DecimatedTrail;
////import de.sciss.eisenkraut.net.SuperColliderClient;
////import de.sciss.eisenkraut.realtime.Transport;
////import de.sciss.eisenkraut.session.Session;
////import de.sciss.eisenkraut.util.PrefsUtil;
//import de.sciss.fscape.Main;
//import de.sciss.fscape.prop.BasicProperties;
//import de.sciss.fscape.session.ModulePanel;
//import de.sciss.fscape.session.DocumentHandler;
//import de.sciss.fscape.session.Session;
//import de.sciss.fscape.util.PrefsUtil;
//
//import de.sciss.gui.GUIUtil;
//import de.sciss.gui.MenuAction;
//import de.sciss.gui.MenuGroup;
//import de.sciss.gui.MenuItem;
//import de.sciss.gui.StringItem;
//import de.sciss.util.Flag;
//
///**
// *  <code>JMenu</code>s cannot be added to more than
// *  one frame. Since on MacOS there's one
// *  global menu for all the application windows
// *  we need to 'duplicate' a menu prototype.
// *  Synchronizing all menus is accomplished
// *  by using the same action objects for all
// *  menu copies. However when items are added
// *  or removed, synchronization needs to be
// *  performed manually. That's the point about
// *  this class.
// *  <p>
// *  There can be only one instance of <code>MenuFactory</code>
// *  for the application, and that will be created by the
// *  <code>Main</code> class.
// *
// *  @author		Hanns Holger Rutz
// *  @version	0.72, 18-Feb-10
// *
// *  @see	de.sciss.eisenkraut.Main#menuFactory
// */
//public class MenuFactory
//extends BasicMenuFactory
//{
//	// ---- misc actions ----
//	private ActionOpen				actionOpen;
////	private actionOpenMMClass			actionOpenMM;
////	private actionNewEmptyClass			actionNewEmpty;
//
////	private final List					collGlobalKeyCmd	= new ArrayList();
//
////	private static final String			CLIENT_BG	= "de.sciss.gui.BG";	// radio button group
//
//	// -------------------- modules
//	private static final String M_MOD_UTIL		= "Utilities";
//	private static final String MI_CHANGEGAIN	= "Change Gain";
//	private static final String MI_CHANMGR		= "Channel Manager";
//	private static final String MI_CONCAT		= "Concat";
//	private static final String MI_SPLICE		= "Splice";
//	private static final String MI_MAKELOOP		= "Make Loop";
//	private static final String MI_STATISTICS	= "Statistics";
//	private static final String MI_UNARYOP		= "Unary Operator";
//	private static final String MI_BINARYOP		= "Binary Operator";
//	private static final String MI_AMPSHAPER	= "Amplitude Shaper";
//	private static final String MI_RESAMPLE		= "Resample";
//
//	private static final String M_MOD_SPECT 	= "Spectral Domain";
//	private static final String MI_FIRDESIGN	= "FIR Designer";
//	private static final String MI_CONVOLVE		= "Convolution";
//	private static final String MI_COMPLEXCONV	="Complex Convolution";
//	private static final String MI_FOURIER		= "Fourier Translation";
//	private static final String MI_TRANSL		= "Wavelet Translation";
//	private static final String MI_HILBERT		= "Hilbert Filter";
//	private static final String MI_MOSAIC		= "Mosaic";
//	private static final String MI_SONAGRAMEXP	= "Sonagram Export";
//	private static final String MI_BLEACH		= "Bleach";
//
//	private static final String M_MOD_TIME		= "Time Domain";
//	private static final String MI_STEPBACK		= "Step Back";
//	private static final String MI_KRIECH		= "Kriechstrom";
//	private static final String MI_SERIAL		= "Serial Killa";
//	private static final String MI_SEEKENJOY	= "Seek + Enjoy";
//	private static final String MI_LUECKENBUESSER = "L\u00FCckenb\u00FC\u00DFer";
//	private static final String MI_PEARSONPLOT	="Pearson Plotter";
//	private static final String MI_SEDIMENT		="Sediment";
//	private static final String MI_DRMURKE		="Dr Murke";
//
//	private static final String M_MOD_BETA		= "Unfinished";
//	private static final String MI_SMPSYN		= "Sample Synthesis";
//	private static final String MI_TRANSEXTR	= "Transient Extractor";
//	private static final String MI_LPFILTER		= "Linear Predictive Filtering";
//
//	private static final String M_MOD_BANDS 	= "Multiband";
//	private static final String MI_BANDSPLIT	= "Band Splitting";
//	private static final String MI_CHEBYCHEV	= "Chebychev Waveshaping";
//	private static final String MI_EXCITER		= "Exciter";
//	private static final String MI_VOOCOODER	= "Voocooder";
//
//	private static final String M_MOD_DIST		= "Distortion";
//	private static final String MI_FREQMOD		= "Frequency Modulation";
//	private static final String MI_LAGUERRE		= "Laguerre Warping";
//	private static final String MI_ICHNEUMON	= "Ichneumon";
//	private static final String MI_NEEDLEHOLE	= "Needlehole Cherry Blossom";
//	private static final String MI_ROTATION		= "Rotation";
//	private static final String MI_SEEKDESTROY	="Seek + Destroy";
//	private static final String MI_BLUNDERFINGER ="BlunderFinger";
//
//	private static final String M_MOD_REST		= "Restauration";
//	private static final String MI_COMBFLT		= "Comb Filter";
//	private static final String MI_DECLICK		= "Declick";
//	private static final String MI_SCHIZO		= "Schizophrenia";
//
//	private static final String M_MOD_MISC		= "Miscellaneous";
//	private static final String MI_SACHENSUCHER = "Sachensucher";
//	private static final String MI_RECYCLE		= "Recycle Or Die";
//	private static final String MI_CONVERT		= "Convert Files";
//	private static final String MI_CDVERIFY		= "CD Verification";
//	private static final String MI_SMPTESYNTH	= "SMPTE Synthesizer";
//	private static final String MI_BELTRAMI		= "Beltrami Decomposition";
//
//	private static final String MI_BATCH		= "Batch Processor";
//	private static final String MI_SPECTPATCH	= "Spectral Patcher";
////	private static final String MI_COLLAGE		= "&3Collage";
////	private static final String MI_EXTERNAL		= "External...";
////	private static final String MI_CURVE		= "Edit Curve";
//
//	private static final int PROC_MODIF   = InputEvent.CTRL_MASK |
//		(Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == InputEvent.CTRL_MASK ? InputEvent.ALT_MASK : 0);
//		// on windows, use ctrl alt to avoid confusion with normal accelerators
//
//	private static final Object[][][] mSub = {
//	{
//		{ M_MOD_UTIL },
//		{ new StringItem( "ChangeGain",		MI_CHANGEGAIN ),	KeyStroke.getKeyStroke( KeyEvent.VK_N, PROC_MODIF ) },
//		{ new StringItem( "ChannelMgr",		MI_CHANMGR ),		KeyStroke.getKeyStroke( KeyEvent.VK_C, PROC_MODIF ) },
//		{ new StringItem( "Concat",			MI_CONCAT ),		KeyStroke.getKeyStroke( KeyEvent.VK_T, PROC_MODIF ) },
//		{ new StringItem( "Splice",			MI_SPLICE ),		null },
//		{ new StringItem( "MakeLoop",		MI_MAKELOOP ),		null },
//		{ new StringItem( "Statistics",		MI_STATISTICS ),	KeyStroke.getKeyStroke( KeyEvent.VK_I, PROC_MODIF ) },
//		{ new StringItem( "UnaryOp",		MI_UNARYOP ),		KeyStroke.getKeyStroke( KeyEvent.VK_1, PROC_MODIF ) },
//		{ new StringItem( "BinaryOp",		MI_BINARYOP ),		KeyStroke.getKeyStroke( KeyEvent.VK_2, PROC_MODIF ) },
//		{ new StringItem( "AmpShaper",		MI_AMPSHAPER ),		KeyStroke.getKeyStroke( KeyEvent.VK_A, PROC_MODIF ) },
//		{ new StringItem( "Resample",		MI_RESAMPLE ),		KeyStroke.getKeyStroke( KeyEvent.VK_P, PROC_MODIF ) }
//	},{
//		{ M_MOD_SPECT },
//		{ new StringItem( "FIRDesigner",	MI_FIRDESIGN ),		KeyStroke.getKeyStroke( KeyEvent.VK_R, PROC_MODIF ) },
//		{ new StringItem( "Convolution",	MI_CONVOLVE ),		KeyStroke.getKeyStroke( KeyEvent.VK_L, PROC_MODIF ) },
//		{ new StringItem( "ComplexConv",	MI_COMPLEXCONV ),	null },
//		{ new StringItem( "Fourier",		MI_FOURIER ),		KeyStroke.getKeyStroke( KeyEvent.VK_F, PROC_MODIF ) },
//		{ new StringItem( "Wavelet",		MI_TRANSL ),		KeyStroke.getKeyStroke( KeyEvent.VK_W, PROC_MODIF ) },
//		{ new StringItem( "Hilbert",		MI_HILBERT ),		KeyStroke.getKeyStroke( KeyEvent.VK_H, PROC_MODIF ) },
//		{ new StringItem( "Mosaic",			MI_MOSAIC ),		null },
//		{ new StringItem( "SonagramExport",	MI_SONAGRAMEXP ),	null },
//		{ new StringItem( "Bleach",			MI_BLEACH ),		null }
//	},{
//		{ M_MOD_BANDS },
//		{ new StringItem( "BandSplit",		MI_BANDSPLIT ),		KeyStroke.getKeyStroke( KeyEvent.VK_S, PROC_MODIF ) },
//		{ new StringItem( "Chebychev",		MI_CHEBYCHEV ),		null },
//		{ new StringItem( "Exciter",		MI_EXCITER ),		null },
//		{ new StringItem( "Voocooder",		MI_VOOCOODER ),		KeyStroke.getKeyStroke( KeyEvent.VK_V, PROC_MODIF ) }
//	},{
//		{ M_MOD_TIME },
//		{ new StringItem( "StepBack",		MI_STEPBACK ),		KeyStroke.getKeyStroke( KeyEvent.VK_B, PROC_MODIF ) },
//		{ new StringItem( "Kriechstrom",	MI_KRIECH ),		KeyStroke.getKeyStroke( KeyEvent.VK_K, PROC_MODIF ) },
//		{ new StringItem( "SerialKilla",	MI_SERIAL ),		null },
//		{ new StringItem( "SeekEnjoy",		MI_SEEKENJOY ),		null },
//		{ new StringItem( "Lueckenbuesser", MI_LUECKENBUESSER ),null },
//		{ new StringItem( "PearsonPlot",	MI_PEARSONPLOT ),	null },
//		{ new StringItem( "Sediment",		MI_SEDIMENT ),		null },
//		{ new StringItem( "DrMurke",		MI_DRMURKE),		null }
//	},{
//		{ M_MOD_DIST },
//		{ new StringItem( "FreqMod",		MI_FREQMOD ),		KeyStroke.getKeyStroke( KeyEvent.VK_M, PROC_MODIF ) },
//		{ new StringItem( "Laguerre",		MI_LAGUERRE ),		KeyStroke.getKeyStroke( KeyEvent.VK_G, PROC_MODIF ) },
//		{ new StringItem( "Ichneumon",		MI_ICHNEUMON ),		KeyStroke.getKeyStroke( KeyEvent.VK_U, PROC_MODIF ) },
//		{ new StringItem( "Needlehole",		MI_NEEDLEHOLE ),	null },
//		{ new StringItem( "Rotation",		MI_ROTATION ),		null },
//		{ new StringItem( "SeekDestroy",	MI_SEEKDESTROY ),	null },
//		{ new StringItem( "BlunderFinger",	MI_BLUNDERFINGER ),	null }
//	},{
//		{ M_MOD_MISC },
//		{ new StringItem( "Sachensucher",	MI_SACHENSUCHER ),	null },
//		{ new StringItem( "Recycle",		MI_RECYCLE ),		null },
//		{ new StringItem( "Convert",		MI_CONVERT ),		null },
//		{ new StringItem( "CDVerify",		MI_CDVERIFY ),		null },
//		{ new StringItem( "SMPTESynth",		MI_SMPTESYNTH ),	null },
//		{ new StringItem( "BeltramiDecomposition", MI_BELTRAMI ), null }
//	},{
//		{ M_MOD_REST },
//		{ new StringItem( "CombFilter",		MI_COMBFLT ),		null },
//		{ new StringItem( "Declick",		MI_DECLICK ),		null },
//		{ new StringItem( "Schizo",			MI_SCHIZO ),		null }
//	},{
//		{ M_MOD_BETA },
//		{ new StringItem( "SmpSyn",			MI_SMPSYN ),		null },
//		{ new StringItem( "TransientExtr",	MI_TRANSEXTR ),		null },
//		{ new StringItem( "LPFilter",		MI_LPFILTER ),		null }
//	},{
//		{},
//		{ new StringItem( "Batch",			MI_BATCH ),			KeyStroke.getKeyStroke( KeyEvent.VK_9, PROC_MODIF ) },
//		{ new StringItem( "SpectPatch",		MI_SPECTPATCH ),	KeyStroke.getKeyStroke( KeyEvent.VK_3, PROC_MODIF ) }
//	}};
//
//	/**
//	 *  The constructor is called only once by
//	 *  the <code>Main</code> class and will create a prototype
//	 *  main menu from which all copies are
//	 *  derived.
//	 *
//	 *  @param  root	application root
//	 */
//	public MenuFactory( BasicApplication app )
//	{
//		super( app );
//		createActions();
//	}
//
//	public ProcessingThread closeAll( boolean force, Flag confirmed )
//	{
//		final de.sciss.app.DocumentHandler	dh	= AbstractApplication.getApplication().getDocumentHandler();
//		Session								doc;
//		ProcessingThread					pt;
//
//		while( dh.getDocumentCount() > 0 ) {
//			doc	= (Session) dh.getDocument( 0 );
//if( doc.getFrame() == null ) {
////	System.err.println( "Yukk, no doc frame for "+doc.getDisplayDescr().file );
//	System.err.println( "Yukk, no doc frame for "+doc.getName() );
//	try {
//		Thread.sleep( 4000 );
//	} catch( InterruptedException e1 ) {}
//	confirmed.set( true );
//	return null;
//}
//			pt	= doc.getFrame().closeDocument( force, confirmed );
//			if( pt == null ) {
//				if( !confirmed.isSet() ) return null;
//			} else {
//				return pt;
//			}
//		}
//		confirmed.set( true );
//		return null;
//	}
//
//	/**
//	 *  Sets all JMenuBars enabled or disabled.
//	 *  When time taking asynchronous processing
//	 *  is done, like loading a session or bouncing
//	 *  it to disk, the menus need to be disabled
//	 *  to prevent the user from accidentally invoking
//	 *  menu actions that can cause deadlocks if they
//	 *  try to gain access to blocked doors. This
//	 *  method traverses the list of known frames and
//	 *  sets each frame's menu bar enabled or disabled.
//	 *
//	 *  @param  enabled		<code>true</code> to enable
//	 *						all menu bars, <code>false</code>
//	 *						to disable them.
//	 *  @synchronization	must be called in the event thread
//	 */
////	public void setMenuBarsEnabled( boolean enabled )
////	{
////		MenuHost	host;
////		JMenuBar	mb;
////
////		for( int i = 0; i < collMenuHosts.size(); i++ ) {
////			host	= (MenuHost) collMenuHosts.get( i );
////			mb		= host.who.getJMenuBar();
////			if( mb != null ) mb.setEnabled( enabled );
////		}
////	}
//
////	private static int uniqueNumber = 0;	// increased by addGlobalKeyCommand()
//	/**
//	 *  Adds an action object invisibly to all
//	 *  menu bars, enabling its keyboard shortcut
//	 *  to be accessed no matter what window
//	 *  has the focus.
//	 *
//	 *  @param  a   the <code>Action</code> whose
//	 *				accelerator key should be globally
//	 *				accessible. The action
//	 *				is stored in the input and action map of each
//	 *				registered frame's root pane, thus being
//	 *				independant of calls to <code>setMenuBarsEnabled/code>.
//	 *
//	 *  @throws java.lang.IllegalArgumentException  if the action does
//	 *												not have an associated
//	 *												accelerator key
//	 *
//	 *  @see  javax.swing.Action#ACCELERATOR_KEY
//	 *  @synchronization	must be called in the event thread
//	 */
////	public void addGlobalKeyCommand( Action a )
////	{
////System.err.println( "addGlobalKeyCommand : NOT YET FULLY WORKING" );
////		final KeyStroke		acc		= (KeyStroke) a.getValue( Action.ACCELERATOR_KEY );
////		final String		entry;
////		MenuHost			host;
//////		JRootPane			rp;
////		InputMap			imap;
////		ActionMap			amap;
////
////		if( acc == null ) throw new IllegalArgumentException();
////
////		entry = "key" + String.valueOf( uniqueNumber++ );
////		a.putValue( Action.NAME, entry );
////
////		for( int i = 0; i < collMenuHosts.size(); i++ ) {
////			host	= (MenuHost) collMenuHosts.get( i );
////			imap	= host.who.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
//////			rp		= host.who.getRootPane();
//////			imap	= rp.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
//////			amap	= rp.getActionMap();
////			amap	= host.who.getActionMap();
////			imap.put( acc, entry );
////			amap.put( entry, a );
////		}
////		collGlobalKeyCmd.add( a );
////	}
//
//	private void createActions()
//	{
////		// --- file menu ---
////		actionNewEmpty	= new actionNewEmptyClass( getResourceString( "menuNewEmpty" ),
////												KeyStroke.getKeyStroke( KeyEvent.VK_N, MENU_SHORTCUT ));
//		actionOpen		= new ActionOpen(  getResourceString( "menuOpen" ),
//												KeyStroke.getKeyStroke( KeyEvent.VK_O, MENU_SHORTCUT ));
////		actionOpenMM	= new actionOpenMMClass( getResourceString( "menuOpenMM" ),
////												KeyStroke.getKeyStroke( KeyEvent.VK_O, MENU_SHORTCUT + KeyEvent.SHIFT_MASK ));
//	}
//
//	// @todo	this should eventually read the tree from an xml file
//	protected void addMenuItems()
//	{
//		MenuGroup				mg, smg, smg2;
//		int						i;
//
//		// --- file menu ---
//
//		mg	= (MenuGroup) this.get( "file" );
//		smg = new MenuGroup( "new", getResourceString( "menuNew" ));
//		for( int j = 0; j < mSub.length; j++ ) {
//			if( j + 1 < mSub.length ) {
//				smg2	= new MenuGroup( mSub[ j ][ 0 ][ 0 ].toString(), mSub[ j ][ 0 ][ 0 ].toString() ); // XXX use resource strings
//			} else {
//				smg2	= smg;
//			}
//			for( int k = 1; k < mSub[ j ].length; k++ ) {
//				smg2.add( new MenuItem( ((StringItem) mSub[ j ][ k ][ 0 ]).getKey(),
//					new ActionModule( (StringItem) mSub[ j ][ k ][ 0 ], (KeyStroke) mSub[ j ][ k ][ 1 ])));
//			}
//			if( smg2 != smg ) {
//				smg.add( smg2 );
//			}
//		}
//		mg.add( smg, 0 );
//
//		// --- window menu ---
//		mg	= (MenuGroup) this.get( "window" );
//		mg.add( new MenuItem( "main", new ActionShowWindow( getResourceString( "frameMain" ), null, Main.COMP_MAIN )), 0 );
//
//		// --- presets menu ---
//		mg   = new MenuGroup( "presets", getResourceString( "menuPresets" ));
////		mg.setEnabled( false );
//		i	= this.indexOf( "window" );
//		this.add( mg, i );
//
//		// --- debug menu ---
//		mg   = new MenuGroup( "debug", "Debug" );
//		mg.add( new MenuItem( "dumpPrefs", PrefsUtil.getDebugDumpAction() ));
//		i	= this.indexOf( "help" );
//		this.add( mg, i );
//
//		mg = (MenuGroup) this.get( "help" );
//		mg.add( new MenuItem( "module", getResourceString( "menuHelpModule" )), mg.indexOf( "shortcuts" ));
//	}
//
//	public void showPreferences()
//	{
//		PrefsFrame prefsFrame = (PrefsFrame) getApplication().getComponent( Main.COMP_PREFS );
//
//		if( prefsFrame == null ) {
//			prefsFrame = new PrefsFrame();
//		}
//		prefsFrame.setVisible( true );
//		prefsFrame.toFront();
//	}
//
//	protected Action getOpenAction()
//	{
//		return actionOpen;
//	}
//
//	public void openDocument( File f )
//	{
////		actionOpen.perform( f );
//		openDocument( f, true, null );
//	}
//
//// FFFF
//	public void openDocument( File f, boolean visible, DocumentHandler.OpenDoneHandler openDoneHandler )
//	{
//		actionOpen.perform( f, visible, openDoneHandler );
//	}
//
//// FFFF
////	public void openDocument( File[] fs )
////	{
////		actionOpenMM.perform( fs );
////	}
//
//// FFFF
////	public Session newDocument( AudioFileDescr afd )
////	{
////		return actionNewEmpty.perform( afd );
////	}
//
//	public void newDocument( String className )
//	{
//		final Class				clz;
////		final Constructor		cons;
//		final ModulePanel procWin;
////		final Application		app		= AbstractApplication.getApplication();
//
//		try {
//			clz		= Class.forName( "de.sciss.fscape.gui." + className + "Dlg" );
////			cons	= clz.getConstructor( new Class[] { Main.class });
////			procWin	= (DocumentFrame) cons.newInstance( new Object[] { root });
//			procWin	= (ModulePanel) clz.newInstance();
//			getApplication().getDocumentHandler().addDocument( this, procWin.getDocument() );
//			procWin.setVisible( true );
//			// procWin.toFront();
//		} catch( Exception e1 ) {
//			GUIUtil.displayError( null, e1, getResourceString( "menuNew" ));
//		}
//	}
//
//	public void addSCPlugIn( Action a, String[] hierarchy )
//	{
//System.err.println( "addSCPlugIn : NOT YET WORKING" );
////		final JMenuItem mi = new JMenuItem( a );
//////		sma.setProtoType( rbmi );
//////		rbmi.putClientProperty( CLIENT_BG, CLIENT_BG + "window" );
////		addMenuItem( mSuperCollider, mi );	// XXX traverse hierarchy
//	}
//
//	public void removeSCPlugIn( Action a )
//	{
//System.err.println( "removeSCPlugIn : NOT YET WORKING" );
////		removeMenuItem( mSuperCollider, a );
//	}
//
//	protected Session findDocumentForPath( File f )
//	{
//		final de.sciss.app.DocumentHandler	dh	= AbstractApplication.getApplication().getDocumentHandler();
//		Session								doc;
//// FFFF
////		AudioFileDescr[]					afds;
//
//		for( int i = 0; i < dh.getDocumentCount(); i++ ) {
//			doc		= (Session) dh.getDocument( i );
//// FFFF
////			afds	= doc.getDescr();
////			for( int j = 0; j < afds.length; j++ ) {
////				if( (afds[ j ].file != null) && afds[ j ].file.equals( f )) {
////					return doc;
////				}
////			}
//if( f.equals( doc.getFile() )) return doc;
//		}
//		return null;
//	}
//
//// ---------------- Action objects for file (session) operations ----------------
//
//// FFFF
////	// action for the New-Empty Document menu item
////	private class actionNewEmptyClass
////	extends MenuAction
////	{
////		private JPanel				p	= null;
////		private AudioFileFormatPane	affp;
////
////		private actionNewEmptyClass( String text, KeyStroke shortcut )
////		{
////			super( text, shortcut );
////		}
////
////		public void actionPerformed( ActionEvent e )
////		{
////			final AudioFileDescr afd = query();
////			if( afd != null ) perform( afd );
////		}
////
////		private AudioFileDescr query()
////		{
////			final AudioFileDescr		afd			= new AudioFileDescr();
////			final JOptionPane			dlg;
////			final String[]				queryOptions = { getResourceString( "buttonCreate" ),
////														 getResourceString( "buttonCancel" )};
////			final int					result;
////			final Component				c			= ((AbstractWindow) root.getComponent( Main.COMP_MAIN )).getWindow();
////
////			if( p == null ) {
//////				final SpringPanel		msgPane;
//////				final Box				b			= Box.createHorizontalBox();
////
//////				msgPane		= new SpringPanel( 4, 2, 4, 2 );
////				affp		= new AudioFileFormatPane( AudioFileFormatPane.NEW_FILE_FLAGS );
//////				msgPane.gridAdd( new JLabel( getResourceString( "labelFormat" )), 0, 0 );
//////				msgPane.gridAdd( affp, 1, 0 );
//////				msgPane.makeCompactGrid();
//////				GUIUtil.setDeepFont( msgPane, GraphicsUtil.smallGUIFont );
//////				p.add( msgPane, BorderLayout.NORTH );
//////				b.add( new JLabel( getResourceString( "labelFormat" )));
//////				b.add( Box.createHorizontalStrut( 8 ));
//////				b.add( affp );
////
////				p			= new JPanel( new BorderLayout() );
////				p.add( affp, BorderLayout.NORTH );
////				AbstractWindowHandler.setDeepFont( affp );
////			}
////
////			result		= JOptionPane.showOptionDialog( c, p, getValue( NAME ).toString(),
////														JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
////														null, queryOptions, queryOptions[ 0 ]);
////
////			if( result == 0 ) {
////				affp.toDescr( afd );
////				return afd;
////			} else {
////				return null;
////			}
////		}
////
////		private Session perform( AudioFileDescr afd )
////		{
////			final Session doc;
////
////			try {
////				doc = Session.newEmpty( afd, true );
////				AbstractApplication.getApplication().getDocumentHandler().addDocument( this, doc );
////				doc.createFrame();
////				return doc;
////			}
////			catch( IOException e1 ) {	// should never happen
////				GUIUtil.displayError( null, e1, getValue( Action.NAME ).toString() );
////				return null;
////			}
////		}
////	}
//
//	// action for the Open-Session menu item
//	private class ActionOpen
//	extends MenuAction
//	implements ProcessingThread.Client
//	{
//		protected ActionOpen( String text, KeyStroke shortcut )
//		{
//			super( text, shortcut );
//
////			this.text = text;
//		}
//
//		/*
//		 *  Open a Session. If the current Session
//		 *  contains unsaved changes, the user is prompted
//		 *  to confirm. A file chooser will pop up for
//		 *  the user to select the session to open.
//		 */
//		public void actionPerformed( ActionEvent e )
//		{
//			File f = queryFile();
//			if( f != null ) perform( f, true, null );
//		}
//
//		private File queryFile()
//		{
//			FileDialog  fDlg;
//			String		strFile, strDir;
//			final		Component	ownerF	= ((AbstractWindow) getApplication().getComponent( Main.COMP_MAIN )).getWindow();
//			final		boolean		makeF	= !(ownerF instanceof Frame);
//			final		Frame		f		= makeF ? new Frame() : (Frame) ownerF;
//
//			fDlg	= new FileDialog( f, getResourceString( "fileDlgOpen" ), FileDialog.LOAD );
////			fDlg.setFilenameFilter( doc );
//			// fDlg.setDirectory();
//			// fDlg.setFile();
//			fDlg.setVisible( true );
//			if( makeF ) f.dispose();
//			strDir	= fDlg.getDirectory();
//			strFile	= fDlg.getFile();
//
//			if( strFile == null ) return null;   // means the dialog was cancelled
//
//			return( new File( strDir, strFile ));
//		}
//
//		/**
//		 *  Loads a new document file.
//		 *  a <code>ProcessingThread</code>
//		 *  started which loads the new session.
//		 *
//		 *  @param  path	the file of the document to be loaded
//		 *
//		 *  @synchronization	this method must be called in event thread
//		 */
////		private ProcessingThread perform( File path )
//		protected void perform( File path, boolean visible, DocumentHandler.OpenDoneHandler openDoneHandler )
//		{
//			final ProcessingThread pt;
//
//			// check if the document is already open
//			final Session docOld = findDocumentForPath( path );
//			if( docOld != null ) {
//				if( visible ) {
//					docOld.getFrame().setVisible( true );
//					// docOld.getFrame().toFront();
//				}
////				return null;
//				if( openDoneHandler != null ) openDoneHandler.openSucceeded( docOld );
//				return;
//			}
//
////			try {
////				args[ 0 ]	= path; // AudioFile.openAsRead( path );
////				args[ 1 ]	= new Boolean( visible );
////				pt			= new ProcessingThread( this, new ProgressMonitor( null,
////								getResourceString( "labelOpening" ) + " " + path.getName() + " ..." ), root, null, text, args, 0 );
////				pt.start();
//
//// MUST BE CREATED IN THE SWING THREAD!! XXX
//
//pt = new ProcessingThread( this, null, getValue( NAME ).toString() );
//pt.putClientArg( "path", path );
//pt.putClientArg( "visi", new Boolean( visible ));
//final int result = processRun( pt );
//if( result == DONE ) {
//	processFinished( pt );
//	final Session docNew = (Session) pt.getClientArg( "doc" );
//	if( openDoneHandler != null ) openDoneHandler.openSucceeded( docNew );
//} else {
////	if( visible ) GUIUtil.displayError( null, (Exception) args[ 3 ], getValue( NAME ).toString() );
//	if( visible ) GUIUtil.displayError( null, pt.getException(), getValue( NAME ).toString() );
//	if( openDoneHandler != null ) openDoneHandler.openFailed();
//}
//
////				return pt;
//				return;
//
////			}
////			catch( IOException e1 ) {
////				GUIUtil.displayError( null, e1, getValue( Action.NAME ).toString() );
////				return null;
////			}
//		}
//
//		public int processRun( ProcessingThread pt )
//		{
////			final Object[]			args	= (Object[]) argument;
////			final File				f		= (File) args[ 0 ];
//			final File				f		= (File) pt.getClientArg( "path" );
//			final Class				clz;
////			final Constructor		cons;
//			final ModulePanel procWin;
////			final Application		app		= AbstractApplication.getApplication();
//			final BasicProperties	preset;
//			String					className;
//
//			// Dateityp ermitteln
//			try {
////				type	= MRJAdapter.getFileType( f );
////				if( type.equals( "FScP" )) { // == Constants.OSTypePrefs ) {
//
//					preset		= new BasicProperties( null, f );
//					preset.load();
//					className	= preset.getProperty( ModulePanel.PROP_CLASS );
//					if( className != null ) {
//						if( className.startsWith( "fscape." )) {	// pre version 0.67
//							className = "de.sciss." + className;
//						}
//						clz			= Class.forName( className );
////						cons		= clz.getConstructor( new Class[] { Main.class });
////						procWin		= (DocumentFrame) cons.newInstance( new Object[] { root });
//						procWin		= (ModulePanel) clz.newInstance();
////						args[ 2 ]	= procWin.getDocument();
//						pt.putClientArg( "doc", procWin.getDocument() );
//						procWin.loadFile( f );
//						return DONE;
//					} else {
////						context.setException( new IOException( "Wrong file format. Missing entry '" + DocumentFrame.PROP_CLASS + "'" ));
////						args[ 3 ] = new IOException( "Wrong file format. Missing entry '" + DocumentFrame.PROP_CLASS + "'" );
//						pt.setException( new IOException( "Wrong file format. Missing entry '" + ModulePanel.PROP_CLASS + "'" ));
//						return FAILED;
//					}
//
////				} else if( type.equals( "FScD" )) { // == Constants.OSTypeDoc ) {
////					// XXX
////	//				e.setHandled( true );
////					return FAILED;
////				}
//			}
//			catch( Exception e1 ) {
////				context.setException( e1 );
////				args[ 3 ] = e1;
//				pt.setException( e1 );
//				return FAILED;
//			}
//		} // run()
//
//		// nothing to do, mte will check pt state
//		public void processCancel( ProcessingThread pt ) {}
//
//		/**
//		 *  When the session was successfully
//		 *  loaded, its name will be put in the
//		 *  Open-Recent menu. All frames' bounds will be
//		 *  restored depending on the users preferences.
//		 *  <code>setModified</code> will be called on
//		 *  the <code>Main</code> class and the
//		 *  main frame's title is updated
//		 */
//		public void processFinished( ProcessingThread pt )
//		{
////			if( context.getReturnCode() == DONE ) {
////				final Object[]			args	= (Object[]) argument;
////				final File				f		= (File) args[ 0 ];
//				final File				f		= (File) pt.getClientArg( "path" );
////				final boolean			visible	= ((Boolean) args[ 1 ]).booleanValue();
//				final boolean			visible	= ((Boolean) pt.getClientArg( "visi" )).booleanValue();
////				final Session			doc		= (Session) args[ 2 ];
//				final Session			doc		= (Session) pt.getClientArg( "doc" );
//				final ModulePanel procWin	= doc.getFrame();
//
//				AbstractApplication.getApplication().getDocumentHandler().addDocument( this, doc );
//				addRecent( f );
////System.err.println( "visible = "+visible );
//				if( visible ) {
//					procWin.setVisible( true );
//					// procWin.toFront();
//				}
////			}
//		}
//	}
///*
//	// action for the Save-Session menu item
//	private class actionCloseAllClass
//	extends MenuAction
//	implements ProcessingThread.Listener
//	{
//		private actionCloseAllClass( String text, KeyStroke shortcut )
//		{
//			super( text, shortcut );
//			setEnabled( false );	// initially no docs open
//		}
//
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//
//		private void perform()
//		{
//			final ProcessingThread pt = closeAll( false, new Flag( false ));
//			if( pt != null ) {
//				pt.addListener( this );	// ok, let's save the shit and re-try to close all after that
//				pt.start();
//			}
//		}
//
//		public void processStarted( ProcessingThread.Event e ) {}
//
//		// if the saving was successfull, we will call closeAll again
//		public void processStopped( ProcessingThread.Event e )
//		{
//			if( e.isDone() ) {
//				perform();
//			}
//		}
//	}
//*/
//// ---------------- Action objects for window operations ----------------
//
//// FFFF
////	// action for the IOSetup menu item
////	private class actionIOSetupClass
////	extends MenuAction
////	{
////		private actionIOSetupClass( String text, KeyStroke shortcut )
////		{
////			super( text, shortcut );
////		}
////
////		/**
////		 *  Brings up the IOSetup
////		 */
////		public void actionPerformed( ActionEvent e )
////		{
////			IOSetupFrame f = (IOSetupFrame) root.getComponent( Main.COMP_IOSETUP );
////
////			if( f == null ) {
////				f = new IOSetupFrame();		// automatically adds component
////			}
////			f.setVisible( true );
////			f.toFront();
////		}
////	}
////
////	// action for the Control Room menu item
////	private class actionCtrlRoomClass
////	extends MenuAction
////	{
////		private actionCtrlRoomClass( String text, KeyStroke shortcut )
////		{
////			super( text, shortcut );
////		}
////
////		/**
////		 *  Brings up the IOSetup
////		 */
////		public void actionPerformed( ActionEvent e )
////		{
////			ControlRoomFrame f = (ControlRoomFrame) root.getComponent( Main.COMP_CTRLROOM );
////
////			if( f == null ) {
////				f = new ControlRoomFrame();	// automatically adds component
////			}
////			f.setVisible( true );
////			f.toFront();
////		}
////	}
////
////	// action for the Observer menu item
////	private class actionObserverClass
////	extends MenuAction
////	{
////		private actionObserverClass( String text, KeyStroke shortcut )
////		{
////			super( text, shortcut );
////		}
////
////		/**
////		 *  Brings up the IOSetup
////		 */
////		public void actionPerformed( ActionEvent e )
////		{
////			ObserverPalette f = (ObserverPalette) root.getComponent( Main.COMP_OBSERVER );
////
////			if( f == null ) {
////				f = new ObserverPalette();	// automatically adds component
////			}
////			f.setVisible( true );
////			f.toFront();
////		}
////	}
//
//	private class ActionModule
//	extends MenuAction
//	{
//		private final String className;
//
//		protected ActionModule( StringItem module, KeyStroke shortcut )
//		{
//			super( module.toString(), shortcut );
//
//			this.className = module.getKey();
//		}
//
//		public void actionPerformed( ActionEvent e )
//		{
//			newDocument( className );
//		}
//	}
//}