/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class DashboardServiceException extends RuntimeException implements Serializable, IsSerializable {

    private static final long serialVersionUID = 8455035530658729580L;

    public DashboardServiceException() { super(); }

    public DashboardServiceException(String message) { super(message); }

    public DashboardServiceException(Throwable rootCause) { super(rootCause); }

    public DashboardServiceException(String message, Throwable rootCause) { super(message, rootCause); }

}
