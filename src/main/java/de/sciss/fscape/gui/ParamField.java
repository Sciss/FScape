/*
 *  ParamField.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.gui.Jog;
import de.sciss.gui.NumberEvent;
import de.sciss.gui.NumberField;
import de.sciss.gui.NumberListener;

import de.sciss.fscape.util.*;

/**
 *	GUI element for numeric input
 *	including a mouse controlled dial
 *	wheel and optional unit selector
 *	with automatic value conversion.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.701, 31-Dec-06
 */
public class ParamField
extends JPanel
//implements ActionListener, EventManager.Processor
implements EventManager.Processor
{
// -------- public Variablen --------

// -------- private Variablen --------

	private static final String dimTxt[]		= { "", "", "ms", "Hz", "\u00B0" };
	private static final String specialTxt[]	= { "", "beats", "semi", "dB" };
	private static final String percentTxt	= "%";
	private static final String relTxt		= "\u00B1";

//	private static final String	CMD_SETTINGS= "Set";
//	private static final String	CMD_ABBR	= "Abbr";
//	private static final String	CMD_EXPAND	= "Exp";
//	private static final String[]	CMD_UVAL	= { "UserVal1", "UserVal2", "UserVal3", "UserVal4" };
//
//	private   static final String	mParam[][]	= {{ "Save Param as", "User val #1 [F5]", "User val #2 [F6]",
//													 "User val #3 [F7]", "User val #4 [F8]" },
//													 { "Settings" }};
//	private PopupStrip pop;

	private GridBagLayout		lay;
	private GridBagConstraints	con;
//	private NumField			ggNumber;
	private NumberField			ggNumber;
//	private DataWheel			ggJog;
	private final Jog			ggJog;
	private JComboBox			ggUnits;
	private JLabel				lbUnits;

	private final EventManager	elm		= new EventManager( this );

//	private Param			para;
	private Param			reference	= null;	// statischer Referenz-Wert
	private ParamField		refField	= null;	// dynamisches Referenz-Feld
	
	private ParamSpace	spaces[];		// e.g. Constants.spaces[ ... ]
	private int			currentSpaceIdx;	// ArrayIndex in spaces[]
	private ParamSpace	currentSpace;

// -------- public Methoden --------

	public ParamField()
	{
		super();

//		para			= new Param();
		spaces			= new ParamSpace[ 1 ];
		spaces[ 0 ]		= Constants.spaces[ Constants.emptySpace ];
		currentSpaceIdx	= 0;
currentSpace = spaces[ currentSpaceIdx ];

		lay		= new GridBagLayout();
		con		= new GridBagConstraints();
//		ggNumber	= new NumField( currentSpace, para );
//		ggNumber	= new NumberField( currentSpace, para );
ggNumber = new NumberField();
ggNumber.setSpace( currentSpace );
//ggNumber.setNumber( paraToNumber( para ));
//		ggJog	= new DataWheel( ggNumber );
		ggJog	= new Jog() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                super.paintComponent(g);
            }
        };
		ggUnits	= new JComboBox();
		lbUnits = new JLabel();

		ggJog.addListener( new NumberListener() {
			public void numberChanged( NumberEvent e )
			{
				if( currentSpace != null ) {
					final double	inc		= e.getNumber().doubleValue() * currentSpace.inc;
					final Number	num		= ggNumber.getNumber();
					final Number	newNum;
					boolean			changed;
					
					if( currentSpace.isInteger() ) {
						newNum	= new Long( (long) currentSpace.fitValue( num.longValue() + inc ));
					} else {
						newNum	= new Double( currentSpace.fitValue( num.doubleValue() + inc ));
					}
					
					changed	= !newNum.equals( num );
					if( changed ) {
						ggNumber.setNumber( newNum );
					}
					if( changed || !e.isAdjusting() ) {
						fireValueChanged( e.isAdjusting() );
					}
				}
			}
		});

		setLayout( lay );
		con.anchor		= GridBagConstraints.WEST;
		con.fill		= GridBagConstraints.HORIZONTAL;

		con.gridwidth	= 1;
		con.gridheight	= 2;
		con.gridx		= 0;
		con.gridy		= 0;
		con.weighty		= 1.0;
		lay.setConstraints( ggJog, con );
//		ggJog.addMouseListener( new MouseAdapter() {
//			// MouseReleased: weiterleiten
//			public void mouseReleased( MouseEvent e )
//			{
//				dispatchChange();
//			}
//
//			public void mousePressed( MouseEvent e )
//			{
//				if( e.isControlDown() ) {
//		//			ggJog.add( pop );
//					pop.show( ggJog, e.getX(), e.getY() );
//		//			ggJog.remove( pop );
//				}
//			}
//		});
		ggJog.setBorder( BorderFactory.createEmptyBorder( 0, 2, 0, 2 ));
		add( ggJog );

		con.gridheight	= 1;
		con.gridx		= 1;
		con.gridy		= 1;
		con.weightx		= 1.0;
		con.weighty		= 0.0;
		lay.setConstraints( ggNumber, con );
//		ggNumber.addTextListener( this );			// Low-Level
//		ggNumber.addInputMethodListener( this );		// Low-Level
//		ggNumber.addFocusListener( new FocusAdapter() {
//			public void focusLost( FocusEvent e )
//			{
//				textToPara();
//			}
//		});
		ggNumber.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				dispatchChange();
			}
		});			// High-Level Events: Return-Hit weiterleiten
		add( ggNumber );

		con.gridx		= 2;
		con.weightx		= 0.0;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		lay.setConstraints( ggUnits, con );
		lay.setConstraints( lbUnits, con );

		add( ggUnits );
		ggUnits.setVisible( false );
		ggUnits.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				final int newSpace = ggUnits.getSelectedIndex();
				if( newSpace != currentSpaceIdx ) {
					
					// transform
					final Param tempPara, newParam;
					final Param oldParam = getParam();
					tempPara = Param.transform( oldParam, spaces[ newSpace ].unit, (refField == null) ?
												reference : refField.makeReference(), null );
		
					// apply new values
					newParam		= new Param( spaces[ newSpace ].fitValue(
												(tempPara == null) ? oldParam.val : tempPara.val ),
												spaces[ newSpace ].unit );
					currentSpaceIdx	= newSpace;
					currentSpace = spaces[ currentSpaceIdx ];
		
	//				ggNumber.setParam( spaces[ newSpace ], para );
	ggNumber.setSpace( currentSpace );
	ggNumber.setNumber( paraToNumber( newParam ));
		
					// Listener benachrichtigen
					dispatchChange();
				}
			}
		});
		add( lbUnits );

//		pop				= new PopupStrip( mParam, this );
	}

	/**
	 *	Single Unit version
	 */
	public ParamField( ParamSpace space )
	{
		this();

		ParamSpace spcs[]	= new ParamSpace[ 1 ];
		spcs[ 0 ]			= space;
		setSpaces( spcs );
	}

	/**
	 *	Multi Unit version
	 */
	public ParamField( ParamSpace spaces[] )
	{
		this();
		setSpaces( spaces );
	}
	
	protected Number paraToNumber( Param newParam )
	{
		final Number newNum;
	
		if( currentSpace.isInteger() ) {
			newNum				= new Long( (long) newParam.val );
		} else {
			newNum				= new Double( newParam.val );
		}
		return newNum;
	}

	/**
	 *	Aendert die zur Verfuegung stehenden Masseinheiten
	 */
	public void setSpaces( ParamSpace spaces[] )
	{
		int oldNum	= this.spaces.length;	// Zahl der Einheiten vorher
		this.spaces	= spaces;
//		int colNum  = 3;
currentSpaceIdx = Math.min( spaces.length - 1, currentSpaceIdx );
currentSpace = spaces[ currentSpaceIdx ];
		
//		for( i = 0; i < spaces.length; i++ ) {
//			colNum = Math.max( colNum, String.valueOf( spaces[i].inc ).length() +
//							   Math.max( String.valueOf( (long) spaces[i].min ).length(),
//										 String.valueOf( (long) spaces[i].max ).length() ));
//		}
//		ggNumber.setColumns( colNum );

		ggUnits.removeAllItems();
		if( spaces.length > 1 ) {

			for( int i = 0; i < spaces.length; i++ ) {
				ggUnits.addItem( getUnitText( spaces[ i ].unit ));
			}

			if( oldNum == 1 ) {		// war JLabel? dann JLabel gegen JComboBox tauschen
				lbUnits.setVisible( false );
				ggUnits.setVisible( true );
			}

		} else {						// ...als JLabel

			lbUnits.setText( getUnitText( currentSpace.unit ));

			if( oldNum != 1 ) {		// war JComboBox? dann JComboBox gegen JLabel tauschen
				ggUnits.setVisible( false );
				lbUnits.setVisible( true );
			}
		}

ggNumber.setSpace( currentSpace );
//		setParam( para );
	}
	
	/**
	 *	Ermittelt die aktuelle Masseinheit
	 */
	public ParamSpace getSpace()
	{
		return currentSpace;
	}

	/**
	 *	Setzt den Parameter auf einen neuen Wert (ggf. umgerechnet)
	 */
	public void setParam( Param newParam )
	{
		final Number	oldNum		= ggNumber.getNumber();
		final Number	newNum;

		int				newSpace	= 0;
		int				pri;
		final boolean	spaceChange;

		for( int i = 0, bestPri = -99; i < spaces.length; i++ ) {

			pri = Param.getPriority( newParam.unit, spaces[ i ].unit );

			if( pri > bestPri ) {
				bestPri		= pri;
				newSpace	= i;
			}
		}
		
		// transform
//		tempPara = Param.transform( newParam, spaces[ newSpace ].unit, (refField == null) ?
//									reference : refField.makeReference(), null );

spaceChange = currentSpaceIdx != newSpace;
if( spaceChange ) {
	currentSpaceIdx	= newSpace;
	currentSpace = spaces[ currentSpaceIdx ];
	ggNumber.setSpace( currentSpace );
}

		// apply new values
//		para		= new Param( spaces[ newSpace ].fitValue(
//								 (tempPara == null) ? newPara.val : tempPara.val ),
//								 spaces[ newSpace ].unit );
		if( currentSpace.isInteger() ) {
			newNum				= new Long( (long) newParam.val );
		} else {
			newNum				= new Double( newParam.val );
		}

		if( spaces.length > 1 ) {
			ggUnits.setSelectedIndex( currentSpaceIdx );		// JComboBox waehlen
		}
		
//		ggNumber.setParam( currentSpace, para );
//ggNumber.setSpace( currentSpace );
//ggNumber.setNumber( paraToNumber( para ));

		if( spaceChange || !newNum.equals( oldNum )) ggNumber.setNumber( newNum );
	}

	/**
	 *	Setzt einen Referenz-Parameter, an dem sich
	 *	das ParamField orientieren kann, wenn der User
	 *	eine andere Masseinheit waehlt.
	 *	(vgl. Param.transform())
	 */
	public void setReference( Param reference )
	{
		this.reference = reference;
	}
	
	/**
	 *	Legt ein weiteres ParamField als Referenz-Feld fest,
	 *	an dem sich dieses ParamField orientieren kann, wenn
	 *	der User eine andere Masseinheit waehlt.
	 *	(vgl. Param.transform())
	 *
	 *	dieses Feld geniesst Vorrang vor einem mit
	 *	setReference( Param ) bestimmten statischen Referenzwert!
	 */
	public void setReference( ParamField refField )
	{
		this.refField = refField;
	}
	
	/**
	 *	Erzeugt einen Referenz-Wert mit ABSUNIT, der
	 *	von einem anderen Param(Field) als Param.transform()-Referenz
	 *	verwendet werden kann;
	 *
	 *	@return	moeglicherweise null!
	 */
	public Param makeReference()
	{
		int		destUnit = (currentSpace.unit & ~Param.FORM_MASK) | Param.ABSUNIT;
		Param	tempPara;
		final Param oldParam	= getParam();
	
		tempPara = Param.transform( oldParam, destUnit, (refField == null) ?
									reference : refField.makeReference(), null );
		if( tempPara == oldParam ) {
			return (Param) tempPara.clone();	// since this.para is non-"static"
		} else {
			return tempPara;
		}
	}
	
//	/**
//	 *	Besorgt den Parameter
//	 */
//	public Param getParam()
//	{
//		synchronized( para ) {
//			return (Param) para.clone();
//		}
//	}

//	public Param getParam()
//	{
//		return new Param( ggNumber.getNumber().doubleValue(),
//						  currentSpace == null ? ParamSpace.NONE : currentSpace.unit );
//	}
	public Param getParam()
	{
		return new Param( ggNumber.getNumber().doubleValue(),
						  currentSpace == null ? Param.NONE : currentSpace.unit );
	}

	public void addParamListener( ParamListener li )
	{
		elm.addListener( li );
	}

	public void removeParamListener( ParamListener li )
	{
		elm.removeListener( li );
	}

	protected void fireValueChanged( boolean adjusting )
	{
dispatchChange();
//		if( elm != null ) {
//			elm.dispatchEvent( new ParamField.Event( this, ParamField.Event.VALUE, System.currentTimeMillis(),
//				getValue(), getSpace(), getTranslator(), adjusting ));
//		}
	}

	public void processEvent( BasicEvent e )
	{
		ParamListener listener;
		
		for( int i = 0; i < elm.countListeners(); i++ ) {
			listener = (ParamListener) elm.getListener( i );
			switch( e.getID() ) {
			case ParamEvent.CHANGED:
				listener.paramChanged( (ParamEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}
	
	protected void dispatchChange()
	{
		elm.dispatchEvent( new ParamEvent( this, ParamEvent.CHANGED, System.currentTimeMillis(),
										   getParam(), getSpace() ));
	}

	public void setEnabled( boolean state )
	{
		if( !state ) {
			ggJog.requestFocus();	// tricky ggNumber.looseFocus() ;)
		}

		ggNumber.setEnabled( state );
		ggJog.setEnabled( state );
		if( ggUnits.isVisible() ) {
			ggUnits.setEnabled( state );
		}

		if( state ) {
			ggNumber.requestFocus();
		}
	}

// -------- Action Methoden (ggNumber Return-Hit) --------

/*
	public void actionPerformed( ActionEvent e )
	{
		String  cmd			= e.getActionCommand();
		Param	tempPara;
		int		newSpace;
		
		if( e.getSource() == ggUnits ) {
			newSpace = ggUnits.getSelectedIndex();
			if( newSpace != currentSpaceIdx ) {
				
				// transform
				tempPara = Param.transform( para, spaces[ newSpace ].unit, (refField == null) ?
											reference : refField.makeReference(), null );
	
				// apply new values
				para			= new Param( spaces[ newSpace ].fitValue(
											(tempPara == null) ? para.val : tempPara.val ),
											spaces[ newSpace ].unit );
				currentSpaceIdx	= newSpace;
				currentSpace = spaces[ currentSpaceIdx ];
	
//				ggNumber.setParam( spaces[ newSpace ], para );
ggNumber.setSpace( currentSpace );
ggNumber.setNumber( paraToNumber( para ));
	
				// Listener benachrichtigen
				dispatchChange();
			}
			
		} else if( e.getSource() == ggNumber ) {		// -------- from NumField --------
			if( cmd == CMD_SETTINGS ) {
//				ParamSettingsDlg.showDlg( true );
			} else {
				boolean b = false;
			
				if( cmd == CMD_ABBR ) {
					spaces[ currentSpaceIdx ] = abbrSpace( currentSpace );
					currentSpace = spaces[ currentSpaceIdx ];
					setParam( para );
					b = true;
				} else if( cmd == CMD_EXPAND ) {
					spaces[ currentSpaceIdx ] = expandSpace( currentSpace );
					currentSpace = spaces[ currentSpaceIdx ];
					setParam( para );
					b = true;
				} else if( (cmd == CMD_UVAL[0]) || (cmd == CMD_UVAL[1]) ||
						   (cmd == CMD_UVAL[2]) || (cmd == CMD_UVAL[3]) ) {

					String	uval = Application.userPrefs.get( cmd, null );
					if( uval != null ) {
						setParam( Param.valueOf( uval ));
					}
					b = true;
				}
				
				if( b ) {
					dispatchChange();
				} else {
					textToPara();
					dispatchChange();
				}
			}
		} else {										// -------- from popup --------
		
			if( cmd == mParam[ 1 ][ 0 ]) {
//				ParamSettingsDlg.showDlg( true );
			} else {
				for( int i = 1; i < mParam[ 0 ].length; i++ ) {
					if( cmd == mParam[ 0 ][ i ]) {
						para = getParam();
						String uval = para.toString();
						Application.userPrefs.put( CMD_UVAL[ i-1 ], uval );
						break;
					}
				}
			}
		}
	}
*/

// -------- Focus Methoden (ggNumber) --------

/*
	private void textToPara()
	{
//		double val, fittedVal;
//
////		try {
////			val			= Double.valueOf( ggNumber.getText() ).doubleValue();
//			val			= Double.valueOf( ggNumber.getText() ).doubleValue();
//// System.out.println( "JTextField.getText() : "+ggNumber.getText()+" ; double val "+val );
//			fittedVal	= currentSpace.fitValue( val );
//			synchronized( para ) {
//				para.val = fittedVal;
//			}
//			if( Math.abs( val - fittedVal ) > Constants.suckyDoubleError ) {
////				ggNumber.paraToText();
//ggNumber.setNumber( paraToNumber( para ));
//			}
////		} catch( NumberFormatException e2 ) {
////			ggNumber.paraToText();
////		}
	}
*/
	
// -------- private Methoden --------

/*
	private ParamSpace abbrSpace( ParamSpace orig )
	{
		double res = orig.inc;
		if( res < 1.0 ) {
			return( new ParamSpace( orig.min, orig.max, res * 10, orig.unit ));
		} else {
			return orig;
		}
	}

	private ParamSpace expandSpace( ParamSpace orig )
	{
		double res = orig.inc;
		if( res > 0.00001 ) {
			return( new ParamSpace( orig.min, orig.max, res / 10, orig.unit ));
		} else {
			return orig;
		}
	}
*/
	/*
	 *	Textdarstellung der Einheit ermitteln
	 */
	private String getUnitText( int unit )
	{
		String unitTxt = specialTxt[ (unit & Param.SPECIAL_MASK) >> 8 ];
		
		switch( unit & Param.FORM_MASK ) {
		case Param.ABSUNIT:
			if( unitTxt.length() == 0 ) {
				unitTxt = dimTxt[ unit & Param.DIM_MASK ];
			}
			break;
		case Param.ABSPERCENT:
			unitTxt = ((unitTxt.length() == 0) ? percentTxt : unitTxt);
			break;
		case Param.RELUNIT:
			unitTxt = relTxt + ' ' + ((unitTxt.length() == 0) ?
					  dimTxt[ unit & Param.DIM_MASK ] : unitTxt);
			break;
		case Param.RELPERCENT:
			unitTxt = relTxt + ' ' + ((unitTxt.length() == 0) ? percentTxt : unitTxt);
			break;
		default:
			break;
		}

		return unitTxt;
	}

// -------- interne Datawheel-Klasse --------

/*
	private class DataWheel
	extends ToolIcon
	{
		protected Cursor	lastCursor	= null;
		protected Cursor	dragCursor;
		
		protected int		arc			= 90;
		
		protected double	lastArc;
		protected int		lastX;
		protected int		lastY;
	
		protected NumField	associate;
		
		boolean	  ctrlDown	= false;
	
		private DataWheel( NumField associate )
		{
			super( ID_WHEEL, "" );

			this.associate	= associate;
			dragCursor		= new Cursor( Cursor.MOVE_CURSOR );

			enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );
		}

		public void update( Graphics g )
		{
			paint( g );
		}

		public void paint( Graphics g )
		{
			super.paint( g );
			if( state == STATE_SELECTED ) {
				g.setColor( OpIcon.progColor );
				g.fillOval( (ibWidth >>1) + ((int) (Math.cos( arc * Math.PI / 180 ) * 12)) - 3,
							(ibHeight>>1) - ((int) (Math.sin( arc * Math.PI / 180 ) * 12)) - 4,
							5, 5 );
			}
		}

	// ........ private Methoden ........

		protected void processMouseEvent( MouseEvent e )
		{
			int dx, dy;

			switch( e.getID() ) {
			case MouseEvent.MOUSE_PRESSED:
				ctrlDown = e.isControlDown();

				lastCursor = getCursor();
				getParent().getParent().setCursor( dragCursor );	// weil in *zwei* Containern

				dx			= e.getX() - (ibWidth >> 1);
				dy			= e.getY() - (ibHeight >> 1);
				lastX		= e.getX();
				lastY		= e.getY();
				lastArc		= Math.atan2( dx, dy ) + Math.PI;

				setSelected( STATE_SELECTED );
				clicked = true;
				
				requestFocus();
				break;

			case MouseEvent.MOUSE_RELEASED:
//				if( ctrlDown ) break;
				setSelected( STATE_NORMAL );
				clicked = false;
				getParent().getParent().setCursor( lastCursor );

				associate.requestFocus();
				break;

			case MouseEvent.MOUSE_CLICKED:
				if( ctrlDown ) break;
				if( e.isAltDown() ) {			// Alt+Click sets value to origin
					para.val = currentSpace.fitValue( 0.0 );
					arc		 = 90;
				} else {						// "normal" Click = unit up, Shift+Click = unit down
					para.val = currentSpace.fitValue( para.val +
									currentSpace.inc * (e.isShiftDown() ? -1 : 1) );
					arc		 = (arc + 5) % 360;
				}
				associate.paraToText();						
				break;

			default:
				break;
			}
			super.passMouseEvent( e );
		}

		protected void processMouseMotionEvent( MouseEvent e )
		{
			double	thisArc, thisDist;
			double	deltaArc;
			int		dx, dy;
			int		dragAmount;
		
			switch( e.getID() ) {
			case MouseEvent.MOUSE_DRAGGED:			
				dx			= e.getX() - (ibWidth >> 1);
				dy			= e.getY() - (ibHeight >> 1);
				thisDist	= Math.sqrt( dx*dx + dy*dy );
				thisArc		= Math.atan2( dx, dy ) + Math.PI;
				deltaArc	= thisArc - lastArc;
				if( deltaArc < -Math.PI ) {
					deltaArc = Math.PI*2 - deltaArc;
				} else if( deltaArc > Math.PI ) {
					deltaArc = -Math.PI*2 + deltaArc;
				}
				
				dx			= e.getX() - lastX;
				dy			= e.getY() - lastY;	// vvv Java lacks Math.sgn()
				dragAmount	= (int) Math.sqrt( dx*dx + dy*dy );
				arc			= arc + ((deltaArc < 0) ? -1 : 1) * Math.min( 30,
								((int) ((1 + thisDist / 30) * dragAmount ))) % 360;

				if( dragAmount > 1 ) {
					if( dragAmount > 16 ) {		// Beschleunigen
						dragAmount *= (dragAmount - 16);
					}

					lastArc		= thisArc;
					lastX		= e.getX();
					lastY		= e.getY();
					repaint();

					dragAmount	= ((deltaArc < 0) ? 1 : -1) * (dragAmount >> 1);
					
					para.val = currentSpace.fitValue( para.val +
							   currentSpace.inc * dragAmount );
					associate.paraToText();						
				}

			default:
				break;
			}
			super.processMouseEvent( e );
		}	
	}

// -------- interne Nummernfeld-Klasse --------

	private class NumField
	extends JTextField
	{
		protected ParamSpace	space;
		protected Param			para;
		protected long			postMult;	// 10er Potenz mit der Nachkommastellen
											// in den Vorkommabereich multipliziert werden
	
		private NumField( ParamSpace space, Param para )
		{
			super();

			setParam( space, para );	// invokes paraToText()
			// Event handling
			enableEvents( AWTEvent.KEY_EVENT_MASK );
		}

		private void setParam( ParamSpace space, Param para )
		{
			long postMult;

			if( this.space != space ) {
				this.space	= space;
			
				// recalc digit num
				for( postMult = 1; (space.inc * postMult) - Math.floor( space.inc * postMult ) > 0;
					 postMult *= 10 );

				this.postMult = postMult;
				
//				setColumns( Math.max( String.valueOf( (int) (space.max + 0.5) ).length(),
//							String.valueOf( (int) (space.min + 0.5) ).length() ) + String.valueOf( postMult ).length() );
			}

			this.para = para;
			paraToText();
		}

		private void paraToText()
		{
			double	preVal;
			String	postStr;

			if( postMult > 1 ) {		// Nachkommastellen
				if( para.val >= 0 ) {
					preVal	= Math.floor( para.val );
					postStr	= String.valueOf( (long) ((1 + para.val - preVal) * postMult + 0.5) );
					this.setText( String.valueOf( (long) preVal ) + '.' + postStr.substring( 1 ));

				} else {
					preVal	= Math.ceil( para.val );
					postStr	= String.valueOf( (long) ((1 + preVal - para.val) * postMult + 0.5) );
					if( preVal != 0 ) {
						this.setText( String.valueOf( (long) preVal ) + '.' + postStr.substring( 1 ));
					} else {
						this.setText( '-' + String.valueOf( (long) preVal ) + '.' + postStr.substring( 1 ));
					}
				}
				
			} else {					// keine Nachkommastellen
				this.setText( String.valueOf( (long) Math.round( para.val )));
			}
		}

	// ........ private Methoden ........

		protected void processKeyEvent( KeyEvent e )
		{
			String cmd	= null;

			switch( e.getKeyCode() ) {
			case KeyEvent.VK_F1:
				cmd = ParamField.CMD_SETTINGS;
				break;
			case KeyEvent.VK_F2:
				cmd = ParamField.CMD_ABBR;
				break;
			case KeyEvent.VK_F3:
				cmd = ParamField.CMD_EXPAND;
				break;
			case KeyEvent.VK_F5:
				cmd = ParamField.CMD_UVAL[ 0 ];
				break;
			case KeyEvent.VK_F6:
				cmd = ParamField.CMD_UVAL[ 1 ];
				break;
			case KeyEvent.VK_F7:
				cmd = ParamField.CMD_UVAL[ 2 ];
				break;
			case KeyEvent.VK_F8:
				cmd = ParamField.CMD_UVAL[ 3 ];
				break;
			}
			
			if( cmd == null ) {
				super.processKeyEvent( e );
			} else {
				e.consume();
				if( e.getID() == KeyEvent.KEY_RELEASED ) {
					dispatchEvent( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, cmd ));
				}
			}
		}
	} // class NumField
*/
}
// class ParamField
