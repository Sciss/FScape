/*
 *  PrefsFrame.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Change log:
 *		20-May-05	created from de.sciss.meloncillo.gui.PrefsFrame (replaces
 *					former PrefsDlg)
 */

package de.sciss.fscape.gui;

import de.sciss.app.PreferenceEntrySync;
import de.sciss.fscape.Application;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.net.OSCRoot;
import de.sciss.fscape.util.PrefsUtil;
import de.sciss.gui.PrefCheckBox;
import de.sciss.gui.PrefComboBox;
import de.sciss.gui.PrefParamField;
import de.sciss.gui.SpringPanel;
import de.sciss.gui.StringItem;
import de.sciss.io.IOUtil;
import de.sciss.net.OSCChannel;
import de.sciss.util.Flag;
import de.sciss.util.ParamSpace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

/**
 *  This is the frame that
 *  displays the user adjustable
 *  application and session preferences
 */
public class PrefsPanel
        extends JPanel {
    private static final ParamSpace spcIntegerFromZero = new ParamSpace(0, Double.POSITIVE_INFINITY, 1, 0, 0, 0);

    /**
     *  Creates a new preferences frame
     */
    public PrefsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final Container					cp		= this; // getContentPane();
        final OSCRoot					osc;
        final Flag						haveWarned			= new Flag( false );
        final String					txtWarnLookAndFeel	=
                "For Look-and-Feel changes to take effect,\nthe application must be restarted.";
        final Preferences userPrefs = Application.userPrefs;

        JPanel							p, tabWrap;
        SpringPanel tab;
        PrefParamField ggParam;
        PrefPathField					ggPath;
        PrefCheckBox ggCheckBox;
        PrefComboBox ggChoice;
        JTabbedPane						ggTabPane;
        JLabel							lb;
        UIManager.LookAndFeelInfo[]		lafInfos;
        Box								b;

        Preferences						prefs;
        String							key, title;
        int								row;

        // ggTabPane			= new JTabbedPane();

        // ---------- global pane ----------

        tab		= new SpringPanel( 2, 1, 4, 2 );

        row		= 0;
        prefs   = IOUtil.getUserPrefs();
        key		= IOUtil.KEY_TEMPDIR;
        lb		= new JLabel("Temporary Folder:", SwingConstants.TRAILING );
        tab.gridAdd( lb, 0, row );
        ggPath	= new PrefPathField( PathField.TYPE_FOLDER, "Temporary Folder");
        ggPath.setPreferences( prefs, key );
        tab.gridAdd( ggPath, 1, row );

        row++;
        prefs   = userPrefs;
        lb		= new JLabel("Default Audio File Format:", SwingConstants.TRAILING );
        tab.gridAdd( lb, 0, row );
        b		= Box.createHorizontalBox();
        ggChoice = new PrefComboBox();
        for( int i = 0; i < GenericFile.TYPES_SOUND.length; i++ ) {
            ggChoice.addItem( new StringItem( GenericFile.getFileTypeStr( GenericFile.TYPES_SOUND[ i ]), GenericFile.getTypeDescr( GenericFile.TYPES_SOUND[ i ])));
        }
        key		= "audioFileType";
        ggChoice.setPreferences( prefs, key );
        b.add( ggChoice );
        ggChoice = new PrefComboBox();
        for( int i = 0; i < PathField.SNDRES_NUM; i++ ) {
            ggChoice.addItem( new StringItem( PathField.getSoundResID( i ), PathField.getSoundResDescr( i )));
        }
        key		= "audioFileRes";
        ggChoice.setPreferences( prefs, key );
        b.add( ggChoice );
        ggChoice = new PrefComboBox();
        for( int i = 0; i < PathField.SNDRATE_NUM; i++ ) {
            ggChoice.addItem( new StringItem( PathField.getSoundRateID( i ), PathField.getSoundRateDescr( i )));
        }
        key		= "audioFileRate";
        ggChoice.setPreferences( prefs, key );
        b.add( ggChoice );
        tab.gridAdd( b, 1, row, -1, 1 );

        row++;
        prefs   = userPrefs;
        key		= "headroom";
        lb		= new JLabel("Default Headroom:", SwingConstants.TRAILING );
        tab.gridAdd( lb, 0, row );
        ggParam  = new PrefParamField();
        ggParam.addSpace( ParamSpace.spcAmpDecibels );
        ggParam.setPreferences( prefs, key );
        tab.gridAdd( ggParam, 1, row, -1, 1 );

        row++;
        osc		= OSCRoot.getInstance();
        prefs   = osc.getPreferences();
        key		= OSCRoot.KEY_ACTIVE;
        lb		= new JLabel("OSC Server:", SwingConstants.TRAILING );
        tab.gridAdd( lb, 0, row );
        b		= Box.createHorizontalBox();
        ggCheckBox = new PrefCheckBox("Active");
        ggCheckBox.setPreferences( prefs, key );
        b.add( ggCheckBox );

        key		= OSCRoot.KEY_PROTOCOL;
        lb		= new JLabel("Protocol:", SwingConstants.TRAILING );
        b.add( Box.createHorizontalStrut( 16 ));
        b.add( lb );
        ggChoice = new PrefComboBox();
        ggChoice.addItem( new StringItem( OSCChannel.TCP, "TCP" ));
        ggChoice.addItem( new StringItem( OSCChannel.UDP, "UDP" ));
        ggChoice.setPreferences( prefs, key );
        b.add( ggChoice );

        key		= OSCRoot.KEY_PORT;
        lb		= new JLabel("Port:", SwingConstants.TRAILING );
        b.add( Box.createHorizontalStrut( 16 ));
        b.add( lb );
        ggParam  = new PrefParamField();
        ggParam.addSpace( spcIntegerFromZero );
        ggParam.setPreferences( prefs, key );
        b.add( ggParam );
        tab.gridAdd( b, 1, row, -1, 1 );

        row++;
        prefs   = userPrefs;
        key		= PrefsUtil.KEY_LAF_TYPE;
        title	= "Look-and-Feel";
        ggChoice = new PrefComboBox();
        ggChoice.addItem(new StringItem(PrefsUtil.VALUE_LAF_TYPE_NATIVE      , "Native"));
        ggChoice.addItem(new StringItem(PrefsUtil.VALUE_LAF_TYPE_METAL       , "Metal"));
        ggChoice.addItem(new StringItem(PrefsUtil.VALUE_LAF_TYPE_SUBMIN_LIGHT, "Submin Light"));
        ggChoice.addItem(new StringItem(PrefsUtil.VALUE_LAF_TYPE_SUBMIN_DARK , "Submin Dark"));
        ggChoice.setPreferences(prefs, key);
        tab.gridAdd(ggChoice, 1, row, -1, 1);
        ggChoice.addActionListener(new WarnPrefsChange(ggChoice, ggChoice, haveWarned, txtWarnLookAndFeel, title));

        row++;
        prefs   = userPrefs;
        key		= PrefsUtil.KEY_LAF_WINDOWS;
        title	= "Look-and-Feel Window Decoration";
        ggCheckBox = new PrefCheckBox(title);
        ggCheckBox.setPreferences( prefs, key );
        tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
        ggCheckBox.addActionListener(new WarnPrefsChange(ggCheckBox, ggCheckBox, haveWarned, txtWarnLookAndFeel, title));

        tab.makeCompactGrid();
        tabWrap = new JPanel( new BorderLayout() );
        tabWrap.add( tab, BorderLayout.NORTH );
        p		= new JPanel( new FlowLayout( FlowLayout.RIGHT ));
        tabWrap.add( p, BorderLayout.SOUTH );

        cp.add( tabWrap /* ggTabPane */, BorderLayout.CENTER );
    }

    private static class WarnPrefsChange
            implements ActionListener {

        private final PreferenceEntrySync pes;
        private final Component c;
        private final Flag      haveWarned;
        private final String    text;
        private final String    title;
        private final String    initialValue;

        protected WarnPrefsChange(PreferenceEntrySync pes, Component c, Flag haveWarned, String text, String title) {
            this.pes        = pes;
            this.c          = c;
            this.haveWarned = haveWarned;
            this.text       = text;
            this.title      = title;

            initialValue    = pes.getPreferenceNode().get(pes.getPreferenceKey(), null);
        }

        public void actionPerformed(ActionEvent e) {
            final String newValue = pes.getPreferenceNode().get(pes.getPreferenceKey(), initialValue);

            if (!newValue.equals(initialValue) && !haveWarned.isSet()) {
                EventQueue.invokeLater ( new Runnable() {
                    public void run ()
                    {
                        JOptionPane.showMessageDialog(c, text, title, JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                haveWarned.set(true);
            }
        }
    }
}