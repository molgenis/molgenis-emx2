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
      error: false,
    };
  },
  methods: {
    async startMonitorTask() {
      while (
        (!this.error && !this.task) ||
        (this.task && !["COMPLETED", "ERROR"].includes(this.task.status))
      ) {
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
          .then((data) => (this.task = data._tasks[0]))
          .catch((error) => {
            console.log(JSON.stringify(error));
            this.error = true;
          });
        await sleep(500);
      }
      this.loading = false;
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
      <Task taskId="6956bf6d-3f78-4798-873f-dd5382ac0e24"/>
    </demo-item>
  </div>
</template>
</docs>
