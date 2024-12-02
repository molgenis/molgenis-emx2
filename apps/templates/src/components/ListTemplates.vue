<template>
  <div>Manage scripts</div>
  <RoutedTableExplorer
    v-if="session"
    tableId="Templates"
    schemaId="_SYSTEM_"
    :canEdit="true"
    :canManage="false"
  >
  </RoutedTableExplorer>
</template>

<script>
import { RoutedTableExplorer } from "molgenis-components";

export default {
  components: {
    RoutedTableExplorer,
  },
  props: {
    session: Object,
  },
};
</script>
