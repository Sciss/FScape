# Bleach

<BLOCKQUOTE>Employs an adaptive whitening filter, using a simple LMS algorithm. The result is that resonant parts of a sound are removed.
</BLOCKQUOTE>

 A brief overview is given for example in <A HREF="http://saba.kntu.ac.ir/eecd/taghirad/E%20books/TOC/Adaptive%20Filters.pdf" CLASS="ext" TARGET="_blank">saba.kntu.ac.ir/eecd/taghirad/E%20books/TOC/Adaptive%20Filters.pdf</A>. The module can be used in inverse mode, subtracting the whitened signal and thus retaining the resonant aspects of a sound.

Be careful with the feedback gain. Values above -50 dB can blow up the filter.