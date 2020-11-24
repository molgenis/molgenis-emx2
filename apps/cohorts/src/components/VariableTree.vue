<template>
  <Molgenis title="Variables">
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="error">{{ error }}</MessageError>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
    </div>
    <InputSearch placeholder="Search topics..." v-model="search" />
    <div class="row">
      <div class="col-sm-4">
        <ul class="fa-ul">
          <TreeNode
            @select="select"
            :topic="topic"
            v-for="topic in topics"
            :key="topic.name + topic.match + topic.collapsed"
          />
        </ul>
      </div>
      <div class="col-sm-8">
        <VariablePanel
          v-for="variable in variables"
          :key="variable.name"
          :variable="variable"
        />
      </div>
    </div>
    <ShowMore title="debug">
      <pre>
          search = {{ search }}
          selectedTopic = {{ selectedTopic }}
          variables ={{ variables }}
          topics = {{ topics }}</pre
      >
    </ShowMore>
  </Molgenis>
</template>

<script>
import TreeNode from "./TreeNode.vue";
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
      error: null,
      success: null,
      loading: false,
      topics: [],
      search: "",
      selectedTopic: null,
      variables: [],
    };
  },
  methods: {
    load() {
      request(
        "graphql",
        "{Topics(orderby:{order:ASC}){name,parentTopic{name}, childTopics{name,childTopics{name, childTopics{name,childTopics{name,childTopics{name}}}}}}}"
      )
        .then((data) => {
          this.topics = data.Topics.filter(
            (t) => t["parentTopic"] == undefined
          );
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
      if (this.selectedTopic) {
        //
        request(
          "graphql",
          '{Variables(filter:{topic:{name:{equals:"' +
            this.selectedTopic +
            '"}}}){name,table{name},format{name},description,unit{name},codeList{name}}}'
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
      }
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
    this.load();
  },
  watch: {
    search() {
      this.applySearch(this.topics, this.search);
    },
    selectedTopic() {
      this.loadVariables();
    },
  },
};
</script>
