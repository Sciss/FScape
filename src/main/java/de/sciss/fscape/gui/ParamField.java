/*
 *  ParamField.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape.gui;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.gui.Jog;
import de.sciss.gui.NumberEvent;
import de.sciss.gui.NumberField;
import de.sciss.gui.NumberListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *	GUI element for numeric input
 *	including a mouse controlled dial
 *	wheel and optional unit selector
 *	with automatic value conversion.
 */
public class ParamField
        extends JPanel
        implements EventManager.Processor {

// -------- public variables --------

// -------- private variables --------

    private static final String dimTxt[]		= { "", "", "ms", "Hz", "\u00B0" };
    private static final String specialTxt[]	= { "", "beats", "semi", "dB" };
    private static final String percentTxt	= "%";
    private static final String relTxt		= "\u00B1";

    private GridBagLayout		lay;
    private GridBagConstraints	con;
    private NumberField			ggNumber;
    private final Jog           ggJog;
    private JComboBox			ggUnits;
    private JLabel				lbUnits;

    private final EventManager	elm		= new EventManager( this );

//	private Param			para;
    private Param			reference	= null;	// static reference value
    private ParamField		refField	= null;	// dynamic reference field

    protected ParamSpace	spaces[];		// e.g. Constants.spaces[ ... ]
    private int			currentSpaceIdx;	// ArrayIndex in spaces[]
    private ParamSpace	currentSpace;

// -------- public methods --------

    public ParamField()
    {
        super();

        final Color c1 = getForeground ();
        final Color c2 = getBackground ();

        spaces			= new ParamSpace[ 1 ];
        spaces[ 0 ]		= Constants.spaces[ Constants.emptySpace ];
        currentSpaceIdx	= 0;
        currentSpace = spaces[ currentSpaceIdx ];

        lay		= new GridBagLayout();
        con		= new GridBagConstraints();

        ggNumber = new NumberField();
        ggNumber.setSpace( currentSpace );

        ggJog	= new Jog();
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

        ggJog.setBorder( BorderFactory.createEmptyBorder( 0, 2, 0, 2 ));
        add( ggJog );

        con.gridheight	= 1;
        con.gridx	   += 1;
        con.gridy		= 1;
        con.weightx		= 1.0;
        con.weighty		= 0.0;
        lay.setConstraints( ggNumber, con );

        ggNumber.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                dispatchChange();
            }
        });			// High-Level Events: forward Return-Hit
        add( ggNumber );

        con.gridx	   += 1;
        con.weightx		= 0.0;
        con.gridwidth	= GridBagConstraints.REMAINDER;
        lay.setConstraints( ggUnits, con );
        lay.setConstraints( lbUnits, con );

        add(ggUnits);
        ggUnits.setVisible(false);
        ggUnits.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int newSpace = ggUnits.getSelectedIndex();
                if (newSpace != currentSpaceIdx) {

                    // transform
                    final Param tempPara, newParam;
                    final Param oldParam = getParam();
                    tempPara = Param.transform(oldParam, spaces[newSpace].unit, (refField == null) ?
                            reference : refField.makeReference(), null);

                    // apply new values
                    newParam = new Param(spaces[newSpace].fitValue(
                            (tempPara == null) ? oldParam.value : tempPara.value),
                            spaces[newSpace].unit);
                    currentSpaceIdx = newSpace;
                    currentSpace = spaces[currentSpaceIdx];

                    ggNumber.setSpace(currentSpace);
                    ggNumber.setNumber(paraToNumber(newParam));

                    // inform Listener
                    dispatchChange();
                }
            }
        });
        add(lbUnits);
    }

    /**
     *	Single Unit version
     */
    public ParamField(ParamSpace space) {
        this();

        ParamSpace spaces[] = new ParamSpace[1];
        spaces[0] = space;
        setSpaces(spaces);
    }

    /**
     *	Multi Unit version
     */
    public ParamField(ParamSpace spaces[]) {
        this();
        setSpaces(spaces);
    }

    protected Number paraToNumber(Param newParam) {
        final Number newNum;

        if (currentSpace.isInteger()) {
            newNum = new Long((long) newParam.value);
        } else {
            newNum = new Double(newParam.value);
        }
        return newNum;
    }

    /**
     *	Changes the available units
     */
    public void setSpaces(ParamSpace spaces[]) {
        int oldNum	= this.spaces.length;	// previous number of units
        this.spaces	= spaces;

        currentSpaceIdx = Math.min( spaces.length - 1, currentSpaceIdx );
        currentSpace = spaces[ currentSpaceIdx ];

        ggUnits.removeAllItems();
        if (spaces.length > 1) {

            for (int i = 0; i < spaces.length; i++) {
                ggUnits.addItem(getUnitText(spaces[i].unit));
            }

            if (oldNum == 1) {        // was JLabel? than change JLabel for JComboBox
                lbUnits.setVisible(false);
                ggUnits.setVisible(true);
            }

        } else {                        // ...als JLabel

            lbUnits.setText(getUnitText(currentSpace.unit));

            if (oldNum != 1) {        // was JComboBox? then change JComboBox for JLabel
                ggUnits.setVisible(false);
                lbUnits.setVisible(true);
            }
        }

        ggNumber.setSpace(currentSpace);
    }

    /**
     * Retrieves the current unit
     */
    public ParamSpace getSpace() {
        return currentSpace;
    }

    /**
     *	Sets the parameter to a new value (which might be converted in the process)
     */
    public void setParam(Param newParam) {
        final Number	oldNum		= ggNumber.getNumber();
        final Number	newNum;

        int				newSpace	= 0;
        int				pri;
        final boolean	spaceChange;

        for (int i = 0, bestPri = -99; i < spaces.length; i++) {

            pri = Param.getPriority(newParam.unit, spaces[i].unit);

            if (pri > bestPri) {
                bestPri = pri;
                newSpace = i;
            }
        }

        spaceChange = currentSpaceIdx != newSpace;
        if (spaceChange) {
            currentSpaceIdx = newSpace;
            currentSpace = spaces[currentSpaceIdx];
            ggNumber.setSpace(currentSpace);
        }

        if (currentSpace.isInteger()) {
            newNum = new Long((long) newParam.value);
        } else {
            newNum = new Double(newParam.value);
        }

        if (spaces.length > 1) {
            ggUnits.setSelectedIndex(currentSpaceIdx);        // choose JComboBox
        }

        if (spaceChange || !newNum.equals(oldNum)) ggNumber.setNumber(newNum);
    }

    /**
     *	Sets a reference parameter which the
     *	ParamField can use as orientation if the user
     *	chooses a different unit.
     *	(cf. Param.transform())
     */
    public void setReference( Param reference )
    {
        this.reference = reference;
    }

    /**
     *	Selects a second ParamField as reference.
     *	This ParamField will use the reference when
     *	the user chooses a different unit.
     *	(cf. Param.transform())
     *
     *	This field has higher priority than a value
     *	specified through <code>setReference(Param)</code>!
     */
    public void setReference( ParamField refField )
    {
        this.refField = refField;
    }

    /**
     *	Creates a reference value with <code>ABS_UNIT</code> which
     *	can be used by another <code>Param(Field)</code>  als a <code>Param.transform()</code> reference.
     *
     *	@return	possibly <code>null</code>!
     */
    public Param makeReference() {
        int		destUnit = (currentSpace.unit & ~Param.FORM_MASK) | Param.ABS_UNIT;
        Param	tempPara;
        final Param oldParam	= getParam();

        tempPara = Param.transform(oldParam, destUnit, (refField == null) ?
                reference : refField.makeReference(), null);

        if (tempPara == oldParam) {
            return (Param) tempPara.clone();    // since this.para is non-"static"
        } else {
            return tempPara;
        }
    }

    public Param getParam() {
        return new Param(ggNumber.getNumber().doubleValue(),
                currentSpace == null ? Param.NONE : currentSpace.unit);
    }

    public void addParamListener( ParamListener li )
    {
        elm.addListener( li );
    }

    public void removeParamListener( ParamListener li )
    {
        elm.removeListener( li );
    }

    protected void fireValueChanged(boolean adjusting) {
        dispatchChange();
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

    protected void dispatchChange() {
        elm.dispatchEvent(new ParamEvent(this, ParamEvent.CHANGED, System.currentTimeMillis(),
                getParam(), getSpace()));
    }

    public void setEnabled(boolean state) {
        if (!state) {
            ggJog.requestFocus();    // tricky ggNumber.looseFocus() ;)
        }

        ggNumber.setEnabled(state);
        ggJog.setEnabled(state);
        if (ggUnits.isVisible()) {
            ggUnits.setEnabled(state);
        }

        if (state) {
            ggNumber.requestFocus();
        }
    }

    /*
     *	Determines string representation of the unit
     */
    private String getUnitText(int unit) {
        String unitTxt = specialTxt[ (unit & Param.SPECIAL_MASK) >> 8 ];

        switch( unit & Param.FORM_MASK ) {
        case Param.ABS_UNIT:
            if( unitTxt.length() == 0 ) {
                unitTxt = dimTxt[ unit & Param.DIM_MASK ];
            }
            break;
        case Param.ABS_PERCENT:
            unitTxt = ((unitTxt.length() == 0) ? percentTxt : unitTxt);
            break;
        case Param.REL_UNIT:
            unitTxt = relTxt + ' ' + ((unitTxt.length() == 0) ?
                      dimTxt[ unit & Param.DIM_MASK ] : unitTxt);
            break;
        case Param.REL_PERCENT:
            unitTxt = relTxt + ' ' + ((unitTxt.length() == 0) ? percentTxt : unitTxt);
            break;
        default:
            break;
        }

        return unitTxt;
    }
}