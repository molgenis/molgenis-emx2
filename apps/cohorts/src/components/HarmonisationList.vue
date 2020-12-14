<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <p v-if="count == 0">No harmonisations found</p>
    <div v-else>
      <table class="table">
        <thead>
          <tr>
            <th scope="col">variable</th>
            <th scope="col" v-for="d in datasets">{{ d }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in variables">
            <th scope="row">{{ v }}</th>
            <td v-for="d in datasets">
              <HarmonisationDetails
                :compact="true"
                :target-variable="v"
                :target-dataset="datasetName"
                :target-collection="collectionAcronym"
                :source-collection="d.split(':')[0]"
                :source-dataset="d.split(':')[1]"
                :match="matrix[v][d]"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <ShowMore title="debug">
      <pre>
      harmonisations = {{ JSON.stringify(harmonisations) }}

      datasets = {{ JSON.stringify(datasets) }}

      variables = {{ JSON.stringify(variables) }}

      matrix = {{ JSON.stringify(matrix) }}
      </pre>
    </ShowMore>
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
    datasetName: String,
  },
  data() {
    return {
      harmonisations: [],
      count: 0,
      error: null,
      page: 1,
      limit: 10000,
    };
  },
  computed: {
    datasets() {
      return [
        ...new Set(
          this.harmonisations.map(
            (h) =>
              h.sourceDataset.collection.acronym + ":" + h.sourceDataset.name
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
            h.sourceDataset.collection.acronym + ":" + h.sourceDataset.name
          ] = h.match.name;
        }
      });
      return result;
    },
  },
  methods: {
    reload() {
      let filter = {};
      //filter:{targetVariable:{dataset:{name:{equals:"table1"},collection:{acronym:{equals:"LifeCycle"}}}}}
      if (this.collectionAcronym) {
        filter.targetVariable = {
          dataset: {
            collection: { acronym: { equals: this.collectionAcronym } },
          },
        };
      }
      if (this.datasetName) {
        filter.targetVariable.dataset.name = { equals: this.datasetName };
      }
      console.log(JSON.stringify(filter));
      request(
        "graphql",
        `query VariableHarmonisations($filter:VariableHarmonisationsFilter,$offset:Int,$limit:Int){VariableHarmonisations(offset:$offset,limit:$limit,filter:$filter)
          {targetVariable{name}sourceDataset{collection{acronym}name}match{name}}
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
    datasetName() {
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
