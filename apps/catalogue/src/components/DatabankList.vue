<template>
  <div>
    <InputSearch v-model="search" />
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row justify-content-center mb-2">
      <Pagination
        v-if="count > 0"
        :count="count"
        v-model="page"
        :limit="limit"
        :defaultValue="page"
      />
    </div>
    <div class="row">
      <DatabankCard
        v-for="databank in databanks"
        :key="databank.name"
        :databank="databank"
        :providerAcronym="providerAcronym"
      />
    </div>
  </div>
</template>

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
    providerAcronym: String,
    filter: {
      type: Object,
      default() {
        return {};
      },
    },
    search: {
      String,
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
      if (this.providerAcronym) {
        this.filter["provider"] = { acronym: { equals: this.providerAcronym } };
      }
      request(
        "graphql",
        `query Databanks($filter:DatabanksFilter,$offset:Int,$limit:Int){Databanks(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,provider{acronym,name},tables{name,variables{name}}}
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
