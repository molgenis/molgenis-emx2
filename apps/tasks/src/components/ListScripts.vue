<template>
  <div>Manage scripts</div>
  <RoutedTableExplorer
    tableName="Scripts"
    schemaName="ADMIN"
    :canEdit="true"
    :canManage="false"
  >
    <template v-slot:rowheader="slotProps">
      <SubmitButton :script="slotProps.row" />
    </template>
  </RoutedTableExplorer>
</template>

<script>
import { RoutedTableExplorer } from "molgenis-components";
import SubmitButton from "./SubmitButton.vue";

export default {
  components: {
    RoutedTableExplorer,
    SubmitButton,
  },
};
</script>
