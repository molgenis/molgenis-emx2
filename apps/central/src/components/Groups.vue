<template>
  <Spinner v-if="loading" />
  <div v-else>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <IconBar>
      <MessageWarning v-if="count == 0 && session.email == 'anonymous'">
        No public databases found. You might have more luck when you sign in.
      </MessageWarning>
      <MessageWarning v-else-if="count == 0 && session.email != 'admin'">
        You don't have permission to view any database. Please ask a database
        owner for permission to see their data.
      </MessageWarning>
      <label v-else>{{ count }} databases found</label>
      <IconAction
        v-if="session && session.email == 'admin'"
        icon="plus"
        @click="openCreateSchema"
      />
    </IconBar>
    <div v-if="count > 0">
      <InputSearch
        v-if="count > 10"
        placholder="search by name"
        v-model="search"
      />
      <TableSimple
        :columns="['name', 'description']"
        :rows="schemasFiltered"
        @click="openGroup"
        class="bg-white"
      >
        <template v-slot:rowheader="slotProps">
          <IconAction
            icon="external-link"
            @click="openGroup(slotProps.row)"
            :key="slotProps.row.name + 'open'"
          />
          <IconDanger
            v-if="session && session.email == 'admin'"
            icon="trash"
            @click="openDeleteSchema(slotProps.row.name)"
            :key="slotProps.row.name"
          />
        </template>
      </TableSimple>
      <SchemaCreateModal v-if="showCreateSchema" @close="closeCreateSchema" />
      <SchemaDeleteModal
        v-if="showDeleteSchema"
        @close="closeDeleteSchema"
        :schemaName="showDeleteSchema"
      />
      <ShowMore title="debug"
        >session = {{ session }}, schemas = {{ schemas }}
      </ShowMore>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";

import SchemaCreateModal from "./SchemaCreateModal";
import SchemaDeleteModal from "./SchemaDeleteModal";
import {
  TableSimple,
  IconAction,
  IconBar,
  IconDanger,
  ShowMore,
  Spinner,
  MessageWarning,
  InputSearch,
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    TableSimple,
    Spinner,
    SchemaCreateModal,
    SchemaDeleteModal,
    IconBar,
    IconAction,
    IconDanger,
    ShowMore,
    MessageWarning,
    InputSearch,
  },
  props: {
    session: Object,
  },
  data: function () {
    return {
      schemas: [],
      loading: false,
      showCreateSchema: false,
      showDeleteSchema: false,
      graphqlError: null,
      search: null,
    };
  },
  computed: {
    count() {
      return this.schemas.length;
    },
    schemasFiltered() {
      if (this.search && this.search.trim().length > 0) {
        let terms = this.search.split(" ");
        return this.schemas.filter((s) =>
          terms.every((v) => s.name.includes(v))
        );
      }
      return this.schemas;
    },
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
      request("graphql", "{Schemas{name}}")
        .then((data) => {
          this.schemas = data.Schemas;
          this.loading = false;
        })
        .catch(
          (error) =>
            (this.graphqlError = "internal server graphqlError" + error)
        );
    },
  },
};
</script>
