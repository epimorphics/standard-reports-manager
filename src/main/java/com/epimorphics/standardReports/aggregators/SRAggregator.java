/******************************************************************
 * File:        SRAggregator.java
 * Created by:  Dave Reynolds
 * Created on:  23 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MultivaluedMap;

public interface SRAggregator extends Aggregator {

    public void writeAsExcel(OutputStream out, MultivaluedMap<String, String> request) throws IOException;
    
    public void writeAsCSV(OutputStream out, MultivaluedMap<String, String> request) throws IOException;
    
}
