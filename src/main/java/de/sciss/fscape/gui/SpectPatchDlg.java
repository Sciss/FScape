/*
 *  SpectPatchDlg.java
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
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.session.SpectPatch;

import javax.swing.*;
import java.util.Enumeration;

/**
 *	This was the beginning of FScape: a patcher window in the
 *	Max / Reaktor style for manipulation of STFT files created
 *	by SoundHack.
 */
public class SpectPatchDlg
        extends ModulePanel {

// -------- private variables --------

    private SpectPatch	doc;

    // Properties (defaults)
    private static final int PR_DOC			= 0;		// pr.text
    private static final String PRN_DOC		= "Patcher";

    private static final String[]		prText			= { "" };
    private static final String[]		prTextName		= { PRN_DOC };

//	private static final int	GG_SCROLL	= GG_OFF_OTHER + 0;
//	private static final int	GG_DOC		= GG_OFF_OTHER + 1;

    private static	PropertyArray	static_pr		= null;
    private static	Presets			static_presets	= null;

    private OpPanel	gagaPanel	= null;

// -------- public methods --------
    // public Document getOwner();
    // public boolean newDocument();

    /**
     *	Konstruktor des Hauptfensters
     */
    public SpectPatchDlg()
    {
        super( "Spectral Patcher" );
        init2();
    }

    protected void buildGUI()
    {
        // einmalig PropertyArray initialisieren
        if( static_pr == null ) {
            static_pr			= new PropertyArray();
            static_pr.text		= prText;
            static_pr.textName	= prTextName;
//			static_pr.superPr	= DocumentFrame.static_pr;
        }
        // default preset
        if( static_presets == null ) {
            static_presets = new Presets( getClass(), static_pr.toProperties( true ));
        }
        presets	= static_presets;
        pr 		= (PropertyArray) static_pr.clone();

    // -------- build GUI --------

//		JPanel		p			= new JPanel( new BorderLayout() );
        gagaPanel				= new OpPanel( this );
//System.err.println( "BUILDING "+gagaPanel+" (me = "+this.hashCode()+")" );
        JScrollPane ggScroll	= new JScrollPane( gagaPanel );
        ggScroll.putClientProperty ( "styleId", "nofocus" );

        gui				= new GUISupport(); // not used but necessary to prevent null ptr exception

    // -------- Op Panel --------
//opPanel = new OpPanel( this );
//gui.addGadget( opPanel, GG_OFF_OTHER + 999 );

        doc = new SpectPatch( this );

        initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, ggScroll );
    }

    /**
     *	Transfer values from prop-array to GUI
     */
    public void fillGUI()
    {
        super.fillGUI();
        super.fillGUI( gui );

        doc.clear();
        doc.load( Presets.valueToProperties( pr.text[ PR_DOC ]));
    }

    /**
     *	Transfer values from GUI to prop-array
     */
    public void fillPropertyArray()
    {
        super.fillPropertyArray();
        super.fillPropertyArray( gui );

        pr.text[ PR_DOC ] = Presets.propertiesToValue( doc.save() );
    }

// -------- Processor Interface --------

    protected void process()
    {
        float				prog;
        boolean				telepathy;

topLevel:	try {
            telepathy = true;
            doc.start();
            do {
                try {
                    synchronized( this ) {
                        wait( 3000 );
                    }
                } catch( InterruptedException ignored) {}

                prog = calcProg( doc.getOperators() );

                if( prog >= 0f ) {
                    setProgression( prog );
                } else break topLevel;

                if( !doc.running ) {
                    telepathy = false;
                }
            } while( threadRunning && telepathy );

            prog = calcProg( doc.getOperators() );
            if( prog >= 0f ) setProgression( prog );

// XXX			doc.pause( false );
//			doc.pause( true );

        } catch( Exception e98 ) {
            setError( e98 );
        }

        try {
            doc.stop();
        } catch( Exception e99 ) {
            setError( e99 );
        }
    }

    protected float calcProg( Enumeration opEnum )
    {
        int		i;
        float	prog	= 0.0f;

        for( i = 0; opEnum.hasMoreElements(); i++ ) {
            prog += ((Operator) opEnum.nextElement()).getProgress();
        }

        if( i > 0 ) {
            return prog/i;
        } else {
            return -1f;
        }
    }

    /*
     *	Liefert das Operator-Panel
     *	NUR FUER LISTENER!
     */
    public OpPanel getOpPanel()
    {
//System.err.println( "RETURNING "+gagaPanel+" (me = "+this.hashCode()+")" );
//EventQueue.invokeLater( new Runnable() { public void run() { System.err.println( "BUT NOW "+gagaPanel+" (me = "+this.hashCode()+")" ); }});
        return gagaPanel;
    }

    /**
     *	Liefert das Document, dem das Fenster gehoert
     */
    public SpectPatch getDoc()
    {
        return doc;
    }

    /**
     *	Loescht den Panel-Inhalt des Documents
     *	WIRD VON DOCUMENT AUFGERUFEN
     *	altes OpPanel ist danach ungueltig!
     *
     *	@return	false, wenn erfolglos
     */
    public boolean clear()
    {
//		JScrollPane ggScroll;
//		if( gui != null ) {
//			ggScroll = (JScrollPane) gui.getItemObj( GG_SCROLL );
//			if( ggScroll != null ) {
gagaPanel.clear();
//				opPanel = new OpPanel( this );		
//				ggScroll.setViewportView( opPanel );
                return true;
//			}
//		}
//		return false;
    }
}