<template>
  <div>
    <a href="#" @click.prevent="createTable"> create table </a>
    <div v-if="tables.length > 0">
      <TableEditor
        v-for="tableIndex in tables.keys()"
        v-model="tables[tableIndex]"
        :schema="schema"
      />
    </div>
    <ShowMore title="debug">
      {{ timestamp }}
      <pre>{{ schema }}</pre>
    </ShowMore>
  </div>
</template>

<script>
import {
  InputString,
  InputSelect,
  InputBoolean,
  InputText,
  ButtonAlt,
  IconDanger,
  ShowMore,
} from "@mswertz/emx2-styleguide";
import columnTypes from "../columnTypes";
import Draggable from "vuedraggable";
import TableEditor from "./TableEditor";

export default {
  components: {
    InputString,
    InputSelect,
    InputBoolean,
    InputText,
    ButtonAlt,
    IconDanger,
    ShowMore,
    TableEditor,
    Draggable,
  },
  props: {
    value: Object,
  },
  data() {
    return {
      schema: {},
      tables: [],
      columnTypes,
      timestamp: Date.now(), //used for updating when sorting
    };
  },
  methods: {
    createTable() {
      if (!this.schema.tables) {
        this.schema.tables = [];
      }
      this.schema.tables.push({ name: "test" });
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
  watch: {
    schema: {
      deep: true,
      handler() {
        console.log("emit(schema): " + JSON.stringify(this.schema));
        this.$emit("input", this.schema);
      },
    },
    tables: {
      deep: true,
      handler() {
        this.schema.tables = this.tables;
      },
    },
    value() {
      this.schema = this.value;
      if (this.value.tables) {
        this.tables = this.value.tables;
      }
    },
  },
  created() {
    this.schema = this.value;
    if (this.value.tables) {
      this.tables = this.value.tables;
    }
  },
};
</script>
