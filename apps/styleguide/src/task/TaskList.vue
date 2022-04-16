<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <table class="table table-bordered">
      <thead>
        <th scope="col">Task Description</th>
        <th>Task Status</th>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>
            <button
              @click.prevent.stop="$emit('select', task.id)"
              type="button"
              class="btn btn-link"
            >
              {{ task.description }}
            </button>
          </td>
          <td>{{ task.status }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { request } from "graphql-request";
import MessageError from "../forms/MessageError";

export default {
  components: {
    MessageError,
  },
  data() {
    return {
      loading: false,
      tasks: [],
      error: null,
    };
  },
  methods: {
    retrieveTasks() {
      this.loading = true;
      this.graphqlError = null;
      request("graphql", `{_tasks{id,description,status}}`)
        .then((data) => {
          this.tasks = data._tasks;
          this.loading = false;
        })
        .catch((error) => {
          if (Array.isArray(error.response.errors)) {
            this.graphqlError = error.response.errors[0].message;
          } else {
            this.error = error;
          }
          this.loading = false;
        });
    },
  },
  //don't do this on create because prevent to serverside.
  mounted() {
    this.retrieveTasks();
  },
};
</script>

<docs>
Task list from /api/tasks
```
<TaskList/>
```

</docs>
