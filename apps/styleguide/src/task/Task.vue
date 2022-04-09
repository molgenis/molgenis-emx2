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
  props: {
    taskId: String
  },
  computed: {
    color() {
      if (this.task) {
        if (this.task.status == 'COMPLETED') return 'text-success';
        if (this.task.status == 'ERROR') return 'text-danger';
        if (this.task.status == 'WARNING') return 'text-warning';
        if (this.task.status == 'SKIPPED') return 'text-muted';
        return 'text-primary';
      }
      return '';
    },
    icon() {
      if (this.task) {
        if (this.task.status == 'COMPLETED') return 'fa-check';
        if (this.task.status == 'ERROR') return 'fa-times';
        if (this.task.status == 'WARNING') return 'fa-exclamation-circle';
        if (this.task.status == 'SKIPPED') return 'fa-check';
        if (this.task.status == 'RUNNING') return 'fa-spinner fa-spin';
      }
      return 'fa-question';
    }
  },
  methods: {
    startMonitorTask() {
      if (!this.task || !['COMPLETED', 'ERROR'].includes(this.task.status)) {
        setTimeout(this.monitorTask, 500);
      } else {
        if (this.task.status == 'ERROR') {
          this.error = this.task.status.description;
          this.success = null;
        } else {
          this.error = null;
          this.success = this.task.status.description;
        }
      }
    },
    monitorTask() {
      fetch(this.taskUrl)
        .then((response) => {
          if (response.ok) {
            response.json().then((task) => {
              this.task = task;
              this.startMonitorTask();
            });
          } else {
            response.text().then((error) => {
              this.error = error;
              this.startMonitorTask();
            });
          }
        })
        .catch((error) => {
          this.error = error;
          this.startMonitorTask();
        });
    }
  },
  created() {
    this.startMonitorTask();
  }
};
</script>

<docs>
Simply open progress monitoring of a task. You must provide a real task id
```
<Task taskId="blaat"/>
```


</docs>
