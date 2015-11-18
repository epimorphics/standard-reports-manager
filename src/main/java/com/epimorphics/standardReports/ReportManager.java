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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.core.TimerManager;
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
    static Logger log = LoggerFactory.getLogger( ReportManager.class );
    
    public static final String MOCK_DATA = "{webapp}/mockData/All_Postcode_Districts_within_GREATER_LONDON";
    
    protected RequestManager requestManager;

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }
    
    // TODO implement background loop processing requests
    
    @Override
    public void startup(App app) {
        super.startup(app);
        TimerManager.get().scheduleAtFixedRate(new RequestProcessor(), 1000, 100, TimeUnit.MILLISECONDS);
    }
    
    public class RequestProcessor implements Runnable {

        @Override
        public void run() {
            QueueManager queue = requestManager.getQueueManager();
            CacheManager cache = requestManager.getCacheManager();
            BatchRequest request = queue.nextRequest();
            if (request != null) {
                try {
                    log.info("Processing request: " + request.getKey());
                    Thread.sleep(5000);  // Dummy delay for mock up
                    cache.upload(request, "csv", new File( expandFileLocation(MOCK_DATA) + ".csv"));
                    cache.upload(request, "xlsx", new File( expandFileLocation(MOCK_DATA) + ".xlsx"));
                    queue.finishRequest( request.getKey() );
                } catch (Exception e) {
                    log.error("Error during request processing", e);
                    queue.failRequest( request.getKey() );
                }
            }
        }
        
    }
}
