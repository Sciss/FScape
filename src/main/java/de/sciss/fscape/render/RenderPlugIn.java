/*
 *  RenderPlugIn.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		14-Jul-05	created (somewhat modelled from Meloncillo)
 *		??-???-??	copied from eisenkraut
 */

//package de.sciss.eisenkraut.render;
package de.sciss.fscape.render;

import javax.swing.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public interface RenderPlugIn {

    public String getName();
    public boolean hasUserParameters();
    public boolean shouldDisplayParameters();

    /**
     *  This gets called right after the
     *  instantiation of a new render module
     */
    public void init(Preferences prefs);

    public void dispose();

    /**
     *  Asks the plugin to return a component
     *  suitable for presenting to the user
     *  in order to allow parameter adjustments
     *  The provided PlugInContext can be used
     *  to query the number of receivers, transmitters
     *  etc. Since the user might change the
     *  transmitter or receiver collection etc.
     *  before the actual start of the plugin processing,
     *  this method might be called several times,
     *  asking the plugin to re-adapt to the new
     *  values in the context.
     *
     *	@param	context		the context which may serve as
     *						a hint on how to display the GUI.
     *	@return	a component containing the plug-in specific
     *			GUI elements which will be attached to the
     *			host frame.
     */
    public JComponent getSettingsView(RenderContext context);

    /**
     *  Begins the rendering. If the parameters are
     *  not workable for the module, it should throw
     *  or set an Exception or warnings and return false.
     *  It shall return true on success. It can make
     *  adjustments to the RenderContext by setting options
     *  like KEY_TARGETRATE, KEY_MINBLOCKSIZE etc. which
     *  will be read out by the host. Though access to
     *  Session is provided through the init() method,
     *  the render module should only use the fields provided
     *  by the context, such as getSourceRate(), getReceivers()
     *  etc. It needn't deal with door locking which is
     *  provided by the host.
     *
     *	@param	context	render context
     *	@param	source	render source. the plug-in should
     *					check the requests related to the data
     *					it wishes to receive.
     *	@return	<code>false</code> if an error occurs
     *			and rendering should be aborted
     *
     *	@throws	IOException	if a read/write error occurs
     */
    public boolean producerBegin(RenderContext context, RenderSource source)
            throws IOException;

    /**
     *  Renders some output from the provided block of
     *  sense data. Options like target sample-rate or
     *  block size are considered to be set in beginRender()
     *  and thus it's not guaranteed that the host check
     *  a modification of these values. The module should
     *  invoke host.setProgression() if possible allowing
     *  the user to predict the duration of the rendering.
     *
     *	@param	context	render context
     *	@param	source	render source containing the current
     *					data  block
     *	@return	<code>false</code> if an error occurs
     *			and rendering should be aborted
     *
     *	@throws	IOException	if a read/write error occurs
     */
    public boolean producerRender(RenderContext context, RenderSource source)
            throws IOException;

    /**
     *  Allows the render module to perform any necessary
     *  finishing activities like closing files or
     *  normalizing output.
     *
     *	@param	context	render context
     *	@param	source	render source
     *	@return	<code>false</code> if an error occurs
     *			and rendering should be aborted
     *
     *	@throws	IOException	if a read/write error occurs
     */
    public boolean producerFinish(RenderContext context, RenderSource source)
            throws IOException;

    /**
     *  Tells the module that the rendering was aborted.
     *  The module should perform any necessary cleanups
     *  and return as soon as possible.
     *
     *	@param	context	render context
     *	@param	source	render source
     *
     *	@throws	IOException	if a read/write error occurs
     */
    public void producerCancel(RenderContext context, RenderSource source)
            throws IOException;
}