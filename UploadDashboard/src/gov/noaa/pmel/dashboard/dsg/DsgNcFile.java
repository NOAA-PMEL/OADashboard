package gov.noaa.pmel.dashboard.dsg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.datatype.CharDashDataType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;


public abstract class DsgNcFile extends File {

	private static final long serialVersionUID = -7695491814772713480L;

    protected static final Logger logger = LogManager.getLogger(DsgNcFile.class);
    
	protected static final String DSG_VERSION = "DsgNcFile 2.0";
	protected static final String TIME_ORIGIN_ATTRIBUTE = "01-JAN-1970 00:00:00";

	protected DsgMetadata metadata;
	protected StdDataArray stddata;

	public static enum DsgFileType {
		TRAJECTORY,
		PROFILE
	}
	
	public static DsgNcFile createTrajectoryFile(String filename) {
		return new TrajectoryDsgFile(filename);
	}
	public static DsgNcFile createTrajectoryFile(File parent, String filename) {
		return new TrajectoryDsgFile(parent,filename);
	}
//	public static DsgNcFile createProfileFile(String filename) {
//		return new ProfileDsgFile(filename);
//	}
	public static DsgNcFile createProfileFile(File parent, String filename) {
		return new ProfileDsgFile(parent,filename);
	}

	/**
	 * Creates this NetCDF DSG file with the given metadata and standardized user 
	 * provided data.  The internal metadata reference is updated to the given 
	 * DsgMetadata object and the internal data array reference is updated to a new 
	 * standardized data array object created from the appropriate user provided data. 
	 * Every data sample must have a valid longitude, latitude, sample depth, and 
	 * complete date and time specification, to at least the minute.  If the seconds 
	 * of the time is not provided, zero seconds will be used.
	 * 
	 * @param metaData
	 * 		metadata for the dataset
	 * @param userStdData
	 * 		standardized user-provided data
	 * @param dataFileTypes
	 * 		known data types for data files
	 * @throws IllegalArgumentException
	 * 		if any argument is null,
	 * 		if any of the data types in userStdData is {@link DashboardServerUtils#UNKNOWN}
	 * 		if any sample longitude, latitude, sample depth is missing,
	 * 		if any sample time cannot be computed
	 * @throws IOException
	 * 		if creating the NetCDF file throws one
	 * @throws InvalidRangeException
	 * 		if creating the NetCDF file throws one
	 * @throws IllegalAccessException
	 * 		if creating the NetCDF file throws one
	 */
	public void create(DsgMetadata metaData, StdUserDataArray userStdData, KnownDataTypes dataFileTypes) 
			throws Exception, IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		if ( metaData == null )
			throw new IllegalArgumentException("no metadata given");
		metadata = metaData;
		if ( userStdData == null )
			throw new IllegalArgumentException("no data given");

		// The following verifies lon, lat, depth, and time
		// Adds time and, if not already present, year, month, day, hour, minute, and second.
		stddata = userStdData; // new StdDataArray(userStdData, dataFileTypes);

		create(metadata, userStdData);
	}
	
	/**
	 * Creates this NetCDF DSG file with the given metadata and standardized data
	 * for data files.  The internal metadata and stddata references are updated 
	 * to the given DsgMetadata and StdDataArray object.  Every data sample should 
	 * have a valid longitude, latitude, sample depth, year, month of year, day of 
	 * month, hour of day, minute of hour, second of minute, time, sample number, 
	 * and WOCE autocheck value, although this is not fully verified.
	 * 
	 * @param metaData
	 * 		metadata for the dataset
	 * @param fileData
	 * 		standardized data appropriate for data files
	 * @throws IllegalArgumentException
	 * 		if any argument is null, or
	 * 		if there is no longitude, latitude, sample depth, year, month of year,
	 * 			day of month, hour of day, minute of hour, or second of minute, or 
	 * 			time data column
	 * @throws IOException
	 * 		if creating the NetCDF file throws one
	 * @throws InvalidRangeException
	 * 		if creating the NetCDF file throws one
	 * @throws IllegalAccessException
	 * 		if creating the NetCDF file throws one
	 */
	public abstract void create(DsgMetadata metaData, StdDataArray fileData) 
		throws Exception, IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException;
	
	/**
	 * See {@link java.io.File#File(java.lang.String)}
	 * The internal metadata and data array references are set null.
	 */
	protected DsgNcFile(String filename) {
		super(filename);
		metadata = null;
		stddata = null;
	}

	/**
	 * See {@link java.io.File#File(java.io.File,java.lang.String)}
	 * The internal metadata and data array references are set null.
	 */
	protected DsgNcFile(File parent, String child) {
		super(parent, child);
		metadata = null;
		stddata = null;
	}

	/**
	 * Adds the missing_value, _FillValue, long_name, standard_name, ioos_category, 
	 * and units attributes to the given variables in the given NetCDF file.
	 * 
	 * @param ncfile
	 * 		NetCDF file being written containing the variable
	 * @param var
	 * 		the variables to add attributes to
	 * @param missVal
	 * 		if not null, the value for the missing_value and _FillValue attributes
	 * @param longName
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the long_name attribute
	 * @param standardName
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the standard_name attribute
	 * @param ioosCategory
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the ioos_category attribute
	 * @param units
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the units attribute
	 */
	protected static void addAttributes(NetcdfFileWriter ncfile, Variable var, Object missVal, 
			String longName, String standardName, String ioosCategory, String units) {
        if ( var == null ) {
            logger.warn("Add attributes: No ncVar found for varName " + longName);
            return;
        }
		if ( missVal != null ) {
			ncfile.addVariableAttribute(var, new Attribute("missing_value", String.valueOf(missVal)));
			ncfile.addVariableAttribute(var, new Attribute("_FillValue", String.valueOf(missVal)));
		}
		if ( (longName != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(longName) ) {
			ncfile.addVariableAttribute(var, new Attribute("long_name", longName));
		}
		if ( (standardName != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(standardName) ) {
			ncfile.addVariableAttribute(var, new Attribute("standard_name", standardName));
		}
		if ( (ioosCategory != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(ioosCategory) ) {
			ncfile.addVariableAttribute(var, new Attribute("ioos_category", ioosCategory));
		}
		if ( (units != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(units) ) {
			ncfile.addVariableAttribute(var, new Attribute("units", units));
		}
	}
	protected void checkIndeces(StdDataArray stddata) {
		// Quick check of data column indices already assigned in StdDataArray
		if ( ! stddata.hasLongitude() )
			throw new IllegalArgumentException("no longitude data column");
		if ( ! stddata.hasLatitude() )
			throw new IllegalArgumentException("no latitude data column");
		if ( ! ( stddata.hasSampleDepth() || stddata.hasSamplePressure() ))
			throw new IllegalArgumentException("no sample depth data column");
		if ( ! stddata.hasYear() )
			throw new IllegalArgumentException("no year data column");
		if ( ! stddata.hasMonthOfYear() )
			throw new IllegalArgumentException("no month of year data column");
		if ( ! stddata.hasDayOfMonth() )
			throw new IllegalArgumentException("no day of month data column");
		if ( ! stddata.hasHourOfDay() )
			throw new IllegalArgumentException("no hour of day data column");
		if ( ! stddata.hasMinuteOfHour() )
			throw new IllegalArgumentException("no minute of hour data column");
		if ( ! stddata.hasSecondOfMinute() )
			throw new IllegalArgumentException("no second of minute data column");
	}
	
	protected Variable addVariable(NetcdfFileWriter ncfile, String varName, DataType dtype, List<Dimension> dataDims) {
		return addVariable(ncfile, null, varName, dtype, dataDims);
	}
	protected Variable addVariable(NetcdfFileWriter ncfile, Group group, String varName, DataType dtype, List<Dimension> dataDims) {
		Variable var = ncfile.addVariable(group, varName, dtype, dataDims);
		if ( var == null ) { 
			String msg = "Failed to add NetCDF Variable for " + varName +".";
			var = ncfile.findVariable(varName);
			if ( var != null ) {
				msg += " Variable already exists with dimensions " + var.getDimensions();
			}
			throw new IllegalStateException(msg);
		}
		return var;
	}
	
	/**
	 * Creates and assigns the internal metadata 
	 * reference from the contents of this netCDF DSG file.
	 * 
	 * @param metadataTypes
	 * 		metadata file types to read
	 * @return
	 * 		variable names of the metadata fields not assigned from 
	 * 		this netCDF file (will have its default/missing value)
	 * @throws IllegalArgumentException
	 * 		if there are no metadata types given, or
	 * 		if an invalid type for metadata is encountered
	 * @throws IOException
	 * 		if there are problems opening or reading from the netCDF file
	 */
	public ArrayList<String> readMetadata(KnownDataTypes metadataTypes) 
			throws IllegalArgumentException, IOException{
		if ( (metadataTypes == null) || metadataTypes.isEmpty() )
			throw new IllegalArgumentException("no metadata file types given");
		ArrayList<String> namesNotFound = new ArrayList<String>();
		
		try ( NetcdfFile ncfile = NetcdfFile.open(getPath()); ) {
			// Create the metadata with default (missing) values
			metadata = new DsgMetadata(metadataTypes);

			for ( DashDataType<?> dtype : metadataTypes.getKnownTypesSet() ) {
				String varName = dtype.getVarName();
				Variable var = ncfile.findVariable(varName);
				if ( var == null ) {
					namesNotFound.add(varName);
					continue;
				}
				if ( var.getShape(0) != 1 ) 
					throw new IOException("more than one value for a metadata type");
				if ( dtype instanceof StringDashDataType ) {
					ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
					String strval = mvar.getString(0);
					if ( ! DashboardUtils.STRING_MISSING_VALUE.equals(strval) )
						metadata.setValue(dtype, strval);
				}
				else if ( dtype instanceof CharDashDataType ) {
					if ( var.getShape(1) != 1 )
						throw new IOException("more than one character for a character type");
					ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
					Character charval = mvar.get(0, 0);
					if ( ! DashboardUtils.CHAR_MISSING_VALUE.equals(charval) )
						metadata.setValue(dtype, charval);
				}
				else if ( dtype instanceof IntDashDataType ) {
					ArrayInt.D1 mvar = (ArrayInt.D1) var.read();
					Integer intval = mvar.getInt(0);
					if ( ! DashboardUtils.INT_MISSING_VALUE.equals(intval) )
						metadata.setValue(dtype, intval);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					ArrayDouble.D1 mvar = (ArrayDouble.D1) var.read();
					Double dblval = mvar.getDouble(0);
					if ( ! DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval, 
							DashboardUtils.MAX_RELATIVE_ERROR, DashboardUtils.MAX_ABSOLUTE_ERROR) )
						metadata.setValue(dtype, dblval);
				}
				else {
					throw new IllegalArgumentException("invalid metadata file type " + dtype.getVarName());
				}
			}
		}
		return namesNotFound;
	}

	/**
	 * Creates and assigns the internal standard data array 
	 * reference from the contents of this netCDF DSG file.
	 * 
	 * @param dataTypes
	 * 		data files types to read
	 * @return
	 * 		variable names of the data types not assigned from 
	 * 		this netCDF file (will have its default/missing value)
	 * @throws IllegalArgumentException
	 * 		if no known data types are given, or
	 * 		if an invalid type for data files is encountered
	 * @throws IOException
	 * 		if the netCDF file is invalid: it must have a 'time' 
	 * 		variable and all data variables must have the same
	 * 		number of values as the 'time' variable, or
	 * 		if there are problems opening or reading from the netCDF file
	 */
	public ArrayList<String> readData(KnownDataTypes dataTypes) 
			throws IllegalArgumentException, IOException {
		if ( (dataTypes == null) || dataTypes.isEmpty() )
			throw new IllegalArgumentException("no data file types given");
		int numColumns;
		DashDataType<?>[] dataTypesArray;
		{
			TreeSet<DashDataType<?>> dataTypesSet = dataTypes.getKnownTypesSet();
			numColumns = dataTypesSet.size();
			dataTypesArray = new DashDataType<?>[numColumns];
			int idx = -1;
			for ( DashDataType<?> dtype : dataTypesSet ) {
				idx++;
				dataTypesArray[idx] = dtype;
			}
		}

		ArrayList<String> namesNotFound = new ArrayList<String>();
		
		try ( NetcdfFile ncfile = NetcdfFile.open(getPath()); ) {
			// Get the number of samples from the length of the time 1D array
			String varName = DashboardServerUtils.TIME.getVarName();
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IOException("unable to find variable 'time' in " + getName());
			int numSamples = var.getShape(0);

			// Create the array of data values
			Object[][] dataArray = new Object[numSamples][numColumns];

			for (int k = 0; k < numColumns; k++) {
				DashDataType<?> dtype = dataTypesArray[k];
				varName = dtype.getVarName();
				var = ncfile.findVariable(varName);
				if ( var == null ) {
					namesNotFound.add(varName);
					for (int j = 0; j < numSamples; j++)
						dataArray[j][k] = null;
					continue;
				}

				if ( var.getShape(0) != numSamples )
					throw new IOException("number of values for '" + varName + 
							"' (" + Integer.toString(var.getShape(0)) + ") does not match " +
							"the number of values for 'time' (" + Integer.toString(numSamples) + ")");

				if ( dtype instanceof CharDashDataType ) {
					if ( var.getShape(1) != 1 )
						throw new IOException("more than one character for a character type");
					ArrayChar.D2 dvar = (ArrayChar.D2) var.read();
					for (int j = 0; j < numSamples; j++) {
						Character charval = dvar.get(j,0);
						if ( DashboardUtils.CHAR_MISSING_VALUE.equals(charval) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = charval;
					}
				}
				else if ( dtype instanceof IntDashDataType ) {
					ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
					for (int j = 0; j < numSamples; j++) {
						Integer intval = dvar.get(j);
						if ( DashboardUtils.INT_MISSING_VALUE.equals(intval) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = intval;
					}
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
					for (int j = 0; j < numSamples; j++) {
						Double dblval = dvar.get(j);
						if ( DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval, 
								DashboardUtils.MAX_RELATIVE_ERROR, DashboardUtils.MAX_ABSOLUTE_ERROR) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = dblval;
					}
				}
				else {
					throw new IllegalArgumentException("invalid data file type " + dtype.toString());
				}
			}
			stddata = new StdDataArray(dataTypesArray, dataArray);
		}
		return namesNotFound;
	}

	/**
	 * @return
	 * 		the internal metadata reference; may be null
	 */
	public DsgMetadata getMetadata() {
		return metadata;
	}

	/**
	 * @return
	 * 		the internal standard data array reference; may be null
	 */
	public StdDataArray getStdDataArray() {
		return stddata;
	}

	/**
	 * Reads and returns the array of data values for the specified variable 
	 * contained in this DSG file.  The variable must be saved in the DSG 
	 * file as characters.  For some variables, this DSG file must have been 
	 * processed by Ferret for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid, or
	 * 		if the variable is not a single-character array variable
	 */
	public char[] readCharVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		char[] dataVals;
		
		try ( NetcdfFile ncfile = NetcdfFile.open(getPath()); ) {
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 cvar = (ArrayChar.D2) var.read();
			if ( var.getShape(1) != 1 ) 
				throw new IllegalArgumentException("Variable '" + varName + 
						"' is not a single-character array variable in " + getName());
			int numVals = var.getShape(0);
			dataVals = new char[numVals];
			for (int k = 0; k < numVals; k++)
				dataVals[k] = cvar.get(k,0);
		}
		return dataVals;
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in this DSG file.  The variable must be saved in the DSG file
	 * as integers.  For some variables, this DSG file must have been processed 
	 * by Ferret for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public int[] readIntVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		int[] dataVals;
		
		try ( NetcdfFile ncfile = NetcdfFile.open(getPath()); ) {
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
			int numVals = var.getShape(0);
			dataVals = new int[numVals];
			for (int k = 0; k < numVals; k++)
				dataVals[k] = dvar.get(k);
		}
		return dataVals;
	}

	/**
	 * Reads and returns the array of data values for the specified variable contained 
	 * in this DSG file.  The variable must be saved in the DSG file as doubles.  
	 * NaN and infinite values are changed to {@link DsgData#FP_MISSING_VALUE}.  
	 * For some variables, this DSG file must have been processed by Ferret for the data 
	 * values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public double[] readDoubleVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		double[] dataVals;
		
		try ( NetcdfFile ncfile = NetcdfFile.open(getPath()); ) {
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
			int numVals = var.getShape(0);
			dataVals = new double[numVals];
			for (int k = 0; k < numVals; k++) {
				double value = dvar.get(k);
				if ( Double.isNaN(value) || Double.isInfinite(value) )
					value = DashboardUtils.FP_MISSING_VALUE;
				dataVals[k] = value;
			}
		}
		return dataVals;
	}

	/**
	 * Updates the string recorded for the given variable in this DSG file.
	 * 
	 * @param varName
	 * 		name of the variable in this DSG file
	 * @param newValue
	 * 		new string value to record in this DSG file
	 * @throws IllegalArgumentException
	 * 		if this DSG file is not valid
	 * @throws IOException
	 * 		if opening or updating this DSG file throws one
	 * @throws InvalidRangeException 
	 * 		if writing the updated string to this DSG file throws one 
	 * 		or if the updated string is too long for this DSG file
	 */
	public void updateStringVarValue(String varName, String newValue) 
		throws IllegalArgumentException, IOException, InvalidRangeException {
		
		try ( NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath()); ) {
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			int varLen = var.getShape(1);
			if ( newValue.length() > varLen )
				throw new InvalidRangeException("Length of new string (" + 
						newValue.length() + ") exceeds available space (" + 
						varLen + ")");
			ArrayChar.D2 valArray = new ArrayChar.D2(1, varLen);
			valArray.setString(0, newValue);
			ncfile.write(var, valArray);
		}
	}

	/**
	 * Writes the given array of characters as the values 
	 * for the given character data variable.
	 * 
	 * @param varName
	 * 		character data variable name
	 * @param values
	 * 		character values to assign
	 * @throws IOException
	 * 		if reading from or writing to the file throws one
	 * @throws IllegalArgumentException
	 * 		if the variable name or number of provided values
	 * 		is invalid
	 */
	public void writeCharVarDataValues(String varName, char[] values) 
								throws IOException, IllegalArgumentException {
		
		try ( NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath()); ) {
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			if ( var.getShape(1) != 1 ) 
				throw new IllegalArgumentException("Variable '" + varName + 
						"' is not a single-character array variable in " + getName());
			int numVals = var.getShape(0);
			if ( numVals != values.length )
				throw new IllegalArgumentException("Inconstistent number of variables for '" + 
						varName + "' (" + Integer.toString(numVals) + 
						") and provided data (" + Integer.toString(values.length) + ")");
			ArrayChar.D2 dvar = new ArrayChar.D2(numVals, 1);
			for (int k = 0; k < numVals; k++) {
				dvar.set(k, 0, values[k]);
			}
			try {
				ncfile.write(var, dvar);
			} catch (InvalidRangeException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}

}
