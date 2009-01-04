/*
 *  Util.java
 *  FScape
 *
 *  Copyright (c) 2001-2009 Hanns Holger Rutz. All rights reserved.
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
 *		17-Jun-07	extended
 */

package de.sciss.fscape.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 17-Jun-07
 */
public class Util
{
	/**
	 *	Check if a certain int value is an element of a
	 *	specific int array
	 *
	 *	@param	val		value to seek for in array
	 *	@param	array	may be any size, may not be null
	 *	@return	true, if val is an element of array
	 */
	public static boolean isValueInArray( int val, int[] array )
	{
		for( int i = 0; i < array.length; i++ ) {
			if( array[ i ] == val ) return true;
		}
		return false;
	}

	/**
	 *	Check if a certain String value is an element of a
	 *	specific String array
	 *
	 *	@param	val		value to seek for in array
	 *	@param	array	may be any size, may not be null
	 *	@return	true, if val is an element of array
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
	
	public static float sum( float[] a, int off, int length )
	{
		double d = 0.0;
		final int stop = off + length;
		while( off < stop ) d += a[ off++ ];
		return (float) d;
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
	 *	Creates an array from an iterator
	 *
	 *	@param	iter	an iterator whose contents should be returned as an array
	 *	@return	the iterator elements as an array
	 */
	public static Object[] iterToArray( Iterator iter )
	{
		final List foo = new ArrayList();
		while( iter.hasNext() ) {
			foo.add( iter.next() );
		}
		return foo.toArray();
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

	/**
	 *	Lazily sorts the data. Throws a
	 *	<code>RuntimeException</code> if any object
	 *	in the array is not <code>Comparable</code>.
	 *	
	 *	@param	data		array of objects implementing the
	 *						<code>Comparable</code> interface
	 *	@param	ascending	<code>true</code> to sort in ascending order,
	 *						<code>false</code> to sort in descending order
	 *					
	 *	@see	java.lang.Comparable
	 */
	public static void sort( Object[] data, boolean ascending )
	{
		Object		a, b;
		final int	mult	= ascending ? 1 : -1;
		int			i, restart = 0;
	
		do {
			for( i = restart, restart = data.length; i < data.length - 1; i++ ) {
				a		= data[ i ];
				b		= data[ i + 1 ];
				if( ((Comparable) a).compareTo( b ) * mult > 0 ) {
					data[ i+1 ] = a;
					data[ i ]	= b;
					restart		= Math.min( restart, Math.max( 0, i - 1 ));
				}
			}
		} while( restart < data.length );
	}

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
}
// class Util
