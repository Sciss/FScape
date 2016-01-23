/*
 *  Curve2.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

/**
 *  @version	0.71, 15-Nov-07
 */
public class Curve2
{
// -------- public Variablen --------

	public static final int	INT_LINEAR		= 0x000;
	public static final int	INT_SPLINE		= 0x001;
	public static final int	INT_SAMPLEHOLD	= 0x002;
	public static final int	INTMASK			= 0x00F;

	public static final int	TYPE_BIPOLAR	= 0x000;
	public static final int	TYPE_UNIPOLAR	= 0x010;

	public	int			flags;
	public	int			size;		// in x, y ; arrays duerfen aber laenger sein; mind. 2!
	public	float[]		xs;			// nur im Bereich 0...1 ; Randpunkte nicht loeschen! Keine doppelten x
	public	float[]		ys;
	public	boolean		looped		= false;
	public	float		loopStart	= 0.0f;
	public	float		loopEnd		= 1.0f;
	public	int			loopCount	= 1;

// -------- private Variablen --------

	private boolean		valid		= false;
//	private int			clcLpStart, clcLpEnd;
	private float[]		yDrv		= null;		// zweite Ableitung fuer Spline-Interpolation

// -------- public Methoden --------

	public Curve2( int flags )
	{
		this.flags	= flags;
		size		= 2;
		xs			= new float[ 2 ];
		ys			= new float[ 2 ];
		xs[ 0 ]		= 0.0f;
		ys[ 0 ]		= 0.0f;
		xs[ 1 ]		= 1.0f;
		ys[ 1 ]		= 1.0f;
	}

	public Curve2()
	{
		this( INT_LINEAR + TYPE_BIPOLAR );
	}

	/**
	 *	Clont vorgegebene Curve
	 */
	public Curve2( Curve2 src )
	{
		this.flags		= src.flags;
		this.size		= src.size;
		this.looped		= src.looped;
		this.loopStart	= src.loopStart;
		this.loopEnd	= src.loopEnd;
		this.loopCount	= src.loopCount;

		this.xs			= new float[ size + 16 ];
		this.ys			= new float[ size + 16 ];
		System.arraycopy( src.xs, 0, this.xs, 0, size );
		System.arraycopy( src.ys, 0, this.ys, 0, size );
	}

	public Object clone()
	{
		return new Curve2( this );
	}

	/**
	 *	Needs to be called when x, y, size or flags are changed!
	 */
	public void invalidate()
	{
		valid = false;
	}

	/**
	 *	Array-Position eines Abszissen-Wertes bestimmen
	 *
	 *	@return		x liegt zwischen diesem Index und dem diesem+1 (incl.)
	 *				-1 wenn out of range
	 */
	public int indexOf( float x )
	{
		if( looped && (x > loopStart) && (x < loopEnd) ) {
			x = loopStart + (((x - loopStart) * loopCount) % (loopEnd - loopStart));
		}
		
		if( (x < this.xs[ 0 ]) || (x > this.xs[ size-1 ])) return -1;
		
		// bisection; see NumericalRecipes 3.4
		int	idxHi, idxLo, idxMid;

		idxLo	=	0;
		idxHi	=	size - 1;
		while( (idxHi - idxLo) > 1) {
			idxMid = (idxHi + idxLo) >> 1;
			if( x >= this.xs[ idxMid ])	idxLo = idxMid;
			else						idxHi = idxMid;
		}		return idxLo;
	}

	/**
	 *	Array-Position eines Abszissen-Wertes bestimmen
	 *
	 *	@param		x 		Wert
	 *	@param		idxLo	alter Index; damit ist die Suche schneller als obige Routine
	 *	@return				x liegt zwischen diesem Index und dem diesem+1 (incl.)
	 *						-1 wenn out of range
	 */
	public int indexOf( float x, int idxLo )
	{
		if( looped && (x > loopStart) && (x < loopEnd) ) {
			x = loopStart + (((x - loopStart) * loopCount) % (loopEnd - loopStart));
		}
		
		if( (x < this.xs[ 0 ]) || (x > this.xs[ size-1 ])) return -1;
		
		// hunt; see NumericalRecipes 3.4
		int	idxHi, idxMid, inc;

		if( idxLo >= 0 ) {
			inc = 1;	// Initial hunting increment.
			if( x >= this.xs[ idxLo ]) {	// Hunt up
				idxHi = idxLo + 1;
				if( idxHi >= size - 1 ) return idxLo;
				while( x >= this.xs[ idxHi ]) {
					idxLo	= idxHi;
					inc	  <<= 1;
					idxHi  += inc;
					if( idxHi >= size - 1 ) {
						idxHi = size - 1;
						break;
					}
				} // Done hunting, value bracketed.
			} else {					// Hunt down
				idxHi = idxLo;
				idxLo--;
				while( x < this.xs[ idxLo ]) {
					idxHi	= idxLo;
					inc   <<= 1;
					idxLo  -= inc;
					if( idxLo < 0 ) {
						idxLo = 0;
						break;
					}
				} // Done hunting, value bracketed.
			}
		} else {
			idxLo = 0;
			idxHi = size - 1;
		}

// System.out.println( "lo "+idxLo+"; hi "+idxHi );
		// bisection phase:
		while( (idxHi - idxLo) > 1) {
			idxMid = (idxHi + idxLo) >> 1;
			if( x >= this.xs[ idxMid ])	idxLo = idxMid;
			else						idxHi = idxMid;
		}		return idxLo;
	}

	/**
	 *	Ggf. interpolierten Wert an der Stelle x berechnen
	 */
	public float calc( float x )
	{
		return calc( x, indexOf( x ));
	}

	/**
	 *	len Werte im Bereich startX bis stopX berechnen und in a ab off speichern
	 */
	public void calc( float startX, float stopX, float[] a, int off, int len )
	{
		len--;
		
		float	stepX	= (stopX - startX) / len;
		float	x;
		int		idx		= indexOf( startX );

		a[ off ]		= calc( startX, idx );		// first value outside loop
	
		for( int i = 1, j = off; i < len; i++ ) {
			x			= startX + i * stepX;
			idx			= indexOf( x, idx );
			a[ ++j ]	= calc( x, idx );
		}

		a[ off+len ]	= calc( stopX, idx );		// last value outside loop
	}

// -------- private Methoden --------

	private void validate()
	{
//		if( looped ) {
//			clcLpStart	= (int) Math.ceil( indexOf( loopStart ));
//			clcLpEnd	= (int) indexOf( loopEnd );
//		} else {
//			clcLpStart	= 0;
//			clcLpEnd	= size - 1;
//		}
		if( (flags & INT_SPLINE) != 0 ) {
			if( (yDrv == null) || (yDrv.length < size) || ((yDrv.length - 64) > size) ) {
				yDrv = new float[ size + 16 ];
			}
			// spline preparation: second order derivates; see NumericalRecipes 3.3
			float[]	u		= new float[ size - 1 ];
			int		i, j, k;
			float	p, sig;
			
			yDrv[ 0 ]			= 0.0f;		// "natural" splines
			u[ 0 ]				= 0.0f;
			yDrv[ size - 1 ]	= 0.0f;
			for( i = 1, j = 0, k = 2; k < size; i++, j++, k++ ) {
				sig			= (xs[ i ] - xs[ j ]) / (xs[ k ] - xs[ j ]);
				p			= sig * yDrv[ j ] + 2.0f;
				yDrv[ i ]	= (sig - 1.0f) / p;
				u[ i ]		= (ys[ k ] - ys[ i ]) / (xs[ k ] - xs[ i ]) -
							  (ys[ i ] - ys[ j ]) / (xs[ i ] - xs[ j ]);
				u[ i ]		= (6.0f * u[ i ] / (xs[ k ] - xs[ j ]) - sig * u[ j ]) / p;
			}			
			for( i = size - 2; i >= 0; i-- ) {
				yDrv[ i ] = yDrv[ i ] * yDrv[ i+1 ] + u[ i ];
			}
		}
		valid = true;
	}

	private float calc( float x, int idxLo )
	{
		if( !valid ) validate();
		if( idxLo == -1 ) return 0.0f;
		
		switch( flags & INTMASK ) {
		case INT_SAMPLEHOLD:
			if( x < this.xs[ idxLo+1 ]) {
				return( this.ys[ idxLo ]);
			} else {
				return( this.ys[ idxLo+1 ]);
			}
		case INT_SPLINE:
			return spline( x, idxLo );
		default:	// INT_LINEAR
			return linear( x, idxLo );
		}
	}

	private float linear( float x, int idxLo )
	{

		int		idxHi;
		float	h, b, a;

		idxHi	= idxLo + 1;
		h		= this.xs[ idxHi ] - this.xs[ idxLo ];
		a		= (this.xs[ idxHi ] - x) / h;
		b		= 1.0f - a;
		
		return( a * this.ys[ idxLo ] + b * this.ys[ idxHi ]);
	}
	
	// adapted from NumericalRecipes 3.3
	private float spline( float x, int idxLo )
	{
		int		idxHi;
		float	h, b, a;

		idxHi	= idxLo + 1;
		h		= this.xs[ idxHi ] - this.xs[ idxLo ];
		a		= (this.xs[ idxHi ] - x) / h;
		b		= 1.0f - a; // (x - this.x[ idxLo ]) / h;

		return( a * this.ys[ idxLo ] + b * this.ys[ idxHi ] + 
				((a*a*a - a) * yDrv[ idxLo ] + (b*b*b - b) * yDrv[ idxHi ]) * (h*h) / 6.0f );
	}

// -------- StringComm Methoden --------
/*
	public String toString()
	{
		StringBuffer strBuf;
		
		strBuf = new StringBuffer( hSpace.toString() + ';' + vSpace.toString() + ';' + type );
		
		for( int i = 0; i < points.size(); i++ ) {
			strBuf.append( ";" + ((DoublePoint) points.elementAt( i )).toString() );
		}
		
		return( strBuf.toString() );
	}
*/
	/**
	 *	@param	s	MUST BE in the format as returned by Curve2.toString()
	 */
/*	public static Curve2 valueOf( String s )
	{
		StringTokenizer strTok;
		Curve2			c;
		
		strTok	= new StringTokenizer( s, ";" );
		c		= new Curve2( ParamSpace.valueOf( strTok.nextToken() ),		// hSpace
							 ParamSpace.valueOf( strTok.nextToken() ),		// vSpace
							 Integer.parseInt( strTok.nextToken() ));		// type
		
		while( strTok.hasMoreElements() ) {
			c.points.addElement( DoublePoint.valueOf( strTok.nextToken() ));
		}

		return c;
	}*/
}
// class Curve2
