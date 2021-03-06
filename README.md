# Standard Reports Manager

Reports manager for constructing batch reports.

Implements both report submission/status tracking web service and backend report processor.

## Endpoints provided

    POST /sr-manager/report-request?...

submit a request (defined by query parameters) via POST, returns location request status information.

    GET /sr-manager/report-request/{id}

returns status information in JSON for the identified request.

    GET /sr-manager/report/{id}.{csv|xlsx}

returns a prepared report (may be replaced by S3 endpoint in the future).

    GET /sr-manager/latest-month-available

returns the most recent month for which data is available as a plain string (e.g. "2015-09").

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

## Development setup

To run locally but point queries at the dev server set up a local tunnel:

    ssh -f lr-data-staging-b -L 3030:ec2-54-246-79-255.eu-west-1.compute.amazonaws.com:3030 -N

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
