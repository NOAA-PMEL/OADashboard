/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.actions.DatasetModifier;
import gov.noaa.pmel.dashboard.actions.MetadataPoster;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.FeedbackHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.PreviewPlotsHandler;
import gov.noaa.pmel.dashboard.handlers.UserFileHandler;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.programs.CloneSubmission;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.submission.status.StatusRecord;
import gov.noaa.pmel.dashboard.server.submission.status.SubmissionRecord;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardServiceException;
import gov.noaa.pmel.dashboard.shared.DashboardServiceResponse;
import gov.noaa.pmel.dashboard.shared.DashboardServiceResponse.DashboardServiceResponseBuilder;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.dashboard.shared.PreviewPlotResponse;
import gov.noaa.pmel.dashboard.shared.PreviewTab;
import gov.noaa.pmel.dashboard.shared.ADCMessageList;
import gov.noaa.pmel.dashboard.shared.TypesDatasetDataPair;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;
import gov.noaa.pmel.tws.util.TimeUtils;

/**
 * Implementation of DashboardServicesInterface
 * 
 * @author Karl Smith
 */
public class DashboardServices extends RemoteServiceServlet implements DashboardServicesInterface {

	private static final long serialVersionUID = -8189933983319827049L;

    private static DashboardConfigStore store;
    static {
        try {
            store = DashboardConfigStore.get(true);
            logger = LogManager.getLogger(DashboardServices.class);
        } catch (IOException iex) {
            iex.printStackTrace();
            System.exit(-42);
        }
    }
    
	// To get config debug set: -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=TRACE
	private static Logger logger; //  = LogManager.getLogger(DashboardServices.class);
	
	private String username = null;
	private DashboardConfigStore configStore = null;

	@Override
	public void logoutUser() {
		HttpServletRequest request = getThreadLocalRequest();
		username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			// Probably null pointer exception - leave username null
		}
		logger.info("logging out user " + username);
		try {
			request.logout();
		} catch ( Exception ex ) {
			logger.error("request.logout failed: " + ex);
		}
		HttpSession session = request.getSession(false);
		try {
			session.invalidate();
		} catch ( Exception ex ) {
			// Log but otherwise ignore this error
			logger.error("session.invalidate failed: " + ex);
		}
		logger.info("logged out " + username);
	}

    @Override
    public void submitFeedback(String pageUsername, String type, String message) {
        validateRequest(pageUsername);
        try {
            FeedbackHandler.logFeedbackMessage(pageUsername, type, message);
            FeedbackHandler.notifyFeedbackMessage(pageUsername, type, message);
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
        }
    }

    @Override
    public DashboardServiceResponse changePassword(String pageUsername, String currentpw, String newpw) {
        validateRequest(pageUsername);
        DashboardServiceResponseBuilder response = DashboardServiceResponse.builder();
        logger.info("Changing password for user: " + pageUsername);
        boolean changed = false;
        try {
            Users.changeUserPassword(pageUsername, currentpw, newpw);
            changed = true;
        } catch (DashboardException ex) {
            logger.info(ex,ex);
            response.error(ex.getMessage());
        }
        response.wasSuccessful(changed);
        return response.build();
    }
	/**
	 * Validates the given request by retrieving the current username from the request
	 * and verifying that username with the Dashboard data store.  If pageUsername is
	 * given, also checks these usernames are the same.
	 * Assigns the username and configStore fields in this instance.
	 * 
	 * @param pageUsername
	 * 		if not null, check that this matches the current page username
	 * @return
	 * 		true if the request obtained a valid username; otherwise false
	 * @throws IllegalArgumentException
	 * 		if unable to obtain the dashboard data store
	 */
	private boolean validateRequest(String pageUsername) throws IllegalArgumentException {
		username = null;
		HttpServletRequest request = getThreadLocalRequest();
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			// Probably null pointer exception
			return false;
		}
		if ( (pageUsername != null) && ! pageUsername.equals(username) )
			return false;

        // Really ?
		configStore = null;
		try {
			configStore = DashboardConfigStore.get(true);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unexpected configuration error: " + ex.getMessage());
		}
        // XXX TODO: No longer using ConfigStore to validate Users.
//		return configStore.validateUser(username);
        return true;
	}

	@Override
	public DashboardServiceResponse<DashboardDatasetList> getDatasetList(String pageUsername) 
	        throws IllegalArgumentException 
	{
		// Get the dashboard data store and current username
        logger.debug(pageUsername);
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
        try {
    		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(username);
    		logger.info("dataset list returned for " + username);
            String version = getBuildVersion();
            DashboardServiceResponse<DashboardDatasetList> response = 
                    DashboardServiceResponse.<DashboardDatasetList>builder()
                        .response(datasetList)
                        .version(version)
                        .build();
    		return response;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("get dataset list", t);
        }
	}

	/**
     * @return
     */
    private static String buildVersion = null;
    private String getBuildVersion() {
        if ( buildVersion == null ) {
            buildVersion = "v_"+findBuildVersion();
        }
        return buildVersion;
    }

    @Override
	public DashboardDatasetList cloneDataset(String pageUsername, String submissionRecordId,
	                                         boolean copyAssociatedFiles)
		 throws IllegalArgumentException {
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
        
        try {
            CloneSubmission.Clone(submissionRecordId, pageUsername, copyAssociatedFiles);
    		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(username);
    		logger.info("dataset list returned for " + username);
            
    		return datasetList;
        } catch (Throwable iox) {
            logger.warn(iox, iox);
            Notifications.Alert("SDIS Exception", iox);
            throw new DashboardServiceException("Error cloning submission record", iox);
        }
    }
    /**
     * @return
     */
    private String findBuildVersion() {
		HttpServletRequest request = getThreadLocalRequest();
        String cpath = request.getContextPath();
        String wpath = "webapps/" + cpath.substring(1).replaceAll("/", "#") + ".war";
        File warFile = new File(wpath);
        if ( ! warFile.exists()) {
            System.out.println(new File(".").getAbsolutePath());
            warFile = new File("../oa#Dashboard.war");
            if ( ! warFile.exists()) {
                return "n/a";
            }
        }
        long modTime = warFile.lastModified();
        String version =  new SimpleDateFormat("yyyyMMdd.HHmm").format(new Date(modTime));
        return version;
    }

    @Override
	public DashboardDatasetList deleteDatasets(String pageUsername, TreeSet<String> idsSet, 
			Boolean deleteMetadata) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		DataFileHandler dataHandler = configStore.getDataFileHandler();
    		// Delete each of the datesets in the given set
    		for ( String datasetId : idsSet ) {
    			dataHandler.deleteDatasetFiles(datasetId, username, deleteMetadata);
    			// IllegalArgumentException for other problems escape as-is
    			logger.info("dataset " + datasetId + " deleted by " + username);
    		}
    
    		// Return the current list of datasets, which should 
    		// detect the missing datasets and update itself
    		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(username);
    		logger.info("dataset list returned for " + username);
    		return datasetList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error deleting datasets", t);
        }
	}

	@Override
	public DashboardDatasetList filterDatasetsToList(String pageUsername, 
			String wildDatasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Add the datasets to the user's list and return the updated list
    		DashboardDatasetList cruiseList = configStore.getUserFileHandler()
    				.getFilteredDatasets(wildDatasetId, username);
    		logger.info("filtered datasets " + wildDatasetId + " for " + username + ": " + cruiseList);
    		return cruiseList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error filtering datasets", t);
        }
	}

	@Override
	public DashboardDatasetList addDatasetsToList(String pageUsername, 
			String wildDatasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Add the datasets to the user's list and return the updated list
    		DashboardDatasetList cruiseList = configStore.getUserFileHandler()
    				.addDatasetsToListing(wildDatasetId, username);
    		logger.info("added datasets " + wildDatasetId + " for " + username);
    		return cruiseList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error adding datasets", t);
        }
	}

	@Override
	public DashboardDatasetList removeDatasetsFromList(String pageUsername,
			TreeSet<String> idsSet) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Remove the datasets from the user's list and return the updated list
    		DashboardDatasetList datasetList = configStore.getUserFileHandler()
    				.removeDatasetsFromListing(idsSet, username);
    		logger.info("removed datasets " + idsSet.toString() + " for " + username);
    		return datasetList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error removing datasets", t);
        }
	}

	@Override
	public DashboardDatasetList changeDatasetOwner(String pageUsername, 
			TreeSet<String> idsSet, String newOwner) throws IllegalArgumentException {
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
        try {
    		// Get the dashboard username of the new owner
    		String newUsername;
    		if ( Users.validateUser(newOwner) ) {
    			// dashboard username was given
    			newUsername = newOwner;
    		}
    		else {
    			// actual name given? // XXX This won't work.
    			try {
                    User newUser = Users.getUser(newOwner);
                    newUsername = newUser.username();
    			} catch (Exception ex) {
    				newUsername = null;
    			}
    			if ( (newUsername == null) || ! Users.validateUser(newUsername) ) 
    				throw new IllegalArgumentException("Unknown dashboard user " + newOwner);
    		}
    		// Change the owner of the datasets
    		DatasetModifier modifier = new DatasetModifier(configStore);
    		for ( String datasetId : idsSet ) {
    			modifier.changeDatasetOwner(datasetId, newUsername);
    			logger.info("changed owner of " + datasetId + " to " + newUsername);
    		}
    		// Return the updated list of cruises for this user
    		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(pageUsername);
    		return datasetList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error changing dataset owner", t);
        }
	}

	@Override
	public DashboardDatasetList getUpdatedDatasets(String pageUsername, 
			TreeSet<String> idsSet) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Create the set of updated dataset information to return
    		DataFileHandler dataHandler = configStore.getDataFileHandler();
    		DashboardDatasetList datasetList = new DashboardDatasetList(username);
    		datasetList.setManager(Users.isManager(username));
    		for ( String datasetId : idsSet ) {
    			datasetList.put(datasetId, dataHandler.getDatasetFromInfoFile(datasetId));
    		}
    		logger.info("returned updated dataset information for " + username);
    		return datasetList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error getting updated datasets", t);
        }
	}

	@Override
	public DashboardDatasetList deleteAddlDoc(String pageUsername, String deleteFilename, 
			String datasetId, TreeSet<String> allIds) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		DataFileHandler dataHandler = configStore.getDataFileHandler();
    		DashboardDataset dataset = dataHandler.getDatasetFromInfoFile(datasetId);
    
    		// Get the current metadata documents for the cruise
    		MetadataFileHandler mdataHandler = configStore.getMetadataFileHandler();
    		// XXX TODO: OME_FILENAME check
    		if ( MetadataFileHandler.metadataFilename(datasetId).equals(deleteFilename) ) {
    			// Remove the OME XML stub file
    			if ( ! Boolean.TRUE.equals(dataset.isEditable()) ) 
    				throw new IllegalArgumentException("Cannot delete the metadata for a submitted dataset");
    		}
    //		else if ( DashboardUtils.PI_OME_FILENAME.equals(deleteFilename) ) {
    //			// No more PI-provided OME metadata for this cruise
    //			dataset.setMdTimestamp(null);
    //		}
    		else {
    			// Directly modify the additional documents list in this dataset
    			TreeSet<String> addlDocs = dataset.getAddlDocs();
    			// Find this additional document for this cruise
    			String titleToRemove = null;
    			for ( String docTitle : addlDocs ) {
    				String name = DashboardMetadata.splitAddlDocsTitle(docTitle)[0];
    				if ( name.equals(deleteFilename) ) {
    					titleToRemove = docTitle;
    					break;
    				}
    			}
    			if ( (titleToRemove == null) || ! addlDocs.remove(titleToRemove) )
    				throw new IllegalArgumentException("Document " + deleteFilename + 
    						" is not associated with dataset " + datasetId);
    		}
    		mdataHandler.deleteMetadataFile(username, datasetId, deleteFilename);
    
    		logger.info("deleted metadata " + deleteFilename + 
    				" from " + datasetId + " for " + username);
    
    		// Save the updated cruise
    		dataHandler.saveDatasetInfoToFile(dataset, "Removed metadata document " + 
    									deleteFilename + " from dataset " + datasetId);
    
    		// Create the set of updated dataset information to return
    		DashboardDatasetList datasetList = new DashboardDatasetList(username);
    		datasetList.setManager(Users.isManager(username));
    		for ( String id : allIds ) {
    			datasetList.put(id, dataHandler.getDatasetFromInfoFile(id));
    		}
    		logger.info("returned updated dataset information for " + username);
    		return datasetList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error deleting additional documents", t);
        }
	}

	@Override
	public TypesDatasetDataPair getDataColumnSpecs(String pageUsername,
			String datasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Get the list of known user-provided data column types
    		KnownDataTypes knownUserTypes = configStore.getKnownUserDataTypes();
    		if ( knownUserTypes == null )
    			throw new IllegalArgumentException("unexpected missing list of all known data column types");
    		TreeSet<DashDataType<?>> knownTypesSet = knownUserTypes.getKnownTypesSet();
    		if ( knownTypesSet.isEmpty() )
    			throw new IllegalArgumentException("unexpected empty list of all known data column types");
    		ArrayList<DataColumnType> knownTypesList = new ArrayList<DataColumnType>(knownTypesSet.size());
    		for ( DashDataType<?> dtype : knownTypesSet )
    			knownTypesList.add(dtype.dataColumnType());
    
            DataFileHandler dataFileHandler = configStore.getDataFileHandler();
    		// Get the cruise with the first maximum-needed number of rows
            DashboardDatasetData dataset = dataFileHandler
    				.getDatasetDataFromFiles(datasetId, 0, DashboardUtils.MAX_ROWS_PER_GRID_PAGE);
    		if ( dataset == null )
    			throw new IllegalArgumentException(datasetId + " does not exist");
    
    		TypesDatasetDataPair typesAndDataset = new TypesDatasetDataPair();
    		typesAndDataset.setAllKnownTypes(knownTypesList);
    		typesAndDataset.setDatasetData(dataset);
    
    		// So we can show the error message on the page.
    		ADCMessageList msgs = getDataMessages(pageUsername, datasetId);
    		if ( msgs != null ) {
    			typesAndDataset.setMsgList(msgs);
    		}
    		logger.info("data columns specs returned for " + 
    				datasetId + " for " + username);
    		// Return the cruise with the partial data
    		return typesAndDataset;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error getting data column specifications", t);
        }
	}

    @Override
	public ArrayList<ArrayList<String>> getDataWithRowNum(String pageUsername, 
			String datasetId, int firstRow, int numRows) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		int myFirstRow = firstRow;
    		if ( myFirstRow < 0 )
    			myFirstRow = 0;
    		// Get only the desired data from the dataset
    		DashboardDatasetData dataset = configStore.getDataFileHandler()
    									.getDatasetDataFromFiles(datasetId, myFirstRow, numRows);
    		if ( dataset == null )
    			throw new IllegalArgumentException(datasetId + " does not exist");
    		ArrayList<ArrayList<String>> dataWithRowNums = dataset.getDataValues();
    		ArrayList<Integer> rowNums = dataset.getRowNums();
    		// Modify the list in this DashboardDatasetData since it is then thrown away
    		int k = 0;
    		for ( ArrayList<String> rowData : dataWithRowNums ) {
    			rowData.add(0, rowNums.get(k).toString());
    			k++;
    		}
    		int myLastRow = myFirstRow + dataWithRowNums.size() - 1;
    		logger.info(datasetId + " dataset data [" + Integer.toString(myFirstRow) + 
    				" - " + Integer.toString(myLastRow) + "] returned for " + username);
    		if ( logger.isTraceEnabled()) {
    			for (k = 0; k < dataWithRowNums.size(); k++) {
    				logger.debug("  data[" + Integer.toString(k) + "]=" + dataWithRowNums.get(k).toString());
    			}
    		}
    		return dataWithRowNums;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error retrieving dataset data", t);
        }
	}

	@Override
	public DashboardDatasetData saveDataColumnSpecs(String pageUsername, DashboardDataset newSpecs)
			throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Retrieve all the current cruise data
    		DashboardDatasetData dataset = configStore.getDataFileHandler()
    						.getDatasetDataFromFiles(newSpecs.getDatasetId(), 0, -1);
    		if ( ! dataset.isEditable() )
    			throw new IllegalArgumentException(newSpecs.getDatasetId() + 
    					" has been submitted for QC; data column types cannot be modified.");
    
    		// Revise the data column types and units 
    		if ( newSpecs.getDataColTypes().size() != dataset.getDataColTypes().size() )
    			throw new IllegalArgumentException("Unexpected number of data columns (" +
    					newSpecs.getDataColTypes().size() + " instead of " + 
    					dataset.getDataColTypes().size());
    		dataset.setDataColTypes(newSpecs.getDataColTypes());
    
    		// Save and commit the updated data columns
    		configStore.getDataFileHandler().saveDatasetInfoToFile(dataset, 
    				"Data column types, units, and missing values for " + 
    				dataset.getDatasetId() + " updated by " + username);
    		// Update the user-specific data column names to types, units, and missing values 
    		configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, username);
    		if ( ! username.equals(dataset.getOwner()) )
    			configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, dataset.getOwner());
    		
    		logger.info("data columns specs saved for " + 
    				dataset.getDatasetId() + " by " + username);
    		
    		return dataset;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error saving data column specifications", t);
        }
	}
	
	@Override
	public TypesDatasetDataPair updateDataColumnSpecs(String pageUsername,
			DashboardDataset newSpecs) throws IllegalArgumentException {
		TypesDatasetDataPair tddp = new TypesDatasetDataPair();
		
        try {
    		// Get the dashboard data store and current username, and validate that username
    		if ( ! validateRequest(pageUsername) ) 
    			throw new IllegalArgumentException("Invalid user request");
    
    		String datasetId = newSpecs.getDatasetId();
    		
    		// Retrieve all the current cruise data
    		DashboardDatasetData dataset = configStore.getDataFileHandler()
    						.getDatasetDataFromFiles(datasetId, 0, -1);
    		if ( ! dataset.isEditable() )
    			throw new IllegalArgumentException(datasetId + 
    					" has been submitted for QC; data column types cannot be modified.");
    
    		// Revise the data column types and units 
    		if ( newSpecs.getDataColTypes().size() != dataset.getDataColTypes().size() )
    			throw new IllegalArgumentException("Unexpected number of data columns (" +
    					newSpecs.getDataColTypes().size() + " instead of " + 
    					dataset.getDataColTypes().size());
    		dataset.setDataColTypes(newSpecs.getDataColTypes());
    
    		// Run the automated data checker with the updated data types.
    		// Assigns the data check status and the WOCE-3 and WOCE-4 flags.
    		DatasetChecker checker = configStore.getDashboardDatasetChecker(dataset.getFeatureType());
    		StdUserDataArray stdArray;
            try {
        		stdArray = checker.standardizeDataset(dataset, null);
            } catch (Throwable ex) {
                logger.warn("Exception checking dataset:"+ ex, ex);
                throw new IllegalArgumentException("There was an error checking the dataset: " + ex.getMessage());
            }
    
    		// Save and commit the updated data columns
    		configStore.getDataFileHandler().saveDatasetInfoToFile(dataset, 
    				"Data column types, units, and missing values for " + 
    				datasetId + " updated by " + username);
    		// Update the user-specific data column names to types, units, and missing values 
    		configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, username);
    		
            if ( stdArray.hasDate() && stdArray.hasLatitude() && stdArray.hasLongitude() &&
                    ! stdArray.hasMissingTimeOrLocation()) { 
    //              ! stdArray.hasCriticalError()) {
                MetadataFileHandler metafiles = configStore.getMetadataFileHandler();
                try {
                    File metadataFile = metafiles.getMetadataFile(datasetId);
                    if ( ! metadataFile.exists()) { // Should not generally happen.
                        logger.warn("Metadata file not found: " + metadataFile.getPath());
                        OADSMetadata.createInitialOADSMetadataFile(datasetId, pageUsername);
                    }
                    OadsMetadataDocumentType mdDoc = OADSMetadata.extractOADSMetadata(stdArray, metadataFile);
    //                OadsMetadataDocumentType existgMd = MetadataFileHandler.
        			metafiles.saveOadsXmlDoc(mdDoc, datasetId, "Auto-extraction from StdArray");
                } catch (Exception ex) {
                    logger.warn("Exception extracting metadata:"+ex, ex);
                }
                try {
                    metafiles.saveLocationsFile(stdArray);
                } catch (Exception ex) {
                    logger.warn("Exception saving latlon file:"+ex, ex);
                }
    		} else {
    		    logger.info("Dataset " + dataset.getRecordId() + " has critical error.  Unable to extract metadata.");
    		}
    		
    		// ??? Is this possible at this point for a user to be editing another user's dataset ?
    		if ( ! username.equals(dataset.getOwner()) )
    			configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, dataset.getOwner());
    		
    		// Remove all but the first maximum-needed number of rows of cruise data 
    		// to minimize the payload of the returned cruise data
    		int numRows = dataset.getNumDataRows();
    		if ( numRows > DashboardUtils.MAX_ROWS_PER_GRID_PAGE ) {
    			dataset.getDataValues()
    				   .subList(DashboardUtils.MAX_ROWS_PER_GRID_PAGE, numRows)
    				   .clear();
    			dataset.getRowNums()
    				   .subList(DashboardUtils.MAX_ROWS_PER_GRID_PAGE, numRows)
    				   .clear();
    		}
    
    		logger.info("data columns specs updated for " + 
    				datasetId + " by " + username);
    		// Return the updated truncated cruise data for redisplay 
    		// in the DataColumnSpecsPage
    		tddp.setDatasetData(dataset);
    		
    		// So we can show the error message on the page.
    		// XXX TODO: ??? can't we get these from the stdArray ?
    		ADCMessageList msgs = getDataMessages(pageUsername, datasetId);
    		if ( msgs != null ) {
    			tddp.setMsgList(msgs);
    		}
    		return tddp;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error updating data column specifications", t);
        }
	}

	@Override
	public void updateDataColumns(String pageUsername, 
			ArrayList<String> idsList) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		DataFileHandler dataHandler = configStore.getDataFileHandler();
    		UserFileHandler userHandler = configStore.getUserFileHandler();
    		Logger dataSpecsLogger = logger;
    
    		for ( String datasetId : idsList ) {
    			// Retrieve all the current data
    			DashboardDatasetData dataset = dataHandler.getDatasetDataFromFiles(datasetId, 0, -1);
    			if ( ! dataset.isEditable() )
    				throw new IllegalArgumentException("Dataset " + datasetId + 
    						" has been submitted for QC; data column types cannot be modified.");
    
        		DatasetChecker datasetChecker = configStore.getDashboardDatasetChecker(dataset.getFeatureType());
    			try {
    				// Identify the columns from stored names-to-types for this user
    				userHandler.assignDataColumnTypes(dataset);
    				// Save and commit these column assignments in case the sanity checker has problems
    				dataHandler.saveDatasetInfoToFile(dataset, "Column types for " + datasetId + 
    						" updated by " + username + " from post-processing a multiple-dataset upload");
    			
    				// Run the automated data checker with the updated data types.  Saves the messages,
    				// and assigns the data check status and the WOCE-3 and WOCE-4 flags.
    				datasetChecker.standardizeDataset(dataset, null);
    
    				// Save and commit the updated dataset information
    				dataHandler.saveDatasetInfoToFile(dataset, "Automated data check status and flags for " + 
    						datasetId + " updated by " + username + " from post-processing a multiple-dataset upload");
    				dataSpecsLogger.info("Updated data column specs for " + datasetId + " for " + username);
    			} catch (Exception ex) {
    				// ignore problems (such as unidentified columns) - cruise will not have been updated
    				dataSpecsLogger.error("Unable to update data column specs for " + datasetId + ": " + ex.getMessage());
    				continue;
    			}
    		}
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error updating data columns", t);
        }
	}

	@Override
	public ADCMessageList getDataMessages(String pageUsername, 
			String datasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		// Get the list of saved automated data checker messages for this dataset
    		ADCMessageList scMsgList;
    		try {
    			scMsgList = configStore.getCheckerMsgHandler().getCheckerMessages(datasetId);
    		} catch (Exception ex) {
    			logger.info("No messages for dataset id " + datasetId);
    			scMsgList = new ADCMessageList();
    //			throw new IllegalArgumentException("The automated data checker has not been run on dataset " + datasetId);
    		}
    		scMsgList.setUsername(username);
    		logger.info("returned automated data checker messages for " + datasetId + " for " + username);
    		return scMsgList;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error retrieving data check messages", t);
        }
	}

	// XXX NOT USED
	// XXX TODO: OME_FILENAME check
//	@Override
//	public String getOmeXmlPath(String pageUsername, String datasetId, 
//			String previousId) throws IllegalArgumentException {
//		// Get the dashboard data store and current username, and validate that username
//		if ( ! validateRequest(pageUsername) ) 
//			throw new IllegalArgumentException("Invalid user request");
//		MetadataFileHandler metadataHandler = configStore.getMetadataFileHandler();
//
//		if ( ! previousId.isEmpty() ) {
//			// Read the OME XML contents for the previous dataset 
//			// XXX TODO: OME_FILENAME check
//			DashboardMetadata mdata = metadataHandler.getMetadataInfo(previousId, DashboardUtils.OME_FILENAME);
//			DashboardOmeMetadata updatedOmeMData = new DashboardOmeMetadata(mdata, metadataHandler);
//			// Reset the ID and related fields to that for this dataset
//			updatedOmeMData.changeDatasetID(datasetId);
//			// Read the OME XML contents currently saved for activeExpocode
//			mdata = metadataHandler.getMetadataInfo(datasetId, DashboardUtils.OME_FILENAME);
//			DashboardOmeMetadata origOmeMData = new DashboardOmeMetadata(mdata, metadataHandler);
//			// Create the merged OME and save the results
//			DashboardOmeMetadata mergedOmeMData = origOmeMData.mergeModifiable(updatedOmeMData);
//			metadataHandler.saveAsOmeXmlDoc(mergedOmeMData, "Merged OME of " + previousId + 
//															" into OME of " + datasetId);
//		}
//
//		// return the absolute path to the OME.xml for activeExpcode
//		File omeFile = metadataHandler.getMetadataFile(datasetId, DashboardUtils.OME_FILENAME);
//		return omeFile.getAbsolutePath();
//	}
	
	@Override
	public MetadataPreviewInfo getMetadataPreviewInfo(String pageUsername, String datasetId)
		throws NotFoundException, IllegalArgumentException {
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
		try {
			MetadataPreviewInfo preview = OADSMetadata.getMetadataPreviewInfo(pageUsername, datasetId);
			return preview;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error getting metadata preview information", t);
        }
	}

	@SuppressWarnings("resource")
    @Override
	public MetadataPreviewInfo sendMetadataInfo(String pageUsername, String datasetId)
    		throws NotFoundException, IllegalArgumentException {
        try {
    		HttpServletRequest request = getThreadLocalRequest();
            MetadataPreviewInfo mdInfo = MetadataPoster.postMetadata(request, pageUsername, datasetId); 
            return mdInfo;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error posting metadata to the MetadataEditor", t);
        }
	}
    
    /**
     * @param inStream
     * @return
     * @throws IOException 
     */
    private static String readFully(InputStream inStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
     
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();
             
        String text = new String(byteArray, StandardCharsets.UTF_8);
        return text;
    }

    @Override
	public PreviewPlotResponse buildPreviewImages(String pageUsername, String datasetId, 
												  String timetag, boolean force) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		logger.debug(datasetId+" @ " + timetag + " force:"+ force);
    		
    		boolean generatePlots = force || checkNeedToGeneratePlots(datasetId);
    		
    //		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
    		logger.debug("reading data for " + datasetId);
    
    		// Get the complete original cruise data
    		DashboardDataset dataset = configStore.getDataFileHandler().getDatasetFromInfoFile(datasetId);
            
    		// Generate the preview plots for this dataset
    		// TODO: refactor so starts this in a separate thread when firstCall is true and 
    		//       returns false, then when gets called again with firstCall is false for
    		//       a status update, returns false if still working and true if all plots are done
    		if ( generatePlots ) {
        		dataset = configStore.getDataFileHandler().getDatasetDataFromFiles(datasetId, 0, -1);
    			configStore.getPreviewPlotsHandler().createPreviewPlots((DashboardDatasetData)dataset, timetag);
    		}
    		List<PreviewTab> plots = configStore.getPreviewPlotsHandler().getPreviewPlots(datasetId, dataset.getFeatureType());
    		return new PreviewPlotResponse(plots, true);
        } catch (Throwable t) {
            logger.warn(t,t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error building preview images", t);
        }
	}

	private boolean checkNeedToGeneratePlots(final String datasetId) {
		PreviewPlotsHandler pph = configStore.getPreviewPlotsHandler();
		File plotsDir = pph.getDatasetPreviewPlotsDir(datasetId);
		if ( ! plotsDir.exists()) { 
			return true; 
		}
		File[] plots = plotsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(datasetId);
			}
		});
		if ( plots == null || plots.length < 10 ) { 
			return true; 
		}
		long plotsTime = plotsDir.lastModified();
		DataFileHandler dfh = configStore.getDataFileHandler();
		File dataFile = dfh.datasetDataFile(datasetId);
		long dataTime = dataFile.lastModified();
		if ( dataTime > plotsTime ) {
			return true;
		}
		File dataInfoFile = configStore.getDataFileHandler().datasetInfoFile(datasetId, true);
		long infoTime = dataInfoFile.lastModified();
		if ( infoTime > plotsTime ) {
			return true;
		}
		
		return false;
	}

	@Override
	public void submitDatasetsForQC(String pageUsername, HashSet<String> idsSet, String archiveStatus, 
			String timestamp, boolean repeatSend) throws IllegalArgumentException {
        logger.warn(pageUsername + "calling submitForQC");
        throw new IllegalStateException("SubmitForQC: Not implemented.");
//		// Get the dashboard data store and current username, and validate that username
//		if ( ! validateRequest(pageUsername) ) 
//			throw new IllegalArgumentException("Invalid user request");
//
//		// Submit the datasets for QC and possibly send to be archived
//		configStore.getDashboardDatasetSubmitter().submitDatasetsForQC(idsSet, 
//				archiveStatus, timestamp, repeatSend, username);
//		logger.info("datasets " + idsSet.toString() + 
//				" submitted by " + username);
	}

	@Override
	public void submitDatasetsToArchive(String pageUsername, List<String> datasetIds, List<String> columnsList, 
	                                    String archiveStatus, boolean repeatSend,
	                                    String submitMsg, boolean requestDOI) 
        throws IllegalArgumentException {
    	// Get the dashboard data store and current username, and validate that username
    	if ( ! validateRequest(pageUsername) ) 
    		throw new IllegalArgumentException("Invalid user request");
    
    	logger.info("archiving datasets " + datasetIds.toString() + 
    			" submitted by " + username);
    	
        try {
        	// Submit the datasets to Archive
        	configStore.getDashboardDatasetSubmitter().archiveDatasets(datasetIds, columnsList, submitMsg,
        															   requestDOI, archiveStatus, 
        															   repeatSend, username);
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error archiving dataset", t);
        }
    }
	
    @Override
    public String getPackageArchiveStatus(String pageUsername, String datasetId) 
        throws IllegalArgumentException {
    	if ( ! validateRequest(pageUsername) ) 
    		throw new IllegalArgumentException("Invalid user request");
        try {
            SubmissionRecord srec = Archive.getCurrentSubmissionRecordForPackage(datasetId);
            String statusHtml = buildStatusHtml(srec);
            return statusHtml;
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error retrieving archive submission status", t);
        }
    }
    
	/**
     * @param srec
     * @return
     */
    private static String buildStatusHtml(SubmissionRecord srec) {
        StringBuilder sb = new StringBuilder();
        if ( srec == null || srec.getStatusHistory() == null || srec.getStatusHistory().size() == 0 )  {
            sb.append("<ul><li>No status history for dataset.</li></ul>");
        } else {
            List<StatusRecord> history = srec.getStatusHistory();
            sb.append("<ul>");
            for (StatusRecord rec : history) {
                sb.append("<li>")
                  .append(htmlView(rec))
                  .append("</li>");
            }
            sb.append("</ul>");
        }
        return sb.toString();
    }

    /**
     * @param rec
     * @return
     */
    private static String htmlView(StatusRecord rec) {
        StringBuilder sb = new StringBuilder();
        sb.append(TimeUtils.formatUTC(rec.statusTime(), TimeUtils.non_std_ISO_8601_nofrac_noTz))
          .append(" : ")
          .append(rec.status().displayMsg());
        if ( ! StringUtils.emptyOrNull(rec.message())) {
            sb.append("<ul><li>").append(rec.message()).append("</li></ul>");
        }
        return sb.toString();
    }

    @Override
	public void suspendDatasets(String pageUsername, Set<String> idsSet, String timestamp) throws IllegalArgumentException {
		logger.debug("Suspending dataset ids " + idsSet);
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

        try {
    		for (String datasetId : idsSet) {
    			String message = "Suspending dataset " + datasetId;
    			DataFileHandler df = configStore.getDataFileHandler();
    			DashboardDataset ds = df.getDatasetFromInfoFile(datasetId);
    			ds.setSubmitStatus(DashboardUtils.STATUS_SUSPENDED);
    			df.saveDatasetInfoToFile(ds, message);
    		}
        } catch (Throwable t) {
            logger.warn(t, t);
            Notifications.Alert("SDIS Exception", t);
            throw new DashboardServiceException("Error suspending datasets", t);
        }
	}
}
