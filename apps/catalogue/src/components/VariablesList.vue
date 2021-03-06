<template>
  <div>
    {{ topic }}
    <InputSearch v-model="search" />
    <p v-if="count == 0">No variables found</p>
    <div v-else class="mt-2">
      <Pagination class="mb-2" :count="count" :limit="limit" v-model="page" />
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <div class="card-columns">
        <VariableCard
          v-for="variable in variables"
          :key="
            variable.release.resource.acronym +
            variable.table.name +
            variable.name
          "
          :variable="variable"
          :tableName="tableName"
        />
      </div>
    </div>
    <ShowMore title="debug">
      {{ variables }}
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
import {
  InputSearch,
  MessageError,
  Pagination,
  ShowMore,
} from "@mswertz/emx2-styleguide";
import HarmonisationDetails from "./HarmonisationDetails";
import VariableCard from "./VariableCard";

export default {
  components: {
    HarmonisationDetails,
    Pagination,
    MessageError,
    VariableCard,
    InputSearch,
    ShowMore,
  },
  props: {
    resourceAcronym: String,
    tableName: String,
    topic: String,
    version: String,
  },
  data() {
    return {
      variables: [],
      search: null,
      count: 0,
      graphqlError: null,
      page: 1,
      limit: 20,
    };
  },
  methods: {
    reload() {
      this.graphqlError = null;
      let filter = {};
      if (this.resourceAcronym) {
        filter.release = {
          resource: { acronym: { equals: this.resourceAcronym } },
        };
      }
      if (this.tableName) {
        filter.table = { name: { equals: this.tableName } };
      }
      if (this.search) {
        filter._search = this.search;
      }
      if (this.topic) {
        filter.topics = { name: { equals: this.topic } };
      }
      // if (this.version) {
      //   filter.release.version = { equals: this.version };
      // }
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,filter:$filter){name, release{resource{acronym,mg_tableclass},version},table{name},label, format{name},unit{name}, description,topics{name},categories{label,value,isMissing},harmonisations{match{name},sourceRelease{resource{acronym},version},targetRelease{resource{acronym},version}sourceTable{name,release{resource{acronym},version}}}}
        ,Variables_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * this.limit,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
          this.count = data.Variables_agg.count;
          this.$forceUpdate();
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
    resourceAcronym() {
      this.reload();
    },
    tableName() {
      this.reload();
    },
    page() {
      this.reload();
    },
    search() {
      this.reload();
    },
    topic() {
      this.reload();
    },
    version() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
