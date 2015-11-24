/******************************************************************
 * File:        LatestMonthAvailable.java
 * Created by:  Dave Reynolds
 * Created on:  23 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("latest-month-available")
public class LatestMonthAvailable extends SREndpointBase {
    static Logger log = LoggerFactory.getLogger( LatestMonthAvailable.class );
    
    protected static String latestMonth;
    protected long lastCheck = 0;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getLatestMonthAvailable() {
        if (latestMonth == null || (System.currentTimeMillis() - lastCheck) > 12 * 60 * 60 * 1000) {
            updateDate();
        }
        return latestMonth;
    }
    
    public synchronized void updateDate() {
        log.info("Updating latest month available");
        LocalDate probe = LocalDate.now();
        probe = LocalDate.of( probe.getYear(), probe.getMonth(), 1);
        while (! isPresent(probe) ) {
            probe = probe.minus(1,ChronoUnit.MONTHS); 
            log.info("Probing for: " + probe.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        latestMonth = probe.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Latest month available = " + latestMonth);
    }

    public boolean isPresent(LocalDate probe) {
        // TODO implement
        return true;
    }
}
