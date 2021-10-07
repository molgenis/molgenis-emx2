<template>
  <div class="container bg-white" id="_top" v-if="row">
    <ButtonAlt @click="toggleNA" class="float-right text-white">
      {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
    </ButtonAlt>
    <ResourceHeader
      :resource="row"
      :headerCss="'bg-' + color + ' text-white'"
      :table-name="table"
    />
    <div class="row p-2">
      <div :class="'border border-' + color" class="col-10 p-0">
        <div
          v-for="col in tableMetadata.columns.filter(
            (c) =>
              !c.name.startsWith('mg_') &&
              ((row[c.name] && row[c.name].length > 0) ||
                !hideNA ||
                c.columnType == 'HEADING')
          )"
          :key="col.name"
          class="p-0"
        >
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
          <div v-else class="m-2 showcontainer row">
            <div class="col-4 text-right labelcontainer">
              <div
                :class="'tooltip bs-tooltip-top show bg-' + color"
                role="tooltip"
              >
                <div :class="'tooltip-inner bg-' + color">
                  {{ col.description }}
                </div>
              </div>
              <label class="mb-0 font-weight-bold">
                {{ toSentence(col.name) }}:
              </label>
            </div>
            <div class="col-8">
              <p v-if="row[col.name] == undefined">N/A</p>
              <p
                v-else-if="
                  ['STRING', 'TEXT', 'INT', 'BOOL', 'DATE'].includes(
                    col.columnType
                  )
                "
              >
                {{ row[col.name] }}
              </p>
              <span v-else-if="'FILE' == col.columnType">
                <a
                  v-if="row[col.name].id != undefined"
                  :href="row[col.name].url"
                >
                  {{ col.name }}.{{ row[col.name].extension }} ({{
                    renderNumber(row[col.name].size)
                  }}b)
                </a>
                <span v-else>N/A</span>
              </span>
              <span v-else-if="'REFBACK' == col.columnType">
                <RefbackTable
                  :table="col.refTable"
                  :refBack="col.refBack"
                  :pkey="getPkey(row)"
                  :refLabel="col.refLabel"
                />
              </span>
              <span v-else-if="'REF' == col.columnType">
                <RouterLink
                  v-if="row[col.name]"
                  :to="{
                    name: col.refTable + '-details',
                    params: row[col.name],
                  }"
                  >{{ renderValue(row, col)[0] }}</RouterLink
                >
              </span>
              <span v-else-if="'REF_ARRAY' == col.columnType">
                <span v-for="(val, idx) in row[col.name]" :key="idx">
                  <RouterLink
                    v-if="val"
                    :to="{ name: col.refTable + '-details', params: val }"
                    >{{ renderValue(row, col)[idx] }}</RouterLink
                  ><br />
                </span>
              </span>
              <span
                v-else-if="
                  ['ONTOLOGY', 'ONTOLOGY_ARRAY'].includes(col.columnType)
                "
              >
                <div
                  :class="
                    'font-weight-bold mr-2 mb-2 badge bade-lg badge-' + color
                  "
                  v-for="val in renderValue(row, col)"
                  :key="val"
                >
                  {{ val }}
                </div>
              </span>
              <p v-else>{{ renderValue(row, col) }}</p>
            </div>
          </div>
        </div>
      </div>
      <div class="col-2">
        <div v-for="col in tableMetadata.columns" :key="col.name" class="p-0">
          <div v-if="col.columnType == 'HEADING'" class="mt-0">
            <a :href="'#' + col.name" :class="'text-' + color">{{
              col.name
            }}</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.labelcontainer .tooltip {
  visibility: hidden;
}

.labelcontainer:hover .tooltip {
  visibility: visible;
}
</style>

<script>
import { TableMixin, ButtonAlt } from "@mswertz/emx2-styleguide";
import ResourceHeader from "../components/ResourceHeader";
import RefbackTable from "../components/RefbackTable";

export default {
  extends: TableMixin,
  props: {
    color: { type: String, default: "primary" },
  },
  data() {
    return {
      hideNA: true,
    };
  },
  computed: {
    row() {
      return this.data[0];
    },
  },
  methods: {
    openRouterLink(event, name) {
      this.$router.push({
        name: name + "-details",
        params: event[Object.keys(event)[0]],
      });
    },
    toggleNA() {
      this.hideNA = !this.hideNA;
    },
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
      } else if (col.columnType == "REF" || col.columnType == "ONTOLOGY") {
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
    getRefBackType(column) {
      if (column.columnType === "REFBACK") {
        //get the other table, find the refback column and check its type
        return this.getTable(column.refTable)
          .columns.filter((c) => c.name === column.refBack)
          .map((c) => c.columnType)[0];
      }
    },
    renderNumber(number) {
      var SI_SYMBOL = ["", "k", "M", "G", "T", "P", "E"];

      // what tier? (determines SI symbol)
      var tier = (Math.log10(number) / 3) | 0;

      // if zero, we don't need a suffix
      if (tier == 0) return number;

      // get suffix and determine scale
      var suffix = SI_SYMBOL[tier];
      var scale = Math.pow(10, tier * 3);

      // scale the number
      var scaled = number / scale;

      // format number and add suffix
      return scaled.toFixed(1) + suffix;
    },
  },
  components: { ResourceHeader, ButtonAlt, RefbackTable },
};
</script>
