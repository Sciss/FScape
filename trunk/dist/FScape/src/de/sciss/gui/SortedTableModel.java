/*
 *	SortedTableModel.java
 *	Inertia
 *
 *	This is a wrapper table model mainly taken from sun's swing demos
 *	(original class name TableSorter). see the class commentary below
 *	for details and authors. The class has been modified in some ways:
 *	<ul>
 *	<li>once, a column is sorted, it's not possible to switch back to unsorted
 *	mode for all columns (unsorting is only possible by choosing a different
 *	column to sort)</li>
 *	<li>multi-column sorting is not supported</li>
 *	<li>shift+mouseclick and ctrl+mouseclick have been disabled</li>
 *	<li>columns can be exempt from sorting</li>
 *	<li>default string comparator is case insensitive</li>
 *	<li>the triangle icon (ArrayIcon) has been exchanged with aqua look and feel</li>
 *	<li>tooltip has been removed</li>
 *	<li>code has been reformatted for my personal style</li>
 *	</ul>
 *
 *	Changelog:
 *		02-Dec-05	created from TableSorter class by milne et al.
 */

package de.sciss.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *	Original class commentary:
 * SortedTableModel is a decorator for TableModels; adding sorting
 * functionality to a supplied TableModel. SortedTableModel does
 * not store or copy the data in its TableModel; instead it maintains
 * a map from the row indexes of the view to the row indexes of the
 * model. As requests are made of the sorter (like getValueAt(row, col))
 * they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way,
 * the SortedTableModel appears to hold another copy of the table
 * with the rows in a different order.
 * <p/>
 * SortedTableModel registers itself as a listener to the underlying model,
 * just as the JTable itself would. Events recieved from the model
 * are examined, sometimes manipulated (typically widened), and then
 * passed on to the SortedTableModel's listeners (typically the JTable).
 * If a change to the model has invalidated the order of SortedTableModel's
 * rows, a note of this is made and the sorter will resort the
 * rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the
 * table header may be used as a complete UI for SortedTableModel.
 * The default renderer of the tableHeader is decorated with a renderer
 * that indicates the sorting status of each column. In addition,
 * a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns
 * and advances the sorting status of that column through three
 * values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to
 * NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns
 * and cycles the sorting status of the column through the same
 * three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except
 * that the changes to the column do not cancel the statuses of columns
 * that are already sorting - giving a way to initiate a compound
 * sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that
 * first appeared in the swing table demos in 1997.
 * 
 *	@author		Philip Milne
 *	@author		Brendon McLean 
 *	@author		Dan van Enckevort
 *	@author		Parwinder Sekhon
 *	@author		Hanns Holger Rutz
 *	@version	0.31, 03-Dec-05
 *
 *	@todo		row selection should be updated when switching sorting
 */
public class SortedTableModel
extends AbstractTableModel
{
	private TableModel tableModel;

	public static final int DESCENDING	= -1;
	public static final int NOT_SORTED	= 0;
	public static final int ASCENDING	= 1;

	private static final Directive EMPTY_DIRECTIVE = new Directive( -1, NOT_SORTED );

	/**
	 *	A comparator which works on objects
	 *	that implement the Comparable interface
	 */
	public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
		public int compare( Object o1, Object o2 )
		{
			return( ((Comparable) o1).compareTo( o2 ));
		}
	};
	
	/**
	 *	A comparator which converts the objects
	 *	to strings
	 */
	public static final Comparator STRING_COMPARATOR = new Comparator() {
		public int compare( Object o1, Object o2 )
		{
			return( o1.toString().compareTo( o2.toString() ));
		}
	};

	/**
	 *	A comparator which converts the objects
	 *	to strings and converting them to upper case.
	 */
	public static final Comparator STRING_COMPARATOR_UPCASE = new Comparator() {
		public int compare( Object o1, Object o2 )
		{
			return( o1.toString().toUpperCase().compareTo( o2.toString().toUpperCase() ));
		}
	};

	// view index maps to model index. this is null if table model
	// changes and will be re-calculated automatically when calling
	// getViewToModel()
	private Row[] viewToModel;
	// model index maps to view index. this is null if table model
	// changes and will be re-calculated automatically when calling
	// getModelToView()
	private int[] modelToView;

	private JTableHeader				tableHeader;
	private final MouseListener			mouseListener;
	private final TableModelListener	tableModelListener;
	// maps cell value classes to comparators
	private Map							mapComparators		= new HashMap();
	// elements: Directive describing the sorted column
	//	; hierarchy is top-down (first element is highest priority sorting)
	private List						collSorted			= new ArrayList();
	// elements: Integer( columnIndex ) for all columns which shall not be sortable
	private Set							setDisallowedColumns= new HashSet();

	// the icon for the three states DESCENDING, NOT_SORTED, ASCENDING
	// ; note that even not sorted columns have an icon (although clear)
	// to not have the label's text position changing
	private final Icon[]				icnArrow			= new Icon[ 3 ];

	/**
	 *	Creates a new sorted table model
	 *	with no underlying model assigned. Use
	 *	<code>setTableModel</code> to assign such
	 *	an underlying model before using the stm.
	 */
	public SortedTableModel()
	{
		this.mouseListener		= new MouseHandler();
		this.tableModelListener = new TableModelHandler();
		
		icnArrow[ 0 ]			= new ArrowIcon( DESCENDING );
		icnArrow[ 1 ]			= new ArrowIcon( NOT_SORTED );
		icnArrow[ 2 ]			= new ArrowIcon( ASCENDING );
		
		mapComparators.put( String.class, STRING_COMPARATOR_UPCASE );
	}

	/**
	 *	Creates a new sorted table model
	 *	with a given underlying model assigned. Use
	 *	<code>setTableHeader</code> to assign a header
	 *	used for selecting the sorted column.
	 */
	public SortedTableModel( TableModel tableModel )
	{
		this();
		setTableModel( tableModel );
	}

	/**
	 *	Creates a new sorted table model
	 *	with a given underlying model assigned and a table header used
	 *	for selecting the sorted column.
	 */
	public SortedTableModel( TableModel tableModel, JTableHeader tableHeader )
	{
		this();
		setTableHeader( tableHeader );
		setTableModel( tableModel );
	}

	public TableModel getTableModel()
	{
		return tableModel;
	}

	public void setTableModel( TableModel tableModel )
	{
		if( this.tableModel != null ) {
			this.tableModel.removeTableModelListener( tableModelListener );
		}

		this.tableModel = tableModel;
		if( this.tableModel != null ) {
			this.tableModel.addTableModelListener( tableModelListener );
		}

		clearSortingState();
		fireTableStructureChanged();
	}

	public JTableHeader getTableHeader()
	{
		return tableHeader;
	}

	public void setTableHeader( JTableHeader tableHeader )
	{
		if( this.tableHeader != null ) {
			this.tableHeader.removeMouseListener( mouseListener );
			final TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
			if( defaultRenderer instanceof SortableHeaderRenderer ) {
				this.tableHeader.setDefaultRenderer( ((SortableHeaderRenderer) defaultRenderer).tableCellRenderer );
			}
		}
		this.tableHeader = tableHeader;
		if( this.tableHeader != null ) {
			this.tableHeader.addMouseListener( mouseListener );
			this.tableHeader.setDefaultRenderer(
					new SortableHeaderRenderer( this.tableHeader.getDefaultRenderer() ));
		}
	}

	/**
	 *	Queries whether the table is sorted by some column.
	 *	Initially the table is not sorted. As soon as the user
	 *	clicks on a column header or if setSortingStatus is
	 *	called, the table will be sorted.
	 *
	 *	@return	<code>true</code> if the table is sorted by some column
	 */
	public boolean isSorting()
	{
		return( collSorted.size() != 0 );
	}

	/**
	 *	Queries the index of the sorted column.
	 *
	 *	@return	the sorted column index or -1 if not sorted by any column
	 */
	public int getSortedColumnIndex()
	{
		if( collSorted.isEmpty() ) {
			return -1;
		} else {
			return( ((Directive) collSorted.get( 0 )).column );
		}
	}

	/**
	 *	Queries the sorted column's direction.
	 *
	 *	@return	ASCENDING or DESCENDING. If the table is not sorted, returns NOT_SORTED
	 */
	public int getSortedColumnDirection()
	{
		if( collSorted.isEmpty() ) {
			return NOT_SORTED;
		} else {
			return( ((Directive) collSorted.get( 0 )).direction );
		}
	}

	/**
	 *	Sets the sorted column. Clears the previously sorted column.
	 *	Note that this will not check for allowed/disallowed columns.
	 *	
	 *	@param	columnIndex	index of the column to sort
	 *	@param	direction	either of ASCENDING, DESCENDING, or NOT_SORTED (equal to calling
	 *						cancelSorting())
	 */
	public void setSortedColumn( int columnIndex, int direction )
	{
		collSorted.clear();
		if( direction != NOT_SORTED ) {
			collSorted.add( new Directive( columnIndex, direction ));
		}
		sortingStatusChanged();
	}

	/**
	 *	Reverts view back to unsorted mode.
	 */
	public void cancelSorting()
	{
		collSorted.clear();
		sortingStatusChanged();
	}

	/**
	 *	Provides a custom comparator for a given
	 *	class of table cell values.
	 *	Note that this will not alter the currently active sorting
	 *	state in any way.
	 *
	 *	@param	type		the class of the values for which to use the custom comparator
	 *	@param	comparator	the comparator to use for this class of values, or <code>null</code>
	 *						to remove a custom comparator
	 */
	public void setColumnComparator( Class type, Comparator comparator )
	{
		if( comparator == null ) {
			mapComparators.remove( type );
		} else {
			mapComparators.put( type, comparator );
		}
	}
	
	/**
	 *	Defines a column to be sortable or not sortable.
	 *	Use this method to exempt certain columns from being
	 *	sortable. By default all columns are considered sortable.
	 *	Note that this will not alter the currently active sorting
	 *	state in any way.
	 *
	 *	@param	columnIndex	model index of the column to modify
	 *	@param	allowed		<code>true</code> to allow this column to be sorted,
	 *						<code>false</code> to prohibit sorting
	 */
	public void setSortingAllowed( int columnIndex, boolean allowed )
	{
		final Object o = new Integer( columnIndex );
	
		if( allowed ) {
			setDisallowedColumns.remove( o );
		} else {
			setDisallowedColumns.add( o );
		}
	}

	/**
	 *	Defines a column to be sortable or not sortable.
	 *	Use this method to exempt certain columns from being
	 *	sortable. By default all columns are considered sortable.
	 *	Note that this will not alter the currently active sorting
	 *	state in any way.
	 *
	 *	@param	columnIndex	model index of the column to modify
	 *	@return				<code>true</code> if it's allowed to sort this column,
	 *						<code>false</code> otherwise
	 */
	public boolean getSortingAllowed( int columnIndex )
	{
		return( !setDisallowedColumns.contains( new Integer( columnIndex )));
	}

	/**
	 *	Returns the row index in the model based
	 *	on a row index in the visual (sorted) representation.
	 *
	 *	@param	viewIndex	row index in the sorted visible table
	 *	@return				row index in the underlying (not sorted) model
	 *
	 *	@throws	IndexOutOfBoundException	if the viewIndex is out of range
	 */
	public int getModelIndex( int viewIndex )
	{
		return getViewToModel()[ viewIndex ].modelIndex;
	}

	/**
	 *	Returns the row index in the view based
	 *	on a row index in the (non-sorted) model.
	 *
	 *	@param	modelIndex	row index in the underlying (not sorted) model
	 *	@return				row index in the sorted visible table
	 *
	 *	@throws	IndexOutOfBoundException	if the modelIndex is out of range
	 */
	public int getViewIndex( int modelIndex )
	{
		return getModelToView()[ modelIndex ];
	}

	// ----------------- private -----------------

	/*
	 *	Queries whether the table is sorted by some column.
	 *	Initially the table is not sorted. As soon as the user
	 *	clicks on a column header or if setSortingStatus is
	 *	called, the table will be sorted.
	 *
	 *	@param	column	the column index to check for sorting
	 *	@return	<code>true</code> if the table is sorted by the given column
	 */
	private int getSortingStatus( int column )
	{
		return( getDirective( column ).direction );
	}
	
	private void setSortingStatus( int column, int status )
	{
		final Directive directive = getDirective( column );
		if( directive != EMPTY_DIRECTIVE ) {
			collSorted.remove( directive );
		}
		if( status != NOT_SORTED ) {
			collSorted.add( new Directive( column, status ));
		}
		sortingStatusChanged();
	}

	private Icon getHeaderRendererIcon( int column )
	{
		return icnArrow[ getDirective( column ).direction + 1 ];
	}

	private Comparator getComparator( int column )
	{
		final Class			columnType = tableModel.getColumnClass( column );
		final Comparator	comparator = (Comparator) mapComparators.get( columnType );

		if( comparator != null ) {
			return comparator;
		}
		if( Comparable.class.isAssignableFrom( columnType )) {
			return COMPARABLE_COMPARATOR;
		}
		return STRING_COMPARATOR;
	}

	private void clearSortingState()
	{
		viewToModel = null;
		modelToView = null;
	}

	private Directive getDirective( int column )
	{
		Directive directive;
	
		for( int i = 0; i < collSorted.size(); i++ ) {
			directive = (Directive) collSorted.get( i );
			if( directive.column == column ) {
				return directive;
			}
		}
		return EMPTY_DIRECTIVE;
	}

	private void sortingStatusChanged()
	{
		clearSortingState();
		fireTableDataChanged();
		if( tableHeader != null ) {
			tableHeader.repaint();
		}
	}

	private Row[] getViewToModel()
	{
		if( viewToModel == null ) {
			final int tableModelRowCount = tableModel.getRowCount();
			viewToModel = new Row[ tableModelRowCount ];
			for( int row = 0; row < tableModelRowCount; row++ ) {
				viewToModel[ row ] = new Row( row );
			}

			if( isSorting() ) {
				Arrays.sort( viewToModel );
			}
		}
		return viewToModel;
	}

	private int[] getModelToView()
	{
		if( modelToView == null ) {
			final int n = getViewToModel().length;
			modelToView = new int[ n ];
			for( int i = 0; i < n; i++ ) {
				modelToView[ getModelIndex( i )] = i;
			}
		}
		return modelToView;
	}

	// --------------- TableModel interface ---------------

	public int getRowCount()
	{
		return( (tableModel == null) ? 0 : tableModel.getRowCount() );
	}

	public int getColumnCount()
	{
		return( (tableModel == null) ? 0 : tableModel.getColumnCount() );
	}

	public String getColumnName( int column )
	{
		return( tableModel.getColumnName( column ));
	}

	public Class getColumnClass( int column )
	{
		return tableModel.getColumnClass( column );
	}

	public boolean isCellEditable( int row, int column )
	{
		return tableModel.isCellEditable( getModelIndex( row ), column );
	}

	public Object getValueAt( int row, int column )
	{
		return tableModel.getValueAt( getModelIndex( row ), column );
	}

	public void setValueAt( Object aValue, int row, int column )
	{
		tableModel.setValueAt( aValue, getModelIndex( row ), column );
	}

	// ------------- internal helper classes -------------
	
	private class Row
	implements Comparable
	{
		private int modelIndex;

		public Row( int index )
		{
			this.modelIndex = index;
		}

		public int compareTo( Object o )
		{
			final int	row1		= modelIndex;
			final int	row2		= ((Row) o).modelIndex;
			Directive	directive;
			int			column;
			Object		o1, o2;
			int			comparison;

			for( Iterator it = collSorted.iterator(); it.hasNext(); ) {
				directive	= (Directive) it.next();
				column		= directive.column;
				o1			= tableModel.getValueAt( row1, column );
				o2			= tableModel.getValueAt( row2, column );

				comparison = 0;
				// Define null less than everything, except null.
				if( (o1 == null) && (o2 == null) ) {
					comparison = 0;
				} else if( o1 == null ) {
					comparison = -1;
				} else if( o2 == null ) {
					comparison = 1;
				} else {
					comparison = getComparator( column ).compare( o1, o2 );
				}
				if( comparison != 0 ) {
					return( directive.direction == DESCENDING ? -comparison : comparison );
				}
			}
			return 0;
		}
	} // class Row

	private class TableModelHandler
	implements TableModelListener
	{
		public void tableChanged( TableModelEvent e )
		{
			int column, viewIndex;
		
			// If we're not sorting by anything, just pass the event along.				
			if( !isSorting() ) {
				clearSortingState();
				fireTableChanged( e );
				return;
			}
				
			// If the table structure has changed, cancel the sorting; the			   
			// sorting columns may have been either moved or deleted from			  
			// the model. 
			if( e.getFirstRow() == TableModelEvent.HEADER_ROW ) {
				cancelSorting();
				fireTableChanged( e );
				return;
			}

			// We can map a cell event through to the view without widening				
			// when the following conditions apply: 
			// 
			// a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and, 
			// b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
			// c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and, 
			// d) a reverse lookup will not trigger a sort (modelToView != null)
			//
			// Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
			// 
			// The last check, for (modelToView != null) is to see if modelToView 
			// is already allocated. If we don't do this check; sorting can become 
			// a performance bottleneck for applications where cells  
			// change rapidly in different parts of the table. If cells 
			// change alternately in the sorting column and then outside of				
			// it this class can end up re-sorting on alternate cell updates - 
			// which can be a performance problem for large tables. The last 
			// clause avoids this problem. 
			column = e.getColumn();
			if( (e.getFirstRow() == e.getLastRow()) &&
				(column != TableModelEvent.ALL_COLUMNS) &&
				(getSortingStatus( column ) == NOT_SORTED) &&
				(modelToView != null) ) {
				
				viewIndex = getModelToView()[ e.getFirstRow() ];
				fireTableChanged( new TableModelEvent( SortedTableModel.this, 
													   viewIndex, viewIndex, 
													   column, e.getType() ));
				return;
			}

			// Something has happened to the data that may have invalidated the row order. 
			clearSortingState();
			fireTableDataChanged();
			return;
		}
	} // class TableModelHandler

	private class MouseHandler
	extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			final JTableHeader		h			= (JTableHeader) e.getSource();
			final TableColumnModel	columnModel = h.getColumnModel();
			final int				viewColumn	= columnModel.getColumnIndexAtX( e.getX() );
			final int				column		= columnModel.getColumn( viewColumn ).getModelIndex();
			int						status;

			if( (column != -1) && !setDisallowedColumns.contains( new Integer( column ))) {
// SCISS REMOVED
//				status = getSortingStatus( column );
//				if( !e.isControlDown() ) {
//					cancelSorting();
//				}
//				// Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or 
//				// {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed. 
//				status = status + (e.isShiftDown() ? -1 : 1);
//				status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
				status = getSortingStatus( column );
				cancelSorting();
				status = status == ASCENDING ? DESCENDING : ASCENDING;
				setSortingStatus( column, status );
			}
		}
	} // class MouseHandler

	private static class ArrowIcon
	implements Icon
	{
		private static final Color			colrBg = new Color( 0x70, 0x70, 0x70, 0xF0 );
		private static final GeneralPath	shpUpTri;
		private static final GeneralPath	shpDownTri;
		private final		 Shape			shp;
		
		static {
			shpUpTri = new GeneralPath();
			shpUpTri.moveTo( 0f, 7f );
			shpUpTri.lineTo( 3.5f, 0f );
			shpUpTri.lineTo( 7f, 7f );
			shpUpTri.closePath();

			shpDownTri = new GeneralPath();
			shpDownTri.moveTo( 0f, 0f );
			shpDownTri.lineTo( 3.5f, 7f );
			shpDownTri.lineTo( 7f, 0f );
			shpDownTri.closePath();
		}
	
		public ArrowIcon( int style )
		{
			switch( style ) {
			case ASCENDING:
				shp			= shpUpTri;
				break;
			case DESCENDING:
				shp			= shpDownTri;
				break;
			case NOT_SORTED:
				shp			= null;
				break;
			default:
				throw new IllegalArgumentException( String.valueOf( style ));
			}
		}

		public void paintIcon( Component c, Graphics g, int x, int y )
		{
			if( shp == null ) return;
		
			final Graphics2D g2 = (Graphics2D) g;
		
			g2.translate( x, y );
			g2.setColor( colrBg );
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2.fill( shp );
			g2.translate( -x, -y );
		}				

		public int getIconWidth()
		{
			return 8;
		}

		public int getIconHeight()
		{
			return 8;
		}
	} // class ArrowIcon

	private class SortableHeaderRenderer
	implements TableCellRenderer
	{
		private final TableCellRenderer tableCellRenderer;

		public SortableHeaderRenderer( TableCellRenderer tableCellRenderer )
		{
			this.tableCellRenderer = tableCellRenderer;
		}

		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			final Component c = tableCellRenderer.getTableCellRendererComponent(
									table, value, isSelected, hasFocus, row, column );
			final JLabel	l;
			final int		modelColumn;
			
			if( c instanceof JLabel ) {
				l			= (JLabel) c;
				modelColumn = table.convertColumnIndexToModel( column );
				l.setHorizontalTextPosition( JLabel.LEFT );
				l.setIcon( getHeaderRendererIcon( modelColumn ));
			}
			return c;
		}
	} // class SortableHeaderRenderer

	private static class Directive
	{
		private final int column;
		private final int direction;

		public Directive( int column, int direction )
		{
			this.column		= column;
			this.direction	= direction;
		}
	} // class Directive
}