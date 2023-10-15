package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.semantics.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;
import org.molgenis.emx2.semantics.OntologyTerm;
import org.molgenis.emx2.utils.TypeUtils;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Measures {

  private OntologyTerm assayCode;
  private String date;
  private String measurementVariable;
  private MeasurementValue measurementValue;
  private ObservationMoment observationMoment;

  public static Measures[] get(Object measures) {
    if (measures == null) {
      return null;
    }
    List<Map<String, Object>> measuresCast = (List<Map<String, Object>>) measures;
    Measures[] result = new Measures[measuresCast.size()];
    for (int i = 0; i < measuresCast.size(); i++) {
      Map map = measuresCast.get(i);
      Measures m = new Measures();
      m.assayCode = mapToOntologyTerm((Map) map.get("assayCode"));
      m.date = TypeUtils.toString(map.get("date"));
      m.measurementVariable = TypeUtils.toString(map.get("measurementVariable"));
      m.measurementValue =
          new MeasurementValue(
              (Integer) map.get("measurementValue_value"),
              mapToOntologyTerm((Map) map.get("measurementValue_units")));
      m.observationMoment =
          new ObservationMoment(
              new ISO8601duration(
                  TypeUtils.toString(map.get("observationMoment_age_iso8601duration"))));
      result[i] = m;
    }
    return result;
  }
}
