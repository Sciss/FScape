/*
 *  ProcessorEvent.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	created
 */

package de.sciss.fscape.proc;

import de.sciss.app.BasicEvent;

public class ProcessorEvent
        extends BasicEvent {

    public static final int STARTED		= 0;
    public static final int STOPPED		= 1;
    public static final int PAUSED		= 2;
    public static final int RESUMED		= 3;
    public static final int PROGRESS	= 4;

    private final Processor proc;

    /**
     *  Constructs a new <code>PathEvent</code>
     *
     *  @param  source  who originated the action
     *  @param  ID		<code>CHANGED</code>
     *  @param  when	system time when the event occured
     */
    public ProcessorEvent( Object source, int ID, long when, Processor proc )
    {
        super( source, ID, when );

        this.proc		= proc;
    }

    /**
     *  Queries the processor
     *
     *  @return the processor who emitted the event
     */
    public Processor getProcessor()
    {
        return proc;
    }

    public boolean incorporate( BasicEvent oldEvent )
    {
        if( oldEvent instanceof ProcessorEvent &&
            this.getSource() == oldEvent.getSource() &&
            this.getID() == oldEvent.getID() &&
            this.proc == ((ProcessorEvent) oldEvent).proc ) {

            return true;

        } else return false;
    }
}
