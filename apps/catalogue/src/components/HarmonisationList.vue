<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
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
                :target-collection="collectionAcronym"
                :source-collection="dtsplit(':')[0]"
                :source-table="t.split(':')[1]"
                :match="matrix[v][d]"
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
import { MessageError, Pagination, ShowMore } from "@mswertz/emx2-styleguide";
import HarmonisationDetails from "./HarmonisationDetails";

export default {
  components: {
    HarmonisationDetails,
    Pagination,
    MessageError,
    ShowMore,
  },
  props: {
    collectionAcronym: String,
    tableName: String,
  },
  data() {
    return {
      harmonisations: [],
      count: 0,
      error: null,
      page: 1,
      limit: 0,
    };
  },
  computed: {
    tables() {
      return [
        ...new Set(
          this.harmonisations.map(
            (h) => h.sourceTable.collection.acronym + ":" + h.sourceTable.name
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
            h.sourceTable.collection.acronym + ":" + h.sourceTable.name
          ] = h.match.name;
        }
      });
      return result;
    },
  },
  methods: {
    reload() {
      let filter = {};
      //filter:{targetVariable:{table:{name:{equals:"table1"},collection:{acronym:{equals:"LifeCycle"}}}}}
      if (this.collectionAcronym) {
        filter.targetVariable = {
          table: {
            collection: { acronym: { equals: this.collectionAcronym } },
          },
        };
      }
      if (this.tableName !== undefined) {
        filter.targetVariable.table.name = { equals: this.tableName };
      }
      console.log(JSON.stringify(filter));
      request(
        "graphql",
        `query VariableHarmonisations($filter:VariableHarmonisationsFilter,$offset:Int,$limit:Int){VariableHarmonisations(offset:$offset,limit:$limit,filter:$filter)
          {targetVariable{name,table{harmonisations{sourceTable{name}description}}}sourceTable{collection{acronym}name}match{name}}
        ,VariableHarmonisations_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.harmonisations = data.VariableHarmonisations;
          this.count = data.VariableHarmonisations_agg.count;
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  watch: {
    collectionAcronym() {
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
