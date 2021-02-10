<template>
  <div>
    <div class="align-content-center" v-if="count > 0">
      <Pagination
        :count="count"
        v-model="page"
        :limit="limit"
        :defaultValue="page"
      />
      <InputSearch v-if="showSearch" v-model="search" />
    </div>
    <p v-else>No records found.</p>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns">
      <DatasourceCard
        v-for="datasource in datasources"
        :key="datasource.acronym"
        :datasource="datasource"
        :institutionAcronym="institutionAcronym"
      />
    </div>
  </div>
</template>

<style>
.card-columns {
  @include media-breakpoint-only(lg) {
    column-count: 4;
  }
  @include media-breakpoint-only(xl) {
    column-count: 5;
  }
}
</style>

<script>
import {
  MessageError,
  Pagination,
  InputSearch,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import DatasourceCard from "./DatasourceCard";

export default {
  components: {
    DatasourceCard,
    Pagination,
    MessageError,
    InputSearch,
  },
  props: {
    showSearch: { type: Boolean, default: true },
    institutionAcronym: String,
    filter: {
      type: Object,
      default() {
        return {};
      },
    },
    search: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      page: 1,
      limit: 9,
      count: 0,
      error: null,
      loading: false,
      datasources: [],
    };
  },
  methods: {
    reload() {
      let searchString = "";
      if (this.search && this.search.trim() != "") {
        searchString = `search:"${this.search}",`;
      }
      if (this.institutionAcronym) {
        this.filter["provider"] = {
          acronym: { equals: this.institutionAcronym },
        };
      }
      request(
        "graphql",
        `query Datasources($filter:DatasourcesFilter,$offset:Int,$limit:Int){Datasources(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,provider{acronym,name}}
        ,Datasources_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.datasources = data.Datasources;
          this.count = data.Datasources_agg.count;
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
