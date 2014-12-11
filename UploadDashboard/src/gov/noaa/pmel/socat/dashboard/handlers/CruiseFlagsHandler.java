/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.handlers;

import gov.noaa.pmel.socat.dashboard.server.DashboardDataStore;
import gov.noaa.pmel.socat.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.socat.dashboard.server.RowNumSet;
import gov.noaa.pmel.socat.dashboard.shared.DataLocation;
import gov.noaa.pmel.socat.dashboard.shared.SocatWoceEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

/**
 * @author Karl Smith
 */
public class CruiseFlagsHandler {

	private static final String FLAG_MSGS_FILENAME_SUFFIX = "_WOCE_flags.tsv";

	private static final SimpleDateFormat timestamper = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

	private File filesDir;

	/**
	 * Handler for cruise flag messages.  At this time, just WOCE flags.
	 * 
	 * @param filesDirName
	 * 		save user-readable WOCE flag messages in files under this directory
	 */
	public CruiseFlagsHandler(String filesDirName) {
		filesDir = new File(filesDirName);
		if ( ! filesDir.isDirectory() )
			throw new IllegalArgumentException(filesDirName + " is not a directory");
	}

	/**
	 * @param expocode
	 * 		expocode of the cruise
	 * @return
	 * 		the file of user-readable WOCE flag messages associated with the cruise
	 * @throws IllegalArgumentException
	 * 		if the cruise expocode is invalid
	 */
	private File cruiseFlagMsgsFile(String expocode) throws IllegalArgumentException {
		// Check that the expocode is somewhat reasonable
		String upperExpo = DashboardServerUtils.checkExpocode(expocode);
		// Get the name of the cruise messages file
		return new File(filesDir, upperExpo.substring(0,4) + 
				File.separatorChar + upperExpo + FLAG_MSGS_FILENAME_SUFFIX);
	}

	/**
	 * Create the WOCE flags messages file from the WOCE flags in the database.
	 * 
	 * @param expocode
	 * 		create the WOCE flags messages file for this cruise
	 * @param dbHandler
	 * 		get the WOCE flags from the database using this handler
	 * @throws IllegalArgumentException
	 * 		if the expocode is invalid
	 * @throws SQLException
	 * 		if there are problems getting WOCE flags from the database
	 */
	public void generateWoceFlagMsgsFile(String expocode, DatabaseRequestHandler dbHandler) 
											throws IllegalArgumentException, SQLException {
		File msgsFile = cruiseFlagMsgsFile(expocode);
		PrintWriter msgsWriter;
		try {
			msgsWriter = new PrintWriter(msgsFile);
		} catch (FileNotFoundException ex) {
			throw new IllegalArgumentException(
					"Unexpected error opening WOCE flag messages file " + 
					msgsFile.getPath() + "\n    " + ex.getMessage(), ex);
		}
		try {
			RowNumSet rowNums = new RowNumSet();
			// Get the current WOCE flags for this cruise and print them to file
			msgsWriter.println("Expocode: " + expocode);
			msgsWriter.println("WOCE-3 and WOCE-4 flags as of: " + timestamper.format(new Date()));
			msgsWriter.println("Flag\tCol. Name\tNum. Rows\tMessage\tRows");
			ArrayList<SocatWoceEvent> woceEventsList = dbHandler.getWoceEvents(expocode);
			for ( SocatWoceEvent woceEvent : woceEventsList ) {
				// Only report '3' and '4' - skip 'Q' and 'B' which are for old versions
				Character woceFlag = woceEvent.getFlag();
				if ( ! (woceFlag.equals('3') || woceFlag.equals('4')) )
					continue;
				rowNums.clear();
				for ( DataLocation dloc : woceEvent.getLocations() )
					rowNums.add(dloc.getRowNumber());
				msgsWriter.print(woceFlag);
				msgsWriter.print('\t');
				String dataColName = woceEvent.getDataVarName();
				if ( dataColName.trim().isEmpty() )
					dataColName = "(none)";
				msgsWriter.print(dataColName);
				msgsWriter.print('\t');
				msgsWriter.print(rowNums.size());
				msgsWriter.print('\t');
				msgsWriter.print(woceEvent.getComment().replaceAll("\n", "  ").replaceAll("\t", " "));
				msgsWriter.print('\t');
				msgsWriter.print(rowNums.toString());
				msgsWriter.println();
			}			
		} finally {
			msgsWriter.close();
		}
	}

	/**
	 * @param args
	 * 		ExpocodesFile
	 * 
	 * where:
	 * 
	 * ExpocodesFile is a file containing expocodes of the cruises to report
	 */
	public static void main(String[] args) {
		if ( args.length != 1 ) {
			System.err.println("Arguments:  ExpocodesFile");
			System.err.println();
			System.err.println("ExpocodesFile");
			System.err.println("    is a file containing expocodes, one per line, of cruises for which ");
			System.err.println("    to generate WOCE flags report filea from the current WOCE flags in ");
			System.err.println("    the database. ");
			System.err.println();
			System.exit(1);
		}
		String exposFilename = args[0];

		TreeSet<String> expocodes = new TreeSet<String>();
		try {
			BufferedReader reader = 
					new BufferedReader(new FileReader(exposFilename));
			try {
				String dataline = reader.readLine();
				while ( dataline != null ) {
					dataline = dataline.trim().toUpperCase();
					if ( ! dataline.isEmpty() )
						expocodes.add(dataline);
					dataline = reader.readLine();
				}
			} finally {
				reader.close();
			}
		} catch (Exception ex) {
			System.err.println("Problems reading the file of expocodes '" + 
					exposFilename + "': " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}

		DashboardDataStore dataStore = null;
		try {
			dataStore = DashboardDataStore.get();
		} catch (Exception ex) {
			System.err.println("Problems obtaining the default dashboard " +
							   "configuration: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
		DatabaseRequestHandler dbHandler = dataStore.getDatabaseRequestHandler();
		CruiseFlagsHandler flagsHandler = dataStore.getCruiseFlagsHandler();

		int retVal = 0;
		for ( String expo : expocodes ) {
			// Generate the WOCE flags report file from the summary messages and current WOCE flags
			try {
				flagsHandler.generateWoceFlagMsgsFile(expo, dbHandler);
			} catch ( Exception ex ) {
				System.err.println("Error - " + expo + " - problems getting WOCE flags");
				retVal = 1;
				continue;
			}
			System.err.println("Success - " + expo);
		}
		// Done - return zero if no problems
		System.exit(retVal);
	}

}
