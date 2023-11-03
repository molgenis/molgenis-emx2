<template>
  <Spinner v-if="loading" />
  <div v-else class="container-fluid">
    <h1>Schema documentation for '{{ schema.name }}'</h1>
    Table of contents:
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
          >{{ table.name }}</a
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
      <br />
      <b>Display options:</b>
      <InputCheckbox
        id="options"
        :required="true"
        v-model="options"
        :options="[SHOW_TABLE_DIAGRAMS]"
      />
      <h2 class="pt-4">Table: {{ table.name }}</h2>
      <a :id="table.name ? table.name.replaceAll(' ', '_') : ''" />
      <p v-if="getDescription(table)">{{ getDescription(table) }}</p>
      <div v-if="options.includes(SHOW_TABLE_DIAGRAMS)">
        <h3>Overview and relationships:</h3>
        <SchemaDiagram :tables="[table]" />
      </div>
      <div class="mt-3" v-if="table.subclasses">
        <h3>Subdomains:</h3>
        Table '{{ table.name }}' has the following subclasses/specializations:
        <table class="table table-bordered">
          <thead>
            <th style="width: 16em">subclass table name</th>
            <th style="width: 8em">extends</th>
            <th>description</th>
          </thead>
          <tbody>
            <tr>
              <td>{{ table.name }}</td>
              <td>-</td>
              <td>{{ getDescription(table) }}</td>
            </tr>
            <tr v-for="subclass in table.subclasses">
              <td>{{ subclass.name }}</td>
              <td>{{ subclass.inherit }}</td>
              <td>{{ getDescription(subclass) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div>
        <div>
          <h3>Column definitions:</h3>
          <table class="table table-bordered">
            <thead>
              <th style="width: 16em">column name</th>
              <th style="width: 8em">type</th>
              <th style="width: 32em">description</th>
              <th style="width: 16em" v-if="table.subclasses">domain</th>
              <th>definition</th>
            </thead>
            <tbody>
              <tr v-for="column in table.columns" border>
                <td>{{ column.name }}</td>
                <td>{{ column.columnType.toLowerCase() }}</td>
                <td>{{ getDescription(column) }}</td>
                <td v-if="table.subclasses">{{ column.table }}</td>
                <td><ColumnDefinition :column="column"></ColumnDefinition></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <br /><br />
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
import { Spinner, MessageError, InputCheckbox } from "molgenis-components";
import SchemaDiagram from "./SchemaDiagram.vue";

const SHOW_TABLE_DIAGRAMS = "show table diagrams";

export default {
  components: {
    SchemaDiagram,
    Spinner,
    MessageError,
    ColumnDefinition,
    InputCheckbox,
  },
  data() {
    return {
      schema: {},
      loading: false,
      error: null,
      options: [],
      SHOW_TABLE_DIAGRAMS,
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
</script>
