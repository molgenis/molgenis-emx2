package org.molgenis.emx2.datamodels.cafevarime;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.cafevariome.QueryRecord;
import org.molgenis.emx2.cafevariome.response.RecordIndexResponse;
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

  @Test
  void testDiagnosisBool_shouldExist() throws JsonProcessingException {
    String request =
        """
        {
          "hpo": [
            {
              "terms": [
                "1955"
              ]
            }
          ],
          "advanced": {
            "granularity": "boolean"
          }
        }

        """;
    RecordResponse response = QueryRecord.post(patientRegistry, request);
    assertTrue(response.exist());
  }

  @Test
  void testDiagnosisBool_shouldNotExist() throws JsonProcessingException {
    String request =
        """
        {
          "hpo": [
            {
              "terms": [
                "1956"
              ]
            }
          ],
          "advanced": {
            "granularity": "boolean"
          }
        }

        """;
    RecordResponse response = QueryRecord.post(patientRegistry, request);
    assertFalse(response.exist());
  }

  @Test
  void testDetailedList_shouldThrow() {
    String request =
        """
        {
          "ordo": [
            {
              "terms": [
                "1956"
              ],
              "useHPO": true
            }
          ],
          "genes": [
            {
              "alleles": [
                {
                  "gene": "SNORD1"
                }
              ]
            }
          ],
          "variant": {
            "maxAf": 0.1,
            "useLocalAf": false
          },
          "advanced": {
            "granularity": "list"
          }
        }

        """;
    assertThrows(MolgenisException.class, () -> QueryRecord.post(patientRegistry, request));
  }

  @Test
  void testNoFilters() throws JsonProcessingException {
    String request =
        """
        {
          "advanced": {
            "granularity": "count",
            "requiredFilters": {
              "subject": false,
              "hpo": false,
              "ordo": false,
              "genes": false,
              "snomed": false,
              "variant": false,
              "source": false,
              "eav": false,
              "subjectCapability": {
                "age": false,
                "gender": false,
                "familyType": false,
                "affected": false
              }
            }
          }
        }
        """;
    RecordResponse response = QueryRecord.post(patientRegistry, request);
    assertEquals(response.recordCount(), 23);
  }

  @Test
  void testRecordIndex() {
    RecordIndexResponse indexResponse = QueryRecord.getRecordIndex(database, patientRegistry);
    assertEquals(23, indexResponse.recordCount());
    assertTrue(indexResponse.capability().subject());
    assertTrue(indexResponse.capability().eav());
    assertTrue(indexResponse.eavIndex().attributes().containsKey("date of birth"));
  }
}
