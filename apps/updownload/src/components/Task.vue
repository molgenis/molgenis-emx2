<template>
  <li :class="color">
    <i class="fa-li fa" :class="icon"></i>
    {{ task.description }}
    <ul class="fa-ul">
      <task :task="subtask" v-for="(subtask, id) in task.steps" :key="id" />
    </ul>
  </li>
</template>

<script>
export default {
  name: "task",
  props: {
    task: Object,
  },
  computed: {
    color() {
      if (this.task) {
        if (this.task.status == "COMPLETED") return "text-success";
        if (this.task.status == "ERROR") return "text-danger";
        if (this.task.status == "WARNING") return "text-warning";
        if (this.task.status == "SKIPPED") return "text-muted";
        return "text-primary";
      }
      return "";
    },
    icon() {
      if (this.task) {
        if (this.task.status == "COMPLETED") return "fa-check";
        if (this.task.status == "ERROR") return "fa-times";
        if (this.task.status == "WARNING") return "fa-exclamation-circle";
        if (this.task.status == "SKIPPED") return "fa-check";
        if (this.task.status == "RUNNING") return "fa-spinner fa-spin";
      }
      return "fa-question";
    },
  },
};
</script>
