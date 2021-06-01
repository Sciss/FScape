/*
 *  Needlehole.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		06-Feb-05	added standard deviation + minimum filter + improved speed
 *		17-Mar-05	added center clipping
 *		18-Sep-05	refurbished as processing plug-in
 */

package de.sciss.fscape.render;

import de.sciss.gui.PrefCheckBox;
import de.sciss.gui.PrefComboBox;
import de.sciss.gui.PrefNumberField;
import de.sciss.gui.SpringPanel;
import de.sciss.gui.StringItem;
import de.sciss.util.NumberSpace;

import javax.swing.*;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 *	Processing module for moving window
 *	based filtering of a sound.
 */
public class Needlehole
        extends AbstractRenderPlugIn {

    private	Preferences	prefs;
    private static final String KEY_GAINTYPE		= "gaintype";
    private static final String KEY_GAIN			= "gain";

    private static final String KEY_FILTER			= "filter";
    private static final String KEY_LENGTH			= "length";
    private static final String KEY_THRESH			= "thresh";
    private static final String KEY_SUBDRY			= "subdry";

    private static final String GAIN_ABSOLUTE		= "abs";
    private static final String GAIN_NORMALIZED		= "norm";

    private static final String FILTER_MEDIAN		= "median";
    private static final String FILTER_STDDEV		= "stddev";
    private static final String FILTER_MINIMUM		= "minimum";
    private static final String FILTER_CENTER		= "center";

    public boolean hasUserParameters() {
        return true;
    }

    public boolean shouldDisplayParameters() {
        return true;
    }

    public void init(Preferences p) {
        super.init(p);
        prefs = p;
    }

    public JComponent getSettingsView(RenderContext context) {
        final SpringPanel		p			= new SpringPanel();
        final PrefNumberField	ggGain		= new PrefNumberField();
        final PrefComboBox		ggGainType	= new PrefComboBox();
        final PrefNumberField	ggLength	= new PrefNumberField();
        final PrefComboBox		ggFilter	= new PrefComboBox();
        final PrefNumberField	ggThresh	= new PrefNumberField();
        final PrefCheckBox		ggSubDry	= new PrefCheckBox();

        ggGain.setSpace(NumberSpace.genericDoubleSpace);
        ggGainType.addItem(new StringItem(GAIN_NORMALIZED, "normalized"));
        ggGainType.addItem(new StringItem(GAIN_ABSOLUTE, "immediate"));

        ggLength.setSpace(NumberSpace.genericDoubleSpace);        // XXX
        ggThresh.setSpace(NumberSpace.genericDoubleSpace);        // XXX

        ggFilter.addItem(new StringItem(FILTER_MEDIAN, "Median"));
        ggFilter.addItem(new StringItem(FILTER_STDDEV, "Standard Deviation"));
        ggFilter.addItem(new StringItem(FILTER_MINIMUM, "Minimum"));
        ggFilter.addItem(new StringItem(FILTER_CENTER, "Center Clipping"));

        p.gridAdd(new JLabel("Gain", SwingConstants.RIGHT), 0, 0);
        p.gridAdd(ggGain, 1, 0);
        p.gridAdd(ggGainType, 1, 0);

        p.gridAdd(new JLabel("Window length", SwingConstants.RIGHT), 0, 1);
        p.gridAdd(ggLength, 1, 1);
        p.gridAdd(new JLabel("Filter", SwingConstants.RIGHT), 0, 2);
        p.gridAdd(ggFilter, 1, 2);
        p.gridAdd(new JLabel("Clip thresh", SwingConstants.RIGHT), 0, 3);
        p.gridAdd(ggThresh, 1, 3);
        p.gridAdd(new JLabel("Subtract dry signal", SwingConstants.RIGHT), 0, 4);
        p.gridAdd(ggSubDry, 1, 4);

        ggGain    .setPreferences(this.prefs, KEY_GAIN);
        ggGainType.setPreferences(this.prefs, KEY_GAINTYPE);
        ggLength  .setPreferences(this.prefs, KEY_LENGTH);
        ggFilter  .setPreferences(this.prefs, KEY_FILTER);
        ggThresh  .setPreferences(this.prefs, KEY_THRESH);
        ggSubDry  .setPreferences(this.prefs, KEY_SUBDRY);

        p.makeCompactGrid();
        return p;
    }

    public String getName() {
        return "Needlehole Cherry Blossom";
    }

    private RunningWindowFilter prFilter;
    private boolean				prSubDry;
    private boolean				prNormalize;
    private float				prGain;
    private float[][]			prInBuf;
    private int					prInBufSize;
//	private float				prMaxAmp;
    private int					prOffStart;
//	private long				prFramesRead;
    private long				prFramesWritten;
    private long				prRenderLength;
    private int					prWinSize;

    public boolean producerBegin(RenderContext context, RenderSource source)
            throws IOException {

        final double	winSizeMillis	= prefs.getDouble(KEY_LENGTH, 1.0);
        final String	filterType		= prefs.get(KEY_FILTER, FILTER_MEDIAN);
        final double	threshAmp		= Math.exp(prefs.getDouble(KEY_THRESH, -3.0) / 20 * Math.log(10));    // XXX

        final int		outBufSize;
        final Integer	outBufSizeI;

        prSubDry		= prefs.getBoolean(KEY_SUBDRY, false);
        prNormalize		= prefs.get(KEY_GAINTYPE, "").equals(GAIN_NORMALIZED);

        prWinSize		= Math.max(1, (int) (winSizeMillis * context.getSourceRate() + 0.5) & ~1);
        outBufSize		= Math.max(8192, prWinSize);
        outBufSizeI		= new Integer(outBufSize);
        prInBufSize		= outBufSize + prWinSize;
        prOffStart		= prWinSize >> 1;

        prInBuf			= new float[source.numChannels][prInBufSize];

        // cannot have other buffer sizes than this
        context.setOption(RenderContext.KEY_MINBLOCKSIZE , outBufSizeI);
        context.setOption(RenderContext.KEY_PREFBLOCKSIZE, outBufSizeI);
        context.setOption(RenderContext.KEY_MAXBLOCKSIZE , outBufSizeI);

        if (filterType.equals(FILTER_MEDIAN)) {
            prFilter = new MedianFilter(prWinSize, source.numChannels);
        } else if (filterType.equals(FILTER_STDDEV)) {
            prFilter = new StdDevFilter(prWinSize, source.numChannels);
        } else if (filterType.equals(FILTER_MINIMUM)) {
            prFilter = new MinimumFilter(prWinSize, source.numChannels);
        } else if (filterType.equals(FILTER_CENTER)) {
            prFilter = new CenterClippingFilter(prWinSize, source.numChannels, threshAmp);
        } else {
            throw new IOException("Unknown filter type : " + filterType);
        }

        if (prNormalize) {
            prGain = 1.0f;
        } else {
            prGain = (float) Math.exp(prefs.getDouble(KEY_GAIN, 0.0) / 20 * Math.log(10));    // XXX
        }

        prRenderLength  = context.getTimeSpan().getLength();
        prFramesWritten = 0;

        return context.getConsumer().consumerBegin(context, source);
    }

    public boolean producerRender(RenderContext context, RenderSource source)
            throws IOException {

        final int transLen = (int) Math.min(prInBufSize - prWinSize, prRenderLength - prFramesWritten);

        for (int ch = 0; ch < source.numChannels; ch++) {
            System.arraycopy(source.blockBuf[ch], source.blockBufOff, prInBuf, prOffStart,
                    source.blockBufLen);
        }
        // zero-padding last chunk
        if (prOffStart + source.blockBufLen < prInBufSize) {
            fill(prInBuf, prOffStart + source.blockBufLen, prInBufSize - prOffStart - source.blockBufLen, 0f);
        }

        prFilter.process(prInBuf, source.blockBuf, 0, source.blockBufOff, transLen);
        if (prSubDry) {
            subtract(source.blockBuf, source.blockBufOff, prInBuf, prWinSize >> 1, transLen);
        }

        // shift buffers
        for (int ch = 0; ch < source.numChannels; ch++) {
            System.arraycopy(prInBuf[ch], prInBufSize - prWinSize, prInBuf[ch], 0, prWinSize);
        }

        prOffStart		 = prWinSize;
        prFramesWritten += transLen;

        if (prGain != 1.0f) {
            multiply(source.blockBuf, source.blockBufOff, transLen, prGain);
        }
        return context.getConsumer().consumerRender(context, source);
    }

    private static void fill(float[][] buf, int off, int len, float value) {
        float[] convBuf;

        for (int ch = 0; ch < buf.length; ch++) {
            convBuf = buf[ch];
            for (int i = off, j = off + len; i < j; ) {
                convBuf[i++] = value;
            }
        }
    }

    private static void multiply(float[][] buf, int off, int len, float value) {
        float[] convBuf;

        for (int ch = 0; ch < buf.length; ch++) {
            convBuf = buf[ch];
            for (int i = off, j = off + len; i < j; ) {
                convBuf[i++] *= value;
            }
        }
    }

    private static void subtract(float[][] bufA, int offA, float[][] bufB, int offB, int len) {
        float[] convBuf1, convBuf2;

        for (int ch = 0; ch < bufA.length; ch++) {
            convBuf1 = bufA[ch];
            convBuf2 = bufB[ch];
            for (int i = offA, j = offB, k = offA + len; i < k; ) {
                convBuf1[i++] -= convBuf2[j++];
            }
        }
    }

// -------- Window Filter --------

    private interface RunningWindowFilter {
        public void process(float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len) throws IOException;
    }

    private static class StdDevFilter
            implements RunningWindowFilter {

        final int			winSize;
        final int			channels;
        final double[][]	dcMem;
        final int			winSizeM1;

        public StdDevFilter(int winSize, int channels) {
            this.winSize	= winSize;
            this.channels   = channels;
            winSizeM1		= winSize - 1;

            dcMem		    = new double[channels][2];
        }

        public void process(float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len)
                throws IOException {

            int			ch, i, j, k, m, n;
            float[]		convBuf2, convBuf3;
            double[]	convBuf4;
            double		d1, d2, mu, mus, omus, sum;

            for (ch = 0; ch < channels; ch++) {
                convBuf4	= dcMem[ch];
                convBuf2	= inBuf[ch];
                convBuf3	= outBuf[ch];
                // calc first full window sum
                mus			= 0.0;
                for( i = 0, m = inOff; i < winSizeM1; i++, m++ ) {
                    mus	   += convBuf2[ m ];	// sum all but last one in window
                }
                omus		= 0.0;
                for (j = 0, m = inOff, n = outOff; j < len; j++, m++, n++) {
                    // shift by one : remove obsolete sample
                    // and add new last window sample
                    mus		= mus - omus + convBuf2[m + winSizeM1];
                    mu		= mus / winSize;	// mean now
                    sum		= 0.0;
                    for (i = 0, k = m; i < winSize; i++, k++) {
                        d1		= convBuf2[k] - mu;
                        sum	   += d1 * d1;	// variance
                    }
                    d1				= Math.sqrt(sum);    // standard deviation
                // ---- remove DC ----
                    d2				= d1 - convBuf4[0] + 0.99 * convBuf4[1];
                    convBuf3[n]     = (float) d2;
                    convBuf4[0]     = d1;
                    convBuf4[1]     = d2;
                    omus            = convBuf2[m];
                }
            } // for channels
        } // process
    } // class StdDevFilter

    private static class MinimumFilter
            implements RunningWindowFilter {

        final int			winSize;
        final int			channels;
        final int			winSizeM1;

        public MinimumFilter(int winSize, int channels) {
            this.winSize	= winSize;
            this.channels   = channels;
            winSizeM1		= winSize - 1;
        }

        public void process(float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len)
                throws IOException {

            int			ch, i, j, k, m, n, minidx;
            float[]		convBuf2, convBuf3;
            float		f1, f2, min;

            for (ch = 0; ch < channels; ch++) {
                convBuf2	= inBuf[ch];
                convBuf3	= outBuf[ch];
                minidx		= -1;
                min			= 0.0f;
                for (j = 0, m = inOff, n = outOff; j < len; j++, m++, n++) {
                    if (minidx < m) {    // need to find again
                        f1 = Math.abs(convBuf2[m]);
                        minidx = m;
                        for (i = 1, k = m + 1; i < winSize; i++, k++) {
                            f2 = Math.abs(convBuf2[k]);
                            if (f2 < f1) {
                                f1 = f2;
                                minidx = k;
                            }
                        }
                        min = convBuf2[minidx];
                    } else {
                        f1 = convBuf2[m + winSizeM1];
                        if (Math.abs(f1) < Math.abs(min)) {
                            min = f1;
                            minidx = m + winSizeM1;
                        }
                    }
                    convBuf3[n] = min;
                    minidx--;
                }
            } // for channels
        } // process
    } // class MinimumFilter

    private static class MedianFilter
            implements RunningWindowFilter {

        final int		winSize, medianOff, winSizeM;
        final int		channels;
        final float[][] buf;
        final int[][]	idxBuf;

        protected MedianFilter(int winSize, int channels) {
            this.winSize	= winSize;
            this.channels   = channels;

            buf			= new float[channels][winSize];
            idxBuf		= new int  [channels][winSize];
            medianOff   = winSize >> 1;
            winSizeM	= winSize - 1;
        }

        public void process(float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len)
                throws IOException {

            int		ch, i, j, k, m, n;
            float[] convBuf1, convBuf2, convBuf3;
            int[]   convBuf4;
            float   f1;

            for (ch = 0; ch < channels; ch++) {
                convBuf1	= buf[ch];
                convBuf2	= inBuf[ch];
                convBuf3	= outBuf[ch];
                convBuf4	= idxBuf[ch];
                m			= inOff;
                n			= outOff;
                convBuf1[0] = convBuf2[m++];
                convBuf4[0] = 0;

                // --- calculate the initial median by sorting inBuf content of length 'winSize ---
                // XXX this is a really slow sorting algorithm and should be replaced by a fast one
                // e.g. by exchanging the j-loop by a step-algorithm (stepping right into
                // i/2 and if f1 < convBuf1[i/2] steppping to i/4 else i*3/4 etc.
                for (i = 1; i < winSize; i++) {
                    f1 = convBuf2[m++];
                    for (j = 0; j < i; j++) {
                        if (f1 < convBuf1[j]) {
                            System.arraycopy(convBuf1, j, convBuf1, j + 1, i - j);
                            for (k = 0; k < i; k++) {
                                if (convBuf4[k] >= j) convBuf4[k]++;
                            }
                            break;
                        }
                    }
                    convBuf1[j] = f1;
                    convBuf4[i] = j;
                }
                // now the median is approx. (for winSize >> 1) the sample in convBuf1[winSize/2]

//System.err.println( "A---unsorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf2[inOff+p] );
//}
//System.err.println( " --sorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf1[p] );
//}

                // XXX this is a really slow sorting algorithm and should be replaced by a fast one
                // e.g. by exchanging the j-loop by a step-algorithm (stepping right into
                // i/2 and if f1 < convBuf1[i/2] steppping to i/4 else i*3/4 etc.
                // ; also the two arraycopies could be collapsed into one or two shorter ones
                for (i = 0; i < len; i++) {
                    convBuf3[n++] = convBuf1[medianOff];

                    j   = convBuf4[i % winSize];  // index of the element to be removed (i.e. shifted left out of the win)
                    System.arraycopy(convBuf1, j + 1, convBuf1, j, winSizeM - j);
                    for (k = 0; k < winSize; k++) {
                        if (convBuf4[k] > j) convBuf4[k]--;
                    }
                    f1  = convBuf2[m++];
                    for (j = 0; j < winSizeM; j++) {
                        if (f1 < convBuf1[j]) {
                            System.arraycopy(convBuf1, j, convBuf1, j + 1, winSizeM - j);
                            for (k = 0; k < winSize; k++) {
                                if (convBuf4[k] >= j) convBuf4[k]++;
                            }
                            break;
                        }
                    }
                    // j = index of the element to be inserted (i.e. coming from the right side of the win)
                    convBuf1[j] = f1;
                    convBuf4[i % winSize] = j;
                }
//System.err.println( "B---unsorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf2[inOff+len+p] );
//}
//System.err.println( " ---sorted---" );
//for( int p = 0; p < winSize; p++ ) {
//	System.err.println( p + " : "+convBuf1[p] );
//}
            } // for channels
        } // process
    } // class MedianFilter

    // Center Clipping for a variable threshold
    // which is determined by a running histogram
    // and a percentage threshold value
    //
    // this only works if a) process() is
    // called on successive chunks; b) samples don't exceed +12 dBFS
    private static class CenterClippingFilter
            implements RunningWindowFilter {

        final int			channels;
        final int			winSizeM1;
        final int[][]		histogram;
        final int			threshSum;
        boolean	init	= false;

        public CenterClippingFilter(int winSize, int channels, double threshAmp) {
            this.channels   = channels;
            winSizeM1		= winSize - 1;
            histogram		= new int[channels][16384];
            threshSum		= (int) (threshAmp * winSize + 0.5);
        }

        public void process(float[][] inBuf, float[][] outBuf, int inOff, int outOff, int len)
                throws IOException {

            float[]		convBuf2, convBuf3;
            int[]		convBuf4;
            int			histoIdx, histoSum;
            float		f1, clip;

            for (int ch = 0; ch < channels; ch++) {
                convBuf4 = histogram[ch];
                convBuf2 = inBuf[ch];
                convBuf3 = outBuf[ch];
                // calc first maximum
//				max			= 0.0f;
//				for( int i = 0, m = inOff; i < len; i++, m++ ) {
//					f1 = Math.abs( convBuf2[ m ]);
//					if( f1 > max ) max = f1;
//				}
                // then calc initial histo
//				for( int i = 0; i < 8192; i++ ) {
//					convBuf4[ i ] = 0;
//				}
                if (!init) {
                    for (int i = 0, j = inOff; i < winSizeM1; i++, j++) {
                        f1 = convBuf2[j];
                        histoIdx = (int) (Math.sqrt(Math.min(1.0f, Math.abs(f1 / 4))) * 16383.5);
//						histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
                        convBuf4[histoIdx]++;
                    }
                }
                for (int j = 0, m = inOff, n = outOff; j < len; j++, m++, n++) {
                    // shift by one : remove obsolete sample
                    // and add new last window sample
                    f1 = convBuf2[m + winSizeM1];
                    histoIdx = (int) (Math.sqrt(Math.min(1.0f, Math.abs(f1 / 4))) * 16383.5);
//					histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
                    convBuf4[histoIdx]++;

                    // find thresh
                    for (histoIdx = 0, histoSum = 0; histoIdx < 8192 && histoSum < threshSum; histoIdx++) {
                        histoSum += convBuf4[histoIdx];
                    }
                    clip	= (float) histoIdx / 16383;
                    clip	= clip*clip*4;
//					clip	= (float) (Math.exp( (histoIdx - 8191) / 381.2437 ) * 4);
                    f1		= convBuf2[m];
                    if (f1 >= 0.0f) {
                        convBuf3[n] = Math.max(0.0f, f1 - clip);
                    } else {
                        convBuf3[n] = Math.min(0.0f, f1 + clip);
                    }
                    f1 = convBuf2[m];    // now obsolete
                    histoIdx = (int) (Math.sqrt(Math.min(1.0f, Math.abs(f1 / 4))) * 16383.5);
//					histoIdx	= 8191 - (int) (Math.log( Math.max( 4.656613e-10, Math.min( 1.0f, Math.abs( f1 / 4)))) * -381.2437);
                    convBuf4[histoIdx]--;
                }
            } // for channels

            init = true;
        } // process
    } // class CenterClippingFilter
}