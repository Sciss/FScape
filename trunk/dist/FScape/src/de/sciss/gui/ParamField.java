/*
 *  PathField.java
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
 *		16-Sep-05	created
 *		10-Mar-05	implements ComboBoxEditor
 *		20-Apr-07	added setValueAndSpace
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;

import de.sciss.util.DefaultUnitTranslator;
import de.sciss.util.Param;
import de.sciss.util.ParamSpace;

/**
 *  @version	0.30, 25-Sep-07
 *
 *	@todo		custom transfer handler that will deal with params and units
 */
public class ParamField
extends JPanel
implements PropertyChangeListener, EventManager.Processor, ComboBoxEditor
{
	private final Jog						ggJog;
	private final NumberField				ggNumber;
	private final UnitLabel					lbUnit;
	private static final UnitViewFactory	uvf				= new DefaultUnitViewFactory();
	private ParamSpace.Translator			ut;

	protected final List					collSpaces		= new ArrayList();
	private ParamSpace						currentSpace	= null;

	private EventManager					elm				= null;	// lazy creation
	
	private boolean							comboGate		= true;

	public ParamField()
	{
		this( new DefaultUnitTranslator() );
	}

	public ParamField( final ParamSpace.Translator ut )
	{
		super();
		
//		uvf				= new DefaultUnitViewFactory();
		this.ut			= ut;
		
		final GridBagLayout			lay		= new GridBagLayout();
		final GridBagConstraints	con		= new GridBagConstraints();

		setLayout( lay );
		con.anchor		= GridBagConstraints.WEST;
		con.fill		= GridBagConstraints.HORIZONTAL;

		ggJog			= new Jog();
		ggNumber		= new NumberField();
		lbUnit			= new UnitLabel();
		
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
		
		ggNumber.addListener( new NumberListener() {
			public void numberChanged( NumberEvent e )
			{
				fireValueChanged( e.isAdjusting() );
			}
		});
		
		lbUnit.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				selectSpace( lbUnit.getSelectedIndex() );
				
				fireSpaceChanged();
				fireValueChanged( false );
			}
		});

		con.gridwidth	= 1;
		con.gridheight	= 1;
		con.gridx		= 1;
		con.gridy		= 1;
		con.weightx		= 0.0;
		con.weighty		= 0.0;
		lay.setConstraints( ggJog, con );
		ggJog.setBorder( BorderFactory.createEmptyBorder( 0, 2, 0, 2 ));
		add( ggJog );

		con.gridx++;
		con.weightx		= 1.0;
		lay.setConstraints( ggNumber, con );
		add( ggNumber );

		con.gridx++;
		con.weightx		= 0.0;
		con.gridwidth	= GridBagConstraints.REMAINDER;
		lay.setConstraints( lbUnit, con );
		lbUnit.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 0 ));
		add( lbUnit );

		this.addPropertyChangeListener( "font", this );
		this.addPropertyChangeListener( "enabled", this );
	}
	
//	public void focusNumber()
//	{
//		ggNumber.requestFocus();
//	}

	public boolean requestFocusInWindow()
	{
		return ggNumber.requestFocusInWindow();
	}
	
	public void addSpace( ParamSpace spc )
	{
		collSpaces.add( spc );
		final Object view = uvf.createView( spc.unit );
		
		if( view instanceof Icon ) {
			lbUnit.addUnit( (Icon) view );
		} else {
			lbUnit.addUnit( view.toString() );
		}
		
		if( collSpaces.size() == 1 ) {
			currentSpace = spc;
			ggNumber.setSpace( spc );
		}
	}
	
	public Param getValue()
	{
		return new Param( ggNumber.getNumber().doubleValue(),
						  currentSpace == null ? ParamSpace.NONE : currentSpace.unit );
	}
	
	public void setValue( Param newValue )
	{
		final Number oldNum		= ggNumber.getNumber();
		final Param newParam	= ut.translate( newValue, currentSpace );
		final Number newNum;

		if( currentSpace.isInteger() ) {
			newNum				= new Long( (long) newParam.val );
		} else {
			newNum				= new Double( newParam.val );
		}
		if( !newNum.equals( oldNum )) ggNumber.setNumber( newNum );
	}
	
	public void setValueAndSpace( Param newValue )
	{
		int spcIdx		= 0;
		ParamSpace spc	= currentSpace;
		boolean newSpc	= false;
		for( ; spcIdx < collSpaces.size(); spcIdx++ ) {
			spc = (ParamSpace) collSpaces.get( spcIdx );
			if( (spc != currentSpace) && (spc.unit == newValue.unit) ) {
				newSpc = true;
				break;
			}
		}
		if( newSpc ) {
			currentSpace = spc;
			ggNumber.setSpace( currentSpace );
			ggNumber.setFlags( currentSpace.unit & ParamSpace.SPECIAL_MASK );
			if( spcIdx != lbUnit.getSelectedIndex() ) {
				lbUnit.setSelectedIndex( spcIdx );
			}
		}
		setValue( newValue );
		if( newSpc ) {
			this.fireSpaceChanged();
		}
	}

	public ParamSpace getSpace()
	{
		return currentSpace;
	}

	public void setSpace( ParamSpace newSpace )
	{
		for( int i = 0; i < collSpaces.size(); i++ ) {
			if( newSpace == collSpaces.get( i )) {	// rely on references here
				selectSpace( i );
				return;
			}
		}
		throw new IllegalArgumentException( "Illegal space switch "+newSpace );
	}

	public void setCycling( boolean b )
	{
		lbUnit.setCycling( b );
	}
	
	public boolean getCycling()
	{
		return lbUnit.getCycling();
	}

	public ParamSpace.Translator getTranslator()
	{
		return ut;
	}

	public void setTranslator( ParamSpace.Translator ut )
	{
		this.ut	= ut;
	}

	private void selectSpace( int selectedIdx )
	{
		if( selectedIdx >= 0 && selectedIdx < collSpaces.size() ) {

			final ParamSpace oldSpace = currentSpace;
			currentSpace			= (ParamSpace) collSpaces.get( selectedIdx );

			final Number oldNum		= ggNumber.getNumber();
			final Param oldParam	= new Param( oldNum == null ?
				(oldSpace == null ? 0.0 : oldSpace.reset) : oldNum.doubleValue(),
				oldSpace == null ? ParamSpace.NONE : oldSpace.unit );
			final Param newParam	= ut.translate( oldParam, currentSpace );
			final Number newNum;

			if( currentSpace.isInteger() ) {
				newNum				= new Long( (long) newParam.val );
			} else {
				newNum				= new Double( newParam.val );
			}

			ggNumber.setSpace( currentSpace );
			ggNumber.setFlags( currentSpace.unit & ParamSpace.SPECIAL_MASK );
			if( !newNum.equals( oldNum )) ggNumber.setNumber( newNum );

			if( selectedIdx != lbUnit.getSelectedIndex() ) {
				lbUnit.setSelectedIndex( selectedIdx );
			}

		} else {
			currentSpace = null;
		}
	}

	// --- listener registration ---
	
	/**
	 *  Register a <code>NumberListener</code>
	 *  which will be informed about changes of
	 *  the gadgets content.
	 *
	 *  @param  listener	the <code>NumberListener</code> to register
	 *  @see	de.sciss.app.EventManager#addListener( Object )
	 */
	public void addListener( ParamField.Listener listener )
	{
		synchronized( this ) {
			if( elm == null ) {
				elm = new EventManager( this );
			}
			elm.addListener( listener );
		}
	}

	/**
	 *  Unregister a <code>NumberListener</code>
	 *  from receiving number change events.
	 *
	 *  @param  listener	the <code>NumberListener</code> to unregister
	 *  @see	de.sciss.app.EventManager#removeListener( Object )
	 */
	public void removeListener( ParamField.Listener listener )
	{
		if( elm != null ) elm.removeListener( listener );
	}

	public void processEvent( BasicEvent e )
	{
		ParamField.Listener listener;
		
		for( int i = 0; i < elm.countListeners(); i++ ) {
			listener = (ParamField.Listener) elm.getListener( i );
			switch( e.getID() ) {
			case ParamField.Event.VALUE:
				listener.paramValueChanged( (ParamField.Event) e );
				break;
			case ParamField.Event.SPACE:
				listener.paramSpaceChanged( (ParamField.Event) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}

	protected void fireValueChanged( boolean adjusting )
	{
		if( elm != null ) {
			elm.dispatchEvent( new ParamField.Event( this, ParamField.Event.VALUE, System.currentTimeMillis(),
				getValue(), getSpace(), getTranslator(), adjusting ));
		}
	}

	protected void fireSpaceChanged()
	{
		if( elm != null ) {
			elm.dispatchEvent( new ParamField.Event( this, ParamField.Event.SPACE, System.currentTimeMillis(),
				getValue(), getSpace(), getTranslator(), false ));
		}
	}

// ------------------- PropertyChangeListener interface -------------------

	/**
	 *  Forwards <code>Font</code> property
	 *  changes to the child gadgets
	 */
	public void propertyChange( PropertyChangeEvent e )
	{
		if( e.getPropertyName().equals( "font" )) {
			final Font fnt = this.getFont();
			ggNumber.setFont( fnt );
			lbUnit.setFont( fnt );
		} else if( e.getPropertyName().equals( "enabled" )) {
			final boolean enabled = this.isEnabled();
			ggJog.setEnabled( enabled );
			ggNumber.setEnabled( enabled );
			lbUnit.setEnabled( enabled );
		}
	}

// ------------------- ComboBoxEditor interface -------------------

	public Component getEditorComponent()
	{
		return this;
	}
	
	public void setComboGate( boolean gate )
	{
		comboGate = gate;
	}
	
	protected boolean getComboGate()
	{
		return comboGate;
	}

	public void setItem( Object anObject )
	{
		if( !comboGate || (anObject == null) ) return;

		if( anObject instanceof Param ) {
			setValue( (Param) anObject );
		} else if( anObject instanceof StringItem ) {
			setValue( Param.valueOf( ((StringItem) anObject).getKey() ));
		}
	}
	
	public Object getItem()
	{
		final Action	a		= lbUnit.getSelectedUnit();
		final String	unit	= a == null ? null : (String) a.getValue( Action.NAME );
		return new StringItem( getValue().toString(), unit == null ? 
				ggNumber.getText() : ggNumber.getText() + " " + unit );
	}

	public void selectAll()
	{
		ggNumber.requestFocus();
		ggNumber.selectAll();
	}
	
	public void addActionListener( ActionListener l )
	{
		ggNumber.addActionListener( l );	// XXX only until we have multiple units!
	}
	
	public void removeActionListener( ActionListener l )
	{
		ggNumber.removeActionListener( l );
	}

// ------------------- internal classes / interfaces -------------------

	public interface UnitViewFactory
	{
		public Object createView( int unit );
	}
	
	public static class Event
	extends BasicEvent
	{
	// --- ID values ---
		/**
		 *  returned by getID() : the param value changed
		 */
		public static final int VALUE	= 0;
		/**
		 *  returned by getID() : the param space was switched
		 */
		public static final int SPACE	= 1;

		private final Param					value;
		private final ParamSpace			space;
		private final ParamSpace.Translator	ut;
		private final boolean				adjusting;

		public Event( Object source, int ID, long when, Param value, ParamSpace space,
					  ParamSpace.Translator ut, boolean adjusting )
		{
			super( source, ID, when );
		
			this.value			= value;
			this.space			= space;
			this.ut				= ut;
			this.adjusting		= adjusting;
		}
		
		public boolean isAdjusting()
		{
			return adjusting;
		}
		
		public Param getValue()
		{
			return value;
		}

		public Param getTranslatedValue( ParamSpace newSpace )
		{
			return ut.translate( value, newSpace );
		}

		public ParamSpace getSpace()
		{
			return space;
		}

		public boolean incorporate( BasicEvent oldEvent )
		{
			if( oldEvent instanceof ParamField.Event &&
				this.getSource() == oldEvent.getSource() &&
				this.getID() == oldEvent.getID() ) {
				
				return true;

			} else return false;
		}
	}
	
	public interface Listener
	extends EventListener
	{
		public void paramValueChanged( ParamField.Event e );
		public void paramSpaceChanged( ParamField.Event e );
	}
}