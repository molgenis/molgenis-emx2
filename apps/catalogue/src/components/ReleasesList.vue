<template>
  <div>
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
      <div class="cart-columns">
        <ReleaseCard
          v-for="release in releases"
          :key="release.resource.name + release.version"
          :release="release"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import ReleaseCard from "./ReleaseCard";

export default {
  components: {
    ReleaseCard,
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
      releases: [],
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
        `query Releases($filter:ReleasesFilter,$offset:Int,$limit:Int){Releases(offset:$offset,limit:$limit,${searchString}filter:$filter){resource{acronym},version}
        ,Releases_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.releases = data.Releases;
          this.count = data.Releases_agg.count;
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
