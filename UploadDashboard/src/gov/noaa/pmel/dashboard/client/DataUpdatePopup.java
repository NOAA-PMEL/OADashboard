/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * Page for uploading new or updated cruise data files.
 * 
 * @author Karl Smith
 */
public class DataUpdatePopup extends Composite {

	private static final String TITLE_TEXT = "Upload Data Files";

    private static final String TITLE_BASE = 
             "Upload a new version of the data file for dataset ";
	private static final String DESCRIPTION_HTML1 = 
            "Select a new version of the data file "
            + "<span style='font-family:\"Courier New\", Courier, monospace;'>";
	private static final String DESCRIPTION_HTML2 = 
	        "</span> to upload to replace the existing."
            + "<br/>This must be the same observation type as the original.";
	
	private static final String SETTINGS_CAPTION_TEXT = "Settings";

	private static final String ADVANCED_HTML_MSG = 
			"Select a character set encoding for this file." +
			"<ul>" +
			"<li>If you are unsure of the encoding, UTF-8 should work fine.</li>" +
			"<li>The main differences in UTF-8 and ISO encodings are the " +
			"\"extended\" characters.</li>" +
			"<li>Use UTF-16 only if you know your file is encoded in that format, " +
			"but be aware that only Western European characters can be " +
			"properly handled.</li>" +
			"<li>Use the Windows encoding only for files produced by older " +
			"Windows programs. </li>" +
			"<li>The preview button will show the beginning of the file as it will " +
			"be seen by the dashboard using the given encoding.</li>" +
			"</ul>";
	private static final String ENCODING_TEXT = "File encoding:";
	private static final String[] KNOWN_ENCODINGS = {
		"ISO-8859-1", "ISO-8859-15", "UTF-8", "UTF-16", "Windows-1252"
	};
	private static final String PREVIEW_TEXT = "Preview";
	private static final String NO_PREVIEW_HTML_MSG = "<p>(No file previewed)</p>";

	private static final String SUBMIT_TEXT = "Upload";
	private static final String CANCEL_TEXT = "Cancel";

	private static final String SELECT_FEATURE_TYPE_MSG = 
            "Please select observation type.";
	private static final String SELECT_FILE_TYPE_MSG = 
            "Please select file format.";
	private static final String NO_FILE_ERROR_MSG = 
			"Please select a data file to upload";
	private static final String UNEXPLAINED_FAIL_MSG = 
			"<h3>Upload failed.</h3>" + 
			"<p>Unexpectedly, no explanation of the failure was given</p>";
	private static final String FAIL_MSG_START = 
			"<h3>";
	private static final String EXPLAINED_FAIL_MSG_START =
			"<br />Upload failed.</h3>" +
			"<p><pre>\n";
	private static final String EXPLAINED_FAIL_MSG_END = 
			"</pre></p>";
    private static final String UPLOAD_FAILED_SUGGESTIONS = 
            "This may be due to failure to properly parse the file.<br/>"
            + "Only ASCII delimited files and Excel spreadsheets are accepted.<br/>"
            + "You can use the Preview function to check the file.";
	private static final String NO_DATASET_NAME_FAIL_MSG = 
			"<br />Dataset/Cruise name column not found.</h3>" +
			"<p>The data file needs to contain a data column specifying " +
			"the dataset or cruise name for each measurement.  Please " +
			"verify that an appropriately-named column exists for the " +
			"dataset or cruise name, and appropriate values are given for " +
			"each sample.</p>";
	private static final String DATASET_EXISTS_FAIL_MSG_START = 
			"<br />A dataset exists with a given dataset ID.</h3>";
	private static final String DATASET_EXISTS_FAIL_MSG_END = 
			"<p>Either you specified that this data upload file should " +
			"only create new datasets, or you do not have permission " +
			"to modify a dataset for this data.</p>";

	// Remove javascript added by the firewall
	private static final String JAVASCRIPT_START = "<script language=\"javascript\">";
	private static final String JAVASCRIPT_CLOSE = "</script>";

	interface DashboardDatasetUploadPageUiBinder extends UiBinder<Widget, DataUpdatePopup> {
	}

	private static DashboardDatasetUploadPageUiBinder uiBinder = 
			GWT.create(DashboardDatasetUploadPageUiBinder.class);
   
//    @UiField ListBox featureTypeSelector;
    @UiField Hidden featureTypeToken;
    @UiField Hidden observationTypeToken;
//    @UiField Anchor featureTypeHelpAnchor;
//    @UiField Panel featureTypeSpecificContentPanel;
//    @UiField ListBox fileTypeSelector;
    @UiField Hidden fileTypeToken;
//    @UiField Anchor fileTypeHelpAnchor;
    
//    @UiField ApplicationHeaderTemplate header;
	@UiField HTML title;
	@UiField HTML descriptionHtml;
//	@UiField Anchor moreHelpAnchor;
	@UiField FormPanel uploadForm;
    @UiField FileUpload fileUpload;
	@UiField Hidden timestampToken;
	@UiField Hidden actionToken;
	@UiField Hidden encodingToken;
    @UiField Hidden fileDataFormatToken;
    @UiField Hidden datasetIdToken;
    @UiField Hidden datasetIdColumnToken;
    @UiField Hidden previousFileNameToken;
//	@UiField CaptionPanel settingsCaption;
	@UiField DisclosurePanel advancedPanel;
//	@UiField HTML advancedHtml;
	@UiField Label encodingLabel;
	@UiField ListBox encodingListBox;
////	@UiField HTML previewHtml;
//	@UiField RadioButton createRadio;
////	@UiField RadioButton appendRadio;
//	@UiField RadioButton overwriteRadio;
//    @UiField FlowPanel actionSettingsPanel;
	@UiField Button previewButton;
	@UiField Button submitButton;
	@UiField Button cancelButton;

//	private DashboardInfoPopup moreHelpPopup;
//    private DashboardInfoPopup featureTypeHelpPopup;
//    private DashboardInfoPopup fileTypeHelpPopup;
	private Element uploadElement;

	// Singleton instance of this page
	private static DataUpdatePopup singleton = null;
//    private FeatureTypeFields _featureTypeFields = null;
//    private FeatureType selectedFeatureType = FeatureType.UNSPECIFIED;
//    private FileType selectedFileType = FileType.UNSPECIFIED;
    
//    private static Map<FeatureType, FeatureTypeFields> _featureSpecificPanels = new HashMap<FeatureType, FeatureTypeFields>();
//    private static Map<FileType, FeatureTypeFields> _fileSpecificPanels = new HashMap<FileType, FeatureTypeFields>();

    private DashboardDataset dataset;

    private PopupPanel parentPanel;
    
    private String username;

	/**
	 * Creates an empty cruise upload page.  Do not call this 
	 * constructor; instead use the showPage static method 
	 * to show the singleton instance of this page. 
	 */
	DataUpdatePopup(String username) {
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

        parentPanel = new PopupPanel(false, true);
        parentPanel.setWidget(this);
        
		this.username = username;

		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
//        fileUpload.getElement().setAttribute("multiple", "multiple");
        fileUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                String files = fileUpload.getFilename(); // getInputFileNames(uploadElement);
                boolean fileSelected = files != null && files.length() > 0;
                submitButton.setEnabled(fileSelected);
                previewButton.setEnabled(fileSelected);
                cancelButton.setText("Cancel");
            }
        });
        uploadElement = fileUpload.getElement();
		// Create the HTML5 multiple-file upload in the HTML <div>
//		dataUpload.setHTML("<input type=\"file\" name=\"datafiles\" " + "id=\"datafiles\" style=\"width: 100%;\" multiple />");
		// Get the multiple file input element within the HTML <div>
//		uploadElement = dataUpload.getElement();
		for (int k = 0; k < uploadElement.getChildCount(); k++) {
			Element childElem = (Element) uploadElement.getChild(k);
			if ( "datafiles".equals(childElem.getId()) ) {
				uploadElement = childElem;
				break;
			}
		}

		clearTokens();

//		settingsCaption.setCaptionText(SETTINGS_CAPTION_TEXT);
//
//		createRadio.setText(CREATE_TEXT);
//		createRadio.setTitle(CREATE_HOVER_HELP);
////		appendRadio.setText(APPEND_TEXT);
////		appendRadio.setTitle(APPEND_HOVER_HELP);
//		overwriteRadio.setText(OVERWRITE_TEXT);
//		overwriteRadio.setTitle(OVERWRITE_HOVER_HELP);
//		createRadio.setValue(true, false);
////		appendRadio.setValue(false, false);
//		overwriteRadio.setValue(false, false);
//        actionSettingsPanel.setVisible(false);

		submitButton.setText(SUBMIT_TEXT);
        submitButton.setEnabled(false); // files and feature type must be selected.
        previewButton.setEnabled(false); // files and feature type must be selected.
		cancelButton.setText(CANCEL_TEXT);

//		advancedHtml.setHTML(ADVANCED_HTML_MSG);
		encodingLabel.setText(ENCODING_TEXT);
		encodingListBox.setVisibleItemCount(1);
		for ( String encoding : KNOWN_ENCODINGS )
			encodingListBox.addItem(encoding);
		previewButton.setText(PREVIEW_TEXT);
	}

//    private static FeatureTypeFields getFileTypePanel(FileType selectedType) {
//        FeatureTypeFields fieldsPanel = null;
//        if ( _fileSpecificPanels.containsKey(selectedType)) {
//            fieldsPanel = _fileSpecificPanels.get(selectedType);
//        } else {
//            switch ( selectedType ) {
//              case OTHER:
////              case NETCDF:
////              case NC_DSG:
//                  fieldsPanel = new OpaqueUploadFeatureFields();
//                  break;
//              case DELIMITED:
//                  fieldsPanel = new CommonFeatureFields();
//                  break;
//              default: // just to shut-up the code checker
//            }
//            if ( fieldsPanel != null ) {
//                _fileSpecificPanels.put(selectedType, fieldsPanel);
//            }
//        }
//        return fieldsPanel;
//    }
//    private static FeatureTypeFields getFeatureTypePanel(FeatureType selectedType) {
//        FeatureTypeFields fieldsPanel = null;
//        if ( _featureSpecificPanels.containsKey(selectedType)) {
//            fieldsPanel = _featureSpecificPanels.get(selectedType);
//        } else {
//            switch ( selectedType ) {
//              case OTHER:
//                  fieldsPanel = new OpaqueUploadFeatureFields();
//                  break;
//              case PROFILE:
//                  fieldsPanel = new ProfileFeatureFields(); // TODO: Right now, this is really only a container for CommonFields
//                  break;
//              case TIMESERIES:
//              case TRAJECTORY:
//              case TRAJECTORY_PROFILE:
//                  fieldsPanel = new CommonFeatureFields();
//                  break;
//              default: // just to shut-up the code checker
//            }
//            if ( fieldsPanel != null ) {
//                _featureSpecificPanels.put(selectedType, fieldsPanel);
//            }
//        }
//        return fieldsPanel;
//    }
    
    private String getDisplayText(DashboardDataset dataset) {
        String text;
        String uploadedFile = dataset.getUploadFilename();
        if ( uploadedFile == null || uploadedFile.trim().equals("")) {
            text = "Upload data file for for submission record " + dataset.getRecordId();
        } else {
            text = TITLE_BASE + dataset.getUserDatasetName();
        }
        return text;
    }
    private void setDisplayTitle(String titleText) {
        title.setHTML(titleText);
    }
    private void setDescription(String filename) {
        descriptionHtml.setHTML(DESCRIPTION_HTML1 + filename + DESCRIPTION_HTML2);
    }

    void setDataset(DashboardDataset dataset) {
        this.dataset = dataset;
    }
	/**
	 * Display the cruise upload page in the RootLayoutPanel
	 * after clearing as much of the page as possible.  
	 * The upload filename cannot be cleared. 
	 * Adds this page to the page history.
	 */
	void showPage(DashboardDataset dataset) {
		clearTokens();
        clearForm();
        setDataset(dataset);
        // setting this changes datasetname (user dataset name) property.
//        setDatasetIdToken(dataset.getDatasetId());
        setDisplayTitle(getDisplayText(dataset));
        previousFileNameToken.setValue(dataset.getUploadFilename());
		uploadForm.setAction(GWT.getModuleBaseURL() + "DataUploadService/update/" + dataset.getDatasetId());
//        setDescription(dataset.getUploadFilename());
//		singleton.userInfoLabel.setText(WELCOME_INTRO + singleton.getUsername());
//        singleton.featureTypeSelector.removeStyleName("missingInfoItem");
//        singleton.featureTypeSelector.setSelectedIndex(0);
//        singleton.fileTypeSelector.removeStyleName("missingInfoItem");
//        singleton.fileTypeSelector.setSelectedIndex(0);
//        if ( singleton._featureTypeFields != null ) {
//            singleton.featureTypeSpecificContentPanel.remove(singleton._featureTypeFields);
//            singleton._featureTypeFields = null;
//        }
//		singleton.previewHtml.setHTML(NO_PREVIEW_HTML_MSG);
		encodingListBox.setSelectedIndex(2);
		advancedPanel.setOpen(false);
//        singleton.header.userInfoLabel.setText(WELCOME_INTRO + username);
//		UploadDashboard.updateCurrentPage(singleton, true);
//		History.newItem(PagesEnum.UPLOAD_DATA.name(), false);
        parentPanel.center();
	}

	/**
	 * Assigns the values of the Hidden tokens on the page.
	 * 
	 * @param requestAction
	 * 		action to request (value to assign to the actionToken)
	 */
	private void assignTolkiens(String requestAction) {
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		String encoding = KNOWN_ENCODINGS[encodingListBox.getSelectedIndex()];
		timestampToken.setValue(localTimestamp);
		actionToken.setValue(requestAction);
		encodingToken.setValue(encoding);
        featureTypeToken.setValue(dataset.getFeatureTypeName());
        observationTypeToken.setValue(dataset.getUserObservationType());
        fileTypeToken.setValue(dataset.getFileTypeName());
	}

	/**
	 * Clears the values of the Hidden tokens on the page.
	 */
	private void clearTokens() {
		timestampToken.setValue("");
		actionToken.setValue("");
		encodingToken.setValue("");
	}

	/**
	 * @param input
	 * 		multiple file input HTML element
	 * @return
	 * 		a " ; "-separated list of the filenames given 
	 * 		in the multiple file input HTML element
	 */
	private static native String getInputFileNames(Element input) /*-{
        var namesString = "";

        // Just in case not multiple
        if ( typeof (input.files) == 'undefined' || 
             typeof (input.files.length) == 'undefined') {
            return input.value;
        }

        for (var k = 0; k < input.files.length; k++) {
            if ( k > 0 ) {
                namesString += " ; ";
            }
            namesString += input.files[k].name;
        }
        return namesString;
	}-*/;

	private static native void clearInputFileNames(Element input) /*-{
        if ( typeof (input.files) == 'undefined' || 
             typeof (input.files.length) == 'undefined') {
            input.value = 'undefined';
        } else {
           
        }
    }-*/;
	
	@UiHandler("previewButton") 
	void previewButtonOnClick(ClickEvent event) {
		String namesString = getInputFileNames(uploadElement).trim();
		if (  namesString.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}
		assignTolkiens(DashboardUtils.PREVIEW_REQUEST_TAG);
		uploadForm.submit();
	}

	@UiHandler("submitButton") 
	void submitButtonOnClick(ClickEvent event) {
		String newFileName = getInputFileNames(uploadElement).trim();
		if (  newFileName.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}
        if ( ! dataset.getUploadFilename().equals("")) {
            String replaceMessage;
            if ( ! newFileName.equals(dataset.getUploadFilename())) {
                replaceMessage = buildReplaceMessage(dataset.getUploadFilename(), newFileName);
            } else {
                replaceMessage = buildUpdateMessage(newFileName);
            }
            UploadDashboard.ask(replaceMessage,"Yes","Cancel",MessageInformationType.QUESTION,new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean answer) {
                    if ( answer.booleanValue()) {
                		assignTolkiens(DashboardUtils.OVERWRITE_DATASETS_REQUEST_TAG);
                		uploadForm.submit();
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    UploadDashboard.logToConsole("Ask failure:" + String.valueOf(t));
                    // ignore // XXX shouldn't happen...
                }
            });
        } else { // just do it
       		assignTolkiens(DashboardUtils.OVERWRITE_DATASETS_REQUEST_TAG);
       		uploadForm.submit();
        }
	}

	/**
     * @param uploadFilename
     * @param newFileName
     * @return
     */
    private static String buildReplaceMessage(String uploadFilename, String newFileName) {
        StringBuilder sb = new StringBuilder("Confirm: replace file ");
        sb.append("<span style='font-family:\"Courier New\", Courier, monospace;'>")
          .append(uploadFilename)
          .append("</span><br/>")
          .append("with new file ")
          .append("<span style='font-family:\"Courier New\", Courier, monospace;'>")
          .append(newFileName)
          .append("</span>?");
        return sb.toString();
    }

    /**
     * @param newFileName
     * @return
     */
    private static String buildUpdateMessage(String newFileName) {
        StringBuilder sb = new StringBuilder("Confirm: replace uploaded file ");
        sb.append("<span style='font-family:\"Courier New\", Courier, monospace;'>")
          .append(newFileName)
          .append("</span><br/>")
          .append("with a new version?");
        
        return sb.toString();
    }

    @UiHandler("cancelButton")
	void cancelButtonOnClick(ClickEvent event) {
        parentPanel.hide();
		// Make sure the normal cursor is shown
		UploadDashboard.showAutoCursor();
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmit(SubmitEvent event) {
		UploadDashboard.showWaitCursor();
	}

    private void clearForm() {
        uploadForm.reset();
        clearInputFileNames(uploadElement);
//        featureTypeSelector.setSelectedIndex(0);
//        fileTypeSelector.setSelectedIndex(0);
        submitButton.setEnabled(false); 
        previewButton.setEnabled(false); 
        cancelButton.setText("Cancel");
    }
        
	@UiHandler("uploadForm")
	void uploadFormOnSubmitComplete(SubmitCompleteEvent event) {
		boolean wasSuccess = processResultMsg(event.getResults());
        GWT.log("Upload was successful: " + wasSuccess);
        if ( wasSuccess ) {
            clearForm();
    		clearTokens();
            parentPanel.hide();
        }
		UploadDashboard.showAutoCursor();
        DatasetListPage.showPage();
	}

	/**
	 * Process the message returned from the upload of a data file.
	 * 
	 * @param resultMsg
	 * 		message returned from the upload of a dataset
	 */
	private boolean processResultMsg(String resultMsg) {
		// Check the returned results
		if ( resultMsg == null ) {
			UploadDashboard.showMessage(UNEXPLAINED_FAIL_MSG);
			return false;
		}
		String[] splitMsgs = resultMsg.trim().split("\n");

		// Preview is a special case - the start of the first file is returned
		if ( splitMsgs[0].startsWith(DashboardUtils.FILE_PREVIEW_HEADER_TAG) ) {
			// show partial file contents in the preview
			String previewMsg = "<pre>\n";
			for (int k = 1; k < splitMsgs.length; k++) {
				// Some clean-up: remove the javascript that is added by the firewall
				if ( splitMsgs[k].trim().startsWith(JAVASCRIPT_START) ) {
					do {
						k++;
						if ( k >= splitMsgs.length )
							break;
					} while ( ! splitMsgs[k].trim().startsWith(JAVASCRIPT_CLOSE) );
				}
				else {
					previewMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
				}
			}
			previewMsg += "</pre>";
//			advancedPanel.setOpen(true);
//			previewHtml.setHTML(previewMsg);
            int wHeight = Window.getClientHeight();
            int cHeight = (int)(.6 * wHeight);
            int wWidth = Window.getClientWidth();
            int cWidth = (int)(.7 * wWidth);
            UploadDashboard.showDataPreviewPopup(previewMsg, cHeight, cWidth);
			return false;
		}

		ArrayList<String> cruiseIDs = new ArrayList<String>();
		ArrayList<String> errMsgs = new ArrayList<String>();
		for (int k = 0; k < splitMsgs.length; k++) {
			String responseMsgItem = splitMsgs[k].trim();
			if ( responseMsgItem.startsWith(DashboardUtils.SUCCESS_HEADER_TAG) ) {
				// Success
				cruiseIDs.add(responseMsgItem.substring(DashboardUtils.SUCCESS_HEADER_TAG.length()).trim());
			}
			else if ( responseMsgItem.startsWith(DashboardUtils.INVALID_FILE_HEADER_TAG) ) {
				// An exception was thrown while processing the input file
				String filename = responseMsgItem.substring(DashboardUtils.INVALID_FILE_HEADER_TAG.length()).trim();
				String failMsg = FAIL_MSG_START + SafeHtmlUtils.htmlEscape(filename) + EXPLAINED_FAIL_MSG_START;
				for (k++; k < splitMsgs.length; k++) {
					if ( splitMsgs[k].trim().startsWith(DashboardUtils.END_OF_ERROR_MESSAGE_TAG) )
						break;
					failMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
				}
				errMsgs.add(failMsg);
				errMsgs.add(EXPLAINED_FAIL_MSG_END);
				errMsgs.add(UPLOAD_FAILED_SUGGESTIONS);
			}
			else if ( responseMsgItem.startsWith(DashboardUtils.DATASET_EXISTS_HEADER_TAG) ) {
				// Dataset file exists and not permitted to modify
				String[] info = responseMsgItem.substring(DashboardUtils.DATASET_EXISTS_HEADER_TAG.length()).trim().split(" ; ", 4);
				String failMsg = FAIL_MSG_START;
				if ( info.length > 1 ) 
					failMsg += SafeHtmlUtils.htmlEscape(info[1].trim()) + " - ";
				failMsg += SafeHtmlUtils.htmlEscape(info[0].trim());
				failMsg += DATASET_EXISTS_FAIL_MSG_START;
				if ( info.length > 2 )
					failMsg += "<p>&nbsp;&nbsp;&nbsp;&nbsp;Owner = " + SafeHtmlUtils.htmlEscape(info[2].trim()) + "</p>";
				if ( info.length > 3 )
					failMsg += "<p>&nbsp;&nbsp;&nbsp;&nbsp;Submit Status = " + SafeHtmlUtils.htmlEscape(info[3].trim()) + "</p>";
				errMsgs.add(failMsg + DATASET_EXISTS_FAIL_MSG_END); 
			}
			else if ( responseMsgItem.startsWith(JAVASCRIPT_START) ) {
				// ignore the added javascript from the firewall
				do {
					k++;
					if ( k >= splitMsgs.length )
						break;
				} while ( ! splitMsgs[k].trim().startsWith(JAVASCRIPT_CLOSE) );
			} else if ( resultMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) {
			    UploadDashboard.showLoginPopup();
                return false; // XXX mid-method bail-out. yuk.
			} else {
				//  some other error message, display the whole message and be done with it
				String failMsg = "<pre>";
				do {
					failMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
					k++;
				} while ( k < splitMsgs.length );
				errMsgs.add(failMsg + "</pre>");
			}
		}

		boolean wasCompleteSuccess = errMsgs.size() == 0;
		
		// Display any error messages from the upload
		if ( errMsgs.size() > 0 ) {
			String errors = "";
			for ( String msg : errMsgs ) 
				errors += msg;
			UploadDashboard.showMessage(errors);
		}

		// Process any successes
		if ( ! cruiseIDs.isEmpty() ) {
//			for ( String expo : cruiseIDs )
//				DatasetListPage.addSelectedDataset(expo);
			DatasetListPage.resortTable();
//            DatasetListPage.showPage();
            parentPanel.hide();
//            if ( ! ( // featureTypeSelector.getSelectedValue().equals(FeatureType.OTHER.name()) || 
//                     fileTypeSelector.getSelectedValue().equals(FileType.OTHER.name()))) {
//    			DataColumnSpecsPage.showPage(getUsername(), cruiseIDs);
//            } else {
//                UploadDashboard.showMessage("Upload Successful!");
//            }
		}
		return wasCompleteSuccess;
	}

    /**
     * @param value
     */
    public void setDatasetIdToken(String value) {
        datasetIdToken.setValue(value);
    }
    
    public void setDatasetIdColumnNameToken(String value) {
        datasetIdColumnToken.setValue(value);
    }

    /**
     * @param name
     */
    public void setFileDataFormatToken(String name) {
        fileDataFormatToken.setValue(name);
    }
}
