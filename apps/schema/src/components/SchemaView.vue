<template>
  <div>
    <div id="molgenis_tables_container">
      <span v-if="schema.tables && schema.tables?.length > 0">
        <span
          v-for="(table, index) in schema.tables"
          :key="JSON.stringify(table) + schema.tables.length + '_' + index"
        >
          <TableView
            v-if="table.inherit === undefined"
            v-model="schema.tables[index]"
            :schema="schema"
            :schemaNames="schemaNames"
            @input="$emit('input', schema)"
            @delete="deleteTable(index)"
          />
        </span>
      </span>
      <a id="molgenis_bottom_tables_anchor"></a>
      <div v-if="schema.ontologies && schema.ontologies?.length > 0">
        <h4>Ontologies</h4>
        <table
          v-if="schema.ontologies && schema.ontologies?.length > 0"
          class="table table-bordered"
        >
          <thead>
            <tr>
              <th style="width: 20ch" scope="col">name</th>
              <th scope="col">description</th>
            </tr>
          </thead>
          <tbody>
            <OntologyView
              v-for="(ontology, index) in schema.ontologies"
              :key="schema.ontologies.length + '_' + index"
              v-model="schema.ontologies[index]"
              :schema="schema"
              :schemaNames="schemaNames"
              @input="$emit('input', schema)"
              @delete="deleteOntology(index)"
            />
          </tbody>
        </table>
      </div>
    </div>
    <a id="molgenis_bottom_ontologies_anchor"></a>
  </div>
</template>

<script>
import TableView from "./TableView.vue";
import OntologyView from "./OntologyView.vue";

export default {
  components: {
    TableView,
    OntologyView,
  },
  props: {
    value: {
      type: Object,
      required: true,
    },
    schemaNames: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      schema: {},
    };
  },
  methods: {
    deleteTable(index) {
      this.schema.tables.splice(index, 1);
      this.$emit("input", this.schema);
    },
    deleteOntology(index) {
      this.schema.ontologies.splice(index, 1);
      this.$emit("input", this.schema);
    },
  },
  created() {
    this.schema = this.value;
  },
};
</script>
