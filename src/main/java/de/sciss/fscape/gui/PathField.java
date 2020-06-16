/*
 *  PathField.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		07-Jan-05	fillStream( AudioStream ) sets type field
 *		21-May-05	fillStream( AudioFileDescr ) will write the file field
 */

package de.sciss.fscape.gui;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.fscape.Application;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.io.ImageStream;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.util.Util;
import de.sciss.gui.ColouredTextField;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFileDescr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 *	GUI component containing a path string,
 *	a path selector icon (which is also a
 *	drag target for the Finder) and optional
 *	info fields and type selectors.
 */
public class PathField
		extends JPanel
		implements ActionListener, ComponentListener, ItemListener, PathListener,
		EventManager.Processor {

// -------- public variables --------

    public static final int TYPE_BASICMASK	= 0x0F;
    /**
     *	Datei zum Einlesen
     */
    public static final int TYPE_INPUTFILE	= 0;
    /**
     *	OR fuer TYPE_INPUTFILE/OUTPUT_FILE
     */
    public static final int TYPE_FORMATFIELD= 0x10;
    /**
     *	OR fuer TYPE_OUTPUT_FILE
     */
    public static final int TYPE_RESFIELD	= 0x20;
    public static final int TYPE_RATEFIELD	= 0x40;
    /**
     *	Dateiname fuer Schreibdatei
     */
    public static final int TYPE_OUTPUTFILE	= 1;
    /**
     *	Directory
     */
    public static final int TYPE_FOLDER		= 2;

    public static final int SNDRES_16		= 0;
    public static final int SNDRES_24		= 1;
    public static final int SNDRES_32F		= 2;
    public static final int SNDRES_32		= 3;
    public static final int SNDRES_NUM		= 4;

    public static final int SNDRATE_96		= 0;
    public static final int SNDRATE_48		= 1;
    public static final int SNDRATE_44		= 2;
    public static final int SNDRATE_32		= 3;
    public static final int SNDRATE_NUM		= 4;

    public static final int IMGRES_8		= 0;
    public static final int IMGRES_16		= 1;

// -------- private variables --------

    private static final float[] sndRate	= { 96000.0f, 48000.0f, 44100.0f, 32000.0f };
//	private static final float[] movRate	= { 8.0f, 10.0f, 12.0f, 15.0f, 24.0f, 25.0f, 29.97f, 30.0f };
    private static final float[] spectRate	= sndRate;

    private static final String[] sndResID	= { "int16", "int24", "int32", "float32" };
    private static final String[] sndResTxt	= { "16-bit int", "24-bit int", "32-bit float", "32-bit int" };
    private static final String[] sndRateTxt= { "96 kHz", "48 kHz", "44.1 kHz", "32 kHz" };
    private static final String[] spectRateTxt= sndRateTxt;
    private static final String[] imgResTxt	= { "8-bit", "16-bit" };
    private static final String[] movRateTxt= { "8 fps", "10 fps", "12 fps", "15 fps", "24 fps",
                                                "25 fps", "29.97 fps", "30 fps" };

    private IOTextField			ggPath;
//	private PathIcon			ggChoose;
    private SelectPathButton			ggChoose;
    private ColouredTextField	ggFormat	= null;
    private VirtualChoice		ggType		= null;
    private VirtualChoice		ggRes		= null;
    private VirtualChoice		ggRate		= null;

    private static final Color  COLOR_ERR   = new Color( 0xFF, 0x00, 0x00, 0x2F );
    private static final Color  COLOR_EXISTS= new Color( 0x00, 0x00, 0xFF, 0x2F );
    private static final Color  COLOR_PROPSET=new Color( 0x00, 0xFF, 0x00, 0x2F );

//	private static final int MAXRESNUM	= 3;
//	private static final int MAXRATENUM	= 4;

//	protected Vector collListeners  = new Vector();
    protected final Vector<PathField> collChildren = new Vector<PathField>();

    private int					type;
    private int					handledTypes[]	= null;	// the ones that we can handle (auto setFormat)
//	private String				dlgTxt;

    private String				scheme;
    private String				protoScheme;
    private PathField			superPaths[];

    private boolean					init		= true;
    private boolean					enabled		= true;

    // constants for abbreviate
    protected static final int ABBR_LENGTH = 12;

    private final EventManager elm = new EventManager(this);

// -------- public methods --------

    public PathField(int type, String dlgTxt) {
        super();
        this.type		= type;
//		this.dlgTxt		= dlgTxt;

        final GridBagLayout       lay = new GridBagLayout();
        final GridBagConstraints  con = new GridBagConstraints();
        ggPath			= new IOTextField();
        ggChoose        = new SelectPathButton(type, dlgTxt);
        ggChoose.addPathListener( this );

        setLayout(lay);
        con.fill		= GridBagConstraints.HORIZONTAL;

        con.anchor		= GridBagConstraints.WEST;
//		con.gridheight	= 1;
        con.gridwidth	= GridBagConstraints.RELATIVE;
        con.gridy		= 1;
        con.weightx		= 1.0;
        con.weighty		= 0.0;
        lay.setConstraints(ggPath, con);
        ggPath.addActionListener( this );		// High-Level Events: Return-Hit weiterleiten
        add(ggPath);

        if ((type & TYPE_FORMATFIELD) != 0) {
            con.gridx		= 0;
            con.gridy		= 2;
            con.gridwidth	= 1;
            if( (type & TYPE_BASICMASK) == TYPE_INPUTFILE ) {
                ggFormat	= new ColouredTextField();
                ggFormat.setEditable (false);
                ggFormat.setFocusable(false);
                // ggFormat.setBackground( null );
//				ggFormat.setPaint( null );
//				con.gridheight	= 1;
                lay.setConstraints(ggFormat, con);
                add(ggFormat);
            } else {
                ggType			= new VirtualChoice();
                con.weightx		= 0.3;
                lay.setConstraints(ggType, con);
                add(ggType);
//				ggType.addItemListener( this );
                ggType.addSpecialItemListener(this);
                if ((type & TYPE_RESFIELD) != 0) {
                    ggRes = new VirtualChoice();
                    con.gridx++;
                    lay.setConstraints(ggRes, con);
                    add(ggRes);
//					ggRes.addItemListener( this );
                }
                if ((type & TYPE_RATEFIELD) != 0) {
                    ggRate = new VirtualChoice();
                    con.gridx++;
                    lay.setConstraints(ggRate, con);
                    add(ggRate);
//					ggRate.addItemListener( this );
                }
            }
            con.gridx++;
            con.gridheight	= 3;
            con.anchor		= GridBagConstraints.NORTHWEST;
        } else {
            con.gridheight	= 2;
        }

//		con.gridwidth	= GridBagConstraints.REMAINDER;
        con.gridy		= 0;
        con.weightx		= 0.0;
        con.weighty		= 1.0;
        lay.setConstraints(ggChoose, con);
        add(ggChoose);
//        final JButton ggTest = new JButton(">");
//        con.gridx++;
//        lay.setConstraints(ggChoose, con);
//        add(ggTest);

        deriveFrom(new PathField[0], (ggType != null) ? "$E" : "");
        addComponentListener(this);

        this.addPropertyChangeListener("font", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                Font fnt = ((PathField) e.getSource()).getFont();
                ggPath.setFont(fnt);
                if (ggFormat != null) ggFormat.setFont(fnt);
            }
        });
    }

    public static String getSoundResID(int idx) {
        return sndResID[idx];
    }

    public static int getSoundResIdx(String id) {
        for (int idx = 0; idx < sndResID.length; idx++) {
            if (sndResID[idx].equals(id)) return idx;
        }
        return -1;
    }

    public static String getSoundResDescr(int idx) {
        return sndResTxt[idx];
    }

    public static String getSoundRateID(int idx) {
        return String.valueOf(sndRate[idx]);
    }

    public static int getSoundRateIdx(String id) {
        for (int idx = 0; idx < sndRate.length; idx++) {
            if (String.valueOf(sndRate[idx]).equals(id)) return idx;
        }
        return -1;
    }

    public static String getSoundRateDescr(int idx) {
        return sndRateTxt[idx];
    }

    public void setPath(File path) {
        setPathIgnoreScheme(path);
        scheme = createScheme(path.getPath());
    }

    protected void setPathIgnoreScheme(File path) {
        ggPath.setText(path.getPath());
        ggChoose.setPath(path);
        synchronized (collChildren) {
            for (int i = 0; i < collChildren.size(); i++) {
                collChildren.get(i).motherSpeaks(path);
            }
        }
        feedback();
    }

    protected void setPathAndDispatchEvent(File path) {
        setPathIgnoreScheme(path);
        elm.dispatchEvent(new PathEvent(this, PathEvent.CHANGED, System.currentTimeMillis(), path));
    }

    public File getPath() {
        return (new File(ggPath.getText()));
    }

    public void setFormat(String txt) {
        if (ggFormat != null) {
            ggFormat.setText(txt);
        }
    }

    public String getFormat() {
        if (ggFormat != null) {
            return ggFormat.getText();
        } else {
            return null;
        }
    }

    /**
     *	Fill AudioFileDescr or ImageStream with
     *	data corresponding to Resolution + Rate
     */
    public void fillStream(AudioFileDescr afd) {
        int idx;

        afd.file = getPath();

        if (ggType != null) {
            afd.type = GenericFile.getAudioFileType(getType());
        }
        if (ggRes != null) {
            idx = ggRes.getSelectedIndex();
            switch (idx) {
                case SNDRES_16:
                    afd.bitsPerSample   = 16;
                    afd.sampleFormat    = AudioFileDescr.FORMAT_INT;
                    break;
                case SNDRES_24:
                    afd.bitsPerSample   = 24;
                    afd.sampleFormat    = AudioFileDescr.FORMAT_INT;
                    break;
                case SNDRES_32F:
                    afd.bitsPerSample   = 32;
                    afd.sampleFormat    = AudioFileDescr.FORMAT_FLOAT;
                    break;
                case SNDRES_32:
                    afd.bitsPerSample   = 32;
                    afd.sampleFormat    = AudioFileDescr.FORMAT_INT;
                    break;
            }
        }
        if (ggRate != null) {
            idx = ggRate.getSelectedIndex();
            if ((idx >= 0) && (idx < sndRate.length)) {
                afd.rate = sndRate[idx];
            }
        }
    }

    /**
     *	Fill AudioFileDescr or ImageStream with
     *	data corresponding to Resolution + Rate
     */
    public void fillStream(SpectStream stream) {
        int ID;

        if (ggRate != null) {
            ID = ggRate.getSelectedIndex();
            if ((ID >= 0) && (ID < spectRate.length)) {
                stream.smpRate = spectRate[ID];
            }
        }
    }

    public void fillStream(ImageStream stream) {
        int ID;

        if (ggRes != null) {
            ID = ggRes.getSelectedIndex();
            switch (ID) {
                case IMGRES_8:
                    stream.bitsPerSmp = 8;
                    break;
                case IMGRES_16:
                    stream.bitsPerSmp = 16;
                    break;
            }
        }
    }

    public int getType() {
        if (ggType != null) {
            return GenericFile.getType(ggType.getSelectedItem().toString());
        } else {
            return GenericFile.MODE_GENERIC;
        }
    }

    public JComboBox getTypeGadget()	{ return ggType; }
    public JComboBox getResGadget()		{ return ggRes ; }
    public JComboBox getRateGadget()	{ return ggRate; }

    /**
     *  PathField offers a mechanism to automatically derive
     *  a path name from a "mother" PathField. This applies
     *  usually to output files whose names are derived from
     *  PathFields which represent input paths. The provided
     *  'scheme' String can contain the Tags
     *
     *  $Dx = Directory of superPath x; $Fx = Filename; $E = Extension; $Bx = Brief filename
     *
     *  where 'x' is the index in the provided array of
     *  mother PathFields. Whenever the mother contents
     *  changes, the child PathField will recalculate its
     *  name. When the user changes the contents of the child
     *  PathField, an algorithm tries to find out which components
     *  are related to the mother's pathname, parts that cannot
     *  be identified will not be automatically changing any more
     *  unless the user completely clears the PathField (i.e.
     *  restores full automation).
     *
     *  The user can abbreviate or extend filenames by pressing the appropriate
     *  key; in this case the $F and $B tags are exchanged in the scheme.
     */
    public void deriveFrom(PathField[] superPth, String schm) {
        superPaths  = superPth;
        scheme      = schm;
        protoScheme = schm;

        for (int i = 0; i < superPth.length; i++) {
            superPth[i].addChildPathField(this);
        }
    }

    protected void addChildPathField(PathField child) {
        synchronized (collChildren) {
            if (!collChildren.contains(child)) collChildren.add(child);
        } // synchronized( collChildren )
    }

    protected void motherSpeaks(File superPath) {
        setPathAndDispatchEvent(new File(evalScheme(scheme)));
    }

    public void setEnabled(boolean state) {
        if (state == enabled) return;
        enabled = state;

        if (!state) {
            ggChoose.requestFocus();    // tricky ggPath.looseFocus() ;)
        }
        ggPath  .setEnabled(state);
        ggChoose.setEnabled(state);
        if (ggType != null) ggType.setEnabled(state);
        if (ggRes  != null) ggRes .setEnabled(state);
        if (ggRate != null) ggRate.setEnabled(state);

        feedback();

        if (state) {
            ggPath.requestFocus();
        }
    }

    public void addPathListener(PathListener list) {
        elm.addListener(list);
    }

    public void removePathListener(PathListener list) {
        elm.removeListener(list);
    }

    public void processEvent(BasicEvent e) {
        PathListener listener;

        for (int i = 0; i < elm.countListeners(); i++) {
            listener = (PathListener) elm.getListener(i);
            switch (e.getID()) {
                case PathEvent.CHANGED:
                    listener.pathChanged((PathEvent) e);
                    break;
                default:
                    assert false : e.getID();
            }
        } // for( i = 0; i < elm.countListeners(); i++ )
    }

    public void handleTypes(int types[]) {
        handledTypes = types;
        if ((type & TYPE_BASICMASK) == TYPE_OUTPUTFILE) {
            ggType.removeAllItems();
            for (int i = 0; i < types.length; i++) {
                ggType.addItem(GenericFile.getTypeDescr(types[i]));
            }
        }
    }

    /**
     *	For Convenience a two-dimensional version
     */
    public void handleTypes(int types[][]) {
        int i, j, num;
        int[] unitedTypes;

        for (i = 0, num = 0; i < types.length; i++) {
            num += types[i].length;
        }
        unitedTypes = new int[num];
        for (i = 0, j = 0; i < types.length; i++) {
            for (num = 0; num < types[i].length; num++) {
                unitedTypes[j++] = types[i][num];
            }
        }
        handleTypes(unitedTypes);
    }

// -------- private methods --------

    protected void calcFormat() {
        String fPath = getPath().getPath();
        GenericFile f;
        int typ;
        String typeStr = null;
        boolean success = false;

        if ((fPath.length() > 0) && enabled) {
            try {
                f = new GenericFile(fPath, GenericFile.MODE_INPUT);
                typ = f.mode & GenericFile.MODE_TYPEMASK;
                typeStr = f.getTypeDescr();
                for (int i = 0; i < handledTypes.length; i++) {
                    if (typ == handledTypes[i]) {
//						setFormat( typeStr + "; " + f.getFormat() );
                        setFormat(f.getFormat());
                        success = true;
                        break;
                    }
                }
                if (!success) setFormat("Wrong format - " + typeStr);
                f.cleanUp();
            } catch (IOException e1) {
                setFormat(((typeStr == null) ? "" : (typeStr + "; ")) +
                        "I/O Error: " + e1.getMessage());
            }
        } else {
            setFormat("");
            success = true;
        }
        ggFormat.setPaint(success ? null : COLOR_ERR);
    }

    protected void checkExist() {
        String fPath = getPath().getPath();
        boolean exists = false;
        Color c;

        if (fPath.length() > 0) {
            try {
                exists = new File(fPath).isFile();
            } catch (SecurityException e) {
                // ignore
            }
        }
        c = exists && enabled ? COLOR_EXISTS : null;
        if (c != ggPath.getPaint()) {
            ggPath.setPaint(c);
        }
    }

    /*
     *	Tags: $Dx = Directory of superPath x; $Fx = Filename; $E = Extension; $Bx = Brief filename
     */
    protected String evalScheme(String schm) {
        String txt2;
        int i, j, k;

        for (i = schm.indexOf("$D"); (i >= 0) && (i < schm.length() - 2); i = schm.indexOf("$D", i)) {
            j = schm.charAt(i + 2) - 48;
            try {
                txt2 = superPaths[j].getPath().getPath();
            } catch (ArrayIndexOutOfBoundsException e1) {
                txt2 = "";
            }
            // sucky java 1.1 stringbuffer is impotent
            schm = schm.substring(0, i) + txt2.substring(0, txt2.lastIndexOf(File.separatorChar) + 1) +
                    schm.substring(i + 3);
        }
        for (i = schm.indexOf("$F"); (i >= 0) && (i < schm.length() - 2); i = schm.indexOf("$F", i)) {
            j = schm.charAt(i + 2) - 48;
            try {
                txt2 = superPaths[j].getPath().getPath();
            } catch (ArrayIndexOutOfBoundsException e1) {
                txt2 = "";
            }
            txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1);
            k = txt2.lastIndexOf('.');
            schm = schm.substring(0, i) + ((k > 0) ? txt2.substring(0, k) : txt2) +
                    schm.substring(i + 3);
        }
        for (i = schm.indexOf("$X"); (i >= 0) && (i < schm.length() - 2); i = schm.indexOf("$X", i)) {
            j = schm.charAt(i + 2) - 48;
            try {
                txt2 = superPaths[j].getPath().getPath();
            } catch (ArrayIndexOutOfBoundsException e1) {
                txt2 = "";
            }
            txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1);
            k = txt2.lastIndexOf('.');
            schm = schm.substring(0, i) + ((k > 0) ? txt2.substring(k) : "") +
                    schm.substring(i + 3);
        }
        for (i = schm.indexOf("$B"); (i >= 0) && (i < schm.length() - 2); i = schm.indexOf("$B", i)) {
            j = schm.charAt(i + 2) - 48;
            try {
                txt2 = superPaths[j].getPath().getPath();
            } catch (ArrayIndexOutOfBoundsException e1) {
                txt2 = "";
            }
            txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1);
            k = txt2.lastIndexOf('.');
            txt2 = abbreviate((k > 0) ? txt2.substring(0, k) : txt2);
            schm = schm.substring(0, i) + txt2 + schm.substring(i + 3);
        }
        for (i = schm.indexOf("$E"); i >= 0; i = schm.indexOf("$E", i)) {
            j = getType();
            schm = schm.substring(0, i) + GenericFile.getExtStr(j) + schm.substring(i + 2);
        }

        return schm;
    }

    /**
     *  A filename will be abbrevated. This is not so
     *  critical on MacOS X any more because filenames
     *  can be virtually as long as possible; on some
     *  systems however when filenames are combinations
     *  of more than one mother file this can be
     *  crucial to keep the total filename length within
     *  the file system's allowed bounds.
     */
    protected static String abbreviate(String longStr) {
        StringBuffer shortStr;
        int i, j;
        char c;

        j = longStr.length();
        if (j <= ABBR_LENGTH) return longStr;

        shortStr = new StringBuffer(j);
        for (i = 0; (i < j) && (shortStr.length() + j - i > ABBR_LENGTH); i++) {
            c = longStr.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                shortStr.append(c);
            }
        }
        shortStr.append(longStr.substring(i));
        longStr = shortStr.toString();
        j = longStr.length();
        if (j <= ABBR_LENGTH) return longStr;

        shortStr = new StringBuffer(j);
        shortStr.append(longStr.charAt(0));
        for (i = 1; (i < j - 1) && (shortStr.length() + j - i > ABBR_LENGTH); i++) {
            c = longStr.charAt(i);
            if ("aeiouäöü".indexOf(c) < 0) {
                shortStr.append(c);
            }
        }
        shortStr.append(longStr.substring(i));
        longStr = shortStr.toString();
        j = longStr.length();
        if (j <= ABBR_LENGTH) return longStr;

        i = (ABBR_LENGTH >> 1) - 1;

        return (longStr.substring(0, i) + '\'' + longStr.substring(longStr.length() - i));
    }

    protected String createScheme(String applied) {
        String txt2;
        int i;
        int k = 0;
        int m;
        int checkedAbbrev;
        boolean checkedFull;

        if (applied.length() == 0) return protoScheme;

        for (i = 0; i < superPaths.length; i++) {
            txt2 = superPaths[i].getPath().getPath();
            txt2 = txt2.substring(0, txt2.lastIndexOf(File.separatorChar) + 1);
            if (applied.startsWith(txt2)) {
                applied = "$D" + (char) (i + 48) + applied.substring(txt2.length());
                k = 3;
                break;
            }
        }
        k = Math.max(k, applied.lastIndexOf(File.separatorChar) + 1);
        for (i = 0, checkedAbbrev = -1; i < superPaths.length; i++) {
            txt2 = superPaths[i].getPath().getPath();
            txt2 = txt2.substring(txt2.lastIndexOf(File.separatorChar) + 1);
            m = txt2.lastIndexOf('.');
            txt2 = (m > 0) ? txt2.substring(0, m) : txt2;
            if ((!protoScheme.contains("$B" + (char) (i + 48))) || (checkedAbbrev == i)) {
                m = applied.indexOf(txt2, k);
                if (m >= 0) {
                    applied = applied.substring(0, m) + "$F" + (char) (i + 48) + applied.substring(m + txt2.length());
                    k = m + 3;
                    continue;
                }
                checkedFull = true;
            } else {
                checkedFull = false;
            }
            if (checkedAbbrev == i) continue;
            txt2 = abbreviate(txt2);
            m = applied.indexOf(txt2, k);
            if (m >= 0) {
                applied = applied.substring(0, m) + "$B" + (char) (i + 48) + applied.substring(m + txt2.length());
                k = m + 3;
            } else if (!checkedFull) {
                checkedAbbrev = i;
                i--;                // retry non-abbrevated
            }
        }
        txt2 = GenericFile.getExtStr(getType());
        if (applied.endsWith(txt2)) {
            applied = applied.substring(0, applied.length() - txt2.length()) + "$E";
        }

        return applied;
    }

    protected String abbrScheme(String orig) {
        int i = orig.lastIndexOf("$F");
        if (i >= 0) {
            return (orig.substring(0, i) + "$B" + orig.substring(i + 2));
        } else {
            return orig;
        }
    }

    protected String expandScheme(String orig) {
        int i = orig.indexOf("$B");
        if (i >= 0) {
            return (orig.substring(0, i) + "$F" + orig.substring(i + 2));
        } else {
            return orig;
        }
    }

    protected String udirScheme(String orig, String udirPr) {
        int i;
        String udir = Application.userPrefs.get(udirPr, null);

        if (udir == null) return orig;

        if (orig.startsWith("$D")) {
            i = 3;
        } else {
            i = orig.lastIndexOf(File.separatorChar) + 1;
        }

        return (new File(udir, orig.substring(i)).getPath());
    }

    protected void feedback() {
        if ((handledTypes != null) && ((type & TYPE_BASICMASK) == TYPE_INPUTFILE)) {
            calcFormat();
        } else if ((type & TYPE_BASICMASK) == TYPE_OUTPUTFILE) {
            checkExist();
        }
    }

// -------- PathListener interface --------
// we're listening to ggChoose

    public void pathChanged(PathEvent e) {
        File path = e.getPath();
        scheme = createScheme(path.getPath());
        setPathAndDispatchEvent(path);
    }

// -------- Action methods (ggPath Return-Hit) --------
// we're listening to ggPath

    public void actionPerformed(ActionEvent e) {
        String str = ggPath.getText();
        if (str.length() == 0) {                // automatic generation
            scheme = protoScheme;
            str = evalScheme(scheme);
        } else {
            scheme = createScheme(str);
        }
        setPathAndDispatchEvent(new File(str));
    }

// -------- Component methods (Panel) --------

    public void componentResized(ComponentEvent e) {
        if ((ggType != null) && init) {    // makes us update info on type, res, rate etc.
            init = false;
            itemStateChanged(new ItemEvent(ggType, ItemEvent.ITEM_STATE_CHANGED, ggType.getSelectedItem(), ItemEvent.SELECTED));
        }
    }

    public void componentShown (ComponentEvent e) {}
    public void componentHidden(ComponentEvent e) {}
    public void componentMoved (ComponentEvent e) {}

// -------- Item methods (ggType) --------

    public void itemStateChanged(ItemEvent e) {
        int mode, ID, i;

        if (e.getSource() == ggType) {        // -------------------- update Res/Rate fields ---------
            if ((ggRes != null) || (ggRate != null)) {
                mode = getType();
                if (ggRes != null) {
                    ID = ggRes.getSelectedIndex();
                    ggRes.removeAllItems();
                    if (Util.isValueInArray(mode, GenericFile.TYPES_SOUND)) {            // --- sound ----
                        for (i = 0; i < sndResTxt.length; i++) {
                            ggRes.addItem(sndResTxt[i]);
                        }
                    } else if (Util.isValueInArray(mode, GenericFile.TYPES_IMAGE)) {    // --- image ----
                        for (i = 0; i < imgResTxt.length; i++) {
                            ggRes.addItem(imgResTxt[i]);
                        }
                    }
                    ggRes.setSelectedIndex(ID);
                }

                if (ggRate != null) {
                    ID = ggRate.getSelectedIndex();
                    ggRate.removeAllItems();
                    if (Util.isValueInArray(mode, GenericFile.TYPES_SOUND)) {            // --- sound ----
                        for (i = 0; i < sndRateTxt.length; i++) {
                            ggRate.addItem(sndRateTxt[i]);
                        }
                    } else if (Util.isValueInArray(mode, GenericFile.TYPES_SPECT)) {    // --- spect ----
                        for (i = 0; i < spectRateTxt.length; i++) {
                            ggRate.addItem(spectRateTxt[i]);
                        }
                    } else if (Util.isValueInArray(mode, GenericFile.TYPES_MOVIE)) {    // --- movie ----
                        for (i = 0; i < movRateTxt.length; i++) {
                            ggRate.addItem(movRateTxt[i]);
                        }
                    }
                    ggRate.setSelectedIndex(ID);
                }
            }

            setPathAndDispatchEvent(new File(evalScheme(scheme)));
        }
    }

// -------- interne IOTextfeld-Klasse --------

    class IOTextField extends ColouredTextField {

        public IOTextField() {
            super(32);

            InputMap	inputMap	= getInputMap();
            ActionMap	actionMap   = getActionMap();
            int			i;
            String		s;

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.META_MASK), "abbr");
            actionMap.put("abbr", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    scheme = abbrScheme(scheme);
                    setPathAndDispatchEvent(new File(evalScheme(scheme)));
                }
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.META_MASK), "expd");
            actionMap.put("expd", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    scheme = expandScheme(scheme);
                    setPathAndDispatchEvent(new File(evalScheme(scheme)));
                }
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.META_MASK), "auto");
            actionMap.put("auto", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    scheme = protoScheme;
                    setPathAndDispatchEvent(new File(evalScheme(scheme)));
                }
            });
            for (i = 1; i <= 9; i++) {
                s = "sudir" + i;
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + i, InputEvent.META_MASK + InputEvent.SHIFT_MASK), s);
                actionMap.put(s, new SetUserDirAction(i));
                s = "rudir" + i;
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + i, InputEvent.META_MASK), s);
                actionMap.put(s, new RecallUserDirAction(i));
            }
        }

        class SetUserDirAction extends AbstractAction {
            private int idx;
            private javax.swing.Timer visualFeedback;
            private Paint oldPaint = null;

            public SetUserDirAction(int idx) {
                this.idx = idx;
                visualFeedback = new javax.swing.Timer(250, this);
                visualFeedback.setRepeats(false);
            }

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == visualFeedback) {
                    ggPath.setPaint(oldPaint);
                } else {
                    String dir = getPath().getParent();
                    if (dir != null) {
                        Application.userPrefs.put("UserDir" + idx, dir);
                        if (visualFeedback.isRunning()) {
                            visualFeedback.restart();
                        } else {
                            oldPaint = ggPath.getPaint();
                            ggPath.setPaint(COLOR_PROPSET);
                            visualFeedback.start();
                        }
                    }
                }
            }
        }

        class RecallUserDirAction extends AbstractAction {
            private int idx;

            public RecallUserDirAction(int idx) {
                this.idx = idx;
            }

            public void actionPerformed(ActionEvent e) {
                scheme = udirScheme(scheme, "UserDir" + idx);
                setPathAndDispatchEvent(new File(evalScheme(scheme)));
            }
        }
    } // class IOTextField
}