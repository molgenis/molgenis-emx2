package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.*;

import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.Trigger;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import org.molgenis.emx2.analytics.model.actions.DeleteTriggerAction;
import org.molgenis.emx2.analytics.model.actions.UpdateTriggerAction;
import org.molgenis.emx2.analytics.repository.TriggerRepositoryImpl;
import org.molgenis.emx2.analytics.service.AnalyticsServiceImpl;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.transformers.ActionTransformer;
import org.molgenis.emx2.web.transformers.JsonTransformer;
import spark.Request;
import spark.Response;

public class AnalyticsApi {

  private static final ActionTransformer actionTransformer = new ActionTransformer();
  public static final String STATUS_SUCCESS = "{\"status\": \"success\"}";
  public static final String STATUS_FAILED = "{\"status\": \"failed\"}";
  private static final String TRIGGER_PARAM = ":trigger";

  public static void create() {

    new TriggerRepositoryImpl(new SqlDatabase(false));

    JsonTransformer jsonTransformer = new JsonTransformer();

    post("/:schema/api/trigger", AnalyticsApi::addTrigger, jsonTransformer);
    get("/:schema/api/trigger", AnalyticsApi::listSchemaTriggers, jsonTransformer);
    delete("/:schema/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::deleteTrigger, jsonTransformer);
    put("/:schema/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::updateTrigger, jsonTransformer);
  }

  private static String deleteTrigger(Request request, Response response) {
    var action = new DeleteTriggerAction(sanitize(request.params(TRIGGER_PARAM)));
    MolgenisSession session = sessionManager.getSession(request);
    String schemaName = sanitize(request.params(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);

    return analyticsService.deleteTriggerForSchema(schema, action) ? STATUS_SUCCESS : STATUS_FAILED;
  }

  private static List<Trigger> listSchemaTriggers(Request request, Response response) {
    MolgenisSession session = sessionManager.getSession(request);
    String schemaName = sanitize(request.params(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);

    return triggerRepository.getTriggersForSchema(schema);
  }

  private static String addTrigger(Request request, Response response) {
    var createTriggerAction =
        actionTransformer.transform(request.body(), CreateTriggerAction.class);
    MolgenisSession session = sessionManager.getSession(request);
    String schemaName = sanitize(request.params(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);
    analyticsService.createTriggerForSchema(schema, createTriggerAction);

    return STATUS_SUCCESS;
  }

  private static String updateTrigger(Request request, Response response) {
    var action = actionTransformer.transform(request.body(), UpdateTriggerAction.class);
    MolgenisSession session = sessionManager.getSession(request);
    String schemaName = sanitize(request.params(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);
    analyticsService.updateTriggerForSchema(
        schema, sanitize(request.params(TRIGGER_PARAM)), action);

    return STATUS_SUCCESS;
  }
}
