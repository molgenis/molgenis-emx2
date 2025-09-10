package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.analytics.model.actions.CreateTriggerAction;
import org.molgenis.emx2.analytics.model.actions.DeleteTriggerAction;
import org.molgenis.emx2.analytics.model.actions.UpdateTriggerAction;
import org.molgenis.emx2.analytics.repository.TriggerRepositoryImpl;
import org.molgenis.emx2.analytics.service.AnalyticsServiceImpl;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.response.ResponseStatus;
import org.molgenis.emx2.web.response.Status;
import org.molgenis.emx2.web.transformers.ActionTransformer;

public class AnalyticsApi {

  private static final ActionTransformer actionTransformer = new ActionTransformer();
  public static final ResponseStatus STATUS_SUCCESS = new ResponseStatus(Status.SUCCESS);
  public static final ResponseStatus STATUS_FAILED = new ResponseStatus(Status.FAILED);
  private static final String TRIGGER_PARAM = "{trigger}";

  private AnalyticsApi() {
    // hide constructor
  }

  public static void create(Javalin app) {

    new TriggerRepositoryImpl(new SqlDatabase(false));

    app.post("/{schema}/api/trigger", AnalyticsApi::addTrigger);
    app.get("/{schema}/api/trigger", AnalyticsApi::listSchemaTriggers);
    app.delete("/{schema}/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::deleteTrigger);
    app.put("/{schema}/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::updateTrigger);

    app.post("apps/{app}/{schema}/api/trigger", AnalyticsApi::addTrigger);
    app.get("apps/{app}/{schema}/api/trigger", AnalyticsApi::listSchemaTriggers);
    app.delete("apps/{app}/{schema}/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::deleteTrigger);
    app.put("apps/{app}/{schema}/api/trigger/" + TRIGGER_PARAM, AnalyticsApi::updateTrigger);
  }

  private static void deleteTrigger(Context ctx) {
    ctx.contentType("application/json");
    var action = new DeleteTriggerAction(sanitize(ctx.pathParam(TRIGGER_PARAM)));
    MolgenisSession session = sessionManager.getSession(ctx.req());
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);

    ctx.json(
        analyticsService.deleteTriggerForSchema(schema, action) ? STATUS_SUCCESS : STATUS_FAILED);
  }

  private static void listSchemaTriggers(Context ctx) {
    ctx.contentType("application/json");
    MolgenisSession session = sessionManager.getSession(ctx.req());
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);

    ctx.json(triggerRepository.getTriggersForSchema(schema));
  }

  private static void addTrigger(Context ctx) {
    ctx.contentType("application/json");
    CreateTriggerAction createTriggerAction =
        actionTransformer.transform(ctx.body(), CreateTriggerAction.class);
    MolgenisSession session = sessionManager.getSession(ctx.req());
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);
    analyticsService.createTriggerForSchema(schema, createTriggerAction);

    ctx.json(STATUS_SUCCESS);
  }

  private static void updateTrigger(Context ctx) {
    ctx.contentType("application/json");
    UpdateTriggerAction action = actionTransformer.transform(ctx.body(), UpdateTriggerAction.class);
    MolgenisSession session = sessionManager.getSession(ctx.req());
    String schemaName = sanitize(ctx.pathParam(SCHEMA));
    Database database = session.getDatabase();
    Schema schema = database.getSchema(schemaName);

    TriggerRepositoryImpl triggerRepository = new TriggerRepositoryImpl(database);
    AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(triggerRepository);
    analyticsService.updateTriggerForSchema(schema, sanitize(ctx.pathParam(TRIGGER_PARAM)), action);

    ctx.json(STATUS_SUCCESS);
  }
}
