<template>
  <div>
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
      <CollectionsCard
        v-for="collection in collections"
        :key="collection.name"
        :collection="collection"
      />
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import CollectionsCard from "../components/CollectionCard";

export default {
  components: {
    CollectionsCard,
    Pagination,
    MessageError,
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
      collections: [],
    };
  },
  methods: {
    reload() {
      console.log("collections reload");
      let searchString = "";
      if (this.search && this.search.trim() != "") {
        searchString = `search:"${this.search}",`;
      }
      if (this.providerAcronym) {
        this.filter["provider"] = { acronym: { equals: this.providerAcronym } };
      }
      request(
        "graphql",
        `query Collections($filter:CollectionsFilter,$offset:Int,$limit:Int){Collections(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,provider{acronym,name},datasets{name,variables{name}}}
        ,Collections_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.collections = data.Collections;
          this.count = data.Collections_agg.count;
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
