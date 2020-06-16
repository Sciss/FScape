/*
 *  Processor.java
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
 *		21-May-05	uses ProcessorListener
 */

package de.sciss.fscape.proc;

public interface Processor
        extends Runnable {

    public void start();
    public void pause();
    public void resume();
    public void stop();

    public void addProcessorListener   (ProcessorListener li);
    public void removeProcessorListener(ProcessorListener li);

    public float getProgression();

    public Exception getError();

    public void setError(Exception e);
}