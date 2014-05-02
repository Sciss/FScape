# Wavelet Translation

<BLOCKQUOTE>Transform from time domain to a multi-resolution sub-band decoding which is a mixed time-frequency 'scalogram'. Wavelets are small "packets" of signals that can be thought of as high- and low-pass
filters. A wavelet transform applies those filters recursively to a signal.</BLOCKQUOTE>

This module contains a pyramidal algorithm: The signal is high- and low-pass filtered and decimated (half of the samples are "thrown away"). Then the low-pass filtered signal is again put through the high/low-pass stage and so on until the size of the decimated sub-band signal is very small (the size of the filter kernel).

What you get is a multi-resolution sub-band coded version of the signal, a mixture of a time domain and a frequency domain representation. High frequency information has bad frequency resolution and good time resolution, low frequency information has a fine frequency resolution and a bad time resolution which somehow corresponds to our auditive perception system. Like the _Fourier Translation_ module, the transform is invertible. Unlike the fourier transform the signals are always real and not complex and the filter has compact support (i.e. the coefficients are localized in time while the fourier transform uses sinusoidal filters that have an infinite duration).

## Parameters

_Input and output file:_ Time domain or wavelet domain signal
depending on the direction chosen.
The output of the forward transform is that of a pyramid: We start with the few coefficients of the last
low-pass filter stage followed by the last high-pass filter. Next we find the high-pass coefficients of the
successive scales (going from sub-bands belonging to lower frequencies to those belonging to high frequencies).
Each successive sub-band is twice as long as its predecessor.

_Filter:_ Although an infinite number of filters (wavelets) exist you can
only choose between a few that are widely used and were introduced by Ingrid Daubechies. The number after the
name corresponds to the filter order (number of coefficients). The difference between the higher order filters
is very small while the Daub4 is most different from the others. Try to use different filters for forward and
backward transform! Unfortunately the module requests that the input file for a backward transform be of a
certain size (the exact size of a forward transform with the same filter). Be careful when you process the
forward transformed files because changing the length of the file may cause FScape to reject the file for
backward translation. This will be fixed in the next versions.

_Gain per Scale:_ Often the coefficients belong to low scales are much higher
than those belong to high scales. This can cause problems for integer output files because it will introduce
high quantization noise in the high scales. Try to tune this parameter so that the volume of the forward
transform output will remain approximately constant over the whole file (alternatively use floating point
files).

_Direction:_ Forward for time input/ fourier output; backward for fourier input/ time output.
