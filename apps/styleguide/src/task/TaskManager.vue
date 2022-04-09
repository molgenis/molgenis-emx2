<template>
  <div>
    <Task v-if="selectedTaskId" taskId="selectedTaskId" />
    <TaskList v-else />
  </div>
</template>

<script>
import Task from './Task';
import TaskList from './TaskList';

export default {
  components: {Task, TaskList},
  data() {
    return {
      selectedTaskId: null
    };
  }
};
</script>

<docs>
Task viewer
```
<TaskManager/>
```
</docs>
