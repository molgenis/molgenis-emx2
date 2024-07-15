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
        placeholder="search in schemas"
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
                  v-if="session && session.email == 'admin'"
                  icon="edit"
                  @click="openEditSchema(schema.id, schema.description)"
                />
                <IconDanger
                  v-if="session && session.email == 'admin'"
                  icon="trash"
                  @click="openDeleteSchema(schema.id)"
                />
              </div>
            </td>
            <td>
              <a :href="'/' + schema.id">{{ schema.label }}</a>
            </td>
            <td>
              {{ schema.description }}
            </td>
            <td v-if="showChangeColumn">
              <LastUpdateField
                v-if="changelogSchemas.includes(schema.id)"
                :schema="schema.id"
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
      changelogSchemas: [],
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
        this.session.email == "admin" ||
        (this.session &&
          this.session.roles &&
          this.session.roles.includes("Manager"))
      );
    },
    showChangeColumn() {
      return this.hasManagerPermission && this.changelogSchemas.length;
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
      request("graphql", "{_schemas{id,label,description}}")
        .then((data) => {
          this.schemas = data._schemas;
          this.loading = false;
          if (this.hasManagerPermission) {
            this.fetchChangelogStatus();
          }
        })
        .catch(
          (error) =>
            (this.graphqlError = "internal server graphqlError" + error)
        );
    },
    fetchChangelogStatus() {
      this.schemas.forEach((schema) => {
        request(
          `/${schema.id}/settings/graphql`,
          `{_settings (keys: ["isChangelogEnabled"]){ key, value }}`
        )
          .then((data) => {
            if (data._settings[0].value.toLowerCase() === "true") {
              this.changelogSchemas.push(schema.id);
            }
          })
          .catch((error) => {
            console.log(error);
          });
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
            return a.id.localeCompare(b.id);
          }
        });
      } else {
        sorted = unsorted.sort((a, b) => a.id.localeCompare(b.id));
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
};
</script>

<style scoped>
.sort-col:hover {
  cursor: pointer;
}
</style>
