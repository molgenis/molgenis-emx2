<template>
  <div>
    <InputCheckbox
      id="showAttributes"
      v-model="showAttributes"
      :defaultValue="showAttributes"
      :options="['attributes', 'external']"
    />
    <div v-html="nomnomlSVG" class="bg-white" style="max-width: 100%"></div>
  </div>
</template>

<script>
import { renderSvg } from "nomnoml";
import { InputCheckbox } from "molgenis-components";

export default {
  props: {
    schema: Object,
  },
  components: {
    InputCheckbox,
  },
  data() {
    return {
      imgFullscreen: false,
      showAttributes: [],
    };
  },
  methods: {
    nomnomColumnsForTable(table, tableName) {
      let res = "";
      if (
        Array.isArray(table.columns) &&
        this.showAttributes.includes("attributes")
      ) {
        res += "|";
        table.columns
          .filter((column) => column.table === tableName)
          .forEach((column) => {
            if (column.columnType.includes("REF")) {
              res += `${column.name}: ${column.refTable}`;
            } else {
              res += `${column.name}: ${column.columnType.toLowerCase()}`;
            }
            res += `${column.nullable ? ";" : "*;"}`;
          });
        //remove trailing ;
        res = res.replace(/;\s*$/, "");
      }
      return res;
    },
  },
  computed: {
    tables() {
      return this.schema.tables;
    },
    nomnomlSVG() {
      return renderSvg(this.nomnomSource);
    },
    nomnomSource() {
      if (!this.tables || this.tables.length === 0) return "";
      let res = "#.box: fill=white solid\n#stroke: #007bff\n";
      // classes
      this.tables
        .filter(
          (t) => !t.externalSchema || this.showAttributes.includes("external")
        )
        .forEach((table) => {
          res += `[<box> ${table.name}`;
          res += this.nomnomColumnsForTable(table, table.name);
          res += "]\n";
          if (table.subclasses !== undefined) {
            table.subclasses.forEach((subclass) => {
              res += `[<box> ${subclass.name}`;
              res += this.nomnomColumnsForTable(table, subclass.name);
              res += "]\n";
              res += `[<box>${subclass.inherit}]<:-[<box>${subclass.name}]\n`;
            });
          }
        });

      // relations
      this.tables
        .filter(
          (t) =>
            t.externalSchema === undefined ||
            this.showAttributes.includes("external")
        )
        .forEach((table) => {
          if (Array.isArray(table.columns)) {
            table.columns
              .filter(
                (c) =>
                  !c.inherited &&
                  (c.refSchema === undefined ||
                    this.showAttributes.includes("external"))
              )
              .forEach((column) => {
                if (column.columnType === "REF") {
                  res += `[<box>${column.refTable}]<-${column.name}[<box>${table.name}]\n`;
                } else if (column.columnType === "REF_ARRAY") {
                  res += `[<box>${column.refTable}]*<-${column.name}[<box>${table.name}]\n`;
                }
              });
          }
        });

      return res;
    },
  },
};
</script>
