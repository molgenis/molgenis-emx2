<template>
  <div>
    <h4
      :id="name"
      style="display: inline-block; text-transform: none !important"
    >
      <!-- todo make updateable in backend, sorry <InputString
      class="ml-1"
      v-model="name"
      :inplace="true"
      :error="validateName()"
    />-->
      {{ name }}
    </h4>
    <InputString class="ml-1" v-model="description" :inplace="true" />
    <br />
    <label>jsonldType:</label>
    <InputString class="ml-1" v-model="jsonldType" :inplace="true" />
    <table class="table table-sm" :key="timestamp">
      <thead class="font-weight-bold">
        <th style="width: 2em">
          <IconAction
            icon="plus"
            @click="createColumn"
            class="btn-sm hoverIcon"
          />
        </th>
        <th style="width: 10em">columnName</th>
        <th style="width: 8em">columnType</th>
        <th style="width: 3em">key</th>
        <th style="width: 5em">required</th>
        <th style="width: 10em">refTable</th>
        <th style="width: 10em" v-if="needsMappedByColumn">mappedBy</th>
        <th style="width: 10em">refLink</th>
        <th style="width: 10em">jsonldType</th>
        <th>description</th>
        <th style="width: 3em"></th>
      </thead>
      <Draggable
        v-model="columns"
        tag="tbody"
        @end="
          timestamp = Date.now();
          applyPosition();
        "
        :key="timestamp"
      >
        <ColumnEditor
          v-for="columnIndex in columns.keys()"
          :key="columnIndex"
          v-model="columns[columnIndex]"
          :schema="schema"
          :columnIndex="columnIndex"
          :tableName="name"
          :needsMappedByColumn="needsMappedByColumn"
        />
      </Draggable>
    </table>
  </div>
</template>

<style>
.hoverIcon {
  visibility: hidden;
}

table:hover .hoverIcon {
  visibility: visible;
}
</style>
<script>
import {
  InputString,
  InputSelect,
  InputBoolean,
  InputText,
  ButtonAlt,
  IconDanger,
  IconAction,
  ShowMore,
} from "@mswertz/emx2-styleguide";
import columnTypes from "../columnTypes";
import ColumnEditor from "./ColumnEditor";
import Draggable from "vuedraggable";

export default {
  components: {
    InputString,
    IconAction,
    InputSelect,
    InputBoolean,
    InputText,
    ButtonAlt,
    IconDanger,
    ShowMore,
    ColumnEditor,
    Draggable,
  },
  props: {
    value: Object,
    schema: Object,
  },
  data() {
    return {
      name: null,
      description: null,
      jsonldType: null,
      columns: [],
      columnTypes,
      timestamp: Date.now(), //used for updating when sorting
    };
  },
  methods: {
    applyPosition() {
      let position = 1;
      this.columns.forEach((c) => (c.position = position++));
      timestamp: Date.now();
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
      let table = {};
      table.name = this.name;
      table.description = this.description;
      table.columns = this.columns;
      console.log("emit(table) " + JSON.stringify(table));
      this.$emit("input", table);
    },
    createColumn() {
      this.columns.push({ columnType: "STRING" });
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
    name() {
      this.emitValue();
    },
    description() {
      this.emitValue();
    },
    columns() {
      this.emitValue();
    },
  },
  created() {
    if (this.value) {
      this.name = this.value.name;
      this.description = this.value.description;
      this.jsonldType = this.value.jsonldType;
      if (this.value.columns) {
        this.columns = this.value.columns;
      }
    }
  },
};
</script>
