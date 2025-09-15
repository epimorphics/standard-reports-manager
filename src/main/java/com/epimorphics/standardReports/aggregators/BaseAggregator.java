/******************************************************************
 * File:        BaseAggregator.java
 * Created by:  Dave Reynolds
 * Created on:  5 Jan 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import static com.epimorphics.standardReports.Constants.AGE;
import static com.epimorphics.standardReports.Constants.AGGREGATE;
import static com.epimorphics.standardReports.Constants.AREA;
import static com.epimorphics.standardReports.Constants.AREA_TYPE;
import static com.epimorphics.standardReports.Constants.AT_NONE;
import static com.epimorphics.standardReports.Constants.PERIOD;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

public abstract class BaseAggregator implements SRAggregator {
    protected int row_len;      // Length of sheet rows
    protected int[] widths;     // Widths in characters for each column in the excel output

    abstract protected void format(SheetWriter writer, MultivaluedMap<String, String> request);
    
    @Override
    public void writeAsCSV(OutputStream out, MultivaluedMap<String, String> request) throws IOException {
        CSVOutput writer = new CSVOutput(out, row_len);
        format(writer, request);
        writer.close();
    }
    
    @Override
    public void writeAsExcel(OutputStream out, MultivaluedMap<String, String> request) throws IOException {
        ExcelWriter writer = new ExcelWriter();
//        SExcelWriter writer = new SExcelWriter();
        writer.setColumnWidths(widths);
        format(writer, request);
        writer.write(out);
    }
    
    protected List<String> makeMetadataRows(String resultType, MultivaluedMap<String, String> request) {
        List<String> metadata = new ArrayList<>();
        metadata.add(resultType);
        String aggregate = request.getFirst(AGGREGATE);
        if (aggregate.equals(AT_NONE)) {
            metadata.add( String.format("For %s - %s", request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        } else {
            metadata.add( String.format("By %s for %s - %s", aggregate, request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        }
        metadata.add( request.getFirst(PERIOD) );
        metadata.add( "Age: " + request.getFirst(AGE) );
        metadata.add( "Report created on: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        return metadata;
    }    
}
