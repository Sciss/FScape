/*
 *  StatisticsDlg.java
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
 *		07-Jan-05	fixed painter method
 *		16-Feb-05	uses VectorDisplay instead of own custom paint methods
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

import de.sciss.fscape.io.FloatFile;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.ModulePanel;
import de.sciss.fscape.spect.Fourier;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Util;
import de.sciss.gui.Axis;
import de.sciss.gui.VectorSpace;

import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;
import de.sciss.io.Marker;

/**
 *	Various graphic info displays about a sound file,
 *	such as amplitude or power spectrum. Only mono at the moment.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 21-Jan-09
 */
public class StatisticsDlg
extends ModulePanel
{
// -------- private Variablen --------

	// Properties (defaults)
	private static final int PR_INPUTFILE		= 0;		// pr.text
	private static final int PR_PROPERTY		= 0;		// pr.intg
	private static final int PR_CHANNEL			= 1;
	private static final int PR_DATALEN			= 2;
	private static final int PR_ANALYSISWIN		= 3;
	private static final int PR_SUM				= 0;		// pr.bool
	private static final int PR_HLOG			= 1;
	private static final int PR_VLOG			= 2;

	private static final int PROP_ASPECT		= 0;
	private static final int PROP_PSPECT		= 1;
	private static final int PROP_PHSPECT		= 2;
	private static final int PROP_ELONG			= 3;
	private static final int NUMPROP			= 4;

//	private static final int CHANNEL_LEFT		= 0;
//	private static final int CHANNEL_RIGHT		= 1;
	private static final int CHANNEL_SUM		= 2;
//	private static final int CHANNEL_SUB		= 3;

	private static final String PRN_INPUTFILE	= "InputFile";
	private static final String PRN_PROPERTY	= "Property";
	private static final String PRN_CHANNEL		= "Channel";
	private static final String PRN_DATALEN		= "DataLen";
	private static final String PRN_ANALYSISWIN	= "AnalysisWin";
	private static final String PRN_SUM			= "Histo";	// named this way for compatibility reasons
	private static final String PRN_HLOG		= "HLog";
	private static final String PRN_VLOG		= "VLog";

	private static final String	prText[]		= { "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE };
	private static final int	prIntg[]		= { PROP_ASPECT, CHANNEL_SUM, 3, 0 };
	private static final String	prIntgName[]	= { PRN_PROPERTY, PRN_CHANNEL, PRN_DATALEN, PRN_ANALYSISWIN };
	private static final boolean prBool[]		= { false, false, false };
	private static final String	prBoolName[]	= { PRN_SUM, PRN_HLOG, PRN_VLOG };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_PROPERTY		= GG_OFF_CHOICE		+ PR_PROPERTY;
	private static final int GG_CHANNEL			= GG_OFF_CHOICE		+ PR_CHANNEL;
	private static final int GG_DATALEN			= GG_OFF_CHOICE		+ PR_DATALEN;
	private static final int GG_ANALYSISWIN		= GG_OFF_CHOICE		+ PR_ANALYSISWIN;
	private static final int GG_SUM				= GG_OFF_CHECKBOX	+ PR_SUM;
	private static final int GG_HLOG			= GG_OFF_CHECKBOX	+ PR_HLOG;
	private static final int GG_VLOG			= GG_OFF_CHECKBOX	+ PR_VLOG;
	private static final int GG_DATASET			= GG_OFF_OTHER		+ 0;

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;

//	private ImageCanvas	ggVectorDisplay;
	private VectorDisplay		ggVectorDisplay;
	private DataRecord			properties[];
	private Point				lastPt						= null;		// X-Hair
	private String				lastTxt;
	private boolean				paintCrossHair				= false;
	private final Cursor		xhairCursor					= new Cursor( Cursor.CROSSHAIR_CURSOR );
	private Axis				haxis, vaxis;

	private FontMetrics			fntMetr;
	private static final Color	colrCross	= new Color( 0x00, 0x00, 0x00, 0x7F );
	private static final Color	colrTextBg	= new Color( 0xFF, 0xFF, 0xFF, 0xA0 );

	private final MessageFormat msgHertz	= new MessageFormat( "{0,number,0.0} Hz", Locale.US );  // XXX US locale
	private final MessageFormat msgDecibel	= new MessageFormat( "{0,number,0.0} dB", Locale.US );  // XXX US locale
	private final MessageFormat msgPlain	= new MessageFormat( "{0,number,0.000}", Locale.US );  // XXX US locale
	private final MessageFormat msgDegree	= new MessageFormat( "{0,number,0.000} rad", Locale.US );  // XXX US locale

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
	public StatisticsDlg()
	{
		super( "Statistics" );
		init2();
	}
	
	protected void buildGUI()
	{
		// einmalig PropertyArray initialisieren
		if( static_pr == null ) {
			static_pr			= new PropertyArray();

			static_pr.text		= prText;
			static_pr.textName	= prTextName;
			static_pr.intg		= prIntg;
			static_pr.intgName	= prIntgName;
			static_pr.bool		= prBool;
			static_pr.boolName	= prBoolName;

//			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets		= static_presets;
		pr 			= (PropertyArray) static_pr.clone();

	// -------- init --------

		properties	= new DataRecord[ NUMPROP ];
		for( int i = 0; i < properties.length; i++ ) {
			properties[ i ] = null;
		}

	// -------- GUI bauen --------

		GridBagConstraints	con;
		
		PathField			ggInputFile;
		JComboBox			ggProperty, ggChannel, ggDataLen, ggAnalysisWin;
		JCheckBox			ggSum, ggHLog, ggVLog;
		JPanel				displayPane;
		Box					box;
		
		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		MouseInputAdapter	mia = new MouseInputAdapter() {
			public void mousePressed( MouseEvent e )
			{
				redrawCrosshair( e );
			}

			public void mouseEntered( MouseEvent e )
			{
				ggVectorDisplay.setCursor( xhairCursor );
			}

			public void mouseExited( MouseEvent e )
			{
				ggVectorDisplay.setCursor( null );
			}

			public void mouseDragged( MouseEvent e )
			{
				redrawCrosshair( e );
			}
		};

		TopPainter	tp = new TopPainter() {
			public void paintOnTop( Graphics2D g )
			{
				Dimension dim = ggVectorDisplay.getSize();
			
				synchronized( properties ) {
					if( paintCrossHair ) {
						g.setColor( colrCross );
						g.drawLine( 0, lastPt.y, dim.width - 1, lastPt.y );
						g.drawLine( lastPt.x, 0, lastPt.x, dim.height - 1 );
						g.setColor( colrTextBg );
						g.fillRect( 1, 1, fntMetr.stringWidth( lastTxt ) + 6, fntMetr.getHeight() + 4 );
						g.setColor( Color.blue );
						g.drawString( lastTxt, 4, fntMetr.getHeight() + 1 );
					}
				}
			}
		};

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int			ID		= gui.getItemID( e );
				DataRecord	rec;

				switch( ID ) {
				case GG_PROPERTY:
					synchronized( properties ) {
						rec = properties[ pr.intg[ PR_PROPERTY ]];
						if( rec != null ) {
							rec.data	= null;
						}
					}
					// THRU
				case GG_CHANNEL:
					pr.intg[ ID - GG_OFF_CHOICE ] = ((JComboBox) e.getSource()).getSelectedIndex();
					redrawDataset();
					break;
				case GG_SUM:
				case GG_HLOG:
				case GG_VLOG:
					pr.bool[ ID - GG_OFF_CHECKBOX ] = ((JCheckBox) e.getSource()).isSelected();
					redrawDataset();
					break;
				}
			}
		};

	// -------- I/O-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Sound Source", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		ggInputFile.handleTypes( GenericFile.TYPES_SOUND );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "File name", SwingConstants.RIGHT ));
		con.gridheight	= 2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, null );

	// -------- --------
	gui.addLabel( new GroupLabel( "Description", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggProperty		= new JComboBox();
		ggProperty.addItem( "Amp. spectrum" );
		ggProperty.addItem( "Power spectrum" );
		ggProperty.addItem( "Diff'ed phase spect" );
		ggProperty.addItem( "Sample histogram" );
		con.weightx		= 0.133;
		con.gridwidth	= 1;
		gui.addLabel( new JLabel( "Property", SwingConstants.RIGHT ));
		con.weightx		= 0.2;
		con.gridwidth	= 2;
		gui.addChoice( ggProperty, GG_PROPERTY, il );

		ggDataLen		= new JComboBox();
		for( int i = 32; i <= 65536; i <<= 1 ) {
			ggDataLen.addItem( String.valueOf( i ));
		}
		con.gridwidth	= 1;
		con.weightx		= 0.2;
		gui.addLabel( new JLabel( "Record size", SwingConstants.RIGHT ));
		con.weightx		= 0.133;
		gui.addChoice( ggDataLen, GG_DATALEN, il );

		ggAnalysisWin	= new JComboBox();
		GUISupport.addItemsToChoice( Filter.getWindowNames(), ggAnalysisWin );
		con.weightx		= 0.133;
		gui.addLabel( new JLabel( "Analysis win", SwingConstants.RIGHT ));
		con.weightx		= 0.2;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addChoice( ggAnalysisWin, GG_ANALYSISWIN, il );

		ggVectorDisplay		= new VectorDisplay();
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		gui.registerGadget( ggVectorDisplay, GG_DATASET );
		ggVectorDisplay.addMouseListener( mia );
		ggVectorDisplay.addMouseMotionListener( mia );
		ggVectorDisplay.addTopPainter( tp );
		ggVectorDisplay.setPreferredSize( new Dimension( 256, 256 )); // XXX
		displayPane		= new JPanel( new BorderLayout() );
		displayPane.add( ggVectorDisplay, BorderLayout.CENTER );
		haxis			= new Axis( Axis.HORIZONTAL );
		vaxis			= new Axis( Axis.VERTICAL );
		box				= Box.createHorizontalBox();
		box.add( Box.createHorizontalStrut( vaxis.getPreferredSize().width ));
		box.add( haxis );
		displayPane.add( box, BorderLayout.NORTH );
		displayPane.add( vaxis, BorderLayout.WEST );
		gui.addGadget( displayPane, -1 );

		ggChannel		= new JComboBox();
		ggChannel.addItem( "Left" );
		ggChannel.addItem( "Right" );
		ggChannel.addItem( "Sum" );
		ggChannel.addItem( "Difference" );
		con.weightx		= 0.15;
		con.weighty		= 0.0;
		con.gridwidth	= 2;
		gui.addLabel( new JLabel( "View channel(s)", SwingConstants.RIGHT ));
		con.weightx		= 0.1;
		con.gridwidth	= 1;
		gui.addChoice( ggChannel, GG_CHANNEL, il );

		ggSum			= new JCheckBox( "Integ." );
		con.weightx		= 0.25;
		gui.addCheckbox( ggSum, GG_SUM, il );

		ggHLog			= new JCheckBox( "Log h.scale" );
		con.weightx		= 0.25;
		gui.addCheckbox( ggHLog, GG_HLOG, il );
		ggVLog			= new JCheckBox( "Log v.scale" );
		con.weightx		= 0.25;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		gui.addCheckbox( ggVLog, GG_VLOG, il );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );

        Font fnt = getFont();
		fntMetr = getComponent().getFontMetrics(fnt);
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );
	}
	
// -------- Processor Interface --------
		
	protected void process()
	{
		int			i, j, k, ch;
		long		progOff, progLen;
		float		max;

		AudioFile	inF				= null;
		AudioFileDescr	inStream;
		File		tempFile		= null;
		FloatFile	floatF			= null;

		int			inLength;
		int			inChanNum;
		int			off;
		int			chunkLength;
//		int			normFactor;

		int			framesRead;
		int			totalInSamples;
		
		float		inBuf[][];
		float		procBuf[]		= null;
		float		rotBuf[]		= null;
		float		convBuf1[];
		DataRecord	rec;
		float		data[];
		float		data2[];
		int			dataLen;
		int			frameSize, stepSize;
		float		win[]			= null;
		int			support;

		int			propertyID;
		long		dispTime, dispDelta;

//		Param		ampRef			= new Param( 1.0, Param.ABS_AMP );			// transform-Referenz
		
		java.util.List	markers;
		
topLevel: try {
			synchronized( properties ) {
				propertyID	= pr.intg[ PR_PROPERTY ];
				rec			= properties[ propertyID ];
				properties[ propertyID ] = null;
				if( rec != null ) {
					rec.tempFile.delete();
					rec.data		= null;
					rec.tempFile	= null;
				}
			}

		// ---- open files ----

			inF				= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
			inStream		= inF.getDescr();
			inChanNum		= inStream.channels;
			inLength		= (int) inStream.length;
			totalInSamples	= inLength * inChanNum;
			// this helps to prevent errors from empty files!
			if( totalInSamples <= 0 ) throw new EOFException( ERR_EMPTY );
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- preparations ----

			tempFile	= IOUtil.createTempFile();
			floatF		= new FloatFile( tempFile, GenericFile.MODE_OUTPUT );
			dataLen		= 1 << (pr.intg[ PR_DATALEN ] + 5);
			data		= new float[ dataLen ];
			data2		= new float[ dataLen ];
			frameSize	= dataLen;
			stepSize	= frameSize;
			support		= 0;

			rec			= new DataRecord();
//			rec.data	= data2;
			rec.tempFile= tempFile;
			rec.afd		= inStream;

			switch( pr.intg[ PR_PROPERTY ]) {
			case PROP_PHSPECT:
				markers	= (java.util.List) inStream.getProperty( AudioFileDescr.KEY_MARKERS );
				if( markers != null ) {
					i	= Marker.find( markers, FIRDesignerDlg.MARK_SUPPORT, 0 );
					if(	i >= 0 ) {	// impulse file contains "support" marker ==> adjust rotation so we get zero phase
						support	= (int) ((Marker) markers.get( i )).pos;
					}
				}
				rotBuf	= new float[ frameSize << 1 ];
				// THRU
			case PROP_ASPECT:
			case PROP_PSPECT:
				frameSize <<= 1;
//				stepSize  >>= 1;	// 4x overlap
				win			= Filter.createWindow( dataLen, pr.intg[ PR_ANALYSISWIN ]);
				procBuf		= new float[ frameSize + 2 ];
				break;
			}
			inBuf		= new float[ inChanNum ][ frameSize + 2 ];
			
			// clear buffers
			Util.clear( data );
			Util.clear( inBuf );

			progOff		= 0;
			progLen		= ((long) inLength << 1) + dataLen;
			dispDelta	= 10000;	// first prelim. display after 10 sec.
			dispTime	= System.currentTimeMillis() + dispDelta;

		// ---- calc ----
			
//			normFactor = 0;
			off			= 0;
			chunkLength = Math.min( frameSize, inLength );
			inF.readFrames( inBuf, 0, chunkLength );
			framesRead	= chunkLength;
			progOff		+= chunkLength;
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
		// .... check running ....
			if( !threadRunning ) break topLevel;

			do {
				switch( pr.intg[ PR_PROPERTY ]) {
				case PROP_ASPECT:	// XXX
				case PROP_PSPECT:
				case PROP_PHSPECT:
					for( ch = 0; ch < 1; ch++ ) {
						// so kopieren, dass chronologie stimmt wg. windowing
						System.arraycopy( inBuf[ ch ], off, procBuf, 0, frameSize - off );
						System.arraycopy( inBuf[ ch ], 0, procBuf, frameSize - off, off );
						for( i = 1, j = dataLen; j < frameSize-1; i++, j++ ) {
							procBuf[ j ] *= win[ i ];
						}
						procBuf[ j ] = 0.0f;
						for( i = 0, j = dataLen - 1; j >= 0; i++, j-- ) {
							procBuf[ j ] *= win[ i ];
						}
						if( pr.intg[ PR_PROPERTY ] == PROP_PHSPECT ) {		// XXX
							Util.rotate( procBuf, frameSize, rotBuf, -((support - off) % frameSize) );
						}
						Fourier.realTransform( procBuf, frameSize, Fourier.FORWARD );
						switch( pr.intg[ PR_PROPERTY ]) {
						case PROP_ASPECT:	// XXX
							Fourier.rect2Polar( procBuf, 0, procBuf, 0, frameSize + 2 );
							for( i = 0, j = 0; i < dataLen; i++, j += 2 ) {
								data[ i ] += procBuf[ j ];
							}
//							normFactor++;
							break;
						case PROP_PSPECT:
							for( i = 0, j = 0; i < dataLen; i++ ) {
								data[ i ] += (float) Math.abs( (double) procBuf[ j++ ] * (double) procBuf[ j++ ]);
							}
							break;
						case PROP_PHSPECT:
							Fourier.rect2Polar( procBuf, 0, procBuf, 0, frameSize + 2 );
							Fourier.unwrapPhases( procBuf, 0, procBuf, 0, frameSize + 2 );
							for( i = 0, j = 1; i < dataLen; i++ ) {
								k			= j + 2;
								data[ i ]  += procBuf[ k ] - procBuf[ j ];
								j			= k;
							}
							break;
						}
					}
					break;
				
				case PROP_ELONG:	// XXX
					for( ch = 0; ch < 1; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = 0; i < chunkLength; i++ ) {
//							j = (int) ((1.0 + Math.log( Math.max( 1.192092896e-07, Math.abs( convBuf1[ i ]))) / 15.94238515) *
//									  (dataLen - 1) + 0.5f);
							j = (int) (Math.abs( convBuf1[ i ]) * (dataLen - 1) + 0.5f);
							data[ Math.min( dataLen - 1, j )]++;
						}
					}
					break;
				}

				progOff		+= chunkLength;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );

			// .... read next chunk ....
				chunkLength  = Math.min( stepSize, inLength - framesRead );
				inF.readFrames( inBuf, off, chunkLength );
				framesRead	+= chunkLength;
				progOff		+= chunkLength;
			// .... progress ....
				setProgression( (float) progOff / (float) progLen );
			// .... check running ....
				if( !threadRunning ) break topLevel;

				// zero-padding
				if( chunkLength < stepSize ) {
					for( ch = 0; ch < inChanNum; ch++ ) {
						convBuf1 = inBuf[ ch ];
						for( i = chunkLength, j = i + off; i < stepSize; i++, j++ ) {
							convBuf1[ j ] = 0.0f;
						}
					}
				}
				off = (off + stepSize) % frameSize;

			// ---- update display after increasing time deltas and after last chunk ----
				if( (System.currentTimeMillis() > dispTime) || (chunkLength == 0) ) {
				
					System.arraycopy( data, 0, data2, 0, dataLen );
					max = 0.0f;
					for( i = 0; i < dataLen; i++ ) {
						if( data2[ i ] > max ) {
							max = data2[ i ];
						}
					}
					if( max > 0.0f ) {
						for( i = 0; i < dataLen; i++ ) {
							data2[ i ] /= max;
						}
					}

//					calcMinMax( rec );
					floatF.seekFloat( 0 );
					floatF.writeFloats( data2, 0, dataLen );
					floatF.flush();

					synchronized( properties ) {
						properties[ propertyID ] = rec;
						rec.data = null;	// force reload
						redrawDataset();
					}
					
					dispDelta <<= 1;
					dispTime   += dispDelta;
				}

			} while( threadRunning && (chunkLength > 0) );
		// .... check running ....
			if( !threadRunning ) break topLevel;

//			rec.data = data2;

			floatF.close();
			floatF		= null;
			inF.close();
			inF			= null;
			inStream	= null;
			progOff	   += dataLen;
		// .... progress ....
			setProgression( (float) progOff / (float) progLen );
		// .... check running ....
			if( !threadRunning ) break topLevel;
			
		// ---- Finish ----

// System.out.println( "progOff "+progOff+"; progLen "+progLen );

		} // topLevel
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			inBuf		= null;
			procBuf		= null;
			rotBuf		= null;
			convBuf1	= null;
			inStream	= null;
			data		= null;
			data2		= null;
			win			= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup (topLevel) ----
		if( inF != null ) {
			inF.cleanUp();
		}
		if( floatF != null ) {
			floatF.cleanUp();
			if( getError() != null ) tempFile.delete();
		}
	}

// -------- private Methoden --------

	protected void redrawDataset()
	{
		DataRecord	rec			= properties[ pr.intg[ PR_PROPERTY ]];
		FloatFile	floatF		= null;
		boolean		needsReload;
		int			decimate	= 0;
		int			i, j, dispLen, dataLen;
		float[]		dispBuf, data;
		float		f1, max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
		double		d2, d3, log, scale, weight, offset, fmin, fmax, fc = 1.0, f0;
		double		decibelWeight	= 20.0 / Constants.ln10;

		synchronized( properties ) {
			if( rec != null ) {
				needsReload = (rec.sum && !pr.bool[ PR_SUM ]) ||
							  (rec.hlog  && !pr.bool[ PR_HLOG ])  ||
							  (rec.vlog  && !pr.bool[ PR_VLOG ])  ||
							  (pr.bool[ PR_SUM ] &&
								  ((rec.hlog != pr.bool[ PR_HLOG ])  ||
								   (rec.vlog != pr.bool[ PR_VLOG ]))) ||
							  ((rec.hlog != pr.bool[ PR_HLOG ]) && pr.bool[ PR_VLOG ]);

				if( needsReload || (rec.data == null) ) {
					try {
						floatF		= new FloatFile( rec.tempFile, GenericFile.MODE_INPUT );
						rec.sum		= false;
						rec.hlog	= false;
						rec.vlog	= false;
						rec.data	= new float[ (int) floatF.getSize() ];
						floatF.seekFloat( 0 );
						floatF.readFloats( rec.data, 0, rec.data.length );
						floatF.close();

//						calcMinMax( rec );
					}
					catch( Exception e1 ) {
						System.err.println( e1.getLocalizedMessage() );
						rec.data = null;
						if( floatF != null ) {
							floatF.cleanUp();
							floatF = null;
						}
					}
				}

				// adjust + display dataset
				if( rec.data != null ) {
					data		= rec.data;
					dataLen		= data.length;

					// sum
					if( pr.bool[ PR_SUM ] && !rec.sum ) {
						d2 = 0.0;
						for( i = 0; i < dataLen; i++ ) {
							d2		   += data[ i ];
							data[ i ]   = (float) d2;
						}
						rec.sum	= true;
					}
					
					if( pr.bool[ PR_HLOG ] && !rec.hlog ) {
						dispLen		= 2049; // 4097;
						dispBuf		= new float[ dispLen ];
						switch( pr.intg[ PR_PROPERTY ]) {
						case PROP_ELONG:
							fc			= Math.sqrt( 0.5 );		// gewuenschte centerfreq
							fmin		= 0.0001;		// gewuenschte min. freq
							fmax		= 1.0;			// oberste freq.
							break;
						default:
							fc			= 1000.0;			// gewuenschte centerfreq
							fmin		= 16.0; // 16.0;				// gewuenschte min. freq
							fmax		= rec.afd.rate/2;	// oberste freq.
							break;
						}
						f0			= fc*fc / fmax;
						log			= Math.log( fmax/f0 );
						offset		= Math.log( fmin/f0 ) / log;
						weight		= (1.0 - offset) / (dispLen - 1);
						scale		= f0 / fmax * (dataLen - 1);
						
						for( j = 0; j < dispLen; j++ ) {
							d2				= Math.exp( (j * weight + offset) * log );
							d3				= d2 * scale;
							i				= ((int) d3);
							d2				= d3 % 1.0;
							f1				= data[ i ];
							d3				= f1 * (1.0 - d2);
							if( i+1 < dataLen ) {
								f1			= data[ i+1 ];
								f1			= (float) (d3 + f1 * d2);
							} else {
								f1			= (float) d3;
							}
							dispBuf[ j ]	= f1;
							if( f1 > max ) max = f1;
							if( f1 < min ) min = f1;
						}

					} else {
						for( i = 4096; i < dataLen; i <<= 1, decimate++ ) ;

						dispLen		= (dataLen >> decimate) + 1;
						dispBuf		= new float[ dispLen ];
					
						for( i = 0, j = 0, decimate = 1 << decimate; i < dataLen; i += decimate, j++ ) {
							f1				= data[ i ];
							dispBuf[ j ]	= f1;
							if( f1 > max ) max = f1;
							if( f1 < min ) min = f1;
						}
						
						fmin		= 0.0;
						fmax		= rec.afd.rate/2;
					}
					
					if( pr.bool[ PR_VLOG ] && !rec.vlog ) {
						for( j = 0; j < dispLen; j++ ) {
							dispBuf[ j ] = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, dispBuf[ j ])));
						}
						min = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, min )));
						max = (float) (decibelWeight * Math.log( Math.max( 1.0e-8, max )));
						if( (max - min) >= 40.0 ) {	// round to multiples of 6 dB for high dynamics
							max = (float) (Math.ceil( max / 6.0 ) * 6.0);
							min	= (float) (Math.floor( min / 6.0 ) * 6.0);
						} else if( (max - min) >= 20.0 ) {	// multiples of 3 dB for medium dynamics
							max = (float) (Math.ceil( max / 3.0 ) * 3.0);
							min	= (float) (Math.floor( min / 3.0 ) * 3.0);
						} else {	// multiples of 1 dB elsewise
							max = (float) (Math.ceil( max / 1.0 ) * 1.0);
							min	= (float) (Math.floor( min / 1.0 ) * 1.0);
						}
					} else {
						max = (float) (Math.ceil( max * 10.0 ) / 10.0);
						min = (float) (Math.floor( min * 10.0 ) / 10.0);
					}
					
					if( pr.bool[ PR_HLOG ] ) {
//						if( pr.bool[ PR_VLOG ]) {
//							rec.space = VectorSpace.createLogSpace( fmin, fmax, 1000.0, min, max, (min+max)/2, null, null, null, null );
//						} else {
							rec.space = VectorSpace.createLogLinSpace( fmin, fmax, fc, min, max, null, null, null, null );
//						}
					} else {
//						if( pr.bool[ PR_VLOG ]) {
//							rec.space = VectorSpace.createLinLogSpace( fmin, fmax, min, max, (min+max)/2, null, null, null, null );
//						} else {
							rec.space = VectorSpace.createLinSpace( fmin, fmax, min, max, null, null, null, null );
//						}
					}
					ggVectorDisplay.setMinMax( min, max );
					ggVectorDisplay.setVector( this, dispBuf );
					haxis.setSpace( rec.space );
					vaxis.setSpace( rec.space );
				}
			}
		}
	}


	protected void redrawCrosshair( MouseEvent e )
	{
		Dimension	dim	= ggVectorDisplay.getSize();
		int			x	= e.getX();
		int			y	= e.getY();
		int			propertyID;
		DataRecord	rec;
		String		xTxt, yTxt;
		int			dataLen;
		double		dx, dy;
		float[]		v;
		
		synchronized( properties ) {
			propertyID		= pr.intg[ PR_PROPERTY ];
			rec				= properties[ propertyID ];
			paintCrossHair	= false;

			if( (rec != null) && (rec.data != null) && !e.isAltDown() ) {

				v		= ggVectorDisplay.getVector();
				dataLen	= v.length;
	
				if( (x >= 0) && (y >= 0) && (x < dim.width) && (y < dim.height) ) {
					if( e.isShiftDown() ) {
						dy	= rec.space.vUnityToSpace( 1.0 - (double) y / (dim.height - 1) );
						dx	= rec.space.hUnityToSpace( (double) x / (dim.width - 1) );
					} else {
						x	= (int) ((double) x / (dim.width - 1) * (dataLen - 1) + 0.5);
						dy	= v[ x ];
						dx	= rec.space.hUnityToSpace( (double) x / (dataLen - 1) );
						y	= (int) ((1.0 - rec.space.vSpaceToUnity( dy )) * (dim.height - 1) + 0.5);
						x	= e.getX(); // (int) (rec.space.hSpaceToUnity( dx ) * (dim.width - 1) + 0.5);
					}
					lastPt	= new Point( x, y );

					if( pr.bool[ PR_VLOG ] ) {
						yTxt	= msgDecibel.format( new Object[] { new Double( dy )});
					} else if( !pr.bool[ PR_SUM ] && propertyID == PROP_PHSPECT ) {
						yTxt	= msgDegree.format( new Object[] { new Double( dy )});
					} else {
						yTxt	= msgPlain.format( new Object[] { new Double( dy )});
					}
					
					switch( propertyID ) {
					case PROP_ASPECT:
					case PROP_PSPECT:
					case PROP_PHSPECT:
						xTxt	= msgHertz.format( new Object[] { new Double( dx )});
						break;
					default:
					case PROP_ELONG:
						xTxt	= msgPlain.format( new Object[] { new Double( dx )});
						break;
					}
					lastTxt	= yTxt + " @ " + xTxt;
					paintCrossHair	= true;
				}
			}
		}
		ggVectorDisplay.repaint();
	}

// -------- interne DataRecord-Klasse --------

	protected static class DataRecord
	{
		private File			tempFile	= null;
		private float[]			data		= null;
//		private float			dataMin		= 0.0f;
//		private float			dataRange	= 0.0f;
		private boolean			sum			= false;
		private boolean			hlog		= false;
		private boolean			vlog		= false;
		private AudioFileDescr	afd			= null;
		private VectorSpace		space;
	} // class DataRecord
}
// class StatisticsDlg
