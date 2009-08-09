/*
 *  BasicFrame.java
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import de.sciss.fscape.prop.*;
import de.sciss.fscape.util.*;

/**
 *  Dialog for breakpoint editing.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class EditEnvDlg
extends BasicDialog
{
// -------- public Variablen --------

	public static final int GG_PRESETS		= 1;		// IDs und zugleich Hash-Keys
	public static final int GG_ADDPRESET	= 2;
	public static final int GG_DELPRESET	= 3;

	public static final int GG_ATK			= 10;	// WICHTIG: immer aufeinanderfolgende IDs
	public static final int GG_SUS			= 11;	//			fuer Atk/Sus/Rls Gadgets!!!
	public static final int GG_RLS			= 12;
	public static final int GG_ATKCURVE		= 13;
	public static final int GG_SUSCURVE		= 14;
	public static final int GG_RLSCURVE		= 15;
	public static final int GG_ATKTIME		= 16;
	public static final int GG_SUSTIME		= 17;
	public static final int GG_RLSTIME		= 18;
	public static final int GG_POINTX		= 19;
	public static final int GG_POINTY		= 20;

	public static final int GG_OK			= 30;
	public static final int GG_CANCEL		= 31;

// -------- private Variablen --------

	protected GUISupport gui;		// Dialog-Oberflaeche

	private boolean choice = false;			// keine Veränderung bei Cancel nötig

	protected static PropertyArray	static_pr	= null;
	protected static Presets		presets		= null;

	protected PropertyArray	pr;									// consists just of single env...

	// Properties (defaults)
	private static final int PR_ENV			= 0;			// Array-Indices: pr.envl
	private static final String PRN_ENV		= "Env";		// Property-Keynames

	private static final Envelope	prEnvl[]		= { null };
	private static final String	prEnvlName[]	= { PRN_ENV };
	
	private final EditEnvDlg enc_this	= this;

// -------- public Methoden --------
	// public boolean getJComboBox();

	/**
	 *	!! setVisible() und dispose() bleibt dem Aufrufer ueberlassen
	 *
	 *	@param parent	aufrufendes Fenster
	 */
	public EditEnvDlg( Component parent, Envelope env )
	{
		super( parent, "Edit Curve: ", true );

		// einmalig PropertyArray initialisieren
		if( static_pr == null ) {
			static_pr					= new PropertyArray();
			static_pr.envl				= prEnvl;
			static_pr.envl[ PR_ENV ]	= Envelope.createBasicEnvelope( Envelope.BASIC_TIME );
			static_pr.envlName			= prEnvlName;
		}
		// einmalig Presets initialisieren
		if( presets == null ) {
			presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		
		pr					= (PropertyArray) static_pr.clone();
		pr.envl[ PR_ENV ]	= env;

	// -------- GUI bauen --------
		
		GridBagConstraints		con;
		GridBagLayout			lay;
		EditEnvDlgListener 		list;
		
		Panel		toolBar;
		JComboBox	ggPresets;
		Iterator	presetNames;
		ToolIcon	ggAddPreset, ggDelPreset;

		CurvePanel	ggAtkCurve, ggSusCurve, ggRlsCurve;
		GroupLabel	lbAtk, lbSus, lbRls;
		JCheckBox	ggAtk, ggSus, ggRls;
		ParamField	ggAtkTime, ggSusTime, ggRlsTime;
		GroupLabel	lbPoint;
		ParamField	ggPointX, ggPointY;
		
		JPanel		buttonBar;
		JButton		ggOk, ggCancel;

		gui		= new GUISupport();
		con		= gui.getGridBagConstraints();
		lay		= gui.getGridBagLayout();
		list	= new EditEnvDlgListener( this, gui );
		con.insets = new Insets( 2, 2, 2, 2 );

	// -------- Toolbar --------
		con.fill = GridBagConstraints.HORIZONTAL;
		toolBar = new Panel();
		toolBar.setLayout( new FlowLayout( FlowLayout.LEFT, 2, 2 ) );

		ggAddPreset = new ToolIcon( ToolIcon.ID_ADDPRESET, null );
		gui.registerGadget( ggAddPreset, GG_ADDPRESET );
		ggAddPreset.addMouseListener( list );
		ggDelPreset = new ToolIcon( ToolIcon.ID_DELPRESET, null );
		gui.registerGadget( ggDelPreset, GG_DELPRESET );
		ggDelPreset.addMouseListener( list );

		ggPresets = new JComboBox();
		presetNames = getPresets().presetNames().iterator();
		while( presetNames.hasNext() ) {
			ggPresets.addItem( (String) presetNames.next() );
		}
		ggPresets.setSelectedItem( Presets.DEFAULT );						// default wählen und
		ggDelPreset.setEnabled( false );							// DelPreset deaktivieren
		gui.registerGadget( ggPresets, GG_PRESETS );
		ggPresets.addItemListener( list );

		toolBar.add( new JLabel( "Preset" ));
		toolBar.add( ggAddPreset );
		toolBar.add( ggDelPreset );
		toolBar.add( ggPresets );
		con.gridwidth = GridBagConstraints.REMAINDER;
		lay.setConstraints( toolBar, con );
		gui.add( toolBar );

	// -------- Eigentliche Gadgets --------
		lbAtk = new GroupLabel( "Attack",  GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM );
		lbSus = new GroupLabel( "Sustain", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM );
		lbRls = new GroupLabel( "Release", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM );

		con.weightx = 0.17;
		con.gridwidth = 2;
		gui.addLabel( lbAtk );
		con.weightx = 0.66;
		gui.addLabel( lbSus );
		con.weightx = 0.17;
		con.gridwidth = GridBagConstraints.REMAINDER;
		gui.addLabel( lbRls );

		con.fill = GridBagConstraints.BOTH;
		ggAtkCurve = new CurvePanel();
		ggSusCurve = new CurvePanel();
		ggRlsCurve = new CurvePanel();
		con.weighty = 1.0;
		con.weightx = 0.17;
		con.gridwidth = 2;
		ggAtkCurve.addActionListener( list );
		gui.addGadget( ggAtkCurve, GG_ATKCURVE );
		con.weightx = 0.66;
		ggSusCurve.addActionListener( list );
		gui.addGadget( ggSusCurve, GG_SUSCURVE );
		con.weightx = 0.17;
		con.gridwidth = GridBagConstraints.REMAINDER;
		ggRlsCurve.addActionListener( list );
		gui.addGadget( ggRlsCurve, GG_RLSCURVE );

// TEST
ParamSpace[] timeSpace	= new ParamSpace[ 3 ];
timeSpace[ 0 ]			= new ParamSpace( Constants.spaces[ Constants.absMsSpace ]);
//timeSpace[ 0 ].min		= timeSpace[ 0 ].inc;
timeSpace[ 0 ]			= new ParamSpace( timeSpace[ 0 ].inc, timeSpace[ 0 ].max, timeSpace[ 0 ].inc, timeSpace[ 0 ].unit );
timeSpace[ 1 ]			= new ParamSpace( Constants.spaces[ Constants.absBeatsSpace ]);
//timeSpace[ 1 ].min		= timeSpace[ 1 ].inc;
timeSpace[ 0 ]			= new ParamSpace( timeSpace[ 1 ].inc, timeSpace[ 1 ].max, timeSpace[ 1 ].inc, timeSpace[ 1 ].unit );
timeSpace[ 2 ]			= new ParamSpace( Constants.spaces[ Constants.ratioTimeSpace ]);
//timeSpace[ 2 ].min		= timeSpace[ 2 ].inc;
timeSpace[ 2 ]			= new ParamSpace( timeSpace[ 2 ].inc, timeSpace[ 2 ].max, timeSpace[ 2 ].inc, timeSpace[ 2 ].unit );

		ggAtkTime	= new ParamField( timeSpace );
		ggSusTime	= new ParamField( timeSpace );
		ggRlsTime	= new ParamField( timeSpace );
		ggAtk		= new JCheckBox();
		ggSus		= new JCheckBox();
		ggRls		= new JCheckBox();

		con.fill = GridBagConstraints.HORIZONTAL;
		con.weighty = 0.0;
		con.weightx = 0.0;
		con.gridwidth = 1;
		gui.addCheckbox( ggAtk, GG_ATK, list );
		con.weightx = 0.17;
		gui.addParamField( ggAtkTime, GG_ATKTIME, list );
		con.weightx = 0.0;
		gui.addCheckbox( ggSus, GG_SUS, list );
		con.weightx = 0.66;
		gui.addParamField( ggSusTime, GG_SUSTIME, list );
		con.weightx = 0.0;
		gui.addCheckbox( ggRls, GG_RLS, list );
		con.weightx = 0.17;
		con.gridwidth = GridBagConstraints.REMAINDER;
		gui.addParamField( ggRlsTime, GG_RLSTIME, list );

		lbPoint = new GroupLabel( "Point",  GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_BOTTOM );
		con.gridwidth = 4;
		gui.addLabel( lbPoint );
		con.gridwidth = GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

		ggPointX	= new ParamField( Constants.spaces[ Constants.absMsSpace ]);
		ggPointY	= new ParamField( Constants.spaces[ Constants.modSpace ]);
		con.weightx = 0.0;
		con.gridwidth = 1;
		gui.addLabel( new JLabel( "X", JLabel.RIGHT ));
		con.weightx = 0.17;
		gui.addParamField( ggPointX, GG_POINTX, list );
		con.weightx = 0.0;
		gui.addLabel( new JLabel( "Y", JLabel.RIGHT ));
		con.weightx = 0.17;
		gui.addParamField( ggPointY, GG_POINTY, list );
		con.gridwidth = GridBagConstraints.REMAINDER;
		gui.addLabel( new JLabel() );

	// -------- Ok/Cancel Gadgets --------
		buttonBar = new JPanel();
		buttonBar.setLayout( new BorderLayout( 2, 2 ));
		
		ggCancel = new JButton( " Cancel " );
		gui.registerGadget( ggCancel, GG_CANCEL );
		ggCancel.addActionListener( list );
		ggOk = new JButton( "   OK   ");
		gui.registerGadget( ggOk, GG_OK );
		ggOk.addActionListener( list );

		buttonBar.add( ggCancel, "West" );
		buttonBar.add( ggOk, "East" );
		con.gridwidth = GridBagConstraints.REMAINDER;
		con.weightx = 0.0;
		lay.setConstraints( buttonBar, con );
		gui.add( buttonBar );

		// Gadgets an die Envelope anpassen
		envelopeToGUI();
		getContentPane().add( gui );

		// Position und Größe aus dem Preferences ermitteln
		setResizable( true );

		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e )
			{
				closeMe();
			}
		});
		
		super.initDialog();
	}
	
	private void closeMe()
	{
		this.setVisible( false );
		try {
			this.getPresets().store();		// Presets ggf. speichern
		} catch( IOException e2 ) {
			// vorerst egal XXX
			System.err.println( "error storing presets : " + e2.getLocalizedMessage() );
		}
		this.dispose();
	}

	/**
	 *	GUI an die Envelope anpassen
	 *	(sie befindet sich in pr.envl[ PR_ENV ])
	 */
	public void envelopeToGUI()
	{
		CurvePanel	curvePanel;
		JCheckBox	checkBox;
		ParamField	paramField;
		ParamSpace	xSpace;
		Curve		curve	= null;
		boolean		state	= false;
		Envelope	env		= pr.envl[ PR_ENV ];

		for( int i = 0; i < 3; i++ ) {

			switch( i ) {
			case 0:
				curve	= env.atkCurve;
				state	= env.atkState;
				break;
			case 1:
				curve	= env.susCurve;
				state	= env.susState;
				break;
			case 2:
				curve	= env.rlsCurve;
				state	= env.rlsState;
				break;
			default:
				break;
			}
			curvePanel	= (CurvePanel)	gui.getItemObj( GG_ATKCURVE + i );
			checkBox	= (JCheckBox)	gui.getItemObj( GG_ATK + i );
			paramField	= (ParamField)	gui.getItemObj( GG_ATKTIME + i );

			if( (curvePanel != null) && (curve != null) &&
				(checkBox != null) && (paramField != null) ) {

				curvePanel.setCurve( curve );
				curvePanel.setEnabled( state );
				checkBox.setSelected( state );
				xSpace = curvePanel.getHSpace();
				paramField.setParam( new Param( xSpace.max, xSpace.unit ));
				paramField.setEnabled( state );
			}
		}
	}

	/**
	 *	Envelope aus dem GUI beziehen
	 */
	public void GUIToEnvelope()
	{
		CurvePanel	curvePanel;
		Envelope	env;
		Curve		atkCurve	= null;
		Curve		susCurve	= null;
		Curve		rlsCurve	= null;
		boolean		atkState	= false;
		boolean		susState	= false;
		boolean		rlsState	= false;

		curvePanel	= (CurvePanel)	gui.getItemObj( GG_ATKCURVE );
		if( curvePanel != null ) {
			atkCurve = curvePanel.getCurve();
			atkState = curvePanel.isEnabled();
		}

		curvePanel	= (CurvePanel)	gui.getItemObj( GG_SUSCURVE );
		if( curvePanel != null ) {
			susCurve = curvePanel.getCurve();
			susState = curvePanel.isEnabled();
		}

		curvePanel	= (CurvePanel)	gui.getItemObj( GG_RLSCURVE );
		if( curvePanel != null ) {
			rlsCurve = curvePanel.getCurve();
			rlsState = curvePanel.isEnabled();
		}

		env					= new Envelope( atkCurve, susCurve, rlsCurve );
		env.atkState		= atkState;
		env.susState		= susState;
		env.rlsState		= rlsState;
		pr.envl[ PR_ENV ]	= env;
	}

	/**
	 *	Besorgt die Parameter des Dialogs in Form eines PropertyArray Objekts
	 */
	public PropertyArray getPropertyArray()
	{
		return pr;
	}

	/**
	 *	Liefert die Presets der Envelopes
	 */	
	public Presets getPresets()
	{
		return presets;
	}

	/**
	 *	Dialog-Ergebnis ermitteln
	 *
	 *	@return	modifizierte Envelope oder null bei Cancel
	 */
	public Envelope getEnvelope()
	{
		if( choice ) {
			return pr.envl[ PR_ENV ];
		} else {
			return null;
		}
	}

// -------- quasi-protected Methoden --------

	/*
	 *	NUR FUER DEN LISTENER
	 */
	public void setJComboBox( boolean choice )
	{
		this.choice = choice;
	}

	/*
	 *	Implementation der Listener
	 */
	private class EditEnvDlgListener
	implements	ActionListener, AdjustmentListener,
				ItemListener, MouseListener, ParamListener
	{
	// -------- private Variablen --------

		private EditEnvDlg dlg;		// Edit-Dialog, der den Listener installiert
		private GUISupport gui;		// gui, dessen Objecte hier überwacht werden

		private	CurvePanel	curvePanel = null;		// derzeit aktives

	// -------- public Methoden --------

		/**
		 *	@param dlg	Edit-Dialog, der den Listener installiert
		 *	@param gui	zu überwachendes GUI
		 */
		public EditEnvDlgListener( EditEnvDlg dlg, GUISupport gui )
		{
			this.dlg = dlg;
			this.gui = gui;
		}

	// -------- Action Methoden --------

		public void actionPerformed( ActionEvent e )
		{
			int			ID		= gui.getItemID( e );
			Component	associate, associate2;
			Param		xPara, yPara;
			ParamSpace	xSpace, ySpace;
			ParamSpace	spaces[];
			DoublePoint	pt;
			String		cmd		= e.getActionCommand();
			
			switch( ID ) {
			case EditEnvDlg.GG_ATKCURVE:				// ---------- Curve geaendert ----------
			case EditEnvDlg.GG_SUSCURVE:
			case EditEnvDlg.GG_RLSCURVE:
				
				associate	= gui.getItemObj( EditEnvDlg.GG_POINTX );
				associate2	= gui.getItemObj( EditEnvDlg.GG_POINTY );
				
				if( (associate != null) && (associate2 != null) ) {
		
					// neue Werte laden, ggf. Space aendern, Gadget enablen
					if( (cmd == CurvePanel.ACTION_POINTSELECTED) ||
						(cmd == CurvePanel.ACTION_POINTCHANGED) ) {
						
						pt = ((CurvePanel) e.getSource()).getPoint();
						if( pt == null ) break;

						if( curvePanel != e.getSource() ) {		// different panel, adjust spaces

							xSpace		= ((CurvePanel) e.getSource()).getHSpace();
							spaces		= new ParamSpace[ 1 ];
							spaces[ 0 ]	= xSpace;
							((ParamField) associate).setSpaces( spaces );
						}
						
						xSpace	= ((ParamField) associate).getSpace();
						xPara	= new Param( pt.x, xSpace.unit );
						ySpace	= ((ParamField) associate2).getSpace();
						yPara	= new Param( pt.y, ySpace.unit );

						associate.setEnabled( true );
						((ParamField) associate).setParam( xPara );
						associate2.setEnabled( true );
						((ParamField) associate2).setParam( yPara );
				
					} else if( cmd == CurvePanel.ACTION_POINTDESELECTED ) {

						associate.setEnabled( false );
						associate2.setEnabled( false );	

					} else if( cmd == CurvePanel.ACTION_SPACECHANGED ) {

						xSpace		= ((CurvePanel) e.getSource()).getHSpace();
						spaces		= new ParamSpace[ 1 ];
						spaces[ 0 ]	= xSpace;
						((ParamField) associate).setSpaces( spaces );
					}
				}
				
				curvePanel	= (CurvePanel) e.getSource();
				break;

			case EditEnvDlg.GG_OK:				// ---------- OK / Cancel ----------
				dlg.GUIToEnvelope();
				dlg.setJComboBox( true );
				// THRU
			case EditEnvDlg.GG_CANCEL:
				dlg.closeMe();
				break;

			default:
				break;
			}
		}

	// -------- ParamListener interface --------

		public void paramChanged( ParamEvent e )
		{
			int			ID		= gui.getItemID( e );
			int			ID2;
			Component	associate, associate2;
			Param		xPara, yPara;
			ParamSpace	xSpace;
			DoublePoint	pt;
			
			switch( ID ) {
			case EditEnvDlg.GG_ATKTIME:				// ---------- Curve-Time ----------
			case EditEnvDlg.GG_SUSTIME:
			case EditEnvDlg.GG_RLSTIME:
				
				ID2			= ID - EditEnvDlg.GG_ATKTIME + EditEnvDlg.GG_ATKCURVE;
				associate	= gui.getItemObj( ID2 );
				
				if( associate != null ) {

					xSpace	= ((ParamField) e.getSource()).getSpace();
					xPara	= ((ParamField) e.getSource()).getParam();
					xSpace	= new ParamSpace( 0.0, xPara.val, xSpace.inc, xSpace.unit );

					((CurvePanel) associate).rescale( xSpace, null );
				}
				break;

			case EditEnvDlg.GG_POINTX:				// ---------- Punkt-Koordinaten ----------
			case EditEnvDlg.GG_POINTY:

				associate	= gui.getItemObj( EditEnvDlg.GG_POINTX );
				associate2	= gui.getItemObj( EditEnvDlg.GG_POINTY );
				
				if( (associate != null) && (associate2 != null) && (curvePanel != null) ) {
		
					// neue Werte uebermitteln
//					if( cmd == ParamField.ACTION_VALUECHANGED ) {
					
						xPara	= ((ParamField) associate).getParam();
						yPara	= ((ParamField) associate2).getParam();
						pt		= new DoublePoint( xPara.val, yPara.val );

						curvePanel.setPoint( pt );
//					}
				}
				break;

			default:
				break;
			}
		}

	// -------- Adjustment Methoden --------

		public void adjustmentValueChanged( AdjustmentEvent e ) {}

	// -------- Item Methoden --------

		public void itemStateChanged( ItemEvent e )
		{
			int				ID		= gui.getItemID( e );
			int				ID2, ID3;
			Component		associate, associate2;
			String			name;
			PropertyArray	pa;
			Properties		preset;

			switch( ID ) {
			case EditEnvDlg.GG_ATK:					// ---------- Curventeile (de)aktiviert ----------
			case EditEnvDlg.GG_SUS:
			case EditEnvDlg.GG_RLS:

				ID2 = ID - EditEnvDlg.GG_ATK + EditEnvDlg.GG_ATKCURVE;
				ID3 = ID - EditEnvDlg.GG_ATK + EditEnvDlg.GG_ATKTIME;
				
				associate	= gui.getItemObj( ID2 );
				associate2	= gui.getItemObj( ID3 );

				if( (associate != null) && (associate2 != null) ) {

					associate.setEnabled( ((JCheckBox) e.getSource()).isSelected() );
					associate2.setEnabled( ((JCheckBox) e.getSource()).isSelected() );
				}
				break;

			case EditEnvDlg.GG_PRESETS:		// ------------------------------ Switch Preset ----------------

				associate = gui.getItemObj( EditEnvDlg.GG_PRESETS );
				if( associate != null ) {

					name = ((JComboBox) associate).getSelectedItem().toString();
					if( name != null ) {
						pa		= dlg.getPropertyArray();
						preset	= dlg.getPresets().getPreset( name );
						pa.fromProperties( false, preset );
						dlg.envelopeToGUI();

						associate = gui.getItemObj( EditEnvDlg.GG_DELPRESET );
						if( associate != null ) {				// enable or disable del
							associate.setEnabled( !name.equals( Presets.DEFAULT ));
						}
					}
				}
				break;

			default:
				break;
			}
		}

	// -------- Mouse Methoden --------

		public void mouseClicked( MouseEvent e )
		{
			int				ID = gui.getItemID( e );
			Component		associate;
			String			name;
	//		PromptDlg		prompt;
	//		ConfirmDlg		confirm;
			boolean			exists;
			int				i;
			PropertyArray	pa;
			Properties		preset;

	topLevel: switch( ID ) {
			case EditEnvDlg.GG_ADDPRESET:		// ------------------------------ Add Preset -------------------

				associate = gui.getItemObj( EditEnvDlg.GG_PRESETS );
				if( associate != null) {

					name = JOptionPane.showInputDialog( enc_this, "Enter preset name" );
					if( name != null && name.length() > 0 ) {
						if( name.equals( Presets.DEFAULT )) {
							JOptionPane.showMessageDialog( enc_this, "Cannot overwrite defaults!" );
							break topLevel;
						}

						for( i = ((JComboBox) associate).getItemCount() - 1, exists = false;
							(i >= 0) && !exists; i-- ) {
							exists = ((JComboBox) associate).getItemAt( i ).equals( name );	// existiert schon?
						}
						if( exists ) {
							i = JOptionPane.showConfirmDialog( enc_this,  "Overwrite existing preset\n\"" +
													  name + "\"", "Confirm", JOptionPane.YES_NO_OPTION );
							if( i != 0 ) break topLevel;	// nicht überschreiben
						}

						dlg.GUIToEnvelope();
						pa		= dlg.getPropertyArray();
						preset	= pa.toProperties( false );

						if( !preset.isEmpty() ) {								// erfolgreich?
		
							if( dlg.getPresets().setPreset( name, preset ) != null ) {

								if( !exists ) {
									((JComboBox) associate).addItem( name );			// neues JComboBox-Item
								}
								((JComboBox) associate).setSelectedItem( name );

								associate = gui.getItemObj( EditEnvDlg.GG_DELPRESET );
								if( associate != null ) {
									associate.setEnabled( true );		// enable del
								}
							} else {
								// vorerst keine Fehlermeldung XXX
							}
						} else {
							// vorerst keine Fehlermeldung XXX
						}
					}
				}
				break;
				
			case EditEnvDlg.GG_DELPRESET:		// ------------------------------ Delete Preset ----------------

				associate = gui.getItemObj( EditEnvDlg.GG_PRESETS );
				if( associate != null) {

					name = ((JComboBox) associate).getSelectedItem().toString();
					if( name != null ) {
						if( name.equals( Presets.DEFAULT )) {
							JOptionPane.showMessageDialog( enc_this, "Cannot delete defaults!" );
							break topLevel;
						}

						if( dlg.getPresets().removePreset( name ) != null ) {

							((JComboBox) associate).removeItem( name );
							if( ((JComboBox) associate).getItemCount() == 0 ||
								((JComboBox) associate).getSelectedItem().equals( Presets.DEFAULT )) {

								associate = gui.getItemObj( EditEnvDlg.GG_DELPRESET );
								if( associate != null ) {
									associate.setEnabled( false );		// cannot del default
								}
							}
						} else {
							// vorerst keine Fehlermeldung XXX
						}
					}
				}
				break;
				
			default:
				break;
			}
		}

		public void mousePressed( MouseEvent e ) {}
		public void mouseReleased( MouseEvent e ) {}
		public void mouseEntered( MouseEvent e ) {}
		public void mouseExited( MouseEvent e ) {}
	}
	// class EditEnvDlgListener
}
// class EditEnvDlg
