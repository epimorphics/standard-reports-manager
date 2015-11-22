/******************************************************************
 * File:        ReportManager.java
 * Created by:  Dave Reynolds
 * Created on:  18 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.armlib.CacheManager;
import com.epimorphics.armlib.QueueManager;
import com.epimorphics.armlib.RequestManager;

/**
 * The manager for requesting and generating standard reports.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ReportManager extends ComponentBase implements Startup {
    public static final String TEST_PARAM = "test";
    public static final String MOCK_DATA = "{webapp}/mockData/All_Postcode_Districts_within_GREATER_LONDON";
    
    static Logger log = LoggerFactory.getLogger( ReportManager.class );
    
    protected RequestManager requestManager;
    protected SRQueryFactory srQueryFactory;

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }
    
    public void setTemplateDir(String templateDir) {
        srQueryFactory = new SRQueryFactory( expandFileLocation(templateDir) );
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        TimerManager.get().schedule(new RequestProcessor(), 1, TimeUnit.SECONDS);
    }
    
    public class RequestProcessor implements Runnable {

        @Override
        public void run() {
            QueueManager queue = requestManager.getQueueManager();
            CacheManager cache = requestManager.getCacheManager();
            SparqlSource source = AppConfig.getApp().getA(SparqlSource.class);
            if (source == null) {
                log.error("Can't find source to query from");
                return;
            }

            log.info("Request processor starting");
            try {
                while (true) {
                    BatchRequest request = queue.nextRequest(1000);
                    if (request != null) {
                        log.info("Processing request: " + request.getKey());
                        
                        if (request.getParameters().containsKey(TEST_PARAM)) {
                            // Dummy delay for mock up
                            Thread.sleep(10000);
                            cache.upload(request, "csv", new File( expandFileLocation(MOCK_DATA) + ".csv"));
                            cache.upload(request, "xlsx", new File( expandFileLocation(MOCK_DATA) + ".xlsx"));
                            
                        } else {
                            // TODO Temporary test implementation
                            SRQuery query = srQueryFactory.get("testAPV.sq");
                            if (query == null) {
                                log.error("Can't find query");
                            } else {
                                try {
                                    ResultSet results = source.select( query.getQuery() );
                                    log.info("Query results started");
                                    System.out.println( ResultSetFormatter.asText(results) );
                                    log.info("Request completed");
                                    queue.finishRequest( request.getKey() );
                                } catch (Exception e) {
                                    // TODO some sort of retry scheme
                                    log.error("Request failed", e);
                                    queue.failRequest( request.getKey() );
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.info("Request processor interrupted, exiting");
            }
        }
    }
}
