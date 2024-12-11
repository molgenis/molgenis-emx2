package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.FileApi.addFileColumnToResponse;
import static org.molgenis.emx2.web.MolgenisWebservice.hostUrl;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.*;

// TODO make the tasks private to schema; then you need schema edit or manager to view them
// TODO move into graphql api, so it is documentation and less silly
public class TaskApi {

  // todo, make jobs private to the user?
  public static TaskService taskService = new TaskServiceInDatabase(SYSTEM_SCHEMA, hostUrl);
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

  private static void viewScheduledTasks(Context ctx) throws JsonProcessingException {
    // mainly for testing/verification purposes
    ctx.json(new ObjectMapper().writeValueAsString(taskSchedulerService.scheduledTaskNames()));
  }

  private static void postScript(Context ctx) throws MalformedURLException {
    MolgenisSession session = sessionManager.getSession(ctx.req());
    String user = session.getSessionUser();
    if (!"admin".equals(user)) {
      throw new MolgenisException("Submit task failed: for now can only be done by 'admin");
    }
    String name = URLDecoder.decode(ctx.pathParam("name"), StandardCharsets.UTF_8);
    String parameters = ctx.body();

    String id = taskService.submitTaskFromName(name, parameters);
    ctx.json(new TaskReference(id));
  }

  private static void getScript(Context ctx) throws InterruptedException {
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
    Task task = taskService.getTask(id);
    int maxTimeout = 120;
    int stepSizeInMs = 500;
    int timeout = 0;
    while (task.isRunning() && timeout < maxTimeout) {
      task = taskService.getTask(id);
      timeout++;
      Thread.sleep(stepSizeInMs);
    }
    if (task.isRunning()) {
      throw new MolgenisException(
          "Task timed out after " + (maxTimeout * stepSizeInMs / 1000) + "s");
    }
    if (!task.getStatus().equals(TaskStatus.COMPLETED)) {
      throw new MolgenisException("Task failed with status " + task.getStatus());
    }
    byte[] outputFileBytes = ((ScriptTask) task).getOutput();
    if (outputFileBytes != null) {
      ctx.result(outputFileBytes);
    } else {
      ctx.result("Task succeeded with no output file");
    }
  }

  private static void getTaskOutput(Context ctx) throws IOException {
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
  }

  private static void clearTasks(Context ctx) {
    taskService.clear();
    ctx.result("{status: 'SUCCESS'}");
  }

  private static void deleteTask(Context ctx) {
    taskService.removeTask(ctx.pathParam("id"));
    ctx.result("{status: 'SUCCESS'}");
  }

  private static void listTasks(Context ctx) {
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
    ctx.result(result);
  }

  private static void getTask(Context ctx) {
    Task step = taskService.getTask(ctx.pathParam("id"));
    if (step == null) {
      step = new Task("Task unknown").setStatus(TaskStatus.UNKNOWN);
    }
    ctx.result(step.toString());
  }

  public static String submit(Task task) {
    return taskService.submit(task);
  }

  public static String submit(Task task, String parentTaskId) {
    if (parentTaskId != null) {
      Task parentTask = taskService.getTask(parentTaskId);
      task.setParentTask(parentTask);
    }
    return submit(task);
  }
}
