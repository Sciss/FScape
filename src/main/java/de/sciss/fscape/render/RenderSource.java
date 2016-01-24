/*
 *  RenderSource.java
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
 *  Changelog:
 *		14-Jul-05   created from de.sciss.meloncillo.render.RenderSource
 *		31-Aug-05	adds clipboard support (quick + dirty)
 *		??-???-??	copied from eisenkraut
 */

//package de.sciss.eisenkraut.render;
package de.sciss.fscape.render;

import de.sciss.io.*;

/**
 *  A RenderSource describes the source
 *  data for generating rendering output.
 *  This data is restricted to dynamic
 *  scalar vector data, i.e. data that
 *  changes over time : sense data
 *  (a receiver's sensibility at a point
 *  described by a transmitter trajectory)
 *  and trajectory data of a transmitter.
 */
public class RenderSource
{
    public final int		numChannels;

    /**
     *  The blockSpan describes the
     *  current time span of the provided data
     *  in the source rate sense.
     *  Thus, blockSpan.getLength() equals
     *  blockBufLen
     *
     *	@todo	check what happens when resampling is active
     */
    public Span				blockSpan;
    /**
     */
    public float[][]		blockBuf;
    /**
     *  Offset to use when reading data
     *  from blockBuf
     */
    public int				blockBufOff;
    /**
     *  Length to use when reading data
     *  from blockBuf
     */
    public int				blockBufLen;

    public float[][]		clipboardBuf;	// offset + len identical to blockBufOff/Len !

    /**
     *  Constructs a new RenderSource, where
     *  the arrays are preallocated for the
     *  given number of transmitters and receivers.
     *  Note that the final vectors are not
     *  initialized, i.e. senseBlockBuf will
     *  become new float[numTrns][numRcv][] etc.
     *  All request fields are set to false by default.
     *
     *	@param	numTrns	number of transmitters used for rendering
     *	@param	numRcv	number of receivers used for rendering
     */
    public RenderSource( int numChannels )
    {
        this.numChannels	= numChannels;
        blockBuf			= new float[ numChannels ][];
    }

    /**
     *  Constructs a new RenderSource by
     *  copying a template. Note that the
     *  vectors themselves are not allocated
     *  just like in the RenderSource( int numTrns, int numRcv )
     *  constructor! However, the requests
     *  are copied 1:1 to the new RenderSource.
     *
     *	@param	template	a template request whose dimensions
     *						and requests are copied to the newly
     *						created render source
     */
    public RenderSource( RenderSource template )
    {
        this.numChannels	= template.numChannels;
        blockBuf			= new float[ numChannels ][];
    }
}