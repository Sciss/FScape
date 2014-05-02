# Amplitude Shaper

<BLOCKQUOTE>This module lets you change the amplitude envelope of a sound. You can remove it,
replace it or superimpose a second one.
</BLOCKQUOTE>

## Parameters

_Input file:_ Sound whose amplitude shall be modified.

_Envelope file:_ Sound file whose amplitude shall replace or superimpose the original amplitude. Becomes active when Source is set to 'Sound file' or 'Envelope file'.

_Outfile file:_  Modified input file.

_Envelope output:_ Check to create a separate file that contains the original input envelope. This is useful when you want to restore the original envelope after intermediate processing. Besides you can use this envelope file (of ordinary sound file format) as a source of 'Envelope file' for a different AmpShaper process.

_Operation mode:_ In the Source choice gadget you can select which envelope you want to apply -&gt; the one of the input file itself (needed to remove an envelope), the one calculated in-place from another sound file, the one written as a separate envelope output (see parameter 4) or one taken from a graphical envelope editor (this doesn't work yet!). You can feed the module a sound file containing an audio-frequency sine and choose 'Envelope file' as a source to create ring modulation.<br>'Inversion' takes the inverse of the chosen envelope (loud parts become silent and vice versa).<br>The envelope can either replace the original one or superimpose it (i.e. the destination envelope is a combination of the original and the new envelope). Use the combination 'Source=Input file'/'Inversion'/'Superimpose' to flatten the envelope of a sound file.

_Maximum boost:_ Replacement of an envelope can cause almost silent parts to become very loud which may not be desired (because you get just loud (quantization or environmental) noise. This limits the amount of amplitude boost.

_Smoothing:_ Determines the range over which the momentary amplitude is calculated. Affects both input file envelope calculation (in 'Replacement' and 'Source=Input file' mode) and modifier file envelope calculation ('Source=Sound file'). Does not affect a 'Source=Envelope file'. The amplitude is calculated as a RMS value (i.e. all samples are squared, summed, then the square root is taken. It's therefore proportional to the short time signal energy).

_Envelope:_ not yet implemented.

## Notes

You should normalize the output. Because the amplitude is calculated as RMS you would otherwise get very silent or clipped files depending on the application mode.
