<template>
  <div class="container bg-white">
    <div class="p-2 bg-primary text-white">
      <h6>
        <RouterLink to="/" class="text-white"> home</RouterLink>
        /
        <RouterLink to="/models" class="text-white"> models</RouterLink>
        /
      </h6>
    </div>
    <h4>
      <span
        class="badge badge-pill badge-secondary float-right"
        v-if="model.type"
      >
        {{ model.type.map((t) => t.name).join(",") }}
      </span>
    </h4>
    <h1>
      {{ modelAcronym }}
      <br />
      {{ model.name }}
    </h1>
    <MessageError v-if="error">{{ error }}</MessageError>
    <a v-if="model.website" :href="model.website">{{ model.website }}</a>
    <p v-else>Website: N/A</p>
    <p v-if="model.description">{{ model.description }}</p>
    <p v-else>Description: N/A</p>
    <div class="row">
      <div class="col">
        <h6>Coordinator</h6>
        <p>{{ model.provider ? model.provider.name : "N/A" }}</p>
        <h6>Institutions</h6>
        <PartnersList :institutions="model.partners" />
        <h6>Networks involved</h6>
        <NetworkList :networks="model.networks" />
        <h6>Databanks involved</h6>
        <DatabankList :databanks="model.databanks" />
        <h6>Funding</h6>
        <p>{{ model.funding ? model.funding : "N/A" }}</p>
      </div>
      <div class="col">
        <h6>Model releases</h6>
        <ul v-if="model.releases">
          <li v-for="r in model.releases" :key="r.resource.name + r.version">
            <RouterLink
              :to="{ name: 'release', params: { releaseAcronym: r.resource } }"
              >{{ r.version }}
            </RouterLink>
          </li>
        </ul>
        <p v-else>N/A</p>
        <h6>Documentation</h6>
        <ul v-if="model.documentation">
          <li v-for="p in model.documentation" :key="p.name">{{ p.name }}</li>
        </ul>
        <p v-else>N/A</p>
        <h6>Publications</h6>
        <p>{{ model.publications ? model.publications : "N/A" }}</p>
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
import HarmonisationList from "../components/HarmonisationList";
import DatabankList from "../components/DatabankList";
import NetworkList from "../components/NetworkList";
import InstitutionList from "../components/InstitutionList";
import PartnersList from "../components/PartnersList";

export default {
  components: {
    PartnersList,
    InstitutionList,
    NetworkList,
    DatabankList,
    HarmonisationList,
    MessageError,
    ReadMore,
    InputSelect,
    NavTabs,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      version: null,
      error: null,
      model: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Models($acronym:String){Models(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,homepage, partners{institution{acronym,name,country{name}}},releases{resource{name},version}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.model = data.Models[0];
        })
        .catch((error) => {
          if (error.response) this.error = error.response.errors[0].message;
          else this.error = error;
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
    modelAcronym() {
      this.reload();
    },
  },
};
</script>
