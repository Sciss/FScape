/*
 *  SmpMap.java
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
 */

package de.sciss.fscape.util;

import java.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class SmpMap
implements Cloneable
{
// -------- public Variablen --------

	/**
	 *	READ-ONLY!
	 */
	public	ParamSpace	hSpace;
	public	ParamSpace	vSpace;
	public	int			type;

// -------- private Variablen --------

	protected Vector	smps;		// Element = SmpZones

// -------- public Methoden --------

	public SmpMap( ParamSpace hSpace, ParamSpace vSpace, int type )
	{
		this.hSpace	= hSpace;
		this.vSpace	= vSpace;
		this.type	= type;
		smps		= new Vector();
	}

	public SmpMap( ParamSpace hSpace, ParamSpace vSpace )
	{
		this( hSpace, vSpace, 0 );
	}

	/**
	 *	Clont vorgegebene SmpMap
	 */
	public SmpMap( SmpMap src )
	{
		this.hSpace	= src.hSpace;
		this.vSpace	= src.vSpace;
		this.type	= src.type;
		smps			= (Vector) src.smps.clone();
	}

	public Object clone()
	{
		return new SmpMap( this );
	}
	
	/**
	 *	Fuegt eine neue Sample-Zone in der Karte ein
	 *
	 *	@return	Index in der Sample-Kette; -1 wenn sich die Zone
	 *			mit bereits vorhandenen ueberschneidet oder die
	 *			Zone ausserhalb der Karte liegt
	 */
	public int addSample( SmpZone smp )
	{
		Param	freqHi, freqLo, velHi, velLo;
		double	nFreqHi, nFreqLo, nVelHi, nVelLo;	// neighbour values
		double	dist, nDist;	// distance from topleft marks Vector-index; don't need to take sqrt()
		double	d;
		int		index	= -1;
		SmpZone	neighbour;
	
		freqHi	= Param.transform( smp.freqHi, vSpace.unit, null, null );
		freqLo	= Param.transform( smp.freqLo, vSpace.unit, null, null );
		velHi	= Param.transform( smp.velHi,  hSpace.unit, null, null );
		velLo	= Param.transform( smp.velLo,  hSpace.unit, null, null );

		if( (freqHi != null) && vSpace.contains( freqHi.val ) &&
			(freqLo != null) && vSpace.contains( freqLo.val ) &&
			(velHi  != null) && hSpace.contains( velHi.val )  &&
			(velLo  != null) && hSpace.contains( velLo.val )) {

			d 		= (freqHi.val - vSpace.min) / (vSpace.max - vSpace.min);
			dist	= d*d;
			d		= (velHi.val  - hSpace.min) / (hSpace.max - hSpace.min);
			dist   += d*d;

			index	= this.smps.size();
			for( int i = 0; i < this.smps.size(); i++ ) {
			
				neighbour	= (SmpZone) this.smps.elementAt( i );
				nFreqHi		= Param.transform( neighbour.freqHi, vSpace.unit, null, null ).val;
				nFreqLo		= Param.transform( neighbour.freqLo, vSpace.unit, null, null ).val;
				nVelHi		= Param.transform( neighbour.velHi,  hSpace.unit, null, null ).val;
				nVelLo		= Param.transform( neighbour.velLo,  hSpace.unit, null, null ).val;

				if( index > i ) {

					d 			= (nFreqHi - vSpace.min) / (vSpace.max - vSpace.min);
					nDist		= d*d;
					d			= (nVelHi  - hSpace.min) / (hSpace.max - hSpace.min);
					nDist 	  += d*d;
					
					if( dist <= nDist ) {
						index	= i;		// all following nDists are surely greater
					}
				}

				if( (nVelLo < (velHi.val - Constants.suckyDoubleError) ) &&
					(nVelHi > (velLo.val + Constants.suckyDoubleError) ) &&
					(nFreqLo < (freqHi.val - Constants.suckyDoubleError) ) &&
					(nFreqHi > (freqLo.val + Constants.suckyDoubleError) )) {

					return -1;	// schneidet andere Zone
				}
			}

			if( index >= 0 ) {
				this.smps.insertElementAt( smp, index );
			}
		}

		return index;
	}
	
	/**
	 *	Entfernt eine SampleZone von der Karte
	 *
	 *	@return	false, wenn Zone nicht existiert
	 */
	public boolean removeSample( int index )
	{
		try {
			smps.removeElementAt( index );
			return true;
		}
		catch( IndexOutOfBoundsException e ) {
			return false;
		}
	}

	/**
	 *	Besorgt eine Zone
	 */
	public SmpZone getSample( int index )
	{
		SmpZone smp = null;
	
		try {
			smp = (SmpZone) smps.elementAt( index );
		}
		catch( IndexOutOfBoundsException e ) {}
		
		return smp;
	}
		
	/**
	 *	Besorgt eine Aufzaehlung aller SampleZonen
	 */
	public Enumeration getSamples()
	{
		return smps.elements();
	}

	/**
	 *	Ermittelt Anzahl der SampleZonen
	 */
	public int size()
	{
		return smps.size();
	}

	/**
	 *	Index eines (fiktiven) Punktes erfragen
	 *	DER X-WERT WIRD NICHT GERASTERT
	 *
	 *	@return	der Index ist interpoliert, d.h. der Punkt liegt zwischen
	 *			Math.floor( result ) und Math.ceil( result )!
	 *			Double.NEGATIVE_INFINITY: Punkt liegt links der Kurvengrenze;
	 *			Double.POSITIVE_INFINITY: Punkt liegt rechts der Kurzengrenze;
	 */
/*	public double indexOf( double x )
	{
		int			size = points.size();
		DoublePoint	pt1, pt2, pt3;
		int			index1, index2, index3;
		
		if( size == 0 ) return Double.POSITIVE_INFINITY;					// keine Punkte
		pt1 = (DoublePoint) points.firstElement();
		pt2 = (DoublePoint) points.lastElement();
		if( pt1.x > x ) return Double.NEGATIVE_INFINITY;	// zu klein
		if( pt2.x < x ) return Double.POSITIVE_INFINITY;	// zu gross
		
		index1 = 0;
		index2 = size - 1;

		// Suchverfahren: Strecke immer halbieren und diejenige
		// weiterverfolgen, in der das gesuchte X liegt		
		while( index1 + 1 < index2 ) {
			index3	= (index1 + index2) / 2;		// "floor"
			pt3		= (DoublePoint) points.elementAt( index3 );
			if( pt3.x > x ) {
				pt2		= pt3;
				index2	= index3;
			} else {
				pt1		= pt3;
				index1	= index3;
			}
		}

		if( pt1.x != pt2.x ) {
			return( (double) index1 + (x - pt1.x) / (pt2.x - pt1.x) );
		} else {
			return( (double) index1 );
		}
	}
*/	
	/**
	 *	Transformiert eine Kurve von einer Groesse/Masseinheit (zweidimensional)
	 *	in eine andere (zweidimensional); vgl. auch Param.transform()!
	 *
	 *	- bei gleichen ParamSpaces wird einfach src zurueckgeliefert.
	 *	- ein ParamSpace.fitValue() wird auf jeden Punkt angewandt!
	 *
	 *	@param	hRef		optionale (horizontale) Referenz, der ggf. fuer Umrechnungen von/in
	 *						relative Werte benoetigt wird; kann null sein;
	 *						WENN ER BENOETIGT WIRD, MUSS ER ENTWEDER IN DER FORM
	 *						ABS_UNIT VORLIEGEN ODER (OHNE REFERENZ) IN DIESE UMWANDELBAR SEIN!
	 *
	 *	@param	vRef		optionale vertikale Referenz
	 *
	 *	@param	stream		optionaler Stream-Referrer, der ggf. fuer Umrechnungen
	 *						von/in Beats oder Semitones benoetigt wird; darf null sein
	 *						(ggf. wird dann auf die globale Geschwindigkeit/Scala
	 *						zurueckgegriffen)
	 */
/*	public static SmpMap transform( SmpMap src, ParamSpace destHSpace, ParamSpace destVSpace,
								   Param hRef, Param vRef, SpectStream stream )
	{
		if( destHSpace.contains( src.hSpace ) &&
			destVSpace.contains( src.vSpace )) return src;

		SmpMap		dest = new SmpMap( destHSpace, destVSpace, src.type );
		DoublePoint	pt;
		Param		newX, newY;

		for( int i = 0; i < src.points.size(); i++ ) {
			pt		= (DoublePoint) src.points.elementAt( i );
			newX	= Param.transform( new Param( pt.x, src.hSpace.unit ), destHSpace.unit,
									   hRef, stream );
			newY	= Param.transform( new Param( pt.y, src.vSpace.unit ), destVSpace.unit,
									   vRef, stream );

			if( (newX != null) && (newY != null) ) {
				dest.addPoint( newX.val, newY.val );
			}
		}
		
		return dest;
	}
*/	
// -------- StringComm Methoden --------

	public String toString()
	{
		StringBuffer strBuf;
		
		strBuf = new StringBuffer( hSpace.toString() + '|' + vSpace.toString() + '|' + type );
		
		for( int i = 0; i < smps.size(); i++ ) {
			strBuf.append( "|" + ((SmpZone) smps.elementAt( i )).toString() );
		}
		
		return( strBuf.toString() );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by SmpMap.toString()
	 */
	public static SmpMap valueOf( String s )
	{
		StringTokenizer strTok;
		SmpMap			map;
		
		strTok	= new StringTokenizer( s, "|" );
		map		= new SmpMap( ParamSpace.valueOf( strTok.nextToken() ),		// hSpace
							  ParamSpace.valueOf( strTok.nextToken() ),		// vSpace
							  Integer.parseInt( strTok.nextToken() ));		// type
		
		while( strTok.hasMoreElements() ) {
			map.smps.addElement( SmpZone.valueOf( strTok.nextToken() ));
		}

		return map;
	}
}
// class SmpMap
