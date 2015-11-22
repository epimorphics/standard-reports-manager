/******************************************************************
 * File:        AverageAggregator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;

import com.epimorphics.util.EpiException;

/**
 * Aggregates result rows as a set of accumulators indexed by some
 * variable.. 
 */
public class IndexedAggregator implements Aggregator {
    protected String countVar = "count";
    protected String totalVar = "total";
    protected String indexVar = "index";
    protected Supplier<Aggregator> supplier = Accumulator::new;
    protected Accumulator total;
    protected Map<String, Aggregator> aggregates = new HashMap<>();
    
    public IndexedAggregator() {
    }
    
    public IndexedAggregator(String indexVar) {
        this.indexVar = indexVar;
    }
    
    public IndexedAggregator(Supplier<Aggregator> aggregatorSupplier) {
        this.supplier = aggregatorSupplier;
    }
    
    public IndexedAggregator(String indexVar, Supplier<Aggregator> aggregatorSupplier) {
        this.indexVar = indexVar;
        this.supplier = aggregatorSupplier;
    }
    
    public void setCountVar(String countVar) {
        this.countVar = countVar;
    }

    public void setTotalVar(String totalVar) {
        this.totalVar = totalVar;
    }

    public void setIndexVar(String indexVar) {
        this.indexVar = indexVar;
    }
    
    public Accumulator getTotal() {
        return total;
    }
    
    public List<String> listIndexes() {
        List<String> indexes = new ArrayList<>( aggregates.keySet() );
        Collections.sort(indexes);
        return indexes;
    }
    
    public Aggregator getAggregator(String index) {
        return aggregates.get(index);
    }
    
    public Aggregator getSafeAggregator(String index) {
        Aggregator a = getAggregator(index);
        return a == null ? new Accumulator() : a;
    }

    public void add(QuerySolution row) {
        if (total == null) {
            total = new Accumulator(countVar, totalVar);
        }
        total.add(row);

        String index = getIndex(row);
        Aggregator a = aggregates.get( index );
        if (a == null) {
            a = supplier.get();
            aggregates.put(index, a);
        }
        a.add(row);
    }
    
    private String getIndex(QuerySolution row) {
        Literal index = row.getLiteral(indexVar);
        if (index == null) {
            throw new EpiException("No value for index found in solution row");
        }
        return index.getLexicalForm();
    }
    
}
