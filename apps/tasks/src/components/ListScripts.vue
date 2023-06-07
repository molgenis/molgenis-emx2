<template>
  <div>Manage scripts</div>
  <RoutedTableExplorer
    v-if="session"
    tableName="Scripts"
    schemaName="_SYSTEM_"
    :canEdit="true"
    :canManage="false">
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
  props: {
    session: Object,
  },
};
</script>
