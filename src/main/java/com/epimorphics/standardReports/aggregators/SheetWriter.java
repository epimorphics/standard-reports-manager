/******************************************************************
 * File:        SheetWriter.java
 * Created by:  Dave Reynolds
 * Created on:  24 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

/**
 * Abstraction of CSV and Excel output support.
 * The highlight options will be ignored for CSV but output for excel.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface SheetWriter {
    public enum Style {Bold, Header, Plain};
    
    public void addMetaRow(String meta);
    
    public void addHeaderRow(String[] headers);
    
    public void startRow();
    
    /** handle string, long, average-accumulator */
    public void add(Object o, Style style);
    
    /** handle string, long, average-accumulator */
    public void add(Object o, Style style, boolean stripe);
}
