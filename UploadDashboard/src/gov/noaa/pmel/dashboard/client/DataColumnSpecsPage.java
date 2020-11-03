/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.text.client.IntegerParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.ADCMessageList;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;
import gov.noaa.pmel.dashboard.shared.TypesDatasetDataPair;

/**
 * Page for specifying the data column types in a DashboardDatasetData.
 * 
 * @author Karl Smith
 */
public class DataColumnSpecsPage extends CompositeWithUsername {

    private static class TypePosition { 
        final String type;
        final int position;
        TypePosition(String _type, int _position) {
            if ( _type == null || _type.trim().length() == 0 ) throw new IllegalArgumentException("Empty datatype");
            type = _type.trim();
            position = _position;
        }
        @Override
        public int hashCode() {
            return type.hashCode() + position;
        }
        @Override
        public boolean equals(Object other) {
            if ( other == null ) return false;
            if ( ! (other instanceof TypePosition )) return false;
            TypePosition otp = (TypePosition)other;
            return this.type.equals(otp.type) && this.position == otp.position;
        }
    }
	private static final int DATA_COLUMN_WIDTH = 16;

    private static boolean HIGHLIGHT_USER_FLAGS = false;
    
	private static final String TITLE_TEXT = "Identify Data Columns";

	private static final String MESSAGES_TEXT = "Show errors/warnings";

	private static final String SUBMIT_TEXT = "Check Data";
	private static final String ENABLED_SUBMIT_HOVER_HELP = 
			"Submits the current data column types and checks the given data";
	private static final String DISABLED_SUBMIT_HOVER_HELP = 
			"This cruise has been submitted for QC.  Data column types cannot be modified.";

	private static final String DONE_TEXT = "Done";
	private static final String SAVE_BUTTON_TEXT = "Save";
	private static final String SAVE_BUTTON_HOVER_HELP = "Save column data type definitions";

	private static final String PAGER_LABEL_TEXT = "Rows shown";

	private static final String NOT_CHECKED_MSG = "(data values not checked)";

	private static final String UNKNOWN_COLUMN_TYPE_PROLOGUE = 
			" data columns:<ul>";
	private static final String UNKNOWN_COLUMN_TYPE_EPILOGUE = 
			"</ul> are still <em>(unknown)</em> and need to be specified.";
	private static final String NO_LONGITUDE_ERROR_MSG = 
			"No data column has been identified as the longitude";
	private static final String NO_LATITUDE_ERROR_MSG =
			"No data column has been identified as the latitude";
	private static final String NO_DEPTH_ERROR_MSG =
			"No data column has been identified as the sample pressure or depth";
	private static final String NO_TIMESTAMP_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the date and time of each measurement";
	private static final String NO_DATE_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the date of each measurement";
	private static final String NO_TIME_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the time of each measurement";
	private static final String MISSING_DATE_PIECE_ERROR_MSG =
			"The data columns identified do not completely specify " +
			"the date of each measurement";
	private static final String MISSING_TIME_PIECE_ERROR_MSG =
			"The data columns identified do not completely specify " +
			"the time of each measurement";
	private static final String MULTIPLE_COLUMN_TYPES_ERROR_MSG =
			"More than one column has the type: ";

	private static final String DEFAULT_SECONDS_WARNING_QUESTION = 
			"No data columns have been identified providing the seconds " +
			"for the time of each measurement.  It is strongly recommended " +
			"that seconds be provided; however, a default value of zero " +
			"seconds can be added to the data." +
			"<br />" +
			"Is this okay?";
	private static final String USE_DEFAULT_SECONDS_TEXT = "Yes";
	private static final String NO_DEFAULT_SECONDS_TEXT = "No";

	private static final String GET_COLUMN_SPECS_FAIL_MSG = 
			"Problems obtaining the data column types";
	private static final String SUBMIT_FAIL_MSG = 
			"Problems updating the data column types";
	private static final String MORE_DATA_FAIL_MSG = 
			"Problems obtaining more data from the dataset";
	private static final String SAVE_FAIL_MSG = 
			"Problems saving column definitions";

	private static final String SANITY_CHECK_FAIL_MSG = 
			"The data check failed, indicating very serious errors in the data.";
	private static final String SANITY_CHECK_ERROR_MSG = 
			"The data check found serious errors in the data.";
	private static final String SANITY_CHECK_WARNING_MSG = 
			"The data check found possible errors (warnings) in the data";
	private static final String SANITY_CHECK_SUCCESS_MSG =
			"The data check did not find any problems in the data";

	private static final String DATA_NEVER_CHECKED_HTML = 
			"<h3>Warning: Data has not been checked.</h3>" +
			"<p>You will need need to check the data in a dataset before you " +
			"can submit the dataset for QC.  This data check will identify " +
			"errors in the data and discover incorrectly identified or unknown " +
			"data columns.  Although this data check can be run any time before " +
			"submitting a dataset, we recommend running it immediately after " +
			"uploading a dataset so that any incorrectly identified or unknown " +
			"data columns are correctly identified for subsequent dataset " +
			"uploads.</p>" +
			"<p>Leave this page without checking the data?</p>";
	private static final String CHANGES_NOT_SAVED_HTML =
			"<h3>Warning: Column settings not saved.</h3>" +
			"<p>Changes to data column types, units, and missing values are not " +
			"saved until the data has been checked with this updated data column " +
			"information.</p>" +
			"<p>Leave this page and lose the changes you have made?</p>";
	private static final String RETURN_TO_CRUISE_LIST_TEXT = "Yes";
	private static final String STAY_ON_THIS_PAGE_TEXT = "No";

	interface DataColumnSpecsPageUiBinder extends UiBinder<Widget, DataColumnSpecsPage> {
	}

	private static DataColumnSpecsPageUiBinder uiBinder = 
			GWT.create(DataColumnSpecsPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

    @UiField ApplicationHeaderTemplate header;
	@UiField DataGrid<ArrayList<String>> dataGrid;
	@UiField Label pagerLabel;
	@UiField Label messagesLabel;
	@UiField SimplePager gridPager;
	@UiField Button messagesButton;
	@UiField Button submitButton;
	@UiField Button doneButton;
	@UiField Button saveButton;
	
	private SingleSelectionModel<ArrayList<String>> selectionModel;
	private ADCMessageList datasetMessages;
	private Map<Integer, ADCMessage> rowMsgMap = new HashMap<Integer, ADCMessage>();

	// Popup to confirm continue with default zero seconds
	private DashboardAskPopup defaultSecondsPopup;
	// List of all known user data column types
	private ArrayList<DataColumnType> knownUserTypes;
	// Dataset associated with and updated by this page
	private DashboardDataset cruise;
	// List of DatasetDataColumn objects associated with the column Headers
	private ArrayList<DatasetDataColumn> cruiseDataCols;
	// Asynchronous data provider for the data grid 
	private AsyncDataProvider<ArrayList<String>> dataProvider;
	// Dialog warning that data has never been checked or changes have not been saved
	private DashboardAskPopup notCheckedPopup;
	// Flag indicating if the cruise data was ever checked
	private boolean cruiseNeverChecked;
	// Flag indicating if a logout generated the above warnings
	private boolean wasLoggingOut;
	// List of cruises to be assigned once this page is dismissed
	private ArrayList<String> expocodes;

	protected int selectedRow;

	protected int selectedColumn;

	// Singleton instance of this page
	private static DataColumnSpecsPage singleton = null;

    private static Logger logger = Logger.getLogger("DataColumnSpecsPage");
	/**
	 * Creates an empty cruise data column specification page.  
	 * Allows the user to update the data column types for a
	 * cruise when populated.
	 */
	DataColumnSpecsPage() {
        super(PagesEnum.IDENTIFY_COLUMNS.name());
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;
        
        logger.addHandler(new ConsoleLogHandler());
        logger.setLevel(Level.ALL);

		setUsername(null);
		defaultSecondsPopup = null;
		notCheckedPopup = null;
		cruiseNeverChecked = false;
		wasLoggingOut = false;

		header.setPageTitle(TITLE_TEXT);
        header.setLogoutHandler(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                logoutOnClick();
            }
        });

		messagesButton.setText(MESSAGES_TEXT);
		pagerLabel.setText(PAGER_LABEL_TEXT);
		submitButton.setText(SUBMIT_TEXT);
		doneButton.setText(DONE_TEXT);
		saveButton.setText(SAVE_BUTTON_TEXT);

		knownUserTypes = new ArrayList<DataColumnType>();
		cruise = new DashboardDataset();
		cruiseDataCols = new ArrayList<DatasetDataColumn>();
		expocodes = new ArrayList<String>();

		selectionModel = new SingleSelectionModel<ArrayList<String>>(new ProvidesKey<ArrayList<String>>() {
			@Override
			public Object getKey(ArrayList<String> item) {
				return item.get(0);
			}
		});
		dataGrid.setRowStyles(new RowStyles<ArrayList<String>>() {
			@Override
			public String getStyleNames(ArrayList<String> row, int rowIndex) {
				String rowStyle = "oa_row ";
				ADCMessage amsg = rowMsgMap.get(rowIndex);
				if ( amsg != null ) {
					switch (amsg.getSeverity()) {
						case WARNING:
							rowStyle += amsg.isUserFlag() && HIGHLIGHT_USER_FLAGS ? "oa_datagrid_user_warning_row" : "oa_datagrid_checker_warning_row";
							break;
						case ERROR:
						case CRITICAL:
							rowStyle += amsg.isUserFlag() && HIGHLIGHT_USER_FLAGS ? "oa_datagrid_user_error_row" : "oa_datagrid_checker_error_row";
							break;
						default:
					}
				}
				return rowStyle;
			}
		});
		// Create the asynchronous data provider for the data grid
		dataProvider = new AsyncDataProvider<ArrayList<String>>() {
			@Override
			protected void onRangeChanged(HasData<ArrayList<String>> display) {
				// Ignore the call if there is no dataset assigned
				if ( cruise.getDatasetId().isEmpty() )
					return;
				UploadDashboard.showWaitCursor();
				// Get the data for the cruise from the server
				final Range range = display.getVisibleRange();
                UploadDashboard.debugLog("Getting new data range:" + range);
				service.getDataWithRowNum(getUsername(), cruise.getDatasetId(), 
						range.getStart(), range.getLength(), 
						new OAPAsyncCallback<ArrayList<ArrayList<String>>>("get data rows") {
					@Override
					public void onSuccess(ArrayList<ArrayList<String>> newData) {
						int actualStart;
						try {
							actualStart = IntegerParser.instance().parse(newData.get(0).get(0).trim()) - 1;
						} catch (ParseException e) {
							actualStart = -1;
						}
                        UploadDashboard.debugLog("Received new data starting at " + actualStart);
						if ( actualStart < 0 )
							actualStart = range.getStart();
						updateRowData(actualStart, newData);
						updateScroll(true);
						UploadDashboard.showAutoCursor();
					}
				});
			}
		};
		dataProvider.addDataDisplay(dataGrid);
		// Assign the pager controlling which rows of the the data grid are shown
		gridPager.setDisplay(dataGrid);
		
		dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<ArrayList<String>>() {
			@Override
			public void onCellPreview(CellPreviewEvent<ArrayList<String>> event) {
				String eType = event.getNativeEvent().getType();
				if ( BrowserEvents.CLICK.equals(eType)) {
					selectedRow = event.getIndex();
					selectedColumn = event.getColumn();
					GWT.log("click");
				}
//				if ( BrowserEvents.DBLCLICK.equals(eType)) {
//					GWT.log("Double click");
//				}
			}
		});
		dataGrid.addDomHandler(new DoubleClickHandler() {
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				ADCMessage amsg = rowMsgMap.get(selectedRow);
				if ( amsg == null ) { return; }
				NativeEvent nevt = event.getNativeEvent();
				String ntype = nevt.getType();
				GWT.log("ntype:"+ntype);
				int left = event.getClientX();
				int top = event.getClientY();
				String msg = amsg.getDetailedComment();
				final PopupMsg pu = new PopupMsg(msg);
				pu.row = amsg.getRowIndex();
				pu.col = amsg.getColNumber();
				pu.addDomHandler(new DoubleClickHandler() {
					@Override
					public void onDoubleClick(DoubleClickEvent event) {
					    PopupMsg puSrc = (PopupMsg)event.getSource();
						Integer row = puSrc.row;
						Integer col = puSrc.col;
						if ( col != null ) { 
							setView(row, col);
						}
						puSrc.setVisible(false);
					}
				}, DoubleClickEvent.getType());
				pu.setPopupPosition(left, top);
				pu.show();
			}
		}, DoubleClickEvent.getType());
//		dataGrid.addDomHandler(new DoubleClickHandler() {
//			@Override
//			public void onDoubleClick(DoubleClickEvent event) {
//				ADCMessage amsg = rowMsgMap.get(selectedRow);
//				if ( amsg == null ) { return; }
//				Integer row = amsg.getRowIndex();
//				if ( row == null ) { return; } // XXX Shouldn't happen
//				Integer col = amsg.getColNumber();
//				int colIdx = col != null ? col.intValue()+2 : 0;
//				setView(row, colIdx);
//			}
//		}, DoubleClickEvent.getType());
	}

	Integer scrollToRow = null;
	Integer scrollToCol = null;
	
	private void updateScroll(boolean pageChanged) {
        int rowAdjustment = pageChanged ? 4 : 0;
        int colAdjustment = -2;
        int rowIdx = 0;
        int colIdx = 0;
	    try {
			if ( scrollToRow != null ) {
				rowIdx = scrollToRow != null ? scrollToRow.intValue() + rowAdjustment : 0;
				colIdx = scrollToCol != null ? scrollToCol.intValue() : 0;
                if ( colIdx >= 3 ) {
                    colIdx += colAdjustment;
                }
                // TODO: Adjust row and column indexes to put row&cell +/- center
                UploadDashboard.logToConsole("scrollRow:" + scrollToRow + ", scrollCol:"+ scrollToCol);
                UploadDashboard.logToConsole("currentPage:"+ gridPager.getPage() + ", start: " + gridPager.getPageStart() + ", size: "+ gridPager.getPageSize());
                Range range = dataGrid.getVisibleRange();
                int start = range.getStart();
                rowIdx = rowIdx - start;
                UploadDashboard.logToConsole("Range start: "+ range.getStart() + ", length:" + range.getLength());
				TableRowElement row;
                row = dataGrid.getRowElement(rowIdx);
                if ( row != null ) {
                    colIdx = Math.min(colIdx, row.getCells().getLength()-1);
                    UploadDashboard.logToConsole("getRow:col:"+rowIdx + ":"+ colIdx);
    				TableCellElement cell = row.getCells().getItem(colIdx);
    				cell.scrollIntoView();
                } else {
                    UploadDashboard.logToConsole("Failed to get row " + rowIdx );
                }
			}
	    } catch (Exception ex) {
            UploadDashboard.logToConsole(String.valueOf(ex));
            String msg = "page:"+gridPager.getPage() + ", rowIdx:"+rowIdx + ", showRow:"+ scrollToRow + ", colIdx: " + colIdx + " : " + ex;
            UploadDashboard.logToConsole(msg);
	    } finally {
			scrollToRow = null;
			scrollToCol = null;
	    }
		
//		TableCellElement idCol = row.getCells().getItem(0);
//		String id = String.valueOf(idCol.getInnerText());
//		ArrayList<String> al = new ArrayList<String>();
//		al.add(id);
//		selectionModel.setSelected(al, true);
	}
	
	/**
	 * Display the cruise data column specifications page for a cruise
	 * with the latest cruise data column specifications from the server.
	 * Adds this page to the page history.
	 * 
	 * @param username
	 * 		username for this page
	 * @param dataset
	 * 		show the specifications for this cruise
	 */
	static void showPage(String username, DashboardDatasetList cruises) {
        showPage(username, cruises.values());
	}
	static void showPage(String username, Collection<DashboardDataset> cruises) {
        if ( cruises.size() < 1 ) {
            throw new IllegalStateException("No datasets specified for Data Column Identification.");
        }
		if ( singleton == null )
			singleton = new DataColumnSpecsPage();

		singleton.setUsername(username);
        singleton.setDatasetIds(cruises);
		UploadDashboard.showWaitCursor();
		service.getDataColumnSpecs(singleton.getUsername(), cruises.iterator().next().getDatasetId(), 
								new OAPAsyncCallback<TypesDatasetDataPair>("get data column information") {
			@Override
			public void onSuccess(TypesDatasetDataPair cruiseSpecs) {
				if ( cruiseSpecs != null ) {
					UploadDashboard.updateCurrentPage(singleton);
					singleton.updateCruiseSpecs(cruiseSpecs);
				}
				else {
					UploadDashboard.showMessage(GET_COLUMN_SPECS_FAIL_MSG + 
						" (unexpected null cruise column specificiations)");
				}
				UploadDashboard.showAutoCursor();
			}
		});
	}

	/**
     * @param expocodes2
     */
    private void setDatasetIds(Collection<DashboardDataset> datasets) {
		expocodes.clear();
        List<String>datasetIds = new ArrayList<>(datasets.size());
        List<String>names = new ArrayList<>(datasets.size());
        for (DashboardDataset dd : datasets) {
            datasetIds.add(dd.getRecordId());
            names.add(dd.getUserDatasetName());
        }
		expocodes.addAll(datasetIds);
        header.addDatasetIds(names);
    }

    protected void updateCruiseSpecs(TypesDatasetDataPair cruiseSpecs) {
		knownUserTypes.clear();
		if ( cruiseSpecs.getAllKnownTypes() != null )
			knownUserTypes.addAll(cruiseSpecs.getAllKnownTypes());
		updateDatasetDataCheckMessages(cruiseSpecs.getMsgList());
		updateDatasetUserQcFlagMessages(cruiseSpecs.getDatasetData());
		updateDatasetColumnSpecs(cruiseSpecs.getDatasetData());
		scrollToCol = null;
		scrollToRow = null;
    }

    /**
	 * Redisplays the last version of this page if the username
	 * associated with this page matches the given username.
	 */
	static void redisplayPage(String username) {
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) ) {
			DatasetListPage.showPage();
		}
		else {
			UploadDashboard.updateCurrentPage(singleton);
		}
	}

	static void redisplayPage(String username, Integer rowNumber, Integer columnNumber) {
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) ) {
			DatasetListPage.showPage();
		}
		else {
			UploadDashboard.updateCurrentPage(singleton);
			singleton.setView(rowNumber, columnNumber);
		}
	}
	
	void setView(Integer rowNumber, Integer columnNumber) {
		int showColumnIdx = columnNumber == null || columnNumber.equals(DashboardUtils.INT_MISSING_VALUE) ? 
								0 : columnNumber.intValue() - 1;
		int showRowIdx = rowNumber == null || rowNumber.equals(DashboardUtils.INT_MISSING_VALUE) ? 
								0 : rowNumber.intValue() - 1;
        UploadDashboard.logToConsole("colIdx:"+showColumnIdx +", rowIdx:"+ showRowIdx);
		int nPages = gridPager.getPageCount();
		int pageSize = gridPager.getPageSize();
		int showPage = showRowIdx / pageSize;
		int pageRow = showRowIdx > pageSize ? showRowIdx % pageSize : showRowIdx;
		int pageMaxIdx = pageSize - 1;
		if ( showPage == nPages - 1) {
			int totalRows = dataGrid.getRowCount();
			pageMaxIdx = ( totalRows % pageSize ) - 1;
		}
		pageRow = Math.min(pageRow, pageMaxIdx);
        pageRow = pageRow + ( pageSize * showPage );
//        int nCols = dataGrid.getRowElement(0).getCells().getLength();
		scrollToRow = new Integer(pageRow);
		scrollToCol = new Integer(showColumnIdx);
		int cPage = gridPager.getPage();
		if ( cPage != showPage ) {
            UploadDashboard.logToConsole("Set page from " + cPage + " to " + showPage);
			gridPager.setPage(showPage); // scroll will happen (if necessary) on page data load
		} 
		else {
    		try {
    			updateScroll(false);
    		} catch (Exception ex) {
                UploadDashboard.logToConsole(String.valueOf(ex));
                String msg = "page:"+gridPager.getPage() + ", rowIdx:"+showRowIdx + ", showRow:"+ pageRow + ", colIdx: " + showColumnIdx + " : " + ex;
                UploadDashboard.logToConsole(msg);
    		}
		}
		
		
//			int dgHeight = singleton.dataGrid.getOffsetHeight();
//			int vpos = singleton.dgScroll.getVerticalScrollPosition();
//			int hpos = singleton.dgScroll.getHorizontalScrollPosition();
//			Window.alert("h:"+dgHeight+", v:"+vpos+", h:"+hpos);
	}

	/**
	 * Updates the data column specification page with the given
	 * column types and data.  Modifies cruiseSpecs in that the
	 * dashboard-generated sample number is inserted at the beginning
	 * of each row of data. 
	 * 
	 * @param cruiseSpecs
	 * 		current cruise data column type specifications and
	 * 		initial cruise data for display
	 */
	private void updateDatasetColumnSpecs(DashboardDatasetData cruiseSpecs) {
		header.userInfoLabel.setText(WELCOME_INTRO + getUsername());

		String status = cruiseSpecs.getDataCheckStatus();
		if ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED)) {
			cruiseNeverChecked = true;
			messagesLabel.setText(NOT_CHECKED_MSG);
		}
		else {
			cruiseNeverChecked = false;
			String msgText;
			int numErrors = cruiseSpecs.getNumErrorRows();
			if ( numErrors == 0 )
				msgText = "no";
			else
				msgText = Integer.toString(numErrors);
			msgText += " errors; ";
			int numWarns = cruiseSpecs.getNumWarnRows();
			if ( numWarns == 0 )
				msgText += "no";
			else
				msgText += Integer.toString(numWarns);
			msgText += " warnings";
			messagesLabel.setText(msgText);
		}

		if ( Boolean.TRUE.equals(cruiseSpecs.isEditable()) ) {
			submitButton.setEnabled(true);
			submitButton.setTitle(ENABLED_SUBMIT_HOVER_HELP);
			saveButton.setEnabled(true);
			saveButton.setTitle(SAVE_BUTTON_HOVER_HELP);
		}
		else {
			submitButton.setEnabled(false);
			submitButton.setTitle(DISABLED_SUBMIT_HOVER_HELP);
			saveButton.setEnabled(false);
			saveButton.setTitle(DISABLED_SUBMIT_HOVER_HELP);
		}

        String recordId = cruiseSpecs.getRecordId();
        
		// Clear the dataset in case the data provider gets called while clearing
        cruiseSpecs.setRecordId(null);
		cruise.setRecordId(null);

		// Delete any existing columns and headers
		int k = dataGrid.getColumnCount();
		while ( k > 0 ) {
			k--;
			dataGrid.removeColumn(k);
		}
		// Clear the list of DatasetDataColumns
		cruiseDataCols.clear();

        cruise = cruiseSpecs;
        
        // --- from here
        /*
		// Assign the new cruise information needed by this page
		cruise.setNumDataRows(cruiseSpecs.getNumDataRows());
		cruise.setUserColNames(cruiseSpecs.getUserColNames());
		cruise.setDataColTypes(cruiseSpecs.getDataColTypes());

		TreeSet<QCFlag> woceSet = new TreeSet<QCFlag>();
		for ( QCFlag chkwoce : cruiseSpecs.getCheckerFlags() )
			woceSet.add(new QCFlag(null, null, chkwoce.getSeverity(), chkwoce.getColumnIndex(), chkwoce.getRowIndex()));
		cruise.setCheckerFlags(woceSet);

		woceSet.clear();
		for ( QCFlag uwoce : cruiseSpecs.getUserFlags() )
			woceSet.add(new QCFlag(null, null, uwoce.getSeverity(), null, uwoce.getRowIndex()));
		cruise.setUserFlags(woceSet);

		cruise.setSubmitStatus(cruiseSpecs.getSubmitStatus());
		cruise.setArchiveStatus(cruiseSpecs.getArchiveStatus());
		cruise.setDatasetId(cruiseSpecs.getDatasetId());
        */
        cruiseSpecs.setRecordId(recordId);

//		introHtml.setHTML(INTRO_HTML_PROLOGUE +  
//				SafeHtmlUtils.htmlEscape(cruise.getDatasetId()) + 
//				INTRO_HTML_EPILOGUE);

		// Rebuild the data grid using the provided DatasetDataColumnSpecs
		if ( cruise.getDataColTypes().size() < 4 )
			throw new IllegalArgumentException(
					"Unexpected small number of data columns: " + 
					cruise.getDataColTypes().size());
		int minTableWidth = 2;
		// First column is the dashboard-generated sample number (no header)
		ArrayListTextColumn rowNumColumn = new ArrayListTextColumn(0, cruise, rowMsgMap);
		dataGrid.addColumn(rowNumColumn);
		dataGrid.setColumnWidth(rowNumColumn, UploadDashboard.NARROW_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NARROW_COLUMN_WIDTH;
		// Rest of the columns are actual data columns
		for (k = 0; k < cruise.getDataColTypes().size(); k++) {
			// TextColumn for displaying the data strings for this column
			ArrayListTextColumn dataColumn = new ArrayListTextColumn(k+1, cruise, rowMsgMap);
			// DatasetDataColumn for creating the Header cell for this column
			DatasetDataColumn cruiseColumn = new DatasetDataColumn(knownUserTypes, cruise, k);
			// Maintain a reference to the DatasetDataColumn object
			cruiseDataCols.add(cruiseColumn);
			// Add this data column and the header to the grid
			dataGrid.addColumn(dataColumn, cruiseColumn.getHeader());
			// Set the width of this column - all the same width
			dataGrid.setColumnWidth(dataColumn, DATA_COLUMN_WIDTH, Style.Unit.EM);
			// Add this width to the minimum table width
			minTableWidth += DATA_COLUMN_WIDTH;
		}
		// Set the minimum table width
		dataGrid.setMinimumTableWidth(minTableWidth, Style.Unit.EM);
		// Update the data provider with the data in the DatasetDataColumnSpecs
		dataProvider.updateRowCount(cruise.getNumDataRows(), true);
		// Just insert the row numbers into each data row (modifying cruiseSpecs)
		ArrayList<ArrayList<String>> dataWithRowNums = cruiseSpecs.getDataValues();
		ArrayList<Integer> rowNums = cruiseSpecs.getRowNums();
		k = 0;
		for ( ArrayList<String> dataRow : dataWithRowNums ) {
			String rowNumStr = rowNums.get(k).toString();
			dataRow.add(0, rowNumStr);
			k++;
		}
		
		dataProvider.updateRowData(0, cruiseSpecs.getDataValues());
		// Reset shown rows to the start of the data
		dataGrid.setPageStart(0);
		// Set the number of data rows to display in the grid.
		// This will refresh the view.
		dataGrid.setPageSize(DashboardUtils.MAX_ROWS_PER_GRID_PAGE);
	}

	private void updateDatasetUserQcFlagMessages(DashboardDatasetData cruiseSpecs) {
        if ( ! HIGHLIGHT_USER_FLAGS ) { return; }
		for ( QCFlag flag : cruiseSpecs.getUserFlags()) {
			Integer rowIdx = flag.getRowIndex();
			if ( DashboardUtils.INT_MISSING_VALUE.equals(rowIdx)) { continue; }
			ADCMessage m = new ADCMessage();
            m.setUserFlag(true);
			m.setSeverity(flag.getSeverity());
			m.setColIndex(flag.getColumnIndex());
			m.setRowIndex(rowIdx);
			String msg = "User QC Flag set to " + flag.getFlagValue() + " for row " + (rowIdx.intValue()+1);
			Integer colIdx = flag.getColumnIndex();
			if ( ! DashboardUtils.INT_MISSING_VALUE.equals(colIdx)) { 
				msg += " on column " + (colIdx.intValue()+1);
			}
			m.setDetailedComment(msg);
			rowMsgMap.put(rowIdx, m);
		}
	}
	protected void updateDatasetDataCheckMessages(ADCMessageList msgList) {
		rowMsgMap.clear();
		datasetMessages = msgList;
		if ( datasetMessages != null ) {
			for (ADCMessage msg : datasetMessages) {
				rowMsgMap.put(msg.getRowIndex(), msg);
			}
		}
	}

    //	keep this here, since we have to do change detection
	void logoutOnClick() {
		// Check if any changes have been made
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
			// Ask before logging out
			wasLoggingOut = true;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(CHANGES_NOT_SAVED_HTML);
		}
		else {
			// No changes; just log out
            header.doLogout();
		}
	}

	@UiHandler("doneButton")
	void doneOnClick(ClickEvent event) {
        UploadDashboard.pingService(new OAPAsyncCallback<Void>("session check") {
            @Override
            public void onSuccess(Void nothing) {
                _doneOnClick(event);
            }
        });
	}
	void _doneOnClick(ClickEvent event) {
		// Check if any changes have been made
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
			// Ask before returning to the cruise list
			wasLoggingOut = false;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(CHANGES_NOT_SAVED_HTML);
		}
		else if ( cruiseNeverChecked ) {
			// Ask before returning to the cruise list
			wasLoggingOut = false;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(DATA_NEVER_CHECKED_HTML);
		}
		else {
			// No changes since last update
			// If only one cruise, done
			if ( expocodes.size() < 2 ) {
				DatasetListPage.showPage();
				return;
			}
			// Put up the wait cursor and send the rest of the cruises through the sanity checker
			UploadDashboard.showWaitCursor();
			expocodes.remove(0);
			service.updateDataColumns(getUsername(), expocodes, new OAPAsyncCallback<Void>("update data column specifications") {
				@Override
				public void onSuccess(Void result) {
					// Go to the list of cruises without comment; return to the normal cursor
					DatasetListPage.showPage();
					UploadDashboard.showAutoCursor();
					return;
				}
			});
		}
	}

	/**
	 * Generate the question popup warning that the data has never been checked
	 */
	private void makeNotCheckedPopup() {
		notCheckedPopup = new DashboardAskPopup(RETURN_TO_CRUISE_LIST_TEXT, 
				STAY_ON_THIS_PAGE_TEXT, new AsyncCallback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				if ( result.booleanValue() ) {
					if ( wasLoggingOut ) {
						wasLoggingOut = false;
						header.doLogout();
					}
					else {
						// Return to the latest cruise listing page, which may  
						// have been updated from previous actions on this page.
						DatasetListPage.showPage();
					}
				}
				else {
					// Just stay on this page
					wasLoggingOut = false;
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				Window.alert("Error from popup dialog: " + ex.toString());  // should never be called
			}
		});
	}

	@UiHandler("messagesButton") 
	void showMessagesOnClick(ClickEvent event) {
		DataMessagesPage.showPage(getUsername(), cruise.getRecordId(), cruise.getUserDatasetName());
	}

	@UiHandler("submitButton")
	void submitOnClick(ClickEvent event) {
		if ( ! Boolean.TRUE.equals(cruise.isEditable()) ) {
			// Should never get here, but just in case
			UploadDashboard.showMessage(DISABLED_SUBMIT_HOVER_HELP);
			return;
		}
        UploadDashboard.pingService(new OAPAsyncCallback<Void>("session check") {
            @Override
            public void onSuccess(Void nothing) {
                _submitOnClick(event);
            }
        });
	}
	void _submitOnClick(ClickEvent event) {
        
		// longitude given?
		boolean hasLongitude = false;
		// latitude given?
		boolean hasLatitude = false;
		// date/time given?
		boolean hasYear = false;
		boolean hasMonth = false;
		boolean hasDay = false;
		boolean hasHour = false;
		boolean hasMinute = false;
		boolean hasSecond = false;

		// list of data columns still given as unknown
		ArrayList<Integer> unknownIndices = new ArrayList<Integer>();

		// Check the column types 
		// Marking what is given no longer tries to guess what the sanity checker
		// can work with for time and date; only the possibility.
		// Similarly with aqueous CO2 and fCO2 recomputations.
		int k = 0;
		for ( DataColumnType colType : cruise.getDataColTypes() ) {
			if ( DashboardUtils.UNKNOWN.typeNameEquals(colType) ) {
				unknownIndices.add(k);
			}
			else if ( DashboardUtils.TIMESTAMP.typeNameEquals(colType) ) {
				hasYear = true;
				hasMonth = true;
				hasDay = true;
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.DATE.typeNameEquals(colType) ) {
				hasYear = true;
				hasMonth = true;
				hasDay = true;
			}
			else if ( DashboardUtils.DAY_OF_YEAR.typeNameEquals(colType) ) {
				hasMonth = true;
				hasDay = true;
				// Day of year could be floating point; verification needed?
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.SECOND_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.YEAR.typeNameEquals(colType) ) {
				hasYear = true;
			}
			else if ( DashboardUtils.MONTH_OF_YEAR.typeNameEquals(colType) ) {
				hasMonth = true;
			}
			else if ( DashboardUtils.DAY_OF_MONTH.typeNameEquals(colType) ) {
				hasDay = true;
			}
			else if ( DashboardUtils.TIME_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.HOUR_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
			}
			else if ( DashboardUtils.MINUTE_OF_HOUR.typeNameEquals(colType) ) {
				hasMinute = true;
			}
			else if ( DashboardUtils.SECOND_OF_MINUTE.typeNameEquals(colType) ) {
				hasSecond = true;
			}
			else if ( DashboardUtils.LONGITUDE.typeNameEquals(colType) ) {
				hasLongitude = true;
			}
			else if ( DashboardUtils.LATITUDE.typeNameEquals(colType) ) {
				hasLatitude = true;
			}
			k++;
		}
		if ( ! hasLongitude ) {
			// no longitude - error
			UploadDashboard.showMessage(NO_LONGITUDE_ERROR_MSG);
			return;
		}
		if ( ! hasLatitude ) {
			// no latitude - error
			UploadDashboard.showMessage(NO_LATITUDE_ERROR_MSG);
			return;
		}
		if ( ! (hasYear || hasMonth || hasDay || hasHour || hasMinute) ) {
			// timestamp completely missing - error
			UploadDashboard.showMessage(NO_TIMESTAMP_ERROR_MSG);
			return;
		}
		if ( ! (hasYear || hasMonth || hasDay) ) {
			// date completely missing - error
			UploadDashboard.showMessage(NO_DATE_ERROR_MSG);
			return;
		}
		if ( ! (hasHour || hasMinute) ) {
			// time completely missing - error
			UploadDashboard.showMessage(NO_TIME_ERROR_MSG);
			return;
		}
		if ( ! (hasYear && hasMonth && hasDay) ) {
			// incomplete date given - error
			UploadDashboard.showMessage(MISSING_DATE_PIECE_ERROR_MSG);
			return;
		}
		if ( ! (hasHour && hasMinute) ) {
			// incomplete time given - error
			UploadDashboard.showMessage(MISSING_TIME_PIECE_ERROR_MSG);
			return;
		}
		// Make sure there is no more than one of each column types - except OTHER
		HashMap<String, TypePosition> typeSet = new HashMap<>();
		Set<TypePosition> duplicates = new HashSet<TypePosition>();
        int pos = 0;
		for ( DataColumnType colType : cruise.getDataColTypes() ) {
            TypePosition ctp = new TypePosition(colType.getDisplayName(), ++pos);
			if ( DashboardUtils.OTHER.typeNameEquals(colType) ) {
				continue; // Multiple OTHER column types are allowed
			} else if ( DashboardUtils.UNKNOWN.typeNameEquals(colType) ) {
				continue; // Multiple UNKNOWN column types are allowed .. for now.
			} else if ( typeSet.containsKey(colType.getDisplayName())) {
				duplicates.add(ctp);
			} else {
			    typeSet.put(colType.getDisplayName(), ctp);
			}
		}
		if ( duplicates.size() > 0 ) {
			String errMsg = MULTIPLE_COLUMN_TYPES_ERROR_MSG;
			int cnt = 0;
			for ( TypePosition dup : duplicates ) {
				cnt++;
				if ( (cnt == 5) && (duplicates.size() > 5) ) {
					errMsg += "<li> ... </li>";
					break;
				}
				errMsg += "<li>" + dup.type + " at columns " + typeSet.get(dup.type).position + " and " + dup.position + "</li>";
			}
			UploadDashboard.showMessage(errMsg);
			return;
		}

//		if ( unknownIndices.size() > 0 ) {
//			// Unknown column data types found; put up error message and return
////			ArrayList<String> colNames = cruise.getUserColNames();
////			String errMsg = Integer.toString(unknownIndices.size()) + 
////					UNKNOWN_COLUMN_TYPE_PROLOGUE;
////			int cnt = 0;
////			for ( int idx : unknownIndices ) {
////				cnt++;
////				if ( (cnt == 5) && (unknownIndices.size() > 5) ) {
////					errMsg += "<li> ... </li>";
////					break;
////				}
////				errMsg += "<li>" + SafeHtmlUtils.htmlEscape(colNames.get(idx)) + "</li>";
////			}
////			errMsg += UNKNOWN_COLUMN_TYPE_EPILOGUE;
////            UploadDashboard.showMessage(errMsg);
////			return;
//			UploadDashboard.theresAproblem("There are data columns of unknown type. Continue anyway?", "Continue", "Cancel", new AsyncCallback<Boolean>() {
//                @Override
//                public void onFailure(Throwable arg0) {
//                    // Doesn't happen
//                }
//                @Override
//                public void onSuccess(Boolean ignore) {
//                    if ( ignore.booleanValue() ) {
//                        doSubmit();
//                    }
//                }
//			}); 
//		} else if ( ! hasSecond ) {
//			// Warning about missing seconds, asking whether to continue
//			if ( defaultSecondsPopup == null ) {
//				defaultSecondsPopup = new DashboardAskPopup(USE_DEFAULT_SECONDS_TEXT,
//						NO_DEFAULT_SECONDS_TEXT, new AsyncCallback<Boolean>() {
//					@Override
//					public void onSuccess(Boolean okay) {
//						// Only continue if okay to use default zero for seconds
//						if ( okay )
//							doSubmit();
//					}
//					@Override
//					public void onFailure(Throwable caught) {
//						// never called
//					}
//				});
//			}
//			defaultSecondsPopup.askQuestion(DEFAULT_SECONDS_WARNING_QUESTION);
//			return;
//	   } else {
//    		// longitude, latitude, sea water co2, and some form of a timestamp 
//    		// is present so continue on  
    		doSubmit();
//	   }
	}

	private void doSubmit() {
		// Show the wait cursor
		UploadDashboard.showWaitCursor();
		// Submit the updated data column types to the server.
		// This update invokes the SanityChecker on the data and
		// the results are then reported back to this page.
		service.updateDataColumnSpecs(getUsername(), cruise, 
				new OAPAsyncCallback<TypesDatasetDataPair>("check data") {
			@Override
			public void onSuccess(TypesDatasetDataPair tddp) {
				if ( tddp == null ) {
					UploadDashboard.showMessage(SUBMIT_FAIL_MSG + " (unexpected null response returned)");
					UploadDashboard.showAutoCursor();
					return;
				}
				DashboardDatasetData ddd = tddp.getDatasetData();
				if ( ddd == null ) {
					UploadDashboard.showMessage(SUBMIT_FAIL_MSG + " (unexpected null cruise information returned)");
					UploadDashboard.showAutoCursor();
					return;
				}
				updateDatasetColumnSpecs(ddd);
				String status = ddd.getDataCheckStatus();
				if ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED) ||
					 status.startsWith(DashboardUtils.CHECK_STATUS_CRITICAL_ERRORS_PREFIX) ||
					 status.equals(DashboardUtils.CHECK_STATUS_UNACCEPTABLE) ) {
					// the sanity checker had serious problems
					UploadDashboard.theresAproblem(MessageInformationType.CRITICAL, 
					                               buildFailureMessage(SANITY_CHECK_FAIL_MSG, ddd), 
					                               "Show Errors / Warnings", "Dismiss", 
					                               new AsyncCallback<Boolean>() {
                        @Override
                        public void onFailure(Throwable arg0) {
                            ; // Doesn't happen
                        }
                        @Override
                        public void onSuccess(Boolean showErrors) {
                            if ( showErrors.booleanValue()) {
                                DataMessagesPage.showPage(getUsername(), cruise.getRecordId(), cruise.getUserDatasetName());
                            }
                        }
                    });
				}
				else if ( status.startsWith(DashboardUtils.CHECK_STATUS_ERRORS_PREFIX) ) {
					// errors issued
					UploadDashboard.theresAproblem(SANITY_CHECK_ERROR_MSG, "Show Errors / Warnings", "Dismiss", 
                           new AsyncCallback<Boolean>() {
                                @Override
                                public void onFailure(Throwable arg0) {
                                    ; // Doesn't happen
                                }
                                @Override
                                public void onSuccess(Boolean showErrors) {
                                    if ( showErrors.booleanValue()) {
                                        DataMessagesPage.showPage(getUsername(), cruise.getRecordId(), cruise.getUserDatasetName());
                                    }
                                }
                        });
        		}
        		else if ( status.startsWith(DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX) ) {
					// warnings issued
					UploadDashboard.theresAproblem(SANITY_CHECK_WARNING_MSG, "Show Errors / Warnings", "Dismiss", 
                           new AsyncCallback<Boolean>() {
                                @Override
                                public void onFailure(Throwable arg0) {
                                    ; // Doesn't happen
                                }
                                @Override
                                public void onSuccess(Boolean showErrors) {
                                    if ( showErrors.booleanValue()) {
                                        DataMessagesPage.showPage(getUsername(), cruise.getRecordId(), cruise.getUserDatasetName());
                                    }
                                }
                        });
				}
				else {
					// no problems
					UploadDashboard.showMessage(SANITY_CHECK_SUCCESS_MSG);
				}
				updateDatasetDataCheckMessages(tddp.getMsgList());
				updateDatasetUserQcFlagMessages(ddd);
				// Show the normal cursor
				UploadDashboard.showAutoCursor();
			}
		});
	}
    private static final int MAX_ERROR_LINES = 5;
	private String buildFailureMessage(String sanityCheckFailMsg, DashboardDatasetData ddd) {
        StringBuilder sb = new StringBuilder(sanityCheckFailMsg);
        int maxLines = MAX_ERROR_LINES;
        int more = 0;
        sb.append("<ul>");
        for (QCFlag qf : ddd.getCheckerFlags()) {
            if ( qf.getSeverity() == Severity.CRITICAL ) {
                if ( maxLines > 0 ) {
                    addMessageLine(sb, qf);
                    maxLines -= 1;
                } else {
                    more += 1;
                }
            }
        }
        if ( maxLines <= MAX_ERROR_LINES ) {
            for (QCFlag qf : ddd.getCheckerFlags()) {
                if ( qf.getSeverity() == Severity.ERROR ) {
                    if ( maxLines > 0 ) {
                        addMessageLine(sb, qf);
                        maxLines -= 1;
                    } else {
                        more += 1;
                    }
                }
            }
        }
        if ( more > 0 ) {
            sb.append("<li>Plus ").append(more).append(" additional error")
              .append(more > 1 ? "s" : "").append(".");
        }
        sb.append("</ul>");
        return sb.toString();
    }
    private static void addMessageLine(StringBuilder sb, QCFlag qf) {
        String comma = "";
        sb.append("<li>").append(qf.getSeverity().name()).append(": ")
          .append(qf.getComment());
        Integer row = qf.getRowIndex() != null ? qf.getRowIndex() : DashboardUtils.INT_MISSING_VALUE;
        Integer col = qf.getColumnIndex() != null ? qf.getColumnIndex() : DashboardUtils.INT_MISSING_VALUE;
        if ( ! row.equals(DashboardUtils.INT_MISSING_VALUE) || 
            ! col.equals(DashboardUtils.INT_MISSING_VALUE)) {
           sb.append(" at ");
           if ( ! row.equals(DashboardUtils.INT_MISSING_VALUE)) {
               sb.append("row: ").append(row.intValue()+1);
               comma = ", ";
           }
           if ( ! col.equals(DashboardUtils.INT_MISSING_VALUE)) {
               sb.append(comma).append("col: ").append(col);
           }
        }
    }
    
    @UiHandler("saveButton")
    void saveOnClick(ClickEvent event) {
    		if ( ! Boolean.TRUE.equals(cruise.isEditable()) ) {
			// Should never get here, but just in case
			UploadDashboard.showMessage(DISABLED_SUBMIT_HOVER_HELP);
			return;
		}
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
            UploadDashboard.pingService(new OAPAsyncCallback<Void>("session check") {
                @Override
                public void onSuccess(Void nothing) {
        			doSave();
                }
            });
		} else {
			UploadDashboard.showMessage("There have been no changes to data column definitions.");
		}
	}

	private void doSave() {
		// Show the wait cursor
		UploadDashboard.showWaitCursor();
		// Submit the updated data column types to the server.
		// This update invokes the SanityChecker on the data and
		// the results are then reported back to this page.
		service.saveDataColumnSpecs(getUsername(), cruise, 
				new OAPAsyncCallback<DashboardDatasetData>("save data column specification") {
			@Override
			public void onSuccess(DashboardDatasetData specs) {
				if ( specs == null ) {
					UploadDashboard.showMessage(SAVE_FAIL_MSG + " (unexpected null information returned)");
				} else {
					UploadDashboard.showMessage("Data column definitions saved.");
					updateDatasetColumnSpecs(specs);
				}
				UploadDashboard.showAutoCursor();
			}
		});
		
	}
}
