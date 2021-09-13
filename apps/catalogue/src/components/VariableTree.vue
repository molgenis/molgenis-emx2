<template>
  <div>
    {{ graphqlError }}
    <div class="row">
      <div class="col-3">
        <label>Topics:</label>
        <TopicFilter
          :topics="topics"
          @select="select"
          @deselect="deselect"
          :selected="selectedTopic"
        />
      </div>
      <VariablesList
        class="col-9"
        :resource-pid="resourcePid"
        :topic="selectedTopic"
      />
    </div>
  </div>
</template>

<script>
import TreeNode from "./TreeFilter.vue";
import CohortSelection from "./CohortSelection";
import VariablesList from "./VariablesList";
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
  Spinner,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";
import TopicFilter from "./TopicSelector";

export default {
  components: {
    TopicFilter,
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
    VariablesList,
  },
  props: {
    resourcePid: String,
  },
  data: function () {
    return {
      selectedCollections: [],
      graphqlError: null,
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
    select(topic) {
      this.selectedTopic = topic.name;
    },
    deselect() {
      this.selectedTopic = null;
    },
    loadTopics() {
      request(
        "graphql",
        "{Topics(orderby:{order:ASC}){name,parent{name},variables_agg{count},children{name,variables_agg{count},children{name, variables_agg{count},children{name,variables{name},children{name,variables{name},children{name}}}}}}}"
      )
        .then((data) => {
          this.topics = data.Topics.filter(
            (t) => t["parentTopic"] == undefined
          );
          this.topics = this.topicsWithContents(this.topics);
          this.applySearch(this.topics, this.search);
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message;
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
  },
  created() {
    this.loadTopics();
  },
  watch: {
    search() {
      this.applySearch(this.topics, this.search);
    },
  },
};
</script>
