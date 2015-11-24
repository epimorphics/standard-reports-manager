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

import com.epimorphics.standardReports.aggregators.SheetWriter.Style;
import com.epimorphics.util.EpiException;

/**
 * Accumulates data for average prices by area and type, then provides csv and Excel serializations.
 * Non-streaming, data sizes don't warrant it.
 */
public class AveragePriceAggregator implements SRAggregator {
    public static final String[] types = new String[]{ "Detached", "Semi-detached", "Terraced", "Flat-maisonette" /*, "other" */ };
    public static final int ROW_LEN = 11;
    public static final String[] HEADER = new String[]{"", "Detached", "Sales", "Semi-det", "Sales", "Terraced", "Sales", "Flat/mais", "Sales", "Overall average", "Total sales"};
    public static final int[] WIDTHS = new int[]{35, 15, 10, 15, 10, 15, 10, 15, 10, 15, 15};
    
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
            throw new EpiException("Internal error, no accumulator for that area");
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
        format(writer, request);
        writer.close();
    }
    
    public void writeAsExcel(OutputStream out, MultivaluedMap<String, String> request) throws IOException {
        ExcelWriter writer = new ExcelWriter();
        writer.setColumnWidths(WIDTHS);
        format(writer, request);
        writer.write(out);
    }
    
    private void format(SheetWriter writer, MultivaluedMap<String, String> request) {
        for (String mrow : makeMetadataRows(request)) {
            writer.addMetaRow( mrow );
        }
        writer.addHeaderRow( HEADER );
        boolean stripe = false;
        for (String area : listAreas()) {
            writer.startRow();
            writer.add(area, Style.Bold, stripe);
            for (String type : types) {
                writer.add(getAreaTypeAccumulator(area, type), Style.Plain, stripe);
            }
            writer.add(getAreaTotal(area), Style.Bold, stripe);
            stripe = !stripe;
        }
        writer.startRow();
        writer.add("Total", Style.Header);
        for (String type : types) {
            writer.add( totals.getSafeAggregator(type), Style.Header);
        }
        writer.add( totals.getTotal(), Style.Header);
    }
    
    private List<String> makeMetadataRows(MultivaluedMap<String, String> request) {
        List<String> metadata = new ArrayList<>();
        metadata.add("Average Prices and Volumes of Sales");
        String aggregate = request.getFirst(AGGREGATE);
        if (aggregate.equals(AT_NONE)) {
            metadata.add( String.format("For %s - %s", request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        } else {
            metadata.add( String.format("By %s for %s - %s", aggregate, request.getFirst(AREA_TYPE), request.getFirst(AREA)) );
        }
        metadata.add( request.getFirst(PERIOD) );
        metadata.add( "Age: " + request.getFirst(AGE) );
        return metadata;
    }

}
