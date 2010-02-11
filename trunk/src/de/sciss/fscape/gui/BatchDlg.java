/*
 *  BatchDlg.java
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
 */

package de.sciss.fscape.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultFormatter;

import de.sciss.app.AbstractApplication;
import de.sciss.app.DocumentHandler;
import de.sciss.gui.GUIUtil;
import de.sciss.util.Flag;

import de.sciss.fscape.proc.ProcessorEvent;
import de.sciss.fscape.proc.ProcessorListener;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.session.Session;
import de.sciss.fscape.util.Util;

/**
 *  Module for batch processing
 *	a list of other modules.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.73, 27-Jun-09
 */
public class BatchDlg
extends DocumentFrame
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_BATCH			= 0;		// pr.text
//	private static final int PR_OUTPUTTYPE		= 0;		// pr.intg
//	private static final int PR_GAIN			= 0;		// pr.para
	private static final int PR_VISIBLE			= 0;		// pr.bool
	private static final int PR_CONSOLE			= 1;

	private static final String PRN_BATCH			= "Batch";
	private static final String PRN_VISIBLE			= "Visible";
	private static final String PRN_CONSOLE			= "Console";

	private static final String	prText[]		= { "" };
	private static final String	prTextName[]	= { PRN_BATCH };
//	private static final int		prIntg[]	= { 0, 0, GAIN_UNITY };
//	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };
//	private static final Param	prPara[]		= { null };
//	private static final String	prParaName[]	= { PRN_GAIN };
	private static final boolean	prBool[]	= { false, false };
	private static final String	prBoolName[]	= { PRN_VISIBLE, PRN_CONSOLE };

	private static final int GG_BATCH			= GG_OFF_OTHER		+ 0;
	private static final int GG_CMDCUT			= GG_OFF_OTHER		+ 1;
	private static final int GG_CMDCOPY			= GG_OFF_OTHER		+ 2;
	private static final int GG_CMDPASTE		= GG_OFF_OTHER		+ 3;
	private static final int GG_CMDOPEN			= GG_OFF_OTHER		+ 4;
	private static final int GG_CMDADD			= GG_OFF_OTHER		+ 5;
//	private static final int GG_CMDS			= GG_OFF_OTHER		+ 8;
//	private static final int GG_ERROR			= GG_OFF_OTHER		+ 9;
//	private static final int GG_ERRSKIP			= GG_OFF_OTHER		+ 10;
	private static final int GG_PARAMS			= GG_OFF_OTHER		+ 11;
//	private static final int GG_ENTRYPARAM		= GG_OFF_OTHER		+ 12;
//	private static final int GG_FILE			= GG_OFF_OTHER		+ 13;
	private static final int GG_BATCHPANE		= GG_OFF_OTHER		+ 14;
	private static final int GG_PARAMSPANE		= GG_OFF_OTHER		+ 15;
	private static final int GG_VISIBLE			= GG_OFF_CHECKBOX	+ PR_VISIBLE;
	private static final int GG_CONSOLE			= GG_OFF_CHECKBOX	+ PR_CONSOLE;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

	private boolean telepathy	= false;

	private static final String ERR_DELETE	= "File was not deleted";

	protected static final String[]	EXCLUDE_DLG	= { "MainFrame", "PrefsDlg", "ParamSettingsDlg", "FileInfoDlg" };

	// Gaga, Batch Table REDO
	protected   BatchTableModel		batchTM;
    protected   ParamTableModel		paramTM;
	private   BatchCellRenderer		batchCR;
	protected   JTable				batchTable;
    private   JTable				paramTable;
	protected List					batchVector;
//	private   Vector				paramVector;
	
	private TableModelListener		tml;
	protected ClipboardOwner			cbo;
	private ProcessorListener		procL;
	
// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public BatchDlg()
	{
		super( "Batch Processor" );
		init2();
	}
	
	protected void buildGUI()
	{

		// einmalig PropertyArray initialisieren
		if( static_pr == null ) {
			static_pr			= new PropertyArray();
			static_pr.text		= prText;
			static_pr.textName	= prTextName;
//			static_pr.intg		= prIntg;
//			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;
//			static_pr.para		= prPara;
//			static_pr.para[ PR_GAIN ]		= new Param( 0.0, Param.DECIBEL_AMP );
//			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- other init --------

		batchVector		= new ArrayList();
//		paramVector		= new Vector();
		new BatchObjectArray(); // need to initialize DataFlavor (getClass() doesn't work in static initializer ??)
		
	// -------- GUI bauen --------

		GridBagConstraints	con;

		JButton			ggCmd;
		JCheckBox		ggVisible, ggConsole;
		JScrollPane		ggBatchPane, ggParamPane;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		final BatchDlg enc_this = this;

		cbo	= new ClipboardOwner() {
			public void lostOwnership( Clipboard clipboard, Transferable contents )
			{
				// XXX
			}
		};
		
		procL = new ProcessorListener() {
			public void processorStarted( ProcessorEvent e ) { teleAction(); }
			public void processorStopped( ProcessorEvent e ) { teleAction(); }
			public void processorPaused( ProcessorEvent e ) { teleAction(); }
			public void processorResumed( ProcessorEvent e ) { teleAction(); }
			public void processorProgress( ProcessorEvent e ) { teleAction(); }

			private void teleAction()
			{
				synchronized( enc_this ) {
					enc_this.notify();
				}
			}
		};
		
		ActionListener	al = 	new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				int				ID	= gui.getItemID( e );
				int				i, j, k;
				int[]			rows;
				BatchObject[]   dup;
				Transferable	t;
				BatchObject		bObj;

				switch( ID ) {
				case GG_CMDOPEN:
					if( batchTable.getSelectedRowCount() != 1 ) break;
					bObj = (BatchObject) batchVector.get( batchTable.getSelectedRow() );
					if( bObj.command != BatchObject.CMD_MODULE ) break;

					DocumentFrame procWin;
					try {
						procWin = getProcInstance( bObj, null );
						procWin.fillGUI();
						procWin.setVisible( true );
					}
					catch( Exception e1 ) {
						GUIUtil.displayError( getComponent(), e1, getTitle() );
					}
					break;
					
				case GG_CMDADD:
					i = batchTable.getSelectedRow() + 1;
					i = i == 0 ? batchVector.size() : i;
					batchTable.clearSelection();
					batchVector.add( i, new BatchObject() );
					batchTM.fireTableRowsInserted( i, i );
					batchTable.setRowSelectionInterval( i, i );
					break;
					
				case GG_CMDCUT:
				case GG_CMDCOPY:
					rows = batchTable.getSelectedRows();
					if( rows.length > 0 ) {
						dup = new BatchObject[ rows.length ];
						for( i = 0; i < rows.length; i++ ) {
							dup[i] = new BatchObject( (BatchObject) batchVector.get( rows[i] ));
						}
						AbstractApplication.getApplication().getClipboard().setContents(
							new BatchObjectArray( dup ), cbo );
							
						if( ID == GG_CMDCUT ) {
							for( boolean finished = false; !finished; ) {
								for( i = 0, j = -1, k = -1; i < rows.length; i++ ) {
									if( rows[i] > j ) {
										j = rows[i];
										k = i;
									}
								}
								if( j >= 0 ) {
									batchVector.remove( j );
									rows[k] = -1;
									batchTM.fireTableRowsDeleted( j, j );
								} else {
									finished = true;
								}
							}
						}
					}
					break;
				
				case GG_CMDPASTE:
		//			System.err.println( "Pastie" );
					i = batchTable.getSelectedRow() + 1;
					i = i == 0 ? batchVector.size() : i;
					try {
						t = AbstractApplication.getApplication().getClipboard().getContents( enc_this );
						if( t != null ) {
							if( t.isDataFlavorSupported( BatchObjectArray.flavor )) {
								dup = (BatchObject[]) t.getTransferData( BatchObjectArray.flavor );
								if( dup.length > 0 ) {
									batchTable.clearSelection();
									for( j = 0, k = i; j < dup.length; j++, k++ ) {
										batchVector.add( k, dup[j] );
									}
									batchTM.fireTableRowsInserted( i, k - 1 );
									batchTable.setRowSelectionInterval( i, k - 1 );
								}
							}
						}
					}
					catch( IllegalStateException e97 ) { /* ignored */}
					catch( IOException e98 ) { /* ignored */}
					catch( UnsupportedFlavorException e99 ) { /* ignored */}
					break;
				}
			}
		};

		tml = new TableModelListener() {
			public void	tableChanged( TableModelEvent e )
			{
				int			i, k;
				BatchObject bObj;
			
				if( e.getSource() == batchTM ) {
					if( e.getType() == TableModelEvent.DELETE ) return;
					k = e.getFirstRow();
					if( k >= 0 && !batchVector.isEmpty() ) {
						bObj = (BatchObject) batchVector.get( k );
						if( (bObj.command == BatchObject.CMD_MODULE) && (bObj.modObj.modClass == null) ) {

//							Iterator		winIter		= MainMenu.getWindows();
//							Object[]		modules		= Util.iterToArray( winIter );
final DocumentHandler dh = AbstractApplication.getApplication().getDocumentHandler();
//final Object[] modules = new Object[ dh.getDocumentCount() ];
final DocumentFrame[] modules = new DocumentFrame[ dh.getDocumentCount() ];
for( int m = 0; m < modules.length; m++ ) {
	modules[ m ] = ((Session) dh.getDocument( m )).getFrame();
}
							String[]		winNames;
							ListDlg			dlg;
							PropertyArray	pa;
							String			str;
							int				winNum		= modules.length;
							List			v;
							
							for( i = 0; i < winNum; i++ ) {
								str = modules[ i ].getClass().getName();
								str	= str.substring( str.lastIndexOf( '.' ) + 1 );
								
								if( (modules[ i ] == enc_this) || Util.isValueInArray( str, EXCLUDE_DLG )) {	// exclude ourself
									winNum--;
									for( int j = i; j < winNum; j++ ) {
										modules[ j ] = modules[ j + 1 ];
									}
									i--;
									continue;
								}
							}
							
							winNames	= new String[ winNum ];
							for( i = 0; i < winNum; i++ ) {
//								winNames[ i ] = ((Frame) modules[ i ]).getTitle();
								winNames[ i ] = modules[ i ].getTitle();
							}
			
							if( winNum > 1 ) {		// only prompt if there's a real choice ;)
								dlg = new ListDlg( getWindow(), "Choose Module", winNames );
								i	= dlg.getList();
							} else {
								i	= winNum - 1;
							}
							if( i < 0 ) return;		// cancel
							bObj.modObj.name		= winNames[ i ];
							((DocumentFrame) modules[ i ]).fillPropertyArray();
							pa						= ((DocumentFrame) modules[ i ]).getPropertyArray();
							bObj.modObj.prParam		= pa.toProperties( true );
							bObj.modObj.modClass	= modules[ i ].getClass().getName();
							v = new ArrayList();
							for( int j = 0; j < pa.text.length; j++ ) {
								if( pa.textName[ j ].indexOf( "File" ) >= 0 ) {
									v.add( new Integer(j) );
								}
							}
							bObj.modObj.modParam	= new String[ v.size() ][ 2 ];
							for( int j = 0; j < v.size(); j++ ) {
								bObj.modObj.modParam[j][0]  = pa.textName[ j ];
								bObj.modObj.modParam[j][1]  = pa.text[ j ];
							}
							batchTM.fireTableRowsUpdated( k, k );
							updateParamTable();
						}
					}
					
				} else if( e.getSource() == paramTM ) {
					// ignore
				}
			}
		};
		
	// -------- Command-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Batch List", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		initBatchTable();
		gui.registerGadget( batchTable, GG_BATCH );
		ggBatchPane		= new JScrollPane( batchTable );
		gui.addGadget( ggBatchPane, GG_BATCHPANE );

		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridwidth	= 1;
		con.weightx		= 0.05;
		con.weighty		= 0.0;
final JToolBar tb = new JToolBar();
tb.setBorderPainted( false );
tb.setFloatable( false );
//		ggCmd			= new JButton( "Add" );
//		gui.addButton( ggCmd, GG_CMDADD, al );
//		ggCmd			= new JButton( "Cut" );
//		gui.addButton( ggCmd, GG_CMDCUT, al );
//		ggCmd			= new JButton( "Copy" );
//		gui.addButton( ggCmd, GG_CMDCOPY, al );
//		ggCmd			= new JButton( "Paste" );
//		gui.addButton( ggCmd, GG_CMDPASTE, al );
//		ggCmd			= new JButton( "Edit" );
//		con.gridwidth	= GridBagConstraints.REMAINDER;
//		gui.addButton( ggCmd, GG_CMDEDIT, al );
		ggCmd			= new JButton( " Add " );
		gui.registerGadget( ggCmd, GG_CMDADD );
		tb.add( ggCmd );
		ggCmd.addActionListener( al );
		ggCmd			= new JButton( " Cut " );
		gui.registerGadget( ggCmd, GG_CMDCUT );
		tb.add( ggCmd );
		ggCmd.addActionListener( al );
		ggCmd			= new JButton( " Copy " );
		gui.registerGadget( ggCmd, GG_CMDCOPY );
		tb.add( ggCmd );
		ggCmd.addActionListener( al );
		ggCmd			= new JButton( " Paste " );
		gui.registerGadget( ggCmd, GG_CMDPASTE );
		tb.add( ggCmd );
		ggCmd.addActionListener( al );
		ggCmd			= new JButton( " Open " );
//ggCmd.setEnabled( false );
		gui.registerGadget( ggCmd, GG_CMDOPEN );
		tb.add( ggCmd );
		ggCmd.addActionListener( al );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addGadget( tb, GG_OFF_OTHER + 666 );

	// -------- Entry-Settings --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Module Parameter Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 1.0;
		con.weighty		= 0.3;
		initParamTable();
		gui.registerGadget( paramTable, GG_PARAMS );
		ggParamPane		= new JScrollPane( paramTable );
		gui.addGadget( ggParamPane, GG_PARAMSPANE );
		con.weighty		= 0.0;
		
	// -------- Control-Settings --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Control Settings", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		con.fill		= GridBagConstraints.HORIZONTAL;
		con.gridwidth	= 1;
		ggVisible		= new JCheckBox( "Visible modules" );
		gui.addCheckbox( ggVisible, GG_VISIBLE, null );
		con.gridwidth	= GridBagConstraints.REMAINDER;
		ggConsole		= new JCheckBox( "Console output" );
		gui.addCheckbox( ggConsole, GG_CONSOLE, null );
		
		// XXX
		setPreferredSize( new Dimension( 500, 600 ));

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR | FLAGS_PROGBARASYNC, gui );
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		int			i, num;
		BatchObject	bObj;
		Properties	p;
		
		super.fillGUI();
		super.fillGUI( gui );

		p	= Presets.valueToProperties( getPropertyArray().text[ PR_BATCH ]);
		num	= p.size();
		batchVector.clear();
		
		for( i = 0; i < num; i++ ) {
			bObj = BatchObject.valueOf( (String) p.get( String.valueOf( i )));
			batchVector.add( bObj );
		}
		
		batchTM.fireTableDataChanged();
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		Properties	p	= new Properties();
		int			num	= batchVector.size();
	
		super.fillPropertyArray();
		super.fillPropertyArray( gui );
		
		for( int i = 0; i < num; i++ ) {
			p.put( String.valueOf( i ), ((BatchObject) batchVector.get( i )).toString() );
		}
		getPropertyArray().text[ PR_BATCH ] = Presets.propertiesToValue( p );
	}

	/*
	 *	Batch-JTable bauen
	 */
	private void initBatchTable()
	{
		TableColumn			column;
		BatchCellEditor		batchCE;
		int					i;
		
		batchTM		= new BatchTableModel( batchVector );
		batchTable  = new JTable( batchTM );
		batchCR		= new BatchCellRenderer();
		
		int[] prefWidth = { 16, 64, 256, 48, 48 };
		for ( i = 0; i < prefWidth.length; i++) {
			column = batchTable.getColumnModel().getColumn( i );
			column.setPreferredWidth( prefWidth[i] );
			column.setCellRenderer( batchCR );
		}
		batchTable.getTableHeader().setReorderingAllowed( false );
	
		JComboBox cmdCombo = new JComboBox();
		for( i = 0; i < BatchCellRenderer.CMD_NAMES.length; i++ ) {
			cmdCombo.addItem( BatchCellRenderer.CMD_NAMES[i] );
		}
		JComboBox errCombo = new JComboBox();
		for( i = 0; i < BatchCellRenderer.ERR_NAMES.length; i++ ) {
			errCombo.addItem( BatchCellRenderer.ERR_NAMES[i] );
		}
	
		batchCE = new BatchCellEditor();
		batchTable.getColumnModel().getColumn(1).setCellEditor( new DefaultCellEditor( cmdCombo ));
		batchTable.getColumnModel().getColumn(2).setCellEditor( batchCE );
		batchTable.getColumnModel().getColumn(3).setCellEditor( new DefaultCellEditor( errCombo ));
		batchTable.getColumnModel().getColumn(4).setCellEditor( batchCE );
		
		batchTable.setShowHorizontalLines( true );
		batchTable.setShowVerticalLines( true );
		batchTable.setIntercellSpacing( new Dimension( 1, 1 ));
		batchTable.setGridColor( Color.lightGray );
	
		batchTM.addTableModelListener( tml );
		batchTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent e )
			{
				updateParamTable();
			}
		});
	}

	/**
	 *	Params-JTable bauen
	 */
	protected void initParamTable()
	{
		TableColumn			column;
		int					i;
		BasicCellRenderer   bcr = new BasicCellRenderer();
	
		paramTM		= new ParamTableModel();
		paramTM.addTableModelListener( tml );
		paramTable  = new JTable( paramTM );
		paramTable.getTableHeader().setReorderingAllowed( false );
	
		int[] prefWidth = { 108, 324 };
		for ( i = 0; i < prefWidth.length; i++) {
			column = paramTable.getColumnModel().getColumn( i );
			column.setPreferredWidth( prefWidth[i] );
			column.setCellRenderer( bcr );
		}
		
		paramTable.setShowHorizontalLines( true );
		paramTable.setShowVerticalLines( true );
		paramTable.setIntercellSpacing( new Dimension( 1, 1 ));
		paramTable.setGridColor( Color.lightGray );
	}

// -------- Processor Interface --------
		
	protected void process()
	{
		int				i, j;
		int				progGoal;
//		float			modProg;
		
		List			loops		= new ArrayList();
		int				line;
		int				lines		= batchVector.size();
		BatchObject		bObj, bObj2;
		DocumentFrame	procWin;
		Exception		cmdErr;
		int				errorCount = 0;
		Component		c;
		StringBuffer	sb;
		String			s;
		DateFormat		df			= DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM );
		
topLevel: try {
batchLoop:	for( line = 0; threadRunning && (line < lines); ) {

			// ============================== recalc progress weights ==============================
			// ============================== execute command ==============================

				bObj		= (BatchObject) batchVector.get( line );
				batchTable.clearSelection();
				batchTable.setRowSelectionInterval( line, line );
				batchTable.scrollRectToVisible( batchTable.getCellRect( line, 0, true ));
				cmdErr		= null;
				
			// ---- output to console ----
				if( pr.bool[ PR_CONSOLE ]) {
					sb  = new StringBuffer();
					s   = String.valueOf( line + 1 );
					sb.append( "     ".substring( s.length() ));
					sb.append( s );
					sb.append( "  " );
					c  = batchCR.getTableCellRendererComponent( batchTable, bObj, false, false, line, 1 );
					if( c instanceof JLabel ) {
						sb.append( ((JLabel) c).getText() );
					}
					sb.append( ": " );
					c  = batchCR.getTableCellRendererComponent( batchTable, bObj, false, false, line, 2 );
					if( c instanceof JLabel ) {
						sb.append( replaceLoopVars( ((JLabel) c).getText(), loops ));
					}
					sb.append( "  " );
					consoleLine( sb.toString(), df );
				}

				switch( bObj.command ) {
				case BatchObject.CMD_MODULE:							// -------------------- module --------------------
					bObj.process = 0.001f;
					batchTM.fireTableCellUpdated( line, 2 );
					try {
						procWin	= getProcInstance( bObj, loops );
						procWin.fillGUI();
						if( pr.bool[ PR_VISIBLE ]) {
							procWin.setVisible( true );
						}
						try {
							telepathy = true;
							procWin.addProcessorListener( procL );
							procWin.start();
							progGoal	= 1;
							do {
								try {
									synchronized( this ) {
										wait( 3000 );
									}
								} catch( InterruptedException e3 ) { /* ignored */ }
								bObj.process = procWin.getProgression();
								batchTM.fireTableCellUpdated( line, 2 );
								j		= (int) (bObj.process * 10);
								if( j >= progGoal ) {
									if( pr.bool[ PR_CONSOLE ] ) {
										System.out.print( (j * 10) + "%  " );
									}
									progGoal = j + 1;
								}
								setProgression( 0.5f );

								if( !procWin.isThreadRunning() ) {
									telepathy = false;
								}

							} while( threadRunning && telepathy );

							if( procWin.getError() != null ) throw procWin.getError();

							telepathy = false;
							procWin.removeProcessorListener( procL );
							procWin.stop();
							if( pr.bool[ PR_VISIBLE ]) {
								procWin.setVisible( false );
							}
							procWin.dispose();

						} catch( Exception e2 ) {
							telepathy = false;
							procWin.removeProcessorListener( procL );
							procWin.stop();
							if( pr.bool[ PR_VISIBLE ]) {
								procWin.setVisible( false );
							}
							procWin.dispose();
							throw e2;
						}
					}
					catch( Exception e1 ) {
						cmdErr = e1;
					}
					setProgression( 0.5f );
					bObj.process = 0.0f;
					batchTM.fireTableCellUpdated( line, 2 );
					break;

				case  BatchObject.CMD_BEGLOOP:							// -------------------- loop start --------------------
				
					bObj.loopObj.processIdx = bObj.loopObj.startIdx;
					loops.add( bObj );
					break;

				case  BatchObject.CMD_ENDLOOP:							// -------------------- loop end --------------------
					for( i = loops.size() - 1; i >= 0; i-- ) {
						bObj2 = (BatchObject) loops.get( i );
						if( bObj.loopObj.variable == bObj2.loopObj.variable ) {
							if( ++bObj2.loopObj.processIdx <= bObj2.loopObj.stopIdx ) {
								line = batchVector.indexOf( bObj2 );
								if( pr.bool[ PR_CONSOLE ]) {
									System.out.print( " = " + bObj2.loopObj.processIdx );
								}
							} else {
								loops.remove( i );
							}
							break;
						}
					}
					break;

				case BatchObject.CMD_DELFILE:									// -------------------- del file --------------------
					try {
						if( !new File( replaceLoopVars( bObj.fileObj, loops )).delete() ) {
							throw new IOException( ERR_DELETE );
						}
					}
					catch( Exception e1 ) {
						cmdErr = e1;
					}
					break;

				case BatchObject.CMD_SKIP:										// -------------------- skip --------------------
					line = findLabelLine( bObj.labelObj ) - 1;
					break;
				}

			// ---- error handling ----
				if( cmdErr == null ) {
					line++;
				} else {
					setError( cmdErr );
					errorCount++;
					if( pr.bool[ PR_CONSOLE ]) {
						sb = new StringBuffer();
						sb.append( "\n" );
						sb.append( getError().getClass().getName() );
						sb.append( ": " );
						sb.append( getError().getLocalizedMessage() );
						System.out.println( sb.toString() );
					}
					switch( bObj.errorCmd ) {
					case BatchObject.ERR_STOP:
						break batchLoop;
					case BatchObject.ERR_SKIP:
						line = findLabelLine( bObj.labelObj );
						break;
					case BatchObject.ERR_CONTINUE:
						line++;
						break;
					default:
						assert false : bObj.errorCmd;
					}
				}
				
				if( pr.bool[ PR_CONSOLE ]) {
					System.out.println();
				}
			} // while lines < lines

		// .... check running ....
			if( !threadRunning ) break topLevel;
			setProgression( 1.0f );

			// ---- finish ----
			if( pr.bool[ PR_CONSOLE ]) {
				sb = new StringBuffer();
				sb.append( "  Done. There " );
				sb.append( errorCount == 1 ? "was " : "were " );
				sb.append( String.valueOf( errorCount ));
				sb.append( errorCount == 1 ? " error.\n" : " errors.\n" );
				consoleLine( sb.toString(), df );
			}
			if( errorCount > 0 ) {
				setError( new Exception( getError().getClass().getName() + ": \n" +
										 getError().getLocalizedMessage() +
										 "\n(last out of "+errorCount+" errors)" ));
			}
			
		} catch( IllegalStateException e99 ) {
			setError( e99 );
		}
		
		telepathy = false;
	} // process()

// -------- private Methoden --------

	private int findLabelLine( String label )
	{
		BatchObject bObj;
		int i;
	
		for( i = 0; i < batchVector.size(); i++ ) {
			bObj = (BatchObject) batchVector.get( i );
			if( (bObj.command == BatchObject.CMD_LABEL) && (bObj.labelObj.equals( label ))) break;
		}
		return i;
	}

	private void consoleLine( String line, DateFormat df )
	{
		System.out.print( df.format( new Date() ) + line );
	}

	protected void updateParamTable()
	{
		String[][]  modParam = null;
		BatchObject bObj;
		
		if( batchTable.getSelectedRowCount() == 1 ) {
			bObj = (BatchObject) batchVector.get( batchTable.getSelectedRow() );
			if( bObj.command == BatchObject.CMD_MODULE ) {
				modParam = bObj.modObj.modParam;
			}
		}
		paramTM.setParam( modParam );
	}
	
	protected DocumentFrame getProcInstance( BatchObject bObj, List loops )
	throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		DocumentFrame procWin;
		int i, j;
		PropertyArray pa;
		
		procWin	= (DocumentFrame) Class.forName( bObj.modObj.modClass ).newInstance();
		pa = procWin.getPropertyArray();
		pa.fromProperties( true, bObj.modObj.prParam );
		for( i = 0; i <  bObj.modObj.modParam.length; i++ ) {
			for( j = 0; j < pa.textName.length; j++ ) {
				if( bObj.modObj.modParam[i][0].equals( pa.textName[j] )) {
					pa.text[j] = replaceLoopVars( bObj.modObj.modParam[i][1], loops );
					break;
				}
			}
		}
		return procWin;
	}
	
	private String replaceLoopVars( String pattern, List loops )
	{
		if( (loops == null) || (loops.isEmpty()) || (pattern.indexOf( "$" ) == -1) ) return pattern;
		
		return replaceLoopVars( pattern, loops, loops.size() - 1, new Flag( false ));
	}
	
	private String replaceLoopVars( String s, List loops, int idx, Flag exists )
	{
		final LoopObject	lObj		= ((BatchObject) loops.get( idx )).loopObj;
		final String		searchStr	= "$" + lObj.variable;
		int					j;
		String				result		= null;
		
		for( int k = 0; k < 2; k++ ) {
			final StringBuffer	sb			= new StringBuffer( s );
			final boolean		useZeros	= k == 1; // this way the "failing" case is with all zero-padded (original behaviour)
			while( (j = sb.indexOf( searchStr )) >= 0 ) {
				final String	procStr	 = String.valueOf( lObj.processIdx );
				final int		numZeros = String.valueOf( lObj.stopIdx ).length() - procStr.length();
				final String	replc;
				if( useZeros ) {
					replc	= "000000".substring( 0, numZeros ) + procStr;
				} else {
					replc	= procStr;
				}
				sb.replace( j, j + 2, replc );
			}
			result = sb.toString();
			if( idx > 0 ) {
				result = replaceLoopVars( result, loops, idx - 1, exists );
			} else {
				exists.set( new File( result ).exists() );
			}
			if( exists.isSet() ) return result;
		}
		return result;
	}
	
// -------------- internal classes --------------

	protected static class BatchTableModel
	extends AbstractTableModel
	{
		private static final String[] columnNames = { "Line", "Command", "Object", "On Error", "Error Label" };
		
		private List		   batchVector;
		private static Class   integerClass		= Integer.class;
		private static Class   batchObjectClass    = BatchObject.class;
		
		protected BatchTableModel( List batchVector )
		{
			this.batchVector = batchVector;
		}
		
		public int getColumnCount()
		{
			return columnNames.length;
		}

		public int getRowCount()
		{
			return batchVector.size();
		}

		public String getColumnName( int column )
		{
			return columnNames[ column ];
		}

		public Object getValueAt( int row, int column )
		{
			switch( column ) {
			case 0:     return new Integer( row + 1 );
			default:    return batchVector.get( row );
		}
		}

		public Class getColumnClass( int column )
		{
			switch( column ) {
				case 0:     return integerClass;
				default:    return batchObjectClass;
			}
		}

		/*
		* Don't need to implement this method unless your table's
		* editable.
		*/
		public boolean isCellEditable( int row, int col )
		{
			return( col >= 1 );
		}

		public void setValueAt( Object value, int row, int column )
		{
			int i;
			
			switch( column ) {
			case 1:
				if( !(value instanceof String) ) break;
				for( i = 0; i < BatchCellRenderer.CMD_NAMES.length; i++ ) {
					if( value.equals( BatchCellRenderer.CMD_NAMES[i] )) {
						((BatchObject) batchVector.get( row )).command = i;
						break;
					}
				}
				break;
				
			case 2:
				batchVector.set( row, value );
				break;
				
			case 3:
				if( !(value instanceof String) ) break;
				for( i = 0; i < BatchCellRenderer.ERR_NAMES.length; i++ ) {
					if( value.equals( BatchCellRenderer.ERR_NAMES[i] )) {
						((BatchObject) batchVector.get( row )).errorCmd = i;
						break;
					}
				}
				break;
			}
			fireTableRowsUpdated( row, row );
		}
	} // BatchTableModel

	protected static class BatchCellRenderer
	extends BasicCellRenderer
	{
		protected static final String[] CMD_NAMES = { "Module", "Delete File", "Begin Loop", "End Loop", "Skip To", "Label" };
		protected static final String[] ERR_NAMES = { "Stop", "Continue", "Skip To" };

		private JProgressBar progBar = new JProgressBar( 0, 100 );
		
		protected BatchCellRenderer()
		{
			super();
			progBar.setStringPainted( true );
		}
		
		public Component getTableCellRendererComponent( JTable table, Object obj, boolean isSelected, boolean hasFocus,
														int row, int column )
		{
			super.getTableCellRendererComponent( table, obj, isSelected, hasFocus, row, column );

			BatchObject bObj;
			
			setText( "" );
			
			switch( column ) {
			case 0:
				if( !(obj instanceof Integer) ) break;
				setText( obj.toString() );
				setHorizontalAlignment( RIGHT );
				break;
		
			case 1:
				if( !(obj instanceof BatchObject) ) break;
				bObj = (BatchObject) obj;
				setText( CMD_NAMES[ bObj.command ]);
				break;
		
			case 2:
				if( !(obj instanceof BatchObject) ) break;
				bObj = (BatchObject) obj;
		
				switch( bObj.command ) {
				case BatchObject.CMD_MODULE:
					if( bObj.process == 0.0f ) {
						setFont( boldFont );
						setText( bObj.modObj.name );
					} else {
						progBar.setString( bObj.modObj.name );
						progBar.setValue( (int) (bObj.process * 100) );
						return progBar;
					}
					break;
				case BatchObject.CMD_DELFILE:
					setText( bObj.fileObj );
					break;
				case BatchObject.CMD_BEGLOOP:
					setFont( monoFont );
					setText( bObj.loopObj.variable + " = " + bObj.loopObj.startIdx + " TO " + bObj.loopObj.stopIdx );
					break;
				case BatchObject.CMD_ENDLOOP:
					setFont( monoFont );
					setText( String.valueOf( bObj.loopObj.variable ));
					break;
				case BatchObject.CMD_SKIP:
				case BatchObject.CMD_LABEL:
					setFont( italicFont );
					setText( bObj.labelObj );
					break;
				default:
					setText( "" );
					break;
				}
				break;
				
			case 3:
				if( !(obj instanceof BatchObject) ) break;
				bObj = (BatchObject) obj;
				if( BatchObject.CMD_CANERROR[ bObj.command ]) {
					setText( ERR_NAMES[ bObj.errorCmd ]);
				}
				break;
				
			case 4:
				if( !(obj instanceof BatchObject) ) break;
				bObj = (BatchObject) obj;
				if( (bObj.errorCmd == BatchObject.ERR_SKIP) && BatchObject.CMD_CANERROR[ bObj.command ]) {
					setFont( italicFont );
					setText( bObj.labelObj );
				}
				break;
		
			default:
				setText( obj.toString() );
				break;
			}
		
			return this;
		}
	} // BatchCellRenderer

	protected class BatchCellEditor
	extends AbstractCellEditor
	implements TableCellEditor
	{
		private ModuleCellEditor    modCE   = new ModuleCellEditor();
		private JFormattedTextField modFTF  = new JFormattedTextField( modCE );
		private LabelCellEditor     labCE   = new LabelCellEditor();
		private JFormattedTextField labFTF  = new JFormattedTextField( labCE );
		private FileCellEditor      fileCE  = new FileCellEditor();
		private JFormattedTextField fileFTF = new JFormattedTextField( fileCE );
		private LoopCellEditor      loopCE  = new LoopCellEditor();
		private JFormattedTextField loopFTF = new JFormattedTextField( loopCE );

		private BatchObject obj;

		public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected,
								int row, int column )
		{
			if( !(value instanceof BatchObject) ) return null;
		
			this.obj = (BatchObject) value;

			switch( column ) {
			case 2:
				switch( obj.command ) {
				case BatchObject.CMD_MODULE:
					modFTF.setValue( obj );
					return modFTF;
				case BatchObject.CMD_SKIP:
				case BatchObject.CMD_LABEL:
					labFTF.setValue( obj );
					return labFTF;
				case BatchObject.CMD_DELFILE:
					fileFTF.setValue( obj );
					return fileFTF;
				case BatchObject.CMD_BEGLOOP:
				case BatchObject.CMD_ENDLOOP:
					loopFTF.setValue( obj );
					return loopFTF;
				}
				break;
			
			case 4:
				labFTF.setValue( obj );
				return labFTF;
			}
			return null;
		}
			
		public Object getCellEditorValue()
		{
			return obj;
		}
	} // BatchCellEditor
		
	protected class ModuleCellEditor
	extends DefaultFormatter
	{
		private BatchObject  obj = null;

		public String valueToString( Object o )
		{
			obj = (BatchObject) o;
			if( obj != null ) {
				return obj.modObj.name;
			} else return "";
		}
		
		public Object stringToValue( String s )
		{
			if( obj != null ) {
				obj.modObj.name = s;
			}
			return obj;
		}
	} // ModuleCellEditor

	protected class LabelCellEditor
	extends DefaultFormatter
	{
		private BatchObject  obj = new BatchObject();

		public String valueToString( Object o )
		{
			obj = (BatchObject) o;
			if( obj != null ) {
				return obj.labelObj;
			} else return "";
		}
		
		public Object stringToValue( String s )
		{
			obj.labelObj = s;
			return obj;
		}
	} // LabelCellEditor
		
	protected class FileCellEditor
	extends DefaultFormatter
	{
		private BatchObject  obj = new BatchObject();

		public String valueToString( Object o )
		{
			obj = (BatchObject) o;
			if( obj != null ) {
				return obj.fileObj;
			} else return "";
		}
		
		public Object stringToValue( String s )
		{
			obj.fileObj = s;
			return obj;
		}
	} // FileCellEditor

	protected class LoopCellEditor
	extends DefaultFormatter
	{
		private BatchObject  obj = null;

		public String valueToString( Object o )
		{
			obj = (BatchObject) o;
			if( obj != null ) {
				if( obj.command == BatchObject.CMD_BEGLOOP ) {
					return( String.valueOf( obj.loopObj.variable ) + " = " + String.valueOf( obj.loopObj.startIdx ) + " TO " + String.valueOf( obj.loopObj.stopIdx ));
				} else {
					return( String.valueOf( obj.loopObj.variable ));
				}
			} else return "";
		}
		
		public Object stringToValue( String s )
		{
			if( obj != null ) {
				int i, j, k;
				final String sNorm = s.toUpperCase();
				i = sNorm.indexOf( "=" );
				j = sNorm.indexOf( "TO", i+1 );
				if( (sNorm.length() > 0) && Character.isLetter( sNorm.charAt( 0 ))) {
					obj.loopObj.variable = sNorm.charAt( 0 );
				}
				if( (i > 0) && (j > (i+1)) && (j+2 < sNorm.length()) ) {
					try {
						k = Math.max( 0, Integer.parseInt( sNorm.substring( i+1, j ).trim() ));
						obj.loopObj.startIdx = k;
						k = Math.max( k, Integer.parseInt( sNorm.substring( j+2 ).trim() ));
						obj.loopObj.stopIdx = k;
					} catch( NumberFormatException e99 ) { /* ignore */}				
				}
			}
			return obj;
		}
	} // LoopCellEditor

	protected static class BatchObjectArray
	implements Transferable
	{
		protected static DataFlavor flavor;
		
		private static DataFlavor[] flavors;
		private BatchObject[] obj;
		
		protected BatchObjectArray()
		{
			if( flavor == null ) {
				flavor      = new DataFlavor( getClass(), "Batch Object Array" );
				flavors     = new DataFlavor[1];
				flavors[0]  = flavor;
			}
		}
		
		protected BatchObjectArray( BatchObject[] obj )
		{
			this.obj = obj;
		}
			
		public DataFlavor[] getTransferDataFlavors()
		{
			return flavors;
		}
		
		public boolean isDataFlavorSupported( DataFlavor fl )
		{
			for( int i = 0; i < flavors.length; i++ ) {
				if( fl.match( flavors[i] )) return true;
			}
			return false;
		}
		
		public Object getTransferData( DataFlavor fl )
		throws UnsupportedFlavorException
		{
			for( int i = 0; i < flavors.length; i++ ) {
				if( fl.match( flavors[i] )) {
					BatchObject[] objCopy = new BatchObject[ obj.length ];
					for( int j = 0; j < objCopy.length; j++ ) {
						objCopy[j] = new BatchObject( obj[j] );
					}
					return objCopy;
				}
			}
			throw new UnsupportedFlavorException( fl );
		}
	}

	/**
	 *	Interne Klasse fuer die Listeneintraege
	 */
	protected static class BatchObject
	{
		protected static final int CMD_MODULE      = 0;
		protected static final int CMD_DELFILE     = 1;	
		protected static final int CMD_BEGLOOP     = 2;
		protected static final int CMD_ENDLOOP     = 3;
		protected static final int CMD_SKIP	    = 4;
		protected static final int CMD_LABEL       = 5;
		
		protected static final boolean[] CMD_CANERROR  = { true, true, false, false, false, false };

		private static final int ERR_STOP	    = 0;
		private static final int ERR_CONTINUE  	= 1;	
		private static final int ERR_SKIP	    = 2;

		protected  int			command     = CMD_LABEL;
		protected  int			errorCmd    = ERR_STOP;
		protected  ModuleObject   modObj      = new ModuleObject();
		protected  LoopObject     loopObj     = new LoopObject();
		protected  String			fileObj     = "file";
		protected  String			labelObj    = "label";
		protected  float			process		= 0.0f;		// wird >0 waehrend des Ablaufeens des zugehoerigen Moduls
		
		private final static String	PR_CMD		= "comd";
		private final static String	PR_ERRCMD	= "ecmd";
		private final static String	PR_FILE		= "file";
		private final static String	PR_LABEL	= "labl";
		private final static String	PR_MODNAME  = "mnam";
		private final static String	PR_MODCLASS = "mcls";
		private final static String	PR_MODPAR	= "mpar";
		private final static String	PR_MODMOD   = "mmod";
		private final static String	PR_LOOPVAR  = "lvar";
		private final static String	PR_LPSTART  = "lbeg";
		private final static String	PR_LPSTOP   = "lend";

		protected BatchObject() { /* nothing */ }
		
		protected BatchObject( BatchObject source )
		{
			command     = source.command;
			errorCmd    = source.errorCmd;
			modObj      = new ModuleObject( source.modObj );
			loopObj     = new LoopObject( source.loopObj );
			fileObj     = source.fileObj;
			labelObj    = source.labelObj;
		}

		public static BatchObject valueOf( String s )
		{
			Properties	p			= Presets.valueToProperties( s );
			BatchObject	bObj		= new BatchObject();
			String		s2;
			Properties  p2;
			Enumeration en;
			int			i;

			try {
				bObj.fileObj			= (String) p.get( PR_FILE );
				bObj.labelObj			= (String) p.get( PR_LABEL );
				bObj.modObj.name		= (String) p.get( PR_MODNAME );
				bObj.modObj.modClass	= (String) p.get( PR_MODCLASS );
				bObj.command			= Integer.parseInt( (String) p.get( PR_CMD ));
				bObj.errorCmd			= Integer.parseInt( (String) p.get( PR_ERRCMD ));
				s2						= (String) p.get( PR_LOOPVAR );
				if( s2.length() > 0 ) {
					bObj.loopObj.variable = s2.charAt( 0 );
				}
				bObj.loopObj.startIdx   = Integer.parseInt( (String) p.get( PR_LPSTART ));
				bObj.loopObj.stopIdx	= Integer.parseInt( (String) p.get( PR_LPSTOP ));
			}
			catch( NumberFormatException e1 ) { /* ignored */ }
			catch( NullPointerException e2 ) { /* ignored */ }

			s2 = (String) p.get( PR_MODPAR );
			if( s2 != null ) bObj.modObj.prParam = Presets.valueToProperties( s2 );
			s2 = (String) p.get( PR_MODMOD );
			if( s2 != null ) {
				p2 = Presets.valueToProperties( s2 );
				bObj.modObj.modParam	= new String[ p2.size() ][ 2 ];
				en					= p2.propertyNames();
				for( i = 0; en.hasMoreElements(); i++ ) {
					s2 = (String) en.nextElement();
					bObj.modObj.modParam[i][0]  = s2;
					bObj.modObj.modParam[i][1]  = p2.getProperty( s2 );
				}
			}
			return bObj;
		}

		public String toString()
		{
			Properties  p	= new Properties();
			Properties  p2  = new Properties();
			int			i;

			p.put( PR_FILE, this.fileObj );
			p.put( PR_LABEL, this.labelObj );
			p.put( PR_MODNAME, this.modObj.name );
			if( this.modObj.modClass != null ) p.put( PR_MODCLASS, this.modObj.modClass );
			p.put( PR_CMD, String.valueOf( this.command ));
			p.put( PR_ERRCMD, String.valueOf( this.errorCmd ));
			p.put( PR_LOOPVAR, String.valueOf( this.loopObj.variable ));
			p.put( PR_LPSTART, String.valueOf( this.loopObj.startIdx ));
			p.put( PR_LPSTOP, String.valueOf( this.loopObj.stopIdx ));
			
			if( this.modObj.prParam != null ) {
				p.put( PR_MODPAR, Presets.propertiesToValue( this.modObj.prParam ));
			}
			if( this.modObj.modParam != null ) {
				for( i = 0; i < this.modObj.modParam.length; i++ ) {
					p2.setProperty( this.modObj.modParam[i][0], this.modObj.modParam[i][1] );
				}
				p.put( PR_MODMOD, Presets.propertiesToValue( p2 ));
			}
			return( Presets.propertiesToValue( p ));
		}
	} // BatchObject

	/**
	 *	Interne Klasse fuer die Listeneintraege
	 */
	protected static class LoopObject
	{
		protected  char   variable    = 'A';
		protected  int    startIdx    = 1;
		protected  int    stopIdx     = 9;
		protected  int	processIdx;
		
		protected LoopObject() { /* nothing */}
		
		protected LoopObject( LoopObject source )
		{
			variable    = source.variable;
			startIdx    = source.startIdx;
			stopIdx     = source.stopIdx;
		}
	} // LoopObject

	/**
	 *	Interne Klasse fuer die Listeneintraege
	 */
	protected static class ModuleObject
	{
		protected  String  name			= "module";
		protected  String  modClass		= null;
		protected  Properties prParam		= new Properties();
		protected  String[][]  modParam	= new String[0][2];
		
		protected ModuleObject() { /* nothing */ }

		protected ModuleObject( ModuleObject source )
		{
			int i;

			name	    = source.name;
			modClass    = source.modClass;
			prParam		= source.prParam;
			modParam	= new String[ source.modParam.length ][ 2 ];
			
			for( i = 0; i < modParam.length; i++ ) {
				modParam[i][0] = source.modParam[i][0];
				modParam[i][1] = source.modParam[i][1];
			}
		}
	} // ModuleObject

	protected static class ParamTableModel
	extends AbstractTableModel
	{
		private static final String[] columnNames = { "Parameter", "Value" };
		
		private String[][]  emptyData   = new String[0][2];
		private String[][]  paramData   = emptyData;
		
		protected void setParam( String[][] paramData )
		{
			if( paramData != null ) {
				this.paramData = paramData;
				fireTableDataChanged();
			} else {
				boolean wasEmpty = this.paramData.length == 0;
				this.paramData  = emptyData;
				if( !wasEmpty ) fireTableDataChanged();
			}
		}
		
		public int getColumnCount()
		{
			return columnNames.length;
		}

		public int getRowCount()
		{
			return paramData.length;
		}

		public String getColumnName( int column )
		{
			return columnNames[ column ];
		}

		public Object getValueAt( int row, int column )
		{
			return paramData[row][column];
		}

		public Class getColumnClass( int column )
		{
			if( paramData.length == 0 ) return null;
			return paramData[0][column].getClass();
		}

		/*
		* Don't need to implement this method unless your table's
		* editable.
		*/
		public boolean isCellEditable( int row, int col )
		{
			return( col >= 1 );
		}

		public void setValueAt( Object value, int row, int column )
		{
			paramData[row][column] = (String) value;
			fireTableRowsUpdated( row, row );
		}
	} // ParamTableModel
}
// class BatchDlg
