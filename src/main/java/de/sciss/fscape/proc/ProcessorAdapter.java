/*
 *  ProcessorAdapter.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
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

public class ProcessorAdapter
		implements ProcessorListener {
	public void processorStarted (ProcessorEvent e) {}
    public void processorStopped (ProcessorEvent e) {}
    public void processorPaused  (ProcessorEvent e) {}
    public void processorResumed (ProcessorEvent e) {}
    public void processorProgress(ProcessorEvent e) {}
}