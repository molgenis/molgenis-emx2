<template>
  <div>
    <div class="row" v-if="count > 0">
      <Pagination
        class="col-3"
        :count="count"
        v-model="page"
        :limit="limit"
        :defaultValue="page"
      />
      <InputSearch v-model="search" class="col-9" />
    </div>
    <p v-else>No records found.</p>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="card-columns">
      <DatabankCard
        v-for="databank in databanks"
        :key="databank.name"
        :databank="databank"
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
import DatabankCard from "./DatabankCard";

export default {
  components: {
    DatabankCard,
    Pagination,
    MessageError,
    InputSearch,
  },
  props: {
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
      databanks: [],
    };
  },
  methods: {
    reload() {
      let searchString = "";
      if (this.search && this.search.trim() != "") {
        searchString = `search:"${this.search}",`;
      }
      if (this.institutionAcronym) {
        this.filter["institution"] = {
          acronym: { equals: this.institutionAcronym },
        };
      }
      request(
        "graphql",
        `query Databanks($filter:DatabanksFilter,$offset:Int,$limit:Int){Databanks(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,institution{acronym,name}}
        ,Databanks_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.databanks = data.Databanks;
          this.count = data.Databanks_agg.count;
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
