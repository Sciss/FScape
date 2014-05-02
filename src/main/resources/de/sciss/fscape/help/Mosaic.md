# Mosaic

<BLOCKQUOTE>A technique that tries to imitate photo-mosaicing on the auditory level. Given a visual template of the desired sound (essentially a sonogram) and some input sounds, try to spread chunks of the input sound across the time-frequency plane of the template.
</BLOCKQUOTE>

This algorithm is inspired by the idea of Synthrumentation by Klarenz Barlow. It was developed for the piece &quot;Achrostichon&quot; dedicated to Folkmar Hein.

The template time-frequency plane is given by a black-and-white sonogram _Image input_ file. Frequency is considered logarithmically spaced! Small number of bands (e.g. 1 to 3 per octave) usually produce more interesting results than a literal sonogram. The sound material used to &quot;paint&quot; the template is given by the _Audio input_ field. Note that this file must be a magnitude longer than the target sound file's duration. If necessary, use a sound editor to concatenate the sound material to itself a couple of times to produce the necessary length.

The target duration is given by _Nominal duration_. It is called &quot;nominal&quot; because the actual duration may differ a little bit due to the randomization processes. The time resolution of the input image is automatically derived from the nominal duration, e.g. when the image has a width of 100 pixels and the nominal duration is 10 seconds, each pixel corresponds to 100 ms.

The frequency span of the input image is given by _Min freq._ and _Max freq._, the bands per octave are automatically derived from these settings and the input image's height.

If you take the photo-mosaic metaphor, the algorithm now reads in chunks from the input sound file as &quot;photos&quot; and places them on the time-frequency canvas. The overlapping in the time-frequency plane is specified by _Time Spacing_ and _Freq Spacing_ (where 100% means dense packing without overlap, 50% means 1x overlap etc.). To produce less regular rhythmic and harmonic output, use some _Time Jitter_ and _Freq Jitter_ which is a ratio of the time chunk length and frequency band coverage in percent, by which the &quot;photo&quot; maybe moved from the nominal position.

The smoothness of the &quot;grains&quot; or &quot;photos&quot; depends on the fade-in and fade-out of the chunks, as given by _Attack_ and _Release_.

White pixels in the input image constitute the loud parts, black pixels the silent parts. The corresponding minimum volume is given by the _Noise Floor_ parameter. The parameter _Max Boost_ specifies the maximum gain that can be applied to each &quot;photo&quot; to make it match the template pixel's volume.

Finally, if your input sound material has already been segmented, you can use markers to choose the photo-boundaries instead of the fixed-size time windows resulting from nominal duration divided by image-width. Each new marker in the input sound file marks the beginning of the next photo chunk. To use this approach, check the _Read Markers_ option.
