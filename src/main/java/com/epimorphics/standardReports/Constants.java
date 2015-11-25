/******************************************************************
 * File:        Constants.java
 * Created by:  Dave Reynolds
 * Created on:  23 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import java.util.regex.Pattern;

public class Constants {
    public static final String AREA_TYPE = "areaType";
    public static final String AREA      = "area";
    public static final String AGGREGATE = "aggregate";
    public static final String AGE       = "age";
    public static final String PERIOD    = "period";
    public static final String REPORT    = "report";
    public static final String STICKY    = "sticky";
    
    public static final String AT_COUNTRY = "country";
    public static final String AT_COUNTY = "county";
    public static final String AT_REGION = "region";
    public static final String AT_NONE    = "none";
    public static final String AT_PC_PREFIX = "pc";
    public static final String PC_AREA      = "pcArea";
    public static final String PC_DISTRICT  = "pcDistrict";
    public static final String PC_SECTOR    = "pcSector";
    
    public static final String AGE_OLD = "old";
    public static final String AGE_NEW = "new";
    public static final String AGE_ANY = "any";
    
    public static final String REPORT_BYPRICE = "avgPrice";
    public static final String REPORT_BANDED = "banded";
    
    public static final String TEST_PARAM = "test";
    
    public static String common(String elt) {
        return "http://landregistry.data.gov.uk/def/common/" + elt;
    }
    
    public static String ppi(String elt) {
        return "http://landregistry.data.gov.uk/def/ppi/" + elt;
    }

    public static final Pattern YEAR_FILTER = Pattern.compile("^(\\d\\d\\d\\d)$");
    public static final Pattern QUARTER_FILTER = Pattern.compile("^(\\d\\d\\d\\d)-Q(\\d)$");
    public static final Pattern MONTH_FILTER = Pattern.compile("^(\\d\\d\\d\\d)-(\\d\\d)$");
}
