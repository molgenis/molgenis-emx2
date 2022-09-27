<template>
  <div>
    <Spinner v-if="loading" />
    <SubTask v-else-if="task" :task="task" />
    <span v-else>Task with id = '{{ taskId }}' not found</span>
  </div>
</template>

<script>
import { request } from "../../client/client.js";
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
          .then((data) => {
            this.task = data._tasks[0];
            this.loading = false;
            this.$emit("taskUpdated", this.task);
          })
          .catch((error) => {
            console.log(JSON.stringify(error));
            this.loading = false;
            this.error = true;
          });
        await sleep(500);
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
      <InputString id="task-id-input" v-model="taskId"/>
      <ButtonAction v-if="!showTask" v-on:click="showTask = true"
      >Send
      </ButtonAction
      >
      <Task v-if="showTask" :taskId="taskId"/>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        taskId: "not existing taskId",
        showTask: false,
      };
    },
  };
</script>
</docs>
