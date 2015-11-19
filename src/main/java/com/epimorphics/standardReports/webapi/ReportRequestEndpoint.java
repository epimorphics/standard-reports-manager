/******************************************************************
 * File:        SubmitEndpoint.java
 * Created by:  Dave Reynolds
 * Created on:  18 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.armlib.BatchStatus;
import com.epimorphics.armlib.BatchStatus.StatusFlag;
import com.epimorphics.simpleAPI.requests.Request;

@Path("reportRequest")
public class ReportRequestEndpoint extends SREndpointBase {
    public static final String AREA_TYPE = "areaType";
    public static final String AREA      = "area";
    public static final String AGGREGATE = "aggregate";
    public static final String AGE       = "age";
    public static final String PERIOD    = "period";
    public static final String REPORT    = "report";
    public static final String STICKY    = "sticky";
    
    public static final String URL_KEY   = "url";
    public static final String XLS_URL_KEY   = "urlXlsx";

    @POST
    public Response submitRequest() {
        Request request = getRequest();
        BatchStatus bs = getRequestManager().submit( makeBatchRequest(request) );
        String statusURL = context.getContextPath() + "/reportRequest/" + bs.getKey();
        try {
            return Response.created( new URI(statusURL) ).build();
        } catch (URISyntaxException e) {
            // "Can't happen"
            throw new WebApiException(Status.INTERNAL_SERVER_ERROR, "Illegal internal URI: " + statusURL);
        }
    }
    
    public static BatchRequest makeBatchRequest(Request request) {
        BatchRequest br = new BatchRequest("reportRequest", request.getParametersMap());
        // TODO validation and defaults
        StringBuffer keyBuf = new StringBuffer();
        keyBuf.append( request.getFirst(REPORT) );
        keyBuf.append( "-" + request.getFirst(AREA_TYPE) );
        keyBuf.append( "-" + request.getFirst(AREA).replace(" ", "_"));
        keyBuf.append( "-by-" + request.getFirst(AGGREGATE) );
        keyBuf.append( "-" + request.getFirst(AGE) );
        keyBuf.append( "-" + request.getFirst(PERIOD) );
        br.setKey( keyBuf.toString() );
        return br;
    }
    
    @Path("{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getStatus(@PathParam("key") String key) {
        BatchStatus status = getRequestManager().getFullStatus(key);
        if (status.getStatus() == StatusFlag.Unknown) {
            throw new NotFoundException();
        }
        JsonObject statusj = status.asJson();
        // Fix up the URLs for the dual result versions
        if (statusj.hasKey(URL_KEY)) {
            String url = uriInfo.getBaseUri() + statusj.get(URL_KEY).getAsString().value();
            statusj.put(URL_KEY, url);
            statusj.put(XLS_URL_KEY, url.replaceAll("\\.csv$", ".xlsx"));
        }
        return statusj;
    }
}