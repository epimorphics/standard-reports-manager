/******************************************************************
 * File:        CSVOutput.java
 * Created by:  Dave Reynolds
 * Created on:  23 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.epimorphics.util.EpiException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Support for writing standard reports out as CSV files.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CSVOutput {
    protected int rowlen;
    protected CSVWriter writer;
    protected String[] row;
    protected int col = 0;
    
    public CSVOutput(OutputStream out, int rowlen) {
        writer = new CSVWriter( new OutputStreamWriter(out) );
        this.rowlen = rowlen;
    }
    
    public void startRow() {
        if (row != null){
            writer.writeNext(row);
        }
        row = new String[rowlen];
        for (int i = 0; i < rowlen; i++) {
            row[i] = "";
        }
        col = 0;
    }
    
    public void addMetaRow(String meta) {
        startRow();
        row[0] = meta;
        writer.writeNext(row);
        row = null;
    }

    public void addHeaderRow(String[] header) {
        if (header.length != rowlen) throw new EpiException("Header wrong length");
        writer.writeNext(header);
    }
    
    public void add(String value) {
        if (col > rowlen - 1) throw new EpiException("Too many elements for row");
        row[col++] = value;
    }
    
    public void add(Accumulator a) {
        if (col > rowlen - 2) throw new EpiException("Too many elements for row");
        row[col++] = Long.toString( a.getAverage().longValue() );
        row[col++] = Long.toString( a.getCount() );
    }
    
    public void add(Object o) {
        if (o instanceof Accumulator) {
            add((Accumulator)o);
        } else {
            add(o.toString());
        }
    }
    
    public void close() throws IOException {
        if (row != null) {
            writer.writeNext(row);
        }
        writer.close();
    }
}
