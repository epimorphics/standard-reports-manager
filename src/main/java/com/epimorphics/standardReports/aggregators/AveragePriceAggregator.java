/******************************************************************
 * File:        AveragePriceAccumulator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import static com.epimorphics.standardReports.Constants.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.QuerySolution;

import com.epimorphics.util.EpiException;

/**
 * Accumulates data for average prices by area and type, then provides csv and Excel serializations.
 * Non-streaming, data sizes don't warrant it.
 */
public class AveragePriceAggregator implements SRAggregator {
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
    
    public void writeAsCSV(OutputStream out, MultivaluedMap<String, String> request) throws IOException {
        CSVOutput writer = new CSVOutput(out, ROW_LEN);
        for (String mrow : makeMetadataRows(request)) {
            writer.addMetaRow(mrow);
        }
        writer.addHeaderRow(HEADER);
        for (String area : listAreas()) {
            writer.startRow();
            writer.add(area);
            for (String type : types) {
                writer.add( getAreaTypeAccumulator(area, type) );
            }
            writer.add( getAreaTotal(area) );
        }
        writer.startRow();
        writer.add("Total");
        for (String type : types) {
            writer.add( totals.getSafeAggregator(type) );
        }
        writer.add( totals.getTotal() );
        writer.close();
    }
    
    public void writeAsExcel(OutputStream out, MultivaluedMap<String, String> request) throws IOException {
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
    
    private List<String> makeMetadataRows(MultivaluedMap<String, String> request) {
        List<String> metadata = new ArrayList<>();
        metadata.add("Average Prices and Volumes of Sales");
        metadata.add( String.format("By %s for %s - %s", request.getFirst(AGGREGATE), request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        metadata.add( request.getFirst(PERIOD) );
        metadata.add( "Age: " + request.getFirst(AGE) );
        return metadata;
    }

}
