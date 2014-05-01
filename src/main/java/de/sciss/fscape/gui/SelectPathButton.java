package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.gui.PathEvent;
import de.sciss.gui.PathListener;
import de.sciss.icons.raphael.Shapes;

// import net.roydesign.ui.FolderDialog;

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
 *  @version	0.18, 07-Feb-10
 *
 *  @see		java.awt.FileDialog
 */
public class SelectPathButton
        extends JButton
        implements EventManager.Processor
{
    private File				path	= null;
    private final int			type;
    private String				dlgTxt;
    private final EventManager	elm		= new EventManager( this );
    private FilenameFilter		filter	= null;

    protected static final DataFlavor[] supportedFlavors = {
            DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor
    };

    public SelectPathButton()
    {
        this(PathField.TYPE_INPUTFILE);
    }

    public SelectPathButton(int type) {
        super(); // SHAPE_LIST);
        this.type = type;

        Path2D p = new GeneralPath();
        Shapes.Search(p);
        // Shapes.ListView(p);
        // Shapes.Picker(p);
        final Shape icnShp = p.createTransformedShape(AffineTransform.getScaleInstance(0.5, 0.5));

        setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(isEnabled() ? Color.black : Color.gray);
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.fill(icnShp);
                g2.translate(-x, -y);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        });

        setToolTipText("Choose File...");
        setTransferHandler(new PathTransferHandler());

        final MouseInputAdapter mia = new MouseInputAdapter() {
            private MouseEvent dndInit = null;
            private boolean dndStarted = false;

            public void mousePressed(MouseEvent e) {
                dndInit     = e;
                dndStarted  = false;
            }

            public void mouseReleased(MouseEvent e) {
                if (!dndStarted && contains(e.getPoint())) showFileChooser();
                dndInit     = null;
                dndStarted  = false;
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

        addMouseListener      (mia);
        addMouseMotionListener(mia);
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
    public SelectPathButton(int type, String dlgTxt) {
        this(type);
        setDialogText(dlgTxt);
    }

    public void setDialogText(String dlgTxt) {
        this.dlgTxt = dlgTxt;
    }

    /**
     *  Sets the button's path. This is path will be
     *  used as default setting when the file chooser is shown
     *
     *  @param  path	the new path for the button
     */
    public void setPath(File path) {
        this.path = path;
    }

    /*
     *  Sets a new path and dispatches a <code>PathEvent</code>
     *  to registered listeners
     *
     *  @param  path	the new path for the button and the event
     */
    protected void setPathAndDispatchEvent(File path) {
        setPath(path);
        elm.dispatchEvent(new PathEvent(this, PathEvent.CHANGED, System.currentTimeMillis(), path));
    }

    /**
     *  Returns the path set for the button
     *  or chosen by the user after a file chooser
     *  has been shown.
     *
     *  @return the button's path or <code>null</code>
     *			if no path was set or the file chooser was cancelled
     */
    public File getPath() {
        return path;
    }

    /**
     * 	Sets the filter to use for enabling or disabling items
     * 	in the file dialog.
     *
     * 	@param	filter	the new filter or null to remove an existing filter
     */
    public void setFilter(FilenameFilter filter) {
        this.filter = filter;
    }

    /**
     * 	Queries the current item filter for the file dialog.
     *
     * 	@return	the current filter or null if no filter is installed
     */
    public FilenameFilter getFilter() {
        return filter;
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
    public void addPathListener(PathListener listener) {
        elm.addListener(listener);
    }

    /**
     *  Unregister a <code>PathListener</code>
     *  from receiving path change events.
     *
     *  @param  listener	the <code>PathListener</code> to unregister
     *  @see	de.sciss.app.EventManager#removeListener( Object )
     */
    public void removePathListener(PathListener listener) {
        elm.removeListener(listener);
    }

    public void processEvent(BasicEvent e) {
        PathListener listener;
        int i;

        for (i = 0; i < elm.countListeners(); i++) {
            listener = (PathListener) elm.getListener(i);
            switch (e.getID()) {
                case PathEvent.CHANGED:
                    listener.pathChanged((PathEvent) e);
                    break;
                default:
                    assert false : e.getID();
            }
        } // for( i = 0; i < elm.countListeners(); i++ )
    }

    protected void showDialog(Dialog dlg) {
        dlg.setVisible(true);
    }

    protected void showFileChooser() {
        File p;
        FileDialog fDlg;
        String fDir, fFile; // , fPath;
//		int			i;
        Component win;

        for (win = this; !(win instanceof Frame); ) {
            win = SwingUtilities.getWindowAncestor(win);
            if (win == null) return;
        }

        p = getPath();
        switch( type & PathField.TYPE_BASICMASK ) {
            case PathField.TYPE_INPUTFILE:
                fDlg = new FileDialog( (Frame) win, dlgTxt, FileDialog.LOAD );
                break;
            case PathField.TYPE_OUTPUTFILE:
                fDlg = new FileDialog( (Frame) win, dlgTxt, FileDialog.SAVE );
                break;
            case PathField.TYPE_FOLDER:
                fDlg = new FileDialog( (Frame) win, dlgTxt, FileDialog.SAVE );
                // fDlg = new FolderDialog( (Frame) win, dlgTxt );
                break;
            default:
                fDlg = null;
                assert false : (type & PathField.TYPE_BASICMASK);
                break;
        }
        if( p != null ) {
            fDlg.setFile( p.getName() );
            fDlg.setDirectory( p.getParent() );
        }
        if( filter != null ) {
            fDlg.setFilenameFilter( filter );
        }
        showDialog( fDlg );
        fDir	= fDlg.getDirectory();
        fFile	= fDlg.getFile();

        if( ((type & PathField.TYPE_BASICMASK) != PathField.TYPE_FOLDER) && (fDir == null) ) {
            fDir = "";
        }

        if( (fFile != null) && (fDir != null) ) {

            if( (type & PathField.TYPE_BASICMASK) == PathField.TYPE_FOLDER ) {
                p = new File( fDir );
            } else {
                p = new File( fDir + fFile );
            }
            setPathAndDispatchEvent( p );
        }

        fDlg.dispose();
    }

// ----------- interner TransferHandler -----------

    private class PathTransferHandler
            extends TransferHandler
    {
        protected PathTransferHandler() { /* empty */ }

        /**
         * Overridden to import a Pathname (Fileliste or String) if it is available.
         */
        public boolean importData( JComponent c, Transferable t )
        {
            Object		o;
            List		fileList;
            File		newPath	= null;

            try {
                if( t.isDataFlavorSupported( DataFlavor.javaFileListFlavor )) {
                    o =  t.getTransferData( DataFlavor.javaFileListFlavor );
                    if( o instanceof List ) {
                        fileList = (List) o;
                        if( !fileList.isEmpty() ) {
                            o  =  fileList.get( 0 );
                            if( o instanceof File ) {
                                newPath = (File) o;
                            } else {
                                newPath = new File( o.toString() );
                            }
                        }
                    }
                } else if( t.isDataFlavorSupported( DataFlavor.stringFlavor )) {
                    newPath = new File( (String) t.getTransferData( DataFlavor.stringFlavor ));
                }
                if( newPath != null ) {
                    setPathAndDispatchEvent( newPath );
                    return true;
                }
            }
            catch( UnsupportedFlavorException e1 ) { e1.printStackTrace(); }
            catch( IOException e2 ) { e2.printStackTrace(); }

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

        protected PathTransferable( File f )
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