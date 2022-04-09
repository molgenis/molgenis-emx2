<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <table class="table table-bordered">
      <thead>
        <th scope="col">Task Description</th>
        <th>Task Status</th>
      </thead>
      <tbody>
        <tr v-for="task in taskList" :key="task.id">
          <td>{{ task.description }}</td>
          <td>{{ task.status }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      taskList: [],
      error: null
    };
  },
  methods: {
    retrieveTasks() {
      fetch('api/tasks')
        .then((response) => {
          if (response.ok) {
            response.json().then((taskList) => {
              console.log(JSON.stringify(taskList));
              this.taskList = taskList.tasks;
            });
          } else {
            response.text().then((error) => {
              this.error = error;
            });
          }
        })
        .catch((error) => {
          this.error = error;
        });
    }
  },
  created() {
    this.retrieveTasks();
  }
};
</script>

<docs>
Task list from /api/tasks
```
<TaskList/>
```

</docs>
