package org.molgenis.emx2.cafevariome.get;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSetsItem;
import spark.Request;

public class CafeVariomeIndexService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  public static IndexResponse index(Request request, List<Table> tables) throws Exception {
    IndexResponse indexResponse = new IndexResponse();
    indexResponse.setSource_id("1");

    List<IndividualsResultSets> individualsResultSets = queryIndividuals(tables);
    Map<String, String> sexDisplayNames = new HashMap<>();
    for (IndividualsResultSets individuals : individualsResultSets) {
      for (IndividualsResultSetsItem individual : individuals.getResults()) {
        OntologyTerm sex = individual.getSex();
        sexDisplayNames.put(sex.URI, sex.getLabel());
      }
    }

    Map<String, String> attributeDisplayNames = new HashMap<>();
    attributeDisplayNames.put("sex", "Gender at birth");

    Map<String, String[]> attributes_values = new HashMap<>();
    attributes_values.put("sex", sexDisplayNames.keySet().toArray(new String[0]));

    indexResponse.setAttributes_values(attributes_values);
    indexResponse.setValues_display_names(sexDisplayNames);
    indexResponse.setAttributes_display_names(attributeDisplayNames);

    return indexResponse;
  }
}
