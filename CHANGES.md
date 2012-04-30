## change history

### v0.74 (nov 2010 -- SVN rev. 69)

 - New: Dr Murke
 - Improved: Added Icon for Windows
 - Fixes: StepBackDlg (lost one corrStep per marker pos), BinaryOpDlg (fixing percentage offset), OS X Bug where Preferences menu item disappeared
 - Help files: Mosaic, Blunderfinger, Sediment

### v0.73 (jun 2010 -- SVN rev. 63)

 - New: Sonagram export, Beltrami decomposition, Bleach, AmpEnvOp (Spectral)
 - Improved: Mosaic (read-markers option), Batch (open-module), Prefs (audiofile defaults), SuperCollider (postWindowProcess)
 - Fixes: Mosaic, ScissLib (w64), Batch (loop-variables), GUI (menu accelerator blockage in OS X, numberfield commit)
 - Help files: Mosaic, Blunderfinger, Sediment

### v0.72 (jan 2009 -- SVN rev. 31)

 - new modules: SMPTE Synthesizer, BlunderFinger, Mosaic
 - new spectral operators: LogFreq
 - externalizing ScissLib, support for Wave64
 - bug fixes

### v0.71 (nov 2007)

 - got rid of Xcode, now using Eclipse and Ant
 - updated application framework classes from Eisenkraut
 - bug fixes
 - new module (experimental): Sediment

### v0.70 (oct 2006)

 - integrates OSC server. very basic functionality for now, but already usefull for running fscape in installations for example
 - updated application framework classes from eisenkraut
 - new window management allows to switch to internal-frames mode on windows systems
 - new modules: splice, makeloop
 - fixes: convolution (echoes in minimum phase mode), probably others

v0.701 fixes bugs in parameter field gadgets and opening AIFF files with APPL chunks.

### v0.681 (jun 2005)

 - needlehole cherry blossom: new center clipping filter. for each step in the sliding window, values below the given clipping thresh re the current peak amplitude will be clipped.
 - path button : now you can not only drop files from the finder onto this icon, but you can click and drag from this button onto an application in the finder's dock, such as QuickTime player, Peak or AudioSculpt.
 - complex convolution : the cepstral options are hidden now because they never worked. the module has been greatly speed up by providing a user definable amount of RAM allocation.
 - chebychev waveshaping: now the frequency and time interpolation works (all nine panels are functional).
 - convolution : a new option has been added to make a minimum phase�version of the impulse response which results in a reverberant / resonant kind of character. this is still buggy and sometimes produces a strange delay for some IR lengths.
 - fir designer : a new option has been added to make a minimum phase version of the filter (instead of symmetric linear-phase which is the default). there's no perfect way now to determine the IR length, it's just empirical, so the resulting FIRs are maybe longer than needed. The minimum phase filters tend to sound more punchy at least for long filter lengths.
 - envelope editor : the GUI was better integrated in java swing, graphic anti-aliasing is used, the default envelope is now a simple rising line instead of the old strange ASR curve.
 - new cd verification module (miscellaneous menu)
 - the convert-files module was accidentally hidden in the previous version and can now be found in the miscellaneous menu. You can use it to convert spectral files (as written by the output-file module in the spectral patcher) into TIFF images to process in Photoshop, and vice versa.
 - a lot of classes have been unified between Meloncillo and FScape and placed in a higher level hierarchy package, such as de.sciss.io, de.sciss.gui, de.sciss.app . This includes the new help dialog for example, and the new preferences dialog (which now allows selection of a GUI look-and-feel).
 - minor improvements in the GUI of the spectral patcher (java swing integration)
 - temp files are now written as 32bit floating point aiff files instead of headerless files. this allows you to access these files directly while processing, for example by making a preliminary copy using the change-gain module!
 - pausing + resuming modules works again. the keyboard shortcuts have been changed. to pause and resume a module, press Meta(=Apply key)+Shift+Comma. to stop a module, press Meta+Period.
 - the preferences classes of java 1.4 are used now, so the application's preferences is saved in the default place of your operating system, such as ~/Library/Preferences/de.sciss.fscape.plist on MacOS. the separate packages de.sciss.io and de.sciss.gui save their preferences separately which means these preferences (such as your favourite user paths) are shared between FScape and Meloncillo.
 - The presets files are force-saved each time a preset is saved or deleted. This should fix the bug of presets being not saved sometimes. However I encountered that overwriting an existing preset is not always permanently saved. This has to be monitored for a next version.

### v0.66 (feb 2005)

 - bugfixes in FIRDesigner (array-index-exception), PathField (wrong # of channels display), passim. Statistics has improved display (still limited to left channel however). Needlehole has Std.deviation and Minimum filters.

### v0.65 (jan 2005)

 - this is the first open-source GPL version. all macos dependencies have been removed and it was tested on win xp, linux should work but not yet tested. a few modules have been added (rotation, ichneumon, lueckenbuesser, sachensucher), a lot of bugs have been fixed, some modules have been improved (unary-operator offset, reverse; fir designer spectral display preliminary; statistics display field). the audio file classes have been replaced by those from <a href="http://www.sciss.de/meloncillo">Meloncillo</a> and WAVE format is supported now. some cosmetics. everything has been packaged using <A HREF="http://www.izforge.com/izpack/">IzPack</A>.

### v0.61 (jul 2004)

 - added more brief help pages. (use options-menu -&gt; show help)
