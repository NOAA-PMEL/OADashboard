package uk.ac.uea.socat.sanitychecker;

import java.util.Enumeration;
import java.util.Properties;

public class Message {

	public static final int DATA_MESSAGE = 1;
	
	public static final int METADATA_MESSAGE = 2;

	public static final int WARNING = 0;
	
	public static final int ERROR = 1;
	
	public static final String NAME_PROPERTY = "name";
	
	public static final String MIN_PROPERTY = "min";
	
	public static final String MAX_PROPERTY = "max";
	
	protected int itsMessageType;
	
	protected int itsLine;
	
	private int itsSeverity;
	
	protected String itsMessage;
	
	private Properties itsProperties;

	private int itsInputItemIndex;
	
	private String itsInputItemName;
	
	private int itsItemIndex;
	
	private String itsItemName;
	
	public Message(int type, int severity, int line, String message) {
		itsMessageType = type;
		itsSeverity = severity;
		itsLine = line;
		itsMessage = message;
		itsProperties = new Properties();
		
		itsInputItemIndex = -1;
		itsInputItemName = null;
		itsItemIndex = -1;
		itsItemName = null;
	}
	
	public Message(int type, int severity, int line, int inputItemIndex, String inputItemName, int itemIndex, String itemName, String message) {
		itsMessageType = type;
		itsSeverity = severity;
		itsLine = line;
		itsMessage = message;
		itsProperties = new Properties();
		
		itsInputItemIndex = inputItemIndex;
		itsInputItemName = inputItemName;

		itsItemIndex = itemIndex;
		itsItemName = itemName;
	}
	
	public Message(int type, int severity, int line, String name, String message) {
		itsMessageType = type;
		itsSeverity = severity;
		itsLine = line;
		itsMessage = message;
		itsProperties = new Properties();
		
		itsInputItemIndex = -1;
		itsInputItemName = null;
		itsItemIndex = -1;
		itsItemName = name;
	}
	
	public void addProperty(String name, String value) {
		itsProperties.setProperty(name, value);
	}
	
	public int getSeverity() {
		return itsSeverity;
	}
	
	public boolean isError() {
		return itsSeverity == ERROR;
	}
	
	public boolean isWarning() {
		return itsSeverity == WARNING;
	}

	public int getMessageType() {
		return itsMessageType;
	}

	public int getLineIndex() {
		return itsLine;
	}

	public int getItemIndex() {
		return itsItemIndex;
	}
	
	public String getItemName() {
		return itsItemName;
	}
	
	public int getInputItemIndex() {
		return itsInputItemIndex;
	}
	
	public String getInputItemName() {
		return itsInputItemName;
	}

	public String toString() {
		StringBuffer output = new StringBuffer();
		
		if (isWarning()) {
			output.append("WARNING: ");
		} else if (isError()) {
			output.append("ERROR: ");
		}
		
		if (itsLine != -1) {
			output.append("LINE " + itsLine + ":");
		}

		if (itsInputItemIndex >= 0) {
			output.append(" ITEM " + itsInputItemIndex + " ('" + itsInputItemName + "'):");
		} else if (null != itsInputItemName) {
			output.append(" ITEM '" + itsInputItemName + "':");
		}

		output.append(" " + itsMessage);
		
		if (itsProperties.size() > 0) {
			output.append("\n");
			Enumeration<?> propNames = itsProperties.propertyNames();
			while (propNames.hasMoreElements()) {
				String name = (String) propNames.nextElement();
				output.append("  " + name + " = " + itsProperties.getProperty(name) + "\n");
			}
		}

		return output.toString();
	}
	
	public String getProperty(String name) {
		return itsProperties.getProperty(name);
	}

	public Enumeration<?> getPropertyNames() {
		return itsProperties.propertyNames();
	}

	public String getMessage() {
		return itsMessage;
	}

}
