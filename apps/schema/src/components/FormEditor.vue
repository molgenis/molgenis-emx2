<template>
  <div>
    <h1>Table metadata editor</h1>
    <Spinner v-if="loading" />
    <div v-else>
      <InputSelect
        v-model="tableName"
        label="Choose table"
        :options="tables"
        :key="tableName"
      />
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <div v-if="tableName">
        <span>
          <ButtonAction @click="saveTable">Save table</ButtonAction>
          <ButtonDanger @click="loadSchema()">Reset</ButtonDanger>
        </span>
        <div class="row">
          <div class="col">
            <div v-if="tableName">
              Use
              <IconAction icon="cog" />
              to change settings for a column. Use
              <IconAction icon="plus" />
              to add a column. Drag and drop columns to change order. NOTE:
              COLUMN NAME CHANGE AND REORDER NOT YET COMPLETELY IMPLEMENTED OR
              TESTED IN BACKEND, ALWAYS CHECK!
              <h2>{{ selectedTable.name }}</h2>
              <p>{{ selectedTable.description }}</p>
              <Draggable :list="selectedTable.columns">
                <div
                  v-for="(column, idx) in selectedTable.columns"
                  :key="JSON.stringify(column)"
                  class="column-hover"
                >
                  <div v-if="column.name != 'mg_tableclass'">
                    <span class="float-right">
                      <IconAction
                        icon="cog"
                        @click="
                          selectedColumn = column;
                          selectedColumnName = selectedColumn.name;

                          if (!selectedColumn.oldName) {
                            selectedColumn.oldName = selectedColumn.name;
                          }
                        "
                      />
                      <IconAction
                        icon="plus"
                        class="mr-2"
                        @click="
                          selectedColumn = { name: 'new column' };
                          selectedColumn.columnType = 'STRING';
                          selectedTable.columns.splice(
                            idx + 1,
                            0,
                            selectedColumn
                          );
                          selectedColumnName = selectedColumn.name;
                        "
                      />
                      <IconAction
                        icon="trash"
                        class="mr-2"
                        @click="
                          column.drop = true;
                          selectedTable.columns.splice(
                            idx + 1,
                            0,
                            selectedColumn
                          );
                        "
                      />
                    </span>
                    <RowFormInput
                      v-if="visible(column.visibleExpression)"
                      v-model="value[column.name]"
                      :label="column.name"
                      :help="column.description"
                      :columnType="column.columnType"
                      :refTable="column.refTable"
                      :nullable="column.nullable"
                      :error="errorPerColumn[column.name]"
                      class="pl-2 pr-2"
                      :class="{
                        'border border-primary':
                          column.name == selectedColumnName,
                      }"
                    />
                    <p v-else>{{ column.name }} is invisible</p>
                  </div>
                </div>
              </Draggable>
            </div>
          </div>
          <div v-if="selectedColumnName" class="col">
            <form>
              <div class="d-flex justify-content-between">
                <h2>column: {{ selectedColumnName }}</h2>
                <IconAction
                  icon="close"
                  @click="selectedColumnName = null"
                  class="float-right"
                />
              </div>
              <p v-if="selectedColumn.inherited">
                Cannot edit this column because it is inherited from table '{{
                  selectedTable.inherit
                }}'
              </p>
              <ColumnEdit
                v-else
                v-model="selectedColumn"
                :tables="tables"
                :table="selectedTable"
                :key="selectedColumnName"
              />
            </form>
          </div>
          <div v-else class="col"></div>
        </div>
      </div>
    </div>
    <ShowMore title="debug">
      selectedColumn: {{ selectedColumn }} <br />
      errorPerColumn: {{ errorPerColumn }} <br />
      selectedTable:
      <pre>{{ JSON.stringify(selectedTable, null, 2) }}</pre>
    </ShowMore>
  </div>
</template>

<style>
.column-hover label:hover {
  cursor: grab;
}
</style>

<script>
import {
  IconAction,
  RowFormInput,
  InputSelect,
  ShowMore,
  MessageSuccess,
  MessageError,
  ButtonAction,
  ButtonDanger,
} from "@mswertz/emx2-styleguide";
import Draggable from "vuedraggable";
import ColumnEditModal from "./ColumnEditModal";
import { request } from "graphql-request";
import ColumnEdit from "./ColumnEdit";

export default {
  components: {
    ColumnEdit,
    IconAction,
    ColumnEditModal,
    RowFormInput,
    InputSelect,
    ShowMore,
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonDanger,
    Draggable,
  },
  data() {
    return {
      tableName: null,
      value: {},
      schema: {},
      selectedColumn: null,
      selectedColumnName: null,
      selectedTable: null,
      error: null,
      success: null,
      loading: false,
      errorPerColumn: {},
    };
  },
  computed: {
    tables() {
      if (!this.schema) {
        return [];
      }
      return this.schema.tables.map((t) => t.name);
    },
  },
  methods: {
    eval(expression) {
      try {
        return eval("(function (row) { " + expression + "})")(this.value); // eslint-disable-line
      } catch (e) {
        return "Script error contact admin: " + e.message;
      }
    },
    visible(expression) {
      if (expression) {
        return this.eval(expression);
      } else {
        return true;
      }
    },
    validate() {
      if (this.selectedTable) {
        this.selectedTable.columns.forEach((column) => {
          // make really empty if empty
          if (/^\s*$/.test(this.value[column.name])) {
            //this.value[column.name] = null;
          }
          delete this.errorPerColumn[column.name];
          // when empty
          if (
            this.value[column.name] == null ||
            (typeof this.value[column.name] === "number" &&
              isNaN(this.value[column.name]))
          ) {
            // when required
            if (column.nullable !== true) {
              this.errorPerColumn[column.name] = column.name + " is required ";
            }
          } else {
            // when not empty
            // when validation
            if (
              typeof this.value[column.name] !== "undefined" &&
              typeof column.validationExpression !== "undefined"
            ) {
              let value = this.value[column.name]; //used for eval, two lines below
              this.errorPerColumn[column.name] = value; //dummy assign
              this.errorPerColumn[column.name] = this.eval(expression);
            }
          }
        });
      }
    },
    saveTable() {
      this.loading = true;
      this.error = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($tables:[MolgenisTableInput]){change(tables:$tables){message}}`,
        {
          tables: this.selectedTable,
        }
      )
        .then((data) => {
          this.success = `Table ${this.selectedTable.name} saved`;
          this.loadSchema();
        })
        .catch((error) => {
          if (error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else {
            this.error = error.response.errors[0].message;
            console.error(JSON.stringify(this.error));
          }
        });
      this.loading = false;
    },
    loadSchema() {
      (this.tableName = null), (this.value = {});
      this.schema = null;
      this.selectedColumn = null;
      this.selectedColumnName = null;
      this.selectedTable = null;
      this.error = null;
      this.loading = true;
      this.errorPerColumn = {};
      request(
        "graphql",
        "{_schema{name,tables{name,inherit,externalSchema,description,jsonldType,columns{name,columnType,columnFormat,inherited,key,refSchema,refTable,refLink,mappedBy,nullable,description,jsonldType,validationExpression,visibleExpression}}}}"
      )
        .then((data) => {
          this.schema = data._schema;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
          if (
            this.error.includes("Field '_schema' in type 'Query' is undefined")
          ) {
            this.error =
              "Schema is unknown or permission denied (might you need to login with authorized user?)";
          }
        })
        .finally((this.loading = false));
    },
  },
  watch: {
    value: {
      deep: true,
      handler() {
        this.validate();
      },
    },
    tableName() {
      if (this.tableName) {
        this.selectedTable = this.schema.tables.find(
          (t) => t.name == this.tableName
        );
        this.selectedColumnName = null;
      }
    },
  },
  created() {
    this.loadSchema();
  },
};
</script>
