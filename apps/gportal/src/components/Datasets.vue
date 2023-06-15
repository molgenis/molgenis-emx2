<template>
  <div>
    <RoutedTableExplorer
      tableName="Dataset"
      :showColumns="['id', 'title', 'description', 'publisher']"
      :canEdit="false"
      :canManage="false"
    >
      <template v-slot:rowheader="slotProps">
        <ButtonAction href="todo">Request</ButtonAction>
      </template>
    </RoutedTableExplorer>
  </div>
</template>
<script>
import { RoutedTableExplorer, ButtonAction } from "molgenis-components";
export default {
  components: {
    RoutedTableExplorer,
    ButtonAction,
  },
};
</script>
