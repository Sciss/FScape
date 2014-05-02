# Resample

<BLOCKQUOTE>You can either resample a sound to be of shorter duration/higher pitch or
longer duration/lower pitch or use this module for sampling rate conversion.
</BLOCKQUOTE>

You can either change the speed and pitch of a file, retaining its nominal sampling rate, or convert from one sampling rate to another (e.g. 44.1 kHz to 48.0 kHz). This depends on the setting of the _Change Pitch/Speed_ button.

This module uses band-limited resampling using sinc-interpolated low-pass filters, as described by Julius O. Smith.

## Parameters

_Input/output files:_ The sampling rate of the input is the
reference value for the "new rate" field.

_New rate:_ Destination sampling rate. For resampling check
the "Keep old rate" checkbox and enter the amount of semitones in this field. Note that due to
the logic of the conversion positive semitone shift corresponds to a lower pitched output and
vice versa. To get a sound with a pitch one octave above the original one choose "-12 semi". For
one octave down choose "+12 semi". For sampling rate conversion select the absolute "Hz" unit
and enter the desired rate. Deselect the "Change Pitch/Speed" button.

_Rate modulation:_ Activate the checkbox if you want the rate
to change dynamically over time. In the ParamField right to the checkbox choose the maximum
deviation relative to the "new rate". Click on the envelope icon to edit the modulation
curve.

_Distinct right channel:_ Activate the checkbox if you want
a different modulation on the right channel. If the input file has more than two channels the
first envelope corresponds to channel 1, the second one to the highest channel, all other
channels are linearly interpolated. Note that due to round-off errors you might end up in
a severe phase offset between two channels. Also note that at the moment the envelope is sampled
at an interval of 4 milliseconds and not continuously.

_Desired length:_ For resampling, instead of entering the pitch
shift you can enter the destination file length. The "new rate" field is updated automatically
and vice versa. At the moment it is not possible to specify a destination length when the rate
is modulated.

_Change Pitch/Speed:_ When checked, the output file's header contains the same sampling rate as the input file. E.g. if your input is 44.1 kHz and you choose a new rate of "+12 semi" then with
this button checked you get an output at 44.1 kHz which is one octave down. When you deselect this
button, you do nominal sample rate conversion: With "+12 semi" you get the same process as before, but the output file has a nominal sampling rate of 88.2 kHz, therefore sounding exactly like the input when played back properly at 88.2 kHz.

_Quality:_ The algorithm performs a band-limited sinc interpolation which is the most accurate method for resampling. A sinc is of infinite length, therefore we have to truncate it. Shorter FIRs speed up the processing but result in slightly worse quality and a broader low-pass transition band. The FIR is stored in a table,
and if "Interpolate" is checked the table entries are linearly interpolated, improving the noise level even further although using more CPU time to render.

## Notes

For nominal conversions, you will want to change the _New Rate_ unit type to absolute Hz. You can then enter the desired target file's sampling rate.