# Limiter

<BLOCKQUOTE>This module applies a single band brick-wall limiter to a sound file.
</BLOCKQUOTE>

## Parameters

_Waveform input:_ Sound file to be limited.

_Waveform output:_ Destination sound file.

_Boost:_ Gain applied to the input before entering the limiting stage.

_Ceiling:_ Absolute output ceiling in decibels full-scale.

_Attack:_ Attack time specified as the point where the gain control envelope rises above 1 permille.

_Release:_ Release time specified as the point where the gain control envelope falls below 1 permille.

_Synchronize Channels:_ If checked (default), all channels of a multi-channel file will be affected by the same amount of limiting, taking the maximum limiting factor across the channels. If not checked, the gain control of each channel is independent.

## Notes

This algorithm is as follows: For each successive input sample, a cumulative attenuation gain is calculated, projecting an exponential envelope whose maximum corresponds to this gain onto this cumulative gain buffer. As a result, there is no notion of a sustained limiting or hold segment, but instead the control envelope is automatically smoothed out. The default time constants emphasize loudness, for a more punchy sound increase these values by a factor of five to ten.

The higher the time parameters, the less the distortion products. For example, limiting a sustained full scale 220.5 Hz sine wave with 6 dB boost, produces a third harmonic of strength around -72 dB compared to the fundamental when using the default values of 20ms/200ms. Increasing these values to 200ms/2000ms, the third harmonic appears at around -108 dB (aka inaudible). With the latter setting, a sine tone of 110.25 Hz has a third harmonic as low as -86 dB.