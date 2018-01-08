/*
 *  LimiterDlg.java
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

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 * Processing module for peak limiting.
 *
 * TODO: Use `FastLog` to improve speed
 */
public class LimiterDlg
        extends ModulePanel {

    // Properties (defaults)
    private static final int PR_INPUTFILE   = 0;        // pr.text
    private static final int PR_OUTPUTFILE  = 1;
    private static final int PR_OUTPUTTYPE  = 0;        // pr.intg
    private static final int PR_OUTPUTRES   = 1;
    private static final int PR_SYNC        = 0;        // pr.bool
    private static final int PR_BOOST       = 0;        // pr.para
    private static final int PR_CEILING     = 1;
    private static final int PR_ATTACK      = 2;
    private static final int PR_RELEASE     = 3;

    private static final String PRN_INPUTFILE   = "InputFile";
    private static final String PRN_OUTPUTFILE  = "OutputFile";
    private static final String PRN_OUTPUTTYPE  = "OutputType";
    private static final String PRN_OUTPUTRES   = "OutputReso";
    private static final String PRN_SYNC        = "Sync";
    private static final String PRN_BOOST       = "Boost";
    private static final String PRN_CEILING     = "Ceiling";
    private static final String PRN_ATTACK      = "Attack";
    private static final String PRN_RELEASE     = "Release";

    private static final String prText[]        = { "", "" };
    private static final String prTextName[]    = { PRN_INPUTFILE, PRN_OUTPUTFILE };
    private static final int    prIntg[]        = { 0, 0 };
    private static final String prIntgName[]    = { PRN_OUTPUTTYPE, PRN_OUTPUTRES };
    private static final boolean prBool[]		= { true };
    private static final String	prBoolName[]	= { PRN_SYNC };
    private static final Param  prPara[]        = { null, null, null, null };
    private static final String prParaName[]    = { PRN_BOOST, PRN_CEILING, PRN_ATTACK, PRN_RELEASE };

    private static final int GG_INPUTFILE   = GG_OFF_PATHFIELD  + PR_INPUTFILE;
    private static final int GG_OUTPUTFILE  = GG_OFF_PATHFIELD  + PR_OUTPUTFILE;
    private static final int GG_OUTPUTTYPE  = GG_OFF_CHOICE     + PR_OUTPUTTYPE;
    private static final int GG_OUTPUTRES   = GG_OFF_CHOICE     + PR_OUTPUTRES;
    private static final int GG_SYNC        = GG_OFF_CHECKBOX   + PR_SYNC;
    private static final int GG_BOOST       = GG_OFF_PARAMFIELD + PR_BOOST;
    private static final int GG_CEILING     = GG_OFF_PARAMFIELD + PR_CEILING;
    private static final int GG_ATTACK      = GG_OFF_PARAMFIELD + PR_ATTACK;
    private static final int GG_RELEASE     = GG_OFF_PARAMFIELD + PR_RELEASE;

    private static PropertyArray static_pr = null;
    private static Presets static_presets = null;

    public LimiterDlg() {
        super("Limiter");
        init2();
    }

    protected void buildGUI() {
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.text = prText;
            static_pr.textName = prTextName;
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.bool		= prBool;
            static_pr.boolName	= prBoolName;
            static_pr.para = prPara;
            static_pr.paraName = prParaName;
            static_pr.para[PR_BOOST]    = new Param(  3.0, Param.DECIBEL_AMP);
            static_pr.para[PR_CEILING]  = new Param( -0.2, Param.DECIBEL_AMP);
            static_pr.para[PR_ATTACK]   = new Param( 20.0, Param.ABS_MS     );
            static_pr.para[PR_RELEASE]  = new Param(200.0, Param.ABS_MS     );

            fillDefaultAudioDescr(static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES);
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();

        // -------- build GUI --------

        GridBagConstraints con;

        PathField ggInputFile, ggOutputFile;
        PathField[] ggInputs;
        ParamField ggParam;
        JCheckBox ggCheck;

        gui = new GUISupport();
        con = gui.getGridBagConstraints();
        con.insets = new Insets(1, 2, 1, 2);

        // -------- input gadgets --------
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;

        gui.addLabel(new GroupLabel("Waveform Input", GroupLabel.ORIENT_HORIZONTAL,
                GroupLabel.BRACE_NONE));

        ggInputFile = new PathField(PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
                "Select input file");
        ggInputFile.handleTypes(GenericFile.TYPES_SOUND);
        con.gridwidth   = 1;
        con.weightx     = 0.1;
        gui.addLabel(new JLabel("File Name:", SwingConstants.RIGHT));
        con.gridwidth   = GridBagConstraints.REMAINDER;
        con.weightx     = 0.9;
        gui.addPathField(ggInputFile, GG_INPUTFILE, null);

        // -------- output gadgets --------
        gui.addLabel(new GroupLabel("Output", GroupLabel.ORIENT_HORIZONTAL,
                GroupLabel.BRACE_NONE));

        ggOutputFile    = new PathField(PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
                PathField.TYPE_RESFIELD, "Select output file");
        ggOutputFile.handleTypes(GenericFile.TYPES_SOUND);
        ggInputs        = new PathField[1];
        ggInputs[0]     = ggInputFile;
        ggOutputFile.deriveFrom(ggInputs, "$D0$F0Lim$E");
        con.gridwidth   = 1;
        con.weightx     = 0.1;
        gui.addLabel(new JLabel("File Name:", SwingConstants.RIGHT));
        con.gridwidth   = GridBagConstraints.REMAINDER;
        con.weightx     = 0.9;
        gui.addPathField(ggOutputFile, GG_OUTPUTFILE, null);
        gui.registerGadget(ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE);
        gui.registerGadget(ggOutputFile.getResGadget(), GG_OUTPUTRES);

        // -------- settings gadgets --------
        gui.addLabel(new GroupLabel("Settings", GroupLabel.ORIENT_HORIZONTAL,
                GroupLabel.BRACE_NONE));

        final ParamSpace[] spcAtkRls = new ParamSpace[] { Constants.spaces[Constants.absMsSpace] };
        final ParamSpace[] spcBoost  = new ParamSpace[] {
            Constants.spaces[Constants.decibelAmpSpace],
            Constants.spaces[Constants.factorAmpSpace ]
        };
        final ParamSpace[] spcCeiling = new ParamSpace[] {
                new ParamSpace(Constants.minDecibel, 0.0, 0.01, Param.DECIBEL_AMP),
                Constants.spaces[Constants.ratioAmpSpace]
        };

        ggParam = new ParamField(spcBoost);
        con.weightx     = 0.1;
        con.gridwidth   = 1;
        gui.addLabel(new JLabel("Boost:", SwingConstants.RIGHT));
        con.weightx     = 0.4;
        gui.addParamField(ggParam, GG_BOOST, null);

        con.ipadx       = 8;
        ggParam         = new ParamField(spcAtkRls);
        con.weightx     = 0.1;
        gui.addLabel(new JLabel("Attack [-60 dB]:", SwingConstants.RIGHT));
        con.ipadx       = 0;
        con.weightx     = 0.4;
        con.gridwidth   = GridBagConstraints.REMAINDER;
        gui.addParamField(ggParam, GG_ATTACK, null);

        ggParam = new ParamField(spcCeiling);
        con.weightx     = 0.1;
        con.gridwidth   = 1;
        gui.addLabel(new JLabel("Ceiling:", SwingConstants.RIGHT));
        con.weightx     = 0.4;
        gui.addParamField(ggParam, GG_CEILING, null);

        con.ipadx       = 8;
        ggParam         = new ParamField(spcAtkRls);
        con.weightx     = 0.1;
        gui.addLabel(new JLabel("Release [-60 dB]:", SwingConstants.RIGHT));
        con.ipadx       = 0;
        con.weightx     = 0.4;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addParamField(ggParam, GG_RELEASE, null);

        con.ipady       = 4;
        ggCheck			= new JCheckBox("Synchronize Channels");
        con.weightx		= 0.5;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        gui.addCheckbox(ggCheck, GG_SYNC, null);

        initGUI(this, FLAGS_PRESETS | FLAGS_PROGBAR, gui);
    }

    public void fillGUI() {
        super.fillGUI();
        super.fillGUI(gui);
    }

    public void fillPropertyArray() {
        super.fillPropertyArray();
        super.fillPropertyArray(gui);
    }

// -------- Processor Interface --------

    protected void process() {
        AudioFile inF   = null;
        AudioFile outF  = null;

        final AudioFileDescr inSpec, outSpec;
        final int numChannels;
        long numFrames;

        final PathField ggOutput;

        final int atkSize, rlsSize;    // sample frames re -150 dB
        final int lapSize, inBufSize, grainBufSize, envSize;
        final float[][]  inBuf;
        final double[][] gainBuf;
        final double[] env;
        final boolean sync;

        final Param ampRef;
        final double ceil, boost;
        final int gainChans;

        final double atk60, rls60, atkCoef, rlsCoef;

    topLevel:
        try {
            // ---- open input ----
            inF         = AudioFile.openAsRead(new File(pr.text[PR_INPUTFILE]));
            inSpec      = inF.getDescr();
            numChannels = inSpec.channels;
            numFrames   = inSpec.length;

            // ---- open output ----
            ggOutput    = (PathField) gui.getItemObj(GG_OUTPUTFILE);
            if (ggOutput == null) throw new IOException(ERR_MISSINGPROP);
            outSpec     = new AudioFileDescr(inSpec);
            ggOutput.fillStream(outSpec);
            outF = AudioFile.openAsWrite(outSpec);
            // .... check running ....
            if (!threadRunning) break topLevel;

            // attack and release durations in sample frames re -60 dB
            atk60      = Math.max(1, (int) AudioFileDescr.millisToSamples(inSpec, pr.para[PR_ATTACK ].value + 0.5));
            rls60      = Math.max(1, (int) AudioFileDescr.millisToSamples(inSpec, pr.para[PR_RELEASE].value + 0.5));
            // damping coefficients
            atkCoef    = Math.pow(1.0e-3, 1.0 / atk60);
            rlsCoef    = Math.pow(1.0e-3, 1.0 / rls60);

            // instead of -60 dB, we calculate the number of frames necessary to fall down to -60*2.5 = -150 dB
            // which is the internal noise floor of the attack/release envelope
            atkSize     = (int) (atk60 * 2.5);
            rlsSize     = (int) (rls60 * 2.5);

            envSize     = atkSize + rlsSize;
            env         = new double[envSize];
            env[atkSize] = 1.0;
            for (int i = 1; i < atkSize; i++) env[atkSize - i] = Math.pow(atkCoef, i);
            for (int i = 1; i < rlsSize; i++) env[atkSize + i] = Math.pow(rlsCoef, i);

            ampRef      = new Param(1.0, Param.ABS_AMP);
            boost       = (Param.transform(pr.para[PR_BOOST  ], Param.ABS_AMP, ampRef, null)).value;
            ceil        = (Param.transform(pr.para[PR_CEILING], Param.ABS_AMP, ampRef, null)).value;

            lapSize     = Math.max(atkSize, 8192);
            grainBufSize = envSize + lapSize;
            sync        = pr.bool[PR_SYNC];

            gainChans   = sync ? 1 : numChannels;
            gainBuf     = new double[gainChans][grainBufSize];
            inBufSize   = atkSize + lapSize;
            inBuf       = new float[numChannels][inBufSize];

            Util.fill(gainBuf, 0, grainBufSize, boost);

            int outSkip = atkSize;
            for (long framesRead = 0L, framesWritten = 0L; (framesWritten < numFrames) && threadRunning; ) {
                // ---- read input ----
                final int readLen = (int) Math.min(lapSize, numFrames - framesRead);
                inF.readFrames(inBuf, atkSize, readLen);

                // ---- adjust gain buffer ----
                for (int i = atkSize, ii = i + readLen; i < ii; i++) {
                    for (int ch = 0; ch < gainChans; ch++) {
                        float max = 0.0f;
                        if (sync) {
                            for (float[] chBuf : inBuf) {
                                max = Math.max(max, Math.abs(chBuf[i]));
                            }
                        } else {
                            max = Math.abs(inBuf[ch][i]);
                        }
                        final double[] gainBufCh = gainBuf[ch];
                        final double gain0  = gainBufCh[i];
                        final double amp0   = max * gain0;
                        // if the cumulative amplitude exceeds the
                        // threshold by now, multiply the gain buffer
                        // with the attack/release envelope for that
                        // particular frame
                        if (amp0 > ceil) {
                            // lin-lin:
                            //   (in - inLow) / (inHigh - inLow) * (outHigh - outLow) + outLow
                            // thus env.linlin(0, 1, 1.0, ceil/amp0) becomes
                            //   (env - 0) / (1 - 0) * (ceil/amp0 - 1) + 1
                            // = env * (ceil/amp0 - 1) + 1
                            //
                            // E.g. if we're overshooting by 3 dB,
                            // then ceil/amp0 = 0.7. At peak we'll multiply by -0.3 + 1.0 = 0.7
                            final double mul = ceil / amp0 - 1.0;

                            for (int j = 0, k = i - atkSize; j < envSize; j++, k++) {
                                gainBufCh[k] *= env[j] * mul + 1.0;
                            }
                        }
                    }
                }

                // ---- calculate output ----
                final int writeLen  = (int) Math.min(lapSize, numFrames - framesWritten) - outSkip;
                final int outStop   = outSkip + writeLen;
                for (int ch = 0; ch < numChannels; ch++) {
                    final double[] gainBufCh    = gainBuf[ch % gainChans];
                    final float[]  inBufCh      = inBuf[ch];
                    for (int i = outSkip; i < outStop; i++) {
                        final float gain = (float) gainBufCh[i];
                        if (gain != 1f) inBufCh[i] *= gain;
                    }
                }

                // ---- write output ----
                outF.writeFrames(inBuf, outSkip, writeLen);

                // ---- buffer rotation ----
                for (double[] gainBufCh : gainBuf) {
                    System.arraycopy(gainBufCh, outStop, gainBufCh, 0, grainBufSize - outStop);
                }
                Util.fill(gainBuf, grainBufSize - outStop, outStop, boost);

                for (float[] inBufCh : inBuf) {
                    System.arraycopy(inBufCh, outStop, inBufCh, 0, inBufSize - outStop);
                }

                outSkip = 0;

                framesRead    += readLen;
                framesWritten += writeLen;

                // .... progress ....
                setProgression((float) ((double) (framesRead + framesWritten) / (numFrames + numFrames)));
            }
            // .... check running ....
            if (!threadRunning) break topLevel;

                // ---- finish ----
            inF.close();
            inF = null;
            outF.close();
            outF = null;

        } catch (IOException e1) {
            setError(e1);
        }

            // ---- clean up (topLevel) ----
        if (inF  != null) inF .cleanUp();
        if (outF != null) outF.cleanUp();
    }
}