<template>
  <div v-if="schema.tables">
    <ButtonAlt @click="imgFullscreen = !imgFullscreen"
      >{{ imgFullscreen ? "show small size" : "show full size" }}
    </ButtonAlt>
    <InputCheckbox
      v-model="showAttributes"
      :defaultValue="showAttributes"
      :options="['attributes', 'external']"
    />
    <div
      v-scroll-lock="imgFullscreen"
      :style="{
        height: imgFullscreen ? 'auto' : '300px',
      }"
      style="text-align: center; overflow: auto"
    >
      <Spinner v-if="loadingYuml" />
      <img
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
import {
  Spinner,
  InputCheckbox,
  IconAction,
  ButtonAlt,
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    Spinner,
    InputCheckbox,
    IconAction,
    ButtonAlt,
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
      this.loadingYuml = true;
      if (!this.tables) return "";
      let res = "http://yuml.me/diagram/plain;dir:bt/class/";
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
