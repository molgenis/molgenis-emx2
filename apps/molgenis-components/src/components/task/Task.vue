<template>
  <div>
    <Spinner v-if="loading" />
    <SubTask v-else-if="task" :task="task" />
    <span v-else>Task with id = '{{ taskId }}' not found</span>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ITask from "./ITask";
import { request } from "../../client/client";
import SubTask from "./SubTask.vue";
import Spinner from "../layout/Spinner.vue";

export default defineComponent({
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
      task: null as ITask | null,
      loading: true,
      error: false,
      timeoutId: null as ReturnType<typeof setTimeout> | null,
    };
  },
  methods: {
    async startMonitorTask() {
      while (
        (!this.error && !this.task) ||
        (this.task &&
          !["COMPLETED", "ERROR", "CANCELLED"].includes(this.task.status))
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
                  id, description, status, subTasks
                    {
                      id, description, status
                    }
                }
              }
            }
          }
        }`;
        request("graphql", query)
          .then((data) => {
            this.task = data._tasks[0];
            this.$emit("taskUpdated", this.task);
            this.loading = false;
          })
          .catch((error) => {
            console.log(JSON.stringify(error));
            this.error = error;
            this.loading = false;
          });
        await new Promise((resolve) => {
          this.timeoutId = setTimeout(resolve, 500);
        });
      }
    },
  },
  created() {
    this.startMonitorTask();
  },
  beforeUnmount() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.task = null;
  },
});
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
