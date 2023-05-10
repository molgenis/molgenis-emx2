<template>
  <LayoutModal
    v-if="modalVisible === true"
    title="Edit column metadata"
    :isCloseButtonShown="false"
  >
    <template v-slot:body>
      <div class="row">
        <div class="column-scroll col">
          <Spinner v-if="loading" />
          <div v-else>
            <MessageWarning v-if="column.drop"
              >Marked for deletion
            </MessageWarning>
            <MessageError v-if="error">{{ error }}</MessageError>
            <div class="row">
              <div class="col-4">
                <InputString
                  id="column_name"
                  v-model="column.name"
                  label="columnName"
                  :errorMessage="nameInvalid"
                />
              </div>
              <div class="col-4">
                <InputTextLocalized
                  id="column_label"
                  v-model="column.labels"
                  label="label"
                  :locales="locales"
                />
              </div>
              <div class="col-4">
                <div class="input-group">
                  <InputTextLocalized
                    id="column_description"
                    v-model="column.descriptions"
                    label="description"
                    :locales="locales"
                  />
                </div>
              </div>
            </div>
            <div class="row">
              <div class="col-4">
                <InputSelect
                  id="column_columnType"
                  v-model="column.columnType"
                  :options="columnTypes"
                  label="columnType"
                />
              </div>
              <div
                class="col-4"
                v-if="
                  column.columnType === 'REF' ||
                  column.columnType === 'REF_ARRAY' ||
                  column.columnType === 'REFBACK' ||
                  column.columnType === 'ONTOLOGY' ||
                  column.columnType === 'ONTOLOGY_ARRAY'
                "
              >
                <InputSelect
                  id="column_refTable"
                  v-model="column.refTable"
                  :errorMessage="
                    column.refTable === undefined || column.name === ''
                      ? 'Referenced table is required'
                      : undefined
                  "
                  :options="tableNames"
                  label="refTable"
                />
                <InputSelect
                  id="column_refSchema"
                  v-model="column.refSchema"
                  :options="schemaNames"
                  @update:modelValue="loadRefSchema"
                  label="refSchema"
                  description="When you want to refer to table in another schema"
                />
                <InputString
                  id="column_refLabel"
                  v-model="column.refLabel"
                  label="refLabel"
                  description="(Optional) customize how ref values should be shown. E.g. '${name}' or '${firstName} ${lastName}'"
                />
              </div>
              <div class="col-4" v-if="column.columnType === 'REFBACK'">
                <InputSelect
                  id="column_refBack"
                  label="refBack"
                  v-model="column.refBack"
                  :options="refBackCandidates(column.refTable, table.name)"
                />
              </div>
              <div
                class="col-4"
                v-if="
                  column.refTable &&
                  (column.columnType === 'REF' ||
                    column.columnType === 'REF_ARRAY')
                "
              >
                <InputSelect
                  v-if="refLinkCandidates.length > 0"
                  id="column_refLink"
                  v-model="column.refLink"
                  :options="refLinkCandidates"
                  label="refLink"
                  description="refLink enables to define overlapping references, e.g. 'patientId', 'sampleId' (where sample also overlaps with patientId)"
                />
              </div>
            </div>
            <div class="row">
              <div
                class="col-4"
                v-if="column.columnType !== 'CONSTANT' && !column.computed"
              >
                <InputBoolean
                  id="column_required"
                  v-model="column.required"
                  label="required"
                  description="Will give error unless field is filled in. Is not checked if not visible"
                />
              </div>
              <div
                class="col-4"
                v-if="column.columnType !== 'CONSTANT' && !column.computed"
              >
                <InputBoolean
                  id="column_readonly"
                  v-model="column.readonly"
                  label="isReadonly"
                />
              </div>
            </div>
            <div class="row">
              <div class="col-4" v-if="column.columnType !== 'CONSTANT'">
                <InputSelect
                  id="column_key"
                  v-model="column.key"
                  :options="[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
                  label="key"
                />
              </div>
            </div>
            <div class="row">
              <div class="col">
                <ButtonAction
                  class="float-right"
                  v-if="!previewShow"
                  @click="previewShow = true"
                >
                  show form preview
                </ButtonAction>
              </div>
            </div>
            <div class="row">
              <div class="col-4" v-if="column.columnType !== 'CONSTANT'">
                <InputText
                  id="column_validation"
                  v-model="column.validation"
                  label="validation"
                  description="When javascript expression returns 'false' the expression itself is shown. Example: name === 'John'. When javascript expression returns a string then this string is shown. Example if(name!=='John')'name should be John'. Is not checked if not visible."
                />
              </div>
              <div class="col-4">
                <InputText
                  id="column_visible"
                  v-model="column.visible"
                  label="visible"
                  description="When set only show when javascript expression is !null or !false. Example: other > 5"
                />
              </div>
              <div class="col-4">
                <InputText
                  id="column_computed"
                  v-model="column.computed"
                  label="computed"
                  description="When set only the input will be readonly and value computed using this formula"
                />
              </div>
            </div>
            <div class="row">
              <div class="col-4">
                <InputString
                  id="column_semantics"
                  v-model="column.semantics"
                  :list="true"
                  label="semantics"
                />
              </div>
            </div>

            <div class="row" v-if="subclassNames !== undefined">
              <div class="col">
                <InputSelect
                  :readonly="column.oldName !== undefined"
                  id="column_table"
                  v-model="column.table"
                  :options="subclassNames"
                  :list="true"
                  label="Available in subclass"
                  description="indicate this column is available in particular subclass only. Cannot be changed after creation. We hope to enable this in future version"
                />
              </div>
            </div>
          </div>
        </div>
        <div v-if="previewShow" class="col-4 bg-white column-scroll">
          <h4>
            Form preview
            <ButtonAlt @click="previewShow = false" class="pl-0 pr-0"
              >hide
            </ButtonAlt>
          </h4>
          <RowEdit
            id="form-edit"
            v-model="previewData"
            :schemaMetaData="schema"
            :tableMetaData="table"
            :tableName="table.name"
            :key="JSON.stringify(table)"
          />
          Values:
          {{ previewData }}
        </div>
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="cancel">Cancel</ButtonAlt>
      <ButtonAction @click="apply" :disabled="isDisabled">Apply</ButtonAction>
    </template>
  </LayoutModal>
  <IconAction
    v-else
    class="btn-sm hoverIcon"
    :icon="operation === 'add' ? 'plus' : 'pencil-alt'"
    :tooltip="tooltip"
    @click="showModal"
  />
</template>

<style>
.column-scroll {
  /** want to have columns not heigher than modal allows so we get seperate scroll bars for preview */
  max-height: calc(100vh - 240px);
  overflow-y: auto;
}
</style>

<script>
import {
  LayoutForm,
  InputText,
  InputTextLocalized,
  InputString,
  InputBoolean,
  InputSelect,
  IconAction,
  LayoutModal,
  ButtonAction,
  MessageWarning,
  MessageError,
  ButtonAlt,
  Client,
  Spinner,
  RowEdit,
  deepClone,
} from "molgenis-components";
import columnTypes from "../columnTypes.js";

export default {
  components: {
    LayoutForm,
    InputText,
    InputString,
    InputBoolean,
    InputSelect,
    IconAction,
    InputTextLocalized,
    LayoutModal,
    ButtonAction,
    MessageWarning,
    MessageError,
    ButtonAlt,
    Spinner,
    RowEdit,
  },
  props: {
    /** Column metadata object entered as v-model in case of updates*/
    modelValue: {
      type: Object,
    },
    /** schema  column is part of, used for ref options*/
    tableName: {
      type: String,
    },
    /** schema column is part of, used for ref options*/
    schema: {
      type: Object,
      required: true,
    },
    /** list of schemas for externalSchema select */
    schemaNames: {
      type: Array,
      required: true,
    },
    /** can be set to 'add' */
    operation: {
      type: String,
      default: "edit",
    },
    /** Optional tooltip*/
    tooltip: {
      type: String,
      required: false,
    },
    locales: {
      type: Array,
    },
    columnIndex: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      //if modal is visible
      modalVisible: false,
      // working value of the column (copy of the value)
      column: null,
      //the type options
      columnTypes,
      //in case a refSchema has to be used for the table lookup
      refSchema: undefined,
      error: null,
      client: null,
      loading: false,
      previewShow: false,
      previewData: {},
    };
  },
  computed: {
    //current table object unedited
    originalTable() {
      return this.schema.tables.find(
        (table) =>
          table.name === this.tableName ||
          table.name === this.column.table ||
          (table.subclasses && table.subclasses.includes(this.column.table))
      );
    },
    //current table object edited
    table() {
      const table = deepClone(this.originalTable);
      //replace column with current changes
      const index = table.columns.findIndex(
        (c) => c.name == this.column.name || c.name == this.column.oldName
      );
      // or if new, we add it
      if (index === -1) {
        table.columns.splice(this.columnIndex, 0, this.column);
      } else {
        table.columns[index] = this.column;
      }
      return table;
    },
    //listing of related subclasses, used to indicate if column is part of subclass
    subclassNames() {
      if (this.table?.subclasses) {
        return this.table?.subclasses.map((subclass) => subclass.name);
      } else {
        return undefined;
      }
    },
    //listing of all tables, used for refs
    tableNames() {
      if (this.refSchema !== undefined) {
        if (
          this.column.columnType === "ONTOLOGY" ||
          this.column.columnType === "ONTOLOGY_ARRAY"
        ) {
          return this.refSchema.tables
            .filter((t) => t.tableType === "ONTOLOGIES")
            .map((t) => t.name);
        } else {
          return this.refSchema.tables
            .filter((t) => t.tableType !== "ONTOLOGIES")
            .map((t) => t.name);
        }
      } else {
        if (
          this.column.columnType === "ONTOLOGY" ||
          this.column.columnType === "ONTOLOGY_ARRAY"
        ) {
          return this.schema.ontologies.map((t) => t.name);
        } else {
          return this.schema.tables.map((t) => t.name);
        }
      }
    },
    nameInvalid() {
      if (this.column.name === undefined || this.column.name === "") {
        return "Name is required";
      }
      if (!this.column.name.match(/^[a-zA-Z][a-zA-Z0-9_ ]+$/)) {
        return "Name should start with letter, followed by letter, number, whitespace or underscore ([a-zA-Z][a-zA-Z0-9_ ]*)";
      }
      if (
        (this.modelValue === undefined ||
          this.modelValue.name !== this.column.name) &&
        this.originalTable.columns?.filter((c) => c.name === this.column.name)
          .length > 0
      ) {
        return "Name should be unique";
      } else {
        return undefined;
      }
    },
    isDisabled() {
      return this.operation !== "add" && this.nameInvalid;
    },
  },
  methods: {
    showModal() {
      this.modalVisible = true;
    },
    apply() {
      this.modalVisible = false;
      if (this.operation === "edit") {
        this.$emit("update:modelValue", this.column);
      } else {
        this.$emit("add", this.column);
        this.reset();
      }
    },
    cancel() {
      this.reset();
      this.modalVisible = false;
    },
    refLinkCandidates() {
      return this.table.columns
        .filter(
          (c) =>
            (c.columnType === "REF" || c.columnType === "REF_ARRAY") &&
            c.name !== this.modelValue.name
        )
        .map((c) => c.name);
    },
    refBackCandidates(fromTable, toTable) {
      const schema =
        this.refSchema !== undefined ? this.refSchema : this.schema;

      const columns = schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0];
      return columns?.filter((c) => c.refTable === toTable).map((c) => c.name);
    },
    async loadRefSchema() {
      this.error = undefined;
      this.loading = true;
      if (this.column.refSchema) {
        this.client = Client.newClient(this.column.refSchema, this.$axios);
        const schema = await this.client.fetchSchemaMetaData((error) => {
          this.error = error;
        });
        this.refSchema = schema;
      } else {
        this.refSchema = {};
      }
      this.loading = false;
    },
    reset() {
      //deep copy so it doesn't update during edits
      if (this.modelValue) {
        this.column = deepClone(this.modelValue);
      } else {
        this.column = { table: this.tableName, columnType: "STRING" };
      }
      //if reference to external schema
      if (this.column.refSchema != undefined) {
        this.loadRefSchema();
      }
      this.modalVisible = false;
    },
  },
  created() {
    this.reset();
  },
  emits: ["add", "update:modelValue"],
};
</script>
