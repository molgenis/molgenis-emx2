<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <div class="row">
      <Pagination
        v-if="count > 0"
        class="justify-content-center col-10 mb-2"
        :count="count"
        v-model="page"
        :limit="limit"
        :defaultValue="page"
      />
    </div>
    <div class="row">
      <ConsortiumCard
        v-for="consortium in consortia"
        :key="consortium.name"
        :consortium="consortium"
      />
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import ConsortiumCard from "./ConsortiumCard";
import TableOfContents from "../components/TableOfContents";

export default {
  components: {
    TableOfContents,
    ConsortiumCard,
    Pagination,
    MessageError,
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
      error: null,
      loading: false,
      consortia: [],
    };
  },
  methods: {
    reload() {
      let searchString = "";
      if (this.search && this.search.trim() != "") {
        searchString = `search:"${this.search}",`;
      }
      request(
        "graphql",
        `query Consortia($filter:ConsortiaFilter,$offset:Int,$limit:Int){Consortia(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,provider{name},tables{name,variables{name}}}
        ,Consortia_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.consortia = data.Consortia;
          this.count = data.Consortia_agg.count;
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
