<template>
  <Molgenis :v-model="session">
    <div class="app-nav">
      <div>
        <router-link :to="{ name: 'charts-home' }">molgenis-viz</router-link>
      </div>
      <nav>
        <ul>
          <li>
            <router-link :to="{ name: 'charts-home' }">View Charts</router-link>
          </li>
          <li>
            <router-link :to="{ name: 'charts-builder' }">
              Add Chart
            </router-link>
          </li>
          <li><router-link :to="{ name: 'docs' }">Docs</router-link></li>
        </ul>
      </nav>
    </div>
    <RouterView :session="session" :page="page" />
    <template v-slot:footer>
      <PageFooter>
        <MolgenisLogo />
      </PageFooter>
    </template>
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

.app-nav {
  display: grid;
  grid-template-columns: 2fr 1fr;
  align-items: center;
  padding: 1em 2em;
  box-shadow: $box-shadow;

  nav {
    ul {
      padding: 0;
      margin: 0;
      display: flex;
      justify-content: flex-end;
      gap: 1.5em;

      li {
        display: block;
      }
    }
  }
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
