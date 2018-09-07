/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import gov.noaa.pmel.dashboard.handlers.FileXferService.XFER_PROTOCOL;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;

/**
 * @author kamb
 *
 */
public abstract class BaseTransferAgent {

    protected final String SPACE = " ";
    private String userid;
    private String host;
    private String idFileLocation;
    private String targetDestination;
    
    protected FileXferService.XFER_PROTOCOL _protocol;
    
    protected BaseTransferAgent(XFER_PROTOCOL protocol) {
        _protocol = protocol;
    }
    
    protected String getUserId() throws PropertyNotFoundException {
        if ( userid == null ) {
            userid = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".username");
        }
        return userid;
    }
    protected String getHost() throws PropertyNotFoundException {
        if ( host == null ) {
            host = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".hostname");
        }
        return host;
    }
    protected String getIdFileLocation() throws PropertyNotFoundException {
        if ( idFileLocation == null ) {
            idFileLocation = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".id_file");
        }
        return idFileLocation;
    }
    protected String getTargetDestination() throws PropertyNotFoundException {
        if ( targetDestination == null ) {
            targetDestination = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".destination");
        }
        return targetDestination;
    }
    
}