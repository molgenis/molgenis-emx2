<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <p v-if="count == 0">No harmonisations found</p>
    <div v-else class="mt-2">
      <i class="fa fa-check-circle text-success" /> = complete match,
      <i class="fa fa-check-circle text-warning" /> = partial match,
      <i class="fa fa-times text-primary" /> = no match,
      <i class="fa fa-question text-primary" /> = not provided.
      <table class="table mt-2">
        <thead>
          <tr>
            <th scope="col">variable</th>
            <th scope="col" v-for="t in tables">{{ t }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in variables">
            <th scope="row">{{ v }}</th>
            <td v-for="t in tables">
              <HarmonisationDetails
                :compact="true"
                :target-variable="v"
                :target-table="tableName"
                :target-resource="resourcePid"
                :source-resource="t.split(':')[0]"
                :source-table="t.split(':')[1]"
                :match="matrix[v][t]"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
dt {
  float: left;
  clear: left;
  width: 100px;
  font-weight: bold;
}

dd {
  margin: 0 0 0 110px;
  padding: 0 0 0.5em 0;
}
</style>

<script>
import { request } from "graphql-request";
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import HarmonisationDetails from "./HarmonisationDetails";

export default {
  components: {
    HarmonisationDetails,
    Pagination,
    MessageError,
  },
  props: {
    resourcePid: String,
    tableName: String,
  },
  data() {
    return {
      harmonisations: [],
      count: 0,
      graphqlError: null,
      page: 1,
      limit: 0,
    };
  },
  computed: {
    tables() {
      return [
        ...new Set(
          this.harmonisations.map(
            (h) => h.sourceRelease.resource.pid + ":" + h.sourceTable.name
          )
        ),
      ].sort();
    },
    variables() {
      return [
        ...new Set(this.harmonisations.map((h) => h.targetVariable.name)),
      ].sort();
    },
    matrix() {
      //create scaffold
      let result = {};
      this.variables.forEach((v) => {
        result[v] = {};
      });
      // put harmonisations over defaults
      this.harmonisations.forEach((h) => {
        if (h.match) {
          result[h.targetVariable.name][
            h.sourceTable.resource.pid + ":" + h.sourceTable.name
          ] = h.match.name;
        }
      });
      return result;
    },
  },
  methods: {
    reload() {
      let filter = {};
      //filter:{targetVariable:{table:{name:{equals:"table1"},resource:{pid:{equals:"LifeCycle"}}}}}
      if (this.resourcePid) {
        filter.targetRelease = {
          resource: { pid: { equals: this.resourcePid } },
        };
      }
      if (this.tableName !== undefined) {
        filter.targetVariable.table.name = { equals: this.tableName };
      }
      request(
        "graphql",
        `query VariableHarmonisations($filter:VariableHarmonisationsFilter,$offset:Int,$limit:Int){VariableHarmonisations(offset:$offset,limit:$limit,filter:$filter)
          {targetVariable{name,table{harmonisations{sourceTable{name}description}}}sourceTable{release{resource{pid}}name}match{name}}
        ,VariableHarmonisations_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * this.limit,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.harmonisations = data.VariableHarmonisations;
          this.count = data.VariableHarmonisations_agg.count;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  watch: {
    resourcePid() {
      this.reload();
    },
    tableName() {
      this.reload();
    },
    page() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
