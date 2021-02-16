<template>
  <div>
    <h1>
      <small>Project:</small><br />{{ project.name }} ({{ project.acronym }})
    </h1>
    <NavTabs
      :options="['Variables', 'Description', 'Partners', 'Inventory']"
      v-model="tab"
      class="nav-pills"
    />
    <div class="tab-content" id="myTabContent">
      <div v-if="tab == 'Description'" class="tab-pane show active">
        <ReadMore
          :text="project.description"
          :length="1000"
          v-if="project.description"
        />
      </div>
      <div v-if="tab == 'Partners'" class="tab-pane show active">
        <table v-if="project.partners" class="table">
          <thead>
            <tr>
              <th scope="col">Acronym</th>
              <th scope="col">Name</th>
              <th scope="col">contact(s)</th>
              <th scope="col">type</th>
              <th scope="col">country</th>
            </tr>
          </thead>
          <tr v-for="partner in project.partners">
            <td>{{ partner.acronym }}</td>
            <td>{{ partner.name }}</td>
            <td>{{ partner.type ? partner.type.name : "" }}</td>
            <td>{{ partner.country ? partner.country.name : "" }}</td>
          </tr>
        </table>
      </div>
      <div v-if="tab == 'Variables'" class="tab-pane show active">
        <span v-if="project.releases == null">No data loaded</span>
        <span v-else>
          <InputSelect
            label="choose a release"
            v-if="project.releases"
            v-model="version"
            :options="project.releases.map((r) => r.version)" />
          <VariableTree
            v-if="version"
            :resourceAcronym="project.acronym"
            :version="version"
        /></span>
      </div>
      <div v-if="tab == 'Inventory'" class="tab-pane show active">
        <HarmonisationList :resource-acronym="projectAcronym" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  MessageError,
  ReadMore,
  InputSelect,
  NavTabs,
} from "@mswertz/emx2-styleguide";
import TableList from "../components/TableList";
import VariableTree from "../components/VariableTree";
import HarmonisationList from "../components/HarmonisationList";

export default {
  components: {
    HarmonisationList,
    VariableTree,
    MessageError,
    ReadMore,
    TableList,
    InputSelect,
    NavTabs,
  },
  props: {
    projectAcronym: String,
  },
  data() {
    return {
      version: null,
      error: null,
      project: {},
      tab: "Variables",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Projects($acronym:String){Projects(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, partners{acronym,name,country{name}} supplementaryInformation, releases{version}}}`,
        {
          acronym: this.projectAcronym,
        }
      )
        .then((data) => {
          this.project = data.Projects[0];
          if (this.project.releases) {
            this.version = this.project.releases[
              this.project.releases.length - 1
            ].version;
          }
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
    projectAcronym() {
      this.reload();
    },
  },
};
</script>
