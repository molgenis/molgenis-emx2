<template>
  <div>
    <InputCheckbox
      id="displaySettings"
      v-model="displaySettings"
      :defaultValue="displaySettings"
      :options="['attributes', 'vertical', 'ontologies']"
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
                column.refTableName
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
    ontologies() {
      return this.schema.ontologies;
    },
    nomnomlSVG() {
      return renderSvg(this.nomnomSource);
    },
    nomnomSource() {
      if (!this.tables || this.tables.length === 0) return "";
      let res = `
#.table: fill=white solid
#.external: fill=white stroke=grey text=italic solid
#.ontology: fill=white solid visual=ellipse
#.externalo: fill=white stroke=grey solid visual=ellipse
#stroke: #007bff
#direction: ${this.displaySettings.includes("vertical") ? "right" : "down"}\n`;
      // classes
      if (this.tables) {
        this.tables.forEach((table) => {
          res += `[<table> ${table.name}`;
          res += this.nomnomColumnsForTable(table, table.name);
          res += "]\n";
          if (table.subclasses !== undefined) {
            table.subclasses.forEach((subclass) => {
              res += `[<table> ${subclass.name}`;
              res += this.nomnomColumnsForTable(table, subclass.name);
              res += "]\n";
              res += `[<table>${subclass.inheritName}]<:-[<table>${subclass.name}]\n`;
            });
          }
        });
      }
      if (this.displaySettings.includes("ontologies") && this.ontologies) {
        this.ontologies.forEach((table) => {
          res += `[<ontology> ${table.name}]\n`;
        });
      }

      // relations
      this.tables.forEach((table) => {
        if (Array.isArray(table.columns)) {
          table.columns
            .filter((c) => !c.inherited)
            .forEach((column) => {
              if (
                this.displaySettings.includes("ontologies") &&
                (column.columnType === "ONTOLOGY" ||
                  column.columnType === "ONTOLOGY_ARRAY")
              ) {
                const type = column.refSchemaName ? "externalo" : "ontology";
                res += `[<${type}>${column.refTableName}]<- ${column.name} [<table>${table.name}]\n`;
              } else if (
                column.columnType === "REF_ARRAY" ||
                column.columnType === "REF"
              ) {
                const type = column.refSchemaName ? "external" : "table";
                res += `[<${type}>${column.refTableName}]<- ${column.name} [<table>${table.name}]\n`;
              }
            });
        }
      });

      return res;
    },
  },
};
</script>
