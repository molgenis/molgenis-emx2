<template>
  <RouterView />
  <PageFooter>
    <MolgenisLogo />
  </PageFooter>
</template>

<script setup>
import { RouterView } from "vue-router";
import PageFooter from "./components/layouts/PageFooter.vue";
import MolgenisLogo from "./components/display/MolgenisLogo.vue";
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

.message-box {
  .message-text {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    p {
      margin: 0;
    }
  }
}

.page-footer {
  padding: 2em 0;
}
</style>
