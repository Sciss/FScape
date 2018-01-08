/*
 *  PrefPathField.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.app.LaterInvocationManager;
import de.sciss.app.PreferenceEntrySync;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;

import java.io.File;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 *  Subclass of PathField
 *  that enables automatic
 *  Preferences association
 */
public class PrefPathField
        extends PathField
        implements DynamicListening, PreferenceChangeListener,
        LaterInvocationManager.Listener, PreferenceEntrySync {

    private boolean							listening		= false;
    private Preferences						prefs			= null;
    private String							key				= null;
    private final LaterInvocationManager	lim				= new LaterInvocationManager( this );

    private PathListener listener;
    private File							defaultValue	= null;
    private boolean							readPrefs		= true;
    protected boolean						writePrefs		= true;

    /**
     *  Constructs a new <code>PrefPathField</code>.
     *
     *  @param  type		type of path field, e.g. TYPE_INPUTFILE.
     *  @param  dlgTxt		text for the file chooser dialog or null
     *  @synchronization	Like any other Swing component,
     *						the constructor is to be called
     *						from the event thread.
     */
    public PrefPathField( int type, String dlgTxt )
    {
        super( type, dlgTxt );

        new DynamicAncestorAdapter( this ).addTo( this );
        listener = new PathListener() {
            public void pathChanged( PathEvent e )
            {
                if( writePrefs ) writePrefs();
            }
        };
    }

    public void setReadPrefs( boolean b )
    {
        if( b != readPrefs ) {
            readPrefs	= b;
            if( (prefs != null) && listening ) {
                if( readPrefs ) {
                    prefs.addPreferenceChangeListener( this );
                } else {
                    prefs.removePreferenceChangeListener( this );
                }
            }
        }
    }

    public boolean getReadPrefs()
    {
        return readPrefs;
    }

    public void setWritePrefs( boolean b )
    {
        if( b != writePrefs ) {
            writePrefs	= b;
            if( (prefs != null) && listening ) {
                if( writePrefs ) {
                    this.addPathListener( listener );
                } else {
                    this.removePathListener( listener );
                }
            }
        }
    }

    public boolean getWritePrefs()
    {
        return writePrefs;
    }

    public void setPath( File f )
    {
        super.setPath( f );
        if( writePrefs ) writePrefs();
    }

    public void writePrefs()
    {
        if( (prefs != null) && (key != null) ) {
            String oldValue		= prefs.get( key, "" );
            File prefsPath 		= new File( oldValue );
            final File guiPath	= getPath();
            if( !guiPath.equals( prefsPath )) {
                prefs.put( key, guiPath.getPath() );
            }
        }
    }

    public void setPreferenceNode( Preferences prefs )
    {
        setPreferences( prefs, this.key );
    }

    public void setPreferenceKey( String key )
    {
        setPreferences( this.prefs, key );
    }

    /**
     *  Enable Preferences synchronization.
     *  This method is not thread safe and
     *  must be called from the event thread.
     *  When a preference change is received,
     *  the path is updated in the GUi and
     *  the PathField dispatches a PathEvent.
     *  Likewise, if the user adjusts the path
     *  in the GUI, the preference will be
     *  updated. The same is true, if you
     *  call setPath.
     *
     *  @param  prefs   the preferences node in which
     *					the value is stored, or null
     *					to disable prefs sync.
     *  @param  key		the key used to store and recall
     *					prefs. the value is the path
     *					converted to a string.
     */
    public void setPreferences( Preferences prefs, String key )
    {
        if( (this.prefs == null) || (this.key != null) ) {
            defaultValue = getPath();
        }
        if( listening ) {
            stopListening();
            this.prefs  = prefs;
            this.key	= key;
            startListening();
        } else {
            this.prefs  = prefs;
            this.key	= key;
        }
    }

    public Preferences getPreferenceNode() { return prefs; }
    public String getPreferenceKey() { return key; }

    public void startListening()
    {
        if( prefs != null ) {
            listening = true;
            if( writePrefs ) this.addPathListener( listener );
            if( readPrefs ) {
                prefs.addPreferenceChangeListener( this );
                readPrefs();
            }
        }
    }

    public void stopListening()
    {
        if( prefs != null ) {
            if( readPrefs ) prefs.removePreferenceChangeListener( this );
            if( writePrefs ) this.removePathListener( listener );
            listening = false;
        }
    }

    // o instanceof PreferenceChangeEvent
    public void laterInvocation( Object o )
    {
        String prefsValue   = ((PreferenceChangeEvent) o).getNewValue();
        readPrefsFromString( prefsValue );
    }

    public void readPrefs()
    {
        if( (prefs != null) && (key != null) ) readPrefsFromString( prefs.get( key, null ));
    }

    private void readPrefsFromString( String prefsValue )
    {
        if( prefsValue == null ) {
            if( defaultValue != null ) {
                setPath( defaultValue );
                if( writePrefs ) writePrefs();
            }
            return;
        }

        final File prefsPath = new File( prefsValue );

//System.err.println( "lim : "+prefsPath );
        if( !prefsPath.equals( getPath() )) {
            if( listening && writePrefs ) this.removePathListener( listener );
            setPathAndDispatchEvent( prefsPath );
            if( listening && writePrefs ) this.addPathListener( listener );
        }
    }

    public void preferenceChange( PreferenceChangeEvent e )
    {
//System.err.println( "preferenceChange : "+e.getKey()+" = "+e.getNewValue() );
        if( e.getKey().equals( key )) {
            lim.queue( e );
        }
    }
}