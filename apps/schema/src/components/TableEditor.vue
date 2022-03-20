<template>
  <div>
    <div :key="timestamp">
      <div class="hoverContainer">
        <h4
          :id="table.name"
          style="display: inline-block; text-transform: none !important"
          :style="table.drop ? 'text-decoration: line-through' : ''"
        >
          <InputString v-model="table.name" :inplace="true" />
        </h4>
        <IconDanger
          icon="trash"
          @click="deleteTable"
          class="btn-sm hoverIcon"
        />
        <ButtonAction @click="formedit = true" class="hoverIcon float-right">
          Open form editor
        </ButtonAction>
        <div v-if="!table.drop">
          <label>Inherits: </label>
          <InputString v-model="table.inherit" :inplace="true" />
          <br />
          <label>Description: </label>
          <InputString
            class="ml-1"
            v-model="table.description"
            :inplace="true"
          />
          <br />
          <label>Semantics:</label>
          <InputString
            class="ml-1"
            v-model="table.semantics"
            :list="true"
            :inplace="true"
          />
        </div>

        <table class="table table-sm" :key="timestamp">
          <thead class="font-weight-bold">
            <th style="width: 2em">
              <IconAction
                icon="plus"
                @click="createColumn"
                class="btn-sm hoverIcon"
              />
            </th>
            <th scope="col" style="width: 10em">columnName</th>
            <th scope="col" style="width: 8em">columnType</th>
            <th scope="col" style="width: 3em">key</th>
            <th scope="col" style="width: 5em">required</th>
            <th scope="col" style="width: 10em">refTable</th>
            <th scope="col" style="width: 10em">refLink</th>
            <th scope="col" style="width: 10em">refBack</th>
            <th scope="col" style="width: 10em">semantics</th>
            <th scope="col" style="width: 10em">validation expression</th>
            <th scope="col" style="width: 10em">visibility expression</th>
            <th scope="col">description</th>
            <th scope="col" style="width: 3em"></th>
          </thead>
          <Draggable
            v-model="table.columns"
            tag="tbody"
            @end="
              timestamp = Date.now();
              applyPosition();
            "
            :key="timestamp"
          >
            <ColumnEditor
              class="hoverContainer"
              v-for="columnIndex in table.columns.keys()"
              :key="columnIndex"
              v-model="table.columns[columnIndex]"
              :schema="schema"
              :columnIndex="columnIndex"
            />
          </Draggable>
        </table>
      </div>
    </div>
    <LayoutModal
      :show="formedit"
      title="Form editor"
      @close="
        formedit = false;
        timestamp = Date.now();
      "
    >
      <template v-slot:body>
        <FormEdit :schema="schema" v-model="table" />
      </template>
    </LayoutModal>
  </div>
</template>

<style>
.hoverIcon {
  visibility: hidden;
}

.hoverContainer:hover .hoverIcon {
  visibility: visible;
}
</style>
<script>
import {
  InputString,
  ButtonAction,
  IconDanger,
  IconAction,
  LayoutModal
} from '@mswertz/emx2-styleguide';
import columnTypes from '../columnTypes';
import ColumnEditor from './ColumnEditor';
import Draggable from 'vuedraggable';
import FormEdit from './FormEdit';

export default {
  components: {
    FormEdit,
    InputString,
    ButtonAction,
    IconAction,
    IconDanger,
    ColumnEditor,
    LayoutModal,
    Draggable
  },
  props: {
    value: Object,
    schema: Object
  },
  data() {
    return {
      table: {},
      formedit: false,
      columnTypes,
      timestamp: Date.now() //used for updating when sorting
    };
  },
  methods: {
    applyPosition() {
      let position = 1;
      this.columns.forEach((c) => (c.position = position++));
      this.timestamp = Date.now();
    },
    validateName() {
      if (!this.name) {
        return 'Table name is required';
      }
      if (this.schema.tables.filter((t) => t.name == this.name).length > 1) {
        return 'Table name must be unique within schema';
      }
    },
    emitValue() {
      this.$emit('input', this.table);
    },
    createColumn() {
      this.table.columns.push({
        name: 'NewColumn',
        columnType: 'STRING'
      });
      this.timestamp = Date.now();
    },
    deleteTable() {
      if (this.table.drop) {
        delete this.table.drop;
      } else {
        this.table.drop = true;
      }
      this.timestamp = Date.now();
    },
    refBackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    }
  },
  computed: {
    tables() {
      if (this.schema.tables) {
        return this.schema.tables.map((t) => t.name);
      }
      return [];
    }
  },
  watch: {
    table: {
      deep: true,
      handler() {
        this.emitValue();
      }
    },
    value() {
      if (this.value) {
        this.table = this.value;
      }
    }
  },
  created() {
    if (this.value) {
      this.table = this.value;
    }
  }
};
</script>
