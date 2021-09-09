<template>
  <div v-if="table">
    <div class="row">
      <div class="col">
        <h2>Preview</h2>
        <form>
          <Draggable :list="table.columns" handle="column-hover">
            <div
              v-for="(column, idx) in table.columns"
              :key="ide + changetime"
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
                    icon="trash"
                    class="mr-2 hoverIcon"
                    @click="
                      column.drop = true;
                      table.columns = table.columns.filter(
                        (c) => c.name != column.name
                      );
                    "
                  />
                </span>
                <div v-if="visible(column.visible)">
                  <RowFormInput
                    v-model="example[column.name]"
                    :label="column.name ? column.name : 'please edit name'"
                    :description="column.description"
                    :columnType="column.columnType"
                    :refTable="column.refTable"
                    :required="column.required"
                    :errorMessage="errorPerColumn[column.name]"
                    class="pl-2 pr-2"
                  />
                </div>
                <p v-else>{{ column.name }} is invisible</p>
              </div>
            </div>
          </Draggable>
        </form>
        <IconAction
          icon="plus"
          class="mr-2"
          @click="
            selectedColumn = { name: 'new' + Math.round(Math.random() * 100) };
            selectedColumn.columnType = 'STRING';
            table.columns.push(selectedColumn);
            selectedColumnName = selectedColumn.name;
          "
        />
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
            @update="changed"
          />
        </form>
      </div>
      <div v-else class="col"></div>
    </div>
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
      changetime: Date.now(),
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
    changed() {
      this.changetime = Date.now();
    },
    eval(expression) {
      try {
        return eval("(function (row) { " + expression + "})")(this.example); // eslint-disable-line
      } catch (e) {
        return "Error in validation script: " + e.message;
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
            typeof column.validation !== "undefined"
          ) {
            let value = this.example[column.name]; //used for eval, two lines below
            this.errorPerColumn[column.name] = value; //dummy assign
            this.errorPerColumn[column.name] = this.eval(column.validation);
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
