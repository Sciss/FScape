/*
 *  VectorSpace.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
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
 *		16-Feb-05	created
 */

package de.sciss.fscape.gui;

import java.awt.geom.*;

/**
 *  @version	0.71, 15-Nov-07
 */
public class VectorSpace
{
	public final double		hmin, hmax, vmin, vmax;
	public final boolean	hlog, vlog;
	private final double	h0, hoffset, v0, voffset;
//	private final String	hlabel, hunit, vlabel, vunit;
	private final double	hlogfactor, vlogfactor;
	
	private VectorSpace( double hmin, double hmax, double vmin, double vmax,
						 boolean hlog, boolean vlog, double h0, double v0,
						 String hlabel, String hunit, String vlabel, String vunit )
	{
		this.hmin		= hmin;
		this.hmax		= hmax;
		this.vmin		= vmin;
		this.vmax		= vmax;
		this.hlog		= hlog;
		this.vlog		= vlog;
		this.h0			= h0;
		this.v0			= v0;
//		this.hlabel		= hlabel;
//		this.hunit		= hunit;
//		this.vlabel		= vlabel;
//		this.vunit		= vunit;
		
		this.hlogfactor	= Math.log( hmax / h0 );
		this.vlogfactor	= Math.log( vmax / v0 );
		this.hoffset	= Math.log( hmin / h0 ) / hlogfactor;
		this.voffset	= Math.log( vmin / v0 ) / vlogfactor;
	}

	public double hUnityToSpace( double hu )
	{
		if( hlog ) {
			return( Math.exp( (hu * (1.0 - hoffset) + hoffset) * hlogfactor ) * h0 );
		} else {
			return( hmin + (hmax - hmin) * hu );
		}
	}

	public double vUnityToSpace( double vu )
	{
		if( vlog ) {
			return( Math.exp( (vu * (1.0 - voffset) + voffset) * vlogfactor ) * v0 );
		} else {
			return( vmin + (vmax - vmin) * vu );
		}
	}
		
	public double hSpaceToUnity( double hs )
	{
		if( hlog ) {
			return( (Math.log( hs / h0 ) / hlogfactor - hoffset) / (1.0 - hoffset) );
		} else {
			return( (hs - hmin) / (hmax - hmin) );
		}
	}

	public double vSpaceToUnity( double vs )
	{
		if( vlog ) {
			return( (Math.log( vs / v0 ) / vlogfactor - voffset) / (1.0 - voffset) );
		} else {
			return( (vs - vmin) / (vmax - vmin) );
		}
	}
	
	public Point2D unityToSpace( Point2D unityPt )
	{
		return new Point2D.Double( hUnityToSpace( unityPt.getX() ), vUnityToSpace( unityPt.getY() ));
	}

	public Point2D spaceToUnity( Point2D spacePt )
	{
		return new Point2D.Double( hSpaceToUnity( spacePt.getX() ), vSpaceToUnity( spacePt.getY() ));
	}
	
	public static VectorSpace createLinSpace( double hmin, double hmax, double vmin, double vmax,
											  String hlabel, String hunit, String vlabel, String vunit )
	{
		return new VectorSpace( hmin, hmax, vmin, vmax, false, false, hmin, vmin,
								hlabel, hunit, vlabel, vunit );
	}

	public static VectorSpace createLogLinSpace( double hmin, double hmax, double hcenter, double vmin, double vmax,
												 String hlabel, String hunit, String vlabel, String vunit )
	{
		double h0		= hcenter * hcenter / hmax;

		return new VectorSpace( hmin, hmax, vmin, vmax, true, false, h0, vmin,
								hlabel, hunit, vlabel, vunit );
	}

	public static VectorSpace createLinLogSpace( double hmin, double hmax, double vmin, double vmax, double vcenter,
												 String hlabel, String hunit, String vlabel, String vunit )
	{
		double v0		= vcenter * vcenter / vmax;

		return new VectorSpace( hmin, hmax, vmin, vmax, false, true, hmin, v0,
								hlabel, hunit, vlabel, vunit );
	}

	public static VectorSpace createLogSpace( double hmin, double hmax, double hcenter,
											  double vmin, double vmax, double vcenter,
											  String hlabel, String hunit, String vlabel, String vunit )
	{
		double h0		= hcenter * hcenter / hmax;
		double v0		= vcenter * vcenter / vmax;

		return new VectorSpace( hmin, hmax, vmin, vmax, true, true, h0, v0,
								hlabel, hunit, vlabel, vunit );
	}
}