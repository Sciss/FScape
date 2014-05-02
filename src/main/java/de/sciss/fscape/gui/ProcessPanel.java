/*
 *  ProcessPanel.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		21-May-05	modernized
 *		03-Oct-06	updated
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

import de.sciss.fscape.proc.*;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.ProgressComponent;

/**
 *	GUI container with a progress bar and
 *	start / stop button.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class ProcessPanel
extends JPanel
implements EventManager.Processor
{
// -------- public Variablen --------

	public static final int TYPE_ASYNC			= 0x04;

	public static final int	STATE_STOPPED	= 0;
	public static final int	STATE_RUNNING	= 1;
	public static final int	STATE_PAUSING	= 2;
	public static final int STATE_WARMUP	= 3;	// Process gestartet, aber noch keine Rueckmeldung

// -------- private Variablen --------

	private final ProgressPanel			pProgress;
	private final Action				actionClose;
	private final ActionProcess	actionProcess;
	private	final JButton				ggProcess;

	private final EventManager			elm		= new EventManager( this );
	private final ProcessorListener		pl;

	private Window				win;
	private int					state			= STATE_STOPPED;

	private Processor			proc;
	private Thread				procThread;
	
	private static final String	txt[]		= { " Render ", "Stop", "Resume", "???" };

// -------- public Methoden --------

	/**
	 *	@param	type		TYPE_... (ODER-verknuepft)
	 *	@param	proc		zu startender Algorithmus
	 */
	public ProcessPanel( final int type, final ProgressPanel pProgress, Processor proc )
	{
		super();

		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		
		this.pProgress	= pProgress;

		actionClose		= new ActionClose("Close");
		actionProcess	= new ActionProcess();

		pl = new ProcessorListener() {
			public void processorProgress( ProcessorEvent e )
			{
				pProgress.setProgression( e.getProcessor().getProgression() );
			}
			
			public void processorStarted( ProcessorEvent e )
			{
				actionClose.setEnabled( false );
				state	= STATE_RUNNING;
				if( (type & TYPE_ASYNC) != 0 ) {
					pProgress.setProgression( -1f );
				}
				updateSchnucki( e );
			}

			public void processorStopped( ProcessorEvent e )
			{
				actionClose.setEnabled( true );
				if( e.getProcessor().getError() != null ) {
					pProgress.finishProgression( ProgressComponent.FAILED );
				} else if( e.getProcessor().getProgression() == 1.0f ) {
					pProgress.finishProgression( ProgressComponent.DONE );
				} else if( (type & TYPE_ASYNC) != 0 ) {
					pProgress.resetProgression();
				}
				state	= STATE_STOPPED;
				updateSchnucki( e );
			}

			public void processorPaused( ProcessorEvent e )
			{
				state	= STATE_PAUSING;
				pProgress.pause();
				updateSchnucki( e );
			}

			public void processorResumed( ProcessorEvent e )
			{
				state	= STATE_RUNNING;
				pProgress.resume();
				updateSchnucki( e );
			}
			
			private void updateSchnucki( ProcessorEvent e )
			{
				actionProcess.updateState();
if( state != STATE_RUNNING ) setEnabled( true );

				elm.dispatchEvent( new ProcessorEvent( e.getSource(), e.getID(),
					System.currentTimeMillis(), e.getProcessor() ));
			}
		};

		ggProcess		= new JButton( actionProcess );

		final InputMap	imap	= ggProcess.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
		final ActionMap	amap	= ggProcess.getActionMap();

		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_PERIOD, InputEvent.META_MASK ), "stop" );
		amap.put( "stop", new ActionStop() );

		add( pProgress );
		add( ggProcess );
		// add( CoverGrowBox.create() );
        add(Box.createHorizontalStrut(16));
		
		ggProcess.setFocusable( false );
		addPropertyChangeListener( "font", new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent e )
			{
				final Dimension d = ggProcess.getPreferredSize();
				GUIUtil.constrainSize( ggProcess, d.width, d.height );
			}
		});
		
		pProgress.addCancelListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				stop();
			}
		});
		
		setProcessor( proc );
	}

	public void setProcessor( Processor proc )
	{
		if( this.proc != null ) {
			this.proc.removeProcessorListener( pl );
		}
		this.proc = proc;
		if( proc != null ) {
			actionProcess.setEnabled( true );
			proc.addProcessorListener( pl );
		} else {
			actionProcess.setEnabled( false );
		}
	}

	public int getState()
	{
		return state;
	}

	public void start()
	{
		if( (proc != null) && (state == STATE_STOPPED) ) {
			setEnabled( false );
//			ggProgress.reset();
			pProgress.resetProgression();
			procThread = new Thread( proc, proc.toString() );
			procThread.start();			// ! wenn jemand getState() aufruft, k�nnte nach
			state = STATE_WARMUP;		// ! Thread.start() ein STATE_STOPPED fatal sein!!
		}
	}

	public void stop()
	{
		if( (proc != null) && ((state == STATE_RUNNING) || (state == STATE_PAUSING)) ) {
			setEnabled( false );
			proc.stop();
			if( (procThread != null) && !procThread.isAlive() ) {
				// Thread died because of error, unblock GUI
				pl.processorStopped( new ProcessorEvent( proc, ProcessorEvent.STOPPED, 0, proc ));
			}
		}
	}
	
	public void pause()
	{
		if( (proc != null) && (state == STATE_RUNNING) ) {
			setEnabled( false );
			proc.pause();
			if( (procThread != null) && !procThread.isAlive() ) {
				// Thread died because of error, unblock GUI
				pl.processorStopped( new ProcessorEvent( proc, ProcessorEvent.STOPPED, 0, proc ));
			}
		}
	}
	
	public void resume()
	{
		if( (proc != null) && (state == STATE_PAUSING) ) {
			setEnabled( false );
			proc.resume();
		}
	}
	
	public void addProcessorListener( ProcessorListener li )
	{
		elm.addListener( li );
	}

	public void removeProcessorListener( ProcessorListener li )
	{
		elm.removeListener( li );
	}

// ---------- EventManager.Processor interface ----------

	public void processEvent( BasicEvent e )
	{
		ProcessorListener li;
		
		for( int i = 0; i < elm.countListeners(); i++ ) {
			li = (ProcessorListener) elm.getListener( i );
			switch( e.getID() ) {
//			case ProcessorEvent.PROGRESS:
//				li.processorProgress( (ProcessorEvent) e );
//				break;
			case ProcessorEvent.STARTED:
				li.processorStarted( (ProcessorEvent) e );
				break;
			case ProcessorEvent.STOPPED:
				li.processorStopped( (ProcessorEvent) e );
				break;
			case ProcessorEvent.PAUSED:
				li.processorPaused( (ProcessorEvent) e );
				break;
			case ProcessorEvent.RESUMED:
				li.processorResumed( (ProcessorEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}
	
	public void setEnabled( boolean state )
	{
		actionProcess.setEnabled( state );
	}
	
	public void setPaint( Paint c )
	{
		pProgress.setPaint( c );
	}

	public void setText( String t )
	{
		pProgress.setProgressionText( t );
	}

// -------- internal classes --------

	private class ActionProcess
	extends AbstractAction
	{
		protected ActionProcess()
		{
			super( txt[ state ]);
		}
		
		protected void updateState()
		{
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( state == STATE_STOPPED ) {
				start();
			}
		}
	}

	private class ActionStop
	extends AbstractAction
	{
		protected ActionStop()
		{
			super();
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( state == STATE_RUNNING || state == STATE_PAUSING ) {
				stop();
			}
		}
	}

	private class ActionClose
	extends AbstractAction
	{
		protected ActionClose( String label )
		{
			super( label );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( (state == STATE_STOPPED) && (win != null) ) {
				win.dispatchEvent( new WindowEvent( win, WindowEvent.WINDOW_CLOSING ));
			}
		}
	}
}
