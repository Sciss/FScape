# Lückenbüßer

<BLOCKQUOTE>Creates a mixture of two sounds by repeatedly cross-fading them at each of their zero crossings.
</BLOCKQUOTE>

The experimental process that starts with input A until a zero-crossing occurs. It then scans input B and finds a zero crossing in B. it writes A up to the crossing and performs a cross-fade for the specified length. Both zero crossings are chosen to have the same tangent signum. the algorithm gives up after the span specified as &quot;patience&quot; and does a cross-fade in this case after the patience span. the process stops when one of the inputs is exhausted.

The idea was to create a mixture of two similar (phase correlated) sounds, but they run out of each other very quickly. often not too interesting results. the idea is nice but the algorithm should be modified by introducing &quot;magnetism&quot; between time offsets in A and B.