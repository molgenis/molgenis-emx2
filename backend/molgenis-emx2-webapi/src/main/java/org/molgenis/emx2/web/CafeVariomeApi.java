package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.endpoints.FilteringTerms;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.cafevariome.CafeVariomeQuery;
import org.molgenis.emx2.cafevariome.QueryRecord;

public class CafeVariomeApi {

  public static void create(Javalin app) {
    app.post("/{schema}/api/cafevariome/record", CafeVariomeApi::postRecord);
    app.get("/{schema}/api/cafevariome/eav-index", CafeVariomeApi::getEavIndex);
  }

  private static void getEavIndex(Context ctx) {
    Schema schema = getSchema(ctx);

    Database database = sessionManager.getSession(ctx.req()).getDatabase();

    Set<FilteringTerm> filteringTermsSet = new HashSet<>();
    new FilteringTerms(database)
        .getResponse()
        .getFilteringTermsFromTables(
            database, List.of("Individuals"), filteringTermsSet, schema.getName());

    ctx.json(filteringTermsSet.toArray(new FilteringTerm[0]));
  }

  private static void postRecord(Context ctx) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    CafeVariomeQuery query = mapper.readValue(ctx.body(), CafeVariomeQuery.class);

    Schema schema = getSchema(ctx);
    Object result = QueryRecord.post(schema, query);

    ctx.json(result);
  }
}
