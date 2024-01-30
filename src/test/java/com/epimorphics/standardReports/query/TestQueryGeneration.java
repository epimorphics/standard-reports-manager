package com.epimorphics.standardReports.query;

import com.epimorphics.armlib.BatchRequest;
import com.epimorphics.standardReports.SRQuery;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MultivaluedHashMap;

import static com.epimorphics.standardReports.Constants.*;

public class TestQueryGeneration {

    private BatchRequest makeRequest() {
        MultivaluedHashMap<String, String> params = new MultivaluedHashMap<String, String>();
        params.add(AREA_TYPE, "district");
        params.add(AREA, "KING'S_LYNN_AND_WEST_NORFOLK");
        params.add(AGGREGATE, "none");
        params.add(AGE, "any");
        params.add(PERIOD, "2023-Q1");
        return new BatchRequest("/report-request", params);
    }
    @Test public void testQueryInjection() {
        SRQuery query = new SRQuery("SELECT * WHERE { $_graphPattern }", new PrefixMappingImpl());
        query = query.bindRequest(makeRequest());
        String expected = "SELECT * WHERE {  " +
                "?address <http://landregistry.data.gov.uk/def/common/district> 'KING\\'S_LYNN_AND_WEST_NORFOLK' . " +
                "BIND('KING\\'S_LYNN_AND_WEST_NORFOLK' AS ?area) " +
                "FILTER(?date >= '2023-01-01'^^<http://www.w3.org/2001/XMLSchema#date> && ?date < '2023-04-01'^^<http://www.w3.org/2001/XMLSchema#date>) }";
        assertEquals(expected, query.getQuery());
    }
}
