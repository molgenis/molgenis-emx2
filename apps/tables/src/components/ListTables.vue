<template>
  <div v-if="schema" class="container">
    <h1>Tables in '{{ schema.label }}'</h1>
    <MessageWarning v-if="!schema.tables">
      No tables found. You might want to go to design
      <a href="../schema/">design</a> or
      <a href="../updownload/">upload</a> your schema to create them.
    </MessageWarning>
    <div v-else>
      Download all tables:
      <a href="../api/zip">zip</a> | <a href="../api/excel">excel</a> |
      <a href="../api/rdf?format=jsonld">jsonld</a> |
      <a href="../api/rdf?format=ttl">ttl</a><br />
      <InputSearch
        id="tables-list-search-input"
        placeholder="search in tables"
        v-model="search"
      />
      <h2>Data tables</h2>
      <TablesTable
        v-if="tables.length > 0"
        :tables="tables"
        :locale="session?.locale"
      />
      <p v-else>No tables found</p>
      <h2>Ontology tables</h2>
      <p>
        These tables are automatically created for each column with type =
        ontology or ontology_array.
      </p>
      <TablesTable v-if="ontologies.length > 0" :tables="ontologies" />
      <p v-else>No ontologies found</p>
    </div>
  </div>
</template>

<script>
import { MessageWarning, InputSearch } from "molgenis-components";
import TablesTable from "./TablesTable.vue";

export default {
  name: "App",
  components: {
    MessageWarning,
    InputSearch,
    TablesTable,
  },
  props: {
    session: Object,
    schema: Object,
  },
  data() {
    return {
      tableFilter: [],
      search: null,
    };
  },
  computed: {
    count() {
      if (!this.schema || !this.schema.tables) {
        return 0;
      }
      return this.schema.tables.length;
    },
    tablesFiltered() {
      if (!this.schema || !this.schema.tables) {
        return [];
      }
      if (this.search && this.search.trim().length > 0) {
        let terms = this.search.toLowerCase().split(" ");
        return this.schema.tables
          .filter((table) => table.externalSchema === this.schema.id)
          .filter((table) =>
            terms.every(
              (term) =>
                table.label.toLowerCase().includes(term) ||
                (table.description &&
                  table.description.toLowerCase().includes(term))
            )
          );
      } else {
        return this.schema.tables;
      }
    },
    tables() {
      return this.tablesFiltered.filter((table) => table.tableType == "DATA");
    },
    ontologies() {
      return this.tablesFiltered.filter(
        (table) => table.tableType == "ONTOLOGIES"
      );
    },
  },
};
</script>
