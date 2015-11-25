/******************************************************************
 * File:        SRQuery.java
 * Created by:  Dave Reynolds
 * Created on:  20 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import static com.epimorphics.standardReports.Constants.*;

import java.util.regex.Matcher;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.XSD;

import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.sparql.exprs.Call;
import com.epimorphics.sparql.exprs.Infix;
import com.epimorphics.sparql.exprs.Op;
import com.epimorphics.sparql.graphpatterns.Basic;
import com.epimorphics.sparql.graphpatterns.Bind;
import com.epimorphics.sparql.query.AbstractSparqlQuery;
import com.epimorphics.sparql.templates.Settings;
import com.epimorphics.sparql.terms.Filter;
import com.epimorphics.sparql.terms.Literal;
import com.epimorphics.sparql.terms.TermAtomic;
import com.epimorphics.sparql.terms.Text;
import com.epimorphics.sparql.terms.Triple;
import com.epimorphics.sparql.terms.URI;
import com.epimorphics.sparql.terms.Var;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.PrefixUtils;

/**
 * Represents a configurable Standard Reports query
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SRQuery {
    public static final String TEXT_PATTERN = "_textPattern";
    
    protected AbstractSparqlQuery query;
    protected Settings settings = new Settings();
    
    public SRQuery(String template, PrefixMapping prefixes) {
        query = new AbstractSparqlQuery();
        query.setTemplate(template);
        settings.setPrefixMapping( prefixes );
    }
    
    protected SRQuery(AbstractSparqlQuery query, Settings settings) {
        this.query = query;
        this.settings = settings;
    }
    
    public SRQuery bindRequest(BatchRequest request) {
        MultivaluedMap<String, String> params = request.getParameters();
        String areaType = params.getFirst(AREA_TYPE);
        String aggregate = params.getFirst(AGGREGATE);
        String ageFilter = params.getFirst(AGE);
        String period = params.getFirst(PERIOD);
        
        Settings realSettings = new Settings();
        realSettings.setPrefixMapping( settings.getPrefixMapping() );
        realSettings.putParam(TEXT_PATTERN, new Text(""));
        
        AbstractSparqlQuery q = query.copy();
        SRQuery nq = new SRQuery(q, realSettings);
        
        Var addressVar     = new Var("address");
        Var transactionVar = new Var("transaction");
        Var areaVar        = new Var("area");
        
        
        String  areaStr = params.getFirst(AREA);
        Literal area = asLiteral( areaStr );
        
        Var pc = new Var("pc");
        boolean boundPostcode = false;
        
        // Inject the area filter
        if ( ! areaType.equals(AT_COUNTRY) ) {
            if (areaType.startsWith(AT_PC_PREFIX)) {
                // Text filter for more efficient generation of candidates
                String pcAreaOrDistrict = areaStr.replaceAll(" .*", "");
                String textFilter = String.format("?address  text:query (common:postcode '%s*' 3000000).", pcAreaOrDistrict);
                realSettings.putParam(TEXT_PATTERN, new Text(textFilter));
                
                // Exact match on the postcode prefix
                nq.addPattern( addressVar, new URI(common("postcode")), pc );
                boundPostcode = true;
                nq.query.addLaterPattern( new Basic( new Filter( new Infix( pcReplace(pcPattern(areaType)), Op.opEq, area) ) ) );
            } else if ( areaType.equals(AT_REGION)) {
                String fragmentName = "region_" + areaStr.replace(" ", "_") + ".sq";
                realSettings.putParam(TEXT_PATTERN, new Text( SRQueryFactory.get().getRaw(fragmentName))); 
                nq.addPattern( addressVar, new URI(common("county")), new Var("county") );
            } else {
                nq.addPattern( addressVar, new URI(common(areaType)), area );
            }
        }
        
        // Inject the aggregation level pattern
        if ( aggregate.startsWith(AT_PC_PREFIX) ) {
            if (!boundPostcode) {
                nq.addPattern( addressVar, new URI(common("postcode")), pc );
                boundPostcode = true;
            }
            nq.query.addEarlyPattern( new Bind( pcReplace(pcPattern(aggregate)), areaVar) );
            
        } else if (aggregate.equals(AT_NONE)) {
            nq.query.addEarlyPattern( new Bind( area, areaVar) );
        } else if (aggregate.equals(AT_REGION)) {
            nq.query.addEarlyPattern(new Basic( new Text( SRQueryFactory.get().getRaw("regionAggregate.sq" ) ) ));
        } else {
            nq.addPattern( addressVar, new URI(common(aggregate)), areaVar );
        }
        
        // Possible filter by age
        if ( ! ageFilter.equals(AGE_ANY)) {
            nq.addPattern( transactionVar, new URI(ppi("newBuild")), asLiteral( ageFilter.equals(AGE_NEW) ) );
        }
        
        // Date filter
        Matcher m = YEAR_FILTER.matcher(period);
        if ( m.matches() ) {
            int year = Integer.parseInt(period);
            nq.from( String.format("%4d-01-01", year), String.format("%4d-01-01", year+1) );
        } else {
            m = QUARTER_FILTER.matcher(period);
            if (m.matches()) {
                int year = Integer.parseInt( m.group(1) );
                int quarter = Integer.parseInt( m.group(2) );
                if (quarter == 4) {
                    nq.from( String.format("%4d-%02d-01", year, 1 + (quarter-1)*3), String.format("%4d-01-01", year + 1) );
                } else {
                    nq.from( String.format("%4d-%02d-01", year, 1 + (quarter-1)*3), String.format("%4d-%02d-01", year, 1 + (quarter)*3) );
                }
            } else {
                m = MONTH_FILTER.matcher(period);
                if (m.matches()) {
                    int year = Integer.parseInt( m.group(1) );
                    int month = Integer.parseInt( m.group(2) );
                    if (month == 12) {
                        nq.from( String.format("%4d-%02d-01", year, month), String.format("%4d-01-01", year + 1) );
                    } else {
                        nq.from( String.format("%4d-%02d-01", year, month), String.format("%4d-%02d-01", year, month+1) );
                    }
                } else {
                    throw new EpiException("Internal error validation let through an ill-formed period request");
                }
            }
        }
        return nq;
    }
    
    protected Call pcReplace(String pattern) {
        Var pc = new Var("pc");
        return new Call(Op.fnREPLACE, new Call(Op.fnSTR, pc), asLiteral(pattern), asLiteral("$1"));
    }
    
    protected String pcPattern(String pcType) {
        if (pcType.equals(PC_AREA)) {
            return "([A-Z]*).*";
        } else if (pcType.equals(PC_DISTRICT)) {
            return "([A-Z0-9]*) .*";
        } else {
            // Must be a sector
            return "([A-Z0-9]* \\\\d).*";
        }
    }
    
    protected Literal asLiteral(String string) {
        return new Literal(string, Literal.xsdString);
    }
    
    protected Literal asLiteral(boolean bool) {
        return new Literal(Boolean.toString(bool), Literal.xsdBoolean);
    }
    
    protected Literal asDate(String date) {
        return new Literal(date, new URI( XSD.date.getURI() ));
    }
    
    protected void from(String start, String end) {
        Var date = new Var("date");
        query.addLaterPattern(new Basic(new Filter(
                new Infix(
                        new Infix(date, Op.opGreaterEq, asDate(start)),
                        Op.opAnd, 
                        new Infix(date, Op.opLess, asDate(end))))));
    }
    
    protected void addPattern(TermAtomic S, TermAtomic P, TermAtomic O) {
        query.addEarlyPattern( new Basic( new Triple(S, P, O) ) );
    }
    
    public String getQuery() {
        String qstr = query.toSparqlSelect( settings );
        qstr = PrefixUtils.expandQuery(qstr, settings.getPrefixMapping());
        return qstr;
    }
}
