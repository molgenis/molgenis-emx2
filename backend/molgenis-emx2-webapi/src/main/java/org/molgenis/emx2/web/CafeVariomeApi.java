package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.endpoints.FilteringTerms;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.cafevariome.CafeVariomeQuery;
import org.molgenis.emx2.cafevariome.QueryRecord;
import org.molgenis.emx2.cafevariome.response.RecordIndexResponse;
import org.molgenis.emx2.cafevariome.response.RecordResponse;

public class CafeVariomeApi {

  public static void create(Javalin app) {
    app.post("/{schema}/api/cafevariome/record", CafeVariomeApi::postRecord);
    app.get("/{schema}/api/cafevariome/eav-index", CafeVariomeApi::getRecordIndex);
  }

  private static void getRecordIndex(Context ctx) {
    Schema schema = getSchema(ctx);

    Database database = sessionManager.getSession(ctx.req()).getDatabase();

    Map<String, String> attributes = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    Map<String, List<String>> mappings = new HashMap<>();

    Set<FilteringTerm> filteringTermsSet = new HashSet<>();
    new FilteringTerms(database)
        .getResponse()
        .getFilteringTermsFromTables(
            database, List.of("Individuals"), filteringTermsSet, schema.getName());

    for (FilteringTerm filteringTerm : filteringTermsSet) {
      if (filteringTerm.getType().equals("ontology")) {
        attributes.put(filteringTerm.getLabel(), filteringTerm.getLabel());
        values.put(filteringTerm.getId(), filteringTerm.getLabel());
        if (mappings.containsKey(filteringTerm.getId())) {
          List<String> terms = mappings.get(filteringTerm.getId());
          terms.add(filteringTerm.getLabel());
        } else {
          mappings.put(filteringTerm.getId(), List.of(filteringTerm.getLabel()));
        }
      } else if (filteringTerm.getType().equals("alphanumeric")) {
        attributes.put(filteringTerm.getId(), filteringTerm.getId());
      }
    }

    RecordResponse records = QueryRecord.post(schema, new CafeVariomeQuery());
    RecordIndexResponse response =
        new RecordIndexResponse(
            records.recordCount(), new RecordIndexResponse.EavIndex(attributes, values, mappings));

    ctx.json(response);
  }

  private static void postRecord(Context ctx) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    CafeVariomeQuery query = mapper.readValue(ctx.body(), CafeVariomeQuery.class);

    Schema schema = getSchema(ctx);
    Object result = QueryRecord.post(schema, query);

    ctx.json(result);
  }
}
