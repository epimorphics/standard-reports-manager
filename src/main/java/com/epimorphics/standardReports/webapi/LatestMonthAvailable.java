/******************************************************************
 * File:        LatestMonthAvailable.java
 * Created by:  Dave Reynolds
 * Created on:  23 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("latest-month-available")
public class LatestMonthAvailable extends SREndpointBase {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getLatestMonthAvailable() {
        // TODO implement
        return "2015-09";
    }
}
