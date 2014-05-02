# Complex Convolution

<BLOCKQUOTE>This works exactly as the normal _Convolution_ module, however it is capable of using arbitrary length impulse responses (larger than fit into the working memory), and using complex input (a combination of real and imaginary files such as output by the fourier translation module).
</BLOCKQUOTE>

The normal convolution module needs to load the impulse response completely into the RAM, which can take up a lot if you bear in mind that sample resolution is 32bit internal, that impulses can be multichannel and that for convolution the buffer needs to be twice the size of the IR. so for impulses with durations greater than 30 seconds or one minute, the module may break down with a out-of-memory exception. In this case, you can use the complex-convolution module which is performing a partitioned convolution.

You specify the amount of RAM in megabytes that you wish to dedicate to the process. Depending on this value and the size and number of channels of the input files, the amount of steps into which the process is split, is shown in the text field right to the &quot;Mem.alloc&quot; gadget. When the step size increases way beyond 32 or so, the process may take up a really long time, so try to increase memory in this case.

When you leave the imaginary inputs unchecked, you perform a normal (real) convolution. Note that in version 0.68 the cepstral options are hidden because they never really worked.
