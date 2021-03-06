<template>
  <div :key="timestamp" class="tableContainer">
    <h4
      :id="table.name"
      style="display: inline-block; text-transform: none !important"
      :style="table.drop ? 'text-decoration: line-through' : ''"
    >
      <InputString v-model="table.name" inplace="true" />
    </h4>
    <IconDanger icon="trash" @click="deleteTable" class="btn-sm hoverIcon" />
    <ButtonAction @click="formedit = true" class="hoverIcon float-right">
      Open form editor
    </ButtonAction>
    <div v-if="!table.drop">
      <label>Inherits: </label>
      <InputString v-model="table.inherit" inplace="true" />
      <br />
      <label>Description: </label>
      <InputString class="ml-1" v-model="table.description" :inplace="true" />
      <br />
      <label>jsonldType:</label>
      <InputString class="ml-1" v-model="table.jsonldType" :inplace="true" />
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
          <th scope="col" style="width: 10em" v-if="needsMappedByColumn">
            mappedBy
          </th>
          <th scope="col" style="width: 10em">refLink</th>
          <th scope="col" style="width: 10em">jsonldType</th>
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
            v-for="columnIndex in table.columns.keys()"
            :key="columnIndex"
            v-model="table.columns[columnIndex]"
            :schema="schema"
            :columnIndex="columnIndex"
            :needsMappedByColumn="needsMappedByColumn"
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
        <template v-slot:body>
          <FormEdit :schema="schema" v-model="table" />
        </template>
      </LayoutModal>
    </div>
  </div>
</template>

<style>
.hoverIcon {
  visibility: hidden;
}

.tableContainer:hover .hoverIcon {
  visibility: visible;
}
</style>
<script>
import {
  InputString,
  InputSelect,
  InputBoolean,
  ButtonAction,
  InputText,
  ButtonAlt,
  IconDanger,
  IconAction,
  ShowMore,
  LayoutModal,
} from "@mswertz/emx2-styleguide";
import columnTypes from "../columnTypes";
import ColumnEditor from "./ColumnEditor";
import Draggable from "vuedraggable";
import FormEdit from "./FormEdit";

export default {
  components: {
    FormEdit,
    InputString,
    ButtonAction,
    IconAction,
    InputSelect,
    InputBoolean,
    InputText,
    ButtonAlt,
    IconDanger,
    ShowMore,
    ColumnEditor,
    LayoutModal,
    Draggable,
  },
  props: {
    value: Object,
    schema: Object,
  },
  data() {
    return {
      table: {},
      formedit: false,
      columnTypes,
      timestamp: Date.now(), //used for updating when sorting
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
        return "Table name is required";
      }
      if (this.schema.tables.filter((t) => t.name == this.name).length > 1) {
        return "Table name must be unique within schema";
      }
    },
    emitValue() {
      console.log("emit(table) " + JSON.stringify(table));
      this.$emit("input", this.table);
    },
    createColumn() {
      this.table.columns.push({
        name: "NewColumn",
        columnType: "STRING",
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
    refbackCandidates(fromTable, toTable) {
      return this.schema.tables
        .filter((t) => t.name === fromTable)
        .map((t) => t.columns)[0]
        .filter((c) => c.refTable === toTable)
        .map((c) => c.name);
    },
  },
  computed: {
    tables() {
      if (this.schema.tables) {
        return this.schema.tables.map((t) => t.name);
      }
      return [];
    },
    needsMappedByColumn() {
      if (this.schema && this.schema.tables) {
        return (
          this.schema.tables.filter(
            //each table
            (t) =>
              t.columns &&
              t.columns.filter(
                //has refback column with ambigious mapped by
                (c) =>
                  c.columnType === "REFBACK" &&
                  this.refbackCandidates(c.refTable, t.name).length > 1
              ).length > 0
          ).length > 0
        );
      }
    },
  },
  watch: {
    table: {
      deep: true,
      handler() {
        this.emitValue();
      },
    },
    value() {
      if (this.value) {
        this.table = this.value;
      }
    },
  },
  created() {
    if (this.value) {
      this.table = this.value;
    }
  },
};
</script>
