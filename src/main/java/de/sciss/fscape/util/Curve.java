/*
 *  Curve.java
 *  FScape
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.fscape.spect.*;

import java.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class Curve
implements Cloneable
{
// -------- public Variablen --------

	public static final int	TYPE_DIA		= 0;
	public static final int	TYPE_ORTHO		= 1;

	/**
	 *	READ-ONLY!
	 */
	public	ParamSpace	hSpace;
	public	ParamSpace	vSpace;
	public	int			type;

// -------- private Variablen --------

	protected Vector	points;		// Element = DoublePoints

// -------- public Methoden --------

	public Curve( ParamSpace hSpace, ParamSpace vSpace, int type )
	{
		this.hSpace	= hSpace;
		this.vSpace	= vSpace;
		this.type	= type;
		points		= new Vector();
	}

	public Curve( ParamSpace hSpace, ParamSpace vSpace )
	{
		this( hSpace, vSpace, TYPE_DIA );
	}

	/**
	 *	Clont vorgegebene Curve
	 */
	public Curve( Curve src )
	{
		this.hSpace	= src.hSpace;
		this.vSpace	= src.vSpace;
		this.type	= src.type;
		points		= (Vector) src.points.clone();
	}

	public Object clone()
	{
		return new Curve( this );
	}
	
	/**
	 *	Fuegt einen neuen Punkt in die Kurve ein
	 *
	 *	@return	Index in der Punkt-Kette; -1 wenn Punkt ausserhalb des Raumes lag
	 *			oder bereits ein Punkt mit exakt demselben X-Wert existiert
	 */
	public int addPoint( double x, double y )
	{
		DoublePoint	ptThis;
		DoublePoint	ptNeighbour;
		double		doubleIndex;
		int			index			= -1;
		int			neighbourIndex	= -1;

		if( hSpace.contains( x ) && vSpace.contains( y ))
		{
			ptThis = new DoublePoint( hSpace.fitValue( x ), vSpace.fitValue( y ));

			doubleIndex	= indexOf( x );

			if( doubleIndex < 0 ) {
				index			= 0;
				neighbourIndex	= 0;
				
			} else if( doubleIndex > points.size() ) {
				index			= points.size();
				neighbourIndex	= index - 1;

			} else {
				index			= (int) Math.ceil( doubleIndex );
				neighbourIndex	= (int) Math.rint( doubleIndex );
			}

			// Abstand ok?
			ptNeighbour	= getPoint( neighbourIndex );
			if( ptNeighbour != null ) {
				if( ((neighbourIndex >= index) &&
					(ptNeighbour.x - hSpace.inc < ptThis.x - Constants.suckyDoubleError)) ||
					((neighbourIndex  < index) &&
					(ptNeighbour.x + hSpace.inc > ptThis.x + Constants.suckyDoubleError)) )
					return -1;
			}

			points.insertElementAt( ptThis, index );
		}

		return index;
	}
	
	public int addPoint( DoublePoint pt )
	{
		return addPoint( pt.x, pt.y );
	}

	/**
	 *	Entfernt einen Punkt aus der Kurve
	 *
	 *	@return	false, wenn Punkt nicht existiert
	 */
	public boolean removePoint( int index )
	{
		try {
			points.removeElementAt( index );
			return true;
		}
		catch( IndexOutOfBoundsException e ) {
			return false;
		}
	}

	/**
	 *	Besorgt einen Punkt
	 */
	public DoublePoint getPoint( int index )
	{
		DoublePoint pt = null;
	
		try {
			pt = (DoublePoint) points.elementAt( index );
		}
		catch( IndexOutOfBoundsException e ) {}
		
		return pt;
	}
		
	/**
	 *	Besorgt eine Aufzaehlung aller Punkte
	 */
	public Enumeration getPoints()
	{
		return points.elements();
	}

	/**
	 *	Ermittelt Anzahl der Punkte
	 */
	public int size()
	{
		return points.size();
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
	public double indexOf( double x )
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
	public static Curve transform( Curve src, ParamSpace destHSpace, ParamSpace destVSpace,
								   Param hRef, Param vRef, SpectStream stream )
	{
		if( destHSpace.contains( src.hSpace ) &&
			destVSpace.contains( src.vSpace )) return src;

		Curve		dest = new Curve( destHSpace, destVSpace, src.type );
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
	
	/**
	 *	Integriert die Kurve ueber ein Intervall und teilt durch
	 *	die Intervallgroesse
	 *
	 *	@param	start	linke  Intervall Grenze, darf "zu klein" sein
	 *	@param	end		rechte Intervall Grenze, darf "zu gross" sein
	 */
	public static double average( Curve c, double start, double end )
	{
		double		width = end - start;
		int			floor, ceil;
		int			firstIndex, lastIndex;	// ceil( startIndex bzw. endIndex )
		double		startIndex, endIndex;
		DoublePoint floorPt, ceilPt;
		DoublePoint	predPt, succPt, lastPt;
		double		integral;
	
		if( width > 0 ) {	// integrieren und teilen

			startIndex	= Math.min( (double) (c.points.size() - 1), Math.max( 0.0, c.indexOf( start )));
			endIndex	= Math.max( 0.0, Math.min( (double) (c.points.size() - 1), c.indexOf( end )));

			// virtueller Startpunkt
			floor		= (int) Math.floor( startIndex );
			ceil		= (int) Math.ceil( startIndex );
			
			floorPt 	= (DoublePoint) c.points.elementAt( floor );
			ceilPt		= (DoublePoint) c.points.elementAt( ceil );

			predPt		= new DoublePoint( start, floorPt.y + (startIndex - floor) *
										   (ceilPt.y - floorPt.y) );
			firstIndex	= ceil;

			// virtueller Endpunkt
			floor		= (int) Math.floor( endIndex );
			ceil		= (int) Math.ceil( endIndex );
			
			floorPt 	= (DoublePoint) c.points.elementAt( floor );
			ceilPt		= (DoublePoint) c.points.elementAt( ceil );

			lastPt		= new DoublePoint( end, floorPt.y + (endIndex - floor) *
										   (ceilPt.y - floorPt.y) );
			lastIndex	= floor;
			
			// integrieren: punkt-zu-punkt flaeche = delta-x * (y0 + 1/2 delta-y) ; aufaddieren
			integral = 0.0;
			for( int i = firstIndex; i <= lastIndex; i++ ) {
				
				succPt		= (DoublePoint) c.points.elementAt( i );
				integral   += (succPt.x - predPt.x) * (predPt.y + (succPt.y - predPt.y) / 2);
				predPt		= succPt;
			}
			integral += (lastPt.x - predPt.x) * (predPt.y + (lastPt.y - predPt.y) / 2);

			return( integral / width );	// average

		} else {			// einzelnen Y-Wert ermitteln

			startIndex	= Math.min( (double) c.points.size(), Math.max( 0.0, c.indexOf( start )));
			
			floor		= (int) Math.floor( startIndex );
			ceil		= (int) Math.ceil( startIndex );
			
			floorPt 	= (DoublePoint) c.points.elementAt( floor );
			ceilPt		= (DoublePoint) c.points.elementAt( ceil );
			
			//					  vvv 0...1 (quasi Gewichtung zwischen floor+ceil)
			return( floorPt.y + (startIndex - floor) * (ceilPt.y - floorPt.y) );
		}
	}
	
// -------- StringComm Methoden --------

	public String toString()
	{
		StringBuffer strBuf;
		
		strBuf = new StringBuffer( hSpace.toString() + ';' + vSpace.toString() + ';' + type );
		
		for( int i = 0; i < points.size(); i++ ) {
			strBuf.append( ";" + ((DoublePoint) points.elementAt( i )).toString() );
		}
		
		return( strBuf.toString() );
	}
	
	/**
	 *	@param	s	MUST BE in the format as returned by Curve.toString()
	 */
	public static Curve valueOf( String s )
	{
		StringTokenizer strTok;
		Curve			c;
		
		strTok	= new StringTokenizer( s, ";" );
		c		= new Curve( ParamSpace.valueOf( strTok.nextToken() ),		// hSpace
							 ParamSpace.valueOf( strTok.nextToken() ),		// vSpace
							 Integer.parseInt( strTok.nextToken() ));		// type
		
		while( strTok.hasMoreElements() ) {
			c.points.addElement( DoublePoint.valueOf( strTok.nextToken() ));
		}

		return c;
	}
}
// class Curve
