<template>
  <div class="sticky-top mr-n3 overflow-auto" style="top: 50px; height: 90vh">
    <div class="hoverContainer mb-2">
      <h4 style="display: inline-block">Tables:</h4>
      <TableEditModal @add="addTable" operation="add" :schema="schema" />
    </div>
    <div v-if="schema.tables?.length > 0">
      <span v-for="table in schema.tables" :key="table.name">
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
          >{{ table.name }}</a
        >
        <div v-if="table.subclasses">
          <div
            v-for="subtable in table.subclasses"
            :key="subtable.name"
            class="ml-4"
          >
            <a
              :href="'#'"
              v-scroll-to="{
                el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
                offset: -50,
              }"
              >{{ subtable.name }}</a
            >
          </div>
        </div>
        <br v-else />
      </span>
    </div>
    <p v-else>No tables defined</p>
    <div class="mb-2 hoverContainer">
      <h4 style="display: inline-block">Ontologies:</h4>
      <TableEditModal
        @add="addOntology"
        operation="add"
        tableType="ontology"
        :schema="schema"
      />
    </div>
    <ul v-if="schema.ontologies?.length > 0">
      <li v-for="ontology in schema.ontologies" :key="ontology.name">
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + (ontology.name ? ontology.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
        >
          {{ ontology.name }}
        </a>
      </li>
    </ul>
    <p v-else>No ontologies defined</p>
  </div>
</template>

<script>
import Vue from "vue";
import VueScrollTo from "vue-scrollto";
import TableEditModal from "./TableEditModal.vue";

Vue.use(VueScrollTo);

export default {
  components: { TableEditModal },
  props: {
    /** schema v-model */
    value: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      schema: null,
    };
  },
  methods: {
    addTable(table) {
      this.$scrollTo("#molgenis_bottom_tables_anchor");
      this.schema.tables.push(table);
      this.$emit("input", this.schema);
    },
    addOntology(ontology) {
      this.$scrollTo("#molgenis_bottom_ontologies_anchor");
      this.schema.ontologies.push(ontology);
      this.$emit("input", this.schema);
    },
  },
  created() {
    this.schema = this.value;
  },
};
</script>
