/*
 *  AudioFileFormatPane.java
 *  de.sciss.io package
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
 *		25-Jan-05	created from de.sciss.meloncillo.gui.AudioFileFormatPane
 *		07-Mar-05	implemented fromDescr()
 *		08-Sep-05	added automaticFileSuffix()
 *		10-Mar-06	extends SpringPanel ; supports numChannels ; moved to de.sciss.gui package
 */

package de.sciss.io;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import de.sciss.app.PreferenceNodeSync;
import de.sciss.gui.ComboBoxEditorBorder;
import de.sciss.gui.PathField;
import de.sciss.gui.PrefCheckBox;
import de.sciss.gui.PrefComboBox;
import de.sciss.gui.PrefParamField;
import de.sciss.gui.SpringPanel;
import de.sciss.gui.StringItem;
import de.sciss.util.Param;
import de.sciss.util.ParamSpace;

/**
 *  A multi component panel
 *  that provides gadgets for
 *  specification of the output
 *  format of an audio file,
 *  such as file format, resolution
 *  or sample rate. It implements
 *  the <code>PreferenceNodeSync</code>
 *  interface, allowing the automatic
 *  saving and recalling of its gadget's
 *  values from/to preferences.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 10-Mar-06
 *
 *  @see	de.sciss.app.PreferenceNodeSync
 *  @see	AudioFileDescr
 *
 *  @todo   sample rates should be user adjustable through a
 *			JComboBox with an editable field. this to-do has
 *			low priority since meloncillo is not really
 *			interested in audio files.
 *
 *	@synchronization	all methods should be invoked only
 *						in the event thread
 */
public class AudioFileFormatPane
extends SpringPanel
implements ItemListener, PreferenceNodeSync
{
	/**
	 *  Constructor-Flag : create file type gadget
	 */
	public static final int FORMAT		= 1 << 0;
	/**
	 *  Constructor-Flag : create sample encoding gadget
	 */
	public static final int ENCODING	= 1 << 1;
	/**
	 *  Constructor-Flag : create sample rate gadget
	 */
	public static final int RATE		= 1 << 2;
	/**
	 *  Constructor-Flag : create gain gadget
	 */
	public static final int GAIN		= 1 << 4;
	/**
	 *  Constructor-Flag : create normalize option
	 */
	public static final int NORMALIZE	= 1 << 5;
	/**
	 *  Constructor-Flag : create channel num gadgets
	 */
	public static final int CHANNELS	= 1 << 6;

	/**
	 *  Constructor-Flag : conventient combination
	 *  of <code>FORMAT</code>, <code>ENCODING</code> and <code>RATE</code>.
	 */
	public static final int FORMAT_ENCODING_RATE	= FORMAT | ENCODING | RATE;
	/**
	 *  Constructor-Flag : conventient combination
	 *  of <code>FORMAT</code>, <code>ENCODING</code>, <code>RATE</code>, <code>CHANNELS</code>.
	 */
	public static final int NEW_FILE_FLAGS			= FORMAT | ENCODING | RATE | CHANNELS;
	/**
	 *  Constructor-Flag : conventient combination
	 *  of <code>GAIN</code> and <code>NORMALIZE</code>.
	 */
	public static final int GAIN_NORMALIZE			= GAIN | NORMALIZE;
	
	private static final int[] BITSPERSMP			= { 16, 24, 32, 32 };   // idx corresp. to ENCODING_ITEMS
	private static final int[] ENCODINGS			= {						// idx corresp. to ENCODING_ITEMS
		AudioFileDescr.FORMAT_INT, AudioFileDescr.FORMAT_INT,
		AudioFileDescr.FORMAT_INT, AudioFileDescr.FORMAT_FLOAT
	};
	private static final StringItem[]   ENCODING_ITEMS  = {
		new StringItem( "int16", "16-bit int" ),
		new StringItem( "int24", "24-bit int" ),
		new StringItem( "int32", "32-bit int" ),
		new StringItem( "float32", "32-bit float" )
	};
//	private static final float[] RATES = {    // idx corresp. to RATE_ITEMS
//		32000.0f, 44100.0f, 48000.0f, 88200.0f, 96000.0f
//	};
	private static final StringItem[] RATE_ITEMS = {
		new StringItem( new Param( 32000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "32 kHz" ),
		new StringItem( new Param( 44100, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "44.1 kHz" ),
		new StringItem( new Param( 48000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "48 kHz" ),
		new StringItem( new Param( 88200, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "88.2 kHz" ),
		new StringItem( new Param( 96000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString(), "96 kHz" )
	};
	private static final int			DEFAULT_ENCODING	= 1;		// default int24
	private static final int			DEFAULT_RATE		= 1;		// default 44.1 kHz
	private static final Param			DEFAULT_GAIN		= new Param(
		0.0, ParamSpace.AMP | ParamSpace.REL | ParamSpace.DECIBEL );
	private static final boolean		DEFAULT_NORMALIZE	= true;		// default normalization
	
	// prefs keys
	private static final String		KEY_FORMAT		= "format";
	private static final String		KEY_ENCODING	= "encoding";
	private static final String		KEY_RATE		= "rate";
	private static final String		KEY_GAIN		= "gain";
	private static final String		KEY_NORMALIZE	= "normalize";
	private static final String		KEY_CHANNELS	= "channels";
	
	private JLabel				lbGainType;
	private PrefCheckBox		ggNormalize = null;
	private PrefComboBox		ggFormat	= null;
	private PrefComboBox		ggEncoding	= null;
	private PrefParamField		ggRate		= null;
	private JComboBox			ggRateCombo	= null;
	private PrefParamField		ggGain		= null;
	private JToolBar			pChan		= null;
	private ButtonGroup			chanGroup	= null;
	private JToggleButton		ggMono		= null;
	private JToggleButton		ggStereo	= null;
	private JToggleButton		ggMulti		= null;
	private PrefParamField		ggChanNum	= null;
	
	private List				ggPaths		= null;		// lazy; set with automaticFileSuffix method
	
	private int					flags		= 0;
	
	private SpringPanel			pEnc		= null;
	private SpringPanel			pGain		= null;
	
	private Preferences			prefs		= null;
	
	public AudioFileFormatPane()
	{
		super();
	}

	/**
	 *  Construct a new AudioFileFormatPane with the
	 *  shown components specified by the given flags.
	 *
	 *  @param  flags   a bitwise OR combination of
	 *					gadget creation flags such as FORMAT or GAIN_NORMALIZE
	 */
	public AudioFileFormatPane( int flags )
	{
		this();
		setFlags( flags );
	}
	
	public void setFlags( int flags )
	{
		final int		flagsAdded		= flags & ~this.flags;
		final int		flagsRemoved	= this.flags & ~flags;
	
		StringItem[]	items;

		if( (flagsRemoved & FORMAT_ENCODING_RATE) != 0 ) {
			if( (flagsRemoved & FORMAT) != 0 ) {
				ggFormat.removeItemListener( this );
				if( prefs != null ) ggFormat.setPreferences( null, null );
				pEnc.remove( ggFormat );
				ggFormat	= null;
			}
			if( (flagsRemoved & ENCODING) != 0 ) {
				if( prefs != null ) ggEncoding.setPreferences( null, null );
				pEnc.remove( ggEncoding );
				ggEncoding	= null;
			}
			if( (flagsRemoved & RATE) != 0 ) {
				if( prefs != null ) ggRate.setPreferences( null, null );
				pEnc.remove( ggRateCombo );
				ggRateCombo	= null;
				ggRate		= null;
			}
			if( pEnc.getComponentCount() == 0 ) {
				remove( pEnc );
				pEnc = null;
			}
		}
		
		if( (flagsRemoved & CHANNELS) != 0 ) {
			ggMulti.removeItemListener( this );
			if( prefs != null ) ggChanNum.setPreferences( null, null );
			remove( pChan );
			pChan.remove( ggMono );
			pChan.remove( ggStereo );
			pChan.remove( ggMulti );
			pChan.remove( ggChanNum );
			chanGroup.remove( ggMono );
			chanGroup.remove( ggStereo );
			chanGroup.remove( ggMulti );
			ggMono		= null;
			ggStereo	= null;
			ggMulti		= null;
			ggChanNum	= null;
			pChan		= null;
			chanGroup	= null;
		}
		
		if( (flagsRemoved & GAIN_NORMALIZE) != 0 ) {
			if( (flagsRemoved & GAIN) != 0 ) {
				if( prefs != null ) ggGain.setPreferences( null, null );
				pGain.remove( ggGain );
				ggGain	= null;
			}
			if( (flagsRemoved & NORMALIZE) != 0 ) {
				ggNormalize.removeItemListener( this );
				if( prefs != null ) ggNormalize.setPreferences( null, null );
				pGain.remove( ggNormalize );
				ggNormalize	= null;
			}
			if( pGain.getComponentCount() == 0 ) {
				remove( pGain );
				pGain = null;
			}
		}

		if( (flagsAdded & FORMAT_ENCODING_RATE) != 0 ) {
			if( pEnc == null ) {
				pEnc = new SpringPanel( 4, 2, 4, 2 );
				gridAdd( pEnc, 0, 0, -1, 1 );
			}
			if( (flagsAdded & FORMAT) != 0 ) {
				ggFormat	= new PrefComboBox();
				items		= AudioFileDescr.getFormatItems();
				for( int i = 0; i < items.length; i++ ) {
					ggFormat.addItem( items[ i ]);
				}
				ggFormat.setSelectedIndex( 0 );
				if( prefs != null ) ggFormat.setPreferences( prefs, KEY_FORMAT );
				ggFormat.addItemListener( this );
				pEnc.gridAdd( ggFormat, 0, 0 );
			}
			if( (flagsAdded & ENCODING) != 0 ) {
				ggEncoding	= new PrefComboBox();
				items		= ENCODING_ITEMS;
				for( int i = 0; i < items.length; i++ ) {
					ggEncoding.addItem( items[ i ]);
				}
				ggEncoding.setSelectedIndex( DEFAULT_ENCODING );
				if( prefs != null ) ggEncoding.setPreferences( prefs, KEY_ENCODING );
				pEnc.gridAdd( ggEncoding, 1, 0 );
			}
			if( (flagsAdded & RATE) != 0 ) {				
				ggRateCombo = new JComboBox();
				ggRate		= new PrefParamField();
				ggRate.addSpace( ParamSpace.spcFreqHertz );
				items		= RATE_ITEMS;
				for( int i = 0; i < items.length; i++ ) {
					ggRateCombo.addItem( items[ i ]);
				}
//				ggRate.setBackground( Color.white );
//final javax.swing.plaf.basic.BasicComboBoxRenderer bcbr = new javax.swing.plaf.basic.BasicComboBoxRenderer();
ggRate.setBorder( new ComboBoxEditorBorder() );
//ggRate.setMaximumSize( ggRate.getPreferredSize() );
				ggRateCombo.setEditor( ggRate );
				ggRateCombo.setEditable( true );
				ggRateCombo.setSelectedIndex( DEFAULT_RATE );
				if( prefs != null ) ggRate.setPreferences( prefs, KEY_RATE );
				pEnc.gridAdd( ggRateCombo, 2, 0 );
			}
			pEnc.makeCompactGrid();
		}

		if( (flagsAdded & CHANNELS) != 0 ) {
			pChan		= new JToolBar();
			pChan.setFloatable( false );
			chanGroup	= new ButtonGroup();
			ggMono		= new JToggleButton( getResourceString( "buttonMono" ));
			ggMono.setSelected( true );
			chanGroup.add( ggMono );
			pChan.add( ggMono );
			ggStereo	= new JToggleButton( getResourceString( "buttonStereo" ));
			chanGroup.add( ggStereo );
			pChan.add( ggStereo );
			ggChanNum	= new PrefParamField();
			ggChanNum.addSpace( new ParamSpace( 0, 0xFFFF, 1, 0, 0, 4, ParamSpace.NONE ));
			ggChanNum.setEnabled( false );
			ggMulti		= new JToggleButton( getResourceString( "buttonMultichannel" ));
			if( prefs != null ) ggChanNum.setPreferences( prefs, KEY_CHANNELS );
			ggMulti.addItemListener( this );
			chanGroup.add( ggMulti );
			pChan.add( ggMulti );
			pChan.add( ggChanNum );
			
			gridAdd( pChan, 0, 1, -1, 1 );
		}

		if( (flagsAdded & GAIN_NORMALIZE) != 0 ) {
			if( pGain == null ) {
				pGain = new SpringPanel( 4, 2, 4, 2 );
				gridAdd( pGain, 0, 2, -1, 1 );
			}
			if( (flagsAdded & GAIN) != 0 ) {
				ggGain  = new PrefParamField();
				ggGain.addSpace( ParamSpace.spcAmpDecibels );
				ggGain.addSpace( ParamSpace.spcAmpPercentF );
				ggGain.setValue( DEFAULT_GAIN );
				pGain.gridAdd( ggGain, 0, 0 );
				lbGainType  = new JLabel();
				pGain.gridAdd( lbGainType, 1, 0 );
			}
			if( (flagsAdded & NORMALIZE) != 0 ) {
				ggNormalize = new PrefCheckBox( getResourceString( "labelNormalize" ));
				ggNormalize.setSelected( DEFAULT_NORMALIZE );
				ggNormalize.addItemListener( this );
				pGain.gridAdd( ggNormalize, 2, 0 );
				if( ggGain != null ) setGainLabel();
			}
			pGain.makeCompactGrid();
		}
		
		if( flags != this.flags ) {
			this.flags = flags;
			makeCompactGrid();
		}
	}
	
	public int getFlags()
	{
		return flags;
	}
	
	private String getResourceString( String key )
	{
		return IOUtil.getResourceString( key );
	}
	
	/**
	 *  Copy the internal state of
	 *  the <code>AudioFileFormatPane</code> into the
	 *  <code>AudioFileDescr</code> object. This will
	 *  fill in the <code>type</code>,
	 *  <code>bitsPerSample</code>, <code>sampleFormat</code>,
	 *  <code>rate</code> and <code>channels</code> fields,
	 *  provided that the pane was specified
	 *  to contain corresponding gadgets
	 *
	 *  @param  target  the description whose
	 *					format values are to be overwritten.
	 */
	public void toDescr( AudioFileDescr target )
	{
		if( ggFormat != null ) {
			target.type = ggFormat.getSelectedIndex();
		}
		if( ggEncoding != null ) {
			target.bitsPerSample	= BITSPERSMP[ ggEncoding.getSelectedIndex() ];
			target.sampleFormat		= ENCODINGS[ ggEncoding.getSelectedIndex() ];
		}
		if( ggRate != null ) {
			target.rate				= ggRate.getValue().val;
		}
		if( chanGroup != null ) {
			if( ggMono.isSelected() ) {
				target.channels		= 1;
			} else if( ggStereo.isSelected() ) {
				target.channels		= 2;
			} else {
				target.channels		= (int) ggChanNum.getValue().val;
			}
		}
	}
	
	/**
	 *  Return the value of the
	 *  gain gadget (in decibels).
	 *  If the pane was not created without
	 *  a dedicated gain gadget, this
	 *  method returns 0.0.
	 *
	 *  @return		the pane's gain setting
	 *				or 0.0 if no gain gadget exists.
	 */
	public double getGain()
	{
		if( ggGain != null ) {
			return ggGain.getTranslator().translate( ggGain.getValue(), ParamSpace.spcAmpDecibels ).val;
		} else {
			return 0.0;
		}
	}
	
	/**
	 *	Returns a text representation of the encoding
	 *	(integer/floating point and bit depth). This String
	 *	is compatible with SuperCollider's <code>/b_write</code> command.
	 *
	 *	@return	encoding string, such as <code>&quot;int16&quot;</code>
	 */
	public String getEncodingString()
	{
		return ((StringItem) ggEncoding.getSelectedItem()).getKey();
	}

	/**
	 *	Returns a text representation of the file format type.
	 *	This String is compatible with SuperCollider's <code>/b_write</code> command.
	 *
	 *	@return	file format string, such as <code>&quot;aiff&quot;</code>
	 */
	public String getFormatString()
	{
		return ((StringItem) ggFormat.getSelectedItem()).getKey();
	}
	
	/**
	 *  Return the state of the 'normalized'
	 *  checkbox of the pane.
	 *
	 *  @return		<code>true</code> if the pane's
	 *				'normalized' checkbox was checked,
	 *				<code>false</code> otherwise or if no checkbox
	 *				gadget exists.
	 */
	public boolean getNormalized()
	{
		if( ggNormalize != null ) {
			return ggNormalize.isSelected();
		} else {
			return false;
		}
	}
	
	/**
	 *	Registers a <code>PathField</code> to
	 *	be updated upon format switches.
	 *	When the user selects a different format,
	 *	the path's suffix will be updated accordingly.
	 *
	 *	@param	ggPath	the path field to update
	 *					or <code>null</code> to stop
	 *					updating.
	 */
	public void automaticFileSuffix( PathField ggPath )
	{
		if( ggPaths == null ) ggPaths = new ArrayList();
		ggPaths.add( ggPath );
		updateFileSuffix();
	}
	
	/**
	 *  Copy a sound format from the given
	 *  <code>AudioFileDescr</code> to the
	 *  corresponding gadgets in the pane.
	 */
	public void fromDescr( AudioFileDescr source )
	{
		if( ggFormat != null ) {
			ggFormat.setSelectedIndex( source.type );
		}
		if( ggEncoding != null ) {
			for( int i = 0; i < ENCODINGS.length; i++ ) {
				if( (BITSPERSMP[ i ] == source.bitsPerSample) &&
					(ENCODINGS[ i ] == source.sampleFormat) ) {

					ggEncoding.setSelectedIndex( i );
					break;
				}
			}
		}
		if( ggRate != null ) {
//			for( int i = 0; i < RATES.length; i++ ) {
//				if( RATES[ i ] == source.rate ) {
//					ggRate.setSelectedIndex( i );
//					break;
//				}
//			}
			ggRate.setValue( new Param( source.rate, ParamSpace.spcFreqHertz.unit ));
		}
		if( chanGroup != null ) {
			switch( source.channels ) {
			case 1:
				chanGroup.setSelected( ggMono.getModel(), true );
				ggChanNum.setEnabled( false );
				break;
			case 2:
				chanGroup.setSelected( ggStereo.getModel(), true );
				ggChanNum.setEnabled( false );
				break;
			default:
				chanGroup.setSelected( ggMulti.getModel(), true );
				ggChanNum.setEnabled( true );
				break;
			}
		}
	}
	
	// update the gain's label when the normalize checkbox is toggled
	private void setGainLabel()
	{
		boolean normalize   = (ggNormalize != null) && ggNormalize.isSelected();
		
		lbGainType.setText( getResourceString( normalize ? "labelDBHeadroom" : "labelDBGain" ));
	}
	
	// sync's a path field's path extension
	// with the selected encoding
	private void updateFileSuffix()
	{
		if( ggFormat == null ) return;
		
		final String		suffix	= AudioFileDescr.getFormatSuffix( ggFormat.getSelectedIndex() );
		File				path, newPath;
		PathField			ggPath;
		
		if( ggPaths != null ) {
			for( int i = 0; i < ggPaths.size(); i++ ) {
				ggPath	= (PathField) ggPaths.get( i );
				path	= ggPath.getPath();
				newPath	= IOUtil.setFileSuffix( path, suffix );

				if( newPath != path ) {	// IOUtil returns same ref in case of equality
					ggPath.setPath( newPath );
				}
			}
		}
	}
	
	// we're listening to the normalize checkbox + format combo + multi-channel
	public void itemStateChanged( ItemEvent e )
	{
		if( e.getSource() == ggNormalize ) {
			setGainLabel();		
		} else if( e.getSource() == ggFormat ) {
			updateFileSuffix();
		} else if( e.getSource() == ggMulti ) {
			final boolean isMulti = ggMulti.isSelected();
			ggChanNum.setEnabled( isMulti );
			if( isMulti ) ggChanNum.requestFocusInWindow(); // focusNumber();
		}
	}

// --------------------- PreferenceNodeSync interface ---------------------
	
	public void setPreferences( Preferences prefs )
	{
		if( ggFormat != null ) {
			ggFormat.setPreferences( prefs, KEY_FORMAT );
		}
		if( ggEncoding != null ) {
			ggEncoding.setPreferences( prefs, KEY_ENCODING );
		}
		if( ggRate != null ) {
			ggRate.setPreferences( prefs, KEY_RATE );
		}
		if( ggGain != null ) {
			ggGain.setPreferences( prefs, KEY_GAIN );
		}
		if( ggNormalize != null ) {
			ggNormalize.setPreferences( prefs, KEY_NORMALIZE );
		}
		if( ggChanNum != null ) {
			ggChanNum.setPreferences( prefs, KEY_CHANNELS );
			if( prefs != null ) {
				final int numCh = prefs.getInt( KEY_CHANNELS, 1 );
				switch( numCh ) {
				case 1:
					chanGroup.setSelected( ggMono.getModel(), true );
					ggChanNum.setEnabled( false );
					break;
				case 2:
					chanGroup.setSelected( ggStereo.getModel(), true );
					ggChanNum.setEnabled( false );
					break;
				default:
					chanGroup.setSelected( ggMulti.getModel(), true );
					ggChanNum.setEnabled( true );
					break;
				}
			}
		}
	}
}
