<template>
  <div class="table-responsive" v-if="rows.length > 0">
    <span class="float-right"
      >download variable and mapping definitions as:
      <a :href="downloadMappingsZipUrl">Zip file</a> or
      <a :href="downloadMappingsExcelUrl">Excel file</a></span
    >
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
          <a href="" @click.prevent>
            {{ row.linkedResource?.name }}
            <span v-if="row.linkedResource?.name !== row.linkedResource?.id"
              >({{ row.linkedResource?.id }})</span
            >
          </a>
        </td>
        <td>{{ row.linkedResource?.type?.map((t) => t.name).join(", ") }}</td>
        <td>{{ row.linkedResource?.linkageStrategy?.name }}</td>
        <td>{{ row.linkedResource?.linkageDescription?.name }}</td>
        <td>{{ row.linkedResource?.linkageVariable }}</td>
        <td>{{ row.linkedResource?.linkageVariableUnique?.name }}</td>
        <td>{{ row.linkedResource?.linkageCompleteness }}</td>
      </tr>
    </table>
  </div>
</template>

<script>
import { request } from "molgenis-components";

/* will show custom display for the linked data sources. Motivation is that we want to show 'type' as part of the label */
export default {
  name: "linkedResourcesFieldValue",
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
        mainResource: { equals: this.metaData.primaryTableKey },
      };
    },
    downloadMappingsZipUrl() {
      return (
        "../api/reports/zip?id=0,1,2,3&resources=" +
        this.rows.map((row) => row.linkedResource.id).join(",")
      );
    },
    downloadMappingsExcelUrl() {
      return (
        "../api/reports/excel?id=0,1,2,3&resources=" +
        this.rows.map((row) => row.linkedResource.id).join(",")
      );
    },
  },
  methods: {
    handleRowClick(row) {
      this.$router.push({
        name: "Resources-details",
        params: {
          id: row.linkedResource.id,
        },
      });
    },
  },
  async mounted() {
    const query = `
      query LinkedResources($filter: LinkedResourcesFilter) {
        LinkedResources(filter: $filter) {
            mainResource {
              id
              name
            }
            linkedResource {
              id
              name
              type {
                name
              }
            }
            linkageVariable
            linkageVariableUnique,
            linkageStrategy {
              name
            }
            linkageCompleteness
            preLinked
          }
        }`;
    const response = await request("graphql", query, {
      filter: this.filter,
    }).catch((error) => (this.error = error));
    this.rows = response.LinkedResources;
  },
};
</script>
