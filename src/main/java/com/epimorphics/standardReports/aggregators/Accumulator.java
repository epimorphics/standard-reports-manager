/******************************************************************
 * File:        Accumulator.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.jena.query.QuerySolution;

import com.epimorphics.util.EpiException;

/**
 * Tracks counts and totals of rows to arbitrary precision.
 * If the totalVar is null then only tracks counts
 */
public class Accumulator implements Aggregator {
    protected String countVar = "count";
    protected String totalVar = "total";

    protected long count = 0;
    protected BigDecimal total = new BigDecimal(0);
    
    public Accumulator() {
    }
    
    public Accumulator(String countVar, String totalVar) {
        this.countVar = countVar;
        this.totalVar = totalVar;
    }
    
    
    public void setCountVar(String countVar) {
        this.countVar = countVar;
    }

    public void setTotalVar(String totalVar) {
        this.totalVar = totalVar;
    }

    public void add(QuerySolution row) {
        if (hasVar(row, countVar)) {
            count += getVarAsLong(row, countVar);
        }
        if (hasVar(row, totalVar)) {
            total = total.add( getVarAsDecimal(row, totalVar) );
        }
    }
    
    public void add(long count, BigDecimal total) {
        this.count += count;
        this.total = this.total.add(total);
    }
    
    public void add(long count, BigInteger total) {
        this.count += count;
        this.total = this.total.add( new BigDecimal(total) );
    }
    
    public void add(long count, long total) {
        this.count += count;
        this.total = this.total.add( new BigDecimal(total) );
    }
    
    public void addCount(long count) {
        this.count += count;
    }
    
    public long getCount() {
        return count;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public BigDecimal getAverage() {
        if (count == 0) {
            return new BigDecimal(0);
        } else {
            return total.divide( new BigDecimal(count) );
        }
    }
    
    public static boolean hasVar(QuerySolution row, String varname) {
        return varname != null && row.get(varname) != null;
    }
    
    public static long getVarAsLong(QuerySolution row, String varname) {
        Object value = row.getLiteral(varname).getValue();
        if (value instanceof Number) {
            return ((Number)value).longValue();
        } else {
            throw new EpiException("Found non-numeric value for " + varname);
        }
    }
    
    public static BigDecimal getVarAsDecimal(QuerySolution row, String varname) {
        Object value = row.getLiteral(varname).getValue();
        if (value instanceof Number) {
            if (value instanceof BigDecimal) {
                return  (BigDecimal)value;
            } if (value instanceof BigInteger) {
                return new BigDecimal( (BigInteger)value );
            } else if (value instanceof Float || value instanceof Double) {
                return new BigDecimal( value.toString() );
            } else {
                return new BigDecimal( ((Number)value).longValue() );
            }
        } else {
            throw new EpiException("Found non-numeric value for " + varname);
        }
    }
}
