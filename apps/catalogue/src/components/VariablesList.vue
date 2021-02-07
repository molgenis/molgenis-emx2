<template>
  <div>
    {{ topic }}
    <InputSearch v-model="search" />
    <p v-if="count == 0">No variables found</p>
    <div v-else class="mt-2">
      <Pagination class="mb-2" :count="count" :limit="limit" v-model="page" />
      <MessageError v-if="error">{{ error }}</MessageError>
      <div class="card-columns">
        <VariableCard
          v-for="variable in variables"
          :key="
            variable.table.resource.acronym +
            variable.table.name +
            variable.name
          "
          :variable="variable"
          :tableName="tableName"
        />
      </div>
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
import {
  InputSearch,
  MessageError,
  Pagination,
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
  },
  props: {
    resourceAcronym: String,
    tableName: String,
    topic: String,
  },
  data() {
    return {
      variables: [],
      search: null,
      count: 0,
      error: null,
      page: 1,
      limit: 20,
    };
  },
  methods: {
    reload() {
      this.error = null;
      let filter = {};
      if (this.resourceAcronym) {
        filter.resource = { acronym: { equals: this.resourceAcronym } };
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
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,filter:$filter){name, table{name,resource{acronym,mg_tableclass}},label, format{name},unit{name}, description,topics{name},categories{label,value,isMissing},harmonisations{match{name},sourceTable{name,resource{acronym}}}}
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
  },
  created() {
    this.reload();
  },
};
</script>
