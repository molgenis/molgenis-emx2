<template>
  <div :key="timestamp">
    <div v-if="schema.tables && schema.tables.length > 0">
      <TableEditor
        v-for="tableIndex in schema.tables.keys()"
        v-model="schema.tables[tableIndex]"
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
      columnTypes,
      timestamp: Date.now(), //used for updating when sorting
    };
  },
  methods: {
    refBackCandidates(fromTable, toTable) {
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
        this.$emit("input", this.schema);
      },
    },
    value() {
      this.schema = this.value;
    },
  },
  created() {
    this.schema = this.value;
  },
};
</script>
