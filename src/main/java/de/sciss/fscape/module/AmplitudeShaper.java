//package de.sciss.fscape.module;
//
//import de.sciss.fscape.gui.PathField;
//import de.sciss.fscape.io.FloatFile;
//import de.sciss.fscape.io.GenericFile;
//import de.sciss.fscape.proc.Module;
//import de.sciss.fscape.util.Param;
//import de.sciss.fscape.util.Util;
//import de.sciss.io.AudioFile;
//import de.sciss.io.AudioFileDescr;
//import de.sciss.io.IOUtil;
//
//import java.io.File;
//import java.io.IOException;
//
//public class AmplitudeShaper extends Module {
//    private final Settings settings;
//
//    enum EnvelopeSource { Input, SoundFile, EnvelopeFile, Envelope }
//
//    public static interface Settings {
//        public File input();
//        public EnvelopeSource source();
//        public File envelopeInput();
//        public File envelopeOutput();
//        public boolean inverted();
//        public boolean rightChannel();
//        public boolean writeEnvelope();
//        public Param maximumChange();
//        public Param envelopeGain();
//    }
//
//    public AmplitudeShaper(Settings settings) {
//        this.settings = settings;
//    }
//
//    public void process()
//    {
//        int					i, j, ch, len, off, chunkLength;
//        long				progOff, progLen;
//        float				f1, f2;
//        double				d1;
//        boolean				extraAudioFile;
//
//        // io
//        AudioFile           inF				= null;
//        AudioFile           outF			= null;
//        AudioFile           envInF			= null;
//        AudioFile           envOutF			= null;
//        AudioFileDescr      inStream		= null;
//        AudioFileDescr      outStream		= null;
//        AudioFileDescr      envInStream		= null;
//        AudioFileDescr      envOutStream	= null;
//        FloatFile[]			outFloatF		= null;
//        FloatFile[]			envFloatF		= null;
//        File                outTempFile[]	= null;
//        File				envTempFile[]	= null;
//        int					inChanNum, outChanNum, envInChanNum, envOutChanNum, shapeChanNum;
//
//        int[][]				shapeChan		= null;
//        int[][]				inChan			= null;
//        float[][]			shapeChanWeight	= null;
//        float[][]			inChanWeight	= null;
//
//        // buffers
//        float[][]			inBuf			= null;		// Sound-In
//        float[][]			outBuf			= null;		// Sound-Out
//        float[][]			inEnvBuf		= null;		// Envelope of Input
//        float[][]			shapeEnvBuf		= null;		// Envelope of Shaper
//        float[][]			envInBuf		= null;		// Direct-In of Shaper-File
//        float[]				convBuf1, convBuf2;
//
//        int					inLength, outLength, envInLength, envOutLength;
//        int					framesRead, framesWritten;		// re sound-files
//        int					framesRead2, framesWritten2;	// re env-files
//
//        Param ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
//        Param				peakGain;
//        float				gain			= 1.0f;								 		// gain abs amp
//        float				envGain			= 1.0f;								 		// gain abs amp
//        float				maxAmp			= 0.0f;
//        float				envMaxAmp		= 0.0f;
//
//        float				maxChange;
//        int					average, avrOff;
//
//        double[]			inEnergy, envInEnergy;
//
//        PathField ggOutput;
//
//        topLevel: try {
//
//            // ---- open input, output; init ----
//
//            // input
//            inF				= AudioFile.openAsRead(settings.input());
//            inStream		= inF.getDescr();
//            inChanNum		= inStream.channels;
//            inLength		= (int) inStream.length;
//            // this helps to prevent errors from empty files!
//            if( (inLength < 1) || (inChanNum < 1) ) errorEmptyFile();
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            envInLength		= 0;
//            envInChanNum	= inChanNum;
//            shapeChanNum	= 0;
//
//            // shape input
//            switch(settings.source()) {
//                case SoundFile:
//                case EnvelopeFile:
//                    envInF			= AudioFile.openAsRead(settings.envelopeInput());
//                    envInStream		= envInF.getDescr();
//                    envInChanNum	= envInStream.channels;
//                    shapeChanNum	= envInChanNum;
//                    envInLength		= (int) envInStream.length;
//                    // this helps to prevent errors from empty files!
//                    if( (envInLength < 1) || (envInChanNum < 1) ) errorEmptyFile();
//
//                    i			= Math.min( inLength, envInLength );
//                    inLength	= i;
//                    envInLength	= i;
//                    break;
//
//                case Envelope:
//                    if(settings.rightChannel()) {
//                        shapeChanNum	= 2;
//                        envInChanNum	= Math.max( envInChanNum, shapeChanNum );	// ggf. mono => stereo
//                    } else {
//                        shapeChanNum	= 1;
//                    }
//                    break;
//
//                case Input:
//                    shapeChanNum	= inChanNum;
//                    break;
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            outChanNum		= Math.max( inChanNum, envInChanNum );
//            outLength		= inLength;
//
//            shapeChan		= new int[ outChanNum ][ 2 ];
//            shapeChanWeight	= new float[ outChanNum ][ 2 ];
//            inChan			= new int[ outChanNum ][ 2 ];
//            inChanWeight	= new float[ outChanNum ][ 2 ];
//            extraAudioFile	= (envInF != null) && (pr.intg[ PR_ENVSOURCE ] == SRC_SOUNDFILE);	// not if SRC_ENVFILE!!!
//
//            // calc weights
//            for( ch = 0; ch < outChanNum; ch++ ) {
//                if( shapeChanNum == 1 ) {
//                    shapeChan[ ch ][ 0 ]		= 0;
//                    shapeChan[ ch ][ 1 ]		= 0;
//                    shapeChanWeight[ ch ][ 0 ]	= 1.0f;
//                    shapeChanWeight[ ch ][ 1 ]	= 0.0f;
//                } else {
//                    f1							= ((float) ch / (float) (outChanNum - 1)) * (float) (shapeChanNum - 1);
//                    shapeChan[ ch ][ 0 ]		= (int) f1;									// Math.max verhindert ArrayIndex-Fehler
//                    shapeChan[ ch ][ 1 ]		= Math.min( (int) f1 + 1, shapeChanNum-1 );	// (Weight ist dabei eh Null)
//                    f1						   %= 1.0f;
//                    shapeChanWeight[ ch ][ 0 ]	= 1.0f - f1;
//                    shapeChanWeight[ ch ][ 1 ]	= f1;
//                }
//                if( inChanNum == 1 ) {
//                    inChan[ ch ][ 0 ]			= 0;
//                    inChan[ ch ][ 1 ]			= 0;
//                    inChanWeight[ ch ][ 0 ]		= 1.0f;
//                    inChanWeight[ ch ][ 1 ]		= 0.0f;
//                } else {
//                    f1							= ((float) ch / (float) (outChanNum - 1)) * (float) (inChanNum - 1);
//                    inChan[ ch ][ 0 ]			= (int) f1;
//                    inChan[ ch ][ 1 ]			= Math.min( (int) f1 + 1, inChanNum-1 );
//                    f1						   %= 1.0f;
//                    inChanWeight[ ch ][ 0 ]		= 1.0f - f1;
//                    inChanWeight[ ch ][ 1 ]		= f1;
//                }
//            /*
//            for( i = 0; i < 2; i++ ) {
//                System.out.println( "shapeChan["+ch+"]["+i+"] = "+shapeChan[ch][i] );
//                System.out.println( "shapeWeig["+ch+"]["+i+"] = "+shapeChanWeight[ch][i] );
//                System.out.println( "inputChan["+ch+"]["+i+"] = "+inChan[ch][i] );
//                System.out.println( "inputWeig["+ch+"]["+i+"] = "+inChanWeight[ch][i] );
//            }
//            */
//            }
//
//            // output
//            ggOutput			= (PathField) gui.getItemObj( GG_OUTPUTFILE );
//            if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
//            outStream			= new AudioFileDescr( inStream );
//            ggOutput.fillStream( outStream );
//            outStream.channels	= outChanNum;
//            outF				= AudioFile.openAsWrite( outStream );
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            envOutLength	= 0;
//            envOutChanNum	= 0;
//
//            // envelope output
//            if(settings.writeEnvelope()) {
//                ggOutput	= (PathField) gui.getItemObj( GG_ENVOUTFILE );
//                if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
//                envOutStream	= new AudioFileDescr( inStream );
//                ggOutput.fillStream( envOutStream );
//                envOutStream.file = settings.envelopeOutput();
//                envOutF			= AudioFile.openAsWrite( envOutStream );
//                envOutLength	= inLength;
//                envOutChanNum	= inChanNum;
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // average buffer size
//            d1			= Param.transform( pr.para[ PR_AVERAGE ], Param.ABS_MS,
//                    new Param( AudioFileDescr.samplesToMillis( inStream, inLength ), Param.ABS_MS ),
//                    null ).val;		// average in millis
//            average		= ((int) (AudioFileDescr.millisToSamples( inStream, d1 ) + 0.5) & ~1) + 1;		// always odd
//            avrOff		= (average >> 1) + 1;				// first element needed for subtraction (see calcEnv())
//
//            progOff		= 0;
//            progLen		= (long) Math.max( average - avrOff, inLength) +
//                    (long) (extraAudioFile ? Math.max( average - avrOff, envInLength ) : envInLength) +
//                    (long) outLength + (long) envOutLength;
//
//            // normalization requires temp files
//            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
//                outTempFile	= new File[ outChanNum ];
//                outFloatF	= new FloatFile[ outChanNum ];
//                for( ch = 0; ch < outChanNum; ch++ ) {		// first zero them because an exception might be thrown
//                    outTempFile[ ch ]	= null;
//                    outFloatF[ ch ]		= null;
//                }
//                for( ch = 0; ch < outChanNum; ch++ ) {
//                    outTempFile[ ch ]	= IOUtil.createTempFile();
//                    outFloatF[ ch ]		= new FloatFile( outTempFile[ ch ], GenericFile.MODE_OUTPUT );
//                }
//                progLen	   += (long) outLength;
//            } else {
//                gain		= (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // normalization requires temp files
//            if( pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY ) {
//                envTempFile	= new File[ envOutChanNum ];
//                envFloatF	= new FloatFile[ envOutChanNum ];
//                for( ch = 0; ch < envOutChanNum; ch++ ) {		// first zero them because an exception might be thrown
//                    envTempFile[ ch ]	= null;
//                    envFloatF[ ch ]		= null;
//                }
//                for( ch = 0; ch < envOutChanNum; ch++ ) {
//                    envTempFile[ ch ]	= IOUtil.createTempFile();
//                    envFloatF[ ch ]		= new FloatFile( envTempFile[ ch ], GenericFile.MODE_OUTPUT );
//                }
//                progLen	   += (long) envOutLength;
//            } else {
//                envGain		= (float) (Param.transform(settings.envelopeGain(), Param.ABS_AMP, ampRef, null )).val;
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // ---- further inits ----
//            maxChange	= (float) (Param.transform(settings.maximumChange(), Param.ABS_AMP, ampRef, null )).val;
//
//            inBuf		= new float[ inChanNum ][ 8192 + average ];
//            Util.clear(inBuf);
//            outBuf		= new float[ outChanNum ][ 8192 ];
//            Util.clear( outBuf );
//            if( extraAudioFile ) {
//                envInBuf= new float[ envInChanNum ][ 8192 + average ];
//                Util.clear( envInBuf );
//            }
//            inEnvBuf	= new float[ inChanNum ][ 8192 ];	// = envOutBuf
//            Util.clear( inEnvBuf );
//            shapeEnvBuf	= new float[ envInChanNum ][ 8192 ];
//            Util.clear( shapeEnvBuf );
//
//            inEnergy	= new double[ inChanNum ];
//            for( ch = 0; ch < inChanNum; ch++ ) {
//                inEnergy[ ch ] = 0.0;
//            }
//            envInEnergy	= new double[ envInChanNum ];
//            for( ch = 0; ch < envInChanNum; ch++ ) {
//                envInEnergy[ ch ] = 0.0;
//            }
//
//            // System.out.println( "inLength "+inLength+"; envInLength "+envInLength+"; envOutLength "+envOutLength+"; outLength "+outLength );
//            // System.out.println( "average "+average+"; avrOff "+avrOff );
//
//            // ----==================== buffer init ====================----
//
//            framesRead	= 0;	// re inF
//            framesRead2	= 0;	// re envInF
//
//            // ---- init buffers ----
//            for( off = avrOff; threadRunning() && (off < average); ) {
//                len = Math.min( inLength - framesRead, Math.min( 8192, average - off ));
//                if( len == 0 ) break;
//
//                inF.readFrames( inBuf, off, len );
//                // calc initial energy per channel (see calcEnv())
//                for( ch = 0; ch < inChanNum; ch++ ) {
//                    convBuf1	= inBuf[ ch ];
//                    d1			= 0.0;
//                    for( i = 0, j = off; i < len; i++ ) {
//                        f1	= convBuf1[ j++ ];
//                        d1 += f1*f1;
//                    }
//                    inEnergy[ ch ] += d1;
//                }
//                framesRead	+= len;
//                off			+= len;
//                progOff		+= len;
//                // .... progress ....
//                setProgression( (float) progOff / (float) progLen );
//            }
//            // zero padding bereits durch initialisierung mit Util.clear() passiert!
//
//            if( extraAudioFile ) {
//                for( off = avrOff; threadRunning() && (off < average); ) {
//                    len = Math.min( envInLength - framesRead2, Math.min( 8192, average - off ));
//                    if( len == 0 ) break;
//
//                    envInF.readFrames( envInBuf, off, len );
//                    // calc initial energy per channel (see calcEnv())
//                    for( ch = 0; ch < envInChanNum; ch++ ) {
//                        convBuf1	= envInBuf[ ch ];
//                        d1			= 0.0;
//                        for( i = 0, j = off; i < len; i++ ) {
//                            f1	 = convBuf1[ j++ ];
//                            d1	+= f1*f1;
//                        }
//                        envInEnergy[ ch ] += d1;
//                    }
//                    framesRead2	+= len;
//                    off			+= len;
//                    progOff		+= len;
//                    // .... progress ....
//                    setProgression( (float) progOff / (float) progLen );
//                }
//                // zero padding bereits durch initialisierung mit Util.clear() passiert!
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // ----==================== the real stuff ====================----
//
//            framesWritten	= 0;	// re OutF
//            framesWritten2	= 0;	// re envOutF
//
//            while( threadRunning() && (framesWritten < outLength) ) {
//
//                chunkLength = Math.min( 8192, outLength - framesWritten );
//                // ---- read input chunk ----
//                len			= Math.min( inLength - framesRead, chunkLength );
//                inF.readFrames( inBuf, average, len );
//                // zero padding
//                for( ch = 0; ch < inChanNum; ch++ ) {
//                    convBuf1 = inBuf[ ch ];
//                    for( i = len, j = len + average; i < chunkLength; i++ ) {
//                        convBuf1[ j++ ] = 0.0f;
//                    }
//                }
//                framesRead	+= len;
//                progOff		+= len;
//                // .... progress ....
//                setProgression( (float) progOff / (float) progLen );
//                // .... check running ....
//                if( !threadRunning() ) break topLevel;
//
//                // ---- read input env chunk ----
//                if( envInF != null ) {
//                    len	= Math.min( envInLength - framesRead2, chunkLength );
//                    if( extraAudioFile) {											// ........ needs averaging ........
//                        envInF.readFrames( envInBuf, average, len );
//                        // zero padding
//                        for( ch = 0; ch < envInChanNum; ch++ ) {
//                            convBuf1 = envInBuf[ ch ];
//                            for( i = len, j = len + average; i < chunkLength; i++ ) {
//                                convBuf1[ j++ ] = 0.0f;
//                            }
//                        }
//                    } else {														// ........ is already env ........
//                        envInF.readFrames( shapeEnvBuf, 0, len );
//                        // zero padding
//                        for( ch = 0; ch < envInChanNum; ch++ ) {
//                            convBuf1 = shapeEnvBuf[ ch ];
//                            for( i = len; i < chunkLength; i++ ) {
//                                convBuf1[ i ] = 0.0f;
//                            }
//                        }
//                    }
//                    framesRead2	+= len;
//                    progOff		+= len;
//                    // .... progress ....
//                    setProgression( (float) progOff / (float) progLen );
//                }
//                // .... check running ....
//                if( !threadRunning() ) break topLevel;
//
//                // ---- calc input envelope ----
//                for( ch = 0; ch < inChanNum; ch++ ) {
//                    inEnergy[ ch ] = calcEnv( inBuf[ ch ], inEnvBuf[ ch ], average, chunkLength, inEnergy[ ch ]);
//                }
//
//                // ---- write output env file ----
//                if( pr.bool[ PR_ENVOUTPUT ]) {
//                    if( envFloatF != null ) {		// i.e. unity gain
//                        for( ch = 0; ch < envOutChanNum; ch++ ) {
//                            convBuf1 = inEnvBuf[ ch ];
//                            for( i = 0; i < chunkLength; i++ ) {	// measure max amp
//                                f1 = Math.abs( convBuf1[ i ]);
//                                if( f1 > envMaxAmp ) {
//                                    envMaxAmp = f1;
//                                }
//                            }
//                            envFloatF[ ch ].writeFloats( convBuf1, 0, chunkLength );
//                        }
//                    } else {						// i.e. abs gain
//                        for( ch = 0; ch < envOutChanNum; ch++ ) {
//                            convBuf1 = inEnvBuf[ ch ];
//                            for( i = 0; i < chunkLength; i++ ) {	// measure max amp + adjust gain
//                                f1				= Math.abs( convBuf1[ i ]);
//                                convBuf1[ i ]  *= envGain;
//                                if( f1 > envMaxAmp ) {
//                                    envMaxAmp = f1;
//                                }
//                            }
//                        }
//                        envOutF.writeFrames( inEnvBuf, 0, chunkLength );
//                    }
//                    framesWritten2	+= chunkLength;
//                    progOff			+= chunkLength;
//                    // .... progress ....
//                    setProgression( (float) progOff / (float) progLen );
//                }
//                // .... check running ....
//                if( !threadRunning() ) break topLevel;
//
//                // ---- calc shape envelope ----
//                switch( pr.intg[ PR_ENVSOURCE ]) {
//                    case SRC_INPUT:								// shape env = input env
//                        for( ch = 0; ch < inChanNum; ch++ ) {
//                            System.arraycopy( inEnvBuf[ ch ], 0, shapeEnvBuf[ ch ], 0, chunkLength );
//                        }
//                        break;
//                    case SRC_SOUNDFILE:							// calc shape env from envInBuf
//                        for( ch = 0; ch < envInChanNum; ch++ ) {
//                            envInEnergy[ ch ] = calcEnv( envInBuf[ ch ], shapeEnvBuf[ ch ], average, chunkLength, envInEnergy[ ch ]);
//                        }
//                        break;
//                    case SRC_ENVFILE:							// nothing to do, we have already loaded the env
//                        break;									//    in the correct buffer
//                    case SRC_ENV:
//                        throw new IOException( "Graphic env not yet supported" );
//                }
//
//                // ---- calc output ----
//                // first generate output envelope
//                switch( pr.intg[ PR_MODE ]) {
//                    case MODE_SUPERPOSE:
//                        if( !pr.bool[ PR_INVERT ]) {			// multiply by shape
//                            for( ch = 0; ch < outChanNum; ch++ ) {
//                                convBuf1 = outBuf[ ch ];
//                                for( i = 0; i < chunkLength; i++ ) {
//                                    f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
//                                            shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
//                                    convBuf1[ i ] = Math.min( maxChange, f1 );
//                                }
//                            }
//
//                        } else {								// divide by shape
//                            for( ch = 0; ch < outChanNum; ch++ ) {
//                                convBuf1 = outBuf[ ch ];
//                                for( i = 0; i < chunkLength; i++ ) {
//                                    f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
//                                            shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
//                                    if( f1 > 0.0f ) {
//                                        convBuf1[ i ] = Math.min( maxChange, 1.0f / f1 );
//                                    } else {
//                                        convBuf1[ i ] = maxChange;
//                                    }
//                                }
//                            }
//                        }
//                        break;
//
//                    case MODE_REPLACE:
//                        if( !pr.bool[ PR_INVERT ]) {			// shape / input
//                            for( ch = 0; ch < outChanNum; ch++ ) {
//                                convBuf1 = outBuf[ ch ];
//                                for( i = 0; i < chunkLength; i++ ) {
//                                    f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
//                                            shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
//                                    f2 = inEnvBuf[ inChan[ ch ][ 0 ]][ i ] * inChanWeight[ ch ][ 0 ] +
//                                            inEnvBuf[ inChan[ ch ][ 1 ]][ i ] * inChanWeight[ ch ][ 1 ];
//                                    if( f2 > 0.0f ) {
//                                        convBuf1[ i ] = Math.min( maxChange, f1 / f2 );
//                                    } else {
//                                        convBuf1[ i ] = 0.0f;	// input ist eh ueberall null, somit unveraenderlich
//                                    }
//                                }
//                            }
//
//                        } else {								// 1 / (shape * input)
//                            for( ch = 0; ch < outChanNum; ch++ ) {
//                                convBuf1 = outBuf[ ch ];
//                                for( i = 0; i < chunkLength; i++ ) {
//                                    f1 = shapeEnvBuf[ shapeChan[ ch ][ 0 ]][ i ] * shapeChanWeight[ ch ][ 0 ] +
//                                            shapeEnvBuf[ shapeChan[ ch ][ 1 ]][ i ] * shapeChanWeight[ ch ][ 1 ];
//                                    f1*= inEnvBuf[ inChan[ ch ][ 0 ]][ i ] * inChanWeight[ ch ][ 0 ] +
//                                            inEnvBuf[ inChan[ ch ][ 1 ]][ i ] * inChanWeight[ ch ][ 1 ];
//                                    if( f1 > 0.0f ) {
//                                        convBuf1[ i ] = Math.min( maxChange, 1.0f / f1 );
//                                    } else {
//                                        convBuf1[ i ] = maxChange;
//                                    }
//                                }
//                            }
//                        }
//                        break;
//                }
//                // then multiply input bites
//                if( inChanNum == outChanNum ) {		// no weighting - use faster routine
//                    for( ch = 0; ch < outChanNum; ch++ ) {
//                        convBuf1 = outBuf[ ch ];
//                        convBuf2 = inBuf[ ch ];
//                        for( i = 0, j = avrOff; i < chunkLength; i++, j++ ) {
//                            convBuf1[ i ] *= convBuf2[ j ];
//                        }
//                    }
//                } else {
//                    for( ch = 0; ch < outChanNum; ch++ ) {
//                        convBuf1 = outBuf[ ch ];
//                        for( i = 0, j = avrOff; i < chunkLength; i++, j++ ) {
//                            f1 = inBuf[ inChan[ ch ][ 0 ]][ j ] * inChanWeight[ ch ][ 0 ] +
//                                    inBuf[ inChan[ ch ][ 1 ]][ j ] * inChanWeight[ ch ][ 1 ];
//                            convBuf1[ i ] *= f1;
//                        }
//                    }
//                }
//
//                // ---- write output sound file ----
//                if( outFloatF != null ) {		// i.e. unity gain
//                    for( ch = 0; ch < outChanNum; ch++ ) {
//                        convBuf1 = outBuf[ ch ];
//                        for( i = 0; i < chunkLength; i++ ) {	// measure max amp
//                            f1 = Math.abs( convBuf1[ i ]);
//                            if( f1 > maxAmp ) {
//                                maxAmp = f1;
//                            }
//                        }
//                        outFloatF[ ch ].writeFloats( convBuf1, 0, chunkLength );
//                    }
//                } else {						// i.e. abs gain
//                    for( ch = 0; ch < outChanNum; ch++ ) {
//                        convBuf1 = outBuf[ ch ];
//                        for( i = 0; i < chunkLength; i++ ) {	// measure max amp + adjust gain
//                            f1				= Math.abs( convBuf1[ i ]);
//                            convBuf1[ i ]  *= gain;
//                            if( f1 > maxAmp ) {
//                                maxAmp = f1;
//                            }
//                        }
//                    }
//                    outF.writeFrames( outBuf, 0, chunkLength );
//                }
//                framesWritten	+= chunkLength;
//                progOff			+= chunkLength;
//                // .... progress ....
//                setProgression( (float) progOff / (float) progLen );
//
//                // ---- shift buffers ----
//                for( ch = 0; ch < inChanNum; ch++ ) {	// zero padding is performed after AudioFile.readFrames()!
//                    System.arraycopy( inBuf[ ch ], chunkLength, inBuf[ ch ], 0, average );
//                }
//                if( extraAudioFile ) {
//                    for( ch = 0; ch < envInChanNum; ch++ ) {	// zero padding is performed after AudioFile.readFrames()!
//                        System.arraycopy( envInBuf[ ch ], chunkLength, envInBuf[ ch ], 0, average );
//                    }
//                }
//
//            } // until framesWritten == outLength
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // ---- normalize output ----
//
//            // sound file
//            if( pr.intg[ PR_GAINTYPE ] == GAIN_UNITY ) {
//                peakGain = new Param( (double) maxAmp, Param.ABS_AMP );
//                gain	 = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP,
//                        new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
//                f1		 = 1.0f;
//                if( (envOutF != null) && (pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY) ) {	// leave prog space
//                    f1	 = (1.0f + getProgression()) / 2;
//                }
//                normalizeAudioFile( outFloatF, outF, outBuf, gain, f1 );
//                for( ch = 0; ch < outChanNum; ch++ ) {
//                    outFloatF[ ch ].cleanUp();
//                    outFloatF[ ch ] = null;
//                    outTempFile[ ch ].delete();
//                    outTempFile[ ch ] = null;
//                }
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // envelope file
//            if( (envOutF != null) && (pr.intg[ PR_ENVGAINTYPE ] == GAIN_UNITY) ) {
//                peakGain = new Param( (double) envMaxAmp, Param.ABS_AMP );
//                envGain	 = (float) (Param.transform( pr.para[ PR_ENVGAIN ], Param.ABS_AMP,
//                        new Param( 1.0 / peakGain.val, peakGain.unit ), null )).val;
//
//                normalizeAudioFile( envFloatF, envOutF, inEnvBuf, envGain, 1.0f );
//                for( ch = 0; ch < envOutChanNum; ch++ ) {
//                    envFloatF[ ch ].cleanUp();
//                    envFloatF[ ch ] = null;
//                    envTempFile[ ch ].delete();
//                    envTempFile[ ch ] = null;
//                }
//            }
//            // .... check running ....
//            if( !threadRunning() ) break topLevel;
//
//            // ---- Finish ----
//
//            outF.close();
//            outF		= null;
//            outStream	= null;
//            if( envOutF != null ) {
//                envOutF.close();
//                envOutF		= null;
//                envOutStream= null;
//            }
//            if( envInF != null ) {
//                envInF.close();
//                envInF		= null;
//                envInStream	= null;
//            }
//            inF.close();
//            inF			= null;
//            inStream	= null;
//            outBuf		= null;
//            inBuf		= null;
//            inEnvBuf	= null;
//            envInBuf	= null;
//            shapeEnvBuf	= null;
//
//            // inform about clipping/ low level
//            maxAmp		*= gain;
//            handleClipping( maxAmp );
//            envMaxAmp	*= envGain;
////			handleClipping( envMaxAmp );	// ;( routine nicht flexibel genug!
//
//        }
//        catch( IOException e1 ) {
//            setError( e1 );
//        }
//        catch( OutOfMemoryError e2 ) {
//            inStream	= null;
//            outStream	= null;
//            envInStream	= null;
//            envOutStream= null;
//            inBuf		= null;
//            outBuf		= null;
//            inEnvBuf	= null;
//            envInBuf	= null;
//            shapeEnvBuf	= null;
//            convBuf1	= null;
//            convBuf2	= null;
//            System.gc();
//
//            errorOutOfMemory();
//        }
//
//        // ---- cleanup (topLevel) ----
//        if( inF != null ) {
//            inF.cleanUp();
//            inF = null;
//        }
//        if( outF != null ) {
//            outF.cleanUp();
//            outF = null;
//        }
//        if( envInF != null ) {
//            envInF.cleanUp();
//            envInF = null;
//        }
//        if( envOutF != null ) {
//            envOutF.cleanUp();
//            envOutF = null;
//        }
//        if( outFloatF != null ) {
//            for( ch = 0; ch < outFloatF.length; ch++ ) {
//                if( outFloatF[ ch ] != null ) outFloatF[ ch ].cleanUp();
//                if( outTempFile[ ch ] != null ) outTempFile[ ch ].delete();
//            }
//        }
//        if( envFloatF != null ) {
//            for( ch = 0; ch < envFloatF.length; ch++ ) {
//                if( envFloatF[ ch ] != null ) envFloatF[ ch ].cleanUp();
//                if( envTempFile[ ch ] != null ) envTempFile[ ch ].delete();
//            }
//        }
//    }
//
//    /*
//     *	@param	a			Quell-Wellenform
//     *	@param	env			Ziel-RMS
//     *	@param	average		Laenge der Samples in a, aus denen jeweils ein RMS berechnet wird
//     *						(RMS = sqrt( energy/average ))
//     *	@param	length		Zahl der generierten RMS (in env)
//     *	@param	lastEnergy	Rueckgabewert aus dem letzten Aufruf dieser Routine
//     *						(richtige Initialisierung siehe process(): summe der quadrate der prebuffered samples)
//     *	@return				neuer Energiewert, der beim naechsten Aufruf als lastEnergy uebergeben werden muss
//     */
//    private static double calcEnv(float[] a, float[] env, int average, int length, double lastEnergy) {
//        for (int i = 0, j = average; i < length; i++, j++) {            //   zu alten leistungswert "vergessen" und
//            lastEnergy = lastEnergy - a[i] * a[i] + a[j] * a[j];    // neuen addieren
//            env[i] = (float) Math.sqrt(lastEnergy / average);
//        }
//        return lastEnergy;
//    }
//}
