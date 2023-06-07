<template>
  <LayoutModal
    v-if="modalVisible === true"
    :title="title"
    :isCloseButtonShown="false">
    <template v-slot:body>
      <MessageWarning v-if="table.drop">Marked for deletion</MessageWarning>
      <InputString
        id="table_name"
        v-model="table.name"
        label="Name"
        :errorMessage="nameInvalid" />
      <InputTextLocalized
        id="table_label"
        v-model="table.labels"
        label="label"
        :locales="locales" />
      <InputTextLocalized
        id="table_description"
        v-model="table.descriptions"
        label="description"
        :locales="locales" />
      <InputSelect
        v-if="rootTable !== undefined"
        id="table_extends"
        v-model="table.inherit"
        :required="true"
        :options="inheritOptions"
        :readonly="table.oldName !== undefined"
        :errorMessage="subclassInvalid"
        label="Extends table (can not be edited after creation)" />
      <InputString
        id="table_semantics"
        :list="true"
        v-model="table.semantics"
        label="Semantics (comma separated list of IRI defining type, and/or keyword 'id')" />
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="cancel">Cancel</ButtonAlt>
      <ButtonAction @click="emitOperation" :disabled="isDisabled"
        >Apply</ButtonAction
      >
    </template>
  </LayoutModal>
  <IconAction
    v-else
    class="btn-sm hoverIcon"
    :icon="operation === 'add' ? 'plus' : 'pencil-alt'"
    @click="showModal" />
</template>

<script>
import {
  InputString,
  LayoutModal,
  IconAction,
  ButtonAction,
  MessageWarning,
  InputSelect,
  ButtonAlt,
  deepClone,
  InputTextLocalized,
} from "molgenis-components";

export default {
  components: {
    LayoutModal,
    InputString,
    IconAction,
    ButtonAction,
    MessageWarning,
    InputSelect,
    ButtonAlt,
    InputTextLocalized,
  },
  props: {
    /** Existing Table metadata object entered as v-model. In case of a new table this should be left empty. */
    modelValue: {
      type: Object,
      required: false,
    },
    /** root table, used in case of subclasses */
    rootTable: {
      type: Object,
      required: false,
    },
    /** schema, used for uniques check */
    schema: {
      type: Object,
      required: true,
    },
    /** action, either 'add' or 'input */
    operation: {
      type: String,
      default: "update:modelValue",
    },
    /** type, either 'ontology' or nothing*/
    tableType: {
      type: String,
    },
    locales: {
      type: Array,
      default: ["en"],
    },
  },
  data: function () {
    return {
      /** copy of table metadata being edited now */
      table: {},
      /** whether modal is visible */
      modalVisible: false,
    };
  },
  computed: {
    title() {
      return this.tableType === "ontology"
        ? `${this.operation} ontology definition`
        : `${this.operation} table definition`;
    },
    inheritOptions() {
      if (this.rootTable) {
        const result = [this.rootTable.name];
        if (this.rootTable.subclasses !== undefined) {
          result.push(
            ...this.rootTable.subclasses
              .map(subclass => subclass.name)
              .filter(name => name !== this.table.name)
          );
        }
        return result;
      }
      return undefined;
    },
    nameInvalid() {
      if (
        this.table.name === undefined ||
        this.table.name.trim() === "" ||
        this.table.name.search(/^[a-zA-Z0-9 _]*$/)
      ) {
        return "Name is required and can only contain 'azAZ_ '";
      }
      if (
        this.modelValue?.name !== this.table.name &&
        ((this.schema.tables &&
          this.schema.tables.filter(
            table =>
              table.name === this.table.name ||
              (table.subclasses !== undefined &&
                table.subclasses
                  .map(subclass => subclass.name)
                  .includes(this.table.name))
          ).length > 0) ||
          (this.schema.ontologies &&
            this.schema.ontologies.filter(
              ontology => ontology.name === this.table.name
            ).length > 0))
      ) {
        return "Name should be unique (no other table or ontology can have same name)";
      }
      return null;
    },
    subclassInvalid() {
      return this.inheritOptions && this.table.inherit === undefined
        ? "Extends is required in case of subclass"
        : null;
    },
    isDisabled() {
      return this.nameInvalid !== null || this.subclassInvalid !== null;
    },
  },
  methods: {
    showModal() {
      if (!this.modelValue) {
        this.table = {};
      }
      this.modalVisible = true;
    },
    emitOperation() {
      this.$emit(this.operation, this.table);
      this.modalVisible = false;
    },
    cancel() {
      this.reset();
      this.modalVisible = false;
    },
    reset() {
      if (this.modelValue) {
        this.table = deepClone(this.modelValue);
      } else {
        this.table = {};
      }
    },
  },
  created() {
    this.reset();
  },
  emits: ["add", "update:modelValue"],
};
</script>
