# Slice

<BLOCKQUOTE>Slices a sound into a series of equal-length chunks</BLOCKQUOTE>

## Parameters

_Waveform I/O:_ The input file is a sound file which is to be sliced. The output file is a prototype for each slice. For example, if output is &quot;MySoundCut.aif&quot; and &quot;Separate files&quot; is checked, assuming the number of slices to be five, you'll get five files named &quot;MySoundCut1.aif&quot; to &quot;MySoundCut5.aif&quot;. These files can in turn be glued together again using the _Concat_ module. If &quot;Separate files&quot; is __not__ checked, there will be only one output file &quot;MySoundCut.aif&quot; which contains all chunks right after another.

_Slice settings:_ the algorithm initially advances in the input file by the length given in &quot;Skip length&quot; (unless zero). It then writes a chunk of length &quot;Slice length&quot; (first slice). It then advances by the length given in &quot;Skip length&quot; (unless zero). It then writes the second slice and so on. Note that &quot;Skip length&quot; may be negative which means that the slices will overlap. Also note that if &quot;Skip length&quot; is negative and its magnitude is greater than the &quot;Slice length&quot; you can walk backwards in the sound file. A value of &quot;Skip length = -Slice Length&quot; is forbidden.

In the normal mode, as many chunks (slices) are written as possible until reaching the ending (or beginning in &quot;backward mode&quot;) of the input file. &quot;Automatic rescale&quot; enables an alternative mode which rescales &quot;Slice Length&quot; and &quot;Skip Length&quot; by a factor so as to result in a predefined number of slices, given by &quot;# of Slices&quot;. In this alternative mode, you may optionally define a &quot;Final Skip&quot; which is the length between the ending of the last chunk (slice) and the ending of the sound file.

&quot;Normal mode&quot; example: let your input file be one minute long. Assume &quot;Slice Length&quot; to be four and &quot;Skip Length&quot; to be three seconds. Assume &quot;Initial skip&quot; to be two seconds. Output: Slice 1 is taken from input file 00:02 to 00:06 ; slice 2 is taken from input file 00:09 to 00:13 and so on; slice 8 is taken from input file 00:51 to 00:55, slice 9 is taken from input file 00:58 to 01:02 (with the last two seconds being silent). slice 9 is the last slice.

&quot;Automatic rescale&quot; example: let your input file be one minute long. Assume &quot;Slice Length&quot; to be four and &quot;Skip Length&quot; to be three seconds. Assume &quot;Initial skip&quot; to be two and &quot;Final skip&quot; to be three seconds. The net length of the input file is thus 60 seconds minus two minus three seconds = 55 seconds. Now assume &quot;# of Slices&quot; is set to five (you want five slices). &quot;Slice Length&quot; and &quot;Skip Length&quot; are rescaled by a factor of f = 55/(5*4+(5-1)*3) = 1.71875, so each slice has length 6.875 seconds, each skipping amounts to 5.15625 seconds. (the ratio between slice length and skip length remains 4:3).

## Notes

The counter-part to this module is the _Concat_ module.