<template>
  <div id="app">
    <Molgenis>
      <BreadCrumb
        v-if="collectionAcronym"
        :collectionAcronym="collectionAcronym"
        :datasetName="datasetName"
      />
      <div class="row">
        <TableOfContents
          class="pr-4 col-3"
          style="overflow: scroll"
          :collectionAcronym="collectionAcronym"
          :datasetName="datasetName"
        />
        <RouterView class="col-9" />
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
