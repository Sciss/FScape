/*
 *  MultiStateButton.java
 *  de.sciss.gui package
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
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
 *		15-Apr-06	created from SwingOSC ; slightly modified look ;
 *					new focus ; setNumColumns ; option to use gradient colours
 *		04-May-06	added autoStep option
 *		17-Apr-07	added icon support, ActionEvents have modifiers
 *		02-Jul-07	added getLastModifiers
 */
 
package de.sciss.gui;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.SwingConstants;

/**
 *	A button with a variable number of states.
 *	Each state is defined by a label and colours,
 *	so as to mimic the functionality of SuperCollider's SCButton.
 *	However, we decided to use a more aqua'ish look.
 *	<p>
 *	New version paints focus WITHOUT requiring <code>focus.png</code> resource.
 *
 *	@author		Hanns Holger Rutz
 *	@version	0.70, 04-May-06
 *
 *	@todo		preferred size : maxH calculation is stupid ; we should try to
 *				get a LineBreakMeasure instance and reveal ascent / descent / leading
 *	@todo		maybe an option to pass arming state and switch directly with
 *				a mouse press...
 *	@todo		maybe option to add an icon
 *
 *	@warning	when using gradient colours, display becomes notably sluggish
 *				because the use of CompositeContext is hell slow
 */
public class MultiStateButton
extends AbstractButton
implements	MouseListener, MouseMotionListener, FocusListener,
			KeyListener, PropertyChangeListener, Composite, CompositeContext
{
	private DefaultButtonModel	model;
	private String				lastTxt;
	private int					txtAscent, txtWidth, txtHeight;

	private int	state			= 0;
	private int	numStates		= 0;
	private int lastModifiers	= 0;
	
	private int recentWidth		= -1;
	
	private final List collStateViews	= new ArrayList();	// element class: StateView

	private static final int NORMAL		= 0;
	private static final int ARMED		= 1;
	private static final int DISABLED	= 2;

	private static final int W1			= 76;
	private static final int W2			= 180;

	private static final int[] pntBackPix		 = { 0xFFEDEDED, 0xFFEEEEEE, 0xFFF0F0F0, 0xFFF1F1F1, 0xFFF2F2F2, 0xFFF3F3F3 };	// normal
	private static final int[] pntBackPixN		 = { 0x12000000, 0x11000000, 0x0F000000, 0x0E000000, 0x0D000000, 0x0C000000 };	// null color

	private static final Color[] colrBorderTop		= { new Color( 0x7F, 0x7F, 0x7F ), new Color( 0x3F, 0x3F, 0x3F ), new Color( 0x7F, 0x7F, 0x7F, 0x7F )};
	private static final Color[] colrBorderRest		= { new Color( 0x97, 0x97, 0x97 ), new Color( 0x4B, 0x4B, 0x4B ), new Color( 0x97, 0x97, 0x97, 0x7F )};
	private static final Color[] colrBorderCorner	= { new Color( 0x83, 0x83, 0x83 ), new Color( 0x41, 0x41, 0x41 ), new Color( 0x83, 0x83, 0x83, 0x7F )};

	private static final Color colrBorderTopS	= new Color( 0x00, 0x00, 0x00, 0x15 );
	private static final Color colrBorderBotS	= new Color( 0xFF, 0xFF, 0xFF, 0x80 );

	private static final Color[] colrBackTop	= { new Color( 0xFB, 0xFB, 0xFB ), new Color( 0x7D, 0x7D, 0x7D ), new Color( 0xFB, 0xFB, 0xFB, 0x7F )};
	private static final Color[] colrBackBot	= { new Color( 0xEC, 0xEC, 0xEC ), new Color( 0x76, 0x76, 0x76 ), new Color( 0xEC, 0xEC, 0xEC, 0x7F )};

	private final static Paint[] pntBackBot;
	
	private static final Color colrBackTopN		= new Color( 0x00, 0x00, 0x00, 0x04 );
	private static final Color colrBackBotN		= new Color( 0x00, 0x00, 0x00, 0x13 );

	private int numColumns = 0;
	
	private boolean autoStep = true;

	static {
		final Rectangle r = new Rectangle( 0, 0, 1, 6 );
		BufferedImage img;
		pntBackBot = new Paint[ 3 ];
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, pntBackPix, 0, 1 );
		pntBackBot[ NORMAL ] = new TexturePaint( img, r );
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, createdArmedPixels( pntBackPix ), 0, 1 );
		pntBackBot[ ARMED ] = new TexturePaint( img, r );
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, createdDisabledPixels( pntBackPix ), 0, 1 );
		pntBackBot[ DISABLED ] = new TexturePaint( img, r );
	}

	public MultiStateButton()
	{
		super();
	
		setBorder( new AquaFocusBorder() );
		addMouseListener( this );
		addMouseMotionListener( this );
		addFocusListener( this );
		addKeyListener( this );
		addPropertyChangeListener( "font", this );
		model = new DefaultButtonModel();
		setModel( model );
		setFocusable( true );
	}
	
	public void setNumColumns( int num )
	{
		if( num != numColumns ) {
			numColumns = num;
			recalcPrefSize();
		}
	}
	
	public int getNumColumns()
	{
		return numColumns;
	}
	
	public void setAutoStep( boolean onOff )
	{
		autoStep = onOff;
	}
	
	public boolean getAutoStep()
	{
		return autoStep;
	}
	
	private void recalcPrefSize()
	{
		final Font	fnt = getFont();
		
		if( fnt == null ) return;
		
		final FontRenderContext frc = new FontRenderContext( GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().getDefaultConfiguration().getNormalizingTransform(), true, true );

		int			maxW = 0, maxH = 0;
		Rectangle2D	r;
		StateView	sv;

		if( numColumns == 0 ) {
			for( int i = 0; i < numStates; i++ ) {
				sv		= (StateView) collStateViews.get( i );
				if( sv.text != null ) {
					r		= fnt.getStringBounds( sv.text, frc );
					maxW	= Math.max( maxW, (int) r.getWidth() );
//					maxH	= Math.max( maxH, (int) r.getHeight() );
				}
				if( sv.icon != null ) {
					maxW	= Math.max( maxW, sv.icon.getIconWidth() );
					maxH	= Math.max( maxH, sv.icon.getIconWidth() );
				}
			}
			r		= fnt.getStringBounds( "Mp", frc );
		} else {
			r		= fnt.getStringBounds( "Mp", frc );
			maxW	= (int) (r.getWidth() * numColumns / 2);
//			maxH	= (int) (r.getHeight() * numColumns / 2);
		}
maxH = Math.max( maxH, (int) r.getHeight() );
//System.err.println( "prefW " + (maxW+24) + "; prefH " + (maxH+13) );
		setPreferredSize( new Dimension( maxW + 24, maxH + 13 ));
	}
	
	private void configureTextColor( StateView sv, Color c )
	{
		sv.colrLabel[ NORMAL ]		= c; // new Color( argb, true );
		sv.colrLabel[ ARMED ]		= createArmedColor( sv.colrLabel[ NORMAL ]);
		sv.colrLabel[ DISABLED ]	= createDisabledColor( sv.colrLabel[ NORMAL ]);
	}
		
	private void configureBgColor( StateView sv )
	{
		sv.pntBack		= null;
		sv.isGradient	= false;
		sv.isClear		= true;
	}

	private void configureBgColor( StateView sv, Color c )
	{
		configureColor( sv.colrBackTop, colrBackTop[ NORMAL ], colrBackTopN, c );
		configureColor( sv.colrBackBot, colrBackBot[ NORMAL ], colrBackBotN, c );

		BufferedImage	img;
		int[]			p;

		p	= mixPixels( pntBackPix, pntBackPixN, c );
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, p, 0, 1 );
		sv.pntBackBot[ NORMAL ] = new TexturePaint( img, new Rectangle( 0, 0, 1, 6 ));
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, createdArmedPixels( p ), 0, 1 );
		sv.pntBackBot[ ARMED ] = new TexturePaint( img, new Rectangle( 0, 0, 1, 6 ));
		img = new BufferedImage( 1, 6, BufferedImage.TYPE_INT_ARGB );
		img.setRGB( 0, 0, 1, 6, createdDisabledPixels( p ), 0, 1 );
		sv.pntBackBot[ DISABLED ] = new TexturePaint( img, new Rectangle( 0, 0, 1, 6 ));

		sv.pntBack		= c;
		sv.isGradient	= false;
	}

	private int[] mixPixels( int[] pNous, int[] pNousN, Color cVous )
	{
		final int[] result	= new int[ pNous.length ];
		
		for( int i = 0; i < result.length; i++ ) {
			result[ i ] = mixPixel( pNous[ i ], pNousN[ i ], cVous );
		}
		
		return result;
	}

	private int mixPixel( int pNous, int pNousN, Color cVous )
	{
		final float va1		= (float) cVous.getAlpha() / 255;
		final float va2		= 1.0f - va1;
		final float	na		= ((pNous >> 24) & 0xFF) * va1 + ((pNousN >> 24) & 0xFF) * va2;
		final float	nr		= ((pNous >> 16) & 0xFF) * va1 + ((pNousN >> 16) & 0xFF) * va2;
		final float	ng		= ((pNous >> 8) & 0xFF) * va1 + ((pNousN >> 8) & 0xFF) * va2;
		final float	nb		= (pNous & 0xFF) * va1 + (pNousN & 0xFF) * va2;
		
		final int vr		= cVous.getRed(); // (pVous >> 16) & 0xFF;
		final int vg		= cVous.getGreen(); // (pVous >> 8) & 0xFF;
		final int vb		= cVous.getBlue(); // pVous & 0xFF;

		return( ((int) na << 24) |
			    (((int) (nr * W1 + (((nr * vr) / 255) * W2)) << 8) & 0xFF0000) |
			     ((int) (ng * W1 + (((ng * vg) / 255) * W2)) & 0xFF00) |
				 ((int) (nb * W1 + (((nb * vb) / 255) * W2)) >> 8) );
	}

	private void configureColor( Color[] svc, Color cNous, Color cNousN, Color cVous )
	{
		svc[ NORMAL ]	= mixColor( cNous, cNousN, cVous );
		svc[ ARMED ]		= createArmedColor( svc[ NORMAL ]);
		svc[ DISABLED ]	= createDisabledColor( svc[ NORMAL ]);
	}

	private Color mixColor( Color cNous, Color cNousN, Color cVous )
	{
		if( cVous == null ) return cNous;
		
		final float va1 = (float) cVous.getAlpha() / 255;
		final float va2 = 1.0f - va1;
		final float nr	= cNous.getRed() * va1 + cNousN.getRed() * va2;
		final float ng	= cNous.getGreen() * va1 + cNousN.getGreen() * va2;
		final float nb	= cNous.getBlue() * va1 + cNousN.getBlue() * va2;
		final float na	= cNous.getAlpha() * va1 + cNousN.getAlpha() * va2;
	
		return new Color( (int) (nr * W1 + (((nr * cVous.getRed()) / 255) * W2)) >> 8,
						  (int) (ng * W1 + (((ng * cVous.getGreen()) / 255) * W2)) >> 8,
						  (int) (nb * W1 + (((nb * cVous.getBlue()) / 255) * W2)) >> 8,
//						  (int) (na * W1 + (((na * cVous.getAlpha()) / 255) * W2)) >> 8 );
						  (int) na );
	}

	private void configureBgColor( StateView sv, Color c1, Color c2 )
	{
		sv.pntBack			= null;
		sv.colrBackGrad1	= c1;
		sv.colrBackGrad2	= c2;
		sv.isGradient		= true;
	}

	private static Color createArmedColor( Color c )
	{
		return new Color( c.getRed() >> 1, c.getGreen() >> 1, c.getBlue() >> 1, 0xFF - ((0xFF - c.getAlpha()) >> 1) );
	}
	
	private static int[] createdArmedPixels( int[] p )
	{
		final int[] result	= new int[ p.length ];
		
		for( int i = 0; i < result.length; i++ ) {
			result[ i ] = ((p[ i ] >> 1) & 0x7F7F7F) | ((0xFF - ((0xFF - ((p[ i ] >> 24) & 0xFF)) >> 1)) << 24);
		}
		
		return result;
	}

	private static int[] createdDisabledPixels( int[] p )
	{
		final int[]		result	= new int[ p.length ];
		final float[]	hsb		= new float[ 3 ];
		int				grey;
		
		for( int i = 0; i < result.length; i++ ) {
			Color.RGBtoHSB( (p[ i ] >> 16) & 0xFF, (p[ i ] >> 8) & 0xFF, p[ i ] & 0xFF, hsb );
			grey		= (int) (hsb[ 2 ] * 0xFF);
			result[ i ] = (grey << 16) | (grey << 8) | grey | ((p[ i ] >> 1) & 0x7F000000);
		}
		
		return result;
	}

	private static Color createDisabledColor( Color c )
	{
		final float[] hsb	= Color.RGBtoHSB( c.getRed(), c.getGreen(), c.getBlue(), null );
		final int     grey	= (int) (hsb[ 2 ] * 0xFF);
	
		return new Color( grey, grey, grey, c.getAlpha() >> 1 );
	}
	
	public void setNumItems( int num )
	{
		StateView sv;
	
		if( (num >= 0) && (num != numStates ) ) {
		
			if( state >= num ) {
				setSelectedIndex( num - 1 );
			}
			while( numStates > num ) {
				collStateViews.remove( numStates - 1 );
				numStates--;
			}
			while( numStates < num ) {
				sv = new StateView();
				collStateViews.add( sv );
				numStates++;
				configureTextColor( sv, Color.black );
//				configureBgColor( sv, Color.lightGray );
				configureBgColor( sv );
			}
		
			if( numColumns == 0 ) recalcPrefSize();
		}
	}
	
	public void removeAllItems()
	{
		setNumItems( 0 );
	}
	
	public void setItemText( int configureState, String text )
	{
		final StateView sv;
		
		if( configureState < numStates ) {
			sv = (StateView) collStateViews.get( configureState );
			sv.text = text;
			if( numColumns == 0 ) recalcPrefSize();
			if( configureState == state ) repaint();
		}
	}
	
	public void setItemIcon( int configureState, Icon icon )
	{
		final StateView sv;
		
		if( configureState < numStates ) {
			sv = (StateView) collStateViews.get( configureState );
			sv.icon = icon;
			if( numColumns == 0 ) recalcPrefSize();
			if( configureState == state ) repaint();
		}
	}
	
	public void setItemTextColor( int configureState, Color c )
	{
		if( configureState < numStates ) {
			final StateView sv = (StateView) collStateViews.get( configureState );
			configureTextColor( sv, c == null ? Color.black : c );
			if( configureState == state ) repaint();
		}
	}
	
	public void setItemBgColor( int configureState, Color c )
	{
		if( configureState < numStates ) {
			final StateView sv = (StateView) collStateViews.get( configureState );
			if( c == null ) {
				configureBgColor( sv );
			} else {
				configureBgColor( sv, c );
			}
			if( configureState == state ) repaint();
		}
	}

	public void setItemBgColor( int configureState, Color c1, Color c2 )
	{
		if( configureState < numStates ) {
			final StateView sv = (StateView) collStateViews.get( configureState );
			configureBgColor( sv, c1 == null ? Color.white : c1, c2 == null ? Color.white : c2 );
			if( configureState == state ) repaint();
		}
	}

	public void setItem( int configureState, String text, Color clrText, Color clrBg )
	{
		if( configureState < numStates ) {
			final StateView sv = (StateView) collStateViews.get( configureState );
			sv.text = text;
			configureTextColor( sv, clrText == null ? Color.black : clrText );
			if( clrBg == null ) {
				configureBgColor( sv );
			} else {
				configureBgColor( sv, clrBg );
			}
			if( numColumns == 0 ) recalcPrefSize();
			if( configureState == state ) repaint();
		}
	}
	
	public void setItem( int configureState, String text, Color clrText, Color clrBg1, Color clrBg2 )
	{
		if( configureState < numStates ) {
			final StateView sv = (StateView) collStateViews.get( configureState );
			sv.text = text;
			configureTextColor( sv, clrText == null ? Color.black : clrText );
			configureBgColor( sv, clrBg1 == null ? Color.white : clrBg1, clrBg2 == null ? Color.white : clrBg2 );
			if( numColumns == 0 ) recalcPrefSize();
			if( configureState == state ) repaint();
		}
	}

	public void addItem( Object text )
	{
		addItem( text.toString(), Color.black, null );
	}

	public void addItem( String text, Color clrText, Color clrBg )
	{
		final int		configureState	= numStates;
		final StateView	sv				= new StateView();
		
		collStateViews.add( sv );
		numStates++;
		sv.text = text;
		configureTextColor( sv, clrText == null ? Color.black : clrText );
		if( clrBg == null ) {
			configureBgColor( sv );
		} else {
			configureBgColor( sv, clrBg );
		}
		if( numColumns == 0 ) recalcPrefSize();
		if( configureState == 0 ) setSelectedIndex( configureState );
	}

	public void addItem( String text, Color clrText, Color clrBg1, Color clrBg2 )
	{
		final int		configureState	= numStates;
		final StateView	sv				= new StateView();
		
		collStateViews.add( sv );
		numStates++;
		sv.text = text;
		configureTextColor( sv, clrText == null ? Color.black : clrText );
		configureBgColor( sv, clrBg1 == null ? Color.white : clrBg1, clrBg2 == null ? Color.white : clrBg2 );
		if( numColumns == 0 ) recalcPrefSize();
		if( configureState == 0 ) setSelectedIndex( configureState );
	}

	public void setSelectedIndex( int state )
	{
		if( this.state != state ) {
			this.state = state;
			repaint();
		}
	}
	
	public int getSelectedIndex()
	{
		return state;
	}
	
	public int getLastModifiers()
	{
		return lastModifiers;
	}
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );

		if( (state < 0) || (state >= numStates) ) return;
				
		final Graphics2D		g2		= (Graphics2D) g;
		final AffineTransform	atOrig	= g2.getTransform();
		final Composite			cmpOrig	= g2.getComposite();
		final int				cidx	= isEnabled() ? (model.isArmed() ? ARMED : NORMAL) : DISABLED;
		StateView				sv;
		
		final Insets i	= getInsets();
		final int x		= i.left;
		final int y		= i.top;
		final int xi	= x + 3;
		final int yi	= y + 3;
		final int x2	= getWidth() - i.right;
		final int y2	= getHeight() - i.bottom;
		final int w		= x2 - x;
		final int h		= y2 - y;
		final int wi	= w - 6;
		final int hi	= h - 6;
		final int hh	= h >> 1;
		final int hh2	= h - hh;
		final int iconX, iconY, iconWidth, iconHeight, txtX, txtY;
		final int txtIconX, txtIconY, txtIconWidth, txtIconHeight;
		
		if( w != recentWidth ) {
			for( int j = 0; j < collStateViews.size(); j++ ) {
				sv = (StateView) collStateViews.get( j );
				if( sv.isGradient ) {
					sv.pntBack = null;
				}
			}
			recentWidth = w;
		}

		sv = (StateView) collStateViews.get( state );
		if( sv.isGradient && (sv.pntBack == null) ) {
			sv.pntBack = new GradientPaint( 0, 0, sv.colrBackGrad1, w - 1, 0, sv.colrBackGrad2 );
		}
	
		if( sv.isGradient || sv.isClear ) {
			g2.setColor( colrBackTop[ cidx ]);
			g2.fillRect( x + 1, y + 1, w - 2, hh - 1 );
			g2.setColor( colrBackBot[ cidx ]);
			g2.fillRect( x + 1, y + hh, w - 2, hh2 - 7 );
			g2.translate( x + 1, y2 - 7 );
			g2.setPaint( pntBackBot[ cidx ]);
			g2.fillRect( 0, 0, w - 2, 6 );
			
			if( sv.isGradient ) {
				g2.translate( 0, y - y2 + 8 );
				g2.setComposite( this );
				g2.setPaint( sv.pntBack );
				g2.fillRect( 0, 0, w - 2, h - 2 );
				g2.setComposite( cmpOrig );
			}
		} else {
			g2.setColor( sv.colrBackTop[ cidx ]);
			g2.fillRect( x + 1, y + 1, w - 2, hh - 1 );
			g2.setColor( sv.colrBackBot[ cidx ]);
			g2.fillRect( x + 1, y + hh, w - 2, hh2 - 7 );
			g2.translate( x + 1, y2 - 7 );
			g2.setPaint( sv.pntBackBot[ cidx ]);
			g2.fillRect( 0, 0, w - 2, 6 );		
		}
		g2.setTransform( atOrig );
				
		g2.setColor( colrBorderCorner[ cidx ]);
		g2.drawLine( x, y, x + 1, y );
		g2.drawLine( x2 - 2, y, x2 - 1, y );
		g2.setColor( colrBorderTopS );
		g2.drawLine( x, y - 1, x2, y - 1 );
		g2.setColor( colrBorderTop[ cidx ]);
		g2.drawLine( x + 1, y, x2 - 2, y );
		g2.setColor( colrBorderRest[ cidx ]);
		g2.drawLine( x, y + 1, x, y2 - 2 );
		g2.drawLine( x2 - 1, y + 1, x2 - 1, y2 - 2 );
		g2.drawLine( x, y2 - 1, x2 - 1, y2 - 1 );
		g2.setColor( colrBorderBotS );
		g2.drawLine( x, y2, x2 - 1, y2 );

		if( (sv.text != null) && (sv.text != lastTxt) ) {
			final FontMetrics fm= g.getFontMetrics( g.getFont() );
			lastTxt				= sv.text;
			txtWidth			= fm.stringWidth( lastTxt );
			txtHeight			= fm.getHeight();
			txtAscent			= fm.getAscent();
		}

		if( sv.icon == null ) {
			iconWidth			= 0;
			iconHeight			= 0;
			if( sv.text == null ) {
				txtIconWidth	= 0;
				txtIconHeight	= 0;
			} else {
				txtIconWidth	= txtWidth;
				txtIconHeight	= txtHeight;
			}
		} else {
			iconWidth			= sv.icon.getIconWidth();
			iconHeight			= sv.icon.getIconHeight();
			if( sv.text == null ) {
				txtIconWidth	= iconWidth;
				txtIconHeight	= iconHeight;
			} else {
				if( this.getHorizontalTextPosition() != SwingConstants.CENTER ) {
					txtIconWidth= iconWidth + txtWidth + getIconTextGap();
				} else {
					txtIconWidth= Math.max( iconWidth, txtWidth );
				}
				txtIconHeight	= Math.max( txtHeight, iconHeight );
			}
		}
		switch( getHorizontalAlignment() ) {
		case SwingConstants.CENTER:
			txtIconX = ((wi - txtIconWidth) >> 1) + xi;
			break;
		case SwingConstants.LEFT:
		case SwingConstants.LEADING:
			txtIconX = xi;
			break;
		case SwingConstants.RIGHT:
		case SwingConstants.TRAILING:
			txtIconX = wi - txtIconWidth + xi;
			break;
		default:
			throw new IllegalArgumentException( "horizontalAlignment " + getHorizontalAlignment() );
		}
		switch( getVerticalAlignment() ) {
		case SwingConstants.CENTER:
			txtIconY = ((hi - txtIconHeight) >> 1) + yi;
			break;
		case SwingConstants.TOP:
			txtIconY = yi;
			break;
		case SwingConstants.BOTTOM:
			txtIconY = hi - txtIconHeight + yi;
			break;
		default:
			throw new IllegalArgumentException( "verticalAlignment " + getVerticalAlignment() );
		}
		
		if( sv.icon != null ) {
			switch( getHorizontalTextPosition() ) {
			case SwingConstants.CENTER:
				iconX = ((txtIconWidth - iconWidth) >> 1) + txtIconX;
				break;
			case SwingConstants.LEFT:
			case SwingConstants.LEADING:
				iconX = txtIconWidth - iconWidth + txtIconX;
				break;
			case SwingConstants.RIGHT:
			case SwingConstants.TRAILING:
				iconX = txtIconX;
				break;
			default:
				throw new IllegalArgumentException( "horizontalTextPosition " + getHorizontalTextPosition() );
			}
			switch( getVerticalTextPosition() ) {
			case SwingConstants.CENTER: // XXX
				iconY = ((txtIconHeight - iconHeight) >> 1) + txtIconY;
				break;
			case SwingConstants.TOP: // XXX
				iconY = txtIconY;
				break;
			case SwingConstants.BOTTOM: // XXX
				iconY = txtIconHeight - iconHeight + txtIconY;
				break;
			default:
				throw new IllegalArgumentException( "verticalTextPosition " + getVerticalTextPosition() );
			}
			sv.icon.paintIcon( this, g, iconX, iconY );
		}
		
		if( sv.text != null ) {
			switch( getHorizontalTextPosition() ) {
			case SwingConstants.CENTER:
				txtX = ((txtIconWidth - txtWidth) >> 1) + txtIconX;
				break;
			case SwingConstants.LEFT:
			case SwingConstants.LEADING:
				txtX = txtIconX;
				break;
			case SwingConstants.RIGHT:
			case SwingConstants.TRAILING:
				txtX = txtIconWidth - txtWidth + txtIconX;
				break;
			default:
				throw new IllegalArgumentException( "horizontalTextPosition " + getHorizontalTextPosition() );
			}
			switch( getVerticalTextPosition() ) {
			case SwingConstants.CENTER:
				txtY = ((txtIconHeight - txtHeight) >> 1) + txtIconY;
				break;
			case SwingConstants.TOP:
				txtY = txtIconY;
				break;
			case SwingConstants.BOTTOM:
				txtY = txtIconHeight - txtHeight + txtIconY;
				break;
			default:
				throw new IllegalArgumentException( "verticalTextPosition " + getVerticalTextPosition() );
			}

			g2.setColor( sv.colrLabel[ cidx ]);
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2.drawString( lastTxt, txtX, txtY + txtAscent );
		}
	}
	
	public static void makeTestFrame( int htextPos, int vtextPos )
	{
		final javax.swing.JFrame f = new javax.swing.JFrame( "Test ");
		final java.awt.Container c = f.getContentPane();
		final int[] halign = new int[] { SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT };
		final int[] valign = new int[] { SwingConstants.TOP, SwingConstants.CENTER, SwingConstants.BOTTOM };
		MultiStateButton b;
		final Icon i = new Icon() {
			public void paintIcon( java.awt.Component c, Graphics g, int x, int y )
			{
				g.setColor( Color.red );
				g.drawRect( x, y, 39, 39 );
			}
			
			public int getIconWidth() { return 40; }
			public int getIconHeight() { return 40; }
		};
		
		c.setLayout( new java.awt.GridLayout( 3, 3 ));
		for( int row = 0; row < 3; row++ ) {
			for( int col = 0; col < 3; col++ ) {
				b = new MultiStateButton();
				b.addItem( "Test" );
				b.setItemIcon( 0, i );
				b.setHorizontalAlignment( halign[ col ]);
				b.setVerticalAlignment( valign[ row ]);
				b.setHorizontalTextPosition( htextPos );
				b.setVerticalTextPosition( vtextPos );
				b.setIconTextGap( 16 );
				c.add( b );
			}
		}
		
		f.pack();
		f.setVisible( true );
		f.toFront();
	}

	private void lala( InputEvent e )
	{
		model.setPressed( true );
		if( model.isArmed() ) {
			if( autoStep && (numStates > 0) ) state = (state + 1) % numStates;
			lastModifiers = e.getModifiers();
			fireActionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "",
				e.getWhen(), lastModifiers ));
			model.setArmed( false );
			repaint();
		}
	}

// ---------------- Composite interface ----------------

	public CompositeContext createContext( ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints ) 
	{
		return this;
	}

// ---------------- CompositeContext interface ----------------

	// this composite implements a "multiply"
	// algorithm as known e.g. from photoshop
	// (for each RGB component, source and target (normalized to 0 ... 1)
	// are multiplied)
	public void compose( Raster srcR, Raster dstInR, WritableRaster dstOutR ) 
	{
		final DataBuffer	srcBuf		= srcR.getDataBuffer();
		final DataBuffer	dstInBuf	= dstInR.getDataBuffer();
		final DataBuffer	dstOutBuf	= dstOutR.getDataBuffer();
		final int			num			= dstOutBuf.getSize();
	
		int src, dst;
		
		for( int i = 0; i < num; i++ ) {
			src = srcBuf.getElem( i );
			dst = dstInBuf.getElem( i );
			dstOutBuf.setElem( i,
				(((src & 0xFF) * (dst & 0xFF) >> 8)) |
				((((src >> 8) & 0xFF) * ((dst >> 8) & 0xFF)) & 0xFF00) |
				(((((src >> 16) & 0xFF) * ((dst >> 16) & 0xFF)) << 8) & 0xFF0000) |
				(((((src >> 24) & 0xFF) * ((dst >> 24) & 0xFF)) << 16) & 0xFF000000)
			);
		}
	}
	
	public void dispose() {}

// ---------------- Mouse(Motion)Listener interfaces ----------------

	public void mousePressed( MouseEvent e )
	{
		if( !isEnabled() ) return;

		requestFocus();

		if( e.isControlDown() ) return;

		model.setArmed( true );
		repaint();
	}

	public void mouseReleased( MouseEvent e )
	{
		if( isEnabled() ) {
			lala( e );
		}
	}

	public void mouseDragged( MouseEvent e )
	{
		if( !isEnabled() || e.isControlDown() ) return;

		final boolean oldState = model.isArmed();
		final boolean newState = this.contains( e.getPoint() );
		if( oldState != newState ) {
			model.setArmed( newState );
			repaint();
		}
	}
	
	public void mouseClicked( MouseEvent e ) {}
	public void mouseEntered( MouseEvent e ) {}
	public void mouseExited( MouseEvent e ) {}
	public void mouseMoved( MouseEvent e ) {}

// ---------------- KeyListener interface ----------------

	public void keyPressed( KeyEvent e )
	{
		if( isEnabled() && (e.getKeyChar() == ' ') && !model.isArmed() ) {
			model.setArmed( true );
			repaint();
		}
	}
	
	public void keyReleased( KeyEvent e )
	{
		if( isEnabled() && (e.getKeyChar() == ' ') ) {
			lala( e );
		}
	}

	public void keyTyped( KeyEvent e ) {}

// ---------------- FocusListener interface ----------------

	public void focusGained( FocusEvent e )
	{
		repaint();
	}

	public void focusLost( FocusEvent e )
	{
		repaint();
	}
	
// ---------------- PropertyChangeListener interface ----------------

	public void propertyChange( PropertyChangeEvent e )
	{
		lastTxt = null;
		recalcPrefSize();
		repaint();
	}

// ---------------- internal classes ----------------

	private static class StateView
	{
		private String	text;
		private Paint	pntBack;
		private Color[]	colrLabel;
		private Color	colrBackGrad1, colrBackGrad2;
		private boolean	isGradient;
		private boolean	isClear;
		private Color[]	colrBackTop, colrBackBot;
		private Paint[]	pntBackBot;
		private Icon	icon;
		
		private StateView()
		{
			colrLabel				= new Color[ 3 ];
			colrBackTop				= new Color[ 3 ];
			colrBackBot				= new Color[ 3 ];
			pntBackBot				= new Paint[ 3 ];
		}

//		private StateView( StateView orig )
//		{
//			this.text				= orig.text;
//			this.pntBack				= orig.pntBack;
//			this.colrLabel			= new Color[] { orig.colrLabel[0], orig.colrLabel[1], orig.colrLabel[2] };
//			this.colrBackGrad1		= orig.colrBackGrad1;
//			this.colrBackGrad2		= orig.colrBackGrad2;
//			this.isGradient			= orig.isGradient;
//			this.isClear				= orig.isClear;
//			this.colrBackTop			= new Color[] { orig.colrBackTop[0], orig.colrBackTop[1], orig.colrBackTop[2] };
//			this.colrBackBot			= new Color[] { orig.colrBackBot[0], orig.colrBackBot[1], orig.colrBackBot[2] };
//			this.pntBackBot			= new Paint[] { orig.pntBackBot[0], orig.pntBackBot[1], orig.pntBackBot[2] };
//		}
	}
}