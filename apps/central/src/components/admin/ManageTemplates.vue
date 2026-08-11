<template>
  <div>Manage templates</div>
  <TableExplorer
    ref="templatesTable"
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
        :tableName="slotProps.row.tableName"
        type="update"
        @saved="reloadTemplates"
      />
    </template>
  </TableExplorer>
  <TemplateEditButton
    type="insert"
    icon="plus"
    @saved="reloadTemplates"
  ></TemplateEditButton>
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
  methods: {
    // a saved template is a new or changed row, so the list must show it without a page refresh
    reloadTemplates() {
      this.$refs.templatesTable?.reload();
    },
  },
};
</script>
