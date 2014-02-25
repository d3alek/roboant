package uk.ac.ed.insectlab.ant;

import java.util.regex.Pattern;

public class Constants {
	 
    public static final String CLOSED_CONNECTION = "close_connection";
    public static final String LOGIN_NAME = "roboant";
    
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	public static final Pattern mRecordPattern = Pattern.compile("record\\s(on|off)");
	public static final Pattern mNavigationPattern = Pattern.compile("ai");
 
}
