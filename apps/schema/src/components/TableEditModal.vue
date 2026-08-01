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
        v-if="rootTable !== undefined && table.oldName === undefined"
        id="table_extends"
        v-model="table.inheritNames"
        :required="true"
        :multiple="true"
        :options="inheritOptions"
        :errorMessage="subclassInvalid"
        description="Hold ctrl (Windows) or cmd (Mac) to select multiple parent tables"
        label="Extends tables (can not be edited after creation)"
      />
      <div v-else-if="rootTable !== undefined">
        <label class="mb-0"
          >Extends tables (can not be edited after creation):</label
        >
        <div class="ml-4">{{ table.inheritNames?.join(", ") }}</div>
      </div>
      <InputSelect
        v-if="rootTable !== undefined"
        id="table_type"
        v-model="table.tableType"
        :options="tableTypeOptions"
        :readonly="table.oldName !== undefined"
        label="Table type (can not be edited after creation)"
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
          v-model="table.inheritNames"
          :multiple="true"
          :options="parentTableNames"
          :errorMessage="parentTableInvalid"
          :noOptionsProvidedMessage="`No table found in schema '${selectedRefSchema}'`"
          description="Hold ctrl (Windows) or cmd (Mac) to select multiple parent tables"
          label="Extends tables (can not be edited after creation)"
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
  getSelectableTableTypes,
  DEFAULT_TABLE_TYPE,
} from "molgenis-components";
import {
  toInheritSchemaName,
  extendableTableNames,
  parentTableOptions,
  resolveInheritNames,
} from "../inheritSchema";

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
      return this.rootTable
        ? parentTableOptions(this.rootTable, this.table.name)
        : undefined;
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
        !this.table.inheritNames?.length
        ? "Extends table is required when refSchema is set"
        : null;
    },
    tableTypeOptions() {
      return getSelectableTableTypes();
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
      return this.inheritOptions && !this.table.inheritNames?.length
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
      this.table.inheritNames = undefined;
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
      this.table.inheritName = this.table.inheritNames?.[0];
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
        this.initNewTable();
      }
      if (this.inheritOptions) {
        this.table.inheritNames = resolveInheritNames(
          this.table.inheritNames,
          this.inheritOptions
        );
      }
      this.selectedRefSchema = this.table.inheritSchemaName;
      this.refSchema = undefined;
      this.refSchemaError = null;
    },
    initNewTable() {
      this.table = {};
      if (this.rootTable !== undefined) {
        this.table.tableType = DEFAULT_TABLE_TYPE;
        this.table.inheritNames = [this.rootTable.name];
      }
    },
  },
  created() {
    this.reset();
  },
  emits: ["add", "update:modelValue"],
};
</script>
