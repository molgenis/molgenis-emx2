<template>
  <LayoutModal
    v-if="show"
    title="Edit column metadata"
    @close="cancel"
    :isCloseButtonShown="!isDisabled"
  >
    <template v-slot:body>
      <Spinner v-if="loading" />
      <LayoutForm v-else>
        <MessageWarning v-if="column.drop">Marked for deletion</MessageWarning>
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
              (column.columnType === 'REF' || column.columnType === 'REF_ARRAY')
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
          <div class="col-4" v-if="column.columnType !== 'CONSTANT'">
            <InputBoolean
              id="column_required"
              v-model="column.required"
              :label="column.visibleIf ? 'required (if visible)' : 'required'"
            />
          </div>
          <div class="col-4" v-if="column.columnType !== 'CONSTANT'">
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
          <div class="col-4" v-if="column.columnType !== 'CONSTANT'">
            <InputText
              id="column_validation"
              v-model="column.validation"
              label="validationExpression"
              description="Example: {name} == 'John'"
            />
          </div>
          <div class="col-4">
            <InputText
              id="column_visible"
              v-model="column.visible"
              label="visibleIf"
              description="{other} > 5"
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
      </LayoutForm>
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
    @click="click"
    :tooltip="tooltip"
  />
</template>

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
    /** schema  column is part of, used for ref options*/
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
  },
  data() {
    return {
      //show
      show: false,
      // working value of the column (copy of the value)
      column: null,
      //the type options
      columnTypes,
      //in case a refSchema has to be used for the table lookup
      refSchema: undefined,
      error: null,
      client: null,
      loading: false,
    };
  },
  computed: {
    //current table object
    table() {
      return this.schema.tables.find(
        (table) =>
          table.name === this.tableName ||
          table.name === this.column.table ||
          (table.subclasses && table.subclasses.includes(this.column.table))
      );
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
        this.table.columns?.filter((c) => c.name === this.column.name).length >
          0
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
    click() {
      this.show = true;
    },
    apply() {
      this.show = false;
      if (this.operation === "edit") {
        this.$emit("update:modelValue", this.column);
      } else {
        this.$emit("add", this.column);
        this.reset();
      }
    },
    cancel() {
      this.show = false;
      this.reset();
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
      if (this.column.refSchema !== undefined) {
        this.client = Client.newClient(
          "/" + this.column.refSchema + "/graphql",
          this.$axios
        );
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
      this.show = false;
    },
  },
  created() {
    this.reset();
  },
  emits: ["add", "update:modelValue"],
};
</script>
