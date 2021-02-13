<template>
  <div v-if="table">
    <div class="row">
      <div class="col">
        <h2>Form preview</h2>
        <h1>
          <InputString v-model="table.name" :inplace="true"></InputString>
        </h1>
        <IconAction
          icon="plus"
          class="mr-2"
          @click="
            selectedColumn = { name: 'new column' };
            selectedColumn.columnType = 'STRING';
            table.columns.splice(idx + 1, 0, selectedColumn);
            selectedColumnName = selectedColumn.name;
          "
        />
        <Draggable :list="table.columns">
          <div
            v-for="(column, idx) in table.columns"
            :key="JSON.stringify(column)"
            class="column-hover"
          >
            <div v-if="column.name != 'mg_tableclass'">
              <span class="float-right">
                <IconAction
                  icon="cog"
                  class="hoverIcon"
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
                  class="mr-2 hoverIcon"
                  @click="
                    selectedColumn = { name: 'new column' };
                    selectedColumn.columnType = 'STRING';
                    table.columns.splice(idx + 1, 0, selectedColumn);
                    selectedColumnName = selectedColumn.name;
                  "
                />
                <IconAction
                  icon="trash"
                  class="mr-2 hoverIcon"
                  @click="
                    column.drop = true;
                    table.columns.splice(idx + 1, 0, selectedColumn);
                  "
                />
              </span>
              <div v-if="visible(column.visibleExpression)">
                <RowFormInput
                  v-model="example[column.name]"
                  :label="column.name ? column.name : 'please edit name'"
                  :help="column.description"
                  :columnType="column.columnType"
                  :refTable="column.refTable"
                  :required="column.required"
                  :error="errorPerColumn[column.name]"
                  class="pl-2 pr-2"
                  :class="{
                    'border border-primary': column.name == selectedColumnName,
                  }"
                />
              </div>
              <p v-else>{{ column.name }} is invisible</p>
            </div>
          </div>
        </Draggable>
      </div>
      <div v-if="selectedColumnName" class="col">
        <form>
          <div class="d-flex justify-content-between">
            <h2>column settings</h2>
            <IconAction
              icon="close"
              @click="selectedColumnName = null"
              class="float-right"
            />
          </div>
          <p v-if="selectedColumn.inherited">
            Cannot edit this column because it is inherited.
          </p>
          <ColumnEdit
            v-else
            v-model="selectedColumn"
            :table="table"
            :tables="tables"
            :key="selectedColumnName"
          />
        </form>
      </div>
      <div v-else class="col"></div>
    </div>
    <ShowMore title="debug">
      columns: {{ table }} <br />
      selectedColumn: {{ selectedColumn }} <br />
      errorPerColumn: {{ errorPerColumn }} <br />
    </ShowMore>
  </div>
</template>

<style>
.column-hover label:hover {
  cursor: grab;
}

.column-hover .hoverIcon {
  visibility: hidden;
}

.column-hover:hover .hoverIcon {
  visibility: visible;
}
</style>

<script>
import {
  IconAction,
  RowFormInput,
  InputSelect,
  ShowMore,
  InputString,
  MessageSuccess,
  MessageError,
  ButtonAction,
  ButtonDanger,
} from "@mswertz/emx2-styleguide";
import Draggable from "vuedraggable";
import ColumnEditModal from "./ColumnEditModal";
import ColumnEdit from "./ColumnEdit";

export default {
  components: {
    ColumnEdit,
    IconAction,
    InputString,
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
  props: {
    schema: Object,
    /** metadata of a table*/
    value: Object,
  },
  data() {
    return {
      table: {},
      example: {},
      selectedColumn: null,
      selectedColumnName: null,
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
        return eval("(function (row) { " + expression + "})")(this.example); // eslint-disable-line
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
      this.table.columns.forEach((column) => {
        // make really empty if empty
        if (/^\s*$/.test(this.example[column.name])) {
          //this.value[column.name] = null;
        }
        delete this.errorPerColumn[column.name];
        // when empty
        if (
          this.example[column.name] == null ||
          (typeof this.example[column.name] === "number" &&
            isNaN(this.example[column.name]))
        ) {
          // when required
          if (column.required) {
            this.errorPerColumn[column.name] = column.name + " is required ";
          }
        } else {
          // when not empty
          // when validation
          if (
            typeof this.example[column.name] !== "undefined" &&
            typeof column.validationExpression !== "undefined"
          ) {
            let value = this.example[column.name]; //used for eval, two lines below
            this.errorPerColumn[column.name] = value; //dummy assign
            this.errorPerColumn[column.name] = this.eval(
              column.validationExpression
            );
          }
        }
      });
    },
  },
  watch: {
    example: {
      deep: true,
      handler() {
        this.validate();
      },
    },
    table: {
      deep: true,
      handler() {
        this.validate();
        this.$emit("input", this.table);
      },
    },
    value() {
      this.table = this.value;
    },
  },
  created() {
    this.table = this.value;
  },
};
</script>
