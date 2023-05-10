<template>
  <div class="table-responsive">
    <table class="table table-sm bg-white table-bordered table-hover">
      <thead>
        <th>name</th>
        <th>databank family</th>
        <th>linkage strategy</th>
        <th>linkage description</th>
        <th>linkage variable</th>
        <th>linkage variable unique</th>
        <th>linkage completeness</th>
      </thead>
      <tr v-for="row in rows" :key="row.id" @click="handleRowClick(row)">
        <td>
          <a href="" @click.prevent
            >{{ row.linkedDatasource?.name }}
            <span v-if="row.linkedDatasource?.name !== row.linkedDatasource?.id"
              >({{ row.linkedDatasource?.id }})</span
            ></a
          >
        </td>
        <td>{{ row.linkedDatasource?.type?.map((t) => t.name).join(",") }}</td>
        <td>{{ row.linkedDatasource?.linkageStrategy?.name }}</td>
        <td>{{ row.linkedDatasource?.linkageDescription?.name }}</td>
        <td>{{ row.linkedDatasource?.linkageVariable }}</td>
        <td>{{ row.linkedDatasource?.linkageVariableUnique?.name }}</td>
        <td>{{ row.linkedDatasource?.linkageCompleteness }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
import { request } from "molgenis-components";

/* will show custom display for the linked data sources. Motivation is that we want to show 'type' as part of the label */
export default {
  name: "LinkedDataSourcesFieldValue",
  data() {
    return {
      rows: [],
    };
  },
  props: {
    /** metadata of the current column that refback should point to */
    metaData: {
      type: Object,
      required: true,
    },
  },
  computed: {
    filter() {
      return {
        mainDatasource: { equals: this.metaData.primaryTableKey },
      };
    },
  },
  methods: {
    handleRowClick(row) {
      this.$router.push({
        name: "Resources-details",
        params: {
          id: row.linkedDatasource.id,
        },
      });
    },
  },
  async mounted() {
    const query = `
      query LinkedDataSources($filter: LinkedDataSourcesFilter) {
        LinkedDataSources(filter: $filter) {
            mainDatasource {
              id
              name
            }
            linkedDatasource {
              id
              name
              type {
                name
              }
            }
            linkageVariable
            linkageStrategy {
              name
            }
            linkageDescription
            linkageCompleteness
            preLinked
          }
        }`;
    const response = await request("graphql", query, {
      filter: this.filter,
    }).catch((error) => (this.error = error));
    this.rows = response.LinkedDataSources;
  },
};
</script>
