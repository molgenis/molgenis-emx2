<template>
  <div>
    <Pagination class="mt-2" :count="count" :limit="limit" v-model="page" />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns mt-2">
      <TableCard
        v-for="table in tables"
        :key="table.collection.acronym + ':' + table.name"
        :table="table"
        :databankAcronym="databankAcronym"
        :consortiumAcronym="consortiumAcronym"
        :providerAcronym="providerAcronym"
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
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import TableCard from "./TableCard";

export default {
  components: {
    TableCard,
    Pagination,
    MessageError,
  },
  props: {
    databankAcronym: String,
    providerAcronym: String,
    consortiumAcronym: String,
  },
  data() {
    return {
      tables: [],
      count: 0,
      error: null,
      page: 1,
      limit: 9,
    };
  },
  methods: {
    reload() {
      let filter = {};
      if (this.databankAcronym) {
        filter.collection = { acronym: { equals: this.databankAcronym } };
      }
      if (this.consortiumAcronym) {
        filter.collection = { acronym: { equals: this.consortiumAcronym } };
      }
      request(
        "graphql",
        `query Tables($filter:TablesFilter,$offset:Int,$limit:Int){Tables(offset:$offset,limit:$limit,filter:$filter){name,collection{acronym,name,mg_tableclass},label,variables_agg{count}}
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
    consortiumAcronym() {
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
