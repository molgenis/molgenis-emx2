<template>
  <div>View jobs</div>
  <RoutedTableExplorer
    tableId="Jobs"
    schemaId="_SYSTEM_"
    showOrderBy="submitDate"
    showOrder="DESC"
    :canEdit="true"
    :canDelete="true"
    :canUpdate="true"
    :canInsert="true"
    :canManage="true"
  >
    <template v-slot:rowheader="slotProps">
      <TaskViewButton :taskId="slotProps.row.id" />
      <IconAction
        v-if="slotProps.row.status.name === 'RUNNING'"
        icon="stop"
        @click="cancelJob(slotProps.row)"
      />
    </template>
  </RoutedTableExplorer>
</template>

<script>
import { IconAction, RoutedTableExplorer } from "molgenis-components";
import TaskViewButton from "./TaskViewButton.vue";

export default {
  components: {
    RoutedTableExplorer,
    TaskViewButton,
    IconAction,
  },
  methods: {
    cancelJob(row) {
      fetch(`/api/tasks/${row.id}/cancel`, {
        method: "POST",
        body: this.parameters,
      })
        .then(() => {
          // reload page to update table
          window.location.reload();
        })
        .catch((error) => {
          console.error("Error cancelling job:", error);
        });
    },
  },
};
</script>
