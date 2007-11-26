/*
 *  PathButton.java
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
 *		20-May-05	created from de.sciss.meloncillo.gui.PathButton
 *		29-May-05	supports drag export
 *		03-Aug-05	extends ModificationButton
 *		15-Aug-05	getTransferData checks for file being null
 */

package de.sciss.gui;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;

import net.roydesign.ui.FolderDialog;

/**
 *  This class is a rewritten version
 *  of FScape's <code>PathIcon</code> and provides
 *  a simple ToolIcon like button to
 *  allow the user to select a file
 *  from the harddisk. Besides, the user
 *  can drag files from the Finder onto
 *  the button's icon to set the button's
 *  path.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.17, 26-Mar-07
 *
 *  @see		java.awt.FileDialog
 *  @see		net.roydesign.ui.FolderDialog
 */
public class PathButton
extends ModificationButton
implements EventManager.Processor
{
	private File						path	= null;
	private final int					type;
	private String						dlgTxt;
	private final EventManager	elm		= new EventManager( this );

	private static final DataFlavor[] supportedFlavors = {
		DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor
	};
	
	public PathButton()
	{
		this( PathField.TYPE_INPUTFILE );
	}
	
	public PathButton( int type )
	{
		super( SHAPE_LIST );
		this.type   = type;

		setToolTipText( GUIUtil.getResourceString( "buttonChoosePathTT" ));
		setTransferHandler( new PathTransferHandler() );
		
		final MouseInputAdapter mia = new MouseInputAdapter() {
			private MouseEvent dndInit = null;
			private boolean dndStarted = false;

			public void mousePressed( MouseEvent e )
			{
				dndInit		= e;
				dndStarted	= false;
			}
			
			public void mouseReleased( MouseEvent e )
			{
				if( !dndStarted && contains( e.getPoint() )) showFileChooser();
				dndInit		= null;
				dndStarted	= false;
			}
			
			public void mouseDragged( MouseEvent e )
			{
				if( !dndStarted && (dndInit != null) &&
					((Math.abs( e.getX() - dndInit.getX() ) > 5) ||
					 (Math.abs( e.getY() - dndInit.getY() ) > 5))) {
			
					JComponent c = (JComponent) e.getSource();
					c.getTransferHandler().exportAsDrag( c, e, TransferHandler.COPY );
					dndStarted = true;
				}
			}
		};
		
		addMouseListener( mia );
		addMouseMotionListener( mia );
	}

	/**
	 *  Constructs a new <code>PathButton</code> for
	 *  given type of file chooser and optional dialog text
	 *
	 *  @param  type	the type of file chooser to display. the values
	 *					are those from <code>PathField</code>, e.g.
	 *					<code>PathField.TYPE_INPUT</code>
	 *  @param  dlgTxt  text to display in the file chooser dialog or <code>null</code>
	 */
	public PathButton( int type, String dlgTxt )
	{
		this( type );
		setDialogText( dlgTxt );
	}
	
	public void setDialogText( String dlgTxt )
	{
		this.dlgTxt = dlgTxt;
	}
	
	/**
	 *  Sets the button's path. This is path will be
	 *  used as default setting when the file chooser is shown
	 *
	 *  @param  path	the new path for the button
	 */
	public void setPath( File path )
	{
		this.path = path;
	}

	/*
	 *  Sets a new path and dispatches a <code>PathEvent</code>
	 *  to registered listeners
	 *
	 *  @param  path	the new path for the button and the event
	 */
	private void setPathAndDispatchEvent( File path )
	{
		setPath( path );
		elm.dispatchEvent( new PathEvent( this, PathEvent.CHANGED, System.currentTimeMillis(), path ));
	}
	
	/**
	 *  Returns the path set for the button
	 *  or chosen by the user after a file chooser
	 *  has been shown.
	 *
	 *  @return the button's path or <code>null</code>
	 *			if no path was set or the file chooser was cancelled
	 */
	public File getPath()
	{
		return path;
	}
	
	// --- listener registration ---
	
	/**
	 *  Register a <code>PathListener</code>
	 *  which will be informed about changes of
	 *  the path (i.e. user selections in the
	 *  file chooser).
	 *
	 *  @param  listener	the <code>PathListener</code> to register
	 *  @see	de.sciss.app.EventManager#addListener( Object )
	 */
	public void addPathListener( PathListener listener )
	{
		elm.addListener( listener );
	}

	/**
	 *  Unregister a <code>PathListener</code>
	 *  from receiving path change events.
	 *
	 *  @param  listener	the <code>PathListener</code> to unregister
	 *  @see	de.sciss.app.EventManager#removeListener( Object )
	 */
	public void removePathListener( PathListener listener )
	{
		elm.removeListener( listener );
	}

	public void processEvent( BasicEvent e )
	{
		PathListener listener;
		int i;
		
		for( i = 0; i < elm.countListeners(); i++ ) {
			listener = (PathListener) elm.getListener( i );
			switch( e.getID() ) {
			case PathEvent.CHANGED:
				listener.pathChanged( (PathEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}

	private void showFileChooser()
	{
		File		path;
		FileDialog	fDlg;
		String		fDir, fFile; // , fPath;
//		int			i;
		Component	win;

		for( win = this; !(win instanceof Frame); ) { 
			win = SwingUtilities.getWindowAncestor( win );
			if( win == null ) return;
		}

		path = getPath();
		switch( type & PathField.TYPE_BASICMASK ) {
		case PathField.TYPE_INPUTFILE:
			fDlg = new FileDialog( (Frame) win, dlgTxt, FileDialog.LOAD );
			break;
		case PathField.TYPE_OUTPUTFILE:
			fDlg = new FileDialog( (Frame) win, dlgTxt, FileDialog.SAVE );
			break;
		case PathField.TYPE_FOLDER:
			fDlg = new FolderDialog( (Frame) win, dlgTxt );
			break;
		default:
			fDlg = null;
			assert false : (type & PathField.TYPE_BASICMASK);
			break;
		}
		if( path != null ) {
			fDlg.setFile( path.getName() );
			fDlg.setDirectory( path.getParent() );
		}
		fDlg.setVisible( true );
		fDir	= fDlg.getDirectory();
		fFile	= fDlg.getFile();
					
		if( ((type & PathField.TYPE_BASICMASK) != PathField.TYPE_FOLDER) && (fDir == null) ) {
			fDir = "";
		}

		if( (fFile != null) && (fDir != null) ) {

			if( (type & PathField.TYPE_BASICMASK) == PathField.TYPE_FOLDER ) {
				path = new File( fDir );
			} else {
				path = new File( fDir + fFile );
			}
			setPathAndDispatchEvent( path );
		}

		fDlg.dispose();
	}

// ----------- interner TransferHandler -----------

	private class PathTransferHandler
	extends TransferHandler
	{
		private PathTransferHandler() {}

		/**
		 * Overridden to import a Pathname (Fileliste or String) if it is available.
		 */
		public boolean importData( JComponent c, Transferable t )
		{
			Object		o;
			List		fileList;
			File		path	= null;
		
			try {
				if( t.isDataFlavorSupported( DataFlavor.javaFileListFlavor )) {
					o =  t.getTransferData( DataFlavor.javaFileListFlavor );
					if( o instanceof List ) {
						fileList = (List) o;
						if( !fileList.isEmpty() ) {
							o  =  fileList.get( 0 );
							if( o instanceof File ) {
								path = (File) o;
							} else {
								path = new File( o.toString() );
							}
						}
					}
				} else if( t.isDataFlavorSupported( DataFlavor.stringFlavor )) {
					path = new File( (String) t.getTransferData( DataFlavor.stringFlavor ));
				}
				if( path != null ) {
					setPathAndDispatchEvent( path );
					return true;
				}
			}
			catch( UnsupportedFlavorException e1 ) {}
			catch( IOException e2 ) {}

			return false;
		}
		
		public int getSourceActions( JComponent c )
		{
			return COPY;
		}
		
		protected Transferable createTransferable( JComponent c )
		{
//			System.err.println( "createTransferable" );
			return new PathTransferable( getPath() );
		}
		
		protected void exportDone( JComponent source, Transferable data, int action )
		{
//			System.err.println( "exportDone. Action == "+action );
		}

		public boolean canImport( JComponent c, DataFlavor[] flavors )
		{
// System.err.println( "canImport" );

			for( int i = 0; i < flavors.length; i++ ) {
				for( int j = 0; j < supportedFlavors.length; j++ ) {
					if( flavors[i].equals( supportedFlavors[j] )) return true;
				}
			}
			return false;
		}

//		public Icon getVisualRepresentation( Transferable t )
//		{
//System.err.println( "getVisualRepresentation" );
//			return FileSystemView.getFileSystemView().getSystemIcon( new File( System.getProperty( "user.home" )));
//		}
	} // class PathTransferHandler
	
	private static class PathTransferable
	implements Transferable
	{
		private final File f;
		
		private PathTransferable( File f )
		{
			this.f	= f;
		}
		
		public DataFlavor[] getTransferDataFlavors()
		{
			return supportedFlavors;
		}
		
		public boolean isDataFlavorSupported( DataFlavor flavor )
		{
			for( int i = 0; i < supportedFlavors.length; i++ ) {
				if( supportedFlavors[ i ].equals( flavor )) return true;
			}
			return false;
		}
		
		public Object getTransferData( DataFlavor flavor )
		throws UnsupportedFlavorException, IOException
		{
			if( f == null ) throw new IOException();
			if( flavor.equals( DataFlavor.javaFileListFlavor )) {
				final List coll = new ArrayList( 1 );
				coll.add( f );
				return coll;
			} else if( flavor.equals( DataFlavor.stringFlavor )) {
				return f.getAbsolutePath();
			}
			throw new UnsupportedFlavorException( flavor );
		}
	}
}