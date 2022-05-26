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

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.standardReports.SRQueryFactory;

@Path("latest-month-available")
public class LatestMonthAvailable extends SREndpointBase {
    static Logger log = LoggerFactory.getLogger( LatestMonthAvailable.class );
    static final long MAX_AGE = 12 * 60 * 60 * 1000;
    protected static String latestMonth;
    protected static long lastCheck = 0;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getLatestMonthAvailable() {
        if (latestMonth == null || (System.currentTimeMillis() - lastCheck) > MAX_AGE) {
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
        lastCheck = System.currentTimeMillis();
    }

    public boolean isPresent(LocalDate probe) {
        SparqlSource source = AppConfig.getApp().getA(SparqlSource.class);
        if (source == null) {
            log.error("Fatal configuration error: can't find source to query from");
            return true;
        }

        // Probe tests 3 days in the month so as to be sure to avoid weekends or other quiet days
        String probeQuery = SRQueryFactory.get().getRaw("latestMonthProbe.sq");
        probeQuery = probeQuery.replace("?first", probe.format(DateTimeFormatter.ISO_LOCAL_DATE));
        LocalDate second = probe.plus(1, ChronoUnit.DAYS);
        probeQuery = probeQuery.replace("?second", second.format(DateTimeFormatter.ISO_LOCAL_DATE));
        LocalDate third = second.plus(1, ChronoUnit.DAYS);
        probeQuery = probeQuery.replace("?third", third.format(DateTimeFormatter.ISO_LOCAL_DATE));

        return source.ask(probeQuery);
    }

    public static void clearCache() {
        latestMonth = null;
    }
}
