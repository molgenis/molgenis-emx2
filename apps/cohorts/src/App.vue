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
          :key="session"
          v-if="collectionAcronym"
          :collectionAcronym="collectionAcronym"
          :datasetName="datasetName"
        />
        <div class="row cohorts-scroll" :key="session">
          <TableOfContents
            class="pr-4 col-3"
            style="overflow: scroll"
            :collectionAcronym="collectionAcronym"
            :datasetName="datasetName"
          />
          <RouterView
            class="col-9"
            :key="collectionAcronym + ': ' + datasetName"
          />
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
    collectionAcronym() {
      return this.$route.params.collectionAcronym;
    },
    datasetName() {
      return this.$route.params.datasetName;
    },
  },
};
</script>
