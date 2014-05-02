# Hilbert Filter

<BLOCKQUOTE>A Hilbert transformer takes an input signal and calculates two output signals that have a
constant 90 degrees phase difference across the whole spectrum. Applications are single sideband modulation (SSB)
also known as frequency shifting and envelope generation.
</BLOCKQUOTE>

## Parameters

_Input file:_ The file to transform

_Output file:_ The 90 degrees outputs can be thought of as the cosine and the sine part of the signal or as the real and imaginary part alternatively. When doing the pure transform
without modifications you'll get two output files. When performing frequency shifting or envelope generation
the output is real again (single file).

_Operation:_ What should be done after the hilbert transform. You can leave the signal unchanged, shift the spectrum up or down or calculate the sound's envelope.

_Anti-aliasing:_ When frequency shifting is applied we will normally want to
have a protection against aliasing. Aliasing occurs for example when you shift a signal downwards by 100 Hz and
the signal has significant energy below 100 Hz. Think of a partial at 30 Hz. When you downshift it by 100 Hz the
mathematical outcome is -70 Hz and physically you'll hear it at +70 Hz. By checking this gadget all signal content
that would generate aliasing is eliminated. There are applications where you want to have aliasing. For example
try to shift a signal up or down half the sampling rate (22050 Hz for a 44.1 kHz sound) - when you allow
aliasing the result will be an inverted spectrum! (1000 Hz becomes 21050 Hz, 2000 Hz becomes 20050 Hz etc.).<p>
Note that because of the anti-aliasing filter the bass frequencies are slightly attenuated. Try to feed this
module with white noise and see what comes out. If you don't like it apply a bass compensation filter afterward.

_Shift amount:_ The the "shift up" and "shift down" operation. Note that
frequency shifting is <B>not</B> pitch transposition. That means that harmonic relationships are destroyed
resulting in bell-like spectra with non-linear partial spacing, the kind of sound you know from ring
modulation.

_Shift envelope:_ Not yet implemented!
