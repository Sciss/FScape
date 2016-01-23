/*
 *  ConvertDlg.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		07-Jan-05	removed deprecated soundfile methods
 *		21-May-05	fixed bug in 16bit TIFF mode
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

import de.sciss.gui.AbstractWindowHandler;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *	Processing module for converting different
 *	data representations, e.g. sound file,
 *	spectral file, image file.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 04-Jan-09
 */
public class ConvertDlg
extends ModulePanel
{
// -------- public Variablen --------

//	private static final int OUTPUT_FFT			= 0;
//	private static final int OUTPUT_DFT			= 1;
//	private static final int OUTPUT_WAVELET		= 2;

// -------- private Variablen --------

	private static	PropertyArray	static_pr		= null;
	private static	Presets			static_presets	= null;
	private			PropertyGUI		settingsGUI;

	private int inType	= TYPE_UNKNOWN;
	private int outType	= TYPE_UNKNOWN;

	private static final int TYPE_UNKNOWN = -1;
	private static final int TYPE_SOUND   = 0;
	private static final int TYPE_SPECT   = 1;
	private static final int TYPE_IMAGE   = 2;
		
	// Fehlermeldungen
	private static final String	ERR_CONVERSION	= "Unsupported conversion";

	// Properties (defaults)
	private static final int PR_INPUTFILE			= 0;		// pr.text
	private static final int PR_OUTPUTFILE			= 1;
	private static final int PR_OUTPUTTYPE			= 0;		// pr.intg
	private static final int PRS_BANDS				= 1;
	private static final int PRS_BANDWIDTH			= 2;
	private static final int PRS_FRAMELEN			= 3;
	private static final int PRS_TRANSFORM			= 4;
//	private static final int PRS_FILTER				= 5;
	private static final int PRS_WINDOW				= 6;
	private static final int PRS_OVERLAP			= 7;
	private static final int PRS_COLORMODE			= 8;
	private static final int PRS_OVERHEAD			= 9;
	private static final int PRS_CHANNELS			= 10;
	private static final int PR_OUTPUTRES			= 11;
	private static final int PR_OUTPUTRATE			= 12;
	private static final int PR_ADJUSTGAIN			= 0;		// pr.bool
	private static final int PR_GAIN				= 0;		// pr.para
	private static final int PRS_NOISEFLOOR			= 1;

	private static final int PRS_TRANSFORM_FFT		= 0;
	private static final int PRS_TRANSFORM_DFT		= 1;
	private static final int PRS_TRANSFORM_FWT		= 2;

	private static final int PRS_FILTER_DAUB4		= 0;

//	private static final int PRS_WINDOW_HAMMING		= 0;	// CORRESPONDIERT mit Filter.WIN_... !!
//	private static final int PRS_WINDOW_BLACKMAN	= 1;
//	private static final int PRS_WINDOW_KAISER4		= 2;
	private static final int PRS_WINDOW_KAISER5		= 3;
//	private static final int PRS_WINDOW_KAISER6		= 4;
//	private static final int PRS_WINDOW_KAISER8		= 5;

//	private static final int PRS_OVERLAP_400		= 0;
//	private static final int PRS_OVERLAP_200		= 1;
	private static final int PRS_OVERLAP_100		= 2;
//	private static final int PRS_OVERLAP_50			= 3;

	private static final int PRS_COLORMODE_CHAN		= 0;
	private static final int PRS_COLORMODE_POLAR	= 1;

	private static final int PRS_OVERHEAD_TILE		= 0;
//	private static final int PRS_OVERHEAD_FILES		= 1;

//	private static final int PRS_CHANNELS_UNTOUCHED	= 0;
//	private static final int PRS_CHANNELS_1			= 1;
//	private static final int PRS_CHANNELS_2			= 2;
	private static final int PRS_CHANNELS_3			= 3;

	private static final double freqScales[]	= { 1.0036166659754628, 1.0048251256952678,		// 1/16, 1/12 semitone
													1.007246412223704, 1.0145453349375237,		// 1/8, 1/4 semitone
													1.029302236643492, 1.0594630943592953 };	// 1/2, 1/1 semitone

	private static final String PRN_INPUTFILE		= "InputFile";
	private static final String PRN_OUTPUTFILE		= "OutputFile";
	private static final String PRN_OUTPUTTYPE		= "OutputType";
	private static final String PRN_OUTPUTRES		= "OutputRes";
	private static final String PRN_OUTPUTRATE		= "OutputRate";

	private static final String PRN_BANDS			= "Bands";
	private static final String PRN_BANDWIDTH		= "Bandwidth";
	private static final String PRN_FRAMELEN		= "FrameLen";
	private static final String PRN_TRANSFORM		= "Transform";
	private static final String PRN_FILTER			= "Filter";
	private static final String PRN_WINDOW			= "Window";
	private static final String PRN_OVERLAP			= "Overlap";
	private static final String PRN_COLORMODE		= "ColorMode";
	private static final String PRN_OVERHEAD		= "Overhead";
	private static final String PRN_CHANNELS		= "Channels";
	private static final String PRN_NOISEFLOOR		= "NoiseFloor";
	private static final String PRN_ADJUSTGAIN		= "AdjustGain";

	private static final String	prText[]		= { "", "" };
	private static final String	prTextName[]	= { PRN_INPUTFILE, PRN_OUTPUTFILE };
	private static final int		prIntg[]	= { 0, 1, 1, 1, PRS_TRANSFORM_FFT, PRS_FILTER_DAUB4,
													PRS_WINDOW_KAISER5, PRS_OVERLAP_100,
													PRS_COLORMODE_CHAN, PRS_OVERHEAD_TILE,
													PRS_CHANNELS_3, 0, 0 };
	private static final String	prIntgName[]	= { PRN_OUTPUTTYPE, PRN_BANDS, PRN_BANDWIDTH, PRN_FRAMELEN, PRN_TRANSFORM, PRN_FILTER,
													PRN_WINDOW, PRN_OVERLAP, PRN_COLORMODE,
													PRN_OVERHEAD, PRN_CHANNELS, PRN_OUTPUTRES, PRN_OUTPUTRATE };
	private static final boolean	prBool[]	= { false };
	private static final String	prBoolName[]	= { PRN_ADJUSTGAIN };
	private static final Param	prPara[]		= { null, null };
	private static final String	prParaName[]	= { PRN_GAIN, PRN_NOISEFLOOR };

	private static final int GG_INPUTFILE		= GG_OFF_PATHFIELD	+ PR_INPUTFILE;
	private static final int GG_OUTPUTFILE		= GG_OFF_PATHFIELD	+ PR_OUTPUTFILE;
	private static final int GG_OUTPUTTYPE		= GG_OFF_CHOICE		+ PR_OUTPUTTYPE;
	private static final int GG_OUTPUTRES		= GG_OFF_CHOICE		+ PR_OUTPUTRES;
	private static final int GG_OUTPUTRATE		= GG_OFF_CHOICE		+ PR_OUTPUTRATE;
	private static final int GG_SETTINGS		= GG_OFF_OTHER		+ 0;
	
	// GUI Types
	private static final int GUI_ANALYSIS		= 0;		// Waveform => Spect
	private static final int GUI_SYNTHESIS		= 1;		// Spect => Waveform
	private static final int GUI_TOIMAGE		= 2;		// Spect => Image
	private static final int GUI_FROMIMAGE		= 3;		// Image => Spect

// -------- public Methoden --------

	/**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 *
	 *	@param parent	aufrufendes Fenster
	 */
	public ConvertDlg()
	{
		super( "Convert files" );
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
			static_pr.para		= prPara;
			static_pr.para[ PR_GAIN ]		= new Param( 0.0, Param.DECIBEL_AMP );
			static_pr.para[ PRS_NOISEFLOOR ]= new Param( -120.0, Param.DECIBEL_AMP );
			static_pr.paraName	= prParaName;
//			static_pr.superPr	= DocumentFrame.static_pr;
		}
		// default preset
		if( static_presets == null ) {
			static_presets = new Presets( getClass(), static_pr.toProperties( true ));
		}
		presets	= static_presets;
		pr 		= (PropertyArray) static_pr.clone();

	// -------- GUI bauen --------

		GridBagConstraints	con;

		PathField	ggInputFile, ggOutputFile;
		PathField[] ggInputs;
		int[][]		handledTypes;

		JScrollPane		ggSettings;

		gui				= new GUISupport();
		con				= gui.getGridBagConstraints();
		con.insets		= new Insets( 1, 2, 1, 2 );

		ItemListener il = new ItemListener() {
			public void itemStateChanged( ItemEvent e )
			{
				int				ID			= gui.getItemID( e );

				switch( ID ) {
				case ConvertDlg.GG_OUTPUTTYPE:
					outputChanged();
					break;

				default:
					break;
				}
			}
		};
		
		PathListener pathL = new PathListener() {
			public void pathChanged( PathEvent e )
			{
				int	ID	= gui.getItemID( e );

				switch( ID ) {
				case ConvertDlg.GG_INPUTFILE:
					setInput( ((PathField) e.getSource()).getPath().getPath() );
					outputChanged();
					break;
				}
			}
		};
		
		AdjustmentListener al = new AdjustmentListener() {
			public void adjustmentValueChanged( AdjustmentEvent e ) {}
		};
		
	// -------- I/O-Gadgets --------
		con.fill		= GridBagConstraints.BOTH;
		con.gridwidth	= GridBagConstraints.REMAINDER;
	gui.addLabel( new GroupLabel( "Input / Output", GroupLabel.ORIENT_HORIZONTAL,
								  GroupLabel.BRACE_NONE ));

		ggInputFile		= new PathField( PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD,
										 "Select input file" );
		handledTypes	= new int[3][];
		handledTypes[0] = GenericFile.TYPES_SOUND;
		handledTypes[1] = GenericFile.TYPES_SPECT;
		handledTypes[2] = GenericFile.TYPES_IMAGE;
		ggInputFile.handleTypes( handledTypes );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Input file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggInputFile, GG_INPUTFILE, pathL );

		ggOutputFile	= new PathField( PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD +
										 PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD, "Select output file" );
		ggOutputFile.handleTypes( handledTypes );
		ggInputs		= new PathField[ 1 ];
		ggInputs[ 0 ]	= ggInputFile;
		ggOutputFile.deriveFrom( ggInputs, "$D0$F0$E" );
		con.gridwidth	= 1;
		con.weightx		= 0.1;
		gui.addLabel( new JLabel( "Output file", SwingConstants.RIGHT ));
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 0.9;
		gui.addPathField( ggOutputFile, GG_OUTPUTFILE, pathL );
		gui.registerGadget( ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE );
		ggOutputFile.getTypeGadget().addItemListener( il );
		gui.registerGadget( ggOutputFile.getResGadget(),  GG_OUTPUTRES );
		gui.registerGadget( ggOutputFile.getRateGadget(), GG_OUTPUTRATE );
		
	// -------- Conversion-Parameter --------

		gui.addLabel( new JLabel( "Conversion settings", SwingConstants.CENTER ));
		con.fill		= GridBagConstraints.BOTH;
		ggSettings		= new JScrollPane(); // ( ScrollPane.SCROLLBARS_AS_NEEDED );
		ggSettings.setPreferredSize( new Dimension( 256, 256 ));	// XXX
		con.gridwidth	= GridBagConstraints.REMAINDER;
		con.weightx		= 1.0;
		con.weighty		= 1.0;
		gui.addScrollPane( ggSettings, GG_SETTINGS, al );

		initGUI( this, FLAGS_PRESETS | FLAGS_PROGBAR, gui );

//		outputChanged();
	}

	/*
	 *	Settings-GUI in Abhaengigkeit von der jeweiligen Konvertierungs-Art
	 */
	protected PropertyGUI createGUI( int type )
	{
		PropertyGUI	g;
	
		String	gain		= "cbAdjust gain,actrue|100|en,acfalse|100|di,pr"+PRN_ADJUSTGAIN+";"+
							  "pf"+Constants.decibelAmpSpace+",id100,pr"+PRN_GAIN;
		String	transform	= "lbTransform;ch,pr"+PRN_TRANSFORM+
							    ",ac"+PRS_TRANSFORM_FFT+"|21|di|22|di|23|di"+
							    ",ac"+PRS_TRANSFORM_DFT+"|21|di|22|en|23|en"+
							    ",ac"+PRS_TRANSFORM_FWT+"|21|en|22|di|23|di,"+
							  "itFast Fourier,itDiscrete Fourier (log. Scale),itFast Wavelet\n"+
							  "lbFilter;ch,id21,pr"+PRN_FILTER+",itDaubechies 4\n";
		String	window		= "lbWindow;ch,pr"+PRN_WINDOW+",itHamming,itBlackman,itKaiser \u03B2=4,itKaiser \u03B2=5,itKaiser \u03B2=6,itKaiser \u03B2=8\n";
		String	overlap		= "lbOverlap;ch,pr"+PRN_OVERLAP+",it1x,it2x,it4x,it8x,it16x\n";
		String	bands		= "lbBands;ch,pr"+PRN_BANDS+",it4096,it2048,it1024,it512,it256,it128,it64,it32\n"+
							  "lbBandwidth [semi];ch,id22,pr"+PRN_BANDWIDTH+",it1/16,it1/12,it1/8,it1/4,it1/2,it1/1\n"+
							  "lbFrame length;ch,id23,pr"+PRN_FRAMELEN+",it4096,it2048,it1024,it512,it256,it128,it64,it32\n";
		String	smpRate		= "";
		String	smpRes		= "";
		String	image		= "lbSeparate colors for;ch,pr"+PRN_COLORMODE+",itEach channel,itAmp + Phase\n"+
							  "lbHandle overhead;ch,pr"+PRN_OVERHEAD+",itBy horizontal tiling,"+
							  "itBy creating separate files\n"+
							  "lbChannels;ch,pr"+PRN_CHANNELS+",itLeave untouched,it1,it2,it3\n"+
							  "lbNoisefloor;pf"+Constants.decibelAmpSpace+",pr"+PRN_NOISEFLOOR+"\n";
		String	image2		= "";

		String	guiDescr	= null;

		switch( type ) {
		case GUI_ANALYSIS:
			guiDescr	= transform + bands + window + overlap + gain;
			break;
		case GUI_SYNTHESIS:
			guiDescr	= transform + window + smpRes + gain;
			break;
		case GUI_TOIMAGE:
			guiDescr	= image + image2 + gain;
			break;
		case GUI_FROMIMAGE:
			guiDescr	= transform + smpRate + overlap + image + gain;
			break;
		default:
			return null;
		}

	 	g = new PropertyGUI( guiDescr );
	 	g.setType( type );
	 	return g;
	}
	
	/**
	 *	Liefert PropertyGUI der Settings-Gadgets
	 */
	public PropertyGUI getSettingsGUI()
	{
		return settingsGUI;
	}

	/**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
	public void fillGUI()
	{
		super.fillGUI();
		super.fillGUI( gui );

		PropertyGUI 	g = getSettingsGUI();

		setInput( pr.text[ PR_INPUTFILE ]);
		outputChanged();

		if( g != null ) {
			g.fillGUI( pr );		// load values
		}
	}

	/**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
	public void fillPropertyArray()
	{
		super.fillPropertyArray();
		super.fillPropertyArray( gui );

		PropertyGUI		g	= getSettingsGUI();
		
		if( g != null ) {
			g.fillPropertyArray( pr );		// save values
		}
	}
	
	/**
	 *	Wechselt das Settings-GUI
	 *	bei ungueltigem type bleibt das Panel leer
	 */
	public void setSettingsGUI( int type )
	{
		Dimension	dim;
		JScrollPane	ggSettings = (JScrollPane) gui.getItemObj( GG_SETTINGS );
	
		if( ggSettings == null ) return;
	
		settingsGUI = createGUI( type );
		if( settingsGUI != null ) {
			settingsGUI.fillGUI( getPropertyArray() );
			dim = ggSettings.getSize();
			settingsGUI.setSize( dim.width, dim.height );
//			GUIUtil.setDeepFont( settingsGUI, Main.getFont( Main.FONT_GUI ));
			ggSettings.setViewportView( settingsGUI );
//			setFont( getFont() );
			AbstractWindowHandler.setDeepFont( settingsGUI );
		} else {
			ggSettings.setViewportView( null );
		}
	}	

	/**
	 *	Neues Inputfile setzen
	 */
	public void setInput( String fname )
	{
		GenericFile	f;
		int			mode;

	// ---- Typ ermitteln ----
		inType		= TYPE_UNKNOWN;

		try {	// ---- Spectral file ----
			f		= new GenericFile( fname, GenericFile.MODE_INPUT );
			mode	= f.mode & GenericFile.MODE_TYPEMASK;
			if( Util.isValueInArray( mode, GenericFile.TYPES_IMAGE )) {
				inType	= TYPE_IMAGE;
			} else if( Util.isValueInArray( mode, GenericFile.TYPES_SOUND )) {
				inType	= TYPE_SOUND;
			} else if( Util.isValueInArray( mode, GenericFile.TYPES_SPECT )) {
				inType	= TYPE_SPECT;
			} else {
				inType	= TYPE_UNKNOWN;
			}
			f.close();
		
		} catch( IOException e1 ) {}		
	}

	/**
	 *	automatischer Output Name bei geaendertem Input oder Output-Type
	 *	veranlasst Aktivierung / Deaktivierung des Convert-JButtons
	 *	und passt das Settings-Panel an die Einstellung an
	 *	(inType muss korrekt sein dafuer!)
	 */
	public void outputChanged()
	{
		int				mode;
		int				guiType			= -1;
		PathField		ggOutput		= (PathField) gui.getItemObj( GG_OUTPUTFILE );
		
		outType			= TYPE_UNKNOWN;
		if( ggOutput != null ) {
			mode	= ggOutput.getType();
			if( Util.isValueInArray( mode, GenericFile.TYPES_IMAGE )) {
				outType	= TYPE_IMAGE;
			} else if( Util.isValueInArray( mode, GenericFile.TYPES_SOUND )) {
				outType	= TYPE_SOUND;
			} else if( Util.isValueInArray( mode, GenericFile.TYPES_SPECT )) {
				outType	= TYPE_SPECT;
			} else {
				outType	= TYPE_UNKNOWN;
			}
		}
		switch( outType ) {
		case TYPE_SPECT:
			if( inType == TYPE_SOUND ) {
				guiType		= GUI_ANALYSIS;
			} else if( inType == TYPE_IMAGE ) {
				guiType		= GUI_FROMIMAGE;
			}
			break;
		case TYPE_SOUND:
			if( inType == TYPE_SPECT ) {
				guiType		= GUI_SYNTHESIS;
			}
			break;
		case TYPE_IMAGE:
			if( inType == TYPE_SPECT ) {
				guiType		= GUI_TOIMAGE;
			}
			break;
		default:
			break;
		}

	// ---- Conversion Settings ----
		settingsGUI = getSettingsGUI();
		if( (settingsGUI == null) || (settingsGUI.getType() != guiType) ) {
			setSettingsGUI( guiType );
		}
	}
	
// -------- Processor Interface --------
		
	protected void process()
	{
		int 	m;
		
		// io
		Object				inF				= null;
		Object				outF			= null;
		Object				inStream		= null;
		Object				outStream		= null;
		SpectFrame			frame			= null;
		PathField			ggOutput;

		// Convert
		float				progress		= 0.0f;
		int					chanNum			= pr.intg[ PRS_CHANNELS ];
		float				smpRate;				// = smpRates[ pr.intg[ PRS_SMPRATE ]];
		int					frames			= 0;
		int					bands			= (1 << (12 - pr.intg[ PRS_BANDS ])) + 1;
		int					bandsD;
		int					frameLen		= (1 << (12 - pr.intg[ PRS_FRAMELEN ]));
		int					overlap			= pr.intg[ PRS_OVERLAP ];
		float				loFreq			= 0.0f;
		float				hiFreq;
		final float			masterTune		= 440.0f;		// XXX let user pick it
		int					shorty;
		float				floaty;
		float				gain			= 1.0f;			// gain abs amp
		final Param			ampRef			= new Param( 1.0, Param.ABS_AMP );	// transform-Referenz
		
		// Image conversion
		byte[]				row				= null;
		int					tile;
		final float			phasePerByte	= (float) (Constants.PI2 / 255.0);
		final float			phasePerWord	= (float) (Constants.PI2 / 65535.0);
		final float			ampPerByte		= (float) ((20.0 * 255.0 / pr.para[ PRS_NOISEFLOOR ].val) / Constants.ln10);
		final float			ampPerWord		= (float) ((20.0 * 65535.0 / pr.para[ PRS_NOISEFLOOR ].val) / Constants.ln10);
		int					smpPerPixel		= 1;	// will be filled with ImageStream-value!
		
		// FFT, DFT, FWT
		double				freqScale		= 0f;	// DFT: Freq.factor von einem Band zum naechsthoeheren
		float				img, real;
		float[]				smpData			= null;
		float[]				smpData2		= null;	// gewindowte Version
		float[][]			cos				= null;	// 1. Dimension = Freq-Offset, 2. Dimension = Zeit-Offset
		float[][]			sin				= null;
		float[]				fCos, fSin;			// cos/sin mit aufgeloester erster Dimension
		float[]				win				= null;	// Window-Funktion
		
		float[][]			tempBuf			= null;
	
		long				audioOutLength	= 0;

topLevel: try {
		// ---- other init ----
		
			if( pr.bool[ PR_ADJUSTGAIN ]) {
				gain = (float) (Param.transform( pr.para[ PR_GAIN ], Param.ABS_AMP, ampRef, null )).val;
			}
			
		// --- open input ----
			switch( inType ) {
			case TYPE_SPECT:
				inF			= new SpectralFile( pr.text[ PR_INPUTFILE ], GenericFile.MODE_INPUT );
				inStream	= ((SpectralFile) inF).getDescr();
				// this helps to prevent errors from empty files!
				if( ((SpectStream) inStream).frames <= 0 ) throw new EOFException( ERR_EMPTY );
				break;

			case TYPE_SOUND:
				inF			= AudioFile.openAsRead( new File( pr.text[ PR_INPUTFILE ]));
				inStream	= ((AudioFile) inF).getDescr();
				// this helps to prevent errors from empty files!
				if( ((AudioFileDescr) inStream).length <= 0 ) throw new EOFException( ERR_EMPTY );
				break;

			case TYPE_IMAGE:
				inF			= new ImageFile( pr.text[ PR_INPUTFILE ], GenericFile.MODE_INPUT );
				inStream	= ((ImageFile) inF).initReader();
				// this helps to prevent errors from empty files!
				if( ((ImageStream) inStream).height <= 0 ) throw new EOFException( ERR_EMPTY );
				row			= ((ImageFile) inF).allocRow();
				break;
			
			default:
				throw new IOException( ERR_CONVERSION );
			}
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// --- open + prepare output ----
			switch( outType ) {
			case TYPE_SPECT:
				ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
				if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
				outF		= new SpectralFile( pr.text[ PR_OUTPUTFILE ], GenericFile.MODE_OUTPUT | ggOutput.getType() );
				outStream	= new SpectStream();
				ggOutput.fillStream( (SpectStream) outStream );

				switch( inType ) {
				case TYPE_IMAGE:		// ------------------------------ TIFF ==> Spect ------------------------------
				
					if( chanNum == 0 ) {
						chanNum = 1;
					}
					if( pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_CHAN ) {
						chanNum = Math.min( chanNum, ((ImageStream) inStream).smpPerPixel );
					}

					tile = (pr.intg[ PRS_OVERHEAD ] == PRS_OVERHEAD_TILE) ?
						   ((pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_CHAN) ? 2 : chanNum) : 1;

					bands = ((ImageStream) inStream).width / tile;
					if( pr.intg[ PRS_TRANSFORM ] == PRS_TRANSFORM_FFT ) {	// power of 2
						for( m = bands - 1, bands = 1; m > 1; m >>= 1, bands <<= 1 ) ;
						bands++;
					}
					
					smpRate		= ((SpectStream) outStream).smpRate;
					hiFreq		= smpRate / 2;
					smpPerPixel = ((ImageStream) inStream).smpPerPixel;
					frames		= ((ImageStream) inStream).height;
					break;

				case TYPE_SOUND:		// ------------------------------ Waveform => Spect ------------------------------
					chanNum = ((AudioFileDescr) inStream).channels;
					smpRate	= (float) ((AudioFileDescr) inStream).rate;
					
					switch( pr.intg[ PRS_TRANSFORM ]) {
//					case PRS_TRANSFORM_FFT:						// .... fast fourier ....
//						loFreq		= 0.0f;
//						hiFreq		= smpRate/2;
//						frameLen	= bands-1;
//						break;
					case PRS_TRANSFORM_DFT:						// .... discrete fourier w/ log scale ....
						bands--;
						// what we are doing here: Anhand der Bandbreite und Sampling-Rate die Frequenz
						// des untersten Bandes errechnen und ggf. nach unten so anpassen, dass die
						// Tuning Frequenz (masterTune=440 Hz) erreicht wird
						freqScale	= freqScales[ pr.intg[ PRS_BANDWIDTH ]];
						loFreq		= (float) (smpRate / (2 * Math.pow( freqScale, bands - 1 )));
						m			= (int) Math.ceil( Math.log( masterTune / loFreq ) / Math.log( freqScale ));
						loFreq		= (float) (masterTune / Math.pow( freqScale, m ));
						hiFreq		= (float) (loFreq * Math.pow( freqScale, bands - 1 ));

						// Sin/Cos Tabellen anlegen
						cos			= new float[ bands ][ frameLen ];
						sin			= new float[ bands ][ frameLen ];
						calcCosineTables( cos, sin, loFreq, hiFreq, smpRate, +1 );
						break;
					case PRS_TRANSFORM_FWT:						// .... fast wavelet (pyramid) ....
						bands--;
						loFreq		= 0.0f;
						hiFreq		= smpRate/2;		// nicht wirklich sinnvoll ;)
						frameLen	= bands;
						break;
					default:
						throw new IOException( ERR_CONVERSION );
					}

					win		= Filter.createWindow( (frameLen >> 1), pr.intg[ PRS_WINDOW ]);
					if( pr.bool[ PR_ADJUSTGAIN ]) {
						for( int i = 0; i < win.length; i++ ) {
							win[ i ] *= gain;					// Gain-Adjustment gleich reinrechnen
						}
					}
					smpData	= new float[ frameLen * chanNum ];
					smpData2= new float[ frameLen * chanNum ];
					tempBuf	= new float[ chanNum ][ frameLen ];
					frames	= (int) (((AudioFileDescr) inStream).length + frameLen-1) / frameLen;
					break;
					
				default:
					throw new IOException( ERR_CONVERSION );
				}

				((SpectStream) outStream).setChannels( chanNum );
// XXX smpPerFrame = bands * (1 - overlap) ?!
//				final int smpPerFrame = (bands - 1) << Math.max(  0, 2 - overlap ) >> Math.max( 0, overlap - 2 );
final int smpPerFrame = (bands - 1) >> overlap;
//System.out.println( "(bands - 1) = " + (bands - 1) + "; (overlap - 2) = " + (overlap - 2) + "; smpPerFrame = " + smpPerFrame );
				((SpectStream) outStream).setRate( smpRate, smpPerFrame );
//					((SpectStream) outStream).setRate( smpRate, bands - 1 );
				((SpectStream) outStream).setEstimatedLength( frames );
				((SpectStream) outStream).setBands( loFreq, hiFreq, bands,
					(pr.intg[ PRS_TRANSFORM ] == PRS_TRANSFORM_FFT) ?
						SpectStream.MODE_LIN : SpectStream.MODE_LOG );
					
				((SpectralFile) outF).initWriter( (SpectStream) outStream,
					(pr.intg[ PRS_TRANSFORM ] == PRS_TRANSFORM_FWT) ?
						SpectralFile.PVA_MAG : SpectralFile.PVA_POLAR );	// Wavelet: only magnitudes!

				frame = ((SpectralFile) outF).allocFrame();
				break;

			case TYPE_SOUND:
				ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
				if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
				outStream	= new AudioFileDescr();
				ggOutput.fillStream( (AudioFileDescr) outStream );
//				outF		= new AudioFile( pr.text[ PR_OUTPUTFILE ], AudioFile.MODE_OUTPUT | ggOutput.getType() );

				switch( inType ) {
				case TYPE_SPECT:		// ------------------------------ Spect ==> Waveform --------------------
					bands		= ((SpectStream) inStream).bands;
					chanNum		= ((SpectStream) inStream).chanNum;
					frameLen	= ((SpectStream) inStream).smpPerFrame;
					smpRate		= ((SpectStream) inStream).smpRate;

					((AudioFileDescr) outStream).channels = chanNum;
					((AudioFileDescr) outStream).rate = smpRate;
					audioOutLength = frameLen * ((SpectStream) inStream).frames;

					switch( pr.intg[ PRS_TRANSFORM ]) {
//					switch( ((SpectStream) inStream).freqMode ) {
//					case SpectStream.MODE_LIN:		// ---- inverse FFT ----
//					case PRS_TRANSFORM_FFT:
//						break;
//					case SpectStream.MODE_LOG:		// ---- inverse DFT ----
					case PRS_TRANSFORM_DFT:
						loFreq		= ((SpectStream) inStream).loFreq;
						hiFreq		= ((SpectStream) inStream).hiFreq;

						// Sin/Cos Tabellen anlegen
						cos			= new float[ bands ][ frameLen ];
						sin			= new float[ bands ][ frameLen ];
						calcCosineTables( cos, sin, loFreq, hiFreq, smpRate, -1 );
						
						break;
					case PRS_TRANSFORM_FWT:			// ---- inverse FWT ----
						loFreq		= 0.0f;
						hiFreq		= smpRate/2;
						for( m = bands - 1, bands = 1; m > 1; m >>= 1, bands <<= 1 ) ;
						frameLen	= bands++;
						break;
					default:
						throw new IOException( ERR_CONVERSION );
					}

					win		= Filter.createWindow( (frameLen >> 1), pr.intg[ PRS_WINDOW ]);
					if( pr.bool[ PR_ADJUSTGAIN ]) {
						for( int i = 0; i < win.length; i++ ) {
							win[ i ] *= gain;					// Gain-Adjustment gleich reinrechnen
						}
					}
					break;

				default:
					throw new IOException( ERR_CONVERSION );
				}

				outF		= AudioFile.openAsWrite( (AudioFileDescr) outStream );
				smpData	= new float[ frameLen * chanNum ];
				smpData2= new float[ frameLen * chanNum ];
				tempBuf	= new float[ chanNum ][ frameLen ];
				break;

			case TYPE_IMAGE:
				ggOutput	= (PathField) gui.getItemObj( GG_OUTPUTFILE );
				if( ggOutput == null ) throw new IOException( ERR_MISSINGPROP );
				outF		= new ImageFile( pr.text[ PR_OUTPUTFILE ], GenericFile.MODE_OUTPUT | ggOutput.getType() );
				outStream	= new ImageStream();
				ggOutput.fillStream( (ImageStream) outStream );

				switch( inType ) {
				case TYPE_SPECT:		// ------------------------------ Spect ==> TIFF ------------------------------
					((ImageStream) outStream).height	= (int) ((SpectStream) inStream).frames;
					((ImageStream) outStream).hRes		= 1f / 72f;
					((ImageStream) outStream).vRes		= 1f / 72f;
					((ImageStream) outStream).resUnit	= ImageStream.RES_INCH;
//					((ImageStream) outStream).bitsPerSmp= 8 << pr.intg[ PRS_COLORDEPTH ];

					bands = ((SpectStream) inStream).bands;

					if( chanNum == 0 ) {
						chanNum = ((SpectStream) inStream).chanNum;
					}
					chanNum = Math.min( chanNum, ((SpectStream) inStream).chanNum );
	
					if( (pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_CHAN) && (chanNum > 3) ) {
						chanNum = 3;
					}
	
					if( ((pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_CHAN) && (chanNum > 1)) ||
						(pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_POLAR) ) {
						
						((ImageStream) outStream).smpPerPixel = 3;	// rgb for multichannel or amp/phase code
					} else {
						((ImageStream) outStream).smpPerPixel = 1;	// grayscale else
					}
					smpPerPixel = ((ImageStream) outStream).smpPerPixel;
	
					tile = (pr.intg[ PRS_OVERHEAD ] == PRS_OVERHEAD_TILE) ?
						   ((pr.intg[ PRS_COLORMODE ] == PRS_COLORMODE_CHAN) ? 2 : chanNum) : 1;
	
					((ImageStream) outStream).width = bands * tile;

//System.out.println( "width "+((ImageStream) outStream).width+"; smpPerPixel "+((ImageStream) outStream).smpPerPixel+"; bitsPerSmp "+((ImageStream) outStream).bitsPerSmp );
	
					((ImageFile) outF).initWriter( (ImageStream) outStream );
					row	= ((ImageFile) outF).allocRow();
					break;
					
				default:
					throw new IOException( ERR_CONVERSION );
				}
				break;
			
			default:
				throw new IOException( ERR_CONVERSION );
			}

			bandsD = bands << 1;

		// ---- Conversion Loop ----
		
			while( (progress < 1.0f) && threadRunning ) {

				switch( inType ) {
				case TYPE_IMAGE:		// ------------------------------ TIFF ==> Spect ------------------------------

//					System.out.println( "reading row " + ((ImageStream) inStream).rowsRead +
//					                    " of " + ((ImageStream) inStream).width + " x " + ((ImageStream) inStream).height );
					
					((ImageFile) inF).readRow( row );
					switch( pr.intg[ PRS_COLORMODE ]) {

					case PRS_COLORMODE_CHAN:
						if( ((ImageStream) inStream).bitsPerSmp == 8 ) {	// 8 bit
							for( int ch = 0; ch < chanNum; ch++ ) {
								for( int band = 0, bandD = 0; band < bands; band++, bandD += 2 ) {
									frame.data[ ch ][ bandD + SpectFrame.AMP ] = (float) Math.exp( (float)
										(255 - ((int) row[ band * smpPerPixel + ch ] & 0xFF)) / ampPerByte );
									frame.data[ ch ][ bandD + SpectFrame.PHASE ] = phasePerByte * (float) ((int)
										row[ (bands + band) * smpPerPixel + ch ] & 0xFF) - (float) Math.PI;
								}
							}
						} else {	// 16 bit
							for( int ch = 0; ch < chanNum; ch++ ) {
								for( int band = 0; band < bandsD; band += 2 ) {
									shorty = (((int) row[ band * smpPerPixel + ch ] & 0xFF) << 8) |
											 ((int) row[ band * smpPerPixel + ch+1 ] & 0xFF);
									frame.data[ ch ][ band + SpectFrame.AMP ] = (float) Math.exp( (float)
										(65535 - shorty) / ampPerWord );

									shorty = (((int) row[ (bandsD + band) * smpPerPixel + ch ] & 0xFF) << 8) |
											 ((int) row[ (bandsD + band) * smpPerPixel + ch+1 ] & 0xFF);
									frame.data[ ch ][ band + SpectFrame.PHASE ] = phasePerWord *
										(float) shorty - (float) Math.PI;
								}
							}
						}
						break;
					
					default:	// polar
						if( ((ImageStream) inStream).bitsPerSmp == 8 ) {	// 8 bit
							if( ((ImageStream) inStream).smpPerPixel >= 2 ) {
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0, bandD = 0; band < bands; band++, bandD += 2 ) {
										frame.data[ ch ][ bandD + SpectFrame.AMP ] = (float) Math.exp( (float)
											(255 - ((int) row[ (band + bands * ch) * smpPerPixel + ch ] & 0xFF)) / ampPerByte );
										frame.data[ ch ][ bandD + SpectFrame.PHASE ] = phasePerByte * (float) ((int)
											row[ (band + bands * ch) * smpPerPixel + ch+1 ] & 0xFF) - (float) Math.PI;
									}
								}
							} else {	// grayscale pict => use it as amp-values, pad phases with zero
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0, bandD = 0; band < bands; band++, bandD += 2 ) {
										frame.data[ ch ][ bandD + SpectFrame.AMP ] = (float) Math.exp( (float)
											(255 - ((int) row[ (band + bands * ch) * smpPerPixel + ch ] & 0xFF)) / ampPerByte );
										frame.data[ ch ][ bandD + SpectFrame.PHASE ] = 0.0f;
									}
								}
							}
						} else {	// 16 bit
							if( ((ImageStream) inStream).smpPerPixel >= 2 ) {
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0; band < bandsD; band += 2 ) {
										shorty = (((int) row[ (band + bandsD * ch) * smpPerPixel + ch ] & 0xFF) << 8) |
												 ((int) row[ (band + bandsD * ch) * smpPerPixel + ch+1 ] & 0xFF);
										frame.data[ ch ][ band + SpectFrame.AMP ] = (float) Math.exp( (float)
											(65535 - shorty) / ampPerWord );

										shorty = (((int) row[ (band + bandsD * ch) * smpPerPixel + ch+2 ] & 0xFF) << 8) |
												 ((int) row[ (band + bandsD * ch) * smpPerPixel + ch+3 ] & 0xFF);
										frame.data[ ch ][ band + SpectFrame.PHASE ] = phasePerWord *
											(float) shorty - (float) Math.PI;
									}
								}
							} else {	// grayscale pict => use it as amp-values, pad phases with zero
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0; band < bandsD; band += 2 ) {
										shorty = (((int) row[ (band + bandsD * ch) * smpPerPixel + ch ] & 0xFF) << 8) |
												 ((int) row[ (band + bandsD * ch) * smpPerPixel + ch+1 ] & 0xFF);
										frame.data[ ch ][ band + SpectFrame.AMP ] = (float) Math.exp( (float)
											(65535 - shorty) / ampPerWord );

										frame.data[ ch ][ band + SpectFrame.PHASE ] = 0.0f;
									}
								}
							}
						}
						break;
					}

					if( pr.bool[ PR_ADJUSTGAIN ]) {
						for( int ch = 0; ch < chanNum; ch++ ) {
							for( int band = 0; band < bandsD; band += 2 ) {
								frame.data[ ch ][ band + SpectFrame.AMP ] *= gain;		// XXX falsch; mit Bandwidth gewichten!
							}
						}
					}
					((SpectralFile) outF).writeFrame( frame );

					progress = (float) ((ImageStream) inStream).rowsRead /
							   (float) ((ImageStream) inStream).height;
					break;


				case TYPE_SPECT:		// ------------------------------ Spect input ------------------------------
					frame = ((SpectralFile) inF).readFrame();

					switch( outType ) {
					case TYPE_IMAGE:		// ------------------------------ Spect ==> TIFF ------------------------------

						switch( pr.intg[ PRS_COLORMODE ]) {
						case PRS_COLORMODE_CHAN:
							if( ((ImageStream) outStream).bitsPerSmp == 8 ) {	// 8 bit
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0, bandD = 0; band < bands; band++, bandD += 2 ) {
										row[ band * smpPerPixel + ch ] = (byte) Math.max( 0, Math.min( 255, 255 - (int) Math.rint(
											ampPerByte * Math.log( frame.data[ ch ][ bandD + SpectFrame.AMP ]))));
										row[ (bands + band) * smpPerPixel + ch ] = (byte) Math.rint(
											((frame.data[ ch ][ bandD + SpectFrame.PHASE ] + Math.PI) % Constants.PI2) / phasePerByte);
									}
								}
							} else {	// 16 bit
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0; band < bandsD; band += 2 ) {
										shorty = Math.max( 0, Math.min( 65535, 65535 - (int) Math.rint(
											ampPerWord * Math.log( frame.data[ ch ][ band + SpectFrame.AMP ]))));
										row[ band * smpPerPixel + ch ] = (byte) ((shorty & 0xFF00) >> 8);
										row[ band * smpPerPixel + ch+1 ] = (byte) (shorty & 0x00FF);

										shorty = (int) Math.rint( ((frame.data[ ch ][ band + SpectFrame.PHASE ] + Math.PI) %
											Constants.PI2) / phasePerWord);
										row[ (bandsD + band) * smpPerPixel + ch ] = (byte) ((shorty & 0xFF00) >> 8);
										row[ (bandsD + band) * smpPerPixel + ch+1 ] = (byte) (shorty & 0x00FF);
									}
								}
							}
							break;
						
						default:	// polar
							if( ((ImageStream) outStream).bitsPerSmp == 8 ) {	// 8 bit
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0; band < bands; band++ ) {
										row[ (band + bands * ch) * smpPerPixel + ch ] = (byte) Math.max( 0, Math.min( 255, 255 - (int) Math.rint(
											ampPerByte * Math.log( frame.data[ ch ][ (band << 1) + SpectFrame.AMP ]))));
										row[ (band + bands * ch) * smpPerPixel + ch+1 ] = (byte) Math.rint(
											((frame.data[ ch ][ (band << 1) + SpectFrame.PHASE ] + Math.PI) % Constants.PI2) / phasePerByte);
									}
								}
							} else {	// 16 bit
								for( int ch = 0; ch < chanNum; ch++ ) {
									for( int band = 0; band < bandsD; band += 2 ) {
										shorty = Math.max( 0, Math.min( 65535, 65535 - (int) Math.rint(
											ampPerWord * Math.log( frame.data[ ch ][ band + SpectFrame.AMP ]))));
										row[ (band + bandsD * ch) * smpPerPixel + ch ] = (byte) ((shorty & 0xFF00) >> 8);
										row[ (band + bandsD * ch) * smpPerPixel + ch+1 ] = (byte) (shorty & 0x00FF);

										shorty = (int) Math.rint( ((frame.data[ ch ][ band + SpectFrame.PHASE ] + Math.PI) %
											Constants.PI2) / phasePerWord);
										row[ (band + bandsD * ch) * smpPerPixel + ch+2 ] = (byte) ((shorty & 0xFF00) >> 8);
										row[ (band + bandsD * ch) * smpPerPixel + ch+3 ] = (byte) (shorty & 0x00FF);
									}
								}
							}
							break;
						}
	
						((ImageFile) outF).writeRow( row );
	
						progress = (float) ((ImageStream) outStream).rowsWritten /
								   (float) ((ImageStream) outStream).height;
						break;

					case TYPE_SOUND:		// ------------------------------ Spect ==> Waveform ----------------------

						switch( pr.intg[ PRS_TRANSFORM ]) {
//						case PRS_TRANSFORM_FFT:						// ---- inverse FFT ----
//							break;
						case PRS_TRANSFORM_DFT:						// ---- inverse DFT ----
							for( int ch = 0; ch < chanNum; ch++ ) {
								for( int band = 0; band < (bands << 1); band += 2 ) {		// AMP = real, PHASE = img
									real	= frame.data[ ch ][ band + SpectFrame.AMP ];
									img		= frame.data[ ch ][ band + SpectFrame.PHASE ];
									frame.data[ ch ][ band + SpectFrame.AMP ]	= (float) (real * Math.cos( img ));
									frame.data[ ch ][ band + SpectFrame.PHASE ]	= (float) (real * Math.sin( img ));
								}
							}

							for( int ch = 0; ch < chanNum; ch++ ) {
								for( int i = 0, j = ch; i < frameLen; i++, j += chanNum ) {
									real = 0f;
									for( int band = 0, k = 0; band < bands; band++, k+= 2 ) {
										real   += frame.data[ ch ][ k + SpectFrame.AMP ]   * cos[ band ][ i ];
										real   -= frame.data[ ch ][ k + SpectFrame.PHASE ] * sin[ band ][ i ];
									}
									smpData[ j ] = real;
								}
							}
							break;
						case PRS_TRANSFORM_FWT:						// ---- inverse FWT ----
							for( int ch = 0; ch < chanNum; ch++ ) {
								// Deinterleave Amp/Phase (omit Phase)
								for( int i = SpectFrame.AMP, j = 0; j < frameLen; j++, i += 2 ) {
									frame.data[ ch ][ j ] = frame.data[ ch ][ i ];
								}
								Wavelet.invTransformDaub4( frame.data[ ch ], frameLen );
								for( int j = 0, i = ch; j < frameLen; j++, i += chanNum ) {
									smpData[ i ] = frame.data[ ch ][ j ];
								}
							}
							break;
						}

// XXX						for( ch = 0; ch < chanNum; ch++ ) {
//							for( i = 0, j = (frameLen >> 1) * chanNum + ch, k = j; i < win.length;
//								 i++, j += chanNum, k -= chanNum ) {
//
//								smpData2[ j ] = smpData[ j ] * win[ i ];
//								smpData2[ k ] = smpData[ k ] * win[ i ];
//							}
//						}
//						((AudioFile) outF).writeSamples( smpData2, 0, smpData2.length );
//						((AudioFile) outF).writeSamples( smpData, 0, smpData.length );
						writeInterleaved( (AudioFile) outF, smpData, 0, smpData.length, tempBuf, chanNum );

						progress = (float) ((AudioFileDescr) outStream).length /
								   (float) audioOutLength;
						break;

					default:
						throw new IOException( ERR_CONVERSION );
					}					

					((SpectralFile) inF).freeFrame( frame );
					break;

				case TYPE_SOUND:		// ------------------------------ Waveform ==> Spect --------------------------

//					((AudioFile) inF).readSamples( smpData, 0, smpData.length );
					readInterleaved( (AudioFile) inF, smpData, 0, smpData.length, tempBuf, chanNum );
					for( int ch = 0; ch < chanNum; ch++ ) {
						for( int i = 0, j = (frameLen >> 1) * chanNum + ch, k = j; i < win.length;
							 i++, j += chanNum, k -= chanNum ) {

							smpData2[ j ] = smpData[ j ] * win[ i ];
							smpData2[ k ] = smpData[ k ] * win[ i ];
						}
					}

					switch( pr.intg[ PRS_TRANSFORM ]) {
//					case PRS_TRANSFORM_FFT:						// ---- FFT ----
//						break;
					case PRS_TRANSFORM_DFT:						// ---- DFT ----
						for( int ch = 0; ch < chanNum; ch++ ) {
							for( int band = 0, k = 0; band < bands; band++, k += 2 ) {
								real	= 0f;
								img		= 0f;
								fCos	= cos[ band ];
								fSin	= sin[ band ];
								for( int i = 0, j = ch; i < frameLen; i++, j += chanNum ) {
									floaty	= smpData2[ j ];
									real   += floaty * fCos[ i ];
									img	   += floaty * fSin[ i ];
								}
								frame.data[ ch ][ k + SpectFrame.AMP ]	= (float) Math.sqrt( img*img + real*real );
								frame.data[ ch ][ k + SpectFrame.PHASE ]= (float) Math.atan2( img, real );
							}
						}
						break;
					case PRS_TRANSFORM_FWT:						// ---- FWT ----
						for( int ch = 0; ch < chanNum; ch++ ) {
							for( int j = 0, i = ch; j < frameLen; j++, i += chanNum ) {
								frame.data[ ch ][ j ] = smpData2[ i ];
							}
							Wavelet.fwdTransformDaub4( frame.data[ ch ], frameLen );
							// Interleave Amp/Phase (create zero Phase)
							for( int i = ((frameLen-1) << 1), j = frameLen-1; j >= 0; j--, i -= 2 ) {
								frame.data[ ch ][ i + SpectFrame.AMP ]	= frame.data[ ch ][ i ];
								frame.data[ ch ][ i + SpectFrame.PHASE]	= 0.0f;
							}
						}
						break;
					}

					((SpectralFile) outF).writeFrame( frame );

					progress = (float) ((AudioFile) inF).getFramePosition() /
							   (float) ((AudioFileDescr) inStream).length;
					break;

				default:
					throw new IOException( ERR_CONVERSION );
				}

		// .... progress ....
				setProgression( progress );
			} 									// (while not progress == 100% and running)
		// .... check running ....
			if( !threadRunning ) break topLevel;

		// ---- Finish ----
		
			if( outF instanceof AudioFile ) {
				((AudioFile) outF).close();
			} else if( outF instanceof GenericFile ) {
				((GenericFile) outF).close();
			}
			outF = null;
			if( inF instanceof AudioFile ) {
				((AudioFile) inF).close();
			} else if( inF instanceof GenericFile ) {
				((GenericFile) inF).close();
			}
			inF = null;

		}
		catch( IOException e1 ) {
			setError( e1 );
		}
		catch( OutOfMemoryError e2 ) {
			smpData	= null;
			smpData2= null;
			frame	= null;
			sin		= null;
			cos		= null;
			System.gc();

			setError( new Exception( ERR_MEMORY ));;
		}

	// ---- cleanup ----
		if( inF != null ) {
			switch( inType ) {
			case TYPE_SPECT:
				if( frame != null ) ((SpectralFile) inF).freeFrame( frame );
				((SpectralFile) inF).cleanUp();
				break;
			case TYPE_SOUND:
				((AudioFile) inF).cleanUp();
				break;
			case TYPE_IMAGE:
				((ImageFile) inF).cleanUp();
				break;
			default:
				break;
			}
		}
		if( outF != null ) {
			switch( outType ) {
			case TYPE_SPECT:
				((SpectralFile) outF).cleanUp();
				break;
			case TYPE_SOUND:
				((AudioFile) outF).cleanUp();
				break;
			case TYPE_IMAGE:
				((ImageFile) outF).cleanUp();
				break;
			default:
				break;
			}
		}
	} // process()

// -------- private Methoden --------

	// fuer DFT (run())
	// iSign: +1 fuer Transformation, -1 fuer inverse Transformation
	private void calcCosineTables( float[][] cos, float[][] sin, float loFreq, float hiFreq,
								   float smpRate, int iSign )
	{
		double	d1, d2;
		double	freq, angle;
		int		i;
		int		bands		= cos.length;
		int		frameLen	= cos[ 0 ].length;
		float	fCos[], fSin[];
	
		d1			= Math.log( (double) hiFreq / (double) loFreq ) / (double) (bands-1);
		d2			= iSign * Constants.PI2 / smpRate;
		for( int band = 0; band < bands; band++ ) {
			freq = d2 * Math.exp( (double) band * d1 ) * loFreq;
			fCos = cos[ band ];
			fSin = sin[ band ];
			for( i = 0; i < frameLen; i++ ) {
				angle = freq * i;
				fCos[ i ] = (float) (Math.cos( angle ) / frameLen);	// Normalisierungs-Faktor 1/frameLen
				fSin[ i ] = (float) (Math.sin( angle ) / frameLen);	//    bereits reinmultipliziert!!
			}
		}
	}

	private static int readInterleaved( AudioFile f, float[] iBuf, int iOff, int iLen, float[][] tempBuf, int numCh )
	throws IOException
	{
		assert (iLen % numCh) == 0 : "invalid interleaved length";
		
		int ch, i, j, len = iLen / numCh;
		
		f.readFrames( tempBuf, 0, len );
		
		for( ch = 0; ch < numCh; ch++ ) {
			for( i = iOff + ch, j = 0; j < iLen; i++, j += numCh ) {
				iBuf[ i ] = tempBuf[ ch ][ j ];
			}
		}
		
		return iLen;
	}

	private static void writeInterleaved( AudioFile f, float[] iBuf, int iOff, int iLen, float[][] tempBuf, int numCh )
	throws IOException
	{
		assert (iLen % numCh) == 0 : "invalid interleaved length";
		
		int ch, i, j, len = iLen / numCh;
		
		for( ch = 0; ch < numCh; ch++ ) {
			for( i = iOff + ch, j = 0; j < iLen; i++, j += numCh ) {
				tempBuf[ ch ][ j ] = iBuf[ i ];
			}
		}
		f.writeFrames( tempBuf, 0, len );
	}
}
// class ConvertDlg
