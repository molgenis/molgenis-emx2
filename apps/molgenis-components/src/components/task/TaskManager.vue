<template>
  <div>
    <span v-if="selectedTaskId">
      <span @click="clearSelection">
        <IconAction icon="angle-left" />
        Back to task list
      </span>
      <h1>Task details</h1>
      <Task :taskId="selectedTaskId" />
    </span>
    <div v-else>
      <h1>Task list</h1>
      <TaskList @select="selectTask" />
    </div>
  </div>
</template>

<script>
import Task from "./Task.vue";
import TaskList from "./TaskList.vue";
import IconAction from "../forms/IconAction.vue";

export default {
  name: "TaskManager",
  components: { Task, TaskList, IconAction },
  data() {
    return {
      selectedTaskId: null,
    };
  },
  methods: {
    selectTask(taskId) {
      this.selectedTaskId = taskId;
    },
    clearSelection() {
      this.selectedTaskId = null;
    },
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item>
      Task viewer
      <TaskManager/>
    </demo-item>
  </div>
</template>
</docs>
