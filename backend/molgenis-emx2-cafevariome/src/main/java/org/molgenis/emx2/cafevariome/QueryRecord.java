package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;
import org.molgenis.emx2.cafevariome.response.RecordResponse;

public class QueryRecord {

  private static final String TABLE_ID = EntryType.INDIVIDUALS.getId();

  public static RecordResponse post(Schema schema, String json) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    CafeVariomeQuery query = mapper.readValue(json, CafeVariomeQuery.class);
    return post(schema, query);
  }

  public static RecordResponse post(Schema schema, CafeVariomeQuery query) {
    Table table = schema.getTableById(TABLE_ID);

    List<String> filters = parseFilters(query);
    int count = QueryEntryType.doCountQuery(table, filters);

    return switch (query.advanced().granularity()) {
      case BOOLEAN -> new RecordResponse(null, null, count > 0);
      case COUNT -> new RecordResponse(count, new Range(count, count), count > 0);
      default ->
          throw new MolgenisException(
              "Not implemented granularity: " + query.advanced().granularity());
    };
  }

  private static List<String> parseFilters(CafeVariomeQuery query) {
    List<String> filters = new ArrayList<>();

    if (query.subject() != null && query.subject().ageFirstDiagnosis() != null) {
      String ageAtDiagFilter =
          FilterConceptVP.AGE_AT_DIAG
              .getGraphQlQuery()
              .formatted(
                  "\"P" + query.subject().ageFirstDiagnosis().min() + "Y\"",
                  "\"P" + query.subject().ageFirstDiagnosis().max() + "Y\"");
      filters.add(ageAtDiagFilter);
    }

    if (query.hpo() != null && !query.hpo().isEmpty()) {
      String diseaseTermFilter =
          FilterConceptVP.DISEASE
              .getGraphQlQuery()
              .formatted(query.hpo().getFirst().terms().getFirst());
      filters.add(diseaseTermFilter);
    }

    return filters;
  }
}
