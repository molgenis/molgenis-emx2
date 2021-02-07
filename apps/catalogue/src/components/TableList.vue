<template>
  <div>
    <InputSearch v-model="search" />
    <p v-if="count == 0">No tables found</p>
    <Pagination class="mt-2" :count="count" :limit="limit" v-model="page" />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns mt-2">
      <TableCard
        v-for="table in tables"
        :key="table.resource.acronym + ':' + table.name"
        :table="table"
        :databankAcronym="databankAcronym"
        :projectAcronym="projectAcronym"
        :institutionAcronym="institutionAcronym"
      />
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
  MessageError,
  Pagination,
  InputSearch,
} from "@mswertz/emx2-styleguide";
import TableCard from "./TableCard";

export default {
  components: {
    TableCard,
    Pagination,
    MessageError,
    InputSearch,
  },
  props: {
    databankAcronym: String,
    institutionAcronym: String,
    projectAcronym: String,
  },
  data() {
    return {
      tables: [],
      count: 0,
      error: null,
      search: null,
      page: 1,
      limit: 9,
    };
  },
  methods: {
    reload() {
      let filter = {};
      if (this.databankAcronym) {
        filter.resource = { acronym: { equals: this.databankAcronym } };
      }
      if (this.projectAcronym) {
        filter.resource = { acronym: { equals: this.projectAcronym } };
      }
      if (this.search) {
        filter._search = this.search;
      }
      request(
        "graphql",
        `query Tables($filter:TablesFilter,$offset:Int,$limit:Int){Tables(offset:$offset,limit:$limit,filter:$filter){name,resource{acronym,name,mg_tableclass},label,variables_agg{count}}
        ,Tables_agg(filter:$filter){count}}`,
        {
          filter: filter,
          offset: (this.page - 1) * this.limit,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.tables = data.Tables;
          this.count = data.Tables_agg.count;
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
    databankAcronym() {
      this.reload();
    },
    projectAcronym() {
      this.reload();
    },
    page() {
      this.reload();
    },
    search() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
};
</script>
