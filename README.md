# Standard Reports Manager

Reports manager for constructing batch reports.

Implements both report submission/status tracking web service and backend report processor.

## Endpoints provided

### Report processing

Submit a request (defined by query parameters) via POST, returns location request status information:

    POST /sr-manager/report-request?...

Get status information in JSON for the identified request:

    GET /sr-manager/report-request/{id}

Retrieve a prepared report:

    GET /sr-manager/report/{id}.{csv|xlsx}

Get the most recent month for which data is available as a plain string (e.g. "2015-09"):

    GET /sr-manager/latest-month-available


### Management and metrics

Clear the transient cache:

    GET /sr-manager/system/clear-cache

Clear the transient and persistent caches:

    GET /sr-manager/system/clear-cache-all

Prune old completed records from the queue manager:

    POST /sr-manager/system/clear-old-records

Suspend report processing (after current report finishes):

    POST /sr-manager/system/suspend

Resume report processing:

    POST /sr-manager/system/resume

Return the current status of report processing (Running, Suspending, or Suspended):

    POST /sr-manager/system/status

Prometheus metrics scrape endpoint:

    POST /sr-manager/system/metrics

## Requests

Parameter | Values | Default
---|---|---
`areaType` | AreaType (see below) |
`area` | Name of area (use EW for country) |
`aggregate` | AreaType or `none` | `none`
`age` | `new` `old` `any` | `any`
`period` | 2015   2015-Q1  2015-03 |
`report` | `avgPrice` `banded` | `avgPrice`
`sticky` | `true` `false` | `true`
`test` | `true` `false` | `false`

Where AreaType is one of:

   * `country`
   * `region`
   * `county`
   * `district`
   * `pcArea`
   * `pcDistrict`
   * `pcSector`

The `test` flag bypasses all live processing and just uploads a fixed pair of pre-canned reports after a 10s pause. All the other parameter are ignored in this case so long as they are legal.

## Status response

Status is reported as a JSON object will the following fields:

Field | Value | Notes
---|---|---
`key` | id string | unique identifier for report
`status` | `Pending` `InProgress` `Completed` `Failed` `Unknown` |
`positionInQueue` | number | Valid number if Pending, 0 if InProgress, absent otherwise
`eta` | time left in ms | Only available of Pending/InProgress
`started` | date-time string | Time an InProgress request started processing

## Configuration

The configuration of the request processing (queue manager and location, store manager and location, sparql endpoint) is all specified in an appbase configuration file in `/etc/standard-reports/app.conf`. The desired configuration file should be mounted into this location in the container. 

The default configuration is suitable for testing only and uses an in-memory queue, file store manager (in `/tmp`) and assumes a sparql endpoint of `http://localhost:3030/landregistry_to/query`

## Development setup

To run the container locally with the default configuration (sparql endpoint of `http://localhost:3030/landregistry_to/query`):

    AWS_PROFILE=lr docker run -v -p 8080:8080 018852084843.dkr.ecr.eu-west-1.amazonaws.com/epimorphics/standard-reports-manager/dev:${VERSION}

To use the public LR sparql endpoint (which is subject to a 90s/120s timeout) then use the configuration file in `dev/app.conf` e.g.:

    AWS_PROFILE=lr docker run -v $(pwd)/dev/app.conf:/etc/standard-reports/app.conf -p 8080:8080 018852084843.dkr.ecr.eu-west-1.amazonaws.com/epimorphics/standard-reports-manager/dev:${VERSION}

Example test cases:

    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=HAMPSHIRE&aggregate=district&period=2015-Q3&report=avgPrice"

    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=DEVON&aggregate=district&period=2015-06&age=new&report=avgPrice"

    curl -i http://localhost:8080/sr-manager/latest-month-available

    # Large result test case
    curl -i -X POST "http://localhost:8080/sr-manager/report-request?area=EW&period=2015&areaType=country&report=banded&age=any&aggregate=pcSector"

    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=district&area=KENSINGTON+AND+CHELSEA&aggregate=district&period=2015&age=any&report=banded"    

    curl -i -X POST http://localhost:8080/sr-manager/system/suspend
    curl -i -X POST http://localhost:8080/sr-manager/system/resume
    curl -i http://localhost:8080/sr-manager/system/status


Test sequence:
    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=HAMPSHIRE&aggregate=district&period=2015-Q3&report=avgPrice"
    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=DEVON&aggregate=district&period=2015-06&age=new&report=avgPrice"
    curl -i -X POST http://localhost:8080/sr-manager/system/suspend
    curl -i http://localhost:8080/sr-manager/system/status

    curl -i -X POST http://localhost:8080/sr-manager/system/resume

Extra band check case:

curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=district&area=KENSINGTON+AND+CHELSEA&aggregate=district&period=2015-Q4&age=new&report=banded" 

Should have:

1001k - 1250k  1
1251k - 1500k  1
1501k - 1750k  2
1751k - 2000k  7
