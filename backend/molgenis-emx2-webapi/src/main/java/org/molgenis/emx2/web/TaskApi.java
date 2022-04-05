package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.delete;
import static spark.Spark.get;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.tasks.*;
import spark.Request;
import spark.Response;

// TODO make the tasks private to schema; then you need schema edit or manager to view them
public class TaskApi {

  private static TaskService taskService = new TaskServiceInMemory();

  public static void create() {
    get("/:schema/api/tasks", TaskApi::listTasks);
    get("/:schema/api/tasks/clear", TaskApi::clearTasks);
    get("/:schema/api/task/:id", TaskApi::getTask);

    // convenient delete
    delete("/:schema/api/task/:id", TaskApi::deleteTask);

    get("/:schema/api/task/:id/delete", TaskApi::deleteTask);
    get("/:schema/api/tasks", TaskApi::listTasks);
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
    if (getSchema(request) != null) {

      String clearUrl = "/" + request.params("schema") + "/api/tasks/clear";
      String result = String.format("{clearUrl:'%s', tasks:[", clearUrl);

      for (String id : taskService.getJobIds()) {
        Task task = taskService.getTask(id);
        String getUrl = "/" + request.params("schema") + "/api/task/" + id;
        String deleteUrl = getUrl + "/delete";
        result +=
            String.format(
                "{\"id\":\"%s\", description:\"%s\",\"status\":\"%s\",\"url\":\"%s\",\"deleteUrl\":\"%s\"}",
                id, task.getDescription(), task.getStatus(), getUrl, deleteUrl);
      }

      result += "]}";
      return result;
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  private static String getTask(Request request, Response response) {
    if (getSchema(request) != null) {
      Step step = taskService.getTask(request.params("id"));
      if (step == null) {
        step = new Step("Task unknown").setStatus(StepStatus.UNKNOWN);
      }
      return step.toString();
    }
    throw new MolgenisException("Schema doesn't exist or permission denied");
  }

  public static String submit(Task task) {
    return taskService.submit(task);
  }
}
