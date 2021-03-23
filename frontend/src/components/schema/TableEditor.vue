<template>
  <div :key="timestamp" class="tableContainer">
    <h4
      :id="table.name"
      style="display: inline-block; text-transform: none !important;"
      :style="table.drop ? 'text-decoration: line-through' : ''"
    >
      <InputString v-model="table.name" inplace="true" />
    </h4>
    <IconDanger class="btn-sm hoverIcon" icon="trash" @click="deleteTable" />
    <ButtonAction class="hoverIcon float-right" @click="formedit = true">
      Open form editor
    </ButtonAction>
    <div v-if="!table.drop">
      <label>Inherits: </label>
      <InputString v-model="table.inherit" inplace="true" />
      <br>
      <label>Description: </label>
      <InputString v-model="table.description" class="ml-1" :inplace="true" />
      <br>
      <label>semantics:</label>
      <InputString
        v-model="table.semantics"
        class="ml-1"
        :inplace="true"
        :list="true"
      />
      <table :key="timestamp" class="table table-sm">
        <thead class="font-weight-bold">
          <th style="width: 2em;">
            <IconAction
              class="btn-sm hoverIcon"
              icon="plus"
              @click="createColumn"
            />
          </th>
          <th scope="col" style="width: 10em;">
            columnName
          </th>
          <th scope="col" style="width: 8em;">
            columnType
          </th>
          <th scope="col" style="width: 3em;">
            key
          </th>
          <th scope="col" style="width: 5em;">
            required
          </th>
          <th scope="col" style="width: 10em;">
            refTable
          </th>
          <th v-if="needsMappedByColumn" scope="col" style="width: 10em;">
            mappedBy
          </th>
          <th scope="col" style="width: 10em;">
            refLink
          </th>
          <th scope="col" style="width: 10em;">
            semantics
          </th>
          <th scope="col">
            description
          </th>
          <th scope="col" style="width: 3em;" />
        </thead>
        <Draggable
          :key="timestamp"
          v-model="table.columns"
          tag="tbody"
          @end="
            timestamp = Date.now();
            applyPosition();
          "
        >
          <ColumnEditor
            v-for="columnIndex in table.columns.keys()"
            :key="columnIndex"
            v-model="table.columns[columnIndex]"
            :column-index="columnIndex"
            :needs-mapped-by-column="needsMappedByColumn"
            :schema="schema"
          />
        </Draggable>
      </table>
      <LayoutModal
        :show="formedit"
        title="Form editor"
        @close="
          formedit = false;
          timestamp = Date.now();
        "
      >
        <template #body>
          <FormEdit v-model="table" :schema="schema" />
        </template>
      </LayoutModal>
    </div>
  </div>
</template>

<script>
import ColumnEditor from './ColumnEditor.vue'
import columnTypes from '../columnTypes.vue'
import Draggable from 'vuedraggable'
import FormEdit from './FormEdit'
import {ButtonAction, IconAction, IconDanger, InputString, LayoutModal} from '@//components/ui/index.js'

export default {
  components: {
    ButtonAction,
    ColumnEditor,
    Draggable,
    FormEdit,
    IconAction,
    IconDanger,
    InputString,
    LayoutModal,
  },
  props: {
    schema: Object,
    value: Object,
  },
  emits: ['input'],
  data() {
    return {
      columnTypes,
      formedit: false,
      table: {},
      timestamp: Date.now(), // used for updating when sorting
    }
  },
  computed: {
    // eslint-disable-next-line vue/return-in-computed-property
    needsMappedByColumn() {
      if (this.schema && this.schema.tables) {
        return (
          this.schema.tables.filter(
            // each table
            (t) =>
              t.columns &&
              t.columns.filter(
                // has refback column with ambigious mapped by
                (c) =>
                  c.columnType === 'REFBACK' &&
                  this.refbackCandidates(c.refTable, t.name).length > 1,
              ).length > 0,
          ).length > 0
        )
      }
    },
    tables() {
      if (this.schema.tables) {
        return this.schema.tables.map((t) => t.name)
      }
      return []
    },

  },
  watch: {
    table: {
      deep: true,
      handler() {
        this.emitValue()
      },
    },
    value() {
      if (this.value) {
        this.table = this.value
      }
    },
  },
  created() {
    if (this.value) {
      this.table = this.value
    }
  },
  methods: {
    applyPosition() {
      let position = 1
      this.columns.forEach((c) => (c.position = position++))
      this.timestamp = Date.now()
    },
    createColumn() {
      this.table.columns.push({
        columnType: 'STRING',
        name: 'NewColumn',
      })
      this.timestamp = Date.now()
    },
    deleteTable() {
      if (this.table.drop) {
        delete this.table.drop
      } else {
        this.table.drop = true
      }
      this.timestamp = Date.now()
    },
    emitValue() {
      this.$emit('input', this.table)
    },
    refbackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name)
    },
    validateName() {
      if (!this.name) {
        return 'Table name is required'
      }
      if (this.schema.tables.filter((t) => t.name == this.name).length > 1) {
        return 'Table name must be unique within schema'
      }
    },
  },
}
</script>

<style>
.hoverIcon {
  visibility: hidden;
}

.tableContainer:hover .hoverIcon {
  visibility: visible;
}
</style>
