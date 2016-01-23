/*
 *  OSCRouter.java
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
 *		19-Jan-06	created
 *		03-Oct-06	copied from eisenkraut
 */

package de.sciss.fscape.net;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 10-Sep-06
 */
public interface OSCRouter
{
	public String oscGetPathComponent();
	public void oscRoute( RoutedOSCMessage rom );
	public void oscAddRouter( OSCRouter subRouter );
	public void oscRemoveRouter( OSCRouter subRouter );
}
