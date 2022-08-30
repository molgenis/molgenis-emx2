<template>
  <div>
    <InputCheckbox
      id="displaySettings"
      v-model="displaySettings"
      :defaultValue="displaySettings"
      :options="['attributes', 'external', 'vertical', 'ontologies']"
    />
    <div v-html="nomnomlSVG" class="bg-white" style="max-width: 100%"></div>
  </div>
</template>

<script>
import { renderSvg } from "nomnoml";
import { InputCheckbox } from "molgenis-components";

export default {
  props: {
    schema: {
      type: Object,
      required: true,
    },
  },
  components: {
    InputCheckbox,
  },
  data() {
    return {
      imgFullscreen: false,
      displaySettings: [],
    };
  },
  methods: {
    nomnomColumnsForTable(table, tableName) {
      let result = "";
      if (
        Array.isArray(table.columns) &&
        this.displaySettings.includes("attributes")
      ) {
        result += "|";
        table.columns
          .filter((column) => column.table === tableName)
          .forEach((column) => {
            if (
              column.columnType.includes("REF") ||
              column.columnType.includes("ONTOLOGY")
            ) {
              result += `${column.name}: ${column.columnType.toLowerCase()}(${
                column.refTable
              })`;
            } else {
              result += `${column.name}: ${column.columnType.toLowerCase()}`;
            }
            result += `${column.nullable ? ";" : "*;"}`;
          });
        //remove trailing ;
        result = result.replace(/;\s*$/, "");
      }
      return result;
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
      let res = `#.box: fill=white solid\n#stroke: #007bff\n#direction: ${
        this.displaySettings.includes("vertical") ? "right" : "down"
      }\n`;
      // classes
      this.tables
        .filter(
          (t) => !t.externalSchema || this.displaySettings.includes("external")
        )
        .forEach((table) => {
          res += `[${table.externalSchema ? "<external>" : "<box>"} ${
            table.name
          }`;
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
            this.displaySettings.includes("external")
        )
        .forEach((table) => {
          if (Array.isArray(table.columns)) {
            table.columns
              .filter(
                (c) =>
                  !c.inherited &&
                  (c.refSchema === undefined ||
                    this.displaySettings.includes("external"))
              )
              .forEach((column) => {
                if (
                  column.columnType === "REF" ||
                  (column.columnType === "ONTOLOGY" &&
                    this.displaySettings.includes("ontologies"))
                ) {
                  res += `[<box>${column.refTable}]<-${column.name}[<box>${table.name}]\n`;
                } else if (
                  column.columnType === "REF_ARRAY" ||
                  (column.columnType === "ONTOLOGY_ARRAY" &&
                    this.displaySettings.includes("ontologies"))
                ) {
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
