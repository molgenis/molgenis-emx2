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
      <InstituteCard
        v-for="Institute in Institutes"
        :key="Institute.name"
        :Institute="Institute"
      />
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import InstituteCard from "../components/InstituteCard";

export default {
  components: {
    InstituteCard,
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
      Institutes: [],
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
        `query Institutes($filter:InstitutesFilter,$offset:Int,$limit:Int){Institutes(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,description,website}
        ,Institutes_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * this.limit,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.Institutes = data.Institutes;
          this.count = data.Institutes_agg.count;
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
