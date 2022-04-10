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
