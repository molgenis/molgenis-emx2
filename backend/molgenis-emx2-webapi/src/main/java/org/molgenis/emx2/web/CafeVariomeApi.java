package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MOLGENIS_OIDC_CLIENT_NAME;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.servlet.JavalinServletContext;
import io.javalin.http.servlet.Task;
import java.util.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.endpoints.FilteringTerms;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.cafevariome.CafeVariomeQuery;
import org.molgenis.emx2.cafevariome.QueryRecord;
import org.molgenis.emx2.cafevariome.response.RecordIndexResponse;
import org.molgenis.emx2.cafevariome.response.RecordResponse;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.javalin.JavalinFrameworkParameters;

public class CafeVariomeApi {

  private static MolgenisSessionManager sessionManager;
  private static final Config config = new SecurityConfigFactory().build();
  private static final String clientName =
      (String) EnvironmentProperty.getParameter(MOLGENIS_OIDC_CLIENT_NAME, "MolgenisAuth", STRING);

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;
    app.before("/{schema}/api/cafevariome/record", CafeVariomeApi::checkAuth);
    app.post("/{schema}/api/cafevariome/record", CafeVariomeApi::postRecord);
    app.before("/{schema}/api/cafevariome/record-index", CafeVariomeApi::checkAuth);
    app.get("/{schema}/api/cafevariome/record-index", CafeVariomeApi::getRecordIndex);
  }

  private static void checkAuth(Context ctx) {
    MolgenisSession session = sessionManager.getSession(ctx.req());
    if (!session.getDatabase().isAnonymous()) {
      return;
    }

    FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);
    Object result =
        config
            .getSecurityLogic()
            .perform(
                config,
                (context, store, profiles) -> "AUTH_GRANTED",
                clientName,
                null,
                null,
                new JavalinFrameworkParameters(ctx));

    System.out.println("--- AUTH RESULT ---" + result);

    if (!"AUTH_GRANTED".equals(result)) {
      ctx.status(401).result("Unauthorized");
      ((JavalinServletContext) ctx).getTasks().removeIf(Task::getSkipIfExceptionOccurred);
    }
  }

  private static void postRecord(Context ctx) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    CafeVariomeQuery query = mapper.readValue(ctx.body(), CafeVariomeQuery.class);

    Schema schema = getSchema(ctx);
    Object result = QueryRecord.post(schema, query);

    ctx.json(result);
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

    RecordResponse records = QueryRecord.post(schema, new CafeVariomeQuery());
    RecordIndexResponse response =
        new RecordIndexResponse(
            records.recordCount(), new RecordIndexResponse.EavIndex(attributes, values, mappings));

    ctx.json(response);
  }
}
