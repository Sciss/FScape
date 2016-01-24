/*
 *  SpectFrame.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.spect;

/**
 *	Gelesene (nicht geklonte) Frames sind READONLY
 *	(zumindest solange der accessCount > 1 ist) !!!
 */
public class SpectFrame
implements Cloneable
{
// -------- public variables --------

    public static final int AMP		= 0;
    public static final int PHASE	= 1;

    public static final int FLAGS_UNTOUCHED	= 0;	// never change since Op use it
    public static final int FLAGS_LEFT		= 1;	//   as JComboBox-Item-IDs!
    public static final int FLAGS_RIGHT		= 2;
    public static final int FLAGS_SUM		= 3;

    /**
     *	erste  Dimension = Kanal-Nr.;
     *	zweite Dimension = Band-Nr.*2 + Daten-Typ (Amp/Phase);
     */
    public float	data[][];

    /**
     *	Zahl der gegenwaertigen Nutzer; wenn der Count auf Null geht,
     *	kann das Frame wieder "recyclet" werden (abfrage nur im synchronized( this )!)
     */
    public int	accessCount;

// -------- public methods --------
    // public void gainAccess();
    // public void looseAccess();
    // public static clear( Frame fr );

    /**
     *	NOTE: der Inhalt von data ist undefiniert,
     *	notfalls clear() benutzen!
     *
     *	Als Benutzerzahl wird 1 eingetragen
     */
    public SpectFrame( int chanNum, int bands )
    {
        data		= new float[ chanNum ][ bands << 1 ];
        accessCount	= 1;
    }

    /**
     *	Neues Frame wird erzeugt und mit den
     *	Werten des uebergebenen Frames gefuellt
     *	(Cloning)
     *
     *	@param	flags	FLAGS_... erlaubt zusammenmischen der Kanaele
     */
    public SpectFrame( SpectFrame src, int flags )
    {
        this( (flags == FLAGS_UNTOUCHED) ? src.data.length : 1, src.data[ 0 ].length >> 1 );

        copyData( src, this, flags );
    }

    public SpectFrame( SpectFrame src )
    {
        this( src, FLAGS_UNTOUCHED );
    }

    /**
     *	Meldet weiteren Nutzer des Frames an
     */
    public void gainAccess()
    {
        synchronized( this ) {
            accessCount++;
        }
    }

    /**
     *	Meldet Nutzer des Frames ab
     */
    public void looseAccess()
    {
        synchronized( this ) {
            accessCount--;
        }
    }

    public Object clone()
    {
        return new SpectFrame( this, FLAGS_UNTOUCHED );
    }

    /**
     *	Loescht die Daten eines Frames
     *	d.h. Amplitude und Phase werden auf 0.0 gesetzt
     */
    public static void clear( SpectFrame fr )
    {
        for( int i = 0; i < fr.data.length; i++ ) {					// channels
            for( int j = 0; j < fr.data[ i ].length; j++ ) {		// bands
                fr.data[ i ][ j ] = 0.0f;
            }
        }
    }

// -------- private methods --------

    protected void copyData( SpectFrame src, SpectFrame dest, int flags )
    {
        double	destImg, destReal;
        float	srcAmp, srcPhase;
        int		srcStartCh, chanNum;

//		try {
            switch( flags ) {
            case FLAGS_SUM:			// Vectoren addieren; zunaechst Polar ==> Rect
                for( int i = 0; i < src.data[ 0 ].length; i += 2 ) {
                    destImg  = 0.0;
                    destReal = 0.0;
                    for( int j = 0; j < src.data.length; j++ ) {
                        srcAmp    = src.data[ j ][ i + AMP ];
                        srcPhase  = src.data[ j ][ i + PHASE ];
                        destImg  += srcAmp * Math.sin( srcPhase );
                        destReal += srcAmp * Math.cos( srcPhase );
                    }
                    destImg   /= src.data.length;	// ...und Durchschnitt bilden
                    destReal  /= src.data.length;
                    dest.data[ 0 ][ i + AMP ]	=
                            (float) Math.sqrt( destImg*destImg + destReal*destReal );	// Rect ==> Polar
                    dest.data[ 0 ][ i + PHASE]	=
                            (float) Math.atan2( destImg, destReal );	// richtig rum? XXX
                }
                break;

            default:
                switch( flags ) {
                case FLAGS_LEFT:
                    srcStartCh	= 0;
                    chanNum		= 1;
                    break;

                case FLAGS_RIGHT:
                    srcStartCh	= Math.min( 1, src.data.length - 1 );
                    chanNum		= 1;
                    break;

                default:	// FLAGS_UNTOUCHED
                    srcStartCh	= 0;
                    chanNum		= Math.min( src.data.length, dest.data.length );
                    break;
                }

                for( int i = 0; i < chanNum; i++ ) {		// 1:1 copy
                    System.arraycopy( src.data[ srcStartCh + i ], 0, dest.data[ i ], 0,
                                      src.data[ srcStartCh + i ].length );
                }
                break;
            }
//		}
//		catch( IndexOutOfBoundsException e ) {}	// Nothing yet XXX
    }
}
// class SpectFrame
