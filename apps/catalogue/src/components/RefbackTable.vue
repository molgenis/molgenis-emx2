<template>
  <Spinner v-if="!tableMetadata" />
  <div
    class="table-responsive"
    v-else-if="pkey && tableMetadata && refBackData"
  >
    <table class="table table-sm bg-white table-bordered table-hover">
      <thead>
        <th
          v-for="col in visibleColumns.filter((c) => c.columnType != 'HEADING')"
          :key="col.id"
          scope="col"
        >
          <h6 class="mb-0">{{ col.label }}</h6>
        </th>
      </thead>
      <tbody>
        <tr
          v-for="(row, idx) in refBackData"
          :key="idx + JSON.stringify(Object.keys(row))"
          @click.prevent
        >
          <td
            v-for="col in visibleColumns.filter(
              (c) => c.columnType != 'HEADING'
            )"
            :key="idx + col.id"
            style="cursor: pointer"
          >
            <div v-if="col.key === 1">
              <a href="" @click="handleRowClick(row)">{{
                Array.isArray(renderValue(row, col))
                  ? renderValue(row, col)[0]
                  : renderValue(row, col)
              }}</a>
            </div>
            <div
              v-else-if="
                'REF' === col.columnType ||
                ('REFBACK' === col.columnType && !Array.isArray(row[col.id]))
              "
            >
              <RouterLink
                v-if="row[col.id]"
                :to="{
                  name: col.refTableId + '-details',
                  params: routeParams(col, row[col.id]),
                }"
              >
                {{ renderValue(row, col)[0] }}
              </RouterLink>
            </div>
            <span
              v-else-if="
                'REF_ARRAY' == col.columnType ||
                ('REFBACK' === col.columnType && Array.isArray(row[col.id]))
              "
            >
              <span v-for="(val, idx) in row[col.id]" :key="idx">
                <RouterLink
                  v-if="val"
                  :to="{
                    name: col.refTableId + '-details',
                    params: routeParams(col, val),
                  }"
                >
                  {{ renderValue(row, col)[idx] }} </RouterLink
                ><br />
              </span>
            </span>
            <div
              v-else
              v-for="(value, idx2) in renderValue(row, col)"
              :key="idx + col.id + idx2"
            >
              <div v-if="'TEXT' === col.columnType">
                <ReadMore :text="value" />
              </div>
              <div v-else-if="'FILE' === col.columnType">
                <a v-if="row[col.id].id" :href="row[col.id].url">
                  {{ col.id }}.{{ row[col.id].extension }} ({{
                    renderNumber(row[col.id].size)
                  }}b)
                </a>
              </div>
              <span v-else>{{ value }}</span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import {
  Spinner,
  ReadMore,
  Client,
  flattenObject,
  applyJsTemplate,
} from "molgenis-components";

export default {
  components: {
    Spinner,
    ReadMore,
  },
  props: {
    tableId: {
      type: String,
      required: true,
    },
    refLabel: String,
    /** id of the column in the other table */
    refBackId: String,
    /** pkey of the current table that refback should point to */
    pkey: Object,
  },
  data() {
    return {
      tableMetadata: null,
      refBackData: null,
    };
  },
  methods: {
    handleRowClick(row) {
      let params = {};
      params.id = row.id || this.pkey.id;
      params.resource = row.id || this.pkey.id;
      if (row.name) params.name = row.name;
      if (row.source?.id) params.source = row.source.id;
      if (row.sourceDataset?.name)
        params.sourceDataset = row.sourceDataset.name;
      if (row.target?.id) params.target = row.target?.id;
      if (row.targetDataset?.name)
        params.targetDataset = row.targetDataset.name;
      console.log(
        "hoi " + JSON.stringify(row) + " => " + JSON.stringify(params)
      );

      //good guessing the parameters :-)
      this.$router.push({
        name: this.tableId + "-details",
        params,
      });
    },
    routeParams(column, value) {
      if (column.id === "datasets") {
        let result = {
          resource: value.resource.id,
          name: value.name,
        };
        return result;
      } else if (column.name === "contacts") {
        return {
          resource: value.resource.id,
          firstName: value.firstName,
          lastName: value.lastName,
        };
      } else {
        return value;
      }
    },
    click(value) {
      this.$emit("click", value);
    },
    renderValue(row, col) {
      if (row[col.id] === undefined) {
        return [];
      }
      if (
        col.columnType == "REF_ARRAY" ||
        col.columnType == "REFBACK" ||
        col.columnType == "ONTOLOGY_ARRAY"
      ) {
        return row[col.id].map((v) => {
          if (col.id === "datasets") {
            //hack, ideally we start setting refLabel in configuration! <= actually we do
            return v.name;
          } else if (col.refLabel) {
            return applyJsTemplate(v, col.refLabel);
          } else {
            return applyJsTemplate(v, col.refLabelDefault);
          }
        });
      } else if (col.columnType == "REF" || col.columnType == "ONTOLOGY") {
        if (col.refLabel) {
          return [applyJsTemplate(row[col.id], col.refLabel)];
        } else {
          return applyJsTemplate(row[col.id], col.refLabelDefault);
        }
      } else if (col.columnType.includes("ARRAY")) {
        return row[col.id];
      } else {
        return [row[col.id]];
      }
    },
  },
  computed: {
    graphqlFilter() {
      var result = new Object();
      result[this.refBackId] = {
        equals: this.pkey,
      };
      return result;
    },
    visibleColumnIds() {
      return this.visibleColumns.map((c) => c.id);
    },
    visibleColumns() {
      //columns, excludes refback and mg_
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.filter(
          (c) => c.id != this.refBackId && !c.id.startsWith("mg_")
        );
      }
      return [];
    },
  },
  async created() {
    this.client = Client.newClient();
    this.tableMetadata = await this.client.fetchTableMetaData(this.tableId);
    this.refBackData = await this.client.fetchTableDataValues(this.tableId, {
      filter: this.graphqlFilter,
    });
  },
};
</script>
