package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Measures {

  private OntologyTerm assayCode;
  private String date;
  private String measurementVariable;
  private MeasurementValue measurementValue;
  private ObservationMoment observationMoment;

  public static Measures[] get(Object measures) {
    List<Map<String, Object>> measuresCast = (List<Map<String, Object>>) measures;
    Measures[] result = new Measures[measuresCast.size()];
    for (int i = 0; i < measuresCast.size(); i++) {
      Map map = measuresCast.get(i);
      Measures m = new Measures();
      m.assayCode = mapToOntologyTerm((Map) map.get("assayCode"));
      m.date = (String) map.get("date");
      m.measurementVariable = (String) map.get("measurementVariable");
      m.measurementValue =
          new MeasurementValue(
              (Integer) map.get("measurementValue__value"),
              mapToOntologyTerm((Map) map.get("measurementValue__units")));
      m.observationMoment =
          new ObservationMoment(
              new ISO8601duration((String) map.get("observationMoment__age__iso8601duration")));
      result[i] = m;
    }
    return result;
  }
}
