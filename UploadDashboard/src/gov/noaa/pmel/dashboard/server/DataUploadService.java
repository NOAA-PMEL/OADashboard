/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import gov.noaa.pmel.dashboard.server.util.FileTypeTest;
import gov.noaa.pmel.dashboard.server.util.UIDGen;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.shared.ObservationType;
import gov.noaa.pmel.dashboard.upload.RecordOrientedFileReader;
import gov.noaa.pmel.dashboard.upload.StandardUploadFields;
import gov.noaa.pmel.dashboard.upload.UploadProcessor;
import gov.noaa.pmel.dashboard.util.FormUtils;
import gov.noaa.pmel.oads.util.StringUtils;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;


/**
 * Service to receive the uploaded cruise file from the client
 * 
 * @author Karl Smith
 */
public class DataUploadService extends HttpServlet { 
	private static final long serialVersionUID = 1547524322159252520L;

    private static Logger logger = LogManager.getLogger(DataUploadService.class);
    
    public static int DEFAULT_MAX_ALLOWED_UPLOAD_SIZE = 102400000;
    public static String DEFAULT_MAX_ALLOWED_SIZE_DISPLAY_STR = "~100MB";
    public static int MAX_ALLOWED_UPLOAD_SIZE = ApplicationConfiguration.getProperty("oap.upload.max_size", DEFAULT_MAX_ALLOWED_UPLOAD_SIZE);
    public static String MAX_ALLOWED_SIZE_DISPLAY_STR = ApplicationConfiguration.getProperty("oap.upload.max_size.display", DEFAULT_MAX_ALLOWED_SIZE_DISPLAY_STR);

	private ServletFileUpload datafileUpload;

    public static class BadRequestException extends Exception {
        private static final long serialVersionUID = 529541444284268094L;
        public BadRequestException() { super(); }
        public BadRequestException(String message) { super(message); }
        public BadRequestException(Throwable cause) { super(cause); }
        public BadRequestException(String message, Throwable cause) { super(message, cause); }
    }
        
	public DataUploadService() {
		
		DiskFileItemFactory factory = new DiskFileItemFactory();
		try {
			File servletTmpDir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(servletTmpDir);
		} catch (Exception ex) {
            // factory will use default temp dir
            logger.debug(ex);
		}
		datafileUpload = new ServletFileUpload(factory);
	}

    @Override 
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug(request);
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    
    private static String getUploadField(String fieldName, Map<String,List<FileItem>> paramMap) {
        return FormUtils.getFormField(fieldName, paramMap, false);
    }
    
    private static String getRequiredField(String fieldName, Map<String,List<FileItem>> paramMap) throws NoSuchFieldException {
        return FormUtils.getRequiredFormField(fieldName, paramMap, false);
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // log request
        logger.info(request);
	    
        StandardUploadFields stdFields;
		Map<String,List<FileItem>> paramMap = null;
        
        try {
            String username = getUsername(request);
            logger.info("Processing upload for user " + username);
            
    	    // verify message
            checkRequestMime(request);
            
    	    // extract meta info
            paramMap = extractParameterMap(request);
            stdFields = extractStandardFields(paramMap);
            stdFields.username(username);
            
            // XXX should really move this to a different service
            if ( isPreviewRequest(getUploadField("dataaction", paramMap))) {
                sendPreview(stdFields, response);
                return;
            }
            
            String requestPathInfo = request.getPathInfo();
            boolean isUpdateRequest = isUpdateRequest(requestPathInfo);
            
            String submissionRecordId = isUpdateRequest ? getUpdateRecordId(requestPathInfo) : UIDGen.genId();
            
            UploadProcessor uploadProcessor = new UploadProcessor(stdFields);
            uploadProcessor.processUpload(submissionRecordId, isUpdateRequest);
            List<String>messages = uploadProcessor.getMessages();
            Set<String>successes = uploadProcessor.getSuccesses();
            sendResponseMsg(response, successes, messages);
        } catch (BadRequestException vex) { // baseRequestValidation
            logger.warn(vex);
            sendErrMsg(response, vex);
        } catch (FileUploadException fex) { // parseParameterMap
            logger.warn(fex);
            sendErrMsg(response, fex);
        } catch (IllegalStateException iex) { // no username
            logger.warn(iex);
            sendErrMsg(response, iex);
        } catch (NoSuchFieldException nsf) { // missing field
            logger.warn(nsf);
            sendErrMsg(response, nsf);
        } catch (IllegalArgumentException iex) { // no files
            logger.warn(iex);
            sendErrMsg(response, iex);
        } catch (Throwable ex) {
            logger.warn(ex, ex);
//            StackTraceElement[] trace = ex.getStackTrace();
//            StackTraceElement spot = trace != null && trace.length > 0 ?
//                                        trace[0] :
//                                        null;    
            StringBuilder msg = 
                new StringBuilder("There was an error processing your upload.  Please try again later.\n")
                        .append("If the problem persists, please notify the system administrator at pmel.sdis@noaa.gov")
;//                    .append("\nError:")
//                    .append(ex.getClass().getName());
//            if ( spot != null ) {
//                msg.append(":")
//                   .append(spot.getFileName()).append(":").append(spot.getLineNumber());
//            }
            sendErrMsg(response, msg.toString());
        } finally {
            if ( paramMap != null ) {
    			for ( Entry<String,List<FileItem>> paramEntry : paramMap.entrySet() ) {
    				for ( FileItem item : paramEntry.getValue() ) {
    					item.delete();
    				}
    			}
            }
        }
	}
    
    /**
     * @param pathInfo
     * @return
     */
    private static boolean isUpdateRequest(String pathInfo) {
        return !StringUtils.emptyOrNull(pathInfo) && pathInfo.contains("update");
    }
    
    private static String getUpdateRecordId(String pathInfo) {
        String checkString = pathInfo;
        if ( checkString.endsWith("/")) { checkString = checkString.substring(0, checkString.length()-1); }
        String recId = pathInfo.substring(pathInfo.lastIndexOf("/")+1);
        return recId;
    }

    private Map<String, List<FileItem>> extractParameterMap(HttpServletRequest request) throws FileUploadException {
        Map<String,List<FileItem>> paramMap = datafileUpload.parseParameterMap(request);
        return paramMap;
    }
    private static StandardUploadFields extractStandardFields(Map<String,List<FileItem>> paramMap) 
            throws FileUploadException, NoSuchFieldException {
        StandardUploadFields sup = null;
        try {
            sup = StandardUploadFields.builder()
                    .parameterMap(paramMap)
                    .datasetId(getUploadField("datasetId", paramMap))
                    .datasetIdColumnName(getUploadField("datasetIdColumn", paramMap))
                    .dataAction(getRequiredField("dataaction", paramMap))
                    .fileDataEncoding(getUploadField("dataencoding", paramMap))
                    .timestamp(getUploadField("timestamp", paramMap)) // DashboardServerUtils.formatUTC(new Date()))
                    .observationType(getUploadField("observationType", paramMap))
                    .featureType(getFeatureType(paramMap))
                    .fileType(getFileType(paramMap))
                    .dataFiles(extractDataFiles(paramMap))
                    .build();
        } catch (NullPointerException nex) {
            nex.printStackTrace();
            throw new NoSuchFieldException(nex.getMessage());
        }
        return sup;
    }

    private static List<FileItem> extractDataFiles(Map<String, List<FileItem>> paramMap) throws IllegalArgumentException {
        List<FileItem> datafiles = paramMap.get("dataUpload");
        if ( datafiles == null || datafiles.isEmpty()) {
            throw new IllegalArgumentException("No upload files found.");
        }
        for ( FileItem item : datafiles ) {
            if ( item.getSize() > MAX_ALLOWED_UPLOAD_SIZE ) {
                throw new IllegalArgumentException("File size exceeds max allowable size of " + MAX_ALLOWED_SIZE_DISPLAY_STR );
            }
        }
        return datafiles;
    }

    private static String getUsername(HttpServletRequest request) {
        try {
            String username = request.getUserPrincipal().getName();
            return username;
        } catch (Exception ex) {
            logger.info(ex);
            throw new IllegalStateException("No user found.");
        }
    }

    private static FeatureType getFeatureType(Map<String, List<FileItem>> paramMap) throws NoSuchFieldException {
        String observationTypeName = getUploadField("observationType", paramMap);
        FeatureType featureType = ObservationType.featureTypeOf(observationTypeName);
        return featureType;
    }
    
    private static FileType getFileType(Map<String, List<FileItem>> paramMap) throws NoSuchFieldException {
        FileType fileType = null;
        String fileTypeName = getUploadField("fileType", paramMap);
        try { 
            if ( ! StringUtils.emptyOrNull(fileTypeName)) {
                fileType = FileType.valueOf(fileTypeName);
            }
        } catch (Exception ex) {
            logger.warn("Unknown file type specified: " + fileTypeName);
            fileType = FileType.UNKNOWN;
        }
        return fileType;
    }

    private static void checkRequestMime(HttpServletRequest request) throws BadRequestException {
		if ( ! ServletFileUpload.isMultipartContent(request) ) {
            throw new BadRequestException("Invalid request contents format for this service.");
		}
    }

	private static boolean isPreviewRequest(String action) {
		return DashboardUtils.PREVIEW_REQUEST_TAG.equals(action);
    }

    private static void sendPreview(StandardUploadFields stdFields, HttpServletResponse response) throws Exception  {
        FileItem firstItem = stdFields.dataFiles().iterator().next();
        String filename = firstItem.getName();
        String encoding = stdFields.fileDataEncoding(); 
		ArrayList<String> contentsList = new ArrayList<String>(50);
        try (BufferedInputStream inStream = new BufferedInputStream(firstItem.getInputStream()); ) {
            String fileType = FileTypeTest.getFileType(inStream);
            if ( FileTypeTest.fileIsDelimited(fileType)) {
                RecordOrientedFileReader reader = RecordOrientedFileReader.getFileReader(fileType, inStream);
                Iterator<String[]> iterator = reader.iterator();
                
    			for (int k = 0; k < 50 && iterator.hasNext(); k++) {
    				String[] linedata = iterator.next();
    				contentsList.add(reader.reconstitute(linedata));
                }
            } else {
        		BufferedReader cruiseReader = new BufferedReader(new InputStreamReader(inStream, encoding));
    			for (int k = 0; k < 50; k++) {
    				String dataline = cruiseReader.readLine();
    				if ( dataline == null )
    					break;
    				contentsList.add(dataline);
    			}
			}
		} finally {
		    firstItem.delete();
		}

		// Respond with some info and the interpreted contents
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
        try ( PrintWriter respWriter = response.getWriter(); ) {
			respWriter.println(DashboardUtils.FILE_PREVIEW_HEADER_TAG);
			respWriter.println("----------------------------------------");
			respWriter.println("Filename: " + filename);
			respWriter.println("Encoding: " + encoding);
			respWriter.println("(Partial) Contents:");
			respWriter.println("----------------------------------------");
			for ( String dataline : contentsList )
				respWriter.println(dataline);
			response.flushBuffer();
			return;
        }
    }

    /**
     * @param response
     * @param messages
     * @throws IOException 
     */
    private void sendResponseMsg(HttpServletResponse response, Set<String> successes, List<String> messages) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		try ( PrintWriter respWriter = response.getWriter(); ) {
            for (String msg :  messages ) {
                respWriter.println(msg);
    		}
    		response.flushBuffer();
		}
    }

	private static void sendOkMsg(HttpServletResponse response, Set<String> datasets) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		try ( PrintWriter respWriter = response.getWriter(); ) {
            respWriter.write(DashboardUtils.SUCCESS_HEADER_TAG);
            String comma = "";
    		for ( String dsid : datasets ) {
    			respWriter.println(dsid + comma);
    			comma = ",";
    		}
    		response.flushBuffer();
		}
    }

    /**
	 * Returns an error message in the given Response object.  
	 * The response number is still 200 (SC_OK) so the message 
	 * goes through cleanly.
	 * 
	 * @param response
	 * 		write the error message here
	 * @param errMsg
	 * 		error message to return
	 */
	private static void sendErrMsg(HttpServletResponse response, String errMsg) {
        try {
    		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    		response.setContentType("text/html;charset=UTF-8");
            try ( PrintWriter respWriter = response.getWriter(); ) {
        		respWriter.println(errMsg);
        		response.flushBuffer();
            }
        } catch (IOException iox) {
            logger.warn(iox);
        }
	}

	private static void sendErrMsg(HttpServletResponse response, Exception ex) {
        try {
            ex.printStackTrace();
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.setContentType("text/html;charset=UTF-8");
            try ( PrintWriter respWriter = response.getWriter(); ) {
                respWriter.println("There was an error on the server:");
        		respWriter.println(ex.getMessage());
        		response.flushBuffer();
            }
        } catch (IOException iox) {
            logger.warn(iox);
        }
	}

}
