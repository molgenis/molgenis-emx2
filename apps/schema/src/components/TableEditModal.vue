<template>
  <LayoutModal
    v-if="modalVisible === true"
    :title="title"
    :isCloseButtonShown="false"
  >
    <template v-slot:body>
      <MessageWarning v-if="table.drop">Marked for deletion</MessageWarning>
      <InputString
        id="table_name"
        v-model="table.name"
        label="Name"
        :errorMessage="nameInvalid"
      />
      <InputTextLocalized
        id="table_label"
        v-model="table.labels"
        label="label"
        :locales="locales"
      />
      <InputTextLocalized
        id="table_description"
        v-model="table.descriptions"
        label="description"
        :locales="locales"
      />
      <InputSelect
        v-if="rootTable !== undefined"
        id="table_extends"
        v-model="table.inheritName"
        :required="true"
        :options="inheritOptions"
        :readonly="table.oldName !== undefined"
        :errorMessage="subclassInvalid"
        label="Extends table (can not be edited after creation)"
      />
      <template v-if="canChooseParentSchema">
        <InputSelect
          id="table_refSchema"
          v-model="selectedRefSchema"
          :options="schemaNames"
          @update:modelValue="loadRefSchema"
          label="refSchema"
          description="When you want to extend a table from another schema (can not be edited after creation)"
        />
        <Spinner v-if="loadingRefSchema" />
        <MessageError v-else-if="refSchemaError">
          {{ refSchemaError }}
        </MessageError>
        <InputSelect
          v-else-if="selectedRefSchema"
          id="table_parent_extends"
          v-model="table.inheritName"
          :options="parentTableNames"
          :errorMessage="parentTableInvalid"
          :noOptionsProvidedMessage="`No table found in schema '${selectedRefSchema}'`"
          label="Extends table (can not be edited after creation)"
        />
      </template>
      <ArrayInput
        id="table_semantics"
        columnType="STRING_ARRAY"
        v-model="table.semantics"
        label="Semantics"
      />
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
    @click="showModal"
  />
</template>

<script>
import {
  constants,
  Client,
  InputString,
  LayoutModal,
  IconAction,
  ButtonAction,
  MessageError,
  MessageWarning,
  InputSelect,
  ArrayInput,
  ButtonAlt,
  Spinner,
  deepClone,
  InputTextLocalized,
} from "molgenis-components";
import { toInheritSchemaName, extendableTableNames } from "../inheritSchema";

export default {
  components: {
    ArrayInput,
    LayoutModal,
    InputString,
    IconAction,
    ButtonAction,
    MessageError,
    MessageWarning,
    InputSelect,
    ButtonAlt,
    Spinner,
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
    schemaNames: {
      type: Array,
      default: () => [],
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
      selectedRefSchema: undefined,
      refSchema: undefined,
      refSchemaError: null,
      loadingRefSchema: false,
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
              .map((subclass) => subclass.name)
              .filter((name) => name !== this.table.name)
          );
        }
        this.table.inheritName = result[0];
        return result;
      }
      return undefined;
    },
    canChooseParentSchema() {
      return (
        this.tableType !== "ontology" &&
        this.rootTable === undefined &&
        this.table.oldName === undefined
      );
    },
    parentTableNames() {
      return this.selectedRefSchema === this.schema.name
        ? extendableTableNames(this.schema)
        : extendableTableNames(this.refSchema);
    },
    parentTableInvalid() {
      return this.canChooseParentSchema &&
        this.selectedRefSchema &&
        !this.table.inheritName
        ? "Extends table is required when refSchema is set"
        : null;
    },
    nameInvalid() {
      if (
        this.table.name === undefined ||
        this.table.name.trim() === "" ||
        this.table.name.search(constants.TABLE_NAME_REGEX)
      ) {
        return "Name is required and must start with a letter, followed by zero or more letters, numbers, spaces or underscores. A space immediately before or after an underscore is not allowed. The character limit is 31.";
      }
      if (
        this.modelValue?.name !== this.table.name &&
        ((this.schema.tables &&
          this.schema.tables.filter(
            (table) =>
              table.name === this.table.name ||
              (table.subclasses !== undefined &&
                table.subclasses
                  .map((subclass) => subclass.name)
                  .includes(this.table.name))
          ).length > 0) ||
          (this.schema.ontologies &&
            this.schema.ontologies.filter(
              (ontology) => ontology.name === this.table.name
            ).length > 0))
      ) {
        return "Name should be unique (no other table or ontology can have same name)";
      }
      return null;
    },
    subclassInvalid() {
      return this.inheritOptions && this.table.inheritName === undefined
        ? "Extends is required in case of subclass"
        : null;
    },
    isDisabled() {
      return (
        this.nameInvalid !== null ||
        this.subclassInvalid !== null ||
        this.parentTableInvalid !== null
      );
    },
  },
  methods: {
    showModal() {
      this.reset();
      this.modalVisible = true;
    },
    async loadRefSchema() {
      this.refSchemaError = null;
      this.refSchema = undefined;
      this.table.inheritName = undefined;
      if (!this.selectedRefSchema) {
        return;
      }
      if (this.selectedRefSchema === this.schema.name) {
        return;
      }
      this.loadingRefSchema = true;
      try {
        this.refSchema = await Client.newClient(
          this.selectedRefSchema
        ).fetchSchemaMetaData();
      } catch (error) {
        console.error(error);
        this.refSchemaError = `Cannot read schema '${this.selectedRefSchema}': ${error}`;
      }
      this.loadingRefSchema = false;
    },
    emitOperation() {
      const inheritSchemaName = toInheritSchemaName(
        this.selectedRefSchema,
        this.schema.name
      );
      if (inheritSchemaName) {
        this.table.inheritSchemaName = inheritSchemaName;
      } else {
        delete this.table.inheritSchemaName;
      }
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
      this.selectedRefSchema = this.table.inheritSchemaName;
      this.refSchema = undefined;
      this.refSchemaError = null;
    },
  },
  created() {
    this.reset();
  },
  emits: ["add", "update:modelValue"],
};
</script>
