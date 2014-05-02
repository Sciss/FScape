# Sediment

<BLOCKQUOTE>Spreads crumbles of one file (pattern) across the timeline of another (control).</BLOCKQUOTE>

To do so, a multi-pass process is applied: For the duration of the pattern file, sequentially a grain is extracted from this file, having a random duration within the given limits. Next, the whole control file is traversed and the pattern grain correlated against it. The "best" matching position is memorized and the grain is "plotted" at this position in the output file, potentially scaled due to the time-scale setting.

Automatic gain control is employed to match the pattern and control grain, where the boosting of the pattern grain is limited by the max-boost setting. To avoid clicks, the an envelope is applied to each pattern grain, using a fade-length determined by the win-size setting.

Determination of the "best" match in multichannel situations is controlled by the multichannel-correlation setting. Finally, the clump parameter can be used to speed up processing. The clump size determines how many grains are generated for each pass through the control file. At the small disadvantage of having several grains of exactly the same size, this clumping saves FFT calculations in the generation of the control file spectra.
