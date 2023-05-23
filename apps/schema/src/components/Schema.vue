<template>
  <div class="container-fluid bg-white">
    <div class="sticky-top bg-white">
      <div class="d-flex flex-row">
        <h1>Schema: {{ schema.name }}</h1>
        <div class="form-inline">
          <ButtonAction v-if="dirty" @click="saveSchema" class="ml-2"> Save </ButtonAction>
          <ButtonAction v-if="dirty" @click="loadSchema" class="ml-2"> Reset </ButtonAction>
          <ButtonAction v-if="schema.tables?.length > 0" @click="toggleShowDiagram" class="ml-2">
            {{ showDiagram ? "Hide" : "Show" }} Diagram
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
        <NomnomDiagram :schema="schema" v-if="showDiagram" />
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
import { request, gql } from "graphql-request";
import SchemaView from "./SchemaView.vue";
import SchemaToc from "./SchemaToc.vue";
import NomnomDiagram from "./NomnomDiagram.vue";
import { ButtonAction, MessageError, MessageSuccess, MessageWarning, Spinner, deepClone } from "molgenis-components";
import VueScrollTo from "vue-scrollto";

export default {
  components: {
    SchemaView,
    ButtonAction,
    MessageError,
    MessageWarning,
    MessageSuccess,
    SchemaToc,
    Spinner,
    NomnomDiagram,
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
        delete table.externalSchema;
        table.columns = table.columns ? table.columns.filter((column) => column.table === table.name) : [];
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
      const query = gql`
        {
          _session {
            schemas
            roles
          }
          _schema {
            name
            tables {
              name
              tableType
              inherit
              externalSchema
              labels {
                locale
                value
              }
              descriptions {
                locale
                value
              }
              semantics
              columns {
                id
                name
                labels {
                  locale
                  value
                }
                table
                position
                columnType
                inherited
                key
                refSchema
                refTable
                refLink
                refBack
                refLabel
                required
                readonly
                descriptions {
                  locale
                  value
                }
                semantics
                validation
                visible
                computed
              }
            }
          }
        }
      `;
      request("graphql", query)
        .then((data) => {
          this.rawSchema = this.addOldNamesAndRemoveMeta(data._schema);
          this.schema = this.convertToSubclassTables(this.rawSchema);
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
    addOldNamesAndRemoveMeta(rawSchema) {
      //deep copy to not change the input
      const schema = deepClone(rawSchema);
      if (schema) {
        //normal tables
        let tables = !schema.tables
          ? []
          : schema.tables.filter((table) => table.tableType !== "ONTOLOGIES" && table.externalSchema === schema.name);
        tables.forEach((t) => {
          t.oldName = t.name;
          if (t.columns) {
            t.columns = t.columns
              .filter((c) => !c.name.startsWith("mg_"))
              .map((c) => {
                c.oldName = c.name;
                return c;
              })
              .filter((c) => !c.inherited);
          } else {
            t.columns = [];
          }
        });
        schema.ontologies = !schema.tables
          ? []
          : schema.tables.filter((table) => table.tableType === "ONTOLOGIES" && table.externalSchema === schema.name);
        //set old name so we can delete them properly
        schema.ontologies.forEach((o) => {
          o.oldName = o.name;
        });
        schema.tables = tables;
      }

      return schema;
    },
    convertToSubclassTables(rawSchema) {
      //deep copy to not change the input
      const schema = deepClone(rawSchema);
      //columns of subclasses should be put in root tables, sorted by position
      // this because position can only edited in context of root table
      schema.tables.forEach((table) => {
        if (table.inherit === undefined) {
          this.getSubclassTables(schema, table.name).forEach((subclass) => {
            //get columns from subclass tables
            table.columns.push(...subclass.columns);
            //remove the columns from subclass table
            subclass.columns = [];
            subclass.oldName = subclass.name;
            //add subclass to root table
            if (!table.subclasses) {
              table.subclasses = [subclass];
            } else {
              table.subclasses.push(subclass);
            }
          });
        }
        //sort
        table.columns.sort((a, b) => a.position - b.position);
      });
      //remove the subclass tables
      schema.tables = schema.tables.filter((table) => table.inherit === undefined);
      return schema;
    },
    getSubclassTables(schema, tableName) {
      let subclasses = schema.tables.filter((table) => table.inherit === tableName);
      return subclasses.concat(
        subclasses
          .map((table) => {
            return this.getSubclassTables(schema, table.name);
          })
          .flat(1)
      );
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>
