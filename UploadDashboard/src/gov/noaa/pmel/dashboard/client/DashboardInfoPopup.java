/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * An message embedded within a PopupPanel.
 * 
 * @author Karl Smith
 */
public class DashboardInfoPopup extends Composite {

	private static final String DISMISS_TEXT = "Dismiss";

	interface DashboardInfoPopupUiBinder extends UiBinder<Widget, DashboardInfoPopup> {
	}

	private static DashboardInfoPopupUiBinder uiBinder = 
			GWT.create(DashboardInfoPopupUiBinder.class);

    @UiField Image msgTypeIcon;
	@UiField HTML infoHtml;
	@UiField Button dismissButton;

    private OAPAsyncCallback<?> continuation;
    
	private PopupPanel parentPanel;

	/**
	 * Creates an empty message within a PopupPanel.
	 * The popup includes a dismiss button to hide it.  
	 * Use {@link #setInfoMessage(String)} to assign 
	 * the message to be displayed.  
	 * Use {@link #showAtPosition(int, int)} 
	 * or {@link #showInCenterOf(UIObject)} 
	 * to show the popup.
	 */
	DashboardInfoPopup() {
		initWidget(uiBinder.createAndBindUi(this));
		dismissButton.setText(DISMISS_TEXT);
		parentPanel = new PopupPanel(false);
		parentPanel.setWidget(this);
        msgTypeIcon.setVisible(false);
	}

	DashboardInfoPopup(String htmlMsg, OAPAsyncCallback<?> continuation) {
        this(htmlMsg, DISMISS_TEXT, continuation);
	}
    
	DashboardInfoPopup(String htmlMsg, String dismissBtnText, OAPAsyncCallback<?> continuation) {
        this();
        this.setInfoMessage(htmlMsg);
        this.dismissButton.setText(dismissBtnText);
        this.continuation = continuation;
	}
    
	DashboardInfoPopup(String htmlMsg, String dismissBtnText, 
	                   MessageInformationType msgType, OAPAsyncCallback<?> continuation) {
        this(htmlMsg, dismissBtnText, continuation);
        setMessageType(msgType);
	}
    
	void setMessageType(MessageInformationType msgType) {
        if ( msgType != null ) {
            msgTypeIcon.setVisible(true);
            msgTypeIcon.setUrl(msgType.iconSrc());
        } else {
            msgTypeIcon.setVisible(false);
        }
	}
    
	/**
	 * @param htmlMessage
	 * 		the unchecked HTML message to display.
	 * 		For safety, use only known (static) HTML.
	 */
	void setInfoMessage(String htmlMessage) {
		infoHtml.setHTML(htmlMessage);
	}

	/**
	 * Show the popup relative to the given object.
	 * See {@link PopupPanel#showRelativeTo(UIObject)}.
	 * 
	 * @param obj
	 * 		show relative to this UI object
	 */
	void showRelativeTo(UIObject obj) {
		parentPanel.showRelativeTo(obj);
	}

	/**
	 * Show the popup centered in the browser window.
	 */
	void showCentered() {
		parentPanel.center();
	}

	@UiHandler("dismissButton")
	void onClick(ClickEvent e) {
		parentPanel.hide();
        if ( continuation != null ) {
            continuation.onSuccess(null);
        }
	}

    public void dismiss() {
		parentPanel.hide();
	}
}
