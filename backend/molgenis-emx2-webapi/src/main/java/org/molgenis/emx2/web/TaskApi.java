package org.molgenis.emx2.web;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.web.FileApi.addFileColumnToResponse;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static spark.Spark.*;

import java.io.IOException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.*;
import spark.Request;
import spark.Response;

// TODO make the tasks private to schema; then you need schema edit or manager to view them
// TODO move into graphql api, so it is documentation and less silly
public class TaskApi {

  // todo, make jobs private to the user?
  public static TaskService taskService = new TaskServiceInDatabase();

  public static void create() {
    get("/api/tasks", TaskApi::listTasks);
    get("/api/tasks/clear", TaskApi::clearTasks);
    post("/api/task", TaskApi::submitTask); // is convention plurar or singular?
    post("/api/tasks", TaskApi::submitTask);
    get("/api/task/:id", TaskApi::getTask);
    get("/api/task/:id/output", TaskApi::getTaskOutput);

    // convenient delete
    delete("/api/task/:id", TaskApi::deleteTask);
    get("/api/task/:id/delete", TaskApi::deleteTask);

    // also works in context schema
    // todo: make tasks scoped?
    get("/:schema/api/tasks", TaskApi::listTasks);
    get("/:schema/api/tasks/clear", TaskApi::clearTasks);
    get("/:schema/api/task/:id", TaskApi::getTask);

    // convenient delete
    delete("/:schema/:app/api/task/:id", TaskApi::deleteTask);
    get("/:schema/:app/api/task/:id/delete", TaskApi::deleteTask);

    // also in app
    get("/:schema/:app/api/tasks", TaskApi::listTasks);
    get("/:schema/:app/api/tasks/clear", TaskApi::clearTasks);
    get("/:schema/:app/api/task/:id", TaskApi::getTask);

    // convenient delete
    delete("/:schema/:app/api/task/:id", TaskApi::deleteTask);
    get("/:schema/:app/api/task/:id/delete", TaskApi::deleteTask);
  }

  private static Object submitTask(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {

      MolgenisSession session = sessionManager.getSession(request);
      String user = session.getSessionUser();
      if (!"admin".equals(user)) {
        throw new MolgenisException("Submit task failed: for now can only be done by 'admin");
      }
      String name = request.queryParams("name");
      String id =
          taskService.submitTaskFromName(
              name, user, JWTgenerator.createTemporaryToken(session.getDatabase(), user));
      return new TaskReference(id).toString();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static Object getTaskOutput(Request request, Response response) throws IOException {
    if (request.params("schema") == null || getSchema(request) != null) {

      MolgenisSession session = sessionManager.getSession(request);
      Schema adminSchema = session.getDatabase().getSchema("ADMIN");
      String jobId = request.params("id");
      Row jobMetadata =
          adminSchema
              .getTable("Jobs")
              .query()
              // make sure we include all file metadata
              .select(s("output", s("contents"), s("mimetype"), s("extension")))
              .where(f("id", Operator.EQUALS, jobId))
              .retrieveRows()
              .get(0);

      if (jobMetadata == null) {
        throw new MolgenisException(
            "Get output for task failed: couldn't find task with id " + jobId);
      }
      // reuse implementation from FileApi
      addFileColumnToResponse(response, "output", jobMetadata);
      return "";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String clearTasks(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {
      taskService.clear();
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String deleteTask(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {
      taskService.removeTask(request.params("id"));
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String listTasks(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {

      String clearUrl = "/" + request.params("schema") + "/api/tasks/clear";
      String result = String.format("{\"clearUrl\":\"%s\", \"tasks\":[", clearUrl);

      for (String id : taskService.getJobIds()) {
        Task task = taskService.getTask(id);
        String getUrl = "/" + request.params("schema") + "/api/task/" + id;
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

  private static String getTask(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {
      Task step = taskService.getTask(request.params("id"));
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
