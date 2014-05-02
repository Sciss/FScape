# Change Gain

<BLOCKQUOTE>This module simply changes the gain (volume) of a sound file. Besides it can be used to find the peak sample and the RMS energy of a file.
</BLOCKQUOTE>

## Parameters

_Waveform input:_ Sound file to be volume adjusted.

_Info button:_ Click to find the peak and RMS of the input file. The text box is frequently updated while scanning the file. Preliminary results are placed in brackets. A letter or number attached to the peak location indicates the channel in which the peak occurs. Once the info has been generated normalization is faster because FScape remembers the peak. Therefore if you change the file contents after the info has been output you have to re-enter the input name or re-press the info button to notify this module.

_Waveform output:_ Destination sound file and gain control. If the gain type is set to "immediate" then the input is directly tuned according to the dB amount. In the "normalized" mode the input is first normalized and then adjusted by the dB amount (head-room).

## Notes

A special feature is marker printout to the console, invoked by holding down the <tt>Meta</tt> or <tt>Alt</tt> modifier key while clicking on the Info button.