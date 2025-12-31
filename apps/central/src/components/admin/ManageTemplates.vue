<template>
  <div>Manage templates</div>
  <TableExplorer
    tableId="Templates"
    schemaId="_SYSTEM_"
    :canEdit="false"
    :canManage="false"
  >
    <template v-slot:rowheader="slotProps">
      <TemplateEditButton
        :template="slotProps.row.template"
        :schema="slotProps.row.schema"
        :api="slotProps.row.endpoint"
        type="update"
      />
    </template>
  </TableExplorer>
  <TemplateEditButton type="insert" icon="plus"></TemplateEditButton>
</template>

<script>
import { TableExplorer } from "molgenis-components";
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
