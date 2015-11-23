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

## Requests

Parameter | Values | Default
---|---|---
`areaType` | AreaType (see below) |
`area` | Name of area (use EW for country) |
`aggregate` | AreaType or `none` | `none`
`age` | `new` `old` `any` | `any`
`period` | 2015   2015-Q1  2015-03 |
`report` | `byPrice` `banded` | `byPrice`
`sticky` | `true` `false` | `true`
`test` | `true` `false` | `false`

Where AreaType is one of:

   * `country`
   * `region`
   * `county`
   * `district`
   * `pc-area`
   * `pc-district`
   * `pc-sector`

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

    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=HAMPSHIRE&aggregate=district&period=2015-Q3&report=byPrice"

    curl -i -X POST "http://localhost:8080/sr-manager/report-request?areaType=county&area=DEVON&aggregate=district&period=2015-06&age=new&report=byPrice"
