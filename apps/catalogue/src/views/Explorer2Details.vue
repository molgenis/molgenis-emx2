<template>
  <div>
    <h1>Details</h1>
    network = {{ network }} <br />
    table = {{ table }} <br />
    variable = {{ variable }}
    <table class="table bg-white">
      <thead>
        <th>Source</th>
        <th>Match</th>
        <th>Syntax</th>
        <th>Description</th>
        <th>Source variables</th>
      </thead>
      <tbody>
        <tr v-for="m in mappings">
          <td>{{ m.fromRelease.resource.acronym }}</td>
          <td>{{ m.match.name }}</td>
          <td>
            <pre>{{ m.syntax }}</pre>
          </td>
          <td>{{ m.description }}</td>
          <td>{{ m.fromVariables }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { request } from "graphql-request";

export default {
  props: {
    network: String,
    table: String,
    variable: String,
  },
  data() {
    return {
      mappings: [],
    };
  },
  methods: {
    reload() {
      let graphqlFilter = {
        toVariable: {
          name: { equals: this.variable },
        },
        toRelease: { resource: { acronym: { equals: this.network } } },
        toTable: { name: { equals: this.table } },
      };
      request(
        "graphql",
        `query VariableMappings($filter: VariableMappingsFilter){VariableMappings(filter:$filter,limit:10000){
          fromVariable{name,table{name}},fromTable{name},match{name},description,syntax,fromRelease{resource{acronym}}}
          }`,
        {
          filter: graphqlFilter,
        }
      )
        .then((data) => {
          this.mappings = data.VariableMappings;
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    filters: {
      deep: true,
      handler() {
        this.reload();
      },
    },
  },
};
</script>
