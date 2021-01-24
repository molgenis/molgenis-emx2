<template>
  <div>
    <h1>Providers</h1>
    <p>Universities, Biobanks, Companies, Research institutes and more ...</p>
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
      <ProviderCard
        v-for="organisation in organisations"
        :key="organisation.name"
        :organisation="organisation"
      />
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import ProviderCard from "../components/ProviderCard";

export default {
  components: {
    ProviderCard,
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
      organisations: [],
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
        `query Organisations($filter:OrganisationsFilter,$offset:Int,$limit:Int){Organisations(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,description,website}
        ,Organisations_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.organisations = data.Organisations;
          this.count = data.Organisations_agg.count;
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
