package com.epimorphics.standardReports.webapi;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.util.NameUtils;
import org.slf4j.MDC;

/**
 * A Filter that can be added to filter chain to log all incoming requests and
 * the corresponding response (with response code and execution time). Assigns a
 * simple request number to each request and includes that in the response headers
 * for diagnosis. Not robust against restarts but easier to work with than UUIDs.
 */
public class LogRequestFilter implements Filter {
    public static final String RESPONSE_ID_HEADER  = "X-Response-ID";
    public static final String REQUEST_ID_HEADER  = "X-Request-ID";
    public static final String REQUEST_ID_LOG_FIELD  = "request_id";
    public static final String REPORT_ID_LOG_FIELD  = "report_id";

    static final Logger log = LoggerFactory.getLogger( LogRequestFilter.class );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String path = httpRequest.getRequestURI();
        String query = httpRequest.getQueryString();
        String requestID = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestID != null && !requestID.isEmpty()) {
            MDC.put(REQUEST_ID_LOG_FIELD, requestID);
        }
        long start = System.currentTimeMillis();

        String fullpath = path + (query == null ? "" : ("?" + query));
        MDC.put("path", fullpath);
        // log.info( String.format("Request  [%s] : %s", requestID, fullpath) );
        httpResponse.addHeader(RESPONSE_ID_HEADER, requestID);
        chain.doFilter(request, response);
        int status = httpResponse.getStatus();
        Long duration = System.currentTimeMillis() - start;
        MDC.put("status", Integer.toString(status));
        MDC.put("duration", Long.toString(duration*1000));
        log.info(String.format("Request [%s] : %s : %s", requestID, fullpath, status));
    }

    @Override
    public void destroy() {
    }

    public static WebApiException badRequestException(String message) {
        log.warn("Bad request: " + message);
        return new WebApiException(Status.BAD_REQUEST, message);
    }

    public static WebApiException serverErrorException(String message) {
        log.warn("Server error: " + message);
        return new WebApiException(Status.INTERNAL_SERVER_ERROR, message);
    }

}

