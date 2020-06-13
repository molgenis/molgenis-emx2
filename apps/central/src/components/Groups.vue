<template>
  <Spinner v-if="loading" />
  <div v-else>
    <IconBar
      ><label>{{ count }} schemas found</label>
      <IconAction icon="plus" @click="openCreateSchema" />
    </IconBar>
    <DataTable
      :columns="['name', 'description']"
      :rows="schemas"
      @click="openGroup"
    >
      <template v-slot:rowheader="slotProps">
        <IconDanger
          icon="trash"
          @click="openDeleteSchema(slotProps.row.name)"
          :key="slotProps.row.name"
        />
      </template>
    </DataTable>
    <SchemaCreateModal v-if="showCreateSchema" @close="closeCreateSchema" />
    <SchemaDeleteModal
      v-if="showDeleteSchema"
      @close="closeDeleteSchema"
      :schemaName="showDeleteSchema"
    />
  </div>
</template>

<script>
    import {request} from "graphql-request";

    import SchemaCreateModal from "./SchemaCreateModal";
    import SchemaDeleteModal from "./SchemaDeleteModal";
    import {DataTable, IconAction, IconBar, IconDanger, Spinner} from "@mswertz/emx2-styleguide";

    export default {
  components: {
    DataTable,
    Spinner,
    SchemaCreateModal,
    SchemaDeleteModal,
    IconBar,
    IconAction,
    IconDanger
  },
  data: function() {
    return {
      schemas: [],
      loading: false,
      showCreateSchema: false,
      showDeleteSchema: false
    };
  },
  computed: {
    count() {
      return this.schemas.length;
    }
  },
  created() {
    this.getSchemaList();
  },
  methods: {
    openGroup(group) {
      window.open("/" + group.name + "/tables/", "_blank");
    },
    openCreateSchema() {
      this.showCreateSchema = true;
    },
    closeCreateSchema() {
      this.showCreateSchema = false;
      this.getSchemaList();
    },
    openDeleteSchema(schemaName) {
      this.showDeleteSchema = schemaName;
    },
    closeDeleteSchema() {
      this.showDeleteSchema = null;
      this.getSchemaList();
    },
    getSchemaList() {
      this.loading = true;
      request("/api/graphql", "{Schemas{name}}")
        .then(data => {
          this.schemas = data.Schemas;
          this.loading = false;
        })
        .catch(error => (this.error = "internal server error" + error));
    }
  }
};
</script>
