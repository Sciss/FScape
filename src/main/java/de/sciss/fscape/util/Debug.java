/*
 *  Debug.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.util;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;

import de.sciss.fscape.gui.Axis;
import de.sciss.fscape.gui.VectorDisplay;
import de.sciss.gui.VectorSpace;

/**
 *	@version	0.72, 21-Jan-09
 *	@author		Hanns Holger Rutz
 */
public class Debug
{
	public static void view( float[] data, int off, int length, String descr )
	{
		view( data, off, length, descr, false, true );
	}
	
	/**
	 *	Open window to view 1D float data
	 */
	public static void view( float[] data, int off, int length, String descr,
							 boolean centerZero, boolean decimate )
	{
		final float[] dataCopy = new float[ length ];
		
		System.arraycopy( data, off, dataCopy, 0, length );
		
		int width = 256;
		int decimF = decimate ? Math.max( 1, 2 * length / width ) : 1;
		int decimLen = length / decimF;
		
		final float[] decim = new float[ decimLen ];
		float f1, f2, f3;
		
		f2 = Float.NEGATIVE_INFINITY;
		f3 = Float.POSITIVE_INFINITY;
		for( int i = 0, j = 0; i < decimLen; ) {
			f1 = dataCopy[ j++ ];
			for( int k = 1; k < decimF; k++ ) {
				f1 = Math.max( f1, dataCopy[ j++ ]);
			}
			decim[ i++ ] = f1;
			f2 = Math.max( f2, f1 );
			f3 = Math.min( f3, f1 );
		}
		if( Float.isInfinite( f2 )) f2 = 1f;
		if( Float.isInfinite( f3 )) f3 = 0f;
		
		final VectorDisplay ggVectorDisplay = new VectorDisplay( decim );
		final Axis			haxis	= new Axis( Axis.HORIZONTAL );
		final Axis			vaxis	= new Axis( Axis.VERTICAL );
		
		if( centerZero ) {
			f2 = Math.max( Math.abs( f2 ), Math.abs( f3 ));
			f3 = -f2;
		}

		ggVectorDisplay.setMinMax( f3, f2 );
//		ggVectorDisplay.addMouseListener( mia );
//		ggVectorDisplay.addMouseMotionListener( mia );
//		ggVectorDisplay.addTopPainter( tp );
//		ggVectorDisplay.setPreferredSize( new Dimension( width, 256 )); // XXX
		final JPanel		displayPane = new JPanel( new BorderLayout() );
		displayPane.add( ggVectorDisplay, BorderLayout.CENTER );
		final VectorSpace	spc		= VectorSpace.createLinSpace( off, off + length, f3, f2, null, null, null, null );
		haxis.setSpace( spc );
		vaxis.setSpace( spc );
		final Box			box		= Box.createHorizontalBox();
		box.add( Box.createHorizontalStrut( vaxis.getPreferredSize().width ));
		box.add( haxis );
		displayPane.add( box, BorderLayout.NORTH );
		displayPane.add( vaxis, BorderLayout.WEST );
		
		final JFrame f = new JFrame( descr );
		f.setSize( width, 256 );
		f.getContentPane().add( displayPane, BorderLayout.CENTER );
		f.setVisible( true );
	}
}
// class Debug
