# Declick

<BLOCKQUOTE>Digital click artifacts are detected by a stochastic comparison and/or removed by filling
in the silence with a short reverberation.
</BLOCKQUOTE>

## Parameters

_Input/Output:_ Input sound containing clicks and declicked output file.

_Detection sources:_ 'Let FScape detect' activates this module's own mechanism for finding clicks in the input. Controlled by the parameters 3 (see below).<br>'Read markers' includes clicks located by Markers named "Click" in the sound file (e.g. placed in Peak). Therefore, if FScape fails to find clicks itself, you can place manual markers and then use the Declicker only to repair these ones.

_Detection settings:_ Active when 'Let FScape detect' is checked. Detection is based on the assumption of a Gaussian distribution of sample changes (difference between adjacent sample values). Mean and standard deviation are calculated from frames defined by 'Check size'. Then each sample change is threshold by the chosen 'Probability bound' (e.g. if this is set to '1:2000' it means FScape detects a click if there's a sample change in a frame that has a chance of less than 1:2000 to appear in a Gaussian distribution with the calculated mean/stddev). Because such irregularities occur often in very silent regions which are hardly audible as "clicks" you have to choose a reasonable noise floor ('Min. amp').

_Repair modes:_ 'Let FScape repair' activates this module's own mechanism for repairing clicks in in the input. Controlled by parameters 5 and 6 (see below).<br>'Write markers' inserts Markers named 'Click' in the output file, so when you deselect 'Let FScape repair' and check 'Write markers' you can manually repair the detected clicks (for example with the click repair function of Peak).

_X-Fade size:_ When you choose FScape to repair the clicks, it will fadeout the input just before the click and fade it in after the click. In order to conceal this fading the last 'X-Fade size' samples before the click are convolved with an IR specified in parameter 6 (see below) and added to the clip location.

_Impulse response:_ Normally use the file that comes with FScape ('sounds/declickIR.aif'). That is the IR of a short reverb. Therefore when a click occurs the portion before the click is reverberated and covers the attenuated click location. Try small delays as well. The IR should have a quite flat spectrum.

_Click view:_ A visual feedback while processing. Indicates clicks as red vertical lines whereby the horizontal position corresponds with the timeline position in the input file.