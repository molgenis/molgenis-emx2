package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTermsFetcher;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;
import org.molgenis.emx2.cafevariome.response.RecordIndexResponse;
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

  public static RecordIndexResponse getRecordIndex(Database database, Schema schema) {
    RecordResponse records = QueryRecord.post(schema, new CafeVariomeQuery());
    RecordIndexResponse.EavIndex eavIndex =
        mapEavIndex(database, schema, EntryType.INDIVIDUALS.getId());
    return new RecordIndexResponse(records.recordCount(), eavIndex);
  }

  private static RecordIndexResponse.EavIndex mapEavIndex(
      Database database, Schema schema, String tableId) {
    Map<String, String> attributes = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    Map<String, List<String>> mappings = new HashMap<>();

    Set<FilteringTerm> filteringTermsSet =
        new FilteringTermsFetcher(database)
            .getFilteringTermsFromTables(List.of(tableId), schema.getName());

    for (FilteringTerm filteringTerm : filteringTermsSet) {
      if (filteringTerm.getType().equals("ontology")) {
        String filteringTermName = filteringTerm.getColumn().getName();
        attributes.put(filteringTermName, filteringTermName);
        values.put(filteringTerm.getId(), filteringTerm.getLabel());
        if (mappings.containsKey(filteringTermName)) {
          List<String> terms = mappings.get(filteringTermName);
          terms.add(filteringTerm.getId());
        } else {
          List<String> terms = new ArrayList<>();
          terms.add(filteringTerm.getId());
          mappings.put(filteringTermName, terms);
        }
      } else if (filteringTerm.getType().equals("alphanumeric")) {
        attributes.put(filteringTerm.getId(), filteringTerm.getId());
      }
    }
    return new RecordIndexResponse.EavIndex(attributes, values, mappings);
  }
}
