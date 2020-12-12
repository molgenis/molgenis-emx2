<template>
  <div>
    <div
      v-for="collection in collections"
      :key="collection.name"
      :collection="collection"
    >
      <span>
        <h2>{{ collection.name }}<span v-if="collection.acronym"></span></h2>
        <a href="collection.url">website</a>
      </span>
      <p>
        <ReadMore v-if="collection.description">
          {{ collection.description }}
        </ReadMore>
        <span v-else>No description provided</span>
      </p>
      <ul>
        <div v-for="table in collection.tables">
          {{ table.name }}
          <ul>
            <li v-for="variable in table.variables">
              {{ variable.name }}
            </li>
          </ul>
        </div>
      </ul>
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
import { ButtonAction, Pagination, ReadMore } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    Pagination,
    ReadMore,
    ButtonAction,
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
        `query Collections($filter:CollectionsFilter,$offset:Int,$limit:Int){Collections(offset:$offset,limit:$limit,${searchString}filter:$filter){name,description,tables{name,variables{name}}}
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
