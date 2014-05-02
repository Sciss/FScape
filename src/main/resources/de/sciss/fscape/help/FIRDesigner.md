# FIR Designer

<BLOCKQUOTE>A patch panel where you can place and connect simple filters for low/high/band-pass and
band-stop. The output is a finite impulse response file that can be used in the _Convolution_ module</BLOCKQUOTE>

There is a tab view to switch between a circuit building panel and a spectrum plot of the resulting filter. Filter units are created by double-clicking on the circuit panel. When a unit is selected, its parameters can be adjusted on the right hand side.

## Parameters

_File name:_ The FIR Designer creates an ordinary sound file that contains the impulse response of the particular filter. This sound file can be used as the IR in the _Convolution_ module. Since you normally won't listen to it, 32-bit floating point is usually the best choice for output resolution.

### Circuit panel

The desired filter is built up from elementary sinc-filters (brick wall high-pass/low-pass/band-pass/notch). You can assembly any number of those filters in series are parallel to create the desired frequency and phase response.<br>Double click on the panel to add a new basic filter. Double click left or right to an existing filter block the create a serial setting. Double click above or below an existing filter block to create a parallel setting. Very often you only need one filter block. Alt+Click will erase one block. The black line symbolizes the input/output wire. To change the type and settings of a filter block, just click on that block (it turns blue) and adjust the settings on the right (parameters 3 to 6).

_Type:_ Basic filter type. 'Allpass' means a flat spectrum (a unit impulse, possibly delayed and attenuated). 'Subtract' means that filter is subtracted from a unit impulse, therefore turning a low-pass into a high-pass etc. All filters are windowed sinc-filters as designed by the Kaiser method.

_Cutoff/Bandwidth:_ Margin from pass-band to stop-band. For band-passes and notches you also have a gadget for the bandwidth. In that case the cutoff is the middle frequency - e.g. if cutoff is 400 Hz and bandwidth is 200 Hz, the low frequency of the band-pass becomes 300 Hz and the upper frequency becomes 500 Hz. Cutoff becomes a geometric middle frequency (Sqrt(F_upper*F_lower)) when the bandwidth is given in semitones - e.g. if cutoff is 400 Hz and bandwidth is 12 semitones, the low frequency of the band-pass becomes 283 Hz and the upper frequency becomes 566 Hz. This is really awful and should be replaced by a direct parameter for lower/upper cutoff frequency.

_Roll-off_ can be specified for a cosine-modulated filter. Without roll-off, the filter will have brick wall characteristic.

_Gain/Delay:_ When building a circuit consisting of multiple elementary filter blocks these define the attenuation and delay of each block. You could, for example, place one allpass with zero delay and unity gain in parallel to an attenuated low-pass delayed by 1000 ms to create a tape style delay effect IR.

_Overtones:_ Following a stupid idea of myself. I wanted to create a filter that removes 50 Hz hum so I thought I'd build it up of a bunch of narrow band-passes tuned to 50 Hz, 100 Hz, 150 Hz etc. For band-passes and notches checking this gadget will cause the designer to generate multiple band-passes with the same settings but each spaced from the fundamental band-pass as given in 'Spacing' parameter until the "overtone" frequency reaches the 'Limit freq'. Well it works but don't expect to be able to remove hum with this.

_Filter length/window:_ In theory the sinc-filters are infinitely long. Therefore to realize them in a computer they have to be truncated at a reasonable number of taps. The higher the quality setting the longer the FIR will be resulting in shorter transition bands. Often, however, it is not desirable to have that narrow transition widths, so you might choose a low "quality" as well. Besides, the longer the filters gets, the more time smearing is produced by the filtering.<br>After truncation a window (an envelope rising from zero to maximum and ending again at zero) is applied to the filter. The windowing is a compromise between the amount of stop-band ripple that results from the truncation and the width of the transition band. Try out different windows and watch the result in the _Statistics_ module's spectrum view. Often Blackman is a good choice.

_Normalize gain:_ When checked the output file will have 0 dB peak. When not, the filter is just written "as is". That means the filter gain is untouched, so you can subtract for example a sound convolved with say a low pass filter from the wet signal and get exactly the high-pass signal. Often desirable when floating point output is written.