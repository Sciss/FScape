# Make Loop

<BLOCKQUOTE>Apply a cross-fade to a sound so that it can be seamlessly played back as a loop</BLOCKQUOTE>

This works by creating a cross-faded overlap between the beginning and ending of the sound. either a piece from the beginning is cut away (= cross-fade position &quot;end of loop \ pre loop /&quot;) or the ending is cut away (= cross-fade position &quot;begin of loop \ post loop /&quot;). in the former case the maximum fade length is determined by the initial skip setting, in the latter case the maximum fade length is determined by the final skip setting. the actual cross-fade length is the minimum of this implicit maximum length and the &quot;cross-fade length&quot; setting.

for example, with cross-fade position &quot;end of loop \ pre loop /&quot;, choosing an initial skip of 2 seconds and a cross-fade length of 1 second results in a sound that is 2 seconds shorter than the input sound, starting directly at offset 2 seconds, running right to the end and having a final cross-fade of one second at the very end that makes the loop work.

for strictly phase correlated signals, an &quot;equal energy&quot; fade type is provided, however usually signals are not correlated and you use &quot;equal power&quot; to approximately maintain original loudness during the cross-fade.
