<template>
  <div class="row p-2">
    <div :class="'border border-' + color" class="col-10 p-0">
      <div v-for="col in columns" :key="col.name" class="p-0">
        <div v-if="col.columnType == 'HEADING'" class="mt-0">
          <a href="#_top" class="float-right text-white">back to top</a>
          <h3
              :class="'pl-2 pr-2 pb-2 mb-0 text-white bg-' + color"
              :id="col.name"
          >
            <a>{{ col.name }}</a>
          </h3>
          <p class="p-2 bg-light mt-0">{{ col.description }}</p>
        </div>
        <div class="m-2 showcontainer">
          <h6>{{ toSentence(col.name) }}</h6>
          <small
              v-if="col.description"
              class="form-text text-muted showonhover"
          >{{ col.description }}</small
          >
          <p v-if="row[col.name] == undefined">N/A</p>
          <p
              v-else-if="
              ['STRING', 'TEXT', 'INT', 'BOOL'].includes(col.columnType)
            "
          >
            {{ row[col.name] }}
          </p>
          <p v-else-if="col.columnType == 'REF'">
            <OntologyTerms :terms="row[col.name]" :color="color" />
          </p>
          <p v-else-if="col.columnType == 'REF_ARRAY'">
            <OntologyTerms :terms="row[col.name]" :color="color" />
          </p>
          <p v-else>{{ renderValue(row, col) }}</p>
        </div>
      </div>
    </div>
    <div class="col-2">
      <div v-for="col in columns" :key="col.name" class="p-0">
        <div v-if="col.columnType == 'HEADING'" class="mt-0">
          <a :href="'#' + col.name" :class="'text-' + color">{{ col.name }}</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style :scoped>
/*.showonhover {*/
/*  display: none;*/
/*}*/

/*.showcontainer:hover > .showonhover {*/
/*  display: block;*/
/*}*/
</style>

<script>
import OntologyTerms from "./OntologyTerms";

export default {
  components: { OntologyTerms },
  props: {
    tableMetadata: Object,
    row: Object,
    showColumns: Array,
    hideNA: Boolean,
    color: { type: String, default: "primary" },
  },
  computed: {
    columns() {
      //return column metadata in order of showColumns
      if (this.tableMetadata && this.row) {
        return this.showColumns
            .map((name) =>
                this.tableMetadata.columns.find((column) => column.name == name)
            )
            .filter(
                (column) =>
                    !this.hideNA ||
                    column.columnType == "HEADING" ||
                    this.row[column.name] != undefined
            );
      } else return [];
    },
  },
  methods: {
    toSentence(str) {
      return (
          str
              // insert a space before all caps
              .replace(/([A-Z])/g, " $1")
              // uppercase the first character
              .replace(/^./, function (str) {
                return str.toUpperCase();
              })
      );
    },
    renderValue(row, col) {
      if (row[col.name] === undefined) {
        return [];
      }
      if (
          col.columnType == "REF_ARRAY" ||
          col.columnType == "MREF" ||
          col.columnType == "REFBACK" ||
          col.columnType == "ONTOLOGY_ARRAY"
      ) {
        return row[col.name].map((v) => {
          if (col.refLabel) {
            return this.applyJsTemplate(col.refLabel, v);
          } else {
            return this.flattenObject(v);
          }
        });
      } else if (col.columnType == "REF") {
        if (col.refLabel) {
          return [this.applyJsTemplate(col.refLabel, row[col.name])];
        } else {
          return [this.flattenObject(row[col.name])];
        }
      } else if (col.columnType.includes("ARRAY")) {
        return row[col.name];
      } else {
        return [row[col.name]];
      }
    },
    applyJsTemplate(template, object) {
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, "return `" + template + "`;")(...vals);
      } catch (err) {
        return (
            err.message +
            " we got keys:" +
            JSON.stringify(names) +
            " vals:" +
            JSON.stringify(vals) +
            " and template: " +
            template
        );
      }
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += "." + object[key];
        }
      });
      return result.replace(/^\./, "");
    },
  },
};
</script>
