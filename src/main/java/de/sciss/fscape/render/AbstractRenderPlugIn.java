/*
 *  AbstractRenderPlugIn.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		15-Jul-05	created
 *		??-???-??	copied from eisenkraut
 */

//package de.sciss.eisenkraut.render;
package de.sciss.fscape.render;

import java.io.IOException;
import java.util.prefs.*;
import javax.swing.*;

// @version	0.56, 15-Sep-05
public abstract class AbstractRenderPlugIn
implements RenderPlugIn
{
	/**
	 *	Default behaviour : no user parameters (false)
	 */
	public boolean hasUserParameters()
	{
		return false;
	}
	
	/**
	 *	Default behaviour : shouldn't display parameters (false)
	 */
	public boolean shouldDisplayParameters()
	{
		return false;
	}
	
	/**
	 *	Sub-classes should call super.init() !
	 */
	public void init( /* Document doc, */ Preferences prefs )
	{
		// nothing
	}

	/**
	 *	Sub-classes should call super.init() !
	 */
	public void dispose()
	{
		// nothing
	}
	
	/**
	 *	Default behaviour : returns null (no GUI)
	 */
	public JComponent getSettingsView( RenderContext context )
	{
		return null;
	}
	
	/**
	 *	Default behaviour : simply calls consumer.consumerBegin()
	 */
	public boolean producerBegin( RenderContext context, RenderSource source )
	throws IOException
	{
		return context.getConsumer().consumerBegin( context, source );
	}
	
	/**
	 *	Default behaviour : simply calls consumer.consumerRender(), i.e. bypass
	 */
	public boolean producerRender( RenderContext context, RenderSource source )
	throws IOException
	{
		return context.getConsumer().consumerRender( context, source );
	}

	/**
	 *	Default behaviour : simply calls consumer.consumerFinish()
	 */
	public boolean producerFinish( RenderContext context, RenderSource source )
	throws IOException
	{
		return context.getConsumer().consumerFinish( context, source );
	}
	
	/**
	 *	Default behaviour : simply calls consumer.consumerCancel()
	 */
	public void producerCancel( RenderContext context, RenderSource source )
	throws IOException
	{
		context.getConsumer().consumerCancel( context, source );
	}
}