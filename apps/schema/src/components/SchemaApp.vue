<template>
  <div class="container-fluid bg-white">
    <div class="sticky-top d-flex justify-content-between">
      <div class="form-inline">
        <h1>Schema editor: {{ schema.name }}</h1>
        <span v-if="schema.tables && dirty">
          <ButtonAction @click="saveSchema" class="ml-2">Save</ButtonAction>
          &nbsp;
          <ButtonAction @click="loadSchema" class="ml-2">Reset</ButtonAction>
        </span>
      </div>
      <div v-if="schema.tables">
        <ButtonAction @click="toggleShowDiagram">
          {{ showDiagram ? "Hide" : "Show" }} Diagram
        </ButtonAction>
      </div>
    </div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <MessageWarning v-if="warning">{{ warning }}</MessageWarning>
    <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    <Spinner v-if="loading === true || !schema.tables" />
    <div v-else class="row">
      <div class="col-2 bg-white">
        <div class="sticky-top mr-n3 overflow-auto vh-100" style="top: 50px">
          <SchemaToc v-model="schema" v-if="schema.tables" />
        </div>
      </div>
      <div class="bg-white col ml-2 overflow-auto">
        <a id="molgenis_diagram_anchor"></a>
        <NomnomDiagram
          :schema="schema"
          :key="JSON.stringify(schema)"
          v-if="showDiagram"
        />
        <SchemaView
          v-model="schema"
          :schemaNames="schemaNames"
          v-if="schema.tables"
          @input="
            dirty = true;
            success = null;
          "
        />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import SchemaView from "./SchemaView.vue";
import SchemaToc from "./SchemaToc.vue";
import NomnomDiagram from "./NomnomDiagram.vue";
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  MessageWarning,
  Spinner,
} from "molgenis-components";

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
  data() {
    return {
      schema: {},
      loading: false,
      graphqlError: null,
      warning: null,
      success: null,
      showDiagram: false,
      schemaNames: [],
      dirty: false,
    };
  },
  methods: {
    toggleShowDiagram() {
      this.showDiagram = !this.showDiagram;
      this.$scrollTo({ el: "#molgenis_diagram_anchor", offset: -50 });
    },
    saveSchema() {
      this.loading = true;
      this.graphqlError = null;
      this.warning = "submitting changes";
      this.success = null;
      //copy so in case of error user can continue to edit
      let schema = JSON.parse(JSON.stringify(this.schema));
      let tables = schema.tables;

      //transform subclasses back into their original tables.
      //create a map of tables
      const tableMap = tables.reduce((map, table) => {
        map[table.name] = table;
        if (table.subclasses) {
          table.subclasses.forEach(
            (subclass) => (map[subclass.name] = subclass)
          );
        }
        delete table.subclasses;
        return map;
      }, {});
      //redistribute the columns to subclasses
      tables.forEach((table) => {
        table.columns.forEach((column) => {
          if (column.table !== table.name) {
            tableMap[column.table].columns.push(column);
          }
        });
      });
      tables.forEach((table) => {
        table.columns = table.columns.filter(
          (column) => column.table === table.name
        );
      });
      tables = Object.values(tableMap);
      request(
        "graphql",
        `mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}`,
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
            this.graphqlError = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.graphqlError = error.response.errors[0].message;
          }
          this.warning = null;
          this.loading = false;
        });
      this.loading = false;
    },
    loadSchema() {
      console.log("load schema");
      this.graphqlError = null;
      this.loading = true;
      this.dirty = false;
      request(
        "graphql",
        "{_session{schemas,roles}_schema{name,tables{name,tableType,inherit,externalSchema,description,semantics,columns{name,table,position,columnType,inherited,key,refSchema,refTable,refLink,refBack,required,description,semantics,validation,visible}}}}"
      )
        .then((data) => {
          let _schema = this.addOldNamesAndRemoveMeta(data._schema);
          this.schema = this.convertToSubclassTables(_schema);
          this.schemaNames = data._session.schemas;
        })
        .catch((error) => {
          this.graphqlError = error;
          if (error.response.status === 400) {
            this.graphqlError = "Schema not found. Do you need to login?";
          }
        });
      this.loading = false;
    },
    addOldNamesAndRemoveMeta(schema) {
      if (schema) {
        if (schema.tables) {
          let tables = schema.tables.filter(
            (table) => table.tableType !== "ONTOLOGIES"
          );
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
          schema.tables = tables;
        } else {
          schema.tables = [];
        }
      }
      return schema;
    },
    convertToSubclassTables(schema) {
      //columns of subclasses should be put in root tables, sorted by position
      // this because position can only edited in context of root table
      schema.tables.forEach((table) => {
        if (table.inherit === undefined) {
          this.subclassTables(schema, table.name).forEach((subclass) => {
            //get columns from subclass tables
            table.columns.push(...subclass.columns);
            subclass.columns = [];
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
      schema.tables = schema.tables.filter(
        (table) => table.inherit === undefined
      );
      return schema;
    },
    subclassTables(schema, tableName) {
      let subclasses = schema.tables.filter(
        (table) => table.inherit === tableName
      );
      subclasses
        .map((table) => table.name)
        .forEach((subclassName) => {
          subclasses = subclasses.concat(
            this.subclassTables(schema, subclassName)
          );
        });
      return subclasses;
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>
