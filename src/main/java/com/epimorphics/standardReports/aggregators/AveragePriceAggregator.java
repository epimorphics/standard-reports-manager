/******************************************************************
 * File:        AveragePriceAccumulator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import static com.epimorphics.standardReports.webapi.ReportRequestEndpoint.AGE;
import static com.epimorphics.standardReports.webapi.ReportRequestEndpoint.AGGREGATE;
import static com.epimorphics.standardReports.webapi.ReportRequestEndpoint.AREA;
import static com.epimorphics.standardReports.webapi.ReportRequestEndpoint.AREA_TYPE;
import static com.epimorphics.standardReports.webapi.ReportRequestEndpoint.PERIOD;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QuerySolution;

import com.epimorphics.simpleAPI.requests.Request;
import com.epimorphics.util.EpiException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Accumulates data for average prices by area and type, then provides csv and Excel serializations.
 * Non-streaming, data sizes don't warrant it.
 */
public class AveragePriceAggregator implements Aggregator {
    public static final String[] types = new String[]{ "Detached", "Semi-detached", "Terraced", "Flat-maisonette" /*, "other" */ };
    public static final int ROW_LEN = 11;
    public static final String[] HEADER = new String[]{"", "Detached", "Sales", "Semi-det", "Sales", "Terraced", "Sales", "Flat/mais", "Sales", "Overall average", "Total sales"};
    
    protected IndexedAggregator aggregator = new IndexedAggregator("area", () -> new IndexedAggregator("type")); 
    protected IndexedAggregator totals = new IndexedAggregator("type");
    
    public Accumulator getTotal() {
        return aggregator.getTotal();
    }
    
    public List<String> listAreas() {
        return aggregator.listIndexes();
    }
    
    public Accumulator getAreaTotal(String area) {
        return getAreaAgg(area).getTotal();
    }

    private IndexedAggregator getAreaAgg(String area) {
        Aggregator a = aggregator.getAggregator(area);
        if (a == null) {
            throw new EpiException("Internal area, no accumulator for that area");
        } else {
            return (IndexedAggregator) a;
        }
    }
    
    public Accumulator getAreaTypeAccumulator(String area, String type) {
        return (Accumulator) getAreaAgg(area).getSafeAggregator(type);
    }

    @Override
    public void add(QuerySolution row) {
        aggregator.add(row);
        totals.add(row);
    }
    
    public void writeAsCSV(OutputStream out, Request request) throws IOException {
        CSVWriter writer = new CSVWriter( new OutputStreamWriter(out) );
        for (String mrow : makeMetadataRows(request)) {
            writer.writeNext( makeRow(mrow) );
        }
        writer.writeNext(HEADER);
        for (String area : listAreas()) {
            MakeRow row = new MakeRow();
            row.add(area);
            for (String type : types) {
                row.add( getAreaTypeAccumulator(area, type) );
            }
            row.add( getAreaTotal(area) );
            row.write(writer);
        }
        MakeRow row = new MakeRow();
        row.add("Total");
        for (String type : types) {
            row.add( totals.getSafeAggregator(type) );
        }
        row.add( totals.getTotal() );
        row.write(writer);
        writer.close();
    }
    
    private String[] makeRow(Object...args) {
        MakeRow row = new MakeRow();
        for (Object arg : args) row.add(arg);
        return row.getRow();
    }
    
    private List<String> makeMetadataRows(Request request) {
        List<String> metadata = new ArrayList<>();
        metadata.add("Average Prices and Volumes of Sales");
        metadata.add( String.format("By %s for %s - %s", request.getFirst(AGGREGATE), request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        metadata.add( request.getFirst(PERIOD) );
        metadata.add( "Age: " + request.getFirst(AGE) );
        return metadata;
    }
    
    private static class MakeRow {
        String[] row = new String[ROW_LEN];
        int count = 0;
        
        public MakeRow() {
            for(int i = 0; i < ROW_LEN; i++) row[i] = "";
        }
        
        public void add(String value) {
            if (count >= ROW_LEN +1) throw new EpiException("Too many elements for row");
            row[count++] = value;
        }
        
        public void add(Accumulator a) {
            if (count >= ROW_LEN +2) throw new EpiException("Too many elements for row");
            row[count++] = Long.toString( a.getAverage().longValue() );
            row[count++] = Long.toString( a.getCount() );
        }
        
        public void add(Object o) {
            if (o instanceof Accumulator) {
                add((Accumulator)o);
            } else {
                add(o.toString());
            }
        }
        
        public String[] getRow() {
            return row;
        }
        
        public void write(CSVWriter writer) {
            writer.writeNext(row);
        }
    }
    
    
    public void writeAsExcel(OutputStream out, Request request) throws IOException {
        ExcelWriter writer = new ExcelWriter();
        for (String mrow : makeMetadataRows(request)) {
            writer.addMetaRow( mrow );
        }
        writer.addHeaderRow( HEADER );
        for (String area : listAreas()) {
            writer.startRow();
            writer.addLabelCell(area);
            for (String type : types) {
                writer.addAccumulator(getAreaTypeAccumulator(area, type), false);
            }
            writer.addAccumulator(getAreaTotal(area), true);
        }
        writer.startRow();
        writer.addCell("Total", writer.highlightStyle);
        for (String type : types) {
            writer.addAccumulator( (Accumulator) totals.getSafeAggregator(type), true);
        }
        writer.addAccumulator( totals.getTotal(), true);
        writer.write(out);
    }

}
