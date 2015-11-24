/******************************************************************
 * File:        ByVarAggregator.java
 * Created by:  Dave Reynolds
 * Created on:  24 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;

/**
 * Aggregator that accumulates each of a specified set of variables in the row.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ByVarAggregator implements Aggregator {
    protected String[] aggVars;
    protected Map<String, Accumulator> aggregates = new HashMap<>();
    protected Accumulator total = new Accumulator();
    
    public ByVarAggregator(String[] aggVars) {
        this.aggVars = aggVars;
        for (String aggVar : aggVars) {
            aggregates.put(aggVar, new Accumulator(aggVar, null));
        }
    }
    
    public Accumulator getTotal() {
        return total;
    }
    
    public Accumulator getAggregator(String index) {
        return aggregates.get(index);
    }

    public void add(QuerySolution row) {
        for (String aggVar : aggVars) {
            Literal lit = row.getLiteral(aggVar);
            if (lit != null) {
                Object val = lit.getValue();
                if (val instanceof Number) {
                    long count = ((Number)val).longValue();
                    Accumulator a = aggregates.get(aggVar);
                    a.add(count, 0);
                    total.add(count, 0);
                }
            }
        }
    }
    
}
