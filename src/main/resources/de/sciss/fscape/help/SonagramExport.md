# Sonogram Export

<BLOCKQUOTE>Converts a sound file to a sonogram image file.</BLOCKQUOTE>

The sonogram is created using a Constant-Q transform, resulting in a logarithmically spaced frequency axis.
Brightness depends on the logarithmic amplitude in decimals, therefore a _Noise Floor_ must be specified.
The frequency resolution is given by the number of _Bands per Octave_, whereas the _Max Time Resolution_ is
given in milliseconds. The actual temporal resolution in the bass frequencies is a trade-off with the steepness
of the frequency filters, producing some temporal smearing. The FFT size is limited by _Max FFT Size_. The
smaller this size, the less temporal smearing occurs in the bass frequencies, but the worse the frequency
resolution in the bass register.

The image is written as a gray-scale TIFF file.