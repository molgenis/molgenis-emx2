<template>
  <div id="app">
    <Molgenis v-model="session">
      <div
        v-if="!session || !session.roles || !session.roles.includes('Viewer')"
      >
        <h1 class="text-centered">You have to login to view the data.</h1>
      </div>
      <div v-else>
        <BreadCrumb
          :databankAcronym="databankAcronym"
          :institutionAcronym="institutionAcronym"
          :projectAcronym="projectAcronym"
          :tableName="tableName"
          :resourceAcronym="resourceAcronym"
          :datasourceAcronym="datasourceAcronym"
          :version="version"
        />
        <div class="container-fluid">
          <RouterView :key="databankAcronym + ': ' + tableName" />
        </div>
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";
import BreadCrumb from "./components/BreadCrumb";

export default {
  components: {
    BreadCrumb,
    Molgenis,
  },
  data() {
    return {
      session: {},
    };
  },
  computed: {
    view() {
      return this.$route.name;
    },
    databankAcronym() {
      return this.$route.params.databankAcronym;
    },
    institutionAcronym() {
      return this.$route.params.institutionAcronym;
    },
    projectAcronym() {
      return this.$route.params.projectAcronym;
    },
    tableName() {
      return this.$route.params.tableName;
    },
    resourceAcronym() {
      return this.$route.params.resourceAcronym;
    },
    datasourceAcronym() {
      return this.$route.params.datasourceAcronym;
    },
    version() {
      return this.$route.params.version;
    },
  },
};
</script>
