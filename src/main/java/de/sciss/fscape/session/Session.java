/*
 *  Session.java
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
 *		25-Jan-05	created
 *		15-Jul-05	removed SessionChangeListener stuff. relies more on SessionCollection.Listener
 *					for doc.tracks
 *		21-Jan-06	implements OSCRouter ; moved a lot of actions from DocumentFrame to this class
 *		24-Jun-06	created from de.sciss.eisenkraut.session.Session
 */

package de.sciss.fscape.session;

import java.awt.EventQueue;
import java.io.*;

import de.sciss.fscape.net.*;

import de.sciss.app.*;
import de.sciss.common.BasicDocument;
import de.sciss.common.ProcessingThread;
import de.sciss.io.*;
import de.sciss.util.*;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class Session
extends BasicDocument
implements OSCRouter	// EventManager.Processor
{
// XXX FFFF
//	private AudioFileDescr					displayAFD;
//	private AudioFileDescr[]				afds;
//	
//	public final Timeline					timeline;
//
//	private AudioTrail						mte				= null;
//	private DecimatedTrail					dt				= null;
//
//	public final MarkerTrail				markers;
//	public final MarkerTrack				markerTrack;
//
//	/**
//	 *  Use this <code>LockManager</code> to gain access to
//	 *  <code>receiverCollection</code>, <code>transmitterCollection</code>,
//	 *  <code>timeline</code> and a transmitter's <code>AudioTrail</code>.
//	 */
//	public final LockManager				bird			= new LockManager( 3 );
//	
//	/**
//	 *  Bitmask for putting a lock on the <code>timeline</code>
//	 */
//	public  static final int				DOOR_TIME		= 0x01;
//	/**
//	 *  Bitmask for putting a lock on a <code>AudioTrail</code>
//	 */
//	public  static final int				DOOR_MTE		= 0x02;
//	/**
//	 *  Bitmask for putting a lock on the channel tracks
//	 */
//	public  static final int				DOOR_TRACKS		= 0x04;
//
//	public static final int					DOOR_ALL		= DOOR_TIME | DOOR_TRACKS | DOOR_MTE;
//	
//	public final SessionCollection			tracks			= new SessionCollection();	// should be tracking audioTracks automatically
//	public final SessionCollection			audioTracks		= new SessionCollection();
//	public final SessionCollection			selectedTracks	= new SessionCollection();
//
//	private Transport						transport		= null;
	private ModulePanel frame			= null;
	
	// --- actions ---

	private final ActionSave			actionSave;

	// ---  ---
	// private final de.sciss.app.UndoManager	undo			= new de.sciss.app.UndoManager( this );
	private boolean							dirty			= false;

	private final Session					enc_this		= this;

	private static int						nodeIDAlloc		= 0;
	private final int						nodeID;

	private final OSCRouterWrapper			osc;
	
	private File							file			= null;
	
	private ProcessingThread				currentPT		= null;

	public Session()
	{
		nodeID				= ++nodeIDAlloc;
		
		osc					= new OSCRouterWrapper( null, this );

		actionSave			= new ActionSave();
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

//	public void clear( Object source )
//	throws IOException
//	{
//		timeline.clear( source );
//		selectedTracks.clear( source );
//		audioTracks.clear( source );
//		tracks.clear( source );
//		if( mte != null ) {
//			mte.clear( null );
//		}
//		markers.clear( source );
//		updateTitle();
//	}
	
// XXX FFFF
//	// pausing dispatcher is up to the caller!
//	// should ensure that old mte was cleared!
//	public void setAudioTrail( Object source, AudioTrail mte )
//	{
//		this.mte = mte;
////		tracks.pauseDispatcher();
//		audioTracks.clear( source );
//		
//		AudioTrack t;
//		java.util.List	collNewTracks = new ArrayList();
//		final int		numChannels = mte.getChannelNum();
//		final double	deltaAngle	= 360.0 / numChannels;
//		final double	startAngle	= numChannels < 2 ? 0.0 : -deltaAngle/2;	// reasonable for mono to octo
//		
//		for( int ch = 0; ch < mte.getChannelNum(); ch++ ) {
//			t = new AudioTrack( mte, ch );
//			t.setName( String.valueOf( ch + 1 ));
//			t.getMap().putValue( source, AudioTrack.MAP_KEY_PANAZIMUTH, new Double( startAngle + ch * deltaAngle ));
//			collNewTracks.add( t );
//		}
//		audioTracks.addAll( source, collNewTracks );
//		tracks.addAll( source, collNewTracks );
//		selectedTracks.addAll( source, collNewTracks );
////		tracks.resumeDispatcher();
////		dispatchChange( source );
//
//		updateTitle();
//	}
//
//	public AudioTrail getAudioTrail()
//	{
//		return mte;
//	}
//	
//	public void setDecimatedTrail( DecimatedTrail dt )
//	{
//		this.dt = dt;
//	}
//	
//	public DecimatedTrail getDecimatedTrail()
//	{
//		return dt;
//	}
//	
//	public void setDescr( AudioFileDescr[] afds )
//	{
//		this.afds = afds;
//		autoCreateDisplayDescr();
//		updateTitle();
//	}
//
//	private void autoCreateDisplayDescr()
//	{
//		if( afds.length == 0 ) {
//			displayAFD.file				= null;
//		} else {
//			final AudioFileDescr proto	= afds[ 0 ];
//			displayAFD.type				= proto.type;
//			displayAFD.rate				= proto.rate;
//			displayAFD.bitsPerSample	= proto.bitsPerSample;
//			displayAFD.sampleFormat		= proto.sampleFormat;
//			displayAFD.length			= proto.length;
//			displayAFD.channels			= proto.channels;
//
//			if( proto.file == null ) {
//				displayAFD.file			= null;
//			} else {
//				final String			name	= proto.file.getName();
//				int						left	= name.length();
//				int						right	= 0;
//				String					name2;
//				int						trunc;
//
//				displayAFD.type				= proto.type;
//				displayAFD.rate				= proto.rate;
//				displayAFD.bitsPerSample	= proto.bitsPerSample;
//				displayAFD.sampleFormat		= proto.sampleFormat;
//				displayAFD.length			= proto.length;
//				displayAFD.channels			= proto.channels;
//				
//				for( int i = 1; i < afds.length; i++ ) {
//					name2				 = afds[ i ].file.getName();
//					displayAFD.channels	+= afds[ i ].channels;
//					for( trunc = 0; trunc < Math.min( name2.length(), left ); trunc++ ) {
//						if( !(name2.charAt( trunc ) == name.charAt( trunc ))) break;
//					}
//					left	= trunc;
//					for( trunc = 0; trunc < Math.min( name2.length(), name.length() - right ); trunc++ ) {
//						if( !(name2.charAt( name2.length() - trunc - 1 ) == name.charAt( name.length() - trunc - 1 ))) break;
//					}
//					right	= trunc;
//				}
//				
//				if( left >= name.length() - right ) {
//					displayAFD.file	= afds[ 0 ].file;
//				} else {
//					final StringBuffer strBuf = new StringBuffer();
//					strBuf.append( name.substring( 0, left ));
//					for( int i = 0; i < afds.length; i++ ) {
//						strBuf.append( i == 0 ? '[' : ',' );
//						name2 = afds[ i ].file.getName();
//						strBuf.append( name2.substring( left, name2.length() - right ));
//					}
//					strBuf.append( ']' );
//					strBuf.append( name.substring( name.length() - right ));
//					displayAFD.file	= new File( afds[ 0 ].file.getParentFile(), strBuf.toString() );
//				}
//			}
//		}
//	}
//	
	public File getFile()
	{
		return file;
	}
	
	public void setFile( File f )
	{
		file = f;
	}

	public String getName()
	{
		if( file != null ) {
			return file.getName();
		} else {
			return getResourceString( "frameUntitled" );
		}
	}

	private void updateTitle()
	{
		// if( frame != null ) frame.updateTitle();
	}

// XXX FFFF
//	public ProcessingThread procDelete( String name, Span span, int mode )
//	{
//		return actionDelete.initiate( name, span, mode );
//	}

	public ProcessingThread procSave( String name, Span span, AudioFileDescr[] afds, boolean asCopy )
	{
		return actionSave.initiate( name, span, afds, asCopy );
	}
	
// XXX FFFF
//	public MenuAction getCutAction()
//	{
//		return actionCut;
//	}
//
//	public MenuAction getCopyAction()
//	{
//		return actionCopy;
//	}
//
//	public MenuAction getPasteAction()
//	{
//		return actionPaste;
//	}
//
//	public MenuAction getDeleteAction()
//	{
//		return actionDelete;
//	}
//	
//	public MenuAction getSilenceAction()
//	{
//		return actionSilence;
//	}
//	
//	public MenuAction getTrimAction()
//	{
//		return actionTrim;
//	}
	
	private String getResourceString( String key )
	{
		return AbstractApplication.getApplication().getResourceString( key );
	}

// XXX FFFF
//	public ProcessingThread insertSilence( long pos, long numFrames )
//	{
//		return actionSilence.initiate( pos, numFrames );
//	}

	public ProcessingThread closeDocument( boolean force, Flag wasClosed )
	{
		return frame.closeDocument( force, wasClosed );	// XXX should be in here not frame!!!
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

	public void oscCmd_close( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		final ProcessingThread	pt;
		final boolean			force;

		try {
			if( rom.msg.getArgCount() > 1 ) {
				force = ((Number) rom.msg.getArg( 1 )).intValue() != 0;
			} else {
				force = false;
			}
			pt = closeDocument( force, new Flag( false ));
			if( pt != null ) pt.start();
		}
		catch( IndexOutOfBoundsException e1 ) {
			OSCRoot.failedArgCount( rom );
			return;
		}
		catch( ClassCastException e1 ) {
			OSCRoot.failedArgType( rom, 1 );
		}
	}

	public void oscCmd_start( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		frame.start();
	}

	public void oscCmd_stop( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		frame.stop();
	}

	public void oscCmd_pause( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		frame.pause();
	}

	public void oscCmd_resume( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		frame.resume();
	}

	public void oscCmd_activate( RoutedOSCMessage rom )
	{
		if( frame == null ) {
			OSCRoot.failed( rom.msg, getResourceString( "errWindowNotFound" ));
		}
	
		frame.setVisible( true );
		// frame.toFront();
//		frame.requestFocus();
	}

// XXX FFFF
//	public void oscCmd_cut( RoutedOSCMessage rom )
//	{
//		actionCut.perform();
//	}
//	
//	public void oscCmd_copy( RoutedOSCMessage rom )
//	{
//		actionCopy.perform();
//	}
//	
//	public void oscCmd_paste( RoutedOSCMessage rom )
//	{
//		actionPaste.perform();
//	}
//
//	public void oscCmd_delete( RoutedOSCMessage rom )
//	{
//		actionDelete.perform();
//	}
//
//	public void oscCmd_insertsilence( RoutedOSCMessage rom )
//	{
//		final long				pos, numFrames;
//		final ProcessingThread	pt;
//		int						argIdx	= 1;
//	
//		if( !bird.attemptShared( DOOR_TIME, 250 )) return;
//		try {
//			pos			= timeline.getPosition();
//			numFrames	= Math.max( 0, Math.min( timeline.getLength() - pos, ((Number) rom.msg.getArg( argIdx )).longValue() ));
//		}
//		catch( IndexOutOfBoundsException e1 ) {
//			OSCRoot.failedArgCount( rom );
//			return;
//		}
//		catch( ClassCastException e1 ) {
//			OSCRoot.failedArgType( rom, argIdx );
//			return;
//		}
//		finally {
//			bird.releaseExclusive( Session.DOOR_TIME );
//		}
//		
//		pt = actionSilence.initiate( pos, numFrames );
//		if( pt != null ) pt.start();
//	}
//
//	public void oscCmd_trim( RoutedOSCMessage rom )
//	{
//		actionTrim.perform();
//	}

	public Object oscQuery_id()
	{
		return new Integer( getNodeID() );
	}

	public Object oscQuery_process()
	{
		if( frame == null ) {
			return null;
		}
		
		final String	className	= frame.getClass().getName();
		final int		i			= className.lastIndexOf( '.' );
		final boolean	dlg			= className.endsWith( "Dlg" );
		
		return className.substring( i + 1, className.length() - (dlg ? 3 : 0) );
	}

	public Object oscQuery_running()
	{
		if( frame == null ) {
			return null;
		}
	
		return new Integer( frame.isThreadRunning() ? 1 : 0 );
	}

	public Object oscQuery_progression()
	{
		if( frame == null ) {
			return null;
		}
	
		return new Float( frame.getProgression() );
	}

	public Object oscQuery_error()
	{
		if( frame == null ) {
			return null;
		}
		
		final Exception e = frame.getError();
		
		return( e == null ? "" : e.getClass().getName() + " : " + e.getLocalizedMessage() );
	}

	public Object oscQuery_dirty()
	{
		return new Integer( isDirty() ? 1 : 0 );
	}
	
// XXX FFFF
//	public Object oscQuery_editmode()
//	{
//		return EDITMODES[ getEditMode() ];
//	}
	
	public Object oscQuery_name()
	{
		return getName(); // getDisplayDescr().file.getName();
	}

	public Object oscQuery_file()
	{
		return( file == null ? "" : file.getAbsolutePath() );
	}

// ---------------- Document interface ----------------

	public de.sciss.app.Application getApplication()
	{
		return AbstractApplication.getApplication();
	}

    public de.sciss.app.UndoManager getUndoManager()
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
	
// XXX FFFF
//		if( transport != null ) {
//			transport.quit();
//			transport = null;
//		}
		if( frame != null ) {
//			frame.setVisible( false );
			frame.dispose();
			frame = null;
		}
// XXX FFFF
//		if( mte != null ) {
////			mte.dispose();
//			mte.clear( null );
//			mte = null;
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
	
// ------------------ internal classes ------------------
	
	protected class ActionSave
	implements ProcessingThread.Client
	{
		/**
		 *  Initiate the save process.
		 *  Transport is stopped before, if it was running.
		 *  On success, undo history is purged and
		 *  <code>setModified</code> and <code>updateTitle</code>
		 *  are called, and the file is added to
		 *  the Open-Recent menu. Note that returned
		 *	process has not yet been started, as to allow
		 *	other objects to add listeners. So it's the
		 *	job of the caller to invoke the processing thread's
		 *	<code>start</code> method.
		 *
		 *  @synchronization	this method is to be called in the event thread
		 */
		protected ProcessingThread initiate( String name, Span span, AudioFileDescr[] afds, boolean asCopy )
		{
			final ProcessingThread pt;
		
// XXX FFFF
//			getTransport().stopAndWait();
//			Object[] args	= new Object[ 5 ];
//			args[ 0 ]		= afds;
//			args[ 1 ]		= enc_this;
//			args[ 2 ]		= new Boolean( asCopy );
//			args[ 3 ]		= span == null ? new Span( 0, timeline.getLength() ) : span;
//			pt				= new ProcessingThread( this, getFrame(), bird, name, args, Session.DOOR_ALL );
			pt				= new ProcessingThread( this, getFrame(), name );
pt.putClientArg( "afds", afds );
pt.putClientArg( "doc", enc_this );
pt.putClientArg( "copy", new Boolean( asCopy ));
			return pt;
		}

		public int processRun( ProcessingThread context )
		{
// XXX FFFF
return FAILED;
//			final Object[]					args		= (Object[]) argument;
//			final AudioFileDescr[]			afds		= (AudioFileDescr[]) args[ 0 ];
//			final int						numFiles	= afds.length;
//			final Session					doc			= (Session) args[ 1 ];
//			final boolean					asCopy		= ((Boolean) args[ 2 ]).booleanValue();
//			final Span						span		= (Span) args[ 3 ];
//			final AudioTrail				at			= doc.getAudioTrail();
//			final File[]					tempFs		= new File[ numFiles ];
//			final boolean[]					renamed		= new boolean[ numFiles ];
//			final InterleavedStreamFile[]	afs			= new AudioFile[ numFiles ];
//			final StringBuffer				excTxt		= new StringBuffer();
//			boolean							success		= false;
////			SyncCompoundEdit				edit		= new BasicSyncCompoundEdit( doc.bird, Session.DOOR_ALL );
//
//			try {
//				if( afds[ 0 ].isPropertySupported( AudioFileDescr.KEY_MARKERS )) {
//					doc.markers.copyToAudioFile( afds[ 0 ], span );	// XXX
//				}
//				for( int i = 0; i < numFiles; i++ ) {
//					if( afds[ i ].file.exists() ) {
//						tempFs[ i ] = File.createTempFile( "eis", null, afds[ i ].file.getParentFile() );
//						tempFs[ i ].delete();
//						if( !afds[ i ].file.renameTo( tempFs[ i ] )) {
//							excTxt.append( getResourceString( "errRenameFile" ) +
//								"\n" + afds[ i ].file.getName() + " \u2192 " + tempFs[ i ].getName() + "\n" );
//							return FAILED;
//						}
//						renamed[ i ] = true;
//					}
//					afs[ i ] = AudioFile.openAsWrite( afds[ i ]);
//				}
////if( true ) return CANCELLED;
//// UUU
////				if( asCopy ) {
//					if( !at.flatten( afs, context, span )) return CANCELLED;
//					for( int i = 0; i < numFiles; i++ ) {
//						afs[ i ].close();
//						afs[ i ] = null;
//					}
////				} else {
////					if( !mte.flattenAndExchange( afs, context, 0.0f, 1.0f, root.cacheManager )) return CANCELLED;
////					for( int i = 0; i < numFiles; i++ ) {
////						afs[ i ] = null;
////					}
////				}
//				
//				for( int i = 0; i < numFiles; i++ ) {
//					if( (tempFs[ i ] != null) && !tempFs[ i ].delete() ) {
//						excTxt.append( getResourceString( "errDeleteFile" ) + "\n" +
//							tempFs[ i ].getAbsolutePath() + "\n" );
//					}
//				}
//				if( excTxt.length() > 0 ) return FAILED;
//
//				success	 = true;
//				return DONE;
//			}
//			catch( IOException e1 ) {
//				excTxt.append( e1.getLocalizedMessage() );
//				excTxt.append( '\n' );
//				return FAILED;
//			}
//			finally {
//				if( !success ) {
//					for( int i = 0; i < numFiles; i++ ) {
//						if( afs[ i ] != null ) {
//							try {
//								afs[ i ].close();
//							} catch( IOException e1 ) {}
//							afds[ i ].file.delete();
//							afs[ i ] = null;
//						}
//						if( renamed[ i ] && tempFs[ i ].renameTo( afds[ i ].file )) {
//							excTxt.append( getResourceString( "errBackupExists" ) + "\n" + tempFs[ i ].getAbsolutePath() + "\n" );
//						}
//					}
//					if( excTxt.length() > 0 ) {
//						context.setException( new IOException( excTxt.toString() ));
//					}
//				}
//			}
		} // run

		public void processFinished( ProcessingThread context ) {}
		public void processCancel( ProcessingThread context ) {}
	}
	
// XXX FFFF
//	private class actionCutClass
//	extends MenuAction
//	{
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//		
//		private void perform()
//		{
//			final ProcessingThread pt; // = null;
//			
//			if( actionCopy.perform() ) {
//				if( !bird.attemptShared( Session.DOOR_TIME )) return;
//				try {
//					pt = procDelete( getValue( NAME ).toString(), timeline.getSelectionSpan(), getEditMode() );
//				}
//				finally {
//					bird.releaseShared( Session.DOOR_TIME | Session.DOOR_MTE );
//				}
//				if( pt != null ) pt.start();
//			}
//		}
//	}
//
//	private class actionCopyClass
//	extends MenuAction
//	{
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//
//		private ClipboardTrackList getSelectionAsTrackList()
//		{
//			final Span span;
//			
//			if( !bird.attemptShared( Session.DOOR_TIME | Session.DOOR_TRACKS, 250 )) return null;
//			try {
//				span = timeline.getSelectionSpan();
//				if( span.isEmpty() ) return null;
//
//				return new ClipboardTrackList( enc_this );
//			}
//			finally {
//				bird.releaseShared( Session.DOOR_TIME | Session.DOOR_TRACKS );
//			}
//		}
//
//		private boolean perform()
//		{
//			boolean						success	= false;
//			final ClipboardTrackList	tl		= getSelectionAsTrackList();
//
//			if( tl == null ) return success;
//
//			try {
//				AbstractApplication.getApplication().getClipboard().setContents( tl, tl );
//				success = true;
//			}
//			catch( IllegalStateException e1 ) {
//				System.err.println( getResourceString( "errClipboard" ));
//			}
//
//			return success;
//		}
//	}
//	
//	private class actionPasteClass
//	extends MenuAction
//	implements ProcessingThread.Client
//	{
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//		
//		private void perform()
//		{
//			perform( getValue( NAME ).toString(), getEditMode() );
//		}
//		
//		private void perform( String name, int mode )
//		{
//			final Transferable			t;
//			final ClipboardTrackList	tl;
//
//			try {
//				t = AbstractApplication.getApplication().getClipboard().getContents( this );
//				if( t == null ) return;
//				
//				if( !t.isDataFlavorSupported( ClipboardTrackList.trackListFlavor )) return;
//				tl = (ClipboardTrackList) t.getTransferData( ClipboardTrackList.trackListFlavor );
//			}
//			catch( IOException e11 ) {
//				System.err.println( e11.getLocalizedMessage() );
//				return;
//			}
//			catch( UnsupportedFlavorException e12 ) {
//				System.err.println( e12.getLocalizedMessage() );
//				return;
//			}
//			
//			pasteTrackListToDocument( tl, timeline.getPosition(), name, mode );	// XXX sync
//		}
//
//		private void pasteTrackListToDocument( ClipboardTrackList tl, long insertPos, String name, int mode )
//		{
//			final ProcessingThread	pt;
//			final Object[]			args;
//			final java.util.List	tis;
//			Track.Info				ti;
//		
//			if( !bird.attemptShared( Session.DOOR_TRACKS, 250 )) return;
//			try {
//				args = new Object[] {
//					enc_this, tl, new Long( insertPos ), null, new Integer( mode ),
//					Track.getInfos( selectedTracks.getAll(), tracks.getAll() ), null };
//			}
//			finally {
//				bird.releaseShared( Session.DOOR_TRACKS );
//			}
//			pt	= new ProcessingThread( this, getFrame(), root, bird, name, args, Session.DOOR_ALL );
//			pt.start();
//		}
//		
//		// --------- ProcessingThread.Client interface ---------
//
//		/**
//		 *  This method is called by ProcessingThread
//		 */
//		public int processRun( ProcessingThread context, Object argument )
//		{
//			final Object[]					args				= (Object[]) argument;
//			final Session					doc					= (Session) args[0];
//			final ClipboardTrackList		tl					= (ClipboardTrackList) args[1];
//			final long						insertPos			= ((Long) args[2]).longValue();
//			final int						mode				= ((Integer) args[4]).intValue();
//			final java.util.List			tis					= (java.util.List) args[5];
//			final long						docLength			= doc.timeline.getLength();
//			final long						pasteLength;
//			final Span						oldSelSpan			= doc.timeline.getSelectionSpan();
//			final SyncCompoundEdit			edit;
////			final BlendContext				bc;
//			final Span						insertSpan, copySpan;
//			final long						delta				= insertPos - tl.getSpan().start;
//			final boolean					expTimeline, cutTimeline;
//			final Span						cutTimelineSpan;
////			Span							readSpan, writeSpan;
//			Track.Info						ti;
//			Trail							srcTrail;
//			AudioTrail						at;
//			boolean[]						trackMap;
//			boolean							isAudio;
//			boolean							hasSelectedAudio	= false;
//			boolean							success				= false;
//			boolean							b;
//
//			if( mode == EDIT_INSERT ) {
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti		= (Track.Info) tis.get( i );
//					isAudio = ti.trail instanceof AudioTrail;
//					if( !ti.getChannelSync() ) {
//						context.setException( new IllegalStateException( getResourceString( "errAudioWillLooseSync" )));
//						return FAILED;
//					}
//					if( isAudio && ti.selected ) {
//						hasSelectedAudio = true;
//					}
//				}
//			}
//
//			edit			= new BasicSyncCompoundEdit( doc.bird, Session.DOOR_ALL, context.getName() );
////			edit			= new SyncCompoundSessioObjEdit( this, doc.bird, Session.DOOR_ALL, doc.audioTracks.getAll(), // XXX getAll
////																AudioTrack.OWNER_WAVE, null, null, context.getName() );
//			args[ 6 ]		= edit;
//
//			expTimeline		= (mode == EDIT_INSERT) && hasSelectedAudio;
//			cutTimeline		= (mode == EDIT_INSERT) && !hasSelectedAudio;
//			pasteLength		= expTimeline ? tl.getSpan().getLength() :
//											Math.min( tl.getSpan().getLength(), docLength - insertPos );
//			cutTimelineSpan	= cutTimeline ? new Span( doc.timeline.getLength(), doc.timeline.getLength() + pasteLength ) : null;
//			copySpan		= new Span( tl.getSpan().start, tl.getSpan().start + pasteLength );
//			insertSpan		= new Span( insertPos, insertPos + pasteLength );
//
//// UUU
////			bc			= createBlendContext(
////				Math.min( pasteLength, Math.min( insertPos, docLength - insertPos )) / 2 );
//
//			try {
//				if( !oldSelSpan.isEmpty() ) { // deselect
//					edit.addPerform( TimelineVisualEdit.select( this, doc, new Span() ));
////							insertPos = oldSelSpan.getStart();
//				}
//
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti		= (Track.Info) tis.get( i );
//					try {
//						ti.trail.editBegin( edit );
//						isAudio	= ti.trail instanceof AudioTrail;
//						if( ti.selected ) {
//							if( mode == EDIT_INSERT ) {
//								ti.trail.editInsert( this, insertSpan, edit );
//								if( cutTimeline ) ti.trail.editRemove( this, cutTimelineSpan, edit );
//							} else if( (mode == EDIT_OVERWRITE) || isAudio ) { // Audio needs to be cleared even in Mix mode!
//								ti.trail.editClear( this, insertSpan, edit );
//							}
//							
//							if( isAudio ) {
//								at			= (AudioTrail) ti.trail;
//								srcTrail	= tl.getSubTrail( ti.trail.getClass() );
//								trackMap	= tl.getTrackMap( ti.trail.getClass() );
//								
////System.err.println( "clipboard tm : " );
////for( int x = 0; x < trackMap.length; x++ ) { System.err.println( "  " + trackMap[ x ]); }
//								int[] trackMap2 = new int[ at.getChannelNum() ];
//								for( int j = 0, k = 0; j < trackMap2.length; j++ ) {
//									if( ti.trackMap[ j ]) {	// target track selected
//										for( ; (k < trackMap.length) && !trackMap[ k ] ; k++ ) ;
//										if( k < trackMap.length ) {	// source track exiting
//											trackMap2[ j ] = k++;
//										} else if( tl.getTrackNum( ti.trail.getClass() ) > 0 ) {		// ran out of source tracks, fold over (simple mono -> stereo par exemple)
//											for( k = 0; !trackMap[ k ] ; k++ ) ;
//											trackMap2[ j ] = k++;
//										} else {
//											trackMap2[ j ] = -1;		// there aren't any clipboard tracks ....
//										}
//									} else {						// target track not selected
//										trackMap2[ j ] = -1;
//									}
//								}
//								at.copyRangeFrom( (AudioTrail) srcTrail, copySpan, insertPos, mode, this, edit, trackMap2, false );
//
//							} else if( (ti.numTracks == 1) && (tl.getTrackNum( ti.trail.getClass() ) == 1) ) {
//								srcTrail = tl.getSubTrail( ti.trail.getClass() );
//								ti.trail.editAddAll( this, srcTrail.getCuttedRange(
//									copySpan, true, srcTrail.getDefaultTouchMode(), delta ), edit );
//							}
//						}
//					}
//					finally {
//						ti.trail.editEnd( edit );
//					}
//				}
//
//				if( expTimeline && (pasteLength != 0) ) {	// adjust timeline
//					edit.addPerform( new EditSetTimelineLength( this, doc, docLength + pasteLength ));
//					if( doc.timeline.getVisibleSpan().isEmpty() ) {
//						edit.addPerform( TimelineVisualEdit.scroll( this, doc, insertSpan ));
//					}
//				}
//				if( !insertSpan.isEmpty() ) {
//					edit.addPerform( TimelineVisualEdit.select( this, doc, insertSpan ));
//					edit.addPerform( TimelineVisualEdit.position( this, doc, insertSpan.stop ));
//				}
//
//				success = true;
//				return DONE;
//			}
//			catch( IOException e1 ) {
//				context.setException( e1 );
//				return FAILED;
//			}
//			finally {
//				if( !success ) {
//					edit.cancel();
//				}
//			}
//		}
//
//		public void processFinished( ProcessingThread context, Object argument )
//		{
//			final Object[]					args		= (Object[]) argument;
//			final Session					doc			= (Session) args[0];
//			final ProcessingThread.Client	doneAction	= (ProcessingThread.Client) args[ 3 ];
//			final SyncCompoundEdit			edit		= (SyncCompoundEdit) args[ 6 ];
//			
//			if( (context.getReturnCode() == DONE) && (edit != null) ) {
//				edit.perform();
//				edit.end();
//				doc.getUndoManager().addEdit( edit );
//			}
//
//			if( doneAction != null ) doneAction.processFinished( context, doc );
//		}
//
//		// mte will check pt.shouldCancel() itself
//		public void processCancel( ProcessingThread context, Object argument ) {}
//	} // class actionPasteClass
//
//	/**
//	 *	@todo	when a cutted region spans entire view,
//	 *			selecting undo results in empty visible span
//	 */
//	private class actionDeleteClass
//	extends MenuAction
//	implements ProcessingThread.Client
//	{
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//		
//		private void perform()
//		{		
//			final Span				span	= timeline.getSelectionSpan(); // XXX sync
//			if( span.isEmpty() ) return;
//			
//			final ProcessingThread	pt		= initiate( getValue( NAME ).toString(), span, getEditMode() );
//			pt.start();
//		}
//		
//		// XXX sync
//		protected ProcessingThread initiate( String name, Span span, int mode )
//		{
//			return new ProcessingThread( this, getFrame(), root, bird, name, new Object[]
//				 { enc_this, span, new Integer( mode ),
//				   Track.getInfos( selectedTracks.getAll(), tracks.getAll() ), null }, Session.DOOR_ALL );
//		}
//
//		// --------- ProcessingThread.Client interface ---------
//		
//		/**
//		 *  This method is called by ProcessingThread
//		 */
//		public int processRun( ProcessingThread context, Object argument )
//		{
//			final Object[]					args				= (Object[]) argument;
//			final Session					doc					= (Session) args[0];
//			final Span						span				= (Span) args[1];
//			final int						mode				= ((Integer) args[2]).intValue();
//			final java.util.List			tis					= (java.util.List) args[3];
//			final Span						selSpan				= doc.timeline.getSelectionSpan();
//			final SyncCompoundEdit			edit;
//			AudioTrail						at;
//			Track.Info						ti;
//			boolean							success				= false;
//			boolean							hasSelectedAudio	= false;
//			final boolean					cutTimeline;
//			final Span						cutTimelineSpan;
//			boolean							isAudio;
//			long							start;
//// UUU
////			BlendContext					bc;
//
//// user probably does not want no-op, so MIX is treated as OVERWRITE
////			if( mode == EDIT_MIX ) return;	// well ...
//
////			edit		= new SyncCompoundSessionObjEdit( this, bird, Session.DOOR_ALL, audioTracks.getAll(), // XXX getAll
////														  AudioTrack.OWNER_WAVE, null, null, context.getName() );
//// UUU
////			bc			= createBlendContext( span.getLength() / 2 );
////			bc			= createBlendContext(
////				Math.min( span.getLength() / 2, Math.min( span.getStart(), timeline.getLength() - span.getStop() )));
//
//			if( mode == EDIT_INSERT ) {
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti		= (Track.Info) tis.get( i );
//					isAudio = ti.trail instanceof AudioTrail;
//					if( !ti.getChannelSync() ) {
//						context.setException( new IllegalStateException( getResourceString( "errAudioWillLooseSync" )));
//						return FAILED;
//					}
//					if( isAudio && ti.selected ) {
//						hasSelectedAudio = true;
//					}
//				}
//			}
//			
//			edit			= new BasicSyncCompoundEdit( doc.bird, Session.DOOR_ALL, context.getName() );
//			args[ 4 ]		= edit;
//
//			cutTimeline		= (mode == EDIT_INSERT) && hasSelectedAudio;
//			cutTimelineSpan	= cutTimeline ? new Span( doc.timeline.getLength() - span.getLength(), doc.timeline.getLength() ) : null;
//			
//			try {
//				if( (mode == EDIT_INSERT) && !selSpan.isEmpty() ) {
//					edit.addPerform( TimelineVisualEdit.position( this, doc, span.start ));
//					edit.addPerform( TimelineVisualEdit.select( this, doc, new Span() ));
//				}
//				if( cutTimeline ) {
//					if( doc.timeline.getVisibleSpan().stop > cutTimelineSpan.start ) {
//						TimelineVisualEdit tve = TimelineVisualEdit.scroll( this, doc,
//							new Span( Math.max( 0, cutTimelineSpan.start - doc.timeline.getVisibleSpan().getLength() ), cutTimelineSpan.start ));
////						tve.setDontMerge( true );
//						edit.addPerform( tve );
//					}
//					edit.addPerform( new EditSetTimelineLength( this, doc, cutTimelineSpan.start ));
//				}
//
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti		= (Track.Info) tis.get( i );
//					try {
//						ti.trail.editBegin( edit );
//						isAudio = ti.trail instanceof AudioTrail;
//						if( ti.selected ) {
//							if( mode == EDIT_INSERT ) {	// AudioTrail is not possible here
//								ti.trail.editRemove( this, span, edit );
//							} else {
//								ti.trail.editClear( this, span, edit );
//								if( isAudio ) {
//									at = (AudioTrail) ti.trail;
//									if( ti.getChannelSync() ) {
//										ti.trail.editAdd( this, at.allocSilent( span ), edit );
//									} else {
//										at.copyRangeFrom( at, span, span.start, EDIT_OVERWRITE, this, edit, ti.createChannelMap( ti.numChannels, 0, true ), true );
//									}
//								}
//							}
//						} else if( cutTimeline ) {
//							ti.trail.editRemove( this, cutTimelineSpan, edit );
//						}
//					}
//					finally {
//						ti.trail.editEnd( edit );
//					}
//				}
//
//				success = true;
//				return DONE;
//			}
//			catch( IOException e1 ) {
//				context.setException( e1 );
//				return FAILED;
//			}
//			finally {
//				if( !success ) {
//					edit.cancel();
//				}
//			}
//		} // run
//
//		public void processFinished( ProcessingThread context, Object argument )
//		{
//			final Object[]				args		= (Object[]) argument;
//			final Session				doc			= (Session) args[0];
//			final SyncCompoundEdit		edit		= (SyncCompoundEdit) args[ 4 ];
//
//			if( (context.getReturnCode() == DONE) && (edit != null) ) {
//				edit.perform();
//				edit.end();
//				doc.getUndoManager().addEdit( edit );
//			}
//		}
//
//		// mte will check pt.shouldCancel() itself
//		public void processCancel( ProcessingThread context, Object argument ) {}
//	} // class actionDeleteClass
//
//	private class actionTrimClass
//	extends MenuAction
//	{
//		// performs inplace (no runnable processing) coz it's always fast
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//		
//		private void perform()
//		{
//			final Span						selSpan, deleteBefore, deleteAfter;
//			final BasicSyncCompoundEdit		edit;
//			final java.util.List			tis;
//			Track.Info						ti;
//
//			if( !bird.attemptExclusive( Session.DOOR_ALL, 500 )) return;
//			try {
//				selSpan			= timeline.getSelectionSpan();
////				if( selSpan.isEmpty() ) return;
//				tis				= Track.getInfos( selectedTracks.getAll(), tracks.getAll() );
//				deleteBefore	= new Span( 0, selSpan.start );
//				deleteAfter		= new Span( selSpan.stop, timeline.getLength() );
//				edit			= new BasicSyncCompoundEdit( bird, Session.DOOR_ALL, getValue( NAME ).toString() );
//
//				// deselect
//				edit.addPerform( TimelineVisualEdit.select( this, enc_this, new Span() ));
//				edit.addPerform( TimelineVisualEdit.position( this, enc_this, 0 ));
//
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti = (Track.Info) tis.get( i );
//					if( !deleteAfter.isEmpty() ) ti.trail.editRemove( this, deleteAfter, edit );
//					if( !deleteBefore.isEmpty() ) ti.trail.editRemove( this, deleteBefore, edit );
//				}
//
//				edit.addPerform( new EditSetTimelineLength( this, enc_this, selSpan.getLength() ));
//				edit.addPerform( TimelineVisualEdit.select( this, enc_this, selSpan.shift( -selSpan.start )));
//
//				edit.perform();
//				edit.end();
//				getUndoManager().addEdit( edit );
//			}
//			finally {
//				bird.releaseExclusive( Session.DOOR_ALL );
//			}
//		}
//	} // class actionTrimClass
//
//	/**
//	 *	@todo	when edit mode != EDIT_INSERT, audio tracks are cleared which should be bypassed and vice versa
//	 *	@todo	waveform display not automatically updated when edit mode != EDIT_INSERT
//	 */
//	private class actionSilenceClass
//	extends MenuAction
//	implements ProcessingThread.Client
//	{
//		private JPanel					msgPane		= null;	// lazy
//		private DefaultUnitTranslator	timeTrans	= null;
//		private ParamField				ggDuration	= null;
//
//		public void actionPerformed( ActionEvent e )
//		{
//			perform();
//		}
//		
//		private void perform()
//		{
//			final int		result;
//			final Param		durationSmps;
//			
//			if( msgPane == null ) {
//				msgPane			= new JPanel( new SpringLayout() );
//				// XXX sync
//				timeTrans		= new DefaultUnitTranslator();
//				ggDuration		= new ParamField( timeTrans );
//				ggDuration.addSpace( ParamSpace.spcTimeHHMMSS );
//				ggDuration.addSpace( ParamSpace.spcTimeSmps );
//				ggDuration.addSpace( ParamSpace.spcTimeMillis );
//				ggDuration.addSpace( ParamSpace.spcTimePercentF );
//				ggDuration.setValue( new Param( 1.0, ParamSpace.TIME | ParamSpace.SECS ));
//				msgPane.add( ggDuration );
//				
//				GUIUtil.makeCompactSpringGrid( msgPane, 1, 1, 4, 2, 4, 2 );	// #row #col initx inity padx pady
//	//			HelpGlassPane.setHelp( msgPane, "InsertSilence" );
//			}
//
//			if( !bird.attemptShared( DOOR_TIME, 250 )) return;
//			try {
//				timeTrans.setLengthAndRate( timeline.getLength(), timeline.getRate() );
//			}
//			finally {
//				bird.releaseShared( DOOR_TIME );
//			}
//			result = JOptionPane.showOptionDialog( getFrame(), msgPane, getValue( NAME ).toString(),
//				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null );
//
//			if( result == JOptionPane.OK_OPTION ) {
//				durationSmps	= timeTrans.translate( ggDuration.getValue(), ParamSpace.spcTimeSmps );
//				if( durationSmps.val > 0.0 ) {
//					final ProcessingThread pt;
//					
//					if( !bird.attemptShared( DOOR_TIME, 250 )) return;
//					try {
//						pt = initiate( timeline.getPosition(), (long) durationSmps.val );
//					}
//					finally {
//						bird.releaseShared( DOOR_TIME );
//					}
//					if( pt != null ) pt.start();
//				}
//			}
//		}
//
//		// XXX sync
//		public ProcessingThread initiate( long pos, long numFrames )
//		{
//			return new ProcessingThread( this, getFrame(), root, bird,
//										 getValue( NAME ).toString(), new Object[]
//				 { enc_this, new Long( pos ), new Long( numFrames ),
//				   Track.getInfos( selectedTracks.getAll(), tracks.getAll() ), null }, Session.DOOR_ALL );
//		}
//
//		/**
//		 *  This method is called by ProcessingThread
//		 */
//		public int processRun( ProcessingThread context, Object argument )
//		{
//			final Object[]					args		= (Object[]) argument;
//			final Session					doc			= (Session) args[0];
//			final long						insertPos	= ((Long) args[1]).longValue();
//			final long						numFrames	= ((Long) args[2]).longValue();
////			final int						mode		= ((Integer) args[3]).intValue();
//			final java.util.List			tis			= (java.util.List) args[3];
//			final long						docLength;
//			final Span						oldSelSpan;
//			final BasicSyncCompoundEdit		edit;
//			final Span						insertSpan;
//			final long						pasteLength;
//			Track.Info						ti;
//			AudioTrail						at;
//			boolean							success		= false;
//
////			if( mode == EDIT_MIX ) return DONE;	// well ...
//
//			edit		= new BasicSyncCompoundEdit( doc.bird, Session.DOOR_ALL, context.getName() );
//			args[4]		= edit;
////			edit		= new SyncCompoundSessionObjEdit( this, doc.bird, Session.DOOR_ALL, doc.tracks.getAll(),  // XXX getAll
////								Track.OWNER_WAVE, null, null, context.getName() );
//			oldSelSpan	= doc.timeline.getSelectionSpan();
//			docLength	= doc.timeline.getLength();
////			pasteLength	= mode == EDIT_INSERT ? numFrames : Math.min( numFrames, docLength - insertPos );
//			pasteLength	= numFrames;
//			insertSpan	= new Span( insertPos, insertPos + pasteLength );
//// UUU
////			bc			= BlendingAction.createBlendContext(
////				AbstractApplication.getApplication().getUserPrefs().node( BlendingAction.DEFAULT_NODE ),
////				timelineRate, Math.min( numFrames, Math.min( position, docLength - position )) / 2 );
//
//			try {
//				if( !oldSelSpan.isEmpty() ) { // deselect
//					edit.addPerform( TimelineVisualEdit.select( this, doc, new Span() ));
////							insertPos = oldSelSpan.getStart();
//				}
//
//				for( int i = 0; i < tis.size(); i++ ) {
//					ti = (Track.Info) tis.get( i );
//					ti.trail.editBegin( edit );
//					try {
////						if( mode == EDIT_INSERT ) {
//							ti.trail.editInsert( this, insertSpan, edit );
////						}
////						if( ti.selected ) {
////							if( mode == EDIT_OVERWRITE ) {
////								ti.trail.editClear( this, insertSpan, ti.trail.getDefaultTouchMode(), edit );
////							}
//							if( ti.trail instanceof AudioTrail ) {
//								at			= (AudioTrail) ti.trail;							
//								at.editAdd( this, at.allocSilent( insertSpan ), edit );
//							}
////						}
//					}
//					finally {
//						ti.trail.editEnd( edit );
//					}
//				}
//// UUU
////				if( !mte.insert( insertPos, tl, bc, edit, context, 0.0f, 1.0f )) return CANCELLED;
////				if( (mode == EDIT_INSERT) && (pasteLength != 0) ) {	// adjust timeline
//				if( pasteLength != 0 ) {	// adjust timeline
//					edit.addPerform( new EditSetTimelineLength( this, doc, docLength + pasteLength ));
//					if( doc.timeline.getVisibleSpan().isEmpty() ) {
//						edit.addPerform( TimelineVisualEdit.scroll( this, doc, insertSpan ));
//					}
//				}
//				if( !insertSpan.isEmpty() ) {
//					edit.addPerform( TimelineVisualEdit.select( this, doc, insertSpan ));
//					edit.addPerform( TimelineVisualEdit.position( this, doc, insertSpan.stop ));
//				}
//				success = true;
//				return DONE;
//			}
//			catch( IOException e1 ) {
//				context.setException( e1 );
//				return FAILED;
//			}
//			finally {
//				if( !success ) {
//					edit.cancel();
//				}
//			}
//		}
//
//		public void processFinished( ProcessingThread context, Object argument )
//		{
//			final Object[]				args		= (Object[]) argument;
//			final Session				doc			= (Session) args[0];
//			final SyncCompoundEdit		edit		= (SyncCompoundEdit) args[ 4 ];
//
//			if( (context.getReturnCode() == DONE) && (edit != null) ) {
////edit.debugDump( 0 );
//				edit.perform();
////System.err.println( "--- after perform ---" );
////edit.debugDump( 0 );
//				edit.end();
//				doc.getUndoManager().addEdit( edit );
//			}
//		}
//
//		// mte will check pt.shouldCancel() itself
//		public void processCancel( ProcessingThread context, Object argument ) {}
//	} // class actionSilenceClass
}
