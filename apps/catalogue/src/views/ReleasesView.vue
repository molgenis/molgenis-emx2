<template>
  <div v-if="release">
    <h1>
      <small>Release:</small><br />{{ release.resource.acronym }} ({{
        release.version
      }})
    </h1>
    <h4>Variables:</h4>
    <VariableTree :resourceAcronym="release.resource.acronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import VariablesList from "../components/VariablesList";
import TopicSelector from "../components/TopicSelector";
import VariableTree from "../components/VariableTree";

export default {
  components: { VariableTree, TopicSelector, VariablesList },
  props: {
    resourceAcronym: String,
    version: String,
  },
  data() {
    return {
      release: null,
    };
  },
  methods: {
    reload() {
      console.log(this.version + " " + this.resourceAcronym);
      request(
        "graphql",
        `query Releases($acronym:String,$version:String){Releases(filter:{resource:{acronym:{equals:[$acronym]}},version:{equals:[$version]}}){resource{acronym},version}}`,
        {
          acronym: this.resourceAcronym,
          version: this.version,
        }
      )
        .then((data) => {
          this.release = data.Releases[0];
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    resourceAcronym() {
      this.reload();
    },
    version() {
      this.reload();
    },
  },
};
</script>
