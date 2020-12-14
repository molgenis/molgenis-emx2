<template>
  <div>
    <div class="card-columns">
      <VariablePanel
        v-for="variable in variables"
        :key="variable.collection.name + variable.table.name + variable.name"
        :variable="variable"
      />
    </div>
    <Pagination
      class="justify-content-center"
      :count="count"
      v-model="page"
      :limit="limit"
      :defaultValue="page"
    />
  </div>
</template>

<script>
import VariablePanel from "../components/VariablePanel";
import { Pagination } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    VariablePanel,
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
      variables: [],
    };
  },
  methods: {
    reload() {
      console.log("variables reload");
      let searchString = "";
      if (this.search && this.search.trim() != "") {
        searchString = `search:"${this.search}",`;
      }
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,${searchString}filter:$filter){name,collection{name},table{name},topics{name},mandatory,valueLabels,missingValues,harmonisations{sourceTable{collection{name}}},unit{name},format{name},description,unit{name},codeList{name,codes{value,label}}}
        ,Variables_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
          this.count = data.Variables_agg.count;
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
