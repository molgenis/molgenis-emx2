<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div>
      <Pagination
        v-if="count > 0"
        class="justify-content-center mb-2"
        :count="count"
        v-model="page"
        :limit="limit"
        :defaultValue="page"
      />
      <p v-else>No records found.</p>
    </div>
    <div class="row">
      <ProjectCard
        v-for="project in projects"
        :key="project.name"
        :project="project"
      />
    </div>
  </div>
</template>

<script>
import { MessageError, Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import ProjectCard from "./ProjectCard";

export default {
  components: {
    ProjectCard,
    Pagination,
    MessageError,
  },
  props: {
    pid: String,
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
      graphqlError: null,
      loading: false,
      projects: [],
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
        `query Projects($filter:ProjectsFilter,$offset:Int,$limit:Int){Projects(offset:$offset,limit:$limit,${searchString}filter:$filter){name,pid,type{name},description,website,institution{name}}
        ,Projects_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.projects = data.Projects;
          this.count = data.Projects_agg.count;
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
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
