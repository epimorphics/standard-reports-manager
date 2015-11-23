/******************************************************************
 * File:        TestAccumulator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.FileManager;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Test;

public class TestAggregators {

    @Test
    public void testAccumulatorRaw() {
        Accumulator a = new Accumulator();
        a.add(1, 10);
        a.add(3,  new BigDecimal("30"));
        a.add(5,  new BigInteger("50"));
        assertEquals(9, a.getCount());
        assertEquals(90, a.getTotal().longValueExact());
        assertEquals(10, a.getAverage().longValueExact());
    }
    
    @Test
    public void testAccumulatorByRow() {
        Accumulator a = new Accumulator();
        a.add( makeRow("count", 2, "total", 5) );
        a.add( makeRow("count", 3, "total", 15) );
        assertEquals(5, a.getCount());
        assertEquals(20, a.getTotal().longValueExact());
        assertEquals(4, a.getAverage().longValueExact());
    }
    
    @Test
    public void testIndexAccumulator() {
        IndexedAggregator a = new IndexedAggregator();
        a.add( makeRow("count", 2, "total", 5, "index", "foo") );
        a.add( makeRow("count", 3, "total", 15, "index", "foo") );
        a.add( makeRow("count", 5, "total", 5, "index", "bar") );
        a.add( makeRow("count", 10, "total", 10, "index", "bar") );
        
        Accumulator total = a.getTotal();
        assertEquals(20, total.getCount());
        assertEquals(35, total.getTotal().longValueExact());

        Accumulator i = (Accumulator) a.getAggregator("foo");
        assertEquals(5, i.getCount());
        assertEquals(20, i.getTotal().longValueExact());

        i = (Accumulator) a.getAggregator("bar");
        assertEquals(15, i.getCount());
        assertEquals(15, i.getTotal().longValueExact());
    }
    
    @Test
    public void testPriceCSV() throws IOException {
        AveragePriceAggregator apa = new AveragePriceAggregator();
        apa.add( makeRow("count", 10, "total", 1000000, "area", "foo", "type", "Detached") );
        apa.add( makeRow("count", 10, "total",  100000, "area", "foo", "type", "Semi-detached") );
        apa.add( makeRow("count", 10, "total",   10000, "area", "foo", "type", "Terraced") );
        apa.add( makeRow("count", 5, "total",  2000000, "area", "bar", "type", "Detached") );
        apa.add( makeRow("count", 5, "total",   200000, "area", "bar", "type", "Semi-detached") );
        apa.add( makeRow("count", 5, "total",    20000, "area", "bar", "type", "Terraced") );
        
        MultivaluedMap<String, String> request = new MultivaluedStringMap();
        request.add("area", "HAMPSHIRE");
        request.add("areaType", "County");
        request.add("aggregate", "district");
        request.add("age", "any");
        request.add("period", "2015-Q1");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        apa.writeAsCSV(out, request);
        
        String expected = FileManager.get().readWholeFileAsUTF8("src/test/data/testAPV.csv");
        assertEquals(expected, out.toString());
        
        // Testing the output is correct is tricky (and currently manual) but at least this checks it runs
        FileOutputStream fout = new FileOutputStream("target/test.xlsx");
        apa.writeAsExcel(fout, request);
    }
    
    public static QuerySolution makeRow(Object...args) {
        QuerySolutionMap row = new QuerySolutionMap();
        for (int i = 0; i < args.length;) {
            String varname = args[i++].toString();
            Object value = args[i++];
            if (value instanceof RDFNode) {
                row.add(varname, (RDFNode)value);
            } else if (value instanceof String) {
                row.add(varname, ResourceFactory.createPlainLiteral((String)value));
            } else {
                row.add(varname, ResourceFactory.createTypedLiteral(value));
            }
        }
        return row;
    }
}
