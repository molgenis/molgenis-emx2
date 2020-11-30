<template>
  <Molgenis title="Browse variables">
    <ShowMore title="debug">
      <pre>
          search = {{ search }}
          selectedTopic = {{ selectedTopic }}
          selectedCollections = {{ selectedCollections }}
          topics = {{ topics }}
      </pre>
    </ShowMore>
    <CohortSelection v-model="selectedCollections" />
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    <div class="row">
      <div class="col-6 col-md-4">
        <LayoutCard title="Topics">
          <InputSearch placeholder="Filter topics..." v-model="search" />
          <div>
            <ul class="fa-ul">
              <TreeNode
                @select="select"
                :topic="topic"
                v-for="topic in topics"
                :key="topic.name + topic.match + topic.collapsed"
              />
            </ul>
          </div>
        </LayoutCard>
      </div>
      <div class="col-md-8">
        <LayoutCard title="Variables">
          <InputSearch
            placeholder="Filter variables..."
            v-model="variableSearch"
          />
          <div>
            <VariablePanel
              v-for="variable in variables"
              :key="
                variable.collection.name + variable.table.name + variable.name
              "
              :variable="variable"
            />
          </div>
        </LayoutCard>
      </div>
    </div>
  </Molgenis>
</template>

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
  MessageError,
  MessageSuccess,
  Molgenis,
  ShowMore,
  Spinner,
} from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: {
    CohortSelection,
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
      search: "",
      variableSearch: "",
      selectedTopic: null,
      variables: [],
    };
  },
  methods: {
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
        `query Variables($filter:VariablesFilter){Variables(${search}filter:$filter){name,collection{name},table{name},topics{name},valueLabels,missingValues,harmonisations{sourceTable{collection{name}}},format{name},description,unit{name},codeList{name,codes{value,label}}}}`,
        {
          filter: filter,
        }
      )
        .then((data) => {
          this.variables = data.Variables;
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
    selectedTopic() {
      this.loadVariables();
    },
    selectedCollections() {
      this.loadVariables();
    },
  },
};
</script>
