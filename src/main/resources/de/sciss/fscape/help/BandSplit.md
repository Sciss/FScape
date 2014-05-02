# Band Splitting

<BLOCKQUOTE>This module takes an input file and writes up to nine output files which are generated from
tuneable bandpass filters. Useful for emulating multi-band behaviour with full-band algorithms,
e.g. dynamics.</BLOCKQUOTE>

## Parameters

_Input file:_ full band input sound file.

_Outfile file:_ prototype file name. FScape inserts a number before the type extension running from 1 to the desired number of bands. E.g. "OutputSplt.aif" would cause FScape to create the files "OutputSplt1.aif", "OutputSplt2.aif" etc.

_Normalize each file:_ When checked each split band file is normalized separately. If not checked, normalization is first applied to the loudest output file and all other gains are adjusted in order to maintain the original relative gains of the bands (i.e. summing the files together restores the original file).

_Number of bands:_ The spectrum of the input file is split up into this number of adjacent bands that cover the full spectrum.

_Maximum boost:_ Replacement of an envelope can cause almost silent parts to become very loud which may not be desired (because you get just loud (quantization or environmental) noise. This limits the amount of amplitude boost.

_Quality:_ determines the length of the FIR bandpass filters. The name is badly chosen because in many applications you will want to have "Low" quality, i.e. short filters to minimize time smearing. Also "better" quality will increase processing time. For 3 or 4 band splitting as a pre-process for separate dynamic treatment "low" is a good choice. The higher the quality the narrower the transition band can get (see parameter 7).

_CrossOvers:_ The (n-1) frequencies dividing the spectrum into (n) bands. E.g. choosing 3 bands with crossovers 1 kHz and 5 kHz will result in three files, the first being the low-pass up to 1 kHz, the second being the bandpass 1 kHz to 5 kHz, the third being the high=pass above 5 kHz.

_Roll-off:_ determines the width of the transition between adjacent bands. 0% means no transition/hard cut. The roll-offs of the band-passes will be as small as possible depending on the quality setting. 100% means the frequency response of each band is in the shape of a raised cosine (cos^2), this is good for separate dynamic processing of the bands because the transitions are smooth enough to conceal the multi-band nature of the processing.
