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
    </template>
  </RoutedTableExplorer>
</template>

<script>
import { RoutedTableExplorer } from "molgenis-components";
import TaskViewButton from "./TaskViewButton.vue";

export default {
  components: {
    RoutedTableExplorer,
    TaskViewButton,
  },
};
</script>
