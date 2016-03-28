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
 */

package de.sciss.fscape.render;

import de.sciss.io.Span;

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
public class RenderSource {
    public final int numChannels;

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
     *  the arrays are pre-allocated for the
     *  given number of channels.
     *  Note that the final vectors are not
     *  initialized, i.e. blockBuf will
     *  become new float[numChannels][] etc.
     */
    public RenderSource(int numChannels) {
        this.numChannels    = numChannels;
        blockBuf            = new float[numChannels][];
    }

    /**
     *  Constructs a new RenderSource by
     *  copying a template. Note that the
     *  vectors themselves are not allocated
     *  just like in the `RenderSource(int numChannels)`
     *  constructor!
     *
     *	@param	template	a template request whose dimensions
     *						and requests are copied to the newly
     *						created render source
     */
    public RenderSource(RenderSource template) {
        this.numChannels    = template.numChannels;
        blockBuf            = new float[numChannels][];
    }
}