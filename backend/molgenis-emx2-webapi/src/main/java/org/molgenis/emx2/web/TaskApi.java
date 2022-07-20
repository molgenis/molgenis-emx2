package org.molgenis.emx2.web;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.delete;
import static spark.Spark.get;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.tasks.*;
import spark.Request;
import spark.Response;

// TODO make the tasks private to schema; then you need schema edit or manager to view them
// TODO move into graphql api, so it is documentation and less silly
public class TaskApi {

  // todo, make jobs private to the user?
  public static TaskService taskService = new TaskServicePersisted();

  public static void create() {
    get("/api/tasks", TaskApi::listTasks);
    get("/api/tasks/clear", TaskApi::clearTasks);
    get("/api/task/:id", TaskApi::getTask);

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

  private static String clearTasks(Request request, Response response) {
    if (getSchema(request) != null) {
      taskService.clear();
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String deleteTask(Request request, Response response) {
    if (getSchema(request) != null) {
      taskService.removeTask(request.params("id"));
      return "{status: 'SUCCESS'}";
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String listTasks(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {

      final var schema = request.params("schema");
      String clearUrl = "/" + schema + "/api/tasks/clear";
      String result = format("{\"clearUrl\":\"%s\", \"tasks\":[", clearUrl);

      result +=
          taskService.listTaskInfos().stream()
              .map(info -> taskInfoToJson(info, schema))
              .collect(joining(","));

      result += "]}";
      return result;
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String taskInfoToJson(TaskInfo taskInfo, String schema) {
    String getUrl = "/" + schema + "/api/task/" + taskInfo.getId();
    String deleteUrl = getUrl + "/delete";
    // TODO can't this be mapped automatically instead of doing it ourselves?
    return format(
        "{\"id\":\"%s\", \"description\":\"%s\", \"status\":\"%s\", \"url\":\"%s\", \"deleteUrl\":\"%s\"}",
        taskInfo.getId(), taskInfo.getDescription(), taskInfo.getStatus(), getUrl, deleteUrl);
  }

  private static String getTask(Request request, Response response) {
    if (request.params("schema") == null || getSchema(request) != null) {
      final var id = request.params("id");
      TaskInfo step = taskService.getTaskInfo(id);
      if (step == null) {
        throw new MolgenisException(format("Unknown task: %s", id));
      }
      return step.toString();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  public static String submit(Task task) {
    return taskService.submit(task);
  }
}
