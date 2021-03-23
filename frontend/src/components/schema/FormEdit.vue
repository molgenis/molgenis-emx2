<template>
  <div v-if="table">
    <div class="row">
      <div class="col">
        <h2>Form preview</h2>
        <h1>
          <InputString v-model="table.name" :inplace="true" />
        </h1>
        <IconAction
          class="mr-2"
          icon="plus"
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
                  class="hoverIcon"
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
                  class="mr-2 hoverIcon"
                  icon="plus"
                  @click="
                    selectedColumn = { name: 'new column' };
                    selectedColumn.columnType = 'STRING';
                    table.columns.splice(idx + 1, 0, selectedColumn);
                    selectedColumnName = selectedColumn.name;
                  "
                />
                <IconAction
                  class="mr-2 hoverIcon"
                  icon="trash"
                  @click="
                    column.drop = true;
                    table.columns.splice(idx + 1, 0, selectedColumn);
                  "
                />
              </span>
              <div v-if="visible(column.visibleExpression)">
                <RowFormInput
                  v-model="example[column.name]"
                  class="pl-2 pr-2"
                  :class="{
                    'border border-primary': column.name == selectedColumnName,
                  }"
                  :column-type="column.columnType"
                  :error-message="errorPerColumn[column.name]"
                  :help="column.description"
                  :label="column.name ? column.name : 'please edit name'"
                  :ref-table="column.refTable"
                  :required="column.required"
                />
              </div>
              <p v-else>
                {{ column.name }} is invisible
              </p>
            </div>
          </div>
        </Draggable>
      </div>
      <div v-if="selectedColumnName" class="col">
        <form>
          <div class="d-flex justify-content-between">
            <h2>column settings</h2>
            <IconAction
              class="float-right"
              icon="close"
              @click="selectedColumnName = null"
            />
          </div>
          <p v-if="selectedColumn.inherited">
            Cannot edit this column because it is inherited.
          </p>
          <ColumnEdit
            v-else
            :key="selectedColumnName"
            v-model="selectedColumn"
            :table="table"
            :tables="tables"
          />
        </form>
      </div>
      <div v-else class="col" />
    </div>
    <ShowMore title="debug">
      columns: {{ table }} <br>
      selectedColumn: {{ selectedColumn }} <br>
      errorPerColumn: {{ errorPerColumn }} <br>
    </ShowMore>
  </div>
</template>

<script>
import ColumnEdit from './ColumnEdit.vue'
import Draggable from 'vuedraggable'
import {IconAction, InputString, RowFormInput, ShowMore} from '@/components/ui/index.js'

export default {
  components: {
    ColumnEdit,
    Draggable,
    IconAction,
    InputString,
    RowFormInput,
    ShowMore,
  },
  props: {
    schema: Object,
    /** metadata of a table*/
    value: Object,
  },
  emits: ['input'],
  data() {
    return {
      errorPerColumn: {},
      example: {},
      selectedColumn: null,
      selectedColumnName: null,
      table: {},
    }
  },
  computed: {
    tables() {
      if (!this.schema) {
        return []
      }
      return this.schema.tables.map((t) => t.name)
    },
  },
  watch: {
    example: {
      deep: true,
      handler() {
        this.validate()
      },
    },
    table: {
      deep: true,
      handler() {
        this.validate()
        this.$emit('input', this.table)
      },
    },
    value() {
      this.table = this.value
    },
  },
  created() {
    this.table = this.value
  },
  methods: {
    eval(expression) {
      try {
        return eval("(function (row) { " + expression + "})")(this.example); // eslint-disable-line
      } catch (e) {
        return 'Script graphqlError contact admin: ' + e.message
      }
    },
    validate() {
      this.table.columns.forEach((column) => {
        // make really empty if empty
        if (/^\s*$/.test(this.example[column.name])) {
          // this.value[column.name] = null;
        }
        delete this.errorPerColumn[column.name]
        // when empty
        if (
          this.example[column.name] == null ||
          (typeof this.example[column.name] === 'number' &&
            isNaN(this.example[column.name]))
        ) {
          // when required
          if (column.required) {
            this.errorPerColumn[column.name] = column.name + ' is required '
          }
        } else {
          // when not empty
          // when validation
          if (
            typeof this.example[column.name] !== 'undefined' &&
            typeof column.validationExpression !== 'undefined'
          ) {
            let value = this.example[column.name] // used for eval, two lines below
            this.errorPerColumn[column.name] = value // dummy assign
            this.errorPerColumn[column.name] = this.eval(
              column.validationExpression,
            )
          }
        }
      })
    },
    visible(expression) {
      if (expression) {
        return this.eval(expression)
      } else {
        return true
      }
    },
  },
}
</script>

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
