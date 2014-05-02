# Concat

<BLOCKQUOTE>Glues a series of sounds together</BLOCKQUOTE>

A series of systematically named files is taken and concatenated, optionally applying a cross-fade and overlap between their succession. The series is specified as the first and last file in the series, and the module then tries to determine the system of naming. For this to work, the file names must be identical except for a monotonously increasing or decreasing integer number component.

## Parameters

_Waveform I/O:_ The sounds to be concatenated should contain integer numbers in their names. So if you drop "foo001.aif" as the first sound and "foo123.aif" as the last sound, the current input gadget will tell you that the programme detected 123 sound files. Alternatively, you can name the first file "foo1.aif". For a reverse sorting, use "foo123.aif" as the first file and "foo001.aif" or "foo1.aif" as the last file.

_Scissor settings:_ Each input sound file is known as a "chunk". You can cut off some of each sounds beginning by increasing the __chunk offset__, or truncate the chunk's length (cutting off bits at the end) by using a value smaller than 100% for the __chunk length__.

A __chunk overlap__ of zero means a new chunk starts exactly one sample frame after the last sample of the preceding chunk, whereas increasing the chunk overlap allows you to blend the sounds together, applying a cross-fade. Note that the chunk overlap is not identical to the cross-fade lengths, which is specified by __Overlap Cross-fade__. So, if you have an overlap of one second and no cross-fade, then chunk 2 simply starts one second before chunk 1 is finished, but will not be faded in or out. If you set Overlap Cross-fade to 100%, then the whole _overlapping region_ is blended.

_Cross-fade Type:__ If the chunks are phase-correlated choose the Equal-Energy cross-fade type, in most other cases you will want to choose Equal-Power cross-fades which usually give the smoothest transition without an audible dip in the loudness.

## Notes

You can use this tool also to mix multiple signals together without any spacing between them. This is done by setting the __Chunk Overlap__ to 0%.

The counter-part to this module is the _Slice_ module.