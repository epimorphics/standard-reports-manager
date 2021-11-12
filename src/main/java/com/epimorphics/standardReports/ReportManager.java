/******************************************************************
 * File:        ReportManager.java
 * Created by:  Dave Reynolds
 * Created on:  18 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports;

import static com.epimorphics.standardReports.Constants.REPORT;
import static com.epimorphics.standardReports.Constants.REPORT_BYPRICE;
import static com.epimorphics.standardReports.Constants.TEST_PARAM;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import com.epimorphics.appbase.core.Shutdown;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.jena.query.ResultSet;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.armlib.CacheManager;
import com.epimorphics.armlib.QueueManager;
import com.epimorphics.armlib.RequestManager;
import com.epimorphics.standardReports.aggregators.AveragePriceAggregator;
import com.epimorphics.standardReports.aggregators.BandedPriceAggregator;
import com.epimorphics.standardReports.aggregators.SRAggregator;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.NameUtils;

/**
 * The manager for requesting and generating standard reports.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ReportManager extends ComponentBase implements Startup, Shutdown {
    public static final String MOCK_DATA = "{webapp}/mockData/All_Postcode_Districts_within_GREATER_LONDON";

    protected static int RETRY_LIMIT = 2;

    static Logger log = LoggerFactory.getLogger(ReportManager.class);

    protected RequestManager requestManager;
    protected SRQueryFactory srQueryFactory;
    protected File workDir;
    protected File templateDir;
    protected long recordRentionPeriod = 30;
    protected boolean suspend = false;
    protected boolean suspended = false;

    public enum Status {
        Running, Suspending, Suspended
    };

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    protected PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    protected Counter metricFailed =Counter
            .builder("reports.failed")
            .description("number of reports which started but failed")
            .register(registry);
    protected Counter metricProcessed =Counter
            .builder("reports.processed")
            .description("number of reports processed, includes retries and fails")
            .register(registry);
    protected Counter metricRetries =Counter
            .builder("reports.retries")
            .description("number of reports which had to be retried")
            .register(registry);
    protected Timer metricTimer = Timer
            .builder("reports.duration")
            .description("timer mentics on how long reports are taking to run")
            .register(registry);

    /**
     * Time for which to keep old completion record (in days)
     */
    public long getRecordRetentionPeriod() {
        return recordRentionPeriod;
    }

    public void setRecordRetentionPeriod(long recordRetentionPeriod) {
        this.recordRentionPeriod = recordRetentionPeriod;
    }

    public void setTemplateDir(String templateDir) {
        String td = expandFileLocation(templateDir);
        srQueryFactory = new SRQueryFactory(td);
        this.templateDir = new File(td);
    }

    public void setWorkDir(String workDir) {
        String dir = expandFileLocation(workDir);
        this.workDir = new File(dir);
        FileUtil.ensureDir(dir);
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }
    /**
     * Request processor stop after current request
     */
    public synchronized void suspend() {
        suspend = true;
    }
    
    /**
     * Request processor restart after a suspension
     */
    public synchronized void resume() {
        suspend = false;
        if (suspended) {
            suspended = false;
            log.info("Requesting resumption of batch processing");
            TimerManager.get().schedule(new RequestProcessor(), 1,
                    TimeUnit.SECONDS);    
        }
    }

    /**
     * Check current processing state
     */
    public synchronized Status getStatus() {
        if (suspend) {
            return suspended ? Status.Suspended : Status.Suspending;
        } else {
            return Status.Running;
        }
    }

    @Override
    public void startup(App app) {
        super.startup(app);
        TimerManager.get().schedule(new RequestProcessor(), 1,
                TimeUnit.SECONDS);
        initMetrics();
    }

    @Override
    public void shutdown() {
        TimerManager.get().shutdown();
    }

    private void initMetrics() {
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
    }

    public SRQuery getQuery(String templateName) {
        return srQueryFactory.get(templateName);
    }

    public String getRawQuery(String templateName) {
        return FileManager.get().readWholeFileAsUTF8(
                new File(templateDir, templateName).getPath());
    }

    public class RequestProcessor implements Runnable {

        @Override
        public void run() {
            QueueManager queue = requestManager.getQueueManager();
            CacheManager cache = requestManager.getCacheManager();
            SparqlSource source = AppConfig.getApp().getA(SparqlSource.class);
            if (source == null) {
                log.error(
                        "Fatal configuration error: can't find source to query from");
                return;
            }

            log.info("Request processor starting");
            try {
                while ( ! suspend ) {
                    BatchRequest request = queue.nextRequest(1000);
                    if (request != null) {
                        log.info("Processing request: " + request.getKey());

                        if (request.getParameters().containsKey(TEST_PARAM)) {
                            // Dummy delay for mock up
                            Thread.sleep(10000);
                            cache.upload(request, "csv", new File(
                                    expandFileLocation(MOCK_DATA) + ".csv"));
                            cache.upload(request, "xlsx", new File(
                                    expandFileLocation(MOCK_DATA) + ".xlsx"));

                        } else {
                            String reportType = request.getParameters()
                                    .getFirst(REPORT);
                            String queryTemplate = reportType + ".sq";
                            SRQuery query = srQueryFactory.get(queryTemplate);

                            if (query == null) {
                                log.error(
                                        "Fatal configuration error: can't find query - "
                                                + queryTemplate);
                                queue.failRequest(request.getKey());

                            } else {
                                query = query.bindRequest(request);
                                boolean succeeded = false;
                                metricProcessed.increment();
                                for (int retry = 0; retry < RETRY_LIMIT
                                        && !succeeded; retry++) {
                                    try {
                                        long start = System.currentTimeMillis();
                                        String queryStr = query.getQuery();
                                        log.info("Running query: " + queryStr);
                                        ResultSet results = source
                                                .select(queryStr);
                                        SRAggregator agg = reportType
                                                .equals(REPORT_BYPRICE)
                                                        ? new AveragePriceAggregator()
                                                        : new BandedPriceAggregator();
                                        while (results.hasNext()) {
                                            agg.add(results.next());
                                        }

                                        File file = new File(workDir,
                                                request.getKey() + ".csv");
                                        agg.writeAsCSV(
                                                new FileOutputStream(file),
                                                request.getParameters());
                                        cache.upload(request, "csv", file);
                                        file.delete();

                                        file = new File(workDir,
                                                request.getKey() + ".xlsx");
                                        agg.writeAsExcel(
                                                new FileOutputStream(file),
                                                request.getParameters());
                                        cache.upload(request, "xlsx", file);
                                        file.delete();

                                        long duration = System
                                                .currentTimeMillis() - start;
                                        log.info("Request completed: "
                                                + request.getKey() + " in "
                                                + NameUtils.formatDuration(
                                                        duration));
                                        metricTimer.record(duration, TimeUnit.MILLISECONDS);
                                        queue.finishRequest(request.getKey());
                                        succeeded = true;

                                    } catch (Exception e) {
                                        if (retry < RETRY_LIMIT - 1) {
                                            log.warn(
                                                    "Request "
                                                            + request.getKey()
                                                            + " failed, retrying after 10s",
                                                    e);
                                            metricRetries.increment();
                                            Thread.sleep(10000);
                                        } else {
                                            log.error("Request "
                                                    + request.getKey()
                                                    + " failed", e);
                                            queue.failRequest(request.getKey());
                                            metricFailed.increment();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.info("Request processor interrupted");
            }
            suspended = true;
            log.info("Request processing halted");
        }
    }
}
