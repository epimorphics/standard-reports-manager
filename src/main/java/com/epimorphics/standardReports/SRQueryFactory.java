/******************************************************************
 * File:        SRQueryFactory.java
 * Created by:  Dave Reynolds
 * Created on:  20 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.simpleAPI.core.API;

/**
 * Retrieves a base instance of an SRQuery than can be customized.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SRQueryFactory {
    static Logger log = LoggerFactory.getLogger( SRQueryFactory.class );
    
    public static SRQueryFactory theSRQueryFactory;
    
    protected File templateDir;
    protected PrefixMapping prefixes;
    protected Map<String, SRQuery> queries = new HashMap<>();
    protected Map<String, String> rawQueries = new HashMap<>();
    
    public SRQueryFactory(String templateDir) {
        this.templateDir = new File( templateDir );
        theSRQueryFactory = this;
    }
    
    public static SRQueryFactory get() {
        return theSRQueryFactory;
    }
    
    public PrefixMapping getPrefixes() {
        if (prefixes == null) {
            prefixes = API.get().getPrefixes();
        }
        return prefixes;
    }
    
    public String getRaw(String templateName) {
        String template = rawQueries.get(templateName);
        if (template == null) {
            String fname = new File(templateDir, templateName).getPath();
            try {
                template = FileManager.get().readWholeFileAsUTF8( fname );
                rawQueries.put(templateName, template);
            } catch (Exception e) {
                log.error("Could not locate template: " + templateName);
            }
        }
        return template;
    }
    
    public SRQuery get(String templateName) {
        SRQuery query = queries.get(templateName);
        if (query == null) {
            String template = getRaw(templateName);
            if (template != null){
                query = new SRQuery(template, getPrefixes());
                queries.put(templateName, query);
            }
        }
        return query;
    }
}
