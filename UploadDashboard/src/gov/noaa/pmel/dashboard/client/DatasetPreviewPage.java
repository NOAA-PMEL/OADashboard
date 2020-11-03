/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.apache.logging.log4j.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.PreviewPlotImage;
import gov.noaa.pmel.dashboard.shared.PreviewPlotResponse;
import gov.noaa.pmel.dashboard.shared.PreviewTab;

/**
 * Page showing various plots of cruise data.
 * These plots are to be examined by a user 
 * to catch errors prior to submitting for QC.
 * 
 * @author Karl Smith
 */
public class DatasetPreviewPage extends CompositeWithUsername {
	
	Logger logger = Logger.getLogger("DatasetPreviewPage");

	private static final String TITLE_TEXT = "Preview Dataset";

	private static final String INTRO_HTML_PROLOGUE = 
			"Plots of the dataset: ";

	private static final String REFRESH_TEXT = "Refresh plots";
	private static final String REFRESH_HOVER_HELP = "Regenerate the plot images";
	private static final String DONE_TEXT = "Done";

	private static final String PLOT_GENERATION_FAILURE_HTML = "<b>Problems generating the plot previews</b>";

	private static final String TAB0_TEXT = "Overview";
	private static final String TAB1_TEXT = "Profiles";
	private static final String TAB2_TEXT = "BioGeoChem";
	private static final String TAB3_TEXT = "Nutrients +";

	private static final String TAB0_ALT_TEXT = "Overview plots";
	private static final String TAB1_ALT_TEXT = "Plots vs depth";
	private static final String TAB2_ALT_TEXT = "Property-property plots";
	private static final String TAB3_ALT_TEXT = "Measured nutrients vs depth";

	public static final String LAT_VS_LON_IMAGE_NAME = "lat_vs_lon";
	public static final String LAT_LON_IMAGE_NAME = "lat_lon";
	public static final String SAMPLE_VS_TIME_IMAGE_NAME = "sample_vs_time";

	interface DatasetPreviewPageUiBinder extends UiBinder<Widget, DatasetPreviewPage> {
	}

	private static DatasetPreviewPageUiBinder uiBinder = 
			GWT.create(DatasetPreviewPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

    @UiField ApplicationHeaderTemplate header;
	@UiField HTML introHtml;
	@UiField Button refreshButton;
	@UiField Button doneButton;
	
//	@UiField ResizeLayoutPanel resizePanel;
	@UiField TabLayoutPanel tabsPanel;
	@UiField FlowPanel tab0Panel;
	@UiField FlowPanel tab1Panel;
	@UiField FlowPanel tab2Panel;
	@UiField FlowPanel tab3Panel;

	@UiField HTML tab0Html;
	@UiField HTML tab1Html;
	@UiField HTML tab2Html;
	@UiField HTML tab3Html;

	List<PreviewTab> availablePlots;
	List<FlowPanel> tabPanels = new ArrayList<FlowPanel>();
    List<HTML> tabHeadings = new ArrayList<>();
	List<List<Image>> tabImages = new ArrayList<List<Image>>();
	
	private String datasetId;
	private String timetag;
    private FeatureType obsType;

	private AsyncCallback<PreviewPlotResponse> checkStatusCallback;

	// The singleton instance of this page
	private static DatasetPreviewPage singleton;

	public DatasetPreviewPage() {
        super(PagesEnum.PREVIEW_DATASET.name());
		initWidget(uiBinder.createAndBindUi(this));
		
		logger.addHandler(new ConsoleLogHandler());
		logger.setLevel(Level.ALL);
		
		singleton = this;

		setUsername(null);
		datasetId = "";
		// Callback when generating plots
		checkStatusCallback = new OAPAsyncCallback<PreviewPlotResponse>("get preview images") {
			@Override
			public void onSuccess(PreviewPlotResponse plotResponse) {
				logger.fine("Got response " + plotResponse);
				boolean isDone = plotResponse.isFinished();
				if ( UploadDashboard.isCurrentPage(singleton) ) {
					if ( isDone ) {
						UploadDashboard.showAutoCursor();
					}
					availablePlots = plotResponse.getPlotTabs();
					// Refresh this page to get the new image(s)
					singleton.resetImageUrls();
					if ( ! isDone ) {
						// More images to be generated - inquire again
						service.buildPreviewImages(getUsername(), singleton.datasetId, 
								singleton.timetag, false, checkStatusCallback);
					}
				}
			}
			@Override
			public void handleException(Throwable ex) {
                super.handleException(ex);
				singleton.clearTabImages();
			}
		};

		header.setPageTitle(TITLE_TEXT);
		
		refreshButton.setText(REFRESH_TEXT);
		refreshButton.setTitle(REFRESH_HOVER_HELP);
		doneButton.setText(DONE_TEXT);

        resetTabs();
        
		tabPanels.add(tab0Panel);
        tabHeadings.add(tab0Html);
		tabPanels.add(tab1Panel);
        tabHeadings.add(tab1Html);
		tabPanels.add(tab2Panel);
        tabHeadings.add(tab2Html);
		tabPanels.add(tab3Panel);
        tabHeadings.add(tab3Html);
	}

	/**
	 * Display the preview page in the RootLayoutPanel with data plots  
	 * for the first cruise in the given cruiseList.  
	 * Adds this page to the page history.
	 */
	static void showPage(DashboardDatasetList cruiseList) {
		if ( singleton == null )
			singleton = new DatasetPreviewPage();
// 		String datasetId = cruiseList.keySet().iterator().next(); 
		DashboardDataset dataset = cruiseList.get(cruiseList.keySet().iterator().next()); 
		singleton.updatePreviewPlots(dataset,
									 cruiseList.getUsername(), false);
		UploadDashboard.updateCurrentPage(singleton);
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

	/**
	 * Updates the this page with the plots for the indicated cruise.
	 * 
	 * @param dataset
	 * 		cruises to use
	 * @param username
	 * 		user requesting these plots 
	 */
	private void updatePreviewPlots(DashboardDataset dataset, String username, boolean force) {
        String recordId = dataset.getRecordId();
		if ( recordId == null ) { throw new IllegalArgumentException("Null dataset id"); }
        if ( ! recordId.equals(datasetId)) { logger.warning("Changing dataset ID without changing feature type."); }
		this.datasetId = recordId;
        obsType = dataset.getFeatureType();
		setUsername(username);
		header.setDatasetIds(dataset.getUserDatasetName());
        header.userInfoLabel.setText(WELCOME_INTRO + getUsername()); // XXX TODO: This should be in setUsername...
        this.updatePreviewPlots(recordId, username, force);
	}
	private void updatePreviewPlots(String recordId, String username, boolean force) {
		
//		introHtml.setHTML(INTRO_HTML_PROLOGUE + SafeHtmlUtils.htmlEscape(this.datasetId));
//		if ( this.expocode.length() > 11 ) { // WTF?
			// Tell the server to generate the preview plots
			UploadDashboard.showWaitCursor();
			DateTimeFormat formatter = DateTimeFormat.getFormat("MMddHHmmss");
			this.timetag = formatter.format(new Date(), TimeZone.createTimeZone(0));
			service.buildPreviewImages(getUsername(), this.datasetId, 
										this.timetag, force, checkStatusCallback);
//		}
		// Set the URLs for the images.
		resetImageUrls();
	}

//	private ClickHandler imageClicked = new ClickHandler() {
//		@Override
//		public void onClick(ClickEvent event) {
//			String eventString = event.toDebugString();
//			logger.log(Level.FINE, "Click:"+eventString);
//		}
//	};
	/**
	 * Assigns the URLs to the images.  
	 * This triggers load events so the page should refresh when this is called.
	 */
	private void resetImageUrls() {
		String imagePrefix;
		// XXX This should really come from the server...
		imagePrefix = "preview/plots/" + datasetId.substring(0,4) + "/" + datasetId + "/";
		clearTabImages();
		if ( availablePlots == null || availablePlots.isEmpty()) {
			showLoadingImg();
		} else {
			int tab = 0;
			Image img = null;
			String noCache="?ts=" + System.currentTimeMillis();
			for (PreviewTab tabImageFileNames : availablePlots) {
				List<Image> tabXimages = new ArrayList<Image>(); // tabImages.get(tab);
				tabImages.add(tabXimages);
				FlowPanel tabPanel = tabPanels.get(tab);
//                tabsPanel.add(tabPanel);
                tabPanel.setVisible(true);
                HTML tabHeading = tabHeadings.get(tab);
                tabHeading.getParent().setVisible(true);
                tabHeading.setHTML(tabImageFileNames.title());
				for (final PreviewPlotImage imageInfo : tabImageFileNames.getPlots()) {
					String url = imagePrefix + imageInfo.fileName + noCache;
					img = new Image(url);
					img.setTitle(imageInfo.imageTitle);
					img.addDomHandler(new DoubleClickHandler() {
						@Override
						public void onDoubleClick(DoubleClickEvent event) {
							String eventString = "DblClick:"+event.toDebugString();
							logger.log(Level.FINE, eventString);
							Object oSource = event.getSource();
							Image image = (Image)oSource;
							String iSrc = image.getUrl();
							UploadDashboard.showPreviewImage(singleton, imageInfo, iSrc);
						}

					}, DoubleClickEvent.getType());
					img.setStyleName("plotsimage");
					tabPanel.add(img);
					tabXimages.add(img);
				}
				tab += 1;
				if ( img != null ) {
					img.addStyleName("lastimage");
				}
			}
		}
	}

	private void showLoadingImg() {
		String url = "images/loadingCircleSpinner.gif";
		Image img = new Image(url);
		img.setStyleName("loadingimage");
		List<Image> tab0Images = new ArrayList<>();
		tab0Images.add(img);
		tab0Panel.add(img);
		tabImages.add(tab0Images);
		tabsPanel.selectTab(0);
	}

	private void clearTabImages() {
		if ( tabImages.isEmpty()) { return; }
		for (int tab = 0; tab < tabImages.size(); tab++ ) {
			List<Image> tabXimages = tabImages.get(tab);
			FlowPanel tabPanel = tabPanels.get(tab);
			for (Image img : tabXimages) {
				tabPanel.remove(img);
			}
			tabXimages.clear();
		}
		tabImages.clear();
        resetTabs();
	}
    
	private void resetTabs() {
        // Tab HTML text
		tab0Html.setHTML("Loading..."); // TAB0_TEXT);
		tab1Html.setHTML("Tab1"); // TAB1_TEXT);
		tab2Html.setHTML("Tab2"); // TAB2_TEXT);
		tab3Html.setHTML("Tab3"); // TAB3_TEXT);
		// Set hover helps for the tabs
		tab0Html.setTitle(TAB0_ALT_TEXT);
		tab1Html.setTitle(TAB1_ALT_TEXT);
		tab2Html.setTitle(TAB2_ALT_TEXT);
		tab3Html.setTitle(TAB3_ALT_TEXT);
		// Hide until necessary
        tab1Panel.setVisible(false);
        tab1Html.getParent().setVisible(false);
        tab2Panel.setVisible(false);
        tab2Html.getParent().setVisible(false);
        tab3Panel.setVisible(false);
        tab3Html.getParent().setVisible(false);
	}

	private void resetImageUrls(boolean forceRebuild) {
		if ( forceRebuild ) {
			clearTabImages();
			if ( availablePlots != null ) {
				availablePlots.clear();
			}
			updatePreviewPlots(datasetId, getUsername(), forceRebuild);
		} else {
			resetImageUrls();
		}
	}
	
	@UiHandler("refreshButton")
	void refreshOnClick(ClickEvent event) {
		// Forces a regeneration of the preview images.
		availablePlots = null;
		UploadDashboard.closePreviews(this);
		UploadDashboard.closePopups();
		resetImageUrls(true);
	}

	@UiHandler("doneButton")
	void cancelOnClick(ClickEvent event) {
		availablePlots = null;
		UploadDashboard.closePreviews(this);
		DatasetListPage.showPage();
	}

}
