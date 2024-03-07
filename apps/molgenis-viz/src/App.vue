<template>
  <Molgenis :v-model="session">
    <RouterView :session="session" :page="page" />
    <PageFooter>
      <MolgenisLogo />
    </PageFooter>
  </Molgenis>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { RouterView } from "vue-router";
import { Molgenis } from "molgenis-components";
import PageFooter from "./components/layouts/PageFooter.vue";
import MolgenisLogo from "./components/display/MolgenisLogo.vue";

const session = ref(null);
const page = ref(null);
</script>

<style lang="scss">
body {
  padding: 0;
  margin: 0;
}

.molgenis-logo {
  display: block;
  margin: 0 auto;
  padding: 0;
}

.app-page {
  font-family: Arial, Helvetica, sans-serif;
  font-size: 14pt;

  .bkg-light {
    background-color: $gray-050;
  }

  .output {
    display: block;
    width: 100%;
    box-sizing: content-box;
    padding: 1em;
    background-color: $gray-700;
    color: $gray-050;
  }
}
.page-footer {
  font-family: Arial, Helvetica, sans-serif;
  padding: 2em 0;
  .molgenis-citation {
    background-color: transparent;
  }
}
</style>
