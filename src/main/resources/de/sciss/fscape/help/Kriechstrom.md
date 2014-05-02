# Kriechstrom

<BLOCKQUOTE>This module provides a granular synthesis generator. I used to like the GRM Freeze plugin a lot, so this is kind of the same idea.
</BLOCKQUOTE>

The input sound is split into chunks (small grains or greater pieces) specified by Min/Max chunk length. Min/Max simultaneous chunks specifies the density of the texture (inverse proportional to rendering speed). You can choose whether chunks may be repeated (Min/Max repeats) and if you allow repetition, if the chunk length should be altered on each repetition ("Instantaneous length update"). The maximum distance in the original sound file between two chunks is given by "Max. entry offset", where increasing the offset significantly slows down the algorithm. "Filter amount" should be set to 0% (50% default is bad) because any other value will add a random low or high pass filter (depending on the colour setting) which tends to make all output sound bubbling.
