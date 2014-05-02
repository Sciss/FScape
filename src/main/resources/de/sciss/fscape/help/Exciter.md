# Exciter

<BLOCKQUOTE>A frequency exciter that generates a signal that is one octave above
the input (second harmonics). Very slow but good working for rich harmonic and not too noisy sounds.</BLOCKQUOTE>

## Parameters

_CrossOvers:_ The frequency band between these margins is split off and taken to the excitation path. Excitation of frequencies above half nyquist (e.g. 11025 Hz for 44.1 kHz samples) is not possible. The smaller the low crossover is the longer the FIRs will get naturally and therefore processing time increases. Besides it hardly makes sense to excite frequencies below 300 or 400 Hz.

_Roll-off:_ The part above the low crossover is high-pass filtered before taken to the excitation stage. The roll-off specifies the transition bandwidth of this high-pass filter. Usually one octave is a good choice (i.e. if low crossover is 400 Hz, then the high-pass has full attenuation at 400 Hz and no attenuation at 800 Hz).

_Bands per octave:_ The exciter works by splitting up the signal into narrow frequency bands and modulating these bands by themselves (producing second order harmonics and DC which is removed). The bigger the number of bands the longer the filter needs to be (parameter 6) and therefore the worse time smearing becomes (Note that 72 bands per octave are senseless when using a short filter length). The smaller the number of bands the better the attack quality is maintained while inter-harmonic distortion increases. The number of bands also proportionally influences processing speed.

_Dry/wet mix:_ In "normal" excitement applications the newly generated harmonics tend to be much more quiet than the dry signal. In other cases you want to get the "solo" excitation which means you should set the dry mix to 0% and wet mix to 100%.

_Filter length:_ The bands are split up using windowed sinc FIR filters. Long FIRs produce a lot of time smearing which means your attacks sound more like water drops than like clicks. When the harmonics are well separated you can use a small filter length (or in case you <b>want</b> inter-harmonic distortion), vice versa. Long filters need much more processing time unfortunately.

## Notes

As an alternative, try the _Chebyshev Waveshaping_ module.
