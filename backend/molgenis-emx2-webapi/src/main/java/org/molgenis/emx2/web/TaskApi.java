package org.molgenis.emx2.web;

import static spark.Spark.get;

import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;
import spark.Request;
import spark.Response;

public class TaskApi {

  private static TaskService taskService = new TaskServiceInMemory();

  public static void create() {
    get("/:schema/api/task/:id", TaskApi::getTask);
  }

  private static String getTask(Request request, Response response) {
    return taskService.getTask(request.params("id")).toString();
  }

  public static String submit(Task task) {
    return taskService.submit(task);
  }
}
