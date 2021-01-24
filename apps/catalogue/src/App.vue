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
          :collectionAcronym="collectionAcronym"
          :providerAcronym="providerAcronym"
          :datasetName="datasetName"
        />
        <div class="container-fluid">
          <div class="row cohorts-scroll">
            <RouterView :key="collectionAcronym + ': ' + datasetName" />
          </div>
        </div>
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";
import TableOfContents from "./components/TableOfContents";
import BreadCrumb from "./components/BreadCrumb";

export default {
  components: {
    BreadCrumb,
    Molgenis,
    TableOfContents,
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
    collectionAcronym() {
      return this.$route.params.collectionAcronym;
    },
    providerAcronym() {
      return this.$route.params.providerAcronym;
    },
    datasetName() {
      return this.$route.params.datasetName;
    },
  },
};
</script>
