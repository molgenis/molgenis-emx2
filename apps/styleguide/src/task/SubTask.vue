<template>
  <ul class="fa-ul">
    <li :class="color" v-if="task">
      <i class="fa-li fa" :class="icon"></i>
      {{ task.description }}
      <SubTask
        :task="subtask"
        v-for="subtask in task.subTasks"
        :key="subtask.id"
      />
    </li>
  </ul>
</template>

<script>
export default {
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
      switch (this.task) {
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
