/******************************************************************
 * File:        AveragePriceAccumulator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.QuerySolution;

import com.epimorphics.standardReports.Constants;
import com.epimorphics.standardReports.aggregators.SheetWriter.Style;
import com.epimorphics.util.EpiException;

/**
 * Accumulates data for average prices by area and type, then provides csv and Excel serializations.
 * Non-streaming, data sizes don't warrant it.
 */
public class BandedPriceAggregator extends BaseAggregator implements SRAggregator {
    protected static final String[] types = new String[]{ "Detached", "Semi-detached", "Terraced", "Flat-maisonette" /*, "other" */ };
    protected static final String[] bandVars = new String[]{ "count0",   "count1",   "count2",   "count3",   "count4",   "count5",   "count6",   "count7",   "count8",   "count9",  "count10",  "count12",  "count15",  "count20",  "count30",  "count40",  "count50",  "count60",  "count80", "count100", "count125", "count150", "count175", "count200"};
    protected static final int ROW_LEN = bandVars.length + 3;
    protected static final String[] HEADER = new String[]{"Area", "Type", "Under 10,000",  "10,000 - 20,000",  "20,001 - 30,000", "30,001 - 40,000", "40,001 - 50,000", "50,001 - 60,000",  "60,001 - 70,000", "70,001 - 80,000", "80,001 - 90,000", "90,001 - 100,000",    "100,001 - 120,000",   "120,001 - 150,000",   "150,001 - 200,000",   "200,001 - 300,000",   "300,001 - 400,000",   "400,001 - 500,000",   "500,001 - 600,000",   "600,001 - 800,000",   "800,001 - 1,000,000",  "1,000,001 - 1,250,000",    "1,250,001 - 1,500,000",   "1,500,001 - 1,750,000",   "1,750,001 - 2,000,000",   "over 2,000,000",  "Total"};
    protected int[] WIDTHS = new int[]{35, 20, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12};
    
    protected IndexedAggregator aggregator = new IndexedAggregator("area", () -> new IndexedAggregator("type", () -> new ByVarAggregator(bandVars))); 
    protected IndexedAggregator areaTotals = new IndexedAggregator("area", () -> new ByVarAggregator(bandVars)); 
    protected ByVarAggregator   totals = new ByVarAggregator(bandVars);
    
    public BandedPriceAggregator() {
        row_len = ROW_LEN;
        widths = WIDTHS;
    }
    
    public Accumulator getTotal() {
        return aggregator.getTotal();
    }
    
    public List<String> listAreas() {
        return aggregator.listIndexes();
    }
    
    public ByVarAggregator getAreaTotal(String area) {
        return (ByVarAggregator) areaTotals.getAggregator(area);
    }

    private IndexedAggregator getAreaAgg(String area) {
        Aggregator a = aggregator.getAggregator(area);
        if (a == null) {
            throw new EpiException("Internal error, no accumulator for that area");
        } else {
            return (IndexedAggregator) a;
        }
    }
    
    public ByVarAggregator getAreaTypeAccumulator(String area, String type) {
        return (ByVarAggregator) getAreaAgg(area).getSafeAggregator(type);
    }

    @Override
    public void add(QuerySolution row) {
        aggregator.add(row);
        areaTotals.add(row);
        totals.add(row);
    }
    
    @Override
    protected void format(SheetWriter writer, MultivaluedMap<String, String> request) {
        for (String mrow : makeMetadataRows("Volumes of Sales by Price Band", request)) {
            writer.addMetaRow(mrow);
        }
        writer.addHeaderRow(HEADER);
        boolean areaStripe = false;
        for (String area : listAreas()) {
            boolean lineStripe = false;
            boolean started = false;
            for (String type : types) {
                writer.startRow();
                writer.add( started ? "" : area, Style.Bold, areaStripe);
                started = true;
                writer.add(type, Style.Bold, lineStripe);
                ByVarAggregator bands = getAreaTypeAccumulator(area, type);
                for (String band : bandVars) {
                    writer.add( bands.getAggregator(band).getCount(), Style.Plain, lineStripe );
                }
                writer.add( bands.getTotal().getCount(), Style.Header );
                lineStripe = !lineStripe;
            }
            areaStripe = !areaStripe;
            // Subtotal for area
            writer.startRow();
            writer.add("", Style.Plain);
            writer.add("Total", Style.Header);
            ByVarAggregator bands = getAreaTotal(area);
            for (String band : bandVars) {
                writer.add( bands.getAggregator(band).getCount(), Style.Header );
            }
            writer.add( bands.getTotal().getCount(), Style.Header );
        }
        // Overall total
        if ( ! Constants.AT_NONE.equals( request.getFirst(Constants.AGGREGATE) ) ) {
            writer.startRow();
            writer.add("Total", Style.Header);
            writer.add("", Style.Header);
            for (String band : bandVars) {
                writer.add( totals.getAggregator(band).getCount(), Style.Header );
            }
            writer.add( totals.getTotal().getCount(), Style.Header );
        }
    }
    
}
