<template>
  <div
    class="schema-toc sticky-top mr-n3 overflow-auto"
    style="top: 50px; height: 90vh"
  >
    <div class="hoverContainer">
      <label class="m-0">Tables:</label>
      <TableEditModal
        v-if="isManager"
        @add="addTable"
        operation="add"
        :schema="schema"
        @update:modelValue="$emit('update:modelValue', schema)"
      />
    </div>
    <div v-if="schema.tables?.length > 0">
      <span
        v-for="table in schema.tables"
        :key="table.name + schema.tables.length"
        class="ml-2"
      >
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
    <p v-else><i>none defined</i></p>
    <div class="hoverContainer">
      <label class="m-0 mt-2">Ontologies:</label>
      <TableEditModal
        v-if="isManager"
        @add="addOntology"
        operation="add"
        tableType="ontology"
        :schema="schema"
        @update:modelValue="$emit('update:modelValue', schema)"
      />
    </div>
    <div v-if="schema.ontologies?.length > 0">
      <div
        v-for="ontology in schema.ontologies"
        :key="ontology.name"
        class="mb-0 ml-2"
      >
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + ontology.name ? ontology.name.replaceAll(' ', '_') : '',
            offset: -200,
          }"
        >
          {{ ontology.name }}
        </a>
      </div>
    </div>
    <p v-else><i>none defined</i></p>
  </div>
</template>

<style scoped>
/*
  Place below other sticky page component ( Schema.vue)
*/
div.schema-toc.sticky-top {
  z-index: 998;
}
</style>

<script>
import TableEditModal from "./TableEditModal.vue";

export default {
  components: { TableEditModal },
  props: {
    /** schema v-model */
    modelValue: {
      type: Object,
      required: true,
    },
    isManager: {
      type: Boolean,
      default: false,
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
      if (!Array.isArray(this.schema.tables)) {
        this.schema.tables = [];
      }
      this.schema.tables.push(table);
      this.$emit("update:modelValue", this.schema);
    },
    addOntology(ontology) {
      this.$scrollTo("#molgenis_bottom_ontologies_anchor");
      if (!Array.isArray(this.schema.ontologies)) {
        this.schema.ontologies = [];
      }
      ontology.tableType = "ONTOLOGIES";
      this.schema.ontologies.push(ontology);
      this.$emit("update:modelValue", this.schema);
    },
  },
  created() {
    this.schema = this.modelValue;
  },
  emits: ["update:modelValue"],
};
</script>
