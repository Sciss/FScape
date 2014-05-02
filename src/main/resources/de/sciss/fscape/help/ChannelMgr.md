# Channel Manager

<BLOCKQUOTE>Utility for splitting multi channel sound files into distinct mono files and vice versa.
</BLOCKQUOTE>

In some situations you may want to combine individual mono files into one interleaved multi-channel file. For example, interleaved files are generally more hard-disk friendly, or an application may not support the synchronization of multiple mono files. In other situations you want to split an interleaved file into individual channels, for example to process them one by one.

This module has two operating modes. The first step is to choose the operation mode based on whether you want to merge mono channels or split a multi-channel file into mono channels.

You then add one or more input files. In splitting mode, you then select the naming scheme for the output files.

## Parameters

_Add file:_ Drop now sound files to be processed. Each new file will be added to the list gadget beneath. Use the Remove/ RemoveAll/ MoveUp/ MoveDown buttons to adjust the order of files and get rid of some or all.

_Operation mode:_ In __"Merge channels"__ mode, all channels from the input files are collected in the order of the list and put together info one target sound file. Input files needn't be mono. E.g., if you have two stereo input files "foo1.aif" and "foo2.aif", you'll get a four channel output file with channels 1 and 2 corresponding to "foo1.aif" and channels three and four representing "foo2.aif". You can mix sampling rates and bit rates and even file lengths, however the target file will have the sampling rate of the first input file (no resampling algorithm is applied) and the length will be the minimum (?) of all input file's lengths. In __"Split channels"__ mode, a single (multichannel) input file is split into multiple mono files.

_Output file:_ Destination sound file and gain control. Note that the default suffix is shown as a hyphen, so if you drop "foo.aif" as input file, the output file will read "foo-.aif". In merging mode, the output file will become "foo-mono.aif", in splitting mode the output files will be renamed "foo-1.aif", "foo-2.aif" or "foo-l.aif", "foo-r.aif" depending on the naming scheme. __Beware:__ if you remove the hyphen from the output file's name you might overwrite your input files.(?)

_Suffix list:_ A list of suffixes used for the output files. You can rearrange them with the MoveUp/ MoveDown buttons. Switch the Auto-scheme gadget to choose between a naming scheme. Whenever you select a suffix from the list, it will be appended to the output file name. You can now edit the output file name and confirm changes by hitting return. The chosen suffix is now known as "manual" and won't be affected by changes of the Auto-scheme. To go back to auto mode, select the suffix from the list and hit "Auto".
