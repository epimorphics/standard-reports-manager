/******************************************************************
 * File:        SREndpointBase.java
 * Created by:  Dave Reynolds
 * Created on:  18 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.armlib.RequestManager;
import com.epimorphics.simpleAPI.webapi.EndpointsBase;
import com.epimorphics.standardReports.ReportManager;

/**
 * Utilities helpful across several SR endpoints.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SREndpointBase extends EndpointsBase {
    protected ReportManager reportManager;
    
    public ReportManager getReportManager() {
        if (reportManager == null) {
            reportManager = AppConfig.getApp().getA(ReportManager.class);
        }
        return reportManager;
    }
    
    public RequestManager getRequestManager() {
        return getReportManager().getRequestManager();
    }
}

