<template>
  <div class="container-fluid bg-white">
    <div class="sticky-top bg-white">
      <div class="d-flex flex-row">
        <h1>Schema: {{ schema.name }}</h1>
        <div class="form-inline">
          <ButtonAction v-if="dirty" @click="saveSchema" class="ml-2">
            Save
          </ButtonAction>
          <ButtonAction v-if="dirty" @click="loadSchema" class="ml-2">
            Reset
          </ButtonAction>
          <ButtonAction
            v-if="schema.tables?.length > 0"
            @click="toggleShowDiagram"
            class="ml-2"
          >
            {{ showDiagram ? "Hide" : "Show" }} Diagram
          </ButtonAction>
          <ButtonAction
            :href="`/${schema.name}/schema/#/print`"
            target="_blank"
            class="ml-2"
          >
            Show printable table
          </ButtonAction>
          <ButtonAction
            :href="`/${schema.name}/schema/#/print-list`"
            target="_blank"
            class="ml-2"
          >
            Show printable list
          </ButtonAction>
          <MessageError v-if="error" class="ml-2 m-0 p-2">
            {{ error }}
          </MessageError>
          <MessageWarning v-if="warning" class="ml-2 m-0 p-2">
            {{ warning }}
          </MessageWarning>
          <MessageSuccess v-if="success" class="ml-2 m-0 p-2">
            {{ success }}
          </MessageSuccess>
        </div>
      </div>
      <div class="subsets-panel" v-if="isManager && schema.bundleName">
        <div class="subsets-panel__header">
          <strong :title="schema.bundleDescription || undefined">{{
            schema.bundleName
          }}</strong>
          <span class="subsets-panel__label ml-2 text-muted"
            >Active profiles</span
          >
        </div>
        <div class="subsets-panel__list">
          <template v-if="schema.availableProfiles?.length">
            <div
              v-for="item in schema.availableProfiles"
              :key="item.name"
              class="form-check form-check-inline"
            >
              <input
                class="form-check-input"
                type="checkbox"
                :id="`subset_${item.name}`"
                :checked="schema.activeProfiles?.includes(item.name)"
                @change="toggleSubset(item.name, $event.target.checked)"
              />
              <label
                class="form-check-label"
                :for="`subset_${item.name}`"
                :title="item.description || undefined"
                >{{ item.name
                }}<small
                  v-if="item.description"
                  class="ml-1 text-muted d-block"
                  >{{ item.description }}</small
                ></label
              >
            </div>
          </template>
        </div>
      </div>
    </div>
    <Spinner v-if="loading === true" />
    <div v-else class="row">
      <div class="col-2 bg-white">
        <SchemaToc
          :modelValue="schema"
          v-if="schema.tables"
          @update:modelValue="handleInput"
          :key="key"
          :isManager="isManager"
        />
      </div>
      <div class="bg-white col ml-2 overflow-auto">
        <a id="molgenis_diagram_anchor"></a>
        <InputBoolean
          v-if="showDiagram"
          id="showColumns"
          label="Show columns"
          :required="true"
          v-model="showColumns"
        />
        <SchemaDiagram
          :tables="schema.tables.filter((table) => table.tableType === 'DATA')"
          :showColumns="showColumns"
          v-if="showDiagram"
        />
        <SchemaView
          :modelValue="schema"
          :schemaNames="schemaNames"
          @update:modelValue="handleInput"
          :isManager="isManager"
          :locales="session?.settings?.locales"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
table {
  table-layout: fixed;
}

/*
  Work around for bootstrap 4 interaction effect with dropdown ( from Breadcrumb )
  Use the available space between z layers to move the sticky app header below the menu dropdown and modals
  1000 - 2 = 998
*/
.sticky-top {
  z-index: 998;
}

.subsets-panel__list .form-check-inline {
  display: block;
}

.subsets-panel__group-label {
  display: block;
  font-size: 0.75rem;
  margin-top: 4px;
}

.subsets-panel__header {
  margin-bottom: 4px;
}
</style>

<script>
import { request } from "graphql-request";
import SchemaView from "./SchemaView.vue";
import SchemaToc from "./SchemaToc.vue";
import SchemaDiagram from "./SchemaDiagram.vue";
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  MessageWarning,
  Spinner,
  deepClone,
  InputBoolean,
} from "molgenis-components";
import VueScrollTo from "vue-scrollto";
import {
  schemaQuery,
  addOldNamesAndRemoveMeta,
  convertToSubclassTables,
} from "../utils.ts";
import gql from "graphql-tag";

export default {
  components: {
    InputBoolean,
    SchemaView,
    ButtonAction,
    MessageError,
    MessageWarning,
    MessageSuccess,
    SchemaToc,
    Spinner,
    SchemaDiagram,
  },
  props: {
    session: {
      type: Object,
    },
  },
  data() {
    return {
      rawSchema: {}, //for debug purposes
      schema: {},
      loading: false,
      error: null,
      warning: null,
      success: null,
      showDiagram: false,
      showColumns: false,
      schemaNames: [],
      dirty: false,
      key: Date.now(),
      isManager: false,
    };
  },
  methods: {
    async toggleSubset(name, activate) {
      const mutation = activate
        ? `mutation { enableProfile(name: "${name}") { message } }`
        : `mutation { disableProfile(name: "${name}") { message } }`;
      try {
        await request("graphql", mutation);
        await this.loadSchema();
      } catch (error) {
        this.error =
          error?.response?.errors?.[0]?.message ||
          `Failed to ${activate ? "enable" : "disable"} profile "${name}"`;
        await this.loadSchema();
      }
    },
    handleInput() {
      this.dirty = true;
      this.success = null;
      this.key = Date.now();
      this.$emit("update:modelValue", this.schema);
    },
    toggleShowDiagram() {
      this.showDiagram = !this.showDiagram;
      VueScrollTo.scrollTo({ el: "#molgenis_diagram_anchor", offset: -50 });
    },
    saveSchema() {
      this.loading = true;
      this.error = null;
      this.warning = "submitting changes";
      this.success = null;
      //copy so in case of error user can continue to edit
      let schema = deepClone(this.schema);
      let tables = schema.tables ? schema.tables : [];

      //transform subclasses back into their original tables.
      //create a map of tables
      let tableMap = {};
      tables.forEach((table) => {
        tableMap[table.name] = table;
        if (table.subclasses) {
          table.subclasses.forEach((subclass) => {
            tableMap[subclass.name] = subclass;
          });
          delete table.subclasses;
        }
      });
      //redistribute the columns to subclasses
      tables.forEach((table) => {
        if (table.columns?.length) {
          table.columns.forEach((column) => {
            if (column.table !== table.oldName) {
              if (!tableMap[column.table]) {
                tableMap[column.table] = { columns: [] };
              }
              if (!tableMap[column.table].columns) {
                tableMap[column.table].columns = [];
              }
              tableMap[column.table].columns.push(column);
            }
          });
        }
      });
      tables.forEach((table) => {
        table.columns =
          table.columns?.filter((column) => column.table === table.name) || [];
      });
      tables = Object.values(tableMap);
      //add ontologies
      tables.push(...schema.ontologies);
      request(
        "graphql",
        gql`
          mutation change($tables: [MolgenisTableInput]) {
            change(tables: $tables) {
              message
            }
          }
        `,
        {
          tables: tables,
        }
      )
        .then(() => {
          this.warning = "submission complete, reloading schema";
          this.loadSchema();
          this.warning = null;
          this.success = `Schema saved`;
          this.loading = false;
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.error = error.response.errors[0].message;
          }
          this.warning = null;
          this.loading = false;
        });
    },
    loadSchema() {
      this.error = null;
      this.loading = true;
      const query = schemaQuery;
      request("graphql", query)
        .then((data) => {
          this.rawSchema = addOldNamesAndRemoveMeta(data._schema);
          this.schema = convertToSubclassTables(this.rawSchema);
          this.schemaNames = data._session.schemas;
          this.loading = false;
          this.key = Date.now();
          this.dirty = false;
          this.isManager = data._session.roles?.includes("Manager");
        })
        .catch((error) => {
          if (error.response?.errors[0]?.message) {
            this.error = error.response.errors[0].message;
          } else {
            this.error = error;
          }
          this.loading = false;
        });
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>
