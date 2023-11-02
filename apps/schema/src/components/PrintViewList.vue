<template>
  <Spinner v-if="loading" />
  <div v-else class="container-fluid" id="print">
    <h1>Schema documentation for '{{ schema.name }}'</h1>
    Table list:
    <ul>
      <li
        v-for="table in schema.tables.filter(
          (table) => table.tableType == 'DATA'
        )"
      >
        <a
          href="#"
          v-scroll-to="{
            el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
          >Table: {{ table.name }}</a
        >
      </li>
    </ul>
    <a
      href="#"
      v-scroll-to="{
        el: '#appendix',
        offset: -50,
      }"
    >
      Appendix: full schema diagram
    </a>
    <div
      v-for="table in schema.tables.filter(
        (table) => table.tableType == 'DATA'
      )"
    >
      <h2 class="pt-4">Table: {{ table.name }}</h2>
      <a :id="table.name ? table.name.replaceAll(' ', '_') : ''" />
      <div>
        <h3>Overview and relationships:</h3>
        <SchemaDiagram :tables="[table]" />
      </div>
      <div v-if="getDescription(table)">
        <br />
        <h3>Table definition:</h3>
        <div>{{ getDescription(table) }}</div>
      </div>
      <div v-if="table.subclasses">
        <br />
        <h3>Extended table definitions:</h3>
        Table '{{ table.name }}' has the following subclasses/specializations:
        <div>
          <div>
            <b>{{ table.name }}</b>
          </div>
          <div v-if="getDescription(table)">
            <i>{{ getDescription(table) }}</i>
          </div>
        </div>
        <div v-for="subclass in table.subclasses">
          <div>
            <b>{{ subclass.name }}</b> (extends: {{ subclass.inherit }})
          </div>
          <div v-if="getDescription(subclass)">
            <i>{{ getDescription(subclass) }}</i>
          </div>
        </div>
      </div>
      <div v-if="table.columns">
        <br />
        <h3>Column definitions:</h3>
        <div v-for="column in table.columns">
          <div v-if="column.columnType == 'HEADING'">
            <br />
            <b
              ><u>section: {{ column.name }}</u></b
            >
          </div>
          <div v-else>
            <br />
            <div>
              <b>{{ column.name }}</b>
            </div>
            <div v-if="getDescription(column)">
              <i>{{ getDescription(column) }}</i>
            </div>
            <div v-if="table.subclasses">
              &nbsp;&nbsp;&nbsp;&nbsp;domain:
              {{ column.table }}
            </div>
            <div>
              &nbsp;&nbsp;&nbsp;&nbsp;constraints:
              <ColumnDefinition :column="column"></ColumnDefinition>
            </div>
          </div>
        </div>
      </div>
    </div>
    <h2>Appendix: schema UML</h2>
    <a id="appendix" />
    <SchemaDiagram
      :tables="this.schema.tables.filter((table) => table.tableType === 'DATA')"
    />
    <MessageError v-if="error">{{ error }}</MessageError>
  </div>
</template>

<style scoped>
h3 {
  font-size: 18px;
  text-decoration: underline;
  margin-top: 10px;
}
h2 {
  font-size: 20px;
}
h1 {
  font-size: 24px;
}
body {
  font-size: 16px;
}
p {
  margin: 0px;
}
</style>

<script lang="ts">
import {
  addOldNamesAndRemoveMeta,
  convertToSubclassTables,
  schemaQuery,
} from "../utils.ts";
import { request } from "graphql-request";
import ColumnDefinition from "./ColumnDefinition.vue";
import { Spinner, MessageError } from "molgenis-components";
import { renderSvg } from "nomnoml";
import SchemaDiagram from "./SchemaDiagram.vue";

export default {
  components: {
    Spinner,
    MessageError,
    ColumnDefinition,
    SchemaDiagram,
  },
  data() {
    return {
      schema: {},
      loading: false,
      error: null,
    };
  },
  methods: {
    //todo: we could move this method into a helper method so it is shared with Schema.vue
    loadSchema() {
      this.error = null;
      this.loading = true;
      const query = schemaQuery;
      request("graphql", query)
        .then((data) => {
          const rawSchema = addOldNamesAndRemoveMeta(data._schema);
          this.schema = convertToSubclassTables(rawSchema);
          this.loading = false;
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
    getDescription,
  },
  created() {
    this.loadSchema();
  },
};

function getDescription(item) {
  if (item.descriptions?.filter((desc) => desc.locale == "en").length == 1) {
    return item.descriptions?.filter((desc) => desc.locale == "en")[0].value;
  }
}

function figureForSchemaSVG(schema) {
  let result = `
#.table: fill=white solid
#stroke: #007bff
#direction: down
  `;
  schema.tables.forEach((table) => {
    result += `
    [<table>${table.name}]
    `;
    table.columns.forEach((column) => {
      if (column.columnType == "REF" || column.columnType == "REF_ARRAY") {
        result += `[<table>${findRootTable(schema, column.refTable)}]<- ${
          column.name
        } [<table>${table.name}]\n`;
      }
    });
  });
  return renderSvg(result);
}

function findRootTable(schema, tableName) {
  let result;
  schema.tables.forEach((table) => {
    if (table.name == tableName) {
      result = table.name;
    }
    if (table.subclasses) {
      table.subclasses.forEach((subclass) => {
        if (subclass.name == tableName) {
          result = table.name;
        }
      });
    }
  });
  return result;
}
</script>
