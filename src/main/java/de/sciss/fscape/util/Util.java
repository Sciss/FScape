/*
 *  Util.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		17-Jun-07	extended
 */

package de.sciss.fscape.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Util
{
	/**
	 *	Checks if a certain int value is an element of a
	 *	specific int array.
	 *
	 *	@param	val		value to seek for in array
	 *	@param	array	may be any size, may not be null
	 *	@return	true, if value is an element of array
	 */
	public static boolean isValueInArray( int val, int[] array )
	{
		for( int i = 0; i < array.length; i++ ) {
			if( array[ i ] == val ) return true;
		}
		return false;
	}

	/**
	 *	Checks if a certain String value is an element of a
	 *	specific String array.
	 *
	 *	@param	val		value to seek for in array
	 *	@param	array	may be any size, may not be null
	 *	@return	true, if value is an element of array
	 */
	public static boolean isValueInArray( String val, String[] array )
	{
		for( int i = 0; i < array.length; i++ ) {
			if( array[ i ].equals( val )) return true;
		}
		return false;
	}

	/**
	 *	Zirkulaere Buffer-Rotation
	 *
	 *	@param	a		zu rotierender puffer
	 *	@param	len		von a
	 *	@param	buf		zwischenpuffer; laenge muss mind. abs( shift ) sein!
	 *					darf null sein
	 *	@param	shift	bewegungsumfang
	 */
	public static void rotate( float[] a, int len, float[] buf, int shift )
	{
		if( buf == null ) {
			buf = new float[ shift ];
		}
	
		if( shift < 0 ) {
		
			shift = -shift;
			System.arraycopy( a, 0, buf, 0, shift );
			System.arraycopy( a, shift, a, 0, len - shift );
			System.arraycopy( buf, 0, a, len - shift, shift );
		
		} else if( shift > 0 ) {

			System.arraycopy( a, len - shift, buf, 0, shift );
			System.arraycopy( a, 0, a, shift, len - shift );
			System.arraycopy( buf, 0, a, 0, shift );
		}
	}

	/**
	 *	Polynom-Interpolation (oder Extrapolation)
	 *
	 *	@param	xa	bekannte zu benutzende Stellen
	 *	@param	ya	Werte zu xa
	 *	@param	n	Interpolationsgrad
	 *	@param	x	Stelle, die berechnet werden soll
	 *	@return		interpolierter Wert
	 *
	 *	taken + adapted from Numerical Recipes in C
	 */
	public static float polyInterpolate( float xa[], float ya[], int n, float x )
	{
		int		i, m, ns;
		float	y;
		float	den, minDif, dif, ho, hp, w, rescue;
		float[]	c, d;

		minDif	= Math.abs( x - xa[ 1 ]);
		c		= new float[ n ];
		d		= new float[ n ];

		// Here we find the index ns of the closest table entry,
		for( i = 0, ns = 0; i < n; i++)  {	
			dif = Math.abs( x - xa[ i ]);
			if( dif < minDif) {
				ns		= i;
				minDif	= dif;
			}
			c[ i ] = ya[ i ];		// ...and initialize the tableau of c's and d's.
			d[ i ] = ya[ i ];
		}
		rescue	= ya[ ns ];
		y		= rescue;			// This is the initial approximation to y.

		// For each column of the tableau,
		for( m = 1; m < n; m++ ) {
			// ...we loop over the current c's and d's and update them.
			for( i = 0; i < (n - m); i++ ) {
				ho		= xa[ i ]   - x;
				hp		= xa[ i+m ] - x;
				w		= c[  i+1 ] - d[ i ];
				den		= ho - hp;
				// This error can occur only if two input xa's are (to within roundoff) identical.
				if( den == 0.0f ) return rescue;
				den		= w / den;
				d[ i ]	= hp * den;		// Here the c's and d's are updated.
				c[ i ]	= ho * den;
			}
			// After each column in the tableau is completed, we decide which correction, c or d,
			// we want to add to our accumulating value of y, i.e., which path to take through the
			// tableau|forking up or down. We do this in such a way as to take the most \straight
			// line" route through the tableau to its apex, updating ns accordingly to keep track of
			// where we are. This route keeps the partial approximations centered (insofar as possible)
			// on the target x. Thelastdy added is thus the error indication.
			y += ((ns << 1) < (n - m)) ? c[ ns ] : d [ --ns ];
		}
		
		return y;
	}
	
	/**
	 *	Eindimensionales Array loeschen
	 */
	public static void clear( float[] a )
	{
		for( int i = 0; i < a.length; i++ ) {
			a[ i ] = 0.0f;
		}
	}

	/** Fills a two-dimensional array with a constant value. */
	public static void fill(float[][] a, int off, int len, float value) {
		final int stop = off + len;
		for (float[] ch : a) {
			for (int j = off; j < stop; j++) {
				ch[j] = value;
			}
		}
	}

	/** Fills a two-dimensional array with a constant value. */
	public static void fill(double[][] a, int off, int len, double value) {
		final int stop = off + len;
		for (double[] ch : a) {
			for (int j = off; j < stop; j++) {
				ch[j] = value;
			}
		}
	}

	/**
	 *	Eindimensionales Array loeschen
	 */
	public static void clear( double[] a )
	{
		for( int i = 0; i < a.length; i++ ) {
			a[ i ] = 0.0;
		}
	}

	/**
	 *	Eindimensionales Array zu einem zweiten addieren
	 */
	public static void add( float[] src, int srcOff, float[] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
	
		while( srcOff < stop ) {
			dest[ destOff++ ] += src[ srcOff++ ];
		}
	}

	/**
	 *	Zweidimensionales Array zu einem zweiten addieren
	 */
	public static void add( float[][] src, int srcOff, float[][] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
		float[] b, c;
		
		for( int i = 0; i < src.length; i++ ) {
			b = src[ i ];
			c = dest[ i ];
			for( int j = srcOff, k = destOff; j < stop; j++, k++ ) {
				c[ k ] += b[ j ];
			}
		}
	}

	/**
	 *	Konstante in eindimensionales Array addieren
	 */
	public static void add( float[] a, int off, int length, float val )
	{
		final int stop = off + length;
	
		while( off < stop ) a[ off++ ] += val;
	}

	/**
	 *	Konstante in zweidimensionales Array addieren
	 */
	public static void add( float[][] a, int off, int length, float val )
	{
		final int stop = off + length;
		float[] b;
	
		for( int i = 0; i < a.length; i++ ) {
			b = a[ i ];
			for( int j = off; j < stop; j++ ) {
				b[ j ] += val;
			}
		}
	}

	/**
	 *	Eindimensionales Array von einem zweiten subtrahieren
	 */
	public static void sub( float[] src, int srcOff, float[] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
	
		while( srcOff < stop ) {
			dest[ destOff++ ] -= src[ srcOff++ ];
		}
	}

	/**
	 *	Eindimensionales Array in eine zweites multiplizieren
	 */
	public static void mult( float[] src, int srcOff, float[] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
	
		while( srcOff < stop ) {
			dest[ destOff++ ] *= src[ srcOff++ ];
		}
	}

	/**
	 *	Eindimensionales Array in ein zweites zweidimensionales multiplizieren
	 */
	public static void mult( float[] src, int srcOff, float[][] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
		float[] b;
		
		for( int i = 0; i < dest.length; i++ ) {
			b = dest[ i ];
			for( int j = srcOff, k = destOff; j < stop; j++, k++ ) {
				b[ k ] *= src[ j ];
			}
		}
	}

	/**
	 *	Zweidimensionales Array in ein zweites multiplizieren
	 */
	public static void mult( float[][] src, int srcOff, float[][] dest, int destOff, int length )
	{
		final int stop = srcOff + length;
		float[] b, c;
		
		for( int i = 0; i < dest.length; i++ ) {
			b = dest[ i ];
			c = src[ i ];
			for( int j = srcOff, k = destOff; j < stop; j++, k++ ) {
				b[ k ] *= c[ j ];
			}
		}
	}

	/**
	 *	Eindimensionales Array mit skalar multiplizieren
	 */
	public static void mult( float[] a, int off, int length, float gain )
	{
		final int stop = off + length;
	
		while( off < stop ) {
			a[ off++ ] *= gain;
		}
	}

	/**
	 *	Zweidimensionales Array mit skalar multiplizieren
	 */
	public static void mult( float[][] a, int off, int length, float gain )
	{
		final int stop = off + length;
		float[] b;
	
		for( int i = 0; i < a.length; i++ ) {
			b = a[ i ];
			for( int j = off; j < stop; j++ ) {
				b[ j ] *= gain;
			}
		}
	}

	/**
	 *	Zweidimensionales Array loeschen
	 */
	public static void clear( float[][] a )
	{
		float[] b;
	
		for( int i = 0; i < a.length; i++ ) {
			b = a[ i ];
			for( int j = 0; j < b.length; j++ ) {
				b[ j ] = 0.0f;
			}
		}
	}
	
    /** Clear two dimensional array. */
    public static void clear(double[][] a) {
        double[] b;

        for (int i = 0; i < a.length; i++) {
            b = a[i];
            for (int j = 0; j < b.length; j++) {
                b[j] = 0.0;
            }
        }
    }

    /**
	 *	Zweidimensionales Array loeschen
	 */
	public static void clear( float[][] a, int off, int length )
	{
		final int stop = off + length;
		float[] b;

		for( int i = 0; i < a.length; i++ ) {
			b = a[ i ];
			for( int j = off; j < stop; j++ ) {
				b[ j ] = 0.0f;
			}
		}
	}

    /** Clears two dimensional array. */
    public static void clear(double[][] a, int off, int length) {
        final int stop = off + length;
        double[] b;

        for (int i = 0; i < a.length; i++) {
            b = a[i];
            for (int j = off; j < stop; j++) {
                b[j] = 0.0;
            }
        }
    }

    /** Clears one dimensioal array. */
    public static void clear(double[] a, int off, int length) {
        final int stop = off + length;

        for (int j = off; j < stop; j++) {
            a[j] = 0.0;
        }
    }

    /**
	 *	Eindimensionales Array loeschen
	 */
	public static void clear( float[] a, int off, int length )
	{
		final int stop = off + length;

		for( int j = off; j < stop; j++ ) {
			a[ j ] = 0.0f;
		}
	}

	/**
	 *	Eindimensionales Array umkehren
	 */
	public static void reverse( float[] a, int off, int length )
	{
		float tmp;
		
		for( int i = off, j = off + length - 1; i < j; i++, j-- ) {
			tmp = a[ i ];
			a[ i ] = a[ j ];
			a[ j ] = tmp;
		}
	}

	/**
	 *	Zweidimensionales Array umkehren
	 */
	public static void reverse( float[][] a, int off, int length )
	{
		final int last = off + length - 1;
		float tmp;
		float[] b;
		
		for( int k = 0; k < a.length; k++ ) {
			b = a[ k ];
			for( int i = off, j = last; i < j; i++, j-- ) {
				tmp = b[ i ];
				b[ i ] = b[ j ];
				b[ j ] = tmp;
			}
		}
	}
	
	public static void reverse( double[][] a, int off, int length )
	{
		final int last = off + length - 1;
		double tmp;
		double[] b;
		
		for( int k = 0; k < a.length; k++ ) {
			b = a[ k ];
			for( int i = off, j = last; i < j; i++, j-- ) {
				tmp = b[ i ];
				b[ i ] = b[ j ];
				b[ j ] = tmp;
			}
		}
	}
	
	public static float sum( float[] a, int off, int length )
	{
		double d = 0.0;
		final int stop = off + length;
		while( off < stop ) d += a[ off++ ];
		return (float) d;
	}
	
	public static float meanSquare( float[] a, int off, int length )
	{
		double d = 0.0, d1;
		final int stop = off + length;
		while( off < stop ) {
			d1 = a[ off++ ];
			d += d1 * d1;
		}
		return (float) (d / length);
	}
	
	public static float mean( float[] a, int off, int length )
	{
		return sum( a, off, length ) / length;
	}
	
	public static float maxAbs( float[] a, int off, int length )
	{
		final int stop = off + length;
		float f1 = 0f, f2;
		while( off < stop ) {
			f2 = Math.abs( a[ off++ ]);
			if( f2 > f1 ) f1 = f2;
		}
		return f1;
	}
	
	public static float maxAbs( float[][] a, int off, int length )
	{
		final int stop = off + length;
		float f1 = 0f, f2;
		float[] b;
		
		for( int i = 0; i < a.length; i++ ) {
			b = a[ i ];
			for( int j = off; j < stop; j++ ) {
				f2 = Math.abs( b[ j ]);
				if( f2 > f1 ) f1 = f2;
			}
		}
		return f1;
	}
	
	public static float removeDC( float[] a, int off, int length )
	{
		if( length == 0 ) return 0f;
		
		final float sum = sum( a, off, length );
		final float dc  = sum / length;
		if( dc != 0f ) {
			add( a, off, length, -dc );
		}
		return dc;
	}
	
	public static void copy( float[][] src, int srcOff, float[][] dest, int destOff, int length )
	{
		for( int i = 0; i < src.length; i++ ) {
			System.arraycopy( src[ i ], srcOff, dest[ i ], destOff, length );
		}
	}
	
	public static int nextPowerOfTwo( int x )
	{
		int y = 1;
		while( y < x ) y <<= 1;
		return y;
	}

	/**
	 *	Creates a list from an iterator
	 *
	 *	@param	iter	an iterator whose contents should be returned as an array
	 *	@return	the iterator elements as an array
	 */
	public static List iterToList( Iterator iter )
	{
		final List foo = new ArrayList();
		while( iter.hasNext() ) {
			foo.add( iter.next() );
		}
		return foo;
	}
	
	/**
	 *	Creates an array from an iterator
	 *
	 *	@param	iter	an iterator whose contents should be returned as an array
	 *	@return	the iterator elements as an array
	 */
	public static Object[] iterToArray( Iterator iter )
	{
		return iterToList( iter ).toArray();
	}

	/**
	 *	Erzeugt Array aus einer Enumeration
	 */
//	public static Object[] enumToArray( Enumeration enum )
//	{
//		Vector foo = new Vector();
//		while( enum.hasMoreElements() ) {
//			foo.addElement( enum.nextElement() );
//		}
//		Object[] array = new Object[ foo.size() ];
//		for( int i = 0; i < foo.size(); i++ ) {
//			array[ i ] = foo.elementAt( i );
//		}
//		return array;
//	}

	public static double linexp( double x, double inLo, double inHi, double outLo, double outHi )
	{
		return Math.pow( outHi / outLo, (x - inLo) / (inHi - inLo) ) * outLo;
	}
	
	public static double explin( double x, double inLo, double inHi, double outLo, double outHi )
	{
		return Math.log( x / inLo ) / Math.log( inHi / inLo ) * (outHi - outLo) + outLo;
	}

	/*
	 *	Sorts an array data[0...len-1] into ascending order using Quicksort, while making the corresponding
	 *	rearrangement of the array dataIdx[0...len-1].
	 */
	public static void quickSortFloat( float[] data, int[] dataIdx, int len )
	{
		int		scanGt, scanLt, iterLen = len, k, l = 1;
		int[]	stack = new int[ 65 ];			// >= 2 * ld N
		int		stackIdx = 0;
		float	dataElem, tempF;
		int		dataIdxElem, tempI;
		int		kDec, lDec = 0, iterLenDec = iterLen - 1;

		while( true ) { // Insertion sort when subarray small enough.

			if( iterLen - l < 7 ) {		// M = 7
				for( scanGt = l; scanGt < iterLen; scanGt++ ) {
					dataElem				= data[scanGt];
					dataIdxElem				= dataIdx[scanGt];
					for( scanLt = scanGt-1; scanLt+1 >= l; scanLt-- ) {
						if( data[scanLt] <= dataElem ) break;
						data[scanLt+1]		= data[scanLt];
						dataIdx[scanLt+1]	= dataIdx[scanLt];
					}
					data[scanLt+1]			= dataElem;
					dataIdx[scanLt+1]		= dataIdxElem;
				}
				if( stackIdx == 0 ) {
					// free_lvector(stack,1,NSTACK);
					return;
				}
				iterLen		= stack[stackIdx]; 	// Pop stack and begin a new round of partitioning.
				iterLenDec	= iterLen-1;
				l			= stack[stackIdx-1];
				lDec		= l-1;
				stackIdx   -= 2;

			} else {

				// Choose median of left, center and right elements
				// as partitioning element dataElem. Also
				// rearrange so that dataElem[l] <= dataElem[l+1] <= dataElem[iterLen].
				k				= (l+iterLen) >> 1;
				kDec			= k-1;

				tempF			= data[kDec];				// SWAP(data[k],data[l+1])
				data[kDec]		= data[l];
				data[l]			= tempF;

				tempI			= dataIdx[kDec];			// SWAP(dataIdx[k],dataIdx[l+1])
				dataIdx[kDec]	= dataIdx[l];
				dataIdx[l]		= tempI;
				
				if( data[lDec] > data[iterLenDec] ) {

					tempF				= data[lDec];		// SWAP(data[l],data[iterLen])
					data[lDec]			= data[iterLenDec];
					data[iterLenDec]	= tempF;
	
					tempI				= dataIdx[lDec];	// SWAP(dataIdx[l],dataIdx[iterLen])
					dataIdx[lDec]		= dataIdx[iterLenDec];
					dataIdx[iterLenDec]	= tempI;
				}
				
				if( data[l] > data[iterLenDec] ) {
				
					tempF				= data[l];			// SWAP(data[l+1],data[iterLen])
					data[l]				= data[iterLenDec];
					data[iterLenDec]	= tempF;

					tempI				= dataIdx[l];		// SWAP(dataIdx[l+1],dataIdx[iterLen])
					dataIdx[l]			= dataIdx[iterLenDec];
					dataIdx[iterLenDec]	= tempI;
				}
				
				if( data[lDec] > data[l] ) {

					tempF				= data[lDec];		// SWAP(data[l],data[l+1])
					data[lDec]			= data[l];
					data[l]				= tempF;
				
					tempI				= dataIdx[lDec];	// SWAP(dataIdx[l],dataIdx[l+1])
					dataIdx[lDec]		= dataIdx[l];
					dataIdx[l]			= tempI;
				}
				
				scanGt			= l;						// Initialize pointers for partitioning.
				scanLt			= iterLenDec;
				dataElem		= data[l];					// Partitioning element.
				dataIdxElem		= dataIdx[l];

				while( true ) { 	// Beginning of innermost loop.
					do scanGt++; while( data[scanGt] < dataElem ); // Scan up to find element > dataElem.
					do scanLt--; while( data[scanLt] > dataElem ); // Scan down to find element < dataElem.

					if( scanLt < scanGt ) break;			// Pointers crossed. Partitioning complete.

					tempF				= data[scanGt];		// SWAP(data[scanGt],data[scanLt])
					data[scanGt]		= data[scanLt];
					data[scanLt]		= tempF;
				
					tempI				= dataIdx[scanGt];	// SWAP(dataIdx[scanGt],dataIdx[scanLt])
					dataIdx[scanGt]		= dataIdx[scanLt];
					dataIdx[scanLt]		= tempI;

				} // End of innermost loop.

				data[l]				= data[scanLt]; 		// Insert partitioning element in both arrays.
				data[scanLt]		= dataElem;
				dataIdx[l]			= dataIdx[scanLt];
				dataIdx[scanLt]		= dataIdxElem;
				stackIdx		   += 2;
				
				scanGt++;
				
				// Push pointers to larger subarray on stack, process smaller subarray immediately.
			//	if( stackIdx > NSTACK ) nrerror("NSTACK too small in sort2.");
				if( iterLen-scanGt >= scanLt-l ) {
				
					stack[stackIdx]		= iterLen;
					stack[stackIdx-1]	= scanGt;
					iterLen				= scanLt;
					iterLenDec			= iterLen-1;
					
				} else {
					stack[stackIdx]		= scanLt;
					stack[stackIdx-1]	= l;
					l					= scanGt;
					lDec				= l-1;
				}
			}
		}
	}

//	/**
//	 *	Lazily sorts the data. Throws a
//	 *	<code>RuntimeException</code> if any object
//	 *	in the array is not <code>Comparable</code>.
//	 *	
//	 *	@param	data		array of objects implementing the
//	 *						<code>Comparable</code> interface
//	 *	@param	ascending	<code>true</code> to sort in ascending order,
//	 *						<code>false</code> to sort in descending order
//	 *					
//	 *	@see	java.lang.Comparable
//	 */
//	public static void sort( Object[] data, boolean ascending )
//	{
//		Object		a, b;
//		final int	mult	= ascending ? 1 : -1;
//		int			i, restart = 0;
//	
//		do {
//			for( i = restart, restart = data.length; i < data.length - 1; i++ ) {
//				a		= data[ i ];
//				b		= data[ i + 1 ];
//				if( ((Comparable) a).compareTo( b ) * mult > 0 ) {
//					data[ i+1 ] = a;
//					data[ i ]	= b;
//					restart		= Math.min( restart, Math.max( 0, i - 1 ));
//				}
//			}
//		} while( restart < data.length );
//	}

	/*
	 *	Sorts an array data[0...len-1] into ascending order using Quicksort
	 *	rearrangement of the array dataIdx[0...len-1].
	 */
	public static void quickSortInt( int[] data, int[] dataIdx, int len )
	{
		int		scanGt, scanLt, iterLen = len, k, l = 1;
		int[]	stack = new int[ 65 ];			// >= 2 * ld N
		int		stackIdx = 0;
		int		dataElem, dataIdxElem, temp;
		int		kDec, lDec = 0, iterLenDec = iterLen - 1;

		while( true ) { // Insertion sort when subarray small enough.
			if( iterLen - l < 7 ) {		// M = 7
				for( scanGt = l; scanGt < iterLen; scanGt++ ) {
					dataElem				= data[scanGt];
					dataIdxElem				= dataIdx[scanGt];
					for( scanLt = scanGt-1; scanLt+1 >= l; scanLt-- ) {
						if( data[scanLt] <= dataElem ) break;
						data[scanLt+1]		= data[scanLt];
						dataIdx[scanLt+1]	= dataIdx[scanLt];
					}
					data[scanLt+1]			= dataElem;
					dataIdx[scanLt+1]		= dataIdxElem;
				}
				if( stackIdx == 0 ) {
					// free_lvector(stack,1,NSTACK);
					return;
				}
				iterLen		= stack[stackIdx]; 	// Pop stack and begin a new round of partitioning.
				iterLenDec	= iterLen-1;
				l			= stack[stackIdx-1];
				lDec		= l-1;
				stackIdx   -= 2;

			} else {

				// Choose median of left, center and right elements
				// as partitioning element dataElem. Also
				// rearrange so that dataElem[l] <= dataElem[l+1] <= dataElem[iterLen].
				k				= (l+iterLen) >> 1;
				kDec			= k-1;

				temp			= data[kDec];				// SWAP(data[k],data[l+1])
				data[kDec]		= data[l];
				data[l]			= temp;

				temp			= dataIdx[kDec];			// SWAP(dataIdx[k],dataIdx[l+1])
				dataIdx[kDec]	= dataIdx[l];
				dataIdx[l]		= temp;
				
				if( data[lDec] > data[iterLenDec] ) {

					temp				= data[lDec];		// SWAP(data[l],data[iterLen])
					data[lDec]			= data[iterLenDec];
					data[iterLenDec]	= temp;
	
					temp				= dataIdx[lDec];	// SWAP(dataIdx[l],dataIdx[iterLen])
					dataIdx[lDec]		= dataIdx[iterLenDec];
					dataIdx[iterLenDec]	= temp;
				}
				
				if( data[l] > data[iterLenDec] ) {
				
					temp				= data[l];			// SWAP(data[l+1],data[iterLen])
					data[l]				= data[iterLenDec];
					data[iterLenDec]	= temp;

					temp				= dataIdx[l];		// SWAP(dataIdx[l+1],dataIdx[iterLen])
					dataIdx[l]			= dataIdx[iterLenDec];
					dataIdx[iterLenDec]	= temp;
				}
				
				if( data[lDec] > data[l] ) {

					temp				= data[lDec];		// SWAP(data[l],data[l+1])
					data[lDec]			= data[l];
					data[l]				= temp;
				
					temp				= dataIdx[lDec];	// SWAP(dataIdx[l],dataIdx[l+1])
					dataIdx[lDec]		= dataIdx[l];
					dataIdx[l]			= temp;
				}
				
				scanGt			= l;						// Initialize pointers for partitioning.
				scanLt			= iterLenDec;
				dataElem		= data[l];					// Partitioning element.
				dataIdxElem		= dataIdx[l];

				while( true ) { 	// Beginning of innermost loop.
					do scanGt++; while( data[scanGt] < dataElem ); // Scan up to find element > dataElem.
					do scanLt--; while( data[scanLt] > dataElem ); // Scan down to find element < dataElem.

					if( scanLt < scanGt ) break;			// Pointers crossed. Partitioning complete.

					temp				= data[scanGt];		// SWAP(data[scanGt],data[scanLt])
					data[scanGt]		= data[scanLt];
					data[scanLt]		= temp;
				
					temp				= dataIdx[scanGt];	// SWAP(dataIdx[scanGt],dataIdx[scanLt])
					dataIdx[scanGt]		= dataIdx[scanLt];
					dataIdx[scanLt]		= temp;

				} // End of innermost loop.

				data[l]				= data[scanLt]; 		// Insert partitioning element in both arrays.
				data[scanLt]		= dataElem;
				dataIdx[l]			= dataIdx[scanLt];
				dataIdx[scanLt]		= dataIdxElem;
				stackIdx		   += 2;
				
				scanGt++;
				
				// Push pointers to larger subarray on stack, process smaller subarray immediately.
			//	if( stackIdx > NSTACK ) nrerror("NSTACK too small in sort2.");
				if( iterLen-scanGt >= scanLt-l ) {
				
					stack[stackIdx]		= iterLen;
					stack[stackIdx-1]	= scanGt;
					iterLen				= scanLt;
					iterLenDec			= iterLen-1;
					
				} else {
					stack[stackIdx]		= scanLt;
					stack[stackIdx-1]	= l;
					l					= scanGt;
					lDec				= l-1;
				}
			}
		}
	}

	/*
	 * 	The below sort algorithm is a modification of the Arrays.sort
	 * 	method of OpenJDK 7 which is (C)opyright 1997-2007 by Sun
	 * 	Microsystems, Inc., and covered by the GNU General Public
	 * 	License v2.
	 * 	
	 *	The modification made is the additional corr argument which
	 *	is an arbitrary array that gets sorted along with the x vector. 	
	 */

	/*
	 * Sorts the specified sub-array of doubles into ascending order.
	 */
	public static void sort( double x[], Object[] corr, int off, int len )
	{
		// Insertion sort on smallest arrays
		if( len < 7 ) {
			for( int i = off; i < len + off; i++ ) {
				for( int j = i; (j > off) && (x[ j - 1 ] > x[ j ]); j-- ) {
					swap( x, corr, j, j - 1 );
				}
			}
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if( len > 7 ) {
			int l = off;
			int n = off + len - 1;
			if( len > 40 ) { // Big arrays, pseudomedian of 9
				final int s = len / 8;
				l = med3( x, l, l + s, l + 2 * s );
				m = med3( x, m - s, m, m + s );
				n = med3( x, n - 2 * s, n - s, n );
			}
			m = med3( x, l, m, n ); // Mid-size, med of 3
		}
		final double v = x[ m ];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while( true ) {
			while( (b <= c) && (x[ b ] <= v) ) {
				if( x[ b ] == v ) swap( x, corr, a++, b );
				b++;
			}
			while( c >= b && x[ c ] >= v ) {
				if( x[ c ] == v ) swap( x, corr, c, d-- );
				c--;
			}
			if( b > c ) break;
			swap( x, corr, b++, c-- );
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min( a - off, b - a );
		vecswap( x, corr, off, b - s, s );
		s = Math.min( d - c, n - d - 1 );
		vecswap( x, corr, b, n - s, s );

		// Recursively sort non-partition-elements
		if( (s = b - a) > 1 ) sort( x, corr, off, s );
		if( (s = d - c) > 1 ) sort( x, corr, n - s, s );
	}

	/*
	 * Swaps x[a] with x[b].
	 */
	private static void swap( double x[], Object[] corr, int a, int b )
	{
		final double tmpX = x[ a ];
		x[ a ] = x[ b ];
		x[ b ] = tmpX;
		final Object tmpCorr = corr[ a ];
		corr[ a ] = corr[ b ];
		corr[ b ] = tmpCorr;
	}

	/*
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap( double x[], Object[] corr, int a, int b, int n )
	{
		for( int i = 0; i < n; i++, a++, b++ ) {
			swap( x, corr, a, b );
		}
	}

	/**
	 * Returns the index of the median of the three indexed doubles.
	 */
	private static int med3( double x[], int a, int b, int c )
	{
		return(x[ a ] < x[ b ] ? (x[ b ] < x[ c ] ? b : x[ a ] < x[ c ] ? c : a) : (x[ b ] > x[ c ] ? b
		: x[ a ] > x[ c ] ? c : a));
	}
}
// class Util
