/******************************************************************
 * File:        SubmitEndpoint.java
 * Created by:  Dave Reynolds
 * Created on:  18 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.webapi;

import static com.epimorphics.standardReports.Constants.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.armlib.BatchStatus;
import com.epimorphics.armlib.BatchStatus.StatusFlag;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.simpleAPI.requests.Request;
import com.epimorphics.simpleAPI.requests.RequestCheck;

@Path("report-request")
public class ReportRequestEndpoint extends SREndpointBase {
    static Logger log = LoggerFactory.getLogger( ReportRequestEndpoint.class );
    
    public static final String URL_KEY   = "url";
    public static final String XLS_URL_KEY   = "urlXlsx";
    
    protected static RequestCheck validator;
    
    static {
        try {
            InputStream specIS = new FileInputStream( AppConfig.getAppConfig().expandFileLocation("{webapp}/WEB-INF/request-validator.yaml") );
//        InputStream specIS = ClassLoader.class.getResourceAsStream("/request-validator.yaml");
            JsonObject spec = JsonUtil.asJson( new Yaml().load( specIS ) ).getAsObject();
            validator = RequestCheck.fromJson(spec);
        } catch (IOException e) {
            log.error("Fatal configuration error: could not locate request-validator.yaml");
        }
    }
    
    @POST
    public Response submitRequest() {
        Request request = getRequest();
        BatchStatus bs = getRequestManager().submit( makeBatchRequest(request) );
        String statusURL = context.getContextPath() + "/report-request/" + bs.getKey();
        try {
            return Response.created( new URI(statusURL) ).build();
        } catch (URISyntaxException e) {
            // "Can't happen"
            throw new WebApiException(Status.INTERNAL_SERVER_ERROR, "Illegal internal URI: " + statusURL);
        }
    }
    
    public static BatchRequest makeBatchRequest(Request request) {
        BatchRequest br = new BatchRequest("report-request", request.getParametersMap());
        if (validator != null) validator.checkRequest(request);
        StringBuffer keyBuf = new StringBuffer();
        keyBuf.append( request.getFirst(REPORT) );
        String areaType = request.getFirst(AREA_TYPE);
        keyBuf.append( "-" + areaType );
        String area = request.getFirst(AREA);
        keyBuf.append( "-" + area.replace(" ", "_"));
        keyBuf.append( "-by-" + request.getFirst(AGGREGATE) );
        keyBuf.append( "-" + request.getFirst(AGE) );
        keyBuf.append( "-" + request.getFirst(PERIOD) );
        br.setKey( keyBuf.toString() );
        
        br.setSticky( request.getFirst(STICKY).equalsIgnoreCase("true"));
        
        if (areaType.equals(AT_COUNTRY)) {
            br.setEstimatedTime(1000 * 60 * 15);
        } else if (areaType.equals(AT_REGION)) {
            br.setEstimatedTime(1000 * 60 * 4);
        } else if (areaType.equals(AT_COUNTY)){
            if (area.contains("LONDON")) {
                br.setEstimatedTime(1000 * 60 * 5);
            } else {
                br.setEstimatedTime(1000 * 30);
            }
        } else {
            br.setEstimatedTime(1000 * 15);
        }
        
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
