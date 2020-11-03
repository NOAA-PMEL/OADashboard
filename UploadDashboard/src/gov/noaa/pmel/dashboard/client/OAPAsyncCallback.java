/**
 * 
 */
package gov.noaa.pmel.dashboard.client;


import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * @author kamb
 *
 */
public abstract class OAPAsyncCallback<T> implements AsyncCallback<T> {

    private static Logger logger = Logger.getLogger(OAPAsyncCallback.class.getName());
    
    private String _operation;
    
//    public OAPAsyncCallback() {
//        _operation = "";
//    }
    
    public OAPAsyncCallback(String operation) {
        _operation = operation;
    }
    
    /* (non-Javadoc)
     * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
     */
    @Override
    public void onFailure(Throwable error) {
        UploadDashboard.showAutoCursor();
        UploadDashboard.logToConsole(error.toString());
        if ( !sessionExpired(error) ) {
            handleException(error);
        }
    }
    private static final String SESSION_EXPIRED_KEY = "SESSION HAS EXPIRED";
    protected static boolean sessionExpired(Throwable error) {
        if (( error.getMessage().indexOf(SESSION_EXPIRED_KEY) >= 0 ) ||
              ( error instanceof StatusCodeException &&
              ((StatusCodeException)error).getStatusCode() == 401 )) {
            UploadDashboard.showLoginPopup();
            return true;
        } else {
            return false;
        }
    }
    
    public void handleException(Throwable error) {
        UploadDashboard.serviceException(_operation, error);
    }
}
