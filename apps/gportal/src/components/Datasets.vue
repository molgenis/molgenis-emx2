<template>
  <div>
    <RoutedTableExplorer
      tableName="Datasets" 
      :showColumns="['id', 'title', 'description', 'publisher']"
      :canEdit="false"
      :canManage="false"
    >
      <template v-slot:rowheader="slotProps">
        <ButtonAction href="#">Request</ButtonAction>
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
