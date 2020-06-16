/*
 *  SpectStreamSlot.java
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

package de.sciss.fscape.spect;

import de.sciss.fscape.gui.Spectrogram;
import de.sciss.fscape.op.Operator;
import de.sciss.fscape.op.SlotAlreadyConnectedException;
import de.sciss.fscape.util.Slots;

import java.io.IOException;
import java.io.SyncFailedException;
import java.rmi.NotBoundException;
import java.util.NoSuchElementException;

/**
 *	Administration of a spectral stream slot
 */
public class SpectStreamSlot {

// -------- public variables --------

    public static final int	STATE_UNKNOWN	= 0;
    public static final int	STATE_ACTIVE	= 1;
    public static final int	STATE_DEAD		= 2;
    public static final int	STATE_WAITING	= 3;
    public static final int	STATE_DUMMY		= 4;	// kein Reader verlinkt

    public int state = STATE_UNKNOWN;		        // signalisert ggf. Warte-Prozesse
                                                    // ZUGRIFF NUR IN SYNCHRONIZED( THIS) BLOCK

// -------- private variables --------

    protected int					flags;		    // SLOTS_...
    protected String				name;		    // wird zurueckgegeben ueber toString()
    protected Operator				owner;
    protected Thread				ownerThread	= null;

    protected SpectStreamSlot		linked		= null;
    protected SpectStream			stream		= null;
    protected Spectrogram			spectro		= null;

    protected static final String ERR_INTERRUPTED	= "Slot was interrupted";
    protected static final String ERR_STILLINUSE	= "Slot still in use";

// -------- public methods --------

    /**
     *	@param type	Typ wie z.B. Slots.SLOTS_READER
     */
    public SpectStreamSlot(Operator owner, int type, String name) {
        flags       = (type & Slots.SLOTS_TYPEMASK) | Slots.SLOTS_FREE;    // unlinked so far
        this.name   = name;
        this.owner  = owner;
    }

    public SpectStreamSlot(Operator owner, int type) {
        this(owner, type, (type & Slots.SLOTS_TYPEMASK) == Slots.SLOTS_READER ?
                Slots.SLOTS_DEFREADER : Slots.SLOTS_DEFWRITER);
    }

    public int getFlags()
    {
        return flags;
    }

    public String toString()
    {
        return name;
    }

    public Operator getOwner()
    {
        return owner;
    }

    /**
     *	Ermittelt korrespondierenden Reader zu einem Writer und vice versa
     *
     *	@return null, wenn nicht verlinkt!
     */
    public SpectStreamSlot getLinked()
    {
        return linked;
    }

    /**
     *	Verknuepft diesen Slot mit einem anderen
     */
    public void linkTo(SpectStreamSlot dest)
            throws SyncFailedException,
            SlotAlreadyConnectedException {
        if ((this.linked != null) || (dest.linked != null)) {
            throw new SlotAlreadyConnectedException();
        }
        if ((this.flags & Slots.SLOTS_TYPEMASK) == (dest.flags & Slots.SLOTS_TYPEMASK)) {
            throw new SyncFailedException("");
        }
        this.linked = dest;
        this.flags |= Slots.SLOTS_LINKED;
        this.flags &= ~Slots.SLOTS_FREE;
        dest.linked = this;
        dest.flags |= Slots.SLOTS_LINKED;
        dest.flags &= ~Slots.SLOTS_FREE;
    }

    /**
     *	Trennt diesen Slot von seinem Counterpart
     */
    public void divorce()
            throws NotBoundException {
        if (linked != null) {
            linked.linked = null;
            linked.flags &= ~Slots.SLOTS_LINKED;
            linked.flags |= Slots.SLOTS_FREE;
            this.linked = null;
            this.flags &= ~Slots.SLOTS_LINKED;
            this.flags |= Slots.SLOTS_FREE;
        } else {
            throw new NotBoundException();
        }
    }

    public Thread getOwnerThread()
    {
        return ownerThread;
    }

    /**
     *	stellt einen SpectStream zur Verfuegung, der
     *	vom Counterpart-Slot gelesen werden kann (benachrichtigt diesen ggf.);
     *
     *	diese Methode ruft SpectStream.initWriter() auf! Siehe dort fuer Details
     *	wenn ein Reader auf den Stream wartet, wird ihm das signalisiert
     *
     *	erzeugt eine SlotAlreadyConnectedException, wenn der Slot bereits einen Stream fuehrt
     */
    public void initWriter(SpectStream strm)
            throws SlotAlreadyConnectedException {
        synchronized (this) {
            if (stream != null) {            // nanu, schon ein Stream praesent?!
                throw new SlotAlreadyConnectedException(ERR_STILLINUSE);
            }
            strm.initWriter();
            ownerThread = Thread.currentThread();
            stream = strm;
            state = STATE_ACTIVE;
        }

        if (linked != null) {
            synchronized (linked) {
                linked.stream = strm;
                if (linked.state == STATE_WAITING) {
                    linked.notify();            // tell dem to start
                }
            }
        } else {
            state = STATE_DUMMY;
        }
    }

    /**
     *	besorgt den SpectStream des Counterpart-Writers,
     *	ggf. wird mit wait() auf dessen Einrichtung gewartet!
     *
     *	diese Methode ruft SpectStream.getDescr() auf! Siehe dort fuer Details
     *
     *	erzeugt eine InterruptedException, wenn der Warteprozess unterbrochen wurde
     *	(dies darf nicht als "Fehler" behandelt werden, sondern sollte zur Ueberpruefung
     *	des threadPaused + threadDead Flags fuehren!)
     *	NICHT AUFRUFEN, WENN DER SLOT NICHT VERLINKT IST
     */
    public SpectStream getDescr()
            throws InterruptedException {
        synchronized (this) {
            try {
                ownerThread = Thread.currentThread();
                while (stream == null) {
                    state = STATE_WAITING;
                    wait();
                    state = STATE_ACTIVE;
                }
                stream.getDescr();
            } catch (InterruptedException e) {
                state = STATE_ACTIVE;
                throw e;
            }
            return stream;
        }
    }

    /**
     *	Erzeugt ein laufendes Spectrogram des Slot-Streams
     */
    public void createSpectrogram() {
        synchronized (this) {
            if (stream != null) {
                if (spectro == null) {
                    spectro = new Spectrogram(this);
                }
                spectro.newStream(stream);
            }
        }
    }

    /**
     *	liest einen Frame von der Input-Pipe;
     *	sollte statt SpectStream.readFrame() benutzt werden, da diese Methode
     *	ggf. wartet und den Warte-Status markiert und somit einen Abbruch durch den Hauptthread
     *	gestattet! Ausserdem wird bei vollem Puffer der Writer benachrichtigt, wenn wieder
     *	Platz ist
     *
     *	erzeugt eine InterruptedException, wenn der Warteprozess unterbrochen wurde
     *	(dies darf nicht als "Fehler" behandelt werden, sondern sollte zur Ueberpruefung
     *	des threadPaused + threadDead Flags fuehren!)
     */
    public SpectFrame readFrame()
            throws IOException,
            InterruptedException {
        SpectFrame fr = null;

        synchronized (this) {
            try {
                while (fr == null) {
                    try {
//						System.out.println( "framesReadable : " + stream.framesReadable() );
                        if (stream.framesReadable() != 0) {    // auch -1, loest ja EOFException aus
                            fr = stream.readFrame();
                            if (linked.state == STATE_WAITING) {
                                // geschachtelte synchronized nur, wenn klar ist, dass der andere wartet!
                                synchronized (linked) {
                                    linked.notify();            // yo, du kannst wieder schreiben
                                }
                            }
                        } else {
                            state = STATE_WAITING;
                            wait();
                            state = STATE_ACTIVE;
                        }
                    } catch (NoSuchElementException ignored) {}    // dann warten wir eben
                }
            } catch (InterruptedException e) {
                state = STATE_ACTIVE;
                throw e;
            }
        }
        return fr;
    }

    /**
     *	schreibt einen Frame
     *	sollte statt SpectStream.writeFrame() benutzt werden, da diese Methode
     *	einen evtl. wartenden Counterpart benachrichtigt; ausserdem wird gewartet,
     *	bis Platz im Puffer ist
     *
     *	erzeugt eine InterruptedException, wenn der Warteprozess unterbrochen wurde
     *	(dies darf nicht als "Fehler" behandelt werden, sondern sollte zur Ueberpruefung
     *	des threadPaused + threadDead Flags fuehren!)
     *
     *	NICHT AUFRUFEN, WENN DER SLOT NICHT VERLINKT IST
     */
    public void writeFrame(SpectFrame fr)
            throws IOException,
            InterruptedException {
        if (state == STATE_DUMMY) {
            stream.writeDummy(fr);
            return;
        }

        synchronized (this) {
            try {
                while (fr != null) {
                    try {
                        if (stream.framesWriteable() != 0) {    // auch -1, loest ja EOFException aus
                            stream.writeFrame(fr);
                            if (spectro != null) {
                                spectro.addFrame(fr);
                            }
                            fr = null;
                            if (linked.state == STATE_WAITING) {
                                // geschachtelte synchronized nur, wenn klar ist, dass der andere wartet!
                                synchronized (linked) {
                                    linked.notify();            // yo, du kannst wieder lesen
                                }
                            }
                        } else {
                            state = STATE_WAITING;
                            wait();
                            state = STATE_ACTIVE;
                        }
                    } catch (IndexOutOfBoundsException ignored) {}        // dann warten wir eben
                }
            } catch (InterruptedException e) {
                state = STATE_ACTIVE;
                throw e;
            }
        }
    }

    /**
     *	Gibt einen ueber dieses Objekt erzeugtes Frame frei
     */
    public void freeFrame(SpectFrame fr) {
        stream.freeFrame(fr);
    }

    /**
     *	Raeumt den Slot nach Benutzung auf, schiesst ggf. den Stream
     *	UNBEDINGT AUFRUFEN AUCH BEIM FEHLER-ABBRUCH, WEIL DER SLOT
     *	SONST NICHT WIEDERBENUTZT WERDEN KANN!
     */
    public void cleanUp() {
        synchronized (this) {
            if (spectro != null) {
                spectro.ownerTerminated();
                spectro = null;
            }

            if (stream != null) {
                try {
                    if ((flags & Slots.SLOTS_TYPEMASK) == Slots.SLOTS_READER) {
                        stream.closeReader();
                    }
                } catch (IOException ignored) {}    // spielt keine Rolle mehr

                try {
                    if ((flags & Slots.SLOTS_TYPEMASK) == Slots.SLOTS_WRITER) {
                        stream.closeWriter();
                    }
                } catch (IOException ignored) {}    // spielt keine Rolle mehr
                stream = null;
            }
            this.state = STATE_DEAD;
            ownerThread = null;
        }
    }

    /**
     *	Liefert den zugehoerigen SpectStream
     */
    public SpectStream getStream() {
        synchronized (this) {
            return stream;
        }
    }

    /**
     *	Ermittelt den Fortschritt des Slots im Bereich 0.0 bis 1.0 (= 100%)
     *
     *	@param	slot	darf null sein; das Ergebnis ist dann 0.0
     */
    public static float progress(SpectStreamSlot slot) {
        if ((slot == null) || (slot.stream == null)) return 0.0f;

        synchronized (slot) {
            if ((slot.flags & Slots.SLOTS_TYPEMASK) == Slots.SLOTS_READER) {
                return ((float) slot.stream.framesRead / slot.stream.frames);

            } else {
                return ((float) slot.stream.framesWritten / slot.stream.frames);
            }
        }
    }
}