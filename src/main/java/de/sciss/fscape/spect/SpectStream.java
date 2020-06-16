/*
 *  SpectStream.java
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

import java.io.EOFException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 *	Klangstromklasse, mit der alle Operatoren arbeiten;
 *	gearbeitet wird mit Frames, wobei das SpectStream-Objekt
 *	einen Puffer mit Frames verwaltet. Wenn der Puffer voll
 *	ist, muss auch der Writer warten. Die Frames werden wg.
 *	der Geschwindigkeit nicht kopiert, sondern "weitergereicht";
 *	freigegebene Frames wandern zurueck in einen internen Puffer
 */
public class SpectStream
{
// -------- public variables --------

    /**
     * 	linear frequency bands
     */
    public static final int MODE_LIN	= SpectralFile.PVA_LIN;
    /**
     * 	logarithmic frequency bands
     */
    public static final int MODE_LOG	= SpectralFile.PVA_EXP;
    /**
     *	number of associated channels
     *	alle folgenden Variablen sind ReadOnly,
     *	zum Schreiben die entsprechenden methods benutzen!
     */
    public int		chanNum;
    /**
     *	number of frequency bands
     */
    public int		bands;
    /**
     *	lowest represented frequency (Hz)
     */
    public float	loFreq;
    /**
     *	highest represented freq (Hz)
     */
    public float	hiFreq;
    /**
     *	MODE_...
     */
    public int		freqMode;
    /**
     *	e.g. 44,100
     */
    public float	smpRate;
    /**
     *	time resolution
     */
    public int		smpPerFrame;
    /**
     *	estimated(!) number of frames; don't rely on this
     */
    public long frames;
    /**
     *	number of frames written to pipe (total, NOT number currently in buffer)
     */
    public long		framesWritten	= 0L;
    /**
     *	number of frames read from the pipe (total)
     */
    public long		framesRead		= 0L;

    public static final String ERR_NOREADER		= "Reader closed the stream";
    public static final String ERR_NOWRITER		= "Writer closed the stream";

// -------- private variables --------

    protected Thread	readerThread	= null;
    protected Thread	writerThread	= null;

    protected static final int STATE_UNKNOWN	=	0;
    protected static final int STATE_ACTIVE		=	1;
    protected static final int STATE_DEAD		=	2;

    protected int	readerState	= STATE_UNKNOWN;
    protected int	writerState	= STATE_UNKNOWN;

    protected static final String ERR_BUFEMPTY		= "Streambuffer empty";
    protected static final String ERR_BUFFULL		= "Streambuffer full";

    protected static final int	DEFAULT_BUF_SIZE	= 8;	// number of frames

    protected Vector<SpectFrame> activeBuf;	// geschriebene Frames
    protected Vector<SpectFrame> deadBuf;	// nicht mehr benoetigte Frames; darauf kann alloc() zurueckgreifen!
    protected int		bufSize;	// max. Frame-Zahl in activeBuf und deadBuf

// -------- public methods --------

    /**
     *	@param	origin	Strom, dessen Daten wie Kanalzahl, Baender,
     *					Frequenzen etc. uebernommen werden
     *	@param	bufSize	Zahl der Frames, die der Puffer des Stroms maximal enthaelt
     */
    public SpectStream(SpectStream origin, int bufSize) {
        if (origin != null) {
            synchronized (origin) {
                // copy params
                this.chanNum        = origin.chanNum;
                this.bands          = origin.bands;
                this.loFreq         = origin.loFreq;
                this.hiFreq         = origin.hiFreq;
                this.freqMode       = origin.freqMode;
                this.smpRate        = origin.smpRate;
                this.smpPerFrame    = origin.smpPerFrame;
                this.frames         = origin.frames;
            }
        } else {
            // initialize to null
            this.chanNum		= 0;
            this.bands			= 0;
            this.loFreq			= 0.0f;
            this.hiFreq			= 0.0f;
            this.freqMode		= MODE_LIN;
            this.smpRate		= 0.0f;
            this.smpPerFrame	= 0;
            this.frames			= 0;
        }

        this.bufSize    = bufSize;
        activeBuf       = new Vector<SpectFrame>(bufSize);
        deadBuf         = new Vector<SpectFrame>(bufSize);
    }

    public SpectStream(SpectStream origin) {
        this(origin, origin.bufSize);
    }

    public SpectStream() {
        this(null, DEFAULT_BUF_SIZE);
    }

    public SpectStream(int bufSize) {
        this(null, bufSize);
    }

    /**
     *	registriert den aufrufenden Thread als Schreibberechtigten;
     *	DIES MUSS EIN EIGENTSTAENDIG LAUFENDER OPERATOR SEIN!
     *	AUF KEINEN FALL MEHRMALS AUFRUFEN
     */
    public void initWriter() {
        synchronized (this) {
            activeBuf.removeAllElements();
            writerThread    = Thread.currentThread();
            writerState     = STATE_ACTIVE;
            framesWritten   = 0;
        }
    }

    /**
     *	registriert den aufrufenden Thread als Leseberechtigten;
     *	DIES MUSS EIN EIGENTSTAENDIG LAUFENDER OPERATOR SEIN!
     *	AUF KEINEN FALL MEHRMALS AUFRUFEN
     */
    public void getDescr() {
        synchronized (this) {
            readerThread    = Thread.currentThread();
            readerState     = STATE_ACTIVE;
            framesRead      = 0;
        }
    }

    /**
     *	Zahl der Kanaele festlegen
     *	AUF KEINEN FALL MEHR NACH initWriter() / getDescr() AUFRUFEN
     *
     *	@param chanNum	Zahl der Kanaele, 1 = mono, 2 = stereo etc.
     */
    public void setChannels(int chanNum) {
        synchronized (this) {
            activeBuf.removeAllElements();    // Sicherheitsmassnahme
            deadBuf  .removeAllElements();
            this.chanNum = chanNum;
        }
    }

    /**
     *	Zahl der Baender und Frequenzbereich festlegen
     *	AUF KEINEN FALL MEHR NACH initWriter() / getDescr() AUFRUFEN
     *
     *	@param loFreq	Frequenz des untersten Bandes
     *	@param hiFreq	Frequenz des obersten Bandes
     *	@param bands	Anzahl der Baender
     *	@param mode		Verteilungsmodus, MODE_LIN = linear, MODE_LOG = logarithmisch
     */
    public void setBands(float loFreq, float hiFreq, int bands, int mode) {
        synchronized (this) {
            activeBuf.removeAllElements();    // Sicherheitsmassnahme
            deadBuf  .removeAllElements();
            this.loFreq     = loFreq;
            this.hiFreq     = hiFreq;
            this.bands      = bands;
            this.freqMode   = mode;
        }
    }

    /**
     *	Samplingrate und Framebreite ermitteln
     *	AUF KEINEN FALL MEHR NACH initWriter() / getDescr() AUFRUFEN
     *
     *	@param smpRate		Samplingfrequenz in Hz
     *	@param smpPerFrame	Zahl der Samples pro Frame
     */
    public void setRate(float smpRate, int smpPerFrame) {
        synchronized (this) {
            this.smpRate        = smpRate;
            this.smpPerFrame    = smpPerFrame;
        }
    }

    /**
     *	Geschaetzte Laenge (im Frames) festlegen
     */
    public void setEstimatedLength(long frames) {
        this.frames = frames;
    }

    /**
     *	Erzeugt einen neuen Frame;
     *
     *	DER AUFRUFER SOLLTE java.lang.OutOfMemoryError CATCHEN!
     */
    public SpectFrame allocFrame() {
        SpectFrame fr;

        synchronized (this) {
            if (!deadBuf.isEmpty()) {
                fr = deadBuf.firstElement();
                deadBuf.removeElement(fr);
                fr.gainAccess();
            } else {
                fr = new SpectFrame(chanNum, bands);
            }
        }
        return fr;
    }

    /**
     *	Erzeugt ein neues Array, das als Puffer fuer mehrere Frames dient;
     *
     *	DER AUFRUFER SOLLTE java.lang.OutOfMemoryError CATCHEN!
     *
     *	@param	frameNum	Anzahl der Frames (Index der ersten Dimension des Arrays)
     */
    public static SpectFrame[] allocFrames(SpectStream stream, int frameNum) {
        SpectFrame frames[] = new SpectFrame[frameNum];

        for (int i = 0; i < frameNum; i++) {
            frames[i] = stream.allocFrame();
        }
        return frames;
    }

    /**
     *	Gibt ein nicht mehr benoetigtes Frame frei;
     *	der Aufruf dieser Methode ist wichtig, weil das Frame wieder
     *	per allocFrame() zur Verfuegung gestellt werden kann, ohne
     *	dass ueberfluessige Garbage entsteht!
     *
     *	Anschliessend sollten alle Referenzen dieses Frames auf NULL gesetzt werden!
     */
    public void freeFrame(SpectFrame fr) {
        fr.looseAccess();
        synchronized (fr) {
            if (fr.accessCount == 0) {
                synchronized (this) {
                    if (deadBuf.size() < bufSize) {
                        deadBuf.addElement(fr);
                    }
                }
            }
        }
    }

    /**
     *	Gibt mehrere Frames zurueck; vgl. freeFrame()
     */
    public static void freeFrames(SpectStream stream, SpectFrame frames[]) {
        for (int i = 0; i < frames.length; i++) {
            stream.freeFrame(frames[i]);
        }
    }

    /**
     *	Schreibt ein Frame; dass heisst, es wird weitergereicht
     *	wenn der Writer das Frame nicht mehr benoetigt, muss
     *	er weiterhin freeFrame() aufrufen!
     *
     *	METHODE DARF NUR VOM REGISTRIERTEN WRITER AUFGERUFEN WERDEN!
     *
     *	wenn der Puffer voll ist, wird eine IndexOutOfBoundsException ausgeloest
     *	wenn der Strom readerseitig geschlossen wurde, wird eine EOFException ausgeloest
     *	(in diesem Fall wird closeWriter() automatisch aufgerufen!)
     *
     *	die IOException muss generell abgefangen werden, weil der SpectStream moeglicherweise
     *	in der Zukunft auf temporaere Dateien zugreift!
     *
     *	@param	fr  sollte mit allocFrame(s)() beschafft worden sein!
     */
    public void writeFrame(SpectFrame fr)
            throws IOException,
            IndexOutOfBoundsException {
        switch (framesWriteable()) {
            case -1:
                closeWriter();
                throw new EOFException(ERR_NOREADER);

            case 0:
                throw new IndexOutOfBoundsException(ERR_BUFFULL);

            default:
                synchronized (this) {
                    fr.gainAccess();    // Frame nicht freigeben, bis vom Reader verarbeitet!
                    activeBuf.addElement(fr);
                    framesWritten++;
                }
                break;
        }
    }

    public void writeDummy(SpectFrame fr) {
        framesWritten++;
    }

    /**
     *	Schreibt mehrere Frames; vgl. writeFrame()
     *
     *	@return	-1, wenn readerseitig der Strom geschlossen wurde,
     *			sonst Zahl der geschriebenen Frames
     */
    public static int writeFrames(SpectStream stream, SpectFrame frames[])
            throws IOException {
        int frameNum = 0;

        try {
            for (int i = 0; i < frames.length; i++) {
                stream.writeFrame(frames[i]);
                frameNum++;
            }
        } catch (IndexOutOfBoundsException ignored) {
        } catch (EOFException e) {
            return -1;
        }
        return frameNum;
    }

    /**
     *	Liest ein Frame aus dem Puffer
     *	; wenn das Frame verarbeitet ist, muss freeFrame() aufgerufen werden!
     *
     *	METHODE DARF NUR VOM REGISTRIERTEN READER AUFGERUFEN WERDEN!
     *
     *	wenn der Puffer leer ist, wird eine NoSuchElementException ausgeloest
     *	wenn ausserdem der Strom versiegt ist, eine EOFException
     *	(in diesem Fall wird closeReader() automatisch aufgerufen!)
     *
     *	die IOException muss generell abgefangen werden, weil der SpectStream moeglicherweise
     *	in der Zukunft auf temporaere Dateien zugreift!
     */
    public SpectFrame readFrame()
            throws IOException,
            NoSuchElementException {
        SpectFrame fr;

        switch (framesReadable()) {
            case -1:
                closeReader();
                throw new EOFException(ERR_NOWRITER);

            case 0:
                throw new NoSuchElementException(ERR_BUFEMPTY);

            default:
                synchronized (this) {
                    fr = activeBuf.firstElement();
                    activeBuf.removeElement(fr);
                    framesRead++;
                }
                return fr;
        }
    }

    /**
     *	Liest mehrere Frames; vgl. readFrame()
     *
     *	@param	frames	die erste Dimension gibt die Zahl der frames an,
     *			die gelesen werden sollen
     *	@return	-1, wenn writerseitig der Strom geschlossen wurde und KEIN
     *			frame mehr verfuegbar war, sonst Zahl der gelesenen Frames
     */
    public static int readFrames(SpectStream stream, SpectFrame frames[])
            throws IOException {
        int frameNum = 0;

        try {
            for (int i = 0; i < frames.length; i++) {
                frames[i] = stream.readFrame();
                frameNum++;
            }
        } catch (NoSuchElementException ignored) {
        } catch (EOFException e) {
            if (frameNum == 0) {
                frameNum = -1;
            }
        }
        return frameNum;
    }

    /**
     *	Schliesst den Strom, markiert damit dessen Ende
     *	DARF NUR VOM REGISTRIERTEN WRITER AUFGERUFEN WERDEN!
     *	Kann auch aufgerufen werden, wenn der Strom schon geschlossen wurde
     *
     *	Anschliessend sollten alle Referenzen des SpectStreams auf NULL gesetzt werden
     */
    public void closeWriter()
            throws IOException {
        synchronized (this) {
            writerThread = null;
            writerState = STATE_DEAD;
            deadBuf.removeAllElements();
        }
    }

    /**
     *	Schliesst Reader-seitig den Strom
     *	DARF NUR VOM REGISTRIERTEN READER AUFGERUFEN WERDEN!
     *	sollte nur im Falle eines IO-Fehlers beim CleanUp aufgerufen werden;
     *	Kann auch aufgerufen werden, wenn der Strom schon geschlossen wurde
     *
     *	Anschliessend sollten alle Referenzen des SpectStreams auf NULL gesetzt werden
     */
    public void closeReader()
            throws IOException {
        synchronized (this) {
            writerThread    = null;
            writerState     = STATE_DEAD;
            activeBuf.removeAllElements();
        }
    }

    /**
     *	Liefert die Zahl der ohne Blockierung lesbaren frames
     *
     *	@return	-1 bedeutet, dass keine Frames mehr gelesen werden koennen
     *			UND auch keine mehr kommen werden, weil der Writer den
     *			Strom geschlossen hat
     */
    public int framesReadable() {
        int frameNum;

        synchronized (this) {
            frameNum = activeBuf.size();
            if ((frameNum == 0) && (writerState == STATE_DEAD)) {
                frameNum = -1;
            }
        }
        return frameNum;
    }

    /**
     *	Liefert die Zahl der ohne Blockierung schreibbaren frames
     *
     *	@return	-1 bedeutet, dass keine Frames mehr geschrieben werden koennen
     *			UND auch keine mehr abgeholt werden, weil der Reader den
     *			Strom geschlossen hat
     */
    public int framesWriteable() {
        int frameNum;

        synchronized (this) {
            frameNum = bufSize - activeBuf.size();
            if (readerState == STATE_DEAD) {
                frameNum = -1;
            }
        }
        return frameNum;
    }

    /**
     *	Besorgt Zeit-Offset des naechsten Frames in Millisekunden
     */
    public double getTime() {
        if (Thread.currentThread() == readerThread) {
            return (framesRead * ((double) smpPerFrame / smpRate) * 1000);
        } else {
            return (framesWritten * ((double) smpPerFrame / smpRate) * 1000);
        }
    }

    /**
     *	Format-String erzeugen
     */
    public static String getFormat(SpectStream stream) {
        String chanTxt = null;
        String lengthTxt;
        int min, sec, millis;

        switch (stream.chanNum) {
            case 1:
                chanTxt = "Mono ";
                break;
            case 2:
                chanTxt = "Stereo ";
                break;
            default:
                chanTxt = stream.chanNum + "-chn. ";
                break;
        }

        millis	= (int) (framesToMillis( stream, stream.frames ) + 0.5);
        min		= millis / 60000;
        sec		= (millis / 1000) % 60;
        millis	= millis % 1000;
        lengthTxt = "; " + min + ':' + String.valueOf( sec + 100 ).substring( 1 ) + '.' +
                    String.valueOf( millis + 1000 ).substring( 1 );

        return( chanTxt + stream.bands + " bands (" + (int) (stream.loFreq + 0.5f) + '-' +
                (int) (stream.hiFreq + 0.5f) + ((stream.freqMode == SpectStream.MODE_LIN) ?
                " Hz linear)" : " Hz exp.)") + lengthTxt );
    }

    /**
     *	Berechnet die Frame-Nummer nach angebenen Millisekunden
     */
    public static double millisToFrames(SpectStream stream, double ms) {
        //			seconds * framesPerSec
        return ((ms / 1000) * ((double) stream.smpRate / stream.smpPerFrame));
    }

    /**
     *	Rechnet Framezahl in Millisekunden um
     */
    public static double framesToMillis(SpectStream stream, long frames) {
        return (frames * ((double) stream.smpPerFrame / stream.smpRate) * 1000);
    }
}