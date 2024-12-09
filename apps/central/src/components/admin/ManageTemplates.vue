<template>
  <div>Manage scripts</div>
  <InputSelect :options="schemas"></InputSelect>
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
import { request } from "graphql-request";

export default {
  components: {
    TemplateEditButton,
    TableExplorer,
    InputSelect,
  },
  props: {
    session: Object,
  },
  data: function () {
    return {
      schemas: [],
      loading: false,
      graphqlError: null,
    };
  },
  created() {
    this.getSchemaList();
  },
  methods: {
    getSchemaList() {
      this.loading = true;
      const schemaFragment = "_schemas{id,label,description}";
      const lastUpdateFragment =
        "_lastUpdate{schemaName, tableName, stamp, userId, operation}";
      request(
        "graphql",
        `{${schemaFragment} ${this.showChangeColumn ? lastUpdateFragment : ""}}`
      )
        .then((data) => {
          this.schemas = data._schemas;
          this.loading = false;
        })
        .catch((error) => {
          console.error("internal server error", error);
          this.graphqlError = "internal server error" + error;
          this.loading = false;
        });
    },
  },
};
</script>
