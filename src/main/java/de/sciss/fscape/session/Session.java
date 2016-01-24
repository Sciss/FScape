/*
 *  Session.java
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
 *		25-Jan-05	created
 *		15-Jul-05	removed SessionChangeListener stuff. relies more on SessionCollection.Listener
 *					for doc.tracks
 *		21-Jan-06	implements OSCRouter ; moved a lot of actions from DocumentFrame to this class
 *		24-Jun-06	created from de.sciss.eisenkraut.session.Session
 */

package de.sciss.fscape.session;

import de.sciss.common.ProcessingThread;
import de.sciss.fscape.Application;
import de.sciss.fscape.net.OSCRoot;
import de.sciss.fscape.net.OSCRouter;
import de.sciss.fscape.net.OSCRouterWrapper;
import de.sciss.fscape.net.RoutedOSCMessage;
import de.sciss.util.Flag;

import javax.swing.undo.UndoManager;
import java.awt.*;
import java.io.File;

public class Session
        implements OSCRouter {

    private ModulePanel frame = null;

    // ---  ---
    private boolean							dirty			= false;

    private final Session					enc_this		= this;

    private static int						nodeIDAlloc		= 0;
    private final int						nodeID;

    private final OSCRouterWrapper osc;

    private File file			= null;

    private ProcessingThread				currentPT		= null;

    public Session()
    {
        nodeID				= ++nodeIDAlloc;
        osc					= new OSCRouterWrapper( null, this );
    }

    public int getNodeID()
    {
        return nodeID;
    }

    /**
     * 	Checks if a process is currently running. This method should be called
     * 	before launching a process using the <code>start()</code> method.
     * 	If a process is ongoing, this method waits for a default timeout period
     * 	for the thread to finish.
     *
     *	@return	<code>true</code> if a new process can be launched, <code>false</code>
     *			if a previous process is ongoing and a new process cannot be launched
     *	@throws	IllegalMonitorStateException	if called from outside the event thread
     *	@synchronization	must be called in the event thread
     */
    public boolean checkProcess()
    {
        return checkProcess( 500 );
    }

    /**
     * 	Checks if a process is currently running. This method should be called
     * 	before launching a process using the <code>start()</code> method.
     * 	If a process is ongoing, this method waits for a given timeout period
     * 	for the thread to finish.
     *
     * 	@param	timeout	the maximum duration in milliseconds to wait for an ongoing process
     *	@return	<code>true</code> if a new process can be launched, <code>false</code>
     *			if a previous process is ongoing and a new process cannot be launched
     *	@throws	IllegalMonitorStateException	if called from outside the event thread
     *	@synchronization	must be called in the event thread
     */
    public boolean checkProcess( int timeout )
    {
//System.out.println( "checking..." );
        if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();
        if( currentPT == null ) return true;
        if( timeout == 0 ) return false;

//System.out.println( "sync " + timeout );
        currentPT.sync( timeout );
//System.out.println( "sync done" );
        return( (currentPT == null) || !currentPT.isRunning() );
    }

    /**
     * 	Starts a <code>ProcessingThread</code>. Only one thread
     * 	can exist at a time. To ensure that no other thread is running,
     * 	call <code>checkProcess()</code>.
     *
     * 	@param	process	the thread to launch
     * 	@throws	IllegalMonitorStateException	if called from outside the event thread
     * 	@throws	IllegalStateException			if another process is still running
     * 	@see	#checkProcess()
     * 	@synchronization	must be called in the event thread
     */
    public void start( ProcessingThread process )
    {
        if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();
        if( this.currentPT != null ) throw new IllegalStateException( "Process already running" );

        currentPT = process;
        currentPT.addListener( new ProcessingThread.Listener() {
            public void processStarted( ProcessingThread.Event e ) {}
            public void processStopped( ProcessingThread.Event e )
            {
                currentPT = null;
            }
        });
        currentPT.start();
    }

    public ModulePanel getFrame()
    {
        return frame;
    }

    public void setFrame( ModulePanel frame )
    {
        this.frame = frame;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File f )
    {
        file = f;
    }

    public String getName() {
        if (file != null) {
            String f = file.getName();
            int i = f.lastIndexOf('.');
            return i < 0 ? f : f.substring(0, i);
        } else {
            return getResourceString("Untitled");
        }
    }

    private void updateTitle()
    {
        // if( frame != null ) frame.updateTitle();
    }

    private String getResourceString( String key )
    {
        return key;
    }

    public ProcessingThread closeDocument(boolean force, Flag wasClosed) {
        return frame.closeDocument(force, wasClosed);    // XXX should be in here not frame!!!
    }

    // ------------- OSCRouter interface -------------

    public String oscGetPathComponent()
    {
        return null;
    }

    public void oscRoute( RoutedOSCMessage rom )
    {
        osc.oscRoute( rom );
    }

    public void oscAddRouter( OSCRouter subRouter )
    {
        if( osc != null ) osc.oscAddRouter( subRouter );
    }

    public void oscRemoveRouter( OSCRouter subRouter )
    {
        if( osc != null ) osc.oscRemoveRouter( subRouter );
    }

    public void oscCmd_close(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        final ProcessingThread pt;
        final boolean force;

        try {
            if (rom.msg.getArgCount() > 1) {
                force = ((Number) rom.msg.getArg(1)).intValue() != 0;
            } else {
                force = false;
            }
            pt = closeDocument(force, new Flag(false));
            if (pt != null) {
                pt.start();
//				pt.addListener(new ProcessingThread.Listener() {
//					@Override
//					public void processStarted(ProcessingThread.Event e) {
//						System.out.println("PROC STARTED");
//					}
//
//					@Override
//					public void processStopped(ProcessingThread.Event e) {
//						System.out.println("PROC STOPPED");
//					}
//				});
            } else {
//				System.out.println("WAS CLOSED");
            }

        } catch (IndexOutOfBoundsException e1) {
            OSCRoot.failedArgCount(rom);
        } catch (ClassCastException e1) {
            OSCRoot.failedArgType(rom, 1);
        }
    }

    public void oscCmd_start(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        frame.start();
    }

    public void oscCmd_stop(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        frame.stop();
    }

    public void oscCmd_pause(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        frame.pause();
    }

    public void oscCmd_resume(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        frame.resume();
    }

    public void oscCmd_activate(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }

        // frame.setVisible(true);
        Application.documentHandler.setActive(this);
        // frame.toFront();
//		frame.requestFocus();
    }

    public Object oscQuery_id() {
        return new Integer(getNodeID());
    }

    public Object oscQuery_process() {
        if (frame == null) {
            return null;
        }

        final String className 	= frame.getClass().getName();
        final int i 			= className.lastIndexOf('.');
        final boolean dlg 		= className.endsWith("Dlg");

        return className.substring(i + 1, className.length() - (dlg ? 3 : 0));
    }

    public Object oscQuery_running() {
        if (frame == null) {
            return null;
        }

        return new Integer(frame.isThreadRunning() ? 1 : 0);
    }

    public Object oscQuery_progression() {
        if (frame == null) {
            return null;
        }

        return new Float(frame.getProgression());
    }

    public Object oscQuery_error() {
        if (frame == null) {
            return null;
        }

        final Exception e = frame.getError();

        return (e == null ? "" : e.getClass().getName() + " : " + e.getLocalizedMessage());
    }

    public Object oscQuery_dirty() {
        return new Integer(isDirty() ? 1 : 0);
    }

    public Object oscQuery_name() {
        return getName(); // getDisplayDescr().file.getName();
    }

    public Object oscQuery_file() {
        return (file == null ? "" : file.getAbsolutePath());
    }

// ---------------- Document interface ----------------

    public UndoManager getUndoManager()
    {
        throw new IllegalStateException("Not implemented");
        // return undo;
    }

    public void dispose()
    {
        // undo.discardAllEdits();

        if( osc != null ) {
            osc.remove();
//			osc = null;
        }

//		if( frame != null ) {
////			frame.setVisible( false );
//			frame.dispose();
//			frame = null;
//		}
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty( boolean dirty )
    {
        if( !this.dirty == dirty ) {
            this.dirty = dirty;
            updateTitle();
        }
    }
}
