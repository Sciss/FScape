/*
 *  EditOpDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.fscape.op.Operator;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.util.Slots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

/**
 *	Dialog presenting the parameters of a
 *	spectral operator.
 */
public class EditOpDlg
        extends BasicDialog {

// -------- public variables --------

    public static final int GG_PRESETS		= 1;		// IDs und zugleich Hash-Keys
    public static final int GG_ADDPRESET	= 2;
    public static final int GG_DELPRESET	= 3;
    public static final int GG_SPECTROGRAM	= 4;
    public static final int GG_NORMALIZE	= 5;
    public static final int GG_OPPREFS		= 6;
    public static final int GG_OK			= 7;
    public static final int GG_CANCEL		= 8;

// -------- private variables --------

    private Operator	op;
    private PropertyGUI	opGUI;

    private boolean choice = false;			// keine Veraenderung bei Cancel noetig

    private final EditOpDlg enc_this = this;

// -------- public methods --------
    // public boolean getJComboBox();
    // public Operator getOperator();
    // public GUISupport getOpGUI();

    /**
     *	!! setVisible() und dispose() bleibt dem Aufrufer ueberlassen
     *
     *	@param parent	aufrufendes Fenster
     */
    public EditOpDlg( Component parent, Operator op )
    {
        super( parent, "Edit Operator: " + op.getIcon().getName(), true,
               op.getClass().getName().substring( op.getClass().getName().lastIndexOf( '.' ) + 1 ));
        this.op = op;

        GUISupport			gui;
        GridBagConstraints	con;
        GridBagLayout		lay;
        EditOpDlgListener 	list;
        Properties			prop;
        String				val;

        JPanel		toolBar;
        JComboBox   ggPresets;
        Iterator	presetNames;
        ToolIcon	ggAddPreset, ggDelPreset;
        JCheckBox	ggSpectrogram;

        JPanel		buttonBar;
        JButton		ggOk, ggCancel;

        gui		= new GUISupport();
        con		= gui.getGridBagConstraints();
        lay		= gui.getGridBagLayout();
        list	= new EditOpDlgListener( this, gui );
        con.insets = new Insets( 2, 2, 2, 2 );

    // -------- Toolbar --------
        con.fill = GridBagConstraints.HORIZONTAL;
        toolBar = new JPanel();
        toolBar.setLayout( new FlowLayout( FlowLayout.LEFT, 2, 2 ) );

        ggAddPreset = new ToolIcon( ToolIcon.ID_ADDPRESET, null );
        gui.registerGadget( ggAddPreset, GG_ADDPRESET );
        ggAddPreset.addMouseListener( list );
        ggDelPreset = new ToolIcon( ToolIcon.ID_DELPRESET, null );
        gui.registerGadget( ggDelPreset, GG_DELPRESET );
        ggDelPreset.addMouseListener( list );

        ggPresets = new JComboBox();
        presetNames = op.getPresets().presetNames().iterator();
        while( presetNames.hasNext() ) {
            ggPresets.addItem( (String) presetNames.next() );
        }
        ggPresets.setSelectedItem( Presets.DEFAULT );						// default waehlen und
        ggDelPreset.setEnabled( false );							// DelPreset deaktivieren
        gui.registerGadget( ggPresets, GG_PRESETS );
        ggPresets.addItemListener( list );

        ggSpectrogram = new JCheckBox( "Spectrogram" );
        gui.registerGadget( ggSpectrogram, GG_SPECTROGRAM );
        ggSpectrogram.addItemListener( list );
        // load spectro property
        prop	= op.getPropertyArray().superPr.toProperties( true );
        val		= prop.getProperty( Operator.PRN_SPECTRO );
        if( val != null ) {
            ggSpectrogram.setSelected( Boolean.valueOf( val ).booleanValue() );
        }
        if( op.getSlots( Slots.SLOTS_WRITER ).isEmpty() ) {
            ggSpectrogram.setVisible( false );
        }

        toolBar.add( new JLabel( "Preset" ));
        toolBar.add( ggAddPreset );
        toolBar.add( ggDelPreset );
        toolBar.add( ggPresets );
        toolBar.add( ggSpectrogram );
        con.gridwidth = GridBagConstraints.REMAINDER;
        lay.setConstraints( toolBar, con );
        gui.add( toolBar );

    // -------- Edit-Panel --------
        con.fill = GridBagConstraints.BOTH;
//		ggOpPrefs = new JScrollPane(); // ScrollPain( ScrollPane.SCROLLBARS_AS_NEEDED );
        opGUI = op.createGUI( Operator.GUI_PREFS );
        if( opGUI != null ) {
            opGUI.fillGUI( op.getPropertyArray() );		// load values
//			ggOpPrefs.setViewportView( opGUI );
        }
        con.weightx = 1.0;
        con.weighty = 1.0;
//		gui.addScrollPane( ggOpPrefs, GG_OPPREFS, list );
        gui.addGadget( opGUI, GG_OPPREFS );

    // -------- Ok/Cancel Gadgets --------
        con.fill = GridBagConstraints.HORIZONTAL;
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
        con.weighty = 0.0;
        lay.setConstraints( buttonBar, con );
        gui.add( buttonBar );

        getContentPane().add( gui );

        // Position und Groesse aus dem Preferences ermitteln
        setResizable( true );

        addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e )
            {
                closeMe();
            }
        });

        super.initDialog();
    }

//	protected boolean alwaysPackSize()
//	{
//		return true;
//	}

    protected void closeMe()
    {
        this.setVisible( false );
        try {
            op.getPrefs().store();			// Prefs ggf. speichern
            op.getPresets().store();		// Presets ggf. speichern
        } catch( IOException e2 ) {
            // vorerst egal XXX
            System.err.println( "error storing presets : "+e2.getLocalizedMessage() );
        }
        this.dispose();
    }

    /**
     *	Dialog-Ergebnis ermitteln
     *
     *	@return	true fuer Ok, false fuer Cancel
     */
    public boolean getChoice()
    {
        return choice;
    }

    /**
     *	Zugrundeliegenden Operator ermitteln
     */
    public Operator getOperator()
    {
        return op;
    }

    /**
     *	Operator-GUI ermitteln
     */
    public PropertyGUI getOpGUI()
    {
        return opGUI;
    }

// -------- quasi-protected methods --------

    /*
     *	NUR FUER DEN LISTENER
     */
    public void setChoice( boolean choice )
    {
        this.choice = choice;
    }

    /*
     *	Implementation der Listener
     */
    private class EditOpDlgListener
    implements	ActionListener, AdjustmentListener,
                ItemListener, MouseListener
    {
    // -------- private variables --------

        private EditOpDlg dlg;		// Edit-Dialog, der den Listener installiert
        private GUISupport gui;		// gui, dessen Objecte hier ueberwacht werden

    // -------- public methods --------

        /**
         *	@param dlg	Edit-Dialog, der den Listener installiert
         *	@param gui	zu ueberwachendes GUI
         */
        public EditOpDlgListener( EditOpDlg dlg, GUISupport gui )
        {
            this.dlg = dlg;
            this.gui = gui;
        }

    // -------- Action methods --------

        public void actionPerformed( ActionEvent e )
        {
            int			ID = gui.getItemID( e );
            Component	associate;
            Properties	prop;

            switch( ID ) {
            case EditOpDlg.GG_OK:
                dlg.setChoice( true );
                dlg.getOpGUI().fillPropertyArray( op.getPropertyArray() );
                // Spectro:
                associate = gui.getItemObj( EditOpDlg.GG_SPECTROGRAM );
                if( associate != null ) {
                    prop = new Properties();
                    prop.put( Operator.PRN_SPECTRO, String.valueOf( ((JCheckBox) associate).isSelected() ));
                    op.getPropertyArray().superPr.fromProperties( true, prop );
                }
                // THRU
            case EditOpDlg.GG_CANCEL:
                dlg.closeMe();
                break;
            default:
                break;
            }
        }

    // -------- Adjustment methods --------

        public void adjustmentValueChanged( AdjustmentEvent e ) {}

    // -------- Item methods --------

        public void itemStateChanged( ItemEvent e )
        {
            int				ID = gui.getItemID( e );
            Properties		preset;
            PropertyArray	pa;
            Component		associate;
            String			name;

            switch( ID ) {
            case EditOpDlg.GG_PRESETS:		// ------------------------------ Switch Preset ----------------

                associate = gui.getItemObj( EditOpDlg.GG_PRESETS );
                if( associate != null ) {

                    name = ((JComboBox) associate).getSelectedItem().toString();
                    if( name != null ) {
                        pa		= (PropertyArray) op.getPropertyArray().clone();
                        preset	= op.getPresets().getPreset( name );
                        pa.fromProperties( false, preset );
                        dlg.getOpGUI().fillGUI( pa );

                        associate = gui.getItemObj( EditOpDlg.GG_DELPRESET );
                        if( associate != null ) {				// enable or disable del
                            associate.setEnabled( !name.equals( Presets.DEFAULT ));
                        }
                    }
                }
                break;

            case EditOpDlg.GG_SPECTROGRAM:
//				selected = (e.getStateChange() == ItemEvent.SELECTED);
                // nothing here yet XXX
                break;

            case EditOpDlg.GG_NORMALIZE:
//				selected = (e.getStateChange() == ItemEvent.SELECTED);
                // nothing here yet XXX
                break;

            default:
                break;
            }
        }

    // -------- Mouse methods --------

        public void mouseClicked( MouseEvent e )
        {
            int				ID = gui.getItemID( e );
            Properties		preset;
            PropertyArray	pa;
            Component		associate;
            String			name;
    //		PromptDlg		prompt;
    //		ConfirmDlg		confirm;
            boolean			exists;
            int				i;

    topLevel: switch( ID ) {
            case EditOpDlg.GG_ADDPRESET:		// ------------------------------ Add Preset -------------------

                associate = gui.getItemObj( EditOpDlg.GG_PRESETS );
                if( associate != null) {

                    name = JOptionPane.showInputDialog( this, "Enter preset name" );
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
                            if( i != 0 ) break topLevel;	// nicht ueberschreiben
                        }

                        pa		= (PropertyArray) op.getPropertyArray().clone();
                        dlg.getOpGUI().fillPropertyArray( pa );
                        preset	= pa.toProperties( false );

                        if( !preset.isEmpty() ) {								// erfolgreich?

                            if( op.getPresets().setPreset( name, preset ) != null ) {

                                if( !exists ) {
                                    ((JComboBox) associate).addItem( name );			// neues JComboBox-Item
                                }
                                ((JComboBox) associate).setSelectedItem( name );

                                associate = gui.getItemObj( EditOpDlg.GG_DELPRESET );
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

            case EditOpDlg.GG_DELPRESET:		// ------------------------------ Delete Preset ----------------

                associate = gui.getItemObj( EditOpDlg.GG_PRESETS );
                if( associate != null) {

                    name = ((JComboBox) associate).getSelectedItem().toString();
                    if( name != null ) {
                        if( name.equals( Presets.DEFAULT )) {
                            JOptionPane.showMessageDialog( enc_this, "Cannot delete defaults!" );
                            break topLevel;
                        }

                        if( op.getPresets().removePreset( name ) != null ) {

                            ((JComboBox) associate).removeItem( name );
                            if( ((JComboBox) associate).getItemCount() == 0 ||
                                ((JComboBox) associate).getSelectedItem().equals( Presets.DEFAULT )) {

                                associate = gui.getItemObj( EditOpDlg.GG_DELPRESET );
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

        public void mousePressed (MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered (MouseEvent e) {}
        public void mouseExited  (MouseEvent e) {}
    }
    // class EditOpDlgListener
}