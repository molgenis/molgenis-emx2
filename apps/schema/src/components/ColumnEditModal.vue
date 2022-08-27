<template>
  <IconAction
    v-if="!show"
    class="btn-sm hoverIcon"
    icon="pencil-alt"
    @click="show = true"
  />
  <LayoutModal v-else @close="close">
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
          <div class="col-8">
            <InputText
              id="column_description"
              v-model="column.description"
              label="description"
            />
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
              @input="loadRefSchema"
              label="refSchema"
            />
          </div>
          <div
            class="col-4"
            v-if="
              column.columnType === 'REFBACK' &&
              refBackCandidates(column.refTable, table.name).length > 1
            "
          >
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
            <InputString
              id="column_refLink"
              v-model="column.refLink"
              label="refLink"
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
      <ButtonAction @click="close" :disabled="isDisabled">Apply</ButtonAction>
      <ButtonAlt @click="cancel">Cancel</ButtonAlt>
    </template>
  </LayoutModal>
</template>

<script>
import {
  LayoutForm,
  InputText,
  InputString,
  InputBoolean,
  InputSelect,
  IconAction,
  LayoutModal,
  ButtonAction,
  MessageWarning,
  ButtonAlt,
  Client,
  Spinner,
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
    LayoutModal,
    ButtonAction,
    MessageWarning,
    ButtonAlt,
    Spinner,
  },
  props: {
    /** Column metadata object entered as v-model */
    value: {
      type: Object,
      required: true,
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
      return this.schema.tables.filter(
        (table) =>
          table.name === this.column.table ||
          (table.subclasses && table.subclasses.includes(this.column.table))
      )[0];
    },
    //listing of related subclasses, used to indicate if column is part of subclass
    subclassNames() {
      if (this.table.subclasses) {
        return this.table.subclasses.map((subclass) => subclass.name);
      }
      return undefined;
    },
    //listing of all tables, used for refs
    tableNames() {
      if (this.refSchema !== undefined) {
        return this.refSchema.tables.map((t) => t.name);
      } else {
        return this.schema.tables.map((t) => t.name);
      }
    },
    nameInvalid() {
      if (this.column.name === undefined || this.column.name === "") {
        return "Name is required";
      }
      if (!this.column.name.match(/^[a-zA-Z]\w+$/)) {
        return "Name should start with letter, followed by letter, number or underscore ([a-zA-Z][a-zA-Z0-9_]*)";
      }
      if (
        this.value.name !== this.column.name &&
        Array.isArray(this.table.columns) &&
        this.table.columns.filter((c) => c.name === this.column.name).length > 0
      ) {
        return "Name should be unique";
      }
    },
    isDisabled() {
      return this.nameInvalid || this.subclassInvalid;
    },
  },
  methods: {
    close() {
      this.show = false;
      this.$emit("input", this.column);
    },
    cancel() {
      this.column = JSON.parse(JSON.stringify(this.value));
      this.show = false;
    },
    refBackCandidates(fromTable, toTable) {
      const schema =
        this.refSchema !== undefined ? this.refSchema : this.schema;
      return schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    },
    async loadRefSchema() {
      this.error = undefined;
      this.loading = true;
      if (this.column.refSchema !== undefined) {
        this.client = Client.newClient(
          "/" + this.column.refSchema + "/graphql",
          this.$axios
        );
        const schema = await this.client.fetchMetaData((error) => {
          this.error = error;
        });
        this.refSchema = schema;
      } else {
        this.refSchema = {};
      }
      this.loading = false;
    },
  },
  created() {
    //deep copy so it doesn't update during edits
    this.column = JSON.parse(JSON.stringify(this.value));
    //show new columns in editor
    if (this.column.name === undefined) {
      this.show = true;
    }
    //if reference to external schema
    if (this.column.refSchema != undefined) {
      this.loadRefSchema();
    }
  },
};
</script>
