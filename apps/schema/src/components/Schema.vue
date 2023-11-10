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
          <ButtonAction href="./#/print" target="_blank" class="ml-2">
            Show printable table
          </ButtonAction>
          <ButtonAction href="./#/print-list" target="_blank" class="ml-2">
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
        if (table.columns !== undefined) {
          table.columns.forEach((column) => {
            if (column.table !== table.oldName) {
              if (tableMap[column.table].columns === undefined) {
                tableMap[column.table].columns = [];
              }
              tableMap[column.table].columns.push(column);
            }
          });
        }
      });
      tables.forEach((table) => {
        delete table.schemaId;
        table.columns = table.columns
          ? table.columns.filter((column) => column.table === table.name)
          : [];
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
        })
        .catch((error) => {
          if (error.response.status === "403") {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.error = error.response.errors[0].message;
          }
          this.warning = null;
          this.loading = false;
        });
      this.loading = false;
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
