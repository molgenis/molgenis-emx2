<template>
  <Spinner v-if="loading" />
  <div v-else class="container">
    <h1>Welcome to MOLGENIS</h1>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <IconBar>
      <MessageWarning v-if="count == 0 && session.email == 'anonymous'">
        No public databases found. You might have more luck when you sign in.
      </MessageWarning>
      <MessageWarning v-else-if="count == 0 && session.email != 'admin'">
        You don't have permission to view any database. Please ask a database
        owner for permission to see their data.
      </MessageWarning>
    </IconBar>
    <div v-if="count > 0 || search || (session && session.email == 'admin')">
      <InputSearch
        id="groups-search-input"
        placeholder="search by name"
        v-model="search"
      />
      <label>{{ count }} databases found</label>
      <table class="table table-hover table-bordered bg-white">
        <thead>
          <th style="width: 1px">
            <IconAction
              v-if="session && session.email == 'admin'"
              icon="plus"
              @click="openCreateSchema"
            />
          </th>
          <th @click="changeSortOrder('name')" class="sort-col">
            name
            <IconAction
              v-if="sortColumn === 'name'"
              :icon="sortOrder == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0"
            />
          </th>
          <th>description</th>
          <th v-if="showChangeColumn" @click="changeSortOrder('lastUpdate')" class="sort-col">
            last update
            <IconAction
              v-if="sortColumn === 'lastUpdate'"
              :icon="sortOrder == 'ASC' ? 'sort-alpha-down' : 'sort-alpha-up'"
              class="d-inline p-0"
            />
          </th>
        </thead>
        <tbody>
          <tr v-for="schema in schemasFilteredAndSorted" :key="schema.name">
            <td>
              <div style="display: flex">
                <IconAction
                  v-if="session && session.email == 'admin'"
                  icon="edit"
                  @click="openEditSchema(schema.name, schema.description)"
                />
                <IconDanger
                  v-if="session && session.email == 'admin'"
                  icon="trash"
                  @click="openDeleteSchema(schema.name)"
                />
              </div>
            </td>
            <td>
              <a :href="'/' + schema.name">{{ schema.name }}</a>
            </td>
            <td>
              {{ schema.description }}
            </td>
            <td v-if="showChangeColumn">
              <LastUpdateField
                v-if="changelogSchemas.includes(schema.name)"
                :schema="schema.name"
                @input="
                  (i) => {
                    schema.update = new Date(i);
                    handleLastUpdateChange();
                  }
                "
              />
            </td>
          </tr>
        </tbody>
      </table>
      <SchemaCreateModal v-if="showCreateSchema" @close="closeCreateSchema" />
      <SchemaDeleteModal
        v-if="showDeleteSchema"
        @close="closeDeleteSchema"
        :schemaName="showDeleteSchema"
      />
      <SchemaEditModal
        v-if="showEditSchema"
        @close="closeEditSchema"
        :schemaName="showEditSchema"
        :schemaDescription="editDescription"
      />
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";

import SchemaCreateModal from "./SchemaCreateModal";
import SchemaDeleteModal from "./SchemaDeleteModal";
import SchemaEditModal from "./SchemaEditModal";
import {
  IconAction,
  IconBar,
  IconDanger,
  Spinner,
  MessageWarning,
  InputSearch,
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
    InputSearch,
    LastUpdateField,
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
      sortOrder: "ASC",
    };
  },
  computed: {
    count() {
      return this.schemasFilteredAndSorted.length;
    },
    schemasFilteredAndSorted() {
      return this.sortSchemas(this.filterSchema(this.schemas));
    },
    showChangeColumn() {
      return (
        (this.session.email == "admin" ||
          (this.session &&
            this.session.roles &&
            this.session.roles.includes("Manager"))) &&
        this.changelogSchemas
      );
    },
    changelogSchemas() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings["CHANGELOG_SCHEMAS"]
      ) {
        return this.session.settings["CHANGELOG_SCHEMAS"]
          .split(",")
          .map((s) => s.trim());
      } else {
        return [];
      }
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
    openDeleteSchema(schemaName) {
      this.showDeleteSchema = schemaName;
    },
    closeDeleteSchema() {
      this.showDeleteSchema = null;
      this.getSchemaList();
    },
    openEditSchema(schemaName, schemaDescription) {
      this.showEditSchema = schemaName;
      this.editDescription = schemaDescription;
    },
    closeEditSchema() {
      this.showEditSchema = null;
      this.editDescription = null;
      this.getSchemaList();
    },
    getSchemaList() {
      this.loading = true;
      request("graphql", "{Schemas{name description}}")
        .then((data) => {
          this.schemas = data.Schemas;
          this.loading = false;
        })
        .catch(
          (error) =>
            (this.graphqlError = "internal server graphqlError" + error)
        );
    },
    filterSchema(unfiltered) {
      let filtered = unfiltered;
      if (this.search && this.search.trim().length > 0) {
        let terms = this.search.toLowerCase().split(" ");
        filtered = this.schemas.filter((s) =>
          terms.every(
            (v) =>
              s.name.toLowerCase().includes(v) ||
              (s.description && s.description.toLowerCase().includes(v))
          )
        );
      }
      return filtered;
    },
    sortSchemas(unsorted) {
      let sorted = [];
      if (this.sortColumn === "lastUpdate") {
        sorted = unsorted.sort((a, b) => {
          if (a.update && b.update) {
            return a.update.getTime() - b.update.getTime();
          } else if (a.update && !b.update) {
            return 1;
          } else if (!a.update && b.update) {
            return -1;
          }
          {
            return a.name.localeCompare(b.name);
          }
        });
      } else {
        sorted = unsorted.sort((a, b) => a.name.localeCompare(b.name));
      }

      if (this.sortOrder === "DESC") {
        sorted.reverse();
      }
      return sorted;
    },
    changeSortOrder(columnName) {
      if (this.sortColumn === columnName) {
        // reverse the sort
        this.sortOrder = this.sortOrder === "ASC" ? "DESC" : "ASC";
      } else {
        this.sortOrder = "ASC";
        this.sortColumn = columnName;
      }
    },
    handleLastUpdateChange() {
      if (this.sortColumn === "lastUpdate") {
        this.sortSchemas(this.schemasFilteredAndSorted);
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
