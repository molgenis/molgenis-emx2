<template>
  <div>
    <div class="card-columns">
      <CollectionsView
        v-for="collection in collections"
        :key="collection.name"
        :collection="collection"
      />
    </div>
    <Pagination
      v-if="count > 0"
      class="justify-content-center"
      :count="count"
      v-model="page"
      :limit="limit"
      :defaultValue="page"
    />
  </div>
</template>

<script>
import { Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import CollectionsView from "./CollectionsView";

export default {
  components: {
    CollectionsView,
    Pagination,
  },
  props: {
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
      limit: 20,
      count: 0,
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
      request(
        "graphql",
        `query Collections($filter:CollectionsFilter,$offset:Int,$limit:Int){Collections(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,tables{name,variables{name}}}
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
