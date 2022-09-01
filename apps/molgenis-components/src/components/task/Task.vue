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
<<<<<<< HEAD
=======
      error: false,
>>>>>>> master
    };
  },
  methods: {
    async startMonitorTask() {
      while (
<<<<<<< HEAD
        !this.task ||
        !(this.task.status === "ERROR") ||
        !(this.task.status === "COMPLETED")
      ) {
        await sleep(500);
=======
        (!this.error && !this.task) ||
        (this.task && !["COMPLETED", "ERROR"].includes(this.task.status))
      ) {
>>>>>>> master
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
<<<<<<< HEAD
          .then((data) => {
            this.task = data._tasks[0];
            this.loading = false;
          })
          .catch((error) => {
            if (Array.isArray(error.response.errors)) {
              console.log(error.response.errors[0].message);
            } else {
              console.log(error);
            }
          });
      }
=======
          .then((data) => (this.task = data._tasks[0]))
          .catch((error) => {
            console.log(JSON.stringify(error));
            this.error = true;
          });
        await sleep(500);
      }
      this.loading = false;
>>>>>>> master
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
<<<<<<< HEAD
      This component is not demoable, as it needs an existing task-id. It is also shown in the TaskList and TaskManager components.
    </demo-item>
  </div>
</template>
=======
      <StringInput v-model="taskId" />
      <Task :taskId="taskId" />
    </demo-item>
  </div>
</template>
<script>
export default {
  data: function () {
    return { taskId: "not existing taskId" };
  },
};
</script>
>>>>>>> master
</docs>
