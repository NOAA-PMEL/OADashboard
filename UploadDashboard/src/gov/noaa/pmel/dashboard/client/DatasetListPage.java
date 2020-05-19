/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;

import gov.noaa.pmel.dashboard.client.DashboardAskPopup.QuestionType;
import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;

/**
 * Main upload dashboard page.  Shows uploaded cruise files
 * and their status.  Provides connections to upload data files,
 * describe the contents of these data files, and submit the data
 * for QC.
 * 
 * @author Karl Smith
 */
public class DatasetListPage extends CompositeWithUsername {

    private static Logger logger = Logger.getLogger(DatasetListPage.class.getName());
    
	private static enum SubmitFor {
		QC,
		ARCHIVE
	}
	
    private static final String[] EMPTY_MESSAGES = new String[0];
    
	private static final String TITLE_TEXT = "My Datasets";

	private static final String NEW_SUBMISSION_BUTTON_TEXT = "New Submission";
	private static final String NEW_SUBMISSION_BUTTON_HOVER_HELP = 
			"Create a new archive submission record.";
	private static final String UPDATE_SUBMISSION_BUTTON_TEXT = "Update Submission";
	private static final String UPDATE_SUBMISSION_BUTTON_HOVER_HELP = 
			"Update the selected submission.";

	private static final String VIEW_DATA_TEXT = "Identify Columns";
	private static final String VIEW_DATA_HOVER_HELP =
			"Review and modify data column type assignments for the " +
			"selected dataset;\nidentify issues in the data.";

	static final String METADATA_TEXT = "Manage Metadata";
	private static final String METADATA_HOVER_HELP =
			"Enter metadata for the selected datasets.";

	private static final String ADDL_DOCS_TEXT = "Supplemental Documents";
	private static final String ADDL_DOCS_HOVER_HELP =
			"Add supplemental documents for the selected datasets.";

//	private static final String QC_SUBMIT_TEXT = "Submit for QC";
//	private static final String QC_SUBMIT_HOVER_HELP =
//			"submit the selected datasets for quality control assessment";

	private static final String ARCHIVE_SUBMIT_TEXT = "Submit to Archive";
	private static final String ARCHIVE_SUBMIT_HOVER_HELP =
			"Submit dataset to permanent data archive.";

//	private static final String SUSPEND_TEXT = "Suspend Dataset";
//	private static final String SUSPEND_HOVER_HELP =
//			"suspend the selected datasets from quality control assessment to allow updates";

	private static final String REVIEW_TEXT = "Preview Dataset";
	private static final String REVIEW_HOVER_HELP =
			"Reveiw plots of selected data in the dataset.";

	private static final String SHOW_DATASETS_TEXT = 
			"Filter Datasets";
	private static final String SHOW_DATASETS_HOVER_HELP = 
			"Filter datasets displayed in your list.";

	private static final String HIDE_DATASETS_TEXT = 
			"Hide Datasets";
	private static final String HIDE_DATASETS_HOVER_HELP =
			"Hides the selected datasets from your list of displayed datasets; " +
			"this will NOT delete the datasets from the system.";

//	private static final String CHANGE_OWNER_TEXT =
//			"Change Datasets Owner";
//	private static final String CHANGE_OWNER_HOVER_HELP = 
//			"change the owner of the selected datasets to a dashboard user you specify";

	private static final String DELETE_TEXT = "Delete Datasets";
	private static final String DELETE_HOVER_HELP =
			"Delete the selected datasets from the system.";

	// Error message when the request for the latest cruise list fails
	private static final String GET_DATASET_LIST_ERROR_MSG = 
			"Problems obtaining the latest dataset listing.";

	// Starts of error messages for improper cruise selections
	private static final String SUBMITTED_DATASETS_SELECTED_ERR_START = 
			"Only datasets which have not been submitted for QC, " +
			"or which have been suspended or excluded, may be selected ";
	private static final String ARCHIVED_DATASETS_SELECTED_ERR_START =
			"Only datasets which have not been archived may be selected ";
	private static final String NO_DATASET_SELECTED_ERR_START = 
			"No dataset is selected ";
	private static final String MANY_DATASETS_SELECTED_ERR_START = 
			"Only one dataset may be selected ";

	// Ends of error messages for improper cruise selections
	private static final String FOR_REVIEWING_ERR_END = 
			"for reviewing data.";
	private static final String FOR_MD_ERR_END =
			"for managing metadata.";
	private static final String FOR_ADDL_DOCS_ERR_END = 
			"for managing supplemental documents.";
	private static final String FOR_PREVIEW_ERR_END = 
			"for dataset preview.";
	private static final String FOR_QC_SUBMIT_ERR_END =
			"for submitting for QC and archival.";
	private static final String FOR_SUSPEND_ERR_END =
			"for suspension.";
	private static final String FOR_DELETE_ERR_END = 
			"for deletion from the system.";
	private static final String FOR_HIDE_ERR_END = 
			"for hiding from your list of displayed datasets.";
	private static final String FOR_CHANGE_OWNER_ERR_END = 
			"for changing ownership";

	private static final String CANNOT_PREVIEW_UNCHECKED_ERRMSG =
			"Preview plots cannot be generated for datasets " +
			"with unidentified columns or unchecked data.";
	private static final String CANNOT_PREVIEW_WITH_SERIOUS_ERRORS_ERRMSG =
			"Preview plots cannot be generated for datasets " +
			"with longitude, latitude, or time errors.";

	private static final String NO_METADATA_HTML_PROLOGUE = 
			"The following datasets do not have appropriate metadata: <ul>";
	private static final String NO_METADATA_HTML_EPILOGUE = 
			"</ul> Appropriate metadata needs to be uploaded " +
			"for these datasets before submitting them for archival. ";
	private static final String CANNOT_SUBMIT_HTML_PROLOGUE = 
			"The following datasets have not been checked, or have very " +
			"serious errors detected by the automated data checker: <ul>";
	private static final String CANNOT_SUBMIT_HTML_EPILOGUE =
			"</ul> These datasets cannot be submitted for QC or archival " +
			"until these problems have been resolved.";
	private static final String DATA_AUTOFAIL_HTML_PROLOGUE = 
			"The following datasets have errors detected " +
			"by the automated data checker: <ul>";
	private static final String AUTOFAIL_HTML_EPILOGUE = 
			"</ul> These dataset can be submitted for QC and archival, " +
			"but datsets with a large number of error will <em>probably</em> " +
			"be suspended by reviewers.<br />" +
			"Do you want to continue? ";
	private static final String AUTOFAIL_YES_TEXT = "Yes";
	private static final String AUTOFAIL_NO_TEXT = "No";

	private static final String DATASETS_TO_SHOW_MSG = 
			"Enter the ID, possibly with wildcards * and ?, of the dataset(s) " +
			"you want to show in your list of displayed datasets";
	private static final String SHOW_DATASET_FAIL_MSG = 
			"Unable to show the specified datasets " +
			"in your personal list of displayed datasets";

	private static final String HIDE_DATASET_HTML_PROLOGUE = 
			"The following datasets will be hidden from your " +
			"list of displayed datasets;<br/>The data, metadata, and " +
			"supplemental documents will <b>not</b> be removed: <ul>";
	private static final String HIDE_DATASET_HTML_EPILOGUE = 
			"</ul> Do you want to proceed?";
	private static final String HIDE_YES_TEXT = "Yes";
	private static final String HIDE_NO_TEXT = "No";
	private static final String HIDE_DATASET_FAIL_MSG = 
			"Unable to hide the selected datasets from " +
			"your list of displayed datasets";

	private static final String CHANGE_OWNER_HTML_PROLOGUE = 
			"The owner of the following datasets will be " +
			"changed to the new owner you specify below: <ul>";
	private static final String CHANGE_OWNER_HTML_EPILOGUE = 
			"</ul>";
	private static final String CHANGE_OWNER_INPUT_TEXT = "New Owner:";
	private static final String CHANGE_OWNER_YES_TEXT = "Proceed";
	private static final String CHANGE_OWNER_NO_TEXT = "Cancel";
	private static final String CHANGE_OWNER_FAIL_MSG = 
			"An error occurred when changing ownership of these datasets";

	private static final String DELETE_DATASET_HTML_PROLOGUE = 
			"All data will be deleted for the following datasets: <ul>";
	private static final String DELETE_DATASET_HTML_EPILOGUE =
			"</ul> Do you want to proceed?";
	private static final String DELETE_YES_TEXT = "Yes";
	private static final String DELETE_NO_TEXT = "No";
	private static final String DELETE_DATASET_FAIL_MSG = 
			"Unable to delete the datasets";

	private static final String UNEXPECTED_INVALID_DATESET_LIST_MSG = 
			" (unexpected invalid datasets list returned)";

	private static final String SELECT_TO_ENABLE_MSG = 
			"** SELECT A DATASET TO ENABLE BUTTON **\n";
	private static final String ONLY_ONE_TO_ENABLE_MSG = 
			"** SELECT ONLY ONE DATASET TO ENABLE BUTTON **\n";
	private static final String NOT_FOR_OTHER = 
			"** THIS OPTION IS NOT AVAILABLE FOR OTHER-type DATASETS **\n";
	
	// Select options
	private static final String SELECTION_OPTION_LABEL = "Select...";
	private static final String ALL_SELECTION_OPTION = "All";
//	private static final String EDITABLE_SELECTION_OPTION = "Editable";
//	private static final String SUBMITTED_SELECTION_OPTION = "Submitted";
//	private static final String PUBLISHED_SELECTION_OPTION = "Published";
	private static final String ARCHIVED_SELECTION_OPTION = "Archived";
	private static final String CLEAR_SELECTION_OPTION = "None";

	// Column header strings
	private static final String DATASET_ID_COLUMN_NAME = "Dataset ID";
	private static final String FEATURE_TYPE_COLUMN_NAME = "Observation Type";
	private static final String TIMESTAMP_COLUMN_NAME = "Upload Date";
	private static final String DATA_CHECK_COLUMN_NAME = "Data Status";
	private static final String METADATA_COLUMN_NAME = "Metadata";
	private static final String ADDL_DOCS_COLUMN_NAME = "Supplemental<br />Documents";
	private static final String VERSION_COLUMN_NAME = "Version";
	private static final String SUBMITTED_COLUMN_NAME = "QC Status";
	private static final String ARCHIVED_COLUMN_NAME = "Archival";
	private static final String FILENAME_COLUMN_NAME = "File Name";
	private static final String DATASET_NAME_COLUMN_NAME = "Dataset Name";
	private static final String OWNER_COLUMN_NAME = "Owner";

	// Replacement strings for empty or null values
	private static final String EMPTY_TABLE_TEXT = "(no uploaded datasets)";
	private static final String NO_DATASET_ID_STRING = "(unknown)";
	private static final String NO_TIMESTAMP_STRING = "(unknown)";
	private static final String STATUS_CANNOT_CHECK_STRING = "Cannot check";
	private static final String NO_DATA_CHECK_STATUS_STRING = "Not checked";
	private static final String NO_METADATA_STATUS_STRING = "(no metadata)";
	private static final String NO_QC_STATUS_STRING = "Private";
	private static final String NO_ARCHIVE_STATUS_STRING = "Not specified";
	private static final String NO_UPLOAD_FILENAME_STRING = "(unknown)";
	private static final String NO_ADDL_DOCS_STATUS_STRING = "(no documents)";
	private static final String NO_OWNER_STRING = "(unknown)";

	interface DatasetListPageUiBinder extends UiBinder<Widget, DatasetListPage> {
	}

	private static DatasetListPageUiBinder uiBinder = 
			GWT.create(DatasetListPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

    @UiField ApplicationHeaderTemplate header;
    
//	@UiField Label titleLabel;
//	@UiField InlineLabel userInfoLabel;
//	@UiField Button logoutButton;
	
	@UiField Button newSubmissionButton;
	@UiField Button viewDataAndColumnsButton;
	@UiField Button metadataButton;
	@UiField Button addlDocsButton;
	@UiField Button previewButton;
//	@UiField Button qcSubmitButton;
	@UiField Button archiveSubmitButton;
//	@UiField Button suspendDatasetButton;
//	@UiField Label firstSeparator;
	@UiField Button showDatasetButton;
	@UiField Button hideDatasetButton;
//	@UiField Button changeOwnerButton;
	@UiField Label secondSeparator;
	@UiField Button deleteButton;
	@UiField DataGrid<DashboardDataset> datasetsGrid;

	private Button[] selectSet;
	private Button[] singleSet;
	private Button[] noOpaque;
	
	private Header<String> selectHeader;
	
	private ListDataProvider<DashboardDataset> listProvider;
	private DashboardAskPopup askDeletePopup;
	private DashboardAskPopup askRemovePopup;
//	private DashboardInputPopup changeOwnerPopup;
	private DashboardDatasetList selectedDatasets;
	private DashboardAskPopup askDataAutofailPopup;
	// private boolean managerButtonsShown;
    private Column<DashboardDataset,Boolean> selectedColumn;
	private TextColumn<DashboardDataset> timestampColumn;
	private TextColumn<DashboardDataset> recordIdColumn;
    private TextColumn<DashboardDataset> filenameColumn;

    private boolean doUpdateSubmission = false;
    
	// The singleton instance of this page
	private static DatasetListPage singleton;

    private int clickRowIdx = -1;
    private DashboardDataset clickedDataset;
    
	private class SelectionHandler implements CellPreviewEvent.Handler<DashboardDataset> {
        @Override
        public void onCellPreview(CellPreviewEvent<DashboardDataset> event) {
            clickRowIdx = event.getIndex();
            clickedDataset = event.getValue();
        }
	}
    private SelectionHandler selectionHandler = new SelectionHandler();
    
    private void setSelection(DoubleClickEvent event) {
        GWT.log(event.getNativeEvent().getType());
        if ( clickRowIdx >= 0 && clickRowIdx < 50 
             && clickedDataset != null ) {
            GWT.log(String.valueOf(clickedDataset));
            clickedDataset.setSelected(! clickedDataset.isSelected());
            datasetsGrid.redrawRow(clickRowIdx);
            updateAvailableButtons();
        } else {
            Window.alert("bad click : " + clickRowIdx + ":" + clickedDataset);
        }
    }
    
	/**
	 * Creates an empty dataset list page.  Do not call this 
	 * constructor; instead use the showPage static method 
	 * to show the singleton instance of this page with the
	 * latest dataset list from the server. 
	 */
	DatasetListPage(DashboardDatasetList datasets) {
        super(PagesEnum.SHOW_DATASETS.name());
		initWidget(uiBinder.createAndBindUi(this));
        boolean showManagerFields = datasets.isManager();
		singleton = this;

		buildDatasetListTable(showManagerFields);

		buildSelectionSets();
		
        datasetsGrid.addCellPreviewHandler(selectionHandler);
        datasetsGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                setSelection(event);
            }
        }, DoubleClickEvent.getType());
        
        header.setPageTitle(TITLE_TEXT);

		newSubmissionButton.setText(NEW_SUBMISSION_BUTTON_TEXT);
		newSubmissionButton.setTitle(NEW_SUBMISSION_BUTTON_HOVER_HELP);

		viewDataAndColumnsButton.setText(VIEW_DATA_TEXT);
		viewDataAndColumnsButton.setTitle(VIEW_DATA_HOVER_HELP);

		metadataButton.setText(METADATA_TEXT);
		metadataButton.setTitle(METADATA_HOVER_HELP);

		addlDocsButton.setText(ADDL_DOCS_TEXT);
		addlDocsButton.setTitle(ADDL_DOCS_HOVER_HELP);

		previewButton.setText(REVIEW_TEXT);
		previewButton.setTitle(REVIEW_HOVER_HELP);

//		qcSubmitButton.setText(QC_SUBMIT_TEXT);
//		qcSubmitButton.setTitle(QC_SUBMIT_HOVER_HELP);

		archiveSubmitButton.setText(ARCHIVE_SUBMIT_TEXT);
		archiveSubmitButton.setTitle(ARCHIVE_SUBMIT_HOVER_HELP);

		showDatasetButton.setText(SHOW_DATASETS_TEXT);
		showDatasetButton.setTitle(SHOW_DATASETS_HOVER_HELP);
        showDatasetButton.setVisible(showManagerFields);

		hideDatasetButton.setText(HIDE_DATASETS_TEXT);
		hideDatasetButton.setTitle(HIDE_DATASETS_HOVER_HELP);
        hideDatasetButton.setVisible(showManagerFields);

        secondSeparator.setVisible(showManagerFields);
        
		deleteButton.setText(DELETE_TEXT);
		deleteButton.setTitle(DELETE_HOVER_HELP);

		// managerButtonsShown = true;
		askDeletePopup = null;
		askRemovePopup = null;
		askDataAutofailPopup = null;
		newSubmissionButton.setFocus(true);
	}

	private void buildSelectionSets() {
		selectSet = new Button[] {
			viewDataAndColumnsButton,
			metadataButton,
			addlDocsButton,
			previewButton,
//			qcSubmitButton,
			archiveSubmitButton,
//			suspendDatasetButton,
			hideDatasetButton,
//			changeOwnerButton,
			deleteButton
		};
		singleSet = new Button[] {
			viewDataAndColumnsButton,
			metadataButton,
//			addlDocsButton,
			previewButton,
//			qcSubmitButton,
			archiveSubmitButton,
//			suspendDatasetButton,
//			hideDatasetButton,
//			changeOwnerButton,
//			deleteButton
		};
        noOpaque = new Button[] {
			viewDataAndColumnsButton,
			previewButton
        };
	}

	/**
	 * Display the dataset list page in the RootLayoutPanel 
	 * with the latest information from the server.
	 * Adds this page to the page history.
	 */
	static void showPage() {
		UploadDashboard.showWaitCursor();
		// Request the latest cruise list
		service.getDatasetList(null, new OAPAsyncCallback<DashboardDatasetList>() {
			@Override
			public void onSuccess(DashboardDatasetList cruises) {
				if ( singleton == null )
					singleton = new DatasetListPage(cruises);
				UploadDashboard.updateCurrentPage(singleton);
				singleton.updateDatasets(cruises);
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void customFailure(Throwable ex) {
				String exMsg = ex.getMessage();
				if ( exMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) {
					UploadDashboard.showMessage("Your session has expired.<br/><br/>Please log in again.");
				} else {
					UploadDashboard.showFailureMessage(GET_DATASET_LIST_ERROR_MSG, ex);
					UploadDashboard.showAutoCursor();
				}
			}
		});
        if ( meLink != null ) {
            meLink.setAttribute("style", "cursor:pointer;");
            meLink = null;
        }
	}

	/**
	 * Redisplays the last version of this page if the username
	 * associated with this page matches the given username.
	 */
	static void redisplayPage(String username) {
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) )
			DatasetListPage.showPage();
		else
			UploadDashboard.updateCurrentPage(singleton);
	}

//	/**
//	 * Add a dataset to the selected list when the page is displayed.
//	 * This should be called just prior to calling showPage().  
//	 * If no dataset with the given ID exists in the updated 
//	 * list of datasets, this ID will be ignored. 
//	 * 
//	 * @param dataset
//	 * 		select the cruise with this dataset
//	 */
//	static void addSelectedDataset(String datasetId) {
//		if ( singleton == null )
//			singleton = new DatasetListPage();
////		singleton.datasetIdsSet.add(datasetId);
//	}

	/**
	 * Resorts the cruise list table first by upload timestamp 
	 * in descending order, then by dataset in ascending order.
	 */
	static void resortTable() {
		if ( singleton == null ) // do nothing
            return;
		ColumnSortList sortList = singleton.datasetsGrid.getColumnSortList();
		sortList.push(new ColumnSortInfo(singleton.filenameColumn, true));
		sortList.push(new ColumnSortInfo(singleton.timestampColumn, false));
		ColumnSortEvent.fire(singleton.datasetsGrid, sortList);
	}

	/**
	 * Updates the dataset list page with the current username and 
	 * with the datasets given in the argument.
	 * 
	 * @param newList
	 * 		datasets to display
	 */
	private void updateDatasets(DashboardDatasetList newList) {
        if ( newList == null ) { // PANIC
            UploadDashboard.logToConsole("NULL dataset list to DatasetListPage!");
            newList = new DashboardDatasetList(getUsername());
        }
		// Update the username
		setUsername(newList.getUsername());
        if ( selectedDatasets == null || ! selectedDatasets.getUsername().equals(newList.getUsername())) {
            selectedDatasets = new DashboardDatasetList(newList.getUsername());
        }
		header.userInfoLabel.setText(WELCOME_INTRO + getUsername());
		// Update the cruises shown by resetting the data in the data provider
		List<DashboardDataset> providerList = listProvider.getList();
		providerList.clear();
		providerList.addAll(newList.values());
		for ( DashboardDataset dataset : providerList ) {
			if ( selectedDatasets.containsKey(dataset.getDatasetId()) )
				dataset.setSelected(true);
			else
				dataset.setSelected(false);
		}
		updateAvailableButtons();
		datasetsGrid.setRowCount(providerList.size());
		datasetsGrid.setVisibleRange(0, providerList.size());
		// Make sure the table is sorted according to the last specification
		ColumnSortEvent.fire(datasetsGrid, datasetsGrid.getColumnSortList());
	}

	/**
	 * Selects dataset of the given type in the dataset list, supplementing the currently
	 * selected datasets, or clears all selected datasets.
	 * 
	 * @param option
	 * 		string indicating the cruise types to select;  one of: <br />
	 * 		SELECTION_OPTION_LABEL - does nothing; <br />
	 * 		ALL_SELECTION_OPTION - selects all cruises; <br />
	 * 		EDITABLE_SELECTION_OPTION - selects editable cruises; <br />
	 * 		SUBMITTED_SELECTION_OPTION - selects submitted cruises; <br />
	 * 		PUBLISHED_SELECTION_OPTION - selects published cruises; or <br />
	 * 		CLEAR_SELECTION_OPTION - clears all selected cruises.
	 */
	private void setDatasetSelection(String option) {
		// Do nothing is SELECTION_OPTION_LABEL is given
		if ( SELECTION_OPTION_LABEL.equals(option) )
			return;
		// Modify the dataset selection
		List<DashboardDataset> providerList = listProvider.getList();
		if ( ALL_SELECTION_OPTION.equals(option) ) {
			for ( DashboardDataset dataset : providerList )
				dataset.setSelected(true);
		}
//		else if ( EDITABLE_SELECTION_OPTION.equals(option) ) {
//			for ( DashboardDataset dataset : providerList ) {
//				if ( Boolean.TRUE.equals(dataset.isEditable()) )
//					dataset.setSelected(true);
//				else
//					dataset.setSelected(false);
//			}
//		}
//		else if ( SUBMITTED_SELECTION_OPTION.equals(option) ) {
//			for ( DashboardDataset dataset : providerList ) {
//				if ( Boolean.FALSE.equals(dataset.isEditable()) )
//					dataset.setSelected(true);
//				else
//					dataset.setSelected(false);					
//			}
//		}
//		else if ( PUBLISHED_SELECTION_OPTION.equals(option) ) {
		else if ( ARCHIVED_SELECTION_OPTION.equals(option) ) {
			for ( DashboardDataset dataset : providerList )
				dataset.setSelected(dataset.isArchived());					
		}
		else if ( CLEAR_SELECTION_OPTION.equals(option) ) {
			for ( DashboardDataset dataset : providerList )
				dataset.setSelected(false);
		}
		else {
			throw new RuntimeException("Unexpected option given the setDatasetSelection: " + option);
		}
		updateAvailableButtons();
		Object key = selectHeader.getKey();
		((SelectionCell)selectHeader.getCell()).setViewData(key, SELECTION_OPTION_LABEL);
		datasetsGrid.setRowCount(providerList.size());
		datasetsGrid.setVisibleRange(0, providerList.size());
		// Make sure the table is sorted according to the last specification
		ColumnSortEvent.fire(datasetsGrid, datasetsGrid.getColumnSortList());
	}

	/**
	 * Assigns datasetsSet with the selected cruises, and 
	 * datasetIdsSet with the expocodes of these cruises. 
	 *  
	 * @param onlyEditable
	 * 		if true, fails if a submitted or published cruise is selected;
	 * 		if false, fails if a published cruise is selected;
	 * 		if null, always succeeds.
	 * @return
	 * 		if successful
	 */
	private boolean getSelectedDatasets(Boolean onlyEditable) {
		selectedDatasets.clear();
		for ( DashboardDataset dataset : listProvider.getList() ) {
			if ( dataset.isSelected() ) {
				if ( onlyEditable != null ) {
					Boolean editable = dataset.isEditable();
					// check if from a previous version
					if ( editable == null )
						return false;
					// check if editable, if requested
					if ( onlyEditable && ! editable )
						return false;
				}
				String expocode = dataset.getDatasetId();
				selectedDatasets.put(expocode, dataset);
			}
		}
		return true;
	}
    
    private DashboardDatasetList getSelectedDatasets() {
		selectedDatasets.clear();
		for ( DashboardDataset dataset : listProvider.getList() ) {
			if ( dataset.isSelected() ) {
				String expocode = dataset.getDatasetId();
				selectedDatasets.put(expocode, dataset);
			}
		}
        return selectedDatasets;
    }
    
	/**
     * @param dd
     * @return
     */
    private static boolean hasNoUnknownColumns(DashboardDataset dd) {
        for (DataColumnType col : dd.getDataColTypes()) {
            if ( col.typeNameEquals(DashboardUtils.UNKNOWN)) {
                return false;
            }
        }
        return true;
    }

    @UiHandler("newSubmissionButton")
	void uploadDatasetOnClick(ClickEvent event) {
		// Save the IDs of the currently selected datasets
		getSelectedDatasets(null);
        if ( doUpdateSubmission ) {
            if ( selectedDatasets.size() == 0 ) {
                Window.alert("No dataasets are selected!");
                updateAvailableButtons();
                doUpdateSubmission = false;
                return;
            }
            if ( selectedDatasets.size() > 1 ) {
                Window.alert("You can only update 1 submission at a time.");
                updateAvailableButtons();
                doUpdateSubmission = false;
                return;
            }
            DashboardDataset selectedDataset = selectedDatasets.values().iterator().next();
            UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
                @Override
                public void onSuccess(Void arg0) {
                    UploadDashboard.showUpdateSubmissionDialog(getUsername(), selectedDataset);
                }
            });
        } else {
    		// Go to the dataset upload page
    		DataUploadPage.showPage(getUsername());
        }
	}

	@UiHandler("viewDataAndColumnsButton")
	void dataCheckOnClick(ClickEvent event) {
		if ( ! getSelectedDatasets(true) ) {
			UploadDashboard.showMessage(
					SUBMITTED_DATASETS_SELECTED_ERR_START + FOR_REVIEWING_ERR_END);
			return;
		}
		if ( selectedDatasets.size() < 1 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_REVIEWING_ERR_END);
			return;
		}
		DataColumnSpecsPage.showPage(getUsername(), selectedDatasets);
	}

	@UiHandler("metadataButton")
	void metadataOnClick(ClickEvent event) {
		getSelectedDatasets(null);
		if ( selectedDatasets.size() < 1 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_MD_ERR_END);
			return;
		}
		// Until the MD is in place, only accept one cruise
		if ( selectedDatasets.size() > 1 ) {
			UploadDashboard.showMessage(
					MANY_DATASETS_SELECTED_ERR_START + FOR_MD_ERR_END);
			return;
		}
		MetadataManagerPage.showPage(selectedDatasets);
	}

	@UiHandler("addlDocsButton")
	void addlDocsOnClick(ClickEvent event) {
		getSelectedDatasets(null);
		if ( selectedDatasets.size() < 1 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_ADDL_DOCS_ERR_END);
			return;
		}
		AddlDocsManagerPage.showPage(selectedDatasets);
	}

	@UiHandler("previewButton")
	void previewButtonOnClick(ClickEvent event) {
	    UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful ping.");
                _previewButtonOnClick(event);
            }
            @Override
            public void customFailure(Throwable t) {
                GWT.log("ping fail: "+ t);
            }
        });
	}
	void _previewButtonOnClick(ClickEvent event) {
		getSelectedDatasets(null);
		if ( selectedDatasets.size() < 1 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_PREVIEW_ERR_END);
			return;
		}
		if ( selectedDatasets.size() > 1 ) {
			UploadDashboard.showMessage(
					MANY_DATASETS_SELECTED_ERR_START + FOR_PREVIEW_ERR_END);
			return;
		}
		for ( DashboardDataset dataset : selectedDatasets.values() ) {
			String status = dataset.getDataCheckStatus();
			if ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED) ) {
				UploadDashboard.showMessage(CANNOT_PREVIEW_UNCHECKED_ERRMSG);
				return;
			}
			else if ( status.contains(DashboardUtils.GEOPOSITION_ERRORS_MSG) ) {
				UploadDashboard.showMessage(CANNOT_PREVIEW_WITH_SERIOUS_ERRORS_ERRMSG);
				return;				
			}
		}
		DatasetPreviewPage.showPage(selectedDatasets);
		return;
	}

//	@UiHandler("qcSubmitButton")
//	void qcSubmitOnClick(ClickEvent event) {
//		if ( ! getSelectedDatasets(false) ) {
//			UploadDashboard.showMessage(
//					ARCHIVED_DATASETS_SELECTED_ERR_START + FOR_QC_SUBMIT_ERR_END);
//			return;
//		}
//		if ( datasetsSet.size() == 0 ) {
//			UploadDashboard.showMessage(
//					NO_DATASET_SELECTED_ERR_START + FOR_QC_SUBMIT_ERR_END);
//			return;
//		}
//		checkSet.clear();
//		checkSet.putAll(datasetsSet);
//		checkSet.setUsername(getUsername());
//		checkDatasetsForSubmitting(SubmitFor.QC);
//	}

	@UiHandler("archiveSubmitButton")
	void archiveSubmitOnClick(ClickEvent event) {
        GWT.log("arhiveSubmitButton pressed.");
        preLaunchArchiveSubmit(getSelectedDatasets());
	}
	void preLaunchArchiveSubmit(DashboardDatasetList datasets) {
	    UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful ping.");
                _archiveSubmitOnClick(datasets);
            }
            @Override
            public void customFailure(Throwable t) {
                GWT.log("ping fail: "+ t);
            }
        });
	}
	void _archiveSubmitOnClick(DashboardDatasetList datasets) {
		if ( checkDatasetsForSubmitting(datasets, SubmitFor.ARCHIVE)) {
		    submitDatasets(selectedDatasets, SubmitFor.ARCHIVE);
		}
	}

//	@UiHandler("suspendDatasetButton")
//	void suspendDatasetOnClick(ClickEvent event) {
//		if ( ! getSelectedDatasets(false) ) {
//			UploadDashboard.showMessage(
//					ARCHIVED_DATASETS_SELECTED_ERR_START + FOR_SUSPEND_ERR_END);
//			return;
//		}
//		if ( datasetsSet.size() == 0 ) {
//			UploadDashboard.showMessage(
//					NO_DATASET_SELECTED_ERR_START + FOR_SUSPEND_ERR_END);
//			return;
//		}
//		checkSet.clear();
//		checkSet.putAll(datasetsSet);
//		checkSet.setUsername(getUsername());
//		checkDatasetsForSuspension();
//		suspendDatasets();
//	}
	
	@UiHandler("deleteButton")
	void deleteDatasetOnClick(ClickEvent event) {
	    UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful ping.");
                _deleteDatasetOnClick(event);
            }
            @Override
            public void customFailure(Throwable t) {
                GWT.log("ping fail: "+ t);
            }
        });
	}
	void _deleteDatasetOnClick(ClickEvent event) {
		if ( ! getSelectedDatasets(true) ) {
			UploadDashboard.showMessage(
					SUBMITTED_DATASETS_SELECTED_ERR_START + FOR_DELETE_ERR_END);
			return;
		}
		if ( selectedDatasets.size() == 0 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_DELETE_ERR_END);
			return;
		}
		// Confirm cruises to be deleted
		String message = DELETE_DATASET_HTML_PROLOGUE;
		for ( DashboardDataset dataset : selectedDatasets.values()) {
			message += "<li>" + SafeHtmlUtils.htmlEscape(dataset.getUserDatasetName()) + "</li>";
		}
		message += DELETE_DATASET_HTML_EPILOGUE;
		if ( askDeletePopup == null ) {
			askDeletePopup = new DashboardAskPopup(DELETE_YES_TEXT, 
					DELETE_NO_TEXT, QuestionType.WARNING,
					new AsyncCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean okay) {
					// Only proceed only if yes button was selected
					if ( okay ) {
						// never delete the metadata or supplemental documents
                        // why? ? now deleting.
						continueDeleteDatasets(true);
					}
				}
				@Override
				public void onFailure(Throwable ex) {
					// Never called
					;
				}
			});
		}
		askDeletePopup.askQuestion(message);
	}

	/**
	 * Makes the request to delete the currently selected cruises,
	 * and processes the results.
	 */
	private void continueDeleteDatasets(Boolean deleteMetadata) {
		UploadDashboard.showWaitCursor();
		service.deleteDatasets(getUsername(), new TreeSet<String>(selectedDatasets.keySet()), deleteMetadata, 
				new OAPAsyncCallback<DashboardDatasetList>() {
			@Override
			public void onSuccess(DashboardDatasetList datasetList) {
				if ( getUsername().equals(datasetList.getUsername()) ) {
					DatasetListPage.this.updateDatasets(datasetList);
				}
				else {
					UploadDashboard.showMessage(DELETE_DATASET_FAIL_MSG + 
							UNEXPECTED_INVALID_DATESET_LIST_MSG);
				}
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void customFailure(Throwable ex) {
                ex.printStackTrace();
				UploadDashboard.showFailureMessage(DELETE_DATASET_FAIL_MSG, ex);
				UploadDashboard.showAutoCursor();
			}
		});
	}

	@UiHandler("showDatasetButton")
	void addToListOnClick(ClickEvent event) {
	    UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful ping.");
                _addToListOnClick(event);
            }
            @Override
            public void customFailure(Throwable t) {
                GWT.log("ping fail: "+ t);
            }
        });
	}
	void _addToListOnClick(ClickEvent event) {
		String wildDatasetId = Window.prompt(DATASETS_TO_SHOW_MSG, "");  // UploadDashboard.getResponse(DATASETS_TO_SHOW_MSG, "");
		if ( (wildDatasetId != null) && ! wildDatasetId.trim().isEmpty() ) {
			UploadDashboard.showWaitCursor();
			// Save the currently selected cruises
			getSelectedDatasets(null);
			service.filterDatasetsToList(getUsername(), wildDatasetId, 
					new OAPAsyncCallback<DashboardDatasetList>() {
				@Override
				public void onSuccess(DashboardDatasetList cruises) {
					if ( getUsername().equals(cruises.getUsername()) ) {
						DatasetListPage.this.updateDatasets(cruises);
					}
					else {
						UploadDashboard.showMessage(SHOW_DATASET_FAIL_MSG + 
								UNEXPECTED_INVALID_DATESET_LIST_MSG);
					}
					UploadDashboard.showAutoCursor();
				}
				@Override
				public void customFailure(Throwable ex) {
					UploadDashboard.showFailureMessage(SHOW_DATASET_FAIL_MSG, ex);
					UploadDashboard.showAutoCursor();
				}
			});
		}
	}

	@UiHandler("hideDatasetButton")
	void removeFromListOnClick(ClickEvent event) {
	    UploadDashboard.pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful ping.");
                _removeFromListOnClick(event);
            }
            @Override
            public void customFailure(Throwable t) {
                GWT.log("ping fail: "+ t);
            }
        });
	}
	void _removeFromListOnClick(ClickEvent event) {
		getSelectedDatasets(null);
		if ( selectedDatasets.size() == 0 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_HIDE_ERR_END);
			return;
		}
		// Confirm cruises to be removed
		String message = HIDE_DATASET_HTML_PROLOGUE;
		for ( DashboardDataset dataset : selectedDatasets.values()) {
			message += "<li>" + SafeHtmlUtils.htmlEscape(dataset.getUserDatasetName()) + "</li>";
		}
		message += HIDE_DATASET_HTML_EPILOGUE;
		if ( askRemovePopup == null ) {
			askRemovePopup = new DashboardAskPopup(HIDE_YES_TEXT, 
					HIDE_NO_TEXT, DashboardAskPopup.QuestionType.QUESTION,
					new AsyncCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean result) {
					// Only proceed if yes; ignore if no or null
                    // Where's the null check?
					if ( result == true )
						continueRemoveDatasetsFromList();
				}
				@Override
				public void onFailure(Throwable ex) {
					// Never called
					;
				}
			});
		}
		askRemovePopup.askQuestion(message);
	}

	/**
	 * Makes the request to remove cruises from a user's list,
	 * and processes the results.
	 */
	private void continueRemoveDatasetsFromList() {
		UploadDashboard.showWaitCursor();
		service.removeDatasetsFromList(getUsername(), new TreeSet<String>(selectedDatasets.keySet()), 
				new OAPAsyncCallback<DashboardDatasetList>() {
			@Override
			public void onSuccess(DashboardDatasetList cruises) {
				if ( getUsername().equals(cruises.getUsername()) ) {
					DatasetListPage.this.updateDatasets(cruises);
				}
				else {
					UploadDashboard.showMessage(HIDE_DATASET_FAIL_MSG + 
							UNEXPECTED_INVALID_DATESET_LIST_MSG);
				}
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void customFailure(Throwable ex) {
                ex.printStackTrace();
				UploadDashboard.showFailureMessage(HIDE_DATASET_FAIL_MSG, ex);
				UploadDashboard.showAutoCursor();
			}
		});
	}

//	@UiHandler("changeOwnerButton")
//  XXX This should really come out (along with continueChangeOwner)
//  but commenting it out causes an exception to be thrown at runtime in debug mode
//  at the first line of buildArchiveStatusColumn with error: ReferenceError: CYh_g$ is not defined
//  Standard GWT-compiled war runs fine in tomcat...
	void changeOwnerOnClick(ClickEvent event) {
//		getSelectedDatasets(null);
//		if ( selectedDatasets.size() == 0 ) {
//			UploadDashboard.showMessage(
//					NO_DATASET_SELECTED_ERR_START + FOR_CHANGE_OWNER_ERR_END);
//			return;
//		}
//		// Confirm cruises to be removed
//		String message = CHANGE_OWNER_HTML_PROLOGUE;
//		for ( String expocode : selectedDatasets.keySet() )
//			message += "<li>" + SafeHtmlUtils.htmlEscape(expocode) + "</li>";
//		message += CHANGE_OWNER_HTML_EPILOGUE;
//		if ( changeOwnerPopup == null ) {
//			changeOwnerPopup = new DashboardInputPopup(CHANGE_OWNER_INPUT_TEXT, 
//					CHANGE_OWNER_YES_TEXT, CHANGE_OWNER_NO_TEXT, new AsyncCallback<String>() {
//				@Override
//				public void onSuccess(String newOwner) {
//					if ( newOwner != null ) {
//						continueChangeOwner(newOwner);
//					}
//				}
//				@Override
//				public void onFailure(Throwable ex) {
//					// Never called
//					;
//				}
//			});
//		}
//		changeOwnerPopup.askForInput(message);
	}

//	/**
//	 * Makes the request to remove cruises from a user's list,
//	 * and processes the results.
//	 */
//	private void continueChangeOwner(String newOwner) {
//		UploadDashboard.showWaitCursor();
//		service.changeDatasetOwner(getUsername(), new TreeSet<String>(selectedDatasets.keySet()), newOwner, 
//				new OAPAsyncCallback<DashboardDatasetList>() {
//			@Override
//			public void onSuccess(DashboardDatasetList cruises) {
//				if ( getUsername().equals(cruises.getUsername()) ) {
//					DatasetListPage.this.updateDatasets(cruises);
//				}
//				else {
//					UploadDashboard.showMessage(CHANGE_OWNER_FAIL_MSG + 
//							UNEXPECTED_INVALID_DATESET_LIST_MSG);
//				}
//				UploadDashboard.showAutoCursor();
//			}
//			@Override
//			public void customFailure(Throwable ex) {
//				UploadDashboard.showFailureMessage(CHANGE_OWNER_FAIL_MSG, ex);
//				UploadDashboard.showAutoCursor();
//			}
//		});
//	}

	/**
	 * Creates the cruise data table columns.  The table will still need 
	 * to be populated using {@link #updateDatasets(DashboardDatasetList)}.
	 * @param showManagerColumns 
	 */
	private void buildDatasetListTable(boolean showManagerColumns) {
		selectHeader = buildSelectionHeader();

		// Create the columns for this table
		TextColumn<DashboardDataset> rowNumColumn = buildRowNumColumn();
		selectedColumn = buildSelectedColumn();
		recordIdColumn = buildDatasetIdColumn();
		filenameColumn = buildFilenameColumn();
		timestampColumn = buildTimestampColumn();
		Column<DashboardDataset,String> featureTypeColumn = buildFeatureTypeColumn();
		Column<DashboardDataset,String> dataCheckColumn = buildDataCheckColumn();
		Column<DashboardDataset,String> metadataColumn = buildMetadataColumn();
		Column<DashboardDataset,String> addlDocsColumn = buildAddnDocsColumn();
//		TextColumn<DashboardDataset> versionColumn = buildVersionColumn();
//		Column<DashboardDataset,String> qcStatusColumn = buildQCStatusColumn();
		Column<DashboardDataset,String> archiveStatusColumn = buildArchiveStatusColumn();
		TextColumn<DashboardDataset> ownerColumn = buildOwnerColumn();

		// Add the columns, with headers, to the table
		datasetsGrid.addColumn(rowNumColumn, selectHeader);
		datasetsGrid.addColumn(selectedColumn, selectHeader);
		datasetsGrid.addColumn(filenameColumn, 
				SafeHtmlUtils.fromSafeConstant(FILENAME_COLUMN_NAME));
		datasetsGrid.addColumn(featureTypeColumn, 
				SafeHtmlUtils.fromSafeConstant(FEATURE_TYPE_COLUMN_NAME));
		datasetsGrid.addColumn(timestampColumn, 
				SafeHtmlUtils.fromSafeConstant(TIMESTAMP_COLUMN_NAME));
		datasetsGrid.addColumn(dataCheckColumn, 
				SafeHtmlUtils.fromSafeConstant(DATA_CHECK_COLUMN_NAME));
		datasetsGrid.addColumn(metadataColumn, 
				SafeHtmlUtils.fromSafeConstant(METADATA_COLUMN_NAME));
		datasetsGrid.addColumn(addlDocsColumn, 
				SafeHtmlUtils.fromSafeConstant(ADDL_DOCS_COLUMN_NAME));
//		datasetsGrid.addColumn(versionColumn,
//				SafeHtmlUtils.fromSafeConstant(VERSION_COLUMN_NAME));
//		datasetsGrid.addColumn(qcStatusColumn, 
//				SafeHtmlUtils.fromSafeConstant(SUBMITTED_COLUMN_NAME));
		datasetsGrid.addColumn(archiveStatusColumn, 
				SafeHtmlUtils.fromSafeConstant(ARCHIVED_COLUMN_NAME));
		datasetsGrid.addColumn(recordIdColumn, 
				SafeHtmlUtils.fromSafeConstant(DATASET_ID_COLUMN_NAME));
        if (showManagerColumns) {
    		datasetsGrid.addColumn(ownerColumn, 
    				SafeHtmlUtils.fromSafeConstant(OWNER_COLUMN_NAME));
        }

		// Set the minimum widths of the columns
		double minTableWidth = 0.0;
		datasetsGrid.setColumnWidth(rowNumColumn, 
				UploadDashboard.CHECKBOX_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.CHECKBOX_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(selectedColumn, 
				UploadDashboard.SELECT_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NARROW_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(recordIdColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(featureTypeColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(timestampColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(dataCheckColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(metadataColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(addlDocsColumn, 
				UploadDashboard.FILENAME_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.FILENAME_COLUMN_WIDTH;
//		datasetsGrid.setColumnWidth(versionColumn,
//				UploadDashboard.NARROW_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NARROW_COLUMN_WIDTH;
//		datasetsGrid.setColumnWidth(qcStatusColumn, 
//				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
//		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(archiveStatusColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(filenameColumn, 
				UploadDashboard.FILENAME_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.FILENAME_COLUMN_WIDTH;
		datasetsGrid.setColumnWidth(ownerColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;

		// Set the minimum width of the full table
		datasetsGrid.setMinimumTableWidth(minTableWidth, Style.Unit.EM);

		// Create the data provider for this table
		listProvider = new ListDataProvider<DashboardDataset>();
		listProvider.addDataDisplay(datasetsGrid);

		// Make the columns sortable
		recordIdColumn.setSortable(true);
        featureTypeColumn.setSortable(true);
		timestampColumn.setSortable(true);
		dataCheckColumn.setSortable(true);
		metadataColumn.setSortable(true);
		addlDocsColumn.setSortable(true);
//		versionColumn.setSortable(true);
//		qcStatusColumn.setSortable(true);
		archiveStatusColumn.setSortable(true);
		filenameColumn.setSortable(true);
		ownerColumn.setSortable(true);

		// Add a column sorting handler for these columns
		ListHandler<DashboardDataset> columnSortHandler = 
				new ListHandler<DashboardDataset>(listProvider.getList());
		columnSortHandler.setComparator(recordIdColumn, 
				DashboardDataset.datasetIdComparator);
		columnSortHandler.setComparator(featureTypeColumn, 
				DashboardDataset.featureTypeComparator);
		columnSortHandler.setComparator(timestampColumn, 
				DashboardDataset.timestampComparator);
		columnSortHandler.setComparator(dataCheckColumn, 
				DashboardDataset.dataCheckComparator);
		columnSortHandler.setComparator(metadataColumn, 
				DashboardDataset.mdTimestampComparator);
		columnSortHandler.setComparator(addlDocsColumn, 
				DashboardDataset.addlDocsComparator);
//		columnSortHandler.setComparator(versionColumn, 
//				DashboardDataset.versionComparator);
//		columnSortHandler.setComparator(qcStatusColumn, 
//				DashboardDataset.qcStatusComparator);
		columnSortHandler.setComparator(archiveStatusColumn, 
				DashboardDataset.archiveStatusComparator);
		columnSortHandler.setComparator(filenameColumn, 
				DashboardDataset.filenameComparator);
		columnSortHandler.setComparator(ownerColumn, 
				DashboardDataset.ownerComparator);

		// Add the sort handler to the table, and set the default sort order
		datasetsGrid.addColumnSortHandler(columnSortHandler);
		resortTable();

		// Set the contents if there are no rows
		datasetsGrid.setEmptyTableWidget(new Label(EMPTY_TABLE_TEXT));

		// Following recommended to improve efficiency with IE
		datasetsGrid.setSkipRowHoverCheck(false);
		datasetsGrid.setSkipRowHoverFloatElementCheck(false);
		datasetsGrid.setSkipRowHoverStyleUpdate(false);
	}

	/**
	 * @return the row number column for the table
	 */
	private TextColumn<DashboardDataset> buildRowNumColumn() {
		TextColumn<DashboardDataset> rowNumColumn = new TextColumn<DashboardDataset>() {
			@Override
			public String getValue(DashboardDataset cruise) {
				String expocode = cruise.getDatasetId();
				List<DashboardDataset> cruiseList = listProvider.getList();
				int k = 0;
				while ( k < cruiseList.size() ) {
					// Only check expocodes since they should be unique
					if ( expocode.equals(cruiseList.get(k).getDatasetId()) )
						break;
					k++;
				}
				return Integer.toString(k+1);
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				String msg = getValue(cruise);
				sb.appendHtmlConstant("<div style=\"color: " + 
						UploadDashboard.ROW_NUMBER_COLOR + ";\">");
				for (int k = msg.length(); k < 4; k++)
					sb.appendHtmlConstant("&nbsp;");
				sb.appendEscaped(msg);
				sb.appendHtmlConstant("</div>");
			}
		};
		return rowNumColumn;
	}

	/**
	 * @return the selection header for the table
	 */
	private Header<String> buildSelectionHeader() {
		SelectionCell selectHeaderCell = new SelectionCell(Arrays.asList(
				SELECTION_OPTION_LABEL, 
				ALL_SELECTION_OPTION, 
//				EDITABLE_SELECTION_OPTION, 
//				SUBMITTED_SELECTION_OPTION, 
//				PUBLISHED_SELECTION_OPTION, 
				ARCHIVED_SELECTION_OPTION, 
				CLEAR_SELECTION_OPTION));
		selectHeader = new Header<String>(selectHeaderCell) {
			@Override
			public String getValue() {
				return SELECTION_OPTION_LABEL;
			}
		};
		selectHeader.setUpdater(new ValueUpdater<String>() {
			@Override
			public void update(String option) {
				if ( option == null )
					return;
				setDatasetSelection(option);
			}
		});
		return selectHeader;
	}

	/**
	 * @return the selection column for the table
	 */
	private Column<DashboardDataset,Boolean> buildSelectedColumn() {
		Column<DashboardDataset,Boolean> theSelectedColumn = 
				new Column<DashboardDataset,Boolean>(new CheckboxCell(true, true)) {
			@Override
			public Boolean getValue(DashboardDataset cruise) {
				return new Boolean(cruise.isSelected());
			}
		};
		theSelectedColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,Boolean>() {
			@Override
			public void update(int index, DashboardDataset cruise, Boolean value) {
				cruise.setSelected(value.booleanValue());
				updateAvailableButtons();
			}

		});
		return theSelectedColumn;
	}

	private void updateAvailableButtons() {
		int selectCount = 0;
        Set<FileType>selectedFileTypes = new TreeSet<>();
		for (DashboardDataset cruise : listProvider.getList()) {
			if ( cruise.isSelected()) { 
			    selectCount += 1; 
                selectedFileTypes.add(cruise.getFileType());
		    }
		}
		if ( selectCount == 0 ) {
			disableButtons(selectSet, SELECT_TO_ENABLE_MSG);
		} else if ( selectCount >= 1 ) {
			enableButtons(selectSet);
            if ( selectedFileTypes.contains(FileType.OTHER)) {
                disableInapropriateButtons(selectedFileTypes);
            }
		} 
		if ( selectCount > 1) {
			disableButtons(singleSet, ONLY_ONE_TO_ENABLE_MSG);
		}
        updateSubmissionButton(selectCount);
	}
			
	/**
     * @param selectCount
     */
    private void updateSubmissionButton(int selectCount) {
        if ( selectCount == 1 ) {
            newSubmissionButton.setText(UPDATE_SUBMISSION_BUTTON_TEXT);
            newSubmissionButton.setTitle(UPDATE_SUBMISSION_BUTTON_HOVER_HELP);
            doUpdateSubmission = true;
        } else {
            newSubmissionButton.setText(NEW_SUBMISSION_BUTTON_TEXT);
            newSubmissionButton.setTitle(NEW_SUBMISSION_BUTTON_HOVER_HELP);
            doUpdateSubmission = false;
        }
    }

    /**
     * @param selectedFeatures
     */
    private void disableInapropriateButtons(Set<FileType> selectedFileTypes) {
        for (Button b : noOpaque) {
            b.setEnabled(false);
            String btitle = b.getTitle();
            if ( btitle.indexOf("**" ) < 0 ) {
                b.setTitle(NOT_FOR_OTHER+b.getTitle());
            }
        }
        
    }

    private static void enableButtons(Button[] enableSet) {
		for (Button button : enableSet) {
			String tt = button.getTitle();
			int idx = tt.lastIndexOf("**");
			if ( idx >= 0 ) {
				String revised = tt.substring(idx+3);
				button.setTitle(revised);
			}
			button.setEnabled(true);
		}
	}

	private static void disableButtons(Button[] disableSet, String msg) {
		for (Button button : disableSet) {
            if ( button.getTitle().indexOf("**") < 0 ) {
    			button.setTitle(msg+button.getTitle());
    			button.setEnabled(false);
            }
		}
	}

	/**
	 * @return the dataset column for the table
	 */
	private TextColumn<DashboardDataset> buildDatasetIdColumn() {
		TextColumn<DashboardDataset> expocodeColumn = 
				new TextColumn<DashboardDataset> () {
			@Override
			public String getValue(DashboardDataset cruise) {
				String expocode = cruise.getDatasetId();
				if ( expocode.isEmpty() )
					expocode = NO_DATASET_ID_STRING;
				return expocode;
			}
		};
		return expocodeColumn;
	}

    /**
     * @return the feature type column for the table
     */
    private TextColumn<DashboardDataset> buildFeatureTypeColumn() {
        TextColumn<DashboardDataset> featureTypeColumn = 
                new TextColumn<DashboardDataset> () {
            @Override
            public String getValue(DashboardDataset cruise) {
                String featureType = cruise.getFeatureTypeName();
                if ( featureType.isEmpty() )
                    featureType = FeatureType.UNSPECIFIED.name(); // XXX Should be an error!
                return featureType;
            }
        };
        return featureTypeColumn;
    }


	/**
	 * @return the timestamp column for the table
	 */
	private static TextColumn<DashboardDataset> buildTimestampColumn() {
		TextColumn<DashboardDataset> timestampColumn = 
				new TextColumn<DashboardDataset> () {
			@Override
			public String getValue(DashboardDataset cruise) {
				String timestamp = getZoneTrimmedTime(cruise.getUploadTimestamp());
				return timestamp;
			}
		};
		return timestampColumn;
	}
    
    private static String getZoneTrimmedTime(String timestamp) {
		if ( timestamp == null || timestamp.isEmpty() ) {
			timestamp = NO_TIMESTAMP_STRING;
		} else if ( timestamp.endsWith("Z")) {
            timestamp = timestamp.substring(0, timestamp.indexOf('Z')-1).trim();
        } else if ( timestamp.matches(".*[+-].*")) {
            timestamp = removeOffset(timestamp);
        }
        return timestamp;
    }

    private static String removeOffset(String timestamp) {
//        GWT.log("time:"+timestamp);
        int idx = timestamp.contains("+") ? timestamp.indexOf('+') : timestamp.lastIndexOf('-');
//        GWT.log("idx:"+idx);
        if ( idx > 0 ) { 
            timestamp = timestamp.substring(0, idx-1).trim();
        }
        return timestamp;
    }

	/**
	 * @return the data-check status column for the table
	 */
	private Column<DashboardDataset,String> buildDataCheckColumn() {
		Column<DashboardDataset,String> dataCheckColumn = 
				new Column<DashboardDataset,String> (new ClickableTextCell()) {
			@Override
			public String getValue(DashboardDataset cruise) { 
//                if ( FeatureType.OTHER == cruise.getFeatureType() ) {
//                    return STATUS_CANNOT_CHECK_STRING + ":<br/>Obs Type";
//                }
                if ( FileType.OTHER == cruise.getFileType()) {
                    return STATUS_CANNOT_CHECK_STRING + ":<br/>File Format";
                }
				String status = cruise.getDataCheckStatus();
				if ( status.isEmpty() ) {
					status = NO_DATA_CHECK_STATUS_STRING;
				}
				else if ( status.startsWith( 
						DashboardUtils.CHECK_STATUS_ERRORS_PREFIX) ) { 
					status = status.substring(
							DashboardUtils.CHECK_STATUS_ERRORS_PREFIX.length());
				}
				else if ( status.startsWith(
						DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX) ) {
					status = status.substring(
							DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX.length());
				}
				return status;
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
                
				String msg = getValue(cruise);
                if ( FileType.OTHER == cruise.getFileType() ) {
					sb.appendHtmlConstant("<div >"); // style=\"background-color:" + UploadDashboard.CHECKER_WARNING_COLOR + ";\">");
					sb.appendHtmlConstant(msg);
					sb.appendHtmlConstant("</div>");
                } else
				if ( msg.equals(DashboardUtils.CHECK_STATUS_ACCEPTABLE) ) {
					// No problems - use normal background
					sb.appendHtmlConstant("<div style=\"cursor:pointer;\"><u><em>");
					sb.appendEscaped(msg);
					sb.appendHtmlConstant("</em></u></div>");
				}
				else if ( msg.contains("warnings") || 
						  ( msg.contains("errors") && 
						    ( ! msg.contains(DashboardUtils.GEOPOSITION_ERRORS_MSG) ) && 
						    ( cruise.getNumErrorRows() <= DashboardUtils.MAX_ACCEPTABLE_ERRORS ) ) ) {
					// Only warnings or a few minor errors - use warning background color
					sb.appendHtmlConstant("<div style=\"cursor:pointer; background-color:" +
							UploadDashboard.CHECKER_WARNING_COLOR + ";\"><u><em>");
					sb.appendEscaped(msg);
					sb.appendHtmlConstant("</em></u></div>");
				}
				else if (NO_DATA_CHECK_STATUS_STRING.equals(msg)) {
					sb.appendHtmlConstant("<div style=\"cursor:pointer; background-color:" +
							UploadDashboard.CHECKER_LIGHT_WARNING_COLOR + ";\"><u><em>");
					sb.appendEscaped(msg);
					sb.appendHtmlConstant("</em></u></div>");
				}
				else {
					// Many errors, unacceptable, or not checked - use error background color
					sb.appendHtmlConstant("<div style=\"cursor:pointer; background-color:" +
							UploadDashboard.CHECKER_ERROR_COLOR + ";\"><u><em>");
					sb.appendEscaped(msg);
					sb.appendHtmlConstant("</em></u></div>");
				}
			}
		};
		dataCheckColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,String>() {
			@Override
			public void update(int index, DashboardDataset cruise, String value) {
                if ( FileType.OTHER.equals(cruise.getFileType())) {
                    GWT.log("Cannot view/edit columns for OTHER-type datasets.");
//                    UploadDashboard.showMessage("This observation type cannot be checked.");
                    return;
                }
				// Save the currently selected cruises
				getSelectedDatasets(null);
				// Open the data column specs page for this one cruise
				ArrayList<DashboardDataset> cruises = new ArrayList<>(1);
				cruises.add(cruise);
				DataColumnSpecsPage.showPage(getUsername(), cruises);
			}
		});
		return dataCheckColumn;
	}
    
	static Element meLink;
	
	/**
	 * @return the metadata filename column for the table
	 */
	private Column<DashboardDataset,String> buildMetadataColumn() {
		Column<DashboardDataset,String> metadataColumn = 
				new Column<DashboardDataset,String> (new ClickableTextCell()) {
			@Override
			public String getValue(DashboardDataset cruise) {
				//SafeHtmlBuilder sb = new SafeHtmlBuilder();
                String mdMessage = cruise.getMdStatus();
                if ( ! mdMessage.isEmpty()) {
                    return mdMessage;
                }
				String mdTimestamp = cruise.getMdTimestamp();
				if ( mdTimestamp.isEmpty() ) {
					mdTimestamp = NO_METADATA_STATUS_STRING;
				} else {
				    mdTimestamp = getZoneTrimmedTime(mdTimestamp);
				}
				// sb.appendHtmlConstant("<small>");
				//sb.appendEscaped(mdTimestamp);
				// sb.appendHtmlConstant("</small>");
				//return sb.toSafeHtml().asString();
				return mdTimestamp;
			}
			public void _render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				sb.appendHtmlConstant("<div style=\"cursor:pointer;\"><u><em>");
				sb.appendHtmlConstant(getValue(cruise));
				sb.appendHtmlConstant("</em></u></div>");
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				String cid = cruise.getDatasetId();
				String divid = "mecol_"+cid;
			    sb.appendHtmlConstant("<div id=\""+ divid + "\" style=\"cursor:pointer;\"><u><em>");
				sb.appendEscaped(getValue(cruise));
				sb.appendHtmlConstant("</em></u></div>");
			}
		};
		metadataColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,String>() {
			@Override
			public void update(int index, DashboardDataset cruise, String value) {
                UploadDashboard.logToConsole("load metadata editor for " + cruise + " with " + value + " at " + index);
                String divid = "mecol_"+cruise.getDatasetId();
                meLink = Document.get().getElementById(divid);
                meLink.setAttribute("style", "cursor:wait;");
                UploadDashboard.showWaitCursor();
				// Save the currently selected cruises
				getSelectedDatasets(null);
				// Show the metadata manager page for this one cruise
//				checkSet.clear();
//				checkSet.setUsername(getUsername());
//				checkSet.put(cruise.getDatasetId(), cruise);
                DashboardDatasetList checkSet = new DashboardDatasetList(getUsername());
				checkSet.put(cruise.getDatasetId(), cruise);
				MetadataManagerPage.showPage(checkSet);
			}
		});
		return metadataColumn;
	}

	/**
	 * @return the additional metadata files column for the table
	 */
	private Column<DashboardDataset,String> buildAddnDocsColumn() {
		Column<DashboardDataset,String> addnDocsColumn = 
				new Column<DashboardDataset,String>(new ClickableTextCell()) {
			@Override
			public String getValue(DashboardDataset cruise) {
				TreeSet<String> addlDocTitles = cruise.getAddlDocs();
				if ( addlDocTitles.size() == 0 )
					return NO_ADDL_DOCS_STATUS_STRING;
				SafeHtmlBuilder sb = new SafeHtmlBuilder();
				boolean firstEntry = true;
				for ( String title : addlDocTitles ) {
					if ( firstEntry )
						firstEntry = false;
					else
						sb.appendHtmlConstant("<br />");
					String[] pieces = DashboardMetadata.splitAddlDocsTitle(title);
					sb.appendEscaped(pieces[0]);
					sb.appendHtmlConstant("<small>&nbsp;&nbsp;(");
					sb.appendEscaped(pieces[1]);
					sb.appendHtmlConstant(")</small>");
				}
				return sb.toSafeHtml().asString();
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				sb.appendHtmlConstant("<div style=\"cursor:pointer;\"><u><em>");
				sb.appendHtmlConstant(getValue(cruise));
				sb.appendHtmlConstant("</em></u></div>");
			}
		};
		addnDocsColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,String>() {
			@Override
			public void update(int index, DashboardDataset cruise, String value) {
				// Save the currently selected cruises (in datasetIdsSet)
//				getSelectedDatasets(null);
				// Go to the additional docs page with just this one cruise
				// Go to the QC page after performing the client-side checks on this one cruise
                DashboardDatasetList checkSet = new DashboardDatasetList(getUsername());
				checkSet.put(cruise.getDatasetId(), cruise);
				AddlDocsManagerPage.showPage(checkSet);
			}
		});
		return addnDocsColumn;
	}

	/**
	 * @return the version number column for the table 
	 */
	private TextColumn<DashboardDataset> buildVersionColumn() {
		TextColumn<DashboardDataset> versionColumn = 
				new TextColumn<DashboardDataset> () {
			@Override
			public String getValue(DashboardDataset cruise) {
				return cruise.getVersion();
			}
		};
		return versionColumn;
	}

	/**
	 * @return the QC submission status column for the table
	 *
	private Column<DashboardDataset,String> buildQCStatusColumn() {
		Column<DashboardDataset,String> qcStatusColumn = 
				new Column<DashboardDataset,String> (new ClickableTextCell()) {
			@Override
			public String getValue(DashboardDataset cruise) {
				String status = cruise.getSubmitStatus();
				if ( status.isEmpty() )
					status = NO_QC_STATUS_STRING;
				return status;
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				Boolean editable = cruise.isEditable();
				if ( editable != null ) {
					sb.appendHtmlConstant("<div style=\"cursor:pointer;\"><u><em>");
					sb.appendEscaped(getValue(cruise));
					sb.appendHtmlConstant("</em></u></div>");
				}
				else {
					sb.appendHtmlConstant("<div>");
					sb.appendEscaped(getValue(cruise));
					sb.appendHtmlConstant("</div>");
				}
			}
		};
		qcStatusColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,String>() {
			@Override
			public void update(int index, DashboardDataset cruise, String value) {
				// Respond only for cruises in this version
				Boolean editable = cruise.isEditable();
				if ( editable != null ) {
					// Save the currently selected cruises (in datasetIdsSet)
					getSelectedDatasets(null);
					// Go to the QC page after performing the client-side checks on this one cruise
					checkSet.clear();
					checkSet.setUsername(getUsername());
					checkSet.put(cruise.getDatasetId(), cruise);
					checkDatasetsForSubmitting(SubmitFor.QC);
				}
			}
		});
		return qcStatusColumn;
	}
    */

	/**
	 * @return the archive submission status column for the table
	 */
	private Column<DashboardDataset,String> buildArchiveStatusColumn() {
		Column<DashboardDataset,String> archiveStatusColumn = 
				new Column<DashboardDataset,String> (new ClickableTextCell()) {
			@Override
			public String getValue(DashboardDataset cruise) {
				String status = cruise.getArchiveStatus();
				if ( status.isEmpty() )
					status = NO_ARCHIVE_STATUS_STRING;
				return status;
			}
			@Override
			public void render(Cell.Context ctx, DashboardDataset cruise, 
													SafeHtmlBuilder sb) {
				Boolean editable = cruise.isEditable();
				if ( editable != null ) {
					sb.appendHtmlConstant("<div style=\"cursor:pointer;\"><u><em>");
					sb.appendEscaped(getValue(cruise));
					sb.appendHtmlConstant("</em></u></div>");
				}
				else {
					sb.appendHtmlConstant("<div>");
					sb.appendEscaped(getValue(cruise));
					sb.appendHtmlConstant("</div>");
				}
			}
		};
		archiveStatusColumn.setFieldUpdater(new FieldUpdater<DashboardDataset,String>() {
			@Override
			public void update(int index, DashboardDataset dataset, String value) {
                DashboardDatasetList dlist = new DashboardDatasetList(getUsername()) {{ put(dataset.getDatasetId(), dataset); }};
                preLaunchArchiveSubmit(dlist);
			}
		});
		return archiveStatusColumn;
	}
    
    // Not used.  Leaving for now to remind about "editable"
	private void submitToArchive(DashboardDataset dataset) {
				// Respond only for cruises in this version
				Boolean editable = dataset.isEditable();
				if ( editable != null ) {
					// Save the currently selected cruises (in datasetIdsSet)
//					getSelectedDatasets(null);
					// Go to the QC page after performing the client-side checks on this one cruise
//					checkSet.clear();
//					checkSet.setUsername(getUsername());
//					checkSet.put(dataset.getDatasetId(), dataset);
					checkDatasetsForSubmitting(dataset, SubmitFor.ARCHIVE);
				}
        
	}

	/**
	 * @return the filename column for the table
	 */
	private TextColumn<DashboardDataset> buildFilenameColumn() {
		TextColumn<DashboardDataset> filenameColumn = 
				new TextColumn<DashboardDataset> () {
			@Override
			public String getValue(DashboardDataset cruise) {
				String uploadFilename = cruise.getUploadFilename();
				if ( uploadFilename.isEmpty() )
					uploadFilename = NO_UPLOAD_FILENAME_STRING;
				return uploadFilename;
			}
		};
		return filenameColumn;
	}

	/**
	 * @return the owner column for the table
	 */
	 private TextColumn<DashboardDataset> buildOwnerColumn() {
	 	TextColumn<DashboardDataset> myOwnerColumn = 
	 			new TextColumn<DashboardDataset> () {
	 		@Override
	 		public String getValue(DashboardDataset cruise) {
	 			String owner = cruise.getOwner();
	 			if ( owner.isEmpty() )
	 				owner = NO_OWNER_STRING;
	 			return owner;
	 		}
	 	};
	 	return myOwnerColumn;
	 }

	/**
	 * Checks the cruises given in checkSet in this instance for metadata 
	 * compatibility for submitting for QC.  At this time this only checks 
	 * that an metadata document is associated with each cruise.
	 * 
	 * Then checks the cruises given in checkSet in this instance for data 
	 * compatibility for submitting for QC.  If the data has not been checked 
	 * or is unacceptable, this method presents an error message and returns.  
	 * If the data has many serious issues, asks the user if the submit should 
	 * be continued.  If the answer is yes, or if there were no serious data 
	 * issues, continues submitting for QC by calling 
	 * {@link SubmitForQCPage#showPage(java.util.HashSet)}.
	 */
	private boolean checkDatasetsForSubmitting(SubmitFor to) {
		if ( ! getSelectedDatasets(Boolean.FALSE) ) {
			UploadDashboard.showMessage(
					ARCHIVED_DATASETS_SELECTED_ERR_START + FOR_QC_SUBMIT_ERR_END);
			return false;
		}
		if ( selectedDatasets.size() == 0 ) {
			UploadDashboard.showMessage(
					NO_DATASET_SELECTED_ERR_START + FOR_QC_SUBMIT_ERR_END);
			return false;
		}
        return checkDatasetsForSubmitting(selectedDatasets, to);
	}
	private boolean checkDatasetsForSubmitting(DashboardDataset dataset, SubmitFor to) {
	    DashboardDatasetList checkSet = new DashboardDatasetList(getUsername());
	    checkSet.put(dataset.getDatasetId(), dataset);
        return checkDatasetsForSubmitting(checkSet, to);
	}
    static boolean doItLater = true;
	private static boolean checkDatasetsForSubmitting(DashboardDatasetList checkSet, final SubmitFor to) {
        boolean okToSubmit = true;
        StringBuilder errorMsgBldr = new StringBuilder("The following problems were found:");
        errorMsgBldr.append("<ul>");
        
		for ( DashboardDataset dataset : checkSet.values() ) {
            boolean thisOneIsOk = true;
            String[] errorMessages;
            errorMsgBldr.append("<li>").append(dataset.getDatasetId())
                        .append("<ul>");
            errorMessages = checkMetadata(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
                thisOneIsOk = false;
            }
            okToSubmit = okToSubmit && thisOneIsOk;
		}
        if ( !okToSubmit ) {
            errorMsgBldr.append("</ul>");
            UploadDashboard.showMessage(errorMsgBldr.toString());
            return false;
        }
		for ( DashboardDataset dataset : checkSet.values() ) {
            boolean thisOneIsOk = true;
            String[] errorMessages;
            errorMessages = dataCheck(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
                thisOneIsOk = false;
            }
            errorMessages = columnCheck(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
                thisOneIsOk = false;
            }
            errorMsgBldr.append("</ul></li>");
            okToSubmit = okToSubmit && thisOneIsOk;
		}
        errorMsgBldr.append("</ul>");
        
        if ( !okToSubmit ) {
            boolean finalAnswer;
            errorMsgBldr.append("<br/><br/>Do you still wish to submit to the archive?");
            UploadDashboard.ask(errorMsgBldr.toString(), "Yes", "No", QuestionType.WARNING, 
                                new AsyncCallback<Boolean>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        UploadDashboard.logToConsole("Failure to ask: " + String.valueOf(t));
                                    }
                                    @Override
                                    public void onSuccess(Boolean answer) {
                                        if ( answer.booleanValue() ) {
                                            submitDatasets(checkSet, SubmitFor.ARCHIVE);
                                        }
                                    }
                                });
        }
        return okToSubmit;
	}

    /**
     * @param dataset
     * @return
     */
    private static String[] checkMetadata(DashboardDataset dataset) {
        String[] messages = EMPTY_MESSAGES;
        String status = dataset.getMdStatus();
        if ( dataset.getMdTimestamp().isEmpty() ||
             status.isEmpty() || 
             status.contains("incomplete")) {
            messages = new String[] { "Missing or incomplete metadata." };
        }
        return messages;
    }

    /**
     * @param dataset
     * @return
     */
    private static String[] dataCheck(DashboardDataset dataset) {
        String status = dataset.getDataCheckStatus().toLowerCase();
        if ( FileType.OTHER.equals(dataset.getFileType()) 
             || DashboardUtils.CHECK_STATUS_ACCEPTABLE.equals(status)) {
            return EMPTY_MESSAGES;
        }
        if ( DashboardUtils.CHECK_STATUS_NOT_CHECKED.equals(status)) {
            return new String[] { "Dataset has not been checked." };
        }
        if ( status.contains("error")) {
            return new String[] { "Dataset has data validation errors." };
        }
        return EMPTY_MESSAGES;
    }

	/**
     * @param dataset
     * @return
     */
    private static String[] columnCheck(DashboardDataset dataset) {
        if ( ! hasNoUnknownColumns(dataset)) {
            return new String[] { "Dataset has Unknown columns." };
        }
        return EMPTY_MESSAGES;
    }
    /**
     * @param errorMsgBldr
     * @param errorMessages
     */
    private static void addErrorMessages(StringBuilder errorMsgBldr, String[] errorMessages) {
        for ( String msg : errorMessages ) {
            errorMsgBldr.append("<li>").append(SafeHtmlUtils.htmlEscape(msg)).append("</li>");
        }
    }

    private void old_checkDatasetsForSubmitting(DashboardDatasetList checkSet, final SubmitFor to) {
        for ( DashboardDataset dataset : selectedDatasets.values()) {
            if ( !hasNoUnknownColumns(dataset)) {
                UploadDashboard.showMessage("Dataset " + dataset.getDatasetId() + " has \"Unknown\" columns."
                        + "<br/>All dataset columns must be defined or excluded by being labelled as \"Other.\"");
            }
        }

		// Check if the cruises have metadata documents
		String errMsg = NO_METADATA_HTML_PROLOGUE;
		boolean cannotSubmit = false;
		for ( DashboardDataset cruise : checkSet.values() ) {
			// At this time, just check that some metadata file exists
			// and do not worry about the contents
			if ( to.equals(SubmitFor.ARCHIVE) &&
			        cruise.getMdTimestamp().isEmpty() &&
					cruise.getAddlDocs().isEmpty() ) {
				errMsg += "<li>" + 
						SafeHtmlUtils.htmlEscape(cruise.getDatasetId()) + "</li>";
				cannotSubmit = true;
			}
		}

		// If no metadata documents, cannot submit
		if ( cannotSubmit ) {
			errMsg += NO_METADATA_HTML_EPILOGUE;
			UploadDashboard.showMessage(errMsg);
			return;
		}

		// Check that the cruise data is checked and reasonable
		errMsg = CANNOT_SUBMIT_HTML_PROLOGUE;
		String warnMsg = DATA_AUTOFAIL_HTML_PROLOGUE;
		boolean willAutofail = false;
		for ( DashboardDataset cruise : checkSet.values() ) {
			String status = cruise.getDataCheckStatus();
			if ( ! cruise.getFeatureType().equals(FeatureType.OTHER) &&
			     ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED) ||
				   status.equals(DashboardUtils.CHECK_STATUS_UNACCEPTABLE) ||
				   status.contains(DashboardUtils.GEOPOSITION_ERRORS_MSG) )) {
				errMsg += "<li>" + 
						 SafeHtmlUtils.htmlEscape(cruise.getDatasetId()) + "</li>";
				cannotSubmit = true;
			}
			else if ( cruise.getFeatureType().equals(FeatureType.OTHER) ||
			          status.equals(DashboardUtils.CHECK_STATUS_ACCEPTABLE) ||
					  status.startsWith(DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX) ||
					  ( status.startsWith(DashboardUtils.CHECK_STATUS_ERRORS_PREFIX) &&
						(cruise.getNumErrorRows() <= DashboardUtils.MAX_ACCEPTABLE_ERRORS) ) ) {
				// Acceptable
			}
			else {
				warnMsg += "<li>" + 
					 SafeHtmlUtils.htmlEscape(cruise.getDatasetId()) + "</li>";
				willAutofail = true;
			}
		}

		// If unchecked or very serious data issues, put up error message and stop
		if ( cannotSubmit ) {
			errMsg += CANNOT_SUBMIT_HTML_EPILOGUE;
			UploadDashboard.showMessage(errMsg);
			return;
		}

		// If unreasonable data, ask to continue
		if ( willAutofail ) {
			warnMsg += AUTOFAIL_HTML_EPILOGUE;
			if ( askDataAutofailPopup == null ) {
				askDataAutofailPopup = new DashboardAskPopup(AUTOFAIL_YES_TEXT,
						AUTOFAIL_NO_TEXT, new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean okay) {
						// Only proceed if yes; ignore if no or null
						if ( okay )
							submitDatasets(checkSet, to);
//							SubmitForQCPage.showPage(checkSet);
					}
					@Override
					public void onFailure(Throwable ex) {
						// Never called
					}
				});
			}
			askDataAutofailPopup.askQuestion(warnMsg);
			return;
		}
		// No problems; continue on
		submitDatasets(checkSet, to);
	}

	private static void submitDatasets(DashboardDatasetList submitSet, SubmitFor to) {
		switch (to) {
			case QC:
				SubmitForQCPage.showPage(submitSet);
				break;
			case ARCHIVE:
				SubmitToArchivePage.showPage(submitSet);
				break;
		}
	}

	private void checkDatasetsForSuspension() {
	}
	
	private void suspendDatasets() {
		String username = getUsername();
		HashSet<String> datasetIds = new HashSet<String>();
		for (DashboardDataset ds : selectedDatasets.values()) {
			datasetIds.add(ds.getDatasetId());
		}
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		UploadDashboard.showWaitCursor();
		service.suspendDatasets(username, datasetIds, localTimestamp, new OAPAsyncCallback<Void>() {
			@Override
			public void customFailure(Throwable caught) {
				String errMsg = "There was a problem suspending the datasets: " + caught.getMessage();
				UploadDashboard.showMessage(errMsg);
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void onSuccess(Void result) {
				DatasetListPage.showPage();
				UploadDashboard.showAutoCursor();
			}
		});
		
	}
}
