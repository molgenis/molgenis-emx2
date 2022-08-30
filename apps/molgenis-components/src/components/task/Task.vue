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
    taskId: String,
  },
  data() {
    return {
      task: null,
      loading: true,
    };
  },
  methods: {
    startMonitorTask() {
      if (!this.task || !["COMPLETED", "ERROR"].includes(this.task.status)) {
        setTimeout(this.monitorTask, 500);
      } else {
        if (this.task.status == "ERROR") {
          this.error = this.task.status.description;
          this.success = null;
        } else {
          this.error = null;
          this.success = this.task.status.description;
        }
      }
    },
    monitorTask() {
      console.log("bla");

      request(
        "graphql",
        `{
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
        }`
      )
        .then((data) => {
          this.task = data._tasks[0];
          this.loading = false;
        })
        .catch((error) => {
          if (Array.isArray(error.response.errors)) {
            this.error = error.response.errors[0].message;
            this.startMonitorTask();
          } else {
            this.error = error;
            this.startMonitorTask();
          }
        });
    },
  },
  created() {
    this.startMonitorTask();
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item>
      task
      <Task :taskId="task" />
    </demo-item>
  </div>
</template>

<script>
export default {
  data() {
    return {
      task: "taskID",
    };
  },
};
</script>
</docs>
