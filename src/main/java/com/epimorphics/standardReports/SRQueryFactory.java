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

import com.epimorphics.simpleAPI.core.API;

/**
 * Retrieves a base instance of an SRQuery than can be customized.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SRQueryFactory {
    protected File templateDir;
    protected PrefixMapping prefixes;
    protected Map<String, SRQuery> queries = new HashMap<>();
    
    public SRQueryFactory(String templateDir) {
        this.templateDir = new File( templateDir );
    }
    
    public PrefixMapping getPrefixes() {
        if (prefixes == null) {
            prefixes = API.get().getPrefixes();
        }
        return prefixes;
    }
    
    public SRQuery get(String templateName) {
        SRQuery query = queries.get(templateName);
        if (query == null) {
            String fname = new File(templateDir, templateName).getPath();
            String qstr = FileManager.get().readWholeFileAsUTF8( fname );
            query = new SRQuery(qstr, getPrefixes());
            queries.put(templateName, query);
        }
        return query;
    }
}
