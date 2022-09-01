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
      error: null,
      success: null,
    };
  },
  methods: {
    startMonitorTask() {
      if (
        (!this.error && !this.task) ||
        !["COMPLETED", "ERROR"].includes(this.task.status)
      ) {
        setTimeout(this.monitorTask, 500);
      } else {
        console.log("timed out");
      }
    },
    monitorTask() {
      request(
        "graphql",
        `{_tasks(id:"${this.taskId}"){id,description,status,subTasks{id,description,status,subTasks{id,description,status,subTasks{id,description,status}}}}}`
      )
        .then((data) => {
          this.task = data._tasks[0];
          this.loading = false;
        })
        .catch((error) => {
          console.log(JSON.stringify(error));
          if (Array.isArray(error.response.errors)) {
            this.error = error.response.errors[0].message;
          } else {
            this.error = error;
          }
        });
      this.startMonitorTask();
    },
  },
  created() {
    this.startMonitorTask();
  },
};
</script>

<docs>
<template>
  <demo-item>
    <Task taskId="6956bf6d-3f78-4798-873f-dd5382ac0e24"/>
  </demo-item>
</template>
</docs>
