/*
 *  AbstractRenderPlugIn.java
 *  FScape
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

import de.sciss.app.Document;

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
	public void init( Document doc, Preferences prefs )
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