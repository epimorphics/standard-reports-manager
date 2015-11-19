# Standard Reports Manager

Reports manager for constructing batch reports.

Implements both report submission/status tracking web service and backend report processor.

## Endpoints provided

    POST /sr-manager/reportRequest?...

submit a request (defined by query parameters) via POST, returns location request status information.

    GET /sr-manager/reportRequest/{id}

returns status information in JSON for the identified request.

    GET /sr-manager/report/{id}.{csv|xlsx}

returns a prepared report (may be replaced by S3 endpoint in the future).

## Requests

Parameter | Values | Default
---|---|---
`areaType` | AreaType (see below) |
`area` | Name of area (use EW for country) |
`aggregate` | AreaType |
`age` | `new` `old` `any` | `any`
`period` | 2015   2015-Q1  2015-03 |
`report` | `byPrice` `banded` | `byPrice`
`sticky` | `true` `false` | `true`

Where AreaType is one of:

   * `country`
   * `region`
   * `county`
   * `district`
   * `pc-area`
   * `pc-district`
   * `pc-sector`

## Status response

Status is reported as a JSON object will the following fields:

Field | Value | Notes
---|---|---
`key` | id string | unique identifier for report
`status` | `Pending` `InProgress` `Completed` `Failed` `Unknown` |
`positionInQueue` | number | Valid number if Pending, 0 if InProgress, absent otherwise
`eta` | time left in ms | Only available of Pending/InProgress
`started` | date-time string | Time an InProgress request started processing

