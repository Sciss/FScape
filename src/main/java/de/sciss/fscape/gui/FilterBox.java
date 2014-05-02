/*
 *  FilterBox.java
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
 *		14-Mar-05	added roll-off (raised cosine modulation) ; defaults to
 *					zero for backward compatibility
 *		XXX XXX THAT'S IN PROGRESS, ROLL-OFF IS NOT SAVED!
 *		XXX XXX WORKS ONLY FOR LOWPASS + HIGHPASS NOW
 */
// WARNING: changes affect both FIRDesignerDlg and HilbertDlg!!!!

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import de.sciss.fscape.util.*;

import de.sciss.io.AudioFileDescr;

/**
 *  Strange hybrid mixture of
 *	a filterbox GUI representation
 *	in the FIR designer and the
 *	actual FIR coefficient calculation.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 12-May-08
 *
 *	@todo	windowing should use full windows instead of half wings
 *	@todo	graphical hint for rolloff other than zero
 */
public class FilterBox
implements CircuitPanel.Box, javax.swing.Icon
{
// -------- public Variablen --------

	public static final int FLT_ALLPASS			= 0;	// filterType
	public static final int FLT_LOWPASS			= 1;
	public static final int FLT_HIGHPASS		= 2;
	public static final int FLT_BANDPASS		= 3;
	public static final int FLT_BANDSTOP		= 4;
	public static final int FLTNUM				= 5;

	public int		filterType	= FLT_ALLPASS;
	public boolean	sign		= false;
	public Param	cutOff;
	public Param	rollOff;
	public Param	bandwidth;
	public Param	gain;
	public Param	delay;
	public boolean	overtones	= false;
	public Param	otLimit;
	public Param	otSpacing;

	// graphical representation
	private static final Stroke	strkLine	= new BasicStroke( 0.5f );
	private static final Paint	pntBack		= new Color( 0xFF, 0xFF, 0xFF, 0x7F );
	private static final Paint	pntArea		= new Color( 0x42, 0x5E, 0x9D, 0x7F );
	private static final Paint	pntLine		= Color.black;

// -------- private Variablen --------

	private static final int ICON_WIDTH		= 32;
	private static final int ICON_HEIGHT	= 32;

	private static final int[][]	iconPolyX	= {
		{ 0, 31, 31, 0 },			// byp
		{ 0, 10, 20, 31, 0 },		// lpf
		{ 0, 10, 20, 31, 31 },		// hpf
		{ 0, 6, 12, 18, 24, 31 },	// bpf
		{ 0, 6, 12, 18, 24, 31, 31, 0 }	// bsf
	};
	private static final int[][]	iconPolyY	= {
		{ 8, 8, 31, 31 },			// byp
		{ 8, 8, 31, 31, 31 },		// lpf
		{ 31, 31, 8, 8, 31 },		// hpf
		{ 31, 31, 8, 8, 31, 31 },	// bpf
		{ 8, 8, 31, 31, 8, 8, 31, 31  }	// bsf
	};
	private static final int[]	iconLineNum	= {
		2, 4, 4, 6, 6, 6, 6
	};

// -------- public Methoden --------

	public FilterBox()
	{
		super();

		cutOff		= new Param( 1000.0, Param.ABS_HZ );
		rollOff		= new Param(    0.0, Param.OFFSET_HZ );
		bandwidth	= new Param(  250.0, Param.OFFSET_HZ );
		gain		= new Param(    0.0, Param.DECIBEL_AMP );
		delay		= new Param(    0.0, Param.ABS_MS );
		otLimit		= new Param( 5000.0, Param.ABS_HZ );
		otSpacing	= new Param( 1000.0, Param.OFFSET_HZ );
	}

	public FilterBox( FilterBox proto )
	{
		super();

		cutOff		= (Param) proto.cutOff.clone();
		rollOff		= (Param) proto.rollOff.clone();
		bandwidth	= (Param) proto.bandwidth.clone();
		gain		= (Param) proto.gain.clone();
		delay		= (Param) proto.delay.clone();
		otLimit		= (Param) proto.otLimit.clone();
		otSpacing	= (Param) proto.otSpacing.clone();
		filterType	= proto.filterType;
		sign		= proto.sign;
		overtones	= proto.overtones;
	}

	public Point calcLength( AudioFileDescr ref, int quality )
	{
		double		smpRate			= (double) ref.rate;
		double		smpPerPeriod;
		int			numPeriods		= 3;
		Point		len;
		int			delaySmp			= (int) (AudioFileDescr.millisToSamples( ref, 
											Param.transform( this.delay, Param.ABS_MS, null, null ).val ) + 0.5);
		int			halfWinSize;
		double[][]	freqs			= calcFrequencies( smpRate );
		double[]	sincFreqs		= freqs[ 0 ];
		double		minFreq			= smpRate / 2;

		if( sincFreqs.length == 0 ) {	// AllPass
			len = new Point( 0, 1 + delaySmp );
			return len;
		}
		
		for( int i = 0; i < sincFreqs.length; i++ ) {
			if( (sincFreqs[ i ] < minFreq) && (sincFreqs[ i ] > 0.0) ) {
				minFreq = sincFreqs[ i ];
			}
		}
		smpPerPeriod	= smpRate / minFreq;
			
		switch( quality ) {
		case FIRDesignerDlg.QUAL_MEDIUM:
			numPeriods	= 6;
			break;
		case FIRDesignerDlg.QUAL_GOOD:
			numPeriods	= 12;
			break;
		case FIRDesignerDlg.QUAL_VERYGOOD:
			numPeriods	= 24;
			break;
		}

		halfWinSize = (int) ((double) numPeriods * smpPerPeriod + 0.5);
		len			= new Point( halfWinSize, Math.max( 0, halfWinSize - 1 + delaySmp) );

		return len;
	}

	/**
	 *	@param	buf		will initially be completely cleared
	 */
	public void calcIR( AudioFileDescr ref, int quality, int winType, float[] buf, Point totalLength )
	{
		final double	smpRate			= (double) ref.rate;
		final double	freqNorm		= Constants.PI2 / smpRate;
		final double	cosineNorm		= 4.0 / (Math.PI*Math.PI);
		final int		dlySmp			= (int) (AudioFileDescr.millisToSamples( ref, 
											Param.transform( this.delay, Param.ABS_MS, null, null ).val ) + 0.5);
		final int		off				= totalLength.x + dlySmp;
		final Param		ampRef			= new Param( 1.0, Param.ABS_AMP );
		final double[][] freqs			= calcFrequencies( smpRate );
		final double[]	sincFreqs		= freqs[ 0 ];
		final double[]	cosineFreqs		= freqs[ 1 ];

		double			sincBase, cosineBase, impPower, coverage, d1, d2, d3;
		int				numPeriods		= 3;
		int				halfWinSize;
		double			factor			= 1.0;
		double			minFreq			= smpRate / 2;
		float[]			win;
		double			gainFactor		= Param.transform( this.gain, Param.ABS_AMP, ampRef, null ).val *
										  (sign ? -1 : 1);

		for( int i = 0; i < buf.length; i++ ) {		// clear buf
			buf[ i ] = 0.0f;
		}

		if( filterType == FLT_ALLPASS ) {			// -------- allpass --------
			buf[ off ] = (float) gainFactor;

		} else {									// -------- multiband --------

			switch( quality ) {
			case FIRDesignerDlg.QUAL_MEDIUM:
				numPeriods	= 6;
				break;
			case FIRDesignerDlg.QUAL_GOOD:
				numPeriods	= 12;
				break;
			case FIRDesignerDlg.QUAL_VERYGOOD:
				numPeriods	= 24;
				break;
			}

			for( int i = 0; i < sincFreqs.length; i++ ) {
				if( (sincFreqs[ i ] < minFreq) && (sincFreqs[ i ] > 0.0) ) {
					minFreq = sincFreqs[ i ];
				}
			}
			halfWinSize	= Math.max( 1, (int) ((double) numPeriods * smpRate / minFreq + 0.5) );

			// IR = factor * sinc(f1) - factor * sinc(f2) + factor * sinc(f3) etc.
			if( filterType == FLT_BANDPASS ) {
				factor = -1.0;
			}
	
			// calc the sincs
			coverage = 0.0;
			for( int i = 0; i < sincFreqs.length; i++, factor = -factor ) {
				if( sincFreqs[ i ] <= 0.0 ) continue;
				sincBase	= freqNorm * sincFreqs[ i ];
				coverage   += factor   * sincFreqs[ i ];
				cosineBase	= freqNorm * cosineFreqs[ i ];

// impPower	= 0.0;
				for( int j = 1; j < halfWinSize; j++ ) {
					// sinc-filter
					d1				= factor * (Math.sin( sincBase * j ) / (double) j);
					// raised cosine modulation
					d3				= cosineBase * j;
					d2				= cosineNorm * d3 * d3;
					d1			   *= (Math.cos( d3 ) / (1.0 - d2));
					buf[ off + j ] += (float) d1;
					buf[ off - j ] += (float) d1;
// impPower	   += d1;
				}
				buf[ off ] += (float) (factor * sincBase);	// cosine( 0 ) == 1.0
// impPower = 2 * impPower + (factor * sincBase);
// System.out.println( "@"+fc+" Hz: "+impPower );
			}
			coverage /= smpRate;		// 0...50% whereby 50% means full energy (no filtering)

			// create + apply window, normalize gain
			win			= Filter.createWindow( halfWinSize, winType );
			impPower	= 0.0;
			for( int j = 0; j < halfWinSize; j++ ) {
				buf[ off + j ] *= win[ j ];
				buf[ off - j ] *= win[ j ];			// impPower = half-wing impulse power
				impPower	   += (double) buf[ off + j ] * (double) buf[ off + j ];
			}
			win			= null;
			impPower   -= ((double) buf[ off ] * (double) buf[ off]) / 2;
			gainFactor	   *= Math.sqrt( coverage / impPower );
// System.out.println( "impPower "+impPower +"; coverage "+coverage+"; gain "+gain );

			// apply gain
			for( int j = 1; j < halfWinSize; j++ ) {
				buf[ off + j ] *= (float) gainFactor;
				buf[ off - j ] *= (float) gainFactor;
			}
			buf[ off ] *= (float) gainFactor;

		} // if not allpass
	}

// -------- private Methoden --------

	private double[][] calcFrequencies( double smpRate )
	{
		double[]		sincFreqs;
		double[]		cosineFreqs;
		double[][]		result;
		java.util.List	li;
		Param			p1, p2, p3, p4, p5;
		double			d;
		boolean			neg;
		boolean			ot;
		int				numFreq;
	
		switch( filterType ) {
		case FLT_LOWPASS:
		case FLT_HIGHPASS:
			numFreq						= filterType == FLT_LOWPASS ? 1 : 2;
			sincFreqs					= new double[ numFreq ];
			cosineFreqs					= new double[ numFreq ];
			if( numFreq == 2 ) {
				sincFreqs[ 0 ]			= smpRate / 2;
				cosineFreqs[ 0 ]		= 0.0;
			}
			p1							= cutOff;
			p2							= new Param( -Math.abs( rollOff.val / 2 ), rollOff.unit );
			p3							= new Param( +Math.abs( rollOff.val / 2 ), rollOff.unit );
			p4							= Param.transform( p2, Param.ABS_HZ, p1, null );
			p5							= Param.transform( p3, Param.ABS_HZ, p1, null );
			sincFreqs[ numFreq - 1 ]	= cutOff.val;
			cosineFreqs[ numFreq - 1 ]	= Math.max( p4.val, p5.val ) - Math.min( p4.val, p5.val );
			break;
		
		case FLT_BANDPASS:
		case FLT_BANDSTOP:
		
			p1			= cutOff;
			p2			= new Param( -Math.abs( bandwidth.val / 2 ), bandwidth.unit );
			p3			= new Param( +Math.abs( bandwidth.val / 2 ), bandwidth.unit );
			d			= Param.transform( otLimit, Param.ABS_HZ, cutOff, null ).val;
			li			= new ArrayList();
			neg			= otSpacing.val < 0;
			ot			= overtones && (otSpacing.val != 0.0);
			do {
				p4		= Param.transform( p2, Param.ABS_HZ, p1, null );
				p5		= Param.transform( p3, Param.ABS_HZ, p1, null );
				li.add( p4 );
				li.add( p5 );
				p1		= Param.transform( otSpacing, Param.ABS_HZ, p1, null );
			} while( ot && ((neg && (p1.val > d)) ||			// for( ot < limit )
						   (!neg && (p1.val < d))) );

			if( filterType == FLT_BANDSTOP ) {
				li.add( new Param( smpRate / 2, Param.ABS_HZ ));
			}
			sincFreqs		= new double[ li.size() ];
			cosineFreqs		= new double[ li.size() ];
			for( int i = 0; i < sincFreqs.length; i++ ) {
				sincFreqs[ i ] = ((Param) li.get( i )).val;
				cosineFreqs[ i ] = 0.0;	// XXX
			}
			break;
		
		case FLT_ALLPASS:
			sincFreqs	= new double[ 0 ];
			cosineFreqs	= new double[ 1 ];
			break;

		default:
			assert false : filterType;
			return null;
		}
		
		result		= new double[ 2 ][];
		result[ 0 ]	= sincFreqs;
		result[ 1 ]	= cosineFreqs;
		
		return result;
	}

/*	protected void processFocusEvent( FocusEvent e )
	{
		switch( e.getID()) {
		case FocusEvent.FOCUS_GAINED:
			java11hasFocus = true;
			repaint();
			break;
		case FocusEvent.FOCUS_LOST:
			java11hasFocus = false;
			repaint();
			break;
		}
		super.processFocusEvent( e );
	}
*/

// ------------ CircuitPanel.Box interface ------------

	public javax.swing.Icon getIcon()
	{
		return this;
	}
	
	public CircuitPanel.Box duplicate()
	{
		return new FilterBox( this );
	}
	
	public String toString()
	{
		return( String.valueOf( filterType ) + ';' + String.valueOf( sign ) + ';' +
				cutOff.toString() + ';' + bandwidth.toString() + ';' + gain.toString() + ';' +
				delay.toString() + ';' + String.valueOf( overtones ) + ';' +
				otLimit.toString() + ';' + otSpacing.toString() + ";" +
				rollOff.toString() );
	}

	public CircuitPanel.Box fromString( String s )
	{
		FilterBox		box		= new FilterBox();
		StringTokenizer strTok	= new StringTokenizer( s, ";" );
		
		box.filterType	= Integer.parseInt( strTok.nextToken() );
		box.sign		= Boolean.valueOf( strTok.nextToken() ).booleanValue();
		box.cutOff		= Param.valueOf( strTok.nextToken() );
		box.bandwidth	= Param.valueOf( strTok.nextToken() );
		box.gain		= Param.valueOf( strTok.nextToken() );
		box.delay		= Param.valueOf( strTok.nextToken() );
		box.overtones	= Boolean.valueOf( strTok.nextToken() ).booleanValue();
		box.otLimit		= Param.valueOf( strTok.nextToken() );
		box.otSpacing	= Param.valueOf( strTok.nextToken() );
		if( strTok.hasMoreTokens() ) { // backwards compatible!
			box.rollOff	= Param.valueOf( strTok.nextToken() );
		}

		return box;
	}

// ---------------- Icon interface ---------------- 

	public int getIconWidth()
	{
		return ICON_WIDTH;
	}

	public int getIconHeight()
	{
		return ICON_HEIGHT;
	}
	
	public void paintIcon( Component c, Graphics g, int x, int y )
	{
		Graphics2D g2 = (Graphics2D) g;
		AffineTransform origTrns	= g2.getTransform();

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g2.setStroke( strkLine );
		g2.setPaint( pntBack );
		g2.translate( x, y );
		g2.fillRect( 0, 0, ICON_WIDTH, ICON_HEIGHT );
		g2.setPaint( pntArea );
		g2.fillPolygon( iconPolyX[ filterType ], iconPolyY[ filterType ], iconPolyX[ filterType ].length );
		g2.setPaint( pntLine );
		g2.drawPolyline( iconPolyX[ filterType ], iconPolyY[ filterType ], iconLineNum[ filterType ]);

		if( sign ) {
			g2.drawArc( 0, 0, 6, 6, 0, 360 );
			g2.drawLine( 0, 6, 6, 0 );
		}

		g2.translate( 8, 0 );

		if( gain.val < 0.0 ) {
			g2.drawLine( 6, 0, 0, 3 );
			g2.drawLine( 0, 3, 6, 6 );
		} else if( gain.val > 0.0 ) {
			g2.drawLine( 0, 0, 6, 3 );
			g2.drawLine( 6, 3, 0, 6 );
		}

		g2.translate( 8, 0 );

		if( delay.val != 0.0 ) {
			g2.drawLine( 1, 0, 5, 0 );
			g2.drawLine( 3, 0, 3, 6 );
		}

		g2.translate( 8, 0 );

		if( overtones ) {
			g2.drawLine( 0, 0, 0, 6 );
			g2.drawLine( 2, 1, 2, 6 );
			g2.drawLine( 4, 2, 4, 6 );
			g2.drawLine( 6, 3, 6, 6 );
		}
		g2.setTransform( origTrns );
	}
}
// class FilterBox
