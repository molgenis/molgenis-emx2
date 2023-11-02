<template>
  <Spinner v-if="loading" />
  <div v-else class="container-fluid">
    <h1>Schema documentation for '{{ schema.name }}'</h1>
    <div
      v-html="figureForSchemaSVG(schema)"
      class="bg-white"
      style="max-width: 100%"
    ></div>
    Table list:
    <ul>
      <li v-for="table in schema.tables">
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
    <div v-for="table in schema.tables">
      <h2 class="pt-4">Table: {{ table.name }}</h2>
      <a :id="table.name ? table.name.replaceAll(' ', '_') : ''" />
      <div v-if="getDescription(table)">
        <h5>Definition:</h5>
        <p>{{ getDescription(table) }}</p>
      </div>
      <div class="mt-3" v-if="table.subclasses">
        <h5>Subdomains:</h5>
        Table '{{ table.name }}' has the following subclasses/specializations:
        <div
          v-html="figureForTableSVG(table)"
          class="bg-white"
          style="max-width: 100%"
        ></div>
        <div class="ml-3 mt-3">
          <b>{{ table.name }}</b>
          <span v-if="getDescription(table)"
            ><i> - {{ getDescription(table) }}</i></span
          >
        </div>
        <div class="ml-3" v-for="subclass in table.subclasses">
          <b>{{ subclass.name }}</b> (extends: {{ subclass.inherit }})
          <span v-if="getDescription(subclass)"
            ><i>- {{ getDescription(subclass) }}</i></span
          >
        </div>
      </div>
      <div>
        <div class="mt-3">
          <h5>Column definitions:</h5>
          <div v-for="column in table.columns">
            <div class="pt-3" v-if="column.columnType == 'HEADING'">
              <b>section: {{ column.name }}</b>
            </div>
            <div class="ml-3 mt-3" v-else>
              <b>{{ column.name }}</b>
              <span v-if="getDescription(column)">
                - <i>{{ getDescription(column) }}</i>
              </span>
              <div class="pl-3" v-if="table.subclasses">
                domain: {{ column.table }}
              </div>
              <div class="pl-3">
                definition:
                <ColumnDefinition :column="column"></ColumnDefinition>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    {{ schema }}
    <br /><br />
    <MessageError v-if="error">{{ error }}</MessageError>
  </div>
</template>
<script lang="ts">
import {
  addOldNamesAndRemoveMeta,
  convertToSubclassTables,
  nomnomColumnsForTable,
  schemaQuery,
} from "../utils.ts";
import { request } from "graphql-request";
import ColumnDefinition from "./ColumnDefinition.vue";
import { Spinner, MessageError } from "molgenis-components";
import { renderSvg } from "nomnoml";

export default {
  components: {
    Spinner,
    MessageError,
    ColumnDefinition,
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
    figureForTableSVG,
    figureForSchemaSVG,
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

function figureForTableSVG(table) {
  let result = `
#.table: fill=white solid
#stroke: #007bff
#direction: down
  `;
  if (table.subclasses) {
    table.subclasses.forEach((subclass) => {
      result += `
[<table>${subclass.inherit}]<:-[<table>${subclass.name}]
`;
    });
  }
  return renderSvg(result);
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
