<template>
  <Spinner v-if="loading" />
  <div v-else class="container-fluid">
    <h1>Schema documentation for '{{ schema.name }}'</h1>
    Table of contents:
    <ul>
      <li v-for="table in schema.tables">
        <a href="#"
           v-scroll-to="{
            el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
        >{{ table.name }}</a>
      </li>
    </ul>
    <div v-for="table in schema.tables">
      <h2 class="pt-4">Table: {{ table.name }}</h2>
      <a :id="(table.name ? table.name.replaceAll(' ', '_'):'')"/>
      <p v-if="getDescription(table)">{{ getDescription(table) }}</p>
      <div class="mt-3" v-if="table.subclasses">
        <h5>Subclasses:</h5>
          <ul v-for="subclass in table.subclasses">
            <li>{{subclass.name}} <i>extends</i>{{subclass.inherit}} {{getDescription(subclass)}}</li>
          </ul>
        </div>
      <div>
        <div>
      <h5>Column definitions:</h5>
        <div v-for="column in table.columns">
          <p>
            <b>{{ column.name }}</b> <span v-if="getDescription(column)">- </span><i>{{getDescription(column) }}</i>
          <div class="pl-3" v-if="table.subclasses">domain: {{ column.table }}</div>
          <div class="pl-3">definition: <ColumnDefinition :column="column"></ColumnDefinition></div>
          </p>
        </div>
        </div>
      </div>
    </div>

    <br /><br />
    {{ JSON.stringify(schema) }}
    <MessageError v-if="error">{{ error }}</MessageError>
  </div>
</template>
<script lang="ts">
import {
  addOldNamesAndRemoveMeta,
  convertToSubclassTables,
  schemaQuery,
} from "../utils.ts";
import { request } from "graphql-request";
import ColumnDefinition from "./ColumnDefinition.vue";
import { Spinner, MessageError } from "molgenis-components";

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
