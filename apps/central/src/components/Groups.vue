<template>
  <Spinner v-if="loading" />
  <div v-else class="container">
    <h1>Welcome to MOLGENIS</h1>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <IconBar>
      <MessageWarning v-if="count == 0 && session.email == 'anonymous'">
        No public databases found. You might have more luck when you sign in.
      </MessageWarning>
      <MessageWarning v-else-if="count == 0 && !session.admin">
        You don't have permission to view any database. Please ask a database
        owner for permission to see their data.
      </MessageWarning>
    </IconBar>
    <div v-if="count > 0 || search || (session && session.admin)">
      <InputSearch
        id="groups-search-input"
        placeholder="search in schemas"
        v-model="search"
      />

      <label>{{ count }} databases found</label>

      <table class="table table-hover table-bordered bg-white">
        <thead>
          <th style="width: 1px">
            <IconAction
              v-if="session && session.admin"
              icon="plus"
              @click="openCreateSchema"
            />
          </th>
          <th @click="changeSortOrder('label')" class="sort-col">
            label
            <IconAction
              v-if="sortOrder && sortColumn === 'label'"
              :icon="sortOrder == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0 hide-icon"
            />
          </th>
          <th>description</th>
          <th
            v-if="showChangeColumn"
            @click="changeSortOrder('lastUpdate')"
            class="sort-col"
          >
            last update
            <IconAction
              v-if="sortOrder && sortColumn === 'lastUpdate'"
              :icon="sortOrder == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0 hide-icon"
            />
          </th>
        </thead>
        <tbody>
          <tr v-for="schema in schemasFilteredAndSorted" :key="schema.id">
            <td>
              <div style="display: flex">
                <IconAction
                  v-if="session && session.admin"
                  icon="edit"
                  @click="openEditSchema(schema.id, schema.description)"
                />
                <IconDanger
                  v-if="session && session.admin"
                  icon="trash"
                  @click="openDeleteSchema(schema.id)"
                />
              </div>
            </td>
            <td>
              <a :href="'/' + schema.id + '/tables'">{{ schema.label }}</a>
            </td>
            <td>
              {{ schema.description }}
            </td>
            <td v-if="showChangeColumn">
              <LastUpdateField
                v-if="schema.update"
                :lastUpdate="schema.update"
              />
            </td>
          </tr>
        </tbody>
      </table>
      <SchemaCreateModal v-if="showCreateSchema" @close="closeCreateSchema" />
      <SchemaDeleteModal
        v-if="showDeleteSchema"
        @close="closeDeleteSchema"
        :schemaId="showDeleteSchema"
      />
      <SchemaEditModal
        v-if="showEditSchema"
        @close="closeEditSchema"
        :schemaId="showEditSchema"
        :schemaDescription="editDescription"
      />
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";

import SchemaCreateModal from "./SchemaCreateModal.vue";
import SchemaDeleteModal from "./SchemaDeleteModal.vue";
import SchemaEditModal from "./SchemaEditModal.vue";
import {
  IconAction,
  IconBar,
  IconDanger,
  Spinner,
  MessageWarning,
  InputSearch,
  MessageError,
  ButtonOutline,
} from "molgenis-components";
import LastUpdateField from "./LastUpdateField.vue";

export default {
  components: {
    Spinner,
    SchemaCreateModal,
    SchemaDeleteModal,
    SchemaEditModal,
    IconBar,
    IconAction,
    IconDanger,
    MessageWarning,
    MessageError,
    InputSearch,
    LastUpdateField,
    ButtonOutline,
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
      showEditSchema: false,
      editDescription: null,
      graphqlError: null,
      search: null,
      sortColumn: "name",
      sortOrder: null,
      lastUpdates: [],
    };
  },
  computed: {
    count() {
      return this.schemasFilteredAndSorted.length;
    },
    schemasFilteredAndSorted() {
      return this.sortSchemas(this.filterSchema(this.schemas));
    },
    hasManagerPermission() {
      return (
        this.session.admin ||
        (this.session &&
          this.session.roles &&
          this.session.roles.includes("Manager"))
      );
    },
    showChangeColumn() {
      return this.session.admin;
    },
  },
  created() {
    this.getSchemaList();
  },
  methods: {
    openCreateSchema() {
      this.showCreateSchema = true;
    },
    closeCreateSchema() {
      this.showCreateSchema = false;
      this.getSchemaList();
    },
    openDeleteSchema(schemaId) {
      this.showDeleteSchema = schemaId;
    },
    closeDeleteSchema() {
      this.showDeleteSchema = null;
      this.getSchemaList();
    },
    openEditSchema(schemaId, schemaDescription) {
      this.showEditSchema = schemaId;
      this.editDescription = schemaDescription;
    },
    closeEditSchema() {
      this.showEditSchema = null;
      this.editDescription = null;
      this.getSchemaList();
    },
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
          const lastUpdates = data._lastUpdate ?? [];
          lastUpdates.forEach((lastUpdate) => {
            const schemaLastUpdate = this.schemas.find(
              (schema) => schema.id === lastUpdate.schemaName
            );
            if (schemaLastUpdate) {
              schemaLastUpdate.update = lastUpdate;
            }
          });
          this.loading = false;
        })
        .catch((error) => {
          console.error("internal server error", error);
          this.graphqlError = "internal server error" + error;
          this.loading = false;
        });
    },
    filterSchema(unfiltered) {
      let filtered = unfiltered;
      if (this.search && this.search.trim().length > 0) {
        let terms = this.search.toLowerCase().split(" ");
        filtered = this.schemas.filter((s) =>
          terms.every(
            (v) =>
              s.id.toLowerCase().includes(v) ||
              (s.description && s.description.toLowerCase().includes(v))
          )
        );
      }
      return filtered;
    },
    sortSchemas(unsorted) {
      const unsortedCopy = unsorted.slice();
      let sorted = [];
      if (this.sortColumn === "lastUpdate") {
        sorted = unsortedCopy.sort((a, b) => {
          if (a.update && b.update) {
            return new Date(a.update.stamp) - new Date(b.update.stamp);
          } else if (a.update && !b.update) {
            return 1;
          } else if (!a.update && b.update) {
            return -1;
          }
          {
            return a.id.localeCompare(b.id);
          }
        });
      } else {
        sorted = unsortedCopy.sort((a, b) => a.id.localeCompare(b.id));
      }

      if (this.sortOrder === "DESC") {
        sorted.reverse();
      }
      return sorted;
    },
    changeSortOrder(columnId) {
      if (this.sortColumn === columnId) {
        // reverse the sort
        this.sortOrder = this.sortOrder === "ASC" ? "DESC" : "ASC";
      } else {
        this.sortOrder = "ASC";
        this.sortColumn = columnId;
      }
    },
    handleLastUpdateChange() {
      if (this.sortColumn === "lastUpdate") {
        this.sortSchemas(this.schemasFilteredAndSorted);
      }
    },
  },
  watch: {
    showChangeColumn(val) {
      if (val) {
        this.getSchemaList();
      }
    },
  },
};
</script>

<style scoped>
.sort-col:hover {
  cursor: pointer;
}
</style>
