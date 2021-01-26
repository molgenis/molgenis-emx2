<template></template>

<script>
import TreeNode from "./TreeFilter.vue";
import CohortSelection from "./CohortSelection";
import VariablePanel from "./VariablePanel";
import {
  ButtonAction,
  ButtonAlt,
  DataTable,
  InputCheckbox,
  InputFile,
  InputSearch,
  InputSelect,
  InputString,
  LayoutCard,
  LayoutNavTabs,
  MessageError,
  MessageSuccess,
  Molgenis,
  Pagination,
  ShowMore,
  Spinner,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import TreeMultiFilter from "./TreeMultiFilter";

export default {
  components: {
    TreeMultiFilter,
    CohortSelection,
    LayoutNavTabs,
    TreeNode,
    ButtonAction,
    ButtonAlt,
    InputFile,
    DataTable,
    InputSearch,
    MessageError,
    MessageSuccess,
    LayoutCard,
    Spinner,
    Molgenis,
    Pagination,
    InputCheckbox,
    InputString,
    InputSelect,
    ShowMore,
    VariablePanel,
  },
  data: function () {
    return {
      selectedCollections: [],
      error: null,
      success: null,
      loading: false,
      topics: [],
      count: 0,
      limit: 20,
      page: 1,
      search: "",
      variableSearch: "",
      selectedTopic: null,
      variables: [],
      timestamp: Date.now(),
    };
  },
  methods: {
    selectedTopics(topics) {
      if (Array.isArray(topics)) {
        return topics.filter((t) => t.checked).map((t) => t.name);
      }
      return [];
    },
    loadTopics() {
      request(
        "graphql",
        "{Topics(orderby:{order:ASC}){name,parentTopic{name},variables{name},childTopics{name,variables{name},childTopics{name, variables{name},childTopics{name,variables{name},childTopics{name,variables{name},childTopics{name}}}}}}}"
      )
        .then((data) => {
          this.topics = data.Topics.filter(
            (t) => t["parentTopic"] == undefined
          );
          this.topics = this.topicsWithContents(this.topics);
          this.applySearch(this.topics, this.search);
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadVariables() {
      let filter = {};
      let search = "";
      if (this.variableSearch && this.variableSearch.trim() != "") {
        search = `search:"${this.variableSearch}",`;
      }
      if (this.selectedTopic) {
        filter.topics = { name: { equals: this.selectedTopic } };
      }
      if (this.selectedCollections.length > 0) {
        filter.collection = {
          name: {
            equals: this.selectedCollections,
          },
        };
      }
      request(
        "graphql",
        `query Variables($filter:VariablesFilter,$offset:Int,$limit:Int){Variables(offset:$offset,limit:$limit,${search}filter:$filter){name,collection{name},dataset{name},topics{name},valueLabels,missingValues,harmonisations{sourceDataset{collection{name}}},format{name},description,unit{name},codeList{name,codes{value,label}}}
        ,Variables_agg(${search}filter:$filter){count}}`,
        {
          filter: filter,
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
    topicsWithContents(topics) {
      let result = [];
      if (topics)
        topics.forEach((t) => {
          let childTopics = this.topicsWithContents(t.childTopics);
          if (t.variables || childTopics.length > 0) {
            result.push({ name: t.name, childTopics: childTopics });
          }
        });
      return result;
    },
    applySearch(topics, terms) {
      let result = false;
      topics.forEach((t) => {
        t.match = false;
        if (
          terms == null ||
          t.name.toLowerCase().includes(terms.toLowerCase())
        ) {
          t.match = true;
          result = true;
        }
        if (t.childTopics && this.applySearch(t.childTopics, terms)) {
          t.match = true;
          result = true;
        }
      });
      return result;
    },
    select(topic) {
      this.selectedTopic = topic.name;
    },
  },
  created() {
    this.loadTopics();
    this.loadVariables();
  },
  watch: {
    search() {
      this.applySearch(this.topics, this.search);
    },
    variableSearch() {
      this.loadVariables();
    },
    selectedCollections() {
      this.loadVariables();
    },
    page() {
      this.loadVariables();
    },
  },
};
</script>
