<template>
  <Spinner v-if="!tableMetadata" />
  <div class="table-responsive" v-else-if="pkey && tableMetadata && data">
    <table class="table table-sm bg-white table-bordered table-hover">
      <thead>
        <th
          v-for="col in visibleColumns.filter((c) => c.columnType != 'HEADING')"
          :key="col.name"
          scope="col"
        >
          <h6 class="mb-0">{{ col.name }}</h6>
        </th>
      </thead>
      <tbody>
        <tr
          v-for="(row, idx) in data"
          :key="idx + JSON.stringify(Object.keys(row))"
        >
          <td
            v-for="col in visibleColumns.filter(
              (c) => c.columnType != 'HEADING'
            )"
            :key="idx + col.name"
            style="cursor: pointer"
          >
            <div
              v-if="
                'REF' === col.columnType ||
                ('REFBACK' === col.columnType && !Array.isArray(row[col.name]))
              "
            >
              <RouterLink
                v-if="row[col.name]"
                :to="{
                  name: col.refTable + '-details',
                  params: routeParams(col, row[col.name])
                }"
              >
                {{ renderValue(row, col)[0] }}
              </RouterLink>
            </div>
            <span
              v-else-if="
                'REF_ARRAY' == col.columnType ||
                ('REFBACK' === col.columnType && Array.isArray(row[col.name]))
              "
            >
              <span v-for="(val, idx) in row[col.name]" :key="idx">
                <RouterLink
                  v-if="val"
                  :to="{
                    name: col.refTable + '-details',
                    params: routeParams(col, val)
                  }"
                >
                  {{ renderValue(row, col)[idx] }} </RouterLink
                ><br />
              </span>
            </span>
            <div
              v-else
              v-for="(value, idx2) in renderValue(row, col)"
              :key="idx + col.name + idx2"
            >
              <div v-if="'TEXT' === col.columnType">
                <ReadMore :text="value" />
              </div>
              <div v-else-if="'FILE' === col.columnType">
                <a v-if="row[col.name].id" :href="row[col.name].url">
                  {{ col.name }}.{{ row[col.name].extension }} ({{
                    renderNumber(row[col.name].size)
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
import {TableMixin, Spinner, ReadMore} from '@mswertz/emx2-styleguide';

export default {
  mixins: [TableMixin],
  components: {
    Spinner,
    ReadMore
  },
  props: {
    /** name of the column in the other table */
    refBack: String,
    /** pkey of the current table that refback should point to */
    pkey: Object
  },
  methods: {
    routeParams(column, value) {
      if (column.name === 'tables') {
        let result = {
          pid: value.dataDictionary.resource.pid,
          version: value.dataDictionary.version,
          name: value.name
        };
        return result;
      } else {
        return value;
      }
    },
    click(value) {
      this.$emit('click', value);
    },
    renderValue(row, col) {
      if (row[col.name] === undefined) {
        return [];
      }
      if (
        col.columnType == 'REF_ARRAY' ||
        col.columnType == 'REFBACK' ||
        col.columnType == 'ONTOLOGY_ARRAY'
      ) {
        return row[col.name].map((v) => {
          if (col.name === 'tables') {
            //hack, ideally we start setting refLabel in configuration!
            return v.name;
          } else if (col.refLabel) {
            return this.applyJsTemplate(col.refLabel, v);
          } else {
            return this.flattenObject(v);
          }
        });
      } else if (col.columnType == 'REF' || col.columnType == 'ONTOLOGY') {
        if (col.refLabel) {
          return [this.applyJsTemplate(col.refLabel, row[col.name])];
        } else {
          return [this.flattenObject(row[col.name])];
        }
      } else if (col.columnType.includes('ARRAY')) {
        return row[col.name];
      } else {
        return [row[col.name]];
      }
    },
    applyJsTemplate(template, object) {
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, 'return `' + template + '`;')(...vals);
      } catch (err) {
        return (
          err.message +
          ' we got keys:' +
          JSON.stringify(names) +
          ' vals:' +
          JSON.stringify(vals) +
          ' and template: ' +
          template
        );
      }
    },
    flattenObject(object) {
      let result = '';
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key]);
        } else {
          result += '.' + object[key];
        }
      });
      return result.replace(/^\./, '');
    }
  },
  computed: {
    graphqlFilter() {
      var result = new Object();
      result[this.refBack] = {
        equals: this.pkey
      };
      return result;
    },
    visibleColumnNames() {
      return this.visibleColumns.map((c) => c.name);
    },
    visibleColumns() {
      //columns, excludes refback and mg_
      if (this.tableMetadata && this.tableMetadata.columns) {
        return this.tableMetadata.columns.filter(
          (c) => c.name != this.refBack && !c.name.startsWith('mg_')
        );
      }
      return [];
    }
  }
};
</script>
