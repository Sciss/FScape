/*
 *  Operator.java
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

package de.sciss.fscape.op;

import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.SpectPatch;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Slots;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.SyncFailedException;
import java.rmi.NotBoundException;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

/**
 *	Operator mit Basisfunktionen
 */
public class Operator
        implements Runnable, Slots, Cloneable, Transferable {

// -------- public variables --------

    public static final String	PACKAGE		= "de.sciss.fscape.op";
    public static DataFlavor	flavor		= null;				// DataFlavor representing this class

    public static final	int		GUI_PREFS	= 0;				// fuer createGUI()
    public static final String	PRN_SPECTRO	= "Spectrogram";	// Property-Keynames

    public static final int FLAGS_ALIAS		= 0x01;				// Operator is an Alias-Object
    public static final int FLAGS_BYPASS	= 0x02;				// Operator is bypassed

    /**
     *	wird vom Hauptthread gesetzt, wenn der Thread beendet werden soll
     *	darf nur vom Document veraendert werden
     */
    public boolean	threadDead		= true;

    /**
     *	wird vom Hauptthread gesetzt, wenn der Thread pausieren soll
     *	der Thread darf erst weiterarbeiten, wenn threadPaused wieder false ist
     *	darf nur vom Document veraendert werden
     */
    public boolean	threadPaused	= false;

    /**
     *	wird vom Operator gesetzt, wenn er de facto im Pause-Modus angekommen ist
     */
    public boolean	threadPausing	= false;

    /**
     *	wird vom Hauptthread gesetzt, wenn der Thread gestartet wird
     *	beim Beenden der run() Methode muss der Operator owner.operatorTerminated() aufrufen!
     */
    public SpectPatch owner			= null;

// -------- private variables --------

    /*
     *	Subclassen sollten setID und setName
     *	ausfuehren
     */
    protected OpIcon icon;
    /*
     *	Subclassen sollten addElement()
     *	ausfuehren
     */
    protected Vector<SpectStreamSlot> slots;						// verwaltet die SpectStreamSlots (in + out)

    protected Vector<Operator>	aliases;				// alle Alia des Operators
    protected Operator	original = null;		// Original eines Aliases
    protected static final String	ERR_ALIASSYNC	= "Original is of different type";
    protected static final String	ERR_ALREADYALIAS= "Object is already an alias";

    private	static TreeMap<String, String> OPERATORS;			// values sind alle Op-Klassennamen, keys deren "echte" Namen (String)
    private static DataFlavor flavors[] = null;	// alle erlaubten DataFlavors

    protected 			   boolean	disposed		= false;
    protected static final String	ERR_DISPOSED	= "Operator disposed";

    /*
     *	Operatoren sollten statische Variablen fuer den
     *	Operatoren-Namen, Presets und Preferences haben.
     *	im Konstruktor sollten die statischen Variablen
     *	in diese Instanzvariablen uebertragen werden!
     */
    protected String		opName	= null;		// = class name ohne .class (INTERNAL)
    protected Presets		presets	= null;		// call Operator.getDefaultPresets() when creating this!
    protected Prefs			prefs	= null;		// call Operator.getDefaultPrefs() when creating this!

    /*
     *	Subclassen muessen dieses pr ueberschreiben und static_pr.clone() in superPr eintragen!
     */
    protected			PropertyArray	pr;

    protected static	PropertyArray	op_static_pr	= null;

    // Properties (defaults)
    private static final int PR_SPECTRO	= 0;		// Array-Indices: pr.bool
    private static final int PR_FLAGS		= 0;		// pr.intg

    protected static final String	PRN_FLAGS	= "Flags";	// Property-Keynames

    private static final boolean	prBool[]	= { false };
    private static final String	prBoolName[]	= { PRN_SPECTRO };
    private static final int		prIntg[]	= { 0 };
    private static final String	prIntgName[]	= { PRN_FLAGS };

    /*
     *	Hier wird der Laufzeit-Fehler als Text festgehalten
     */
    private String	threadError		= null;

    /*
     *	geschaetzter Fortschritt 0...100%
     *	darf nur vom Operator veraendert werden
     */
    private float	threadProgress	= 0;

// -------- static constructor --------

    static {
        OPERATORS = new TreeMap<String, String>();
    }

// -------- public methods --------

    public Operator() {
        if (op_static_pr == null) {
            op_static_pr = new PropertyArray();

            op_static_pr.bool		= prBool;
            op_static_pr.boolName	= prBoolName;
            op_static_pr.intg		= prIntg;
            op_static_pr.intgName	= prIntgName;
        }

        slots = new Vector<SpectStreamSlot>();
        aliases = new Vector<Operator>();

        // data flavors
        if (Operator.flavor == null) {
            Operator.flavor = new DataFlavor(getClass(), "Operator");
        }
    }

    /**
     *	Liefert eine Hashtable mit allen verfuegbaren Ops
     *
     *	@return	 keys = "richtige" Namen; values = Klassennamen
     */
    public static Map getOperators() {
        if (OPERATORS.isEmpty()) {        // XXX spaeter die Liste extern nachladen!
            OPERATORS.put("Input file", "InputOp");
            OPERATORS.put("Output file", "OutputOp");
            OPERATORS.put("Analyse", "AnalysisOp");
            OPERATORS.put("Synthesize", "SynthesisOp");
            OPERATORS.put("Flip freq", "FlipFreqOp");
            OPERATORS.put("Log freq", "LogFreqOp");
            OPERATORS.put("Splitter", "SplitterOp");
            OPERATORS.put("Unitor", "UnitorOp");
            OPERATORS.put("Mono2Stereo", "Mono2StereoOp");
            OPERATORS.put("Smear", "SmearOp");
            OPERATORS.put("Zoom", "ZoomOp");
            OPERATORS.put("Shrink", "ShrinkOp");
            OPERATORS.put("Envelope", "EnvOp");
            OPERATORS.put("Cepstral", "CepstralOp");
            OPERATORS.put("Convolve", "ConvOp");
            OPERATORS.put("Contrast", "ContrastOp");
            OPERATORS.put("Extrapolate", "ExtrapolateOp");
            OPERATORS.put("Tarnish", "TarnishOp");
            OPERATORS.put("Percussion", "PercussionOp");
            OPERATORS.put("Mindmachine", "MindmachineOp");
            OPERATORS.put("Amp Env", "AmpEnvOp");
        }
        return OPERATORS;
    }

    /**
     *	Besorgt die Parameter des Operators in Form eines PropertyArray Objekts
     */
    public PropertyArray getPropertyArray()
    {
        return pr;
    }

    /**
     *	Liefert die Presets des Operators
     *	diese sind statisch d.h. fuer alle Operatoren derselben Klasse identisch
     */
    public Presets getPresets()
    {
        return presets;
    }

    /**
     *	Liefert die Preferences des Operators
     *	diese sind statisch d.h. fuer alle Operatoren derselben Klasse identisch
     */
    public Prefs getPrefs()
    {
        return prefs;
    }

    /**
     *	Liefert Flags wie FLAGS_ALIAS oder FLAGS_BYPASS
     */
    public int getFlags()
    {
        return pr.superPr.intg[ PR_FLAGS ];
    }

    /**
     *	Alle vom Operator belegten Ressourcen freigeben
     *	(danach nicht mehr benutzen!)
     */
    public void dispose() {
        if (original != null) {
            original.forgetAlias(this);
        }
        disposed = true;
    }

// -------- Alias-bezogene methods --------

    /**
     *	Liefert eine Auflistung aller Alia des Operators (possibly empty)
     */
    public Enumeration getAliases()
    {
        return aliases.elements();
    }

    /**
     *	Liefert des Original eines Aliases
     *
     *	@return	null, falls Operator kein Alias ist
     */
    public Operator getOriginal()
    {
        return original;
    }

    /**
     *	Erklaert diesen Operator zu einem Alias
     *
     *	SyncFailedException, wenn "original" nicht dieselben Operator-Subklasse
     *	SlotAlreadyConnectedException, wenn dieser Op schon ein Alias ist
     */
    public void turnIntoAlias(Operator orig)
            throws SyncFailedException, SlotAlreadyConnectedException {
        while (orig.getOriginal() != null) {
            orig = orig.getOriginal();
        }

        if (orig.getClass() != this.getClass()) {
            throw new SyncFailedException(ERR_ALIASSYNC);
        }
        if (original != null) {
            throw new SlotAlreadyConnectedException(ERR_ALREADYALIAS);
        }

        original = orig;
        orig.registerAlias(this);
        pr.superPr.intg[PR_FLAGS] |= FLAGS_ALIAS;
        ((OpIcon) getIcon()).operatorFlagsChanged(pr.superPr.intg[PR_FLAGS]);
    }

    /**
     *	Formt ein Alias in einen eigenstaendingen Operator um
     *	(wenn dieser Op kein Alias war, passiert nichts)
     */
    public void turnIntoGenuine() {
        PropertyArray oldSuper;

        if (original != null) {
            oldSuper        = pr.superPr;
            pr              = new PropertyArray(original.getPropertyArray());
            pr.superPr      = oldSuper;
            original.forgetAlias(this);
            this.original   = null;
            pr.superPr.intg[PR_FLAGS] &= ~FLAGS_ALIAS;
            ((OpIcon) getIcon()).operatorFlagsChanged(pr.superPr.intg[PR_FLAGS]);
        }
    }

    /**
     *	Registriert ein Alias
     */
    protected void registerAlias(Operator aliasOp) {
        aliases.addElement(aliasOp);
    }

    /**
     *	Meldet einen Alias ab (vor dessen Aufloesung)
     */
    protected void forgetAlias(Operator aliasOp) {
        aliases.removeElement(aliasOp);
    }

// -------- GUI methods --------

    /**
     *	MUSS VON SUBCLASS UEBERLAGERT WERDEN
     */
    public PropertyGUI createGUI(int type) {
        return new PropertyGUI("lbNothing here");
    }

    /**
     *	Subclassen sollten dies auch benutzen statt direkt auf icon zuzugreifen!
     */
    public Component getIcon()
    {
        return icon;
    }

// -------- Runnable methods + Assoziiertes --------

    /**
     *	Falls der Operator seinen Thread wg. eines Fehlers beendet hat
     *	(getIcon().isSelected() == OpIcon.STATE_ERROR)
     *	kann hiermit die Fehlermeldung ermittelt werden
     *
     *	@return	null, wenn keine Meldung vorliegt ("Uns liegen zur Zeit keine Meldungen vor")
     */
    public String getError()
    {
        return threadError;
    }

    /**
     *	Runtime-Fehlermessage setzen
     */
    public void setError(String threadError) {
        this.threadError = threadError;
    }

    /**
     *	Fortschritt des Threads 0...100 %
     */
    public float getProgress()
    {
        return threadProgress;
    }

    /**
     *	Fortschritt des Threads setzen
     */
    public void setProgress(float progress) {
        this.threadProgress = progress;
    }

    /*
     *	run() initialisieren; muss von subclassen zuerst aufgerufen werden!
     */
    protected void runInit() {
        PropertyArray oldSuper;

        setProgress(0.0f);
        if (original != null) {    // Aliase: Daten abgleichen
            oldSuper    = pr.superPr;
            pr          = new PropertyArray(original.getPropertyArray());
            pr.superPr  = oldSuper;
        }
    }

    /*
     *	aufzurufen vor der Hauptschleife
     *	; ggf. werden Spectrogramme eingerichtet
     */
    protected void runSlotsReady()
    {
        Enumeration		slts;
        SpectStreamSlot	slot;

        if( pr.superPr.bool[ PR_SPECTRO ]) {
            slts = getSlots( Slots.SLOTS_WRITER ).elements();
            while( slts.hasMoreElements() ) {
                slot = (SpectStreamSlot) slts.nextElement();
                slot.createSpectrogram();
            }
        }
    }

    /*
     *	Operator stoppen
     *	UNMITTELBAR NACH RUECKKEHR DIESER METHODE SOLLTE run() VERLASSEN WERDEN!
     *
     *	@param	e	null, wenn ordnungsgemaess beendet; sonst die Fehler-Exception
     */
    protected void runQuit(Exception e) {
        SpectStreamSlot slot;
        Enumeration slts = getSlots(Slots.SLOTS_ANY).elements();

        threadDead = true;
        while (slts.hasMoreElements()) {
            slot = (SpectStreamSlot) slts.nextElement();
            slot.cleanUp();
        }
        if (e == null) {
            setProgress(1.0f);
            getIcon().repaint();
        } else {
            ((OpIcon) getIcon()).setSelected(OpIcon.STATE_ERROR);
            setError(e.getMessage());
            System.out.println(icon.getName() + ": aborted because of: " + threadError);
        }

        owner.operatorTerminated(this);
    }

    /*
     *	prueft, ob der Operator pausieren soll und tut dies ggf.
     */
    protected synchronized void runCheckPause() {
        try {
            if (threadPaused && !threadDead) {
                threadPausing = true;
                owner.operatorPaused(this);

                while (threadPaused && !threadDead) {
                    wait();
                }
                threadPausing = false;
            }
        } catch (InterruptedException e) {
            threadPausing = false;
        }
    }

    /*
     *	Updated Icon-Progressindikator
     *
     *	@param	slot	Slot, der ein verarbeitetes Frame empfangen hat; darf null sein
     *	@param	fr		verarbeitetes Frame; darf null sein
     */
    protected void runFrameDone(SpectStreamSlot slot, SpectFrame fr) {
        float progress = SpectStreamSlot.progress(slot);

        if (progress - getProgress() >= 0.04f) {
            setProgress(progress);
            ((OpIcon) getIcon()).paintProgress(null);
        }
    }

    public void run() {
        runInit();
        runSlotsReady();
        runQuit(null);
    }

    public synchronized void runStop() {
        threadPaused    = false;
        threadDead      = true;
        notify();
    }

    public synchronized void runPause(boolean state) {
        threadPaused = state;
        notify();
    }
    
// -------- Slots methods --------

    /**
     *	@param filter	Jedes gesetzte Flag im filter-Parameter muss im Suchobject gesetzt sein!
     */
    public Vector<SpectStreamSlot> getSlots(int filter) {
        Vector<SpectStreamSlot> fltSlots = new Vector<SpectStreamSlot>();
        SpectStreamSlot slot;

        for (int i = 0; i < slots.size(); i++) {
            slot = slots.elementAt(i);
            if ((slot.getFlags() & filter) == filter) {
                fltSlots.addElement(slot);
            }
        }
        return fltSlots;
    }

    /**
     *	@return	null bei Fehler
     */
    public SpectStreamSlot getSlot(String name) {
        SpectStreamSlot slot;
        for (int i = 0; i < slots.size(); i++) {
            slot = slots.elementAt(i);
            if (slot.toString().equals(name)) return slot;
        }
        return null;
    }

    public void linkTo(String thisName, Slots destSlots, String destName)
            throws SyncFailedException, SlotAlreadyConnectedException, NoSuchElementException {
        throw new NoSuchElementException();
    }

    public void divorceFrom(String thisName, Slots srcSlots, String srcName)
            throws NotBoundException, NoSuchElementException {
        throw new NoSuchElementException();
    }

// -------- Cloneable methods --------

    /**
     *	@return	NB: MAY RETURN NULL IN CASE OF ERROR !
     */
    public Object clone() {
        if (disposed) return null;

        Operator op = null;

        try {
            op = this.getClass().newInstance();
            op.pr = (PropertyArray) this.getPropertyArray().clone();

            if (getOriginal() != null) {
                op.turnIntoAlias(getOriginal());
            }
            ((OpIcon) op.getIcon()).setName(((OpIcon) this.getIcon()).getName());

            return op;
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (SyncFailedException e3) {
            op.dispose();
        } catch (SlotAlreadyConnectedException e4) {
            op.dispose();
        }

        return null;
    }

// -------- Transferable methods --------

    public DataFlavor[] getTransferDataFlavors() {
        if (flavors == null) {
            DataFlavor iconFlavors[] = icon.getTransferDataFlavors();
            flavors     = new DataFlavor[iconFlavors.length + 1];
            flavors[0]  = Operator.flavor;
            for (int i = 0; i < iconFlavors.length; i++) {
                flavors[i + 1] = iconFlavors[i];
            }
        }
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor fl) {
        DataFlavor flavs[] = getTransferDataFlavors();
        for (int i = 0; i < flavs.length; i++) {
            if (flavs[i].equals(fl)) return true;
        }
        return false;
    }

    public Object getTransferData(DataFlavor fl)
            throws UnsupportedFlavorException, IOException {
        if (fl.equals(Operator.flavor)) {
            if (disposed) throw new IOException(ERR_DISPOSED);
            return this;

        } else {
            return icon.getTransferData(fl);
        }
    }

// -------- private methods --------

    /*
     *	this method should be invoked by subclass
     */
    protected static Properties getDefaultPrefs() {
        Properties defPrefs = new Properties();
        defPrefs.put(OpPrefs.PR_EDITDIM, "256,256");
        return defPrefs;
    }
}