<template>
  <div>Manage scripts</div>
  <TableExplorer
    tableId="Templates"
    schemaId="_SYSTEM_"
    :canEdit="true"
    :canManage="false"
  >
    <template v-slot:rowheader="slotProps">
      <TemplateEditButton :taskId="slotProps.row.id" />
    </template>
  </TableExplorer>
</template>

<script>
import { TableExplorer, InputSelect } from "molgenis-components";
import TemplateEditButton from "./TemplateEditButton.vue";

export default {
  components: {
    TemplateEditButton,
    TableExplorer,
  },
  props: {
    session: Object,
  },
};
</script>
