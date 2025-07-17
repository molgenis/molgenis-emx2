package org.molgenis.emx2.datamodels.cafevarime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.cafevariome.QueryRecord;
import org.molgenis.emx2.cafevariome.response.RecordResponse;
import org.molgenis.emx2.datamodels.TestLoaders;

public class QueryRecordTest extends TestLoaders {

  @Test
  void testDiagnosis() throws JsonProcessingException {
    String request =
        """
        {
          "subject": {
            "affectedOnly": false,
            "ageFirstDiagnosis": {
              "min": 18,
              "max": 65
            },
            "familyType": {
              "family": false,
              "singletons": false,
              "trios": false
            },
            "gender": "female"
          },
          "hpo": [
            {
              "terms": [
                "1955"
              ]
            }
          ],
          "advanced": {
            "granularity": "count"
          }
        }

        """;
    RecordResponse response = QueryRecord.post(patientRegistry, request);
    assertEquals(response.recordCount(), 1);
  }
}
