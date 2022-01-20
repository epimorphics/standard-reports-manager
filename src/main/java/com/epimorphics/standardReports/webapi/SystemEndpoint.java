/******************************************************************
 * File:        SystemEndpoint.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.epimorphics.armlib.CacheManager;
import com.epimorphics.armlib.QueueManager;
import com.epimorphics.standardReports.ReportManager;

/**
 * Endpoints to support system maintenance
 */
@Path("system")
public class SystemEndpoint extends SREndpointBase {

    /**
     * Clear the transient cache
     */
    @POST
    @Path("clear-cache")
    public Response clearCache() {
        CacheManager cache = getReportManager().getRequestManager().getCacheManager();
        cache.clearNonSticky();
        return Response.ok().build();
    }

    /**
     * Clear both transient and sticky caches
     */
    @POST
    @Path("clear-cache-all")
    public Response clearAllCache() {
        CacheManager cache = getReportManager().getRequestManager().getCacheManager();
        cache.clear();
        return Response.ok().build();
    }

    /**
     * Clear the older completion records from queue manager
     */
    @POST
    @Path("clear-old-records")
    public Response clearOldRecords() {
        ReportManager rm = getReportManager();
        QueueManager queue = rm.getRequestManager().getQueueManager();
        long cutoff = System.currentTimeMillis() - rm.getRecordRetentionPeriod() * 60 * 60 * 24 * 1000;
        queue.removeOldCompletedRequests(cutoff);
        return Response.ok().build();
    }
    
    /**
     * Requests suspension of processing
     */
    @POST
    @Path("suspend")
    public Response suspend() {
        getReportManager().suspend();
        return Response.ok("Suspending").build();
    }
    
    /**
     * Resume processing after a suspension
     */
    @POST
    @Path("resume")
    public Response resume() {
        getReportManager().resume();
        return Response.ok("Resuming").build();
    }
    
    /**
     * Return the current processing status: Running, Suspending, or Suspended 
     */
    @GET
    @Path("status")
    public String getStatus() {
        return getReportManager().getStatus().name();
    }

    /**
     * Return prometheus format text metrics
     */
    @GET
    @Path("metrics")
    @Produces("text/plain;version=0.0.4")
    public String getMetrics() {
        return getReportManager().getRegistry().scrape();
    }
}
