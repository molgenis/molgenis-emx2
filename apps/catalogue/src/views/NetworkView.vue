<template>
  <div class="container-fluid bg-white">
    <div class="p-2 bg-success text-white">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/projects" class="text-white"> consortia</RouterLink>
        /
      </h6>
    </div>
    <h4>
      <span
        class="badge badge-pill badge-secondary float-right"
        v-if="project.type"
      >
        {{ project.type.map((t) => t.name).join(",") }}
      </span>
    </h4>
    <h1>
      {{ projectAcronym }}
      <br />
      {{ project.name }}
    </h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <a v-if="project.website" :href="project.website">{{ project.website }}</a>
    <p v-else>Website: N/A</p>
    <p v-if="project.description">{{ project.description }}</p>
    <p v-else>Description: N/A</p>
    <div class="row">
      <div class="col">
        <h6>Coordinator</h6>
        <p>{{ project.provider ? project.provider.name : "N/A" }}</p>
        <h6>Intitutions involved</h6>
        <ul v-if="project.partners">
          <li v-for="p in project.partners" :key="p.name">{{ p.name }}</li>
        </ul>
        <p v-else>N/A</p>
        <h6>Datasources involved</h6>
        <ul v-if="project.datasources">
          <li v-for="d in project.datasources" :key="d.acronym">
            <RouterLink
              :to="{
                name: 'datasource',
                params: { datasourceAcronym: d.acronym },
              }"
              >{{ d.acronym }} - {{ d.name }}
            </RouterLink>
          </li>
        </ul>
        <p v-else>N/A</p>
        <h6>Databanks involved</h6>
        <ul v-if="project.databanks">
          <li v-for="d in project.databanks" :key="d.acronym">
            <RouterLink
              :to="{ name: 'databank', params: { databankAcronym: d.acronym } }"
              >{{ d.acronym }} - {{ d.name }}
            </RouterLink>
          </li>
        </ul>
        <p v-else>N/A</p>
        <h6>Funding</h6>
        <p>{{ project.funding ? project.funding : "N/A" }}</p>
      </div>
      <div class="col">
        <h6>Protocols</h6>
        <ul v-if="project.documentation">
          <li v-for="d in project.documentation" :key="d.name">{{ d.name }}</li>
        </ul>
        <p v-else>N/A</p>
        <h6>Data releases</h6>
        <ul v-if="project.releases">
          <li v-for="r in project.releases" :key="r.resource.name + r.version">
            <RouterLink
              :to="{
                name: 'release',
                params: {
                  resourceAcronym: r.resource.acronym,
                  version: r.version,
                },
              }"
              >{{ r.version }}
            </RouterLink>
          </li>
        </ul>
        <p v-else>N/A</p>
        <h6>Publications</h6>
        <p>{{ project.publications ? project.publications : "N/A" }}</p>
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
        `query Projects($acronym:String){Projects(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,website, partners{acronym,name,country{name}}, datasources{acronym,name}, databanks{acronym,name}, supplementary, releases{resource{acronym,name},version}}}`,
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
