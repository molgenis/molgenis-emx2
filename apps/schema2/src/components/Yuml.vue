<template>
  <div v-if="schema.tables">
    <InputCheckbox
      v-model="showAttributes"
      :defaultValue="showAttributes"
      :options="['attributes', 'external']"
    />
    <div style="text-align: center" class="overflow-auto">
      <Spinner v-if="loadingYuml" />
      <img
        :class="imgFullscreen ? '' : 'img-fluid'"
        @click="imgFullscreen = !imgFullscreen"
        v-else
        :key="JSON.stringify(showAttributes)"
        :src="yuml"
        alt="Small"
        style="max-height: 100%"
        @load="loadingYuml = false"
      />
    </div>
  </div>
</template>

<script>
import { Spinner, InputCheckbox } from "molgenis-components";

export default {
  components: {
    Spinner,
    InputCheckbox,
  },
  props: {
    schema: Object,
  },
  data() {
    return {
      loadingYuml: false,
      imgFullscreen: false,
      showAttributes: [],
    };
  },
  computed: {
    tables() {
      return this.schema.tables;
    },
    yuml() {
      if (!this.tables || this.tables.length === 0) return "";
      this.loadingYuml = true;
      let res = "https://yuml.me/diagram/plain;dir:bt/class/";
      // classes
      this.tables
        .filter(
          (t) => !t.externalSchema || this.showAttributes.includes("external")
        )
        .forEach((table) => {
          res += `[${table.name}`;

          if (
            Array.isArray(table.columns) &&
            this.showAttributes.includes("attributes")
          ) {
            res += "|";
            table.columns
              .filter((column) => !column.inherited)
              .forEach((column) => {
                if (column.columnType.includes("REF")) {
                  res += `${column.name}:${column.refTable}`;
                } else {
                  res += `${column.name}:${column.columnType}`;
                }
                res += `［${column.nullable ? "0" : "1"}..${
                  column.columnType.includes("ARRAY") ? "*" : "1"
                }］;`; //notice I use not standard [] to not break yuml
              });
          }
          if (table.externalSchema) {
            res += `],`;
          } else {
            res += `{bg:dodgerblue}],`;
          }
        });

      // relations
      this.tables
        .filter(
          (t) =>
            t.externalSchema == undefined ||
            this.showAttributes.includes("external")
        )
        .forEach((table) => {
          if (table.inherit) {
            res += `[${table.inherit}]^-[${table.name}],`;
          }
          if (Array.isArray(table.columns)) {
            table.columns
              .filter(
                (c) =>
                  !c.inherited &&
                  (c.refSchema == undefined ||
                    this.showAttributes.includes("external"))
              )
              .forEach((column) => {
                if (column.columnType === "REF") {
                  res += `[${table.name}]${column.name}->[${column.refTable}],`;
                } else if (column.columnType === "REF_ARRAY") {
                  res += `[${table.name}]${column.name}-*>[${column.refTable}],`;
                }
              });
          }
        });

      return res;
    },
  },
};
</script>
