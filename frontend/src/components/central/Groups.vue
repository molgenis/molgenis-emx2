<template>
    <Spinner v-if="loading" />
    <div v-else>
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <IconBar>
            <label>{{ count }} databases found</label>
            <IconAction
                v-if="session && session.email == 'admin'"
                icon="plus"
                @click="openCreateSchema"
            />
        </IconBar>
        <TableSimple
            :columns="['name', 'description']"
            :rows="schemas"
            @click="openGroup"
        >
            <template #rowheader="slotProps">
                <IconDanger
                    v-if="session && session.email == 'admin'"
                    :key="slotProps.row.name"
                    icon="trash"
                    @click="openDeleteSchema(slotProps.row.name)"
                />
            </template>
        </TableSimple>
        <SchemaCreateModal v-if="showCreateSchema" @close="closeCreateSchema" />
        <SchemaDeleteModal
            v-if="showDeleteSchema"
            :schema-name="showDeleteSchema"
            @close="closeDeleteSchema"
        />
        <ShowMore
            title="debug"
        >
            session = {{ session }}, schemas = {{ schemas }}
        </ShowMore>
    </div>
</template>

<script>
import { request } from "graphql-request";

import SchemaCreateModal from "./SchemaCreateModal.vue";
import SchemaDeleteModal from "./SchemaDeleteModal.vue";
import {
  TableSimple,
  IconAction,
  IconBar,
  IconDanger,
  ShowMore,
  Spinner,
} from "../ui/index.js";

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
    };
  },
  computed: {
    count() {
      return this.schemas.length;
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
