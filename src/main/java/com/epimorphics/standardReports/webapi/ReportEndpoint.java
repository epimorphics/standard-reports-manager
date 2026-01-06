/******************************************************************
 * File:        ReportEndpoint.java
 * Created by:  Dave Reynolds
 * Created on:  19 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("report")
public class ReportEndpoint extends SREndpointBase {

    @Path("/{key:.*}")
    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public InputStream getReportXLSX(@PathParam("key") String key) {
        return getReport(key, "xlsx");
    }

    @Path("/{key:.*}")
    @GET
    @Produces("text/csv")
    public InputStream getReportCSV(@PathParam("key") String key) {
        return getReport(key, "csv");
    }
    
    protected InputStream getReport(String key, String suffix) {
        InputStream result = getRequestManager().getCacheManager().readResult(key, suffix);
        if (result == null){
            throw new NotFoundException();
        }
        return result;
    }
}
