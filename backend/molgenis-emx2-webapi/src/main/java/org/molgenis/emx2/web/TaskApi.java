package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.utils.URIUtils.extractHost;
import static org.molgenis.emx2.web.FileApi.addFileColumnToResponse;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.*;

// TODO make the tasks private to schema; then you need schema edit or manager to view them
// TODO move into graphql api, so it is documentation and less silly
public class TaskApi {

  // todo, make jobs private to the user?
  public static TaskService taskService = new TaskServiceInDatabase(SYSTEM_SCHEMA);
  // to schedule jobs, see MolgenisSessionManager how we keep this in sync with Database using a
  // TableListener
  public static TaskServiceScheduler taskSchedulerService = new TaskServiceScheduler(taskService);

  public static void create(Javalin app) {
    app.get("/api/tasks", TaskApi::listTasks);
    app.get("/api/tasks/clear", TaskApi::clearTasks);
    app.get("/api/tasks/scheduled", TaskApi::viewScheduledTasks);
    app.get("/api/scripts/{name}", TaskApi::getScript); // run synchronously, with parameters on url
    app.post(
        "/api/scripts/{name}",
        TaskApi::postScript); // run async, using parameters as body and returning task status
    app.get("/api/tasks/{id}", TaskApi::getTask);
    app.get("/api/tasks/{id}/output", TaskApi::getTaskOutput);

    // convenient delete
    app.delete("/api/tasks/{id}", TaskApi::deleteTask);
    app.get("/api/tasks/{id}/delete", TaskApi::deleteTask);

    // also works in context schema
    // todo: make tasks scoped?
    app.get("/{schema}/api/tasks", TaskApi::listTasks);
    app.get("/{schema}/api/tasks/clear", TaskApi::clearTasks);
    app.get("/{schema}/api/tasks/{id}", TaskApi::getTask);
    app.get(
        "/{schema}/api/scripts/{name}",
        TaskApi::getScript); // run synchronously, with parameters on url
    app.post(
        "/{schema}/api/scripts/{name}",
        TaskApi::postScript); // run async, using parameters as body and returning task status
    app.get("/{schema}/api/tasks/{id}/output", TaskApi::getTaskOutput);

    // convenient delete
    app.delete("/{schema}/{app}/api/tasks/{id}", TaskApi::deleteTask);
    app.get("/{schema}/{app}/api/tasks/{id}/delete", TaskApi::deleteTask);

    // also in app
    app.get("/{schema}/{app}/api/tasks", TaskApi::listTasks);
    app.get("/{schema}/{app}/api/tasks/clear", TaskApi::clearTasks);
    app.get("/{schema}/{app}/api/tasks/{id}", TaskApi::getTask);
  }

  private static String viewScheduledTasks(Context ctx) throws JsonProcessingException {
    // mainly for testing/verification purposes
    return new ObjectMapper().writeValueAsString(taskSchedulerService.scheduledTaskNames());
  }

  private static String postScript(Context ctx) throws MalformedURLException {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {
      MolgenisSession session = sessionManager.getSession(ctx.req());
      String user = session.getSessionUser();
      if (!"admin".equals(user)) {
        throw new MolgenisException("Submit task failed: for now can only be done by 'admin");
      }
      String name = URLDecoder.decode(ctx.pathParam("name"), StandardCharsets.UTF_8);
      String parameters = ctx.body();

      URL host = new URL(extractHost(ctx.url()));
      String id = taskService.submitTaskFromName(name, parameters, host);
      return new TaskReference(id).toString();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static byte[] getScript(Context ctx)
      throws InterruptedException, UnsupportedEncodingException {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {
      MolgenisSession session = sessionManager.getSession(ctx.req());
      String user = session.getSessionUser();
      if (!"admin".equals(user)) {
        throw new MolgenisException("Submit task failed: for now can only be done by 'admin");
      }
      String name = URLDecoder.decode(ctx.pathParam("name"), StandardCharsets.UTF_8);
      String parameters =
          ctx.queryParam("parameters") != null
              ? URLDecoder.decode(ctx.queryParam("parameters"), StandardCharsets.UTF_8)
              : null;
      String id = taskService.submitTaskFromName(name, parameters);
      // wait until done or timeout
      int timeout = 0;
      Task task = taskService.getTask(id);
      // timeout a minute for these I would say?
      while (task.isRunning() && timeout < 120) {
        task = taskService.getTask(id);
        timeout++;
        Thread.sleep(500);
      }
      // get the output as bytes
      return ((ScriptTask) task).getOutput();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String getTaskOutput(Context ctx) throws IOException {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {

      MolgenisSession session = sessionManager.getSession(ctx.req());
      Schema adminSchema = session.getDatabase().getSchema(SYSTEM_SCHEMA);
      String jobId = ctx.pathParam("id");
      Row jobMetadata =
          adminSchema
              .getTable("Jobs")
              .query()
              // make sure we include all file metadata
              .select(s("output", s("contents"), s("mimetype"), s("filename"), s("extension")))
              .where(f("id", Operator.EQUALS, jobId))
              .retrieveRows()
              .get(0);

      if (jobMetadata == null) {
        throw new MolgenisException(
            "Get output for task failed: couldn't find task with id " + jobId);
      }
      // reuse implementation from FileApi
      addFileColumnToResponse(ctx, "output", jobMetadata);
      return "";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String clearTasks(Context ctx) {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {
      taskService.clear();
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String deleteTask(Context ctx) {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {
      taskService.removeTask(ctx.pathParam("id"));
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String listTasks(Context ctx) {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {

      String clearUrl = "/" + ctx.pathParam("schema") + "/api/tasks/clear";
      String result = String.format("{\"clearUrl\":\"%s\", \"tasks\":[", clearUrl);

      for (String id : taskService.getJobIds()) {
        Task task = taskService.getTask(id);
        String getUrl = "/" + ctx.pathParam("schema") + "/api/tasks/" + id;
        String deleteUrl = getUrl + "/delete";
        result +=
            String.format(
                "{\"id\":\"%s\", \"description\":\"%s\", \"status\":\"%s\", \"url\":\"%s\", \"deleteUrl\":\"%s\"}",
                id, task.getDescription(), task.getStatus(), getUrl, deleteUrl);
      }
      result += "]}";
      return result;
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String getTask(Context ctx) {
    if (ctx.pathParam("schema").isEmpty() || getSchema(ctx) != null) {
      Task step = taskService.getTask(ctx.pathParam("id"));
      if (step == null) {
        step = new Task("Task unknown").setStatus(TaskStatus.UNKNOWN);
      }
      return step.toString();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  public static String submit(Task task) {
    return taskService.submit(task);
  }
}
