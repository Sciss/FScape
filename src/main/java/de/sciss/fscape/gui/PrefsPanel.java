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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.*;

import de.sciss.app.PreferenceEntrySync;
import de.sciss.fscape.Application;
import de.sciss.io.IOUtil;
import de.sciss.net.OSCChannel;
import de.sciss.util.Flag;
import de.sciss.util.ParamSpace;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.net.OSCRoot;
import de.sciss.gui.PrefCheckBox;
import de.sciss.gui.PrefComboBox;
import de.sciss.gui.PrefParamField;
import de.sciss.gui.SpringPanel;
import de.sciss.gui.StringItem;

/**
 *  This is the frame that
 *  displays the user adjustable
 *  application and session preferences
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.73, 09-Aug-09
 */
public class PrefsPanel
extends JPanel
{
	private static final ParamSpace	spcIntegerFromZero = new ParamSpace( 0, Double.POSITIVE_INFINITY, 1, 0, 0, 0 );
//	private static final ParamSpace	spcIntegerFromOne  = new ParamSpace( 1, Double.POSITIVE_INFINITY, 1, 0, 0, 1 );

	/**
	 *  Creates a new preferences frame
	 */
    public PrefsPanel()
    {
		super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		final Container					cp		= this; // getContentPane();
		final OSCRoot					osc;
		final Flag						haveWarned			= new Flag( false );
		final String					txtWarnLookAndFeel	=
                "For Look-and-Feel changes to take effect,\nthe application must be restarted.";
        final Preferences userPrefs = Application.userPrefs;

		JPanel							p, tabWrap;
		SpringPanel						tab;
		PrefParamField					ggParam;
		PrefPathField					ggPath;
		PrefCheckBox					ggCheckBox;
        PrefComboBox					ggChoice;
		JTabbedPane						ggTabPane;
		JLabel							lb;
        UIManager.LookAndFeelInfo[]		lafInfos;
		Box								b;

		Preferences						prefs;
		String							key, key2, title;
		int								row;

		// ggTabPane			= new JTabbedPane();

		// ---------- global pane ----------

		tab		= new SpringPanel( 2, 1, 4, 2 );

		row		= 0;
		prefs   = IOUtil.getUserPrefs();
		key		= IOUtil.KEY_TEMPDIR;
		key2	= "prefsTmpDir";
		lb		= new JLabel("Temporary Folder:", SwingConstants.TRAILING );
		tab.gridAdd( lb, 0, row );
		ggPath	= new PrefPathField( PathField.TYPE_FOLDER, "Temporary Folder");
		ggPath.setPreferences( prefs, key );
		tab.gridAdd( ggPath, 1, row );

		row++;
		prefs   = userPrefs;
		key2	= "prefsAudioFileFormat";
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
		key2	= "prefsHeadroom";
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
//		key2	= "prefsOSCActive";
		key2	= "prefsOSCServer";
		lb		= new JLabel("OSC Server:", SwingConstants.TRAILING );
		tab.gridAdd( lb, 0, row );
		b		= Box.createHorizontalBox();
		ggCheckBox = new PrefCheckBox("Active");
		ggCheckBox.setPreferences( prefs, key );
//		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
		b.add( ggCheckBox );

		key		= OSCRoot.KEY_PROTOCOL;
		key2	= "prefsOSCProtocol";
		lb		= new JLabel("Protocol:", SwingConstants.TRAILING );
//		tab.gridAdd( lb, 2, row );
		b.add( Box.createHorizontalStrut( 16 ));
		b.add( lb );
		ggChoice = new PrefComboBox();
		ggChoice.addItem( new StringItem( OSCChannel.TCP, "TCP" ));
		ggChoice.addItem( new StringItem( OSCChannel.UDP, "UDP" ));
		ggChoice.setPreferences( prefs, key );
//		tab.gridAdd( ggChoice, 3, row, -1, 1 );
		b.add( ggChoice );

		key		= OSCRoot.KEY_PORT;
		key2	= "prefsOSCPort";
		lb		= new JLabel("Port:", SwingConstants.TRAILING );
//		tab.gridAdd( lb, 4, row );
		b.add( Box.createHorizontalStrut( 16 ));
		b.add( lb );
		ggParam  = new PrefParamField();
		ggParam.addSpace( spcIntegerFromZero );
		ggParam.setPreferences( prefs, key );
//		tab.gridAdd( ggParam, 5, row, -1, 1 );
		b.add( ggParam );
		tab.gridAdd( b, 1, row, -1, 1 );

        //		row++;
        //		prefs   = userPrefs;
        //        key     = PrefsUtil.KEY_LOOKANDFEEL;
        //		key2	= "prefsLookAndFeel";
        //		title	= "Look-and-Feel:";
        //		lb		= new JLabel( title, SwingConstants.TRAILING );
        //		tab.gridAdd( lb, 0, row );
        //		ggChoice = new PrefComboBox();
        //		lafInfos = UIManager.getInstalledLookAndFeels();
        //        for( int i = 0; i < lafInfos.length; i++ ) {
        //            ggChoice.addItem( new StringItem( lafInfos[i].getClassName(), lafInfos[i].getName() ));
        //        }
        //		ggChoice.setPreferences( prefs, key );
        //		ggChoice.addActionListener( new WarnPrefsChange( ggChoice, ggChoice, haveWarned, txtWarnLookAndFeel, title ));
        //		tab.gridAdd( ggChoice, 1, row, -1, 1 );

		row++;
       	key		= "lafdecoration"; // BasicWindowHandler.KEY_LAFDECORATION;
		key2	= "prefsLAFDecoration";
		title	= "Look-and-Feel Window Decoration";
        ggCheckBox = new PrefCheckBox(title);
        ggCheckBox.setPreferences( prefs, key );
		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
		ggCheckBox.addActionListener( new WarnPrefsChange( ggCheckBox, ggCheckBox, haveWarned, txtWarnLookAndFeel, title ));

        //		row++;
        //       	key		= "internalframes"; // BasicWindowHandler.KEY_INTERNALFRAMES;
        //		key2	= "prefsInternalFrames";
        //		title	= getResourceString( key2 );
        ////		lb		= new JLabel( title, JLabel.TRAILING );
        ////		tab.gridAdd( lb, 0, row );
        //		ggCheckBox  = new PrefCheckBox( title );
        //		ggCheckBox.setPreferences( prefs, key );
        //		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
        //		ggCheckBox.addActionListener( new WarnPrefsChange( ggCheckBox, ggCheckBox, haveWarned, txtWarnLookAndFeel, title ));

        //		row++;
        //       	key		= CoverGrowBox.KEY_INTRUDINGSIZE;
        //		key2	= "prefsIntrudingSize";
        ////		lb		= new JLabel( getResourceString( key2 ), JLabel.TRAILING );
        ////		tab.gridAdd( lb, 0, row );
        //		ggCheckBox  = new PrefCheckBox( getResourceString( key2 ));
        //		ggCheckBox.setPreferences( prefs, key );
        //		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );

        //		row++;
        //       	key		= "floatingpalettes"; // BasicWindowHandler.KEY_FLOATINGPALETTES;
        //		key2	= "prefsFloatingPalettes";
        ////		lb		= new JLabel( getResourceString( key2 ), JLabel.TRAILING );
        ////		tab.gridAdd( lb, 0, row );
        //		ggCheckBox  = new PrefCheckBox( getResourceString( key2 ));
        //		ggCheckBox.setPreferences( prefs, key );
        //		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
        //		ggCheckBox.addActionListener( new WarnPrefsChange( ggCheckBox, ggCheckBox, haveWarned, txtWarnLookAndFeel, title ));

//		row++;
//  	prefs   = GUIUtil.getUserPrefs();
//		key2	= "prefsKeyStrokeHelp";
//		lb		= new JLabel( getResourceString( key2 ), JLabel.TRAILING );
//		tab.gridAdd( lb, 0, row );
//		ggKeyStroke = new KeyStrokeTextField();
//		ggKeyStroke.setPreferences( prefs, key );
//		tab.gridAdd( ggKeyStroke, 1, row, -1, 1 );
		
 		key2	= "prefsGeneral";
		tab.makeCompactGrid();
		tabWrap = new JPanel( new BorderLayout() );
		tabWrap.add( tab, BorderLayout.NORTH );
		p		= new JPanel( new FlowLayout( FlowLayout.RIGHT ));
		// p.add( new HelpButton( key2 ));
		tabWrap.add( p, BorderLayout.SOUTH );

        // ggTabPane.addTab("General", null, tabWrap, null);

		// ---------- generic gadgets ----------

//		ggButton	= new JButton( getResourceString( "buttonClose" ));
//		buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 4, 4 ));
//		buttonPanel.add( ggButton );
//		ggButton.addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent newEvent )
//			{
//				setVisible( false );
//				dispose();
//			}	
//		});
//		if( app.getUserPrefs().getBoolean( PrefsUtil.KEY_INTRUDINGSIZE, false )) {
//			buttonPanel.add( Box.createHorizontalStrut( 16 ));
//		}

		cp.add( tabWrap /* ggTabPane */, BorderLayout.CENTER );

		// ---------- listeners ----------
		
//		addListener( new AbstractWindow.Adapter() {
//			public void windowClosing( AbstractWindow.Event e )
//			{
//				setVisible( false );
//				dispose();
//			}
//		});

		// setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		// init();
		// app.addComponent( Main.COMP_PREFS, this );
    }

    //	public void dispose()
    //	{
    //		super.dispose();
    //	}

    //	private static String getResourceString( String key )
    //	{
    //		return key;
    //	}

	private static class WarnPrefsChange
	implements ActionListener
	{
		private final PreferenceEntrySync	pes;
		private final Component				c;
		private final Flag					haveWarned;
		private final String				text;
		private final String				title;
		private final String				initialValue;
	
		protected WarnPrefsChange( PreferenceEntrySync pes, Component c, Flag haveWarned, String text, String title )
		{
			this.pes		= pes;
			this.c			= c;
			this.haveWarned	= haveWarned;
			this.text		= text;
			this.title		= title;
			
			initialValue	= pes.getPreferenceNode().get( pes.getPreferenceKey(), null );
		}

		public void actionPerformed( ActionEvent e )
		{
			final String newValue = pes.getPreferenceNode().get( pes.getPreferenceKey(), initialValue );
		
			if( !newValue.equals( initialValue ) && !haveWarned.isSet() ) {
				JOptionPane.showMessageDialog( c, text, title, JOptionPane.INFORMATION_MESSAGE );
				haveWarned.set( true );
			}
		}
	}
}