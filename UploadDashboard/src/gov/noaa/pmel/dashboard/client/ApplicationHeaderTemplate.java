/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;

/**
 * @author kamb
 *
 */
public class ApplicationHeaderTemplate extends Composite {

    private static Logger logger = Logger.getLogger(ApplicationHeaderTemplate.class.getName());
    
	public static final String LOGOUT_TEXT = "Logout";
	
    @UiField Label titleLabel;
    @UiField FlowPanel headerRightPanel;
    @UiField Label userInfoLabel;
    @UiField MenuBar menuBar;
    @UiField MenuItem sendFeedbackBtn;
    @UiField MyMenuBar changePasswordMenuBar;
    @UiField MenuItem changePasswordBtn;
    @UiField MenuItem logoutSeparator;
    @UiField MenuItem logoutBtn;
    boolean overMenu = false;
    
    interface ApplicationHeaderTemplateUiBinder extends UiBinder<Widget, ApplicationHeaderTemplate> {
        // nothing needed here.
    }

    private static ApplicationHeaderTemplateUiBinder uiBinder = GWT.create(ApplicationHeaderTemplateUiBinder.class);

    public ApplicationHeaderTemplate() {
        initWidget(uiBinder.createAndBindUi(this));
        menuBar.setAutoOpen(true);
        changePasswordBtn.getElement().setId("changePasswordBtn");
        changePasswordMenuBar.setParentMenu(menuBar);
		logoutBtn.setText(LOGOUT_TEXT);
		logoutBtn.setTitle(LOGOUT_TEXT);
        logoutBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                GWT.log("AHT execute logout command");
                doLogout();
            }
        });
        logoutSeparator.setEnabled(false);
        changePasswordBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                showChangePasswordPopup();
            }
        });
        sendFeedbackBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                doSendFeedback();
            }
        });
    }

    public void setLogoutHandler(ScheduledCommand cmd) {
        logoutBtn.setScheduledCommand(cmd);
    }
    
    protected void setPageTitle(String title) {
        titleLabel.setText(title);
    }
    
    void doLogout() {
        logger.info("Logger Header logout");
        UploadDashboard.closePopups();
        try {
        UploadDashboard.getService().logoutUser(new OAPAsyncCallback<Void>("logout") {
            @Override
            public void onSuccess(Void nada) {
                UploadDashboard.logToConsole("Logout success");
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign("dashboardlogout.html");
            }
            @Override
            public void handleException(Throwable ex) {
                UploadDashboard.logToConsole("Logout error:" + ex.toString());
//                Window.alert(String.valueOf(ex));
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign("dashboardlogout.html");
            }
        });
        } catch (Throwable t) {
            UploadDashboard.logToConsole("logout exception: "+ String.valueOf(t));
//            Window.alert(String.valueOf(t));
            Window.Location.assign("dashboardlogout.html");
        }
    }

    static void doSendFeedback() {
        GWT.log("GWT log Header sendFeedback");
        logger.info("Logger Header sendFeedback");
        UploadDashboard.showFeedbackPopup();
    }

    private static void showChangePasswordPopup() {
        GWT.log("show change password popoupa");
        UploadDashboard.showChangePasswordPopup();
    }
    
    public void setDatasetIds(String datasetIds) {
        String currentText = titleLabel.getText();
        if ( currentText.indexOf(':') > 0 ) {
            currentText = currentText.substring(0, currentText.indexOf(':'));
        }
        String newText = currentText + ": " + datasetIds;
        titleLabel.setText(newText);
    }

    /**
     * @param cruises
     */
    public void addDatasetIds(DashboardDatasetList cruises) {
        String cruiseIds = extractCruiseIds(cruises);
        setDatasetIds(cruiseIds);
    }
    
    public void addDatasetIds(Collection<String> cruiseIds) {
        String datasetIds = extractCruiseIds(cruiseIds);
        setDatasetIds(datasetIds);
    }

    private static String extractCruiseIds(DashboardDatasetList cruises) {
        List<String> names = new ArrayList<>(cruises.values().size()); 
        for (DashboardDataset dd : cruises.values()) {
            names.add(dd.getUserDatasetName());
        }
        return extractCruiseIds(names);
    }
    
    /**
     * @param cruises
     * @return
     */
    private static String extractCruiseIds(Collection<String> cruises) {
        StringBuilder ids = new StringBuilder();
        String comma = "";
        for (String id : cruises) {
            ids.append(comma).append(id);
            comma = ", ";
        }
        return ids.toString();
    }
}
