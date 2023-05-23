<template>
  <div>
    <div id="molgenis_tables_container">
      <span v-if="schema.tables && schema.tables?.length > 0">
        <span v-for="(table, index) in schema.tables" :key="index">
          <TableView
            v-if="table.inherit === undefined"
            v-model="schema.tables[index]"
            :schema="schema"
            :schemaNames="schemaNames"
            @update:modelValue="$emit('update:modelValue', schema)"
            @delete="deleteTable(index)"
            :isManager="isManager"
            :locales="locales"
          />
        </span>
      </span>
      <a id="molgenis_bottom_tables_anchor"></a>
      <div v-if="schema.ontologies && schema.ontologies?.length > 0">
        <div class="hoverContainer">
          <h4 class="d-inline-block">Ontologies</h4>
          <TableEditModal
            v-if="isManager"
            @add="addOntology"
            operation="add"
            tableType="ontology"
            :schema="schema"
            @update:modelValue="$emit('update:modelValue', schema)"
          />
        </div>
        <table v-if="schema.ontologies && schema.ontologies?.length > 0" class="table table-bordered table-sm">
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
              @update:modelValue="$emit('update:modelValue', schema)"
              @delete="deleteOntology(index)"
              :isManager="isManager"
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
import TableEditModal from "./TableEditModal.vue";

export default {
  components: {
    TableView,
    OntologyView,
    TableEditModal,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
    schemaNames: {
      type: Array,
      required: true,
    },
    isManager: {
      type: Boolean,
      default: false,
    },
    locales: {
      type: Array,
    },
  },
  data() {
    return {
      schema: {},
    };
  },
  methods: {
    addOntology(ontology) {
      if (!Array.isArray(this.schema.ontologies)) {
        this.schema.ontologies = [];
      }
      ontology.tableType = "ONTOLOGIES";
      this.schema.ontologies.push(ontology);
      this.$emit("update:modelValue", this.schema);
    },
    deleteTable(index) {
      this.schema.tables.splice(index, 1);
      this.$emit("update:modelValue", this.schema);
    },
    deleteOntology(index) {
      this.schema.ontologies.splice(index, 1);
      this.$emit("update:modelValue", this.schema);
    },
  },
  created() {
    this.schema = this.modelValue;
  },
  emits: ["update:modelValue"],
};
</script>
