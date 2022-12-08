<template>
  <ul class="fa-ul">
    <li :class="color" v-if="task">
      <i class="fa-li fa" :class="icon"></i>
      {{ task.description }}
      <SubTask
          v-for="subtask in task.subTasks"
          :task="subtask"
          :key="subtask.id"
      />
    </li>
  </ul>
</template>

<script>
export default {
  name: "SubTask",
  props: {
    task: Object,
  },
  computed: {
    color() {
      switch (this.task.status) {
        case "COMPLETED":
          return "text-success";
        case "ERROR":
          return "text-danger";
        case "WARNING":
          return "text-warning";
        case "SKIPPED":
          return "text-muted";
        default:
          return "text-primary";
      }
    },
    icon() {
      switch (this.task.status) {
        case "COMPLETED":
          return "fa-check";
        case "ERROR":
          return "fa-times";
        case "WARNING":
          return "fa-exclamation-circle";
        case "SKIPPED":
          return "fa-check";
        case "RUNNING":
          return "fa-spinner fa-spin";
        default:
          return "fa-question";
      }
    },
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item>
      simple task
      <SubTask :task="task" />
    </demo-item>
    <demo-item>
      task with sub tasks
      <SubTask :task="subtasks" />
    </demo-item>
  </div>
</template>

<script>
export default {
  data() {
    return {
      task: {
        description: "description",
        status: "ERROR",
      },
      subtasks: {
        description: "description",
        status: "COMPLETED",
        subTasks: [
          {
            description: "subtask 1",
            status: "WARNING",
          },
          {
            description: "subtask 2",
            status: "SKIPPED",
          },
          {
            description: "subtask 3",
            status: "UNDEFINED",
            subTasks: [
              {
                description: "sub subtask 1",
                status: "WARNING",
              },
            ],
          },
          {
            description: "subtask 4",
            status: "RUNNING",
          },
        ],
      },
    };
  },
};
</script>
</docs>