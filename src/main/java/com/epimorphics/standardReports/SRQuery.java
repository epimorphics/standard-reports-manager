/******************************************************************
 * File:        SRQuery.java
 * Created by:  Dave Reynolds
 * Created on:  20 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import org.apache.jena.shared.PrefixMapping;

import com.epimorphics.sparql.query.AbstractSparqlQuery;
import com.epimorphics.sparql.templates.Settings;
import com.epimorphics.util.PrefixUtils;

/**
 * Represents a configurable Standard Reports query
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SRQuery {
    protected AbstractSparqlQuery query;
    protected Settings settings = new Settings();
    
    public SRQuery(String template, PrefixMapping prefixes) {
        query = new AbstractSparqlQuery();
        query.setTemplate(template);
        settings.setPrefixMapping( prefixes );
    }
    
    public String getQuery() {
        // TODO implement
        String qstr = query.toSparqlSelect( new Settings() );
        return PrefixUtils.expandQuery(qstr, settings.getPrefixMapping());
    }
}
