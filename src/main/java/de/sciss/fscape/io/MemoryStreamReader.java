/*
 *  MemoryStreamReader.java
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

package de.sciss.fscape.io;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MemoryStreamReader
        extends BufferedReader {

// -------- private variables --------

    protected char	buf[];
    protected int	bufSize;			// = buf.length
    protected int	bufOff		= 0;

    protected static final String ERR_NOMARKS	= "Requested marks not found";

// -------- public methods --------

    public MemoryStreamReader( InputStream stream )
    throws IOException
    {
        super( new InputStreamReader( stream ));

        bufSize			= 1024;
        buf				= new char[ bufSize ];
    }

    public int skipTo( String[] stopMarks )
    throws IOException
    {
        return readTo( stopMarks, null, 0 );
    }

    public int skipTo( String[] stopMarks, long timeOut )
    throws IOException
    {
        return readTo( stopMarks, null, timeOut );
    }

    public int readTo( String[] stopMarks, StringBuffer content )
    throws IOException
    {
        return readTo( stopMarks, content, 0 );
    }

    /**
     *	Read ahead until one of the passend mark strings is encountered
     *
     *	@param	stopMarks	if anyone of these is found the method will return and
     *						the next read starts at the first character of that
     *						mark string
     *	@param	content		all content from the last read position to the stop mark
     *						will be appended to this buffer; clearing the buffer
     *						must be done outside this method!
     *	@param	timeOut		millisecs - method will return approximately after this
     *						interval if no stopMark has yet been encountered; returns
     *						-1 in this case, so you may probably retry after some interaction
     *	@return				index of the mark; -1 in case of timeout
     */
    public int readTo( String[] stopMarks, StringBuffer content, long timeOut )
    throws IOException
    {
        int		maxMark, numRead, markLen, i, j, k, m, n;
        String	mark;
        boolean	checkTime	= timeOut > 0;
        long	stopTime	= timeOut;
        boolean saveContent	= content != null;

        maxMark = checkBufSize( stopMarks );	// bufSize must exceed max of stopMark lengths!

        if( checkTime ) {
            stopTime += System.currentTimeMillis();
        }

        do {
            numRead	= read( buf, bufOff, bufSize - bufOff );
            j		= Math.max( 0, numRead );
            bufOff += j;

            for( i = 0; i < stopMarks.length; i++ ) {
                mark	= stopMarks[ i ];
                markLen	= mark.length();

bufLp:			for( k = markLen; k < bufOff; k++ ) {
                    for( m = k, n = markLen; n > 0; ) {
                        if( mark.charAt( --n ) != buf[ --m ]) continue bufLp;
                    }
                    // Strings are matching
                    bufOff -= m;
                    if( saveContent ) {
                        content.append( buf, 0, m );
                    }
                    System.arraycopy( buf, m, buf, 0, bufOff );

                    return i;
                }
            }

            // Shift buffer
            m		= Math.max( 0, bufOff - maxMark );
            bufOff -= m;
            if( saveContent ) {
                content.append( buf, 0, m );
            }
            System.arraycopy( buf, m, buf, 0, bufOff );

            if( checkTime ) {
                if( System.currentTimeMillis() > stopTime ) return -1;
            }

        } while( numRead > 0 );

        throw new EOFException( ERR_NOMARKS );
    }

// -------- private methods --------

    protected int checkBufSize( String[] stopMarks )
    {
        int i, j, k;

        for( i = 0, j = 0; i < stopMarks.length; i++ ) {
            k = stopMarks[ i ].length();
            if( k > j ) {
                j = k;
            }
        }
        if( j > bufSize ) {
            bufSize			= j + (j >> 1);
            buf				= new char[ bufSize ];
        }

        return j;
    }
}