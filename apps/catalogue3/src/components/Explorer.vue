<template>
  <div id="app">
    <Molgenis>
      <Spinner v-if="loading" />
      <div v-else>
        <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      </div>
      <div class="row">
        <div class="col-2">
          <FilterSidebar v-model="filters" />
        </div>
        <div class="col-9">
          <div class="row">
            <div class="col">
              <h1>Variable Catalogue</h1>
              <p>
                From cohorts, biobanks, registries, harmonisation projects, and
                more ...
              </p>
            </div>
            <div class="col">
              <InputSearch placeholder="Search..." v-model="search" />
              <form style="width: 100%" class="d-flex flex-row-reverse">
                <div>
                  <label>Limit search to:</label>
                  <InputCheckbox
                    class="custom-control-inline ml-2"
                    :options="['Collections', 'Tables', 'Variables', 'Topics']"
                  />
                </div>
              </form>
            </div>
          </div>
          <FilterWells v-model="filters" />
          <ul class="nav nav-tabs">
            <li>
              <router-link
                class="nav-link"
                :class="{ active: selected == 'Collections' }"
                to="resources"
              >
                Collections ({{ resourceCount }})
              </router-link>
            </li>
            <li>
              <router-link
                class="nav-link"
                :class="{ active: selected == 'Tables' }"
                to="tables"
              >
                Dataset ({{ datasetCount }})
              </router-link>
            </li>
            <li>
              <router-link
                class="nav-link"
                :class="{ active: selected == 'Variables' }"
                to="variables"
              >
                Variables ({{ variableCount }})
              </router-link>
            </li>
          </ul>
          <br />
          <router-view :search="search" />
        </div>
      </div>
    </Molgenis>
  </div>
</template>

<script>
import {
  FilterSidebar,
  FilterWells,
  InputCheckbox,
  InputSearch,
  MessageError,
  MessageSuccess,
  Molgenis,
  Spinner,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    FilterSidebar,
    FilterWells,
    InputSearch,
    MessageError,
    MessageSuccess,
    Spinner,
    Molgenis,
    InputCheckbox,
  },
  data: function () {
    return {
      selectedDatabanks: [],
      graphqlError: null,
      success: null,
      loading: false,
      topics: [],
      databanksCount: 0,
      variableCount: 0,
      datasetCount: 0,
      limit: 20,
      page: 1,
      search: "",
      variableSearch: "",
      selectedTopic: null,
      variables: [],
      timestamp: Date.now(),
      filters: [
        {
          name: "Topic",
          refTable: "Topics",
          columnType: "REF",
        },
        {
          name: "Population",
          refTable: "InclusionCriteria",
          columnType: "REF",
        },
        {
          name: "Inclusion Criteria",
          refTable: "InclusionCriteria",
          columnType: "REF",
        },

        {
          name: "Number Of Participants",
          refTable: "AgeCategories",
          columnType: "REF",
        },
        {
          name: "Recruitment age",
          refTable: "AgeCategories",
          columnType: "REF",
        },
        {
          name: "Country",
          refTable: "InclusionCriteria",
          columnType: "REF",
        },
        {
          name: "Host organisation",
          refTable: "Institutes",
          columnType: "REF",
        },
        {
          name: "Format",
          refTable: "Formats",
          columnType: "REF",
        },
        {
          name: "Unit",
          refTable: "Units",
          columnType: "REF",
        },
      ],
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
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
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadVariables() {
      let filter = {};
      let search = "";
      if (this.search && this.search.trim() != "") {
        search = `search:"${this.search}",`;
      }
      if (this.selectedTopic) {
        filter.topics = { name: { equals: this.selectedTopic } };
      }
      if (this.selectedCollections.length > 0) {
        filter.resource = {
          name: {
            equals: this.selectedCollections,
          },
        };
      }
      request(
        "graphql",
        `query countQuery($cFilter:CollectionsFilter,$vFilter:VariablesFilter,$tFilter:DatasetsFilter){Collections_agg(${search}filter:$cFilter){count}
        ,Variables_agg(${search}filter:$vFilter){count},Datasets_agg(${search}filter:$tFilter){count}}`,
        {
          cFilter: filter,
          vFilter: filter,
          tFilter: filter,
          offset: (this.page - 1) * 10,
          limit: this.limit,
        }
      )
        .then((data) => {
          this.variableCount = data.Variables_agg.count;
          this.resourceCount = data.Collections_agg.count;
          this.DatasetCount = data.Datasets_agg.count;
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
