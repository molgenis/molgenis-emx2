<template>
  <div>
    <Spinner v-if="loading" />
    <SubTask v-else-if="task" :task="task" />
    <span v-else>Task with id = '{{ taskId }}' not found</span>
  </div>
</template>

<script>
import { request } from "graphql-request";
import SubTask from "./SubTask.vue";
import Spinner from "../layout/Spinner.vue";

export default {
  name: "Task",
  components: {
    SubTask,
    Spinner,
  },
  props: {
    taskId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      task: null,
      loading: true,
      errorOccurred: false,
    };
  },
  methods: {
    async startMonitorTask() {
      while (
        (!this.task && !this.errorOccurred) ||
        !["COMPLETED", "ERROR"].includes(this.task.status)
      ) {
        await sleep(500);
        const query = `{
          _tasks(id:"${this.taskId}")
          {
            id, description, status, subTasks
            {
              id, description, status, subTasks
              {
                id, description, status, subTasks
                {
                  id, description, status
                }
              }
            }
          }
        }`;
        request("graphql", query)
          .then((data) => {
            this.task = data._tasks[0];
            this.loading = false;
          })
          .catch((error) => {
            console.log(JSON.stringify(error));
            this.errorOccurred = true;
          });
      }
    },
  },
  created() {
    this.startMonitorTask();
  },
};

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
</script>

<docs>
<template>
  <div>
    <demo-item>
      This component is not demoable, as it needs an existing task-id. It is also shown in the TaskList and TaskManager components.
    </demo-item>
  </div>
</template>
</docs>
