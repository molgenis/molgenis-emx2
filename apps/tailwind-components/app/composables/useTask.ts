import type { Ref } from "vue";
import type { TaskStatus } from "../../../metadata-utils/src/types";

const taskPollingFields =
  "{ id, description, status, subTasks { id, description, status } }";

interface TaskPollingResponse {
  status: TaskStatus;
  description?: string;
  subTasks?: TaskPollingResponse[];
}

export const useTask = (schemaId: string, taskId: string) => {
  function pollStatus(
    status: Ref<TaskStatus>,
    interval: number = 2000
  ): Promise<TaskPollingResponse> {
    return new Promise((resolve, reject) => {
      const checkStatus = async () => {
        try {
          const response = await $fetch(`/${schemaId}/graphql`, {
            method: "POST",
            body: {
              query: `query { _tasks(id:"${taskId}") ${taskPollingFields} }`,
            },
          });

          if (!response.data._tasks[0]?.status) {
            status.value = "UNKNOWN";
            reject(new Error("Task status not found"));
            return;
          } else {
            status.value = response.data._tasks[0].status;
          }

          if (status.value === "COMPLETED" || status.value === "ERROR" || status.value === "CANCELLED") {
            resolve({
              status: status.value,
              description: response.data._tasks[0].description,
            });
          } else if (status.value === "RUNNING" || status.value === "WAITING") {
            setTimeout(checkStatus, interval);
          } else {
            reject(new Error(`Task failed with status: ${status.value}`));
          }
        } catch (error) {
          reject(error);
        }
      };

      checkStatus();
    });
  }

  return {
    pollStatus,
  };
};
