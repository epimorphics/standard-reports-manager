/******************************************************************
 * File:        Aggregator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import org.apache.jena.query.QuerySolution;

/**
 * Generic signature for classes that accumulate values from query solution rows.
 */
public interface Aggregator {
    
    public void add(QuerySolution row);
}
