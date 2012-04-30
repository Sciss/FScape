/*
 *  ProcessPanel.java
 *  FScape
 *
 *  Copyright (c) 2001-2012 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.app.AbstractApplication;
import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.gui.CoverGrowBox;
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

//	public static final int TYPE_CLOSEGADGET	= 0x01;
//	public static final int TYPE_CANPAUSE		= 0x02;
	public static final int TYPE_ASYNC			= 0x04;

	public static final int	STATE_STOPPED	= 0;
	public static final int	STATE_RUNNING	= 1;
	public static final int	STATE_PAUSING	= 2;
	public static final int STATE_WARMUP	= 3;	// Process gestartet, aber noch keine Rueckmeldung

// -------- private Variablen --------

//	private final ProgressBar			ggProgress;
	private final ProgressPanel			pProgress;
	private final Action				actionClose;
	private final ActionProcess	actionProcess;
	private	final JButton				ggProcess;

	private final EventManager			elm		= new EventManager( this );
	private final ProcessorListener		pl;

//	private int					type;
	private Window				win;
	private int					state			= STATE_STOPPED;
//	private boolean				altMode			= false;

	private Processor			proc;
	private Thread				procThread;
	
	private static final String	txt[]		= { " Render ", "Stop", "Resume", "???" };
//	private static final String	txtAlt[]	= { null, "  Pause  ", "  Stop  " };
	
// -------- public Methoden --------

	/**
	 *	@param	type		TYPE_... (ODER-verknuepft)
	 *	@param	proc		zu startender Algorithmus
	 */
	public ProcessPanel( final int type, final ProgressPanel pProgress, Processor proc )
	{
		super();

		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		
//		this.type		= type;
		this.pProgress	= pProgress;

		final de.sciss.app.Application	app			= AbstractApplication.getApplication();
		
		actionClose		= new ActionClose( app.getResourceString( "buttonClose" ));
		actionProcess	= new ActionProcess();
		
//		setFocusable( true );

//		kl	= new KeyAdapter() {
//			public void keyPressed( KeyEvent e )
//			{
//				switch( e.getKeyCode() ) {
//				case KeyEvent.VK_ALT:
//					if( (((state == STATE_RUNNING) && ((type & TYPE_CANPAUSE) != 0)) ||
//						  (state == STATE_PAUSING)) && !altMode ) {
//
//						altMode = true;
//						actionProcess.updateState();
//					}
//					break;
//				
//				default:
//					break;
//				}
//			}
//
//			public void keyReleased( KeyEvent e )
//			{
//				switch( e.getKeyCode() ) {
//				case KeyEvent.VK_ALT:
//					if( altMode ) {
//						altMode = false;
//						actionProcess.updateState();
//					}
//					break;
//
//				case KeyEvent.VK_PAUSE:
//					if( (state == STATE_RUNNING) && !altMode && ((type & TYPE_CANPAUSE) != 0) ) {
//						altMode = true;
//						ggProcess.doClick();		// "pause"
//					} else if( (state == STATE_PAUSING) && !altMode ) {
//						ggProcess.doClick();		// "continue"
//					}
//					break;
//				
//				default:
//					break;
//				}
//			}
//		};

		pl = new ProcessorListener() {
			public void processorProgress( ProcessorEvent e )
			{
//				ggProgress.setProgression( e.getProcessor().getProgression() );
				pProgress.setProgression( e.getProcessor().getProgression() );
			}
			
			public void processorStarted( ProcessorEvent e )
			{
				actionClose.setEnabled( false );
				state	= STATE_RUNNING;
				if( (type & TYPE_ASYNC) != 0 ) {
//					ggProgress.setIndeterminate( true );
					pProgress.setProgression( -1f );
				}
				updateSchnucki( e );
			}

			public void processorStopped( ProcessorEvent e )
			{
				actionClose.setEnabled( true );
				if( e.getProcessor().getError() != null ) {
//					ggProgress.finish( false );
					pProgress.finishProgression( ProgressComponent.FAILED );
				} else if( e.getProcessor().getProgression() == 1.0f ) {
//					ggProgress.finish( true );
					pProgress.finishProgression( ProgressComponent.DONE );
				} else if( (type & TYPE_ASYNC) != 0 ) {
//					ggProgress.reset();
					pProgress.resetProgression();
				}
				state	= STATE_STOPPED;
//				ggProgress.requestFocus();	// Avoid accidental restart by hitting the return key!
//				enc_this.requestFocus();	// Avoid accidental restart by hitting the return key!
//				FocusManager.getCurrentManager().clearGlobalFocusOwner();
//				if( win != null ) win.requestFocus();
				updateSchnucki( e );
			}

			public void processorPaused( ProcessorEvent e )
			{
				state	= STATE_PAUSING;
//				ggProgress.pause();
				pProgress.pause();
				updateSchnucki( e );
			}

			public void processorResumed( ProcessorEvent e )
			{
				state	= STATE_RUNNING;
//				ggProgress.resume();
				pProgress.resume();
				updateSchnucki( e );
			}
			
			private void updateSchnucki( ProcessorEvent e )
			{
//				altMode = false;
				actionProcess.updateState();
//				setEnabled( true );
if( state != STATE_RUNNING ) setEnabled( true );

				elm.dispatchEvent( new ProcessorEvent( e.getSource(), e.getID(),
					System.currentTimeMillis(), e.getProcessor() ));
			}
		};
			
//		if( (type & TYPE_CLOSEGADGET) != 0 ) {
//			ggClose		= new JButton( actionClose );
////			add( ggClose, BorderLayout.WEST );
//			add( ggClose );
//		} else {
//			ggClose		= null;
//		}
		ggProcess		= new JButton( actionProcess );

		final InputMap	imap	= ggProcess.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
		final ActionMap	amap	= ggProcess.getActionMap();

//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_COLON, KeyEvent.META_MASK ), "stop" );
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_PERIOD, InputEvent.META_MASK ), "stop" );
		amap.put( "stop", new ActionStop() );
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_COMMA, KeyEvent.META_MASK ), "pause" );
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_COMMA, KeyEvent.META_MASK + KeyEvent.SHIFT_MASK ), "pause" );
//		amap.put( "pause", new actionPauseClass() );

//		ggProcess.addKeyListener( kl );
//		ggProgress		= new ProgressBar();
//		if( (type & TYPE_ASYNC) != 0 ) ggProgress.setIndeterminate( true );
//		add( ggProcess, BorderLayout.EAST );
		add( pProgress );
		add( ggProcess );
//		add( ggProgress, BorderLayout.CENTER );
//		add( pProgress, BorderLayout.CENTER );
		add( CoverGrowBox.create() );
		
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
//			ggProcess.requestFocus();
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
//			ggProcess.requestFocus();
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
//			ggProcess.requestFocus();
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
//		ggProgress.setPaint( c );
		pProgress.setPaint( c );
	}

	public void setText( String t )
	{
//		ggProgress.setText( t );
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
//			putValue( NAME, altMode ? txtAlt[ state ] : txt[ state ]);
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( state == STATE_STOPPED ) {
				start();
//			} else if( ((state == STATE_PAUSING) && altMode) ||
//					   ((state == STATE_RUNNING) && !altMode) ) {
//				stop();
//			} else if( state == STATE_PAUSING ) {
//				resume();
//			} else {
//				pause();
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

/*
	private class actionPauseClass
	extends AbstractAction
	{
		private actionPauseClass()
		{
			super();
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( state == STATE_RUNNING ) {
				pause();
			} else if( state == STATE_PAUSING ) {
				resume();
			}
		}
	}
*/
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
// class ProcessPanel
